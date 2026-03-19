package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.EventList;
import android.app.usage.EventStats;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import android.util.ArrayMap;
import android.util.ArraySet;
import java.util.List;

class IntervalStats {
    public Configuration activeConfiguration;
    public long beginTime;
    public long endTime;
    public EventList events;
    public long lastTimeSaved;
    public final EventTracker interactiveTracker = new EventTracker();
    public final EventTracker nonInteractiveTracker = new EventTracker();
    public final EventTracker keyguardShownTracker = new EventTracker();
    public final EventTracker keyguardHiddenTracker = new EventTracker();
    public final ArrayMap<String, UsageStats> packageStats = new ArrayMap<>();
    public final ArrayMap<Configuration, ConfigurationStats> configurations = new ArrayMap<>();
    private final ArraySet<String> mStringCache = new ArraySet<>();

    IntervalStats() {
    }

    public static final class EventTracker {
        public int count;
        public long curStartTime;
        public long duration;
        public long lastEventTime;

        public void commitTime(long j) {
            if (this.curStartTime != 0) {
                this.duration += j - this.duration;
                this.curStartTime = 0L;
            }
        }

        public void update(long j) {
            if (this.curStartTime == 0) {
                this.count++;
            }
            commitTime(j);
            this.curStartTime = j;
            this.lastEventTime = j;
        }

        void addToEventStats(List<EventStats> list, int i, long j, long j2) {
            if (this.count != 0 || this.duration != 0) {
                EventStats eventStats = new EventStats();
                eventStats.mEventType = i;
                eventStats.mCount = this.count;
                eventStats.mTotalTime = this.duration;
                eventStats.mLastEventTime = this.lastEventTime;
                eventStats.mBeginTimeStamp = j;
                eventStats.mEndTimeStamp = j2;
                list.add(eventStats);
            }
        }
    }

    UsageStats getOrCreateUsageStats(String str) {
        UsageStats usageStats = this.packageStats.get(str);
        if (usageStats == null) {
            UsageStats usageStats2 = new UsageStats();
            usageStats2.mPackageName = getCachedStringRef(str);
            usageStats2.mBeginTimeStamp = this.beginTime;
            usageStats2.mEndTimeStamp = this.endTime;
            this.packageStats.put(usageStats2.mPackageName, usageStats2);
            return usageStats2;
        }
        return usageStats;
    }

    ConfigurationStats getOrCreateConfigurationStats(Configuration configuration) {
        ConfigurationStats configurationStats = this.configurations.get(configuration);
        if (configurationStats == null) {
            ConfigurationStats configurationStats2 = new ConfigurationStats();
            configurationStats2.mBeginTimeStamp = this.beginTime;
            configurationStats2.mEndTimeStamp = this.endTime;
            configurationStats2.mConfiguration = configuration;
            this.configurations.put(configuration, configurationStats2);
            return configurationStats2;
        }
        return configurationStats;
    }

    UsageEvents.Event buildEvent(String str, String str2) {
        UsageEvents.Event event = new UsageEvents.Event();
        event.mPackage = getCachedStringRef(str);
        if (str2 != null) {
            event.mClass = getCachedStringRef(str2);
        }
        return event;
    }

    private boolean isStatefulEvent(int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    private boolean isUserVisibleEvent(int i) {
        return (i == 6 || i == 11) ? false : true;
    }

    void update(String str, long j, int i) {
        UsageStats orCreateUsageStats = getOrCreateUsageStats(str);
        if ((i == 2 || i == 3) && (orCreateUsageStats.mLastEvent == 1 || orCreateUsageStats.mLastEvent == 4)) {
            orCreateUsageStats.mTotalTimeInForeground += j - orCreateUsageStats.mLastTimeUsed;
        }
        if (isStatefulEvent(i)) {
            orCreateUsageStats.mLastEvent = i;
        }
        if (isUserVisibleEvent(i)) {
            orCreateUsageStats.mLastTimeUsed = j;
        }
        orCreateUsageStats.mEndTimeStamp = j;
        if (i == 1) {
            orCreateUsageStats.mLaunchCount++;
        }
        this.endTime = j;
    }

    void updateChooserCounts(String str, String str2, String str3) {
        ArrayMap arrayMap;
        UsageStats orCreateUsageStats = getOrCreateUsageStats(str);
        if (orCreateUsageStats.mChooserCounts == null) {
            orCreateUsageStats.mChooserCounts = new ArrayMap();
        }
        int iIndexOfKey = orCreateUsageStats.mChooserCounts.indexOfKey(str3);
        if (iIndexOfKey < 0) {
            arrayMap = new ArrayMap();
            orCreateUsageStats.mChooserCounts.put(str3, arrayMap);
        } else {
            arrayMap = (ArrayMap) orCreateUsageStats.mChooserCounts.valueAt(iIndexOfKey);
        }
        arrayMap.put(str2, Integer.valueOf(((Integer) arrayMap.getOrDefault(str2, 0)).intValue() + 1));
    }

    void updateConfigurationStats(Configuration configuration, long j) {
        if (this.activeConfiguration != null) {
            ConfigurationStats configurationStats = this.configurations.get(this.activeConfiguration);
            configurationStats.mTotalTimeActive += j - configurationStats.mLastTimeActive;
            configurationStats.mLastTimeActive = j - 1;
        }
        if (configuration != null) {
            ConfigurationStats orCreateConfigurationStats = getOrCreateConfigurationStats(configuration);
            orCreateConfigurationStats.mLastTimeActive = j;
            orCreateConfigurationStats.mActivationCount++;
            this.activeConfiguration = orCreateConfigurationStats.mConfiguration;
        }
        this.endTime = j;
    }

    void incrementAppLaunchCount(String str) {
        getOrCreateUsageStats(str).mAppLaunchCount++;
    }

    void commitTime(long j) {
        this.interactiveTracker.commitTime(j);
        this.nonInteractiveTracker.commitTime(j);
        this.keyguardShownTracker.commitTime(j);
        this.keyguardHiddenTracker.commitTime(j);
    }

    void updateScreenInteractive(long j) {
        this.interactiveTracker.update(j);
        this.nonInteractiveTracker.commitTime(j);
    }

    void updateScreenNonInteractive(long j) {
        this.nonInteractiveTracker.update(j);
        this.interactiveTracker.commitTime(j);
    }

    void updateKeyguardShown(long j) {
        this.keyguardShownTracker.update(j);
        this.keyguardHiddenTracker.commitTime(j);
    }

    void updateKeyguardHidden(long j) {
        this.keyguardHiddenTracker.update(j);
        this.keyguardShownTracker.commitTime(j);
    }

    void addEventStatsTo(List<EventStats> list) {
        this.interactiveTracker.addToEventStats(list, 15, this.beginTime, this.endTime);
        this.nonInteractiveTracker.addToEventStats(list, 16, this.beginTime, this.endTime);
        this.keyguardShownTracker.addToEventStats(list, 17, this.beginTime, this.endTime);
        this.keyguardHiddenTracker.addToEventStats(list, 18, this.beginTime, this.endTime);
    }

    private String getCachedStringRef(String str) {
        int iIndexOf = this.mStringCache.indexOf(str);
        if (iIndexOf < 0) {
            this.mStringCache.add(str);
            return str;
        }
        return this.mStringCache.valueAt(iIndexOf);
    }
}
