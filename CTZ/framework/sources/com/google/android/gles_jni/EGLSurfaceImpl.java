package com.google.android.gles_jni;

import com.android.internal.logging.nano.MetricsProto;
import javax.microedition.khronos.egl.EGLSurface;

public class EGLSurfaceImpl extends EGLSurface {
    long mEGLSurface;

    public EGLSurfaceImpl() {
        this.mEGLSurface = 0L;
    }

    public EGLSurfaceImpl(long j) {
        this.mEGLSurface = j;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.mEGLSurface == ((EGLSurfaceImpl) obj).mEGLSurface) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + ((int) (this.mEGLSurface ^ (this.mEGLSurface >>> 32)));
    }
}
