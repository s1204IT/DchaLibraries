package android.icu.impl;

import dalvik.bytecode.Opcodes;

public class CalendarCache {
    private static final int[] primes = {61, 127, 509, 1021, 2039, 4093, Opcodes.OP_SPUT_BYTE_JUMBO, 16381, 32749, 65521, 131071, 262139};
    public static long EMPTY = Long.MIN_VALUE;
    private int pIndex = 0;
    private int size = 0;
    private int arraySize = primes[this.pIndex];
    private int threshold = (this.arraySize * 3) / 4;
    private long[] keys = new long[this.arraySize];
    private long[] values = new long[this.arraySize];

    public CalendarCache() {
        makeArrays(this.arraySize);
    }

    private void makeArrays(int i) {
        this.keys = new long[i];
        this.values = new long[i];
        for (int i2 = 0; i2 < i; i2++) {
            this.values[i2] = EMPTY;
        }
        this.arraySize = i;
        this.threshold = (int) (((double) this.arraySize) * 0.75d);
        this.size = 0;
    }

    public synchronized long get(long j) {
        return this.values[findIndex(j)];
    }

    public synchronized void put(long j, long j2) {
        if (this.size >= this.threshold) {
            rehash();
        }
        int iFindIndex = findIndex(j);
        this.keys[iFindIndex] = j;
        this.values[iFindIndex] = j2;
        this.size++;
    }

    private final int findIndex(long j) {
        int iHash = hash(j);
        int iHash2 = 0;
        while (this.values[iHash] != EMPTY && this.keys[iHash] != j) {
            if (iHash2 == 0) {
                iHash2 = hash2(j);
            }
            iHash = (iHash + iHash2) % this.arraySize;
        }
        return iHash;
    }

    private void rehash() {
        int i = this.arraySize;
        long[] jArr = this.keys;
        long[] jArr2 = this.values;
        if (this.pIndex < primes.length - 1) {
            int[] iArr = primes;
            int i2 = this.pIndex + 1;
            this.pIndex = i2;
            this.arraySize = iArr[i2];
        } else {
            this.arraySize = (this.arraySize * 2) + 1;
        }
        this.size = 0;
        makeArrays(this.arraySize);
        for (int i3 = 0; i3 < i; i3++) {
            if (jArr2[i3] != EMPTY) {
                put(jArr[i3], jArr2[i3]);
            }
        }
    }

    private final int hash(long j) {
        int i = (int) (((j * 15821) + 1) % ((long) this.arraySize));
        if (i < 0) {
            return i + this.arraySize;
        }
        return i;
    }

    private final int hash2(long j) {
        return (this.arraySize - 2) - ((int) (j % ((long) (this.arraySize - 2))));
    }
}
