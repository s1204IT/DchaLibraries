package android.support.v7.graphics.drawable;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;

public class DrawableWrapper extends Drawable implements Drawable.Callback {
    private Drawable mDrawable;

    public DrawableWrapper(Drawable drawable) {
        setWrappedDrawable(drawable);
    }

    @Override
    public void draw(Canvas canvas) {
        this.mDrawable.draw(canvas);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.mDrawable.setBounds(bounds);
    }

    @Override
    public void setChangingConfigurations(int configs) {
        this.mDrawable.setChangingConfigurations(configs);
    }

    @Override
    public int getChangingConfigurations() {
        return this.mDrawable.getChangingConfigurations();
    }

    @Override
    public void setDither(boolean dither) {
        this.mDrawable.setDither(dither);
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        this.mDrawable.setFilterBitmap(filter);
    }

    @Override
    public void setAlpha(int alpha) {
        this.mDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.mDrawable.setColorFilter(cf);
    }

    @Override
    public boolean isStateful() {
        return this.mDrawable.isStateful();
    }

    @Override
    public boolean setState(int[] stateSet) {
        return this.mDrawable.setState(stateSet);
    }

    @Override
    public int[] getState() {
        return this.mDrawable.getState();
    }

    @Override
    public void jumpToCurrentState() {
        DrawableCompat.jumpToCurrentState(this.mDrawable);
    }

    @Override
    public Drawable getCurrent() {
        return this.mDrawable.getCurrent();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        return super.setVisible(visible, restart) || this.mDrawable.setVisible(visible, restart);
    }

    @Override
    public int getOpacity() {
        return this.mDrawable.getOpacity();
    }

    @Override
    public Region getTransparentRegion() {
        return this.mDrawable.getTransparentRegion();
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mDrawable.getIntrinsicHeight();
    }

    @Override
    public int getMinimumWidth() {
        return this.mDrawable.getMinimumWidth();
    }

    @Override
    public int getMinimumHeight() {
        return this.mDrawable.getMinimumHeight();
    }

    @Override
    public boolean getPadding(Rect padding) {
        return this.mDrawable.getPadding(padding);
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    protected boolean onLevelChange(int level) {
        return this.mDrawable.setLevel(level);
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        DrawableCompat.setAutoMirrored(this.mDrawable, mirrored);
    }

    @Override
    public boolean isAutoMirrored() {
        return DrawableCompat.isAutoMirrored(this.mDrawable);
    }

    @Override
    public void setTint(int tint) {
        DrawableCompat.setTint(this.mDrawable, tint);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        DrawableCompat.setTintList(this.mDrawable, tint);
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        DrawableCompat.setTintMode(this.mDrawable, tintMode);
    }

    @Override
    public void setHotspot(float x, float y) {
        DrawableCompat.setHotspot(this.mDrawable, x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        DrawableCompat.setHotspotBounds(this.mDrawable, left, top, right, bottom);
    }

    public Drawable getWrappedDrawable() {
        return this.mDrawable;
    }

    public void setWrappedDrawable(Drawable drawable) {
        if (this.mDrawable != null) {
            this.mDrawable.setCallback(null);
        }
        this.mDrawable = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
        }
    }
}
