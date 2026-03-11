package com.android.systemui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import com.android.settingslib.Utils;

public class HardwareBgDrawable extends LayerDrawable {
    private final Drawable[] mLayers;
    private final Paint mPaint;
    private int mPoint;
    private boolean mRotatedBackground;
    private final boolean mRoundTop;

    public HardwareBgDrawable(boolean z, boolean z2, Context context) {
        this(z, getLayers(context, z, z2));
    }

    public HardwareBgDrawable(boolean z, Drawable[] drawableArr) {
        super(drawableArr);
        this.mPaint = new Paint();
        if (drawableArr.length != 2) {
            throw new IllegalArgumentException("Need 2 layers");
        }
        this.mRoundTop = z;
        this.mLayers = drawableArr;
    }

    private static Drawable[] getLayers(Context context, boolean z, boolean z2) {
        Drawable[] drawableArr;
        int i = z2 ? R.drawable.rounded_bg_full : R.drawable.rounded_bg;
        if (z) {
            drawableArr = new Drawable[]{context.getDrawable(i).mutate(), context.getDrawable(i).mutate()};
        } else {
            drawableArr = new Drawable[2];
            drawableArr[0] = context.getDrawable(i).mutate();
            drawableArr[1] = context.getDrawable(z2 ? R.drawable.rounded_full_bg_bottom : R.drawable.rounded_bg_bottom).mutate();
        }
        drawableArr[1].setTint(Utils.getColorAttr(context, android.R.attr.colorPrimary));
        return drawableArr;
    }

    public void setCutPoint(int i) {
        this.mPoint = i;
        invalidateSelf();
    }

    public int getCutPoint() {
        return this.mPoint;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mPoint >= 0 && !this.mRotatedBackground) {
            Rect bounds = getBounds();
            int i = bounds.top + this.mPoint;
            if (i > bounds.bottom) {
                i = bounds.bottom;
            }
            if (this.mRoundTop) {
                this.mLayers[0].setBounds(bounds.left, bounds.top, bounds.right, i);
            } else {
                this.mLayers[1].setBounds(bounds.left, i, bounds.right, bounds.bottom);
            }
            if (this.mRoundTop) {
                this.mLayers[1].draw(canvas);
                this.mLayers[0].draw(canvas);
                return;
            } else {
                this.mLayers[0].draw(canvas);
                this.mLayers[1].draw(canvas);
                return;
            }
        }
        this.mLayers[0].draw(canvas);
    }

    @Override
    public void setAlpha(int i) {
        this.mPaint.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return -1;
    }

    public void setRotatedBackground(boolean z) {
        this.mRotatedBackground = z;
    }
}
