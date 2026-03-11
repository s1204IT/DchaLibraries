package com.android.quicksearchbox;

import android.os.SystemClock;

public class LatencyTracker {
    private long mStartTime = SystemClock.uptimeMillis();

    public int getLatency() {
        long now = SystemClock.uptimeMillis();
        return (int) (now - this.mStartTime);
    }
}
