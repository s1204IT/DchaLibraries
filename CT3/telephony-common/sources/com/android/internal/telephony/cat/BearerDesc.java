package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class BearerDesc implements Parcelable {
    public int bearerType = 0;

    @Override
    public abstract void writeToParcel(Parcel parcel, int i);

    @Override
    public int describeContents() {
        return 0;
    }
}
