package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.HandlerThread;
import android.os.Process;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.ExtTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.galleryportable.SystemPropertyUtils;

@TargetApi(11)
public abstract class SurfaceTextureScreenNail implements SurfaceTexture.OnFrameAvailableListener, ScreenNail {
    private static final int GL_TEXTURE_EXTERNAL_OES = 36197;
    protected static final int INTERVALS = 60;
    private static final String TAG = "Gallery2/SurfaceTextureScreenNail";
    protected boolean mDebug;
    protected boolean mDebugLevel2;
    protected ExtTexture mExtTexture;
    private int mHeight;
    private SurfaceTexture mSurfaceTexture;
    private int mWidth;
    private static int sMaxHightProrityFrameCount = 8;
    private static HandlerThread sFrameListener = new HandlerThread("FrameListener");
    private float[] mTransform = new float[16];
    private boolean mHasTexture = false;
    protected int mDebugFlag = SystemPropertyUtils.getInt("cam.debug", 0);
    protected int mDrawFrameCount = 0;
    protected int mRequestCount = 0;
    protected long mRequestStartTime = 0;
    protected long mDrawStartTime = 0;
    private int mCurrentFrameCount = 0;

    @Override
    public abstract void noDraw();

    @Override
    public abstract void onFrameAvailable(SurfaceTexture surfaceTexture);

    @Override
    public abstract void recycle();

    public SurfaceTextureScreenNail() {
        this.mDebug = false;
        this.mDebugLevel2 = false;
        this.mDebug = this.mDebugFlag > 0;
        this.mDebugLevel2 = this.mDebugFlag > 1;
    }

    public void acquireSurfaceTexture(GLCanvas gLCanvas) {
        this.mExtTexture = new ExtTexture(gLCanvas, GL_TEXTURE_EXTERNAL_OES);
        this.mExtTexture.setSize(this.mWidth, this.mHeight);
        if (!sFrameListener.isAlive()) {
            sFrameListener.start();
        }
        com.mediatek.gallery3d.util.Log.e(TAG, "<acquireSurfaceTexture> stscrnail construct surfacetexture with id " + this.mExtTexture.getId());
        this.mSurfaceTexture = new SurfaceTexture(this.mExtTexture.getId());
        initializePriority();
        setDefaultBufferSize(this.mSurfaceTexture, this.mWidth, this.mHeight);
        this.mSurfaceTexture.setOnFrameAvailableListener(this);
        synchronized (this) {
            this.mHasTexture = true;
        }
    }

    public void acquireSurfaceTexture() {
        if (this.mExtTexture == null) {
            this.mExtTexture = new ExtTexture(GL_TEXTURE_EXTERNAL_OES);
        }
        this.mExtTexture.setSize(this.mWidth, this.mHeight);
        if (!sFrameListener.isAlive()) {
            sFrameListener.start();
        }
        this.mSurfaceTexture = new SurfaceTexture(this.mExtTexture.getId());
        initializePriority();
        setDefaultBufferSize(this.mSurfaceTexture, this.mWidth, this.mHeight);
        this.mSurfaceTexture.setOnFrameAvailableListener(this);
        synchronized (this) {
            this.mHasTexture = true;
        }
    }

    @TargetApi(15)
    private static void setDefaultBufferSize(SurfaceTexture surfaceTexture, int i, int i2) {
        if (ApiHelper.HAS_SET_DEFALT_BUFFER_SIZE) {
            surfaceTexture.setDefaultBufferSize(i, i2);
        }
    }

    @TargetApi(14)
    private static void releaseSurfaceTexture(SurfaceTexture surfaceTexture) {
        surfaceTexture.setOnFrameAvailableListener(null);
        if (ApiHelper.HAS_RELEASE_SURFACE_TEXTURE) {
            surfaceTexture.release();
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mSurfaceTexture;
    }

    public void releaseSurfaceTexture(boolean z) {
        com.mediatek.gallery3d.util.Log.d(TAG, "<releaseSurfaceTexture> releaseSurfaceTexture needReleaseExtTexture = " + z);
        if (z) {
            synchronized (this) {
                this.mHasTexture = false;
            }
            this.mExtTexture.recycle();
            this.mExtTexture = null;
        }
        releaseSurfaceTexture(this.mSurfaceTexture);
        this.mSurfaceTexture = null;
    }

    public void fullHandlerCapacity() {
        if (!FeatureConfig.SUPPORT_EMULATOR) {
            com.mediatek.gallery3d.util.Log.d(TAG, "<fullHandlerCapacity> set urgent display");
            Process.setThreadPriority(sFrameListener.getThreadId(), -8);
        }
    }

    public void normalHandlerCapacity() {
        if (!FeatureConfig.SUPPORT_EMULATOR) {
            com.mediatek.gallery3d.util.Log.d(TAG, "<normalHandlerCapacity> set normal");
            Process.setThreadPriority(sFrameListener.getThreadId(), 0);
        }
    }

    private void initializePriority() {
        fullHandlerCapacity();
        this.mCurrentFrameCount = 0;
    }

    private void checkThreadPriority() {
        if (this.mCurrentFrameCount == sMaxHightProrityFrameCount) {
            normalHandlerCapacity();
            this.mCurrentFrameCount++;
        } else if (this.mCurrentFrameCount < sMaxHightProrityFrameCount) {
            this.mCurrentFrameCount++;
        }
    }

    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
    }

    public void resizeTexture() {
        if (this.mExtTexture != null) {
            this.mExtTexture.setSize(this.mWidth, this.mHeight);
            setDefaultBufferSize(this.mSurfaceTexture, this.mWidth, this.mHeight);
        }
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }

    @Override
    public void draw(GLCanvas gLCanvas, int i, int i2, int i3, int i4) {
        synchronized (this) {
            if (this.mHasTexture) {
                checkThreadPriority();
                this.mSurfaceTexture.updateTexImage();
                this.mSurfaceTexture.getTransformMatrix(this.mTransform);
                gLCanvas.save(2);
                gLCanvas.translate((i3 / 2) + i, (i4 / 2) + i2);
                gLCanvas.scale(1.0f, -1.0f, 1.0f);
                gLCanvas.translate(-r0, -r1);
                updateTransformMatrix(this.mTransform);
                gLCanvas.drawTexture(this.mExtTexture, this.mTransform, i, i2, i3, i4);
                gLCanvas.restore();
                if (this.mDebug) {
                    if (this.mDebugLevel2) {
                        com.mediatek.gallery3d.util.Log.d(TAG, "<draw> GLCanvas drawing Frame");
                    }
                    this.mDrawFrameCount++;
                    if (this.mDrawFrameCount % INTERVALS == 0) {
                        long jCurrentTimeMillis = System.currentTimeMillis();
                        int i5 = (int) (jCurrentTimeMillis - this.mDrawStartTime);
                        com.mediatek.gallery3d.util.Log.d(TAG, "<draw> Drawing frame, fps = " + ((this.mDrawFrameCount * 1000.0f) / i5) + " in last " + i5 + " millisecond.");
                        this.mDrawStartTime = jCurrentTimeMillis;
                        this.mDrawFrameCount = 0;
                    }
                }
            }
        }
    }

    @Override
    public void draw(GLCanvas gLCanvas, RectF rectF, RectF rectF2) {
        com.mediatek.gallery3d.util.Log.d(TAG, "<draw> draw fails!!!");
    }

    protected void updateTransformMatrix(float[] fArr) {
    }

    @Override
    public MediaItem getMediaItem() {
        return null;
    }

    @Override
    public boolean isAnimating() {
        return false;
    }
}
