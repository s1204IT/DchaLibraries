package com.mediatek.camera.common.sound;

import android.content.Context;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class SoundPlaybackImpl implements ISoundPlayback {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SoundPlaybackImpl.class.getSimpleName());
    private Context mContext;
    private MediaActionSoundPlayer mMediaActionSoundPlayer = new MediaActionSoundPlayer();
    private SoundPlayer mSoundPlayer;

    public SoundPlaybackImpl(Context context) {
        this.mContext = context;
        this.mSoundPlayer = new SoundPlayer(this.mContext);
    }

    @Override
    public void play(int i) {
        LogHelper.d(TAG, "[play] play sound with action " + i);
        this.mMediaActionSoundPlayer.play(i);
    }

    public void pause() {
        this.mSoundPlayer.unloadSound();
    }

    public void release() {
        LogHelper.d(TAG, "[release]");
        this.mMediaActionSoundPlayer.release();
        this.mSoundPlayer.unloadSound();
        this.mSoundPlayer.release();
    }
}
