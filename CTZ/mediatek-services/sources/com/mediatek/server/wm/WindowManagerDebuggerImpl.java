package com.mediatek.server.wm;

import android.os.Build;
import android.util.Slog;
import android.view.WindowManager;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.PolicyControl;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.WindowManagerDebugConfig;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.lang.reflect.Field;

public class WindowManagerDebuggerImpl extends WindowManagerDebugger {
    private static final String TAG = "WindowManagerDebuggerImpl";

    public WindowManagerDebuggerImpl() {
        WMS_DEBUG_ENG = "eng".equals(Build.TYPE);
        WMS_DEBUG_USER = true;
    }

    public void runDebug(PrintWriter printWriter, String[] strArr, int i) {
        char c;
        int i2 = i;
        String str = "help";
        if (i2 < strArr.length) {
            str = strArr[i2];
            i2++;
        }
        int i3 = 2;
        if ("help".equals(str)) {
            printWriter.println("Window manager debug options:");
            printWriter.println("  -d enable <zone zone ...> : enable the debug zone");
            printWriter.println("  -d disable <zone zone ...> : disable the debug zone");
            printWriter.println("zone may be some of:");
            printWriter.println("  a[all]");
            c = 0;
        } else if (!"enable".equals(str)) {
            if (!"disable".equals(str)) {
                printWriter.println("Unknown debug argument: " + str + "; use \"-d help\" for help");
                return;
            }
            c = 2;
        } else {
            c = 1;
        }
        Field[] declaredFields = WindowManagerDebugConfig.class.getDeclaredFields();
        Field[] declaredFields2 = PhoneWindowManager.class.getDeclaredFields();
        String str2 = str;
        int i4 = i2;
        boolean z = false;
        while (!z) {
            if (c == 0 || i4 < strArr.length) {
                if (i4 < strArr.length) {
                    String str3 = strArr[i4];
                    i4++;
                    str2 = str3;
                }
                boolean z2 = c == 0 || "all".equals(str2) || "a".equals(str2);
                int i5 = 0;
                while (i5 < declaredFields.length) {
                    String name = declaredFields[i5].getName();
                    if (name != null && (name.contains("DEBUG") || name.contains("SHOW") || name.equals("localLOGV"))) {
                        if (z2) {
                            if (c != 0) {
                            }
                            Object[] objArr = new Object[i3];
                            objArr[0] = name;
                            objArr[1] = Boolean.valueOf(declaredFields[i5].getBoolean(null));
                            printWriter.println(String.format("  %s = %b", objArr));
                        } else {
                            try {
                                if (name.equals(str2)) {
                                    if (c != 0) {
                                        declaredFields[i5].setAccessible(true);
                                        declaredFields[i5].setBoolean(null, c == 1);
                                        if (name.equals("localLOGV")) {
                                            WindowManagerService.localLOGV = c == 1;
                                        }
                                        int i6 = 0;
                                        while (true) {
                                            if (i6 >= declaredFields2.length) {
                                                break;
                                            }
                                            if (!declaredFields2[i6].getName().equals(name)) {
                                                i6++;
                                            } else {
                                                declaredFields2[i6].setAccessible(true);
                                                declaredFields2[i6].setBoolean(null, c == 1);
                                            }
                                        }
                                    }
                                    Object[] objArr2 = new Object[i3];
                                    objArr2[0] = name;
                                    objArr2[1] = Boolean.valueOf(declaredFields[i5].getBoolean(null));
                                    printWriter.println(String.format("  %s = %b", objArr2));
                                }
                            } catch (IllegalAccessException e) {
                                Slog.e(TAG, name + " setBoolean failed", e);
                            }
                        }
                    }
                    i5++;
                    i3 = 2;
                }
                z = z2;
            } else {
                return;
            }
        }
    }

