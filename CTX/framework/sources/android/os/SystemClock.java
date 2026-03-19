package android.os;

import android.app.IAlarmManager;
import android.util.Slog;
import dalvik.annotation.optimization.CriticalNative;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneOffset;

public final class SystemClock {
    private static final String TAG = "SystemClock";

    @CriticalNative
    public static native long currentThreadTimeMicro();

    @CriticalNative
    public static native long currentThreadTimeMillis();

    @CriticalNative
    public static native long currentTimeMicro();

    @CriticalNative
    public static native long elapsedRealtime();

    @CriticalNative
    public static native long elapsedRealtimeNanos();

    @CriticalNative
    public static native long uptimeMillis();

    private SystemClock() {
    }

    public static void sleep(long j) {
        long jUptimeMillis = uptimeMillis();
        boolean z = false;
        long jUptimeMillis2 = j;
        do {
            try {
                Thread.sleep(jUptimeMillis2);
            } catch (InterruptedException e) {
                z = true;
            }
            jUptimeMillis2 = (jUptimeMillis + j) - uptimeMillis();
        } while (jUptimeMillis2 > 0);
        if (z) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean setCurrentTimeMillis(long j) {
        IAlarmManager iAlarmManagerAsInterface = IAlarmManager.Stub.asInterface(ServiceManager.getService("alarm"));
        if (iAlarmManagerAsInterface == null) {
            return false;
        }
        try {
            return iAlarmManagerAsInterface.setTime(j);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to set RTC", e);
            return false;
        } catch (SecurityException e2) {
            Slog.e(TAG, "Unable to set RTC", e2);
            return false;
        }
    }

    @Deprecated
    public static Clock uptimeMillisClock() {
        return uptimeClock();
    }

    public static Clock uptimeClock() {
        return new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.uptimeMillis();
            }
        };
    }

    public static Clock elapsedRealtimeClock() {
        return new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
    }

    public static long currentNetworkTimeMillis() throws Throwable {
        IAlarmManager iAlarmManagerAsInterface = IAlarmManager.Stub.asInterface(ServiceManager.getService("alarm"));
        if (iAlarmManagerAsInterface != null) {
            try {
                return iAlarmManagerAsInterface.currentNetworkTimeMillis();
            } catch (ParcelableException e) {
                e.maybeRethrow(DateTimeException.class);
                throw new RuntimeException(e);
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException(new DeadSystemException());
    }

    public static Clock currentNetworkTimeClock() {
        return new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.currentNetworkTimeMillis();
            }
        };
    }
}
