package com.android.dreams.basic;

import android.os.Handler;
import android.os.HandlerThread;
import android.service.dreams.DreamService;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Colors extends DreamService implements SurfaceHolder.Callback {
    static final String TAG = Colors.class.getSimpleName();
    private ColorsGLRenderer mRenderer;
    private Handler mRendererHandler;
    private HandlerThread mRendererHandlerThread;
    private SurfaceView mSurfaceView;

    public static void LOG(String str, Object... objArr) {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setInteractive(false);
        this.mSurfaceView = new SurfaceView(this);
        this.mSurfaceView.getHolder().addCallback(this);
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
        setContentView(this.mSurfaceView);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {
        LOG("surfaceCreated(%s, %d, %d)", surfaceHolder.getSurface(), Integer.valueOf(surfaceHolder.getSurfaceFrame().width()), Integer.valueOf(surfaceHolder.getSurfaceFrame().height()));
        this.mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Colors.this.mRenderer != null) {
                    Colors.this.mRenderer.stop();
                }
                Colors.this.mRenderer = new ColorsGLRenderer(surfaceHolder.getSurface(), surfaceHolder.getSurfaceFrame().width(), surfaceHolder.getSurfaceFrame().height());
                Colors.this.mRenderer.start();
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, final int i2, final int i3) {
        LOG("surfaceChanged(%s, %d,  %d, %d)", surfaceHolder.getSurface(), Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
        this.mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Colors.this.mRenderer != null) {
                    Colors.this.mRenderer.setSize(i2, i3);
                }
            }
        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        LOG("surfaceDestroyed(%s)", surfaceHolder.getSurface());
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
    }
}
