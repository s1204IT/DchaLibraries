package com.android.quickstep.util;

import android.animation.ValueAnimator;
import android.view.animation.Interpolator;
import java.util.ArrayList;

public abstract class MultiValueUpdateListener implements ValueAnimator.AnimatorUpdateListener {
    private final ArrayList<FloatProp> mAllProperties = new ArrayList<>();

    public abstract void onUpdate(float f);

    @Override
    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        float animatedFraction = valueAnimator.getAnimatedFraction();
        float duration = valueAnimator.getDuration() * animatedFraction;
        for (int size = this.mAllProperties.size() - 1; size >= 0; size--) {
            FloatProp floatProp = this.mAllProperties.get(size);
            float interpolation = floatProp.mInterpolator.getInterpolation(Math.min(1.0f, Math.max(0.0f, duration - floatProp.mDelay) / floatProp.mDuration));
            floatProp.value = (floatProp.mEnd * interpolation) + (floatProp.mStart * (1.0f - interpolation));
        }
        onUpdate(animatedFraction);
    }

    public final class FloatProp {
        private final float mDelay;
        private final float mDuration;
        private final float mEnd;
        private final Interpolator mInterpolator;
        private final float mStart;
        public float value;

        public FloatProp(float f, float f2, float f3, float f4, Interpolator interpolator) {
            this.mStart = f;
            this.value = f;
            this.mEnd = f2;
            this.mDelay = f3;
            this.mDuration = f4;
            this.mInterpolator = interpolator;
            MultiValueUpdateListener.this.mAllProperties.add(this);
        }
    }
}
