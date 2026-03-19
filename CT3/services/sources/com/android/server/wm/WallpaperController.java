package com.android.server.wm;

import android.app.IWallpaperManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.DisplayInfo;
import com.android.server.pm.PackageManagerService;
import com.mediatek.multiwindow.MultiWindowManager;
import java.io.PrintWriter;
import java.util.ArrayList;

class WallpaperController {
    private static final String TAG = "WindowManager";
    private static final int WALLPAPER_DRAW_NORMAL = 0;
    private static final int WALLPAPER_DRAW_PENDING = 1;
    private static final long WALLPAPER_DRAW_PENDING_TIMEOUT_DURATION = 500;
    private static final int WALLPAPER_DRAW_TIMEOUT = 2;
    private static final long WALLPAPER_TIMEOUT = 150;
    private static final long WALLPAPER_TIMEOUT_RECOVERY = 10000;
    private long mLastWallpaperTimeoutTime;
    private final WindowManagerService mService;
    WindowState mWaitingOnWallpaper;
    private int mWallpaperAnimLayerAdjustment;
    private IWallpaperManager mWallpaperManagerService;
    private final ArrayList<WindowToken> mWallpaperTokens = new ArrayList<>();
    private WindowState mWallpaperTarget = null;
    private WindowState mLowerWallpaperTarget = null;
    private WindowState mUpperWallpaperTarget = null;
    private float mLastWallpaperX = -1.0f;
    private float mLastWallpaperY = -1.0f;
    private float mLastWallpaperXStep = -1.0f;
    private float mLastWallpaperYStep = -1.0f;
    private int mLastWallpaperDisplayOffsetX = Integer.MIN_VALUE;
    private int mLastWallpaperDisplayOffsetY = Integer.MIN_VALUE;
    private WindowState mDeferredHideWallpaper = null;
    private int mWallpaperDrawState = 0;
    private final FindWallpaperTargetResult mFindResults = new FindWallpaperTargetResult(null);

    public WallpaperController(WindowManagerService service) {
        this.mService = service;
    }

    WindowState getWallpaperTarget() {
        return this.mWallpaperTarget;
    }

    WindowState getLowerWallpaperTarget() {
        return this.mLowerWallpaperTarget;
    }

    WindowState getUpperWallpaperTarget() {
        return this.mUpperWallpaperTarget;
    }

    boolean isWallpaperTarget(WindowState win) {
        return win == this.mWallpaperTarget;
    }

    boolean isBelowWallpaperTarget(WindowState win) {
        return this.mWallpaperTarget != null && this.mWallpaperTarget.mLayer >= win.mBaseLayer;
    }

    boolean isWallpaperVisible() {
        return isWallpaperVisible(this.mWallpaperTarget);
    }

