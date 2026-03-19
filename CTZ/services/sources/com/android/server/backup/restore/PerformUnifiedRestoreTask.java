package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.app.backup.RestoreDescription;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.BackupUtils;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;

public class PerformUnifiedRestoreTask implements BackupRestoreTask {
    private BackupManagerService backupManagerService;
    private List<PackageInfo> mAcceptSet;
    private IBackupAgent mAgent;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    ParcelFileDescriptor mBackupData;
    private File mBackupDataName;
    private int mCount;
    private PackageInfo mCurrentPackage;
    private final int mEphemeralOpToken;
    private boolean mIsSystemRestore;
    private final OnTaskFinishedListener mListener;
    private IBackupManagerMonitor mMonitor;
    ParcelFileDescriptor mNewState;
    private File mNewStateName;
    private IRestoreObserver mObserver;
    private PackageManagerBackupAgent mPmAgent;
    private int mPmToken;
    private RestoreDescription mRestoreDescription;
    private File mSavedStateName;
    private File mStageName;
    File mStateDir;
    private int mStatus;
    private PackageInfo mTargetPackage;
    private long mToken;
    private final TransportClient mTransportClient;
    private final TransportManager mTransportManager;
    private byte[] mWidgetData;
    private UnifiedRestoreState mState = UnifiedRestoreState.INITIAL;
    private long mStartRealtime = SystemClock.elapsedRealtime();
    private boolean mFinished = false;
    private boolean mDidLaunch = false;

