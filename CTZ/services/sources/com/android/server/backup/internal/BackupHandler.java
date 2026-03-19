package com.android.server.backup.internal;

import android.app.backup.RestoreSet;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.TransportManager;
import com.android.server.backup.fullbackup.PerformAdbBackupTask;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.params.AdbBackupParams;
import com.android.server.backup.params.AdbParams;
import com.android.server.backup.params.AdbRestoreParams;
import com.android.server.backup.params.BackupParams;
import com.android.server.backup.params.ClearParams;
import com.android.server.backup.params.ClearRetryParams;
import com.android.server.backup.params.RestoreGetSetsParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.restore.ActiveRestoreSession;
import com.android.server.backup.restore.ActiveRestoreSession.EndRestoreRunnable;
import com.android.server.backup.restore.PerformAdbRestoreTask;
import com.android.server.backup.restore.PerformUnifiedRestoreTask;
import com.android.server.backup.transport.TransportClient;
import com.android.server.wm.WindowManagerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

public class BackupHandler extends Handler {
    public static final int MSG_BACKUP_OPERATION_TIMEOUT = 17;
    public static final int MSG_BACKUP_RESTORE_STEP = 20;
    public static final int MSG_FULL_CONFIRMATION_TIMEOUT = 9;
    public static final int MSG_OP_COMPLETE = 21;
    public static final int MSG_REQUEST_BACKUP = 15;
    public static final int MSG_RESTORE_OPERATION_TIMEOUT = 18;
    public static final int MSG_RESTORE_SESSION_TIMEOUT = 8;
    public static final int MSG_RETRY_CLEAR = 12;
    public static final int MSG_RETRY_INIT = 11;
    public static final int MSG_RUN_ADB_BACKUP = 2;
    public static final int MSG_RUN_ADB_RESTORE = 10;
    public static final int MSG_RUN_BACKUP = 1;
    public static final int MSG_RUN_CLEAR = 4;
    public static final int MSG_RUN_FULL_TRANSPORT_BACKUP = 14;
    public static final int MSG_RUN_GET_RESTORE_SETS = 6;
    public static final int MSG_RUN_RESTORE = 3;
    public static final int MSG_SCHEDULE_BACKUP_PACKAGE = 16;
    public static final int MSG_WIDGET_BROADCAST = 13;
    private final BackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;

