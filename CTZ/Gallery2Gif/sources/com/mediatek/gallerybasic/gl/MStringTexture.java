package com.mediatek.gallerybasic.gl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

public class MStringTexture extends MCanvasTexture {
    private final Paint.FontMetricsInt mMetrics;
    private final TextPaint mPaint;
    private final String mText;

    private MStringTexture(String str, TextPaint textPaint, Paint.FontMetricsInt fontMetricsInt, int i, int i2) {
        super(i, i2);
        this.mText = str;
        this.mPaint = textPaint;
        this.mMetrics = fontMetricsInt;
    }

    public static synchronized TextPaint getDefaultPaint(float f, int i) {
        TextPaint textPaint;
        textPaint = new TextPaint();
        textPaint.setTextSize(f);
        textPaint.setAntiAlias(true);
        textPaint.setColor(i);
        textPaint.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216);
        return textPaint;
    }

    public static MStringTexture newInstance(String str, float f, int i) {
        return newInstance(str, getDefaultPaint(f, i));
    }

    public static MStringTexture newInstance(String str, float f, int i, float f2, boolean z) {
        TextPaint defaultPaint = getDefaultPaint(f, i);
        if (z) {
            defaultPaint.setTypeface(Typeface.defaultFromStyle(1));
        }
        if (f2 > 0.0f) {
            str = TextUtils.ellipsize(str, defaultPaint, f2, TextUtils.TruncateAt.END).toString();
        }
        return newInstance(str, defaultPaint);
    }

    private static MStringTexture newInstance(String str, TextPaint textPaint) {
        Paint.FontMetricsInt fontMetricsInt = textPaint.getFontMetricsInt();
        int iCeil = (int) Math.ceil(textPaint.measureText(str));
        int i = fontMetricsInt.bottom - fontMetricsInt.top;
        return new MStringTexture(str, textPaint, fontMetricsInt, iCeil <= 0 ? 1 : iCeil, i <= 0 ? 1 : i);
    }

    @Override
    protected void onDraw(Canvas canvas, Bitmap bitmap) {
        canvas.translate(0.0f, -this.mMetrics.ascent);
        canvas.drawText(this.mText, 0.0f, 0.0f, this.mPaint);
    }
}
