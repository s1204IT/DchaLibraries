package com.android.server.backup;

import android.app.backup.IBackupManager;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.ISelectBackupTransportCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.BatteryService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;

public class Trampoline extends IBackupManager.Stub {
    static final String BACKUP_DISABLE_PROPERTY = "ro.backup.disable";
    static final String BACKUP_SUPPRESS_FILENAME = "backup-suppress";
    static final boolean DEBUG_TRAMPOLINE = false;
    static final String TAG = "BackupManagerService";
    final Context mContext;
    private HandlerThread mHandlerThread;
    volatile BackupManagerServiceInterface mService;
    final boolean mGlobalDisable = isBackupDisabled();
    final File mSuppressFile = getSuppressFile();

    public Trampoline(Context context) {
        this.mContext = context;
        this.mSuppressFile.getParentFile().mkdirs();
    }

    protected boolean isBackupDisabled() {
        return SystemProperties.getBoolean(BACKUP_DISABLE_PROPERTY, false);
    }

    protected int binderGetCallingUid() {
        return Binder.getCallingUid();
    }

    protected File getSuppressFile() {
        return new File(new File(Environment.getDataDirectory(), BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD), BACKUP_SUPPRESS_FILENAME);
    }

    protected BackupManagerServiceInterface createBackupManagerService() {
        return BackupManagerService.create(this.mContext, this, this.mHandlerThread);
    }

    public void initialize(int i) {
        if (i == 0) {
            if (this.mGlobalDisable) {
                Slog.i("BackupManagerService", "Backup/restore not supported");
                return;
            }
            synchronized (this) {
                if (!this.mSuppressFile.exists()) {
                    this.mService = createBackupManagerService();
                } else {
                    Slog.i("BackupManagerService", "Backup inactive in user " + i);
                }
            }
        }
    }

    void unlockSystemUser() {
        this.mHandlerThread = new HandlerThread(BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD, 10);
        this.mHandlerThread.start();
        new Handler(this.mHandlerThread.getLooper()).post(new Runnable() {
            @Override
            public final void run() {
                Trampoline.lambda$unlockSystemUser$0(this.f$0);
            }
        });
    }

