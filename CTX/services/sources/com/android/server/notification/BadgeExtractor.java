package com.android.server.notification;

import android.content.Context;

public class BadgeExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "BadgeExtractor";
    private RankingConfig mConfig;

    @Override
    public void initialize(Context context, NotificationUsageStats notificationUsageStats) {
    }

    @Override
    public RankingReconsideration process(NotificationRecord notificationRecord) {
        if (notificationRecord == null || notificationRecord.getNotification() == null || this.mConfig == null) {
            return null;
        }
        boolean zBadgingEnabled = this.mConfig.badgingEnabled(notificationRecord.sbn.getUser());
        boolean zCanShowBadge = this.mConfig.canShowBadge(notificationRecord.sbn.getPackageName(), notificationRecord.sbn.getUid());
        if (!zBadgingEnabled || !zCanShowBadge) {
            notificationRecord.setShowBadge(false);
        } else if (notificationRecord.getChannel() != null) {
            notificationRecord.setShowBadge(notificationRecord.getChannel().canShowBadge() && zCanShowBadge);
        } else {
            notificationRecord.setShowBadge(zCanShowBadge);
        }
        if (notificationRecord.isIntercepted() && (notificationRecord.getSuppressedVisualEffects() & 64) != 0) {
            notificationRecord.setShowBadge(false);
        }
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
