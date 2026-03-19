package com.android.server.wm.utils;

import android.util.SparseArray;

public class RotationCache<T, R> {
    private final SparseArray<R> mCache = new SparseArray<>(4);
    private T mCachedFor;
    private final RotationDependentComputation<T, R> mComputation;

    @FunctionalInterface
    public interface RotationDependentComputation<T, R> {
        R compute(T t, int i);
    }

    public RotationCache(RotationDependentComputation<T, R> rotationDependentComputation) {
        this.mComputation = rotationDependentComputation;
    }

    public R getOrCompute(T t, int i) {
        if (t != this.mCachedFor) {
            this.mCache.clear();
            this.mCachedFor = t;
        }
        int iIndexOfKey = this.mCache.indexOfKey(i);
        if (iIndexOfKey >= 0) {
            return this.mCache.valueAt(iIndexOfKey);
        }
        R rCompute = this.mComputation.compute(t, i);
        this.mCache.put(i, rCompute);
        return rCompute;
    }
}
