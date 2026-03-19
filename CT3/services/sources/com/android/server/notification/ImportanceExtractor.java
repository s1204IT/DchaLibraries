package com.android.server.notification;

import android.content.Context;

public class ImportanceExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "ImportantTopicExtractor";
    private RankingConfig mConfig;

    @Override
    public void initialize(Context ctx, NotificationUsageStats usageStats) {
    }

    @Override
    public RankingReconsideration process(NotificationRecord record) {
        if (record == null || record.getNotification() == null || this.mConfig == null) {
            return null;
        }
        record.setUserImportance(this.mConfig.getImportance(record.sbn.getPackageName(), record.sbn.getUid()));
        return null;
    }

    @Override
    public void setConfig(RankingConfig config) {
        this.mConfig = config;
    }
}
