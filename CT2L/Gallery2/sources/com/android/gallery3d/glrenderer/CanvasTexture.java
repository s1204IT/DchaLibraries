package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;

abstract class CanvasTexture extends UploadedTexture {
    protected Canvas mCanvas;
    private final Bitmap.Config mConfig = Bitmap.Config.ARGB_8888;

    protected abstract void onDraw(Canvas canvas, Bitmap bitmap);

    public CanvasTexture(int width, int height) {
        setSize(width, height);
        setOpaque(false);
    }

    @Override
    protected Bitmap onGetBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(this.mWidth, this.mHeight, this.mConfig);
        this.mCanvas = new Canvas(bitmap);
        onDraw(this.mCanvas, bitmap);
        return bitmap;
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
        if (!inFinalizer()) {
            bitmap.recycle();
        }
    }
}
