package com.android.gallery3d.glrenderer;

public class FadeOutTexture extends FadeTexture {
    private final BasicTexture mTexture;

    public FadeOutTexture(BasicTexture basicTexture) {
        super(basicTexture.getWidth(), basicTexture.getHeight(), basicTexture.isOpaque());
        this.mTexture = basicTexture;
    }

    @Override
    public void draw(GLCanvas gLCanvas, int i, int i2, int i3, int i4) {
        if (isAnimating()) {
            gLCanvas.save(1);
            gLCanvas.setAlpha(getRatio());
            this.mTexture.draw(gLCanvas, i, i2, i3, i4);
            gLCanvas.restore();
        }
    }
}
