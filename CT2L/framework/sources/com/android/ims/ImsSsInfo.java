package com.android.ims;

import android.os.Parcel;
import android.os.Parcelable;

public class ImsSsInfo implements Parcelable {
    public static final Parcelable.Creator<ImsSsInfo> CREATOR = new Parcelable.Creator<ImsSsInfo>() {
        @Override
        public ImsSsInfo createFromParcel(Parcel in) {
            return new ImsSsInfo(in);
        }

        @Override
        public ImsSsInfo[] newArray(int size) {
            return new ImsSsInfo[size];
        }
    };
    public static final int DISABLED = 0;
    public static final int ENABLED = 1;
    public static final int NOT_REGISTERED = -1;
    public int mStatus;

    public ImsSsInfo() {
    }

    public ImsSsInfo(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mStatus);
    }

    public String toString() {
        return super.toString() + ", Status: " + (this.mStatus == 0 ? "disabled" : "enabled");
    }

    private void readFromParcel(Parcel in) {
        this.mStatus = in.readInt();
    }
}
