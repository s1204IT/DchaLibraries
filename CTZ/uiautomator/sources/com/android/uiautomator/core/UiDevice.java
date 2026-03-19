package com.android.uiautomator.core;

import android.app.UiAutomation;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Deprecated
public class UiDevice {
    private static final long KEY_PRESS_EVENT_TIMEOUT = 1000;
    private static final String LOG_TAG = UiDevice.class.getSimpleName();
    private static UiDevice sDevice;
    private UiAutomatorBridge mUiAutomationBridge;
    private final HashMap<String, UiWatcher> mWatchers = new HashMap<>();
    private final List<String> mWatchersTriggers = new ArrayList();
    private boolean mInWatcherContext = false;

    private UiDevice() {
    }

    public void initialize(UiAutomatorBridge uiAutomatorBridge) {
        this.mUiAutomationBridge = uiAutomatorBridge;
    }

    boolean isInWatcherContext() {
        return this.mInWatcherContext;
    }

    UiAutomatorBridge getAutomatorBridge() {
        if (this.mUiAutomationBridge == null) {
            throw new RuntimeException("UiDevice not initialized");
        }
        return this.mUiAutomationBridge;
    }

    public void setCompressedLayoutHeirarchy(boolean z) {
        getAutomatorBridge().setCompressedLayoutHierarchy(z);
    }

    public static UiDevice getInstance() {
        if (sDevice == null) {
            sDevice = new UiDevice();
        }
        return sDevice;
    }

    public Point getDisplaySizeDp() {
        Tracer.trace(new Object[0]);
        Display defaultDisplay = getAutomatorBridge().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getRealSize(point);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getRealMetrics(displayMetrics);
        float f = point.x / displayMetrics.density;
        float f2 = point.y / displayMetrics.density;
        point.x = Math.round(f);
        point.y = Math.round(f2);
        return point;
    }

    public String getProductName() {
        Tracer.trace(new Object[0]);
        return Build.PRODUCT;
    }

    public String getLastTraversedText() {
        Tracer.trace(new Object[0]);
        return getAutomatorBridge().getQueryController().getLastTraversedText();
    }

    public void clearLastTraversedText() {
        Tracer.trace(new Object[0]);
        getAutomatorBridge().getQueryController().clearLastTraversedText();
    }

    public boolean pressMenu() {
        Tracer.trace(new Object[0]);
        waitForIdle();
        return getAutomatorBridge().getInteractionController().sendKeyAndWaitForEvent(82, 0, 2048, KEY_PRESS_EVENT_TIMEOUT);
    }

    public boolean pressBack() {
        Tracer.trace(new Object[0]);
        waitForIdle();
        return getAutomatorBridge().getInteractionController().sendKeyAndWaitForEvent(4, 0, 2048, KEY_PRESS_EVENT_TIMEOUT);
    }

    public boolean pressHome() {
        Tracer.trace(new Object[0]);
        waitForIdle();
        return getAutomatorBridge().getInteractionController().sendKeyAndWaitForEvent(3, 0, 2048, KEY_PRESS_EVENT_TIMEOUT);
    }

    public boolean pressSearch() {
        Tracer.trace(new Object[0]);
        return pressKeyCode(84);
    }

    public boolean pressDPadCenter() {
        Tracer.trace(new Object[0]);
        return pressKeyCode(23);
    }

    public boolean pressDPadDown() {
        Tracer.trace(new Object[0]);
        return pressKeyCode(20);
    }

    public boolean pressDPadUp() {
        Tracer.trace(new Object[0]);
        return pressKeyCode(19);
    }

    public boolean pressDPadLeft() {
        Tracer.trace(new Object[0]);
        return pressKeyCode(21);
    }

    public boolean pressDPadRight() {
        Tracer.trace(new Object[0]);
        return pressKeyCode(22);
    }

    public boolean pressDelete() {
        Tracer.trace(new Object[0]);
        return pressKeyCode(67);
    }

    public boolean pressEnter() {
        Tracer.trace(new Object[0]);
        return pressKeyCode(66);
    }

