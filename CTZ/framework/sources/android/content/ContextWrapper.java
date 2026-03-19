package android.content;

import android.annotation.SystemApi;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
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
import android.view.autofill.AutofillManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

public class ContextWrapper extends Context {
    Context mBase;

    public ContextWrapper(Context context) {
        this.mBase = context;
    }

    protected void attachBaseContext(Context context) {
        if (this.mBase != null) {
            throw new IllegalStateException("Base context already set");
        }
        this.mBase = context;
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
    public Executor getMainExecutor() {
        return this.mBase.getMainExecutor();
    }

    @Override
    public Context getApplicationContext() {
        return this.mBase.getApplicationContext();
    }

    @Override
    public void setTheme(int i) {
        this.mBase.setTheme(i);
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
    public SharedPreferences getSharedPreferences(String str, int i) {
        return this.mBase.getSharedPreferences(str, i);
    }

    @Override
    public SharedPreferences getSharedPreferences(File file, int i) {
        return this.mBase.getSharedPreferences(file, i);
    }

    @Override
    public void reloadSharedPreferences() {
        this.mBase.reloadSharedPreferences();
    }

    @Override
    public boolean moveSharedPreferencesFrom(Context context, String str) {
        return this.mBase.moveSharedPreferencesFrom(context, str);
    }

    @Override
    public boolean deleteSharedPreferences(String str) {
        return this.mBase.deleteSharedPreferences(str);
    }

    @Override
    public FileInputStream openFileInput(String str) throws FileNotFoundException {
        return this.mBase.openFileInput(str);
    }

    @Override
    public FileOutputStream openFileOutput(String str, int i) throws FileNotFoundException {
        return this.mBase.openFileOutput(str, i);
    }

    @Override
    public boolean deleteFile(String str) {
        return this.mBase.deleteFile(str);
    }

    @Override
    public File getFileStreamPath(String str) {
        return this.mBase.getFileStreamPath(str);
    }

    @Override
    public File getSharedPreferencesPath(String str) {
        return this.mBase.getSharedPreferencesPath(str);
    }

    @Override
    public String[] fileList() {
        return this.mBase.fileList();
    }

    @Override
    public File getDataDir() {
        return this.mBase.getDataDir();
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
    public File getExternalFilesDir(String str) {
        return this.mBase.getExternalFilesDir(str);
    }

    @Override
    public File[] getExternalFilesDirs(String str) {
        return this.mBase.getExternalFilesDirs(str);
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
    public File getDir(String str, int i) {
        return this.mBase.getDir(str, i);
    }

    @Override
    public File getPreloadsFileCache() {
        return this.mBase.getPreloadsFileCache();
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String str, int i, SQLiteDatabase.CursorFactory cursorFactory) {
        return this.mBase.openOrCreateDatabase(str, i, cursorFactory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String str, int i, SQLiteDatabase.CursorFactory cursorFactory, DatabaseErrorHandler databaseErrorHandler) {
        return this.mBase.openOrCreateDatabase(str, i, cursorFactory, databaseErrorHandler);
    }

    @Override
    public boolean moveDatabaseFrom(Context context, String str) {
        return this.mBase.moveDatabaseFrom(context, str);
    }

    @Override
    public boolean deleteDatabase(String str) {
        return this.mBase.deleteDatabase(str);
    }

    @Override
    public File getDatabasePath(String str) {
        return this.mBase.getDatabasePath(str);
    }

    @Override
    public String[] databaseList() {
        return this.mBase.databaseList();
    }

    @Override
    @Deprecated
    public Drawable getWallpaper() {
        return this.mBase.getWallpaper();
    }

    @Override
    @Deprecated
    public Drawable peekWallpaper() {
        return this.mBase.peekWallpaper();
    }

    @Override
    @Deprecated
    public int getWallpaperDesiredMinimumWidth() {
        return this.mBase.getWallpaperDesiredMinimumWidth();
    }

    @Override
    @Deprecated
    public int getWallpaperDesiredMinimumHeight() {
        return this.mBase.getWallpaperDesiredMinimumHeight();
    }

    @Override
    @Deprecated
    public void setWallpaper(Bitmap bitmap) throws IOException {
        this.mBase.setWallpaper(bitmap);
    }

    @Override
    @Deprecated
    public void setWallpaper(InputStream inputStream) throws IOException {
        this.mBase.setWallpaper(inputStream);
    }

    @Override
    @Deprecated
    public void clearWallpaper() throws IOException {
        this.mBase.clearWallpaper();
    }

    @Override
    public void startActivity(Intent intent) {
        this.mBase.startActivity(intent);
    }

    @Override
    public void startActivityAsUser(Intent intent, UserHandle userHandle) {
        this.mBase.startActivityAsUser(intent, userHandle);
    }

    @Override
    public void startActivityForResult(String str, Intent intent, int i, Bundle bundle) {
        this.mBase.startActivityForResult(str, intent, i, bundle);
    }

    @Override
    public boolean canStartActivityForResult() {
        return this.mBase.canStartActivityForResult();
    }

    @Override
    public void startActivity(Intent intent, Bundle bundle) {
        this.mBase.startActivity(intent, bundle);
    }

    @Override
    public void startActivityAsUser(Intent intent, Bundle bundle, UserHandle userHandle) {
        this.mBase.startActivityAsUser(intent, bundle, userHandle);
    }

    @Override
    public void startActivities(Intent[] intentArr) {
        this.mBase.startActivities(intentArr);
    }

    @Override
    public void startActivities(Intent[] intentArr, Bundle bundle) {
        this.mBase.startActivities(intentArr, bundle);
    }

    @Override
    public int startActivitiesAsUser(Intent[] intentArr, Bundle bundle, UserHandle userHandle) {
        return this.mBase.startActivitiesAsUser(intentArr, bundle, userHandle);
    }

    @Override
    public void startIntentSender(IntentSender intentSender, Intent intent, int i, int i2, int i3) throws IntentSender.SendIntentException {
        this.mBase.startIntentSender(intentSender, intent, i, i2, i3);
    }

    @Override
    public void startIntentSender(IntentSender intentSender, Intent intent, int i, int i2, int i3, Bundle bundle) throws IntentSender.SendIntentException {
        this.mBase.startIntentSender(intentSender, intent, i, i2, i3, bundle);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        this.mBase.sendBroadcast(intent);
    }

    @Override
    public void sendBroadcast(Intent intent, String str) {
        this.mBase.sendBroadcast(intent, str);
    }

    @Override
    public void sendBroadcastMultiplePermissions(Intent intent, String[] strArr) {
        this.mBase.sendBroadcastMultiplePermissions(intent, strArr);
    }

    @Override
    public void sendBroadcastAsUserMultiplePermissions(Intent intent, UserHandle userHandle, String[] strArr) {
        this.mBase.sendBroadcastAsUserMultiplePermissions(intent, userHandle, strArr);
    }

    @Override
    @SystemApi
    public void sendBroadcast(Intent intent, String str, Bundle bundle) {
        this.mBase.sendBroadcast(intent, str, bundle);
    }

    @Override
    public void sendBroadcast(Intent intent, String str, int i) {
        this.mBase.sendBroadcast(intent, str, i);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String str) {
        this.mBase.sendOrderedBroadcast(intent, str);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String str, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str2, Bundle bundle) {
        this.mBase.sendOrderedBroadcast(intent, str, broadcastReceiver, handler, i, str2, bundle);
    }

    @Override
    @SystemApi
    public void sendOrderedBroadcast(Intent intent, String str, Bundle bundle, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str2, Bundle bundle2) {
        this.mBase.sendOrderedBroadcast(intent, str, bundle, broadcastReceiver, handler, i, str2, bundle2);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String str, int i, BroadcastReceiver broadcastReceiver, Handler handler, int i2, String str2, Bundle bundle) {
        this.mBase.sendOrderedBroadcast(intent, str, i, broadcastReceiver, handler, i2, str2, bundle);
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle) {
        this.mBase.sendBroadcastAsUser(intent, userHandle);
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle, String str) {
        this.mBase.sendBroadcastAsUser(intent, userHandle, str);
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle, String str, Bundle bundle) {
        this.mBase.sendBroadcastAsUser(intent, userHandle, str, bundle);
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle, String str, int i) {
        this.mBase.sendBroadcastAsUser(intent, userHandle, str, i);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, String str, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str2, Bundle bundle) {
        this.mBase.sendOrderedBroadcastAsUser(intent, userHandle, str, broadcastReceiver, handler, i, str2, bundle);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, String str, int i, BroadcastReceiver broadcastReceiver, Handler handler, int i2, String str2, Bundle bundle) {
        this.mBase.sendOrderedBroadcastAsUser(intent, userHandle, str, i, broadcastReceiver, handler, i2, str2, bundle);
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, String str, int i, Bundle bundle, BroadcastReceiver broadcastReceiver, Handler handler, int i2, String str2, Bundle bundle2) {
        this.mBase.sendOrderedBroadcastAsUser(intent, userHandle, str, i, bundle, broadcastReceiver, handler, i2, str2, bundle2);
    }

    @Override
    @Deprecated
    public void sendStickyBroadcast(Intent intent) {
        this.mBase.sendStickyBroadcast(intent);
    }

    @Override
    @Deprecated
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str, Bundle bundle) {
        this.mBase.sendStickyOrderedBroadcast(intent, broadcastReceiver, handler, i, str, bundle);
    }

    @Override
    @Deprecated
    public void removeStickyBroadcast(Intent intent) {
        this.mBase.removeStickyBroadcast(intent);
    }

    @Override
    @Deprecated
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle userHandle) {
        this.mBase.sendStickyBroadcastAsUser(intent, userHandle);
    }

    @Override
    @Deprecated
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle userHandle, Bundle bundle) {
        this.mBase.sendStickyBroadcastAsUser(intent, userHandle, bundle);
    }

    @Override
    @Deprecated
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, BroadcastReceiver broadcastReceiver, Handler handler, int i, String str, Bundle bundle) {
        this.mBase.sendStickyOrderedBroadcastAsUser(intent, userHandle, broadcastReceiver, handler, i, str, bundle);
    }

    @Override
    @Deprecated
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle userHandle) {
        this.mBase.removeStickyBroadcastAsUser(intent, userHandle);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter) {
        return this.mBase.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, int i) {
        return this.mBase.registerReceiver(broadcastReceiver, intentFilter, i);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, String str, Handler handler) {
        return this.mBase.registerReceiver(broadcastReceiver, intentFilter, str, handler);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, String str, Handler handler, int i) {
        return this.mBase.registerReceiver(broadcastReceiver, intentFilter, str, handler, i);
    }

