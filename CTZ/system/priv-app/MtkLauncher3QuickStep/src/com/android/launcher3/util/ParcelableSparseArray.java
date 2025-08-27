package com.android.launcher3.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

/* loaded from: classes.dex */
public class ParcelableSparseArray extends SparseArray<Parcelable> implements Parcelable {
    public static final Parcelable.Creator<ParcelableSparseArray> CREATOR = new Parcelable.Creator<ParcelableSparseArray>() { // from class: com.android.launcher3.util.ParcelableSparseArray.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelableSparseArray createFromParcel(Parcel parcel) {
            ParcelableSparseArray parcelableSparseArray = new ParcelableSparseArray();
            ClassLoader classLoader = parcelableSparseArray.getClass().getClassLoader();
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                parcelableSparseArray.put(parcel.readInt(), parcel.readParcelable(classLoader));
            }
            return parcelableSparseArray;
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelableSparseArray[] newArray(int i) {
            return new ParcelableSparseArray[i];
        }
    };

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        int size = size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            parcel.writeInt(keyAt(i2));
            parcel.writeParcelable(valueAt(i2), 0);
        }
    }
}
