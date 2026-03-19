package android.app.backup;

import android.app.backup.IBackupManager;
import android.app.backup.IBackupObserver;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Pair;

public class BackupManager {
    public static final int ERROR_AGENT_FAILURE = -1003;
    public static final int ERROR_BACKUP_NOT_ALLOWED = -2001;
    public static final int ERROR_PACKAGE_NOT_FOUND = -2002;
    public static final int ERROR_TRANSPORT_ABORTED = -1000;
    public static final int ERROR_TRANSPORT_PACKAGE_REJECTED = -1002;
    public static final int ERROR_TRANSPORT_QUOTA_EXCEEDED = -1005;
    public static final int SUCCESS = 0;
    private static final String TAG = "BackupManager";
    private static IBackupManager sService;
    private Context mContext;

    private static void checkServiceBinder() {
        if (sService != null) {
            return;
        }
        sService = IBackupManager.Stub.asInterface(ServiceManager.getService(Context.BACKUP_SERVICE));
    }

    public BackupManager(Context context) {
        this.mContext = context;
    }

    public void dataChanged() {
        checkServiceBinder();
        if (sService == null) {
            return;
        }
        try {
            sService.dataChanged(this.mContext.getPackageName());
        } catch (RemoteException e) {
            Log.d(TAG, "dataChanged() couldn't connect");
        }
    }

    public static void dataChanged(String packageName) {
        checkServiceBinder();
        if (sService == null) {
            return;
        }
        try {
            sService.dataChanged(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "dataChanged(pkg) couldn't connect");
        }
    }

    public int requestRestore(RestoreObserver observer) throws Throwable {
        int result = -1;
        checkServiceBinder();
        if (sService != null) {
            RestoreSession session = null;
            try {
                try {
                    IRestoreSession binder = sService.beginRestoreSession(this.mContext.getPackageName(), null);
                    if (binder != null) {
                        RestoreSession session2 = new RestoreSession(this.mContext, binder);
                        try {
                            result = session2.restorePackage(this.mContext.getPackageName(), observer);
                            session = session2;
                        } catch (RemoteException e) {
                            session = session2;
                            Log.e(TAG, "restoreSelf() unable to contact service");
                            if (session != null) {
                                session.endRestoreSession();
                            }
                        } catch (Throwable th) {
                            th = th;
                            session = session2;
                            if (session != null) {
                                session.endRestoreSession();
                            }
                            throw th;
                        }
                    }
                    if (session != null) {
                        session.endRestoreSession();
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (RemoteException e2) {
            }
        }
        return result;
    }

    public RestoreSession beginRestoreSession() {
        checkServiceBinder();
        if (sService == null) {
            return null;
        }
        try {
            IRestoreSession binder = sService.beginRestoreSession(null, null);
            if (binder == null) {
                return null;
            }
            RestoreSession session = new RestoreSession(this.mContext, binder);
            return session;
        } catch (RemoteException e) {
            Log.e(TAG, "beginRestoreSession() couldn't connect");
            return null;
        }
    }

    public void setBackupEnabled(boolean isEnabled) {
        checkServiceBinder();
        if (sService == null) {
            return;
        }
        try {
            sService.setBackupEnabled(isEnabled);
        } catch (RemoteException e) {
            Log.e(TAG, "setBackupEnabled() couldn't connect");
        }
    }

    public boolean isBackupEnabled() {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.isBackupEnabled();
            } catch (RemoteException e) {
                Log.e(TAG, "isBackupEnabled() couldn't connect");
                return false;
            }
        }
        return false;
    }

    public void setAutoRestore(boolean isEnabled) {
        checkServiceBinder();
        if (sService == null) {
            return;
        }
        try {
            sService.setAutoRestore(isEnabled);
        } catch (RemoteException e) {
            Log.e(TAG, "setAutoRestore() couldn't connect");
        }
    }

    public String getCurrentTransport() {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.getCurrentTransport();
            } catch (RemoteException e) {
                Log.e(TAG, "getCurrentTransport() couldn't connect");
            }
        }
        return null;
    }

    public String[] listAllTransports() {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.listAllTransports();
            } catch (RemoteException e) {
                Log.e(TAG, "listAllTransports() couldn't connect");
            }
        }
        return null;
    }

    public String selectBackupTransport(String transport) {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.selectBackupTransport(transport);
            } catch (RemoteException e) {
                Log.e(TAG, "selectBackupTransport() couldn't connect");
            }
        }
        return null;
    }

    public void backupNow() {
        checkServiceBinder();
        if (sService == null) {
            return;
        }
        try {
            sService.backupNow();
        } catch (RemoteException e) {
            Log.e(TAG, "backupNow() couldn't connect");
        }
    }

    public long getAvailableRestoreToken(String packageName) {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.getAvailableRestoreToken(packageName);
            } catch (RemoteException e) {
                Log.e(TAG, "getAvailableRestoreToken() couldn't connect");
                return 0L;
            }
        }
        return 0L;
    }

    public boolean isAppEligibleForBackup(String packageName) {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.isAppEligibleForBackup(packageName);
            } catch (RemoteException e) {
                Log.e(TAG, "isAppEligibleForBackup(pkg) couldn't connect");
                return false;
            }
        }
        return false;
    }

    public int requestBackup(String[] packages, BackupObserver observer) {
        BackupObserverWrapper backupObserverWrapper;
        checkServiceBinder();
        if (sService != null) {
            if (observer == null) {
                backupObserverWrapper = null;
            } else {
                try {
                    backupObserverWrapper = new BackupObserverWrapper(this.mContext, observer);
                } catch (RemoteException e) {
                    Log.e(TAG, "requestBackup() couldn't connect");
                    return -1;
                }
            }
            return sService.requestBackup(packages, backupObserverWrapper);
        }
        return -1;
    }

    private class BackupObserverWrapper extends IBackupObserver.Stub {
        static final int MSG_FINISHED = 3;
        static final int MSG_RESULT = 2;
        static final int MSG_UPDATE = 1;
        final Handler mHandler;
        final BackupObserver mObserver;

        BackupObserverWrapper(Context context, BackupObserver observer) {
            this.mHandler = new Handler(context.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                            Pair<String, BackupProgress> obj = (Pair) msg.obj;
                            BackupObserverWrapper.this.mObserver.onUpdate((String) obj.first, (BackupProgress) obj.second);
                            break;
                        case 2:
                            BackupObserverWrapper.this.mObserver.onResult((String) msg.obj, msg.arg1);
                            break;
                        case 3:
                            BackupObserverWrapper.this.mObserver.backupFinished(msg.arg1);
                            break;
                        default:
                            Log.w(BackupManager.TAG, "Unknown message: " + msg);
                            break;
                    }
                }
            };
            this.mObserver = observer;
        }

        @Override
        public void onUpdate(String currentPackage, BackupProgress backupProgress) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, Pair.create(currentPackage, backupProgress)));
        }

        @Override
        public void onResult(String currentPackage, int status) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, status, 0, currentPackage));
        }

        @Override
        public void backupFinished(int status) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(3, status, 0));
        }
    }
}
