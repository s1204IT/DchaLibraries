package android.app.backup;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.backup.IBackupManager;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.ISelectBackupTransportCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;

public class BackupManager {

    @SystemApi
    public static final int ERROR_AGENT_FAILURE = -1003;

    @SystemApi
    public static final int ERROR_BACKUP_CANCELLED = -2003;

    @SystemApi
    public static final int ERROR_BACKUP_NOT_ALLOWED = -2001;

    @SystemApi
    public static final int ERROR_PACKAGE_NOT_FOUND = -2002;

    @SystemApi
    public static final int ERROR_TRANSPORT_ABORTED = -1000;

    @SystemApi
    public static final int ERROR_TRANSPORT_INVALID = -2;

    @SystemApi
    public static final int ERROR_TRANSPORT_PACKAGE_REJECTED = -1002;

    @SystemApi
    public static final int ERROR_TRANSPORT_QUOTA_EXCEEDED = -1005;

    @SystemApi
    public static final int ERROR_TRANSPORT_UNAVAILABLE = -1;
    public static final String EXTRA_BACKUP_SERVICES_AVAILABLE = "backup_services_available";

    @SystemApi
    public static final int FLAG_NON_INCREMENTAL_BACKUP = 1;

    @SystemApi
    public static final String PACKAGE_MANAGER_SENTINEL = "@pm@";

    @SystemApi
    public static final int SUCCESS = 0;
    private static final String TAG = "BackupManager";
    private static IBackupManager sService;
    private Context mContext;

    private static void checkServiceBinder() {
        if (sService == null) {
            sService = IBackupManager.Stub.asInterface(ServiceManager.getService(Context.BACKUP_SERVICE));
        }
    }

    public BackupManager(Context context) {
        this.mContext = context;
    }

    public void dataChanged() {
        checkServiceBinder();
        if (sService != null) {
            try {
                sService.dataChanged(this.mContext.getPackageName());
            } catch (RemoteException e) {
                Log.d(TAG, "dataChanged() couldn't connect");
            }
        }
    }

    public static void dataChanged(String str) {
        checkServiceBinder();
        if (sService != null) {
            try {
                sService.dataChanged(str);
            } catch (RemoteException e) {
                Log.e(TAG, "dataChanged(pkg) couldn't connect");
            }
        }
    }

    @Deprecated
    public int requestRestore(RestoreObserver restoreObserver) {
        return requestRestore(restoreObserver, null);
    }

    @SystemApi
    @Deprecated
    public int requestRestore(RestoreObserver restoreObserver, BackupManagerMonitor backupManagerMonitor) {
        Log.w(TAG, "requestRestore(): Since Android P app can no longer request restoring of its backup.");
        return -1;
    }

    @SystemApi
    public RestoreSession beginRestoreSession() {
        checkServiceBinder();
        if (sService == null) {
            return null;
        }
        try {
            IRestoreSession iRestoreSessionBeginRestoreSession = sService.beginRestoreSession(null, null);
            if (iRestoreSessionBeginRestoreSession != null) {
                return new RestoreSession(this.mContext, iRestoreSessionBeginRestoreSession);
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "beginRestoreSession() couldn't connect");
            return null;
        }
    }

    @SystemApi
    public void setBackupEnabled(boolean z) {
        checkServiceBinder();
        if (sService != null) {
            try {
                sService.setBackupEnabled(z);
            } catch (RemoteException e) {
                Log.e(TAG, "setBackupEnabled() couldn't connect");
            }
        }
    }

    @SystemApi
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

