package com.android.bluetoothmidiservice;

public class MidiBtleTimeTracker {
    private long mBaseHostTimeNanos;
    private long mPreviousNow;
    private long mPreviousResult;
    private int mPreviousTimestamp;
    private int mWindowMillis = 20;
    private long mWindowNanos = ((long) this.mWindowMillis) * 1000000;
    private long mPeripheralTimeMillis = 0;

    public MidiBtleTimeTracker(long j) {
        this.mBaseHostTimeNanos = j;
        this.mPreviousNow = j;
    }

    public long convertTimestampToNanotime(int i, long j) {
        long j2 = i - this.mPreviousTimestamp;
        if (j2 < 0) {
            j2 += 8192;
        }
        this.mPeripheralTimeMillis += j2;
        if (j - this.mPreviousNow > 4096000000L) {
            long j3 = ((j - this.mBaseHostTimeNanos) - 4096000000L) / 1000000;
            while (this.mPeripheralTimeMillis < j3) {
                this.mPeripheralTimeMillis += 8192;
            }
        }
        long j4 = (this.mPeripheralTimeMillis * 1000000) + this.mBaseHostTimeNanos;
        if (j4 > j) {
            this.mPeripheralTimeMillis = 0L;
            this.mBaseHostTimeNanos = j;
            j4 = j;
        } else {
            long j5 = j - this.mWindowNanos;
            if (j4 < j5) {
                this.mPeripheralTimeMillis = 0L;
                this.mBaseHostTimeNanos = j5;
                j4 = j5;
            }
        }
        if (j4 < this.mPreviousResult) {
            j4 = this.mPreviousResult;
        }
        this.mPreviousResult = j4;
        this.mPreviousTimestamp = i;
        this.mPreviousNow = j;
        return j4;
    }
}
