package com.android.gallery3d.glrenderer;

import android.opengl.GLES20;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import javax.microedition.khronos.opengles.GL11;

public class GLES20IdImpl implements GLId, MGLCanvas.Generator {
    private final int[] mTempIntArray = new int[1];

    @Override
    public int generateTexture() {
        GLES20.glGenTextures(1, this.mTempIntArray, 0);
        GLES20Canvas.checkError();
        return this.mTempIntArray[0];
    }

    @Override
    public void glGenBuffers(int i, int[] iArr, int i2) {
        GLES20.glGenBuffers(i, iArr, i2);
        GLES20Canvas.checkError();
    }

    @Override
    public void glDeleteTextures(GL11 gl11, int i, int[] iArr, int i2) {
        GLES20.glDeleteTextures(i, iArr, i2);
        GLES20Canvas.checkError();
    }

    @Override
    public void glDeleteBuffers(GL11 gl11, int i, int[] iArr, int i2) {
        GLES20.glDeleteBuffers(i, iArr, i2);
        GLES20Canvas.checkError();
    }
}
