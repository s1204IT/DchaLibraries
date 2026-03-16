package com.android.deskclock;

import android.content.Context;
import android.os.PowerManager;

public class AlarmAlertWakeLock {
    private static PowerManager.WakeLock sCpuWakeLock;

    public static PowerManager.WakeLock createPartialWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService("power");
        return pm.newWakeLock(1, "AlarmClock");
    }

    public static void acquireCpuWakeLock(Context context) {
        if (sCpuWakeLock == null) {
            sCpuWakeLock = createPartialWakeLock(context);
            sCpuWakeLock.acquire();
        }
    }

    public static void acquireScreenCpuWakeLock(Context context) {
        if (sCpuWakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService("power");
            sCpuWakeLock = pm.newWakeLock(805306369, "AlarmClock");
            sCpuWakeLock.acquire();
        }
    }

    public static void releaseCpuLock() {
        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }
}
