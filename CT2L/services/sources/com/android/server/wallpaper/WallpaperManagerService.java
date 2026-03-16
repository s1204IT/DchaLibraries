package com.android.server.wallpaper;

import android.R;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.IUserSwitchObserver;
import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
import android.app.PendingIntent;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.backup.BackupManager;
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
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
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
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class WallpaperManagerService extends IWallpaperManager.Stub {
    static final boolean DEBUG = false;
    static final int MAX_WALLPAPER_COMPONENT_LOG_LENGTH = 128;
    static final long MIN_WALLPAPER_CRASH_TIME = 10000;
    static final String TAG = "WallpaperManagerService";
    static final String WALLPAPER = "wallpaper";
    static final String WALLPAPER_INFO = "wallpaper_info.xml";
    final Context mContext;
    int mCurrentUserId;
    final ComponentName mImageWallpaper;
    WallpaperData mLastWallpaper;
    final Object mLock = new Object[0];
    SparseArray<WallpaperData> mWallpaperMap = new SparseArray<>();
    final IWindowManager mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    final IPackageManager mIPackageManager = AppGlobals.getPackageManager();
    final MyPackageMonitor mMonitor = new MyPackageMonitor();

    private class WallpaperObserver extends FileObserver {
        final WallpaperData mWallpaper;
        final File mWallpaperDir;
        final File mWallpaperFile;
        final File mWallpaperInfoFile;

        public WallpaperObserver(WallpaperData wallpaper) {
            super(WallpaperManagerService.getWallpaperDir(wallpaper.userId).getAbsolutePath(), 1672);
            this.mWallpaperDir = WallpaperManagerService.getWallpaperDir(wallpaper.userId);
            this.mWallpaper = wallpaper;
            this.mWallpaperFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER);
            this.mWallpaperInfoFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER_INFO);
        }

        @Override
        public void onEvent(int event, String path) {
            boolean written = WallpaperManagerService.DEBUG;
            if (path != null) {
                synchronized (WallpaperManagerService.this.mLock) {
                    File changedFile = new File(this.mWallpaperDir, path);
                    if (this.mWallpaperFile.equals(changedFile) || this.mWallpaperInfoFile.equals(changedFile)) {
                        long origId = Binder.clearCallingIdentity();
                        BackupManager bm = new BackupManager(WallpaperManagerService.this.mContext);
                        bm.dataChanged();
                        Binder.restoreCallingIdentity(origId);
                    }
                    if (this.mWallpaperFile.equals(changedFile)) {
                        WallpaperManagerService.this.notifyCallbacksLocked(this.mWallpaper);
                        if (event == 8 || event == 128) {
                            written = true;
                        }
                        if (this.mWallpaper.wallpaperComponent == null || event != 8 || this.mWallpaper.imageWallpaperPending) {
                            if (written) {
                                this.mWallpaper.imageWallpaperPending = WallpaperManagerService.DEBUG;
                            }
                            WallpaperManagerService.this.bindWallpaperComponentLocked(WallpaperManagerService.this.mImageWallpaper, true, WallpaperManagerService.DEBUG, this.mWallpaper, null);
                            WallpaperManagerService.this.saveSettingsLocked(this.mWallpaper);
                        }
                    }
                }
            }
        }
    }

    static class WallpaperData {
        WallpaperConnection connection;
        boolean imageWallpaperPending;
        long lastDiedTime;
        ComponentName nextWallpaperComponent;
        int userId;
        ComponentName wallpaperComponent;
        File wallpaperFile;
        WallpaperObserver wallpaperObserver;
        boolean wallpaperUpdating;
        String name = "";
        private RemoteCallbackList<IWallpaperManagerCallback> callbacks = new RemoteCallbackList<>();
        int width = -1;
        int height = -1;
        final Rect padding = new Rect(0, 0, 0, 0);

        WallpaperData(int userId) {
            this.userId = userId;
            this.wallpaperFile = new File(WallpaperManagerService.getWallpaperDir(userId), WallpaperManagerService.WALLPAPER);
        }
    }

    class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        IWallpaperEngine mEngine;
        final WallpaperInfo mInfo;
        IRemoteCallback mReply;
        IWallpaperService mService;
        WallpaperData mWallpaper;
        final Binder mToken = new Binder();
        boolean mDimensionsChanged = WallpaperManagerService.DEBUG;
        boolean mPaddingChanged = WallpaperManagerService.DEBUG;

        public WallpaperConnection(WallpaperInfo info, WallpaperData wallpaper) {
            this.mInfo = info;
            this.mWallpaper = wallpaper;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection == this) {
                    this.mService = IWallpaperService.Stub.asInterface(service);
                    WallpaperManagerService.this.attachServiceLocked(this, this.mWallpaper);
                    WallpaperManagerService.this.saveSettingsLocked(this.mWallpaper);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (WallpaperManagerService.this.mLock) {
                this.mService = null;
                this.mEngine = null;
                if (this.mWallpaper.connection == this) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper service gone: " + this.mWallpaper.wallpaperComponent);
                    if (!this.mWallpaper.wallpaperUpdating && this.mWallpaper.userId == WallpaperManagerService.this.mCurrentUserId) {
                        if (this.mWallpaper.lastDiedTime != 0 && this.mWallpaper.lastDiedTime + WallpaperManagerService.MIN_WALLPAPER_CRASH_TIME > SystemClock.uptimeMillis()) {
                            Slog.w(WallpaperManagerService.TAG, "Reverting to built-in wallpaper!");
                            WallpaperManagerService.this.clearWallpaperLocked(true, this.mWallpaper.userId, null);
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
                    this.mDimensionsChanged = WallpaperManagerService.DEBUG;
                    if (!this.mPaddingChanged) {
                        try {
                            this.mEngine.setDisplayPadding(this.mWallpaper.padding);
                        } catch (RemoteException e2) {
                            Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper padding", e2);
                        }
                        this.mPaddingChanged = WallpaperManagerService.DEBUG;
                    }
                } else if (!this.mPaddingChanged) {
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
            ParcelFileDescriptor parcelFileDescriptorUpdateWallpaperBitmapLocked;
            synchronized (WallpaperManagerService.this.mLock) {
                parcelFileDescriptorUpdateWallpaperBitmapLocked = this.mWallpaper.connection == this ? WallpaperManagerService.this.updateWallpaperBitmapLocked(name, this.mWallpaper) : null;
            }
            return parcelFileDescriptorUpdateWallpaperBitmapLocked;
        }
    }

    class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId == getChangingUserId()) {
                    WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                    if (wallpaper != null && wallpaper.wallpaperComponent != null && wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                        wallpaper.wallpaperUpdating = WallpaperManagerService.DEBUG;
                        ComponentName comp = wallpaper.wallpaperComponent;
                        WallpaperManagerService.this.clearWallpaperComponentLocked(wallpaper);
                        if (!WallpaperManagerService.this.bindWallpaperComponentLocked(comp, WallpaperManagerService.DEBUG, WallpaperManagerService.DEBUG, wallpaper, null)) {
                            Slog.w(WallpaperManagerService.TAG, "Wallpaper no longer available; reverting to default");
                            WallpaperManagerService.this.clearWallpaperLocked(WallpaperManagerService.DEBUG, wallpaper.userId, null);
                        }
                    }
                }
            }
        }

        public void onPackageModified(String packageName) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId == getChangingUserId()) {
                    WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                    if (wallpaper != null) {
                        if (wallpaper.wallpaperComponent != null && wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                            doPackagesChangedLocked(true, wallpaper);
                        }
                    }
                }
            }
        }

        public void onPackageUpdateStarted(String packageName, int uid) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId == getChangingUserId()) {
                    WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                    if (wallpaper != null && wallpaper.wallpaperComponent != null && wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                        wallpaper.wallpaperUpdating = true;
                    }
                }
            }
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            synchronized (WallpaperManagerService.this.mLock) {
                boolean changed = WallpaperManagerService.DEBUG;
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return WallpaperManagerService.DEBUG;
                }
                WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    boolean res = doPackagesChangedLocked(doit, wallpaper);
                    changed = false | res;
                }
                return changed;
            }
        }

        public void onSomePackagesChanged() {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId == getChangingUserId()) {
                    WallpaperData wallpaper = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                    if (wallpaper != null) {
                        doPackagesChangedLocked(true, wallpaper);
                    }
                }
            }
        }

        boolean doPackagesChangedLocked(boolean doit, WallpaperData wallpaper) {
            int change;
            int change2;
            boolean changed = WallpaperManagerService.DEBUG;
            if (wallpaper.wallpaperComponent != null && ((change2 = isPackageDisappearing(wallpaper.wallpaperComponent.getPackageName())) == 3 || change2 == 2)) {
                changed = true;
                if (doit) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper uninstalled, removing: " + wallpaper.wallpaperComponent);
                    WallpaperManagerService.this.clearWallpaperLocked(WallpaperManagerService.DEBUG, wallpaper.userId, null);
                }
            }
            if (wallpaper.nextWallpaperComponent != null && ((change = isPackageDisappearing(wallpaper.nextWallpaperComponent.getPackageName())) == 3 || change == 2)) {
                wallpaper.nextWallpaperComponent = null;
            }
            if (wallpaper.wallpaperComponent != null && isPackageModified(wallpaper.wallpaperComponent.getPackageName())) {
                try {
                    WallpaperManagerService.this.mContext.getPackageManager().getServiceInfo(wallpaper.wallpaperComponent, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper component gone, removing: " + wallpaper.wallpaperComponent);
                    WallpaperManagerService.this.clearWallpaperLocked(WallpaperManagerService.DEBUG, wallpaper.userId, null);
                }
            }
            if (wallpaper.nextWallpaperComponent != null && isPackageModified(wallpaper.nextWallpaperComponent.getPackageName())) {
                try {
                    WallpaperManagerService.this.mContext.getPackageManager().getServiceInfo(wallpaper.nextWallpaperComponent, 0);
                } catch (PackageManager.NameNotFoundException e2) {
                    wallpaper.nextWallpaperComponent = null;
                }
            }
            return changed;
        }
    }

    public WallpaperManagerService(Context context) {
        this.mContext = context;
        this.mImageWallpaper = ComponentName.unflattenFromString(context.getResources().getString(R.string.config_defaultAssistant));
        this.mMonitor.register(context, null, UserHandle.ALL, true);
        getWallpaperDir(0).mkdirs();
        loadSettingsLocked(0);
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

    public void systemRunning() {
        WallpaperData wallpaper = this.mWallpaperMap.get(0);
        switchWallpaper(wallpaper, null);
        wallpaper.wallpaperObserver = new WallpaperObserver(wallpaper);
        wallpaper.wallpaperObserver.startWatching();
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        userFilter.addAction("android.intent.action.USER_STOPPING");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    WallpaperManagerService.this.onRemoveUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
                }
            }
        }, userFilter);
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new IUserSwitchObserver.Stub() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    WallpaperManagerService.this.switchUser(newUserId, reply);
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
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

    void onStoppingUser(int userId) {
        if (userId >= 1) {
            synchronized (this.mLock) {
                WallpaperData wallpaper = this.mWallpaperMap.get(userId);
                if (wallpaper != null) {
                    if (wallpaper.wallpaperObserver != null) {
                        wallpaper.wallpaperObserver.stopWatching();
                        wallpaper.wallpaperObserver = null;
                    }
                    this.mWallpaperMap.remove(userId);
                }
            }
        }
    }

    void onRemoveUser(int userId) {
        if (userId >= 1) {
            synchronized (this.mLock) {
                onStoppingUser(userId);
                File wallpaperFile = new File(getWallpaperDir(userId), WALLPAPER);
                wallpaperFile.delete();
                File wallpaperInfoFile = new File(getWallpaperDir(userId), WALLPAPER_INFO);
                wallpaperInfoFile.delete();
            }
        }
    }

    void switchUser(int userId, IRemoteCallback reply) {
        synchronized (this.mLock) {
            this.mCurrentUserId = userId;
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper == null) {
                wallpaper = new WallpaperData(userId);
                this.mWallpaperMap.put(userId, wallpaper);
                loadSettingsLocked(userId);
            }
            if (wallpaper.wallpaperObserver == null) {
                wallpaper.wallpaperObserver = new WallpaperObserver(wallpaper);
                wallpaper.wallpaperObserver.startWatching();
            }
            switchWallpaper(wallpaper, reply);
        }
    }

    void switchWallpaper(WallpaperData wallpaper, IRemoteCallback reply) {
        synchronized (this.mLock) {
            RuntimeException e = null;
            try {
                ComponentName cname = wallpaper.wallpaperComponent != null ? wallpaper.wallpaperComponent : wallpaper.nextWallpaperComponent;
                if (bindWallpaperComponentLocked(cname, true, DEBUG, wallpaper, reply)) {
                    return;
                }
            } catch (RuntimeException e1) {
                e = e1;
            }
            Slog.w(TAG, "Failure starting previous wallpaper", e);
            clearWallpaperLocked(DEBUG, wallpaper.userId, reply);
        }
    }

    public void clearWallpaper() {
        synchronized (this.mLock) {
            clearWallpaperLocked(DEBUG, UserHandle.getCallingUserId(), null);
        }
    }

    void clearWallpaperLocked(boolean defaultFailed, int userId, IRemoteCallback reply) {
        WallpaperData wallpaper = this.mWallpaperMap.get(userId);
        File f = new File(getWallpaperDir(userId), WALLPAPER);
        if (f.exists()) {
            f.delete();
        }
        long ident = Binder.clearCallingIdentity();
        RuntimeException e = null;
        try {
            try {
                wallpaper.imageWallpaperPending = DEBUG;
                if (userId != this.mCurrentUserId) {
                    return;
                }
                if (bindWallpaperComponentLocked(defaultFailed ? this.mImageWallpaper : null, true, DEBUG, wallpaper, reply)) {
                    return;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } catch (IllegalArgumentException e1) {
            e = e1;
        }
        Slog.e(TAG, "Default wallpaper component not found!", e);
        clearWallpaperComponentLocked(wallpaper);
        if (reply != null) {
            try {
                reply.sendResult((Bundle) null);
            } catch (RemoteException e2) {
            }
        }
    }

    public boolean hasNamedWallpaper(String name) {
        synchronized (this.mLock) {
            long ident = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = ((UserManager) this.mContext.getSystemService("user")).getUsers();
                Binder.restoreCallingIdentity(ident);
                for (UserInfo user : users) {
                    WallpaperData wd = this.mWallpaperMap.get(user.id);
                    if (wd == null) {
                        loadSettingsLocked(user.id);
                        wd = this.mWallpaperMap.get(user.id);
                    }
                    if (wd != null && name.equals(wd.name)) {
                        return true;
                    }
                }
                return DEBUG;
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

    public void setDimensionHints(int width, int height) throws RemoteException {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        synchronized (this.mLock) {
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper == null) {
                throw new IllegalStateException("Wallpaper not yet initialized for user " + userId);
            }
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("width and height must be > 0");
            }
            Point displaySize = getDefaultDisplaySize();
            int width2 = Math.max(width, displaySize.x);
            int height2 = Math.max(height, displaySize.y);
            if (width2 != wallpaper.width || height2 != wallpaper.height) {
                wallpaper.width = width2;
                wallpaper.height = height2;
                saveSettingsLocked(wallpaper);
                if (this.mCurrentUserId == userId) {
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
    }

    public int getWidthHint() throws RemoteException {
        int i;
        synchronized (this.mLock) {
            WallpaperData wallpaper = this.mWallpaperMap.get(UserHandle.getCallingUserId());
            i = wallpaper.width;
        }
        return i;
    }

    public int getHeightHint() throws RemoteException {
        int i;
        synchronized (this.mLock) {
            WallpaperData wallpaper = this.mWallpaperMap.get(UserHandle.getCallingUserId());
            i = wallpaper.height;
        }
        return i;
    }

    public void setDisplayPadding(Rect padding) {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        synchronized (this.mLock) {
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper == null) {
                throw new IllegalStateException("Wallpaper not yet initialized for user " + userId);
            }
            if (padding.left < 0 || padding.top < 0 || padding.right < 0 || padding.bottom < 0) {
                throw new IllegalArgumentException("padding must be positive: " + padding);
            }
            if (!padding.equals(wallpaper.padding)) {
                wallpaper.padding.set(padding);
                saveSettingsLocked(wallpaper);
                if (this.mCurrentUserId == userId) {
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
    }

    public ParcelFileDescriptor getWallpaper(IWallpaperManagerCallback cb, Bundle outParams) {
        int wallpaperUserId;
        ParcelFileDescriptor parcelFileDescriptorOpen = null;
        synchronized (this.mLock) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 1000) {
                wallpaperUserId = this.mCurrentUserId;
            } else {
                wallpaperUserId = UserHandle.getUserId(callingUid);
            }
            WallpaperData wallpaper = this.mWallpaperMap.get(wallpaperUserId);
            if (outParams != null) {
                try {
                    outParams.putInt("width", wallpaper.width);
                    outParams.putInt("height", wallpaper.height);
                } catch (FileNotFoundException e) {
                    Slog.w(TAG, "Error getting wallpaper", e);
                }
            }
            wallpaper.callbacks.register(cb);
            File f = new File(getWallpaperDir(wallpaperUserId), WALLPAPER);
            if (f.exists()) {
                parcelFileDescriptorOpen = ParcelFileDescriptor.open(f, 268435456);
            }
        }
        return parcelFileDescriptorOpen;
    }

    public WallpaperInfo getWallpaperInfo() {
        WallpaperInfo wallpaperInfo;
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            wallpaperInfo = wallpaper.connection != null ? wallpaper.connection.mInfo : null;
        }
        return wallpaperInfo;
    }

    public ParcelFileDescriptor setWallpaper(String name) {
        ParcelFileDescriptor pfd;
        checkPermission("android.permission.SET_WALLPAPER");
        synchronized (this.mLock) {
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper == null) {
                throw new IllegalStateException("Wallpaper not yet initialized for user " + userId);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                pfd = updateWallpaperBitmapLocked(name, wallpaper);
                if (pfd != null) {
                    wallpaper.imageWallpaperPending = true;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return pfd;
    }

    ParcelFileDescriptor updateWallpaperBitmapLocked(String name, WallpaperData wallpaper) {
        if (name == null) {
            name = "";
        }
        try {
            File dir = getWallpaperDir(wallpaper.userId);
            if (!dir.exists()) {
                dir.mkdir();
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            File file = new File(dir, WALLPAPER);
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, 1006632960);
            if (!SELinux.restorecon(file)) {
                return null;
            }
            wallpaper.name = name;
            return fd;
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "Error setting wallpaper", e);
            return null;
        }
    }

    public void setWallpaperComponent(ComponentName name) {
        checkPermission("android.permission.SET_WALLPAPER_COMPONENT");
        synchronized (this.mLock) {
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper == null) {
                throw new IllegalStateException("Wallpaper not yet initialized for user " + userId);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                wallpaper.imageWallpaperPending = DEBUG;
                bindWallpaperComponentLocked(name, DEBUG, true, wallpaper, null);
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
                return DEBUG;
            }
        }
        int serviceUserId = wallpaper.userId;
        ServiceInfo si = this.mIPackageManager.getServiceInfo(componentName, 4224, serviceUserId);
        if (si == null) {
            Slog.w(TAG, "Attempted wallpaper " + componentName + " is unavailable");
            return DEBUG;
        }
        if (!"android.permission.BIND_WALLPAPER".equals(si.permission)) {
            String msg2 = "Selected service does not require android.permission.BIND_WALLPAPER: " + componentName;
            if (fromUser) {
                throw new SecurityException(msg2);
            }
            Slog.w(TAG, msg2);
            return DEBUG;
        }
        WallpaperInfo wi = null;
        Intent intent = new Intent("android.service.wallpaper.WallpaperService");
        if (componentName != null) {
            if (!componentName.equals(this.mImageWallpaper)) {
                List<ResolveInfo> ris = this.mIPackageManager.queryIntentServices(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 128, serviceUserId);
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
                            return DEBUG;
                        } catch (XmlPullParserException e3) {
                            if (fromUser) {
                                throw new IllegalArgumentException(e3);
                            }
                            Slog.w(TAG, e3);
                            return DEBUG;
                        }
                    }
                }
                if (wi == null) {
                    String msg3 = "Selected service is not a wallpaper: " + componentName;
                    if (fromUser) {
                        throw new SecurityException(msg3);
                    }
                    Slog.w(TAG, msg3);
                    return DEBUG;
                }
            }
        }
        WallpaperConnection newConn = new WallpaperConnection(wi, wallpaper);
        intent.setComponent(componentName);
        intent.putExtra("android.intent.extra.client_label", R.string.keyguard_accessibility_user_selector);
        intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(this.mContext, 0, Intent.createChooser(new Intent("android.intent.action.SET_WALLPAPER"), this.mContext.getText(R.string.keyguard_accessibility_widget)), 0, null, new UserHandle(serviceUserId)));
        if (!this.mContext.bindServiceAsUser(intent, newConn, 536870913, new UserHandle(serviceUserId))) {
            String msg4 = "Unable to bind service: " + componentName;
            if (fromUser) {
                throw new IllegalArgumentException(msg4);
            }
            Slog.w(TAG, msg4);
            return DEBUG;
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
            }
        } catch (RemoteException e4) {
        }
        return true;
    }

    void detachWallpaperLocked(WallpaperData wallpaper) {
        if (wallpaper.connection != null) {
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
    }

    void clearWallpaperComponentLocked(WallpaperData wallpaper) {
        wallpaper.wallpaperComponent = null;
        detachWallpaperLocked(wallpaper);
    }

    void attachServiceLocked(WallpaperConnection conn, WallpaperData wallpaper) {
        try {
            conn.mService.attach(conn, conn.mToken, 2013, DEBUG, wallpaper.width, wallpaper.height, wallpaper.padding);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed attaching wallpaper; clearing", e);
            if (!wallpaper.wallpaperUpdating) {
                bindWallpaperComponentLocked(null, DEBUG, DEBUG, wallpaper, null);
            }
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
        if (this.mContext.checkCallingOrSelfPermission(permission) != 0) {
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + permission);
        }
    }

    private static JournaledFile makeJournaledFile(int userId) {
        String base = new File(getWallpaperDir(userId), WALLPAPER_INFO).getAbsolutePath();
        return new JournaledFile(new File(base), new File(base + ".tmp"));
    }

    private void saveSettingsLocked(WallpaperData wallpaper) {
        FileOutputStream stream;
        JournaledFile journal = makeJournaledFile(wallpaper.userId);
        FileOutputStream stream2 = null;
        try {
            stream = new FileOutputStream(journal.chooseForWrite(), DEBUG);
        } catch (IOException e) {
        }
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(stream, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, "wp");
            fastXmlSerializer.attribute(null, "width", Integer.toString(wallpaper.width));
            fastXmlSerializer.attribute(null, "height", Integer.toString(wallpaper.height));
            if (wallpaper.padding.left != 0) {
                fastXmlSerializer.attribute(null, "paddingLeft", Integer.toString(wallpaper.padding.left));
            }
            if (wallpaper.padding.top != 0) {
                fastXmlSerializer.attribute(null, "paddingTop", Integer.toString(wallpaper.padding.top));
            }
            if (wallpaper.padding.right != 0) {
                fastXmlSerializer.attribute(null, "paddingRight", Integer.toString(wallpaper.padding.right));
            }
            if (wallpaper.padding.bottom != 0) {
                fastXmlSerializer.attribute(null, "paddingBottom", Integer.toString(wallpaper.padding.bottom));
            }
            fastXmlSerializer.attribute(null, "name", wallpaper.name);
            if (wallpaper.wallpaperComponent != null && !wallpaper.wallpaperComponent.equals(this.mImageWallpaper)) {
                fastXmlSerializer.attribute(null, "component", wallpaper.wallpaperComponent.flattenToShortString());
            }
            fastXmlSerializer.endTag(null, "wp");
            fastXmlSerializer.endDocument();
            stream.close();
            journal.commit();
        } catch (IOException e2) {
            stream2 = stream;
            if (stream2 != null) {
                try {
                    stream2.close();
                } catch (IOException e3) {
                }
            }
            journal.rollback();
        }
    }

    private void migrateFromOld() {
        File oldWallpaper = new File("/data/data/com.android.settings/files/wallpaper");
        File oldInfo = new File("/data/system/wallpaper_info.xml");
        if (oldWallpaper.exists()) {
            File newWallpaper = new File(getWallpaperDir(0), WALLPAPER);
            oldWallpaper.renameTo(newWallpaper);
        }
        if (oldInfo.exists()) {
            File newInfo = new File(getWallpaperDir(0), WALLPAPER_INFO);
            oldInfo.renameTo(newInfo);
        }
    }

    private int getAttributeInt(XmlPullParser parser, String name, int defValue) {
        String value = parser.getAttributeValue(null, name);
        if (value == null) {
            return defValue;
        }
        int defValue2 = Integer.parseInt(value);
        return defValue2;
    }

    private void loadSettingsLocked(int userId) {
        FileInputStream stream;
        int type;
        JournaledFile journal = makeJournaledFile(userId);
        FileInputStream stream2 = null;
        File file = journal.chooseForRead();
        if (!file.exists()) {
            migrateFromOld();
        }
        WallpaperData wallpaper = this.mWallpaperMap.get(userId);
        if (wallpaper == null) {
            wallpaper = new WallpaperData(userId);
            this.mWallpaperMap.put(userId, wallpaper);
        }
        boolean success = DEBUG;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            e = e2;
        } catch (IndexOutOfBoundsException e3) {
            e = e3;
        } catch (NullPointerException e4) {
            e = e4;
        } catch (NumberFormatException e5) {
            e = e5;
        } catch (XmlPullParserException e6) {
            e = e6;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, null);
            do {
                type = parser.next();
                if (type == 2) {
                    String tag = parser.getName();
                    if ("wp".equals(tag)) {
                        wallpaper.width = Integer.parseInt(parser.getAttributeValue(null, "width"));
                        wallpaper.height = Integer.parseInt(parser.getAttributeValue(null, "height"));
                        wallpaper.padding.left = getAttributeInt(parser, "paddingLeft", 0);
                        wallpaper.padding.top = getAttributeInt(parser, "paddingTop", 0);
                        wallpaper.padding.right = getAttributeInt(parser, "paddingRight", 0);
                        wallpaper.padding.bottom = getAttributeInt(parser, "paddingBottom", 0);
                        wallpaper.name = parser.getAttributeValue(null, "name");
                        String comp = parser.getAttributeValue(null, "component");
                        wallpaper.nextWallpaperComponent = comp != null ? ComponentName.unflattenFromString(comp) : null;
                        if (wallpaper.nextWallpaperComponent == null || "android".equals(wallpaper.nextWallpaperComponent.getPackageName())) {
                            wallpaper.nextWallpaperComponent = this.mImageWallpaper;
                        }
                    }
                }
            } while (type != 1);
            success = true;
            stream2 = stream;
        } catch (FileNotFoundException e7) {
            stream2 = stream;
            Slog.w(TAG, "no current wallpaper -- first boot?");
        } catch (IOException e8) {
            e = e8;
            stream2 = stream;
            Slog.w(TAG, "failed parsing " + file + " " + e);
        } catch (IndexOutOfBoundsException e9) {
            e = e9;
            stream2 = stream;
            Slog.w(TAG, "failed parsing " + file + " " + e);
        } catch (NullPointerException e10) {
            e = e10;
            stream2 = stream;
            Slog.w(TAG, "failed parsing " + file + " " + e);
        } catch (NumberFormatException e11) {
            e = e11;
            stream2 = stream;
            Slog.w(TAG, "failed parsing " + file + " " + e);
        } catch (XmlPullParserException e12) {
            e = e12;
            stream2 = stream;
            Slog.w(TAG, "failed parsing " + file + " " + e);
        }
        if (stream2 != null) {
            try {
                stream2.close();
            } catch (IOException e13) {
            }
        }
        if (!success) {
            wallpaper.width = -1;
            wallpaper.height = -1;
            wallpaper.padding.set(0, 0, 0, 0);
            wallpaper.name = "";
        }
        int baseSize = getMaximumSizeDimension();
        if (wallpaper.width < baseSize) {
            wallpaper.width = baseSize;
        }
        if (wallpaper.height < baseSize) {
            wallpaper.height = baseSize;
        }
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
            loadSettingsLocked(0);
            wallpaper = this.mWallpaperMap.get(0);
            if (wallpaper.nextWallpaperComponent != null && !wallpaper.nextWallpaperComponent.equals(this.mImageWallpaper)) {
                if (!bindWallpaperComponentLocked(wallpaper.nextWallpaperComponent, DEBUG, DEBUG, wallpaper, null)) {
                    bindWallpaperComponentLocked(null, DEBUG, DEBUG, wallpaper, null);
                }
                success = true;
            } else {
                if ("".equals(wallpaper.name)) {
                    success = true;
                } else {
                    success = restoreNamedResourceLocked(wallpaper);
                }
                if (success) {
                    bindWallpaperComponentLocked(wallpaper.nextWallpaperComponent, DEBUG, DEBUG, wallpaper, null);
                }
            }
        }
        if (!success) {
            Slog.e(TAG, "Failed to restore wallpaper: '" + wallpaper.name + "'");
            wallpaper.name = "";
            getWallpaperDir(0).delete();
        }
        synchronized (this.mLock) {
            saveSettingsLocked(wallpaper);
        }
    }

    boolean restoreNamedResourceLocked(WallpaperData wallpaper) throws Throwable {
        if (wallpaper.name.length() > 4 && "res:".equals(wallpaper.name.substring(0, 4))) {
            String resName = wallpaper.name.substring(4);
            int colon = resName.indexOf(58);
            String pkg = colon > 0 ? resName.substring(0, colon) : null;
            int slash = resName.lastIndexOf(47);
            String ident = slash > 0 ? resName.substring(slash + 1) : null;
            String type = null;
            if (colon > 0 && slash > 0 && slash - colon > 1) {
                type = resName.substring(colon + 1, slash);
            }
            if (pkg != null && ident != null && type != null) {
                int resId = -1;
                InputStream res = null;
                FileOutputStream fos = null;
                try {
                    try {
                        Context c = this.mContext.createPackageContext(pkg, 4);
                        Resources r = c.getResources();
                        resId = r.getIdentifier(resName, null, null);
                        if (resId == 0) {
                            Slog.e(TAG, "couldn't resolve identifier pkg=" + pkg + " type=" + type + " ident=" + ident);
                            if (0 != 0) {
                                try {
                                    res.close();
                                } catch (IOException e) {
                                }
                            }
                            if (0 == 0) {
                                return DEBUG;
                            }
                            FileUtils.sync(null);
                            try {
                                fos.close();
                                return DEBUG;
                            } catch (IOException e2) {
                                return DEBUG;
                            }
                        }
                        res = r.openRawResource(resId);
                        if (wallpaper.wallpaperFile.exists()) {
                            wallpaper.wallpaperFile.delete();
                        }
                        FileOutputStream fos2 = new FileOutputStream(wallpaper.wallpaperFile);
                        try {
                            byte[] buffer = new byte[32768];
                            while (true) {
                                int amt = res.read(buffer);
                                if (amt <= 0) {
                                    break;
                                }
                                fos2.write(buffer, 0, amt);
                            }
                            Slog.v(TAG, "Restored wallpaper: " + resName);
                            if (res != null) {
                                try {
                                    res.close();
                                } catch (IOException e3) {
                                }
                            }
                            if (fos2 == null) {
                                return true;
                            }
                            FileUtils.sync(fos2);
                            try {
                                fos2.close();
                                return true;
                            } catch (IOException e4) {
                                return true;
                            }
                        } catch (PackageManager.NameNotFoundException e5) {
                            fos = fos2;
                            Slog.e(TAG, "Package name " + pkg + " not found");
                            if (res != null) {
                                try {
                                    res.close();
                                } catch (IOException e6) {
                                }
                            }
                            if (fos != null) {
                                FileUtils.sync(fos);
                                try {
                                    fos.close();
                                } catch (IOException e7) {
                                }
                            }
                            return DEBUG;
                        } catch (Resources.NotFoundException e8) {
                            fos = fos2;
                            Slog.e(TAG, "Resource not found: " + resId);
                            if (res != null) {
                                try {
                                    res.close();
                                } catch (IOException e9) {
                                }
                            }
                            if (fos != null) {
                                FileUtils.sync(fos);
                                try {
                                    fos.close();
                                } catch (IOException e10) {
                                }
                            }
                            return DEBUG;
                        } catch (IOException e11) {
                            e = e11;
                            fos = fos2;
                            Slog.e(TAG, "IOException while restoring wallpaper ", e);
                            if (res != null) {
                                try {
                                    res.close();
                                } catch (IOException e12) {
                                }
                            }
                            if (fos != null) {
                                FileUtils.sync(fos);
                                try {
                                    fos.close();
                                } catch (IOException e13) {
                                }
                            }
                            return DEBUG;
                        } catch (Throwable th) {
                            th = th;
                            fos = fos2;
                            if (res != null) {
                                try {
                                    res.close();
                                } catch (IOException e14) {
                                }
                            }
                            if (fos == null) {
                                throw th;
                            }
                            FileUtils.sync(fos);
                            try {
                                fos.close();
                                throw th;
                            } catch (IOException e15) {
                                throw th;
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (PackageManager.NameNotFoundException e16) {
                } catch (Resources.NotFoundException e17) {
                } catch (IOException e18) {
                    e = e18;
                }
            }
        }
        return DEBUG;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump wallpaper service from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mLock) {
            pw.println("Current Wallpaper Service state:");
            for (int i = 0; i < this.mWallpaperMap.size(); i++) {
                WallpaperData wallpaper = this.mWallpaperMap.valueAt(i);
                pw.println(" User " + wallpaper.userId + ":");
                pw.print("  mWidth=");
                pw.print(wallpaper.width);
                pw.print(" mHeight=");
                pw.println(wallpaper.height);
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
        }
    }
}
