package com.android.systemui.statusbar;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

public abstract class ActivatableNotificationView extends ExpandableOutlineView {
    private boolean mActivated;
    private float mAnimationTranslationY;
    private PorterDuffColorFilter mAppearAnimationFilter;
    private float mAppearAnimationFraction;
    private RectF mAppearAnimationRect;
    private float mAppearAnimationTranslation;
    private ValueAnimator mAppearAnimator;
    private Paint mAppearPaint;
    private ObjectAnimator mBackgroundAnimator;
    private NotificationBackgroundView mBackgroundDimmed;
    private NotificationBackgroundView mBackgroundNormal;
    private int mBgTint;
    private Interpolator mCurrentAlphaInterpolator;
    private Interpolator mCurrentAppearInterpolator;
    private boolean mDark;
    private boolean mDimmed;
    private float mDownX;
    private float mDownY;
    private boolean mDrawingAppearAnimation;
    private final Interpolator mFastOutSlowInInterpolator;
    private boolean mIsBelowSpeedBump;
    private final int mLegacyColor;
    private final Interpolator mLinearInterpolator;
    private final Interpolator mLinearOutSlowInInterpolator;
    private final int mLowPriorityColor;
    private final int mLowPriorityRippleColor;
    private final int mNormalColor;
    private final int mNormalRippleColor;
    private OnActivatedListener mOnActivatedListener;
    private final int mRoundedRectCornerRadius;
    private boolean mShowingLegacyBackground;
    private final Interpolator mSlowOutFastInInterpolator;
    private final Interpolator mSlowOutLinearInInterpolator;
    private final Runnable mTapTimeoutRunnable;
    private final int mTintedRippleColor;
    private final float mTouchSlop;
    private static final Interpolator ACTIVATE_INVERSE_INTERPOLATOR = new PathInterpolator(0.6f, 0.0f, 0.5f, 1.0f);
    private static final Interpolator ACTIVATE_INVERSE_ALPHA_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.5f, 1.0f);

    public interface OnActivatedListener {
        void onActivated(ActivatableNotificationView activatableNotificationView);

        void onActivationReset(ActivatableNotificationView activatableNotificationView);
    }

    public ActivatableNotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBgTint = 0;
        this.mAppearAnimationRect = new RectF();
        this.mAppearPaint = new Paint();
        this.mAppearAnimationFraction = -1.0f;
        this.mTapTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                ActivatableNotificationView.this.makeInactive(true);
            }
        };
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
        this.mSlowOutFastInInterpolator = new PathInterpolator(0.8f, 0.0f, 0.6f, 1.0f);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.mSlowOutLinearInInterpolator = new PathInterpolator(0.8f, 0.0f, 1.0f, 1.0f);
        this.mLinearInterpolator = new LinearInterpolator();
        setClipChildren(false);
        setClipToPadding(false);
        this.mAppearAnimationFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);
        this.mRoundedRectCornerRadius = getResources().getDimensionPixelSize(com.android.systemui.R.dimen.notification_material_rounded_rect_radius);
        this.mLegacyColor = getResources().getColor(com.android.systemui.R.color.notification_legacy_background_color);
        this.mNormalColor = getResources().getColor(com.android.systemui.R.color.notification_material_background_color);
        this.mLowPriorityColor = getResources().getColor(com.android.systemui.R.color.notification_material_background_low_priority_color);
        this.mTintedRippleColor = context.getResources().getColor(com.android.systemui.R.color.notification_ripple_tinted_color);
        this.mLowPriorityRippleColor = context.getResources().getColor(com.android.systemui.R.color.notification_ripple_color_low_priority);
        this.mNormalRippleColor = context.getResources().getColor(com.android.systemui.R.color.notification_ripple_untinted_color);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mBackgroundNormal = (NotificationBackgroundView) findViewById(com.android.systemui.R.id.backgroundNormal);
        this.mBackgroundDimmed = (NotificationBackgroundView) findViewById(com.android.systemui.R.id.backgroundDimmed);
        this.mBackgroundNormal.setCustomBackground(com.android.systemui.R.drawable.notification_material_bg);
        this.mBackgroundDimmed.setCustomBackground(com.android.systemui.R.drawable.notification_material_bg_dim);
        updateBackground();
        updateBackgroundTint();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.mDimmed ? handleTouchEventDimmed(event) : super.onTouchEvent(event);
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        if (!this.mDimmed) {
            this.mBackgroundNormal.drawableHotspotChanged(x, y);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mDimmed) {
            this.mBackgroundDimmed.setState(getDrawableState());
        } else {
            this.mBackgroundNormal.setState(getDrawableState());
        }
    }

    private boolean handleTouchEventDimmed(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case 0:
                this.mDownX = event.getX();
                this.mDownY = event.getY();
                if (this.mDownY > getActualHeight()) {
                    return false;
                }
                break;
            case 1:
                if (isWithinTouchSlop(event)) {
                    if (!this.mActivated) {
                        makeActive();
                        postDelayed(this.mTapTimeoutRunnable, 1200L);
                    } else {
                        boolean performed = performClick();
                        if (performed) {
                            removeCallbacks(this.mTapTimeoutRunnable);
                        }
                    }
                } else {
                    makeInactive(true);
                }
                break;
            case 2:
                if (!isWithinTouchSlop(event)) {
                    makeInactive(true);
                    return false;
                }
                break;
            case 3:
                makeInactive(true);
                break;
        }
        return true;
    }

    private void makeActive() {
        startActivateAnimation(false);
        this.mActivated = true;
        if (this.mOnActivatedListener != null) {
            this.mOnActivatedListener.onActivated(this);
        }
    }

    private void startActivateAnimation(boolean reverse) {
        Animator animator;
        Interpolator interpolator;
        Interpolator alphaInterpolator;
        if (isAttachedToWindow()) {
            int widthHalf = this.mBackgroundNormal.getWidth() / 2;
            int heightHalf = this.mBackgroundNormal.getActualHeight() / 2;
            float radius = (float) Math.sqrt((widthHalf * widthHalf) + (heightHalf * heightHalf));
            if (reverse) {
                animator = ViewAnimationUtils.createCircularReveal(this.mBackgroundNormal, widthHalf, heightHalf, radius, 0.0f);
            } else {
                animator = ViewAnimationUtils.createCircularReveal(this.mBackgroundNormal, widthHalf, heightHalf, 0.0f, radius);
            }
            this.mBackgroundNormal.setVisibility(0);
            if (!reverse) {
                interpolator = this.mLinearOutSlowInInterpolator;
                alphaInterpolator = this.mLinearOutSlowInInterpolator;
            } else {
                interpolator = ACTIVATE_INVERSE_INTERPOLATOR;
                alphaInterpolator = ACTIVATE_INVERSE_ALPHA_INTERPOLATOR;
            }
            animator.setInterpolator(interpolator);
            animator.setDuration(220L);
            if (reverse) {
                this.mBackgroundNormal.setAlpha(1.0f);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (ActivatableNotificationView.this.mDimmed) {
                            ActivatableNotificationView.this.mBackgroundNormal.setVisibility(4);
                        }
                    }
                });
                animator.start();
            } else {
                this.mBackgroundNormal.setAlpha(0.4f);
                animator.start();
            }
            this.mBackgroundNormal.animate().alpha(reverse ? 0.0f : 1.0f).setInterpolator(alphaInterpolator).setDuration(220L);
        }
    }

    public void makeInactive(boolean animate) {
        if (this.mActivated) {
            if (this.mDimmed) {
                if (animate) {
                    startActivateAnimation(true);
                } else {
                    this.mBackgroundNormal.setVisibility(4);
                }
            }
            this.mActivated = false;
        }
        if (this.mOnActivatedListener != null) {
            this.mOnActivatedListener.onActivationReset(this);
        }
        removeCallbacks(this.mTapTimeoutRunnable);
    }

    private boolean isWithinTouchSlop(MotionEvent event) {
        return Math.abs(event.getX() - this.mDownX) < this.mTouchSlop && Math.abs(event.getY() - this.mDownY) < this.mTouchSlop;
    }

    @Override
    public void setDimmed(boolean dimmed, boolean fade) {
        if (this.mDimmed != dimmed) {
            this.mDimmed = dimmed;
            if (fade) {
                fadeDimmedBackground();
            } else {
                updateBackground();
            }
        }
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        if (this.mDark != dark) {
            this.mDark = dark;
            if (!dark && fade) {
                if (this.mActivated) {
                    this.mBackgroundDimmed.setVisibility(0);
                    this.mBackgroundNormal.setVisibility(0);
                } else if (this.mDimmed) {
                    this.mBackgroundDimmed.setVisibility(0);
                    this.mBackgroundNormal.setVisibility(4);
                } else {
                    this.mBackgroundDimmed.setVisibility(4);
                    this.mBackgroundNormal.setVisibility(0);
                }
                fadeInFromDark(delay);
                return;
            }
            updateBackground();
        }
    }

    public void setShowingLegacyBackground(boolean showing) {
        this.mShowingLegacyBackground = showing;
        updateBackgroundTint();
    }

    @Override
    public void setBelowSpeedBump(boolean below) {
        super.setBelowSpeedBump(below);
        if (below != this.mIsBelowSpeedBump) {
            this.mIsBelowSpeedBump = below;
            updateBackgroundTint();
        }
    }

    public void setTintColor(int color) {
        this.mBgTint = color;
        updateBackgroundTint();
    }

    private void updateBackgroundTint() {
        int color = getBackgroundColor();
        int rippleColor = getRippleColor();
        if (color == this.mNormalColor) {
            color = 0;
        }
        this.mBackgroundDimmed.setTint(color);
        this.mBackgroundNormal.setTint(color);
        this.mBackgroundDimmed.setRippleColor(rippleColor);
        this.mBackgroundNormal.setRippleColor(rippleColor);
    }

    private void fadeInFromDark(long delay) {
        final View background = this.mDimmed ? this.mBackgroundDimmed : this.mBackgroundNormal;
        background.setAlpha(0.0f);
        background.setPivotX(this.mBackgroundDimmed.getWidth() / 2.0f);
        background.setPivotY(getActualHeight() / 2.0f);
        background.setScaleX(0.93f);
        background.setScaleY(0.93f);
        background.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(170L).setStartDelay(delay).setInterpolator(this.mLinearOutSlowInInterpolator).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                background.setScaleX(1.0f);
                background.setScaleY(1.0f);
                background.setAlpha(1.0f);
            }
        }).start();
    }

    private void fadeDimmedBackground() {
        this.mBackgroundDimmed.animate().cancel();
        this.mBackgroundNormal.animate().cancel();
        if (this.mDimmed) {
            this.mBackgroundDimmed.setVisibility(0);
        } else {
            this.mBackgroundNormal.setVisibility(0);
        }
        float startAlpha = this.mDimmed ? 1.0f : 0.0f;
        float endAlpha = this.mDimmed ? 0.0f : 1.0f;
        int duration = 220;
        if (this.mBackgroundAnimator != null) {
            startAlpha = ((Float) this.mBackgroundAnimator.getAnimatedValue()).floatValue();
            duration = (int) this.mBackgroundAnimator.getCurrentPlayTime();
            this.mBackgroundAnimator.removeAllListeners();
            this.mBackgroundAnimator.cancel();
            if (duration <= 0) {
                updateBackground();
                return;
            }
        }
        this.mBackgroundNormal.setAlpha(startAlpha);
        this.mBackgroundAnimator = ObjectAnimator.ofFloat(this.mBackgroundNormal, (Property<NotificationBackgroundView, Float>) View.ALPHA, startAlpha, endAlpha);
        this.mBackgroundAnimator.setInterpolator(this.mFastOutSlowInInterpolator);
        this.mBackgroundAnimator.setDuration(duration);
        this.mBackgroundAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (ActivatableNotificationView.this.mDimmed) {
                    ActivatableNotificationView.this.mBackgroundNormal.setVisibility(4);
                } else {
                    ActivatableNotificationView.this.mBackgroundDimmed.setVisibility(4);
                }
                ActivatableNotificationView.this.mBackgroundAnimator = null;
            }
        });
        this.mBackgroundAnimator.start();
    }

    private void updateBackground() {
        cancelFadeAnimations();
        if (this.mDark) {
            this.mBackgroundDimmed.setVisibility(4);
            this.mBackgroundNormal.setVisibility(4);
        } else if (this.mDimmed) {
            this.mBackgroundDimmed.setVisibility(0);
            this.mBackgroundNormal.setVisibility(4);
        } else {
            this.mBackgroundDimmed.setVisibility(4);
            this.mBackgroundNormal.setVisibility(0);
            this.mBackgroundNormal.setAlpha(1.0f);
            removeCallbacks(this.mTapTimeoutRunnable);
        }
    }

    private void cancelFadeAnimations() {
        if (this.mBackgroundAnimator != null) {
            this.mBackgroundAnimator.cancel();
        }
        this.mBackgroundDimmed.animate().cancel();
        this.mBackgroundNormal.animate().cancel();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setPivotX(getWidth() / 2);
    }

    @Override
    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        super.setActualHeight(actualHeight, notifyListeners);
        setPivotY(actualHeight / 2);
        this.mBackgroundNormal.setActualHeight(actualHeight);
        this.mBackgroundDimmed.setActualHeight(actualHeight);
    }

    @Override
    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        this.mBackgroundNormal.setClipTopAmount(clipTopAmount);
        this.mBackgroundDimmed.setClipTopAmount(clipTopAmount);
    }

    @Override
    public void performRemoveAnimation(long duration, float translationDirection, Runnable onFinishedRunnable) {
        enableAppearDrawing(true);
        if (this.mDrawingAppearAnimation) {
            startAppearAnimation(false, translationDirection, 0L, duration, onFinishedRunnable);
        } else if (onFinishedRunnable != null) {
            onFinishedRunnable.run();
        }
    }

    @Override
    public void performAddAnimation(long delay, long duration) {
        enableAppearDrawing(true);
        if (this.mDrawingAppearAnimation) {
            startAppearAnimation(true, -1.0f, delay, duration, null);
        }
    }

    private void startAppearAnimation(boolean isAppearing, float translationDirection, long delay, long duration, final Runnable onFinishedRunnable) {
        float targetValue;
        if (this.mAppearAnimator != null) {
            this.mAppearAnimator.cancel();
        }
        this.mAnimationTranslationY = getActualHeight() * translationDirection;
        if (this.mAppearAnimationFraction == -1.0f) {
            if (isAppearing) {
                this.mAppearAnimationFraction = 0.0f;
                this.mAppearAnimationTranslation = this.mAnimationTranslationY;
            } else {
                this.mAppearAnimationFraction = 1.0f;
                this.mAppearAnimationTranslation = 0.0f;
            }
        }
        if (isAppearing) {
            this.mCurrentAppearInterpolator = this.mSlowOutFastInInterpolator;
            this.mCurrentAlphaInterpolator = this.mLinearOutSlowInInterpolator;
            targetValue = 1.0f;
        } else {
            this.mCurrentAppearInterpolator = this.mFastOutSlowInInterpolator;
            this.mCurrentAlphaInterpolator = this.mSlowOutLinearInInterpolator;
            targetValue = 0.0f;
        }
        this.mAppearAnimator = ValueAnimator.ofFloat(this.mAppearAnimationFraction, targetValue);
        this.mAppearAnimator.setInterpolator(this.mLinearInterpolator);
        this.mAppearAnimator.setDuration((long) (duration * Math.abs(this.mAppearAnimationFraction - targetValue)));
        this.mAppearAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ActivatableNotificationView.this.mAppearAnimationFraction = ((Float) animation.getAnimatedValue()).floatValue();
                ActivatableNotificationView.this.updateAppearAnimationAlpha();
                ActivatableNotificationView.this.updateAppearRect();
                ActivatableNotificationView.this.invalidate();
            }
        });
        if (delay > 0) {
            updateAppearAnimationAlpha();
            updateAppearRect();
            this.mAppearAnimator.setStartDelay(delay);
        }
        this.mAppearAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mWasCancelled;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (onFinishedRunnable != null) {
                    onFinishedRunnable.run();
                }
                if (!this.mWasCancelled) {
                    ActivatableNotificationView.this.mAppearAnimationFraction = -1.0f;
                    ActivatableNotificationView.this.setOutlineRect(null);
                    ActivatableNotificationView.this.enableAppearDrawing(false);
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                this.mWasCancelled = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mWasCancelled = true;
            }
        });
        this.mAppearAnimator.start();
    }

    public void updateAppearRect() {
        float top;
        float bottom;
        float inverseFraction = 1.0f - this.mAppearAnimationFraction;
        float translationFraction = this.mCurrentAppearInterpolator.getInterpolation(inverseFraction);
        float translateYTotalAmount = translationFraction * this.mAnimationTranslationY;
        this.mAppearAnimationTranslation = translateYTotalAmount;
        float widthFraction = (inverseFraction - 0.0f) / 0.8f;
        float left = getWidth() * 0.475f * this.mCurrentAppearInterpolator.getInterpolation(Math.min(1.0f, Math.max(0.0f, widthFraction)));
        float right = getWidth() - left;
        float heightFraction = this.mCurrentAppearInterpolator.getInterpolation(Math.max(0.0f, (inverseFraction - 0.0f) / 1.0f));
        int actualHeight = getActualHeight();
        if (this.mAnimationTranslationY > 0.0f) {
            bottom = (actualHeight - ((this.mAnimationTranslationY * heightFraction) * 0.1f)) - translateYTotalAmount;
            top = bottom * heightFraction;
        } else {
            top = (((actualHeight + this.mAnimationTranslationY) * heightFraction) * 0.1f) - translateYTotalAmount;
            bottom = (actualHeight * (1.0f - heightFraction)) + (top * heightFraction);
        }
        this.mAppearAnimationRect.set(left, top, right, bottom);
        setOutlineRect(left, this.mAppearAnimationTranslation + top, right, this.mAppearAnimationTranslation + bottom);
    }

    public void updateAppearAnimationAlpha() {
        int backgroundColor = getBackgroundColor();
        if (backgroundColor != -1) {
            float contentAlphaProgress = this.mAppearAnimationFraction;
            int sourceColor = Color.argb((int) (255.0f * (1.0f - this.mCurrentAlphaInterpolator.getInterpolation(Math.min(1.0f, contentAlphaProgress / 1.0f)))), Color.red(backgroundColor), Color.green(backgroundColor), Color.blue(backgroundColor));
            this.mAppearAnimationFilter.setColor(sourceColor);
            this.mAppearPaint.setColorFilter(this.mAppearAnimationFilter);
        }
    }

    private int getBackgroundColor() {
        if (this.mBgTint != 0) {
            return this.mBgTint;
        }
        if (this.mShowingLegacyBackground) {
            return this.mLegacyColor;
        }
        if (this.mIsBelowSpeedBump) {
            return this.mLowPriorityColor;
        }
        return this.mNormalColor;
    }

    private int getRippleColor() {
        if (this.mBgTint != 0) {
            return this.mTintedRippleColor;
        }
        if (this.mShowingLegacyBackground) {
            return this.mTintedRippleColor;
        }
        if (this.mIsBelowSpeedBump) {
            return this.mLowPriorityRippleColor;
        }
        return this.mNormalRippleColor;
    }

    public void enableAppearDrawing(boolean enable) {
        if (enable != this.mDrawingAppearAnimation) {
            if (enable) {
                if (getWidth() != 0 && getActualHeight() != 0) {
                    Bitmap bitmap = Bitmap.createBitmap(getWidth(), getActualHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    draw(canvas);
                    this.mAppearPaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                } else {
                    return;
                }
            } else {
                this.mAppearPaint.setShader(null);
            }
            this.mDrawingAppearAnimation = enable;
            invalidate();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!this.mDrawingAppearAnimation) {
            super.dispatchDraw(canvas);
        } else {
            drawAppearRect(canvas);
        }
    }

    private void drawAppearRect(Canvas canvas) {
        canvas.save();
        canvas.translate(0.0f, this.mAppearAnimationTranslation);
        canvas.drawRoundRect(this.mAppearAnimationRect, this.mRoundedRectCornerRadius, this.mRoundedRectCornerRadius, this.mAppearPaint);
        canvas.restore();
    }

    public void setOnActivatedListener(OnActivatedListener onActivatedListener) {
        this.mOnActivatedListener = onActivatedListener;
    }

    public void reset() {
        setTintColor(0);
        setShowingLegacyBackground(false);
        setBelowSpeedBump(false);
    }
}
