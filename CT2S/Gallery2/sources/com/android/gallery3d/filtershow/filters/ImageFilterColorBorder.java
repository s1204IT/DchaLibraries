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
        return new FilterColorBorderRepresentation(0, -1, 3, 2);
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterColorBorderRepresentation parameters = (FilterColorBorderRepresentation) representation;
        this.mParameters = parameters;
    }

    public FilterColorBorderRepresentation getParameters() {
        return this.mParameters;
    }

    private void applyHelper(Canvas canvas, int w, int h) {
        if (getParameters() != null) {
            float size = getParameters().getBorderSize();
            float radius = getParameters().getBorderRadius();
            this.mPaint.reset();
            this.mPaint.setColor(getParameters().getColor());
            this.mPaint.setAntiAlias(true);
            this.mBounds.set(0.0f, 0.0f, w, h);
            this.mBorderPath.reset();
            this.mBorderPath.moveTo(0.0f, 0.0f);
            float bs = (size / 100.0f) * this.mBounds.width();
            float r = (radius / 100.0f) * this.mBounds.width();
            this.mInsideBounds.set(this.mBounds.left + bs, this.mBounds.top + bs, this.mBounds.right - bs, this.mBounds.bottom - bs);
            this.mBorderPath.moveTo(this.mBounds.left, this.mBounds.top);
            this.mBorderPath.lineTo(this.mBounds.right, this.mBounds.top);
            this.mBorderPath.lineTo(this.mBounds.right, this.mBounds.bottom);
            this.mBorderPath.lineTo(this.mBounds.left, this.mBounds.bottom);
            this.mBorderPath.addRoundRect(this.mInsideBounds, r, r, Path.Direction.CCW);
            canvas.drawPath(this.mBorderPath, this.mPaint);
        }
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        Canvas canvas = new Canvas(bitmap);
        applyHelper(canvas, bitmap.getWidth(), bitmap.getHeight());
        return bitmap;
    }
}