    public PerformUnifiedRestoreTask(BackupManagerService backupManagerService, TransportClient transportClient, IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j, PackageInfo packageInfo, int i, boolean z, String[] strArr, OnTaskFinishedListener onTaskFinishedListener) {
        this.backupManagerService = backupManagerService;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mEphemeralOpToken = backupManagerService.generateRandomIntegerToken();
        this.mTransportClient = transportClient;
        this.mObserver = iRestoreObserver;
        this.mMonitor = iBackupManagerMonitor;
        this.mToken = j;
        this.mPmToken = i;
        this.mTargetPackage = packageInfo;
        this.mIsSystemRestore = z;
        this.mListener = onTaskFinishedListener;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        if (packageInfo != null) {
            this.mAcceptSet = new ArrayList();
            this.mAcceptSet.add(packageInfo);
            return;
        }
        if (strArr == null) {
            strArr = packagesToNames(PackageManagerBackupAgent.getStorableApplications(backupManagerService.getPackageManager()));
            Slog.i(BackupManagerService.TAG, "Full restore; asking about " + strArr.length + " apps");
        }
        this.mAcceptSet = new ArrayList(strArr.length);
        boolean z2 = false;
        boolean z3 = false;
        for (String str : strArr) {
            try {
                PackageManager packageManager = backupManagerService.getPackageManager();
                PackageInfo packageInfo2 = packageManager.getPackageInfo(str, 0);
                if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageInfo2.packageName)) {
                    z2 = true;
                } else if (BackupManagerService.SETTINGS_PACKAGE.equals(packageInfo2.packageName)) {
                    z3 = true;
                } else if (AppBackupUtils.appIsEligibleForBackup(packageInfo2.applicationInfo, packageManager)) {
                    this.mAcceptSet.add(packageInfo2);
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        if (z2) {
            try {
                this.mAcceptSet.add(0, backupManagerService.getPackageManager().getPackageInfo(PackageManagerService.PLATFORM_PACKAGE_NAME, 0));
            } catch (PackageManager.NameNotFoundException e2) {
            }
        }
        if (z3) {
            try {
                this.mAcceptSet.add(backupManagerService.getPackageManager().getPackageInfo(BackupManagerService.SETTINGS_PACKAGE, 0));
            } catch (PackageManager.NameNotFoundException e3) {
            }
        }
    }

    private String[] packagesToNames(List<PackageInfo> list) {
        int size = list.size();
        String[] strArr = new String[size];
        for (int i = 0; i < size; i++) {
            strArr[i] = list.get(i).packageName;
        }
        return strArr;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState = new int[UnifiedRestoreState.values().length];

        static {
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.INITIAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.RUNNING_QUEUE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.RESTORE_KEYVALUE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.RESTORE_FULL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.RESTORE_FINISHED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.FINAL.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    @Override
    public void execute() throws Exception {
        switch (AnonymousClass1.$SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[this.mState.ordinal()]) {
            case 1:
                startRestore();
                break;
            case 2:
                dispatchNextRestore();
                break;
            case 3:
                restoreKeyValue();
                break;
            case 4:
                restoreFull();
                break;
            case 5:
                restoreFinished();
                break;
            case 6:
                if (!this.mFinished) {
                    finalizeRestore();
                } else {
                    Slog.e(BackupManagerService.TAG, "Duplicate finish");
                }
                this.mFinished = true;
                break;
        }
    }

    private void startRestore() {
        sendStartRestore(this.mAcceptSet.size());
        if (this.mIsSystemRestore) {
            AppWidgetBackupBridge.restoreStarting(0);
        }
        try {
            this.mStateDir = new File(this.backupManagerService.getBaseStateDir(), this.mTransportManager.getTransportDirName(this.mTransportClient.getTransportComponent()));
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.packageName = BackupManagerService.PACKAGE_MANAGER_SENTINEL;
            this.mAcceptSet.add(0, packageInfo);
            PackageInfo[] packageInfoArr = (PackageInfo[]) this.mAcceptSet.toArray(new PackageInfo[0]);
            IBackupTransport iBackupTransportConnectOrThrow = this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask.startRestore()");
            this.mStatus = iBackupTransportConnectOrThrow.startRestore(this.mToken, packageInfoArr);
            if (this.mStatus != 0) {
                Slog.e(BackupManagerService.TAG, "Transport error " + this.mStatus + "; no restore possible");
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            RestoreDescription restoreDescriptionNextRestorePackage = iBackupTransportConnectOrThrow.nextRestorePackage();
            if (restoreDescriptionNextRestorePackage == null) {
                Slog.e(BackupManagerService.TAG, "No restore metadata available; halting");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 22, this.mCurrentPackage, 3, null);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            if (!BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(restoreDescriptionNextRestorePackage.getPackageName())) {
                Slog.e(BackupManagerService.TAG, "Required package metadata but got " + restoreDescriptionNextRestorePackage.getPackageName());
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 23, this.mCurrentPackage, 3, null);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            this.mCurrentPackage = new PackageInfo();
            this.mCurrentPackage.packageName = BackupManagerService.PACKAGE_MANAGER_SENTINEL;
            this.mPmAgent = this.backupManagerService.makeMetadataAgent(null);
            this.mAgent = IBackupAgent.Stub.asInterface(this.mPmAgent.onBind());
            initiateOneRestore(this.mCurrentPackage, 0L);
            this.backupManagerService.getBackupHandler().removeMessages(18);
            if (!this.mPmAgent.hasMetadata()) {
                Slog.e(BackupManagerService.TAG, "PM agent has no metadata, so not restoring");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 24, this.mCurrentPackage, 3, null);
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, BackupManagerService.PACKAGE_MANAGER_SENTINEL, "Package manager restore metadata missing");
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                this.backupManagerService.getBackupHandler().removeMessages(20, this);
                executeNextState(UnifiedRestoreState.FINAL);
            }
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Unable to contact transport for restore: " + e.getMessage());
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 25, null, 1, null);
            this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.backupManagerService.getBackupHandler().removeMessages(20, this);
            executeNextState(UnifiedRestoreState.FINAL);
        }
    }

    private void dispatchNextRestore() {
        UnifiedRestoreState unifiedRestoreState;
        UnifiedRestoreState unifiedRestoreState2 = UnifiedRestoreState.FINAL;
        try {
            this.mRestoreDescription = this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask.dispatchNextRestore()").nextRestorePackage();
            String packageName = this.mRestoreDescription != null ? this.mRestoreDescription.getPackageName() : null;
            if (packageName == null) {
                Slog.e(BackupManagerService.TAG, "Failure getting next package name");
                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                unifiedRestoreState2 = UnifiedRestoreState.FINAL;
                return;
            }
            if (this.mRestoreDescription == RestoreDescription.NO_MORE_PACKAGES) {
                Slog.v(BackupManagerService.TAG, "No more packages; finishing restore");
                EventLog.writeEvent(EventLogTags.RESTORE_SUCCESS, Integer.valueOf(this.mCount), Integer.valueOf((int) (SystemClock.elapsedRealtime() - this.mStartRealtime)));
                unifiedRestoreState2 = UnifiedRestoreState.FINAL;
                return;
            }
            Slog.i(BackupManagerService.TAG, "Next restore package: " + this.mRestoreDescription);
            sendOnRestorePackage(packageName);
            PackageManagerBackupAgent.Metadata restoredMetadata = this.mPmAgent.getRestoredMetadata(packageName);
            if (restoredMetadata == null) {
                Slog.e(BackupManagerService.TAG, "No metadata for " + packageName);
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Package metadata missing");
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                return;
            }
            try {
                this.mCurrentPackage = this.backupManagerService.getPackageManager().getPackageInfo(packageName, 134217728);
                if (restoredMetadata.versionCode > this.mCurrentPackage.getLongVersionCode()) {
                    if ((this.mCurrentPackage.applicationInfo.flags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) == 0) {
                        String str = "Source version " + restoredMetadata.versionCode + " > installed version " + this.mCurrentPackage.getLongVersionCode();
                        Slog.w(BackupManagerService.TAG, "Package " + packageName + ": " + str);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 27, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_RESTORE_VERSION", restoredMetadata.versionCode), "android.app.backup.extra.LOG_RESTORE_ANYWAY", false));
                        EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, str);
                        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                        return;
                    }
                    Slog.v(BackupManagerService.TAG, "Source version " + restoredMetadata.versionCode + " > installed version " + this.mCurrentPackage.getLongVersionCode() + " but restoreAnyVersion");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 27, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_RESTORE_VERSION", restoredMetadata.versionCode), "android.app.backup.extra.LOG_RESTORE_ANYWAY", true));
                }
                this.mWidgetData = null;
                int dataType = this.mRestoreDescription.getDataType();
                if (dataType == 1) {
                    unifiedRestoreState = UnifiedRestoreState.RESTORE_KEYVALUE;
                } else {
                    if (dataType != 2) {
                        Slog.e(BackupManagerService.TAG, "Unrecognized restore type " + dataType);
                        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                        return;
                    }
                    unifiedRestoreState = UnifiedRestoreState.RESTORE_FULL;
                }
                executeNextState(unifiedRestoreState);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(BackupManagerService.TAG, "Package not present: " + packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 26, this.mCurrentPackage, 3, null);
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Package missing on device");
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            }
        } catch (Exception e2) {
            Slog.e(BackupManagerService.TAG, "Can't get next restore target from transport; halting: " + e2.getMessage());
            EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
            unifiedRestoreState2 = UnifiedRestoreState.FINAL;
        } finally {
            executeNextState(unifiedRestoreState2);
        }
    }

