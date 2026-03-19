package com.google.android.gles_jni;

import javax.microedition.khronos.egl.EGLConfig;

public class EGLConfigImpl extends EGLConfig {
    private long mEGLConfig;

    EGLConfigImpl(long j) {
        this.mEGLConfig = j;
    }

    long get() {
        return this.mEGLConfig;
    }
}
