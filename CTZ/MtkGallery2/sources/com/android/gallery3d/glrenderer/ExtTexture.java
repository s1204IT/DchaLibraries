package com.android.gallery3d.glrenderer;

import com.android.gallery3d.common.ApiHelper;

public class ExtTexture extends BasicTexture {
    private int mTarget;

    public ExtTexture(GLCanvas gLCanvas, int i) {
        this.mId = gLCanvas.getGLId().generateTexture();
        this.mTarget = i;
    }

    public ExtTexture(int i) {
        this.mId = (ApiHelper.HAS_GLES20_REQUIRED ? GLES20Canvas.getCameraGLId() : GLES11Canvas.getCameraGLId()).generateTexture();
        this.mTarget = i;
    }

    private void uploadToCanvas(GLCanvas gLCanvas) {
        gLCanvas.setTextureParameters(this);
        setAssociatedCanvas(gLCanvas);
        this.mState = 1;
    }

    @Override
    protected boolean onBind(GLCanvas gLCanvas) {
        if (!isLoaded()) {
            uploadToCanvas(gLCanvas);
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
