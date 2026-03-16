package android.support.v4.view;

import android.view.View;

class AccessibilityDelegateCompatIcs {
    public static Object newAccessibilityDelegateDefaultImpl() {
        return new View.AccessibilityDelegate();
    }
}
