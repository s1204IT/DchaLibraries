package com.android.internal.view;

import android.os.Handler;
import android.os.Message;
import android.view.Choreographer;
import android.view.Display;

public class SurfaceFlingerVsyncChoreographer {
    private static final long ONE_MS_IN_NS = 1000000;
    private static final long ONE_S_IN_NS = 1000000000;
    private final Choreographer mChoreographer;
    private final Handler mHandler;
    private long mSurfaceFlingerOffsetMs;

    public SurfaceFlingerVsyncChoreographer(Handler handler, Display display, Choreographer choreographer) {
        this.mHandler = handler;
        this.mChoreographer = choreographer;
        this.mSurfaceFlingerOffsetMs = calculateAppSurfaceFlingerVsyncOffsetMs(display);
    }

    public long getSurfaceFlingerOffsetMs() {
        return this.mSurfaceFlingerOffsetMs;
    }

    private long calculateAppSurfaceFlingerVsyncOffsetMs(Display display) {
        return Math.max(0L, ((((long) (1.0E9f / display.getRefreshRate())) - (display.getPresentationDeadlineNanos() - 1000000)) - display.getAppVsyncOffsetNanos()) / 1000000);
    }

    public void scheduleAtSfVsync(Runnable runnable) {
        long jCalculateDelay = calculateDelay();
        if (jCalculateDelay <= 0) {
            runnable.run();
        } else {
            this.mHandler.postDelayed(runnable, jCalculateDelay);
        }
    }

    public void scheduleAtSfVsync(Handler handler, Message message) {
        long jCalculateDelay = calculateDelay();
        if (jCalculateDelay <= 0) {
            handler.handleMessage(message);
        } else {
            message.setAsynchronous(true);
            handler.sendMessageDelayed(message, jCalculateDelay);
        }
    }

    private long calculateDelay() {
        return this.mSurfaceFlingerOffsetMs - ((System.nanoTime() - this.mChoreographer.getLastFrameTimeNanos()) / 1000000);
    }
}
