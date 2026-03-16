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
            long frameTimeNanos = RampAnimator.this.mChoreographer.getFrameTimeNanos();
            float timeDelta = (frameTimeNanos - RampAnimator.this.mLastFrameTimeNanos) * 1.0E-9f;
            RampAnimator.this.mLastFrameTimeNanos = frameTimeNanos;
            float scale = ValueAnimator.getDurationScale();
            if (scale != 0.0f) {
                float amount = (RampAnimator.this.mRate * timeDelta) / scale;
                if (RampAnimator.this.mTargetValue > RampAnimator.this.mCurrentValue) {
                    RampAnimator.this.mAnimatedValue = Math.min(RampAnimator.this.mAnimatedValue + amount, RampAnimator.this.mTargetValue);
                } else {
                    RampAnimator.this.mAnimatedValue = Math.max(RampAnimator.this.mAnimatedValue - amount, RampAnimator.this.mTargetValue);
                }
            } else {
                RampAnimator.this.mAnimatedValue = RampAnimator.this.mTargetValue;
            }
            int oldCurrentValue = RampAnimator.this.mCurrentValue;
            RampAnimator.this.mCurrentValue = Math.round(RampAnimator.this.mAnimatedValue);
            if (oldCurrentValue != RampAnimator.this.mCurrentValue) {
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

    public RampAnimator(T object, IntProperty<T> property) {
        this.mObject = object;
        this.mProperty = property;
    }

    public boolean animateTo(int target, int rate) {
        if (this.mFirstTime || rate <= 0) {
            if (!this.mFirstTime && target == this.mCurrentValue) {
                return false;
            }
            this.mFirstTime = false;
            this.mRate = 0;
            this.mTargetValue = target;
            this.mCurrentValue = target;
            this.mProperty.setValue(this.mObject, target);
            if (this.mAnimating) {
                this.mAnimating = false;
                cancelAnimationCallback();
            }
            if (this.mListener != null) {
                this.mListener.onAnimationEnd();
            }
            return true;
        }
        if (!this.mAnimating || rate > this.mRate || ((target <= this.mCurrentValue && this.mCurrentValue <= this.mTargetValue) || (this.mTargetValue <= this.mCurrentValue && this.mCurrentValue <= target))) {
            this.mRate = rate;
        }
        boolean changed = this.mTargetValue != target;
        this.mTargetValue = target;
        if (!this.mAnimating && target != this.mCurrentValue) {
            this.mAnimating = true;
            this.mAnimatedValue = this.mCurrentValue;
            this.mLastFrameTimeNanos = System.nanoTime();
            postAnimationCallback();
            return changed;
        }
        return changed;
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
