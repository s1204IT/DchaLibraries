package com.android.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class CallInfo implements Parcelable {
    public static final Parcelable.Creator<CallInfo> CREATOR = new Parcelable.Creator<CallInfo>() {
        @Override
        public CallInfo createFromParcel(Parcel parcel) {
            return new CallInfo(parcel.readString());
        }

        @Override
        public CallInfo[] newArray(int i) {
            return new CallInfo[i];
        }
    };
    private String handle;

    public CallInfo(String str) {
        this.handle = str;
    }

    public String getHandle() {
        return this.handle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.handle);
    }
}
