package com.android.systemui.statusbar;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.View;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NotificationData {
    private final Environment mEnvironment;
    private NotificationListenerService.RankingMap mRankingMap;
    private final ArrayMap<String, Entry> mEntries = new ArrayMap<>();
    private final ArrayList<Entry> mSortedAndFiltered = new ArrayList<>();
    private ArraySet<String> mGroupsWithSummaries = new ArraySet<>();
    private final NotificationListenerService.Ranking mTmpRanking = new NotificationListenerService.Ranking();
    private final Comparator<Entry> mRankingComparator = new Comparator<Entry>() {
        private final NotificationListenerService.Ranking mRankingA = new NotificationListenerService.Ranking();
        private final NotificationListenerService.Ranking mRankingB = new NotificationListenerService.Ranking();

        @Override
        public int compare(Entry a, Entry b) {
            String mediaNotification = NotificationData.this.mEnvironment.getCurrentMediaNotificationKey();
            boolean aMedia = a.key.equals(mediaNotification);
            boolean bMedia = b.key.equals(mediaNotification);
            if (aMedia != bMedia) {
                return aMedia ? -1 : 1;
            }
            StatusBarNotification na = a.notification;
            StatusBarNotification nb = b.notification;
            boolean aSystemMax = na.getNotification().priority >= 2 && NotificationData.isSystemNotification(na);
            boolean bSystemMax = nb.getNotification().priority >= 2 && NotificationData.isSystemNotification(nb);
            if (aSystemMax != bSystemMax) {
                return !aSystemMax ? 1 : -1;
            }
            if (NotificationData.this.mRankingMap != null) {
                NotificationData.this.mRankingMap.getRanking(a.key, this.mRankingA);
                NotificationData.this.mRankingMap.getRanking(b.key, this.mRankingB);
                return this.mRankingA.getRank() - this.mRankingB.getRank();
            }
            int d = nb.getScore() - na.getScore();
            return a.interruption != b.interruption ? !a.interruption ? 1 : -1 : d != 0 ? d : (int) (nb.getNotification().when - na.getNotification().when);
        }
    };

    public interface Environment {
        String getCurrentMediaNotificationKey();

        boolean isDeviceProvisioned();

        boolean isNotificationForCurrentProfiles(StatusBarNotification statusBarNotification);

        boolean shouldHideSensitiveContents(int i);
    }

    public static final class Entry {
        public boolean autoRedacted;
        public View expanded;
        public View expandedBig;
        public View expandedPublic;
        public StatusBarIconView icon;
        private boolean interruption;
        public String key;
        public boolean legacy;
        public StatusBarNotification notification;
        public ExpandableNotificationRow row;
        public int targetSdk;

        public Entry(StatusBarNotification n, StatusBarIconView ic) {
            this.key = n.getKey();
            this.notification = n;
            this.icon = ic;
        }

        public void setBigContentView(View bigContentView) {
            this.expandedBig = bigContentView;
            this.row.setExpandable(bigContentView != null);
        }

        public View getBigContentView() {
            return this.expandedBig;
        }

        public View getPublicContentView() {
            return this.expandedPublic;
        }

        public void setInterruption() {
            this.interruption = true;
        }

        public boolean hasInterrupted() {
            return this.interruption;
        }

        public void reset() {
            this.expanded = null;
            this.expandedPublic = null;
            this.expandedBig = null;
            this.autoRedacted = false;
            this.legacy = false;
            if (this.row != null) {
                this.row.reset();
            }
        }
    }

    public NotificationData(Environment environment) {
        this.mEnvironment = environment;
    }

    public ArrayList<Entry> getActiveNotifications() {
        return this.mSortedAndFiltered;
    }

    public Entry get(String key) {
        return this.mEntries.get(key);
    }

    public void add(Entry entry, NotificationListenerService.RankingMap ranking) {
        this.mEntries.put(entry.notification.getKey(), entry);
        updateRankingAndSort(ranking);
    }

    public Entry remove(String key, NotificationListenerService.RankingMap ranking) {
        Entry removed = this.mEntries.remove(key);
        if (removed == null) {
            return null;
        }
        updateRankingAndSort(ranking);
        return removed;
    }

    public void updateRanking(NotificationListenerService.RankingMap ranking) {
        updateRankingAndSort(ranking);
    }

    public boolean isAmbient(String key) {
        if (this.mRankingMap == null) {
            return false;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return this.mTmpRanking.isAmbient();
    }

    public int getVisibilityOverride(String key) {
        if (this.mRankingMap == null) {
            return -1000;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return this.mTmpRanking.getVisibilityOverride();
    }

    private void updateRankingAndSort(NotificationListenerService.RankingMap ranking) {
        if (ranking != null) {
            this.mRankingMap = ranking;
        }
        filterAndSort();
    }

    public void filterAndSort() {
        this.mSortedAndFiltered.clear();
        this.mGroupsWithSummaries.clear();
        int N = this.mEntries.size();
        for (int i = 0; i < N; i++) {
            Entry entry = this.mEntries.valueAt(i);
            StatusBarNotification sbn = entry.notification;
            if (!shouldFilterOut(sbn)) {
                if (sbn.getNotification().isGroupSummary()) {
                    this.mGroupsWithSummaries.add(sbn.getGroupKey());
                }
                this.mSortedAndFiltered.add(entry);
            }
        }
        if (!this.mGroupsWithSummaries.isEmpty()) {
            int M = this.mSortedAndFiltered.size();
            for (int i2 = M - 1; i2 >= 0; i2--) {
                Entry ent = this.mSortedAndFiltered.get(i2);
                StatusBarNotification sbn2 = ent.notification;
                if (sbn2.getNotification().isGroupChild() && this.mGroupsWithSummaries.contains(sbn2.getGroupKey())) {
                    this.mSortedAndFiltered.remove(i2);
                }
            }
        }
        Collections.sort(this.mSortedAndFiltered, this.mRankingComparator);
    }

    public boolean isGroupWithSummary(String groupKey) {
        return this.mGroupsWithSummaries.contains(groupKey);
    }

    boolean shouldFilterOut(StatusBarNotification sbn) {
        if ((this.mEnvironment.isDeviceProvisioned() || showNotificationEvenIfUnprovisioned(sbn)) && this.mEnvironment.isNotificationForCurrentProfiles(sbn)) {
            return sbn.getNotification().visibility == -1 && this.mEnvironment.shouldHideSensitiveContents(sbn.getUserId());
        }
        return true;
    }

    public boolean hasActiveClearableNotifications() {
        for (Entry e : this.mSortedAndFiltered) {
            if (e.expanded != null && e.notification.isClearable()) {
                return true;
            }
        }
        return false;
    }

    public static boolean showNotificationEvenIfUnprovisioned(StatusBarNotification sbn) {
        return "android".equals(sbn.getPackageName()) && sbn.getNotification().extras.getBoolean("android.allowDuringSetup");
    }

    public void dump(PrintWriter pw, String indent) {
        int N = this.mSortedAndFiltered.size();
        pw.print(indent);
        pw.println("active notifications: " + N);
        int active = 0;
        while (active < N) {
            Entry e = this.mSortedAndFiltered.get(active);
            dumpEntry(pw, indent, active, e);
            active++;
        }
        int M = this.mEntries.size();
        pw.print(indent);
        pw.println("inactive notifications: " + (M - active));
        int inactiveCount = 0;
        for (int i = 0; i < M; i++) {
            Entry entry = this.mEntries.valueAt(i);
            if (!this.mSortedAndFiltered.contains(entry)) {
                dumpEntry(pw, indent, inactiveCount, entry);
                inactiveCount++;
            }
        }
    }

    private void dumpEntry(PrintWriter pw, String indent, int i, Entry e) {
        pw.print(indent);
        pw.println("  [" + i + "] key=" + e.key + " icon=" + e.icon);
        StatusBarNotification n = e.notification;
        pw.print(indent);
        pw.println("      pkg=" + n.getPackageName() + " id=" + n.getId() + " score=" + n.getScore());
        pw.print(indent);
        pw.println("      notification=" + n.getNotification());
        pw.print(indent);
        pw.println("      tickerText=\"" + ((Object) n.getNotification().tickerText) + "\"");
    }

    public static boolean isSystemNotification(StatusBarNotification sbn) {
        String sbnPackage = sbn.getPackageName();
        return "android".equals(sbnPackage) || "com.android.systemui".equals(sbnPackage);
    }
}
