package com.mediatek.server.wm;

import android.graphics.Rect;
import android.os.IBinder;
import android.view.DisplayInfo;
import android.view.WindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.AppWindowToken;
import com.android.server.wm.WindowState;

public class WmsExt {
    public static final String TAG = "WindowManager";
    public static final Rect mEmptyFrame = new Rect();

    public boolean isFullscreenSwitchSupport() {
        return false;
    }

    public boolean isFocusWindowReady(WindowManagerPolicy.WindowState windowState) {
        return false;
    }

    public Rect getSwitchFrame(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2, int i, int i2) {
        return mEmptyFrame;
    }

    public void resetSwitchFrame() {
    }

    public boolean initFullscreenSwitchState(IBinder iBinder) {
        return true;
    }

    public boolean isMultiWindow(AppWindowToken appWindowToken) {
        return false;
    }

    public boolean isFullScreenCropState(AppWindowToken appWindowToken) {
        return false;
    }

    public Rect getSwitchFrame(int i, int i2) {
        return mEmptyFrame;
    }

    public boolean getFullscreenMode(String str) {
        return true;
    }

    public boolean isAppResolutionTunerSupport() {
        return false;
    }

    public void loadResolutionTunerAppList() {
    }

    public void setWindowScaleByWL(WindowState windowState, DisplayInfo displayInfo, WindowManager.LayoutParams layoutParams, int i, int i2) {
    }
}
