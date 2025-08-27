package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;

/* loaded from: classes.dex */
public class AlphaUpdateListener extends AnimationSuccessListener implements ValueAnimator.AnimatorUpdateListener {
    private static final float ALPHA_CUTOFF_THRESHOLD = 0.01f;
    private View mView;

    public AlphaUpdateListener(View view) {
        this.mView = view;
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        updateVisibility(this.mView);
    }

    @Override // com.android.launcher3.anim.AnimationSuccessListener
    public void onAnimationSuccess(Animator animator) {
        updateVisibility(this.mView);
    }

    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
    public void onAnimationStart(Animator animator) {
        this.mView.setVisibility(0);
    }

    public static void updateVisibility(View view) {
        if (view.getAlpha() < ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != 4) {
            view.setVisibility(4);
        } else if (view.getAlpha() > ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != 0) {
            view.setVisibility(0);
        }
    }
}
