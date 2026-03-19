package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.deskclock.uidata.UiDataModel;

public final class MoveScreensaverRunnable implements Runnable {
    private static final long FADE_TIME = 3000;
    private Animator mActiveAnimator;
    private final View mContentView;
    private final View mSaverView;
    private final Interpolator mAcceleration = new AccelerateInterpolator();
    private final Interpolator mDeceleration = new DecelerateInterpolator();

    public MoveScreensaverRunnable(View view, View view2) {
        this.mContentView = view;
        this.mSaverView = view2;
    }

    public void start() {
        stop();
        this.mSaverView.setAlpha(0.0f);
        run();
        UiDataModel.getUiDataModel().addMinuteCallback(this, -3000L);
    }

    public void stop() {
        UiDataModel.getUiDataModel().removePeriodicCallback(this);
        if (this.mActiveAnimator != null) {
            this.mActiveAnimator.end();
            this.mActiveAnimator = null;
        }
    }

    @Override
    public void run() {
        Utils.enforceMainLooper();
        if (this.mSaverView.getAlpha() == 0.0f) {
            int iMin = Math.min(this.mContentView.getWidth(), this.mContentView.getHeight());
            float randomPoint = getRandomPoint(iMin - this.mSaverView.getWidth());
            float randomPoint2 = getRandomPoint(iMin - this.mSaverView.getHeight());
            this.mSaverView.setX(randomPoint);
            this.mSaverView.setY(randomPoint2);
            this.mActiveAnimator = AnimatorUtils.getAlphaAnimator(this.mSaverView, 0.0f, 1.0f);
            this.mActiveAnimator.setDuration(FADE_TIME);
            this.mActiveAnimator.setInterpolator(this.mDeceleration);
            this.mActiveAnimator.start();
            return;
        }
        final float randomPoint3 = getRandomPoint(this.mContentView.getWidth() - this.mSaverView.getWidth());
        final float randomPoint4 = getRandomPoint(this.mContentView.getHeight() - this.mSaverView.getHeight());
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(FADE_TIME);
        animatorSet.setInterpolator(this.mAcceleration);
        animatorSet.play(AnimatorUtils.getAlphaAnimator(this.mSaverView, 1.0f, 0.0f)).with(AnimatorUtils.getScaleAnimator(this.mSaverView, 1.0f, 0.85f));
        AnimatorSet animatorSet2 = new AnimatorSet();
        animatorSet2.setDuration(FADE_TIME);
        animatorSet2.setInterpolator(this.mDeceleration);
        animatorSet2.play(AnimatorUtils.getAlphaAnimator(this.mSaverView, 0.0f, 1.0f)).with(AnimatorUtils.getScaleAnimator(this.mSaverView, 0.85f, 1.0f));
        animatorSet2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                MoveScreensaverRunnable.this.mSaverView.setX(randomPoint3);
                MoveScreensaverRunnable.this.mSaverView.setY(randomPoint4);
            }
        });
        AnimatorSet animatorSet3 = new AnimatorSet();
        animatorSet3.play(animatorSet2).after(animatorSet);
        this.mActiveAnimator = animatorSet3;
        this.mActiveAnimator.start();
    }

    private static float getRandomPoint(float f) {
        return (int) (Math.random() * ((double) f));
    }
}
