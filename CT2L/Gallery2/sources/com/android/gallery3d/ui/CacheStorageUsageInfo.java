package com.android.gallery3d.ui;

import android.content.Context;
import android.os.StatFs;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.util.ThreadPool;
import java.io.File;

public class CacheStorageUsageInfo {
    private AbstractGalleryActivity mActivity;
    private Context mContext;
    private long mTargetCacheBytes;
    private long mTotalBytes;
    private long mUsedBytes;
    private long mUsedCacheBytes;
    private long mUserChangeDelta;

    public CacheStorageUsageInfo(AbstractGalleryActivity activity) {
        this.mActivity = activity;
        this.mContext = activity.getAndroidContext();
    }

    public void increaseTargetCacheSize(long delta) {
        this.mUserChangeDelta += delta;
    }

    public void loadStorageInfo(ThreadPool.JobContext jc) {
        File cacheDir = this.mContext.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = this.mContext.getCacheDir();
        }
        String path = cacheDir.getAbsolutePath();
        StatFs stat = new StatFs(path);
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long totalBlocks = stat.getBlockCount();
        this.mTotalBytes = blockSize * totalBlocks;
        this.mUsedBytes = (totalBlocks - availableBlocks) * blockSize;
        this.mUsedCacheBytes = this.mActivity.getDataManager().getTotalUsedCacheSize();
        this.mTargetCacheBytes = this.mActivity.getDataManager().getTotalTargetCacheSize();
    }

    public long getTotalBytes() {
        return this.mTotalBytes;
    }

    public long getExpectedUsedBytes() {
        return (this.mUsedBytes - this.mUsedCacheBytes) + this.mTargetCacheBytes + this.mUserChangeDelta;
    }

    public long getUsedBytes() {
        return this.mUsedBytes;
    }

    public long getFreeBytes() {
        return this.mTotalBytes - this.mUsedBytes;
    }
}
