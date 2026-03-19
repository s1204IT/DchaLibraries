package com.android.dreams.basic;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

final class ColorsGLRenderer implements Choreographer.FrameCallback {
    static final String TAG = ColorsGLRenderer.class.getSimpleName();
    private EGL10 mEgl;
    private EGLContext mEglContext;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private int mHeight;
    private Square mSquare;
    private final Surface mSurface;
    private int mWidth;
    private int mFrameNum = 0;
    private final Choreographer mChoreographer = Choreographer.getInstance();

    public ColorsGLRenderer(Surface surface, int i, int i2) {
        this.mSurface = surface;
        this.mWidth = i;
        this.mHeight = i2;
    }

    public void start() {
        initGL();
        this.mSquare = new Square();
        this.mFrameNum = 0;
        this.mChoreographer.postFrameCallback(this);
    }

    public void stop() {
        this.mChoreographer.removeFrameCallback(this);
        this.mSquare = null;
        finishGL();
    }

    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
    }

    @Override
    public void doFrame(long j) {
        this.mFrameNum++;
        if (this.mFrameNum == 1) {
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        }
        checkCurrent();
        GLES20.glViewport(0, 0, this.mWidth, this.mHeight);
        GLES20.glClear(16384);
        checkGlError();
        this.mSquare.draw();
        if (!this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface)) {
            throw new RuntimeException("Cannot swap buffers");
        }
        checkEglError();
        this.mChoreographer.postFrameCallback(this);
    }

    private void checkCurrent() {
        if ((!this.mEglContext.equals(this.mEgl.eglGetCurrentContext()) || !this.mEglSurface.equals(this.mEgl.eglGetCurrentSurface(12377))) && !this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }
    }

    private void initGL() {
        this.mEgl = (EGL10) EGLContext.getEGL();
        this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }
        if (!this.mEgl.eglInitialize(this.mEglDisplay, new int[2])) {
            throw new RuntimeException("eglInitialize failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }
        EGLConfig eGLConfigChooseEglConfig = chooseEglConfig();
        if (eGLConfigChooseEglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }
        this.mEglContext = createContext(this.mEgl, this.mEglDisplay, eGLConfigChooseEglConfig);
        this.mEglSurface = this.mEgl.eglCreateWindowSurface(this.mEglDisplay, eGLConfigChooseEglConfig, this.mSurface, null);
        if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
            int iEglGetError = this.mEgl.eglGetError();
            if (iEglGetError == 12299) {
                Log.e(TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                return;
            }
            throw new RuntimeException("createWindowSurface failed " + GLUtils.getEGLErrorString(iEglGetError));
        }
        if (!this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }
    }

    private void finishGL() {
        this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
        this.mEgl.eglDestroySurface(this.mEglDisplay, this.mEglSurface);
    }

    private static EGLContext createContext(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig eGLConfig) {
        return egl10.eglCreateContext(eGLDisplay, eGLConfig, EGL10.EGL_NO_CONTEXT, new int[]{12440, 2, 12344});
    }

    private EGLConfig chooseEglConfig() {
        int[] iArr = new int[1];
        EGLConfig[] eGLConfigArr = new EGLConfig[1];
        if (!this.mEgl.eglChooseConfig(this.mEglDisplay, getConfig(), eGLConfigArr, 1, iArr)) {
            throw new IllegalArgumentException("eglChooseConfig failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }
        if (iArr[0] > 0) {
            return eGLConfigArr[0];
        }
        return null;
    }

    private static int[] getConfig() {
        return new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12344};
    }

    private static int buildProgram(String str, String str2) {
        int iBuildShader;
        int iBuildShader2 = buildShader(str, 35633);
        if (iBuildShader2 == 0 || (iBuildShader = buildShader(str2, 35632)) == 0) {
            return 0;
        }
        int iGlCreateProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(iGlCreateProgram, iBuildShader2);
        checkGlError();
        GLES20.glAttachShader(iGlCreateProgram, iBuildShader);
        checkGlError();
        GLES20.glLinkProgram(iGlCreateProgram);
        checkGlError();
        int[] iArr = new int[1];
        GLES20.glGetProgramiv(iGlCreateProgram, 35714, iArr, 0);
        if (iArr[0] != 1) {
            String strGlGetProgramInfoLog = GLES20.glGetProgramInfoLog(iGlCreateProgram);
            Log.d(TAG, "Error while linking program:\n" + strGlGetProgramInfoLog);
            GLES20.glDeleteShader(iBuildShader2);
            GLES20.glDeleteShader(iBuildShader);
            GLES20.glDeleteProgram(iGlCreateProgram);
            return 0;
        }
        return iGlCreateProgram;
    }

    private static int buildShader(String str, int i) {
        int iGlCreateShader = GLES20.glCreateShader(i);
        GLES20.glShaderSource(iGlCreateShader, str);
        checkGlError();
        GLES20.glCompileShader(iGlCreateShader);
        checkGlError();
        int[] iArr = new int[1];
        GLES20.glGetShaderiv(iGlCreateShader, 35713, iArr, 0);
        if (iArr[0] != 1) {
            String strGlGetShaderInfoLog = GLES20.glGetShaderInfoLog(iGlCreateShader);
            Log.d(TAG, "Error while compiling shader:\n" + strGlGetShaderInfoLog);
            GLES20.glDeleteShader(iGlCreateShader);
            return 0;
        }
        return iGlCreateShader;
    }

    private void checkEglError() {
        int iEglGetError = this.mEgl.eglGetError();
        if (iEglGetError != 12288) {
            Log.w(TAG, "EGL error = 0x" + Integer.toHexString(iEglGetError));
        }
    }

    private static void checkGlError() {
        checkGlError("");
    }

    private static void checkGlError(String str) {
        int iGlGetError = GLES20.glGetError();
        if (iGlGetError != 0) {
            Log.w(TAG, "GL error: (" + str + ") = 0x" + Integer.toHexString(iGlGetError));
        }
    }

    private static final class Square {
        private final FloatBuffer colorBuffer;
        private int cornerRotation;
        private ShortBuffer drawListBuffer;
        private int mColorHandle;
        private int mPositionHandle;
        private final int mProgram;
        private final FloatBuffer vertexBuffer;
        private final String vertexShaderCode = "attribute vec4 a_position;attribute vec4 a_color;varying vec4 v_color;void main() {  gl_Position = a_position;  v_color = a_color;}";
        private final String fragmentShaderCode = "precision mediump float;varying vec4 v_color;void main() {  gl_FragColor = v_color;}";
        final int COORDS_PER_VERTEX = 3;
        float[] squareCoords = {-1.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f};
        private short[] drawOrder = {0, 1, 2, 0, 2, 3};
        private final float[] HUES = {60.0f, 120.0f, 343.0f, 200.0f};
        private final int vertexCount = this.squareCoords.length / 3;
        private final int vertexStride = 12;
        private float[] cornerFrequencies = new float[this.vertexCount];
        final int COLOR_PLANES_PER_VERTEX = 4;
        private final int colorStride = 16;
        final float[] _tmphsv = new float[3];

        public Square() {
            for (int i = 0; i < this.vertexCount; i++) {
                this.cornerFrequencies[i] = 1.0f + ((float) (Math.random() * 5.0d));
            }
            this.cornerRotation = (int) (Math.random() * ((double) this.vertexCount));
            ByteBuffer byteBufferAllocateDirect = ByteBuffer.allocateDirect(this.squareCoords.length * 4);
            byteBufferAllocateDirect.order(ByteOrder.nativeOrder());
            this.vertexBuffer = byteBufferAllocateDirect.asFloatBuffer();
            this.vertexBuffer.put(this.squareCoords);
            this.vertexBuffer.position(0);
            ByteBuffer byteBufferAllocateDirect2 = ByteBuffer.allocateDirect(this.vertexCount * 16);
            byteBufferAllocateDirect2.order(ByteOrder.nativeOrder());
            this.colorBuffer = byteBufferAllocateDirect2.asFloatBuffer();
            ByteBuffer byteBufferAllocateDirect3 = ByteBuffer.allocateDirect(this.drawOrder.length * 2);
            byteBufferAllocateDirect3.order(ByteOrder.nativeOrder());
            this.drawListBuffer = byteBufferAllocateDirect3.asShortBuffer();
            this.drawListBuffer.put(this.drawOrder);
            this.drawListBuffer.position(0);
            this.mProgram = ColorsGLRenderer.buildProgram("attribute vec4 a_position;attribute vec4 a_color;varying vec4 v_color;void main() {  gl_Position = a_position;  v_color = a_color;}", "precision mediump float;varying vec4 v_color;void main() {  gl_FragColor = v_color;}");
            GLES20.glUseProgram(this.mProgram);
            ColorsGLRenderer.checkGlError("glUseProgram(" + this.mProgram + ")");
            this.mPositionHandle = GLES20.glGetAttribLocation(this.mProgram, "a_position");
            ColorsGLRenderer.checkGlError("glGetAttribLocation(a_position)");
            GLES20.glEnableVertexAttribArray(this.mPositionHandle);
            GLES20.glVertexAttribPointer(this.mPositionHandle, 3, 5126, false, 12, (Buffer) this.vertexBuffer);
            this.mColorHandle = GLES20.glGetAttribLocation(this.mProgram, "a_color");
            ColorsGLRenderer.checkGlError("glGetAttribLocation(a_color)");
            GLES20.glEnableVertexAttribArray(this.mColorHandle);
            ColorsGLRenderer.checkGlError("glEnableVertexAttribArray");
        }

        public void draw() {
            long jUptimeMillis = SystemClock.uptimeMillis();
            this.colorBuffer.clear();
            float f = jUptimeMillis / 4000.0f;
            for (int i = 0; i < this.vertexCount; i++) {
                float fSin = (float) Math.sin((6.283185307179586d * ((double) f)) / ((double) this.cornerFrequencies[i]));
                this._tmphsv[0] = this.HUES[(this.cornerRotation + i) % this.vertexCount];
                this._tmphsv[1] = 1.0f;
                this._tmphsv[2] = (fSin * 0.25f) + 0.75f;
                int iHSVToColor = Color.HSVToColor(this._tmphsv);
                this.colorBuffer.put(((16711680 & iHSVToColor) >> 16) / 255.0f);
                this.colorBuffer.put(((65280 & iHSVToColor) >> 8) / 255.0f);
                this.colorBuffer.put((iHSVToColor & 255) / 255.0f);
                this.colorBuffer.put(1.0f);
            }
            this.colorBuffer.position(0);
            GLES20.glVertexAttribPointer(this.mColorHandle, 4, 5126, false, 16, (Buffer) this.colorBuffer);
            ColorsGLRenderer.checkGlError("glVertexAttribPointer");
            GLES20.glDrawArrays(6, 0, this.vertexCount);
        }
    }
}
