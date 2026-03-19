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
import android.view.animation.Interpolator;
import com.android.settingslib.Utils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class TrustDrawable extends Drawable {
    private int mAlpha;
    private boolean mAnimating;
    private int mCurAlpha;
    private Animator mCurAnimator;
    private float mCurInnerRadius;
    private final float mInnerRadiusEnter;
    private final float mInnerRadiusExit;
    private final float mInnerRadiusVisibleMax;
    private final float mInnerRadiusVisibleMin;
    private Paint mPaint;
    private final float mThickness;
    private boolean mTrustManaged;
    private final Animator mVisibleAnimator;
    private int mState = -1;
    private final ValueAnimator.AnimatorUpdateListener mAlphaUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            TrustDrawable.this.mCurAlpha = ((Integer) valueAnimator.getAnimatedValue()).intValue();
            TrustDrawable.this.invalidateSelf();
        }
    };
    private final ValueAnimator.AnimatorUpdateListener mRadiusUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            TrustDrawable.this.mCurInnerRadius = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            TrustDrawable.this.invalidateSelf();
        }
    };

    public TrustDrawable(Context context) {
        Resources resources = context.getResources();
        this.mInnerRadiusVisibleMin = resources.getDimension(R.dimen.trust_circle_inner_radius_visible_min);
        this.mInnerRadiusVisibleMax = resources.getDimension(R.dimen.trust_circle_inner_radius_visible_max);
        this.mInnerRadiusExit = resources.getDimension(R.dimen.trust_circle_inner_radius_exit);
        this.mInnerRadiusEnter = resources.getDimension(R.dimen.trust_circle_inner_radius_enter);
        this.mThickness = resources.getDimension(R.dimen.trust_circle_thickness);
        this.mCurInnerRadius = this.mInnerRadiusEnter;
        this.mVisibleAnimator = makeVisibleAnimator();
        this.mPaint = new Paint();
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setColor(Utils.getColorAttr(context, R.attr.wallpaperTextColor));
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStrokeWidth(this.mThickness);
    }

    @Override
    public void draw(Canvas canvas) {
        int i = (this.mCurAlpha * this.mAlpha) / 256;
        if (i == 0) {
            return;
        }
        Rect bounds = getBounds();
        this.mPaint.setAlpha(i);
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), this.mCurInnerRadius, this.mPaint);
    }

    @Override
    public void setAlpha(int i) {
        this.mAlpha = i;
    }

    @Override
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
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

    public void setTrustManaged(boolean z) {
        if (z != this.mTrustManaged || this.mState == -1) {
            this.mTrustManaged = z;
            updateState(true);
        }
    }

    private void updateState(boolean z) {
        ?? r0;
        if (!this.mAnimating) {
            return;
        }
        int i = this.mState;
        if (this.mState == -1) {
            r0 = this.mTrustManaged;
        } else if (this.mState == 0) {
            r0 = i;
            if (this.mTrustManaged) {
                r0 = 1;
            }
        } else if (this.mState == 1) {
            r0 = i;
            if (!this.mTrustManaged) {
                r0 = 3;
            }
        } else if (this.mState == 2) {
            r0 = i;
            if (!this.mTrustManaged) {
            }
        } else {
            r0 = i;
            if (this.mState == 3) {
                r0 = i;
                if (this.mTrustManaged) {
                }
            }
        }
        r0 = r0;
        if (!z) {
            if (r0 == 1) {
                r0 = 2;
            }
            if (r0 == 3) {
                r0 = 0;
            }
        }
        if (r0 != this.mState) {
            if (this.mCurAnimator != null) {
                this.mCurAnimator.cancel();
                this.mCurAnimator = null;
            }
            if (r0 == 0) {
                this.mCurAlpha = 0;
                this.mCurInnerRadius = this.mInnerRadiusEnter;
            } else if (r0 == 1) {
                this.mCurAnimator = makeEnterAnimator(this.mCurInnerRadius, this.mCurAlpha);
                if (this.mState == -1) {
                    this.mCurAnimator.setStartDelay(200L);
                }
            } else if (r0 == 2) {
                this.mCurAlpha = 76;
                this.mCurInnerRadius = this.mInnerRadiusVisibleMax;
                this.mCurAnimator = this.mVisibleAnimator;
            } else if (r0 == 3) {
                this.mCurAnimator = makeExitAnimator(this.mCurInnerRadius, this.mCurAlpha);
            }
            this.mState = r0;
            if (this.mCurAnimator != null) {
                this.mCurAnimator.start();
            }
            invalidateSelf();
        }
    }

    private Animator makeVisibleAnimator() {
        return makeAnimators(this.mInnerRadiusVisibleMax, this.mInnerRadiusVisibleMin, 76, 38, 1000L, Interpolators.ACCELERATE_DECELERATE, true, false);
    }

    private Animator makeEnterAnimator(float f, int i) {
        return makeAnimators(f, this.mInnerRadiusVisibleMax, i, 76, 500L, Interpolators.LINEAR_OUT_SLOW_IN, false, true);
    }

    private Animator makeExitAnimator(float f, int i) {
        return makeAnimators(f, this.mInnerRadiusExit, i, 0, 500L, Interpolators.FAST_OUT_SLOW_IN, false, true);
    }

    private Animator makeAnimators(float f, float f2, int i, int i2, long j, Interpolator interpolator, boolean z, boolean z2) {
        ValueAnimator valueAnimatorConfigureAnimator = configureAnimator(ValueAnimator.ofInt(i, i2), j, this.mAlphaUpdateListener, interpolator, z);
        ValueAnimator valueAnimatorConfigureAnimator2 = configureAnimator(ValueAnimator.ofFloat(f, f2), j, this.mRadiusUpdateListener, interpolator, z);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(valueAnimatorConfigureAnimator, valueAnimatorConfigureAnimator2);
        if (z2) {
            animatorSet.addListener(new StateUpdateAnimatorListener());
        }
        return animatorSet;
    }

    private ValueAnimator configureAnimator(ValueAnimator valueAnimator, long j, ValueAnimator.AnimatorUpdateListener animatorUpdateListener, Interpolator interpolator, boolean z) {
        valueAnimator.setDuration(j);
        valueAnimator.addUpdateListener(animatorUpdateListener);
        valueAnimator.setInterpolator(interpolator);
        if (z) {
            valueAnimator.setRepeatCount(-1);
            valueAnimator.setRepeatMode(2);
        }
        return valueAnimator;
    }

    private class StateUpdateAnimatorListener extends AnimatorListenerAdapter {
        boolean mCancelled;

        private StateUpdateAnimatorListener() {
        }

        @Override
        public void onAnimationStart(Animator animator) {
            this.mCancelled = false;
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            this.mCancelled = true;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (!this.mCancelled) {
                TrustDrawable.this.updateState(false);
            }
        }
    }
}