    public boolean pressKeyCode(int i) {
        Tracer.trace(Integer.valueOf(i));
        waitForIdle();
        return getAutomatorBridge().getInteractionController().sendKey(i, 0);
    }

    public boolean pressKeyCode(int i, int i2) {
        Tracer.trace(Integer.valueOf(i), Integer.valueOf(i2));
        waitForIdle();
        return getAutomatorBridge().getInteractionController().sendKey(i, i2);
    }

    public boolean pressRecentApps() throws RemoteException {
        Tracer.trace(new Object[0]);
        waitForIdle();
        return getAutomatorBridge().getInteractionController().toggleRecentApps();
    }

    public boolean openNotification() {
        Tracer.trace(new Object[0]);
        waitForIdle();
        return getAutomatorBridge().getInteractionController().openNotification();
    }

    public boolean openQuickSettings() {
        Tracer.trace(new Object[0]);
        waitForIdle();
        return getAutomatorBridge().getInteractionController().openQuickSettings();
    }

    public int getDisplayWidth() {
        Tracer.trace(new Object[0]);
        Display defaultDisplay = getAutomatorBridge().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        return point.x;
    }

    public int getDisplayHeight() {
        Tracer.trace(new Object[0]);
        Display defaultDisplay = getAutomatorBridge().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        return point.y;
    }

    public boolean click(int i, int i2) {
        Tracer.trace(Integer.valueOf(i), Integer.valueOf(i2));
        if (i >= getDisplayWidth() || i2 >= getDisplayHeight()) {
            return false;
        }
        return getAutomatorBridge().getInteractionController().clickNoSync(i, i2);
    }

