package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

/* loaded from: classes.dex */
public abstract class AnimationSuccessListener extends AnimatorListenerAdapter {
    protected boolean mCancelled = false;

    public abstract void onAnimationSuccess(Animator animator);

    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
    public void onAnimationCancel(Animator animator) {
        this.mCancelled = true;
    }

    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
    public void onAnimationEnd(Animator animator) {
        if (!this.mCancelled) {
            onAnimationSuccess(animator);
        }
    }
}
