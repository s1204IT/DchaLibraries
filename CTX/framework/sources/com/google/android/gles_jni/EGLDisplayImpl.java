package com.google.android.gles_jni;

import com.android.internal.logging.nano.MetricsProto;
import javax.microedition.khronos.egl.EGLDisplay;

public class EGLDisplayImpl extends EGLDisplay {
    long mEGLDisplay;

    public EGLDisplayImpl(long j) {
        this.mEGLDisplay = j;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.mEGLDisplay == ((EGLDisplayImpl) obj).mEGLDisplay) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + ((int) (this.mEGLDisplay ^ (this.mEGLDisplay >>> 32)));
    }
}
