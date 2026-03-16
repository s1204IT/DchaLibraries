package com.android.sharedstoragebackup;

import android.app.Service;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackup;
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

public class ObbBackupService extends Service {
    IObbBackupService mService = new IObbBackupService.Stub() {
        public void backupObbs(String packageName, ParcelFileDescriptor data, int token, IBackupManager callbackBinder) {
            ArrayList<File> obbList;
            FileDescriptor outFd = data.getFileDescriptor();
            try {
                try {
                    File obbDir = Environment.buildExternalStorageAppObbDirs(packageName)[0];
                    if (obbDir != null && obbDir.exists() && (obbList = allFileContents(obbDir)) != null) {
                        Log.i("ObbBackupService", obbList.size() + " files to back up");
                        String rootPath = obbDir.getCanonicalPath();
                        BackupDataOutput out = new BackupDataOutput(outFd);
                        for (File f : obbList) {
                            String filePath = f.getCanonicalPath();
                            Log.i("ObbBackupService", "storing: " + filePath);
                            FullBackup.backupToTar(packageName, "obb", (String) null, rootPath, filePath, out);
                        }
                    }
                    try {
                        FileOutputStream out2 = new FileOutputStream(outFd);
                        byte[] buf = new byte[4];
                        out2.write(buf);
                    } catch (IOException e) {
                        Log.e("ObbBackupService", "Unable to finalize obb backup stream!");
                    }
                    try {
                        callbackBinder.opComplete(token);
                    } catch (RemoteException e2) {
                    }
                } catch (IOException e3) {
                    Log.w("ObbBackupService", "Exception backing up OBBs for " + packageName, e3);
                    try {
                        FileOutputStream out3 = new FileOutputStream(outFd);
                        byte[] buf2 = new byte[4];
                        out3.write(buf2);
                    } catch (IOException e4) {
                        Log.e("ObbBackupService", "Unable to finalize obb backup stream!");
                    }
                    try {
                        callbackBinder.opComplete(token);
                    } catch (RemoteException e5) {
                    }
                }
            } catch (Throwable th) {
                try {
                    FileOutputStream out4 = new FileOutputStream(outFd);
                    byte[] buf3 = new byte[4];
                    out4.write(buf3);
                } catch (IOException e6) {
                    Log.e("ObbBackupService", "Unable to finalize obb backup stream!");
                }
                try {
                    callbackBinder.opComplete(token);
                    throw th;
                } catch (RemoteException e7) {
                    throw th;
                }
            }
        }

        public void restoreObbFile(String packageName, ParcelFileDescriptor data, long fileSize, int type, String path, long mode, long mtime, int token, IBackupManager callbackBinder) {
            try {
                try {
                    File outFile = Environment.buildExternalStorageAppObbDirs(packageName)[0];
                    if (outFile != null) {
                        outFile = new File(outFile, path);
                    }
                    FullBackup.restoreFile(data, fileSize, type, -1L, mtime, outFile);
                } catch (IOException e) {
                    Log.i("ObbBackupService", "Exception restoring OBB " + path, e);
                    try {
                        callbackBinder.opComplete(token);
                    } catch (RemoteException e2) {
                    }
                }
            } finally {
                try {
                    callbackBinder.opComplete(token);
                } catch (RemoteException e3) {
                }
            }
        }

        ArrayList<File> allFileContents(File rootDir) {
            ArrayList<File> files = new ArrayList<>();
            ArrayList<File> dirs = new ArrayList<>();
            dirs.add(rootDir);
            while (!dirs.isEmpty()) {
                File dir = dirs.remove(0);
                File[] contents = dir.listFiles();
                if (contents != null) {
                    for (File f : contents) {
                        if (f.isDirectory()) {
                            dirs.add(f);
                        } else if (f.isFile()) {
                            files.add(f);
                        }
                    }
                }
            }
            return files;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return this.mService.asBinder();
    }
}
