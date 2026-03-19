package com.android.server.backup;

import android.app.backup.IBackupManager;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreSession;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
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
    final boolean mGlobalDisable;
    volatile BackupManagerService mService;
    final File mSuppressFile;

    public Trampoline(Context context) {
        this.mContext = context;
        File dir = new File(Environment.getDataDirectory(), "backup");
        dir.mkdirs();
        this.mSuppressFile = new File(dir, BACKUP_SUPPRESS_FILENAME);
        this.mGlobalDisable = SystemProperties.getBoolean(BACKUP_DISABLE_PROPERTY, false);
    }

    public void initialize(int whichUser) {
        if (whichUser != 0) {
            return;
        }
        if (this.mGlobalDisable) {
            Slog.i(TAG, "Backup/restore not supported");
            return;
        }
        synchronized (this) {
            if (!this.mSuppressFile.exists()) {
                this.mService = new BackupManagerService(this.mContext, this);
            } else {
                Slog.i(TAG, "Backup inactive in user " + whichUser);
            }
        }
    }

    public void setBackupServiceActive(int userHandle, boolean makeActive) {
        int caller = Binder.getCallingUid();
        if (caller != 1000 && caller != 0) {
            throw new SecurityException("No permission to configure backup activity");
        }
        if (this.mGlobalDisable) {
            Slog.i(TAG, "Backup/restore not supported");
            return;
        }
        if (userHandle != 0) {
            return;
        }
        synchronized (this) {
            if (makeActive != isBackupServiceActive(userHandle)) {
                Slog.i(TAG, "Making backup " + (makeActive ? "" : "in") + "active in user " + userHandle);
                if (makeActive) {
                    this.mService = new BackupManagerService(this.mContext, this);
                    this.mSuppressFile.delete();
                } else {
                    this.mService = null;
                    try {
                        this.mSuppressFile.createNewFile();
                    } catch (IOException e) {
                        Slog.e(TAG, "Unable to persist backup service inactivity");
                    }
                }
            }
        }
    }

    public boolean isBackupServiceActive(int userHandle) {
        boolean z;
        if (userHandle != 0) {
            return false;
        }
        synchronized (this) {
            z = this.mService != null;
        }
        return z;
    }

    public void dataChanged(String packageName) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.dataChanged(packageName);
    }

    public void clearBackupData(String transportName, String packageName) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.clearBackupData(transportName, packageName);
    }

    public void agentConnected(String packageName, IBinder agent) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.agentConnected(packageName, agent);
    }

    public void agentDisconnected(String packageName) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.agentDisconnected(packageName);
    }

    public void restoreAtInstall(String packageName, int token) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.restoreAtInstall(packageName, token);
    }

    public void setBackupEnabled(boolean isEnabled) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.setBackupEnabled(isEnabled);
    }

    public void setAutoRestore(boolean doAutoRestore) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.setAutoRestore(doAutoRestore);
    }

    public void setBackupProvisioned(boolean isProvisioned) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.setBackupProvisioned(isProvisioned);
    }

    public boolean isBackupEnabled() throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.isBackupEnabled();
        }
        return false;
    }

    public boolean setBackupPassword(String currentPw, String newPw) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.setBackupPassword(currentPw, newPw);
        }
        return false;
    }

    public boolean hasBackupPassword() throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.hasBackupPassword();
        }
        return false;
    }

    public void backupNow() throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.backupNow();
    }

    public void fullBackup(ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean allApps, boolean allIncludesSystem, boolean doCompress, String[] packageNames) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.fullBackup(fd, includeApks, includeObbs, includeShared, doWidgets, allApps, allIncludesSystem, doCompress, packageNames);
    }

    public void fullTransportBackup(String[] packageNames) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.fullTransportBackup(packageNames);
    }

    public void fullRestore(ParcelFileDescriptor fd) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.fullRestore(fd);
    }

    public void acknowledgeFullBackupOrRestore(int token, boolean allow, String curPassword, String encryptionPassword, IFullBackupRestoreObserver observer) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.acknowledgeFullBackupOrRestore(token, allow, curPassword, encryptionPassword, observer);
    }

    public String getCurrentTransport() throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.getCurrentTransport();
        }
        return null;
    }

    public String[] listAllTransports() throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.listAllTransports();
        }
        return null;
    }

    public String[] getTransportWhitelist() {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.getTransportWhitelist();
        }
        return null;
    }

    public String selectBackupTransport(String transport) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.selectBackupTransport(transport);
        }
        return null;
    }

    public Intent getConfigurationIntent(String transport) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.getConfigurationIntent(transport);
        }
        return null;
    }

    public String getDestinationString(String transport) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.getDestinationString(transport);
        }
        return null;
    }

    public Intent getDataManagementIntent(String transport) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.getDataManagementIntent(transport);
        }
        return null;
    }

    public String getDataManagementLabel(String transport) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.getDataManagementLabel(transport);
        }
        return null;
    }

    public IRestoreSession beginRestoreSession(String packageName, String transportID) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.beginRestoreSession(packageName, transportID);
        }
        return null;
    }

    public void opComplete(int token, long result) throws RemoteException {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.opComplete(token, result);
    }

    public long getAvailableRestoreToken(String packageName) {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.getAvailableRestoreToken(packageName);
        }
        return 0L;
    }

    public boolean isAppEligibleForBackup(String packageName) {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.isAppEligibleForBackup(packageName);
        }
        return false;
    }

    public int requestBackup(String[] packages, IBackupObserver observer) throws RemoteException {
        BackupManagerService svc = this.mService;
        return (svc != null ? Integer.valueOf(svc.requestBackup(packages, observer)) : null).intValue();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        BackupManagerService svc = this.mService;
        if (svc != null) {
            svc.dump(fd, pw, args);
        } else {
            pw.println("Inactive");
        }
    }

    boolean beginFullBackup(FullBackupJob scheduledJob) {
        BackupManagerService svc = this.mService;
        if (svc != null) {
            return svc.beginFullBackup(scheduledJob);
        }
        return false;
    }

    void endFullBackup() {
        BackupManagerService svc = this.mService;
        if (svc == null) {
            return;
        }
        svc.endFullBackup();
    }
}
