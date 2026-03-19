package com.android.sharedstoragebackup;

import android.app.backup.FullBackup;
import android.app.backup.FullBackupAgent;
import android.app.backup.FullBackupDataOutput;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.ArraySet;
import android.util.Slog;
import java.io.File;
import java.io.IOException;

public class SharedStorageAgent extends FullBackupAgent {
    StorageVolume[] mVolumes;

    public void onCreate() {
        StorageManager storageManager = (StorageManager) getSystemService("storage");
        if (storageManager != null) {
            this.mVolumes = storageManager.getVolumeList();
        } else {
            Slog.e("SharedStorageAgent", "Unable to access Storage Manager");
        }
    }

    public void onFullBackup(FullBackupDataOutput fullBackupDataOutput) throws IOException {
        if (this.mVolumes != null) {
            Slog.i("SharedStorageAgent", "Backing up " + this.mVolumes.length + " shared volumes");
            ArraySet arraySet = new ArraySet();
            arraySet.add(new File(Environment.getExternalStorageDirectory(), "Android").getCanonicalPath());
            for (int i = 0; i < this.mVolumes.length; i++) {
                fullBackupFileTree(null, "shared/" + i, this.mVolumes[i].getPath(), null, arraySet, fullBackupDataOutput);
            }
        }
    }

    public void onRestoreFile(ParcelFileDescriptor parcelFileDescriptor, long j, int i, String str, String str2, long j2, long j3) throws IOException {
        Slog.d("SharedStorageAgent", "Shared restore: [ " + str + " : " + str2 + "]");
        int iIndexOf = str2.indexOf(47);
        File file = null;
        if (iIndexOf > 0) {
            try {
                int i2 = Integer.parseInt(str2.substring(0, iIndexOf));
                if (i2 <= this.mVolumes.length) {
                    File file2 = new File(this.mVolumes[i2].getPath(), str2.substring(iIndexOf + 1));
                    try {
                        Slog.i("SharedStorageAgent", " => " + file2.getAbsolutePath());
                        file = file2;
                    } catch (NumberFormatException e) {
                        file = file2;
                        Slog.w("SharedStorageAgent", "Bad volume number token: " + str2.substring(0, iIndexOf));
                    }
                } else {
                    Slog.w("SharedStorageAgent", "Cannot restore data for unavailable volume " + i2);
                }
            } catch (NumberFormatException e2) {
            }
        } else {
            Slog.i("SharedStorageAgent", "Can't find volume-number token");
        }
        File file3 = file;
        if (file3 == null) {
            Slog.e("SharedStorageAgent", "Skipping data with malformed path " + str2);
        }
        FullBackup.restoreFile(parcelFileDescriptor, j, i, -1L, j3, file3);
    }
}
