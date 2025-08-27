package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.TransitionDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;

/* loaded from: classes.dex */
public class QSDetailClipper {
    private Animator mAnimator;
    private final TransitionDrawable mBackground;
    private final View mDetail;
    private final Runnable mReverseBackground = new Runnable() { // from class: com.android.systemui.qs.QSDetailClipper.1
        @Override // java.lang.Runnable
        public void run() {
            if (QSDetailClipper.this.mAnimator != null) {
                QSDetailClipper.this.mBackground.reverseTransition((int) (QSDetailClipper.this.mAnimator.getDuration() * 0.35d));
            }
        }
    };
    private final AnimatorListenerAdapter mVisibleOnStart = new AnimatorListenerAdapter() { // from class: com.android.systemui.qs.QSDetailClipper.2
        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
            QSDetailClipper.this.mDetail.setVisibility(0);
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            QSDetailClipper.this.mAnimator = null;
        }
    };
    private final AnimatorListenerAdapter mGoneOnEnd = new AnimatorListenerAdapter() { // from class: com.android.systemui.qs.QSDetailClipper.3
        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            QSDetailClipper.this.mDetail.setVisibility(8);
            QSDetailClipper.this.mBackground.resetTransition();
            QSDetailClipper.this.mAnimator = null;
        }
    };

    public QSDetailClipper(View view) {
        this.mDetail = view;
        this.mBackground = (TransitionDrawable) view.getBackground();
    }

    public void animateCircularClip(int i, int i2, boolean z, Animator.AnimatorListener animatorListener) {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        if (this.mDetail == null || !this.mDetail.isAttachedToWindow()) {
            Log.w("QSDetailClipper", "mDetail view is null or detached!");
            return;
        }
        int width = this.mDetail.getWidth() - i;
        int height = this.mDetail.getHeight() - i2;
        int iMin = 0;
        if (i < 0 || width < 0 || i2 < 0 || height < 0) {
            iMin = Math.min(Math.min(Math.min(Math.abs(i), Math.abs(i2)), Math.abs(width)), Math.abs(height));
        }
        int i3 = i * i;
        int i4 = i2 * i2;
        int i5 = width * width;
        int i6 = height * height;
        int iMax = (int) Math.max((int) Math.max((int) Math.max((int) Math.ceil(Math.sqrt(i3 + i4)), Math.ceil(Math.sqrt(i4 + i5))), Math.ceil(Math.sqrt(i5 + i6))), Math.ceil(Math.sqrt(i3 + i6)));
        if (z) {
            this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, i, i2, iMin, iMax);
        } else {
            this.mAnimator = ViewAnimationUtils.createCircularReveal(this.mDetail, i, i2, iMax, iMin);
        }
        this.mAnimator.setDuration((long) (this.mAnimator.getDuration() * 1.5d));
        if (animatorListener != null) {
            this.mAnimator.addListener(animatorListener);
        }
        if (z) {
            this.mBackground.startTransition((int) (this.mAnimator.getDuration() * 0.6d));
            this.mAnimator.addListener(this.mVisibleOnStart);
        } else {
            this.mDetail.postDelayed(this.mReverseBackground, (long) (this.mAnimator.getDuration() * 0.65d));
            this.mAnimator.addListener(this.mGoneOnEnd);
        }
        this.mAnimator.start();
    }

    public void showBackground() {
        this.mBackground.showSecondLayer();
    }
}
