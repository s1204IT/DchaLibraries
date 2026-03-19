package com.android.settingslib.applications;

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import java.io.IOException;

public class StorageStatsSource {
    private StorageStatsManager mStorageStatsManager;

    public interface AppStorageStats {
        long getTotalBytes();
    }

    public StorageStatsSource(Context context) {
        this.mStorageStatsManager = (StorageStatsManager) context.getSystemService(StorageStatsManager.class);
    }

    public AppStorageStats getStatsForUid(String str, int i) throws IOException {
        return new AppStorageStatsImpl(this.mStorageStatsManager.queryStatsForUid(str, i));
    }

    public static class AppStorageStatsImpl implements AppStorageStats {
        private StorageStats mStats;

        public AppStorageStatsImpl(StorageStats storageStats) {
            this.mStats = storageStats;
        }

        @Override
        public long getTotalBytes() {
            return this.mStats.getAppBytes() + this.mStats.getDataBytes();
        }
    }
}
