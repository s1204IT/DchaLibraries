package com.android.musicvis.vis2;

import com.android.musicvis.AudioCapture;
import com.android.musicvis.GenericWaveRS;
import com.android.musicvis.R;

class Visualization2RS extends GenericWaveRS {
    Visualization2RS(int width, int height) {
        super(width, height, R.drawable.fire);
    }

    @Override
    public void start() {
        if (this.mAudioCapture == null) {
            this.mAudioCapture = new AudioCapture(0, 1024);
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.mAudioCapture != null) {
            this.mAudioCapture.release();
            this.mAudioCapture = null;
        }
    }

    @Override
    public void update() {
        int len = 0;
        if (this.mAudioCapture != null) {
            this.mVizData = this.mAudioCapture.getFormattedData(1, 1);
            len = this.mVizData.length;
        }
        if (len == 0) {
            if (this.mWorldState.idle == 0) {
                this.mWorldState.idle = 1;
                updateWorldState();
                return;
            }
            return;
        }
        int outlen = this.mPointData.length / 8;
        if (len > outlen) {
            len = outlen;
        }
        if (this.mWorldState.idle != 0) {
            this.mWorldState.idle = 0;
            updateWorldState();
        }
        for (int i = 0; i < len; i++) {
            int amp = this.mVizData[i];
            this.mPointData[(i * 8) + 1] = amp;
            this.mPointData[(i * 8) + 5] = -amp;
        }
        this.mPointAlloc.copyFromUnchecked(this.mPointData);
    }
}
