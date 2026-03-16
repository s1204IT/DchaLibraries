package com.android.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class DcParamObject implements Parcelable {
    public static final Parcelable.Creator<DcParamObject> CREATOR = new Parcelable.Creator<DcParamObject>() {
        @Override
        public DcParamObject createFromParcel(Parcel in) {
            return new DcParamObject(in);
        }

        @Override
        public DcParamObject[] newArray(int size) {
            return new DcParamObject[size];
        }
    };
    private int mSubId;

    public DcParamObject(int subId) {
        this.mSubId = subId;
    }

    public DcParamObject(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mSubId);
    }

    private void readFromParcel(Parcel in) {
        this.mSubId = in.readInt();
    }

    public int getSubId() {
        return this.mSubId;
    }
}
