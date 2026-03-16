package com.bumptech.glide.request.target;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.util.TimeUtils;

public class SquaringDrawable extends Drawable {
    private int side;
    private final Drawable wrapped;

    public SquaringDrawable(Drawable wrapped, int side) {
        this.wrapped = wrapped;
        this.side = side;
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        this.wrapped.setBounds(left, top, right, bottom);
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        this.wrapped.setBounds(bounds);
    }

    @Override
    public void setChangingConfigurations(int configs) {
        this.wrapped.setChangingConfigurations(configs);
    }

    @Override
    public int getChangingConfigurations() {
        return this.wrapped.getChangingConfigurations();
    }

    @Override
    public void setDither(boolean dither) {
        this.wrapped.setDither(dither);
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        this.wrapped.setFilterBitmap(filter);
    }

    @Override
    @TargetApi(11)
    public Drawable.Callback getCallback() {
        return this.wrapped.getCallback();
    }

    @Override
    @TargetApi(TimeUtils.HUNDRED_DAY_FIELD_LEN)
    public int getAlpha() {
        return this.wrapped.getAlpha();
    }

    @Override
    public void setColorFilter(int color, PorterDuff.Mode mode) {
        this.wrapped.setColorFilter(color, mode);
    }

    @Override
    public void clearColorFilter() {
        this.wrapped.clearColorFilter();
    }

    @Override
    public Drawable getCurrent() {
        return this.wrapped.getCurrent();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        return this.wrapped.setVisible(visible, restart);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.side;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.side;
    }

    @Override
    public int getMinimumWidth() {
        return this.wrapped.getMinimumWidth();
    }

    @Override
    public int getMinimumHeight() {
        return this.wrapped.getMinimumHeight();
    }

    @Override
    public boolean getPadding(Rect padding) {
        return this.wrapped.getPadding(padding);
    }

    @Override
    public void invalidateSelf() {
        super.invalidateSelf();
        this.wrapped.invalidateSelf();
    }

    @Override
    public void unscheduleSelf(Runnable what) {
        super.unscheduleSelf(what);
        this.wrapped.unscheduleSelf(what);
    }

    @Override
    public void scheduleSelf(Runnable what, long when) {
        super.scheduleSelf(what, when);
        this.wrapped.scheduleSelf(what, when);
    }

    @Override
    public void draw(Canvas canvas) {
        this.wrapped.draw(canvas);
    }

    @Override
    public void setAlpha(int i) {
        this.wrapped.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.wrapped.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return this.wrapped.getOpacity();
    }
}
