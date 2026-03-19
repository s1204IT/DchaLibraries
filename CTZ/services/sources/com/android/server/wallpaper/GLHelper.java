package com.android.server.wallpaper;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemProperties;
import android.util.Log;

class GLHelper {
    private static final String TAG = GLHelper.class.getSimpleName();
    private static final int sMaxTextureSize;

    GLHelper() {
    }

    static {
        int iRetrieveTextureSizeFromGL = SystemProperties.getInt("sys.max_texture_size", 0);
        if (iRetrieveTextureSizeFromGL <= 0) {
            iRetrieveTextureSizeFromGL = retrieveTextureSizeFromGL();
        }
        sMaxTextureSize = iRetrieveTextureSizeFromGL;
    }

    private static int retrieveTextureSizeFromGL() {
        try {
            EGLDisplay eGLDisplayEglGetDisplay = EGL14.eglGetDisplay(0);
            if (eGLDisplayEglGetDisplay == null || eGLDisplayEglGetDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            }
            EGLConfig eGLConfig = null;
            if (!EGL14.eglInitialize(eGLDisplayEglGetDisplay, null, 0, null, 1)) {
                throw new RuntimeException("eglInitialize failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            }
            int[] iArr = new int[1];
            EGLConfig[] eGLConfigArr = new EGLConfig[1];
            if (!EGL14.eglChooseConfig(eGLDisplayEglGetDisplay, new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12327, 12344, 12344}, 0, eGLConfigArr, 0, 1, iArr, 0)) {
                throw new RuntimeException("eglChooseConfig failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            }
            if (iArr[0] > 0) {
                eGLConfig = eGLConfigArr[0];
            }
            if (eGLConfig == null) {
                throw new RuntimeException("eglConfig not initialized!");
            }
            EGLContext eGLContextEglCreateContext = EGL14.eglCreateContext(eGLDisplayEglGetDisplay, eGLConfig, EGL14.EGL_NO_CONTEXT, new int[]{12440, 2, 12344}, 0);
            if (eGLContextEglCreateContext == null || eGLContextEglCreateContext == EGL14.EGL_NO_CONTEXT) {
                throw new RuntimeException("eglCreateContext failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            }
            EGLSurface eGLSurfaceEglCreatePbufferSurface = EGL14.eglCreatePbufferSurface(eGLDisplayEglGetDisplay, eGLConfig, new int[]{12375, 1, 12374, 1, 12344}, 0);
            EGL14.eglMakeCurrent(eGLDisplayEglGetDisplay, eGLSurfaceEglCreatePbufferSurface, eGLSurfaceEglCreatePbufferSurface, eGLContextEglCreateContext);
            int[] iArr2 = new int[1];
            GLES20.glGetIntegerv(3379, iArr2, 0);
            EGL14.eglMakeCurrent(eGLDisplayEglGetDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eGLDisplayEglGetDisplay, eGLSurfaceEglCreatePbufferSurface);
            EGL14.eglDestroyContext(eGLDisplayEglGetDisplay, eGLContextEglCreateContext);
            EGL14.eglTerminate(eGLDisplayEglGetDisplay);
            return iArr2[0];
        } catch (RuntimeException e) {
            Log.w(TAG, "Retrieve from GL failed", e);
            return Integer.MAX_VALUE;
        }
    }

    static int getMaxTextureSize() {
        return sMaxTextureSize;
    }
}
