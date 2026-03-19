package com.android.internal.util;

import java.lang.reflect.Array;
import java.util.Arrays;

public class RingBuffer<T> {
    private final T[] mBuffer;
    private long mCursor = 0;

    public RingBuffer(Class<T> cls, int i) {
        Preconditions.checkArgumentPositive(i, "A RingBuffer cannot have 0 capacity");
        this.mBuffer = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, i));
    }

    public int size() {
        return (int) Math.min(this.mBuffer.length, this.mCursor);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void clear() {
        for (int i = 0; i < size(); i++) {
            this.mBuffer[i] = null;
        }
        this.mCursor = 0L;
    }

    public void append(T t) {
        T[] tArr = this.mBuffer;
        long j = this.mCursor;
        this.mCursor = 1 + j;
        tArr[indexOf(j)] = t;
    }

    public T getNextSlot() {
        long j = this.mCursor;
        this.mCursor = 1 + j;
        int iIndexOf = indexOf(j);
        if (this.mBuffer[iIndexOf] == null) {
            this.mBuffer[iIndexOf] = createNewItem();
        }
        return this.mBuffer[iIndexOf];
    }

    protected T createNewItem() {
        try {
            return (T) this.mBuffer.getClass().getComponentType().newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            return null;
        }
    }

    public T[] toArray() {
        T[] tArr = (T[]) Arrays.copyOf(this.mBuffer, size(), this.mBuffer.getClass());
        long j = this.mCursor - 1;
        int length = tArr.length - 1;
        while (length >= 0) {
            tArr[length] = this.mBuffer[indexOf(j)];
            length--;
            j--;
        }
        return tArr;
    }

    private int indexOf(long j) {
        return (int) Math.abs(j % ((long) this.mBuffer.length));
    }
}
