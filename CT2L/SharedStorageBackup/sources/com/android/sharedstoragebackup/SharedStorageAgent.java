package com.android.sharedstoragebackup;

import android.app.backup.FullBackup;
import android.app.backup.FullBackupAgent;
import android.app.backup.FullBackupDataOutput;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class SharedStorageAgent extends FullBackupAgent {
    StorageVolume[] mVolumes;

    public void onCreate() {
        StorageManager mgr = (StorageManager) getSystemService("storage");
        if (mgr != null) {
            this.mVolumes = mgr.getVolumeList();
        } else {
            Slog.e("SharedStorageAgent", "Unable to access Storage Manager");
        }
    }

    public void onFullBackup(FullBackupDataOutput output) throws IOException {
        if (this.mVolumes != null) {
            Slog.i("SharedStorageAgent", "Backing up " + this.mVolumes.length + " shared volumes");
            HashSet<String> externalFilesDirFilter = new HashSet<>();
            File externalAndroidRoot = new File(Environment.getExternalStorageDirectory(), "Android");
            externalFilesDirFilter.add(externalAndroidRoot.getCanonicalPath());
            for (int i = 0; i < this.mVolumes.length; i++) {
                StorageVolume v = this.mVolumes[i];
                String domain = "shared/" + i;
                fullBackupFileTree(null, domain, v.getPath(), externalFilesDirFilter, output);
            }
        }
    }

    public void onRestoreFile(ParcelFileDescriptor data, long size, int type, String domain, String relpath, long mode, long mtime) throws IOException {
        Slog.d("SharedStorageAgent", "Shared restore: [ " + domain + " : " + relpath + "]");
        File outFile = null;
        int slash = relpath.indexOf(47);
        if (slash > 0) {
            try {
                int i = Integer.parseInt(relpath.substring(0, slash));
                if (i <= this.mVolumes.length) {
                    File outFile2 = new File(this.mVolumes[i].getPath(), relpath.substring(slash + 1));
                    try {
                        Slog.i("SharedStorageAgent", " => " + outFile2.getAbsolutePath());
                        outFile = outFile2;
                    } catch (NumberFormatException e) {
                        outFile = outFile2;
                        Slog.w("SharedStorageAgent", "Bad volume number token: " + relpath.substring(0, slash));
                    }
                } else {
                    Slog.w("SharedStorageAgent", "Cannot restore data for unavailable volume " + i);
                }
            } catch (NumberFormatException e2) {
            }
        } else {
            Slog.i("SharedStorageAgent", "Can't find volume-number token");
        }
        if (outFile == null) {
            Slog.e("SharedStorageAgent", "Skipping data with malformed path " + relpath);
        }
        FullBackup.restoreFile(data, size, type, -1L, mtime, outFile);
    }
}
