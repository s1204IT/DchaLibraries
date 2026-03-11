package com.android.launcher3.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.android.launcher3.Utilities;

@TargetApi(21)
public class UiThreadCircularReveal {
    public static ValueAnimator createCircularReveal(View v, int x, int y, float r0, float r1) {
        return createCircularReveal(v, x, y, r0, r1, ViewOutlineProvider.BACKGROUND);
    }

    public static ValueAnimator createCircularReveal(final View v, int x, int y, float r0, float r1, final ViewOutlineProvider originalProvider) {
        ValueAnimator va = ValueAnimator.ofFloat(0.0f, 1.0f);
        final RevealOutlineProvider outlineProvider = new RevealOutlineProvider(x, y, r0, r1);
        final float elevation = v.getElevation();
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                v.setOutlineProvider(outlineProvider);
                v.setClipToOutline(true);
                v.setTranslationZ(-elevation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                v.setOutlineProvider(originalProvider);
                v.setClipToOutline(false);
                v.setTranslationZ(0.0f);
            }
        });
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator arg0) {
                float progress = arg0.getAnimatedFraction();
                outlineProvider.setProgress(progress);
                v.invalidateOutline();
                if (Utilities.ATLEAST_LOLLIPOP_MR1) {
                    return;
                }
                v.invalidate();
            }
        });
        return va;
    }
}
