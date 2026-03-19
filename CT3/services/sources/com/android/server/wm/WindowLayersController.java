package com.android.server.wm;

import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayDeque;

public class WindowLayersController {
    private int mInputMethodAnimLayerAdjustment;
    private final WindowManagerService mService;
    private int mHighestApplicationLayer = 0;
    private ArrayDeque<WindowState> mPinnedWindows = new ArrayDeque<>();
    private ArrayDeque<WindowState> mDockedWindows = new ArrayDeque<>();
    private ArrayDeque<WindowState> mInputMethodWindows = new ArrayDeque<>();
    private WindowState mDockDivider = null;
    private ArrayDeque<WindowState> mReplacingWindows = new ArrayDeque<>();

    public WindowLayersController(WindowManagerService service) {
        this.mService = service;
    }

    final void assignLayersLocked(WindowList windows) {
        if (WindowManagerDebugConfig.DEBUG_LAYERS) {
            Slog.v("WindowManager", "Assigning layers based on windows=" + windows, new RuntimeException("here").fillInStackTrace());
        }
        clear();
        int curBaseLayer = 0;
        int curLayer = 0;
        boolean anyLayerChanged = false;
        int windowCount = windows.size();
        for (int i = 0; i < windowCount; i++) {
            WindowState w = windows.get(i);
            boolean layerChanged = false;
            int oldLayer = w.mLayer;
            if (w.mBaseLayer == curBaseLayer || w.mIsImWindow || (i > 0 && w.mIsWallpaper)) {
                curLayer += 5;
            } else {
                curLayer = w.mBaseLayer;
                curBaseLayer = curLayer;
            }
            assignAnimLayer(w, curLayer);
            if (w.mLayer != oldLayer || w.mWinAnimator.mAnimLayer != oldLayer) {
                layerChanged = true;
                anyLayerChanged = true;
            }
            if (w.mAppToken != null) {
                this.mHighestApplicationLayer = Math.max(this.mHighestApplicationLayer, w.mWinAnimator.mAnimLayer);
            }
            collectSpecialWindows(w);
            if (layerChanged) {
                w.scheduleAnimationIfDimming();
            }
        }
        adjustSpecialWindows();
        if (this.mService.mAccessibilityController != null && anyLayerChanged && windows.get(windows.size() - 1).getDisplayId() == 0) {
            this.mService.mAccessibilityController.onWindowLayersChangedLocked();
        }
        if (WindowManagerDebugConfig.DEBUG_LAYERS) {
            logDebugLayers(windows);
        }
    }

    void setInputMethodAnimLayerAdjustment(int adj) {
        if (WindowManagerDebugConfig.DEBUG_LAYERS) {
            Slog.v("WindowManager", "Setting im layer adj to " + adj);
        }
        this.mInputMethodAnimLayerAdjustment = adj;
        WindowState imw = this.mService.mInputMethodWindow;
        if (imw != null) {
            imw.mWinAnimator.mAnimLayer = imw.mLayer + adj;
            if (WindowManagerDebugConfig.DEBUG_LAYERS) {
                Slog.v("WindowManager", "IM win " + imw + " anim layer: " + imw.mWinAnimator.mAnimLayer);
            }
            for (int i = imw.mChildWindows.size() - 1; i >= 0; i--) {
                WindowState childWindow = imw.mChildWindows.get(i);
                childWindow.mWinAnimator.mAnimLayer = childWindow.mLayer + adj;
                if (WindowManagerDebugConfig.DEBUG_LAYERS) {
                    Slog.v("WindowManager", "IM win " + childWindow + " anim layer: " + childWindow.mWinAnimator.mAnimLayer);
                }
            }
        }
        for (int i2 = this.mService.mInputMethodDialogs.size() - 1; i2 >= 0; i2--) {
            WindowState dialog = this.mService.mInputMethodDialogs.get(i2);
            dialog.mWinAnimator.mAnimLayer = dialog.mLayer + adj;
            if (WindowManagerDebugConfig.DEBUG_LAYERS) {
                Slog.v("WindowManager", "IM win " + imw + " anim layer: " + dialog.mWinAnimator.mAnimLayer);
            }
        }
    }

