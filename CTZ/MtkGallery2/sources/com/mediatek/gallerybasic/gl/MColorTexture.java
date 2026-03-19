package com.mediatek.gallerybasic.gl;

public class MColorTexture implements MTexture {
    private final int mColor;
    private int mWidth = 1;
    private int mHeight = 1;

    public MColorTexture(int i) {
        this.mColor = i;
    }

    @Override
    public void draw(MGLCanvas mGLCanvas, int i, int i2) {
        draw(mGLCanvas, i, i2, this.mWidth, this.mHeight);
    }

    @Override
    public void draw(MGLCanvas mGLCanvas, int i, int i2, int i3, int i4) {
        mGLCanvas.fillRect(i, i2, i3, i4, this.mColor);
    }

    @Override
    public boolean isOpaque() {
        return Utils.isOpaque(this.mColor);
    }

    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
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
