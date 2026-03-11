package com.android.launcher3.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

public class ParcelableSparseArray extends SparseArray<Parcelable> implements Parcelable {
    public static final Parcelable.Creator<ParcelableSparseArray> CREATOR = new Parcelable.Creator<ParcelableSparseArray>() {
        @Override
        public ParcelableSparseArray createFromParcel(Parcel parcel) {
            ParcelableSparseArray parcelableSparseArray = new ParcelableSparseArray();
            ClassLoader classLoader = parcelableSparseArray.getClass().getClassLoader();
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                parcelableSparseArray.put(parcel.readInt(), parcel.readParcelable(classLoader));
            }
            return parcelableSparseArray;
        }

        @Override
        public ParcelableSparseArray[] newArray(int i) {
            return new ParcelableSparseArray[i];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        int size = size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            parcel.writeInt(keyAt(i2));
            parcel.writeParcelable(valueAt(i2), 0);
        }
    }
}
