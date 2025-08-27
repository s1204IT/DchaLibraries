package com.android.settings.applications;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.utils.StringUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class AppStateNotificationBridge extends AppStateBaseBridge {
    private NotificationBackend mBackend;
    private final Context mContext;
    private IUsageStatsManager mUsageStatsManager;
    protected List<Integer> mUserIds;
    public static final ApplicationsState.AppFilter FILTER_APP_NOTIFICATION_RECENCY = new ApplicationsState.AppFilter() { // from class: com.android.settings.applications.AppStateNotificationBridge.1
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(ApplicationsState.AppEntry appEntry) {
            NotificationsSentState notificationsSentState = AppStateNotificationBridge.getNotificationsSentState(appEntry);
            return (notificationsSentState == null || notificationsSentState.lastSent == 0) ? false : true;
        }
    };
    public static final ApplicationsState.AppFilter FILTER_APP_NOTIFICATION_FREQUENCY = new ApplicationsState.AppFilter() { // from class: com.android.settings.applications.AppStateNotificationBridge.2
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(ApplicationsState.AppEntry appEntry) {
            NotificationsSentState notificationsSentState = AppStateNotificationBridge.getNotificationsSentState(appEntry);
            return (notificationsSentState == null || notificationsSentState.sentCount == 0) ? false : true;
        }
    };
    public static final Comparator<ApplicationsState.AppEntry> RECENT_NOTIFICATION_COMPARATOR = new Comparator<ApplicationsState.AppEntry>() { // from class: com.android.settings.applications.AppStateNotificationBridge.3
        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(ApplicationsState.AppEntry appEntry, ApplicationsState.AppEntry appEntry2) {
            NotificationsSentState notificationsSentState = AppStateNotificationBridge.getNotificationsSentState(appEntry);
            NotificationsSentState notificationsSentState2 = AppStateNotificationBridge.getNotificationsSentState(appEntry2);
            if (notificationsSentState == null && notificationsSentState2 != null) {
                return -1;
            }
            if (notificationsSentState != null && notificationsSentState2 == null) {
                return 1;
            }
            if (notificationsSentState != null && notificationsSentState2 != null) {
                if (notificationsSentState.lastSent < notificationsSentState2.lastSent) {
                    return 1;
                }
                if (notificationsSentState.lastSent > notificationsSentState2.lastSent) {
                    return -1;
                }
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(appEntry, appEntry2);
        }
    };
    public static final Comparator<ApplicationsState.AppEntry> FREQUENCY_NOTIFICATION_COMPARATOR = new Comparator<ApplicationsState.AppEntry>() { // from class: com.android.settings.applications.AppStateNotificationBridge.4
        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(ApplicationsState.AppEntry appEntry, ApplicationsState.AppEntry appEntry2) {
            NotificationsSentState notificationsSentState = AppStateNotificationBridge.getNotificationsSentState(appEntry);
            NotificationsSentState notificationsSentState2 = AppStateNotificationBridge.getNotificationsSentState(appEntry2);
            if (notificationsSentState == null && notificationsSentState2 != null) {
                return -1;
            }
            if (notificationsSentState != null && notificationsSentState2 == null) {
                return 1;
            }
            if (notificationsSentState != null && notificationsSentState2 != null) {
                if (notificationsSentState.sentCount < notificationsSentState2.sentCount) {
                    return 1;
                }
                if (notificationsSentState.sentCount > notificationsSentState2.sentCount) {
                    return -1;
                }
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(appEntry, appEntry2);
        }
    };

    public static class NotificationsSentState {
        public boolean blockable;
        public boolean blocked;
        public boolean systemApp;
        public int avgSentDaily = 0;
        public int avgSentWeekly = 0;
        public long lastSent = 0;
        public int sentCount = 0;
    }

    public AppStateNotificationBridge(Context context, ApplicationsState applicationsState, AppStateBaseBridge.Callback callback, IUsageStatsManager iUsageStatsManager, UserManager userManager, NotificationBackend notificationBackend) {
        super(applicationsState, callback);
        this.mContext = context;
        this.mUsageStatsManager = iUsageStatsManager;
        this.mBackend = notificationBackend;
        this.mUserIds = new ArrayList();
        this.mUserIds.add(Integer.valueOf(this.mContext.getUserId()));
        int managedProfileId = Utils.getManagedProfileId(userManager, this.mContext.getUserId());
        if (managedProfileId != -10000) {
            this.mUserIds.add(Integer.valueOf(managedProfileId));
        }
    }

    @Override // com.android.settings.applications.AppStateBaseBridge
    protected void loadAllExtraInfo() {
        ArrayList<ApplicationsState.AppEntry> allApps = this.mAppSession.getAllApps();
        if (allApps == null) {
            return;
        }
        Map<String, NotificationsSentState> aggregatedUsageEvents = getAggregatedUsageEvents();
        Iterator<ApplicationsState.AppEntry> it = allApps.iterator();
        while (it.hasNext()) {
            ApplicationsState.AppEntry next = it.next();
            NotificationsSentState notificationsSentState = aggregatedUsageEvents.get(getKey(UserHandle.getUserId(next.info.uid), next.info.packageName));
            calculateAvgSentCounts(notificationsSentState);
            addBlockStatus(next, notificationsSentState);
            next.extraInfo = notificationsSentState;
        }
    }

    @Override // com.android.settings.applications.AppStateBaseBridge
    protected void updateExtraInfo(ApplicationsState.AppEntry appEntry, String str, int i) {
        NotificationsSentState aggregatedUsageEvents = getAggregatedUsageEvents(UserHandle.getUserId(appEntry.info.uid), appEntry.info.packageName);
        calculateAvgSentCounts(aggregatedUsageEvents);
        addBlockStatus(appEntry, aggregatedUsageEvents);
        appEntry.extraInfo = aggregatedUsageEvents;
    }

    public static CharSequence getSummary(Context context, NotificationsSentState notificationsSentState, boolean z) {
        if (z) {
            if (notificationsSentState.lastSent == 0) {
                return context.getString(R.string.notifications_sent_never);
            }
            return StringUtil.formatRelativeTime(context, System.currentTimeMillis() - notificationsSentState.lastSent, true);
        }
        if (notificationsSentState.avgSentWeekly > 0) {
            return context.getString(R.string.notifications_sent_weekly, Integer.valueOf(notificationsSentState.avgSentWeekly));
        }
        return context.getString(R.string.notifications_sent_daily, Integer.valueOf(notificationsSentState.avgSentDaily));
    }

    private void addBlockStatus(ApplicationsState.AppEntry appEntry, NotificationsSentState notificationsSentState) {
        if (notificationsSentState != null) {
            notificationsSentState.blocked = this.mBackend.getNotificationsBanned(appEntry.info.packageName, appEntry.info.uid);
            notificationsSentState.systemApp = this.mBackend.isSystemApp(this.mContext, appEntry.info);
            notificationsSentState.blockable = !notificationsSentState.systemApp || (notificationsSentState.systemApp && notificationsSentState.blocked);
        }
    }

    private void calculateAvgSentCounts(NotificationsSentState notificationsSentState) {
        if (notificationsSentState != null) {
            notificationsSentState.avgSentDaily = Math.round(notificationsSentState.sentCount / 7.0f);
            if (notificationsSentState.sentCount < 7) {
                notificationsSentState.avgSentWeekly = notificationsSentState.sentCount;
            }
        }
    }

    protected Map<String, NotificationsSentState> getAggregatedUsageEvents() {
        ArrayMap arrayMap = new ArrayMap();
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j = jCurrentTimeMillis - 604800000;
        Iterator<Integer> it = this.mUserIds.iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            UsageEvents usageEventsQueryEventsForUser = null;
            try {
                usageEventsQueryEventsForUser = this.mUsageStatsManager.queryEventsForUser(j, jCurrentTimeMillis, iIntValue, this.mContext.getPackageName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (usageEventsQueryEventsForUser != null) {
                UsageEvents.Event event = new UsageEvents.Event();
                while (usageEventsQueryEventsForUser.hasNextEvent()) {
                    usageEventsQueryEventsForUser.getNextEvent(event);
                    NotificationsSentState notificationsSentState = (NotificationsSentState) arrayMap.get(getKey(iIntValue, event.getPackageName()));
                    if (notificationsSentState == null) {
                        notificationsSentState = new NotificationsSentState();
                        arrayMap.put(getKey(iIntValue, event.getPackageName()), notificationsSentState);
                    }
                    if (event.getEventType() == 12) {
                        if (event.getTimeStamp() > notificationsSentState.lastSent) {
                            notificationsSentState.lastSent = event.getTimeStamp();
                        }
                        notificationsSentState.sentCount++;
                    }
                }
            }
        }
        return arrayMap;
    }

    protected NotificationsSentState getAggregatedUsageEvents(int i, String str) {
        UsageEvents usageEventsQueryEventsForPackageForUser;
        long jCurrentTimeMillis = System.currentTimeMillis();
        NotificationsSentState notificationsSentState = null;
        try {
            usageEventsQueryEventsForPackageForUser = this.mUsageStatsManager.queryEventsForPackageForUser(jCurrentTimeMillis - 604800000, jCurrentTimeMillis, i, str, this.mContext.getPackageName());
        } catch (RemoteException e) {
            e.printStackTrace();
            usageEventsQueryEventsForPackageForUser = null;
        }
        if (usageEventsQueryEventsForPackageForUser != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            while (usageEventsQueryEventsForPackageForUser.hasNextEvent()) {
                usageEventsQueryEventsForPackageForUser.getNextEvent(event);
                if (event.getEventType() == 12) {
                    if (notificationsSentState == null) {
                        notificationsSentState = new NotificationsSentState();
                    }
                    if (event.getTimeStamp() > notificationsSentState.lastSent) {
                        notificationsSentState.lastSent = event.getTimeStamp();
                    }
                    notificationsSentState.sentCount++;
                }
            }
        }
        return notificationsSentState;
    }

    private static NotificationsSentState getNotificationsSentState(ApplicationsState.AppEntry appEntry) {
        if (appEntry == null || appEntry.extraInfo == null || !(appEntry.extraInfo instanceof NotificationsSentState)) {
            return null;
        }
        return (NotificationsSentState) appEntry.extraInfo;
    }

    protected static String getKey(int i, String str) {
        return i + "|" + str;
    }

    public View.OnClickListener getSwitchOnClickListener(final ApplicationsState.AppEntry appEntry) {
        if (appEntry != null) {
            return new View.OnClickListener() { // from class: com.android.settings.applications.-$$Lambda$AppStateNotificationBridge$3yb6PrF82n91FG3YEHY_Ccl1JyI
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    AppStateNotificationBridge.lambda$getSwitchOnClickListener$0(this.f$0, appEntry, view);
                }
            };
        }
        return null;
    }

    public static /* synthetic */ void lambda$getSwitchOnClickListener$0(AppStateNotificationBridge appStateNotificationBridge, ApplicationsState.AppEntry appEntry, View view) {
        Switch r6 = (Switch) ((ViewGroup) view).findViewById(R.id.switchWidget);
        if (r6 == null || !r6.isEnabled()) {
            return;
        }
        r6.toggle();
        appStateNotificationBridge.mBackend.setNotificationsEnabledForPackage(appEntry.info.packageName, appEntry.info.uid, r6.isChecked());
        NotificationsSentState notificationsSentState = getNotificationsSentState(appEntry);
        if (notificationsSentState != null) {
            notificationsSentState.blocked = !r6.isChecked();
        }
    }

    public static final boolean enableSwitch(ApplicationsState.AppEntry appEntry) {
        NotificationsSentState notificationsSentState = getNotificationsSentState(appEntry);
        if (notificationsSentState == null) {
            return false;
        }
        return notificationsSentState.blockable;
    }

    public static final boolean checkSwitch(ApplicationsState.AppEntry appEntry) {
        if (getNotificationsSentState(appEntry) == null) {
            return false;
        }
        return !r0.blocked;
    }
}
