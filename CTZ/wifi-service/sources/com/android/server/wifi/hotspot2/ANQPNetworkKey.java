package com.android.server.wifi.hotspot2;

import android.text.TextUtils;

public class ANQPNetworkKey {
    private final int mAnqpDomainID;
    private final long mBSSID;
    private final long mHESSID;
    private final String mSSID;

    public ANQPNetworkKey(String str, long j, long j2, int i) {
        this.mSSID = str;
        this.mBSSID = j;
        this.mHESSID = j2;
        this.mAnqpDomainID = i;
    }

    public static ANQPNetworkKey buildKey(String str, long j, long j2, int i) {
        if (i == 0) {
            return new ANQPNetworkKey(str, j, 0L, 0);
        }
        if (j2 != 0) {
            return new ANQPNetworkKey(null, 0L, j2, i);
        }
        return new ANQPNetworkKey(str, 0L, 0L, i);
    }

    public int hashCode() {
        if (this.mHESSID != 0) {
            return (int) (((((this.mHESSID >>> 32) * 31) + this.mHESSID) * 31) + ((long) this.mAnqpDomainID));
        }
        if (this.mBSSID != 0) {
            return (int) (((((long) (this.mSSID.hashCode() * 31)) + (this.mBSSID >>> 32)) * 31) + this.mBSSID);
        }
        return (this.mSSID.hashCode() * 31) + this.mAnqpDomainID;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ANQPNetworkKey)) {
            return false;
        }
        ANQPNetworkKey aNQPNetworkKey = (ANQPNetworkKey) obj;
        return TextUtils.equals(aNQPNetworkKey.mSSID, this.mSSID) && aNQPNetworkKey.mBSSID == this.mBSSID && aNQPNetworkKey.mHESSID == this.mHESSID && aNQPNetworkKey.mAnqpDomainID == this.mAnqpDomainID;
    }

    public String toString() {
        if (this.mHESSID != 0) {
            return Utils.macToString(this.mHESSID) + ":" + this.mAnqpDomainID;
        }
        if (this.mBSSID != 0) {
            return Utils.macToString(this.mBSSID) + ":<" + this.mSSID + ">";
        }
        return "<" + this.mSSID + ">:" + this.mAnqpDomainID;
    }
}
