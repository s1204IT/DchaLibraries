package com.android.server.wm;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.MagnificationSpec;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;

class WindowStateAnimator {
    static final int COMMIT_DRAW_PENDING = 2;
    static final int DRAW_PENDING = 1;
    static final int HAS_DRAWN = 4;
    static final int NO_SURFACE = 0;
    static final long PENDING_TRANSACTION_FINISH_WAIT_TIME = 100;
    static final int READY_TO_SHOW = 3;
    static final int STACK_CLIP_AFTER_ANIM = 0;
    static final int STACK_CLIP_BEFORE_ANIM = 1;
    static final int STACK_CLIP_NONE = 2;
    static final String TAG = "WindowManager";
    static final int WINDOW_FREEZE_LAYER = 2000000;
    private int mAnimDx;
    private int mAnimDy;
    int mAnimLayer;
    boolean mAnimating;
    Animation mAnimation;
    boolean mAnimationIsEntrance;
    private boolean mAnimationStartDelayed;
    long mAnimationStartTime;
    final WindowAnimator mAnimator;
    AppWindowAnimator mAppAnimator;
    final WindowStateAnimator mAttachedWinAnimator;
    int mAttrType;
    final Context mContext;
    private boolean mDestroyPreservedSurfaceUponRedraw;
    int mDrawState;
    boolean mEnterAnimationPending;
    boolean mEnteringAnimation;
    boolean mForceScaleUntilResize;
    boolean mHasClipRect;
    boolean mHasLocalTransformation;
    boolean mHasTransformation;
    boolean mHaveMatrix;
    final boolean mIsWallpaper;
    boolean mKeyguardGoingAwayAnimation;
    boolean mKeyguardGoingAwayWithWallpaper;
    long mLastAnimationTime;
    boolean mLastHidden;
    int mLastLayer;
    boolean mLocalAnimating;
    private WindowSurfaceController mPendingDestroySurface;
    final WindowManagerPolicy mPolicy;
    boolean mReportSurfaceResized;
    final WindowManagerService mService;
    final Session mSession;
    WindowSurfaceController mSurfaceController;
    boolean mSurfaceDestroyDeferred;
    int mSurfaceFormat;
    boolean mSurfaceResized;
    final WallpaperController mWallpaperControllerLocked;
    boolean mWasAnimating;
    final WindowState mWin;
    final Transformation mTransformation = new Transformation();
    int mStackClip = 1;
    float mShownAlpha = 0.0f;
    float mAlpha = 0.0f;
    float mLastAlpha = 0.0f;
    Rect mClipRect = new Rect();
    Rect mTmpClipRect = new Rect();
    Rect mTmpFinalClipRect = new Rect();
    Rect mLastClipRect = new Rect();
    Rect mLastFinalClipRect = new Rect();
    Rect mTmpStackBounds = new Rect();
    private final Rect mSystemDecorRect = new Rect();
    private final Rect mLastSystemDecorRect = new Rect();
    private boolean mAnimateMove = false;
    float mDsDx = 1.0f;
    float mDtDx = 0.0f;
    float mDsDy = 0.0f;
    float mDtDy = 1.0f;
    float mLastDsDx = 1.0f;
    float mLastDtDx = 0.0f;
    float mLastDsDy = 0.0f;
    float mLastDtDy = 1.0f;
    long mDeferTransactionUntilFrame = -1;
    long mDeferTransactionTime = -1;
    float mExtraHScale = 1.0f;
    float mExtraVScale = 1.0f;
    private final Rect mTmpSize = new Rect();
    final int mWmDuration = SystemProperties.getInt("debug.wm.duration", -1);
    final int mWmOffset = SystemProperties.getInt("debug.wm.offset", -1);
    final int mWmSleep = SystemProperties.getInt("debug.wm.sleep", -1);
    boolean mDrawNeeded = true;

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

