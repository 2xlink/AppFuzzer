package com.example.link.appfuzzer;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.link.appfuzzer.XMLdumper.XMLdumper;
import com.example.link.appfuzzer.XMLdumper.XMLdumperAction;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by link on 24.04.17.
 */

/**
 * Handles the injection of events into an {@link AccessibilityNodeInfo}.
 */
public class EventInjector {
    private static String LOGTAG = "EventInjector_" + Thread.currentThread().getId();
    private MyAccessibilityService as;
    XMLdumper dumper;
    Random random = new Random();

    /**
     * Constructs an new Eventinjector. Needs a {@link MyAccessibilityService} to press the back button.
     * @param as The {@link MyAccessibilityService}
     */
    public EventInjector(MyAccessibilityService as) {
        this.as = as;
    }

    /**
     * Strips invalid XML chars from a CharSequence. Should be called before you write to an XML file.
     * @param cs CharSequence input
     * @return A sanitized String.
     */
    public static String safeCharSeqToString(CharSequence cs) {
        return XMLTransformations.safeCharSeqToString(cs);
    }

//    public boolean isFrame(AccessibilityNodeInfo node) {
////        Log.d(LOGTAG, safeCharSeqToString(node.getClassName()));
//        if (node == null) return false;
//        if (safeCharSeqToString(node.getClassName()).equals("android.widget.FrameLayout")) return true;
//
//        int count = node.getChildCount();
//        for (int i = 0; i < count; i++) {
//            AccessibilityNodeInfo child = node.getChild(i);
//            if (child != null) {
//                if (child.isVisibleToUser()) {
////                if (true) {
//                    // If this child tree contains a FrameLayout, return true
//                    // else check the next child
//                    if(isFrame(child)) return true;
//                    child.recycle();
//                } else {
//                    Log.d(LOGTAG, String.format("Skipping invisible child: %s", child.toString()));
//                }
//            } else {
//                Log.d(LOGTAG, String.format("Null child %d/%d, parent: %s",
//                        i, count, node.toString()));
//            }
//        }
//        return false;
//    }

    /**
     * <p>Finds nodes defined by a String and an attribute. Searches <code>node</code> and its children.</p>
     * <code>Attr</code> defines which attribute of the node should be matched with
     * <code>value</code>. If something matches, it is added to returned list.
     * @param node The root {@link AccessibilityNodeInfo}.
     * @param value A String which should be matched with the text in the attribute.
     * @param attr The {@link AccessibilityNodeAttribute} of the node to match against. When searching
     *             for CLASS or TEXT, only nodes are returned which match <code>value</code>.<br>
     *             For CLICKABLE and SCROLLABLE, all nodes which fulfill the respective property are returned
     *             independent of <code>value</code>.
     * @return An ArrayList of {@link AccessibilityNodeInfo} which match.
     */
    private ArrayList<AccessibilityNodeInfo> findNodesByAttribute(AccessibilityNodeInfo node,
                                                                  String value,
                                                                  AccessibilityNodeAttribute attr) {
        ArrayList<String> values = new ArrayList<>();
        values.add(value);
        return findNodesByAttribute(node, values, attr);
    }