    private boolean isWallpaperVisible(WindowState wallpaperTarget) {
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v(TAG, "Wallpaper vis: target " + wallpaperTarget + ", obscured=" + (wallpaperTarget != null ? Boolean.toString(wallpaperTarget.mObscured) : "??") + " anim=" + ((wallpaperTarget == null || wallpaperTarget.mAppToken == null) ? null : wallpaperTarget.mAppToken.mAppAnimator.animation) + " upper=" + this.mUpperWallpaperTarget + " lower=" + this.mLowerWallpaperTarget);
        }
        if ((wallpaperTarget == null || (wallpaperTarget.mObscured && (wallpaperTarget.mAppToken == null || wallpaperTarget.mAppToken.mAppAnimator.animation == null))) && this.mUpperWallpaperTarget == null) {
            return this.mLowerWallpaperTarget != null;
        }
        return true;
    }

    boolean isWallpaperTargetAnimating() {
        return (this.mWallpaperTarget == null || !this.mWallpaperTarget.mWinAnimator.isAnimationSet() || this.mWallpaperTarget.mWinAnimator.isDummyAnimation()) ? false : true;
    }

    void updateWallpaperVisibility() {
        DisplayContent displayContent = this.mWallpaperTarget.getDisplayContent();
        if (displayContent == null) {
            return;
        }
        boolean visible = isWallpaperVisible(this.mWallpaperTarget);
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WindowToken token = this.mWallpaperTokens.get(curTokenNdx);
            if (token.hidden == visible) {
                token.hidden = !visible;
                displayContent.layoutNeeded = true;
            }
            WindowList windows = token.windows;
            for (int wallpaperNdx = windows.size() - 1; wallpaperNdx >= 0; wallpaperNdx--) {
                WindowState wallpaper = windows.get(wallpaperNdx);
                if (visible) {
                    updateWallpaperOffset(wallpaper, dw, dh, false);
                }
                dispatchWallpaperVisibility(wallpaper, visible);
            }
        }
    }

    void hideDeferredWallpapersIfNeeded() {
        if (this.mDeferredHideWallpaper == null) {
            return;
        }
        hideWallpapers(this.mDeferredHideWallpaper);
        this.mDeferredHideWallpaper = null;
    }

    void hideWallpapers(WindowState winGoingAway) {
        if (this.mWallpaperTarget == null || (this.mWallpaperTarget == winGoingAway && this.mLowerWallpaperTarget == null)) {
            if (this.mService.mAppTransition.isRunning()) {
                this.mDeferredHideWallpaper = winGoingAway;
                return;
            }
            boolean wasDeferred = this.mDeferredHideWallpaper == winGoingAway;
            for (int i = this.mWallpaperTokens.size() - 1; i >= 0; i--) {
                WindowToken token = this.mWallpaperTokens.get(i);
                for (int j = token.windows.size() - 1; j >= 0; j--) {
                    WindowState wallpaper = token.windows.get(j);
                    WindowStateAnimator winAnimator = wallpaper.mWinAnimator;
                    if (!winAnimator.mLastHidden || wasDeferred) {
                        winAnimator.hide("hideWallpapers");
                        dispatchWallpaperVisibility(wallpaper, false);
                        DisplayContent displayContent = wallpaper.getDisplayContent();
                        if (displayContent != null) {
                            displayContent.pendingLayoutChanges |= 4;
                        }
                    }
                }
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT && !token.hidden) {
                    Slog.d(TAG, "Hiding wallpaper " + token + " from " + winGoingAway + " target=" + this.mWallpaperTarget + " lower=" + this.mLowerWallpaperTarget + "\n" + Debug.getCallers(5, "  "));
                }
                token.hidden = true;
            }
        }
    }

    void dispatchWallpaperVisibility(WindowState wallpaper, boolean visible) {
        if (wallpaper.mWallpaperVisible != visible) {
            if (this.mDeferredHideWallpaper == null || visible) {
                wallpaper.mWallpaperVisible = visible;
                try {
                    if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                        Slog.v(TAG, "Updating vis of wallpaper " + wallpaper + ": " + visible + " from:\n" + Debug.getCallers(4, "  "));
                    }
                    wallpaper.mClient.dispatchAppVisibility(visible);
                    if (SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1")) {
                        if (this.mWallpaperManagerService == null) {
                            this.mWallpaperManagerService = IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper"));
                        }
                        if (this.mWallpaperManagerService != null) {
                            this.mWallpaperManagerService.onVisibilityChanged(visible);
                        }
                    }
                } catch (RemoteException e) {
                }
            }
        }
    }

    boolean updateWallpaperOffset(WindowState wallpaperWin, int dw, int dh, boolean sync) {
        boolean rawChanged = false;
        float wpx = this.mLastWallpaperX >= 0.0f ? this.mLastWallpaperX : 0.5f;
        float wpxs = this.mLastWallpaperXStep >= 0.0f ? this.mLastWallpaperXStep : -1.0f;
        int availw = (wallpaperWin.mFrame.right - wallpaperWin.mFrame.left) - dw;
        int offset = availw > 0 ? -((int) ((availw * wpx) + 0.5f)) : 0;
        if (this.mLastWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
            offset += this.mLastWallpaperDisplayOffsetX;
        }
        boolean changed = wallpaperWin.mXOffset != offset;
        if (changed) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v(TAG, "Update wallpaper " + wallpaperWin + " x: " + offset);
            }
            wallpaperWin.mXOffset = offset;
        }
        if (wallpaperWin.mWallpaperX != wpx || wallpaperWin.mWallpaperXStep != wpxs) {
            wallpaperWin.mWallpaperX = wpx;
            wallpaperWin.mWallpaperXStep = wpxs;
            rawChanged = true;
        }
        float wpy = this.mLastWallpaperY >= 0.0f ? this.mLastWallpaperY : 0.5f;
        float wpys = this.mLastWallpaperYStep >= 0.0f ? this.mLastWallpaperYStep : -1.0f;
        int availh = (wallpaperWin.mFrame.bottom - wallpaperWin.mFrame.top) - dh;
        int offset2 = availh > 0 ? -((int) ((availh * wpy) + 0.5f)) : 0;
        if (this.mLastWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            offset2 += this.mLastWallpaperDisplayOffsetY;
        }
        if (wallpaperWin.mYOffset != offset2) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v(TAG, "Update wallpaper " + wallpaperWin + " y: " + offset2);
            }
            changed = true;
            wallpaperWin.mYOffset = offset2;
        }
        if (wallpaperWin.mWallpaperY != wpy || wallpaperWin.mWallpaperYStep != wpys) {
            wallpaperWin.mWallpaperY = wpy;
            wallpaperWin.mWallpaperYStep = wpys;
            rawChanged = true;
        }
        if (rawChanged && (wallpaperWin.mAttrs.privateFlags & 4) != 0) {
            try {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v(TAG, "Report new wp offset " + wallpaperWin + " x=" + wallpaperWin.mWallpaperX + " y=" + wallpaperWin.mWallpaperY);
                }
                if (sync) {
                    this.mWaitingOnWallpaper = wallpaperWin;
                }
                wallpaperWin.mClient.dispatchWallpaperOffsets(wallpaperWin.mWallpaperX, wallpaperWin.mWallpaperY, wallpaperWin.mWallpaperXStep, wallpaperWin.mWallpaperYStep, sync);
                if (sync && this.mWaitingOnWallpaper != null) {
                    long start = SystemClock.uptimeMillis();
                    if (this.mLastWallpaperTimeoutTime + 10000 < start) {
                        try {
                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                                Slog.v(TAG, "Waiting for offset complete...");
                            }
                            this.mService.mWindowMap.wait(WALLPAPER_TIMEOUT);
                        } catch (InterruptedException e) {
                        }
                        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                            Slog.v(TAG, "Offset complete!");
                        }
                        if (WALLPAPER_TIMEOUT + start < SystemClock.uptimeMillis()) {
                            Slog.i(TAG, "Timeout waiting for wallpaper to offset: " + wallpaperWin);
                            this.mLastWallpaperTimeoutTime = start;
                        }
                    }
                    this.mWaitingOnWallpaper = null;
                }
            } catch (RemoteException e2) {
            }
        }
        return changed;
    }

    void setWindowWallpaperPosition(WindowState window, float x, float y, float xStep, float yStep) {
        if (window.mWallpaperX == x && window.mWallpaperY == y) {
            return;
        }
        window.mWallpaperX = x;
        window.mWallpaperY = y;
        window.mWallpaperXStep = xStep;
        window.mWallpaperYStep = yStep;
        updateWallpaperOffsetLocked(window, true);
    }

    void setWindowWallpaperDisplayOffset(WindowState window, int x, int y) {
        if (window.mWallpaperDisplayOffsetX == x && window.mWallpaperDisplayOffsetY == y) {
            return;
        }
        window.mWallpaperDisplayOffsetX = x;
        window.mWallpaperDisplayOffsetY = y;
        updateWallpaperOffsetLocked(window, true);
    }

    Bundle sendWindowWallpaperCommand(WindowState window, String action, int x, int y, int z, Bundle extras, boolean sync) {
        if (window == this.mWallpaperTarget || window == this.mLowerWallpaperTarget || window == this.mUpperWallpaperTarget) {
            for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
                WindowList windows = this.mWallpaperTokens.get(curTokenNdx).windows;
                for (int wallpaperNdx = windows.size() - 1; wallpaperNdx >= 0; wallpaperNdx--) {
                    WindowState wallpaper = windows.get(wallpaperNdx);
                    try {
                        wallpaper.mClient.dispatchWallpaperCommand(action, x, y, z, extras, sync);
                        sync = false;
                    } catch (RemoteException e) {
                    }
                }
            }
            if (sync) {
            }
            return null;
        }
        return null;
    }

    void updateWallpaperOffsetLocked(WindowState changingTarget, boolean sync) {
        DisplayContent displayContent = changingTarget.getDisplayContent();
        if (displayContent == null) {
            return;
        }
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        WindowState target = this.mWallpaperTarget;
        if (target != null) {
            if (target.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = target.mWallpaperX;
            } else if (changingTarget.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = changingTarget.mWallpaperX;
            }
            if (target.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = target.mWallpaperY;
            } else if (changingTarget.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = changingTarget.mWallpaperY;
            }
            if (target.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = target.mWallpaperDisplayOffsetX;
            } else if (changingTarget.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = changingTarget.mWallpaperDisplayOffsetX;
            }
            if (target.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = target.mWallpaperDisplayOffsetY;
            } else if (changingTarget.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = changingTarget.mWallpaperDisplayOffsetY;
            }
            if (target.mWallpaperXStep >= 0.0f) {
                this.mLastWallpaperXStep = target.mWallpaperXStep;
            } else if (changingTarget.mWallpaperXStep >= 0.0f) {
                this.mLastWallpaperXStep = changingTarget.mWallpaperXStep;
            }
            if (target.mWallpaperYStep >= 0.0f) {
                this.mLastWallpaperYStep = target.mWallpaperYStep;
            } else if (changingTarget.mWallpaperYStep >= 0.0f) {
                this.mLastWallpaperYStep = changingTarget.mWallpaperYStep;
            }
        }
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WindowList windows = this.mWallpaperTokens.get(curTokenNdx).windows;
            for (int wallpaperNdx = windows.size() - 1; wallpaperNdx >= 0; wallpaperNdx--) {
                WindowState wallpaper = windows.get(wallpaperNdx);
                if (updateWallpaperOffset(wallpaper, dw, dh, sync)) {
                    WindowStateAnimator winAnimator = wallpaper.mWinAnimator;
                    winAnimator.computeShownFrameLocked();
                    winAnimator.setWallpaperOffset(wallpaper.mShownPosition);
                    sync = false;
                }
            }
        }
    }

    void clearLastWallpaperTimeoutTime() {
        this.mLastWallpaperTimeoutTime = 0L;
    }

    void wallpaperCommandComplete(IBinder window) {
        if (this.mWaitingOnWallpaper == null || this.mWaitingOnWallpaper.mClient.asBinder() != window) {
            return;
        }
        this.mWaitingOnWallpaper = null;
        this.mService.mWindowMap.notifyAll();
    }

    void wallpaperOffsetsComplete(IBinder window) {
        if (this.mWaitingOnWallpaper == null || this.mWaitingOnWallpaper.mClient.asBinder() != window) {
            return;
        }
        this.mWaitingOnWallpaper = null;
        this.mService.mWindowMap.notifyAll();
    }

    int getAnimLayerAdjustment() {
        return this.mWallpaperAnimLayerAdjustment;
    }

    void setAnimLayerAdjustment(WindowState win, int adj) {
        if (win != this.mWallpaperTarget || this.mLowerWallpaperTarget != null) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_LAYERS || WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v(TAG, "Setting wallpaper layer adj to " + adj);
        }
        this.mWallpaperAnimLayerAdjustment = adj;
        for (int i = this.mWallpaperTokens.size() - 1; i >= 0; i--) {
            WindowList windows = this.mWallpaperTokens.get(i).windows;
            for (int j = windows.size() - 1; j >= 0; j--) {
                WindowState wallpaper = windows.get(j);
                wallpaper.mWinAnimator.mAnimLayer = wallpaper.mLayer + adj;
                if (WindowManagerDebugConfig.DEBUG_LAYERS || WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v(TAG, "setWallpaper win " + wallpaper + " anim layer: " + wallpaper.mWinAnimator.mAnimLayer);
                }
            }
        }
    }

    private void findWallpaperTarget(WindowList windows, FindWallpaperTargetResult result) {
        WindowAnimator winAnimator = this.mService.mAnimator;
        result.reset();
        WindowState w = null;
        int windowDetachedI = -1;
        boolean resetTopWallpaper = false;
        boolean inFreeformSpace = false;
        boolean replacing = false;
        for (int i = windows.size() - 1; i >= 0; i--) {
            w = windows.get(i);
            if (w.mAttrs.type != 2013) {
                resetTopWallpaper = true;
                if (w == winAnimator.mWindowDetachedWallpaper || w.mAppToken == null || !w.mAppToken.hidden || w.mAppToken.mAppAnimator.animation != null) {
                    if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                        Slog.v(TAG, "Win #" + i + " " + w + ": isOnScreen=" + w.isOnScreen() + " mDrawState=" + w.mWinAnimator.mDrawState);
                    }
                    if (!inFreeformSpace) {
                        TaskStack stack = w.getStack();
                        inFreeformSpace = stack != null && stack.mStackId == 2;
                    }
                    replacing = !replacing ? w.mWillReplaceWindow : true;
                    boolean hasWallpaper = (w.mAttrs.flags & PackageManagerService.DumpState.DUMP_DEXOPT) == 0 ? w.mAppToken != null ? w.mWinAnimator.mKeyguardGoingAwayWithWallpaper : false : true;
                    if (hasWallpaper && w.isOnScreen() && (this.mWallpaperTarget == w || w.isDrawFinishedLw())) {
                        if (!MultiWindowManager.isSupported() || !inFreeformSpace) {
                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                                Slog.v(TAG, "Found wallpaper target: #" + i + "=" + w);
                            }
                            result.setWallpaperTarget(w, i);
                            if (w != this.mWallpaperTarget || !w.mWinAnimator.isAnimationSet()) {
                                break;
                            } else if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                                Slog.v(TAG, "Win " + w + ": token animating, looking behind.");
                            }
                        }
                    } else if (w == winAnimator.mWindowDetachedWallpaper) {
                        windowDetachedI = i;
                    }
                } else if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v(TAG, "Skipping hidden and not animating token: " + w);
                }
            } else if (result.topWallpaper == null || resetTopWallpaper) {
                result.setTopWallpaper(w, i);
                resetTopWallpaper = false;
            }
        }
        if (result.wallpaperTarget == null && windowDetachedI >= 0) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                Slog.v(TAG, "Found animating detached wallpaper activity: #" + windowDetachedI + "=" + w);
            }
            result.setWallpaperTarget(w, windowDetachedI);
        }
        if (result.wallpaperTarget == null) {
            if (inFreeformSpace || (replacing && this.mWallpaperTarget != null)) {
                result.setWallpaperTarget(result.topWallpaper, result.topWallpaperIndex);
            }
        }
    }

    private boolean updateWallpaperWindowsTarget(WindowList windows, FindWallpaperTargetResult result) {
        boolean targetChanged = false;
        WindowState wallpaperTarget = result.wallpaperTarget;
        int wallpaperTargetIndex = result.wallpaperTargetIndex;
        if (this.mWallpaperTarget != wallpaperTarget && (this.mLowerWallpaperTarget == null || this.mLowerWallpaperTarget != wallpaperTarget)) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                Slog.v(TAG, "New wallpaper target: " + wallpaperTarget + " oldTarget: " + this.mWallpaperTarget);
            }
            this.mLowerWallpaperTarget = null;
            this.mUpperWallpaperTarget = null;
            WindowState oldW = this.mWallpaperTarget;
            this.mWallpaperTarget = wallpaperTarget;
            targetChanged = true;
            if (wallpaperTarget != null && oldW != null) {
                boolean oldAnim = oldW.isAnimatingLw();
                boolean foundAnim = wallpaperTarget.isAnimatingLw();
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.v(TAG, "New animation: " + foundAnim + " old animation: " + oldAnim);
                }
                if (foundAnim && oldAnim) {
                    int oldI = windows.indexOf(oldW);
                    if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                        Slog.v(TAG, "New i: " + wallpaperTargetIndex + " old i: " + oldI);
                    }
                    if (oldI >= 0) {
                        if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                            Slog.v(TAG, "Animating wallpapers: old#" + oldI + "=" + oldW + "; new#" + wallpaperTargetIndex + "=" + wallpaperTarget);
                        }
                        if (wallpaperTarget.mAppToken != null && wallpaperTarget.mAppToken.hiddenRequested) {
                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                                Slog.v(TAG, "Old wallpaper still the target.");
                            }
                            this.mWallpaperTarget = oldW;
                            wallpaperTarget = oldW;
                            wallpaperTargetIndex = oldI;
                        } else if (wallpaperTargetIndex > oldI) {
                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                                Slog.v(TAG, "Found target above old target.");
                            }
                            this.mUpperWallpaperTarget = wallpaperTarget;
                            this.mLowerWallpaperTarget = oldW;
                            wallpaperTarget = oldW;
                            wallpaperTargetIndex = oldI;
                        } else {
                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                                Slog.v(TAG, "Found target below old target.");
                            }
                            this.mUpperWallpaperTarget = oldW;
                            this.mLowerWallpaperTarget = wallpaperTarget;
                        }
                    }
                }
            }
        } else if (this.mLowerWallpaperTarget != null && (!this.mLowerWallpaperTarget.isAnimatingLw() || !this.mUpperWallpaperTarget.isAnimatingLw())) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                Slog.v(TAG, "No longer animating wallpaper targets!");
            }
            this.mLowerWallpaperTarget = null;
            this.mUpperWallpaperTarget = null;
            this.mWallpaperTarget = wallpaperTarget;
            targetChanged = true;
        }
        result.setWallpaperTarget(wallpaperTarget, wallpaperTargetIndex);
        return targetChanged;
    }

    boolean updateWallpaperWindowsTargetByLayer(WindowList windows, FindWallpaperTargetResult result) {
        int i = 0;
        WindowState wallpaperTarget = result.wallpaperTarget;
        int wallpaperTargetIndex = result.wallpaperTargetIndex;
        boolean visible = wallpaperTarget != null;
        if (visible) {
            visible = isWallpaperVisible(wallpaperTarget);
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v(TAG, "Wallpaper visibility: " + visible);
            }
            if (this.mLowerWallpaperTarget == null && wallpaperTarget.mAppToken != null) {
                i = wallpaperTarget.mAppToken.mAppAnimator.animLayerAdjustment;
            }
            this.mWallpaperAnimLayerAdjustment = i;
            int maxLayer = (this.mService.mPolicy.getMaxWallpaperLayer() * 10000) + 1000;
            while (wallpaperTargetIndex > 0) {
                WindowState wb = windows.get(wallpaperTargetIndex - 1);
                if (wb.mBaseLayer < maxLayer && wb.mAttachedWindow != wallpaperTarget && ((wallpaperTarget.mAttachedWindow == null || wb.mAttachedWindow != wallpaperTarget.mAttachedWindow) && (wb.mAttrs.type != 3 || wallpaperTarget.mToken == null || wb.mToken != wallpaperTarget.mToken))) {
                    break;
                }
                wallpaperTarget = wb;
                wallpaperTargetIndex--;
            }
        } else if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v(TAG, "No wallpaper target");
        }
        result.setWallpaperTarget(wallpaperTarget, wallpaperTargetIndex);
        return visible;
    }

    boolean updateWallpaperWindowsPlacement(WindowList windows, WindowState wallpaperTarget, int wallpaperTargetIndex, boolean visible) {
        DisplayInfo displayInfo = this.mService.getDefaultDisplayContentLocked().getDisplayInfo();
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        try {
            if (SystemProperties.get("ro.mtk_gmo_ram_optimize").equals("1") && !hasAnyWallpaperLock() && visible) {
                if (this.mWallpaperManagerService == null) {
                    this.mWallpaperManagerService = IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper"));
                }
                if (this.mWallpaperManagerService != null) {
                    this.mWallpaperManagerService.onVisibilityChanged(true);
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "WALLPAPER_SERVICE onVisibilityChanged error: ", e);
        }
        boolean changed = false;
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WindowToken token = this.mWallpaperTokens.get(curTokenNdx);
            if (token.hidden == visible) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.d(TAG, "Wallpaper token " + token + " hidden=" + (!visible));
                }
                token.hidden = !visible;
                this.mService.getDefaultDisplayContentLocked().layoutNeeded = true;
            }
            WindowList tokenWindows = token.windows;
            for (int wallpaperNdx = tokenWindows.size() - 1; wallpaperNdx >= 0; wallpaperNdx--) {
                WindowState wallpaper = tokenWindows.get(wallpaperNdx);
                if (visible) {
                    updateWallpaperOffset(wallpaper, dw, dh, false);
                }
                dispatchWallpaperVisibility(wallpaper, visible);
                wallpaper.mWinAnimator.mAnimLayer = wallpaper.mLayer + this.mWallpaperAnimLayerAdjustment;
                if (WindowManagerDebugConfig.DEBUG_LAYERS || WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    Slog.v(TAG, "adjustWallpaper win " + wallpaper + " anim layer: " + wallpaper.mWinAnimator.mAnimLayer);
                }
                if (wallpaper == wallpaperTarget) {
                    wallpaperTargetIndex--;
                    wallpaperTarget = wallpaperTargetIndex > 0 ? windows.get(wallpaperTargetIndex - 1) : null;
                } else {
                    int oldIndex = windows.indexOf(wallpaper);
                    if (oldIndex >= 0) {
                        if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                            Slog.v(TAG, "Wallpaper removing at " + oldIndex + ": " + wallpaper);
                        }
                        windows.remove(oldIndex);
                        this.mService.mWindowsChanged = true;
                        if (oldIndex < wallpaperTargetIndex) {
                            wallpaperTargetIndex--;
                        }
                    }
                    int insertionIndex = 0;
                    if (visible && wallpaperTarget != null) {
                        int type = wallpaperTarget.mAttrs.type;
                        int privateFlags = wallpaperTarget.mAttrs.privateFlags;
                        if ((privateFlags & 1024) != 0 || type == 2029) {
                            insertionIndex = windows.indexOf(wallpaperTarget);
                        }
                    }
                    if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT || WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT || (WindowManagerDebugConfig.DEBUG_ADD_REMOVE && oldIndex != insertionIndex)) {
                        Slog.v(TAG, "Moving wallpaper " + wallpaper + " from " + oldIndex + " to " + insertionIndex);
                    }
                    windows.add(insertionIndex, wallpaper);
                    this.mService.mWindowsChanged = true;
                    changed = true;
                }
            }
        }
        return changed;
    }

    boolean adjustWallpaperWindows() {
        WindowState wallpaperTarget;
        this.mService.mWindowPlacerLocked.mWallpaperMayChange = false;
        WindowList windows = this.mService.getDefaultWindowListLocked();
        findWallpaperTarget(windows, this.mFindResults);
        boolean targetChanged = updateWallpaperWindowsTarget(windows, this.mFindResults);
        boolean visible = updateWallpaperWindowsTargetByLayer(windows, this.mFindResults);
        WindowState wallpaperTarget2 = this.mFindResults.wallpaperTarget;
        int wallpaperTargetIndex = this.mFindResults.wallpaperTargetIndex;
        if (wallpaperTarget2 == null && this.mFindResults.topWallpaper != null) {
            wallpaperTarget = this.mFindResults.topWallpaper;
            wallpaperTargetIndex = this.mFindResults.topWallpaperIndex + 1;
        } else if (wallpaperTargetIndex > 0) {
            WindowState wallpaperTarget3 = windows.get(wallpaperTargetIndex - 1);
            wallpaperTarget = wallpaperTarget3;
        } else {
            wallpaperTarget = null;
        }
        if (visible) {
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
        boolean changed = updateWallpaperWindowsPlacement(windows, wallpaperTarget, wallpaperTargetIndex, visible);
        if (targetChanged && WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
            Slog.d(TAG, "New wallpaper: target=" + this.mWallpaperTarget + " lower=" + this.mLowerWallpaperTarget + " upper=" + this.mUpperWallpaperTarget);
        }
        return changed;
    }

    boolean processWallpaperDrawPendingTimeout() {
        if (this.mWallpaperDrawState == 1) {
            this.mWallpaperDrawState = 2;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v(TAG, "*** WALLPAPER DRAW TIMEOUT");
            }
            return true;
        }
        return false;
    }

    boolean wallpaperTransitionReady() {
        boolean transitionReady = true;
        boolean wallpaperReady = true;
        for (int curTokenIndex = this.mWallpaperTokens.size() - 1; curTokenIndex >= 0 && wallpaperReady; curTokenIndex--) {
            WindowToken token = this.mWallpaperTokens.get(curTokenIndex);
            int curWallpaperIndex = token.windows.size() - 1;
            while (true) {
                if (curWallpaperIndex >= 0) {
                    WindowState wallpaper = token.windows.get(curWallpaperIndex);
                    if (!wallpaper.mWallpaperVisible || wallpaper.isDrawnLw()) {
                        curWallpaperIndex--;
                    } else {
                        wallpaperReady = false;
                        if (this.mWallpaperDrawState != 2) {
                            transitionReady = false;
                        }
                        if (this.mWallpaperDrawState == 0) {
                            this.mWallpaperDrawState = 1;
                            this.mService.mH.removeMessages(39);
                            this.mService.mH.sendEmptyMessageDelayed(39, 500L);
                        }
                        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                            Slog.v(TAG, "Wallpaper should be visible but has not been drawn yet. mWallpaperDrawState=" + this.mWallpaperDrawState);
                        }
                    }
                }
            }
        }
        if (wallpaperReady) {
            this.mWallpaperDrawState = 0;
            this.mService.mH.removeMessages(39);
        }
        return transitionReady;
    }

    void addWallpaperToken(WindowToken token) {
        this.mWallpaperTokens.add(token);
    }

    void removeWallpaperToken(WindowToken token) {
        this.mWallpaperTokens.remove(token);
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mWallpaperTarget=");
        pw.println(this.mWallpaperTarget);
        if (this.mLowerWallpaperTarget != null || this.mUpperWallpaperTarget != null) {
            pw.print(prefix);
            pw.print("mLowerWallpaperTarget=");
            pw.println(this.mLowerWallpaperTarget);
            pw.print(prefix);
            pw.print("mUpperWallpaperTarget=");
            pw.println(this.mUpperWallpaperTarget);
        }
        pw.print(prefix);
        pw.print("mLastWallpaperX=");
        pw.print(this.mLastWallpaperX);
        pw.print(" mLastWallpaperY=");
        pw.println(this.mLastWallpaperY);
        if (this.mLastWallpaperDisplayOffsetX == Integer.MIN_VALUE && this.mLastWallpaperDisplayOffsetY == Integer.MIN_VALUE) {
            return;
        }
        pw.print(prefix);
        pw.print("mLastWallpaperDisplayOffsetX=");
        pw.print(this.mLastWallpaperDisplayOffsetX);
        pw.print(" mLastWallpaperDisplayOffsetY=");
        pw.println(this.mLastWallpaperDisplayOffsetY);
    }

    void dumpTokens(PrintWriter pw, String prefix, boolean dumpAll) {
        if (this.mWallpaperTokens.isEmpty()) {
            return;
        }
        pw.println();
        pw.print(prefix);
        pw.println("Wallpaper tokens:");
        for (int i = this.mWallpaperTokens.size() - 1; i >= 0; i--) {
            WindowToken token = this.mWallpaperTokens.get(i);
            pw.print(prefix);
            pw.print("Wallpaper #");
            pw.print(i);
            pw.print(' ');
            pw.print(token);
            if (dumpAll) {
                pw.println(':');
                token.dump(pw, "    ");
            } else {
                pw.println();
            }
        }
    }

    private static final class FindWallpaperTargetResult {
        WindowState topWallpaper;
        int topWallpaperIndex;
        WindowState wallpaperTarget;
        int wallpaperTargetIndex;

        FindWallpaperTargetResult(FindWallpaperTargetResult findWallpaperTargetResult) {
            this();
        }

        private FindWallpaperTargetResult() {
            this.topWallpaperIndex = 0;
            this.topWallpaper = null;
            this.wallpaperTargetIndex = 0;
            this.wallpaperTarget = null;
        }

        void setTopWallpaper(WindowState win, int index) {
            this.topWallpaper = win;
            this.topWallpaperIndex = index;
        }

        void setWallpaperTarget(WindowState win, int index) {
            this.wallpaperTarget = win;
            this.wallpaperTargetIndex = index;
        }

        void reset() {
            this.topWallpaperIndex = 0;
            this.topWallpaper = null;
            this.wallpaperTargetIndex = 0;
            this.wallpaperTarget = null;
        }
    }

    private boolean hasAnyWallpaperLock() {
        for (int i = this.mWallpaperTokens.size(); i > 0; i--) {
            WindowToken token = this.mWallpaperTokens.get(i - 1);
            if (token.windows.size() > 0) {
                return true;
            }
        }
        return false;
    }
}
