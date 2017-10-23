package com.example.link.appfuzzer;

/**
 * Created by link on 23.02.17.
 */

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

/**
 * <p>Holds a list of enums which are used to specify the nodes to search in
 * {@link EventInjector#findNodesByAttribute(AccessibilityNodeInfo, String, AccessibilityNodeAttribute)} and
 * {@link EventInjector#findNodesByAttribute(AccessibilityNodeInfo, ArrayList, AccessibilityNodeAttribute)}.</p>
 *
 * <p>Supports <b>CLASS</b>: Searches for the {@link AccessibilityNodeInfo#getClassName()} attribute.<br>
 * Supports <b>TEXT</b>: Searches for the {@link AccessibilityNodeInfo#getText()} attribute.<br>
 * Supports <b>CLICKABLE</b>: Searches for the {@link AccessibilityNodeInfo#isClickable()} attribute.<br>
 * Supports <b>SCROLLABLE</b>: Searches for the {@link AccessibilityNodeInfo#isScrollable()} attribute.<br>
 *     </p>
 */
public enum AccessibilityNodeAttribute {
    CLASS, TEXT, CLICKABLE, SCROLLABLE;
}
