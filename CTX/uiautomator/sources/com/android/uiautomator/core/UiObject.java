package com.android.uiautomator.core;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;

@Deprecated
public class UiObject {
    protected static final int FINGER_TOUCH_HALF_WIDTH = 20;
    private static final String LOG_TAG = UiObject.class.getSimpleName();
    protected static final int SWIPE_MARGIN_LIMIT = 5;

    @Deprecated
    protected static final long WAIT_FOR_EVENT_TMEOUT = 3000;
    protected static final long WAIT_FOR_SELECTOR_POLL = 1000;

    @Deprecated
    protected static final long WAIT_FOR_SELECTOR_TIMEOUT = 10000;
    protected static final long WAIT_FOR_WINDOW_TMEOUT = 5500;
    private final Configurator mConfig = Configurator.getInstance();
    private final UiSelector mSelector;

    public UiObject(UiSelector uiSelector) {
        this.mSelector = uiSelector;
    }

    public final UiSelector getSelector() {
        Tracer.trace(new Object[0]);
        return new UiSelector(this.mSelector);
    }

    QueryController getQueryController() {
        return UiDevice.getInstance().getAutomatorBridge().getQueryController();
    }

    InteractionController getInteractionController() {
        return UiDevice.getInstance().getAutomatorBridge().getInteractionController();
    }

    public UiObject getChild(UiSelector uiSelector) throws UiObjectNotFoundException {
        Tracer.trace(uiSelector);
        return new UiObject(getSelector().childSelector(uiSelector));
    }

    public UiObject getFromParent(UiSelector uiSelector) throws UiObjectNotFoundException {
        Tracer.trace(uiSelector);
        return new UiObject(getSelector().fromParent(uiSelector));
    }

