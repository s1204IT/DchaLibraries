package com.android.launcher3.util;

import android.util.LongSparseArray;
import java.util.Iterator;

public class LongArrayMap<E> extends LongSparseArray<E> implements Iterable<E> {
    public boolean containsKey(long j) {
        return indexOfKey(j) >= 0;
    }

    public boolean isEmpty() {
        return size() <= 0;
    }

    @Override
    public LongArrayMap<E> clone() {
        return (LongArrayMap) super.clone();
    }

    @Override
    public Iterator<E> iterator() {
        return new ValueIterator();
    }

    class ValueIterator implements Iterator<E> {
        private int mNextIndex = 0;

        ValueIterator() {
        }

        @Override
        public boolean hasNext() {
            return this.mNextIndex < LongArrayMap.this.size();
        }

        @Override
        public E next() {
            LongArrayMap longArrayMap = LongArrayMap.this;
            int i = this.mNextIndex;
            this.mNextIndex = i + 1;
            return longArrayMap.valueAt(i);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
