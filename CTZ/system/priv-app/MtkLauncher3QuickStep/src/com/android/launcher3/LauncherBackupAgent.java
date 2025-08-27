package com.android.launcher3;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.provider.RestoreDbTask;

/* loaded from: classes.dex */
public class LauncherBackupAgent extends BackupAgent {
    @Override // android.app.backup.BackupAgent
    public void onCreate() {
        super.onCreate();
        FileLog.setDir(getFilesDir());
    }

    @Override // android.app.backup.BackupAgent
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) {
    }

    @Override // android.app.backup.BackupAgent
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) {
    }

    @Override // android.app.backup.BackupAgent
    public void onRestoreFinished() {
        RestoreDbTask.setPending(this, true);
    }
}
