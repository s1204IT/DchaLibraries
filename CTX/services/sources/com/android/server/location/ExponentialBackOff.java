package com.android.server.location;

class ExponentialBackOff {
    private static final int MULTIPLIER = 2;
    private long mCurrentIntervalMillis;
    private final long mInitIntervalMillis;
    private final long mMaxIntervalMillis;

    ExponentialBackOff(long j, long j2) {
        this.mInitIntervalMillis = j;
        this.mMaxIntervalMillis = j2;
        this.mCurrentIntervalMillis = this.mInitIntervalMillis / 2;
    }

    long nextBackoffMillis() {
        if (this.mCurrentIntervalMillis > this.mMaxIntervalMillis) {
            return this.mMaxIntervalMillis;
        }
        this.mCurrentIntervalMillis *= 2;
        return this.mCurrentIntervalMillis;
    }

    void reset() {
        this.mCurrentIntervalMillis = this.mInitIntervalMillis / 2;
    }
}
