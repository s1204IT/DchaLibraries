package com.android.deskclock;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnimatorUtils {
    private static Method sAnimateValue;
    public static final Interpolator DECELERATE_ACCELERATE_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float f) {
            float f2 = f - 0.5f;
            return 0.5f + (4.0f * f2 * f2 * f2);
        }
    };
    public static final Interpolator INTERPOLATOR_FAST_OUT_SLOW_IN = new FastOutSlowInInterpolator();
    public static final Property<View, Integer> BACKGROUND_ALPHA = new Property<View, Integer>(Integer.class, "background.alpha") {
        @Override
        public Integer get(View view) {
            Drawable background = view.getBackground();
            if (background instanceof LayerDrawable) {
                LayerDrawable layerDrawable = (LayerDrawable) background;
                if (layerDrawable.getNumberOfLayers() > 0) {
                    background = layerDrawable.getDrawable(0);
                }
            }
            return Integer.valueOf(background.getAlpha());
        }

        @Override
        public void set(View view, Integer num) {
            AnimatorUtils.setBackgroundAlpha(view, num);
        }
    };
    public static final Property<ImageView, Integer> DRAWABLE_ALPHA = new Property<ImageView, Integer>(Integer.class, "drawable.alpha") {
        @Override
        public Integer get(ImageView imageView) {
            return Integer.valueOf(imageView.getDrawable().getAlpha());
        }

        @Override
        public void set(ImageView imageView, Integer num) {
            imageView.getDrawable().setAlpha(num.intValue());
        }
    };
    public static final Property<ImageView, Integer> DRAWABLE_TINT = new Property<ImageView, Integer>(Integer.class, "drawable.tint") {
        @Override
        public Integer get(ImageView imageView) {
            return null;
        }

        @Override
        public void set(ImageView imageView, Integer num) {
            Drawable drawable = imageView.getDrawable();
            Drawable drawableWrap = DrawableCompat.wrap(drawable);
            if (drawableWrap != drawable) {
                imageView.setImageDrawable(drawableWrap);
            }
            DrawableCompat.setTint(drawableWrap, num.intValue());
        }
    };
    public static final TypeEvaluator<Integer> ARGB_EVALUATOR = new ArgbEvaluator();
    private static boolean sTryAnimateValue = true;
    public static final Property<View, Integer> VIEW_LEFT = new Property<View, Integer>(Integer.class, "left") {
        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getLeft());
        }

        @Override
        public void set(View view, Integer num) {
            view.setLeft(num.intValue());
        }
    };
    public static final Property<View, Integer> VIEW_TOP = new Property<View, Integer>(Integer.class, "top") {
        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getTop());
        }

        @Override
        public void set(View view, Integer num) {
            view.setTop(num.intValue());
        }
    };
    public static final Property<View, Integer> VIEW_BOTTOM = new Property<View, Integer>(Integer.class, "bottom") {
        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getBottom());
        }

        @Override
        public void set(View view, Integer num) {
            view.setBottom(num.intValue());
        }
    };
    public static final Property<View, Integer> VIEW_RIGHT = new Property<View, Integer>(Integer.class, "right") {
        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getRight());
        }

        @Override
        public void set(View view, Integer num) {
            view.setRight(num.intValue());
        }
    };

    public static void setBackgroundAlpha(View view, Integer num) {
        Drawable background = view.getBackground();
        if (background instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) background;
            if (layerDrawable.getNumberOfLayers() > 0) {
                background = layerDrawable.getDrawable(0);
            }
        }
        background.setAlpha(num.intValue());
    }

    public static void setAnimatedFraction(ValueAnimator valueAnimator, float f) {
        if (Utils.isLMR1OrLater()) {
            valueAnimator.setCurrentFraction(f);
            return;
        }
        if (sTryAnimateValue) {
            try {
                if (sAnimateValue == null) {
                    sAnimateValue = ValueAnimator.class.getDeclaredMethod("animateValue", Float.TYPE);
                    sAnimateValue.setAccessible(true);
                }
                sAnimateValue.invoke(valueAnimator, Float.valueOf(f));
                return;
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LogUtils.e("Unable to use animateValue directly", e);
                sTryAnimateValue = false;
            }
        }
        valueAnimator.setCurrentPlayTime(Math.round(f * valueAnimator.getDuration()));
    }

    public static void reverse(ValueAnimator... valueAnimatorArr) {
        for (ValueAnimator valueAnimator : valueAnimatorArr) {
            float animatedFraction = valueAnimator.getAnimatedFraction();
            if (animatedFraction > 0.0f) {
                valueAnimator.reverse();
                setAnimatedFraction(valueAnimator, 1.0f - animatedFraction);
            }
        }
    }

    public static void cancel(ValueAnimator... valueAnimatorArr) {
        for (ValueAnimator valueAnimator : valueAnimatorArr) {
            valueAnimator.cancel();
        }
    }

    public static ValueAnimator getScaleAnimator(View view, float... fArr) {
        return ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, fArr), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, fArr));
    }

    public static ValueAnimator getAlphaAnimator(View view, float... fArr) {
        return ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, fArr);
    }

    public static Animator getBoundsAnimator(View view, View view2, View view3) {
        Rect rect = new Rect();
        view.getBackground().getPadding(rect);
        Rect rect2 = new Rect();
        view2.getBackground().getPadding(rect2);
        Rect rect3 = new Rect();
        view3.getBackground().getPadding(rect3);
        return getBoundsAnimator(view, (view2.getLeft() - rect2.left) + rect.left, (view2.getTop() - rect2.top) + rect.top, (view2.getRight() - rect2.right) + rect.right, (view2.getBottom() - rect2.bottom) + rect.bottom, (view3.getLeft() - rect3.left) + rect.left, (view3.getTop() - rect3.top) + rect.top, (view3.getRight() - rect3.right) + rect.right, (view3.getBottom() - rect3.bottom) + rect.bottom);
    }

    public static Animator getBoundsAnimator(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        view.setLeft(i);
        view.setTop(i2);
        view.setRight(i3);
        view.setBottom(i4);
        return ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofInt(VIEW_LEFT, i5), PropertyValuesHolder.ofInt(VIEW_TOP, i6), PropertyValuesHolder.ofInt(VIEW_RIGHT, i7), PropertyValuesHolder.ofInt(VIEW_BOTTOM, i8));
    }

    public static void startDrawableAnimation(ImageView imageView) {
        Object drawable = imageView.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
    }
}
