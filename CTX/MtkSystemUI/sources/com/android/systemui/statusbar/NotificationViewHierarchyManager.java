package com.android.systemui.statusbar;

import android.content.Context;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class NotificationViewHierarchyManager {
    private final boolean mAlwaysExpandNonGroupedNotification;
    private NotificationEntryManager mEntryManager;
    private NotificationListContainer mListContainer;
    private NotificationPresenter mPresenter;
    private final HashMap<ExpandableNotificationRow, List<ExpandableNotificationRow>> mTmpChildOrderMap = new HashMap<>();
    protected final NotificationLockscreenUserManager mLockscreenUserManager = (NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class);
    protected final NotificationGroupManager mGroupManager = (NotificationGroupManager) Dependency.get(NotificationGroupManager.class);
    protected final VisualStabilityManager mVisualStabilityManager = (VisualStabilityManager) Dependency.get(VisualStabilityManager.class);

    public NotificationViewHierarchyManager(Context context) {
        this.mAlwaysExpandNonGroupedNotification = context.getResources().getBoolean(R.bool.config_alwaysExpandNonGroupedNotifications);
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, NotificationEntryManager notificationEntryManager, NotificationListContainer notificationListContainer) {
        this.mPresenter = notificationPresenter;
        this.mEntryManager = notificationEntryManager;
        this.mListContainer = notificationListContainer;
    }

    public void updateNotificationViews() {
        ArrayList<NotificationData.Entry> activeNotifications = this.mEntryManager.getNotificationData().getActiveNotifications();
        ArrayList arrayList = new ArrayList(activeNotifications.size());
        int size = activeNotifications.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            }
            NotificationData.Entry entry = activeNotifications.get(i);
            if (!entry.row.isDismissed() && !entry.row.isRemoved()) {
                int userId = entry.notification.getUserId();
                boolean zIsLockscreenPublicMode = this.mLockscreenUserManager.isLockscreenPublicMode(this.mLockscreenUserManager.getCurrentUserId());
                boolean z = zIsLockscreenPublicMode || this.mLockscreenUserManager.isLockscreenPublicMode(userId);
                boolean zNeedsRedaction = this.mLockscreenUserManager.needsRedaction(entry);
                entry.row.setSensitive(z && zNeedsRedaction, zIsLockscreenPublicMode && !this.mLockscreenUserManager.userAllowsPrivateNotificationsInPublic(this.mLockscreenUserManager.getCurrentUserId()));
                entry.row.setNeedsRedaction(zNeedsRedaction);
                if (this.mGroupManager.isChildInGroupWithSummary(entry.row.getStatusBarNotification())) {
                    ExpandableNotificationRow groupSummary = this.mGroupManager.getGroupSummary(entry.row.getStatusBarNotification());
                    List<ExpandableNotificationRow> arrayList2 = this.mTmpChildOrderMap.get(groupSummary);
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList<>();
                        this.mTmpChildOrderMap.put(groupSummary, arrayList2);
                    }
                    arrayList2.add(entry.row);
                } else {
                    arrayList.add(entry.row);
                }
            }
            i++;
        }
        ArrayList<ExpandableNotificationRow> arrayList3 = new ArrayList();
        for (int i2 = 0; i2 < this.mListContainer.getContainerChildCount(); i2++) {
            View containerChildAt = this.mListContainer.getContainerChildAt(i2);
            if (!arrayList.contains(containerChildAt) && (containerChildAt instanceof ExpandableNotificationRow)) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) containerChildAt;
                if (!expandableNotificationRow.isBlockingHelperShowing()) {
                    arrayList3.add(expandableNotificationRow);
                }
            }
        }
        for (ExpandableNotificationRow expandableNotificationRow2 : arrayList3) {
            if (this.mGroupManager.isChildInGroupWithSummary(expandableNotificationRow2.getStatusBarNotification())) {
                this.mListContainer.setChildTransferInProgress(true);
            }
            if (expandableNotificationRow2.isSummaryWithChildren()) {
                expandableNotificationRow2.removeAllChildren();
            }
            this.mListContainer.removeContainerView(expandableNotificationRow2);
            this.mListContainer.setChildTransferInProgress(false);
        }
        removeNotificationChildren();
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            View view = (View) arrayList.get(i3);
            if (view.getParent() == null) {
                this.mVisualStabilityManager.notifyViewAddition(view);
                this.mListContainer.addContainerView(view);
            }
        }
        addNotificationChildrenAndSort();
        int i4 = 0;
        for (int i5 = 0; i5 < this.mListContainer.getContainerChildCount(); i5++) {
            View containerChildAt2 = this.mListContainer.getContainerChildAt(i5);
            if ((containerChildAt2 instanceof ExpandableNotificationRow) && !((ExpandableNotificationRow) containerChildAt2).isBlockingHelperShowing()) {
                ExpandableNotificationRow expandableNotificationRow3 = (ExpandableNotificationRow) arrayList.get(i4);
                if (containerChildAt2 != expandableNotificationRow3) {
                    if (this.mVisualStabilityManager.canReorderNotification(expandableNotificationRow3)) {
                        this.mListContainer.changeViewPosition(expandableNotificationRow3, i5);
                    } else {
                        this.mVisualStabilityManager.addReorderingAllowedCallback(this.mEntryManager);
                    }
                }
                i4++;
            }
        }
        this.mVisualStabilityManager.onReorderingFinished();
        this.mTmpChildOrderMap.clear();
        updateRowStates();
        this.mListContainer.onNotificationViewUpdateFinished();
    }

    private void addNotificationChildrenAndSort() {
        boolean zApplyChildOrder = false;
        for (int i = 0; i < this.mListContainer.getContainerChildCount(); i++) {
            View containerChildAt = this.mListContainer.getContainerChildAt(i);
            if (containerChildAt instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) containerChildAt;
                List<ExpandableNotificationRow> notificationChildren = expandableNotificationRow.getNotificationChildren();
                List<ExpandableNotificationRow> list = this.mTmpChildOrderMap.get(expandableNotificationRow);
                for (int i2 = 0; list != null && i2 < list.size(); i2++) {
                    ExpandableNotificationRow expandableNotificationRow2 = list.get(i2);
                    if (notificationChildren == null || !notificationChildren.contains(expandableNotificationRow2)) {
                        if (expandableNotificationRow2.getParent() != null) {
                            Log.wtf("NotificationViewHierarchyManager", "trying to add a notification child that already has a parent. class:" + expandableNotificationRow2.getParent().getClass() + "\n child: " + expandableNotificationRow2);
                            ((ViewGroup) expandableNotificationRow2.getParent()).removeView(expandableNotificationRow2);
                        }
                        this.mVisualStabilityManager.notifyViewAddition(expandableNotificationRow2);
                        expandableNotificationRow.addChildNotification(expandableNotificationRow2, i2);
                        this.mListContainer.notifyGroupChildAdded(expandableNotificationRow2);
                    }
                }
                zApplyChildOrder |= expandableNotificationRow.applyChildOrder(list, this.mVisualStabilityManager, this.mEntryManager);
            }
        }
        if (zApplyChildOrder) {
            this.mListContainer.generateChildOrderChangedEvent();
        }
    }

    private void removeNotificationChildren() {
        ArrayList<ExpandableNotificationRow> arrayList = new ArrayList();
        for (int i = 0; i < this.mListContainer.getContainerChildCount(); i++) {
            View containerChildAt = this.mListContainer.getContainerChildAt(i);
            if (containerChildAt instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) containerChildAt;
                List<ExpandableNotificationRow> notificationChildren = expandableNotificationRow.getNotificationChildren();
                List<ExpandableNotificationRow> list = this.mTmpChildOrderMap.get(expandableNotificationRow);
                if (notificationChildren != null) {
                    arrayList.clear();
                    for (ExpandableNotificationRow expandableNotificationRow2 : notificationChildren) {
                        if (list == null || !list.contains(expandableNotificationRow2)) {
                            if (!expandableNotificationRow2.keepInParent()) {
                                arrayList.add(expandableNotificationRow2);
                            }
                        }
                    }
                    for (ExpandableNotificationRow expandableNotificationRow3 : arrayList) {
                        expandableNotificationRow.removeChildNotification(expandableNotificationRow3);
                        if (this.mEntryManager.getNotificationData().get(expandableNotificationRow3.getStatusBarNotification().getKey()) == null) {
                            this.mListContainer.notifyGroupChildRemoved(expandableNotificationRow3, expandableNotificationRow.getChildrenContainer());
                        }
                    }
                }
            }
        }
    }

    public void updateRowStates() {
        int maxNotificationsWhileLocked;
        Trace.beginSection("NotificationViewHierarchyManager#updateRowStates");
        int containerChildCount = this.mListContainer.getContainerChildCount();
        boolean zIsPresenterLocked = this.mPresenter.isPresenterLocked();
        if (zIsPresenterLocked) {
            maxNotificationsWhileLocked = this.mPresenter.getMaxNotificationsWhileLocked(true);
        } else {
            maxNotificationsWhileLocked = -1;
        }
        this.mListContainer.setMaxDisplayedNotifications(maxNotificationsWhileLocked);
        Stack stack = new Stack();
        for (int i = containerChildCount - 1; i >= 0; i--) {
            View containerChildAt = this.mListContainer.getContainerChildAt(i);
            if (containerChildAt instanceof ExpandableNotificationRow) {
                stack.push((ExpandableNotificationRow) containerChildAt);
            }
        }
        int i2 = 0;
        while (!stack.isEmpty()) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) stack.pop();
            NotificationData.Entry entry = expandableNotificationRow.getEntry();
            boolean zIsChildInGroupWithSummary = this.mGroupManager.isChildInGroupWithSummary(entry.notification);
            expandableNotificationRow.setOnKeyguard(zIsPresenterLocked);
            if (!zIsPresenterLocked) {
                expandableNotificationRow.setSystemExpanded(this.mAlwaysExpandNonGroupedNotification || !(i2 != 0 || zIsChildInGroupWithSummary || expandableNotificationRow.isLowPriority()));
            }
            entry.row.setShowAmbient(this.mPresenter.isDozing());
            int userId = entry.notification.getUserId();
            boolean z = this.mGroupManager.isSummaryOfSuppressedGroup(entry.notification) && !entry.row.isRemoved();
            boolean zShouldShowOnKeyguard = this.mLockscreenUserManager.shouldShowOnKeyguard(entry.notification);
            if (z || this.mLockscreenUserManager.shouldHideNotifications(userId) || (zIsPresenterLocked && !zShouldShowOnKeyguard)) {
                entry.row.setVisibility(8);
            } else {
                boolean z2 = entry.row.getVisibility() == 8;
                if (z2) {
                    entry.row.setVisibility(0);
                }
                if (!zIsChildInGroupWithSummary && !entry.row.isRemoved()) {
                    if (z2) {
                        this.mListContainer.generateAddAnimation(entry.row, !zShouldShowOnKeyguard);
                    }
                    i2++;
                }
            }
            if (expandableNotificationRow.isSummaryWithChildren()) {
                List<ExpandableNotificationRow> notificationChildren = expandableNotificationRow.getNotificationChildren();
                for (int size = notificationChildren.size() - 1; size >= 0; size--) {
                    stack.push(notificationChildren.get(size));
                }
            }
            expandableNotificationRow.showAppOpsIcons(entry.mActiveAppOps);
        }
        Trace.beginSection("NotificationPresenter#onUpdateRowStates");
        this.mPresenter.onUpdateRowStates();
        Trace.endSection();
        Trace.endSection();
    }
}
