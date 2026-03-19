package com.android.server.backup;

import android.app.IBackupAgent;
import android.app.backup.FullBackup;
import android.app.backup.FullBackupDataOutput;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.backup.utils.FullBackupUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import libcore.io.IoUtils;

public class KeyValueAdbBackupEngine {
    private static final String BACKUP_KEY_VALUE_BACKUP_DATA_FILENAME_SUFFIX = ".data";
    private static final String BACKUP_KEY_VALUE_BLANK_STATE_FILENAME = "blank_state";
    private static final String BACKUP_KEY_VALUE_DIRECTORY_NAME = "key_value_dir";
    private static final String BACKUP_KEY_VALUE_NEW_STATE_FILENAME_SUFFIX = ".new";
    private static final boolean DEBUG = false;
    private static final String TAG = "KeyValueAdbBackupEngine";
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private ParcelFileDescriptor mBackupData;
    private final File mBackupDataName;
    private BackupManagerServiceInterface mBackupManagerService;
    private final File mBlankStateName;
    private final PackageInfo mCurrentPackage;
    private final File mDataDir;
    private final File mManifestFile;
    private ParcelFileDescriptor mNewState;
    private final File mNewStateName;
    private final OutputStream mOutput;
    private final PackageManager mPackageManager;
    private ParcelFileDescriptor mSavedState;
    private final File mStateDir;

    public KeyValueAdbBackupEngine(OutputStream outputStream, PackageInfo packageInfo, BackupManagerServiceInterface backupManagerServiceInterface, PackageManager packageManager, File file, File file2) {
        this.mOutput = outputStream;
        this.mCurrentPackage = packageInfo;
        this.mBackupManagerService = backupManagerServiceInterface;
        this.mPackageManager = packageManager;
        this.mDataDir = file2;
        this.mStateDir = new File(file, BACKUP_KEY_VALUE_DIRECTORY_NAME);
        this.mStateDir.mkdirs();
        String str = this.mCurrentPackage.packageName;
        this.mBlankStateName = new File(this.mStateDir, BACKUP_KEY_VALUE_BLANK_STATE_FILENAME);
        this.mBackupDataName = new File(this.mDataDir, str + BACKUP_KEY_VALUE_BACKUP_DATA_FILENAME_SUFFIX);
        this.mNewStateName = new File(this.mStateDir, str + BACKUP_KEY_VALUE_NEW_STATE_FILENAME_SUFFIX);
        this.mManifestFile = new File(this.mDataDir, BackupManagerService.BACKUP_MANIFEST_FILENAME);
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerServiceInterface.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    public void backupOnePackage() throws IOException {
        IBackupAgent iBackupAgentBindToAgent;
        ApplicationInfo applicationInfo = this.mCurrentPackage.applicationInfo;
        try {
            try {
                prepareBackupFiles(this.mCurrentPackage.packageName);
                iBackupAgentBindToAgent = bindToAgent(applicationInfo);
            } catch (FileNotFoundException e) {
                Slog.e(TAG, "Failed creating files for package " + this.mCurrentPackage.packageName + " will ignore package. " + e);
            }
            if (iBackupAgentBindToAgent == null) {
                Slog.e(TAG, "Failed binding to BackupAgent for package " + this.mCurrentPackage.packageName);
                return;
            }
            if (invokeAgentForAdbBackup(this.mCurrentPackage.packageName, iBackupAgentBindToAgent)) {
                writeBackupData();
                return;
            }
            Slog.e(TAG, "Backup Failed for package " + this.mCurrentPackage.packageName);
        } finally {
            cleanup();
        }
    }

    private void prepareBackupFiles(String str) throws FileNotFoundException {
        this.mSavedState = ParcelFileDescriptor.open(this.mBlankStateName, 402653184);
        this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
        if (!SELinux.restorecon(this.mBackupDataName)) {
            Slog.e(TAG, "SELinux restorecon failed on " + this.mBackupDataName);
        }
        this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
    }

    private IBackupAgent bindToAgent(ApplicationInfo applicationInfo) {
        try {
            return this.mBackupManagerService.bindToAgentSynchronous(applicationInfo, 0);
        } catch (SecurityException e) {
            Slog.e(TAG, "error in binding to agent for package " + applicationInfo.packageName + ". " + e);
            return null;
        }
    }

    private boolean invokeAgentForAdbBackup(String str, IBackupAgent iBackupAgent) {
        int iGenerateRandomIntegerToken = this.mBackupManagerService.generateRandomIntegerToken();
        try {
            this.mBackupManagerService.prepareOperationTimeout(iGenerateRandomIntegerToken, this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis(), null, 0);
            iBackupAgent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, JobStatus.NO_LATEST_RUNTIME, iGenerateRandomIntegerToken, this.mBackupManagerService.getBackupManagerBinder(), 0);
            if (!this.mBackupManagerService.waitUntilOperationComplete(iGenerateRandomIntegerToken)) {
                Slog.e(TAG, "Key-value backup failed on package " + str);
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG, "Error invoking agent for backup on " + str + ". " + e);
            return false;
        }
    }

