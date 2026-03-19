package com.android.internal.telephony;

public class LinkCapacityEstimate {
    public static final int INVALID = -1;
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_SUSPENDED = 1;
    public final int confidence;
    public final int downlinkCapacityKbps;
    public final int status;
    public final int uplinkCapacityKbps;

    public LinkCapacityEstimate(int i, int i2, int i3) {
        this.downlinkCapacityKbps = i;
        this.confidence = i2;
        this.status = i3;
        this.uplinkCapacityKbps = -1;
    }

    public LinkCapacityEstimate(int i, int i2) {
        this.downlinkCapacityKbps = i;
        this.uplinkCapacityKbps = i2;
        this.confidence = -1;
        this.status = -1;
    }

    public String toString() {
        return "{downlinkCapacityKbps=" + this.downlinkCapacityKbps + ", uplinkCapacityKbps=" + this.uplinkCapacityKbps + ", confidence=" + this.confidence + ", status=" + this.status;
    }
}
