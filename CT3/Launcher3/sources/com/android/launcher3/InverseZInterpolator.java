package com.android.launcher3;

import android.animation.TimeInterpolator;

class InverseZInterpolator implements TimeInterpolator {
    private ZInterpolator zInterpolator;

    public InverseZInterpolator(float foc) {
        this.zInterpolator = new ZInterpolator(foc);
    }

    @Override
    public float getInterpolation(float input) {
        return 1.0f - this.zInterpolator.getInterpolation(1.0f - input);
    }
}
