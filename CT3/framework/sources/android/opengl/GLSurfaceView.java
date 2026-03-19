package android.opengl;

import android.content.Context;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.mediatek.perfservice.IPerfServiceWrapper;
import com.mediatek.perfservice.PerfServiceWrapper;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class GLSurfaceView extends SurfaceView implements SurfaceHolder.Callback2 {
    private static final int DBG_ATTACH_DETACH = 1;
    private static final int DBG_EGL = 64;
    private static final int DBG_PAUSE_RESUME = 4;
    private static final int DBG_RENDERER = 16;
    private static final int DBG_RENDERER_DRAW_FRAME = 32;
    private static final int DBG_SURFACE = 8;
    private static final int DBG_THREADS = 2;
    public static final int DEBUG_CHECK_GL_ERROR = 1;
    public static final int DEBUG_LOG_GL_CALLS = 2;
    private static final String LOG_PROPERTY_NAME = "debug.glsurfaceview.dumpinfo";
    public static final int RENDERMODE_CONTINUOUSLY = 1;
    public static final int RENDERMODE_WHEN_DIRTY = 0;
    private static final String TAG = "GLSurfaceView";
    private int mDebugFlags;
    private boolean mDetached;
    private EGLConfigChooser mEGLConfigChooser;
    private int mEGLContextClientVersion;
    private EGLContextFactory mEGLContextFactory;
    private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private GLThread mGLThread;
    private GLWrapper mGLWrapper;
    private boolean mPreserveEGLContextOnPause;
    private Renderer mRenderer;
    private final WeakReference<GLSurfaceView> mThisWeakRef;
    private static boolean LOG_ATTACH_DETACH = false;
    private static boolean LOG_THREADS = false;
    private static boolean LOG_PAUSE_RESUME = false;
    private static boolean LOG_SURFACE = false;
    private static boolean LOG_RENDERER = false;
    private static boolean LOG_RENDERER_DRAW_FRAME = false;
    private static boolean LOG_EGL = false;
    private static final GLThreadManager sGLThreadManager = new GLThreadManager(null);

    public interface EGLConfigChooser {
        javax.microedition.khronos.egl.EGLConfig chooseConfig(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay);
    }

    public interface EGLContextFactory {
        javax.microedition.khronos.egl.EGLContext createContext(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLConfig eGLConfig);

        void destroyContext(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLContext eGLContext);
    }

    public interface EGLWindowSurfaceFactory {
        javax.microedition.khronos.egl.EGLSurface createWindowSurface(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLConfig eGLConfig, Object obj);

        void destroySurface(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLSurface eGLSurface);
    }

    public interface GLWrapper {
        GL wrap(GL gl);
    }

    public interface Renderer {
        void onDrawFrame(GL10 gl10);

        void onSurfaceChanged(GL10 gl10, int i, int i2);

        void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eGLConfig);
    }

    public GLSurfaceView(Context context) {
        super(context);
        this.mThisWeakRef = new WeakReference<>(this);
        init();
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mThisWeakRef = new WeakReference<>(this);
        init();
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mGLThread != null) {
                this.mGLThread.requestExitAndWait();
            }
        } finally {
            super.finalize();
        }
    }

    private void init() {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        checkLogProperty();
    }

    public void setGLWrapper(GLWrapper glWrapper) {
        this.mGLWrapper = glWrapper;
    }

    public void setDebugFlags(int debugFlags) {
        this.mDebugFlags = debugFlags;
    }

    public int getDebugFlags() {
        return this.mDebugFlags;
    }

    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        this.mPreserveEGLContextOnPause = preserveOnPause;
    }

    public boolean getPreserveEGLContextOnPause() {
        return this.mPreserveEGLContextOnPause;
    }

    public void setRenderer(Renderer renderer) {
        DefaultContextFactory defaultContextFactory = null;
        Object[] objArr = 0;
        checkRenderThreadState();
        Log.i(TAG, "setRenderer(), this = " + this);
        if (this.mEGLConfigChooser == null) {
            this.mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
        if (this.mEGLContextFactory == null) {
            this.mEGLContextFactory = new DefaultContextFactory(this, defaultContextFactory);
        }
        if (this.mEGLWindowSurfaceFactory == null) {
            this.mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory(objArr == true ? 1 : 0);
        }
        this.mRenderer = renderer;
        this.mGLThread = new GLThread(this.mThisWeakRef);
        this.mGLThread.start();
    }

    public void setEGLContextFactory(EGLContextFactory factory) {
        checkRenderThreadState();
        this.mEGLContextFactory = factory;
        Log.i(TAG, "setEGLContextFactory(), factory = " + factory + ", this = " + this);
    }

    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
        checkRenderThreadState();
        this.mEGLWindowSurfaceFactory = factory;
        Log.i(TAG, "setEGLWindowSurfaceFactory(), factory = " + factory + ", this = " + this);
    }

    public void setEGLConfigChooser(EGLConfigChooser configChooser) {
        checkRenderThreadState();
        this.mEGLConfigChooser = configChooser;
    }

    public void setEGLConfigChooser(boolean needDepth) {
        setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
    }

    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
        setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize));
    }

    public void setEGLContextClientVersion(int version) {
        checkRenderThreadState();
        this.mEGLContextClientVersion = version;
    }

    public void setRenderMode(int renderMode) {
        Log.i(TAG, "setRenderMode = " + renderMode + ", this = " + this);
        this.mGLThread.setRenderMode(renderMode);
    }

    public int getRenderMode() {
        return this.mGLThread.getRenderMode();
    }

    public void requestRender() {
        this.mGLThread.requestRender();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.mGLThread.surfaceCreated();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.mGLThread.surfaceDestroyed();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        this.mGLThread.onWindowResize(w, h);
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        if (this.mGLThread == null) {
            return;
        }
        this.mGLThread.requestRenderAndWait();
    }

    public void onPause() {
        this.mGLThread.onPause();
    }

    public void onResume() {
        this.mGLThread.onResume();
    }

    public void queueEvent(Runnable r) {
        this.mGLThread.queueEvent(r);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onAttachedToWindow reattach =" + this.mDetached + ", this = " + this);
        }
        if (this.mDetached && this.mRenderer != null) {
            int renderMode = 1;
            if (this.mGLThread != null) {
                renderMode = this.mGLThread.getRenderMode();
            }
            this.mGLThread = new GLThread(this.mThisWeakRef);
            if (renderMode != 1) {
                this.mGLThread.setRenderMode(renderMode);
            }
            this.mGLThread.start();
        }
        this.mDetached = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onDetachedFromWindow, this = " + this);
        }
        if (this.mGLThread != null) {
            this.mGLThread.requestExitAndWait();
        }
        this.mDetached = true;
        super.onDetachedFromWindow();
    }

    private class DefaultContextFactory implements EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION;

        DefaultContextFactory(GLSurfaceView this$0, DefaultContextFactory defaultContextFactory) {
            this();
        }

        private DefaultContextFactory() {
            this.EGL_CONTEXT_CLIENT_VERSION = 12440;
        }

        @Override
        public javax.microedition.khronos.egl.EGLContext createContext(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLConfig config) {
            int[] attrib_list = {this.EGL_CONTEXT_CLIENT_VERSION, GLSurfaceView.this.mEGLContextClientVersion, EGL14.EGL_NONE};
            Log.i("DefaultContextFactory", "createContext = " + Thread.currentThread().getId() + ", this = " + GLSurfaceView.this);
            javax.microedition.khronos.egl.EGLContext eGLContext = EGL10.EGL_NO_CONTEXT;
            if (GLSurfaceView.this.mEGLContextClientVersion == 0) {
                attrib_list = null;
            }
            return egl.eglCreateContext(display, config, eGLContext, attrib_list);
        }

        @Override
        public void destroyContext(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLContext context) {
            Log.i("DefaultContextFactory", "eglDestroyContext = " + Thread.currentThread().getId() + ", this = " + GLSurfaceView.this);
            if (egl.eglDestroyContext(display, context)) {
                return;
            }
            Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
            if (GLSurfaceView.LOG_THREADS) {
                Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId() + ", this = " + GLSurfaceView.this);
            }
            EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
        }
    }

    private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {
        DefaultWindowSurfaceFactory(DefaultWindowSurfaceFactory defaultWindowSurfaceFactory) {
            this();
        }

        private DefaultWindowSurfaceFactory() {
        }

        @Override
        public javax.microedition.khronos.egl.EGLSurface createWindowSurface(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLConfig config, Object nativeWindow) {
            try {
                javax.microedition.khronos.egl.EGLSurface result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
                return result;
            } catch (IllegalArgumentException e) {
                Log.e("DefaultWindowSurfaceFactory", "eglCreateWindowSurface", e);
                return null;
            }
        }

        @Override
        public void destroySurface(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLSurface surface) {
            egl.eglDestroySurface(display, surface);
        }
    }

    private abstract class BaseConfigChooser implements EGLConfigChooser {
        protected int[] mConfigSpec;

        abstract javax.microedition.khronos.egl.EGLConfig chooseConfig(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLConfig[] eGLConfigArr);

        public BaseConfigChooser(int[] configSpec) {
            this.mConfigSpec = filterConfigSpec(configSpec);
        }

        @Override
        public javax.microedition.khronos.egl.EGLConfig chooseConfig(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display) {
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, this.mConfigSpec, null, 0, num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }
            int numConfigs = num_config[0];
            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }
            javax.microedition.khronos.egl.EGLConfig[] configs = new javax.microedition.khronos.egl.EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, this.mConfigSpec, configs, numConfigs, num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            javax.microedition.khronos.egl.EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        private int[] filterConfigSpec(int[] configSpec) {
            if (GLSurfaceView.this.mEGLContextClientVersion != 2 && GLSurfaceView.this.mEGLContextClientVersion != 3) {
                return configSpec;
            }
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = 12352;
            if (GLSurfaceView.this.mEGLContextClientVersion == 2) {
                newConfigSpec[len] = 4;
            } else {
                newConfigSpec[len] = 64;
            }
            newConfigSpec[len + 1] = 12344;
            return newConfigSpec;
        }
    }

    private class ComponentSizeChooser extends BaseConfigChooser {
        protected int mAlphaSize;
        protected int mBlueSize;
        protected int mDepthSize;
        protected int mGreenSize;
        protected int mRedSize;
        protected int mStencilSize;
        private int[] mValue;

        public ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
            super(new int[]{EGL14.EGL_RED_SIZE, redSize, EGL14.EGL_GREEN_SIZE, greenSize, EGL14.EGL_BLUE_SIZE, blueSize, EGL14.EGL_ALPHA_SIZE, alphaSize, EGL14.EGL_DEPTH_SIZE, depthSize, EGL14.EGL_STENCIL_SIZE, stencilSize, EGL14.EGL_NONE});
            this.mValue = new int[1];
            this.mRedSize = redSize;
            this.mGreenSize = greenSize;
            this.mBlueSize = blueSize;
            this.mAlphaSize = alphaSize;
            this.mDepthSize = depthSize;
            this.mStencilSize = stencilSize;
        }

        @Override
        public javax.microedition.khronos.egl.EGLConfig chooseConfig(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLConfig[] configs) {
            for (javax.microedition.khronos.egl.EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config, EGL14.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config, EGL14.EGL_STENCIL_SIZE, 0);
                if (d >= this.mDepthSize && s >= this.mStencilSize) {
                    int r = findConfigAttrib(egl, display, config, EGL14.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl, display, config, EGL14.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl, display, config, EGL14.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl, display, config, EGL14.EGL_ALPHA_SIZE, 0);
                    if (r == this.mRedSize && g == this.mGreenSize && b == this.mBlueSize && a == this.mAlphaSize) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLConfig config, int attribute, int defaultValue) {
            if (egl.eglGetConfigAttrib(display, config, attribute, this.mValue)) {
                return this.mValue[0];
            }
            return defaultValue;
        }
    }

    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    private static class EglHelper {
        EGL10 mEgl;
        javax.microedition.khronos.egl.EGLConfig mEglConfig;
        javax.microedition.khronos.egl.EGLContext mEglContext;
        javax.microedition.khronos.egl.EGLDisplay mEglDisplay;
        javax.microedition.khronos.egl.EGLSurface mEglSurface;
        private WeakReference<GLSurfaceView> mGLSurfaceViewWeakRef;

        public EglHelper(WeakReference<GLSurfaceView> glSurfaceViewWeakRef) {
            this.mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        public void start() {
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
            }
            this.mEgl = (EGL10) javax.microedition.khronos.egl.EGLContext.getEGL();
            this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }
            int[] version = new int[2];
            Log.i("EglHelper", "eglInitialize = " + Thread.currentThread().getId() + ", this = " + this.mGLSurfaceViewWeakRef.get());
            if (!this.mEgl.eglInitialize(this.mEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed");
            }
            GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
            if (view == null) {
                this.mEglConfig = null;
                this.mEglContext = null;
            } else {
                Log.i("EglHelper", "chooseConfig = " + Thread.currentThread().getId() + ", this = " + view);
                this.mEglConfig = view.mEGLConfigChooser.chooseConfig(this.mEgl, this.mEglDisplay);
                this.mEglContext = view.mEGLContextFactory.createContext(this.mEgl, this.mEglDisplay, this.mEglConfig);
            }
            if (this.mEglContext == null || this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                this.mEglContext = null;
                throwEglException("createContext");
            }
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "createContext " + this.mEglContext + " tid=" + Thread.currentThread().getId());
            }
            this.mEglSurface = null;
        }

        public boolean createSurface() {
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
            }
            if (this.mEgl == null) {
                throw new RuntimeException("egl not initialized");
            }
            if (this.mEglDisplay == null) {
                throw new RuntimeException("eglDisplay not initialized");
            }
            if (this.mEglConfig == null) {
                throw new RuntimeException("mEglConfig not initialized");
            }
            destroySurfaceImp();
            GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
            if (view != null) {
                this.mEglSurface = view.mEGLWindowSurfaceFactory.createWindowSurface(this.mEgl, this.mEglDisplay, this.mEglConfig, view.getHolder());
            } else {
                this.mEglSurface = null;
            }
            if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                int error = this.mEgl.eglGetError();
                if (error == 12299) {
                    Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
                return false;
            }
            if (!this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", this.mEgl.eglGetError());
                return false;
            }
            return true;
        }

        GL createGL() {
            GL gl = this.mEglContext.getGL();
            GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
            if (view != null) {
                if (view.mGLWrapper != null) {
                    gl = view.mGLWrapper.wrap(gl);
                }
                if ((view.mDebugFlags & 3) != 0) {
                    int configFlags = 0;
                    Writer log = null;
                    if ((view.mDebugFlags & 1) != 0) {
                        configFlags = 1;
                    }
                    if ((view.mDebugFlags & 2) != 0) {
                        log = new LogWriter();
                    }
                    return GLDebugHelper.wrap(gl, configFlags, log);
                }
                return gl;
            }
            return gl;
        }

        public int swap() {
            if (!this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface)) {
                return this.mEgl.eglGetError();
            }
            return 12288;
        }

        public void destroySurface() {
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
            }
            destroySurfaceImp();
        }

        private void destroySurfaceImp() {
            if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                return;
            }
            this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
            if (view != null) {
                view.mEGLWindowSurfaceFactory.destroySurface(this.mEgl, this.mEglDisplay, this.mEglSurface);
            }
            this.mEglSurface = null;
        }

        public void finish() {
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
            }
            if (this.mEglContext != null) {
                GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mEGLContextFactory.destroyContext(this.mEgl, this.mEglDisplay, this.mEglContext);
                }
                this.mEglContext = null;
            }
            if (this.mEglDisplay == null) {
                return;
            }
            this.mEgl.eglTerminate(this.mEglDisplay);
            this.mEglDisplay = null;
        }

        private void throwEglException(String function) {
            throwEglException(function, this.mEgl.eglGetError());
        }

        public static void throwEglException(String function, int error) {
            String message = formatEglError(function, error);
            if (GLSurfaceView.LOG_THREADS) {
                Log.e("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + message);
            }
            throw new RuntimeException(message);
        }

        public static void logEglErrorAsWarning(String tag, String function, int error) {
            Log.w(tag, formatEglError(function, error));
        }

        public static String formatEglError(String function, int error) {
            return function + " failed: " + EGLLogWrapper.getErrorString(error);
        }
    }

    static class GLThread extends Thread {
        private EglHelper mEglHelper;
        private boolean mExited;
        private boolean mFinishedCreatingEglSurface;
        private WeakReference<GLSurfaceView> mGLSurfaceViewWeakRef;
        private boolean mHasSurface;
        private boolean mHaveEglContext;
        private boolean mHaveEglSurface;
        private boolean mPaused;
        private boolean mRenderComplete;
        private boolean mRequestPaused;
        private boolean mShouldExit;
        private boolean mShouldReleaseEglContext;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private ArrayList<Runnable> mEventQueue = new ArrayList<>();
        private boolean mSizeChanged = true;
        private int mWidth = 0;
        private int mHeight = 0;
        private boolean mRequestRender = true;
        private int mRenderMode = 1;
        private boolean mWantRenderNotification = false;
        private IPerfServiceWrapper mPerfServiceWrapper = new PerfServiceWrapper((Context) null);

        GLThread(WeakReference<GLSurfaceView> glSurfaceViewWeakRef) {
            this.mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        @Override
        public void run() {
            setName("GLThread " + getId());
            if (GLSurfaceView.LOG_THREADS) {
                Log.i("GLThread", "starting tid=" + getId());
            }
            Trace.traceBegin(2L, "GLSurfaceView-run");
            Trace.traceEnd(2L);
            try {
                guardedRun();
            } catch (InterruptedException e) {
            } finally {
                GLSurfaceView.sGLThreadManager.threadExiting(this);
            }
        }

        private void stopEglSurfaceLocked() {
            if (!this.mHaveEglSurface) {
                return;
            }
            this.mHaveEglSurface = false;
            this.mEglHelper.destroySurface();
        }

        private void stopEglContextLocked() {
            if (!this.mHaveEglContext) {
                return;
            }
            this.mEglHelper.finish();
            this.mHaveEglContext = false;
            GLSurfaceView.sGLThreadManager.releaseEglContextLocked(this);
        }

        private void guardedRun() throws InterruptedException {
            this.mEglHelper = new EglHelper(this.mGLSurfaceViewWeakRef);
            this.mHaveEglContext = false;
            this.mHaveEglSurface = false;
            this.mWantRenderNotification = false;
            GL10 gl = null;
            boolean createEglContext = false;
            boolean createEglSurface = false;
            boolean createGlInterface = false;
            boolean lostEglContext = false;
            boolean sizeChanged = false;
            boolean wantRenderNotification = false;
            boolean doRenderNotification = false;
            boolean askedToReleaseEglContext = false;
            int w = 0;
            int h = 0;
            Runnable runnableRemove = null;
            while (true) {
                try {
                    synchronized (GLSurfaceView.sGLThreadManager) {
                        while (!this.mShouldExit) {
                            if (this.mEventQueue.isEmpty()) {
                                boolean pausing = false;
                                if (this.mPaused != this.mRequestPaused) {
                                    pausing = this.mRequestPaused;
                                    this.mPaused = this.mRequestPaused;
                                    GLSurfaceView.sGLThreadManager.notifyAll();
                                    if (GLSurfaceView.LOG_PAUSE_RESUME) {
                                        Log.i("GLThread", "mPaused is now " + this.mPaused + " tid=" + getId());
                                    }
                                }
                                if (this.mShouldReleaseEglContext) {
                                    if (GLSurfaceView.LOG_SURFACE) {
                                        Log.i("GLThread", "releasing EGL context because asked to tid=" + getId());
                                    }
                                    stopEglSurfaceLocked();
                                    stopEglContextLocked();
                                    this.mShouldReleaseEglContext = false;
                                    askedToReleaseEglContext = true;
                                }
                                if (lostEglContext) {
                                    stopEglSurfaceLocked();
                                    stopEglContextLocked();
                                    lostEglContext = false;
                                }
                                if (pausing && this.mHaveEglSurface) {
                                    if (GLSurfaceView.LOG_SURFACE) {
                                        Log.i("GLThread", "releasing EGL surface because paused tid=" + getId());
                                    }
                                    stopEglSurfaceLocked();
                                }
                                if (pausing && this.mHaveEglContext) {
                                    GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
                                    boolean preserveEglContextOnPause = view == null ? false : view.mPreserveEGLContextOnPause;
                                    if (!preserveEglContextOnPause || GLSurfaceView.sGLThreadManager.shouldReleaseEGLContextWhenPausing()) {
                                        stopEglContextLocked();
                                        if (GLSurfaceView.LOG_SURFACE) {
                                            Log.i("GLThread", "releasing EGL context because paused tid=" + getId());
                                        }
                                    }
                                }
                                if (pausing && GLSurfaceView.sGLThreadManager.shouldTerminateEGLWhenPausing()) {
                                    this.mEglHelper.finish();
                                    if (GLSurfaceView.LOG_SURFACE) {
                                        Log.i("GLThread", "terminating EGL because paused tid=" + getId());
                                    }
                                }
                                if (!this.mHasSurface && !this.mWaitingForSurface) {
                                    if (GLSurfaceView.LOG_SURFACE) {
                                        Log.i("GLThread", "noticed surfaceView surface lost tid=" + getId());
                                    }
                                    if (this.mHaveEglSurface) {
                                        stopEglSurfaceLocked();
                                    }
                                    this.mWaitingForSurface = true;
                                    this.mSurfaceIsBad = false;
                                    GLSurfaceView.sGLThreadManager.notifyAll();
                                }
                                if (this.mHasSurface && this.mWaitingForSurface) {
                                    if (GLSurfaceView.LOG_SURFACE) {
                                        Log.i("GLThread", "noticed surfaceView surface acquired tid=" + getId());
                                    }
                                    this.mWaitingForSurface = false;
                                    GLSurfaceView.sGLThreadManager.notifyAll();
                                }
                                if (doRenderNotification) {
                                    if (GLSurfaceView.LOG_SURFACE) {
                                        Log.i("GLThread", "sending render notification tid=" + getId());
                                    }
                                    this.mWantRenderNotification = false;
                                    doRenderNotification = false;
                                    this.mRenderComplete = true;
                                    GLSurfaceView.sGLThreadManager.notifyAll();
                                }
                                if (readyToDraw()) {
                                    if (!this.mHaveEglContext) {
                                        if (askedToReleaseEglContext) {
                                            askedToReleaseEglContext = false;
                                        } else if (GLSurfaceView.sGLThreadManager.tryAcquireEglContextLocked(this)) {
                                            try {
                                                this.mEglHelper.start();
                                                this.mHaveEglContext = true;
                                                createEglContext = true;
                                                GLSurfaceView.sGLThreadManager.notifyAll();
                                            } catch (RuntimeException t) {
                                                GLSurfaceView.sGLThreadManager.releaseEglContextLocked(this);
                                                throw t;
                                            }
                                        }
                                    }
                                    if (this.mHaveEglContext && !this.mHaveEglSurface) {
                                        this.mHaveEglSurface = true;
                                        createEglSurface = true;
                                        createGlInterface = true;
                                        sizeChanged = true;
                                    }
                                    if (this.mHaveEglSurface) {
                                        if (this.mSizeChanged) {
                                            sizeChanged = true;
                                            w = this.mWidth;
                                            h = this.mHeight;
                                            this.mWantRenderNotification = true;
                                            if (GLSurfaceView.LOG_SURFACE) {
                                                Log.i("GLThread", "noticing that we want render notification tid=" + getId());
                                            }
                                            createEglSurface = true;
                                            this.mSizeChanged = false;
                                        }
                                        this.mRequestRender = false;
                                        GLSurfaceView.sGLThreadManager.notifyAll();
                                        if (this.mWantRenderNotification) {
                                            wantRenderNotification = true;
                                        }
                                    }
                                }
                                if (GLSurfaceView.LOG_THREADS) {
                                    Log.i("GLThread", "waiting tid=" + getId() + " mHaveEglContext: " + this.mHaveEglContext + " mHaveEglSurface: " + this.mHaveEglSurface + " mFinishedCreatingEglSurface: " + this.mFinishedCreatingEglSurface + " mPaused: " + this.mPaused + " mHasSurface: " + this.mHasSurface + " mSurfaceIsBad: " + this.mSurfaceIsBad + " mWaitingForSurface: " + this.mWaitingForSurface + " mWidth: " + this.mWidth + " mHeight: " + this.mHeight + " mRequestRender: " + this.mRequestRender + " mRenderMode: " + this.mRenderMode);
                                }
                                GLSurfaceView.sGLThreadManager.wait();
                            } else {
                                runnableRemove = this.mEventQueue.remove(0);
                            }
                        }
                        synchronized (GLSurfaceView.sGLThreadManager) {
                            stopEglSurfaceLocked();
                            stopEglContextLocked();
                        }
                        return;
                    }
                } catch (Throwable th) {
                    synchronized (GLSurfaceView.sGLThreadManager) {
                    }
                }
                if (runnableRemove == null) {
                    if (createEglSurface) {
                        if (GLSurfaceView.LOG_SURFACE) {
                            Log.w("GLThread", "egl createSurface");
                        }
                        if (this.mEglHelper.createSurface()) {
                            synchronized (GLSurfaceView.sGLThreadManager) {
                                this.mFinishedCreatingEglSurface = true;
                                GLSurfaceView.sGLThreadManager.notifyAll();
                            }
                            createEglSurface = false;
                        } else {
                            synchronized (GLSurfaceView.sGLThreadManager) {
                                this.mFinishedCreatingEglSurface = true;
                                this.mSurfaceIsBad = true;
                                GLSurfaceView.sGLThreadManager.notifyAll();
                            }
                        }
                        synchronized (GLSurfaceView.sGLThreadManager) {
                            stopEglSurfaceLocked();
                            stopEglContextLocked();
                            throw th;
                        }
                    }
                    if (createGlInterface) {
                        gl = (GL10) this.mEglHelper.createGL();
                        GLSurfaceView.sGLThreadManager.checkGLDriver(gl);
                        createGlInterface = false;
                    }
                    if (createEglContext) {
                        if (GLSurfaceView.LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceCreated start");
                        }
                        GLSurfaceView view2 = this.mGLSurfaceViewWeakRef.get();
                        if (view2 != null) {
                            try {
                                Trace.traceBegin(8L, "onSurfaceCreated");
                                view2.mRenderer.onSurfaceCreated(gl, this.mEglHelper.mEglConfig);
                                Trace.traceEnd(8L);
                            } finally {
                            }
                        }
                        createEglContext = false;
                        if (GLSurfaceView.LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceCreated end");
                        }
                    }
                    if (sizeChanged) {
                        if (GLSurfaceView.LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceChanged(" + w + ", " + h + ") start");
                        }
                        GLSurfaceView view3 = this.mGLSurfaceViewWeakRef.get();
                        if (view3 != null) {
                            try {
                                Trace.traceBegin(8L, "onSurfaceChanged");
                                view3.mRenderer.onSurfaceChanged(gl, w, h);
                                Trace.traceEnd(8L);
                            } finally {
                            }
                        }
                        sizeChanged = false;
                        if (GLSurfaceView.LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceChanged(" + w + ", " + h + ") end");
                        }
                    }
                    if (GLSurfaceView.LOG_RENDERER_DRAW_FRAME) {
                        Log.w("GLThread", "onDrawFrame Start tid=" + getId());
                    }
                    GLSurfaceView view4 = this.mGLSurfaceViewWeakRef.get();
                    if (view4 != null) {
                        try {
                            Trace.traceBegin(8L, "onDrawFrame");
                            this.mPerfServiceWrapper.notifyFrameUpdate(0);
                            view4.mRenderer.onDrawFrame(gl);
                            Trace.traceEnd(8L);
                        } finally {
                        }
                    }
                    if (GLSurfaceView.LOG_RENDERER_DRAW_FRAME) {
                        Log.w("GLThread", "onDrawFrame End tid=" + getId());
                    }
                    int swapError = this.mEglHelper.swap();
                    switch (swapError) {
                        case 12288:
                            if (wantRenderNotification) {
                                doRenderNotification = true;
                                wantRenderNotification = false;
                            }
                            break;
                        case EGL14.EGL_CONTEXT_LOST:
                            if (GLSurfaceView.LOG_SURFACE) {
                                Log.i("GLThread", "egl context lost tid=" + getId());
                            }
                            lostEglContext = true;
                            if (wantRenderNotification) {
                            }
                            break;
                        default:
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);
                            synchronized (GLSurfaceView.sGLThreadManager) {
                                this.mSurfaceIsBad = true;
                                GLSurfaceView.sGLThreadManager.notifyAll();
                            }
                            if (wantRenderNotification) {
                            }
                            break;
                    }
                } else {
                    runnableRemove.run();
                    runnableRemove = null;
                }
            }
        }

        public boolean ableToDraw() {
            if (this.mHaveEglContext && this.mHaveEglSurface) {
                return readyToDraw();
            }
            return false;
        }

        private boolean readyToDraw() {
            if (this.mPaused || !this.mHasSurface || this.mSurfaceIsBad || this.mWidth <= 0 || this.mHeight <= 0) {
                return false;
            }
            return this.mRequestRender || this.mRenderMode == 1;
        }

        public void setRenderMode(int renderMode) {
            if (renderMode < 0 || renderMode > 1) {
                throw new IllegalArgumentException("renderMode");
            }
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mRenderMode = renderMode;
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
        }

        public int getRenderMode() {
            int i;
            synchronized (GLSurfaceView.sGLThreadManager) {
                i = this.mRenderMode;
            }
            return i;
        }

        public void requestRender() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "requestRender start tid=" + getId());
                }
                Trace.traceBegin(2L, "requestRender");
                this.mRequestRender = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
                Trace.traceEnd(2L);
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "requestRender end tid=" + getId());
                }
            }
        }

        public void requestRenderAndWait() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (Thread.currentThread() == this) {
                    return;
                }
                this.mWantRenderNotification = true;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused && !this.mRenderComplete && ableToDraw()) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void surfaceCreated() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "surfaceCreated start tid=" + getId());
                }
                Trace.traceBegin(2L, "surfaceCreated");
                this.mHasSurface = true;
                this.mFinishedCreatingEglSurface = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (this.mWaitingForSurface && !this.mFinishedCreatingEglSurface && !this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Trace.traceEnd(2L);
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "surfaceCreated end tid=" + getId());
                }
            }
        }

        public void surfaceDestroyed() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "surfaceDestroyed start tid=" + getId());
                }
                Trace.traceBegin(2L, "surfaceDestroyed");
                this.mHasSurface = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mWaitingForSurface && !this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Trace.traceEnd(2L);
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "surfaceDestroyed end tid=" + getId());
                }
            }
        }

        public void onPause() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onPause start tid=" + getId());
                }
                Trace.traceBegin(2L, "onPause");
                this.mRequestPaused = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused) {
                    if (GLSurfaceView.LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onPause waiting for mPaused.");
                    }
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Trace.traceEnd(2L);
                if (GLSurfaceView.LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onPause end tid=" + getId());
                }
            }
        }

        public void onResume() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onResume start tid=" + getId());
                }
                Trace.traceBegin(2L, "onResume");
                this.mRequestPaused = false;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && this.mPaused && !this.mRenderComplete) {
                    if (GLSurfaceView.LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onResume waiting for !mPaused.");
                    }
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Trace.traceEnd(2L);
                if (GLSurfaceView.LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onResume end tid=" + getId());
                }
            }
        }

        public void onWindowResize(int w, int h) {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "onWindowResize start tid=" + getId());
                }
                Trace.traceBegin(2L, "onWindowResize");
                this.mWidth = w;
                this.mHeight = h;
                this.mSizeChanged = true;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                if (Thread.currentThread() == this) {
                    return;
                }
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused && !this.mRenderComplete && ableToDraw()) {
                    if (GLSurfaceView.LOG_SURFACE) {
                        Log.i("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
                    }
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Trace.traceEnd(2L);
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "onWindowResize end tid=" + getId());
                }
            }
        }

        public void requestExitAndWait() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "requestExitAndWait start tid=" + getId());
                }
                Trace.traceBegin(2L, "requestExotAndWait");
                this.mShouldExit = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Trace.traceEnd(2L);
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "requestExitAndWait end tid=" + getId());
                }
            }
        }

        public void requestReleaseEglContextLocked() {
            if (GLSurfaceView.LOG_THREADS) {
                Log.i("GLThread", "requestReleaseEglContextLocked start tid=" + getId());
            }
            Trace.traceBegin(2L, "requestReleaseEglContextLocked");
            this.mShouldReleaseEglContext = true;
            GLSurfaceView.sGLThreadManager.notifyAll();
            Trace.traceEnd(2L);
            if (!GLSurfaceView.LOG_THREADS) {
                return;
            }
            Log.i("GLThread", "requestReleaseEglContextLocked end tid=" + getId());
        }

        public void queueEvent(Runnable r) {
            if (r == null) {
                throw new IllegalArgumentException("r must not be null");
            }
            if (GLSurfaceView.LOG_THREADS) {
                Log.i("GLThread", "queueEvent start tid=" + getId() + " runnable=" + r);
            }
            Trace.traceBegin(2L, "queueEvent");
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mEventQueue.add(r);
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
            Trace.traceEnd(2L);
            if (!GLSurfaceView.LOG_THREADS) {
                return;
            }
            Log.i("GLThread", "queueEvent end tid=" + getId());
        }
    }

    static class LogWriter extends Writer {
        private StringBuilder mBuilder = new StringBuilder();

        LogWriter() {
        }

        @Override
        public void close() {
            flushBuilder();
        }

        @Override
        public void flush() {
            flushBuilder();
        }

        @Override
        public void write(char[] buf, int offset, int count) {
            for (int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if (c == '\n') {
                    flushBuilder();
                } else {
                    this.mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (this.mBuilder.length() <= 0) {
                return;
            }
            Log.v(GLSurfaceView.TAG, this.mBuilder.toString());
            this.mBuilder.delete(0, this.mBuilder.length());
        }
    }

    private void checkRenderThreadState() {
        if (this.mGLThread == null) {
        } else {
            throw new IllegalStateException("setRenderer has already been called for this instance.");
        }
    }

    private static class GLThreadManager {
        private static String TAG = "GLThreadManager";
        private static final int kGLES_20 = 131072;
        private static final String kMSM7K_RENDERER_PREFIX = "Q3Dimension MSM7500 ";
        private GLThread mEglOwner;
        private boolean mGLESDriverCheckComplete;
        private int mGLESVersion;
        private boolean mGLESVersionCheckComplete;
        private boolean mLimitedGLESContexts;
        private boolean mMultipleGLESContextsAllowed;

        GLThreadManager(GLThreadManager gLThreadManager) {
            this();
        }

        private GLThreadManager() {
        }

        public synchronized void threadExiting(GLThread thread) {
            if (GLSurfaceView.LOG_THREADS) {
                Log.i("GLThread", "exiting tid=" + thread.getId());
            }
            thread.mExited = true;
            if (this.mEglOwner == thread) {
                this.mEglOwner = null;
            }
            notifyAll();
        }

        public boolean tryAcquireEglContextLocked(GLThread thread) {
            if (this.mEglOwner == thread || this.mEglOwner == null) {
                this.mEglOwner = thread;
                notifyAll();
                return true;
            }
            checkGLESVersion();
            if (this.mMultipleGLESContextsAllowed) {
                return true;
            }
            if (this.mEglOwner != null) {
                this.mEglOwner.requestReleaseEglContextLocked();
                return false;
            }
            return false;
        }

        public void releaseEglContextLocked(GLThread thread) {
            if (this.mEglOwner == thread) {
                this.mEglOwner = null;
            }
            notifyAll();
        }

        public synchronized boolean shouldReleaseEGLContextWhenPausing() {
            return this.mLimitedGLESContexts;
        }

        public synchronized boolean shouldTerminateEGLWhenPausing() {
            checkGLESVersion();
            return !this.mMultipleGLESContextsAllowed;
        }

        public synchronized void checkGLDriver(GL10 gl) {
            synchronized (this) {
                if (!this.mGLESDriverCheckComplete) {
                    checkGLESVersion();
                    String renderer = gl.glGetString(7937);
                    if (this.mGLESVersion < 131072) {
                        this.mMultipleGLESContextsAllowed = !renderer.startsWith(kMSM7K_RENDERER_PREFIX);
                        notifyAll();
                    }
                    this.mLimitedGLESContexts = this.mMultipleGLESContextsAllowed ? false : true;
                    if (GLSurfaceView.LOG_SURFACE) {
                        Log.w(TAG, "checkGLDriver renderer = \"" + renderer + "\" multipleContextsAllowed = " + this.mMultipleGLESContextsAllowed + " mLimitedGLESContexts = " + this.mLimitedGLESContexts);
                    }
                    this.mGLESDriverCheckComplete = true;
                }
            }
        }

        private void checkGLESVersion() {
            if (this.mGLESVersionCheckComplete) {
                return;
            }
            this.mGLESVersion = SystemProperties.getInt("ro.opengles.version", 0);
            if (this.mGLESVersion >= 131072) {
                this.mMultipleGLESContextsAllowed = true;
            }
            if (GLSurfaceView.LOG_SURFACE) {
                Log.w(TAG, "checkGLESVersion mGLESVersion = " + this.mGLESVersion + " mMultipleGLESContextsAllowed = " + this.mMultipleGLESContextsAllowed);
            }
            this.mGLESVersionCheckComplete = true;
        }
    }

    private static void checkLogProperty() {
        String dumpString = SystemProperties.get(LOG_PROPERTY_NAME);
        if (dumpString != null) {
            if (dumpString.length() <= 0 || dumpString.length() > 7) {
                Log.d(TAG, "checkGLSurfaceViewlLogProperty get invalid command");
                return;
            }
            int logFilter = 0;
            try {
                logFilter = Integer.parseInt(dumpString, 2);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid format of propery string: " + dumpString);
            }
            LOG_ATTACH_DETACH = (logFilter & 1) == 1;
            LOG_THREADS = (logFilter & 2) == 2;
            LOG_PAUSE_RESUME = (logFilter & 4) == 4;
            LOG_SURFACE = (logFilter & 8) == 8;
            LOG_RENDERER = (logFilter & 16) == 16;
            LOG_RENDERER_DRAW_FRAME = (logFilter & 32) == 32;
            LOG_EGL = (logFilter & 64) == 64;
            Log.d(TAG, "checkGLSurfaceViewlLogProperty debug filter: ATTACH_DETACH=" + LOG_ATTACH_DETACH + ", THREADS=" + LOG_THREADS + ", PAUSE_RESUME=" + LOG_PAUSE_RESUME + ", SURFACE=" + LOG_SURFACE + ", RENDERER=" + LOG_RENDERER + ", RENDERER_DRAW_FRAME=" + LOG_RENDERER_DRAW_FRAME + ", EGL=" + LOG_EGL);
        }
    }
}
