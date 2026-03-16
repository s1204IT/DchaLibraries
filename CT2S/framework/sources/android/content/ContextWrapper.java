package android.content;

import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.view.Display;
import android.view.DisplayAdjustments;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ContextWrapper extends Context {
    Context mBase;

    public ContextWrapper(Context base) {
        this.mBase = base;
    }

    protected void attachBaseContext(Context base) {
        if (this.mBase != null) {
            throw new IllegalStateException("Base context already set");
        }
        this.mBase = base;
    }

    public Context getBaseContext() {
        return this.mBase;
    }

    @Override
    public AssetManager getAssets() {
        return this.mBase.getAssets();
    }

    @Override
    public Resources getResources() {
        return this.mBase.getResources();
    }

    @Override
    public PackageManager getPackageManager() {
        return this.mBase.getPackageManager();
    }

    @Override
    public ContentResolver getContentResolver() {
        return this.mBase.getContentResolver();
    }

    @Override
    public Looper getMainLooper() {
        return this.mBase.getMainLooper();
    }

    @Override
    public Context getApplicationContext() {
        return this.mBase.getApplicationContext();
    }

    @Override
    public void setTheme(int resid) {
        this.mBase.setTheme(resid);
    }

    @Override
    public int getThemeResId() {
        return this.mBase.getThemeResId();
    }

    @Override
    public Resources.Theme getTheme() {
        return this.mBase.getTheme();
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.mBase.getClassLoader();
    }

    @Override
    public String getPackageName() {
        return this.mBase.getPackageName();
    }

    @Override
    public String getBasePackageName() {
        return this.mBase.getBasePackageName();
    }

    @Override
    public String getOpPackageName() {
        return this.mBase.getOpPackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return this.mBase.getApplicationInfo();
    }

    @Override
    public String getPackageResourcePath() {
        return this.mBase.getPackageResourcePath();
    }

    @Override
    public String getPackageCodePath() {
        return this.mBase.getPackageCodePath();
    }

    @Override
    public File getSharedPrefsFile(String name) {
        return this.mBase.getSharedPrefsFile(name);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return this.mBase.getSharedPreferences(name, mode);
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return this.mBase.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return this.mBase.openFileOutput(name, mode);
    }

    @Override
    public boolean deleteFile(String name) {
        return this.mBase.deleteFile(name);
    }

    @Override
    public File getFileStreamPath(String name) {
        return this.mBase.getFileStreamPath(name);
    }

    @Override
    public String[] fileList() {
        return this.mBase.fileList();
    }

    @Override
    public File getFilesDir() {
        return this.mBase.getFilesDir();
    }

    @Override
    public File getNoBackupFilesDir() {
        return this.mBase.getNoBackupFilesDir();
    }

    @Override
    public File getExternalFilesDir(String type) {
        return this.mBase.getExternalFilesDir(type);
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        return this.mBase.getExternalFilesDirs(type);
    }

    @Override
    public File getObbDir() {
        return this.mBase.getObbDir();
    }

    @Override
    public File[] getObbDirs() {
        return this.mBase.getObbDirs();
    }

    @Override
    public File getCacheDir() {
        return this.mBase.getCacheDir();
    }

    @Override
    public File getCodeCacheDir() {
        return this.mBase.getCodeCacheDir();
    }

    @Override
    public File getExternalCacheDir() {
        return this.mBase.getExternalCacheDir();
    }

    @Override
    public File[] getExternalCacheDirs() {
        return this.mBase.getExternalCacheDirs();
    }

    @Override
    public File[] getExternalMediaDirs() {
        return this.mBase.getExternalMediaDirs();
    }

    @Override
    public File getDir(String name, int mode) {
        return this.mBase.getDir(name, mode);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return this.mBase.openOrCreateDatabase(name, mode, factory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return this.mBase.openOrCreateDatabase(name, mode, factory, errorHandler);
    }

    @Override
    public boolean deleteDatabase(String name) {
        return this.mBase.deleteDatabase(name);
    }

    @Override
    public File getDatabasePath(String name) {
        return this.mBase.getDatabasePath(name);
    }

    @Override
    public String[] databaseList() {
        return this.mBase.databaseList();
    }

    @Override
    public Drawable getWallpaper() {
        return this.mBase.getWallpaper();
    }

    @Override
    public Drawable peekWallpaper() {
        return this.mBase.peekWallpaper();
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        return this.mBase.getWallpaperDesiredMinimumWidth();
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        return this.mBase.getWallpaperDesiredMinimumHeight();
    }

    @Override
    public void setWallpaper(Bitmap bitmap) throws IOException {
        this.mBase.setWallpaper(bitmap);
    }

    @Override
    public void setWallpaper(InputStream data) throws IOException {
        this.mBase.setWallpaper(data);
    }

    @Override
    public void clearWallpaper() throws IOException {
        this.mBase.clearWallpaper();
    }

    @Override
    public void startActivity(Intent intent) {
        this.mBase.startActivity(intent);
    }

    @Override
    public void startActivityAsUser(Intent intent, UserHandle user) {
        this.mBase.startActivityAsUser(intent, user);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        this.mBase.startActivity(intent, options);
    }

    @Override
    public void startActivityAsUser(Intent intent, Bundle options, UserHandle user) {
        this.mBase.startActivityAsUser(intent, options, user);
    }

    @Override
    public void startActivities(Intent[] intents) {
        this.mBase.startActivities(intents);
    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {
        this.mBase.startActivities(intents, options);
    }

    @Override
    public void startActivitiesAsUser(Intent[] intents, Bundle options, UserHandle userHandle) {
        this.mBase.startActivitiesAsUser(intents, options, userHandle);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        this.mBase.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        this.mBase.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        this.mBase.sendBroadcast(intent);
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        this.mBase.sendBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission, int appOp) {
        this.mBase.sendBroadcast(intent, receiverPermission, appOp);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        this.mBase.sendOrderedBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        this.mBase.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, int appOp, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        this.mBase.sendOrderedBroadcast(intent, receiverPermission, appOp, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
        this.mBase.sendBroadcastAsUser(intent, user);
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
        this.mBase.sendBroadcastAsUser(intent, user, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        this.mBase.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, int appOp, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        this.mBase.sendOrderedBroadcastAsUser(intent, user, receiverPermission, appOp, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        this.mBase.sendStickyBroadcast(intent);
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        this.mBase.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        this.mBase.removeStickyBroadcast(intent);
    }

    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
        this.mBase.sendStickyBroadcastAsUser(intent, user);
    }

    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        this.mBase.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }

    @Override
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
        this.mBase.removeStickyBroadcastAsUser(intent, user);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return this.mBase.registerReceiver(receiver, filter);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return this.mBase.registerReceiver(receiver, filter, broadcastPermission, scheduler);
    }

    @Override
    public Intent registerReceiverAsUser(BroadcastReceiver receiver, UserHandle user, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return this.mBase.registerReceiverAsUser(receiver, user, filter, broadcastPermission, scheduler);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        this.mBase.unregisterReceiver(receiver);
    }

    @Override
    public ComponentName startService(Intent service) {
        return this.mBase.startService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        return this.mBase.stopService(name);
    }

    @Override
    public ComponentName startServiceAsUser(Intent service, UserHandle user) {
        return this.mBase.startServiceAsUser(service, user);
    }

    @Override
    public boolean stopServiceAsUser(Intent name, UserHandle user) {
        return this.mBase.stopServiceAsUser(name, user);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return this.mBase.bindService(service, conn, flags);
    }

    @Override
    public boolean bindServiceAsUser(Intent service, ServiceConnection conn, int flags, UserHandle user) {
        return this.mBase.bindServiceAsUser(service, conn, flags, user);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        this.mBase.unbindService(conn);
    }

    @Override
    public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
        return this.mBase.startInstrumentation(className, profileFile, arguments);
    }

    @Override
    public Object getSystemService(String name) {
        return this.mBase.getSystemService(name);
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return this.mBase.checkPermission(permission, pid, uid);
    }

    @Override
    public int checkPermission(String permission, int pid, int uid, IBinder callerToken) {
        return this.mBase.checkPermission(permission, pid, uid, callerToken);
    }

    @Override
    public int checkCallingPermission(String permission) {
        return this.mBase.checkCallingPermission(permission);
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        return this.mBase.checkCallingOrSelfPermission(permission);
    }

    @Override
    public void enforcePermission(String permission, int pid, int uid, String message) {
        this.mBase.enforcePermission(permission, pid, uid, message);
    }

    @Override
    public void enforceCallingPermission(String permission, String message) {
        this.mBase.enforceCallingPermission(permission, message);
    }

    @Override
    public void enforceCallingOrSelfPermission(String permission, String message) {
        this.mBase.enforceCallingOrSelfPermission(permission, message);
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
        this.mBase.grantUriPermission(toPackage, uri, modeFlags);
    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
        this.mBase.revokeUriPermission(uri, modeFlags);
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        return this.mBase.checkUriPermission(uri, pid, uid, modeFlags);
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags, IBinder callerToken) {
        return this.mBase.checkUriPermission(uri, pid, uid, modeFlags, callerToken);
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        return this.mBase.checkCallingUriPermission(uri, modeFlags);
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        return this.mBase.checkCallingOrSelfUriPermission(uri, modeFlags);
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
        return this.mBase.checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags);
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
        this.mBase.enforceUriPermission(uri, pid, uid, modeFlags, message);
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
        this.mBase.enforceCallingUriPermission(uri, modeFlags, message);
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
        this.mBase.enforceCallingOrSelfUriPermission(uri, modeFlags, message);
    }

    @Override
    public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
        this.mBase.enforceUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags, message);
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        return this.mBase.createPackageContext(packageName, flags);
    }

    @Override
    public Context createPackageContextAsUser(String packageName, int flags, UserHandle user) throws PackageManager.NameNotFoundException {
        return this.mBase.createPackageContextAsUser(packageName, flags, user);
    }

    @Override
    public Context createApplicationContext(ApplicationInfo application, int flags) throws PackageManager.NameNotFoundException {
        return this.mBase.createApplicationContext(application, flags);
    }

    @Override
    public int getUserId() {
        return this.mBase.getUserId();
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return this.mBase.createConfigurationContext(overrideConfiguration);
    }

    @Override
    public Context createDisplayContext(Display display) {
        return this.mBase.createDisplayContext(display);
    }

    @Override
    public boolean isRestricted() {
        return this.mBase.isRestricted();
    }

    @Override
    public DisplayAdjustments getDisplayAdjustments(int displayId) {
        return this.mBase.getDisplayAdjustments(displayId);
    }
}
