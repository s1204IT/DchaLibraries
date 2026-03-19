package com.android.server.wallpaper;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IUserSwitchObserver;
import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
import android.app.PendingIntent;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.system.ErrnoException;
import android.system.Os;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManager;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.server.EventLogTags;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.mediatek.Manifest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WallpaperManagerService extends IWallpaperManager.Stub {
    private static final String ACTION_BG_RELEASE = "ACTION_BG_RELEASE";
    static final boolean DEBUG = false;
    static final int MAX_WALLPAPER_COMPONENT_LOG_LENGTH = 128;
    static final long MIN_WALLPAPER_CRASH_TIME = 10000;
    private static final int MSG_BIND_WP = 10101;
    static final String TAG = "WallpaperManagerService";
    final AppOpsManager mAppOpsManager;
    final Context mContext;
    int mCurrentUserId;
    final ComponentName mImageWallpaper;
    IWallpaperManagerCallback mKeyguardListener;
    private Intent mLastIntent;
    WallpaperData mLastWallpaper;
    private boolean mVisible;
    boolean mWaitingForUnlock;
    int mWallpaperId;
    static final String WALLPAPER = "wallpaper_orig";
    static final String WALLPAPER_CROP = "wallpaper";
    static final String WALLPAPER_LOCK_ORIG = "wallpaper_lock_orig";
    static final String WALLPAPER_LOCK_CROP = "wallpaper_lock";
    static final String WALLPAPER_INFO = "wallpaper_info.xml";
    static final String[] sPerUserFiles = {WALLPAPER, WALLPAPER_CROP, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP, WALLPAPER_INFO};
    final Object mLock = new Object();
    final SparseArray<WallpaperData> mWallpaperMap = new SparseArray<>();
    final SparseArray<WallpaperData> mLockWallpaperMap = new SparseArray<>();
    private boolean mExpectedLiving = true;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == WallpaperManagerService.MSG_BIND_WP) {
                int userId = UserHandle.getCallingUserId();
                Slog.v(WallpaperManagerService.TAG, "Receive message MSG_BIND_WP, bind service: " + WallpaperManagerService.this.mLastIntent.getComponent() + ",connection: " + WallpaperManagerService.this.mLastWallpaper.connection);
                WallpaperManagerService.this.mContext.bindServiceAsUser(WallpaperManagerService.this.mLastIntent, WallpaperManagerService.this.mLastWallpaper.connection, 536870913, new UserHandle(userId));
            }
        }
    };
    final IWindowManager mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    final IPackageManager mIPackageManager = AppGlobals.getPackageManager();
    final MyPackageMonitor mMonitor = new MyPackageMonitor();

    public static class Lifecycle extends SystemService {
        private WallpaperManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mService = new WallpaperManagerService(getContext());
            publishBinderService(WallpaperManagerService.WALLPAPER_CROP, this.mService);
        }

        @Override
        public void onBootPhase(int phase) throws Throwable {
            if (phase == 550) {
                this.mService.systemReady();
            } else {
                if (phase != 600) {
                    return;
                }
                this.mService.switchUser(0, null);
            }
        }

        @Override
        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }
    }

    private class WallpaperObserver extends FileObserver {
        final int mUserId;
        final WallpaperData mWallpaper;
        final File mWallpaperDir;
        final File mWallpaperFile;
        final File mWallpaperInfoFile;
        final File mWallpaperLockFile;

        public WallpaperObserver(WallpaperData wallpaper) {
            super(WallpaperManagerService.getWallpaperDir(wallpaper.userId).getAbsolutePath(), 1672);
            this.mUserId = wallpaper.userId;
            this.mWallpaperDir = WallpaperManagerService.getWallpaperDir(wallpaper.userId);
            this.mWallpaper = wallpaper;
            this.mWallpaperFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER);
            this.mWallpaperLockFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER_LOCK_ORIG);
            this.mWallpaperInfoFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER_INFO);
        }

        private WallpaperData dataForEvent(boolean sysChanged, boolean lockChanged) {
            WallpaperData wallpaper = null;
            synchronized (WallpaperManagerService.this.mLock) {
                if (lockChanged) {
                    wallpaper = WallpaperManagerService.this.mLockWallpaperMap.get(this.mUserId);
                    if (wallpaper == null) {
                        wallpaper = WallpaperManagerService.this.mWallpaperMap.get(this.mUserId);
                    }
                } else if (wallpaper == null) {
                }
            }
            return wallpaper != null ? wallpaper : this.mWallpaper;
        }

        @Override
        public void onEvent(int event, String path) {
            if (path == null) {
                return;
            }
            boolean moved = event == 128;
            boolean z = event != 8 ? moved : true;
            File changedFile = new File(this.mWallpaperDir, path);
            boolean sysWallpaperChanged = this.mWallpaperFile.equals(changedFile);
            boolean lockWallpaperChanged = this.mWallpaperLockFile.equals(changedFile);
            WallpaperData wallpaper = dataForEvent(sysWallpaperChanged, lockWallpaperChanged);
            if (moved && lockWallpaperChanged) {
                SELinux.restorecon(changedFile);
                WallpaperManagerService.this.notifyLockWallpaperChanged();
                return;
            }
            synchronized (WallpaperManagerService.this.mLock) {
                if (sysWallpaperChanged || lockWallpaperChanged) {
                    WallpaperManagerService.this.notifyCallbacksLocked(wallpaper);
                    if ((wallpaper.wallpaperComponent == null || event != 8 || wallpaper.imageWallpaperPending) && z) {
                        if (moved) {
                            SELinux.restorecon(changedFile);
                            WallpaperManagerService.this.loadSettingsLocked(wallpaper.userId, true);
                        }
                        WallpaperManagerService.this.generateCrop(wallpaper);
                        wallpaper.imageWallpaperPending = false;
                        if (wallpaper.setComplete != null) {
                            try {
                                wallpaper.setComplete.onWallpaperChanged();
                            } catch (RemoteException e) {
                            }
                        }
                        if (sysWallpaperChanged) {
                            WallpaperManagerService.this.bindWallpaperComponentLocked(WallpaperManagerService.this.mImageWallpaper, true, false, wallpaper, null);
                        }
                        if (lockWallpaperChanged || (wallpaper.whichPending & 2) != 0) {
                            if (!lockWallpaperChanged) {
                                WallpaperManagerService.this.mLockWallpaperMap.remove(wallpaper.userId);
                            }
                            WallpaperManagerService.this.notifyLockWallpaperChanged();
                        }
                        WallpaperManagerService.this.saveSettingsLocked(wallpaper.userId);
                    }
                }
            }
        }
    }

    void notifyLockWallpaperChanged() {
        IWallpaperManagerCallback cb = this.mKeyguardListener;
        if (cb == null) {
            return;
        }
        try {
            cb.onWallpaperChanged();
        } catch (RemoteException e) {
        }
    }

    private void generateCrop(WallpaperData wallpaper) throws Throwable {
        BitmapFactory.Options scaler;
        boolean success = false;
        Rect cropHint = new Rect(wallpaper.cropHint);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(wallpaper.wallpaperFile.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            Slog.e(TAG, "Invalid wallpaper data");
            success = false;
        } else {
            boolean needCrop = false;
            if (cropHint.isEmpty()) {
                cropHint.top = 0;
                cropHint.left = 0;
                cropHint.right = options.outWidth;
                cropHint.bottom = options.outHeight;
            } else {
                cropHint.offset(cropHint.right > options.outWidth ? options.outWidth - cropHint.right : 0, cropHint.bottom > options.outHeight ? options.outHeight - cropHint.bottom : 0);
                needCrop = options.outHeight >= cropHint.height() && options.outWidth >= cropHint.width();
            }
            boolean needScale = wallpaper.height != cropHint.height();
            if (!needCrop && !needScale) {
                success = FileUtils.copyFile(wallpaper.wallpaperFile, wallpaper.cropFile);
                if (!success) {
                    wallpaper.cropFile.delete();
                }
            } else {
                FileOutputStream f = null;
                BufferedOutputStream bos = null;
                try {
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(wallpaper.wallpaperFile.getAbsolutePath(), false);
                    int actualScale = cropHint.height() / wallpaper.height;
                    int scale = 1;
                    while (scale * 2 < actualScale) {
                        scale *= 2;
                    }
                    if (scale > 1) {
                        scaler = new BitmapFactory.Options();
                        scaler.inSampleSize = scale;
                    } else {
                        scaler = null;
                    }
                    Bitmap cropped = decoder.decodeRegion(cropHint, scaler);
                    decoder.recycle();
                    if (cropped == null) {
                        Slog.e(TAG, "Could not decode new wallpaper");
                    } else {
                        cropHint.offsetTo(0, 0);
                        cropHint.right /= scale;
                        cropHint.bottom /= scale;
                        float heightR = wallpaper.height / cropHint.height();
                        int destWidth = (int) (cropHint.width() * heightR);
                        Bitmap finalCrop = Bitmap.createScaledBitmap(cropped, destWidth, wallpaper.height, true);
                        FileOutputStream f2 = new FileOutputStream(wallpaper.cropFile);
                        try {
                            BufferedOutputStream bos2 = new BufferedOutputStream(f2, PackageManagerService.DumpState.DUMP_VERSION);
                            try {
                                finalCrop.compress(Bitmap.CompressFormat.JPEG, 100, bos2);
                                bos2.flush();
                                success = true;
                                bos = bos2;
                                f = f2;
                            } catch (Exception e) {
                                bos = bos2;
                                f = f2;
                                IoUtils.closeQuietly(bos);
                                IoUtils.closeQuietly(f);
                            } catch (Throwable th) {
                                th = th;
                                bos = bos2;
                                f = f2;
                                IoUtils.closeQuietly(bos);
                                IoUtils.closeQuietly(f);
                                throw th;
                            }
                        } catch (Exception e2) {
                            f = f2;
                        } catch (Throwable th2) {
                            th = th2;
                            f = f2;
                        }
                    }
                    IoUtils.closeQuietly(bos);
                    IoUtils.closeQuietly(f);
                } catch (Exception e3) {
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        }
        if (!success) {
            Slog.e(TAG, "Unable to apply new wallpaper");
            wallpaper.cropFile.delete();
        }
        if (!wallpaper.cropFile.exists()) {
            return;
        }
        SELinux.restorecon(wallpaper.cropFile.getAbsoluteFile());
    }

    static class WallpaperData {
        boolean allowBackup;
        WallpaperConnection connection;
        final File cropFile;
        boolean imageWallpaperPending;
        long lastDiedTime;
        ComponentName nextWallpaperComponent;
        IWallpaperManagerCallback setComplete;
        int userId;
        ComponentName wallpaperComponent;
        final File wallpaperFile;
        int wallpaperId;
        WallpaperObserver wallpaperObserver;
        boolean wallpaperUpdating;
        int whichPending;
        String name = "";
        private RemoteCallbackList<IWallpaperManagerCallback> callbacks = new RemoteCallbackList<>();
        int width = -1;
        int height = -1;
        final Rect cropHint = new Rect(0, 0, 0, 0);
        final Rect padding = new Rect(0, 0, 0, 0);

        WallpaperData(int userId, String inputFileName, String cropFileName) {
            this.userId = userId;
            File wallpaperDir = WallpaperManagerService.getWallpaperDir(userId);
            this.wallpaperFile = new File(wallpaperDir, inputFileName);
            this.cropFile = new File(wallpaperDir, cropFileName);
        }

        boolean cropExists() {
            return this.cropFile.exists();
        }
    }

    int makeWallpaperIdLocked() {
        do {
            this.mWallpaperId++;
        } while (this.mWallpaperId == 0);
        return this.mWallpaperId;
    }

    class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        IWallpaperEngine mEngine;
        final WallpaperInfo mInfo;
        IRemoteCallback mReply;
        IWallpaperService mService;
        WallpaperData mWallpaper;
        final Binder mToken = new Binder();
        boolean mDimensionsChanged = false;
        boolean mPaddingChanged = false;

        public WallpaperConnection(WallpaperInfo info, WallpaperData wallpaper) {
            this.mInfo = info;
            this.mWallpaper = wallpaper;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection == this) {
                    WallpaperManagerService.this.mExpectedLiving = true;
                    this.mService = IWallpaperService.Stub.asInterface(service);
                    WallpaperManagerService.this.attachServiceLocked(this, this.mWallpaper);
                    WallpaperManagerService.this.saveSettingsLocked(this.mWallpaper.userId);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (WallpaperManagerService.this.mLock) {
                this.mService = null;
                this.mEngine = null;
                Slog.w(WallpaperManagerService.TAG, "onServiceDisconnected(): " + name);
                WallpaperManagerService.this.mExpectedLiving = false;
                if (WallpaperManagerService.this.isGmoRamOptimizeSupport() && !WallpaperManagerService.this.mVisible) {
                    return;
                }
                if (this.mWallpaper.connection == this) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper service gone: " + this.mWallpaper.wallpaperComponent);
                    if (!this.mWallpaper.wallpaperUpdating && this.mWallpaper.userId == WallpaperManagerService.this.mCurrentUserId) {
                        if (this.mWallpaper.lastDiedTime != 0 && this.mWallpaper.lastDiedTime + 10000 > SystemClock.uptimeMillis()) {
                            Slog.w(WallpaperManagerService.TAG, "Reverting to built-in wallpaper!");
                            WallpaperManagerService.this.clearWallpaperLocked(true, 1, this.mWallpaper.userId, null);
                        } else {
                            this.mWallpaper.lastDiedTime = SystemClock.uptimeMillis();
                        }
                        String flattened = name.flattenToString();
                        EventLog.writeEvent(EventLogTags.WP_WALLPAPER_CRASHED, flattened.substring(0, Math.min(flattened.length(), 128)));
                    }
                }
            }
        }

        public void attachEngine(IWallpaperEngine engine) {
            synchronized (WallpaperManagerService.this.mLock) {
                this.mEngine = engine;
                if (this.mDimensionsChanged) {
                    try {
                        this.mEngine.setDesiredSize(this.mWallpaper.width, this.mWallpaper.height);
                    } catch (RemoteException e) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper dimensions", e);
                    }
                    this.mDimensionsChanged = false;
                    if (this.mPaddingChanged) {
                        try {
                            this.mEngine.setDisplayPadding(this.mWallpaper.padding);
                        } catch (RemoteException e2) {
                            Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper padding", e2);
                        }
                        this.mPaddingChanged = false;
                    }
                } else if (this.mPaddingChanged) {
                }
            }
        }

        public void engineShown(IWallpaperEngine engine) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mReply != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        this.mReply.sendResult((Bundle) null);
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(ident);
                    }
                    this.mReply = null;
                }
            }
        }

        public ParcelFileDescriptor setWallpaper(String name) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection != this) {
                    return null;
                }
                return WallpaperManagerService.this.updateWallpaperBitmapLocked(name, this.mWallpaper, null);
            }
        }
    }

    class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null && wallpaper.wallpaperComponent != null && wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                    wallpaper.wallpaperUpdating = false;
                    ComponentName comp = wallpaper.wallpaperComponent;
                    WallpaperManagerService.this.clearWallpaperComponentLocked(wallpaper);
                    if (!WallpaperManagerService.this.bindWallpaperComponentLocked(comp, false, false, wallpaper, null)) {
                        Slog.w(WallpaperManagerService.TAG, "Wallpaper no longer available; reverting to default");
                        WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                    }
                }
            }
        }

        public void onPackageModified(String packageName) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    if (wallpaper.wallpaperComponent == null || !wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                    } else {
                        doPackagesChangedLocked(true, wallpaper);
                    }
                }
            }
        }

        public void onPackageUpdateStarted(String packageName, int uid) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null && wallpaper.wallpaperComponent != null && wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                    wallpaper.wallpaperUpdating = true;
                }
            }
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            synchronized (WallpaperManagerService.this.mLock) {
                boolean changed = false;
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return false;
                }
                WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    boolean res = doPackagesChangedLocked(doit, wallpaper);
                    changed = res;
                }
                return changed;
            }
        }

        public void onSomePackagesChanged() {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    doPackagesChangedLocked(true, wallpaper);
                }
            }
        }

        boolean doPackagesChangedLocked(boolean doit, WallpaperData wallpaper) throws Throwable {
            int change;
            int change2;
            boolean changed = false;
            if (wallpaper.wallpaperComponent != null && ((change2 = isPackageDisappearing(wallpaper.wallpaperComponent.getPackageName())) == 3 || change2 == 2)) {
                changed = true;
                if (doit) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper uninstalled, removing: " + wallpaper.wallpaperComponent);
                    WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                }
            }
            if (wallpaper.nextWallpaperComponent != null && ((change = isPackageDisappearing(wallpaper.nextWallpaperComponent.getPackageName())) == 3 || change == 2)) {
                wallpaper.nextWallpaperComponent = null;
            }
            if (wallpaper.wallpaperComponent != null && isPackageModified(wallpaper.wallpaperComponent.getPackageName())) {
                try {
                    WallpaperManagerService.this.mContext.getPackageManager().getServiceInfo(wallpaper.wallpaperComponent, 786432);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper component gone, removing: " + wallpaper.wallpaperComponent);
                    WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                }
            }
            if (wallpaper.nextWallpaperComponent != null && isPackageModified(wallpaper.nextWallpaperComponent.getPackageName())) {
                try {
                    WallpaperManagerService.this.mContext.getPackageManager().getServiceInfo(wallpaper.nextWallpaperComponent, 786432);
                } catch (PackageManager.NameNotFoundException e2) {
                    wallpaper.nextWallpaperComponent = null;
                }
            }
            return changed;
        }
    }

    public WallpaperManagerService(Context context) throws Throwable {
        this.mContext = context;
        this.mImageWallpaper = ComponentName.unflattenFromString(context.getResources().getString(R.string.config_systemAppProtectionService));
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mMonitor.register(context, null, UserHandle.ALL, true);
        getWallpaperDir(0).mkdirs();
        loadSettingsLocked(0, false);
    }

    private static File getWallpaperDir(int userId) {
        return Environment.getUserSystemDirectory(userId);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        for (int i = 0; i < this.mWallpaperMap.size(); i++) {
            WallpaperData wallpaper = this.mWallpaperMap.valueAt(i);
            wallpaper.wallpaperObserver.stopWatching();
        }
    }

    void systemReady() throws Throwable {
        WallpaperData wallpaper = this.mWallpaperMap.get(0);
        if (this.mImageWallpaper.equals(wallpaper.nextWallpaperComponent)) {
            if (!wallpaper.cropExists()) {
                generateCrop(wallpaper);
            }
            if (!wallpaper.cropExists()) {
                clearWallpaperLocked(false, 1, 0, null);
            }
        }
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!"android.intent.action.USER_REMOVED".equals(action)) {
                    return;
                }
                WallpaperManagerService.this.onRemoveUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
            }
        }, userFilter);
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new IUserSwitchObserver.Stub() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    WallpaperManagerService.this.switchUser(newUserId, reply);
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                }

                public void onForegroundProfileSwitch(int newProfileId) {
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        String str;
        if (Binder.getCallingUid() != 1000) {
            throw new RuntimeException("getName() can only be called from the system process");
        }
        synchronized (this.mLock) {
            str = this.mWallpaperMap.get(0).name;
        }
        return str;
    }

    void stopObserver(WallpaperData wallpaper) {
        if (wallpaper == null || wallpaper.wallpaperObserver == null) {
            return;
        }
        wallpaper.wallpaperObserver.stopWatching();
        wallpaper.wallpaperObserver = null;
    }

    void stopObserversLocked(int userId) {
        stopObserver(this.mWallpaperMap.get(userId));
        stopObserver(this.mLockWallpaperMap.get(userId));
        this.mWallpaperMap.remove(userId);
        this.mLockWallpaperMap.remove(userId);
    }

    void onUnlockUser(int userId) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == userId && this.mWaitingForUnlock) {
                switchUser(userId, null);
            }
        }
    }

    void onRemoveUser(int userId) {
        if (userId < 1) {
            return;
        }
        File wallpaperDir = getWallpaperDir(userId);
        synchronized (this.mLock) {
            stopObserversLocked(userId);
            for (String filename : sPerUserFiles) {
                new File(wallpaperDir, filename).delete();
            }
        }
    }

    void switchUser(int userId, IRemoteCallback reply) {
        synchronized (this.mLock) {
            this.mCurrentUserId = userId;
            WallpaperData wallpaper = getWallpaperSafeLocked(userId, 1);
            if (wallpaper.wallpaperObserver == null) {
                wallpaper.wallpaperObserver = new WallpaperObserver(wallpaper);
                wallpaper.wallpaperObserver.startWatching();
            }
            switchWallpaper(wallpaper, reply);
        }
    }

    void switchWallpaper(WallpaperData wallpaper, IRemoteCallback reply) {
        synchronized (this.mLock) {
            this.mWaitingForUnlock = false;
            ComponentName cname = wallpaper.wallpaperComponent != null ? wallpaper.wallpaperComponent : wallpaper.nextWallpaperComponent;
            if (!bindWallpaperComponentLocked(cname, true, false, wallpaper, reply)) {
                ServiceInfo si = null;
                try {
                    si = this.mIPackageManager.getServiceInfo(cname, PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED, wallpaper.userId);
                } catch (RemoteException e) {
                }
                if (si == null) {
                    Slog.w(TAG, "Failure starting previous wallpaper; clearing");
                    clearWallpaperLocked(false, 1, wallpaper.userId, reply);
                } else {
                    Slog.w(TAG, "Wallpaper isn't direct boot aware; using fallback until unlocked");
                    wallpaper.wallpaperComponent = wallpaper.nextWallpaperComponent;
                    WallpaperData fallback = new WallpaperData(wallpaper.userId, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                    ensureSaneWallpaperData(fallback);
                    bindWallpaperComponentLocked(this.mImageWallpaper, true, false, fallback, reply);
                    this.mWaitingForUnlock = true;
                }
            }
        }
    }

    public void clearWallpaper(String callingPackage, int which, int userId) {
        checkPermission("android.permission.SET_WALLPAPER");
        if (!isWallpaperSupported(callingPackage) || !isSetWallpaperAllowed(callingPackage)) {
            return;
        }
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "clearWallpaper", null);
        synchronized (this.mLock) {
            clearWallpaperLocked(false, which, userId2, null);
        }
    }

    void clearWallpaperLocked(boolean defaultFailed, int which, int userId, IRemoteCallback reply) throws Throwable {
        WallpaperData wallpaper;
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper to read");
        }
        if (which == 2) {
            wallpaper = this.mLockWallpaperMap.get(userId);
            if (wallpaper == null) {
                return;
            }
        } else {
            wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper == null) {
                loadSettingsLocked(userId, false);
                wallpaper = this.mWallpaperMap.get(userId);
            }
        }
        if (wallpaper == null) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            if (wallpaper.wallpaperFile.exists()) {
                wallpaper.wallpaperFile.delete();
                wallpaper.cropFile.delete();
                if (which == 2) {
                    this.mLockWallpaperMap.remove(userId);
                    IWallpaperManagerCallback cb = this.mKeyguardListener;
                    if (cb != null) {
                        try {
                            cb.onWallpaperChanged();
                        } catch (RemoteException e) {
                        }
                    }
                    saveSettingsLocked(userId);
                    return;
                }
            }
            RuntimeException e2 = null;
            try {
                wallpaper.imageWallpaperPending = false;
                if (userId != this.mCurrentUserId) {
                    return;
                }
                if (bindWallpaperComponentLocked(defaultFailed ? this.mImageWallpaper : null, true, false, wallpaper, reply)) {
                    return;
                }
            } catch (IllegalArgumentException e1) {
                e2 = e1;
            }
            Slog.e(TAG, "Default wallpaper component not found!", e2);
            clearWallpaperComponentLocked(wallpaper);
            if (reply != null) {
                try {
                    reply.sendResult((Bundle) null);
                } catch (RemoteException e3) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean hasNamedWallpaper(String name) {
        synchronized (this.mLock) {
            long ident = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = ((UserManager) this.mContext.getSystemService("user")).getUsers();
                Binder.restoreCallingIdentity(ident);
                for (UserInfo user : users) {
                    if (!user.isManagedProfile()) {
                        WallpaperData wd = this.mWallpaperMap.get(user.id);
                        if (wd == null) {
                            loadSettingsLocked(user.id, false);
                            wd = this.mWallpaperMap.get(user.id);
                        }
                        if (wd != null && name.equals(wd.name)) {
                            return true;
                        }
                    }
                }
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
    }

    private Point getDefaultDisplaySize() {
        Point p = new Point();
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        Display d = wm.getDefaultDisplay();
        d.getRealSize(p);
        return p;
    }

    public void setDimensionHints(int width, int height, String callingPackage) throws RemoteException {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        if (!isWallpaperSupported(callingPackage)) {
            return;
        }
        synchronized (this.mLock) {
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = getWallpaperSafeLocked(userId, 1);
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("width and height must be > 0");
            }
            Point displaySize = getDefaultDisplaySize();
            int width2 = Math.max(width, displaySize.x);
            int height2 = Math.max(height, displaySize.y);
            if (width2 != wallpaper.width || height2 != wallpaper.height) {
                wallpaper.width = width2;
                wallpaper.height = height2;
                saveSettingsLocked(userId);
                if (this.mCurrentUserId != userId) {
                    return;
                }
                if (wallpaper.connection != null) {
                    if (wallpaper.connection.mEngine != null) {
                        try {
                            wallpaper.connection.mEngine.setDesiredSize(width2, height2);
                        } catch (RemoteException e) {
                        }
                        notifyCallbacksLocked(wallpaper);
                    } else if (wallpaper.connection.mService != null) {
                        wallpaper.connection.mDimensionsChanged = true;
                    }
                }
            }
        }
    }

    public int getWidthHint() throws RemoteException {
        synchronized (this.mLock) {
            WallpaperData wallpaper = this.mWallpaperMap.get(UserHandle.getCallingUserId());
            if (wallpaper != null) {
                return wallpaper.width;
            }
            return 0;
        }
    }

    public int getHeightHint() throws RemoteException {
        synchronized (this.mLock) {
            WallpaperData wallpaper = this.mWallpaperMap.get(UserHandle.getCallingUserId());
            if (wallpaper != null) {
                return wallpaper.height;
            }
            return 0;
        }
    }

    public void setDisplayPadding(Rect padding, String callingPackage) {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        if (!isWallpaperSupported(callingPackage)) {
            return;
        }
        synchronized (this.mLock) {
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = getWallpaperSafeLocked(userId, 1);
            if (padding.left < 0 || padding.top < 0 || padding.right < 0 || padding.bottom < 0) {
                throw new IllegalArgumentException("padding must be positive: " + padding);
            }
            if (!padding.equals(wallpaper.padding)) {
                wallpaper.padding.set(padding);
                saveSettingsLocked(userId);
                if (this.mCurrentUserId != userId) {
                    return;
                }
                if (wallpaper.connection != null) {
                    if (wallpaper.connection.mEngine != null) {
                        try {
                            wallpaper.connection.mEngine.setDisplayPadding(padding);
                        } catch (RemoteException e) {
                        }
                        notifyCallbacksLocked(wallpaper);
                    } else if (wallpaper.connection.mService != null) {
                        wallpaper.connection.mPaddingChanged = true;
                    }
                }
            }
        }
    }

    public ParcelFileDescriptor getWallpaper(IWallpaperManagerCallback cb, int which, Bundle outParams, int wallpaperUserId) {
        int wallpaperUserId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), wallpaperUserId, false, true, "getWallpaper", null);
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper to read");
        }
        synchronized (this.mLock) {
            SparseArray<WallpaperData> whichSet = which == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
            WallpaperData wallpaper = whichSet.get(wallpaperUserId2);
            if (wallpaper == null) {
                loadSettingsLocked(wallpaperUserId2, false);
                wallpaper = whichSet.get(wallpaperUserId2);
                if (wallpaper == null) {
                    return null;
                }
            }
            if (outParams != null) {
                try {
                    outParams.putInt("width", wallpaper.width);
                    outParams.putInt("height", wallpaper.height);
                    if (cb != null) {
                        wallpaper.callbacks.register(cb);
                    }
                    if (wallpaper.cropFile.exists()) {
                        return null;
                    }
                    return ParcelFileDescriptor.open(wallpaper.cropFile, 268435456);
                } catch (FileNotFoundException e) {
                    Slog.w(TAG, "Error getting wallpaper", e);
                    return null;
                }
            }
            if (cb != null) {
            }
            if (wallpaper.cropFile.exists()) {
            }
        }
    }

    public WallpaperInfo getWallpaperInfo() {
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper == null || wallpaper.connection == null) {
                return null;
            }
            return wallpaper.connection.mInfo;
        }
    }

    public int getWallpaperIdForUser(int which, int userId) {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getWallpaperIdForUser", null);
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper");
        }
        SparseArray<WallpaperData> map = which == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
        synchronized (this.mLock) {
            WallpaperData wallpaper = map.get(userId2);
            if (wallpaper != null) {
                return wallpaper.wallpaperId;
            }
            return -1;
        }
    }

    public boolean setLockWallpaperCallback(IWallpaperManagerCallback cb) {
        checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW");
        synchronized (this.mLock) {
            this.mKeyguardListener = cb;
        }
        return true;
    }

    public ParcelFileDescriptor setWallpaper(String name, String callingPackage, Rect cropHint, boolean allowBackup, Bundle extras, int which, IWallpaperManagerCallback completion) {
        WallpaperData wallpaper;
        ParcelFileDescriptor pfd;
        checkPermission("android.permission.SET_WALLPAPER");
        if ((which & 3) == 0) {
            Slog.e(TAG, "Must specify a valid wallpaper category to set");
            throw new IllegalArgumentException("Must specify a valid wallpaper category to set");
        }
        if (!isWallpaperSupported(callingPackage) || !isSetWallpaperAllowed(callingPackage)) {
            return null;
        }
        if (cropHint == null) {
            cropHint = new Rect(0, 0, 0, 0);
        } else if (cropHint.isEmpty() || cropHint.left < 0 || cropHint.top < 0) {
            throw new IllegalArgumentException("Invalid crop rect supplied: " + cropHint);
        }
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            if (which == 1) {
                if (this.mLockWallpaperMap.get(userId) == null) {
                    migrateSystemToLockWallpaperLocked(userId);
                }
                wallpaper = getWallpaperSafeLocked(userId, which);
                long ident = Binder.clearCallingIdentity();
                try {
                    pfd = updateWallpaperBitmapLocked(name, wallpaper, extras);
                    if (pfd != null) {
                        wallpaper.imageWallpaperPending = true;
                        wallpaper.whichPending = which;
                        wallpaper.setComplete = completion;
                        wallpaper.cropHint.set(cropHint);
                        if ((which & 1) != 0) {
                            wallpaper.allowBackup = allowBackup;
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                wallpaper = getWallpaperSafeLocked(userId, which);
                long ident2 = Binder.clearCallingIdentity();
                pfd = updateWallpaperBitmapLocked(name, wallpaper, extras);
                if (pfd != null) {
                }
            }
        }
        return pfd;
    }

    private void migrateSystemToLockWallpaperLocked(int userId) {
        WallpaperData sysWP = this.mWallpaperMap.get(userId);
        if (sysWP == null) {
            return;
        }
        WallpaperData lockWP = new WallpaperData(userId, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
        lockWP.wallpaperId = sysWP.wallpaperId;
        lockWP.cropHint.set(sysWP.cropHint);
        lockWP.width = sysWP.width;
        lockWP.height = sysWP.height;
        lockWP.allowBackup = false;
        try {
            Os.rename(sysWP.wallpaperFile.getAbsolutePath(), lockWP.wallpaperFile.getAbsolutePath());
            Os.rename(sysWP.cropFile.getAbsolutePath(), lockWP.cropFile.getAbsolutePath());
            this.mLockWallpaperMap.put(userId, lockWP);
        } catch (ErrnoException e) {
            Slog.e(TAG, "Can't migrate system wallpaper: " + e.getMessage());
            lockWP.wallpaperFile.delete();
            lockWP.cropFile.delete();
        }
    }

    ParcelFileDescriptor updateWallpaperBitmapLocked(String name, WallpaperData wallpaper, Bundle extras) {
        if (name == null) {
            name = "";
        }
        try {
            File dir = getWallpaperDir(wallpaper.userId);
            if (!dir.exists()) {
                dir.mkdir();
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(wallpaper.wallpaperFile, 1006632960);
            if (!SELinux.restorecon(wallpaper.wallpaperFile)) {
                return null;
            }
            wallpaper.name = name;
            wallpaper.wallpaperId = makeWallpaperIdLocked();
            if (extras != null) {
                extras.putInt("android.service.wallpaper.extra.ID", wallpaper.wallpaperId);
            }
            return fd;
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "Error setting wallpaper", e);
            return null;
        }
    }

    public void setWallpaperComponentChecked(ComponentName name, String callingPackage) {
        if (!isWallpaperSupported(callingPackage) || !isSetWallpaperAllowed(callingPackage)) {
            return;
        }
        setWallpaperComponent(name);
    }

    public void setWallpaperComponent(ComponentName name) {
        checkPermission(Manifest.permission.SET_WALLPAPER_COMPONENT);
        synchronized (this.mLock) {
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper == null) {
                throw new IllegalStateException("Wallpaper not yet initialized for user " + userId);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                wallpaper.imageWallpaperPending = false;
                if (bindWallpaperComponentLocked(name, false, true, wallpaper, null)) {
                    wallpaper.wallpaperId = makeWallpaperIdLocked();
                    notifyCallbacksLocked(wallpaper);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    boolean bindWallpaperComponentLocked(ComponentName componentName, boolean force, boolean fromUser, WallpaperData wallpaper, IRemoteCallback reply) {
        if (!force && wallpaper.connection != null) {
            if (wallpaper.wallpaperComponent == null) {
                if (componentName == null) {
                    return true;
                }
            } else if (wallpaper.wallpaperComponent.equals(componentName)) {
                return true;
            }
        }
        if (componentName == null) {
            try {
                componentName = WallpaperManager.getDefaultWallpaperComponent(this.mContext);
                if (componentName == null) {
                    componentName = this.mImageWallpaper;
                }
            } catch (RemoteException e) {
                String msg = "Remote exception for " + componentName + "\n" + e;
                if (fromUser) {
                    throw new IllegalArgumentException(msg);
                }
                Slog.w(TAG, msg);
                return false;
            }
        }
        int serviceUserId = wallpaper.userId;
        ServiceInfo si = this.mIPackageManager.getServiceInfo(componentName, 4224, serviceUserId);
        if (si == null) {
            Slog.w(TAG, "Attempted wallpaper " + componentName + " is unavailable");
            return false;
        }
        if (!"android.permission.BIND_WALLPAPER".equals(si.permission)) {
            String msg2 = "Selected service does not require android.permission.BIND_WALLPAPER: " + componentName;
            if (fromUser) {
                throw new SecurityException(msg2);
            }
            Slog.w(TAG, msg2);
            return false;
        }
        WallpaperInfo wi = null;
        Intent intent = new Intent("android.service.wallpaper.WallpaperService");
        if (componentName != null) {
            if (!componentName.equals(this.mImageWallpaper)) {
                List<ResolveInfo> ris = this.mIPackageManager.queryIntentServices(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 128, serviceUserId).getList();
                int i = 0;
                while (true) {
                    if (i >= ris.size()) {
                        break;
                    }
                    ServiceInfo rsi = ris.get(i).serviceInfo;
                    if (!rsi.name.equals(si.name) || !rsi.packageName.equals(si.packageName)) {
                        i++;
                    } else {
                        try {
                            break;
                        } catch (IOException e2) {
                            if (fromUser) {
                                throw new IllegalArgumentException(e2);
                            }
                            Slog.w(TAG, e2);
                            return false;
                        } catch (XmlPullParserException e3) {
                            if (fromUser) {
                                throw new IllegalArgumentException(e3);
                            }
                            Slog.w(TAG, e3);
                            return false;
                        }
                    }
                }
                if (wi == null) {
                    String msg3 = "Selected service is not a wallpaper: " + componentName;
                    if (fromUser) {
                        throw new SecurityException(msg3);
                    }
                    Slog.w(TAG, msg3);
                    return false;
                }
            }
        }
        WallpaperConnection newConn = new WallpaperConnection(wi, wallpaper);
        intent.setComponent(componentName);
        intent.putExtra("android.intent.extra.client_label", R.string.foreground_service_multiple_separator);
        intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(this.mContext, 0, Intent.createChooser(new Intent("android.intent.action.SET_WALLPAPER"), this.mContext.getText(R.string.foreground_service_tap_for_details)), 0, null, new UserHandle(serviceUserId)));
        if (!this.mContext.bindServiceAsUser(intent, newConn, 570425345, new UserHandle(serviceUserId))) {
            String msg4 = "Unable to bind service: " + componentName;
            if (fromUser) {
                throw new IllegalArgumentException(msg4);
            }
            Slog.w(TAG, msg4);
            return false;
        }
        if (isGmoRamOptimizeSupport()) {
            this.mLastIntent = intent;
            ActivityManagerNative.getDefault().setWallpaperProcess(componentName);
            Slog.v(TAG, "Tell ActivityManager current wallpaper process is " + componentName);
        }
        if (wallpaper.userId == this.mCurrentUserId && this.mLastWallpaper != null) {
            detachWallpaperLocked(this.mLastWallpaper);
        }
        wallpaper.wallpaperComponent = componentName;
        wallpaper.connection = newConn;
        newConn.mReply = reply;
        try {
            if (wallpaper.userId == this.mCurrentUserId) {
                this.mIWindowManager.addWindowToken(newConn.mToken, 2013);
                this.mLastWallpaper = wallpaper;
                return true;
            }
            return true;
        } catch (RemoteException e4) {
            return true;
        }
    }

    void detachWallpaperLocked(WallpaperData wallpaper) {
        if (wallpaper.connection == null) {
            return;
        }
        if (wallpaper.connection.mReply != null) {
            try {
                wallpaper.connection.mReply.sendResult((Bundle) null);
            } catch (RemoteException e) {
            }
            wallpaper.connection.mReply = null;
        }
        if (wallpaper.connection.mEngine != null) {
            try {
                wallpaper.connection.mEngine.destroy();
            } catch (RemoteException e2) {
            }
        }
        this.mContext.unbindService(wallpaper.connection);
        try {
            this.mIWindowManager.removeWindowToken(wallpaper.connection.mToken);
        } catch (RemoteException e3) {
        }
        wallpaper.connection.mService = null;
        wallpaper.connection.mEngine = null;
        wallpaper.connection = null;
    }

    void clearWallpaperComponentLocked(WallpaperData wallpaper) {
        wallpaper.wallpaperComponent = null;
        detachWallpaperLocked(wallpaper);
    }

    void attachServiceLocked(WallpaperConnection conn, WallpaperData wallpaper) {
        try {
            conn.mService.attach(conn, conn.mToken, 2013, false, wallpaper.width, wallpaper.height, wallpaper.padding);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed attaching wallpaper; clearing", e);
            if (wallpaper.wallpaperUpdating) {
                return;
            }
            bindWallpaperComponentLocked(null, false, false, wallpaper, null);
        }
    }

    private void notifyCallbacksLocked(WallpaperData wallpaper) {
        int n = wallpaper.callbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                wallpaper.callbacks.getBroadcastItem(i).onWallpaperChanged();
            } catch (RemoteException e) {
            }
        }
        wallpaper.callbacks.finishBroadcast();
        Intent intent = new Intent("android.intent.action.WALLPAPER_CHANGED");
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(this.mCurrentUserId));
    }

    private void checkPermission(String permission) {
        if (this.mContext.checkCallingOrSelfPermission(permission) == 0) {
        } else {
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + permission);
        }
    }

    public boolean isWallpaperSupported(String callingPackage) {
        return this.mAppOpsManager.checkOpNoThrow(48, Binder.getCallingUid(), callingPackage) == 0;
    }

    public boolean isSetWallpaperAllowed(String callingPackage) {
        PackageManager pm = this.mContext.getPackageManager();
        String[] uidPackages = pm.getPackagesForUid(Binder.getCallingUid());
        boolean uidMatchPackage = Arrays.asList(uidPackages).contains(callingPackage);
        if (!uidMatchPackage) {
            return false;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
        if (dpm.isDeviceOwnerApp(callingPackage) || dpm.isProfileOwnerApp(callingPackage)) {
            return true;
        }
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        return !um.hasUserRestriction("no_set_wallpaper");
    }

    public boolean isWallpaperBackupEligible(int userId) throws Throwable {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call isWallpaperBackupEligible");
        }
        WallpaperData wallpaper = getWallpaperSafeLocked(userId, 1);
        if (wallpaper != null) {
            return wallpaper.allowBackup;
        }
        return false;
    }

    private static JournaledFile makeJournaledFile(int userId) {
        String base = new File(getWallpaperDir(userId), WALLPAPER_INFO).getAbsolutePath();
        return new JournaledFile(new File(base), new File(base + ".tmp"));
    }

    private void saveSettingsLocked(int userId) {
        JournaledFile journal = makeJournaledFile(userId);
        BufferedOutputStream stream = null;
        try {
            XmlSerializer out = new FastXmlSerializer();
            FileOutputStream fstream = new FileOutputStream(journal.chooseForWrite(), false);
            try {
                BufferedOutputStream stream2 = new BufferedOutputStream(fstream);
                try {
                    out.setOutput(stream2, StandardCharsets.UTF_8.name());
                    out.startDocument(null, true);
                    WallpaperData wallpaper = this.mWallpaperMap.get(userId);
                    if (wallpaper != null) {
                        writeWallpaperAttributes(out, "wp", wallpaper);
                    }
                    WallpaperData wallpaper2 = this.mLockWallpaperMap.get(userId);
                    if (wallpaper2 != null) {
                        writeWallpaperAttributes(out, "kwp", wallpaper2);
                    }
                    out.endDocument();
                    stream2.flush();
                    FileUtils.sync(fstream);
                    stream2.close();
                    journal.commit();
                } catch (IOException e) {
                    stream = stream2;
                    IoUtils.closeQuietly(stream);
                    journal.rollback();
                }
            } catch (IOException e2) {
            }
        } catch (IOException e3) {
        }
    }

    private void writeWallpaperAttributes(XmlSerializer out, String tag, WallpaperData wallpaper) throws IllegalStateException, IOException, IllegalArgumentException {
        out.startTag(null, tag);
        out.attribute(null, "id", Integer.toString(wallpaper.wallpaperId));
        out.attribute(null, "width", Integer.toString(wallpaper.width));
        out.attribute(null, "height", Integer.toString(wallpaper.height));
        out.attribute(null, "cropLeft", Integer.toString(wallpaper.cropHint.left));
        out.attribute(null, "cropTop", Integer.toString(wallpaper.cropHint.top));
        out.attribute(null, "cropRight", Integer.toString(wallpaper.cropHint.right));
        out.attribute(null, "cropBottom", Integer.toString(wallpaper.cropHint.bottom));
        if (wallpaper.padding.left != 0) {
            out.attribute(null, "paddingLeft", Integer.toString(wallpaper.padding.left));
        }
        if (wallpaper.padding.top != 0) {
            out.attribute(null, "paddingTop", Integer.toString(wallpaper.padding.top));
        }
        if (wallpaper.padding.right != 0) {
            out.attribute(null, "paddingRight", Integer.toString(wallpaper.padding.right));
        }
        if (wallpaper.padding.bottom != 0) {
            out.attribute(null, "paddingBottom", Integer.toString(wallpaper.padding.bottom));
        }
        out.attribute(null, "name", wallpaper.name);
        if (wallpaper.wallpaperComponent != null && !wallpaper.wallpaperComponent.equals(this.mImageWallpaper)) {
            out.attribute(null, "component", wallpaper.wallpaperComponent.flattenToShortString());
        }
        if (wallpaper.allowBackup) {
            out.attribute(null, "backup", "true");
        }
        out.endTag(null, tag);
    }

    private void migrateFromOld() {
        File oldWallpaper = new File("/data/data/com.android.settings/files/wallpaper");
        File oldInfo = new File("/data/system/wallpaper_info.xml");
        if (oldWallpaper.exists()) {
            File newWallpaper = new File(getWallpaperDir(0), WALLPAPER);
            oldWallpaper.renameTo(newWallpaper);
        }
        if (!oldInfo.exists()) {
            return;
        }
        File newInfo = new File(getWallpaperDir(0), WALLPAPER_INFO);
        oldInfo.renameTo(newInfo);
    }

    private int getAttributeInt(XmlPullParser parser, String name, int defValue) {
        String value = parser.getAttributeValue(null, name);
        if (value == null) {
            return defValue;
        }
        return Integer.parseInt(value);
    }

    private WallpaperData getWallpaperSafeLocked(int userId, int which) throws Throwable {
        SparseArray<WallpaperData> whichSet = which == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
        WallpaperData wallpaper = whichSet.get(userId);
        if (wallpaper == null) {
            loadSettingsLocked(userId, false);
            WallpaperData wallpaper2 = whichSet.get(userId);
            if (wallpaper2 == null) {
                if (which == 2) {
                    WallpaperData wallpaper3 = new WallpaperData(userId, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                    this.mLockWallpaperMap.put(userId, wallpaper3);
                    ensureSaneWallpaperData(wallpaper3);
                    return wallpaper3;
                }
                Slog.wtf(TAG, "Didn't find wallpaper in non-lock case!");
                WallpaperData wallpaper4 = new WallpaperData(userId, WALLPAPER, WALLPAPER_CROP);
                this.mWallpaperMap.put(userId, wallpaper4);
                ensureSaneWallpaperData(wallpaper4);
                return wallpaper4;
            }
            return wallpaper2;
        }
        return wallpaper;
    }

    private void loadSettingsLocked(int userId, boolean keepDimensionHints) throws Throwable {
        int type;
        ComponentName componentNameUnflattenFromString;
        JournaledFile journal = makeJournaledFile(userId);
        FileInputStream stream = null;
        File file = journal.chooseForRead();
        if (!file.exists()) {
            migrateFromOld();
        }
        WallpaperData wallpaper = this.mWallpaperMap.get(userId);
        if (wallpaper == null) {
            wallpaper = new WallpaperData(userId, WALLPAPER, WALLPAPER_CROP);
            wallpaper.allowBackup = true;
            this.mWallpaperMap.put(userId, wallpaper);
            if (!wallpaper.cropExists()) {
                generateCrop(wallpaper);
            }
        }
        boolean success = false;
        try {
            FileInputStream stream2 = new FileInputStream(file);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream2, StandardCharsets.UTF_8.name());
                do {
                    type = parser.next();
                    if (type == 2) {
                        String tag = parser.getName();
                        if ("wp".equals(tag)) {
                            parseWallpaperAttributes(parser, wallpaper, keepDimensionHints);
                            String comp = parser.getAttributeValue(null, "component");
                            if (comp != null) {
                                componentNameUnflattenFromString = ComponentName.unflattenFromString(comp);
                            } else {
                                componentNameUnflattenFromString = null;
                            }
                            wallpaper.nextWallpaperComponent = componentNameUnflattenFromString;
                            if (wallpaper.nextWallpaperComponent == null || "android".equals(wallpaper.nextWallpaperComponent.getPackageName())) {
                                wallpaper.nextWallpaperComponent = this.mImageWallpaper;
                            }
                        } else if ("kwp".equals(tag)) {
                            WallpaperData lockWallpaper = this.mLockWallpaperMap.get(userId);
                            if (lockWallpaper == null) {
                                lockWallpaper = new WallpaperData(userId, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                                this.mLockWallpaperMap.put(userId, lockWallpaper);
                            }
                            parseWallpaperAttributes(parser, lockWallpaper, false);
                        }
                    }
                } while (type != 1);
                success = true;
                stream = stream2;
            } catch (FileNotFoundException e) {
                stream = stream2;
                Slog.w(TAG, "no current wallpaper -- first boot?");
            } catch (IOException e2) {
                e = e2;
                stream = stream2;
                Slog.w(TAG, "failed parsing " + file + " " + e);
            } catch (IndexOutOfBoundsException e3) {
                e = e3;
                stream = stream2;
                Slog.w(TAG, "failed parsing " + file + " " + e);
            } catch (NullPointerException e4) {
                e = e4;
                stream = stream2;
                Slog.w(TAG, "failed parsing " + file + " " + e);
            } catch (NumberFormatException e5) {
                e = e5;
                stream = stream2;
                Slog.w(TAG, "failed parsing " + file + " " + e);
            } catch (XmlPullParserException e6) {
                e = e6;
                stream = stream2;
                Slog.w(TAG, "failed parsing " + file + " " + e);
            }
        } catch (FileNotFoundException e7) {
        } catch (IOException e8) {
            e = e8;
        } catch (IndexOutOfBoundsException e9) {
            e = e9;
        } catch (NullPointerException e10) {
            e = e10;
        } catch (NumberFormatException e11) {
            e = e11;
        } catch (XmlPullParserException e12) {
            e = e12;
        }
        IoUtils.closeQuietly(stream);
        if (!success) {
            wallpaper.width = -1;
            wallpaper.height = -1;
            wallpaper.cropHint.set(0, 0, 0, 0);
            wallpaper.padding.set(0, 0, 0, 0);
            wallpaper.name = "";
            this.mLockWallpaperMap.remove(userId);
        } else if (wallpaper.wallpaperId <= 0) {
            wallpaper.wallpaperId = makeWallpaperIdLocked();
        }
        ensureSaneWallpaperData(wallpaper);
        WallpaperData lockWallpaper2 = this.mLockWallpaperMap.get(userId);
        if (lockWallpaper2 == null) {
            return;
        }
        ensureSaneWallpaperData(lockWallpaper2);
    }

    private void ensureSaneWallpaperData(WallpaperData wallpaper) {
        int baseSize = getMaximumSizeDimension();
        if (wallpaper.width < baseSize) {
            wallpaper.width = baseSize;
        }
        if (wallpaper.height < baseSize) {
            wallpaper.height = baseSize;
        }
        if (wallpaper.cropHint.width() > 0 && wallpaper.cropHint.height() > 0) {
            return;
        }
        wallpaper.cropHint.set(0, 0, wallpaper.width, wallpaper.height);
    }

    private void parseWallpaperAttributes(XmlPullParser parser, WallpaperData wallpaper, boolean keepDimensionHints) {
        String idString = parser.getAttributeValue(null, "id");
        if (idString != null) {
            int id = Integer.parseInt(idString);
            wallpaper.wallpaperId = id;
            if (id > this.mWallpaperId) {
                this.mWallpaperId = id;
            }
        } else {
            wallpaper.wallpaperId = makeWallpaperIdLocked();
        }
        if (!keepDimensionHints) {
            wallpaper.width = Integer.parseInt(parser.getAttributeValue(null, "width"));
            wallpaper.height = Integer.parseInt(parser.getAttributeValue(null, "height"));
        }
        wallpaper.cropHint.left = getAttributeInt(parser, "cropLeft", 0);
        wallpaper.cropHint.top = getAttributeInt(parser, "cropTop", 0);
        wallpaper.cropHint.right = getAttributeInt(parser, "cropRight", 0);
        wallpaper.cropHint.bottom = getAttributeInt(parser, "cropBottom", 0);
        wallpaper.padding.left = getAttributeInt(parser, "paddingLeft", 0);
        wallpaper.padding.top = getAttributeInt(parser, "paddingTop", 0);
        wallpaper.padding.right = getAttributeInt(parser, "paddingRight", 0);
        wallpaper.padding.bottom = getAttributeInt(parser, "paddingBottom", 0);
        wallpaper.name = parser.getAttributeValue(null, "name");
        wallpaper.allowBackup = "true".equals(parser.getAttributeValue(null, "backup"));
    }

    private int getMaximumSizeDimension() {
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        Display d = wm.getDefaultDisplay();
        return d.getMaximumSizeDimension();
    }

    public void settingsRestored() {
        WallpaperData wallpaper;
        boolean success;
        if (Binder.getCallingUid() != 1000) {
            throw new RuntimeException("settingsRestored() can only be called from the system process");
        }
        synchronized (this.mLock) {
            loadSettingsLocked(0, false);
            wallpaper = this.mWallpaperMap.get(0);
            wallpaper.wallpaperId = makeWallpaperIdLocked();
            wallpaper.allowBackup = true;
            if (wallpaper.nextWallpaperComponent != null && !wallpaper.nextWallpaperComponent.equals(this.mImageWallpaper)) {
                if (!bindWallpaperComponentLocked(wallpaper.nextWallpaperComponent, false, false, wallpaper, null)) {
                    bindWallpaperComponentLocked(null, false, false, wallpaper, null);
                }
                success = true;
            } else {
                if ("".equals(wallpaper.name)) {
                    success = true;
                } else {
                    success = restoreNamedResourceLocked(wallpaper);
                }
                if (success) {
                    generateCrop(wallpaper);
                    bindWallpaperComponentLocked(wallpaper.nextWallpaperComponent, true, false, wallpaper, null);
                }
            }
        }
        if (!success) {
            Slog.e(TAG, "Failed to restore wallpaper: '" + wallpaper.name + "'");
            wallpaper.name = "";
            getWallpaperDir(0).delete();
        }
        synchronized (this.mLock) {
            saveSettingsLocked(0);
        }
    }

    boolean restoreNamedResourceLocked(WallpaperData wallpaper) throws Throwable {
        if (wallpaper.name.length() <= 4 || !"res:".equals(wallpaper.name.substring(0, 4))) {
            return false;
        }
        String resName = wallpaper.name.substring(4);
        int colon = resName.indexOf(58);
        String pkg = colon > 0 ? resName.substring(0, colon) : null;
        int slash = resName.lastIndexOf(47);
        String ident = slash > 0 ? resName.substring(slash + 1) : null;
        String type = null;
        if (colon > 0 && slash > 0 && slash - colon > 1) {
            type = resName.substring(colon + 1, slash);
        }
        if (pkg == null || ident == null || type == null) {
            return false;
        }
        int resId = -1;
        InputStream res = null;
        FileOutputStream fos = null;
        FileOutputStream cos = null;
        try {
            try {
                Context c = this.mContext.createPackageContext(pkg, 4);
                Resources r = c.getResources();
                resId = r.getIdentifier(resName, null, null);
                if (resId == 0) {
                    Slog.e(TAG, "couldn't resolve identifier pkg=" + pkg + " type=" + type + " ident=" + ident);
                    IoUtils.closeQuietly((AutoCloseable) null);
                    IoUtils.closeQuietly((AutoCloseable) null);
                    IoUtils.closeQuietly((AutoCloseable) null);
                    return false;
                }
                res = r.openRawResource(resId);
                if (wallpaper.wallpaperFile.exists()) {
                    wallpaper.wallpaperFile.delete();
                    wallpaper.cropFile.delete();
                }
                FileOutputStream fos2 = new FileOutputStream(wallpaper.wallpaperFile);
                try {
                    FileOutputStream cos2 = new FileOutputStream(wallpaper.cropFile);
                    try {
                        byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
                        while (true) {
                            int amt = res.read(buffer);
                            if (amt <= 0) {
                                break;
                            }
                            fos2.write(buffer, 0, amt);
                            cos2.write(buffer, 0, amt);
                        }
                        Slog.v(TAG, "Restored wallpaper: " + resName);
                        IoUtils.closeQuietly(res);
                        if (fos2 != null) {
                            FileUtils.sync(fos2);
                        }
                        if (cos2 != null) {
                            FileUtils.sync(cos2);
                        }
                        IoUtils.closeQuietly(fos2);
                        IoUtils.closeQuietly(cos2);
                        return true;
                    } catch (PackageManager.NameNotFoundException e) {
                        cos = cos2;
                        fos = fos2;
                        Slog.e(TAG, "Package name " + pkg + " not found");
                        IoUtils.closeQuietly(res);
                        if (fos != null) {
                            FileUtils.sync(fos);
                        }
                        if (cos != null) {
                            FileUtils.sync(cos);
                        }
                        IoUtils.closeQuietly(fos);
                        IoUtils.closeQuietly(cos);
                        return false;
                    } catch (Resources.NotFoundException e2) {
                        cos = cos2;
                        fos = fos2;
                        Slog.e(TAG, "Resource not found: " + resId);
                        IoUtils.closeQuietly(res);
                        if (fos != null) {
                            FileUtils.sync(fos);
                        }
                        if (cos != null) {
                            FileUtils.sync(cos);
                        }
                        IoUtils.closeQuietly(fos);
                        IoUtils.closeQuietly(cos);
                        return false;
                    } catch (IOException e3) {
                        e = e3;
                        cos = cos2;
                        fos = fos2;
                        Slog.e(TAG, "IOException while restoring wallpaper ", e);
                        IoUtils.closeQuietly(res);
                        if (fos != null) {
                            FileUtils.sync(fos);
                        }
                        if (cos != null) {
                            FileUtils.sync(cos);
                        }
                        IoUtils.closeQuietly(fos);
                        IoUtils.closeQuietly(cos);
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        cos = cos2;
                        fos = fos2;
                        IoUtils.closeQuietly(res);
                        if (fos != null) {
                            FileUtils.sync(fos);
                        }
                        if (cos != null) {
                            FileUtils.sync(cos);
                        }
                        IoUtils.closeQuietly(fos);
                        IoUtils.closeQuietly(cos);
                        throw th;
                    }
                } catch (PackageManager.NameNotFoundException e4) {
                    fos = fos2;
                } catch (Resources.NotFoundException e5) {
                    fos = fos2;
                } catch (IOException e6) {
                    e = e6;
                    fos = fos2;
                } catch (Throwable th2) {
                    th = th2;
                    fos = fos2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (PackageManager.NameNotFoundException e7) {
        } catch (Resources.NotFoundException e8) {
        } catch (IOException e9) {
            e = e9;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump wallpaper service from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mLock) {
            pw.println("System wallpaper state:");
            for (int i = 0; i < this.mWallpaperMap.size(); i++) {
                WallpaperData wallpaper = this.mWallpaperMap.valueAt(i);
                pw.print(" User ");
                pw.print(wallpaper.userId);
                pw.print(": id=");
                pw.println(wallpaper.wallpaperId);
                pw.print("  mWidth=");
                pw.print(wallpaper.width);
                pw.print(" mHeight=");
                pw.println(wallpaper.height);
                pw.print("  mCropHint=");
                pw.println(wallpaper.cropHint);
                pw.print("  mPadding=");
                pw.println(wallpaper.padding);
                pw.print("  mName=");
                pw.println(wallpaper.name);
                pw.print("  mWallpaperComponent=");
                pw.println(wallpaper.wallpaperComponent);
                if (wallpaper.connection != null) {
                    WallpaperConnection conn = wallpaper.connection;
                    pw.print("  Wallpaper connection ");
                    pw.print(conn);
                    pw.println(":");
                    if (conn.mInfo != null) {
                        pw.print("    mInfo.component=");
                        pw.println(conn.mInfo.getComponent());
                    }
                    pw.print("    mToken=");
                    pw.println(conn.mToken);
                    pw.print("    mService=");
                    pw.println(conn.mService);
                    pw.print("    mEngine=");
                    pw.println(conn.mEngine);
                    pw.print("    mLastDiedTime=");
                    pw.println(wallpaper.lastDiedTime - SystemClock.uptimeMillis());
                }
            }
            pw.println("Lock wallpaper state:");
            for (int i2 = 0; i2 < this.mLockWallpaperMap.size(); i2++) {
                WallpaperData wallpaper2 = this.mLockWallpaperMap.valueAt(i2);
                pw.print(" User ");
                pw.print(wallpaper2.userId);
                pw.print(": id=");
                pw.println(wallpaper2.wallpaperId);
                pw.print("  mWidth=");
                pw.print(wallpaper2.width);
                pw.print(" mHeight=");
                pw.println(wallpaper2.height);
                pw.print("  mCropHint=");
                pw.println(wallpaper2.cropHint);
                pw.print("  mPadding=");
                pw.println(wallpaper2.padding);
                pw.print("  mName=");
                pw.println(wallpaper2.name);
            }
        }
    }

    public void onVisibilityChanged(boolean isVisible) throws RemoteException {
        if (!isGmoRamOptimizeSupport() || this.mVisible == isVisible) {
            return;
        }
        this.mVisible = isVisible;
        modifyWallpaperAdj(this.mVisible);
        doVisibilityChanged(this.mVisible);
    }

    private void doVisibilityChanged(boolean isVisible) {
        if (!isVisible || this.mExpectedLiving || this.mLastWallpaper.wallpaperComponent.toString().equals(this.mImageWallpaper.toString())) {
            return;
        }
        this.mHandler.removeMessages(MSG_BIND_WP);
        this.mHandler.sendEmptyMessage(MSG_BIND_WP);
    }

    private void modifyWallpaperAdj(boolean isVisible) {
        try {
            ActivityManagerNative.getDefault().updateWallpaperState(isVisible);
        } catch (RemoteException e) {
            Slog.w(TAG, "Modify wallpaper's ADJ, catch RemoteException!!!!!");
        }
    }

    private boolean isGmoRamOptimizeSupport() {
        return SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1");
    }
}
