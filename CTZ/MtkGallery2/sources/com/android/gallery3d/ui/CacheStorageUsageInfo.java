package com.android.gallery3d.ui;

import android.content.Context;
import android.os.StatFs;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import java.io.File;

public class CacheStorageUsageInfo {
    private static final String TAG = "Gallery2/CacheStorageUsageInfo";
    private AbstractGalleryActivity mActivity;
    private Context mContext;
    private long mTargetCacheBytes;
    private long mTotalBytes;
    private long mUsedBytes;
    private long mUsedCacheBytes;
    private long mUserChangeDelta;

    public CacheStorageUsageInfo(AbstractGalleryActivity abstractGalleryActivity) {
        this.mActivity = abstractGalleryActivity;
        this.mContext = abstractGalleryActivity.getAndroidContext();
    }

    public void increaseTargetCacheSize(long j) {
        this.mUserChangeDelta += j;
    }

    public void loadStorageInfo(ThreadPool.JobContext jobContext) {
        File externalCacheDir = FeatureHelper.getExternalCacheDir(this.mContext);
        if (externalCacheDir == null) {
            com.mediatek.gallery3d.util.Log.d(TAG, "<loadStorageInfo> failed to get cache dir, call Context.getCacheDir()");
            externalCacheDir = this.mContext.getCacheDir();
        }
        StatFs statFs = new StatFs(externalCacheDir.getAbsolutePath());
        long blockSize = statFs.getBlockSize();
        long availableBlocks = statFs.getAvailableBlocks();
        long blockCount = statFs.getBlockCount();
        this.mTotalBytes = blockSize * blockCount;
        this.mUsedBytes = blockSize * (blockCount - availableBlocks);
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
