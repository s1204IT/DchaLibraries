package com.android.server.wm;

import android.R;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.MagnificationSpec;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import com.android.server.pm.PackageManagerService;
import com.android.server.voiceinteraction.SoundTriggerHelper;
import java.io.PrintWriter;
import java.util.ArrayList;

class WindowStateAnimator {
    static final int COMMIT_DRAW_PENDING = 2;
    static final int DRAW_PENDING = 1;
    static final int HAS_DRAWN = 4;
    static final int NO_SURFACE = 0;
    static final int READY_TO_SHOW = 3;
    private static final int SYSTEM_UI_FLAGS_LAYOUT_STABLE_FULLSCREEN = 1280;
    static final String TAG = "WindowStateAnimator";
    int mAnimDh;
    int mAnimDw;
    int mAnimLayer;
    boolean mAnimating;
    Animation mAnimation;
    boolean mAnimationIsEntrance;
    long mAnimationStartTime;
    final WindowAnimator mAnimator;
    AppWindowAnimator mAppAnimator;
    final WindowStateAnimator mAttachedWinAnimator;
    int mAttrType;
    final Context mContext;
    int mDrawState;
    boolean mEnterAnimationPending;
    boolean mEnteringAnimation;
    boolean mHasClipRect;
    boolean mHasLocalTransformation;
    boolean mHasTransformation;
    boolean mHaveMatrix;
    final boolean mIsWallpaper;
    boolean mKeyguardGoingAwayAnimation;
    long mLastAnimationTime;
    boolean mLastHidden;
    int mLastLayer;
    boolean mLocalAnimating;
    SurfaceControl mPendingDestroySurface;
    final WindowManagerPolicy mPolicy;
    final WindowManagerService mService;
    final Session mSession;
    float mSurfaceAlpha;
    SurfaceControl mSurfaceControl;
    boolean mSurfaceDestroyDeferred;
    float mSurfaceH;
    int mSurfaceLayer;
    boolean mSurfaceResized;
    boolean mSurfaceShown;
    float mSurfaceW;
    float mSurfaceX;
    float mSurfaceY;
    boolean mWasAnimating;
    final WindowState mWin;
    final Transformation mUniverseTransform = new Transformation();
    final Transformation mTransformation = new Transformation();
    float mShownAlpha = 0.0f;
    float mAlpha = 0.0f;
    float mLastAlpha = 0.0f;
    Rect mClipRect = new Rect();
    Rect mTmpClipRect = new Rect();
    Rect mLastClipRect = new Rect();
    float mDsDx = 1.0f;
    float mDtDx = 0.0f;
    float mDsDy = 0.0f;
    float mDtDy = 1.0f;
    float mLastDsDx = 1.0f;
    float mLastDtDx = 0.0f;
    float mLastDsDy = 0.0f;
    float mLastDtDy = 1.0f;

    String drawStateToString() {
        switch (this.mDrawState) {
            case 0:
                return "NO_SURFACE";
            case 1:
                return "DRAW_PENDING";
            case 2:
                return "COMMIT_DRAW_PENDING";
            case 3:
                return "READY_TO_SHOW";
            case 4:
                return "HAS_DRAWN";
            default:
                return Integer.toString(this.mDrawState);
        }
    }

