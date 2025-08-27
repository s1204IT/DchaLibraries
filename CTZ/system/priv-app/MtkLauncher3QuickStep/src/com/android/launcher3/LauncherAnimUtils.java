package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.util.Property;
import android.view.View;
import android.view.ViewTreeObserver;
import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;

/* loaded from: classes.dex */
public class LauncherAnimUtils {
    public static final int ALL_APPS_TRANSITION_MS = 320;
    public static final float MIN_PROGRESS_TO_ALL_APPS = 0.5f;
    public static final int OVERVIEW_TRANSITION_MS = 250;
    public static final int SPRING_LOADED_EXIT_DELAY = 500;
    public static final int SPRING_LOADED_TRANSITION_MS = 150;
    static WeakHashMap<Animator, Object> sAnimators = new WeakHashMap<>();
    static Animator.AnimatorListener sEndAnimListener = new Animator.AnimatorListener() { // from class: com.android.launcher3.LauncherAnimUtils.1
        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
            LauncherAnimUtils.sAnimators.put(animator, null);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            LauncherAnimUtils.sAnimators.remove(animator);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            LauncherAnimUtils.sAnimators.remove(animator);
        }
    };
    public static final Property<Drawable, Integer> DRAWABLE_ALPHA = new Property<Drawable, Integer>(Integer.TYPE, "drawableAlpha") { // from class: com.android.launcher3.LauncherAnimUtils.3
        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Integer get(Drawable drawable) {
            return Integer.valueOf(drawable.getAlpha());
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(Drawable drawable, Integer num) {
            drawable.setAlpha(num.intValue());
        }
    };
    public static final Property<View, Float> SCALE_PROPERTY = new Property<View, Float>(Float.class, "scale") { // from class: com.android.launcher3.LauncherAnimUtils.4
        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(View view) {
            return Float.valueOf(view.getScaleX());
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(View view, Float f) {
            view.setScaleX(f.floatValue());
            view.setScaleY(f.floatValue());
        }
    };

    public static void cancelOnDestroyActivity(Animator animator) {
        animator.addListener(sEndAnimListener);
    }

    public static void startAnimationAfterNextDraw(final Animator animator, final View view) {
        view.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() { // from class: com.android.launcher3.LauncherAnimUtils.2
            private boolean mStarted = false;

            @Override // android.view.ViewTreeObserver.OnDrawListener
            public void onDraw() {
                if (this.mStarted) {
                    return;
                }
                this.mStarted = true;
                if (animator.getDuration() == 0) {
                    return;
                }
                animator.start();
                view.post(new Runnable() { // from class: com.android.launcher3.LauncherAnimUtils.2.1
                    @Override // java.lang.Runnable
                    public void run() {
                        view.getViewTreeObserver().removeOnDrawListener(this);
                    }
                });
            }
        });
    }

    public static void onDestroyActivity() {
        Iterator it = new HashSet(sAnimators.keySet()).iterator();
        while (it.hasNext()) {
            Animator animator = (Animator) it.next();
            if (animator.isRunning()) {
                animator.cancel();
            }
            sAnimators.remove(animator);
        }
    }

    public static AnimatorSet createAnimatorSet() {
        AnimatorSet animatorSet = new AnimatorSet();
        cancelOnDestroyActivity(animatorSet);
        return animatorSet;
    }

    public static ValueAnimator ofFloat(float... fArr) {
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setFloatValues(fArr);
        cancelOnDestroyActivity(valueAnimator);
        return valueAnimator;
    }

    public static ObjectAnimator ofFloat(View view, Property<View, Float> property, float... fArr) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, property, fArr);
        cancelOnDestroyActivity(objectAnimatorOfFloat);
        new FirstFrameAnimatorHelper(objectAnimatorOfFloat, view);
        return objectAnimatorOfFloat;
    }

    public static ObjectAnimator ofViewAlphaAndScale(View view, float f, float f2, float f3) {
        return ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat((Property<?, Float>) View.ALPHA, f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, f2), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, f3));
    }

    public static ObjectAnimator ofPropertyValuesHolder(View view, PropertyValuesHolder... propertyValuesHolderArr) {
        return ofPropertyValuesHolder(view, view, propertyValuesHolderArr);
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object obj, View view, PropertyValuesHolder... propertyValuesHolderArr) {
        ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(obj, propertyValuesHolderArr);
        cancelOnDestroyActivity(objectAnimatorOfPropertyValuesHolder);
        new FirstFrameAnimatorHelper(objectAnimatorOfPropertyValuesHolder, view);
        return objectAnimatorOfPropertyValuesHolder;
    }

    public static int blockedFlingDurationFactor(float f) {
        return (int) Utilities.boundToRange(Math.abs(f) / 2.0f, 2.0f, 6.0f);
    }
}
