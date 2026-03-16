package com.android.camera.widget;

import android.animation.Animator;
import android.graphics.Canvas;
import android.view.MotionEvent;

public abstract class AnimationEffects {
    public abstract void drawForeground(Canvas canvas);

    public abstract void endAnimation();

    public abstract void setSize(int i, int i2);

    public abstract void startAnimation(Animator.AnimatorListener animatorListener);

    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    public void drawBackground(Canvas canvas) {
    }

    public boolean cancelAnimation() {
        return false;
    }

    public boolean shouldDrawSuper() {
        return true;
    }
}
