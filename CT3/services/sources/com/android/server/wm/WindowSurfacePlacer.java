package com.android.server.wm;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Debug;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import android.view.animation.Animation;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.mediatek.multiwindow.MultiWindowManager;
import java.io.PrintWriter;
import java.util.ArrayList;

class WindowSurfacePlacer {
    static final int SET_FORCE_HIDING_CHANGED = 4;
    static final int SET_ORIENTATION_CHANGE_COMPLETE = 8;
    static final int SET_TURN_ON_SCREEN = 16;
    static final int SET_UPDATE_ROTATION = 1;
    static final int SET_WALLPAPER_ACTION_PENDING = 32;
    static final int SET_WALLPAPER_MAY_CHANGE = 2;
    private static final String TAG = "WindowManager";
    private int mLayoutRepeatCount;
    private final WindowManagerService mService;
    private boolean mTraversalScheduled;
    private final WallpaperController mWallpaperControllerLocked;
    private boolean mInLayout = false;
    boolean mWallpaperMayChange = false;
    boolean mOrientationChangeComplete = true;
    boolean mWallpaperActionPending = false;
    private boolean mWallpaperForceHidingChanged = false;
    private Object mLastWindowFreezeSource = null;
    private Session mHoldScreen = null;
    private boolean mObscured = false;
    private boolean mSyswin = false;
    private float mScreenBrightness = -1.0f;
    private float mButtonBrightness = -1.0f;
    private long mUserActivityTimeout = -1;
    private boolean mUpdateRotation = false;
    private final Rect mTmpStartRect = new Rect();
    private final Rect mTmpContentRect = new Rect();
    private boolean mDisplayHasContent = false;
    private boolean mObscureApplicationContentOnSecondaryDisplays = false;
    private float mPreferredRefreshRate = 0.0f;
    private int mPreferredModeId = 0;
    private int mDeferDepth = 0;
    private boolean mSustainedPerformanceModeEnabled = false;
    private boolean mSustainedPerformanceModeCurrent = false;
    WindowState mHoldScreenWindow = null;
    WindowState mObsuringWindow = null;
    private final LayerAndToken mTmpLayerAndToken = new LayerAndToken(null);
    private final ArrayList<SurfaceControl> mPendingDestroyingSurfaces = new ArrayList<>();

    private static final class LayerAndToken {
        public int layer;
        public AppWindowToken token;

        LayerAndToken(LayerAndToken layerAndToken) {
            this();
        }

        private LayerAndToken() {
        }
    }

    public WindowSurfacePlacer(WindowManagerService service) {
        this.mService = service;
        this.mWallpaperControllerLocked = this.mService.mWallpaperControllerLocked;
    }

    void deferLayout() {
        this.mDeferDepth++;
    }

    void continueLayout() {
        this.mDeferDepth--;
        if (this.mDeferDepth > 0) {
            return;
        }
        performSurfacePlacement();
    }

    final void performSurfacePlacement() {
        if (this.mDeferDepth > 0) {
            return;
        }
        int loopCount = 6;
        do {
            this.mTraversalScheduled = false;
            performSurfacePlacementLoop();
            this.mService.mH.removeMessages(4);
            loopCount--;
            if (!this.mTraversalScheduled) {
                break;
            }
        } while (loopCount > 0);
        this.mWallpaperActionPending = false;
    }

    private void performSurfacePlacementLoop() {
        if (this.mInLayout) {
            if (WindowManagerDebugConfig.DEBUG) {
                throw new RuntimeException("Recursive call!");
            }
            Slog.w(TAG, "performLayoutAndPlaceSurfacesLocked called while in layout. Callers=" + Debug.getCallers(3));
            return;
        }
        if (this.mService.mWaitingForConfig || !this.mService.mDisplayReady) {
            return;
        }
        Trace.traceBegin(32L, "wmLayout");
        this.mInLayout = true;
        boolean recoveringMemory = false;
        if (!this.mService.mForceRemoves.isEmpty()) {
            recoveringMemory = true;
            while (!this.mService.mForceRemoves.isEmpty()) {
                WindowState ws = this.mService.mForceRemoves.remove(0);
                Slog.i(TAG, "Force removing: " + ws);
                this.mService.removeWindowInnerLocked(ws);
            }
            Slog.w(TAG, "Due to memory failure, waiting a bit for next layout");
            Object tmp = new Object();
            synchronized (tmp) {
                try {
                    tmp.wait(250L);
                } catch (InterruptedException e) {
                }
            }
        }
        try {
            performSurfacePlacementInner(recoveringMemory);
            this.mInLayout = false;
            if (this.mService.needsLayout()) {
                int i = this.mLayoutRepeatCount + 1;
                this.mLayoutRepeatCount = i;
                if (i < 6) {
                    requestTraversal();
                } else {
                    Slog.e(TAG, "Performed 6 layouts in a row. Skipping");
                    this.mLayoutRepeatCount = 0;
                }
            } else {
                this.mLayoutRepeatCount = 0;
            }
            if (this.mService.mWindowsChanged && !this.mService.mWindowChangeListeners.isEmpty()) {
                this.mService.mH.removeMessages(19);
                this.mService.mH.sendEmptyMessage(19);
            }
        } catch (RuntimeException e2) {
            this.mInLayout = false;
            Slog.e(TAG, "Unhandled exception while laying out windows", e2);
        }
        Trace.traceEnd(32L);
    }

    void debugLayoutRepeats(String msg, int pendingLayoutChanges) {
        if (this.mLayoutRepeatCount < 4) {
            return;
        }
        Slog.v(TAG, "Layouts looping: " + msg + ", mPendingLayoutChanges = 0x" + Integer.toHexString(pendingLayoutChanges));
    }