    public void debugInterceptKeyBeforeQueueing(String str, int i, boolean z, boolean z2, int i2, boolean z3, boolean z4, boolean z5, boolean z6, int i3, boolean z7, boolean z8) {
        Slog.d(str, "interceptKeyTq keycode=" + i + " interactive=" + z + " keyguardActive=" + z2 + " policyFlags=" + Integer.toHexString(i2) + " down =" + z3 + " canceled = " + z4 + " isWakeKey=" + z5 + " mVolumeDownKeyTriggered =" + z6 + " result = " + i3 + " useHapticFeedback = " + z7 + " isInjected = " + z8);
    }

    public void debugApplyPostLayoutPolicyLw(String str, WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams, WindowManagerPolicy.WindowState windowState2, WindowManagerPolicy.WindowState windowState3, WindowManagerPolicy.WindowState windowState4, boolean z, boolean z2) {
        Slog.i(str, "applyPostLayoutPolicyLw Win " + windowState + ": win.isVisibleLw()=" + windowState.isVisibleLw() + ", win.hasDrawnLw()=" + windowState.hasDrawnLw() + ", win.isDrawnLw()=" + windowState.isDrawnLw() + ", attrs.type=" + layoutParams.type + ", attrs.privateFlags=#" + Integer.toHexString(layoutParams.privateFlags) + ", fl=#" + Integer.toHexString(PolicyControl.getWindowFlags(windowState, layoutParams)) + ", mTopFullscreenOpaqueWindowState=" + windowState2 + ", win.isGoneForLayoutLw()=" + windowState.isGoneForLayoutLw() + ", attached=" + windowState3 + ", imeTarget=" + windowState4 + ", isFullscreen=" + layoutParams.isFullscreen() + ", normallyFullscreenWindows=, mDreamingLockscreen=" + z + ", mShowingDream=" + z2);
    }

    public void debugLayoutWindowLw(String str, int i, int i2, int i3, boolean z, int i4) {
        Slog.v(str, "layoutWindowLw : sim=#" + Integer.toHexString(i) + ", type=" + i2 + ", flag=" + i3 + ", canHideNavigationBar=" + z + ", sysUiFl=" + i4);
    }

    public void debugGetOrientation(String str, boolean z, int i, int i2) {
        Slog.v(str, "Checking window orientation: mDisplayFrozen=" + z + ", mLastWindowForcedOrientation=" + i + ", mLastKeyguardForcedOrientation=" + i2);
    }

    public void debugGetOrientingWindow(String str, WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams, boolean z, boolean z2, boolean z3, boolean z4) {
        Slog.v(str, windowState + " screenOrientation=" + layoutParams.screenOrientation + ", visibility=" + z + ", mPolicyVisibilityAfterAnim=" + z2 + ", mPolicyVisibility=" + z3 + ", destroying=" + z4);
    }

    public void debugPrepareSurfaceLocked(String str, boolean z, WindowManagerPolicy.WindowState windowState, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7) {
        Slog.v(str, windowState + " prepareSurfaceLocked , mIsWallpaper=" + z + ", mWin.mWallpaperVisible=" + z2 + ", w.isOnScreen=" + z3 + ", w.mPolicyVisibility=" + z4 + ", w.mHasSurface=" + z5 + ", w.mDestroying=" + z6 + ", mLastHidden=" + z7);
    }

    public void debugRelayoutWindow(String str, WindowManagerPolicy.WindowState windowState, int i, int i2) {
        Slog.e(str, "Window : " + windowState + "changes the window type!!\nOriginal type : " + i + "\nChanged type : " + i2);
    }

    public void debugInputAttr(String str, WindowManager.LayoutParams layoutParams) {
        Slog.v(str, "Input attr :" + layoutParams);
    }

    public void debugViewVisibility(String str, WindowManagerPolicy.WindowState windowState, int i, int i2, boolean z) {
        if (i == 0 && i2 != 0) {
            Slog.i(str, "Relayout " + windowState + ": oldVis=" + i2 + " newVis=" + i + " focusMayChange = " + z);
        }
    }
}