    WindowStateAnimator(WindowState win) {
        WindowManagerService service = win.mService;
        this.mService = service;
        this.mAnimator = service.mAnimator;
        this.mPolicy = service.mPolicy;
        this.mContext = service.mContext;
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent != null) {
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            this.mAnimDx = displayInfo.appWidth;
            this.mAnimDy = displayInfo.appHeight;
        } else {
            Slog.w(TAG, "WindowStateAnimator ctor: Display has been removed");
        }
        this.mWin = win;
        this.mAttachedWinAnimator = win.mAttachedWindow == null ? null : win.mAttachedWindow.mWinAnimator;
        this.mAppAnimator = win.mAppToken != null ? win.mAppToken.mAppAnimator : null;
        this.mSession = win.mSession;
        this.mAttrType = win.mAttrs.type;
        this.mIsWallpaper = win.mIsWallpaper;
        this.mWallpaperControllerLocked = this.mService.mWallpaperControllerLocked;
    }

    public void setAnimation(Animation anim, long startTime, int stackClip) {
        if (WindowManagerService.localLOGV) {
            Slog.v(TAG, "Setting animation in " + this + ": " + anim);
        }
        this.mAnimating = false;
        this.mLocalAnimating = false;
        this.mAnimation = anim;
        this.mAnimation.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        this.mAnimation.scaleCurrentDuration(this.mService.getWindowAnimationScaleLocked());
        this.mTransformation.clear();
        this.mTransformation.setAlpha(this.mLastHidden ? 0 : 1);
        this.mHasLocalTransformation = true;
        this.mAnimationStartTime = startTime;
        this.mStackClip = stackClip;
    }

    public void setAnimation(Animation anim, int stackClip) {
        setAnimation(anim, -1L, stackClip);
    }

    public void setAnimation(Animation anim) {
        setAnimation(anim, -1L, 0);
    }

    public void clearAnimation() {
        if (this.mAnimation == null) {
            return;
        }
        this.mAnimating = true;
        this.mLocalAnimating = false;
        this.mAnimation.cancel();
        this.mAnimation = null;
        this.mKeyguardGoingAwayAnimation = false;
        this.mKeyguardGoingAwayWithWallpaper = false;
        this.mStackClip = 1;
    }

    boolean isAnimationSet() {
        if (this.mAnimation != null || (this.mAttachedWinAnimator != null && this.mAttachedWinAnimator.mAnimation != null)) {
            return true;
        }
        if (this.mAppAnimator != null) {
            return this.mAppAnimator.isAnimating();
        }
        return false;
    }

    boolean isAnimationStarting() {
        return isAnimationSet() && !this.mAnimating;
    }

    boolean isDummyAnimation() {
        return this.mAppAnimator != null && this.mAppAnimator.animation == AppWindowAnimator.sDummyAnimation;
    }

    boolean isWindowAnimationSet() {
        return this.mAnimation != null;
    }

    boolean isWaitingForOpening() {
        if (this.mService.mAppTransition.isTransitionSet() && isDummyAnimation()) {
            return this.mService.mOpeningApps.contains(this.mWin.mAppToken);
        }
        return false;
    }

    void cancelExitAnimationForNextAnimationLocked() {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.d(TAG, "cancelExitAnimationForNextAnimationLocked: " + this.mWin);
        }
        if (this.mAnimation == null) {
            return;
        }
        this.mAnimation.cancel();
        this.mAnimation = null;
        this.mLocalAnimating = false;
        this.mWin.destroyOrSaveSurface();
    }

    private boolean stepAnimation(long currentTime) {
        if (this.mAnimation == null || !this.mLocalAnimating) {
            return false;
        }
        long currentTime2 = getAnimationFrameTime(this.mAnimation, currentTime);
        this.mTransformation.clear();
        boolean more = this.mAnimation.getTransformation(currentTime2, this.mTransformation);
        if (this.mAnimationStartDelayed && this.mAnimationIsEntrance) {
            this.mTransformation.setAlpha(0.0f);
        }
        return more;
    }

    boolean stepAnimationLocked(long currentTime) {
        this.mWasAnimating = this.mAnimating;
        DisplayContent displayContent = this.mWin.getDisplayContent();
        if (displayContent != null && this.mService.okToDisplay()) {
            if (this.mWin.isDrawnLw() && this.mAnimation != null) {
                this.mHasTransformation = true;
                this.mHasLocalTransformation = true;
                if (!this.mLocalAnimating) {
                    if (WindowManagerDebugConfig.DEBUG_ANIM) {
                        Slog.v(TAG, "Starting animation in " + this + " @ " + currentTime + ": ww=" + this.mWin.mFrame.width() + " wh=" + this.mWin.mFrame.height() + " dx=" + this.mAnimDx + " dy=" + this.mAnimDy + " scale=" + this.mService.getWindowAnimationScaleLocked());
                    }
                    DisplayInfo displayInfo = displayContent.getDisplayInfo();
                    if (this.mAnimateMove) {
                        this.mAnimateMove = false;
                        this.mAnimation.initialize(this.mWin.mFrame.width(), this.mWin.mFrame.height(), this.mAnimDx, this.mAnimDy);
                    } else {
                        this.mAnimation.initialize(this.mWin.mFrame.width(), this.mWin.mFrame.height(), displayInfo.appWidth, displayInfo.appHeight);
                    }
                    this.mAnimDx = displayInfo.appWidth;
                    this.mAnimDy = displayInfo.appHeight;
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
                if (WindowManagerDebugConfig.DEBUG_ANIM) {
                    Slog.v(TAG, "Finished animation in " + this + " @ " + currentTime);
                }
            }
            this.mHasLocalTransformation = false;
            if ((!this.mLocalAnimating || this.mAnimationIsEntrance) && this.mAppAnimator != null && this.mAppAnimator.animation != null) {
                this.mAnimating = true;
                this.mHasTransformation = true;
                this.mTransformation.clear();
                return false;
            }
            if (this.mHasTransformation || isAnimationSet()) {
                this.mAnimating = true;
            }
        } else if (this.mAnimation != null) {
            this.mAnimating = true;
        }
        if (!this.mAnimating && !this.mLocalAnimating) {
            return false;
        }
        Trace.traceBegin(4128L, "win animation done");
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v(TAG, "Animation done in " + this + ": exiting=" + this.mWin.mAnimatingExit + ", reportedVisible=" + (this.mWin.mAppToken != null ? this.mWin.mAppToken.reportedVisible : false));
        }
        Trace.traceEnd(4128L);
        this.mAnimating = false;
        this.mKeyguardGoingAwayAnimation = false;
        this.mKeyguardGoingAwayWithWallpaper = false;
        this.mLocalAnimating = false;
        if (this.mAnimation != null) {
            this.mAnimation.cancel();
            this.mAnimation = null;
        }
        if (this.mAnimator.mWindowDetachedWallpaper == this.mWin) {
            this.mAnimator.mWindowDetachedWallpaper = null;
        }
        this.mAnimLayer = this.mWin.mLayer + this.mService.mLayersController.getSpecialWindowAnimLayerAdjustment(this.mWin);
        if (WindowManagerDebugConfig.DEBUG_LAYERS) {
            Slog.v(TAG, "Stepping win " + this + " anim layer: " + this.mAnimLayer);
        }
        this.mHasTransformation = false;
        this.mHasLocalTransformation = false;
        this.mStackClip = 1;
        this.mWin.checkPolicyVisibilityChange();
        this.mTransformation.clear();
        if (this.mDrawState == 4 && this.mWin.mAttrs.type == 3 && this.mWin.mAppToken != null && this.mWin.mAppToken.firstWindowDrawn && this.mWin.mAppToken.startingData != null) {
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                Slog.v(TAG, "Finish starting " + this.mWin.mToken + ": first real window done animating");
            }
            this.mService.mFinishedStarting.add(this.mWin.mAppToken);
            this.mService.mH.sendEmptyMessage(7);
            if (this.mService.isFastStartingWindowSupport() && this.mService.isCacheFirstFrame()) {
                doCacheBitmap();
            }
        } else if (this.mAttrType == 2000 && this.mWin.mPolicyVisibility && displayContent != null) {
            displayContent.layoutNeeded = true;
        }
        finishExit();
        int displayId = this.mWin.getDisplayId();
        this.mAnimator.setPendingLayoutChanges(displayId, 8);
        if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
            this.mService.mWindowPlacerLocked.debugLayoutRepeats("WindowStateAnimator", this.mAnimator.getPendingLayoutChanges(displayId));
        }
        if (this.mWin.mAppToken == null) {
            return false;
        }
        this.mWin.mAppToken.updateReportedVisibilityLocked();
        return false;
    }

    void finishExit() {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v(TAG, "finishExit in " + this + ": exiting=" + this.mWin.mAnimatingExit + " remove=" + this.mWin.mRemoveOnExit + " windowAnimating=" + isWindowAnimationSet());
        }
        if (!this.mWin.mChildWindows.isEmpty()) {
            WindowList childWindows = new WindowList(this.mWin.mChildWindows);
            for (int i = childWindows.size() - 1; i >= 0; i--) {
                childWindows.get(i).mWinAnimator.finishExit();
            }
        }
        if (this.mEnteringAnimation) {
            this.mEnteringAnimation = false;
            this.mService.requestTraversal();
            if (this.mWin.mAppToken == null) {
                try {
                    this.mWin.mClient.dispatchWindowShown();
                } catch (RemoteException e) {
                }
            }
        }
        if (!isWindowAnimationSet() && this.mService.mAccessibilityController != null && this.mWin.getDisplayId() == 0) {
            this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
        }
        if (this.mWin.mAnimatingExit && !isWindowAnimationSet()) {
            if (WindowManagerService.localLOGV || WindowManagerDebugConfig.DEBUG_ADD_REMOVE) {
                Slog.v(TAG, "Exit animation finished in " + this + ": remove=" + this.mWin.mRemoveOnExit);
            }
            this.mWin.mDestroying = true;
            boolean hasSurface = hasSurface();
            if (hasSurface) {
                hide("finishExit");
            }
            if (this.mWin.mAppToken != null) {
                this.mWin.mAppToken.destroySurfaces();
            } else {
                if (hasSurface) {
                    this.mService.mDestroySurface.add(this.mWin);
                }
                if (this.mWin.mRemoveOnExit) {
                    this.mService.mPendingRemove.add(this.mWin);
                    this.mWin.mRemoveOnExit = false;
                }
            }
            this.mWin.mAnimatingExit = false;
            this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
        }
    }

    void hide(String reason) {
        if (this.mLastHidden) {
            return;
        }
        this.mLastHidden = true;
        if (this.mSurfaceController == null) {
            return;
        }
        this.mService.updateNonSystemOverlayWindowsVisibilityIfNeeded(this.mWin, false);
        this.mSurfaceController.hideInTransaction(reason);
    }

    boolean finishDrawingLocked() {
        boolean startingWindow = this.mWin.mAttrs.type == 3;
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && startingWindow) {
            Slog.v(TAG, "Finishing drawing window " + this.mWin + ": mDrawState=" + drawStateToString());
        }
        boolean layoutNeeded = this.mWin.clearAnimatingWithSavedSurface();
        if (this.mDrawState == 1) {
            if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE || WindowManagerDebugConfig.DEBUG_ANIM || WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.DEBUG_ORIENTATION || !WindowManagerService.IS_USER_BUILD) {
                Slog.v(TAG, "finishDrawingLocked: mDrawState=COMMIT_DRAW_PENDING " + this.mWin + " in " + this.mSurfaceController);
            }
            if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && startingWindow) {
                Slog.v(TAG, "Draw state now committed in " + this.mWin);
            }
            this.mDrawState = 2;
            return true;
        }
        return layoutNeeded;
    }

    boolean commitFinishDrawingLocked() {
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && this.mWin.mAttrs.type == 3) {
            Slog.i(TAG, "commitFinishDrawingLocked: " + this.mWin + " cur mDrawState=" + drawStateToString());
        }
        if (this.mDrawState != 2 && this.mDrawState != 3) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE || WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.i(TAG, "commitFinishDrawingLocked: mDrawState=READY_TO_SHOW " + this.mSurfaceController);
        }
        this.mDrawState = 3;
        AppWindowToken atoken = this.mWin.mAppToken;
        if (atoken != null && !atoken.allDrawn && this.mWin.mAttrs.type != 3) {
            return false;
        }
        boolean result = performShowLocked();
        return result;
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
            this.mSurfaceController.setLayer(this.mAnimLayer + 1);
        }
        this.mDestroyPreservedSurfaceUponRedraw = true;
        this.mSurfaceDestroyDeferred = true;
        destroySurfaceLocked();
    }

    void destroyPreservedSurfaceLocked() {
        if (!this.mDestroyPreservedSurfaceUponRedraw) {
            return;
        }
        destroyDeferredSurfaceLocked();
        this.mDestroyPreservedSurfaceUponRedraw = false;
    }

    void markPreservedSurfaceForDestroy() {
        if (!this.mDestroyPreservedSurfaceUponRedraw || this.mService.mDestroyPreservedSurface.contains(this.mWin)) {
            return;
        }
        this.mService.mDestroyPreservedSurface.add(this.mWin);
    }

    WindowSurfaceController createSurfaceLocked() {
        DisplayContent displayContent;
        WindowState w = this.mWin;
        if (w.hasSavedSurface()) {
            if (WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.i(TAG, "createSurface: " + this + ": called when we had a saved surface");
            }
            w.restoreSavedSurface();
            return this.mSurfaceController;
        }
        if (this.mSurfaceController != null) {
            return this.mSurfaceController;
        }
        w.setHasSurface(false);
        if (WindowManagerDebugConfig.DEBUG_ANIM || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.i(TAG, "createSurface " + this + ": mDrawState=DRAW_PENDING");
        }
        this.mDrawState = 1;
        if (w.mAppToken != null) {
            if (w.mAppToken.mAppAnimator.animation == null) {
                w.mAppToken.clearAllDrawn();
            } else {
                w.mAppToken.deferClearAllDrawn = true;
            }
        }
        this.mService.makeWindowFreezingScreenIfNeededLocked(w);
        WindowManager.LayoutParams attrs = w.mAttrs;
        int flags = this.mService.isSecureLocked(w) ? 132 : 4;
        this.mTmpSize.set(w.mFrame.left + w.mXOffset, w.mFrame.top + w.mYOffset, 0, 0);
        calculateSurfaceBounds(w, attrs);
        int width = this.mTmpSize.width();
        int height = this.mTmpSize.height();
        if (this.mService.isFastStartingWindowSupport() && w.isFastStartingWindow()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG, "[StartingWindow] window " + this);
            }
            if (width == 1 && height == 1 && (displayContent = w.getDisplayContent()) != null) {
                DisplayInfo displayInfo = displayContent.getDisplayInfo();
                width = displayInfo.logicalWidth;
                height = displayInfo.logicalHeight;
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.v(TAG, "[StartingWindow] apply logic width height");
                }
            }
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "Creating surface in session " + this.mSession.mSurfaceSession + " window " + this + " w=" + width + " h=" + height + " x=" + this.mTmpSize.left + " y=" + this.mTmpSize.top + " format=" + attrs.format + " flags=" + flags);
        }
        this.mLastSystemDecorRect.set(0, 0, 0, 0);
        this.mHasClipRect = false;
        this.mClipRect.set(0, 0, 0, 0);
        this.mLastClipRect.set(0, 0, 0, 0);
        try {
            boolean isHwAccelerated = (attrs.flags & 16777216) != 0;
            int format = isHwAccelerated ? -3 : attrs.format;
            if (!PixelFormat.formatHasAlpha(attrs.format) && attrs.surfaceInsets.left == 0 && attrs.surfaceInsets.top == 0 && attrs.surfaceInsets.right == 0 && attrs.surfaceInsets.bottom == 0 && !w.isDragResizing()) {
                flags |= 1024;
            }
            this.mSurfaceController = new WindowSurfaceController(this.mSession.mSurfaceSession, attrs.getTitle().toString(), width, height, format, flags, this);
            w.setHasSurface(true);
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC || !WindowManagerService.IS_USER_BUILD) {
                Slog.i(TAG, "  CREATE SURFACE " + this.mSurfaceController + " IN SESSION " + this.mSession.mSurfaceSession + ": pid=" + this.mSession.mPid + " format=" + attrs.format + " flags=0x" + Integer.toHexString(flags) + " / " + this);
            }
            if (WindowManagerService.localLOGV) {
                Slog.v(TAG, "Got surface: " + this.mSurfaceController + ", set left=" + w.mFrame.left + " top=" + w.mFrame.top + ", animLayer=" + this.mAnimLayer);
            }
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, ">>> OPEN TRANSACTION createSurfaceLocked");
                WindowManagerService.logSurface(w, "CREATE pos=(" + w.mFrame.left + "," + w.mFrame.top + ") (" + width + "x" + height + "), layer=" + this.mAnimLayer + " HIDE", false);
            }
            int layerStack = w.getDisplayContent().getDisplay().getLayerStack();
            this.mSurfaceController.setPositionAndLayer(this.mTmpSize.left, this.mTmpSize.top, layerStack, this.mAnimLayer);
            this.mLastHidden = true;
            if (WindowManagerService.localLOGV) {
                Slog.v(TAG, "Created surface " + this);
            }
            if (this.mService.isFastStartingWindowSupport() && w.isFastStartingWindow()) {
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... para) {
                        try {
                            WindowStateAnimator.this.drawIfNeeded();
                            return null;
                        } catch (Exception e) {
                            Slog.e(WindowStateAnimator.TAG, "FSW Exception: " + e);
                            return null;
                        }
                    }
                };
                task.execute(new Void[0]);
            }
            return this.mSurfaceController;
        } catch (Surface.OutOfResourcesException e) {
            Slog.w(TAG, "OutOfResourcesException creating surface");
            this.mService.reclaimSomeSurfaceMemoryLocked(this, "create", true);
            this.mDrawState = 0;
            return null;
        } catch (Exception e2) {
            Slog.e(TAG, "Exception creating surface", e2);
            this.mDrawState = 0;
            return null;
        }
    }

    private void calculateSurfaceBounds(WindowState w, WindowManager.LayoutParams attrs) {
        if ((attrs.flags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0) {
            this.mTmpSize.right = this.mTmpSize.left + w.mRequestedWidth;
            this.mTmpSize.bottom = this.mTmpSize.top + w.mRequestedHeight;
        } else if (w.isDragResizing()) {
            if (w.getResizeMode() == 0) {
                this.mTmpSize.left = 0;
                this.mTmpSize.top = 0;
            }
            DisplayInfo displayInfo = w.getDisplayInfo();
            this.mTmpSize.right = this.mTmpSize.left + displayInfo.logicalWidth;
            this.mTmpSize.bottom = this.mTmpSize.top + displayInfo.logicalHeight;
        } else {
            this.mTmpSize.right = this.mTmpSize.left + w.mCompatFrame.width();
            this.mTmpSize.bottom = this.mTmpSize.top + w.mCompatFrame.height();
        }
        if (this.mTmpSize.width() < 1) {
            this.mTmpSize.right = this.mTmpSize.left + 1;
        }
        if (this.mTmpSize.height() < 1) {
            this.mTmpSize.bottom = this.mTmpSize.top + 1;
        }
        this.mTmpSize.left -= attrs.surfaceInsets.left;
        this.mTmpSize.top -= attrs.surfaceInsets.top;
        this.mTmpSize.right += attrs.surfaceInsets.right;
        this.mTmpSize.bottom += attrs.surfaceInsets.bottom;
    }

    boolean hasSurface() {
        if (this.mWin.hasSavedSurface() || this.mSurfaceController == null) {
            return false;
        }
        return this.mSurfaceController.hasSurface();
    }

    void destroySurfaceLocked() {
        AppWindowToken wtoken = this.mWin.mAppToken;
        if (wtoken != null && this.mWin == wtoken.startingWindow) {
            wtoken.startingDisplayed = false;
        }
        this.mWin.clearHasSavedSurface();
        if (this.mSurfaceController == null) {
            return;
        }
        int i = this.mWin.mChildWindows.size();
        while (!this.mDestroyPreservedSurfaceUponRedraw && i > 0) {
            i--;
            WindowState c = this.mWin.mChildWindows.get(i);
            c.mAttachedHidden = true;
        }
        try {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                WindowManagerService.logWithStack(TAG, "Window " + this + " destroying surface " + this.mSurfaceController + ", session " + this.mSession);
            }
            if (!this.mSurfaceDestroyDeferred) {
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                    WindowManagerService.logSurface(this.mWin, "DESTROY", true);
                }
                destroySurface();
            } else if (this.mSurfaceController != null && this.mPendingDestroySurface != this.mSurfaceController) {
                if (this.mPendingDestroySurface != null) {
                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                        WindowManagerService.logSurface(this.mWin, "DESTROY PENDING", true);
                    }
                    this.mPendingDestroySurface.destroyInTransaction();
                }
                this.mPendingDestroySurface = this.mSurfaceController;
            }
            if (!this.mDestroyPreservedSurfaceUponRedraw) {
                this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
            }
        } catch (RuntimeException e) {
            Slog.w(TAG, "Exception thrown when destroying Window " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e.toString());
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
                this.mPendingDestroySurface.destroyInTransaction();
                if (!this.mDestroyPreservedSurfaceUponRedraw) {
                    this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
                }
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
        WindowState wallpaperTarget = this.mWallpaperControllerLocked.getWallpaperTarget();
        if (this.mIsWallpaper && wallpaperTarget != null && this.mService.mAnimateWallpaperWithTarget) {
            WindowStateAnimator wallpaperAnimator = wallpaperTarget.mWinAnimator;
            if (wallpaperAnimator.mHasLocalTransformation && wallpaperAnimator.mAnimation != null && !wallpaperAnimator.mAnimation.getDetachWallpaper()) {
                attachedTransformation = wallpaperAnimator.mTransformation;
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER && attachedTransformation != null) {
                    Slog.v(TAG, "WP target attached xform: " + attachedTransformation);
                }
            }
            AppWindowAnimator wpAppAnimator = wallpaperTarget.mAppToken == null ? null : wallpaperTarget.mAppToken.mAppAnimator;
            if (wpAppAnimator != null && wpAppAnimator.hasTransformation && wpAppAnimator.animation != null && !wpAppAnimator.animation.getDetachWallpaper()) {
                appTransformation = wpAppAnimator.transformation;
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER && appTransformation != null) {
                    Slog.v(TAG, "WP target app xform: " + appTransformation);
                }
            }
        }
        int displayId = this.mWin.getDisplayId();
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(displayId);
        boolean zIsAnimating = screenRotationAnimation != null ? screenRotationAnimation.isAnimating() : false;
        this.mHasClipRect = false;
        if (!selfTransformation && attachedTransformation == null && appTransformation == null && !zIsAnimating) {
            if ((this.mIsWallpaper && this.mService.mWindowPlacerLocked.mWallpaperActionPending) || this.mWin.isDragResizeChanged()) {
                return;
            }
            if (WindowManagerService.localLOGV) {
                Slog.v(TAG, "computeShownFrameLocked: " + this + " not attached, mAlpha=" + this.mAlpha);
            }
            MagnificationSpec spec2 = null;
            if (this.mService.mAccessibilityController != null && displayId == 0) {
                spec2 = this.mService.mAccessibilityController.getMagnificationSpecForWindowLocked(this.mWin);
            }
            if (spec2 == null) {
                this.mWin.mShownPosition.set(this.mWin.mFrame.left, this.mWin.mFrame.top);
                if (this.mWin.mXOffset != 0 || this.mWin.mYOffset != 0) {
                    this.mWin.mShownPosition.offset(this.mWin.mXOffset, this.mWin.mYOffset);
                }
                this.mShownAlpha = this.mAlpha;
                this.mHaveMatrix = false;
                this.mDsDx = this.mWin.mGlobalScale;
                this.mDtDx = 0.0f;
                this.mDsDy = 0.0f;
                this.mDtDy = this.mWin.mGlobalScale;
                return;
            }
            Rect frame = this.mWin.mFrame;
            float[] tmpFloats = this.mService.mTmpFloats;
            Matrix tmpMatrix = this.mWin.mTmpMatrix;
            tmpMatrix.setScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
            tmpMatrix.postTranslate(frame.left + this.mWin.mXOffset, frame.top + this.mWin.mYOffset);
            if (spec2 != null && !spec2.isNop()) {
                tmpMatrix.postScale(spec2.scale, spec2.scale);
                tmpMatrix.postTranslate(spec2.offsetX, spec2.offsetY);
            }
            tmpMatrix.getValues(tmpFloats);
            this.mHaveMatrix = true;
            this.mDsDx = tmpFloats[0];
            this.mDtDx = tmpFloats[3];
            this.mDsDy = tmpFloats[1];
            this.mDtDy = tmpFloats[4];
            this.mWin.mShownPosition.set((int) tmpFloats[2], (int) tmpFloats[5]);
            this.mShownAlpha = this.mAlpha;
            return;
        }
        Rect frame2 = this.mWin.mFrame;
        float[] tmpFloats2 = this.mService.mTmpFloats;
        Matrix tmpMatrix2 = this.mWin.mTmpMatrix;
        if (zIsAnimating && screenRotationAnimation.isRotating()) {
            float w = frame2.width();
            float h = frame2.height();
            if (w < 1.0f || h < 1.0f) {
                tmpMatrix2.reset();
            } else {
                tmpMatrix2.setScale((2.0f / w) + 1.0f, (2.0f / h) + 1.0f, w / 2.0f, h / 2.0f);
            }
        } else {
            tmpMatrix2.reset();
        }
        tmpMatrix2.postScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
        if (selfTransformation) {
            tmpMatrix2.postConcat(this.mTransformation.getMatrix());
        }
        if (attachedTransformation != null) {
            tmpMatrix2.postConcat(attachedTransformation.getMatrix());
        }
        if (appTransformation != null) {
            tmpMatrix2.postConcat(appTransformation.getMatrix());
        }
        tmpMatrix2.postTranslate(frame2.left + this.mWin.mXOffset, frame2.top + this.mWin.mYOffset);
        if (zIsAnimating) {
            tmpMatrix2.postConcat(screenRotationAnimation.getEnterTransformation().getMatrix());
        }
        if (this.mService.mAccessibilityController != null && displayId == 0 && (spec = this.mService.mAccessibilityController.getMagnificationSpecForWindowLocked(this.mWin)) != null && !spec.isNop()) {
            tmpMatrix2.postScale(spec.scale, spec.scale);
            tmpMatrix2.postTranslate(spec.offsetX, spec.offsetY);
        }
        this.mHaveMatrix = true;
        tmpMatrix2.getValues(tmpFloats2);
        this.mDsDx = tmpFloats2[0];
        this.mDtDx = tmpFloats2[3];
        this.mDsDy = tmpFloats2[1];
        this.mDtDy = tmpFloats2[4];
        float x = tmpFloats2[2];
        float y = tmpFloats2[5];
        this.mWin.mShownPosition.set((int) x, (int) y);
        this.mShownAlpha = this.mAlpha;
        if (!this.mService.mLimitedAlphaCompositing || !PixelFormat.formatHasAlpha(this.mWin.mAttrs.format) || (this.mWin.isIdentityMatrix(this.mDsDx, this.mDtDx, this.mDsDy, this.mDtDy) && x == frame2.left && y == frame2.top)) {
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
                    this.mHasClipRect = true;
                    if (this.mWin.layoutInParentFrame()) {
                        this.mClipRect.offset(this.mWin.mContainingFrame.left - this.mWin.mFrame.left, this.mWin.mContainingFrame.top - this.mWin.mFrame.top);
                    }
                }
            }
            if (zIsAnimating) {
                this.mShownAlpha *= screenRotationAnimation.getEnterTransformation().getAlpha();
            }
        }
        if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE || WindowManagerService.localLOGV) {
            if (this.mShownAlpha == 1.0d || this.mShownAlpha == 0.0d) {
                Slog.v(TAG, "computeShownFrameLocked: Animating " + this + " mAlpha=" + this.mAlpha + " self=" + (selfTransformation ? Float.valueOf(this.mTransformation.getAlpha()) : "null") + " attached=" + (attachedTransformation == null ? "null" : Float.valueOf(attachedTransformation.getAlpha())) + " app=" + (appTransformation == null ? "null" : Float.valueOf(appTransformation.getAlpha())) + " screen=" + (zIsAnimating ? Float.valueOf(screenRotationAnimation.getEnterTransformation().getAlpha()) : "null"));
            }
        }
    }

    private void calculateSystemDecorRect() {
        WindowState w = this.mWin;
        Rect decorRect = w.mDecorFrame;
        int width = w.mFrame.width();
        int height = w.mFrame.height();
        int left = w.mXOffset + w.mFrame.left;
        int top = w.mYOffset + w.mFrame.top;
        if (w.isDockedResizing() || (w.isChildWindow() && w.mAttachedWindow.isDockedResizing())) {
            DisplayInfo displayInfo = w.getDisplayContent().getDisplayInfo();
            this.mSystemDecorRect.set(0, 0, Math.max(width, displayInfo.logicalWidth), Math.max(height, displayInfo.logicalHeight));
        } else {
            this.mSystemDecorRect.set(0, 0, width, height);
        }
        boolean cropToDecor = (w.inFreeformWorkspace() && w.isAnimatingLw()) ? false : true;
        if (cropToDecor) {
            this.mSystemDecorRect.intersect(decorRect.left - left, decorRect.top - top, decorRect.right - left, decorRect.bottom - top);
        }
        if (!w.mEnforceSizeCompat || w.mInvGlobalScale == 1.0f) {
            return;
        }
        float scale = w.mInvGlobalScale;
        this.mSystemDecorRect.left = (int) ((this.mSystemDecorRect.left * scale) - 0.5f);
        this.mSystemDecorRect.top = (int) ((this.mSystemDecorRect.top * scale) - 0.5f);
        this.mSystemDecorRect.right = (int) (((this.mSystemDecorRect.right + 1) * scale) - 0.5f);
        this.mSystemDecorRect.bottom = (int) (((this.mSystemDecorRect.bottom + 1) * scale) - 0.5f);
    }

    void calculateSurfaceWindowCrop(Rect clipRect, Rect finalClipRect) {
        WindowState w = this.mWin;
        DisplayContent displayContent = w.getDisplayContent();
        if (displayContent == null) {
            clipRect.setEmpty();
            finalClipRect.setEmpty();
            return;
        }
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "Updating crop win=" + w + " mLastCrop=" + this.mLastClipRect);
        }
        if (!w.isDefaultDisplay()) {
            this.mSystemDecorRect.set(0, 0, w.mCompatFrame.width(), w.mCompatFrame.height());
            this.mSystemDecorRect.intersect(-w.mCompatFrame.left, -w.mCompatFrame.top, displayInfo.logicalWidth - w.mCompatFrame.left, displayInfo.logicalHeight - w.mCompatFrame.top);
        } else if (w.mLayer >= this.mService.mSystemDecorLayer || w.mDecorFrame.isEmpty()) {
            this.mSystemDecorRect.set(0, 0, w.mCompatFrame.width(), w.mCompatFrame.height());
        } else if (w.mAttrs.type == 2013 && this.mAnimator.isAnimating()) {
            this.mTmpClipRect.set(this.mSystemDecorRect);
            calculateSystemDecorRect();
            this.mSystemDecorRect.union(this.mTmpClipRect);
        } else {
            calculateSystemDecorRect();
            if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
                Slog.d(TAG, "Applying decor to crop win=" + w + " mDecorFrame=" + w.mDecorFrame + " mSystemDecorRect=" + this.mSystemDecorRect);
            }
        }
        boolean fullscreen = w.isFrameFullscreen(displayInfo);
        boolean isFreeformResizing = w.isDragResizing() && w.getResizeMode() == 0;
        clipRect.set((!this.mHasClipRect || fullscreen) ? this.mSystemDecorRect : this.mClipRect);
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "win=" + w + " Initial clip rect: " + clipRect + " mHasClipRect=" + this.mHasClipRect + " fullscreen=" + fullscreen);
        }
        if (isFreeformResizing && !w.isChildWindow()) {
            clipRect.offset(w.mShownPosition.x, w.mShownPosition.y);
        }
        WindowManager.LayoutParams attrs = w.mAttrs;
        clipRect.left -= attrs.surfaceInsets.left;
        clipRect.top -= attrs.surfaceInsets.top;
        clipRect.right += attrs.surfaceInsets.right;
        clipRect.bottom += attrs.surfaceInsets.bottom;
        if (this.mHasClipRect && fullscreen) {
            clipRect.intersect(this.mClipRect);
        }
        clipRect.offset(attrs.surfaceInsets.left, attrs.surfaceInsets.top);
        finalClipRect.setEmpty();
        adjustCropToStackBounds(w, clipRect, finalClipRect, isFreeformResizing);
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "win=" + w + " Clip rect after stack adjustment=" + clipRect);
        }
        w.transformClipRectFromScreenToSurfaceSpace(clipRect);
        if (w.hasJustMovedInStack() && this.mLastClipRect.isEmpty() && !clipRect.isEmpty()) {
            clipRect.setEmpty();
        }
    }

    void updateSurfaceWindowCrop(Rect clipRect, Rect finalClipRect, boolean recoveringMemory) {
        if (WindowManagerDebugConfig.DEBUG_WINDOW_CROP) {
            Slog.d(TAG, "updateSurfaceWindowCrop: win=" + this.mWin + " clipRect=" + clipRect + " finalClipRect=" + finalClipRect);
        }
        if (clipRect != null) {
            if (!clipRect.equals(this.mLastClipRect)) {
                this.mLastClipRect.set(clipRect);
                this.mSurfaceController.setCropInTransaction(clipRect, recoveringMemory);
            }
        } else {
            this.mSurfaceController.clearCropInTransaction(recoveringMemory);
        }
        if (finalClipRect.equals(this.mLastFinalClipRect)) {
            return;
        }
        this.mLastFinalClipRect.set(finalClipRect);
        this.mSurfaceController.setFinalCropInTransaction(finalClipRect);
        if (!this.mDestroyPreservedSurfaceUponRedraw || this.mPendingDestroySurface == null) {
            return;
        }
        this.mPendingDestroySurface.setFinalCropInTransaction(finalClipRect);
    }

    private int resolveStackClip() {
        if (this.mAppAnimator != null && this.mAppAnimator.animation != null) {
            return this.mAppAnimator.getStackClip();
        }
        return this.mStackClip;
    }

    private void adjustCropToStackBounds(WindowState w, Rect clipRect, Rect finalClipRect, boolean isFreeformResizing) {
        Task task;
        DisplayContent displayContent = w.getDisplayContent();
        if ((displayContent != null && !displayContent.isDefaultDisplay) || (task = w.getTask()) == null || !task.cropWindowsToStackBounds()) {
            return;
        }
        int stackClip = resolveStackClip();
        if (isAnimationSet() && stackClip == 2) {
            return;
        }
        WindowState winShowWhenLocked = (WindowState) this.mPolicy.getWinShowWhenLockedLw();
        if (w == winShowWhenLocked && this.mPolicy.isKeyguardShowingOrOccluded()) {
            return;
        }
        TaskStack stack = task.mStack;
        stack.getDimBounds(this.mTmpStackBounds);
        Rect surfaceInsets = w.getAttrs().surfaceInsets;
        int frameX = isFreeformResizing ? (int) this.mSurfaceController.getX() : (w.mFrame.left + this.mWin.mXOffset) - surfaceInsets.left;
        int frameY = isFreeformResizing ? (int) this.mSurfaceController.getY() : (w.mFrame.top + this.mWin.mYOffset) - surfaceInsets.top;
        boolean useFinalClipRect = (isAnimationSet() && stackClip == 0) ? true : this.mDestroyPreservedSurfaceUponRedraw;
        if (useFinalClipRect) {
            finalClipRect.set(this.mTmpStackBounds);
            return;
        }
        if (ActivityManager.StackId.hasWindowShadow(stack.mStackId) && !ActivityManager.StackId.isTaskResizeAllowed(stack.mStackId)) {
            this.mTmpStackBounds.inset(-surfaceInsets.left, -surfaceInsets.top, -surfaceInsets.right, -surfaceInsets.bottom);
        }
        clipRect.left = Math.max(0, Math.max(this.mTmpStackBounds.left, clipRect.left + frameX) - frameX);
        clipRect.top = Math.max(0, Math.max(this.mTmpStackBounds.top, clipRect.top + frameY) - frameY);
        clipRect.right = Math.max(0, Math.min(this.mTmpStackBounds.right, clipRect.right + frameX) - frameX);
        clipRect.bottom = Math.max(0, Math.min(this.mTmpStackBounds.bottom, clipRect.bottom + frameY) - frameY);
    }

    void setSurfaceBoundariesLocked(boolean recoveringMemory) {
        WindowState w = this.mWin;
        Task task = w.getTask();
        if (w.isResizedWhileNotDragResizing() && !w.isGoneForLayoutLw()) {
            return;
        }
        this.mTmpSize.set(w.mShownPosition.x, w.mShownPosition.y, 0, 0);
        calculateSurfaceBounds(w, w.getAttrs());
        this.mExtraHScale = 1.0f;
        this.mExtraVScale = 1.0f;
        boolean wasForceScaled = this.mForceScaleUntilResize;
        boolean wasResized = this.mSurfaceResized;
        if (!w.inPinnedWorkspace() || !w.mRelayoutCalled || w.mInRelayout) {
            this.mSurfaceResized = this.mSurfaceController.setSizeInTransaction(this.mTmpSize.width(), this.mTmpSize.height(), recoveringMemory);
        } else {
            this.mSurfaceResized = false;
        }
        this.mForceScaleUntilResize = this.mForceScaleUntilResize && !this.mSurfaceResized;
        calculateSurfaceWindowCrop(this.mTmpClipRect, this.mTmpFinalClipRect);
        float surfaceWidth = this.mSurfaceController.getWidth();
        float surfaceHeight = this.mSurfaceController.getHeight();
        if ((task != null && task.mStack.getForceScaleToCrop()) || this.mForceScaleUntilResize) {
            int hInsets = w.getAttrs().surfaceInsets.left + w.getAttrs().surfaceInsets.right;
            int vInsets = w.getAttrs().surfaceInsets.top + w.getAttrs().surfaceInsets.bottom;
            if (!this.mForceScaleUntilResize) {
                this.mSurfaceController.forceScaleableInTransaction(true);
            }
            this.mExtraHScale = (this.mTmpClipRect.width() - hInsets) / (surfaceWidth - hInsets);
            this.mExtraVScale = (this.mTmpClipRect.height() - vInsets) / (surfaceHeight - vInsets);
            int posX = (int) (this.mTmpSize.left - (w.mAttrs.x * (1.0f - this.mExtraHScale)));
            int posY = (int) (this.mTmpSize.top - (w.mAttrs.y * (1.0f - this.mExtraVScale)));
            this.mSurfaceController.setPositionInTransaction((float) Math.floor((int) (posX + (w.getAttrs().surfaceInsets.left * (1.0f - this.mExtraHScale)))), (float) Math.floor((int) (posY + (w.getAttrs().surfaceInsets.top * (1.0f - this.mExtraVScale)))), recoveringMemory);
            this.mTmpClipRect.set(0, 0, (int) surfaceWidth, (int) surfaceHeight);
            this.mTmpFinalClipRect.setEmpty();
            this.mForceScaleUntilResize = true;
        } else {
            this.mSurfaceController.setPositionInTransaction(this.mTmpSize.left, this.mTmpSize.top, recoveringMemory);
            if (w.mIsWallpaper && wasResized != this.mSurfaceResized) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.d(TAG, w + " forceScaleableInTransaction " + this.mSurfaceResized);
                }
                this.mSurfaceController.forceScaleableInTransaction(this.mSurfaceResized);
            }
        }
        if (wasForceScaled && !this.mForceScaleUntilResize) {
            this.mSurfaceController.setPositionAppliesWithResizeInTransaction(true);
            this.mSurfaceController.forceScaleableInTransaction(false);
        }
        Rect clipRect = this.mTmpClipRect;
        if (w.inPinnedWorkspace()) {
            clipRect = null;
            task.mStack.getDimBounds(this.mTmpFinalClipRect);
            this.mTmpFinalClipRect.inset(-w.mAttrs.surfaceInsets.left, -w.mAttrs.surfaceInsets.top, -w.mAttrs.surfaceInsets.right, -w.mAttrs.surfaceInsets.bottom);
        }
        updateSurfaceWindowCrop(clipRect, this.mTmpFinalClipRect, recoveringMemory);
        this.mSurfaceController.setMatrixInTransaction(this.mDsDx * w.mHScale * this.mExtraHScale, this.mDtDx * w.mVScale * this.mExtraVScale, this.mDsDy * w.mHScale * this.mExtraHScale, this.mDtDy * w.mVScale * this.mExtraVScale, recoveringMemory);
        if (!this.mSurfaceResized) {
            return;
        }
        this.mReportSurfaceResized = true;
        this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 4);
        w.applyDimLayerIfNeeded();
    }

    void prepareSurfaceLocked(boolean recoveringMemory) {
        WindowState w = this.mWin;
        if (!hasSurface()) {
            if (w.mOrientationChanging) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v(TAG, "Orientation change skips hidden " + w);
                }
                w.mOrientationChanging = false;
                return;
            }
            return;
        }
        if (isWaitingForOpening()) {
            return;
        }
        boolean displayed = false;
        computeShownFrameLocked();
        setSurfaceBoundariesLocked(recoveringMemory);
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            Slog.v(TAG, this + " prepareSurfaceLocked , mIsWallpaper=" + this.mIsWallpaper + ", mWin.mWallpaperVisible=" + this.mWin.mWallpaperVisible + ", w.mAttachedHidden=" + w.mAttachedHidden + ", w.isOnScreen=" + w.isOnScreen() + ", w.mPolicyVisibility=" + w.mPolicyVisibility + ", w.isOnScreenIgnoringKeyguard=" + w.isOnScreenIgnoringKeyguard() + ", w.mHasSurface=" + w.mHasSurface + ", w.mDestroying=" + w.mDestroying + ", mLastHidden=" + this.mLastHidden);
        }
        if (this.mIsWallpaper && !this.mWin.mWallpaperVisible) {
            hide("prepareSurfaceLocked");
        } else if (w.mAttachedHidden || !w.isOnScreen()) {
            hide("prepareSurfaceLocked");
            this.mWallpaperControllerLocked.hideWallpapers(w);
            if (w.mOrientationChanging) {
                w.mOrientationChanging = false;
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v(TAG, "Orientation change skips hidden " + w);
                }
            }
        } else if (this.mLastLayer == this.mAnimLayer && this.mLastAlpha == this.mShownAlpha && this.mLastDsDx == this.mDsDx && this.mLastDtDx == this.mDtDx && this.mLastDsDy == this.mDsDy && this.mLastDtDy == this.mDtDy && w.mLastHScale == w.mHScale && w.mLastVScale == w.mVScale && !this.mLastHidden) {
            if (WindowManagerDebugConfig.DEBUG_ANIM && isAnimationSet()) {
                Slog.v(TAG, "prepareSurface: No changes in animation for " + this);
            }
            displayed = true;
        } else {
            displayed = true;
            this.mLastAlpha = this.mShownAlpha;
            this.mLastLayer = this.mAnimLayer;
            this.mLastDsDx = this.mDsDx;
            this.mLastDtDx = this.mDtDx;
            this.mLastDsDy = this.mDsDy;
            this.mLastDtDy = this.mDtDy;
            w.mLastHScale = w.mHScale;
            w.mLastVScale = w.mVScale;
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                WindowManagerService.logSurface(w, "controller=" + this.mSurfaceController + "alpha=" + this.mShownAlpha + " layer=" + this.mAnimLayer + " matrix=[" + this.mDsDx + "*" + w.mHScale + "," + this.mDtDx + "*" + w.mVScale + "][" + this.mDsDy + "*" + w.mHScale + "," + this.mDtDy + "*" + w.mVScale + "]", false);
            }
            boolean prepared = this.mSurfaceController.prepareToShowInTransaction(this.mShownAlpha, this.mAnimLayer, this.mDsDx * w.mHScale * this.mExtraHScale, this.mDtDx * w.mVScale * this.mExtraVScale, this.mDsDy * w.mHScale * this.mExtraHScale, this.mDtDy * w.mVScale * this.mExtraVScale, recoveringMemory);
            if (prepared && this.mLastHidden && this.mDrawState == 4) {
                if (showSurfaceRobustlyLocked()) {
                    markPreservedSurfaceForDestroy();
                    this.mAnimator.requestRemovalOfReplacedWindows(w);
                    this.mLastHidden = false;
                    if (this.mIsWallpaper) {
                        this.mWallpaperControllerLocked.dispatchWallpaperVisibility(w, true);
                    }
                    this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 8);
                } else {
                    w.mOrientationChanging = false;
                }
            }
            if (hasSurface()) {
                w.mToken.hasVisible = true;
            }
        }
        if (displayed) {
            if (w.mOrientationChanging) {
                if (w.isDrawnLw()) {
                    w.mOrientationChanging = false;
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v(TAG, "Orientation change complete in " + w);
                    }
                } else {
                    this.mAnimator.mBulkUpdateParams &= -9;
                    this.mAnimator.mLastWindowFreezeSource = w;
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v(TAG, "Orientation continue waiting for draw in " + w);
                    }
                }
            }
            w.mToken.hasVisible = true;
        }
    }

    void setTransparentRegionHintLocked(Region region) {
        if (this.mSurfaceController == null) {
            Slog.w(TAG, "setTransparentRegionHint: null mSurface after mHasSurface true");
        } else {
            this.mSurfaceController.setTransparentRegionHint(region);
        }
    }

    void setWallpaperOffset(Point shownPosition) {
        WindowManager.LayoutParams attrs = this.mWin.getAttrs();
        int left = shownPosition.x - attrs.surfaceInsets.left;
        int top = shownPosition.y - attrs.surfaceInsets.top;
        try {
            try {
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i(TAG, ">>> OPEN TRANSACTION setWallpaperOffset");
                }
                SurfaceControl.openTransaction();
                this.mSurfaceController.setPositionInTransaction(this.mWin.mFrame.left + left, this.mWin.mFrame.top + top, false);
                calculateSurfaceWindowCrop(this.mTmpClipRect, this.mTmpFinalClipRect);
                updateSurfaceWindowCrop(this.mTmpClipRect, this.mTmpFinalClipRect, false);
                SurfaceControl.closeTransaction();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i(TAG, "<<< CLOSE TRANSACTION setWallpaperOffset");
                }
            } catch (RuntimeException e) {
                Slog.w(TAG, "Error positioning surface of " + this.mWin + " pos=(" + left + "," + top + ")", e);
                SurfaceControl.closeTransaction();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i(TAG, "<<< CLOSE TRANSACTION setWallpaperOffset");
                }
            }
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION setWallpaperOffset");
            }
            throw th;
        }
    }

    boolean tryChangeFormatInPlaceLocked() {
        if (this.mSurfaceController == null) {
            return false;
        }
        WindowManager.LayoutParams attrs = this.mWin.getAttrs();
        boolean isHwAccelerated = (attrs.flags & 16777216) != 0;
        int format = isHwAccelerated ? -3 : attrs.format;
        if (format != this.mSurfaceFormat) {
            return false;
        }
        setOpaqueLocked(PixelFormat.formatHasAlpha(attrs.format) ? false : true);
        return true;
    }

    void setOpaqueLocked(boolean isOpaque) {
        if (this.mSurfaceController == null) {
            return;
        }
        this.mSurfaceController.setOpaque(isOpaque);
    }

    void setSecureLocked(boolean isSecure) {
        if (this.mSurfaceController == null) {
            return;
        }
        this.mSurfaceController.setSecure(isSecure);
    }

    boolean performShowLocked() {
        if (this.mWin.isHiddenFromUserLocked()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.w(TAG, "hiding " + this.mWin + ", belonging to " + this.mWin.mOwnerUid);
            }
            this.mWin.hideLw(false);
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY || (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && this.mWin.mAttrs.type == 3)) {
            Slog.v(TAG, "performShow on " + this + ": mDrawState=" + drawStateToString() + " readyForDisplay=" + this.mWin.isReadyForDisplayIgnoringKeyguard() + " starting=" + (this.mWin.mAttrs.type == 3) + " during animation: policyVis=" + this.mWin.mPolicyVisibility + " attHidden=" + this.mWin.mAttachedHidden + " tok.hiddenRequested=" + (this.mWin.mAppToken != null ? this.mWin.mAppToken.hiddenRequested : false) + " tok.hidden=" + (this.mWin.mAppToken != null ? this.mWin.mAppToken.hidden : false) + " animating=" + this.mAnimating + " tok animating=" + (this.mAppAnimator != null ? this.mAppAnimator.animating : false) + " Callers=" + Debug.getCallers(3));
        }
        if (this.mDrawState != 3 || !this.mWin.isReadyForDisplayIgnoringKeyguard()) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY || (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && this.mWin.mAttrs.type == 3)) {
            Slog.v(TAG, "Showing " + this + " during animation: policyVis=" + this.mWin.mPolicyVisibility + " attHidden=" + this.mWin.mAttachedHidden + " tok.hiddenRequested=" + (this.mWin.mAppToken != null ? this.mWin.mAppToken.hiddenRequested : false) + " tok.hidden=" + (this.mWin.mAppToken != null ? this.mWin.mAppToken.hidden : false) + " animating=" + this.mAnimating + " tok animating=" + (this.mAppAnimator != null ? this.mAppAnimator.animating : false));
        }
        this.mService.enableScreenIfNeededLocked();
        applyEnterAnimationLocked();
        this.mLastAlpha = -1.0f;
        if (WindowManagerDebugConfig.DEBUG_SURFACE_TRACE || WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v(TAG, "performShowLocked: mDrawState=HAS_DRAWN in " + this.mWin);
        }
        this.mDrawState = 4;
        this.mService.scheduleAnimationLocked();
        int i = this.mWin.mChildWindows.size();
        while (i > 0) {
            i--;
            WindowState c = this.mWin.mChildWindows.get(i);
            if (c.mAttachedHidden) {
                c.mAttachedHidden = false;
                if (c.mWinAnimator.mSurfaceController != null) {
                    c.mWinAnimator.performShowLocked();
                    DisplayContent displayContent = c.getDisplayContent();
                    if (displayContent != null) {
                        displayContent.layoutNeeded = true;
                    }
                }
            }
        }
        if (this.mWin.mAttrs.type != 3 && this.mWin.mAppToken != null) {
            this.mWin.mAppToken.onFirstWindowDrawn(this.mWin, this);
        }
        if (this.mWin.mAttrs.type == 2011) {
            this.mWin.mDisplayContent.mDividerControllerLocked.resetImeHideRequested();
        }
        return true;
    }

    private boolean showSurfaceRobustlyLocked() {
        Task task = this.mWin.getTask();
        if (task != null && ActivityManager.StackId.windowsAreScaleable(task.mStack.mStackId)) {
            this.mSurfaceController.forceScaleableInTransaction(true);
        }
        boolean shown = this.mSurfaceController.showRobustlyInTransaction();
        if (!shown) {
            return false;
        }
        this.mService.updateNonSystemOverlayWindowsVisibilityIfNeeded(this.mWin, true);
        if (this.mWin.mTurnOnScreen) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY || !WindowManagerService.IS_USER_BUILD) {
                Slog.v(TAG, "Show surface turning screen on: " + this.mWin);
            }
            this.mWin.mTurnOnScreen = false;
            this.mAnimator.mBulkUpdateParams |= 16;
        }
        return true;
    }

    void applyEnterAnimationLocked() {
        int transit;
        if (this.mWin.mSkipEnterAnimationForSeamlessReplacement) {
            return;
        }
        if (this.mEnterAnimationPending) {
            this.mEnterAnimationPending = false;
            transit = 1;
        } else {
            transit = 3;
        }
        applyAnimationLocked(transit, true);
        if (this.mService.mAccessibilityController == null || this.mWin.getDisplayId() != 0) {
            return;
        }
        this.mService.mAccessibilityController.onWindowTransitionLocked(this.mWin, transit);
    }

    boolean applyAnimationLocked(int transit, boolean isEntrance) {
        if ((this.mLocalAnimating && this.mAnimationIsEntrance == isEntrance) || this.mKeyguardGoingAwayAnimation) {
            if (this.mAnimation != null && this.mKeyguardGoingAwayAnimation && transit == 5) {
                applyFadeoutDuringKeyguardExitAnimation();
            }
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
            if (WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation: win=" + this + " anim=" + anim + " attr=0x" + Integer.toHexString(attr) + " a=" + a + " transit=" + transit + " isEntrance=" + isEntrance + " Callers " + Debug.getCallers(3));
            }
            if (a != null) {
                if (WindowManagerDebugConfig.DEBUG_ANIM) {
                    WindowManagerService.logWithStack(TAG, "Loaded animation " + a + " for " + this);
                }
                if (this.mService.isFastStartingWindowSupport() && this.mWin.isFastStartingWindow() && transit == 5) {
                    if (this.mWmDuration != -1) {
                        a.setDuration(this.mWmDuration);
                    }
                    if (this.mWmOffset != -1) {
                        a.setStartOffset(this.mWmOffset);
                    }
                }
                setAnimation(a);
                this.mAnimationIsEntrance = isEntrance;
            }
        } else {
            clearAnimation();
        }
        if (this.mWin.mAttrs.type == 2011) {
            this.mService.adjustForImeIfNeeded(this.mWin.mDisplayContent);
            if (isEntrance) {
                this.mWin.setDisplayLayoutNeeded();
                this.mService.mWindowPlacerLocked.requestTraversal();
            }
        }
        return this.mAnimation != null;
    }

    private void applyFadeoutDuringKeyguardExitAnimation() {
        long startTime = this.mAnimation.getStartTime();
        long duration = this.mAnimation.getDuration();
        long elapsed = this.mLastAnimationTime - startTime;
        long fadeDuration = duration - elapsed;
        if (fadeDuration <= 0) {
            return;
        }
        AnimationSet newAnimation = new AnimationSet(false);
        newAnimation.setDuration(duration);
        newAnimation.setStartTime(startTime);
        newAnimation.addAnimation(this.mAnimation);
        Animation fadeOut = AnimationUtils.loadAnimation(this.mContext, R.anim.activity_translucent_close_exit);
        fadeOut.setDuration(fadeDuration);
        fadeOut.setStartOffset(elapsed);
        newAnimation.addAnimation(fadeOut);
        newAnimation.initialize(this.mWin.mFrame.width(), this.mWin.mFrame.height(), this.mAnimDx, this.mAnimDy);
        this.mAnimation = newAnimation;
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
            pw.print(this.mAnimation);
            pw.print(" mStackClip=");
            pw.println(this.mStackClip);
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
        if (this.mSurfaceController != null) {
            this.mSurfaceController.dump(pw, prefix, dumpAll);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mDrawState=");
            pw.print(drawStateToString());
            pw.print(prefix);
            pw.print(" mLastHidden=");
            pw.println(this.mLastHidden);
            pw.print(prefix);
            pw.print("mSystemDecorRect=");
            this.mSystemDecorRect.printShortString(pw);
            pw.print(" last=");
            this.mLastSystemDecorRect.printShortString(pw);
            pw.print(" mHasClipRect=");
            pw.print(this.mHasClipRect);
            pw.print(" mLastClipRect=");
            this.mLastClipRect.printShortString(pw);
            if (!this.mLastFinalClipRect.isEmpty()) {
                pw.print(" mLastFinalClipRect=");
                this.mLastFinalClipRect.printShortString(pw);
            }
            pw.println();
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
        if (!this.mAnimationStartDelayed) {
            return;
        }
        pw.print(prefix);
        pw.print("mAnimationStartDelayed=");
        pw.print(this.mAnimationStartDelayed);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("WindowStateAnimator{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.mWin.mAttrs.getTitle());
        sb.append('}');
        return sb.toString();
    }

    void reclaimSomeSurfaceMemory(String operation, boolean secure) {
        this.mService.reclaimSomeSurfaceMemoryLocked(this, operation, secure);
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
                    this.mSurfaceController.destroyInTransaction();
                }
                this.mWin.setHasSurface(false);
                this.mSurfaceController = null;
            } catch (RuntimeException e) {
                Slog.w(TAG, "Exception thrown when destroying surface " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e);
                this.mWin.setHasSurface(false);
                this.mSurfaceController = null;
            }
            this.mDrawState = 0;
        } catch (Throwable th) {
            this.mWin.setHasSurface(false);
            this.mSurfaceController = null;
            this.mDrawState = 0;
            throw th;
        }
    }

    void setMoveAnimation(int left, int top) {
        Animation a = AnimationUtils.loadAnimation(this.mContext, R.anim.submenu_exit);
        setAnimation(a);
        this.mAnimDx = this.mWin.mLastFrame.left - left;
        this.mAnimDy = this.mWin.mLastFrame.top - top;
        this.mAnimateMove = true;
    }

    void deferTransactionUntilParentFrame(long frameNumber) {
        if (!this.mWin.isChildWindow()) {
            return;
        }
        this.mDeferTransactionUntilFrame = frameNumber;
        this.mDeferTransactionTime = System.currentTimeMillis();
        if (this.mWin.mAttachedWindow.mWinAnimator.mSurfaceController == null) {
            return;
        }
        this.mSurfaceController.deferTransactionUntil(this.mWin.mAttachedWindow.mWinAnimator.mSurfaceController.getHandle(), frameNumber);
    }

    void deferToPendingTransaction() {
        if (this.mDeferTransactionUntilFrame < 0) {
            return;
        }
        long time = System.currentTimeMillis();
        if (time > this.mDeferTransactionTime + PENDING_TRANSACTION_FINISH_WAIT_TIME) {
            this.mDeferTransactionTime = -1L;
            this.mDeferTransactionUntilFrame = -1L;
        } else {
            if (this.mWin.mAttachedWindow.mWinAnimator.mSurfaceController == null) {
                return;
            }
            this.mSurfaceController.deferTransactionUntil(this.mWin.mAttachedWindow.mWinAnimator.mSurfaceController.getHandle(), this.mDeferTransactionUntilFrame);
        }
    }

    private long getAnimationFrameTime(Animation animation, long currentTime) {
        if (this.mAnimationStartDelayed) {
            animation.setStartTime(currentTime);
            return 1 + currentTime;
        }
        return currentTime;
    }

    void startDelayingAnimationStart() {
        this.mAnimationStartDelayed = true;
    }

    void endDelayingAnimationStart() {
        this.mAnimationStartDelayed = false;
    }

    void drawIfNeeded() {
        if (this.mSurfaceController == null) {
            return;
        }
        if (this.mSurfaceController.mSurfaceControl == null) {
            Slog.i(TAG, "drawIfNeeded, mSurfaceControl is released");
            return;
        }
        if (this.mDrawNeeded) {
            Slog.i(TAG, "drawIfNeeded");
            Surface mSurface = new Surface();
            try {
                mSurface.copyFrom(this.mSurfaceController.mSurfaceControl);
            } catch (Surface.OutOfResourcesException e) {
                Slog.e(TAG, "copyFrom, OutOfResourcesException");
            } catch (IllegalArgumentException e2) {
                Slog.e(TAG, "copyFrom, IllegalArgumentException");
            } catch (NullPointerException e3) {
                Slog.e(TAG, "copyFrom, NullPointerException");
            }
            if (mSurface == null) {
                Slog.i(TAG, "drawIfNeeded, mSurface is null");
                return;
            }
            int dw = (int) this.mSurfaceController.mSurfaceW;
            int dh = (int) this.mSurfaceController.mSurfaceH;
            this.mDrawNeeded = false;
            Rect dirty = new Rect(0, 0, dw, dh);
            Canvas c = null;
            try {
                try {
                    c = mSurface.lockCanvas(dirty);
                    Slog.i(TAG, "lockCanvas, mToken =" + this.mWin.mToken);
                    if (c != null && this.mWin.mToken != null) {
                        Bitmap bitmap = this.mService.getBitmapByToken(this.mWin.mToken.token);
                        if (bitmap != null) {
                            c.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
                        }
                        Slog.i(TAG, "unlockCanvasAndPost");
                        mSurface.unlockCanvasAndPost(c);
                    }
                } catch (Surface.OutOfResourcesException e4) {
                    Slog.e(TAG, "OutOfResourcesException, surface = " + mSurface + ", canvas = " + c + ", this = " + this, e4);
                    if (mSurface == null) {
                        return;
                    } else {
                        mSurface.release();
                    }
                } catch (IllegalArgumentException e5) {
                    Slog.e(TAG, "Could not unlock surface, surface = " + mSurface + ", canvas = " + c + ", this = " + this, e5);
                    if (mSurface == null) {
                        return;
                    } else {
                        mSurface.release();
                    }
                } catch (IllegalStateException e6) {
                    Slog.e(TAG, "IllegalStateException, this = " + this, e6);
                    if (mSurface == null) {
                        return;
                    } else {
                        mSurface.release();
                    }
                }
                if (mSurface != null) {
                    mSurface.release();
                }
            } catch (Throwable th) {
                if (mSurface != null) {
                    mSurface.release();
                }
                throw th;
            }
        }
    }

    void doCacheBitmap() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... para) {
                try {
                    if (WindowStateAnimator.this.mWmSleep != -1) {
                        Thread.sleep(WindowStateAnimator.this.mWmSleep);
                    } else {
                        Thread.sleep(WindowStateAnimator.PENDING_TRANSACTION_FINISH_WAIT_TIME);
                    }
                } catch (Exception e) {
                }
                Trace.traceBegin(32L, "AsyncScreenshot");
                Bitmap bmShot = SurfaceControl.screenshot(new Rect(), (int) WindowStateAnimator.this.mWin.mWinAnimator.mSurfaceController.mSurfaceW, (int) WindowStateAnimator.this.mWin.mWinAnimator.mSurfaceController.mSurfaceH, WindowStateAnimator.this.mSurfaceController.mSurfaceLayer, WindowStateAnimator.this.mSurfaceController.mSurfaceLayer, false, 0);
                Slog.i(WindowStateAnimator.TAG, "doCacheBitmap, mToken =" + WindowStateAnimator.this.mWin.mToken);
                if (bmShot != null && WindowStateAnimator.this.mWin.mToken != null) {
                    WindowStateAnimator.this.mService.setBitmapByToken(WindowStateAnimator.this.mWin.mToken.token, bmShot.copy(bmShot.getConfig(), true));
                    bmShot.recycle();
                }
                Trace.traceEnd(32L);
                return null;
            }
        };
        task.execute(new Void[0]);
    }

    void doCacheBitmap(View view) {
        if (this.mWin.mToken == null) {
            return;
        }
        IBinder token = this.mWin.mToken.token;
        Trace.traceBegin(32L, "SyncScreenshot");
        try {
            view.setDrawingCacheEnabled(true);
            Bitmap bmShot = Bitmap.createBitmap(view.getDrawingCache());
            Slog.i(TAG, "doCacheBitmap, mToken =" + token);
            if (bmShot != null && token != null) {
                this.mService.setBitmapByToken(token, bmShot.copy(bmShot.getConfig(), true));
                bmShot.recycle();
            }
        } catch (Throwable e) {
            Slog.e(TAG, "doSyncCacheBitmap, this = " + this, e);
        }
        Trace.traceEnd(32L);
    }
}
