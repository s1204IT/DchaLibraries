package com.android.bips.jni;

import android.os.Parcel;
import android.os.Parcelable;

public class SizeD implements Parcelable {
    public static final Parcelable.Creator<SizeD> CREATOR = new Parcelable.Creator<SizeD>() {
        @Override
        public SizeD createFromParcel(Parcel parcel) {
            return new SizeD(parcel);
        }

        @Override
        public SizeD[] newArray(int i) {
            return new SizeD[i];
        }
    };
    private final double mHeight;
    private final double mWidth;

    public SizeD(double d, double d2) {
        validate("width", d);
        validate("height", d2);
        this.mWidth = d;
        this.mHeight = d2;
    }

    private void validate(String str, double d) {
        if (d < 0.0d || !Double.isFinite(d)) {
            throw new IllegalArgumentException("invalid " + str + ": " + d);
        }
    }

    public SizeD(Parcel parcel) {
        this(parcel.readDouble(), parcel.readDouble());
    }

    public double getWidth() {
        return this.mWidth;
    }

    public double getHeight() {
        return this.mHeight;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(this.mWidth);
        parcel.writeDouble(this.mHeight);
    }
}
