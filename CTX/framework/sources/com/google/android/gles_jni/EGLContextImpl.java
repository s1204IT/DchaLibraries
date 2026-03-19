package com.google.android.gles_jni;

import com.android.internal.logging.nano.MetricsProto;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL;

public class EGLContextImpl extends EGLContext {
    long mEGLContext;
    private GLImpl mGLContext = new GLImpl();

    public EGLContextImpl(long j) {
        this.mEGLContext = j;
    }

    @Override
    public GL getGL() {
        return this.mGLContext;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.mEGLContext == ((EGLContextImpl) obj).mEGLContext) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + ((int) (this.mEGLContext ^ (this.mEGLContext >>> 32)));
    }
}
