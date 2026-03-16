package com.android.calendar.alerts;

import com.android.calendar.alerts.AlertService;

public abstract class NotificationMgr {
    public abstract void cancel(int i);

    public abstract void notify(int i, AlertService.NotificationWrapper notificationWrapper);

    public void cancelAll() {
        cancelAllBetween(0, 20);
    }

    public void cancelAllBetween(int from, int to) {
        for (int i = from; i <= to; i++) {
            cancel(i);
        }
    }
}
