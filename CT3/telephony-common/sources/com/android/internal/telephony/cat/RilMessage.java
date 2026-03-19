package com.android.internal.telephony.cat;

class RilMessage {
    Object mData;
    int mId;
    ResultCode mResCode;
    boolean mSetUpMenuFromMD;

    RilMessage(int msgId, String rawData) {
        this.mId = msgId;
        this.mData = rawData;
        this.mSetUpMenuFromMD = false;
    }

    RilMessage(RilMessage other) {
        this.mId = other.mId;
        this.mData = other.mData;
        this.mResCode = other.mResCode;
        this.mSetUpMenuFromMD = other.mSetUpMenuFromMD;
    }

    void setSetUpMenuFromMD(boolean flag) {
        this.mSetUpMenuFromMD = flag;
    }
}
