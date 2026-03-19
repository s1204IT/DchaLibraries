package com.android.music;

import android.os.Trace;

public class PDebug {
    private static Boolean DEBUG = false;
    private static long TRACE_TAG = 0;

    public static void Start(String str) {
        if (DEBUG.booleanValue()) {
            Trace.traceCounter(TRACE_TAG, "P$" + str, 1);
        }
    }

    public static void End(String str) {
        if (DEBUG.booleanValue()) {
            Trace.traceCounter(TRACE_TAG, "P$" + str, 0);
        }
    }
}
