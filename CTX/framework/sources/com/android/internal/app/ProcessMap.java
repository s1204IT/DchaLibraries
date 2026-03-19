package com.android.internal.app;

import android.util.ArrayMap;
import android.util.SparseArray;

public class ProcessMap<E> {
    final ArrayMap<String, SparseArray<E>> mMap = new ArrayMap<>();

    public E get(String str, int i) {
        SparseArray<E> sparseArray = this.mMap.get(str);
        if (sparseArray == null) {
            return null;
        }
        return sparseArray.get(i);
    }

    public E put(String str, int i, E e) {
        SparseArray<E> sparseArray = this.mMap.get(str);
        if (sparseArray == null) {
            sparseArray = new SparseArray<>(2);
            this.mMap.put(str, sparseArray);
        }
        sparseArray.put(i, e);
        return e;
    }

    public E remove(String str, int i) {
        SparseArray<E> sparseArray = this.mMap.get(str);
        if (sparseArray != null) {
            E eRemoveReturnOld = sparseArray.removeReturnOld(i);
            if (sparseArray.size() == 0) {
                this.mMap.remove(str);
            }
            return eRemoveReturnOld;
        }
        return null;
    }

    public ArrayMap<String, SparseArray<E>> getMap() {
        return this.mMap;
    }

    public int size() {
        return this.mMap.size();
    }
}
