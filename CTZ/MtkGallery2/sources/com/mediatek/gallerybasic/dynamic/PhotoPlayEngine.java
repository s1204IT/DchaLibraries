package com.mediatek.gallerybasic.dynamic;

import android.os.SystemClock;
import com.mediatek.gallerybasic.base.LayerManager;
import com.mediatek.gallerybasic.base.MediaCenter;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.PlayEngine;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.base.ThumbType;
import com.mediatek.gallerybasic.dynamic.PlayList;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MTexture;
import com.mediatek.gallerybasic.util.DebugUtils;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.galleryportable.TraceHelper;

public class PhotoPlayEngine extends PlayEngine implements Player.OnFrameAvailableListener, PlayList.EntryFilling {
    static final boolean $assertionsDisabled = false;
    private static final int RENDER_DEBUG_COLOR = 1140915968;
    private static final String TAG = "MtkGallery2/PhotoPlayEngine";
    private PlayEngine.OnFrameAvailableListener mFrameAvailableListener;
    private LayerManager mLayerManager;
    private MediaCenter mMediaCenter;
    private int mMiddleIndex;
    private int mPlayCount;
    private PlayList mPlayList;
    private int mPlayRangeBegin;
    private int mPlayRangeEnd;
    private PlayThreads mPlayThreads;
    private Status mStatus = Status.NEW;
    private int[] mSubmitTaskOrder;
    private ThumbType mThumbType;
    private int mTotalCount;
    private int mWorkThreadNum;

    private enum Status {
        NEW,
        RESUMED,
        PAUSED
    }

    public PhotoPlayEngine(MediaCenter mediaCenter, int i, int i2, int i3, ThumbType thumbType) {
        TraceHelper.beginSection(">>>>PhotoPlayEngine-new");
        this.mMediaCenter = mediaCenter;
        this.mTotalCount = i;
        this.mPlayCount = i2;
        this.mPlayRangeBegin = (this.mTotalCount - this.mPlayCount) / 2;
        this.mPlayRangeEnd = (this.mPlayRangeBegin + this.mPlayCount) - 1;
        this.mMiddleIndex = this.mTotalCount / 2;
        this.mWorkThreadNum = i3;
        this.mPlayList = new PlayList(this.mTotalCount, this);
        initTaskOrder();
        this.mThumbType = thumbType;
        this.mPlayThreads = new PlayThreads(this.mWorkThreadNum);
        logAfterNew();
        TraceHelper.endSection();
    }

    @Override
    public void setOnFrameAvailableListener(PlayEngine.OnFrameAvailableListener onFrameAvailableListener) {
        this.mFrameAvailableListener = onFrameAvailableListener;
    }

    @Override
    public void setLayerManager(LayerManager layerManager) {
        this.mLayerManager = layerManager;
    }

    @Override
    public synchronized void resume() {
        TraceHelper.beginSection(">>>>PhotoPlayEngine-resume");
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.mPlayThreads.start();
        this.mStatus = Status.RESUMED;
        Log.d(TAG, "<resume> cost " + (SystemClock.uptimeMillis() - jUptimeMillis) + " ms");
        TraceHelper.endSection();
    }

    @Override
    public synchronized void pause() {
        TraceHelper.beginSection(">>>>PhotoPlayEngine-pause");
        long jUptimeMillis = SystemClock.uptimeMillis();
        updateData(new MediaData[this.mTotalCount]);
        this.mPlayThreads.stop();
        this.mStatus = Status.PAUSED;
        Log.d(TAG, "<pause> cost " + (SystemClock.uptimeMillis() - jUptimeMillis) + " ms");
        TraceHelper.endSection();
    }

    @Override
    public synchronized void updateData(MediaData[] mediaDataArr) {
        TraceHelper.beginSection(">>>>PhotoPlayEngine-updateData");
        if (this.mStatus != Status.RESUMED) {
            Log.d(TAG, "<updateData> not Status.RESUMED, no need to update data, mStatus = " + this.mStatus);
            TraceHelper.endSection();
            return;
        }
        logMediaDataArray("<updateData> Input data", mediaDataArr);
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.mPlayList.update(mediaDataArr);
        this.mPlayThreads.clearAllCmds();
        PlayList.Entry[] releaseList = this.mPlayList.getReleaseList();
        if (releaseList != null) {
            for (int i = 0; i < releaseList.length; i++) {
                if (releaseList[i] != null && releaseList[i].data != null && releaseList[i].player != null) {
                    this.mPlayThreads.submit(releaseList[i], Player.State.RELEASED);
                }
            }
        }
        for (int i2 = 0; i2 < this.mTotalCount; i2++) {
            int i3 = this.mSubmitTaskOrder[i2];
            if (this.mPlayList.get(i3) != null && this.mPlayList.get(i3).data != null && this.mPlayList.get(i3).player != null) {
                if (i3 >= this.mPlayRangeBegin && i3 <= this.mPlayRangeEnd) {
                    this.mPlayThreads.submit(this.mPlayList.get(i3), Player.State.PLAYING);
                } else {
                    this.mPlayThreads.submit(this.mPlayList.get(i3), Player.State.PREPARED);
                }
            }
        }
        Log.d(TAG, "<updateData> cost " + (SystemClock.uptimeMillis() - jUptimeMillis) + " ms");
        this.mPlayThreads.logCmdsWaitToRun("<Cmds queue after updateData>");
        TraceHelper.endSection();
    }

