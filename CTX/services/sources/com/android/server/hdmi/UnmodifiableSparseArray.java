package com.android.server.hdmi;

import android.util.SparseArray;

final class UnmodifiableSparseArray<E> {
    private static final String TAG = "ImmutableSparseArray";
    private final SparseArray<E> mArray;

    public UnmodifiableSparseArray(SparseArray<E> sparseArray) {
        this.mArray = sparseArray;
    }

    public int size() {
        return this.mArray.size();
    }

    public E get(int i) {
        return this.mArray.get(i);
    }

    public E get(int i, E e) {
        return this.mArray.get(i, e);
    }

    public int keyAt(int i) {
        return this.mArray.keyAt(i);
    }

    public E valueAt(int i) {
        return this.mArray.valueAt(i);
    }

    public int indexOfValue(E e) {
        return this.mArray.indexOfValue(e);
    }

    public String toString() {
        return this.mArray.toString();
    }
}
