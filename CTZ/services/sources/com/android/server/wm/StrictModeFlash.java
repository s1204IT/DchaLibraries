package com.android.server.wm;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceControl;

class StrictModeFlash {
    private static final String TAG = "WindowManager";
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private final SurfaceControl mSurfaceControl;
    private final Surface mSurface = new Surface();
    private final int mThickness = 20;

    public StrictModeFlash(DisplayContent displayContent) {
        SurfaceControl surfaceControlBuild;
        try {
            surfaceControlBuild = displayContent.makeOverlay().setName("StrictModeFlash").setSize(1, 1).setFormat(-3).build();
            try {
                surfaceControlBuild.setLayer(1010000);
                surfaceControlBuild.setPosition(0.0f, 0.0f);
                surfaceControlBuild.show();
                this.mSurface.copyFrom(surfaceControlBuild);
            } catch (Surface.OutOfResourcesException e) {
            }
        } catch (Surface.OutOfResourcesException e2) {
            surfaceControlBuild = null;
        }
        this.mSurfaceControl = surfaceControlBuild;
        this.mDrawNeeded = true;
    }

    private void drawIfNeeded() {
        Canvas canvasLockCanvas;
        if (!this.mDrawNeeded) {
            return;
        }
        this.mDrawNeeded = false;
        int i = this.mLastDW;
        int i2 = this.mLastDH;
        try {
            canvasLockCanvas = this.mSurface.lockCanvas(new Rect(0, 0, i, i2));
        } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
            canvasLockCanvas = null;
        }
        if (canvasLockCanvas == null) {
            return;
        }
        canvasLockCanvas.save();
        canvasLockCanvas.clipRect(new Rect(0, 0, i, 20));
        canvasLockCanvas.drawColor(-65536);
        canvasLockCanvas.restore();
        canvasLockCanvas.save();
        canvasLockCanvas.clipRect(new Rect(0, 0, 20, i2));
        canvasLockCanvas.drawColor(-65536);
        canvasLockCanvas.restore();
        canvasLockCanvas.save();
        canvasLockCanvas.clipRect(new Rect(i - 20, 0, i, i2));
        canvasLockCanvas.drawColor(-65536);
        canvasLockCanvas.restore();
        canvasLockCanvas.save();
        canvasLockCanvas.clipRect(new Rect(0, i2 - 20, i, i2));
        canvasLockCanvas.drawColor(-65536);
        canvasLockCanvas.restore();
        this.mSurface.unlockCanvasAndPost(canvasLockCanvas);
    }

    public void setVisibility(boolean z) {
        if (this.mSurfaceControl == null) {
            return;
        }
        drawIfNeeded();
        if (z) {
            this.mSurfaceControl.show();
        } else {
            this.mSurfaceControl.hide();
        }
    }

    void positionSurface(int i, int i2) {
        if (this.mLastDW == i && this.mLastDH == i2) {
            return;
        }
        this.mLastDW = i;
        this.mLastDH = i2;
        this.mSurfaceControl.setSize(i, i2);
        this.mDrawNeeded = true;
    }
}
