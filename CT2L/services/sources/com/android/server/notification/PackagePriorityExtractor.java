package com.android.server.notification;

import android.content.Context;

public class PackagePriorityExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "ImportantPackageExtractor";
    private RankingConfig mConfig;

    @Override
    public void initialize(Context ctx) {
    }

    @Override
    public RankingReconsideration process(NotificationRecord record) {
        if (record != null && record.getNotification() != null && this.mConfig != null) {
            int packagePriority = this.mConfig.getPackagePriority(record.sbn.getPackageName(), record.sbn.getUid());
            record.setPackagePriority(packagePriority);
        }
        return null;
    }

    @Override
    public void setConfig(RankingConfig config) {
        this.mConfig = config;
    }
}
