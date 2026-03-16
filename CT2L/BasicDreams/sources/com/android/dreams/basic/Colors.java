package com.android.dreams.basic;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.service.dreams.DreamService;
import android.view.TextureView;

public class Colors extends DreamService implements TextureView.SurfaceTextureListener {
    static final String TAG = Colors.class.getSimpleName();
    private ColorsGLRenderer mRenderer;
    private Handler mRendererHandler;
    private HandlerThread mRendererHandlerThread;
    private TextureView mTextureView;

    public static void LOG(String fmt, Object... args) {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setInteractive(false);
        this.mTextureView = new TextureView(this);
        this.mTextureView.setSurfaceTextureListener(this);
        if (this.mRendererHandlerThread == null) {
            this.mRendererHandlerThread = new HandlerThread(TAG);
            this.mRendererHandlerThread.start();
            this.mRendererHandler = new Handler(this.mRendererHandlerThread.getLooper());
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setLowProfile(true);
        setFullscreen(true);
        setContentView(this.mTextureView);
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
        LOG("onSurfaceTextureAvailable(%s, %d, %d)", surface, Integer.valueOf(width), Integer.valueOf(height));
        this.mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Colors.this.mRenderer != null) {
                    Colors.this.mRenderer.stop();
                }
                Colors.this.mRenderer = new ColorsGLRenderer(surface, width, height);
                Colors.this.mRenderer.start();
            }
        });
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, final int width, final int height) {
        LOG("onSurfaceTextureSizeChanged(%s, %d, %d)", surface, Integer.valueOf(width), Integer.valueOf(height));
        this.mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Colors.this.mRenderer != null) {
                    Colors.this.mRenderer.setSize(width, height);
                }
            }
        });
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        LOG("onSurfaceTextureDestroyed(%s)", surface);
        this.mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Colors.this.mRenderer != null) {
                    Colors.this.mRenderer.stop();
                    Colors.this.mRenderer = null;
                }
                Colors.this.mRendererHandlerThread.quit();
            }
        });
        try {
            this.mRendererHandlerThread.join();
        } catch (InterruptedException e) {
            LOG("Error while waiting for renderer", e);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        LOG("onSurfaceTextureUpdated(%s)", surface);
    }
}
