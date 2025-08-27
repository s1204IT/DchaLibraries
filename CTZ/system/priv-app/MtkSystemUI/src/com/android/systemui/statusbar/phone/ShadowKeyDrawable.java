package com.android.systemui.statusbar.phone;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/* loaded from: classes.dex */
public class ShadowKeyDrawable extends Drawable {
    private final Paint mPaint;
    private final ShadowDrawableState mState;

    public ShadowKeyDrawable(Drawable drawable) {
        this(drawable, new ShadowDrawableState());
    }

    private ShadowKeyDrawable(Drawable drawable, ShadowDrawableState shadowDrawableState) {
        this.mPaint = new Paint(3);
        this.mState = shadowDrawableState;
        if (drawable != null) {
            this.mState.mBaseHeight = drawable.getIntrinsicHeight();
            this.mState.mBaseWidth = drawable.getIntrinsicWidth();
            this.mState.mChangingConfigurations = drawable.getChangingConfigurations();
            this.mState.mChildState = drawable.getConstantState();
        }
    }

    public void setRotation(float f) {
        if (this.mState.mRotateDegrees != f) {
            this.mState.mRotateDegrees = f;
            this.mState.mLastDrawnBitmap = null;
            invalidateSelf();
        }
    }

    public void setShadowProperties(int i, int i2, int i3, int i4) {
        if (this.mState.mShadowOffsetX != i || this.mState.mShadowOffsetY != i2 || this.mState.mShadowSize != i3 || this.mState.mShadowColor != i4) {
            this.mState.mShadowOffsetX = i;
            this.mState.mShadowOffsetY = i2;
            this.mState.mShadowSize = i3;
            this.mState.mShadowColor = i4;
            this.mState.mLastDrawnBitmap = null;
            invalidateSelf();
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        if (this.mState.mLastDrawnBitmap == null) {
            regenerateBitmapCache();
        }
        canvas.drawBitmap(this.mState.mLastDrawnBitmap, (Rect) null, bounds, this.mPaint);
    }

    @Override // android.graphics.drawable.Drawable
    public void setTint(int i) {
        super.setTint(i);
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        this.mPaint.setAlpha(i);
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public Drawable.ConstantState getConstantState() {
        return this.mState;
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicHeight() {
        return this.mState.mBaseHeight;
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicWidth() {
        return this.mState.mBaseWidth;
    }

    @Override // android.graphics.drawable.Drawable
    public boolean canApplyTheme() {
        return this.mState.canApplyTheme();
    }

    private void regenerateBitmapCache() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(getIntrinsicWidth(), getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.save();
        float f = (float) ((this.mState.mRotateDegrees * 3.141592653589793d) / 180.0d);
        if (this.mState.mShadowSize == 0) {
            canvas.rotate(this.mState.mRotateDegrees, r1 / 2, r2 / 2);
        }
        Drawable drawableMutate = this.mState.mChildState.newDrawable().mutate();
        drawableMutate.setBounds(0, 0, this.mState.mBaseWidth, this.mState.mBaseHeight);
        drawableMutate.draw(canvas);
        if (this.mState.mShadowSize > 0) {
            Paint paint = new Paint(3);
            paint.setMaskFilter(new BlurMaskFilter(this.mState.mShadowSize, BlurMaskFilter.Blur.NORMAL));
            Bitmap bitmapExtractAlpha = bitmapCreateBitmap.extractAlpha(paint, new int[2]);
            paint.setMaskFilter(null);
            paint.setColor(this.mState.mShadowColor);
            bitmapCreateBitmap.eraseColor(0);
            canvas.rotate(this.mState.mRotateDegrees, r1 / 2, r2 / 2);
            double d = f;
            canvas.drawBitmap(bitmapExtractAlpha, r10[0] + ((float) ((Math.sin(d) * this.mState.mShadowOffsetY) + (Math.cos(d) * this.mState.mShadowOffsetX))), r10[1] + ((float) ((Math.cos(d) * this.mState.mShadowOffsetY) - (Math.sin(d) * this.mState.mShadowOffsetX))), paint);
            drawableMutate.draw(canvas);
        }
        this.mState.mLastDrawnBitmap = bitmapCreateBitmap.copy(Bitmap.Config.HARDWARE, false);
        canvas.restore();
    }

    private static class ShadowDrawableState extends Drawable.ConstantState {
        int mBaseHeight;
        int mBaseWidth;
        int mChangingConfigurations;
        Drawable.ConstantState mChildState;
        Bitmap mLastDrawnBitmap;
        float mRotateDegrees;
        int mShadowColor;
        int mShadowOffsetX;
        int mShadowOffsetY;
        int mShadowSize;

        private ShadowDrawableState() {
        }

        @Override // android.graphics.drawable.Drawable.ConstantState
        public Drawable newDrawable() {
            return new ShadowKeyDrawable(null, this);
        }

        @Override // android.graphics.drawable.Drawable.ConstantState
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }

        @Override // android.graphics.drawable.Drawable.ConstantState
        public boolean canApplyTheme() {
            return true;
        }
    }
}
