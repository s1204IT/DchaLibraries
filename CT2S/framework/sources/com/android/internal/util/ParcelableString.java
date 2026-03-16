package com.android.internal.util;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableString implements Parcelable {
    public static final Parcelable.Creator<ParcelableString> CREATOR = new Parcelable.Creator<ParcelableString>() {
        @Override
        public ParcelableString createFromParcel(Parcel in) {
            ParcelableString ret = new ParcelableString();
            ret.string = in.readString();
            return ret;
        }

        @Override
        public ParcelableString[] newArray(int size) {
            return new ParcelableString[size];
        }
    };
    public String string;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.string);
    }
}
