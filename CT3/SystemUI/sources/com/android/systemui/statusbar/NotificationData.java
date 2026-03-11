package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.view.View;
import android.widget.RemoteViews;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class NotificationData {
    private final Environment mEnvironment;
    private NotificationGroupManager mGroupManager;
    private HeadsUpManager mHeadsUpManager;
    private NotificationListenerService.RankingMap mRankingMap;
    private final ArrayMap<String, Entry> mEntries = new ArrayMap<>();
    private final ArrayList<Entry> mSortedAndFiltered = new ArrayList<>();
    private final NotificationListenerService.Ranking mTmpRanking = new NotificationListenerService.Ranking();
    private final Comparator<Entry> mRankingComparator = new Comparator<Entry>() {
        private final NotificationListenerService.Ranking mRankingA = new NotificationListenerService.Ranking();
        private final NotificationListenerService.Ranking mRankingB = new NotificationListenerService.Ranking();

        @Override
        public int compare(Entry a, Entry b) {
            boolean zIsSystemNotification;
            boolean zIsSystemNotification2;
            StatusBarNotification na = a.notification;
            StatusBarNotification nb = b.notification;
            int aImportance = 3;
            int bImportance = 3;
            int aRank = 0;
            int bRank = 0;
            if (NotificationData.this.mRankingMap != null) {
                NotificationData.this.mRankingMap.getRanking(a.key, this.mRankingA);
                NotificationData.this.mRankingMap.getRanking(b.key, this.mRankingB);
                aImportance = this.mRankingA.getImportance();
                bImportance = this.mRankingB.getImportance();
                aRank = this.mRankingA.getRank();
                bRank = this.mRankingB.getRank();
            }
            String mediaNotification = NotificationData.this.mEnvironment.getCurrentMediaNotificationKey();
            boolean aMedia = a.key.equals(mediaNotification) && aImportance > 1;
            boolean bMedia = b.key.equals(mediaNotification) && bImportance > 1;
            if (aImportance < 5) {
                zIsSystemNotification = false;
            } else {
                zIsSystemNotification = NotificationData.isSystemNotification(na);
            }
            if (bImportance < 5) {
                zIsSystemNotification2 = false;
            } else {
                zIsSystemNotification2 = NotificationData.isSystemNotification(nb);
            }
            boolean isHeadsUp = a.row.isHeadsUp();
            if (isHeadsUp != b.row.isHeadsUp()) {
                return isHeadsUp ? -1 : 1;
            }
            if (isHeadsUp) {
                return NotificationData.this.mHeadsUpManager.compare(a, b);
            }
            if (aMedia != bMedia) {
                return aMedia ? -1 : 1;
            }
            if (zIsSystemNotification != zIsSystemNotification2) {
                return zIsSystemNotification ? -1 : 1;
            }
            if (aRank != bRank) {
                return aRank - bRank;
            }
            return (int) (nb.getNotification().when - na.getNotification().when);
        }
    };

    public interface Environment {
        String getCurrentMediaNotificationKey();

        NotificationGroupManager getGroupManager();

        boolean isDeviceProvisioned();

        boolean isNotificationForCurrentProfiles(StatusBarNotification statusBarNotification);

        boolean onSecureLockScreen();

        boolean shouldHideNotifications(int i);

        boolean shouldHideNotifications(String str);
    }

    public static final class Entry {
        public boolean autoRedacted;
        public RemoteViews cachedBigContentView;
        public RemoteViews cachedContentView;
        public RemoteViews cachedHeadsUpContentView;
        public RemoteViews cachedPublicContentView;
        public StatusBarIconView icon;
        private boolean interruption;
        public String key;
        private long lastFullScreenIntentLaunchTime = -2000;
        public boolean legacy;
        public StatusBarNotification notification;
        public CharSequence remoteInputText;
        public ExpandableNotificationRow row;
        public int targetSdk;

        public Entry(StatusBarNotification n, StatusBarIconView ic) {
            this.key = n.getKey();
            this.notification = n;
            this.icon = ic;
        }

        public void setInterruption() {
            this.interruption = true;
        }

        public boolean hasInterrupted() {
            return this.interruption;
        }

        public void reset() {
            this.autoRedacted = false;
            this.legacy = false;
            this.lastFullScreenIntentLaunchTime = -2000L;
            if (this.row == null) {
                return;
            }
            this.row.reset();
        }

        public View getContentView() {
            return this.row.getPrivateLayout().getContractedChild();
        }

        public View getExpandedContentView() {
            return this.row.getPrivateLayout().getExpandedChild();
        }

        public View getHeadsUpContentView() {
            return this.row.getPrivateLayout().getHeadsUpChild();
        }

        public View getPublicContentView() {
            return this.row.getPublicLayout().getContractedChild();
        }

        public boolean cacheContentViews(Context ctx, Notification updatedNotification) {
            if (updatedNotification == null) {
                Notification.Builder builder = Notification.Builder.recoverBuilder(ctx, this.notification.getNotification());
                this.cachedContentView = builder.createContentView();
                this.cachedBigContentView = builder.createBigContentView();
                this.cachedHeadsUpContentView = builder.createHeadsUpContentView();
                this.cachedPublicContentView = builder.makePublicContentView();
                return false;
            }
            Notification.Builder updatedNotificationBuilder = Notification.Builder.recoverBuilder(ctx, updatedNotification);
            RemoteViews newContentView = updatedNotificationBuilder.createContentView();
            RemoteViews newBigContentView = updatedNotificationBuilder.createBigContentView();
            RemoteViews newHeadsUpContentView = updatedNotificationBuilder.createHeadsUpContentView();
            RemoteViews newPublicNotification = updatedNotificationBuilder.makePublicContentView();
            boolean sameCustomView = Objects.equals(Boolean.valueOf(this.notification.getNotification().extras.getBoolean("android.contains.customView")), Boolean.valueOf(updatedNotification.extras.getBoolean("android.contains.customView")));
            boolean applyInPlace = (compareRemoteViews(this.cachedContentView, newContentView) && compareRemoteViews(this.cachedBigContentView, newBigContentView) && compareRemoteViews(this.cachedHeadsUpContentView, newHeadsUpContentView) && compareRemoteViews(this.cachedPublicContentView, newPublicNotification)) ? sameCustomView : false;
            this.cachedPublicContentView = newPublicNotification;
            this.cachedHeadsUpContentView = newHeadsUpContentView;
            this.cachedBigContentView = newBigContentView;
            this.cachedContentView = newContentView;
            return applyInPlace;
        }

        private boolean compareRemoteViews(RemoteViews a, RemoteViews b) {
            if (a == null && b == null) {
                return true;
            }
            if (a == null || b == null || b.getPackage() == null || a.getPackage() == null || !a.getPackage().equals(b.getPackage())) {
                return false;
            }
            return a.getLayoutId() == b.getLayoutId();
        }

        public void notifyFullScreenIntentLaunched() {
            this.lastFullScreenIntentLaunchTime = SystemClock.elapsedRealtime();
        }

        public boolean hasJustLaunchedFullScreenIntent() {
            return SystemClock.elapsedRealtime() < this.lastFullScreenIntentLaunchTime + 2000;
        }
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public NotificationData(Environment environment) {
        this.mEnvironment = environment;
        this.mGroupManager = environment.getGroupManager();
    }

    public ArrayList<Entry> getActiveNotifications() {
        return this.mSortedAndFiltered;
    }

    public Entry get(String key) {
        return this.mEntries.get(key);
    }

    public void add(Entry entry, NotificationListenerService.RankingMap ranking) {
        synchronized (this.mEntries) {
            this.mEntries.put(entry.notification.getKey(), entry);
        }
        this.mGroupManager.onEntryAdded(entry);
        updateRankingAndSort(ranking);
    }

    public Entry remove(String key, NotificationListenerService.RankingMap ranking) {
        Entry removed;
        synchronized (this.mEntries) {
            removed = this.mEntries.remove(key);
        }
        if (removed == null) {
            return null;
        }
        this.mGroupManager.onEntryRemoved(removed);
        updateRankingAndSort(ranking);
        return removed;
    }

    public void updateRanking(NotificationListenerService.RankingMap ranking) {
        updateRankingAndSort(ranking);
    }

    public boolean isAmbient(String key) {
        if (this.mRankingMap != null) {
            this.mRankingMap.getRanking(key, this.mTmpRanking);
            return this.mTmpRanking.isAmbient();
        }
        return false;
    }

    public int getVisibilityOverride(String key) {
        if (this.mRankingMap != null) {
            this.mRankingMap.getRanking(key, this.mTmpRanking);
            return this.mTmpRanking.getVisibilityOverride();
        }
        return -1000;
    }

    public boolean shouldSuppressScreenOff(String key) {
        if (this.mRankingMap == null) {
            return false;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return (this.mTmpRanking.getSuppressedVisualEffects() & 1) != 0;
    }

    public boolean shouldSuppressScreenOn(String key) {
        if (this.mRankingMap == null) {
            return false;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return (this.mTmpRanking.getSuppressedVisualEffects() & 2) != 0;
    }

    public int getImportance(String key) {
        if (this.mRankingMap != null) {
            this.mRankingMap.getRanking(key, this.mTmpRanking);
            return this.mTmpRanking.getImportance();
        }
        return -1000;
    }

    public String getOverrideGroupKey(String key) {
        if (this.mRankingMap == null) {
            return null;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return this.mTmpRanking.getOverrideGroupKey();
    }

    private void updateRankingAndSort(NotificationListenerService.RankingMap ranking) {
        if (ranking != null) {
            this.mRankingMap = ranking;
            synchronized (this.mEntries) {
                int N = this.mEntries.size();
                for (int i = 0; i < N; i++) {
                    Entry entry = this.mEntries.valueAt(i);
                    StatusBarNotification oldSbn = entry.notification.clone();
                    String overrideGroupKey = getOverrideGroupKey(entry.key);
                    if (!Objects.equals(oldSbn.getOverrideGroupKey(), overrideGroupKey)) {
                        entry.notification.setOverrideGroupKey(overrideGroupKey);
                        this.mGroupManager.onEntryUpdated(entry, oldSbn);
                    }
                }
            }
        }
        filterAndSort();
    }

    public void filterAndSort() {
        this.mSortedAndFiltered.clear();
        synchronized (this.mEntries) {
            int N = this.mEntries.size();
            for (int i = 0; i < N; i++) {
                Entry entry = this.mEntries.valueAt(i);
                StatusBarNotification sbn = entry.notification;
                if (!shouldFilterOut(sbn)) {
                    this.mSortedAndFiltered.add(entry);
                }
            }
        }
        Collections.sort(this.mSortedAndFiltered, this.mRankingComparator);
    }

    boolean shouldFilterOut(StatusBarNotification sbn) {
        if (!(!this.mEnvironment.isDeviceProvisioned() ? showNotificationEvenIfUnprovisioned(sbn) : true) || !this.mEnvironment.isNotificationForCurrentProfiles(sbn)) {
            return true;
        }
        if (this.mEnvironment.onSecureLockScreen() && (sbn.getNotification().visibility == -1 || this.mEnvironment.shouldHideNotifications(sbn.getUserId()) || this.mEnvironment.shouldHideNotifications(sbn.getKey()))) {
            return true;
        }
        return !BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS && this.mGroupManager.isChildInGroupWithSummary(sbn);
    }

    public boolean hasActiveClearableNotifications() {
        for (Entry e : this.mSortedAndFiltered) {
            if (e.getContentView() != null && e.notification.isClearable()) {
                return true;
            }
        }
        return false;
    }

    public static boolean showNotificationEvenIfUnprovisioned(StatusBarNotification sbn) {
        if ("android".equals(sbn.getPackageName())) {
            return sbn.getNotification().extras.getBoolean("android.allowDuringSetup");
        }
        return false;
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
        synchronized (this.mEntries) {
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
    }

    private void dumpEntry(PrintWriter pw, String indent, int i, Entry e) {
        this.mRankingMap.getRanking(e.key, this.mTmpRanking);
        pw.print(indent);
        pw.println("  [" + i + "] key=" + e.key + " icon=" + e.icon);
        StatusBarNotification n = e.notification;
        pw.print(indent);
        pw.println("      pkg=" + n.getPackageName() + " id=" + n.getId() + " importance=" + this.mTmpRanking.getImportance());
        pw.print(indent);
        pw.println("      notification=" + n.getNotification());
        pw.print(indent);
        pw.println("      tickerText=\"" + n.getNotification().tickerText + "\"");
    }

    public static boolean isSystemNotification(StatusBarNotification sbn) {
        String sbnPackage = sbn.getPackageName();
        if ("android".equals(sbnPackage)) {
            return true;
        }
        return "com.android.systemui".equals(sbnPackage);
    }
}
