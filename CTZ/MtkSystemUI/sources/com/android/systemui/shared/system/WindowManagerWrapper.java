package com.android.systemui.shared.system;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture;
import com.android.systemui.shared.recents.view.RecentsTransition;

public class WindowManagerWrapper {
    private static final WindowManagerWrapper sInstance = new WindowManagerWrapper();

    public static WindowManagerWrapper getInstance() {
        return sInstance;
    }

    public void overridePendingAppTransitionMultiThumbFuture(AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture, Runnable runnable, Handler handler, boolean z) {
        try {
            WindowManagerGlobal.getWindowManagerService().overridePendingAppTransitionMultiThumbFuture(appTransitionAnimationSpecsFuture.getFuture(), RecentsTransition.wrapStartedListener(handler, runnable), z);
        } catch (RemoteException e) {
            Log.w("WindowManagerWrapper", "Failed to override pending app transition (multi-thumbnail future): ", e);
        }
    }

    public void setNavBarVirtualKeyHapticFeedbackEnabled(boolean z) {
        try {
            WindowManagerGlobal.getWindowManagerService().setNavBarVirtualKeyHapticFeedbackEnabled(z);
        } catch (RemoteException e) {
            Log.w("WindowManagerWrapper", "Failed to enable or disable navigation bar button haptics: ", e);
        }
    }

    public int getNavBarPosition() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getNavBarPosition();
        } catch (RemoteException e) {
            Log.w("WindowManagerWrapper", "Failed to get nav bar position");
            return -1;
        }
    }
}
