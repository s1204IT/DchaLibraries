package com.android.server.power.batterysaver;

import android.metrics.LogMaker;
import android.os.BatteryManagerInternal;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BatterySavingStats {
    private static final boolean DEBUG = false;
    private static final int STATE_CHARGING = -2;
    private static final int STATE_NOT_INITIALIZED = -1;
    private static final String TAG = "BatterySavingStats";
    private BatteryManagerInternal mBatteryManagerInternal;

    @GuardedBy("mLock")
    private int mBatterySaverEnabledCount;

    @GuardedBy("mLock")
    private int mCurrentState;

    @GuardedBy("mLock")
    private boolean mIsBatterySaverEnabled;

    @GuardedBy("mLock")
    private long mLastBatterySaverDisabledTime;

    @GuardedBy("mLock")
    private long mLastBatterySaverEnabledTime;
    private final Object mLock;
    private final MetricsLogger mMetricsLogger;
    private final MetricsLoggerHelper mMetricsLoggerHelper;

    @GuardedBy("mLock")
    @VisibleForTesting
    private boolean mSendTronLog;

    @GuardedBy("mLock")
    @VisibleForTesting
    final ArrayMap<Integer, Stat> mStats;

    interface BatterySaverState {
        public static final int BITS = 1;
        public static final int MASK = 1;
        public static final int OFF = 0;
        public static final int ON = 1;
        public static final int SHIFT = 0;

        static int fromIndex(int i) {
            return (i >> 0) & 1;
        }
    }

    interface InteractiveState {
        public static final int BITS = 1;
        public static final int INTERACTIVE = 1;
        public static final int MASK = 1;
        public static final int NON_INTERACTIVE = 0;
        public static final int SHIFT = 1;

        static int fromIndex(int i) {
            return (i >> 1) & 1;
        }
    }

    interface DozeState {
        public static final int BITS = 2;
        public static final int DEEP = 2;
        public static final int LIGHT = 1;
        public static final int MASK = 3;
        public static final int NOT_DOZING = 0;
        public static final int SHIFT = 2;

        static int fromIndex(int i) {
            return (i >> 2) & 3;
        }
    }

    static class Stat {
        public int endBatteryLevel;
        public int endBatteryPercent;
        public long endTime;
        public int startBatteryLevel;
        public int startBatteryPercent;
        public long startTime;
        public int totalBatteryDrain;
        public int totalBatteryDrainPercent;
        public long totalTimeMillis;

        Stat() {
        }

        public long totalMinutes() {
            return this.totalTimeMillis / 60000;
        }

        public double drainPerHour() {
            if (this.totalTimeMillis == 0) {
                return 0.0d;
            }
            return ((double) this.totalBatteryDrain) / (this.totalTimeMillis / 3600000.0d);
        }

        public double drainPercentPerHour() {
            if (this.totalTimeMillis == 0) {
                return 0.0d;
            }
            return ((double) this.totalBatteryDrainPercent) / (this.totalTimeMillis / 3600000.0d);
        }

        @VisibleForTesting
        String toStringForTest() {
            return "{" + totalMinutes() + "m," + this.totalBatteryDrain + "," + String.format("%.2f", Double.valueOf(drainPerHour())) + "uA/H," + String.format("%.2f", Double.valueOf(drainPercentPerHour())) + "%}";
        }
    }

    @VisibleForTesting
    public BatterySavingStats(Object obj, MetricsLogger metricsLogger) {
        this.mCurrentState = -1;
        this.mStats = new ArrayMap<>();
        this.mBatterySaverEnabledCount = 0;
        this.mLastBatterySaverEnabledTime = 0L;
        this.mLastBatterySaverDisabledTime = 0L;
        this.mMetricsLoggerHelper = new MetricsLoggerHelper();
        this.mLock = obj;
        this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
        this.mMetricsLogger = metricsLogger;
    }

    public BatterySavingStats(Object obj) {
        this(obj, new MetricsLogger());
    }

    public void setSendTronLog(boolean z) {
        synchronized (this.mLock) {
            this.mSendTronLog = z;
        }
    }

    private BatteryManagerInternal getBatteryManagerInternal() {
        if (this.mBatteryManagerInternal == null) {
            this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            if (this.mBatteryManagerInternal == null) {
                Slog.wtf(TAG, "BatteryManagerInternal not initialized");
            }
        }
        return this.mBatteryManagerInternal;
    }

    @VisibleForTesting
    static int statesToIndex(int i, int i2, int i3) {
        return (i & 1) | ((i2 & 1) << 1) | ((i3 & 3) << 2);
    }

    @VisibleForTesting
    static String stateToString(int i) {
        switch (i) {
            case -2:
                return "Charging";
            case -1:
                return "NotInitialized";
            default:
                return "BS=" + BatterySaverState.fromIndex(i) + ",I=" + InteractiveState.fromIndex(i) + ",D=" + DozeState.fromIndex(i);
        }
    }

    @VisibleForTesting
    Stat getStat(int i) {
        Stat stat;
        synchronized (this.mLock) {
            stat = this.mStats.get(Integer.valueOf(i));
            if (stat == null) {
                stat = new Stat();
                this.mStats.put(Integer.valueOf(i), stat);
            }
        }
        return stat;
    }

    private Stat getStat(int i, int i2, int i3) {
        return getStat(statesToIndex(i, i2, i3));
    }

    @VisibleForTesting
    long injectCurrentTime() {
        return SystemClock.elapsedRealtime();
    }

    @VisibleForTesting
    int injectBatteryLevel() {
        BatteryManagerInternal batteryManagerInternal = getBatteryManagerInternal();
        if (batteryManagerInternal == null) {
            return 0;
        }
        return batteryManagerInternal.getBatteryChargeCounter();
    }

    @VisibleForTesting
    int injectBatteryPercent() {
        BatteryManagerInternal batteryManagerInternal = getBatteryManagerInternal();
        if (batteryManagerInternal == null) {
            return 0;
        }
        return batteryManagerInternal.getBatteryLevel();
    }

    public void transitionState(int i, int i2, int i3) {
        synchronized (this.mLock) {
            transitionStateLocked(statesToIndex(i, i2, i3));
        }
    }

    public void startCharging() {
        synchronized (this.mLock) {
            transitionStateLocked(-2);
        }
    }

    @GuardedBy("mLock")
    private void transitionStateLocked(int i) {
        if (this.mCurrentState == i) {
            return;
        }
        long jInjectCurrentTime = injectCurrentTime();
        int iInjectBatteryLevel = injectBatteryLevel();
        int iInjectBatteryPercent = injectBatteryPercent();
        boolean z = BatterySaverState.fromIndex(this.mCurrentState) != 0;
        boolean z2 = BatterySaverState.fromIndex(i) != 0;
        if (z != z2) {
            this.mIsBatterySaverEnabled = z2;
            if (z2) {
                this.mBatterySaverEnabledCount++;
                this.mLastBatterySaverEnabledTime = injectCurrentTime();
            } else {
                this.mLastBatterySaverDisabledTime = injectCurrentTime();
            }
        }
        endLastStateLocked(jInjectCurrentTime, iInjectBatteryLevel, iInjectBatteryPercent);
        startNewStateLocked(i, jInjectCurrentTime, iInjectBatteryLevel, iInjectBatteryPercent);
        this.mMetricsLoggerHelper.transitionStateLocked(i, jInjectCurrentTime, iInjectBatteryLevel, iInjectBatteryPercent);
    }

    @GuardedBy("mLock")
    private void endLastStateLocked(long j, int i, int i2) {
        if (this.mCurrentState < 0) {
            return;
        }
        Stat stat = getStat(this.mCurrentState);
        stat.endBatteryLevel = i;
        stat.endBatteryPercent = i2;
        stat.endTime = j;
        long j2 = stat.endTime - stat.startTime;
        int i3 = stat.startBatteryLevel - stat.endBatteryLevel;
        int i4 = stat.startBatteryPercent - stat.endBatteryPercent;
        stat.totalTimeMillis += j2;
        stat.totalBatteryDrain += i3;
        stat.totalBatteryDrainPercent += i4;
        EventLogTags.writeBatterySavingStats(BatterySaverState.fromIndex(this.mCurrentState), InteractiveState.fromIndex(this.mCurrentState), DozeState.fromIndex(this.mCurrentState), j2, i3, i4, stat.totalTimeMillis, stat.totalBatteryDrain, stat.totalBatteryDrainPercent);
    }

    @GuardedBy("mLock")
    private void startNewStateLocked(int i, long j, int i2, int i3) {
        this.mCurrentState = i;
        if (this.mCurrentState < 0) {
            return;
        }
        Stat stat = getStat(this.mCurrentState);
        stat.startBatteryLevel = i2;
        stat.startBatteryPercent = i3;
        stat.startTime = j;
        stat.endTime = 0L;
    }

    public void dump(PrintWriter printWriter, String str) {
        synchronized (this.mLock) {
            printWriter.print(str);
            printWriter.println("Battery saving stats:");
            String str2 = str + "  ";
            long jCurrentTimeMillis = System.currentTimeMillis();
            long jInjectCurrentTime = injectCurrentTime();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            printWriter.print(str2);
            printWriter.print("Battery Saver is currently: ");
            printWriter.println(this.mIsBatterySaverEnabled ? "ON" : "OFF");
            if (this.mLastBatterySaverEnabledTime > 0) {
                printWriter.print(str2);
                printWriter.print("  ");
                printWriter.print("Last ON time: ");
                printWriter.print(simpleDateFormat.format(new Date((jCurrentTimeMillis - jInjectCurrentTime) + this.mLastBatterySaverEnabledTime)));
                printWriter.print(" ");
                TimeUtils.formatDuration(this.mLastBatterySaverEnabledTime, jInjectCurrentTime, printWriter);
                printWriter.println();
            }
            if (this.mLastBatterySaverDisabledTime > 0) {
                printWriter.print(str2);
                printWriter.print("  ");
                printWriter.print("Last OFF time: ");
                printWriter.print(simpleDateFormat.format(new Date((jCurrentTimeMillis - jInjectCurrentTime) + this.mLastBatterySaverDisabledTime)));
                printWriter.print(" ");
                TimeUtils.formatDuration(this.mLastBatterySaverDisabledTime, jInjectCurrentTime, printWriter);
                printWriter.println();
            }
            printWriter.print(str2);
            printWriter.print("  ");
            printWriter.print("Times enabled: ");
            printWriter.println(this.mBatterySaverEnabledCount);
            printWriter.println();
            printWriter.print(str2);
            printWriter.println("Drain stats:");
            printWriter.print(str2);
            printWriter.println("                   Battery saver OFF                          ON");
            dumpLineLocked(printWriter, str2, 0, "NonIntr", 0, "NonDoze");
            dumpLineLocked(printWriter, str2, 1, "   Intr", 0, "       ");
            dumpLineLocked(printWriter, str2, 0, "NonIntr", 2, "Deep   ");
            dumpLineLocked(printWriter, str2, 1, "   Intr", 2, "       ");
            dumpLineLocked(printWriter, str2, 0, "NonIntr", 1, "Light  ");
            dumpLineLocked(printWriter, str2, 1, "   Intr", 1, "       ");
        }
    }

    private void dumpLineLocked(PrintWriter printWriter, String str, int i, String str2, int i2, String str3) {
        printWriter.print(str);
        printWriter.print(str3);
        printWriter.print(" ");
        printWriter.print(str2);
        printWriter.print(": ");
        Stat stat = getStat(0, i, i2);
        Stat stat2 = getStat(1, i, i2);
        printWriter.println(String.format("%6dm %6dmAh(%3d%%) %8.1fmAh/h     %6dm %6dmAh(%3d%%) %8.1fmAh/h", Long.valueOf(stat.totalMinutes()), Integer.valueOf(stat.totalBatteryDrain / 1000), Integer.valueOf(stat.totalBatteryDrainPercent), Double.valueOf(stat.drainPerHour() / 1000.0d), Long.valueOf(stat2.totalMinutes()), Integer.valueOf(stat2.totalBatteryDrain / 1000), Integer.valueOf(stat2.totalBatteryDrainPercent), Double.valueOf(stat2.drainPerHour() / 1000.0d)));
    }

    @VisibleForTesting
    class MetricsLoggerHelper {
        private static final int STATE_CHANGE_DETECT_MASK = 3;
        private int mLastState = -1;
        private int mStartBatteryLevel;
        private int mStartPercent;
        private long mStartTime;

        MetricsLoggerHelper() {
        }

        public void transitionStateLocked(int i, long j, int i2, int i3) {
            if (((this.mLastState >= 0) ^ (i >= 0)) || ((this.mLastState ^ i) & 3) != 0) {
                if (this.mLastState >= 0) {
                    reportLocked(this.mLastState, j - this.mStartTime, this.mStartBatteryLevel, this.mStartPercent, i2, i3);
                }
                this.mStartTime = j;
                this.mStartBatteryLevel = i2;
                this.mStartPercent = i3;
            }
            this.mLastState = i;
        }

        void reportLocked(int i, long j, int i2, int i3, int i4, int i5) {
            if (BatterySavingStats.this.mSendTronLog) {
                BatterySavingStats.this.mMetricsLogger.write(new LogMaker(1302).setSubtype(BatterySaverState.fromIndex(i) != 0 ? 1 : 0).addTaggedData(1303, Integer.valueOf(InteractiveState.fromIndex(i) != 0 ? 1 : 0)).addTaggedData(1304, Long.valueOf(j)).addTaggedData(1305, Integer.valueOf(i2)).addTaggedData(1307, Integer.valueOf(i3)).addTaggedData(1306, Integer.valueOf(i4)).addTaggedData(1308, Integer.valueOf(i5)));
            }
        }
    }
}
