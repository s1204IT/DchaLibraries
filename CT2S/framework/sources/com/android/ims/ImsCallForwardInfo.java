package com.android.ims;

import android.os.Parcel;
import android.os.Parcelable;

public class ImsCallForwardInfo implements Parcelable {
    public static final Parcelable.Creator<ImsCallForwardInfo> CREATOR = new Parcelable.Creator<ImsCallForwardInfo>() {
        @Override
        public ImsCallForwardInfo createFromParcel(Parcel in) {
            return new ImsCallForwardInfo(in);
        }

        @Override
        public ImsCallForwardInfo[] newArray(int size) {
            return new ImsCallForwardInfo[size];
        }
    };
    public int mCondition;
    public String mNumber;
    public int mStatus;
    public int mTimeSeconds;
    public int mToA;

    public ImsCallForwardInfo() {
    }

    public ImsCallForwardInfo(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mCondition);
        out.writeInt(this.mStatus);
        out.writeInt(this.mToA);
        out.writeString(this.mNumber);
        out.writeInt(this.mTimeSeconds);
    }

    public String toString() {
        return super.toString() + ", Condition: " + this.mCondition + ", Status: " + (this.mStatus == 0 ? "disabled" : "enabled") + ", ToA: " + this.mToA + ", Number=" + this.mNumber + ", Time (seconds): " + this.mTimeSeconds;
    }

    private void readFromParcel(Parcel in) {
        this.mCondition = in.readInt();
        this.mStatus = in.readInt();
        this.mToA = in.readInt();
        this.mNumber = in.readString();
        this.mTimeSeconds = in.readInt();
    }
}
