package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class ImageFilterColorBorder extends ImageFilter {
    private FilterColorBorderRepresentation mParameters = null;
    Paint mPaint = new Paint();
    RectF mBounds = new RectF();
    RectF mInsideBounds = new RectF();
    Path mBorderPath = new Path();

    public ImageFilterColorBorder() {
        this.mName = "Border";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterColorBorderRepresentation(-1, 3, 2);
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterColorBorderRepresentation) filterRepresentation;
    }

    public FilterColorBorderRepresentation getParameters() {
        return this.mParameters;
    }

    private void applyHelper(Canvas canvas, int i, int i2) {
        if (getParameters() == null) {
            return;
        }
        float borderSize = getParameters().getBorderSize();
        float borderRadius = getParameters().getBorderRadius();
        this.mPaint.reset();
        this.mPaint.setColor(getParameters().getColor());
        this.mPaint.setAntiAlias(true);
        this.mBounds.set(0.0f, 0.0f, i, i2);
        this.mBorderPath.reset();
        this.mBorderPath.moveTo(0.0f, 0.0f);
        float fMin = (borderSize / 100.0f) * Math.min(this.mBounds.width(), this.mBounds.height());
        this.mInsideBounds.set(this.mBounds.left + fMin, this.mBounds.top + fMin, this.mBounds.right - fMin, this.mBounds.bottom - fMin);
        this.mBorderPath.moveTo(this.mBounds.left, this.mBounds.top);
        this.mBorderPath.lineTo(this.mBounds.right, this.mBounds.top);
        this.mBorderPath.lineTo(this.mBounds.right, this.mBounds.bottom);
        this.mBorderPath.lineTo(this.mBounds.left, this.mBounds.bottom);
        float fMin2 = (borderRadius / 200.0f) * Math.min(this.mInsideBounds.width(), this.mInsideBounds.height());
        this.mBorderPath.addRoundRect(this.mInsideBounds, fMin2, fMin2, Path.Direction.CCW);
        canvas.drawPath(this.mBorderPath, this.mPaint);
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        applyHelper(new Canvas(bitmap), bitmap.getWidth(), bitmap.getHeight());
        return bitmap;
    }
}
