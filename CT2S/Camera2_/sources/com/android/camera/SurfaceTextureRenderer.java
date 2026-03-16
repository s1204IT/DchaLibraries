package com.android.camera;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import com.android.camera.debug.Log;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class SurfaceTextureRenderer {
    private static final int EGL_CONTEXT_CLIENT_VERSION = 12440;
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private EGL10 mEgl;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private EGLDisplay mEglDisplay;
    private final Handler mEglHandler;
    private EGLSurface mEglSurface;
    private final FrameDrawer mFrameDrawer;
    private GL10 mGl;
    private static final Log.Tag TAG = new Log.Tag("SurfTexRenderer");
    private static final int[] CONFIG_SPEC = {12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12344};
    private volatile boolean mDrawPending = false;
    private final Object mRenderLock = new Object();
    private final Runnable mRenderTask = new Runnable() {
        @Override
        public void run() {
            synchronized (SurfaceTextureRenderer.this.mRenderLock) {
                if (SurfaceTextureRenderer.this.mEglDisplay != null && SurfaceTextureRenderer.this.mEglSurface != null) {
                    SurfaceTextureRenderer.this.mFrameDrawer.onDrawFrame(SurfaceTextureRenderer.this.mGl);
                    SurfaceTextureRenderer.this.mEgl.eglSwapBuffers(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglSurface);
                    SurfaceTextureRenderer.this.mDrawPending = false;
                }
                SurfaceTextureRenderer.this.mRenderLock.notifyAll();
            }
        }
    };

    public interface FrameDrawer {
        void onDrawFrame(GL10 gl10);
    }

    public SurfaceTextureRenderer(SurfaceTexture tex, Handler handler, FrameDrawer renderer) {
        this.mEglHandler = handler;
        this.mFrameDrawer = renderer;
        initialize(tex);
    }

    public void release() {
        this.mEglHandler.post(new Runnable() {
            @Override
            public void run() {
                SurfaceTextureRenderer.this.mEgl.eglDestroySurface(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglSurface);
                SurfaceTextureRenderer.this.mEgl.eglDestroyContext(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglContext);
                SurfaceTextureRenderer.this.mEgl.eglMakeCurrent(SurfaceTextureRenderer.this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                SurfaceTextureRenderer.this.mEgl.eglTerminate(SurfaceTextureRenderer.this.mEglDisplay);
                SurfaceTextureRenderer.this.mEglSurface = null;
                SurfaceTextureRenderer.this.mEglContext = null;
                SurfaceTextureRenderer.this.mEglDisplay = null;
            }
        });
    }

    public void draw(boolean sync) {
        synchronized (this.mRenderLock) {
            if (!this.mDrawPending) {
                this.mEglHandler.post(this.mRenderTask);
                this.mDrawPending = true;
                if (sync) {
                    try {
                        this.mRenderLock.wait();
                    } catch (InterruptedException e) {
                        Log.v(TAG, "RenderLock.wait() interrupted");
                    }
                }
            }
        }
    }

    private void initialize(final SurfaceTexture target) {
        this.mEglHandler.post(new Runnable() {
            @Override
            public void run() {
                SurfaceTextureRenderer.this.mEgl = (EGL10) EGLContext.getEGL();
                SurfaceTextureRenderer.this.mEglDisplay = SurfaceTextureRenderer.this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
                if (SurfaceTextureRenderer.this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                    throw new RuntimeException("eglGetDisplay failed");
                }
                int[] version = new int[2];
                if (SurfaceTextureRenderer.this.mEgl.eglInitialize(SurfaceTextureRenderer.this.mEglDisplay, version)) {
                    Log.v(SurfaceTextureRenderer.TAG, "EGL version: " + version[0] + '.' + version[1]);
                    int[] attribList = {SurfaceTextureRenderer.EGL_CONTEXT_CLIENT_VERSION, 2, 12344};
                    SurfaceTextureRenderer.this.mEglConfig = SurfaceTextureRenderer.chooseConfig(SurfaceTextureRenderer.this.mEgl, SurfaceTextureRenderer.this.mEglDisplay);
                    SurfaceTextureRenderer.this.mEglContext = SurfaceTextureRenderer.this.mEgl.eglCreateContext(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglConfig, EGL10.EGL_NO_CONTEXT, attribList);
                    if (SurfaceTextureRenderer.this.mEglContext == null || SurfaceTextureRenderer.this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                        throw new RuntimeException("failed to createContext");
                    }
                    SurfaceTextureRenderer.this.mEglSurface = SurfaceTextureRenderer.this.mEgl.eglCreateWindowSurface(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglConfig, target, null);
                    if (SurfaceTextureRenderer.this.mEglSurface != null && SurfaceTextureRenderer.this.mEglSurface != EGL10.EGL_NO_SURFACE) {
                        if (!SurfaceTextureRenderer.this.mEgl.eglMakeCurrent(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglSurface, SurfaceTextureRenderer.this.mEglSurface, SurfaceTextureRenderer.this.mEglContext)) {
                            throw new RuntimeException("failed to eglMakeCurrent");
                        }
                        SurfaceTextureRenderer.this.mGl = (GL10) SurfaceTextureRenderer.this.mEglContext.getGL();
                        return;
                    }
                    throw new RuntimeException("failed to createWindowSurface");
                }
                throw new RuntimeException("eglInitialize failed");
            }
        });
        waitDone();
    }

    private void waitDone() {
        final Object lock = new Object();
        synchronized (lock) {
            this.mEglHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            });
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.v(TAG, "waitDone() interrupted");
            }
        }
    }

    private static void checkEglError(String prompt, EGL10 egl) {
        while (true) {
            int error = egl.eglGetError();
            if (error != 12288) {
                Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, Integer.valueOf(error)));
            } else {
                return;
            }
        }
    }

    private static EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int[] numConfig = new int[1];
        if (!egl.eglChooseConfig(display, CONFIG_SPEC, null, 0, numConfig)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }
        int numConfigs = numConfig[0];
        if (numConfigs <= 0) {
            throw new IllegalArgumentException("No configs match configSpec");
        }
        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!egl.eglChooseConfig(display, CONFIG_SPEC, configs, numConfigs, numConfig)) {
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }
        return configs[0];
    }
}
