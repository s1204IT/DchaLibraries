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

    public void setFileSizeLimit(File file, long maxBytes) {
        this.mRecordingFile = file;
        this.mMaxBytes = maxBytes;
    }

    public void reset() {
        this.mCurrentLowerLimit = 0;
        this.mBlocksChangedTime = -1L;
        this.mFileSizeChangedTime = -1L;
    }

    public long timeRemaining() {
        StatFs fs = new StatFs(this.mSDCardDirectory.getAbsolutePath());
        long blocks = fs.getAvailableBlocks();
        long blockSize = fs.getBlockSize();
        long now = System.currentTimeMillis();
        if (this.mBlocksChangedTime == -1 || blocks != this.mLastBlocks) {
            this.mBlocksChangedTime = now;
            this.mLastBlocks = blocks;
        }
        long result = ((this.mLastBlocks * blockSize) / ((long) this.mBytesPerSecond)) - ((now - this.mBlocksChangedTime) / 1000);
        if (this.mRecordingFile == null) {
            this.mCurrentLowerLimit = 2;
            return result;
        }
        this.mRecordingFile = new File(this.mRecordingFile.getAbsolutePath());
        long fileSize = this.mRecordingFile.length();
        if (this.mFileSizeChangedTime == -1 || fileSize != this.mLastFileSize) {
            this.mFileSizeChangedTime = now;
            this.mLastFileSize = fileSize;
        }
        long result2 = (((this.mMaxBytes - fileSize) / ((long) this.mBytesPerSecond)) - ((now - this.mFileSizeChangedTime) / 1000)) - 1;
        this.mCurrentLowerLimit = result < result2 ? 2 : 1;
        return Math.min(result, result2);
    }

    public int currentLowerLimit() {
        return this.mCurrentLowerLimit;
    }

    public boolean diskSpaceAvailable() {
        StatFs fs = new StatFs(this.mSDCardDirectory.getAbsolutePath());
        return fs.getAvailableBlocks() > 1;
    }

    public void setBitRate(int bitRate) {
        this.mBytesPerSecond = bitRate / 8;
    }
}
