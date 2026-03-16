package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.util.ArrayList;

public class SearchPanelCircleView extends FrameLayout {
    private boolean mAnimatingOut;
    private final Interpolator mAppearInterpolator;
    private final Paint mBackgroundPaint;
    private final int mBaseMargin;
    private float mCircleAnimationEndValue;
    private ValueAnimator mCircleAnimator;
    private boolean mCircleHidden;
    private final int mCircleMinSize;
    private final Rect mCircleRect;
    private float mCircleSize;
    private ValueAnimator.AnimatorUpdateListener mCircleUpdateListener;
    private AnimatorListenerAdapter mClearAnimatorListener;
    private boolean mClipToOutline;
    private final Interpolator mDisappearInterpolator;
    private boolean mDraggedFarEnough;
    private ValueAnimator mFadeOutAnimator;
    private final Interpolator mFastOutSlowInInterpolator;
    private boolean mHorizontal;
    private ImageView mLogo;
    private final int mMaxElevation;
    private float mOffset;
    private boolean mOffsetAnimatingIn;
    private ValueAnimator mOffsetAnimator;
    private ValueAnimator.AnimatorUpdateListener mOffsetUpdateListener;
    private float mOutlineAlpha;
    private final Paint mRipplePaint;
    private ArrayList<Ripple> mRipples;
    private final int mStaticOffset;
    private final Rect mStaticRect;

    public SearchPanelCircleView(Context context) {
        this(context, null);
    }

