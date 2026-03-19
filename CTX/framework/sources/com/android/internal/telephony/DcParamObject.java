package com.android.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class DcParamObject implements Parcelable {
    public static final Parcelable.Creator<DcParamObject> CREATOR = new Parcelable.Creator<DcParamObject>() {
        @Override
        public DcParamObject createFromParcel(Parcel parcel) {
            return new DcParamObject(parcel);
        }

        @Override
        public DcParamObject[] newArray(int i) {
            return new DcParamObject[i];
        }
    };
    private int mSubId;

    public DcParamObject(int i) {
        this.mSubId = i;
    }

    public DcParamObject(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSubId);
    }

    private void readFromParcel(Parcel parcel) {
        this.mSubId = parcel.readInt();
    }

    public int getSubId() {
        return this.mSubId;
    }
}
