package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;
import android.os.Process;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.GLES11Canvas;
import com.android.gallery3d.glrenderer.GLES20Canvas;
import com.android.gallery3d.glrenderer.UploadedTexture;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MotionEventHelper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class GLRootView extends GLSurfaceView implements GLSurfaceView.Renderer, GLRoot {
    private final ArrayList<CanvasAnimation> mAnimations;
    private GLCanvas mCanvas;
    private int mCompensation;
    private Matrix mCompensationMatrix;
    private GLView mContentView;
    private int mDisplayRotation;
    private boolean mFirstDraw;
    private int mFlags;
    private int mFrameCount;
    private long mFrameCountingStart;
    private boolean mFreeze;
    private final Condition mFreezeCondition;
    private GL11 mGL;
    private final ArrayDeque<GLRoot.OnGLIdleListener> mIdleListeners;
    private final IdleRunner mIdleRunner;
    private boolean mInDownState;
    private int mInvalidateColor;
    private OrientationSource mOrientationSource;
    private final ReentrantLock mRenderLock;
    private volatile boolean mRenderRequested;
    private Runnable mRequestRenderOnAnimationFrame;

    public GLRootView(Context context) {
        this(context, null);
    }

    public GLRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        this.mFlags |= 1;
        setBackgroundDrawable(null);
        setEGLContextClientVersion(ApiHelper.HAS_GLES20_REQUIRED ? 2 : 1);
        if (ApiHelper.USE_888_PIXEL_FORMAT) {
            setEGLConfigChooser(8, 8, 8, 0, 0, 0);
        } else {
            setEGLConfigChooser(5, 6, 5, 0, 0, 0);
        }
        setRenderer(this);
        if (ApiHelper.USE_888_PIXEL_FORMAT) {
            getHolder().setFormat(3);
        } else {
            getHolder().setFormat(4);
        }
    }

    @Override
    public void registerLaunchedAnimation(CanvasAnimation animation) {
        this.mAnimations.add(animation);
    }

    @Override
    public void addOnGLIdleListener(GLRoot.OnGLIdleListener listener) {
        synchronized (this.mIdleListeners) {
            this.mIdleListeners.addLast(listener);
            this.mIdleRunner.enable();
        }
    }

    @Override
    public void setContentPane(GLView content) {
        if (this.mContentView != content) {
            if (this.mContentView != null) {
                if (this.mInDownState) {
                    long now = SystemClock.uptimeMillis();
                    MotionEvent cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                    this.mContentView.dispatchTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                    this.mInDownState = false;
                }
                this.mContentView.detachFromRoot();
                BasicTexture.yieldAllTextures();
            }
            this.mContentView = content;
            if (content != null) {
                content.attachToRoot(this);
                requestLayoutContentPane();
            }
        }
    }

    @Override
    public void requestRender() {
        if (!this.mRenderRequested) {
            this.mRenderRequested = true;
            if (ApiHelper.HAS_POST_ON_ANIMATION) {
                postOnAnimation(this.mRequestRenderOnAnimationFrame);
            } else {
                super.requestRender();
            }
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
                if ((this.mFlags & 1) != 0) {
                    this.mFlags |= 2;
                    requestRender();
                }
            }
        } finally {
            this.mRenderLock.unlock();
        }
    }

    private void layoutContentPane() {
        int displayRotation;
        int compensation;
        this.mFlags &= -3;
        int w = getWidth();
        int h = getHeight();
        if (this.mOrientationSource != null) {
            displayRotation = this.mOrientationSource.getDisplayRotation();
            compensation = this.mOrientationSource.getCompensation();
        } else {
            displayRotation = 0;
            compensation = 0;
        }
        if (this.mCompensation != compensation) {
            this.mCompensation = compensation;
            if (this.mCompensation % 180 != 0) {
                this.mCompensationMatrix.setRotate(this.mCompensation);
                this.mCompensationMatrix.preTranslate((-w) / 2, (-h) / 2);
                this.mCompensationMatrix.postTranslate(h / 2, w / 2);
            } else {
                this.mCompensationMatrix.setRotate(this.mCompensation, w / 2, h / 2);
            }
        }
        this.mDisplayRotation = displayRotation;
        if (this.mCompensation % 180 != 0) {
            w = h;
            h = w;
        }
        Log.i("GLRootView", "layout content pane " + w + "x" + h + " (compensation " + this.mCompensation + ")");
        if (this.mContentView != null && w != 0 && h != 0) {
            this.mContentView.layout(0, 0, w, h);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            requestLayoutContentPane();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl1, EGLConfig config) {
        GL11 gl = (GL11) gl1;
        if (this.mGL != null) {
            Log.i("GLRootView", "GLObject has changed from " + this.mGL + " to " + gl);
        }
        this.mRenderLock.lock();
        try {
            this.mGL = gl;
            this.mCanvas = ApiHelper.HAS_GLES20_REQUIRED ? new GLES20Canvas() : new GLES11Canvas(gl);
            BasicTexture.invalidateAllTextures();
            this.mRenderLock.unlock();
            setRenderMode(0);
        } catch (Throwable th) {
            this.mRenderLock.unlock();
            throw th;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl1, int width, int height) {
        Log.i("GLRootView", "onSurfaceChanged: " + width + "x" + height + ", gl10: " + gl1.toString());
        Process.setThreadPriority(-4);
        GalleryUtils.setRenderThread();
        GL11 gl = (GL11) gl1;
        Utils.assertTrue(this.mGL == gl);
        this.mCanvas.setSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        AnimationTime.update();
        this.mRenderLock.lock();
        while (this.mFreeze) {
            this.mFreezeCondition.awaitUninterruptibly();
        }
        try {
            onDrawFrameLocked(gl);
            this.mRenderLock.unlock();
            if (this.mFirstDraw) {
                this.mFirstDraw = false;
                post(new Runnable() {
                    @Override
                    public void run() {
                        View root = GLRootView.this.getRootView();
                        View cover = root.findViewById(R.id.gl_root_cover);
                        cover.setVisibility(8);
                    }
                });
            }
        } catch (Throwable th) {
            this.mRenderLock.unlock();
            throw th;
        }
    }

    private void onDrawFrameLocked(GL10 gl) {
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
            long now = AnimationTime.get();
            int n = this.mAnimations.size();
            for (int i = 0; i < n; i++) {
                this.mAnimations.get(i).setStartTime(now);
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
    }

    private void rotateCanvas(int degrees) {
        if (degrees != 0) {
            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            this.mCanvas.translate(cx, cy);
            this.mCanvas.rotate(degrees, 0.0f, 0.0f, 1.0f);
            if (degrees % 180 != 0) {
                this.mCanvas.translate(-cy, -cx);
            } else {
                this.mCanvas.translate(-cx, -cy);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean handled = false;
        if (isEnabled()) {
            int action = event.getAction();
            if (action == 3 || action == 1) {
                this.mInDownState = false;
            } else if (this.mInDownState || action == 0) {
            }
            if (this.mCompensation != 0) {
                event = MotionEventHelper.transformEvent(event, this.mCompensationMatrix);
            }
            this.mRenderLock.lock();
            try {
                if (this.mContentView != null && this.mContentView.dispatchTouchEvent(event)) {
                    handled = true;
                }
                if (action == 0 && handled) {
                    this.mInDownState = true;
                }
            } finally {
                this.mRenderLock.unlock();
            }
        }
        return handled;
    }

    private class IdleRunner implements Runnable {
        private boolean mActive;

        private IdleRunner() {
            this.mActive = false;
        }

        @Override
        public void run() {
            synchronized (GLRootView.this.mIdleListeners) {
                this.mActive = false;
                if (!GLRootView.this.mIdleListeners.isEmpty()) {
                    GLRoot.OnGLIdleListener listener = (GLRoot.OnGLIdleListener) GLRootView.this.mIdleListeners.removeFirst();
                    GLRootView.this.mRenderLock.lock();
                    try {
                        boolean keepInQueue = listener.onGLIdle(GLRootView.this.mCanvas, GLRootView.this.mRenderRequested);
                        GLRootView.this.mRenderLock.unlock();
                        synchronized (GLRootView.this.mIdleListeners) {
                            if (keepInQueue) {
                                GLRootView.this.mIdleListeners.addLast(listener);
                                if (!GLRootView.this.mRenderRequested && !GLRootView.this.mIdleListeners.isEmpty()) {
                                    enable();
                                }
                            } else if (!GLRootView.this.mRenderRequested) {
                                enable();
                            }
                        }
                    } catch (Throwable th) {
                        GLRootView.this.mRenderLock.unlock();
                        throw th;
                    }
                }
            }
        }

        public void enable() {
            if (!this.mActive) {
                this.mActive = true;
                GLRootView.this.queueEvent(this);
            }
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
        super.onPause();
    }

    @Override
    public void setOrientationSource(OrientationSource source) {
        this.mOrientationSource = source;
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
        this.mFreeze = false;
        this.mFreezeCondition.signalAll();
        this.mRenderLock.unlock();
    }

    @Override
    @TargetApi(NotificationCompat.FLAG_AUTO_CANCEL)
    public void setLightsOutMode(boolean enabled) {
        if (ApiHelper.HAS_SET_SYSTEM_UI_VISIBILITY) {
            int flags = 0;
            if (enabled) {
                flags = 1;
                if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
                    flags = 1 | 260;
                }
            }
            setSystemUiVisibility(flags);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        unfreeze();
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        unfreeze();
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        unfreeze();
        super.surfaceDestroyed(holder);
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
}
