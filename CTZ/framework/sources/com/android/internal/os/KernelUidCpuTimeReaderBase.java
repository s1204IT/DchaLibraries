package com.android.internal.os;

import android.os.SystemClock;
import com.android.internal.os.KernelUidCpuTimeReaderBase.Callback;

public abstract class KernelUidCpuTimeReaderBase<T extends Callback> {
    protected static final boolean DEBUG = false;
    private static final long DEFAULT_THROTTLE_INTERVAL = 10000;
    private final String TAG = getClass().getSimpleName();
    private long mLastTimeReadMs = Long.MIN_VALUE;
    private long mThrottleInterval = 10000;

    public interface Callback {
    }

    protected abstract void readDeltaImpl(T t);

    public void readDelta(T t) {
        if (SystemClock.elapsedRealtime() < this.mLastTimeReadMs + this.mThrottleInterval) {
            return;
        }
        readDeltaImpl(t);
        this.mLastTimeReadMs = SystemClock.elapsedRealtime();
    }

    public void setThrottleInterval(long j) {
        if (j >= 0) {
            this.mThrottleInterval = j;
        }
    }
}
