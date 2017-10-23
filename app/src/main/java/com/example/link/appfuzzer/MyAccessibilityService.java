package com.example.link.appfuzzer;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.link.appfuzzer.XMLdumper.XMLdumper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by link on 12.02.17.
 * Source: https://developer.android.com/training/accessibility/service.html
 */

/**
 * Handles the {@link AccessibilityEvent}s.
 */
public class MyAccessibilityService extends AccessibilityService {

    private static String LOGTAG = "onAccessibilityEvent";
    private int REPS = Configuration.getMax_reps();
    private int CURRENT_REP = 0;
    private int SETS = Configuration.getMax_sets();
    private int CURRENT_SET = 0;
    private MyTimerTask _myTimerTask;
    XMLdumper dumper = XMLdumper.getInstance();
    private Lock lock = new ReentrantLock();
//        private static int window_dump_counter = 1000;

    private boolean isRunning = false;
    public Lock getLock() {
        return lock;
    }
    public void setLock(Lock lock) {
        this.lock = lock;
    }

    public int getCURRENT_SET() {
        return CURRENT_SET;
    }

    /**
     * Creates a new instance of {@link MyAccessibilityService} and initializes a {@link MyTimerTask}.
     */
    public MyAccessibilityService() {
        _myTimerTask = new MyTimerTask(this, null);
    }

