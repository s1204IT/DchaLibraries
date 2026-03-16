package com.android.calendar.alerts;

import android.app.PendingIntent;

public interface AlarmManagerInterface {
    void set(int i, long j, PendingIntent pendingIntent);
}
