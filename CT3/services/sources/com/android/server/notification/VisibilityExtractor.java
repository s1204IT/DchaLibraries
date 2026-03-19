package com.android.server.notification;

import android.content.Context;

public class VisibilityExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "VisibilityExtractor";
    private RankingConfig mConfig;

    @Override
    public void initialize(Context ctx, NotificationUsageStats usageStats) {
    }

    @Override
    public RankingReconsideration process(NotificationRecord record) {
        if (record == null || record.getNotification() == null || this.mConfig == null) {
            return null;
        }
        record.setPackageVisibilityOverride(this.mConfig.getVisibilityOverride(record.sbn.getPackageName(), record.sbn.getUid()));
        return null;
    }

    @Override
    public void setConfig(RankingConfig config) {
        this.mConfig = config;
    }
}
