package com.mediatek.camera.common.gles.egl;

import android.annotation.TargetApi;
import android.opengl.EGL14;

public class EglUtil {
    @TargetApi(17)
    public static void checkEglError(String str) {
        int iEglGetError = EGL14.eglGetError();
        if (iEglGetError != 12288) {
            throw new RuntimeException(str + "eglGetError:0x" + Integer.toHexString(iEglGetError));
        }
    }
}
