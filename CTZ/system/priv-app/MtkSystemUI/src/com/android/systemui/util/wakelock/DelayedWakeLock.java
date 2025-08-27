package com.android.systemui.util.wakelock;

import android.os.Handler;
import java.util.Objects;

/* loaded from: classes.dex */
public class DelayedWakeLock implements WakeLock {
    private final Handler mHandler;
    private final WakeLock mInner;
    private final Runnable mRelease;

    public DelayedWakeLock(Handler handler, WakeLock wakeLock) {
        this.mHandler = handler;
        this.mInner = wakeLock;
        final WakeLock wakeLock2 = this.mInner;
        Objects.requireNonNull(wakeLock2);
        this.mRelease = new Runnable() { // from class: com.android.systemui.util.wakelock.-$$Lambda$CFIjGRHyMGVbSujKFcwVsXltENg
            @Override // java.lang.Runnable
            public final void run() {
                wakeLock2.release();
            }
        };
    }

    @Override // com.android.systemui.util.wakelock.WakeLock
    public void acquire() {
        this.mInner.acquire();
    }

    @Override // com.android.systemui.util.wakelock.WakeLock
    public void release() {
        this.mHandler.postDelayed(this.mRelease, 140L);
    }

    @Override // com.android.systemui.util.wakelock.WakeLock
    public Runnable wrap(Runnable runnable) {
        return WakeLock.wrapImpl(this, runnable);
    }
}
