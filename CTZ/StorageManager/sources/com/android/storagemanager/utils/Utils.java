package com.android.storagemanager.utils;

import android.R;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class Utils {
    public static void handleLoadingContainer(View view, View view2, boolean z, boolean z2) {
        setViewShown(view, !z, z2);
        setViewShown(view2, z, z2);
    }

    private static void setViewShown(final View view, boolean z, boolean z2) {
        if (z2) {
            Animation animationLoadAnimation = AnimationUtils.loadAnimation(view.getContext(), z ? R.anim.fade_in : R.anim.fade_out);
            if (z) {
                view.setVisibility(0);
            } else {
                animationLoadAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(8);
                    }
                });
            }
            view.startAnimation(animationLoadAnimation);
            return;
        }
        view.clearAnimation();
        view.setVisibility(z ? 0 : 8);
    }
}
