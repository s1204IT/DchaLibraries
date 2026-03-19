package com.android.server.wm;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.Debug;
import android.os.IBinder;
import android.os.Trace;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowContentFrameStats;
import java.io.PrintWriter;

class WindowSurfaceController {
    static final String TAG = "WindowManager";
    final WindowStateAnimator mAnimator;
    private final WindowManagerService mService;
    SurfaceControl mSurfaceControl;
    private int mSurfaceH;
    private int mSurfaceW;
    private final Session mWindowSession;
    private final int mWindowType;
    private final String title;
    private boolean mSurfaceShown = false;
    private float mSurfaceX = 0.0f;
    private float mSurfaceY = 0.0f;
    private float mLastDsdx = 1.0f;
    private float mLastDtdx = 0.0f;
    private float mLastDsdy = 0.0f;
    private float mLastDtdy = 1.0f;
    private float mSurfaceAlpha = 0.0f;
    private int mSurfaceLayer = 0;
    private boolean mHiddenForCrop = false;
    private boolean mHiddenForOtherReasons = true;
    private final SurfaceControl.Transaction mTmpTransaction = new SurfaceControl.Transaction();

    public WindowSurfaceController(SurfaceSession surfaceSession, String str, int i, int i2, int i3, int i4, WindowStateAnimator windowStateAnimator, int i5, int i6) {
        this.mSurfaceW = 0;
        this.mSurfaceH = 0;
        this.mAnimator = windowStateAnimator;
        this.mSurfaceW = i;
        this.mSurfaceH = i2;
        this.title = str;
        this.mService = windowStateAnimator.mService;
        WindowState windowState = windowStateAnimator.mWin;
        this.mWindowType = i5;
        this.mWindowSession = windowState.mSession;
        Trace.traceBegin(32L, "new SurfaceControl");
        this.mSurfaceControl = windowState.makeSurface().setBufferLayer().setParent(windowState.getSurfaceControl()).setName(str).setSize(i, i2).setFormat(i3).setFlags(i4).setMetadata(i5, i6).build();
        Trace.traceEnd(32L);
    }

    private void logSurface(String str, RuntimeException runtimeException) {
        String str2 = "  SURFACE " + str + ": " + this.title;
        if (runtimeException != null) {
            Slog.i("WindowManager", str2, runtimeException);
        } else {
            Slog.i("WindowManager", str2);
        }
    }

