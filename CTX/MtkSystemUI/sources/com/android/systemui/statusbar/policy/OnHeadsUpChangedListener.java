package com.android.systemui.statusbar.policy;

import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;

public interface OnHeadsUpChangedListener {
    default void onHeadsUpPinnedModeChanged(boolean z) {
    }

    default void onHeadsUpPinned(ExpandableNotificationRow expandableNotificationRow) {
    }

    default void onHeadsUpUnPinned(ExpandableNotificationRow expandableNotificationRow) {
    }

    default void onHeadsUpStateChanged(NotificationData.Entry entry, boolean z) {
    }
}