    /**
     * <p>Launches the app specified in {@link Configuration#getPackageName()} via intent.</p>
     * <p>If the app fails to launch due to strange package name issues, the AccessibilityService
     * is disabled and, to avoid busywaits, the Thread will sleep for 60 seconds.</p>
     */
    public void launchApp() {
        String packageName = Configuration.getPackageName();
        Log.d(LOGTAG, "Launching the app " + packageName);
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Log.e(LOGTAG, "Could not find package " + packageName);
            try {
                disableAccessibilityService();
                Log.i(LOGTAG, "Disabled Acc.Service.");
                writeStatusFile(1);
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * <p>Clears the application data of the app specified in {@link Configuration#getPackageName()}
     * via <code>adb shell pm clear</code>.</p>
     * @return True if successful, False otherwise.
     */
    public void clearAppData() {
        if (Configuration.getRoot()) {
            try {
                Runtime runtime = Runtime.getRuntime();
                String[] cmdline = { "su", "root", "pm", "clear", Configuration.getPackageName()};
                Process p = runtime.exec(cmdline);
                p.waitFor();
                if (p.exitValue() == 0) {
                    Log.d(LOGTAG, "Cleared application data.");
                } else {
                    Log.w(LOGTAG, "Could not clear application data!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.w(LOGTAG, "Did not clear application data, as root is not available.");
        }
    }

    /**
     * Turns the accessibilityService on or off.
     * @param onOff String: "1" turns on, "0" turns off
     */
    private void turnAccessibilityService(String onOff) {
        Settings.Secure.putString( getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "com.example.link.appfuzzer/.MyAccessibilityService");
        Settings.Secure.putString(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, onOff);
    }

    /**
     * Enables the AccessibilityService.
     */
    public void enableAccessibilityService() {
        turnAccessibilityService("1");
    }

    /**
     * Disables the AccessibilityService.
     */
    public void disableAccessibilityService() {
        turnAccessibilityService("0");
    }

    /**
     * <p>Writes the .done or .failed file to notify the wrapper script to kill our process.</p>
     * <p>The file will be created at <code>Environment.getExternalStorageDirectory()/AppFuzzer/</code>.</p>
     * <p>If this fails somehow, an error message is printed and the thread sleeps for a long time.</p>
     * @param status int: Whether to write the done or failed file. <br>0 = done, otherwise failed.
     */
    private void writeStatusFile(int status) {
        try {
            File dir = getFilesDir();
            File file;
            if (status == 0) {
                file = new File(dir, Configuration.getPackageName() + ".done");
                Log.i(LOGTAG, "Writing " + dir + "/" + Configuration.getPackageName() + ".done");
            }
            else {
                file = new File(dir, Configuration.getPackageName() + ".failed");
                Log.i(LOGTAG, "Writing " + dir + "/" + Configuration.getPackageName() + ".failed");
            }

//                new FileOutputStream(file).close();
            FileWriter writer = new FileWriter(file);
            writer.write("");
            writer.close();
        } catch (Exception e) {
            Log.e(LOGTAG, "Could not write file.");
            throw new RuntimeException(e);
        }
    }
    public void writeRAM() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        double availableMegs = mi.availMem / 0x100000L;

//Percentage can be calculated for API 16+
        double percentAvail = mi.availMem / (double)mi.totalMem;
        Log.w("*** RAM availability", Double.toString(percentAvail));
    }

    // This method is called back by the system when it detects an AccessibilityEvent

    /**
     * <p>Called by the system when an event occurs.</p>
     * <p>Locks, and then starts {@link MyTimerTask#injectEventCaller(AccessibilityNodeInfo)}.</p>
     * @param event The event supplied by the Android system.
     */
    @Override
    public void onAccessibilityEvent (AccessibilityEvent event) {
        Log.i(LOGTAG, "********** New Event received: " + event.toString());
        Log.v(LOGTAG, "Locking.");
        lock.lock();

        writeRAM();

        try {
            _myTimerTask.cancelAllTimers();

            AccessibilityNodeInfo node = event.getSource();
            preprocessNode(node);

            dumper.startFile(new File(getFilesDir(), Configuration.getPackageName() + CURRENT_SET));
            dumper.setSource("Accessibility");
            dumper.setRoot(node);

            _myTimerTask.injectEventCaller(node);
        } finally {
            lock.unlock();
            Log.v(LOGTAG, "Unlocked.");
        }
    }

    /**
     * <p>Does some preprocessing on the supplied node. Dumps the node into the private app directory
     * (i.e. <code>/data/data/com.example.link.appfuzzer/files/</code>) for debugging purposes.</p>
     * <p>Also is responsible for handling the current sets and reps. If max_sets (specified in
     * {@link Configuration#getMax_sets()} is reached, {@link MyAccessibilityService#writeStatusFile(int)}
     * is called and the Thread will wait to be killed by the wrapper script.</p>
     * @param node The node.
     */
    public void preprocessNode(AccessibilityNodeInfo node) {
//        AccessibilityNodeInfoDumper.dumpWindowToFile(node,
//                new File(getFilesDir(), "window_dump.xml" + window_dump_counter), 0, 540, 960);
//        Log.i(LOGTAG, "Dumped event to file window_dump.xml" + window_dump_counter);
//        window_dump_counter++;

        CURRENT_REP++;
        Log.i(LOGTAG, "Current rep: " + CURRENT_REP + ", Current set: " + CURRENT_SET);
        if (CURRENT_REP >= REPS) {
            Log.w(LOGTAG, "Maximum reps reached. This was set " + CURRENT_SET);
            _myTimerTask.cancelAllTimers();
            clearAppData();
            dumper.endFile();
            writeLogcatToFile();
//            Log.e("*****************", "Called writeLogcatToFile()!");
//            try {
//                Thread.sleep(1000000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            CURRENT_SET++;
            CURRENT_REP = 0;
        }
        if (CURRENT_SET >= SETS && isRunning) {
            isRunning = false;
            Log.w(LOGTAG, "Maximum sets reached.");
//            _myTimerTask.cancelAllTimers();
//            dumper.endFile();
            disableAccessibilityService();

            CURRENT_SET = 0;
            CURRENT_REP = 0;

            writeStatusFile(0);

            // We just kill ourselves
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
        Log.i(LOGTAG, "CURRENT_STEP is now " + CURRENT_REP);
    }

    /**
     * Dumps the logcat into a file.
     */
    private void writeLogcatToFile() {
        try {
            Log.i(LOGTAG, "Writing logcat to file.");
            File target_file = new File(getFilesDir(), Configuration.getPackageName() + CURRENT_SET + "_logcat");
            // Dump and clear logcat
//            Process process = Runtime.getRuntime().exec("logcat -d " + target_file.getAbsolutePath() + " && logcat -c");
            StringBuilder builder = new StringBuilder();
            Runtime runtime = Runtime.getRuntime();
            String[] cmdline = { "logcat", "-d" };
            Process p = runtime.exec(cmdline);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line + "\n");
            }
            PrintWriter writer = new PrintWriter(target_file);
            writer.write(builder.toString());
            reader.close();
            writer.close();
            p.waitFor();
            Log.i(LOGTAG, "Written " + target_file.getAbsolutePath() + " with exit value " + p.exitValue());

            MainActivity.clearLogcat();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the system wants to interrupt our service.
     */
    @Override
    public void onInterrupt() {
        Log.w(LOGTAG, "onInterrupt was called");
    }

    /**
     * Called when the system successfully connected to our service.
     */
    @Override
    public void onServiceConnected()
    {
        Log.d(LOGTAG, "***** onServiceConnected");
        isRunning = true;
        launchApp();
    }
}