    class KeyValueAdbBackupDataCopier implements Runnable {
        private final PackageInfo mPackage;
        private final ParcelFileDescriptor mPipe;
        private final int mToken;

        KeyValueAdbBackupDataCopier(PackageInfo packageInfo, ParcelFileDescriptor parcelFileDescriptor, int i) throws IOException {
            this.mPackage = packageInfo;
            this.mPipe = ParcelFileDescriptor.dup(parcelFileDescriptor.getFileDescriptor());
            this.mToken = i;
        }

        @Override
        public void run() {
            try {
                try {
                    FullBackupDataOutput fullBackupDataOutput = new FullBackupDataOutput(this.mPipe);
                    FullBackupUtils.writeAppManifest(this.mPackage, KeyValueAdbBackupEngine.this.mPackageManager, KeyValueAdbBackupEngine.this.mManifestFile, false, false);
                    FullBackup.backupToTar(this.mPackage.packageName, "k", (String) null, KeyValueAdbBackupEngine.this.mDataDir.getAbsolutePath(), KeyValueAdbBackupEngine.this.mManifestFile.getAbsolutePath(), fullBackupDataOutput);
                    KeyValueAdbBackupEngine.this.mManifestFile.delete();
                    FullBackup.backupToTar(this.mPackage.packageName, "k", (String) null, KeyValueAdbBackupEngine.this.mDataDir.getAbsolutePath(), KeyValueAdbBackupEngine.this.mBackupDataName.getAbsolutePath(), fullBackupDataOutput);
                    try {
                        new FileOutputStream(this.mPipe.getFileDescriptor()).write(new byte[4]);
                    } catch (IOException e) {
                        Slog.e(KeyValueAdbBackupEngine.TAG, "Unable to finalize backup stream!");
                    }
                    try {
                        KeyValueAdbBackupEngine.this.mBackupManagerService.getBackupManagerBinder().opComplete(this.mToken, 0L);
                    } catch (RemoteException e2) {
                    }
                } catch (IOException e3) {
                    Slog.e(KeyValueAdbBackupEngine.TAG, "Error running full backup for " + this.mPackage.packageName + ". " + e3);
                }
            } finally {
                IoUtils.closeQuietly(this.mPipe);
            }
        }
    }

    private void writeBackupData() throws Throwable {
        ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe;
        ParcelFileDescriptor parcelFileDescriptor;
        int iGenerateRandomIntegerToken = this.mBackupManagerService.generateRandomIntegerToken();
        long kvBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis();
        ParcelFileDescriptor[] parcelFileDescriptorArr = null;
        try {
            try {
                parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
            } catch (Throwable th) {
                th = th;
                parcelFileDescriptorArrCreatePipe = parcelFileDescriptorArr;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            this.mBackupManagerService.prepareOperationTimeout(iGenerateRandomIntegerToken, kvBackupAgentTimeoutMillis, null, 0);
            KeyValueAdbBackupDataCopier keyValueAdbBackupDataCopier = new KeyValueAdbBackupDataCopier(this.mCurrentPackage, parcelFileDescriptorArrCreatePipe[1], iGenerateRandomIntegerToken);
            parcelFileDescriptorArrCreatePipe[1].close();
            parcelFileDescriptorArrCreatePipe[1] = null;
            new Thread(keyValueAdbBackupDataCopier, "key-value-app-data-runner").start();
            FullBackupUtils.routeSocketDataToOutput(parcelFileDescriptorArrCreatePipe[0], this.mOutput);
            if (!this.mBackupManagerService.waitUntilOperationComplete(iGenerateRandomIntegerToken)) {
                Slog.e(TAG, "Full backup failed on package " + this.mCurrentPackage.packageName);
            }
            this.mOutput.flush();
        } catch (IOException e2) {
            e = e2;
            parcelFileDescriptorArr = parcelFileDescriptorArrCreatePipe;
            Slog.e(TAG, "Error backing up " + this.mCurrentPackage.packageName + ": " + e);
            this.mOutput.flush();
            if (parcelFileDescriptorArr != null) {
                IoUtils.closeQuietly(parcelFileDescriptorArr[0]);
                parcelFileDescriptor = parcelFileDescriptorArr[1];
            }
        } catch (Throwable th2) {
            th = th2;
            this.mOutput.flush();
            if (parcelFileDescriptorArrCreatePipe != null) {
                IoUtils.closeQuietly(parcelFileDescriptorArrCreatePipe[0]);
                IoUtils.closeQuietly(parcelFileDescriptorArrCreatePipe[1]);
            }
            throw th;
        }
        if (parcelFileDescriptorArrCreatePipe != null) {
            IoUtils.closeQuietly(parcelFileDescriptorArrCreatePipe[0]);
            parcelFileDescriptor = parcelFileDescriptorArrCreatePipe[1];
            IoUtils.closeQuietly(parcelFileDescriptor);
        }
    }

    private void cleanup() {
        this.mBackupManagerService.tearDownAgentAndKill(this.mCurrentPackage.applicationInfo);
        this.mBlankStateName.delete();
        this.mNewStateName.delete();
        this.mBackupDataName.delete();
    }
}
