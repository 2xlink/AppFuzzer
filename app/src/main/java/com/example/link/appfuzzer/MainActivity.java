package com.example.link.appfuzzer;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * Draws the main UI and handles intents from the wrapper script.
 */
public class MainActivity extends AppCompatActivity {

    private static final String LOGTAG = "MainActivity";
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
     * Clears the logcat.
     */
    protected static void clearLogcat() {
        try {
            Runtime runtime = Runtime.getRuntime();
            String[] cmdline = { "logcat", "-c"};
            java.lang.Process p = runtime.exec(cmdline);
            p.waitFor();
            if (p.exitValue() == 0) {
                Log.d(LOGTAG, "Cleared logcat.");
            } else {
                Log.w(LOGTAG, "Could not clear logcat!")
;            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Takes an intent and sets the Configuration according to its Extras.
     * <table>
     <tr><td>Extra String</td> <td>package_name</td>
     <tr><td>Extra String</td> <td>username</td>
     <tr><td>Extra String</td> <td>password</td>
     <tr><td>Extra String</td> <td>launcher_package_name</td>
     <tr><td>Extra String</td> <td>url</td>
     <tr><td>Extra int</td> <td>max_reps</td>
     <tr><td>Extra int</td> <td>max_sets</td>
     <tr><td>Extra int</td> <td>timeout (The timeout in ms until the Timer Callback kicks in)</td>
     <tr><td>Extra Float</td> <td>text_input_chance</td>
     <tr><td>Extra Float</td> <td>checkbox_tick_chance</td>
     <tr><td>Extra Float</td> <td>radiobutton_tick_chance</td>
     <tr><td>Extra Float</td> <td>scroll_chance</td>
     <tr><td>Extra Float</td> <td>OAuth_search_chance</td>
     <tr><td>Extra Float</td> <td>backbutton_press_chance</td>
     </table>
     * @param intent {@link Intent}: The intent to extract Extras from.
     */
    private void setConfiguration(Intent intent) {
        String packagename = intent.getStringExtra("package_name");
        if (packagename != null) Configuration.setPackageName(packagename);
        String username = intent.getStringExtra("username");
        if (username != null) Configuration.setUsername(username);
        String password = intent.getStringExtra("password");
        if (password != null) Configuration.setPassword(password);
        int max_reps = intent.getIntExtra("max_reps", -1);
        if (max_reps != -1) Configuration.setMax_reps(max_reps);
        int max_sets = intent.getIntExtra("max_sets", -1);
        if (max_sets != -1) Configuration.setMax_sets(max_sets);
        String launcher_package_name = intent.getStringExtra("launcher_package_name");
        if (launcher_package_name != null) Configuration.setLauncherPackageName(launcher_package_name);
        String url = intent.getStringExtra("url");
        if (url != null) Configuration.setUrl(url);
        int timeout = intent.getIntExtra("timeout", -1);
        if (timeout != -1) Configuration.setTimeout(timeout);
        double text_input_chance = intent.getFloatExtra("text_input_chance", -1);
        if (text_input_chance != -1) Configuration.setText_input_chance(text_input_chance);
        double checkbox_tick_chance = intent.getFloatExtra("checkbox_tick_chance", -1);
        if (checkbox_tick_chance != -1) Configuration.setCheckbox_tick_chance(checkbox_tick_chance);
        double radiobutton_tick_chance = intent.getFloatExtra("radiobutton_tick_chance", -1);
        if (radiobutton_tick_chance != -1) Configuration.setRadiobutton_tick_chance(radiobutton_tick_chance);
        double scroll_chance = intent.getFloatExtra("scroll_chance", -1);
        if (scroll_chance != -1) Configuration.setScroll_chance(scroll_chance);
        double OAuth_search_chance = intent.getFloatExtra("OAuth_search_chance", -1);
        if (OAuth_search_chance != -1) Configuration.setOAuth_search_chance(OAuth_search_chance);
        double backbutton_press_chance = intent.getFloatExtra("backbutton_press_chance", -1);
        if (backbutton_press_chance != -1) Configuration.setBackbutton_press_chance(backbutton_press_chance);

        Log.i(LOGTAG, "Configuration is now: \n" +
                "Packagename: " + Configuration.getPackageName() + "\n" +
                "Username: " + Configuration.getUsername() + "\n" +
                "Password: " + Configuration.getPassword() + "\n" +
                "Url: " + Configuration.getUrl() + "\n" +
                "Max_reps: " + Configuration.getMax_reps() + "\n" +
                "Max_sets: " + Configuration.getMax_sets() + "\n" +
                "LauncherPackageName: " + Configuration.getLauncherPackageName() + "\n" +
                "Timeout: " + Configuration.getTimeout() + "\n" +
                "Text_input_chance: " + Configuration.getText_input_chance() + "\n" +
                "Checkbox_tick_chance: " + Configuration.getCheckbox_tick_chance() + "\n" +
                "Radiobutton_tick_chance: " + Configuration.getRadiobutton_tick_chance() + "\n" +
                "Scroll_chance: " + Configuration.getScroll_chance() + "\n" +
                "OAuth_search_chance: " + Configuration.getOAuth_search_chance() + "\n" +
                "Backbutton_press_chance: " + Configuration.getBackbutton_press_chance());

    }

    /**
     * Executes a simple <code>su root true</code> to check for root access (and show the dialog for
     * the user).
     */
    private void showRootAccessDialog() {
        try {
            TextView textView = (TextView)findViewById(R.id.textView);
            Runtime runtime = Runtime.getRuntime();
            String[] cmdline = { "su", "root", "exit" };
            Process p = runtime.exec(cmdline);
            p.waitFor();
            if (p.exitValue() == 0) {
                Toast.makeText(this, "Root access granted!", Toast.LENGTH_SHORT).show();
                Configuration.setRoot(true);
                textView.setText("Root access granted!");
            } else {
                Toast.makeText(this, "Could not get root access.", Toast.LENGTH_SHORT).show();
                Configuration.setRoot(false);
                textView.setText("Could not get root access.");
            }
            Log.d(LOGTAG, "Root Access successful? " + Boolean.toString(p.exitValue() == 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClickButtonRootAccess(View v) {
        showRootAccessDialog();
    }

    /**
     * Is called when the activity is created.
     * Sets Configuration according to intent (see {@link MainActivity#setConfiguration(Intent)}),
     * clears the app data and enables the AccessibilityService.<br>
     * If the package name is empty, disables the AccessibilityService.
     * Checks if AppFuzzer dir in sdcard exists and if not, creates it.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOGTAG, "onCreate called.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setConfiguration(getIntent());
        clearLogcat();
//        disablePackageVerifier();
        showRootAccessDialog();

        // Clear internal files in case of restart of app
        File dir = getFilesDir();
        for (File f : dir.listFiles()) {
            if (!f.delete()) Log.w(LOGTAG, "Could not remove " + f.getName());
        }

        Button clickButton = (Button) findViewById(R.id.button);
        clickButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                enableAccessibilityService();
            }
        });
        if (Configuration.getPackageName() != null && !Configuration.getPackageName().trim().equals("")) {
            enableAccessibilityService();
        } else {
            Log.i(LOGTAG, "onCreate: No package name set.");
            disableAccessibilityService();
        }

        Log.v(LOGTAG, "End Main Activity");
    }

    /**
     * Is called when an activity already exists and an intent is sent to it.
     * Sets Configuration according to intent (see {@link MainActivity#setConfiguration(Intent)}),
     * clears the app data and enables the AccessibilityService. <br>
     * If the package name is empty, disables the AccessibilityService.
     * @param intent Intent: The intent that was sent to the activity.
     */
    public void onNewIntent(Intent intent) {
        Log.i(LOGTAG, "Received new intent");
        setConfiguration(intent);
        clearLogcat();
        if (Configuration.getPackageName() != null && !Configuration.getPackageName().equals("")) {
            enableAccessibilityService();
        } else {
            Log.i(LOGTAG, "onNewIntent: No package name set.");
            disableAccessibilityService();
        }
    }
}
