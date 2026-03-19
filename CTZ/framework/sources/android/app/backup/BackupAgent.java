package android.app.backup;

import android.app.IBackupAgent;
import android.app.QueuedWork;
import android.app.backup.FullBackup;
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
import android.util.ArraySet;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;

public abstract class BackupAgent extends ContextWrapper {
    private static final boolean DEBUG = false;
    public static final int FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED = 1;
    public static final int FLAG_DEVICE_TO_DEVICE_TRANSFER = 2;
    public static final int FLAG_FAKE_CLIENT_SIDE_ENCRYPTION_ENABLED = Integer.MIN_VALUE;
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
        Handler handler = getHandler();
        SharedPrefsSynchronizer sharedPrefsSynchronizer = new SharedPrefsSynchronizer();
        handler.postAtFrontOfQueue(sharedPrefsSynchronizer);
        try {
            sharedPrefsSynchronizer.mLatch.await();
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

    public void onRestore(BackupDataInput backupDataInput, long j, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        onRestore(backupDataInput, (int) j, parcelFileDescriptor);
    }

    public void onFullBackup(FullBackupDataOutput fullBackupDataOutput) throws IOException {
        String canonicalPath;
        FullBackup.BackupScheme backupScheme = FullBackup.getBackupScheme(this);
        if (!backupScheme.isFullBackupContentEnabled()) {
            return;
        }
        try {
            Map<String, Set<FullBackup.BackupScheme.PathWithRequiredFlags>> mapMaybeParseAndGetCanonicalIncludePaths = backupScheme.maybeParseAndGetCanonicalIncludePaths();
            ArraySet<FullBackup.BackupScheme.PathWithRequiredFlags> arraySetMaybeParseAndGetCanonicalExcludePaths = backupScheme.maybeParseAndGetCanonicalExcludePaths();
            String packageName = getPackageName();
            ApplicationInfo applicationInfo = getApplicationInfo();
            Context contextCreateCredentialProtectedStorageContext = createCredentialProtectedStorageContext();
            String canonicalPath2 = contextCreateCredentialProtectedStorageContext.getDataDir().getCanonicalPath();
            String canonicalPath3 = contextCreateCredentialProtectedStorageContext.getFilesDir().getCanonicalPath();
            String canonicalPath4 = contextCreateCredentialProtectedStorageContext.getNoBackupFilesDir().getCanonicalPath();
            String canonicalPath5 = contextCreateCredentialProtectedStorageContext.getDatabasePath("foo").getParentFile().getCanonicalPath();
            String canonicalPath6 = contextCreateCredentialProtectedStorageContext.getSharedPreferencesPath("foo").getParentFile().getCanonicalPath();
            String canonicalPath7 = contextCreateCredentialProtectedStorageContext.getCacheDir().getCanonicalPath();
            String canonicalPath8 = contextCreateCredentialProtectedStorageContext.getCodeCacheDir().getCanonicalPath();
            Context contextCreateDeviceProtectedStorageContext = createDeviceProtectedStorageContext();
            String canonicalPath9 = contextCreateDeviceProtectedStorageContext.getDataDir().getCanonicalPath();
            String canonicalPath10 = contextCreateDeviceProtectedStorageContext.getFilesDir().getCanonicalPath();
            String canonicalPath11 = contextCreateDeviceProtectedStorageContext.getNoBackupFilesDir().getCanonicalPath();
            String canonicalPath12 = contextCreateDeviceProtectedStorageContext.getDatabasePath("foo").getParentFile().getCanonicalPath();
            String canonicalPath13 = contextCreateDeviceProtectedStorageContext.getSharedPreferencesPath("foo").getParentFile().getCanonicalPath();
            String canonicalPath14 = contextCreateDeviceProtectedStorageContext.getCacheDir().getCanonicalPath();
            String canonicalPath15 = contextCreateDeviceProtectedStorageContext.getCodeCacheDir().getCanonicalPath();
            if (applicationInfo.nativeLibraryDir != null) {
                canonicalPath = new File(applicationInfo.nativeLibraryDir).getCanonicalPath();
            } else {
                canonicalPath = null;
            }
            ArraySet<String> arraySet = new ArraySet<>();
            arraySet.add(canonicalPath3);
            arraySet.add(canonicalPath4);
            arraySet.add(canonicalPath5);
            arraySet.add(canonicalPath6);
            arraySet.add(canonicalPath7);
            arraySet.add(canonicalPath8);
            arraySet.add(canonicalPath10);
            arraySet.add(canonicalPath11);
            arraySet.add(canonicalPath12);
            arraySet.add(canonicalPath13);
            arraySet.add(canonicalPath14);
            arraySet.add(canonicalPath15);
            if (canonicalPath != null) {
                arraySet.add(canonicalPath);
            }
            applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.ROOT_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            arraySet.add(canonicalPath2);
            applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.DEVICE_ROOT_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            arraySet.add(canonicalPath9);
            arraySet.remove(canonicalPath3);
            applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.FILES_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            arraySet.add(canonicalPath3);
            arraySet.remove(canonicalPath10);
            applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.DEVICE_FILES_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            arraySet.add(canonicalPath10);
            arraySet.remove(canonicalPath5);
            applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.DATABASE_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            arraySet.add(canonicalPath5);
            arraySet.remove(canonicalPath12);
            applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.DEVICE_DATABASE_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            arraySet.add(canonicalPath12);
            arraySet.remove(canonicalPath6);
            applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.SHAREDPREFS_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            arraySet.add(canonicalPath6);
            arraySet.remove(canonicalPath13);
            applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.DEVICE_SHAREDPREFS_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            arraySet.add(canonicalPath13);
            if (Process.myUid() != 1000 && getExternalFilesDir(null) != null) {
                applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.MANAGED_EXTERNAL_TREE_TOKEN, mapMaybeParseAndGetCanonicalIncludePaths, arraySetMaybeParseAndGetCanonicalExcludePaths, arraySet, fullBackupDataOutput);
            }
        } catch (IOException | XmlPullParserException e) {
            if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                Log.v("BackupXmlParserLogging", "Exception trying to parse fullBackupContent xml file! Aborting full backup.", e);
            }
        }
    }

    public void onQuotaExceeded(long j, long j2) {
    }

    private void applyXmlFiltersAndDoFullBackupForDomain(String str, String str2, Map<String, Set<FullBackup.BackupScheme.PathWithRequiredFlags>> map, ArraySet<FullBackup.BackupScheme.PathWithRequiredFlags> arraySet, ArraySet<String> arraySet2, FullBackupDataOutput fullBackupDataOutput) throws IOException {
        if (map == null || map.size() == 0) {
            fullBackupFileTree(str, str2, FullBackup.getBackupScheme(this).tokenToDirectoryPath(str2), arraySet, arraySet2, fullBackupDataOutput);
            return;
        }
        if (map.get(str2) != null) {
            for (FullBackup.BackupScheme.PathWithRequiredFlags pathWithRequiredFlags : map.get(str2)) {
                if (areIncludeRequiredTransportFlagsSatisfied(pathWithRequiredFlags.getRequiredFlags(), fullBackupDataOutput.getTransportFlags())) {
                    fullBackupFileTree(str, str2, pathWithRequiredFlags.getPath(), arraySet, arraySet2, fullBackupDataOutput);
                }
            }
        }
    }

    private boolean areIncludeRequiredTransportFlagsSatisfied(int i, int i2) {
        return (i2 & i) == i;
    }

    public final void fullBackupFile(File file, FullBackupDataOutput fullBackupDataOutput) {
        String str;
        String canonicalPath;
        String str2;
        String str3;
        String str4;
        ApplicationInfo applicationInfo = getApplicationInfo();
        try {
            Context contextCreateCredentialProtectedStorageContext = createCredentialProtectedStorageContext();
            String canonicalPath2 = contextCreateCredentialProtectedStorageContext.getDataDir().getCanonicalPath();
            String canonicalPath3 = contextCreateCredentialProtectedStorageContext.getFilesDir().getCanonicalPath();
            String canonicalPath4 = contextCreateCredentialProtectedStorageContext.getNoBackupFilesDir().getCanonicalPath();
            String canonicalPath5 = contextCreateCredentialProtectedStorageContext.getDatabasePath("foo").getParentFile().getCanonicalPath();
            String canonicalPath6 = contextCreateCredentialProtectedStorageContext.getSharedPreferencesPath("foo").getParentFile().getCanonicalPath();
            String canonicalPath7 = contextCreateCredentialProtectedStorageContext.getCacheDir().getCanonicalPath();
            String canonicalPath8 = contextCreateCredentialProtectedStorageContext.getCodeCacheDir().getCanonicalPath();
            Context contextCreateDeviceProtectedStorageContext = createDeviceProtectedStorageContext();
            String canonicalPath9 = contextCreateDeviceProtectedStorageContext.getDataDir().getCanonicalPath();
            String canonicalPath10 = contextCreateDeviceProtectedStorageContext.getFilesDir().getCanonicalPath();
            String canonicalPath11 = contextCreateDeviceProtectedStorageContext.getNoBackupFilesDir().getCanonicalPath();
            String canonicalPath12 = contextCreateDeviceProtectedStorageContext.getDatabasePath("foo").getParentFile().getCanonicalPath();
            String canonicalPath13 = contextCreateDeviceProtectedStorageContext.getSharedPreferencesPath("foo").getParentFile().getCanonicalPath();
            String canonicalPath14 = contextCreateDeviceProtectedStorageContext.getCacheDir().getCanonicalPath();
            String canonicalPath15 = contextCreateDeviceProtectedStorageContext.getCodeCacheDir().getCanonicalPath();
            String canonicalPath16 = applicationInfo.nativeLibraryDir == null ? null : new File(applicationInfo.nativeLibraryDir).getCanonicalPath();
            if (Process.myUid() != 1000) {
                str = canonicalPath10;
                canonicalPath = null;
                File externalFilesDir = getExternalFilesDir(null);
                if (externalFilesDir != null) {
                    canonicalPath = externalFilesDir.getCanonicalPath();
                }
            } else {
                str = canonicalPath10;
                canonicalPath = null;
            }
            String canonicalPath17 = file.getCanonicalPath();
            if (canonicalPath17.startsWith(canonicalPath7) || canonicalPath17.startsWith(canonicalPath8) || canonicalPath17.startsWith(canonicalPath4) || canonicalPath17.startsWith(canonicalPath14) || canonicalPath17.startsWith(canonicalPath15) || canonicalPath17.startsWith(canonicalPath11) || canonicalPath17.startsWith(canonicalPath16)) {
                Log.w(TAG, "lib, cache, code_cache, and no_backup files are not backed up");
                return;
            }
            if (canonicalPath17.startsWith(canonicalPath5)) {
                str2 = FullBackup.DATABASE_TREE_TOKEN;
                str3 = canonicalPath5;
            } else if (canonicalPath17.startsWith(canonicalPath6)) {
                str2 = FullBackup.SHAREDPREFS_TREE_TOKEN;
                str3 = canonicalPath6;
            } else if (canonicalPath17.startsWith(canonicalPath3)) {
                str2 = FullBackup.FILES_TREE_TOKEN;
                str3 = canonicalPath3;
            } else if (canonicalPath17.startsWith(canonicalPath2)) {
                str2 = FullBackup.ROOT_TREE_TOKEN;
                str3 = canonicalPath2;
            } else if (canonicalPath17.startsWith(canonicalPath12)) {
                str2 = FullBackup.DEVICE_DATABASE_TREE_TOKEN;
                str3 = canonicalPath12;
            } else if (canonicalPath17.startsWith(canonicalPath13)) {
                str2 = FullBackup.DEVICE_SHAREDPREFS_TREE_TOKEN;
                str3 = canonicalPath13;
            } else {
                String str5 = str;
                if (canonicalPath17.startsWith(str5)) {
                    str4 = FullBackup.DEVICE_FILES_TREE_TOKEN;
                } else {
                    str5 = canonicalPath9;
                    if (canonicalPath17.startsWith(str5)) {
                        str4 = FullBackup.DEVICE_ROOT_TREE_TOKEN;
                    } else {
                        if (canonicalPath == null || !canonicalPath17.startsWith(canonicalPath)) {
                            Log.w(TAG, "File " + canonicalPath17 + " is in an unsupported location; skipping");
                            return;
                        }
                        str2 = FullBackup.MANAGED_EXTERNAL_TREE_TOKEN;
                        str3 = canonicalPath;
                    }
                }
                str3 = str5;
                str2 = str4;
            }
            FullBackup.backupToTar(getPackageName(), str2, null, str3, canonicalPath17, fullBackupDataOutput);
        } catch (IOException e) {
            Log.w(TAG, "Unable to obtain canonical paths");
        }
    }

    protected final void fullBackupFileTree(String str, String str2, String str3, ArraySet<FullBackup.BackupScheme.PathWithRequiredFlags> arraySet, ArraySet<String> arraySet2, FullBackupDataOutput fullBackupDataOutput) {
        StructStat structStatLstat;
        File[] fileArrListFiles;
        String str4 = FullBackup.getBackupScheme(this).tokenToDirectoryPath(str2);
        if (str4 == null) {
            return;
        }
        File file = new File(str3);
        if (file.exists()) {
            LinkedList linkedList = new LinkedList();
            linkedList.add(file);
            while (linkedList.size() > 0) {
                File file2 = (File) linkedList.remove(0);
                try {
                    structStatLstat = Os.lstat(file2.getPath());
                } catch (ErrnoException e) {
                    if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                        Log.v("BackupXmlParserLogging", "Error scanning file " + file2 + " : " + e);
                    }
                } catch (IOException e2) {
                    if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                        Log.v("BackupXmlParserLogging", "Error canonicalizing path of " + file2);
                    }
                }
                if (OsConstants.S_ISREG(structStatLstat.st_mode) || OsConstants.S_ISDIR(structStatLstat.st_mode)) {
                    String canonicalPath = file2.getCanonicalPath();
                    if (arraySet == null || !manifestExcludesContainFilePath(arraySet, canonicalPath)) {
                        if (arraySet2 == null || !arraySet2.contains(canonicalPath)) {
                            if (OsConstants.S_ISDIR(structStatLstat.st_mode) && (fileArrListFiles = file2.listFiles()) != null) {
                                for (File file3 : fileArrListFiles) {
                                    linkedList.add(0, file3);
                                }
                            }
                            FullBackup.backupToTar(str, str2, null, str4, canonicalPath, fullBackupDataOutput);
                        }
                    }
                }
            }
        }
    }

    private boolean manifestExcludesContainFilePath(ArraySet<FullBackup.BackupScheme.PathWithRequiredFlags> arraySet, String str) {
        Iterator<FullBackup.BackupScheme.PathWithRequiredFlags> it = arraySet.iterator();
        while (it.hasNext()) {
            String path = it.next().getPath();
            if (path != null && path.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public void onRestoreFile(ParcelFileDescriptor parcelFileDescriptor, long j, File file, int i, long j2, long j3) throws IOException {
        FullBackup.restoreFile(parcelFileDescriptor, j, i, j2, j3, isFileEligibleForRestore(file) ? file : null);
    }

    private boolean isFileEligibleForRestore(File file) throws IOException {
        FullBackup.BackupScheme backupScheme = FullBackup.getBackupScheme(this);
        if (!backupScheme.isFullBackupContentEnabled()) {
            if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                Log.v("BackupXmlParserLogging", "onRestoreFile \"" + file.getCanonicalPath() + "\" : fullBackupContent not enabled for " + getPackageName());
            }
            return false;
        }
        String canonicalPath = file.getCanonicalPath();
        try {
            Map<String, Set<FullBackup.BackupScheme.PathWithRequiredFlags>> mapMaybeParseAndGetCanonicalIncludePaths = backupScheme.maybeParseAndGetCanonicalIncludePaths();
            ArraySet<FullBackup.BackupScheme.PathWithRequiredFlags> arraySetMaybeParseAndGetCanonicalExcludePaths = backupScheme.maybeParseAndGetCanonicalExcludePaths();
            if (arraySetMaybeParseAndGetCanonicalExcludePaths != null && isFileSpecifiedInPathList(file, arraySetMaybeParseAndGetCanonicalExcludePaths)) {
                if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                    Log.v("BackupXmlParserLogging", "onRestoreFile: \"" + canonicalPath + "\": listed in excludes; skipping.");
                }
                return false;
            }
            if (mapMaybeParseAndGetCanonicalIncludePaths != null && !mapMaybeParseAndGetCanonicalIncludePaths.isEmpty()) {
                Iterator<Set<FullBackup.BackupScheme.PathWithRequiredFlags>> it = mapMaybeParseAndGetCanonicalIncludePaths.values().iterator();
                boolean zIsFileSpecifiedInPathList = false;
                while (it.hasNext() && !((zIsFileSpecifiedInPathList = zIsFileSpecifiedInPathList | isFileSpecifiedInPathList(file, it.next())))) {
                }
                if (!zIsFileSpecifiedInPathList) {
                    if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                        Log.v("BackupXmlParserLogging", "onRestoreFile: Trying to restore \"" + canonicalPath + "\" but it isn't specified in the included files; skipping.");
                    }
                    return false;
                }
                return true;
            }
            return true;
        } catch (XmlPullParserException e) {
            if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                Log.v("BackupXmlParserLogging", "onRestoreFile \"" + canonicalPath + "\" : Exception trying to parse fullBackupContent xml file! Aborting onRestoreFile.", e);
            }
            return false;
        }
    }

    private boolean isFileSpecifiedInPathList(File file, Collection<FullBackup.BackupScheme.PathWithRequiredFlags> collection) throws IOException {
        Iterator<FullBackup.BackupScheme.PathWithRequiredFlags> it = collection.iterator();
        while (it.hasNext()) {
            String path = it.next().getPath();
            File file2 = new File(path);
            if (file2.isDirectory()) {
                if (file.isDirectory()) {
                    return file.equals(file2);
                }
                return file.getCanonicalPath().startsWith(path);
            }
            if (file.equals(file2)) {
                return true;
            }
        }
        return false;
    }

    protected void onRestoreFile(ParcelFileDescriptor parcelFileDescriptor, long j, int i, String str, String str2, long j2, long j3) throws IOException {
        long j4;
        String str3 = FullBackup.getBackupScheme(this).tokenToDirectoryPath(str);
        if (str.equals(FullBackup.MANAGED_EXTERNAL_TREE_TOKEN)) {
            j4 = -1;
        } else {
            j4 = j2;
        }
        if (str3 != null) {
            File file = new File(str3, str2);
            if (file.getCanonicalPath().startsWith(str3 + File.separatorChar)) {
                onRestoreFile(parcelFileDescriptor, j, file, i, j4, j3);
                return;
            }
        }
        FullBackup.restoreFile(parcelFileDescriptor, j, i, j4, j3, null);
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
        public void doBackup(ParcelFileDescriptor parcelFileDescriptor, ParcelFileDescriptor parcelFileDescriptor2, ParcelFileDescriptor parcelFileDescriptor3, long j, int i, IBackupManager iBackupManager, int i2) throws RemoteException {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            BackupDataOutput backupDataOutput = new BackupDataOutput(parcelFileDescriptor2.getFileDescriptor(), j, i2);
            try {
                try {
                    BackupAgent.this.onBackup(parcelFileDescriptor, backupDataOutput, parcelFileDescriptor3);
                    BackupAgent.this.waitForSharedPrefs();
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    try {
                        iBackupManager.opComplete(i, 0L);
                    } catch (RemoteException e) {
                    }
                    if (Binder.getCallingPid() != Process.myPid()) {
                        IoUtils.closeQuietly(parcelFileDescriptor);
                        IoUtils.closeQuietly(parcelFileDescriptor2);
                        IoUtils.closeQuietly(parcelFileDescriptor3);
                    }
                } finally {
                }
            } catch (IOException e2) {
                Log.d(TAG, "onBackup (" + BackupAgent.this.getClass().getName() + ") threw", e2);
                throw new RuntimeException(e2);
            } catch (RuntimeException e3) {
                Log.d(TAG, "onBackup (" + BackupAgent.this.getClass().getName() + ") threw", e3);
                throw e3;
            }
        }

        @Override
        public void doRestore(ParcelFileDescriptor parcelFileDescriptor, long j, ParcelFileDescriptor parcelFileDescriptor2, int i, IBackupManager iBackupManager) throws RemoteException {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            BackupAgent.this.waitForSharedPrefs();
            try {
                try {
                    try {
                        BackupAgent.this.onRestore(new BackupDataInput(parcelFileDescriptor.getFileDescriptor()), j, parcelFileDescriptor2);
                        BackupAgent.this.reloadSharedPreferences();
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        try {
                            iBackupManager.opComplete(i, 0L);
                        } catch (RemoteException e) {
                        }
                        if (Binder.getCallingPid() != Process.myPid()) {
                            IoUtils.closeQuietly(parcelFileDescriptor);
                            IoUtils.closeQuietly(parcelFileDescriptor2);
                        }
                    } catch (IOException e2) {
                        Log.d(TAG, "onRestore (" + BackupAgent.this.getClass().getName() + ") threw", e2);
                        throw new RuntimeException(e2);
                    }
                } catch (RuntimeException e3) {
                    Log.d(TAG, "onRestore (" + BackupAgent.this.getClass().getName() + ") threw", e3);
                    throw e3;
                }
            } finally {
            }
        }

        @Override
        public void doFullBackup(ParcelFileDescriptor parcelFileDescriptor, long j, int i, IBackupManager iBackupManager, int i2) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            BackupAgent.this.waitForSharedPrefs();
            try {
                try {
                    try {
                        BackupAgent.this.onFullBackup(new FullBackupDataOutput(parcelFileDescriptor, j, i2));
                        BackupAgent.this.waitForSharedPrefs();
                        try {
                            new FileOutputStream(parcelFileDescriptor.getFileDescriptor()).write(new byte[4]);
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to finalize backup stream!");
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        try {
                            iBackupManager.opComplete(i, 0L);
                        } catch (RemoteException e2) {
                        }
                        if (Binder.getCallingPid() != Process.myPid()) {
                            IoUtils.closeQuietly(parcelFileDescriptor);
                        }
                    } catch (IOException e3) {
                        Log.d(TAG, "onFullBackup (" + BackupAgent.this.getClass().getName() + ") threw", e3);
                        throw new RuntimeException(e3);
                    }
                } catch (RuntimeException e4) {
                    Log.d(TAG, "onFullBackup (" + BackupAgent.this.getClass().getName() + ") threw", e4);
                    throw e4;
                }
            } finally {
            }
        }

        @Override
        public void doMeasureFullBackup(long j, int i, IBackupManager iBackupManager, int i2) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            FullBackupDataOutput fullBackupDataOutput = new FullBackupDataOutput(j, i2);
            BackupAgent.this.waitForSharedPrefs();
            try {
                try {
                    try {
                        BackupAgent.this.onFullBackup(fullBackupDataOutput);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        try {
                            iBackupManager.opComplete(i, fullBackupDataOutput.getSize());
                        } catch (RemoteException e) {
                        }
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        try {
                            iBackupManager.opComplete(i, fullBackupDataOutput.getSize());
                        } catch (RemoteException e2) {
                        }
                        throw th;
                    }
                } catch (RuntimeException e3) {
                    Log.d(TAG, "onFullBackup[M] (" + BackupAgent.this.getClass().getName() + ") threw", e3);
                    throw e3;
                }
            } catch (IOException e4) {
                Log.d(TAG, "onFullBackup[M] (" + BackupAgent.this.getClass().getName() + ") threw", e4);
                throw new RuntimeException(e4);
            }
        }

        @Override
        public void doRestoreFile(ParcelFileDescriptor parcelFileDescriptor, long j, int i, String str, String str2, long j2, long j3, int i2, IBackupManager iBackupManager) throws RemoteException {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    BackupAgent.this.onRestoreFile(parcelFileDescriptor, j, i, str, str2, j2, j3);
                    BackupAgent.this.waitForSharedPrefs();
                    BackupAgent.this.reloadSharedPreferences();
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    try {
                        iBackupManager.opComplete(i2, 0L);
                    } catch (RemoteException e) {
                    }
                    if (Binder.getCallingPid() != Process.myPid()) {
                        IoUtils.closeQuietly(parcelFileDescriptor);
                    }
                } catch (IOException e2) {
                    Log.d(TAG, "onRestoreFile (" + BackupAgent.this.getClass().getName() + ") threw", e2);
                    throw new RuntimeException(e2);
                }
            } finally {
            }
        }

        @Override
        public void doRestoreFinished(int i, IBackupManager iBackupManager) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    BackupAgent.this.onRestoreFinished();
                    BackupAgent.this.waitForSharedPrefs();
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    try {
                        iBackupManager.opComplete(i, 0L);
                    } catch (RemoteException e) {
                    }
                } catch (Exception e2) {
                    Log.d(TAG, "onRestoreFinished (" + BackupAgent.this.getClass().getName() + ") threw", e2);
                    throw e2;
                }
            } catch (Throwable th) {
                BackupAgent.this.waitForSharedPrefs();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                try {
                    iBackupManager.opComplete(i, 0L);
                } catch (RemoteException e3) {
                }
                throw th;
            }
        }

        @Override
        public void fail(String str) {
            BackupAgent.this.getHandler().post(new FailRunnable(str));
        }

        @Override
        public void doQuotaExceeded(long j, long j2) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    BackupAgent.this.onQuotaExceeded(j, j2);
                } catch (Exception e) {
                    Log.d(TAG, "onQuotaExceeded(" + BackupAgent.this.getClass().getName() + ") threw", e);
                    throw e;
                }
            } finally {
                BackupAgent.this.waitForSharedPrefs();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    static class FailRunnable implements Runnable {
        private String mMessage;

        FailRunnable(String str) {
            this.mMessage = str;
        }

        @Override
        public void run() {
            throw new IllegalStateException(this.mMessage);
        }
    }
}
