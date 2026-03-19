package com.android.internal.location;

import android.os.Parcel;
import android.os.Parcelable;

public final class ProviderProperties implements Parcelable {
    public static final Parcelable.Creator<ProviderProperties> CREATOR = new Parcelable.Creator<ProviderProperties>() {
        @Override
        public ProviderProperties createFromParcel(Parcel parcel) {
            return new ProviderProperties(parcel.readInt() == 1, parcel.readInt() == 1, parcel.readInt() == 1, parcel.readInt() == 1, parcel.readInt() == 1, parcel.readInt() == 1, parcel.readInt() == 1, parcel.readInt(), parcel.readInt());
        }

        @Override
        public ProviderProperties[] newArray(int i) {
            return new ProviderProperties[i];
        }
    };
    public final int mAccuracy;
    public final boolean mHasMonetaryCost;
    public final int mPowerRequirement;
    public final boolean mRequiresCell;
    public final boolean mRequiresNetwork;
    public final boolean mRequiresSatellite;
    public final boolean mSupportsAltitude;
    public final boolean mSupportsBearing;
    public final boolean mSupportsSpeed;

    public ProviderProperties(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, int i, int i2) {
        this.mRequiresNetwork = z;
        this.mRequiresSatellite = z2;
        this.mRequiresCell = z3;
        this.mHasMonetaryCost = z4;
        this.mSupportsAltitude = z5;
        this.mSupportsSpeed = z6;
        this.mSupportsBearing = z7;
        this.mPowerRequirement = i;
        this.mAccuracy = i2;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRequiresNetwork ? 1 : 0);
        parcel.writeInt(this.mRequiresSatellite ? 1 : 0);
        parcel.writeInt(this.mRequiresCell ? 1 : 0);
        parcel.writeInt(this.mHasMonetaryCost ? 1 : 0);
        parcel.writeInt(this.mSupportsAltitude ? 1 : 0);
        parcel.writeInt(this.mSupportsSpeed ? 1 : 0);
        parcel.writeInt(this.mSupportsBearing ? 1 : 0);
        parcel.writeInt(this.mPowerRequirement);
        parcel.writeInt(this.mAccuracy);
    }
}
