package android.app;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.IBinder;
import android.os.IStatsManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AndroidException;
import android.util.Slog;

@SystemApi
public final class StatsManager {
    public static final String ACTION_STATSD_STARTED = "android.app.action.STATSD_STARTED";
    private static final boolean DEBUG = false;
    public static final String EXTRA_STATS_BROADCAST_SUBSCRIBER_COOKIES = "android.app.extra.STATS_BROADCAST_SUBSCRIBER_COOKIES";
    public static final String EXTRA_STATS_CONFIG_KEY = "android.app.extra.STATS_CONFIG_KEY";
    public static final String EXTRA_STATS_CONFIG_UID = "android.app.extra.STATS_CONFIG_UID";
    public static final String EXTRA_STATS_DIMENSIONS_VALUE = "android.app.extra.STATS_DIMENSIONS_VALUE";
    public static final String EXTRA_STATS_SUBSCRIPTION_ID = "android.app.extra.STATS_SUBSCRIPTION_ID";
    public static final String EXTRA_STATS_SUBSCRIPTION_RULE_ID = "android.app.extra.STATS_SUBSCRIPTION_RULE_ID";
    private static final String TAG = "StatsManager";
    private final Context mContext;
    private IStatsManager mService;

    public StatsManager(Context context) {
        this.mContext = context;
    }

    public void addConfig(long j, byte[] bArr) throws StatsUnavailableException {
        synchronized (this) {
            try {
                try {
                    try {
                        getIStatsManagerLocked().addConfiguration(j, bArr, this.mContext.getOpPackageName());
                    } catch (SecurityException e) {
                        throw new StatsUnavailableException(e.getMessage(), e);
                    }
                } catch (RemoteException e2) {
                    Slog.e(TAG, "Failed to connect to statsd when adding configuration");
                    throw new StatsUnavailableException("could not connect", e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean addConfiguration(long j, byte[] bArr) {
        try {
            addConfig(j, bArr);
            return true;
        } catch (StatsUnavailableException | IllegalArgumentException e) {
            return false;
        }
    }

    public void removeConfig(long j) throws StatsUnavailableException {
        synchronized (this) {
            try {
                try {
                    try {
                        getIStatsManagerLocked().removeConfiguration(j, this.mContext.getOpPackageName());
                    } catch (SecurityException e) {
                        throw new StatsUnavailableException(e.getMessage(), e);
                    }
                } catch (RemoteException e2) {
                    Slog.e(TAG, "Failed to connect to statsd when removing configuration");
                    throw new StatsUnavailableException("could not connect", e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean removeConfiguration(long j) {
        try {
            removeConfig(j);
            return true;
        } catch (StatsUnavailableException e) {
            return false;
        }
    }

    public void setBroadcastSubscriber(PendingIntent pendingIntent, long j, long j2) throws StatsUnavailableException {
        synchronized (this) {
            try {
                try {
                    try {
                        IStatsManager iStatsManagerLocked = getIStatsManagerLocked();
                        if (pendingIntent != null) {
                            iStatsManagerLocked.setBroadcastSubscriber(j, j2, pendingIntent.getTarget().asBinder(), this.mContext.getOpPackageName());
                        } else {
                            iStatsManagerLocked.unsetBroadcastSubscriber(j, j2, this.mContext.getOpPackageName());
                        }
                    } catch (SecurityException e) {
                        throw new StatsUnavailableException(e.getMessage(), e);
                    }
                } catch (RemoteException e2) {
                    Slog.e(TAG, "Failed to connect to statsd when adding broadcast subscriber", e2);
                    throw new StatsUnavailableException("could not connect", e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean setBroadcastSubscriber(long j, long j2, PendingIntent pendingIntent) {
        try {
            setBroadcastSubscriber(pendingIntent, j, j2);
            return true;
        } catch (StatsUnavailableException e) {
            return false;
        }
    }

    public void setFetchReportsOperation(PendingIntent pendingIntent, long j) throws StatsUnavailableException {
        synchronized (this) {
            try {
                try {
                    IStatsManager iStatsManagerLocked = getIStatsManagerLocked();
                    if (pendingIntent == null) {
                        iStatsManagerLocked.removeDataFetchOperation(j, this.mContext.getOpPackageName());
                    } else {
                        iStatsManagerLocked.setDataFetchOperation(j, pendingIntent.getTarget().asBinder(), this.mContext.getOpPackageName());
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to connect to statsd when registering data listener.");
                    throw new StatsUnavailableException("could not connect", e);
                } catch (SecurityException e2) {
                    throw new StatsUnavailableException(e2.getMessage(), e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean setDataFetchOperation(long j, PendingIntent pendingIntent) {
        try {
            setFetchReportsOperation(pendingIntent, j);
            return true;
        } catch (StatsUnavailableException e) {
            return false;
        }
    }

    public byte[] getReports(long j) throws StatsUnavailableException {
        byte[] data;
        synchronized (this) {
            try {
                try {
                    try {
                        data = getIStatsManagerLocked().getData(j, this.mContext.getOpPackageName());
                    } catch (SecurityException e) {
                        throw new StatsUnavailableException(e.getMessage(), e);
                    }
                } catch (RemoteException e2) {
                    Slog.e(TAG, "Failed to connect to statsd when getting data");
                    throw new StatsUnavailableException("could not connect", e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return data;
    }

    public byte[] getData(long j) {
        try {
            return getReports(j);
        } catch (StatsUnavailableException e) {
            return null;
        }
    }

    public byte[] getStatsMetadata() throws StatsUnavailableException {
        byte[] metadata;
        synchronized (this) {
            try {
                try {
                    try {
                        metadata = getIStatsManagerLocked().getMetadata(this.mContext.getOpPackageName());
                    } catch (SecurityException e) {
                        throw new StatsUnavailableException(e.getMessage(), e);
                    }
                } catch (RemoteException e2) {
                    Slog.e(TAG, "Failed to connect to statsd when getting metadata");
                    throw new StatsUnavailableException("could not connect", e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return metadata;
    }

    public byte[] getMetadata() {
        try {
            return getStatsMetadata();
        } catch (StatsUnavailableException e) {
            return null;
        }
    }

    private class StatsdDeathRecipient implements IBinder.DeathRecipient {
        private StatsdDeathRecipient() {
        }

        @Override
        public void binderDied() {
            synchronized (this) {
                StatsManager.this.mService = null;
            }
        }
    }

    private IStatsManager getIStatsManagerLocked() throws StatsUnavailableException {
        if (this.mService != null) {
            return this.mService;
        }
        this.mService = IStatsManager.Stub.asInterface(ServiceManager.getService(Context.STATS_MANAGER));
        if (this.mService == null) {
            throw new StatsUnavailableException("could not be found");
        }
        try {
            this.mService.asBinder().linkToDeath(new StatsdDeathRecipient(), 0);
            return this.mService;
        } catch (RemoteException e) {
            throw new StatsUnavailableException("could not connect when linkToDeath", e);
        }
    }

    public static class StatsUnavailableException extends AndroidException {
        public StatsUnavailableException(String str) {
            super("Failed to connect to statsd: " + str);
        }

        public StatsUnavailableException(String str, Throwable th) {
            super("Failed to connect to statsd: " + str, th);
        }
    }
}