    private void performSurfacePlacementInner(boolean recoveringMemory) {
        if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
            Slog.v(TAG, "performSurfacePlacementInner: entry. Called by " + Debug.getCallers(3));
        }
        boolean updateInputWindowsNeeded = false;
        if (this.mService.mFocusMayChange) {
            this.mService.mFocusMayChange = false;
            updateInputWindowsNeeded = this.mService.updateFocusedWindowLocked(3, false);
        }
        int numDisplays = this.mService.mDisplayContents.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = this.mService.mDisplayContents.valueAt(displayNdx);
            for (int i = displayContent.mExitingTokens.size() - 1; i >= 0; i--) {
                displayContent.mExitingTokens.get(i).hasVisible = false;
            }
        }
        for (int stackNdx = this.mService.mStackIdToStack.size() - 1; stackNdx >= 0; stackNdx--) {
            AppTokenList exitingAppTokens = this.mService.mStackIdToStack.valueAt(stackNdx).mExitingAppTokens;
            for (int tokenNdx = exitingAppTokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                exitingAppTokens.get(tokenNdx).hasVisible = false;
            }
        }
        this.mHoldScreen = null;
        this.mHoldScreenWindow = null;
        this.mObsuringWindow = null;
        this.mScreenBrightness = -1.0f;
        this.mButtonBrightness = -1.0f;
        this.mUserActivityTimeout = -1L;
        this.mObscureApplicationContentOnSecondaryDisplays = false;
        this.mSustainedPerformanceModeCurrent = false;
        this.mService.mTransactionSequence++;
        DisplayContent defaultDisplay = this.mService.getDefaultDisplayContentLocked();
        DisplayInfo defaultInfo = defaultDisplay.getDisplayInfo();
        int defaultDw = defaultInfo.logicalWidth;
        int defaultDh = defaultInfo.logicalHeight;
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i(TAG, ">>> OPEN TRANSACTION performLayoutAndPlaceSurfaces");
        }
        SurfaceControl.openTransaction();
        try {
            try {
                applySurfaceChangesTransaction(recoveringMemory, numDisplays, defaultDw, defaultDh);
            } catch (RuntimeException e) {
                Slog.wtf(TAG, "Unhandled exception in Window Manager", e);
                SurfaceControl.closeTransaction();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i(TAG, "<<< CLOSE TRANSACTION performLayoutAndPlaceSurfaces");
                }
            }
            WindowList defaultWindows = defaultDisplay.getWindowList();
            if (this.mService.mAppTransition.isReady()) {
                defaultDisplay.pendingLayoutChanges |= handleAppTransitionReadyLocked(defaultWindows);
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    debugLayoutRepeats("after handleAppTransitionReadyLocked", defaultDisplay.pendingLayoutChanges);
                }
            }
            if (!this.mService.mAnimator.mAppWindowAnimating && this.mService.mAppTransition.isRunning()) {
                defaultDisplay.pendingLayoutChanges |= this.mService.handleAnimatingStoppedAndTransitionLocked();
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    debugLayoutRepeats("after handleAnimStopAndXitionLock", defaultDisplay.pendingLayoutChanges);
                }
            }
            if (this.mWallpaperForceHidingChanged && defaultDisplay.pendingLayoutChanges == 0 && !this.mService.mAppTransition.isReady()) {
                defaultDisplay.pendingLayoutChanges |= 1;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    debugLayoutRepeats("after animateAwayWallpaperLocked", defaultDisplay.pendingLayoutChanges);
                }
            }
            this.mWallpaperForceHidingChanged = false;
            if (this.mWallpaperMayChange) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.v(TAG, "Wallpaper may change!  Adjusting");
                }
                defaultDisplay.pendingLayoutChanges |= 4;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    debugLayoutRepeats("WallpaperMayChange", defaultDisplay.pendingLayoutChanges);
                }
            }
            if (this.mService.mFocusMayChange) {
                this.mService.mFocusMayChange = false;
                if (this.mService.updateFocusedWindowLocked(2, false)) {
                    updateInputWindowsNeeded = true;
                    defaultDisplay.pendingLayoutChanges |= 8;
                }
            }
            if (this.mService.needsLayout()) {
                defaultDisplay.pendingLayoutChanges |= 1;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    debugLayoutRepeats("mLayoutNeeded", defaultDisplay.pendingLayoutChanges);
                }
            }
            for (int i2 = this.mService.mResizingWindows.size() - 1; i2 >= 0; i2--) {
                WindowState win = this.mService.mResizingWindows.get(i2);
                if (!win.mAppFreezing) {
                    if (win.mAppToken != null) {
                        win.mAppToken.destroySavedSurfaces();
                    }
                    win.reportResized();
                    this.mService.mResizingWindows.remove(i2);
                }
            }
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION && this.mService.mDisplayFrozen) {
                Slog.v(TAG, "With display frozen, orientationChangeComplete=" + this.mOrientationChangeComplete);
            }
            if (this.mOrientationChangeComplete) {
                if (this.mService.mWindowsFreezingScreen != 0) {
                    this.mService.mWindowsFreezingScreen = 0;
                    this.mService.mLastFinishedFreezeSource = this.mLastWindowFreezeSource;
                    this.mService.mH.removeMessages(11);
                }
                this.mService.stopFreezingDisplayLocked();
            }
            boolean wallpaperDestroyed = false;
            int i3 = this.mService.mDestroySurface.size();
            if (i3 > 0) {
                do {
                    i3--;
                    WindowState win2 = this.mService.mDestroySurface.get(i3);
                    win2.mDestroying = false;
                    if (this.mService.mInputMethodWindow == win2) {
                        this.mService.mInputMethodWindow = null;
                    }
                    if (this.mWallpaperControllerLocked.isWallpaperTarget(win2)) {
                        wallpaperDestroyed = true;
                    }
                    win2.destroyOrSaveSurface();
                } while (i3 > 0);
                this.mService.mDestroySurface.clear();
            }
            for (int displayNdx2 = 0; displayNdx2 < numDisplays; displayNdx2++) {
                ArrayList<WindowToken> exitingTokens = this.mService.mDisplayContents.valueAt(displayNdx2).mExitingTokens;
                for (int i4 = exitingTokens.size() - 1; i4 >= 0; i4--) {
                    WindowToken token = exitingTokens.get(i4);
                    if (!token.hasVisible) {
                        exitingTokens.remove(i4);
                        if (token.windowType == 2013) {
                            this.mWallpaperControllerLocked.removeWallpaperToken(token);
                        }
                    }
                }
            }
            for (int stackNdx2 = this.mService.mStackIdToStack.size() - 1; stackNdx2 >= 0; stackNdx2--) {
                AppTokenList exitingAppTokens2 = this.mService.mStackIdToStack.valueAt(stackNdx2).mExitingAppTokens;
                for (int i5 = exitingAppTokens2.size() - 1; i5 >= 0; i5--) {
                    AppWindowToken token2 = exitingAppTokens2.get(i5);
                    if (!token2.hasVisible && !this.mService.mClosingApps.contains(token2) && (!token2.mIsExiting || token2.allAppWindows.isEmpty())) {
                        token2.mAppAnimator.clearAnimation();
                        token2.mAppAnimator.animating = false;
                        if (WindowManagerDebugConfig.DEBUG_ADD_REMOVE || WindowManagerDebugConfig.DEBUG_TOKEN_MOVEMENT) {
                            Slog.v(TAG, "performLayout: App token exiting now removed" + token2);
                        }
                        token2.removeAppFromTaskLocked();
                    }
                }
            }
            if (wallpaperDestroyed) {
                defaultDisplay.pendingLayoutChanges |= 4;
                defaultDisplay.layoutNeeded = true;
            }
            for (int displayNdx3 = 0; displayNdx3 < numDisplays; displayNdx3++) {
                DisplayContent displayContent2 = this.mService.mDisplayContents.valueAt(displayNdx3);
                if (displayContent2.pendingLayoutChanges != 0) {
                    displayContent2.layoutNeeded = true;
                }
            }
            this.mService.mInputMonitor.updateInputWindowsLw(true);
            this.mService.setHoldScreenLocked(this.mHoldScreen);
            if (!this.mService.mDisplayFrozen) {
                if (this.mScreenBrightness < 0.0f || this.mScreenBrightness > 1.0f) {
                    this.mService.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(-1);
                } else {
                    this.mService.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(toBrightnessOverride(this.mScreenBrightness));
                }
                if (this.mButtonBrightness < 0.0f || this.mButtonBrightness > 1.0f) {
                    this.mService.mPowerManagerInternal.setButtonBrightnessOverrideFromWindowManager(-1);
                } else {
                    this.mService.mPowerManagerInternal.setButtonBrightnessOverrideFromWindowManager(toBrightnessOverride(this.mButtonBrightness));
                }
                this.mService.mPowerManagerInternal.setUserActivityTimeoutOverrideFromWindowManager(this.mUserActivityTimeout);
            }
            if (this.mSustainedPerformanceModeCurrent != this.mSustainedPerformanceModeEnabled) {
                this.mSustainedPerformanceModeEnabled = this.mSustainedPerformanceModeCurrent;
                this.mService.mPowerManagerInternal.powerHint(6, this.mSustainedPerformanceModeEnabled ? 1 : 0);
            }
            if (this.mService.mTurnOnScreen) {
                if (this.mService.mAllowTheaterModeWakeFromLayout || Settings.Global.getInt(this.mService.mContext.getContentResolver(), "theater_mode_on", 0) == 0) {
                    if (!WindowManagerService.IS_USER_BUILD || WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_POWER) {
                        Slog.v(TAG, "Turning screen on after layout!");
                    }
                    this.mService.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.wm:TURN_ON");
                }
                this.mService.mTurnOnScreen = false;
            }
            if (this.mUpdateRotation) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.d(TAG, "Performing post-rotate rotation");
                }
                if (this.mService.updateRotationUncheckedLocked(false)) {
                    this.mService.mH.sendEmptyMessage(18);
                } else {
                    this.mUpdateRotation = false;
                }
            }
            if (this.mService.mWaitingForDrawnCallback != null || (this.mOrientationChangeComplete && !defaultDisplay.layoutNeeded && !this.mUpdateRotation)) {
                this.mService.checkDrawnWindowsLocked();
            }
            int N = this.mService.mPendingRemove.size();
            if (N > 0) {
                if (this.mService.mPendingRemoveTmp.length < N) {
                    this.mService.mPendingRemoveTmp = new WindowState[N + 10];
                }
                this.mService.mPendingRemove.toArray(this.mService.mPendingRemoveTmp);
                this.mService.mPendingRemove.clear();
                DisplayContentList displayList = new DisplayContentList();
                for (int i6 = 0; i6 < N; i6++) {
                    WindowState w = this.mService.mPendingRemoveTmp[i6];
                    this.mService.removeWindowInnerLocked(w);
                    DisplayContent displayContent3 = w.getDisplayContent();
                    if (displayContent3 != null && !displayList.contains(displayContent3)) {
                        displayList.add(displayContent3);
                    }
                }
                for (DisplayContent displayContent4 : displayList) {
                    this.mService.mLayersController.assignLayersLocked(displayContent4.getWindowList());
                    displayContent4.layoutNeeded = true;
                }
            }
            for (int displayNdx4 = this.mService.mDisplayContents.size() - 1; displayNdx4 >= 0; displayNdx4--) {
                this.mService.mDisplayContents.valueAt(displayNdx4).checkForDeferredActions();
            }
            if (updateInputWindowsNeeded) {
                this.mService.mInputMonitor.updateInputWindowsLw(false);
            }
            this.mService.setFocusTaskRegionLocked();
            if (MultiWindowManager.isSupported()) {
                this.mService.showOrHideRestoreButton(this.mService.mCurrentFocus);
            }
            this.mService.enableScreenIfNeededLocked();
            this.mService.scheduleAnimationLocked();
            this.mService.mWindowPlacerLocked.destroyPendingSurfaces();
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                Slog.e(TAG, "performSurfacePlacementInner exit: animating=" + this.mService.mAnimator.isAnimating());
            }
        } finally {
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION performLayoutAndPlaceSurfaces");
            }
        }
    }

    private void applySurfaceChangesTransaction(boolean recoveringMemory, int numDisplays, int defaultDw, int defaultDh) {
        if (this.mService.mWatermark != null) {
            this.mService.mWatermark.positionSurface(defaultDw, defaultDh);
        }
        if (this.mService.mStrictModeFlash != null) {
            this.mService.mStrictModeFlash.positionSurface(defaultDw, defaultDh);
        }
        if (this.mService.mCircularDisplayMask != null) {
            this.mService.mCircularDisplayMask.positionSurface(defaultDw, defaultDh, this.mService.mRotation);
        }
        if (this.mService.mEmulatorDisplayOverlay != null) {
            this.mService.mEmulatorDisplayOverlay.positionSurface(defaultDw, defaultDh, this.mService.mRotation);
        }
        boolean focusDisplayed = false;
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = this.mService.mDisplayContents.valueAt(displayNdx);
            boolean updateAllDrawn = false;
            WindowList windows = displayContent.getWindowList();
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            int displayId = displayContent.getDisplayId();
            int dw = displayInfo.logicalWidth;
            int dh = displayInfo.logicalHeight;
            int i = displayInfo.appWidth;
            int i2 = displayInfo.appHeight;
            boolean isDefaultDisplay = displayId == 0;
            this.mDisplayHasContent = false;
            this.mPreferredRefreshRate = 0.0f;
            this.mPreferredModeId = 0;
            int repeats = 0;
            while (true) {
                repeats++;
                if (repeats > 6) {
                    Slog.w(TAG, "Animation repeat aborted after too many iterations");
                    displayContent.layoutNeeded = false;
                    break;
                }
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    debugLayoutRepeats("On entry to LockedInner", displayContent.pendingLayoutChanges);
                }
                if ((displayContent.pendingLayoutChanges & 4) != 0 && this.mWallpaperControllerLocked.adjustWallpaperWindows()) {
                    this.mService.mLayersController.assignLayersLocked(windows);
                    displayContent.layoutNeeded = true;
                }
                if (isDefaultDisplay && (displayContent.pendingLayoutChanges & 2) != 0) {
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                        Slog.v(TAG, "Computing new config from layout");
                    }
                    if (this.mService.updateOrientationFromAppTokensLocked(true)) {
                        displayContent.layoutNeeded = true;
                        this.mService.mH.sendEmptyMessage(18);
                    }
                }
                if ((displayContent.pendingLayoutChanges & 1) != 0) {
                    displayContent.layoutNeeded = true;
                }
                if (repeats < 4) {
                    performLayoutLockedInner(displayContent, repeats == 1, false);
                } else {
                    Slog.w(TAG, "Layout repeat skipped after too many iterations");
                }
                displayContent.pendingLayoutChanges = 0;
                if (isDefaultDisplay) {
                    this.mService.mPolicy.beginPostLayoutPolicyLw(dw, dh);
                    for (int i3 = windows.size() - 1; i3 >= 0; i3--) {
                        WindowState w = windows.get(i3);
                        if (w.mHasSurface) {
                            this.mService.mPolicy.applyPostLayoutPolicyLw(w, w.mAttrs, w.mAttachedWindow);
                        }
                    }
                    displayContent.pendingLayoutChanges |= this.mService.mPolicy.finishPostLayoutPolicyLw();
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                        debugLayoutRepeats("after finishPostLayoutPolicyLw", displayContent.pendingLayoutChanges);
                    }
                }
                if (displayContent.pendingLayoutChanges == 0) {
                    break;
                }
            }
            this.mObscured = false;
            this.mSyswin = false;
            displayContent.resetDimming();
            boolean someoneLosingFocus = !this.mService.mLosingFocus.isEmpty();
            for (int i4 = windows.size() - 1; i4 >= 0; i4--) {
                WindowState w2 = windows.get(i4);
                Task task = w2.getTask();
                boolean obscuredChanged = w2.mObscured != this.mObscured;
                w2.mObscured = this.mObscured;
                if (!this.mObscured) {
                    handleNotObscuredLocked(w2, displayInfo);
                }
                w2.applyDimLayerIfNeeded();
                if (isDefaultDisplay && obscuredChanged && this.mWallpaperControllerLocked.isWallpaperTarget(w2) && w2.isVisibleLw()) {
                    this.mWallpaperControllerLocked.updateWallpaperVisibility();
                }
                WindowStateAnimator winAnimator = w2.mWinAnimator;
                if (w2.hasMoved()) {
                    int left = w2.mFrame.left;
                    int top = w2.mFrame.top;
                    boolean adjustedForMinimizedDockOrIme = task != null ? !task.mStack.isAdjustedForMinimizedDockedStack() ? task.mStack.isAdjustedForIme() : true : false;
                    if ((w2.mAttrs.privateFlags & 64) == 0 && !w2.isDragResizing() && !adjustedForMinimizedDockOrIme && ((task == null || w2.getTask().mStack.hasMovementAnimations()) && !w2.mWinAnimator.mLastHidden)) {
                        winAnimator.setMoveAnimation(left, top);
                    }
                    if (this.mService.mAccessibilityController != null && displayId == 0) {
                        this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
                    }
                    try {
                        w2.mClient.moved(left, top);
                    } catch (RemoteException e) {
                    }
                    w2.mMovedByResize = false;
                }
                w2.mContentChanged = false;
                if (w2.mHasSurface) {
                    winAnimator.deferToPendingTransaction();
                    boolean committed = winAnimator.commitFinishDrawingLocked();
                    if (isDefaultDisplay && committed) {
                        if (w2.mAttrs.type == 2023) {
                            displayContent.pendingLayoutChanges |= 1;
                            if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                                debugLayoutRepeats("dream and commitFinishDrawingLocked true", displayContent.pendingLayoutChanges);
                            }
                        }
                        if ((w2.mAttrs.flags & PackageManagerService.DumpState.DUMP_DEXOPT) != 0) {
                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                                Slog.v(TAG, "First draw done in potential wallpaper target " + w2);
                            }
                            this.mWallpaperMayChange = true;
                            displayContent.pendingLayoutChanges |= 4;
                            if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                                debugLayoutRepeats("wallpaper and commitFinishDrawingLocked true", displayContent.pendingLayoutChanges);
                            }
                        }
                    }
                    if (!winAnimator.isAnimationStarting() && !winAnimator.isWaitingForOpening()) {
                        winAnimator.computeShownFrameLocked();
                    }
                    winAnimator.setSurfaceBoundariesLocked(recoveringMemory);
                }
                AppWindowToken atoken = w2.mAppToken;
                if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && atoken != null && w2 == atoken.startingWindow) {
                    Slog.d(TAG, "updateWindows: starting " + w2 + " isOnScreen=" + w2.isOnScreen() + " allDrawn=" + atoken.allDrawn + " freezingScreen=" + atoken.mAppAnimator.freezingScreen);
                }
                if (atoken != null && (!atoken.allDrawn || !atoken.allDrawnExcludingSaved || atoken.mAppAnimator.freezingScreen)) {
                    if (atoken.lastTransactionSequence != this.mService.mTransactionSequence) {
                        atoken.lastTransactionSequence = this.mService.mTransactionSequence;
                        atoken.numDrawnWindows = 0;
                        atoken.numInterestingWindows = 0;
                        atoken.numInterestingWindowsExcludingSaved = 0;
                        atoken.numDrawnWindowsExclusingSaved = 0;
                        atoken.startingDisplayed = false;
                    }
                    if (!atoken.allDrawn && w2.mightAffectAllDrawn(false)) {
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                            Slog.v(TAG, "Eval win " + w2 + ": isDrawn=" + w2.isDrawnLw() + ", isAnimationSet=" + winAnimator.isAnimationSet());
                            if (!w2.isDrawnLw()) {
                                Slog.v(TAG, "Not displayed: s=" + winAnimator.mSurfaceController + " pv=" + w2.mPolicyVisibility + " mDrawState=" + winAnimator.drawStateToString() + " ah=" + w2.mAttachedHidden + " th=" + atoken.hiddenRequested + " a=" + winAnimator.mAnimating);
                            }
                        }
                        if (w2 != atoken.startingWindow) {
                            if (w2.isInteresting()) {
                                atoken.numInterestingWindows++;
                                if (w2.isDrawnLw()) {
                                    atoken.numDrawnWindows++;
                                    if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                                        Slog.v(TAG, "tokenMayBeDrawn: " + atoken + " w=" + w2 + " numInteresting=" + atoken.numInterestingWindows + " freezingScreen=" + atoken.mAppAnimator.freezingScreen + " mAppFreezing=" + w2.mAppFreezing);
                                    }
                                    updateAllDrawn = true;
                                }
                            }
                        } else if (w2.isDrawnLw()) {
                            this.mService.mH.sendEmptyMessage(50);
                            atoken.startingDisplayed = true;
                        }
                    }
                    if (!atoken.allDrawnExcludingSaved && w2.mightAffectAllDrawn(true) && w2 != atoken.startingWindow && w2.isInteresting()) {
                        atoken.numInterestingWindowsExcludingSaved++;
                        if (w2.isDrawnLw() && !w2.isAnimatingWithSavedSurface()) {
                            atoken.numDrawnWindowsExclusingSaved++;
                            if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                                Slog.v(TAG, "tokenMayBeDrawnExcludingSaved: " + atoken + " w=" + w2 + " numInteresting=" + atoken.numInterestingWindowsExcludingSaved + " freezingScreen=" + atoken.mAppAnimator.freezingScreen + " mAppFreezing=" + w2.mAppFreezing);
                            }
                            updateAllDrawn = true;
                        }
                    }
                }
                if (isDefaultDisplay && someoneLosingFocus && w2 == this.mService.mCurrentFocus && w2.isDisplayedLw()) {
                    focusDisplayed = true;
                }
                this.mService.updateResizingWindows(w2);
            }
            this.mService.mDisplayManagerInternal.setDisplayProperties(displayId, this.mDisplayHasContent, this.mPreferredRefreshRate, this.mPreferredModeId, true);
            this.mService.getDisplayContentLocked(displayId).stopDimmingIfNeeded();
            if (updateAllDrawn) {
                updateAllDrawnLocked(displayContent);
            }
        }
        if (focusDisplayed) {
            this.mService.mH.sendEmptyMessage(3);
        }
        this.mService.mDisplayManagerInternal.performTraversalInTransactionFromWindowManager();
    }

    boolean isInLayout() {
        return this.mInLayout;
    }

    final void performLayoutLockedInner(DisplayContent displayContent, boolean initial, boolean updateInputWindows) {
        if (displayContent.layoutNeeded) {
            displayContent.layoutNeeded = false;
            WindowList windows = displayContent.getWindowList();
            boolean isDefaultDisplay = displayContent.isDefaultDisplay;
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            int dw = displayInfo.logicalWidth;
            int dh = displayInfo.logicalHeight;
            if (this.mService.mInputConsumer != null) {
                this.mService.mInputConsumer.layout(dw, dh);
            }
            if (this.mService.mWallpaperInputConsumer != null) {
                this.mService.mWallpaperInputConsumer.layout(dw, dh);
            }
            int N = windows.size();
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v(TAG, "-------------------------------------");
                Slog.v(TAG, "performLayout: needed=" + displayContent.layoutNeeded + " dw=" + dw + " dh=" + dh);
            }
            this.mService.mPolicy.beginLayoutLw(isDefaultDisplay, dw, dh, this.mService.mRotation, this.mService.mCurConfiguration.uiMode);
            if (isDefaultDisplay) {
                this.mService.mSystemDecorLayer = this.mService.mPolicy.getSystemDecorLayerLw();
                this.mService.mScreenRect.set(0, 0, dw, dh);
            }
            this.mService.mPolicy.getContentRectLw(this.mTmpContentRect);
            displayContent.resize(this.mTmpContentRect);
            int seq = this.mService.mLayoutSeq + 1;
            if (seq < 0) {
                seq = 0;
            }
            this.mService.mLayoutSeq = seq;
            boolean behindDream = false;
            int topAttached = -1;
            for (int i = N - 1; i >= 0; i--) {
                WindowState win = windows.get(i);
                boolean zIsGoneForLayoutLw = (behindDream && this.mService.mPolicy.canBeForceHidden(win, win.mAttrs)) ? true : win.isGoneForLayoutLw();
                if (WindowManagerDebugConfig.DEBUG_LAYOUT && !win.mLayoutAttached) {
                    Slog.v(TAG, "1ST PASS " + win + ": gone=" + zIsGoneForLayoutLw + " mHaveFrame=" + win.mHaveFrame + " mLayoutAttached=" + win.mLayoutAttached + " screen changed=" + win.isConfigChanged());
                    AppWindowToken atoken = win.mAppToken;
                    if (zIsGoneForLayoutLw) {
                        Slog.v(TAG, "  GONE: mViewVisibility=" + win.mViewVisibility + " mRelayoutCalled=" + win.mRelayoutCalled + " hidden=" + win.mRootToken.hidden + " hiddenRequested=" + (atoken != null ? atoken.hiddenRequested : false) + " mAttachedHidden=" + win.mAttachedHidden);
                    } else {
                        Slog.v(TAG, "  VIS: mViewVisibility=" + win.mViewVisibility + " mRelayoutCalled=" + win.mRelayoutCalled + " hidden=" + win.mRootToken.hidden + " hiddenRequested=" + (atoken != null ? atoken.hiddenRequested : false) + " mAttachedHidden=" + win.mAttachedHidden);
                    }
                }
                if (!zIsGoneForLayoutLw || !win.mHaveFrame || win.mLayoutNeeded || ((win.isConfigChanged() || win.setInsetsChanged()) && !win.isGoneForLayoutLw() && ((win.mAttrs.privateFlags & 1024) != 0 || (win.mHasSurface && win.mAppToken != null && win.mAppToken.layoutConfigChanges)))) {
                    if (!win.mLayoutAttached) {
                        if (initial) {
                            win.mContentChanged = false;
                        }
                        if (win.mAttrs.type == 2023) {
                            behindDream = true;
                        }
                        win.mLayoutNeeded = false;
                        win.prelayout();
                        this.mService.mPolicy.layoutWindowLw(win, (WindowManagerPolicy.WindowState) null);
                        win.mLayoutSeq = seq;
                        Task task = win.getTask();
                        if (task != null) {
                            displayContent.mDimLayerController.updateDimLayer(task);
                        }
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            Slog.v(TAG, "  LAYOUT: mFrame=" + win.mFrame + " mContainingFrame=" + win.mContainingFrame + " mDisplayFrame=" + win.mDisplayFrame);
                        }
                    } else if (topAttached < 0) {
                        topAttached = i;
                    }
                }
            }
            boolean attachedBehindDream = false;
            for (int i2 = topAttached; i2 >= 0; i2--) {
                WindowState win2 = windows.get(i2);
                if (win2.mLayoutAttached) {
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                        Slog.v(TAG, "2ND PASS " + win2 + " mHaveFrame=" + win2.mHaveFrame + " mViewVisibility=" + win2.mViewVisibility + " mRelayoutCalled=" + win2.mRelayoutCalled);
                    }
                    if ((!attachedBehindDream || !this.mService.mPolicy.canBeForceHidden(win2, win2.mAttrs)) && ((win2.mViewVisibility != 8 && win2.mRelayoutCalled) || !win2.mHaveFrame || win2.mLayoutNeeded)) {
                        if (initial) {
                            win2.mContentChanged = false;
                        }
                        win2.mLayoutNeeded = false;
                        win2.prelayout();
                        this.mService.mPolicy.layoutWindowLw(win2, win2.mAttachedWindow);
                        win2.mLayoutSeq = seq;
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            Slog.v(TAG, "  LAYOUT: mFrame=" + win2.mFrame + " mContainingFrame=" + win2.mContainingFrame + " mDisplayFrame=" + win2.mDisplayFrame);
                        }
                    }
                } else if (win2.mAttrs.type == 2023) {
                    attachedBehindDream = behindDream;
                }
            }
            this.mService.mInputMonitor.setUpdateInputWindowsNeededLw();
            if (updateInputWindows) {
                this.mService.mInputMonitor.updateInputWindowsLw(false);
            }
            this.mService.mPolicy.finishLayoutLw();
            this.mService.mH.sendEmptyMessage(41);
        }
    }

    private int handleAppTransitionReadyLocked(WindowList windows) {
        AppWindowToken appWindowToken;
        AppWindowToken upperWallpaperAppToken;
        AppWindowToken wtoken;
        WindowState ws;
        int appsCount = this.mService.mOpeningApps.size();
        if (!transitionGoodToGo(appsCount)) {
            return 0;
        }
        Trace.traceBegin(4128L, "animation start");
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v(TAG, "**** GOOD TO GO");
        }
        Trace.traceEnd(4128L);
        int transit = this.mService.mAppTransition.getAppTransition();
        if (this.mService.mSkipAppTransitionAnimation) {
            transit = -1;
        }
        this.mService.mSkipAppTransitionAnimation = false;
        this.mService.mNoAnimationNotifyOnTransitionFinished.clear();
        this.mService.mH.removeMessages(13);
        this.mService.rebuildAppWindowListLocked();
        this.mWallpaperMayChange = false;
        WindowManager.LayoutParams animLp = null;
        int bestAnimLayer = -1;
        boolean fullscreenAnim = false;
        boolean voiceInteraction = false;
        WindowState lowerWallpaperTarget = this.mWallpaperControllerLocked.getLowerWallpaperTarget();
        WindowState upperWallpaperTarget = this.mWallpaperControllerLocked.getUpperWallpaperTarget();
        boolean openingAppHasWallpaper = false;
        boolean closingAppHasWallpaper = false;
        if (lowerWallpaperTarget == null) {
            upperWallpaperAppToken = null;
            appWindowToken = null;
        } else {
            appWindowToken = lowerWallpaperTarget.mAppToken;
            upperWallpaperAppToken = upperWallpaperTarget.mAppToken;
        }
        int closingAppsCount = this.mService.mClosingApps.size();
        int appsCount2 = closingAppsCount + this.mService.mOpeningApps.size();
        for (int i = 0; i < appsCount2; i++) {
            if (i < closingAppsCount) {
                wtoken = this.mService.mClosingApps.valueAt(i);
                if (wtoken == appWindowToken || wtoken == upperWallpaperAppToken) {
                    closingAppHasWallpaper = true;
                }
            } else {
                wtoken = this.mService.mOpeningApps.valueAt(i - closingAppsCount);
                if (wtoken == appWindowToken || wtoken == upperWallpaperAppToken) {
                    openingAppHasWallpaper = true;
                }
            }
            voiceInteraction |= wtoken.voiceInteraction;
            if (wtoken.appFullscreen) {
                WindowState ws2 = wtoken.findMainWindow();
                if (ws2 != null) {
                    animLp = ws2.mAttrs;
                    bestAnimLayer = ws2.mLayer;
                    fullscreenAnim = true;
                }
            } else if (!fullscreenAnim && (ws = wtoken.findMainWindow()) != null && ws.mLayer > bestAnimLayer) {
                animLp = ws.mAttrs;
                bestAnimLayer = ws.mLayer;
            }
        }
        int transit2 = maybeUpdateTransitToWallpaper(transit, openingAppHasWallpaper, closingAppHasWallpaper, lowerWallpaperTarget, upperWallpaperTarget);
        if (!this.mService.mPolicy.allowAppAnimationsLw()) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "Animations disallowed by keyguard or dream.");
            }
            animLp = null;
        }
        processApplicationsAnimatingInPlace(transit2);
        this.mTmpLayerAndToken.token = null;
        handleClosingApps(transit2, animLp, voiceInteraction, this.mTmpLayerAndToken);
        AppWindowToken topClosingApp = this.mTmpLayerAndToken.token;
        int topClosingLayer = this.mTmpLayerAndToken.layer;
        AppWindowToken topOpeningApp = handleOpeningApps(transit2, animLp, voiceInteraction, topClosingLayer);
        this.mService.mAppTransition.goodToGo(topOpeningApp == null ? null : topOpeningApp.mAppAnimator, topClosingApp == null ? null : topClosingApp.mAppAnimator, this.mService.mOpeningApps, this.mService.mClosingApps);
        this.mService.mAppTransition.postAnimationCallback();
        this.mService.mAppTransition.clear();
        this.mService.mOpeningApps.clear();
        this.mService.mClosingApps.clear();
        this.mService.getDefaultDisplayContentLocked().layoutNeeded = true;
        if (windows == this.mService.getDefaultWindowListLocked() && !this.mService.moveInputMethodWindowsIfNeededLocked(true)) {
            this.mService.mLayersController.assignLayersLocked(windows);
        }
        this.mService.updateFocusedWindowLocked(2, true);
        this.mService.mFocusMayChange = false;
        this.mService.notifyActivityDrawnForKeyguard();
        return 3;
    }

    private AppWindowToken handleOpeningApps(int transit, WindowManager.LayoutParams animLp, boolean voiceInteraction, int topClosingLayer) {
        AppWindowToken topOpeningApp = null;
        int appsCount = this.mService.mOpeningApps.size();
        for (int i = 0; i < appsCount; i++) {
            AppWindowToken wtoken = this.mService.mOpeningApps.valueAt(i);
            AppWindowAnimator appAnimator = wtoken.mAppAnimator;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "Now opening app" + wtoken);
            }
            if (!appAnimator.usingTransferredAnimation) {
                appAnimator.clearThumbnail();
                appAnimator.setNullAnimation();
            }
            wtoken.inPendingTransaction = false;
            if (!this.mService.setTokenVisibilityLocked(wtoken, animLp, true, transit, false, voiceInteraction)) {
                this.mService.mNoAnimationNotifyOnTransitionFinished.add(wtoken.token);
            }
            wtoken.updateReportedVisibilityLocked();
            wtoken.waitingToShow = false;
            appAnimator.mAllAppWinAnimators.clear();
            int windowsCount = wtoken.allAppWindows.size();
            for (int j = 0; j < windowsCount; j++) {
                appAnimator.mAllAppWinAnimators.add(wtoken.allAppWindows.get(j).mWinAnimator);
            }
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(TAG, ">>> OPEN TRANSACTION handleAppTransitionReadyLocked()");
            }
            SurfaceControl.openTransaction();
            try {
                this.mService.mAnimator.orAnimating(appAnimator.showAllWindowsLocked());
                this.mService.mAnimator.mAppWindowAnimating |= appAnimator.isAnimating();
                int topOpeningLayer = 0;
                if (animLp != null) {
                    int layer = -1;
                    for (int j2 = 0; j2 < wtoken.allAppWindows.size(); j2++) {
                        WindowState win = wtoken.allAppWindows.get(j2);
                        if (!win.mWillReplaceWindow && !win.mRemoveOnExit) {
                            win.mAnimatingExit = false;
                            win.mWinAnimator.mAnimating = false;
                        }
                        if (win.mWinAnimator.mAnimLayer > layer) {
                            layer = win.mWinAnimator.mAnimLayer;
                        }
                    }
                    if (topOpeningApp == null || layer > 0) {
                        topOpeningApp = wtoken;
                        topOpeningLayer = layer;
                    }
                }
                if (this.mService.mAppTransition.isNextAppTransitionThumbnailUp()) {
                    createThumbnailAppAnimator(transit, wtoken, topOpeningLayer, topClosingLayer);
                }
            } finally {
                SurfaceControl.closeTransaction();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i(TAG, "<<< CLOSE TRANSACTION handleAppTransitionReadyLocked()");
                }
            }
        }
        return topOpeningApp;
    }

    private void handleClosingApps(int transit, WindowManager.LayoutParams animLp, boolean voiceInteraction, LayerAndToken layerAndToken) {
        int appsCount = this.mService.mClosingApps.size();
        for (int i = 0; i < appsCount; i++) {
            AppWindowToken wtoken = this.mService.mClosingApps.valueAt(i);
            wtoken.markSavedSurfaceExiting();
            AppWindowAnimator appAnimator = wtoken.mAppAnimator;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "Now closing app " + wtoken);
            }
            appAnimator.clearThumbnail();
            appAnimator.setNullAnimation();
            wtoken.inPendingTransaction = false;
            this.mService.setTokenVisibilityLocked(wtoken, animLp, false, transit, false, voiceInteraction);
            wtoken.updateReportedVisibilityLocked();
            wtoken.allDrawn = true;
            wtoken.deferClearAllDrawn = false;
            if (wtoken.startingWindow != null && !wtoken.startingWindow.mAnimatingExit) {
                this.mService.scheduleRemoveStartingWindowLocked(wtoken);
            }
            this.mService.mAnimator.mAppWindowAnimating |= appAnimator.isAnimating();
            if (animLp != null) {
                int layer = -1;
                for (int j = 0; j < wtoken.windows.size(); j++) {
                    WindowState win = wtoken.windows.get(j);
                    if (win.mWinAnimator.mAnimLayer > layer) {
                        layer = win.mWinAnimator.mAnimLayer;
                    }
                }
                if (layerAndToken.token == null || layer > layerAndToken.layer) {
                    layerAndToken.token = wtoken;
                    layerAndToken.layer = layer;
                }
            }
            if (this.mService.mAppTransition.isNextAppTransitionThumbnailDown()) {
                createThumbnailAppAnimator(transit, wtoken, 0, layerAndToken.layer);
            }
        }
    }

    private boolean transitionGoodToGo(int appsCount) {
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v(TAG, "Checking " + appsCount + " opening apps (frozen=" + this.mService.mDisplayFrozen + " timeout=" + this.mService.mAppTransition.isTimeout() + ")...");
        }
        int reason = 3;
        if (this.mService.mAppTransition.isTimeout()) {
            this.mService.mH.obtainMessage(47, 3, 0).sendToTarget();
            return true;
        }
        for (int i = 0; i < appsCount; i++) {
            AppWindowToken wtoken = this.mService.mOpeningApps.valueAt(i);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "Check opening app=" + wtoken + ": allDrawn=" + wtoken.allDrawn + " startingDisplayed=" + wtoken.startingDisplayed + " startingMoved=" + wtoken.startingMoved + " isRelaunching()=" + wtoken.isRelaunching());
            }
            if (wtoken.isRelaunching()) {
                return false;
            }
            boolean drawnBeforeRestoring = wtoken.allDrawn;
            wtoken.restoreSavedSurfaces();
            if (!wtoken.allDrawn && !wtoken.startingDisplayed && !wtoken.startingMoved) {
                return false;
            }
            reason = wtoken.allDrawn ? drawnBeforeRestoring ? 2 : 0 : 1;
        }
        if (this.mService.mAppTransition.isFetchingAppTransitionsSpecs()) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "isFetchingAppTransitionSpecs=true");
            }
            return false;
        }
        boolean wallpaperReady = this.mWallpaperControllerLocked.isWallpaperVisible() ? this.mWallpaperControllerLocked.wallpaperTransitionReady() : true;
        if (!wallpaperReady) {
            return false;
        }
        this.mService.mH.obtainMessage(47, reason, 0).sendToTarget();
        return true;
    }

    private int maybeUpdateTransitToWallpaper(int transit, boolean openingAppHasWallpaper, boolean closingAppHasWallpaper, WindowState lowerWallpaperTarget, WindowState upperWallpaperTarget) {
        WindowState wallpaperTarget = this.mWallpaperControllerLocked.getWallpaperTarget();
        WindowState windowState = this.mWallpaperControllerLocked.isWallpaperTargetAnimating() ? null : wallpaperTarget;
        ArraySet<AppWindowToken> openingApps = this.mService.mOpeningApps;
        ArraySet<AppWindowToken> closingApps = this.mService.mClosingApps;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v(TAG, "New wallpaper target=" + wallpaperTarget + ", oldWallpaper=" + windowState + ", lower target=" + lowerWallpaperTarget + ", upper target=" + upperWallpaperTarget + ", openingApps=" + openingApps + ", closingApps=" + closingApps);
        }
        this.mService.mAnimateWallpaperWithTarget = false;
        if (closingAppHasWallpaper && openingAppHasWallpaper) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "Wallpaper animation!");
            }
            switch (transit) {
                case 6:
                case 8:
                case 10:
                    transit = 14;
                    break;
                case 7:
                case 9:
                case 11:
                    transit = 15;
                    break;
            }
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "New transit: " + AppTransition.appTransitionToString(transit));
            }
        } else if (windowState != null && !this.mService.mOpeningApps.isEmpty() && !openingApps.contains(windowState.mAppToken) && closingApps.contains(windowState.mAppToken)) {
            transit = 12;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "New transit away from wallpaper: " + AppTransition.appTransitionToString(12));
            }
        } else if (wallpaperTarget != null && wallpaperTarget.isVisibleLw() && openingApps.contains(wallpaperTarget.mAppToken)) {
            transit = 13;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.v(TAG, "New transit into wallpaper: " + AppTransition.appTransitionToString(13));
            }
        } else {
            this.mService.mAnimateWallpaperWithTarget = true;
        }
        return transit;
    }

    private void handleNotObscuredLocked(WindowState w, DisplayInfo dispInfo) {
        WindowManager.LayoutParams attrs = w.mAttrs;
        int attrFlags = attrs.flags;
        boolean canBeSeen = w.isDisplayedLw();
        int privateflags = attrs.privateFlags;
        if (canBeSeen && w.isObscuringFullscreen(dispInfo)) {
            if (!this.mObscured) {
                this.mObsuringWindow = w;
            }
            this.mObscured = true;
        }
        if (!w.mHasSurface) {
            return;
        }
        if ((attrFlags & 128) != 0) {
            this.mHoldScreen = w.mSession;
            this.mHoldScreenWindow = w;
        } else if (WindowManagerDebugConfig.DEBUG_KEEP_SCREEN_ON && w == this.mService.mLastWakeLockHoldingWindow) {
            Slog.d("DebugKeepScreenOn", "handleNotObscuredLocked: " + w + " was holding screen wakelock but no longer has FLAG_KEEP_SCREEN_ON!!! called by" + Debug.getCallers(10));
        }
        if (!this.mSyswin && w.mAttrs.screenBrightness >= 0.0f && this.mScreenBrightness < 0.0f) {
            this.mScreenBrightness = w.mAttrs.screenBrightness;
        }
        if (!this.mSyswin && w.mAttrs.buttonBrightness >= 0.0f && this.mButtonBrightness < 0.0f) {
            this.mButtonBrightness = w.mAttrs.buttonBrightness;
        }
        if (!this.mSyswin && w.mAttrs.userActivityTimeout >= 0 && this.mUserActivityTimeout < 0) {
            this.mUserActivityTimeout = w.mAttrs.userActivityTimeout;
        }
        int type = attrs.type;
        if (canBeSeen && (type == 2008 || type == 2010 || (attrs.privateFlags & 1024) != 0)) {
            this.mSyswin = true;
        }
        if (!canBeSeen) {
            return;
        }
        DisplayContent displayContent = w.getDisplayContent();
        if (displayContent != null && displayContent.isDefaultDisplay) {
            if (type == 2023 || (attrs.privateFlags & 1024) != 0) {
                this.mObscureApplicationContentOnSecondaryDisplays = true;
            }
            this.mDisplayHasContent = true;
        } else if (displayContent != null && (!this.mObscureApplicationContentOnSecondaryDisplays || (this.mObscured && type == 2009))) {
            this.mDisplayHasContent = true;
        }
        if (this.mPreferredRefreshRate == 0.0f && w.mAttrs.preferredRefreshRate != 0.0f) {
            this.mPreferredRefreshRate = w.mAttrs.preferredRefreshRate;
        }
        if (this.mPreferredModeId == 0 && w.mAttrs.preferredDisplayModeId != 0) {
            this.mPreferredModeId = w.mAttrs.preferredDisplayModeId;
        }
        if ((262144 & privateflags) == 0) {
            return;
        }
        this.mSustainedPerformanceModeCurrent = true;
    }

    private void updateAllDrawnLocked(DisplayContent displayContent) {
        int numInteresting;
        int numInteresting2;
        ArrayList<TaskStack> stacks = displayContent.getStacks();
        for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
            ArrayList<Task> tasks = stacks.get(stackNdx).getTasks();
            for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
                for (int tokenNdx = tokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                    AppWindowToken wtoken = tokens.get(tokenNdx);
                    if (!wtoken.allDrawn && (numInteresting2 = wtoken.numInterestingWindows) > 0 && wtoken.numDrawnWindows >= numInteresting2) {
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(TAG, "allDrawn: " + wtoken + " interesting=" + numInteresting2 + " drawn=" + wtoken.numDrawnWindows);
                        }
                        wtoken.allDrawn = true;
                        displayContent.layoutNeeded = true;
                        this.mService.mH.obtainMessage(32, wtoken.token).sendToTarget();
                    }
                    if (!wtoken.allDrawnExcludingSaved && (numInteresting = wtoken.numInterestingWindowsExcludingSaved) > 0 && wtoken.numDrawnWindowsExclusingSaved >= numInteresting) {
                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(TAG, "allDrawnExcludingSaved: " + wtoken + " interesting=" + numInteresting + " drawn=" + wtoken.numDrawnWindowsExclusingSaved);
                        }
                        wtoken.allDrawnExcludingSaved = true;
                        displayContent.layoutNeeded = true;
                        if (wtoken.isAnimatingInvisibleWithSavedSurface() && !this.mService.mFinishedEarlyAnim.contains(wtoken)) {
                            this.mService.mFinishedEarlyAnim.add(wtoken);
                        }
                    }
                }
            }
        }
    }

    private static int toBrightnessOverride(float value) {
        return (int) (255.0f * value);
    }

    private void processApplicationsAnimatingInPlace(int transit) {
        WindowState win;
        if (transit != 17 || (win = this.mService.findFocusedWindowLocked(this.mService.getDefaultDisplayContentLocked())) == null) {
            return;
        }
        AppWindowToken wtoken = win.mAppToken;
        AppWindowAnimator appAnimator = wtoken.mAppAnimator;
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v(TAG, "Now animating app in place " + wtoken);
        }
        appAnimator.clearThumbnail();
        appAnimator.setNullAnimation();
        this.mService.updateTokenInPlaceLocked(wtoken, transit);
        wtoken.updateReportedVisibilityLocked();
        appAnimator.mAllAppWinAnimators.clear();
        int N = wtoken.allAppWindows.size();
        for (int j = 0; j < N; j++) {
            appAnimator.mAllAppWinAnimators.add(wtoken.allAppWindows.get(j).mWinAnimator);
        }
        this.mService.mAnimator.mAppWindowAnimating |= appAnimator.isAnimating();
        this.mService.mAnimator.orAnimating(appAnimator.showAllWindowsLocked());
    }

    private void createThumbnailAppAnimator(int transit, AppWindowToken appToken, int openingLayer, int closingLayer) {
        Animation anim;
        AppWindowAnimator openingAppAnimator = appToken == null ? null : appToken.mAppAnimator;
        if (openingAppAnimator == null || openingAppAnimator.animation == null) {
            return;
        }
        int taskId = appToken.mTask.mTaskId;
        Bitmap thumbnailHeader = this.mService.mAppTransition.getAppTransitionThumbnailHeader(taskId);
        if (thumbnailHeader == null || thumbnailHeader.getConfig() == Bitmap.Config.ALPHA_8) {
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                Slog.d(TAG, "No thumbnail header bitmap for: " + taskId);
                return;
            }
            return;
        }
        Rect dirty = new Rect(0, 0, thumbnailHeader.getWidth(), thumbnailHeader.getHeight());
        try {
            DisplayContent displayContent = this.mService.getDefaultDisplayContentLocked();
            Display display = displayContent.getDisplay();
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            SurfaceControl surfaceControl = new SurfaceControl(this.mService.mFxSession, "thumbnail anim", dirty.width(), dirty.height(), -3, 4);
            surfaceControl.setLayerStack(display.getLayerStack());
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                Slog.i(TAG, "  THUMBNAIL " + surfaceControl + ": CREATE");
            }
            Surface drawSurface = new Surface();
            drawSurface.copyFrom(surfaceControl);
            Canvas c = drawSurface.lockCanvas(dirty);
            c.drawBitmap(thumbnailHeader, 0.0f, 0.0f, (Paint) null);
            drawSurface.unlockCanvasAndPost(c);
            drawSurface.release();
            if (this.mService.mAppTransition.isNextThumbnailTransitionAspectScaled()) {
                WindowState win = appToken.findMainWindow();
                Rect appRect = win != null ? win.getContentFrameLw() : new Rect(0, 0, displayInfo.appWidth, displayInfo.appHeight);
                anim = this.mService.mAppTransition.createThumbnailAspectScaleAnimationLocked(appRect, win != null ? win.mContentInsets : null, thumbnailHeader, taskId, this.mService.mCurConfiguration.uiMode, this.mService.mCurConfiguration.orientation);
                openingAppAnimator.thumbnailForceAboveLayer = Math.max(openingLayer, closingLayer);
                openingAppAnimator.deferThumbnailDestruction = !this.mService.mAppTransition.isNextThumbnailTransitionScaleUp();
            } else {
                anim = this.mService.mAppTransition.createThumbnailScaleAnimationLocked(displayInfo.appWidth, displayInfo.appHeight, transit, thumbnailHeader);
            }
            anim.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            anim.scaleCurrentDuration(this.mService.getTransitionAnimationScaleLocked());
            openingAppAnimator.thumbnail = surfaceControl;
            openingAppAnimator.thumbnailLayer = openingLayer;
            openingAppAnimator.thumbnailAnimation = anim;
            this.mService.mAppTransition.getNextAppTransitionStartRect(taskId, this.mTmpStartRect);
        } catch (Surface.OutOfResourcesException e) {
            Slog.e(TAG, "Can't allocate thumbnail/Canvas surface w=" + dirty.width() + " h=" + dirty.height(), e);
            openingAppAnimator.clearThumbnail();
        }
    }

    boolean copyAnimToLayoutParamsLocked() {
        boolean doRequest = false;
        int bulkUpdateParams = this.mService.mAnimator.mBulkUpdateParams;
        if ((bulkUpdateParams & 1) != 0) {
            this.mUpdateRotation = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 2) != 0) {
            this.mWallpaperMayChange = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 4) != 0) {
            this.mWallpaperForceHidingChanged = true;
            doRequest = true;
        }
        if ((bulkUpdateParams & 8) == 0) {
            this.mOrientationChangeComplete = false;
        } else {
            this.mOrientationChangeComplete = true;
            this.mLastWindowFreezeSource = this.mService.mAnimator.mLastWindowFreezeSource;
            if (this.mService.mWindowsFreezingScreen != 0) {
                doRequest = true;
            }
        }
        if ((bulkUpdateParams & 16) != 0) {
            this.mService.mTurnOnScreen = true;
        }
        if ((bulkUpdateParams & 32) != 0) {
            this.mWallpaperActionPending = true;
        }
        return doRequest;
    }

    void requestTraversal() {
        if (this.mTraversalScheduled) {
            return;
        }
        this.mTraversalScheduled = true;
        this.mService.mH.sendEmptyMessage(4);
    }

    void destroyAfterTransaction(SurfaceControl surface) {
        this.mPendingDestroyingSurfaces.add(surface);
    }

    void destroyPendingSurfaces() {
        for (int i = this.mPendingDestroyingSurfaces.size() - 1; i >= 0; i--) {
            this.mPendingDestroyingSurfaces.get(i).destroy();
        }
        this.mPendingDestroyingSurfaces.clear();
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mTraversalScheduled=");
        pw.println(this.mTraversalScheduled);
        pw.print(prefix);
        pw.print("mHoldScreenWindow=");
        pw.println(this.mHoldScreenWindow);
        pw.print(prefix);
        pw.print("mObsuringWindow=");
        pw.println(this.mObsuringWindow);
    }
}
