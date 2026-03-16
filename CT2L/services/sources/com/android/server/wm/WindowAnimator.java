package com.android.server.wm;

import android.content.Context;
import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.view.SurfaceControl;
import android.view.WindowManagerPolicy;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;

public class WindowAnimator {
    static final int KEYGUARD_ANIMATING_IN = 1;
    static final int KEYGUARD_ANIMATING_OUT = 3;
    private static final long KEYGUARD_ANIM_TIMEOUT_MS = 1000;
    static final int KEYGUARD_NOT_SHOWN = 0;
    static final int KEYGUARD_SHOWN = 2;
    private static final String TAG = "WindowAnimator";
    private int mAnimTransactionSequence;
    boolean mAnimating;
    final Context mContext;
    long mCurrentTime;
    boolean mKeyguardGoingAway;
    boolean mKeyguardGoingAwayDisableWindowAnimations;
    boolean mKeyguardGoingAwayToNotificationShade;
    Object mLastWindowFreezeSource;
    final WindowManagerPolicy mPolicy;
    Animation mPostKeyguardExitAnimation;
    final WindowManagerService mService;
    WindowState mWindowDetachedWallpaper = null;
    WindowStateAnimator mUniverseBackground = null;
    int mAboveUniverseLayer = 0;
    int mBulkUpdateParams = 0;
    SparseArray<DisplayContentsAnimator> mDisplayContentsAnimators = new SparseArray<>(2);
    boolean mInitialized = false;
    int mForceHiding = 0;
    final Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (WindowAnimator.this.mService.mWindowMap) {
                WindowAnimator.this.mService.mAnimationScheduled = false;
                WindowAnimator.this.animateLocked();
            }
        }
    };

    private String forceHidingToString() {
        switch (this.mForceHiding) {
            case 0:
                return "KEYGUARD_NOT_SHOWN";
            case 1:
                return "KEYGUARD_ANIMATING_IN";
            case 2:
                return "KEYGUARD_SHOWN";
            case 3:
                return "KEYGUARD_ANIMATING_OUT";
            default:
                return "KEYGUARD STATE UNKNOWN " + this.mForceHiding;
        }
    }

    WindowAnimator(WindowManagerService service) {
        this.mService = service;
        this.mContext = service.mContext;
        this.mPolicy = service.mPolicy;
    }

    void addDisplayLocked(int displayId) {
        getDisplayContentsAnimatorLocked(displayId);
        if (displayId == 0) {
            this.mInitialized = true;
        }
    }

    void removeDisplayLocked(int displayId) {
        DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.get(displayId);
        if (displayAnimator != null && displayAnimator.mScreenRotationAnimation != null) {
            displayAnimator.mScreenRotationAnimation.kill();
            displayAnimator.mScreenRotationAnimation = null;
        }
        this.mDisplayContentsAnimators.delete(displayId);
    }

    void hideWallpapersLocked(WindowState w) {
        WindowState wallpaperTarget = this.mService.mWallpaperTarget;
        WindowState lowerWallpaperTarget = this.mService.mLowerWallpaperTarget;
        ArrayList<WindowToken> wallpaperTokens = this.mService.mWallpaperTokens;
        if ((wallpaperTarget == w && lowerWallpaperTarget == null) || wallpaperTarget == null) {
            int numTokens = wallpaperTokens.size();
            for (int i = numTokens - 1; i >= 0; i--) {
                WindowToken token = wallpaperTokens.get(i);
                int numWindows = token.windows.size();
                for (int j = numWindows - 1; j >= 0; j--) {
                    WindowState wallpaper = token.windows.get(j);
                    WindowStateAnimator winAnimator = wallpaper.mWinAnimator;
                    if (!winAnimator.mLastHidden) {
                        winAnimator.hide();
                        this.mService.dispatchWallpaperVisibility(wallpaper, false);
                        setPendingLayoutChanges(0, 4);
                    }
                }
                token.hidden = true;
            }
        }
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
                    boolean wasAnimating = (appAnimator.animation == null || appAnimator.animation == AppWindowAnimator.sDummyAnimation) ? false : true;
                    if (appAnimator.stepAnimationLocked(this.mCurrentTime)) {
                        this.mAnimating = true;
                    } else if (wasAnimating) {
                        setAppLayoutChanges(appAnimator, 4, "appToken " + appAnimator.mAppToken + " done");
                    }
                }
            }
            AppTokenList exitingAppTokens = stack.mExitingAppTokens;
            int NEAT = exitingAppTokens.size();
            for (int i = 0; i < NEAT; i++) {
                AppWindowAnimator appAnimator2 = exitingAppTokens.get(i).mAppAnimator;
                boolean wasAnimating2 = (appAnimator2.animation == null || appAnimator2.animation == AppWindowAnimator.sDummyAnimation) ? false : true;
                if (appAnimator2.stepAnimationLocked(this.mCurrentTime)) {
                    this.mAnimating = true;
                } else if (wasAnimating2) {
                    setAppLayoutChanges(appAnimator2, 4, "exiting appToken " + appAnimator2.mAppToken + " done");
                }
            }
        }
    }

    private boolean shouldForceHide(WindowState win) {
        WindowState imeTarget = this.mService.mInputMethodTarget;
        boolean showImeOverKeyguard = (imeTarget == null || !imeTarget.isVisibleNow() || (imeTarget.getAttrs().flags & 524288) == 0) ? false : true;
        WindowState winShowWhenLocked = (WindowState) this.mPolicy.getWinShowWhenLockedLw();
        AppWindowToken appShowWhenLocked = winShowWhenLocked == null ? null : winShowWhenLocked.mAppToken;
        boolean hideWhenLocked = !((win.mIsImWindow || imeTarget == win) && showImeOverKeyguard) && (appShowWhenLocked == null || (appShowWhenLocked != win.mAppToken && (win.mAttrs.privateFlags & PackageManagerService.DumpState.DUMP_VERIFIERS) == 0));
        return (this.mForceHiding == 1 && (!win.mWinAnimator.isAnimating() || hideWhenLocked)) || (this.mForceHiding == 2 && hideWhenLocked);
    }

    private void updateWindowsLocked(int displayId) {
        Animation a;
        this.mAnimTransactionSequence++;
        WindowList windows = this.mService.getWindowListLocked(displayId);
        if (this.mKeyguardGoingAway) {
            int i = windows.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                WindowState win = windows.get(i);
                if (this.mPolicy.isKeyguardHostWindow(win.mAttrs)) {
                    WindowStateAnimator winAnimator = win.mWinAnimator;
                    if ((win.mAttrs.privateFlags & 1024) != 0) {
                        if (!winAnimator.mAnimating) {
                            winAnimator.mAnimation = new AlphaAnimation(1.0f, 1.0f);
                            winAnimator.mAnimation.setDuration(KEYGUARD_ANIM_TIMEOUT_MS);
                            winAnimator.mAnimationIsEntrance = false;
                            winAnimator.mAnimationStartTime = -1L;
                        }
                    } else {
                        this.mKeyguardGoingAway = false;
                        winAnimator.clearAnimation();
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
        for (int i2 = windows.size() - 1; i2 >= 0; i2--) {
            WindowState win2 = windows.get(i2);
            WindowStateAnimator winAnimator2 = win2.mWinAnimator;
            int flags = win2.mAttrs.flags;
            boolean canBeForceHidden = this.mPolicy.canBeForceHidden(win2, win2.mAttrs);
            boolean shouldBeForceHidden = shouldForceHide(win2);
            if (winAnimator2.mSurfaceControl != null) {
                boolean wasAnimating = winAnimator2.mWasAnimating;
                boolean nowAnimating = winAnimator2.stepAnimationLocked(this.mCurrentTime);
                this.mAnimating |= nowAnimating;
                if (wasAnimating && !winAnimator2.mAnimating && this.mService.mWallpaperTarget == win2) {
                    this.mBulkUpdateParams |= 2;
                    setPendingLayoutChanges(0, 4);
                    this.mService.debugLayoutRepeats("updateWindowsAndWallpaperLocked 2", getPendingLayoutChanges(0));
                }
                if (this.mPolicy.isForceHiding(win2.mAttrs)) {
                    if (!wasAnimating && nowAnimating) {
                        this.mBulkUpdateParams |= 4;
                        setPendingLayoutChanges(displayId, 4);
                        this.mService.debugLayoutRepeats("updateWindowsAndWallpaperLocked 3", getPendingLayoutChanges(displayId));
                        this.mService.mFocusMayChange = true;
                    } else if (this.mKeyguardGoingAway && !nowAnimating) {
                        Slog.e(TAG, "Timeout waiting for animation to startup");
                        this.mPolicy.startKeyguardExitAnimation(0L, 0L);
                        this.mKeyguardGoingAway = false;
                    }
                    if (win2.isReadyForDisplay()) {
                        if (nowAnimating) {
                            if (winAnimator2.mAnimationIsEntrance) {
                                this.mForceHiding = 1;
                            } else {
                                this.mForceHiding = 3;
                            }
                        } else {
                            this.mForceHiding = win2.isDrawnLw() ? 2 : 0;
                        }
                    }
                } else {
                    if (canBeForceHidden) {
                        if (shouldBeForceHidden) {
                            if (!win2.hideLw(false, false)) {
                            }
                            if ((1048576 & flags) != 0) {
                                this.mBulkUpdateParams |= 2;
                                setPendingLayoutChanges(0, 4);
                                this.mService.debugLayoutRepeats("updateWindowsAndWallpaperLocked 4", getPendingLayoutChanges(0));
                            }
                        } else {
                            boolean applyExistingExitAnimation = (this.mPostKeyguardExitAnimation == null || winAnimator2.mKeyguardGoingAwayAnimation || !win2.hasDrawnLw() || win2.mAttachedWindow != null || this.mForceHiding == 0) ? false : true;
                            if (win2.showLw(false, false) || applyExistingExitAnimation) {
                                boolean visibleNow = win2.isVisibleNow();
                                if (!visibleNow) {
                                    win2.hideLw(false, false);
                                } else {
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
                                        winAnimator2.setAnimation(this.mPolicy.createForceHideEnterAnimation(false, this.mKeyguardGoingAwayToNotificationShade), this.mPostKeyguardExitAnimation.getStartTime());
                                        winAnimator2.mKeyguardGoingAwayAnimation = true;
                                    }
                                    WindowState currentFocus = this.mService.mCurrentFocus;
                                    if (currentFocus == null || currentFocus.mLayer < win2.mLayer) {
                                        this.mService.mFocusMayChange = true;
                                    }
                                    if ((1048576 & flags) != 0) {
                                    }
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
                this.mService.debugLayoutRepeats("updateWindowsAndWallpaperLocked 5", getPendingLayoutChanges(displayId));
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
            if (!this.mKeyguardGoingAwayDisableWindowAnimations) {
                boolean first = true;
                for (int i3 = unForceHiding.size() - 1; i3 >= 0; i3--) {
                    WindowStateAnimator winAnimator3 = unForceHiding.get(i3);
                    Animation a2 = this.mPolicy.createForceHideEnterAnimation(wallpaperInUnForceHiding && !startingInUnForceHiding, this.mKeyguardGoingAwayToNotificationShade);
                    if (a2 != null) {
                        winAnimator3.setAnimation(a2);
                        winAnimator3.mKeyguardGoingAwayAnimation = true;
                        if (first) {
                            this.mPostKeyguardExitAnimation = a2;
                            this.mPostKeyguardExitAnimation.setStartTime(this.mCurrentTime);
                            first = false;
                        }
                    }
                }
            } else if (this.mKeyguardGoingAway) {
                this.mPolicy.startKeyguardExitAnimation(this.mCurrentTime, 0L);
                this.mKeyguardGoingAway = false;
            }
            if (!wallpaperInUnForceHiding && wallpaper != null && !this.mKeyguardGoingAwayDisableWindowAnimations && (a = this.mPolicy.createForceHideWallpaperExitAnimation(this.mKeyguardGoingAwayToNotificationShade)) != null) {
                wallpaper.mWinAnimator.setAnimation(a);
            }
        }
        if (this.mPostKeyguardExitAnimation != null) {
            if (this.mKeyguardGoingAway) {
                this.mPolicy.startKeyguardExitAnimation(this.mCurrentTime + this.mPostKeyguardExitAnimation.getStartOffset(), this.mPostKeyguardExitAnimation.getDuration());
                this.mKeyguardGoingAway = false;
            } else if (this.mCurrentTime - this.mPostKeyguardExitAnimation.getStartTime() > this.mPostKeyguardExitAnimation.getDuration()) {
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
            if (winAnimator.mSurfaceControl != null) {
                int flags = win.mAttrs.flags;
                if (winAnimator.mAnimating) {
                    if (winAnimator.mAnimation != null) {
                        if ((flags & 1048576) != 0 && winAnimator.mAnimation.getDetachWallpaper()) {
                            detachedWallpaper = win;
                        }
                        int color = winAnimator.mAnimation.getBackgroundColor();
                        if (color != 0 && (stack2 = win.getStack()) != null) {
                            stack2.setAnimationBackground(winAnimator, color);
                        }
                    }
                    this.mAnimating = true;
                }
                AppWindowAnimator appAnimator = winAnimator.mAppAnimator;
                if (appAnimator != null && appAnimator.animation != null && appAnimator.animating) {
                    if ((flags & 1048576) != 0 && appAnimator.animation.getDetachWallpaper()) {
                        detachedWallpaper = win;
                    }
                    int color2 = appAnimator.animation.getBackgroundColor();
                    if (color2 != 0 && (stack = win.getStack()) != null) {
                        stack.setAnimationBackground(winAnimator, color2);
                    }
                }
            }
        }
        if (this.mWindowDetachedWallpaper != detachedWallpaper) {
            this.mWindowDetachedWallpaper = detachedWallpaper;
            this.mBulkUpdateParams |= 2;
        }
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
                            setAppLayoutChanges(appAnimator, 4, "testTokenMayBeDrawnLocked: freezingScreen");
                        } else {
                            setAppLayoutChanges(appAnimator, 8, "testTokenMayBeDrawnLocked");
                            if (!this.mService.mOpeningApps.contains(wtoken)) {
                                this.mAnimating |= appAnimator.showAllWindowsLocked();
                            }
                        }
                    }
                }
            }
        }
    }

    private void animateLocked() {
        if (this.mInitialized) {
            this.mCurrentTime = SystemClock.uptimeMillis();
            this.mBulkUpdateParams = 8;
            boolean wasAnimating = this.mAnimating;
            this.mAnimating = false;
            SurfaceControl.openTransaction();
            SurfaceControl.setAnimationTransaction();
            try {
                int numDisplays = this.mDisplayContentsAnimators.size();
                for (int i = 0; i < numDisplays; i++) {
                    int displayId = this.mDisplayContentsAnimators.keyAt(i);
                    updateAppWindowsLocked(displayId);
                    DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.valueAt(i);
                    ScreenRotationAnimation screenRotationAnimation = displayAnimator.mScreenRotationAnimation;
                    if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
                        if (screenRotationAnimation.stepAnimationLocked(this.mCurrentTime)) {
                            this.mAnimating = true;
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
                    this.mAnimating |= this.mService.getDisplayContentLocked(displayId2).animateDimLayers();
                    if (this.mService.mAccessibilityController != null && displayId2 == 0) {
                        this.mService.mAccessibilityController.drawMagnifiedRegionBorderIfNeededLocked();
                    }
                }
                if (this.mAnimating) {
                    this.mService.scheduleAnimationLocked();
                }
                this.mService.setFocusedStackLayer();
                if (this.mService.mWatermark != null) {
                    this.mService.mWatermark.drawIfNeeded();
                }
            } catch (RuntimeException e) {
                Slog.wtf(TAG, "Unhandled exception in Window Manager", e);
            } finally {
                SurfaceControl.closeTransaction();
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
            boolean doRequest = false;
            if (this.mBulkUpdateParams != 0) {
                doRequest = this.mService.copyAnimToLayoutParamsLocked();
            }
            if (hasPendingLayoutChanges || doRequest) {
                this.mService.requestTraversalLocked();
            }
            if (!this.mAnimating && wasAnimating) {
                this.mService.requestTraversalLocked();
            }
        }
    }

    static String bulkUpdateParamsToString(int bulkUpdateParams) {
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
        if (this.mWindowDetachedWallpaper != null) {
            pw.print(prefix);
            pw.print("mWindowDetachedWallpaper=");
            pw.println(this.mWindowDetachedWallpaper);
        }
        if (this.mUniverseBackground != null) {
            pw.print(prefix);
            pw.print("mUniverseBackground=");
            pw.print(this.mUniverseBackground);
            pw.print(" mAboveUniverseLayer=");
            pw.println(this.mAboveUniverseLayer);
        }
    }

    int getPendingLayoutChanges(int displayId) {
        if (displayId < 0) {
            return 0;
        }
        return this.mService.getDisplayContentLocked(displayId).pendingLayoutChanges;
    }

    void setPendingLayoutChanges(int displayId, int changes) {
        if (displayId >= 0) {
            this.mService.getDisplayContentLocked(displayId).pendingLayoutChanges |= changes;
        }
    }

    void setAppLayoutChanges(AppWindowAnimator appAnimator, int changes, String s) {
        SparseIntArray displays = new SparseIntArray(2);
        WindowList windows = appAnimator.mAppToken.allAppWindows;
        for (int i = windows.size() - 1; i >= 0; i--) {
            int displayId = windows.get(i).getDisplayId();
            if (displayId >= 0 && displays.indexOfKey(displayId) < 0) {
                setPendingLayoutChanges(displayId, changes);
                this.mService.debugLayoutRepeats(s, getPendingLayoutChanges(displayId));
                displays.put(displayId, changes);
            }
        }
    }

    private DisplayContentsAnimator getDisplayContentsAnimatorLocked(int displayId) {
        DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.get(displayId);
        if (displayAnimator == null) {
            DisplayContentsAnimator displayAnimator2 = new DisplayContentsAnimator();
            this.mDisplayContentsAnimators.put(displayId, displayAnimator2);
            return displayAnimator2;
        }
        return displayAnimator;
    }

    void setScreenRotationAnimationLocked(int displayId, ScreenRotationAnimation animation) {
        if (displayId >= 0) {
            getDisplayContentsAnimatorLocked(displayId).mScreenRotationAnimation = animation;
        }
    }

    ScreenRotationAnimation getScreenRotationAnimationLocked(int displayId) {
        if (displayId < 0) {
            return null;
        }
        return getDisplayContentsAnimatorLocked(displayId).mScreenRotationAnimation;
    }

    private class DisplayContentsAnimator {
        ScreenRotationAnimation mScreenRotationAnimation;

        private DisplayContentsAnimator() {
            this.mScreenRotationAnimation = null;
        }
    }
}
