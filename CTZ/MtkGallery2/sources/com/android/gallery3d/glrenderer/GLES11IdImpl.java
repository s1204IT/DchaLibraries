package com.android.gallery3d.glrenderer;

import com.mediatek.gallerybasic.gl.MGLCanvas;
import javax.microedition.khronos.opengles.GL11;

public class GLES11IdImpl implements GLId, MGLCanvas.Generator {
    private static int sNextId = 1;
    private static Object sLock = new Object();

    @Override
    public int generateTexture() {
        int i;
        synchronized (sLock) {
            i = sNextId;
            sNextId = i + 1;
        }
        return i;
    }

    @Override
    public void glGenBuffers(int i, int[] iArr, int i2) {
        synchronized (sLock) {
            while (true) {
                int i3 = i - 1;
                if (i > 0) {
                    int i4 = i2 + i3;
                    try {
                        int i5 = sNextId;
                        sNextId = i5 + 1;
                        iArr[i4] = i5;
                        i = i3;
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                throw th;
            }
        }
    }

    @Override
    public void glDeleteTextures(GL11 gl11, int i, int[] iArr, int i2) {
        synchronized (sLock) {
            gl11.glDeleteTextures(i, iArr, i2);
        }
    }

    @Override
    public void glDeleteBuffers(GL11 gl11, int i, int[] iArr, int i2) {
        synchronized (sLock) {
            gl11.glDeleteBuffers(i, iArr, i2);
        }
    }
}
