package com.mediatek.internal.telephony;

public class MtkTimerRecord {
    public String address;
    public Object mTracker;
    public int msgCount;
    public int refNumber;

    public MtkTimerRecord(String str, int i, int i2, Object obj) {
        this.address = str;
        this.refNumber = i;
        this.msgCount = i2;
        this.mTracker = obj;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MtkTimerRecord)) {
            return false;
        }
        MtkTimerRecord mtkTimerRecord = (MtkTimerRecord) obj;
        return this.address.equals(mtkTimerRecord.address) && this.refNumber == mtkTimerRecord.refNumber;
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
