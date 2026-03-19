package com.mediatek.calendar;

import android.os.SystemProperties;
import android.os.Trace;

public class PDebug {
    private static Boolean DEBUG;
    private static long TRACE_TAG;

    static {
        DEBUG = true;
        TRACE_TAG = 0L;
        DEBUG = Boolean.valueOf(true ^ SystemProperties.get("vendor.ap.performance.debug", "0").equals("0"));
        if (DEBUG.booleanValue()) {
            TRACE_TAG = 1 << ((int) Long.parseLong(SystemProperties.get("ap.performance.debug")));
        }
    }

    public static void Start(String str) {
        if (DEBUG.booleanValue()) {
            Trace.traceCounter(TRACE_TAG, "P$Calendar." + str, 1);
        }
    }

    public static void End(String str) {
        if (DEBUG.booleanValue()) {
            Trace.traceCounter(TRACE_TAG, "P$Calendar." + str, 0);
        }
    }

    public static void EndAndStart(String str, String str2) {
        if (DEBUG.booleanValue()) {
            Trace.traceCounter(TRACE_TAG, "P$Calendar." + str, 0);
            Trace.traceCounter(TRACE_TAG, "P$Calendar." + str2, 1);
        }
    }
}
