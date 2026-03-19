package com.android.internal.telephony.cat;

public class RilMessage {
    public Object mData;
    public int mId;
    public ResultCode mResCode;

    public RilMessage(int i, String str) {
        this.mId = i;
        this.mData = str;
    }

    public RilMessage(RilMessage rilMessage) {
        this.mId = rilMessage.mId;
        this.mData = rilMessage.mData;
        this.mResCode = rilMessage.mResCode;
    }
}
