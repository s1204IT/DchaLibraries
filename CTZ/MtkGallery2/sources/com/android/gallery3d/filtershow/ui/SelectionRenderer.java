package com.android.gallery3d.filtershow.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

public class SelectionRenderer {
    public static void drawSelection(Canvas canvas, int i, int i2, int i3, int i4, int i5, Paint paint, int i6, Paint paint2) {
        float f = i;
        float f2 = i2;
        float f3 = i3;
        float f4 = i2 + i5;
        canvas.drawRect(f, f2, f3, f4, paint);
        float f5 = i4 - i5;
        float f6 = i4;
        canvas.drawRect(f, f5, f3, f6, paint);
        float f7 = i + i5;
        canvas.drawRect(f, f2, f7, f6, paint);
        float f8 = i3 - i5;
        canvas.drawRect(f8, f2, f3, f6, paint);
        canvas.drawRect(f7, f4, f8, r1 + i6, paint2);
        canvas.drawRect(f7, r11 - i6, f8, f5, paint2);
        canvas.drawRect(f7, f4, r0 + i6, f5, paint2);
        canvas.drawRect(r2 - i6, f4, f8, f5, paint2);
    }
}
