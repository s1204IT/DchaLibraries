package com.mediatek.ims;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public class MtkImsCallForwardInfo implements Parcelable {
    public static final Parcelable.Creator<MtkImsCallForwardInfo> CREATOR = new Parcelable.Creator<MtkImsCallForwardInfo>() {
        @Override
        public MtkImsCallForwardInfo createFromParcel(Parcel parcel) {
            return new MtkImsCallForwardInfo(parcel);
        }

        @Override
        public MtkImsCallForwardInfo[] newArray(int i) {
            return new MtkImsCallForwardInfo[i];
        }
    };
    public int mCondition;
    public String mNumber;
    public int mServiceClass;
    public int mStatus;
    public int mTimeSeconds;
    public long[] mTimeSlot;
    public int mToA;

    public MtkImsCallForwardInfo() {
    }

    public MtkImsCallForwardInfo(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mCondition);
        parcel.writeInt(this.mStatus);
        parcel.writeInt(this.mServiceClass);
        parcel.writeInt(this.mToA);
        parcel.writeString(this.mNumber);
        parcel.writeInt(this.mTimeSeconds);
        parcel.writeLongArray(this.mTimeSlot);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", Condition: ");
        sb.append(this.mCondition);
        sb.append(", Status: ");
        sb.append(this.mStatus == 0 ? "disabled" : "enabled");
        sb.append(", ServiceClass: ");
        sb.append(this.mServiceClass);
        sb.append(", ToA: ");
        sb.append(this.mToA);
        sb.append(", Number=");
        sb.append(this.mNumber);
        sb.append(", Time (seconds): ");
        sb.append(this.mTimeSeconds);
        sb.append(", timeSlot: ");
        sb.append(Arrays.toString(this.mTimeSlot));
        return sb.toString();
    }

    private void readFromParcel(Parcel parcel) {
        this.mCondition = parcel.readInt();
        this.mStatus = parcel.readInt();
        this.mServiceClass = parcel.readInt();
        this.mToA = parcel.readInt();
        this.mNumber = parcel.readString();
        this.mTimeSeconds = parcel.readInt();
        this.mTimeSlot = new long[2];
        try {
            parcel.readLongArray(this.mTimeSlot);
        } catch (RuntimeException e) {
            this.mTimeSlot = null;
        }
    }
}
