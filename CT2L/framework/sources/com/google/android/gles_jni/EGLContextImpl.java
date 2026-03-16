package com.google.android.gles_jni;

import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL;

public class EGLContextImpl extends EGLContext {
    long mEGLContext;
    private GLImpl mGLContext = new GLImpl();

    public EGLContextImpl(long ctx) {
        this.mEGLContext = ctx;
    }

    @Override
    public GL getGL() {
        return this.mGLContext;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EGLContextImpl that = (EGLContextImpl) o;
        return this.mEGLContext == that.mEGLContext;
    }

    public int hashCode() {
        int result = ((int) (this.mEGLContext ^ (this.mEGLContext >>> 32))) + 527;
        return result;
    }
}
