package android.support.v4.view.accessibility;

import android.view.accessibility.AccessibilityRecord;
/* loaded from: b.zip:android/support/v4/view/accessibility/AccessibilityRecordCompatIcs.class */
class AccessibilityRecordCompatIcs {
    AccessibilityRecordCompatIcs() {
    }

    public static void setFromIndex(Object obj, int i) {
        ((AccessibilityRecord) obj).setFromIndex(i);
    }

    public static void setItemCount(Object obj, int i) {
        ((AccessibilityRecord) obj).setItemCount(i);
    }

    public static void setScrollable(Object obj, boolean z) {
        ((AccessibilityRecord) obj).setScrollable(z);
    }

    public static void setToIndex(Object obj, int i) {
        ((AccessibilityRecord) obj).setToIndex(i);
    }
}
