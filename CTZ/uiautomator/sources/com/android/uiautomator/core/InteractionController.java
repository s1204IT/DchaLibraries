package com.android.uiautomator.core;

import android.app.UiAutomation;
import android.graphics.Point;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

class InteractionController {
    private static final int MOTION_EVENT_INJECTION_DELAY_MILLIS = 5;
    private static final long REGULAR_CLICK_LENGTH = 100;
    private long mDownTime;
    private final KeyCharacterMap mKeyCharacterMap = KeyCharacterMap.load(-1);
    private final UiAutomatorBridge mUiAutomatorBridge;
    private static final String LOG_TAG = InteractionController.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(LOG_TAG, 3);

    public InteractionController(UiAutomatorBridge uiAutomatorBridge) {
        this.mUiAutomatorBridge = uiAutomatorBridge;
    }

    class WaitForAnyEventPredicate implements UiAutomation.AccessibilityEventFilter {
        int mMask;

        WaitForAnyEventPredicate(int i) {
            this.mMask = i;
        }

        @Override
        public boolean accept(AccessibilityEvent accessibilityEvent) {
            if ((accessibilityEvent.getEventType() & this.mMask) != 0) {
                return true;
            }
            return false;
        }
    }

    class EventCollectingPredicate implements UiAutomation.AccessibilityEventFilter {
        List<AccessibilityEvent> mEventsList;
        int mMask;

        EventCollectingPredicate(int i, List<AccessibilityEvent> list) {
            this.mMask = i;
            this.mEventsList = list;
        }

        @Override
        public boolean accept(AccessibilityEvent accessibilityEvent) {
            if ((accessibilityEvent.getEventType() & this.mMask) != 0) {
                this.mEventsList.add(AccessibilityEvent.obtain(accessibilityEvent));
                return false;
            }
            return false;
        }
    }

    class WaitForAllEventPredicate implements UiAutomation.AccessibilityEventFilter {
        int mMask;

        WaitForAllEventPredicate(int i) {
            this.mMask = i;
        }

        @Override
        public boolean accept(AccessibilityEvent accessibilityEvent) {
            if ((accessibilityEvent.getEventType() & this.mMask) == 0) {
                return false;
            }
            this.mMask = (~accessibilityEvent.getEventType()) & this.mMask;
            return this.mMask == 0;
        }
    }

    private AccessibilityEvent runAndWaitForEvents(Runnable runnable, UiAutomation.AccessibilityEventFilter accessibilityEventFilter, long j) {
        try {
            return this.mUiAutomatorBridge.executeCommandAndWaitForAccessibilityEvent(runnable, accessibilityEventFilter, j);
        } catch (TimeoutException e) {
            Log.w(LOG_TAG, "runAndwaitForEvent timedout waiting for events");
            return null;
        } catch (Exception e2) {
            Log.e(LOG_TAG, "exception from executeCommandAndWaitForAccessibilityEvent", e2);
            return null;
        }
    }

