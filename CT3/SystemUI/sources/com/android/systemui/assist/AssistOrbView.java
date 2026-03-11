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

    public AssistOrbView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistOrbView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AssistOrbView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mBackgroundPaint = new Paint();
        this.mCircleRect = new Rect();
        this.mStaticRect = new Rect();
        this.mOvershootInterpolator = new OvershootInterpolator();
        this.mCircleUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                AssistOrbView.this.applyCircleSize(((Float) animation.getAnimatedValue()).floatValue());
                AssistOrbView.this.updateElevation();
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                AssistOrbView.this.mCircleAnimator = null;
            }
        };
        this.mOffsetUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                AssistOrbView.this.mOffset = ((Float) animation.getAnimatedValue()).floatValue();
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
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        this.mLogo.layout(0, 0, this.mLogo.getMeasuredWidth(), this.mLogo.getMeasuredHeight());
        if (!changed) {
            return;
        }
        updateCircleRect(this.mStaticRect, this.mStaticOffset, true);
    }

    public void animateCircleSize(float circleSize, long duration, long startDelay, Interpolator interpolator) {
        if (circleSize == this.mCircleAnimationEndValue) {
            return;
        }
        if (this.mCircleAnimator != null) {
            this.mCircleAnimator.cancel();
        }
        this.mCircleAnimator = ValueAnimator.ofFloat(this.mCircleSize, circleSize);
        this.mCircleAnimator.addUpdateListener(this.mCircleUpdateListener);
        this.mCircleAnimator.addListener(this.mClearAnimatorListener);
        this.mCircleAnimator.setInterpolator(interpolator);
        this.mCircleAnimator.setDuration(duration);
        this.mCircleAnimator.setStartDelay(startDelay);
        this.mCircleAnimator.start();
        this.mCircleAnimationEndValue = circleSize;
    }

    public void applyCircleSize(float circleSize) {
        this.mCircleSize = circleSize;
        updateLayout();
    }

    public void updateElevation() {
        float t = (this.mStaticOffset - this.mOffset) / this.mStaticOffset;
        float offset = (1.0f - Math.max(t, 0.0f)) * this.mMaxElevation;
        setElevation(offset);
    }

    public void animateOffset(float offset, long duration, long startDelay, Interpolator interpolator) {
        if (this.mOffsetAnimator != null) {
            this.mOffsetAnimator.removeAllListeners();
            this.mOffsetAnimator.cancel();
        }
        this.mOffsetAnimator = ValueAnimator.ofFloat(this.mOffset, offset);
        this.mOffsetAnimator.addUpdateListener(this.mOffsetUpdateListener);
        this.mOffsetAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                AssistOrbView.this.mOffsetAnimator = null;
            }
        });
        this.mOffsetAnimator.setInterpolator(interpolator);
        this.mOffsetAnimator.setStartDelay(startDelay);
        this.mOffsetAnimator.setDuration(duration);
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
        boolean clip = this.mCircleSize < ((float) this.mCircleMinSize);
        if (clip == this.mClipToOutline) {
            return;
        }
        setClipToOutline(clip);
        this.mClipToOutline = clip;
    }

    private void updateLogo() {
        float translationX = ((this.mCircleRect.left + this.mCircleRect.right) / 2.0f) - (this.mLogo.getWidth() / 2.0f);
        float translationY = (((this.mCircleRect.top + this.mCircleRect.bottom) / 2.0f) - (this.mLogo.getHeight() / 2.0f)) - (this.mCircleMinSize / 7.0f);
        float t = (this.mStaticOffset - this.mOffset) / this.mStaticOffset;
        float alpha = 1.0f - t;
        this.mLogo.setImageAlpha((int) (255.0f * Math.max((alpha - 0.5f) * 2.0f, 0.0f)));
        this.mLogo.setTranslationX(translationX);
        this.mLogo.setTranslationY(translationY + (this.mStaticOffset * t * 0.1f));
    }

    private void updateCircleRect() {
        updateCircleRect(this.mCircleRect, this.mOffset, false);
    }

    private void updateCircleRect(Rect rect, float offset, boolean useStaticSize) {
        float circleSize = useStaticSize ? this.mCircleMinSize : this.mCircleSize;
        int left = ((int) (getWidth() - circleSize)) / 2;
        int top = (int) (((getHeight() - (circleSize / 2.0f)) - this.mBaseMargin) - offset);
        rect.set(left, top, (int) (left + circleSize), (int) (top + circleSize));
    }

    public void startExitAnimation(long delay) {
        animateCircleSize(0.0f, 200L, delay, Interpolators.FAST_OUT_LINEAR_IN);
        animateOffset(0.0f, 200L, delay, Interpolators.FAST_OUT_LINEAR_IN);
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
