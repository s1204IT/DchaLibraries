package com.android.internal.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ImageUtils {
    private static final int ALPHA_TOLERANCE = 50;
    private static final int COMPACT_BITMAP_SIZE = 64;
    private static final int TOLERANCE = 20;
    private int[] mTempBuffer;
    private Bitmap mTempCompactBitmap;
    private Canvas mTempCompactBitmapCanvas;
    private Paint mTempCompactBitmapPaint;
    private final Matrix mTempMatrix = new Matrix();

    public boolean isGrayscale(Bitmap bitmap) {
        Bitmap bitmap2;
        int i;
        int i2;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        if (height > 64 || width > 64) {
            if (this.mTempCompactBitmap == null) {
                this.mTempCompactBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
                this.mTempCompactBitmapCanvas = new Canvas(this.mTempCompactBitmap);
                this.mTempCompactBitmapPaint = new Paint(1);
                this.mTempCompactBitmapPaint.setFilterBitmap(true);
            }
            this.mTempMatrix.reset();
            this.mTempMatrix.setScale(64.0f / width, 64.0f / height, 0.0f, 0.0f);
            this.mTempCompactBitmapCanvas.drawColor(0, PorterDuff.Mode.SRC);
            this.mTempCompactBitmapCanvas.drawBitmap(bitmap, this.mTempMatrix, this.mTempCompactBitmapPaint);
            bitmap2 = this.mTempCompactBitmap;
            i = 64;
            i2 = 64;
        } else {
            bitmap2 = bitmap;
            i2 = height;
            i = width;
        }
        int i3 = i2 * i;
        ensureBufferSize(i3);
        bitmap2.getPixels(this.mTempBuffer, 0, i, 0, 0, i, i2);
        for (int i4 = 0; i4 < i3; i4++) {
            if (!isGrayscale(this.mTempBuffer[i4])) {
                return false;
            }
        }
        return true;
    }

    private void ensureBufferSize(int i) {
        if (this.mTempBuffer == null || this.mTempBuffer.length < i) {
            this.mTempBuffer = new int[i];
        }
    }

    public static boolean isGrayscale(int i) {
        if (((i >> 24) & 255) < 50) {
            return true;
        }
        int i2 = (i >> 16) & 255;
        int i3 = (i >> 8) & 255;
        int i4 = i & 255;
        return Math.abs(i2 - i3) < 20 && Math.abs(i2 - i4) < 20 && Math.abs(i3 - i4) < 20;
    }

    public static Bitmap buildScaledBitmap(Drawable drawable, int i, int i2) {
        if (drawable == null) {
            return null;
        }
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth <= i && intrinsicHeight <= i2 && (drawable instanceof BitmapDrawable)) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        if (intrinsicHeight <= 0 || intrinsicWidth <= 0) {
            return null;
        }
        float f = intrinsicWidth;
        float f2 = intrinsicHeight;
        float fMin = Math.min(1.0f, Math.min(i / f, i2 / f2));
        int i3 = (int) (f * fMin);
        int i4 = (int) (fMin * f2);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i3, i4, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        drawable.setBounds(0, 0, i3, i4);
        drawable.draw(canvas);
        return bitmapCreateBitmap;
    }
}
