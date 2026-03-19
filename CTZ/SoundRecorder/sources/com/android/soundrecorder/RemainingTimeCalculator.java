package com.android.soundrecorder;

import android.os.Environment;
import android.os.StatFs;
import java.io.File;

class RemainingTimeCalculator {
    private long mBlocksChangedTime;
    private int mBytesPerSecond;
    private long mFileSizeChangedTime;
    private long mLastBlocks;
    private long mLastFileSize;
    private long mMaxBytes;
    private File mRecordingFile;
    private int mCurrentLowerLimit = 0;
    private File mSDCardDirectory = Environment.getExternalStorageDirectory();

    public void setFileSizeLimit(File file, long j) {
        this.mRecordingFile = file;
        this.mMaxBytes = j;
    }

    public void reset() {
        this.mCurrentLowerLimit = 0;
        this.mBlocksChangedTime = -1L;
        this.mFileSizeChangedTime = -1L;
    }

    public long timeRemaining() {
        StatFs statFs = new StatFs(this.mSDCardDirectory.getAbsolutePath());
        long availableBlocks = statFs.getAvailableBlocks();
        long blockSize = statFs.getBlockSize();
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.mBlocksChangedTime == -1 || availableBlocks != this.mLastBlocks) {
            this.mBlocksChangedTime = jCurrentTimeMillis;
            this.mLastBlocks = availableBlocks;
        }
        long j = ((this.mLastBlocks * blockSize) / ((long) this.mBytesPerSecond)) - ((jCurrentTimeMillis - this.mBlocksChangedTime) / 1000);
        if (this.mRecordingFile == null) {
            this.mCurrentLowerLimit = 2;
            return j;
        }
        this.mRecordingFile = new File(this.mRecordingFile.getAbsolutePath());
        long length = this.mRecordingFile.length();
        if (this.mFileSizeChangedTime == -1 || length != this.mLastFileSize) {
            this.mFileSizeChangedTime = jCurrentTimeMillis;
            this.mLastFileSize = length;
        }
        long j2 = (((this.mMaxBytes - length) / ((long) this.mBytesPerSecond)) - ((jCurrentTimeMillis - this.mFileSizeChangedTime) / 1000)) - 1;
        this.mCurrentLowerLimit = j >= j2 ? 1 : 2;
        return Math.min(j, j2);
    }

    public int currentLowerLimit() {
        return this.mCurrentLowerLimit;
    }

    public boolean diskSpaceAvailable() {
        return new StatFs(this.mSDCardDirectory.getAbsolutePath()).getAvailableBlocks() > 1;
    }

    public void setBitRate(int i) {
        this.mBytesPerSecond = i / 8;
    }
}
