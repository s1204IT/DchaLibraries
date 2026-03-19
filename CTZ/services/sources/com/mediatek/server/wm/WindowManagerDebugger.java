package com.mediatek.server.wm;

import android.view.WindowManager;
import com.android.server.policy.WindowManagerPolicy;
import java.io.PrintWriter;

public class WindowManagerDebugger {
    public static final String TAG = "WindowManagerDebugger";
    public static boolean WMS_DEBUG_ENG = false;
    public static boolean WMS_DEBUG_USER = false;
    public static boolean WMS_DEBUG_LOG_OFF = false;

    public void runDebug(PrintWriter printWriter, String[] strArr, int i) {
    }

    public void debugInterceptKeyBeforeQueueing(String str, int i, boolean z, boolean z2, int i2, boolean z3, boolean z4, boolean z5, boolean z6, int i3, boolean z7, boolean z8) {
    }

    public void debugApplyPostLayoutPolicyLw(String str, WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams, WindowManagerPolicy.WindowState windowState2, WindowManagerPolicy.WindowState windowState3, WindowManagerPolicy.WindowState windowState4, boolean z, boolean z2) {
    }

    public void debugLayoutWindowLw(String str, int i, int i2, int i3, boolean z, int i4) {
    }

    public void debugGetOrientation(String str, boolean z, int i, int i2) {
    }

    public void debugGetOrientingWindow(String str, WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams, boolean z, boolean z2, boolean z3, boolean z4) {
    }

    public void debugPrepareSurfaceLocked(String str, boolean z, WindowManagerPolicy.WindowState windowState, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7) {
    }

    public void debugRelayoutWindow(String str, WindowManagerPolicy.WindowState windowState, int i, int i2) {
    }

    public void debugInputAttr(String str, WindowManager.LayoutParams layoutParams) {
    }

    public void debugViewVisibility(String str, WindowManagerPolicy.WindowState windowState, int i, int i2, boolean z) {
    }
}
