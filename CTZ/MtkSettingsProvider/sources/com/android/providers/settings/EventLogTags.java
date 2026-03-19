package com.android.providers.settings;

import android.util.EventLog;

public class EventLogTags {
    public static void writeUnsupportedSettingsQuery(String str, String str2, String str3) {
        EventLog.writeEvent(52100, str, str2, str3);
    }
}
