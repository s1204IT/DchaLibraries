package com.android.systemui.assist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class AssistOrbView extends FrameLayout {
    private final Paint mBackgroundPaint;
    private final int mBaseMargin;
    private float mCircleAnimationEndValue;
    private ValueAnimator mCircleAnimator;
    private final int mCircleMinSize;
    private final Rect mCircleRect;
    private float mCircleSize;
    private ValueAnimator.AnimatorUpdateListener mCircleUpdateListener;
    private AnimatorListenerAdapter mClearAnimatorListener;
    private boolean mClipToOutline;
    private ImageView mLogo;
    private final int mMaxElevation;
    private float mOffset;
    private ValueAnimator mOffsetAnimator;
    private ValueAnimator.AnimatorUpdateListener mOffsetUpdateListener;
    private float mOutlineAlpha;
    private final Interpolator mOvershootInterpolator;
    private final int mStaticOffset;
    private final Rect mStaticRect;

    public AssistOrbView(Context context) {
        this(context, null);
    }

    public AssistOrbView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AssistOrbView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AssistOrbView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mBackgroundPaint = new Paint();
        this.mCircleRect = new Rect();
        this.mStaticRect = new Rect();
        this.mOvershootInterpolator = new OvershootInterpolator();
        this.mCircleUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                AssistOrbView.this.applyCircleSize(((Float) valueAnimator.getAnimatedValue()).floatValue());
                AssistOrbView.this.updateElevation();
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                AssistOrbView.this.mCircleAnimator = null;
            }
        };
        this.mOffsetUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                AssistOrbView.this.mOffset = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                AssistOrbView.this.updateLayout();
            }
        };
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (AssistOrbView.this.mCircleSize > 0.0f) {
                    outline.setOval(AssistOrbView.this.mCircleRect);
                } else {
                    outline.setEmpty();
                }
                outline.setAlpha(AssistOrbView.this.mOutlineAlpha);
            }
        });
        setWillNotDraw(false);
        this.mCircleMinSize = context.getResources().getDimensionPixelSize(R.dimen.assist_orb_size);
        this.mBaseMargin = context.getResources().getDimensionPixelSize(R.dimen.assist_orb_base_margin);
        this.mStaticOffset = context.getResources().getDimensionPixelSize(R.dimen.assist_orb_travel_distance);
        this.mMaxElevation = context.getResources().getDimensionPixelSize(R.dimen.assist_orb_elevation);
        this.mBackgroundPaint.setAntiAlias(true);
        this.mBackgroundPaint.setColor(getResources().getColor(R.color.assist_orb_color));
    }

    public ImageView getLogo() {
        return this.mLogo;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawCircle(this.mCircleRect.centerX(), this.mCircleRect.centerY(), this.mCircleSize / 2.0f, this.mBackgroundPaint);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLogo = (ImageView) findViewById(R.id.search_logo);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mLogo.layout(0, 0, this.mLogo.getMeasuredWidth(), this.mLogo.getMeasuredHeight());
        if (z) {
            updateCircleRect(this.mStaticRect, this.mStaticOffset, true);
        }
    }

    public void animateCircleSize(float f, long j, long j2, Interpolator interpolator) {
        if (f == this.mCircleAnimationEndValue) {
            return;
        }
        if (this.mCircleAnimator != null) {
            this.mCircleAnimator.cancel();
        }
        this.mCircleAnimator = ValueAnimator.ofFloat(this.mCircleSize, f);
        this.mCircleAnimator.addUpdateListener(this.mCircleUpdateListener);
        this.mCircleAnimator.addListener(this.mClearAnimatorListener);
        this.mCircleAnimator.setInterpolator(interpolator);
        this.mCircleAnimator.setDuration(j);
        this.mCircleAnimator.setStartDelay(j2);
        this.mCircleAnimator.start();
        this.mCircleAnimationEndValue = f;
    }

    public void applyCircleSize(float f) {
        this.mCircleSize = f;
        updateLayout();
    }

    public void updateElevation() {
        setElevation((1.0f - Math.max((this.mStaticOffset - this.mOffset) / this.mStaticOffset, 0.0f)) * this.mMaxElevation);
    }

    public void animateOffset(float f, long j, long j2, Interpolator interpolator) {
        if (this.mOffsetAnimator != null) {
            this.mOffsetAnimator.removeAllListeners();
            this.mOffsetAnimator.cancel();
        }
        this.mOffsetAnimator = ValueAnimator.ofFloat(this.mOffset, f);
        this.mOffsetAnimator.addUpdateListener(this.mOffsetUpdateListener);
        this.mOffsetAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                AssistOrbView.this.mOffsetAnimator = null;
            }
        });
        this.mOffsetAnimator.setInterpolator(interpolator);
        this.mOffsetAnimator.setStartDelay(j2);
        this.mOffsetAnimator.setDuration(j);
        this.mOffsetAnimator.start();
    }

    public void updateLayout() {
        updateCircleRect();
        updateLogo();
        invalidateOutline();
        invalidate();
        updateClipping();
    }

    private void updateClipping() {
        boolean z = this.mCircleSize < ((float) this.mCircleMinSize);
        if (z != this.mClipToOutline) {
            setClipToOutline(z);
            this.mClipToOutline = z;
        }
    }

    private void updateLogo() {
        float f = (this.mStaticOffset - this.mOffset) / this.mStaticOffset;
        this.mLogo.setImageAlpha((int) (Math.max(((1.0f - f) - 0.5f) * 2.0f, 0.0f) * 255.0f));
        this.mLogo.setTranslationX(((this.mCircleRect.left + this.mCircleRect.right) / 2.0f) - (this.mLogo.getWidth() / 2.0f));
        this.mLogo.setTranslationY(((((this.mCircleRect.top + this.mCircleRect.bottom) / 2.0f) - (this.mLogo.getHeight() / 2.0f)) - (this.mCircleMinSize / 7.0f)) + (this.mStaticOffset * f * 0.1f));
    }

    private void updateCircleRect() {
        updateCircleRect(this.mCircleRect, this.mOffset, false);
    }

    private void updateCircleRect(Rect rect, float f, boolean z) {
        float f2 = z ? this.mCircleMinSize : this.mCircleSize;
        int width = ((int) (getWidth() - f2)) / 2;
        int height = (int) (((getHeight() - (f2 / 2.0f)) - this.mBaseMargin) - f);
        rect.set(width, height, (int) (width + f2), (int) (height + f2));
    }

    public void startExitAnimation(long j) {
        animateCircleSize(0.0f, 200L, j, Interpolators.FAST_OUT_LINEAR_IN);
        animateOffset(0.0f, 200L, j, Interpolators.FAST_OUT_LINEAR_IN);
    }

    public void startEnterAnimation() {
        applyCircleSize(0.0f);
        post(new Runnable() {
            @Override
            public void run() {
                AssistOrbView.this.animateCircleSize(AssistOrbView.this.mCircleMinSize, 300L, 0L, AssistOrbView.this.mOvershootInterpolator);
                AssistOrbView.this.animateOffset(AssistOrbView.this.mStaticOffset, 400L, 0L, Interpolators.LINEAR_OUT_SLOW_IN);
            }
        });
    }

    public void reset() {
        this.mClipToOutline = false;
        this.mBackgroundPaint.setAlpha(255);
        this.mOutlineAlpha = 1.0f;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
