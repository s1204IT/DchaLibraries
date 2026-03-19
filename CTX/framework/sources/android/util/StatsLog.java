package android.util;

import android.content.Context;
import android.os.IStatsManager;
import android.os.RemoteException;
import android.os.ServiceManager;

public final class StatsLog extends StatsLogInternal {
    private static final boolean DEBUG = false;
    private static final String TAG = "StatsLog";
    private static IStatsManager sService;

    private StatsLog() {
    }

    public static boolean logStart(int i) {
        synchronized (StatsLog.class) {
            try {
                try {
                    IStatsManager iStatsManagerLocked = getIStatsManagerLocked();
                    if (iStatsManagerLocked == null) {
                        return false;
                    }
                    iStatsManagerLocked.sendAppBreadcrumbAtom(i, 3);
                    return true;
                } catch (RemoteException e) {
                    sService = null;
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public static boolean logStop(int i) {
        synchronized (StatsLog.class) {
            try {
                try {
                    IStatsManager iStatsManagerLocked = getIStatsManagerLocked();
                    if (iStatsManagerLocked == null) {
                        return false;
                    }
                    iStatsManagerLocked.sendAppBreadcrumbAtom(i, 2);
                    return true;
                } catch (RemoteException e) {
                    sService = null;
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public static boolean logEvent(int i) {
        synchronized (StatsLog.class) {
            try {
                try {
                    IStatsManager iStatsManagerLocked = getIStatsManagerLocked();
                    if (iStatsManagerLocked == null) {
                        return false;
                    }
                    iStatsManagerLocked.sendAppBreadcrumbAtom(i, 1);
                    return true;
                } catch (RemoteException e) {
                    sService = null;
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static IStatsManager getIStatsManagerLocked() throws RemoteException {
        if (sService != null) {
            return sService;
        }
        sService = IStatsManager.Stub.asInterface(ServiceManager.getService(Context.STATS_MANAGER));
        return sService;
    }
}