    public SearchPanelCircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchPanelCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SearchPanelCircleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mBackgroundPaint = new Paint();
        this.mRipplePaint = new Paint();
        this.mCircleRect = new Rect();
        this.mStaticRect = new Rect();
        this.mRipples = new ArrayList<>();
        this.mCircleUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                SearchPanelCircleView.this.applyCircleSize(((Float) animation.getAnimatedValue()).floatValue());
                SearchPanelCircleView.this.updateElevation();
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SearchPanelCircleView.this.mCircleAnimator = null;
            }
        };
        this.mOffsetUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                SearchPanelCircleView.this.setOffset(((Float) animation.getAnimatedValue()).floatValue());
            }
        };
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (SearchPanelCircleView.this.mCircleSize > 0.0f) {
                    outline.setOval(SearchPanelCircleView.this.mCircleRect);
                } else {
                    outline.setEmpty();
                }
                outline.setAlpha(SearchPanelCircleView.this.mOutlineAlpha);
            }
        });
        setWillNotDraw(false);
        this.mCircleMinSize = context.getResources().getDimensionPixelSize(R.dimen.search_panel_circle_size);
        this.mBaseMargin = context.getResources().getDimensionPixelSize(R.dimen.search_panel_circle_base_margin);
        this.mStaticOffset = context.getResources().getDimensionPixelSize(R.dimen.search_panel_circle_travel_distance);
        this.mMaxElevation = context.getResources().getDimensionPixelSize(R.dimen.search_panel_circle_elevation);
        this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.linear_out_slow_in);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_slow_in);
        this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in);
        this.mBackgroundPaint.setAntiAlias(true);
        this.mBackgroundPaint.setColor(getResources().getColor(R.color.search_panel_circle_color));
        this.mRipplePaint.setColor(getResources().getColor(R.color.search_panel_ripple_color));
        this.mRipplePaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawRipples(canvas);
    }

    private void drawRipples(Canvas canvas) {
        for (int i = 0; i < this.mRipples.size(); i++) {
            Ripple ripple = this.mRipples.get(i);
            ripple.draw(canvas);
        }
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
        if (changed) {
            updateCircleRect(this.mStaticRect, this.mStaticOffset, true);
        }
    }

    public void setCircleSize(float circleSize) {
        setCircleSize(circleSize, false, null, 0, null);
    }

    public void setCircleSize(float circleSize, boolean animated, final Runnable endRunnable, int startDelay, Interpolator interpolator) {
        boolean isAnimating = this.mCircleAnimator != null;
        boolean animationPending = isAnimating && !this.mCircleAnimator.isRunning();
        boolean animatingOut = isAnimating && this.mCircleAnimationEndValue == 0.0f;
        if (animated || animationPending || animatingOut) {
            if (isAnimating) {
                if (circleSize != this.mCircleAnimationEndValue) {
                    this.mCircleAnimator.cancel();
                } else {
                    return;
                }
            }
            this.mCircleAnimator = ValueAnimator.ofFloat(this.mCircleSize, circleSize);
            this.mCircleAnimator.addUpdateListener(this.mCircleUpdateListener);
            this.mCircleAnimator.addListener(this.mClearAnimatorListener);
            this.mCircleAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (endRunnable != null) {
                        endRunnable.run();
                    }
                }
            });
            Interpolator desiredInterpolator = interpolator != null ? interpolator : circleSize == 0.0f ? this.mDisappearInterpolator : this.mAppearInterpolator;
            this.mCircleAnimator.setInterpolator(desiredInterpolator);
            this.mCircleAnimator.setDuration(300L);
            this.mCircleAnimator.setStartDelay(startDelay);
            this.mCircleAnimator.start();
            this.mCircleAnimationEndValue = circleSize;
            return;
        }
        if (isAnimating) {
            float diff = circleSize - this.mCircleAnimationEndValue;
            PropertyValuesHolder[] values = this.mCircleAnimator.getValues();
            values[0].setFloatValues(diff, circleSize);
            this.mCircleAnimator.setCurrentPlayTime(this.mCircleAnimator.getCurrentPlayTime());
            this.mCircleAnimationEndValue = circleSize;
            return;
        }
        applyCircleSize(circleSize);
        updateElevation();
    }

    private void applyCircleSize(float circleSize) {
        this.mCircleSize = circleSize;
        updateLayout();
    }

    private void updateElevation() {
        float t = (this.mStaticOffset - this.mOffset) / this.mStaticOffset;
        float offset = (1.0f - Math.max(t, 0.0f)) * this.mMaxElevation;
        setElevation(offset);
    }

    public void setOffset(float offset) {
        setOffset(offset, false, 0, null, null);
    }

    private void setOffset(float offset, boolean animate, int startDelay, Interpolator interpolator, final Runnable endRunnable) {
        if (!animate) {
            this.mOffset = offset;
            updateLayout();
            if (endRunnable != null) {
                endRunnable.run();
                return;
            }
            return;
        }
        if (this.mOffsetAnimator != null) {
            this.mOffsetAnimator.removeAllListeners();
            this.mOffsetAnimator.cancel();
        }
        this.mOffsetAnimator = ValueAnimator.ofFloat(this.mOffset, offset);
        this.mOffsetAnimator.addUpdateListener(this.mOffsetUpdateListener);
        this.mOffsetAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SearchPanelCircleView.this.mOffsetAnimator = null;
                if (endRunnable != null) {
                    endRunnable.run();
                }
            }
        });
        Interpolator desiredInterpolator = interpolator != null ? interpolator : offset == 0.0f ? this.mDisappearInterpolator : this.mAppearInterpolator;
        this.mOffsetAnimator.setInterpolator(desiredInterpolator);
        this.mOffsetAnimator.setStartDelay(startDelay);
        this.mOffsetAnimator.setDuration(300L);
        this.mOffsetAnimator.start();
        this.mOffsetAnimatingIn = offset != 0.0f;
    }

    private void updateLayout() {
        updateCircleRect();
        updateLogo();
        invalidateOutline();
        invalidate();
        updateClipping();
    }

    private void updateClipping() {
        boolean clip = this.mCircleSize < ((float) this.mCircleMinSize) || !this.mRipples.isEmpty();
        if (clip != this.mClipToOutline) {
            setClipToOutline(clip);
            this.mClipToOutline = clip;
        }
    }

    private void updateLogo() {
        boolean exitAnimationRunning = this.mFadeOutAnimator != null;
        Rect rect = exitAnimationRunning ? this.mCircleRect : this.mStaticRect;
        float translationX = ((rect.left + rect.right) / 2.0f) - (this.mLogo.getWidth() / 2.0f);
        float translationY = ((rect.top + rect.bottom) / 2.0f) - (this.mLogo.getHeight() / 2.0f);
        float t = (this.mStaticOffset - this.mOffset) / this.mStaticOffset;
        if (!exitAnimationRunning) {
            if (this.mHorizontal) {
                translationX += this.mStaticOffset * t * 0.3f;
            } else {
                translationY += this.mStaticOffset * t * 0.3f;
            }
            float alpha = 1.0f - t;
            this.mLogo.setAlpha(Math.max((alpha - 0.5f) * 2.0f, 0.0f));
        } else {
            translationY += (this.mOffset - this.mStaticOffset) / 2.0f;
        }
        this.mLogo.setTranslationX(translationX);
        this.mLogo.setTranslationY(translationY);
    }

    private void updateCircleRect() {
        updateCircleRect(this.mCircleRect, this.mOffset, false);
    }

    private void updateCircleRect(Rect rect, float offset, boolean useStaticSize) {
        int left;
        int top;
        float circleSize = useStaticSize ? this.mCircleMinSize : this.mCircleSize;
        if (this.mHorizontal) {
            left = (int) (((getWidth() - (circleSize / 2.0f)) - this.mBaseMargin) - offset);
            top = (int) ((getHeight() - circleSize) / 2.0f);
        } else {
            left = ((int) (getWidth() - circleSize)) / 2;
            top = (int) (((getHeight() - (circleSize / 2.0f)) - this.mBaseMargin) - offset);
        }
        rect.set(left, top, (int) (left + circleSize), (int) (top + circleSize));
    }

    public void setHorizontal(boolean horizontal) {
        this.mHorizontal = horizontal;
        updateCircleRect(this.mStaticRect, this.mStaticOffset, true);
        updateLayout();
    }

    public void setDragDistance(float distance) {
        if (this.mAnimatingOut) {
            return;
        }
        if (!this.mCircleHidden || this.mDraggedFarEnough) {
            float circleSize = this.mCircleMinSize + rubberband(distance);
            setCircleSize(circleSize);
        }
    }

    private float rubberband(float diff) {
        return (float) Math.pow(Math.abs(diff), 0.6000000238418579d);
    }

    public void startAbortAnimation(Runnable endRunnable) {
        if (this.mAnimatingOut) {
            if (endRunnable != null) {
                endRunnable.run();
            }
        } else {
            setCircleSize(0.0f, true, null, 0, null);
            setOffset(0.0f, true, 0, null, endRunnable);
            this.mCircleHidden = true;
        }
    }

    public void startEnterAnimation() {
        if (!this.mAnimatingOut) {
            applyCircleSize(0.0f);
            setOffset(0.0f);
            setCircleSize(this.mCircleMinSize, true, null, 50, null);
            setOffset(this.mStaticOffset, true, 50, null, null);
            this.mCircleHidden = false;
        }
    }

    public void startExitAnimation(Runnable endRunnable) {
        if (!this.mHorizontal) {
            float offset = getHeight() / 2.0f;
            setOffset(offset - this.mBaseMargin, true, 50, this.mFastOutSlowInInterpolator, null);
            float xMax = getWidth() / 2;
            float yMax = getHeight() / 2;
            float maxRadius = (float) Math.ceil(Math.hypot(xMax, yMax) * 2.0d);
            setCircleSize(maxRadius, true, null, 50, this.mFastOutSlowInInterpolator);
            performExitFadeOutAnimation(50, 300, endRunnable);
            return;
        }
        endRunnable.run();
    }

    private void performExitFadeOutAnimation(int startDelay, int duration, final Runnable endRunnable) {
        this.mFadeOutAnimator = ValueAnimator.ofFloat(this.mBackgroundPaint.getAlpha() / 255.0f, 0.0f);
        this.mFadeOutAnimator.setInterpolator(new LinearInterpolator());
        this.mFadeOutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = animation.getAnimatedFraction();
                float logoValue = animatedFraction > 0.5f ? 1.0f : animatedFraction / 0.5f;
                float logoValue2 = PhoneStatusBar.ALPHA_OUT.getInterpolation(1.0f - logoValue);
                float backgroundValue = 1.0f - (animatedFraction < 0.2f ? 0.0f : PhoneStatusBar.ALPHA_OUT.getInterpolation((animatedFraction - 0.2f) / 0.8f));
                SearchPanelCircleView.this.mBackgroundPaint.setAlpha((int) (255.0f * backgroundValue));
                SearchPanelCircleView.this.mOutlineAlpha = backgroundValue;
                SearchPanelCircleView.this.mLogo.setAlpha(logoValue2);
                SearchPanelCircleView.this.invalidateOutline();
                SearchPanelCircleView.this.invalidate();
            }
        });
        this.mFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (endRunnable != null) {
                    endRunnable.run();
                }
                SearchPanelCircleView.this.mLogo.setAlpha(1.0f);
                SearchPanelCircleView.this.mBackgroundPaint.setAlpha(255);
                SearchPanelCircleView.this.mOutlineAlpha = 1.0f;
                SearchPanelCircleView.this.mFadeOutAnimator = null;
            }
        });
        this.mFadeOutAnimator.setStartDelay(startDelay);
        this.mFadeOutAnimator.setDuration(duration);
        this.mFadeOutAnimator.start();
    }

    public void setDraggedFarEnough(boolean farEnough) {
        if (farEnough != this.mDraggedFarEnough) {
            if (farEnough) {
                if (this.mCircleHidden) {
                    startEnterAnimation();
                }
                if (this.mOffsetAnimator == null) {
                    addRipple();
                } else {
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            SearchPanelCircleView.this.addRipple();
                        }
                    }, 100L);
                }
            } else {
                startAbortAnimation(null);
            }
            this.mDraggedFarEnough = farEnough;
        }
    }

    private void addRipple() {
        float xInterpolation;
        float yInterpolation;
        if (this.mRipples.size() <= 1) {
            if (this.mHorizontal) {
                xInterpolation = 0.75f;
                yInterpolation = 0.5f;
            } else {
                xInterpolation = 0.5f;
                yInterpolation = 0.75f;
            }
            float circleCenterX = (this.mStaticRect.left * (1.0f - xInterpolation)) + (this.mStaticRect.right * xInterpolation);
            float circleCenterY = (this.mStaticRect.top * (1.0f - yInterpolation)) + (this.mStaticRect.bottom * yInterpolation);
            float radius = Math.max(this.mCircleSize, this.mCircleMinSize * 1.25f) * 0.75f;
            Ripple ripple = new Ripple(circleCenterX, circleCenterY, radius);
            ripple.start();
        }
    }

    public void reset() {
        this.mDraggedFarEnough = false;
        this.mAnimatingOut = false;
        this.mCircleHidden = true;
        this.mClipToOutline = false;
        if (this.mFadeOutAnimator != null) {
            this.mFadeOutAnimator.cancel();
        }
        this.mBackgroundPaint.setAlpha(255);
        this.mOutlineAlpha = 1.0f;
    }

    public boolean isAnimationRunning(boolean enterAnimation) {
        return this.mOffsetAnimator != null && enterAnimation == this.mOffsetAnimatingIn;
    }

    public void performOnAnimationFinished(final Runnable runnable) {
        if (this.mOffsetAnimator != null) {
            this.mOffsetAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            });
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public void setAnimatingOut(boolean animatingOut) {
        this.mAnimatingOut = animatingOut;
    }

    public boolean isAnimatingOut() {
        return this.mAnimatingOut;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private class Ripple {
        float alpha;
        float endRadius;
        float radius;
        float x;
        float y;

        Ripple(float x, float y, float endRadius) {
            this.x = x;
            this.y = y;
            this.endRadius = endRadius;
        }

        void start() {
            ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    Ripple.this.alpha = 1.0f - animation.getAnimatedFraction();
                    Ripple.this.alpha = SearchPanelCircleView.this.mDisappearInterpolator.getInterpolation(Ripple.this.alpha);
                    Ripple.this.radius = SearchPanelCircleView.this.mAppearInterpolator.getInterpolation(animation.getAnimatedFraction());
                    Ripple.this.radius *= Ripple.this.endRadius;
                    SearchPanelCircleView.this.invalidate();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    SearchPanelCircleView.this.mRipples.remove(Ripple.this);
                    SearchPanelCircleView.this.updateClipping();
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    SearchPanelCircleView.this.mRipples.add(Ripple.this);
                    SearchPanelCircleView.this.updateClipping();
                }
            });
            animator.setDuration(400L);
            animator.start();
        }

        public void draw(Canvas canvas) {
            SearchPanelCircleView.this.mRipplePaint.setAlpha((int) (this.alpha * 255.0f));
            canvas.drawCircle(this.x, this.y, this.radius, SearchPanelCircleView.this.mRipplePaint);
        }
    }
}
