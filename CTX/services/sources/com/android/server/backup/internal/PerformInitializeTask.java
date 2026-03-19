package com.android.server.backup.internal;

import android.app.backup.IBackupObserver;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.backup.IBackupTransport;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.transport.TransportClient;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class PerformInitializeTask implements Runnable {
    private final BackupManagerService mBackupManagerService;
    private final File mBaseStateDir;
    private final OnTaskFinishedListener mListener;
    private IBackupObserver mObserver;
    private final String[] mQueue;
    private final TransportManager mTransportManager;

    public PerformInitializeTask(BackupManagerService backupManagerService, String[] strArr, IBackupObserver iBackupObserver, OnTaskFinishedListener onTaskFinishedListener) {
        this(backupManagerService, backupManagerService.getTransportManager(), strArr, iBackupObserver, onTaskFinishedListener, backupManagerService.getBaseStateDir());
    }

    @VisibleForTesting
    PerformInitializeTask(BackupManagerService backupManagerService, TransportManager transportManager, String[] strArr, IBackupObserver iBackupObserver, OnTaskFinishedListener onTaskFinishedListener, File file) {
        this.mBackupManagerService = backupManagerService;
        this.mTransportManager = transportManager;
        this.mQueue = strArr;
        this.mObserver = iBackupObserver;
        this.mListener = onTaskFinishedListener;
        this.mBaseStateDir = file;
    }

    private void notifyResult(String str, int i) {
        try {
            if (this.mObserver != null) {
                this.mObserver.onResult(str, i);
            }
        } catch (RemoteException e) {
            this.mObserver = null;
        }
    }

    private void notifyFinished(int i) {
        try {
            if (this.mObserver != null) {
                this.mObserver.backupFinished(i);
            }
        } catch (RemoteException e) {
            this.mObserver = null;
        }
    }

    @Override
    public void run() throws Throwable {
        ?? r7;
        boolean zHasNext;
        boolean zHasNext2;
        ?? arrayList = new ArrayList(this.mQueue.length);
        ?? r4 = 0;
        TransportManager transportManager = null;
        try {
            try {
                String[] strArr = this.mQueue;
                int length = strArr.length;
                int i = 0;
                r7 = 0;
                while (i < length) {
                    try {
                        String str = strArr[i];
                        TransportClient transportClient = this.mTransportManager.getTransportClient(str, "PerformInitializeTask.run()");
                        if (transportClient == null) {
                            Slog.e(BackupManagerService.TAG, "Requested init for " + str + " but not found");
                        } else {
                            arrayList.add(transportClient);
                            Slog.i(BackupManagerService.TAG, "Initializing (wiping) backup transport storage: " + str);
                            String transportDirName = this.mTransportManager.getTransportDirName(transportClient.getTransportComponent());
                            EventLog.writeEvent(EventLogTags.BACKUP_START, transportDirName);
                            long jElapsedRealtime = SystemClock.elapsedRealtime();
                            IBackupTransport iBackupTransportConnectOrThrow = transportClient.connectOrThrow("PerformInitializeTask.run()");
                            int iInitializeDevice = iBackupTransportConnectOrThrow.initializeDevice();
                            if (iInitializeDevice == 0) {
                                iInitializeDevice = iBackupTransportConnectOrThrow.finishBackup();
                            }
                            if (iInitializeDevice == 0) {
                                Slog.i(BackupManagerService.TAG, "Device init successful");
                                int iElapsedRealtime = (int) (SystemClock.elapsedRealtime() - jElapsedRealtime);
                                EventLog.writeEvent(EventLogTags.BACKUP_INITIALIZE, new Object[0]);
                                this.mBackupManagerService.resetBackupState(new File(this.mBaseStateDir, transportDirName));
                                EventLog.writeEvent(EventLogTags.BACKUP_SUCCESS, 0, Integer.valueOf(iElapsedRealtime));
                                this.mBackupManagerService.recordInitPending(false, str, transportDirName);
                                notifyResult(str, 0);
                            } else {
                                Slog.e(BackupManagerService.TAG, "Transport error in initializeDevice()");
                                EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
                                this.mBackupManagerService.recordInitPending(true, str, transportDirName);
                                notifyResult(str, iInitializeDevice);
                                try {
                                    long jRequestBackupTime = iBackupTransportConnectOrThrow.requestBackupTime();
                                    Slog.w(BackupManagerService.TAG, "Init failed on " + str + " resched in " + jRequestBackupTime);
                                    this.mBackupManagerService.getAlarmManager().set(0, System.currentTimeMillis() + jRequestBackupTime, this.mBackupManagerService.getRunInitIntent());
                                    r7 = iInitializeDevice;
                                } catch (Exception e) {
                                    e = e;
                                    Slog.e(BackupManagerService.TAG, "Unexpected error performing init", e);
                                    Iterator it = arrayList.iterator();
                                    while (true) {
                                        zHasNext = it.hasNext();
                                        if (!zHasNext) {
                                            break;
                                        } else {
                                            this.mTransportManager.disposeOfTransportClient((TransportClient) it.next(), "PerformInitializeTask.run()");
                                        }
                                    }
                                    notifyFinished(JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                    arrayList = it;
                                    r4 = zHasNext;
                                    this.mListener.onFinished("PerformInitializeTask.run()");
                                } catch (Throwable th) {
                                    th = th;
                                    r7 = iInitializeDevice;
                                    Iterator it2 = arrayList.iterator();
                                    while (it2.hasNext()) {
                                        this.mTransportManager.disposeOfTransportClient((TransportClient) it2.next(), "PerformInitializeTask.run()");
                                    }
                                    notifyFinished(r7);
                                    this.mListener.onFinished("PerformInitializeTask.run()");
                                    throw th;
                                }
                            }
                        }
                        i++;
                        r7 = r7;
                    } catch (Exception e2) {
                        e = e2;
                        boolean z = r7 == true ? 1 : 0;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                Iterator it3 = arrayList.iterator();
                while (true) {
                    zHasNext2 = it3.hasNext();
                    if (!zHasNext2) {
                        break;
                    }
                    TransportClient transportClient2 = (TransportClient) it3.next();
                    TransportManager transportManager2 = this.mTransportManager;
                    transportManager2.disposeOfTransportClient(transportClient2, "PerformInitializeTask.run()");
                    transportManager = transportManager2;
                }
                notifyFinished(r7 == true ? 1 : 0);
                arrayList = zHasNext2;
                r4 = transportManager;
            } catch (Throwable th3) {
                th = th3;
                r7 = r4;
            }
        } catch (Exception e3) {
            e = e3;
        }
        this.mListener.onFinished("PerformInitializeTask.run()");
    }
}
