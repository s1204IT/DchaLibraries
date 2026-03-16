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
import android.view.SurfaceSession;

class EmulatorDisplayOverlay {
    private static final String TAG = "EmulatorDisplayOverlay";
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private Drawable mOverlay;
    private int mRotation;
    private final SurfaceControl mSurfaceControl;
    private boolean mVisible;
    private final Surface mSurface = new Surface();
    private Point mScreenSize = new Point();

    public EmulatorDisplayOverlay(Context context, Display display, SurfaceSession session, int zOrder) {
        SurfaceControl ctrl;
        display.getSize(this.mScreenSize);
        try {
            ctrl = new SurfaceControl(session, TAG, this.mScreenSize.x, this.mScreenSize.y, -3, 4);
            try {
                ctrl.setLayerStack(display.getLayerStack());
                ctrl.setLayer(zOrder);
                ctrl.setPosition(0.0f, 0.0f);
                ctrl.show();
                this.mSurface.copyFrom(ctrl);
            } catch (Surface.OutOfResourcesException e) {
            }
        } catch (Surface.OutOfResourcesException e2) {
            ctrl = null;
        }
        this.mSurfaceControl = ctrl;
        this.mDrawNeeded = true;
        this.mOverlay = context.getDrawable(R.drawable.chooser_file_generic);
    }

    private void drawIfNeeded() {
        if (this.mDrawNeeded && this.mVisible) {
            this.mDrawNeeded = false;
            Rect dirty = new Rect(0, 0, this.mScreenSize.x, this.mScreenSize.y);
            Canvas c = null;
            try {
                c = this.mSurface.lockCanvas(dirty);
            } catch (Surface.OutOfResourcesException e) {
            } catch (IllegalArgumentException e2) {
            }
            if (c != null) {
                c.drawColor(0, PorterDuff.Mode.SRC);
                this.mSurfaceControl.setPosition(0.0f, 0.0f);
                this.mOverlay.setBounds(0, 0, this.mScreenSize.x, this.mScreenSize.y);
                this.mOverlay.draw(c);
                this.mSurface.unlockCanvasAndPost(c);
            }
        }
    }

    public void setVisibility(boolean on) {
        if (this.mSurfaceControl != null) {
            this.mVisible = on;
            drawIfNeeded();
            if (on) {
                this.mSurfaceControl.show();
            } else {
                this.mSurfaceControl.hide();
            }
        }
    }

    void positionSurface(int dw, int dh, int rotation) {
        if (this.mLastDW != dw || this.mLastDH != dh || this.mRotation != rotation) {
            this.mLastDW = dw;
            this.mLastDH = dh;
            this.mDrawNeeded = true;
            this.mRotation = rotation;
            drawIfNeeded();
        }
    }
}
