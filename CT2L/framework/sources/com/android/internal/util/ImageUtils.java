package com.android.internal.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;

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
            bitmap = this.mTempCompactBitmap;
            height = 64;
            width = 64;
        }
        int size = height * width;
        ensureBufferSize(size);
        bitmap.getPixels(this.mTempBuffer, 0, width, 0, 0, width, height);
        for (int i = 0; i < size; i++) {
            if (!isGrayscale(this.mTempBuffer[i])) {
                return false;
            }
        }
        return true;
    }

    private void ensureBufferSize(int size) {
        if (this.mTempBuffer == null || this.mTempBuffer.length < size) {
            this.mTempBuffer = new int[size];
        }
    }

    public static boolean isGrayscale(int color) {
        int alpha = (color >> 24) & 255;
        if (alpha < 50) {
            return true;
        }
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        return Math.abs(r - g) < 20 && Math.abs(r - b) < 20 && Math.abs(g - b) < 20;
    }
}
