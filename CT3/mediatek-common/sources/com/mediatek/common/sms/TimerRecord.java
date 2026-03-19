package com.mediatek.common.sms;

public class TimerRecord {
    public String address;
    public Object mTracker;
    public int msgCount;
    public int refNumber;

    public TimerRecord(String address, int refNumber, int msgCount, Object tracker) {
        this.address = address;
        this.refNumber = refNumber;
        this.msgCount = msgCount;
        this.mTracker = tracker;
    }

    public boolean equals(Object obj) {
        return obj != 0 && (obj instanceof TimerRecord) && this.address.equals(obj.address) && this.refNumber == obj.refNumber;
    }

    public int hashCode() {
        return (this.refNumber * 31) + this.address.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("TimerRecord: ");
        sb.append("address = ");
        sb.append(this.address);
        sb.append(", refNumber = ");
        sb.append(this.refNumber);
        sb.append(", msgCount = ");
        sb.append(this.msgCount);
        return sb.toString();
    }
}
