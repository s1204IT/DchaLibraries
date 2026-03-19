package com.android.server.wm;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;

class EmulatorDisplayOverlay {
    private static final String TAG = "WindowManager";
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private Drawable mOverlay;
    private int mRotation;
    private Point mScreenSize;
    private final Surface mSurface = new Surface();
    private final SurfaceControl mSurfaceControl;
    private boolean mVisible;

    public EmulatorDisplayOverlay(Context context, DisplayContent displayContent, int i) {
        SurfaceControl surfaceControlBuild;
        Display display = displayContent.getDisplay();
        this.mScreenSize = new Point();
        display.getSize(this.mScreenSize);
        try {
            surfaceControlBuild = displayContent.makeOverlay().setName("EmulatorDisplayOverlay").setSize(this.mScreenSize.x, this.mScreenSize.y).setFormat(-3).build();
            try {
                surfaceControlBuild.setLayer(i);
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
        this.mOverlay = context.getDrawable(R.drawable.btn_zoom_down);
    }

    private void drawIfNeeded() {
        Canvas canvasLockCanvas;
        if (!this.mDrawNeeded || !this.mVisible) {
            return;
        }
        this.mDrawNeeded = false;
        try {
            canvasLockCanvas = this.mSurface.lockCanvas(new Rect(0, 0, this.mScreenSize.x, this.mScreenSize.y));
        } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
            canvasLockCanvas = null;
        }
        if (canvasLockCanvas != null) {
            canvasLockCanvas.drawColor(0, PorterDuff.Mode.SRC);
            this.mSurfaceControl.setPosition(0.0f, 0.0f);
            int iMax = Math.max(this.mScreenSize.x, this.mScreenSize.y);
            this.mOverlay.setBounds(0, 0, iMax, iMax);
            this.mOverlay.draw(canvasLockCanvas);
            this.mSurface.unlockCanvasAndPost(canvasLockCanvas);
        }
    }

    public void setVisibility(boolean z) {
        if (this.mSurfaceControl == null) {
            return;
        }
        this.mVisible = z;
        drawIfNeeded();
        if (z) {
            this.mSurfaceControl.show();
        } else {
            this.mSurfaceControl.hide();
        }
    }

    void positionSurface(int i, int i2, int i3) {
        if (this.mLastDW == i && this.mLastDH == i2 && this.mRotation == i3) {
            return;
        }
        this.mLastDW = i;
        this.mLastDH = i2;
        this.mDrawNeeded = true;
        this.mRotation = i3;
        drawIfNeeded();
    }
}
