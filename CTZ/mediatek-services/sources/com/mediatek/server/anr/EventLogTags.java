package com.mediatek.server.anr;

import android.util.EventLog;

public class EventLogTags {
    public static final int AM_ANR = 30008;

    private EventLogTags() {
    }

    public static void writeAmAnr(int i, int i2, String str, int i3, String str2) {
        EventLog.writeEvent(AM_ANR, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), str2);
    }
}
