package com.android.server.notification;

import android.app.Notification;
import android.content.Context;
import android.util.Log;
import android.util.Slog;

public class NotificationIntrusivenessExtractor implements NotificationSignalExtractor {
    private static final long HANG_TIME_MS = 10000;
    private static final String TAG = "IntrusivenessExtractor";
    private static final boolean DBG = Log.isLoggable(TAG, 3);

    @Override
    public void initialize(Context ctx, NotificationUsageStats usageStats) {
        if (DBG) {
            Slog.d(TAG, "Initializing  " + getClass().getSimpleName() + ".");
        }
    }

    @Override
    public RankingReconsideration process(NotificationRecord record) {
        if (record == null || record.getNotification() == null) {
            if (DBG) {
                Slog.d(TAG, "skipping empty notification");
            }
            return null;
        }
        if (record.getImportance() >= 3) {
            Notification notification = record.getNotification();
            if ((notification.defaults & 2) != 0 || notification.vibrate != null || (notification.defaults & 1) != 0 || notification.sound != null || notification.fullScreenIntent != null) {
                record.setRecentlyIntrusive(true);
            }
        }
        return new RankingReconsideration(record.getKey(), 10000L) {
            @Override
            public void work() {
            }

            @Override
            public void applyChangesLocked(NotificationRecord record2) {
                record2.setRecentlyIntrusive(false);
            }
        };
    }

    @Override
    public void setConfig(RankingConfig config) {
    }
}
