package com.android.server.backup.internal;

import android.content.pm.PackageInfo;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.transport.TransportClient;
import java.io.File;

public class PerformClearTask implements Runnable {
    private final BackupManagerService mBackupManagerService;
    private final OnTaskFinishedListener mListener;
    private final PackageInfo mPackage;
    private final TransportClient mTransportClient;
    private final TransportManager mTransportManager;

    PerformClearTask(BackupManagerService backupManagerService, TransportClient transportClient, PackageInfo packageInfo, OnTaskFinishedListener onTaskFinishedListener) {
        this.mBackupManagerService = backupManagerService;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mTransportClient = transportClient;
        this.mPackage = packageInfo;
        this.mListener = onTaskFinishedListener;
    }

    @Override
    public void run() throws Throwable {
        String str;
        StringBuilder sb;
        IBackupTransport iBackupTransportConnectOrThrow;
        IBackupTransport iBackupTransport = null;
        try {
            try {
                new File(new File(this.mBackupManagerService.getBaseStateDir(), this.mTransportManager.getTransportDirName(this.mTransportClient.getTransportComponent())), this.mPackage.packageName).delete();
                iBackupTransportConnectOrThrow = this.mTransportClient.connectOrThrow("PerformClearTask.run()");
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            iBackupTransportConnectOrThrow.clearBackupData(this.mPackage);
            if (iBackupTransportConnectOrThrow != null) {
                try {
                    iBackupTransportConnectOrThrow.finishBackup();
                } catch (Exception e2) {
                    e = e2;
                    str = BackupManagerService.TAG;
                    sb = new StringBuilder();
                    sb.append("Unable to mark clear operation finished: ");
                    sb.append(e.getMessage());
                    Slog.e(str, sb.toString());
                }
            }
        } catch (Exception e3) {
            e = e3;
            iBackupTransport = iBackupTransportConnectOrThrow;
            Slog.e(BackupManagerService.TAG, "Transport threw clearing data for " + this.mPackage + ": " + e.getMessage());
            if (iBackupTransport != null) {
                try {
                    iBackupTransport.finishBackup();
                } catch (Exception e4) {
                    e = e4;
                    str = BackupManagerService.TAG;
                    sb = new StringBuilder();
                    sb.append("Unable to mark clear operation finished: ");
                    sb.append(e.getMessage());
                    Slog.e(str, sb.toString());
                }
            }
        } catch (Throwable th2) {
            th = th2;
            iBackupTransport = iBackupTransportConnectOrThrow;
            if (iBackupTransport != null) {
                try {
                    iBackupTransport.finishBackup();
                } catch (Exception e5) {
                    Slog.e(BackupManagerService.TAG, "Unable to mark clear operation finished: " + e5.getMessage());
                }
            }
            this.mListener.onFinished("PerformClearTask.run()");
            this.mBackupManagerService.getWakelock().release();
            throw th;
        }
        this.mListener.onFinished("PerformClearTask.run()");
        this.mBackupManagerService.getWakelock().release();
    }
}
