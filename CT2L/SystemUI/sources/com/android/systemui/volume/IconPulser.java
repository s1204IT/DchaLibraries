package com.android.systemui.volume;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class IconPulser {
    private final Interpolator mFastOutSlowInInterpolator;

    public IconPulser(Context context) {
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
    }

    public void start(final View target) {
        if (target != null && target.getScaleX() == 1.0f) {
            target.animate().cancel();
            target.animate().scaleX(1.1f).scaleY(1.1f).setInterpolator(this.mFastOutSlowInInterpolator).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    target.animate().scaleX(1.0f).scaleY(1.0f).setListener(null);
                }
            });
        }
    }
}
