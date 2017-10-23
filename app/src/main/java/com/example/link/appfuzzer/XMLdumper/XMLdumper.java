package com.example.link.appfuzzer.XMLdumper;


import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.link.appfuzzer.XMLTransformations;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * <p>Takes care of writing the log files. Log files are created in <code>/data/data/com.example.link.appfuzzer/files/</code>
 * and are named <code>[package_name][current_set]</code>. There is one log file for each set.</p>
 * <p>The workflow for using this class is:
 * <ol>
 *     <li>Call {@link XMLdumper#startFile(File)} with your desired file to write to. This will initialize
 *     the file.</li>
 *     <li>Set properties using the various setter methods.</li>
 *     <li>Call {@link XMLdumper#writeEvent()}. This will write your set properties to the file and reinitialize them.</li>
 *     <li>Call {@link XMLdumper#endFile()}. This closes the file.</li>
 * </ol>
 * </p>
 * <p>It is possible to call {@link XMLdumper#startFile(File)} when a file is already being processed
 * and {@link XMLdumper#endFile()} when a file is already closed. The methods will print warnings and quit.</p>
 */
public class XMLdumper {

    private String source = "";                 // Timer|Accessibility
    private int eventId = 0;                    // event eventId
    private AccessibilityNodeInfo root;         // the node
    private ArrayList<XMLdumperAction> actions = new ArrayList<>(); // A list of actions which were conducted

    private static XMLdumper instance;
    private FileWriter writer;
    private XmlSerializer serializer;
    private StringWriter stringWriter;
    private boolean isRunning = false;

    private static String LOGTAG = "XMLdumper";

    private XMLdumper() {
    }

    /**
     * Returns the instance.
     * @return The instance.
     */
    public static XMLdumper getInstance() {
        if (instance == null) {
            instance = new XMLdumper();
        }
        return instance;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        Log.v(LOGTAG, "setSource called, old: " + this.source + "; new: " + source);
        if (!(this.source != null && source == null))
            this.source = source;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public AccessibilityNodeInfo getRoot() {
        return root;
    }

    public void setRoot(AccessibilityNodeInfo root) {
        this.root = root;
    }

    /**
     * <p>Adds a {@link XMLdumperAction} to an event.</p>
     * @param action The action which was taken.
     */
    public void addAction(XMLdumperAction action) {
        // Increment eventId
        if (!actions.isEmpty()) {
            action.setId(actions.get(actions.size() - 1).getId() + 1);
        }
        actions.add(action);
    }

    public void setActions(ArrayList<XMLdumperAction> actions) {
        this.actions = actions;
    }

    public ArrayList<XMLdumperAction> getActions() {
        return actions;
    }

    /**
     * Initializes a new file. Does nothing if a file is already being processed.
     * @param dumpFile The file to write to.
     */
    public void startFile(File dumpFile) {
        if (isRunning) {
            Log.w(LOGTAG, "startFile called, but a file is already being processed");
            return;
        }
        Log.v(LOGTAG, "startFile called");
        isRunning = true;
        try {
            File baseDir = new File(Environment.getDataDirectory(), "local");
            if (!baseDir.exists()) {
                baseDir.mkdir();
                baseDir.setExecutable(true, false);
                baseDir.setWritable(true, false);
                baseDir.setReadable(true, false);
            }
            writer = new FileWriter(dumpFile);
            serializer = Xml.newSerializer();
            stringWriter = new StringWriter();
            serializer.setOutput(stringWriter);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "events");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the current file.
     */
    public void endFile() {
        if (!isRunning) {
            Log.w(LOGTAG, "endFile called, but there is no file to end");
            return;
        }
        Log.v(LOGTAG, "endFile called");
        isRunning = false;
        if (writer != null) {
            try {
                serializer.endTag("", "events");
                serializer.endDocument();
                writer.write(stringWriter.toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * <p>Writes an event to the file using the supplied data. After writing, the set properties
     * are reinitialized.</p>
     */
    public void writeEvent() {
        Log.v(LOGTAG, "writeEvent called with " +
                MessageFormat.format("{0}, {1}, {2}", getSource(), getEventId(), actions.get(0).getEventType()));
        try {
            // This gets called after every action is taken
            // incremental until new set
            // then a new file
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").format(new java.util.Date());

            serializer.startTag("", "event");
                serializer.attribute("", "timestamp", timeStamp);
                serializer.attribute("", "source", getSource());
                serializer.attribute("", "eventId", "" + getEventId());
                serializer.startTag("", "content");
                    dumpNodeRec(root, serializer, 0);
                serializer.endTag("", "content");
                serializer.startTag("", "actions");
                    for (XMLdumperAction action : actions) {
                        serializer.startTag("", "action");
                            serializer.attribute("", "eventType", action.getEventType());
                            serializer.attribute("", "actionId", "" + action.getId());
                            serializer.startTag("", "resourceID");
                            serializer.text(action.getResourceID());
                            serializer.endTag("", "resourceID");
                            serializer.startTag("", "value");
                            serializer.text(action.getValue());
                            serializer.endTag("", "value");
                        serializer.endTag("", "action");
                    }
                serializer.endTag("", "actions");
            serializer.endTag("", "event");

            // Clean up and increment eventId
            Log.d(LOGTAG, "Cleaning up.");
            setEventId(getEventId() + 1);
            setSource("");
            setRoot(null);
            setActions(new ArrayList<XMLdumperAction>());
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to dump event", e);
        }
    }

    /**
     * Dumps a node in its XML representation using an XMLSerializer.
     * @param node
     * @param serializer
     * @param index
     * @throws IOException
     */
    private static void dumpNodeRec(AccessibilityNodeInfo node, XmlSerializer serializer,
                                   int index) throws IOException {
        serializer.startTag("", "node");
        serializer.attribute("", "index", Integer.toString(index));
        serializer.attribute("", "text", safeCharSeqToString(node.getText()));
        serializer.attribute("", "resource-eventId", safeCharSeqToString(node.getViewIdResourceName()));
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
                node, 0, 0).toShortString()); // width and height set to 0, as the are hopyfully not used anyway
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (child.isVisibleToUser()) {
//                if (true) {
                    dumpNodeRec(child, serializer, i);
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

    private static String safeCharSeqToString(CharSequence cs) {
        return XMLTransformations.safeCharSeqToString(cs);
    }
}
