package com.android.server.wm;

import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.pm.DumpState;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Predicate;

class WallpaperController {
    private static final String TAG = "WindowManager";
    private static final int WALLPAPER_DRAW_NORMAL = 0;
    private static final int WALLPAPER_DRAW_PENDING = 1;
    private static final long WALLPAPER_DRAW_PENDING_TIMEOUT_DURATION = 500;
    private static final int WALLPAPER_DRAW_TIMEOUT = 2;
    private static final long WALLPAPER_TIMEOUT = 150;
    private static final long WALLPAPER_TIMEOUT_RECOVERY = 10000;
    private long mLastWallpaperTimeoutTime;
    private WindowManagerService mService;
    private WindowState mTmpTopWallpaper;
    private WindowState mWaitingOnWallpaper;
    private final ArrayList<WallpaperWindowToken> mWallpaperTokens = new ArrayList<>();
    private WindowState mWallpaperTarget = null;
    private WindowState mPrevWallpaperTarget = null;
    private float mLastWallpaperX = -1.0f;
    private float mLastWallpaperY = -1.0f;
    private float mLastWallpaperXStep = -1.0f;
    private float mLastWallpaperYStep = -1.0f;
    private int mLastWallpaperDisplayOffsetX = Integer.MIN_VALUE;
    private int mLastWallpaperDisplayOffsetY = Integer.MIN_VALUE;
    WindowState mDeferredHideWallpaper = null;
    private int mWallpaperDrawState = 0;
    private final FindWallpaperTargetResult mFindResults = new FindWallpaperTargetResult();
    private final ToBooleanFunction<WindowState> mFindWallpaperTargetFunction = new ToBooleanFunction() {
        public final boolean apply(Object obj) {
            return WallpaperController.lambda$new$0(this.f$0, (WindowState) obj);
        }
    };

