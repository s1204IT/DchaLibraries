package com.android.internal.os;

import java.util.HashMap;

public class KernelWakelockStats extends HashMap<String, Entry> {
    int kernelWakelockVersion;

    public static class Entry {
        public int mCount;
        public long mTotalTime;
        public int mVersion;

        Entry(int i, long j, int i2) {
            this.mCount = i;
            this.mTotalTime = j;
            this.mVersion = i2;
        }
    }
}
