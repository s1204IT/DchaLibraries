package com.android.ex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

public class ReplacementDrawableSpan extends ReplacementSpan {
    protected Drawable mDrawable;
    private float mExtraMargin;
    private final Paint mWorkPaint = new Paint();

    public ReplacementDrawableSpan(Drawable drawable) {
        this.mDrawable = drawable;
    }

    public void setExtraMargin(float margin) {
        this.mExtraMargin = margin;
    }

    private void setupFontMetrics(Paint.FontMetricsInt fm, Paint paint) {
        this.mWorkPaint.set(paint);
        if (fm != null) {
            this.mWorkPaint.getFontMetricsInt(fm);
            Rect bounds = getBounds();
            int textHeight = fm.descent - fm.ascent;
            int halfMargin = ((int) this.mExtraMargin) / 2;
            fm.ascent = Math.min(fm.top, fm.top + ((textHeight - bounds.bottom) / 2)) - halfMargin;
            fm.descent = Math.max(fm.bottom, fm.bottom + ((bounds.bottom - textHeight) / 2)) + halfMargin;
            fm.top = fm.ascent;
            fm.bottom = fm.descent;
        }
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int i, int i2, Paint.FontMetricsInt fm) {
        setupFontMetrics(fm, paint);
        return getBounds().right;
    }

    @Override
    public void draw(Canvas canvas, CharSequence charSequence, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        canvas.save();
        int transY = ((bottom - this.mDrawable.getBounds().bottom) + top) / 2;
        canvas.translate(x, transY);
        this.mDrawable.draw(canvas);
        canvas.restore();
    }

    protected Rect getBounds() {
        return this.mDrawable.getBounds();
    }
}
