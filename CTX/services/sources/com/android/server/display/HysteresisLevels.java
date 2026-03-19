package com.android.server.display;

import java.io.PrintWriter;
import java.util.Arrays;

final class HysteresisLevels {
    private static final boolean DEBUG = false;
    private static final float DEFAULT_BRIGHTENING_HYSTERESIS = 0.1f;
    private static final float DEFAULT_DARKENING_HYSTERESIS = 0.2f;
    private static final String TAG = "HysteresisLevels";
    private final float[] mBrightLevels;
    private final float[] mDarkLevels;
    private final float[] mLuxLevels;

    public HysteresisLevels(int[] iArr, int[] iArr2, int[] iArr3) {
        if (iArr.length != iArr2.length || iArr2.length != iArr3.length + 1) {
            throw new IllegalArgumentException("Mismatch between hysteresis array lengths.");
        }
        this.mBrightLevels = setArrayFormat(iArr, 1000.0f);
        this.mDarkLevels = setArrayFormat(iArr2, 1000.0f);
        this.mLuxLevels = setArrayFormat(iArr3, 1.0f);
    }

    public float getBrighteningThreshold(float f) {
        return f * (1.0f + getReferenceLevel(f, this.mBrightLevels));
    }

    public float getDarkeningThreshold(float f) {
        return f * (1.0f - getReferenceLevel(f, this.mDarkLevels));
    }

    private float getReferenceLevel(float f, float[] fArr) {
        int i = 0;
        while (this.mLuxLevels.length > i && f >= this.mLuxLevels[i]) {
            i++;
        }
        return fArr[i];
    }

    private float[] setArrayFormat(int[] iArr, float f) {
        float[] fArr = new float[iArr.length];
        for (int i = 0; fArr.length > i; i++) {
            fArr[i] = iArr[i] / f;
        }
        return fArr;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println(TAG);
        printWriter.println("  mBrightLevels=" + Arrays.toString(this.mBrightLevels));
        printWriter.println("  mDarkLevels=" + Arrays.toString(this.mDarkLevels));
        printWriter.println("  mLuxLevels=" + Arrays.toString(this.mLuxLevels));
    }
}
