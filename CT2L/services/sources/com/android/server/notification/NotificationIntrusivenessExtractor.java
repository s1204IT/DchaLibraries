package com.android.server.notification;

import android.app.Notification;
import android.content.Context;

public class NotificationIntrusivenessExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final long HANG_TIME_MS = 10000;
    private static final String TAG = "NotificationNoiseExtractor";

    @Override
    public void initialize(Context ctx) {
    }

    @Override
    public RankingReconsideration process(NotificationRecord record) {
        if (record == null || record.getNotification() == null) {
            return null;
        }
        Notification notification = record.getNotification();
        if ((notification.defaults & 2) != 0 || notification.vibrate != null || (notification.defaults & 1) != 0 || notification.sound != null || notification.fullScreenIntent != null) {
            record.setRecentlyIntusive(true);
        }
        return new RankingReconsideration(record.getKey(), HANG_TIME_MS) {
            @Override
            public void work() {
            }

            @Override
            public void applyChangesLocked(NotificationRecord record2) {
                record2.setRecentlyIntusive(NotificationIntrusivenessExtractor.DBG);
            }
        };
    }

    @Override
    public void setConfig(RankingConfig config) {
    }
}
