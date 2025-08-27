package com.android.systemui.util.wakelock;

import android.content.Context;
import android.os.PowerManager;

/* loaded from: classes.dex */
public interface WakeLock {
    void acquire();

    void release();

    Runnable wrap(Runnable runnable);

    static WakeLock createPartial(Context context, String str) {
        return wrap(createPartialInner(context, str));
    }

    static PowerManager.WakeLock createPartialInner(Context context, String str) {
        return ((PowerManager) context.getSystemService(PowerManager.class)).newWakeLock(1, str);
    }

    static Runnable wrapImpl(final WakeLock wakeLock, final Runnable runnable) {
        wakeLock.acquire();
        return new Runnable() { // from class: com.android.systemui.util.wakelock.-$$Lambda$WakeLock$Rdut1DSGlHtP-OM8Y87P7galvtM
            @Override // java.lang.Runnable
            public final void run() {
                WakeLock.lambda$wrapImpl$0(runnable, wakeLock);
            }
        };
    }

    static /* synthetic */ void lambda$wrapImpl$0(Runnable runnable, WakeLock wakeLock) {
        try {
            runnable.run();
        } finally {
            wakeLock.release();
        }
    }

    static WakeLock wrap(final PowerManager.WakeLock wakeLock) {
        return new WakeLock() { // from class: com.android.systemui.util.wakelock.WakeLock.1
            @Override // com.android.systemui.util.wakelock.WakeLock
            public void acquire() {
                wakeLock.acquire();
            }

            @Override // com.android.systemui.util.wakelock.WakeLock
            public void release() {
                wakeLock.release();
            }

            @Override // com.android.systemui.util.wakelock.WakeLock
            public Runnable wrap(Runnable runnable) {
                return WakeLock.wrapImpl(this, runnable);
            }
        };
    }
}
