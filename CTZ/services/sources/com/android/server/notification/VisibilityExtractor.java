package com.android.server.notification;

import android.content.Context;

public class VisibilityExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "VisibilityExtractor";
    private RankingConfig mConfig;

    @Override
    public void initialize(Context context, NotificationUsageStats notificationUsageStats) {
    }

    @Override
    public RankingReconsideration process(NotificationRecord notificationRecord) {
        if (notificationRecord == null || notificationRecord.getNotification() == null || this.mConfig == null) {
            return null;
        }
        notificationRecord.setPackageVisibilityOverride(notificationRecord.getChannel().getLockscreenVisibility());
        return null;
    }

    @Override
    public void setConfig(RankingConfig rankingConfig) {
        this.mConfig = rankingConfig;
    }

    @Override
    public void setZenHelper(ZenModeHelper zenModeHelper) {
    }
}
