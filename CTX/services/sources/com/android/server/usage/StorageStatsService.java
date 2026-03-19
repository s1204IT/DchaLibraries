package com.android.server.usage;

import android.app.AppOpsManager;
import android.app.usage.ExternalStorageStats;
import android.app.usage.IStorageStatsManager;
import android.app.usage.StorageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelableException;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.DataUnit;
import android.util.Slog;
import android.util.SparseLongArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import com.android.server.storage.CacheQuotaStrategy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

public class StorageStatsService extends IStorageStatsManager.Stub {
    private static final long DEFAULT_QUOTA = DataUnit.MEBIBYTES.toBytes(64);
    private static final long DELAY_IN_MILLIS = 30000;
    private static final String PROP_DISABLE_QUOTA = "fw.disable_quota";
    private static final String PROP_VERIFY_STORAGE = "fw.verify_storage";
    private static final String TAG = "StorageStatsService";
    private final AppOpsManager mAppOps;
    private final ArrayMap<String, SparseLongArray> mCacheQuotas = new ArrayMap<>();
    private final Context mContext;
    private final H mHandler;
    private final Installer mInstaller;
    private final PackageManager mPackage;
    private final StorageManager mStorage;
    private final UserManager mUser;

    public static class Lifecycle extends SystemService {
        private StorageStatsService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mService = new StorageStatsService(getContext());
            publishBinderService("storagestats", this.mService);
        }
    }

    public StorageStatsService(Context context) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mAppOps = (AppOpsManager) Preconditions.checkNotNull((AppOpsManager) context.getSystemService(AppOpsManager.class));
        this.mUser = (UserManager) Preconditions.checkNotNull((UserManager) context.getSystemService(UserManager.class));
        this.mPackage = (PackageManager) Preconditions.checkNotNull(context.getPackageManager());
        this.mStorage = (StorageManager) Preconditions.checkNotNull((StorageManager) context.getSystemService(StorageManager.class));
        this.mInstaller = new Installer(context);
        this.mInstaller.onStart();
        invalidateMounts();
        this.mHandler = new H(IoThread.get().getLooper());
        this.mHandler.sendEmptyMessage(101);
        this.mStorage.registerListener(new StorageEventListener() {
            public void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
                switch (volumeInfo.type) {
                    case 1:
                    case 2:
                        if (i2 == 2) {
                            StorageStatsService.this.invalidateMounts();
                        }
                        break;
                }
            }
        });
    }

    private void invalidateMounts() {
        try {
            this.mInstaller.invalidateMounts();
        } catch (Installer.InstallerException e) {
            Slog.wtf(TAG, "Failed to invalidate mounts", e);
        }
    }

    private void enforcePermission(int i, String str) {
        int iNoteOp = this.mAppOps.noteOp(43, i, str);
        if (iNoteOp != 0) {
            if (iNoteOp == 3) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", TAG);
                return;
            }
            throw new SecurityException("Package " + str + " from UID " + i + " blocked by mode " + iNoteOp);
        }
    }

    public boolean isQuotaSupported(String str, String str2) throws ParcelableException {
        enforcePermission(Binder.getCallingUid(), str2);
        try {
            return this.mInstaller.isQuotaSupported(str);
        } catch (Installer.InstallerException e) {
            throw new ParcelableException(new IOException(e.getMessage()));
        }
    }

    public boolean isReservedSupported(String str, String str2) {
        enforcePermission(Binder.getCallingUid(), str2);
        if (str == StorageManager.UUID_PRIVATE_INTERNAL) {
            return SystemProperties.getBoolean("vold.has_reserved", false);
        }
        return false;
    }

    public long getTotalBytes(String str, String str2) throws ParcelableException {
        if (str == StorageManager.UUID_PRIVATE_INTERNAL) {
            return FileUtils.roundStorageSize(this.mStorage.getPrimaryStorageSize());
        }
        VolumeInfo volumeInfoFindVolumeByUuid = this.mStorage.findVolumeByUuid(str);
        if (volumeInfoFindVolumeByUuid == null) {
            throw new ParcelableException(new IOException("Failed to find storage device for UUID " + str));
        }
        return volumeInfoFindVolumeByUuid.disk.size;
    }

    public long getFreeBytes(String str, String str2) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                File fileFindPathForUuid = this.mStorage.findPathForUuid(str);
                if (!isQuotaSupported(str, PackageManagerService.PLATFORM_PACKAGE_NAME)) {
                    return fileFindPathForUuid.getUsableSpace();
                }
                return fileFindPathForUuid.getUsableSpace() + Math.max(0L, getCacheBytes(str, PackageManagerService.PLATFORM_PACKAGE_NAME) - this.mStorage.getStorageCacheBytes(fileFindPathForUuid, 0));
            } catch (FileNotFoundException e) {
                throw new ParcelableException(e);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public long getCacheBytes(String str, String str2) {
        enforcePermission(Binder.getCallingUid(), str2);
        Iterator it = this.mUser.getUsers().iterator();
        long j = 0;
        while (it.hasNext()) {
            j += queryStatsForUser(str, ((UserInfo) it.next()).id, null).cacheBytes;
        }
        return j;
    }

    public long getCacheQuotaBytes(String str, int i, String str2) {
        enforcePermission(Binder.getCallingUid(), str2);
        if (this.mCacheQuotas.containsKey(str)) {
            return this.mCacheQuotas.get(str).get(i, DEFAULT_QUOTA);
        }
        return DEFAULT_QUOTA;
    }

    public StorageStats queryStatsForPackage(String str, String str2, int i, String str3) throws ParcelableException {
        if (i != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        try {
            ApplicationInfo applicationInfoAsUser = this.mPackage.getApplicationInfoAsUser(str2, 8192, i);
            if (Binder.getCallingUid() != applicationInfoAsUser.uid) {
                enforcePermission(Binder.getCallingUid(), str3);
            }
            if (ArrayUtils.defeatNullable(this.mPackage.getPackagesForUid(applicationInfoAsUser.uid)).length == 1) {
                return queryStatsForUid(str, applicationInfoAsUser.uid, str3);
            }
            int userId = UserHandle.getUserId(applicationInfoAsUser.uid);
            String[] strArr = {str2};
            long[] jArr = new long[1];
            String[] strArr2 = new String[0];
            if (!applicationInfoAsUser.isSystemApp() || applicationInfoAsUser.isUpdatedSystemApp()) {
                strArr2 = (String[]) ArrayUtils.appendElement(String.class, strArr2, applicationInfoAsUser.getCodePath());
            }
            String[] strArr3 = strArr2;
            PackageStats packageStats = new PackageStats(TAG);
            try {
                this.mInstaller.getAppSize(str, strArr, i, 0, userId, jArr, strArr3, packageStats);
                return translate(packageStats);
            } catch (Installer.InstallerException e) {
                throw new ParcelableException(new IOException(e.getMessage()));
            }
        } catch (PackageManager.NameNotFoundException e2) {
            throw new ParcelableException(e2);
        }
    }

    public StorageStats queryStatsForUid(String str, int i, String str2) throws ParcelableException {
        PackageStats packageStats;
        int userId = UserHandle.getUserId(i);
        int appId = UserHandle.getAppId(i);
        if (userId != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        if (Binder.getCallingUid() != i) {
            enforcePermission(Binder.getCallingUid(), str2);
        }
        String[] strArrDefeatNullable = ArrayUtils.defeatNullable(this.mPackage.getPackagesForUid(i));
        long[] jArr = new long[strArrDefeatNullable.length];
        String[] strArr = new String[0];
        for (String str3 : strArrDefeatNullable) {
            try {
                ApplicationInfo applicationInfoAsUser = this.mPackage.getApplicationInfoAsUser(str3, 8192, userId);
                if (!applicationInfoAsUser.isSystemApp() || applicationInfoAsUser.isUpdatedSystemApp()) {
                    strArr = (String[]) ArrayUtils.appendElement(String.class, strArr, applicationInfoAsUser.getCodePath());
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new ParcelableException(e);
            }
        }
        PackageStats packageStats2 = new PackageStats(TAG);
        try {
            String[] strArr2 = strArr;
            this.mInstaller.getAppSize(str, strArrDefeatNullable, userId, getDefaultFlags(), appId, jArr, strArr, packageStats2);
            if (SystemProperties.getBoolean(PROP_VERIFY_STORAGE, false)) {
                PackageStats packageStats3 = new PackageStats(TAG);
                this.mInstaller.getAppSize(str, strArrDefeatNullable, userId, 0, appId, jArr, strArr2, packageStats3);
                packageStats = packageStats2;
                checkEquals("UID " + i, packageStats3, packageStats);
            } else {
                packageStats = packageStats2;
            }
            return translate(packageStats);
        } catch (Installer.InstallerException e2) {
            throw new ParcelableException(new IOException(e2.getMessage()));
        }
    }

    public StorageStats queryStatsForUser(String str, int i, String str2) throws ParcelableException {
        if (i != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        enforcePermission(Binder.getCallingUid(), str2);
        int[] appIds = getAppIds(i);
        PackageStats packageStats = new PackageStats(TAG);
        try {
            this.mInstaller.getUserSize(str, i, getDefaultFlags(), appIds, packageStats);
            if (SystemProperties.getBoolean(PROP_VERIFY_STORAGE, false)) {
                PackageStats packageStats2 = new PackageStats(TAG);
                this.mInstaller.getUserSize(str, i, 0, appIds, packageStats2);
                checkEquals("User " + i, packageStats2, packageStats);
            }
            return translate(packageStats);
        } catch (Installer.InstallerException e) {
            throw new ParcelableException(new IOException(e.getMessage()));
        }
    }

    public ExternalStorageStats queryExternalStatsForUser(String str, int i, String str2) throws ParcelableException {
        if (i != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        enforcePermission(Binder.getCallingUid(), str2);
        int[] appIds = getAppIds(i);
        try {
            long[] externalSize = this.mInstaller.getExternalSize(str, i, getDefaultFlags(), appIds);
            if (SystemProperties.getBoolean(PROP_VERIFY_STORAGE, false)) {
                checkEquals("External " + i, this.mInstaller.getExternalSize(str, i, 0, appIds), externalSize);
            }
            ExternalStorageStats externalStorageStats = new ExternalStorageStats();
            externalStorageStats.totalBytes = externalSize[0];
            externalStorageStats.audioBytes = externalSize[1];
            externalStorageStats.videoBytes = externalSize[2];
            externalStorageStats.imageBytes = externalSize[3];
            externalStorageStats.appBytes = externalSize[4];
            externalStorageStats.obbBytes = externalSize[5];
            return externalStorageStats;
        } catch (Installer.InstallerException e) {
            throw new ParcelableException(new IOException(e.getMessage()));
        }
    }

    private int[] getAppIds(int i) {
        Iterator it = this.mPackage.getInstalledApplicationsAsUser(8192, i).iterator();
        int[] iArrAppendInt = null;
        while (it.hasNext()) {
            int appId = UserHandle.getAppId(((ApplicationInfo) it.next()).uid);
            if (!ArrayUtils.contains(iArrAppendInt, appId)) {
                iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, appId);
            }
        }
        return iArrAppendInt;
    }

    private static int getDefaultFlags() {
        return SystemProperties.getBoolean(PROP_DISABLE_QUOTA, false) ? 0 : 4096;
    }

    private static void checkEquals(String str, long[] jArr, long[] jArr2) {
        for (int i = 0; i < jArr.length; i++) {
            checkEquals(str + "[" + i + "]", jArr[i], jArr2[i]);
        }
    }

    private static void checkEquals(String str, PackageStats packageStats, PackageStats packageStats2) {
        checkEquals(str + " codeSize", packageStats.codeSize, packageStats2.codeSize);
        checkEquals(str + " dataSize", packageStats.dataSize, packageStats2.dataSize);
        checkEquals(str + " cacheSize", packageStats.cacheSize, packageStats2.cacheSize);
        checkEquals(str + " externalCodeSize", packageStats.externalCodeSize, packageStats2.externalCodeSize);
        checkEquals(str + " externalDataSize", packageStats.externalDataSize, packageStats2.externalDataSize);
        checkEquals(str + " externalCacheSize", packageStats.externalCacheSize, packageStats2.externalCacheSize);
    }

    private static void checkEquals(String str, long j, long j2) {
        if (j != j2) {
            Slog.e(TAG, str + " expected " + j + " actual " + j2);
        }
    }

    private static StorageStats translate(PackageStats packageStats) {
        StorageStats storageStats = new StorageStats();
        storageStats.codeBytes = packageStats.codeSize + packageStats.externalCodeSize;
        storageStats.dataBytes = packageStats.dataSize + packageStats.externalDataSize;
        storageStats.cacheBytes = packageStats.cacheSize + packageStats.externalCacheSize;
        return storageStats;
    }

    private class H extends Handler {
        private static final boolean DEBUG = false;
        private static final double MINIMUM_CHANGE_DELTA = 0.05d;
        private static final int MSG_CHECK_STORAGE_DELTA = 100;
        private static final int MSG_LOAD_CACHED_QUOTAS_FROM_FILE = 101;
        private static final int UNSET = -1;
        private double mMinimumThresholdBytes;
        private long mPreviousBytes;
        private final StatFs mStats;

        public H(Looper looper) {
            super(looper);
            this.mStats = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            this.mPreviousBytes = this.mStats.getAvailableBytes();
            this.mMinimumThresholdBytes = this.mStats.getTotalBytes() * MINIMUM_CHANGE_DELTA;
        }

        @Override
        public void handleMessage(Message message) {
            if (!StorageStatsService.isCacheQuotaCalculationsEnabled(StorageStatsService.this.mContext.getContentResolver())) {
            }
            switch (message.what) {
                case 100:
                    if (Math.abs(this.mPreviousBytes - this.mStats.getAvailableBytes()) > this.mMinimumThresholdBytes) {
                        this.mPreviousBytes = this.mStats.getAvailableBytes();
                        recalculateQuotas(getInitializedStrategy());
                        StorageStatsService.this.notifySignificantDelta();
                    }
                    sendEmptyMessageDelayed(100, 30000L);
                    break;
                case 101:
                    CacheQuotaStrategy initializedStrategy = getInitializedStrategy();
                    this.mPreviousBytes = -1L;
                    try {
                        this.mPreviousBytes = initializedStrategy.setupQuotasFromFile();
                    } catch (IOException e) {
                        Slog.e(StorageStatsService.TAG, "An error occurred while reading the cache quota file.", e);
                    } catch (IllegalStateException e2) {
                        Slog.e(StorageStatsService.TAG, "Cache quota XML file is malformed?", e2);
                    }
                    if (this.mPreviousBytes < 0) {
                        this.mPreviousBytes = this.mStats.getAvailableBytes();
                        recalculateQuotas(initializedStrategy);
                    }
                    sendEmptyMessageDelayed(100, 30000L);
                    break;
            }
        }

        private void recalculateQuotas(CacheQuotaStrategy cacheQuotaStrategy) {
            cacheQuotaStrategy.recalculateQuotas();
        }

        private CacheQuotaStrategy getInitializedStrategy() {
            return new CacheQuotaStrategy(StorageStatsService.this.mContext, (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class), StorageStatsService.this.mInstaller, StorageStatsService.this.mCacheQuotas);
        }
    }

    @VisibleForTesting
    static boolean isCacheQuotaCalculationsEnabled(ContentResolver contentResolver) {
        return Settings.Global.getInt(contentResolver, "enable_cache_quota_calculation", 1) != 0;
    }

    void notifySignificantDelta() {
        this.mContext.getContentResolver().notifyChange(Uri.parse("content://com.android.externalstorage.documents/"), (ContentObserver) null, false);
    }
}
