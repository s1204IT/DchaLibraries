package com.mediatek.camera.common.mode.photo.heif;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.nio.Buffer;
import java.nio.FloatBuffer;

public class Texture2dProgram {
    public static final float[] IDENTITY_MATRIX = new float[16];
    public static final float[] V_FLIP_MATRIX;
    private int mProgramHandle;
    private int mProgramType;
    private int mTextureTarget;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;

    static {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
        V_FLIP_MATRIX = new float[16];
        Matrix.setIdentityM(V_FLIP_MATRIX, 0);
        Matrix.translateM(V_FLIP_MATRIX, 0, 0.0f, 1.0f, 0.0f);
        Matrix.scaleM(V_FLIP_MATRIX, 0, 1.0f, -1.0f, 1.0f);
    }

    public Texture2dProgram(int i) {
        this.mProgramType = i;
        switch (i) {
            case 0:
                this.mTextureTarget = 3553;
                this.mProgramHandle = createProgram("uniform mat4 uMVPMatrix;\nuniform mat4 uTexMatrix;\nattribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n    gl_Position = uMVPMatrix * aPosition;\n    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n}\n", "precision mediump float;\nvarying vec2 vTextureCoord;\nuniform sampler2D sTexture;\nvoid main() {\n    gl_FragColor = texture2D(sTexture, vTextureCoord);\n}\n");
                break;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                this.mTextureTarget = 36197;
                this.mProgramHandle = createProgram("uniform mat4 uMVPMatrix;\nuniform mat4 uTexMatrix;\nattribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n    gl_Position = uMVPMatrix * aPosition;\n    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n}\n", "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform samplerExternalOES sTexture;\nvoid main() {\n    gl_FragColor = texture2D(sTexture, vTextureCoord);\n}\n");
                break;
            default:
                throw new RuntimeException("Unhandled type " + i);
        }
        if (this.mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        Log.d("Texture2dProgram", "Created program " + this.mProgramHandle + " (" + i + ")");
        this.maPositionLoc = GLES20.glGetAttribLocation(this.mProgramHandle, "aPosition");
        checkLocation(this.maPositionLoc, "aPosition");
        this.maTextureCoordLoc = GLES20.glGetAttribLocation(this.mProgramHandle, "aTextureCoord");
        checkLocation(this.maTextureCoordLoc, "aTextureCoord");
        this.muMVPMatrixLoc = GLES20.glGetUniformLocation(this.mProgramHandle, "uMVPMatrix");
        checkLocation(this.muMVPMatrixLoc, "uMVPMatrix");
        this.muTexMatrixLoc = GLES20.glGetUniformLocation(this.mProgramHandle, "uTexMatrix");
        checkLocation(this.muTexMatrixLoc, "uTexMatrix");
    }

    public void release() {
        Log.d("Texture2dProgram", "deleting program " + this.mProgramHandle);
        GLES20.glDeleteProgram(this.mProgramHandle);
        this.mProgramHandle = -1;
    }

    public int createTextureObject() {
        int[] iArr = new int[1];
        GLES20.glGenTextures(1, iArr, 0);
        checkGlError("glGenTextures");
        int i = iArr[0];
        GLES20.glBindTexture(this.mTextureTarget, i);
        checkGlError("glBindTexture " + i);
        GLES20.glTexParameterf(this.mTextureTarget, 10241, 9728.0f);
        GLES20.glTexParameterf(this.mTextureTarget, 10240, this.mTextureTarget != 3553 ? 9729.0f : 9728.0f);
        GLES20.glTexParameteri(this.mTextureTarget, 10242, 33071);
        GLES20.glTexParameteri(this.mTextureTarget, 10243, 33071);
        checkGlError("glTexParameter");
        return i;
    }

    public void draw(float[] fArr, FloatBuffer floatBuffer, int i, int i2, int i3, int i4, float[] fArr2, FloatBuffer floatBuffer2, int i5, int i6) {
        checkGlError("draw start");
        GLES20.glUseProgram(this.mProgramHandle);
        checkGlError("glUseProgram");
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(this.mTextureTarget, i5);
        GLES20.glUniformMatrix4fv(this.muMVPMatrixLoc, 1, false, fArr, 0);
        checkGlError("glUniformMatrix4fv");
        GLES20.glUniformMatrix4fv(this.muTexMatrixLoc, 1, false, fArr2, 0);
        checkGlError("glUniformMatrix4fv");
        GLES20.glEnableVertexAttribArray(this.maPositionLoc);
        checkGlError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(this.maPositionLoc, i3, 5126, false, i4, (Buffer) floatBuffer);
        checkGlError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(this.maTextureCoordLoc);
        checkGlError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(this.maTextureCoordLoc, 2, 5126, false, i6, (Buffer) floatBuffer2);
        checkGlError("glVertexAttribPointer");
        GLES20.glDrawArrays(5, i, i2);
        checkGlError("glDrawArrays");
        GLES20.glDisableVertexAttribArray(this.maPositionLoc);
        GLES20.glDisableVertexAttribArray(this.maTextureCoordLoc);
        GLES20.glBindTexture(this.mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }

    public static int createProgram(String str, String str2) {
        int iLoadShader;
        int iLoadShader2 = loadShader(35633, str);
        if (iLoadShader2 == 0 || (iLoadShader = loadShader(35632, str2)) == 0) {
            return 0;
        }
        int iGlCreateProgram = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (iGlCreateProgram == 0) {
            Log.e("Texture2dProgram", "Could not create program");
        }
        GLES20.glAttachShader(iGlCreateProgram, iLoadShader2);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(iGlCreateProgram, iLoadShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(iGlCreateProgram);
        int[] iArr = new int[1];
        GLES20.glGetProgramiv(iGlCreateProgram, 35714, iArr, 0);
        if (iArr[0] != 1) {
            Log.e("Texture2dProgram", "Could not link program: ");
            Log.e("Texture2dProgram", GLES20.glGetProgramInfoLog(iGlCreateProgram));
            GLES20.glDeleteProgram(iGlCreateProgram);
            return 0;
        }
        return iGlCreateProgram;
    }

    public static int loadShader(int i, String str) {
        int iGlCreateShader = GLES20.glCreateShader(i);
        checkGlError("glCreateShader type=" + i);
        GLES20.glShaderSource(iGlCreateShader, str);
        GLES20.glCompileShader(iGlCreateShader);
        int[] iArr = new int[1];
        GLES20.glGetShaderiv(iGlCreateShader, 35713, iArr, 0);
        if (iArr[0] != 0) {
            return iGlCreateShader;
        }
        Log.e("Texture2dProgram", "Could not compile shader " + i + ":");
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        sb.append(GLES20.glGetShaderInfoLog(iGlCreateShader));
        Log.e("Texture2dProgram", sb.toString());
        GLES20.glDeleteShader(iGlCreateShader);
        return 0;
    }

    public static void checkLocation(int i, String str) {
        if (i < 0) {
            throw new RuntimeException("Unable to locate '" + str + "' in program");
        }
    }

    public static void checkGlError(String str) {
        int iGlGetError = GLES20.glGetError();
        if (iGlGetError != 0) {
            String str2 = str + ": glError 0x" + Integer.toHexString(iGlGetError);
            Log.e("Texture2dProgram", str2);
            throw new RuntimeException(str2);
        }
    }
}
