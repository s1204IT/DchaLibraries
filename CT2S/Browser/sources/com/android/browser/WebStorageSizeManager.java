package com.android.browser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.StatFs;
import android.webkit.WebStorage;
import com.android.browser.preferences.WebsiteSettingsFragment;
import java.io.File;

public class WebStorageSizeManager {
    private static long mLastOutOfSpaceNotificationTime = -1;
    private long mAppCacheMaxSize;
    private final Context mContext;
    private DiskInfo mDiskInfo;
    private final long mGlobalLimit = getGlobalLimit();

    public interface AppCacheInfo {
        long getAppCacheSizeBytes();
    }

    public interface DiskInfo {
        long getFreeSpaceSizeBytes();

        long getTotalSizeBytes();
    }

    public static class StatFsDiskInfo implements DiskInfo {
        private StatFs mFs;

        public StatFsDiskInfo(String path) {
            this.mFs = new StatFs(path);
        }

        @Override
        public long getFreeSpaceSizeBytes() {
            return ((long) this.mFs.getAvailableBlocks()) * ((long) this.mFs.getBlockSize());
        }

        @Override
        public long getTotalSizeBytes() {
            return ((long) this.mFs.getBlockCount()) * ((long) this.mFs.getBlockSize());
        }
    }

    public static class WebKitAppCacheInfo implements AppCacheInfo {
        private String mAppCachePath;

        public WebKitAppCacheInfo(String path) {
            this.mAppCachePath = path;
        }

        @Override
        public long getAppCacheSizeBytes() {
            File file = new File(this.mAppCachePath + File.separator + "ApplicationCache.db");
            return file.length();
        }
    }

    public WebStorageSizeManager(Context ctx, DiskInfo diskInfo, AppCacheInfo appCacheInfo) {
        this.mContext = ctx.getApplicationContext();
        this.mDiskInfo = diskInfo;
        this.mAppCacheMaxSize = Math.max(this.mGlobalLimit / 4, appCacheInfo.getAppCacheSizeBytes());
    }

    public long getAppCacheMaxSize() {
        return this.mAppCacheMaxSize;
    }

    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
        long newOriginQuota;
        long totalUnusedQuota = (this.mGlobalLimit - totalUsedQuota) - this.mAppCacheMaxSize;
        if (totalUnusedQuota <= 0) {
            if (totalUsedQuota > 0) {
                scheduleOutOfSpaceNotification();
            }
            quotaUpdater.updateQuota(currentQuota);
            return;
        }
        if (currentQuota != 0) {
            long quotaIncrease = estimatedSize == 0 ? Math.min(1048576L, totalUnusedQuota) : estimatedSize;
            newOriginQuota = currentQuota + quotaIncrease;
            if (quotaIncrease > totalUnusedQuota) {
                newOriginQuota = currentQuota;
            }
        } else if (totalUnusedQuota >= estimatedSize) {
            newOriginQuota = estimatedSize;
        } else {
            newOriginQuota = 0;
        }
        quotaUpdater.updateQuota(newOriginQuota);
    }

    public void onReachedMaxAppCacheSize(long spaceNeeded, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
        long totalUnusedQuota = (this.mGlobalLimit - totalUsedQuota) - this.mAppCacheMaxSize;
        if (totalUnusedQuota < spaceNeeded + 524288) {
            if (totalUsedQuota > 0) {
                scheduleOutOfSpaceNotification();
            }
            quotaUpdater.updateQuota(0L);
        } else {
            this.mAppCacheMaxSize += spaceNeeded + 524288;
            quotaUpdater.updateQuota(this.mAppCacheMaxSize);
        }
    }

    public static void resetLastOutOfSpaceNotificationTime() {
        mLastOutOfSpaceNotificationTime = (System.currentTimeMillis() - 300000) + 3000;
    }

    private long getGlobalLimit() {
        long freeSpace = this.mDiskInfo.getFreeSpaceSizeBytes();
        long fileSystemSize = this.mDiskInfo.getTotalSizeBytes();
        return calculateGlobalLimit(fileSystemSize, freeSpace);
    }

    static long calculateGlobalLimit(long fileSystemSizeBytes, long freeSpaceBytes) {
        if (fileSystemSizeBytes <= 0 || freeSpaceBytes <= 0 || freeSpaceBytes > fileSystemSizeBytes) {
            return 0L;
        }
        long fileSystemSizeRatio = 2 << ((int) Math.floor(Math.log10(fileSystemSizeBytes / 1048576)));
        long maxSizeBytes = (long) Math.min(Math.floor(fileSystemSizeBytes / fileSystemSizeRatio), Math.floor(freeSpaceBytes / 2));
        if (maxSizeBytes < 1048576) {
            return 0L;
        }
        long roundingExtra = maxSizeBytes % 1048576 != 0 ? 1L : 0L;
        return 1048576 * ((maxSizeBytes / 1048576) + roundingExtra);
    }

    private void scheduleOutOfSpaceNotification() {
        if (mLastOutOfSpaceNotificationTime == -1 || System.currentTimeMillis() - mLastOutOfSpaceNotificationTime > 300000) {
            CharSequence title = this.mContext.getString(R.string.webstorage_outofspace_notification_title);
            CharSequence text = this.mContext.getString(R.string.webstorage_outofspace_notification_text);
            long when = System.currentTimeMillis();
            Intent intent = new Intent(this.mContext, (Class<?>) BrowserPreferencesPage.class);
            intent.putExtra(":android:show_fragment", WebsiteSettingsFragment.class.getName());
            PendingIntent contentIntent = PendingIntent.getActivity(this.mContext, 0, intent, 0);
            Notification notification = new Notification(android.R.drawable.stat_sys_warning, title, when);
            notification.setLatestEventInfo(this.mContext, title, text, contentIntent);
            notification.flags |= 16;
            NotificationManager mgr = (NotificationManager) this.mContext.getSystemService("notification");
            if (mgr != null) {
                mLastOutOfSpaceNotificationTime = System.currentTimeMillis();
                mgr.notify(1, notification);
            }
        }
    }
}
