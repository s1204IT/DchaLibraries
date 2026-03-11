package com.android.photos.views;

import android.content.Context;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.widget.FrameLayout;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.GLES20Canvas;
import com.android.photos.views.TiledImageRenderer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TiledImageView extends FrameLayout {
    private Choreographer.FrameCallback mFrameCallback;
    private Runnable mFreeTextures;
    GLSurfaceView mGLSurfaceView;
    boolean mInvalPending;
    protected Object mLock;
    private boolean mNeedFreeTextures;
    protected ImageRendererWrapper mRenderer;
    private RectF mTempRectF;
    private float[] mValues;

    protected static class ImageRendererWrapper {
        public int centerX;
        public int centerY;
        TiledImageRenderer image;
        Runnable isReadyCallback;
        public int rotation;
        public float scale;
        public TiledImageRenderer.TileSource source;

        protected ImageRendererWrapper() {
        }
    }

    public TiledImageView(Context context) {
        this(context, null);
    }

    public TiledImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInvalPending = false;
        this.mNeedFreeTextures = false;
        this.mValues = new float[9];
        this.mLock = new Object();
        this.mFreeTextures = new Runnable() {
            @Override
            public void run() {
                TiledImageView.this.mRenderer.image.freeTextures();
                TiledImageView.this.mNeedFreeTextures = false;
            }
        };
        this.mTempRectF = new RectF();
        this.mRenderer = new ImageRendererWrapper();
        this.mRenderer.image = new TiledImageRenderer(this);
        this.mGLSurfaceView = new GLSurfaceView(context);
        this.mGLSurfaceView.setEGLContextClientVersion(2);
        this.mGLSurfaceView.setRenderer(new TileRenderer());
        this.mGLSurfaceView.setRenderMode(0);
        addView(this.mGLSurfaceView, new FrameLayout.LayoutParams(-1, -1));
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        this.mGLSurfaceView.setVisibility(visibility);
    }

    public void destroy() {
        this.mNeedFreeTextures = true;
        this.mGLSurfaceView.queueEvent(this.mFreeTextures);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!this.mNeedFreeTextures) {
            return;
        }
        this.mRenderer.image.freeTextures();
        this.mNeedFreeTextures = false;
    }

    public void setTileSource(TiledImageRenderer.TileSource source, Runnable isReadyCallback) {
        synchronized (this.mLock) {
            this.mRenderer.source = source;
            this.mRenderer.isReadyCallback = isReadyCallback;
            this.mRenderer.centerX = source != null ? source.getImageWidth() / 2 : 0;
            this.mRenderer.centerY = source != null ? source.getImageHeight() / 2 : 0;
            this.mRenderer.rotation = source != null ? source.getRotation() : 0;
            this.mRenderer.scale = 0.0f;
            updateScaleIfNecessaryLocked(this.mRenderer);
        }
        invalidate();
    }

    public TiledImageRenderer.TileSource getTileSource() {
        return this.mRenderer.source;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        synchronized (this.mLock) {
            updateScaleIfNecessaryLocked(this.mRenderer);
        }
    }

    private void updateScaleIfNecessaryLocked(ImageRendererWrapper renderer) {
        if (renderer == null || renderer.source == null || renderer.scale > 0.0f || getWidth() == 0) {
            return;
        }
        renderer.scale = Math.min(getWidth() / renderer.source.getImageWidth(), getHeight() / renderer.source.getImageHeight());
    }

    @Override
    public void invalidate() {
        invalOnVsync();
    }

    private void invalOnVsync() {
        if (this.mInvalPending) {
            return;
        }
        this.mInvalPending = true;
        if (this.mFrameCallback == null) {
            this.mFrameCallback = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    TiledImageView.this.mInvalPending = false;
                    TiledImageView.this.mGLSurfaceView.requestRender();
                }
            };
        }
        Choreographer.getInstance().postFrameCallback(this.mFrameCallback);
    }

    class TileRenderer implements GLSurfaceView.Renderer {
        private GLES20Canvas mCanvas;

        TileRenderer() {
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            this.mCanvas = new GLES20Canvas();
            BasicTexture.invalidateAllTextures();
            TiledImageView.this.mRenderer.image.setModel(TiledImageView.this.mRenderer.source, TiledImageView.this.mRenderer.rotation);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            this.mCanvas.setSize(width, height);
            TiledImageView.this.mRenderer.image.setViewSize(width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            Runnable readyCallback;
            this.mCanvas.deleteRecycledResources();
            this.mCanvas.clearBuffer();
            synchronized (TiledImageView.this.mLock) {
                readyCallback = TiledImageView.this.mRenderer.isReadyCallback;
                TiledImageView.this.mRenderer.image.setModel(TiledImageView.this.mRenderer.source, TiledImageView.this.mRenderer.rotation);
                TiledImageView.this.mRenderer.image.setPosition(TiledImageView.this.mRenderer.centerX, TiledImageView.this.mRenderer.centerY, TiledImageView.this.mRenderer.scale);
            }
            boolean complete = TiledImageView.this.mRenderer.image.draw(this.mCanvas);
            if (!complete || readyCallback == null) {
                return;
            }
            synchronized (TiledImageView.this.mLock) {
                if (TiledImageView.this.mRenderer.isReadyCallback == readyCallback) {
                    TiledImageView.this.mRenderer.isReadyCallback = null;
                }
            }
            if (readyCallback == null) {
                return;
            }
            TiledImageView.this.post(readyCallback);
        }
    }
}
