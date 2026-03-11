package com.android.systemui.util.wakelock;

import android.os.Handler;
import java.util.Objects;

public class DelayedWakeLock implements WakeLock {
    private final Handler mHandler;
    private final WakeLock mInner;
    private final Runnable mRelease;

    public DelayedWakeLock(Handler handler, WakeLock wakeLock) {
        this.mHandler = handler;
        this.mInner = wakeLock;
        final WakeLock wakeLock2 = this.mInner;
        Objects.requireNonNull(wakeLock2);
        this.mRelease = new Runnable() {
            @Override
            public final void run() {
                wakeLock2.release();
            }
        };
    }

    @Override
    public void acquire() {
        this.mInner.acquire();
    }

    @Override
    public void release() {
        this.mHandler.postDelayed(this.mRelease, 140L);
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        return WakeLock.wrapImpl(this, runnable);
    }
}
