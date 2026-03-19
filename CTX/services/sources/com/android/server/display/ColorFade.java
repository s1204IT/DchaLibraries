package com.android.server.display;

import android.R;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManagerInternal;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import com.android.server.LocalServices;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import libcore.io.Streams;

final class ColorFade {
    private static final int COLOR_FADE_LAYER = 1073741825;
    private static final boolean DEBUG = false;
    private static final int DEJANK_FRAMES = 3;
    public static final int MODE_COOL_DOWN = 1;
    public static final int MODE_FADE = 2;
    public static final int MODE_WARM_UP = 0;
    private static final String TAG = "ColorFade";
    private boolean mCreatedResources;
    private int mDisplayHeight;
    private final int mDisplayId;
    private int mDisplayLayerStack;
    private int mDisplayWidth;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private int mGammaLoc;
    private int mMode;
    private int mOpacityLoc;
    private boolean mPrepared;
    private int mProgram;
    private int mProjMatrixLoc;
    private Surface mSurface;
    private float mSurfaceAlpha;
    private SurfaceControl mSurfaceControl;
    private NaturalSurfaceLayout mSurfaceLayout;
    private SurfaceSession mSurfaceSession;
    private boolean mSurfaceVisible;
    private int mTexCoordLoc;
    private int mTexMatrixLoc;
    private boolean mTexNamesGenerated;
    private int mTexUnitLoc;
    private int mVertexLoc;
    private final int[] mTexNames = new int[1];
    private final float[] mTexMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final int[] mGLBuffers = new int[2];
    private final FloatBuffer mVertexBuffer = createNativeFloatBuffer(8);
    private final FloatBuffer mTexCoordBuffer = createNativeFloatBuffer(8);
    private final DisplayManagerInternal mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);

    public ColorFade(int i) {
        this.mDisplayId = i;
    }

    public boolean prepare(Context context, int i) {
        this.mMode = i;
        DisplayInfo displayInfo = this.mDisplayManagerInternal.getDisplayInfo(this.mDisplayId);
        this.mDisplayLayerStack = displayInfo.layerStack;
        this.mDisplayWidth = displayInfo.getNaturalWidth();
        this.mDisplayHeight = displayInfo.getNaturalHeight();
        if (!createSurface() || !createEglContext() || !createEglSurface() || !captureScreenshotTextureAndSetViewport()) {
            dismiss();
            return false;
        }
        if (!attachEglContext()) {
            return false;
        }
        try {
            if (!initGLShaders(context) || !initGLBuffers() || checkGlErrors("prepare")) {
                detachEglContext();
                dismiss();
                return false;
            }
            detachEglContext();
            this.mCreatedResources = true;
            this.mPrepared = true;
            if (i == 1) {
                for (int i2 = 0; i2 < 3; i2++) {
                    draw(1.0f);
                }
            }
            return true;
        } finally {
            detachEglContext();
        }
    }

    private String readFile(Context context, int i) {
        try {
            return new String(Streams.readFully(new InputStreamReader(context.getResources().openRawResource(i))));
        } catch (IOException e) {
            Slog.e(TAG, "Unrecognized shader " + Integer.toString(i));
            throw new RuntimeException(e);
        }
    }

    private int loadShader(Context context, int i, int i2) {
        String file = readFile(context, i);
        int iGlCreateShader = GLES20.glCreateShader(i2);
        GLES20.glShaderSource(iGlCreateShader, file);
        GLES20.glCompileShader(iGlCreateShader);
        int[] iArr = new int[1];
        GLES20.glGetShaderiv(iGlCreateShader, 35713, iArr, 0);
        if (iArr[0] != 0) {
            return iGlCreateShader;
        }
        Slog.e(TAG, "Could not compile shader " + iGlCreateShader + ", " + i2 + ":");
        Slog.e(TAG, GLES20.glGetShaderSource(iGlCreateShader));
        Slog.e(TAG, GLES20.glGetShaderInfoLog(iGlCreateShader));
        GLES20.glDeleteShader(iGlCreateShader);
        return 0;
    }

    private boolean initGLShaders(Context context) {
        int iLoadShader = loadShader(context, R.raw.color_fade_vert, 35633);
        int iLoadShader2 = loadShader(context, R.raw.color_fade_frag, 35632);
        GLES20.glReleaseShaderCompiler();
        if (iLoadShader == 0 || iLoadShader2 == 0) {
            return false;
        }
        this.mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(this.mProgram, iLoadShader);
        GLES20.glAttachShader(this.mProgram, iLoadShader2);
        GLES20.glDeleteShader(iLoadShader);
        GLES20.glDeleteShader(iLoadShader2);
        GLES20.glLinkProgram(this.mProgram);
        this.mVertexLoc = GLES20.glGetAttribLocation(this.mProgram, "position");
        this.mTexCoordLoc = GLES20.glGetAttribLocation(this.mProgram, "uv");
        this.mProjMatrixLoc = GLES20.glGetUniformLocation(this.mProgram, "proj_matrix");
        this.mTexMatrixLoc = GLES20.glGetUniformLocation(this.mProgram, "tex_matrix");
        this.mOpacityLoc = GLES20.glGetUniformLocation(this.mProgram, "opacity");
        this.mGammaLoc = GLES20.glGetUniformLocation(this.mProgram, "gamma");
        this.mTexUnitLoc = GLES20.glGetUniformLocation(this.mProgram, "texUnit");
        GLES20.glUseProgram(this.mProgram);
        GLES20.glUniform1i(this.mTexUnitLoc, 0);
        GLES20.glUseProgram(0);
        return true;
    }

    private void destroyGLShaders() {
        GLES20.glDeleteProgram(this.mProgram);
        checkGlErrors("glDeleteProgram");
    }

    private boolean initGLBuffers() {
        setQuad(this.mVertexBuffer, 0.0f, 0.0f, this.mDisplayWidth, this.mDisplayHeight);
        GLES20.glBindTexture(36197, this.mTexNames[0]);
        GLES20.glTexParameteri(36197, 10240, 9728);
        GLES20.glTexParameteri(36197, 10241, 9728);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        GLES20.glBindTexture(36197, 0);
        GLES20.glGenBuffers(2, this.mGLBuffers, 0);
        GLES20.glBindBuffer(34962, this.mGLBuffers[0]);
        GLES20.glBufferData(34962, this.mVertexBuffer.capacity() * 4, this.mVertexBuffer, 35044);
        GLES20.glBindBuffer(34962, this.mGLBuffers[1]);
        GLES20.glBufferData(34962, this.mTexCoordBuffer.capacity() * 4, this.mTexCoordBuffer, 35044);
        GLES20.glBindBuffer(34962, 0);
        return true;
    }

    private void destroyGLBuffers() {
        GLES20.glDeleteBuffers(2, this.mGLBuffers, 0);
        checkGlErrors("glDeleteBuffers");
    }

    private static void setQuad(FloatBuffer floatBuffer, float f, float f2, float f3, float f4) {
        floatBuffer.put(0, f);
        floatBuffer.put(1, f2);
        floatBuffer.put(2, f);
        float f5 = f4 + f2;
        floatBuffer.put(3, f5);
        float f6 = f + f3;
        floatBuffer.put(4, f6);
        floatBuffer.put(5, f5);
        floatBuffer.put(6, f6);
        floatBuffer.put(7, f2);
    }

    public void dismissResources() {
        if (this.mCreatedResources) {
            attachEglContext();
            try {
                destroyScreenshotTexture();
                destroyGLShaders();
                destroyGLBuffers();
                destroyEglSurface();
                detachEglContext();
                GLES20.glFlush();
                this.mCreatedResources = false;
            } catch (Throwable th) {
                detachEglContext();
                throw th;
            }
        }
    }

    public void dismiss() {
        if (this.mPrepared) {
            dismissResources();
            destroySurface();
            this.mPrepared = false;
        }
    }

    public boolean draw(float f) {
        if (!this.mPrepared) {
            return false;
        }
        if (this.mMode == 2) {
            return showSurface(1.0f - f);
        }
        if (!attachEglContext()) {
            return false;
        }
        try {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(16384);
            double d = 1.0f - f;
            double dCos = Math.cos(3.141592653589793d * d);
            drawFaded(((float) (-Math.pow(d, 2.0d))) + 1.0f, 1.0f / ((float) ((((((dCos < 0.0d ? -1.0d : 1.0d) * 0.5d) * Math.pow(dCos, 2.0d)) + 0.5d) * 0.9d) + 0.1d)));
            if (checkGlErrors("drawFrame")) {
                return false;
            }
            EGL14.eglSwapBuffers(this.mEglDisplay, this.mEglSurface);
            detachEglContext();
            return showSurface(1.0f);
        } finally {
            detachEglContext();
        }
    }

    private void drawFaded(float f, float f2) {
        GLES20.glUseProgram(this.mProgram);
        GLES20.glUniformMatrix4fv(this.mProjMatrixLoc, 1, false, this.mProjMatrix, 0);
        GLES20.glUniformMatrix4fv(this.mTexMatrixLoc, 1, false, this.mTexMatrix, 0);
        GLES20.glUniform1f(this.mOpacityLoc, f);
        GLES20.glUniform1f(this.mGammaLoc, f2);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.mTexNames[0]);
        GLES20.glBindBuffer(34962, this.mGLBuffers[0]);
        GLES20.glEnableVertexAttribArray(this.mVertexLoc);
        GLES20.glVertexAttribPointer(this.mVertexLoc, 2, 5126, false, 0, 0);
        GLES20.glBindBuffer(34962, this.mGLBuffers[1]);
        GLES20.glEnableVertexAttribArray(this.mTexCoordLoc);
        GLES20.glVertexAttribPointer(this.mTexCoordLoc, 2, 5126, false, 0, 0);
        GLES20.glDrawArrays(6, 0, 4);
        GLES20.glBindTexture(36197, 0);
        GLES20.glBindBuffer(34962, 0);
    }

    private void ortho(float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = f2 - f;
        this.mProjMatrix[0] = 2.0f / f7;
        this.mProjMatrix[1] = 0.0f;
        this.mProjMatrix[2] = 0.0f;
        this.mProjMatrix[3] = 0.0f;
        this.mProjMatrix[4] = 0.0f;
        float f8 = f4 - f3;
        this.mProjMatrix[5] = 2.0f / f8;
        this.mProjMatrix[6] = 0.0f;
        this.mProjMatrix[7] = 0.0f;
        this.mProjMatrix[8] = 0.0f;
        this.mProjMatrix[9] = 0.0f;
        float f9 = f6 - f5;
        this.mProjMatrix[10] = (-2.0f) / f9;
        this.mProjMatrix[11] = 0.0f;
        this.mProjMatrix[12] = (-(f2 + f)) / f7;
        this.mProjMatrix[13] = (-(f4 + f3)) / f8;
        this.mProjMatrix[14] = (-(f6 + f5)) / f9;
        this.mProjMatrix[15] = 1.0f;
    }

    private boolean captureScreenshotTextureAndSetViewport() {
        if (!attachEglContext()) {
            return false;
        }
        try {
            if (!this.mTexNamesGenerated) {
                GLES20.glGenTextures(1, this.mTexNames, 0);
                if (checkGlErrors("glGenTextures")) {
                    return false;
                }
                this.mTexNamesGenerated = true;
            }
            SurfaceTexture surfaceTexture = new SurfaceTexture(this.mTexNames[0]);
            Surface surface = new Surface(surfaceTexture);
            try {
                SurfaceControl.screenshot(SurfaceControl.getBuiltInDisplay(0), surface);
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(this.mTexMatrix);
                surface.release();
                surfaceTexture.release();
                this.mTexCoordBuffer.put(0, 0.0f);
                this.mTexCoordBuffer.put(1, 0.0f);
                this.mTexCoordBuffer.put(2, 0.0f);
                this.mTexCoordBuffer.put(3, 1.0f);
                this.mTexCoordBuffer.put(4, 1.0f);
                this.mTexCoordBuffer.put(5, 1.0f);
                this.mTexCoordBuffer.put(6, 1.0f);
                this.mTexCoordBuffer.put(7, 0.0f);
                GLES20.glViewport(0, 0, this.mDisplayWidth, this.mDisplayHeight);
                ortho(0.0f, this.mDisplayWidth, 0.0f, this.mDisplayHeight, -1.0f, 1.0f);
                return true;
            } catch (Throwable th) {
                surface.release();
                surfaceTexture.release();
                throw th;
            }
        } finally {
            detachEglContext();
        }
    }

    private void destroyScreenshotTexture() {
        if (this.mTexNamesGenerated) {
            this.mTexNamesGenerated = false;
            GLES20.glDeleteTextures(1, this.mTexNames, 0);
            checkGlErrors("glDeleteTextures");
        }
    }

    private boolean createEglContext() {
        if (this.mEglDisplay == null) {
            this.mEglDisplay = EGL14.eglGetDisplay(0);
            if (this.mEglDisplay == EGL14.EGL_NO_DISPLAY) {
                logEglError("eglGetDisplay");
                return false;
            }
            int[] iArr = new int[2];
            if (!EGL14.eglInitialize(this.mEglDisplay, iArr, 0, iArr, 1)) {
                this.mEglDisplay = null;
                logEglError("eglInitialize");
                return false;
            }
        }
        if (this.mEglConfig == null) {
            int[] iArr2 = new int[1];
            EGLConfig[] eGLConfigArr = new EGLConfig[1];
            if (!EGL14.eglChooseConfig(this.mEglDisplay, new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 8, 12344}, 0, eGLConfigArr, 0, eGLConfigArr.length, iArr2, 0)) {
                logEglError("eglChooseConfig");
                return false;
            }
            if (iArr2[0] <= 0) {
                Slog.e(TAG, "no valid config found");
                return false;
            }
            this.mEglConfig = eGLConfigArr[0];
        }
        if (this.mEglContext == null) {
            this.mEglContext = EGL14.eglCreateContext(this.mEglDisplay, this.mEglConfig, EGL14.EGL_NO_CONTEXT, new int[]{12440, 2, 12344}, 0);
            if (this.mEglContext == null) {
                logEglError("eglCreateContext");
                return false;
            }
        }
        return true;
    }

    private boolean createSurface() {
        int i;
        if (this.mSurfaceSession == null) {
            this.mSurfaceSession = new SurfaceSession();
        }
        SurfaceControl.openTransaction();
        try {
            if (this.mSurfaceControl == null) {
                try {
                    if (this.mMode == 2) {
                        i = 131076;
                    } else {
                        i = UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_SUPRESS;
                    }
                    this.mSurfaceControl = new SurfaceControl.Builder(this.mSurfaceSession).setName(TAG).setSize(this.mDisplayWidth, this.mDisplayHeight).setFlags(i).build();
                    this.mSurfaceControl.setLayerStack(this.mDisplayLayerStack);
                    this.mSurfaceControl.setSize(this.mDisplayWidth, this.mDisplayHeight);
                    this.mSurface = new Surface();
                    this.mSurface.copyFrom(this.mSurfaceControl);
                    this.mSurfaceLayout = new NaturalSurfaceLayout(this.mDisplayManagerInternal, this.mDisplayId, this.mSurfaceControl);
                    this.mSurfaceLayout.onDisplayTransaction();
                } catch (Surface.OutOfResourcesException e) {
                    Slog.e(TAG, "Unable to create surface.", e);
                    SurfaceControl.closeTransaction();
                    return false;
                }
            }
            SurfaceControl.closeTransaction();
            return true;
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
            throw th;
        }
    }

    private boolean createEglSurface() {
        if (this.mEglSurface == null) {
            this.mEglSurface = EGL14.eglCreateWindowSurface(this.mEglDisplay, this.mEglConfig, this.mSurface, new int[]{12344}, 0);
            if (this.mEglSurface == null) {
                logEglError("eglCreateWindowSurface");
                return false;
            }
        }
        return true;
    }

    private void destroyEglSurface() {
        if (this.mEglSurface != null) {
            if (!EGL14.eglDestroySurface(this.mEglDisplay, this.mEglSurface)) {
                logEglError("eglDestroySurface");
            }
            this.mEglSurface = null;
        }
    }

    private void destroySurface() {
        if (this.mSurfaceControl != null) {
            this.mSurfaceLayout.dispose();
            this.mSurfaceLayout = null;
            SurfaceControl.openTransaction();
            try {
                this.mSurfaceControl.destroy();
                this.mSurface.release();
                SurfaceControl.closeTransaction();
                this.mSurfaceControl = null;
                this.mSurfaceVisible = false;
                this.mSurfaceAlpha = 0.0f;
            } catch (Throwable th) {
                SurfaceControl.closeTransaction();
                throw th;
            }
        }
    }

    private boolean showSurface(float f) {
        if (!this.mSurfaceVisible || this.mSurfaceAlpha != f) {
            SurfaceControl.openTransaction();
            try {
                this.mSurfaceControl.setLayer(COLOR_FADE_LAYER);
                this.mSurfaceControl.setAlpha(f);
                this.mSurfaceControl.show();
                SurfaceControl.closeTransaction();
                this.mSurfaceVisible = true;
                this.mSurfaceAlpha = f;
            } catch (Throwable th) {
                SurfaceControl.closeTransaction();
                throw th;
            }
        }
        return true;
    }

    private boolean attachEglContext() {
        if (this.mEglSurface == null) {
            return false;
        }
        if (!EGL14.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
            logEglError("eglMakeCurrent");
            return false;
        }
        return true;
    }

    private void detachEglContext() {
        if (this.mEglDisplay != null) {
            EGL14.eglMakeCurrent(this.mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        }
    }

    private static FloatBuffer createNativeFloatBuffer(int i) {
        ByteBuffer byteBufferAllocateDirect = ByteBuffer.allocateDirect(i * 4);
        byteBufferAllocateDirect.order(ByteOrder.nativeOrder());
        return byteBufferAllocateDirect.asFloatBuffer();
    }

    private static void logEglError(String str) {
        Slog.e(TAG, str + " failed: error " + EGL14.eglGetError(), new Throwable());
    }

    private static boolean checkGlErrors(String str) {
        return checkGlErrors(str, true);
    }

    private static boolean checkGlErrors(String str, boolean z) {
        boolean z2 = false;
        while (true) {
            int iGlGetError = GLES20.glGetError();
            if (iGlGetError != 0) {
                if (z) {
                    Slog.e(TAG, str + " failed: error " + iGlGetError, new Throwable());
                }
                z2 = true;
            } else {
                return z2;
            }
        }
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println();
        printWriter.println("Color Fade State:");
        printWriter.println("  mPrepared=" + this.mPrepared);
        printWriter.println("  mMode=" + this.mMode);
        printWriter.println("  mDisplayLayerStack=" + this.mDisplayLayerStack);
        printWriter.println("  mDisplayWidth=" + this.mDisplayWidth);
        printWriter.println("  mDisplayHeight=" + this.mDisplayHeight);
        printWriter.println("  mSurfaceVisible=" + this.mSurfaceVisible);
        printWriter.println("  mSurfaceAlpha=" + this.mSurfaceAlpha);
    }

    private static final class NaturalSurfaceLayout implements DisplayManagerInternal.DisplayTransactionListener {
        private final int mDisplayId;
        private final DisplayManagerInternal mDisplayManagerInternal;
        private SurfaceControl mSurfaceControl;

        public NaturalSurfaceLayout(DisplayManagerInternal displayManagerInternal, int i, SurfaceControl surfaceControl) {
            this.mDisplayManagerInternal = displayManagerInternal;
            this.mDisplayId = i;
            this.mSurfaceControl = surfaceControl;
            this.mDisplayManagerInternal.registerDisplayTransactionListener(this);
        }

        public void dispose() {
            synchronized (this) {
                this.mSurfaceControl = null;
            }
            this.mDisplayManagerInternal.unregisterDisplayTransactionListener(this);
        }

        public void onDisplayTransaction() {
            synchronized (this) {
                if (this.mSurfaceControl == null) {
                    return;
                }
                switch (this.mDisplayManagerInternal.getDisplayInfo(this.mDisplayId).rotation) {
                    case 0:
                        this.mSurfaceControl.setPosition(0.0f, 0.0f);
                        this.mSurfaceControl.setMatrix(1.0f, 0.0f, 0.0f, 1.0f);
                        break;
                    case 1:
                        this.mSurfaceControl.setPosition(0.0f, r0.logicalHeight);
                        this.mSurfaceControl.setMatrix(0.0f, -1.0f, 1.0f, 0.0f);
                        break;
                    case 2:
                        this.mSurfaceControl.setPosition(r0.logicalWidth, r0.logicalHeight);
                        this.mSurfaceControl.setMatrix(-1.0f, 0.0f, 0.0f, -1.0f);
                        break;
                    case 3:
                        this.mSurfaceControl.setPosition(r0.logicalWidth, 0.0f);
                        this.mSurfaceControl.setMatrix(0.0f, 1.0f, -1.0f, 0.0f);
                        break;
                }
            }
        }
    }
}
