package com.mediatek.gallerybasic.gl;

public class MExtTexture extends MBasicTexture {
    private int mTarget;

    public MExtTexture(MGLCanvas mGLCanvas, int i) {
        this.mId = mGLCanvas.generateTexture();
        this.mTarget = i;
    }

    public MExtTexture(MGLCanvas mGLCanvas, int i, boolean z) {
        super(mGLCanvas, 0, 1);
        this.mId = mGLCanvas.generateTexture();
        this.mTarget = i;
    }

    private void uploadToCanvas(MGLCanvas mGLCanvas) {
        mGLCanvas.setTextureParameters(this);
        setAssociatedCanvas(mGLCanvas);
        this.mState = 1;
    }

    @Override
    protected boolean onBind(MGLCanvas mGLCanvas) {
        if (!isLoaded()) {
            uploadToCanvas(mGLCanvas);
            return true;
        }
        return true;
    }

    @Override
    public int getTarget() {
        return this.mTarget;
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public void yield() {
    }
}
