package com.android.systemui.statusbar;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.DisplayListCanvas;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.systemui.Interpolators;

/* loaded from: classes.dex */
public class KeyguardAffordanceView extends ImageView {
    private ValueAnimator mAlphaAnimator;
    private AnimatorListenerAdapter mAlphaEndListener;
    private int mCenterX;
    private int mCenterY;
    private ValueAnimator mCircleAnimator;
    private int mCircleColor;
    private AnimatorListenerAdapter mCircleEndListener;
    private final Paint mCirclePaint;
    private float mCircleRadius;
    private float mCircleStartRadius;
    private float mCircleStartValue;
    private boolean mCircleWillBeHidden;
    private AnimatorListenerAdapter mClipEndListener;
    private final ArgbEvaluator mColorInterpolator;
    private final int mDarkIconColor;
    private boolean mFinishing;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private CanvasProperty<Float> mHwCenterX;
    private CanvasProperty<Float> mHwCenterY;
    private CanvasProperty<Paint> mHwCirclePaint;
    private CanvasProperty<Float> mHwCircleRadius;
    private float mImageScale;
    private boolean mLaunchingAffordance;
    private float mMaxCircleSize;
    private final int mMinBackgroundRadius;
    private final int mNormalColor;
    private Animator mPreviewClipper;
    private View mPreviewView;
    private float mRestingAlpha;
    private ValueAnimator mScaleAnimator;
    private AnimatorListenerAdapter mScaleEndListener;
    private boolean mShouldTint;
    private boolean mSupportHardware;
    private int[] mTempPoint;

