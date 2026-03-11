package com.android.systemui.util.wakelock;

import android.content.Context;
import android.os.PowerManager;

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
        return new Runnable() {
            @Override
            public final void run() {
                WakeLock.lambda$wrapImpl$0(runnable, wakeLock);
            }
        };
    }

    static void lambda$wrapImpl$0(Runnable runnable, WakeLock wakeLock) {
        try {
            runnable.run();
        } finally {
            wakeLock.release();
        }
    }

    static WakeLock wrap(final PowerManager.WakeLock wakeLock) {
        return new WakeLock() {
            @Override
            public void acquire() {
                wakeLock.acquire();
            }

            @Override
            public void release() {
                wakeLock.release();
            }

            @Override
            public Runnable wrap(Runnable runnable) {
                return WakeLock.wrapImpl(this, runnable);
            }
        };
    }
}
