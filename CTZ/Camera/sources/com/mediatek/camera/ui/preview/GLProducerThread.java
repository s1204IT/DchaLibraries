package com.mediatek.camera.ui.preview;

import android.annotation.TargetApi;
import android.view.Surface;
import com.mediatek.camera.common.gles.egl.EglCore;
import com.mediatek.camera.common.gles.egl.EglSurfaceBase;

@TargetApi(18)
class GLProducerThread extends Thread {
    private EglCore mEglCore;
    private EglSurfaceBase mEglSurfaceBase = null;
    private GLRenderer mRenderer;
    private Surface mSurface;
    private Object mSyncLock;

    interface GLRenderer {
        void drawFrame();
    }

    GLProducerThread(Surface surface, GLRenderer gLRenderer, Object obj) {
        this.mSurface = surface;
        this.mRenderer = gLRenderer;
        this.mSyncLock = obj;
    }

    @Override
    public void run() {
        try {
            try {
                initGL();
                if (this.mRenderer != null) {
                    ((GLRendererImpl) this.mRenderer).initGL();
                }
                if (this.mRenderer != null) {
                    this.mRenderer.drawFrame();
                }
                this.mEglSurfaceBase.swapBuffers();
                destroyGL();
                synchronized (this.mSyncLock) {
                    this.mSyncLock.notifyAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
                destroyGL();
                synchronized (this.mSyncLock) {
                    this.mSyncLock.notifyAll();
                }
            }
        } catch (Throwable th) {
            destroyGL();
            synchronized (this.mSyncLock) {
                this.mSyncLock.notifyAll();
                throw th;
            }
        }
    }

    private void initGL() {
        this.mEglCore = new EglCore();
        this.mEglSurfaceBase = new EglSurfaceBase(this.mEglCore);
        this.mEglSurfaceBase.createWindowSurface(this.mSurface);
        this.mEglSurfaceBase.makeCurrent();
    }

    private void destroyGL() {
        this.mEglSurfaceBase.makeNothingCurrent();
        this.mEglSurfaceBase.releaseEglSurface();
        this.mEglCore.release();
    }
}
