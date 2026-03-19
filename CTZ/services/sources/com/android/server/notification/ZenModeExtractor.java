package com.android.server.notification;

import android.content.Context;
import android.util.Log;
import android.util.Slog;

public class ZenModeExtractor implements NotificationSignalExtractor {
    private ZenModeHelper mZenModeHelper;
    private static final String TAG = "ZenModeExtractor";
    private static final boolean DBG = Log.isLoggable(TAG, 3);

    @Override
    public void initialize(Context context, NotificationUsageStats notificationUsageStats) {
        if (DBG) {
            Slog.d(TAG, "Initializing  " + getClass().getSimpleName() + ".");
        }
    }

    @Override
    public RankingReconsideration process(NotificationRecord notificationRecord) {
        if (notificationRecord == null || notificationRecord.getNotification() == null) {
            if (DBG) {
                Slog.d(TAG, "skipping empty notification");
            }
            return null;
        }
        if (this.mZenModeHelper == null) {
            if (DBG) {
                Slog.d(TAG, "skipping - no zen info available");
            }
            return null;
        }
        notificationRecord.setIntercepted(this.mZenModeHelper.shouldIntercept(notificationRecord));
        if (notificationRecord.isIntercepted()) {
            notificationRecord.setSuppressedVisualEffects(this.mZenModeHelper.getNotificationPolicy().suppressedVisualEffects);
        } else {
            notificationRecord.setSuppressedVisualEffects(0);
        }
        return null;
    }

    @Override
    public void setConfig(RankingConfig rankingConfig) {
    }

    @Override
    public void setZenHelper(ZenModeHelper zenModeHelper) {
        this.mZenModeHelper = zenModeHelper;
    }
}
