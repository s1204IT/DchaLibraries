package com.mediatek.camera.feature.setting.selftimer;

import android.app.Activity;
import android.media.SoundPool;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class SelfTimerSoundManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SelfTimerSoundManager.class.getSimpleName());
    private Activity mActvity;
    private int mBeepOnce;
    private int mBeepOnceStreamId;
    private int mBeepTwice;
    private int mBeepTwiceStreamId;
    private SoundPool mSoundPool;

    SelfTimerSoundManager(IApp iApp) {
        this.mActvity = iApp.getActivity();
    }

    public void load() {
        if (this.mSoundPool == null) {
            this.mSoundPool = new SoundPool(1, 7, 0);
            this.mBeepOnce = this.mSoundPool.load(this.mActvity, R.raw.beep_once, 1);
            this.mBeepTwice = this.mSoundPool.load(this.mActvity, R.raw.beep_twice, 1);
        }
        LogHelper.d(TAG, "[load] mSoundPool :" + this.mSoundPool);
    }

    public void play(int i) {
        LogHelper.d(TAG, "[play]");
        if (this.mSoundPool == null) {
            return;
        }
        if (i == 0) {
            this.mBeepOnceStreamId = this.mSoundPool.play(this.mBeepOnce, 1.0f, 1.0f, 0, 0, 1.0f);
        } else {
            this.mBeepTwiceStreamId = this.mSoundPool.play(this.mBeepTwice, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void stop() {
        LogHelper.d(TAG, "[stop]");
        if (this.mSoundPool != null) {
            this.mSoundPool.stop(this.mBeepOnceStreamId);
            this.mSoundPool.stop(this.mBeepTwiceStreamId);
        }
    }

    public void release() {
        LogHelper.d(TAG, "[release]");
        if (this.mSoundPool != null) {
            this.mSoundPool.unload(this.mBeepOnce);
            this.mSoundPool.unload(this.mBeepTwice);
            this.mSoundPool.release();
            this.mSoundPool = null;
        }
    }
}
