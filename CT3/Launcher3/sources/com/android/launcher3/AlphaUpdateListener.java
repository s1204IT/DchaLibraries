package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;

class AlphaUpdateListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
    private boolean mAccessibilityEnabled;
    private View mView;

    public AlphaUpdateListener(View v, boolean accessibilityEnabled) {
        this.mView = v;
        this.mAccessibilityEnabled = accessibilityEnabled;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator arg0) {
        updateVisibility(this.mView, this.mAccessibilityEnabled);
    }

    public static void updateVisibility(View view, boolean accessibilityEnabled) {
        int invisibleState = accessibilityEnabled ? 8 : 4;
        if (view.getAlpha() < 0.01f && view.getVisibility() != invisibleState) {
            view.setVisibility(invisibleState);
        } else {
            if (view.getAlpha() <= 0.01f || view.getVisibility() == 0) {
                return;
            }
            view.setVisibility(0);
        }
    }

    @Override
    public void onAnimationEnd(Animator arg0) {
        updateVisibility(this.mView, this.mAccessibilityEnabled);
    }

    @Override
    public void onAnimationStart(Animator arg0) {
        this.mView.setVisibility(0);
    }
}
