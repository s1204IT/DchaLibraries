package com.android.keyguard;

import android.content.Context;
import android.view.animation.Interpolator;

public class DisappearAnimationUtils extends AppearAnimationUtils {
    public DisappearAnimationUtils(Context ctx, long duration, float translationScaleFactor, float delayScaleFactor, Interpolator interpolator) {
        super(ctx, duration, translationScaleFactor, delayScaleFactor, interpolator);
        this.mScaleTranslationWithRow = true;
        this.mAppearing = false;
    }

    @Override
    protected long calculateDelay(int row, int col) {
        return (long) ((((double) (row * 60)) + (((double) col) * (Math.pow(row, 0.4d) + 0.4d) * 10.0d)) * ((double) this.mDelayScale));
    }
}
