package com.android.musicvis;

import android.media.audiofx.Visualizer;
import android.util.Log;

public class AudioCapture {
    private static long MAX_IDLE_TIME_MS = 3000;
    private int[] mFormattedVizData;
    private long mLastValidCaptureTimeMs;
    private byte[] mRawVizData;
    private int mType;
    private Visualizer mVisualizer;
    private byte[] mRawNullData = new byte[0];
    private int[] mFormattedNullData = new int[0];

    public AudioCapture(int type, int size) {
        this.mType = type;
        int[] iArr = new int[2];
        int[] range = Visualizer.getCaptureSizeRange();
        size = size < range[0] ? range[0] : size;
        size = size > range[1] ? range[1] : size;
        this.mRawVizData = new byte[size];
        this.mFormattedVizData = new int[size];
        this.mVisualizer = null;
        try {
            this.mVisualizer = new Visualizer(0);
            if (this.mVisualizer != null) {
                if (this.mVisualizer.getEnabled()) {
                    this.mVisualizer.setEnabled(false);
                }
                this.mVisualizer.setCaptureSize(this.mRawVizData.length);
            }
        } catch (IllegalStateException e) {
            Log.e("AudioCapture", "Visualizer cstor IllegalStateException");
        } catch (UnsupportedOperationException e2) {
            Log.e("AudioCapture", "Visualizer cstor UnsupportedOperationException");
        } catch (RuntimeException e3) {
            Log.e("AudioCapture", "Visualizer cstor RuntimeException");
        }
    }

    public void start() {
        if (this.mVisualizer != null) {
            try {
                if (!this.mVisualizer.getEnabled()) {
                    this.mVisualizer.setEnabled(true);
                    this.mLastValidCaptureTimeMs = System.currentTimeMillis();
                }
            } catch (IllegalStateException e) {
                Log.e("AudioCapture", "start() IllegalStateException");
            }
        }
    }

    public void stop() {
        if (this.mVisualizer != null) {
            try {
                if (this.mVisualizer.getEnabled()) {
                    this.mVisualizer.setEnabled(false);
                }
            } catch (IllegalStateException e) {
                Log.e("AudioCapture", "stop() IllegalStateException");
            }
        }
    }

    public void release() {
        if (this.mVisualizer != null) {
            this.mVisualizer.release();
            this.mVisualizer = null;
        }
    }

    public int[] getFormattedData(int num, int den) {
        if (!captureData()) {
            return this.mFormattedNullData;
        }
        if (this.mType == 0) {
            for (int i = 0; i < this.mFormattedVizData.length; i++) {
                int tmp = (this.mRawVizData[i] & 255) - 128;
                this.mFormattedVizData[i] = (tmp * num) / den;
            }
        } else {
            for (int i2 = 0; i2 < this.mFormattedVizData.length; i2++) {
                this.mFormattedVizData[i2] = (this.mRawVizData[i2] * num) / den;
            }
        }
        return this.mFormattedVizData;
    }

    private boolean captureData() {
        try {
            try {
                int status = this.mVisualizer != null ? this.mType == 0 ? this.mVisualizer.getWaveForm(this.mRawVizData) : this.mVisualizer.getFft(this.mRawVizData) : -1;
                if (status != 0) {
                    Log.e("AudioCapture", "captureData() :  " + this + " error: " + status);
                    return false;
                }
                byte nullValue = this.mType == 0 ? (byte) -128 : (byte) 0;
                int i = 0;
                while (i < this.mRawVizData.length && this.mRawVizData[i] == nullValue) {
                    i++;
                }
                if (i == this.mRawVizData.length) {
                    return System.currentTimeMillis() - this.mLastValidCaptureTimeMs <= MAX_IDLE_TIME_MS;
                }
                this.mLastValidCaptureTimeMs = System.currentTimeMillis();
                return true;
            } catch (IllegalStateException e) {
                Log.e("AudioCapture", "captureData() IllegalStateException: " + this);
                if (-1 != 0) {
                    Log.e("AudioCapture", "captureData() :  " + this + " error: -1");
                    return false;
                }
                byte nullValue2 = this.mType == 0 ? (byte) -128 : (byte) 0;
                int i2 = 0;
                while (i2 < this.mRawVizData.length && this.mRawVizData[i2] == nullValue2) {
                    i2++;
                }
                if (i2 == this.mRawVizData.length) {
                    return System.currentTimeMillis() - this.mLastValidCaptureTimeMs <= MAX_IDLE_TIME_MS;
                }
                this.mLastValidCaptureTimeMs = System.currentTimeMillis();
                return true;
            }
        } catch (Throwable th) {
            if (-1 != 0) {
                Log.e("AudioCapture", "captureData() :  " + this + " error: -1");
            } else {
                byte nullValue3 = this.mType == 0 ? (byte) -128 : (byte) 0;
                int i3 = 0;
                while (i3 < this.mRawVizData.length && this.mRawVizData[i3] == nullValue3) {
                    i3++;
                }
                if (i3 != this.mRawVizData.length) {
                    this.mLastValidCaptureTimeMs = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - this.mLastValidCaptureTimeMs > MAX_IDLE_TIME_MS) {
                }
            }
            throw th;
        }
    }
}
