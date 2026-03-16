package com.android.dreams.basic;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
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
    private final SurfaceTexture mSurface;
    private int mWidth;
    private int mFrameNum = 0;
    private final Choreographer mChoreographer = Choreographer.getInstance();

    public ColorsGLRenderer(SurfaceTexture surface, int width, int height) {
        this.mSurface = surface;
        this.mWidth = width;
        this.mHeight = height;
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

    public void setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
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
        int[] version = new int[2];
        if (!this.mEgl.eglInitialize(this.mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }
        EGLConfig eglConfig = chooseEglConfig();
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }
        this.mEglContext = createContext(this.mEgl, this.mEglDisplay, eglConfig);
        this.mEglSurface = this.mEgl.eglCreateWindowSurface(this.mEglDisplay, eglConfig, this.mSurface, null);
        if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
            int error = this.mEgl.eglGetError();
            if (error == 12299) {
                Log.e(TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                return;
            }
            throw new RuntimeException("createWindowSurface failed " + GLUtils.getEGLErrorString(error));
        }
        if (!this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }
    }

    private void finishGL() {
        this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
        this.mEgl.eglDestroySurface(this.mEglDisplay, this.mEglSurface);
    }

    private static EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int[] attrib_list = {12440, 2, 12344};
        return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
    }

    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = getConfig();
        if (!this.mEgl.eglChooseConfig(this.mEglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("eglChooseConfig failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }
        if (configsCount[0] > 0) {
            return configs[0];
        }
        return null;
    }

    private static int[] getConfig() {
        return new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12344};
    }

    private static int buildProgram(String vertex, String fragment) {
        int fragmentShader;
        int vertexShader = buildShader(vertex, 35633);
        if (vertexShader != 0 && (fragmentShader = buildShader(fragment, 35632)) != 0) {
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            checkGlError();
            GLES20.glAttachShader(program, fragmentShader);
            checkGlError();
            GLES20.glLinkProgram(program);
            checkGlError();
            int[] status = new int[1];
            GLES20.glGetProgramiv(program, 35714, status, 0);
            if (status[0] != 1) {
                String error = GLES20.glGetProgramInfoLog(program);
                Log.d(TAG, "Error while linking program:\n" + error);
                GLES20.glDeleteShader(vertexShader);
                GLES20.glDeleteShader(fragmentShader);
                GLES20.glDeleteProgram(program);
                return 0;
            }
            return program;
        }
        return 0;
    }

    private static int buildShader(String source, int type) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        checkGlError();
        GLES20.glCompileShader(shader);
        checkGlError();
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, 35713, status, 0);
        if (status[0] != 1) {
            String error = GLES20.glGetShaderInfoLog(shader);
            Log.d(TAG, "Error while compiling shader:\n" + error);
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void checkEglError() {
        int error = this.mEgl.eglGetError();
        if (error != 12288) {
            Log.w(TAG, "EGL error = 0x" + Integer.toHexString(error));
        }
    }

    private static void checkGlError() {
        checkGlError("");
    }

    private static void checkGlError(String what) {
        int error = GLES20.glGetError();
        if (error != 0) {
            Log.w(TAG, "GL error: (" + what + ") = 0x" + Integer.toHexString(error));
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
            ByteBuffer bb = ByteBuffer.allocateDirect(this.squareCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            this.vertexBuffer = bb.asFloatBuffer();
            this.vertexBuffer.put(this.squareCoords);
            this.vertexBuffer.position(0);
            ByteBuffer bb2 = ByteBuffer.allocateDirect(this.vertexCount * 16);
            bb2.order(ByteOrder.nativeOrder());
            this.colorBuffer = bb2.asFloatBuffer();
            ByteBuffer dlb = ByteBuffer.allocateDirect(this.drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            this.drawListBuffer = dlb.asShortBuffer();
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
            long now = SystemClock.uptimeMillis();
            this.colorBuffer.clear();
            float t = now / 4000.0f;
            for (int i = 0; i < this.vertexCount; i++) {
                float freq = (float) Math.sin((6.283185307179586d * ((double) t)) / ((double) this.cornerFrequencies[i]));
                this._tmphsv[0] = this.HUES[(this.cornerRotation + i) % this.vertexCount];
                this._tmphsv[1] = 1.0f;
                this._tmphsv[2] = (0.25f * freq) + 0.75f;
                int c = Color.HSVToColor(this._tmphsv);
                this.colorBuffer.put(((16711680 & c) >> 16) / 255.0f);
                this.colorBuffer.put(((65280 & c) >> 8) / 255.0f);
                this.colorBuffer.put((c & 255) / 255.0f);
                this.colorBuffer.put(1.0f);
            }
            this.colorBuffer.position(0);
            GLES20.glVertexAttribPointer(this.mColorHandle, 4, 5126, false, 16, (Buffer) this.colorBuffer);
            ColorsGLRenderer.checkGlError("glVertexAttribPointer");
            GLES20.glDrawArrays(6, 0, this.vertexCount);
        }
    }
}
