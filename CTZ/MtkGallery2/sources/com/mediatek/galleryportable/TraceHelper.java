package com.mediatek.galleryportable;

import android.os.Build;
import android.os.Trace;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TraceHelper {
    private static boolean sHasCheck = false;
    private static boolean sSupportTrace;
    private static Method sTraceBeginMethod;
    private static Method sTraceEndMethod;
    private static long sViewTag;

    static {
        sSupportTrace = Build.VERSION.SDK_INT >= 18;
    }

    public static void beginSection(String sectionName) {
        checkWhetherSupport();
        if (sTraceBeginMethod != null && sTraceEndMethod != null) {
            try {
                sTraceBeginMethod.invoke(null, Long.valueOf(sViewTag), sectionName);
                return;
            } catch (IllegalAccessException e) {
                android.util.Log.d("MtkGallery2/TraceHelper", "<beginSection> IllegalAccessException", e);
                return;
            } catch (InvocationTargetException e2) {
                android.util.Log.d("MtkGallery2/TraceHelper", "<beginSection> InvocationTargetException", e2);
                return;
            }
        }
        if (sSupportTrace) {
            Trace.beginSection(sectionName);
        }
    }

    public static void endSection() {
        checkWhetherSupport();
        if (sTraceBeginMethod != null && sTraceEndMethod != null) {
            try {
                sTraceEndMethod.invoke(null, Long.valueOf(sViewTag));
                return;
            } catch (IllegalAccessException e) {
                android.util.Log.d("MtkGallery2/TraceHelper", "<endSection> IllegalAccessException", e);
                return;
            } catch (InvocationTargetException e2) {
                android.util.Log.d("MtkGallery2/TraceHelper", "<endSection> InvocationTargetException", e2);
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
            Field viewTagFiled = Trace.class.getDeclaredField("TRACE_TAG_VIEW");
            viewTagFiled.setAccessible(true);
            sViewTag = viewTagFiled.getLong(null);
        } catch (IllegalAccessException e) {
            android.util.Log.d("MtkGallery2/TraceHelper", "<checkWhetherSupport> IllegalAccessException", e);
        } catch (NoSuchFieldException e2) {
            android.util.Log.d("MtkGallery2/TraceHelper", "<checkWhetherSupport> NoSuchFieldException", e2);
        } catch (NoSuchMethodException e3) {
            android.util.Log.d("MtkGallery2/TraceHelper", "<checkWhetherSupport> NoSuchMethodException", e3);
        }
        sHasCheck = true;
    }
}
