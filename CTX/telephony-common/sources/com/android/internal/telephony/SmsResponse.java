package com.android.internal.telephony;

public class SmsResponse {
    String mAckPdu;
    public int mErrorCode;
    public int mMessageRef;

    public SmsResponse(int i, String str, int i2) {
        this.mMessageRef = i;
        this.mAckPdu = str;
        this.mErrorCode = i2;
    }

    public String toString() {
        return "{ mMessageRef = " + this.mMessageRef + ", mErrorCode = " + this.mErrorCode + ", mAckPdu = " + this.mAckPdu + "}";
    }
}
