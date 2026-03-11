package com.android.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class DefaultIndicatorSeekBar extends SeekBar {
    private int mDefaultProgress;

    public DefaultIndicatorSeekBar(Context context) {
        super(context);
        this.mDefaultProgress = -1;
    }

    public DefaultIndicatorSeekBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDefaultProgress = -1;
    }

    public DefaultIndicatorSeekBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mDefaultProgress = -1;
    }

    public DefaultIndicatorSeekBar(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mDefaultProgress = -1;
    }

    protected void drawTickMarks(Canvas canvas) {
        if (isEnabled() && this.mDefaultProgress <= getMax() && this.mDefaultProgress >= getMin()) {
            Drawable tickMark = getTickMark();
            int intrinsicWidth = tickMark.getIntrinsicWidth();
            int intrinsicHeight = tickMark.getIntrinsicHeight();
            int i = intrinsicWidth >= 0 ? intrinsicWidth / 2 : 1;
            int i2 = intrinsicHeight >= 0 ? intrinsicHeight / 2 : 1;
            tickMark.setBounds(-i, -i2, i, i2);
            int width = (getWidth() - this.mPaddingLeft) - this.mPaddingRight;
            float max = getMax() - getMin();
            int i3 = (int) (((max > 0.0f ? this.mDefaultProgress / max : 0.0f) * width) + 0.5f);
            int i4 = (isLayoutRtl() && getMirrorForRtl()) ? (width - i3) + this.mPaddingRight : this.mPaddingLeft + i3;
            int iSave = canvas.save();
            canvas.translate(i4, getHeight() / 2);
            tickMark.draw(canvas);
            canvas.restoreToCount(iSave);
        }
    }

    public void setDefaultProgress(int i) {
        if (this.mDefaultProgress != i) {
            this.mDefaultProgress = i;
            invalidate();
        }
    }

    public int getDefaultProgress() {
        return this.mDefaultProgress;
    }
}
