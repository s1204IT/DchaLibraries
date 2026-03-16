package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.util.Gusterpolator;
import com.android.camera2.R;

public class ModeTransitionView extends View {
    private static final int ALPHA_FULLY_OPAQUE = 255;
    private static final int ALPHA_FULLY_TRANSPARENT = 0;
    private static final int ALPHA_HALF_TRANSPARENT = 127;
    private static final int FADE_OUT = 4;
    private static final int FADE_OUT_DURATION_MS = 250;
    private static final int ICON_FADE_OUT_DURATION_MS = 850;
    private static final int IDLE = 0;
    private static final int PEEP_HOLE_ANIMATION = 3;
    private static final int PEEP_HOLE_ANIMATION_DURATION_MS = 300;
    private static final int PULL_DOWN_SHADE = 2;
    private static final int PULL_UP_SHADE = 1;
    private static final float SCROLL_DISTANCE_MULTIPLY_FACTOR = 2.0f;
    private static final int SHOW_STATIC_IMAGE = 5;
    private static final Log.Tag TAG = new Log.Tag("ModeTransView");
    private CameraAppUI.AnimationFinishedListener mAnimationFinishedListener;
    private int mAnimationType;
    private Bitmap mBackgroundBitmap;
    private int mBackgroundColor;
    private final Drawable mDefaultDrawable;
    private final GestureDetector mGestureDetector;
    private int mHeight;
    private Drawable mIconDrawable;
    private final Rect mIconRect;
    private int mIconSize;
    private final Paint mMaskPaint;
    private AnimatorSet mPeepHoleAnimator;
    private int mPeepHoleCenterX;
    private int mPeepHoleCenterY;
    private float mRadius;
    private float mScrollDistance;
    private float mScrollTrend;
    private final Paint mShadePaint;
    private final Path mShadePath;
    private int mWidth;

