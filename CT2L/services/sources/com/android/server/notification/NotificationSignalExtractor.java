package com.android.server.notification;

import android.content.Context;

public interface NotificationSignalExtractor {
    void initialize(Context context);

    RankingReconsideration process(NotificationRecord notificationRecord);

    void setConfig(RankingConfig rankingConfig);
}
