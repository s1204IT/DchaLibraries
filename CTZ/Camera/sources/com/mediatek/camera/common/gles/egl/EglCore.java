package com.mediatek.camera.common.gles.egl;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

@TargetApi(18)
public final class EglCore {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(EglCore.class.getSimpleName());
    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;
    private int mGlVersion = -1;
    private int mOutputPixelFormat = -1;

    public EglCore() {
        init(null, 0, null);
    }

    public void release() {
        if (this.mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(this.mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(this.mEGLDisplay, this.mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(this.mEGLDisplay);
        }
        this.mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        this.mEGLContext = EGL14.EGL_NO_CONTEXT;
        this.mEGLConfig = null;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                LogHelper.w(TAG, "EglCore was not explicitly released -- state my be leaked!!!");
                release();
            }
        } finally {
            super.finalize();
        }
    }

    public void releaseEglSurface(EGLSurface eGLSurface) {
        if (eGLSurface != null) {
            EGL14.eglDestroySurface(this.mEGLDisplay, eGLSurface);
        }
    }

    public EGLSurface createWindowSurface(Object obj) {
        if (!(obj instanceof Surface) && !(obj instanceof SurfaceTexture)) {
            throw new RuntimeException("invalid surface: " + obj);
        }
        EGLSurface eGLSurfaceEglCreateWindowSurface = EGL14.eglCreateWindowSurface(this.mEGLDisplay, this.mEGLConfig, obj, new int[]{12344}, 0);
        EglUtil.checkEglError("[createWindowSurface] eglCreateWindowSurface");
        if (eGLSurfaceEglCreateWindowSurface == null) {
            throw new RuntimeException("[createWindowSurface] surface was null");
        }
        return eGLSurfaceEglCreateWindowSurface;
    }

    public void makeCurrent(EGLSurface eGLSurface) {
        if (this.mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            LogHelper.w(TAG, "[makeCurrent] NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(this.mEGLDisplay, eGLSurface, eGLSurface, this.mEGLContext)) {
            throw new RuntimeException("[makeCurrent] eglMakeCurrent failed");
        }
    }

    public void makeNothingCurrent() {
        if (!EGL14.eglMakeCurrent(this.mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    public boolean swapBuffers(EGLSurface eGLSurface) {
        return EGL14.eglSwapBuffers(this.mEGLDisplay, eGLSurface);
    }

    private void init(EGLContext eGLContext, int i, int[] iArr) {
        LogHelper.d(TAG, "[init]+");
        if (this.mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("[init] EGL already set up");
        }
        if (eGLContext == null) {
            eGLContext = EGL14.EGL_NO_CONTEXT;
        }
        this.mEGLDisplay = EGL14.eglGetDisplay(0);
        if (this.mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("[init] unable to get EGL14 display");
        }
        int[] iArr2 = new int[2];
        if (!EGL14.eglInitialize(this.mEGLDisplay, iArr2, 0, iArr2, 1)) {
            this.mEGLDisplay = null;
            throw new RuntimeException("[init] unable to initialize EGL14");
        }
        EglConfigSelector eglConfigSelector = new EglConfigSelector();
        if (iArr != null) {
            eglConfigSelector.setSupportedFormats(iArr);
        }
        this.mEGLConfig = eglConfigSelector.chooseConfigEGL14(this.mEGLDisplay, (i & 1) != 0);
        this.mOutputPixelFormat = eglConfigSelector.getSelectedPixelFormat();
        this.mEGLContext = null;
        if ((i & 2) != 0) {
            this.mGlVersion = 3;
            this.mEGLContext = EGL14.eglCreateContext(this.mEGLDisplay, this.mEGLConfig, eGLContext, new int[]{12440, 3, 12344}, 0);
            if (EGL14.eglGetError() != 12288) {
                LogHelper.i(TAG, "[init] GLES 3.x not available");
                this.mEGLContext = null;
            }
        }
        if (this.mEGLContext == null) {
            this.mGlVersion = 2;
            this.mEGLContext = EGL14.eglCreateContext(this.mEGLDisplay, this.mEGLConfig, eGLContext, new int[]{12440, 2, 12344}, 0);
        }
        EglUtil.checkEglError("[init] eglCreateContext");
        if (this.mEGLContext == null) {
            throw new RuntimeException("[init] null context");
        }
        LogHelper.d(TAG, "[init]-");
    }
}
