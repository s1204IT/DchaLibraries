package com.android.launcher3.anim;

import android.animation.PropertyValuesHolder;
import android.util.Property;
import android.view.View;
import java.util.ArrayList;

public class PropertyListBuilder {
    private final ArrayList<PropertyValuesHolder> mProperties = new ArrayList<>();

    public PropertyListBuilder translationX(float f) {
        this.mProperties.add(PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_X, f));
        return this;
    }

    public PropertyListBuilder translationY(float f) {
        this.mProperties.add(PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_Y, f));
        return this;
    }

    public PropertyListBuilder scaleX(float f) {
        this.mProperties.add(PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, f));
        return this;
    }

    public PropertyListBuilder scaleY(float f) {
        this.mProperties.add(PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, f));
        return this;
    }

    public PropertyListBuilder scale(float f) {
        return scaleX(f).scaleY(f);
    }

    public PropertyListBuilder alpha(float f) {
        this.mProperties.add(PropertyValuesHolder.ofFloat((Property<?, Float>) View.ALPHA, f));
        return this;
    }

    public PropertyValuesHolder[] build() {
        return (PropertyValuesHolder[]) this.mProperties.toArray(new PropertyValuesHolder[this.mProperties.size()]);
    }
}
