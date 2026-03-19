package com.android.deskclock;

import android.content.Context;
import android.os.PowerManager;

public class AlarmAlertWakeLock {
    private static final String TAG = "AlarmAlertWakeLock";
    private static PowerManager.WakeLock sCpuWakeLock;

    public static PowerManager.WakeLock createPartialWakeLock(Context context) {
        return ((PowerManager) context.getSystemService("power")).newWakeLock(1, TAG);
    }

    public static void acquireCpuWakeLock(Context context) {
        if (sCpuWakeLock != null) {
            return;
        }
        sCpuWakeLock = createPartialWakeLock(context);
        sCpuWakeLock.acquire();
    }

    public static void acquireScreenCpuWakeLock(Context context) {
        if (sCpuWakeLock != null) {
            return;
        }
        sCpuWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(805306369, TAG);
        sCpuWakeLock.acquire();
    }

    public static void releaseCpuLock() {
        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }
}
