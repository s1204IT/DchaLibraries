package com.android.quickstep;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.util.FloatProperty;
import com.android.launcher3.LauncherSettings;

public class AnimatedFloat {
    public static FloatProperty<AnimatedFloat> VALUE = new FloatProperty<AnimatedFloat>(LauncherSettings.Settings.EXTRA_VALUE) {
        @Override
        public void setValue(AnimatedFloat animatedFloat, float f) {
            animatedFloat.updateValue(f);
        }

        @Override
        public Float get(AnimatedFloat animatedFloat) {
            return Float.valueOf(animatedFloat.value);
        }
    };
    private final Runnable mUpdateCallback;
    private ObjectAnimator mValueAnimator;
    public float value;

    public AnimatedFloat(Runnable runnable) {
        this.mUpdateCallback = runnable;
    }

    public ObjectAnimator animateToValue(float f, float f2) {
        cancelAnimation();
        this.mValueAnimator = ObjectAnimator.ofFloat(this, VALUE, f, f2);
        this.mValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (AnimatedFloat.this.mValueAnimator == animator) {
                    AnimatedFloat.this.mValueAnimator = null;
                }
            }
        });
        return this.mValueAnimator;
    }

    public void updateValue(float f) {
        if (Float.compare(f, this.value) != 0) {
            this.value = f;
            this.mUpdateCallback.run();
        }
    }

    public void cancelAnimation() {
        if (this.mValueAnimator != null) {
            this.mValueAnimator.cancel();
        }
    }

    public void finishAnimation() {
        if (this.mValueAnimator != null && this.mValueAnimator.isRunning()) {
            this.mValueAnimator.end();
        }
    }

    public ObjectAnimator getCurrentAnimation() {
        return this.mValueAnimator;
    }
}
