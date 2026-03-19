package com.android.internal.textservice;

import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import java.util.function.IntUnaryOperator;

@VisibleForTesting
public final class LazyIntToIntMap {
    private final SparseIntArray mMap = new SparseIntArray();
    private final IntUnaryOperator mMappingFunction;

    public LazyIntToIntMap(IntUnaryOperator intUnaryOperator) {
        this.mMappingFunction = intUnaryOperator;
    }

    public void delete(int i) {
        this.mMap.delete(i);
    }

    public int get(int i) {
        int iIndexOfKey = this.mMap.indexOfKey(i);
        if (iIndexOfKey >= 0) {
            return this.mMap.valueAt(iIndexOfKey);
        }
        int iApplyAsInt = this.mMappingFunction.applyAsInt(i);
        this.mMap.append(i, iApplyAsInt);
        return iApplyAsInt;
    }
}
