package com.android.gallery3d.glrenderer;

import android.util.Log;

public class RawTexture extends BasicTexture {
    private boolean mIsFlipped;
    private final boolean mOpaque;

    @Override
    public boolean isOpaque() {
        return this.mOpaque;
    }

    @Override
    public boolean isFlippedVertically() {
        return this.mIsFlipped;
    }

    @Override
    protected boolean onBind(GLCanvas gLCanvas) {
        if (isLoaded()) {
            return true;
        }
        Log.w("RawTexture", "lost the content due to context change");
        return false;
    }

    @Override
    protected int getTarget() {
        return 3553;
    }
}
