package com.android.deskclock.widget.sgv;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.util.Property;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import java.util.List;

public class SgvAnimationHelper {
    private static int sAnimationDuration = 450;
    private static Interpolator sDecelerateQuintInterpolator;

    public enum AnimationIn {
        NONE,
        FLY_UP_ALL_VIEWS,
        EXPAND_NEW_VIEWS,
        EXPAND_NEW_VIEWS_NO_CASCADE,
        FLY_IN_NEW_VIEWS,
        SLIDE_IN_NEW_VIEWS,
        FADE
    }

    public enum AnimationOut {
        NONE,
        FADE,
        FLY_DOWN,
        SLIDE,
        COLLAPSE
    }

    public static void initialize(Context context) {
        sDecelerateQuintInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.decelerate_quint);
        Point size = new Point();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getSize(size);
        int screenHeight = size.y;
        if (screenHeight >= 1600) {
            sAnimationDuration = 500;
        } else if (screenHeight >= 1200) {
            sAnimationDuration = 450;
        } else {
            sAnimationDuration = 400;
        }
    }

    public static int getDefaultAnimationDuration() {
        return sAnimationDuration;
    }

    private static void addXTranslationAnimators(List<Animator> animators, View view, int startTranslation, int endTranslation, int animationDelay, Animator.AnimatorListener listener) {
        view.setTranslationX(startTranslation);
        ObjectAnimator translateAnimatorX = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.TRANSLATION_X, startTranslation, endTranslation);
        translateAnimatorX.setInterpolator(sDecelerateQuintInterpolator);
        translateAnimatorX.setDuration(sAnimationDuration);
        translateAnimatorX.setStartDelay(animationDelay);
        if (listener != null) {
            translateAnimatorX.addListener(listener);
        }
        animators.add(translateAnimatorX);
    }

    private static void addYTranslationAnimators(List<Animator> animators, View view, int startTranslation, int endTranslation, int animationDelay, Animator.AnimatorListener listener) {
        view.setTranslationY(startTranslation);
        ObjectAnimator translateAnimatorY = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.TRANSLATION_Y, startTranslation, endTranslation);
        translateAnimatorY.setInterpolator(sDecelerateQuintInterpolator);
        translateAnimatorY.setDuration(sAnimationDuration);
        translateAnimatorY.setStartDelay(animationDelay);
        if (listener != null) {
            translateAnimatorY.addListener(listener);
        }
        animators.add(translateAnimatorY);
    }

    public static void addXYTranslationAnimators(List<Animator> animators, View view, int xTranslation, int yTranslation, int animationDelay) {
        addXTranslationAnimators(animators, view, xTranslation, 0, animationDelay, null);
        addYTranslationAnimators(animators, view, yTranslation, 0, animationDelay, null);
    }

    public static void addTranslationRotationAnimators(List<Animator> animators, final View view, int xTranslation, int yTranslation, float rotation, int animationDelay) {
        addXYTranslationAnimators(animators, view, xTranslation, yTranslation, animationDelay);
        view.setLayerType(2, null);
        view.setRotation(rotation);
        ObjectAnimator rotateAnimatorY = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ROTATION, view.getRotation(), 0.0f);
        rotateAnimatorY.setInterpolator(sDecelerateQuintInterpolator);
        rotateAnimatorY.setDuration(sAnimationDuration);
        rotateAnimatorY.setStartDelay(animationDelay);
        rotateAnimatorY.addListener(new AnimatorListenerAdapter() {
            private boolean mIsCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mIsCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!this.mIsCanceled) {
                    view.setRotation(0.0f);
                }
                view.setLayerType(0, null);
            }
        });
        animators.add(rotateAnimatorY);
    }

    public static void addExpandInAnimators(List<Animator> animators, final View view, int animationDelay) {
        view.setLayerType(2, null);
        view.setScaleY(0.0f);
        ObjectAnimator scaleAnimatorY = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.SCALE_Y, view.getScaleY(), 1.0f);
        scaleAnimatorY.setInterpolator(sDecelerateQuintInterpolator);
        scaleAnimatorY.setDuration(sAnimationDuration);
        scaleAnimatorY.setStartDelay(animationDelay);
        scaleAnimatorY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setScaleY(1.0f);
                view.setLayerType(0, null);
            }
        });
        animators.add(scaleAnimatorY);
    }

    public static void addCollapseOutAnimators(List<Animator> animators, final View view, int animationDelay) {
        view.setLayerType(2, null);
        ObjectAnimator scaleAnimatorY = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.SCALE_Y, view.getScaleY(), 0.0f);
        scaleAnimatorY.setInterpolator(sDecelerateQuintInterpolator);
        scaleAnimatorY.setDuration(sAnimationDuration);
        scaleAnimatorY.setStartDelay(animationDelay);
        scaleAnimatorY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setScaleY(0.0f);
                view.setLayerType(0, null);
            }
        });
        animators.add(scaleAnimatorY);
    }

    public static void addCollapseOutAnimators(List<Animator> animators, View view) {
        addCollapseOutAnimators(animators, view, 0);
    }

    public static void addFlyOutAnimators(List<Animator> animators, View view, int startTranslation, int endTranslation, int animationDelay) {
        addYTranslationAnimators(animators, view, startTranslation, endTranslation, animationDelay, null);
    }

    public static void addFlyOutAnimators(List<Animator> animators, View view, int startTranslation, int endTranslation) {
        addFlyOutAnimators(animators, view, startTranslation, endTranslation, 0);
    }

    public static void addSlideInFromRightAnimators(List<Animator> animators, View view, int startTranslation, int animationDelay) {
        addXTranslationAnimators(animators, view, startTranslation, 0, animationDelay, null);
        addFadeAnimators(animators, view, 0.0f, 1.0f, animationDelay);
    }

    public static void addSlideOutAnimators(List<Animator> animators, View view, int startTranslation, int endTranslation, int animationDelay) {
        addFadeAnimators(animators, view, view.getAlpha(), 0.0f, animationDelay);
        addXTranslationAnimators(animators, view, startTranslation, endTranslation, animationDelay, null);
    }

    public static void addSlideOutAnimators(List<Animator> animators, View view, int startTranslation, int endTranslation) {
        addSlideOutAnimators(animators, view, startTranslation, endTranslation, 0);
    }

    public static void addFadeAnimators(List<Animator> animators, final View view, float startAlpha, final float endAlpha, int animationDelay) {
        if (startAlpha != endAlpha) {
            view.setLayerType(2, null);
            view.setAlpha(startAlpha);
            ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, view.getAlpha(), endAlpha);
            fadeAnimator.setInterpolator(sDecelerateQuintInterpolator);
            fadeAnimator.setDuration(sAnimationDuration);
            fadeAnimator.setStartDelay(animationDelay);
            fadeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setAlpha(endAlpha);
                    view.setLayerType(0, null);
                }
            });
            animators.add(fadeAnimator);
        }
    }

    public static void addFadeAnimators(List<Animator> animators, View view, float startAlpha, float endAlpha) {
        addFadeAnimators(animators, view, startAlpha, endAlpha, 0);
    }
}
