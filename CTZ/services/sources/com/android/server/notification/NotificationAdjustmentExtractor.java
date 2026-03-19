package com.android.server.notification;

import android.content.Context;

public class NotificationAdjustmentExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "AdjustmentExtractor";

    @Override
    public void initialize(Context context, NotificationUsageStats notificationUsageStats) {
    }

    @Override
    public RankingReconsideration process(NotificationRecord notificationRecord) {
        if (notificationRecord == null || notificationRecord.getNotification() == null) {
            return null;
        }
        notificationRecord.applyAdjustments();
        return null;
    }

    @Override
    public void setConfig(RankingConfig rankingConfig) {
    }

    @Override
    public void setZenHelper(ZenModeHelper zenModeHelper) {
    }
}
