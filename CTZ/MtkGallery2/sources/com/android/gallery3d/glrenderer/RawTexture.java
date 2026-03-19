package com.android.gallery3d.glrenderer;

import com.mediatek.gallery3d.util.Log;

public class RawTexture extends BasicTexture {
    private boolean mIsFlipped;
    private final boolean mOpaque;

    public RawTexture(int i, int i2, boolean z) {
        this.mOpaque = z;
        setSize(i, i2);
    }

    @Override
    public boolean isOpaque() {
        return this.mOpaque;
    }

    @Override
    public boolean isFlippedVertically() {
        return this.mIsFlipped;
    }

    protected void prepare(GLCanvas gLCanvas) {
        this.mId = gLCanvas.getGLId().generateTexture();
        gLCanvas.initializeTextureSize(this, 6408, 5121);
        gLCanvas.setTextureParameters(this);
        this.mState = 1;
        setAssociatedCanvas(gLCanvas);
    }

    @Override
    protected boolean onBind(GLCanvas gLCanvas) {
        if (isLoaded()) {
            return true;
        }
        Log.w("Gallery2/RawTexture", "lost the content due to context change");
        return false;
    }

    @Override
    public void yield() {
    }

    @Override
    protected int getTarget() {
        return 3553;
    }
}
