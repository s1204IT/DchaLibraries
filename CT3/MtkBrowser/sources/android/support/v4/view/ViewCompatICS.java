package android.support.v4.view;

import android.support.annotation.Nullable;
import android.view.View;

class ViewCompatICS {
    ViewCompatICS() {
    }

    public static boolean canScrollHorizontally(View v, int direction) {
        return v.canScrollHorizontally(direction);
    }

    public static void setAccessibilityDelegate(View v, @Nullable Object delegate) {
        v.setAccessibilityDelegate((View.AccessibilityDelegate) delegate);
    }
}
