package com.mediatek.server.am.AutoBootControl;

public class ReceiverRecord {
    public boolean enabled;
    public final String packageName;

    public ReceiverRecord(String _packageName) {
        this.enabled = true;
        this.packageName = _packageName;
        this.enabled = true;
    }

    public ReceiverRecord(String _packageName, boolean _enable) {
        this.enabled = true;
        this.packageName = _packageName;
        this.enabled = _enable;
    }

    public ReceiverRecord(ReceiverRecord data) {
        this.enabled = true;
        this.packageName = data.packageName;
        this.enabled = data.enabled;
    }

    public String toString() {
        return "ReceiverRecord {" + this.packageName + ", " + this.enabled + "}";
    }
}
