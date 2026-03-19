package android.support.design.circularreveal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.os.Build;
import android.support.design.circularreveal.CircularRevealWidget;
import android.util.Property;
import android.view.View;
import android.view.ViewAnimationUtils;

public final class CircularRevealCompat {
    private CircularRevealCompat() {
    }

    public static Animator createCircularReveal(CircularRevealWidget circularRevealWidget, float centerX, float centerY, float endRadius) {
        Animator revealInfoAnimator = ObjectAnimator.ofObject(circularRevealWidget, (Property<CircularRevealWidget, V>) CircularRevealWidget.CircularRevealProperty.CIRCULAR_REVEAL, (TypeEvaluator) CircularRevealWidget.CircularRevealEvaluator.CIRCULAR_REVEAL, (Object[]) new CircularRevealWidget.RevealInfo[]{new CircularRevealWidget.RevealInfo(centerX, centerY, endRadius)});
        if (Build.VERSION.SDK_INT >= 21) {
            CircularRevealWidget.RevealInfo revealInfo = circularRevealWidget.getRevealInfo();
            if (revealInfo == null) {
                throw new IllegalStateException("Caller must set a non-null RevealInfo before calling this.");
            }
            float startRadius = revealInfo.radius;
            Animator circularRevealAnimator = ViewAnimationUtils.createCircularReveal((View) circularRevealWidget, (int) centerX, (int) centerY, startRadius, endRadius);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(revealInfoAnimator, circularRevealAnimator);
            return set;
        }
        return revealInfoAnimator;
    }

    public static Animator createCircularReveal(CircularRevealWidget circularRevealWidget, float centerX, float centerY, float startRadius, float endRadius) {
        Animator revealInfoAnimator = ObjectAnimator.ofObject(circularRevealWidget, (Property<CircularRevealWidget, V>) CircularRevealWidget.CircularRevealProperty.CIRCULAR_REVEAL, (TypeEvaluator) CircularRevealWidget.CircularRevealEvaluator.CIRCULAR_REVEAL, (Object[]) new CircularRevealWidget.RevealInfo[]{new CircularRevealWidget.RevealInfo(centerX, centerY, startRadius), new CircularRevealWidget.RevealInfo(centerX, centerY, endRadius)});
        if (Build.VERSION.SDK_INT >= 21) {
            Animator circularRevealAnimator = ViewAnimationUtils.createCircularReveal((View) circularRevealWidget, (int) centerX, (int) centerY, startRadius, endRadius);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(revealInfoAnimator, circularRevealAnimator);
            return set;
        }
        return revealInfoAnimator;
    }

    public static Animator.AnimatorListener createCircularRevealListener(final CircularRevealWidget view) {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.buildCircularRevealCache();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.destroyCircularRevealCache();
            }
        };
    }
}
