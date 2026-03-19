package com.android.providers.contacts;

import android.util.EventLog;

public class EventLogTags {
    public static void writeContactsUpgradeReceiver(long j) {
        EventLog.writeEvent(4100, j);
    }
}
