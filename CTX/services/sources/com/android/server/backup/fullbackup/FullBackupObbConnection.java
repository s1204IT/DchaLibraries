package com.android.server.backup.fullbackup;

import android.app.backup.IBackupManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.backup.IObbBackupService;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.utils.FullBackupUtils;
import java.io.IOException;
import java.io.OutputStream;

public class FullBackupObbConnection implements ServiceConnection {
    private BackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    volatile IObbBackupService mService = null;

    public FullBackupObbConnection(BackupManagerService backupManagerService) {
        this.backupManagerService = backupManagerService;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    public void establish() {
        this.backupManagerService.getContext().bindServiceAsUser(new Intent().setComponent(new ComponentName(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, "com.android.sharedstoragebackup.ObbBackupService")), this, 1, UserHandle.SYSTEM);
    }

    public void tearDown() {
        this.backupManagerService.getContext().unbindService(this);
    }

    public boolean backupObbs(PackageInfo packageInfo, OutputStream outputStream) throws Throwable {
        ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe;
        Exception e;
        waitForConnection();
        try {
            parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
            try {
                try {
                    int iGenerateRandomIntegerToken = this.backupManagerService.generateRandomIntegerToken();
                    this.backupManagerService.prepareOperationTimeout(iGenerateRandomIntegerToken, this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), null, 0);
                    this.mService.backupObbs(packageInfo.packageName, parcelFileDescriptorArrCreatePipe[1], iGenerateRandomIntegerToken, this.backupManagerService.getBackupManagerBinder());
                    FullBackupUtils.routeSocketDataToOutput(parcelFileDescriptorArrCreatePipe[0], outputStream);
                    boolean zWaitUntilOperationComplete = this.backupManagerService.waitUntilOperationComplete(iGenerateRandomIntegerToken);
                    try {
                        outputStream.flush();
                        if (parcelFileDescriptorArrCreatePipe != null) {
                            if (parcelFileDescriptorArrCreatePipe[0] != null) {
                                parcelFileDescriptorArrCreatePipe[0].close();
                            }
                            if (parcelFileDescriptorArrCreatePipe[1] != null) {
                                parcelFileDescriptorArrCreatePipe[1].close();
                            }
                        }
                    } catch (IOException e2) {
                        Slog.w(BackupManagerService.TAG, "I/O error closing down OBB backup", e2);
                    }
                    return zWaitUntilOperationComplete;
                } catch (Exception e3) {
                    e = e3;
                    Slog.w(BackupManagerService.TAG, "Unable to back up OBBs for " + packageInfo, e);
                    try {
                        outputStream.flush();
                        if (parcelFileDescriptorArrCreatePipe == null) {
                            return false;
                        }
                        if (parcelFileDescriptorArrCreatePipe[0] != null) {
                            parcelFileDescriptorArrCreatePipe[0].close();
                        }
                        if (parcelFileDescriptorArrCreatePipe[1] == null) {
                            return false;
                        }
                        parcelFileDescriptorArrCreatePipe[1].close();
                        return false;
                    } catch (IOException e4) {
                        Slog.w(BackupManagerService.TAG, "I/O error closing down OBB backup", e4);
                        return false;
                    }
                }
            } catch (Throwable th) {
                th = th;
                try {
                    outputStream.flush();
                    if (parcelFileDescriptorArrCreatePipe != null) {
                        if (parcelFileDescriptorArrCreatePipe[0] != null) {
                            parcelFileDescriptorArrCreatePipe[0].close();
                        }
                        if (parcelFileDescriptorArrCreatePipe[1] != null) {
                            parcelFileDescriptorArrCreatePipe[1].close();
                        }
                    }
                } catch (IOException e5) {
                    Slog.w(BackupManagerService.TAG, "I/O error closing down OBB backup", e5);
                }
                throw th;
            }
        } catch (Exception e6) {
            parcelFileDescriptorArrCreatePipe = null;
            e = e6;
        } catch (Throwable th2) {
            th = th2;
            parcelFileDescriptorArrCreatePipe = null;
            outputStream.flush();
            if (parcelFileDescriptorArrCreatePipe != null) {
            }
            throw th;
        }
    }

    public void restoreObbFile(String str, ParcelFileDescriptor parcelFileDescriptor, long j, int i, String str2, long j2, long j3, int i2, IBackupManager iBackupManager) {
        waitForConnection();
        try {
            this.mService.restoreObbFile(str, parcelFileDescriptor, j, i, str2, j2, j3, i2, iBackupManager);
        } catch (Exception e) {
            Slog.w(BackupManagerService.TAG, "Unable to restore OBBs for " + str, e);
        }
    }

    private void waitForConnection() {
        synchronized (this) {
            while (this.mService == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this) {
            this.mService = IObbBackupService.Stub.asInterface(iBinder);
            notifyAll();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        synchronized (this) {
            this.mService = null;
            notifyAll();
        }
    }
}
