package com.android.deskclock.data;

public final class Lap {
    private final long mAccumulatedTime;
    private final int mLapNumber;
    private final long mLapTime;

    Lap(int i, long j, long j2) {
        this.mLapNumber = i;
        this.mLapTime = j;
        this.mAccumulatedTime = j2;
    }

    public int getLapNumber() {
        return this.mLapNumber;
    }

    public long getLapTime() {
        return this.mLapTime;
    }

    public long getAccumulatedTime() {
        return this.mAccumulatedTime;
    }
}
