package com.android.settingslib;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseBooleanArray;

/* loaded from: classes.dex */
public class AppItem implements Parcelable, Comparable<AppItem> {
    public static final Parcelable.Creator<AppItem> CREATOR = new Parcelable.Creator<AppItem>() { // from class: com.android.settingslib.AppItem.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AppItem createFromParcel(Parcel parcel) {
            return new AppItem(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AppItem[] newArray(int i) {
            return new AppItem[i];
        }
    };
    public int category;
    public final int key;
    public boolean restricted;
    public long total;
    public SparseBooleanArray uids;

    public AppItem() {
        this.uids = new SparseBooleanArray();
        this.key = 0;
    }

    public AppItem(int i) {
        this.uids = new SparseBooleanArray();
        this.key = i;
    }

    public AppItem(Parcel parcel) {
        this.uids = new SparseBooleanArray();
        this.key = parcel.readInt();
        this.uids = parcel.readSparseBooleanArray();
        this.total = parcel.readLong();
    }

    public void addUid(int i) {
        this.uids.put(i, true);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.key);
        parcel.writeSparseBooleanArray(this.uids);
        parcel.writeLong(this.total);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    /* JADX DEBUG: Method merged with bridge method: compareTo(Ljava/lang/Object;)I */
    @Override // java.lang.Comparable
    public int compareTo(AppItem appItem) {
        int iCompare = Integer.compare(this.category, appItem.category);
        if (iCompare == 0) {
            return Long.compare(appItem.total, this.total);
        }
        return iCompare;
    }
}
