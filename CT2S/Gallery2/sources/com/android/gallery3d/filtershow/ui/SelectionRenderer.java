package com.android.gallery3d.filtershow.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

public class SelectionRenderer {
    public static void drawSelection(Canvas canvas, int left, int top, int right, int bottom, int stroke, Paint selectPaint, int border, Paint borderPaint) {
        canvas.drawRect(left, top, right, top + stroke, selectPaint);
        canvas.drawRect(left, bottom - stroke, right, bottom, selectPaint);
        canvas.drawRect(left, top, left + stroke, bottom, selectPaint);
        canvas.drawRect(right - stroke, top, right, bottom, selectPaint);
        canvas.drawRect(left + stroke, top + stroke, right - stroke, top + stroke + border, borderPaint);
        canvas.drawRect(left + stroke, (bottom - stroke) - border, right - stroke, bottom - stroke, borderPaint);
        canvas.drawRect(left + stroke, top + stroke, left + stroke + border, bottom - stroke, borderPaint);
        canvas.drawRect((right - stroke) - border, top + stroke, right - stroke, bottom - stroke, borderPaint);
    }
}
