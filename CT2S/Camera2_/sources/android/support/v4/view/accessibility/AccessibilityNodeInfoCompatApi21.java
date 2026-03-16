package android.support.v4.view.accessibility;

import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

class AccessibilityNodeInfoCompatApi21 {
    AccessibilityNodeInfoCompatApi21() {
    }

    static List<Object> getActionList(Object info) {
        return ((AccessibilityNodeInfo) info).getActionList();
    }

    static void addAction(Object info, Object action) {
        ((AccessibilityNodeInfo) info).addAction((AccessibilityNodeInfo.AccessibilityAction) action);
    }

    public static Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical, int selectionMode) {
        return AccessibilityNodeInfo.CollectionInfo.obtain(rowCount, columnCount, hierarchical, selectionMode);
    }

    public static Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex, int columnSpan, boolean heading, boolean selected) {
        return AccessibilityNodeInfo.CollectionItemInfo.obtain(rowIndex, rowSpan, columnIndex, columnSpan, heading, selected);
    }

    static class CollectionItemInfo {
        CollectionItemInfo() {
        }

        public static boolean isSelected(Object info) {
            return ((AccessibilityNodeInfo.CollectionItemInfo) info).isSelected();
        }
    }

    static Object newAccessibilityAction(int actionId, CharSequence label) {
        return new AccessibilityNodeInfo.AccessibilityAction(actionId, label);
    }

    static int getAccessibilityActionId(Object action) {
        return ((AccessibilityNodeInfo.AccessibilityAction) action).getId();
    }

    static CharSequence getAccessibilityActionLabel(Object action) {
        return ((AccessibilityNodeInfo.AccessibilityAction) action).getLabel();
    }
}
