package android.app.backup;

import android.app.IBackupAgent;
import android.app.QueuedWork;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

public abstract class BackupAgent extends ContextWrapper {
    private static final boolean DEBUG = true;
    private static final String TAG = "BackupAgent";
    public static final int TYPE_DIRECTORY = 2;
    public static final int TYPE_EOF = 0;
    public static final int TYPE_FILE = 1;
    public static final int TYPE_SYMLINK = 3;
    private final IBinder mBinder;
    Handler mHandler;

    public abstract void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException;

    public abstract void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException;

    Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        }
        return this.mHandler;
    }

    class SharedPrefsSynchronizer implements Runnable {
        public final CountDownLatch mLatch = new CountDownLatch(1);

        SharedPrefsSynchronizer() {
        }

        @Override
        public void run() {
            QueuedWork.waitToFinish();
            this.mLatch.countDown();
        }
    }

    private void waitForSharedPrefs() {
        Handler h = getHandler();
        SharedPrefsSynchronizer s = new SharedPrefsSynchronizer();
        h.postAtFrontOfQueue(s);
        try {
            s.mLatch.await();
        } catch (InterruptedException e) {
        }
    }

    public BackupAgent() {
        super(null);
        this.mHandler = null;
        this.mBinder = new BackupServiceBinder().asBinder();
    }

    public void onCreate() {
    }

    public void onDestroy() {
    }

    public void onFullBackup(FullBackupDataOutput data) throws IOException {
        File efLocation;
        ApplicationInfo appInfo = getApplicationInfo();
        String rootDir = new File(appInfo.dataDir).getCanonicalPath();
        String filesDir = getFilesDir().getCanonicalPath();
        String databaseDir = getDatabasePath("foo").getParentFile().getCanonicalPath();
        String sharedPrefsDir = getSharedPrefsFile("foo").getParentFile().getCanonicalPath();
        String cacheDir = getCacheDir().getCanonicalPath();
        String libDir = appInfo.nativeLibraryDir != null ? new File(appInfo.nativeLibraryDir).getCanonicalPath() : null;
        HashSet<String> filterSet = new HashSet<>();
        String packageName = getPackageName();
        if (libDir != null) {
            filterSet.add(libDir);
        }
        filterSet.add(cacheDir);
        filterSet.add(databaseDir);
        filterSet.add(sharedPrefsDir);
        filterSet.add(filesDir);
        fullBackupFileTree(packageName, FullBackup.ROOT_TREE_TOKEN, rootDir, filterSet, data);
        filterSet.add(rootDir);
        filterSet.remove(filesDir);
        fullBackupFileTree(packageName, FullBackup.DATA_TREE_TOKEN, filesDir, filterSet, data);
        filterSet.add(filesDir);
        filterSet.remove(databaseDir);
        fullBackupFileTree(packageName, FullBackup.DATABASE_TREE_TOKEN, databaseDir, filterSet, data);
        filterSet.add(databaseDir);
        filterSet.remove(sharedPrefsDir);
        fullBackupFileTree(packageName, FullBackup.SHAREDPREFS_TREE_TOKEN, sharedPrefsDir, filterSet, data);
        if (Process.myUid() != 1000 && (efLocation = getExternalFilesDir(null)) != null) {
            fullBackupFileTree(packageName, FullBackup.MANAGED_EXTERNAL_TREE_TOKEN, efLocation.getCanonicalPath(), null, data);
        }
    }

    public final void fullBackupFile(File file, FullBackupDataOutput output) {
        String domain;
        String rootpath;
        File efLocation;
        String efDir = null;
        ApplicationInfo appInfo = getApplicationInfo();
        try {
            String mainDir = new File(appInfo.dataDir).getCanonicalPath();
            String filesDir = getFilesDir().getCanonicalPath();
            String nbFilesDir = getNoBackupFilesDir().getCanonicalPath();
            String dbDir = getDatabasePath("foo").getParentFile().getCanonicalPath();
            String spDir = getSharedPrefsFile("foo").getParentFile().getCanonicalPath();
            String cacheDir = getCacheDir().getCanonicalPath();
            String libDir = appInfo.nativeLibraryDir == null ? null : new File(appInfo.nativeLibraryDir).getCanonicalPath();
            if (Process.myUid() != 1000 && (efLocation = getExternalFilesDir(null)) != null) {
                efDir = efLocation.getCanonicalPath();
            }
            String filePath = file.getCanonicalPath();
            if (filePath.startsWith(cacheDir) || filePath.startsWith(libDir) || filePath.startsWith(nbFilesDir)) {
                Log.w(TAG, "lib, cache, and no_backup files are not backed up");
                return;
            }
            if (filePath.startsWith(dbDir)) {
                domain = FullBackup.DATABASE_TREE_TOKEN;
                rootpath = dbDir;
            } else if (filePath.startsWith(spDir)) {
                domain = FullBackup.SHAREDPREFS_TREE_TOKEN;
                rootpath = spDir;
            } else if (filePath.startsWith(filesDir)) {
                domain = FullBackup.DATA_TREE_TOKEN;
                rootpath = filesDir;
            } else if (filePath.startsWith(mainDir)) {
                domain = FullBackup.ROOT_TREE_TOKEN;
                rootpath = mainDir;
            } else if (efDir != null && filePath.startsWith(efDir)) {
                domain = FullBackup.MANAGED_EXTERNAL_TREE_TOKEN;
                rootpath = efDir;
            } else {
                Log.w(TAG, "File " + filePath + " is in an unsupported location; skipping");
                return;
            }
            Log.i(TAG, "backupFile() of " + filePath + " => domain=" + domain + " rootpath=" + rootpath);
            FullBackup.backupToTar(getPackageName(), domain, null, rootpath, filePath, output.getData());
        } catch (IOException e) {
            Log.w(TAG, "Unable to obtain canonical paths");
        }
    }

    protected final void fullBackupFileTree(String packageName, String domain, String rootPath, HashSet<String> excludes, FullBackupDataOutput output) {
        String filePath;
        File[] contents;
        File rootFile = new File(rootPath);
        if (rootFile.exists()) {
            LinkedList<File> scanQueue = new LinkedList<>();
            scanQueue.add(rootFile);
            while (scanQueue.size() > 0) {
                File file = scanQueue.remove(0);
                try {
                    filePath = file.getCanonicalPath();
                } catch (ErrnoException e) {
                    Log.w(TAG, "Error scanning file " + file + " : " + e);
                } catch (IOException e2) {
                    Log.w(TAG, "Error canonicalizing path of " + file);
                }
                if (excludes == null || !excludes.contains(filePath)) {
                    StructStat stat = Os.lstat(filePath);
                    if (OsConstants.S_ISLNK(stat.st_mode)) {
                        Log.i(TAG, "Symlink (skipping)!: " + file);
                    } else {
                        if (OsConstants.S_ISDIR(stat.st_mode) && (contents = file.listFiles()) != null) {
                            for (File entry : contents) {
                                scanQueue.add(0, entry);
                            }
                        }
                        FullBackup.backupToTar(packageName, domain, null, rootPath, filePath, output.getData());
                    }
                }
            }
        }
    }

    public void onRestoreFile(ParcelFileDescriptor data, long size, File destination, int type, long mode, long mtime) throws IOException {
        FullBackup.restoreFile(data, size, type, mode, mtime, destination);
    }

    protected void onRestoreFile(ParcelFileDescriptor data, long size, int type, String domain, String path, long mode, long mtime) throws IOException {
        String basePath = null;
        Log.d(TAG, "onRestoreFile() size=" + size + " type=" + type + " domain=" + domain + " relpath=" + path + " mode=" + mode + " mtime=" + mtime);
        if (domain.equals(FullBackup.DATA_TREE_TOKEN)) {
            basePath = getFilesDir().getCanonicalPath();
        } else if (domain.equals(FullBackup.DATABASE_TREE_TOKEN)) {
            basePath = getDatabasePath("foo").getParentFile().getCanonicalPath();
        } else if (domain.equals(FullBackup.ROOT_TREE_TOKEN)) {
            basePath = new File(getApplicationInfo().dataDir).getCanonicalPath();
        } else if (domain.equals(FullBackup.SHAREDPREFS_TREE_TOKEN)) {
            basePath = getSharedPrefsFile("foo").getParentFile().getCanonicalPath();
        } else if (domain.equals(FullBackup.CACHE_TREE_TOKEN)) {
            basePath = getCacheDir().getCanonicalPath();
        } else if (domain.equals(FullBackup.MANAGED_EXTERNAL_TREE_TOKEN)) {
            if (Process.myUid() != 1000) {
                File efLocation = getExternalFilesDir(null);
                if (efLocation != null) {
                    basePath = getExternalFilesDir(null).getCanonicalPath();
                    mode = -1;
                }
            }
        } else if (domain.equals(FullBackup.NO_BACKUP_TREE_TOKEN)) {
            basePath = getNoBackupFilesDir().getCanonicalPath();
        } else {
            Log.i(TAG, "Unrecognized domain " + domain);
        }
        if (basePath != null) {
            File outFile = new File(basePath, path);
            String outPath = outFile.getCanonicalPath();
            if (outPath.startsWith(basePath + File.separatorChar)) {
                Log.i(TAG, "[" + domain + " : " + path + "] mapped to " + outPath);
                onRestoreFile(data, size, outFile, type, mode, mtime);
                return;
            }
            Log.e(TAG, "Cross-domain restore attempt: " + outPath);
        }
        Log.i(TAG, "[ skipping file " + path + "]");
        FullBackup.restoreFile(data, size, type, mode, mtime, null);
    }

    public void onRestoreFinished() {
    }

    public final IBinder onBind() {
        return this.mBinder;
    }

    public void attach(Context context) {
        attachBaseContext(context);
    }

    private class BackupServiceBinder extends IBackupAgent.Stub {
        private static final String TAG = "BackupServiceBinder";

        private BackupServiceBinder() {
        }

        @Override
        public void doBackup(ParcelFileDescriptor oldState, ParcelFileDescriptor data, ParcelFileDescriptor newState, int token, IBackupManager callbackBinder) throws RemoteException {
            long ident = Binder.clearCallingIdentity();
            Log.v(TAG, "doBackup() invoked");
            BackupDataOutput output = new BackupDataOutput(data.getFileDescriptor());
            try {
                try {
                    BackupAgent.this.onBackup(oldState, output, newState);
                } finally {
                    BackupAgent.this.waitForSharedPrefs();
                    Binder.restoreCallingIdentity(ident);
                    try {
                        callbackBinder.opComplete(token);
                    } catch (RemoteException e) {
                    }
                }
            } catch (IOException ex) {
                Log.d(TAG, "onBackup (" + BackupAgent.this.getClass().getName() + ") threw", ex);
                throw new RuntimeException(ex);
            } catch (RuntimeException ex2) {
                Log.d(TAG, "onBackup (" + BackupAgent.this.getClass().getName() + ") threw", ex2);
                throw ex2;
            }
        }

        @Override
        public void doRestore(ParcelFileDescriptor data, int appVersionCode, ParcelFileDescriptor newState, int token, IBackupManager callbackBinder) throws RemoteException {
            long ident = Binder.clearCallingIdentity();
            Log.v(TAG, "doRestore() invoked");
            BackupDataInput input = new BackupDataInput(data.getFileDescriptor());
            try {
                try {
                    BackupAgent.this.onRestore(input, appVersionCode, newState);
                } finally {
                    BackupAgent.this.waitForSharedPrefs();
                    Binder.restoreCallingIdentity(ident);
                    try {
                        callbackBinder.opComplete(token);
                    } catch (RemoteException e) {
                    }
                }
            } catch (IOException ex) {
                Log.d(TAG, "onRestore (" + BackupAgent.this.getClass().getName() + ") threw", ex);
                throw new RuntimeException(ex);
            } catch (RuntimeException ex2) {
                Log.d(TAG, "onRestore (" + BackupAgent.this.getClass().getName() + ") threw", ex2);
                throw ex2;
            }
        }

        @Override
        public void doFullBackup(ParcelFileDescriptor data, int token, IBackupManager callbackBinder) {
            long ident = Binder.clearCallingIdentity();
            Log.v(TAG, "doFullBackup() invoked");
            BackupAgent.this.waitForSharedPrefs();
            try {
                try {
                    BackupAgent.this.onFullBackup(new FullBackupDataOutput(data));
                    BackupAgent.this.waitForSharedPrefs();
                    try {
                        FileOutputStream out = new FileOutputStream(data.getFileDescriptor());
                        byte[] buf = new byte[4];
                        out.write(buf);
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to finalize backup stream!");
                    }
                    Binder.restoreCallingIdentity(ident);
                    try {
                        callbackBinder.opComplete(token);
                    } catch (RemoteException e2) {
                    }
                } catch (IOException ex) {
                    Log.d(TAG, "onBackup (" + BackupAgent.this.getClass().getName() + ") threw", ex);
                    throw new RuntimeException(ex);
                } catch (RuntimeException ex2) {
                    Log.d(TAG, "onBackup (" + BackupAgent.this.getClass().getName() + ") threw", ex2);
                    throw ex2;
                }
            } catch (Throwable th) {
                BackupAgent.this.waitForSharedPrefs();
                try {
                    FileOutputStream out2 = new FileOutputStream(data.getFileDescriptor());
                    byte[] buf2 = new byte[4];
                    out2.write(buf2);
                } catch (IOException e3) {
                    Log.e(TAG, "Unable to finalize backup stream!");
                }
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token);
                    throw th;
                } catch (RemoteException e4) {
                    throw th;
                }
            }
        }

        @Override
        public void doRestoreFile(ParcelFileDescriptor data, long size, int type, String domain, String path, long mode, long mtime, int token, IBackupManager callbackBinder) throws RemoteException {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    BackupAgent.this.onRestoreFile(data, size, type, domain, path, mode, mtime);
                } finally {
                    BackupAgent.this.waitForSharedPrefs();
                    Binder.restoreCallingIdentity(ident);
                    try {
                        callbackBinder.opComplete(token);
                    } catch (RemoteException e) {
                    }
                }
            } catch (IOException e2) {
                throw new RuntimeException(e2);
            }
        }

        @Override
        public void doRestoreFinished(int token, IBackupManager callbackBinder) {
            long ident = Binder.clearCallingIdentity();
            try {
                BackupAgent.this.onRestoreFinished();
            } finally {
                BackupAgent.this.waitForSharedPrefs();
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void fail(String message) {
            BackupAgent.this.getHandler().post(new FailRunnable(message));
        }
    }

    static class FailRunnable implements Runnable {
        private String mMessage;

        FailRunnable(String message) {
            this.mMessage = message;
        }

        @Override
        public void run() {
            throw new IllegalStateException(this.mMessage);
        }
    }
}
