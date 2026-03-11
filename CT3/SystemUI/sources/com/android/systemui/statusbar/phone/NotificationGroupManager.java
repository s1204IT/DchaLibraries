package com.android.systemui.statusbar.phone;

import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class NotificationGroupManager implements HeadsUpManager.OnHeadsUpChangedListener {
    private HeadsUpManager mHeadsUpManager;
    private OnGroupChangeListener mListener;
    private final HashMap<String, NotificationGroup> mGroupMap = new HashMap<>();
    private int mBarState = -1;
    private HashMap<String, StatusBarNotification> mIsolatedEntries = new HashMap<>();

    public interface OnGroupChangeListener {
        void onGroupCreatedFromChildren(NotificationGroup notificationGroup);

        void onGroupExpansionChanged(ExpandableNotificationRow expandableNotificationRow, boolean z);

        void onGroupsChanged();
    }

    public void setOnGroupChangeListener(OnGroupChangeListener listener) {
        this.mListener = listener;
    }

    public boolean isGroupExpanded(StatusBarNotification sbn) {
        NotificationGroup group = this.mGroupMap.get(getGroupKey(sbn));
        if (group == null) {
            return false;
        }
        return group.expanded;
    }

    public void setGroupExpanded(StatusBarNotification sbn, boolean expanded) {
        NotificationGroup group = this.mGroupMap.get(getGroupKey(sbn));
        if (group == null) {
            return;
        }
        setGroupExpanded(group, expanded);
    }

    private void setGroupExpanded(NotificationGroup group, boolean expanded) {
        group.expanded = expanded;
        if (group.summary == null) {
            return;
        }
        this.mListener.onGroupExpansionChanged(group.summary.row, expanded);
    }

    public void onEntryRemoved(NotificationData.Entry removed) {
        onEntryRemovedInternal(removed, removed.notification);
        this.mIsolatedEntries.remove(removed.key);
    }

    private void onEntryRemovedInternal(NotificationData.Entry removed, StatusBarNotification sbn) {
        String groupKey = getGroupKey(sbn);
        NotificationGroup group = this.mGroupMap.get(groupKey);
        if (group == null) {
            return;
        }
        if (isGroupChild(sbn)) {
            group.children.remove(removed);
        } else {
            group.summary = null;
        }
        updateSuppression(group);
        if (!group.children.isEmpty() || group.summary != null) {
            return;
        }
        this.mGroupMap.remove(groupKey);
    }

    public void onEntryAdded(NotificationData.Entry added) {
        StatusBarNotification sbn = added.notification;
        boolean isGroupChild = isGroupChild(sbn);
        String groupKey = getGroupKey(sbn);
        NotificationGroup group = this.mGroupMap.get(groupKey);
        if (group == null) {
            group = new NotificationGroup();
            this.mGroupMap.put(groupKey, group);
        }
        if (isGroupChild) {
            group.children.add(added);
            updateSuppression(group);
            return;
        }
        group.summary = added;
        group.expanded = added.row.areChildrenExpanded();
        updateSuppression(group);
        if (group.children.isEmpty()) {
            return;
        }
        HashSet<NotificationData.Entry> childrenCopy = (HashSet) group.children.clone();
        for (NotificationData.Entry child : childrenCopy) {
            onEntryBecomingChild(child);
        }
        this.mListener.onGroupCreatedFromChildren(group);
    }

    private void onEntryBecomingChild(NotificationData.Entry entry) {
        if (!entry.row.isHeadsUp()) {
            return;
        }
        onHeadsUpStateChanged(entry, true);
    }

    private void updateSuppression(NotificationGroup group) {
        boolean zHasIsolatedChildren = true;
        if (group == null) {
            return;
        }
        boolean prevSuppressed = group.suppressed;
        if (group.summary == null || group.expanded) {
            zHasIsolatedChildren = false;
        } else if (group.children.size() != 1) {
            zHasIsolatedChildren = (group.children.size() == 0 && group.summary.notification.getNotification().isGroupSummary()) ? hasIsolatedChildren(group) : false;
        }
        group.suppressed = zHasIsolatedChildren;
        if (prevSuppressed == group.suppressed) {
            return;
        }
        if (group.suppressed) {
            handleSuppressedSummaryHeadsUpped(group.summary);
        }
        this.mListener.onGroupsChanged();
    }

    private boolean hasIsolatedChildren(NotificationGroup group) {
        return getNumberOfIsolatedChildren(group.summary.notification.getGroupKey()) != 0;
    }

    private int getNumberOfIsolatedChildren(String groupKey) {
        int count = 0;
        for (StatusBarNotification sbn : this.mIsolatedEntries.values()) {
            if (sbn.getGroupKey().equals(groupKey) && isIsolated(sbn)) {
                count++;
            }
        }
        return count;
    }

    private NotificationData.Entry getIsolatedChild(String groupKey) {
        for (StatusBarNotification sbn : this.mIsolatedEntries.values()) {
            if (sbn.getGroupKey().equals(groupKey) && isIsolated(sbn)) {
                return this.mGroupMap.get(sbn.getKey()).summary;
            }
        }
        return null;
    }

    public void onEntryUpdated(NotificationData.Entry entry, StatusBarNotification oldNotification) {
        if (this.mGroupMap.get(getGroupKey(oldNotification)) != null) {
            onEntryRemovedInternal(entry, oldNotification);
        }
        onEntryAdded(entry);
        if (isIsolated(entry.notification)) {
            this.mIsolatedEntries.put(entry.key, entry.notification);
            String oldKey = oldNotification.getGroupKey();
            String newKey = entry.notification.getGroupKey();
            if (oldKey.equals(newKey)) {
                return;
            }
            updateSuppression(this.mGroupMap.get(oldKey));
            updateSuppression(this.mGroupMap.get(newKey));
            return;
        }
        if (isGroupChild(oldNotification) || !isGroupChild(entry.notification)) {
            return;
        }
        onEntryBecomingChild(entry);
    }

    public boolean isSummaryOfSuppressedGroup(StatusBarNotification sbn) {
        if (isGroupSuppressed(getGroupKey(sbn))) {
            return sbn.getNotification().isGroupSummary();
        }
        return false;
    }

    private boolean isOnlyChild(StatusBarNotification sbn) {
        return !sbn.getNotification().isGroupSummary() && getTotalNumberOfChildren(sbn) == 1;
    }

    public boolean isOnlyChildInGroup(StatusBarNotification sbn) {
        ExpandableNotificationRow logicalGroupSummary;
        return (!isOnlyChild(sbn) || (logicalGroupSummary = getLogicalGroupSummary(sbn)) == null || logicalGroupSummary.getStatusBarNotification().equals(sbn)) ? false : true;
    }

    private int getTotalNumberOfChildren(StatusBarNotification sbn) {
        int isolatedChildren = getNumberOfIsolatedChildren(sbn.getGroupKey());
        NotificationGroup group = this.mGroupMap.get(sbn.getGroupKey());
        int realChildren = group != null ? group.children.size() : 0;
        return isolatedChildren + realChildren;
    }

    private boolean isGroupSuppressed(String groupKey) {
        NotificationGroup group = this.mGroupMap.get(groupKey);
        if (group != null) {
            return group.suppressed;
        }
        return false;
    }

    public void setStatusBarState(int newState) {
        if (this.mBarState == newState) {
            return;
        }
        this.mBarState = newState;
        if (this.mBarState != 1) {
            return;
        }
        collapseAllGroups();
    }

    public void collapseAllGroups() {
        ArrayList<NotificationGroup> groupCopy = new ArrayList<>(this.mGroupMap.values());
        int size = groupCopy.size();
        for (int i = 0; i < size; i++) {
            NotificationGroup group = groupCopy.get(i);
            if (group.expanded) {
                setGroupExpanded(group, false);
            }
            updateSuppression(group);
        }
    }

    public boolean isChildInGroupWithSummary(StatusBarNotification sbn) {
        NotificationGroup group;
        return (!isGroupChild(sbn) || (group = this.mGroupMap.get(getGroupKey(sbn))) == null || group.summary == null || group.suppressed || group.children.isEmpty()) ? false : true;
    }

    public boolean isSummaryOfGroup(StatusBarNotification sbn) {
        NotificationGroup group;
        return (!isGroupSummary(sbn) || (group = this.mGroupMap.get(getGroupKey(sbn))) == null || group.children.isEmpty()) ? false : true;
    }

    public ExpandableNotificationRow getGroupSummary(StatusBarNotification sbn) {
        return getGroupSummary(getGroupKey(sbn));
    }

    public ExpandableNotificationRow getLogicalGroupSummary(StatusBarNotification sbn) {
        return getGroupSummary(sbn.getGroupKey());
    }

    @Nullable
    private ExpandableNotificationRow getGroupSummary(String groupKey) {
        NotificationGroup group = this.mGroupMap.get(groupKey);
        if (group == null || group.summary == null) {
            return null;
        }
        return group.summary.row;
    }

    public boolean toggleGroupExpansion(StatusBarNotification sbn) {
        NotificationGroup group = this.mGroupMap.get(getGroupKey(sbn));
        if (group == null) {
            return false;
        }
        setGroupExpanded(group, group.expanded ? false : true);
        return group.expanded;
    }

    private boolean isIsolated(StatusBarNotification sbn) {
        return this.mIsolatedEntries.containsKey(sbn.getKey());
    }

    private boolean isGroupSummary(StatusBarNotification sbn) {
        if (isIsolated(sbn)) {
            return true;
        }
        return sbn.getNotification().isGroupSummary();
    }

    private boolean isGroupChild(StatusBarNotification sbn) {
        return (isIsolated(sbn) || !sbn.isGroup() || sbn.getNotification().isGroupSummary()) ? false : true;
    }

    private String getGroupKey(StatusBarNotification sbn) {
        if (isIsolated(sbn)) {
            return sbn.getKey();
        }
        return sbn.getGroupKey();
    }

    @Override
    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
    }

    @Override
    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
    }

    @Override
    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    @Override
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        StatusBarNotification sbn = entry.notification;
        if (entry.row.isHeadsUp()) {
            if (shouldIsolate(sbn)) {
                onEntryRemovedInternal(entry, entry.notification);
                this.mIsolatedEntries.put(sbn.getKey(), sbn);
                onEntryAdded(entry);
                updateSuppression(this.mGroupMap.get(entry.notification.getGroupKey()));
                this.mListener.onGroupsChanged();
                return;
            }
            handleSuppressedSummaryHeadsUpped(entry);
            return;
        }
        if (!this.mIsolatedEntries.containsKey(sbn.getKey())) {
            return;
        }
        onEntryRemovedInternal(entry, entry.notification);
        this.mIsolatedEntries.remove(sbn.getKey());
        onEntryAdded(entry);
        this.mListener.onGroupsChanged();
    }

    private void handleSuppressedSummaryHeadsUpped(NotificationData.Entry entry) {
        StatusBarNotification sbn = entry.notification;
        if (!isGroupSuppressed(sbn.getGroupKey()) || !sbn.getNotification().isGroupSummary() || !entry.row.isHeadsUp()) {
            return;
        }
        NotificationGroup notificationGroup = this.mGroupMap.get(sbn.getGroupKey());
        if (notificationGroup != null) {
            Iterator<NotificationData.Entry> iterator = notificationGroup.children.iterator();
            NotificationData.Entry child = iterator.hasNext() ? iterator.next() : null;
            if (child == null) {
                child = getIsolatedChild(sbn.getGroupKey());
            }
            if (child != null) {
                if (this.mHeadsUpManager.isHeadsUp(child.key)) {
                    this.mHeadsUpManager.updateNotification(child, true);
                } else {
                    this.mHeadsUpManager.showNotification(child);
                }
            }
        }
        this.mHeadsUpManager.releaseImmediately(entry.key);
    }

    private boolean shouldIsolate(StatusBarNotification sbn) {
        NotificationGroup notificationGroup = this.mGroupMap.get(sbn.getGroupKey());
        if (!sbn.isGroup() || sbn.getNotification().isGroupSummary()) {
            return false;
        }
        if (sbn.getNotification().fullScreenIntent != null || notificationGroup == null || !notificationGroup.expanded) {
            return true;
        }
        return isGroupNotFullyVisible(notificationGroup);
    }

    private boolean isGroupNotFullyVisible(NotificationGroup notificationGroup) {
        return notificationGroup.summary == null || notificationGroup.summary.row.getClipTopAmount() > 0 || notificationGroup.summary.row.getTranslationY() < 0.0f;
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GroupManager state:");
        pw.println("  number of groups: " + this.mGroupMap.size());
        for (Map.Entry<String, NotificationGroup> entry : this.mGroupMap.entrySet()) {
            pw.println("\n    key: " + entry.getKey());
            pw.println(entry.getValue());
        }
        pw.println("\n    isolated entries: " + this.mIsolatedEntries.size());
        for (Map.Entry<String, StatusBarNotification> entry2 : this.mIsolatedEntries.entrySet()) {
            pw.print("      ");
            pw.print(entry2.getKey());
            pw.print(", ");
            pw.println(entry2.getValue());
        }
    }

    public static class NotificationGroup {
        public final HashSet<NotificationData.Entry> children = new HashSet<>();
        public boolean expanded;
        public NotificationData.Entry summary;
        public boolean suppressed;

        public String toString() {
            String result = "    summary:\n      " + (this.summary != null ? this.summary.notification : "null");
            String result2 = result + "\n    children size: " + this.children.size();
            for (NotificationData.Entry child : this.children) {
                result2 = result2 + "\n      " + child.notification;
            }
            return result2;
        }
    }
}
