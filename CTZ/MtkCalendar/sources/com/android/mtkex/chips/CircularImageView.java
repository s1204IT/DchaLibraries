package com.android.mtkex.chips;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CircularImageView extends ImageView {
    private static float circularImageBorder = 1.0f;
    private final Paint bitmapPaint;
    private final Paint borderPaint;
    private final RectF destination;
    private final Matrix matrix;
    private final RectF source;

    public CircularImageView(Context context) {
        this(context, null, 0);
    }

    public CircularImageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CircularImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.matrix = new Matrix();
        this.source = new RectF();
        this.destination = new RectF();
        this.bitmapPaint = new Paint();
        this.bitmapPaint.setAntiAlias(true);
        this.bitmapPaint.setFilterBitmap(true);
        this.bitmapPaint.setDither(true);
        this.borderPaint = new Paint();
        this.borderPaint.setColor(0);
        this.borderPaint.setStyle(Paint.Style.STROKE);
        this.borderPaint.setStrokeWidth(circularImageBorder);
        this.borderPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        BitmapDrawable bitmapDrawable;
        Bitmap bitmap;
        ?? drawable = getDrawable();
        if (drawable instanceof StateListDrawable) {
            if (drawable.getCurrent() != null) {
                bitmapDrawable = (BitmapDrawable) drawable.getCurrent();
            } else {
                bitmapDrawable = null;
            }
        } else {
            bitmapDrawable = (BitmapDrawable) drawable;
        }
        if (bitmapDrawable == null || (bitmap = bitmapDrawable.getBitmap()) == null) {
            return;
        }
        this.source.set(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());
        this.destination.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        drawBitmapWithCircleOnCanvas(bitmap, canvas, this.source, this.destination);
    }

    public void drawBitmapWithCircleOnCanvas(Bitmap bitmap, Canvas canvas, RectF rectF, RectF rectF2) {
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        this.matrix.reset();
        this.matrix.setRectToRect(rectF, rectF2, Matrix.ScaleToFit.FILL);
        bitmapShader.setLocalMatrix(this.matrix);
        this.bitmapPaint.setShader(bitmapShader);
        canvas.drawCircle(rectF2.centerX(), rectF2.centerY(), rectF2.width() / 2.0f, this.bitmapPaint);
        canvas.drawCircle(rectF2.centerX(), rectF2.centerY(), (rectF2.width() / 2.0f) - (circularImageBorder / 2.0f), this.borderPaint);
    }
}
