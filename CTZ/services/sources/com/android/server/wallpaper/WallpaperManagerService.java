package com.android.server.wallpaper;

import android.R;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
import android.app.PendingIntent;
import android.app.UserSwitchObserver;
import android.app.WallpaperColors;
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
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.system.ErrnoException;
import android.system.Os;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.WindowManager;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.server.BatteryService;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.wallpaper.WallpaperManagerService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WallpaperManagerService extends IWallpaperManager.Stub implements IWallpaperManagerService {
    static final boolean DEBUG = false;
    static final boolean DEBUG_LIVE = true;
    private static final int MAX_BITMAP_SIZE = 104857600;
    static final int MAX_WALLPAPER_COMPONENT_LOG_LENGTH = 128;
    static final long MIN_WALLPAPER_CRASH_TIME = 10000;
    static final String TAG = "WallpaperManagerService";
    final AppOpsManager mAppOpsManager;
    final Context mContext;
    final ComponentName mDefaultWallpaperComponent;
    final ComponentName mImageWallpaper;
    boolean mInAmbientMode;
    IWallpaperManagerCallback mKeyguardListener;
    WallpaperData mLastWallpaper;
    int mThemeMode;
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
    final SparseArray<Boolean> mUserRestorecon = new SparseArray<>();
    int mCurrentUserId = -10000;
    boolean mShuttingDown = false;
    final IWindowManager mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    final IPackageManager mIPackageManager = AppGlobals.getPackageManager();
    final MyPackageMonitor mMonitor = new MyPackageMonitor();
    final SparseArray<RemoteCallbackList<IWallpaperManagerCallback>> mColorsChangedListeners = new SparseArray<>();

    public static class Lifecycle extends SystemService {
        private IWallpaperManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            try {
                this.mService = (IWallpaperManagerService) Class.forName(getContext().getResources().getString(R.string.app_category_maps)).getConstructor(Context.class).newInstance(getContext());
                publishBinderService(WallpaperManagerService.WALLPAPER_CROP, this.mService);
            } catch (Exception e) {
                Slog.wtf(WallpaperManagerService.TAG, "Failed to instantiate WallpaperManagerService", e);
            }
        }

        @Override
        public void onBootPhase(int i) {
            if (this.mService != null) {
                this.mService.onBootPhase(i);
            }
        }

        @Override
        public void onUnlockUser(int i) {
            if (this.mService != null) {
                this.mService.onUnlockUser(i);
            }
        }
    }

    private class WallpaperObserver extends FileObserver {
        final int mUserId;
        final WallpaperData mWallpaper;
        final File mWallpaperDir;
        final File mWallpaperFile;
        final File mWallpaperLockFile;

        public WallpaperObserver(WallpaperData wallpaperData) {
            super(WallpaperManagerService.getWallpaperDir(wallpaperData.userId).getAbsolutePath(), 1672);
            this.mUserId = wallpaperData.userId;
            this.mWallpaperDir = WallpaperManagerService.getWallpaperDir(wallpaperData.userId);
            this.mWallpaper = wallpaperData;
            this.mWallpaperFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER);
            this.mWallpaperLockFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER_LOCK_ORIG);
        }

        private WallpaperData dataForEvent(boolean z, boolean z2) {
            WallpaperData wallpaperData;
            synchronized (WallpaperManagerService.this.mLock) {
                if (z2) {
                    try {
                        wallpaperData = WallpaperManagerService.this.mLockWallpaperMap.get(this.mUserId);
                    } catch (Throwable th) {
                        throw th;
                    }
                } else {
                    wallpaperData = null;
                }
                if (wallpaperData == null) {
                    wallpaperData = WallpaperManagerService.this.mWallpaperMap.get(this.mUserId);
                }
            }
            return wallpaperData != null ? wallpaperData : this.mWallpaper;
        }

        @Override
        public void onEvent(int i, String str) throws Throwable {
            Object obj;
            int i2;
            WallpaperData wallpaperData;
            if (str == null) {
                return;
            }
            int i3 = 0;
            Object obj2 = i == 128 ? 1 : 0;
            boolean z = i == 8 || obj2 != 0;
            File file = new File(this.mWallpaperDir, str);
            boolean zEquals = this.mWallpaperFile.equals(file);
            boolean zEquals2 = this.mWallpaperLockFile.equals(file);
            WallpaperData wallpaperDataDataForEvent = dataForEvent(zEquals, zEquals2);
            if (obj2 != 0 && zEquals2) {
                SELinux.restorecon(file);
                WallpaperManagerService.this.notifyLockWallpaperChanged();
                WallpaperManagerService.this.notifyWallpaperColorsChanged(wallpaperDataDataForEvent, 2);
                return;
            }
            Object obj3 = WallpaperManagerService.this.mLock;
            synchronized (obj3) {
                try {
                    if (zEquals || zEquals2) {
                        try {
                            WallpaperManagerService.this.notifyCallbacksLocked(wallpaperDataDataForEvent);
                            if ((wallpaperDataDataForEvent.wallpaperComponent == null || i != 8 || wallpaperDataDataForEvent.imageWallpaperPending) && z) {
                                SELinux.restorecon(file);
                                if (obj2 != 0) {
                                    WallpaperManagerService.this.loadSettingsLocked(wallpaperDataDataForEvent.userId, true);
                                }
                                WallpaperManagerService.this.generateCrop(wallpaperDataDataForEvent);
                                wallpaperDataDataForEvent.imageWallpaperPending = false;
                                if (zEquals) {
                                    obj = obj3;
                                    i2 = 2;
                                    wallpaperData = wallpaperDataDataForEvent;
                                    WallpaperManagerService.this.bindWallpaperComponentLocked(WallpaperManagerService.this.mImageWallpaper, true, false, wallpaperDataDataForEvent, null);
                                    i3 = 1;
                                } else {
                                    obj = obj3;
                                    i2 = 2;
                                    wallpaperData = wallpaperDataDataForEvent;
                                }
                                if (zEquals2 || (i2 & wallpaperData.whichPending) != 0) {
                                    if (!zEquals2) {
                                        WallpaperManagerService.this.mLockWallpaperMap.remove(wallpaperData.userId);
                                    }
                                    WallpaperManagerService.this.notifyLockWallpaperChanged();
                                    i3 |= 2;
                                }
                                WallpaperManagerService.this.saveSettingsLocked(wallpaperData.userId);
                                if (wallpaperData.setComplete != null) {
                                    try {
                                        wallpaperData.setComplete.onWallpaperChanged();
                                    } catch (RemoteException e) {
                                    }
                                }
                            } else {
                                obj = obj3;
                                wallpaperData = wallpaperDataDataForEvent;
                            }
                        } catch (Throwable th) {
                            th = th;
                            obj2 = obj3;
                            throw th;
                        }
                    }
                    if (i3 != 0) {
                        WallpaperManagerService.this.notifyWallpaperColorsChanged(wallpaperData, i3);
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }

    private class ThemeSettingsObserver extends ContentObserver {
        public ThemeSettingsObserver(Handler handler) {
            super(handler);
        }

        public void startObserving(Context context) {
            context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("theme_mode"), false, this);
        }

        public void stopObserving(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean z) {
            WallpaperManagerService.this.onThemeSettingsChanged();
        }
    }

    private boolean needUpdateLocked(WallpaperColors wallpaperColors, int i) {
        if (wallpaperColors == null || i == this.mThemeMode) {
            return false;
        }
        boolean z = true;
        boolean z2 = (wallpaperColors.getColorHints() & 2) != 0;
        switch (i) {
            case 0:
                z = this.mThemeMode == 1 ? z2 : !z2;
                this.mThemeMode = i;
                break;
            case 1:
                if (this.mThemeMode == 0) {
                }
                this.mThemeMode = i;
                break;
            case 2:
                if (this.mThemeMode == 0) {
                    z = !z2;
                }
                this.mThemeMode = i;
                break;
            default:
                Slog.w(TAG, "unkonwn theme mode " + i);
                break;
        }
        return false;
    }

    void onThemeSettingsChanged() {
        synchronized (this.mLock) {
            WallpaperData wallpaperData = this.mWallpaperMap.get(this.mCurrentUserId);
            if (needUpdateLocked(wallpaperData.primaryColors, Settings.Secure.getInt(this.mContext.getContentResolver(), "theme_mode", 0))) {
                if (wallpaperData != null) {
                    notifyWallpaperColorsChanged(wallpaperData, 1);
                }
            }
        }
    }

    void notifyLockWallpaperChanged() {
        IWallpaperManagerCallback iWallpaperManagerCallback = this.mKeyguardListener;
        if (iWallpaperManagerCallback != null) {
            try {
                iWallpaperManagerCallback.onWallpaperChanged();
            } catch (RemoteException e) {
            }
        }
    }

    private void notifyWallpaperColorsChanged(WallpaperData wallpaperData, int i) {
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> remoteCallbackList = this.mColorsChangedListeners.get(wallpaperData.userId);
            RemoteCallbackList<IWallpaperManagerCallback> remoteCallbackList2 = this.mColorsChangedListeners.get(-1);
            if (emptyCallbackList(remoteCallbackList) && emptyCallbackList(remoteCallbackList2)) {
                return;
            }
            boolean z = wallpaperData.primaryColors == null;
            notifyColorListeners(wallpaperData.primaryColors, i, wallpaperData.userId);
            if (z) {
                extractColors(wallpaperData);
                synchronized (this.mLock) {
                    if (wallpaperData.primaryColors == null) {
                        return;
                    }
                    notifyColorListeners(wallpaperData.primaryColors, i, wallpaperData.userId);
                }
            }
        }
    }

    private static <T extends IInterface> boolean emptyCallbackList(RemoteCallbackList<T> remoteCallbackList) {
        return remoteCallbackList == null || remoteCallbackList.getRegisteredCallbackCount() == 0;
    }

    private void notifyColorListeners(WallpaperColors wallpaperColors, int i, int i2) {
        IWallpaperManagerCallback iWallpaperManagerCallback;
        int i3;
        WallpaperColors themeColorsLocked;
        ArrayList arrayList = new ArrayList();
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> remoteCallbackList = this.mColorsChangedListeners.get(i2);
            RemoteCallbackList<IWallpaperManagerCallback> remoteCallbackList2 = this.mColorsChangedListeners.get(-1);
            iWallpaperManagerCallback = this.mKeyguardListener;
            if (remoteCallbackList != null) {
                int iBeginBroadcast = remoteCallbackList.beginBroadcast();
                for (int i4 = 0; i4 < iBeginBroadcast; i4++) {
                    arrayList.add(remoteCallbackList.getBroadcastItem(i4));
                }
                remoteCallbackList.finishBroadcast();
            }
            if (remoteCallbackList2 != null) {
                int iBeginBroadcast2 = remoteCallbackList2.beginBroadcast();
                for (int i5 = 0; i5 < iBeginBroadcast2; i5++) {
                    arrayList.add(remoteCallbackList2.getBroadcastItem(i5));
                }
                remoteCallbackList2.finishBroadcast();
            }
            themeColorsLocked = getThemeColorsLocked(wallpaperColors);
        }
        int size = arrayList.size();
        for (i3 = 0; i3 < size; i3++) {
            try {
                ((IWallpaperManagerCallback) arrayList.get(i3)).onWallpaperColorsChanged(themeColorsLocked, i, i2);
            } catch (RemoteException e) {
            }
        }
        if (iWallpaperManagerCallback != null) {
            try {
                iWallpaperManagerCallback.onWallpaperColorsChanged(themeColorsLocked, i, i2);
            } catch (RemoteException e2) {
            }
        }
    }

    private void extractColors(WallpaperData wallpaperData) {
        WallpaperColors wallpaperColorsFromBitmap;
        String absolutePath;
        int i;
        Bitmap bitmapDecodeFile;
        synchronized (this.mLock) {
            wallpaperColorsFromBitmap = null;
            if ((this.mImageWallpaper.equals(wallpaperData.wallpaperComponent) || wallpaperData.wallpaperComponent == null) && wallpaperData.cropFile != null && wallpaperData.cropFile.exists()) {
                absolutePath = wallpaperData.cropFile.getAbsolutePath();
            } else {
                absolutePath = null;
            }
            i = wallpaperData.wallpaperId;
        }
        if (absolutePath != null && (bitmapDecodeFile = BitmapFactory.decodeFile(absolutePath)) != null) {
            wallpaperColorsFromBitmap = WallpaperColors.fromBitmap(bitmapDecodeFile);
            bitmapDecodeFile.recycle();
        }
        if (wallpaperColorsFromBitmap == null) {
            Slog.w(TAG, "Cannot extract colors because wallpaper could not be read.");
            return;
        }
        synchronized (this.mLock) {
            if (wallpaperData.wallpaperId == i) {
                wallpaperData.primaryColors = wallpaperColorsFromBitmap;
                saveSettingsLocked(wallpaperData.userId);
            } else {
                Slog.w(TAG, "Not setting primary colors since wallpaper changed");
            }
        }
    }

    private WallpaperColors getThemeColorsLocked(WallpaperColors wallpaperColors) {
        boolean z;
        if (wallpaperColors == null) {
            Slog.w(TAG, "Cannot get theme colors because WallpaperColors is null.");
            return null;
        }
        int colorHints = wallpaperColors.getColorHints();
        if ((colorHints & 2) == 0) {
            z = false;
        } else {
            z = true;
        }
        if (this.mThemeMode == 0 || ((this.mThemeMode == 1 && !z) || (this.mThemeMode == 2 && z))) {
            return wallpaperColors;
        }
        WallpaperColors wallpaperColors2 = new WallpaperColors(wallpaperColors.getPrimaryColor(), wallpaperColors.getSecondaryColor(), wallpaperColors.getTertiaryColor());
        if (this.mThemeMode == 1) {
            colorHints &= -3;
        } else if (this.mThemeMode == 2) {
            colorHints |= 2;
        }
        wallpaperColors2.setColorHints(colorHints);
        return wallpaperColors2;
    }

    private void generateCrop(WallpaperData wallpaperData) throws Throwable {
        boolean z;
        FileOutputStream fileOutputStream;
        int i;
        int i2;
        Rect rect;
        float fHeight;
        Bitmap bitmapDecodeRegion;
        BufferedOutputStream bufferedOutputStream;
        Rect rect2 = new Rect(wallpaperData.cropHint);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(wallpaperData.wallpaperFile.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            Slog.w(TAG, "Invalid wallpaper data");
        } else {
            if (rect2.isEmpty()) {
                rect2.top = 0;
                rect2.left = 0;
                rect2.right = options.outWidth;
                rect2.bottom = options.outHeight;
            } else {
                rect2.offset(rect2.right > options.outWidth ? options.outWidth - rect2.right : 0, rect2.bottom > options.outHeight ? options.outHeight - rect2.bottom : 0);
                if (rect2.left < 0) {
                    rect2.left = 0;
                }
                if (rect2.top < 0) {
                    rect2.top = 0;
                }
                if (options.outHeight > rect2.height() || options.outWidth > rect2.width()) {
                    z = true;
                }
                boolean z2 = wallpaperData.height == rect2.height() || rect2.height() > GLHelper.getMaxTextureSize() || rect2.width() > GLHelper.getMaxTextureSize();
                if (!z || z2) {
                    BufferedOutputStream bufferedOutputStream2 = null;
                    try {
                        BitmapRegionDecoder bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance(wallpaperData.wallpaperFile.getAbsolutePath(), false);
                        i = 1;
                        while (true) {
                            i2 = 2 * i;
                            if (i2 <= rect2.height() / wallpaperData.height) {
                                break;
                            } else {
                                i = i2;
                            }
                        }
                        options.inSampleSize = i;
                        options.inJustDecodeBounds = false;
                        rect = new Rect(rect2);
                        rect.scale(1.0f / options.inSampleSize);
                        fHeight = wallpaperData.height / rect.height();
                        rect.height();
                        if (((int) (rect.width() * fHeight)) > GLHelper.getMaxTextureSize()) {
                            int i3 = (int) (wallpaperData.height / fHeight);
                            int i4 = (int) (wallpaperData.width / fHeight);
                            rect.set(rect2);
                            rect.left += (rect2.width() - i4) / 2;
                            rect.top += (rect2.height() - i3) / 2;
                            rect.right = rect.left + i4;
                            rect.bottom = rect.top + i3;
                            rect2.set(rect);
                            rect.scale(1.0f / options.inSampleSize);
                        }
                        int iHeight = (int) (rect.height() * fHeight);
                        int iWidth = (int) (rect.width() * fHeight);
                        bitmapDecodeRegion = bitmapRegionDecoderNewInstance.decodeRegion(rect2, options);
                        bitmapRegionDecoderNewInstance.recycle();
                        if (bitmapDecodeRegion != null) {
                            Slog.e(TAG, "Could not decode new wallpaper");
                            fileOutputStream = null;
                        } else {
                            Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapDecodeRegion, iWidth, iHeight, true);
                            if (bitmapCreateScaledBitmap.getByteCount() > MAX_BITMAP_SIZE) {
                                throw new RuntimeException("Too large bitmap, limit=104857600");
                            }
                            fileOutputStream = new FileOutputStream(wallpaperData.cropFile);
                            try {
                                bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 32768);
                            } catch (Exception e) {
                            } catch (Throwable th) {
                                th = th;
                            }
                            try {
                                bitmapCreateScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferedOutputStream);
                                bufferedOutputStream.flush();
                                zCopyFile = true;
                                bufferedOutputStream2 = bufferedOutputStream;
                            } catch (Exception e2) {
                                bufferedOutputStream2 = bufferedOutputStream;
                            } catch (Throwable th2) {
                                th = th2;
                                bufferedOutputStream2 = bufferedOutputStream;
                                IoUtils.closeQuietly(bufferedOutputStream2);
                                IoUtils.closeQuietly(fileOutputStream);
                                throw th;
                            }
                        }
                    } catch (Exception e3) {
                        fileOutputStream = null;
                    } catch (Throwable th3) {
                        th = th3;
                        fileOutputStream = null;
                    }
                    IoUtils.closeQuietly(bufferedOutputStream2);
                    IoUtils.closeQuietly(fileOutputStream);
                } else {
                    zCopyFile = ((long) ((options.outWidth * options.outHeight) * 4)) < 104857600 ? FileUtils.copyFile(wallpaperData.wallpaperFile, wallpaperData.cropFile) : false;
                    if (!zCopyFile) {
                        wallpaperData.cropFile.delete();
                    }
                }
            }
            z = false;
            if (wallpaperData.height == rect2.height()) {
                if (z) {
                    BufferedOutputStream bufferedOutputStream22 = null;
                    BitmapRegionDecoder bitmapRegionDecoderNewInstance2 = BitmapRegionDecoder.newInstance(wallpaperData.wallpaperFile.getAbsolutePath(), false);
                    i = 1;
                    while (true) {
                        i2 = 2 * i;
                        if (i2 <= rect2.height() / wallpaperData.height) {
                        }
                        i = i2;
                    }
                    options.inSampleSize = i;
                    options.inJustDecodeBounds = false;
                    rect = new Rect(rect2);
                    rect.scale(1.0f / options.inSampleSize);
                    fHeight = wallpaperData.height / rect.height();
                    rect.height();
                    if (((int) (rect.width() * fHeight)) > GLHelper.getMaxTextureSize()) {
                    }
                    int iHeight2 = (int) (rect.height() * fHeight);
                    int iWidth2 = (int) (rect.width() * fHeight);
                    bitmapDecodeRegion = bitmapRegionDecoderNewInstance2.decodeRegion(rect2, options);
                    bitmapRegionDecoderNewInstance2.recycle();
                    if (bitmapDecodeRegion != null) {
                    }
                    IoUtils.closeQuietly(bufferedOutputStream22);
                    IoUtils.closeQuietly(fileOutputStream);
                }
            }
        }
        if (!zCopyFile) {
            Slog.e(TAG, "Unable to apply new wallpaper");
            wallpaperData.cropFile.delete();
        }
        if (wallpaperData.cropFile.exists()) {
            SELinux.restorecon(wallpaperData.cropFile.getAbsoluteFile());
        }
    }

    static class WallpaperData {
        boolean allowBackup;
        WallpaperConnection connection;
        final File cropFile;
        boolean imageWallpaperPending;
        long lastDiedTime;
        ComponentName nextWallpaperComponent;
        WallpaperColors primaryColors;
        IWallpaperManagerCallback setComplete;
        ThemeSettingsObserver themeSettingsObserver;
        int userId;
        ComponentName wallpaperComponent;
        final File wallpaperFile;
        int wallpaperId;
        WallpaperObserver wallpaperObserver;
        boolean wallpaperUpdating;
        int whichPending;
        String name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        private RemoteCallbackList<IWallpaperManagerCallback> callbacks = new RemoteCallbackList<>();
        int width = -1;
        int height = -1;
        final Rect cropHint = new Rect(0, 0, 0, 0);
        final Rect padding = new Rect(0, 0, 0, 0);

        WallpaperData(int i, String str, String str2) {
            this.userId = i;
            File wallpaperDir = WallpaperManagerService.getWallpaperDir(i);
            this.wallpaperFile = new File(wallpaperDir, str);
            this.cropFile = new File(wallpaperDir, str2);
        }

        boolean cropExists() {
            return this.cropFile.exists();
        }

        boolean sourceExists() {
            return this.wallpaperFile.exists();
        }
    }

    int makeWallpaperIdLocked() {
        do {
            this.mWallpaperId++;
        } while (this.mWallpaperId == 0);
        return this.mWallpaperId;
    }

    class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        private static final long WALLPAPER_RECONNECT_TIMEOUT_MS = 10000;
        IWallpaperEngine mEngine;
        final WallpaperInfo mInfo;
        IRemoteCallback mReply;
        IWallpaperService mService;
        WallpaperData mWallpaper;
        final Binder mToken = new Binder();
        boolean mDimensionsChanged = false;
        boolean mPaddingChanged = false;
        private Runnable mResetRunnable = new Runnable() {
            @Override
            public final void run() {
                WallpaperManagerService.WallpaperConnection.lambda$new$0(this.f$0);
            }
        };

        public static void lambda$new$0(WallpaperConnection wallpaperConnection) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mShuttingDown) {
                    Slog.i(WallpaperManagerService.TAG, "Ignoring relaunch timeout during shutdown");
                    return;
                }
                if (!wallpaperConnection.mWallpaper.wallpaperUpdating && wallpaperConnection.mWallpaper.userId == WallpaperManagerService.this.mCurrentUserId) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper reconnect timed out for " + wallpaperConnection.mWallpaper.wallpaperComponent + ", reverting to built-in wallpaper!");
                    WallpaperManagerService.this.clearWallpaperLocked(true, 1, wallpaperConnection.mWallpaper.userId, null);
                }
            }
        }

        public WallpaperConnection(WallpaperInfo wallpaperInfo, WallpaperData wallpaperData) {
            this.mInfo = wallpaperInfo;
            this.mWallpaper = wallpaperData;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection == this) {
                    this.mService = IWallpaperService.Stub.asInterface(iBinder);
                    WallpaperManagerService.this.attachServiceLocked(this, this.mWallpaper);
                    WallpaperManagerService.this.saveSettingsLocked(this.mWallpaper.userId);
                    FgThread.getHandler().removeCallbacks(this.mResetRunnable);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (WallpaperManagerService.this.mLock) {
                Slog.w(WallpaperManagerService.TAG, "Wallpaper service gone: " + componentName);
                if (!Objects.equals(componentName, this.mWallpaper.wallpaperComponent)) {
                    Slog.e(WallpaperManagerService.TAG, "Does not match expected wallpaper component " + this.mWallpaper.wallpaperComponent);
                }
                this.mService = null;
                this.mEngine = null;
                if (this.mWallpaper.connection == this && !this.mWallpaper.wallpaperUpdating) {
                    WallpaperManagerService.this.mContext.getMainThreadHandler().postDelayed(new Runnable() {
                        @Override
                        public final void run() {
                            WallpaperManagerService.WallpaperConnection wallpaperConnection = this.f$0;
                            wallpaperConnection.lambda$onServiceDisconnected$1(wallpaperConnection);
                        }
                    }, 1000L);
                }
            }
        }

        public void scheduleTimeoutLocked() {
            Handler handler = FgThread.getHandler();
            handler.removeCallbacks(this.mResetRunnable);
            handler.postDelayed(this.mResetRunnable, 10000L);
            Slog.i(WallpaperManagerService.TAG, "Started wallpaper reconnect timeout for " + this.mWallpaper.wallpaperComponent);
        }

        private void lambda$onServiceDisconnected$1(ServiceConnection serviceConnection) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (serviceConnection == this.mWallpaper.connection) {
                    ComponentName componentName = this.mWallpaper.wallpaperComponent;
                    if (!this.mWallpaper.wallpaperUpdating && this.mWallpaper.userId == WallpaperManagerService.this.mCurrentUserId && !Objects.equals(WallpaperManagerService.this.mDefaultWallpaperComponent, componentName) && !Objects.equals(WallpaperManagerService.this.mImageWallpaper, componentName)) {
                        if (this.mWallpaper.lastDiedTime != 0 && this.mWallpaper.lastDiedTime + 10000 > SystemClock.uptimeMillis()) {
                            Slog.w(WallpaperManagerService.TAG, "Reverting to built-in wallpaper!");
                            WallpaperManagerService.this.clearWallpaperLocked(true, 1, this.mWallpaper.userId, null);
                        } else {
                            this.mWallpaper.lastDiedTime = SystemClock.uptimeMillis();
                            WallpaperManagerService.this.clearWallpaperComponentLocked(this.mWallpaper);
                            if (WallpaperManagerService.this.bindWallpaperComponentLocked(componentName, false, false, this.mWallpaper, null)) {
                                this.mWallpaper.connection.scheduleTimeoutLocked();
                            } else {
                                Slog.w(WallpaperManagerService.TAG, "Reverting to built-in wallpaper!");
                                WallpaperManagerService.this.clearWallpaperLocked(true, 1, this.mWallpaper.userId, null);
                            }
                        }
                        String strFlattenToString = componentName.flattenToString();
                        EventLog.writeEvent(EventLogTags.WP_WALLPAPER_CRASHED, strFlattenToString.substring(0, Math.min(strFlattenToString.length(), 128)));
                    }
                } else {
                    Slog.i(WallpaperManagerService.TAG, "Wallpaper changed during disconnect tracking; ignoring");
                }
            }
        }

        public void onWallpaperColorsChanged(WallpaperColors wallpaperColors) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mImageWallpaper.equals(this.mWallpaper.wallpaperComponent)) {
                    return;
                }
                this.mWallpaper.primaryColors = wallpaperColors;
                int i = 1;
                if (WallpaperManagerService.this.mLockWallpaperMap.get(this.mWallpaper.userId) == null) {
                    i = 3;
                }
                if (i != 0) {
                    WallpaperManagerService.this.notifyWallpaperColorsChanged(this.mWallpaper, i);
                }
            }
        }

        public void attachEngine(IWallpaperEngine iWallpaperEngine) {
            synchronized (WallpaperManagerService.this.mLock) {
                this.mEngine = iWallpaperEngine;
                if (this.mDimensionsChanged) {
                    try {
                        this.mEngine.setDesiredSize(this.mWallpaper.width, this.mWallpaper.height);
                    } catch (RemoteException e) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper dimensions", e);
                    }
                    this.mDimensionsChanged = false;
                    if (!this.mPaddingChanged) {
                        try {
                            this.mEngine.setDisplayPadding(this.mWallpaper.padding);
                        } catch (RemoteException e2) {
                            Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper padding", e2);
                        }
                        this.mPaddingChanged = false;
                        if (this.mInfo == null && this.mInfo.getSupportsAmbientMode()) {
                            try {
                                this.mEngine.setInAmbientMode(WallpaperManagerService.this.mInAmbientMode, false);
                            } catch (RemoteException e3) {
                                Slog.w(WallpaperManagerService.TAG, "Failed to set ambient mode state", e3);
                            }
                            this.mEngine.requestWallpaperColors();
                        } else {
                            try {
                                this.mEngine.requestWallpaperColors();
                            } catch (RemoteException e4) {
                                Slog.w(WallpaperManagerService.TAG, "Failed to request wallpaper colors", e4);
                            }
                        }
                    } else if (this.mInfo == null) {
                        this.mEngine.requestWallpaperColors();
                    }
                } else if (!this.mPaddingChanged) {
                }
            }
        }

        public void engineShown(IWallpaperEngine iWallpaperEngine) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mReply != null) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        this.mReply.sendResult((Bundle) null);
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                    this.mReply = null;
                }
            }
        }

        public ParcelFileDescriptor setWallpaper(String str) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection != this) {
                    return null;
                }
                return WallpaperManagerService.this.updateWallpaperBitmapLocked(str, this.mWallpaper, null);
            }
        }
    }

    class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        public void onPackageUpdateFinished(String str, int i) {
            ComponentName componentName;
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaperData = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaperData != null && (componentName = wallpaperData.wallpaperComponent) != null && componentName.getPackageName().equals(str)) {
                    Slog.i(WallpaperManagerService.TAG, "Wallpaper " + componentName + " update has finished");
                    wallpaperData.wallpaperUpdating = false;
                    WallpaperManagerService.this.clearWallpaperComponentLocked(wallpaperData);
                    if (!WallpaperManagerService.this.bindWallpaperComponentLocked(componentName, false, false, wallpaperData, null)) {
                        Slog.w(WallpaperManagerService.TAG, "Wallpaper " + componentName + " no longer available; reverting to default");
                        WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaperData.userId, null);
                    }
                }
            }
        }

        public void onPackageModified(String str) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaperData = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaperData != null) {
                    if (wallpaperData.wallpaperComponent != null && wallpaperData.wallpaperComponent.getPackageName().equals(str)) {
                        doPackagesChangedLocked(true, wallpaperData);
                    }
                }
            }
        }

        public void onPackageUpdateStarted(String str, int i) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaperData = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaperData != null && wallpaperData.wallpaperComponent != null && wallpaperData.wallpaperComponent.getPackageName().equals(str)) {
                    Slog.i(WallpaperManagerService.TAG, "Wallpaper service " + wallpaperData.wallpaperComponent + " is updating");
                    wallpaperData.wallpaperUpdating = true;
                    if (wallpaperData.connection != null) {
                        FgThread.getHandler().removeCallbacks(wallpaperData.connection.mResetRunnable);
                    }
                }
            }
        }

        public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return false;
                }
                WallpaperData wallpaperData = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                return wallpaperData != null ? false | doPackagesChangedLocked(z, wallpaperData) : false;
            }
        }

        public void onSomePackagesChanged() {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaperData = WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaperData != null) {
                    doPackagesChangedLocked(true, wallpaperData);
                }
            }
        }

        boolean doPackagesChangedLocked(boolean z, WallpaperData wallpaperData) throws Throwable {
            boolean z2;
            int iIsPackageDisappearing;
            int iIsPackageDisappearing2;
            if (wallpaperData.wallpaperComponent == null || !((iIsPackageDisappearing2 = isPackageDisappearing(wallpaperData.wallpaperComponent.getPackageName())) == 3 || iIsPackageDisappearing2 == 2)) {
                z2 = false;
            } else {
                if (z) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper uninstalled, removing: " + wallpaperData.wallpaperComponent);
                    WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaperData.userId, null);
                }
                z2 = true;
            }
            if (wallpaperData.nextWallpaperComponent != null && ((iIsPackageDisappearing = isPackageDisappearing(wallpaperData.nextWallpaperComponent.getPackageName())) == 3 || iIsPackageDisappearing == 2)) {
                wallpaperData.nextWallpaperComponent = null;
            }
            if (wallpaperData.wallpaperComponent != null && isPackageModified(wallpaperData.wallpaperComponent.getPackageName())) {
                try {
                    WallpaperManagerService.this.mContext.getPackageManager().getServiceInfo(wallpaperData.wallpaperComponent, 786432);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper component gone, removing: " + wallpaperData.wallpaperComponent);
                    WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaperData.userId, null);
                }
            }
            if (wallpaperData.nextWallpaperComponent != null && isPackageModified(wallpaperData.nextWallpaperComponent.getPackageName())) {
                try {
                    WallpaperManagerService.this.mContext.getPackageManager().getServiceInfo(wallpaperData.nextWallpaperComponent, 786432);
                } catch (PackageManager.NameNotFoundException e2) {
                    wallpaperData.nextWallpaperComponent = null;
                }
            }
            return z2;
        }
    }

    public WallpaperManagerService(Context context) {
        this.mContext = context;
        this.mImageWallpaper = ComponentName.unflattenFromString(context.getResources().getString(R.string.config_inputEventCompatProcessorOverrideClassName));
        this.mDefaultWallpaperComponent = WallpaperManager.getDefaultWallpaperComponent(context);
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
    }

    void initialize() throws Throwable {
        this.mMonitor.register(this.mContext, null, UserHandle.ALL, true);
        getWallpaperDir(0).mkdirs();
        loadSettingsLocked(0, false);
        getWallpaperSafeLocked(0, 1);
    }

    private static File getWallpaperDir(int i) {
        return Environment.getUserSystemDirectory(i);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        for (int i = 0; i < this.mWallpaperMap.size(); i++) {
            this.mWallpaperMap.valueAt(i).wallpaperObserver.stopWatching();
        }
    }

    void systemReady() throws Throwable {
        initialize();
        WallpaperData wallpaperData = this.mWallpaperMap.get(0);
        if (this.mImageWallpaper.equals(wallpaperData.nextWallpaperComponent)) {
            if (!wallpaperData.cropExists()) {
                generateCrop(wallpaperData);
            }
            if (!wallpaperData.cropExists()) {
                clearWallpaperLocked(false, 1, 0, null);
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                    WallpaperManagerService.this.onRemoveUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
                }
            }
        }, intentFilter);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
                    synchronized (WallpaperManagerService.this.mLock) {
                        WallpaperManagerService.this.mShuttingDown = true;
                    }
                }
            }
        }, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"));
        try {
            ActivityManager.getService().registerUserSwitchObserver(new UserSwitchObserver() {
                public void onUserSwitching(int i, IRemoteCallback iRemoteCallback) {
                    WallpaperManagerService.this.switchUser(i, iRemoteCallback);
                }
            }, TAG);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
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

    void stopObserver(WallpaperData wallpaperData) {
        if (wallpaperData != null) {
            if (wallpaperData.wallpaperObserver != null) {
                wallpaperData.wallpaperObserver.stopWatching();
                wallpaperData.wallpaperObserver = null;
            }
            if (wallpaperData.themeSettingsObserver != null) {
                wallpaperData.themeSettingsObserver.stopObserving(this.mContext);
                wallpaperData.themeSettingsObserver = null;
            }
        }
    }

    void stopObserversLocked(int i) {
        stopObserver(this.mWallpaperMap.get(i));
        stopObserver(this.mLockWallpaperMap.get(i));
        this.mWallpaperMap.remove(i);
        this.mLockWallpaperMap.remove(i);
    }

    @Override
    public void onBootPhase(int i) throws Throwable {
        if (i == 550) {
            systemReady();
        } else if (i == 600) {
            switchUser(0, null);
        }
    }

    @Override
    public void onUnlockUser(final int i) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == i) {
                if (this.mWaitingForUnlock) {
                    switchWallpaper(getWallpaperSafeLocked(i, 1), null);
                }
                if (this.mUserRestorecon.get(i) != Boolean.TRUE) {
                    this.mUserRestorecon.put(i, Boolean.TRUE);
                    BackgroundThread.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            File wallpaperDir = WallpaperManagerService.getWallpaperDir(i);
                            for (String str : WallpaperManagerService.sPerUserFiles) {
                                File file = new File(wallpaperDir, str);
                                if (file.exists()) {
                                    SELinux.restorecon(file);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    void onRemoveUser(int i) {
        if (i < 1) {
            return;
        }
        File wallpaperDir = getWallpaperDir(i);
        synchronized (this.mLock) {
            stopObserversLocked(i);
            for (String str : sPerUserFiles) {
                new File(wallpaperDir, str).delete();
            }
            this.mUserRestorecon.remove(i);
        }
    }

    void switchUser(int i, IRemoteCallback iRemoteCallback) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == i) {
                return;
            }
            this.mCurrentUserId = i;
            final WallpaperData wallpaperSafeLocked = getWallpaperSafeLocked(i, 1);
            final WallpaperData wallpaperData = this.mLockWallpaperMap.get(i);
            if (wallpaperData == null) {
                wallpaperData = wallpaperSafeLocked;
            }
            if (wallpaperSafeLocked.wallpaperObserver == null) {
                wallpaperSafeLocked.wallpaperObserver = new WallpaperObserver(wallpaperSafeLocked);
                wallpaperSafeLocked.wallpaperObserver.startWatching();
            }
            if (wallpaperSafeLocked.themeSettingsObserver == null) {
                wallpaperSafeLocked.themeSettingsObserver = new ThemeSettingsObserver(null);
                wallpaperSafeLocked.themeSettingsObserver.startObserving(this.mContext);
            }
            this.mThemeMode = Settings.Secure.getInt(this.mContext.getContentResolver(), "theme_mode", 0);
            switchWallpaper(wallpaperSafeLocked, iRemoteCallback);
            FgThread.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    WallpaperManagerService.lambda$switchUser$0(this.f$0, wallpaperSafeLocked, wallpaperData);
                }
            });
        }
    }

    public static void lambda$switchUser$0(WallpaperManagerService wallpaperManagerService, WallpaperData wallpaperData, WallpaperData wallpaperData2) {
        wallpaperManagerService.notifyWallpaperColorsChanged(wallpaperData, 1);
        wallpaperManagerService.notifyWallpaperColorsChanged(wallpaperData2, 2);
    }

    void switchWallpaper(WallpaperData wallpaperData, IRemoteCallback iRemoteCallback) {
        ServiceInfo serviceInfo;
        synchronized (this.mLock) {
            this.mWaitingForUnlock = false;
            ComponentName componentName = wallpaperData.wallpaperComponent != null ? wallpaperData.wallpaperComponent : wallpaperData.nextWallpaperComponent;
            if (!bindWallpaperComponentLocked(componentName, true, false, wallpaperData, iRemoteCallback)) {
                try {
                    serviceInfo = this.mIPackageManager.getServiceInfo(componentName, DumpState.DUMP_DOMAIN_PREFERRED, wallpaperData.userId);
                } catch (RemoteException e) {
                    serviceInfo = null;
                }
                if (serviceInfo == null) {
                    Slog.w(TAG, "Failure starting previous wallpaper; clearing");
                    clearWallpaperLocked(false, 1, wallpaperData.userId, iRemoteCallback);
                } else {
                    Slog.w(TAG, "Wallpaper isn't direct boot aware; using fallback until unlocked");
                    wallpaperData.wallpaperComponent = wallpaperData.nextWallpaperComponent;
                    WallpaperData wallpaperData2 = new WallpaperData(wallpaperData.userId, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                    ensureSaneWallpaperData(wallpaperData2);
                    bindWallpaperComponentLocked(this.mImageWallpaper, true, false, wallpaperData2, iRemoteCallback);
                    this.mWaitingForUnlock = true;
                }
            }
        }
    }

    public void clearWallpaper(String str, int i, int i2) {
        WallpaperData wallpaperData;
        checkPermission("android.permission.SET_WALLPAPER");
        if (!isWallpaperSupported(str) || !isSetWallpaperAllowed(str)) {
            return;
        }
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i2, false, true, "clearWallpaper", null);
        synchronized (this.mLock) {
            clearWallpaperLocked(false, i, iHandleIncomingUser, null);
            wallpaperData = i == 2 ? this.mLockWallpaperMap.get(iHandleIncomingUser) : null;
            if (i == 1 || wallpaperData == null) {
                wallpaperData = this.mWallpaperMap.get(iHandleIncomingUser);
            }
        }
        if (wallpaperData != null) {
            notifyWallpaperColorsChanged(wallpaperData, i);
        }
    }

    void clearWallpaperLocked(boolean z, int i, int i2, IRemoteCallback iRemoteCallback) throws Throwable {
        WallpaperData wallpaperData;
        if (i != 1 && i != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper to clear");
        }
        if (i == 2) {
            wallpaperData = this.mLockWallpaperMap.get(i2);
            if (wallpaperData == null) {
                return;
            }
        } else {
            wallpaperData = this.mWallpaperMap.get(i2);
            if (wallpaperData == null) {
                loadSettingsLocked(i2, false);
                wallpaperData = this.mWallpaperMap.get(i2);
            }
        }
        if (wallpaperData == null) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (wallpaperData.wallpaperFile.exists()) {
                wallpaperData.wallpaperFile.delete();
                wallpaperData.cropFile.delete();
                if (i == 2) {
                    this.mLockWallpaperMap.remove(i2);
                    IWallpaperManagerCallback iWallpaperManagerCallback = this.mKeyguardListener;
                    if (iWallpaperManagerCallback != null) {
                        try {
                            iWallpaperManagerCallback.onWallpaperChanged();
                        } catch (RemoteException e) {
                        }
                    }
                    saveSettingsLocked(i2);
                    return;
                }
            }
            try {
                wallpaperData.primaryColors = null;
                wallpaperData.imageWallpaperPending = false;
                if (i2 != this.mCurrentUserId) {
                    return;
                }
                if (bindWallpaperComponentLocked(z ? this.mImageWallpaper : null, true, false, wallpaperData, iRemoteCallback)) {
                    return;
                } else {
                    e = null;
                }
            } catch (IllegalArgumentException e2) {
                e = e2;
            }
            Slog.e(TAG, "Default wallpaper component not found!", e);
            clearWallpaperComponentLocked(wallpaperData);
            if (iRemoteCallback != null) {
                try {
                    iRemoteCallback.sendResult((Bundle) null);
                } catch (RemoteException e3) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean hasNamedWallpaper(String str) {
        synchronized (this.mLock) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = ((UserManager) this.mContext.getSystemService("user")).getUsers();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                for (UserInfo userInfo : users) {
                    if (!userInfo.isManagedProfile()) {
                        WallpaperData wallpaperData = this.mWallpaperMap.get(userInfo.id);
                        if (wallpaperData == null) {
                            loadSettingsLocked(userInfo.id, false);
                            wallpaperData = this.mWallpaperMap.get(userInfo.id);
                        }
                        if (wallpaperData != null && str.equals(wallpaperData.name)) {
                            return true;
                        }
                    }
                }
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
    }

    private Point getDefaultDisplaySize() {
        Point point = new Point();
        ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRealSize(point);
        return point;
    }

    public void setDimensionHints(int i, int i2, String str) throws RemoteException {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        if (!isWallpaperSupported(str)) {
            return;
        }
        int iMin = Math.min(i, GLHelper.getMaxTextureSize());
        int iMin2 = Math.min(i2, GLHelper.getMaxTextureSize());
        synchronized (this.mLock) {
            int callingUserId = UserHandle.getCallingUserId();
            WallpaperData wallpaperSafeLocked = getWallpaperSafeLocked(callingUserId, 1);
            if (iMin <= 0 || iMin2 <= 0) {
                throw new IllegalArgumentException("width and height must be > 0");
            }
            Point defaultDisplaySize = getDefaultDisplaySize();
            int iMax = Math.max(iMin, defaultDisplaySize.x);
            int iMax2 = Math.max(iMin2, defaultDisplaySize.y);
            if (iMax != wallpaperSafeLocked.width || iMax2 != wallpaperSafeLocked.height) {
                wallpaperSafeLocked.width = iMax;
                wallpaperSafeLocked.height = iMax2;
                saveSettingsLocked(callingUserId);
                if (this.mCurrentUserId != callingUserId) {
                    return;
                }
                if (wallpaperSafeLocked.connection != null) {
                    if (wallpaperSafeLocked.connection.mEngine != null) {
                        try {
                            wallpaperSafeLocked.connection.mEngine.setDesiredSize(iMax, iMax2);
                        } catch (RemoteException e) {
                        }
                        notifyCallbacksLocked(wallpaperSafeLocked);
                    } else if (wallpaperSafeLocked.connection.mService != null) {
                        wallpaperSafeLocked.connection.mDimensionsChanged = true;
                    }
                }
            }
        }
    }

    public int getWidthHint() throws RemoteException {
        synchronized (this.mLock) {
            WallpaperData wallpaperData = this.mWallpaperMap.get(UserHandle.getCallingUserId());
            if (wallpaperData != null) {
                return wallpaperData.width;
            }
            return 0;
        }
    }

    public int getHeightHint() throws RemoteException {
        synchronized (this.mLock) {
            WallpaperData wallpaperData = this.mWallpaperMap.get(UserHandle.getCallingUserId());
            if (wallpaperData != null) {
                return wallpaperData.height;
            }
            return 0;
        }
    }

    public void setDisplayPadding(Rect rect, String str) {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        if (!isWallpaperSupported(str)) {
            return;
        }
        synchronized (this.mLock) {
            int callingUserId = UserHandle.getCallingUserId();
            WallpaperData wallpaperSafeLocked = getWallpaperSafeLocked(callingUserId, 1);
            if (rect.left < 0 || rect.top < 0 || rect.right < 0 || rect.bottom < 0) {
                throw new IllegalArgumentException("padding must be positive: " + rect);
            }
            if (!rect.equals(wallpaperSafeLocked.padding)) {
                wallpaperSafeLocked.padding.set(rect);
                saveSettingsLocked(callingUserId);
                if (this.mCurrentUserId != callingUserId) {
                    return;
                }
                if (wallpaperSafeLocked.connection != null) {
                    if (wallpaperSafeLocked.connection.mEngine != null) {
                        try {
                            wallpaperSafeLocked.connection.mEngine.setDisplayPadding(rect);
                        } catch (RemoteException e) {
                        }
                        notifyCallbacksLocked(wallpaperSafeLocked);
                    } else if (wallpaperSafeLocked.connection.mService != null) {
                        wallpaperSafeLocked.connection.mPaddingChanged = true;
                    }
                }
            }
        }
    }

    private void enforceCallingOrSelfPermissionAndAppOp(String str, String str2, int i, String str3) {
        this.mContext.enforceCallingOrSelfPermission(str, str3);
        String strPermissionToOp = AppOpsManager.permissionToOp(str);
        if (strPermissionToOp != null && this.mAppOpsManager.noteOp(strPermissionToOp, i, str2) != 0) {
            throw new SecurityException(str3 + ": " + str2 + " is not allowed to " + str);
        }
    }

    public ParcelFileDescriptor getWallpaper(String str, IWallpaperManagerCallback iWallpaperManagerCallback, int i, Bundle bundle, int i2) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_WALLPAPER_INTERNAL") != 0) {
            enforceCallingOrSelfPermissionAndAppOp("android.permission.READ_EXTERNAL_STORAGE", str, Binder.getCallingUid(), "read wallpaper");
        }
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i2, false, true, "getWallpaper", null);
        if (i != 1 && i != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper to read");
        }
        synchronized (this.mLock) {
            try {
                WallpaperData wallpaperData = (i == 2 ? this.mLockWallpaperMap : this.mWallpaperMap).get(iHandleIncomingUser);
                if (wallpaperData == null) {
                    return null;
                }
                if (bundle != null) {
                    try {
                        bundle.putInt("width", wallpaperData.width);
                        bundle.putInt("height", wallpaperData.height);
                    } catch (FileNotFoundException e) {
                        Slog.w(TAG, "Error getting wallpaper", e);
                        return null;
                    }
                }
                if (iWallpaperManagerCallback != null) {
                    wallpaperData.callbacks.register(iWallpaperManagerCallback);
                }
                if (!wallpaperData.cropFile.exists()) {
                    return null;
                }
                return ParcelFileDescriptor.open(wallpaperData.cropFile, 268435456);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public WallpaperInfo getWallpaperInfo(int i) {
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, true, "getWallpaperInfo", null);
        synchronized (this.mLock) {
            WallpaperData wallpaperData = this.mWallpaperMap.get(iHandleIncomingUser);
            if (wallpaperData != null && wallpaperData.connection != null) {
                return wallpaperData.connection.mInfo;
            }
            return null;
        }
    }

    public int getWallpaperIdForUser(int i, int i2) {
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i2, false, true, "getWallpaperIdForUser", null);
        if (i != 1 && i != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper");
        }
        SparseArray<WallpaperData> sparseArray = i == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
        synchronized (this.mLock) {
            WallpaperData wallpaperData = sparseArray.get(iHandleIncomingUser);
            if (wallpaperData != null) {
                return wallpaperData.wallpaperId;
            }
            return -1;
        }
    }

    public void registerWallpaperColorsCallback(IWallpaperManagerCallback iWallpaperManagerCallback, int i) {
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, true, true, "registerWallpaperColorsCallback", null);
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> remoteCallbackList = this.mColorsChangedListeners.get(iHandleIncomingUser);
            if (remoteCallbackList == null) {
                remoteCallbackList = new RemoteCallbackList<>();
                this.mColorsChangedListeners.put(iHandleIncomingUser, remoteCallbackList);
            }
            remoteCallbackList.register(iWallpaperManagerCallback);
        }
    }

    public void unregisterWallpaperColorsCallback(IWallpaperManagerCallback iWallpaperManagerCallback, int i) {
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, true, true, "unregisterWallpaperColorsCallback", null);
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> remoteCallbackList = this.mColorsChangedListeners.get(iHandleIncomingUser);
            if (remoteCallbackList != null) {
                remoteCallbackList.unregister(iWallpaperManagerCallback);
            }
        }
    }

    public void setInAmbientMode(boolean z, boolean z2) {
        IWallpaperEngine iWallpaperEngine;
        synchronized (this.mLock) {
            this.mInAmbientMode = z;
            WallpaperData wallpaperData = this.mWallpaperMap.get(this.mCurrentUserId);
            if (wallpaperData != null && wallpaperData.connection != null && wallpaperData.connection.mInfo != null && wallpaperData.connection.mInfo.getSupportsAmbientMode()) {
                iWallpaperEngine = wallpaperData.connection.mEngine;
            } else {
                iWallpaperEngine = null;
            }
        }
        if (iWallpaperEngine != null) {
            try {
                iWallpaperEngine.setInAmbientMode(z, z2);
            } catch (RemoteException e) {
            }
        }
    }

    public boolean setLockWallpaperCallback(IWallpaperManagerCallback iWallpaperManagerCallback) {
        checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW");
        synchronized (this.mLock) {
            this.mKeyguardListener = iWallpaperManagerCallback;
        }
        return true;
    }

    public WallpaperColors getWallpaperColors(int i, int i2) throws RemoteException {
        WallpaperData wallpaperData;
        WallpaperColors themeColorsLocked;
        boolean z = true;
        if (i != 2 && i != 1) {
            throw new IllegalArgumentException("which should be either FLAG_LOCK or FLAG_SYSTEM");
        }
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i2, false, true, "getWallpaperColors", null);
        synchronized (this.mLock) {
            if (i == 2) {
                try {
                    wallpaperData = this.mLockWallpaperMap.get(iHandleIncomingUser);
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                wallpaperData = null;
            }
            if (wallpaperData == null) {
                wallpaperData = this.mWallpaperMap.get(iHandleIncomingUser);
            }
            if (wallpaperData == null) {
                return null;
            }
            if (wallpaperData.primaryColors != null) {
                z = false;
            }
            if (z) {
                extractColors(wallpaperData);
            }
            synchronized (this.mLock) {
                themeColorsLocked = getThemeColorsLocked(wallpaperData.primaryColors);
            }
            return themeColorsLocked;
        }
    }

    public ParcelFileDescriptor setWallpaper(String str, String str2, Rect rect, boolean z, Bundle bundle, int i, IWallpaperManagerCallback iWallpaperManagerCallback, int i2) {
        ParcelFileDescriptor parcelFileDescriptorUpdateWallpaperBitmapLocked;
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), i2, false, true, "changing wallpaper", null);
        checkPermission("android.permission.SET_WALLPAPER");
        if ((i & 3) == 0) {
            Slog.e(TAG, "Must specify a valid wallpaper category to set");
            throw new IllegalArgumentException("Must specify a valid wallpaper category to set");
        }
        if (!isWallpaperSupported(str2) || !isSetWallpaperAllowed(str2)) {
            return null;
        }
        if (rect == null) {
            rect = new Rect(0, 0, 0, 0);
        } else if (rect.isEmpty() || rect.left < 0 || rect.top < 0) {
            throw new IllegalArgumentException("Invalid crop rect supplied: " + rect);
        }
        synchronized (this.mLock) {
            if (i == 1) {
                try {
                    if (this.mLockWallpaperMap.get(iHandleIncomingUser) == null) {
                        migrateSystemToLockWallpaperLocked(iHandleIncomingUser);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            WallpaperData wallpaperSafeLocked = getWallpaperSafeLocked(iHandleIncomingUser, i);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                parcelFileDescriptorUpdateWallpaperBitmapLocked = updateWallpaperBitmapLocked(str, wallpaperSafeLocked, bundle);
                if (parcelFileDescriptorUpdateWallpaperBitmapLocked != null) {
                    wallpaperSafeLocked.imageWallpaperPending = true;
                    wallpaperSafeLocked.whichPending = i;
                    wallpaperSafeLocked.setComplete = iWallpaperManagerCallback;
                    wallpaperSafeLocked.cropHint.set(rect);
                    wallpaperSafeLocked.allowBackup = z;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return parcelFileDescriptorUpdateWallpaperBitmapLocked;
    }

    private void migrateSystemToLockWallpaperLocked(int i) {
        WallpaperData wallpaperData = this.mWallpaperMap.get(i);
        if (wallpaperData == null) {
            return;
        }
        WallpaperData wallpaperData2 = new WallpaperData(i, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
        wallpaperData2.wallpaperId = wallpaperData.wallpaperId;
        wallpaperData2.cropHint.set(wallpaperData.cropHint);
        wallpaperData2.width = wallpaperData.width;
        wallpaperData2.height = wallpaperData.height;
        wallpaperData2.allowBackup = wallpaperData.allowBackup;
        wallpaperData2.primaryColors = wallpaperData.primaryColors;
        try {
            Os.rename(wallpaperData.wallpaperFile.getAbsolutePath(), wallpaperData2.wallpaperFile.getAbsolutePath());
            Os.rename(wallpaperData.cropFile.getAbsolutePath(), wallpaperData2.cropFile.getAbsolutePath());
            this.mLockWallpaperMap.put(i, wallpaperData2);
        } catch (ErrnoException e) {
            Slog.e(TAG, "Can't migrate system wallpaper: " + e.getMessage());
            wallpaperData2.wallpaperFile.delete();
            wallpaperData2.cropFile.delete();
        }
    }

    ParcelFileDescriptor updateWallpaperBitmapLocked(String str, WallpaperData wallpaperData, Bundle bundle) {
        if (str == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        try {
            File wallpaperDir = getWallpaperDir(wallpaperData.userId);
            if (!wallpaperDir.exists()) {
                wallpaperDir.mkdir();
                FileUtils.setPermissions(wallpaperDir.getPath(), 505, -1, -1);
            }
            ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(wallpaperData.wallpaperFile, 1006632960);
            if (!SELinux.restorecon(wallpaperData.wallpaperFile)) {
                return null;
            }
            wallpaperData.name = str;
            wallpaperData.wallpaperId = makeWallpaperIdLocked();
            if (bundle != null) {
                bundle.putInt("android.service.wallpaper.extra.ID", wallpaperData.wallpaperId);
            }
            wallpaperData.primaryColors = null;
            return parcelFileDescriptorOpen;
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "Error setting wallpaper", e);
            return null;
        }
    }

    public void setWallpaperComponentChecked(ComponentName componentName, String str, int i) {
        if (isWallpaperSupported(str) && isSetWallpaperAllowed(str)) {
            setWallpaperComponent(componentName, i);
        }
    }

    public void setWallpaperComponent(ComponentName componentName) {
        setWallpaperComponent(componentName, UserHandle.getCallingUserId());
    }

    private void setWallpaperComponent(ComponentName componentName, int i) {
        WallpaperData wallpaperData;
        int i2;
        boolean z;
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), i, false, true, "changing live wallpaper", null);
        checkPermission("android.permission.SET_WALLPAPER_COMPONENT");
        synchronized (this.mLock) {
            wallpaperData = this.mWallpaperMap.get(iHandleIncomingUser);
            if (wallpaperData == null) {
                throw new IllegalStateException("Wallpaper not yet initialized for user " + iHandleIncomingUser);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            if (this.mImageWallpaper.equals(wallpaperData.wallpaperComponent) && this.mLockWallpaperMap.get(iHandleIncomingUser) == null) {
                migrateSystemToLockWallpaperLocked(iHandleIncomingUser);
            }
            if (this.mLockWallpaperMap.get(iHandleIncomingUser) == null) {
                i2 = 3;
            } else {
                i2 = 1;
            }
            z = false;
            try {
                wallpaperData.imageWallpaperPending = false;
                boolean zChangingToSame = changingToSame(componentName, wallpaperData);
                if (bindWallpaperComponentLocked(componentName, false, true, wallpaperData, null)) {
                    if (!zChangingToSame) {
                        wallpaperData.primaryColors = null;
                    }
                    wallpaperData.wallpaperId = makeWallpaperIdLocked();
                    notifyCallbacksLocked(wallpaperData);
                    z = true;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        if (z) {
            notifyWallpaperColorsChanged(wallpaperData, i2);
        }
    }

    private boolean changingToSame(ComponentName componentName, WallpaperData wallpaperData) {
        if (wallpaperData.connection != null) {
            return wallpaperData.wallpaperComponent == null ? componentName == null : wallpaperData.wallpaperComponent.equals(componentName);
        }
        return false;
    }

    boolean bindWallpaperComponentLocked(ComponentName componentName, boolean z, boolean z2, WallpaperData wallpaperData, IRemoteCallback iRemoteCallback) {
        ComponentName componentName2 = componentName;
        Slog.v(TAG, "bindWallpaperComponentLocked: componentName=" + componentName2);
        if (!z && changingToSame(componentName2, wallpaperData)) {
            return true;
        }
        if (componentName2 == null) {
            try {
                ComponentName componentName3 = this.mDefaultWallpaperComponent;
                if (componentName3 == null) {
                    try {
                        componentName2 = this.mImageWallpaper;
                        Slog.v(TAG, "No default component; using image wallpaper");
                    } catch (RemoteException e) {
                        e = e;
                        componentName2 = componentName3;
                        String str = "Remote exception for " + componentName2 + "\n" + e;
                        if (z2) {
                            throw new IllegalArgumentException(str);
                        }
                        Slog.w(TAG, str);
                        return false;
                    }
                } else {
                    componentName2 = componentName3;
                }
            } catch (RemoteException e2) {
                e = e2;
            }
        }
        int i = wallpaperData.userId;
        ServiceInfo serviceInfo = this.mIPackageManager.getServiceInfo(componentName2, 4224, i);
        if (serviceInfo == null) {
            Slog.w(TAG, "Attempted wallpaper " + componentName2 + " is unavailable");
            return false;
        }
        if (!"android.permission.BIND_WALLPAPER".equals(serviceInfo.permission)) {
            String str2 = "Selected service does not require android.permission.BIND_WALLPAPER: " + componentName2;
            if (z2) {
                throw new SecurityException(str2);
            }
            Slog.w(TAG, str2);
            return false;
        }
        WallpaperInfo wallpaperInfo = null;
        Intent intent = new Intent("android.service.wallpaper.WallpaperService");
        if (componentName2 != null && !componentName2.equals(this.mImageWallpaper)) {
            List list = this.mIPackageManager.queryIntentServices(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 128, i).getList();
            int i2 = 0;
            while (true) {
                if (i2 >= list.size()) {
                    break;
                }
                ServiceInfo serviceInfo2 = ((ResolveInfo) list.get(i2)).serviceInfo;
                if (!serviceInfo2.name.equals(serviceInfo.name) || !serviceInfo2.packageName.equals(serviceInfo.packageName)) {
                    i2++;
                } else {
                    try {
                        break;
                    } catch (IOException e3) {
                        if (z2) {
                            throw new IllegalArgumentException(e3);
                        }
                        Slog.w(TAG, e3);
                        return false;
                    } catch (XmlPullParserException e4) {
                        if (z2) {
                            throw new IllegalArgumentException(e4);
                        }
                        Slog.w(TAG, e4);
                        return false;
                    }
                }
            }
            if (wallpaperInfo == null) {
                String str3 = "Selected service is not a wallpaper: " + componentName2;
                if (z2) {
                    throw new SecurityException(str3);
                }
                Slog.w(TAG, str3);
                return false;
            }
        }
        WallpaperConnection wallpaperConnection = new WallpaperConnection(wallpaperInfo, wallpaperData);
        intent.setComponent(componentName2);
        intent.putExtra("android.intent.extra.client_label", R.string.network_switch_metered_detail);
        intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(this.mContext, 0, Intent.createChooser(new Intent("android.intent.action.SET_WALLPAPER"), this.mContext.getText(R.string.accessibility_shortcut_menu_item_status_on)), 67108864, null, new UserHandle(i)));
        if (!this.mContext.bindServiceAsUser(intent, wallpaperConnection, 570425345, new UserHandle(i))) {
            String str4 = "Unable to bind service: " + componentName2;
            if (z2) {
                throw new IllegalArgumentException(str4);
            }
            Slog.w(TAG, str4);
            return false;
        }
        if (wallpaperData.userId == this.mCurrentUserId && this.mLastWallpaper != null) {
            detachWallpaperLocked(this.mLastWallpaper);
        }
        wallpaperData.wallpaperComponent = componentName2;
        wallpaperData.connection = wallpaperConnection;
        wallpaperConnection.mReply = iRemoteCallback;
        try {
            if (wallpaperData.userId == this.mCurrentUserId) {
                this.mIWindowManager.addWindowToken(wallpaperConnection.mToken, 2013, 0);
                this.mLastWallpaper = wallpaperData;
            }
        } catch (RemoteException e5) {
        }
        return true;
    }

    void detachWallpaperLocked(WallpaperData wallpaperData) {
        if (wallpaperData.connection != null) {
            if (wallpaperData.connection.mReply != null) {
                try {
                    wallpaperData.connection.mReply.sendResult((Bundle) null);
                } catch (RemoteException e) {
                }
                wallpaperData.connection.mReply = null;
            }
            if (wallpaperData.connection.mEngine != null) {
                try {
                    wallpaperData.connection.mEngine.destroy();
                } catch (RemoteException e2) {
                }
            }
            this.mContext.unbindService(wallpaperData.connection);
            try {
                this.mIWindowManager.removeWindowToken(wallpaperData.connection.mToken, 0);
            } catch (RemoteException e3) {
            }
            wallpaperData.connection.mService = null;
            wallpaperData.connection.mEngine = null;
            wallpaperData.connection = null;
        }
    }

    void clearWallpaperComponentLocked(WallpaperData wallpaperData) {
        wallpaperData.wallpaperComponent = null;
        detachWallpaperLocked(wallpaperData);
    }

    void attachServiceLocked(WallpaperConnection wallpaperConnection, WallpaperData wallpaperData) {
        try {
            wallpaperConnection.mService.attach(wallpaperConnection, wallpaperConnection.mToken, 2013, false, wallpaperData.width, wallpaperData.height, wallpaperData.padding);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed attaching wallpaper; clearing", e);
            if (!wallpaperData.wallpaperUpdating) {
                bindWallpaperComponentLocked(null, false, false, wallpaperData, null);
            }
        }
    }

    private void notifyCallbacksLocked(WallpaperData wallpaperData) {
        int iBeginBroadcast = wallpaperData.callbacks.beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                wallpaperData.callbacks.getBroadcastItem(i).onWallpaperChanged();
            } catch (RemoteException e) {
            }
        }
        wallpaperData.callbacks.finishBroadcast();
        this.mContext.sendBroadcastAsUser(new Intent("android.intent.action.WALLPAPER_CHANGED"), new UserHandle(this.mCurrentUserId));
    }

    private void checkPermission(String str) {
        if (this.mContext.checkCallingOrSelfPermission(str) != 0) {
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + str);
        }
    }

    public boolean isWallpaperSupported(String str) {
        return this.mAppOpsManager.checkOpNoThrow(48, Binder.getCallingUid(), str) == 0;
    }

    public boolean isSetWallpaperAllowed(String str) {
        if (!Arrays.asList(this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid())).contains(str)) {
            return false;
        }
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
        if (devicePolicyManager.isDeviceOwnerApp(str) || devicePolicyManager.isProfileOwnerApp(str)) {
            return true;
        }
        return !((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_set_wallpaper");
    }

    public boolean isWallpaperBackupEligible(int i, int i2) {
        WallpaperData wallpaperData;
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call isWallpaperBackupEligible");
        }
        if (i == 2) {
            wallpaperData = this.mLockWallpaperMap.get(i2);
        } else {
            wallpaperData = this.mWallpaperMap.get(i2);
        }
        if (wallpaperData != null) {
            return wallpaperData.allowBackup;
        }
        return false;
    }

    private static JournaledFile makeJournaledFile(int i) {
        String absolutePath = new File(getWallpaperDir(i), WALLPAPER_INFO).getAbsolutePath();
        return new JournaledFile(new File(absolutePath), new File(absolutePath + ".tmp"));
    }

    private void saveSettingsLocked(int i) {
        BufferedOutputStream bufferedOutputStream;
        XmlSerializer fastXmlSerializer;
        FileOutputStream fileOutputStream;
        JournaledFile journaledFileMakeJournaledFile = makeJournaledFile(i);
        try {
            fastXmlSerializer = new FastXmlSerializer();
            fileOutputStream = new FileOutputStream(journaledFileMakeJournaledFile.chooseForWrite(), false);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        } catch (IOException e) {
            bufferedOutputStream = null;
        }
        try {
            fastXmlSerializer.setOutput(bufferedOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            WallpaperData wallpaperData = this.mWallpaperMap.get(i);
            if (wallpaperData != null) {
                writeWallpaperAttributes(fastXmlSerializer, "wp", wallpaperData);
            }
            WallpaperData wallpaperData2 = this.mLockWallpaperMap.get(i);
            if (wallpaperData2 != null) {
                writeWallpaperAttributes(fastXmlSerializer, "kwp", wallpaperData2);
            }
            fastXmlSerializer.endDocument();
            bufferedOutputStream.flush();
            FileUtils.sync(fileOutputStream);
            bufferedOutputStream.close();
            journaledFileMakeJournaledFile.commit();
        } catch (IOException e2) {
            IoUtils.closeQuietly(bufferedOutputStream);
            journaledFileMakeJournaledFile.rollback();
        }
    }

    private void writeWallpaperAttributes(XmlSerializer xmlSerializer, String str, WallpaperData wallpaperData) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.startTag(null, str);
        xmlSerializer.attribute(null, "id", Integer.toString(wallpaperData.wallpaperId));
        xmlSerializer.attribute(null, "width", Integer.toString(wallpaperData.width));
        xmlSerializer.attribute(null, "height", Integer.toString(wallpaperData.height));
        xmlSerializer.attribute(null, "cropLeft", Integer.toString(wallpaperData.cropHint.left));
        xmlSerializer.attribute(null, "cropTop", Integer.toString(wallpaperData.cropHint.top));
        xmlSerializer.attribute(null, "cropRight", Integer.toString(wallpaperData.cropHint.right));
        xmlSerializer.attribute(null, "cropBottom", Integer.toString(wallpaperData.cropHint.bottom));
        if (wallpaperData.padding.left != 0) {
            xmlSerializer.attribute(null, "paddingLeft", Integer.toString(wallpaperData.padding.left));
        }
        if (wallpaperData.padding.top != 0) {
            xmlSerializer.attribute(null, "paddingTop", Integer.toString(wallpaperData.padding.top));
        }
        if (wallpaperData.padding.right != 0) {
            xmlSerializer.attribute(null, "paddingRight", Integer.toString(wallpaperData.padding.right));
        }
        if (wallpaperData.padding.bottom != 0) {
            xmlSerializer.attribute(null, "paddingBottom", Integer.toString(wallpaperData.padding.bottom));
        }
        if (wallpaperData.primaryColors != null) {
            int size = wallpaperData.primaryColors.getMainColors().size();
            xmlSerializer.attribute(null, "colorsCount", Integer.toString(size));
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    xmlSerializer.attribute(null, "colorValue" + i, Integer.toString(((Color) wallpaperData.primaryColors.getMainColors().get(i)).toArgb()));
                }
            }
            xmlSerializer.attribute(null, "colorHints", Integer.toString(wallpaperData.primaryColors.getColorHints()));
        }
        xmlSerializer.attribute(null, com.android.server.pm.Settings.ATTR_NAME, wallpaperData.name);
        if (wallpaperData.wallpaperComponent != null && !wallpaperData.wallpaperComponent.equals(this.mImageWallpaper)) {
            xmlSerializer.attribute(null, "component", wallpaperData.wallpaperComponent.flattenToShortString());
        }
        if (wallpaperData.allowBackup) {
            xmlSerializer.attribute(null, BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD, "true");
        }
        xmlSerializer.endTag(null, str);
    }

    private void migrateFromOld() {
        File file = new File(getWallpaperDir(0), WALLPAPER_CROP);
        File file2 = new File("/data/data/com.android.settings/files/wallpaper");
        File file3 = new File(getWallpaperDir(0), WALLPAPER);
        if (file.exists()) {
            if (!file3.exists()) {
                FileUtils.copyFile(file, file3);
            }
        } else if (file2.exists()) {
            File file4 = new File("/data/system/wallpaper_info.xml");
            if (file4.exists()) {
                file4.renameTo(new File(getWallpaperDir(0), WALLPAPER_INFO));
            }
            FileUtils.copyFile(file2, file);
            file2.renameTo(file3);
        }
    }

    private int getAttributeInt(XmlPullParser xmlPullParser, String str, int i) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            return i;
        }
        return Integer.parseInt(attributeValue);
    }

    private WallpaperData getWallpaperSafeLocked(int i, int i2) throws Throwable {
        SparseArray<WallpaperData> sparseArray = i2 == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
        WallpaperData wallpaperData = sparseArray.get(i);
        if (wallpaperData == null) {
            loadSettingsLocked(i, false);
            WallpaperData wallpaperData2 = sparseArray.get(i);
            if (wallpaperData2 == null) {
                if (i2 == 2) {
                    WallpaperData wallpaperData3 = new WallpaperData(i, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                    this.mLockWallpaperMap.put(i, wallpaperData3);
                    ensureSaneWallpaperData(wallpaperData3);
                    return wallpaperData3;
                }
                Slog.wtf(TAG, "Didn't find wallpaper in non-lock case!");
                WallpaperData wallpaperData4 = new WallpaperData(i, WALLPAPER, WALLPAPER_CROP);
                this.mWallpaperMap.put(i, wallpaperData4);
                ensureSaneWallpaperData(wallpaperData4);
                return wallpaperData4;
            }
            return wallpaperData2;
        }
        return wallpaperData;
    }

    private void loadSettingsLocked(int i, boolean z) throws Throwable {
        FileInputStream fileInputStream;
        int next;
        ComponentName componentNameUnflattenFromString;
        File fileChooseForRead = makeJournaledFile(i).chooseForRead();
        WallpaperData wallpaperData = this.mWallpaperMap.get(i);
        boolean z2 = true;
        if (wallpaperData == null) {
            migrateFromOld();
            wallpaperData = new WallpaperData(i, WALLPAPER, WALLPAPER_CROP);
            wallpaperData.allowBackup = true;
            this.mWallpaperMap.put(i, wallpaperData);
            if (!wallpaperData.cropExists()) {
                if (wallpaperData.sourceExists()) {
                    generateCrop(wallpaperData);
                } else {
                    Slog.i(TAG, "No static wallpaper imagery; defaults will be shown");
                }
            }
        }
        try {
            fileInputStream = new FileInputStream(fileChooseForRead);
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
                do {
                    next = xmlPullParserNewPullParser.next();
                    if (next == 2) {
                        String name = xmlPullParserNewPullParser.getName();
                        if ("wp".equals(name)) {
                            parseWallpaperAttributes(xmlPullParserNewPullParser, wallpaperData, z);
                            String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "component");
                            if (attributeValue != null) {
                                componentNameUnflattenFromString = ComponentName.unflattenFromString(attributeValue);
                            } else {
                                componentNameUnflattenFromString = null;
                            }
                            wallpaperData.nextWallpaperComponent = componentNameUnflattenFromString;
                            if (wallpaperData.nextWallpaperComponent == null || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(wallpaperData.nextWallpaperComponent.getPackageName())) {
                                wallpaperData.nextWallpaperComponent = this.mImageWallpaper;
                            }
                        } else if ("kwp".equals(name)) {
                            WallpaperData wallpaperData2 = this.mLockWallpaperMap.get(i);
                            if (wallpaperData2 == null) {
                                wallpaperData2 = new WallpaperData(i, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                                this.mLockWallpaperMap.put(i, wallpaperData2);
                            }
                            parseWallpaperAttributes(xmlPullParserNewPullParser, wallpaperData2, false);
                        }
                    }
                } while (next != 1);
            } catch (FileNotFoundException e) {
                Slog.w(TAG, "no current wallpaper -- first boot?");
                z2 = false;
            } catch (IOException e2) {
                e = e2;
                Slog.w(TAG, "failed parsing " + fileChooseForRead + " " + e);
                z2 = false;
            } catch (IndexOutOfBoundsException e3) {
                e = e3;
                Slog.w(TAG, "failed parsing " + fileChooseForRead + " " + e);
                z2 = false;
            } catch (NullPointerException e4) {
                e = e4;
                Slog.w(TAG, "failed parsing " + fileChooseForRead + " " + e);
                z2 = false;
            } catch (NumberFormatException e5) {
                e = e5;
                Slog.w(TAG, "failed parsing " + fileChooseForRead + " " + e);
                z2 = false;
            } catch (XmlPullParserException e6) {
                e = e6;
                Slog.w(TAG, "failed parsing " + fileChooseForRead + " " + e);
                z2 = false;
            }
        } catch (FileNotFoundException e7) {
            fileInputStream = null;
        } catch (IOException e8) {
            e = e8;
            fileInputStream = null;
        } catch (IndexOutOfBoundsException e9) {
            e = e9;
            fileInputStream = null;
        } catch (NullPointerException e10) {
            e = e10;
            fileInputStream = null;
        } catch (NumberFormatException e11) {
            e = e11;
            fileInputStream = null;
        } catch (XmlPullParserException e12) {
            e = e12;
            fileInputStream = null;
        }
        IoUtils.closeQuietly(fileInputStream);
        if (!z2) {
            wallpaperData.width = -1;
            wallpaperData.height = -1;
            wallpaperData.cropHint.set(0, 0, 0, 0);
            wallpaperData.padding.set(0, 0, 0, 0);
            wallpaperData.name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.mLockWallpaperMap.remove(i);
        } else if (wallpaperData.wallpaperId <= 0) {
            wallpaperData.wallpaperId = makeWallpaperIdLocked();
        }
        ensureSaneWallpaperData(wallpaperData);
        WallpaperData wallpaperData3 = this.mLockWallpaperMap.get(i);
        if (wallpaperData3 != null) {
            ensureSaneWallpaperData(wallpaperData3);
        }
    }

    private void ensureSaneWallpaperData(WallpaperData wallpaperData) {
        int maximumSizeDimension = getMaximumSizeDimension();
        if (wallpaperData.width < maximumSizeDimension) {
            wallpaperData.width = maximumSizeDimension;
        }
        if (wallpaperData.height < maximumSizeDimension) {
            wallpaperData.height = maximumSizeDimension;
        }
        if (wallpaperData.cropHint.width() <= 0 || wallpaperData.cropHint.height() <= 0) {
            wallpaperData.cropHint.set(0, 0, wallpaperData.width, wallpaperData.height);
        }
    }

    private void parseWallpaperAttributes(XmlPullParser xmlPullParser, WallpaperData wallpaperData, boolean z) {
        String attributeValue = xmlPullParser.getAttributeValue(null, "id");
        if (attributeValue != null) {
            int i = Integer.parseInt(attributeValue);
            wallpaperData.wallpaperId = i;
            if (i > this.mWallpaperId) {
                this.mWallpaperId = i;
            }
        } else {
            wallpaperData.wallpaperId = makeWallpaperIdLocked();
        }
        if (!z) {
            wallpaperData.width = Integer.parseInt(xmlPullParser.getAttributeValue(null, "width"));
            wallpaperData.height = Integer.parseInt(xmlPullParser.getAttributeValue(null, "height"));
        }
        wallpaperData.cropHint.left = getAttributeInt(xmlPullParser, "cropLeft", 0);
        wallpaperData.cropHint.top = getAttributeInt(xmlPullParser, "cropTop", 0);
        wallpaperData.cropHint.right = getAttributeInt(xmlPullParser, "cropRight", 0);
        wallpaperData.cropHint.bottom = getAttributeInt(xmlPullParser, "cropBottom", 0);
        wallpaperData.padding.left = getAttributeInt(xmlPullParser, "paddingLeft", 0);
        wallpaperData.padding.top = getAttributeInt(xmlPullParser, "paddingTop", 0);
        wallpaperData.padding.right = getAttributeInt(xmlPullParser, "paddingRight", 0);
        wallpaperData.padding.bottom = getAttributeInt(xmlPullParser, "paddingBottom", 0);
        int attributeInt = getAttributeInt(xmlPullParser, "colorsCount", 0);
        if (attributeInt > 0) {
            Color color = null;
            Color color2 = null;
            Color color3 = null;
            for (int i2 = 0; i2 < attributeInt; i2++) {
                Color colorValueOf = Color.valueOf(getAttributeInt(xmlPullParser, "colorValue" + i2, 0));
                if (i2 == 0) {
                    color = colorValueOf;
                } else if (i2 == 1) {
                    color2 = colorValueOf;
                } else if (i2 != 2) {
                    break;
                } else {
                    color3 = colorValueOf;
                }
            }
            wallpaperData.primaryColors = new WallpaperColors(color, color2, color3, getAttributeInt(xmlPullParser, "colorHints", 0));
        }
        wallpaperData.name = xmlPullParser.getAttributeValue(null, com.android.server.pm.Settings.ATTR_NAME);
        wallpaperData.allowBackup = "true".equals(xmlPullParser.getAttributeValue(null, BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD));
    }

    private int getMaximumSizeDimension() {
        return ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getMaximumSizeDimension();
    }

    public void settingsRestored() {
        WallpaperData wallpaperData;
        boolean zRestoreNamedResourceLocked;
        if (Binder.getCallingUid() != 1000) {
            throw new RuntimeException("settingsRestored() can only be called from the system process");
        }
        synchronized (this.mLock) {
            loadSettingsLocked(0, false);
            wallpaperData = this.mWallpaperMap.get(0);
            wallpaperData.wallpaperId = makeWallpaperIdLocked();
            zRestoreNamedResourceLocked = true;
            wallpaperData.allowBackup = true;
            if (wallpaperData.nextWallpaperComponent != null && !wallpaperData.nextWallpaperComponent.equals(this.mImageWallpaper)) {
                if (!bindWallpaperComponentLocked(wallpaperData.nextWallpaperComponent, false, false, wallpaperData, null)) {
                    bindWallpaperComponentLocked(null, false, false, wallpaperData, null);
                }
            } else {
                if (!BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(wallpaperData.name)) {
                    zRestoreNamedResourceLocked = restoreNamedResourceLocked(wallpaperData);
                }
                if (zRestoreNamedResourceLocked) {
                    generateCrop(wallpaperData);
                    bindWallpaperComponentLocked(wallpaperData.nextWallpaperComponent, true, false, wallpaperData, null);
                }
            }
        }
        if (!zRestoreNamedResourceLocked) {
            Slog.e(TAG, "Failed to restore wallpaper: '" + wallpaperData.name + "'");
            wallpaperData.name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            getWallpaperDir(0).delete();
        }
        synchronized (this.mLock) {
            saveSettingsLocked(0);
        }
    }

    boolean restoreNamedResourceLocked(WallpaperData wallpaperData) throws Throwable {
        Object obj;
        int identifier;
        Resources resources;
        if (wallpaperData.name.length() > 4 && "res:".equals(wallpaperData.name.substring(0, 4))) {
            String strSubstring = wallpaperData.name.substring(4);
            int iIndexOf = strSubstring.indexOf(58);
            InputStream inputStream = null;
            String strSubstring2 = iIndexOf > 0 ? strSubstring.substring(0, iIndexOf) : null;
            int iLastIndexOf = strSubstring.lastIndexOf(47);
            String strSubstring3 = iLastIndexOf > 0 ? strSubstring.substring(iLastIndexOf + 1) : null;
            ?? Substring = (iIndexOf <= 0 || iLastIndexOf <= 0 || iLastIndexOf - iIndexOf <= 1) ? 0 : strSubstring.substring(iIndexOf + 1, iLastIndexOf);
            if (strSubstring2 != null && strSubstring3 != null && Substring != 0) {
                ?? fileOutputStream = -1;
                try {
                    try {
                        try {
                            resources = this.mContext.createPackageContext(strSubstring2, 4).getResources();
                            identifier = resources.getIdentifier(strSubstring, null, null);
                        } catch (Resources.NotFoundException e) {
                            obj = null;
                            identifier = -1;
                        }
                        try {
                            if (identifier == 0) {
                                Slog.e(TAG, "couldn't resolve identifier pkg=" + strSubstring2 + " type=" + Substring + " ident=" + strSubstring3);
                                IoUtils.closeQuietly((AutoCloseable) null);
                                IoUtils.closeQuietly((AutoCloseable) null);
                                IoUtils.closeQuietly((AutoCloseable) null);
                                return false;
                            }
                            InputStream inputStreamOpenRawResource = resources.openRawResource(identifier);
                            try {
                                if (wallpaperData.wallpaperFile.exists()) {
                                    wallpaperData.wallpaperFile.delete();
                                    wallpaperData.cropFile.delete();
                                }
                                Substring = new FileOutputStream(wallpaperData.wallpaperFile);
                                try {
                                    fileOutputStream = new FileOutputStream(wallpaperData.cropFile);
                                    try {
                                        byte[] bArr = new byte[32768];
                                        while (true) {
                                            int i = inputStreamOpenRawResource.read(bArr);
                                            if (i <= 0) {
                                                Slog.v(TAG, "Restored wallpaper: " + strSubstring);
                                                IoUtils.closeQuietly(inputStreamOpenRawResource);
                                                FileUtils.sync(Substring);
                                                FileUtils.sync(fileOutputStream);
                                                IoUtils.closeQuietly((AutoCloseable) Substring);
                                                IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                                return true;
                                            }
                                            Substring.write(bArr, 0, i);
                                            fileOutputStream.write(bArr, 0, i);
                                        }
                                    } catch (PackageManager.NameNotFoundException e2) {
                                        inputStream = inputStreamOpenRawResource;
                                        Substring = Substring;
                                        fileOutputStream = fileOutputStream;
                                        Slog.e(TAG, "Package name " + strSubstring2 + " not found");
                                        IoUtils.closeQuietly(inputStream);
                                        if (Substring != 0) {
                                            FileUtils.sync(Substring);
                                        }
                                        if (fileOutputStream != 0) {
                                        }
                                        IoUtils.closeQuietly((AutoCloseable) Substring);
                                        IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                        return false;
                                    } catch (Resources.NotFoundException e3) {
                                        inputStream = inputStreamOpenRawResource;
                                        Substring = Substring;
                                        fileOutputStream = fileOutputStream;
                                        Slog.e(TAG, "Resource not found: " + identifier);
                                        IoUtils.closeQuietly(inputStream);
                                        if (Substring != 0) {
                                        }
                                        if (fileOutputStream != 0) {
                                        }
                                        IoUtils.closeQuietly((AutoCloseable) Substring);
                                        IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                        return false;
                                    } catch (IOException e4) {
                                        e = e4;
                                        inputStream = inputStreamOpenRawResource;
                                        Substring = Substring;
                                        fileOutputStream = fileOutputStream;
                                        Slog.e(TAG, "IOException while restoring wallpaper ", e);
                                        IoUtils.closeQuietly(inputStream);
                                        if (Substring != 0) {
                                            FileUtils.sync(Substring);
                                        }
                                        if (fileOutputStream != 0) {
                                        }
                                        IoUtils.closeQuietly((AutoCloseable) Substring);
                                        IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                        return false;
                                    } catch (Throwable th) {
                                        th = th;
                                        inputStream = inputStreamOpenRawResource;
                                        IoUtils.closeQuietly(inputStream);
                                        if (Substring != 0) {
                                            FileUtils.sync(Substring);
                                        }
                                        if (fileOutputStream != 0) {
                                            FileUtils.sync(fileOutputStream);
                                        }
                                        IoUtils.closeQuietly((AutoCloseable) Substring);
                                        IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                                        throw th;
                                    }
                                } catch (PackageManager.NameNotFoundException e5) {
                                    fileOutputStream = 0;
                                } catch (Resources.NotFoundException e6) {
                                    fileOutputStream = 0;
                                } catch (IOException e7) {
                                    e = e7;
                                    fileOutputStream = 0;
                                } catch (Throwable th2) {
                                    th = th2;
                                    fileOutputStream = 0;
                                }
                            } catch (PackageManager.NameNotFoundException e8) {
                                Substring = 0;
                                fileOutputStream = 0;
                            } catch (Resources.NotFoundException e9) {
                                Substring = 0;
                                fileOutputStream = 0;
                            } catch (IOException e10) {
                                e = e10;
                                Substring = 0;
                                fileOutputStream = 0;
                            } catch (Throwable th3) {
                                th = th3;
                                Substring = 0;
                                fileOutputStream = 0;
                            }
                        } catch (Resources.NotFoundException e11) {
                            obj = null;
                            fileOutputStream = obj;
                            Substring = obj;
                            Slog.e(TAG, "Resource not found: " + identifier);
                            IoUtils.closeQuietly(inputStream);
                            if (Substring != 0) {
                                FileUtils.sync(Substring);
                            }
                            if (fileOutputStream != 0) {
                                FileUtils.sync(fileOutputStream);
                            }
                            IoUtils.closeQuietly((AutoCloseable) Substring);
                            IoUtils.closeQuietly((AutoCloseable) fileOutputStream);
                            return false;
                        }
                    } catch (PackageManager.NameNotFoundException e12) {
                        Substring = 0;
                        fileOutputStream = 0;
                    } catch (IOException e13) {
                        e = e13;
                        Substring = 0;
                        fileOutputStream = 0;
                    } catch (Throwable th4) {
                        th = th4;
                        Substring = 0;
                        fileOutputStream = 0;
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            }
        }
        return false;
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            synchronized (this.mLock) {
                printWriter.println("System wallpaper state:");
                for (int i = 0; i < this.mWallpaperMap.size(); i++) {
                    WallpaperData wallpaperDataValueAt = this.mWallpaperMap.valueAt(i);
                    printWriter.print(" User ");
                    printWriter.print(wallpaperDataValueAt.userId);
                    printWriter.print(": id=");
                    printWriter.println(wallpaperDataValueAt.wallpaperId);
                    printWriter.print("  mWidth=");
                    printWriter.print(wallpaperDataValueAt.width);
                    printWriter.print(" mHeight=");
                    printWriter.println(wallpaperDataValueAt.height);
                    printWriter.print("  mCropHint=");
                    printWriter.println(wallpaperDataValueAt.cropHint);
                    printWriter.print("  mPadding=");
                    printWriter.println(wallpaperDataValueAt.padding);
                    printWriter.print("  mName=");
                    printWriter.println(wallpaperDataValueAt.name);
                    printWriter.print("  mAllowBackup=");
                    printWriter.println(wallpaperDataValueAt.allowBackup);
                    printWriter.print("  mWallpaperComponent=");
                    printWriter.println(wallpaperDataValueAt.wallpaperComponent);
                    if (wallpaperDataValueAt.connection != null) {
                        WallpaperConnection wallpaperConnection = wallpaperDataValueAt.connection;
                        printWriter.print("  Wallpaper connection ");
                        printWriter.print(wallpaperConnection);
                        printWriter.println(":");
                        if (wallpaperConnection.mInfo != null) {
                            printWriter.print("    mInfo.component=");
                            printWriter.println(wallpaperConnection.mInfo.getComponent());
                        }
                        printWriter.print("    mToken=");
                        printWriter.println(wallpaperConnection.mToken);
                        printWriter.print("    mService=");
                        printWriter.println(wallpaperConnection.mService);
                        printWriter.print("    mEngine=");
                        printWriter.println(wallpaperConnection.mEngine);
                        printWriter.print("    mLastDiedTime=");
                        printWriter.println(wallpaperDataValueAt.lastDiedTime - SystemClock.uptimeMillis());
                    }
                }
                printWriter.println("Lock wallpaper state:");
                for (int i2 = 0; i2 < this.mLockWallpaperMap.size(); i2++) {
                    WallpaperData wallpaperDataValueAt2 = this.mLockWallpaperMap.valueAt(i2);
                    printWriter.print(" User ");
                    printWriter.print(wallpaperDataValueAt2.userId);
                    printWriter.print(": id=");
                    printWriter.println(wallpaperDataValueAt2.wallpaperId);
                    printWriter.print("  mWidth=");
                    printWriter.print(wallpaperDataValueAt2.width);
                    printWriter.print(" mHeight=");
                    printWriter.println(wallpaperDataValueAt2.height);
                    printWriter.print("  mCropHint=");
                    printWriter.println(wallpaperDataValueAt2.cropHint);
                    printWriter.print("  mPadding=");
                    printWriter.println(wallpaperDataValueAt2.padding);
                    printWriter.print("  mName=");
                    printWriter.println(wallpaperDataValueAt2.name);
                    printWriter.print("  mAllowBackup=");
                    printWriter.println(wallpaperDataValueAt2.allowBackup);
                }
            }
        }
    }
}
