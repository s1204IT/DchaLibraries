package android.app.backup;

import android.annotation.SystemApi;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

@SystemApi
public class RestoreSession {
    static final String TAG = "RestoreSession";
    IRestoreSession mBinder;
    final Context mContext;
    RestoreObserverWrapper mObserver = null;

    public int getAvailableRestoreSets(RestoreObserver restoreObserver, BackupManagerMonitor backupManagerMonitor) {
        BackupManagerMonitorWrapper backupManagerMonitorWrapper;
        RestoreObserverWrapper restoreObserverWrapper = new RestoreObserverWrapper(this.mContext, restoreObserver);
        if (backupManagerMonitor == null) {
            backupManagerMonitorWrapper = null;
        } else {
            backupManagerMonitorWrapper = new BackupManagerMonitorWrapper(backupManagerMonitor);
        }
        try {
            return this.mBinder.getAvailableRestoreSets(restoreObserverWrapper, backupManagerMonitorWrapper);
        } catch (RemoteException e) {
            Log.d(TAG, "Can't contact server to get available sets");
            return -1;
        }
    }

    public int getAvailableRestoreSets(RestoreObserver restoreObserver) {
        return getAvailableRestoreSets(restoreObserver, null);
    }

    public int restoreAll(long j, RestoreObserver restoreObserver, BackupManagerMonitor backupManagerMonitor) {
        BackupManagerMonitorWrapper backupManagerMonitorWrapper;
        if (this.mObserver != null) {
            Log.d(TAG, "restoreAll() called during active restore");
            return -1;
        }
        this.mObserver = new RestoreObserverWrapper(this.mContext, restoreObserver);
        if (backupManagerMonitor == null) {
            backupManagerMonitorWrapper = null;
        } else {
            backupManagerMonitorWrapper = new BackupManagerMonitorWrapper(backupManagerMonitor);
        }
        try {
            return this.mBinder.restoreAll(j, this.mObserver, backupManagerMonitorWrapper);
        } catch (RemoteException e) {
            Log.d(TAG, "Can't contact server to restore");
            return -1;
        }
    }

    public int restoreAll(long j, RestoreObserver restoreObserver) {
        return restoreAll(j, restoreObserver, null);
    }

    public int restoreSome(long j, RestoreObserver restoreObserver, BackupManagerMonitor backupManagerMonitor, String[] strArr) {
        BackupManagerMonitorWrapper backupManagerMonitorWrapper;
        if (this.mObserver != null) {
            Log.d(TAG, "restoreAll() called during active restore");
            return -1;
        }
        this.mObserver = new RestoreObserverWrapper(this.mContext, restoreObserver);
        if (backupManagerMonitor == null) {
            backupManagerMonitorWrapper = null;
        } else {
            backupManagerMonitorWrapper = new BackupManagerMonitorWrapper(backupManagerMonitor);
        }
        try {
            return this.mBinder.restoreSome(j, this.mObserver, backupManagerMonitorWrapper, strArr);
        } catch (RemoteException e) {
            Log.d(TAG, "Can't contact server to restore packages");
            return -1;
        }
    }

    public int restoreSome(long j, RestoreObserver restoreObserver, String[] strArr) {
        return restoreSome(j, restoreObserver, null, strArr);
    }

    public int restorePackage(String str, RestoreObserver restoreObserver, BackupManagerMonitor backupManagerMonitor) {
        BackupManagerMonitorWrapper backupManagerMonitorWrapper;
        if (this.mObserver != null) {
            Log.d(TAG, "restorePackage() called during active restore");
            return -1;
        }
        this.mObserver = new RestoreObserverWrapper(this.mContext, restoreObserver);
        if (backupManagerMonitor == null) {
            backupManagerMonitorWrapper = null;
        } else {
            backupManagerMonitorWrapper = new BackupManagerMonitorWrapper(backupManagerMonitor);
        }
        try {
            return this.mBinder.restorePackage(str, this.mObserver, backupManagerMonitorWrapper);
        } catch (RemoteException e) {
            Log.d(TAG, "Can't contact server to restore package");
            return -1;
        }
    }

    public int restorePackage(String str, RestoreObserver restoreObserver) {
        return restorePackage(str, restoreObserver, null);
    }

    public void endRestoreSession() {
        try {
            try {
                this.mBinder.endRestoreSession();
            } catch (RemoteException e) {
                Log.d(TAG, "Can't contact server to get available sets");
            }
        } finally {
            this.mBinder = null;
        }
    }

    RestoreSession(Context context, IRestoreSession iRestoreSession) {
        this.mContext = context;
        this.mBinder = iRestoreSession;
    }

    private class RestoreObserverWrapper extends IRestoreObserver.Stub {
        static final int MSG_RESTORE_FINISHED = 3;
        static final int MSG_RESTORE_SETS_AVAILABLE = 4;
        static final int MSG_RESTORE_STARTING = 1;
        static final int MSG_UPDATE = 2;
        final RestoreObserver mAppObserver;
        final Handler mHandler;

        RestoreObserverWrapper(Context context, RestoreObserver restoreObserver) {
            this.mHandler = new Handler(context.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    switch (message.what) {
                        case 1:
                            RestoreObserverWrapper.this.mAppObserver.restoreStarting(message.arg1);
                            break;
                        case 2:
                            RestoreObserverWrapper.this.mAppObserver.onUpdate(message.arg1, (String) message.obj);
                            break;
                        case 3:
                            RestoreObserverWrapper.this.mAppObserver.restoreFinished(message.arg1);
                            break;
                        case 4:
                            RestoreObserverWrapper.this.mAppObserver.restoreSetsAvailable((RestoreSet[]) message.obj);
                            break;
                    }
                }
            };
            this.mAppObserver = restoreObserver;
        }

        @Override
        public void restoreSetsAvailable(RestoreSet[] restoreSetArr) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(4, restoreSetArr));
        }

        @Override
        public void restoreStarting(int i) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, i, 0));
        }

        @Override
        public void onUpdate(int i, String str) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, i, 0, str));
        }

        @Override
        public void restoreFinished(int i) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(3, i, 0));
        }
    }

    private class BackupManagerMonitorWrapper extends IBackupManagerMonitor.Stub {
        final BackupManagerMonitor mMonitor;

        BackupManagerMonitorWrapper(BackupManagerMonitor backupManagerMonitor) {
            this.mMonitor = backupManagerMonitor;
        }

        @Override
        public void onEvent(Bundle bundle) throws RemoteException {
            this.mMonitor.onEvent(bundle);
        }
    }
}
