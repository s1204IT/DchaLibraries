package com.android.systemui.statusbar;

import com.android.systemui.statusbar.NotificationData;

public interface NotificationLifetimeExtender {

    public interface NotificationSafeToRemoveCallback {
        void onSafeToRemove(String str);
    }

    void setCallback(NotificationSafeToRemoveCallback notificationSafeToRemoveCallback);

    void setShouldManageLifetime(NotificationData.Entry entry, boolean z);

    boolean shouldExtendLifetime(NotificationData.Entry entry);

    default boolean shouldExtendLifetimeForPendingNotification(NotificationData.Entry entry) {
        return false;
    }
}
