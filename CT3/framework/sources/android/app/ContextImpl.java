package android.app;

import android.app.LoadedApk;
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
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ProxyInfo;
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
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IMountService;
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
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import com.mediatek.aee.ExceptionLog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

class ContextImpl extends Context {
    private static final boolean DEBUG = false;
    private static final String TAG = "ContextImpl";

    @GuardedBy("ContextImpl.class")
    private static ArrayMap<String, ArrayMap<File, SharedPreferencesImpl>> sSharedPrefsCache;
    private final IBinder mActivityToken;
    private final String mBasePackageName;

    @GuardedBy("mSync")
    private File mCacheDir;

    @GuardedBy("mSync")
    private File mCodeCacheDir;
    private final ApplicationContentResolver mContentResolver;

    @GuardedBy("mSync")
    private File mDatabasesDir;
    private Display mDisplay;

    @GuardedBy("mSync")
    private File mFilesDir;
    private final int mFlags;
    final ActivityThread mMainThread;

    @GuardedBy("mSync")
    private File mNoBackupFilesDir;
    private final String mOpPackageName;
    final LoadedApk mPackageInfo;
    private PackageManager mPackageManager;

    @GuardedBy("mSync")
    private File mPreferencesDir;
    private final Resources mResources;
    private final ResourcesManager mResourcesManager;

    @GuardedBy("ContextImpl.class")
    private ArrayMap<String, File> mSharedPrefsPaths;
    private final UserHandle mUser;
    private int mThemeResource = 0;
    private Resources.Theme mTheme = null;
    private Context mReceiverRestrictedContext = null;
    private final Object mSync = new Object();
    final Object[] mServiceCache = SystemServiceRegistry.createServiceCache();
    private Context mOuterContext = this;

    static ContextImpl getImpl(Context context) {
        Context nextContext;
        while ((context instanceof ContextWrapper) && (nextContext = ((ContextWrapper) context).getBaseContext()) != null) {
            context = nextContext;
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
        IPackageManager pm = ActivityThread.getPackageManager();
        if (pm == null) {
            return null;
        }
        ApplicationPackageManager applicationPackageManager = new ApplicationPackageManager(this, pm);
        this.mPackageManager = applicationPackageManager;
        return applicationPackageManager;
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
    public Context getApplicationContext() {
        return this.mPackageInfo != null ? this.mPackageInfo.getApplication() : this.mMainThread.getApplication();
    }

    @Override
    public void setTheme(int resId) {
        if (this.mThemeResource == resId) {
            return;
        }
        this.mThemeResource = resId;
        initializeTheme();
    }

    @Override
    public int getThemeResId() {
        return this.mThemeResource;
    }

    @Override
    public Resources.Theme getTheme() {
        if (this.mTheme != null) {
            return this.mTheme;
        }
        this.mThemeResource = Resources.selectDefaultTheme(this.mThemeResource, getOuterContext().getApplicationInfo().targetSdkVersion);
        initializeTheme();
        return this.mTheme;
    }

    private void initializeTheme() {
        if (this.mTheme == null) {
            this.mTheme = this.mResources.newTheme();
        }
        this.mTheme.applyStyle(this.mThemeResource, true);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.mPackageInfo != null ? this.mPackageInfo.getClassLoader() : ClassLoader.getSystemClassLoader();
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
    public SharedPreferences getSharedPreferences(String name, int mode) {
        File file;
        if (this.mPackageInfo.getApplicationInfo().targetSdkVersion < 19 && name == null) {
            name = "null";
        }
        synchronized (ContextImpl.class) {
            if (this.mSharedPrefsPaths == null) {
                this.mSharedPrefsPaths = new ArrayMap<>();
            }
            file = this.mSharedPrefsPaths.get(name);
            if (file == null) {
                file = getSharedPreferencesPath(name);
                this.mSharedPrefsPaths.put(name, file);
            }
        }
        return getSharedPreferences(file, mode);
    }

    @Override
    public SharedPreferences getSharedPreferences(File file, int mode) {
        checkMode(mode);
        synchronized (ContextImpl.class) {
            ArrayMap<File, SharedPreferencesImpl> cache = getSharedPreferencesCacheLocked();
            SharedPreferencesImpl sp = cache.get(file);
            if (sp == null) {
                SharedPreferencesImpl sp2 = new SharedPreferencesImpl(file, mode);
                cache.put(file, sp2);
                return sp2;
            }
            if ((mode & 4) != 0 || getApplicationInfo().targetSdkVersion < 11) {
                sp.startReloadIfChangedUnexpectedly();
            }
            return sp;
        }
    }

    private ArrayMap<File, SharedPreferencesImpl> getSharedPreferencesCacheLocked() {
        if (sSharedPrefsCache == null) {
            sSharedPrefsCache = new ArrayMap<>();
        }
        String packageName = getPackageName();
        ArrayMap<File, SharedPreferencesImpl> packagePrefs = sSharedPrefsCache.get(packageName);
        if (packagePrefs == null) {
            ArrayMap<File, SharedPreferencesImpl> packagePrefs2 = new ArrayMap<>();
            sSharedPrefsCache.put(packageName, packagePrefs2);
            return packagePrefs2;
        }
        return packagePrefs;
    }

    private static int moveFiles(File sourceDir, File targetDir, final String prefix) throws Throwable {
        File[] sourceFiles = FileUtils.listFilesOrEmpty(sourceDir, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix);
            }
        });
        int res = 0;
        for (File sourceFile : sourceFiles) {
            File targetFile = new File(targetDir, sourceFile.getName());
            Log.d(TAG, "Migrating " + sourceFile + " to " + targetFile);
            try {
                FileUtils.copyFileOrThrow(sourceFile, targetFile);
                FileUtils.copyPermissions(sourceFile, targetFile);
                if (!sourceFile.delete()) {
                    throw new IOException("Failed to clean up " + sourceFile);
                }
                if (res != -1) {
                    res++;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to migrate " + sourceFile + ": " + e);
                res = -1;
            }
        }
        return res;
    }

    @Override
    public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
        boolean z;
        synchronized (ContextImpl.class) {
            File source = sourceContext.getSharedPreferencesPath(name);
            File target = getSharedPreferencesPath(name);
            int res = moveFiles(source.getParentFile(), target.getParentFile(), source.getName());
            if (res > 0) {
                ArrayMap<File, SharedPreferencesImpl> cache = getSharedPreferencesCacheLocked();
                cache.remove(source);
                cache.remove(target);
            }
            z = res != -1;
        }
        return z;
    }

