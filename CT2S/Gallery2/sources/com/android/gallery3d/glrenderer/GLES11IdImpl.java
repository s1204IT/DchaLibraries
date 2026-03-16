package com.android.gallery3d.glrenderer;

import javax.microedition.khronos.opengles.GL11;

public class GLES11IdImpl implements GLId {
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
    public void glGenBuffers(int n, int[] buffers, int offset) {
        synchronized (sLock) {
            int n2 = n;
            while (true) {
                int n3 = n2 - 1;
                if (n2 > 0) {
                    int i = offset + n3;
                    int i2 = sNextId;
                    sNextId = i2 + 1;
                    buffers[i] = i2;
                    n2 = n3;
                }
            }
        }
    }

    @Override
    public void glDeleteTextures(GL11 gl, int n, int[] textures, int offset) {
        synchronized (sLock) {
            gl.glDeleteTextures(n, textures, offset);
        }
    }

    @Override
    public void glDeleteBuffers(GL11 gl, int n, int[] buffers, int offset) {
        synchronized (sLock) {
            gl.glDeleteBuffers(n, buffers, offset);
        }
    }
}
