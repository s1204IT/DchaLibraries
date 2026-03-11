package android.support.v4.view.accessibility;

import android.os.Build;
import android.view.accessibility.AccessibilityRecord;

public class AccessibilityRecordCompat {
    private final AccessibilityRecord mRecord;

    public static void setMaxScrollX(AccessibilityRecord accessibilityRecord, int i) {
        if (Build.VERSION.SDK_INT >= 15) {
            accessibilityRecord.setMaxScrollX(i);
        }
    }

    public static void setMaxScrollY(AccessibilityRecord accessibilityRecord, int i) {
        if (Build.VERSION.SDK_INT >= 15) {
            accessibilityRecord.setMaxScrollY(i);
        }
    }

    @Deprecated
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            AccessibilityRecordCompat accessibilityRecordCompat = (AccessibilityRecordCompat) obj;
            return this.mRecord == null ? accessibilityRecordCompat.mRecord == null : this.mRecord.equals(accessibilityRecordCompat.mRecord);
        }
        return false;
    }

    @Deprecated
    public int hashCode() {
        if (this.mRecord == null) {
            return 0;
        }
        return this.mRecord.hashCode();
    }
}
