package com.android.server.statusbar;

import com.android.server.notification.NotificationDelegate;

public interface StatusBarManagerInternal {
    void buzzBeepBlinked();

    void notificationLightOff();

    void notificationLightPulse(int i, int i2, int i3);

    void setNotificationDelegate(NotificationDelegate notificationDelegate);

    void showScreenPinningRequest();
}
