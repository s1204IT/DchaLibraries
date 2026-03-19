package com.mediatek.plugin.utils;

import android.os.Build;
import android.os.Trace;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TraceHelper {
    private static final String TAG = "PluginManager/TraceHelper";
    private static boolean sHasCheck = false;
    private static boolean sSupportTrace;
    private static Method sTraceBeginMethod;
    private static Method sTraceEndMethod;
    private static long sViewTag;

    static {
        sSupportTrace = Build.VERSION.SDK_INT >= 18;
    }

    public static void beginSection(String str) {
        checkWhetherSupport();
        if (sTraceBeginMethod != null && sTraceEndMethod != null) {
            try {
                sTraceBeginMethod.invoke(null, Long.valueOf(sViewTag), str);
                return;
            } catch (IllegalAccessException e) {
                Log.d(TAG, "<beginSection> IllegalAccessException", e);
                return;
            } catch (InvocationTargetException e2) {
                Log.d(TAG, "<beginSection> InvocationTargetException", e2);
                return;
            }
        }
        if (sSupportTrace) {
            Trace.beginSection(str);
        }
    }

    public static void endSection() {
        checkWhetherSupport();
        if (sTraceBeginMethod != null && sTraceEndMethod != null) {
            try {
                sTraceEndMethod.invoke(null, Long.valueOf(sViewTag));
                return;
            } catch (IllegalAccessException e) {
                Log.d(TAG, "<endSection> IllegalAccessException", e);
                return;
            } catch (InvocationTargetException e2) {
                Log.d(TAG, "<endSection> InvocationTargetException", e2);
                return;
            }
        }
        if (sSupportTrace) {
            Trace.endSection();
        }
    }

    private static void checkWhetherSupport() {
        if (sHasCheck) {
            return;
        }
        if (!sSupportTrace) {
            sHasCheck = true;
            return;
        }
        try {
            sTraceBeginMethod = Trace.class.getDeclaredMethod("traceBegin", Long.TYPE, String.class);
            sTraceBeginMethod.setAccessible(true);
            sTraceEndMethod = Trace.class.getDeclaredMethod("traceEnd", Long.TYPE);
            sTraceEndMethod.setAccessible(true);
            Field declaredField = Trace.class.getDeclaredField("TRACE_TAG_VIEW");
            declaredField.setAccessible(true);
            sViewTag = declaredField.getLong(null);
        } catch (IllegalAccessException e) {
            Log.d(TAG, "<checkWhetherSupport> IllegalAccessException", e);
        } catch (NoSuchFieldException e2) {
            Log.d(TAG, "<checkWhetherSupport> NoSuchFieldException", e2);
        } catch (NoSuchMethodException e3) {
            Log.d(TAG, "<checkWhetherSupport> NoSuchMethodException", e3);
        }
        sHasCheck = true;
    }
}
