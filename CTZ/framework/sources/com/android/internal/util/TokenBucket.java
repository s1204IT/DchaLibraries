package com.android.internal.util;

import android.os.SystemClock;

public class TokenBucket {
    private int mAvailable;
    private final int mCapacity;
    private final int mFillDelta;
    private long mLastFill;

    public TokenBucket(int i, int i2, int i3) {
        this.mFillDelta = Preconditions.checkArgumentPositive(i, "deltaMs must be strictly positive");
        this.mCapacity = Preconditions.checkArgumentPositive(i2, "capacity must be strictly positive");
        this.mAvailable = Math.min(Preconditions.checkArgumentNonnegative(i3), this.mCapacity);
        this.mLastFill = scaledTime();
    }

    public TokenBucket(int i, int i2) {
        this(i, i2, i2);
    }

    public void reset(int i) {
        Preconditions.checkArgumentNonnegative(i);
        this.mAvailable = Math.min(i, this.mCapacity);
        this.mLastFill = scaledTime();
    }

    public int capacity() {
        return this.mCapacity;
    }

    public int available() {
        fill();
        return this.mAvailable;
    }

    public boolean has() {
        fill();
        return this.mAvailable > 0;
    }

    public boolean get() {
        return get(1) == 1;
    }

    public int get(int i) {
        fill();
        if (i <= 0) {
            return 0;
        }
        if (i > this.mAvailable) {
            int i2 = this.mAvailable;
            this.mAvailable = 0;
            return i2;
        }
        this.mAvailable -= i;
        return i;
    }

    private void fill() {
        long jScaledTime = scaledTime();
        this.mAvailable = Math.min(this.mCapacity, this.mAvailable + ((int) (jScaledTime - this.mLastFill)));
        this.mLastFill = jScaledTime;
    }

    private long scaledTime() {
        return SystemClock.elapsedRealtime() / ((long) this.mFillDelta);
    }
}
