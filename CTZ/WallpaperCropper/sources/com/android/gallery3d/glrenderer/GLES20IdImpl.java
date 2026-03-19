package com.android.gallery3d.glrenderer;

import android.opengl.GLES20;

public class GLES20IdImpl implements GLId {
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
}
