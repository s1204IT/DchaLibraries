package com.android.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ColorChipView extends View {
    int mBorderWidth;
    int mColor;
    private float mDefStrokeWidth;
    private int mDrawStyle;
    private Paint mPaint;

    public ColorChipView(Context context) {
        super(context);
        this.mDrawStyle = 0;
        this.mBorderWidth = 4;
        init();
    }

    public ColorChipView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDrawStyle = 0;
        this.mBorderWidth = 4;
        init();
    }

    private void init() {
        this.mPaint = new Paint();
        this.mDefStrokeWidth = this.mPaint.getStrokeWidth();
        this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void setDrawStyle(int i) {
        if (i != 0 && i != 1 && i != 2) {
            return;
        }
        this.mDrawStyle = i;
        invalidate();
    }

    public void setColor(int i) {
        this.mColor = i;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth() - 1;
        int height = getHeight() - 1;
        this.mPaint.setColor(this.mDrawStyle == 2 ? Utils.getDeclinedColorFromColor(this.mColor) : this.mColor);
        switch (this.mDrawStyle) {
            case 0:
            case 2:
                this.mPaint.setStrokeWidth(this.mDefStrokeWidth);
                canvas.drawRect(0.0f, 0.0f, width, height, this.mPaint);
                break;
            case 1:
                if (this.mBorderWidth > 0) {
                    int i = this.mBorderWidth / 2;
                    this.mPaint.setStrokeWidth(this.mBorderWidth);
                    float f = i;
                    float f2 = width;
                    float f3 = height - i;
                    float f4 = height;
                    float f5 = width - i;
                    canvas.drawLines(new float[]{0.0f, f, f2, f, 0.0f, f3, f2, f3, f, 0.0f, f, f4, f5, 0.0f, f5, f4}, this.mPaint);
                    break;
                }
                break;
        }
    }
}
