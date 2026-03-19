package com.android.server.storage;

import android.R;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.BenesseExtension;
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
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.EventLogTags;
import com.android.server.SystemService;
import com.android.server.location.LocationFudger;
import com.android.server.pm.InstructionSets;
import com.mediatek.common.MPlugin;
import com.mediatek.common.lowstorage.ILowStorageExt;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DeviceStorageMonitorService extends SystemService {
    private static final int CRITICAL_LOW_THRESHOLD_BYTES = 4194304;
    static final boolean DEBUG = false;
    private static final long DEFAULT_CHECK_INTERVAL = 30000;
    private static final long DEFAULT_DISK_FREE_CHANGE_REPORTING_THRESHOLD = 2097152;
    private static final int DEFAULT_FREE_STORAGE_LOG_INTERVAL_IN_MINUTES = 720;
    private static final int DEFAULT_THRESHOLD_MAX_BYTES = 52428800;
    private static final int DEFAULT_THRESHOLD_PERCENTAGE = 10;
    static final int DEVICE_MEMORY_CRITICAL_LOW = 2;
    static final int DEVICE_MEMORY_WHAT = 1;
    private static final int EMAIL_CHECK_SIZE = 52428800;
    private static final int EXCEPTION_LOW_THRESHOLD_BYTES = 10485760;
    private static final int FULL_THRESHOLD_BYTES = 5242880;
    private static final String IPO_POWER_ON = "android.intent.action.ACTION_BOOT_IPO";
    private static final int LOW_MEMORY_NOTIFICATION_ID = 1;
    private static final int MONITOR_INTERVAL = 1;
    static final String SERVICE = "devicestoragemonitor";
    static final String TAG = "DeviceStorageMonitorService";
    private static final int _FALSE = 0;
    private static final int _TRUE = 1;
    static final boolean localLOGV = false;
    ILowStorageExt lse;
    private CacheFileDeletedObserver mCacheFileDeletedObserver;
    private final StatFs mCacheFileStats;
    private long mCacheSize;
    private boolean mCheckAppSize;
    private CachePackageDataObserver mClearCacheObserver;
    boolean mClearSucceeded;
    boolean mClearingCache;
    private long mCodeSize;
    private boolean mConfigChanged;
    private final StatFs mDataFileStats;
    private long mDataSize;
    private AlertDialog mDialog;
    private long mFreeMem;
    private long mFreeMemAfterLastCacheClear;
    private boolean mGetSize;
    private final Handler mHandler;
    private boolean mIPOBootup;
    private BroadcastReceiver mIntentReceiver;
    private final boolean mIsBootImageOnDisk;
    private int mLastCriticalLowLevel;
    private long mLastReportedFreeMem;
    private long mLastReportedFreeMemTime;
    private final DeviceStorageMonitorInternal mLocalService;
    boolean mLowMemFlag;
    private long mMemCacheStartTrimThreshold;
    private long mMemCacheTrimToThreshold;
    private boolean mMemFullFlag;
    private long mMemFullThreshold;
    long mMemLowThreshold;
    private StatFs mQueryDataFs;
    private final IBinder mRemoteService;
    private final ContentResolver mResolver;
    final IPackageStatsObserver.Stub mStatsObserver;
    private final Intent mStorageFullIntent;
    private final Intent mStorageLowIntent;
    private final Intent mStorageNotFullIntent;
    private final Intent mStorageOkIntent;
    private String[] mStrings;
    private final StatFs mSystemFileStats;
    private long mThreadStartTime;
    private final long mTotalMemory;
    private long mTotalSize;
    private static final File DATA_PATH = Environment.getDataDirectory();
    private static final File SYSTEM_PATH = Environment.getRootDirectory();
    private static final File CACHE_PATH = Environment.getDownloadCacheDirectory();

    private class CachePackageDataObserver extends IPackageDataObserver.Stub {
        CachePackageDataObserver(DeviceStorageMonitorService this$0, CachePackageDataObserver cachePackageDataObserver) {
            this();
        }

        private CachePackageDataObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            DeviceStorageMonitorService.this.mClearSucceeded = succeeded;
            DeviceStorageMonitorService.this.mClearingCache = false;
            DeviceStorageMonitorService.this.postCheckMemoryMsg(false, 0L);
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
        CachePackageDataObserver cachePackageDataObserver = null;
        if (this.mClearCacheObserver == null) {
            this.mClearCacheObserver = new CachePackageDataObserver(this, cachePackageDataObserver);
        }
        this.mClearingCache = true;
        try {
            IPackageManager.Stub.asInterface(ServiceManager.getService("package")).freeStorageAndNotify((String) null, this.mMemCacheTrimToThreshold, this.mClearCacheObserver);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to get handle for PackageManger Exception: " + e);
            this.mClearingCache = false;
            this.mClearSucceeded = false;
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
            if (this.lse == null) {
                this.lse = (ILowStorageExt) MPlugin.createInstance("com.mediatek.common.lowstorage.ILowStorageExt", getContext());
                if (this.lse == null) {
                    Slog.e(TAG, "Failed to create LowStorageExt instance.");
                } else {
                    this.lse.init(getContext(), this.mTotalMemory);
                }
            }
            if (this.lse != null) {
                this.lse.checkStorage(this.mFreeMem);
            }
            if (this.mFreeMem < this.mMemLowThreshold) {
                if (this.mCheckAppSize) {
                    this.mCheckAppSize = false;
                    PackageManager pm = getContext().getPackageManager();
                    pm.getPackageSizeInfo("com.android.email", this.mStatsObserver);
                }
                if (checkCache) {
                    this.mThreadStartTime = System.currentTimeMillis();
                    this.mClearSucceeded = false;
                    clearCache();
                } else {
                    this.mFreeMemAfterLastCacheClear = this.mFreeMem;
                    if (!this.mLowMemFlag) {
                        Slog.i(TAG, "Running low on memory. Sending notification");
                        sendNotification();
                        this.mLowMemFlag = true;
                    }
                }
            } else {
                this.mFreeMemAfterLastCacheClear = this.mFreeMem;
                if (this.mLowMemFlag) {
                    this.mCheckAppSize = true;
                    this.mGetSize = false;
                    Slog.i(TAG, "Memory available. Cancelling notification");
                    cancelNotification();
                    this.mLowMemFlag = false;
                }
            }
            if (!this.mLowMemFlag && !this.mIsBootImageOnDisk) {
                Slog.i(TAG, "No boot image on disk due to lack of space. Sending notification");
                sendNotification();
            }
            if (this.mFreeMem < this.mMemFullThreshold) {
                Slog.v(TAG, "Running on storage full,freeStorage=" + this.mFreeMem);
                if (!this.mMemFullFlag) {
                    sendFullNotification();
                    this.mMemFullFlag = true;
                }
            } else if (this.mMemFullFlag) {
                cancelFullNotification();
                this.mMemFullFlag = false;
            }
            int criticalLowLevel = (int) Math.floor(this.mFreeMem / 1048576);
            if (criticalLowLevel < this.mLastCriticalLowLevel && this.mFreeMem < this.mLastCriticalLowLevel * 1024 * 1024 && this.mGetSize) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
                Slog.i(TAG, "Show warning dialog, critical level: " + criticalLowLevel);
                this.mLastCriticalLowLevel = criticalLowLevel;
            }
            if (this.mLastCriticalLowLevel < criticalLowLevel) {
                this.mLastCriticalLowLevel = Math.min(criticalLowLevel, 4);
            }
        }
        postCheckMemoryMsg(true, DEFAULT_CHECK_INTERVAL);
    }

    void postCheckMemoryMsg(boolean clearCache, long delay) {
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, clearCache ? 1 : 0, 0), delay);
    }

    private long getMemThreshold() {
        long value = (this.mTotalMemory * ((long) Settings.Global.getInt(this.mResolver, "sys_storage_threshold_percentage", 10))) / 100;
        long maxValue = Settings.Global.getInt(this.mResolver, "sys_storage_threshold_max_bytes", 52428800);
        return value < maxValue ? value : maxValue;
    }

    public DeviceStorageMonitorService(Context context) {
        super(context);
        this.mLowMemFlag = false;
        this.mMemFullFlag = false;
        this.mThreadStartTime = -1L;
        this.mClearSucceeded = false;
        this.lse = null;
        this.mConfigChanged = false;
        this.mDialog = null;
        this.mLastCriticalLowLevel = 4;
        this.mIPOBootup = false;
        this.mCheckAppSize = true;
        this.mCacheSize = 0L;
        this.mCodeSize = 0L;
        this.mDataSize = 0L;
        this.mTotalSize = 0L;
        this.mStrings = null;
        this.mGetSize = false;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what != 2) {
                    if (msg.what != 1) {
                        Slog.e(DeviceStorageMonitorService.TAG, "Will not process invalid message");
                        return;
                    } else {
                        DeviceStorageMonitorService.this.checkMemory(msg.arg1 == 1);
                        return;
                    }
                }
                if (DeviceStorageMonitorService.this.mDialog == null || DeviceStorageMonitorService.this.mIPOBootup || DeviceStorageMonitorService.this.mConfigChanged) {
                    DeviceStorageMonitorService.this.mIPOBootup = false;
                    if (DeviceStorageMonitorService.this.mConfigChanged) {
                        DeviceStorageMonitorService.this.mConfigChanged = false;
                    }
                    final Context context2 = DeviceStorageMonitorService.this.getContext();
                    AlertDialog.Builder builder = new AlertDialog.Builder(context2).setIcon(R.drawable.ic_dialog_alert).setTitle(context2.getText(com.mediatek.internal.R.string.low_internal_storage_view_title)).setMessage(context2.getText(com.mediatek.internal.R.string.low_storage_warning_message)).setNegativeButton(context2.getText(134545479), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (BenesseExtension.getDchaState() != 0) {
                                return;
                            }
                            Intent mIntent = new Intent("android.settings.INTERNAL_STORAGE_SETTINGS");
                            mIntent.setFlags(268435456);
                            context2.startActivity(mIntent);
                        }
                    }).setPositiveButton(context2.getText(R.string.cancel), (DialogInterface.OnClickListener) null);
                    if (DeviceStorageMonitorService.this.mTotalSize > 52428800) {
                        builder.setNeutralButton(context2.getText(com.mediatek.internal.R.string.deleteMails), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent mIntent = new Intent();
                                mIntent.setAction("android.intent.action.MAIN");
                                mIntent.addCategory("android.intent.category.APP_EMAIL");
                                mIntent.setFlags(268435456);
                                context2.startActivity(mIntent);
                            }
                        });
                    }
                    DeviceStorageMonitorService.this.mDialog = builder.create();
                }
                DeviceStorageMonitorService.this.mDialog.getWindow().setType(2003);
                if (DeviceStorageMonitorService.this.mDialog.isShowing()) {
                    return;
                }
                if (SystemProperties.getInt("ctsrunning", 0) == 0) {
                    DeviceStorageMonitorService.this.mDialog.show();
                } else {
                    Slog.i(DeviceStorageMonitorService.TAG, "In CTS Running,do not show the no space dailog");
                }
            }
        };
        this.mStatsObserver = new IPackageStatsObserver.Stub() {
            public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
                DeviceStorageMonitorService.this.mCacheSize = stats.cacheSize;
                DeviceStorageMonitorService.this.mCodeSize = stats.codeSize;
                DeviceStorageMonitorService.this.mDataSize = stats.dataSize;
                DeviceStorageMonitorService.this.mTotalSize = DeviceStorageMonitorService.this.mCacheSize + DeviceStorageMonitorService.this.mCodeSize + DeviceStorageMonitorService.this.mDataSize;
                DeviceStorageMonitorService.this.mGetSize = true;
                Slog.v(DeviceStorageMonitorService.TAG, "mStatsObserver  mCacheSize = " + DeviceStorageMonitorService.this.mCacheSize + "mCodeSize = " + DeviceStorageMonitorService.this.mCodeSize + "mDataSize=" + DeviceStorageMonitorService.this.mDataSize + "mTotalSize=" + DeviceStorageMonitorService.this.mTotalSize);
            }
        };
        this.mLocalService = new DeviceStorageMonitorInternal() {
            @Override
            public void checkMemory() {
                DeviceStorageMonitorService.this.postCheckMemoryMsg(true, 0L);
            }

            @Override
            public boolean isMemoryLow() {
                return DeviceStorageMonitorService.this.mLowMemFlag || !DeviceStorageMonitorService.this.mIsBootImageOnDisk;
            }

            @Override
            public long getMemoryLowThreshold() {
                return DeviceStorageMonitorService.this.mMemLowThreshold;
            }

            @Override
            public boolean isMemoryCriticalLow() {
                long tempFreeMem;
                try {
                    DeviceStorageMonitorService.this.mQueryDataFs.restat(DeviceStorageMonitorService.DATA_PATH.getAbsolutePath());
                    tempFreeMem = ((long) DeviceStorageMonitorService.this.mQueryDataFs.getAvailableBlocks()) * ((long) DeviceStorageMonitorService.this.mQueryDataFs.getBlockSize());
                } catch (IllegalArgumentException e) {
                    tempFreeMem = DeviceStorageMonitorService.this.mFreeMem;
                    Slog.v(DeviceStorageMonitorService.TAG, "Failed to get current free storage size.");
                }
                if (tempFreeMem <= 10485760) {
                    return true;
                }
                return false;
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
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.ACTION_BOOT_IPO")) {
                    DeviceStorageMonitorService.this.mIPOBootup = true;
                    DeviceStorageMonitorService.this.mLowMemFlag = false;
                }
                if (!action.equals("android.intent.action.LOCALE_CHANGED")) {
                    return;
                }
                DeviceStorageMonitorService.this.mConfigChanged = true;
                if (DeviceStorageMonitorService.this.mDialog == null) {
                    return;
                }
                DeviceStorageMonitorService.this.mDialog.cancel();
            }
        };
        this.mLastReportedFreeMemTime = 0L;
        this.mResolver = context.getContentResolver();
        this.mIsBootImageOnDisk = isBootImageOnDisk();
        this.mDataFileStats = new StatFs(DATA_PATH.getAbsolutePath());
        this.mSystemFileStats = new StatFs(SYSTEM_PATH.getAbsolutePath());
        this.mCacheFileStats = new StatFs(CACHE_PATH.getAbsolutePath());
        this.mQueryDataFs = new StatFs(DATA_PATH.getAbsolutePath());
        this.mTotalMemory = ((long) this.mDataFileStats.getBlockCount()) * ((long) this.mDataFileStats.getBlockSize());
        SystemProperties.set("sys.lowstorage_flag", "0");
        this.mStorageLowIntent = new Intent("android.intent.action.DEVICE_STORAGE_LOW");
        this.mStorageLowIntent.addFlags(67108864);
        this.mStorageOkIntent = new Intent("android.intent.action.DEVICE_STORAGE_OK");
        this.mStorageOkIntent.addFlags(67108864);
        this.mStorageFullIntent = new Intent("android.intent.action.DEVICE_STORAGE_FULL");
        this.mStorageFullIntent.addFlags(67108864);
        this.mStorageNotFullIntent = new Intent("android.intent.action.DEVICE_STORAGE_NOT_FULL");
        this.mStorageNotFullIntent.addFlags(67108864);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_BOOT_IPO");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        context.registerReceiver(this.mIntentReceiver, filter);
    }

    private static boolean isBootImageOnDisk() {
        for (String instructionSet : InstructionSets.getAllDexCodeInstructionSets()) {
            if (!VMRuntime.isBootClassPathOnDisk(instructionSet)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        this.mMemLowThreshold = getMemThreshold();
        this.mMemFullThreshold = 5242880L;
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
        Intent lowMemIntent = new Intent("android.settings.INTERNAL_STORAGE_SETTINGS");
        lowMemIntent.putExtra("memory", this.mFreeMem);
        lowMemIntent.addFlags(268435456);
        NotificationManager mNotificationMgr = (NotificationManager) context.getSystemService("notification");
        CharSequence title = context.getText(com.mediatek.internal.R.string.low_internal_storage_view_title);
        CharSequence details = context.getText(this.mIsBootImageOnDisk ? R.string.dlg_ok : R.string.done_accessibility_shortcut_menu_button);
        PendingIntent intent = PendingIntent.getActivityAsUser(context, 0, lowMemIntent, 0, null, UserHandle.CURRENT);
        if (BenesseExtension.getDchaState() != 0) {
            intent = null;
        }
        Notification notification = new Notification.Builder(context).setSmallIcon(R.drawable.list_selector_background_disabled).setTicker(title).setColor(context.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(details).setContentIntent(intent).setStyle(new Notification.BigTextStyle().bigText(details)).setVisibility(1).setCategory("sys").build();
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
