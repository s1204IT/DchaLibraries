package com.android.gallery3d.glrenderer;

import com.android.gallery3d.common.Utils;

public class ColorTexture implements Texture {
    private final int mColor;
    private int mWidth = 1;
    private int mHeight = 1;

    public ColorTexture(int color) {
        this.mColor = color;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y) {
        draw(canvas, x, y, this.mWidth, this.mHeight);
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        canvas.fillRect(x, y, w, h, this.mColor);
    }

    @Override
    public boolean isOpaque() {
        return Utils.isOpaque(this.mColor);
    }

    public void setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }
}
