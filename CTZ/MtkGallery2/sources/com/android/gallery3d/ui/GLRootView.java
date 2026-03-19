package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;
import android.os.Process;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import com.android.gallery3d.R;
import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.GLES11Canvas;
import com.android.gallery3d.glrenderer.GLES20Canvas;
import com.android.gallery3d.glrenderer.StringTexture;
import com.android.gallery3d.glrenderer.UploadedTexture;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MotionEventHelper;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallerybasic.gl.GLIdleExecuter;
import com.mediatek.gallerybasic.gl.MBasicTexture;
import com.mediatek.gallerybasic.util.DebugUtils;
import com.mediatek.galleryportable.SystemPropertyUtils;
import com.mediatek.galleryportable.TraceHelper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class GLRootView extends GLSurfaceView implements GLSurfaceView.Renderer, GLRoot {
    private static final boolean DEBUG_DRAWING_STAT = false;
    private static final boolean DEBUG_FPS = false;
    private static final boolean DEBUG_INVALIDATE = false;
    private static final boolean DEBUG_PROFILE = false;
    private static final boolean DEBUG_PROFILE_SLOW_ONLY = false;
    private static final int FLAG_INITIALIZED = 1;
    private static final int FLAG_NEED_LAYOUT = 2;
    private static boolean FPS_PERFORMANCE = false;
    private static final int FRAME_ID_POSITION_X = 0;
    private static final int FRAME_ID_POSITION_Y = 300;
    private static final int FRAME_ID_TEXT_SIZE = 100;
    private static final String TAG = "Gallery2/GLRootView";
    private final ArrayList<CanvasAnimation> mAnimations;
    private GLCanvas mCanvas;
    private int mCompensation;
    private Matrix mCompensationMatrix;
    private GLView mContentView;
    private int mCurrentFrameId;
    private StringTexture mCurrentFrameIdTexture;
    private DebugThread mDebugThread;
    private int mDisplayRotation;
    private boolean mFirstDraw;
    private int mFlags;
    private int mFrameCount;
    private long mFrameCountingStart;
    private boolean mFreeze;
    private final Condition mFreezeCondition;
    private GL11 mGL;
    private GLIdleExecuter mGLIdleExecuter;
    private boolean mGLSurfaceViewPaused;
    private int mHeight;
    private final ArrayDeque<GLRoot.OnGLIdleListener> mIdleListeners;
    private final IdleRunner mIdleRunner;
    private boolean mInDownState;
    private int mInvalidateColor;
    private long mLastDrawFinishTime;
    private OrientationSource mOrientationSource;
    private final ReentrantLock mRenderLock;
    private volatile boolean mRenderRequested;
    private Runnable mRequestRenderOnAnimationFrame;
    private boolean mSupportSW3D;
    private boolean mSurfaceDestroyed;
    private int mWidth;

    public GLRootView(Context context) {
        this(context, null);
        TraceHelper.beginSection(">>>>GLRootView-new1-done");
        TraceHelper.endSection();
    }

    public GLRootView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFrameCount = 0;
        this.mFrameCountingStart = 0L;
        this.mInvalidateColor = 0;
        this.mCompensationMatrix = new Matrix();
        this.mFlags = 2;
        this.mRenderRequested = false;
        this.mAnimations = new ArrayList<>();
        this.mIdleListeners = new ArrayDeque<>();
        this.mIdleRunner = new IdleRunner();
        this.mRenderLock = new ReentrantLock();
        this.mFreezeCondition = this.mRenderLock.newCondition();
        this.mInDownState = false;
        this.mFirstDraw = true;
        this.mRequestRenderOnAnimationFrame = new Runnable() {
            @Override
            public void run() {
                GLRootView.this.superRequestRender();
            }
        };
        this.mGLIdleExecuter = null;
        this.mSurfaceDestroyed = true;
        this.mGLSurfaceViewPaused = false;
        this.mCurrentFrameId = 0;
        TraceHelper.beginSection(">>>>GLRootView-new2-super-done");
        TraceHelper.endSection();
        TraceHelper.beginSection(">>>>GLRootView-new2-other");
        this.mFlags |= 1;
        setBackgroundDrawable(null);
        if (FeatureConfig.SUPPORT_EMULATOR) {
            EGL10 egl10 = (EGL10) EGLContext.getEGL();
            TraceHelper.beginSection(">>>>GLRootView-new2-eglGetDisplay");
            EGLDisplay eGLDisplayEglGetDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>GLRootView-new2-eglInitialize");
            egl10.eglInitialize(eGLDisplayEglGetDisplay, null);
            TraceHelper.endSection();
            if (eGLDisplayEglGetDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }
            this.mSupportSW3D = egl10.eglQueryString(eGLDisplayEglGetDisplay, 12372).startsWith("2");
            Log.d(TAG, "ro.kernel.qemu.gles = " + SchemaSymbols.ATTVAL_TRUE_1.equals(SystemPropertyUtils.get("ro.kernel.qemu.gles")) + ", supportSW3D = " + this.mSupportSW3D);
            TraceHelper.beginSection(">>>>GLRootView-new2-setEGLContextClientVersion");
            if (!ApiHelper.HAS_GLES20_REQUIRED || (!this.mSupportSW3D && !SchemaSymbols.ATTVAL_TRUE_1.equals(SystemPropertyUtils.get("ro.kernel.qemu.gles")))) {
                i = 1;
            }
            setEGLContextClientVersion(i);
            TraceHelper.endSection();
        } else {
            TraceHelper.beginSection(">>>>GLRootView-new2-setEGLContextClientVersion");
            setEGLContextClientVersion(ApiHelper.HAS_GLES20_REQUIRED ? 2 : 1);
            TraceHelper.endSection();
        }
        if (ApiHelper.USE_888_PIXEL_FORMAT) {
            TraceHelper.beginSection(">>>>GLRootView-new2-setEGLConfigChooser");
            setEGLConfigChooser(8, 8, 8, 0, 0, 0);
            TraceHelper.endSection();
        } else {
            TraceHelper.beginSection(">>>>GLRootView-new2-setEGLConfigChooser");
            setEGLConfigChooser(5, 6, 5, 0, 0, 0);
            TraceHelper.endSection();
        }
        TraceHelper.beginSection(">>>>GLRootView-new2-setRenderer");
        setRenderer(this);
        TraceHelper.endSection();
        if (ApiHelper.USE_888_PIXEL_FORMAT) {
            TraceHelper.beginSection(">>>>GLRootView-new2-setFormat");
            getHolder().setFormat(3);
            TraceHelper.endSection();
        } else {
            TraceHelper.beginSection(">>>>GLRootView-new2-setFormat");
            getHolder().setFormat(4);
            TraceHelper.endSection();
        }
        TraceHelper.endSection();
    }

    @Override
    public void registerLaunchedAnimation(CanvasAnimation canvasAnimation) {
        this.mAnimations.add(canvasAnimation);
    }

    @Override
    public void addOnGLIdleListener(GLRoot.OnGLIdleListener onGLIdleListener) {
        synchronized (this.mIdleListeners) {
            this.mIdleListeners.addLast(onGLIdleListener);
            this.mIdleRunner.enable();
        }
    }

    @Override
    public void setContentPane(GLView gLView) {
        if (this.mContentView == gLView) {
            return;
        }
        if (this.mContentView != null) {
            if (this.mInDownState) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                MotionEvent motionEventObtain = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 3, 0.0f, 0.0f, 0);
                this.mContentView.dispatchTouchEvent(motionEventObtain);
                motionEventObtain.recycle();
                this.mInDownState = false;
            }
            this.mContentView.detachFromRoot();
            BasicTexture.yieldAllTextures();
            MBasicTexture.yieldAllTextures();
        }
        this.mContentView = gLView;
        if (gLView != null) {
            gLView.attachToRoot(this);
            requestLayoutContentPane();
        }
    }

    @Override
    public void requestRenderForced() {
        superRequestRender();
    }

    @Override
    public void requestRender() {
        if (this.mRenderRequested) {
            return;
        }
        this.mRenderRequested = true;
        if (ApiHelper.HAS_POST_ON_ANIMATION) {
            postOnAnimation(this.mRequestRenderOnAnimationFrame);
        } else {
            super.requestRender();
        }
    }

    private void superRequestRender() {
        super.requestRender();
    }

    @Override
    public void requestLayoutContentPane() {
        this.mRenderLock.lock();
        try {
            if (this.mContentView != null && (this.mFlags & 2) == 0) {
                if ((this.mFlags & 1) == 0) {
                    return;
                }
                this.mFlags |= 2;
                requestRender();
            }
        } finally {
            this.mRenderLock.unlock();
        }
    }

    private void layoutContentPane() {
        int displayRotation;
        int compensation;
        this.mFlags &= -3;
        int width = getWidth();
        int height = getHeight();
        if (this.mOrientationSource != null) {
            displayRotation = this.mOrientationSource.getDisplayRotation();
            compensation = this.mOrientationSource.getCompensation();
        } else {
            displayRotation = 0;
            compensation = 0;
        }
        if (this.mCompensation != compensation || this.mWidth != width || this.mHeight != height) {
            this.mWidth = width;
            this.mHeight = height;
            this.mCompensation = compensation;
            if (this.mCompensation % 180 != 0) {
                this.mCompensationMatrix.setRotate(this.mCompensation);
                this.mCompensationMatrix.preTranslate((-width) / 2, (-height) / 2);
                this.mCompensationMatrix.postTranslate(height / 2, width / 2);
            } else {
                this.mCompensationMatrix.setRotate(this.mCompensation, width / 2, height / 2);
            }
        }
        this.mDisplayRotation = displayRotation;
        if (this.mCompensation % 180 != 0) {
            height = width;
            width = height;
        }
        Log.i(TAG, "layout content pane " + width + "x" + height + " (compensation " + this.mCompensation + ")");
        if (this.mContentView != null && width != 0 && height != 0) {
            this.mContentView.layout(0, 0, width, height);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        TraceHelper.beginSection(">>>>GLRootView-onLayout");
        if (z) {
            requestLayoutContentPane();
        }
        TraceHelper.endSection();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        TraceHelper.beginSection(">>>>GLRootView-onSurfaceCreated");
        Log.d(TAG, "<onSurfaceCreated> gl1 = " + gl10);
        GL11 gl11 = (GL11) gl10;
        if (this.mGL != null) {
            Log.i(TAG, "GLObject has changed from " + this.mGL + " to " + gl11);
        }
        this.mRenderLock.lock();
        try {
            this.mGL = gl11;
            if (FeatureConfig.SUPPORT_EMULATOR) {
                this.mCanvas = (ApiHelper.HAS_GLES20_REQUIRED && (this.mSupportSW3D || SchemaSymbols.ATTVAL_TRUE_1.equals(SystemPropertyUtils.get("ro.kernel.qemu.gles")))) ? new GLES20Canvas() : new GLES11Canvas(gl11);
            } else {
                this.mCanvas = ApiHelper.HAS_GLES20_REQUIRED ? new GLES20Canvas() : new GLES11Canvas(gl11);
            }
            BasicTexture.invalidateAllTextures();
            MBasicTexture.invalidateAllTextures();
            this.mRenderLock.unlock();
            setRenderMode(0);
            if (this.mGLIdleExecuter != null) {
                this.mGLIdleExecuter.setCanvas(this.mCanvas.getMGLCanvas());
            }
            TraceHelper.endSection();
        } catch (Throwable th) {
            this.mRenderLock.unlock();
            throw th;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i2) {
        Log.i(TAG, "onSurfaceChanged: " + i + "x" + i2 + ", gl10: " + gl10.toString());
        TraceHelper.beginSection(">>>>GLRootView-onSurfaceChanged");
        Process.setThreadPriority(-4);
        GalleryUtils.setRenderThread();
        Utils.assertTrue(this.mGL == ((GL11) gl10));
        this.mCanvas.setSize(i, i2);
        TraceHelper.endSection();
    }

    private void outputFps() {
        long jNanoTime = System.nanoTime();
        if (this.mFrameCountingStart == 0) {
            this.mFrameCountingStart = jNanoTime;
        } else if (jNanoTime - this.mFrameCountingStart > 1000000000) {
            Log.d(TAG, "fps: " + ((((double) this.mFrameCount) * 1.0E9d) / (jNanoTime - this.mFrameCountingStart)));
            this.mFrameCountingStart = jNanoTime;
            this.mFrameCount = 0;
        } else if (FPS_PERFORMANCE && this.mFrameCount != 0) {
            Log.d("Gallery2PerformanceTestCase2", "[Performance Auto Test] Gallery render fps = " + (((long) (this.mFrameCount * 1000000000)) / (jNanoTime - this.mFrameCountingStart)));
            this.mFrameCountingStart = jNanoTime;
            this.mFrameCount = 0;
        }
        this.mFrameCount++;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (this.mSurfaceDestroyed) {
            Log.d(TAG, "<onDrawFrame> mSurfaceDestroyed = true, no render, return");
            return;
        }
        AnimationTime.update();
        this.mRenderLock.lock();
        while (this.mFreeze) {
            try {
                this.mFreezeCondition.awaitUninterruptibly();
            } catch (Throwable th) {
                this.mRenderLock.unlock();
                throw th;
            }
        }
        onDrawFrameLocked(gl10);
        this.mRenderLock.unlock();
        if (this.mFirstDraw) {
            this.mFirstDraw = false;
            post(new Runnable() {
                @Override
                public void run() {
                    GLRootView.this.getRootView().findViewById(R.id.gl_root_cover).setVisibility(8);
                }
            });
        }
    }

    private void onDrawFrameLocked(GL10 gl10) {
        if (FPS_PERFORMANCE) {
            outputFps();
        }
        this.mCanvas.deleteRecycledResources();
        UploadedTexture.resetUploadLimit();
        this.mRenderRequested = false;
        if ((this.mOrientationSource != null && this.mDisplayRotation != this.mOrientationSource.getDisplayRotation()) || (this.mFlags & 2) != 0) {
            layoutContentPane();
        }
        this.mCanvas.save(-1);
        rotateCanvas(-this.mCompensation);
        if (this.mContentView != null) {
            this.mContentView.render(this.mCanvas);
        } else {
            this.mCanvas.clearBuffer();
        }
        this.mCanvas.restore();
        if (!this.mAnimations.isEmpty()) {
            long j = AnimationTime.get();
            int size = this.mAnimations.size();
            for (int i = 0; i < size; i++) {
                this.mAnimations.get(i).setStartTime(j);
            }
            this.mAnimations.clear();
        }
        if (UploadedTexture.uploadLimitReached()) {
            requestRender();
        }
        synchronized (this.mIdleListeners) {
            if (!this.mIdleListeners.isEmpty()) {
                this.mIdleRunner.enable();
            }
        }
        if (this.mGLIdleExecuter != null) {
            this.mGLIdleExecuter.onRenderComplete();
        }
        if (DebugUtils.DEBUG_RENDER) {
            renderFrameId();
        }
    }

    private void rotateCanvas(int i) {
        if (i == 0) {
            return;
        }
        this.mCanvas.translate(getWidth() / 2, getHeight() / 2);
        this.mCanvas.rotate(i, 0.0f, 0.0f, 1.0f);
        if (i % 180 != 0) {
            this.mCanvas.translate(-r1, -r0);
        } else {
            this.mCanvas.translate(-r0, -r1);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        boolean z = false;
        if (!isEnabled()) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action == 3 || action == 1) {
            this.mInDownState = false;
        } else if (!this.mInDownState && action != 0) {
            return false;
        }
        if (this.mCompensation != 0) {
            motionEvent = MotionEventHelper.transformEvent(motionEvent, this.mCompensationMatrix);
        }
        this.mRenderLock.lock();
        try {
            if (this.mContentView != null && this.mContentView.dispatchTouchEvent(motionEvent)) {
                z = true;
            }
            if (action == 0 && z) {
                this.mInDownState = true;
            }
            return z;
        } finally {
            this.mRenderLock.unlock();
        }
    }

    private class IdleRunner implements Runnable {
        private boolean mActive;

        private IdleRunner() {
            this.mActive = false;
        }

        @Override
        public void run() {
            if (GLRootView.this.mSurfaceDestroyed || GLRootView.this.mGLSurfaceViewPaused) {
                Log.d(GLRootView.TAG, "<IdleRunner.run> mSurfaceDestroyed " + GLRootView.this.mSurfaceDestroyed + ", mGLSurfaceViewPaused " + GLRootView.this.mGLSurfaceViewPaused + ", return");
                this.mActive = false;
                return;
            }
            synchronized (GLRootView.this.mIdleListeners) {
                this.mActive = false;
                if (GLRootView.this.mIdleListeners.isEmpty()) {
                    return;
                }
                GLRoot.OnGLIdleListener onGLIdleListener = (GLRoot.OnGLIdleListener) GLRootView.this.mIdleListeners.removeFirst();
                GLRootView.this.mRenderLock.lock();
                try {
                    boolean zOnGLIdle = onGLIdleListener.onGLIdle(GLRootView.this.mCanvas, GLRootView.this.mRenderRequested);
                    GLRootView.this.mRenderLock.unlock();
                    synchronized (GLRootView.this.mIdleListeners) {
                        if (zOnGLIdle) {
                            try {
                                GLRootView.this.mIdleListeners.addLast(onGLIdleListener);
                            } catch (Throwable th) {
                                throw th;
                            }
                        }
                        if (!GLRootView.this.mRenderRequested && !GLRootView.this.mIdleListeners.isEmpty()) {
                            enable();
                        }
                    }
                } catch (Throwable th2) {
                    GLRootView.this.mRenderLock.unlock();
                    throw th2;
                }
            }
        }

        public void enable() {
            if (this.mActive) {
                return;
            }
            this.mActive = true;
            GLRootView.this.queueEvent(this);
        }
    }

    @Override
    public void lockRenderThread() {
        this.mRenderLock.lock();
    }

    @Override
    public void unlockRenderThread() {
        this.mRenderLock.unlock();
    }

    @Override
    public void onPause() {
        unfreeze();
        this.mGLSurfaceViewPaused = true;
        Log.d(TAG, "<onPause> set mGLSurfaceViewPaused as true");
        super.onPause();
    }

    @Override
    public void setOrientationSource(OrientationSource orientationSource) {
        this.mOrientationSource = orientationSource;
    }

    @Override
    public int getDisplayRotation() {
        return this.mDisplayRotation;
    }

    @Override
    public int getCompensation() {
        return this.mCompensation;
    }

    @Override
    public Matrix getCompensationMatrix() {
        return this.mCompensationMatrix;
    }

    @Override
    public void freeze() {
        this.mRenderLock.lock();
        this.mFreeze = true;
        this.mRenderLock.unlock();
    }

    @Override
    public void unfreeze() {
        this.mRenderLock.lock();
        try {
            this.mFreeze = false;
            this.mFreezeCondition.signalAll();
        } finally {
            this.mRenderLock.unlock();
        }
    }

    @Override
    @TargetApi(16)
    public void setLightsOutMode(boolean z) {
        if (ApiHelper.HAS_SET_SYSTEM_UI_VISIBILITY) {
            int i = 0;
            if (z) {
                i = 1;
                if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
                    i = 261;
                }
            }
            setSystemUiVisibility(i);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        Log.d(TAG, "<surfaceChanged> format = " + i + ", w = " + i2 + ", h = " + i3);
        unfreeze();
        super.surfaceChanged(surfaceHolder, i, i2, i3);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "<surfaceCreated> set mSurfaceDestroyed = false");
        this.mSurfaceDestroyed = false;
        unfreeze();
        super.surfaceCreated(surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "<surfaceDestroyed> set mSurfaceDestroyed = true");
        this.mSurfaceDestroyed = true;
        unfreeze();
        super.surfaceDestroyed(surfaceHolder);
    }

    @Override
    protected void onDetachedFromWindow() {
        unfreeze();
        super.onDetachedFromWindow();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            unfreeze();
        } finally {
            super.finalize();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mGLSurfaceViewPaused = false;
        Log.d(TAG, "<onResume> set mGLSurfaceViewPaused as false");
    }

    public GLIdleExecuter getGLIdleExecuter() {
        if (this.mGLIdleExecuter == null) {
            this.mGLIdleExecuter = new GLIdleExecuter(this, new MyGLIdleExecuter());
        }
        if (this.mCanvas != null) {
            this.mGLIdleExecuter.setCanvas(this.mCanvas.getMGLCanvas());
        }
        return this.mGLIdleExecuter;
    }

    private class MyGLIdleExecuter implements GLIdleExecuter.IGLSurfaceViewStatusGetter {
        private MyGLIdleExecuter() {
        }

        @Override
        public boolean isRenderRequested() {
            return GLRootView.this.mRenderRequested;
        }

        @Override
        public boolean isSurfaceDestroyed() {
            return GLRootView.this.mSurfaceDestroyed;
        }
    }

    public void dispatchKeyEventView(KeyEvent keyEvent) {
        this.mRenderLock.lock();
        try {
            if (this.mContentView != null) {
                this.mContentView.dispatchKeyEvent(keyEvent);
            }
        } finally {
            this.mRenderLock.unlock();
        }
    }

    public void setFpsPerformance(boolean z) {
        Log.d(TAG, "[Performance Auto Test] setFpsPerformance = " + z);
        FPS_PERFORMANCE = z;
        if (!z) {
            this.mFrameCountingStart = 0L;
            this.mFrameCount = 0;
        }
    }

    private void renderFrameId() {
        if (this.mCurrentFrameIdTexture != null) {
            this.mCurrentFrameIdTexture.recycle();
        }
        this.mCurrentFrameIdTexture = StringTexture.newInstance(String.valueOf(this.mCurrentFrameId), 100.0f, -65536);
        this.mCanvas.drawTexture(this.mCurrentFrameIdTexture, 0, FRAME_ID_POSITION_Y, this.mCurrentFrameIdTexture.getWidth(), this.mCurrentFrameIdTexture.getHeight());
        this.mCurrentFrameId++;
    }

    public class DebugThread extends Thread {
        int mTime;

        public DebugThread(String str) {
            super(str);
            this.mTime = 0;
        }

        @Override
        public void run() {
            Log.d(GLRootView.TAG, "<DebugThread> start");
            while (true) {
                try {
                    this.mTime++;
                    Log.d(GLRootView.TAG, "<DebugThread> time = " + this.mTime + ", begin #######################");
                    StringBuilder sb = new StringBuilder();
                    sb.append("<DebugThread> mRenderLock = ");
                    sb.append(GLRootView.this.mRenderLock);
                    Log.d(GLRootView.TAG, sb.toString());
                    Log.d(GLRootView.TAG, "<DebugThread> mRenderLock.getHoldCount() = " + GLRootView.this.mRenderLock.getHoldCount());
                    Log.d(GLRootView.TAG, "<DebugThread> mRenderLock.getQueueLength() = " + GLRootView.this.mRenderLock.getQueueLength());
                    Log.d(GLRootView.TAG, "<DebugThread> mRenderLock.hasQueuedThreads() = " + GLRootView.this.mRenderLock.hasQueuedThreads());
                    Log.d(GLRootView.TAG, "<DebugThread> mRenderLock.isFair() = " + GLRootView.this.mRenderLock.isFair());
                    Log.d(GLRootView.TAG, "<DebugThread> mRenderLock.isHeldByCurrentThread() = " + GLRootView.this.mRenderLock.isHeldByCurrentThread());
                    Log.d(GLRootView.TAG, "<DebugThread> mRenderLock.isLocked() = " + GLRootView.this.mRenderLock.isLocked());
                    Log.d(GLRootView.TAG, "<DebugThread> time = " + this.mTime + ", end #########################");
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Log.d(GLRootView.TAG, "<DebugThread> Interrupted, " + e.getMessage());
                    Log.d(GLRootView.TAG, "<DebugThread> stop");
                    return;
                }
            }
        }
    }

    public void startDebug() {
        this.mDebugThread = new DebugThread("DebugThread");
        this.mDebugThread.start();
        Log.d(TAG, "<startDebug> new DebugThread");
    }

    public void stopDebug() {
        if (this.mDebugThread != null) {
            this.mDebugThread.interrupt();
            this.mDebugThread = null;
        }
        Log.d(TAG, "<stopDebug> interrupt DebugThread");
    }
}
