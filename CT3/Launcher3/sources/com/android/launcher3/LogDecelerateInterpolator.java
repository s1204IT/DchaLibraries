package com.android.launcher3;

import android.animation.TimeInterpolator;

public class LogDecelerateInterpolator implements TimeInterpolator {
    int mBase;
    int mDrift;
    final float mLogScale;

    public LogDecelerateInterpolator(int base, int drift) {
        this.mBase = base;
        this.mDrift = drift;
        this.mLogScale = 1.0f / computeLog(1.0f, this.mBase, this.mDrift);
    }

    static float computeLog(float t, int base, int drift) {
        return ((float) (-Math.pow(base, -t))) + 1.0f + (drift * t);
    }

    @Override
    public float getInterpolation(float t) {
        if (Float.compare(t, 1.0f) == 0) {
            return 1.0f;
        }
        return computeLog(t, this.mBase, this.mDrift) * this.mLogScale;
    }
}
