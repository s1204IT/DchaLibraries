package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;

abstract class CanvasTexture extends UploadedTexture {
    protected Canvas mCanvas;
    private final Bitmap.Config mConfig = Bitmap.Config.ARGB_8888;

    protected abstract void onDraw(Canvas canvas, Bitmap bitmap);

    public CanvasTexture(int i, int i2) {
        setSize(i, i2);
        setOpaque(false);
    }

    @Override
    protected Bitmap onGetBitmap() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mWidth, this.mHeight, this.mConfig);
        this.mCanvas = new Canvas(bitmapCreateBitmap);
        onDraw(this.mCanvas, bitmapCreateBitmap);
        return bitmapCreateBitmap;
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
        if (!inFinalizer()) {
            bitmap.recycle();
        }
    }
}
