package com.android.settingslib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class RestrictedLockImageSpan extends ImageSpan {
    private Context mContext;
    private final float mExtraPadding;
    private final Drawable mRestrictedPadlock;

    public RestrictedLockImageSpan(Context context) {
        super((Drawable) null);
        this.mContext = context;
        this.mExtraPadding = this.mContext.getResources().getDimensionPixelSize(R$dimen.restricted_icon_padding);
        this.mRestrictedPadlock = RestrictedLockUtils.getRestrictedPadlock(this.mContext);
    }

    @Override
    public Drawable getDrawable() {
        return this.mRestrictedPadlock;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Drawable drawable = getDrawable();
        canvas.save();
        float transX = x + this.mExtraPadding;
        float transY = (bottom - drawable.getBounds().bottom) / 2.0f;
        canvas.translate(transX, transY);
        drawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fontMetrics) {
        int size = super.getSize(paint, text, start, end, fontMetrics);
        return (int) (size + (this.mExtraPadding * 2.0f));
    }
}
