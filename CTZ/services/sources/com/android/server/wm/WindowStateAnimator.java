package com.android.server.wm;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Debug;
import android.os.Trace;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy;
import com.mediatek.server.wm.WindowManagerDebugger;
import java.io.PrintWriter;

class WindowStateAnimator {
    static final int COMMIT_DRAW_PENDING = 2;
    static final int DRAW_PENDING = 1;
    static final int HAS_DRAWN = 4;
    static final int NO_SURFACE = 0;
    static final int READY_TO_SHOW = 3;
    static final int STACK_CLIP_AFTER_ANIM = 0;
    static final int STACK_CLIP_BEFORE_ANIM = 1;
    static final int STACK_CLIP_NONE = 2;
    static final String TAG = "WindowManager";
    static final int WINDOW_FREEZE_LAYER = 2000000;
    int mAnimLayer;
    boolean mAnimationIsEntrance;
    private boolean mAnimationStartDelayed;
    final WindowAnimator mAnimator;
    int mAttrType;
    final Context mContext;
    private boolean mDestroyPreservedSurfaceUponRedraw;
    int mDrawState;
    boolean mEnterAnimationPending;
    boolean mEnteringAnimation;
    boolean mForceScaleUntilResize;
    boolean mHaveMatrix;
    final boolean mIsWallpaper;
    boolean mLastHidden;
    int mLastLayer;
    private boolean mOffsetPositionForStackResize;
    private WindowSurfaceController mPendingDestroySurface;
    final WindowManagerPolicy mPolicy;
    boolean mReportSurfaceResized;
    final WindowManagerService mService;
    final Session mSession;
    WindowSurfaceController mSurfaceController;
    boolean mSurfaceDestroyDeferred;
    int mSurfaceFormat;
    boolean mSurfaceResized;
    private final WallpaperController mWallpaperControllerLocked;
    final WindowState mWin;
    float mShownAlpha = 0.0f;
    float mAlpha = 0.0f;
    float mLastAlpha = 0.0f;
    Rect mTmpClipRect = new Rect();
    Rect mTmpFinalClipRect = new Rect();
    Rect mLastClipRect = new Rect();
    Rect mLastFinalClipRect = new Rect();
    Rect mTmpStackBounds = new Rect();
    private Rect mTmpAnimatingBounds = new Rect();
    private Rect mTmpSourceBounds = new Rect();
    private final Rect mSystemDecorRect = new Rect();
    float mDsDx = 1.0f;
    float mDtDx = 0.0f;
    float mDsDy = 0.0f;
    float mDtDy = 1.0f;
    private float mLastDsDx = 1.0f;
    private float mLastDtDx = 0.0f;
    private float mLastDsDy = 0.0f;
    private float mLastDtDy = 1.0f;
    private final SurfaceControl.Transaction mTmpTransaction = new SurfaceControl.Transaction();
    float mExtraHScale = 1.0f;
    float mExtraVScale = 1.0f;
    int mXOffset = 0;
    int mYOffset = 0;
    private final Rect mTmpSize = new Rect();
    private final SurfaceControl.Transaction mReparentTransaction = new SurfaceControl.Transaction();
    boolean mChildrenDetached = false;
    boolean mPipAnimationStarted = false;
    private final Point mTmpPos = new Point();

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

    WindowStateAnimator(WindowState windowState) {
        WindowManagerService windowManagerService = windowState.mService;
        this.mService = windowManagerService;
        this.mAnimator = windowManagerService.mAnimator;
        this.mPolicy = windowManagerService.mPolicy;
        this.mContext = windowManagerService.mContext;
        this.mWin = windowState;
        this.mSession = windowState.mSession;
        this.mAttrType = windowState.mAttrs.type;
        this.mIsWallpaper = windowState.mIsWallpaper;
        this.mWallpaperControllerLocked = this.mService.mRoot.mWallpaperController;
    }

    boolean isAnimationSet() {
        return this.mWin.isAnimating();
    }

