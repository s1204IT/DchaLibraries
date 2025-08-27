package com.android.quicksearchbox;

import android.util.EventLog;

/* loaded from: classes.dex */
public class EventLogTags {
    public static void writeQsbStart(String str, int i, String str2, int i2, String str3, String str4, int i3) {
        EventLog.writeEvent(71001, str, Integer.valueOf(i), str2, Integer.valueOf(i2), str3, str4, Integer.valueOf(i3));
    }

    public static void writeQsbClick(long j, String str, String str2, int i, int i2) {
        EventLog.writeEvent(71002, Long.valueOf(j), str, str2, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeQsbSearch(String str, int i, int i2) {
        EventLog.writeEvent(71003, str, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeQsbVoiceSearch(String str) {
        EventLog.writeEvent(71004, str);
    }

    public static void writeQsbExit(String str, int i) {
        EventLog.writeEvent(71005, str, Integer.valueOf(i));
    }
}
