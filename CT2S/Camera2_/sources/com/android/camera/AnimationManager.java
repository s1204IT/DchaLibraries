package com.android.camera;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

public class AnimationManager {
    public static final float FLASH_ALPHA_END = 0.0f;
    public static final float FLASH_ALPHA_START = 0.3f;
    public static final int FLASH_DURATION = 300;
    public static final int HOLD_DURATION = 2500;
    public static final int SHRINK_DURATION = 400;
    public static final int SLIDE_DURATION = 1100;
    private AnimatorSet mCaptureAnimator;
    private ObjectAnimator mFlashAnim;

    public void startCaptureAnimation(final View view) {
        if (this.mCaptureAnimator != null && this.mCaptureAnimator.isStarted()) {
            this.mCaptureAnimator.cancel();
        }
        View parentView = (View) view.getParent();
        float slideDistance = parentView.getWidth() - view.getLeft();
        float scaleX = parentView.getWidth() / view.getWidth();
        float scaleY = parentView.getHeight() / view.getHeight();
        float scale = scaleX > scaleY ? scaleX : scaleY;
        int centerX = view.getLeft() + (view.getWidth() / 2);
        int centerY = view.getTop() + (view.getHeight() / 2);
        ObjectAnimator slide = ObjectAnimator.ofFloat(view, "translationX", 0.0f, slideDistance).setDuration(1100L);
        slide.setStartDelay(2900L);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(view, "translationY", (parentView.getHeight() / 2) - centerY, 0.0f).setDuration(400L);
        translateY.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setClickable(true);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mCaptureAnimator = new AnimatorSet();
        this.mCaptureAnimator.playTogether(ObjectAnimator.ofFloat(view, "scaleX", scale, 1.0f).setDuration(400L), ObjectAnimator.ofFloat(view, "scaleY", scale, 1.0f).setDuration(400L), ObjectAnimator.ofFloat(view, "translationX", (parentView.getWidth() / 2) - centerX, 0.0f).setDuration(400L), translateY, slide);
        this.mCaptureAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                view.setClickable(false);
                view.setVisibility(0);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setScaleX(1.0f);
                view.setScaleX(1.0f);
                view.setTranslationX(0.0f);
                view.setTranslationY(0.0f);
                view.setVisibility(4);
                AnimationManager.this.mCaptureAnimator.removeAllListeners();
                AnimationManager.this.mCaptureAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                view.setVisibility(4);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mCaptureAnimator.start();
    }

    public void startFlashAnimation(final View flashOverlay) {
        if (this.mFlashAnim != null && this.mFlashAnim.isRunning()) {
            this.mFlashAnim.cancel();
        }
        this.mFlashAnim = ObjectAnimator.ofFloat(flashOverlay, "alpha", 0.3f, 0.0f);
        this.mFlashAnim.setDuration(300L);
        this.mFlashAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                flashOverlay.setVisibility(0);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                flashOverlay.setAlpha(0.0f);
                flashOverlay.setVisibility(8);
                AnimationManager.this.mFlashAnim.removeAllListeners();
                AnimationManager.this.mFlashAnim = null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mFlashAnim.start();
    }

    public void cancelAnimations() {
        if (this.mFlashAnim != null && this.mFlashAnim.isRunning()) {
            this.mFlashAnim.cancel();
        }
        if (this.mCaptureAnimator != null && this.mCaptureAnimator.isStarted()) {
            this.mCaptureAnimator.cancel();
        }
    }
}
