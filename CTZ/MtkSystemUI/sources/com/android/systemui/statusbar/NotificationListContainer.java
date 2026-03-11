package com.android.systemui.statusbar;

import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationLogger;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;

public interface NotificationListContainer {
    void addContainerView(View view);

    void changeViewPosition(View view, int i);

    void cleanUpViewState(View view);

    void generateAddAnimation(View view, boolean z);

    void generateChildOrderChangedEvent();

    View getContainerChildAt(int i);

    int getContainerChildCount();

    NotificationSwipeActionHelper getSwipeActionHelper();

    ViewGroup getViewParentForNotification(NotificationData.Entry entry);

    boolean hasPulsingNotifications();

    boolean isInVisibleLocation(ExpandableNotificationRow expandableNotificationRow);

    void notifyGroupChildAdded(View view);

    void notifyGroupChildRemoved(View view, ViewGroup viewGroup);

    void onHeightChanged(ExpandableView expandableView, boolean z);

    void removeContainerView(View view);

    void resetExposedMenuView(boolean z, boolean z2);

    void setChildLocationsChangedListener(NotificationLogger.OnChildLocationsChangedListener onChildLocationsChangedListener);

    void setChildTransferInProgress(boolean z);

    void setMaxDisplayedNotifications(int i);

    void snapViewIfNeeded(ExpandableNotificationRow expandableNotificationRow);

    default void onNotificationViewUpdateFinished() {
    }

    default void applyExpandAnimationParams(ActivityLaunchAnimator.ExpandAnimationParameters expandAnimationParameters) {
    }

    default void setExpandingNotification(ExpandableNotificationRow expandableNotificationRow) {
    }

    default void bindRow(ExpandableNotificationRow expandableNotificationRow) {
    }
}