    /**
     * <p>Finds nodes defined by a list of Strings and an attribute. Searches <code>node</code> and its children.</p>
     * <code>Attr</code> defines which attribute of the node should be matched with all
     * <code>values</code>. If something matches, it is added to returned list.
     * @param node {@link AccessibilityNodeInfo}: The root node to search.
     * @param values A list of strings which should be matched with the text in the attribute.
     * @param attr The {@link AccessibilityNodeAttribute} of the node to match against. When searching
     *             for CLASS or TEXT, only nodes are returned which match at least of String of <code>values</code>.<br>
     *             For CLICKABLE and SCROLLABLE, all nodes which fulfill the respective property are returned
     *             independent of <code>values</code>.
     * @return An ArrayList of {@link AccessibilityNodeInfo} which match.
     */
    private ArrayList<AccessibilityNodeInfo> findNodesByAttribute(AccessibilityNodeInfo node,
                                                                  ArrayList<String> values,
                                                                  AccessibilityNodeAttribute attr) {
//        Log.d(LOGTAG, safeCharSeqToString(node.getClassName()));
        ArrayList<AccessibilityNodeInfo> nodes = new ArrayList<>();
        if (node == null) return nodes;
        for (String type : values) {
            switch (attr) {
                case CLASS: {
                    if (safeCharSeqToString(node.getClassName()) != null &&
                            safeCharSeqToString(node.getClassName()).equals(type)) {
                        nodes.add(node);
                    }
                    break;
                }
                case TEXT: {
                    if (safeCharSeqToString(node.getText()) != null &&
                            safeCharSeqToString(node.getText()).equals(type)) {
                        nodes.add(node);
                    }
                    break;
                }
                case CLICKABLE: {
                    if (node.isClickable()) {
                        nodes.add(node);
                    }
                    break;
                }
                case SCROLLABLE: {
                    if (node.isScrollable()) {
                        nodes.add(node);
                    }
                    break;
                }
            }

        }

        // Recursive search
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (child.isVisibleToUser()) {
                    nodes.addAll(findNodesByAttribute(child, values, attr));
//                    child.recycle();
                } else {
                    Log.d(LOGTAG, String.format("Skipping invisible child: %s", child.toString()));
                }
            } else {
                Log.d(LOGTAG, String.format("Null child %d/%d, parent: %s",
                        i, count, node.toString()));
            }
        }