    private void restoreKeyValue() {
        String str = this.mCurrentPackage.packageName;
        if (this.mCurrentPackage.applicationInfo.backupAgentName == null || BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(this.mCurrentPackage.applicationInfo.backupAgentName)) {
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 28, this.mCurrentPackage, 2, null);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, str, "Package has no agent");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            return;
        }
        PackageManagerBackupAgent.Metadata restoredMetadata = this.mPmAgent.getRestoredMetadata(str);
        if (!BackupUtils.signaturesMatch(restoredMetadata.sigHashes, this.mCurrentPackage, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class))) {
            Slog.w(BackupManagerService.TAG, "Signature mismatch restoring " + str);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 29, this.mCurrentPackage, 3, null);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, str, "Signature mismatch");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            return;
        }
        this.mAgent = this.backupManagerService.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0);
        if (this.mAgent == null) {
            Slog.w(BackupManagerService.TAG, "Can't find backup agent for " + str);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 30, this.mCurrentPackage, 3, null);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, str, "Restore agent missing");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            return;
        }
        this.mDidLaunch = true;
        try {
            initiateOneRestore(this.mCurrentPackage, restoredMetadata.versionCode);
            this.mCount++;
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Error when attempting restore: " + e.toString());
            keyValueAgentErrorCleanup();
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    void initiateOneRestore(PackageInfo packageInfo, long j) {
        String str = packageInfo.packageName;
        Slog.d(BackupManagerService.TAG, "initiateOneRestore packageName=" + str);
        this.mBackupDataName = new File(this.backupManagerService.getDataDir(), str + ".restore");
        this.mStageName = new File(this.backupManagerService.getDataDir(), str + ".stage");
        this.mNewStateName = new File(this.mStateDir, str + ".new");
        this.mSavedStateName = new File(this.mStateDir, str);
        boolean zEquals = str.equals(PackageManagerService.PLATFORM_PACKAGE_NAME) ^ true;
        File file = zEquals ? this.mStageName : this.mBackupDataName;
        try {
            IBackupTransport iBackupTransportConnectOrThrow = this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask.initiateOneRestore()");
            ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(file, 1006632960);
            if (iBackupTransportConnectOrThrow.getRestoreData(parcelFileDescriptorOpen) != 0) {
                Slog.e(BackupManagerService.TAG, "Error getting restore data for " + str);
                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                parcelFileDescriptorOpen.close();
                file.delete();
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            if (zEquals) {
                parcelFileDescriptorOpen.close();
                parcelFileDescriptorOpen = ParcelFileDescriptor.open(file, 268435456);
                this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
                BackupDataInput backupDataInput = new BackupDataInput(parcelFileDescriptorOpen.getFileDescriptor());
                BackupDataOutput backupDataOutput = new BackupDataOutput(this.mBackupData.getFileDescriptor());
                byte[] bArr = new byte[8192];
                while (backupDataInput.readNextHeader()) {
                    String key = backupDataInput.getKey();
                    int dataSize = backupDataInput.getDataSize();
                    if (key.equals(BackupManagerService.KEY_WIDGET_STATE)) {
                        Slog.i(BackupManagerService.TAG, "Restoring widget state for " + str);
                        this.mWidgetData = new byte[dataSize];
                        backupDataInput.readEntityData(this.mWidgetData, 0, dataSize);
                    } else {
                        if (dataSize > bArr.length) {
                            bArr = new byte[dataSize];
                        }
                        backupDataInput.readEntityData(bArr, 0, dataSize);
                        backupDataOutput.writeEntityHeader(key, dataSize);
                        backupDataOutput.writeEntityData(bArr, dataSize);
                    }
                }
                this.mBackupData.close();
            }
            parcelFileDescriptorOpen.close();
            this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
            this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis(), this, 1);
            this.mAgent.doRestore(this.mBackupData, j, this.mNewState, this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder());
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Unable to call app for restore: " + str, e);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, str, e.toString());
            keyValueAgentErrorCleanup();
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void restoreFull() {
        try {
            new Thread(new StreamFeederThread(), "unified-stream-feeder").start();
        } catch (IOException e) {
            Slog.e(BackupManagerService.TAG, "Unable to construct pipes for stream restore!");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void restoreFinished() {
        Slog.d(BackupManagerService.TAG, "restoreFinished packageName=" + this.mCurrentPackage.packageName);
        try {
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, this.mAgentTimeoutParameters.getRestoreAgentFinishedTimeoutMillis(), this, 1);
            this.mAgent.doRestoreFinished(this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder());
        } catch (Exception e) {
            String str = this.mCurrentPackage.packageName;
            Slog.e(BackupManagerService.TAG, "Unable to finalize restore of " + str);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, str, e.toString());
            keyValueAgentErrorCleanup();
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    class StreamFeederThread extends RestoreEngine implements Runnable, BackupRestoreTask {
        FullRestoreEngine mEngine;
        EngineThread mEngineThread;
        private final int mEphemeralOpToken;
        final String TAG = "StreamFeederThread";
        ParcelFileDescriptor[] mTransportPipes = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor[] mEnginePipes = ParcelFileDescriptor.createPipe();

        public StreamFeederThread() throws IOException {
            this.mEphemeralOpToken = PerformUnifiedRestoreTask.this.backupManagerService.generateRandomIntegerToken();
            setRunning(true);
        }

        @Override
        public void run() throws Throwable {
            Throwable th;
            byte b;
            UnifiedRestoreState unifiedRestoreState;
            UnifiedRestoreState unifiedRestoreState2;
            int i;
            UnifiedRestoreState unifiedRestoreState3 = UnifiedRestoreState.RUNNING_QUEUE;
            EventLog.writeEvent(EventLogTags.FULL_RESTORE_PACKAGE, PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
            this.mEngine = new FullRestoreEngine(PerformUnifiedRestoreTask.this.backupManagerService, this, null, PerformUnifiedRestoreTask.this.mMonitor, PerformUnifiedRestoreTask.this.mCurrentPackage, false, false, this.mEphemeralOpToken);
            this.mEngineThread = PerformUnifiedRestoreTask.this.new EngineThread(this.mEngine, this.mEnginePipes[0]);
            ParcelFileDescriptor parcelFileDescriptor = this.mEnginePipes[1];
            ParcelFileDescriptor parcelFileDescriptor2 = this.mTransportPipes[0];
            ParcelFileDescriptor parcelFileDescriptor3 = this.mTransportPipes[1];
            byte[] bArr = new byte[32768];
            FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
            FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor2.getFileDescriptor());
            new Thread(this.mEngineThread, "unified-restore-engine").start();
            try {
                try {
                    IBackupTransport iBackupTransportConnectOrThrow = PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()");
                    byte[] bArr2 = bArr;
                    int i2 = 32768;
                    int i3 = 0;
                    while (true) {
                        if (i3 != 0) {
                            i = i3;
                            break;
                        }
                        try {
                            int nextFullRestoreDataChunk = iBackupTransportConnectOrThrow.getNextFullRestoreDataChunk(parcelFileDescriptor3);
                            if (nextFullRestoreDataChunk > 0) {
                                if (nextFullRestoreDataChunk > i2) {
                                    bArr2 = new byte[nextFullRestoreDataChunk];
                                    i2 = nextFullRestoreDataChunk;
                                }
                                while (nextFullRestoreDataChunk > 0) {
                                    int i4 = fileInputStream.read(bArr2, 0, nextFullRestoreDataChunk);
                                    fileOutputStream.write(bArr2, 0, i4);
                                    nextFullRestoreDataChunk -= i4;
                                }
                            } else {
                                if (nextFullRestoreDataChunk == -1) {
                                    i = 0;
                                    break;
                                }
                                Slog.e("StreamFeederThread", "Error " + nextFullRestoreDataChunk + " streaming restore for " + PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                                i3 = nextFullRestoreDataChunk;
                            }
                        } catch (IOException e) {
                            Slog.e("StreamFeederThread", "Unable to route data for restore");
                            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, "I/O error on pipes");
                            byte b2 = -1003;
                            IoUtils.closeQuietly(this.mEnginePipes[1]);
                            IoUtils.closeQuietly(this.mTransportPipes[0]);
                            IoUtils.closeQuietly(this.mTransportPipes[1]);
                            this.mEngineThread.waitForResult();
                            IoUtils.closeQuietly(this.mEnginePipes[0]);
                            PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                            try {
                                PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()").abortFullRestore();
                            } catch (Exception e2) {
                                Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e2.getMessage());
                                b2 = -1000;
                            }
                            PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, false);
                            if (b2 == -1000) {
                            }
                            PerformUnifiedRestoreTask.this.executeNextState(unifiedRestoreState2);
                            setRunning(false);
                        } catch (Exception e3) {
                            e = e3;
                            Slog.e("StreamFeederThread", "Transport failed during restore: " + e.getMessage());
                            EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                            IoUtils.closeQuietly(this.mEnginePipes[1]);
                            IoUtils.closeQuietly(this.mTransportPipes[0]);
                            IoUtils.closeQuietly(this.mTransportPipes[1]);
                            this.mEngineThread.waitForResult();
                            IoUtils.closeQuietly(this.mEnginePipes[0]);
                            PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                            try {
                                PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()").abortFullRestore();
                            } catch (Exception e4) {
                                Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e4.getMessage());
                            }
                            PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, false);
                        }
                    }
                    IoUtils.closeQuietly(this.mEnginePipes[1]);
                    IoUtils.closeQuietly(this.mTransportPipes[0]);
                    IoUtils.closeQuietly(this.mTransportPipes[1]);
                    this.mEngineThread.waitForResult();
                    IoUtils.closeQuietly(this.mEnginePipes[0]);
                    PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                } catch (Throwable th2) {
                    th = th2;
                    b = 32768;
                    IoUtils.closeQuietly(this.mEnginePipes[1]);
                    IoUtils.closeQuietly(this.mTransportPipes[0]);
                    IoUtils.closeQuietly(this.mTransportPipes[1]);
                    this.mEngineThread.waitForResult();
                    IoUtils.closeQuietly(this.mEnginePipes[0]);
                    PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                    if (b == 0) {
                        try {
                            PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()").abortFullRestore();
                        } catch (Exception e5) {
                            Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e5.getMessage());
                            b = -1000;
                        }
                        PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, false);
                        unifiedRestoreState = b == -1000 ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
                    } else {
                        unifiedRestoreState = UnifiedRestoreState.RESTORE_FINISHED;
                        PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                        PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                    }
                    PerformUnifiedRestoreTask.this.executeNextState(unifiedRestoreState);
                    setRunning(false);
                    throw th;
                }
            } catch (IOException e6) {
            } catch (Exception e7) {
                e = e7;
            } catch (Throwable th3) {
                th = th3;
                b = 0;
                IoUtils.closeQuietly(this.mEnginePipes[1]);
                IoUtils.closeQuietly(this.mTransportPipes[0]);
                IoUtils.closeQuietly(this.mTransportPipes[1]);
                this.mEngineThread.waitForResult();
                IoUtils.closeQuietly(this.mEnginePipes[0]);
                PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                if (b == 0) {
                }
                PerformUnifiedRestoreTask.this.executeNextState(unifiedRestoreState);
                setRunning(false);
                throw th;
            }
            if (i == 0) {
                unifiedRestoreState2 = UnifiedRestoreState.RESTORE_FINISHED;
                PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                PerformUnifiedRestoreTask.this.executeNextState(unifiedRestoreState2);
                setRunning(false);
            }
            try {
                PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()").abortFullRestore();
            } catch (Exception e8) {
                Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e8.getMessage());
                i = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, false);
            unifiedRestoreState2 = i == -1000 ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
            PerformUnifiedRestoreTask.this.executeNextState(unifiedRestoreState2);
            setRunning(false);
        }

        @Override
        public void execute() {
        }

        @Override
        public void operationComplete(long j) {
        }

        @Override
        public void handleCancel(boolean z) {
            PerformUnifiedRestoreTask.this.backupManagerService.removeOperation(this.mEphemeralOpToken);
            Slog.w("StreamFeederThread", "Full-data restore target timed out; shutting down");
            PerformUnifiedRestoreTask.this.mMonitor = BackupManagerMonitorUtils.monitorEvent(PerformUnifiedRestoreTask.this.mMonitor, 45, PerformUnifiedRestoreTask.this.mCurrentPackage, 2, null);
            this.mEngineThread.handleTimeout();
            IoUtils.closeQuietly(this.mEnginePipes[1]);
            this.mEnginePipes[1] = null;
            IoUtils.closeQuietly(this.mEnginePipes[0]);
            this.mEnginePipes[0] = null;
        }
    }

    class EngineThread implements Runnable {
        FullRestoreEngine mEngine;
        FileInputStream mEngineStream;

        EngineThread(FullRestoreEngine fullRestoreEngine, ParcelFileDescriptor parcelFileDescriptor) {
            this.mEngine = fullRestoreEngine;
            fullRestoreEngine.setRunning(true);
            this.mEngineStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor(), true);
        }

        public boolean isRunning() {
            return this.mEngine.isRunning();
        }

        public int waitForResult() {
            return this.mEngine.waitForResult();
        }

        @Override
        public void run() {
            while (this.mEngine.isRunning()) {
                try {
                    this.mEngine.restoreOneFile(this.mEngineStream, false, this.mEngine.mBuffer, this.mEngine.mOnlyPackage, this.mEngine.mAllowApks, this.mEngine.mEphemeralOpToken, this.mEngine.mMonitor);
                } finally {
                    IoUtils.closeQuietly(this.mEngineStream);
                }
            }
        }

        public void handleTimeout() {
            IoUtils.closeQuietly(this.mEngineStream);
            this.mEngine.handleTimeout();
        }
    }

    private void finalizeRestore() throws Exception {
        try {
            this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask.finalizeRestore()").finishRestore();
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Error finishing restore", e);
        }
        if (this.mObserver != null) {
            try {
                this.mObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e2) {
                Slog.d(BackupManagerService.TAG, "Restore observer died at restoreFinished");
            }
        }
        this.backupManagerService.getBackupHandler().removeMessages(8);
        if (this.mPmToken > 0) {
            try {
                this.backupManagerService.getPackageManagerBinder().finishPackageInstall(this.mPmToken, this.mDidLaunch);
            } catch (RemoteException e3) {
            }
        } else {
            this.backupManagerService.getBackupHandler().sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis());
        }
        AppWidgetBackupBridge.restoreFinished(0);
        if (this.mIsSystemRestore && this.mPmAgent != null) {
            this.backupManagerService.setAncestralPackages(this.mPmAgent.getRestoredPackages());
            this.backupManagerService.setAncestralToken(this.mToken);
            this.backupManagerService.writeRestoreTokens();
        }
        synchronized (this.backupManagerService.getPendingRestores()) {
            if (this.backupManagerService.getPendingRestores().size() <= 0) {
                this.backupManagerService.setRestoreInProgress(false);
            } else {
                Slog.d(BackupManagerService.TAG, "Starting next pending restore.");
                this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, this.backupManagerService.getPendingRestores().remove()));
            }
        }
        Slog.i(BackupManagerService.TAG, "Restore complete.");
        this.mListener.onFinished("PerformUnifiedRestoreTask.finalizeRestore()");
    }

    void keyValueAgentErrorCleanup() {
        this.backupManagerService.clearApplicationDataSynchronous(this.mCurrentPackage.packageName, false);
        keyValueAgentCleanup();
    }

    void keyValueAgentCleanup() {
        this.mBackupDataName.delete();
        this.mStageName.delete();
        try {
            if (this.mBackupData != null) {
                this.mBackupData.close();
            }
        } catch (IOException e) {
        }
        try {
            if (this.mNewState != null) {
                this.mNewState.close();
            }
        } catch (IOException e2) {
        }
        this.mNewState = null;
        this.mBackupData = null;
        this.mNewStateName.delete();
        if (this.mCurrentPackage.applicationInfo != null) {
            try {
                this.backupManagerService.getActivityManager().unbindBackupAgent(this.mCurrentPackage.applicationInfo);
                boolean z = this.mCurrentPackage.applicationInfo.uid >= 10000 && (this.mRestoreDescription.getDataType() == 2 || (this.mCurrentPackage.applicationInfo.flags & 65536) != 0);
                if (this.mTargetPackage == null && z) {
                    Slog.d(BackupManagerService.TAG, "Restore complete, killing host process of " + this.mCurrentPackage.applicationInfo.processName);
                    this.backupManagerService.getActivityManager().killApplicationProcess(this.mCurrentPackage.applicationInfo.processName, this.mCurrentPackage.applicationInfo.uid);
                }
            } catch (RemoteException e3) {
            }
        }
        this.backupManagerService.getBackupHandler().removeMessages(18, this);
    }

    @Override
    public void operationComplete(long j) {
        UnifiedRestoreState unifiedRestoreState;
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        int i = AnonymousClass1.$SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[this.mState.ordinal()];
        if (i == 1) {
            unifiedRestoreState = UnifiedRestoreState.RUNNING_QUEUE;
        } else {
            switch (i) {
                case 3:
                case 4:
                    unifiedRestoreState = UnifiedRestoreState.RESTORE_FINISHED;
                    break;
                case 5:
                    EventLog.writeEvent(EventLogTags.RESTORE_PACKAGE, this.mCurrentPackage.packageName, Integer.valueOf((int) this.mBackupDataName.length()));
                    keyValueAgentCleanup();
                    if (this.mWidgetData != null) {
                        this.backupManagerService.restoreWidgetData(this.mCurrentPackage.packageName, this.mWidgetData);
                    }
                    unifiedRestoreState = UnifiedRestoreState.RUNNING_QUEUE;
                    break;
                default:
                    Slog.e(BackupManagerService.TAG, "Unexpected restore callback into state " + this.mState);
                    keyValueAgentErrorCleanup();
                    unifiedRestoreState = UnifiedRestoreState.FINAL;
                    break;
            }
        }
        executeNextState(unifiedRestoreState);
    }

    @Override
    public void handleCancel(boolean z) {
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        Slog.e(BackupManagerService.TAG, "Timeout restoring application " + this.mCurrentPackage.packageName);
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 31, this.mCurrentPackage, 2, null);
        EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, this.mCurrentPackage.packageName, "restore timeout");
        keyValueAgentErrorCleanup();
        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
    }

    void executeNextState(UnifiedRestoreState unifiedRestoreState) {
        this.mState = unifiedRestoreState;
        this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, this));
    }

    void sendStartRestore(int i) {
        if (this.mObserver != null) {
            try {
                this.mObserver.restoreStarting(i);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "Restore observer went away: startRestore");
                this.mObserver = null;
            }
        }
    }

    void sendOnRestorePackage(String str) {
        if (this.mObserver != null) {
            try {
                this.mObserver.onUpdate(this.mCount, str);
            } catch (RemoteException e) {
                Slog.d(BackupManagerService.TAG, "Restore observer died in onUpdate");
                this.mObserver = null;
            }
        }
    }

    void sendEndRestore() {
        if (this.mObserver != null) {
            try {
                this.mObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "Restore observer went away: endRestore");
                this.mObserver = null;
            }
        }
    }
}