    @Override
    public synchronized boolean draw(MediaData mediaData, int i, MGLCanvas mGLCanvas, int i2, int i3) {
        TraceHelper.beginSection(">>>>PhotoPlayEngine-draw");
        if (i >= 0 && i < this.mTotalCount && this.mPlayList.get(i) != null && this.mPlayList.get(i).data != null && this.mPlayList.get(i).player != null && this.mPlayList.get(i).data.equals(mediaData)) {
            MTexture texture = this.mPlayList.get(i).player.getTexture(mGLCanvas);
            if (texture != null) {
                texture.draw(mGLCanvas, 0, 0, i2, i3);
                if (DebugUtils.DEBUG_PLAY_RENDER) {
                    mGLCanvas.fillRect(0.0f, 0.0f, i2, i3, RENDER_DEBUG_COLOR);
                }
                TraceHelper.endSection();
                return true;
            }
            TraceHelper.endSection();
            return false;
        }
        TraceHelper.endSection();
        return false;
    }

    @Override
    public synchronized void onFrameAvailable(Player player) {
        if (this.mFrameAvailableListener == null) {
            return;
        }
        int i = 0;
        while (true) {
            if (i >= this.mTotalCount) {
                break;
            } else if (this.mPlayList.get(i).player == player) {
                break;
            } else {
                i++;
            }
        }
    }

    @Override
    public int getPlayWidth(int i, MediaData mediaData) {
        if (i < 0 || i >= this.mTotalCount || this.mPlayList.get(i) == null || this.mPlayList.get(i).data == null || this.mPlayList.get(i).player == null || !this.mPlayList.get(i).data.equals(mediaData)) {
            return 0;
        }
        return this.mPlayList.get(i).player.getOutputWidth();
    }

    @Override
    public int getPlayHeight(int i, MediaData mediaData) {
        if (i < 0 || i >= this.mTotalCount || this.mPlayList.get(i) == null || this.mPlayList.get(i).data == null || this.mPlayList.get(i).player == null || !this.mPlayList.get(i).data.equals(mediaData)) {
            return 0;
        }
        return this.mPlayList.get(i).player.getOutputHeight();
    }

    @Override
    public boolean isSkipAnimationWhenUpdateSize(int i) {
        if (i < 0 || i >= this.mTotalCount || this.mPlayList.get(i) == null || this.mPlayList.get(i).data == null || this.mPlayList.get(i).player == null) {
            return false;
        }
        return this.mPlayList.get(i).player.isSkipAnimationWhenUpdateSize();
    }

    @Override
    public void fillEntry(int i, PlayList.Entry entry) {
        entry.player = this.mMediaCenter.getPlayer(entry.data, this.mThumbType);
        if (entry.player != null) {
            entry.player.setOnFrameAvailableListener(this);
        }
        if (i == this.mMiddleIndex && this.mLayerManager != null) {
            this.mLayerManager.switchLayer(entry.player, entry.data);
        }
    }

    @Override
    public void updateEntry(int i, PlayList.Entry entry) {
        if (i == this.mMiddleIndex && this.mLayerManager != null) {
            this.mLayerManager.switchLayer(entry.player, entry.data);
        }
    }

    private void initTaskOrder() {
        this.mSubmitTaskOrder = new int[this.mTotalCount];
        int i = 0;
        for (int i2 = this.mPlayRangeBegin; i2 <= this.mPlayRangeEnd; i2++) {
            this.mSubmitTaskOrder[i] = i2;
            i++;
        }
        int i3 = this.mPlayRangeBegin - 1;
        int i4 = this.mPlayRangeEnd + 1;
        while (true) {
            if (i3 >= 0 || i4 < this.mTotalCount) {
                if (i3 >= 0) {
                    this.mSubmitTaskOrder[i] = i3;
                    i++;
                }
                if (i4 < this.mTotalCount) {
                    this.mSubmitTaskOrder[i] = i4;
                    i++;
                }
                i3--;
                i4++;
            } else {
                return;
            }
        }
    }

    private void logMediaDataArray(String str, MediaData[] mediaDataArr) {
        if (!DebugUtils.DEBUG_PLAY_ENGINE) {
            return;
        }
        Log.d(TAG, str + " begin -----------------------------------------");
        for (int i = 0; i < mediaDataArr.length; i++) {
            if (mediaDataArr[i] != null) {
                Log.d(TAG, str + " [" + i + "] " + mediaDataArr[i].filePath);
            } else {
                Log.d(TAG, str + " [" + i + "] " + mediaDataArr[i]);
            }
        }
        Log.d(TAG, str + " end   -----------------------------------------");
    }

    private void logAfterNew() {
        StringBuilder sb = new StringBuilder();
        sb.append("<new> mThumbType = " + this.mThumbType + ", mTotalCount = " + this.mTotalCount + ", mPlayCount = " + this.mPlayCount + ", mPlayRangeBegin = " + this.mPlayRangeBegin + ", mPlayRangeEnd = " + this.mPlayRangeEnd + ", mMiddleIndex = " + this.mMiddleIndex + ", mWorkThreadNum = " + this.mWorkThreadNum + ", mSubmitTaskOrder = [");
        for (int i = 0; i < this.mSubmitTaskOrder.length; i++) {
            if (i == this.mSubmitTaskOrder.length - 1) {
                sb.append(this.mSubmitTaskOrder[i] + "]");
            } else {
                sb.append(this.mSubmitTaskOrder[i] + ", ");
            }
        }
        Log.d(TAG, sb.toString());
    }
}
