package com.mediatek.camera.ui.preview;

import android.content.Context;
import android.opengl.GLES20;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.ui.preview.GLProducerThread;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

class GLRendererImpl implements GLProducerThread.GLRenderer {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(GLRendererImpl.class.getSimpleName());
    private int mHeight;
    private int mProgramObject;
    private ShortBuffer mTexCoords;
    private int mWidth;
    private final float[] mVerticesData = {-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, -0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.0f};
    private final short[] mTexCoordsData = {0, 1, 1, 1, 0, 0, 1, 0};
    private FloatBuffer mVertices = ByteBuffer.allocateDirect(this.mVerticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

    public GLRendererImpl(Context context) {
        this.mVertices.put(this.mVerticesData).position(0);
        this.mTexCoords = ByteBuffer.allocateDirect(this.mTexCoordsData.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        this.mTexCoords.put(this.mTexCoordsData).position(0);
    }

    public void initGL() {
        compileAndLinkProgram();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void drawFrame() {
        GLES20.glViewport(0, 0, this.mWidth, this.mHeight);
        GLES20.glClear(16384);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glDrawArrays(5, 0, 4);
    }

    private int loadShader(int i, String str) {
        int[] iArr = new int[1];
        int iGlCreateShader = GLES20.glCreateShader(i);
        if (iGlCreateShader == 0) {
            return 0;
        }
        GLES20.glShaderSource(iGlCreateShader, str);
        GLES20.glCompileShader(iGlCreateShader);
        GLES20.glGetShaderiv(iGlCreateShader, 35713, iArr, 0);
        if (iArr[0] == 0) {
            LogHelper.e(TAG, GLES20.glGetShaderInfoLog(iGlCreateShader));
            GLES20.glDeleteShader(iGlCreateShader);
            return 0;
        }
        return iGlCreateShader;
    }

    private void compileAndLinkProgram() {
        int[] iArr = new int[1];
        int iLoadShader = loadShader(35633, "attribute vec4 a_position;    \nattribute vec2 a_texCoords; \nvarying vec2 v_texCoords; \nvoid main()                  \n{                            \n   gl_Position = a_position;  \n    v_texCoords = a_texCoords; \n}                            \n");
        int iLoadShader2 = loadShader(35632, "precision mediump float;                     \nuniform sampler2D u_Texture; \nvarying vec2 v_texCoords; \nvoid main()                                  \n{                                            \n  gl_FragColor = texture2D(u_Texture, v_texCoords) ;\n}                                            \n");
        int iGlCreateProgram = GLES20.glCreateProgram();
        if (iGlCreateProgram == 0) {
            return;
        }
        GLES20.glAttachShader(iGlCreateProgram, iLoadShader);
        GLES20.glAttachShader(iGlCreateProgram, iLoadShader2);
        GLES20.glBindAttribLocation(iGlCreateProgram, 0, "a_position");
        GLES20.glBindAttribLocation(iGlCreateProgram, 1, "a_texCoords");
        GLES20.glLinkProgram(iGlCreateProgram);
        GLES20.glGetProgramiv(iGlCreateProgram, 35714, iArr, 0);
        if (iArr[0] == 0) {
            LogHelper.e(TAG, "Error linking program:");
            LogHelper.e(TAG, GLES20.glGetProgramInfoLog(iGlCreateProgram));
            GLES20.glDeleteProgram(iGlCreateProgram);
            return;
        }
        this.mProgramObject = iGlCreateProgram;
    }
}
