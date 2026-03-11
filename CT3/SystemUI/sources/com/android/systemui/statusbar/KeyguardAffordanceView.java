package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
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
import com.android.systemui.R;

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
    private boolean mFinishing;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private CanvasProperty<Float> mHwCenterX;
    private CanvasProperty<Float> mHwCenterY;
    private CanvasProperty<Paint> mHwCirclePaint;
    private CanvasProperty<Float> mHwCircleRadius;
    private float mImageScale;
    private final int mInverseColor;
    private boolean mLaunchingAffordance;
    private float mMaxCircleSize;
    private final int mMinBackgroundRadius;
    private final int mNormalColor;
    private Animator mPreviewClipper;
    private View mPreviewView;
    private float mRestingAlpha;
    private ValueAnimator mScaleAnimator;
    private AnimatorListenerAdapter mScaleEndListener;
    private boolean mSupportHardware;
    private int[] mTempPoint;

    public KeyguardAffordanceView(Context context) {
        this(context, null);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyguardAffordanceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTempPoint = new int[2];
        this.mImageScale = 1.0f;
        this.mRestingAlpha = 0.5f;
        this.mClipEndListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mPreviewClipper = null;
            }
        };
        this.mCircleEndListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mCircleAnimator = null;
            }
        };
        this.mScaleEndListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mScaleAnimator = null;
            }
        };
        this.mAlphaEndListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mAlphaAnimator = null;
            }
        };
        this.mCirclePaint = new Paint();
        this.mCirclePaint.setAntiAlias(true);
        this.mCircleColor = -1;
        this.mCirclePaint.setColor(this.mCircleColor);
        this.mNormalColor = -1;
        this.mInverseColor = -16777216;
        this.mMinBackgroundRadius = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_min_background_radius);
        this.mColorInterpolator = new ArgbEvaluator();
        this.mFlingAnimationUtils = new FlingAnimationUtils(this.mContext, 0.3f);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mCenterX = getWidth() / 2;
        this.mCenterY = getHeight() / 2;
        this.mMaxCircleSize = getMaxCircleSize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.mSupportHardware = false;
        drawBackgroundCircle(canvas);
        canvas.save();
        canvas.scale(this.mImageScale, this.mImageScale, getWidth() / 2, getHeight() / 2);
        super.onDraw(canvas);
        canvas.restore();
    }

    public void setPreviewView(View v) {
        View oldPreviewView = this.mPreviewView;
        this.mPreviewView = v;
        if (this.mPreviewView == null) {
            return;
        }
        this.mPreviewView.setVisibility(this.mLaunchingAffordance ? oldPreviewView.getVisibility() : 4);
    }

    public void updateIconColor() {
        Drawable drawable = getDrawable().mutate();
        float alpha = this.mCircleRadius / this.mMinBackgroundRadius;
        int color = ((Integer) this.mColorInterpolator.evaluate(Math.min(1.0f, alpha), Integer.valueOf(this.mNormalColor), Integer.valueOf(this.mInverseColor))).intValue();
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    private void drawBackgroundCircle(Canvas canvas) {
        if (this.mCircleRadius <= 0.0f && !this.mFinishing) {
            return;
        }
        if (this.mFinishing && this.mSupportHardware) {
            DisplayListCanvas displayListCanvas = (DisplayListCanvas) canvas;
            displayListCanvas.drawCircle(this.mHwCenterX, this.mHwCenterY, this.mHwCircleRadius, this.mHwCirclePaint);
        } else {
            updateCircleColor();
            canvas.drawCircle(this.mCenterX, this.mCenterY, this.mCircleRadius, this.mCirclePaint);
        }
    }

    private void updateCircleColor() {
        float fraction = 0.5f + (Math.max(0.0f, Math.min(1.0f, (this.mCircleRadius - this.mMinBackgroundRadius) / (this.mMinBackgroundRadius * 0.5f))) * 0.5f);
        if (this.mPreviewView != null && this.mPreviewView.getVisibility() == 0) {
            float finishingFraction = 1.0f - (Math.max(0.0f, this.mCircleRadius - this.mCircleStartRadius) / (this.mMaxCircleSize - this.mCircleStartRadius));
            fraction *= finishingFraction;
        }
        int color = Color.argb((int) (Color.alpha(this.mCircleColor) * fraction), Color.red(this.mCircleColor), Color.green(this.mCircleColor), Color.blue(this.mCircleColor));
        this.mCirclePaint.setColor(color);
    }

    public void finishAnimation(float velocity, final Runnable mAnimationEndRunnable) {
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
        this.mFlingAnimationUtils.applyDismissing(animatorToRadius, this.mCircleRadius, maxCircleSize, velocity, maxCircleSize);
        animatorToRadius.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimationEndRunnable.run();
                KeyguardAffordanceView.this.mFinishing = false;
                KeyguardAffordanceView.this.mCircleRadius = maxCircleSize;
                KeyguardAffordanceView.this.invalidate();
            }
        });
        animatorToRadius.start();
        setImageAlpha(0.0f, true);
        if (this.mPreviewView == null) {
            return;
        }
        this.mPreviewView.setVisibility(0);
        this.mPreviewClipper = ViewAnimationUtils.createCircularReveal(this.mPreviewView, getLeft() + this.mCenterX, getTop() + this.mCenterY, this.mCircleRadius, maxCircleSize);
        this.mFlingAnimationUtils.applyDismissing(this.mPreviewClipper, this.mCircleRadius, maxCircleSize, velocity, maxCircleSize);
        this.mPreviewClipper.addListener(this.mClipEndListener);
        this.mPreviewClipper.start();
        if (!this.mSupportHardware) {
            return;
        }
        startRtCircleFadeOut(animatorToRadius.getDuration());
    }

    private void startRtAlphaFadeIn() {
        if (this.mCircleRadius != 0.0f || this.mPreviewView != null) {
            return;
        }
        Paint modifiedPaint = new Paint(this.mCirclePaint);
        modifiedPaint.setColor(this.mCircleColor);
        modifiedPaint.setAlpha(0);
        this.mHwCirclePaint = CanvasProperty.createPaint(modifiedPaint);
        RenderNodeAnimator animator = new RenderNodeAnimator(this.mHwCirclePaint, 1, 255.0f);
        animator.setTarget(this);
        animator.setInterpolator(Interpolators.ALPHA_IN);
        animator.setDuration(250L);
        animator.start();
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

    private void startRtCircleFadeOut(long duration) {
        RenderNodeAnimator animator = new RenderNodeAnimator(this.mHwCirclePaint, 1, 0.0f);
        animator.setDuration(duration);
        animator.setInterpolator(Interpolators.ALPHA_OUT);
        animator.setTarget(this);
        animator.start();
    }

    private Animator getRtAnimatorToRadius(float circleRadius) {
        RenderNodeAnimator animator = new RenderNodeAnimator(this.mHwCircleRadius, circleRadius);
        animator.setTarget(this);
        return animator;
    }

    private void initHwProperties() {
        this.mHwCenterX = CanvasProperty.createFloat(this.mCenterX);
        this.mHwCenterY = CanvasProperty.createFloat(this.mCenterY);
        this.mHwCirclePaint = CanvasProperty.createPaint(this.mCirclePaint);
        this.mHwCircleRadius = CanvasProperty.createFloat(this.mCircleRadius);
    }

    private float getMaxCircleSize() {
        getLocationInWindow(this.mTempPoint);
        float rootWidth = getRootView().getWidth();
        float width = this.mTempPoint[0] + this.mCenterX;
        float width2 = Math.max(rootWidth - width, width);
        float height = this.mTempPoint[1] + this.mCenterY;
        return (float) Math.hypot(width2, height);
    }

    public void setCircleRadius(float circleRadius, boolean slowAnimation) {
        setCircleRadius(circleRadius, slowAnimation, false);
    }

    public void setCircleRadiusWithoutAnimation(float circleRadius) {
        cancelAnimator(this.mCircleAnimator);
        setCircleRadius(circleRadius, false, true);
    }

    private void setCircleRadius(float circleRadius, boolean slowAnimation, boolean noAnimation) {
        boolean radiusHidden;
        Interpolator interpolator;
        if (this.mCircleAnimator != null && this.mCircleWillBeHidden) {
            radiusHidden = true;
        } else {
            radiusHidden = this.mCircleAnimator == null && this.mCircleRadius == 0.0f;
        }
        boolean nowHidden = circleRadius == 0.0f;
        boolean radiusNeedsAnimation = (radiusHidden == nowHidden || noAnimation) ? false : true;
        if (!radiusNeedsAnimation) {
            if (this.mCircleAnimator == null) {
                this.mCircleRadius = circleRadius;
                updateIconColor();
                invalidate();
                if (!nowHidden || this.mPreviewView == null) {
                    return;
                }
                this.mPreviewView.setVisibility(4);
                return;
            }
            if (this.mCircleWillBeHidden) {
                return;
            }
            float diff = circleRadius - this.mMinBackgroundRadius;
            PropertyValuesHolder[] values = this.mCircleAnimator.getValues();
            values[0].setFloatValues(this.mCircleStartValue + diff, circleRadius);
            this.mCircleAnimator.setCurrentPlayTime(this.mCircleAnimator.getCurrentPlayTime());
            return;
        }
        cancelAnimator(this.mCircleAnimator);
        cancelAnimator(this.mPreviewClipper);
        ValueAnimator animator = getAnimatorToRadius(circleRadius);
        if (circleRadius == 0.0f) {
            interpolator = Interpolators.FAST_OUT_LINEAR_IN;
        } else {
            interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
        }
        animator.setInterpolator(interpolator);
        long duration = 250;
        if (!slowAnimation) {
            float durationFactor = Math.abs(this.mCircleRadius - circleRadius) / this.mMinBackgroundRadius;
            long duration2 = (long) (80.0f * durationFactor);
            duration = Math.min(duration2, 200L);
        }
        animator.setDuration(duration);
        animator.start();
        if (this.mPreviewView == null || this.mPreviewView.getVisibility() != 0) {
            return;
        }
        this.mPreviewView.setVisibility(0);
        this.mPreviewClipper = ViewAnimationUtils.createCircularReveal(this.mPreviewView, getLeft() + this.mCenterX, getTop() + this.mCenterY, this.mCircleRadius, circleRadius);
        this.mPreviewClipper.setInterpolator(interpolator);
        this.mPreviewClipper.setDuration(duration);
        this.mPreviewClipper.addListener(this.mClipEndListener);
        this.mPreviewClipper.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                KeyguardAffordanceView.this.mPreviewView.setVisibility(4);
            }
        });
        this.mPreviewClipper.start();
    }

    private ValueAnimator getAnimatorToRadius(float circleRadius) {
        ValueAnimator animator = ValueAnimator.ofFloat(this.mCircleRadius, circleRadius);
        this.mCircleAnimator = animator;
        this.mCircleStartValue = this.mCircleRadius;
        this.mCircleWillBeHidden = circleRadius == 0.0f;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                KeyguardAffordanceView.this.mCircleRadius = ((Float) animation.getAnimatedValue()).floatValue();
                KeyguardAffordanceView.this.updateIconColor();
                KeyguardAffordanceView.this.invalidate();
            }
        });
        animator.addListener(this.mCircleEndListener);
        return animator;
    }

    private void cancelAnimator(Animator animator) {
        if (animator == null) {
            return;
        }
        animator.cancel();
    }

    public void setImageScale(float imageScale, boolean animate) {
        setImageScale(imageScale, animate, -1L, null);
    }

    public void setImageScale(float imageScale, boolean animate, long duration, Interpolator interpolator) {
        cancelAnimator(this.mScaleAnimator);
        if (!animate) {
            this.mImageScale = imageScale;
            invalidate();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(this.mImageScale, imageScale);
        this.mScaleAnimator = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                KeyguardAffordanceView.this.mImageScale = ((Float) animation.getAnimatedValue()).floatValue();
                KeyguardAffordanceView.this.invalidate();
            }
        });
        animator.addListener(this.mScaleEndListener);
        if (interpolator == null) {
            if (imageScale == 0.0f) {
                interpolator = Interpolators.FAST_OUT_LINEAR_IN;
            } else {
                interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
            }
        }
        animator.setInterpolator(interpolator);
        if (duration == -1) {
            float durationFactor = Math.abs(this.mImageScale - imageScale) / 0.19999999f;
            duration = (long) (200.0f * Math.min(1.0f, durationFactor));
        }
        animator.setDuration(duration);
        animator.start();
    }

    public void setRestingAlpha(float alpha) {
        this.mRestingAlpha = alpha;
        setImageAlpha(alpha, false);
    }

    public float getRestingAlpha() {
        return this.mRestingAlpha;
    }

    public void setImageAlpha(float alpha, boolean animate) {
        setImageAlpha(alpha, animate, -1L, null, null);
    }

    public void setImageAlpha(float alpha, boolean animate, long duration, Interpolator interpolator, Runnable runnable) {
        cancelAnimator(this.mAlphaAnimator);
        if (this.mLaunchingAffordance) {
            alpha = 0.0f;
        }
        int endAlpha = (int) (alpha * 255.0f);
        final Drawable background = getBackground();
        if (!animate) {
            if (background != null) {
                background.mutate().setAlpha(endAlpha);
            }
            setImageAlpha(endAlpha);
            return;
        }
        int currentAlpha = getImageAlpha();
        ValueAnimator animator = ValueAnimator.ofInt(currentAlpha, endAlpha);
        this.mAlphaAnimator = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int alpha2 = ((Integer) animation.getAnimatedValue()).intValue();
                if (background != null) {
                    background.mutate().setAlpha(alpha2);
                }
                KeyguardAffordanceView.this.setImageAlpha(alpha2);
            }
        });
        animator.addListener(this.mAlphaEndListener);
        if (interpolator == null) {
            if (alpha == 0.0f) {
                interpolator = Interpolators.FAST_OUT_LINEAR_IN;
            } else {
                interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
            }
        }
        animator.setInterpolator(interpolator);
        if (duration == -1) {
            float durationFactor = Math.abs(currentAlpha - endAlpha) / 255.0f;
            duration = (long) (200.0f * Math.min(1.0f, durationFactor));
        }
        animator.setDuration(duration);
        if (runnable != null) {
            animator.addListener(getEndListener(runnable));
        }
        animator.start();
    }

    private Animator.AnimatorListener getEndListener(final Runnable runnable) {
        return new AnimatorListenerAdapter() {
            boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (this.mCancelled) {
                    return;
                }
                runnable.run();
            }
        };
    }

    public float getCircleRadius() {
        return this.mCircleRadius;
    }

    @Override
    public boolean performClick() {
        if (isClickable()) {
            return super.performClick();
        }
        return false;
    }

    public void setLaunchingAffordance(boolean launchingAffordance) {
        this.mLaunchingAffordance = launchingAffordance;
    }
}
