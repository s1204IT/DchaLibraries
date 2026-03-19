package com.android.bluetooth.hfp;

class HeadsetClccResponse extends HeadsetMessageObject {
    int mDirection;
    int mIndex;
    int mMode;
    boolean mMpty;
    String mNumber;
    int mStatus;
    int mType;

    HeadsetClccResponse(int i, int i2, int i3, int i4, boolean z, String str, int i5) {
        this.mIndex = i;
        this.mDirection = i2;
        this.mStatus = i3;
        this.mMode = i4;
        this.mMpty = z;
        this.mNumber = str;
        this.mType = i5;
    }

    @Override
    public void buildString(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append(getClass().getSimpleName());
        sb.append("[index=");
        sb.append(this.mIndex);
        sb.append(", direction=");
        sb.append(this.mDirection);
        sb.append(", status=");
        sb.append(this.mStatus);
        sb.append(", callMode=");
        sb.append(this.mMode);
        sb.append(", isMultiParty=");
        sb.append(this.mMpty);
        sb.append(", number=");
        if (this.mNumber == null) {
            sb.append("null");
        } else {
            sb.append("***");
        }
        sb.append(", type=");
        sb.append(this.mType);
        sb.append("]");
    }
}
