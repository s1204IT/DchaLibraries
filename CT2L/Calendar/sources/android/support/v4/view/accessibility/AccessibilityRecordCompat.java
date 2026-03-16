package android.support.v4.view.accessibility;

import android.os.Build;
import android.view.View;

public class AccessibilityRecordCompat {
    private static final AccessibilityRecordImpl IMPL;
    private final Object mRecord;

    interface AccessibilityRecordImpl {
        void setSource(Object obj, View view, int i);
    }

    static class AccessibilityRecordStubImpl implements AccessibilityRecordImpl {
        AccessibilityRecordStubImpl() {
        }

        @Override
        public void setSource(Object record, View root, int virtualDescendantId) {
        }
    }

    static class AccessibilityRecordIcsImpl extends AccessibilityRecordStubImpl {
        AccessibilityRecordIcsImpl() {
        }
    }

    static class AccessibilityRecordIcsMr1Impl extends AccessibilityRecordIcsImpl {
        AccessibilityRecordIcsMr1Impl() {
        }
    }

    static class AccessibilityRecordJellyBeanImpl extends AccessibilityRecordIcsMr1Impl {
        AccessibilityRecordJellyBeanImpl() {
        }

        @Override
        public void setSource(Object record, View root, int virtualDescendantId) {
            AccessibilityRecordCompatJellyBean.setSource(record, root, virtualDescendantId);
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 16) {
            IMPL = new AccessibilityRecordJellyBeanImpl();
            return;
        }
        if (Build.VERSION.SDK_INT >= 15) {
            IMPL = new AccessibilityRecordIcsMr1Impl();
        } else if (Build.VERSION.SDK_INT >= 14) {
            IMPL = new AccessibilityRecordIcsImpl();
        } else {
            IMPL = new AccessibilityRecordStubImpl();
        }
    }

    public AccessibilityRecordCompat(Object record) {
        this.mRecord = record;
    }

    public void setSource(View root, int virtualDescendantId) {
        IMPL.setSource(this.mRecord, root, virtualDescendantId);
    }

    public int hashCode() {
        if (this.mRecord == null) {
            return 0;
        }
        return this.mRecord.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            AccessibilityRecordCompat other = (AccessibilityRecordCompat) obj;
            return this.mRecord == null ? other.mRecord == null : this.mRecord.equals(other.mRecord);
        }
        return false;
    }
}
