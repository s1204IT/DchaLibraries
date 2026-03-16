package com.android.wallpaper.walkaround;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import java.io.IOException;
import java.util.List;

public class WalkAroundWallpaper extends WallpaperService {
    private Camera mCamera;
    private WalkAroundEngine mOwner;

    @Override
    public WallpaperService.Engine onCreateEngine() {
        WalkAroundEngine walkAroundEngine = new WalkAroundEngine();
        this.mOwner = walkAroundEngine;
        return walkAroundEngine;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCamera();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mCamera != null && this.mCamera.previewEnabled()) {
            boolean portrait = newConfig.orientation == 1;
            Camera.Parameters params = this.mCamera.getParameters();
            params.set("orientation", portrait ? "portrait" : "landscape");
            this.mCamera.setParameters(params);
            if (this.mCamera.previewEnabled()) {
                this.mCamera.stopPreview();
            }
            this.mCamera.startPreview();
        }
    }

    private void startCamera() {
        if (this.mCamera == null) {
            this.mCamera = Camera.open();
            return;
        }
        try {
            this.mCamera.reconnect();
        } catch (IOException e) {
            this.mCamera.release();
            this.mCamera = null;
            Log.e("WalkAround", "Error opening the camera", e);
        }
    }

    private void stopCamera() {
        if (this.mCamera != null) {
            try {
                this.mCamera.stopPreview();
            } catch (Exception e) {
            }
            try {
                this.mCamera.release();
            } catch (Exception e2) {
            }
            this.mCamera = null;
        }
    }

    class WalkAroundEngine extends WallpaperService.Engine {
        private SurfaceHolder mHolder;

        WalkAroundEngine() {
            super(WalkAroundWallpaper.this);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            surfaceHolder.setType(3);
            this.mHolder = surfaceHolder;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (!visible) {
                if (WalkAroundWallpaper.this.mOwner == this) {
                    WalkAroundWallpaper.this.stopCamera();
                    return;
                }
                return;
            }
            try {
                WalkAroundWallpaper.this.startCamera();
                WalkAroundWallpaper.this.mCamera.setPreviewDisplay(this.mHolder);
                startPreview();
            } catch (IOException e) {
                WalkAroundWallpaper.this.mCamera.release();
                WalkAroundWallpaper.this.mCamera = null;
                Log.e("WalkAround", "Error opening the camera", e);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (holder.isCreating()) {
                try {
                    if (WalkAroundWallpaper.this.mCamera.previewEnabled()) {
                        WalkAroundWallpaper.this.mCamera.stopPreview();
                    }
                    WalkAroundWallpaper.this.mCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    WalkAroundWallpaper.this.mCamera.release();
                    WalkAroundWallpaper.this.mCamera = null;
                    Log.e("WalkAround", "Error opening the camera", e);
                }
            }
            if (isVisible()) {
                startPreview();
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            WalkAroundWallpaper.this.startCamera();
        }

        private void startPreview() {
            Resources resources = WalkAroundWallpaper.this.getResources();
            boolean portrait = resources.getConfiguration().orientation == 1;
            Camera.Parameters params = WalkAroundWallpaper.this.mCamera.getParameters();
            DisplayMetrics metrics = resources.getDisplayMetrics();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            boolean found = false;
            for (Camera.Size size : sizes) {
                if ((portrait && size.width == metrics.heightPixels && size.height == metrics.widthPixels) || (!portrait && size.width == metrics.widthPixels && size.height == metrics.heightPixels)) {
                    params.setPreviewSize(size.width, size.height);
                    found = true;
                }
            }
            if (!found) {
                for (Camera.Size size2 : sizes) {
                    if (size2.width >= metrics.widthPixels && size2.height >= metrics.heightPixels) {
                        params.setPreviewSize(size2.width, size2.height);
                        found = true;
                    }
                }
            }
            if (!found) {
                Canvas canvas = null;
                try {
                    canvas = this.mHolder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawColor(0);
                    }
                    Camera.Size size3 = sizes.get(0);
                    params.setPreviewSize(size3.width, size3.height);
                } finally {
                    if (canvas != null) {
                        this.mHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
            params.set("orientation", portrait ? "portrait" : "landscape");
            WalkAroundWallpaper.this.mCamera.setParameters(params);
            WalkAroundWallpaper.this.mCamera.startPreview();
        }
    }
}
