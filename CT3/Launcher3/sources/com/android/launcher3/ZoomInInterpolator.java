package com.android.launcher3;

import android.animation.TimeInterpolator;
import android.view.animation.DecelerateInterpolator;

class ZoomInInterpolator implements TimeInterpolator {
    private final InverseZInterpolator inverseZInterpolator = new InverseZInterpolator(0.35f);
    private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);

    ZoomInInterpolator() {
    }

    @Override
    public float getInterpolation(float input) {
        return this.decelerate.getInterpolation(this.inverseZInterpolator.getInterpolation(input));
    }
}
