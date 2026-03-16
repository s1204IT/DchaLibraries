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

    public ColorChipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDrawStyle = 0;
        this.mBorderWidth = 4;
        init();
    }

    private void init() {
        this.mPaint = new Paint();
        this.mDefStrokeWidth = this.mPaint.getStrokeWidth();
        this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void setDrawStyle(int style) {
        if (style == 0 || style == 1 || style == 2) {
            this.mDrawStyle = style;
            invalidate();
        }
    }

    public void setColor(int color) {
        this.mColor = color;
        invalidate();
    }

    @Override
    public void onDraw(Canvas c) {
        int right = getWidth() - 1;
        int bottom = getHeight() - 1;
        this.mPaint.setColor(this.mDrawStyle == 2 ? Utils.getDeclinedColorFromColor(this.mColor) : this.mColor);
        switch (this.mDrawStyle) {
            case 0:
            case 2:
                this.mPaint.setStrokeWidth(this.mDefStrokeWidth);
                c.drawRect(0.0f, 0.0f, right, bottom, this.mPaint);
                break;
            case 1:
                if (this.mBorderWidth > 0) {
                    int halfBorderWidth = this.mBorderWidth / 2;
                    this.mPaint.setStrokeWidth(this.mBorderWidth);
                    float[] lines = new float[16];
                    int ptr = 0 + 1;
                    lines[0] = 0.0f;
                    int ptr2 = ptr + 1;
                    lines[ptr] = halfBorderWidth;
                    int ptr3 = ptr2 + 1;
                    lines[ptr2] = right;
                    int ptr4 = ptr3 + 1;
                    lines[ptr3] = halfBorderWidth;
                    int ptr5 = ptr4 + 1;
                    lines[ptr4] = 0.0f;
                    int ptr6 = ptr5 + 1;
                    lines[ptr5] = bottom - halfBorderWidth;
                    int ptr7 = ptr6 + 1;
                    lines[ptr6] = right;
                    int ptr8 = ptr7 + 1;
                    lines[ptr7] = bottom - halfBorderWidth;
                    int ptr9 = ptr8 + 1;
                    lines[ptr8] = halfBorderWidth;
                    int ptr10 = ptr9 + 1;
                    lines[ptr9] = 0.0f;
                    int ptr11 = ptr10 + 1;
                    lines[ptr10] = halfBorderWidth;
                    int ptr12 = ptr11 + 1;
                    lines[ptr11] = bottom;
                    int ptr13 = ptr12 + 1;
                    lines[ptr12] = right - halfBorderWidth;
                    int ptr14 = ptr13 + 1;
                    lines[ptr13] = 0.0f;
                    int ptr15 = ptr14 + 1;
                    lines[ptr14] = right - halfBorderWidth;
                    int i = ptr15 + 1;
                    lines[ptr15] = bottom;
                    c.drawLines(lines, this.mPaint);
                }
                break;
        }
    }
}