    @SystemApi
    public boolean isBackupServiceActive(UserHandle userHandle) {
        this.mContext.enforceCallingOrSelfPermission(Manifest.permission.BACKUP, "isBackupServiceActive");
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.isBackupServiceActive(userHandle.getIdentifier());
            } catch (RemoteException e) {
                Log.e(TAG, "isBackupEnabled() couldn't connect");
                return false;
            }
        }
        return false;
    }

    @SystemApi
    public void setAutoRestore(boolean z) {
        checkServiceBinder();
        if (sService != null) {
            try {
                sService.setAutoRestore(z);
            } catch (RemoteException e) {
                Log.e(TAG, "setAutoRestore() couldn't connect");
            }
        }
    }

    @SystemApi
    public String getCurrentTransport() {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.getCurrentTransport();
            } catch (RemoteException e) {
                Log.e(TAG, "getCurrentTransport() couldn't connect");
                return null;
            }
        }
        return null;
    }

    @SystemApi
    public String[] listAllTransports() {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.listAllTransports();
            } catch (RemoteException e) {
                Log.e(TAG, "listAllTransports() couldn't connect");
                return null;
            }
        }
        return null;
    }

    @SystemApi
    public void updateTransportAttributes(ComponentName componentName, String str, Intent intent, String str2, Intent intent2, String str3) {
        checkServiceBinder();
        if (sService != null) {
            try {
                sService.updateTransportAttributes(componentName, str, intent, str2, intent2, str3);
            } catch (RemoteException e) {
                Log.e(TAG, "describeTransport() couldn't connect");
            }
        }
    }

    @SystemApi
    @Deprecated
    public String selectBackupTransport(String str) {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.selectBackupTransport(str);
            } catch (RemoteException e) {
                Log.e(TAG, "selectBackupTransport() couldn't connect");
                return null;
            }
        }
        return null;
    }

    @SystemApi
    public void selectBackupTransport(ComponentName componentName, SelectBackupTransportCallback selectBackupTransportCallback) {
        SelectTransportListenerWrapper selectTransportListenerWrapper;
        checkServiceBinder();
        if (sService != null) {
            if (selectBackupTransportCallback == null) {
                selectTransportListenerWrapper = null;
            } else {
                try {
                    selectTransportListenerWrapper = new SelectTransportListenerWrapper(this.mContext, selectBackupTransportCallback);
                } catch (RemoteException e) {
                    Log.e(TAG, "selectBackupTransportAsync() couldn't connect");
                    return;
                }
            }
            sService.selectBackupTransportAsync(componentName, selectTransportListenerWrapper);
        }
    }

    @SystemApi
    public void backupNow() {
        checkServiceBinder();
        if (sService != null) {
            try {
                sService.backupNow();
            } catch (RemoteException e) {
                Log.e(TAG, "backupNow() couldn't connect");
            }
        }
    }

    @SystemApi
    public long getAvailableRestoreToken(String str) {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.getAvailableRestoreToken(str);
            } catch (RemoteException e) {
                Log.e(TAG, "getAvailableRestoreToken() couldn't connect");
                return 0L;
            }
        }
        return 0L;
    }

    @SystemApi
    public boolean isAppEligibleForBackup(String str) {
        checkServiceBinder();
        if (sService != null) {
            try {
                return sService.isAppEligibleForBackup(str);
            } catch (RemoteException e) {
                Log.e(TAG, "isAppEligibleForBackup(pkg) couldn't connect");
                return false;
            }
        }
        return false;
    }

    @SystemApi
    public int requestBackup(String[] strArr, BackupObserver backupObserver) {
        return requestBackup(strArr, backupObserver, null, 0);
    }

    @SystemApi
    public int requestBackup(String[] strArr, BackupObserver backupObserver, BackupManagerMonitor backupManagerMonitor, int i) {
        BackupObserverWrapper backupObserverWrapper;
        checkServiceBinder();
        if (sService != null) {
            BackupManagerMonitorWrapper backupManagerMonitorWrapper = null;
            if (backupObserver != null) {
                try {
                    backupObserverWrapper = new BackupObserverWrapper(this.mContext, backupObserver);
                } catch (RemoteException e) {
                    Log.e(TAG, "requestBackup() couldn't connect");
                    return -1;
                }
            } else {
                backupObserverWrapper = null;
            }
            if (backupManagerMonitor != null) {
                backupManagerMonitorWrapper = new BackupManagerMonitorWrapper(backupManagerMonitor);
            }
            return sService.requestBackup(strArr, backupObserverWrapper, backupManagerMonitorWrapper, i);
        }
        return -1;
    }

    @SystemApi
    public void cancelBackups() {
        checkServiceBinder();
        if (sService != null) {
            try {
                sService.cancelBackups();
            } catch (RemoteException e) {
                Log.e(TAG, "cancelBackups() couldn't connect.");
            }
        }
    }

    @SystemApi
    public Intent getConfigurationIntent(String str) {
        if (sService != null) {
            try {
                return sService.getConfigurationIntent(str);
            } catch (RemoteException e) {
                Log.e(TAG, "getConfigurationIntent() couldn't connect");
                return null;
            }
        }
        return null;
    }

    @SystemApi
    public String getDestinationString(String str) {
        if (sService != null) {
            try {
                return sService.getDestinationString(str);
            } catch (RemoteException e) {
                Log.e(TAG, "getDestinationString() couldn't connect");
                return null;
            }
        }
        return null;
    }

    @SystemApi
    public Intent getDataManagementIntent(String str) {
        if (sService != null) {
            try {
                return sService.getDataManagementIntent(str);
            } catch (RemoteException e) {
                Log.e(TAG, "getDataManagementIntent() couldn't connect");
                return null;
            }
        }
        return null;
    }

    @SystemApi
    public String getDataManagementLabel(String str) {
        if (sService != null) {
            try {
                return sService.getDataManagementLabel(str);
            } catch (RemoteException e) {
                Log.e(TAG, "getDataManagementLabel() couldn't connect");
                return null;
            }
        }
        return null;
    }

    private class BackupObserverWrapper extends IBackupObserver.Stub {
        static final int MSG_FINISHED = 3;
        static final int MSG_RESULT = 2;
        static final int MSG_UPDATE = 1;
        final Handler mHandler;
        final BackupObserver mObserver;

        BackupObserverWrapper(Context context, BackupObserver backupObserver) {
            this.mHandler = new Handler(context.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    switch (message.what) {
                        case 1:
                            Pair pair = (Pair) message.obj;
                            BackupObserverWrapper.this.mObserver.onUpdate((String) pair.first, (BackupProgress) pair.second);
                            break;
                        case 2:
                            BackupObserverWrapper.this.mObserver.onResult((String) message.obj, message.arg1);
                            break;
                        case 3:
                            BackupObserverWrapper.this.mObserver.backupFinished(message.arg1);
                            break;
                        default:
                            Log.w(BackupManager.TAG, "Unknown message: " + message);
                            break;
                    }
                }
            };
            this.mObserver = backupObserver;
        }

        @Override
        public void onUpdate(String str, BackupProgress backupProgress) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, Pair.create(str, backupProgress)));
        }

        @Override
        public void onResult(String str, int i) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, i, 0, str));
        }

        @Override
        public void backupFinished(int i) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(3, i, 0));
        }
    }

    private class SelectTransportListenerWrapper extends ISelectBackupTransportCallback.Stub {
        private final Handler mHandler;
        private final SelectBackupTransportCallback mListener;

        SelectTransportListenerWrapper(Context context, SelectBackupTransportCallback selectBackupTransportCallback) {
            this.mHandler = new Handler(context.getMainLooper());
            this.mListener = selectBackupTransportCallback;
        }

        @Override
        public void onSuccess(final String str) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SelectTransportListenerWrapper.this.mListener.onSuccess(str);
                }
            });
        }

        @Override
        public void onFailure(final int i) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SelectTransportListenerWrapper.this.mListener.onFailure(i);
                }
            });
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
