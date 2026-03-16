package com.android.musicvis;

import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import com.android.musicvis.RenderScriptScene;

public abstract class RenderScriptWallpaper<T extends RenderScriptScene> extends WallpaperService {
    protected abstract T createScene(int i, int i2);

    @Override
    public WallpaperService.Engine onCreateEngine() {
        return new RenderScriptEngine();
    }

    private class RenderScriptEngine extends WallpaperService.Engine {
        private T mRenderer;
        private RenderScriptGL mRs;

        private RenderScriptEngine() {
            super(RenderScriptWallpaper.this);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(false);
            surfaceHolder.setSizeFromLayout();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            destroyRenderer();
        }

        private void destroyRenderer() {
            if (this.mRenderer != null) {
                this.mRenderer.stop();
                this.mRenderer = null;
            }
            if (this.mRs != null) {
                this.mRs.destroy();
                this.mRs = null;
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (this.mRenderer != null) {
                if (visible) {
                    this.mRenderer.start();
                } else {
                    this.mRenderer.stop();
                }
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            super.onSurfaceChanged(surfaceHolder, i, i2, i3);
            if (this.mRs != null) {
                this.mRs.setSurface(surfaceHolder, i2, i3);
            }
            if (this.mRenderer == null) {
                this.mRenderer = (T) RenderScriptWallpaper.this.createScene(i2, i3);
                this.mRenderer.init(this.mRs, RenderScriptWallpaper.this.getResources(), isPreview());
                this.mRenderer.start();
                return;
            }
            this.mRenderer.resize(i2, i3);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            this.mRenderer.setOffset(xOffset, yOffset, xPixels, yPixels);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
            this.mRs = new RenderScriptGL(RenderScriptWallpaper.this, sc);
            this.mRs.setPriority(RenderScript.Priority.LOW);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            destroyRenderer();
        }
    }
}
