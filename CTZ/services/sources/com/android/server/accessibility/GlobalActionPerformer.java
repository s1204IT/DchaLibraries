package com.android.server.accessibility;

import android.app.StatusBarManager;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.KeyEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ScreenshotHelper;
import com.android.server.LocalServices;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.wm.WindowManagerInternal;
import java.util.function.Supplier;

public class GlobalActionPerformer {
    private final Context mContext;
    private Supplier<ScreenshotHelper> mScreenshotHelperSupplier;
    private final WindowManagerInternal mWindowManagerService;

    public GlobalActionPerformer(Context context, WindowManagerInternal windowManagerInternal) {
        this.mContext = context;
        this.mWindowManagerService = windowManagerInternal;
        this.mScreenshotHelperSupplier = null;
    }

    @VisibleForTesting
    public GlobalActionPerformer(Context context, WindowManagerInternal windowManagerInternal, Supplier<ScreenshotHelper> supplier) {
        this(context, windowManagerInternal);
        this.mScreenshotHelperSupplier = supplier;
    }

    public boolean performGlobalAction(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            switch (i) {
                case 1:
                    sendDownAndUpKeyEvents(4);
                    return true;
                case 2:
                    sendDownAndUpKeyEvents(3);
                    return true;
                case 3:
                    return openRecents();
                case 4:
                    expandNotifications();
                    return true;
                case 5:
                    expandQuickSettings();
                    return true;
                case 6:
                    showGlobalActions();
                    return true;
                case 7:
                    return toggleSplitScreen();
                case 8:
                    return lockScreen();
                case 9:
                    return takeScreenshot();
                default:
                    return false;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void sendDownAndUpKeyEvents(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        long jUptimeMillis = SystemClock.uptimeMillis();
        sendKeyEventIdentityCleared(i, 0, jUptimeMillis, jUptimeMillis);
        sendKeyEventIdentityCleared(i, 1, jUptimeMillis, SystemClock.uptimeMillis());
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private void sendKeyEventIdentityCleared(int i, int i2, long j, long j2) {
        KeyEvent keyEventObtain = KeyEvent.obtain(j, j2, i2, i, 0, 0, -1, 0, 8, UsbTerminalTypes.TERMINAL_USB_STREAMING, null);
        InputManager.getInstance().injectInputEvent(keyEventObtain, 0);
        keyEventObtain.recycle();
    }

    private void expandNotifications() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ((StatusBarManager) this.mContext.getSystemService("statusbar")).expandNotificationsPanel();
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private void expandQuickSettings() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ((StatusBarManager) this.mContext.getSystemService("statusbar")).expandSettingsPanel();
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private boolean openRecents() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            StatusBarManagerInternal statusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (statusBarManagerInternal != null) {
                statusBarManagerInternal.toggleRecentApps();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return true;
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void showGlobalActions() {
        this.mWindowManagerService.showGlobalActions();
    }

    private boolean toggleSplitScreen() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            StatusBarManagerInternal statusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (statusBarManagerInternal != null) {
                statusBarManagerInternal.toggleSplitScreen();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return true;
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean lockScreen() {
        ((PowerManager) this.mContext.getSystemService(PowerManager.class)).goToSleep(SystemClock.uptimeMillis(), 7, 0);
        this.mWindowManagerService.lockNow();
        return true;
    }

    private boolean takeScreenshot() {
        (this.mScreenshotHelperSupplier != null ? this.mScreenshotHelperSupplier.get() : new ScreenshotHelper(this.mContext)).takeScreenshot(1, true, true, new Handler(Looper.getMainLooper()));
        return true;
    }
}
