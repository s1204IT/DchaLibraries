package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.EventList;
import android.app.usage.EventStats;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.am.AssistDataRequester;
import com.android.server.audio.AudioService;
import com.android.server.pm.Settings;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.usage.IntervalStats;
import com.android.server.usage.UsageStatsDatabase;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class UserUsageStatsService {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsageStatsService";
    private static final int sDateFormatFlags = 131093;
    private final Context mContext;
    private final UsageStatsDatabase mDatabase;
    private String mLastBackgroundedPackage;
    private final StatsUpdatedListener mListener;
    private final String mLogPrefix;
    private final int mUserId;
    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final long[] INTERVAL_LENGTH = {86400000, UnixCalendar.WEEK_IN_MILLIS, UnixCalendar.MONTH_IN_MILLIS, UnixCalendar.YEAR_IN_MILLIS};
    private static final UsageStatsDatabase.StatCombiner<UsageStats> sUsageStatsCombiner = new UsageStatsDatabase.StatCombiner<UsageStats>() {
        @Override
        public void combine(IntervalStats intervalStats, boolean z, List<UsageStats> list) {
            if (!z) {
                list.addAll(intervalStats.packageStats.values());
                return;
            }
            int size = intervalStats.packageStats.size();
            for (int i = 0; i < size; i++) {
                list.add(new UsageStats(intervalStats.packageStats.valueAt(i)));
            }
        }
    };
    private static final UsageStatsDatabase.StatCombiner<ConfigurationStats> sConfigStatsCombiner = new UsageStatsDatabase.StatCombiner<ConfigurationStats>() {
        @Override
        public void combine(IntervalStats intervalStats, boolean z, List<ConfigurationStats> list) {
            if (!z) {
                list.addAll(intervalStats.configurations.values());
                return;
            }
            int size = intervalStats.configurations.size();
            for (int i = 0; i < size; i++) {
                list.add(new ConfigurationStats(intervalStats.configurations.valueAt(i)));
            }
        }
    };
    private static final UsageStatsDatabase.StatCombiner<EventStats> sEventStatsCombiner = new UsageStatsDatabase.StatCombiner<EventStats>() {
        @Override
        public void combine(IntervalStats intervalStats, boolean z, List<EventStats> list) {
            intervalStats.addEventStatsTo(list);
        }
    };
    private boolean mStatsChanged = false;
    private final UnixCalendar mDailyExpiryDate = new UnixCalendar(0);
    private final IntervalStats[] mCurrentStats = new IntervalStats[4];

    interface StatsUpdatedListener {
        void onNewUpdate(int i);

        void onStatsReloaded();

        void onStatsUpdated();
    }

    UserUsageStatsService(Context context, int i, File file, StatsUpdatedListener statsUpdatedListener) {
        this.mContext = context;
        this.mDatabase = new UsageStatsDatabase(file);
        this.mListener = statsUpdatedListener;
        this.mLogPrefix = "User[" + Integer.toString(i) + "] ";
        this.mUserId = i;
    }

    void init(long j) {
        this.mDatabase.init(j);
        int i = 0;
        for (int i2 = 0; i2 < this.mCurrentStats.length; i2++) {
            this.mCurrentStats[i2] = this.mDatabase.getLatestUsageStats(i2);
            if (this.mCurrentStats[i2] == null) {
                i++;
            }
        }
        if (i > 0) {
            if (i != this.mCurrentStats.length) {
                Slog.w(TAG, this.mLogPrefix + "Some stats have no latest available");
            }
            loadActiveStats(j);
        } else {
            updateRolloverDeadline();
        }
        for (IntervalStats intervalStats : this.mCurrentStats) {
            int size = intervalStats.packageStats.size();
            for (int i3 = 0; i3 < size; i3++) {
                UsageStats usageStatsValueAt = intervalStats.packageStats.valueAt(i3);
                if (usageStatsValueAt.mLastEvent == 1 || usageStatsValueAt.mLastEvent == 4) {
                    intervalStats.update(usageStatsValueAt.mPackageName, intervalStats.lastTimeSaved, 3);
                    notifyStatsChanged();
                }
            }
            intervalStats.updateConfigurationStats(null, intervalStats.lastTimeSaved);
        }
        if (this.mDatabase.isNewUpdate()) {
            notifyNewUpdate();
        }
    }

    void onTimeChanged(long j, long j2) {
        persistActiveStats();
        this.mDatabase.onTimeChanged(j2 - j);
        loadActiveStats(j2);
    }

    void reportEvent(UsageEvents.Event event) {
        if (event.mTimeStamp >= this.mDailyExpiryDate.getTimeInMillis()) {
            rolloverStats(event.mTimeStamp);
        }
        IntervalStats intervalStats = this.mCurrentStats[0];
        Configuration configuration = event.mConfiguration;
        if (event.mEventType == 5 && intervalStats.activeConfiguration != null) {
            event.mConfiguration = Configuration.generateDelta(intervalStats.activeConfiguration, configuration);
        }
        if (intervalStats.events == null) {
            intervalStats.events = new EventList();
        }
        if (event.mEventType != 6) {
            intervalStats.events.insert(event);
        }
        boolean z = true;
        if (event.mEventType == 1) {
            if (event.mPackage == null || event.mPackage.equals(this.mLastBackgroundedPackage)) {
            }
            for (IntervalStats intervalStats2 : this.mCurrentStats) {
                int i = event.mEventType;
                if (i == 5) {
                    intervalStats2.updateConfigurationStats(configuration, event.mTimeStamp);
                } else if (i == 9) {
                    intervalStats2.updateChooserCounts(event.mPackage, event.mContentType, event.mAction);
                    String[] strArr = event.mContentAnnotations;
                    if (strArr != null) {
                        for (String str : strArr) {
                            intervalStats2.updateChooserCounts(event.mPackage, str, event.mAction);
                        }
                    }
                } else {
                    switch (i) {
                        case 15:
                            intervalStats2.updateScreenInteractive(event.mTimeStamp);
                            break;
                        case 16:
                            intervalStats2.updateScreenNonInteractive(event.mTimeStamp);
                            break;
                        case 17:
                            intervalStats2.updateKeyguardShown(event.mTimeStamp);
                            break;
                        case 18:
                            intervalStats2.updateKeyguardHidden(event.mTimeStamp);
                            break;
                        default:
                            intervalStats2.update(event.mPackage, event.mTimeStamp, event.mEventType);
                            if (z) {
                                intervalStats2.incrementAppLaunchCount(event.mPackage);
                            }
                            break;
                    }
                }
            }
            notifyStatsChanged();
        }
        if (event.mEventType == 2 && event.mPackage != null) {
            this.mLastBackgroundedPackage = event.mPackage;
        }
        z = false;
        while (i < r5) {
        }
        notifyStatsChanged();
    }

    private <T> List<T> queryStats(int i, long j, long j2, UsageStatsDatabase.StatCombiner<T> statCombiner) {
        if (i == 4 && (i = this.mDatabase.findBestFitBucket(j, j2)) < 0) {
            i = 0;
        }
        int i2 = i;
        if (i2 < 0 || i2 >= this.mCurrentStats.length) {
            return null;
        }
        IntervalStats intervalStats = this.mCurrentStats[i2];
        if (j >= intervalStats.endTime) {
            return null;
        }
        List<T> listQueryUsageStats = this.mDatabase.queryUsageStats(i2, j, Math.min(intervalStats.beginTime, j2), statCombiner);
        if (j < intervalStats.endTime && j2 > intervalStats.beginTime) {
            if (listQueryUsageStats == null) {
                listQueryUsageStats = new ArrayList<>();
            }
            statCombiner.combine(intervalStats, true, listQueryUsageStats);
        }
        return listQueryUsageStats;
    }

    List<UsageStats> queryUsageStats(int i, long j, long j2) {
        return queryStats(i, j, j2, sUsageStatsCombiner);
    }

    List<ConfigurationStats> queryConfigurationStats(int i, long j, long j2) {
        return queryStats(i, j, j2, sConfigStatsCombiner);
    }

    List<EventStats> queryEventStats(int i, long j, long j2) {
        return queryStats(i, j, j2, sEventStatsCombiner);
    }

    UsageEvents queryEvents(final long j, final long j2, final boolean z) {
        final ArraySet arraySet = new ArraySet();
        List listQueryStats = queryStats(0, j, j2, new UsageStatsDatabase.StatCombiner<UsageEvents.Event>() {
            @Override
            public void combine(IntervalStats intervalStats, boolean z2, List<UsageEvents.Event> list) {
                if (intervalStats.events == null) {
                    return;
                }
                int size = intervalStats.events.size();
                for (int iFirstIndexOnOrAfter = intervalStats.events.firstIndexOnOrAfter(j); iFirstIndexOnOrAfter < size && intervalStats.events.get(iFirstIndexOnOrAfter).mTimeStamp < j2; iFirstIndexOnOrAfter++) {
                    UsageEvents.Event obfuscatedIfInstantApp = intervalStats.events.get(iFirstIndexOnOrAfter);
                    if (z) {
                        obfuscatedIfInstantApp = obfuscatedIfInstantApp.getObfuscatedIfInstantApp();
                    }
                    arraySet.add(obfuscatedIfInstantApp.mPackage);
                    if (obfuscatedIfInstantApp.mClass != null) {
                        arraySet.add(obfuscatedIfInstantApp.mClass);
                    }
                    list.add(obfuscatedIfInstantApp);
                }
            }
        });
        if (listQueryStats == null || listQueryStats.isEmpty()) {
            return null;
        }
        String[] strArr = (String[]) arraySet.toArray(new String[arraySet.size()]);
        Arrays.sort(strArr);
        return new UsageEvents(listQueryStats, strArr);
    }

    UsageEvents queryEventsForPackage(final long j, final long j2, final String str) {
        final ArraySet arraySet = new ArraySet();
        arraySet.add(str);
        List listQueryStats = queryStats(0, j, j2, new UsageStatsDatabase.StatCombiner() {
            @Override
            public final void combine(IntervalStats intervalStats, boolean z, List list) {
                UserUsageStatsService.lambda$queryEventsForPackage$0(j, j2, str, arraySet, intervalStats, z, list);
            }
        });
        if (listQueryStats == null || listQueryStats.isEmpty()) {
            return null;
        }
        String[] strArr = (String[]) arraySet.toArray(new String[arraySet.size()]);
        Arrays.sort(strArr);
        return new UsageEvents(listQueryStats, strArr);
    }

    static void lambda$queryEventsForPackage$0(long j, long j2, String str, ArraySet arraySet, IntervalStats intervalStats, boolean z, List list) {
        if (intervalStats.events == null) {
            return;
        }
        int size = intervalStats.events.size();
        for (int iFirstIndexOnOrAfter = intervalStats.events.firstIndexOnOrAfter(j); iFirstIndexOnOrAfter < size && intervalStats.events.get(iFirstIndexOnOrAfter).mTimeStamp < j2; iFirstIndexOnOrAfter++) {
            UsageEvents.Event event = intervalStats.events.get(iFirstIndexOnOrAfter);
            if (str.equals(event.mPackage)) {
                if (event.mClass != null) {
                    arraySet.add(event.mClass);
                }
                list.add(event);
            }
        }
    }

    void persistActiveStats() {
        if (this.mStatsChanged) {
            Slog.i(TAG, this.mLogPrefix + "Flushing usage stats to disk");
            for (int i = 0; i < this.mCurrentStats.length; i++) {
                try {
                    this.mDatabase.putUsageStats(i, this.mCurrentStats[i]);
                } catch (IOException e) {
                    Slog.e(TAG, this.mLogPrefix + "Failed to persist active stats", e);
                    return;
                }
            }
            this.mStatsChanged = false;
        }
    }

    private void rolloverStats(long j) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        Slog.i(TAG, this.mLogPrefix + "Rolling over usage stats");
        int i = 0;
        Configuration configuration = this.mCurrentStats[0].activeConfiguration;
        ArraySet arraySet = new ArraySet();
        IntervalStats[] intervalStatsArr = this.mCurrentStats;
        int length = intervalStatsArr.length;
        int i2 = 0;
        while (i2 < length) {
            IntervalStats intervalStats = intervalStatsArr[i2];
            int size = intervalStats.packageStats.size();
            for (int i3 = i; i3 < size; i3++) {
                UsageStats usageStatsValueAt = intervalStats.packageStats.valueAt(i3);
                if (usageStatsValueAt.mLastEvent == 1 || usageStatsValueAt.mLastEvent == 4) {
                    arraySet.add(usageStatsValueAt.mPackageName);
                    intervalStats.update(usageStatsValueAt.mPackageName, this.mDailyExpiryDate.getTimeInMillis() - 1, 3);
                    notifyStatsChanged();
                }
            }
            intervalStats.updateConfigurationStats(null, this.mDailyExpiryDate.getTimeInMillis() - 1);
            intervalStats.commitTime(this.mDailyExpiryDate.getTimeInMillis() - 1);
            i2++;
            i = 0;
        }
        persistActiveStats();
        this.mDatabase.prune(j);
        loadActiveStats(j);
        int size2 = arraySet.size();
        for (int i4 = 0; i4 < size2; i4++) {
            String str = (String) arraySet.valueAt(i4);
            long j2 = this.mCurrentStats[0].beginTime;
            for (IntervalStats intervalStats2 : this.mCurrentStats) {
                intervalStats2.update(str, j2, 4);
                intervalStats2.updateConfigurationStats(configuration, j2);
                notifyStatsChanged();
            }
        }
        persistActiveStats();
        Slog.i(TAG, this.mLogPrefix + "Rolling over usage stats complete. Took " + (SystemClock.elapsedRealtime() - jElapsedRealtime) + " milliseconds");
    }

    private void notifyStatsChanged() {
        if (!this.mStatsChanged) {
            this.mStatsChanged = true;
            this.mListener.onStatsUpdated();
        }
    }

    private void notifyNewUpdate() {
        this.mListener.onNewUpdate(this.mUserId);
    }

    private void loadActiveStats(long j) {
        for (int i = 0; i < this.mCurrentStats.length; i++) {
            IntervalStats latestUsageStats = this.mDatabase.getLatestUsageStats(i);
            if (latestUsageStats != null && j - 500 >= latestUsageStats.endTime && j < latestUsageStats.beginTime + INTERVAL_LENGTH[i]) {
                this.mCurrentStats[i] = latestUsageStats;
            } else {
                this.mCurrentStats[i] = new IntervalStats();
                this.mCurrentStats[i].beginTime = j;
                this.mCurrentStats[i].endTime = 1 + j;
            }
        }
        this.mStatsChanged = false;
        updateRolloverDeadline();
        this.mListener.onStatsReloaded();
    }

    private void updateRolloverDeadline() {
        this.mDailyExpiryDate.setTimeInMillis(this.mCurrentStats[0].beginTime);
        this.mDailyExpiryDate.addDays(1);
        Slog.i(TAG, this.mLogPrefix + "Rollover scheduled @ " + sDateFormat.format(Long.valueOf(this.mDailyExpiryDate.getTimeInMillis())) + "(" + this.mDailyExpiryDate.getTimeInMillis() + ")");
    }

    void checkin(final IndentingPrintWriter indentingPrintWriter) {
        this.mDatabase.checkinDailyFiles(new UsageStatsDatabase.CheckinAction() {
            @Override
            public boolean checkin(IntervalStats intervalStats) {
                UserUsageStatsService.this.printIntervalStats(indentingPrintWriter, intervalStats, false, false, null);
                return true;
            }
        });
    }

    void dump(IndentingPrintWriter indentingPrintWriter, String str) {
        dump(indentingPrintWriter, str, false);
    }

    void dump(IndentingPrintWriter indentingPrintWriter, String str, boolean z) {
        printLast24HrEvents(indentingPrintWriter, !z, str);
        for (int i = 0; i < this.mCurrentStats.length; i++) {
            indentingPrintWriter.print("In-memory ");
            indentingPrintWriter.print(intervalToString(i));
            indentingPrintWriter.println(" stats");
            printIntervalStats(indentingPrintWriter, this.mCurrentStats[i], !z, true, str);
        }
    }

    private String formatDateTime(long j, boolean z) {
        if (z) {
            return "\"" + sDateFormat.format(Long.valueOf(j)) + "\"";
        }
        return Long.toString(j);
    }

    private String formatElapsedTime(long j, boolean z) {
        if (z) {
            return "\"" + DateUtils.formatElapsedTime(j / 1000) + "\"";
        }
        return Long.toString(j);
    }

    void printEvent(IndentingPrintWriter indentingPrintWriter, UsageEvents.Event event, boolean z) {
        indentingPrintWriter.printPair("time", formatDateTime(event.mTimeStamp, z));
        indentingPrintWriter.printPair(DatabaseHelper.SoundModelContract.KEY_TYPE, eventToString(event.mEventType));
        indentingPrintWriter.printPair(Settings.ATTR_PACKAGE, event.mPackage);
        if (event.mClass != null) {
            indentingPrintWriter.printPair(AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS, event.mClass);
        }
        if (event.mConfiguration != null) {
            indentingPrintWriter.printPair("config", Configuration.resourceQualifierString(event.mConfiguration));
        }
        if (event.mShortcutId != null) {
            indentingPrintWriter.printPair("shortcutId", event.mShortcutId);
        }
        if (event.mEventType == 11) {
            indentingPrintWriter.printPair("standbyBucket", Integer.valueOf(event.getStandbyBucket()));
            indentingPrintWriter.printPair(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, UsageStatsManager.reasonToString(event.getStandbyReason()));
        }
        indentingPrintWriter.printHexPair("flags", event.mFlags);
        indentingPrintWriter.println();
    }

    void printLast24HrEvents(IndentingPrintWriter indentingPrintWriter, boolean z, final String str) {
        final long jCurrentTimeMillis = System.currentTimeMillis();
        UnixCalendar unixCalendar = new UnixCalendar(jCurrentTimeMillis);
        unixCalendar.addDays(-1);
        final long timeInMillis = unixCalendar.getTimeInMillis();
        List listQueryStats = queryStats(0, timeInMillis, jCurrentTimeMillis, new UsageStatsDatabase.StatCombiner<UsageEvents.Event>() {
            @Override
            public void combine(IntervalStats intervalStats, boolean z2, List<UsageEvents.Event> list) {
                if (intervalStats.events == null) {
                    return;
                }
                int size = intervalStats.events.size();
                for (int iFirstIndexOnOrAfter = intervalStats.events.firstIndexOnOrAfter(timeInMillis); iFirstIndexOnOrAfter < size && intervalStats.events.get(iFirstIndexOnOrAfter).mTimeStamp < jCurrentTimeMillis; iFirstIndexOnOrAfter++) {
                    UsageEvents.Event event = intervalStats.events.get(iFirstIndexOnOrAfter);
                    if (str == null || str.equals(event.mPackage)) {
                        list.add(event);
                    }
                }
            }
        });
        indentingPrintWriter.print("Last 24 hour events (");
        if (z) {
            indentingPrintWriter.printPair("timeRange", "\"" + DateUtils.formatDateRange(this.mContext, timeInMillis, jCurrentTimeMillis, sDateFormatFlags) + "\"");
        } else {
            indentingPrintWriter.printPair("beginTime", Long.valueOf(timeInMillis));
            indentingPrintWriter.printPair("endTime", Long.valueOf(jCurrentTimeMillis));
        }
        indentingPrintWriter.println(")");
        if (listQueryStats != null) {
            indentingPrintWriter.increaseIndent();
            Iterator it = listQueryStats.iterator();
            while (it.hasNext()) {
                printEvent(indentingPrintWriter, (UsageEvents.Event) it.next(), z);
            }
            indentingPrintWriter.decreaseIndent();
        }
    }

    void printEventAggregation(IndentingPrintWriter indentingPrintWriter, String str, IntervalStats.EventTracker eventTracker, boolean z) {
        if (eventTracker.count != 0 || eventTracker.duration != 0) {
            indentingPrintWriter.print(str);
            indentingPrintWriter.print(": ");
            indentingPrintWriter.print(eventTracker.count);
            indentingPrintWriter.print("x for ");
            indentingPrintWriter.print(formatElapsedTime(eventTracker.duration, z));
            if (eventTracker.curStartTime != 0) {
                indentingPrintWriter.print(" (now running, started at ");
                formatDateTime(eventTracker.curStartTime, z);
                indentingPrintWriter.print(")");
            }
            indentingPrintWriter.println();
        }
    }

    void printIntervalStats(IndentingPrintWriter indentingPrintWriter, IntervalStats intervalStats, boolean z, boolean z2, String str) {
        Iterator<UsageStats> it;
        UsageStats usageStats;
        if (z) {
            indentingPrintWriter.printPair("timeRange", "\"" + DateUtils.formatDateRange(this.mContext, intervalStats.beginTime, intervalStats.endTime, sDateFormatFlags) + "\"");
        } else {
            indentingPrintWriter.printPair("beginTime", Long.valueOf(intervalStats.beginTime));
            indentingPrintWriter.printPair("endTime", Long.valueOf(intervalStats.endTime));
        }
        indentingPrintWriter.println();
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println("packages");
        indentingPrintWriter.increaseIndent();
        ArrayMap<String, UsageStats> arrayMap = intervalStats.packageStats;
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            UsageStats usageStatsValueAt = arrayMap.valueAt(i);
            if (str == null || str.equals(usageStatsValueAt.mPackageName)) {
                indentingPrintWriter.printPair(Settings.ATTR_PACKAGE, usageStatsValueAt.mPackageName);
                indentingPrintWriter.printPair("totalTime", formatElapsedTime(usageStatsValueAt.mTotalTimeInForeground, z));
                indentingPrintWriter.printPair("lastTime", formatDateTime(usageStatsValueAt.mLastTimeUsed, z));
                indentingPrintWriter.printPair("appLaunchCount", Integer.valueOf(usageStatsValueAt.mAppLaunchCount));
                indentingPrintWriter.println();
            }
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        indentingPrintWriter.println("ChooserCounts");
        indentingPrintWriter.increaseIndent();
        Iterator<UsageStats> it2 = arrayMap.values().iterator();
        while (it2.hasNext()) {
            UsageStats next = it2.next();
            if (str == null || str.equals(next.mPackageName)) {
                indentingPrintWriter.printPair(Settings.ATTR_PACKAGE, next.mPackageName);
                if (next.mChooserCounts != null) {
                    int size2 = next.mChooserCounts.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        String str2 = (String) next.mChooserCounts.keyAt(i2);
                        ArrayMap arrayMap2 = (ArrayMap) next.mChooserCounts.valueAt(i2);
                        int size3 = arrayMap2.size();
                        int i3 = 0;
                        while (i3 < size3) {
                            String str3 = (String) arrayMap2.keyAt(i3);
                            int iIntValue = ((Integer) arrayMap2.valueAt(i3)).intValue();
                            if (iIntValue == 0) {
                                it = it2;
                                usageStats = next;
                            } else {
                                it = it2;
                                StringBuilder sb = new StringBuilder();
                                sb.append(str2);
                                usageStats = next;
                                sb.append(":");
                                sb.append(str3);
                                sb.append(" is ");
                                sb.append(Integer.toString(iIntValue));
                                indentingPrintWriter.printPair("ChooserCounts", sb.toString());
                                indentingPrintWriter.println();
                            }
                            i3++;
                            it2 = it;
                            next = usageStats;
                        }
                    }
                }
                indentingPrintWriter.println();
                it2 = it2;
            }
        }
        indentingPrintWriter.decreaseIndent();
        if (str == null) {
            indentingPrintWriter.println("configurations");
            indentingPrintWriter.increaseIndent();
            ArrayMap<Configuration, ConfigurationStats> arrayMap3 = intervalStats.configurations;
            int size4 = arrayMap3.size();
            for (int i4 = 0; i4 < size4; i4++) {
                ConfigurationStats configurationStatsValueAt = arrayMap3.valueAt(i4);
                indentingPrintWriter.printPair("config", Configuration.resourceQualifierString(configurationStatsValueAt.mConfiguration));
                indentingPrintWriter.printPair("totalTime", formatElapsedTime(configurationStatsValueAt.mTotalTimeActive, z));
                indentingPrintWriter.printPair("lastTime", formatDateTime(configurationStatsValueAt.mLastTimeActive, z));
                indentingPrintWriter.printPair(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.valueOf(configurationStatsValueAt.mActivationCount));
                indentingPrintWriter.println();
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("event aggregations");
            indentingPrintWriter.increaseIndent();
            printEventAggregation(indentingPrintWriter, "screen-interactive", intervalStats.interactiveTracker, z);
            printEventAggregation(indentingPrintWriter, "screen-non-interactive", intervalStats.nonInteractiveTracker, z);
            printEventAggregation(indentingPrintWriter, "keyguard-shown", intervalStats.keyguardShownTracker, z);
            printEventAggregation(indentingPrintWriter, "keyguard-hidden", intervalStats.keyguardHiddenTracker, z);
            indentingPrintWriter.decreaseIndent();
        }
        if (!z2) {
            indentingPrintWriter.println("events");
            indentingPrintWriter.increaseIndent();
            EventList eventList = intervalStats.events;
            int size5 = eventList != null ? eventList.size() : 0;
            for (int i5 = 0; i5 < size5; i5++) {
                UsageEvents.Event event = eventList.get(i5);
                if (str == null || str.equals(event.mPackage)) {
                    printEvent(indentingPrintWriter, event, z);
                }
            }
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
    }

    private static String intervalToString(int i) {
        switch (i) {
            case 0:
                return "daily";
            case 1:
                return "weekly";
            case 2:
                return "monthly";
            case 3:
                return "yearly";
            default:
                return "?";
        }
    }

    private static String eventToString(int i) {
        switch (i) {
            case 0:
                return "NONE";
            case 1:
                return "MOVE_TO_FOREGROUND";
            case 2:
                return "MOVE_TO_BACKGROUND";
            case 3:
                return "END_OF_DAY";
            case 4:
                return "CONTINUE_PREVIOUS_DAY";
            case 5:
                return "CONFIGURATION_CHANGE";
            case 6:
                return "SYSTEM_INTERACTION";
            case 7:
                return "USER_INTERACTION";
            case 8:
                return "SHORTCUT_INVOCATION";
            case 9:
                return "CHOOSER_ACTION";
            case 10:
                return "NOTIFICATION_SEEN";
            case 11:
                return "STANDBY_BUCKET_CHANGED";
            case 12:
                return "NOTIFICATION_INTERRUPTION";
            case 13:
                return "SLICE_PINNED_PRIV";
            case 14:
                return "SLICE_PINNED";
            case 15:
                return "SCREEN_INTERACTIVE";
            case 16:
                return "SCREEN_NON_INTERACTIVE";
            default:
                return "UNKNOWN";
        }
    }

    byte[] getBackupPayload(String str) {
        return this.mDatabase.getBackupPayload(str);
    }

    void applyRestoredPayload(String str, byte[] bArr) {
        this.mDatabase.applyRestoredPayload(str, bArr);
    }
}
