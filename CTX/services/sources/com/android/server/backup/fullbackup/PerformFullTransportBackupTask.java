package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.app.backup.BackupProgress;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FullBackupJob;
import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.internal.Operation;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PerformFullTransportBackupTask extends FullBackupTask implements BackupRestoreTask {
    private static final String TAG = "PFTBT";
    private BackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    IBackupObserver mBackupObserver;
    SinglePackageBackupRunner mBackupRunner;
    private final int mBackupRunnerOpToken;
    private volatile boolean mCancelAll;
    private final Object mCancelLock;
    private final int mCurrentOpToken;
    PackageInfo mCurrentPackage;
    private volatile boolean mIsDoingBackup;
    FullBackupJob mJob;
    CountDownLatch mLatch;
    private final OnTaskFinishedListener mListener;
    IBackupManagerMonitor mMonitor;
    ArrayList<PackageInfo> mPackages;
    private final TransportClient mTransportClient;
    boolean mUpdateSchedule;
    boolean mUserInitiated;

    public static PerformFullTransportBackupTask newWithCurrentTransport(BackupManagerService backupManagerService, IFullBackupRestoreObserver iFullBackupRestoreObserver, String[] strArr, boolean z, FullBackupJob fullBackupJob, CountDownLatch countDownLatch, IBackupObserver iBackupObserver, IBackupManagerMonitor iBackupManagerMonitor, boolean z2, String str) {
        final TransportManager transportManager = backupManagerService.getTransportManager();
        final TransportClient currentTransportClient = transportManager.getCurrentTransportClient(str);
        return new PerformFullTransportBackupTask(backupManagerService, currentTransportClient, iFullBackupRestoreObserver, strArr, z, fullBackupJob, countDownLatch, iBackupObserver, iBackupManagerMonitor, new OnTaskFinishedListener() {
            @Override
            public final void onFinished(String str2) {
                transportManager.disposeOfTransportClient(currentTransportClient, str2);
            }
        }, z2);
    }

    public PerformFullTransportBackupTask(BackupManagerService backupManagerService, TransportClient transportClient, IFullBackupRestoreObserver iFullBackupRestoreObserver, String[] strArr, boolean z, FullBackupJob fullBackupJob, CountDownLatch countDownLatch, IBackupObserver iBackupObserver, IBackupManagerMonitor iBackupManagerMonitor, OnTaskFinishedListener onTaskFinishedListener, boolean z2) {
        super(iFullBackupRestoreObserver);
        this.mCancelLock = new Object();
        this.backupManagerService = backupManagerService;
        this.mTransportClient = transportClient;
        this.mUpdateSchedule = z;
        this.mLatch = countDownLatch;
        this.mJob = fullBackupJob;
        this.mPackages = new ArrayList<>(strArr.length);
        this.mBackupObserver = iBackupObserver;
        this.mMonitor = iBackupManagerMonitor;
        this.mListener = onTaskFinishedListener == null ? OnTaskFinishedListener.NOP : onTaskFinishedListener;
        this.mUserInitiated = z2;
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mBackupRunnerOpToken = backupManagerService.generateRandomIntegerToken();
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        if (backupManagerService.isBackupOperationInProgress()) {
            Slog.d(TAG, "Skipping full backup. A backup is already in progress.");
            this.mCancelAll = true;
            return;
        }
        registerTask();
        for (String str : strArr) {
            try {
                PackageManager packageManager = backupManagerService.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(str, 134217728);
                this.mCurrentPackage = packageInfo;
                if (!AppBackupUtils.appIsEligibleForBackup(packageInfo.applicationInfo, packageManager)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 9, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str, -2001);
                } else if (!AppBackupUtils.appGetsFullBackup(packageInfo)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 10, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str, -2001);
                } else if (AppBackupUtils.appIsStopped(packageInfo.applicationInfo)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 11, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str, -2001);
                } else {
                    this.mPackages.add(packageInfo);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.i(TAG, "Requested package " + str + " not found; ignoring");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 12, this.mCurrentPackage, 3, null);
            }
        }
    }

    private void registerTask() {
        synchronized (this.backupManagerService.getCurrentOpLock()) {
            Slog.d(TAG, "backupmanager pftbt token=" + Integer.toHexString(this.mCurrentOpToken));
            this.backupManagerService.getCurrentOperations().put(this.mCurrentOpToken, new Operation(0, this, 2));
        }
    }

    public void unregisterTask() {
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }

    @Override
    public void execute() {
    }

    @Override
    public void handleCancel(boolean z) {
        synchronized (this.mCancelLock) {
            if (!z) {
                try {
                    Slog.wtf(TAG, "Expected cancelAll to be true.");
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (this.mCancelAll) {
                Slog.d(TAG, "Ignoring duplicate cancel call.");
                return;
            }
            this.mCancelAll = true;
            if (this.mIsDoingBackup) {
                this.backupManagerService.handleCancel(this.mBackupRunnerOpToken, z);
                try {
                    this.mTransportClient.getConnectedTransport("PFTBT.handleCancel()").cancelFullBackup();
                } catch (RemoteException | TransportNotAvailableException e) {
                    Slog.w(TAG, "Error calling cancelFullBackup() on transport: " + e);
                }
            }
        }
    }

    @Override
    public void operationComplete(long j) {
    }

    @Override
    public void run() throws Throwable {
        int i;
        int i2;
        int i3;
        ?? r8;
        ParcelFileDescriptor[] parcelFileDescriptorArr;
        ?? r11;
        ?? CreatePipe;
        ?? r112;
        ?? r82;
        ?? r83;
        ParcelFileDescriptor[] parcelFileDescriptorArr2;
        int i4;
        byte[] bArr;
        int i5;
        IBackupTransport iBackupTransport;
        Bundle bundle;
        long j;
        ?? r12;
        byte[] bArr2;
        IBackupTransport iBackupTransport2;
        int backupResultBlocking;
        boolean z;
        int i6;
        FileInputStream fileInputStream;
        FileOutputStream fileOutputStream;
        int i7;
        long j2;
        int i8;
        int i9;
        int i10;
        long jRequestFullBackupTime = 0;
        char c = 0;
        ?? r84 = 0;
        SinglePackageBackupRunner singlePackageBackupRunner = null;
        try {
        } catch (Exception e) {
            e = e;
            i = -2003;
            i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        } catch (Throwable th) {
            th = th;
            i = -2003;
            i2 = 0;
        }
        if (this.backupManagerService.isEnabled()) {
            try {
                if (this.backupManagerService.isProvisioned()) {
                    IBackupTransport iBackupTransportConnect = this.mTransportClient.connect("PFTBT.run()");
                    ?? r7 = 1;
                    if (iBackupTransportConnect != null) {
                        int size = this.mPackages.size();
                        byte[] bArr3 = new byte[8192];
                        parcelFileDescriptorArr = null;
                        ?? r2 = 0;
                        int i11 = 0;
                        long j3 = 0;
                        while (true) {
                            if (i11 >= size) {
                                break;
                            }
                            try {
                                this.mBackupRunner = singlePackageBackupRunner;
                                PackageInfo packageInfo = this.mPackages.get(i11);
                                String str = packageInfo.packageName;
                                Slog.i(TAG, "Initiating full-data transport backup of " + str + " token: " + this.mCurrentOpToken);
                                EventLog.writeEvent(EventLogTags.FULL_BACKUP_PACKAGE, str);
                                CreatePipe = ParcelFileDescriptor.createPipe();
                                try {
                                    boolean z2 = this.mUserInitiated;
                                    synchronized (this.mCancelLock) {
                                        try {
                                            if (this.mCancelAll) {
                                                break;
                                            }
                                            int iPerformFullBackup = iBackupTransportConnect.performFullBackup(packageInfo, (ParcelFileDescriptor) CreatePipe[c], z2 ? 1 : 0);
                                            if (iPerformFullBackup == 0) {
                                                try {
                                                    long backupQuota = iBackupTransportConnect.getBackupQuota(packageInfo.packageName, (boolean) r7);
                                                    ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
                                                    try {
                                                        i4 = i11;
                                                        bArr = bArr3;
                                                        i5 = size;
                                                        iBackupTransport = iBackupTransportConnect;
                                                        ?? r122 = r7;
                                                        this.mBackupRunner = new SinglePackageBackupRunner(parcelFileDescriptorArrCreatePipe[r7], packageInfo, this.mTransportClient, backupQuota, this.mBackupRunnerOpToken, iBackupTransportConnect.getTransportFlags());
                                                        parcelFileDescriptorArrCreatePipe[r122 == true ? 1 : 0].close();
                                                        bundle = null;
                                                        parcelFileDescriptorArrCreatePipe[r122 == true ? 1 : 0] = null;
                                                        this.mIsDoingBackup = r122;
                                                        j = backupQuota;
                                                        parcelFileDescriptorArr = parcelFileDescriptorArrCreatePipe;
                                                        r12 = r122;
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        parcelFileDescriptorArr2 = parcelFileDescriptorArrCreatePipe;
                                                        i = -2003;
                                                        i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                                        while (true) {
                                                            try {
                                                                try {
                                                                    throw th;
                                                                } catch (Exception e2) {
                                                                    e = e2;
                                                                    parcelFileDescriptorArr = parcelFileDescriptorArr2;
                                                                    r83 = CreatePipe;
                                                                    jRequestFullBackupTime = j3;
                                                                    r8 = r83;
                                                                    Slog.w(TAG, "Exception trying full transport backup", e);
                                                                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                                                                    if (!this.mCancelAll) {
                                                                    }
                                                                    Slog.i(TAG, "Full backup completed with status: " + i);
                                                                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                                                                    cleanUpPipes(r8);
                                                                    cleanUpPipes(parcelFileDescriptorArr);
                                                                    unregisterTask();
                                                                    if (this.mJob != null) {
                                                                    }
                                                                    synchronized (this.backupManagerService.getQueueLock()) {
                                                                    }
                                                                } catch (Throwable th3) {
                                                                    th = th3;
                                                                    parcelFileDescriptorArr = parcelFileDescriptorArr2;
                                                                    CreatePipe = CreatePipe;
                                                                    jRequestFullBackupTime = j3;
                                                                    i2 = 0;
                                                                    r11 = CreatePipe;
                                                                    if (this.mCancelAll) {
                                                                    }
                                                                    Slog.i(TAG, "Full backup completed with status: " + i2);
                                                                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i2);
                                                                    cleanUpPipes(r11);
                                                                    cleanUpPipes(parcelFileDescriptorArr);
                                                                    unregisterTask();
                                                                    if (this.mJob != null) {
                                                                    }
                                                                    synchronized (this.backupManagerService.getQueueLock()) {
                                                                    }
                                                                }
                                                            } catch (Throwable th4) {
                                                                th = th4;
                                                            }
                                                        }
                                                    }
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                    parcelFileDescriptorArr2 = parcelFileDescriptorArr;
                                                }
                                            } else {
                                                i4 = i11;
                                                bArr = bArr3;
                                                i5 = size;
                                                iBackupTransport = iBackupTransportConnect;
                                                r12 = r7;
                                                bundle = null;
                                                j = Long.MAX_VALUE;
                                            }
                                            if (iPerformFullBackup == 0) {
                                                int i12 = 0;
                                                try {
                                                    try {
                                                        CreatePipe[0].close();
                                                        CreatePipe[0] = bundle;
                                                        new Thread(this.mBackupRunner, "package-backup-bridge").start();
                                                        FileInputStream fileInputStream2 = new FileInputStream(parcelFileDescriptorArr[0].getFileDescriptor());
                                                        FileOutputStream fileOutputStream2 = new FileOutputStream(CreatePipe[r12].getFileDescriptor());
                                                        long preflightResultBlocking = this.mBackupRunner.getPreflightResultBlocking();
                                                        int i13 = (preflightResultBlocking > 0L ? 1 : (preflightResultBlocking == 0L ? 0 : -1));
                                                        if (i13 < 0) {
                                                            try {
                                                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 16, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(bundle, "android.app.backup.extra.LOG_PREFLIGHT_ERROR", preflightResultBlocking));
                                                                i6 = (int) preflightResultBlocking;
                                                                bArr2 = bArr;
                                                                iBackupTransport2 = iBackupTransport;
                                                            } catch (Throwable th6) {
                                                                th = th6;
                                                                i2 = 0;
                                                                jRequestFullBackupTime = j3;
                                                                r112 = CreatePipe;
                                                                i = -2003;
                                                                r11 = r112;
                                                                if (this.mCancelAll) {
                                                                }
                                                                Slog.i(TAG, "Full backup completed with status: " + i2);
                                                                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i2);
                                                                cleanUpPipes(r11);
                                                                cleanUpPipes(parcelFileDescriptorArr);
                                                                unregisterTask();
                                                                if (this.mJob != null) {
                                                                }
                                                                synchronized (this.backupManagerService.getQueueLock()) {
                                                                }
                                                            }
                                                        } else {
                                                            long j4 = 0;
                                                            while (true) {
                                                                bArr2 = bArr;
                                                                int i14 = fileInputStream2.read(bArr2);
                                                                if (i14 > 0) {
                                                                    fileOutputStream2.write(bArr2, i12, i14);
                                                                    synchronized (this.mCancelLock) {
                                                                        if (this.mCancelAll) {
                                                                            iBackupTransport2 = iBackupTransport;
                                                                        } else {
                                                                            iBackupTransport2 = iBackupTransport;
                                                                            iPerformFullBackup = iBackupTransport2.sendBackupData(i14);
                                                                        }
                                                                    }
                                                                    fileInputStream = fileInputStream2;
                                                                    long j5 = j4 + ((long) i14);
                                                                    fileOutputStream = fileOutputStream2;
                                                                    if (this.mBackupObserver == null || i13 <= 0) {
                                                                        i7 = i13;
                                                                    } else {
                                                                        i7 = i13;
                                                                        BackupObserverUtils.sendBackupOnUpdate(this.mBackupObserver, str, new BackupProgress(preflightResultBlocking, j5));
                                                                    }
                                                                    j2 = j5;
                                                                } else {
                                                                    fileInputStream = fileInputStream2;
                                                                    fileOutputStream = fileOutputStream2;
                                                                    i7 = i13;
                                                                    iBackupTransport2 = iBackupTransport;
                                                                    j2 = j4;
                                                                }
                                                                i8 = iPerformFullBackup;
                                                                if (i14 <= 0 || i8 != 0) {
                                                                    break;
                                                                }
                                                                iBackupTransport = iBackupTransport2;
                                                                iPerformFullBackup = i8;
                                                                j4 = j2;
                                                                bArr = bArr2;
                                                                fileInputStream2 = fileInputStream;
                                                                fileOutputStream2 = fileOutputStream;
                                                                i13 = i7;
                                                                i12 = 0;
                                                            }
                                                            if (i8 == -1005) {
                                                                Slog.w(TAG, "Package hit quota limit in-flight " + str + ": " + j2 + " of " + j);
                                                                i9 = i8;
                                                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 18, this.mCurrentPackage, 1, null);
                                                                this.mBackupRunner.sendQuotaExceeded(j2, j);
                                                            } else {
                                                                i9 = i8;
                                                            }
                                                            i6 = i9;
                                                        }
                                                        backupResultBlocking = this.mBackupRunner.getBackupResultBlocking();
                                                        synchronized (this.mCancelLock) {
                                                            this.mIsDoingBackup = false;
                                                            if (!this.mCancelAll) {
                                                                if (backupResultBlocking == 0) {
                                                                    int iFinishBackup = iBackupTransport2.finishBackup();
                                                                    if (i6 == 0) {
                                                                        i6 = iFinishBackup;
                                                                    }
                                                                } else {
                                                                    iBackupTransport2.cancelFullBackup();
                                                                }
                                                            }
                                                        }
                                                        if (i6 != 0 || backupResultBlocking == 0) {
                                                            backupResultBlocking = i6;
                                                        }
                                                        if (backupResultBlocking != 0) {
                                                            Slog.e(TAG, "Error " + backupResultBlocking + " backing up " + str);
                                                        }
                                                        jRequestFullBackupTime = iBackupTransport2.requestFullBackupTime();
                                                    } catch (Throwable th7) {
                                                        th = th7;
                                                        jRequestFullBackupTime = j3;
                                                    }
                                                } catch (Exception e3) {
                                                    e = e3;
                                                    r84 = CreatePipe;
                                                    jRequestFullBackupTime = j3;
                                                }
                                                try {
                                                    try {
                                                        Slog.i(TAG, "Transport suggested backoff=" + jRequestFullBackupTime);
                                                    } catch (Throwable th8) {
                                                        th = th8;
                                                        i = -2003;
                                                        i2 = 0;
                                                        r11 = CreatePipe;
                                                        if (this.mCancelAll) {
                                                        }
                                                        Slog.i(TAG, "Full backup completed with status: " + i2);
                                                        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i2);
                                                        cleanUpPipes(r11);
                                                        cleanUpPipes(parcelFileDescriptorArr);
                                                        unregisterTask();
                                                        if (this.mJob != null) {
                                                        }
                                                        synchronized (this.backupManagerService.getQueueLock()) {
                                                        }
                                                    }
                                                } catch (Exception e4) {
                                                    e = e4;
                                                    r84 = CreatePipe;
                                                    i = -2003;
                                                    r82 = r84;
                                                    i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                                    r8 = r82;
                                                    Slog.w(TAG, "Exception trying full transport backup", e);
                                                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                                                    if (!this.mCancelAll) {
                                                    }
                                                    Slog.i(TAG, "Full backup completed with status: " + i);
                                                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                                                    cleanUpPipes(r8);
                                                    cleanUpPipes(parcelFileDescriptorArr);
                                                    unregisterTask();
                                                    if (this.mJob != null) {
                                                    }
                                                    synchronized (this.backupManagerService.getQueueLock()) {
                                                    }
                                                }
                                            } else {
                                                bArr2 = bArr;
                                                iBackupTransport2 = iBackupTransport;
                                                jRequestFullBackupTime = j3;
                                                backupResultBlocking = iPerformFullBackup;
                                            }
                                            try {
                                                if (this.mUpdateSchedule) {
                                                    this.backupManagerService.enqueueFullBackup(str, System.currentTimeMillis());
                                                }
                                                if (backupResultBlocking == -1002) {
                                                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
                                                    Slog.i(TAG, "Transport rejected backup of " + str + ", skipping");
                                                    z = true;
                                                    EventLog.writeEvent(EventLogTags.FULL_BACKUP_AGENT_FAILURE, str, "transport rejected");
                                                    if (this.mBackupRunner != null) {
                                                        this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
                                                    }
                                                } else {
                                                    z = true;
                                                    if (backupResultBlocking == -1005) {
                                                        BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str, -1005);
                                                        Slog.i(TAG, "Transport quota exceeded for package: " + str);
                                                        EventLog.writeEvent(EventLogTags.FULL_BACKUP_QUOTA_EXCEEDED, str);
                                                        this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
                                                    } else if (backupResultBlocking == -1003) {
                                                        BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str, -1003);
                                                        Slog.w(TAG, "Application failure for package: " + str);
                                                        EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, str);
                                                        this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
                                                    } else {
                                                        i = -2003;
                                                        if (backupResultBlocking == -2003) {
                                                            try {
                                                                BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str, -2003);
                                                                Slog.w(TAG, "Backup cancelled. package=" + str + ", cancelAll=" + this.mCancelAll);
                                                                EventLog.writeEvent(EventLogTags.FULL_BACKUP_CANCELLED, str);
                                                                this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
                                                                i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                                                cleanUpPipes(CreatePipe);
                                                                cleanUpPipes(parcelFileDescriptorArr);
                                                                if (packageInfo.applicationInfo != null) {
                                                                    try {
                                                                        try {
                                                                            Slog.i(TAG, "Unbinding agent in " + str);
                                                                            this.backupManagerService.addBackupTrace("unbinding " + str);
                                                                            try {
                                                                                this.backupManagerService.getActivityManager().unbindBackupAgent(packageInfo.applicationInfo);
                                                                            } catch (RemoteException e5) {
                                                                            }
                                                                        } catch (Exception e6) {
                                                                            e = e6;
                                                                            r8 = CreatePipe;
                                                                            Slog.w(TAG, "Exception trying full transport backup", e);
                                                                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                                                                            if (!this.mCancelAll) {
                                                                            }
                                                                            Slog.i(TAG, "Full backup completed with status: " + i);
                                                                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                                                                            cleanUpPipes(r8);
                                                                            cleanUpPipes(parcelFileDescriptorArr);
                                                                            unregisterTask();
                                                                            if (this.mJob != null) {
                                                                            }
                                                                            synchronized (this.backupManagerService.getQueueLock()) {
                                                                            }
                                                                        }
                                                                    } catch (Throwable th9) {
                                                                        th = th9;
                                                                        i2 = 0;
                                                                        r11 = CreatePipe;
                                                                        if (this.mCancelAll) {
                                                                        }
                                                                        Slog.i(TAG, "Full backup completed with status: " + i2);
                                                                        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i2);
                                                                        cleanUpPipes(r11);
                                                                        cleanUpPipes(parcelFileDescriptorArr);
                                                                        unregisterTask();
                                                                        if (this.mJob != null) {
                                                                        }
                                                                        synchronized (this.backupManagerService.getQueueLock()) {
                                                                        }
                                                                    }
                                                                }
                                                                i11 = i4 + 1;
                                                                r7 = z;
                                                                j3 = jRequestFullBackupTime;
                                                                bArr3 = bArr2;
                                                                size = i5;
                                                                singlePackageBackupRunner = null;
                                                                c = 0;
                                                                iBackupTransportConnect = iBackupTransport2;
                                                                r2 = CreatePipe;
                                                            } catch (Exception e7) {
                                                                e = e7;
                                                                r82 = CreatePipe;
                                                                i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                                                r8 = r82;
                                                                Slog.w(TAG, "Exception trying full transport backup", e);
                                                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                                                                if (!this.mCancelAll) {
                                                                }
                                                                Slog.i(TAG, "Full backup completed with status: " + i);
                                                                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                                                                cleanUpPipes(r8);
                                                                cleanUpPipes(parcelFileDescriptorArr);
                                                                unregisterTask();
                                                                if (this.mJob != null) {
                                                                }
                                                                synchronized (this.backupManagerService.getQueueLock()) {
                                                                }
                                                            }
                                                        } else if (backupResultBlocking != 0) {
                                                            try {
                                                                break;
                                                            } catch (Exception e8) {
                                                                e = e8;
                                                                i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                                                r8 = CreatePipe;
                                                                Slog.w(TAG, "Exception trying full transport backup", e);
                                                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                                                                if (!this.mCancelAll) {
                                                                }
                                                                Slog.i(TAG, "Full backup completed with status: " + i);
                                                                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                                                                cleanUpPipes(r8);
                                                                cleanUpPipes(parcelFileDescriptorArr);
                                                                unregisterTask();
                                                                if (this.mJob != null) {
                                                                }
                                                                synchronized (this.backupManagerService.getQueueLock()) {
                                                                }
                                                            }
                                                        } else {
                                                            i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                                            BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str, 0);
                                                            EventLog.writeEvent(EventLogTags.FULL_BACKUP_SUCCESS, str);
                                                            this.backupManagerService.logBackupComplete(str);
                                                            cleanUpPipes(CreatePipe);
                                                            cleanUpPipes(parcelFileDescriptorArr);
                                                            if (packageInfo.applicationInfo != null) {
                                                            }
                                                            i11 = i4 + 1;
                                                            r7 = z;
                                                            j3 = jRequestFullBackupTime;
                                                            bArr3 = bArr2;
                                                            size = i5;
                                                            singlePackageBackupRunner = null;
                                                            c = 0;
                                                            iBackupTransportConnect = iBackupTransport2;
                                                            r2 = CreatePipe;
                                                        }
                                                    }
                                                }
                                                i = -2003;
                                                i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                                cleanUpPipes(CreatePipe);
                                                cleanUpPipes(parcelFileDescriptorArr);
                                                if (packageInfo.applicationInfo != null) {
                                                }
                                                i11 = i4 + 1;
                                                r7 = z;
                                                j3 = jRequestFullBackupTime;
                                                bArr3 = bArr2;
                                                size = i5;
                                                singlePackageBackupRunner = null;
                                                c = 0;
                                                iBackupTransportConnect = iBackupTransport2;
                                                r2 = CreatePipe;
                                            } catch (Exception e9) {
                                                e = e9;
                                                i = -2003;
                                            }
                                            th = th5;
                                            parcelFileDescriptorArr2 = parcelFileDescriptorArr;
                                            i = -2003;
                                            i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                        } catch (Throwable th10) {
                                            th = th10;
                                            i = -2003;
                                            i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                            parcelFileDescriptorArr2 = parcelFileDescriptorArr;
                                        }
                                        while (true) {
                                            throw th;
                                        }
                                    }
                                } catch (Exception e10) {
                                    e = e10;
                                    i = -2003;
                                    i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                } catch (Throwable th11) {
                                    th = th11;
                                    i = -2003;
                                    CreatePipe = CreatePipe;
                                }
                            } catch (Exception e11) {
                                e = e11;
                                i = -2003;
                                i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                r83 = r2;
                            } catch (Throwable th12) {
                                th = th12;
                                i = -2003;
                                CreatePipe = r2;
                            }
                        }
                    } else {
                        try {
                            try {
                                Slog.w(TAG, "Transport not present; full data backup not performed");
                                try {
                                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 15, this.mCurrentPackage, 1, null);
                                    int i15 = this.mCancelAll ? -2003 : JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                    Slog.i(TAG, "Full backup completed with status: " + i15);
                                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i15);
                                    cleanUpPipes(null);
                                    cleanUpPipes(null);
                                    unregisterTask();
                                    if (this.mJob != null) {
                                        this.mJob.finishBackupPass();
                                    }
                                    synchronized (this.backupManagerService.getQueueLock()) {
                                        this.backupManagerService.setRunningFullBackupTask(null);
                                    }
                                    this.mListener.onFinished("PFTBT.run()");
                                    this.mLatch.countDown();
                                    if (this.mUpdateSchedule) {
                                        this.backupManagerService.scheduleNextFullBackupJob(0L);
                                    }
                                    Slog.i(TAG, "Full data backup pass finished.");
                                    this.backupManagerService.getWakelock().release();
                                    return;
                                } catch (Throwable th13) {
                                    th = th13;
                                    parcelFileDescriptorArr = null;
                                    r11 = 0;
                                    i = -2003;
                                    i2 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                    if (this.mCancelAll) {
                                        i2 = i;
                                    }
                                    Slog.i(TAG, "Full backup completed with status: " + i2);
                                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i2);
                                    cleanUpPipes(r11);
                                    cleanUpPipes(parcelFileDescriptorArr);
                                    unregisterTask();
                                    if (this.mJob != null) {
                                        this.mJob.finishBackupPass();
                                    }
                                    synchronized (this.backupManagerService.getQueueLock()) {
                                        this.backupManagerService.setRunningFullBackupTask(null);
                                    }
                                    this.mListener.onFinished("PFTBT.run()");
                                    this.mLatch.countDown();
                                    if (this.mUpdateSchedule) {
                                        this.backupManagerService.scheduleNextFullBackupJob(jRequestFullBackupTime);
                                    }
                                    Slog.i(TAG, "Full data backup pass finished.");
                                    this.backupManagerService.getWakelock().release();
                                    throw th;
                                }
                            } catch (Exception e12) {
                                e = e12;
                                parcelFileDescriptorArr = null;
                                i = -2003;
                                r82 = r84;
                                i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                r8 = r82;
                                Slog.w(TAG, "Exception trying full transport backup", e);
                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                                if (!this.mCancelAll) {
                                }
                                Slog.i(TAG, "Full backup completed with status: " + i);
                                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                                cleanUpPipes(r8);
                                cleanUpPipes(parcelFileDescriptorArr);
                                unregisterTask();
                                if (this.mJob != null) {
                                }
                                synchronized (this.backupManagerService.getQueueLock()) {
                                }
                            }
                        } catch (Throwable th14) {
                            th = th14;
                            parcelFileDescriptorArr = null;
                            r112 = 0;
                            i2 = 0;
                            i = -2003;
                            r11 = r112;
                            if (this.mCancelAll) {
                            }
                            Slog.i(TAG, "Full backup completed with status: " + i2);
                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i2);
                            cleanUpPipes(r11);
                            cleanUpPipes(parcelFileDescriptorArr);
                            unregisterTask();
                            if (this.mJob != null) {
                            }
                            synchronized (this.backupManagerService.getQueueLock()) {
                            }
                        }
                    }
                }
            } catch (Exception e13) {
                e = e13;
                i = -2003;
                i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                parcelFileDescriptorArr = null;
                r8 = 0;
                jRequestFullBackupTime = 0;
            } catch (Throwable th15) {
                th = th15;
                i = -2003;
                parcelFileDescriptorArr = null;
                CreatePipe = 0;
                jRequestFullBackupTime = 0;
            }
            try {
                Slog.w(TAG, "Exception trying full transport backup", e);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                if (!this.mCancelAll) {
                    i = i3;
                }
                Slog.i(TAG, "Full backup completed with status: " + i);
                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                cleanUpPipes(r8);
                cleanUpPipes(parcelFileDescriptorArr);
                unregisterTask();
                if (this.mJob != null) {
                    this.mJob.finishBackupPass();
                }
                synchronized (this.backupManagerService.getQueueLock()) {
                    this.backupManagerService.setRunningFullBackupTask(null);
                }
                this.mListener.onFinished("PFTBT.run()");
                this.mLatch.countDown();
                if (this.mUpdateSchedule) {
                    BackupManagerService backupManagerService = this.backupManagerService;
                    backupManagerService.scheduleNextFullBackupJob(jRequestFullBackupTime);
                }
                Slog.i(TAG, "Full data backup pass finished.");
                this.backupManagerService.getWakelock().release();
                return;
            } catch (Throwable th16) {
                th = th16;
                i2 = i3;
                r11 = r8;
                if (this.mCancelAll) {
                }
                Slog.i(TAG, "Full backup completed with status: " + i2);
                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i2);
                cleanUpPipes(r11);
                cleanUpPipes(parcelFileDescriptorArr);
                unregisterTask();
                if (this.mJob != null) {
                }
                synchronized (this.backupManagerService.getQueueLock()) {
                }
            }
        }
        i = -2003;
        i3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        try {
            try {
                Slog.i(TAG, "full backup requested but enabled=" + this.backupManagerService.isEnabled() + " provisioned=" + this.backupManagerService.isProvisioned() + "; ignoring");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, this.backupManagerService.isProvisioned() ? 13 : 14, null, 3, null);
                i10 = 0;
                try {
                    this.mUpdateSchedule = false;
                    int i16 = this.mCancelAll ? -2003 : -2001;
                    Slog.i(TAG, "Full backup completed with status: " + i16);
                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i16);
                    cleanUpPipes(null);
                    cleanUpPipes(null);
                    unregisterTask();
                    if (this.mJob != null) {
                        this.mJob.finishBackupPass();
                    }
                    synchronized (this.backupManagerService.getQueueLock()) {
                        this.backupManagerService.setRunningFullBackupTask(null);
                    }
                    this.mListener.onFinished("PFTBT.run()");
                    this.mLatch.countDown();
                    if (this.mUpdateSchedule) {
                        this.backupManagerService.scheduleNextFullBackupJob(0L);
                    }
                    Slog.i(TAG, "Full data backup pass finished.");
                    this.backupManagerService.getWakelock().release();
                } catch (Throwable th17) {
                    th = th17;
                    i2 = i10;
                    jRequestFullBackupTime = 0;
                    parcelFileDescriptorArr = null;
                    r11 = 0;
                    if (this.mCancelAll) {
                    }
                    Slog.i(TAG, "Full backup completed with status: " + i2);
                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i2);
                    cleanUpPipes(r11);
                    cleanUpPipes(parcelFileDescriptorArr);
                    unregisterTask();
                    if (this.mJob != null) {
                    }
                    synchronized (this.backupManagerService.getQueueLock()) {
                    }
                }
            } catch (Exception e14) {
                e = e14;
                jRequestFullBackupTime = 0;
                parcelFileDescriptorArr = null;
                r8 = 0;
                Slog.w(TAG, "Exception trying full transport backup", e);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                if (!this.mCancelAll) {
                }
                Slog.i(TAG, "Full backup completed with status: " + i);
                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                cleanUpPipes(r8);
                cleanUpPipes(parcelFileDescriptorArr);
                unregisterTask();
                if (this.mJob != null) {
                }
                synchronized (this.backupManagerService.getQueueLock()) {
                }
            }
        } catch (Throwable th18) {
            th = th18;
            i10 = 0;
        }
    }

    void cleanUpPipes(ParcelFileDescriptor[] parcelFileDescriptorArr) {
        if (parcelFileDescriptorArr != null) {
            if (parcelFileDescriptorArr[0] != null) {
                ParcelFileDescriptor parcelFileDescriptor = parcelFileDescriptorArr[0];
                parcelFileDescriptorArr[0] = null;
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
            if (parcelFileDescriptorArr[1] != null) {
                ParcelFileDescriptor parcelFileDescriptor2 = parcelFileDescriptorArr[1];
                parcelFileDescriptorArr[1] = null;
                try {
                    parcelFileDescriptor2.close();
                } catch (IOException e2) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
        }
    }

    class SinglePackageBackupPreflight implements BackupRestoreTask, FullBackupPreflight {
        private final int mCurrentOpToken;
        final long mQuota;
        final TransportClient mTransportClient;
        private final int mTransportFlags;
        final AtomicLong mResult = new AtomicLong(-1003);
        final CountDownLatch mLatch = new CountDownLatch(1);

        SinglePackageBackupPreflight(TransportClient transportClient, long j, int i, int i2) {
            this.mTransportClient = transportClient;
            this.mQuota = j;
            this.mCurrentOpToken = i;
            this.mTransportFlags = i2;
        }

        @Override
        public int preflightFullBackup(PackageInfo packageInfo, IBackupAgent iBackupAgent) {
            long fullBackupAgentTimeoutMillis = PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                PerformFullTransportBackupTask.this.backupManagerService.prepareOperationTimeout(this.mCurrentOpToken, fullBackupAgentTimeoutMillis, this, 0);
                PerformFullTransportBackupTask.this.backupManagerService.addBackupTrace("preflighting");
                iBackupAgent.doMeasureFullBackup(this.mQuota, this.mCurrentOpToken, PerformFullTransportBackupTask.this.backupManagerService.getBackupManagerBinder(), this.mTransportFlags);
                this.mLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                long j = this.mResult.get();
                if (j < 0) {
                    return (int) j;
                }
                int iCheckFullBackupSize = this.mTransportClient.connectOrThrow("PFTBT$SPBP.preflightFullBackup()").checkFullBackupSize(j);
                if (iCheckFullBackupSize == -1005) {
                    iBackupAgent.doQuotaExceeded(j, this.mQuota);
                    return iCheckFullBackupSize;
                }
                return iCheckFullBackupSize;
            } catch (Exception e) {
                Slog.w(PerformFullTransportBackupTask.TAG, "Exception preflighting " + packageInfo.packageName + ": " + e.getMessage());
                return -1003;
            }
        }

        @Override
        public void execute() {
        }

        @Override
        public void operationComplete(long j) {
            this.mResult.set(j);
            this.mLatch.countDown();
            PerformFullTransportBackupTask.this.backupManagerService.removeOperation(this.mCurrentOpToken);
        }

        @Override
        public void handleCancel(boolean z) {
            this.mResult.set(-1003L);
            this.mLatch.countDown();
            PerformFullTransportBackupTask.this.backupManagerService.removeOperation(this.mCurrentOpToken);
        }

        @Override
        public long getExpectedSizeOrErrorCode() {
            try {
                this.mLatch.await(PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), TimeUnit.MILLISECONDS);
                return this.mResult.get();
            } catch (InterruptedException e) {
                return -1L;
            }
        }
    }

    class SinglePackageBackupRunner implements Runnable, BackupRestoreTask {
        private final int mCurrentOpToken;
        private FullBackupEngine mEngine;
        private final int mEphemeralToken;
        private volatile boolean mIsCancelled;
        final ParcelFileDescriptor mOutput;
        final SinglePackageBackupPreflight mPreflight;
        private final long mQuota;
        final PackageInfo mTarget;
        private final int mTransportFlags;
        final CountDownLatch mPreflightLatch = new CountDownLatch(1);
        final CountDownLatch mBackupLatch = new CountDownLatch(1);
        private volatile int mPreflightResult = -1003;
        private volatile int mBackupResult = -1003;

        SinglePackageBackupRunner(ParcelFileDescriptor parcelFileDescriptor, PackageInfo packageInfo, TransportClient transportClient, long j, int i, int i2) throws IOException {
            this.mOutput = ParcelFileDescriptor.dup(parcelFileDescriptor.getFileDescriptor());
            this.mTarget = packageInfo;
            this.mCurrentOpToken = i;
            this.mEphemeralToken = PerformFullTransportBackupTask.this.backupManagerService.generateRandomIntegerToken();
            this.mPreflight = PerformFullTransportBackupTask.this.new SinglePackageBackupPreflight(transportClient, j, this.mEphemeralToken, i2);
            this.mQuota = j;
            this.mTransportFlags = i2;
            registerTask();
        }

        void registerTask() {
            synchronized (PerformFullTransportBackupTask.this.backupManagerService.getCurrentOpLock()) {
                PerformFullTransportBackupTask.this.backupManagerService.getCurrentOperations().put(this.mCurrentOpToken, new Operation(0, this, 0));
            }
        }

        void unregisterTask() {
            synchronized (PerformFullTransportBackupTask.this.backupManagerService.getCurrentOpLock()) {
                PerformFullTransportBackupTask.this.backupManagerService.getCurrentOperations().remove(this.mCurrentOpToken);
            }
        }

        @Override
        public void run() {
            this.mEngine = new FullBackupEngine(PerformFullTransportBackupTask.this.backupManagerService, new FileOutputStream(this.mOutput.getFileDescriptor()), this.mPreflight, this.mTarget, false, this, this.mQuota, this.mCurrentOpToken, this.mTransportFlags);
            try {
                try {
                    try {
                        try {
                            if (!this.mIsCancelled) {
                                this.mPreflightResult = this.mEngine.preflightCheck();
                            }
                            this.mPreflightLatch.countDown();
                            if (this.mPreflightResult == 0 && !this.mIsCancelled) {
                                this.mBackupResult = this.mEngine.backupOnePackage();
                            }
                            unregisterTask();
                            this.mBackupLatch.countDown();
                            this.mOutput.close();
                        } catch (IOException e) {
                            Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
                        }
                    } catch (Exception e2) {
                        Slog.e(PerformFullTransportBackupTask.TAG, "Exception during full package backup of " + this.mTarget.packageName);
                        unregisterTask();
                        this.mBackupLatch.countDown();
                        this.mOutput.close();
                    }
                } catch (Throwable th) {
                    this.mPreflightLatch.countDown();
                    throw th;
                }
            } catch (Throwable th2) {
                unregisterTask();
                this.mBackupLatch.countDown();
                try {
                    this.mOutput.close();
                } catch (IOException e3) {
                    Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
                }
                throw th2;
            }
        }

        public void sendQuotaExceeded(long j, long j2) {
            this.mEngine.sendQuotaExceeded(j, j2);
        }

        long getPreflightResultBlocking() {
            try {
                this.mPreflightLatch.await(PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), TimeUnit.MILLISECONDS);
                if (this.mIsCancelled) {
                    return -2003L;
                }
                if (this.mPreflightResult == 0) {
                    return this.mPreflight.getExpectedSizeOrErrorCode();
                }
                return this.mPreflightResult;
            } catch (InterruptedException e) {
                return -1003L;
            }
        }

        int getBackupResultBlocking() {
            try {
                this.mBackupLatch.await(PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), TimeUnit.MILLISECONDS);
                if (this.mIsCancelled) {
                    return -2003;
                }
                return this.mBackupResult;
            } catch (InterruptedException e) {
                return -1003;
            }
        }

        @Override
        public void execute() {
        }

        @Override
        public void operationComplete(long j) {
        }

        @Override
        public void handleCancel(boolean z) {
            Slog.w(PerformFullTransportBackupTask.TAG, "Full backup cancel of " + this.mTarget.packageName);
            PerformFullTransportBackupTask.this.mMonitor = BackupManagerMonitorUtils.monitorEvent(PerformFullTransportBackupTask.this.mMonitor, 4, PerformFullTransportBackupTask.this.mCurrentPackage, 2, null);
            this.mIsCancelled = true;
            PerformFullTransportBackupTask.this.backupManagerService.handleCancel(this.mEphemeralToken, z);
            PerformFullTransportBackupTask.this.backupManagerService.tearDownAgentAndKill(this.mTarget.applicationInfo);
            this.mPreflightLatch.countDown();
            this.mBackupLatch.countDown();
            PerformFullTransportBackupTask.this.backupManagerService.removeOperation(this.mCurrentOpToken);
        }
    }
}
