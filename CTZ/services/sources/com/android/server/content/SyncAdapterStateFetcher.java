package com.android.server.content;

import android.app.usage.UsageStatsManagerInternal;
import android.os.SystemClock;
import android.util.Pair;
import com.android.server.AppStateTracker;
import com.android.server.LocalServices;
import java.util.HashMap;

class SyncAdapterStateFetcher {
    private final HashMap<Pair<Integer, String>, Integer> mBucketCache = new HashMap<>();

    public int getStandbyBucket(int i, String str) {
        Pair<Integer, String> pairCreate = Pair.create(Integer.valueOf(i), str);
        Integer num = this.mBucketCache.get(pairCreate);
        if (num != null) {
            return num.intValue();
        }
        UsageStatsManagerInternal usageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        if (usageStatsManagerInternal == null) {
            return -1;
        }
        int appStandbyBucket = usageStatsManagerInternal.getAppStandbyBucket(str, i, SystemClock.elapsedRealtime());
        this.mBucketCache.put(pairCreate, Integer.valueOf(appStandbyBucket));
        return appStandbyBucket;
    }

    public boolean isAppActive(int i) {
        AppStateTracker appStateTracker = (AppStateTracker) LocalServices.getService(AppStateTracker.class);
        if (appStateTracker == null) {
            return false;
        }
        return appStateTracker.isUidActive(i);
    }
}
