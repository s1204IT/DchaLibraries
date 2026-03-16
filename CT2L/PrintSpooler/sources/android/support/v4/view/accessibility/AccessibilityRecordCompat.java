package android.support.v4.view.accessibility;

import android.os.Build;

public class AccessibilityRecordCompat {
    private static final AccessibilityRecordImpl IMPL;
    private final Object mRecord;

    interface AccessibilityRecordImpl {
        void setFromIndex(Object obj, int i);

        void setItemCount(Object obj, int i);

        void setScrollable(Object obj, boolean z);

        void setToIndex(Object obj, int i);
    }

    static class AccessibilityRecordStubImpl implements AccessibilityRecordImpl {
        AccessibilityRecordStubImpl() {
        }

        @Override
        public void setFromIndex(Object record, int fromIndex) {
        }

        @Override
        public void setItemCount(Object record, int itemCount) {
        }

        @Override
        public void setScrollable(Object record, boolean scrollable) {
        }

        @Override
        public void setToIndex(Object record, int toIndex) {
        }
    }

    static class AccessibilityRecordIcsImpl extends AccessibilityRecordStubImpl {
        AccessibilityRecordIcsImpl() {
        }

        @Override
        public void setFromIndex(Object record, int fromIndex) {
            AccessibilityRecordCompatIcs.setFromIndex(record, fromIndex);
        }

        @Override
        public void setItemCount(Object record, int itemCount) {
            AccessibilityRecordCompatIcs.setItemCount(record, itemCount);
        }

        @Override
        public void setScrollable(Object record, boolean scrollable) {
            AccessibilityRecordCompatIcs.setScrollable(record, scrollable);
        }

        @Override
        public void setToIndex(Object record, int toIndex) {
            AccessibilityRecordCompatIcs.setToIndex(record, toIndex);
        }
    }

    static class AccessibilityRecordIcsMr1Impl extends AccessibilityRecordIcsImpl {
        AccessibilityRecordIcsMr1Impl() {
        }
    }

    static class AccessibilityRecordJellyBeanImpl extends AccessibilityRecordIcsMr1Impl {
        AccessibilityRecordJellyBeanImpl() {
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

    public void setScrollable(boolean scrollable) {
        IMPL.setScrollable(this.mRecord, scrollable);
    }

    public void setItemCount(int itemCount) {
        IMPL.setItemCount(this.mRecord, itemCount);
    }

    public void setFromIndex(int fromIndex) {
        IMPL.setFromIndex(this.mRecord, fromIndex);
    }

    public void setToIndex(int toIndex) {
        IMPL.setToIndex(this.mRecord, toIndex);
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
