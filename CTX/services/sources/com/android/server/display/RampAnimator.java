package com.android.server.display;

import android.animation.ValueAnimator;
import android.util.IntProperty;
import android.view.Choreographer;

final class RampAnimator<T> {
    private float mAnimatedValue;
    private boolean mAnimating;
    private int mCurrentValue;
    private long mLastFrameTimeNanos;
    private Listener mListener;
    private final T mObject;
    private final IntProperty<T> mProperty;
    private int mRate;
    private int mTargetValue;
    private boolean mFirstTime = true;
    private final Runnable mAnimationCallback = new Runnable() {
        @Override
        public void run() {
            float f = (r0 - RampAnimator.this.mLastFrameTimeNanos) * 1.0E-9f;
            RampAnimator.this.mLastFrameTimeNanos = RampAnimator.this.mChoreographer.getFrameTimeNanos();
            float durationScale = ValueAnimator.getDurationScale();
            if (durationScale != 0.0f) {
                float f2 = (f * RampAnimator.this.mRate) / durationScale;
                if (RampAnimator.this.mTargetValue > RampAnimator.this.mCurrentValue) {
                    RampAnimator.this.mAnimatedValue = Math.min(RampAnimator.this.mAnimatedValue + f2, RampAnimator.this.mTargetValue);
                } else {
                    RampAnimator.this.mAnimatedValue = Math.max(RampAnimator.this.mAnimatedValue - f2, RampAnimator.this.mTargetValue);
                }
            } else {
                RampAnimator.this.mAnimatedValue = RampAnimator.this.mTargetValue;
            }
            int i = RampAnimator.this.mCurrentValue;
            RampAnimator.this.mCurrentValue = Math.round(RampAnimator.this.mAnimatedValue);
            if (i != RampAnimator.this.mCurrentValue) {
                RampAnimator.this.mProperty.setValue(RampAnimator.this.mObject, RampAnimator.this.mCurrentValue);
            }
            if (RampAnimator.this.mTargetValue != RampAnimator.this.mCurrentValue) {
                RampAnimator.this.postAnimationCallback();
                return;
            }
            RampAnimator.this.mAnimating = false;
            if (RampAnimator.this.mListener != null) {
                RampAnimator.this.mListener.onAnimationEnd();
            }
        }
    };
    private final Choreographer mChoreographer = Choreographer.getInstance();

    public interface Listener {
        void onAnimationEnd();
    }

    public RampAnimator(T t, IntProperty<T> intProperty) {
        this.mObject = t;
        this.mProperty = intProperty;
    }

    public boolean animateTo(int i, int i2) {
        if (this.mFirstTime || i2 <= 0) {
            if (!this.mFirstTime && i == this.mCurrentValue) {
                return false;
            }
            this.mFirstTime = false;
            this.mRate = 0;
            this.mTargetValue = i;
            this.mCurrentValue = i;
            this.mProperty.setValue(this.mObject, i);
            if (this.mAnimating) {
                this.mAnimating = false;
                cancelAnimationCallback();
            }
            if (this.mListener != null) {
                this.mListener.onAnimationEnd();
            }
            return true;
        }
        if (!this.mAnimating || i2 > this.mRate || ((i <= this.mCurrentValue && this.mCurrentValue <= this.mTargetValue) || (this.mTargetValue <= this.mCurrentValue && this.mCurrentValue <= i))) {
            this.mRate = i2;
        }
        boolean z = this.mTargetValue != i;
        this.mTargetValue = i;
        if (!this.mAnimating && i != this.mCurrentValue) {
            this.mAnimating = true;
            this.mAnimatedValue = this.mCurrentValue;
            this.mLastFrameTimeNanos = System.nanoTime();
            postAnimationCallback();
        }
        return z;
    }

    public boolean isAnimating() {
        return this.mAnimating;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    private void postAnimationCallback() {
        this.mChoreographer.postCallback(1, this.mAnimationCallback, null);
    }

    private void cancelAnimationCallback() {
        this.mChoreographer.removeCallbacks(1, this.mAnimationCallback, null);
    }
}
