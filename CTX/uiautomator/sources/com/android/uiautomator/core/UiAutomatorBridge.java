package com.android.uiautomator.core;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public abstract class UiAutomatorBridge {
    private static final String LOG_TAG = UiAutomatorBridge.class.getSimpleName();
    private static final long QUIET_TIME_TO_BE_CONSIDERD_IDLE_STATE = 500;
    private static final long TOTAL_TIME_TO_WAIT_FOR_IDLE_STATE = 10000;
    private final InteractionController mInteractionController = new InteractionController(this);
    private final QueryController mQueryController = new QueryController(this);
    private final UiAutomation mUiAutomation;

    public abstract Display getDefaultDisplay();

    public abstract int getRotation();

    public abstract long getSystemLongPressTime();

    public abstract boolean isScreenOn();

    UiAutomatorBridge(UiAutomation uiAutomation) {
        this.mUiAutomation = uiAutomation;
    }

    InteractionController getInteractionController() {
        return this.mInteractionController;
    }

    QueryController getQueryController() {
        return this.mQueryController;
    }

    public void setOnAccessibilityEventListener(UiAutomation.OnAccessibilityEventListener onAccessibilityEventListener) {
        this.mUiAutomation.setOnAccessibilityEventListener(onAccessibilityEventListener);
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        return this.mUiAutomation.getRootInActiveWindow();
    }

    public boolean injectInputEvent(InputEvent inputEvent, boolean z) {
        return this.mUiAutomation.injectInputEvent(inputEvent, z);
    }

    public boolean setRotation(int i) {
        return this.mUiAutomation.setRotation(i);
    }

    public void setCompressedLayoutHierarchy(boolean z) {
        AccessibilityServiceInfo serviceInfo = this.mUiAutomation.getServiceInfo();
        if (z) {
            serviceInfo.flags &= -3;
        } else {
            serviceInfo.flags |= 2;
        }
        this.mUiAutomation.setServiceInfo(serviceInfo);
    }

    public void waitForIdle() {
        waitForIdle(TOTAL_TIME_TO_WAIT_FOR_IDLE_STATE);
    }

    public void waitForIdle(long j) {
        try {
            this.mUiAutomation.waitForIdle(QUIET_TIME_TO_BE_CONSIDERD_IDLE_STATE, j);
        } catch (TimeoutException e) {
            Log.w(LOG_TAG, "Could not detect idle state.", e);
        }
    }

    public AccessibilityEvent executeCommandAndWaitForAccessibilityEvent(Runnable runnable, UiAutomation.AccessibilityEventFilter accessibilityEventFilter, long j) throws TimeoutException {
        return this.mUiAutomation.executeAndWaitForEvent(runnable, accessibilityEventFilter, j);
    }

    public boolean takeScreenshot(File file, int i) throws Throwable {
        BufferedOutputStream bufferedOutputStream;
        Bitmap bitmapTakeScreenshot = this.mUiAutomation.takeScreenshot();
        if (bitmapTakeScreenshot == null) {
            return false;
        }
        BufferedOutputStream bufferedOutputStream2 = null;
        try {
            try {
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            bitmapTakeScreenshot.compress(Bitmap.CompressFormat.PNG, i, bufferedOutputStream);
            bufferedOutputStream.flush();
            try {
                bufferedOutputStream.close();
            } catch (IOException e2) {
            }
            bitmapTakeScreenshot.recycle();
            return true;
        } catch (IOException e3) {
            e = e3;
            bufferedOutputStream2 = bufferedOutputStream;
            Log.e(LOG_TAG, "failed to save screen shot to file", e);
            if (bufferedOutputStream2 != null) {
                try {
                    bufferedOutputStream2.close();
                } catch (IOException e4) {
                }
            }
            bitmapTakeScreenshot.recycle();
            return false;
        } catch (Throwable th2) {
            th = th2;
            bufferedOutputStream2 = bufferedOutputStream;
            if (bufferedOutputStream2 != null) {
                try {
                    bufferedOutputStream2.close();
                } catch (IOException e5) {
                }
            }
            bitmapTakeScreenshot.recycle();
            throw th;
        }
    }

    public boolean performGlobalAction(int i) {
        return this.mUiAutomation.performGlobalAction(i);
    }
}
