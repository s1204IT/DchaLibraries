package com.android.server.hdmi;

import android.util.SparseIntArray;

final class UnmodifiableSparseIntArray {
    private static final String TAG = "ImmutableSparseIntArray";
    private final SparseIntArray mArray;

    public UnmodifiableSparseIntArray(SparseIntArray sparseIntArray) {
        this.mArray = sparseIntArray;
    }

    public int size() {
        return this.mArray.size();
    }

    public int get(int i) {
        return this.mArray.get(i);
    }

    public int get(int i, int i2) {
        return this.mArray.get(i, i2);
    }

    public int keyAt(int i) {
        return this.mArray.keyAt(i);
    }

    public int valueAt(int i) {
        return this.mArray.valueAt(i);
    }

    public int indexOfValue(int i) {
        return this.mArray.indexOfValue(i);
    }

    public String toString() {
        return this.mArray.toString();
    }
}
