package android.net.util;

import android.net.INetd;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.job.controllers.JobStatus;

public class NetdService {
    private static final long BASE_TIMEOUT_MS = 100;
    private static final long MAX_TIMEOUT_MS = 1000;
    private static final String NETD_SERVICE_NAME = "netd";
    private static final String TAG = NetdService.class.getSimpleName();

    public interface NetdCommand {
        void run(INetd iNetd) throws RemoteException;
    }

    public static INetd getInstance() {
        INetd iNetdAsInterface = INetd.Stub.asInterface(ServiceManager.getService(NETD_SERVICE_NAME));
        if (iNetdAsInterface == null) {
            Log.w(TAG, "WARNING: returning null INetd instance.");
        }
        return iNetdAsInterface;
    }

    public static INetd get(long j) {
        long jElapsedRealtime;
        if (j == 0) {
            return getInstance();
        }
        if (j > 0) {
            jElapsedRealtime = SystemClock.elapsedRealtime() + j;
        } else {
            jElapsedRealtime = JobStatus.NO_LATEST_RUNTIME;
        }
        long jMin = 0;
        while (true) {
            INetd netdService = getInstance();
            if (netdService != null) {
                return netdService;
            }
            long jElapsedRealtime2 = jElapsedRealtime - SystemClock.elapsedRealtime();
            if (jElapsedRealtime2 > 0) {
                jMin = Math.min(Math.min(jMin + BASE_TIMEOUT_MS, 1000L), jElapsedRealtime2);
                try {
                    Thread.sleep(jMin);
                } catch (InterruptedException e) {
                }
            } else {
                return null;
            }
        }
    }

    public static INetd get() {
        return get(-1L);
    }

    public static void run(NetdCommand netdCommand) {
        while (true) {
            try {
                netdCommand.run(get());
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "error communicating with netd: " + e);
            }
        }
    }
}