    void reparentChildrenInTransaction(WindowSurfaceController windowSurfaceController) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            Slog.i("WindowManager", "REPARENT from: " + this + " to: " + windowSurfaceController);
        }
        if (this.mSurfaceControl != null && windowSurfaceController.mSurfaceControl != null) {
            this.mSurfaceControl.reparentChildren(windowSurfaceController.getHandle());
        }
    }

    void detachChildren() {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            Slog.i("WindowManager", "SEVER CHILDREN");
        }
        if (this.mSurfaceControl != null) {
            this.mSurfaceControl.detachChildren();
        }
    }

    void hide(SurfaceControl.Transaction transaction, String str) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("HIDE ( " + str + " )", null);
        }
        this.mHiddenForOtherReasons = true;
        this.mAnimator.destroyPreservedSurfaceLocked();
        if (this.mSurfaceShown) {
            hideSurface(transaction);
        }
    }

    private void hideSurface(SurfaceControl.Transaction transaction) {
        if (this.mSurfaceControl == null) {
            return;
        }
        setShown(false);
        try {
            transaction.hide(this.mSurfaceControl);
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Exception hiding surface in " + this);
        }
    }

    void destroyNotInTransaction() {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
            Slog.i("WindowManager", "Destroying surface " + this + " called by " + Debug.getCallers(8));
        }
        try {
            try {
                if (this.mSurfaceControl != null) {
                    this.mSurfaceControl.destroy();
                }
            } catch (RuntimeException e) {
                Slog.w("WindowManager", "Error destroying surface in: " + this, e);
            }
        } finally {
            setShown(false);
            this.mSurfaceControl = null;
        }
    }

    void disconnectInTransaction() {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
            Slog.i("WindowManager", "Disconnecting client: " + this);
        }
        try {
            if (this.mSurfaceControl != null) {
                this.mSurfaceControl.disconnect();
            }
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Error disconnecting surface in: " + this, e);
        }
    }

    void setCropInTransaction(Rect rect, boolean z) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("CROP " + rect.toShortString(), null);
        }
        try {
            if (rect.width() > 0 && rect.height() > 0) {
                this.mSurfaceControl.setWindowCrop(rect);
                this.mHiddenForCrop = false;
                updateVisibility();
            } else {
                this.mHiddenForCrop = true;
                this.mAnimator.destroyPreservedSurfaceLocked();
                updateVisibility();
            }
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Error setting crop surface of " + this + " crop=" + rect.toShortString(), e);
            if (!z) {
                this.mAnimator.reclaimSomeSurfaceMemory("crop", true);
            }
        }
    }

    void clearCropInTransaction(boolean z) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("CLEAR CROP", null);
        }
        try {
            this.mSurfaceControl.setWindowCrop(new Rect(0, 0, -1, -1));
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Error setting clearing crop of " + this, e);
            if (!z) {
                this.mAnimator.reclaimSomeSurfaceMemory("crop", true);
            }
        }
    }

    void setFinalCropInTransaction(Rect rect) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("FINAL CROP " + rect.toShortString(), null);
        }
        try {
            this.mSurfaceControl.setFinalCrop(rect);
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Error disconnecting surface in: " + this, e);
        }
    }

    void setLayerStackInTransaction(int i) {
        if (this.mSurfaceControl != null) {
            this.mSurfaceControl.setLayerStack(i);
        }
    }

    void setPositionInTransaction(float f, float f2, boolean z) {
        setPosition(null, f, f2, z);
    }

    void setPosition(SurfaceControl.Transaction transaction, float f, float f2, boolean z) {
        if ((this.mSurfaceX == f && this.mSurfaceY == f2) ? false : true) {
            this.mSurfaceX = f;
            this.mSurfaceY = f2;
            try {
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                    logSurface("POS (setPositionInTransaction) @ (" + f + "," + f2 + ")", null);
                }
                if (transaction == null) {
                    this.mSurfaceControl.setPosition(f, f2);
                } else {
                    transaction.setPosition(this.mSurfaceControl, f, f2);
                }
            } catch (RuntimeException e) {
                Slog.w("WindowManager", "Error positioning surface of " + this + " pos=(" + f + "," + f2 + ")", e);
                if (!z) {
                    this.mAnimator.reclaimSomeSurfaceMemory("position", true);
                }
            }
        }
    }

    void setGeometryAppliesWithResizeInTransaction(boolean z) {
        this.mSurfaceControl.setGeometryAppliesWithResize();
    }

    void setMatrixInTransaction(float f, float f2, float f3, float f4, boolean z) {
        setMatrix(null, f, f2, f3, f4, false);
    }

    void setMatrix(SurfaceControl.Transaction transaction, float f, float f2, float f3, float f4, boolean z) {
        if (!((this.mLastDsdx == f && this.mLastDtdx == f2 && this.mLastDtdy == f3 && this.mLastDsdy == f4) ? false : true)) {
            return;
        }
        this.mLastDsdx = f;
        this.mLastDtdx = f2;
        this.mLastDtdy = f3;
        this.mLastDsdy = f4;
        try {
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                logSurface("MATRIX [" + f + "," + f2 + "," + f3 + "," + f4 + "]", null);
            }
            if (transaction == null) {
                this.mSurfaceControl.setMatrix(f, f2, f3, f4);
            } else {
                transaction.setMatrix(this.mSurfaceControl, f, f2, f3, f4);
            }
        } catch (RuntimeException e) {
            Slog.e("WindowManager", "Error setting matrix on surface surface" + this.title + " MATRIX [" + f + "," + f2 + "," + f3 + "," + f4 + "]", (Throwable) null);
            if (!z) {
                this.mAnimator.reclaimSomeSurfaceMemory("matrix", true);
            }
        }
    }

    boolean setSizeInTransaction(int i, int i2, boolean z) {
        if (!((this.mSurfaceW == i && this.mSurfaceH == i2) ? false : true)) {
            return false;
        }
        this.mSurfaceW = i;
        this.mSurfaceH = i2;
        try {
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                logSurface("SIZE " + i + "x" + i2, null);
            }
            this.mSurfaceControl.setSize(i, i2);
            return true;
        } catch (RuntimeException e) {
            Slog.e("WindowManager", "Error resizing surface of " + this.title + " size=(" + i + "x" + i2 + ")", e);
            if (!z) {
                this.mAnimator.reclaimSomeSurfaceMemory("size", true);
            }
            return false;
        }
    }

    boolean prepareToShowInTransaction(float f, float f2, float f3, float f4, float f5, boolean z) {
        if (this.mSurfaceControl != null) {
            try {
                this.mSurfaceAlpha = f;
                this.mSurfaceControl.setAlpha(f);
                this.mLastDsdx = f2;
                this.mLastDtdx = f3;
                this.mLastDsdy = f4;
                this.mLastDtdy = f5;
                this.mSurfaceControl.setMatrix(f2, f3, f4, f5);
            } catch (RuntimeException e) {
                Slog.w("WindowManager", "Error updating surface in " + this.title, e);
                if (!z) {
                    this.mAnimator.reclaimSomeSurfaceMemory("update", true);
                    return false;
                }
                return false;
            }
        }
        return true;
    }

    void setTransparentRegionHint(Region region) {
        if (this.mSurfaceControl == null) {
            Slog.w("WindowManager", "setTransparentRegionHint: null mSurface after mHasSurface true");
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION setTransparentRegion");
        }
        this.mService.openSurfaceTransaction();
        try {
            this.mSurfaceControl.setTransparentRegionHint(region);
        } finally {
            this.mService.closeSurfaceTransaction("setTransparentRegion");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION setTransparentRegion");
            }
        }
    }

    void setOpaque(boolean z) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("isOpaque=" + z, null);
        }
        if (this.mSurfaceControl == null) {
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION setOpaqueLocked");
        }
        this.mService.openSurfaceTransaction();
        try {
            this.mSurfaceControl.setOpaque(z);
        } finally {
            this.mService.closeSurfaceTransaction("setOpaqueLocked");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION setOpaqueLocked");
            }
        }
    }

    void setSecure(boolean z) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("isSecure=" + z, null);
        }
        if (this.mSurfaceControl == null) {
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION setSecureLocked");
        }
        this.mService.openSurfaceTransaction();
        try {
            this.mSurfaceControl.setSecure(z);
        } finally {
            this.mService.closeSurfaceTransaction("setSecure");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION setSecureLocked");
            }
        }
    }

    void getContainerRect(Rect rect) {
        this.mAnimator.getContainerRect(rect);
    }

    boolean showRobustlyInTransaction() {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("SHOW (performLayout)", null);
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Showing " + this + " during relayout");
        }
        this.mHiddenForOtherReasons = false;
        return updateVisibility();
    }

    private boolean updateVisibility() {
        if (this.mHiddenForCrop || this.mHiddenForOtherReasons) {
            if (this.mSurfaceShown) {
                hideSurface(this.mTmpTransaction);
                SurfaceControl.mergeToGlobalTransaction(this.mTmpTransaction);
                return false;
            }
            return false;
        }
        if (!this.mSurfaceShown) {
            return showSurface();
        }
        return true;
    }

    private boolean showSurface() {
        try {
            setShown(true);
            this.mSurfaceControl.show();
            return true;
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Failure showing surface " + this.mSurfaceControl + " in " + this, e);
            this.mAnimator.reclaimSomeSurfaceMemory("show", true);
            return false;
        }
    }

    void deferTransactionUntil(IBinder iBinder, long j) {
        this.mSurfaceControl.deferTransactionUntil(iBinder, j);
    }

    void forceScaleableInTransaction(boolean z) {
        this.mSurfaceControl.setOverrideScalingMode(z ? 1 : -1);
    }

    boolean clearWindowContentFrameStats() {
        if (this.mSurfaceControl == null) {
            return false;
        }
        return this.mSurfaceControl.clearContentFrameStats();
    }

    boolean getWindowContentFrameStats(WindowContentFrameStats windowContentFrameStats) {
        if (this.mSurfaceControl == null) {
            return false;
        }
        return this.mSurfaceControl.getContentFrameStats(windowContentFrameStats);
    }

    boolean hasSurface() {
        return this.mSurfaceControl != null;
    }

    IBinder getHandle() {
        if (this.mSurfaceControl == null) {
            return null;
        }
        return this.mSurfaceControl.getHandle();
    }

    void getSurface(Surface surface) {
        surface.copyFrom(this.mSurfaceControl);
    }

    int getLayer() {
        return this.mSurfaceLayer;
    }

    boolean getShown() {
        return this.mSurfaceShown;
    }

    void setShown(boolean z) {
        this.mSurfaceShown = z;
        this.mService.updateNonSystemOverlayWindowsVisibilityIfNeeded(this.mAnimator.mWin, z);
        if (this.mWindowSession != null) {
            this.mWindowSession.onWindowSurfaceVisibilityChanged(this, this.mSurfaceShown, this.mWindowType);
        }
    }

    float getX() {
        return this.mSurfaceX;
    }

    float getY() {
        return this.mSurfaceY;
    }

    int getWidth() {
        return this.mSurfaceW;
    }

    int getHeight() {
        return this.mSurfaceH;
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1133871366145L, this.mSurfaceShown);
        protoOutputStream.write(1120986464258L, this.mSurfaceLayer);
        protoOutputStream.end(jStart);
    }

    public void dump(PrintWriter printWriter, String str, boolean z) {
        if (z) {
            printWriter.print(str);
            printWriter.print("mSurface=");
            printWriter.println(this.mSurfaceControl);
        }
        printWriter.print(str);
        printWriter.print("Surface: shown=");
        printWriter.print(this.mSurfaceShown);
        printWriter.print(" layer=");
        printWriter.print(this.mSurfaceLayer);
        printWriter.print(" alpha=");
        printWriter.print(this.mSurfaceAlpha);
        printWriter.print(" rect=(");
        printWriter.print(this.mSurfaceX);
        printWriter.print(",");
        printWriter.print(this.mSurfaceY);
        printWriter.print(") ");
        printWriter.print(this.mSurfaceW);
        printWriter.print(" x ");
        printWriter.print(this.mSurfaceH);
        printWriter.print(" transform=(");
        printWriter.print(this.mLastDsdx);
        printWriter.print(", ");
        printWriter.print(this.mLastDtdx);
        printWriter.print(", ");
        printWriter.print(this.mLastDsdy);
        printWriter.print(", ");
        printWriter.print(this.mLastDtdy);
        printWriter.println(")");
    }

    public String toString() {
        return this.mSurfaceControl.toString();
    }
}