    public WindowStateAnimator(WindowState win) {
        WindowManagerService service = win.mService;
        this.mService = service;
        this.mAnimator = service.mAnimator;
        this.mPolicy = service.mPolicy;
        this.mContext = service.mContext;
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent != null) {
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            this.mAnimDw = displayInfo.appWidth;
            this.mAnimDh = displayInfo.appHeight;
        } else {
            Slog.w(TAG, "WindowStateAnimator ctor: Display has been removed");
        }
        this.mWin = win;
        this.mAttachedWinAnimator = win.mAttachedWindow == null ? null : win.mAttachedWindow.mWinAnimator;
        this.mAppAnimator = win.mAppToken != null ? win.mAppToken.mAppAnimator : null;
        this.mSession = win.mSession;
        this.mAttrType = win.mAttrs.type;
        this.mIsWallpaper = win.mIsWallpaper;
    }

    public void setAnimation(Animation anim, long startTime) {
        this.mAnimating = false;
        this.mLocalAnimating = false;
        this.mAnimation = anim;
        this.mAnimation.restrictDuration(10000L);
        this.mAnimation.scaleCurrentDuration(this.mService.getWindowAnimationScaleLocked());
        this.mTransformation.clear();
        this.mTransformation.setAlpha(this.mLastHidden ? 0.0f : 1.0f);
        this.mHasLocalTransformation = true;
        this.mAnimationStartTime = startTime;
    }

    public void setAnimation(Animation anim) {
        setAnimation(anim, -1L);
    }

    public void clearAnimation() {
        if (this.mAnimation != null) {
            this.mAnimating = true;
            this.mLocalAnimating = false;
            this.mAnimation.cancel();
            this.mAnimation = null;
            this.mKeyguardGoingAwayAnimation = false;
        }
    }

    boolean isAnimating() {
        return this.mAnimation != null || !(this.mAttachedWinAnimator == null || this.mAttachedWinAnimator.mAnimation == null) || (this.mAppAnimator != null && (this.mAppAnimator.animation != null || this.mAppAnimator.mAppToken.inPendingTransaction));
    }

    boolean isDummyAnimation() {
        return this.mAppAnimator != null && this.mAppAnimator.animation == AppWindowAnimator.sDummyAnimation;
    }

    boolean isWindowAnimating() {
        return this.mAnimation != null;
    }

    void cancelExitAnimationForNextAnimationLocked() {
        if (this.mAnimation != null) {
            this.mAnimation.cancel();
            this.mAnimation = null;
            this.mLocalAnimating = false;
            destroySurfaceLocked();
        }
    }

    private boolean stepAnimation(long currentTime) {
        if (this.mAnimation == null || !this.mLocalAnimating) {
            return false;
        }
        this.mTransformation.clear();
        return this.mAnimation.getTransformation(currentTime, this.mTransformation);
    }

    boolean stepAnimationLocked(long currentTime) {
        this.mWasAnimating = this.mAnimating;
        DisplayContent displayContent = this.mWin.getDisplayContent();
        if (displayContent != null && this.mService.okToDisplay()) {
            if (this.mWin.isDrawnLw() && this.mAnimation != null) {
                this.mHasTransformation = true;
                this.mHasLocalTransformation = true;
                if (!this.mLocalAnimating) {
                    this.mAnimation.initialize(this.mWin.mFrame.width(), this.mWin.mFrame.height(), this.mAnimDw, this.mAnimDh);
                    DisplayInfo displayInfo = displayContent.getDisplayInfo();
                    this.mAnimDw = displayInfo.appWidth;
                    this.mAnimDh = displayInfo.appHeight;
                    this.mAnimation.setStartTime(this.mAnimationStartTime != -1 ? this.mAnimationStartTime : currentTime);
                    this.mLocalAnimating = true;
                    this.mAnimating = true;
                }
                if (this.mAnimation != null && this.mLocalAnimating) {
                    this.mLastAnimationTime = currentTime;
                    if (stepAnimation(currentTime)) {
                        return true;
                    }
                }
            }
            this.mHasLocalTransformation = false;
            if ((!this.mLocalAnimating || this.mAnimationIsEntrance) && this.mAppAnimator != null && this.mAppAnimator.animation != null) {
                this.mAnimating = true;
                this.mHasTransformation = true;
                this.mTransformation.clear();
                return false;
            }
            if (this.mHasTransformation || isAnimating()) {
                this.mAnimating = true;
            }
        } else if (this.mAnimation != null) {
            this.mAnimating = true;
        }
        if (!this.mAnimating && !this.mLocalAnimating) {
            return false;
        }
        this.mAnimating = false;
        this.mKeyguardGoingAwayAnimation = false;
        this.mLocalAnimating = false;
        if (this.mAnimation != null) {
            this.mAnimation.cancel();
            this.mAnimation = null;
        }
        if (this.mAnimator.mWindowDetachedWallpaper == this.mWin) {
            this.mAnimator.mWindowDetachedWallpaper = null;
        }
        this.mAnimLayer = this.mWin.mLayer;
        if (this.mWin.mIsImWindow) {
            this.mAnimLayer += this.mService.mInputMethodAnimLayerAdjustment;
        } else if (this.mIsWallpaper) {
            this.mAnimLayer += this.mService.mWallpaperAnimLayerAdjustment;
        }
        this.mHasTransformation = false;
        this.mHasLocalTransformation = false;
        if (this.mWin.mPolicyVisibility != this.mWin.mPolicyVisibilityAfterAnim) {
            this.mWin.mPolicyVisibility = this.mWin.mPolicyVisibilityAfterAnim;
            if (displayContent != null) {
                displayContent.layoutNeeded = true;
            }
            if (!this.mWin.mPolicyVisibility) {
                if (this.mService.mCurrentFocus == this.mWin) {
                    this.mService.mFocusMayChange = true;
                }
                this.mService.enableScreenIfNeededLocked();
            }
        }
        this.mTransformation.clear();
        if (this.mDrawState == 4 && this.mWin.mAttrs.type == 3 && this.mWin.mAppToken != null && this.mWin.mAppToken.firstWindowDrawn && this.mWin.mAppToken.startingData != null) {
            this.mService.mFinishedStarting.add(this.mWin.mAppToken);
            this.mService.mH.sendEmptyMessage(7);
        } else if (this.mAttrType == 2000 && this.mWin.mPolicyVisibility && displayContent != null) {
            displayContent.layoutNeeded = true;
        }
        finishExit();
        int displayId = this.mWin.getDisplayId();
        this.mAnimator.setPendingLayoutChanges(displayId, 8);
        this.mService.debugLayoutRepeats(TAG, this.mAnimator.getPendingLayoutChanges(displayId));
        if (this.mWin.mAppToken != null) {
            this.mWin.mAppToken.updateReportedVisibilityLocked();
        }
        return false;
    }

    void finishExit() {
        int N = this.mWin.mChildWindows.size();
        for (int i = 0; i < N; i++) {
            this.mWin.mChildWindows.get(i).mWinAnimator.finishExit();
        }
        if (this.mEnteringAnimation && this.mWin.mAppToken == null) {
            try {
                this.mEnteringAnimation = false;
                this.mWin.mClient.dispatchWindowShown();
            } catch (RemoteException e) {
            }
        }
        if (!isWindowAnimating() && this.mService.mAccessibilityController != null && this.mWin.getDisplayId() == 0) {
            this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
        }
        if (this.mWin.mExiting && !isWindowAnimating()) {
            if (this.mSurfaceControl != null) {
                this.mService.mDestroySurface.add(this.mWin);
                this.mWin.mDestroying = true;
                hide();
            }
            this.mWin.mExiting = false;
            if (this.mWin.mRemoveOnExit) {
                this.mService.mPendingRemove.add(this.mWin);
                this.mWin.mRemoveOnExit = false;
            }
            this.mAnimator.hideWallpapersLocked(this.mWin);
        }
    }

    void hide() {
        if (!this.mLastHidden) {
            this.mLastHidden = true;
            if (this.mSurfaceControl != null) {
                this.mSurfaceShown = false;
                this.mService.updateNonSystemOverlayWindowsVisibilityIfNeeded(this.mWin, false);
                try {
                    this.mSurfaceControl.hide();
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Exception hiding surface in " + this.mWin);
                }
            }
        }
    }

    boolean finishDrawingLocked() {
        if (this.mWin.mAttrs.type == 3) {
        }
        if (this.mDrawState != 1) {
            return false;
        }
        this.mDrawState = 2;
        return true;
    }

    boolean commitFinishDrawingLocked(long currentTime) {
        if (this.mDrawState != 2 && this.mDrawState != 3) {
            return false;
        }
        this.mDrawState = 3;
        AppWindowToken atoken = this.mWin.mAppToken;
        if (atoken == null || atoken.allDrawn || this.mWin.mAttrs.type == 3) {
            performShowLocked();
        }
        return true;
    }

    static class SurfaceTrace extends SurfaceControl {
        private static final String SURFACE_TAG = "SurfaceTrace";
        private static final boolean logSurfaceTrace = false;
        static final ArrayList<SurfaceTrace> sSurfaces = new ArrayList<>();
        private float mDsdx;
        private float mDsdy;
        private float mDtdx;
        private float mDtdy;
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
            this.mSurfaceTraceAlpha = 0.0f;
            this.mPosition = new PointF();
            this.mSize = new Point();
            this.mWindowCrop = new Rect();
            this.mShown = false;
            this.mName = name == null ? "Not named" : name;
            this.mSize.set(w, h);
            synchronized (sSurfaces) {
                sSurfaces.add(0, this);
            }
        }

        public void setAlpha(float alpha) {
            if (this.mSurfaceTraceAlpha != alpha) {
                this.mSurfaceTraceAlpha = alpha;
            }
            super.setAlpha(alpha);
        }

        public void setLayer(int zorder) {
            if (zorder != this.mLayer) {
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
                this.mPosition.set(x, y);
            }
            super.setPosition(x, y);
        }

        public void setSize(int w, int h) {
            if (w != this.mSize.x || h != this.mSize.y) {
                this.mSize.set(w, h);
            }
            super.setSize(w, h);
        }

        public void setWindowCrop(Rect crop) {
            if (crop != null && !crop.equals(this.mWindowCrop)) {
                this.mWindowCrop.set(crop);
            }
            super.setWindowCrop(crop);
        }

        public void setLayerStack(int layerStack) {
            if (layerStack != this.mLayerStack) {
                this.mLayerStack = layerStack;
            }
            super.setLayerStack(layerStack);
        }

        public void setOpaque(boolean isOpaque) {
            if (isOpaque != this.mIsOpaque) {
                this.mIsOpaque = isOpaque;
            }
            super.setOpaque(isOpaque);
        }

        public void setMatrix(float dsdx, float dtdx, float dsdy, float dtdy) {
            if (dsdx != this.mDsdx || dtdx != this.mDtdx || dsdy != this.mDsdy || dtdy != this.mDtdy) {
                this.mDsdx = dsdx;
                this.mDtdx = dtdx;
                this.mDsdy = dsdy;
                this.mDtdy = dtdy;
            }
            super.setMatrix(dsdx, dtdx, dsdy, dtdy);
        }

        public void hide() {
            if (this.mShown) {
                this.mShown = false;
            }
            super.hide();
        }

        public void show() {
            if (!this.mShown) {
                this.mShown = true;
            }
            super.show();
        }

        public void destroy() {
            super.destroy();
            synchronized (sSurfaces) {
                sSurfaces.remove(this);
            }
        }

        @Override
        public void release() {
            super.release();
            synchronized (sSurfaces) {
                sSurfaces.remove(this);
            }
        }

        static void dumpAllSurfaces(PrintWriter pw, String header) {
            synchronized (sSurfaces) {
                int N = sSurfaces.size();
                if (N > 0) {
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
        }

        @Override
        public String toString() {
            return "Surface " + Integer.toHexString(System.identityHashCode(this)) + " " + this.mName + " (" + this.mLayerStack + "): shown=" + this.mShown + " layer=" + this.mLayer + " alpha=" + this.mSurfaceTraceAlpha + " " + this.mPosition.x + "," + this.mPosition.y + " " + this.mSize.x + "x" + this.mSize.y + " crop=" + this.mWindowCrop.toShortString() + " opaque=" + this.mIsOpaque + " (" + this.mDsdx + "," + this.mDtdx + "," + this.mDsdy + "," + this.mDtdy + ")";
        }
    }

    SurfaceControl createSurfaceLocked() {
        int width;
        int height;
        WindowState w = this.mWin;
        if (this.mSurfaceControl == null) {
            this.mDrawState = 1;
            if (w.mAppToken != null) {
                if (w.mAppToken.mAppAnimator.animation == null) {
                    w.mAppToken.allDrawn = false;
                    w.mAppToken.deferClearAllDrawn = false;
                } else {
                    w.mAppToken.deferClearAllDrawn = true;
                }
            }
            this.mService.makeWindowFreezingScreenIfNeededLocked(w);
            int flags = 4;
            WindowManager.LayoutParams attrs = w.mAttrs;
            if ((attrs.flags & PackageManagerService.DumpState.DUMP_INSTALLS) != 0) {
                flags = 4 | 128;
            }
            if (this.mService.isScreenCaptureDisabledLocked(UserHandle.getUserId(this.mWin.mOwnerUid))) {
                flags |= 128;
            }
            if ((attrs.flags & 16384) != 0) {
                width = w.mRequestedWidth;
                height = w.mRequestedHeight;
            } else {
                width = w.mCompatFrame.width();
                height = w.mCompatFrame.height();
            }
            if (width <= 0) {
                width = 1;
            }
            if (height <= 0) {
                height = 1;
            }
            float left = w.mFrame.left + w.mXOffset;
            float top = w.mFrame.top + w.mYOffset;
            int width2 = width + attrs.surfaceInsets.left + attrs.surfaceInsets.right;
            int height2 = height + attrs.surfaceInsets.top + attrs.surfaceInsets.bottom;
            float left2 = left - attrs.surfaceInsets.left;
            float top2 = top - attrs.surfaceInsets.top;
            this.mSurfaceShown = false;
            this.mSurfaceLayer = 0;
            this.mSurfaceAlpha = 0.0f;
            this.mSurfaceX = 0.0f;
            this.mSurfaceY = 0.0f;
            w.mLastSystemDecorRect.set(0, 0, 0, 0);
            this.mLastClipRect.set(0, 0, 0, 0);
            try {
                this.mSurfaceW = width2;
                this.mSurfaceH = height2;
                boolean isHwAccelerated = (attrs.flags & 16777216) != 0;
                int format = isHwAccelerated ? -3 : attrs.format;
                if (!PixelFormat.formatHasAlpha(attrs.format) && attrs.surfaceInsets.left == 0 && attrs.surfaceInsets.top == 0 && attrs.surfaceInsets.right == 0 && attrs.surfaceInsets.bottom == 0) {
                    flags |= 1024;
                }
                this.mSurfaceControl = new SurfaceControl(this.mSession.mSurfaceSession, attrs.getTitle().toString(), width2, height2, format, flags);
                w.mHasSurface = true;
                SurfaceControl.openTransaction();
                try {
                    this.mSurfaceX = left2;
                    this.mSurfaceY = top2;
                    try {
                        this.mSurfaceControl.setPosition(left2, top2);
                        this.mSurfaceLayer = this.mAnimLayer;
                        DisplayContent displayContent = w.getDisplayContent();
                        if (displayContent != null) {
                            this.mSurfaceControl.setLayerStack(displayContent.getDisplay().getLayerStack());
                        }
                        this.mSurfaceControl.setLayer(this.mAnimLayer);
                        this.mSurfaceControl.setAlpha(0.0f);
                        this.mSurfaceShown = false;
                    } catch (RuntimeException e) {
                        Slog.w(TAG, "Error creating surface in " + w, e);
                        this.mService.reclaimSomeSurfaceMemoryLocked(this, "create-init", true);
                    }
                    this.mLastHidden = true;
                } finally {
                    SurfaceControl.closeTransaction();
                }
            } catch (Surface.OutOfResourcesException e2) {
                w.mHasSurface = false;
                Slog.w(TAG, "OutOfResourcesException creating surface");
                this.mService.reclaimSomeSurfaceMemoryLocked(this, "create", true);
                this.mDrawState = 0;
                return null;
            } catch (Exception e3) {
                w.mHasSurface = false;
                Slog.e(TAG, "Exception creating surface", e3);
                this.mDrawState = 0;
                return null;
            }
        }
        return this.mSurfaceControl;
    }

    void destroySurfaceLocked() {
        if (this.mWin.mAppToken != null && this.mWin == this.mWin.mAppToken.startingWindow) {
            this.mWin.mAppToken.startingDisplayed = false;
        }
        if (this.mSurfaceControl != null) {
            int i = this.mWin.mChildWindows.size();
            while (i > 0) {
                i--;
                WindowState c = this.mWin.mChildWindows.get(i);
                c.mAttachedHidden = true;
            }
            try {
                if (this.mSurfaceDestroyDeferred) {
                    if (this.mSurfaceControl != null && this.mPendingDestroySurface != this.mSurfaceControl) {
                        if (this.mPendingDestroySurface != null) {
                            this.mPendingDestroySurface.destroy();
                        }
                        this.mPendingDestroySurface = this.mSurfaceControl;
                    }
                } else {
                    this.mSurfaceControl.destroy();
                }
                this.mAnimator.hideWallpapersLocked(this.mWin);
            } catch (RuntimeException e) {
                Slog.w(TAG, "Exception thrown when destroying Window " + this + " surface " + this.mSurfaceControl + " session " + this.mSession + ": " + e.toString());
            }
            this.mSurfaceShown = false;
            this.mSurfaceControl = null;
            this.mWin.mHasSurface = false;
            this.mDrawState = 0;
        }
    }

    void destroyDeferredSurfaceLocked() {
        try {
            if (this.mPendingDestroySurface != null) {
                this.mPendingDestroySurface.destroy();
                this.mAnimator.hideWallpapersLocked(this.mWin);
            }
        } catch (RuntimeException e) {
            Slog.w(TAG, "Exception thrown when destroying Window " + this + " surface " + this.mPendingDestroySurface + " session " + this.mSession + ": " + e.toString());
        }
        this.mSurfaceDestroyDeferred = false;
        this.mPendingDestroySurface = null;
    }

    void computeShownFrameLocked() {
        MagnificationSpec spec;
        boolean selfTransformation = this.mHasLocalTransformation;
        Transformation attachedTransformation = (this.mAttachedWinAnimator == null || !this.mAttachedWinAnimator.mHasLocalTransformation) ? null : this.mAttachedWinAnimator.mTransformation;
        Transformation appTransformation = (this.mAppAnimator == null || !this.mAppAnimator.hasTransformation) ? null : this.mAppAnimator.transformation;
        WindowState wallpaperTarget = this.mService.mWallpaperTarget;
        if (this.mIsWallpaper && wallpaperTarget != null && this.mService.mAnimateWallpaperWithTarget) {
            WindowStateAnimator wallpaperAnimator = wallpaperTarget.mWinAnimator;
            if (wallpaperAnimator.mHasLocalTransformation && wallpaperAnimator.mAnimation != null && !wallpaperAnimator.mAnimation.getDetachWallpaper()) {
                attachedTransformation = wallpaperAnimator.mTransformation;
            }
            AppWindowAnimator wpAppAnimator = wallpaperTarget.mAppToken == null ? null : wallpaperTarget.mAppToken.mAppAnimator;
            if (wpAppAnimator != null && wpAppAnimator.hasTransformation && wpAppAnimator.animation != null && !wpAppAnimator.animation.getDetachWallpaper()) {
                appTransformation = wpAppAnimator.transformation;
            }
        }
        int displayId = this.mWin.getDisplayId();
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(displayId);
        boolean screenAnimation = screenRotationAnimation != null && screenRotationAnimation.isAnimating();
        if (selfTransformation || attachedTransformation != null || appTransformation != null || screenAnimation) {
            Rect frame = this.mWin.mFrame;
            float[] tmpFloats = this.mService.mTmpFloats;
            Matrix tmpMatrix = this.mWin.mTmpMatrix;
            if (screenAnimation && screenRotationAnimation.isRotating()) {
                float w = frame.width();
                float h = frame.height();
                if (w >= 1.0f && h >= 1.0f) {
                    tmpMatrix.setScale(1.0f + (2.0f / w), 1.0f + (2.0f / h), w / 2.0f, h / 2.0f);
                } else {
                    tmpMatrix.reset();
                }
            } else {
                tmpMatrix.reset();
            }
            tmpMatrix.postScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
            if (selfTransformation) {
                tmpMatrix.postConcat(this.mTransformation.getMatrix());
            }
            tmpMatrix.postTranslate(frame.left + this.mWin.mXOffset, frame.top + this.mWin.mYOffset);
            if (attachedTransformation != null) {
                tmpMatrix.postConcat(attachedTransformation.getMatrix());
            }
            if (appTransformation != null) {
                tmpMatrix.postConcat(appTransformation.getMatrix());
            }
            if (this.mAnimator.mUniverseBackground != null) {
                tmpMatrix.postConcat(this.mAnimator.mUniverseBackground.mUniverseTransform.getMatrix());
            }
            if (screenAnimation) {
                tmpMatrix.postConcat(screenRotationAnimation.getEnterTransformation().getMatrix());
            }
            if (this.mService.mAccessibilityController != null && displayId == 0 && (spec = this.mService.mAccessibilityController.getMagnificationSpecForWindowLocked(this.mWin)) != null && !spec.isNop()) {
                tmpMatrix.postScale(spec.scale, spec.scale);
                tmpMatrix.postTranslate(spec.offsetX, spec.offsetY);
            }
            this.mHaveMatrix = true;
            tmpMatrix.getValues(tmpFloats);
            this.mDsDx = tmpFloats[0];
            this.mDtDx = tmpFloats[3];
            this.mDsDy = tmpFloats[1];
            this.mDtDy = tmpFloats[4];
            float x = tmpFloats[2];
            float y = tmpFloats[5];
            this.mWin.mShownFrame.set(x, y, frame.width() + x, frame.height() + y);
            this.mShownAlpha = this.mAlpha;
            this.mHasClipRect = false;
            if (!this.mService.mLimitedAlphaCompositing || !PixelFormat.formatHasAlpha(this.mWin.mAttrs.format) || (this.mWin.isIdentityMatrix(this.mDsDx, this.mDtDx, this.mDsDy, this.mDtDy) && x == frame.left && y == frame.top)) {
                if (selfTransformation) {
                    this.mShownAlpha *= this.mTransformation.getAlpha();
                }
                if (attachedTransformation != null) {
                    this.mShownAlpha *= attachedTransformation.getAlpha();
                }
                if (appTransformation != null) {
                    this.mShownAlpha *= appTransformation.getAlpha();
                    if (appTransformation.hasClipRect()) {
                        this.mClipRect.set(appTransformation.getClipRect());
                        if (this.mWin.mHScale > 0.0f) {
                            this.mClipRect.left = (int) (r0.left / this.mWin.mHScale);
                            this.mClipRect.right = (int) (r0.right / this.mWin.mHScale);
                        }
                        if (this.mWin.mVScale > 0.0f) {
                            this.mClipRect.top = (int) (r0.top / this.mWin.mVScale);
                            this.mClipRect.bottom = (int) (r0.bottom / this.mWin.mVScale);
                        }
                        this.mHasClipRect = true;
                    }
                }
                if (this.mAnimator.mUniverseBackground != null) {
                    this.mShownAlpha *= this.mAnimator.mUniverseBackground.mUniverseTransform.getAlpha();
                }
                if (screenAnimation) {
                    this.mShownAlpha *= screenRotationAnimation.getEnterTransformation().getAlpha();
                    return;
                }
                return;
            }
            return;
        }
        if (!this.mIsWallpaper || !this.mService.mInnerFields.mWallpaperActionPending) {
            boolean applyUniverseTransformation = (this.mAnimator.mUniverseBackground == null || this.mWin.mAttrs.type == 2025 || this.mWin.mBaseLayer >= this.mAnimator.mAboveUniverseLayer) ? false : true;
            MagnificationSpec spec2 = null;
            if (this.mService.mAccessibilityController != null && displayId == 0) {
                spec2 = this.mService.mAccessibilityController.getMagnificationSpecForWindowLocked(this.mWin);
            }
            if (applyUniverseTransformation || spec2 != null) {
                Rect frame2 = this.mWin.mFrame;
                float[] tmpFloats2 = this.mService.mTmpFloats;
                Matrix tmpMatrix2 = this.mWin.mTmpMatrix;
                tmpMatrix2.setScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
                tmpMatrix2.postTranslate(frame2.left + this.mWin.mXOffset, frame2.top + this.mWin.mYOffset);
                if (applyUniverseTransformation) {
                    tmpMatrix2.postConcat(this.mAnimator.mUniverseBackground.mUniverseTransform.getMatrix());
                }
                if (spec2 != null && !spec2.isNop()) {
                    tmpMatrix2.postScale(spec2.scale, spec2.scale);
                    tmpMatrix2.postTranslate(spec2.offsetX, spec2.offsetY);
                }
                tmpMatrix2.getValues(tmpFloats2);
                this.mHaveMatrix = true;
                this.mDsDx = tmpFloats2[0];
                this.mDtDx = tmpFloats2[3];
                this.mDsDy = tmpFloats2[1];
                this.mDtDy = tmpFloats2[4];
                float x2 = tmpFloats2[2];
                float y2 = tmpFloats2[5];
                this.mWin.mShownFrame.set(x2, y2, frame2.width() + x2, frame2.height() + y2);
                this.mShownAlpha = this.mAlpha;
                if (applyUniverseTransformation) {
                    this.mShownAlpha *= this.mAnimator.mUniverseBackground.mUniverseTransform.getAlpha();
                    return;
                }
                return;
            }
            this.mWin.mShownFrame.set(this.mWin.mFrame);
            if (this.mWin.mXOffset != 0 || this.mWin.mYOffset != 0) {
                this.mWin.mShownFrame.offset(this.mWin.mXOffset, this.mWin.mYOffset);
            }
            this.mShownAlpha = this.mAlpha;
            this.mHaveMatrix = false;
            this.mDsDx = this.mWin.mGlobalScale;
            this.mDtDx = 0.0f;
            this.mDsDy = 0.0f;
            this.mDtDy = this.mWin.mGlobalScale;
        }
    }

    void applyDecorRect(Rect decorRect) {
        WindowState w = this.mWin;
        int width = w.mFrame.width();
        int height = w.mFrame.height();
        int left = w.mXOffset + w.mFrame.left;
        int top = w.mYOffset + w.mFrame.top;
        w.mSystemDecorRect.set(0, 0, width, height);
        w.mSystemDecorRect.intersect(decorRect.left - left, decorRect.top - top, decorRect.right - left, decorRect.bottom - top);
        if (w.mEnforceSizeCompat && w.mInvGlobalScale != 1.0f) {
            float scale = w.mInvGlobalScale;
            w.mSystemDecorRect.left = (int) ((w.mSystemDecorRect.left * scale) - 0.5f);
            w.mSystemDecorRect.top = (int) ((w.mSystemDecorRect.top * scale) - 0.5f);
            w.mSystemDecorRect.right = (int) (((w.mSystemDecorRect.right + 1) * scale) - 0.5f);
            w.mSystemDecorRect.bottom = (int) (((w.mSystemDecorRect.bottom + 1) * scale) - 0.5f);
        }
    }

    void updateSurfaceWindowCrop(boolean recoveringMemory) {
        WindowState w = this.mWin;
        DisplayContent displayContent = w.getDisplayContent();
        if (displayContent != null) {
            if ((w.mAttrs.flags & 16384) != 0) {
                w.mSystemDecorRect.set(0, 0, w.mRequestedWidth, w.mRequestedHeight);
            } else if (!w.isDefaultDisplay()) {
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                w.mSystemDecorRect.set(0, 0, w.mCompatFrame.width(), w.mCompatFrame.height());
                w.mSystemDecorRect.intersect(-w.mCompatFrame.left, -w.mCompatFrame.top, displayInfo.logicalWidth - w.mCompatFrame.left, displayInfo.logicalHeight - w.mCompatFrame.top);
            } else if (w.mLayer >= this.mService.mSystemDecorLayer) {
                if (this.mAnimator.mUniverseBackground == null) {
                    w.mSystemDecorRect.set(0, 0, w.mCompatFrame.width(), w.mCompatFrame.height());
                } else {
                    applyDecorRect(this.mService.mScreenRect);
                }
            } else if (w.mAttrs.type == 2025 || w.mDecorFrame.isEmpty()) {
                w.mSystemDecorRect.set(0, 0, w.mCompatFrame.width(), w.mCompatFrame.height());
            } else if (w.mAttrs.type == 2013 && this.mAnimator.mAnimating) {
                this.mTmpClipRect.set(w.mSystemDecorRect);
                applyDecorRect(w.mDecorFrame);
                w.mSystemDecorRect.union(this.mTmpClipRect);
            } else {
                applyDecorRect(w.mDecorFrame);
            }
            Rect clipRect = this.mTmpClipRect;
            clipRect.set(w.mSystemDecorRect);
            WindowManager.LayoutParams attrs = w.mAttrs;
            clipRect.left -= attrs.surfaceInsets.left;
            clipRect.top -= attrs.surfaceInsets.top;
            clipRect.right += attrs.surfaceInsets.right;
            clipRect.bottom += attrs.surfaceInsets.bottom;
            if (this.mHasClipRect) {
                if ((w.mSystemUiVisibility & SYSTEM_UI_FLAGS_LAYOUT_STABLE_FULLSCREEN) == SYSTEM_UI_FLAGS_LAYOUT_STABLE_FULLSCREEN || (w.mAttrs.flags & SoundTriggerHelper.STATUS_ERROR) != 0) {
                    clipRect.intersect(this.mClipRect);
                } else {
                    int offsetTop = Math.max(clipRect.top, w.mContentInsets.top);
                    clipRect.offset(0, -offsetTop);
                    clipRect.intersect(this.mClipRect);
                    clipRect.offset(0, offsetTop);
                }
            }
            clipRect.offset(attrs.surfaceInsets.left, attrs.surfaceInsets.top);
            if (!clipRect.equals(this.mLastClipRect)) {
                this.mLastClipRect.set(clipRect);
                try {
                    this.mSurfaceControl.setWindowCrop(clipRect);
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Error setting crop surface of " + w + " crop=" + clipRect.toShortString(), e);
                    if (!recoveringMemory) {
                        this.mService.reclaimSomeSurfaceMemoryLocked(this, "crop", true);
                    }
                }
            }
        }
    }

    void setSurfaceBoundariesLocked(boolean recoveringMemory) {
        int width;
        int height;
        TaskStack stack;
        WindowState w = this.mWin;
        if ((w.mAttrs.flags & 16384) != 0) {
            width = w.mRequestedWidth;
            height = w.mRequestedHeight;
        } else {
            width = w.mCompatFrame.width();
            height = w.mCompatFrame.height();
        }
        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }
        float left = w.mShownFrame.left;
        float top = w.mShownFrame.top;
        WindowManager.LayoutParams attrs = w.getAttrs();
        int width2 = width + attrs.surfaceInsets.left + attrs.surfaceInsets.right;
        int height2 = height + attrs.surfaceInsets.top + attrs.surfaceInsets.bottom;
        float left2 = left - attrs.surfaceInsets.left;
        float top2 = top - attrs.surfaceInsets.top;
        boolean surfaceMoved = (this.mSurfaceX == left2 && this.mSurfaceY == top2) ? false : true;
        if (surfaceMoved) {
            this.mSurfaceX = left2;
            this.mSurfaceY = top2;
            try {
                this.mSurfaceControl.setPosition(left2, top2);
            } catch (RuntimeException e) {
                Slog.w(TAG, "Error positioning surface of " + w + " pos=(" + left2 + "," + top2 + ")", e);
                if (!recoveringMemory) {
                    this.mService.reclaimSomeSurfaceMemoryLocked(this, "position", true);
                }
            }
        }
        boolean surfaceResized = (this.mSurfaceW == ((float) width2) && this.mSurfaceH == ((float) height2)) ? false : true;
        if (surfaceResized) {
            this.mSurfaceW = width2;
            this.mSurfaceH = height2;
            this.mSurfaceResized = true;
            try {
                this.mSurfaceControl.setSize(width2, height2);
                this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 4);
                if ((w.mAttrs.flags & 2) != 0 && (stack = w.getStack()) != null) {
                    stack.startDimmingIfNeeded(this);
                }
            } catch (RuntimeException e2) {
                Slog.e(TAG, "Error resizing surface of " + w + " size=(" + width2 + "x" + height2 + ")", e2);
                if (!recoveringMemory) {
                    this.mService.reclaimSomeSurfaceMemoryLocked(this, "size", true);
                }
            }
        }
        updateSurfaceWindowCrop(recoveringMemory);
    }

    public void prepareSurfaceLocked(boolean recoveringMemory) {
        WindowState w = this.mWin;
        if (this.mSurfaceControl == null) {
            if (w.mOrientationChanging) {
                w.mOrientationChanging = false;
                return;
            }
            return;
        }
        boolean displayed = false;
        computeShownFrameLocked();
        setSurfaceBoundariesLocked(recoveringMemory);
        if (this.mIsWallpaper && !this.mWin.mWallpaperVisible) {
            hide();
        } else if (w.mAttachedHidden || !w.isOnScreen()) {
            hide();
            this.mAnimator.hideWallpapersLocked(w);
            if (w.mOrientationChanging) {
                w.mOrientationChanging = false;
            }
        } else if (this.mLastLayer != this.mAnimLayer || this.mLastAlpha != this.mShownAlpha || this.mLastDsDx != this.mDsDx || this.mLastDtDx != this.mDtDx || this.mLastDsDy != this.mDsDy || this.mLastDtDy != this.mDtDy || w.mLastHScale != w.mHScale || w.mLastVScale != w.mVScale || this.mLastHidden) {
            displayed = true;
            this.mLastAlpha = this.mShownAlpha;
            this.mLastLayer = this.mAnimLayer;
            this.mLastDsDx = this.mDsDx;
            this.mLastDtDx = this.mDtDx;
            this.mLastDsDy = this.mDsDy;
            this.mLastDtDy = this.mDtDy;
            w.mLastHScale = w.mHScale;
            w.mLastVScale = w.mVScale;
            if (this.mSurfaceControl != null) {
                try {
                    this.mSurfaceAlpha = this.mShownAlpha;
                    this.mSurfaceControl.setAlpha(this.mShownAlpha);
                    this.mSurfaceLayer = this.mAnimLayer;
                    this.mSurfaceControl.setLayer(this.mAnimLayer);
                    this.mSurfaceControl.setMatrix(this.mDsDx * w.mHScale, this.mDtDx * w.mVScale, this.mDsDy * w.mHScale, this.mDtDy * w.mVScale);
                    if (this.mLastHidden && this.mDrawState == 4) {
                        if (showSurfaceRobustlyLocked()) {
                            this.mLastHidden = false;
                            if (this.mIsWallpaper) {
                                this.mService.dispatchWallpaperVisibility(w, true);
                            }
                            this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 8);
                        } else {
                            w.mOrientationChanging = false;
                        }
                    }
                    if (this.mSurfaceControl != null) {
                        w.mToken.hasVisible = true;
                    }
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Error updating surface in " + w, e);
                    if (!recoveringMemory) {
                        this.mService.reclaimSomeSurfaceMemoryLocked(this, "update", true);
                    }
                }
            }
        } else {
            displayed = true;
        }
        if (displayed) {
            if (w.mOrientationChanging) {
                if (!w.isDrawnLw()) {
                    this.mAnimator.mBulkUpdateParams &= -9;
                    this.mAnimator.mLastWindowFreezeSource = w;
                } else {
                    w.mOrientationChanging = false;
                }
            }
            w.mToken.hasVisible = true;
        }
    }

    void setTransparentRegionHintLocked(Region region) {
        if (this.mSurfaceControl == null) {
            Slog.w(TAG, "setTransparentRegionHint: null mSurface after mHasSurface true");
            return;
        }
        SurfaceControl.openTransaction();
        try {
            this.mSurfaceControl.setTransparentRegionHint(region);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    void setWallpaperOffset(RectF shownFrame) {
        WindowManager.LayoutParams attrs = this.mWin.getAttrs();
        int left = ((int) shownFrame.left) - attrs.surfaceInsets.left;
        int top = ((int) shownFrame.top) - attrs.surfaceInsets.top;
        if (this.mSurfaceX != left || this.mSurfaceY != top) {
            this.mSurfaceX = left;
            this.mSurfaceY = top;
            if (!this.mAnimating) {
                SurfaceControl.openTransaction();
                try {
                    this.mSurfaceControl.setPosition(this.mWin.mFrame.left + left, this.mWin.mFrame.top + top);
                    updateSurfaceWindowCrop(false);
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Error positioning surface of " + this.mWin + " pos=(" + left + "," + top + ")", e);
                } finally {
                    SurfaceControl.closeTransaction();
                }
            }
        }
    }

    void setOpaqueLocked(boolean isOpaque) {
        if (this.mSurfaceControl != null) {
            SurfaceControl.openTransaction();
            try {
                this.mSurfaceControl.setOpaque(isOpaque);
            } finally {
                SurfaceControl.closeTransaction();
            }
        }
    }

    boolean performShowLocked() {
        if (this.mWin.isHiddenFromUserLocked() || this.mDrawState != 3 || !this.mWin.isReadyForDisplayIgnoringKeyguard()) {
            return false;
        }
        this.mService.enableScreenIfNeededLocked();
        applyEnterAnimationLocked();
        this.mLastAlpha = -1.0f;
        this.mDrawState = 4;
        this.mService.scheduleAnimationLocked();
        int i = this.mWin.mChildWindows.size();
        while (i > 0) {
            i--;
            WindowState c = this.mWin.mChildWindows.get(i);
            if (c.mAttachedHidden) {
                c.mAttachedHidden = false;
                if (c.mWinAnimator.mSurfaceControl != null) {
                    c.mWinAnimator.performShowLocked();
                    DisplayContent displayContent = c.getDisplayContent();
                    if (displayContent != null) {
                        displayContent.layoutNeeded = true;
                    }
                }
            }
        }
        if (this.mWin.mAttrs.type != 3 && this.mWin.mAppToken != null) {
            this.mWin.mAppToken.firstWindowDrawn = true;
            if (this.mWin.mAppToken.startingData != null) {
                clearAnimation();
                this.mService.mFinishedStarting.add(this.mWin.mAppToken);
                this.mService.mH.sendEmptyMessage(7);
            }
            this.mWin.mAppToken.updateReportedVisibilityLocked();
        }
        return true;
    }

    boolean showSurfaceRobustlyLocked() {
        try {
            if (this.mSurfaceControl == null) {
                return true;
            }
            this.mSurfaceShown = true;
            this.mSurfaceControl.show();
            this.mService.updateNonSystemOverlayWindowsVisibilityIfNeeded(this.mWin, true);
            if (!this.mWin.mTurnOnScreen) {
                return true;
            }
            this.mWin.mTurnOnScreen = false;
            this.mAnimator.mBulkUpdateParams |= 16;
            return true;
        } catch (RuntimeException e) {
            Slog.w(TAG, "Failure showing surface " + this.mSurfaceControl + " in " + this.mWin, e);
            this.mService.reclaimSomeSurfaceMemoryLocked(this, "show", true);
            return false;
        }
    }

    void applyEnterAnimationLocked() {
        int transit;
        if (this.mEnterAnimationPending) {
            this.mEnterAnimationPending = false;
            transit = 1;
        } else {
            transit = 3;
        }
        applyAnimationLocked(transit, true);
        if (this.mService.mAccessibilityController != null && this.mWin.getDisplayId() == 0) {
            this.mService.mAccessibilityController.onWindowTransitionLocked(this.mWin, transit);
        }
    }

    boolean applyAnimationLocked(int transit, boolean isEntrance) {
        if ((this.mLocalAnimating && this.mAnimationIsEntrance == isEntrance) || this.mKeyguardGoingAwayAnimation) {
            if (this.mAnimation == null || !this.mKeyguardGoingAwayAnimation || transit != 5) {
                return true;
            }
            applyFadeoutDuringKeyguardExitAnimation();
            return true;
        }
        if (this.mService.okToDisplay()) {
            int anim = this.mPolicy.selectAnimationLw(this.mWin, transit);
            int attr = -1;
            Animation a = null;
            if (anim != 0) {
                a = anim != -1 ? AnimationUtils.loadAnimation(this.mContext, anim) : null;
            } else {
                switch (transit) {
                    case 1:
                        attr = 0;
                        break;
                    case 2:
                        attr = 1;
                        break;
                    case 3:
                        attr = 2;
                        break;
                    case 4:
                        attr = 3;
                        break;
                }
                if (attr >= 0) {
                    a = this.mService.mAppTransition.loadAnimationAttr(this.mWin.mAttrs, attr);
                }
            }
            if (a != null) {
                setAnimation(a);
                this.mAnimationIsEntrance = isEntrance;
            }
        } else {
            clearAnimation();
        }
        return this.mAnimation != null;
    }

    private void applyFadeoutDuringKeyguardExitAnimation() {
        long startTime = this.mAnimation.getStartTime();
        long duration = this.mAnimation.getDuration();
        long elapsed = this.mLastAnimationTime - startTime;
        long fadeDuration = duration - elapsed;
        if (fadeDuration > 0) {
            AnimationSet newAnimation = new AnimationSet(false);
            newAnimation.setDuration(duration);
            newAnimation.setStartTime(startTime);
            newAnimation.addAnimation(this.mAnimation);
            Animation fadeOut = AnimationUtils.loadAnimation(this.mContext, R.anim.activity_translucent_close_exit);
            fadeOut.setDuration(fadeDuration);
            fadeOut.setStartOffset(elapsed);
            newAnimation.addAnimation(fadeOut);
            newAnimation.initialize(this.mWin.mFrame.width(), this.mWin.mFrame.height(), this.mAnimDw, this.mAnimDh);
            this.mAnimation = newAnimation;
        }
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (this.mAnimating || this.mLocalAnimating || this.mAnimationIsEntrance || this.mAnimation != null) {
            pw.print(prefix);
            pw.print("mAnimating=");
            pw.print(this.mAnimating);
            pw.print(" mLocalAnimating=");
            pw.print(this.mLocalAnimating);
            pw.print(" mAnimationIsEntrance=");
            pw.print(this.mAnimationIsEntrance);
            pw.print(" mAnimation=");
            pw.println(this.mAnimation);
        }
        if (this.mHasTransformation || this.mHasLocalTransformation) {
            pw.print(prefix);
            pw.print("XForm: has=");
            pw.print(this.mHasTransformation);
            pw.print(" hasLocal=");
            pw.print(this.mHasLocalTransformation);
            pw.print(" ");
            this.mTransformation.printShortString(pw);
            pw.println();
        }
        if (this.mSurfaceControl != null) {
            if (dumpAll) {
                pw.print(prefix);
                pw.print("mSurface=");
                pw.println(this.mSurfaceControl);
                pw.print(prefix);
                pw.print("mDrawState=");
                pw.print(drawStateToString());
                pw.print(" mLastHidden=");
                pw.println(this.mLastHidden);
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
        if (this.mPendingDestroySurface != null) {
            pw.print(prefix);
            pw.print("mPendingDestroySurface=");
            pw.println(this.mPendingDestroySurface);
        }
        if (this.mSurfaceResized || this.mSurfaceDestroyDeferred) {
            pw.print(prefix);
            pw.print("mSurfaceResized=");
            pw.print(this.mSurfaceResized);
            pw.print(" mSurfaceDestroyDeferred=");
            pw.println(this.mSurfaceDestroyDeferred);
        }
        if (this.mWin.mAttrs.type == 2025) {
            pw.print(prefix);
            pw.print("mUniverseTransform=");
            this.mUniverseTransform.printShortString(pw);
            pw.println();
        }
        if (this.mShownAlpha != 1.0f || this.mAlpha != 1.0f || this.mLastAlpha != 1.0f) {
            pw.print(prefix);
            pw.print("mShownAlpha=");
            pw.print(this.mShownAlpha);
            pw.print(" mAlpha=");
            pw.print(this.mAlpha);
            pw.print(" mLastAlpha=");
            pw.println(this.mLastAlpha);
        }
        if (this.mHaveMatrix || this.mWin.mGlobalScale != 1.0f) {
            pw.print(prefix);
            pw.print("mGlobalScale=");
            pw.print(this.mWin.mGlobalScale);
            pw.print(" mDsDx=");
            pw.print(this.mDsDx);
            pw.print(" mDtDx=");
            pw.print(this.mDtDx);
            pw.print(" mDsDy=");
            pw.print(this.mDsDy);
            pw.print(" mDtDy=");
            pw.println(this.mDtDy);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("WindowStateAnimator{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.mWin.mAttrs.getTitle());
        sb.append('}');
        return sb.toString();
    }
}
