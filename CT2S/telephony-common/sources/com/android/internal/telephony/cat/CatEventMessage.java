package com.android.internal.telephony.cat;

public class CatEventMessage {
    private byte[] mAdditionalInfo;
    private int mDestId;
    private int mEvent;
    private boolean mOneShot;
    private int mSourceId;

    public CatEventMessage(int event, int sourceId, int destId, byte[] additionalInfo, boolean oneShot) {
        this.mEvent = 0;
        this.mSourceId = 130;
        this.mDestId = 129;
        this.mAdditionalInfo = null;
        this.mOneShot = false;
        this.mEvent = event;
        this.mSourceId = sourceId;
        this.mDestId = destId;
        this.mAdditionalInfo = additionalInfo;
        this.mOneShot = oneShot;
    }

    public CatEventMessage(int event, byte[] additionalInfo, boolean oneShot) {
        this.mEvent = 0;
        this.mSourceId = 130;
        this.mDestId = 129;
        this.mAdditionalInfo = null;
        this.mOneShot = false;
        this.mEvent = event;
        this.mAdditionalInfo = additionalInfo;
        this.mOneShot = oneShot;
    }

    public int getEvent() {
        return this.mEvent;
    }

    public int getSourceId() {
        return this.mSourceId;
    }

    public int getDestId() {
        return this.mDestId;
    }

    public byte[] getAdditionalInfo() {
        return this.mAdditionalInfo;
    }

    public boolean isOneShot() {
        return this.mOneShot;
    }
}
