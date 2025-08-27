package com.android.systemui.statusbar;

import android.animation.Animator;
import android.content.Context;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.notification.NotificationUtils;

/* loaded from: classes.dex */
public class FlingAnimationUtils {
    private AnimatorProperties mAnimatorProperties;
    private float mCachedStartGradient;
    private float mCachedVelocityFactor;
    private float mHighVelocityPxPerSecond;
    private PathInterpolator mInterpolator;
    private float mLinearOutSlowInX2;
    private float mMaxLengthSeconds;
    private float mMinVelocityPxPerSecond;
    private final float mSpeedUpFactor;
    private final float mY2;

    public FlingAnimationUtils(Context context, float f) {
        this(context, f, 0.0f);
    }

    public FlingAnimationUtils(Context context, float f, float f2) {
        this(context, f, f2, -1.0f, 1.0f);
    }

    public FlingAnimationUtils(Context context, float f, float f2, float f3, float f4) {
        this.mAnimatorProperties = new AnimatorProperties();
        this.mCachedStartGradient = -1.0f;
        this.mCachedVelocityFactor = -1.0f;
        this.mMaxLengthSeconds = f;
        this.mSpeedUpFactor = f2;
        if (f3 < 0.0f) {
            this.mLinearOutSlowInX2 = NotificationUtils.interpolate(0.35f, 0.68f, this.mSpeedUpFactor);
        } else {
            this.mLinearOutSlowInX2 = f3;
        }
        this.mY2 = f4;
        this.mMinVelocityPxPerSecond = 250.0f * context.getResources().getDisplayMetrics().density;
        this.mHighVelocityPxPerSecond = 3000.0f * context.getResources().getDisplayMetrics().density;
    }

    public void apply(Animator animator, float f, float f2, float f3) {
        apply(animator, f, f2, f3, Math.abs(f2 - f));
    }

    public void apply(ViewPropertyAnimator viewPropertyAnimator, float f, float f2, float f3) {
        apply(viewPropertyAnimator, f, f2, f3, Math.abs(f2 - f));
    }

    public void apply(Animator animator, float f, float f2, float f3, float f4) {
        AnimatorProperties properties = getProperties(f, f2, f3, f4);
        animator.setDuration(properties.duration);
        animator.setInterpolator(properties.interpolator);
    }

    public void apply(ViewPropertyAnimator viewPropertyAnimator, float f, float f2, float f3, float f4) {
        AnimatorProperties properties = getProperties(f, f2, f3, f4);
        viewPropertyAnimator.setDuration(properties.duration);
        viewPropertyAnimator.setInterpolator(properties.interpolator);
    }

    private AnimatorProperties getProperties(float f, float f2, float f3, float f4) {
        float f5 = f2 - f;
        float fSqrt = (float) (this.mMaxLengthSeconds * Math.sqrt(Math.abs(f5) / f4));
        float fAbs = Math.abs(f5);
        float fAbs2 = Math.abs(f3);
        float fMin = this.mSpeedUpFactor != 0.0f ? Math.min(fAbs2 / 3000.0f, 1.0f) : 1.0f;
        float fInterpolate = NotificationUtils.interpolate(0.75f, this.mY2 / this.mLinearOutSlowInX2, fMin);
        float f6 = (fInterpolate * fAbs) / fAbs2;
        Interpolator interpolator = getInterpolator(fInterpolate, fMin);
        if (f6 <= fSqrt) {
            this.mAnimatorProperties.interpolator = interpolator;
            fSqrt = f6;
        } else if (fAbs2 >= this.mMinVelocityPxPerSecond) {
            this.mAnimatorProperties.interpolator = new InterpolatorInterpolator(new VelocityInterpolator(fSqrt, fAbs2, fAbs), interpolator, Interpolators.LINEAR_OUT_SLOW_IN);
        } else {
            this.mAnimatorProperties.interpolator = Interpolators.FAST_OUT_SLOW_IN;
        }
        this.mAnimatorProperties.duration = (long) (fSqrt * 1000.0f);
        return this.mAnimatorProperties;
    }

