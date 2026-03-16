package com.android.gallery3d.ui;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class CaptureAnimation {
    private static final Interpolator sZoomOutInterpolator = new DecelerateInterpolator();
    private static final Interpolator sZoomInInterpolator = new AccelerateInterpolator();
    private static final Interpolator sSlideInterpolator = new AccelerateDecelerateInterpolator();

    public static float calculateSlide(float fraction) {
        return sSlideInterpolator.getInterpolation(fraction);
    }

    public static float calculateScale(float fraction) {
        if (fraction <= 0.5f) {
            float value = 1.0f - (sZoomOutInterpolator.getInterpolation(fraction * 2.0f) * 0.2f);
            return value;
        }
        float value2 = 0.8f + (sZoomInInterpolator.getInterpolation((fraction - 0.5f) * 2.0f) * 0.2f);
        return value2;
    }
}
