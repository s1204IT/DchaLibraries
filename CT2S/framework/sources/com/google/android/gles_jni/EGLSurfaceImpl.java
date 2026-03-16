package com.google.android.gles_jni;

import javax.microedition.khronos.egl.EGLSurface;

public class EGLSurfaceImpl extends EGLSurface {
    long mEGLSurface;
    private long mNativePixelRef;

    public EGLSurfaceImpl() {
        this.mEGLSurface = 0L;
        this.mNativePixelRef = 0L;
    }

    public EGLSurfaceImpl(long surface) {
        this.mEGLSurface = surface;
        this.mNativePixelRef = 0L;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EGLSurfaceImpl that = (EGLSurfaceImpl) o;
        return this.mEGLSurface == that.mEGLSurface;
    }

    public int hashCode() {
        int result = ((int) (this.mEGLSurface ^ (this.mEGLSurface >>> 32))) + 527;
        return result;
    }
}
