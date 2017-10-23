package com.example.link.appfuzzer.XMLdumper;

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
        import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;

        import com.example.link.appfuzzer.XMLTransformations;

        import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

/**
 *
 * @hide
 */
public class AccessibilityNodeInfoDumper {
    private static final String LOGTAG = AccessibilityNodeInfoDumper.class.getSimpleName();
    private static final String[] NAF_EXCLUDED_CLASSES = new String[] {
            android.widget.GridView.class.getName(), android.widget.GridLayout.class.getName(),
            android.widget.ListView.class.getName(), android.widget.TableLayout.class.getName()
    };

    private static String safeCharSeqToString(CharSequence cs) {
        return XMLTransformations.safeCharSeqToString(cs);
    }

    /**
     * Using {@link AccessibilityNodeInfo} this method will walk the layout hierarchy
     * and generates an xml dump into the /data/local/window_dump.xml
     * @param root The root accessibility node.
     * @param rotation The rotaion of current display
     * @param width The pixel width of current display
     * @param height The pixel height of current display
     */
    public static void dumpWindowToFile(AccessibilityNodeInfo root, int rotation,
                                        int width, int height) {
        File baseDir = new File(Environment.getDataDirectory(), "local");
        if (!baseDir.exists()) {
            baseDir.mkdir();
            baseDir.setExecutable(true, false);
            baseDir.setWritable(true, false);
            baseDir.setReadable(true, false);
        }
        dumpWindowToFile(root,
                new File(new File(Environment.getDataDirectory(), "local"), "window_dump.xml"),
                rotation, width, height);
    }
    /**
     * Using {@link AccessibilityNodeInfo} this method will walk the layout hierarchy
     * and generates an xml dump to the location specified by <code>dumpFile</code>
     * @param root The root accessibility node.
     * @param dumpFile The file to dump to.
     * @param rotation The rotaion of current display
     * @param width The pixel width of current display
     * @param height The pixel height of current display
     */
    public static void dumpWindowToFile(AccessibilityNodeInfo root, File dumpFile, int rotation,
                                        int width, int height) {
        if (root == null) {
            return;
        }
        final long startTime = SystemClock.uptimeMillis();
        try {
            FileWriter writer = new FileWriter(dumpFile);
            XmlSerializer serializer = Xml.newSerializer();
            StringWriter stringWriter = new StringWriter();
            serializer.setOutput(stringWriter);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "hierarchy");
            serializer.attribute("", "rotation", Integer.toString(rotation));
            dumpNodeRec(root, serializer, 0, width, height);
            serializer.endTag("", "hierarchy");
            serializer.endDocument();
            writer.write(stringWriter.toString());
            writer.close();
        } catch (IOException e) {
            Log.e(LOGTAG, "failed to dump window to file", e);
        }
        final long endTime = SystemClock.uptimeMillis();
        Log.d(LOGTAG, "Fetch time: " + (endTime - startTime) + "ms");
    }

    /**
     * Using {@link AccessibilityNodeInfo} this method will walk the layout hierarchy
     * and returns an xml object.
     * @param root The root accessibility node.
     * @param rotation The rotation of current display
     * @param width The pixel width of current display
     * @param height The pixel height of current display
     */
    public static AccessibilityNodeInfo dumpWindow(AccessibilityNodeInfo root, File dumpFile, int rotation,
                                        int width, int height) {
        if (root == null) {
            return null;
        }
        final long startTime = SystemClock.uptimeMillis();
        try {
            XmlSerializer serializer = Xml.newSerializer();
            StringWriter stringWriter = new StringWriter();
            serializer.setOutput(stringWriter);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "hierarchy");
            serializer.attribute("", "rotation", Integer.toString(rotation));
            dumpNodeRec2(root, serializer, 0, width, height);
            serializer.endTag("", "hierarchy");
            serializer.endDocument();
        } catch (IOException e) {
            Log.e(LOGTAG, "failed to dump window", e);
        }
        final long endTime = SystemClock.uptimeMillis();
        Log.d(LOGTAG, "Fetch time: " + (endTime - startTime) + "ms");

        return null;
    }

    private static void dumpNodeRec2(AccessibilityNodeInfo node, XmlSerializer serializer,int index,
                                    int width, int height) throws IOException {
        serializer.startTag("", "node");
        if (!nafExcludedClass(node) && !nafCheck(node))
            serializer.attribute("", "NAF", Boolean.toString(true));
        serializer.attribute("", "index", Integer.toString(index));
        serializer.attribute("", "text", safeCharSeqToString(node.getText()));
        serializer.attribute("", "resource-id", safeCharSeqToString(node.getViewIdResourceName()));
        serializer.attribute("", "class", safeCharSeqToString(node.getClassName()));
        serializer.attribute("", "package", safeCharSeqToString(node.getPackageName()));
        serializer.attribute("", "content-desc", safeCharSeqToString(node.getContentDescription()));
        serializer.attribute("", "checkable", Boolean.toString(node.isCheckable()));
        serializer.attribute("", "checked", Boolean.toString(node.isChecked()));
        serializer.attribute("", "clickable", Boolean.toString(node.isClickable()));
        serializer.attribute("", "enabled", Boolean.toString(node.isEnabled()));
        serializer.attribute("", "focusable", Boolean.toString(node.isFocusable()));
        serializer.attribute("", "focused", Boolean.toString(node.isFocused()));
        serializer.attribute("", "scrollable", Boolean.toString(node.isScrollable()));
        serializer.attribute("", "long-clickable", Boolean.toString(node.isLongClickable()));
        serializer.attribute("", "password", Boolean.toString(node.isPassword()));
        serializer.attribute("", "selected", Boolean.toString(node.isSelected()));
        serializer.attribute("", "bounds", AccessibilityNodeInfoHelper.getVisibleBoundsInScreen(
                node, width, height).toShortString());
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (child.isVisibleToUser()) {
//                if (true) {
                    dumpNodeRec2(child, serializer, i, width, height);
                    child.recycle();
                } else {
                    Log.d(LOGTAG, String.format("Skipping invisible child: %s", child.toString()));
                }
            } else {
                Log.d(LOGTAG, String.format("Null child %d/%d, parent: %s",
                        i, count, node.toString()));
            }
        }
        serializer.endTag("", "node");
    }



    public static void dumpNodeRec(AccessibilityNodeInfo node, XmlSerializer serializer,int index,
                                    int width, int height) throws IOException {
        serializer.startTag("", "node");
        if (!nafExcludedClass(node) && !nafCheck(node))
            serializer.attribute("", "NAF", Boolean.toString(true));
        serializer.attribute("", "index", Integer.toString(index));
        serializer.attribute("", "text", safeCharSeqToString(node.getText()));
        serializer.attribute("", "resource-id", safeCharSeqToString(node.getViewIdResourceName()));
        serializer.attribute("", "class", safeCharSeqToString(node.getClassName()));
        serializer.attribute("", "package", safeCharSeqToString(node.getPackageName()));
        serializer.attribute("", "content-desc", safeCharSeqToString(node.getContentDescription()));
        serializer.attribute("", "checkable", Boolean.toString(node.isCheckable()));
        serializer.attribute("", "checked", Boolean.toString(node.isChecked()));
        serializer.attribute("", "clickable", Boolean.toString(node.isClickable()));
        serializer.attribute("", "enabled", Boolean.toString(node.isEnabled()));
        serializer.attribute("", "focusable", Boolean.toString(node.isFocusable()));
        serializer.attribute("", "focused", Boolean.toString(node.isFocused()));
        serializer.attribute("", "scrollable", Boolean.toString(node.isScrollable()));
        serializer.attribute("", "long-clickable", Boolean.toString(node.isLongClickable()));
        serializer.attribute("", "password", Boolean.toString(node.isPassword()));
        serializer.attribute("", "selected", Boolean.toString(node.isSelected()));
        serializer.attribute("", "bounds", AccessibilityNodeInfoHelper.getVisibleBoundsInScreen(
                node, width, height).toShortString());
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (child.isVisibleToUser()) {
//                if (true) {
                    dumpNodeRec(child, serializer, i, width, height);
                    child.recycle();
                } else {
                    Log.d(LOGTAG, String.format("Skipping invisible child: %s", child.toString()));
                }
            } else {
                Log.d(LOGTAG, String.format("Null child %d/%d, parent: %s",
                        i, count, node.toString()));
            }
        }
        serializer.endTag("", "node");
    }
    /**
     * The list of classes to exclude my not be complete. We're attempting to
     * only reduce noise from standard layout classes that may be falsely
     * configured to accept clicks and are also enabled.
     *
     * @param node
     * @return true if node is excluded.
     */
    private static boolean nafExcludedClass(AccessibilityNodeInfo node) {
        String className = safeCharSeqToString(node.getClassName());
        for(String excludedClassName : NAF_EXCLUDED_CLASSES) {
            if(className.endsWith(excludedClassName))
                return true;
        }
        return false;
    }
    /**
     * We're looking for UI controls that are enabled, clickable but have no
     * text nor content-description. Such controls configuration indicate an
     * interactive control is present in the UI and is most likely not
     * accessibility friendly. We refer to such controls here as NAF controls
     * (Not Accessibility Friendly)
     *
     * @param node
     * @return false if a node fails the check, true if all is OK
     */
    private static boolean nafCheck(AccessibilityNodeInfo node) {
        boolean isNaf = node.isClickable() && node.isEnabled()
                && safeCharSeqToString(node.getContentDescription()).isEmpty()
                && safeCharSeqToString(node.getText()).isEmpty();
        if (!isNaf)
            return true;
        // check children since sometimes the containing element is clickable
        // and NAF but a child's text or description is available. Will assume
        // such layout as fine.
        return childNafCheck(node);
    }
    /**
     * This should be used when it's already determined that the node is NAF and
     * a further check of its children is in order. A node maybe a container
     * such as LinerLayout and may be set to be clickable but have no text or
     * content description but it is counting on one of its children to fulfill
     * the requirement for being accessibility friendly by having one or more of
     * its children fill the text or content-description. Such a combination is
     * considered by this dumper as acceptable for accessibility.
     *
     * @param node
     * @return false if node fails the check.
     */
    private static boolean childNafCheck(AccessibilityNodeInfo node) {
        int childCount = node.getChildCount();
        for (int x = 0; x < childCount; x++) {
            AccessibilityNodeInfo childNode = node.getChild(x);
            if (childNode == null) {
                Log.d(LOGTAG, String.format("Null child %d/%d, parent: %s",
                        x, childCount, node.toString()));
                continue;
            }
            if (!safeCharSeqToString(childNode.getContentDescription()).isEmpty()
                    || !safeCharSeqToString(childNode.getText()).isEmpty())
                return true;
            if (childNafCheck(childNode))
                return true;
        }
        return false;
    }

}