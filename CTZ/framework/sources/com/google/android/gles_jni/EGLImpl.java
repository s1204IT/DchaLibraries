package com.google.android.gles_jni;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class EGLImpl implements EGL10 {
    private EGLContextImpl mContext = new EGLContextImpl(-1);
    private EGLDisplayImpl mDisplay = new EGLDisplayImpl(-1);
    private EGLSurfaceImpl mSurface = new EGLSurfaceImpl(-1);

    private native long _eglCreateContext(EGLDisplay eGLDisplay, EGLConfig eGLConfig, EGLContext eGLContext, int[] iArr);

    private native long _eglCreatePbufferSurface(EGLDisplay eGLDisplay, EGLConfig eGLConfig, int[] iArr);

    private native void _eglCreatePixmapSurface(EGLSurface eGLSurface, EGLDisplay eGLDisplay, EGLConfig eGLConfig, Object obj, int[] iArr);

    private native long _eglCreateWindowSurface(EGLDisplay eGLDisplay, EGLConfig eGLConfig, Object obj, int[] iArr);

    private native long _eglCreateWindowSurfaceTexture(EGLDisplay eGLDisplay, EGLConfig eGLConfig, Object obj, int[] iArr);

    private native long _eglGetCurrentContext();

    private native long _eglGetCurrentDisplay();

    private native long _eglGetCurrentSurface(int i);

    private native long _eglGetDisplay(Object obj);

    private static native void _nativeClassInit();

    public static native int getInitCount(EGLDisplay eGLDisplay);

    @Override
    public native boolean eglChooseConfig(EGLDisplay eGLDisplay, int[] iArr, EGLConfig[] eGLConfigArr, int i, int[] iArr2);

    @Override
    public native boolean eglCopyBuffers(EGLDisplay eGLDisplay, EGLSurface eGLSurface, Object obj);

    @Override
    public native boolean eglDestroyContext(EGLDisplay eGLDisplay, EGLContext eGLContext);

    @Override
    public native boolean eglDestroySurface(EGLDisplay eGLDisplay, EGLSurface eGLSurface);

    @Override
    public native boolean eglGetConfigAttrib(EGLDisplay eGLDisplay, EGLConfig eGLConfig, int i, int[] iArr);

    @Override
    public native boolean eglGetConfigs(EGLDisplay eGLDisplay, EGLConfig[] eGLConfigArr, int i, int[] iArr);

    @Override
    public native int eglGetError();

    @Override
    public native boolean eglInitialize(EGLDisplay eGLDisplay, int[] iArr);

    @Override
    public native boolean eglMakeCurrent(EGLDisplay eGLDisplay, EGLSurface eGLSurface, EGLSurface eGLSurface2, EGLContext eGLContext);

    @Override
    public native boolean eglQueryContext(EGLDisplay eGLDisplay, EGLContext eGLContext, int i, int[] iArr);

    @Override
    public native String eglQueryString(EGLDisplay eGLDisplay, int i);

    @Override
    public native boolean eglQuerySurface(EGLDisplay eGLDisplay, EGLSurface eGLSurface, int i, int[] iArr);

    @Override
    public native boolean eglReleaseThread();

    @Override
    public native boolean eglSwapBuffers(EGLDisplay eGLDisplay, EGLSurface eGLSurface);

    @Override
    public native boolean eglTerminate(EGLDisplay eGLDisplay);

    @Override
    public native boolean eglWaitGL();

    @Override
    public native boolean eglWaitNative(int i, Object obj);

    @Override
    public EGLContext eglCreateContext(EGLDisplay eGLDisplay, EGLConfig eGLConfig, EGLContext eGLContext, int[] iArr) {
        long j_eglCreateContext = _eglCreateContext(eGLDisplay, eGLConfig, eGLContext, iArr);
        if (j_eglCreateContext == 0) {
            return EGL10.EGL_NO_CONTEXT;
        }
        return new EGLContextImpl(j_eglCreateContext);
    }

    @Override
    public EGLSurface eglCreatePbufferSurface(EGLDisplay eGLDisplay, EGLConfig eGLConfig, int[] iArr) {
        long j_eglCreatePbufferSurface = _eglCreatePbufferSurface(eGLDisplay, eGLConfig, iArr);
        if (j_eglCreatePbufferSurface == 0) {
            return EGL10.EGL_NO_SURFACE;
        }
        return new EGLSurfaceImpl(j_eglCreatePbufferSurface);
    }

    @Override
    public EGLSurface eglCreatePixmapSurface(EGLDisplay eGLDisplay, EGLConfig eGLConfig, Object obj, int[] iArr) {
        EGLSurfaceImpl eGLSurfaceImpl = new EGLSurfaceImpl();
        _eglCreatePixmapSurface(eGLSurfaceImpl, eGLDisplay, eGLConfig, obj, iArr);
        if (eGLSurfaceImpl.mEGLSurface == 0) {
            return EGL10.EGL_NO_SURFACE;
        }
        return eGLSurfaceImpl;
    }

    @Override
    public EGLSurface eglCreateWindowSurface(EGLDisplay eGLDisplay, EGLConfig eGLConfig, Object obj, int[] iArr) {
        Surface surface;
        long j_eglCreateWindowSurfaceTexture;
        if (obj instanceof SurfaceView) {
            surface = ((SurfaceView) obj).getHolder().getSurface();
        } else if (obj instanceof SurfaceHolder) {
            surface = ((SurfaceHolder) obj).getSurface();
        } else if (obj instanceof Surface) {
            surface = (Surface) obj;
        } else {
            surface = null;
        }
        if (surface != null) {
            j_eglCreateWindowSurfaceTexture = _eglCreateWindowSurface(eGLDisplay, eGLConfig, surface, iArr);
        } else if (obj instanceof SurfaceTexture) {
            j_eglCreateWindowSurfaceTexture = _eglCreateWindowSurfaceTexture(eGLDisplay, eGLConfig, obj, iArr);
        } else {
            throw new UnsupportedOperationException("eglCreateWindowSurface() can only be called with an instance of Surface, SurfaceView, SurfaceHolder or SurfaceTexture at the moment.");
        }
        if (j_eglCreateWindowSurfaceTexture == 0) {
            return EGL10.EGL_NO_SURFACE;
        }
        return new EGLSurfaceImpl(j_eglCreateWindowSurfaceTexture);
    }

    @Override
    public synchronized EGLDisplay eglGetDisplay(Object obj) {
        long j_eglGetDisplay = _eglGetDisplay(obj);
        if (j_eglGetDisplay == 0) {
            return EGL10.EGL_NO_DISPLAY;
        }
        if (this.mDisplay.mEGLDisplay != j_eglGetDisplay) {
            this.mDisplay = new EGLDisplayImpl(j_eglGetDisplay);
        }
        return this.mDisplay;
    }

    @Override
    public synchronized EGLContext eglGetCurrentContext() {
        long j_eglGetCurrentContext = _eglGetCurrentContext();
        if (j_eglGetCurrentContext == 0) {
            return EGL10.EGL_NO_CONTEXT;
        }
        if (this.mContext.mEGLContext != j_eglGetCurrentContext) {
            this.mContext = new EGLContextImpl(j_eglGetCurrentContext);
        }
        return this.mContext;
    }

    @Override
    public synchronized EGLDisplay eglGetCurrentDisplay() {
        long j_eglGetCurrentDisplay = _eglGetCurrentDisplay();
        if (j_eglGetCurrentDisplay == 0) {
            return EGL10.EGL_NO_DISPLAY;
        }
        if (this.mDisplay.mEGLDisplay != j_eglGetCurrentDisplay) {
            this.mDisplay = new EGLDisplayImpl(j_eglGetCurrentDisplay);
        }
        return this.mDisplay;
    }

    @Override
    public synchronized EGLSurface eglGetCurrentSurface(int i) {
        long j_eglGetCurrentSurface = _eglGetCurrentSurface(i);
        if (j_eglGetCurrentSurface == 0) {
            return EGL10.EGL_NO_SURFACE;
        }
        if (this.mSurface.mEGLSurface != j_eglGetCurrentSurface) {
            this.mSurface = new EGLSurfaceImpl(j_eglGetCurrentSurface);
        }
        return this.mSurface;
    }

    static {
        _nativeClassInit();
    }
}
