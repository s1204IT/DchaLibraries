package com.android.providers.calendar;

import android.util.EventLog;

public class EventLogTags {
    public static void writeCalendarUpgradeReceiver(long time) {
        EventLog.writeEvent(4000, time);
    }
}
