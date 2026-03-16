package com.android.gallery3d.glrenderer;

public class FadeOutTexture extends FadeTexture {
    private final BasicTexture mTexture;

    public FadeOutTexture(BasicTexture texture) {
        super(texture.getWidth(), texture.getHeight(), texture.isOpaque());
        this.mTexture = texture;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        if (isAnimating()) {
            canvas.save(1);
            canvas.setAlpha(getRatio());
            this.mTexture.draw(canvas, x, y, w, h);
            canvas.restore();
        }
    }
}
