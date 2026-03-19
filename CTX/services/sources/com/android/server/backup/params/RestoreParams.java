package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.content.pm.PackageInfo;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;

public class RestoreParams {
    public final String[] filterSet;
    public final boolean isSystemRestore;
    public final OnTaskFinishedListener listener;
    public final IBackupManagerMonitor monitor;
    public final IRestoreObserver observer;
    public final PackageInfo packageInfo;
    public final int pmToken;
    public final long token;
    public final TransportClient transportClient;

    public static RestoreParams createForSinglePackage(TransportClient transportClient, IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j, PackageInfo packageInfo, OnTaskFinishedListener onTaskFinishedListener) {
        return new RestoreParams(transportClient, iRestoreObserver, iBackupManagerMonitor, j, packageInfo, 0, false, null, onTaskFinishedListener);
    }

    public static RestoreParams createForRestoreAtInstall(TransportClient transportClient, IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j, String str, int i, OnTaskFinishedListener onTaskFinishedListener) {
        return new RestoreParams(transportClient, iRestoreObserver, iBackupManagerMonitor, j, null, i, false, new String[]{str}, onTaskFinishedListener);
    }

    public static RestoreParams createForRestoreAll(TransportClient transportClient, IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j, OnTaskFinishedListener onTaskFinishedListener) {
        return new RestoreParams(transportClient, iRestoreObserver, iBackupManagerMonitor, j, null, 0, true, null, onTaskFinishedListener);
    }

    public static RestoreParams createForRestoreSome(TransportClient transportClient, IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j, String[] strArr, boolean z, OnTaskFinishedListener onTaskFinishedListener) {
        return new RestoreParams(transportClient, iRestoreObserver, iBackupManagerMonitor, j, null, 0, z, strArr, onTaskFinishedListener);
    }

    private RestoreParams(TransportClient transportClient, IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j, PackageInfo packageInfo, int i, boolean z, String[] strArr, OnTaskFinishedListener onTaskFinishedListener) {
        this.transportClient = transportClient;
        this.observer = iRestoreObserver;
        this.monitor = iBackupManagerMonitor;
        this.token = j;
        this.packageInfo = packageInfo;
        this.pmToken = i;
        this.isSystemRestore = z;
        this.filterSet = strArr;
        this.listener = onTaskFinishedListener;
    }
}
