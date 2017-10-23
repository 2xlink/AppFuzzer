package com.example.link.appfuzzer;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.link.appfuzzer.XMLdumper.XMLdumper;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;

/**
 * Created by link on 12.03.17.
 */

/**
 * Handles the timer management.
 */
public class MyTimerTask extends TimerTask{

    private static String LOGTAG = "MyTimerTask_" + Thread.currentThread().getId();
    private MyAccessibilityService as;
    private AccessibilityNodeInfo previous_node;
    private Timer _timer = new Timer();
    private MyTimerTask _timerCallbackInstance;
    private boolean isRunning = false;
    private Lock lock;
    private EventInjector ei;

    /**
     * Creates a new MyTimerTask.
     * @param as {@link MyAccessibilityService}: An instance of the calling MyAccessibilityService.
     * @param previous_node {@link AccessibilityNodeInfo}: The previous node if it exists. Can be null.
     */
    public MyTimerTask(MyAccessibilityService as, AccessibilityNodeInfo previous_node) {
        this.as = as;
        this.previous_node = previous_node;
        this.lock = as.getLock();
        this.ei = new EventInjector(as);
    }

    /**
     * Injects actions into the supplied node using {@link EventInjector}.<br>
     * Cancels all timers, then injects and then starts a new timer with a fresh instance of MyTimerTask.
     * @param node {@link AccessibilityNodeInfo}: The node to inject to.
     */
    public void injectEventCaller(AccessibilityNodeInfo node) {
        cancelAllTimers();
        if (node != null) {
            Log.i(LOGTAG, "Started MyTimerTask:injectEventCaller with node " +
                    EventInjector.safeCharSeqToString(node.toString()));
            ei.inject(node);
        } else {
            Log.w(LOGTAG, "injectEventCaller: Received a null node!");
        }

        Log.d(LOGTAG, "Starting new timer");
        _timerCallbackInstance = new MyTimerTask(as, previous_node);
        _timer = new Timer();
        _timer.schedule(_timerCallbackInstance, Configuration.getTimeout());
    }

    /**
     * Called when the timer kicks in.
     *
     * <p>Tries to get the lock, then finds the current root in the active window. If that fails,
     * the android app overview is opened to force an AccessibilityEvent. <br>
     * If a current_root is found, the XMLDumper begins to write a timer event. Then
     * {@link MyTimerTask#injectEventCaller(AccessibilityNodeInfo)}} is called.</p>
     */
    @Override
    public void run() {
        Log.d(LOGTAG,"TimerCallback called.");
        Log.v(LOGTAG, "Locking.");
        lock.lock();

        as.writeRAM();

        try {
            // Try to get the current root in active window
            AccessibilityNodeInfo current_node = as.getRootInActiveWindow();
            int i = 0;
            while (current_node == null && i < 1) {
                Log.d(LOGTAG, "Tried to get current_node, but it is null");
                current_node = as.getRootInActiveWindow();
                try {
                    Thread.sleep(50);
                } catch (Exception e) {

                }
                i++;
            }
            if (current_node == null) {
                if (previous_node != null) {
                    Log.w(LOGTAG, "Could not find current_node, trying with previous node ...");
                    current_node = previous_node;
                } else {
                    Log.w(LOGTAG, "Could not find current_node, and previous_node is also null." +
                            " Opening app overview now.");
                    // This will show the recent events screen, then the fuzzer will press "back" and
                    // we hopefully land back right where the app was, but with a useful current_root
                    as.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                    lock.unlock();
                    Log.v(LOGTAG, "Unlocked.");
                    return;
                }
            } else {
                Log.d(LOGTAG, "Found current_node: " + current_node);
            }
            // node is now set to something

            as.preprocessNode(current_node);

            Log.d(LOGTAG, "TimerCallback: We have found a current_root and begin writing the XML now");
            XMLdumper dumper = XMLdumper.getInstance();
            dumper.startFile(new File(as.getFilesDir(), Configuration.getPackageName() + as.getCURRENT_SET()));
            dumper.setSource("Timer");
            dumper.setRoot(current_node);

            Log.d(LOGTAG, "TimerCallback: Calling injectEventCaller");
            injectEventCaller(current_node);
        } finally {
            try {
                lock.unlock();
                Log.v(LOGTAG, "Unlocked.");
            } catch (Exception e) {
                // Seems we already unlocked.
                e.printStackTrace();
            }
        }
    }

    /**
     * Cancels all timers.
     */
    public void cancelAllTimers() {
        Log.d(LOGTAG, "Canceled timer.");
        _timer.cancel();
        _timer.purge();
        if(_timerCallbackInstance != null) _timerCallbackInstance.cancelAllTimers();
        _timerCallbackInstance = null;
    }
}