    @Override
    public boolean deleteSharedPreferences(String name) {
        boolean z;
        synchronized (ContextImpl.class) {
            File prefs = getSharedPreferencesPath(name);
            File prefsBackup = SharedPreferencesImpl.makeBackupFile(prefs);
            ArrayMap<File, SharedPreferencesImpl> cache = getSharedPreferencesCacheLocked();
            cache.remove(prefs);
            prefs.delete();
            prefsBackup.delete();
            if (!prefs.exists()) {
                z = !prefsBackup.exists();
            }
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
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        File f = makeFilename(getFilesDir(), name);
        return new FileInputStream(f);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        checkMode(mode);
        boolean append = (32768 & mode) != 0;
        File f = makeFilename(getFilesDir(), name);
        try {
            FileOutputStream fos = new FileOutputStream(f, append);
            setFilePermissionsFromMode(f.getPath(), mode, 0);
            return fos;
        } catch (FileNotFoundException e) {
            File parent = f.getParentFile();
            parent.mkdir();
            FileUtils.setPermissions(parent.getPath(), IActivityManager.GET_STICKY_WINDOW_TRANSACTION, -1, -1);
            FileOutputStream fos2 = new FileOutputStream(f, append);
            setFilePermissionsFromMode(f.getPath(), mode, 0);
            return fos2;
        }
    }

    @Override
    public boolean deleteFile(String name) {
        File f = makeFilename(getFilesDir(), name);
        return f.delete();
    }

    private static File ensurePrivateDirExists(File file) {
        if (!file.exists()) {
            try {
                Os.mkdir(file.getAbsolutePath(), IActivityManager.GET_STICKY_WINDOW_TRANSACTION);
                Os.chmod(file.getAbsolutePath(), IActivityManager.GET_STICKY_WINDOW_TRANSACTION);
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.EEXIST) {
                    Log.w(TAG, "Failed to ensure " + file + ": " + e.getMessage());
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
    public File getExternalFilesDir(String type) {
        return getExternalFilesDirs(type)[0];
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        File[] fileArrEnsureExternalDirsExistOrFilter;
        synchronized (this.mSync) {
            File[] dirs = Environment.buildExternalStorageAppFilesDirs(getPackageName());
            if (type != null) {
                dirs = Environment.buildPaths(dirs, type);
            }
            fileArrEnsureExternalDirsExistOrFilter = ensureExternalDirsExistOrFilter(dirs);
        }
        return fileArrEnsureExternalDirsExistOrFilter;
    }

    @Override
    public File getObbDir() {
        return getObbDirs()[0];
    }

    @Override
    public File[] getObbDirs() {
        File[] fileArrEnsureExternalDirsExistOrFilter;
        synchronized (this.mSync) {
            File[] dirs = Environment.buildExternalStorageAppObbDirs(getPackageName());
            fileArrEnsureExternalDirsExistOrFilter = ensureExternalDirsExistOrFilter(dirs);
        }
        return fileArrEnsureExternalDirsExistOrFilter;
    }

    @Override
    public File getCacheDir() {
        File fileEnsurePrivateDirExists;
        synchronized (this.mSync) {
            if (this.mCacheDir == null) {
                this.mCacheDir = new File(getDataDir(), "cache");
            }
            fileEnsurePrivateDirExists = ensurePrivateDirExists(this.mCacheDir);
        }
        return fileEnsurePrivateDirExists;
    }

    @Override
    public File getCodeCacheDir() {
        File fileEnsurePrivateDirExists;
        synchronized (this.mSync) {
            if (this.mCodeCacheDir == null) {
                this.mCodeCacheDir = new File(getDataDir(), "code_cache");
            }
            fileEnsurePrivateDirExists = ensurePrivateDirExists(this.mCodeCacheDir);
        }
        return fileEnsurePrivateDirExists;
    }

    @Override
    public File getExternalCacheDir() {
        return getExternalCacheDirs()[0];
    }

    @Override
    public File[] getExternalCacheDirs() {
        File[] fileArrEnsureExternalDirsExistOrFilter;
        synchronized (this.mSync) {
            File[] dirs = Environment.buildExternalStorageAppCacheDirs(getPackageName());
            fileArrEnsureExternalDirsExistOrFilter = ensureExternalDirsExistOrFilter(dirs);
        }
        return fileArrEnsureExternalDirsExistOrFilter;
    }

    @Override
    public File[] getExternalMediaDirs() {
        File[] fileArrEnsureExternalDirsExistOrFilter;
        synchronized (this.mSync) {
            File[] dirs = Environment.buildExternalStorageAppMediaDirs(getPackageName());
            fileArrEnsureExternalDirsExistOrFilter = ensureExternalDirsExistOrFilter(dirs);
        }
        return fileArrEnsureExternalDirsExistOrFilter;
    }

    @Override
    public File getFileStreamPath(String name) {
        return makeFilename(getFilesDir(), name);
    }

    @Override
    public File getSharedPreferencesPath(String name) {
        return makeFilename(getPreferencesDir(), name + ".xml");
    }

    @Override
    public String[] fileList() {
        return FileUtils.listOrEmpty(getFilesDir());
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return openOrCreateDatabase(name, mode, factory, null);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        checkMode(mode);
        File f = getDatabasePath(name);
        int flags = 268435456;
        if ((mode & 8) != 0) {
            flags = 805306368;
        }
        if ((mode & 16) != 0) {
            flags |= 16;
        }
        SQLiteDatabase db = SQLiteDatabase.openDatabase(f.getPath(), factory, flags, errorHandler);
        setFilePermissionsFromMode(f.getPath(), mode, 0);
        return db;
    }

    @Override
    public boolean moveDatabaseFrom(Context sourceContext, String name) {
        boolean z;
        synchronized (ContextImpl.class) {
            File source = sourceContext.getDatabasePath(name);
            File target = getDatabasePath(name);
            z = moveFiles(source.getParentFile(), target.getParentFile(), source.getName()) != -1;
        }
        return z;
    }

    @Override
    public boolean deleteDatabase(String name) {
        try {
            File f = getDatabasePath(name);
            return SQLiteDatabase.deleteDatabase(f);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public File getDatabasePath(String name) {
        if (name.charAt(0) == File.separatorChar) {
            String dirPath = name.substring(0, name.lastIndexOf(File.separatorChar));
            File dir = new File(dirPath);
            File f = new File(dir, name.substring(name.lastIndexOf(File.separatorChar)));
            if (!dir.isDirectory() && dir.mkdir()) {
                FileUtils.setPermissions(dir.getPath(), IActivityManager.GET_STICKY_WINDOW_TRANSACTION, -1, -1);
                return f;
            }
            return f;
        }
        return makeFilename(getDatabasesDir(), name);
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
    public void setWallpaper(InputStream data) throws IOException {
        getWallpaperManager().setStream(data);
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
    public void startActivityAsUser(Intent intent, UserHandle user) {
        startActivityAsUser(intent, null, user);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        warnIfCallingFromSystemProcess();
        if ((intent.getFlags() & 268435456) == 0 && options != null && ActivityOptions.fromBundle(options).getLaunchTaskId() == -1) {
            throw new AndroidRuntimeException("Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?");
        }
        this.mMainThread.getInstrumentation().execStartActivity(getOuterContext(), this.mMainThread.getApplicationThread(), (IBinder) null, (Activity) null, intent, -1, options);
    }

    @Override
    public void startActivityAsUser(Intent intent, Bundle options, UserHandle user) {
        try {
            ActivityManagerNative.getDefault().startActivityAsUser(this.mMainThread.getApplicationThread(), getBasePackageName(), intent, intent.resolveTypeIfNeeded(getContentResolver()), null, null, 0, 268435456, null, options, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void startActivities(Intent[] intents) {
        warnIfCallingFromSystemProcess();
        startActivities(intents, null);
    }

    @Override
    public void startActivitiesAsUser(Intent[] intents, Bundle options, UserHandle userHandle) {
        if ((intents[0].getFlags() & 268435456) == 0) {
            throw new AndroidRuntimeException("Calling startActivities() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag on first Intent. Is this really what you want?");
        }
        this.mMainThread.getInstrumentation().execStartActivitiesAsUser(getOuterContext(), this.mMainThread.getApplicationThread(), null, (Activity) null, intents, options, userHandle.getIdentifier());
    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {
        warnIfCallingFromSystemProcess();
        if ((intents[0].getFlags() & 268435456) == 0) {
            throw new AndroidRuntimeException("Calling startActivities() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag on first Intent. Is this really what you want?");
        }
        this.mMainThread.getInstrumentation().execStartActivities(getOuterContext(), this.mMainThread.getApplicationThread(), null, (Activity) null, intents, options);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, null);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        String resolvedType = null;
        if (fillInIntent != null) {
            try {
                fillInIntent.migrateExtraStreamToClipData();
                fillInIntent.prepareToLeaveProcess(this);
                resolvedType = fillInIntent.resolveTypeIfNeeded(getContentResolver());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        int result = ActivityManagerNative.getDefault().startActivityIntentSender(this.mMainThread.getApplicationThread(), intent, fillInIntent, resolvedType, null, null, 0, flagsMask, flagsValues, options);
        if (result == -6) {
            throw new IntentSender.SendIntentException();
        }
        Instrumentation.checkStartActivityResult(result, null);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, null, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        String[] strArr = receiverPermission == null ? null : new String[]{receiverPermission};
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, strArr, -1, null, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcastMultiplePermissions(Intent intent, String[] receiverPermissions) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, receiverPermissions, -1, null, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission, Bundle options) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        String[] strArr = receiverPermission == null ? null : new String[]{receiverPermission};
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, strArr, -1, options, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission, int appOp) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        String[] strArr = receiverPermission == null ? null : new String[]{receiverPermission};
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, strArr, appOp, null, false, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        String[] strArr = receiverPermission == null ? null : new String[]{receiverPermission};
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, strArr, -1, null, true, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        sendOrderedBroadcast(intent, receiverPermission, -1, resultReceiver, scheduler, initialCode, initialData, initialExtras, null);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, Bundle options, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        sendOrderedBroadcast(intent, receiverPermission, -1, resultReceiver, scheduler, initialCode, initialData, initialExtras, options);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, int appOp, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        sendOrderedBroadcast(intent, receiverPermission, appOp, resultReceiver, scheduler, initialCode, initialData, initialExtras, null);
    }

    void sendOrderedBroadcast(Intent intent, String receiverPermission, int appOp, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras, Bundle options) {
        warnIfCallingFromSystemProcess();
        IIntentReceiver rd = null;
        if (resultReceiver != null) {
            if (this.mPackageInfo != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, this.mMainThread.getInstrumentation(), false);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, null, false).getIIntentReceiver();
            }
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        String[] strArr = receiverPermission == null ? null : new String[]{receiverPermission};
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, rd, initialCode, initialData, initialExtras, strArr, appOp, options, true, false, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, null, false, false, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
        sendBroadcastAsUser(intent, user, receiverPermission, -1);
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, int appOp) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        String[] strArr = receiverPermission == null ? null : new String[]{receiverPermission};
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, strArr, appOp, null, false, false, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        sendOrderedBroadcastAsUser(intent, user, receiverPermission, -1, null, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, int appOp, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        sendOrderedBroadcastAsUser(intent, user, receiverPermission, appOp, null, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, int appOp, Bundle options, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        IIntentReceiver rd = null;
        if (resultReceiver != null) {
            if (this.mPackageInfo != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, this.mMainThread.getInstrumentation(), false);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, null, false).getIIntentReceiver();
            }
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        String[] strArr = receiverPermission == null ? null : new String[]{receiverPermission};
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, rd, initialCode, initialData, initialExtras, strArr, appOp, options, true, false, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyBroadcast(Intent intent) {
        warnIfCallingFromSystemProcess();
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, null, false, true, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        warnIfCallingFromSystemProcess();
        IIntentReceiver rd = null;
        if (resultReceiver != null) {
            if (this.mPackageInfo != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, this.mMainThread.getInstrumentation(), false);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, null, false).getIIntentReceiver();
            }
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, rd, initialCode, initialData, initialExtras, null, -1, null, true, true, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void removeStickyBroadcast(Intent intent) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        if (resolvedType != null) {
            Intent intent2 = new Intent(intent);
            intent2.setDataAndType(intent2.getData(), resolvedType);
            intent = intent2;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().unbroadcastIntent(this.mMainThread.getApplicationThread(), intent, getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, null, false, true, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user, Bundle options) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, null, -1, null, null, null, -1, options, false, true, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        IIntentReceiver rd = null;
        if (resultReceiver != null) {
            if (this.mPackageInfo != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, this.mMainThread.getInstrumentation(), false);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(resultReceiver, getOuterContext(), scheduler, null, false).getIIntentReceiver();
            }
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().broadcastIntent(this.mMainThread.getApplicationThread(), intent, resolvedType, rd, initialCode, initialData, initialExtras, null, -1, null, true, true, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    @Deprecated
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        if (resolvedType != null) {
            Intent intent2 = new Intent(intent);
            intent2.setDataAndType(intent2.getData(), resolvedType);
            intent = intent2;
        }
        try {
            intent.prepareToLeaveProcess(this);
            ActivityManagerNative.getDefault().unbroadcastIntent(this.mMainThread.getApplicationThread(), intent, user.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return registerReceiver(receiver, filter, null, null);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return registerReceiverInternal(receiver, getUserId(), filter, broadcastPermission, scheduler, getOuterContext());
    }

    @Override
    public Intent registerReceiverAsUser(BroadcastReceiver receiver, UserHandle user, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return registerReceiverInternal(receiver, user.getIdentifier(), filter, broadcastPermission, scheduler, getOuterContext());
    }

    private Intent registerReceiverInternal(BroadcastReceiver receiver, int userId, IntentFilter filter, String broadcastPermission, Handler scheduler, Context context) {
        IIntentReceiver rd = null;
        if (receiver != null) {
            if (this.mPackageInfo != null && context != null) {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = this.mPackageInfo.getReceiverDispatcher(receiver, context, scheduler, this.mMainThread.getInstrumentation(), true);
            } else {
                if (scheduler == null) {
                    scheduler = this.mMainThread.getHandler();
                }
                rd = new LoadedApk.ReceiverDispatcher(receiver, context, scheduler, null, true).getIIntentReceiver();
            }
        }
        try {
            Intent intent = ActivityManagerNative.getDefault().registerReceiver(this.mMainThread.getApplicationThread(), this.mBasePackageName, rd, filter, broadcastPermission, userId);
            if (intent != null) {
                intent.setExtrasClassLoader(getClassLoader());
                intent.prepareToEnterProcess();
            }
            return intent;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (this.mPackageInfo != null) {
            IIntentReceiver rd = this.mPackageInfo.forgetReceiverDispatcher(getOuterContext(), receiver);
            try {
                ActivityManagerNative.getDefault().unregisterReceiver(rd);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    private void validateServiceIntent(Intent service) {
        if (service.getComponent() != null || service.getPackage() != null) {
            return;
        }
        if (getApplicationInfo().targetSdkVersion >= 21) {
            IllegalArgumentException ex = new IllegalArgumentException("Service Intent must be explicit: " + service);
            throw ex;
        }
        Log.w(TAG, "Implicit intents with startService are not safe: " + service + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + Debug.getCallers(2, 3));
    }

    @Override
    public ComponentName startService(Intent service) {
        warnIfCallingFromSystemProcess();
        return startServiceCommon(service, this.mUser);
    }

    @Override
    public boolean stopService(Intent service) {
        warnIfCallingFromSystemProcess();
        return stopServiceCommon(service, this.mUser);
    }

    @Override
    public ComponentName startServiceAsUser(Intent service, UserHandle user) {
        return startServiceCommon(service, user);
    }

    private ComponentName startServiceCommon(Intent service, UserHandle user) {
        try {
            validateServiceIntent(service);
            service.prepareToLeaveProcess(this);
            ComponentName cn = ActivityManagerNative.getDefault().startService(this.mMainThread.getApplicationThread(), service, service.resolveTypeIfNeeded(getContentResolver()), getOpPackageName(), user.getIdentifier());
            if (cn != null) {
                if (cn.getPackageName().equals("!")) {
                    throw new SecurityException("Not allowed to start service " + service + " without permission " + cn.getClassName());
                }
                if (cn.getPackageName().equals("!!")) {
                    throw new SecurityException("Unable to start service " + service + ": " + cn.getClassName());
                }
            }
            return cn;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public boolean stopServiceAsUser(Intent service, UserHandle user) {
        return stopServiceCommon(service, user);
    }

    private boolean stopServiceCommon(Intent service, UserHandle user) {
        try {
            validateServiceIntent(service);
            service.prepareToLeaveProcess(this);
            int res = ActivityManagerNative.getDefault().stopService(this.mMainThread.getApplicationThread(), service, service.resolveTypeIfNeeded(getContentResolver()), user.getIdentifier());
            if (res < 0) {
                throw new SecurityException("Not allowed to stop service " + service);
            }
            return res != 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        warnIfCallingFromSystemProcess();
        return bindServiceCommon(service, conn, flags, this.mMainThread.getHandler(), Process.myUserHandle());
    }

    @Override
    public boolean bindServiceAsUser(Intent service, ServiceConnection conn, int flags, UserHandle user) {
        return bindServiceCommon(service, conn, flags, this.mMainThread.getHandler(), user);
    }

    @Override
    public boolean bindServiceAsUser(Intent service, ServiceConnection conn, int flags, Handler handler, UserHandle user) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null.");
        }
        return bindServiceCommon(service, conn, flags, handler, user);
    }

    private boolean bindServiceCommon(Intent service, ServiceConnection conn, int flags, Handler handler, UserHandle user) {
        if (conn == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (this.mPackageInfo != null) {
            IServiceConnection sd = this.mPackageInfo.getServiceDispatcher(conn, getOuterContext(), handler, flags);
            validateServiceIntent(service);
            try {
                IBinder token = getActivityToken();
                if (token == null && (flags & 1) == 0 && this.mPackageInfo != null && this.mPackageInfo.getApplicationInfo().targetSdkVersion < 14) {
                    flags |= 32;
                }
                service.prepareToLeaveProcess(this);
                int res = ActivityManagerNative.getDefault().bindService(this.mMainThread.getApplicationThread(), getActivityToken(), service, service.resolveTypeIfNeeded(getContentResolver()), sd, flags, getOpPackageName(), user.getIdentifier());
                if (res < 0) {
                    throw new SecurityException("Not allowed to bind to service " + service);
                }
                return res != 0;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        if (conn == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (this.mPackageInfo != null) {
            IServiceConnection sd = this.mPackageInfo.forgetServiceDispatcher(getOuterContext(), conn);
            try {
                ActivityManagerNative.getDefault().unbindService(sd);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Not supported in system context");
    }

    @Override
    public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
        if (arguments != null) {
            try {
                arguments.setAllowFds(false);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return ActivityManagerNative.getDefault().startInstrumentation(className, profileFile, 0, arguments, null, null, getUserId(), null);
    }

    @Override
    public Object getSystemService(String name) {
        return SystemServiceRegistry.getSystemService(this, name);
    }

    @Override
    public String getSystemServiceName(Class<?> serviceClass) {
        return SystemServiceRegistry.getSystemServiceName(serviceClass);
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        try {
            return ActivityManagerNative.getDefault().checkPermission(permission, pid, uid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public int checkPermission(String permission, int pid, int uid, IBinder callerToken) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        try {
            return ActivityManagerNative.getDefault().checkPermissionWithToken(permission, pid, uid, callerToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public int checkCallingPermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        int pid = Binder.getCallingPid();
        if (pid != Process.myPid()) {
            return checkPermission(permission, pid, Binder.getCallingUid());
        }
        return -1;
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        return checkPermission(permission, Binder.getCallingPid(), Binder.getCallingUid());
    }

    @Override
    public int checkSelfPermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }
        return checkPermission(permission, Process.myPid(), Process.myUid());
    }

    private void enforce(String permission, int resultOfCheck, boolean selfToo, int uid, String message) {
        if (resultOfCheck != 0) {
            throw new SecurityException((message != null ? message + ": " : ProxyInfo.LOCAL_EXCL_LIST) + (selfToo ? "Neither user " + uid + " nor current process has " : "uid " + uid + " does not have ") + permission + ".");
        }
    }

    @Override
    public void enforcePermission(String permission, int pid, int uid, String message) {
        enforce(permission, checkPermission(permission, pid, uid), false, uid, message);
    }

    @Override
    public void enforceCallingPermission(String permission, String message) {
        enforce(permission, checkCallingPermission(permission), false, Binder.getCallingUid(), message);
    }

    @Override
    public void enforceCallingOrSelfPermission(String permission, String message) {
        enforce(permission, checkCallingOrSelfPermission(permission), true, Binder.getCallingUid(), message);
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
        try {
            ActivityManagerNative.getDefault().grantUriPermission(this.mMainThread.getApplicationThread(), toPackage, ContentProvider.getUriWithoutUserId(uri), modeFlags, resolveUserId(uri));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
        try {
            ActivityManagerNative.getDefault().revokeUriPermission(this.mMainThread.getApplicationThread(), ContentProvider.getUriWithoutUserId(uri), modeFlags, resolveUserId(uri));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        try {
            return ActivityManagerNative.getDefault().checkUriPermission(ContentProvider.getUriWithoutUserId(uri), pid, uid, modeFlags, resolveUserId(uri), null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags, IBinder callerToken) {
        try {
            return ActivityManagerNative.getDefault().checkUriPermission(ContentProvider.getUriWithoutUserId(uri), pid, uid, modeFlags, resolveUserId(uri), callerToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private int resolveUserId(Uri uri) {
        return ContentProvider.getUserIdFromUri(uri, getUserId());
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        int pid = Binder.getCallingPid();
        if (pid != Process.myPid()) {
            return checkUriPermission(uri, pid, Binder.getCallingUid(), modeFlags);
        }
        return -1;
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        return checkUriPermission(uri, Binder.getCallingPid(), Binder.getCallingUid(), modeFlags);
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
        if ((modeFlags & 1) != 0 && (readPermission == null || checkPermission(readPermission, pid, uid) == 0)) {
            return 0;
        }
        if ((modeFlags & 2) != 0 && (writePermission == null || checkPermission(writePermission, pid, uid) == 0)) {
            return 0;
        }
        if (uri != null) {
            return checkUriPermission(uri, pid, uid, modeFlags);
        }
        return -1;
    }

    private String uriModeFlagToString(int uriModeFlags) {
        StringBuilder builder = new StringBuilder();
        if ((uriModeFlags & 1) != 0) {
            builder.append("read and ");
        }
        if ((uriModeFlags & 2) != 0) {
            builder.append("write and ");
        }
        if ((uriModeFlags & 64) != 0) {
            builder.append("persistable and ");
        }
        if ((uriModeFlags & 128) != 0) {
            builder.append("prefix and ");
        }
        if (builder.length() > 5) {
            builder.setLength(builder.length() - 5);
            return builder.toString();
        }
        throw new IllegalArgumentException("Unknown permission mode flags: " + uriModeFlags);
    }

    private void enforceForUri(int modeFlags, int resultOfCheck, boolean selfToo, int uid, Uri uri, String message) {
        if (resultOfCheck != 0) {
            throw new SecurityException((message != null ? message + ": " : ProxyInfo.LOCAL_EXCL_LIST) + (selfToo ? "Neither user " + uid + " nor current process has " : "User " + uid + " does not have ") + uriModeFlagToString(modeFlags) + " permission on " + uri + ".");
        }
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
        enforceForUri(modeFlags, checkUriPermission(uri, pid, uid, modeFlags), false, uid, uri, message);
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
        enforceForUri(modeFlags, checkCallingUriPermission(uri, modeFlags), false, Binder.getCallingUid(), uri, message);
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
        enforceForUri(modeFlags, checkCallingOrSelfUriPermission(uri, modeFlags), true, Binder.getCallingUid(), uri, message);
    }

    @Override
    public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
        enforceForUri(modeFlags, checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags), false, uid, uri, message);
    }

    private void warnIfCallingFromSystemProcess() {
        if (Process.myUid() != 1000) {
            return;
        }
        Slog.w(TAG, "Calling a method in the system process without a qualified user: " + Debug.getCallers(5));
    }

    @Override
    public Context createApplicationContext(ApplicationInfo application, int flags) throws PackageManager.NameNotFoundException {
        LoadedApk pi = this.mMainThread.getPackageInfo(application, this.mResources.getCompatibilityInfo(), 1073741824 | flags);
        if (pi != null) {
            ContextImpl c = new ContextImpl(this, this.mMainThread, pi, this.mActivityToken, new UserHandle(UserHandle.getUserId(application.uid)), flags, this.mDisplay, null, -1);
            if (c.mResources != null) {
                return c;
            }
        }
        throw new PackageManager.NameNotFoundException("Application package " + application.packageName + " not found");
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        return createPackageContextAsUser(packageName, flags, this.mUser != null ? this.mUser : Process.myUserHandle());
    }

    @Override
    public Context createPackageContextAsUser(String packageName, int flags, UserHandle user) throws PackageManager.NameNotFoundException {
        if (packageName.equals("system") || packageName.equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
            return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mActivityToken, user, flags, this.mDisplay, null, -1);
        }
        LoadedApk pi = this.mMainThread.getPackageInfo(packageName, this.mResources.getCompatibilityInfo(), 1073741824 | flags, user.getIdentifier());
        if (pi != null) {
            ContextImpl c = new ContextImpl(this, this.mMainThread, pi, this.mActivityToken, user, flags, this.mDisplay, null, -1);
            if (c.mResources != null) {
                return c;
            }
        }
        throw new PackageManager.NameNotFoundException("Application package " + packageName + " not found");
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        if (overrideConfiguration == null) {
            throw new IllegalArgumentException("overrideConfiguration must not be null");
        }
        return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mActivityToken, this.mUser, this.mFlags, this.mDisplay, overrideConfiguration, -1);
    }

    @Override
    public Context createDisplayContext(Display display) {
        if (display == null) {
            throw new IllegalArgumentException("display must not be null");
        }
        return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mActivityToken, this.mUser, this.mFlags, display, null, -1);
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        int flags = (this.mFlags & (-17)) | 8;
        return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mActivityToken, this.mUser, flags, this.mDisplay, null, -1);
    }

    @Override
    public Context createCredentialProtectedStorageContext() {
        int flags = (this.mFlags & (-9)) | 16;
        return new ContextImpl(this, this.mMainThread, this.mPackageInfo, this.mActivityToken, this.mUser, flags, this.mDisplay, null, -1);
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
    public Display getDisplay() {
        DisplayAdjustments displayAdjustments = this.mResources.getDisplayAdjustments();
        if (this.mDisplay == null) {
            return this.mResourcesManager.getAdjustedDisplay(0, displayAdjustments);
        }
        if (!this.mDisplay.getDisplayAdjustments().equals(displayAdjustments)) {
            this.mDisplay = this.mResourcesManager.getAdjustedDisplay(this.mDisplay.getDisplayId(), displayAdjustments);
        }
        return this.mDisplay;
    }

    @Override
    public DisplayAdjustments getDisplayAdjustments(int displayId) {
        return this.mResources.getDisplayAdjustments();
    }

    @Override
    public File getDataDir() {
        File res;
        if (this.mPackageInfo != null) {
            if (isCredentialProtectedStorage()) {
                res = this.mPackageInfo.getCredentialProtectedDataDirFile();
            } else if (isDeviceProtectedStorage()) {
                res = this.mPackageInfo.getDeviceProtectedDataDirFile();
            } else {
                res = this.mPackageInfo.getDataDirFile();
            }
            if (res != null) {
                if (!res.exists() && Process.myUid() == 1000) {
                    Log.e(TAG, "Data directory doesn't exist for package " + getPackageName(), new Throwable());
                }
                return res;
            }
            throw new RuntimeException("No data directory found for package " + getPackageName());
        }
        throw new RuntimeException("No package details found for package " + getPackageName());
    }

    @Override
    public File getDir(String name, int mode) {
        checkMode(mode);
        File file = makeFilename(getDataDir(), "app_" + name);
        if (!file.exists()) {
            file.mkdir();
            setFilePermissionsFromMode(file.getPath(), mode, IActivityManager.GET_STICKY_WINDOW_TRANSACTION);
        }
        return file;
    }

    @Override
    public int getUserId() {
        return this.mUser.getIdentifier();
    }

    static ContextImpl createSystemContext(ActivityThread mainThread) {
        LoadedApk packageInfo = new LoadedApk(mainThread);
        ContextImpl context = new ContextImpl(null, mainThread, packageInfo, null, null, 0, null, null, -1);
        context.mResources.updateConfiguration(context.mResourcesManager.getConfiguration(), context.mResourcesManager.getDisplayMetrics());
        return context;
    }

    static ContextImpl createAppContext(ActivityThread mainThread, LoadedApk packageInfo) {
        if (packageInfo == null) {
            throw new IllegalArgumentException("packageInfo");
        }
        return new ContextImpl(null, mainThread, packageInfo, null, null, 0, null, null, -1);
    }

    static ContextImpl createActivityContext(ActivityThread mainThread, LoadedApk packageInfo, IBinder activityToken, int displayId, Configuration overrideConfiguration) {
        if (packageInfo == null) {
            throw new IllegalArgumentException("packageInfo");
        }
        return new ContextImpl(null, mainThread, packageInfo, activityToken, null, 0, null, overrideConfiguration, displayId);
    }

    private ContextImpl(ContextImpl container, ActivityThread mainThread, LoadedApk packageInfo, IBinder activityToken, UserHandle user, int flags, Display display, Configuration overrideConfiguration, int createDisplayWithId) {
        int displayId;
        if ((flags & 24) == 0) {
            File dataDir = packageInfo.getDataDirFile();
            if (Objects.equals(dataDir, packageInfo.getCredentialProtectedDataDirFile())) {
                flags |= 16;
            } else if (Objects.equals(dataDir, packageInfo.getDeviceProtectedDataDirFile())) {
                flags |= 8;
            }
        }
        this.mMainThread = mainThread;
        this.mActivityToken = activityToken;
        this.mFlags = flags;
        user = user == null ? Process.myUserHandle() : user;
        this.mUser = user;
        this.mPackageInfo = packageInfo;
        this.mResourcesManager = ResourcesManager.getInstance();
        if (createDisplayWithId != -1) {
            displayId = createDisplayWithId;
        } else {
            displayId = display != null ? display.getDisplayId() : 0;
        }
        CompatibilityInfo compatInfo = container != null ? container.getDisplayAdjustments(displayId).getCompatibilityInfo() : null;
        if (compatInfo == null) {
            if (displayId == 0) {
                compatInfo = packageInfo.getCompatibilityInfo();
            } else {
                compatInfo = CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO;
            }
        }
        Resources resources = packageInfo.getResources(mainThread);
        if (resources != null && (displayId != 0 || overrideConfiguration != null || (compatInfo != null && compatInfo.applicationScale != resources.getCompatibilityInfo().applicationScale))) {
            resources = container != null ? this.mResourcesManager.getResources(activityToken, packageInfo.getResDir(), packageInfo.getSplitResDirs(), packageInfo.getOverlayDirs(), packageInfo.getApplicationInfo().sharedLibraryFiles, displayId, overrideConfiguration, compatInfo, packageInfo.getClassLoader()) : this.mResourcesManager.createBaseActivityResources(activityToken, packageInfo.getResDir(), packageInfo.getSplitResDirs(), packageInfo.getOverlayDirs(), packageInfo.getApplicationInfo().sharedLibraryFiles, displayId, overrideConfiguration, compatInfo, packageInfo.getClassLoader());
        }
        this.mResources = resources;
        this.mDisplay = createDisplayWithId != -1 ? this.mResourcesManager.getAdjustedDisplay(displayId, this.mResources.getDisplayAdjustments()) : display;
        if (container != null) {
            this.mBasePackageName = container.mBasePackageName;
            this.mOpPackageName = container.mOpPackageName;
        } else {
            this.mBasePackageName = packageInfo.mPackageName;
            ApplicationInfo ainfo = packageInfo.getApplicationInfo();
            if (ainfo.uid == 1000 && ainfo.uid != Process.myUid()) {
                this.mOpPackageName = ActivityThread.currentPackageName();
            } else {
                this.mOpPackageName = this.mBasePackageName;
            }
        }
        this.mContentResolver = new ApplicationContentResolver(this, mainThread, user);
    }

    void installSystemApplicationInfo(ApplicationInfo info, ClassLoader classLoader) {
        this.mPackageInfo.installSystemApplicationInfo(info, classLoader);
    }

    final void scheduleFinalCleanup(String who, String what) {
        this.mMainThread.scheduleContextCleanup(this, who, what);
    }

    final void performFinalCleanup(String who, String what) {
        this.mPackageInfo.removeContextRegistrations(getOuterContext(), who, what);
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

    final IBinder getActivityToken() {
        return this.mActivityToken;
    }

    private void checkMode(int mode) {
        if (getApplicationInfo().targetSdkVersion < 24) {
            return;
        }
        if ((mode & 1) != 0) {
            throw new SecurityException("MODE_WORLD_READABLE no longer supported");
        }
        if ((mode & 2) == 0) {
        } else {
            throw new SecurityException("MODE_WORLD_WRITEABLE no longer supported");
        }
    }

    static void setFilePermissionsFromMode(String name, int mode, int extraPermissions) {
        int perms = extraPermissions | 432;
        if ((mode & 1) != 0) {
            perms |= 4;
        }
        if ((mode & 2) != 0) {
            perms |= 2;
        }
        FileUtils.setPermissions(name, perms, -1, -1);
    }

    private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException("File " + name + " contains a path separator");
    }

    private File[] ensureExternalDirsExistOrFilter(File[] dirs) {
        File[] result = new File[dirs.length];
        for (int i = 0; i < dirs.length; i++) {
            File dir = dirs[i];
            if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
                IMountService mount = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
                try {
                    int res = mount.mkdirs(getPackageName(), dir.getAbsolutePath());
                    if (res != 0) {
                        Log.w(TAG, "Failed to ensure " + dir + ": " + res);
                        dir = null;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to ensure " + dir + ": " + e);
                    dir = null;
                }
            }
            result[i] = dir;
        }
        return result;
    }

    private static final class ApplicationContentResolver extends ContentResolver {
        public static final String QUERY_TAG = "ProviderLeakDetecter";
        private final ActivityThread mMainThread;
        private QueryHistory mQueryHistory;
        private final UserHandle mUser;

        public ApplicationContentResolver(Context context, ActivityThread mainThread, UserHandle user) {
            super(context);
            this.mQueryHistory = new QueryHistory(this, null);
            this.mMainThread = (ActivityThread) Preconditions.checkNotNull(mainThread);
            this.mUser = (UserHandle) Preconditions.checkNotNull(user);
        }

        @Override
        protected IContentProvider acquireProvider(Context context, String auth) {
            return this.mMainThread.acquireProvider(context, ContentProvider.getAuthorityWithoutUserId(auth), resolveUserIdFromAuthority(auth), true);
        }

        @Override
        protected IContentProvider acquireExistingProvider(Context context, String auth) {
            return this.mMainThread.acquireExistingProvider(context, ContentProvider.getAuthorityWithoutUserId(auth), resolveUserIdFromAuthority(auth), true);
        }

        @Override
        public boolean releaseProvider(IContentProvider provider) {
            return this.mMainThread.releaseProvider(provider, true);
        }

        @Override
        protected IContentProvider acquireUnstableProvider(Context c, String auth) {
            return this.mMainThread.acquireProvider(c, ContentProvider.getAuthorityWithoutUserId(auth), resolveUserIdFromAuthority(auth), false);
        }

        @Override
        public boolean releaseUnstableProvider(IContentProvider icp) {
            return this.mMainThread.releaseProvider(icp, false);
        }

        @Override
        public void unstableProviderDied(IContentProvider icp) {
            this.mMainThread.handleUnstableProviderDied(icp.asBinder(), true);
        }

        @Override
        public void appNotRespondingViaProvider(IContentProvider icp) {
            this.mMainThread.appNotRespondingViaProvider(icp.asBinder());
        }

        protected int resolveUserIdFromAuthority(String auth) {
            return ContentProvider.getUserIdFromAuthority(auth, this.mUser.getIdentifier());
        }

        private final class QueryHistoryRecord {
            public Throwable mStackTrace;
            public String mUri;

            QueryHistoryRecord(String uri, Throwable stackTrace) {
                this.mUri = uri;
                this.mStackTrace = stackTrace;
            }
        }

        private final class QueryHistory {
            public static final int BLUETOOTH_THRESHOLD = 250;
            private Map<Integer, QueryHistoryRecord> mCursorMap;
            private Map<Integer, QueryHistoryRecord> mPfdMap;
            private final boolean mProviderLeakDetect;
            private Map<String, Integer> mUriMap;
            private Map<String, Integer> mUriPfdMap;

            QueryHistory(ApplicationContentResolver this$1, QueryHistory queryHistory) {
                this();
            }

            private QueryHistory() {
                this.mProviderLeakDetect = Log.isLoggable(ApplicationContentResolver.QUERY_TAG, 2);
                this.mCursorMap = new HashMap();
                this.mUriMap = new HashMap();
                this.mPfdMap = new HashMap();
                this.mUriPfdMap = new HashMap();
            }

            private boolean checkAeeWarningList() throws Throwable {
                int uid = Process.myUid();
                FileInputStream fileInputStream = null;
                try {
                    FileInputStream fileInputStream2 = new FileInputStream("/data/system/resmon-uid.txt");
                    if (fileInputStream2 != null) {
                        try {
                            InputStreamReader inputReader = new InputStreamReader(fileInputStream2);
                            BufferedReader buffReader = new BufferedReader(inputReader);
                            for (String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
                                if (uid == Integer.valueOf(line).intValue()) {
                                    if (fileInputStream2 != null) {
                                        try {
                                            fileInputStream2.close();
                                        } catch (IOException e) {
                                        }
                                    }
                                    return true;
                                }
                            }
                        } catch (IOException e2) {
                            fileInputStream = fileInputStream2;
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e3) {
                                }
                            }
                            return false;
                        } catch (Throwable th) {
                            th = th;
                            fileInputStream = fileInputStream2;
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e4) {
                                }
                            }
                            throw th;
                        }
                    }
                    if (fileInputStream2 != null) {
                        try {
                            fileInputStream2.close();
                        } catch (IOException e5) {
                        }
                    }
                    return false;
                } catch (IOException e6) {
                } catch (Throwable th2) {
                    th = th2;
                }
            }

            public boolean add(String uri, Throwable stackTrace, int cursorHashCode) {
                boolean reportException = false;
                synchronized (this.mCursorMap) {
                    if (this.mUriMap.get(uri) == null) {
                        this.mUriMap.put(uri, 1);
                    } else {
                        this.mUriMap.put(uri, Integer.valueOf(this.mUriMap.get(uri).intValue() + 1));
                    }
                    if (this.mProviderLeakDetect && this.mUriMap.get(uri).intValue() >= 5) {
                        Log.e(ApplicationContentResolver.QUERY_TAG, "PossibleCursorLeak:" + uri + ",QueryCounter:" + this.mUriMap.get(uri), stackTrace);
                    }
                    if (this.mCursorMap.get(Integer.valueOf(cursorHashCode)) == null) {
                        QueryHistoryRecord qhr = ApplicationContentResolver.this.new QueryHistoryRecord(uri, stackTrace);
                        this.mCursorMap.put(Integer.valueOf(cursorHashCode), qhr);
                    }
                    if (this.mProviderLeakDetect) {
                        Log.v(ApplicationContentResolver.QUERY_TAG, "Cursor Open:" + cursorHashCode + " Total Opened Cursor Count:" + this.mCursorMap.size() + ".");
                    }
                    if (this.mCursorMap.size() == 70 || this.mCursorMap.size() == 80) {
                        Log.v(ApplicationContentResolver.QUERY_TAG, "Total Opened Cursor Count:" + this.mCursorMap.size() + ".");
                        dump();
                        reportException = true;
                    }
                }
                if (reportException && checkAeeWarningList()) {
                    ExceptionLog exceptionLog = null;
                    try {
                        if (SystemProperties.get("ro.have_aee_feature").equals(WifiEnterpriseConfig.ENGINE_ENABLE)) {
                            ExceptionLog exceptionLog2 = new ExceptionLog();
                            exceptionLog = exceptionLog2;
                        }
                        if (exceptionLog != null) {
                            exceptionLog.systemreport((byte) 0, "CursorLeakDetecter", "Total Opened Cursor Count:" + this.mCursorMap.size() + ".", "/data/cursorleak/traces.txt");
                        }
                    } catch (Exception e) {
                    }
                }
                return true;
            }

            public void remove(int cursorHashCode) {
                synchronized (this.mCursorMap) {
                    QueryHistoryRecord qhr = this.mCursorMap.get(Integer.valueOf(cursorHashCode));
                    if (qhr == null || this.mUriMap.get(qhr.mUri) == null) {
                        Log.e(ApplicationContentResolver.QUERY_TAG, "bad request for cursor:" + cursorHashCode + ".");
                        return;
                    }
                    if (this.mUriMap.get(qhr.mUri).intValue() == 1) {
                        this.mUriMap.remove(qhr.mUri);
                        this.mCursorMap.remove(Integer.valueOf(cursorHashCode));
                    } else if (this.mUriMap.get(qhr.mUri).intValue() > 1) {
                        this.mUriMap.put(qhr.mUri, Integer.valueOf(this.mUriMap.get(qhr.mUri).intValue() - 1));
                        this.mCursorMap.remove(Integer.valueOf(cursorHashCode));
                    }
                    if (this.mProviderLeakDetect) {
                        Log.v(ApplicationContentResolver.QUERY_TAG, "Cursor Close:" + cursorHashCode + " Total Opened Cursor Count:" + this.mCursorMap.size() + ".");
                    }
                }
            }

            public boolean addPfd(String uri, Throwable stackTrace, int hashCode) {
                boolean reportException = false;
                synchronized (this.mPfdMap) {
                    if (this.mUriPfdMap.get(uri) == null) {
                        this.mUriPfdMap.put(uri, 1);
                    } else {
                        this.mUriPfdMap.put(uri, Integer.valueOf(this.mUriPfdMap.get(uri).intValue() + 1));
                    }
                    if (this.mProviderLeakDetect && this.mUriPfdMap.get(uri).intValue() >= 5) {
                        Log.e(ApplicationContentResolver.QUERY_TAG, "Possible PFD Leak:" + uri + ",QueryCounter:" + this.mUriPfdMap.get(uri), stackTrace);
                    }
                    if (this.mPfdMap.get(Integer.valueOf(hashCode)) == null) {
                        QueryHistoryRecord qhr = ApplicationContentResolver.this.new QueryHistoryRecord(uri, stackTrace);
                        this.mPfdMap.put(Integer.valueOf(hashCode), qhr);
                    }
                    if (this.mProviderLeakDetect) {
                        Log.v(ApplicationContentResolver.QUERY_TAG, "PFD Open:" + hashCode + " Total Opened PFD Count:" + this.mPfdMap.size() + ".");
                    }
                    if (this.mPfdMap.size() == 250) {
                        Log.v(ApplicationContentResolver.QUERY_TAG, "Total Opened PFD Count:" + this.mPfdMap.size() + ".");
                        dump();
                        reportException = true;
                    }
                }
                if (reportException && checkAeeWarningList()) {
                    ExceptionLog exceptionLog = null;
                    try {
                        if (SystemProperties.get("ro.have_aee_feature").equals(WifiEnterpriseConfig.ENGINE_ENABLE)) {
                            ExceptionLog exceptionLog2 = new ExceptionLog();
                            exceptionLog = exceptionLog2;
                        }
                        if (exceptionLog != null) {
                            exceptionLog.systemreport((byte) 0, "PFDLeakDetecter", "Total Opened PFD Count:" + this.mPfdMap.size() + ".", "/data/cursorleak/traces.txt");
                        }
                    } catch (Exception e) {
                    }
                }
                return true;
            }

            public void removePfd(int hashCode) {
                synchronized (this.mPfdMap) {
                    QueryHistoryRecord qhr = this.mPfdMap.get(Integer.valueOf(hashCode));
                    if (qhr == null || this.mUriPfdMap.get(qhr.mUri) == null) {
                        Log.e(ApplicationContentResolver.QUERY_TAG, "bad request for pfd:" + hashCode + ".");
                        return;
                    }
                    if (this.mUriPfdMap.get(qhr.mUri).intValue() == 1) {
                        this.mUriPfdMap.remove(qhr.mUri);
                        this.mPfdMap.remove(Integer.valueOf(hashCode));
                    } else if (this.mUriPfdMap.get(qhr.mUri).intValue() > 1) {
                        this.mUriPfdMap.put(qhr.mUri, Integer.valueOf(this.mUriPfdMap.get(qhr.mUri).intValue() - 1));
                        this.mPfdMap.remove(Integer.valueOf(hashCode));
                    }
                    if (this.mProviderLeakDetect) {
                        Log.v(ApplicationContentResolver.QUERY_TAG, "PFD Close:" + hashCode + " Total Opened PFD Count:" + this.mCursorMap.size() + ".");
                    }
                }
            }

            public void dump() {
                Log.v(ApplicationContentResolver.QUERY_TAG, "Total Opened Cursor Count:" + this.mCursorMap.size() + ".");
                Iterator<Map.Entry<Integer, QueryHistoryRecord>> it = this.mCursorMap.entrySet().iterator();
                while (it.hasNext()) {
                    QueryHistoryRecord qhr = it.next().getValue();
                    Log.v(ApplicationContentResolver.QUERY_TAG, "CursorQueryHistory:" + qhr.mUri, qhr.mStackTrace);
                }
                Log.v(ApplicationContentResolver.QUERY_TAG, "Total Opened PFD Count:" + this.mPfdMap.size() + ".");
                Iterator<Map.Entry<Integer, QueryHistoryRecord>> it2 = this.mPfdMap.entrySet().iterator();
                while (it2.hasNext()) {
                    QueryHistoryRecord qhr2 = it2.next().getValue();
                    Log.v(ApplicationContentResolver.QUERY_TAG, "PFDQueryHistory:" + qhr2.mUri, qhr2.mStackTrace);
                }
            }
        }

        @Override
        public boolean addToQueryHistory(String uri, Throwable stackTrace, int hashCode, int type) {
            try {
            } catch (Exception e) {
                Log.e(QUERY_TAG, "AddToQueryHistory", e);
            }
            if (type == 1) {
                return this.mQueryHistory.add(uri, stackTrace, hashCode);
            }
            if (type == 2) {
                return this.mQueryHistory.addPfd(uri, stackTrace, hashCode);
            }
            return true;
        }

        @Override
        public void removeFromQueryHistory(int hashCode, int type) {
            try {
                if (type == 1) {
                    this.mQueryHistory.remove(hashCode);
                } else if (type != 2) {
                } else {
                    this.mQueryHistory.removePfd(hashCode);
                }
            } catch (Exception e) {
                Log.e(QUERY_TAG, "RemoveFromQueryHistory", e);
            }
        }

        public void dumpQueryHistory() {
            this.mQueryHistory.dump();
        }
    }
}