    public BackupHandler(BackupManagerService backupManagerService, Looper looper) {
        super(looper);
        this.backupManagerService = backupManagerService;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    @Override
    public void handleMessage(Message message) throws Throwable {
        TransportClient transportClient;
        boolean z;
        RestoreSet[] availableRestoreSets;
        String str;
        StringBuilder sb;
        final TransportManager transportManager = this.backupManagerService.getTransportManager();
        RestoreSet[] restoreSetArr = null;
        switch (message.what) {
            case 1:
                this.backupManagerService.setLastBackupPass(System.currentTimeMillis());
                final TransportClient currentTransportClient = transportManager.getCurrentTransportClient("BH/MSG_RUN_BACKUP");
                IBackupTransport iBackupTransportConnect = currentTransportClient != null ? currentTransportClient.connect("BH/MSG_RUN_BACKUP") : null;
                if (iBackupTransportConnect == null) {
                    if (currentTransportClient != null) {
                        transportManager.disposeOfTransportClient(currentTransportClient, "BH/MSG_RUN_BACKUP");
                    }
                    Slog.v(BackupManagerService.TAG, "Backup requested but no transport available");
                    synchronized (this.backupManagerService.getQueueLock()) {
                        this.backupManagerService.setBackupRunning(false);
                        break;
                    }
                    this.backupManagerService.getWakelock().release();
                    return;
                }
                ArrayList arrayList = new ArrayList();
                DataChangedJournal journal = this.backupManagerService.getJournal();
                synchronized (this.backupManagerService.getQueueLock()) {
                    if (this.backupManagerService.getPendingBackups().size() > 0) {
                        Iterator<BackupRequest> it = this.backupManagerService.getPendingBackups().values().iterator();
                        while (it.hasNext()) {
                            arrayList.add(it.next());
                        }
                        Slog.v(BackupManagerService.TAG, "clearing pending backups");
                        this.backupManagerService.getPendingBackups().clear();
                        this.backupManagerService.setJournal(null);
                    }
                    break;
                }
                if (arrayList.size() > 0) {
                    try {
                        transportClient = currentTransportClient;
                    } catch (Exception e) {
                        e = e;
                        transportClient = currentTransportClient;
                    }
                    try {
                        sendMessage(obtainMessage(20, new PerformBackupTask(this.backupManagerService, currentTransportClient, iBackupTransportConnect.transportDirName(), arrayList, journal, null, null, new OnTaskFinishedListener() {
                            @Override
                            public final void onFinished(String str2) {
                                transportManager.disposeOfTransportClient(currentTransportClient, str2);
                            }
                        }, Collections.emptyList(), false, false)));
                        z = true;
                    } catch (Exception e2) {
                        e = e2;
                        Slog.e(BackupManagerService.TAG, "Transport became unavailable attempting backup or error initializing backup task", e);
                        z = false;
                    }
                    if (z) {
                        transportManager.disposeOfTransportClient(transportClient, "BH/MSG_RUN_BACKUP");
                        synchronized (this.backupManagerService.getQueueLock()) {
                            this.backupManagerService.setBackupRunning(false);
                            break;
                        }
                        this.backupManagerService.getWakelock().release();
                        return;
                    }
                    return;
                }
                transportClient = currentTransportClient;
                Slog.v(BackupManagerService.TAG, "Backup requested but nothing pending");
                z = false;
                if (z) {
                }
                break;
            case 2:
                AdbBackupParams adbBackupParams = (AdbBackupParams) message.obj;
                new Thread(new PerformAdbBackupTask(this.backupManagerService, adbBackupParams.fd, adbBackupParams.observer, adbBackupParams.includeApks, adbBackupParams.includeObbs, adbBackupParams.includeShared, adbBackupParams.doWidgets, adbBackupParams.curPassword, adbBackupParams.encryptPassword, adbBackupParams.allApps, adbBackupParams.includeSystem, adbBackupParams.doCompress, adbBackupParams.includeKeyValue, adbBackupParams.packages, adbBackupParams.latch), "adb-backup").start();
                return;
            case 3:
                RestoreParams restoreParams = (RestoreParams) message.obj;
                Slog.d(BackupManagerService.TAG, "MSG_RUN_RESTORE observer=" + restoreParams.observer);
                PerformUnifiedRestoreTask performUnifiedRestoreTask = new PerformUnifiedRestoreTask(this.backupManagerService, restoreParams.transportClient, restoreParams.observer, restoreParams.monitor, restoreParams.token, restoreParams.packageInfo, restoreParams.pmToken, restoreParams.isSystemRestore, restoreParams.filterSet, restoreParams.listener);
                synchronized (this.backupManagerService.getPendingRestores()) {
                    if (this.backupManagerService.isRestoreInProgress()) {
                        Slog.d(BackupManagerService.TAG, "Restore in progress, queueing.");
                        this.backupManagerService.getPendingRestores().add(performUnifiedRestoreTask);
                    } else {
                        Slog.d(BackupManagerService.TAG, "Starting restore.");
                        this.backupManagerService.setRestoreInProgress(true);
                        sendMessage(obtainMessage(20, performUnifiedRestoreTask));
                    }
                    break;
                }
                return;
            case 4:
                ClearParams clearParams = (ClearParams) message.obj;
                new PerformClearTask(this.backupManagerService, clearParams.transportClient, clearParams.packageInfo, clearParams.listener).run();
                return;
            case 5:
            case 7:
            case 11:
            case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
            default:
                return;
            case 6:
                RestoreGetSetsParams restoreGetSetsParams = (RestoreGetSetsParams) message.obj;
                try {
                    try {
                        availableRestoreSets = restoreGetSetsParams.transportClient.connectOrThrow("BH/MSG_RUN_GET_RESTORE_SETS").getAvailableRestoreSets();
                    } catch (Exception e3) {
                        e = e3;
                    }
                    break;
                } catch (Throwable th) {
                    th = th;
                    availableRestoreSets = restoreSetArr;
                }
                try {
                    synchronized (restoreGetSetsParams.session) {
                        restoreGetSetsParams.session.setRestoreSets(availableRestoreSets);
                        break;
                    }
                    if (availableRestoreSets == null) {
                        EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                    }
                    if (restoreGetSetsParams.observer != null) {
                        try {
                            restoreGetSetsParams.observer.restoreSetsAvailable(availableRestoreSets);
                        } catch (RemoteException e4) {
                            Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                        } catch (Exception e5) {
                            e = e5;
                            str = BackupManagerService.TAG;
                            sb = new StringBuilder();
                            sb.append("Restore observer threw: ");
                            sb.append(e.getMessage());
                            Slog.e(str, sb.toString());
                        }
                    }
                    break;
                } catch (Exception e6) {
                    e = e6;
                    restoreSetArr = availableRestoreSets;
                    Slog.e(BackupManagerService.TAG, "Error from transport getting set list: " + e.getMessage());
                    if (restoreGetSetsParams.observer != null) {
                        try {
                            restoreGetSetsParams.observer.restoreSetsAvailable(restoreSetArr);
                        } catch (RemoteException e7) {
                            Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                        } catch (Exception e8) {
                            e = e8;
                            str = BackupManagerService.TAG;
                            sb = new StringBuilder();
                            sb.append("Restore observer threw: ");
                            sb.append(e.getMessage());
                            Slog.e(str, sb.toString());
                        }
                    }
                    break;
                } catch (Throwable th2) {
                    th = th2;
                    Throwable th3 = th;
                    if (restoreGetSetsParams.observer != null) {
                        try {
                            restoreGetSetsParams.observer.restoreSetsAvailable(availableRestoreSets);
                        } catch (RemoteException e9) {
                            Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                        } catch (Exception e10) {
                            Slog.e(BackupManagerService.TAG, "Restore observer threw: " + e10.getMessage());
                        }
                        break;
                    }
                    removeMessages(8);
                    sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis());
                    restoreGetSetsParams.listener.onFinished("BH/MSG_RUN_GET_RESTORE_SETS");
                    throw th3;
                }
                removeMessages(8);
                sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis());
                restoreGetSetsParams.listener.onFinished("BH/MSG_RUN_GET_RESTORE_SETS");
                return;
            case 8:
                synchronized (this.backupManagerService) {
                    if (this.backupManagerService.getActiveRestoreSession() != null) {
                        Slog.w(BackupManagerService.TAG, "Restore session timed out; aborting");
                        this.backupManagerService.getActiveRestoreSession().markTimedOut();
                        ActiveRestoreSession activeRestoreSession = this.backupManagerService.getActiveRestoreSession();
                        Objects.requireNonNull(activeRestoreSession);
                        post(activeRestoreSession.new EndRestoreRunnable(this.backupManagerService, this.backupManagerService.getActiveRestoreSession()));
                    }
                    break;
                }
                return;
            case 9:
                synchronized (this.backupManagerService.getAdbBackupRestoreConfirmations()) {
                    AdbParams adbParams = this.backupManagerService.getAdbBackupRestoreConfirmations().get(message.arg1);
                    if (adbParams != null) {
                        Slog.i(BackupManagerService.TAG, "Full backup/restore timed out waiting for user confirmation");
                        this.backupManagerService.signalAdbBackupRestoreCompletion(adbParams);
                        this.backupManagerService.getAdbBackupRestoreConfirmations().delete(message.arg1);
                        if (adbParams.observer != null) {
                            try {
                                adbParams.observer.onTimeout();
                                break;
                            } catch (RemoteException e11) {
                            }
                        }
                    } else {
                        Slog.d(BackupManagerService.TAG, "couldn't find params for token " + message.arg1);
                    }
                    break;
                }
                return;
            case 10:
                AdbRestoreParams adbRestoreParams = (AdbRestoreParams) message.obj;
                new Thread(new PerformAdbRestoreTask(this.backupManagerService, adbRestoreParams.fd, adbRestoreParams.curPassword, adbRestoreParams.encryptPassword, adbRestoreParams.observer, adbRestoreParams.latch), "adb-restore").start();
                return;
            case 12:
                ClearRetryParams clearRetryParams = (ClearRetryParams) message.obj;
                this.backupManagerService.clearBackupData(clearRetryParams.transportName, clearRetryParams.packageName);
                return;
            case 13:
                this.backupManagerService.getContext().sendBroadcastAsUser((Intent) message.obj, UserHandle.SYSTEM);
                return;
            case 14:
                new Thread((PerformFullTransportBackupTask) message.obj, "transport-backup").start();
                return;
            case 15:
                BackupParams backupParams = (BackupParams) message.obj;
                ArrayList arrayList2 = new ArrayList();
                Iterator<String> it2 = backupParams.kvPackages.iterator();
                while (it2.hasNext()) {
                    arrayList2.add(new BackupRequest(it2.next()));
                }
                this.backupManagerService.setBackupRunning(true);
                this.backupManagerService.getWakelock().acquire();
                sendMessage(obtainMessage(20, new PerformBackupTask(this.backupManagerService, backupParams.transportClient, backupParams.dirName, arrayList2, null, backupParams.observer, backupParams.monitor, backupParams.listener, backupParams.fullPackages, true, backupParams.nonIncrementalBackup)));
                return;
            case 16:
                this.backupManagerService.dataChangedImpl((String) message.obj);
                return;
            case 17:
            case 18:
                Slog.d(BackupManagerService.TAG, "Timeout message received for token=" + Integer.toHexString(message.arg1));
                this.backupManagerService.handleCancel(message.arg1, false);
                return;
            case 20:
                try {
                    ((BackupRestoreTask) message.obj).execute();
                    return;
                } catch (ClassCastException e12) {
                    Slog.e(BackupManagerService.TAG, "Invalid backup/restore task in flight, obj=" + message.obj);
                    return;
                }
            case MSG_OP_COMPLETE:
                try {
                    Pair pair = (Pair) message.obj;
                    ((BackupRestoreTask) pair.first).operationComplete(((Long) pair.second).longValue());
                    return;
                } catch (ClassCastException e13) {
                    Slog.e(BackupManagerService.TAG, "Invalid completion in flight, obj=" + message.obj);
                    return;
                }
        }
    }
}