    @Override
    public Intent registerReceiverAsUser(BroadcastReceiver broadcastReceiver, UserHandle userHandle, IntentFilter intentFilter, String str, Handler handler) {
        return this.mBase.registerReceiverAsUser(broadcastReceiver, userHandle, intentFilter, str, handler);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {
        this.mBase.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public ComponentName startService(Intent intent) {
        return this.mBase.startService(intent);
    }

    @Override
    public ComponentName startForegroundService(Intent intent) {
        return this.mBase.startForegroundService(intent);
    }

    @Override
    public boolean stopService(Intent intent) {
        return this.mBase.stopService(intent);
    }

    @Override
    public ComponentName startServiceAsUser(Intent intent, UserHandle userHandle) {
        return this.mBase.startServiceAsUser(intent, userHandle);
    }

    @Override
    public ComponentName startForegroundServiceAsUser(Intent intent, UserHandle userHandle) {
        return this.mBase.startForegroundServiceAsUser(intent, userHandle);
    }

    @Override
    public boolean stopServiceAsUser(Intent intent, UserHandle userHandle) {
        return this.mBase.stopServiceAsUser(intent, userHandle);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return this.mBase.bindService(intent, serviceConnection, i);
    }

    @Override
    public boolean bindServiceAsUser(Intent intent, ServiceConnection serviceConnection, int i, UserHandle userHandle) {
        return this.mBase.bindServiceAsUser(intent, serviceConnection, i, userHandle);
    }

    @Override
    public boolean bindServiceAsUser(Intent intent, ServiceConnection serviceConnection, int i, Handler handler, UserHandle userHandle) {
        return this.mBase.bindServiceAsUser(intent, serviceConnection, i, handler, userHandle);
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        this.mBase.unbindService(serviceConnection);
    }

    @Override
    public boolean startInstrumentation(ComponentName componentName, String str, Bundle bundle) {
        return this.mBase.startInstrumentation(componentName, str, bundle);
    }

    @Override
    public Object getSystemService(String str) {
        return this.mBase.getSystemService(str);
    }

    @Override
    public String getSystemServiceName(Class<?> cls) {
        return this.mBase.getSystemServiceName(cls);
    }

    @Override
    public int checkPermission(String str, int i, int i2) {
        return this.mBase.checkPermission(str, i, i2);
    }

    @Override
    public int checkPermission(String str, int i, int i2, IBinder iBinder) {
        return this.mBase.checkPermission(str, i, i2, iBinder);
    }

    @Override
    public int checkCallingPermission(String str) {
        return this.mBase.checkCallingPermission(str);
    }

    @Override
    public int checkCallingOrSelfPermission(String str) {
        return this.mBase.checkCallingOrSelfPermission(str);
    }

    @Override
    public int checkSelfPermission(String str) {
        return this.mBase.checkSelfPermission(str);
    }

    @Override
    public void enforcePermission(String str, int i, int i2, String str2) {
        this.mBase.enforcePermission(str, i, i2, str2);
    }

    @Override
    public void enforceCallingPermission(String str, String str2) {
        this.mBase.enforceCallingPermission(str, str2);
    }

    @Override
    public void enforceCallingOrSelfPermission(String str, String str2) {
        this.mBase.enforceCallingOrSelfPermission(str, str2);
    }

    @Override
    public void grantUriPermission(String str, Uri uri, int i) {
        this.mBase.grantUriPermission(str, uri, i);
    }

    @Override
    public void revokeUriPermission(Uri uri, int i) {
        this.mBase.revokeUriPermission(uri, i);
    }

    @Override
    public void revokeUriPermission(String str, Uri uri, int i) {
        this.mBase.revokeUriPermission(str, uri, i);
    }

    @Override
    public int checkUriPermission(Uri uri, int i, int i2, int i3) {
        return this.mBase.checkUriPermission(uri, i, i2, i3);
    }

    @Override
    public int checkUriPermission(Uri uri, int i, int i2, int i3, IBinder iBinder) {
        return this.mBase.checkUriPermission(uri, i, i2, i3, iBinder);
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int i) {
        return this.mBase.checkCallingUriPermission(uri, i);
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int i) {
        return this.mBase.checkCallingOrSelfUriPermission(uri, i);
    }

    @Override
    public int checkUriPermission(Uri uri, String str, String str2, int i, int i2, int i3) {
        return this.mBase.checkUriPermission(uri, str, str2, i, i2, i3);
    }

    @Override
    public void enforceUriPermission(Uri uri, int i, int i2, int i3, String str) {
        this.mBase.enforceUriPermission(uri, i, i2, i3, str);
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int i, String str) {
        this.mBase.enforceCallingUriPermission(uri, i, str);
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int i, String str) {
        this.mBase.enforceCallingOrSelfUriPermission(uri, i, str);
    }

    @Override
    public void enforceUriPermission(Uri uri, String str, String str2, int i, int i2, int i3, String str3) {
        this.mBase.enforceUriPermission(uri, str, str2, i, i2, i3, str3);
    }

    @Override
    public Context createPackageContext(String str, int i) throws PackageManager.NameNotFoundException {
        return this.mBase.createPackageContext(str, i);
    }

    @Override
    public Context createPackageContextAsUser(String str, int i, UserHandle userHandle) throws PackageManager.NameNotFoundException {
        return this.mBase.createPackageContextAsUser(str, i, userHandle);
    }

    @Override
    public Context createApplicationContext(ApplicationInfo applicationInfo, int i) throws PackageManager.NameNotFoundException {
        return this.mBase.createApplicationContext(applicationInfo, i);
    }

    @Override
    public Context createContextForSplit(String str) throws PackageManager.NameNotFoundException {
        return this.mBase.createContextForSplit(str);
    }

    @Override
    public int getUserId() {
        return this.mBase.getUserId();
    }

    @Override
    public Context createConfigurationContext(Configuration configuration) {
        return this.mBase.createConfigurationContext(configuration);
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
    public DisplayAdjustments getDisplayAdjustments(int i) {
        return this.mBase.getDisplayAdjustments(i);
    }

    @Override
    public Display getDisplay() {
        return this.mBase.getDisplay();
    }

    @Override
    public void updateDisplay(int i) {
        this.mBase.updateDisplay(i);
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        return this.mBase.createDeviceProtectedStorageContext();
    }

    @Override
    @SystemApi
    public Context createCredentialProtectedStorageContext() {
        return this.mBase.createCredentialProtectedStorageContext();
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        return this.mBase.isDeviceProtectedStorage();
    }

    @Override
    @SystemApi
    public boolean isCredentialProtectedStorage() {
        return this.mBase.isCredentialProtectedStorage();
    }

    @Override
    public boolean canLoadUnsafeResources() {
        return this.mBase.canLoadUnsafeResources();
    }

    @Override
    public IBinder getActivityToken() {
        return this.mBase.getActivityToken();
    }

    @Override
    public IServiceConnection getServiceDispatcher(ServiceConnection serviceConnection, Handler handler, int i) {
        return this.mBase.getServiceDispatcher(serviceConnection, handler, i);
    }

    @Override
    public IApplicationThread getIApplicationThread() {
        return this.mBase.getIApplicationThread();
    }

    @Override
    public Handler getMainThreadHandler() {
        return this.mBase.getMainThreadHandler();
    }

    @Override
    public int getNextAutofillId() {
        return this.mBase.getNextAutofillId();
    }

    @Override
    public AutofillManager.AutofillClient getAutofillClient() {
        return this.mBase.getAutofillClient();
    }

    @Override
    public void setAutofillClient(AutofillManager.AutofillClient autofillClient) {
        this.mBase.setAutofillClient(autofillClient);
    }

    @Override
    public boolean isAutofillCompatibilityEnabled() {
        return this.mBase != null && this.mBase.isAutofillCompatibilityEnabled();
    }

    @Override
    public void setAutofillCompatibilityEnabled(boolean z) {
        if (this.mBase != null) {
            this.mBase.setAutofillCompatibilityEnabled(z);
        }
    }
}
