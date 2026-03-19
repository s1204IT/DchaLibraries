package com.mediatek.camera.feature.setting;

import android.content.Context;
import android.media.SoundPool;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

class CaptureSound {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CaptureSound.class.getSimpleName());
    private static int sUserCount = 0;
    private SoundPool mBurstSound;
    private final Context mContext;
    private int mSoundId;
    private int mStreamId;

    CaptureSound(Context context) {
        this.mContext = context;
    }

    void load() {
        LogHelper.d(TAG, "[load]sUserCount = " + sUserCount);
        sUserCount = sUserCount + 1;
        this.mBurstSound = new SoundPool(10, 7, 0);
        this.mSoundId = this.mBurstSound.load(this.mContext, R.raw.camera_shutter, 1);
    }

    void play() {
        LogHelper.d(TAG, "[play]mBurstSound = " + this.mBurstSound);
        if (this.mBurstSound == null) {
            load();
        }
        this.mStreamId = this.mBurstSound.play(this.mSoundId, 1.0f, 1.0f, 1, -1, 1.0f);
        if (this.mStreamId == 0) {
            load();
            sUserCount--;
            this.mStreamId = this.mBurstSound.play(this.mSoundId, 1.0f, 1.0f, 1, -1, 1.0f);
            LogHelper.d(TAG, "[play]done mStreamId = " + this.mStreamId);
        }
    }

    void stop() {
        LogHelper.d(TAG, "[stop]mStreamId = " + this.mStreamId);
        if (this.mBurstSound != null) {
            this.mBurstSound.stop(this.mStreamId);
        }
    }

    void release() {
        LogHelper.d(TAG, "[release]mBurstSound = " + this.mBurstSound + ", user count = " + sUserCount);
        if (this.mBurstSound != null) {
            sUserCount--;
            this.mBurstSound.unload(this.mSoundId);
            this.mBurstSound.release();
            this.mBurstSound = null;
        }
    }
}
