package com.android.settingslib.utils;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* loaded from: classes.dex */
public class ThreadUtils {
    private static volatile Thread sMainThread;
    private static volatile Handler sMainThreadHandler;
    private static volatile ExecutorService sSingleThreadExecutor;

    public static boolean isMainThread() {
        if (sMainThread == null) {
            sMainThread = Looper.getMainLooper().getThread();
        }
        return Thread.currentThread() == sMainThread;
    }

    public static Handler getUiThreadHandler() {
        if (sMainThreadHandler == null) {
            sMainThreadHandler = new Handler(Looper.getMainLooper());
        }
        return sMainThreadHandler;
    }

    public static void ensureMainThread() {
        if (!isMainThread()) {
            throw new RuntimeException("Must be called on the UI thread");
        }
    }

    public static void postOnBackgroundThread(Runnable runnable) {
        if (sSingleThreadExecutor == null) {
            sSingleThreadExecutor = Executors.newSingleThreadExecutor();
        }
        sSingleThreadExecutor.execute(runnable);
    }

    public static void postOnMainThread(Runnable runnable) {
        getUiThreadHandler().post(runnable);
    }
}
