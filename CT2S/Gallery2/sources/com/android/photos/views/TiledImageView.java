package com.android.photos.views;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;
import android.widget.FrameLayout;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.GLES20Canvas;
import com.android.photos.views.TiledImageRenderer;
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

    public TiledImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        if (IS_SUPPORTED) {
            this.mRenderer = new ImageRendererWrapper();
            this.mRenderer.image = new TiledImageRenderer(this);
            this.mGLSurfaceView = new GLSurfaceView(context);
            this.mGLSurfaceView.setEGLContextClientVersion(2);
            this.mGLSurfaceView.setRenderer(new TileRenderer());
            this.mGLSurfaceView.setRenderMode(0);
            View view = this.mGLSurfaceView;
            addView(view, new FrameLayout.LayoutParams(-1, -1));
        }
    }

    public void destroy() {
        if (IS_SUPPORTED) {
            this.mGLSurfaceView.queueEvent(this.mFreeTextures);
        }
    }

    public void setTileSource(TiledImageRenderer.TileSource source, Runnable isReadyCallback) {
        if (IS_SUPPORTED) {
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
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (IS_SUPPORTED) {
            synchronized (this.mLock) {
                updateScaleIfNecessaryLocked(this.mRenderer);
            }
        }
    }

    private void updateScaleIfNecessaryLocked(ImageRendererWrapper renderer) {
        if (renderer != null && renderer.source != null && renderer.scale <= 0.0f && getWidth() != 0) {
            renderer.scale = Math.min(getWidth() / renderer.source.getImageWidth(), getHeight() / renderer.source.getImageHeight());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (IS_SUPPORTED) {
            super.dispatchDraw(canvas);
        }
    }

    @Override
    @SuppressLint({"NewApi"})
    public void setTranslationX(float translationX) {
        if (IS_SUPPORTED) {
            super.setTranslationX(translationX);
        }
    }

    @Override
    public void invalidate() {
        if (IS_SUPPORTED) {
            if (USE_CHOREOGRAPHER) {
                invalOnVsync();
            } else {
                this.mGLSurfaceView.requestRender();
            }
        }
    }

    @TargetApi(NotificationCompat.FLAG_AUTO_CANCEL)
    private void invalOnVsync() {
        if (!this.mInvalPending) {
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
    }

    private class TileRenderer implements GLSurfaceView.Renderer {
        private GLES20Canvas mCanvas;

        private TileRenderer() {
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
            this.mCanvas.clearBuffer();
            synchronized (TiledImageView.this.mLock) {
                readyCallback = TiledImageView.this.mRenderer.isReadyCallback;
                TiledImageView.this.mRenderer.image.setModel(TiledImageView.this.mRenderer.source, TiledImageView.this.mRenderer.rotation);
                TiledImageView.this.mRenderer.image.setPosition(TiledImageView.this.mRenderer.centerX, TiledImageView.this.mRenderer.centerY, TiledImageView.this.mRenderer.scale);
            }
            boolean complete = TiledImageView.this.mRenderer.image.draw(this.mCanvas);
            if (complete && readyCallback != null) {
                synchronized (TiledImageView.this.mLock) {
                    if (TiledImageView.this.mRenderer.isReadyCallback == readyCallback) {
                        TiledImageView.this.mRenderer.isReadyCallback = null;
                    }
                }
                if (readyCallback != null) {
                    TiledImageView.this.post(readyCallback);
                }
            }
        }
    }
}
