package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.R;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

/* loaded from: classes.dex */
public class TargetDrawable {
    private Drawable mDrawable;
    private int mNumDrawables;
    private final int mResourceId;
    public static final int[] STATE_ACTIVE = {R.attr.state_enabled, R.attr.state_active};
    public static final int[] STATE_INACTIVE = {R.attr.state_enabled, -16842914};
    public static final int[] STATE_FOCUSED = {R.attr.state_enabled, -16842914, R.attr.state_focused};
    private float mTranslationX = 0.0f;
    private float mTranslationY = 0.0f;
    private float mPositionX = 0.0f;
    private float mPositionY = 0.0f;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private float mAlpha = 1.0f;
    private boolean mEnabled = true;

    public TargetDrawable(Resources resources, int i, int i2) throws Resources.NotFoundException {
        this.mNumDrawables = 1;
        this.mResourceId = i;
        setDrawable(resources, i);
        this.mNumDrawables = i2;
    }

    public void setDrawable(Resources resources, int i) throws Resources.NotFoundException {
        Drawable drawable;
        if (i != 0) {
            drawable = resources.getDrawable(i);
        } else {
            drawable = null;
        }
        this.mDrawable = drawable != null ? drawable.mutate() : null;
        resizeDrawables();
        setState(STATE_INACTIVE);
    }

    public void setState(int[] iArr) {
        if (this.mDrawable instanceof StateListDrawable) {
            ((StateListDrawable) this.mDrawable).setState(iArr);
        }
    }

    public boolean isEnabled() {
        return this.mDrawable != null && this.mEnabled;
    }

    private void resizeDrawables() {
        if (!(this.mDrawable instanceof StateListDrawable)) {
            if (this.mDrawable != null) {
                this.mDrawable.setBounds(0, 0, this.mDrawable.getIntrinsicWidth(), this.mDrawable.getIntrinsicHeight());
                return;
            }
            return;
        }
        StateListDrawable stateListDrawable = (StateListDrawable) this.mDrawable;
        int iMax = 0;
        int iMax2 = 0;
        for (int i = 0; i < this.mNumDrawables; i++) {
            stateListDrawable.selectDrawable(i);
            Drawable current = stateListDrawable.getCurrent();
            iMax = Math.max(iMax, current.getIntrinsicWidth());
            iMax2 = Math.max(iMax2, current.getIntrinsicHeight());
        }
        stateListDrawable.setBounds(0, 0, iMax, iMax2);
        for (int i2 = 0; i2 < this.mNumDrawables; i2++) {
            stateListDrawable.selectDrawable(i2);
            stateListDrawable.getCurrent().setBounds(0, 0, iMax, iMax2);
        }
    }

    public void setX(float f) {
        this.mTranslationX = f;
    }

    public void setY(float f) {
        this.mTranslationY = f;
    }

    public void setAlpha(float f) {
        this.mAlpha = f;
    }

    public float getX() {
        return this.mTranslationX;
    }

    public float getY() {
        return this.mTranslationY;
    }

    public void setPositionX(float f) {
        this.mPositionX = f;
    }

    public void setPositionY(float f) {
        this.mPositionY = f;
    }

    public int getWidth() {
        if (this.mDrawable != null) {
            return this.mDrawable.getIntrinsicWidth();
        }
        return 0;
    }

    public int getHeight() {
        if (this.mDrawable != null) {
            return this.mDrawable.getIntrinsicHeight();
        }
        return 0;
    }

    public void draw(Canvas canvas) {
        if (this.mDrawable == null || !this.mEnabled) {
            return;
        }
        canvas.save(1);
        canvas.scale(this.mScaleX, this.mScaleY, this.mPositionX, this.mPositionY);
        canvas.translate(this.mTranslationX + this.mPositionX, this.mTranslationY + this.mPositionY);
        canvas.translate(getWidth() * (-0.5f), (-0.5f) * getHeight());
        this.mDrawable.setAlpha(Math.round(this.mAlpha * 255.0f));
        this.mDrawable.draw(canvas);
        canvas.restore();
    }

    public int getResourceId() {
        return this.mResourceId;
    }
}
