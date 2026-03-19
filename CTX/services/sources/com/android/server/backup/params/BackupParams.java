package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;
import java.util.ArrayList;

public class BackupParams {
    public String dirName;
    public ArrayList<String> fullPackages;
    public ArrayList<String> kvPackages;
    public OnTaskFinishedListener listener;
    public IBackupManagerMonitor monitor;
    public boolean nonIncrementalBackup;
    public IBackupObserver observer;
    public TransportClient transportClient;
    public boolean userInitiated;

    public BackupParams(TransportClient transportClient, String str, ArrayList<String> arrayList, ArrayList<String> arrayList2, IBackupObserver iBackupObserver, IBackupManagerMonitor iBackupManagerMonitor, OnTaskFinishedListener onTaskFinishedListener, boolean z, boolean z2) {
        this.transportClient = transportClient;
        this.dirName = str;
        this.kvPackages = arrayList;
        this.fullPackages = arrayList2;
        this.observer = iBackupObserver;
        this.monitor = iBackupManagerMonitor;
        this.listener = onTaskFinishedListener;
        this.userInitiated = z;
        this.nonIncrementalBackup = z2;
    }
}
