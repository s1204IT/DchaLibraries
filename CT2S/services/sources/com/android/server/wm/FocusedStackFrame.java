package com.android.server.wm;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;

class FocusedStackFrame {
    private static final float ALPHA = 0.3f;
    private static final String TAG = "FocusedStackFrame";
    private static final int THICKNESS = 10;
    private final SurfaceControl mSurfaceControl;
    private final Surface mSurface = new Surface();
    private final Rect mLastBounds = new Rect();
    final Rect mBounds = new Rect();
    private final Rect mTmpDrawRect = new Rect();

    public FocusedStackFrame(Display display, SurfaceSession session) {
        SurfaceControl ctrl;
        try {
            ctrl = new SurfaceControl(session, TAG, 1, 1, -3, 4);
            try {
                ctrl.setLayerStack(display.getLayerStack());
                ctrl.setAlpha(ALPHA);
                this.mSurface.copyFrom(ctrl);
            } catch (Surface.OutOfResourcesException e) {
            }
        } catch (Surface.OutOfResourcesException e2) {
            ctrl = null;
        }
        this.mSurfaceControl = ctrl;
    }

    private void draw(Rect bounds, int color) {
        this.mTmpDrawRect.set(bounds);
        Canvas c = null;
        try {
            c = this.mSurface.lockCanvas(this.mTmpDrawRect);
        } catch (Surface.OutOfResourcesException e) {
        } catch (IllegalArgumentException e2) {
        }
        if (c != null) {
            int w = bounds.width();
            int h = bounds.height();
            this.mTmpDrawRect.set(0, 0, w, 10);
            c.clipRect(this.mTmpDrawRect, Region.Op.REPLACE);
            c.drawColor(color);
            this.mTmpDrawRect.set(0, 10, 10, h - 10);
            c.clipRect(this.mTmpDrawRect, Region.Op.REPLACE);
            c.drawColor(color);
            this.mTmpDrawRect.set(w - 10, 10, w, h - 10);
            c.clipRect(this.mTmpDrawRect, Region.Op.REPLACE);
            c.drawColor(color);
            this.mTmpDrawRect.set(0, h - 10, w, h);
            c.clipRect(this.mTmpDrawRect, Region.Op.REPLACE);
            c.drawColor(color);
            this.mSurface.unlockCanvasAndPost(c);
        }
    }

    private void positionSurface(Rect bounds) {
        this.mSurfaceControl.setSize(bounds.width(), bounds.height());
        this.mSurfaceControl.setPosition(bounds.left, bounds.top);
    }

    public void setVisibility(boolean on) {
        if (this.mSurfaceControl != null) {
            if (on) {
                if (!this.mLastBounds.equals(this.mBounds)) {
                    positionSurface(this.mLastBounds);
                    draw(this.mLastBounds, 0);
                    positionSurface(this.mBounds);
                    draw(this.mBounds, -1);
                    this.mLastBounds.set(this.mBounds);
                }
                this.mSurfaceControl.show();
                return;
            }
            this.mSurfaceControl.hide();
        }
    }

    public void setBounds(TaskStack stack) {
        stack.getBounds(this.mBounds);
    }

    public void setLayer(int layer) {
        this.mSurfaceControl.setLayer(layer);
    }
}
