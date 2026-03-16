package com.android.server.notification;

import android.content.Context;

public class PackageVisibilityExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "PackageVisibilityExtractor";
    private RankingConfig mConfig;

    @Override
    public void initialize(Context ctx) {
    }

    @Override
    public RankingReconsideration process(NotificationRecord record) {
        if (record != null && record.getNotification() != null && this.mConfig != null) {
            int packageVisibility = this.mConfig.getPackageVisibilityOverride(record.sbn.getPackageName(), record.sbn.getUid());
            record.setPackageVisibilityOverride(packageVisibility);
        }
        return null;
    }

    @Override
    public void setConfig(RankingConfig config) {
        this.mConfig = config;
    }
}
