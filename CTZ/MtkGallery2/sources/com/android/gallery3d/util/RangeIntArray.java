package com.android.gallery3d.util;

public class RangeIntArray {
    private int[] mData;
    private int mOffset;

    public RangeIntArray(int[] iArr, int i, int i2) {
        this.mData = iArr;
        this.mOffset = i;
    }

    public int get(int i) {
        return this.mData[i - this.mOffset];
    }
}
