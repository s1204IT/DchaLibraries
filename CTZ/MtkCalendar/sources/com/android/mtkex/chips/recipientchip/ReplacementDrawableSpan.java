package com.android.mtkex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

public class ReplacementDrawableSpan extends ReplacementSpan {
    protected static final Paint sWorkPaint = new Paint();
    protected Drawable mDrawable;
    private float mExtraMargin;

    public ReplacementDrawableSpan(Drawable drawable) {
        this.mDrawable = drawable;
    }

    private void setupFontMetrics(Paint.FontMetricsInt fontMetricsInt, Paint paint) {
        sWorkPaint.set(paint);
        if (fontMetricsInt != null) {
            sWorkPaint.getFontMetricsInt(fontMetricsInt);
            Rect bounds = getBounds();
            int i = fontMetricsInt.descent - fontMetricsInt.ascent;
            int i2 = ((int) this.mExtraMargin) / 2;
            fontMetricsInt.ascent = Math.min(fontMetricsInt.top, fontMetricsInt.top + ((i - bounds.bottom) / 2)) - i2;
            fontMetricsInt.descent = Math.max(fontMetricsInt.bottom, fontMetricsInt.bottom + ((bounds.bottom - i) / 2)) + i2;
            fontMetricsInt.top = fontMetricsInt.ascent;
            fontMetricsInt.bottom = fontMetricsInt.descent;
        }
    }

    @Override
    public int getSize(Paint paint, CharSequence charSequence, int i, int i2, Paint.FontMetricsInt fontMetricsInt) {
        setupFontMetrics(fontMetricsInt, paint);
        return getBounds().right;
    }

    @Override
    public void draw(Canvas canvas, CharSequence charSequence, int i, int i2, float f, int i3, int i4, int i5, Paint paint) {
        canvas.save();
        canvas.translate(f, ((i5 - this.mDrawable.getBounds().bottom) + i3) / 2);
        this.mDrawable.draw(canvas);
        canvas.restore();
    }

    protected Rect getBounds() {
        return this.mDrawable.getBounds();
    }
}
