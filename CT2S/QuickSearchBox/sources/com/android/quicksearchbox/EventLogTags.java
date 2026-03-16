package com.android.quicksearchbox;

import android.util.EventLog;

public class EventLogTags {
    public static void writeQsbStart(String name, int version, String startMethod, int latency, String searchSource, String enabledSources, int onCreateLatency) {
        EventLog.writeEvent(71001, name, Integer.valueOf(version), startMethod, Integer.valueOf(latency), searchSource, enabledSources, Integer.valueOf(onCreateLatency));
    }

    public static void writeQsbClick(long id, String suggestions, String queriedSources, int numChars, int clickType) {
        EventLog.writeEvent(71002, Long.valueOf(id), suggestions, queriedSources, Integer.valueOf(numChars), Integer.valueOf(clickType));
    }

    public static void writeQsbSearch(String searchSource, int method, int numChars) {
        EventLog.writeEvent(71003, searchSource, Integer.valueOf(method), Integer.valueOf(numChars));
    }

    public static void writeQsbVoiceSearch(String searchSource) {
        EventLog.writeEvent(71004, searchSource);
    }

    public static void writeQsbExit(String suggestions, int numChars) {
        EventLog.writeEvent(71005, suggestions, Integer.valueOf(numChars));
    }
}