    public ModeTransitionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMaskPaint = new Paint();
        this.mIconRect = new Rect();
        this.mDefaultDrawable = new ColorDrawable();
        this.mWidth = 0;
        this.mHeight = 0;
        this.mPeepHoleCenterX = 0;
        this.mPeepHoleCenterY = 0;
        this.mRadius = 0.0f;
        this.mAnimationType = 3;
        this.mScrollDistance = 0.0f;
        this.mShadePath = new Path();
        this.mShadePaint = new Paint();
        this.mMaskPaint.setAlpha(0);
        this.mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.mBackgroundColor = getResources().getColor(R.color.video_mode_color);
        this.mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent ev) {
                ModeTransitionView.this.setScrollDistance(0.0f);
                ModeTransitionView.this.mScrollTrend = 0.0f;
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                ModeTransitionView.this.setScrollDistance(ModeTransitionView.this.getScrollDistance() + (ModeTransitionView.SCROLL_DISTANCE_MULTIPLY_FACTOR * distanceY));
                ModeTransitionView.this.mScrollTrend = (0.3f * ModeTransitionView.this.mScrollTrend) + (0.7f * distanceY);
                return false;
            }
        });
        this.mIconSize = getResources().getDimensionPixelSize(R.dimen.mode_transition_view_icon_size);
        setIconDrawable(this.mDefaultDrawable);
    }

    private void updateShade() {
        float shadeHeight;
        if (this.mAnimationType == 1 || this.mAnimationType == 2) {
            this.mShadePath.reset();
            if (this.mAnimationType == 1) {
                this.mShadePath.addRect(0.0f, this.mHeight - getScrollDistance(), this.mWidth, this.mHeight, Path.Direction.CW);
                shadeHeight = getScrollDistance();
            } else {
                this.mShadePath.addRect(0.0f, 0.0f, this.mWidth, -getScrollDistance(), Path.Direction.CW);
                shadeHeight = getScrollDistance() * (-1.0f);
            }
            if (this.mIconDrawable != null) {
                if (shadeHeight < this.mHeight / 2 || this.mHeight == 0) {
                    this.mIconDrawable.setAlpha(0);
                } else {
                    int alpha = ((((int) shadeHeight) - (this.mHeight / 2)) * 255) / (this.mHeight / 2);
                    this.mIconDrawable.setAlpha(alpha);
                }
            }
            invalidate();
        }
    }

    public void setScrollDistance(float scrollDistance) {
        if (this.mAnimationType == 1) {
            scrollDistance = Math.max(Math.min(scrollDistance, this.mHeight), 0.0f);
        } else if (this.mAnimationType == 2) {
            scrollDistance = Math.max(Math.min(scrollDistance, 0.0f), -this.mHeight);
        }
        this.mScrollDistance = scrollDistance;
        updateShade();
    }

    public float getScrollDistance() {
        return this.mScrollDistance;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mAnimationType == 3) {
            canvas.drawColor(this.mBackgroundColor);
            if (this.mPeepHoleAnimator != null) {
                canvas.drawCircle(this.mPeepHoleCenterX, this.mPeepHoleCenterY, this.mRadius, this.mMaskPaint);
            }
        } else if (this.mAnimationType == 1 || this.mAnimationType == 2) {
            canvas.drawPath(this.mShadePath, this.mShadePaint);
        } else if (this.mAnimationType == 0 || this.mAnimationType == 4) {
            canvas.drawColor(this.mBackgroundColor);
        } else if (this.mAnimationType == 5) {
            canvas.drawBitmap(this.mBackgroundBitmap, 0.0f, 0.0f, (Paint) null);
            super.onDraw(canvas);
            return;
        }
        super.onDraw(canvas);
        this.mIconDrawable.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mWidth = right - left;
        this.mHeight = bottom - top;
        this.mIconRect.set((this.mWidth / 2) - (this.mIconSize / 2), (this.mHeight / 2) - (this.mIconSize / 2), (this.mWidth / 2) + (this.mIconSize / 2), (this.mHeight / 2) + (this.mIconSize / 2));
        this.mIconDrawable.setBounds(this.mIconRect);
    }

    public void startPeepHoleAnimation() {
        float x = this.mWidth / 2;
        float y = this.mHeight / 2;
        startPeepHoleAnimation(x, y);
    }

    private void startPeepHoleAnimation(float x, float y) {
        if (this.mPeepHoleAnimator == null || !this.mPeepHoleAnimator.isRunning()) {
            this.mAnimationType = 3;
            this.mPeepHoleCenterX = (int) x;
            this.mPeepHoleCenterY = (int) y;
            int horizontalDistanceToFarEdge = Math.max(this.mPeepHoleCenterX, this.mWidth - this.mPeepHoleCenterX);
            int verticalDistanceToFarEdge = Math.max(this.mPeepHoleCenterY, this.mHeight - this.mPeepHoleCenterY);
            int endRadius = (int) Math.sqrt((horizontalDistanceToFarEdge * horizontalDistanceToFarEdge) + (verticalDistanceToFarEdge * verticalDistanceToFarEdge));
            final ValueAnimator radiusAnimator = ValueAnimator.ofFloat(0.0f, endRadius);
            radiusAnimator.setDuration(300L);
            final ValueAnimator iconScaleAnimator = ValueAnimator.ofFloat(1.0f, 0.5f);
            iconScaleAnimator.setDuration(850L);
            final ValueAnimator iconAlphaAnimator = ValueAnimator.ofInt(127, 0);
            iconAlphaAnimator.setDuration(850L);
            this.mPeepHoleAnimator = new AnimatorSet();
            this.mPeepHoleAnimator.playTogether(radiusAnimator, iconAlphaAnimator, iconScaleAnimator);
            this.mPeepHoleAnimator.setInterpolator(Gusterpolator.INSTANCE);
            iconAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ModeTransitionView.this.mRadius = ((Float) radiusAnimator.getAnimatedValue()).floatValue();
                    ModeTransitionView.this.mIconDrawable.setAlpha(((Integer) iconAlphaAnimator.getAnimatedValue()).intValue());
                    float scale = ((Float) iconScaleAnimator.getAnimatedValue()).floatValue();
                    int size = (int) (ModeTransitionView.this.mIconSize * scale);
                    ModeTransitionView.this.mIconDrawable.setBounds(ModeTransitionView.this.mPeepHoleCenterX - (size / 2), ModeTransitionView.this.mPeepHoleCenterY - (size / 2), ModeTransitionView.this.mPeepHoleCenterX + (size / 2), ModeTransitionView.this.mPeepHoleCenterY + (size / 2));
                    ModeTransitionView.this.invalidate();
                }
            });
            this.mPeepHoleAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ModeTransitionView.this.setLayerType(2, null);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ModeTransitionView.this.setLayerType(0, null);
                    ModeTransitionView.this.mPeepHoleAnimator = null;
                    ModeTransitionView.this.mRadius = 0.0f;
                    ModeTransitionView.this.mIconDrawable.setAlpha(255);
                    ModeTransitionView.this.mIconDrawable.setBounds(ModeTransitionView.this.mIconRect);
                    ModeTransitionView.this.setVisibility(8);
                    ModeTransitionView.this.mAnimationType = 0;
                    if (ModeTransitionView.this.mAnimationFinishedListener != null) {
                        ModeTransitionView.this.mAnimationFinishedListener.onAnimationFinished(true);
                        ModeTransitionView.this.mAnimationFinishedListener = null;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            this.mPeepHoleAnimator.start();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean touchHandled = this.mGestureDetector.onTouchEvent(ev);
        if (ev.getActionMasked() == 1) {
            snap();
        }
        return touchHandled;
    }

    private void snap() {
        if (this.mScrollTrend >= 0.0f && this.mAnimationType == 1) {
            snapShadeTo(this.mHeight, 255);
            return;
        }
        if (this.mScrollTrend <= 0.0f && this.mAnimationType == 2) {
            snapShadeTo(-this.mHeight, 255);
            return;
        }
        if (this.mScrollTrend < 0.0f && this.mAnimationType == 1) {
            snapShadeTo(0, 0, false);
        } else if (this.mScrollTrend > 0.0f && this.mAnimationType == 2) {
            snapShadeTo(0, 0, false);
        }
    }

    private void snapShadeTo(int scrollDistance, int alpha) {
        snapShadeTo(scrollDistance, alpha, true);
    }

    private void snapShadeTo(final int scrollDistance, final int alpha, final boolean snapToFullScreen) {
        if (this.mAnimationType == 1 || this.mAnimationType == 2) {
            ObjectAnimator scrollAnimator = ObjectAnimator.ofFloat(this, "scrollDistance", scrollDistance);
            scrollAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ModeTransitionView.this.setScrollDistance(scrollDistance);
                    ModeTransitionView.this.mIconDrawable.setAlpha(alpha);
                    ModeTransitionView.this.mAnimationType = 0;
                    if (!snapToFullScreen) {
                        ModeTransitionView.this.setVisibility(8);
                    }
                    if (ModeTransitionView.this.mAnimationFinishedListener != null) {
                        ModeTransitionView.this.mAnimationFinishedListener.onAnimationFinished(snapToFullScreen);
                        ModeTransitionView.this.mAnimationFinishedListener = null;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            scrollAnimator.setInterpolator(Gusterpolator.INSTANCE);
            scrollAnimator.start();
        }
    }

    public void prepareToPullUpShade(int shadeColorId, int iconId, CameraAppUI.AnimationFinishedListener listener) {
        prepareShadeAnimation(1, shadeColorId, iconId, listener);
    }

    public void prepareToPullDownShade(int shadeColorId, int modeIconResourceId, CameraAppUI.AnimationFinishedListener listener) {
        prepareShadeAnimation(2, shadeColorId, modeIconResourceId, listener);
    }

    private void prepareShadeAnimation(int animationType, int shadeColorId, int iconResId, CameraAppUI.AnimationFinishedListener listener) {
        this.mAnimationFinishedListener = listener;
        if (this.mPeepHoleAnimator != null && this.mPeepHoleAnimator.isRunning()) {
            this.mPeepHoleAnimator.end();
        }
        this.mAnimationType = animationType;
        resetShade(shadeColorId, iconResId);
    }

    private void resetShade(int shadeColorId, int modeIconResourceId) {
        int shadeColor = getResources().getColor(shadeColorId);
        this.mBackgroundColor = shadeColor;
        this.mShadePaint.setColor(shadeColor);
        setScrollDistance(0.0f);
        updateIconDrawableByResourceId(modeIconResourceId);
        this.mIconDrawable.setAlpha(0);
        setVisibility(0);
    }

    private void updateIconDrawableByResourceId(int modeIconResourceId) {
        Drawable iconDrawable = getResources().getDrawable(modeIconResourceId);
        if (iconDrawable == null) {
            Log.e(TAG, "Invalid resource id for icon drawable. Setting icon drawable to null.");
            setIconDrawable(null);
        } else {
            setIconDrawable(iconDrawable.mutate());
        }
    }

    private void setIconDrawable(Drawable iconDrawable) {
        if (iconDrawable == null) {
            this.mIconDrawable = this.mDefaultDrawable;
        } else {
            this.mIconDrawable = iconDrawable;
        }
    }

    public void setupModeCover(int colorId, int modeIconResourceId) {
        this.mBackgroundBitmap = null;
        if (this.mPeepHoleAnimator != null && this.mPeepHoleAnimator.isRunning()) {
            this.mPeepHoleAnimator.cancel();
        }
        this.mAnimationType = 0;
        this.mBackgroundColor = getResources().getColor(colorId);
        updateIconDrawableByResourceId(modeIconResourceId);
        this.mIconDrawable.setAlpha(255);
        setVisibility(0);
    }

    public void hideModeCover(final CameraAppUI.AnimationFinishedListener animationFinishedListener) {
        if (this.mAnimationType != 0) {
            if (animationFinishedListener != null) {
                animationFinishedListener.onAnimationFinished(false);
            }
        } else {
            this.mAnimationType = 4;
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.0f);
            alphaAnimator.setDuration(250L);
            alphaAnimator.setInterpolator(null);
            alphaAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ModeTransitionView.this.setVisibility(8);
                    ModeTransitionView.this.setAlpha(1.0f);
                    if (animationFinishedListener != null) {
                        animationFinishedListener.onAnimationFinished(true);
                        ModeTransitionView.this.mAnimationType = 0;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            alphaAnimator.start();
        }
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        int alphaScaled = (int) (255.0f * getAlpha());
        this.mBackgroundColor = (this.mBackgroundColor & ViewCompat.MEASURED_SIZE_MASK) | (alphaScaled << 24);
        this.mIconDrawable.setAlpha(alphaScaled);
    }

    public void setupModeCover(Bitmap screenShot) {
        this.mBackgroundBitmap = screenShot;
        setVisibility(0);
        this.mAnimationType = 5;
    }

    public void hideImageCover() {
        this.mBackgroundBitmap = null;
        setVisibility(8);
        this.mAnimationType = 0;
    }
}
