package com.android.server.telecom;

import android.os.Looper;
import android.telecom.Log;

public final class ThreadUtil {
    private static final String TAG = ThreadUtil.class.getSimpleName();

    private ThreadUtil() {
    }

    public static boolean isOnMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    public static void checkNotOnMainThread() {
        if (isOnMainThread()) {
            Log.wtf(TAG, new IllegalStateException(), "Must not be on the main thread!", new Object[0]);
        }
    }
}
