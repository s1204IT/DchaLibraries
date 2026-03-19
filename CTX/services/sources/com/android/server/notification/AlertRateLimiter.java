package com.android.server.notification;

public class AlertRateLimiter {
    static final long ALLOWED_ALERT_INTERVAL = 1000;
    private long mLastNotificationMillis = 0;

    boolean shouldRateLimitAlert(long j) {
        long j2 = j - this.mLastNotificationMillis;
        if (j2 < 0 || j2 < 1000) {
            return true;
        }
        this.mLastNotificationMillis = j;
        return false;
    }
}
