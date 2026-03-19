package com.mediatek.gallerybasic.gl;

import com.mediatek.gallerybasic.util.Log;

public class MRawTexture extends MBasicTexture {
    private static final String TAG = "MtkGallery2/MRawTexture";
    private boolean mIsFlipped;
    private final boolean mOpaque;

    public MRawTexture(int i, int i2, boolean z) {
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

    public void setIsFlippedVertically(boolean z) {
        this.mIsFlipped = z;
    }

    protected void prepare(MGLCanvas mGLCanvas) {
        this.mId = mGLCanvas.generateTexture();
        mGLCanvas.initializeTextureSize(this, 6408, 5121);
        mGLCanvas.setTextureParameters(this);
        this.mState = 1;
        setAssociatedCanvas(mGLCanvas);
    }

    @Override
    protected boolean onBind(MGLCanvas mGLCanvas) {
        if (isLoaded()) {
            return true;
        }
        Log.w(TAG, "<onBind> lost the content due to context change");
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
