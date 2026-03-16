package android.net.http;

import android.os.SystemClock;

class Timer {
    private long mLast;
    private long mStart;

    public Timer() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.mLast = jUptimeMillis;
        this.mStart = jUptimeMillis;
    }

    public void mark(String message) {
        long now = SystemClock.uptimeMillis();
        this.mLast = now;
    }
}
