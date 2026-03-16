package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.R;

public class TrustDrawable extends Drawable {
    private final Interpolator mAccelerateDecelerateInterpolator;
    private int mAlpha;
    private boolean mAnimating;
    private int mCurAlpha;
    private Animator mCurAnimator;
    private float mCurInnerRadius;
    private final Interpolator mFastOutSlowInInterpolator;
    private final float mInnerRadiusEnter;
    private final float mInnerRadiusExit;
    private final float mInnerRadiusVisibleMax;
    private final float mInnerRadiusVisibleMin;
    private final Interpolator mLinearOutSlowInInterpolator;
    private Paint mPaint;
    private final float mThickness;
    private boolean mTrustManaged;
    private final Animator mVisibleAnimator;
    private int mState = -1;
    private final ValueAnimator.AnimatorUpdateListener mAlphaUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            TrustDrawable.this.mCurAlpha = ((Integer) animation.getAnimatedValue()).intValue();
            TrustDrawable.this.invalidateSelf();
        }
    };
    private final ValueAnimator.AnimatorUpdateListener mRadiusUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            TrustDrawable.this.mCurInnerRadius = ((Float) animation.getAnimatedValue()).floatValue();
            TrustDrawable.this.invalidateSelf();
        }
    };

    public TrustDrawable(Context context) {
        Resources r = context.getResources();
        this.mInnerRadiusVisibleMin = r.getDimension(R.dimen.trust_circle_inner_radius_visible_min);
        this.mInnerRadiusVisibleMax = r.getDimension(R.dimen.trust_circle_inner_radius_visible_max);
        this.mInnerRadiusExit = r.getDimension(R.dimen.trust_circle_inner_radius_exit);
        this.mInnerRadiusEnter = r.getDimension(R.dimen.trust_circle_inner_radius_enter);
        this.mThickness = r.getDimension(R.dimen.trust_circle_thickness);
        this.mCurInnerRadius = this.mInnerRadiusEnter;
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in);
        this.mAccelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
        this.mVisibleAnimator = makeVisibleAnimator();
        this.mPaint = new Paint();
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setColor(-1);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStrokeWidth(this.mThickness);
    }

    @Override
    public void draw(Canvas canvas) {
        int newAlpha = (this.mCurAlpha * this.mAlpha) / 256;
        if (newAlpha != 0) {
            Rect r = getBounds();
            this.mPaint.setAlpha(newAlpha);
            canvas.drawCircle(r.exactCenterX(), r.exactCenterY(), this.mCurInnerRadius, this.mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        this.mAlpha = alpha;
    }

    @Override
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    public void start() {
        if (!this.mAnimating) {
            this.mAnimating = true;
            updateState(true);
            invalidateSelf();
        }
    }

    public void stop() {
        if (this.mAnimating) {
            this.mAnimating = false;
            if (this.mCurAnimator != null) {
                this.mCurAnimator.cancel();
                this.mCurAnimator = null;
            }
            this.mState = -1;
            this.mCurAlpha = 0;
            this.mCurInnerRadius = this.mInnerRadiusEnter;
            invalidateSelf();
        }
    }

    public void setTrustManaged(boolean trustManaged) {
        if (trustManaged != this.mTrustManaged || this.mState == -1) {
            this.mTrustManaged = trustManaged;
            updateState(true);
        }
    }

    private void updateState(boolean allowTransientState) {
        if (this.mAnimating) {
            int nextState = this.mState;
            if (this.mState == -1) {
                nextState = this.mTrustManaged ? 1 : 0;
            } else if (this.mState == 0) {
                if (this.mTrustManaged) {
                    nextState = 1;
                }
            } else if (this.mState == 1) {
                if (!this.mTrustManaged) {
                    nextState = 3;
                }
            } else if (this.mState == 2) {
                if (!this.mTrustManaged) {
                    nextState = 3;
                }
            } else if (this.mState == 3 && this.mTrustManaged) {
                nextState = 1;
            }
            if (!allowTransientState) {
                if (nextState == 1) {
                    nextState = 2;
                }
                if (nextState == 3) {
                    nextState = 0;
                }
            }
            if (nextState != this.mState) {
                if (this.mCurAnimator != null) {
                    this.mCurAnimator.cancel();
                    this.mCurAnimator = null;
                }
                if (nextState == 0) {
                    this.mCurAlpha = 0;
                    this.mCurInnerRadius = this.mInnerRadiusEnter;
                } else if (nextState == 1) {
                    this.mCurAnimator = makeEnterAnimator(this.mCurInnerRadius, this.mCurAlpha);
                    if (this.mState == -1) {
                        this.mCurAnimator.setStartDelay(200L);
                    }
                } else if (nextState == 2) {
                    this.mCurAlpha = 76;
                    this.mCurInnerRadius = this.mInnerRadiusVisibleMax;
                    this.mCurAnimator = this.mVisibleAnimator;
                } else if (nextState == 3) {
                    this.mCurAnimator = makeExitAnimator(this.mCurInnerRadius, this.mCurAlpha);
                }
                this.mState = nextState;
                if (this.mCurAnimator != null) {
                    this.mCurAnimator.start();
                }
                invalidateSelf();
            }
        }
    }

    private Animator makeVisibleAnimator() {
        return makeAnimators(this.mInnerRadiusVisibleMax, this.mInnerRadiusVisibleMin, 76, 38, 1000L, this.mAccelerateDecelerateInterpolator, true, false);
    }

    private Animator makeEnterAnimator(float radius, int alpha) {
        return makeAnimators(radius, this.mInnerRadiusVisibleMax, alpha, 76, 500L, this.mLinearOutSlowInInterpolator, false, true);
    }

    private Animator makeExitAnimator(float radius, int alpha) {
        return makeAnimators(radius, this.mInnerRadiusExit, alpha, 0, 500L, this.mFastOutSlowInInterpolator, false, true);
    }

    private Animator makeAnimators(float startRadius, float endRadius, int startAlpha, int endAlpha, long duration, Interpolator interpolator, boolean repeating, boolean stateUpdateListener) {
        ValueAnimator alphaAnimator = configureAnimator(ValueAnimator.ofInt(startAlpha, endAlpha), duration, this.mAlphaUpdateListener, interpolator, repeating);
        ValueAnimator sizeAnimator = configureAnimator(ValueAnimator.ofFloat(startRadius, endRadius), duration, this.mRadiusUpdateListener, interpolator, repeating);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(alphaAnimator, sizeAnimator);
        if (stateUpdateListener) {
            set.addListener(new StateUpdateAnimatorListener());
        }
        return set;
    }

    private ValueAnimator configureAnimator(ValueAnimator animator, long duration, ValueAnimator.AnimatorUpdateListener updateListener, Interpolator interpolator, boolean repeating) {
        animator.setDuration(duration);
        animator.addUpdateListener(updateListener);
        animator.setInterpolator(interpolator);
        if (repeating) {
            animator.setRepeatCount(-1);
            animator.setRepeatMode(2);
        }
        return animator;
    }

    private class StateUpdateAnimatorListener extends AnimatorListenerAdapter {
        boolean mCancelled;

        private StateUpdateAnimatorListener() {
        }

        @Override
        public void onAnimationStart(Animator animation) {
            this.mCancelled = false;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            this.mCancelled = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!this.mCancelled) {
                TrustDrawable.this.updateState(false);
            }
        }
    }
}
