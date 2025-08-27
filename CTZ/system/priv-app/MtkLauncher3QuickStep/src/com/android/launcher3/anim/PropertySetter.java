package com.android.launcher3.anim;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.util.Property;
import android.view.View;

/* loaded from: classes.dex */
public class PropertySetter {
    public static final PropertySetter NO_ANIM_PROPERTY_SETTER = new PropertySetter();

    public void setViewAlpha(View view, float f, TimeInterpolator timeInterpolator) {
        if (view != null) {
            view.setAlpha(f);
            AlphaUpdateListener.updateVisibility(view);
        }
    }

    public <T> void setFloat(T t, Property<T, Float> property, float f, TimeInterpolator timeInterpolator) {
        property.set(t, Float.valueOf(f));
    }

    public <T> void setInt(T t, Property<T, Integer> property, int i, TimeInterpolator timeInterpolator) {
        property.set(t, Integer.valueOf(i));
    }

    public static class AnimatedPropertySetter extends PropertySetter {
        private final long mDuration;
        private final AnimatorSetBuilder mStateAnimator;

        public AnimatedPropertySetter(long j, AnimatorSetBuilder animatorSetBuilder) {
            this.mDuration = j;
            this.mStateAnimator = animatorSetBuilder;
        }

        @Override // com.android.launcher3.anim.PropertySetter
        public void setViewAlpha(View view, float f, TimeInterpolator timeInterpolator) {
            if (view == null || view.getAlpha() == f) {
                return;
            }
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, f);
            objectAnimatorOfFloat.addListener(new AlphaUpdateListener(view));
            objectAnimatorOfFloat.setDuration(this.mDuration).setInterpolator(timeInterpolator);
            this.mStateAnimator.play(objectAnimatorOfFloat);
        }

        @Override // com.android.launcher3.anim.PropertySetter
        public <T> void setFloat(T t, Property<T, Float> property, float f, TimeInterpolator timeInterpolator) {
            if (property.get(t).floatValue() == f) {
                return;
            }
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(t, property, f);
            objectAnimatorOfFloat.setDuration(this.mDuration).setInterpolator(timeInterpolator);
            this.mStateAnimator.play(objectAnimatorOfFloat);
        }

        @Override // com.android.launcher3.anim.PropertySetter
        public <T> void setInt(T t, Property<T, Integer> property, int i, TimeInterpolator timeInterpolator) {
            if (property.get(t).intValue() == i) {
                return;
            }
            ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(t, property, i);
            objectAnimatorOfInt.setDuration(this.mDuration).setInterpolator(timeInterpolator);
            this.mStateAnimator.play(objectAnimatorOfInt);
        }
    }
}
