package com.mediatek.gallery3d.adapter;

import com.android.gallery3d.ui.GLView;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MGLView;

public class MGLRootView implements MGLView {
    GLView mRootView;

    public MGLRootView(GLView gLView) {
        this.mRootView = gLView;
    }

    @Override
    public void doDraw(MGLCanvas mGLCanvas, int i, int i2) {
    }

    @Override
    public void doLayout(boolean z, int i, int i2, int i3, int i4) {
    }

    @Override
    public Object getComponent() {
        return null;
    }

    @Override
    public void addComponent(Object obj) {
        GLView gLView;
        if ((obj instanceof MGLView) && (gLView = (GLView) ((MGLView) obj).getComponent()) != null) {
            this.mRootView.addComponent(gLView);
        }
    }

    @Override
    public void removeComponent(Object obj) {
        if (obj instanceof MGLView) {
            MGLView mGLView = (MGLView) obj;
            if (mGLView.getComponent() != null) {
                this.mRootView.removeComponent((GLView) mGLView.getComponent());
            }
        }
    }
}
