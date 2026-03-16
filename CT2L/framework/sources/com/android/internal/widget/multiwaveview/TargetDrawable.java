package com.android.internal.widget.multiwaveview;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

public class TargetDrawable {
    private static final boolean DEBUG = false;
    private static final String TAG = "TargetDrawable";
    private Drawable mDrawable;
    private final int mResourceId;
    public static final int[] STATE_ACTIVE = {16842910, 16842914};
    public static final int[] STATE_INACTIVE = {16842910, -16842914};
    public static final int[] STATE_FOCUSED = {16842910, -16842914, 16842908};
    private float mTranslationX = 0.0f;
    private float mTranslationY = 0.0f;
    private float mPositionX = 0.0f;
    private float mPositionY = 0.0f;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private float mAlpha = 1.0f;
    private boolean mEnabled = true;

    public TargetDrawable(Resources res, int resId) {
        this.mResourceId = resId;
        setDrawable(res, resId);
    }

    public void setDrawable(Resources res, int resId) {
        Drawable drawable = resId == 0 ? null : res.getDrawable(resId);
        this.mDrawable = drawable != null ? drawable.mutate() : null;
        resizeDrawables();
        setState(STATE_INACTIVE);
    }

    public TargetDrawable(TargetDrawable other) {
        this.mResourceId = other.mResourceId;
        this.mDrawable = other.mDrawable != null ? other.mDrawable.mutate() : null;
        resizeDrawables();
        setState(STATE_INACTIVE);
    }

    public void setState(int[] state) {
        if (this.mDrawable instanceof StateListDrawable) {
            StateListDrawable d = (StateListDrawable) this.mDrawable;
            d.setState(state);
        }
    }

    public boolean hasState(int[] state) {
        if (!(this.mDrawable instanceof StateListDrawable)) {
            return false;
        }
        StateListDrawable d = (StateListDrawable) this.mDrawable;
        return d.getStateDrawableIndex(state) != -1;
    }

    public boolean isActive() {
        if (this.mDrawable instanceof StateListDrawable) {
            StateListDrawable d = (StateListDrawable) this.mDrawable;
            int[] states = d.getState();
            for (int i : states) {
                if (i == 16842908) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEnabled() {
        return this.mDrawable != null && this.mEnabled;
    }

    private void resizeDrawables() {
        if (this.mDrawable instanceof StateListDrawable) {
            StateListDrawable d = (StateListDrawable) this.mDrawable;
            int maxWidth = 0;
            int maxHeight = 0;
            for (int i = 0; i < d.getStateCount(); i++) {
                Drawable childDrawable = d.getStateDrawable(i);
                maxWidth = Math.max(maxWidth, childDrawable.getIntrinsicWidth());
                maxHeight = Math.max(maxHeight, childDrawable.getIntrinsicHeight());
            }
            d.setBounds(0, 0, maxWidth, maxHeight);
            for (int i2 = 0; i2 < d.getStateCount(); i2++) {
                d.getStateDrawable(i2).setBounds(0, 0, maxWidth, maxHeight);
            }
            return;
        }
        if (this.mDrawable != null) {
            this.mDrawable.setBounds(0, 0, this.mDrawable.getIntrinsicWidth(), this.mDrawable.getIntrinsicHeight());
        }
    }

    public void setX(float x) {
        this.mTranslationX = x;
    }

    public void setY(float y) {
        this.mTranslationY = y;
    }

    public void setScaleX(float x) {
        this.mScaleX = x;
    }

    public void setScaleY(float y) {
        this.mScaleY = y;
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

    public float getScaleX() {
        return this.mScaleX;
    }

    public float getScaleY() {
        return this.mScaleY;
    }

    public float getAlpha() {
        return this.mAlpha;
    }

    public void setPositionX(float x) {
        this.mPositionX = x;
    }

    public void setPositionY(float y) {
        this.mPositionY = y;
    }

    public float getPositionX() {
        return this.mPositionX;
    }

    public float getPositionY() {
        return this.mPositionY;
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
        if (this.mDrawable != null && this.mEnabled) {
            canvas.save(1);
            canvas.scale(this.mScaleX, this.mScaleY, this.mPositionX, this.mPositionY);
            canvas.translate(this.mTranslationX + this.mPositionX, this.mTranslationY + this.mPositionY);
            canvas.translate(getWidth() * (-0.5f), getHeight() * (-0.5f));
            this.mDrawable.setAlpha(Math.round(this.mAlpha * 255.0f));
            this.mDrawable.draw(canvas);
            canvas.restore();
        }
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public int getResourceId() {
        return this.mResourceId;
    }
}
