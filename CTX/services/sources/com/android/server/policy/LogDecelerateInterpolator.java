package com.android.server.policy;

import android.view.animation.Interpolator;

public class LogDecelerateInterpolator implements Interpolator {
    private int mBase;
    private int mDrift;
    private final float mLogScale;

    public LogDecelerateInterpolator(int i, int i2) {
        this.mBase = i;
        this.mDrift = i2;
        this.mLogScale = 1.0f / computeLog(1.0f, this.mBase, this.mDrift);
    }

    private static float computeLog(float f, int i, int i2) {
        return ((float) (-Math.pow(i, -f))) + 1.0f + (i2 * f);
    }

    @Override
    public float getInterpolation(float f) {
        return computeLog(f, this.mBase, this.mDrift) * this.mLogScale;
    }
}
