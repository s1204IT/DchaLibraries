package android.app;

import android.app.LoadedApk;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IContentProvider;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.CompatResources;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.service.notification.ZenModeConfig;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.autofill.AutofillManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executor;
import libcore.io.Memory;

public class ContextImpl extends Context {
    private static final boolean DEBUG = false;
    static final int STATE_INITIALIZING = 1;
    static final int STATE_NOT_FOUND = 3;
    static final int STATE_READY = 2;
    static final int STATE_UNINITIALIZED = 0;
    private static final String TAG = "ContextImpl";
    private static final String XATTR_INODE_CACHE = "user.inode_cache";
    private static final String XATTR_INODE_CODE_CACHE = "user.inode_code_cache";

    @GuardedBy("ContextImpl.class")
    private static ArrayMap<String, ArrayMap<File, SharedPreferencesImpl>> sSharedPrefsCache;
    private final IBinder mActivityToken;
    private final String mBasePackageName;

    @GuardedBy("mSync")
    private File mCacheDir;
    private ClassLoader mClassLoader;

    @GuardedBy("mSync")
    private File mCodeCacheDir;
    private final ApplicationContentResolver mContentResolver;

    @GuardedBy("mSync")
    private File mDatabasesDir;
    private Display mDisplay;

    @GuardedBy("mSync")
    private File mFilesDir;
    private final int mFlags;
    private boolean mIsAutofillCompatEnabled;
    final ActivityThread mMainThread;

    @GuardedBy("mSync")
    private File mNoBackupFilesDir;
    private final String mOpPackageName;
    final LoadedApk mPackageInfo;
    private PackageManager mPackageManager;

    @GuardedBy("mSync")
    private File mPreferencesDir;
    private Resources mResources;
    private final ResourcesManager mResourcesManager;

    @GuardedBy("ContextImpl.class")
    private ArrayMap<String, File> mSharedPrefsPaths;
    private String mSplitName;
    private final UserHandle mUser;
    private int mThemeResource = 0;
    private Resources.Theme mTheme = null;
    private Context mReceiverRestrictedContext = null;
    private AutofillManager.AutofillClient mAutofillClient = null;
    private final Object mSync = new Object();
    final Object[] mServiceCache = SystemServiceRegistry.createServiceCache();
    final int[] mServiceInitializationStateArray = new int[this.mServiceCache.length];
    private Context mOuterContext = this;

    @Retention(RetentionPolicy.SOURCE)
    @interface ServiceInitializationState {
    }

