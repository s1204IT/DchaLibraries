package com.android.systemui.statusbar.stack;

import android.animation.AnimatorListenerAdapter;
import android.util.ArrayMap;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;

public class AnimationProperties {
    public long delay;
    public long duration;
    private AnimatorListenerAdapter mAnimatorListenerAdapter;
    private ArrayMap<Property, Interpolator> mInterpolatorMap;

    public AnimationFilter getAnimationFilter() {
        return new AnimationFilter() {
            @Override
            public boolean shouldAnimateProperty(Property property) {
                return true;
            }
        };
    }

    public AnimatorListenerAdapter getAnimationFinishListener() {
        return this.mAnimatorListenerAdapter;
    }

    public AnimationProperties setAnimationFinishListener(AnimatorListenerAdapter animatorListenerAdapter) {
        this.mAnimatorListenerAdapter = animatorListenerAdapter;
        return this;
    }

    public boolean wasAdded(View view) {
        return false;
    }

    public Interpolator getCustomInterpolator(View view, Property property) {
        if (this.mInterpolatorMap != null) {
            return this.mInterpolatorMap.get(property);
        }
        return null;
    }

    public void combineCustomInterpolators(AnimationProperties animationProperties) {
        ArrayMap<Property, Interpolator> arrayMap = animationProperties.mInterpolatorMap;
        if (arrayMap != null) {
            if (this.mInterpolatorMap == null) {
                this.mInterpolatorMap = new ArrayMap<>();
            }
            this.mInterpolatorMap.putAll((ArrayMap<? extends Property, ? extends Interpolator>) arrayMap);
        }
    }

    public AnimationProperties setCustomInterpolator(Property property, Interpolator interpolator) {
        if (this.mInterpolatorMap == null) {
            this.mInterpolatorMap = new ArrayMap<>();
        }
        this.mInterpolatorMap.put(property, interpolator);
        return this;
    }

    public AnimationProperties setDuration(long j) {
        this.duration = j;
        return this;
    }

    public AnimationProperties setDelay(long j) {
        this.delay = j;
        return this;
    }

    public AnimationProperties resetCustomInterpolators() {
        this.mInterpolatorMap = null;
        return this;
    }
}
