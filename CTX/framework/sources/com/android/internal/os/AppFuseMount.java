package com.android.internal.os;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

public class AppFuseMount implements Parcelable {
    public static final Parcelable.Creator<AppFuseMount> CREATOR = new Parcelable.Creator<AppFuseMount>() {
        @Override
        public AppFuseMount createFromParcel(Parcel parcel) {
            return new AppFuseMount(parcel.readInt(), (ParcelFileDescriptor) parcel.readParcelable(null));
        }

        @Override
        public AppFuseMount[] newArray(int i) {
            return new AppFuseMount[i];
        }
    };
    public final ParcelFileDescriptor fd;
    public final int mountPointId;

    public AppFuseMount(int i, ParcelFileDescriptor parcelFileDescriptor) {
        Preconditions.checkNotNull(parcelFileDescriptor);
        this.mountPointId = i;
        this.fd = parcelFileDescriptor;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mountPointId);
        parcel.writeParcelable(this.fd, i);
    }
}
