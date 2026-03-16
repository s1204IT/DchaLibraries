package com.android.musicvis.vis3;

import com.android.musicvis.AudioCapture;
import com.android.musicvis.GenericWaveRS;
import com.android.musicvis.R;

class Visualization3RS extends GenericWaveRS {
    float lastOffset;
    private short[] mAnalyzer;

    Visualization3RS(int width, int height) {
        super(width, height, R.drawable.ice);
        this.mAnalyzer = new short[512];
        this.lastOffset = 0.0f;
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        this.mWorldState.yRotation = 4.0f * xOffset * 360.0f;
        updateWorldState();
    }

    @Override
    public void start() {
        if (this.mAudioCapture == null) {
            this.mAudioCapture = new AudioCapture(1, 512);
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
            len = this.mVizData.length / 2;
        }
        if (len == 0) {
            if (this.mWorldState.idle == 0) {
                this.mWorldState.idle = 1;
                updateWorldState();
                return;
            }
            return;
        }
        int len2 = len / 2;
        if (len2 > this.mAnalyzer.length) {
            len2 = this.mAnalyzer.length;
        }
        if (this.mWorldState.idle != 0) {
            this.mWorldState.idle = 0;
            updateWorldState();
        }
        for (int i = 1; i < len2 - 1; i++) {
            int val1 = this.mVizData[i * 2];
            int val2 = this.mVizData[(i * 2) + 1];
            short newval = (short) (((i / 16) + 1) * ((val1 * val1) + (val2 * val2)));
            short oldval = this.mAnalyzer[i];
            if (newval < oldval - 800) {
                newval = (short) (oldval - 800);
            }
            this.mAnalyzer[i] = newval;
        }
        int outlen = this.mPointData.length / 8;
        int width = this.mWidth > outlen ? outlen : this.mWidth;
        int skip = (outlen - width) / 2;
        int srcidx = 0;
        int cnt = 0;
        for (int i2 = 0; i2 < width; i2++) {
            float val = this.mAnalyzer[srcidx] / 8;
            if (val < 1.0f && val > -1.0f) {
                val = 1.0f;
            }
            this.mPointData[((i2 + skip) * 8) + 1] = val;
            this.mPointData[((i2 + skip) * 8) + 5] = -val;
            cnt += len2;
            if (cnt > width) {
                srcidx++;
                cnt -= width;
            }
        }
        this.mPointAlloc.copyFromUnchecked(this.mPointData);
    }
}
