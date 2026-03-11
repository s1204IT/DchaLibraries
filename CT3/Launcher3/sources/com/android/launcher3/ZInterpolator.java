package com.android.launcher3;

import android.animation.TimeInterpolator;

class ZInterpolator implements TimeInterpolator {
    private float focalLength;

    public ZInterpolator(float foc) {
        this.focalLength = foc;
    }

    @Override
    public float getInterpolation(float input) {
        return (1.0f - (this.focalLength / (this.focalLength + input))) / (1.0f - (this.focalLength / (this.focalLength + 1.0f)));
    }
}
