package com.mediatek.mmsdk;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class BinderHolder implements Parcelable {
    public static final Parcelable.Creator<BinderHolder> CREATOR = new Parcelable.Creator<BinderHolder>() {
        @Override
        public BinderHolder createFromParcel(Parcel parcel) {
            return new BinderHolder(parcel);
        }

        @Override
        public BinderHolder[] newArray(int i) {
            return new BinderHolder[i];
        }
    };
    private IBinder mBinder;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(this.mBinder);
    }

    public void readFromParcel(Parcel parcel) {
        this.mBinder = parcel.readStrongBinder();
    }

    public IBinder getBinder() {
        return this.mBinder;
    }

    public void setBinder(IBinder iBinder) {
        this.mBinder = iBinder;
    }

    public BinderHolder() {
        this.mBinder = null;
    }

    public BinderHolder(IBinder iBinder) {
        this.mBinder = null;
        this.mBinder = iBinder;
    }

    private BinderHolder(Parcel parcel) {
        this.mBinder = null;
        this.mBinder = parcel.readStrongBinder();
    }
}
