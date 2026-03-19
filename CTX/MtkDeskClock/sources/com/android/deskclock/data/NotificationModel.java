package com.android.deskclock.data;

import com.android.deskclock.settings.SettingsActivity;

final class NotificationModel {
    private boolean mApplicationInForeground;

    NotificationModel() {
    }

    void setApplicationInForeground(boolean z) {
        this.mApplicationInForeground = z;
    }

    boolean isApplicationInForeground() {
        return this.mApplicationInForeground;
    }

    int getStopwatchNotificationId() {
        return 2147483646;
    }

    int getUnexpiredTimerNotificationId() {
        return 2147483645;
    }

    int getExpiredTimerNotificationId() {
        return 2147483644;
    }

    int getMissedTimerNotificationId() {
        return 2147483641;
    }

    String getStopwatchNotificationGroupKey() {
        return "3";
    }

    String getTimerNotificationGroupKey() {
        return SettingsActivity.VOLUME_BEHAVIOR_DISMISS;
    }

    String getTimerNotificationSortKey() {
        return SettingsActivity.DEFAULT_VOLUME_BEHAVIOR;
    }

    String getTimerNotificationMissedSortKey() {
        return SettingsActivity.VOLUME_BEHAVIOR_SNOOZE;
    }
}
