package com.android.storagemanager.deletionhelper;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import com.android.storagemanager.utils.AsyncLoader;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AppsAsyncLoader extends AsyncLoader<List<PackageInfo>> {
    protected Clock mClock;
    protected AppFilter mFilter;
    private PackageManagerWrapper mPackageManager;
    private StorageStatsSource mStatsManager;
    private UsageStatsManager mUsageStatsManager;
    private int mUserId;
    private String mUuid;
    public static final Comparator<PackageInfo> PACKAGE_INFO_COMPARATOR = new Comparator<PackageInfo>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(PackageInfo packageInfo, PackageInfo packageInfo2) {
            if (packageInfo.size < packageInfo2.size) {
                return 1;
            }
            if (packageInfo.size > packageInfo2.size) {
                return -1;
            }
            int iCompare = this.sCollator.compare(packageInfo.label, packageInfo2.label);
            if (iCompare != 0) {
                return iCompare;
            }
            int iCompare2 = this.sCollator.compare(packageInfo.packageName, packageInfo2.packageName);
            if (iCompare2 != 0) {
                return iCompare2;
            }
            return packageInfo.userId - packageInfo2.userId;
        }
    };
    public static final AppFilter FILTER_NO_THRESHOLD = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(PackageInfo packageInfo) {
            return (packageInfo == null || AppsAsyncLoader.isBundled(packageInfo) || AppsAsyncLoader.isPersistentProcess(packageInfo) || !AppsAsyncLoader.isExtraInfoValid(packageInfo, Long.MIN_VALUE)) ? false : true;
        }
    };
    public static final AppFilter FILTER_USAGE_STATS = new AppFilter() {
        private long mUnusedDaysThreshold;

        @Override
        public void init() {
            this.mUnusedDaysThreshold = SystemProperties.getLong("debug.asm.app_unused_limit", 90L);
        }

        @Override
        public boolean filterApp(PackageInfo packageInfo) {
            return (packageInfo == null || AppsAsyncLoader.isBundled(packageInfo) || AppsAsyncLoader.isPersistentProcess(packageInfo) || !AppsAsyncLoader.isExtraInfoValid(packageInfo, this.mUnusedDaysThreshold)) ? false : true;
        }
    };

    public interface AppFilter {
        boolean filterApp(PackageInfo packageInfo);

        void init();
    }

    private AppsAsyncLoader(Context context, int i, String str, StorageStatsSource storageStatsSource, PackageManagerWrapper packageManagerWrapper, UsageStatsManager usageStatsManager, AppFilter appFilter) {
        super(context);
        this.mUserId = i;
        this.mUuid = str;
        this.mStatsManager = storageStatsSource;
        this.mPackageManager = packageManagerWrapper;
        this.mUsageStatsManager = usageStatsManager;
        this.mClock = new Clock();
        this.mFilter = appFilter;
    }

    @Override
    public List<PackageInfo> loadInBackground() {
        return loadApps();
    }

    private List<PackageInfo> loadApps() {
        ArraySet arraySet = new ArraySet();
        long currentTime = this.mClock.getCurrentTime();
        long j = currentTime - 31449600000L;
        Map<String, UsageStats> mapQueryAndAggregateUsageStats = this.mUsageStatsManager.queryAndAggregateUsageStats(j, currentTime);
        Map<String, UsageStats> latestUsageStatsByPackageName = getLatestUsageStatsByPackageName(j, currentTime);
        List<ApplicationInfo> installedApplicationsAsUser = this.mPackageManager.getInstalledApplicationsAsUser(0, this.mUserId);
        ArrayList arrayList = new ArrayList();
        int size = installedApplicationsAsUser.size();
        this.mFilter.init();
        for (int i = 0; i < size; i++) {
            ApplicationInfo applicationInfo = installedApplicationsAsUser.get(i);
            if (!arraySet.contains(Integer.valueOf(applicationInfo.uid))) {
                try {
                    PackageInfo packageInfoBuild = new PackageInfo.Builder().setDaysSinceLastUse(getDaysSinceLastUse(getGreaterUsageStats(applicationInfo.packageName, mapQueryAndAggregateUsageStats.get(applicationInfo.packageName), latestUsageStatsByPackageName.get(applicationInfo.packageName)))).setDaysSinceFirstInstall(getDaysSinceInstalled(applicationInfo.packageName)).setUserId(UserHandle.getUserId(applicationInfo.uid)).setPackageName(applicationInfo.packageName).setSize(this.mStatsManager.getStatsForUid(applicationInfo.volumeUuid, applicationInfo.uid).getTotalBytes()).setFlags(applicationInfo.flags).setIcon(this.mPackageManager.getUserBadgedIcon(applicationInfo)).setLabel(this.mPackageManager.loadLabel(applicationInfo)).build();
                    arraySet.add(Integer.valueOf(applicationInfo.uid));
                    if (this.mFilter.filterApp(packageInfoBuild) && !isDefaultLauncher(this.mPackageManager, packageInfoBuild)) {
                        arrayList.add(packageInfoBuild);
                    }
                } catch (IOException e) {
                    Log.w("AppsAsyncLoader", e);
                }
            }
        }
        arrayList.sort(PACKAGE_INFO_COMPARATOR);
        return arrayList;
    }

    UsageStats getGreaterUsageStats(String str, UsageStats usageStats, UsageStats usageStats2) {
        long lastTimeUsed = usageStats != null ? usageStats.getLastTimeUsed() : 0L;
        long lastTimeUsed2 = usageStats2 != null ? usageStats2.getLastTimeUsed() : 0L;
        if (lastTimeUsed != lastTimeUsed2) {
            Log.w("AppsAsyncLoader", "Usage stats mismatch for " + str + " " + lastTimeUsed + " " + lastTimeUsed2);
        }
        return lastTimeUsed > lastTimeUsed2 ? usageStats : usageStats2;
    }

    private Map<String, UsageStats> getLatestUsageStatsByPackageName(long j, long j2) {
        Map map = (Map) this.mUsageStatsManager.queryUsageStats(3, j, j2).stream().collect(Collectors.groupingBy(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((UsageStats) obj).getPackageName();
            }
        }));
        final ArrayMap arrayMap = new ArrayMap();
        map.entrySet().stream().forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                Map.Entry entry = (Map.Entry) obj;
                arrayMap.put((String) entry.getKey(), (UsageStats) Collections.max((Collection) entry.getValue(), new Comparator() {
                    @Override
                    public final int compare(Object obj2, Object obj3) {
                        return Long.compare(((UsageStats) obj2).getLastTimeUsed(), ((UsageStats) obj3).getLastTimeUsed());
                    }
                }));
            }
        });
        return arrayMap;
    }

    @Override
    protected void onDiscardResult(List<PackageInfo> list) {
    }

    private static boolean isDefaultLauncher(PackageManagerWrapper packageManagerWrapper, PackageInfo packageInfo) {
        ComponentName homeActivities;
        if (packageManagerWrapper == null || (homeActivities = packageManagerWrapper.getHomeActivities(new ArrayList())) == null || homeActivities.getPackageName() == null) {
            return false;
        }
        return homeActivities.getPackageName().equals(packageInfo.packageName);
    }

    public static class Builder {
        private Context mContext;
        private AppFilter mFilter;
        private PackageManagerWrapper mPackageManager;
        private StorageStatsSource mStorageStatsSource;
        private int mUid;
        private UsageStatsManager mUsageStatsManager;
        private String mUuid;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setUid(int i) {
            this.mUid = i;
            return this;
        }

        public Builder setUuid(String str) {
            this.mUuid = str;
            return this;
        }

        public Builder setStorageStatsSource(StorageStatsSource storageStatsSource) {
            this.mStorageStatsSource = storageStatsSource;
            return this;
        }

        public Builder setPackageManager(PackageManagerWrapper packageManagerWrapper) {
            this.mPackageManager = packageManagerWrapper;
            return this;
        }

        public Builder setUsageStatsManager(UsageStatsManager usageStatsManager) {
            this.mUsageStatsManager = usageStatsManager;
            return this;
        }

        public Builder setFilter(AppFilter appFilter) {
            this.mFilter = appFilter;
            return this;
        }

        public AppsAsyncLoader build() {
            return new AppsAsyncLoader(this.mContext, this.mUid, this.mUuid, this.mStorageStatsSource, this.mPackageManager, this.mUsageStatsManager, this.mFilter);
        }
    }

    private static boolean isBundled(PackageInfo packageInfo) {
        return (packageInfo.flags & 1) != 0;
    }

    private static boolean isPersistentProcess(PackageInfo packageInfo) {
        return (packageInfo.flags & 8) != 0;
    }

    private static boolean isExtraInfoValid(Object obj, long j) {
        if (obj == null || !(obj instanceof PackageInfo)) {
            return false;
        }
        PackageInfo packageInfo = (PackageInfo) obj;
        if (packageInfo.daysSinceFirstInstall == -1 || packageInfo.daysSinceLastUse == -1) {
            Log.w("AppsAsyncLoader", "Missing information. Skipping app");
            return false;
        }
        long jMin = Math.min(packageInfo.daysSinceFirstInstall, packageInfo.daysSinceLastUse);
        if (jMin >= j) {
            Log.i("AppsAsyncLoader", "Accepting " + packageInfo.packageName + " with a minimum of " + jMin);
        }
        return jMin >= j;
    }

    private long getDaysSinceLastUse(UsageStats usageStats) {
        if (usageStats == null) {
            return Long.MAX_VALUE;
        }
        long lastTimeUsed = usageStats.getLastTimeUsed();
        if (lastTimeUsed <= 0) {
            return -1L;
        }
        long days = TimeUnit.MILLISECONDS.toDays(this.mClock.getCurrentTime() - lastTimeUsed);
        if (days > 365) {
            return Long.MAX_VALUE;
        }
        return days;
    }

    private long getDaysSinceInstalled(String str) {
        android.content.pm.PackageInfo packageInfo;
        try {
            packageInfo = this.mPackageManager.getPackageInfo(str, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AppsAsyncLoader", str + " was not found.");
            packageInfo = null;
        }
        if (packageInfo == null) {
            return -1L;
        }
        return TimeUnit.MILLISECONDS.toDays(this.mClock.getCurrentTime() - packageInfo.firstInstallTime);
    }

    public static class PackageInfo {
        public long daysSinceFirstInstall;
        public long daysSinceLastUse;
        public int flags;
        public Drawable icon;
        public CharSequence label;
        public String packageName;
        public long size;
        public int userId;

        private PackageInfo(long j, long j2, int i, String str, long j3, int i2, Drawable drawable, CharSequence charSequence) {
            this.daysSinceLastUse = j;
            this.daysSinceFirstInstall = j2;
            this.userId = i;
            this.packageName = str;
            this.size = j3;
            this.flags = i2;
            this.icon = drawable;
            this.label = charSequence;
        }

        public static class Builder {
            private long mDaysSinceFirstInstall;
            private long mDaysSinceLastUse;
            private int mFlags;
            private Drawable mIcon;
            private CharSequence mLabel;
            private String mPackageName;
            private long mSize;
            private int mUserId;

            public Builder setDaysSinceLastUse(long j) {
                this.mDaysSinceLastUse = j;
                return this;
            }

            public Builder setDaysSinceFirstInstall(long j) {
                this.mDaysSinceFirstInstall = j;
                return this;
            }

            public Builder setUserId(int i) {
                this.mUserId = i;
                return this;
            }

            public Builder setPackageName(String str) {
                this.mPackageName = str;
                return this;
            }

            public Builder setSize(long j) {
                this.mSize = j;
                return this;
            }

            public Builder setFlags(int i) {
                this.mFlags = i;
                return this;
            }

            public Builder setIcon(Drawable drawable) {
                this.mIcon = drawable;
                return this;
            }

            public Builder setLabel(CharSequence charSequence) {
                this.mLabel = charSequence;
                return this;
            }

            public PackageInfo build() {
                return new PackageInfo(this.mDaysSinceLastUse, this.mDaysSinceFirstInstall, this.mUserId, this.mPackageName, this.mSize, this.mFlags, this.mIcon, this.mLabel);
            }
        }
    }

    static class Clock {
        Clock() {
        }

        public long getCurrentTime() {
            return System.currentTimeMillis();
        }
    }
}
