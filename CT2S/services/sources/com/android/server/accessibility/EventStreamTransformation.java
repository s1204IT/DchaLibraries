package com.android.server.accessibility;

import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;

interface EventStreamTransformation {
    void clear();

    void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);

    void onDestroy();

    void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

    void setNext(EventStreamTransformation eventStreamTransformation);
}
