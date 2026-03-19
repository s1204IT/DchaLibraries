package com.android.gallery3d.util;

public class RangeArray<T> {
    private T[] mData;
    private int mOffset;

    public RangeArray(int i, int i2) {
        this.mData = (T[]) new Object[(i2 - i) + 1];
        this.mOffset = i;
    }

    public void put(int i, T t) {
        this.mData[i - this.mOffset] = t;
    }

    public T get(int i) {
        return this.mData[i - this.mOffset];
    }
}
