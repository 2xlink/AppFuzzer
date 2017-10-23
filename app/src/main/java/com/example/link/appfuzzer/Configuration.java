package com.example.link.appfuzzer;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Created by link on 26.02.17.
 */

/**
 * This class holds information over the behavior of AppFuzzer. Many of its values are set
 * through the intent from the wrapper script.
 */
public class Configuration {
    private static String LOGTAG = "Configuration";

    private static String package_name = "";
    private static String username = "MyUsernameInput";
    private static String password = "MyPasswordInput";
    private static String url = "https://dud.inf.tu-dresden.de";
    private static int max_reps = 1;
    private static int max_sets = 1;
    private static String launcher_package_name = "com.google.android.apps.nexuslauncher";
    private static int timeout = 200; // The timeout in ms until the Timer Callback kicks in
    private static double text_input_chance = 0.5;
    private static double checkbox_tick_chance = 0.5;
    private static double radiobutton_tick_chance = 0.5;
    private static double scroll_chance = 0.25;
    private static double OAuth_search_chance = 0.5;
    private static double backbutton_press_chance = 0.2;

    private static boolean gotRoot = false;

    static String getPackageName() {
        return package_name;
    }
    static void setPackageName(String package_name) {
        Configuration.package_name = package_name;
    }
    static String getUsername() {
        return username;
    }
    static void setUsername(String username) {
        Configuration.username = username;
    }
    static String getPassword() {
        return password;
    }
    static void setPassword(String password) {
        Configuration.password = password;
    }
    static int getMax_reps() {
        return max_reps;
    }
    static void setMax_reps(int max_reps) {
        Configuration.max_reps = max_reps;
    }
    static int getMax_sets() {
        return max_sets;
    }
    static void setMax_sets(int max_sets) {
        Configuration.max_sets = max_sets;
    }
    static String getLauncherPackageName() {
        return launcher_package_name;
    }
    static void setLauncherPackageName(String launcher_package_name) {
        Configuration.launcher_package_name = launcher_package_name;
    }
    static int getTimeout() {
        return timeout;
    }
    static void setTimeout(int timeout) {
        Configuration.timeout = timeout;
    }
    static double getText_input_chance() {
        return text_input_chance;
    }
    static void setText_input_chance(double text_input_chance) {
        Configuration.text_input_chance = text_input_chance;
    }
    static double getCheckbox_tick_chance() {
        return checkbox_tick_chance;
    }
    static void setCheckbox_tick_chance(double checkbox_tick_chance) {
        Configuration.checkbox_tick_chance = checkbox_tick_chance;
    }
    static double getRadiobutton_tick_chance() {
        return radiobutton_tick_chance;
    }
    static void setRadiobutton_tick_chance(double radiobutton_tick_chance) {
        Configuration.radiobutton_tick_chance = radiobutton_tick_chance;
    }
    static double getScroll_chance() {
        return scroll_chance;
    }
    static void setScroll_chance(double scroll_chance) {
        Configuration.scroll_chance = scroll_chance;
    }
    static double getOAuth_search_chance() {
        return OAuth_search_chance;
    }
    static void setOAuth_search_chance(double OAuth_search_chance) {
        Configuration.OAuth_search_chance = OAuth_search_chance;
    }
    static double getBackbutton_press_chance() {
        return backbutton_press_chance;
    }
    static void setBackbutton_press_chance(double backbutton_press_chance) {
        Configuration.backbutton_press_chance = backbutton_press_chance;
    }
    static String getUrl() {
        return url;
    }
    static void setUrl(String url) {
        Configuration.url = url;
    }

    static boolean getTextInputChance() { return Math.random() < text_input_chance; }
    static boolean getCheckboxChance() { return Math.random() < checkbox_tick_chance; }
    static boolean getRadioButtonChance() { return Math.random() < radiobutton_tick_chance; }
    static boolean getScrollChance() { return Math.random() < scroll_chance; }
    static boolean getOAuthSearchChance() { return Math.random() < OAuth_search_chance; }
    static boolean getBackButtonPressChance() { return Math.random() < backbutton_press_chance; }
    // When the back button should be pressed, only press it with this chance

    static boolean getRoot() { return gotRoot; }
    static void setRoot(boolean gotRoot) { Configuration.gotRoot = gotRoot; }

    /**
     * Returns an input for the text or resourceID of an AccessibilityNodeInfo.
     * @param node The Node to query.
     * @return Time input field / Date input field / Number input field / Generic text field
     */
    static String getTextInput(AccessibilityNodeInfo node) {
        String context = "";
        if (node.getText() != null)
            context = XMLTransformations.safeCharSeqToString(node.getText());
        if (context.equals("")) {
            if (node.getViewIdResourceName() != null) {
                context = node.getViewIdResourceName();
            } else {
                Log.i(LOGTAG, "Found a field without text or resourceID.");
            }
        }
        Log.v(LOGTAG, "The context for this field is " + context);
        if (context.toLowerCase().contains("time")) return "Time input field";
        if (context.toLowerCase().contains("date")) return "Date input field";
        if (context.toLowerCase().contains("number")) return "12345678";
        if (context.toLowerCase().contains("phone")) return "12345678";
        if (context.toLowerCase().contains("+1")) return "+1-12345678";
        if (context.toLowerCase().contains("mail")) return getUsername();
        if (context.toLowerCase().contains("user")) return getUsername();
        if (context.toLowerCase().contains("pass")) return getPassword();
        if (context.toLowerCase().contains("city")) return "Dresden";
        if (context.toLowerCase().contains("http")) return getUrl();
        if (context.toLowerCase().contains("url")) return getUrl();

        // Default case
        String[] options = {"Test", "12345", "", "1111", getUrl()};
        java.util.Random random = new java.util.Random();
        int i = random.nextInt(options.length);
        return options[i];
    }
}
