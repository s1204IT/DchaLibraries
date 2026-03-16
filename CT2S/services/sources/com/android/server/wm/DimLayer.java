package com.android.server.wm;

import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import java.io.PrintWriter;

public class DimLayer {
    private static final boolean DEBUG = false;
    private static final String TAG = "DimLayer";
    SurfaceControl mDimSurface;
    final DisplayContent mDisplayContent;
    long mDuration;
    final TaskStack mStack;
    long mStartTime;
    float mAlpha = 0.0f;
    int mLayer = -1;
    Rect mBounds = new Rect();
    Rect mLastBounds = new Rect();
    private boolean mShowing = DEBUG;
    float mStartAlpha = 0.0f;
    float mTargetAlpha = 0.0f;

    DimLayer(WindowManagerService service, TaskStack stack, DisplayContent displayContent) {
        this.mStack = stack;
        this.mDisplayContent = displayContent;
        int displayId = this.mDisplayContent.getDisplayId();
        SurfaceControl.openTransaction();
        try {
            this.mDimSurface = new SurfaceControl(service.mFxSession, TAG, 16, 16, -1, 131076);
            this.mDimSurface.setLayerStack(displayId);
        } catch (Exception e) {
            Slog.e("WindowManager", "Exception creating Dim surface", e);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    boolean isDimming() {
        if (this.mTargetAlpha != 0.0f) {
            return true;
        }
        return DEBUG;
    }

    boolean isAnimating() {
        if (this.mTargetAlpha != this.mAlpha) {
            return true;
        }
        return DEBUG;
    }

    float getTargetAlpha() {
        return this.mTargetAlpha;
    }

    void setLayer(int layer) {
        if (this.mLayer != layer) {
            this.mLayer = layer;
            this.mDimSurface.setLayer(layer);
        }
    }

    int getLayer() {
        return this.mLayer;
    }

    private void setAlpha(float alpha) {
        if (this.mAlpha != alpha) {
            try {
                this.mDimSurface.setAlpha(alpha);
                if (alpha == 0.0f && this.mShowing) {
                    this.mDimSurface.hide();
                    this.mShowing = DEBUG;
                } else if (alpha > 0.0f && !this.mShowing) {
                    this.mDimSurface.show();
                    this.mShowing = true;
                }
            } catch (RuntimeException e) {
                Slog.w(TAG, "Failure setting alpha immediately", e);
            }
            this.mAlpha = alpha;
        }
    }

    void adjustSurface(int layer, boolean inTransaction) {
        int dw;
        int dh;
        float xPos;
        float yPos;
        if (!this.mStack.isFullscreen()) {
            dw = this.mBounds.width();
            dh = this.mBounds.height();
            xPos = this.mBounds.left;
            yPos = this.mBounds.top;
        } else {
            DisplayInfo info = this.mDisplayContent.getDisplayInfo();
            dw = (int) (((double) info.logicalWidth) * 1.5d);
            dh = (int) (((double) info.logicalHeight) * 1.5d);
            xPos = (dw * (-1)) / 6;
            yPos = (dh * (-1)) / 6;
        }
        if (!inTransaction) {
            try {
                try {
                    SurfaceControl.openTransaction();
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Failure setting size or layer", e);
                    if (!inTransaction) {
                        SurfaceControl.closeTransaction();
                    }
                }
            } finally {
                if (!inTransaction) {
                    SurfaceControl.closeTransaction();
                }
            }
        }
        this.mDimSurface.setPosition(xPos, yPos);
        this.mDimSurface.setSize(dw, dh);
        this.mDimSurface.setLayer(layer);
        this.mLastBounds.set(this.mBounds);
        this.mLayer = layer;
    }

    void setBounds(Rect bounds) {
        this.mBounds.set(bounds);
        if (isDimming() && !this.mLastBounds.equals(bounds)) {
            adjustSurface(this.mLayer, DEBUG);
        }
    }

    private boolean durationEndsEarlier(long duration) {
        if (SystemClock.uptimeMillis() + duration < this.mStartTime + this.mDuration) {
            return true;
        }
        return DEBUG;
    }

    void show() {
        if (isAnimating()) {
            show(this.mLayer, this.mTargetAlpha, 0L);
        }
    }

    void show(int layer, float alpha, long duration) {
        if (this.mDimSurface == null) {
            Slog.e(TAG, "show: no Surface");
            this.mAlpha = 0.0f;
            this.mTargetAlpha = 0.0f;
            return;
        }
        if (!this.mLastBounds.equals(this.mBounds) || this.mLayer != layer) {
            adjustSurface(layer, true);
        }
        long curTime = SystemClock.uptimeMillis();
        boolean animating = isAnimating();
        if ((animating && (this.mTargetAlpha != alpha || durationEndsEarlier(duration))) || (!animating && this.mAlpha != alpha)) {
            if (duration <= 0) {
                setAlpha(alpha);
            } else {
                this.mStartAlpha = this.mAlpha;
                this.mStartTime = curTime;
                this.mDuration = duration;
            }
        }
        this.mTargetAlpha = alpha;
    }

    void hide() {
        if (this.mShowing) {
            hide(0L);
        }
    }

    void hide(long duration) {
        if (this.mShowing) {
            if (this.mTargetAlpha != 0.0f || durationEndsEarlier(duration)) {
                show(this.mLayer, 0.0f, duration);
            }
        }
    }

    boolean stepAnimation() {
        if (this.mDimSurface == null) {
            Slog.e(TAG, "stepAnimation: null Surface");
            this.mAlpha = 0.0f;
            this.mTargetAlpha = 0.0f;
            return DEBUG;
        }
        if (isAnimating()) {
            long curTime = SystemClock.uptimeMillis();
            float alphaDelta = this.mTargetAlpha - this.mStartAlpha;
            float alpha = this.mStartAlpha + (((curTime - this.mStartTime) * alphaDelta) / this.mDuration);
            if ((alphaDelta > 0.0f && alpha > this.mTargetAlpha) || (alphaDelta < 0.0f && alpha < this.mTargetAlpha)) {
                alpha = this.mTargetAlpha;
            }
            setAlpha(alpha);
        }
        return isAnimating();
    }

    void destroySurface() {
        if (this.mDimSurface != null) {
            this.mDimSurface.destroy();
            this.mDimSurface = null;
        }
    }

    public void printTo(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mDimSurface=");
        pw.print(this.mDimSurface);
        pw.print(" mLayer=");
        pw.print(this.mLayer);
        pw.print(" mAlpha=");
        pw.println(this.mAlpha);
        pw.print(prefix);
        pw.print("mLastBounds=");
        pw.print(this.mLastBounds.toShortString());
        pw.print(" mBounds=");
        pw.println(this.mBounds.toShortString());
        pw.print(prefix);
        pw.print("Last animation: ");
        pw.print(" mDuration=");
        pw.print(this.mDuration);
        pw.print(" mStartTime=");
        pw.print(this.mStartTime);
        pw.print(" curTime=");
        pw.println(SystemClock.uptimeMillis());
        pw.print(prefix);
        pw.print(" mStartAlpha=");
        pw.print(this.mStartAlpha);
        pw.print(" mTargetAlpha=");
        pw.println(this.mTargetAlpha);
    }
}
