package com.android.systemui.statusbar.phone;

import android.graphics.Rect;
import com.android.systemui.statusbar.policy.BatteryController;

public class LightStatusBarController {
    private final BatteryController mBatteryController;
    private boolean mDockedLight;
    private int mDockedStackVisibility;
    private FingerprintUnlockController mFingerprintUnlockController;
    private boolean mFullscreenLight;
    private int mFullscreenStackVisibility;
    private final StatusBarIconController mIconController;
    private final Rect mLastFullscreenBounds = new Rect();
    private final Rect mLastDockedBounds = new Rect();

    public LightStatusBarController(StatusBarIconController iconController, BatteryController batteryController) {
        this.mIconController = iconController;
        this.mBatteryController = batteryController;
    }

    public void setFingerprintUnlockController(FingerprintUnlockController fingerprintUnlockController) {
        this.mFingerprintUnlockController = fingerprintUnlockController;
    }

    public void onSystemUiVisibilityChanged(int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds, boolean sbModeChanged, int statusBarMode) {
        int oldFullscreen = this.mFullscreenStackVisibility;
        int newFullscreen = ((~mask) & oldFullscreen) | (fullscreenStackVis & mask);
        int diffFullscreen = newFullscreen ^ oldFullscreen;
        int oldDocked = this.mDockedStackVisibility;
        int newDocked = ((~mask) & oldDocked) | (dockedStackVis & mask);
        int diffDocked = newDocked ^ oldDocked;
        if ((diffFullscreen & 8192) != 0 || (diffDocked & 8192) != 0 || sbModeChanged || !this.mLastFullscreenBounds.equals(fullscreenStackBounds) || !this.mLastDockedBounds.equals(dockedStackBounds)) {
            this.mFullscreenLight = isLight(newFullscreen, statusBarMode);
            this.mDockedLight = isLight(newDocked, statusBarMode);
            update(fullscreenStackBounds, dockedStackBounds);
        }
        this.mFullscreenStackVisibility = newFullscreen;
        this.mDockedStackVisibility = newDocked;
        this.mLastFullscreenBounds.set(fullscreenStackBounds);
        this.mLastDockedBounds.set(dockedStackBounds);
    }

    private boolean isLight(int vis, int statusBarMode) {
        boolean isTransparentBar = true;
        if (statusBarMode != 4 && statusBarMode != 6) {
            isTransparentBar = false;
        }
        boolean allowLight = isTransparentBar && !this.mBatteryController.isPowerSave();
        boolean light = (vis & 8192) != 0;
        if (allowLight) {
            return light;
        }
        return false;
    }

    private boolean animateChange() {
        int unlockMode;
        return (this.mFingerprintUnlockController == null || (unlockMode = this.mFingerprintUnlockController.getMode()) == 2 || unlockMode == 1) ? false : true;
    }

    private void update(Rect fullscreenStackBounds, Rect dockedStackBounds) {
        boolean hasDockedStack = !dockedStackBounds.isEmpty();
        if ((this.mFullscreenLight && this.mDockedLight) || (this.mFullscreenLight && !hasDockedStack)) {
            this.mIconController.setIconsDarkArea(null);
            this.mIconController.setIconsDark(true, animateChange());
            return;
        }
        if ((!this.mFullscreenLight && !this.mDockedLight) || (!this.mFullscreenLight && !hasDockedStack)) {
            this.mIconController.setIconsDark(false, animateChange());
            return;
        }
        Rect bounds = this.mFullscreenLight ? fullscreenStackBounds : dockedStackBounds;
        if (bounds.isEmpty()) {
            this.mIconController.setIconsDarkArea(null);
        } else {
            this.mIconController.setIconsDarkArea(bounds);
        }
        this.mIconController.setIconsDark(true, animateChange());
    }
}