    public int getChildCount() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.getChildCount();
    }

    protected AccessibilityNodeInfo findAccessibilityNodeInfo(long j) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = null;
        long jUptimeMillis2 = 0;
        while (jUptimeMillis2 <= j && (accessibilityNodeInfoFindAccessibilityNodeInfo = getQueryController().findAccessibilityNodeInfo(getSelector())) == null) {
            UiDevice.getInstance().runWatchers();
            jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
            if (j > 0) {
                SystemClock.sleep(WAIT_FOR_SELECTOR_POLL);
            }
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo;
    }

    public boolean dragTo(UiObject uiObject, int i) throws UiObjectNotFoundException {
        Rect visibleBounds = getVisibleBounds();
        Rect visibleBounds2 = uiObject.getVisibleBounds();
        return getInteractionController().swipe(visibleBounds.centerX(), visibleBounds.centerY(), visibleBounds2.centerX(), visibleBounds2.centerY(), i, true);
    }

    public boolean dragTo(int i, int i2, int i3) throws UiObjectNotFoundException {
        Rect visibleBounds = getVisibleBounds();
        return getInteractionController().swipe(visibleBounds.centerX(), visibleBounds.centerY(), i, i2, i3, true);
    }

    public boolean swipeUp(int i) throws UiObjectNotFoundException {
        Tracer.trace(Integer.valueOf(i));
        Rect visibleBounds = getVisibleBounds();
        if (visibleBounds.height() <= 10) {
            return false;
        }
        return getInteractionController().swipe(visibleBounds.centerX(), visibleBounds.bottom - 5, visibleBounds.centerX(), visibleBounds.top + SWIPE_MARGIN_LIMIT, i);
    }

    public boolean swipeDown(int i) throws UiObjectNotFoundException {
        Tracer.trace(Integer.valueOf(i));
        Rect visibleBounds = getVisibleBounds();
        if (visibleBounds.height() <= 10) {
            return false;
        }
        return getInteractionController().swipe(visibleBounds.centerX(), visibleBounds.top + SWIPE_MARGIN_LIMIT, visibleBounds.centerX(), visibleBounds.bottom - 5, i);
    }

    public boolean swipeLeft(int i) throws UiObjectNotFoundException {
        Tracer.trace(Integer.valueOf(i));
        Rect visibleBounds = getVisibleBounds();
        if (visibleBounds.width() <= 10) {
            return false;
        }
        return getInteractionController().swipe(visibleBounds.right - 5, visibleBounds.centerY(), visibleBounds.left + SWIPE_MARGIN_LIMIT, visibleBounds.centerY(), i);
    }

    public boolean swipeRight(int i) throws UiObjectNotFoundException {
        Tracer.trace(Integer.valueOf(i));
        Rect visibleBounds = getVisibleBounds();
        if (visibleBounds.width() <= 10) {
            return false;
        }
        return getInteractionController().swipe(visibleBounds.left + SWIPE_MARGIN_LIMIT, visibleBounds.centerY(), visibleBounds.right - 5, visibleBounds.centerY(), i);
    }

    private Rect getVisibleBounds(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (accessibilityNodeInfo == null) {
            return null;
        }
        int displayWidth = UiDevice.getInstance().getDisplayWidth();
        int displayHeight = UiDevice.getInstance().getDisplayHeight();
        Rect visibleBoundsInScreen = AccessibilityNodeInfoHelper.getVisibleBoundsInScreen(accessibilityNodeInfo, displayWidth, displayHeight);
        AccessibilityNodeInfo scrollableParent = getScrollableParent(accessibilityNodeInfo);
        if (scrollableParent == null || visibleBoundsInScreen.intersect(AccessibilityNodeInfoHelper.getVisibleBoundsInScreen(scrollableParent, displayWidth, displayHeight))) {
            return visibleBoundsInScreen;
        }
        return new Rect();
    }

    private AccessibilityNodeInfo getScrollableParent(AccessibilityNodeInfo accessibilityNodeInfo) {
        while (accessibilityNodeInfo != null) {
            accessibilityNodeInfo = accessibilityNodeInfo.getParent();
            if (accessibilityNodeInfo != null && accessibilityNodeInfo.isScrollable()) {
                return accessibilityNodeInfo;
            }
        }
        return null;
    }

    public boolean click() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        return getInteractionController().clickAndSync(visibleBounds.centerX(), visibleBounds.centerY(), this.mConfig.getActionAcknowledgmentTimeout());
    }

    public boolean clickAndWaitForNewWindow() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        return clickAndWaitForNewWindow(WAIT_FOR_WINDOW_TMEOUT);
    }

    public boolean clickAndWaitForNewWindow(long j) throws UiObjectNotFoundException {
        Tracer.trace(Long.valueOf(j));
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        return getInteractionController().clickAndWaitForNewWindow(visibleBounds.centerX(), visibleBounds.centerY(), this.mConfig.getActionAcknowledgmentTimeout());
    }

    public boolean clickTopLeft() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        return getInteractionController().clickNoSync(visibleBounds.left + SWIPE_MARGIN_LIMIT, visibleBounds.top + SWIPE_MARGIN_LIMIT);
    }

    public boolean longClickBottomRight() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        return getInteractionController().longTapNoSync(visibleBounds.right - 5, visibleBounds.bottom - 5);
    }

    public boolean clickBottomRight() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        return getInteractionController().clickNoSync(visibleBounds.right - 5, visibleBounds.bottom - 5);
    }

    public boolean longClick() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        return getInteractionController().longTapNoSync(visibleBounds.centerX(), visibleBounds.centerY());
    }

    public boolean longClickTopLeft() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        return getInteractionController().longTapNoSync(visibleBounds.left + SWIPE_MARGIN_LIMIT, visibleBounds.top + SWIPE_MARGIN_LIMIT);
    }

    public String getText() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        String strSafeStringReturn = safeStringReturn(accessibilityNodeInfoFindAccessibilityNodeInfo.getText());
        Log.d(LOG_TAG, String.format("getText() = %s", strSafeStringReturn));
        return strSafeStringReturn;
    }

    public String getClassName() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        String strSafeStringReturn = safeStringReturn(accessibilityNodeInfoFindAccessibilityNodeInfo.getClassName());
        Log.d(LOG_TAG, String.format("getClassName() = %s", strSafeStringReturn));
        return strSafeStringReturn;
    }

    public String getContentDescription() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return safeStringReturn(accessibilityNodeInfoFindAccessibilityNodeInfo.getContentDescription());
    }

    public boolean setText(String str) throws UiObjectNotFoundException {
        Tracer.trace(str);
        clearTextField();
        return getInteractionController().sendText(str);
    }

    public void clearTextField() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        getInteractionController().longTapNoSync(visibleBounds.left + FINGER_TOUCH_HALF_WIDTH, visibleBounds.centerY());
        UiObject uiObject = new UiObject(new UiSelector().descriptionContains("Select all"));
        if (uiObject.waitForExists(50L)) {
            uiObject.click();
        }
        SystemClock.sleep(250L);
        getInteractionController().sendKey(67, 0);
    }

    public boolean isChecked() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isChecked();
    }

    public boolean isSelected() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isSelected();
    }

    public boolean isCheckable() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isCheckable();
    }

    public boolean isEnabled() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isEnabled();
    }

    public boolean isClickable() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isClickable();
    }

    public boolean isFocused() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isFocused();
    }

    public boolean isFocusable() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isFocusable();
    }

    public boolean isScrollable() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isScrollable();
    }

    public boolean isLongClickable() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return accessibilityNodeInfoFindAccessibilityNodeInfo.isLongClickable();
    }

    public String getPackageName() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return safeStringReturn(accessibilityNodeInfoFindAccessibilityNodeInfo.getPackageName());
    }

    public Rect getVisibleBounds() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        return getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
    }

    public Rect getBounds() throws UiObjectNotFoundException {
        Tracer.trace(new Object[0]);
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect rect = new Rect();
        accessibilityNodeInfoFindAccessibilityNodeInfo.getBoundsInScreen(rect);
        return rect;
    }

    public boolean waitForExists(long j) {
        Tracer.trace(Long.valueOf(j));
        return findAccessibilityNodeInfo(j) != null;
    }

    public boolean waitUntilGone(long j) {
        Tracer.trace(Long.valueOf(j));
        long jUptimeMillis = SystemClock.uptimeMillis();
        long jUptimeMillis2 = 0;
        while (jUptimeMillis2 <= j) {
            if (findAccessibilityNodeInfo(0L) == null) {
                return true;
            }
            jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
            if (j > 0) {
                SystemClock.sleep(WAIT_FOR_SELECTOR_POLL);
            }
        }
        return false;
    }

    public boolean exists() {
        Tracer.trace(new Object[0]);
        return waitForExists(0L);
    }

    private String safeStringReturn(CharSequence charSequence) {
        if (charSequence == null) {
            return "";
        }
        return charSequence.toString();
    }

    public boolean pinchOut(int i, int i2) throws UiObjectNotFoundException {
        if (i < 0) {
            i = 1;
        } else if (i > 100) {
            i = 100;
        }
        float f = i / 100.0f;
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        if (visibleBounds.width() <= 40) {
            throw new IllegalStateException("Object width is too small for operation");
        }
        return performTwoPointerGesture(new Point(visibleBounds.centerX() - 20, visibleBounds.centerY()), new Point(visibleBounds.centerX() + FINGER_TOUCH_HALF_WIDTH, visibleBounds.centerY()), new Point(visibleBounds.centerX() - ((int) ((visibleBounds.width() / 2) * f)), visibleBounds.centerY()), new Point(visibleBounds.centerX() + ((int) ((visibleBounds.width() / 2) * f)), visibleBounds.centerY()), i2);
    }

    public boolean pinchIn(int i, int i2) throws UiObjectNotFoundException {
        if (i < 0) {
            i = 0;
        } else if (i > 100) {
            i = 100;
        }
        float f = i / 100.0f;
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfo = findAccessibilityNodeInfo(this.mConfig.getWaitForSelectorTimeout());
        if (accessibilityNodeInfoFindAccessibilityNodeInfo == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect visibleBounds = getVisibleBounds(accessibilityNodeInfoFindAccessibilityNodeInfo);
        if (visibleBounds.width() <= 40) {
            throw new IllegalStateException("Object width is too small for operation");
        }
        return performTwoPointerGesture(new Point(visibleBounds.centerX() - ((int) ((visibleBounds.width() / 2) * f)), visibleBounds.centerY()), new Point(visibleBounds.centerX() + ((int) ((visibleBounds.width() / 2) * f)), visibleBounds.centerY()), new Point(visibleBounds.centerX() - 20, visibleBounds.centerY()), new Point(visibleBounds.centerX() + FINGER_TOUCH_HALF_WIDTH, visibleBounds.centerY()), i2);
    }

    public boolean performTwoPointerGesture(Point point, Point point2, Point point3, Point point4, int i) {
        int i2 = i == 0 ? 1 : i;
        float f = (point3.x - point.x) / i2;
        float f2 = (point3.y - point.y) / i2;
        float f3 = (point4.x - point2.x) / i2;
        float f4 = (point4.y - point2.y) / i2;
        int i3 = point.x;
        int i4 = point.y;
        int i5 = point2.x;
        int i6 = i2 + 2;
        MotionEvent.PointerCoords[] pointerCoordsArr = new MotionEvent.PointerCoords[i6];
        MotionEvent.PointerCoords[] pointerCoordsArr2 = new MotionEvent.PointerCoords[i6];
        int i7 = point2.y;
        int i8 = i4;
        int i9 = 0;
        while (true) {
            int i10 = i2 + 1;
            if (i9 < i10) {
                MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
                float f5 = i3;
                pointerCoords.x = f5;
                float f6 = i8;
                pointerCoords.y = f6;
                pointerCoords.pressure = 1.0f;
                pointerCoords.size = 1.0f;
                pointerCoordsArr[i9] = pointerCoords;
                MotionEvent.PointerCoords pointerCoords2 = new MotionEvent.PointerCoords();
                float f7 = i5;
                pointerCoords2.x = f7;
                float f8 = i7;
                pointerCoords2.y = f8;
                pointerCoords2.pressure = 1.0f;
                pointerCoords2.size = 1.0f;
                pointerCoordsArr2[i9] = pointerCoords2;
                i3 = (int) (f5 + f);
                i8 = (int) (f6 + f2);
                i5 = (int) (f7 + f3);
                i7 = (int) (f8 + f4);
                i9++;
            } else {
                MotionEvent.PointerCoords pointerCoords3 = new MotionEvent.PointerCoords();
                pointerCoords3.x = point3.x;
                pointerCoords3.y = point3.y;
                pointerCoords3.pressure = 1.0f;
                pointerCoords3.size = 1.0f;
                pointerCoordsArr[i10] = pointerCoords3;
                MotionEvent.PointerCoords pointerCoords4 = new MotionEvent.PointerCoords();
                pointerCoords4.x = point4.x;
                pointerCoords4.y = point4.y;
                pointerCoords4.pressure = 1.0f;
                pointerCoords4.size = 1.0f;
                pointerCoordsArr2[i10] = pointerCoords4;
                return performMultiPointerGesture(pointerCoordsArr, pointerCoordsArr2);
            }
        }
    }

    public boolean performMultiPointerGesture(MotionEvent.PointerCoords[]... pointerCoordsArr) {
        return getInteractionController().performMultiPointerGesture(pointerCoordsArr);
    }
}
