package com.android.photos.views;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.widget.FrameLayout;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.GLES20Canvas;
import com.android.photos.views.TiledImageRenderer;
import com.mediatek.gallerybasic.gl.MBasicTexture;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TiledImageView extends FrameLayout {
    private static final boolean IS_SUPPORTED;
    private static final boolean USE_CHOREOGRAPHER;
    private Choreographer.FrameCallback mFrameCallback;
    private Runnable mFreeTextures;
    private GLSurfaceView mGLSurfaceView;
    private boolean mInvalPending;
    private Object mLock;
    private ImageRendererWrapper mRenderer;
    private RectF mTempRectF;
    private float[] mValues;

    static {
        IS_SUPPORTED = Build.VERSION.SDK_INT >= 16;
        USE_CHOREOGRAPHER = Build.VERSION.SDK_INT >= 16;
    }

    private static class ImageRendererWrapper {
        int centerX;
        int centerY;
        TiledImageRenderer image;
        Runnable isReadyCallback;
        int rotation;
        float scale;
        TiledImageRenderer.TileSource source;

        private ImageRendererWrapper() {
        }
    }

    public TiledImageView(Context context) {
        this(context, null);
    }

    public TiledImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mInvalPending = false;
        this.mValues = new float[9];
        this.mLock = new Object();
        this.mFreeTextures = new Runnable() {
            @Override
            public void run() {
                TiledImageView.this.mRenderer.image.freeTextures();
            }
        };
        this.mTempRectF = new RectF();
        if (!IS_SUPPORTED) {
            return;
        }
        this.mRenderer = new ImageRendererWrapper();
        this.mRenderer.image = new TiledImageRenderer(this);
        this.mGLSurfaceView = new GLSurfaceView(context);
        this.mGLSurfaceView.setEGLContextClientVersion(2);
        this.mGLSurfaceView.setRenderer(new TileRenderer());
        this.mGLSurfaceView.setRenderMode(0);
        addView(this.mGLSurfaceView, new FrameLayout.LayoutParams(-1, -1));
    }

    public void destroy() {
        if (!IS_SUPPORTED) {
            return;
        }
        this.mGLSurfaceView.queueEvent(this.mFreeTextures);
    }

    public void setTileSource(TiledImageRenderer.TileSource tileSource, Runnable runnable) {
        if (!IS_SUPPORTED) {
            return;
        }
        synchronized (this.mLock) {
            this.mRenderer.source = tileSource;
            this.mRenderer.isReadyCallback = runnable;
            this.mRenderer.centerX = tileSource != null ? tileSource.getImageWidth() / 2 : 0;
            this.mRenderer.centerY = tileSource != null ? tileSource.getImageHeight() / 2 : 0;
            this.mRenderer.rotation = tileSource != null ? tileSource.getRotation() : 0;
            this.mRenderer.scale = 0.0f;
            updateScaleIfNecessaryLocked(this.mRenderer);
        }
        invalidate();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (!IS_SUPPORTED) {
            return;
        }
        synchronized (this.mLock) {
            updateScaleIfNecessaryLocked(this.mRenderer);
        }
    }

    private void updateScaleIfNecessaryLocked(ImageRendererWrapper imageRendererWrapper) {
        if (imageRendererWrapper == null || imageRendererWrapper.source == null || imageRendererWrapper.scale > 0.0f || getWidth() == 0) {
            return;
        }
        imageRendererWrapper.scale = Math.min(getWidth() / imageRendererWrapper.source.getImageWidth(), getHeight() / imageRendererWrapper.source.getImageHeight());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!IS_SUPPORTED) {
            return;
        }
        super.dispatchDraw(canvas);
    }

    @Override
    @SuppressLint({"NewApi"})
    public void setTranslationX(float f) {
        if (!IS_SUPPORTED) {
            return;
        }
        super.setTranslationX(f);
    }

    @Override
    public void invalidate() {
        if (!IS_SUPPORTED) {
            return;
        }
        if (USE_CHOREOGRAPHER) {
            invalOnVsync();
        } else {
            this.mGLSurfaceView.requestRender();
        }
    }

    @TargetApi(16)
    private void invalOnVsync() {
        if (!this.mInvalPending) {
            this.mInvalPending = true;
            if (this.mFrameCallback == null) {
                this.mFrameCallback = new Choreographer.FrameCallback() {
                    @Override
                    public void doFrame(long j) {
                        TiledImageView.this.mInvalPending = false;
                        TiledImageView.this.mGLSurfaceView.requestRender();
                    }
                };
            }
            Choreographer.getInstance().postFrameCallback(this.mFrameCallback);
        }
    }

    private class TileRenderer implements GLSurfaceView.Renderer {
        private GLES20Canvas mCanvas;

        private TileRenderer() {
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
            this.mCanvas = new GLES20Canvas();
            BasicTexture.invalidateAllTextures();
            MBasicTexture.invalidateAllTextures();
            TiledImageView.this.mRenderer.image.setModel(TiledImageView.this.mRenderer.source, TiledImageView.this.mRenderer.rotation);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i2) {
            this.mCanvas.setSize(i, i2);
            TiledImageView.this.mRenderer.image.setViewSize(i, i2);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            Runnable runnable;
            this.mCanvas.clearBuffer();
            synchronized (TiledImageView.this.mLock) {
                runnable = TiledImageView.this.mRenderer.isReadyCallback;
                TiledImageView.this.mRenderer.image.setModel(TiledImageView.this.mRenderer.source, TiledImageView.this.mRenderer.rotation);
                TiledImageView.this.mRenderer.image.setPosition(TiledImageView.this.mRenderer.centerX, TiledImageView.this.mRenderer.centerY, TiledImageView.this.mRenderer.scale);
            }
            if (TiledImageView.this.mRenderer.image.draw(this.mCanvas) && runnable != null) {
                synchronized (TiledImageView.this.mLock) {
                    if (TiledImageView.this.mRenderer.isReadyCallback == runnable) {
                        TiledImageView.this.mRenderer.isReadyCallback = null;
                    }
                }
                if (runnable != null) {
                    TiledImageView.this.post(runnable);
                }
            }
        }
    }
}
