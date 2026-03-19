package com.android.server.backup;

import android.app.IBackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackup;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.restore.PerformAdbRestoreTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import libcore.io.IoUtils;

public class KeyValueAdbRestoreEngine implements Runnable {
    private static final boolean DEBUG = false;
    private static final String TAG = "KeyValueAdbRestoreEngine";
    IBackupAgent mAgent;
    private final BackupManagerServiceInterface mBackupManagerService;
    private final File mDataDir;
    ParcelFileDescriptor mInFD;
    FileMetadata mInfo;
    PerformAdbRestoreTask mRestoreTask;
    int mToken;

    public KeyValueAdbRestoreEngine(BackupManagerServiceInterface backupManagerServiceInterface, File file, FileMetadata fileMetadata, ParcelFileDescriptor parcelFileDescriptor, IBackupAgent iBackupAgent, int i) {
        this.mBackupManagerService = backupManagerServiceInterface;
        this.mDataDir = file;
        this.mInfo = fileMetadata;
        this.mInFD = parcelFileDescriptor;
        this.mAgent = iBackupAgent;
        this.mToken = i;
    }

    @Override
    public void run() throws Throwable {
        try {
            invokeAgentForAdbRestore(this.mAgent, this.mInfo, prepareRestoreData(this.mInfo, this.mInFD));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File prepareRestoreData(FileMetadata fileMetadata, ParcelFileDescriptor parcelFileDescriptor) throws Throwable {
        String str = fileMetadata.packageName;
        File file = new File(this.mDataDir, str + ".restore");
        File file2 = new File(this.mDataDir, str + ".sorted");
        FullBackup.restoreFile(parcelFileDescriptor, fileMetadata.size, fileMetadata.type, fileMetadata.mode, fileMetadata.mtime, file);
        sortKeyValueData(file, file2);
        return file2;
    }

    private void invokeAgentForAdbRestore(IBackupAgent iBackupAgent, FileMetadata fileMetadata, File file) throws IOException {
        String str = fileMetadata.packageName;
        try {
            iBackupAgent.doRestore(ParcelFileDescriptor.open(file, 268435456), fileMetadata.version, ParcelFileDescriptor.open(new File(this.mDataDir, str + ".new"), 1006632960), this.mToken, this.mBackupManagerService.getBackupManagerBinder());
        } catch (RemoteException e) {
            Slog.e(TAG, "Exception calling doRestore on agent: " + e);
        } catch (IOException e2) {
            Slog.e(TAG, "Exception opening file. " + e2);
        }
    }

    private void sortKeyValueData(File file, File file2) throws Throwable {
        FileOutputStream fileOutputStream;
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
            try {
                fileOutputStream = new FileOutputStream(file2);
                try {
                    copyKeysInLexicalOrder(new BackupDataInput(fileInputStream.getFD()), new BackupDataOutput(fileOutputStream.getFD()));
                    IoUtils.closeQuietly(fileInputStream);
                    IoUtils.closeQuietly(fileOutputStream);
                } catch (Throwable th) {
                    th = th;
                    if (fileInputStream != null) {
                        IoUtils.closeQuietly(fileInputStream);
                    }
                    if (fileOutputStream != null) {
                        IoUtils.closeQuietly(fileOutputStream);
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                fileOutputStream = null;
            }
        } catch (Throwable th3) {
            th = th3;
            fileOutputStream = null;
            fileInputStream = null;
        }
    }

    private void copyKeysInLexicalOrder(BackupDataInput backupDataInput, BackupDataOutput backupDataOutput) throws IOException {
        HashMap map = new HashMap();
        while (backupDataInput.readNextHeader()) {
            String key = backupDataInput.getKey();
            int dataSize = backupDataInput.getDataSize();
            if (dataSize < 0) {
                backupDataInput.skipEntityData();
            } else {
                byte[] bArr = new byte[dataSize];
                backupDataInput.readEntityData(bArr, 0, dataSize);
                map.put(key, bArr);
            }
        }
        ArrayList<String> arrayList = new ArrayList(map.keySet());
        Collections.sort(arrayList);
        for (String str : arrayList) {
            byte[] bArr2 = (byte[]) map.get(str);
            backupDataOutput.writeEntityHeader(str, bArr2.length);
            backupDataOutput.writeEntityData(bArr2, bArr2.length);
        }
    }
}
