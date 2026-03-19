package android.app.usage;

import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.app.backup.FullBackup;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.media.TtmlUtils;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class UsageStatsManager {

    @SystemApi
    public static final String EXTRA_OBSERVER_ID = "android.app.usage.extra.OBSERVER_ID";

    @SystemApi
    public static final String EXTRA_TIME_LIMIT = "android.app.usage.extra.TIME_LIMIT";

    @SystemApi
    public static final String EXTRA_TIME_USED = "android.app.usage.extra.TIME_USED";
    public static final int INTERVAL_BEST = 4;
    public static final int INTERVAL_COUNT = 4;
    public static final int INTERVAL_DAILY = 0;
    public static final int INTERVAL_MONTHLY = 2;
    public static final int INTERVAL_WEEKLY = 1;
    public static final int INTERVAL_YEARLY = 3;
    public static final int REASON_MAIN_DEFAULT = 256;
    public static final int REASON_MAIN_FORCED = 1024;
    public static final int REASON_MAIN_MASK = 65280;
    public static final int REASON_MAIN_PREDICTED = 1280;
    public static final int REASON_MAIN_TIMEOUT = 512;
    public static final int REASON_MAIN_USAGE = 768;
    public static final int REASON_SUB_MASK = 255;
    public static final int REASON_SUB_PREDICTED_RESTORED = 1;
    public static final int REASON_SUB_USAGE_ACTIVE_TIMEOUT = 7;
    public static final int REASON_SUB_USAGE_EXEMPTED_SYNC_SCHEDULED_DOZE = 12;
    public static final int REASON_SUB_USAGE_EXEMPTED_SYNC_SCHEDULED_NON_DOZE = 11;
    public static final int REASON_SUB_USAGE_EXEMPTED_SYNC_START = 13;
    public static final int REASON_SUB_USAGE_MOVE_TO_BACKGROUND = 5;
    public static final int REASON_SUB_USAGE_MOVE_TO_FOREGROUND = 4;
    public static final int REASON_SUB_USAGE_NOTIFICATION_SEEN = 2;
    public static final int REASON_SUB_USAGE_SLICE_PINNED = 9;
    public static final int REASON_SUB_USAGE_SLICE_PINNED_PRIV = 10;
    public static final int REASON_SUB_USAGE_SYNC_ADAPTER = 8;
    public static final int REASON_SUB_USAGE_SYSTEM_INTERACTION = 1;
    public static final int REASON_SUB_USAGE_SYSTEM_UPDATE = 6;
    public static final int REASON_SUB_USAGE_USER_INTERACTION = 3;
    public static final int STANDBY_BUCKET_ACTIVE = 10;

    @SystemApi
    public static final int STANDBY_BUCKET_EXEMPTED = 5;
    public static final int STANDBY_BUCKET_FREQUENT = 30;

    @SystemApi
    public static final int STANDBY_BUCKET_NEVER = 50;
    public static final int STANDBY_BUCKET_RARE = 40;
    public static final int STANDBY_BUCKET_WORKING_SET = 20;
    private static final UsageEvents sEmptyResults = new UsageEvents();
    private final Context mContext;
    private final IUsageStatsManager mService;

    @Retention(RetentionPolicy.SOURCE)
    public @interface StandbyBuckets {
    }

    public UsageStatsManager(Context context, IUsageStatsManager iUsageStatsManager) {
        this.mContext = context;
        this.mService = iUsageStatsManager;
    }

    public List<UsageStats> queryUsageStats(int i, long j, long j2) {
        try {
            ParceledListSlice parceledListSliceQueryUsageStats = this.mService.queryUsageStats(i, j, j2, this.mContext.getOpPackageName());
            if (parceledListSliceQueryUsageStats != null) {
                return parceledListSliceQueryUsageStats.getList();
            }
        } catch (RemoteException e) {
        }
        return Collections.emptyList();
    }

    public List<ConfigurationStats> queryConfigurations(int i, long j, long j2) {
        try {
            ParceledListSlice parceledListSliceQueryConfigurationStats = this.mService.queryConfigurationStats(i, j, j2, this.mContext.getOpPackageName());
            if (parceledListSliceQueryConfigurationStats != null) {
                return parceledListSliceQueryConfigurationStats.getList();
            }
        } catch (RemoteException e) {
        }
        return Collections.emptyList();
    }

    public List<EventStats> queryEventStats(int i, long j, long j2) {
        try {
            ParceledListSlice parceledListSliceQueryEventStats = this.mService.queryEventStats(i, j, j2, this.mContext.getOpPackageName());
            if (parceledListSliceQueryEventStats != null) {
                return parceledListSliceQueryEventStats.getList();
            }
        } catch (RemoteException e) {
        }
        return Collections.emptyList();
    }

    public UsageEvents queryEvents(long j, long j2) {
        try {
            UsageEvents usageEventsQueryEvents = this.mService.queryEvents(j, j2, this.mContext.getOpPackageName());
            if (usageEventsQueryEvents != null) {
                return usageEventsQueryEvents;
            }
        } catch (RemoteException e) {
        }
        return sEmptyResults;
    }

    public UsageEvents queryEventsForSelf(long j, long j2) {
        try {
            UsageEvents usageEventsQueryEventsForPackage = this.mService.queryEventsForPackage(j, j2, this.mContext.getOpPackageName());
            if (usageEventsQueryEventsForPackage != null) {
                return usageEventsQueryEventsForPackage;
            }
        } catch (RemoteException e) {
        }
        return sEmptyResults;
    }

    public Map<String, UsageStats> queryAndAggregateUsageStats(long j, long j2) {
        List<UsageStats> listQueryUsageStats = queryUsageStats(4, j, j2);
        if (listQueryUsageStats.isEmpty()) {
            return Collections.emptyMap();
        }
        ArrayMap arrayMap = new ArrayMap();
        int size = listQueryUsageStats.size();
        for (int i = 0; i < size; i++) {
            UsageStats usageStats = listQueryUsageStats.get(i);
            UsageStats usageStats2 = (UsageStats) arrayMap.get(usageStats.getPackageName());
            if (usageStats2 == null) {
                arrayMap.put(usageStats.mPackageName, usageStats);
            } else {
                usageStats2.add(usageStats);
            }
        }
        return arrayMap;
    }

    public boolean isAppInactive(String str) {
        try {
            return this.mService.isAppInactive(str, this.mContext.getUserId());
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setAppInactive(String str, boolean z) {
        try {
            this.mService.setAppInactive(str, z, this.mContext.getUserId());
        } catch (RemoteException e) {
        }
    }

    public int getAppStandbyBucket() {
        try {
            return this.mService.getAppStandbyBucket(this.mContext.getOpPackageName(), this.mContext.getOpPackageName(), this.mContext.getUserId());
        } catch (RemoteException e) {
            return 10;
        }
    }

    @SystemApi
    public int getAppStandbyBucket(String str) {
        try {
            return this.mService.getAppStandbyBucket(str, this.mContext.getOpPackageName(), this.mContext.getUserId());
        } catch (RemoteException e) {
            return 10;
        }
    }

    @SystemApi
    public void setAppStandbyBucket(String str, int i) {
        try {
            this.mService.setAppStandbyBucket(str, i, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public Map<String, Integer> getAppStandbyBuckets() {
        try {
            List list = this.mService.getAppStandbyBuckets(this.mContext.getOpPackageName(), this.mContext.getUserId()).getList();
            ArrayMap arrayMap = new ArrayMap();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AppStandbyInfo appStandbyInfo = (AppStandbyInfo) list.get(i);
                arrayMap.put(appStandbyInfo.mPackageName, Integer.valueOf(appStandbyInfo.mStandbyBucket));
            }
            return arrayMap;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setAppStandbyBuckets(Map<String, Integer> map) {
        if (map == null) {
            return;
        }
        ArrayList arrayList = new ArrayList(map.size());
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            arrayList.add(new AppStandbyInfo(entry.getKey(), entry.getValue().intValue()));
        }
        try {
            this.mService.setAppStandbyBuckets(new ParceledListSlice(arrayList), this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void registerAppUsageObserver(int i, String[] strArr, long j, TimeUnit timeUnit, PendingIntent pendingIntent) {
        try {
            this.mService.registerAppUsageObserver(i, strArr, timeUnit.toMillis(j), pendingIntent, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void unregisterAppUsageObserver(int i) {
        try {
            this.mService.unregisterAppUsageObserver(i, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static String reasonToString(int i) {
        StringBuilder sb = new StringBuilder();
        int i2 = 65280 & i;
        if (i2 == 256) {
            sb.append("d");
        } else if (i2 == 512) {
            sb.append("t");
        } else if (i2 == 768) {
            sb.append("u");
            switch (i & 255) {
                case 1:
                    sb.append("-si");
                    break;
                case 2:
                    sb.append("-ns");
                    break;
                case 3:
                    sb.append("-ui");
                    break;
                case 4:
                    sb.append("-mf");
                    break;
                case 5:
                    sb.append("-mb");
                    break;
                case 6:
                    sb.append("-su");
                    break;
                case 7:
                    sb.append("-at");
                    break;
                case 8:
                    sb.append("-sa");
                    break;
                case 9:
                    sb.append("-lp");
                    break;
                case 10:
                    sb.append("-lv");
                    break;
                case 11:
                    sb.append("-en");
                    break;
                case 12:
                    sb.append("-ed");
                    break;
                case 13:
                    sb.append("-es");
                    break;
            }
        } else if (i2 == 1024) {
            sb.append(FullBackup.FILES_TREE_TOKEN);
        } else if (i2 == 1280) {
            sb.append(TtmlUtils.TAG_P);
            if ((i & 255) == 1) {
                sb.append("-r");
            }
        }
        return sb.toString();
    }

    @SystemApi
    public void whitelistAppTemporarily(String str, long j, UserHandle userHandle) {
        try {
            this.mService.whitelistAppTemporarily(str, j, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void onCarrierPrivilegedAppsChanged() {
        try {
            this.mService.onCarrierPrivilegedAppsChanged();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reportChooserSelection(String str, int i, String str2, String[] strArr, String str3) {
        try {
            this.mService.reportChooserSelection(str, i, str2, strArr, str3);
        } catch (RemoteException e) {
        }
    }
}
