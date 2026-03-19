package com.mediatek.camera.portability.memory;

import android.app.ActivityManager;
import com.android.internal.util.MemInfoReader;

public class MemoryInfoManager {
    private MemInfoReader mInfoReader = new MemInfoReader();
    private long[] mInfos;

    public void readMemInfo() {
        this.mInfoReader.readMemInfo();
        this.mInfos = this.mInfoReader.getRawInfo();
    }

    public long getCachedSizeKb() {
        return this.mInfos[3];
    }

    public long getFreeSizeKb() {
        return this.mInfos[1];
    }

    public static class MemoryDetailInfo extends ActivityManager.MemoryInfo {
        public long getForgroundAppThreshold() {
            return ((ActivityManager.MemoryInfo) this).foregroundAppThreshold;
        }
    }
}
