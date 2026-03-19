package com.android.server.backup.utils;

import android.util.SparseArray;
import java.util.HashSet;

public final class SparseArrayUtils {
    private SparseArrayUtils() {
    }

    public static <V> HashSet<V> union(SparseArray<HashSet<V>> sparseArray) {
        HashSet<V> hashSet = new HashSet<>();
        int size = sparseArray.size();
        for (int i = 0; i < size; i++) {
            HashSet<V> hashSetValueAt = sparseArray.valueAt(i);
            if (hashSetValueAt != null) {
                hashSet.addAll(hashSetValueAt);
            }
        }
        return hashSet;
    }
}
