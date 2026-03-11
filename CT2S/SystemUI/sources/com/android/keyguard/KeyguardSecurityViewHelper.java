package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.view.View;

public class KeyguardSecurityViewHelper {
    public static void showBouncer(SecurityMessageDisplay securityMessageDisplay, final View ecaView, Drawable bouncerFrame, int duration) {
        if (securityMessageDisplay != null) {
            securityMessageDisplay.showBouncer(duration);
        }
        if (ecaView != null) {
            if (duration > 0) {
                Animator anim = ObjectAnimator.ofFloat(ecaView, "alpha", 0.0f);
                anim.setDuration(duration);
                anim.addListener(new AnimatorListenerAdapter() {
                    private boolean mCanceled;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        this.mCanceled = true;
                        ecaView.setAlpha(1.0f);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ecaView.setVisibility(this.mCanceled ? 0 : 4);
                    }
                });
                anim.start();
            } else {
                ecaView.setAlpha(0.0f);
                ecaView.setVisibility(4);
            }
        }
        if (bouncerFrame != null) {
            if (duration > 0) {
                Animator anim2 = ObjectAnimator.ofInt(bouncerFrame, "alpha", 0, 255);
                anim2.setDuration(duration);
                anim2.start();
                return;
            }
            bouncerFrame.setAlpha(255);
        }
    }

    public static void hideBouncer(SecurityMessageDisplay securityMessageDisplay, View ecaView, Drawable bouncerFrame, int duration) {
        if (securityMessageDisplay != null) {
            securityMessageDisplay.hideBouncer(duration);
        }
        if (ecaView != null) {
            ecaView.setVisibility(0);
            if (duration > 0) {
                Animator anim = ObjectAnimator.ofFloat(ecaView, "alpha", 1.0f);
                anim.setDuration(duration);
                anim.start();
            } else {
                ecaView.setAlpha(1.0f);
            }
        }
        if (bouncerFrame != null) {
            if (duration > 0) {
                Animator anim2 = ObjectAnimator.ofInt(bouncerFrame, "alpha", 255, 0);
                anim2.setDuration(duration);
                anim2.start();
                return;
            }
            bouncerFrame.setAlpha(0);
        }
    }
}
