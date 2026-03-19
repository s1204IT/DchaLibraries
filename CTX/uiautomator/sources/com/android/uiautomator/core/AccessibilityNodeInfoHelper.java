package com.android.uiautomator.core;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

class AccessibilityNodeInfoHelper {
    AccessibilityNodeInfoHelper() {
    }

    static Rect getVisibleBoundsInScreen(AccessibilityNodeInfo accessibilityNodeInfo, int i, int i2) {
        if (accessibilityNodeInfo == null) {
            return null;
        }
        Rect rect = new Rect();
        accessibilityNodeInfo.getBoundsInScreen(rect);
        Rect rect2 = new Rect();
        rect2.top = 0;
        rect2.left = 0;
        rect2.right = i;
        rect2.bottom = i2;
        if (rect.intersect(rect2)) {
            return rect;
        }
        return new Rect();
    }
}
