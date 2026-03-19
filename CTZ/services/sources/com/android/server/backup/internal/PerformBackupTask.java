package com.android.server.backup.internal;

import android.app.IBackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.WorkSource;
import android.system.ErrnoException;
import android.system.Os;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.KeyValueBackupJob;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportUtils;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class PerformBackupTask implements BackupRestoreTask {
    private static final String TAG = "PerformBackupTask";
    private BackupManagerService backupManagerService;
    private IBackupAgent mAgentBinder;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private ParcelFileDescriptor mBackupData;
    private File mBackupDataName;
    private volatile boolean mCancelAll;
    private final int mCurrentOpToken;
    private PackageInfo mCurrentPackage;
    private BackupState mCurrentState;
    private volatile int mEphemeralOpToken;
    private boolean mFinished;
    private final PerformFullTransportBackupTask mFullBackupTask;
    private DataChangedJournal mJournal;
    private final OnTaskFinishedListener mListener;
    private IBackupManagerMonitor mMonitor;
    private ParcelFileDescriptor mNewState;
    private File mNewStateName;
    private final boolean mNonIncremental;
    private IBackupObserver mObserver;
    private ArrayList<BackupRequest> mOriginalQueue;
    private List<String> mPendingFullBackups;
    private ParcelFileDescriptor mSavedState;
    private File mSavedStateName;
    private File mStateDir;
    private int mStatus;
    private final TransportClient mTransportClient;
    private final boolean mUserInitiated;
    private final Object mCancelLock = new Object();
    private ArrayList<BackupRequest> mQueue = new ArrayList<>();

    public PerformBackupTask(BackupManagerService backupManagerService, TransportClient transportClient, String str, ArrayList<BackupRequest> arrayList, DataChangedJournal dataChangedJournal, IBackupObserver iBackupObserver, IBackupManagerMonitor iBackupManagerMonitor, OnTaskFinishedListener onTaskFinishedListener, List<String> list, boolean z, boolean z2) {
        this.backupManagerService = backupManagerService;
        this.mTransportClient = transportClient;
        this.mOriginalQueue = arrayList;
        this.mJournal = dataChangedJournal;
        this.mObserver = iBackupObserver;
        this.mMonitor = iBackupManagerMonitor;
        this.mListener = onTaskFinishedListener != null ? onTaskFinishedListener : OnTaskFinishedListener.NOP;
        this.mPendingFullBackups = list;
        this.mUserInitiated = z;
        this.mNonIncremental = z2;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        this.mStateDir = new File(backupManagerService.getBaseStateDir(), str);
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mFinished = false;
        synchronized (backupManagerService.getCurrentOpLock()) {
            if (backupManagerService.isBackupOperationInProgress()) {
                Slog.d(TAG, "Skipping backup since one is already in progress.");
                this.mCancelAll = true;
                this.mFullBackupTask = null;
                this.mCurrentState = BackupState.FINAL;
                backupManagerService.addBackupTrace("Skipped. Backup already in progress.");
            } else {
                this.mCurrentState = BackupState.INITIAL;
                this.mFullBackupTask = new PerformFullTransportBackupTask(backupManagerService, transportClient, null, (String[]) this.mPendingFullBackups.toArray(new String[this.mPendingFullBackups.size()]), false, null, new CountDownLatch(1), this.mObserver, this.mMonitor, this.mListener, this.mUserInitiated);
                registerTask();
                backupManagerService.addBackupTrace("STATE => INITIAL");
            }
        }
    }

    private void registerTask() {
        synchronized (this.backupManagerService.getCurrentOpLock()) {
            this.backupManagerService.getCurrentOperations().put(this.mCurrentOpToken, new Operation(0, this, 2));
        }
    }

    private void unregisterTask() {
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$server$backup$internal$BackupState = new int[BackupState.values().length];

        static {
            try {
                $SwitchMap$com$android$server$backup$internal$BackupState[BackupState.INITIAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$backup$internal$BackupState[BackupState.BACKUP_PM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$backup$internal$BackupState[BackupState.RUNNING_QUEUE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$backup$internal$BackupState[BackupState.FINAL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    @Override
    @GuardedBy("mCancelLock")
    public void execute() {
        synchronized (this.mCancelLock) {
            switch (AnonymousClass1.$SwitchMap$com$android$server$backup$internal$BackupState[this.mCurrentState.ordinal()]) {
                case 1:
                    beginBackup();
                    break;
                case 2:
                    backupPm();
                    break;
                case 3:
                    invokeNextAgent();
                    break;
                case 4:
                    if (!this.mFinished) {
                        finalizeBackup();
                    } else {
                        Slog.e(TAG, "Duplicate finish of K/V pass");
                    }
                    break;
            }
        }
    }

    private void beginBackup() {
        this.backupManagerService.clearBackupTrace();
        StringBuilder sb = new StringBuilder(256);
        sb.append("beginBackup: [");
        for (BackupRequest backupRequest : this.mOriginalQueue) {
            sb.append(' ');
            sb.append(backupRequest.packageName);
        }
        sb.append(" ]");
        this.backupManagerService.addBackupTrace(sb.toString());
        this.mAgentBinder = null;
        this.mStatus = 0;
        if (this.mOriginalQueue.isEmpty() && this.mPendingFullBackups.isEmpty()) {
            Slog.w(TAG, "Backup begun with an empty queue - nothing to do.");
            this.backupManagerService.addBackupTrace("queue empty at begin");
            BackupObserverUtils.sendBackupFinished(this.mObserver, 0);
            executeNextState(BackupState.FINAL);
            return;
        }
        this.mQueue = (ArrayList) this.mOriginalQueue.clone();
        boolean z = this.mNonIncremental;
        int i = 0;
        while (true) {
            if (i >= this.mQueue.size()) {
                break;
            }
            if (BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(this.mQueue.get(i).packageName)) {
                this.mQueue.remove(i);
                z = false;
                break;
            }
            i++;
        }
        Slog.v(TAG, "Beginning backup of " + this.mQueue.size() + " targets");
        File file = new File(this.mStateDir, BackupManagerService.PACKAGE_MANAGER_SENTINEL);
        try {
            try {
                IBackupTransport iBackupTransportConnectOrThrow = this.mTransportClient.connectOrThrow("PBT.beginBackup()");
                String strTransportDirName = iBackupTransportConnectOrThrow.transportDirName();
                EventLog.writeEvent(EventLogTags.BACKUP_START, strTransportDirName);
                if (this.mStatus == 0 && file.length() <= 0) {
                    Slog.i(TAG, "Initializing (wiping) backup state and transport storage");
                    this.backupManagerService.addBackupTrace("initializing transport " + strTransportDirName);
                    this.backupManagerService.resetBackupState(this.mStateDir);
                    this.mStatus = iBackupTransportConnectOrThrow.initializeDevice();
                    this.backupManagerService.addBackupTrace("transport.initializeDevice() == " + this.mStatus);
                    if (this.mStatus == 0) {
                        EventLog.writeEvent(EventLogTags.BACKUP_INITIALIZE, new Object[0]);
                    } else {
                        EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
                        Slog.e(TAG, "Transport error in initializeDevice()");
                    }
                }
                if (z) {
                    Slog.d(TAG, "Skipping backup of package metadata.");
                    executeNextState(BackupState.RUNNING_QUEUE);
                } else if (this.mStatus == 0) {
                    executeNextState(BackupState.BACKUP_PM);
                }
                this.backupManagerService.addBackupTrace("exiting prelim: " + this.mStatus);
                if (this.mStatus == 0) {
                    return;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error in backup thread during init", e);
                this.backupManagerService.addBackupTrace("Exception in backup thread during init: " + e);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                this.backupManagerService.addBackupTrace("exiting prelim: " + this.mStatus);
                if (this.mStatus == 0) {
                    return;
                }
            }
            this.backupManagerService.resetBackupState(this.mStateDir);
            BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            executeNextState(BackupState.FINAL);
        } catch (Throwable th) {
            this.backupManagerService.addBackupTrace("exiting prelim: " + this.mStatus);
            if (this.mStatus != 0) {
                this.backupManagerService.resetBackupState(this.mStateDir);
                BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                executeNextState(BackupState.FINAL);
            }
            throw th;
        }
    }

    private void backupPm() {
        try {
            try {
                this.mStatus = invokeAgentForBackup(BackupManagerService.PACKAGE_MANAGER_SENTINEL, IBackupAgent.Stub.asInterface(this.backupManagerService.makeMetadataAgent().onBind()));
                this.backupManagerService.addBackupTrace("PMBA invoke: " + this.mStatus);
                this.backupManagerService.getBackupHandler().removeMessages(17);
                this.backupManagerService.addBackupTrace("exiting backupPm: " + this.mStatus);
            } catch (Exception e) {
                Slog.e(TAG, "Error in backup thread during pm", e);
                this.backupManagerService.addBackupTrace("Exception in backup thread during pm: " + e);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                this.backupManagerService.addBackupTrace("exiting backupPm: " + this.mStatus);
                if (this.mStatus != 0) {
                }
            }
            if (this.mStatus != 0) {
                this.backupManagerService.resetBackupState(this.mStateDir);
                BackupObserverUtils.sendBackupFinished(this.mObserver, invokeAgentToObserverError(this.mStatus));
                executeNextState(BackupState.FINAL);
            }
        } catch (Throwable th) {
            this.backupManagerService.addBackupTrace("exiting backupPm: " + this.mStatus);
            if (this.mStatus != 0) {
                this.backupManagerService.resetBackupState(this.mStateDir);
                BackupObserverUtils.sendBackupFinished(this.mObserver, invokeAgentToObserverError(this.mStatus));
                executeNextState(BackupState.FINAL);
            }
            throw th;
        }
    }

    private int invokeAgentToObserverError(int i) {
        if (i == -1003) {
            return -1003;
        }
        return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
    }

    private void invokeNextAgent() {
        BackupState backupState;
        PackageManager packageManager;
        this.mStatus = 0;
        this.backupManagerService.addBackupTrace("invoke q=" + this.mQueue.size());
        if (this.mQueue.isEmpty()) {
            executeNextState(BackupState.FINAL);
            return;
        }
        BackupRequest backupRequest = this.mQueue.get(0);
        this.mQueue.remove(0);
        Slog.d(TAG, "starting key/value backup of " + backupRequest);
        this.backupManagerService.addBackupTrace("launch agent for " + backupRequest.packageName);
        try {
            try {
                packageManager = this.backupManagerService.getPackageManager();
                this.mCurrentPackage = packageManager.getPackageInfo(backupRequest.packageName, 134217728);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.d(TAG, "Package does not exist; skipping");
                this.backupManagerService.addBackupTrace("no such package");
                this.mStatus = -1004;
                this.backupManagerService.getWakelock().setWorkSource(null);
                if (this.mStatus != 0) {
                    backupState = BackupState.RUNNING_QUEUE;
                    this.mAgentBinder = null;
                    if (this.mStatus == -1003) {
                        this.backupManagerService.dataChangedImpl(backupRequest.packageName);
                        this.mStatus = 0;
                        if (this.mQueue.isEmpty()) {
                            backupState = BackupState.FINAL;
                        }
                    } else if (this.mStatus == -1004) {
                    }
                }
                this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
            }
            if (!AppBackupUtils.appIsEligibleForBackup(this.mCurrentPackage.applicationInfo, packageManager)) {
                Slog.i(TAG, "Package " + backupRequest.packageName + " no longer supports backup; skipping");
                this.backupManagerService.addBackupTrace("skipping - not eligible, completion is noop");
                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                executeNextState(BackupState.RUNNING_QUEUE);
                this.backupManagerService.getWakelock().setWorkSource(null);
                if (this.mStatus == 0) {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                    return;
                }
                BackupState backupState2 = BackupState.RUNNING_QUEUE;
                this.mAgentBinder = null;
                if (this.mStatus == -1003) {
                    this.backupManagerService.dataChangedImpl(backupRequest.packageName);
                    this.mStatus = 0;
                    if (this.mQueue.isEmpty()) {
                        backupState2 = BackupState.FINAL;
                    }
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                } else if (this.mStatus == -1004) {
                    this.mStatus = 0;
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, backupRequest.packageName, -2002);
                } else {
                    revertAndEndBackup();
                    backupState2 = BackupState.FINAL;
                }
                executeNextState(backupState2);
                return;
            }
            if (AppBackupUtils.appGetsFullBackup(this.mCurrentPackage)) {
                Slog.i(TAG, "Package " + backupRequest.packageName + " requests full-data rather than key/value; skipping");
                this.backupManagerService.addBackupTrace("skipping - fullBackupOnly, completion is noop");
                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                executeNextState(BackupState.RUNNING_QUEUE);
                this.backupManagerService.getWakelock().setWorkSource(null);
                if (this.mStatus == 0) {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                    return;
                }
                BackupState backupState3 = BackupState.RUNNING_QUEUE;
                this.mAgentBinder = null;
                if (this.mStatus == -1003) {
                    this.backupManagerService.dataChangedImpl(backupRequest.packageName);
                    this.mStatus = 0;
                    if (this.mQueue.isEmpty()) {
                        backupState3 = BackupState.FINAL;
                    }
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                } else if (this.mStatus == -1004) {
                    this.mStatus = 0;
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, backupRequest.packageName, -2002);
                } else {
                    revertAndEndBackup();
                    backupState3 = BackupState.FINAL;
                }
                executeNextState(backupState3);
                return;
            }
            if (AppBackupUtils.appIsStopped(this.mCurrentPackage.applicationInfo)) {
                this.backupManagerService.addBackupTrace("skipping - stopped");
                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                executeNextState(BackupState.RUNNING_QUEUE);
                this.backupManagerService.getWakelock().setWorkSource(null);
                if (this.mStatus == 0) {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                    return;
                }
                BackupState backupState4 = BackupState.RUNNING_QUEUE;
                this.mAgentBinder = null;
                if (this.mStatus == -1003) {
                    this.backupManagerService.dataChangedImpl(backupRequest.packageName);
                    this.mStatus = 0;
                    if (this.mQueue.isEmpty()) {
                        backupState4 = BackupState.FINAL;
                    }
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                } else if (this.mStatus == -1004) {
                    this.mStatus = 0;
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, backupRequest.packageName, -2002);
                } else {
                    revertAndEndBackup();
                    backupState4 = BackupState.FINAL;
                }
                executeNextState(backupState4);
                return;
            }
            try {
                this.backupManagerService.getWakelock().setWorkSource(new WorkSource(this.mCurrentPackage.applicationInfo.uid));
                IBackupAgent iBackupAgentBindToAgentSynchronous = this.backupManagerService.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0);
                BackupManagerService backupManagerService = this.backupManagerService;
                StringBuilder sb = new StringBuilder();
                sb.append("agent bound; a? = ");
                sb.append(iBackupAgentBindToAgentSynchronous != null);
                backupManagerService.addBackupTrace(sb.toString());
                if (iBackupAgentBindToAgentSynchronous != null) {
                    this.mAgentBinder = iBackupAgentBindToAgentSynchronous;
                    this.mStatus = invokeAgentForBackup(backupRequest.packageName, iBackupAgentBindToAgentSynchronous);
                } else {
                    this.mStatus = -1003;
                }
            } catch (SecurityException e2) {
                Slog.d(TAG, "error in bind/backup", e2);
                this.mStatus = -1003;
                this.backupManagerService.addBackupTrace("agent SE");
            }
            this.backupManagerService.getWakelock().setWorkSource(null);
            if (this.mStatus != 0) {
                backupState = BackupState.RUNNING_QUEUE;
                this.mAgentBinder = null;
                if (this.mStatus == -1003) {
                    this.backupManagerService.dataChangedImpl(backupRequest.packageName);
                    this.mStatus = 0;
                    if (this.mQueue.isEmpty()) {
                        backupState = BackupState.FINAL;
                    }
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                    executeNextState(backupState);
                    return;
                }
                if (this.mStatus == -1004) {
                    this.mStatus = 0;
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, backupRequest.packageName, -2002);
                } else {
                    revertAndEndBackup();
                    backupState = BackupState.FINAL;
                }
                executeNextState(backupState);
                return;
            }
            this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
        } catch (Throwable th) {
            this.backupManagerService.getWakelock().setWorkSource(null);
            if (this.mStatus != 0) {
                BackupState backupState5 = BackupState.RUNNING_QUEUE;
                this.mAgentBinder = null;
                if (this.mStatus == -1003) {
                    this.backupManagerService.dataChangedImpl(backupRequest.packageName);
                    this.mStatus = 0;
                    if (this.mQueue.isEmpty()) {
                        backupState5 = BackupState.FINAL;
                    }
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                } else if (this.mStatus == -1004) {
                    this.mStatus = 0;
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, backupRequest.packageName, -2002);
                } else {
                    revertAndEndBackup();
                    backupState5 = BackupState.FINAL;
                }
                executeNextState(backupState5);
            } else {
                this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
            }
            throw th;
        }
    }

    private void finalizeBackup() {
        this.backupManagerService.addBackupTrace("finishing");
        Iterator<BackupRequest> it = this.mQueue.iterator();
        while (it.hasNext()) {
            this.backupManagerService.dataChangedImpl(it.next().packageName);
        }
        if (this.mJournal != null && !this.mJournal.delete()) {
            Slog.e(TAG, "Unable to remove backup journal file " + this.mJournal);
        }
        if (this.backupManagerService.getCurrentToken() == 0 && this.mStatus == 0) {
            this.backupManagerService.addBackupTrace("success; recording token");
            try {
                this.backupManagerService.setCurrentToken(this.mTransportClient.connectOrThrow("PBT.finalizeBackup()").getCurrentRestoreSet());
                this.backupManagerService.writeRestoreTokens();
            } catch (Exception e) {
                Slog.e(TAG, "Transport threw reporting restore set: " + e.getMessage());
                this.backupManagerService.addBackupTrace("transport threw returning token");
            }
        }
        synchronized (this.backupManagerService.getQueueLock()) {
            this.backupManagerService.setBackupRunning(false);
            if (this.mStatus == -1001) {
                this.backupManagerService.addBackupTrace("init required; rerunning");
                try {
                    this.backupManagerService.getPendingInits().add(this.backupManagerService.getTransportManager().getTransportName(this.mTransportClient.getTransportComponent()));
                } catch (Exception e2) {
                    Slog.w(TAG, "Failed to query transport name for init: " + e2.getMessage());
                }
                clearMetadata();
                this.backupManagerService.backupNow();
            }
        }
        this.backupManagerService.clearBackupTrace();
        unregisterTask();
        if (!this.mCancelAll && this.mStatus == 0 && this.mPendingFullBackups != null && !this.mPendingFullBackups.isEmpty()) {
            Slog.d(TAG, "Starting full backups for: " + this.mPendingFullBackups);
            this.backupManagerService.getWakelock().acquire();
            new Thread(this.mFullBackupTask, "full-transport-requested").start();
        } else if (this.mCancelAll) {
            this.mListener.onFinished("PBT.finalizeBackup()");
            if (this.mFullBackupTask != null) {
                this.mFullBackupTask.unregisterTask();
            }
            BackupObserverUtils.sendBackupFinished(this.mObserver, -2003);
        } else {
            this.mListener.onFinished("PBT.finalizeBackup()");
            this.mFullBackupTask.unregisterTask();
            int i = this.mStatus;
            if (i == -1005 || i == 0) {
                BackupObserverUtils.sendBackupFinished(this.mObserver, 0);
            } else {
                switch (i) {
                    case JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS:
                        break;
                    case JobSchedulerShellCommand.CMD_ERR_NO_JOB:
                        BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        break;
                    default:
                        BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        break;
                }
            }
        }
        this.mFinished = true;
        Slog.i(TAG, "K/V backup pass finished.");
        this.backupManagerService.getWakelock().release();
    }

    private void clearMetadata() {
        File file = new File(this.mStateDir, BackupManagerService.PACKAGE_MANAGER_SENTINEL);
        if (file.exists()) {
            file.delete();
        }
    }

    private int invokeAgentForBackup(String str, IBackupAgent iBackupAgent) {
        boolean z;
        IBackupTransport iBackupTransportConnectOrThrow;
        long backupQuota;
        Slog.d(TAG, "invokeAgentForBackup on " + str);
        this.backupManagerService.addBackupTrace("invoking " + str);
        File file = new File(this.mStateDir, "blank_state");
        this.mSavedStateName = new File(this.mStateDir, str);
        this.mBackupDataName = new File(this.backupManagerService.getDataDir(), str + ".data");
        this.mNewStateName = new File(this.mStateDir, str + ".new");
        this.mSavedState = null;
        this.mBackupData = null;
        this.mNewState = null;
        this.mEphemeralOpToken = this.backupManagerService.generateRandomIntegerToken();
        try {
            try {
                if (str.equals(BackupManagerService.PACKAGE_MANAGER_SENTINEL)) {
                    this.mCurrentPackage = new PackageInfo();
                    this.mCurrentPackage.packageName = str;
                }
                this.mSavedState = ParcelFileDescriptor.open(this.mNonIncremental ? file : this.mSavedStateName, 402653184);
                this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
                if (!SELinux.restorecon(this.mBackupDataName)) {
                    Slog.e(TAG, "SELinux restorecon failed on " + this.mBackupDataName);
                }
                this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
                iBackupTransportConnectOrThrow = this.mTransportClient.connectOrThrow("PBT.invokeAgentForBackup()");
                backupQuota = iBackupTransportConnectOrThrow.getBackupQuota(str, false);
            } catch (Exception e) {
                e = e;
                z = false;
            }
            try {
                this.backupManagerService.addBackupTrace("setting timeout");
                this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis(), this, 0);
                this.backupManagerService.addBackupTrace("calling agent doBackup()");
                iBackupAgent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, backupQuota, this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder(), iBackupTransportConnectOrThrow.getTransportFlags());
                this.backupManagerService.addBackupTrace("invoke success");
                return 0;
            } catch (Exception e2) {
                e = e2;
                z = true;
                Slog.e(TAG, "Error invoking for backup on " + str + ". " + e);
                BackupManagerService backupManagerService = this.backupManagerService;
                StringBuilder sb = new StringBuilder();
                sb.append("exception: ");
                sb.append(e);
                backupManagerService.addBackupTrace(sb.toString());
                EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, str, e.toString());
                errorCleanup();
                int i = z ? -1003 : JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                if (this.mNonIncremental) {
                    file.delete();
                }
                return i;
            }
        } finally {
            if (this.mNonIncremental) {
                file.delete();
            }
        }
    }

    private void failAgent(IBackupAgent iBackupAgent, String str) {
        try {
            iBackupAgent.fail(str);
        } catch (Exception e) {
            Slog.w(TAG, "Error conveying failure to " + this.mCurrentPackage.packageName);
        }
    }

    private String SHA1Checksum(byte[] bArr) {
        try {
            byte[] bArrDigest = MessageDigest.getInstance("SHA-1").digest(bArr);
            StringBuffer stringBuffer = new StringBuffer(bArrDigest.length * 2);
            for (byte b : bArrDigest) {
                stringBuffer.append(Integer.toHexString(b));
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            Slog.e(TAG, "Unable to use SHA-1!");
            return "00";
        }
    }

    private void writeWidgetPayloadIfAppropriate(FileDescriptor fileDescriptor, String str) throws Exception {
        String strSHA1Checksum;
        Throwable th;
        Throwable th2;
        byte[] widgetState = AppWidgetBackupBridge.getWidgetState(str, 0);
        File file = new File(this.mStateDir, str + "_widget");
        boolean zExists = file.exists();
        if (zExists || widgetState != null) {
            if (widgetState != null) {
                strSHA1Checksum = SHA1Checksum(widgetState);
                if (zExists) {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    try {
                        DataInputStream dataInputStream = new DataInputStream(fileInputStream);
                        try {
                            String utf = dataInputStream.readUTF();
                            $closeResource(null, dataInputStream);
                            $closeResource(null, fileInputStream);
                            if (Objects.equals(strSHA1Checksum, utf)) {
                                return;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            th2 = null;
                            $closeResource(th2, dataInputStream);
                            throw th;
                        }
                    } catch (Throwable th4) {
                        $closeResource(null, fileInputStream);
                        throw th4;
                    }
                }
            } else {
                strSHA1Checksum = null;
            }
            BackupDataOutput backupDataOutput = new BackupDataOutput(fileDescriptor);
            if (widgetState == null) {
                backupDataOutput.writeEntityHeader(BackupManagerService.KEY_WIDGET_STATE, -1);
                file.delete();
                return;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
                try {
                    dataOutputStream.writeUTF(strSHA1Checksum);
                    $closeResource(null, dataOutputStream);
                    $closeResource(null, fileOutputStream);
                    backupDataOutput.writeEntityHeader(BackupManagerService.KEY_WIDGET_STATE, widgetState.length);
                    backupDataOutput.writeEntityData(widgetState, widgetState.length);
                } catch (Throwable th5) {
                    th = th5;
                    th = null;
                    $closeResource(th, dataOutputStream);
                    throw th;
                }
            } catch (Throwable th6) {
                $closeResource(null, fileOutputStream);
                throw th6;
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @Override
    @GuardedBy("mCancelLock")
    public void operationComplete(long j) {
        long length;
        Throwable th;
        ParcelFileDescriptor parcelFileDescriptorOpen;
        BackupState backupState;
        int i;
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        synchronized (this.mCancelLock) {
            if (this.mFinished) {
                Slog.d(TAG, "operationComplete received after task finished.");
                return;
            }
            if (this.mBackupData == null) {
                String str = this.mCurrentPackage != null ? this.mCurrentPackage.packageName : "[none]";
                this.backupManagerService.addBackupTrace("late opComplete; curPkg = " + str);
                return;
            }
            String str2 = this.mCurrentPackage.packageName;
            long length2 = this.mBackupDataName.length();
            FileDescriptor fileDescriptor = this.mBackupData.getFileDescriptor();
            ParcelFileDescriptor parcelFileDescriptor = null;
            char c = 1;
            try {
                if (this.mCurrentPackage.applicationInfo != null && (this.mCurrentPackage.applicationInfo.flags & 1) == 0) {
                    ParcelFileDescriptor parcelFileDescriptorOpen2 = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                    BackupDataInput backupDataInput = new BackupDataInput(parcelFileDescriptorOpen2.getFileDescriptor());
                    while (backupDataInput.readNextHeader()) {
                        try {
                            String key = backupDataInput.getKey();
                            if (key != null && key.charAt(0) >= 65280) {
                                failAgent(this.mAgentBinder, "Illegal backup key: " + key);
                                this.backupManagerService.addBackupTrace("illegal key " + key + " from " + str2);
                                Object[] objArr = new Object[2];
                                objArr[0] = str2;
                                objArr[c] = "bad key";
                                EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, objArr);
                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 5, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_ILLEGAL_KEY", key));
                                this.backupManagerService.getBackupHandler().removeMessages(17);
                                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, str2, -1003);
                                errorCleanup();
                                return;
                            }
                            backupDataInput.skipEntityData();
                            c = 1;
                        } finally {
                            if (parcelFileDescriptorOpen2 != null) {
                                parcelFileDescriptorOpen2.close();
                            }
                        }
                    }
                    if (parcelFileDescriptorOpen2 != null) {
                        parcelFileDescriptorOpen2.close();
                    }
                }
                writeWidgetPayloadIfAppropriate(fileDescriptor, str2);
            } catch (IOException e) {
                Slog.w(TAG, "Unable to save widget state for " + str2);
                try {
                    Os.ftruncate(fileDescriptor, length2);
                } catch (ErrnoException e2) {
                    Slog.w(TAG, "Unable to roll back!");
                }
            }
            this.backupManagerService.getBackupHandler().removeMessages(17);
            clearAgentState();
            this.backupManagerService.addBackupTrace("operation complete");
            IBackupTransport iBackupTransportConnect = this.mTransportClient.connect("PBT.operationComplete()");
            this.mStatus = 0;
            try {
                try {
                    try {
                        TransportUtils.checkTransportNotNull(iBackupTransportConnect);
                        length = this.mBackupDataName.length();
                        try {
                            if (length > 0) {
                                boolean z = this.mSavedStateName.length() == 0;
                                if (this.mStatus == 0) {
                                    parcelFileDescriptorOpen = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                                    try {
                                        this.backupManagerService.addBackupTrace("sending data to transport");
                                        boolean z2 = this.mUserInitiated;
                                        if (z) {
                                            i = 4;
                                        } else {
                                            i = 2;
                                        }
                                        this.mStatus = iBackupTransportConnect.performBackup(this.mCurrentPackage, parcelFileDescriptorOpen, (z2 ? 1 : 0) | i);
                                    } catch (Exception e3) {
                                        e = e3;
                                        parcelFileDescriptor = parcelFileDescriptorOpen;
                                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, str2, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                        Slog.e(TAG, "Transport error backing up " + str2, e);
                                        EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, str2);
                                        this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                        if (parcelFileDescriptor != null) {
                                            parcelFileDescriptor.close();
                                        }
                                        if (this.mStatus == 0) {
                                            if (!this.mQueue.isEmpty()) {
                                            }
                                        }
                                        executeNextState(backupState);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        if (parcelFileDescriptorOpen != null) {
                                            try {
                                                parcelFileDescriptorOpen.close();
                                                throw th;
                                            } catch (IOException e4) {
                                                throw th;
                                            }
                                        }
                                        throw th;
                                    }
                                } else {
                                    parcelFileDescriptorOpen = null;
                                }
                                if (z && this.mStatus == -1006) {
                                    Slog.w(TAG, "Transport requested non-incremental but already the case, error");
                                    this.backupManagerService.addBackupTrace("Transport requested non-incremental but already the case, error");
                                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                }
                                this.backupManagerService.addBackupTrace("data delivered: " + this.mStatus);
                                if (this.mStatus == 0) {
                                    this.backupManagerService.addBackupTrace("finishing op on transport");
                                    this.mStatus = iBackupTransportConnect.finishBackup();
                                    this.backupManagerService.addBackupTrace("finished: " + this.mStatus);
                                } else if (this.mStatus == -1002) {
                                    this.backupManagerService.addBackupTrace("transport rejected package");
                                }
                            } else {
                                this.backupManagerService.addBackupTrace("no data to send");
                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 7, this.mCurrentPackage, 3, null);
                                parcelFileDescriptorOpen = null;
                            }
                            if (this.mStatus == 0) {
                                this.mBackupDataName.delete();
                                this.mNewStateName.renameTo(this.mSavedStateName);
                                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, str2, 0);
                                EventLog.writeEvent(EventLogTags.BACKUP_PACKAGE, str2, Long.valueOf(length));
                                this.backupManagerService.logBackupComplete(str2);
                            } else if (this.mStatus == -1002) {
                                this.mBackupDataName.delete();
                                this.mNewStateName.delete();
                                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, str2, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
                                EventLogTags.writeBackupAgentFailure(str2, "Transport rejected");
                            } else if (this.mStatus != -1005) {
                                if (this.mStatus == -1006) {
                                    Slog.i(TAG, "Transport lost data, retrying package");
                                    this.backupManagerService.addBackupTrace("Transport lost data, retrying package:" + str2);
                                    BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 51, this.mCurrentPackage, 1, null);
                                    this.mBackupDataName.delete();
                                    this.mSavedStateName.delete();
                                    this.mNewStateName.delete();
                                    if (!BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(str2)) {
                                        this.mQueue.add(0, new BackupRequest(str2));
                                    }
                                } else {
                                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, str2, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                    EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, str2);
                                }
                            } else {
                                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, str2, -1005);
                                EventLog.writeEvent(EventLogTags.BACKUP_QUOTA_EXCEEDED, str2);
                            }
                        } catch (Exception e5) {
                            e = e5;
                        }
                    } catch (IOException e6) {
                    }
                } catch (Throwable th3) {
                    th = th3;
                    parcelFileDescriptorOpen = null;
                }
            } catch (Exception e7) {
                e = e7;
                length = 0;
            }
            if (parcelFileDescriptorOpen != null) {
                parcelFileDescriptorOpen.close();
            }
            if (this.mStatus == 0 && this.mStatus != -1002) {
                if (this.mStatus == -1006) {
                    if (BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(str2)) {
                        backupState = BackupState.BACKUP_PM;
                    } else {
                        backupState = BackupState.RUNNING_QUEUE;
                    }
                } else if (this.mStatus == -1005) {
                    if (this.mAgentBinder != null) {
                        try {
                            TransportUtils.checkTransportNotNull(iBackupTransportConnect);
                            this.mAgentBinder.doQuotaExceeded(length, iBackupTransportConnect.getBackupQuota(this.mCurrentPackage.packageName, false));
                        } catch (Exception e8) {
                            Slog.e(TAG, "Unable to notify about quota exceeded: " + e8.getMessage());
                        }
                    }
                    backupState = this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE;
                } else {
                    revertAndEndBackup();
                    backupState = BackupState.FINAL;
                }
            } else {
                backupState = !this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE;
            }
            executeNextState(backupState);
        }
    }

    @Override
    @GuardedBy("mCancelLock")
    public void handleCancel(boolean z) {
        String str;
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        synchronized (this.mCancelLock) {
            if (this.mFinished) {
                return;
            }
            this.mCancelAll = z;
            if (this.mCurrentPackage != null) {
                str = this.mCurrentPackage.packageName;
            } else {
                str = "no_package_yet";
            }
            Slog.i(TAG, "Cancel backing up " + str);
            EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, str);
            this.backupManagerService.addBackupTrace("cancel of " + str + ", cancelAll=" + z);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 21, this.mCurrentPackage, 2, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_CANCEL_ALL", this.mCancelAll));
            errorCleanup();
            if (!z) {
                executeNextState(this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE);
                this.backupManagerService.dataChangedImpl(this.mCurrentPackage.packageName);
            } else {
                finalizeBackup();
            }
        }
    }

    private void revertAndEndBackup() {
        long jRequestBackupTime;
        this.backupManagerService.addBackupTrace("transport error; reverting");
        try {
            jRequestBackupTime = this.mTransportClient.connectOrThrow("PBT.revertAndEndBackup()").requestBackupTime();
        } catch (Exception e) {
            Slog.w(TAG, "Unable to contact transport for recommended backoff: " + e.getMessage());
            jRequestBackupTime = 0;
        }
        KeyValueBackupJob.schedule(this.backupManagerService.getContext(), jRequestBackupTime, this.backupManagerService.getConstants());
        Iterator<BackupRequest> it = this.mOriginalQueue.iterator();
        while (it.hasNext()) {
            this.backupManagerService.dataChangedImpl(it.next().packageName);
        }
    }

    private void errorCleanup() {
        this.mBackupDataName.delete();
        this.mNewStateName.delete();
        clearAgentState();
    }

    private void clearAgentState() {
        try {
            if (this.mSavedState != null) {
                this.mSavedState.close();
            }
        } catch (IOException e) {
        }
        try {
            if (this.mBackupData != null) {
                this.mBackupData.close();
            }
        } catch (IOException e2) {
        }
        try {
            if (this.mNewState != null) {
                this.mNewState.close();
            }
        } catch (IOException e3) {
        }
        synchronized (this.backupManagerService.getCurrentOpLock()) {
            this.backupManagerService.getCurrentOperations().remove(this.mEphemeralOpToken);
            this.mNewState = null;
            this.mBackupData = null;
            this.mSavedState = null;
        }
        if (this.mCurrentPackage.applicationInfo != null) {
            this.backupManagerService.addBackupTrace("unbinding " + this.mCurrentPackage.packageName);
            try {
                this.backupManagerService.getActivityManager().unbindBackupAgent(this.mCurrentPackage.applicationInfo);
            } catch (RemoteException e4) {
            }
        }
    }

    private void executeNextState(BackupState backupState) {
        this.backupManagerService.addBackupTrace("executeNextState => " + backupState);
        this.mCurrentState = backupState;
        this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, this));
    }
}
