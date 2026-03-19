package com.android.server.wm;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;

class CircularDisplayMask {
    private static final String TAG = "WindowManager";
    private boolean mDimensionsUnequal;
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private int mMaskThickness;
    private Paint mPaint;
    private int mRotation;
    private int mScreenOffset;
    private Point mScreenSize;
    private final Surface mSurface = new Surface();
    private final SurfaceControl mSurfaceControl;
    private boolean mVisible;

    public CircularDisplayMask(DisplayContent displayContent, int i, int i2, int i3) {
        SurfaceControl surfaceControlBuild;
        this.mScreenOffset = 0;
        this.mDimensionsUnequal = false;
        Display display = displayContent.getDisplay();
        this.mScreenSize = new Point();
        display.getSize(this.mScreenSize);
        if (this.mScreenSize.x != this.mScreenSize.y + i2) {
            Slog.w("WindowManager", "Screen dimensions of displayId = " + display.getDisplayId() + "are not equal, circularMask will not be drawn.");
            this.mDimensionsUnequal = true;
        }
        try {
            surfaceControlBuild = displayContent.makeOverlay().setName("CircularDisplayMask").setSize(this.mScreenSize.x, this.mScreenSize.y).setFormat(-3).build();
            try {
                surfaceControlBuild.setLayerStack(display.getLayerStack());
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
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.mScreenOffset = i2;
        this.mMaskThickness = i3;
    }

    private void drawIfNeeded() {
        if (!this.mDrawNeeded || !this.mVisible || this.mDimensionsUnequal) {
            return;
        }
        this.mDrawNeeded = false;
        Canvas canvasLockCanvas = null;
        try {
            canvasLockCanvas = this.mSurface.lockCanvas(new Rect(0, 0, this.mScreenSize.x, this.mScreenSize.y));
        } catch (Surface.OutOfResourcesException e) {
        } catch (IllegalArgumentException e2) {
        }
        if (canvasLockCanvas == null) {
            return;
        }
        switch (this.mRotation) {
            case 0:
            case 1:
                this.mSurfaceControl.setPosition(0.0f, 0.0f);
                break;
            case 2:
                this.mSurfaceControl.setPosition(0.0f, -this.mScreenOffset);
                break;
            case 3:
                this.mSurfaceControl.setPosition(-this.mScreenOffset, 0.0f);
                break;
        }
        int i = this.mScreenSize.x / 2;
        canvasLockCanvas.drawColor(-16777216);
        float f = i;
        canvasLockCanvas.drawCircle(f, f, i - this.mMaskThickness, this.mPaint);
        this.mSurface.unlockCanvasAndPost(canvasLockCanvas);
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
