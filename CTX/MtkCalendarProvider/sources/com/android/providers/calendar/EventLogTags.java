package com.android.providers.calendar;

import android.util.EventLog;

public class EventLogTags {
    public static void writeCalendarUpgradeReceiver(long j) {
        EventLog.writeEvent(4000, j);
    }
}