    void cancelExitAnimationForNextAnimationLocked() {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.d("WindowManager", "cancelExitAnimationForNextAnimationLocked: " + this.mWin);
        }
        this.mWin.cancelAnimation();
        this.mWin.destroySurfaceUnchecked();
    }

    void onAnimationFinished() {
        Trace.traceBegin(32L, "win animation done");
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            StringBuilder sb = new StringBuilder();
            sb.append("Animation done in ");
            sb.append(this);
            sb.append(": exiting=");
            sb.append(this.mWin.mAnimatingExit);
            sb.append(", reportedVisible=");
            sb.append(this.mWin.mAppToken != null ? this.mWin.mAppToken.reportedVisible : false);
            Slog.v("WindowManager", sb.toString());
        }
        Trace.traceEnd(32L);
        if (this.mAnimator.mWindowDetachedWallpaper == this.mWin) {
            this.mAnimator.mWindowDetachedWallpaper = null;
        }
        this.mWin.checkPolicyVisibilityChange();
        DisplayContent displayContent = this.mWin.getDisplayContent();
        if (this.mAttrType == 2000 && this.mWin.mPolicyVisibility && displayContent != null) {
            displayContent.setLayoutNeeded();
        }
        this.mWin.onExitAnimationDone();
        int displayId = this.mWin.getDisplayId();
        int i = 8;
        if (displayContent.mWallpaperController.isWallpaperTarget(this.mWin)) {
            i = 12;
        }
        this.mAnimator.setPendingLayoutChanges(displayId, i);
        if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
            this.mService.mWindowPlacerLocked.debugLayoutRepeats("WindowStateAnimator", this.mAnimator.getPendingLayoutChanges(displayId));
        }
        if (this.mWin.mAppToken != null) {
            this.mWin.mAppToken.updateReportedVisibilityLocked();
        }
    }

    void hide(SurfaceControl.Transaction transaction, String str) {
        if (!this.mLastHidden) {
            this.mLastHidden = true;
            markPreservedSurfaceForDestroy();
            if (this.mSurfaceController != null) {
                this.mSurfaceController.hide(transaction, str);
            }
        }
    }

    void hide(String str) {
        hide(this.mTmpTransaction, str);
        SurfaceControl.mergeToGlobalTransaction(this.mTmpTransaction);
    }

    boolean finishDrawingLocked() {
        boolean z = this.mWin.mAttrs.type == 3;
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && z) {
            Slog.v("WindowManager", "Finishing drawing window " + this.mWin + ": mDrawState=" + drawStateToString());
        }
        if (this.mDrawState != 1) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_ANIM || WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.v("WindowManager", "finishDrawingLocked: mDrawState=COMMIT_DRAW_PENDING " + this.mWin + " in " + this.mSurfaceController);
        } else {
            WindowManagerDebugger windowManagerDebugger = this.mService.mWindowManagerDebugger;
            if (WindowManagerDebugger.WMS_DEBUG_ENG) {
            }
        }
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && z) {
            Slog.v("WindowManager", "Draw state now committed in " + this.mWin);
        }
        this.mDrawState = 2;
        return true;
    }

    boolean commitFinishDrawingLocked() {
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && this.mWin.mAttrs.type == 3) {
            Slog.i("WindowManager", "commitFinishDrawingLocked: " + this.mWin + " cur mDrawState=" + drawStateToString());
        }
        if (this.mDrawState != 2 && this.mDrawState != 3) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.i("WindowManager", "commitFinishDrawingLocked: mDrawState=READY_TO_SHOW " + this.mSurfaceController);
        }
        this.mDrawState = 3;
        AppWindowToken appWindowToken = this.mWin.mAppToken;
        if (appWindowToken == null || appWindowToken.allDrawn || this.mWin.mAttrs.type == 3) {
            return this.mWin.performShowLocked();
        }
        return false;
    }

    void preserveSurfaceLocked() {
        if (this.mDestroyPreservedSurfaceUponRedraw) {
            this.mSurfaceDestroyDeferred = false;
            destroySurfaceLocked();
            this.mSurfaceDestroyDeferred = true;
            return;
        }
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            WindowManagerService.logSurface(this.mWin, "SET FREEZE LAYER", false);
        }
        if (this.mSurfaceController != null) {
            this.mSurfaceController.mSurfaceControl.setLayer(1);
        }
        this.mDestroyPreservedSurfaceUponRedraw = true;
        this.mSurfaceDestroyDeferred = true;
        destroySurfaceLocked();
    }

    void destroyPreservedSurfaceLocked() {
        if (!this.mDestroyPreservedSurfaceUponRedraw) {
            return;
        }
        if (this.mSurfaceController != null && this.mPendingDestroySurface != null && (this.mWin.mAppToken == null || !this.mWin.mAppToken.isRelaunching())) {
            this.mReparentTransaction.reparentChildren(this.mPendingDestroySurface.mSurfaceControl, this.mSurfaceController.mSurfaceControl.getHandle()).apply();
        }
        destroyDeferredSurfaceLocked();
        this.mDestroyPreservedSurfaceUponRedraw = false;
    }

    void markPreservedSurfaceForDestroy() {
        if (this.mDestroyPreservedSurfaceUponRedraw && !this.mService.mDestroyPreservedSurface.contains(this.mWin)) {
            this.mService.mDestroyPreservedSurface.add(this.mWin);
        }
    }

    private int getLayerStack() {
        return this.mWin.getDisplayContent().getDisplay().getLayerStack();
    }

    void resetDrawState() {
        this.mDrawState = 1;
        if (this.mWin.mAppToken == null) {
            return;
        }
        if (this.mWin.mAppToken.isSelfAnimating()) {
            this.mWin.mAppToken.deferClearAllDrawn = true;
        } else {
            this.mWin.mAppToken.clearAllDrawn();
        }
    }

    WindowSurfaceController createSurfaceLocked(int i, int i2) {
        boolean z;
        int i3;
        int i4;
        WindowState windowState = this.mWin;
        if (this.mSurfaceController != null) {
            return this.mSurfaceController;
        }
        this.mChildrenDetached = false;
        int i5 = (this.mWin.mAttrs.privateFlags & DumpState.DUMP_DEXOPT) != 0 ? 441731 : i;
        windowState.setHasSurface(false);
        if (WindowManagerDebugConfig.DEBUG_ANIM || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.i("WindowManager", "createSurface " + this + ": mDrawState=DRAW_PENDING");
        }
        resetDrawState();
        this.mService.makeWindowFreezingScreenIfNeededLocked(windowState);
        int i6 = 4;
        WindowManager.LayoutParams layoutParams = windowState.mAttrs;
        if (this.mService.isSecureLocked(windowState)) {
            i6 = 132;
        }
        this.mTmpSize.set(0, 0, 0, 0);
        calculateSurfaceBounds(windowState, layoutParams);
        int iWidth = this.mTmpSize.width();
        int iHeight = this.mTmpSize.height();
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Creating surface in session " + this.mSession.mSurfaceSession + " window " + this + " w=" + iWidth + " h=" + iHeight + " x=" + this.mTmpSize.left + " y=" + this.mTmpSize.top + " format=" + layoutParams.format + " flags=" + i6);
        }
        this.mLastClipRect.set(0, 0, 0, 0);
        try {
            try {
                i3 = (layoutParams.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0 ? -3 : layoutParams.format;
                if (!PixelFormat.formatHasAlpha(layoutParams.format) && layoutParams.surfaceInsets.left == 0 && layoutParams.surfaceInsets.top == 0 && layoutParams.surfaceInsets.right == 0 && layoutParams.surfaceInsets.bottom == 0 && !windowState.isDragResizing()) {
                    i6 |= 1024;
                }
                i4 = i6;
                z = true;
            } catch (Surface.OutOfResourcesException e) {
                z = true;
            }
            try {
                this.mSurfaceController = new WindowSurfaceController(this.mSession.mSurfaceSession, layoutParams.getTitle().toString(), iWidth, iHeight, i3, i4, this, i5, i2);
                setOffsetPositionForStackResize(false);
                this.mSurfaceFormat = i3;
                windowState.setHasSurface(true);
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                    Slog.i("WindowManager", "  CREATE SURFACE " + this.mSurfaceController + " IN SESSION " + this.mSession.mSurfaceSession + ": pid=" + this.mSession.mPid + " format=" + layoutParams.format + " flags=0x" + Integer.toHexString(i4) + " / " + this);
                } else {
                    WindowManagerDebugger windowManagerDebugger = this.mService.mWindowManagerDebugger;
                    if (WindowManagerDebugger.WMS_DEBUG_ENG) {
                    }
                }
                if (WindowManagerService.localLOGV) {
                    Slog.v("WindowManager", "Got surface: " + this.mSurfaceController + ", set left=" + windowState.mFrame.left + " top=" + windowState.mFrame.top + ", animLayer=" + this.mAnimLayer);
                }
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", ">>> OPEN TRANSACTION createSurfaceLocked");
                    WindowManagerService.logSurface(windowState, "CREATE pos=(" + windowState.mFrame.left + "," + windowState.mFrame.top + ") (" + iWidth + "x" + iHeight + "), layer=" + this.mAnimLayer + " HIDE", false);
                }
                this.mLastHidden = true;
                if (WindowManagerService.localLOGV) {
                    Slog.v("WindowManager", "Created surface " + this);
                }
                return this.mSurfaceController;
            } catch (Surface.OutOfResourcesException e2) {
                Slog.w("WindowManager", "OutOfResourcesException creating surface");
                this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
                this.mDrawState = 0;
                return null;
            }
        } catch (Exception e3) {
            Slog.e("WindowManager", "Exception creating surface (parent dead?)", e3);
            this.mDrawState = 0;
            return null;
        }
    }

    private void calculateSurfaceBounds(WindowState windowState, WindowManager.LayoutParams layoutParams) {
        if ((layoutParams.flags & 16384) != 0) {
            this.mTmpSize.right = this.mTmpSize.left + windowState.mRequestedWidth;
            this.mTmpSize.bottom = this.mTmpSize.top + windowState.mRequestedHeight;
        } else if (windowState.isDragResizing()) {
            if (windowState.getResizeMode() == 0) {
                this.mTmpSize.left = 0;
                this.mTmpSize.top = 0;
            }
            DisplayInfo displayInfo = windowState.getDisplayInfo();
            this.mTmpSize.right = this.mTmpSize.left + displayInfo.logicalWidth;
            this.mTmpSize.bottom = this.mTmpSize.top + displayInfo.logicalHeight;
        } else {
            this.mTmpSize.right = this.mTmpSize.left + windowState.mCompatFrame.width();
            this.mTmpSize.bottom = this.mTmpSize.top + windowState.mCompatFrame.height();
        }
        if (this.mTmpSize.width() < 1) {
            this.mTmpSize.right = this.mTmpSize.left + 1;
        }
        if (this.mTmpSize.height() < 1) {
            this.mTmpSize.bottom = this.mTmpSize.top + 1;
        }
        this.mTmpSize.left -= layoutParams.surfaceInsets.left;
        this.mTmpSize.top -= layoutParams.surfaceInsets.top;
        this.mTmpSize.right += layoutParams.surfaceInsets.right;
        this.mTmpSize.bottom += layoutParams.surfaceInsets.bottom;
    }

    boolean hasSurface() {
        return this.mSurfaceController != null && this.mSurfaceController.hasSurface();
    }

    void destroySurfaceLocked() {
        AppWindowToken appWindowToken = this.mWin.mAppToken;
        if (appWindowToken != null && this.mWin == appWindowToken.startingWindow) {
            appWindowToken.startingDisplayed = false;
        }
        if (this.mSurfaceController == null) {
            return;
        }
        if (!this.mDestroyPreservedSurfaceUponRedraw) {
            this.mWin.mHidden = true;
        }
        try {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                WindowManagerService.logWithStack("WindowManager", "Window " + this + " destroying surface " + this.mSurfaceController + ", session " + this.mSession);
            }
            if (this.mSurfaceDestroyDeferred) {
                if (this.mSurfaceController != null && this.mPendingDestroySurface != this.mSurfaceController) {
                    if (this.mPendingDestroySurface != null) {
                        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                            WindowManagerService.logSurface(this.mWin, "DESTROY PENDING", true);
                        }
                        this.mPendingDestroySurface.destroyNotInTransaction();
                    }
                    this.mPendingDestroySurface = this.mSurfaceController;
                }
            } else {
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                    WindowManagerService.logSurface(this.mWin, "DESTROY", true);
                }
                destroySurface();
            }
            if (!this.mDestroyPreservedSurfaceUponRedraw) {
                this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
            }
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Exception thrown when destroying Window " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e.toString());
        }
        this.mWin.setHasSurface(false);
        if (this.mSurfaceController != null) {
            this.mSurfaceController.setShown(false);
        }
        this.mSurfaceController = null;
        this.mDrawState = 0;
    }

    void destroyDeferredSurfaceLocked() {
        try {
            if (this.mPendingDestroySurface != null) {
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                    WindowManagerService.logSurface(this.mWin, "DESTROY PENDING", true);
                }
                this.mPendingDestroySurface.destroyNotInTransaction();
                if (!this.mDestroyPreservedSurfaceUponRedraw) {
                    this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
                }
            }
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Exception thrown when destroying Window " + this + " surface " + this.mPendingDestroySurface + " session " + this.mSession + ": " + e.toString());
        }
        this.mSurfaceDestroyDeferred = false;
        this.mPendingDestroySurface = null;
    }

    void computeShownFrameLocked() {
        ScreenRotationAnimation screenRotationAnimationLocked = this.mAnimator.getScreenRotationAnimationLocked(this.mWin.getDisplayId());
        boolean z = screenRotationAnimationLocked != null && screenRotationAnimationLocked.isAnimating();
        if (z) {
            Rect rect = this.mWin.mFrame;
            float[] fArr = this.mService.mTmpFloats;
            Matrix matrix = this.mWin.mTmpMatrix;
            if (screenRotationAnimationLocked.isRotating()) {
                float fWidth = rect.width();
                float fHeight = rect.height();
                if (fWidth >= 1.0f && fHeight >= 1.0f) {
                    matrix.setScale((2.0f / fWidth) + 1.0f, 1.0f + (2.0f / fHeight), fWidth / 2.0f, fHeight / 2.0f);
                } else {
                    matrix.reset();
                }
            } else {
                matrix.reset();
            }
            matrix.postScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
            matrix.postTranslate(this.mWin.mAttrs.surfaceInsets.left, this.mWin.mAttrs.surfaceInsets.top);
            this.mHaveMatrix = true;
            matrix.getValues(fArr);
            this.mDsDx = fArr[0];
            this.mDtDx = fArr[3];
            this.mDtDy = fArr[1];
            this.mDsDy = fArr[4];
            this.mShownAlpha = this.mAlpha;
            if ((!this.mService.mLimitedAlphaCompositing || !PixelFormat.formatHasAlpha(this.mWin.mAttrs.format) || this.mWin.isIdentityMatrix(this.mDsDx, this.mDtDx, this.mDtDy, this.mDsDy)) && z) {
                this.mShownAlpha *= screenRotationAnimationLocked.getEnterTransformation().getAlpha();
            }
            if (WindowManagerDebugConfig.DEBUG_ANIM || WindowManagerService.localLOGV) {
                if (this.mShownAlpha == 1.0d || this.mShownAlpha == 0.0d) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("computeShownFrameLocked: Animating ");
                    sb.append(this);
                    sb.append(" mAlpha=");
                    sb.append(this.mAlpha);
                    sb.append(" screen=");
                    sb.append(z ? Float.valueOf(screenRotationAnimationLocked.getEnterTransformation().getAlpha()) : "null");
                    Slog.v("WindowManager", sb.toString());
                    return;
                }
                return;
            }
            return;
        }
        if ((this.mIsWallpaper && this.mService.mRoot.mWallpaperActionPending) || this.mWin.isDragResizeChanged()) {
            return;
        }
        if (WindowManagerService.localLOGV) {
            Slog.v("WindowManager", "computeShownFrameLocked: " + this + " not attached, mAlpha=" + this.mAlpha);
        }
        this.mShownAlpha = this.mAlpha;
        this.mHaveMatrix = false;
        this.mDsDx = this.mWin.mGlobalScale;
        this.mDtDx = 0.0f;
        this.mDtDy = 0.0f;
        this.mDsDy = this.mWin.mGlobalScale;
    }

    private boolean calculateCrop(Rect rect) {
        WindowState windowState = this.mWin;
        DisplayContent displayContent = windowState.getDisplayContent();
        rect.setEmpty();
        boolean z = false;
        if (displayContent == null || windowState.inPinnedWindowingMode() || windowState.mAttrs.type == 2013) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d("WindowManager", "Updating crop win=" + windowState + " mLastCrop=" + this.mLastClipRect);
        }
        windowState.calculatePolicyCrop(this.mSystemDecorRect);
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d("WindowManager", "Applying decor to crop win=" + windowState + " mDecorFrame=" + windowState.mDecorFrame + " mSystemDecorRect=" + this.mSystemDecorRect);
        }
        Task task = windowState.getTask();
        if (windowState.fillsDisplay() || (task != null && task.isFullscreen())) {
            z = true;
        }
        if (windowState.isDragResizing()) {
            windowState.getResizeMode();
        }
        rect.set(this.mSystemDecorRect);
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d("WindowManager", "win=" + windowState + " Initial clip rect: " + rect + " fullscreen=" + z);
        }
        windowState.expandForSurfaceInsets(rect);
        rect.offset(windowState.mAttrs.surfaceInsets.left, windowState.mAttrs.surfaceInsets.top);
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d("WindowManager", "win=" + windowState + " Clip rect after stack adjustment=" + rect);
        }
        windowState.transformClipRectFromScreenToSurfaceSpace(rect);
        return true;
    }

    private void applyCrop(Rect rect, boolean z) {
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d("WindowManager", "applyCrop: win=" + this.mWin + " clipRect=" + rect);
        }
        if (rect != null) {
            if (!rect.equals(this.mLastClipRect)) {
                this.mLastClipRect.set(rect);
                this.mSurfaceController.setCropInTransaction(rect, z);
                return;
            }
            return;
        }
        this.mSurfaceController.clearCropInTransaction(z);
    }

    void setSurfaceBoundariesLocked(boolean z) {
        Rect rect;
        boolean z2;
        int i;
        int i2;
        float fHeight;
        if (this.mSurfaceController == null) {
            return;
        }
        WindowState windowState = this.mWin;
        WindowManager.LayoutParams attrs = this.mWin.getAttrs();
        Task task = windowState.getTask();
        this.mTmpSize.set(0, 0, 0, 0);
        calculateSurfaceBounds(windowState, attrs);
        this.mExtraHScale = 1.0f;
        this.mExtraVScale = 1.0f;
        boolean z3 = this.mForceScaleUntilResize;
        boolean z4 = windowState.mSeamlesslyRotated;
        boolean z5 = !windowState.mRelayoutCalled || windowState.mInRelayout;
        if (z5) {
            this.mSurfaceResized = this.mSurfaceController.setSizeInTransaction(this.mTmpSize.width(), this.mTmpSize.height(), z);
        } else {
            this.mSurfaceResized = false;
        }
        this.mForceScaleUntilResize = this.mForceScaleUntilResize && !this.mSurfaceResized;
        this.mService.markForSeamlessRotation(windowState, windowState.mSeamlesslyRotated && !this.mSurfaceResized);
        if (calculateCrop(this.mTmpClipRect)) {
            rect = this.mTmpClipRect;
        } else {
            rect = null;
        }
        float width = this.mSurfaceController.getWidth();
        float height = this.mSurfaceController.getHeight();
        Rect rect2 = attrs.surfaceInsets;
        if (isForceScaled()) {
            float f = width - (rect2.left + rect2.right);
            float f2 = height - (rect2.top + rect2.bottom);
            if (!this.mForceScaleUntilResize) {
                this.mSurfaceController.forceScaleableInTransaction(true);
            }
            task.mStack.getDimBounds(this.mTmpStackBounds);
            task.mStack.getFinalAnimationSourceHintBounds(this.mTmpSourceBounds);
            if (!this.mTmpSourceBounds.isEmpty() || ((this.mWin.mLastRelayoutContentInsets.width() <= 0 && this.mWin.mLastRelayoutContentInsets.height() <= 0) || task.mStack.lastAnimatingBoundsWasToFullscreen())) {
                z2 = false;
            } else {
                this.mTmpSourceBounds.set(task.mStack.mPreAnimationBounds);
                this.mTmpSourceBounds.inset(this.mWin.mLastRelayoutContentInsets);
                z2 = true;
            }
            this.mTmpStackBounds.intersectUnchecked(windowState.mParentFrame);
            this.mTmpSourceBounds.intersectUnchecked(windowState.mParentFrame);
            this.mTmpAnimatingBounds.intersectUnchecked(windowState.mParentFrame);
            if (!this.mTmpSourceBounds.isEmpty()) {
                task.mStack.getFinalAnimationBounds(this.mTmpAnimatingBounds);
                float fWidth = this.mTmpAnimatingBounds.width();
                float fWidth2 = this.mTmpSourceBounds.width();
                float fWidth3 = (f - this.mTmpStackBounds.width()) / (f - this.mTmpAnimatingBounds.width());
                this.mExtraHScale = (((fWidth - fWidth2) * fWidth3) + fWidth2) / fWidth2;
                if (z2) {
                    float fHeight2 = this.mTmpAnimatingBounds.height();
                    float fHeight3 = this.mTmpSourceBounds.height();
                    fHeight = (f2 - this.mTmpStackBounds.height()) / (f2 - this.mTmpAnimatingBounds.height());
                    this.mExtraVScale = (((fHeight2 - fHeight3) * fWidth3) + fHeight3) / fHeight3;
                } else {
                    this.mExtraVScale = this.mExtraHScale;
                    fHeight = fWidth3;
                }
                int i3 = 0 - ((int) ((this.mExtraHScale * fWidth3) * this.mTmpSourceBounds.left));
                int i4 = 0 - ((int) ((this.mExtraVScale * fHeight) * this.mTmpSourceBounds.top));
                Rect rect3 = this.mTmpClipRect;
                i = i3;
                i2 = i4;
                rect3.set((int) ((rect2.left + this.mTmpSourceBounds.left) * fWidth3), (int) ((rect2.top + this.mTmpSourceBounds.top) * fHeight), rect2.left + ((int) (width - (fWidth3 * (width - this.mTmpSourceBounds.right)))), rect2.top + ((int) (height - (fHeight * (height - this.mTmpSourceBounds.bottom)))));
                rect = rect3;
            } else {
                this.mExtraHScale = this.mTmpStackBounds.width() / f;
                this.mExtraVScale = this.mTmpStackBounds.height() / f2;
                rect = null;
                i = 0;
                i2 = 0;
            }
            this.mSurfaceController.setPositionInTransaction((float) Math.floor((int) ((i - ((int) (attrs.x * (1.0f - this.mExtraHScale)))) + (rect2.left * (1.0f - this.mExtraHScale)))), (float) Math.floor((int) ((i2 - ((int) (attrs.y * (1.0f - this.mExtraVScale)))) + (rect2.top * (1.0f - this.mExtraVScale)))), z);
            if (!this.mPipAnimationStarted) {
                this.mForceScaleUntilResize = true;
                this.mPipAnimationStarted = true;
            }
        } else {
            this.mPipAnimationStarted = false;
            if (!windowState.mSeamlesslyRotated) {
                int i5 = this.mXOffset;
                int i6 = this.mYOffset;
                if (this.mOffsetPositionForStackResize) {
                    if (z5) {
                        setOffsetPositionForStackResize(false);
                        this.mSurfaceController.deferTransactionUntil(this.mSurfaceController.getHandle(), this.mWin.getFrameNumber());
                    } else {
                        TaskStack stack = this.mWin.getStack();
                        this.mTmpPos.x = 0;
                        this.mTmpPos.y = 0;
                        if (stack != null) {
                            stack.getRelativePosition(this.mTmpPos);
                        }
                        i5 = -this.mTmpPos.x;
                        i6 = -this.mTmpPos.y;
                        if (rect != null) {
                            rect.right += this.mTmpPos.x;
                            rect.bottom += this.mTmpPos.y;
                        }
                    }
                }
                this.mSurfaceController.setPositionInTransaction(i5, i6, z);
            }
        }
        if ((z3 && !this.mForceScaleUntilResize) || (z4 && !windowState.mSeamlesslyRotated)) {
            this.mSurfaceController.setGeometryAppliesWithResizeInTransaction(true);
            this.mSurfaceController.forceScaleableInTransaction(false);
        }
        if (!windowState.mSeamlesslyRotated) {
            applyCrop(rect, z);
            this.mSurfaceController.setMatrixInTransaction(this.mDsDx * windowState.mHScale * this.mExtraHScale, this.mDtDx * windowState.mVScale * this.mExtraVScale, this.mDtDy * windowState.mHScale * this.mExtraHScale, this.mDsDy * windowState.mVScale * this.mExtraVScale, z);
        }
        if (this.mSurfaceResized) {
            this.mReportSurfaceResized = true;
            this.mAnimator.setPendingLayoutChanges(windowState.getDisplayId(), 4);
        }
    }

    void getContainerRect(Rect rect) {
        Task task = this.mWin.getTask();
        if (task != null) {
            task.getDimBounds(rect);
            return;
        }
        rect.bottom = 0;
        rect.right = 0;
        rect.top = 0;
        rect.left = 0;
    }

    void prepareSurfaceLocked(boolean z) {
        boolean z2;
        WindowState windowState = this.mWin;
        if (!hasSurface()) {
            if (windowState.getOrientationChanging() && windowState.isGoneForLayoutLw()) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Orientation change skips hidden " + windowState);
                }
                windowState.setOrientationChanging(false);
                return;
            }
            return;
        }
        computeShownFrameLocked();
        setSurfaceBoundariesLocked(z);
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            this.mService.mWindowManagerDebugger.debugPrepareSurfaceLocked("WindowManager", this.mIsWallpaper, this.mWin, this.mWin.mWallpaperVisible, windowState.isOnScreen(), windowState.mPolicyVisibility, windowState.mHasSurface, windowState.mDestroying, this.mLastHidden);
        }
        if (this.mIsWallpaper && !this.mWin.mWallpaperVisible) {
            hide("prepareSurfaceLocked");
        } else if (windowState.isParentWindowHidden() || !windowState.isOnScreen()) {
            hide("prepareSurfaceLocked");
            this.mWallpaperControllerLocked.hideWallpapers(windowState);
            if (windowState.getOrientationChanging() && windowState.isGoneForLayoutLw()) {
                windowState.setOrientationChanging(false);
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v("WindowManager", "Orientation change skips hidden " + windowState);
                }
            }
        } else {
            if (this.mLastLayer != this.mAnimLayer || this.mLastAlpha != this.mShownAlpha || this.mLastDsDx != this.mDsDx || this.mLastDtDx != this.mDtDx || this.mLastDsDy != this.mDsDy || this.mLastDtDy != this.mDtDy || windowState.mLastHScale != windowState.mHScale || windowState.mLastVScale != windowState.mVScale || this.mLastHidden) {
                this.mLastAlpha = this.mShownAlpha;
                this.mLastLayer = this.mAnimLayer;
                this.mLastDsDx = this.mDsDx;
                this.mLastDtDx = this.mDtDx;
                this.mLastDsDy = this.mDsDy;
                this.mLastDtDy = this.mDtDy;
                windowState.mLastHScale = windowState.mHScale;
                windowState.mLastVScale = windowState.mVScale;
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                    WindowManagerService.logSurface(windowState, "controller=" + this.mSurfaceController + "alpha=" + this.mShownAlpha + " layer=" + this.mAnimLayer + " matrix=[" + this.mDsDx + "*" + windowState.mHScale + "," + this.mDtDx + "*" + windowState.mVScale + "][" + this.mDtDy + "*" + windowState.mHScale + "," + this.mDsDy + "*" + windowState.mVScale + "]", false);
                }
                if (this.mSurfaceController.prepareToShowInTransaction(this.mShownAlpha, this.mExtraHScale * this.mDsDx * windowState.mHScale, this.mExtraVScale * this.mDtDx * windowState.mVScale, this.mExtraHScale * this.mDtDy * windowState.mHScale, this.mExtraVScale * this.mDsDy * windowState.mVScale, z) && this.mDrawState == 4 && this.mLastHidden) {
                    if (showSurfaceRobustlyLocked()) {
                        markPreservedSurfaceForDestroy();
                        this.mAnimator.requestRemovalOfReplacedWindows(windowState);
                        this.mLastHidden = false;
                        if (this.mIsWallpaper) {
                            windowState.dispatchWallpaperVisibility(true);
                        }
                        this.mAnimator.setPendingLayoutChanges(windowState.getDisplayId(), 8);
                    } else {
                        windowState.setOrientationChanging(false);
                    }
                }
                if (hasSurface()) {
                    windowState.mToken.hasVisible = true;
                }
            } else if (WindowManagerDebugConfig.DEBUG_ANIM && isAnimationSet()) {
                Slog.v("WindowManager", "prepareSurface: No changes in animation for " + this);
            }
            z2 = true;
            if (windowState.getOrientationChanging()) {
                if (!windowState.isDrawnLw()) {
                    this.mAnimator.mBulkUpdateParams &= -9;
                    this.mAnimator.mLastWindowFreezeSource = windowState;
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v("WindowManager", "Orientation continue waiting for draw in " + windowState);
                    }
                } else {
                    windowState.setOrientationChanging(false);
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v("WindowManager", "Orientation change complete in " + windowState);
                    }
                }
            }
            if (!z2) {
                windowState.mToken.hasVisible = true;
                return;
            }
            return;
        }
        z2 = false;
        if (windowState.getOrientationChanging()) {
        }
        if (!z2) {
        }
    }

    void setTransparentRegionHintLocked(Region region) {
        if (this.mSurfaceController == null) {
            Slog.w("WindowManager", "setTransparentRegionHint: null mSurface after mHasSurface true");
        } else {
            this.mSurfaceController.setTransparentRegionHint(region);
        }
    }

    boolean setWallpaperOffset(int i, int i2) {
        if (this.mXOffset == i && this.mYOffset == i2) {
            return false;
        }
        this.mXOffset = i;
        this.mYOffset = i2;
        try {
            try {
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", ">>> OPEN TRANSACTION setWallpaperOffset");
                }
                this.mService.openSurfaceTransaction();
                this.mSurfaceController.setPositionInTransaction(i, i2, false);
                applyCrop(null, false);
                this.mService.closeSurfaceTransaction("setWallpaperOffset");
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", "<<< CLOSE TRANSACTION setWallpaperOffset");
                }
                return true;
            } catch (RuntimeException e) {
                Slog.w("WindowManager", "Error positioning surface of " + this.mWin + " pos=(" + i + "," + i2 + ")", e);
                this.mService.closeSurfaceTransaction("setWallpaperOffset");
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", "<<< CLOSE TRANSACTION setWallpaperOffset");
                }
                return true;
            }
        } catch (Throwable th) {
            this.mService.closeSurfaceTransaction("setWallpaperOffset");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION setWallpaperOffset");
            }
            return true;
        }
    }

    boolean tryChangeFormatInPlaceLocked() {
        if (this.mSurfaceController == null) {
            return false;
        }
        WindowManager.LayoutParams attrs = this.mWin.getAttrs();
        if (((attrs.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0 ? -3 : attrs.format) != this.mSurfaceFormat) {
            return false;
        }
        setOpaqueLocked(!PixelFormat.formatHasAlpha(attrs.format));
        return true;
    }

    void setOpaqueLocked(boolean z) {
        if (this.mSurfaceController == null) {
            return;
        }
        this.mSurfaceController.setOpaque(z);
    }

    void setSecureLocked(boolean z) {
        if (this.mSurfaceController == null) {
            return;
        }
        this.mSurfaceController.setSecure(z);
    }

    private boolean showSurfaceRobustlyLocked() {
        if (this.mWin.getWindowConfiguration().windowsAreScaleable()) {
            this.mSurfaceController.forceScaleableInTransaction(true);
        }
        if (!this.mSurfaceController.showRobustlyInTransaction()) {
            return false;
        }
        if (this.mPendingDestroySurface != null && this.mDestroyPreservedSurfaceUponRedraw) {
            this.mPendingDestroySurface.mSurfaceControl.hide();
            this.mPendingDestroySurface.reparentChildrenInTransaction(this.mSurfaceController);
        }
        return true;
    }

    void applyEnterAnimationLocked() {
        int i;
        if (this.mWin.mSkipEnterAnimationForSeamlessReplacement) {
            return;
        }
        if (this.mEnterAnimationPending) {
            this.mEnterAnimationPending = false;
            i = 1;
        } else {
            i = 3;
        }
        applyAnimationLocked(i, true);
        if (this.mService.mAccessibilityController != null && this.mWin.getDisplayId() == 0) {
            this.mService.mAccessibilityController.onWindowTransitionLocked(this.mWin, i);
        }
    }

    boolean applyAnimationLocked(int i, boolean z) {
        int i2 = 1;
        if (this.mWin.isSelfAnimating() && this.mAnimationIsEntrance == z) {
            return true;
        }
        if (z && this.mWin.mAttrs.type == 2011) {
            this.mWin.getDisplayContent().adjustForImeIfNeeded();
            this.mWin.setDisplayLayoutNeeded();
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
        Trace.traceBegin(32L, "WSA#applyAnimationLocked");
        if (this.mWin.mToken.okToAnimate()) {
            int iSelectAnimationLw = this.mPolicy.selectAnimationLw(this.mWin, i);
            Animation animationLoadAnimationAttr = null;
            int i3 = -1;
            if (iSelectAnimationLw != 0) {
                if (iSelectAnimationLw != -1) {
                    animationLoadAnimationAttr = AnimationUtils.loadAnimation(this.mContext, iSelectAnimationLw);
                }
            } else {
                switch (i) {
                    case 1:
                        i3 = 0;
                        break;
                    case 3:
                        i2 = 2;
                    case 2:
                        i3 = i2;
                        break;
                    case 4:
                        i3 = 3;
                        break;
                }
                if (i3 >= 0) {
                    animationLoadAnimationAttr = this.mService.mAppTransition.loadAnimationAttr(this.mWin.mAttrs, i3, 0);
                }
            }
            if (WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v("WindowManager", "applyAnimation: win=" + this + " anim=" + iSelectAnimationLw + " attr=0x" + Integer.toHexString(i3) + " a=" + animationLoadAnimationAttr + " transit=" + i + " isEntrance=" + z + " Callers " + Debug.getCallers(3));
            }
            if (animationLoadAnimationAttr != null) {
                if (WindowManagerDebugConfig.DEBUG_ANIM) {
                    WindowManagerService.logWithStack("WindowManager", "Loaded animation " + animationLoadAnimationAttr + " for " + this);
                }
                this.mWin.startAnimation(animationLoadAnimationAttr);
                this.mAnimationIsEntrance = z;
            }
        } else {
            this.mWin.cancelAnimation();
        }
        if (!z && this.mWin.mAttrs.type == 2011) {
            this.mWin.getDisplayContent().adjustForImeIfNeeded();
        }
        Trace.traceEnd(32L);
        return isAnimationSet();
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        this.mLastClipRect.writeToProto(protoOutputStream, 1146756268033L);
        if (this.mSurfaceController != null) {
            this.mSurfaceController.writeToProto(protoOutputStream, 1146756268034L);
        }
        protoOutputStream.write(1159641169923L, this.mDrawState);
        this.mSystemDecorRect.writeToProto(protoOutputStream, 1146756268036L);
        protoOutputStream.end(jStart);
    }

    public void dump(PrintWriter printWriter, String str, boolean z) {
        if (this.mAnimationIsEntrance) {
            printWriter.print(str);
            printWriter.print(" mAnimationIsEntrance=");
            printWriter.print(this.mAnimationIsEntrance);
        }
        if (this.mSurfaceController != null) {
            this.mSurfaceController.dump(printWriter, str, z);
        }
        if (z) {
            printWriter.print(str);
            printWriter.print("mDrawState=");
            printWriter.print(drawStateToString());
            printWriter.print(str);
            printWriter.print(" mLastHidden=");
            printWriter.println(this.mLastHidden);
            printWriter.print(str);
            printWriter.print("mSystemDecorRect=");
            this.mSystemDecorRect.printShortString(printWriter);
            printWriter.print(" mLastClipRect=");
            this.mLastClipRect.printShortString(printWriter);
            if (!this.mLastFinalClipRect.isEmpty()) {
                printWriter.print(" mLastFinalClipRect=");
                this.mLastFinalClipRect.printShortString(printWriter);
            }
            printWriter.println();
        }
        if (this.mPendingDestroySurface != null) {
            printWriter.print(str);
            printWriter.print("mPendingDestroySurface=");
            printWriter.println(this.mPendingDestroySurface);
        }
        if (this.mSurfaceResized || this.mSurfaceDestroyDeferred) {
            printWriter.print(str);
            printWriter.print("mSurfaceResized=");
            printWriter.print(this.mSurfaceResized);
            printWriter.print(" mSurfaceDestroyDeferred=");
            printWriter.println(this.mSurfaceDestroyDeferred);
        }
        if (this.mShownAlpha != 1.0f || this.mAlpha != 1.0f || this.mLastAlpha != 1.0f) {
            printWriter.print(str);
            printWriter.print("mShownAlpha=");
            printWriter.print(this.mShownAlpha);
            printWriter.print(" mAlpha=");
            printWriter.print(this.mAlpha);
            printWriter.print(" mLastAlpha=");
            printWriter.println(this.mLastAlpha);
        }
        if (this.mHaveMatrix || this.mWin.mGlobalScale != 1.0f) {
            printWriter.print(str);
            printWriter.print("mGlobalScale=");
            printWriter.print(this.mWin.mGlobalScale);
            printWriter.print(" mDsDx=");
            printWriter.print(this.mDsDx);
            printWriter.print(" mDtDx=");
            printWriter.print(this.mDtDx);
            printWriter.print(" mDtDy=");
            printWriter.print(this.mDtDy);
            printWriter.print(" mDsDy=");
            printWriter.println(this.mDsDy);
        }
        if (this.mAnimationStartDelayed) {
            printWriter.print(str);
            printWriter.print("mAnimationStartDelayed=");
            printWriter.print(this.mAnimationStartDelayed);
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer("WindowStateAnimator{");
        stringBuffer.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuffer.append(' ');
        stringBuffer.append(this.mWin.mAttrs.getTitle());
        stringBuffer.append('}');
        return stringBuffer.toString();
    }

    void reclaimSomeSurfaceMemory(String str, boolean z) {
        this.mService.mRoot.reclaimSomeSurfaceMemory(this, str, z);
    }

    boolean getShown() {
        if (this.mSurfaceController != null) {
            return this.mSurfaceController.getShown();
        }
        return false;
    }

    void destroySurface() {
        try {
            try {
                if (this.mSurfaceController != null) {
                    this.mSurfaceController.destroyNotInTransaction();
                }
            } catch (RuntimeException e) {
                Slog.w("WindowManager", "Exception thrown when destroying surface " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e);
            }
        } finally {
            this.mWin.setHasSurface(false);
            this.mSurfaceController = null;
            this.mDrawState = 0;
        }
    }

    void seamlesslyRotateWindow(SurfaceControl.Transaction transaction, int i, int i2) {
        WindowState windowState = this.mWin;
        if (!windowState.isVisibleNow() || windowState.mIsWallpaper) {
            return;
        }
        Rect rect = this.mService.mTmpRect;
        Rect rect2 = this.mService.mTmpRect2;
        RectF rectF = this.mService.mTmpRectF;
        Matrix matrix = this.mService.mTmpTransform;
        float f = windowState.mFrame.left;
        float f2 = windowState.mFrame.top;
        windowState.mFrame.width();
        windowState.mFrame.height();
        this.mService.getDefaultDisplayContentLocked().getBounds(rect2);
        DisplayContent.createRotationMatrix(DisplayContent.deltaRotation(i2, i), f, f2, rect2.width(), rect2.height(), matrix);
        this.mService.markForSeamlessRotation(windowState, true);
        matrix.getValues(this.mService.mTmpFloats);
        float f3 = this.mService.mTmpFloats[0];
        float f4 = this.mService.mTmpFloats[3];
        float f5 = this.mService.mTmpFloats[1];
        float f6 = this.mService.mTmpFloats[4];
        this.mSurfaceController.setPosition(transaction, this.mService.mTmpFloats[2], this.mService.mTmpFloats[5], false);
        this.mSurfaceController.setMatrix(transaction, f3 * windowState.mHScale, f4 * windowState.mVScale, f5 * windowState.mHScale, f6 * windowState.mVScale, false);
    }

    boolean isForceScaled() {
        Task task = this.mWin.getTask();
        if (task != null && task.mStack.isForceScaled()) {
            return true;
        }
        return this.mForceScaleUntilResize;
    }

    void detachChildren() {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.detachChildren();
        }
        this.mChildrenDetached = true;
    }

    int getLayer() {
        return this.mLastLayer;
    }

    void setOffsetPositionForStackResize(boolean z) {
        this.mOffsetPositionForStackResize = z;
    }
}
