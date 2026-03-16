package com.google.android.gles_jni;

import javax.microedition.khronos.egl.EGLDisplay;

public class EGLDisplayImpl extends EGLDisplay {
    long mEGLDisplay;

    public EGLDisplayImpl(long dpy) {
        this.mEGLDisplay = dpy;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EGLDisplayImpl that = (EGLDisplayImpl) o;
        return this.mEGLDisplay == that.mEGLDisplay;
    }

    public int hashCode() {
        int result = ((int) (this.mEGLDisplay ^ (this.mEGLDisplay >>> 32))) + 527;
        return result;
    }
}
