package com.android.launcher3.graphics;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class FastScrollThumbDrawable extends Drawable {
    private static final Matrix sMatrix = new Matrix();
    private final boolean mIsRtl;
    private final Paint mPaint;
    private final Path mPath = new Path();

    public FastScrollThumbDrawable(Paint paint, boolean z) {
        this.mPaint = paint;
        this.mIsRtl = z;
    }

    @Override
    public void getOutline(Outline outline) {
        if (this.mPath.isConvex()) {
            outline.setConvexPath(this.mPath);
        }
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        this.mPath.reset();
        float fHeight = rect.height() * 0.5f;
        float f = 2.0f * fHeight;
        float f2 = fHeight / 5.0f;
        this.mPath.addRoundRect(rect.left, rect.top, rect.left + f, rect.top + f, new float[]{fHeight, fHeight, fHeight, fHeight, f2, f2, fHeight, fHeight}, Path.Direction.CCW);
        sMatrix.setRotate(-45.0f, rect.left + fHeight, rect.top + fHeight);
        if (this.mIsRtl) {
            sMatrix.postTranslate(rect.width(), 0.0f);
            sMatrix.postScale(-1.0f, 1.0f, rect.width(), 0.0f);
        }
        this.mPath.transform(sMatrix);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(this.mPath, this.mPaint);
    }

    @Override
    public void setAlpha(int i) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return -3;
    }
}
