package com.android.launcher3.compat;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

public class AccessibilityManagerCompat {
    public static boolean isAccessibilityEnabled(Context context) {
        return getManager(context).isEnabled();
    }

    public static boolean isObservedEventType(Context context, int i) {
        return isAccessibilityEnabled(context);
    }

    public static void sendCustomAccessibilityEvent(View view, int i, String str) {
        if (isObservedEventType(view.getContext(), i)) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(i);
            view.onInitializeAccessibilityEvent(accessibilityEventObtain);
            accessibilityEventObtain.getText().add(str);
            getManager(view.getContext()).sendAccessibilityEvent(accessibilityEventObtain);
        }
    }

    private static AccessibilityManager getManager(Context context) {
        return (AccessibilityManager) context.getSystemService("accessibility");
    }
}
