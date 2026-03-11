package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.ViewAnimationUtils;

public class QSDetailClipper {
    private Animator mAnimator;
    private final TransitionDrawable mBackground;
    private final View mDetail;
    private final Runnable mReverseBackground = new Runnable() {
        @Override
        public void run() {
            if (QSDetailClipper.this.mAnimator != null) {
                QSDetailClipper.this.mBackground.reverseTransition((int) (QSDetailClipper.this.mAnimator.getDuration() * 0.35d));
            }
        }
    };
    private final AnimatorListenerAdapter mVisibleOnStart = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            QSDetailClipper.this.mDetail.setVisibility(0);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            QSDetailClipper.this.mAnimator = null;
        }
    };
    private final AnimatorListenerAdapter mGoneOnEnd = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            QSDetailClipper.this.mDetail.setVisibility(8);
            QSDetailClipper.this.mBackground.resetTransition();
            QSDetailClipper.this.mAnimator = null;
        }
    };

    public QSDetailClipper(View detail) {
        this.mDetail = detail;
        this.mBackground = (TransitionDrawable) detail.getBackground();
    }

    public void animateCircularClip(int x, int y, boolean in, Animator.AnimatorListener listener) {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        int w = this.mDetail.getWidth() - x;
        int h = this.mDetail.getHeight() - y;
        int r = (int) Math.ceil(Math.sqrt((x * x) + (y * y)));
        int r2 = (int) Math.max((int) Math.max((int) Math.max(r, Math.ceil(Math.sqrt((w * w) + (y * y)))), Math.ceil(Math.sqrt((w * w) + (h * h)))), Math.ceil(Math.sqrt((x * x) + (h * h))));
        if (in) {
            this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, x, y, 0.0f, r2);
        } else {
            this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, x, y, r2, 0.0f);
        }
        this.mAnimator.setDuration((long) (this.mAnimator.getDuration() * 1.5d));
        if (listener != null) {
            this.mAnimator.addListener(listener);
        }
        if (in) {
            this.mBackground.startTransition((int) (this.mAnimator.getDuration() * 0.6d));
            this.mAnimator.addListener(this.mVisibleOnStart);
        } else {
            this.mDetail.postDelayed(this.mReverseBackground, (long) (this.mAnimator.getDuration() * 0.65d));
            this.mAnimator.addListener(this.mGoneOnEnd);
        }
        this.mAnimator.start();
    }
}
