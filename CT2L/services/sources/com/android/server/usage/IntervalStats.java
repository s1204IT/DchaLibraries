package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.TimeSparseArray;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import android.util.ArrayMap;
import android.util.ArraySet;

class IntervalStats {
    public Configuration activeConfiguration;
    public long beginTime;
    public long endTime;
    public TimeSparseArray<UsageEvents.Event> events;
    public long lastTimeSaved;
    public final ArrayMap<String, UsageStats> packageStats = new ArrayMap<>();
    public final ArrayMap<Configuration, ConfigurationStats> configurations = new ArrayMap<>();
    private final ArraySet<String> mStringCache = new ArraySet<>();

    IntervalStats() {
    }

    UsageStats getOrCreateUsageStats(String packageName) {
        UsageStats usageStats = this.packageStats.get(packageName);
        if (usageStats == null) {
            UsageStats usageStats2 = new UsageStats();
            usageStats2.mPackageName = getCachedStringRef(packageName);
            usageStats2.mBeginTimeStamp = this.beginTime;
            usageStats2.mEndTimeStamp = this.endTime;
            this.packageStats.put(usageStats2.mPackageName, usageStats2);
            return usageStats2;
        }
        return usageStats;
    }

    ConfigurationStats getOrCreateConfigurationStats(Configuration config) {
        ConfigurationStats configStats = this.configurations.get(config);
        if (configStats == null) {
            ConfigurationStats configStats2 = new ConfigurationStats();
            configStats2.mBeginTimeStamp = this.beginTime;
            configStats2.mEndTimeStamp = this.endTime;
            configStats2.mConfiguration = config;
            this.configurations.put(config, configStats2);
            return configStats2;
        }
        return configStats;
    }

    UsageEvents.Event buildEvent(String packageName, String className) {
        UsageEvents.Event event = new UsageEvents.Event();
        event.mPackage = getCachedStringRef(packageName);
        if (className != null) {
            event.mClass = getCachedStringRef(className);
        }
        return event;
    }

    void update(String packageName, long timeStamp, int eventType) {
        UsageStats usageStats = getOrCreateUsageStats(packageName);
        if ((eventType == 2 || eventType == 3) && (usageStats.mLastEvent == 1 || usageStats.mLastEvent == 4)) {
            usageStats.mTotalTimeInForeground += timeStamp - usageStats.mLastTimeUsed;
        }
        usageStats.mLastEvent = eventType;
        usageStats.mLastTimeUsed = timeStamp;
        usageStats.mEndTimeStamp = timeStamp;
        if (eventType == 1) {
            usageStats.mLaunchCount++;
        }
        this.endTime = timeStamp;
    }

    void updateConfigurationStats(Configuration config, long timeStamp) {
        if (this.activeConfiguration != null) {
            ConfigurationStats activeStats = this.configurations.get(this.activeConfiguration);
            activeStats.mTotalTimeActive += timeStamp - activeStats.mLastTimeActive;
            activeStats.mLastTimeActive = timeStamp - 1;
        }
        if (config != null) {
            ConfigurationStats configStats = getOrCreateConfigurationStats(config);
            configStats.mLastTimeActive = timeStamp;
            configStats.mActivationCount++;
            this.activeConfiguration = configStats.mConfiguration;
        }
        this.endTime = timeStamp;
    }

    private String getCachedStringRef(String str) {
        int index = this.mStringCache.indexOf(str);
        if (index >= 0) {
            return this.mStringCache.valueAt(index);
        }
        this.mStringCache.add(str);
        return str;
    }
}
