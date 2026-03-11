package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.R;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

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

    public TargetDrawable(Resources res, int resId, int count) {
        this.mNumDrawables = 1;
        this.mResourceId = resId;
        setDrawable(res, resId);
        this.mNumDrawables = count;
    }

    public void setDrawable(Resources res, int resId) {
        Drawable drawable = resId == 0 ? null : res.getDrawable(resId);
        this.mDrawable = drawable != null ? drawable.mutate() : null;
        resizeDrawables();
        setState(STATE_INACTIVE);
    }

    public void setState(int[] state) {
        if (!(this.mDrawable instanceof StateListDrawable)) {
            return;
        }
        StateListDrawable d = (StateListDrawable) this.mDrawable;
        d.setState(state);
    }

    public boolean isEnabled() {
        if (this.mDrawable != null) {
            return this.mEnabled;
        }
        return false;
    }

    private void resizeDrawables() {
        if (this.mDrawable instanceof StateListDrawable) {
            StateListDrawable d = (StateListDrawable) this.mDrawable;
            int maxWidth = 0;
            int maxHeight = 0;
            for (int i = 0; i < this.mNumDrawables; i++) {
                d.selectDrawable(i);
                Drawable childDrawable = d.getCurrent();
                maxWidth = Math.max(maxWidth, childDrawable.getIntrinsicWidth());
                maxHeight = Math.max(maxHeight, childDrawable.getIntrinsicHeight());
            }
            d.setBounds(0, 0, maxWidth, maxHeight);
            for (int i2 = 0; i2 < this.mNumDrawables; i2++) {
                d.selectDrawable(i2);
                d.getCurrent().setBounds(0, 0, maxWidth, maxHeight);
            }
            return;
        }
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.setBounds(0, 0, this.mDrawable.getIntrinsicWidth(), this.mDrawable.getIntrinsicHeight());
    }

    public void setX(float x) {
        this.mTranslationX = x;
    }

    public void setY(float y) {
        this.mTranslationY = y;
    }

    public void setAlpha(float alpha) {
        this.mAlpha = alpha;
    }

    public float getX() {
        return this.mTranslationX;
    }

    public float getY() {
        return this.mTranslationY;
    }

    public void setPositionX(float x) {
        this.mPositionX = x;
    }

    public void setPositionY(float y) {
        this.mPositionY = y;
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
        canvas.translate(getWidth() * (-0.5f), getHeight() * (-0.5f));
        this.mDrawable.setAlpha(Math.round(this.mAlpha * 255.0f));
        this.mDrawable.draw(canvas);
        canvas.restore();
    }

    public int getResourceId() {
        return this.mResourceId;
    }
}
