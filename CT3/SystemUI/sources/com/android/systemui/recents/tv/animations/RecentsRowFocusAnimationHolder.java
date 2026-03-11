package com.android.systemui.recents.tv.animations;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.view.View;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class RecentsRowFocusAnimationHolder {
    private AnimatorSet mFocusGainAnimatorSet;
    private AnimatorSet mFocusLossAnimatorSet;
    private final View mTitleView;
    private final View mView;

    public RecentsRowFocusAnimationHolder(View view, View titleView) {
        this.mView = view;
        this.mTitleView = titleView;
        Resources res = view.getResources();
        int duration = res.getInteger(R.integer.recents_tv_pip_focus_anim_duration);
        float dimAlpha = res.getFloat(R.dimen.recents_recents_row_dim_alpha);
        this.mFocusGainAnimatorSet = new AnimatorSet();
        this.mFocusGainAnimatorSet.playTogether(ObjectAnimator.ofFloat(this.mView, "alpha", 1.0f), ObjectAnimator.ofFloat(this.mTitleView, "alpha", 1.0f));
        this.mFocusGainAnimatorSet.setDuration(duration);
        this.mFocusGainAnimatorSet.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        this.mFocusLossAnimatorSet = new AnimatorSet();
        this.mFocusLossAnimatorSet.playTogether(ObjectAnimator.ofFloat(this.mView, "alpha", 1.0f, dimAlpha), ObjectAnimator.ofFloat(this.mTitleView, "alpha", 0.0f));
        this.mFocusLossAnimatorSet.setDuration(duration);
        this.mFocusLossAnimatorSet.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
    }

    public void startFocusGainAnimation() {
        cancelAnimator(this.mFocusLossAnimatorSet);
        this.mFocusGainAnimatorSet.start();
    }

    public void startFocusLossAnimation() {
        cancelAnimator(this.mFocusGainAnimatorSet);
        this.mFocusLossAnimatorSet.start();
    }

    public void reset() {
        cancelAnimator(this.mFocusLossAnimatorSet);
        cancelAnimator(this.mFocusGainAnimatorSet);
        this.mView.setAlpha(1.0f);
        this.mTitleView.setAlpha(1.0f);
    }

    private static void cancelAnimator(Animator animator) {
        if (!animator.isStarted()) {
            return;
        }
        animator.cancel();
    }
}
