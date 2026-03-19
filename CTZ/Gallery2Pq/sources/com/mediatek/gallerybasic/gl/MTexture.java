package com.mediatek.gallerybasic.gl;

public interface MTexture {
    void draw(MGLCanvas mGLCanvas, int i, int i2);

    void draw(MGLCanvas mGLCanvas, int i, int i2, int i3, int i4);

    int getHeight();

    int getWidth();

    boolean isOpaque();
}
