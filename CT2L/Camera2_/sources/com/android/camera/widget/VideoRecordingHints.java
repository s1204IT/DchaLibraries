package com.android.camera.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import java.lang.ref.WeakReference;

public class VideoRecordingHints extends View {
    private static final int FADE_OUT_DURATION_MS = 600;
    private static final float INITIAL_ROTATION = 0.0f;
    private static final int PORTRAIT_ROTATE_DELAY_MS = 1000;
    private static final float ROTATION_DEGREES = 180.0f;
    private static final int ROTATION_DURATION_MS = 1000;
    private static final int UNSET = -1;
    private final ObjectAnimator mAlphaAnimator;
    private int mCenterX;
    private int mCenterY;
    private final boolean mIsDefaultToPortrait;
    private boolean mIsInLandscape;
    private int mLastOrientation;
    private final Drawable mPhoneGraphic;
    private final int mPhoneGraphicHalfHeight;
    private final int mPhoneGraphicHalfWidth;
    private final Drawable mRotateArrows;
    private final int mRotateArrowsHalfSize;
    private float mRotation;
    private final ValueAnimator mRotationAnimation;

    private static class RotationAnimatorListener implements Animator.AnimatorListener {
        private boolean mCanceled = false;
        private final WeakReference<VideoRecordingHints> mHints;

        public RotationAnimatorListener(VideoRecordingHints hint) {
            this.mHints = new WeakReference<>(hint);
        }

        @Override
        public void onAnimationStart(Animator animation) {
            this.mCanceled = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            VideoRecordingHints hint = this.mHints.get();
            if (hint != null) {
                hint.mRotation = ((int) hint.mRotation) % 360;
                if (!this.mCanceled) {
                    hint.post(new Runnable() {
                        @Override
                        public void run() {
                            VideoRecordingHints hint2 = (VideoRecordingHints) RotationAnimatorListener.this.mHints.get();
                            if (hint2 != null) {
                                hint2.continueRotationAnimation();
                            }
                        }
                    });
                }
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            this.mCanceled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }

    private static class AlphaAnimatorListener implements Animator.AnimatorListener {
        private final WeakReference<VideoRecordingHints> mHints;

        AlphaAnimatorListener(VideoRecordingHints hint) {
            this.mHints = new WeakReference<>(hint);
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            VideoRecordingHints hint = this.mHints.get();
            if (hint != null) {
                hint.invalidate();
                hint.setAlpha(1.0f);
                hint.mRotation = 0.0f;
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }

    public VideoRecordingHints(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRotation = 0.0f;
        this.mIsInLandscape = false;
        this.mCenterX = -1;
        this.mCenterY = -1;
        this.mLastOrientation = -1;
        this.mRotateArrows = getResources().getDrawable(R.drawable.rotate_arrows);
        this.mPhoneGraphic = getResources().getDrawable(R.drawable.ic_phone_graphic);
        this.mRotateArrowsHalfSize = getResources().getDimensionPixelSize(R.dimen.video_hint_arrow_size) / 2;
        this.mPhoneGraphicHalfWidth = getResources().getDimensionPixelSize(R.dimen.video_hint_phone_graphic_width) / 2;
        this.mPhoneGraphicHalfHeight = getResources().getDimensionPixelSize(R.dimen.video_hint_phone_graphic_height) / 2;
        this.mRotationAnimation = ValueAnimator.ofFloat(this.mRotation, this.mRotation + ROTATION_DEGREES);
        this.mRotationAnimation.setDuration(1000L);
        this.mRotationAnimation.setStartDelay(1000L);
        this.mRotationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                VideoRecordingHints.this.mRotation = ((Float) animation.getAnimatedValue()).floatValue();
                VideoRecordingHints.this.invalidate();
            }
        });
        this.mRotationAnimation.addListener(new RotationAnimatorListener(this));
        this.mAlphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.0f);
        this.mAlphaAnimator.setDuration(600L);
        this.mAlphaAnimator.addListener(new AlphaAnimatorListener(this));
        this.mIsDefaultToPortrait = CameraUtil.isDefaultToPortrait(context);
    }

    private void continueRotationAnimation() {
        if (!this.mRotationAnimation.isRunning()) {
            this.mRotationAnimation.setFloatValues(this.mRotation, this.mRotation + ROTATION_DEGREES);
            this.mRotationAnimation.start();
        }
    }

    @Override
    public void onVisibilityChanged(View v, int visibility) {
        super.onVisibilityChanged(v, visibility);
        if (getVisibility() == 0 && !isInLandscape()) {
            continueRotationAnimation();
        } else if (getVisibility() != 0) {
            this.mRotationAnimation.cancel();
            this.mRotation = 0.0f;
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mCenterX = (right - left) / 2;
        this.mCenterY = (bottom - top) / 2;
        this.mRotateArrows.setBounds(this.mCenterX - this.mRotateArrowsHalfSize, this.mCenterY - this.mRotateArrowsHalfSize, this.mCenterX + this.mRotateArrowsHalfSize, this.mCenterY + this.mRotateArrowsHalfSize);
        this.mPhoneGraphic.setBounds(this.mCenterX - this.mPhoneGraphicHalfWidth, this.mCenterY - this.mPhoneGraphicHalfHeight, this.mCenterX + this.mPhoneGraphicHalfWidth, this.mCenterY + this.mPhoneGraphicHalfHeight);
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (!this.mIsInLandscape || this.mAlphaAnimator.isRunning()) {
            canvas.save();
            canvas.rotate(-this.mRotation, this.mCenterX, this.mCenterY);
            this.mRotateArrows.draw(canvas);
            canvas.restore();
            if (this.mIsInLandscape) {
                canvas.save();
                canvas.rotate(90.0f, this.mCenterX, this.mCenterY);
                this.mPhoneGraphic.draw(canvas);
                canvas.restore();
                return;
            }
            this.mPhoneGraphic.draw(canvas);
        }
    }

    public void onOrientationChanged(int orientation) {
        if (this.mLastOrientation != orientation) {
            this.mLastOrientation = orientation;
            if (this.mLastOrientation != -1) {
                this.mIsInLandscape = isInLandscape();
                if (getVisibility() == 0) {
                    if (this.mIsInLandscape) {
                        this.mRotationAnimation.cancel();
                        if (!this.mAlphaAnimator.isRunning()) {
                            this.mAlphaAnimator.start();
                            return;
                        }
                        return;
                    }
                    continueRotationAnimation();
                }
            }
        }
    }

    private boolean isInLandscape() {
        return (this.mLastOrientation % 180 == 90 && this.mIsDefaultToPortrait) || (this.mLastOrientation % 180 == 0 && !this.mIsDefaultToPortrait);
    }
}
