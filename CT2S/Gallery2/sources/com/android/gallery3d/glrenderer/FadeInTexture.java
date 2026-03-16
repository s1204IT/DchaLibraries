package com.android.gallery3d.glrenderer;

public class FadeInTexture extends FadeTexture implements Texture {
    private final int mColor;
    private final TiledTexture mTexture;

    public FadeInTexture(int color, TiledTexture texture) {
        super(texture.getWidth(), texture.getHeight(), texture.isOpaque());
        this.mColor = color;
        this.mTexture = texture;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        if (isAnimating()) {
            this.mTexture.drawMixed(canvas, this.mColor, getRatio(), x, y, w, h);
        } else {
            this.mTexture.draw(canvas, x, y, w, h);
        }
    }
}
