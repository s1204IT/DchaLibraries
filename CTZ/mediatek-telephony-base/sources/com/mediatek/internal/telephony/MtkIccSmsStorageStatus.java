package com.mediatek.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class MtkIccSmsStorageStatus implements Parcelable {
    public static final Parcelable.Creator<MtkIccSmsStorageStatus> CREATOR = new Parcelable.Creator<MtkIccSmsStorageStatus>() {
        @Override
        public MtkIccSmsStorageStatus createFromParcel(Parcel parcel) {
            return new MtkIccSmsStorageStatus(parcel.readInt(), parcel.readInt());
        }

        @Override
        public MtkIccSmsStorageStatus[] newArray(int i) {
            return new MtkIccSmsStorageStatus[i];
        }
    };
    public int mTotal;
    public int mUsed;

    public MtkIccSmsStorageStatus() {
        this.mUsed = 0;
        this.mTotal = 0;
    }

    public MtkIccSmsStorageStatus(int i, int i2) {
        this.mUsed = i;
        this.mTotal = i2;
    }

    public int getUsedCount() {
        return this.mUsed;
    }

    public int getTotalCount() {
        return this.mTotal;
    }

    public int getUnused() {
        return this.mTotal - this.mUsed;
    }

    public void reset() {
        this.mUsed = 0;
        this.mTotal = 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mUsed);
        parcel.writeInt(this.mTotal);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(50);
        sb.append("[");
        sb.append(this.mUsed);
        sb.append(", ");
        sb.append(this.mTotal);
        sb.append("]");
        return sb.toString();
    }
}
