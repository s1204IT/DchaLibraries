package com.android.server.storage;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.EventLogTags;
import com.android.server.SystemService;
import com.android.server.location.LocationFudger;
import com.android.server.pm.PackageManagerService;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DeviceStorageMonitorService extends SystemService {
    static final boolean DEBUG = false;
    private static final long DEFAULT_CHECK_INTERVAL = 60000;
    private static final long DEFAULT_DISK_FREE_CHANGE_REPORTING_THRESHOLD = 2097152;
    private static final int DEFAULT_FREE_STORAGE_LOG_INTERVAL_IN_MINUTES = 720;
    static final int DEVICE_MEMORY_WHAT = 1;
    private static final int LOW_MEMORY_NOTIFICATION_ID = 1;
    private static final int MONITOR_INTERVAL = 1;
    static final String SERVICE = "devicestoragemonitor";
    static final String TAG = "DeviceStorageMonitorService";
    private static final int _FALSE = 0;
    private static final int _TRUE = 1;
    static final boolean localLOGV = false;
    private CacheFileDeletedObserver mCacheFileDeletedObserver;
    private final StatFs mCacheFileStats;
    private CachePackageDataObserver mClearCacheObserver;
    boolean mClearSucceeded;
    boolean mClearingCache;
    private final StatFs mDataFileStats;
    private long mFreeMem;
    private long mFreeMemAfterLastCacheClear;
    private final Handler mHandler;
    private final boolean mIsBootImageOnDisk;
    private long mLastReportedFreeMem;
    private long mLastReportedFreeMemTime;
    private final DeviceStorageMonitorInternal mLocalService;
    boolean mLowMemFlag;
    private long mMemCacheStartTrimThreshold;
    private long mMemCacheTrimToThreshold;
    private boolean mMemFullFlag;
    private long mMemFullThreshold;
    long mMemLowThreshold;
    private final IBinder mRemoteService;
    private final ContentResolver mResolver;
    private final Intent mStorageFullIntent;
    private final Intent mStorageLowIntent;
    private final Intent mStorageNotFullIntent;
    private final Intent mStorageOkIntent;
    private final StatFs mSystemFileStats;
    private long mThreadStartTime;
    private final long mTotalMemory;
    private static final File DATA_PATH = Environment.getDataDirectory();
    private static final File SYSTEM_PATH = Environment.getRootDirectory();
    private static final File CACHE_PATH = Environment.getDownloadCacheDirectory();

    private class CachePackageDataObserver extends IPackageDataObserver.Stub {
        private CachePackageDataObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            DeviceStorageMonitorService.this.mClearSucceeded = succeeded;
            DeviceStorageMonitorService.this.mClearingCache = DeviceStorageMonitorService.DEBUG;
            DeviceStorageMonitorService.this.postCheckMemoryMsg(DeviceStorageMonitorService.DEBUG, 0L);
        }
    }

    private void restatDataDir() {
        try {
            this.mDataFileStats.restat(DATA_PATH.getAbsolutePath());
            this.mFreeMem = ((long) this.mDataFileStats.getAvailableBlocks()) * ((long) this.mDataFileStats.getBlockSize());
        } catch (IllegalArgumentException e) {
        }
        String debugFreeMem = SystemProperties.get("debug.freemem");
        if (!"".equals(debugFreeMem)) {
            this.mFreeMem = Long.parseLong(debugFreeMem);
        }
        long freeMemLogInterval = Settings.Global.getLong(this.mResolver, "sys_free_storage_log_interval", 720L) * 60 * 1000;
        long currTime = SystemClock.elapsedRealtime();
        if (this.mLastReportedFreeMemTime == 0 || currTime - this.mLastReportedFreeMemTime >= freeMemLogInterval) {
            this.mLastReportedFreeMemTime = currTime;
            long mFreeSystem = -1;
            long mFreeCache = -1;
            try {
                this.mSystemFileStats.restat(SYSTEM_PATH.getAbsolutePath());
                mFreeSystem = ((long) this.mSystemFileStats.getAvailableBlocks()) * ((long) this.mSystemFileStats.getBlockSize());
            } catch (IllegalArgumentException e2) {
            }
            try {
                this.mCacheFileStats.restat(CACHE_PATH.getAbsolutePath());
                mFreeCache = ((long) this.mCacheFileStats.getAvailableBlocks()) * ((long) this.mCacheFileStats.getBlockSize());
            } catch (IllegalArgumentException e3) {
            }
            EventLog.writeEvent(EventLogTags.FREE_STORAGE_LEFT, Long.valueOf(this.mFreeMem), Long.valueOf(mFreeSystem), Long.valueOf(mFreeCache));
        }
        long threshold = Settings.Global.getLong(this.mResolver, "disk_free_change_reporting_threshold", DEFAULT_DISK_FREE_CHANGE_REPORTING_THRESHOLD);
        long delta = this.mFreeMem - this.mLastReportedFreeMem;
        if (delta > threshold || delta < (-threshold)) {
            this.mLastReportedFreeMem = this.mFreeMem;
            EventLog.writeEvent(EventLogTags.FREE_STORAGE_CHANGED, this.mFreeMem);
        }
    }

    private void clearCache() {
        if (this.mClearCacheObserver == null) {
            this.mClearCacheObserver = new CachePackageDataObserver();
        }
        this.mClearingCache = true;
        try {
            IPackageManager.Stub.asInterface(ServiceManager.getService("package")).freeStorageAndNotify(this.mMemCacheTrimToThreshold, this.mClearCacheObserver);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to get handle for PackageManger Exception: " + e);
            this.mClearingCache = DEBUG;
            this.mClearSucceeded = DEBUG;
        }
    }

    void checkMemory(boolean checkCache) {
        if (this.mClearingCache) {
            long diffTime = System.currentTimeMillis() - this.mThreadStartTime;
            if (diffTime > LocationFudger.FASTEST_INTERVAL_MS) {
                Slog.w(TAG, "Thread that clears cache file seems to run for ever");
            }
        } else {
            restatDataDir();
            if (this.mFreeMem < this.mMemLowThreshold) {
                if (!checkCache) {
                    this.mFreeMemAfterLastCacheClear = this.mFreeMem;
                    if (!this.mLowMemFlag) {
                        Slog.i(TAG, "Running low on memory. Sending notification");
                        sendNotification();
                        this.mLowMemFlag = true;
                    }
                } else if (this.mFreeMem < this.mMemCacheStartTrimThreshold && this.mFreeMemAfterLastCacheClear - this.mFreeMem >= (this.mMemLowThreshold - this.mMemCacheStartTrimThreshold) / 4) {
                    this.mThreadStartTime = System.currentTimeMillis();
                    this.mClearSucceeded = DEBUG;
                    clearCache();
                }
            } else {
                this.mFreeMemAfterLastCacheClear = this.mFreeMem;
                if (this.mLowMemFlag) {
                    Slog.i(TAG, "Memory available. Cancelling notification");
                    cancelNotification();
                    this.mLowMemFlag = DEBUG;
                }
            }
            if (!this.mLowMemFlag && !this.mIsBootImageOnDisk) {
                Slog.i(TAG, "No boot image on disk due to lack of space. Sending notification");
                sendNotification();
            }
            if (this.mFreeMem < this.mMemFullThreshold) {
                if (!this.mMemFullFlag) {
                    sendFullNotification();
                    this.mMemFullFlag = true;
                }
            } else if (this.mMemFullFlag) {
                cancelFullNotification();
                this.mMemFullFlag = DEBUG;
            }
        }
        postCheckMemoryMsg(true, DEFAULT_CHECK_INTERVAL);
    }

    void postCheckMemoryMsg(boolean clearCache, long delay) {
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, clearCache ? 1 : 0, 0), delay);
    }

    public DeviceStorageMonitorService(Context context) {
        super(context);
        this.mLowMemFlag = DEBUG;
        this.mMemFullFlag = DEBUG;
        this.mThreadStartTime = -1L;
        this.mClearSucceeded = DEBUG;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    Slog.e(DeviceStorageMonitorService.TAG, "Will not process invalid message");
                } else {
                    DeviceStorageMonitorService.this.checkMemory(msg.arg1 != 1 ? DeviceStorageMonitorService.DEBUG : true);
                }
            }
        };
        this.mLocalService = new DeviceStorageMonitorInternal() {
            @Override
            public void checkMemory() {
                DeviceStorageMonitorService.this.postCheckMemoryMsg(true, 0L);
            }

            @Override
            public boolean isMemoryLow() {
                if (DeviceStorageMonitorService.this.mLowMemFlag || !DeviceStorageMonitorService.this.mIsBootImageOnDisk) {
                    return true;
                }
                return DeviceStorageMonitorService.DEBUG;
            }

            @Override
            public long getMemoryLowThreshold() {
                return DeviceStorageMonitorService.this.mMemLowThreshold;
            }
        };
        this.mRemoteService = new Binder() {
            @Override
            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (DeviceStorageMonitorService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                    pw.println("Permission Denial: can't dump devicestoragemonitor from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                } else {
                    DeviceStorageMonitorService.this.dumpImpl(pw);
                }
            }
        };
        this.mLastReportedFreeMemTime = 0L;
        this.mResolver = context.getContentResolver();
        this.mIsBootImageOnDisk = isBootImageOnDisk();
        this.mDataFileStats = new StatFs(DATA_PATH.getAbsolutePath());
        this.mSystemFileStats = new StatFs(SYSTEM_PATH.getAbsolutePath());
        this.mCacheFileStats = new StatFs(CACHE_PATH.getAbsolutePath());
        this.mTotalMemory = ((long) this.mDataFileStats.getBlockCount()) * ((long) this.mDataFileStats.getBlockSize());
        this.mStorageLowIntent = new Intent("android.intent.action.DEVICE_STORAGE_LOW");
        this.mStorageLowIntent.addFlags(67108864);
        this.mStorageOkIntent = new Intent("android.intent.action.DEVICE_STORAGE_OK");
        this.mStorageOkIntent.addFlags(67108864);
        this.mStorageFullIntent = new Intent("android.intent.action.DEVICE_STORAGE_FULL");
        this.mStorageFullIntent.addFlags(67108864);
        this.mStorageNotFullIntent = new Intent("android.intent.action.DEVICE_STORAGE_NOT_FULL");
        this.mStorageNotFullIntent.addFlags(67108864);
    }

    private static boolean isBootImageOnDisk() {
        String[] arr$ = PackageManagerService.getAllDexCodeInstructionSets();
        for (String instructionSet : arr$) {
            if (!VMRuntime.isBootClassPathOnDisk(instructionSet)) {
                return DEBUG;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        StorageManager sm = StorageManager.from(getContext());
        this.mMemLowThreshold = sm.getStorageLowBytes(DATA_PATH);
        this.mMemFullThreshold = sm.getStorageFullBytes(DATA_PATH);
        this.mMemCacheStartTrimThreshold = ((this.mMemLowThreshold * 3) + this.mMemFullThreshold) / 4;
        this.mMemCacheTrimToThreshold = this.mMemLowThreshold + ((this.mMemLowThreshold - this.mMemCacheStartTrimThreshold) * 2);
        this.mFreeMemAfterLastCacheClear = this.mTotalMemory;
        checkMemory(true);
        this.mCacheFileDeletedObserver = new CacheFileDeletedObserver();
        this.mCacheFileDeletedObserver.startWatching();
        publishBinderService(SERVICE, this.mRemoteService);
        publishLocalService(DeviceStorageMonitorInternal.class, this.mLocalService);
    }

    void dumpImpl(PrintWriter pw) {
        Context context = getContext();
        pw.println("Current DeviceStorageMonitor state:");
        pw.print("  mFreeMem=");
        pw.print(Formatter.formatFileSize(context, this.mFreeMem));
        pw.print(" mTotalMemory=");
        pw.println(Formatter.formatFileSize(context, this.mTotalMemory));
        pw.print("  mFreeMemAfterLastCacheClear=");
        pw.println(Formatter.formatFileSize(context, this.mFreeMemAfterLastCacheClear));
        pw.print("  mLastReportedFreeMem=");
        pw.print(Formatter.formatFileSize(context, this.mLastReportedFreeMem));
        pw.print(" mLastReportedFreeMemTime=");
        TimeUtils.formatDuration(this.mLastReportedFreeMemTime, SystemClock.elapsedRealtime(), pw);
        pw.println();
        pw.print("  mLowMemFlag=");
        pw.print(this.mLowMemFlag);
        pw.print(" mMemFullFlag=");
        pw.println(this.mMemFullFlag);
        pw.print(" mIsBootImageOnDisk=");
        pw.print(this.mIsBootImageOnDisk);
        pw.print("  mClearSucceeded=");
        pw.print(this.mClearSucceeded);
        pw.print(" mClearingCache=");
        pw.println(this.mClearingCache);
        pw.print("  mMemLowThreshold=");
        pw.print(Formatter.formatFileSize(context, this.mMemLowThreshold));
        pw.print(" mMemFullThreshold=");
        pw.println(Formatter.formatFileSize(context, this.mMemFullThreshold));
        pw.print("  mMemCacheStartTrimThreshold=");
        pw.print(Formatter.formatFileSize(context, this.mMemCacheStartTrimThreshold));
        pw.print(" mMemCacheTrimToThreshold=");
        pw.println(Formatter.formatFileSize(context, this.mMemCacheTrimToThreshold));
    }

    private void sendNotification() {
        Context context = getContext();
        EventLog.writeEvent(EventLogTags.LOW_STORAGE, this.mFreeMem);
        Intent lowMemIntent = new Intent(Environment.isExternalStorageEmulated() ? "android.settings.INTERNAL_STORAGE_SETTINGS" : "android.intent.action.MANAGE_PACKAGE_STORAGE");
        lowMemIntent.putExtra("memory", this.mFreeMem);
        lowMemIntent.addFlags(268435456);
        NotificationManager mNotificationMgr = (NotificationManager) context.getSystemService("notification");
        CharSequence title = context.getText(R.string.geofencing_service);
        CharSequence details = context.getText(this.mIsBootImageOnDisk ? R.string.global_action_assist : R.string.global_action_bug_report);
        PendingIntent intent = PendingIntent.getActivityAsUser(context, 0, lowMemIntent, 0, null, UserHandle.CURRENT);
        Notification notification = new Notification.Builder(context).setSmallIcon(R.drawable.input_method_item_background_selected).setTicker(title).setColor(context.getResources().getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(details).setContentIntent(intent).setStyle(new Notification.BigTextStyle().bigText(details)).setVisibility(1).setCategory("sys").build();
        notification.flags |= 32;
        mNotificationMgr.notifyAsUser(null, 1, notification, UserHandle.ALL);
        context.sendStickyBroadcastAsUser(this.mStorageLowIntent, UserHandle.ALL);
    }

    private void cancelNotification() {
        Context context = getContext();
        NotificationManager mNotificationMgr = (NotificationManager) context.getSystemService("notification");
        mNotificationMgr.cancelAsUser(null, 1, UserHandle.ALL);
        context.removeStickyBroadcastAsUser(this.mStorageLowIntent, UserHandle.ALL);
        context.sendBroadcastAsUser(this.mStorageOkIntent, UserHandle.ALL);
    }

    private void sendFullNotification() {
        getContext().sendStickyBroadcastAsUser(this.mStorageFullIntent, UserHandle.ALL);
    }

    private void cancelFullNotification() {
        getContext().removeStickyBroadcastAsUser(this.mStorageFullIntent, UserHandle.ALL);
        getContext().sendBroadcastAsUser(this.mStorageNotFullIntent, UserHandle.ALL);
    }

    private static class CacheFileDeletedObserver extends FileObserver {
        public CacheFileDeletedObserver() {
            super(Environment.getDownloadCacheDirectory().getAbsolutePath(), 512);
        }

        @Override
        public void onEvent(int event, String path) {
            EventLogTags.writeCacheFileDeleted(path);
        }
    }
}
