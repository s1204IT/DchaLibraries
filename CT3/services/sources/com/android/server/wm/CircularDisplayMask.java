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
import android.view.SurfaceSession;
import com.android.server.usb.UsbAudioDevice;
import com.android.server.wm.WindowSurfaceController;

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
    private final SurfaceControl mSurfaceControl;
    private boolean mVisible;
    private final Surface mSurface = new Surface();
    private Point mScreenSize = new Point();

    public CircularDisplayMask(Display display, SurfaceSession session, int zOrder, int screenOffset, int maskThickness) {
        SurfaceControl ctrl;
        this.mScreenOffset = 0;
        this.mDimensionsUnequal = false;
        display.getSize(this.mScreenSize);
        if (this.mScreenSize.x != this.mScreenSize.y + screenOffset) {
            Slog.w(TAG, "Screen dimensions of displayId = " + display.getDisplayId() + "are not equal, circularMask will not be drawn.");
            this.mDimensionsUnequal = true;
        }
        try {
            if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
                ctrl = new WindowSurfaceController.SurfaceTrace(session, "CircularDisplayMask", this.mScreenSize.x, this.mScreenSize.y, -3, 4);
            } else {
                ctrl = new SurfaceControl(session, "CircularDisplayMask", this.mScreenSize.x, this.mScreenSize.y, -3, 4);
            }
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
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.mScreenOffset = screenOffset;
        this.mMaskThickness = maskThickness;
    }

    private void drawIfNeeded() {
        if (!this.mDrawNeeded || !this.mVisible || this.mDimensionsUnequal) {
            return;
        }
        this.mDrawNeeded = false;
        Rect dirty = new Rect(0, 0, this.mScreenSize.x, this.mScreenSize.y);
        Canvas c = null;
        try {
            c = this.mSurface.lockCanvas(dirty);
        } catch (Surface.OutOfResourcesException e) {
        } catch (IllegalArgumentException e2) {
        }
        if (c == null) {
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
        int circleRadius = this.mScreenSize.x / 2;
        c.drawColor(UsbAudioDevice.kAudioDeviceMetaMask);
        c.drawCircle(circleRadius, circleRadius, circleRadius - this.mMaskThickness, this.mPaint);
        this.mSurface.unlockCanvasAndPost(c);
    }

    public void setVisibility(boolean on) {
        if (this.mSurfaceControl == null) {
            return;
        }
        this.mVisible = on;
        drawIfNeeded();
        if (on) {
            this.mSurfaceControl.show();
        } else {
            this.mSurfaceControl.hide();
        }
    }

    void positionSurface(int dw, int dh, int rotation) {
        if (this.mLastDW == dw && this.mLastDH == dh && this.mRotation == rotation) {
            return;
        }
        this.mLastDW = dw;
        this.mLastDH = dh;
        this.mDrawNeeded = true;
        this.mRotation = rotation;
        drawIfNeeded();
    }
}