    public static void lambda$unlockSystemUser$0(Trampoline trampoline) {
        Trace.traceBegin(64L, "backup init");
        trampoline.initialize(0);
        Trace.traceEnd(64L);
        BackupManagerServiceInterface backupManagerServiceInterface = trampoline.mService;
        Slog.i("BackupManagerService", "Unlocking system user; mService=" + trampoline.mService);
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.unlockSystemUser();
        }
    }

    public void setBackupServiceActive(int i, boolean z) {
        int iBinderGetCallingUid = binderGetCallingUid();
        if (iBinderGetCallingUid != 1000 && iBinderGetCallingUid != 0) {
            throw new SecurityException("No permission to configure backup activity");
        }
        if (this.mGlobalDisable) {
            Slog.i("BackupManagerService", "Backup/restore not supported");
            return;
        }
        if (i == 0) {
            synchronized (this) {
                if (z != isBackupServiceActive(i)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Making backup ");
                    sb.append(z ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "in");
                    sb.append("active in user ");
                    sb.append(i);
                    Slog.i("BackupManagerService", sb.toString());
                    if (z) {
                        this.mService = createBackupManagerService();
                        this.mSuppressFile.delete();
                    } else {
                        this.mService = null;
                        try {
                            this.mSuppressFile.createNewFile();
                        } catch (IOException e) {
                            Slog.e("BackupManagerService", "Unable to persist backup service inactivity");
                        }
                    }
                }
            }
        }
    }

    public boolean isBackupServiceActive(int i) {
        boolean z;
        if (i != 0) {
            return false;
        }
        synchronized (this) {
            z = this.mService != null;
        }
        return z;
    }

    public void dataChanged(String str) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.dataChanged(str);
        }
    }

    public void initializeTransports(String[] strArr, IBackupObserver iBackupObserver) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.initializeTransports(strArr, iBackupObserver);
        }
    }

    public void clearBackupData(String str, String str2) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.clearBackupData(str, str2);
        }
    }

    public void agentConnected(String str, IBinder iBinder) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.agentConnected(str, iBinder);
        }
    }

    public void agentDisconnected(String str) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.agentDisconnected(str);
        }
    }

    public void restoreAtInstall(String str, int i) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.restoreAtInstall(str, i);
        }
    }

    public void setBackupEnabled(boolean z) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.setBackupEnabled(z);
        }
    }

    public void setAutoRestore(boolean z) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.setAutoRestore(z);
        }
    }

    public void setBackupProvisioned(boolean z) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.setBackupProvisioned(z);
        }
    }

    public boolean isBackupEnabled() throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.isBackupEnabled();
        }
        return false;
    }

    public boolean setBackupPassword(String str, String str2) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.setBackupPassword(str, str2);
        }
        return false;
    }

    public boolean hasBackupPassword() throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.hasBackupPassword();
        }
        return false;
    }

    public void backupNow() throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.backupNow();
        }
    }

    public void adbBackup(ParcelFileDescriptor parcelFileDescriptor, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, boolean z8, String[] strArr) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.adbBackup(parcelFileDescriptor, z, z2, z3, z4, z5, z6, z7, z8, strArr);
        }
    }

    public void fullTransportBackup(String[] strArr) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.fullTransportBackup(strArr);
        }
    }

    public void adbRestore(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.adbRestore(parcelFileDescriptor);
        }
    }

    public void acknowledgeFullBackupOrRestore(int i, boolean z, String str, String str2, IFullBackupRestoreObserver iFullBackupRestoreObserver) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.acknowledgeAdbBackupOrRestore(i, z, str, str2, iFullBackupRestoreObserver);
        }
    }

    public String getCurrentTransport() throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.getCurrentTransport();
        }
        return null;
    }

    public String[] listAllTransports() throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.listAllTransports();
        }
        return null;
    }

    public ComponentName[] listAllTransportComponents() throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.listAllTransportComponents();
        }
        return null;
    }

    public String[] getTransportWhitelist() {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.getTransportWhitelist();
        }
        return null;
    }

    public void updateTransportAttributes(ComponentName componentName, String str, Intent intent, String str2, Intent intent2, String str3) {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.updateTransportAttributes(componentName, str, intent, str2, intent2, str3);
        }
    }

    public String selectBackupTransport(String str) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.selectBackupTransport(str);
        }
        return null;
    }

    public void selectBackupTransportAsync(ComponentName componentName, ISelectBackupTransportCallback iSelectBackupTransportCallback) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.selectBackupTransportAsync(componentName, iSelectBackupTransportCallback);
        } else if (iSelectBackupTransportCallback != null) {
            try {
                iSelectBackupTransportCallback.onFailure(-2001);
            } catch (RemoteException e) {
            }
        }
    }

    public Intent getConfigurationIntent(String str) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.getConfigurationIntent(str);
        }
        return null;
    }

    public String getDestinationString(String str) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.getDestinationString(str);
        }
        return null;
    }

    public Intent getDataManagementIntent(String str) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.getDataManagementIntent(str);
        }
        return null;
    }

    public String getDataManagementLabel(String str) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.getDataManagementLabel(str);
        }
        return null;
    }

    public IRestoreSession beginRestoreSession(String str, String str2) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.beginRestoreSession(str, str2);
        }
        return null;
    }

    public void opComplete(int i, long j) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.opComplete(i, j);
        }
    }

    public long getAvailableRestoreToken(String str) {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.getAvailableRestoreToken(str);
        }
        return 0L;
    }

    public boolean isAppEligibleForBackup(String str) {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.isAppEligibleForBackup(str);
        }
        return false;
    }

    public String[] filterAppsEligibleForBackup(String[] strArr) {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.filterAppsEligibleForBackup(strArr);
        }
        return null;
    }

    public int requestBackup(String[] strArr, IBackupObserver iBackupObserver, IBackupManagerMonitor iBackupManagerMonitor, int i) throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface == null) {
            return -2001;
        }
        return backupManagerServiceInterface.requestBackup(strArr, iBackupObserver, iBackupManagerMonitor, i);
    }

    public void cancelBackups() throws RemoteException {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.cancelBackups();
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, "BackupManagerService", printWriter)) {
            BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
            if (backupManagerServiceInterface != null) {
                backupManagerServiceInterface.dump(fileDescriptor, printWriter, strArr);
            } else {
                printWriter.println("Inactive");
            }
        }
    }

    boolean beginFullBackup(FullBackupJob fullBackupJob) {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            return backupManagerServiceInterface.beginFullBackup(fullBackupJob);
        }
        return false;
    }

    void endFullBackup() {
        BackupManagerServiceInterface backupManagerServiceInterface = this.mService;
        if (backupManagerServiceInterface != null) {
            backupManagerServiceInterface.endFullBackup();
        }
    }
}
