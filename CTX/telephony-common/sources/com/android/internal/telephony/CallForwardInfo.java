package com.android.internal.telephony;

import android.telecom.Log;

public class CallForwardInfo {
    private static final String TAG = "CallForwardInfo";
    public String number;
    public int reason;
    public int serviceClass;
    public int status;
    public int timeSeconds;
    public int toa;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[CallForwardInfo: status=");
        sb.append(this.status == 0 ? " not active " : " active ");
        sb.append(", reason= ");
        sb.append(this.reason);
        sb.append(", serviceClass= ");
        sb.append(this.serviceClass);
        sb.append(", timeSec= ");
        sb.append(this.timeSeconds);
        sb.append(" seconds, number=");
        sb.append(Log.pii(this.number));
        sb.append("]");
        return sb.toString();
    }
}
