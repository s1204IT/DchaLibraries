package com.android.server.wm;

import android.content.Context;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Choreographer;
import android.view.SurfaceControl;
import android.view.WindowManagerPolicy;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;

public class WindowAnimator {
    static final int KEYGUARD_ANIMATING_OUT = 2;
    private static final long KEYGUARD_ANIM_TIMEOUT_MS = 1000;
    static final int KEYGUARD_NOT_SHOWN = 0;
    static final int KEYGUARD_SHOWN = 1;
    private static final String TAG = "WindowManager";
    private int mAnimTransactionSequence;
    private boolean mAnimating;
    boolean mAppWindowAnimating;
    final Context mContext;
    long mCurrentTime;
    boolean mKeyguardGoingAway;
    int mKeyguardGoingAwayFlags;
    Object mLastWindowFreezeSource;
    final WindowManagerPolicy mPolicy;
    Animation mPostKeyguardExitAnimation;
    final WindowManagerService mService;
    private final WindowSurfacePlacer mWindowPlacerLocked;
    WindowState mWindowDetachedWallpaper = null;
    int mBulkUpdateParams = 0;
    SparseArray<DisplayContentsAnimator> mDisplayContentsAnimators = new SparseArray<>(2);
    boolean mInitialized = false;
    int mForceHiding = 0;
    private boolean mRemoveReplacedWindows = false;
    final Choreographer.FrameCallback mAnimationFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNs) {
            synchronized (WindowAnimator.this.mService.mWindowMap) {
                WindowAnimator.this.mService.mAnimationScheduled = false;
                Trace.traceBegin(32L, "wmAnimate");
                WindowAnimator.this.animateLocked(frameTimeNs);
                Trace.traceEnd(32L);
            }
        }
    };

    private String forceHidingToString() {
        switch (this.mForceHiding) {
            case 0:
                return "KEYGUARD_NOT_SHOWN";
            case 1:
                return "KEYGUARD_SHOWN";
            case 2:
                return "KEYGUARD_ANIMATING_OUT";
            default:
                return "KEYGUARD STATE UNKNOWN " + this.mForceHiding;
        }
    }

    WindowAnimator(WindowManagerService service) {
        this.mService = service;
        this.mContext = service.mContext;
        this.mPolicy = service.mPolicy;
        this.mWindowPlacerLocked = service.mWindowPlacerLocked;
    }

    void addDisplayLocked(int displayId) {
        getDisplayContentsAnimatorLocked(displayId);
        if (displayId != 0) {
            return;
        }
        this.mInitialized = true;
    }

    void removeDisplayLocked(int displayId) {
        DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.get(displayId);
        if (displayAnimator != null && displayAnimator.mScreenRotationAnimation != null) {
            displayAnimator.mScreenRotationAnimation.kill();
            displayAnimator.mScreenRotationAnimation = null;
        }
        this.mDisplayContentsAnimators.delete(displayId);
    }

    private void updateAppWindowsLocked(int displayId) {
        ArrayList<TaskStack> stacks = this.mService.getDisplayContentLocked(displayId).getStacks();
        for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = stacks.get(stackNdx);
            ArrayList<Task> tasks = stack.getTasks();
            for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
                for (int tokenNdx = tokens.size() - 1; tokenNdx >= 0; tokenNdx--) {
                    AppWindowAnimator appAnimator = tokens.get(tokenNdx).mAppAnimator;
                    appAnimator.wasAnimating = appAnimator.animating;
                    if (appAnimator.stepAnimationLocked(this.mCurrentTime, displayId)) {
                        appAnimator.animating = true;
                        setAnimating(true);
                        this.mAppWindowAnimating = true;
                    } else if (appAnimator.wasAnimating) {
                        setAppLayoutChanges(appAnimator, 4, "appToken " + appAnimator.mAppToken + " done", displayId);
                        if (WindowManagerDebugConfig.DEBUG_ANIM) {
                            Slog.v(TAG, "updateWindowsApps...: done animating " + appAnimator.mAppToken);
                        }
                    }
                }
            }
            AppTokenList exitingAppTokens = stack.mExitingAppTokens;
            int exitingCount = exitingAppTokens.size();
            for (int i = 0; i < exitingCount; i++) {
                AppWindowAnimator appAnimator2 = exitingAppTokens.get(i).mAppAnimator;
                appAnimator2.wasAnimating = appAnimator2.animating;
                if (appAnimator2.stepAnimationLocked(this.mCurrentTime, displayId)) {
                    setAnimating(true);
                    this.mAppWindowAnimating = true;
                } else if (appAnimator2.wasAnimating) {
                    setAppLayoutChanges(appAnimator2, 4, "exiting appToken " + appAnimator2.mAppToken + " done", displayId);
                    if (WindowManagerDebugConfig.DEBUG_ANIM) {
                        Slog.v(TAG, "updateWindowsApps...: done animating exiting " + appAnimator2.mAppToken);
                    }
                }
            }
        }
    }

    private boolean shouldForceHide(WindowState win) {
        boolean showImeOverKeyguard;
        boolean z = false;
        WindowState imeTarget = this.mService.mInputMethodTarget;
        if (imeTarget == null || !imeTarget.isVisibleNow()) {
            showImeOverKeyguard = false;
        } else {
            showImeOverKeyguard = ((imeTarget.getAttrs().flags & PackageManagerService.DumpState.DUMP_FROZEN) == 0 && this.mPolicy.canBeForceHidden(imeTarget, imeTarget.mAttrs)) ? false : true;
        }
        WindowState winShowWhenLocked = (WindowState) this.mPolicy.getWinShowWhenLockedLw();
        AppWindowToken appWindowToken = winShowWhenLocked == null ? null : winShowWhenLocked.mAppToken;
        boolean allowWhenLocked = ((win.mIsImWindow || imeTarget == win) ? showImeOverKeyguard : false) | ((win.mAttrs.flags & PackageManagerService.DumpState.DUMP_FROZEN) != 0 ? win.mTurnOnScreen : false);
        if (appWindowToken != null) {
            if (appWindowToken == win.mAppToken || (win.mAttrs.flags & PackageManagerService.DumpState.DUMP_FROZEN) != 0 || (win.mAttrs.privateFlags & 256) != 0) {
                z = true;
            }
            allowWhenLocked |= z;
        }
        boolean keyguardOn = this.mPolicy.isKeyguardShowingOrOccluded() && this.mForceHiding != 2;
        boolean hideDockDivider = win.mAttrs.type == 2034 && win.getDisplayContent().getDockedStackLocked() == null;
        if (keyguardOn && !allowWhenLocked && win.getDisplayId() == 0) {
            return true;
        }
        return hideDockDivider;
    }

    private void updateWindowsLocked(int displayId) {
        this.mAnimTransactionSequence++;
        WindowList windows = this.mService.getWindowListLocked(displayId);
        boolean keyguardGoingAwayToShade = (this.mKeyguardGoingAwayFlags & 1) != 0;
        boolean keyguardGoingAwayNoAnimation = (this.mKeyguardGoingAwayFlags & 2) != 0;
        boolean keyguardGoingAwayWithWallpaper = (this.mKeyguardGoingAwayFlags & 4) != 0;
        if (this.mKeyguardGoingAway) {
            int i = windows.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                WindowState win = windows.get(i);
                if (this.mPolicy.isKeyguardHostWindow(win.mAttrs)) {
                    WindowStateAnimator winAnimator = win.mWinAnimator;
                    if ((win.mAttrs.privateFlags & 1024) == 0) {
                        if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
                            Slog.d(TAG, "updateWindowsLocked: StatusBar is no longer keyguard");
                        }
                        this.mKeyguardGoingAway = false;
                        winAnimator.clearAnimation();
                    } else if (!winAnimator.mAnimating) {
                        if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
                            Slog.d(TAG, "updateWindowsLocked: creating delay animation");
                        }
                        winAnimator.mAnimation = new AlphaAnimation(1.0f, 1.0f);
                        winAnimator.mAnimation.setDuration(1000L);
                        winAnimator.mAnimationIsEntrance = false;
                        winAnimator.mAnimationStartTime = -1L;
                        winAnimator.mKeyguardGoingAwayAnimation = true;
                        winAnimator.mKeyguardGoingAwayWithWallpaper = keyguardGoingAwayWithWallpaper;
                    }
                } else {
                    i--;
                }
            }
        }
        this.mForceHiding = 0;
        boolean wallpaperInUnForceHiding = false;
        boolean startingInUnForceHiding = false;
        ArrayList<WindowStateAnimator> unForceHiding = null;
        WindowState wallpaper = null;
        WallpaperController wallpaperController = this.mService.mWallpaperControllerLocked;
        for (int i2 = windows.size() - 1; i2 >= 0; i2--) {
            WindowState win2 = windows.get(i2);
            WindowStateAnimator winAnimator2 = win2.mWinAnimator;
            int flags = win2.mAttrs.flags;
            boolean canBeForceHidden = this.mPolicy.canBeForceHidden(win2, win2.mAttrs);
            boolean shouldBeForceHidden = shouldForceHide(win2);
            if (winAnimator2.hasSurface()) {
                boolean wasAnimating = winAnimator2.mWasAnimating;
                boolean nowAnimating = winAnimator2.stepAnimationLocked(this.mCurrentTime);
                winAnimator2.mWasAnimating = nowAnimating;
                orAnimating(nowAnimating);
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v(TAG, win2 + ": wasAnimating=" + wasAnimating + ", nowAnimating=" + nowAnimating);
                }
                if (wasAnimating && !winAnimator2.mAnimating && wallpaperController.isWallpaperTarget(win2)) {
                    this.mBulkUpdateParams |= 2;
                    setPendingLayoutChanges(0, 4);
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                        this.mWindowPlacerLocked.debugLayoutRepeats("updateWindowsAndWallpaperLocked 2", getPendingLayoutChanges(0));
                    }
                }
                if (this.mPolicy.isForceHiding(win2.mAttrs)) {
                    if (!wasAnimating && nowAnimating) {
                        if (WindowManagerDebugConfig.DEBUG_KEYGUARD || WindowManagerDebugConfig.DEBUG_ANIM || WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(TAG, "Animation started that could impact force hide: " + win2);
                        }
                        this.mBulkUpdateParams |= 4;
                        setPendingLayoutChanges(displayId, 4);
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                            this.mWindowPlacerLocked.debugLayoutRepeats("updateWindowsAndWallpaperLocked 3", getPendingLayoutChanges(displayId));
                        }
                        this.mService.mFocusMayChange = true;
                    } else if (this.mKeyguardGoingAway && !nowAnimating) {
                        Slog.e(TAG, "Timeout waiting for animation to startup");
                        this.mPolicy.startKeyguardExitAnimation(0L, 0L);
                        this.mKeyguardGoingAway = false;
                    }
                    if (win2.isReadyForDisplay()) {
                        if (nowAnimating && win2.mWinAnimator.mKeyguardGoingAwayAnimation) {
                            this.mForceHiding = 2;
                        } else {
                            this.mForceHiding = win2.isDrawnLw() ? 1 : 0;
                        }
                    }
                    if ((WindowManagerDebugConfig.DEBUG_KEYGUARD || WindowManagerDebugConfig.DEBUG_VISIBILITY) && !WindowManagerService.IS_USER_BUILD) {
                        Slog.v(TAG, "Force hide " + forceHidingToString() + " hasSurface=" + win2.mHasSurface + " policyVis=" + win2.mPolicyVisibility + " destroying=" + win2.mDestroying + " attHidden=" + win2.mAttachedHidden + " vis=" + win2.mViewVisibility + " hidden=" + win2.mRootToken.hidden + " anim=" + win2.mWinAnimator.mAnimation);
                    }
                } else {
                    if (canBeForceHidden) {
                        if (!shouldBeForceHidden) {
                            boolean applyExistingExitAnimation = (this.mPostKeyguardExitAnimation == null || this.mPostKeyguardExitAnimation.hasEnded() || winAnimator2.mKeyguardGoingAwayAnimation || !win2.hasDrawnLw() || win2.mAttachedWindow != null || win2.mIsImWindow || displayId != 0) ? false : true;
                            if (win2.showLw(false, false) || applyExistingExitAnimation) {
                                boolean visibleNow = win2.isVisibleNow();
                                if (visibleNow) {
                                    if (WindowManagerDebugConfig.DEBUG_KEYGUARD || WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                        Slog.v(TAG, "Now policy shown: " + win2);
                                    }
                                    if ((this.mBulkUpdateParams & 4) != 0 && win2.mAttachedWindow == null) {
                                        if (unForceHiding == null) {
                                            unForceHiding = new ArrayList<>();
                                        }
                                        unForceHiding.add(winAnimator2);
                                        if ((1048576 & flags) != 0) {
                                            wallpaperInUnForceHiding = true;
                                        }
                                        if (win2.mAttrs.type == 3) {
                                            startingInUnForceHiding = true;
                                        }
                                    } else if (applyExistingExitAnimation) {
                                        if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
                                            Slog.v(TAG, "Applying existing Keyguard exit animation to new window: win=" + win2);
                                        }
                                        winAnimator2.setAnimation(this.mPolicy.createForceHideEnterAnimation(false, keyguardGoingAwayToShade), this.mPostKeyguardExitAnimation.getStartTime(), 1);
                                        winAnimator2.mKeyguardGoingAwayAnimation = true;
                                        winAnimator2.mKeyguardGoingAwayWithWallpaper = keyguardGoingAwayWithWallpaper;
                                    }
                                    WindowState currentFocus = this.mService.mCurrentFocus;
                                    if (currentFocus == null || currentFocus.mLayer < win2.mLayer) {
                                        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                                            Slog.v(TAG, "updateWindowsLocked: setting mFocusMayChange true");
                                        }
                                        this.mService.mFocusMayChange = true;
                                    }
                                    if ((1048576 & flags) != 0) {
                                    }
                                } else {
                                    win2.hideLw(false, false);
                                }
                            }
                        } else if (win2.hideLw(false, false)) {
                            if (WindowManagerDebugConfig.DEBUG_KEYGUARD || WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                Slog.v(TAG, "Now policy hidden: " + win2);
                            }
                            if ((1048576 & flags) != 0) {
                                this.mBulkUpdateParams |= 2;
                                setPendingLayoutChanges(0, 4);
                                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                                    this.mWindowPlacerLocked.debugLayoutRepeats("updateWindowsAndWallpaperLocked 4", getPendingLayoutChanges(0));
                                }
                            }
                        }
                    }
                }
            } else if (canBeForceHidden) {
                if (shouldBeForceHidden) {
                    win2.hideLw(false, false);
                } else {
                    win2.showLw(false, false);
                }
            }
            AppWindowToken atoken = win2.mAppToken;
            if (winAnimator2.mDrawState == 3 && ((atoken == null || atoken.allDrawn) && winAnimator2.performShowLocked())) {
                setPendingLayoutChanges(displayId, 8);
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    this.mWindowPlacerLocked.debugLayoutRepeats("updateWindowsAndWallpaperLocked 5", getPendingLayoutChanges(displayId));
                }
            }
            AppWindowAnimator appAnimator = winAnimator2.mAppAnimator;
            if (appAnimator != null && appAnimator.thumbnail != null) {
                if (appAnimator.thumbnailTransactionSeq != this.mAnimTransactionSequence) {
                    appAnimator.thumbnailTransactionSeq = this.mAnimTransactionSequence;
                    appAnimator.thumbnailLayer = 0;
                }
                if (appAnimator.thumbnailLayer < winAnimator2.mAnimLayer) {
                    appAnimator.thumbnailLayer = winAnimator2.mAnimLayer;
                }
            }
            if (win2.mIsWallpaper) {
                wallpaper = win2;
            }
        }
        if (unForceHiding != null) {
            if (!keyguardGoingAwayNoAnimation) {
                boolean first = true;
                for (int i3 = unForceHiding.size() - 1; i3 >= 0; i3--) {
                    WindowStateAnimator winAnimator3 = unForceHiding.get(i3);
                    Animation a = this.mPolicy.createForceHideEnterAnimation(wallpaperInUnForceHiding && !startingInUnForceHiding, keyguardGoingAwayToShade);
                    if (a != null) {
                        if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
                            Slog.v(TAG, "Starting keyguard exit animation on window " + winAnimator3.mWin);
                        }
                        winAnimator3.setAnimation(a, 1);
                        winAnimator3.mKeyguardGoingAwayAnimation = true;
                        winAnimator3.mKeyguardGoingAwayWithWallpaper = keyguardGoingAwayWithWallpaper;
                        if (first) {
                            this.mPostKeyguardExitAnimation = a;
                            this.mPostKeyguardExitAnimation.setStartTime(this.mCurrentTime);
                            first = false;
                        }
                    }
                }
            } else if (this.mKeyguardGoingAway) {
                this.mPolicy.startKeyguardExitAnimation(this.mCurrentTime, 0L);
                this.mKeyguardGoingAway = false;
            }
            if (!wallpaperInUnForceHiding && wallpaper != null && !keyguardGoingAwayNoAnimation) {
                if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
                    Slog.d(TAG, "updateWindowsLocked: wallpaper animating away");
                }
                Animation a2 = this.mPolicy.createForceHideWallpaperExitAnimation(keyguardGoingAwayToShade);
                if (a2 != null) {
                    wallpaper.mWinAnimator.setAnimation(a2);
                }
            }
        }
        if (this.mPostKeyguardExitAnimation != null) {
            if (this.mKeyguardGoingAway) {
                this.mPolicy.startKeyguardExitAnimation(this.mCurrentTime + this.mPostKeyguardExitAnimation.getStartOffset(), this.mPostKeyguardExitAnimation.getDuration());
                this.mKeyguardGoingAway = false;
            } else if (this.mPostKeyguardExitAnimation.hasEnded() || this.mCurrentTime - this.mPostKeyguardExitAnimation.getStartTime() > this.mPostKeyguardExitAnimation.getDuration()) {
                if (WindowManagerDebugConfig.DEBUG_KEYGUARD) {
                    Slog.v(TAG, "Done with Keyguard exit animations.");
                }
                this.mPostKeyguardExitAnimation = null;
            }
        }
    }

    private void updateWallpaperLocked(int displayId) {
        TaskStack stack;
        TaskStack stack2;
        this.mService.getDisplayContentLocked(displayId).resetAnimationBackgroundAnimator();
        WindowList windows = this.mService.getWindowListLocked(displayId);
        WindowState detachedWallpaper = null;
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowState win = windows.get(i);
            WindowStateAnimator winAnimator = win.mWinAnimator;
            if (winAnimator.mSurfaceController != null && winAnimator.hasSurface()) {
                int flags = win.mAttrs.flags;
                if (winAnimator.mAnimating) {
                    if (winAnimator.mAnimation != null) {
                        if ((flags & PackageManagerService.DumpState.DUMP_DEXOPT) != 0 && winAnimator.mAnimation.getDetachWallpaper()) {
                            detachedWallpaper = win;
                        }
                        int color = winAnimator.mAnimation.getBackgroundColor();
                        if (color != 0 && (stack2 = win.getStack()) != null) {
                            stack2.setAnimationBackground(winAnimator, color);
                        }
                    }
                    setAnimating(true);
                }
                AppWindowAnimator appAnimator = winAnimator.mAppAnimator;
                if (appAnimator != null && appAnimator.animation != null && appAnimator.animating) {
                    if ((flags & PackageManagerService.DumpState.DUMP_DEXOPT) != 0 && appAnimator.animation.getDetachWallpaper()) {
                        detachedWallpaper = win;
                    }
                    int color2 = appAnimator.animation.getBackgroundColor();
                    if (color2 != 0 && (stack = win.getStack()) != null) {
                        stack.setAnimationBackground(winAnimator, color2);
                    }
                }
            }
        }
        if (this.mWindowDetachedWallpaper == detachedWallpaper) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v(TAG, "Detached wallpaper changed from " + this.mWindowDetachedWallpaper + " to " + detachedWallpaper);
        }
        this.mWindowDetachedWallpaper = detachedWallpaper;
        this.mBulkUpdateParams |= 2;
    }

    private void testTokenMayBeDrawnLocked(int displayId) {
        ArrayList<Task> tasks = this.mService.getDisplayContentLocked(displayId).getTasks();
        int numTasks = tasks.size();
        for (int taskNdx = 0; taskNdx < numTasks; taskNdx++) {
            AppTokenList tokens = tasks.get(taskNdx).mAppTokens;
            int numTokens = tokens.size();
            for (int tokenNdx = 0; tokenNdx < numTokens; tokenNdx++) {
                AppWindowToken wtoken = tokens.get(tokenNdx);
                AppWindowAnimator appAnimator = wtoken.mAppAnimator;
                boolean allDrawn = wtoken.allDrawn;
                if (allDrawn != appAnimator.allDrawn) {
                    appAnimator.allDrawn = allDrawn;
                    if (allDrawn) {
                        if (appAnimator.freezingScreen) {
                            appAnimator.showAllWindowsLocked();
                            this.mService.unsetAppFreezingScreenLocked(wtoken, false, true);
                            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                                Slog.i(TAG, "Setting mOrientationChangeComplete=true because wtoken " + wtoken + " numInteresting=" + wtoken.numInterestingWindows + " numDrawn=" + wtoken.numDrawnWindows);
                            }
                            setAppLayoutChanges(appAnimator, 4, "testTokenMayBeDrawnLocked: freezingScreen", displayId);
                        } else {
                            setAppLayoutChanges(appAnimator, 8, "testTokenMayBeDrawnLocked", displayId);
                            if (!this.mService.mOpeningApps.contains(wtoken)) {
                                orAnimating(appAnimator.showAllWindowsLocked());
                            }
                        }
                    }
                }
            }
        }
    }

    private void animateLocked(long frameTimeNs) {
        if (this.mInitialized) {
            this.mCurrentTime = frameTimeNs / 1000000;
            this.mBulkUpdateParams = 8;
            boolean wasAnimating = this.mAnimating;
            setAnimating(false);
            this.mAppWindowAnimating = false;
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                Slog.i(TAG, "!!! animate: entry time=" + this.mCurrentTime);
            }
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                Slog.i(TAG, ">>> OPEN TRANSACTION animateLocked");
            }
            SurfaceControl.openTransaction();
            SurfaceControl.setAnimationTransaction();
            try {
                try {
                    int numDisplays = this.mDisplayContentsAnimators.size();
                    for (int i = 0; i < numDisplays; i++) {
                        int displayId = this.mDisplayContentsAnimators.keyAt(i);
                        updateAppWindowsLocked(displayId);
                        DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.valueAt(i);
                        ScreenRotationAnimation screenRotationAnimation = displayAnimator.mScreenRotationAnimation;
                        if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
                            if (screenRotationAnimation.stepAnimationLocked(this.mCurrentTime)) {
                                setAnimating(true);
                            } else {
                                this.mBulkUpdateParams |= 1;
                                screenRotationAnimation.kill();
                                displayAnimator.mScreenRotationAnimation = null;
                                if (this.mService.mAccessibilityController != null && displayId == 0) {
                                    this.mService.mAccessibilityController.onRotationChangedLocked(this.mService.getDefaultDisplayContentLocked(), this.mService.mRotation);
                                }
                            }
                        }
                        updateWindowsLocked(displayId);
                        updateWallpaperLocked(displayId);
                        WindowList windows = this.mService.getWindowListLocked(displayId);
                        int N = windows.size();
                        for (int j = 0; j < N; j++) {
                            windows.get(j).mWinAnimator.prepareSurfaceLocked(true);
                        }
                    }
                    for (int i2 = 0; i2 < numDisplays; i2++) {
                        int displayId2 = this.mDisplayContentsAnimators.keyAt(i2);
                        testTokenMayBeDrawnLocked(displayId2);
                        ScreenRotationAnimation screenRotationAnimation2 = this.mDisplayContentsAnimators.valueAt(i2).mScreenRotationAnimation;
                        if (screenRotationAnimation2 != null) {
                            screenRotationAnimation2.updateSurfacesInTransaction();
                        }
                        orAnimating(this.mService.getDisplayContentLocked(displayId2).animateDimLayers());
                        orAnimating(this.mService.getDisplayContentLocked(displayId2).getDockedDividerController().animate(this.mCurrentTime));
                        if (this.mService.mAccessibilityController != null && displayId2 == 0) {
                            this.mService.mAccessibilityController.drawMagnifiedRegionBorderIfNeededLocked();
                        }
                    }
                    if (this.mService.mDragState != null) {
                        this.mAnimating |= this.mService.mDragState.stepAnimationLocked(this.mCurrentTime);
                    }
                    if (this.mAnimating) {
                        this.mService.scheduleAnimationLocked();
                    }
                    if (this.mService.mWatermark != null) {
                        this.mService.mWatermark.drawIfNeeded();
                    }
                    SurfaceControl.closeTransaction();
                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                        Slog.i(TAG, "<<< CLOSE TRANSACTION animateLocked");
                    }
                } catch (RuntimeException e) {
                    Slog.d(TAG, "Unhandled exception in Window Manager", e);
                    SurfaceControl.closeTransaction();
                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                        Slog.i(TAG, "<<< CLOSE TRANSACTION animateLocked");
                    }
                }
                boolean hasPendingLayoutChanges = false;
                int numDisplays2 = this.mService.mDisplayContents.size();
                for (int displayNdx = 0; displayNdx < numDisplays2; displayNdx++) {
                    DisplayContent displayContent = this.mService.mDisplayContents.valueAt(displayNdx);
                    int pendingChanges = getPendingLayoutChanges(displayContent.getDisplayId());
                    if ((pendingChanges & 4) != 0) {
                        this.mBulkUpdateParams |= 32;
                    }
                    if (pendingChanges != 0) {
                        hasPendingLayoutChanges = true;
                    }
                }
                boolean doRequest = this.mBulkUpdateParams != 0 ? this.mWindowPlacerLocked.copyAnimToLayoutParamsLocked() : false;
                if (hasPendingLayoutChanges || doRequest) {
                    this.mWindowPlacerLocked.requestTraversal();
                }
                if (this.mAnimating && !wasAnimating && Trace.isTagEnabled(32L)) {
                    Trace.asyncTraceBegin(32L, "animating", 0);
                }
                if (!this.mAnimating && wasAnimating) {
                    this.mWindowPlacerLocked.requestTraversal();
                    if (Trace.isTagEnabled(32L)) {
                        Trace.asyncTraceEnd(32L, "animating", 0);
                    }
                }
                if (this.mRemoveReplacedWindows) {
                    removeReplacedWindowsLocked();
                }
                this.mService.stopUsingSavedSurfaceLocked();
                this.mService.destroyPreservedSurfaceLocked();
                this.mService.mWindowPlacerLocked.destroyPendingSurfaces();
                if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                    Slog.i(TAG, "!!! animate: exit mAnimating=" + this.mAnimating + " mBulkUpdateParams=" + Integer.toHexString(this.mBulkUpdateParams) + " mPendingLayoutChanges(DEFAULT_DISPLAY)=" + Integer.toHexString(getPendingLayoutChanges(0)));
                }
            } catch (Throwable th) {
                SurfaceControl.closeTransaction();
                if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                    Slog.i(TAG, "<<< CLOSE TRANSACTION animateLocked");
                }
                throw th;
            }
        }
    }

    private void removeReplacedWindowsLocked() {
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            Slog.i(TAG, ">>> OPEN TRANSACTION removeReplacedWindows");
        }
        SurfaceControl.openTransaction();
        try {
            for (int i = this.mService.mDisplayContents.size() - 1; i >= 0; i--) {
                DisplayContent display = this.mService.mDisplayContents.valueAt(i);
                WindowList windows = this.mService.getWindowListLocked(display.getDisplayId());
                for (int j = windows.size() - 1; j >= 0; j--) {
                    if (j < windows.size()) {
                        windows.get(j).maybeRemoveReplacedWindow();
                    }
                }
            }
            this.mRemoveReplacedWindows = false;
        } finally {
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                Slog.i(TAG, "<<< CLOSE TRANSACTION removeReplacedWindows");
            }
        }
    }

    private static String bulkUpdateParamsToString(int bulkUpdateParams) {
        StringBuilder builder = new StringBuilder(128);
        if ((bulkUpdateParams & 1) != 0) {
            builder.append(" UPDATE_ROTATION");
        }
        if ((bulkUpdateParams & 2) != 0) {
            builder.append(" WALLPAPER_MAY_CHANGE");
        }
        if ((bulkUpdateParams & 4) != 0) {
            builder.append(" FORCE_HIDING_CHANGED");
        }
        if ((bulkUpdateParams & 8) != 0) {
            builder.append(" ORIENTATION_CHANGE_COMPLETE");
        }
        if ((bulkUpdateParams & 16) != 0) {
            builder.append(" TURN_ON_SCREEN");
        }
        return builder.toString();
    }

    public void dumpLocked(PrintWriter pw, String prefix, boolean dumpAll) {
        String subPrefix = "  " + prefix;
        String subSubPrefix = "  " + subPrefix;
        for (int i = 0; i < this.mDisplayContentsAnimators.size(); i++) {
            pw.print(prefix);
            pw.print("DisplayContentsAnimator #");
            pw.print(this.mDisplayContentsAnimators.keyAt(i));
            pw.println(":");
            DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.valueAt(i);
            WindowList windows = this.mService.getWindowListLocked(this.mDisplayContentsAnimators.keyAt(i));
            int N = windows.size();
            for (int j = 0; j < N; j++) {
                WindowStateAnimator wanim = windows.get(j).mWinAnimator;
                pw.print(subPrefix);
                pw.print("Window #");
                pw.print(j);
                pw.print(": ");
                pw.println(wanim);
            }
            if (displayAnimator.mScreenRotationAnimation != null) {
                pw.print(subPrefix);
                pw.println("mScreenRotationAnimation:");
                displayAnimator.mScreenRotationAnimation.printTo(subSubPrefix, pw);
            } else if (dumpAll) {
                pw.print(subPrefix);
                pw.println("no ScreenRotationAnimation ");
            }
            pw.println();
        }
        pw.println();
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mAnimTransactionSequence=");
            pw.print(this.mAnimTransactionSequence);
            pw.print(" mForceHiding=");
            pw.println(forceHidingToString());
            pw.print(prefix);
            pw.print("mCurrentTime=");
            pw.println(TimeUtils.formatUptime(this.mCurrentTime));
        }
        if (this.mBulkUpdateParams != 0) {
            pw.print(prefix);
            pw.print("mBulkUpdateParams=0x");
            pw.print(Integer.toHexString(this.mBulkUpdateParams));
            pw.println(bulkUpdateParamsToString(this.mBulkUpdateParams));
        }
        if (this.mWindowDetachedWallpaper == null) {
            return;
        }
        pw.print(prefix);
        pw.print("mWindowDetachedWallpaper=");
        pw.println(this.mWindowDetachedWallpaper);
    }

    int getPendingLayoutChanges(int displayId) {
        DisplayContent displayContent;
        if (displayId >= 0 && (displayContent = this.mService.getDisplayContentLocked(displayId)) != null) {
            return displayContent.pendingLayoutChanges;
        }
        return 0;
    }

    void setPendingLayoutChanges(int displayId, int changes) {
        DisplayContent displayContent;
        if (displayId < 0 || (displayContent = this.mService.getDisplayContentLocked(displayId)) == null) {
            return;
        }
        displayContent.pendingLayoutChanges |= changes;
    }

    void setAppLayoutChanges(AppWindowAnimator appAnimator, int changes, String reason, int displayId) {
        WindowList windows = appAnimator.mAppToken.allAppWindows;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (displayId == windows.get(i).getDisplayId()) {
                setPendingLayoutChanges(displayId, changes);
                if (!WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    return;
                }
                this.mWindowPlacerLocked.debugLayoutRepeats(reason, getPendingLayoutChanges(displayId));
                return;
            }
        }
    }

    private DisplayContentsAnimator getDisplayContentsAnimatorLocked(int displayId) {
        DisplayContentsAnimator displayContentsAnimator = null;
        DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.get(displayId);
        if (displayAnimator == null) {
            DisplayContentsAnimator displayAnimator2 = new DisplayContentsAnimator(this, displayContentsAnimator);
            this.mDisplayContentsAnimators.put(displayId, displayAnimator2);
            return displayAnimator2;
        }
        return displayAnimator;
    }

    void setScreenRotationAnimationLocked(int displayId, ScreenRotationAnimation animation) {
        if (displayId < 0) {
            return;
        }
        getDisplayContentsAnimatorLocked(displayId).mScreenRotationAnimation = animation;
    }

    ScreenRotationAnimation getScreenRotationAnimationLocked(int displayId) {
        if (displayId < 0) {
            return null;
        }
        return getDisplayContentsAnimatorLocked(displayId).mScreenRotationAnimation;
    }

    void requestRemovalOfReplacedWindows(WindowState win) {
        this.mRemoveReplacedWindows = true;
    }

    private class DisplayContentsAnimator {
        ScreenRotationAnimation mScreenRotationAnimation;

        DisplayContentsAnimator(WindowAnimator this$0, DisplayContentsAnimator displayContentsAnimator) {
            this();
        }

        private DisplayContentsAnimator() {
            this.mScreenRotationAnimation = null;
        }
    }

    boolean isAnimating() {
        return this.mAnimating;
    }

    void setAnimating(boolean animating) {
        this.mAnimating = animating;
    }

    void orAnimating(boolean animating) {
        this.mAnimating |= animating;
    }
}
