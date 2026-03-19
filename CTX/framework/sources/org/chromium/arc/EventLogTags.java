package org.chromium.arc;

import android.util.EventLog;

public class EventLogTags {
    public static final int ARC_SYSTEM_EVENT = 300000;

    private EventLogTags() {
    }

    public static void writeArcSystemEvent(String str) {
        EventLog.writeEvent(ARC_SYSTEM_EVENT, str);
    }
}
