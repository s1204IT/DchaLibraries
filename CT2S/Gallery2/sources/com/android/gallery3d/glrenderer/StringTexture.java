package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.FloatMath;

public class StringTexture extends CanvasTexture {
    private final Paint.FontMetricsInt mMetrics;
    private final TextPaint mPaint;
    private final String mText;

    private StringTexture(String text, TextPaint paint, Paint.FontMetricsInt metrics, int width, int height) {
        super(width, height);
        this.mText = text;
        this.mPaint = paint;
        this.mMetrics = metrics;
    }

    public static TextPaint getDefaultPaint(float textSize, int color) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216);
        return paint;
    }

    public static StringTexture newInstance(String text, float textSize, int color) {
        return newInstance(text, getDefaultPaint(textSize, color));
    }

    public static StringTexture newInstance(String text, float textSize, int color, float lengthLimit, boolean isBold) {
        TextPaint paint = getDefaultPaint(textSize, color);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(1));
        }
        if (lengthLimit > 0.0f) {
            text = TextUtils.ellipsize(text, paint, lengthLimit, TextUtils.TruncateAt.END).toString();
        }
        return newInstance(text, paint);
    }

    private static StringTexture newInstance(String text, TextPaint paint) {
        Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
        int width = (int) FloatMath.ceil(paint.measureText(text));
        int height = metrics.bottom - metrics.top;
        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }
        return new StringTexture(text, paint, metrics, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas, Bitmap backing) {
        canvas.translate(0.0f, -this.mMetrics.ascent);
        canvas.drawText(this.mText, 0.0f, 0.0f, this.mPaint);
    }
}