    int getSpecialWindowAnimLayerAdjustment(WindowState win) {
        if (win.mIsImWindow) {
            return this.mInputMethodAnimLayerAdjustment;
        }
        if (win.mIsWallpaper) {
            return this.mService.mWallpaperControllerLocked.getAnimLayerAdjustment();
        }
        return 0;
    }

    int getResizeDimLayer() {
        if (this.mDockDivider != null) {
            return this.mDockDivider.mLayer - 1;
        }
        return 1;
    }

    private void logDebugLayers(WindowList windows) {
        int n = windows.size();
        for (int i = 0; i < n; i++) {
            WindowState w = windows.get(i);
            WindowStateAnimator winAnimator = w.mWinAnimator;
            Slog.v("WindowManager", "Assign layer " + w + ": mBase=" + w.mBaseLayer + " mLayer=" + w.mLayer + (w.mAppToken == null ? "" : " mAppLayer=" + w.mAppToken.mAppAnimator.animLayerAdjustment) + " =mAnimLayer=" + winAnimator.mAnimLayer);
        }
    }

    private void clear() {
        this.mHighestApplicationLayer = 0;
        this.mPinnedWindows.clear();
        this.mInputMethodWindows.clear();
        this.mDockedWindows.clear();
        this.mReplacingWindows.clear();
        this.mDockDivider = null;
    }

    private void collectSpecialWindows(WindowState w) {
        if (w.mAttrs.type == 2034) {
            this.mDockDivider = w;
            return;
        }
        if (w.mWillReplaceWindow) {
            this.mReplacingWindows.add(w);
        }
        if (w.mIsImWindow) {
            this.mInputMethodWindows.add(w);
            return;
        }
        TaskStack stack = w.getStack();
        if (stack == null) {
            return;
        }
        if (stack.mStackId == 4) {
            this.mPinnedWindows.add(w);
        } else {
            if (stack.mStackId != 3) {
                return;
            }
            this.mDockedWindows.add(w);
        }
    }

    private void adjustSpecialWindows() {
        int layer = this.mHighestApplicationLayer + 5;
        while (!this.mDockedWindows.isEmpty()) {
            layer = assignAndIncreaseLayerIfNeeded(this.mDockedWindows.remove(), layer);
        }
        int layer2 = assignAndIncreaseLayerIfNeeded(this.mDockDivider, layer);
        if (this.mDockDivider != null && this.mDockDivider.isVisibleLw()) {
            while (!this.mInputMethodWindows.isEmpty()) {
                WindowState w = this.mInputMethodWindows.remove();
                if (layer2 > w.mLayer) {
                    layer2 = assignAndIncreaseLayerIfNeeded(w, layer2);
                }
            }
        }
        while (!this.mReplacingWindows.isEmpty()) {
            layer2 = assignAndIncreaseLayerIfNeeded(this.mReplacingWindows.remove(), layer2);
        }
        while (!this.mPinnedWindows.isEmpty()) {
            layer2 = assignAndIncreaseLayerIfNeeded(this.mPinnedWindows.remove(), layer2);
        }
    }

    private int assignAndIncreaseLayerIfNeeded(WindowState win, int layer) {
        if (win != null) {
            assignAnimLayer(win, layer);
            return layer + 5;
        }
        return layer;
    }

    private void assignAnimLayer(WindowState w, int layer) {
        w.mLayer = layer;
        w.mWinAnimator.mAnimLayer = w.mLayer + w.getAnimLayerAdjustment() + getSpecialWindowAnimLayerAdjustment(w);
        if (w.mAppToken == null || w.mAppToken.mAppAnimator.thumbnailForceAboveLayer <= 0 || w.mWinAnimator.mAnimLayer <= w.mAppToken.mAppAnimator.thumbnailForceAboveLayer) {
            return;
        }
        w.mAppToken.mAppAnimator.thumbnailForceAboveLayer = w.mWinAnimator.mAnimLayer;
    }

    void dump(PrintWriter pw, String s) {
        if (this.mInputMethodAnimLayerAdjustment == 0 && this.mService.mWallpaperControllerLocked.getAnimLayerAdjustment() == 0) {
            return;
        }
        pw.print("  mInputMethodAnimLayerAdjustment=");
        pw.print(this.mInputMethodAnimLayerAdjustment);
        pw.print("  mWallpaperAnimLayerAdjustment=");
        pw.println(this.mService.mWallpaperControllerLocked.getAnimLayerAdjustment());
    }
}