    public boolean swipe(int i, int i2, int i3, int i4, int i5) {
        Tracer.trace(Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5));
        return getAutomatorBridge().getInteractionController().swipe(i, i2, i3, i4, i5);
    }

    public boolean drag(int i, int i2, int i3, int i4, int i5) {
        Tracer.trace(Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5));
        return getAutomatorBridge().getInteractionController().swipe(i, i2, i3, i4, i5, true);
    }

    public boolean swipe(Point[] pointArr, int i) {
        Tracer.trace(pointArr, Integer.valueOf(i));
        return getAutomatorBridge().getInteractionController().swipe(pointArr, i);
    }

    public void waitForIdle() {
        Tracer.trace(new Object[0]);
        waitForIdle(Configurator.getInstance().getWaitForIdleTimeout());
    }

    public void waitForIdle(long j) {
        Tracer.trace(Long.valueOf(j));
        getAutomatorBridge().waitForIdle(j);
    }

    @Deprecated
    public String getCurrentActivityName() {
        Tracer.trace(new Object[0]);
        return getAutomatorBridge().getQueryController().getCurrentActivityName();
    }

    public String getCurrentPackageName() {
        Tracer.trace(new Object[0]);
        return getAutomatorBridge().getQueryController().getCurrentPackageName();
    }

    public void registerWatcher(String str, UiWatcher uiWatcher) {
        Tracer.trace(str, uiWatcher);
        if (this.mInWatcherContext) {
            throw new IllegalStateException("Cannot register new watcher from within another");
        }
        this.mWatchers.put(str, uiWatcher);
    }

    public void removeWatcher(String str) {
        Tracer.trace(str);
        if (this.mInWatcherContext) {
            throw new IllegalStateException("Cannot remove a watcher from within another");
        }
        this.mWatchers.remove(str);
    }

    public void runWatchers() {
        Tracer.trace(new Object[0]);
        if (this.mInWatcherContext) {
            return;
        }
        for (String str : this.mWatchers.keySet()) {
            UiWatcher uiWatcher = this.mWatchers.get(str);
            if (uiWatcher != null) {
                try {
                    try {
                        this.mInWatcherContext = true;
                        if (uiWatcher.checkForCondition()) {
                            setWatcherTriggered(str);
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exceuting watcher: " + str, e);
                    }
                } finally {
                    this.mInWatcherContext = false;
                }
            }
        }
    }

    public void resetWatcherTriggers() {
        Tracer.trace(new Object[0]);
        this.mWatchersTriggers.clear();
    }

    public boolean hasWatcherTriggered(String str) {
        Tracer.trace(str);
        return this.mWatchersTriggers.contains(str);
    }

    public boolean hasAnyWatcherTriggered() {
        Tracer.trace(new Object[0]);
        return this.mWatchersTriggers.size() > 0;
    }

    private void setWatcherTriggered(String str) {
        Tracer.trace(str);
        if (!hasWatcherTriggered(str)) {
            this.mWatchersTriggers.add(str);
        }
    }

    public boolean isNaturalOrientation() {
        Tracer.trace(new Object[0]);
        waitForIdle();
        int rotation = getAutomatorBridge().getRotation();
        return rotation == 0 || rotation == 2;
    }

    public int getDisplayRotation() {
        Tracer.trace(new Object[0]);
        waitForIdle();
        return getAutomatorBridge().getRotation();
    }

    public void freezeRotation() throws RemoteException {
        Tracer.trace(new Object[0]);
        getAutomatorBridge().getInteractionController().freezeRotation();
    }

    public void unfreezeRotation() throws RemoteException {
        Tracer.trace(new Object[0]);
        getAutomatorBridge().getInteractionController().unfreezeRotation();
    }

    public void setOrientationLeft() throws RemoteException {
        Tracer.trace(new Object[0]);
        getAutomatorBridge().getInteractionController().setRotationLeft();
        waitForIdle();
    }

    public void setOrientationRight() throws RemoteException {
        Tracer.trace(new Object[0]);
        getAutomatorBridge().getInteractionController().setRotationRight();
        waitForIdle();
    }

    public void setOrientationNatural() throws RemoteException {
        Tracer.trace(new Object[0]);
        getAutomatorBridge().getInteractionController().setRotationNatural();
        waitForIdle();
    }

    public void wakeUp() throws RemoteException {
        Tracer.trace(new Object[0]);
        if (getAutomatorBridge().getInteractionController().wakeDevice()) {
            SystemClock.sleep(500L);
        }
    }

    public boolean isScreenOn() throws RemoteException {
        Tracer.trace(new Object[0]);
        return getAutomatorBridge().getInteractionController().isScreenOn();
    }

    public void sleep() throws RemoteException {
        Tracer.trace(new Object[0]);
        getAutomatorBridge().getInteractionController().sleepDevice();
    }

    public void dumpWindowHierarchy(String str) {
        Tracer.trace(str);
        AccessibilityNodeInfo accessibilityRootNode = getAutomatorBridge().getQueryController().getAccessibilityRootNode();
        if (accessibilityRootNode != null) {
            Display defaultDisplay = getAutomatorBridge().getDefaultDisplay();
            Point point = new Point();
            defaultDisplay.getSize(point);
            AccessibilityNodeInfoDumper.dumpWindowToFile(accessibilityRootNode, new File(new File(Environment.getDataDirectory(), "local/tmp"), str), defaultDisplay.getRotation(), point.x, point.y);
        }
    }

    public boolean waitForWindowUpdate(final String str, long j) {
        Tracer.trace(str, Long.valueOf(j));
        if (str != null && !str.equals(getCurrentPackageName())) {
            return false;
        }
        try {
            getAutomatorBridge().executeCommandAndWaitForAccessibilityEvent(new Runnable() {
                @Override
                public void run() {
                }
            }, new UiAutomation.AccessibilityEventFilter() {
                @Override
                public boolean accept(AccessibilityEvent accessibilityEvent) {
                    if (accessibilityEvent.getEventType() == 2048) {
                        return str == null || str.equals(accessibilityEvent.getPackageName());
                    }
                    return false;
                }
            }, j);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e2) {
            Log.e(LOG_TAG, "waitForWindowUpdate: general exception from bridge", e2);
            return false;
        }
    }

    public boolean takeScreenshot(File file) {
        Tracer.trace(file);
        return takeScreenshot(file, 1.0f, 90);
    }

    public boolean takeScreenshot(File file, float f, int i) {
        Tracer.trace(file, Float.valueOf(f), Integer.valueOf(i));
        return getAutomatorBridge().takeScreenshot(file, i);
    }
}
