package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

public abstract class AnimationSuccessListener extends AnimatorListenerAdapter {
    protected boolean mCancelled = false;

    public abstract void onAnimationSuccess(Animator animator);

    @Override
    public void onAnimationCancel(Animator animator) {
        this.mCancelled = true;
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (!this.mCancelled) {
            onAnimationSuccess(animator);
        }
    }
}
