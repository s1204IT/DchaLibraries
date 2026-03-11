package com.mediatek.launcher3;

import android.os.Trace;
import android.util.Log;
import java.lang.reflect.Method;

public class LauncherHelper {
    private static Boolean DEBUG = Boolean.valueOf(LauncherLog.DEBUG_PERFORMANCE);

    public static void traceCounter(long traceTag, String counterName, int counterValue) {
        try {
            Method traceCounter = Trace.class.getMethod("traceCounter", new Class[0]);
            traceCounter.invoke(Long.valueOf(traceTag), counterName, Integer.valueOf(counterValue));
        } catch (Exception e) {
            Log.e("LauncherHelper", "traceCounter() reflect fail");
        }
    }

    public static void beginSection(String tag) {
        if (!DEBUG.booleanValue()) {
            return;
        }
        Trace.beginSection(tag);
    }

    public static void endSection() {
        if (!DEBUG.booleanValue()) {
            return;
        }
        Trace.endSection();
    }
}
