package com.android.server.backup.restore;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.RestoreSet;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.params.RestoreGetSetsParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.transport.TransportClient;
import java.util.function.BiFunction;

public class ActiveRestoreSession extends IRestoreSession.Stub {
    private static final String TAG = "RestoreSession";
    private final BackupManagerService mBackupManagerService;
    private final String mPackageName;
    private final TransportManager mTransportManager;
    private final String mTransportName;
    public RestoreSet[] mRestoreSets = null;
    boolean mEnded = false;
    boolean mTimedOut = false;

    public ActiveRestoreSession(BackupManagerService backupManagerService, String str, String str2) {
        this.mBackupManagerService = backupManagerService;
        this.mPackageName = str;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mTransportName = str2;
    }

    public void markTimedOut() {
        this.mTimedOut = true;
    }

    public synchronized int getAvailableRestoreSets(IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor) {
        this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreSets");
        if (iRestoreObserver == null) {
            throw new IllegalArgumentException("Observer must not be null");
        }
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        }
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        }
        try {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                final TransportClient transportClient = this.mTransportManager.getTransportClient(this.mTransportName, "RestoreSession.getAvailableRestoreSets()");
                if (transportClient == null) {
                    Slog.w(TAG, "Null transport client getting restore sets");
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return -1;
                }
                this.mBackupManagerService.getBackupHandler().removeMessages(8);
                final PowerManager.WakeLock wakelock = this.mBackupManagerService.getWakelock();
                wakelock.acquire();
                final TransportManager transportManager = this.mTransportManager;
                this.mBackupManagerService.getBackupHandler().sendMessage(this.mBackupManagerService.getBackupHandler().obtainMessage(6, new RestoreGetSetsParams(transportClient, this, iRestoreObserver, iBackupManagerMonitor, new OnTaskFinishedListener() {
                    @Override
                    public final void onFinished(String str) {
                        ActiveRestoreSession.lambda$getAvailableRestoreSets$0(transportManager, transportClient, wakelock, str);
                    }
                })));
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return 0;
            } catch (Exception e) {
                Slog.e(TAG, "Error in getAvailableRestoreSets", e);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return -1;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity("getAvailableRestoreSets");
            throw th;
        }
    }

    static void lambda$getAvailableRestoreSets$0(TransportManager transportManager, TransportClient transportClient, PowerManager.WakeLock wakeLock, String str) {
        transportManager.disposeOfTransportClient(transportClient, str);
        wakeLock.release();
    }

    public synchronized int restoreAll(final long j, final IRestoreObserver iRestoreObserver, final IBackupManagerMonitor iBackupManagerMonitor) {
        this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
        Slog.d(TAG, "restoreAll token=" + Long.toHexString(j) + " observer=" + iRestoreObserver);
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        }
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        }
        if (this.mRestoreSets == null) {
            Slog.e(TAG, "Ignoring restoreAll() with no restore set");
            return -1;
        }
        if (this.mPackageName != null) {
            Slog.e(TAG, "Ignoring restoreAll() on single-package session");
            return -1;
        }
        if (!this.mTransportManager.isTransportRegistered(this.mTransportName)) {
            Slog.e(TAG, "Transport " + this.mTransportName + " not registered");
            return -1;
        }
        synchronized (this.mBackupManagerService.getQueueLock()) {
            for (int i = 0; i < this.mRestoreSets.length; i++) {
                if (j == this.mRestoreSets[i].token) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        return sendRestoreToHandlerLocked(new BiFunction() {
                            @Override
                            public final Object apply(Object obj, Object obj2) {
                                return RestoreParams.createForRestoreAll((TransportClient) obj, iRestoreObserver, iBackupManagerMonitor, j, (OnTaskFinishedListener) obj2);
                            }
                        }, "RestoreSession.restoreAll()");
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            }
            Slog.w(TAG, "Restore token " + Long.toHexString(j) + " not found");
            return -1;
        }
    }

    public synchronized int restoreSome(final long j, final IRestoreObserver iRestoreObserver, final IBackupManagerMonitor iBackupManagerMonitor, final String[] strArr) {
        this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
        StringBuilder sb = new StringBuilder(128);
        sb.append("restoreSome token=");
        sb.append(Long.toHexString(j));
        sb.append(" observer=");
        sb.append(iRestoreObserver.toString());
        sb.append(" monitor=");
        if (iBackupManagerMonitor == null) {
            sb.append("null");
        } else {
            sb.append(iBackupManagerMonitor.toString());
        }
        sb.append(" packages=");
        if (strArr == null) {
            sb.append("null");
        } else {
            sb.append('{');
            boolean z = true;
            for (String str : strArr) {
                if (!z) {
                    sb.append(", ");
                } else {
                    z = false;
                }
                sb.append(str);
            }
            sb.append('}');
        }
        Slog.d(TAG, sb.toString());
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        }
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        }
        if (this.mRestoreSets == null) {
            Slog.e(TAG, "Ignoring restoreAll() with no restore set");
            return -1;
        }
        if (this.mPackageName != null) {
            Slog.e(TAG, "Ignoring restoreAll() on single-package session");
            return -1;
        }
        if (!this.mTransportManager.isTransportRegistered(this.mTransportName)) {
            Slog.e(TAG, "Transport " + this.mTransportName + " not registered");
            return -1;
        }
        synchronized (this.mBackupManagerService.getQueueLock()) {
            for (int i = 0; i < this.mRestoreSets.length; i++) {
                if (j == this.mRestoreSets[i].token) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        return sendRestoreToHandlerLocked(new BiFunction() {
                            @Override
                            public final Object apply(Object obj, Object obj2) {
                                IRestoreObserver iRestoreObserver2 = iRestoreObserver;
                                IBackupManagerMonitor iBackupManagerMonitor2 = iBackupManagerMonitor;
                                long j2 = j;
                                String[] strArr2 = strArr;
                                return RestoreParams.createForRestoreSome((TransportClient) obj, iRestoreObserver2, iBackupManagerMonitor2, j2, strArr2, strArr2.length > 1, (OnTaskFinishedListener) obj2);
                            }
                        }, "RestoreSession.restoreSome(" + strArr.length + " packages)");
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            }
            Slog.w(TAG, "Restore token " + Long.toHexString(j) + " not found");
            return -1;
        }
    }

    public synchronized int restorePackage(String str, final IRestoreObserver iRestoreObserver, final IBackupManagerMonitor iBackupManagerMonitor) {
        Slog.v(TAG, "restorePackage pkg=" + str + " obs=" + iRestoreObserver + "monitor=" + iBackupManagerMonitor);
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        }
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        }
        if (this.mPackageName != null && !this.mPackageName.equals(str)) {
            Slog.e(TAG, "Ignoring attempt to restore pkg=" + str + " on session for package " + this.mPackageName);
            return -1;
        }
        try {
            final PackageInfo packageInfo = this.mBackupManagerService.getPackageManager().getPackageInfo(str, 0);
            if (this.mBackupManagerService.getContext().checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1 && packageInfo.applicationInfo.uid != Binder.getCallingUid()) {
                Slog.w(TAG, "restorePackage: bad packageName=" + str + " or calling uid=" + Binder.getCallingUid());
                throw new SecurityException("No permission to restore other packages");
            }
            if (!this.mTransportManager.isTransportRegistered(this.mTransportName)) {
                Slog.e(TAG, "Transport " + this.mTransportName + " not registered");
                return -1;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                final long availableRestoreToken = this.mBackupManagerService.getAvailableRestoreToken(str);
                Slog.v(TAG, "restorePackage pkg=" + str + " token=" + Long.toHexString(availableRestoreToken));
                if (availableRestoreToken == 0) {
                    Slog.w(TAG, "No data available for this package; not restoring");
                    return -1;
                }
                return sendRestoreToHandlerLocked(new BiFunction() {
                    @Override
                    public final Object apply(Object obj, Object obj2) {
                        return RestoreParams.createForSinglePackage((TransportClient) obj, iRestoreObserver, iBackupManagerMonitor, availableRestoreToken, packageInfo, (OnTaskFinishedListener) obj2);
                    }
                }, "RestoreSession.restorePackage(" + str + ")");
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Asked to restore nonexistent pkg " + str);
            return -1;
        }
    }

    public void setRestoreSets(RestoreSet[] restoreSetArr) {
        this.mRestoreSets = restoreSetArr;
    }

    private int sendRestoreToHandlerLocked(BiFunction<TransportClient, OnTaskFinishedListener, RestoreParams> biFunction, String str) {
        final TransportClient transportClient = this.mTransportManager.getTransportClient(this.mTransportName, str);
        if (transportClient == null) {
            Slog.e(TAG, "Transport " + this.mTransportName + " got unregistered");
            return -1;
        }
        Handler backupHandler = this.mBackupManagerService.getBackupHandler();
        backupHandler.removeMessages(8);
        final PowerManager.WakeLock wakelock = this.mBackupManagerService.getWakelock();
        wakelock.acquire();
        final TransportManager transportManager = this.mTransportManager;
        OnTaskFinishedListener onTaskFinishedListener = new OnTaskFinishedListener() {
            @Override
            public final void onFinished(String str2) {
                ActiveRestoreSession.lambda$sendRestoreToHandlerLocked$4(transportManager, transportClient, wakelock, str2);
            }
        };
        Message messageObtainMessage = backupHandler.obtainMessage(3);
        messageObtainMessage.obj = biFunction.apply(transportClient, onTaskFinishedListener);
        backupHandler.sendMessage(messageObtainMessage);
        return 0;
    }

    static void lambda$sendRestoreToHandlerLocked$4(TransportManager transportManager, TransportClient transportClient, PowerManager.WakeLock wakeLock, String str) {
        transportManager.disposeOfTransportClient(transportClient, str);
        wakeLock.release();
    }

    public class EndRestoreRunnable implements Runnable {
        BackupManagerService mBackupManager;
        ActiveRestoreSession mSession;

        public EndRestoreRunnable(BackupManagerService backupManagerService, ActiveRestoreSession activeRestoreSession) {
            this.mBackupManager = backupManagerService;
            this.mSession = activeRestoreSession;
        }

        @Override
        public void run() {
            synchronized (this.mSession) {
                this.mSession.mEnded = true;
            }
            this.mBackupManager.clearRestoreSession(this.mSession);
        }
    }

    public synchronized void endRestoreSession() {
        Slog.d(TAG, "endRestoreSession");
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
        } else {
            if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            }
            this.mBackupManagerService.getBackupHandler().post(new EndRestoreRunnable(this.mBackupManagerService, this));
        }
    }
}