    static ContextImpl getImpl(Context context) {
        Context baseContext;
        while ((context instanceof ContextWrapper) && (baseContext = ((ContextWrapper) context).getBaseContext()) != null) {
            context = baseContext;
        }
        return (ContextImpl) context;
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public Resources getResources() {
        return this.mResources;
    }

    @Override
    public PackageManager getPackageManager() {
        if (this.mPackageManager != null) {
            return this.mPackageManager;
        }
        IPackageManager packageManager = ActivityThread.getPackageManager();
        if (packageManager != null) {
            ApplicationPackageManager applicationPackageManager = new ApplicationPackageManager(this, packageManager);
            this.mPackageManager = applicationPackageManager;
            return applicationPackageManager;
        }
        return null;
    }

    @Override
    public ContentResolver getContentResolver() {
        return this.mContentResolver;
    }

    @Override
    public Looper getMainLooper() {
        return this.mMainThread.getLooper();
    }

    @Override
    public Executor getMainExecutor() {
        return this.mMainThread.getExecutor();
    }

    @Override
    public Context getApplicationContext() {
        return this.mPackageInfo != null ? this.mPackageInfo.getApplication() : this.mMainThread.getApplication();
    }

    @Override
    public void setTheme(int i) {
        synchronized (this.mSync) {
            if (this.mThemeResource != i) {
                this.mThemeResource = i;
                initializeTheme();
            }
        }
    }

    @Override
    public int getThemeResId() {
        int i;
        synchronized (this.mSync) {
            i = this.mThemeResource;
        }
        return i;
    }

    @Override
    public Resources.Theme getTheme() {
        synchronized (this.mSync) {
            if (this.mTheme != null) {
                return this.mTheme;
            }
            this.mThemeResource = Resources.selectDefaultTheme(this.mThemeResource, getOuterContext().getApplicationInfo().targetSdkVersion);
            initializeTheme();
            return this.mTheme;
        }
    }

    private void initializeTheme() {
        if (this.mTheme == null) {
            this.mTheme = this.mResources.newTheme();
        }
        this.mTheme.applyStyle(this.mThemeResource, true);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.mClassLoader != null ? this.mClassLoader : this.mPackageInfo != null ? this.mPackageInfo.getClassLoader() : ClassLoader.getSystemClassLoader();
    }

    @Override
    public String getPackageName() {
        if (this.mPackageInfo != null) {
            return this.mPackageInfo.getPackageName();
        }
        return ZenModeConfig.SYSTEM_AUTHORITY;
    }

    @Override
    public String getBasePackageName() {
        return this.mBasePackageName != null ? this.mBasePackageName : getPackageName();
    }

    @Override
    public String getOpPackageName() {
        return this.mOpPackageName != null ? this.mOpPackageName : getBasePackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        if (this.mPackageInfo != null) {
            return this.mPackageInfo.getApplicationInfo();
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public String getPackageResourcePath() {
        if (this.mPackageInfo != null) {
            return this.mPackageInfo.getResDir();
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public String getPackageCodePath() {
        if (this.mPackageInfo != null) {
            return this.mPackageInfo.getAppDir();
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public SharedPreferences getSharedPreferences(String str, int i) {
        File sharedPreferencesPath;
        if (this.mPackageInfo.getApplicationInfo().targetSdkVersion < 19 && str == null) {
            str = "null";
        }
        synchronized (ContextImpl.class) {
            if (this.mSharedPrefsPaths == null) {
                this.mSharedPrefsPaths = new ArrayMap<>();
            }
            sharedPreferencesPath = this.mSharedPrefsPaths.get(str);
            if (sharedPreferencesPath == null) {
                sharedPreferencesPath = getSharedPreferencesPath(str);
                this.mSharedPrefsPaths.put(str, sharedPreferencesPath);
            }
        }
        return getSharedPreferences(sharedPreferencesPath, i);
    }

    @Override
    public SharedPreferences getSharedPreferences(File file, int i) {
        synchronized (ContextImpl.class) {
            ArrayMap<File, SharedPreferencesImpl> sharedPreferencesCacheLocked = getSharedPreferencesCacheLocked();
            SharedPreferencesImpl sharedPreferencesImpl = sharedPreferencesCacheLocked.get(file);
            if (sharedPreferencesImpl == null) {
                checkMode(i);
                if (getApplicationInfo().targetSdkVersion >= 26 && isCredentialProtectedStorage() && !((UserManager) getSystemService(UserManager.class)).isUserUnlockingOrUnlocked(UserHandle.myUserId())) {
                    throw new IllegalStateException("SharedPreferences in credential encrypted storage are not available until after user is unlocked");
                }
                SharedPreferencesImpl sharedPreferencesImpl2 = new SharedPreferencesImpl(file, i);
                sharedPreferencesCacheLocked.put(file, sharedPreferencesImpl2);
                return sharedPreferencesImpl2;
            }
            if ((i & 4) != 0 || getApplicationInfo().targetSdkVersion < 11) {
                sharedPreferencesImpl.startReloadIfChangedUnexpectedly();
            }
            return sharedPreferencesImpl;
        }
    }

    @GuardedBy("ContextImpl.class")
    private ArrayMap<File, SharedPreferencesImpl> getSharedPreferencesCacheLocked() {
        if (sSharedPrefsCache == null) {
            sSharedPrefsCache = new ArrayMap<>();
        }
        String packageName = getPackageName();
        ArrayMap<File, SharedPreferencesImpl> arrayMap = sSharedPrefsCache.get(packageName);
        if (arrayMap == null) {
            ArrayMap<File, SharedPreferencesImpl> arrayMap2 = new ArrayMap<>();
            sSharedPrefsCache.put(packageName, arrayMap2);
            return arrayMap2;
        }
        return arrayMap;
    }

    @Override
    public void reloadSharedPreferences() {
        int i;
        ArrayList arrayList = new ArrayList();
        synchronized (ContextImpl.class) {
            ArrayMap<File, SharedPreferencesImpl> sharedPreferencesCacheLocked = getSharedPreferencesCacheLocked();
            for (int i2 = 0; i2 < sharedPreferencesCacheLocked.size(); i2++) {
                SharedPreferencesImpl sharedPreferencesImplValueAt = sharedPreferencesCacheLocked.valueAt(i2);
                if (sharedPreferencesImplValueAt != null) {
                    arrayList.add(sharedPreferencesImplValueAt);
                }
            }
        }
        for (i = 0; i < arrayList.size(); i++) {
            ((SharedPreferencesImpl) arrayList.get(i)).startReloadIfChangedUnexpectedly();
        }
    }

    private static int moveFiles(File file, File file2, final String str) throws Exception {
        int i = 0;
        for (File file3 : FileUtils.listFilesOrEmpty(file, new FilenameFilter() {
            @Override
            public boolean accept(File file4, String str2) {
                return str2.startsWith(str);
            }
        })) {
            File file4 = new File(file2, file3.getName());
            Log.d(TAG, "Migrating " + file3 + " to " + file4);
            try {
                FileUtils.copyFileOrThrow(file3, file4);
                FileUtils.copyPermissions(file3, file4);
                if (!file3.delete()) {
                    throw new IOException("Failed to clean up " + file3);
                }
                if (i != -1) {
                    i++;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to migrate " + file3 + ": " + e);
                i = -1;
            }
        }
        return i;
    }

    @Override
    public boolean moveSharedPreferencesFrom(Context context, String str) {
        boolean z;
        synchronized (ContextImpl.class) {
            File sharedPreferencesPath = context.getSharedPreferencesPath(str);
            File sharedPreferencesPath2 = getSharedPreferencesPath(str);
            int iMoveFiles = moveFiles(sharedPreferencesPath.getParentFile(), sharedPreferencesPath2.getParentFile(), sharedPreferencesPath.getName());
            if (iMoveFiles > 0) {
                ArrayMap<File, SharedPreferencesImpl> sharedPreferencesCacheLocked = getSharedPreferencesCacheLocked();
                sharedPreferencesCacheLocked.remove(sharedPreferencesPath);
                sharedPreferencesCacheLocked.remove(sharedPreferencesPath2);
            }
            z = iMoveFiles != -1;
        }
        return z;
    }

    @Override
    public boolean deleteSharedPreferences(String str) {
        boolean z;
        synchronized (ContextImpl.class) {
            File sharedPreferencesPath = getSharedPreferencesPath(str);
            File fileMakeBackupFile = SharedPreferencesImpl.makeBackupFile(sharedPreferencesPath);
            getSharedPreferencesCacheLocked().remove(sharedPreferencesPath);
            sharedPreferencesPath.delete();
            fileMakeBackupFile.delete();
            z = (sharedPreferencesPath.exists() || fileMakeBackupFile.exists()) ? false : true;
        }
        return z;
    }

    private File getPreferencesDir() {
        File fileEnsurePrivateDirExists;
        synchronized (this.mSync) {
            if (this.mPreferencesDir == null) {
                this.mPreferencesDir = new File(getDataDir(), "shared_prefs");
            }
            fileEnsurePrivateDirExists = ensurePrivateDirExists(this.mPreferencesDir);
        }
        return fileEnsurePrivateDirExists;
    }

    @Override
    public FileInputStream openFileInput(String str) throws FileNotFoundException {
        return new FileInputStream(makeFilename(getFilesDir(), str));
    }

    @Override
    public FileOutputStream openFileOutput(String str, int i) throws FileNotFoundException {
        checkMode(i);
        boolean z = (32768 & i) != 0;
        File fileMakeFilename = makeFilename(getFilesDir(), str);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileMakeFilename, z);
            setFilePermissionsFromMode(fileMakeFilename.getPath(), i, 0);
            return fileOutputStream;
        } catch (FileNotFoundException e) {
            File parentFile = fileMakeFilename.getParentFile();
            parentFile.mkdir();
            FileUtils.setPermissions(parentFile.getPath(), 505, -1, -1);
            FileOutputStream fileOutputStream2 = new FileOutputStream(fileMakeFilename, z);
            setFilePermissionsFromMode(fileMakeFilename.getPath(), i, 0);
            return fileOutputStream2;
        }
    }

    @Override
    public boolean deleteFile(String str) {
        return makeFilename(getFilesDir(), str).delete();
    }

    private static File ensurePrivateDirExists(File file) {
        return ensurePrivateDirExists(file, 505, -1, null);
    }

    private static File ensurePrivateCacheDirExists(File file, String str) {
        return ensurePrivateDirExists(file, 1529, UserHandle.getCacheAppGid(Process.myUid()), str);
    }

    private static File ensurePrivateDirExists(File file, int i, int i2, String str) {
        if (!file.exists()) {
            String absolutePath = file.getAbsolutePath();
            try {
                Os.mkdir(absolutePath, i);
                Os.chmod(absolutePath, i);
                if (i2 != -1) {
                    Os.chown(absolutePath, -1, i2);
                }
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.EEXIST) {
                    Log.w(TAG, "Failed to ensure " + file + ": " + e.getMessage());
                }
            }
            if (str != null) {
                try {
                    byte[] bArr = new byte[8];
                    Memory.pokeLong(bArr, 0, Os.stat(file.getAbsolutePath()).st_ino, ByteOrder.nativeOrder());
                    Os.setxattr(file.getParentFile().getAbsolutePath(), str, bArr, 0);
                } catch (ErrnoException e2) {
                    Log.w(TAG, "Failed to update " + str + ": " + e2.getMessage());
                }
            }
        }
        return file;
    }

    @Override
    public File getFilesDir() {
        File fileEnsurePrivateDirExists;
        synchronized (this.mSync) {
            if (this.mFilesDir == null) {
                this.mFilesDir = new File(getDataDir(), "files");
            }
            fileEnsurePrivateDirExists = ensurePrivateDirExists(this.mFilesDir);
        }
        return fileEnsurePrivateDirExists;
    }

    @Override
    public File getNoBackupFilesDir() {
        File fileEnsurePrivateDirExists;
        synchronized (this.mSync) {
            if (this.mNoBackupFilesDir == null) {
                this.mNoBackupFilesDir = new File(getDataDir(), "no_backup");
            }
            fileEnsurePrivateDirExists = ensurePrivateDirExists(this.mNoBackupFilesDir);
        }
        return fileEnsurePrivateDirExists;
    }

    @Override
    public File getExternalFilesDir(String str) {
        File[] externalFilesDirs = getExternalFilesDirs(str);
        if (externalFilesDirs == null || externalFilesDirs.length <= 0) {
            return null;
        }
        return externalFilesDirs[0];
    }

    @Override
    public File[] getExternalFilesDirs(String str) {
        File[] fileArrEnsureExternalDirsExistOrFilter;
        synchronized (this.mSync) {
            File[] fileArrBuildExternalStorageAppFilesDirs = Environment.buildExternalStorageAppFilesDirs(getPackageName());
            if (str != null) {
                fileArrBuildExternalStorageAppFilesDirs = Environment.buildPaths(fileArrBuildExternalStorageAppFilesDirs, str);
            }
            fileArrEnsureExternalDirsExistOrFilter = ensureExternalDirsExistOrFilter(fileArrBuildExternalStorageAppFilesDirs);
        }
        return fileArrEnsureExternalDirsExistOrFilter;
    }

    @Override
    public File getObbDir() {
        File[] obbDirs = getObbDirs();
        if (obbDirs == null || obbDirs.length <= 0) {
            return null;
        }
        return obbDirs[0];
    }

    @Override
    public File[] getObbDirs() {
        File[] fileArrEnsureExternalDirsExistOrFilter;
        synchronized (this.mSync) {
            fileArrEnsureExternalDirsExistOrFilter = ensureExternalDirsExistOrFilter(Environment.buildExternalStorageAppObbDirs(getPackageName()));
        }
        return fileArrEnsureExternalDirsExistOrFilter;
    }

    @Override
    public File getCacheDir() {
        File fileEnsurePrivateCacheDirExists;
        synchronized (this.mSync) {
            if (this.mCacheDir == null) {
                this.mCacheDir = new File(getDataDir(), "cache");
            }
            fileEnsurePrivateCacheDirExists = ensurePrivateCacheDirExists(this.mCacheDir, XATTR_INODE_CACHE);
        }
        return fileEnsurePrivateCacheDirExists;
    }

    @Override
    public File getCodeCacheDir() {
        File fileEnsurePrivateCacheDirExists;
        synchronized (this.mSync) {
            if (this.mCodeCacheDir == null) {
                this.mCodeCacheDir = new File(getDataDir(), "code_cache");
            }
            fileEnsurePrivateCacheDirExists = ensurePrivateCacheDirExists(this.mCodeCacheDir, XATTR_INODE_CODE_CACHE);
        }
        return fileEnsurePrivateCacheDirExists;
    }

    @Override
    public File getExternalCacheDir() {
        File[] externalCacheDirs = getExternalCacheDirs();
        if (externalCacheDirs == null || externalCacheDirs.length <= 0) {
            return null;
        }
        return externalCacheDirs[0];
    }

    @Override
    public File[] getExternalCacheDirs() {
        File[] fileArrEnsureExternalDirsExistOrFilter;
        synchronized (this.mSync) {
            fileArrEnsureExternalDirsExistOrFilter = ensureExternalDirsExistOrFilter(Environment.buildExternalStorageAppCacheDirs(getPackageName()));
        }
        return fileArrEnsureExternalDirsExistOrFilter;
    }

    @Override
    public File[] getExternalMediaDirs() {
        File[] fileArrEnsureExternalDirsExistOrFilter;
        synchronized (this.mSync) {
            fileArrEnsureExternalDirsExistOrFilter = ensureExternalDirsExistOrFilter(Environment.buildExternalStorageAppMediaDirs(getPackageName()));
        }
        return fileArrEnsureExternalDirsExistOrFilter;
    }

    @Override
    public File getPreloadsFileCache() {
        return Environment.getDataPreloadsFileCacheDirectory(getPackageName());
    }

    @Override
    public File getFileStreamPath(String str) {
        return makeFilename(getFilesDir(), str);
    }

    @Override
    public File getSharedPreferencesPath(String str) {
        return makeFilename(getPreferencesDir(), str + ".xml");
    }

    @Override
    public String[] fileList() {
        return FileUtils.listOrEmpty(getFilesDir());
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String str, int i, SQLiteDatabase.CursorFactory cursorFactory) {
        return openOrCreateDatabase(str, i, cursorFactory, null);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String str, int i, SQLiteDatabase.CursorFactory cursorFactory, DatabaseErrorHandler databaseErrorHandler) {
        int i2;
        checkMode(i);
        File databasePath = getDatabasePath(str);
        if ((i & 8) != 0) {
            i2 = 805306368;
        } else {
            i2 = 268435456;
        }
        if ((i & 16) != 0) {
            i2 |= 16;
        }
        SQLiteDatabase sQLiteDatabaseOpenDatabase = SQLiteDatabase.openDatabase(databasePath.getPath(), cursorFactory, i2, databaseErrorHandler);
        setFilePermissionsFromMode(databasePath.getPath(), i, 0);
        return sQLiteDatabaseOpenDatabase;
    }

    @Override
    public boolean moveDatabaseFrom(Context context, String str) {
        boolean z;
        synchronized (ContextImpl.class) {
            File databasePath = context.getDatabasePath(str);
            z = moveFiles(databasePath.getParentFile(), getDatabasePath(str).getParentFile(), databasePath.getName()) != -1;
        }
        return z;
    }

    @Override
    public boolean deleteDatabase(String str) {
        try {
            return SQLiteDatabase.deleteDatabase(getDatabasePath(str));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public File getDatabasePath(String str) {
        if (str.charAt(0) == File.separatorChar) {
            File file = new File(str.substring(0, str.lastIndexOf(File.separatorChar)));
            File file2 = new File(file, str.substring(str.lastIndexOf(File.separatorChar)));
            if (!file.isDirectory() && file.mkdir()) {
                FileUtils.setPermissions(file.getPath(), 505, -1, -1);
                return file2;
            }
            return file2;
        }
        return makeFilename(getDatabasesDir(), str);
    }

    @Override
    public String[] databaseList() {
        return FileUtils.listOrEmpty(getDatabasesDir());
    }

    private File getDatabasesDir() {
        File fileEnsurePrivateDirExists;
        synchronized (this.mSync) {
            if (this.mDatabasesDir == null) {
                if (ZenModeConfig.SYSTEM_AUTHORITY.equals(getPackageName())) {
                    this.mDatabasesDir = new File("/data/system");
                } else {
                    this.mDatabasesDir = new File(getDataDir(), "databases");
                }
            }
            fileEnsurePrivateDirExists = ensurePrivateDirExists(this.mDatabasesDir);
        }
        return fileEnsurePrivateDirExists;
    }

    @Override
    @Deprecated
    public Drawable getWallpaper() {
        return getWallpaperManager().getDrawable();
    }

    @Override
    @Deprecated
    public Drawable peekWallpaper() {
        return getWallpaperManager().peekDrawable();
    }

    @Override
    @Deprecated
    public int getWallpaperDesiredMinimumWidth() {
        return getWallpaperManager().getDesiredMinimumWidth();
    }

    @Override
    @Deprecated
    public int getWallpaperDesiredMinimumHeight() {
        return getWallpaperManager().getDesiredMinimumHeight();
    }

    @Override
    @Deprecated
    public void setWallpaper(Bitmap bitmap) throws IOException {
        getWallpaperManager().setBitmap(bitmap);
    }

    @Override
    @Deprecated
    public void setWallpaper(InputStream inputStream) throws IOException {
        getWallpaperManager().setStream(inputStream);
    }

    @Override
    @Deprecated
    public void clearWallpaper() throws IOException {
        getWallpaperManager().clear();
    }

    private WallpaperManager getWallpaperManager() {
        return (WallpaperManager) getSystemService(WallpaperManager.class);
    }

    @Override
    public void startActivity(Intent intent) {
        warnIfCallingFromSystemProcess();
        startActivity(intent, null);
    }

    @Override
    public void startActivityAsUser(Intent intent, UserHandle userHandle) {
        startActivityAsUser(intent, null, userHandle);
    }

    @Override
    public void startActivity(Intent intent, Bundle bundle) {
        warnIfCallingFromSystemProcess();
        int i = getApplicationInfo().targetSdkVersion;
        if ((intent.getFlags() & 268435456) == 0 && ((i < 24 || i >= 28) && (bundle == null || ActivityOptions.fromBundle(bundle).getLaunchTaskId() == -1))) {
            throw new AndroidRuntimeException("Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?");
        }
        this.mMainThread.getInstrumentation().execStartActivity(getOuterContext(), this.mMainThread.getApplicationThread(), (IBinder) null, (Activity) null, intent, -1, bundle);
    }

    @Override
    public void startActivityAsUser(Intent intent, Bundle bundle, UserHandle userHandle) {
        try {
            ActivityManager.getService().startActivityAsUser(this.mMainThread.getApplicationThread(), getBasePackageName(), intent, intent.resolveTypeIfNeeded(getContentResolver()), null, null, 0, 268435456, null, bundle, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void startActivities(Intent[] intentArr) {
        warnIfCallingFromSystemProcess();
        startActivities(intentArr, null);
    }

    @Override
    public int startActivitiesAsUser(Intent[] intentArr, Bundle bundle, UserHandle userHandle) {
        if ((intentArr[0].getFlags() & 268435456) == 0) {
            throw new AndroidRuntimeException("Calling startActivities() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag on first Intent. Is this really what you want?");
        }
        return this.mMainThread.getInstrumentation().execStartActivitiesAsUser(getOuterContext(), this.mMainThread.getApplicationThread(), null, (Activity) null, intentArr, bundle, userHandle.getIdentifier());
    }

    @Override
    public void startActivities(Intent[] intentArr, Bundle bundle) {
        warnIfCallingFromSystemProcess();
        if ((intentArr[0].getFlags() & 268435456) == 0) {
            throw new AndroidRuntimeException("Calling startActivities() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag on first Intent. Is this really what you want?");
        }
        this.mMainThread.getInstrumentation().execStartActivities(getOuterContext(), this.mMainThread.getApplicationThread(), null, (Activity) null, intentArr, bundle);
    }

    @Override
    public void startIntentSender(IntentSender intentSender, Intent intent, int i, int i2, int i3) throws IntentSender.SendIntentException {
        startIntentSender(intentSender, intent, i, i2, i3, null);
    }

    @Override
    public void startIntentSender(IntentSender intentSender, Intent intent, int i, int i2, int i3, Bundle bundle) throws IntentSender.SendIntentException {
        String strResolveTypeIfNeeded;
        if (intent != null) {
            try {
                intent.migrateExtraStreamToClipData();
                intent.prepareToLeaveProcess(this);
                strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        } else {
            strResolveTypeIfNeeded = null;
        }
        int iStartActivityIntentSender = ActivityManager.getService().startActivityIntentSender(this.mMainThread.getApplicationThread(), intentSender != null ? intentSender.getTarget() : null, intentSender != null ? intentSender.getWhitelistToken() : null, intent, strResolveTypeIfNeeded, null, null, 0, i, i2, bundle);
        if (iStartActivityIntentSender == -96) {
            throw new IntentSender.SendIntentException();
        }
        Instrumentation.checkStartActivityResult(iStartActivityIntentSender, null);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        warnIfCallingFromSystemProcess();
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, null, -1, null, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcast(Intent intent, String str) {
        String[] strArr;
        warnIfCallingFromSystemProcess();
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (str != null) {
            strArr = new String[]{str};
        } else {
            strArr = null;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, strArr, -1, null, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcastMultiplePermissions(Intent intent, String[] strArr) {
        warnIfCallingFromSystemProcess();
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, strArr, -1, null, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcastAsUserMultiplePermissions(Intent intent, UserHandle userHandle, String[] strArr) {
        warnIfCallingFromSystemProcess();
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, strArr, -1, null, false, false, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcast(Intent intent, String str, Bundle bundle) {
        String[] strArr;
        warnIfCallingFromSystemProcess();
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (str != null) {
            strArr = new String[]{str};
        } else {
            strArr = null;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, strArr, -1, bundle, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcast(Intent intent, String str, int i) {
        String[] strArr;
        warnIfCallingFromSystemProcess();
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (str != null) {
            strArr = new String[]{str};
        } else {
            strArr = null;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, strArr, i, null, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String str) {
        String[] strArr;
        warnIfCallingFromSystemProcess();
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (str != null) {
            strArr = new String[]{str};
        } else {
            strArr = null;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, strArr, -1, null, true, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String str, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str2, Bundle bundle) {
        sendOrderedBroadcast(intent, str, -1, broadcastReceiver, handler, i, str2, bundle, null);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String str, Bundle bundle, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str2, Bundle bundle2) {
        sendOrderedBroadcast(intent, str, -1, broadcastReceiver, handler, i, str2, bundle2, bundle);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String str, int i, BroadcastReceiver broadcastReceiver, Handler handler, int i2, String str2, Bundle bundle) {
        sendOrderedBroadcast(intent, str, i, broadcastReceiver, handler, i2, str2, bundle, null);
    }

    void sendOrderedBroadcast(Intent intent, String str, int i, BroadcastReceiver broadcastReceiver, Handler handler, int i2, String str2, Bundle bundle, Bundle bundle2) {
        IIntentReceiver iIntentReceiver;
        IIntentReceiver iIntentReceiver2;
        warnIfCallingFromSystemProcess();
        String[] strArr = null;
        if (broadcastReceiver != null) {
            if (this.mPackageInfo != null) {
                iIntentReceiver2 = this.mPackageInfo.getReceiverDispatcher(broadcastReceiver, getOuterContext(), handler == null ? this.mMainThread.getHandler() : handler, this.mMainThread.getInstrumentation(), false);
            } else {
                iIntentReceiver2 = new LoadedApk.ReceiverDispatcher(broadcastReceiver, getOuterContext(), handler == null ? this.mMainThread.getHandler() : handler, null, false).getIIntentReceiver();
            }
            iIntentReceiver = iIntentReceiver2;
        } else {
            iIntentReceiver = null;
        }
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (str != null) {
            strArr = new String[]{str};
        }
        String[] strArr2 = strArr;
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, iIntentReceiver, i2, str2, bundle, strArr2, i, bundle2, true, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle) {
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, null, -1, null, false, false, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle, String str) {
        sendBroadcastAsUser(intent, userHandle, str, -1);
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle, String str, Bundle bundle) {
        String[] strArr;
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (str != null) {
            strArr = new String[]{str};
        } else {
            strArr = null;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, strArr, -1, bundle, false, false, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle, String str, int i) {
        String[] strArr;
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (str != null) {
            strArr = new String[]{str};
        } else {
            strArr = null;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, strArr, i, null, false, false, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, String str, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str2, Bundle bundle) {
        sendOrderedBroadcastAsUser(intent, userHandle, str, -1, null, broadcastReceiver, handler, i, str2, bundle);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, String str, int i, BroadcastReceiver broadcastReceiver, Handler handler, int i2, String str2, Bundle bundle) {
        sendOrderedBroadcastAsUser(intent, userHandle, str, i, null, broadcastReceiver, handler, i2, str2, bundle);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, String str, int i, Bundle bundle, BroadcastReceiver broadcastReceiver, Handler handler, int i2, String str2, Bundle bundle2) {
        IIntentReceiver iIntentReceiver;
        IIntentReceiver iIntentReceiver2;
        String[] strArr = null;
        if (broadcastReceiver != null) {
            if (this.mPackageInfo != null) {
                iIntentReceiver2 = this.mPackageInfo.getReceiverDispatcher(broadcastReceiver, getOuterContext(), handler == null ? this.mMainThread.getHandler() : handler, this.mMainThread.getInstrumentation(), false);
            } else {
                iIntentReceiver2 = new LoadedApk.ReceiverDispatcher(broadcastReceiver, getOuterContext(), handler == null ? this.mMainThread.getHandler() : handler, null, false).getIIntentReceiver();
            }
            iIntentReceiver = iIntentReceiver2;
        } else {
            iIntentReceiver = null;
        }
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (str != null) {
            strArr = new String[]{str};
        }
        String[] strArr2 = strArr;
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, iIntentReceiver, i2, str2, bundle2, strArr2, i, bundle, true, false, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyBroadcast(Intent intent) {
        warnIfCallingFromSystemProcess();
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, null, -1, null, false, true, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str, Bundle bundle) {
        IIntentReceiver iIntentReceiver;
        warnIfCallingFromSystemProcess();
        if (broadcastReceiver != null) {
            if (this.mPackageInfo != null) {
                iIntentReceiver = this.mPackageInfo.getReceiverDispatcher(broadcastReceiver, getOuterContext(), handler == null ? this.mMainThread.getHandler() : handler, this.mMainThread.getInstrumentation(), false);
            } else {
                iIntentReceiver = new LoadedApk.ReceiverDispatcher(broadcastReceiver, getOuterContext(), handler == null ? this.mMainThread.getHandler() : handler, null, false).getIIntentReceiver();
            }
        } else {
            iIntentReceiver = null;
        }
        IIntentReceiver iIntentReceiver2 = iIntentReceiver;
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, iIntentReceiver2, i, str, bundle, null, -1, null, true, true, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void removeStickyBroadcast(Intent intent) {
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (strResolveTypeIfNeeded != null) {
            Intent intent2 = new Intent(intent);
            intent2.setDataAndType(intent2.getData(), strResolveTypeIfNeeded);
            intent = intent2;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().unbroadcastIntent(this.mMainThread.getApplicationThread(), intent, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle userHandle) {
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, null, -1, null, false, true, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle userHandle, Bundle bundle) {
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, null, -1, null, null, null, -1, bundle, false, true, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str, Bundle bundle) {
        IIntentReceiver iIntentReceiver;
        if (broadcastReceiver != null) {
            if (this.mPackageInfo != null) {
                iIntentReceiver = this.mPackageInfo.getReceiverDispatcher(broadcastReceiver, getOuterContext(), handler == null ? this.mMainThread.getHandler() : handler, this.mMainThread.getInstrumentation(), false);
            } else {
                iIntentReceiver = new LoadedApk.ReceiverDispatcher(broadcastReceiver, getOuterContext(), handler == null ? this.mMainThread.getHandler() : handler, null, false).getIIntentReceiver();
            }
        } else {
            iIntentReceiver = null;
        }
        IIntentReceiver iIntentReceiver2 = iIntentReceiver;
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().broadcastIntent(this.mMainThread.getApplicationThread(), intent, strResolveTypeIfNeeded, iIntentReceiver2, i, str, bundle, null, -1, null, true, true, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle userHandle) {
        String strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(getContentResolver());
        if (strResolveTypeIfNeeded != null) {
            Intent intent2 = new Intent(intent);
            intent2.setDataAndType(intent2.getData(), strResolveTypeIfNeeded);
            intent = intent2;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManager.getService().unbroadcastIntent(this.mMainThread.getApplicationThread(), intent, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter) {
        return registerReceiver(broadcastReceiver, intentFilter, null, null);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, int i) {
        return registerReceiver(broadcastReceiver, intentFilter, null, null, i);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, String str, Handler handler) {
        return registerReceiverInternal(broadcastReceiver, getUserId(), intentFilter, str, handler, getOuterContext(), 0);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, String str, Handler handler, int i) {
        return registerReceiverInternal(broadcastReceiver, getUserId(), intentFilter, str, handler, getOuterContext(), i);
    }

    @Override
    public Intent registerReceiverAsUser(BroadcastReceiver broadcastReceiver, UserHandle userHandle, IntentFilter intentFilter, String str, Handler handler) {
        return registerReceiverInternal(broadcastReceiver, userHandle.getIdentifier(), intentFilter, str, handler, getOuterContext(), 0);
    }

    private Intent registerReceiverInternal(BroadcastReceiver broadcastReceiver, int i, IntentFilter intentFilter, String str, Handler handler, Context context, int i2) {
        IIntentReceiver iIntentReceiver;
        if (broadcastReceiver != null) {
            if (this.mPackageInfo != null && context != null) {
                if (handler == null) {
                    handler = this.mMainThread.getHandler();
                }
                iIntentReceiver = this.mPackageInfo.getReceiverDispatcher(broadcastReceiver, context, handler, this.mMainThread.getInstrumentation(), true);
            } else {
                if (handler == null) {
                    handler = this.mMainThread.getHandler();
                }
                iIntentReceiver = new LoadedApk.ReceiverDispatcher(broadcastReceiver, context, handler, null, true).getIIntentReceiver();
            }
        } else {
            iIntentReceiver = null;
        }
        try {
            Intent intentRegisterReceiver = ActivityManager.getService().registerReceiver(this.mMainThread.getApplicationThread(), this.mBasePackageName, iIntentReceiver, intentFilter, str, i, i2);
            if (intentRegisterReceiver != null) {
                intentRegisterReceiver.setExtrasClassLoader(getClassLoader());
                intentRegisterReceiver.prepareToEnterProcess();
            }
            return intentRegisterReceiver;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {
        if (this.mPackageInfo != null) {
            try {
                ActivityManager.getService().unregisterReceiver(this.mPackageInfo.forgetReceiverDispatcher(getOuterContext(), broadcastReceiver));
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    private void validateServiceIntent(Intent intent) {
        if (intent.getComponent() == null && intent.getPackage() == null) {
            if (getApplicationInfo().targetSdkVersion >= 21) {
                throw new IllegalArgumentException("Service Intent must be explicit: " + intent);
            }
            Log.w(TAG, "Implicit intents with startService are not safe: " + intent + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + Debug.getCallers(2, 3));
        }
    }

    @Override
    public ComponentName startService(Intent intent) {
        warnIfCallingFromSystemProcess();
        return startServiceCommon(intent, false, this.mUser);
    }

    @Override
    public ComponentName startForegroundService(Intent intent) {
        warnIfCallingFromSystemProcess();
        return startServiceCommon(intent, true, this.mUser);
    }

    @Override
    public boolean stopService(Intent intent) {
        warnIfCallingFromSystemProcess();
        return stopServiceCommon(intent, this.mUser);
    }

    @Override
    public ComponentName startServiceAsUser(Intent intent, UserHandle userHandle) {
        return startServiceCommon(intent, false, userHandle);
    }

    @Override
    public ComponentName startForegroundServiceAsUser(Intent intent, UserHandle userHandle) {
        return startServiceCommon(intent, true, userHandle);
    }

    private ComponentName startServiceCommon(Intent intent, boolean z, UserHandle userHandle) {
        try {
            validateServiceIntent(intent);
            intent.prepareToLeaveProcess(this);
            ComponentName componentNameStartService = ActivityManager.getService().startService(this.mMainThread.getApplicationThread(), intent, intent.resolveTypeIfNeeded(getContentResolver()), z, getOpPackageName(), userHandle.getIdentifier());
            if (componentNameStartService != null) {
                if (componentNameStartService.getPackageName().equals("!")) {
                    throw new SecurityException("Not allowed to start service " + intent + " without permission " + componentNameStartService.getClassName());
                }
                if (componentNameStartService.getPackageName().equals("!!")) {
                    throw new SecurityException("Unable to start service " + intent + ": " + componentNameStartService.getClassName());
                }
                if (componentNameStartService.getPackageName().equals("?")) {
                    throw new IllegalStateException("Not allowed to start service " + intent + ": " + componentNameStartService.getClassName());
                }
            }
            return componentNameStartService;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public boolean stopServiceAsUser(Intent intent, UserHandle userHandle) {
        return stopServiceCommon(intent, userHandle);
    }

    private boolean stopServiceCommon(Intent intent, UserHandle userHandle) {
        try {
            validateServiceIntent(intent);
            intent.prepareToLeaveProcess(this);
            int iStopService = ActivityManager.getService().stopService(this.mMainThread.getApplicationThread(), intent, intent.resolveTypeIfNeeded(getContentResolver()), userHandle.getIdentifier());
            if (iStopService >= 0) {
                return iStopService != 0;
            }
            throw new SecurityException("Not allowed to stop service " + intent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        warnIfCallingFromSystemProcess();
        return bindServiceCommon(intent, serviceConnection, i, this.mMainThread.getHandler(), getUser());
    }

    @Override
    public boolean bindServiceAsUser(Intent intent, ServiceConnection serviceConnection, int i, UserHandle userHandle) {
        return bindServiceCommon(intent, serviceConnection, i, this.mMainThread.getHandler(), userHandle);
    }

    @Override
    public boolean bindServiceAsUser(Intent intent, ServiceConnection serviceConnection, int i, Handler handler, UserHandle userHandle) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null.");
        }
        return bindServiceCommon(intent, serviceConnection, i, handler, userHandle);
    }

    @Override
    public IServiceConnection getServiceDispatcher(ServiceConnection serviceConnection, Handler handler, int i) {
        return this.mPackageInfo.getServiceDispatcher(serviceConnection, getOuterContext(), handler, i);
    }

    @Override
    public IApplicationThread getIApplicationThread() {
        return this.mMainThread.getApplicationThread();
    }

    @Override
    public Handler getMainThreadHandler() {
        return this.mMainThread.getHandler();
    }

    private boolean bindServiceCommon(Intent intent, ServiceConnection serviceConnection, int i, Handler handler, UserHandle userHandle) {
        if (serviceConnection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (this.mPackageInfo != null) {
            IServiceConnection serviceDispatcher = this.mPackageInfo.getServiceDispatcher(serviceConnection, getOuterContext(), handler, i);
            validateServiceIntent(intent);
            try {
                int i2 = (getActivityToken() != null || (i & 1) != 0 || this.mPackageInfo == null || this.mPackageInfo.getApplicationInfo().targetSdkVersion >= 14) ? i : i | 32;
                intent.prepareToLeaveProcess(this);
                int iBindService = ActivityManager.getService().bindService(this.mMainThread.getApplicationThread(), getActivityToken(), intent, intent.resolveTypeIfNeeded(getContentResolver()), serviceDispatcher, i2, getOpPackageName(), userHandle.getIdentifier());
                if (iBindService >= 0) {
                    return iBindService != 0;
                }
                throw new SecurityException("Not allowed to bind to service " + intent);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        if (serviceConnection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (this.mPackageInfo != null) {
            try {
                ActivityManager.getService().unbindService(this.mPackageInfo.forgetServiceDispatcher(getOuterContext(), serviceConnection));
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public boolean startInstrumentation(ComponentName componentName, String str, Bundle bundle) {
        if (bundle != null) {
            try {
                bundle.setAllowFds(false);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return ActivityManager.getService().startInstrumentation(componentName, str, 0, bundle, null, null, getUserId(), null);
    }

    @Override
    public Object getSystemService(String str) {
        return SystemServiceRegistry.getSystemService(this, str);
    }

    @Override
    public String getSystemServiceName(Class<?> cls) {
        return SystemServiceRegistry.getSystemServiceName(cls);
    }

    @Override
    public int checkPermission(String str, int i, int i2) {
        if (str == null) {
            throw new IllegalArgumentException("permission is null");
        }
        IActivityManager service = ActivityManager.getService();
        if (service == null) {
            int appId = UserHandle.getAppId(i2);
            if (appId == 0 || appId == 1000) {
                Slog.w(TAG, "Missing ActivityManager; assuming " + i2 + " holds " + str);
                return 0;
            }
            Slog.w(TAG, "Missing ActivityManager; assuming " + i2 + " does not hold " + str);
            return -1;
        }
        try {
            return service.checkPermission(str, i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public int checkPermission(String str, int i, int i2, IBinder iBinder) {
        if (str == null) {
            throw new IllegalArgumentException("permission is null");
        }
        try {
            return ActivityManager.getService().checkPermissionWithToken(str, i, i2, iBinder);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public int checkCallingPermission(String str) {
        if (str == null) {
            throw new IllegalArgumentException("permission is null");
        }
        int callingPid = Binder.getCallingPid();
        if (callingPid != Process.myPid()) {
            return checkPermission(str, callingPid, Binder.getCallingUid());
        }
        return -1;
    }

    @Override
    public int checkCallingOrSelfPermission(String str) {
        if (str == null) {
            throw new IllegalArgumentException("permission is null");
        }
        return checkPermission(str, Binder.getCallingPid(), Binder.getCallingUid());
    }

    @Override
    public int checkSelfPermission(String str) {
        if (str == null) {
            throw new IllegalArgumentException("permission is null");
        }
        return checkPermission(str, Process.myPid(), Process.myUid());
    }

    private void enforce(String str, int i, boolean z, int i2, String str2) {
        String str3;
        String str4;
        if (i != 0) {
            StringBuilder sb = new StringBuilder();
            if (str2 != null) {
                str3 = str2 + ": ";
            } else {
                str3 = "";
            }
            sb.append(str3);
            if (z) {
                str4 = "Neither user " + i2 + " nor current process has ";
            } else {
                str4 = "uid " + i2 + " does not have ";
            }
            sb.append(str4);
            sb.append(str);
            sb.append(".");
            throw new SecurityException(sb.toString());
        }
    }

    @Override
    public void enforcePermission(String str, int i, int i2, String str2) {
        enforce(str, checkPermission(str, i, i2), false, i2, str2);
    }

    @Override
    public void enforceCallingPermission(String str, String str2) {
        enforce(str, checkCallingPermission(str), false, Binder.getCallingUid(), str2);
    }

    @Override
    public void enforceCallingOrSelfPermission(String str, String str2) {
        enforce(str, checkCallingOrSelfPermission(str), true, Binder.getCallingUid(), str2);
    }

    @Override
    public void grantUriPermission(String str, Uri uri, int i) {
        try {
            ActivityManager.getService().grantUriPermission(this.mMainThread.getApplicationThread(), str, ContentProvider.getUriWithoutUserId(uri), i, resolveUserId(uri));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void revokeUriPermission(Uri uri, int i) {
        try {
            ActivityManager.getService().revokeUriPermission(this.mMainThread.getApplicationThread(), null, ContentProvider.getUriWithoutUserId(uri), i, resolveUserId(uri));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void revokeUriPermission(String str, Uri uri, int i) {
        try {
            ActivityManager.getService().revokeUriPermission(this.mMainThread.getApplicationThread(), str, ContentProvider.getUriWithoutUserId(uri), i, resolveUserId(uri));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public int checkUriPermission(Uri uri, int i, int i2, int i3) {
        try {
            return ActivityManager.getService().checkUriPermission(ContentProvider.getUriWithoutUserId(uri), i, i2, i3, resolveUserId(uri), null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public int checkUriPermission(Uri uri, int i, int i2, int i3, IBinder iBinder) {
        try {
            return ActivityManager.getService().checkUriPermission(ContentProvider.getUriWithoutUserId(uri), i, i2, i3, resolveUserId(uri), iBinder);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private int resolveUserId(Uri uri) {
        return ContentProvider.getUserIdFromUri(uri, getUserId());
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int i) {
        int callingPid = Binder.getCallingPid();
        if (callingPid != Process.myPid()) {
            return checkUriPermission(uri, callingPid, Binder.getCallingUid(), i);
        }
        return -1;
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int i) {
        return checkUriPermission(uri, Binder.getCallingPid(), Binder.getCallingUid(), i);
    }

    @Override
    public int checkUriPermission(Uri uri, String str, String str2, int i, int i2, int i3) {
        if ((i3 & 1) != 0 && (str == null || checkPermission(str, i, i2) == 0)) {
            return 0;
        }
        if ((i3 & 2) != 0 && (str2 == null || checkPermission(str2, i, i2) == 0)) {
            return 0;
        }
        if (uri != null) {
            return checkUriPermission(uri, i, i2, i3);
        }
        return -1;
    }

    private String uriModeFlagToString(int i) {
        StringBuilder sb = new StringBuilder();
        if ((i & 1) != 0) {
            sb.append("read and ");
        }
        if ((i & 2) != 0) {
            sb.append("write and ");
        }
        if ((i & 64) != 0) {
            sb.append("persistable and ");
        }
        if ((i & 128) != 0) {
            sb.append("prefix and ");
        }
        if (sb.length() > 5) {
            sb.setLength(sb.length() - 5);
            return sb.toString();
        }
        throw new IllegalArgumentException("Unknown permission mode flags: " + i);
    }

    private void enforceForUri(int i, int i2, boolean z, int i3, Uri uri, String str) {
        String str2;
        String str3;
        if (i2 != 0) {
            StringBuilder sb = new StringBuilder();
            if (str != null) {
                str2 = str + ": ";
            } else {
                str2 = "";
            }
            sb.append(str2);
            if (z) {
                str3 = "Neither user " + i3 + " nor current process has ";
            } else {
                str3 = "User " + i3 + " does not have ";
            }
            sb.append(str3);
            sb.append(uriModeFlagToString(i));
            sb.append(" permission on ");
            sb.append(uri);
            sb.append(".");
            throw new SecurityException(sb.toString());
        }
    }

    @Override
    public void enforceUriPermission(Uri uri, int i, int i2, int i3, String str) {
        enforceForUri(i3, checkUriPermission(uri, i, i2, i3), false, i2, uri, str);
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int i, String str) {
        enforceForUri(i, checkCallingUriPermission(uri, i), false, Binder.getCallingUid(), uri, str);
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int i, String str) {
        enforceForUri(i, checkCallingOrSelfUriPermission(uri, i), true, Binder.getCallingUid(), uri, str);
    }

    @Override
    public void enforceUriPermission(Uri uri, String str, String str2, int i, int i2, int i3, String str3) {
        enforceForUri(i3, checkUriPermission(uri, str, str2, i, i2, i3), false, i2, uri, str3);
    }

    private void warnIfCallingFromSystemProcess() {
        if (Process.myUid() == 1000) {
            Slog.w(TAG, "Calling a method in the system process without a qualified user: " + Debug.getCallers(5));
        }
    }

    private static Resources createResources(IBinder iBinder, LoadedApk loadedApk, String str, int i, Configuration configuration, CompatibilityInfo compatibilityInfo) {
        try {
            return ResourcesManager.getInstance().getResources(iBinder, loadedApk.getResDir(), loadedApk.getSplitPaths(str), loadedApk.getOverlayDirs(), loadedApk.getApplicationInfo().sharedLibraryFiles, i, configuration, compatibilityInfo, loadedApk.getSplitClassLoader(str));
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Context createApplicationContext(ApplicationInfo applicationInfo, int i) throws PackageManager.NameNotFoundException {
        LoadedApk packageInfo = this.mMainThread.getPackageInfo(applicationInfo, this.mResources.getCompatibilityInfo(), 1073741824 | i);
        if (packageInfo != null) {
            ContextImpl contextImpl = new ContextImpl(this, this.mMainThread, packageInfo, null, this.mActivityToken, new UserHandle(UserHandle.getUserId(applicationInfo.uid)), i, null);
            int displayId = this.mDisplay != null ? this.mDisplay.getDisplayId() : 0;
            contextImpl.setResources(createResources(this.mActivityToken, packageInfo, null, displayId, null, getDisplayAdjustments(displayId).getCompatibilityInfo()));
            if (contextImpl.mResources != null) {
                return contextImpl;
            }
        }
        throw new PackageManager.NameNotFoundException("Application package " + applicationInfo.packageName + " not found");
    }

    @Override
    public Context createPackageContext(String str, int i) throws PackageManager.NameNotFoundException {
        return createPackageContextAsUser(str, i, this.mUser);
    }

    @Override
    public Context createPackageContextAsUser(String str, int i, UserHandle userHandle) throws PackageManager.NameNotFoundException {
        if (str.equals(StorageManager.UUID_SYSTEM) || str.equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
            return new ContextImpl(this, this.mMainThread, this.mPackageInfo, null, this.mActivityToken, userHandle, i, null);
        }
        LoadedApk packageInfo = this.mMainThread.getPackageInfo(str, this.mResources.getCompatibilityInfo(), 1073741824 | i, userHandle.getIdentifier());
        if (packageInfo != null) {
            ContextImpl contextImpl = new ContextImpl(this, this.mMainThread, packageInfo, null, this.mActivityToken, userHandle, i, null);
            int displayId = this.mDisplay != null ? this.mDisplay.getDisplayId() : 0;
            contextImpl.setResources(createResources(this.mActivityToken, packageInfo, null, displayId, null, getDisplayAdjustments(displayId).getCompatibilityInfo()));
            if (contextImpl.mResources != null) {
                return contextImpl;
            }
        }
        throw new PackageManager.NameNotFoundException("Application package " + str + " not found");
    }

    @Override
    public Context createContextForSplit(String str) throws PackageManager.NameNotFoundException {
        if (!this.mPackageInfo.getApplicationInfo().requestsIsolatedSplitLoading()) {
            return this;
        }
        ClassLoader splitClassLoader = this.mPackageInfo.getSplitClassLoader(str);
        String[] splitPaths = this.mPackageInfo.getSplitPaths(str);
        ContextImpl contextImpl = new ContextImpl(this, this.mMainThread, this.mPackageInfo, str, this.mActivityToken, this.mUser, this.mFlags, splitClassLoader);
        contextImpl.setResources(ResourcesManager.getInstance().getResources(this.mActivityToken, this.mPackageInfo.getResDir(), splitPaths, this.mPackageInfo.getOverlayDirs(), this.mPackageInfo.getApplicationInfo().sharedLibraryFiles, this.mDisplay != null ? this.mDisplay.getDisplayId() : 0, null, this.mPackageInfo.getCompatibilityInfo(), splitClassLoader));
        return contextImpl;
    }

    @Override
    public Context createConfigurationContext(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("overrideConfiguration must not be null");
        }
        ContextImpl contextImpl = new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mSplitName, this.mActivityToken, this.mUser, this.mFlags, this.mClassLoader);
        int displayId = this.mDisplay != null ? this.mDisplay.getDisplayId() : 0;
        contextImpl.setResources(createResources(this.mActivityToken, this.mPackageInfo, this.mSplitName, displayId, configuration, getDisplayAdjustments(displayId).getCompatibilityInfo()));
        return contextImpl;
    }

    @Override
    public Context createDisplayContext(Display display) {
        if (display == null) {
            throw new IllegalArgumentException("display must not be null");
        }
        ContextImpl contextImpl = new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mSplitName, this.mActivityToken, this.mUser, this.mFlags, this.mClassLoader);
        int displayId = display.getDisplayId();
        contextImpl.setResources(createResources(this.mActivityToken, this.mPackageInfo, this.mSplitName, displayId, null, getDisplayAdjustments(displayId).getCompatibilityInfo()));
        contextImpl.mDisplay = display;
        return contextImpl;
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mSplitName, this.mActivityToken, this.mUser, (this.mFlags & (-17)) | 8, this.mClassLoader);
    }

    @Override
    public Context createCredentialProtectedStorageContext() {
        return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mSplitName, this.mActivityToken, this.mUser, (this.mFlags & (-9)) | 16, this.mClassLoader);
    }

    @Override
    public boolean isRestricted() {
        return (this.mFlags & 4) != 0;
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        return (this.mFlags & 8) != 0;
    }

    @Override
    public boolean isCredentialProtectedStorage() {
        return (this.mFlags & 16) != 0;
    }

    @Override
    public boolean canLoadUnsafeResources() {
        return getPackageName().equals(getOpPackageName()) || (this.mFlags & 2) != 0;
    }

    @Override
    public Display getDisplay() {
        if (this.mDisplay == null) {
            return this.mResourcesManager.getAdjustedDisplay(0, this.mResources);
        }
        return this.mDisplay;
    }

    @Override
    public void updateDisplay(int i) {
        this.mDisplay = this.mResourcesManager.getAdjustedDisplay(i, this.mResources);
    }

    @Override
    public DisplayAdjustments getDisplayAdjustments(int i) {
        return this.mResources.getDisplayAdjustments();
    }

    @Override
    public File getDataDir() {
        File dataDirFile;
        if (this.mPackageInfo != null) {
            if (isCredentialProtectedStorage()) {
                dataDirFile = this.mPackageInfo.getCredentialProtectedDataDirFile();
            } else if (isDeviceProtectedStorage()) {
                dataDirFile = this.mPackageInfo.getDeviceProtectedDataDirFile();
            } else {
                dataDirFile = this.mPackageInfo.getDataDirFile();
            }
            if (dataDirFile != null) {
                if (!dataDirFile.exists() && Process.myUid() == 1000) {
                    Log.wtf(TAG, "Data directory doesn't exist for package " + getPackageName(), new Throwable());
                }
                return dataDirFile;
            }
            throw new RuntimeException("No data directory found for package " + getPackageName());
        }
        throw new RuntimeException("No package details found for package " + getPackageName());
    }

    @Override
    public File getDir(String str, int i) {
        checkMode(i);
        File fileMakeFilename = makeFilename(getDataDir(), "app_" + str);
        if (!fileMakeFilename.exists()) {
            fileMakeFilename.mkdir();
            setFilePermissionsFromMode(fileMakeFilename.getPath(), i, 505);
        }
        return fileMakeFilename;
    }

    @Override
    public UserHandle getUser() {
        return this.mUser;
    }

    @Override
    public int getUserId() {
        return this.mUser.getIdentifier();
    }

    @Override
    public AutofillManager.AutofillClient getAutofillClient() {
        return this.mAutofillClient;
    }

    @Override
    public void setAutofillClient(AutofillManager.AutofillClient autofillClient) {
        this.mAutofillClient = autofillClient;
    }

    @Override
    public boolean isAutofillCompatibilityEnabled() {
        return this.mIsAutofillCompatEnabled;
    }

    @Override
    public void setAutofillCompatibilityEnabled(boolean z) {
        this.mIsAutofillCompatEnabled = z;
    }

    static ContextImpl createSystemContext(ActivityThread activityThread) {
        LoadedApk loadedApk = new LoadedApk(activityThread);
        ContextImpl contextImpl = new ContextImpl(null, activityThread, loadedApk, null, null, null, 0, null);
        contextImpl.setResources(loadedApk.getResources());
        contextImpl.mResources.updateConfiguration(contextImpl.mResourcesManager.getConfiguration(), contextImpl.mResourcesManager.getDisplayMetrics());
        return contextImpl;
    }

    static ContextImpl createSystemUiContext(ContextImpl contextImpl) {
        LoadedApk loadedApk = contextImpl.mPackageInfo;
        ContextImpl contextImpl2 = new ContextImpl(null, contextImpl.mMainThread, loadedApk, null, null, null, 0, null);
        contextImpl2.setResources(createResources(null, loadedApk, null, 0, null, loadedApk.getCompatibilityInfo()));
        return contextImpl2;
    }

    static ContextImpl createAppContext(ActivityThread activityThread, LoadedApk loadedApk) {
        if (loadedApk == null) {
            throw new IllegalArgumentException("packageInfo");
        }
        ContextImpl contextImpl = new ContextImpl(null, activityThread, loadedApk, null, null, null, 0, null);
        contextImpl.setResources(loadedApk.getResources());
        return contextImpl;
    }

    static ContextImpl createActivityContext(ActivityThread activityThread, LoadedApk loadedApk, ActivityInfo activityInfo, IBinder iBinder, int i, Configuration configuration) {
        String[] strArr;
        ClassLoader classLoader;
        int i2;
        CompatibilityInfo compatibilityInfo;
        if (loadedApk == null) {
            throw new IllegalArgumentException("packageInfo");
        }
        String[] splitResDirs = loadedApk.getSplitResDirs();
        ClassLoader classLoader2 = loadedApk.getClassLoader();
        if (loadedApk.getApplicationInfo().requestsIsolatedSplitLoading()) {
            Trace.traceBegin(8192L, "SplitDependencies");
            try {
                try {
                    ClassLoader splitClassLoader = loadedApk.getSplitClassLoader(activityInfo.splitName);
                    String[] splitPaths = loadedApk.getSplitPaths(activityInfo.splitName);
                    Trace.traceEnd(8192L);
                    classLoader = splitClassLoader;
                    strArr = splitPaths;
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (Throwable th) {
                Trace.traceEnd(8192L);
                throw th;
            }
        } else {
            strArr = splitResDirs;
            classLoader = classLoader2;
        }
        ContextImpl contextImpl = new ContextImpl(null, activityThread, loadedApk, activityInfo.splitName, iBinder, null, 0, classLoader);
        if (i == -1) {
            i2 = 0;
        } else {
            i2 = i;
        }
        if (i2 == 0) {
            compatibilityInfo = loadedApk.getCompatibilityInfo();
        } else {
            compatibilityInfo = CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO;
        }
        CompatibilityInfo compatibilityInfo2 = compatibilityInfo;
        ResourcesManager resourcesManager = ResourcesManager.getInstance();
        contextImpl.setResources(resourcesManager.createBaseActivityResources(iBinder, loadedApk.getResDir(), strArr, loadedApk.getOverlayDirs(), loadedApk.getApplicationInfo().sharedLibraryFiles, i2, configuration, compatibilityInfo2, classLoader));
        contextImpl.mDisplay = resourcesManager.getAdjustedDisplay(i2, contextImpl.getResources());
        return contextImpl;
    }

    private ContextImpl(ContextImpl contextImpl, ActivityThread activityThread, LoadedApk loadedApk, String str, IBinder iBinder, UserHandle userHandle, int i, ClassLoader classLoader) {
        this.mSplitName = null;
        if ((i & 24) == 0) {
            File dataDirFile = loadedApk.getDataDirFile();
            if (Objects.equals(dataDirFile, loadedApk.getCredentialProtectedDataDirFile())) {
                i |= 16;
            } else if (Objects.equals(dataDirFile, loadedApk.getDeviceProtectedDataDirFile())) {
                i |= 8;
            }
        }
        this.mMainThread = activityThread;
        this.mActivityToken = iBinder;
        this.mFlags = i;
        this.mUser = userHandle == null ? Process.myUserHandle() : userHandle;
        this.mPackageInfo = loadedApk;
        this.mSplitName = str;
        this.mClassLoader = classLoader;
        this.mResourcesManager = ResourcesManager.getInstance();
        if (contextImpl != null) {
            this.mBasePackageName = contextImpl.mBasePackageName;
            this.mOpPackageName = contextImpl.mOpPackageName;
            setResources(contextImpl.mResources);
            this.mDisplay = contextImpl.mDisplay;
        } else {
            this.mBasePackageName = loadedApk.mPackageName;
            ApplicationInfo applicationInfo = loadedApk.getApplicationInfo();
            if (applicationInfo.uid == 1000 && applicationInfo.uid != Process.myUid()) {
                this.mOpPackageName = ActivityThread.currentPackageName();
            } else {
                this.mOpPackageName = this.mBasePackageName;
            }
        }
        this.mContentResolver = new ApplicationContentResolver(this, activityThread);
    }

    void setResources(Resources resources) {
        if (resources instanceof CompatResources) {
            ((CompatResources) resources).setContext(this);
        }
        this.mResources = resources;
    }

    void installSystemApplicationInfo(ApplicationInfo applicationInfo, ClassLoader classLoader) {
        this.mPackageInfo.installSystemApplicationInfo(applicationInfo, classLoader);
    }

    final void scheduleFinalCleanup(String str, String str2) {
        this.mMainThread.scheduleContextCleanup(this, str, str2);
    }

    final void performFinalCleanup(String str, String str2) {
        this.mPackageInfo.removeContextRegistrations(getOuterContext(), str, str2);
    }

    final Context getReceiverRestrictedContext() {
        if (this.mReceiverRestrictedContext != null) {
            return this.mReceiverRestrictedContext;
        }
        ReceiverRestrictedContext receiverRestrictedContext = new ReceiverRestrictedContext(getOuterContext());
        this.mReceiverRestrictedContext = receiverRestrictedContext;
        return receiverRestrictedContext;
    }

    final void setOuterContext(Context context) {
        this.mOuterContext = context;
    }

    final Context getOuterContext() {
        return this.mOuterContext;
    }

    @Override
    public IBinder getActivityToken() {
        return this.mActivityToken;
    }

    private void checkMode(int i) {
        if (getApplicationInfo().targetSdkVersion >= 24) {
            if ((i & 1) != 0) {
                throw new SecurityException("MODE_WORLD_READABLE no longer supported");
            }
            if ((i & 2) != 0) {
                throw new SecurityException("MODE_WORLD_WRITEABLE no longer supported");
            }
        }
    }

    static void setFilePermissionsFromMode(String str, int i, int i2) {
        int i3 = i2 | DevicePolicyManager.PROFILE_KEYGUARD_FEATURES_AFFECT_OWNER;
        if ((i & 1) != 0) {
            i3 |= 4;
        }
        if ((i & 2) != 0) {
            i3 |= 2;
        }
        FileUtils.setPermissions(str, i3, -1, -1);
    }

    private File makeFilename(File file, String str) {
        if (str.indexOf(File.separatorChar) < 0) {
            return new File(file, str);
        }
        throw new IllegalArgumentException("File " + str + " contains a path separator");
    }

    private File[] ensureExternalDirsExistOrFilter(File[] fileArr) {
        StorageManager storageManager = (StorageManager) getSystemService(StorageManager.class);
        File[] fileArr2 = new File[fileArr.length];
        for (int i = 0; i < fileArr.length; i++) {
            File file = fileArr[i];
            if (!file.exists() && !file.mkdirs() && !file.exists()) {
                try {
                    storageManager.mkdirs(file);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to ensure " + file + ": " + e);
                    file = null;
                }
            }
            fileArr2[i] = file;
        }
        return fileArr2;
    }

    private static final class ApplicationContentResolver extends ContentResolver {
        private final ActivityThread mMainThread;

        public ApplicationContentResolver(Context context, ActivityThread activityThread) {
            super(context);
            this.mMainThread = (ActivityThread) Preconditions.checkNotNull(activityThread);
        }

        @Override
        protected IContentProvider acquireProvider(Context context, String str) {
            return this.mMainThread.acquireProvider(context, ContentProvider.getAuthorityWithoutUserId(str), resolveUserIdFromAuthority(str), true);
        }

        @Override
        protected IContentProvider acquireExistingProvider(Context context, String str) {
            return this.mMainThread.acquireExistingProvider(context, ContentProvider.getAuthorityWithoutUserId(str), resolveUserIdFromAuthority(str), true);
        }

        @Override
        public boolean releaseProvider(IContentProvider iContentProvider) {
            return this.mMainThread.releaseProvider(iContentProvider, true);
        }

        @Override
        protected IContentProvider acquireUnstableProvider(Context context, String str) {
            return this.mMainThread.acquireProvider(context, ContentProvider.getAuthorityWithoutUserId(str), resolveUserIdFromAuthority(str), false);
        }

        @Override
        public boolean releaseUnstableProvider(IContentProvider iContentProvider) {
            return this.mMainThread.releaseProvider(iContentProvider, false);
        }

        @Override
        public void unstableProviderDied(IContentProvider iContentProvider) {
            this.mMainThread.handleUnstableProviderDied(iContentProvider.asBinder(), true);
        }

        @Override
        public void appNotRespondingViaProvider(IContentProvider iContentProvider) {
            this.mMainThread.appNotRespondingViaProvider(iContentProvider.asBinder());
        }

        protected int resolveUserIdFromAuthority(String str) {
            return ContentProvider.getUserIdFromAuthority(str, getUserId());
        }
    }
}