    public static boolean lambda$new$0(WallpaperController wallpaperController, WindowState windowState) {
        WindowAnimator windowAnimator = wallpaperController.mService.mAnimator;
        if (windowState.mAttrs.type == 2013) {
            if (wallpaperController.mFindResults.topWallpaper == null || wallpaperController.mFindResults.resetTopWallpaper) {
                wallpaperController.mFindResults.setTopWallpaper(windowState);
                wallpaperController.mFindResults.resetTopWallpaper = false;
            }
            return false;
        }
        wallpaperController.mFindResults.resetTopWallpaper = true;
        if (windowState != windowAnimator.mWindowDetachedWallpaper && windowState.mAppToken != null && windowState.mAppToken.isHidden() && !windowState.mAppToken.isSelfAnimating()) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "Skipping hidden and not animating token: " + windowState);
            }
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v("WindowManager", "Win " + windowState + ": isOnScreen=" + windowState.isOnScreen() + " mDrawState=" + windowState.mWinAnimator.mDrawState);
        }
        if (windowState.mWillReplaceWindow && wallpaperController.mWallpaperTarget == null && !wallpaperController.mFindResults.useTopWallpaperAsTarget) {
            wallpaperController.mFindResults.setUseTopWallpaperAsTarget(true);
        }
        boolean z = windowState.mAppToken != null && windowState.mAppToken.isSelfAnimating() && AppTransition.isKeyguardGoingAwayTransit(windowState.mAppToken.getTransit()) && (windowState.mAppToken.getTransitFlags() & 4) != 0;
        boolean z2 = (windowState.mAttrs.flags & DumpState.DUMP_FROZEN) != 0 && wallpaperController.mService.mPolicy.isKeyguardLocked() && wallpaperController.mService.mPolicy.isKeyguardOccluded() && !(wallpaperController.isFullscreen(windowState.mAttrs) && (windowState.mAppToken == null || windowState.mAppToken.fillsParent()));
        if (z || z2) {
            wallpaperController.mFindResults.setUseTopWallpaperAsTarget(true);
        }
        RecentsAnimationController recentsAnimationController = wallpaperController.mService.getRecentsAnimationController();
        boolean z3 = (windowState.mAttrs.flags & DumpState.DUMP_DEXOPT) != 0 || (windowState.mAppToken != null && windowState.mAppToken.getAnimation() != null && windowState.mAppToken.getAnimation().getShowWallpaper());
        if (recentsAnimationController != null && recentsAnimationController.isWallpaperVisible(windowState)) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "Found recents animation wallpaper target: " + windowState);
            }
            wallpaperController.mFindResults.setWallpaperTarget(windowState);
            return true;
        }
        if (z3 && windowState.isOnScreen() && (wallpaperController.mWallpaperTarget == windowState || windowState.isDrawFinishedLw())) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "Found wallpaper target: " + windowState);
            }
            wallpaperController.mFindResults.setWallpaperTarget(windowState);
            if (windowState == wallpaperController.mWallpaperTarget && windowState.mWinAnimator.isAnimationSet() && WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "Win " + windowState + ": token animating, looking behind.");
            }
            return true;
        }
        if (windowState == windowAnimator.mWindowDetachedWallpaper) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                Slog.v("WindowManager", "Found animating detached wallpaper target win: " + windowState);
            }
            wallpaperController.mFindResults.setUseTopWallpaperAsTarget(true);
        }
        return false;
    }

    public WallpaperController(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
    }

    WindowState getWallpaperTarget() {
        return this.mWallpaperTarget;
    }

    boolean isWallpaperTarget(WindowState windowState) {
        return windowState == this.mWallpaperTarget;
    }

    boolean isBelowWallpaperTarget(WindowState windowState) {
        return this.mWallpaperTarget != null && this.mWallpaperTarget.mLayer >= windowState.mBaseLayer;
    }

    boolean isWallpaperVisible() {
        return isWallpaperVisible(this.mWallpaperTarget);
    }

    void startWallpaperAnimation(Animation animation) {
        for (int size = this.mWallpaperTokens.size() - 1; size >= 0; size--) {
            this.mWallpaperTokens.get(size).startAnimation(animation);
        }
    }

    private final boolean isWallpaperVisible(WindowState windowState) {
        RecentsAnimationController recentsAnimationController = this.mService.getRecentsAnimationController();
        boolean z = recentsAnimationController != null && recentsAnimationController.isWallpaperVisible(windowState);
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            StringBuilder sb = new StringBuilder();
            sb.append("Wallpaper vis: target ");
            sb.append(windowState);
            sb.append(", obscured=");
            sb.append(windowState != null ? Boolean.toString(windowState.mObscured) : "??");
            sb.append(" animating=");
            sb.append((windowState == null || windowState.mAppToken == null) ? null : Boolean.valueOf(windowState.mAppToken.isSelfAnimating()));
            sb.append(" prev=");
            sb.append(this.mPrevWallpaperTarget);
            sb.append(" recentsAnimationWallpaperVisible=");
            sb.append(z);
            Slog.v("WindowManager", sb.toString());
        }
        return (windowState != null && (!windowState.mObscured || z || (windowState.mAppToken != null && windowState.mAppToken.isSelfAnimating()))) || this.mPrevWallpaperTarget != null;
    }

    boolean isWallpaperTargetAnimating() {
        return this.mWallpaperTarget != null && this.mWallpaperTarget.mWinAnimator.isAnimationSet() && (this.mWallpaperTarget.mAppToken == null || !this.mWallpaperTarget.mAppToken.isWaitingForTransitionStart());
    }

    void updateWallpaperVisibility() {
        boolean zIsWallpaperVisible = isWallpaperVisible(this.mWallpaperTarget);
        for (int size = this.mWallpaperTokens.size() - 1; size >= 0; size--) {
            this.mWallpaperTokens.get(size).updateWallpaperVisibility(zIsWallpaperVisible);
        }
    }

    void hideDeferredWallpapersIfNeeded() {
        if (this.mDeferredHideWallpaper != null) {
            hideWallpapers(this.mDeferredHideWallpaper);
            this.mDeferredHideWallpaper = null;
        }
    }

    void hideWallpapers(WindowState windowState) {
        boolean z;
        if (this.mWallpaperTarget != null && (this.mWallpaperTarget != windowState || this.mPrevWallpaperTarget != null)) {
            return;
        }
        if (this.mService.mAppTransition.isRunning()) {
            this.mDeferredHideWallpaper = windowState;
            return;
        }
        if (this.mDeferredHideWallpaper != windowState) {
            z = false;
        } else {
            z = true;
        }
        for (int size = this.mWallpaperTokens.size() - 1; size >= 0; size--) {
            WallpaperWindowToken wallpaperWindowToken = this.mWallpaperTokens.get(size);
            wallpaperWindowToken.hideWallpaperToken(z, "hideWallpapers");
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT && !wallpaperWindowToken.isHidden()) {
                Slog.d("WindowManager", "Hiding wallpaper " + wallpaperWindowToken + " from " + windowState + " target=" + this.mWallpaperTarget + " prev=" + this.mPrevWallpaperTarget + "\n" + Debug.getCallers(5, "  "));
            }
        }
    }

    boolean updateWallpaperOffset(WindowState windowState, int i, int i2, boolean z) {
        int i3;
        boolean z2;
        float f = windowState.isRtl() ? 1.0f : 0.0f;
        if (this.mLastWallpaperX >= 0.0f) {
            f = this.mLastWallpaperX;
        }
        float f2 = this.mLastWallpaperXStep >= 0.0f ? this.mLastWallpaperXStep : -1.0f;
        int i4 = (windowState.mFrame.right - windowState.mFrame.left) - i;
        if (i4 > 0) {
            i3 = -((int) ((i4 * f) + 0.5f));
        } else {
            i3 = 0;
        }
        if (this.mLastWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
            i3 += this.mLastWallpaperDisplayOffsetX;
        }
        if (windowState.mWallpaperX != f || windowState.mWallpaperXStep != f2) {
            windowState.mWallpaperX = f;
            windowState.mWallpaperXStep = f2;
            z2 = true;
        } else {
            z2 = false;
        }
        float f3 = this.mLastWallpaperY >= 0.0f ? this.mLastWallpaperY : 0.5f;
        float f4 = this.mLastWallpaperYStep >= 0.0f ? this.mLastWallpaperYStep : -1.0f;
        int i5 = (windowState.mFrame.bottom - windowState.mFrame.top) - i2;
        int i6 = i5 > 0 ? -((int) ((i5 * f3) + 0.5f)) : 0;
        if (this.mLastWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            i6 += this.mLastWallpaperDisplayOffsetY;
        }
        if (windowState.mWallpaperY != f3 || windowState.mWallpaperYStep != f4) {
            windowState.mWallpaperY = f3;
            windowState.mWallpaperYStep = f4;
            z2 = true;
        }
        boolean wallpaperOffset = windowState.mWinAnimator.setWallpaperOffset(i3, i6);
        if (z2 && (windowState.mAttrs.privateFlags & 4) != 0) {
            try {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v("WindowManager", "Report new wp offset " + windowState + " x=" + windowState.mWallpaperX + " y=" + windowState.mWallpaperY);
                }
                if (z) {
                    this.mWaitingOnWallpaper = windowState;
                }
                windowState.mClient.dispatchWallpaperOffsets(windowState.mWallpaperX, windowState.mWallpaperY, windowState.mWallpaperXStep, windowState.mWallpaperYStep, z);
                if (z && this.mWaitingOnWallpaper != null) {
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    if (this.mLastWallpaperTimeoutTime + 10000 < jUptimeMillis) {
                        try {
                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                                Slog.v("WindowManager", "Waiting for offset complete...");
                            }
                            this.mService.mWindowMap.wait(WALLPAPER_TIMEOUT);
                        } catch (InterruptedException e) {
                        }
                        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                            Slog.v("WindowManager", "Offset complete!");
                        }
                        if (WALLPAPER_TIMEOUT + jUptimeMillis < SystemClock.uptimeMillis()) {
                            Slog.i("WindowManager", "Timeout waiting for wallpaper to offset: " + windowState);
                            this.mLastWallpaperTimeoutTime = jUptimeMillis;
                        }
                    }
                    this.mWaitingOnWallpaper = null;
                }
            } catch (RemoteException e2) {
            }
        }
        return wallpaperOffset;
    }

    void setWindowWallpaperPosition(WindowState windowState, float f, float f2, float f3, float f4) {
        if (windowState.mWallpaperX != f || windowState.mWallpaperY != f2) {
            windowState.mWallpaperX = f;
            windowState.mWallpaperY = f2;
            windowState.mWallpaperXStep = f3;
            windowState.mWallpaperYStep = f4;
            updateWallpaperOffsetLocked(windowState, true);
        }
    }

    void setWindowWallpaperDisplayOffset(WindowState windowState, int i, int i2) {
        if (windowState.mWallpaperDisplayOffsetX != i || windowState.mWallpaperDisplayOffsetY != i2) {
            windowState.mWallpaperDisplayOffsetX = i;
            windowState.mWallpaperDisplayOffsetY = i2;
            updateWallpaperOffsetLocked(windowState, true);
        }
    }

    Bundle sendWindowWallpaperCommand(WindowState windowState, String str, int i, int i2, int i3, Bundle bundle, boolean z) {
        if (windowState == this.mWallpaperTarget || windowState == this.mPrevWallpaperTarget) {
            for (int size = this.mWallpaperTokens.size() - 1; size >= 0; size--) {
                this.mWallpaperTokens.get(size).sendWindowWallpaperCommand(str, i, i2, i3, bundle, z);
            }
            return null;
        }
        return null;
    }

    private void updateWallpaperOffsetLocked(WindowState windowState, boolean z) {
        DisplayContent displayContent = windowState.getDisplayContent();
        if (displayContent == null) {
            return;
        }
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        int i = displayInfo.logicalWidth;
        int i2 = displayInfo.logicalHeight;
        WindowState windowState2 = this.mWallpaperTarget;
        if (windowState2 != null) {
            if (windowState2.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = windowState2.mWallpaperX;
            } else if (windowState.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = windowState.mWallpaperX;
            }
            if (windowState2.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = windowState2.mWallpaperY;
            } else if (windowState.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = windowState.mWallpaperY;
            }
            if (windowState2.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = windowState2.mWallpaperDisplayOffsetX;
            } else if (windowState.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = windowState.mWallpaperDisplayOffsetX;
            }
            if (windowState2.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = windowState2.mWallpaperDisplayOffsetY;
            } else if (windowState.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = windowState.mWallpaperDisplayOffsetY;
            }
            if (windowState2.mWallpaperXStep >= 0.0f) {
                this.mLastWallpaperXStep = windowState2.mWallpaperXStep;
            } else if (windowState.mWallpaperXStep >= 0.0f) {
                this.mLastWallpaperXStep = windowState.mWallpaperXStep;
            }
            if (windowState2.mWallpaperYStep >= 0.0f) {
                this.mLastWallpaperYStep = windowState2.mWallpaperYStep;
            } else if (windowState.mWallpaperYStep >= 0.0f) {
                this.mLastWallpaperYStep = windowState.mWallpaperYStep;
            }
        }
        for (int size = this.mWallpaperTokens.size() - 1; size >= 0; size--) {
            this.mWallpaperTokens.get(size).updateWallpaperOffset(i, i2, z);
        }
    }

    void clearLastWallpaperTimeoutTime() {
        this.mLastWallpaperTimeoutTime = 0L;
    }

    void wallpaperCommandComplete(IBinder iBinder) {
        if (this.mWaitingOnWallpaper != null && this.mWaitingOnWallpaper.mClient.asBinder() == iBinder) {
            this.mWaitingOnWallpaper = null;
            this.mService.mWindowMap.notifyAll();
        }
    }

    void wallpaperOffsetsComplete(IBinder iBinder) {
        if (this.mWaitingOnWallpaper != null && this.mWaitingOnWallpaper.mClient.asBinder() == iBinder) {
            this.mWaitingOnWallpaper = null;
            this.mService.mWindowMap.notifyAll();
        }
    }

    private void findWallpaperTarget(DisplayContent displayContent) {
        this.mFindResults.reset();
        if (displayContent.isStackVisible(5)) {
            this.mFindResults.setUseTopWallpaperAsTarget(true);
        }
        displayContent.forAllWindows(this.mFindWallpaperTargetFunction, true);
        if (this.mFindResults.wallpaperTarget == null && this.mFindResults.useTopWallpaperAsTarget) {
            this.mFindResults.setWallpaperTarget(this.mFindResults.topWallpaper);
        }
    }

    private boolean isFullscreen(WindowManager.LayoutParams layoutParams) {
        return layoutParams.x == 0 && layoutParams.y == 0 && layoutParams.width == -1 && layoutParams.height == -1;
    }

    private void updateWallpaperWindowsTarget(DisplayContent displayContent, FindWallpaperTargetResult findWallpaperTargetResult) {
        WindowState windowState = findWallpaperTargetResult.wallpaperTarget;
        if (this.mWallpaperTarget == windowState || (this.mPrevWallpaperTarget != null && this.mPrevWallpaperTarget == windowState)) {
            if (this.mPrevWallpaperTarget != null && !this.mPrevWallpaperTarget.isAnimatingLw()) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.v("WindowManager", "No longer animating wallpaper targets!");
                }
                this.mPrevWallpaperTarget = null;
                this.mWallpaperTarget = windowState;
                return;
            }
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.v("WindowManager", "New wallpaper target: " + windowState + " prevTarget: " + this.mWallpaperTarget);
        }
        this.mPrevWallpaperTarget = null;
        final WindowState windowState2 = this.mWallpaperTarget;
        this.mWallpaperTarget = windowState;
        if (windowState == null || windowState2 == null) {
            return;
        }
        boolean zIsAnimatingLw = windowState2.isAnimatingLw();
        boolean zIsAnimatingLw2 = windowState.isAnimatingLw();
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.v("WindowManager", "New animation: " + zIsAnimatingLw2 + " old animation: " + zIsAnimatingLw);
        }
        if (!zIsAnimatingLw2 || !zIsAnimatingLw || displayContent.getWindow(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return WallpaperController.lambda$updateWallpaperWindowsTarget$1(windowState2, (WindowState) obj);
            }
        }) == null) {
            return;
        }
        boolean z = false;
        boolean z2 = windowState.mAppToken != null && windowState.mAppToken.hiddenRequested;
        if (windowState2.mAppToken != null && windowState2.mAppToken.hiddenRequested) {
            z = true;
        }
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.v("WindowManager", "Animating wallpapers: old: " + windowState2 + " hidden=" + z + " new: " + windowState + " hidden=" + z2);
        }
        this.mPrevWallpaperTarget = windowState2;
        if (z2 && !z) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                Slog.v("WindowManager", "Old wallpaper still the target.");
            }
            this.mWallpaperTarget = windowState2;
        } else if (z2 == z && !this.mService.mOpeningApps.contains(windowState.mAppToken) && (this.mService.mOpeningApps.contains(windowState2.mAppToken) || this.mService.mClosingApps.contains(windowState2.mAppToken))) {
            this.mWallpaperTarget = windowState2;
        }
        findWallpaperTargetResult.setWallpaperTarget(windowState);
    }

    static boolean lambda$updateWallpaperWindowsTarget$1(WindowState windowState, WindowState windowState2) {
        return windowState2 == windowState;
    }

    private void updateWallpaperTokens(boolean z) {
        for (int size = this.mWallpaperTokens.size() - 1; size >= 0; size--) {
            WallpaperWindowToken wallpaperWindowToken = this.mWallpaperTokens.get(size);
            wallpaperWindowToken.updateWallpaperWindows(z);
            wallpaperWindowToken.getDisplayContent().assignWindowLayers(false);
        }
    }

    void adjustWallpaperWindows(DisplayContent displayContent) {
        boolean z = false;
        this.mService.mRoot.mWallpaperMayChange = false;
        findWallpaperTarget(displayContent);
        updateWallpaperWindowsTarget(displayContent, this.mFindResults);
        if (this.mWallpaperTarget != null && isWallpaperVisible(this.mWallpaperTarget)) {
            z = true;
        }
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v("WindowManager", "Wallpaper visibility: " + z);
        }
        if (z) {
            if (this.mWallpaperTarget.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = this.mWallpaperTarget.mWallpaperX;
                this.mLastWallpaperXStep = this.mWallpaperTarget.mWallpaperXStep;
            }
            if (this.mWallpaperTarget.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = this.mWallpaperTarget.mWallpaperY;
                this.mLastWallpaperYStep = this.mWallpaperTarget.mWallpaperYStep;
            }
            if (this.mWallpaperTarget.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = this.mWallpaperTarget.mWallpaperDisplayOffsetX;
            }
            if (this.mWallpaperTarget.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = this.mWallpaperTarget.mWallpaperDisplayOffsetY;
            }
        }
        updateWallpaperTokens(z);
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.d("WindowManager", "New wallpaper: target=" + this.mWallpaperTarget + " prev=" + this.mPrevWallpaperTarget);
        }
    }

    boolean processWallpaperDrawPendingTimeout() {
        if (this.mWallpaperDrawState == 1) {
            this.mWallpaperDrawState = 2;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "*** WALLPAPER DRAW TIMEOUT");
            }
            if (this.mService.getRecentsAnimationController() != null) {
                this.mService.getRecentsAnimationController().cancelAnimation(2, "wallpaperDrawPendingTimeout");
            }
            return true;
        }
        return false;
    }

    boolean wallpaperTransitionReady() {
        boolean z;
        boolean z2 = true;
        int size = this.mWallpaperTokens.size() - 1;
        while (true) {
            if (size >= 0) {
                if (!this.mWallpaperTokens.get(size).hasVisibleNotDrawnWallpaper()) {
                    size--;
                } else {
                    z = this.mWallpaperDrawState == 2;
                    if (this.mWallpaperDrawState == 0) {
                        this.mWallpaperDrawState = 1;
                        this.mService.mH.removeMessages(39);
                        this.mService.mH.sendEmptyMessageDelayed(39, 500L);
                    }
                    if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                        Slog.v("WindowManager", "Wallpaper should be visible but has not been drawn yet. mWallpaperDrawState=" + this.mWallpaperDrawState);
                    }
                    z2 = false;
                }
            } else {
                z = true;
                break;
            }
        }
        if (z2) {
            this.mWallpaperDrawState = 0;
            this.mService.mH.removeMessages(39);
        }
        return z;
    }

    void adjustWallpaperWindowsForAppTransitionIfNeeded(DisplayContent displayContent, ArraySet<AppWindowToken> arraySet) {
        boolean z = true;
        if ((displayContent.pendingLayoutChanges & 4) == 0) {
            int size = arraySet.size() - 1;
            while (true) {
                if (size >= 0) {
                    if (arraySet.valueAt(size).windowsCanBeWallpaperTarget()) {
                        break;
                    } else {
                        size--;
                    }
                } else {
                    z = false;
                    break;
                }
            }
        }
        if (z) {
            adjustWallpaperWindows(displayContent);
        }
    }

    void addWallpaperToken(WallpaperWindowToken wallpaperWindowToken) {
        this.mWallpaperTokens.add(wallpaperWindowToken);
    }

    void removeWallpaperToken(WallpaperWindowToken wallpaperWindowToken) {
        this.mWallpaperTokens.remove(wallpaperWindowToken);
    }

    Bitmap screenshotWallpaperLocked() {
        if (!this.mService.mPolicy.isScreenOn()) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.i("WindowManager", "Attempted to take screenshot while display was off.");
            }
            return null;
        }
        WindowState topVisibleWallpaper = getTopVisibleWallpaper();
        if (topVisibleWallpaper == null) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.i("WindowManager", "No visible wallpaper to screenshot");
            }
            return null;
        }
        Rect bounds = topVisibleWallpaper.getBounds();
        bounds.offsetTo(0, 0);
        GraphicBuffer graphicBufferCaptureLayers = SurfaceControl.captureLayers(topVisibleWallpaper.getSurfaceControl().getHandle(), bounds, 1.0f);
        if (graphicBufferCaptureLayers == null) {
            Slog.w("WindowManager", "Failed to screenshot wallpaper");
            return null;
        }
        return Bitmap.createHardwareBitmap(graphicBufferCaptureLayers);
    }

    private WindowState getTopVisibleWallpaper() {
        this.mTmpTopWallpaper = null;
        for (int size = this.mWallpaperTokens.size() - 1; size >= 0; size--) {
            this.mWallpaperTokens.get(size).forAllWindows(new ToBooleanFunction() {
                public final boolean apply(Object obj) {
                    return WallpaperController.lambda$getTopVisibleWallpaper$2(this.f$0, (WindowState) obj);
                }
            }, true);
        }
        return this.mTmpTopWallpaper;
    }

    public static boolean lambda$getTopVisibleWallpaper$2(WallpaperController wallpaperController, WindowState windowState) {
        WindowStateAnimator windowStateAnimator = windowState.mWinAnimator;
        if (windowStateAnimator != null && windowStateAnimator.getShown() && windowStateAnimator.mLastAlpha > 0.0f) {
            wallpaperController.mTmpTopWallpaper = windowState;
            return true;
        }
        return false;
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mWallpaperTarget=");
        printWriter.println(this.mWallpaperTarget);
        if (this.mPrevWallpaperTarget != null) {
            printWriter.print(str);
            printWriter.print("mPrevWallpaperTarget=");
            printWriter.println(this.mPrevWallpaperTarget);
        }
        printWriter.print(str);
        printWriter.print("mLastWallpaperX=");
        printWriter.print(this.mLastWallpaperX);
        printWriter.print(" mLastWallpaperY=");
        printWriter.println(this.mLastWallpaperY);
        if (this.mLastWallpaperDisplayOffsetX != Integer.MIN_VALUE || this.mLastWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            printWriter.print(str);
            printWriter.print("mLastWallpaperDisplayOffsetX=");
            printWriter.print(this.mLastWallpaperDisplayOffsetX);
            printWriter.print(" mLastWallpaperDisplayOffsetY=");
            printWriter.println(this.mLastWallpaperDisplayOffsetY);
        }
    }

    private static final class FindWallpaperTargetResult {
        boolean resetTopWallpaper;
        WindowState topWallpaper;
        boolean useTopWallpaperAsTarget;
        WindowState wallpaperTarget;

        private FindWallpaperTargetResult() {
            this.topWallpaper = null;
            this.useTopWallpaperAsTarget = false;
            this.wallpaperTarget = null;
            this.resetTopWallpaper = false;
        }

        void setTopWallpaper(WindowState windowState) {
            this.topWallpaper = windowState;
        }

        void setWallpaperTarget(WindowState windowState) {
            this.wallpaperTarget = windowState;
        }

        void setUseTopWallpaperAsTarget(boolean z) {
            this.useTopWallpaperAsTarget = z;
        }

        void reset() {
            this.topWallpaper = null;
            this.wallpaperTarget = null;
            this.useTopWallpaperAsTarget = false;
            this.resetTopWallpaper = false;
        }
    }
}