    public boolean sendKeyAndWaitForEvent(final int i, final int i2, int i3, long j) {
        return runAndWaitForEvents(new Runnable() {
            @Override
            public void run() {
                long jUptimeMillis = SystemClock.uptimeMillis();
                if (InteractionController.this.injectEventSync(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, i, 0, i2, -1, 0, 0, 257))) {
                    InteractionController.this.injectEventSync(new KeyEvent(jUptimeMillis, jUptimeMillis, 1, i, 0, i2, -1, 0, 0, 257));
                }
            }
        }, new WaitForAnyEventPredicate(i3), j) != null;
    }

    public boolean clickNoSync(int i, int i2) {
        Log.d(LOG_TAG, "clickNoSync (" + i + ", " + i2 + ")");
        if (touchDown(i, i2)) {
            SystemClock.sleep(REGULAR_CLICK_LENGTH);
            if (touchUp(i, i2)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean clickAndSync(int i, int i2, long j) {
        Log.d(LOG_TAG, String.format("clickAndSync(%d, %d)", Integer.valueOf(i), Integer.valueOf(i2)));
        return runAndWaitForEvents(clickRunnable(i, i2), new WaitForAnyEventPredicate(2052), j) != null;
    }

    public boolean clickAndWaitForNewWindow(int i, int i2, long j) {
        Log.d(LOG_TAG, String.format("clickAndWaitForNewWindow(%d, %d)", Integer.valueOf(i), Integer.valueOf(i2)));
        return runAndWaitForEvents(clickRunnable(i, i2), new WaitForAllEventPredicate(2080), j) != null;
    }

    private Runnable clickRunnable(final int i, final int i2) {
        return new Runnable() {
            @Override
            public void run() {
                if (InteractionController.this.touchDown(i, i2)) {
                    SystemClock.sleep(InteractionController.REGULAR_CLICK_LENGTH);
                    InteractionController.this.touchUp(i, i2);
                }
            }
        };
    }

    public boolean longTapNoSync(int i, int i2) {
        if (DEBUG) {
            Log.d(LOG_TAG, "longTapNoSync (" + i + ", " + i2 + ")");
        }
        if (touchDown(i, i2)) {
            SystemClock.sleep(this.mUiAutomatorBridge.getSystemLongPressTime());
            if (touchUp(i, i2)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean touchDown(int i, int i2) {
        if (DEBUG) {
            Log.d(LOG_TAG, "touchDown (" + i + ", " + i2 + ")");
        }
        this.mDownTime = SystemClock.uptimeMillis();
        MotionEvent motionEventObtain = MotionEvent.obtain(this.mDownTime, this.mDownTime, 0, i, i2, 1);
        motionEventObtain.setSource(4098);
        return injectEventSync(motionEventObtain);
    }

    private boolean touchUp(int i, int i2) {
        if (DEBUG) {
            Log.d(LOG_TAG, "touchUp (" + i + ", " + i2 + ")");
        }
        MotionEvent motionEventObtain = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), 1, i, i2, 1);
        motionEventObtain.setSource(4098);
        this.mDownTime = 0L;
        return injectEventSync(motionEventObtain);
    }

    private boolean touchMove(int i, int i2) {
        if (DEBUG) {
            Log.d(LOG_TAG, "touchMove (" + i + ", " + i2 + ")");
        }
        MotionEvent motionEventObtain = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), 2, i, i2, 1);
        motionEventObtain.setSource(4098);
        return injectEventSync(motionEventObtain);
    }

    public boolean scrollSwipe(final int i, final int i2, final int i3, final int i4, final int i5) {
        Log.d(LOG_TAG, "scrollSwipe (" + i + ", " + i2 + ", " + i3 + ", " + i4 + ", " + i5 + ")");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                InteractionController.this.swipe(i, i2, i3, i4, i5);
            }
        };
        ArrayList arrayList = new ArrayList();
        runAndWaitForEvents(runnable, new EventCollectingPredicate(4096, arrayList), Configurator.getInstance().getScrollAcknowledgmentTimeout());
        AccessibilityEvent lastMatchingEvent = getLastMatchingEvent(arrayList, 4096);
        boolean z = false;
        if (lastMatchingEvent == null) {
            recycleAccessibilityEvents(arrayList);
            return false;
        }
        if (lastMatchingEvent.getFromIndex() != -1 && lastMatchingEvent.getToIndex() != -1 && lastMatchingEvent.getItemCount() != -1) {
            if (lastMatchingEvent.getFromIndex() == 0 || lastMatchingEvent.getItemCount() - 1 == lastMatchingEvent.getToIndex()) {
                z = true;
            }
            Log.d(LOG_TAG, "scrollSwipe reached scroll end: " + z);
        } else if (lastMatchingEvent.getScrollX() != -1 && lastMatchingEvent.getScrollY() != -1) {
            if (i == i3) {
                if (lastMatchingEvent.getScrollY() == 0 || lastMatchingEvent.getScrollY() == lastMatchingEvent.getMaxScrollY()) {
                    z = true;
                }
                Log.d(LOG_TAG, "Vertical scrollSwipe reached scroll end: " + z);
            } else if (i2 == i4) {
                if (lastMatchingEvent.getScrollX() == 0 || lastMatchingEvent.getScrollX() == lastMatchingEvent.getMaxScrollX()) {
                    z = true;
                }
                Log.d(LOG_TAG, "Horizontal scrollSwipe reached scroll end: " + z);
            }
        }
        recycleAccessibilityEvents(arrayList);
        return !z;
    }

    private AccessibilityEvent getLastMatchingEvent(List<AccessibilityEvent> list, int i) {
        for (int size = list.size(); size > 0; size--) {
            AccessibilityEvent accessibilityEvent = list.get(size - 1);
            if (accessibilityEvent.getEventType() == i) {
                return accessibilityEvent;
            }
        }
        return null;
    }

    private void recycleAccessibilityEvents(List<AccessibilityEvent> list) {
        Iterator<AccessibilityEvent> it = list.iterator();
        while (it.hasNext()) {
            it.next().recycle();
        }
        list.clear();
    }

    public boolean swipe(int i, int i2, int i3, int i4, int i5) {
        return swipe(i, i2, i3, i4, i5, false);
    }

    public boolean swipe(int i, int i2, int i3, int i4, int i5, boolean z) {
        int i6 = i5 == 0 ? 1 : i5;
        double d = i6;
        double d2 = ((double) (i3 - i)) / d;
        double d3 = ((double) (i4 - i2)) / d;
        boolean z2 = touchDown(i, i2);
        if (z) {
            SystemClock.sleep(this.mUiAutomatorBridge.getSystemLongPressTime());
        }
        for (int i7 = 1; i7 < i6; i7++) {
            double d4 = i7;
            z2 &= touchMove(i + ((int) (d2 * d4)), i2 + ((int) (d4 * d3)));
            if (!z2) {
                break;
            }
            SystemClock.sleep(5L);
        }
        if (z) {
            SystemClock.sleep(REGULAR_CLICK_LENGTH);
        }
        return touchUp(i3, i4) & z2;
    }

    public boolean swipe(Point[] pointArr, int i) {
        int i2;
        int i3;
        if (i != 0) {
            i2 = i;
        } else {
            i2 = 1;
        }
        int i4 = 0;
        if (pointArr.length == 0) {
            return false;
        }
        boolean z = touchDown(pointArr[0].x, pointArr[0].y);
        while (i4 < pointArr.length) {
            int i5 = i4 + 1;
            if (i5 < pointArr.length) {
                double d = i2;
                double d2 = ((double) (pointArr[i5].x - pointArr[i4].x)) / d;
                double d3 = ((double) (pointArr[i5].y - pointArr[i4].y)) / d;
                boolean z2 = z;
                int i6 = 1;
                while (true) {
                    if (i6 >= i) {
                        i3 = i2;
                        break;
                    }
                    double d4 = i6;
                    i3 = i2;
                    z2 &= touchMove(pointArr[i4].x + ((int) (d2 * d4)), pointArr[i4].y + ((int) (d4 * d3)));
                    if (!z2) {
                        break;
                    }
                    SystemClock.sleep(5L);
                    i6++;
                    i2 = i3;
                }
                z = z2;
            } else {
                i3 = i2;
            }
            i4 = i5;
            i2 = i3;
        }
        return touchUp(pointArr[pointArr.length - 1].x, pointArr[pointArr.length - 1].y) & z;
    }

    public boolean sendText(String str) {
        if (DEBUG) {
            Log.d(LOG_TAG, "sendText (" + str + ")");
        }
        KeyEvent[] events = this.mKeyCharacterMap.getEvents(str.toCharArray());
        if (events != null) {
            long keyInjectionDelay = Configurator.getInstance().getKeyInjectionDelay();
            for (KeyEvent keyEvent : events) {
                if (!injectEventSync(KeyEvent.changeTimeRepeat(keyEvent, SystemClock.uptimeMillis(), 0))) {
                    return false;
                }
                SystemClock.sleep(keyInjectionDelay);
            }
            return true;
        }
        return true;
    }

    public boolean sendKey(int i, int i2) {
        int i3;
        int i4;
        if (DEBUG) {
            String str = LOG_TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("sendKey (");
            i3 = i;
            sb.append(i3);
            sb.append(", ");
            i4 = i2;
            sb.append(i4);
            sb.append(")");
            Log.d(str, sb.toString());
        } else {
            i3 = i;
            i4 = i2;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (injectEventSync(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, i3, 0, i4, -1, 0, 0, 257)) && injectEventSync(new KeyEvent(jUptimeMillis, jUptimeMillis, 1, i3, 0, i2, -1, 0, 0, 257))) {
            return true;
        }
        return false;
    }

    public void setRotationRight() {
        this.mUiAutomatorBridge.setRotation(3);
    }

    public void setRotationLeft() {
        this.mUiAutomatorBridge.setRotation(1);
    }

    public void setRotationNatural() {
        this.mUiAutomatorBridge.setRotation(0);
    }

    public void freezeRotation() {
        this.mUiAutomatorBridge.setRotation(-1);
    }

    public void unfreezeRotation() {
        this.mUiAutomatorBridge.setRotation(-2);
    }

    public boolean wakeDevice() throws RemoteException {
        if (isScreenOn()) {
            return false;
        }
        sendKey(26, 0);
        return true;
    }

    public boolean sleepDevice() throws RemoteException {
        if (!isScreenOn()) {
            return false;
        }
        sendKey(26, 0);
        return true;
    }

    public boolean isScreenOn() throws RemoteException {
        return this.mUiAutomatorBridge.isScreenOn();
    }

    private boolean injectEventSync(InputEvent inputEvent) {
        return this.mUiAutomatorBridge.injectInputEvent(inputEvent, true);
    }

    private int getPointerAction(int i, int i2) {
        return i + (i2 << 8);
    }

    public boolean performMultiPointerGesture(MotionEvent.PointerCoords[]... pointerCoordsArr) {
        if (pointerCoordsArr.length < 2) {
            throw new IllegalArgumentException("Must provide coordinates for at least 2 pointers");
        }
        int length = 0;
        for (int i = 0; i < pointerCoordsArr.length; i++) {
            if (length < pointerCoordsArr[i].length) {
                length = pointerCoordsArr[i].length;
            }
        }
        MotionEvent.PointerProperties[] pointerPropertiesArr = new MotionEvent.PointerProperties[pointerCoordsArr.length];
        MotionEvent.PointerCoords[] pointerCoordsArr2 = new MotionEvent.PointerCoords[pointerCoordsArr.length];
        for (int i2 = 0; i2 < pointerCoordsArr.length; i2++) {
            MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
            pointerProperties.id = i2;
            pointerProperties.toolType = 1;
            pointerPropertiesArr[i2] = pointerProperties;
            pointerCoordsArr2[i2] = pointerCoordsArr[i2][0];
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        boolean zInjectEventSync = true & injectEventSync(MotionEvent.obtain(jUptimeMillis, SystemClock.uptimeMillis(), 0, 1, pointerPropertiesArr, pointerCoordsArr2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0));
        int i3 = 1;
        while (i3 < pointerCoordsArr.length) {
            int i4 = i3 + 1;
            zInjectEventSync &= injectEventSync(MotionEvent.obtain(jUptimeMillis, SystemClock.uptimeMillis(), getPointerAction(MOTION_EVENT_INJECTION_DELAY_MILLIS, i3), i4, pointerPropertiesArr, pointerCoordsArr2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0));
            i3 = i4;
        }
        for (int i5 = 1; i5 < length - 1; i5++) {
            for (int i6 = 0; i6 < pointerCoordsArr.length; i6++) {
                if (pointerCoordsArr[i6].length <= i5) {
                    pointerCoordsArr2[i6] = pointerCoordsArr[i6][pointerCoordsArr[i6].length - 1];
                } else {
                    pointerCoordsArr2[i6] = pointerCoordsArr[i6][i5];
                }
            }
            zInjectEventSync &= injectEventSync(MotionEvent.obtain(jUptimeMillis, SystemClock.uptimeMillis(), 2, pointerCoordsArr.length, pointerPropertiesArr, pointerCoordsArr2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0));
            SystemClock.sleep(5L);
        }
        for (int i7 = 0; i7 < pointerCoordsArr.length; i7++) {
            pointerCoordsArr2[i7] = pointerCoordsArr[i7][pointerCoordsArr[i7].length - 1];
        }
        int i8 = 1;
        while (i8 < pointerCoordsArr.length) {
            long jUptimeMillis2 = SystemClock.uptimeMillis();
            int pointerAction = getPointerAction(6, i8);
            i8++;
            zInjectEventSync &= injectEventSync(MotionEvent.obtain(jUptimeMillis, jUptimeMillis2, pointerAction, i8, pointerPropertiesArr, pointerCoordsArr2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0));
        }
        Log.i(LOG_TAG, "x " + pointerCoordsArr2[0].x);
        return zInjectEventSync & injectEventSync(MotionEvent.obtain(jUptimeMillis, SystemClock.uptimeMillis(), 1, 1, pointerPropertiesArr, pointerCoordsArr2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0));
    }

    public boolean toggleRecentApps() {
        return this.mUiAutomatorBridge.performGlobalAction(3);
    }

    public boolean openNotification() {
        return this.mUiAutomatorBridge.performGlobalAction(4);
    }

    public boolean openQuickSettings() {
        return this.mUiAutomatorBridge.performGlobalAction(MOTION_EVENT_INJECTION_DELAY_MILLIS);
    }
}
