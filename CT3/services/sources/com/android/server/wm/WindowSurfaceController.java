package com.android.server.wm;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Debug;
import android.os.IBinder;
import android.util.Slog;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowContentFrameStats;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;

class WindowSurfaceController {
    static final String TAG = "WindowManager";
    final WindowStateAnimator mAnimator;
    SurfaceControl mSurfaceControl;
    float mSurfaceH;
    float mSurfaceW;
    private final String title;
    private boolean mSurfaceShown = false;
    private float mSurfaceX = 0.0f;
    private float mSurfaceY = 0.0f;
    private float mSurfaceAlpha = 0.0f;
    int mSurfaceLayer = 0;
    private boolean mHiddenForCrop = false;
    private boolean mHiddenForOtherReasons = true;

    public WindowSurfaceController(SurfaceSession s, String name, int w, int h, int format, int flags, WindowStateAnimator animator) {
        this.mSurfaceW = 0.0f;
        this.mSurfaceH = 0.0f;
        this.mAnimator = animator;
        this.mSurfaceW = w;
        this.mSurfaceH = h;
        this.title = name;
        if (animator.mWin.isChildWindow() && animator.mWin.mSubLayer < 0 && animator.mWin.mAppToken != null) {
            this.mSurfaceControl = new SurfaceControlWithBackground(s, name, w, h, format, flags, animator.mWin.mAppToken);
        } else if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE) {
            this.mSurfaceControl = new SurfaceTrace(s, name, w, h, format, flags);
        } else {
            this.mSurfaceControl = new SurfaceControl(s, name, w, h, format, flags);
        }
    }

    void logSurface(String msg, RuntimeException where) {
        String str = "  SURFACE " + msg + ": " + this.title;
        if (where != null) {
            Slog.i(TAG, str, where);
        } else {
            Slog.i(TAG, str);
        }
    }

    void hideInTransaction(String reason) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("HIDE ( " + reason + " )", null);
        }
        this.mHiddenForOtherReasons = true;
        this.mAnimator.destroyPreservedSurfaceLocked();
        updateVisibility();
    }

    private void hideSurface() {
        if (this.mSurfaceControl == null) {
            return;
        }
        this.mSurfaceShown = false;
        try {
            this.mSurfaceControl.hide();
        } catch (RuntimeException e) {
            Slog.w(TAG, "Exception hiding surface in " + this);
        }
    }

    void setPositionAndLayer(float left, float top, int layerStack, int layer) {
        SurfaceControl.openTransaction();
        try {
            this.mSurfaceX = left;
            this.mSurfaceY = top;
            try {
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                    logSurface("POS (setPositionAndLayer) @ (" + left + "," + top + ")", null);
                }
                this.mSurfaceControl.setPosition(left, top);
                this.mSurfaceControl.setLayerStack(layerStack);
                this.mSurfaceControl.setLayer(layer);
                this.mSurfaceControl.setAlpha(0.0f);
                this.mSurfaceShown = false;
            } catch (RuntimeException e) {
                Slog.w(TAG, "Error creating surface in " + this, e);
                this.mAnimator.reclaimSomeSurfaceMemory("create-init", true);
            }
        } finally {
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION setPositionAndLayer");
            }
        }
    }

    void destroyInTransaction() {
        Slog.i(TAG, "Destroying surface " + this + " called by " + Debug.getCallers(8));
        try {
            try {
                if (this.mSurfaceControl != null) {
                    this.mSurfaceControl.destroy();
                }
                this.mSurfaceShown = false;
            } catch (RuntimeException e) {
                Slog.w(TAG, "Error destroying surface in: " + this, e);
                this.mSurfaceShown = false;
            }
            this.mSurfaceControl = null;
        } catch (Throwable th) {
            this.mSurfaceShown = false;
            this.mSurfaceControl = null;
            throw th;
        }
    }

    void disconnectInTransaction() {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
            Slog.i(TAG, "Disconnecting client: " + this);
        }
        try {
            if (this.mSurfaceControl == null) {
                return;
            }
            this.mSurfaceControl.disconnect();
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error disconnecting surface in: " + this, e);
        }
    }

    void setCropInTransaction(Rect clipRect, boolean recoveringMemory) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("CROP " + clipRect.toShortString(), null);
        }
        try {
            if (clipRect.width() > 0 && clipRect.height() > 0) {
                this.mSurfaceControl.setWindowCrop(clipRect);
                this.mHiddenForCrop = false;
                updateVisibility();
            } else {
                this.mHiddenForCrop = true;
                this.mAnimator.destroyPreservedSurfaceLocked();
                updateVisibility();
            }
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error setting crop surface of " + this + " crop=" + clipRect.toShortString(), e);
            if (recoveringMemory) {
                return;
            }
            this.mAnimator.reclaimSomeSurfaceMemory("crop", true);
        }
    }

    void clearCropInTransaction(boolean recoveringMemory) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("CLEAR CROP", null);
        }
        try {
            Rect clipRect = new Rect(0, 0, -1, -1);
            this.mSurfaceControl.setWindowCrop(clipRect);
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error setting clearing crop of " + this, e);
            if (recoveringMemory) {
                return;
            }
            this.mAnimator.reclaimSomeSurfaceMemory("crop", true);
        }
    }

    void setFinalCropInTransaction(Rect clipRect) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("FINAL CROP " + clipRect.toShortString(), null);
        }
        try {
            this.mSurfaceControl.setFinalCrop(clipRect);
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error disconnecting surface in: " + this, e);
        }
    }

    void setLayer(int layer) {
        if (this.mSurfaceControl == null) {
            return;
        }
        SurfaceControl.openTransaction();
        try {
            this.mSurfaceControl.setLayer(layer);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    void setPositionInTransaction(float left, float top, boolean recoveringMemory) {
        boolean surfaceMoved = (this.mSurfaceX == left && this.mSurfaceY == top) ? false : true;
        if (surfaceMoved) {
            this.mSurfaceX = left;
            this.mSurfaceY = top;
            try {
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                    logSurface("POS (setPositionInTransaction) @ (" + left + "," + top + ")", null);
                }
                this.mSurfaceControl.setPosition(left, top);
            } catch (RuntimeException e) {
                Slog.w(TAG, "Error positioning surface of " + this + " pos=(" + left + "," + top + ")", e);
                if (recoveringMemory) {
                    return;
                }
                this.mAnimator.reclaimSomeSurfaceMemory("position", true);
            }
        }
    }

    void setPositionAppliesWithResizeInTransaction(boolean recoveringMemory) {
        this.mSurfaceControl.setPositionAppliesWithResize();
    }

    void setMatrixInTransaction(float dsdx, float dtdx, float dsdy, float dtdy, boolean recoveringMemory) {
        try {
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                logSurface("MATRIX [" + dsdx + "," + dtdx + "," + dsdy + "," + dtdy + "]", null);
            }
            this.mSurfaceControl.setMatrix(dsdx, dtdx, dsdy, dtdy);
        } catch (RuntimeException e) {
            Slog.e(TAG, "Error setting matrix on surface surface" + this.title + " MATRIX [" + dsdx + "," + dtdx + "," + dsdy + "," + dtdy + "]", (Throwable) null);
            if (recoveringMemory) {
                return;
            }
            this.mAnimator.reclaimSomeSurfaceMemory("matrix", true);
        }
    }

    boolean setSizeInTransaction(int width, int height, boolean recoveringMemory) {
        boolean surfaceResized = (this.mSurfaceW == ((float) width) && this.mSurfaceH == ((float) height)) ? false : true;
        if (!surfaceResized) {
            return false;
        }
        this.mSurfaceW = width;
        this.mSurfaceH = height;
        try {
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                logSurface("SIZE " + width + "x" + height, null);
            }
            this.mSurfaceControl.setSize(width, height);
            return true;
        } catch (RuntimeException e) {
            Slog.e(TAG, "Error resizing surface of " + this.title + " size=(" + width + "x" + height + ")", e);
            if (!recoveringMemory) {
                this.mAnimator.reclaimSomeSurfaceMemory("size", true);
            }
            return false;
        }
    }

    boolean prepareToShowInTransaction(float alpha, int layer, float dsdx, float dtdx, float dsdy, float dtdy, boolean recoveringMemory) {
        if (this.mSurfaceControl != null) {
            try {
                this.mSurfaceAlpha = alpha;
                this.mSurfaceControl.setAlpha(alpha);
                this.mSurfaceLayer = layer;
                this.mSurfaceControl.setLayer(layer);
                this.mSurfaceControl.setMatrix(dsdx, dtdx, dsdy, dtdy);
            } catch (RuntimeException e) {
                Slog.w(TAG, "Error updating surface in " + this.title, e);
                if (!recoveringMemory) {
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
            Slog.w(TAG, "setTransparentRegionHint: null mSurface after mHasSurface true");
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i(TAG, ">>> OPEN TRANSACTION setTransparentRegion");
        }
        SurfaceControl.openTransaction();
        try {
            this.mSurfaceControl.setTransparentRegionHint(region);
        } finally {
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION setTransparentRegion");
            }
        }
    }

    void setOpaque(boolean isOpaque) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("isOpaque=" + isOpaque, null);
        }
        if (this.mSurfaceControl == null) {
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i(TAG, ">>> OPEN TRANSACTION setOpaqueLocked");
        }
        SurfaceControl.openTransaction();
        try {
            this.mSurfaceControl.setOpaque(isOpaque);
        } finally {
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION setOpaqueLocked");
            }
        }
    }

    void setSecure(boolean isSecure) {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("isSecure=" + isSecure, null);
        }
        if (this.mSurfaceControl == null) {
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i(TAG, ">>> OPEN TRANSACTION setSecureLocked");
        }
        SurfaceControl.openTransaction();
        try {
            this.mSurfaceControl.setSecure(isSecure);
        } finally {
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION setSecureLocked");
            }
        }
    }

    boolean showRobustlyInTransaction() {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            logSurface("SHOW (performLayout)", null);
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "Showing " + this + " during relayout");
        }
        this.mHiddenForOtherReasons = false;
        return updateVisibility();
    }

    private boolean updateVisibility() {
        if (this.mHiddenForCrop || this.mHiddenForOtherReasons) {
            if (this.mSurfaceShown) {
                hideSurface();
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
            this.mSurfaceShown = true;
            this.mSurfaceControl.show();
            return true;
        } catch (RuntimeException e) {
            Slog.w(TAG, "Failure showing surface " + this.mSurfaceControl + " in " + this, e);
            this.mAnimator.reclaimSomeSurfaceMemory("show", true);
            return false;
        }
    }

    void deferTransactionUntil(IBinder handle, long frame) {
        this.mSurfaceControl.deferTransactionUntil(handle, frame);
    }

    void forceScaleableInTransaction(boolean force) {
        int scalingMode = force ? 1 : -1;
        this.mSurfaceControl.setOverrideScalingMode(scalingMode);
    }

    boolean clearWindowContentFrameStats() {
        if (this.mSurfaceControl == null) {
            return false;
        }
        return this.mSurfaceControl.clearContentFrameStats();
    }

    boolean getWindowContentFrameStats(WindowContentFrameStats outStats) {
        if (this.mSurfaceControl == null) {
            return false;
        }
        return this.mSurfaceControl.getContentFrameStats(outStats);
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

    void getSurface(Surface outSurface) {
        outSurface.copyFrom(this.mSurfaceControl);
    }

    int getLayer() {
        return this.mSurfaceLayer;
    }

    boolean getShown() {
        return this.mSurfaceShown;
    }

    void setShown(boolean surfaceShown) {
        this.mSurfaceShown = surfaceShown;
    }

    float getX() {
        return this.mSurfaceX;
    }

    float getY() {
        return this.mSurfaceY;
    }

    float getWidth() {
        return this.mSurfaceW;
    }

    float getHeight() {
        return this.mSurfaceH;
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mSurface=");
            pw.println(this.mSurfaceControl);
        }
        pw.print(prefix);
        pw.print("Surface: shown=");
        pw.print(this.mSurfaceShown);
        pw.print(" layer=");
        pw.print(this.mSurfaceLayer);
        pw.print(" alpha=");
        pw.print(this.mSurfaceAlpha);
        pw.print(" rect=(");
        pw.print(this.mSurfaceX);
        pw.print(",");
        pw.print(this.mSurfaceY);
        pw.print(") ");
        pw.print(this.mSurfaceW);
        pw.print(" x ");
        pw.println(this.mSurfaceH);
    }

    public String toString() {
        return this.mSurfaceControl.toString();
    }

    static class SurfaceTrace extends SurfaceControl {
        private static final String SURFACE_TAG = "WindowManager";
        static final ArrayList<SurfaceTrace> sSurfaces = new ArrayList<>();
        private final boolean LOG_SURFACE_TRACE;
        private float mDsdx;
        private float mDsdy;
        private float mDtdx;
        private float mDtdy;
        private final Rect mFinalCrop;
        private boolean mIsOpaque;
        private int mLayer;
        private int mLayerStack;
        private final String mName;
        private final PointF mPosition;
        private boolean mShown;
        private final Point mSize;
        private float mSurfaceTraceAlpha;
        private final Rect mWindowCrop;

        public SurfaceTrace(SurfaceSession s, String name, int w, int h, int format, int flags) throws Surface.OutOfResourcesException {
            super(s, name, w, h, format, flags);
            this.LOG_SURFACE_TRACE = WindowManagerDebugConfig.DEBUG_SURFACE_TRACE;
            this.mSurfaceTraceAlpha = 0.0f;
            this.mPosition = new PointF();
            this.mSize = new Point();
            this.mWindowCrop = new Rect();
            this.mFinalCrop = new Rect();
            this.mShown = false;
            this.mName = name == null ? "Not named" : name;
            this.mSize.set(w, h);
            if (this.LOG_SURFACE_TRACE) {
                Slog.v(SURFACE_TAG, "ctor: " + this + ". Called by " + Debug.getCallers(3));
            }
            synchronized (sSurfaces) {
                sSurfaces.add(0, this);
            }
        }

        public void setAlpha(float alpha) {
            if (this.mSurfaceTraceAlpha != alpha) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setAlpha(" + alpha + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mSurfaceTraceAlpha = alpha;
            }
            super.setAlpha(alpha);
        }

        public void setLayer(int zorder) {
            if (zorder != this.mLayer) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setLayer(" + zorder + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mLayer = zorder;
            }
            super.setLayer(zorder);
            synchronized (sSurfaces) {
                sSurfaces.remove(this);
                int i = sSurfaces.size() - 1;
                while (i >= 0) {
                    SurfaceTrace s = sSurfaces.get(i);
                    if (s.mLayer < zorder) {
                        break;
                    } else {
                        i--;
                    }
                }
                sSurfaces.add(i + 1, this);
            }
        }

        public void setPosition(float x, float y) {
            if (x != this.mPosition.x || y != this.mPosition.y) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setPosition(" + x + "," + y + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mPosition.set(x, y);
            }
            super.setPosition(x, y);
        }

        public void setPositionAppliesWithResize() {
            if (this.LOG_SURFACE_TRACE) {
                Slog.v(SURFACE_TAG, "setPositionAppliesWithResize(): OLD: " + this + ". Called by" + Debug.getCallers(9));
            }
            super.setPositionAppliesWithResize();
        }

        public void setSize(int w, int h) {
            if (w != this.mSize.x || h != this.mSize.y) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setSize(" + w + "," + h + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mSize.set(w, h);
            }
            super.setSize(w, h);
        }

        public void setWindowCrop(Rect crop) {
            if (crop != null && !crop.equals(this.mWindowCrop)) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setWindowCrop(" + crop.toShortString() + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mWindowCrop.set(crop);
            }
            super.setWindowCrop(crop);
        }

        public void setFinalCrop(Rect crop) {
            if (crop != null && !crop.equals(this.mFinalCrop)) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setFinalCrop(" + crop.toShortString() + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mFinalCrop.set(crop);
            }
            super.setFinalCrop(crop);
        }

        public void setLayerStack(int layerStack) {
            if (layerStack != this.mLayerStack) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setLayerStack(" + layerStack + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mLayerStack = layerStack;
            }
            super.setLayerStack(layerStack);
        }

        public void setOpaque(boolean isOpaque) {
            if (isOpaque != this.mIsOpaque) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setOpaque(" + isOpaque + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mIsOpaque = isOpaque;
            }
            super.setOpaque(isOpaque);
        }

        public void setSecure(boolean isSecure) {
            super.setSecure(isSecure);
        }

        public void setMatrix(float dsdx, float dtdx, float dsdy, float dtdy) {
            if (dsdx != this.mDsdx || dtdx != this.mDtdx || dsdy != this.mDsdy || dtdy != this.mDtdy) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "setMatrix(" + dsdx + "," + dtdx + "," + dsdy + "," + dtdy + "): OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mDsdx = dsdx;
                this.mDtdx = dtdx;
                this.mDsdy = dsdy;
                this.mDtdy = dtdy;
            }
            super.setMatrix(dsdx, dtdx, dsdy, dtdy);
        }

        public void hide() {
            if (this.mShown) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "hide: OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mShown = false;
            }
            super.hide();
        }

        public void show() {
            if (!this.mShown) {
                if (this.LOG_SURFACE_TRACE) {
                    Slog.v(SURFACE_TAG, "show: OLD:" + this + ". Called by " + Debug.getCallers(3));
                }
                this.mShown = true;
            }
            super.show();
        }

        public void destroy() {
            super.destroy();
            if (this.LOG_SURFACE_TRACE) {
                Slog.v(SURFACE_TAG, "destroy: " + this + ". Called by " + Debug.getCallers(3));
            }
            synchronized (sSurfaces) {
                sSurfaces.remove(this);
            }
        }

        @Override
        public void release() {
            super.release();
            if (this.LOG_SURFACE_TRACE) {
                Slog.v(SURFACE_TAG, "release: " + this + ". Called by " + Debug.getCallers(3));
            }
            synchronized (sSurfaces) {
                sSurfaces.remove(this);
            }
        }

        public void setTransparentRegionHint(Region region) {
            if (this.LOG_SURFACE_TRACE) {
                Slog.v(SURFACE_TAG, "setTransparentRegionHint(" + region + "): OLD: " + this + " . Called by " + Debug.getCallers(3));
            }
            super.setTransparentRegionHint(region);
        }

        static void dumpAllSurfaces(PrintWriter pw, String header) {
            synchronized (sSurfaces) {
                int N = sSurfaces.size();
                if (N <= 0) {
                    return;
                }
                if (header != null) {
                    pw.println(header);
                }
                pw.println("WINDOW MANAGER SURFACES (dumpsys window surfaces)");
                for (int i = 0; i < N; i++) {
                    SurfaceTrace s = sSurfaces.get(i);
                    pw.print("  Surface #");
                    pw.print(i);
                    pw.print(": #");
                    pw.print(Integer.toHexString(System.identityHashCode(s)));
                    pw.print(" ");
                    pw.println(s.mName);
                    pw.print("    mLayerStack=");
                    pw.print(s.mLayerStack);
                    pw.print(" mLayer=");
                    pw.println(s.mLayer);
                    pw.print("    mShown=");
                    pw.print(s.mShown);
                    pw.print(" mAlpha=");
                    pw.print(s.mSurfaceTraceAlpha);
                    pw.print(" mIsOpaque=");
                    pw.println(s.mIsOpaque);
                    pw.print("    mPosition=");
                    pw.print(s.mPosition.x);
                    pw.print(",");
                    pw.print(s.mPosition.y);
                    pw.print(" mSize=");
                    pw.print(s.mSize.x);
                    pw.print("x");
                    pw.println(s.mSize.y);
                    pw.print("    mCrop=");
                    s.mWindowCrop.printShortString(pw);
                    pw.println();
                    pw.print("    mFinalCrop=");
                    s.mFinalCrop.printShortString(pw);
                    pw.println();
                    pw.print("    Transform: (");
                    pw.print(s.mDsdx);
                    pw.print(", ");
                    pw.print(s.mDtdx);
                    pw.print(", ");
                    pw.print(s.mDsdy);
                    pw.print(", ");
                    pw.print(s.mDtdy);
                    pw.println(")");
                }
            }
        }

        @Override
        public String toString() {
            return "Surface " + Integer.toHexString(System.identityHashCode(this)) + " " + this.mName + " (" + this.mLayerStack + "): shown=" + this.mShown + " layer=" + this.mLayer + " alpha=" + this.mSurfaceTraceAlpha + " " + this.mPosition.x + "," + this.mPosition.y + " " + this.mSize.x + "x" + this.mSize.y + " crop=" + this.mWindowCrop.toShortString() + " opaque=" + this.mIsOpaque + " (" + this.mDsdx + "," + this.mDtdx + "," + this.mDsdy + "," + this.mDtdy + ")";
        }
    }

    class SurfaceControlWithBackground extends SurfaceTrace {
        private boolean mAppForcedInvisible;
        private AppWindowToken mAppToken;
        private SurfaceControl mBackgroundControl;
        public int mLayer;
        private boolean mOpaque;
        public boolean mVisible;

        public SurfaceControlWithBackground(SurfaceSession s, String name, int w, int h, int format, int flags, AppWindowToken token) throws Surface.OutOfResourcesException {
            super(s, name, w, h, format, flags);
            this.mOpaque = true;
            this.mAppForcedInvisible = false;
            this.mVisible = false;
            this.mLayer = -1;
            this.mBackgroundControl = new SurfaceControl(s, name, w, h, -1, flags | PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS);
            this.mOpaque = (flags & 1024) != 0;
            this.mAppToken = token;
            this.mAppToken.addSurfaceViewBackground(this);
        }

        @Override
        public void setAlpha(float alpha) {
            super.setAlpha(alpha);
            this.mBackgroundControl.setAlpha(alpha);
        }

        @Override
        public void setLayer(int zorder) {
            super.setLayer(zorder);
            this.mBackgroundControl.setLayer(zorder - 1);
            if (this.mLayer == zorder) {
                return;
            }
            this.mLayer = zorder;
            this.mAppToken.updateSurfaceViewBackgroundVisibilities();
        }

        @Override
        public void setPosition(float x, float y) {
            super.setPosition(x, y);
            this.mBackgroundControl.setPosition(x, y);
        }

        @Override
        public void setSize(int w, int h) {
            super.setSize(w, h);
            this.mBackgroundControl.setSize(w, h);
        }

        @Override
        public void setWindowCrop(Rect crop) {
            super.setWindowCrop(crop);
            this.mBackgroundControl.setWindowCrop(crop);
        }

        @Override
        public void setFinalCrop(Rect crop) {
            super.setFinalCrop(crop);
            this.mBackgroundControl.setFinalCrop(crop);
        }

        @Override
        public void setLayerStack(int layerStack) {
            super.setLayerStack(layerStack);
            this.mBackgroundControl.setLayerStack(layerStack);
        }

        @Override
        public void setOpaque(boolean isOpaque) {
            super.setOpaque(isOpaque);
            this.mOpaque = isOpaque;
            updateBackgroundVisibility(this.mAppForcedInvisible);
        }

        @Override
        public void setSecure(boolean isSecure) {
            super.setSecure(isSecure);
        }

        @Override
        public void setMatrix(float dsdx, float dtdx, float dsdy, float dtdy) {
            super.setMatrix(dsdx, dtdx, dsdy, dtdy);
            this.mBackgroundControl.setMatrix(dsdx, dtdx, dsdy, dtdy);
        }

        @Override
        public void hide() {
            super.hide();
            if (!this.mVisible) {
                return;
            }
            this.mVisible = false;
            this.mAppToken.updateSurfaceViewBackgroundVisibilities();
        }

        @Override
        public void show() {
            super.show();
            if (this.mVisible) {
                return;
            }
            this.mVisible = true;
            this.mAppToken.updateSurfaceViewBackgroundVisibilities();
        }

        @Override
        public void destroy() {
            super.destroy();
            this.mBackgroundControl.destroy();
            this.mAppToken.removeSurfaceViewBackground(this);
        }

        @Override
        public void release() {
            super.release();
            this.mBackgroundControl.release();
        }

        @Override
        public void setTransparentRegionHint(Region region) {
            super.setTransparentRegionHint(region);
            this.mBackgroundControl.setTransparentRegionHint(region);
        }

        public void deferTransactionUntil(IBinder handle, long frame) {
            super.deferTransactionUntil(handle, frame);
            this.mBackgroundControl.deferTransactionUntil(handle, frame);
        }

        void updateBackgroundVisibility(boolean forcedInvisible) {
            this.mAppForcedInvisible = forcedInvisible;
            if (this.mOpaque && this.mVisible && !this.mAppForcedInvisible) {
                this.mBackgroundControl.show();
            } else {
                this.mBackgroundControl.hide();
            }
        }
    }
}
