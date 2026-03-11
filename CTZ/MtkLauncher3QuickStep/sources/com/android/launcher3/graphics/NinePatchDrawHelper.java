package com.android.launcher3.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class NinePatchDrawHelper {
    public static final int EXTENSION_PX = 20;
    private final Rect mSrc = new Rect();
    private final RectF mDst = new RectF();
    public final Paint paint = new Paint(1);

    public void draw(Bitmap bitmap, Canvas canvas, float f, float f2, float f3) {
        int height = bitmap.getHeight();
        this.mSrc.top = 0;
        this.mSrc.bottom = height;
        this.mDst.top = f2;
        this.mDst.bottom = f2 + height;
        draw3Patch(bitmap, canvas, f, f3);
    }

    public void drawVerticallyStretched(Bitmap bitmap, Canvas canvas, float f, float f2, float f3, float f4) {
        draw(bitmap, canvas, f, f2, f3);
        int height = bitmap.getHeight();
        this.mSrc.top = height - 5;
        this.mSrc.bottom = height;
        this.mDst.top = f2 + height;
        this.mDst.bottom = f4;
        draw3Patch(bitmap, canvas, f, f3);
    }

    private void draw3Patch(Bitmap bitmap, Canvas canvas, float f, float f2) {
        int width = bitmap.getWidth();
        int i = width / 2;
        float f3 = i;
        float f4 = f + f3;
        drawRegion(bitmap, canvas, 0, i, f, f4);
        float f5 = f2 - f3;
        drawRegion(bitmap, canvas, i, width, f5, f2);
        drawRegion(bitmap, canvas, i - 5, i + 5, f4, f5);
    }

    private void drawRegion(Bitmap bitmap, Canvas canvas, int i, int i2, float f, float f2) {
        this.mSrc.left = i;
        this.mSrc.right = i2;
        this.mDst.left = f;
        this.mDst.right = f2;
        canvas.drawBitmap(bitmap, this.mSrc, this.mDst, this.paint);
    }
}
