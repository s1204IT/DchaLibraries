package com.mediatek.settings;

import android.os.SystemProperties;
import android.os.Trace;

public class PDebug {
    private static Boolean DEBUG;
    private static long TRACE_TAG;

    static {
        DEBUG = true;
        TRACE_TAG = 0L;
        DEBUG = Boolean.valueOf(SystemProperties.get("ap.performance.debug", "0").equals("0") ? false : true);
        if (!DEBUG.booleanValue()) {
            return;
        }
        TRACE_TAG = 1 << ((int) Long.parseLong(SystemProperties.get("ap.performance.debug")));
    }

    public static void Start(String msg) {
        if (!DEBUG.booleanValue()) {
            return;
        }
        Trace.traceCounter(TRACE_TAG, "P$" + msg, 1);
    }

    public static void End(String msg) {
        if (!DEBUG.booleanValue()) {
            return;
        }
        Trace.traceCounter(TRACE_TAG, "P$" + msg, 0);
    }
}
