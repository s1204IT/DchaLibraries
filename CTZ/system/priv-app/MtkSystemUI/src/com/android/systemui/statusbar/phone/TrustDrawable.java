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

/* loaded from: classes.dex */
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
    private final ValueAnimator.AnimatorUpdateListener mAlphaUpdateListener = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.TrustDrawable.1
        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            TrustDrawable.this.mCurAlpha = ((Integer) valueAnimator.getAnimatedValue()).intValue();
            TrustDrawable.this.invalidateSelf();
        }
    };
    private final ValueAnimator.AnimatorUpdateListener mRadiusUpdateListener = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.TrustDrawable.2
        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
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

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        int i = (this.mCurAlpha * this.mAlpha) / 256;
        if (i == 0) {
            return;
        }
        Rect bounds = getBounds();
        this.mPaint.setAlpha(i);
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), this.mCurInnerRadius, this.mPaint);
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        this.mAlpha = i;
    }

    @Override // android.graphics.drawable.Drawable
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override // android.graphics.drawable.Drawable
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

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:32:0x003e */
    /* JADX DEBUG: Multi-variable search result rejected for r0v9, resolved type: boolean */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:12:0x001a  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x0024  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void updateState(boolean z) {
        int i;
        if (!this.mAnimating) {
            return;
        }
        int i2 = this.mState;
        if (this.mState == -1) {
            i = this.mTrustManaged;
        } else if (this.mState == 0) {
            i = i2;
            if (this.mTrustManaged) {
                i = 1;
            }
        } else if (this.mState == 1) {
            i = i2;
            if (!this.mTrustManaged) {
                i = 3;
            }
        } else if (this.mState == 2) {
            i = i2;
            if (!this.mTrustManaged) {
            }
        } else {
            i = i2;
            if (this.mState == 3) {
                i = i2;
                if (this.mTrustManaged) {
                }
            }
        }
        i = i;
        if (!z) {
            if (i == 1) {
                i = 2;
            }
            if (i == 3) {
                i = 0;
            }
        }
        if (i != this.mState) {
            if (this.mCurAnimator != null) {
                this.mCurAnimator.cancel();
                this.mCurAnimator = null;
            }
            if (i == 0) {
                this.mCurAlpha = 0;
                this.mCurInnerRadius = this.mInnerRadiusEnter;
            } else if (i == 1) {
                this.mCurAnimator = makeEnterAnimator(this.mCurInnerRadius, this.mCurAlpha);
                if (this.mState == -1) {
                    this.mCurAnimator.setStartDelay(200L);
                }
            } else if (i == 2) {
                this.mCurAlpha = 76;
                this.mCurInnerRadius = this.mInnerRadiusVisibleMax;
                this.mCurAnimator = this.mVisibleAnimator;
            } else if (i == 3) {
                this.mCurAnimator = makeExitAnimator(this.mCurInnerRadius, this.mCurAlpha);
            }
            this.mState = i;
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

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
            this.mCancelled = false;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            this.mCancelled = true;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (!this.mCancelled) {
                TrustDrawable.this.updateState(false);
            }
        }
    }
}
