package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.app.backup.FullBackup;
import android.app.backup.FullBackupDataOutput;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import android.util.StringBuilderPrinter;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.utils.FullBackupUtils;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FullBackupEngine {
    private BackupManagerService backupManagerService;
    IBackupAgent mAgent;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    boolean mIncludeApks;
    private final int mOpToken;
    OutputStream mOutput;
    PackageInfo mPkg;
    FullBackupPreflight mPreflightHook;
    private final long mQuota;
    BackupRestoreTask mTimeoutMonitor;
    private final int mTransportFlags;
    File mFilesDir = new File("/data/system");
    File mManifestFile = new File(this.mFilesDir, BackupManagerService.BACKUP_MANIFEST_FILENAME);
    File mMetadataFile = new File(this.mFilesDir, BackupManagerService.BACKUP_METADATA_FILENAME);

    class FullBackupRunner implements Runnable {
        IBackupAgent mAgent;
        PackageInfo mPackage;
        ParcelFileDescriptor mPipe;
        boolean mSendApk;
        int mToken;
        byte[] mWidgetData;
        boolean mWriteManifest;

        FullBackupRunner(PackageInfo packageInfo, IBackupAgent iBackupAgent, ParcelFileDescriptor parcelFileDescriptor, int i, boolean z, boolean z2, byte[] bArr) throws IOException {
            this.mPackage = packageInfo;
            this.mWidgetData = bArr;
            this.mAgent = iBackupAgent;
            this.mPipe = ParcelFileDescriptor.dup(parcelFileDescriptor.getFileDescriptor());
            this.mToken = i;
            this.mSendApk = z;
            this.mWriteManifest = z2;
        }

        @Override
        public void run() {
            try {
                try {
                    try {
                        FullBackupDataOutput fullBackupDataOutput = new FullBackupDataOutput(this.mPipe, -1L, FullBackupEngine.this.mTransportFlags);
                        if (this.mWriteManifest) {
                            boolean z = this.mWidgetData != null;
                            FullBackupUtils.writeAppManifest(this.mPackage, FullBackupEngine.this.backupManagerService.getPackageManager(), FullBackupEngine.this.mManifestFile, this.mSendApk, z);
                            FullBackup.backupToTar(this.mPackage.packageName, (String) null, (String) null, FullBackupEngine.this.mFilesDir.getAbsolutePath(), FullBackupEngine.this.mManifestFile.getAbsolutePath(), fullBackupDataOutput);
                            FullBackupEngine.this.mManifestFile.delete();
                            if (z) {
                                FullBackupEngine.this.writeMetadata(this.mPackage, FullBackupEngine.this.mMetadataFile, this.mWidgetData);
                                FullBackup.backupToTar(this.mPackage.packageName, (String) null, (String) null, FullBackupEngine.this.mFilesDir.getAbsolutePath(), FullBackupEngine.this.mMetadataFile.getAbsolutePath(), fullBackupDataOutput);
                                FullBackupEngine.this.mMetadataFile.delete();
                            }
                        }
                        if (this.mSendApk) {
                            FullBackupEngine.this.writeApkToBackup(this.mPackage, fullBackupDataOutput);
                        }
                        long sharedBackupAgentTimeoutMillis = this.mPackage.packageName.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE) ? FullBackupEngine.this.mAgentTimeoutParameters.getSharedBackupAgentTimeoutMillis() : FullBackupEngine.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
                        Slog.d(BackupManagerService.TAG, "Calling doFullBackup() on " + this.mPackage.packageName);
                        FullBackupEngine.this.backupManagerService.prepareOperationTimeout(this.mToken, sharedBackupAgentTimeoutMillis, FullBackupEngine.this.mTimeoutMonitor, 0);
                        this.mAgent.doFullBackup(this.mPipe, FullBackupEngine.this.mQuota, this.mToken, FullBackupEngine.this.backupManagerService.getBackupManagerBinder(), FullBackupEngine.this.mTransportFlags);
                        this.mPipe.close();
                    } catch (IOException e) {
                    }
                } catch (RemoteException e2) {
                    Slog.e(BackupManagerService.TAG, "Remote agent vanished during full backup of " + this.mPackage.packageName);
                    this.mPipe.close();
                } catch (IOException e3) {
                    Slog.e(BackupManagerService.TAG, "Error running full backup for " + this.mPackage.packageName);
                    this.mPipe.close();
                }
            } catch (Throwable th) {
                try {
                    this.mPipe.close();
                } catch (IOException e4) {
                }
                throw th;
            }
        }
    }

    public FullBackupEngine(BackupManagerService backupManagerService, OutputStream outputStream, FullBackupPreflight fullBackupPreflight, PackageInfo packageInfo, boolean z, BackupRestoreTask backupRestoreTask, long j, int i, int i2) {
        this.backupManagerService = backupManagerService;
        this.mOutput = outputStream;
        this.mPreflightHook = fullBackupPreflight;
        this.mPkg = packageInfo;
        this.mIncludeApks = z;
        this.mTimeoutMonitor = backupRestoreTask;
        this.mQuota = j;
        this.mOpToken = i;
        this.mTransportFlags = i2;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    public int preflightCheck() throws RemoteException {
        if (this.mPreflightHook == null) {
            return 0;
        }
        if (initializeAgent()) {
            return this.mPreflightHook.preflightFullBackup(this.mPkg, this.mAgent);
        }
        Slog.w(BackupManagerService.TAG, "Unable to bind to full agent for " + this.mPkg.packageName);
        return -1003;
    }

    public int backupOnePackage() throws Throwable {
        Throwable th;
        ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe;
        int i = -1003;
        if (!initializeAgent()) {
            Slog.w(BackupManagerService.TAG, "Unable to bind to full agent for " + this.mPkg.packageName);
        } else {
            ParcelFileDescriptor[] parcelFileDescriptorArr = null;
            try {
                try {
                    try {
                        parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
                    } catch (Throwable th2) {
                        th = th2;
                        parcelFileDescriptorArrCreatePipe = parcelFileDescriptorArr;
                    }
                } catch (IOException e) {
                    e = e;
                }
                try {
                    ApplicationInfo applicationInfo = this.mPkg.applicationInfo;
                    boolean zEquals = this.mPkg.packageName.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                    FullBackupRunner fullBackupRunner = new FullBackupRunner(this.mPkg, this.mAgent, parcelFileDescriptorArrCreatePipe[1], this.mOpToken, this.mIncludeApks && !zEquals && (applicationInfo.privateFlags & 4) == 0 && ((applicationInfo.flags & 1) == 0 || (applicationInfo.flags & 128) != 0), !zEquals, AppWidgetBackupBridge.getWidgetState(this.mPkg.packageName, 0));
                    parcelFileDescriptorArrCreatePipe[1].close();
                    parcelFileDescriptorArrCreatePipe[1] = null;
                    new Thread(fullBackupRunner, "app-data-runner").start();
                    FullBackupUtils.routeSocketDataToOutput(parcelFileDescriptorArrCreatePipe[0], this.mOutput);
                    if (!this.backupManagerService.waitUntilOperationComplete(this.mOpToken)) {
                        Slog.e(BackupManagerService.TAG, "Full backup failed on package " + this.mPkg.packageName);
                    } else {
                        i = 0;
                    }
                    this.mOutput.flush();
                    if (parcelFileDescriptorArrCreatePipe != null) {
                        if (parcelFileDescriptorArrCreatePipe[0] != null) {
                            parcelFileDescriptorArrCreatePipe[0].close();
                        }
                        if (parcelFileDescriptorArrCreatePipe[1] != null) {
                            parcelFileDescriptorArrCreatePipe[1].close();
                        }
                    }
                } catch (IOException e2) {
                    e = e2;
                    parcelFileDescriptorArr = parcelFileDescriptorArrCreatePipe;
                    Slog.e(BackupManagerService.TAG, "Error backing up " + this.mPkg.packageName + ": " + e.getMessage());
                    this.mOutput.flush();
                    if (parcelFileDescriptorArr != null) {
                        if (parcelFileDescriptorArr[0] != null) {
                            parcelFileDescriptorArr[0].close();
                        }
                        if (parcelFileDescriptorArr[1] != null) {
                            parcelFileDescriptorArr[1].close();
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    try {
                        this.mOutput.flush();
                        if (parcelFileDescriptorArrCreatePipe != null) {
                            if (parcelFileDescriptorArrCreatePipe[0] != null) {
                                parcelFileDescriptorArrCreatePipe[0].close();
                            }
                            if (parcelFileDescriptorArrCreatePipe[1] != null) {
                                parcelFileDescriptorArrCreatePipe[1].close();
                                throw th;
                            }
                            throw th;
                        }
                        throw th;
                    } catch (IOException e3) {
                        Slog.w(BackupManagerService.TAG, "Error bringing down backup stack");
                        throw th;
                    }
                }
            } catch (IOException e4) {
                Slog.w(BackupManagerService.TAG, "Error bringing down backup stack");
                i = -1000;
            }
        }
        tearDown();
        return i;
    }

    public void sendQuotaExceeded(long j, long j2) {
        if (initializeAgent()) {
            try {
                this.mAgent.doQuotaExceeded(j, j2);
            } catch (RemoteException e) {
                Slog.e(BackupManagerService.TAG, "Remote exception while telling agent about quota exceeded");
            }
        }
    }

    private boolean initializeAgent() {
        if (this.mAgent == null) {
            this.mAgent = this.backupManagerService.bindToAgentSynchronous(this.mPkg.applicationInfo, 1);
        }
        return this.mAgent != null;
    }

    private void writeApkToBackup(PackageInfo packageInfo, FullBackupDataOutput fullBackupDataOutput) {
        File[] fileArrListFiles;
        String baseCodePath = packageInfo.applicationInfo.getBaseCodePath();
        FullBackup.backupToTar(packageInfo.packageName, "a", (String) null, new File(baseCodePath).getParent(), baseCodePath, fullBackupDataOutput);
        File file = new Environment.UserEnvironment(0).buildExternalStorageAppObbDirs(packageInfo.packageName)[0];
        if (file != null && (fileArrListFiles = file.listFiles()) != null) {
            String absolutePath = file.getAbsolutePath();
            for (File file2 : fileArrListFiles) {
                FullBackup.backupToTar(packageInfo.packageName, "obb", (String) null, absolutePath, file2.getAbsolutePath(), fullBackupDataOutput);
            }
        }
    }

    private void writeMetadata(PackageInfo packageInfo, File file, byte[] bArr) throws IOException {
        StringBuilder sb = new StringBuilder(512);
        StringBuilderPrinter stringBuilderPrinter = new StringBuilderPrinter(sb);
        stringBuilderPrinter.println(Integer.toString(1));
        stringBuilderPrinter.println(packageInfo.packageName);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
        DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
        bufferedOutputStream.write(sb.toString().getBytes());
        if (bArr != null && bArr.length > 0) {
            dataOutputStream.writeInt(BackupManagerService.BACKUP_WIDGET_METADATA_TOKEN);
            dataOutputStream.writeInt(bArr.length);
            dataOutputStream.write(bArr);
        }
        bufferedOutputStream.flush();
        dataOutputStream.close();
        file.setLastModified(0L);
    }

    private void tearDown() {
        if (this.mPkg != null) {
            this.backupManagerService.tearDownAgentAndKill(this.mPkg.applicationInfo);
        }
    }
}
