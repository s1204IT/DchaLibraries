package com.android.gallery3d.glrenderer;

public class FadeInTexture extends FadeTexture implements Texture {
    private final int mColor;
    private final TiledTexture mTexture;

    @Override
    public void draw(GLCanvas gLCanvas, int i, int i2, int i3, int i4) throws Throwable {
        if (isAnimating()) {
            this.mTexture.drawMixed(gLCanvas, this.mColor, getRatio(), i, i2, i3, i4);
        } else {
            this.mTexture.draw(gLCanvas, i, i2, i3, i4);
        }
    }
}
