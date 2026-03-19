package com.android.contacts.logging;

import com.google.common.base.MoreObjects;

public class EditorEvent {
    public int eventType;
    public int numberRawContacts;

    public String toString() {
        return MoreObjects.toStringHelper(this).add("eventType", this.eventType).add("numberRawContacts", this.numberRawContacts).toString();
    }
}
