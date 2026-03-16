package com.android.deskclock;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;

public class AnimatorUtils {
    public static final Interpolator DECELERATE_ACCELERATE_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float x) {
            return (4.0f * (x - 0.5f) * (x - 0.5f) * (x - 0.5f)) + 0.5f;
        }
    };
    public static final Property<View, Integer> BACKGROUND_ALPHA = new Property<View, Integer>(Integer.class, "background.alpha") {
        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getBackground().getAlpha());
        }

        @Override
        public void set(View view, Integer value) {
            view.getBackground().setAlpha(value.intValue());
        }
    };
    public static final Property<ImageView, Integer> DRAWABLE_ALPHA = new Property<ImageView, Integer>(Integer.class, "drawable.alpha") {
        @Override
        public Integer get(ImageView view) {
            return Integer.valueOf(view.getDrawable().getAlpha());
        }

        @Override
        public void set(ImageView view, Integer value) {
            view.getDrawable().setAlpha(value.intValue());
        }
    };
    public static final Property<ImageView, Integer> DRAWABLE_TINT = new Property<ImageView, Integer>(Integer.class, "drawable.tint") {
        @Override
        public Integer get(ImageView view) {
            return null;
        }

        @Override
        public void set(ImageView view, Integer value) {
            view.getDrawable().setTint(value.intValue());
        }
    };
    public static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

    public static void reverse(ValueAnimator... animators) {
        for (ValueAnimator animator : animators) {
            float fraction = animator.getAnimatedFraction();
            if (fraction > 0.0f) {
                animator.reverse();
            }
        }
    }

    public static ValueAnimator getScaleAnimator(View view, float... values) {
        return ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, values), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, values));
    }
}
