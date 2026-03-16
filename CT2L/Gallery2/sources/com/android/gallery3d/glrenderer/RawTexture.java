package com.android.gallery3d.glrenderer;

import android.util.Log;

public class RawTexture extends BasicTexture {
    private boolean mIsFlipped;
    private final boolean mOpaque;

    public RawTexture(int width, int height, boolean opaque) {
        this.mOpaque = opaque;
        setSize(width, height);
    }

    @Override
    public boolean isOpaque() {
        return this.mOpaque;
    }

    @Override
    public boolean isFlippedVertically() {
        return this.mIsFlipped;
    }

    protected void prepare(GLCanvas canvas) {
        GLId glId = canvas.getGLId();
        this.mId = glId.generateTexture();
        canvas.initializeTextureSize(this, 6408, 5121);
        canvas.setTextureParameters(this);
        this.mState = 1;
        setAssociatedCanvas(canvas);
    }

    @Override
    protected boolean onBind(GLCanvas canvas) {
        if (isLoaded()) {
            return true;
        }
        Log.w("RawTexture", "lost the content due to context change");
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
