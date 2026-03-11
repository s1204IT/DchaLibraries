package com.android.settings.support;

import android.os.Parcel;
import android.os.Parcelable;

public final class SupportPhone implements Parcelable {
    public static final Parcelable.Creator<SupportPhone> CREATOR = new Parcelable.Creator<SupportPhone>() {
        @Override
        public SupportPhone createFromParcel(Parcel parcel) {
            return new SupportPhone(parcel);
        }

        @Override
        public SupportPhone[] newArray(int i) {
            return new SupportPhone[i];
        }
    };
    public final boolean isTollFree;
    public final String language;
    public final String number;

    protected SupportPhone(Parcel parcel) {
        this.language = parcel.readString();
        this.number = parcel.readString();
        this.isTollFree = parcel.readInt() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.language);
        parcel.writeString(this.number);
        parcel.writeInt(this.isTollFree ? 1 : 0);
    }
}
