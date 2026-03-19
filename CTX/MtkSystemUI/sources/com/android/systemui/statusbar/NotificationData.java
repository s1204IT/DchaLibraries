package com.android.systemui.statusbar;

import android.app.AppGlobals;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.Person;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.Dependency;
import com.android.systemui.ForegroundServiceController;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.InflationException;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class NotificationData {
    private final Environment mEnvironment;
    private NotificationGroupManager mGroupManager;
    private HeadsUpManager mHeadsUpManager;
    private NotificationListenerService.RankingMap mRankingMap;
    final ZenModeController mZen = (ZenModeController) Dependency.get(ZenModeController.class);
    final ForegroundServiceController mFsc = (ForegroundServiceController) Dependency.get(ForegroundServiceController.class);
    private final ArrayMap<String, Entry> mEntries = new ArrayMap<>();
    private final ArrayList<Entry> mSortedAndFiltered = new ArrayList<>();
    private final ArrayList<Entry> mFilteredForUser = new ArrayList<>();
    private final NotificationListenerService.Ranking mTmpRanking = new NotificationListenerService.Ranking();
    private final Comparator<Entry> mRankingComparator = new Comparator<Entry>() {
        private final NotificationListenerService.Ranking mRankingA = new NotificationListenerService.Ranking();
        private final NotificationListenerService.Ranking mRankingB = new NotificationListenerService.Ranking();

        @Override
        public int compare(Entry entry, Entry entry2) {
            int importance;
            int rank;
            int rank2;
            StatusBarNotification statusBarNotification = entry.notification;
            StatusBarNotification statusBarNotification2 = entry2.notification;
            int importance2 = 3;
            boolean z = false;
            if (NotificationData.this.mRankingMap != null) {
                NotificationData.this.getRanking(entry.key, this.mRankingA);
                NotificationData.this.getRanking(entry2.key, this.mRankingB);
                importance2 = this.mRankingA.getImportance();
                importance = this.mRankingB.getImportance();
                rank = this.mRankingA.getRank();
                rank2 = this.mRankingB.getRank();
            } else {
                importance = 3;
                rank = 0;
                rank2 = 0;
            }
            String currentMediaNotificationKey = NotificationData.this.mEnvironment.getCurrentMediaNotificationKey();
            boolean z2 = entry.key.equals(currentMediaNotificationKey) && importance2 > 1;
            boolean z3 = entry2.key.equals(currentMediaNotificationKey) && importance > 1;
            boolean z4 = importance2 >= 4 && NotificationData.isSystemNotification(statusBarNotification);
            if (importance >= 4 && NotificationData.isSystemNotification(statusBarNotification2)) {
                z = true;
            }
            boolean zIsHeadsUp = entry.row.isHeadsUp();
            if (zIsHeadsUp != entry2.row.isHeadsUp()) {
                return zIsHeadsUp ? -1 : 1;
            }
            if (zIsHeadsUp) {
                return NotificationData.this.mHeadsUpManager.compare(entry, entry2);
            }
            if (z2 != z3) {
                return z2 ? -1 : 1;
            }
            if (z4 != z) {
                return z4 ? -1 : 1;
            }
            if (rank != rank2) {
                return rank - rank2;
            }
            return Long.compare(statusBarNotification2.getNotification().when, statusBarNotification.getNotification().when);
        }
    };

    public interface Environment {
        String getCurrentMediaNotificationKey();

        NotificationGroupManager getGroupManager();

        boolean isDeviceProvisioned();

        boolean isDozing();

        boolean isNotificationForCurrentProfiles(StatusBarNotification statusBarNotification);

        boolean isSecurelyLocked(int i);

        boolean shouldHideNotifications(int i);

        boolean shouldHideNotifications(String str);
    }

    public static final class Entry {
        public boolean autoRedacted;
        public RemoteViews cachedAmbientContentView;
        public RemoteViews cachedBigContentView;
        public RemoteViews cachedContentView;
        public RemoteViews cachedHeadsUpContentView;
        public RemoteViews cachedPublicContentView;
        public NotificationChannel channel;
        public StatusBarIconView expandedIcon;
        private boolean hasSentReply;
        public CharSequence headsUpStatusBarText;
        public CharSequence headsUpStatusBarTextPublic;
        public StatusBarIconView icon;
        private boolean interruption;
        public String key;
        private Throwable mDebugThrowable;
        public Boolean mIsSystemNotification;
        public StatusBarNotification notification;
        public CharSequence remoteInputText;
        public CharSequence remoteInputTextWhenReset;
        public ExpandableNotificationRow row;
        public List<SnoozeCriterion> snoozeCriteria;
        public int targetSdk;
        private long lastFullScreenIntentLaunchTime = -2000;
        public int userSentiment = 0;
        private int mCachedContrastColor = 1;
        private int mCachedContrastColorIsFor = 1;
        private InflationTask mRunningTask = null;
        public long lastRemoteInputSent = -2000;
        public ArraySet<Integer> mActiveAppOps = new ArraySet<>(3);

        public Entry(StatusBarNotification statusBarNotification) {
            this.key = statusBarNotification.getKey();
            this.notification = statusBarNotification;
        }

        public void setInterruption() {
            this.interruption = true;
        }

        public boolean hasInterrupted() {
            return this.interruption;
        }

        public void reset() {
            if (this.row != null) {
                this.row.reset();
            }
        }

        public View getExpandedContentView() {
            return this.row.getPrivateLayout().getExpandedChild();
        }

        public void notifyFullScreenIntentLaunched() {
            setInterruption();
            this.lastFullScreenIntentLaunchTime = SystemClock.elapsedRealtime();
        }

        public boolean hasJustLaunchedFullScreenIntent() {
            return SystemClock.elapsedRealtime() < this.lastFullScreenIntentLaunchTime + 2000;
        }

        public boolean hasJustSentRemoteInput() {
            return SystemClock.elapsedRealtime() < this.lastRemoteInputSent + 500;
        }

        public void createIcons(Context context, StatusBarNotification statusBarNotification) throws InflationException {
            Notification notification = statusBarNotification.getNotification();
            Icon smallIcon = notification.getSmallIcon();
            if (smallIcon == null) {
                if (StatusBar.DEBUG) {
                    Log.d("NotificationData", "createIcons, smallIcon == null, sbn: " + statusBarNotification);
                }
                throw new InflationException("No small icon in notification from " + statusBarNotification.getPackageName());
            }
            this.icon = new StatusBarIconView(context, statusBarNotification.getPackageName() + "/0x" + Integer.toHexString(statusBarNotification.getId()), statusBarNotification);
            this.icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            this.expandedIcon = new StatusBarIconView(context, statusBarNotification.getPackageName() + "/0x" + Integer.toHexString(statusBarNotification.getId()), statusBarNotification);
            this.expandedIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            StatusBarIcon statusBarIcon = new StatusBarIcon(statusBarNotification.getUser(), statusBarNotification.getPackageName(), smallIcon, notification.iconLevel, notification.number, StatusBarIconView.contentDescForNotification(context, notification));
            if (!this.icon.set(statusBarIcon) || !this.expandedIcon.set(statusBarIcon)) {
                this.icon = null;
                this.expandedIcon = null;
                if (StatusBar.DEBUG) {
                    Log.d("NotificationData", "createIcons, set failed, sbn: " + statusBarNotification);
                }
                throw new InflationException("Couldn't create icon: " + statusBarIcon);
            }
            this.expandedIcon.setVisibility(4);
            this.expandedIcon.setOnVisibilityChangedListener(new StatusBarIconView.OnVisibilityChangedListener() {
                @Override
                public final void onVisibilityChanged(int i) {
                    NotificationData.Entry.lambda$createIcons$0(this.f$0, i);
                }
            });
        }

        public static void lambda$createIcons$0(Entry entry, int i) {
            if (entry.row != null) {
                entry.row.setIconsVisible(i != 0);
            }
        }

        public void setIconTag(int i, Object obj) {
            if (this.icon != null) {
                this.icon.setTag(i, obj);
                this.expandedIcon.setTag(i, obj);
            }
        }

        public void updateIcons(Context context, StatusBarNotification statusBarNotification) throws InflationException {
            if (this.icon != null) {
                Notification notification = statusBarNotification.getNotification();
                StatusBarIcon statusBarIcon = new StatusBarIcon(this.notification.getUser(), this.notification.getPackageName(), notification.getSmallIcon(), notification.iconLevel, notification.number, StatusBarIconView.contentDescForNotification(context, notification));
                this.icon.setNotification(statusBarNotification);
                this.expandedIcon.setNotification(statusBarNotification);
                if (!this.icon.set(statusBarIcon) || !this.expandedIcon.set(statusBarIcon)) {
                    if (StatusBar.DEBUG) {
                        Log.d("NotificationData", "updateIcons, set failed, sbn: " + statusBarNotification);
                    }
                    throw new InflationException("Couldn't update icon: " + statusBarIcon);
                }
            }
        }

        public int getContrastedColor(Context context, boolean z, int i) {
            int i2 = z ? 0 : this.notification.getNotification().color;
            if (this.mCachedContrastColorIsFor == i2 && this.mCachedContrastColor != 1) {
                return this.mCachedContrastColor;
            }
            int iResolveContrastColor = NotificationColorUtil.resolveContrastColor(context, i2, i);
            this.mCachedContrastColorIsFor = i2;
            this.mCachedContrastColor = iResolveContrastColor;
            return this.mCachedContrastColor;
        }

        public void abortTask() {
            if (this.mRunningTask != null) {
                this.mRunningTask.abort();
                this.mRunningTask = null;
            }
        }

        public void setInflationTask(InflationTask inflationTask) {
            InflationTask inflationTask2 = this.mRunningTask;
            abortTask();
            this.mRunningTask = inflationTask;
            if (inflationTask2 != null && this.mRunningTask != null) {
                this.mRunningTask.supersedeTask(inflationTask2);
            }
        }

        public void onInflationTaskFinished() {
            this.mRunningTask = null;
        }

        @VisibleForTesting
        public InflationTask getRunningTask() {
            return this.mRunningTask;
        }

        public void setDebugThrowable(Throwable th) {
            this.mDebugThrowable = th;
        }

        public Throwable getDebugThrowable() {
            return this.mDebugThrowable;
        }

        public void onRemoteInputInserted() {
            this.lastRemoteInputSent = -2000L;
            this.remoteInputTextWhenReset = null;
        }

        public void setHasSentReply() {
            this.hasSentReply = true;
        }

        public boolean isLastMessageFromReply() {
            Notification.MessagingStyle.Message messageFromBundle;
            if (!this.hasSentReply) {
                return false;
            }
            Bundle bundle = this.notification.getNotification().extras;
            if (!ArrayUtils.isEmpty(bundle.getCharSequenceArray("android.remoteInputHistory"))) {
                return true;
            }
            Parcelable[] parcelableArray = bundle.getParcelableArray("android.messages");
            if (parcelableArray != null && parcelableArray.length > 0) {
                Parcelable parcelable = parcelableArray[parcelableArray.length - 1];
                if ((parcelable instanceof Bundle) && (messageFromBundle = Notification.MessagingStyle.Message.getMessageFromBundle((Bundle) parcelable)) != null) {
                    Person senderPerson = messageFromBundle.getSenderPerson();
                    if (senderPerson == null) {
                        return true;
                    }
                    return Objects.equals((Person) bundle.getParcelable("android.messagingUser"), senderPerson);
                }
            }
            return false;
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

    public ArrayList<Entry> getNotificationsForCurrentUser() {
        this.mFilteredForUser.clear();
        synchronized (this.mEntries) {
            int size = this.mEntries.size();
            for (int i = 0; i < size; i++) {
                Entry entryValueAt = this.mEntries.valueAt(i);
                if (this.mEnvironment.isNotificationForCurrentProfiles(entryValueAt.notification)) {
                    this.mFilteredForUser.add(entryValueAt);
                }
            }
        }
        return this.mFilteredForUser;
    }

    public Entry get(String str) {
        return this.mEntries.get(str);
    }

    public void add(Entry entry) {
        synchronized (this.mEntries) {
            this.mEntries.put(entry.notification.getKey(), entry);
        }
        this.mGroupManager.onEntryAdded(entry);
        updateRankingAndSort(this.mRankingMap);
    }

    public Entry remove(String str, NotificationListenerService.RankingMap rankingMap) {
        Entry entryRemove;
        synchronized (this.mEntries) {
            entryRemove = this.mEntries.remove(str);
        }
        if (entryRemove == null) {
            return null;
        }
        this.mGroupManager.onEntryRemoved(entryRemove);
        updateRankingAndSort(rankingMap);
        return entryRemove;
    }

    public void updateRanking(NotificationListenerService.RankingMap rankingMap) {
        updateRankingAndSort(rankingMap);
    }

    public void updateAppOp(int i, int i2, String str, String str2, boolean z) {
        synchronized (this.mEntries) {
            int size = this.mEntries.size();
            for (int i3 = 0; i3 < size; i3++) {
                Entry entryValueAt = this.mEntries.valueAt(i3);
                if (i2 == entryValueAt.notification.getUid() && str.equals(entryValueAt.notification.getPackageName()) && str2.equals(entryValueAt.key)) {
                    if (z) {
                        entryValueAt.mActiveAppOps.add(Integer.valueOf(i));
                    } else {
                        entryValueAt.mActiveAppOps.remove(Integer.valueOf(i));
                    }
                }
            }
        }
    }

    public boolean isAmbient(String str) {
        if (this.mRankingMap != null) {
            getRanking(str, this.mTmpRanking);
            return this.mTmpRanking.isAmbient();
        }
        return false;
    }

    public int getVisibilityOverride(String str) {
        if (this.mRankingMap != null) {
            getRanking(str, this.mTmpRanking);
            return this.mTmpRanking.getVisibilityOverride();
        }
        return -1000;
    }

    public boolean shouldSuppressFullScreenIntent(Entry entry) {
        return shouldSuppressVisualEffect(entry, 4);
    }

    public boolean shouldSuppressPeek(Entry entry) {
        return shouldSuppressVisualEffect(entry, 16);
    }

    public boolean shouldSuppressStatusBar(Entry entry) {
        return shouldSuppressVisualEffect(entry, 32);
    }

    public boolean shouldSuppressAmbient(Entry entry) {
        return shouldSuppressVisualEffect(entry, 128);
    }

    public boolean shouldSuppressNotificationList(Entry entry) {
        return shouldSuppressVisualEffect(entry, 256);
    }

    private boolean shouldSuppressVisualEffect(Entry entry, int i) {
        if (isExemptFromDndVisualSuppression(entry)) {
            return false;
        }
        String str = entry.key;
        if (this.mRankingMap == null) {
            return false;
        }
        getRanking(str, this.mTmpRanking);
        return (this.mTmpRanking.getSuppressedVisualEffects() & i) != 0;
    }

    protected boolean isExemptFromDndVisualSuppression(Entry entry) {
        if (isNotificationBlockedByPolicy(entry.notification.getNotification())) {
            return false;
        }
        if ((entry.notification.getNotification().flags & 64) == 0 && !entry.notification.getNotification().isMediaNotification()) {
            return entry.mIsSystemNotification != null && entry.mIsSystemNotification.booleanValue();
        }
        return true;
    }

    protected boolean isNotificationBlockedByPolicy(Notification notification) {
        if (isCategory("call", notification) || isCategory("msg", notification) || isCategory("alarm", notification) || isCategory("event", notification) || isCategory("reminder", notification)) {
            return true;
        }
        return false;
    }

    private boolean isCategory(String str, Notification notification) {
        return Objects.equals(notification.category, str);
    }

    public int getImportance(String str) {
        if (this.mRankingMap != null) {
            getRanking(str, this.mTmpRanking);
            return this.mTmpRanking.getImportance();
        }
        return -1000;
    }

    public String getOverrideGroupKey(String str) {
        if (this.mRankingMap != null) {
            getRanking(str, this.mTmpRanking);
            return this.mTmpRanking.getOverrideGroupKey();
        }
        return null;
    }

    public List<SnoozeCriterion> getSnoozeCriteria(String str) {
        if (this.mRankingMap != null) {
            getRanking(str, this.mTmpRanking);
            return this.mTmpRanking.getSnoozeCriteria();
        }
        return null;
    }

    public NotificationChannel getChannel(String str) {
        if (this.mRankingMap != null) {
            getRanking(str, this.mTmpRanking);
            return this.mTmpRanking.getChannel();
        }
        return null;
    }

    public int getRank(String str) {
        if (this.mRankingMap != null) {
            getRanking(str, this.mTmpRanking);
            return this.mTmpRanking.getRank();
        }
        return 0;
    }

    public boolean shouldHide(String str) {
        if (this.mRankingMap != null) {
            getRanking(str, this.mTmpRanking);
            return this.mTmpRanking.isSuspended();
        }
        return false;
    }

    private void updateRankingAndSort(NotificationListenerService.RankingMap rankingMap) {
        if (rankingMap != null) {
            this.mRankingMap = rankingMap;
            synchronized (this.mEntries) {
                int size = this.mEntries.size();
                for (int i = 0; i < size; i++) {
                    Entry entryValueAt = this.mEntries.valueAt(i);
                    if (getRanking(entryValueAt.key, this.mTmpRanking)) {
                        StatusBarNotification statusBarNotificationCloneLight = entryValueAt.notification.cloneLight();
                        String overrideGroupKey = getOverrideGroupKey(entryValueAt.key);
                        if (!Objects.equals(statusBarNotificationCloneLight.getOverrideGroupKey(), overrideGroupKey)) {
                            entryValueAt.notification.setOverrideGroupKey(overrideGroupKey);
                            this.mGroupManager.onEntryUpdated(entryValueAt, statusBarNotificationCloneLight);
                        }
                        entryValueAt.channel = getChannel(entryValueAt.key);
                        entryValueAt.snoozeCriteria = getSnoozeCriteria(entryValueAt.key);
                        entryValueAt.userSentiment = this.mTmpRanking.getUserSentiment();
                    }
                }
            }
        }
        filterAndSort();
    }

    @VisibleForTesting
    protected boolean getRanking(String str, NotificationListenerService.Ranking ranking) {
        return this.mRankingMap.getRanking(str, ranking);
    }

    public void filterAndSort() {
        this.mSortedAndFiltered.clear();
        synchronized (this.mEntries) {
            int size = this.mEntries.size();
            for (int i = 0; i < size; i++) {
                Entry entryValueAt = this.mEntries.valueAt(i);
                if (!shouldFilterOut(entryValueAt)) {
                    this.mSortedAndFiltered.add(entryValueAt);
                }
            }
        }
        Collections.sort(this.mSortedAndFiltered, this.mRankingComparator);
    }

    public boolean shouldFilterOut(Entry entry) {
        String[] stringArray;
        StatusBarNotification statusBarNotification = entry.notification;
        if ((!this.mEnvironment.isDeviceProvisioned() && !showNotificationEvenIfUnprovisioned(statusBarNotification)) || !this.mEnvironment.isNotificationForCurrentProfiles(statusBarNotification)) {
            return true;
        }
        if (this.mEnvironment.isSecurelyLocked(statusBarNotification.getUserId()) && (statusBarNotification.getNotification().visibility == -1 || this.mEnvironment.shouldHideNotifications(statusBarNotification.getUserId()) || this.mEnvironment.shouldHideNotifications(statusBarNotification.getKey()))) {
            return true;
        }
        if (this.mEnvironment.isDozing() && shouldSuppressAmbient(entry)) {
            return true;
        }
        if ((!this.mEnvironment.isDozing() && shouldSuppressNotificationList(entry)) || shouldHide(statusBarNotification.getKey())) {
            return true;
        }
        if (!StatusBar.ENABLE_CHILD_NOTIFICATIONS && this.mGroupManager.isChildInGroupWithSummary(statusBarNotification)) {
            return true;
        }
        if (!this.mFsc.isDungeonNotification(statusBarNotification) || this.mFsc.isDungeonNeededForUser(statusBarNotification.getUserId())) {
            return this.mFsc.isSystemAlertNotification(statusBarNotification) && (stringArray = statusBarNotification.getNotification().extras.getStringArray("android.foregroundApps")) != null && stringArray.length >= 1 && !this.mFsc.isSystemAlertWarningNeeded(statusBarNotification.getUserId(), stringArray[0]);
        }
        return true;
    }

    public static boolean showNotificationEvenIfUnprovisioned(StatusBarNotification statusBarNotification) {
        return showNotificationEvenIfUnprovisioned(AppGlobals.getPackageManager(), statusBarNotification);
    }

    @VisibleForTesting
    static boolean showNotificationEvenIfUnprovisioned(IPackageManager iPackageManager, StatusBarNotification statusBarNotification) {
        return checkUidPermission(iPackageManager, "android.permission.NOTIFICATION_DURING_SETUP", statusBarNotification.getUid()) == 0 && statusBarNotification.getNotification().extras.getBoolean("android.allowDuringSetup");
    }

    private static int checkUidPermission(IPackageManager iPackageManager, String str, int i) {
        try {
            return iPackageManager.checkUidPermission(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        int size = this.mSortedAndFiltered.size();
        printWriter.print(str);
        printWriter.println("active notifications: " + size);
        int i = 0;
        while (i < size) {
            dumpEntry(printWriter, str, i, this.mSortedAndFiltered.get(i));
            i++;
        }
        synchronized (this.mEntries) {
            int size2 = this.mEntries.size();
            printWriter.print(str);
            printWriter.println("inactive notifications: " + (size2 - i));
            int i2 = 0;
            for (int i3 = 0; i3 < size2; i3++) {
                Entry entryValueAt = this.mEntries.valueAt(i3);
                if (!this.mSortedAndFiltered.contains(entryValueAt)) {
                    dumpEntry(printWriter, str, i2, entryValueAt);
                    i2++;
                }
            }
        }
    }

    private void dumpEntry(PrintWriter printWriter, String str, int i, Entry entry) {
        getRanking(entry.key, this.mTmpRanking);
        printWriter.print(str);
        printWriter.println("  [" + i + "] key=" + entry.key + " icon=" + entry.icon);
        StatusBarNotification statusBarNotification = entry.notification;
        printWriter.print(str);
        printWriter.println("      pkg=" + statusBarNotification.getPackageName() + " id=" + statusBarNotification.getId() + " importance=" + this.mTmpRanking.getImportance());
        printWriter.print(str);
        StringBuilder sb = new StringBuilder();
        sb.append("      notification=");
        sb.append(statusBarNotification.getNotification());
        printWriter.println(sb.toString());
    }

    private static boolean isSystemNotification(StatusBarNotification statusBarNotification) {
        String packageName = statusBarNotification.getPackageName();
        return "android".equals(packageName) || "com.android.systemui".equals(packageName);
    }
}
