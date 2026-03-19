package com.android.sharedstoragebackup;

import android.app.Service;
import android.app.backup.FullBackup;
import android.app.backup.FullBackupDataOutput;
import android.app.backup.IBackupManager;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.backup.IObbBackupService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class ObbBackupService extends Service {
    IObbBackupService mService = new IObbBackupService.Stub() {
        public void backupObbs(String str, ParcelFileDescriptor parcelFileDescriptor, int i, IBackupManager iBackupManager) {
            ArrayList<File> arrayListAllFileContents;
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            try {
                try {
                    try {
                        File file = Environment.buildExternalStorageAppObbDirs(str)[0];
                        if (file != null && file.exists() && (arrayListAllFileContents = allFileContents(file)) != null) {
                            Log.i("ObbBackupService", arrayListAllFileContents.size() + " files to back up");
                            String canonicalPath = file.getCanonicalPath();
                            FullBackupDataOutput fullBackupDataOutput = new FullBackupDataOutput(parcelFileDescriptor);
                            Iterator<File> it = arrayListAllFileContents.iterator();
                            while (it.hasNext()) {
                                String canonicalPath2 = it.next().getCanonicalPath();
                                Log.i("ObbBackupService", "storing: " + canonicalPath2);
                                FullBackup.backupToTar(str, "obb", (String) null, canonicalPath, canonicalPath2, fullBackupDataOutput);
                            }
                        }
                        try {
                            new FileOutputStream(fileDescriptor).write(new byte[4]);
                        } catch (IOException e) {
                            Log.e("ObbBackupService", "Unable to finalize obb backup stream!");
                        }
                        iBackupManager.opComplete(i, 0L);
                    } catch (RemoteException e2) {
                    }
                } catch (IOException e3) {
                    Log.w("ObbBackupService", "Exception backing up OBBs for " + str, e3);
                    try {
                        new FileOutputStream(fileDescriptor).write(new byte[4]);
                    } catch (IOException e4) {
                        Log.e("ObbBackupService", "Unable to finalize obb backup stream!");
                    }
                    iBackupManager.opComplete(i, 0L);
                }
            } finally {
            }
        }

        public void restoreObbFile(String str, ParcelFileDescriptor parcelFileDescriptor, long j, int i, String str2, long j2, long j3, int i2, IBackupManager iBackupManager) {
            try {
                try {
                    try {
                        File file = Environment.buildExternalStorageAppObbDirs(str)[0];
                        FullBackup.restoreFile(parcelFileDescriptor, j, i, -1L, j3, file != null ? new File(file, str2) : file);
                        iBackupManager.opComplete(i2, 0L);
                    } catch (IOException e) {
                        Log.i("ObbBackupService", "Exception restoring OBB " + str2, e);
                        iBackupManager.opComplete(i2, 0L);
                    }
                } catch (RemoteException e2) {
                }
            } finally {
            }
        }

        ArrayList<File> allFileContents(File file) {
            ArrayList<File> arrayList = new ArrayList<>();
            ArrayList arrayList2 = new ArrayList();
            arrayList2.add(file);
            while (!arrayList2.isEmpty()) {
                File[] fileArrListFiles = ((File) arrayList2.remove(0)).listFiles();
                if (fileArrListFiles != null) {
                    for (File file2 : fileArrListFiles) {
                        if (file2.isDirectory()) {
                            arrayList2.add(file2);
                        } else if (file2.isFile()) {
                            arrayList.add(file2);
                        }
                    }
                }
            }
            return arrayList;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return this.mService.asBinder();
    }
}
