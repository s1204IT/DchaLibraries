package com.mediatek.camera.common.widget;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;

public class ScaleAnimationButton extends RotateImageView {
    private boolean mOldPressed;
    private ObjectAnimator mScaleAnimation;

    public ScaleAnimationButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mScaleAnimation = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.9f), PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.9f));
        this.mScaleAnimation.setInterpolator(new AccelerateInterpolator());
        this.mScaleAnimation.setDuration(60L);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        boolean zIsPressed = isPressed();
        if (zIsPressed != this.mOldPressed) {
            if (zIsPressed) {
                this.mScaleAnimation.start();
            } else {
                this.mScaleAnimation.reverse();
            }
            this.mOldPressed = zIsPressed;
        }
    }
}