//        Log.i(LOGTAG, "Returning " + nodes.toString());
        return nodes;
    }

    /**
     * If the text of the supplied node contains <code>match</code>,
     * fills it with <code>input</code>.
     * @param node {@link AccessibilityNodeInfo}: The node to search.
     * @param match A String which should be matched with the text in the node.
     * @param input The String which will be put in.
     * @return True if something was found, False otherwise.
     */
    private boolean trySetInput(AccessibilityNodeInfo node, String match, String input) {
        ArrayList<String> matches = new ArrayList<>();
        matches.add(match);
        return trySetInput(node, matches, input);
    }

    /**
     * If the text of the supplied node contains one item of <code>matches</code>,
     * fills it with <code>input</code>.
     * @param node {@link AccessibilityNodeInfo}: The node to search.
     * @param matches A list of strings which should be matched with the text in the node.
     * @param input The String which will be put in.
     * @return True if something was found, else False.
     */
    private boolean trySetInput(AccessibilityNodeInfo node, ArrayList<String> matches, String input) {
        for (String match : matches) {
//            if (node.getText() == null) continue;
            if (safeCharSeqToString(node.getText()).toLowerCase().contains(match)) {
                Log.i(LOGTAG, "Found something interesting: " + safeCharSeqToString(node.getText()));
                Log.i(LOGTAG, "Filling with: " + input);
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        input);
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

                Log.d(LOGTAG, "ResourceName: " + node.getViewIdResourceName());
                XMLdumperAction action = new XMLdumperAction("FillForm",
                        safeCharSeqToString(node.getViewIdResourceName()),
                        input);
                dumper.addAction(action);

                return true;
            }
        }
        return false;
    }

    /**
     * Clicks (toggles) a checkbox.
     * @param node An {@link AccessibilityNodeInfo} which is checkable, e.g. a Checkbox.
     */
    private void setCheckedCheckbox(AccessibilityNodeInfo node) {
        if (node.getText() != null) {
            Log.i(LOGTAG, "Toggling Checkbox " + safeCharSeqToString(node.getText()));
        }
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        Log.d(LOGTAG, "ResourceName: " + node.getViewIdResourceName());
        XMLdumperAction action = new XMLdumperAction("Checkbox",
                safeCharSeqToString(node.getViewIdResourceName()),
                "" + node.isChecked());
        dumper.addAction(action);
    }

    /**
     * Clicks a radiobutton.
     * @param node An {@link AccessibilityNodeInfo} which is checkable, e.g. a Checkbox.
     */
    private void setCheckedRadioButton(AccessibilityNodeInfo node) {
        if (node.getText() != null) {
            Log.i(LOGTAG, "Toggling radiobutton " + safeCharSeqToString(node.getText()));
        }
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        Log.d(LOGTAG, "ResourceName: " + node.getViewIdResourceName());
        XMLdumperAction action = new XMLdumperAction("Radiobutton",
                safeCharSeqToString(node.getViewIdResourceName()),
                "" + node.isChecked());
        dumper.addAction(action);
    }

    /**
     * <i><b>Deprecated.</b></i>
     * <p>Try to find login forms in <code>node</code> and its children.</p>
     * <p>Tries to find a edit field with text containing "user" or "mail" and enters the username
     * according to {@link Configuration}. If found, tries to find a field containing "pass" and fills
     * respectively. Then tries to find a button and, if found, clicks it.</p>
     * @param {@link AccessibilityNodeInfo}: The root node to search.
     * @return True if something was found, False otherwise.
     */
    private boolean findLoginForm(AccessibilityNodeInfo node) {
        Log.i(LOGTAG, safeCharSeqToString(node.getClassName()));
        String username = Configuration.getUsername();
        String password = Configuration.getPassword();
        ArrayList<String> classes = new ArrayList<>();
        classes.add("android.widget.EditText");
        classes.add("android.widget.Button");
        ArrayList<AccessibilityNodeInfo> nodes = findNodesByAttribute(node, classes, AccessibilityNodeAttribute.CLASS);

        int l = nodes.size();
        int i = 0;
        ArrayList<String> matches = new ArrayList<>();
        matches.add("user");
        matches.add("mail");
        for ( ; i < l; i++) {
            if (trySetInput(nodes.get(i), matches, username)) break;
        }
        for ( ; i < l; i++) {
            if (trySetInput(nodes.get(i), "pass", password)) break;
        }
        for ( ; i < l; i++) {
            AccessibilityNodeInfo n = nodes.get(i);
            if (n.getClassName().equals("android.widget.Button")) {
                Log.i(LOGTAG, "Found a button: " + n.getText());
                Log.i(LOGTAG, "Found something like a login form");

                Log.d(LOGTAG, "ResourceName: " + node.getViewIdResourceName());
                XMLdumperAction action = new XMLdumperAction("Click",
                        safeCharSeqToString(n.getViewIdResourceName()),
                        "");
                dumper.addAction(action);

                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        // If we can not find something, the login might be split into several windows
        // For this case, we try to find username or password input fields, but don't press anything
        // The username does not have to be set, as it was already set in the code above
        for (int j = 0 ; j < l; j++) {
            if (trySetInput(nodes.get(j), "pass", password)) break;
        }
        return false;
    }

    /**
     * <p>Inserts text into <code>node</code> and its children.</p>
     * <p>Into EditText nodes with a chance according to {@link Configuration#getText_input_chance()}.
     * Inputted text is supplied from {@link Configuration#getTextInput(AccessibilityNodeInfo)}</p>
     * <p>Does the same for checkboxes and radiobuttons with their chance respectively.</p>
     * @param node {@link AccessibilityNodeInfo}: The root node to search.
     */
    private void fillEditfields(AccessibilityNodeInfo node) {
        ArrayList<String> textClasses = new ArrayList<>();
        textClasses.add("android.widget.EditText");
        ArrayList<AccessibilityNodeInfo> texts = findNodesByAttribute(node, textClasses, AccessibilityNodeAttribute.CLASS);
        for (AccessibilityNodeInfo t : texts) {
            if (Configuration.getTextInputChance())
                trySetInput(t, "", Configuration.getTextInput(t));
        }

        ArrayList<AccessibilityNodeInfo> checkboxes = findNodesByAttribute(node, "android.widget.CheckBox", AccessibilityNodeAttribute.CLASS);
        for (AccessibilityNodeInfo c : checkboxes) {
            if (Configuration.getCheckboxChance())
                setCheckedCheckbox(c);
        }

        ArrayList<AccessibilityNodeInfo> radioButtons = findNodesByAttribute(node, "android.widget.CheckedTextView", AccessibilityNodeAttribute.CLASS);
        for (AccessibilityNodeInfo c : radioButtons) {
            if (Configuration.getRadioButtonChance())
                setCheckedRadioButton(c);
        }
    }

    /**
     * Searches a list of clickables for OAuth forms. Android supports global accounts for facebook,
     * google etc. ResourceNames are searched for values which indicate OAuth login buttons. If
     * something was found, it is clicked with the chance given in {@link Configuration#getOAuth_search_chance()}.
     * @param clickables A list of clickable {@link AccessibilityNodeInfo}s.
     * @return True if something was clicked, False otherwise.
     */
    private boolean findOAuthForms(ArrayList<AccessibilityNodeInfo> clickables) {
        if (Configuration.getOAuthSearchChance()) {
            for (AccessibilityNodeInfo clickable : clickables) {
                String resourceName = safeCharSeqToString(clickable.getViewIdResourceName());
                String resourceNameLow = resourceName.toLowerCase();
                if (resourceNameLow.contains("facebook") ||
                        resourceNameLow.toLowerCase().contains("google")) {
                    Log.d(LOGTAG, "Clicking OAuth form with ResourceID: " + safeCharSeqToString(clickable.getViewIdResourceName()));
                    XMLdumperAction action = new XMLdumperAction("Click",
                            safeCharSeqToString(clickable.getViewIdResourceName()),
                            "");
                    dumper.addAction(action);
                    dumper.writeEvent();

                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        } else {
            Log.d(LOGTAG, "Skipped searching for OAuth forms.");
        }
        return false;
    }

    /**
     * <p>Tries to find and click clickable {@link AccessibilityNodeInfo}s.</p>
     * <p>The list of clickables it derived by searching <code>node</code> and its children for clickables.
     * The back button is added to the list. A random clickable is clicked.</p>
     * <p>The back button is always clicked with the chance defined in
     * {@link Configuration#getBackbutton_press_chance()}.</p>
     * @param node {@link AccessibilityNodeInfo}: The root node to search.
     */
    private void findAndClickClickables(AccessibilityNodeInfo node) {
        final ArrayList<AccessibilityNodeInfo> clickables =
                findNodesByAttribute(node, "", AccessibilityNodeAttribute.CLICKABLE);

        if(findOAuthForms(clickables)) return;
        // +1 clickable option for back button
//        int clickableOptions = clickables.size() + 1;
        int i = 0;
        int limit = 1;

        while (i < limit) {
//            int randomNum = ThreadLocalRandom.current().nextInt(0, clickableOptions);
                // press the back button
                if (Configuration.getBackButtonPressChance()) {
                    XMLdumperAction action = new XMLdumperAction("Back",
                            "", "");
                    dumper.addAction(action);
                    dumper.writeEvent();
                    Log.i(LOGTAG, "Pressing back");
                    as.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                } else {
                    if (clickables.isEmpty()) continue;
                    int randomNum = random.nextInt(clickables.size());
                    Log.i(LOGTAG, "Pressing clickable with Text: " + clickables.get(randomNum).getText() +
                            " and ResourceId: " + clickables.get(randomNum).getViewIdResourceName());

                    XMLdumperAction action = new XMLdumperAction("Click",
                            safeCharSeqToString(clickables.get(randomNum).getViewIdResourceName()),
                            "");
                    dumper.addAction(action);
                    dumper.writeEvent();
                    clickables.get(randomNum).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
//            try {
//                Thread.sleep(50);
//            } catch (Exception e) {
//                Log.w(LOGTAG, "Interrupted in findAndClickClickables");
//            }
            i++;
        }
    }

    /**
     * <p>Tries to scroll on <code>node</code> and its children.</p>
     * <p>Only will scroll by chance defined in {@link Configuration#getScroll_chance()} and if
     * the node is scrollable. Has a 50% to scroll up or down respectively.</p>
     * @param node The root node to search.
     * @return True if an action was taken, False otherwise.
     */
    private boolean inputGestures(AccessibilityNodeInfo node) {
        if (!Configuration.getScrollChance()) return false;
        ArrayList<AccessibilityNodeInfo> scrollables =
                findNodesByAttribute(node, "", AccessibilityNodeAttribute.SCROLLABLE);
        if (scrollables.size() == 0) {
            Log.i(LOGTAG, "No scrollables found");
            return false;
        }
//        int randomNum = ThreadLocalRandom.current().nextInt(0, scrollables.size());
        int randomNum = random.nextInt(scrollables.size());
        AccessibilityNodeInfo nodeToScroll = scrollables.get(randomNum);
        Log.i(LOGTAG, "Scrolling node: " + safeCharSeqToString(nodeToScroll.getText()));
        if (Math.random() < 0.5) {
            XMLdumperAction action = new XMLdumperAction("Scroll",
                    safeCharSeqToString(node.getViewIdResourceName()),
                    "ACTION_SCROLL_BACKWARD");
            dumper.addAction(action);
            dumper.writeEvent();

            nodeToScroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        } else {
            XMLdumperAction action = new XMLdumperAction("Scroll",
                    safeCharSeqToString(node.getViewIdResourceName()),
                    "ACTION_SCROLL_FORWARD");
            dumper.addAction(action);
            dumper.writeEvent();

            nodeToScroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
        return true;
    }

    /**
     * <p>Checks for special cases:</p>
     * <p>Checks if the node comes from a App uninstall request. If so, dismiss it.</p>
     * <p>Checks if the node comes from a "Full screen notification" popup. If so, dismiss it.</p>
     * <p>Checks if the node comes from a "App not responding" popup. If so, dismiss it.</p>
     * <p>Checks if the node comes from a permission request. If so, allow it.</p>
     * <p>Checks if the node comes from the launcher. If so, launch the app.</p>
     * <p>Checks if the node matches our target app. If not, discard the it and press back.</p>
     * @param node The node to check.
     * @return <code>True</code> if a special case was found, <code>False</code> otherwise.
     */
    private boolean isSpecialCase(AccessibilityNodeInfo node) {
        // Checks for uninstall popup and dismisses it
        if (safeCharSeqToString(node.getPackageName()).equals("android")) {
            final ArrayList<AccessibilityNodeInfo> clickables =
                    findNodesByAttribute(node, "OK", AccessibilityNodeAttribute.TEXT);
            if (clickables.size() != 1) {
                // This means that this popup is not what we are looking for
                // just ignore it
                Log.w(LOGTAG, "Uninstall popup does not have exactly " +
                        "one 'OK' button (it is " + clickables.size() + ").");
            } else {
                XMLdumperAction action = new XMLdumperAction("Click",
                        safeCharSeqToString(clickables.get(0).getViewIdResourceName()), "");
                dumper.addAction(action);
                dumper.writeEvent();

                // Click the button
                clickables.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }
        // Checks for full screen notification popup and dismisses it
        if (safeCharSeqToString(node.getPackageName()).equals("android")) {
            final ArrayList<AccessibilityNodeInfo> clickables =
                    findNodesByAttribute(node, "Got it", AccessibilityNodeAttribute.TEXT);
            if (clickables.size() != 1) {
                // This means that this popup is not what we are looking for
                // just ignore it
                Log.w(LOGTAG, "Full screen notification popup does not have exactly " +
                        "one 'Got it' button (it is " + clickables.size() + ").");
            } else {
                XMLdumperAction action = new XMLdumperAction("Click",
                        safeCharSeqToString(clickables.get(0).getViewIdResourceName()), "");
                dumper.addAction(action);
                dumper.writeEvent();

                // Click the button
                clickables.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }
        // Checks for not responding popup and dismisses it
        if (safeCharSeqToString(node.getPackageName()).equals("android")) {
            final ArrayList<AccessibilityNodeInfo> clickables =
                    findNodesByAttribute(node, "Wait", AccessibilityNodeAttribute.TEXT);
            if (clickables.size() != 1) {
                // This means that this popup is not what we are looking for
                // just ignore it
                Log.w(LOGTAG, "'Not responding' Popup does not have exactly " +
                        "one Wait button (it is " + clickables.size() + ").");
            } else {
                XMLdumperAction action = new XMLdumperAction("Click",
                        safeCharSeqToString(clickables.get(0).getViewIdResourceName()), "");
                dumper.addAction(action);
                dumper.writeEvent();

                // Click the button
                clickables.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }
        // Checks for permission popup and automatically accepts
        if (safeCharSeqToString(node.getPackageName()).equals("com.android.packageinstaller")) {
            final ArrayList<AccessibilityNodeInfo> clickables =
                    findNodesByAttribute(node, "ALLOW", AccessibilityNodeAttribute.TEXT);
            if (clickables.size() != 1) {
                Log.w(LOGTAG, "Packageinstaller Popup does not have exactly " +
                        "one Allow button (it is " + clickables.size() + ").");
                return true;
            }

            XMLdumperAction action = new XMLdumperAction("Click",
                    safeCharSeqToString(clickables.get(0).getViewIdResourceName()), "");
            dumper.addAction(action);
            dumper.writeEvent();

            // Click the button
            clickables.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        // If the launcher is in foreground, launch the app
        if (safeCharSeqToString(node.getPackageName()).equals(Configuration.getLauncherPackageName())) {
            XMLdumperAction action = new XMLdumperAction("Launch",
                    Configuration.getPackageName(), "");
            dumper.addAction(action);
            dumper.writeEvent();

            as.launchApp();
            return true;
        }
        // Check whether the package is from the application to be monitored
        if (!safeCharSeqToString(node.getPackageName()).equals(Configuration.getPackageName())) {
            Log.d(LOGTAG, "Discarded package " + safeCharSeqToString(node.getPackageName()) + " as it " +
                    "does not equal " + Configuration.getPackageName());

            XMLdumperAction action = new XMLdumperAction("Back", "", "");
            dumper.addAction(action);
            dumper.writeEvent();

            as.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            return true;
        }
        return false;
    }

    /**
     * <p>Calls {@link EventInjector#isSpecialCase(AccessibilityNodeInfo)},
     * {@link EventInjector#fillEditfields(AccessibilityNodeInfo)},
     * {@link EventInjector#inputGestures(AccessibilityNodeInfo)},
     * {@link EventInjector#findAndClickClickables(AccessibilityNodeInfo)} with the supplied <code>node</code>.</p>
     * <p>If an action is taken which will most likely trigger a new AccessibilityEvent (e.g. clicking a button),
     * the method returns without calling all subroutines.</p>
     * @param node {@link AccessibilityNodeInfo}: The root node to search.
     */
    public void inject(AccessibilityNodeInfo node) {
        Log.d(LOGTAG, "Started EventInjector.inject");
        dumper = XMLdumper.getInstance();
        if(isSpecialCase(node)) return;
//        findLoginForm(node);
        fillEditfields(node);
        if (!inputGestures(node)) findAndClickClickables(node);
        Log.d(LOGTAG, "EventInjector.inject finished.");
    }
}
