package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.util.Property;

/* loaded from: classes.dex */
public class PropertyResetListener<T, V> extends AnimatorListenerAdapter {
    private Property<T, V> mPropertyToReset;
    private V mResetToValue;

    public PropertyResetListener(Property<T, V> property, V v) {
        this.mPropertyToReset = property;
        this.mResetToValue = v;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: android.util.FloatProperty */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
    public void onAnimationEnd(Animator animator) {
        this.mPropertyToReset.set(((ObjectAnimator) animator).getTarget(), this.mResetToValue);
    }
}