    private Interpolator getInterpolator(float f, float f2) {
        if (f != this.mCachedStartGradient || f2 != this.mCachedVelocityFactor) {
            float f3 = this.mSpeedUpFactor * (1.0f - f2);
            this.mInterpolator = new PathInterpolator(f3, f3 * f, this.mLinearOutSlowInX2, this.mY2);
            this.mCachedStartGradient = f;
            this.mCachedVelocityFactor = f2;
        }
        return this.mInterpolator;
    }

    public void applyDismissing(Animator animator, float f, float f2, float f3, float f4) {
        AnimatorProperties dismissingProperties = getDismissingProperties(f, f2, f3, f4);
        animator.setDuration(dismissingProperties.duration);
        animator.setInterpolator(dismissingProperties.interpolator);
    }

    private AnimatorProperties getDismissingProperties(float f, float f2, float f3, float f4) {
        float f5 = f2 - f;
        float fPow = (float) (this.mMaxLengthSeconds * Math.pow(Math.abs(f5) / f4, 0.5d));
        float fAbs = Math.abs(f5);
        float fAbs2 = Math.abs(f3);
        float fCalculateLinearOutFasterInY2 = calculateLinearOutFasterInY2(fAbs2);
        PathInterpolator pathInterpolator = new PathInterpolator(0.0f, 0.0f, 0.5f, fCalculateLinearOutFasterInY2);
        float f6 = ((fCalculateLinearOutFasterInY2 / 0.5f) * fAbs) / fAbs2;
        if (f6 <= fPow) {
            this.mAnimatorProperties.interpolator = pathInterpolator;
            fPow = f6;
        } else if (fAbs2 >= this.mMinVelocityPxPerSecond) {
            this.mAnimatorProperties.interpolator = new InterpolatorInterpolator(new VelocityInterpolator(fPow, fAbs2, fAbs), pathInterpolator, Interpolators.LINEAR_OUT_SLOW_IN);
        } else {
            this.mAnimatorProperties.interpolator = Interpolators.FAST_OUT_LINEAR_IN;
        }
        this.mAnimatorProperties.duration = (long) (fPow * 1000.0f);
        return this.mAnimatorProperties;
    }

    private float calculateLinearOutFasterInY2(float f) {
        float fMax = Math.max(0.0f, Math.min(1.0f, (f - this.mMinVelocityPxPerSecond) / (this.mHighVelocityPxPerSecond - this.mMinVelocityPxPerSecond)));
        return ((1.0f - fMax) * 0.4f) + (fMax * 0.5f);
    }

    public float getMinVelocityPxPerSecond() {
        return this.mMinVelocityPxPerSecond;
    }

    private static final class InterpolatorInterpolator implements Interpolator {
        private Interpolator mCrossfader;
        private Interpolator mInterpolator1;
        private Interpolator mInterpolator2;

        InterpolatorInterpolator(Interpolator interpolator, Interpolator interpolator2, Interpolator interpolator3) {
            this.mInterpolator1 = interpolator;
            this.mInterpolator2 = interpolator2;
            this.mCrossfader = interpolator3;
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float f) {
            float interpolation = this.mCrossfader.getInterpolation(f);
            return ((1.0f - interpolation) * this.mInterpolator1.getInterpolation(f)) + (interpolation * this.mInterpolator2.getInterpolation(f));
        }
    }

    private static final class VelocityInterpolator implements Interpolator {
        private float mDiff;
        private float mDurationSeconds;
        private float mVelocity;

        private VelocityInterpolator(float f, float f2, float f3) {
            this.mDurationSeconds = f;
            this.mVelocity = f2;
            this.mDiff = f3;
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float f) {
            return ((f * this.mDurationSeconds) * this.mVelocity) / this.mDiff;
        }
    }

    private static class AnimatorProperties {
        long duration;
        Interpolator interpolator;

        private AnimatorProperties() {
        }
    }
}
