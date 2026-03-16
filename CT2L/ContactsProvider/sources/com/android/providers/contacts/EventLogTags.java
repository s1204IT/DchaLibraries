package com.android.providers.contacts;

import android.util.EventLog;

public class EventLogTags {
    public static void writeContactsUpgradeReceiver(long time) {
        EventLog.writeEvent(4100, time);
    }
}
