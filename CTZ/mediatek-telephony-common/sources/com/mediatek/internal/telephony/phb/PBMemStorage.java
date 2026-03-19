package com.mediatek.internal.telephony.phb;

import android.os.Parcel;

public class PBMemStorage {
    public static final int INT_NOT_SET = -1;
    public static final String STRING_NOT_SET = "";
    private String mStorage = "";
    private int mUsed = -1;
    private int mTotal = -1;

    public static PBMemStorage createFromParcel(Parcel parcel) {
        PBMemStorage pBMemStorage = new PBMemStorage();
        pBMemStorage.mStorage = parcel.readString();
        pBMemStorage.mUsed = parcel.readInt();
        pBMemStorage.mTotal = parcel.readInt();
        return pBMemStorage;
    }

    public String toString() {
        return super.toString() + ";storage: " + this.mStorage + ",used: " + this.mUsed + ",total:" + this.mTotal;
    }

    public void setStorage(String str) {
        this.mStorage = str;
    }

    public void setUsed(int i) {
        this.mUsed = i;
    }

    public void setTotal(int i) {
        this.mTotal = i;
    }

    public String getStorage() {
        return this.mStorage;
    }

    public int getUsed() {
        return this.mUsed;
    }

    public int getTotal() {
        return this.mTotal;
    }
}
