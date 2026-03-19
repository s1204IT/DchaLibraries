package com.android.bluetooth.hfp;

import java.util.Objects;

class HeadsetCallState extends HeadsetMessageObject {
    int mCallState;
    int mNumActive;
    int mNumHeld;
    String mNumber;
    int mType;

    HeadsetCallState(int i, int i2, int i3, String str, int i4) {
        this.mNumActive = i;
        this.mNumHeld = i2;
        this.mCallState = i3;
        this.mNumber = str;
        this.mType = i4;
    }

    @Override
    public void buildString(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append(getClass().getSimpleName());
        sb.append("[numActive=");
        sb.append(this.mNumActive);
        sb.append(", numHeld=");
        sb.append(this.mNumHeld);
        sb.append(", callState=");
        sb.append(this.mCallState);
        sb.append(", number=");
        if (this.mNumber == null) {
            sb.append("null");
        } else {
            sb.append("***");
        }
        sb.append(this.mNumber);
        sb.append(", type=");
        sb.append(this.mType);
        sb.append("]");
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HeadsetCallState)) {
            return false;
        }
        HeadsetCallState headsetCallState = (HeadsetCallState) obj;
        return this.mNumActive == headsetCallState.mNumActive && this.mNumHeld == headsetCallState.mNumHeld && this.mCallState == headsetCallState.mCallState && Objects.equals(this.mNumber, headsetCallState.mNumber) && this.mType == headsetCallState.mType;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mNumActive), Integer.valueOf(this.mNumHeld), Integer.valueOf(this.mCallState), this.mNumber, Integer.valueOf(this.mType));
    }
}
