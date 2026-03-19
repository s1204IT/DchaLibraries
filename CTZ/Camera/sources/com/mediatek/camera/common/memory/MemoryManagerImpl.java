package com.mediatek.camera.common.memory;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.memory.IMemoryManager;
import com.mediatek.camera.portability.memory.MemoryInfoManager;

public class MemoryManagerImpl implements ComponentCallbacks2, IMemoryManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MemoryManagerImpl.class.getSimpleName());
    private Context mContext;
    private int mCount;
    private long mLeftStorage;
    private IMemoryManager.IMemoryListener mListener;
    private final long mMiniMemFreeMb;
    private long mPengdingSize;
    private long mStartTime;
    private int mSuitableSpeed;
    private final long mSystemSlowdownThreshold;
    private final long mSystemStopThreshold;
    private long mUsedStorage;
    private Runtime mRuntime = Runtime.getRuntime();
    private MemoryInfoManager mMemoryInfoManager = new MemoryInfoManager();
    private final long mMaxDvmMemory = this.mRuntime.maxMemory();
    private final long mDvmSlowdownThreshold = (long) (0.4f * this.mMaxDvmMemory);
    private final long mDvmStopThreshold = (long) (0.1f * this.mMaxDvmMemory);

    public MemoryManagerImpl(Context context) {
        long j;
        this.mContext = context;
        MemoryInfoManager.MemoryDetailInfo memoryDetailInfo = new MemoryInfoManager.MemoryDetailInfo();
        ((ActivityManager) context.getSystemService("activity")).getMemoryInfo(memoryDetailInfo);
        this.mMiniMemFreeMb = (memoryDetailInfo.getForgroundAppThreshold() / 1024) / 1024;
        LogHelper.d(TAG, "mMiniMemFreeMb = " + this.mMiniMemFreeMb);
        if (this.mMiniMemFreeMb <= 36) {
            j = 4;
        } else if (this.mMaxDvmMemory >= 512) {
            j = 1;
        } else {
            j = 2;
        }
        this.mSystemSlowdownThreshold = 100 / j;
        this.mSystemStopThreshold = this.mSystemSlowdownThreshold / 2;
        LogHelper.d(TAG, "MemoryManagerImpl, mDvmSlowdownThreshold: " + this.mDvmSlowdownThreshold + ", mDvmStopThreshold: " + this.mDvmStopThreshold + ", mSystemSlowdownThreshold: " + this.mSystemSlowdownThreshold + ", mSystemStopThreshold: " + this.mSystemStopThreshold);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
    }

    @Override
    public void onLowMemory() {
        LogHelper.i(TAG, "onLowMemory");
        onMemoryStateChanged(IMemoryManager.MemoryAction.STOP);
    }

    @Override
    public void onTrimMemory(int i) {
        if (i == 15 || i == 40) {
            LogHelper.i(TAG, "onTrimMemory, info: " + i);
            doSystemMemoryCheckAction(this.mSystemSlowdownThreshold, IMemoryManager.MemoryAction.ADJUST_SPEED);
            return;
        }
        if (i == 60 || i == 80) {
            LogHelper.i(TAG, "onTrimMemory, info: " + i);
            doSystemMemoryCheckAction(this.mSystemStopThreshold, IMemoryManager.MemoryAction.STOP);
        }
    }

    public void addListener(IMemoryManager.IMemoryListener iMemoryListener) {
        if (this.mContext != null) {
            this.mContext.registerComponentCallbacks(this);
        }
        this.mListener = iMemoryListener;
    }

    public void removeListener(IMemoryManager.IMemoryListener iMemoryListener) {
        if (this.mContext != null) {
            this.mContext.unregisterComponentCallbacks(this);
        }
        this.mListener = null;
    }

    public void initStateForCapture(long j) {
        onMemoryStateChanged(IMemoryManager.MemoryAction.NORMAL);
        this.mLeftStorage = j;
        this.mUsedStorage = 0L;
        this.mPengdingSize = 0L;
        this.mCount = 0;
    }

    public void initStartTime() {
        this.mStartTime = System.currentTimeMillis();
    }

    public void checkContinuousShotMemoryAction(long j, long j2) {
        if (this.mListener == null) {
            return;
        }
        this.mCount++;
        this.mUsedStorage += j;
        this.mPengdingSize = j2;
        long jCurrentTimeMillis = System.currentTimeMillis() - this.mStartTime;
        long j3 = (((long) this.mCount) * 1024) / jCurrentTimeMillis;
        long j4 = ((this.mUsedStorage - this.mPengdingSize) / jCurrentTimeMillis) / 1024;
        LogHelper.d(TAG, "[checkContinuousShotMemoryAction]Capture speed=" + j3 + " fps, Save speed=" + j4 + " MB/s");
        if (this.mUsedStorage >= this.mLeftStorage) {
            LogHelper.d(TAG, "checkContinuousShotMemoryAction, usedMemory > availableMemory,stop! used: " + this.mUsedStorage + ", available: " + this.mLeftStorage);
            onMemoryStateChanged(IMemoryManager.MemoryAction.STOP);
            return;
        }
        this.mSuitableSpeed = (int) (((((this.mUsedStorage - this.mPengdingSize) * ((long) this.mCount)) * 1024) / jCurrentTimeMillis) / this.mUsedStorage);
        if (doSystemMemoryCheckAction(this.mSystemStopThreshold, IMemoryManager.MemoryAction.STOP) || doSystemMemoryCheckAction(this.mSystemSlowdownThreshold, IMemoryManager.MemoryAction.ADJUST_SPEED)) {
            return;
        }
        if (this.mPengdingSize >= this.mDvmSlowdownThreshold) {
            LogHelper.i(TAG, "checkContinuousShotMemoryAction, DvmSlowdownThreshold reached, mPengdingSize = " + this.mPengdingSize);
            onMemoryStateChanged(IMemoryManager.MemoryAction.ADJUST_SPEED);
            return;
        }
        long jFreeMemory = this.mRuntime.totalMemory() - this.mRuntime.freeMemory();
        LogHelper.d(TAG, "checkContinuousShotMemoryAction, process total memory: " + this.mRuntime.totalMemory() + ", real used memory: " + jFreeMemory);
        if (this.mMaxDvmMemory - jFreeMemory <= this.mDvmStopThreshold) {
            LogHelper.i(TAG, "checkContinuousShotMemoryAction, DvmStopThreshold reached ");
            onMemoryStateChanged(IMemoryManager.MemoryAction.STOP);
        }
    }

    public void checkOneShotMemoryAction(long j) {
        if (this.mListener == null) {
            return;
        }
        LogHelper.d(TAG, "checkOneShotMemoryAction, pictureSize: " + j);
        if (doSystemMemoryCheckAction(this.mSystemStopThreshold + toMb(j), IMemoryManager.MemoryAction.STOP)) {
            return;
        }
        long jFreeMemory = this.mRuntime.totalMemory() - this.mRuntime.freeMemory();
        LogHelper.d(TAG, "checkOneShotMemoryAction, process total memory: " + this.mRuntime.totalMemory() + ", real used memory: " + jFreeMemory);
        if (this.mMaxDvmMemory - jFreeMemory <= this.mDvmStopThreshold + j) {
            LogHelper.i(TAG, "checkOneShotMemoryAction, DvmStopThreshold reached ");
            onMemoryStateChanged(IMemoryManager.MemoryAction.STOP);
        } else {
            onMemoryStateChanged(IMemoryManager.MemoryAction.NORMAL);
        }
    }

    private boolean doSystemMemoryCheckAction(long j, IMemoryManager.MemoryAction memoryAction) {
        long systemFreeMemory = getSystemFreeMemory() - this.mMiniMemFreeMb;
        if (systemFreeMemory < j) {
            LogHelper.d(TAG, "doSystemMemoryCheckAction, info: " + systemFreeMemory + " < " + j + ", " + memoryAction);
            onMemoryStateChanged(memoryAction);
            return true;
        }
        return false;
    }

    private long getSystemFreeMemory() {
        this.mMemoryInfoManager.readMemInfo();
        return (this.mMemoryInfoManager.getCachedSizeKb() / 1024) + (this.mMemoryInfoManager.getFreeSizeKb() / 1024);
    }

    private void onMemoryStateChanged(IMemoryManager.MemoryAction memoryAction) {
        if (this.mListener != null) {
            this.mListener.onMemoryStateChanged(memoryAction);
        }
    }

    private long toMb(long j) {
        return (j / 1024) / 1024;
    }
}
