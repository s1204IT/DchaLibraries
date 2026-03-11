package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ViewState;

public class PropertyAnimator {
    public static <T extends View> void setProperty(T t, AnimatableProperty animatableProperty, float f, AnimationProperties animationProperties, boolean z) {
        if (((ValueAnimator) ViewState.getChildTag(t, animatableProperty.getAnimatorTag())) != null || z) {
            startAnimation(t, animatableProperty, f, animationProperties);
        } else {
            animatableProperty.getProperty().set(t, Float.valueOf(f));
        }
    }

    public static <T extends View> void startAnimation(final T t, AnimatableProperty animatableProperty, float f, AnimationProperties animationProperties) {
        final Property property = animatableProperty.getProperty();
        final int animationStartTag = animatableProperty.getAnimationStartTag();
        final int animationEndTag = animatableProperty.getAnimationEndTag();
        Float f2 = (Float) ViewState.getChildTag(t, animationStartTag);
        Float f3 = (Float) ViewState.getChildTag(t, animationEndTag);
        if (f3 != null && f3.floatValue() == f) {
            return;
        }
        final int animatorTag = animatableProperty.getAnimatorTag();
        ValueAnimator valueAnimator = (ValueAnimator) ViewState.getChildTag(t, animatorTag);
        if (!animationProperties.getAnimationFilter().shouldAnimateProperty(property)) {
            if (valueAnimator != null) {
                PropertyValuesHolder[] values = valueAnimator.getValues();
                float fFloatValue = f2.floatValue() + (f - f3.floatValue());
                values[0].setFloatValues(fFloatValue, f);
                t.setTag(animationStartTag, Float.valueOf(fFloatValue));
                t.setTag(animationEndTag, Float.valueOf(f));
                valueAnimator.setCurrentPlayTime(valueAnimator.getCurrentPlayTime());
                return;
            }
            property.set(t, Float.valueOf(f));
            return;
        }
        Float f4 = (Float) property.get(t);
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(f4.floatValue(), f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                property.set(t, (Float) valueAnimator2.getAnimatedValue());
            }
        });
        Interpolator customInterpolator = animationProperties.getCustomInterpolator(t, property);
        if (customInterpolator == null) {
            customInterpolator = Interpolators.FAST_OUT_SLOW_IN;
        }
        valueAnimatorOfFloat.setInterpolator(customInterpolator);
        valueAnimatorOfFloat.setDuration(ViewState.cancelAnimatorAndGetNewDuration(animationProperties.duration, valueAnimator));
        if (animationProperties.delay > 0 && (valueAnimator == null || valueAnimator.getAnimatedFraction() == 0.0f)) {
            valueAnimatorOfFloat.setStartDelay(animationProperties.delay);
        }
        AnimatorListenerAdapter animationFinishListener = animationProperties.getAnimationFinishListener();
        if (animationFinishListener != null) {
            valueAnimatorOfFloat.addListener(animationFinishListener);
        }
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                t.setTag(animatorTag, null);
                t.setTag(animationStartTag, null);
                t.setTag(animationEndTag, null);
            }
        });
        ViewState.startAnimator(valueAnimatorOfFloat, animationFinishListener);
        t.setTag(animatorTag, valueAnimatorOfFloat);
        t.setTag(animationStartTag, f4);
        t.setTag(animationEndTag, Float.valueOf(f));
    }
}
