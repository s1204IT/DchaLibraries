package com.android.systemui.statusbar.phone;

import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/* loaded from: classes.dex */
public class NotificationGroupManager implements OnHeadsUpChangedListener {
    private HeadsUpManager mHeadsUpManager;
    private boolean mIsUpdatingUnchangedGroup;
    private OnGroupChangeListener mListener;
    private HashMap<String, NotificationData.Entry> mPendingNotifications;
    private final HashMap<String, NotificationGroup> mGroupMap = new HashMap<>();
    private int mBarState = -1;
    private HashMap<String, StatusBarNotification> mIsolatedEntries = new HashMap<>();

    public interface OnGroupChangeListener {
        void onGroupCreatedFromChildren(NotificationGroup notificationGroup);

        void onGroupExpansionChanged(ExpandableNotificationRow expandableNotificationRow, boolean z);

        void onGroupsChanged();
    }

    public void setOnGroupChangeListener(OnGroupChangeListener onGroupChangeListener) {
        this.mListener = onGroupChangeListener;
    }

    public boolean isGroupExpanded(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification));
        if (notificationGroup == null) {
            return false;
        }
        return notificationGroup.expanded;
    }

    public void setGroupExpanded(StatusBarNotification statusBarNotification, boolean z) {
        NotificationGroup notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification));
        if (notificationGroup == null) {
            return;
        }
        setGroupExpanded(notificationGroup, z);
    }

    private void setGroupExpanded(NotificationGroup notificationGroup, boolean z) {
        notificationGroup.expanded = z;
        if (notificationGroup.summary != null) {
            this.mListener.onGroupExpansionChanged(notificationGroup.summary.row, z);
        }
    }

    public void onEntryRemoved(NotificationData.Entry entry) {
        onEntryRemovedInternal(entry, entry.notification);
        this.mIsolatedEntries.remove(entry.key);
    }

    private void onEntryRemovedInternal(NotificationData.Entry entry, StatusBarNotification statusBarNotification) {
        String groupKey = getGroupKey(statusBarNotification);
        NotificationGroup notificationGroup = this.mGroupMap.get(groupKey);
        if (notificationGroup == null) {
            return;
        }
        if (isGroupChild(statusBarNotification)) {
            notificationGroup.children.remove(entry.key);
        } else {
            notificationGroup.summary = null;
        }
        updateSuppression(notificationGroup);
        if (notificationGroup.children.isEmpty() && notificationGroup.summary == null) {
            this.mGroupMap.remove(groupKey);
        }
    }

    public void onEntryAdded(NotificationData.Entry entry) {
        String str;
        if (entry.row.isRemoved()) {
            entry.setDebugThrowable(new Throwable());
        }
        StatusBarNotification statusBarNotification = entry.notification;
        boolean zIsGroupChild = isGroupChild(statusBarNotification);
        String groupKey = getGroupKey(statusBarNotification);
        NotificationGroup notificationGroup = this.mGroupMap.get(groupKey);
        if (notificationGroup == null) {
            notificationGroup = new NotificationGroup();
            this.mGroupMap.put(groupKey, notificationGroup);
        }
        if (zIsGroupChild) {
            NotificationData.Entry entry2 = notificationGroup.children.get(entry.key);
            if (entry2 != null && entry2 != entry) {
                Throwable debugThrowable = entry2.getDebugThrowable();
                StringBuilder sb = new StringBuilder();
                sb.append("Inconsistent entries found with the same key ");
                sb.append(entry.key);
                sb.append("existing removed: ");
                sb.append(entry2.row.isRemoved());
                if (debugThrowable != null) {
                    str = Log.getStackTraceString(debugThrowable) + "\n";
                } else {
                    str = "";
                }
                sb.append(str);
                sb.append(" added removed");
                sb.append(entry.row.isRemoved());
                Log.wtf("NotificationGroupManager", sb.toString(), new Throwable());
            }
            notificationGroup.children.put(entry.key, entry);
            updateSuppression(notificationGroup);
        } else {
            notificationGroup.summary = entry;
            notificationGroup.expanded = entry.row.areChildrenExpanded();
            updateSuppression(notificationGroup);
            if (!notificationGroup.children.isEmpty()) {
                Iterator it = new ArrayList(notificationGroup.children.values()).iterator();
                while (it.hasNext()) {
                    onEntryBecomingChild((NotificationData.Entry) it.next());
                }
                this.mListener.onGroupCreatedFromChildren(notificationGroup);
            }
        }
        cleanUpHeadsUpStatesOnAdd(notificationGroup, false);
    }

    public void onPendingEntryAdded(NotificationData.Entry entry) {
        NotificationGroup notificationGroup = this.mGroupMap.get(getGroupKey(entry.notification));
        if (notificationGroup != null) {
            cleanUpHeadsUpStatesOnAdd(notificationGroup, true);
        }
    }

    private void cleanUpHeadsUpStatesOnAdd(NotificationGroup notificationGroup, boolean z) {
        boolean z2 = false;
        if (!z && notificationGroup.hunSummaryOnNextAddition) {
            if (!this.mHeadsUpManager.isHeadsUp(notificationGroup.summary.key)) {
                this.mHeadsUpManager.showNotification(notificationGroup.summary);
            }
            notificationGroup.hunSummaryOnNextAddition = false;
        }
        if (SystemClock.elapsedRealtime() - notificationGroup.lastHeadsUpTransfer >= 300 || !onlySummaryAlerts(notificationGroup.summary)) {
            return;
        }
        int size = notificationGroup.children.size();
        NotificationData.Entry isolatedChild = getIsolatedChild(getGroupKey(notificationGroup.summary.notification));
        int pendingChildrenNotAlerting = getPendingChildrenNotAlerting(notificationGroup);
        int i = size + pendingChildrenNotAlerting;
        if (isolatedChild != null) {
            i++;
        }
        if (i <= 1) {
            return;
        }
        ArrayList arrayList = new ArrayList(notificationGroup.children.values());
        int size2 = arrayList.size();
        boolean z3 = false;
        for (int i2 = 0; i2 < size2; i2++) {
            NotificationData.Entry entry = (NotificationData.Entry) arrayList.get(i2);
            if (onlySummaryAlerts(entry) && entry.row.isHeadsUp()) {
                this.mHeadsUpManager.releaseImmediately(entry.key);
                z3 = true;
            }
        }
        if (isolatedChild != null && onlySummaryAlerts(isolatedChild) && isolatedChild.row.isHeadsUp()) {
            this.mHeadsUpManager.releaseImmediately(isolatedChild.key);
            z3 = true;
        }
        if (z3 && !this.mHeadsUpManager.isHeadsUp(notificationGroup.summary.key)) {
            if (i - pendingChildrenNotAlerting > 1) {
                z2 = true;
            }
            if (z2) {
                this.mHeadsUpManager.showNotification(notificationGroup.summary);
            } else {
                notificationGroup.hunSummaryOnNextAddition = true;
            }
            notificationGroup.lastHeadsUpTransfer = 0L;
        }
    }

    private int getPendingChildrenNotAlerting(NotificationGroup notificationGroup) {
        int i = 0;
        if (this.mPendingNotifications == null) {
            return 0;
        }
        String groupKey = getGroupKey(notificationGroup.summary.notification);
        for (NotificationData.Entry entry : this.mPendingNotifications.values()) {
            if (isGroupChild(entry.notification) && Objects.equals(getGroupKey(entry.notification), groupKey) && !notificationGroup.children.containsKey(entry.key) && onlySummaryAlerts(entry)) {
                i++;
            }
        }
        return i;
    }

    private void onEntryBecomingChild(NotificationData.Entry entry) {
        if (entry.row.isHeadsUp()) {
            onHeadsUpStateChanged(entry, true);
        }
    }

    private void updateSuppression(NotificationGroup notificationGroup) {
        if (notificationGroup == null) {
            return;
        }
        boolean z = notificationGroup.suppressed;
        boolean z2 = true;
        if (notificationGroup.summary == null || notificationGroup.expanded || (notificationGroup.children.size() != 1 && (notificationGroup.children.size() != 0 || !notificationGroup.summary.notification.getNotification().isGroupSummary() || !hasIsolatedChildren(notificationGroup)))) {
            z2 = false;
        }
        notificationGroup.suppressed = z2;
        if (z != notificationGroup.suppressed) {
            if (notificationGroup.suppressed) {
                handleSuppressedSummaryHeadsUpped(notificationGroup.summary);
            }
            if (!this.mIsUpdatingUnchangedGroup && this.mListener != null) {
                this.mListener.onGroupsChanged();
            }
        }
    }

    private boolean hasIsolatedChildren(NotificationGroup notificationGroup) {
        return getNumberOfIsolatedChildren(notificationGroup.summary.notification.getGroupKey()) != 0;
    }

    private int getNumberOfIsolatedChildren(String str) {
        int i = 0;
        for (StatusBarNotification statusBarNotification : this.mIsolatedEntries.values()) {
            if (statusBarNotification.getGroupKey().equals(str) && isIsolated(statusBarNotification)) {
                i++;
            }
        }
        return i;
    }

    private NotificationData.Entry getIsolatedChild(String str) {
        for (StatusBarNotification statusBarNotification : this.mIsolatedEntries.values()) {
            if (statusBarNotification.getGroupKey().equals(str) && isIsolated(statusBarNotification)) {
                return this.mGroupMap.get(statusBarNotification.getKey()).summary;
            }
        }
        return null;
    }

    public void onEntryUpdated(NotificationData.Entry entry, StatusBarNotification statusBarNotification) {
        String groupKey = statusBarNotification.getGroupKey();
        String groupKey2 = entry.notification.getGroupKey();
        boolean z = true;
        boolean z2 = !groupKey.equals(groupKey2);
        boolean zIsGroupChild = isGroupChild(statusBarNotification);
        boolean zIsGroupChild2 = isGroupChild(entry.notification);
        if (z2 || zIsGroupChild != zIsGroupChild2) {
            z = false;
        }
        this.mIsUpdatingUnchangedGroup = z;
        if (this.mGroupMap.get(getGroupKey(statusBarNotification)) != null) {
            onEntryRemovedInternal(entry, statusBarNotification);
        }
        onEntryAdded(entry);
        this.mIsUpdatingUnchangedGroup = false;
        if (isIsolated(entry.notification)) {
            this.mIsolatedEntries.put(entry.key, entry.notification);
            if (z2) {
                updateSuppression(this.mGroupMap.get(groupKey));
                updateSuppression(this.mGroupMap.get(groupKey2));
                return;
            }
            return;
        }
        if (!zIsGroupChild && zIsGroupChild2) {
            onEntryBecomingChild(entry);
        }
    }

    public boolean isSummaryOfSuppressedGroup(StatusBarNotification statusBarNotification) {
        return isGroupSuppressed(getGroupKey(statusBarNotification)) && statusBarNotification.getNotification().isGroupSummary();
    }

    private boolean isOnlyChild(StatusBarNotification statusBarNotification) {
        return !statusBarNotification.getNotification().isGroupSummary() && getTotalNumberOfChildren(statusBarNotification) == 1;
    }

    public boolean isOnlyChildInGroup(StatusBarNotification statusBarNotification) {
        ExpandableNotificationRow logicalGroupSummary;
        return (!isOnlyChild(statusBarNotification) || (logicalGroupSummary = getLogicalGroupSummary(statusBarNotification)) == null || logicalGroupSummary.getStatusBarNotification().equals(statusBarNotification)) ? false : true;
    }

    private int getTotalNumberOfChildren(StatusBarNotification statusBarNotification) {
        int numberOfIsolatedChildren = getNumberOfIsolatedChildren(statusBarNotification.getGroupKey());
        NotificationGroup notificationGroup = this.mGroupMap.get(statusBarNotification.getGroupKey());
        return numberOfIsolatedChildren + (notificationGroup != null ? notificationGroup.children.size() : 0);
    }

    private boolean isGroupSuppressed(String str) {
        NotificationGroup notificationGroup = this.mGroupMap.get(str);
        return notificationGroup != null && notificationGroup.suppressed;
    }

    public void setStatusBarState(int i) {
        if (this.mBarState == i) {
            return;
        }
        this.mBarState = i;
        if (this.mBarState == 1) {
            collapseAllGroups();
        }
    }

    public void collapseAllGroups() {
        ArrayList arrayList = new ArrayList(this.mGroupMap.values());
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            NotificationGroup notificationGroup = (NotificationGroup) arrayList.get(i);
            if (notificationGroup.expanded) {
                setGroupExpanded(notificationGroup, false);
            }
            updateSuppression(notificationGroup);
        }
    }

    public boolean isChildInGroupWithSummary(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup;
        return (!isGroupChild(statusBarNotification) || (notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification))) == null || notificationGroup.summary == null || notificationGroup.suppressed || notificationGroup.children.isEmpty()) ? false : true;
    }

    public boolean isSummaryOfGroup(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup;
        if (isGroupSummary(statusBarNotification) && (notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification))) != null) {
            return !notificationGroup.children.isEmpty();
        }
        return false;
    }

    public ExpandableNotificationRow getGroupSummary(StatusBarNotification statusBarNotification) {
        return getGroupSummary(getGroupKey(statusBarNotification));
    }

    public ExpandableNotificationRow getLogicalGroupSummary(StatusBarNotification statusBarNotification) {
        return getGroupSummary(statusBarNotification.getGroupKey());
    }

    private ExpandableNotificationRow getGroupSummary(String str) {
        NotificationGroup notificationGroup = this.mGroupMap.get(str);
        if (notificationGroup == null || notificationGroup.summary == null) {
            return null;
        }
        return notificationGroup.summary.row;
    }

    public boolean toggleGroupExpansion(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification));
        if (notificationGroup == null) {
            return false;
        }
        setGroupExpanded(notificationGroup, !notificationGroup.expanded);
        return notificationGroup.expanded;
    }

    private boolean isIsolated(StatusBarNotification statusBarNotification) {
        return this.mIsolatedEntries.containsKey(statusBarNotification.getKey());
    }

    private boolean isGroupSummary(StatusBarNotification statusBarNotification) {
        if (isIsolated(statusBarNotification)) {
            return true;
        }
        return statusBarNotification.getNotification().isGroupSummary();
    }

    private boolean isGroupChild(StatusBarNotification statusBarNotification) {
        return (isIsolated(statusBarNotification) || !statusBarNotification.isGroup() || statusBarNotification.getNotification().isGroupSummary()) ? false : true;
    }

    private String getGroupKey(StatusBarNotification statusBarNotification) {
        if (isIsolated(statusBarNotification)) {
            return statusBarNotification.getKey();
        }
        return statusBarNotification.getGroupKey();
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinnedModeChanged(boolean z) {
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinned(ExpandableNotificationRow expandableNotificationRow) {
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpUnPinned(ExpandableNotificationRow expandableNotificationRow) {
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean z) {
        StatusBarNotification statusBarNotification = entry.notification;
        if (entry.row.isHeadsUp()) {
            if (shouldIsolate(statusBarNotification)) {
                onEntryRemovedInternal(entry, entry.notification);
                this.mIsolatedEntries.put(statusBarNotification.getKey(), statusBarNotification);
                onEntryAdded(entry);
                updateSuppression(this.mGroupMap.get(entry.notification.getGroupKey()));
                this.mListener.onGroupsChanged();
                return;
            }
            handleSuppressedSummaryHeadsUpped(entry);
            return;
        }
        if (this.mIsolatedEntries.containsKey(statusBarNotification.getKey())) {
            onEntryRemovedInternal(entry, entry.notification);
            this.mIsolatedEntries.remove(statusBarNotification.getKey());
            onEntryAdded(entry);
            this.mListener.onGroupsChanged();
        }
    }

    private void handleSuppressedSummaryHeadsUpped(NotificationData.Entry entry) {
        StatusBarNotification statusBarNotification = entry.notification;
        if (!isGroupSuppressed(statusBarNotification.getGroupKey()) || !statusBarNotification.getNotification().isGroupSummary() || !entry.row.isHeadsUp()) {
            return;
        }
        NotificationGroup notificationGroup = this.mGroupMap.get(statusBarNotification.getGroupKey());
        if (pendingInflationsWillAddChildren(notificationGroup)) {
            return;
        }
        if (notificationGroup != null) {
            Iterator<NotificationData.Entry> it = notificationGroup.children.values().iterator();
            NotificationData.Entry next = it.hasNext() ? it.next() : null;
            if (next == null) {
                next = getIsolatedChild(statusBarNotification.getGroupKey());
            }
            if (next != null) {
                if (next.row.keepInParent() || next.row.isRemoved() || next.row.isDismissed()) {
                    return;
                }
                if (this.mHeadsUpManager.isHeadsUp(next.key)) {
                    this.mHeadsUpManager.updateNotification(next, true);
                } else {
                    if (onlySummaryAlerts(entry)) {
                        notificationGroup.lastHeadsUpTransfer = SystemClock.elapsedRealtime();
                    }
                    this.mHeadsUpManager.showNotification(next);
                }
            }
        }
        this.mHeadsUpManager.releaseImmediately(entry.key);
    }

    private boolean onlySummaryAlerts(NotificationData.Entry entry) {
        return entry.notification.getNotification().getGroupAlertBehavior() == 1;
    }

    private boolean pendingInflationsWillAddChildren(NotificationGroup notificationGroup) {
        if (this.mPendingNotifications == null) {
            return false;
        }
        Collection<NotificationData.Entry> collectionValues = this.mPendingNotifications.values();
        String groupKey = getGroupKey(notificationGroup.summary.notification);
        for (NotificationData.Entry entry : collectionValues) {
            if (isGroupChild(entry.notification) && Objects.equals(getGroupKey(entry.notification), groupKey) && !notificationGroup.children.containsKey(entry.key)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldIsolate(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup = this.mGroupMap.get(statusBarNotification.getGroupKey());
        return statusBarNotification.isGroup() && !statusBarNotification.getNotification().isGroupSummary() && (statusBarNotification.getNotification().fullScreenIntent != null || notificationGroup == null || !notificationGroup.expanded || isGroupNotFullyVisible(notificationGroup));
    }

    private boolean isGroupNotFullyVisible(NotificationGroup notificationGroup) {
        return notificationGroup.summary == null || notificationGroup.summary.row.getClipTopAmount() > 0 || notificationGroup.summary.row.getTranslationY() < 0.0f;
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("GroupManager state:");
        printWriter.println("  number of groups: " + this.mGroupMap.size());
        for (Map.Entry<String, NotificationGroup> entry : this.mGroupMap.entrySet()) {
            printWriter.println("\n    key: " + entry.getKey());
            printWriter.println(entry.getValue());
        }
        printWriter.println("\n    isolated entries: " + this.mIsolatedEntries.size());
        for (Map.Entry<String, StatusBarNotification> entry2 : this.mIsolatedEntries.entrySet()) {
            printWriter.print("      ");
            printWriter.print(entry2.getKey());
            printWriter.print(", ");
            printWriter.println(entry2.getValue());
        }
    }

    public void setPendingEntries(HashMap<String, NotificationData.Entry> map) {
        this.mPendingNotifications = map;
    }

    public static class NotificationGroup {
        public final HashMap<String, NotificationData.Entry> children = new HashMap<>();
        public boolean expanded;
        public boolean hunSummaryOnNextAddition;
        public long lastHeadsUpTransfer;
        public NotificationData.Entry summary;
        public boolean suppressed;

        public String toString() {
            String stackTraceString;
            String stackTraceString2;
            StringBuilder sb = new StringBuilder();
            sb.append("    summary:\n      ");
            sb.append(this.summary != null ? this.summary.notification : "null");
            if (this.summary != null && this.summary.getDebugThrowable() != null) {
                stackTraceString = Log.getStackTraceString(this.summary.getDebugThrowable());
            } else {
                stackTraceString = "";
            }
            sb.append(stackTraceString);
            String string = sb.toString() + "\n    children size: " + this.children.size();
            for (NotificationData.Entry entry : this.children.values()) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(string);
                sb2.append("\n      ");
                sb2.append(entry.notification);
                if (entry.getDebugThrowable() != null) {
                    stackTraceString2 = Log.getStackTraceString(entry.getDebugThrowable());
                } else {
                    stackTraceString2 = "";
                }
                sb2.append(stackTraceString2);
                string = sb2.toString();
            }
            return string;
        }
    }
}