    public KeyguardAffordanceView(Context context) {
        this(context, null);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTempPoint = new int[2];
        this.mImageScale = 1.0f;
        this.mRestingAlpha = 0.5f;
        this.mShouldTint = true;
        this.mClipEndListener = new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                KeyguardAffordanceView.this.mPreviewClipper = null;
            }
        };
        this.mCircleEndListener = new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                KeyguardAffordanceView.this.mCircleAnimator = null;
            }
        };
        this.mScaleEndListener = new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.3
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                KeyguardAffordanceView.this.mScaleAnimator = null;
            }
        };
        this.mAlphaEndListener = new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.4
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                KeyguardAffordanceView.this.mAlphaAnimator = null;
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ImageView);
        this.mCirclePaint = new Paint();
        this.mCirclePaint.setAntiAlias(true);
        this.mCircleColor = -1;
        this.mCirclePaint.setColor(this.mCircleColor);
        this.mNormalColor = typedArrayObtainStyledAttributes.getColor(5, -1);
        this.mDarkIconColor = -16777216;
        this.mMinBackgroundRadius = this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.keyguard_affordance_min_background_radius);
        this.mColorInterpolator = new ArgbEvaluator();
        this.mFlingAnimationUtils = new FlingAnimationUtils(this.mContext, 0.3f);
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setImageDrawable(Drawable drawable, boolean z) {
        super.setImageDrawable(drawable);
        this.mShouldTint = z;
        updateIconColor();
    }

    @Override // android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mCenterX = getWidth() / 2;
        this.mCenterY = getHeight() / 2;
        this.mMaxCircleSize = getMaxCircleSize();
    }

    @Override // android.widget.ImageView, android.view.View
    protected void onDraw(Canvas canvas) {
        this.mSupportHardware = canvas.isHardwareAccelerated();
        drawBackgroundCircle(canvas);
        canvas.save();
        canvas.scale(this.mImageScale, this.mImageScale, getWidth() / 2, getHeight() / 2);
        super.onDraw(canvas);
        canvas.restore();
    }

    public void setPreviewView(View view) {
        View view2 = this.mPreviewView;
        this.mPreviewView = view;
        if (this.mPreviewView != null) {
            this.mPreviewView.setVisibility(this.mLaunchingAffordance ? view2.getVisibility() : 4);
        }
    }

    private void updateIconColor() {
        if (this.mShouldTint) {
            getDrawable().mutate().setColorFilter(((Integer) this.mColorInterpolator.evaluate(Math.min(1.0f, this.mCircleRadius / this.mMinBackgroundRadius), Integer.valueOf(this.mNormalColor), Integer.valueOf(this.mDarkIconColor))).intValue(), PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void drawBackgroundCircle(Canvas canvas) {
        if (this.mCircleRadius > 0.0f || this.mFinishing) {
            if (this.mFinishing && this.mSupportHardware && this.mHwCenterX != null) {
                ((DisplayListCanvas) canvas).drawCircle(this.mHwCenterX, this.mHwCenterY, this.mHwCircleRadius, this.mHwCirclePaint);
            } else {
                updateCircleColor();
                canvas.drawCircle(this.mCenterX, this.mCenterY, this.mCircleRadius, this.mCirclePaint);
            }
        }
    }

    private void updateCircleColor() {
        float fMax = 0.5f + (Math.max(0.0f, Math.min(1.0f, (this.mCircleRadius - this.mMinBackgroundRadius) / (this.mMinBackgroundRadius * 0.5f))) * 0.5f);
        if (this.mPreviewView != null && this.mPreviewView.getVisibility() == 0) {
            fMax *= 1.0f - (Math.max(0.0f, this.mCircleRadius - this.mCircleStartRadius) / (this.mMaxCircleSize - this.mCircleStartRadius));
        }
        this.mCirclePaint.setColor(Color.argb((int) (Color.alpha(this.mCircleColor) * fMax), Color.red(this.mCircleColor), Color.green(this.mCircleColor), Color.blue(this.mCircleColor)));
    }

    public void finishAnimation(float f, final Runnable runnable) {
        Animator animatorToRadius;
        cancelAnimator(this.mCircleAnimator);
        cancelAnimator(this.mPreviewClipper);
        this.mFinishing = true;
        this.mCircleStartRadius = this.mCircleRadius;
        final float maxCircleSize = getMaxCircleSize();
        if (this.mSupportHardware) {
            initHwProperties();
            animatorToRadius = getRtAnimatorToRadius(maxCircleSize);
            startRtAlphaFadeIn();
        } else {
            animatorToRadius = getAnimatorToRadius(maxCircleSize);
        }
        Animator animator = animatorToRadius;
        this.mFlingAnimationUtils.applyDismissing(animator, this.mCircleRadius, maxCircleSize, f, maxCircleSize);
        animator.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.5
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator2) {
                runnable.run();
                KeyguardAffordanceView.this.mFinishing = false;
                KeyguardAffordanceView.this.mCircleRadius = maxCircleSize;
                KeyguardAffordanceView.this.invalidate();
            }
        });
        animator.start();
        setImageAlpha(0.0f, true);
        if (this.mPreviewView != null) {
            this.mPreviewView.setVisibility(0);
            this.mPreviewClipper = ViewAnimationUtils.createCircularReveal(this.mPreviewView, getLeft() + this.mCenterX, getTop() + this.mCenterY, this.mCircleRadius, maxCircleSize);
            this.mFlingAnimationUtils.applyDismissing(this.mPreviewClipper, this.mCircleRadius, maxCircleSize, f, maxCircleSize);
            this.mPreviewClipper.addListener(this.mClipEndListener);
            this.mPreviewClipper.start();
            if (this.mSupportHardware) {
                startRtCircleFadeOut(animator.getDuration());
            }
        }
    }

    private void startRtAlphaFadeIn() {
        if (this.mCircleRadius == 0.0f && this.mPreviewView == null) {
            Paint paint = new Paint(this.mCirclePaint);
            paint.setColor(this.mCircleColor);
            paint.setAlpha(0);
            this.mHwCirclePaint = CanvasProperty.createPaint(paint);
            RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(this.mHwCirclePaint, 1, 255.0f);
            renderNodeAnimator.setTarget(this);
            renderNodeAnimator.setInterpolator(Interpolators.ALPHA_IN);
            renderNodeAnimator.setDuration(250L);
            renderNodeAnimator.start();
        }
    }

    public void instantFinishAnimation() {
        cancelAnimator(this.mPreviewClipper);
        if (this.mPreviewView != null) {
            this.mPreviewView.setClipBounds(null);
            this.mPreviewView.setVisibility(0);
        }
        this.mCircleRadius = getMaxCircleSize();
        setImageAlpha(0.0f, false);
        invalidate();
    }

    private void startRtCircleFadeOut(long j) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(this.mHwCirclePaint, 1, 0.0f);
        renderNodeAnimator.setDuration(j);
        renderNodeAnimator.setInterpolator(Interpolators.ALPHA_OUT);
        renderNodeAnimator.setTarget(this);
        renderNodeAnimator.start();
    }

    private Animator getRtAnimatorToRadius(float f) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(this.mHwCircleRadius, f);
        renderNodeAnimator.setTarget(this);
        return renderNodeAnimator;
    }

    private void initHwProperties() {
        this.mHwCenterX = CanvasProperty.createFloat(this.mCenterX);
        this.mHwCenterY = CanvasProperty.createFloat(this.mCenterY);
        this.mHwCirclePaint = CanvasProperty.createPaint(this.mCirclePaint);
        this.mHwCircleRadius = CanvasProperty.createFloat(this.mCircleRadius);
    }

    private float getMaxCircleSize() {
        getLocationInWindow(this.mTempPoint);
        float width = getRootView().getWidth();
        float f = this.mTempPoint[0] + this.mCenterX;
        return (float) Math.hypot(Math.max(width - f, f), this.mTempPoint[1] + this.mCenterY);
    }

    public void setCircleRadius(float f, boolean z) {
        setCircleRadius(f, z, false);
    }

    public void setCircleRadiusWithoutAnimation(float f) {
        cancelAnimator(this.mCircleAnimator);
        setCircleRadius(f, false, true);
    }

    private void setCircleRadius(float f, boolean z, boolean z2) {
        Interpolator interpolator;
        boolean z3 = (this.mCircleAnimator != null && this.mCircleWillBeHidden) || (this.mCircleAnimator == null && this.mCircleRadius == 0.0f);
        boolean z4 = f == 0.0f;
        if (!((z3 == z4 || z2) ? false : true)) {
            if (this.mCircleAnimator == null) {
                this.mCircleRadius = f;
                updateIconColor();
                invalidate();
                if (z4 && this.mPreviewView != null) {
                    this.mPreviewView.setVisibility(4);
                    return;
                }
                return;
            }
            if (!this.mCircleWillBeHidden) {
                this.mCircleAnimator.getValues()[0].setFloatValues(this.mCircleStartValue + (f - this.mMinBackgroundRadius), f);
                this.mCircleAnimator.setCurrentPlayTime(this.mCircleAnimator.getCurrentPlayTime());
                return;
            }
            return;
        }
        cancelAnimator(this.mCircleAnimator);
        cancelAnimator(this.mPreviewClipper);
        ValueAnimator animatorToRadius = getAnimatorToRadius(f);
        if (f == 0.0f) {
            interpolator = Interpolators.FAST_OUT_LINEAR_IN;
        } else {
            interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
        }
        animatorToRadius.setInterpolator(interpolator);
        long jMin = 250;
        if (!z) {
            jMin = Math.min((long) (80.0f * (Math.abs(this.mCircleRadius - f) / this.mMinBackgroundRadius)), 200L);
        }
        animatorToRadius.setDuration(jMin);
        animatorToRadius.start();
        if (this.mPreviewView != null && this.mPreviewView.getVisibility() == 0) {
            this.mPreviewView.setVisibility(0);
            this.mPreviewClipper = ViewAnimationUtils.createCircularReveal(this.mPreviewView, getLeft() + this.mCenterX, getTop() + this.mCenterY, this.mCircleRadius, f);
            this.mPreviewClipper.setInterpolator(interpolator);
            this.mPreviewClipper.setDuration(jMin);
            this.mPreviewClipper.addListener(this.mClipEndListener);
            this.mPreviewClipper.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.6
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    KeyguardAffordanceView.this.mPreviewView.setVisibility(4);
                }
            });
            this.mPreviewClipper.start();
        }
    }

    private ValueAnimator getAnimatorToRadius(float f) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.mCircleRadius, f);
        this.mCircleAnimator = valueAnimatorOfFloat;
        this.mCircleStartValue = this.mCircleRadius;
        this.mCircleWillBeHidden = f == 0.0f;
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.7
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                KeyguardAffordanceView.this.mCircleRadius = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                KeyguardAffordanceView.this.updateIconColor();
                KeyguardAffordanceView.this.invalidate();
            }
        });
        valueAnimatorOfFloat.addListener(this.mCircleEndListener);
        return valueAnimatorOfFloat;
    }

    private void cancelAnimator(Animator animator) {
        if (animator != null) {
            animator.cancel();
        }
    }

    public void setImageScale(float f, boolean z) {
        setImageScale(f, z, -1L, null);
    }

    public void setImageScale(float f, boolean z, long j, Interpolator interpolator) {
        cancelAnimator(this.mScaleAnimator);
        if (!z) {
            this.mImageScale = f;
            invalidate();
            return;
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.mImageScale, f);
        this.mScaleAnimator = valueAnimatorOfFloat;
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.8
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                KeyguardAffordanceView.this.mImageScale = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                KeyguardAffordanceView.this.invalidate();
            }
        });
        valueAnimatorOfFloat.addListener(this.mScaleEndListener);
        if (interpolator == null) {
            if (f == 0.0f) {
                interpolator = Interpolators.FAST_OUT_LINEAR_IN;
            } else {
                interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
            }
        }
        valueAnimatorOfFloat.setInterpolator(interpolator);
        if (j == -1) {
            j = (long) (200.0f * Math.min(1.0f, Math.abs(this.mImageScale - f) / 0.19999999f));
        }
        valueAnimatorOfFloat.setDuration(j);
        valueAnimatorOfFloat.start();
    }

    public void setRestingAlpha(float f) {
        this.mRestingAlpha = f;
        setImageAlpha(f, false);
    }

    public float getRestingAlpha() {
        return this.mRestingAlpha;
    }

    public void setImageAlpha(float f, boolean z) {
        setImageAlpha(f, z, -1L, null, null);
    }

    public void setImageAlpha(float f, boolean z, long j, Interpolator interpolator, Runnable runnable) {
        Interpolator interpolator2;
        cancelAnimator(this.mAlphaAnimator);
        if (this.mLaunchingAffordance) {
            f = 0.0f;
        }
        int i = (int) (f * 255.0f);
        final Drawable background = getBackground();
        if (!z) {
            if (background != null) {
                background.mutate().setAlpha(i);
            }
            setImageAlpha(i);
            return;
        }
        ValueAnimator valueAnimatorOfInt = ValueAnimator.ofInt(getImageAlpha(), i);
        this.mAlphaAnimator = valueAnimatorOfInt;
        valueAnimatorOfInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.9
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int iIntValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                if (background != null) {
                    background.mutate().setAlpha(iIntValue);
                }
                KeyguardAffordanceView.this.setImageAlpha(iIntValue);
            }
        });
        valueAnimatorOfInt.addListener(this.mAlphaEndListener);
        if (interpolator == null) {
            if (f == 0.0f) {
                interpolator2 = Interpolators.FAST_OUT_LINEAR_IN;
            } else {
                interpolator2 = Interpolators.LINEAR_OUT_SLOW_IN;
            }
            interpolator = interpolator2;
        }
        valueAnimatorOfInt.setInterpolator(interpolator);
        if (j == -1) {
            j = (long) (200.0f * Math.min(1.0f, Math.abs(r9 - i) / 255.0f));
        }
        valueAnimatorOfInt.setDuration(j);
        if (runnable != null) {
            valueAnimatorOfInt.addListener(getEndListener(runnable));
        }
        valueAnimatorOfInt.start();
    }

    private Animator.AnimatorListener getEndListener(final Runnable runnable) {
        return new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.KeyguardAffordanceView.10
            boolean mCancelled;

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (!this.mCancelled) {
                    runnable.run();
                }
            }
        };
    }

    public float getCircleRadius() {
        return this.mCircleRadius;
    }

    @Override // android.view.View
    public boolean performClick() {
        if (isClickable()) {
            return super.performClick();
        }
        return false;
    }

    public void setLaunchingAffordance(boolean z) {
        this.mLaunchingAffordance = z;
    }
}
