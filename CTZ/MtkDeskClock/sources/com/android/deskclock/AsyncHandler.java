package com.android.deskclock;

import android.os.Handler;
import android.os.HandlerThread;

public final class AsyncHandler {
    private static final Handler sHandler;
    private static final HandlerThread sHandlerThread = new HandlerThread("AsyncHandler");

    static {
        sHandlerThread.start();
        sHandler = new Handler(sHandlerThread.getLooper());
    }

    public static void post(Runnable runnable) {
        sHandler.post(runnable);
    }

    private AsyncHandler() {
    }
}
