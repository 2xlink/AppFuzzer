package com.example.link.appfuzzer.XMLdumper;

/**
 * A {@link XMLdumperAction} describes an action which was taken on a node, e.g. "Click on
 * a button with id X" or "Write 'text' into this field".
 */
public class XMLdumperAction {
    private String eventType;       // FillForm|Checkbox|RadioButton|Password|Click|Scroll|Back
    private String resourceID;      // Clicked resourceID
    private String value;           // Inserted value
    private int id = 0;             // id

    /**
     * Create a new {@link XMLdumperAction} using the supplied data.
     * @param eventType The event type. Either "Accessibility" or "Timer". Mandatory.
     * @param resourceID The resource ID.
     * @param value The inserted value for insertion actions.
     */
    public XMLdumperAction(String eventType, String resourceID, String value) {
        this.eventType = eventType;
        this.resourceID = resourceID;
        this.value = value;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getResourceID() {
        return resourceID;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
