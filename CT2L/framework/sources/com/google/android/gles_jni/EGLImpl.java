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
    public EGLContext eglCreateContext(EGLDisplay display, EGLConfig config, EGLContext share_context, int[] attrib_list) {
        long eglContextId = _eglCreateContext(display, config, share_context, attrib_list);
        return eglContextId == 0 ? EGL10.EGL_NO_CONTEXT : new EGLContextImpl(eglContextId);
    }

    @Override
    public EGLSurface eglCreatePbufferSurface(EGLDisplay display, EGLConfig config, int[] attrib_list) {
        long eglSurfaceId = _eglCreatePbufferSurface(display, config, attrib_list);
        return eglSurfaceId == 0 ? EGL10.EGL_NO_SURFACE : new EGLSurfaceImpl(eglSurfaceId);
    }

    @Override
    public EGLSurface eglCreatePixmapSurface(EGLDisplay display, EGLConfig config, Object native_pixmap, int[] attrib_list) {
        EGLSurfaceImpl sur = new EGLSurfaceImpl();
        _eglCreatePixmapSurface(sur, display, config, native_pixmap, attrib_list);
        if (sur.mEGLSurface == 0) {
            return EGL10.EGL_NO_SURFACE;
        }
        return sur;
    }

    @Override
    public EGLSurface eglCreateWindowSurface(EGLDisplay display, EGLConfig config, Object native_window, int[] attrib_list) {
        long eglSurfaceId;
        Surface sur = null;
        if (native_window instanceof SurfaceView) {
            SurfaceView surfaceView = (SurfaceView) native_window;
            sur = surfaceView.getHolder().getSurface();
        } else if (native_window instanceof SurfaceHolder) {
            SurfaceHolder holder = (SurfaceHolder) native_window;
            sur = holder.getSurface();
        } else if (native_window instanceof Surface) {
            sur = (Surface) native_window;
        }
        if (sur != null) {
            eglSurfaceId = _eglCreateWindowSurface(display, config, sur, attrib_list);
        } else if (native_window instanceof SurfaceTexture) {
            eglSurfaceId = _eglCreateWindowSurfaceTexture(display, config, native_window, attrib_list);
        } else {
            throw new UnsupportedOperationException("eglCreateWindowSurface() can only be called with an instance of Surface, SurfaceView, SurfaceHolder or SurfaceTexture at the moment.");
        }
        if (eglSurfaceId == 0) {
            return EGL10.EGL_NO_SURFACE;
        }
        return new EGLSurfaceImpl(eglSurfaceId);
    }

    @Override
    public synchronized EGLDisplay eglGetDisplay(Object native_display) {
        EGLDisplay eGLDisplay;
        long value = _eglGetDisplay(native_display);
        if (value == 0) {
            eGLDisplay = EGL10.EGL_NO_DISPLAY;
        } else {
            if (this.mDisplay.mEGLDisplay != value) {
                this.mDisplay = new EGLDisplayImpl(value);
            }
            eGLDisplay = this.mDisplay;
        }
        return eGLDisplay;
    }

    @Override
    public synchronized EGLContext eglGetCurrentContext() {
        EGLContext eGLContext;
        long value = _eglGetCurrentContext();
        if (value == 0) {
            eGLContext = EGL10.EGL_NO_CONTEXT;
        } else {
            if (this.mContext.mEGLContext != value) {
                this.mContext = new EGLContextImpl(value);
            }
            eGLContext = this.mContext;
        }
        return eGLContext;
    }

    @Override
    public synchronized EGLDisplay eglGetCurrentDisplay() {
        EGLDisplay eGLDisplay;
        long value = _eglGetCurrentDisplay();
        if (value == 0) {
            eGLDisplay = EGL10.EGL_NO_DISPLAY;
        } else {
            if (this.mDisplay.mEGLDisplay != value) {
                this.mDisplay = new EGLDisplayImpl(value);
            }
            eGLDisplay = this.mDisplay;
        }
        return eGLDisplay;
    }

    @Override
    public synchronized EGLSurface eglGetCurrentSurface(int readdraw) {
        EGLSurface eGLSurface;
        long value = _eglGetCurrentSurface(readdraw);
        if (value == 0) {
            eGLSurface = EGL10.EGL_NO_SURFACE;
        } else {
            if (this.mSurface.mEGLSurface != value) {
                this.mSurface = new EGLSurfaceImpl(value);
            }
            eGLSurface = this.mSurface;
        }
        return eGLSurface;
    }

    static {
        _nativeClassInit();
    }
}
