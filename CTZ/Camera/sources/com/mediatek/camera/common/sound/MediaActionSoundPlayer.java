package com.mediatek.camera.common.sound;

import android.annotation.TargetApi;
import android.media.MediaActionSound;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;

@TargetApi(16)
class MediaActionSoundPlayer {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MediaActionSoundPlayer.class.getSimpleName());
    private MediaActionSound mSound = new MediaActionSound();

    protected MediaActionSoundPlayer() {
        this.mSound.load(2);
        this.mSound.load(3);
        this.mSound.load(1);
        this.mSound.load(0);
    }

    protected synchronized void play(int i) {
        if (this.mSound == null) {
            LogHelper.e(TAG, "[play] mSound is null");
            return;
        }
        switch (i) {
            case 0:
                this.mSound.play(1);
                break;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                this.mSound.play(2);
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mSound.play(3);
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                this.mSound.play(0);
                break;
            default:
                LogHelper.w(TAG, "Unrecognized action:" + i);
                break;
        }
    }

    protected void release() {
        if (this.mSound != null) {
            LogHelper.i(TAG, "[release] ");
            this.mSound.release();
            this.mSound = null;
        }
    }
}
