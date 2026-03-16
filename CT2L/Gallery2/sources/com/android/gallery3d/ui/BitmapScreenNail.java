package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.RectF;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.GLCanvas;

public class BitmapScreenNail implements ScreenNail {
    private final BitmapTexture mBitmapTexture;

    public BitmapScreenNail(Bitmap bitmap) {
        this.mBitmapTexture = new BitmapTexture(bitmap);
    }

    @Override
    public int getWidth() {
        return this.mBitmapTexture.getWidth();
    }

    @Override
    public int getHeight() {
        return this.mBitmapTexture.getHeight();
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        this.mBitmapTexture.draw(canvas, x, y, width, height);
    }

    @Override
    public void noDraw() {
    }

    @Override
    public void recycle() {
        this.mBitmapTexture.recycle();
    }

    @Override
    public void draw(GLCanvas canvas, RectF source, RectF dest) {
        canvas.drawTexture(this.mBitmapTexture, source, dest);
    }
}
