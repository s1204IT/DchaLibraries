package com.android.wallpaper;

import android.os.Bundle;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import com.android.wallpaper.RenderScriptScene;

public abstract class RenderScriptWallpaper<T extends RenderScriptScene> extends WallpaperService {
    protected abstract T createScene(int i, int i2);

    private class RenderScriptEngine extends WallpaperService.Engine {
        private T mRenderer;
        private RenderScriptGL mRs;
        final RenderScriptWallpaper this$0;

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
                this.mRs.setSurface((SurfaceHolder) null, 0, 0);
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
                this.mRenderer = (T) this.this$0.createScene(i2, i3);
                this.mRenderer.init(this.mRs, this.this$0.getResources(), isPreview());
                this.mRenderer.start();
                return;
            }
            this.mRenderer.resize(i2, i3);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            if (this.mRenderer != null) {
                this.mRenderer.setOffset(xOffset, yOffset, xPixels, yPixels);
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
            this.mRs = new RenderScriptGL(this.this$0, sc);
            this.mRs.setPriority(RenderScript.Priority.LOW);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            destroyRenderer();
        }

        @Override
        public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
            if (this.mRenderer != null) {
                return this.mRenderer.onCommand(action, x, y, z, extras, resultRequested);
            }
            return null;
        }
    }
}
