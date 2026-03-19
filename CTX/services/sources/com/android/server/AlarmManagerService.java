package com.android.server;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.IAlarmCompleteListener;
import android.app.IAlarmListener;
import android.app.IAlarmManager;
import android.app.IUidObserver;
import android.app.PendingIntent;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelableException;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.system.Os;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.NtpTrustedTime;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.LocalLog;
import com.android.internal.util.StatLogger;
import com.android.server.AlarmManagerService;
import com.android.server.AppStateTracker;
import com.android.server.DeviceIdleController;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.usage.AppStandbyController;
import com.android.server.utils.PriorityDump;
import com.mediatek.server.MtkDataShaping;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.Predicate;

public class AlarmManagerService extends SystemService {
    static final int ACTIVE_INDEX = 0;
    static final int ALARM_EVENT = 1;
    protected static boolean DEBUG_ALARM_CLOCK = false;
    protected static boolean DEBUG_BATCH = false;
    static final boolean DEBUG_BG_LIMIT;
    static final boolean DEBUG_LISTENER_CALLBACK;
    protected static boolean DEBUG_STANDBY = false;
    protected static boolean DEBUG_VALIDATE = false;
    protected static boolean DEBUG_WAKELOCK = false;
    private static final int ELAPSED_REALTIME_MASK = 8;
    private static final int ELAPSED_REALTIME_WAKEUP_MASK = 4;
    static final int FREQUENT_INDEX = 2;
    static final int IS_WAKEUP_MASK = 5;
    static final long MIN_FUZZABLE_INTERVAL = 10000;
    static final int NEVER_INDEX = 4;
    private static final Intent NEXT_ALARM_CLOCK_CHANGED_INTENT;
    static final int PRIO_NORMAL = 2;
    static final int PRIO_TICK = 0;
    static final int PRIO_WAKEUP = 1;
    static final int RARE_INDEX = 3;
    static final boolean RECORD_ALARMS_IN_HISTORY = true;
    static final boolean RECORD_DEVICE_IDLE_ALARMS = false;
    private static final int RTC_MASK = 2;
    private static final int RTC_WAKEUP_MASK = 1;
    private static final String SYSTEM_UI_SELF_PERMISSION = "android.permission.systemui.IDENTITY";
    protected static final String TAG = "AlarmManager";
    static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    static final int TIME_CHANGED_MASK = 65536;
    static final int TYPE_NONWAKEUP_MASK = 1;
    static final boolean WAKEUP_STATS = false;
    static final int WORKING_INDEX = 1;
    protected static boolean localLOGV = false;
    static final BatchTimeOrder sBatchOrder;
    protected static final IncreasingTimeOrder sIncreasingTimeOrder;
    final long RECENT_WAKEUP_PERIOD;
    protected final ArrayList<Batch> mAlarmBatches;
    final Comparator<Alarm> mAlarmDispatchComparator;
    final ArrayList<IdleDispatchEntry> mAllowWhileIdleDispatches;
    AppOpsManager mAppOps;
    private boolean mAppStandbyParole;
    private AppStateTracker mAppStateTracker;
    protected final Intent mBackgroundIntent;
    protected int mBroadcastRefCount;
    final SparseArray<ArrayMap<String, BroadcastStats>> mBroadcastStats;
    ClockReceiver mClockReceiver;
    final Constants mConstants;
    int mCurrentSeq;
    protected PendingIntent mDateChangeSender;
    protected final DeliveryTracker mDeliveryTracker;
    private final AppStateTracker.Listener mForceAppStandbyListener;
    protected final AlarmHandler mHandler;
    private final SparseArray<AlarmManager.AlarmClockInfo> mHandlerSparseAlarmClockArray;
    Bundle mIdleOptions;
    protected ArrayList<InFlight> mInFlight;
    boolean mInteractive;
    InteractiveStateReceiver mInteractiveStateReceiver;
    private ArrayMap<Pair<String, Integer>, Long> mLastAlarmDeliveredForPackage;
    long mLastAlarmDeliveryTime;
    final SparseLongArray mLastAllowWhileIdleDispatch;
    private long mLastTickAdded;
    private long mLastTickIssued;
    private long mLastTickReceived;
    private long mLastTickRemoved;
    private long mLastTickSet;
    long mLastTimeChangeClockTime;
    long mLastTimeChangeRealtime;
    private long mLastTrigger;
    boolean mLastWakeLockUnimportantForLogging;
    private long mLastWakeup;
    private long mLastWakeupSet;

    @GuardedBy("mLock")
    private int mListenerCount;

    @GuardedBy("mLock")
    private int mListenerFinishCount;
    DeviceIdleController.LocalService mLocalDeviceIdleController;
    final Object mLock;
    final LocalLog mLog;
    long mMaxDelayTime;
    protected long mNativeData;
    private final SparseArray<AlarmManager.AlarmClockInfo> mNextAlarmClockForUser;
    private boolean mNextAlarmClockMayChange;
    private long mNextNonWakeup;
    long mNextNonWakeupDeliveryTime;
    Alarm mNextWakeFromIdle;
    private long mNextWakeup;
    long mNonInteractiveStartTime;
    long mNonInteractiveTime;
    int mNumDelayedAlarms;
    int mNumTimeChanged;
    SparseArray<ArrayList<Alarm>> mPendingBackgroundAlarms;
    Alarm mPendingIdleUntil;
    ArrayList<Alarm> mPendingNonWakeupAlarms;
    private final SparseBooleanArray mPendingSendNextAlarmClockChangedForUser;
    ArrayList<Alarm> mPendingWhileIdleAlarms;
    final HashMap<String, PriorityClass> mPriorities;
    Random mRandom;
    final LinkedList<WakeupEvent> mRecentWakeups;

    @GuardedBy("mLock")
    private int mSendCount;

    @GuardedBy("mLock")
    private int mSendFinishCount;
    private final IBinder mService;
    long mStartCurrentDelayTime;
    private final StatLogger mStatLogger;
    int mSystemUiUid;
    protected PendingIntent mTimeTickSender;
    private final SparseArray<AlarmManager.AlarmClockInfo> mTmpSparseAlarmClockArray;
    long mTotalDelayTime;
    private UninstallReceiver mUninstallReceiver;
    private UsageStatsManagerInternal mUsageStatsManagerInternal;
    final SparseBooleanArray mUseAllowWhileIdleShortTime;
    protected PowerManager.WakeLock mWakeLock;

    interface Stats {
        public static final int REBATCH_ALL_ALARMS = 0;
        public static final int REORDER_ALARMS_FOR_STANDBY = 1;
    }

    private native void close(long j);

    private native long init();

    private native int setKernelTime(long j, long j2);

    private native int setKernelTimezone(long j, int i);

    private native int waitForAlarm(long j);

    protected native int set(long j, int i, long j2, long j3);

    static int access$2008(AlarmManagerService alarmManagerService) {
        int i = alarmManagerService.mListenerFinishCount;
        alarmManagerService.mListenerFinishCount = i + 1;
        return i;
    }

    static int access$2108(AlarmManagerService alarmManagerService) {
        int i = alarmManagerService.mSendFinishCount;
        alarmManagerService.mSendFinishCount = i + 1;
        return i;
    }

    static int access$2208(AlarmManagerService alarmManagerService) {
        int i = alarmManagerService.mSendCount;
        alarmManagerService.mSendCount = i + 1;
        return i;
    }

    static int access$2408(AlarmManagerService alarmManagerService) {
        int i = alarmManagerService.mListenerCount;
        alarmManagerService.mListenerCount = i + 1;
        return i;
    }

    static {
        DEBUG_BATCH = localLOGV;
        DEBUG_VALIDATE = localLOGV;
        DEBUG_ALARM_CLOCK = localLOGV;
        DEBUG_LISTENER_CALLBACK = localLOGV;
        DEBUG_WAKELOCK = localLOGV;
        DEBUG_BG_LIMIT = localLOGV;
        DEBUG_STANDBY = localLOGV;
        sIncreasingTimeOrder = new IncreasingTimeOrder();
        NEXT_ALARM_CLOCK_CHANGED_INTENT = new Intent("android.app.action.NEXT_ALARM_CLOCK_CHANGED").addFlags(553648128);
        sBatchOrder = new BatchTimeOrder();
    }

    static final class IdleDispatchEntry {
        long argRealtime;
        long elapsedRealtime;
        String op;
        String pkg;
        String tag;
        int uid;

        IdleDispatchEntry() {
        }
    }

    private final class Constants extends ContentObserver {
        private static final long DEFAULT_ALLOW_WHILE_IDLE_LONG_TIME = 540000;
        private static final long DEFAULT_ALLOW_WHILE_IDLE_SHORT_TIME = 5000;
        private static final long DEFAULT_ALLOW_WHILE_IDLE_WHITELIST_DURATION = 10000;
        private static final long DEFAULT_LISTENER_TIMEOUT = 5000;
        private static final long DEFAULT_MAX_INTERVAL = 31536000000L;
        private static final long DEFAULT_MIN_FUTURITY = 5000;
        private static final long DEFAULT_MIN_INTERVAL = 60000;
        private static final String KEY_ALLOW_WHILE_IDLE_LONG_TIME = "allow_while_idle_long_time";
        private static final String KEY_ALLOW_WHILE_IDLE_SHORT_TIME = "allow_while_idle_short_time";
        private static final String KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION = "allow_while_idle_whitelist_duration";
        private static final String KEY_LISTENER_TIMEOUT = "listener_timeout";
        private static final String KEY_MAX_INTERVAL = "max_interval";
        private static final String KEY_MIN_FUTURITY = "min_futurity";
        private static final String KEY_MIN_INTERVAL = "min_interval";
        public long ALLOW_WHILE_IDLE_LONG_TIME;
        public long ALLOW_WHILE_IDLE_SHORT_TIME;
        public long ALLOW_WHILE_IDLE_WHITELIST_DURATION;
        public long[] APP_STANDBY_MIN_DELAYS;
        private final long[] DEFAULT_APP_STANDBY_DELAYS;
        private final String[] KEYS_APP_STANDBY_DELAY;
        public long LISTENER_TIMEOUT;
        public long MAX_INTERVAL;
        public long MIN_FUTURITY;
        public long MIN_INTERVAL;
        private long mLastAllowWhileIdleWhitelistDuration;
        private final KeyValueListParser mParser;
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            this.KEYS_APP_STANDBY_DELAY = new String[]{"standby_active_delay", "standby_working_delay", "standby_frequent_delay", "standby_rare_delay", "standby_never_delay"};
            this.DEFAULT_APP_STANDBY_DELAYS = new long[]{0, 360000, BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS, AppStandbyController.SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, 864000000};
            this.MIN_FUTURITY = 5000L;
            this.MIN_INTERVAL = 60000L;
            this.MAX_INTERVAL = 31536000000L;
            this.ALLOW_WHILE_IDLE_SHORT_TIME = 5000L;
            this.ALLOW_WHILE_IDLE_LONG_TIME = DEFAULT_ALLOW_WHILE_IDLE_LONG_TIME;
            this.ALLOW_WHILE_IDLE_WHITELIST_DURATION = 10000L;
            this.LISTENER_TIMEOUT = 5000L;
            this.APP_STANDBY_MIN_DELAYS = new long[this.DEFAULT_APP_STANDBY_DELAYS.length];
            this.mParser = new KeyValueListParser(',');
            this.mLastAllowWhileIdleWhitelistDuration = -1L;
            updateAllowWhileIdleWhitelistDurationLocked();
        }

        public void start(ContentResolver contentResolver) {
            this.mResolver = contentResolver;
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("alarm_manager_constants"), false, this);
            updateConstants();
        }

        public void updateAllowWhileIdleWhitelistDurationLocked() {
            if (this.mLastAllowWhileIdleWhitelistDuration != this.ALLOW_WHILE_IDLE_WHITELIST_DURATION) {
                this.mLastAllowWhileIdleWhitelistDuration = this.ALLOW_WHILE_IDLE_WHITELIST_DURATION;
                BroadcastOptions broadcastOptionsMakeBasic = BroadcastOptions.makeBasic();
                broadcastOptionsMakeBasic.setTemporaryAppWhitelistDuration(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION);
                AlarmManagerService.this.mIdleOptions = broadcastOptionsMakeBasic.toBundle();
            }
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (AlarmManagerService.this.mLock) {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, "alarm_manager_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(AlarmManagerService.TAG, "Bad alarm manager settings", e);
                }
                this.MIN_FUTURITY = this.mParser.getLong(KEY_MIN_FUTURITY, 5000L);
                this.MIN_INTERVAL = this.mParser.getLong(KEY_MIN_INTERVAL, 60000L);
                this.MAX_INTERVAL = this.mParser.getLong(KEY_MAX_INTERVAL, 31536000000L);
                this.ALLOW_WHILE_IDLE_SHORT_TIME = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_SHORT_TIME, 5000L);
                this.ALLOW_WHILE_IDLE_LONG_TIME = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_LONG_TIME, DEFAULT_ALLOW_WHILE_IDLE_LONG_TIME);
                this.ALLOW_WHILE_IDLE_WHITELIST_DURATION = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION, 10000L);
                this.LISTENER_TIMEOUT = this.mParser.getLong(KEY_LISTENER_TIMEOUT, 5000L);
                this.APP_STANDBY_MIN_DELAYS[0] = this.mParser.getDurationMillis(this.KEYS_APP_STANDBY_DELAY[0], this.DEFAULT_APP_STANDBY_DELAYS[0]);
                for (int i = 1; i < this.KEYS_APP_STANDBY_DELAY.length; i++) {
                    this.APP_STANDBY_MIN_DELAYS[i] = this.mParser.getDurationMillis(this.KEYS_APP_STANDBY_DELAY[i], Math.max(this.APP_STANDBY_MIN_DELAYS[i - 1], this.DEFAULT_APP_STANDBY_DELAYS[i]));
                }
                updateAllowWhileIdleWhitelistDurationLocked();
            }
        }

        void dump(PrintWriter printWriter) {
            printWriter.println("  Settings:");
            printWriter.print("    ");
            printWriter.print(KEY_MIN_FUTURITY);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MIN_FUTURITY, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MIN_INTERVAL);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MIN_INTERVAL, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MAX_INTERVAL);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MAX_INTERVAL, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LISTENER_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.LISTENER_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_ALLOW_WHILE_IDLE_SHORT_TIME);
            printWriter.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_SHORT_TIME, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_ALLOW_WHILE_IDLE_LONG_TIME);
            printWriter.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_LONG_TIME, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION);
            printWriter.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION, printWriter);
            printWriter.println();
            for (int i = 0; i < this.KEYS_APP_STANDBY_DELAY.length; i++) {
                printWriter.print("    ");
                printWriter.print(this.KEYS_APP_STANDBY_DELAY[i]);
                printWriter.print("=");
                TimeUtils.formatDuration(this.APP_STANDBY_MIN_DELAYS[i], printWriter);
                printWriter.println();
            }
        }

        void dumpProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1112396529665L, this.MIN_FUTURITY);
            protoOutputStream.write(1112396529666L, this.MIN_INTERVAL);
            protoOutputStream.write(1112396529671L, this.MAX_INTERVAL);
            protoOutputStream.write(1112396529667L, this.LISTENER_TIMEOUT);
            protoOutputStream.write(1112396529668L, this.ALLOW_WHILE_IDLE_SHORT_TIME);
            protoOutputStream.write(1112396529669L, this.ALLOW_WHILE_IDLE_LONG_TIME);
            protoOutputStream.write(1112396529670L, this.ALLOW_WHILE_IDLE_WHITELIST_DURATION);
            protoOutputStream.end(jStart);
        }
    }

    final class PriorityClass {
        int priority = 2;
        int seq;

        PriorityClass() {
            this.seq = AlarmManagerService.this.mCurrentSeq - 1;
        }
    }

    static final class WakeupEvent {
        public String action;
        public int uid;
        public long when;

        public WakeupEvent(long j, int i, String str) {
            this.when = j;
            this.uid = i;
            this.action = str;
        }
    }

    public final class Batch {
        final ArrayList<Alarm> alarms;
        long end;
        int flags;
        long start;

        Batch() {
            this.alarms = new ArrayList<>();
            this.start = 0L;
            this.end = JobStatus.NO_LATEST_RUNTIME;
            this.flags = 0;
        }

        Batch(Alarm alarm) {
            this.alarms = new ArrayList<>();
            this.start = alarm.whenElapsed;
            this.end = AlarmManagerService.clampPositive(alarm.maxWhenElapsed);
            this.flags = alarm.flags;
            this.alarms.add(alarm);
            if (alarm.operation == AlarmManagerService.this.mTimeTickSender) {
                AlarmManagerService.this.mLastTickAdded = System.currentTimeMillis();
            }
        }

        public int size() {
            return this.alarms.size();
        }

        Alarm get(int i) {
            return this.alarms.get(i);
        }

        boolean canHold(long j, long j2) {
            return this.end >= j && this.start <= j2;
        }

        boolean add(Alarm alarm) {
            int iBinarySearch = Collections.binarySearch(this.alarms, alarm, AlarmManagerService.sIncreasingTimeOrder);
            boolean z = true;
            if (iBinarySearch < 0) {
                iBinarySearch = (0 - iBinarySearch) - 1;
            }
            this.alarms.add(iBinarySearch, alarm);
            if (alarm.operation == AlarmManagerService.this.mTimeTickSender) {
                AlarmManagerService.this.mLastTickAdded = System.currentTimeMillis();
            }
            if (AlarmManagerService.DEBUG_BATCH) {
                Slog.v(AlarmManagerService.TAG, "Adding " + alarm + " to " + this);
            }
            if (alarm.whenElapsed > this.start) {
                this.start = alarm.whenElapsed;
            } else {
                z = false;
            }
            if (alarm.maxWhenElapsed < this.end) {
                this.end = alarm.maxWhenElapsed;
            }
            this.flags = alarm.flags | this.flags;
            if (AlarmManagerService.DEBUG_BATCH) {
                Slog.v(AlarmManagerService.TAG, "    => now " + this);
            }
            return z;
        }

        static boolean lambda$remove$0(Alarm alarm, Alarm alarm2) {
            return alarm2 == alarm;
        }

        boolean remove(final Alarm alarm) {
            return remove(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return AlarmManagerService.Batch.lambda$remove$0(alarm, (AlarmManagerService.Alarm) obj);
                }
            });
        }

        public boolean remove(Predicate<Alarm> predicate) {
            int i = 0;
            int i2 = 0;
            long j = Long.MAX_VALUE;
            long j2 = 0;
            boolean z = false;
            while (i < this.alarms.size()) {
                Alarm alarm = this.alarms.get(i);
                if (predicate.test(alarm)) {
                    this.alarms.remove(i);
                    if (alarm.alarmClock != null) {
                        AlarmManagerService.this.mNextAlarmClockMayChange = true;
                    }
                    if (alarm.operation == AlarmManagerService.this.mTimeTickSender) {
                        AlarmManagerService.this.mLastTickRemoved = System.currentTimeMillis();
                    }
                    z = true;
                } else {
                    if (alarm.whenElapsed > j2) {
                        j2 = alarm.whenElapsed;
                    }
                    if (alarm.maxWhenElapsed < j) {
                        j = alarm.maxWhenElapsed;
                    }
                    i2 |= alarm.flags;
                    i++;
                }
            }
            if (z) {
                this.start = j2;
                this.end = j;
                this.flags = i2;
            }
            return z;
        }

        boolean hasPackage(String str) {
            int size = this.alarms.size();
            for (int i = 0; i < size; i++) {
                if (this.alarms.get(i).matches(str)) {
                    return true;
                }
            }
            return false;
        }

        boolean hasWakeups() {
            int size = this.alarms.size();
            for (int i = 0; i < size; i++) {
                if ((this.alarms.get(i).type & 1) == 0) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(40);
            sb.append("Batch{");
            sb.append(Integer.toHexString(hashCode()));
            sb.append(" num=");
            sb.append(size());
            sb.append(" start=");
            sb.append(this.start);
            sb.append(" end=");
            sb.append(this.end);
            if (this.flags != 0) {
                sb.append(" flgs=0x");
                sb.append(Integer.toHexString(this.flags));
            }
            sb.append('}');
            return sb.toString();
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j, long j2, long j3) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1112396529665L, this.start);
            protoOutputStream.write(1112396529666L, this.end);
            protoOutputStream.write(1120986464259L, this.flags);
            Iterator<Alarm> it = this.alarms.iterator();
            while (it.hasNext()) {
                it.next().writeToProto(protoOutputStream, 2246267895812L, j2, j3);
            }
            protoOutputStream.end(jStart);
        }
    }

    static class BatchTimeOrder implements Comparator<Batch> {
        BatchTimeOrder() {
        }

        @Override
        public int compare(Batch batch, Batch batch2) {
            long j = batch.start;
            long j2 = batch2.start;
            if (j > j2) {
                return 1;
            }
            if (j < j2) {
                return -1;
            }
            return 0;
        }
    }

    void calculateDeliveryPriorities(ArrayList<Alarm> arrayList) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            Alarm alarm = arrayList.get(i);
            int i2 = (alarm.operation == null || !"android.intent.action.TIME_TICK".equals(alarm.operation.getIntent().getAction())) ? alarm.wakeup ? 1 : 2 : 0;
            PriorityClass priorityClass = alarm.priorityClass;
            String str = alarm.sourcePackage;
            if (priorityClass == null) {
                priorityClass = this.mPriorities.get(str);
            }
            if (priorityClass == null) {
                priorityClass = new PriorityClass();
                alarm.priorityClass = priorityClass;
                this.mPriorities.put(str, priorityClass);
            }
            alarm.priorityClass = priorityClass;
            if (priorityClass.seq != this.mCurrentSeq) {
                priorityClass.priority = i2;
                priorityClass.seq = this.mCurrentSeq;
            } else if (i2 < priorityClass.priority) {
                priorityClass.priority = i2;
            }
        }
    }

    protected void updateWakeupAlarmLog(Alarm alarm) {
    }

    protected boolean isPowerOffAlarmType(int i) {
        return false;
    }

    protected boolean schedulePoweroffAlarm(int i, long j, long j2, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo, String str2) {
        return true;
    }

    protected void updatePoweroffAlarmtoNowRtc() {
    }

    public void cancelPoweroffAlarmImpl(String str) {
    }

    protected void registerWFDStatusChangeReciever() {
    }

    protected boolean isWFDConnected() {
        return false;
    }

    protected void initPpl() {
    }

    protected boolean freePplCheck(ArrayList<Alarm> arrayList, long j) {
        return false;
    }

    protected boolean removeInvalidAlarmLocked(PendingIntent pendingIntent, IAlarmListener iAlarmListener) {
        removeImpl(pendingIntent);
        return true;
    }

    protected boolean needAlarmGrouping() {
        return false;
    }

    protected void resetneedRebatchForRepeatingAlarm() {
    }

    protected boolean needRebatchForRepeatingAlarm() {
        return false;
    }

    protected boolean supportAlarmGrouping() {
        return false;
    }

    protected void initAlarmGrouping() {
    }

    protected long getMaxTriggerTimeforAlarmGrouping(int i, long j, long j2, long j3, long j4, PendingIntent pendingIntent, Alarm alarm) {
        return j;
    }

    public AlarmManagerService(Context context) {
        super(context);
        this.mBackgroundIntent = new Intent().addFlags(4);
        this.mLog = new LocalLog(TAG);
        this.mLock = new Object();
        this.mPendingBackgroundAlarms = new SparseArray<>();
        this.mBroadcastRefCount = 0;
        this.mPendingNonWakeupAlarms = new ArrayList<>();
        this.mInFlight = new ArrayList<>();
        this.mHandler = new AlarmHandler();
        this.mDeliveryTracker = new DeliveryTracker();
        this.mInteractive = true;
        this.mLastAllowWhileIdleDispatch = new SparseLongArray();
        this.mUseAllowWhileIdleShortTime = new SparseBooleanArray();
        this.mAllowWhileIdleDispatches = new ArrayList<>();
        this.mStatLogger = new StatLogger(new String[]{"REBATCH_ALL_ALARMS", "REORDER_ALARMS_FOR_STANDBY"});
        this.mNextAlarmClockForUser = new SparseArray<>();
        this.mTmpSparseAlarmClockArray = new SparseArray<>();
        this.mPendingSendNextAlarmClockChangedForUser = new SparseBooleanArray();
        this.mHandlerSparseAlarmClockArray = new SparseArray<>();
        this.mLastAlarmDeliveredForPackage = new ArrayMap<>();
        this.mPriorities = new HashMap<>();
        this.mCurrentSeq = 0;
        this.mRecentWakeups = new LinkedList<>();
        this.RECENT_WAKEUP_PERIOD = 86400000L;
        this.mAlarmDispatchComparator = new Comparator<Alarm>() {
            @Override
            public int compare(Alarm alarm, Alarm alarm2) {
                if (alarm.priorityClass.priority < alarm2.priorityClass.priority) {
                    return -1;
                }
                if (alarm.priorityClass.priority > alarm2.priorityClass.priority) {
                    return 1;
                }
                if (alarm.whenElapsed < alarm2.whenElapsed) {
                    return -1;
                }
                return alarm.whenElapsed > alarm2.whenElapsed ? 1 : 0;
            }
        };
        this.mAlarmBatches = new ArrayList<>();
        this.mPendingIdleUntil = null;
        this.mNextWakeFromIdle = null;
        this.mPendingWhileIdleAlarms = new ArrayList<>();
        this.mBroadcastStats = new SparseArray<>();
        this.mNumDelayedAlarms = 0;
        this.mTotalDelayTime = 0L;
        this.mMaxDelayTime = 0L;
        this.mService = new IAlarmManager.Stub() {
            public void set(String str, int i, long j, long j2, long j3, int i2, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str2, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo) throws Throwable {
                int i3;
                int i4;
                int callingUid = Binder.getCallingUid();
                AlarmManagerService.this.mAppOps.checkPackage(callingUid, str);
                if (j3 != 0 && iAlarmListener != null) {
                    throw new IllegalArgumentException("Repeating alarms cannot use AlarmReceivers");
                }
                if (workSource != null) {
                    AlarmManagerService.this.getContext().enforcePermission("android.permission.UPDATE_DEVICE_STATS", Binder.getCallingPid(), callingUid, "AlarmManager.set");
                }
                int i5 = i2 & (-11);
                if (callingUid != 1000) {
                    i5 &= -17;
                }
                if (j2 == 0) {
                    i5 |= 1;
                }
                if (alarmClockInfo != null) {
                    i4 = i5 | 3;
                } else if (workSource == null && (callingUid < 10000 || UserHandle.isSameApp(callingUid, AlarmManagerService.this.mSystemUiUid) || (AlarmManagerService.this.mAppStateTracker != null && AlarmManagerService.this.mAppStateTracker.isUidPowerSaveUserWhitelisted(callingUid)))) {
                    i4 = (i5 | 8) & (-5);
                } else {
                    i3 = i5;
                    AlarmManagerService.this.setImpl(i, j, j2, j3, pendingIntent, iAlarmListener, str2, i3, workSource, alarmClockInfo, callingUid, str);
                }
                i3 = i4;
                AlarmManagerService.this.setImpl(i, j, j2, j3, pendingIntent, iAlarmListener, str2, i3, workSource, alarmClockInfo, callingUid, str);
            }

            public boolean setTime(long j) {
                AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "setTime");
                return AlarmManagerService.this.setTimeImpl(j);
            }

            public void setTimeZone(String str) {
                AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME_ZONE", "setTimeZone");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    AlarmManagerService.this.setTimeZoneImpl(str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void remove(PendingIntent pendingIntent, IAlarmListener iAlarmListener) {
                if (pendingIntent == null && iAlarmListener == null) {
                    Slog.w(AlarmManagerService.TAG, "remove() with no intent or listener");
                    return;
                }
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.removeLocked(pendingIntent, iAlarmListener);
                }
            }

            public long getNextWakeFromIdleTime() {
                return AlarmManagerService.this.getNextWakeFromIdleTimeImpl();
            }

            public void cancelPoweroffAlarm(String str) {
                AlarmManagerService.this.cancelPoweroffAlarmImpl(str);
            }

            public AlarmManager.AlarmClockInfo getNextAlarmClock(int i) {
                return AlarmManagerService.this.getNextAlarmClockImpl(ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, false, "getNextAlarmClock", null));
            }

            public long currentNetworkTimeMillis() throws ParcelableException {
                NtpTrustedTime ntpTrustedTime = NtpTrustedTime.getInstance(AlarmManagerService.this.getContext());
                if (ntpTrustedTime.hasCache()) {
                    return ntpTrustedTime.currentTimeMillis();
                }
                throw new ParcelableException(new DateTimeException("Missing NTP fix"));
            }

            protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
                if (DumpUtils.checkDumpAndUsageStatsPermission(AlarmManagerService.this.getContext(), AlarmManagerService.TAG, printWriter)) {
                    if (strArr.length > 0 && PriorityDump.PROTO_ARG.equals(strArr[0])) {
                        AlarmManagerService.this.dumpProto(fileDescriptor);
                    } else {
                        AlarmManagerService.this.dumpWithargs(printWriter, strArr);
                    }
                }
            }

            public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
                new ShellCmd().exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
            }
        };
        this.mForceAppStandbyListener = new AppStateTracker.Listener() {
            @Override
            public void unblockAllUnrestrictedAlarms() {
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.sendAllUnrestrictedPendingBackgroundAlarmsLocked();
                }
            }

            @Override
            public void unblockAlarmsForUid(int i) {
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.sendPendingBackgroundAlarmsLocked(i, null);
                }
            }

            @Override
            public void unblockAlarmsForUidPackage(int i, String str) {
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.sendPendingBackgroundAlarmsLocked(i, str);
                }
            }

            @Override
            public void onUidForeground(int i, boolean z) {
                synchronized (AlarmManagerService.this.mLock) {
                    if (z) {
                        try {
                            AlarmManagerService.this.mUseAllowWhileIdleShortTime.put(i, true);
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                }
            }
        };
        this.mSendCount = 0;
        this.mSendFinishCount = 0;
        this.mListenerCount = 0;
        this.mListenerFinishCount = 0;
        this.mConstants = new Constants(this.mHandler);
        publishLocalService(AlarmManagerInternal.class, new LocalService());
    }

    static long convertToElapsed(long j, int i) {
        boolean z = true;
        if (i != 1 && i != 0) {
            z = false;
        }
        if (z) {
            return j - (System.currentTimeMillis() - SystemClock.elapsedRealtime());
        }
        return j;
    }

    static long maxTriggerTime(long j, long j2, long j3) {
        if (j3 == 0) {
            j3 = j2 - j;
        }
        if (j3 < 10000) {
            j3 = 0;
        }
        return clampPositive(j2 + ((long) (0.75d * j3)));
    }

    static boolean addBatchLocked(ArrayList<Batch> arrayList, Batch batch) {
        int iBinarySearch = Collections.binarySearch(arrayList, batch, sBatchOrder);
        if (iBinarySearch < 0) {
            iBinarySearch = (0 - iBinarySearch) - 1;
        }
        arrayList.add(iBinarySearch, batch);
        return iBinarySearch == 0;
    }

    private void insertAndBatchAlarmLocked(Alarm alarm) {
        int iAttemptCoalesceLocked;
        if ((supportAlarmGrouping() && !alarm.needGrouping) || (alarm.flags & 1) != 0) {
            iAttemptCoalesceLocked = -1;
        } else {
            iAttemptCoalesceLocked = attemptCoalesceLocked(alarm.whenElapsed, alarm.maxWhenElapsed);
        }
        if (iAttemptCoalesceLocked < 0) {
            addBatchLocked(this.mAlarmBatches, new Batch(alarm));
            return;
        }
        Batch batch = this.mAlarmBatches.get(iAttemptCoalesceLocked);
        if (batch.add(alarm)) {
            this.mAlarmBatches.remove(iAttemptCoalesceLocked);
            addBatchLocked(this.mAlarmBatches, batch);
        }
    }

    int attemptCoalesceLocked(long j, long j2) {
        int size = this.mAlarmBatches.size();
        for (int i = 0; i < size; i++) {
            Batch batch = this.mAlarmBatches.get(i);
            if (((batch.flags & 1) == 0 || supportAlarmGrouping()) && batch.canHold(j, j2)) {
                return i;
            }
        }
        return -1;
    }

    static int getAlarmCount(ArrayList<Batch> arrayList) {
        int size = arrayList.size();
        int size2 = 0;
        for (int i = 0; i < size; i++) {
            size2 += arrayList.get(i).size();
        }
        return size2;
    }

    boolean haveAlarmsTimeTickAlarm(ArrayList<Alarm> arrayList) {
        if (arrayList.size() == 0) {
            return false;
        }
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (arrayList.get(i).operation == this.mTimeTickSender) {
                return true;
            }
        }
        return false;
    }

    boolean haveBatchesTimeTickAlarm(ArrayList<Batch> arrayList) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (haveAlarmsTimeTickAlarm(arrayList.get(i).alarms)) {
                return true;
            }
        }
        return false;
    }

    void rebatchAllAlarms() {
        synchronized (this.mLock) {
            rebatchAllAlarmsLocked(true);
        }
    }

    void rebatchAllAlarmsLocked(boolean z) {
        long time = this.mStatLogger.getTime();
        int alarmCount = getAlarmCount(this.mAlarmBatches) + ArrayUtils.size(this.mPendingWhileIdleAlarms);
        boolean z2 = haveBatchesTimeTickAlarm(this.mAlarmBatches) || haveAlarmsTimeTickAlarm(this.mPendingWhileIdleAlarms);
        ArrayList arrayList = (ArrayList) this.mAlarmBatches.clone();
        this.mAlarmBatches.clear();
        Alarm alarm = this.mPendingIdleUntil;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            Batch batch = (Batch) arrayList.get(i);
            int size2 = batch.size();
            for (int i2 = 0; i2 < size2; i2++) {
                reAddAlarmLocked(batch.get(i2), jElapsedRealtime, z);
            }
        }
        if (alarm != null && alarm != this.mPendingIdleUntil) {
            Slog.wtf(TAG, "Rebatching: idle until changed from " + alarm + " to " + this.mPendingIdleUntil);
            if (this.mPendingIdleUntil == null) {
                restorePendingWhileIdleAlarmsLocked();
            }
        }
        int alarmCount2 = getAlarmCount(this.mAlarmBatches) + ArrayUtils.size(this.mPendingWhileIdleAlarms);
        boolean z3 = haveBatchesTimeTickAlarm(this.mAlarmBatches) || haveAlarmsTimeTickAlarm(this.mPendingWhileIdleAlarms);
        if (alarmCount != alarmCount2) {
            Slog.wtf(TAG, "Rebatching: total count changed from " + alarmCount + " to " + alarmCount2);
        }
        if (z2 != z3) {
            Slog.wtf(TAG, "Rebatching: hasTick changed from " + z2 + " to " + z3);
        }
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
        this.mStatLogger.logDurationStat(0, time);
    }

    boolean reorderAlarmsBasedOnStandbyBuckets(ArraySet<Pair<String, Integer>> arraySet) {
        long time = this.mStatLogger.getTime();
        ArrayList arrayList = new ArrayList();
        for (int size = this.mAlarmBatches.size() - 1; size >= 0; size--) {
            Batch batch = this.mAlarmBatches.get(size);
            for (int size2 = batch.size() - 1; size2 >= 0; size2--) {
                Alarm alarm = batch.get(size2);
                Pair pairCreate = Pair.create(alarm.sourcePackage, Integer.valueOf(UserHandle.getUserId(alarm.creatorUid)));
                if ((arraySet == null || arraySet.contains(pairCreate)) && adjustDeliveryTimeBasedOnStandbyBucketLocked(alarm)) {
                    batch.remove(alarm);
                    arrayList.add(alarm);
                }
            }
            if (batch.size() == 0) {
                this.mAlarmBatches.remove(size);
            }
        }
        for (int i = 0; i < arrayList.size(); i++) {
            insertAndBatchAlarmLocked((Alarm) arrayList.get(i));
        }
        this.mStatLogger.logDurationStat(1, time);
        return arrayList.size() > 0;
    }

    void reAddAlarmLocked(Alarm alarm, long j, boolean z) {
        long jMaxTriggerTime;
        alarm.when = alarm.origWhen;
        long jConvertToElapsed = convertToElapsed(alarm.when, alarm.type);
        if (supportAlarmGrouping()) {
            jMaxTriggerTime = getMaxTriggerTimeforAlarmGrouping(alarm.type, jConvertToElapsed, jConvertToElapsed, alarm.windowLength, alarm.repeatInterval, alarm.operation, alarm);
        } else if (alarm.windowLength == 0) {
            jMaxTriggerTime = jConvertToElapsed;
        } else if (alarm.windowLength > 0) {
            jMaxTriggerTime = clampPositive(alarm.windowLength + jConvertToElapsed);
        } else {
            jMaxTriggerTime = maxTriggerTime(j, jConvertToElapsed, alarm.repeatInterval);
        }
        alarm.whenElapsed = jConvertToElapsed;
        alarm.maxWhenElapsed = jMaxTriggerTime;
        setImplLocked(alarm, true, z);
    }

    static long clampPositive(long j) {
        return j >= 0 ? j : JobStatus.NO_LATEST_RUNTIME;
    }

    void sendPendingBackgroundAlarmsLocked(int i, String str) {
        ArrayList<Alarm> arrayList = this.mPendingBackgroundAlarms.get(i);
        if (arrayList == null || arrayList.size() == 0) {
            return;
        }
        if (str != null) {
            if (DEBUG_BG_LIMIT) {
                Slog.d(TAG, "Sending blocked alarms for uid " + i + ", package " + str);
            }
            ArrayList<Alarm> arrayList2 = new ArrayList<>();
            for (int size = arrayList.size() - 1; size >= 0; size--) {
                if (arrayList.get(size).matches(str)) {
                    arrayList2.add(arrayList.remove(size));
                }
            }
            if (arrayList.size() == 0) {
                this.mPendingBackgroundAlarms.remove(i);
            }
            arrayList = arrayList2;
        } else {
            if (DEBUG_BG_LIMIT) {
                Slog.d(TAG, "Sending blocked alarms for uid " + i);
            }
            this.mPendingBackgroundAlarms.remove(i);
        }
        deliverPendingBackgroundAlarmsLocked(arrayList, SystemClock.elapsedRealtime());
    }

    void sendAllUnrestrictedPendingBackgroundAlarmsLocked() {
        ArrayList<Alarm> arrayList = new ArrayList<>();
        findAllUnrestrictedPendingBackgroundAlarmsLockedInner(this.mPendingBackgroundAlarms, arrayList, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return this.f$0.isBackgroundRestricted((AlarmManagerService.Alarm) obj);
            }
        });
        if (arrayList.size() > 0) {
            deliverPendingBackgroundAlarmsLocked(arrayList, SystemClock.elapsedRealtime());
        }
    }

    @VisibleForTesting
    static void findAllUnrestrictedPendingBackgroundAlarmsLockedInner(SparseArray<ArrayList<Alarm>> sparseArray, ArrayList<Alarm> arrayList, Predicate<Alarm> predicate) {
        for (int size = sparseArray.size() - 1; size >= 0; size--) {
            sparseArray.keyAt(size);
            ArrayList<Alarm> arrayListValueAt = sparseArray.valueAt(size);
            for (int size2 = arrayListValueAt.size() - 1; size2 >= 0; size2--) {
                Alarm alarm = arrayListValueAt.get(size2);
                if (!predicate.test(alarm)) {
                    arrayList.add(alarm);
                    arrayListValueAt.remove(size2);
                }
            }
            if (arrayListValueAt.size() == 0) {
                sparseArray.removeAt(size);
            }
        }
    }

    private void deliverPendingBackgroundAlarmsLocked(ArrayList<Alarm> arrayList, long j) {
        AlarmManagerService alarmManagerService;
        long j2;
        int i;
        int i2;
        AlarmManagerService alarmManagerService2 = this;
        ArrayList<Alarm> arrayList2 = arrayList;
        long j3 = j;
        int size = arrayList.size();
        boolean z = false;
        int i3 = 0;
        while (i3 < size) {
            Alarm alarm = arrayList2.get(i3);
            boolean z2 = alarm.wakeup ? true : z;
            alarm.count = 1;
            if (alarm.repeatInterval <= 0) {
                i = size;
                i2 = i3;
            } else {
                alarm.count = (int) (((long) alarm.count) + ((j3 - alarm.expectedWhenElapsed) / alarm.repeatInterval));
                long j4 = ((long) alarm.count) * alarm.repeatInterval;
                long j5 = alarm.whenElapsed + j4;
                i = size;
                i2 = i3;
                alarmManagerService2.setImplLocked(alarm.type, alarm.when + j4, j5, alarm.windowLength, maxTriggerTime(j3, j5, alarm.repeatInterval), alarm.repeatInterval, alarm.operation, null, null, alarm.flags, true, alarm.workSource, alarm.alarmClock, alarm.uid, alarm.packageName, alarm.needGrouping);
            }
            i3 = i2 + 1;
            z = z2;
            size = i;
            j3 = j;
            arrayList2 = arrayList;
            alarmManagerService2 = this;
        }
        if (z) {
            alarmManagerService = this;
            j2 = j;
        } else {
            alarmManagerService = this;
            j2 = j;
            if (alarmManagerService.checkAllowNonWakeupDelayLocked(j2)) {
                if (alarmManagerService.mPendingNonWakeupAlarms.size() == 0) {
                    alarmManagerService.mStartCurrentDelayTime = j2;
                    alarmManagerService.mNextNonWakeupDeliveryTime = j2 + ((alarmManagerService.currentNonWakeupFuzzLocked(j2) * 3) / 2);
                }
                alarmManagerService.mPendingNonWakeupAlarms.addAll(arrayList);
                alarmManagerService.mNumDelayedAlarms += arrayList.size();
                return;
            }
        }
        if (DEBUG_BG_LIMIT) {
            Slog.d(TAG, "Waking up to deliver pending blocked alarms");
        }
        if (alarmManagerService.mPendingNonWakeupAlarms.size() > 0) {
            arrayList.addAll(alarmManagerService.mPendingNonWakeupAlarms);
            long j6 = j2 - alarmManagerService.mStartCurrentDelayTime;
            alarmManagerService.mTotalDelayTime += j6;
            if (alarmManagerService.mMaxDelayTime < j6) {
                alarmManagerService.mMaxDelayTime = j6;
            }
            alarmManagerService.mPendingNonWakeupAlarms.clear();
        }
        calculateDeliveryPriorities(arrayList);
        Collections.sort(arrayList, alarmManagerService.mAlarmDispatchComparator);
        deliverAlarmsLocked(arrayList, j);
    }

    void restorePendingWhileIdleAlarmsLocked() {
        if (this.mPendingWhileIdleAlarms.size() > 0) {
            ArrayList<Alarm> arrayList = this.mPendingWhileIdleAlarms;
            this.mPendingWhileIdleAlarms = new ArrayList<>();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            for (int size = arrayList.size() - 1; size >= 0; size--) {
                reAddAlarmLocked(arrayList.get(size), jElapsedRealtime, false);
            }
        }
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
        try {
            this.mTimeTickSender.send();
        } catch (PendingIntent.CanceledException e) {
        }
    }

    protected static final class InFlight {
        final int mAlarmType;
        public final BroadcastStats mBroadcastStats;
        public final FilterStats mFilterStats;
        final IBinder mListener;
        final PendingIntent mPendingIntent;
        final String mTag;
        final int mUid;
        final long mWhenElapsed;
        final WorkSource mWorkSource;

        public InFlight(AlarmManagerService alarmManagerService, PendingIntent pendingIntent, IAlarmListener iAlarmListener, WorkSource workSource, int i, String str, int i2, String str2, long j) {
            this.mPendingIntent = pendingIntent;
            this.mWhenElapsed = j;
            this.mListener = iAlarmListener != null ? iAlarmListener.asBinder() : null;
            this.mWorkSource = workSource;
            this.mUid = i;
            this.mTag = str2;
            this.mBroadcastStats = pendingIntent != null ? alarmManagerService.getStatsLocked(pendingIntent) : alarmManagerService.getStatsLocked(i, str);
            FilterStats filterStats = this.mBroadcastStats.filterStats.get(this.mTag);
            if (filterStats == null) {
                filterStats = new FilterStats(this.mBroadcastStats, this.mTag);
                this.mBroadcastStats.filterStats.put(this.mTag, filterStats);
            }
            filterStats.lastTime = j;
            this.mFilterStats = filterStats;
            this.mAlarmType = i2;
        }

        public String toString() {
            return "InFlight{pendingIntent=" + this.mPendingIntent + ", when=" + this.mWhenElapsed + ", workSource=" + this.mWorkSource + ", uid=" + this.mUid + ", tag=" + this.mTag + ", broadcastStats=" + this.mBroadcastStats + ", filterStats=" + this.mFilterStats + ", alarmType=" + this.mAlarmType + "}";
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1120986464257L, this.mUid);
            protoOutputStream.write(1138166333442L, this.mTag);
            protoOutputStream.write(1112396529667L, this.mWhenElapsed);
            protoOutputStream.write(1159641169924L, this.mAlarmType);
            if (this.mPendingIntent != null) {
                this.mPendingIntent.writeToProto(protoOutputStream, 1146756268037L);
            }
            if (this.mBroadcastStats != null) {
                this.mBroadcastStats.writeToProto(protoOutputStream, 1146756268038L);
            }
            if (this.mFilterStats != null) {
                this.mFilterStats.writeToProto(protoOutputStream, 1146756268039L);
            }
            if (this.mWorkSource != null) {
                this.mWorkSource.writeToProto(protoOutputStream, 1146756268040L);
            }
            protoOutputStream.end(jStart);
        }
    }

    protected static final class FilterStats {
        long aggregateTime;
        public int count;
        long lastTime;
        final BroadcastStats mBroadcastStats;
        final String mTag;
        public int nesting;
        public int numWakeup;
        public long startTime;

        FilterStats(BroadcastStats broadcastStats, String str) {
            this.mBroadcastStats = broadcastStats;
            this.mTag = str;
        }

        public String toString() {
            return "FilterStats{tag=" + this.mTag + ", lastTime=" + this.lastTime + ", aggregateTime=" + this.aggregateTime + ", count=" + this.count + ", numWakeup=" + this.numWakeup + ", startTime=" + this.startTime + ", nesting=" + this.nesting + "}";
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1138166333441L, this.mTag);
            protoOutputStream.write(1112396529666L, this.lastTime);
            protoOutputStream.write(1112396529667L, this.aggregateTime);
            protoOutputStream.write(1120986464260L, this.count);
            protoOutputStream.write(1120986464261L, this.numWakeup);
            protoOutputStream.write(1112396529670L, this.startTime);
            protoOutputStream.write(1120986464263L, this.nesting);
            protoOutputStream.end(jStart);
        }
    }

    protected static final class BroadcastStats {
        long aggregateTime;
        public int count;
        final ArrayMap<String, FilterStats> filterStats = new ArrayMap<>();
        final String mPackageName;
        final int mUid;
        public int nesting;
        public int numWakeup;
        public long startTime;

        BroadcastStats(int i, String str) {
            this.mUid = i;
            this.mPackageName = str;
        }

        public String toString() {
            return "BroadcastStats{uid=" + this.mUid + ", packageName=" + this.mPackageName + ", aggregateTime=" + this.aggregateTime + ", count=" + this.count + ", numWakeup=" + this.numWakeup + ", startTime=" + this.startTime + ", nesting=" + this.nesting + "}";
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1120986464257L, this.mUid);
            protoOutputStream.write(1138166333442L, this.mPackageName);
            protoOutputStream.write(1112396529667L, this.aggregateTime);
            protoOutputStream.write(1120986464260L, this.count);
            protoOutputStream.write(1120986464261L, this.numWakeup);
            protoOutputStream.write(1112396529670L, this.startTime);
            protoOutputStream.write(1120986464263L, this.nesting);
            protoOutputStream.end(jStart);
        }
    }

    @Override
    public void onStart() throws Throwable {
        this.mNativeData = init();
        this.mNextNonWakeup = 0L;
        this.mNextWakeup = 0L;
        setTimeZoneImpl(SystemProperties.get(TIMEZONE_PROPERTY));
        initPpl();
        registerWFDStatusChangeReciever();
        initAlarmGrouping();
        if (this.mNativeData != 0) {
            long jLastModified = Environment.getRootDirectory().lastModified();
            if (System.currentTimeMillis() < jLastModified) {
                Slog.i(TAG, "Current time only " + System.currentTimeMillis() + ", advancing to build time " + jLastModified);
                setKernelTime(this.mNativeData, jLastModified);
            }
        }
        PackageManager packageManager = getContext().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageManager.getPermissionInfo(SYSTEM_UI_SELF_PERMISSION, 0).packageName, 0);
            if ((applicationInfo.privateFlags & 8) != 0) {
                this.mSystemUiUid = applicationInfo.uid;
            } else {
                Slog.e(TAG, "SysUI permission android.permission.systemui.IDENTITY defined by non-privileged app " + applicationInfo.packageName + " - ignoring");
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (this.mSystemUiUid <= 0 && !"1".equals(SystemProperties.get("ro.config.simplelauncher"))) {
            Slog.wtf(TAG, "SysUI package not found!");
        }
        this.mWakeLock = ((PowerManager) getContext().getSystemService("power")).newWakeLock(1, "*alarm*");
        this.mTimeTickSender = PendingIntent.getBroadcastAsUser(getContext(), 0, new Intent("android.intent.action.TIME_TICK").addFlags(1344274432), 0, UserHandle.ALL);
        Intent intent = new Intent("android.intent.action.DATE_CHANGED");
        intent.addFlags(538968064);
        this.mDateChangeSender = PendingIntent.getBroadcastAsUser(getContext(), 0, intent, 67108864, UserHandle.ALL);
        this.mClockReceiver = new ClockReceiver();
        this.mClockReceiver.scheduleTimeTickEvent();
        this.mClockReceiver.scheduleDateChangedEvent();
        this.mInteractiveStateReceiver = new InteractiveStateReceiver();
        this.mUninstallReceiver = new UninstallReceiver();
        if (this.mNativeData != 0) {
            new AlarmThread().start();
        } else {
            Slog.w(TAG, "Failed to open alarm driver. Falling back to a handler.");
        }
        try {
            ActivityManager.getService().registerUidObserver(new UidObserver(), 14, -1, (String) null);
        } catch (RemoteException e2) {
        }
        publishBinderService("alarm", this.mService);
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            this.mConstants.start(getContext().getContentResolver());
            this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
            this.mLocalDeviceIdleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
            this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
            this.mUsageStatsManagerInternal.addAppIdleStateChangeListener(new AppStandbyTracker());
            this.mAppStateTracker = (AppStateTracker) LocalServices.getService(AppStateTracker.class);
            this.mAppStateTracker.addListener(this.mForceAppStandbyListener);
        }
    }

    protected void finalize() throws Throwable {
        try {
            close(this.mNativeData);
        } finally {
            super.finalize();
        }
    }

    boolean setTimeImpl(long j) {
        boolean z;
        if (this.mNativeData == 0 || this.mNativeData == -1) {
            Slog.w(TAG, "Not setting time since no alarm driver is available.");
            return false;
        }
        synchronized (this.mLock) {
            z = setKernelTime(this.mNativeData, j) == 0;
        }
        return z;
    }

    void setTimeZoneImpl(String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        TimeZone timeZone = TimeZone.getTimeZone(str);
        boolean z = false;
        synchronized (this) {
            String str2 = SystemProperties.get(TIMEZONE_PROPERTY);
            if (str2 == null || !str2.equals(timeZone.getID())) {
                if (localLOGV) {
                    Slog.v(TAG, "timezone changed: " + str2 + ", new=" + timeZone.getID());
                }
                z = true;
                SystemProperties.set(TIMEZONE_PROPERTY, timeZone.getID());
            }
            setKernelTimezone(this.mNativeData, -(timeZone.getOffset(System.currentTimeMillis()) / 60000));
        }
        TimeZone.setDefault(null);
        if (z) {
            Intent intent = new Intent("android.intent.action.TIMEZONE_CHANGED");
            intent.addFlags(555745280);
            intent.putExtra("time-zone", timeZone.getID());
            getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    void removeImpl(PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            return;
        }
        synchronized (this.mLock) {
            removeLocked(pendingIntent, null);
        }
    }

    void setImpl(int i, long j, long j2, long j3, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str, int i2, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo, int i3, String str2) throws Throwable {
        long j4;
        long j5;
        ?? r11;
        long j6;
        long j7;
        long j8;
        long j9;
        long j10;
        long jMaxTriggerTime;
        long j11;
        long j12;
        ?? r35;
        Object obj;
        long j13;
        ?? r3;
        ?? r352;
        int i4 = i;
        long j14 = j2;
        if ((pendingIntent == null && iAlarmListener == null) || (pendingIntent != null && iAlarmListener != null)) {
            Slog.w(TAG, "Alarms must either supply a PendingIntent or an AlarmReceiver");
            return;
        }
        if (j14 > AppStandbyController.SettingsObserver.DEFAULT_NOTIFICATION_TIMEOUT) {
            Slog.w(TAG, "Window length " + j14 + "ms suspiciously long; limiting to 1 hour");
            j14 = AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT;
        }
        long j15 = j14;
        long j16 = this.mConstants.MIN_INTERVAL;
        if (j3 > 0 && j3 < j16) {
            Slog.w(TAG, "Suspiciously short interval " + j3 + " millis; expanding to " + (j16 / 1000) + " seconds");
        } else if (j3 > this.mConstants.MAX_INTERVAL) {
            Slog.w(TAG, "Suspiciously long interval " + j3 + " millis; clamping");
            j16 = this.mConstants.MAX_INTERVAL;
        } else {
            j4 = j3;
            if ((i4 >= 0 || i4 > 3) && !isPowerOffAlarmType(i)) {
                throw new IllegalArgumentException("Invalid alarm type " + i4);
            }
            if (j >= 0) {
                Slog.w(TAG, "Invalid alarm trigger time! " + j + " from uid=" + i3 + " pid=" + Binder.getCallingPid());
                j5 = 0L;
            } else {
                j5 = j;
            }
            ?? r23 = j5;
            long j17 = j4;
            if (schedulePoweroffAlarm(i4, j5, j4, pendingIntent, iAlarmListener, str, workSource, alarmClockInfo, str2)) {
                return;
            }
            if (isPowerOffAlarmType(i)) {
                i4 = 0;
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (DEBUG_BATCH) {
                ?? sb = new StringBuilder();
                sb.append("setImpl debug batch ");
                sb.append(pendingIntent);
                sb.append(") : type=");
                sb.append(i4);
                sb.append(" triggerAtTime=");
                ?? r112 = r23;
                sb.append(r112);
                sb.append(" win=");
                j7 = j15;
                sb.append(j7);
                sb.append(" interval=");
                j6 = j17;
                sb.append(j6);
                sb.append(" flags=0x");
                sb.append(Integer.toHexString(i2));
                sb.append("nowElapsed: ");
                sb.append(jElapsedRealtime);
                Slog.d(TAG, sb.toString());
                r11 = r112;
            } else {
                r11 = r23;
                j6 = j17;
                j7 = j15;
            }
            long jConvertToElapsed = convertToElapsed(r11, i4);
            long j18 = this.mConstants.MIN_FUTURITY + jElapsedRealtime;
            if (jConvertToElapsed > j18) {
                j18 = jConvertToElapsed;
            }
            if (supportAlarmGrouping()) {
                long j19 = j18;
                j8 = j6;
                long j20 = j7;
                ?? r353 = r11;
                jMaxTriggerTime = getMaxTriggerTimeforAlarmGrouping(i4, j19, jConvertToElapsed, j7, j8, pendingIntent, null);
                if (j20 < 0) {
                    j9 = j19;
                    j11 = jMaxTriggerTime - j9;
                } else {
                    j9 = j19;
                    j11 = j20;
                }
                r352 = r353;
                if (DEBUG_BATCH) {
                    r352 = r353;
                    if (j11 < 0) {
                        Slog.wtf(TAG, "Negative window=" + j11 + " tElapsed=" + j9 + " maxElapsed=" + jMaxTriggerTime + "nominalTrigger: " + jConvertToElapsed);
                        r352 = r353;
                    }
                }
            } else {
                j8 = j6;
                long j21 = j7;
                ?? r354 = r11;
                j9 = j18;
                if (j21 != 0) {
                    if (j21 < 0) {
                        jMaxTriggerTime = maxTriggerTime(jElapsedRealtime, j9, j8);
                        j11 = jMaxTriggerTime - j9;
                        r352 = r354;
                    } else {
                        j10 = j9 + j21;
                    }
                } else {
                    j10 = j9;
                }
                j12 = j21;
                r35 = r354;
                obj = this.mLock;
                synchronized (obj) {
                    try {
                        try {
                            if (DEBUG_BATCH) {
                                ?? sb2 = new StringBuilder();
                                sb2.append("set(");
                                sb2.append(pendingIntent);
                                sb2.append(") : type=");
                                sb2.append(i4);
                                sb2.append(" triggerAtTime=");
                                ?? r32 = r35;
                                sb2.append(r32);
                                sb2.append(" win=");
                                sb2.append(j12);
                                sb2.append(" tElapsed=");
                                sb2.append(j9);
                                sb2.append(" maxElapsed=");
                                sb2.append(j10);
                                sb2.append(" interval=");
                                j13 = j8;
                                sb2.append(j13);
                                sb2.append(" flags=0x");
                                sb2.append(Integer.toHexString(i2));
                                Slog.v(TAG, sb2.toString());
                                r3 = r32;
                            } else {
                                j13 = j8;
                                r3 = r35;
                            }
                            setImplLocked(i4, r3, j9, j12, j10, j13, pendingIntent, iAlarmListener, str, i2, true, workSource, alarmClockInfo, i3, str2, needAlarmGrouping());
                        } catch (Throwable th) {
                            th = th;
                            r23 = obj;
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                return;
            }
            j10 = jMaxTriggerTime;
            j12 = j11;
            r35 = r352;
            obj = this.mLock;
            synchronized (obj) {
            }
        }
        j4 = j16;
        if (i4 >= 0) {
            throw new IllegalArgumentException("Invalid alarm type " + i4);
        }
        throw new IllegalArgumentException("Invalid alarm type " + i4);
        if (j >= 0) {
        }
        ?? r232 = j5;
        long j172 = j4;
        if (schedulePoweroffAlarm(i4, j5, j4, pendingIntent, iAlarmListener, str, workSource, alarmClockInfo, str2)) {
        }
    }

    private void setImplLocked(int i, long j, long j2, long j3, long j4, long j5, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str, int i2, boolean z, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo, int i3, String str2, boolean z2) {
        Alarm alarm = new Alarm(i, j, j2, j3, j4, j5, pendingIntent, iAlarmListener, str, workSource, i2, alarmClockInfo, i3, str2, z2);
        try {
            if (ActivityManager.getService().isAppStartModeDisabled(i3, str2)) {
                Slog.w(TAG, "Not setting alarm from " + i3 + ":" + alarm + " -- package not allowed to start");
                return;
            }
        } catch (RemoteException e) {
        }
        removeLocked(pendingIntent, iAlarmListener);
        setImplLocked(alarm, false, z);
    }

    private long getMinDelayForBucketLocked(int i) {
        char c;
        if (i == 50) {
            c = 4;
        } else if (i > 30) {
            c = 3;
        } else if (i > 20) {
            c = 2;
        } else {
            c = i > 10 ? (char) 1 : (char) 0;
        }
        return this.mConstants.APP_STANDBY_MIN_DELAYS[c];
    }

    private boolean adjustDeliveryTimeBasedOnStandbyBucketLocked(Alarm alarm) {
        if (isExemptFromAppStandby(alarm)) {
            return false;
        }
        if (this.mAppStandbyParole) {
            if (alarm.whenElapsed <= alarm.expectedWhenElapsed) {
                return false;
            }
            alarm.whenElapsed = alarm.expectedWhenElapsed;
            alarm.maxWhenElapsed = alarm.expectedMaxWhenElapsed;
            return true;
        }
        long j = alarm.whenElapsed;
        long j2 = alarm.maxWhenElapsed;
        String str = alarm.sourcePackage;
        int userId = UserHandle.getUserId(alarm.creatorUid);
        int appStandbyBucket = this.mUsageStatsManagerInternal.getAppStandbyBucket(str, userId, SystemClock.elapsedRealtime());
        long jLongValue = this.mLastAlarmDeliveredForPackage.getOrDefault(Pair.create(str, Integer.valueOf(userId)), 0L).longValue();
        if (jLongValue > 0) {
            long minDelayForBucketLocked = jLongValue + getMinDelayForBucketLocked(appStandbyBucket);
            if (alarm.expectedWhenElapsed < minDelayForBucketLocked) {
                alarm.maxWhenElapsed = minDelayForBucketLocked;
                alarm.whenElapsed = minDelayForBucketLocked;
            } else {
                alarm.whenElapsed = alarm.expectedWhenElapsed;
                alarm.maxWhenElapsed = alarm.expectedMaxWhenElapsed;
            }
        }
        return (j == alarm.whenElapsed && j2 == alarm.maxWhenElapsed) ? false : true;
    }

    private void setImplLocked(Alarm alarm, boolean z, boolean z2) {
        if ((alarm.flags & 16) != 0) {
            if (this.mNextWakeFromIdle != null && alarm.whenElapsed > this.mNextWakeFromIdle.whenElapsed) {
                long j = this.mNextWakeFromIdle.whenElapsed;
                alarm.maxWhenElapsed = j;
                alarm.whenElapsed = j;
                alarm.when = j;
            }
            int iFuzzForDuration = fuzzForDuration(alarm.whenElapsed - SystemClock.elapsedRealtime());
            if (iFuzzForDuration > 0) {
                if (this.mRandom == null) {
                    this.mRandom = new Random();
                }
                alarm.whenElapsed -= (long) this.mRandom.nextInt(iFuzzForDuration);
                long j2 = alarm.whenElapsed;
                alarm.maxWhenElapsed = j2;
                alarm.when = j2;
            }
        } else if (this.mPendingIdleUntil != null && (alarm.flags & 14) == 0) {
            this.mPendingWhileIdleAlarms.add(alarm);
            return;
        }
        adjustDeliveryTimeBasedOnStandbyBucketLocked(alarm);
        insertAndBatchAlarmLocked(alarm);
        boolean z3 = true;
        if (alarm.alarmClock != null) {
            this.mNextAlarmClockMayChange = true;
        }
        if ((alarm.flags & 16) != 0) {
            if (this.mPendingIdleUntil != alarm && this.mPendingIdleUntil != null) {
                Slog.wtfStack(TAG, "setImplLocked: idle until changed from " + this.mPendingIdleUntil + " to " + alarm);
            }
            this.mPendingIdleUntil = alarm;
        } else if ((alarm.flags & 2) != 0 && (this.mNextWakeFromIdle == null || this.mNextWakeFromIdle.whenElapsed > alarm.whenElapsed)) {
            this.mNextWakeFromIdle = alarm;
            if (this.mPendingIdleUntil == null) {
            }
        } else {
            z3 = false;
        }
        if (!z) {
            if (DEBUG_VALIDATE && z2 && !validateConsistencyLocked()) {
                Slog.v(TAG, "Tipping-point operation: type=" + alarm.type + " when=" + alarm.when + " when(hex)=" + Long.toHexString(alarm.when) + " whenElapsed=" + alarm.whenElapsed + " maxWhenElapsed=" + alarm.maxWhenElapsed + " interval=" + alarm.repeatInterval + " op=" + alarm.operation + " flags=0x" + Integer.toHexString(alarm.flags));
                rebatchAllAlarmsLocked(false);
                z3 = false;
            }
            if (z3) {
                rebatchAllAlarmsLocked(false);
            }
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    private final class LocalService implements AlarmManagerInternal {
        private LocalService() {
        }

        @Override
        public void removeAlarmsForUid(int i) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.removeLocked(i);
            }
        }
    }

    protected void dumpWithargs(PrintWriter printWriter, String[] strArr) {
        dumpImpl(printWriter, strArr);
    }

    protected void dumpImpl(PrintWriter printWriter, String[] strArr) {
        int iBinarySearch;
        ArrayMap<String, BroadcastStats> arrayMap;
        int i;
        synchronized (this.mLock) {
            printWriter.println("Current Alarm Manager state:");
            this.mConstants.dump(printWriter);
            printWriter.println();
            if (this.mAppStateTracker != null) {
                this.mAppStateTracker.dump(printWriter, "  ");
                printWriter.println();
            }
            printWriter.println("  App Standby Parole: " + this.mAppStandbyParole);
            printWriter.println();
            long jCurrentTimeMillis = System.currentTimeMillis();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            long jUptimeMillis = SystemClock.uptimeMillis();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            printWriter.print("  nowRTC=");
            printWriter.print(jCurrentTimeMillis);
            printWriter.print("=");
            printWriter.print(simpleDateFormat.format(new Date(jCurrentTimeMillis)));
            printWriter.print(" nowELAPSED=");
            printWriter.print(jElapsedRealtime);
            printWriter.println();
            printWriter.print("  mLastTimeChangeClockTime=");
            printWriter.print(this.mLastTimeChangeClockTime);
            printWriter.print("=");
            printWriter.println(simpleDateFormat.format(new Date(this.mLastTimeChangeClockTime)));
            printWriter.print("  mLastTimeChangeRealtime=");
            printWriter.println(this.mLastTimeChangeRealtime);
            printWriter.print("  mLastTickIssued=");
            printWriter.println(simpleDateFormat.format(new Date(jCurrentTimeMillis - (jElapsedRealtime - this.mLastTickIssued))));
            printWriter.print("  mLastTickReceived=");
            printWriter.println(simpleDateFormat.format(new Date(this.mLastTickReceived)));
            printWriter.print("  mLastTickSet=");
            printWriter.println(simpleDateFormat.format(new Date(this.mLastTickSet)));
            printWriter.print("  mLastTickAdded=");
            printWriter.println(simpleDateFormat.format(new Date(this.mLastTickAdded)));
            printWriter.print("  mLastTickRemoved=");
            printWriter.println(simpleDateFormat.format(new Date(this.mLastTickRemoved)));
            SystemServiceManager systemServiceManager = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
            if (systemServiceManager != null) {
                printWriter.println();
                printWriter.print("  RuntimeStarted=");
                printWriter.print(simpleDateFormat.format(new Date((jCurrentTimeMillis - jElapsedRealtime) + systemServiceManager.getRuntimeStartElapsedTime())));
                if (systemServiceManager.isRuntimeRestarted()) {
                    printWriter.print("  (Runtime restarted)");
                }
                printWriter.println();
                printWriter.print("  Runtime uptime (elapsed): ");
                TimeUtils.formatDuration(jElapsedRealtime, systemServiceManager.getRuntimeStartElapsedTime(), printWriter);
                printWriter.println();
                printWriter.print("  Runtime uptime (uptime): ");
                TimeUtils.formatDuration(jUptimeMillis, systemServiceManager.getRuntimeStartUptime(), printWriter);
                printWriter.println();
            }
            printWriter.println();
            if (!this.mInteractive) {
                printWriter.print("  Time since non-interactive: ");
                TimeUtils.formatDuration(jElapsedRealtime - this.mNonInteractiveStartTime, printWriter);
                printWriter.println();
            }
            printWriter.print("  Max wakeup delay: ");
            TimeUtils.formatDuration(currentNonWakeupFuzzLocked(jElapsedRealtime), printWriter);
            printWriter.println();
            printWriter.print("  Time since last dispatch: ");
            TimeUtils.formatDuration(jElapsedRealtime - this.mLastAlarmDeliveryTime, printWriter);
            printWriter.println();
            printWriter.print("  Next non-wakeup delivery time: ");
            TimeUtils.formatDuration(jElapsedRealtime - this.mNextNonWakeupDeliveryTime, printWriter);
            printWriter.println();
            long j = jCurrentTimeMillis - jElapsedRealtime;
            long j2 = this.mNextWakeup + j;
            long j3 = this.mNextNonWakeup + j;
            printWriter.print("  Next non-wakeup alarm: ");
            TimeUtils.formatDuration(this.mNextNonWakeup, jElapsedRealtime, printWriter);
            printWriter.print(" = ");
            printWriter.print(this.mNextNonWakeup);
            printWriter.print(" = ");
            printWriter.println(simpleDateFormat.format(new Date(j3)));
            printWriter.print("  Next wakeup alarm: ");
            TimeUtils.formatDuration(this.mNextWakeup, jElapsedRealtime, printWriter);
            printWriter.print(" = ");
            printWriter.print(this.mNextWakeup);
            printWriter.print(" = ");
            printWriter.println(simpleDateFormat.format(new Date(j2)));
            printWriter.print("    set at ");
            TimeUtils.formatDuration(this.mLastWakeupSet, jElapsedRealtime, printWriter);
            printWriter.println();
            printWriter.print("  Last wakeup: ");
            TimeUtils.formatDuration(this.mLastWakeup, jElapsedRealtime, printWriter);
            printWriter.print(" = ");
            printWriter.println(this.mLastWakeup);
            printWriter.print("  Last trigger: ");
            TimeUtils.formatDuration(this.mLastTrigger, jElapsedRealtime, printWriter);
            printWriter.print(" = ");
            printWriter.println(this.mLastTrigger);
            printWriter.print("  Num time change events: ");
            printWriter.println(this.mNumTimeChanged);
            printWriter.println();
            printWriter.println("  Next alarm clock information: ");
            TreeSet treeSet = new TreeSet();
            for (int i2 = 0; i2 < this.mNextAlarmClockForUser.size(); i2++) {
                treeSet.add(Integer.valueOf(this.mNextAlarmClockForUser.keyAt(i2)));
            }
            for (int i3 = 0; i3 < this.mPendingSendNextAlarmClockChangedForUser.size(); i3++) {
                treeSet.add(Integer.valueOf(this.mPendingSendNextAlarmClockChangedForUser.keyAt(i3)));
            }
            Iterator it = treeSet.iterator();
            while (it.hasNext()) {
                int iIntValue = ((Integer) it.next()).intValue();
                AlarmManager.AlarmClockInfo alarmClockInfo = this.mNextAlarmClockForUser.get(iIntValue);
                long triggerTime = alarmClockInfo != null ? alarmClockInfo.getTriggerTime() : 0L;
                boolean z = this.mPendingSendNextAlarmClockChangedForUser.get(iIntValue);
                printWriter.print("    user:");
                printWriter.print(iIntValue);
                printWriter.print(" pendingSend:");
                printWriter.print(z);
                printWriter.print(" time:");
                printWriter.print(triggerTime);
                if (triggerTime > 0) {
                    printWriter.print(" = ");
                    printWriter.print(simpleDateFormat.format(new Date(triggerTime)));
                    printWriter.print(" = ");
                    TimeUtils.formatDuration(triggerTime, jCurrentTimeMillis, printWriter);
                }
                printWriter.println();
            }
            if (this.mAlarmBatches.size() > 0) {
                printWriter.println();
                printWriter.print("  Pending alarm batches: ");
                printWriter.println(this.mAlarmBatches.size());
                for (Iterator<Batch> it2 = this.mAlarmBatches.iterator(); it2.hasNext(); it2 = it2) {
                    Batch next = it2.next();
                    printWriter.print(next);
                    printWriter.println(':');
                    dumpAlarmList(printWriter, next.alarms, "    ", jElapsedRealtime, jCurrentTimeMillis, simpleDateFormat);
                }
            }
            printWriter.println();
            printWriter.println("  Pending user blocked background alarms: ");
            boolean z2 = false;
            int i4 = 0;
            while (i4 < this.mPendingBackgroundAlarms.size()) {
                ArrayList<Alarm> arrayListValueAt = this.mPendingBackgroundAlarms.valueAt(i4);
                if (arrayListValueAt == null || arrayListValueAt.size() <= 0) {
                    i = i4;
                } else {
                    i = i4;
                    dumpAlarmList(printWriter, arrayListValueAt, "    ", jElapsedRealtime, jCurrentTimeMillis, simpleDateFormat);
                    z2 = true;
                }
                i4 = i + 1;
            }
            if (!z2) {
                printWriter.println("    none");
            }
            printWriter.println("  mLastAlarmDeliveredForPackage:");
            for (int i5 = 0; i5 < this.mLastAlarmDeliveredForPackage.size(); i5++) {
                Pair<String, Integer> pairKeyAt = this.mLastAlarmDeliveredForPackage.keyAt(i5);
                printWriter.print("    Package " + ((String) pairKeyAt.first) + ", User " + pairKeyAt.second + ":");
                TimeUtils.formatDuration(this.mLastAlarmDeliveredForPackage.valueAt(i5).longValue(), jElapsedRealtime, printWriter);
                printWriter.println();
            }
            printWriter.println();
            if (this.mPendingIdleUntil != null || this.mPendingWhileIdleAlarms.size() > 0) {
                printWriter.println();
                printWriter.println("    Idle mode state:");
                printWriter.print("      Idling until: ");
                if (this.mPendingIdleUntil != null) {
                    printWriter.println(this.mPendingIdleUntil);
                    this.mPendingIdleUntil.dump(printWriter, "        ", jElapsedRealtime, jCurrentTimeMillis, simpleDateFormat);
                } else {
                    printWriter.println("null");
                }
                printWriter.println("      Pending alarms:");
                dumpAlarmList(printWriter, this.mPendingWhileIdleAlarms, "      ", jElapsedRealtime, jCurrentTimeMillis, simpleDateFormat);
            }
            if (this.mNextWakeFromIdle != null) {
                printWriter.println();
                printWriter.print("  Next wake from idle: ");
                printWriter.println(this.mNextWakeFromIdle);
                this.mNextWakeFromIdle.dump(printWriter, "    ", jElapsedRealtime, jCurrentTimeMillis, simpleDateFormat);
            }
            printWriter.println();
            printWriter.print("  Past-due non-wakeup alarms: ");
            if (this.mPendingNonWakeupAlarms.size() > 0) {
                printWriter.println(this.mPendingNonWakeupAlarms.size());
                dumpAlarmList(printWriter, this.mPendingNonWakeupAlarms, "    ", jElapsedRealtime, jCurrentTimeMillis, simpleDateFormat);
            } else {
                printWriter.println("(none)");
            }
            printWriter.print("    Number of delayed alarms: ");
            printWriter.print(this.mNumDelayedAlarms);
            printWriter.print(", total delay time: ");
            TimeUtils.formatDuration(this.mTotalDelayTime, printWriter);
            printWriter.println();
            printWriter.print("    Max delay time: ");
            TimeUtils.formatDuration(this.mMaxDelayTime, printWriter);
            printWriter.print(", max non-interactive time: ");
            TimeUtils.formatDuration(this.mNonInteractiveTime, printWriter);
            printWriter.println();
            printWriter.println();
            printWriter.print("  Broadcast ref count: ");
            printWriter.println(this.mBroadcastRefCount);
            printWriter.print("  PendingIntent send count: ");
            printWriter.println(this.mSendCount);
            printWriter.print("  PendingIntent finish count: ");
            printWriter.println(this.mSendFinishCount);
            printWriter.print("  Listener send count: ");
            printWriter.println(this.mListenerCount);
            printWriter.print("  Listener finish count: ");
            printWriter.println(this.mListenerFinishCount);
            printWriter.println();
            if (this.mInFlight.size() > 0) {
                printWriter.println("Outstanding deliveries:");
                for (int i6 = 0; i6 < this.mInFlight.size(); i6++) {
                    printWriter.print("   #");
                    printWriter.print(i6);
                    printWriter.print(": ");
                    printWriter.println(this.mInFlight.get(i6));
                }
                printWriter.println();
            }
            if (this.mLastAllowWhileIdleDispatch.size() > 0) {
                printWriter.println("  Last allow while idle dispatch times:");
                for (int i7 = 0; i7 < this.mLastAllowWhileIdleDispatch.size(); i7++) {
                    printWriter.print("    UID ");
                    int iKeyAt = this.mLastAllowWhileIdleDispatch.keyAt(i7);
                    UserHandle.formatUid(printWriter, iKeyAt);
                    printWriter.print(": ");
                    long jValueAt = this.mLastAllowWhileIdleDispatch.valueAt(i7);
                    TimeUtils.formatDuration(jValueAt, jElapsedRealtime, printWriter);
                    long whileIdleMinIntervalLocked = getWhileIdleMinIntervalLocked(iKeyAt);
                    printWriter.print("  Next allowed:");
                    TimeUtils.formatDuration(jValueAt + whileIdleMinIntervalLocked, jElapsedRealtime, printWriter);
                    printWriter.print(" (");
                    TimeUtils.formatDuration(whileIdleMinIntervalLocked, 0L, printWriter);
                    printWriter.print(")");
                    printWriter.println();
                }
            }
            printWriter.print("  mUseAllowWhileIdleShortTime: [");
            for (int i8 = 0; i8 < this.mUseAllowWhileIdleShortTime.size(); i8++) {
                if (this.mUseAllowWhileIdleShortTime.valueAt(i8)) {
                    UserHandle.formatUid(printWriter, this.mUseAllowWhileIdleShortTime.keyAt(i8));
                    printWriter.print(" ");
                }
            }
            printWriter.println("]");
            printWriter.println();
            if (this.mLog.dump(printWriter, "  Recent problems", "    ")) {
                printWriter.println();
            }
            FilterStats[] filterStatsArr = new FilterStats[10];
            Comparator<FilterStats> comparator = new Comparator<FilterStats>() {
                @Override
                public int compare(FilterStats filterStats, FilterStats filterStats2) {
                    if (filterStats.aggregateTime < filterStats2.aggregateTime) {
                        return 1;
                    }
                    if (filterStats.aggregateTime > filterStats2.aggregateTime) {
                        return -1;
                    }
                    return 0;
                }
            };
            int i9 = 0;
            int i10 = 0;
            while (i9 < this.mBroadcastStats.size()) {
                ArrayMap<String, BroadcastStats> arrayMapValueAt = this.mBroadcastStats.valueAt(i9);
                int i11 = i10;
                int i12 = 0;
                while (i12 < arrayMapValueAt.size()) {
                    BroadcastStats broadcastStatsValueAt = arrayMapValueAt.valueAt(i12);
                    int i13 = i11;
                    int i14 = 0;
                    while (i14 < broadcastStatsValueAt.filterStats.size()) {
                        FilterStats filterStatsValueAt = broadcastStatsValueAt.filterStats.valueAt(i14);
                        if (i13 > 0) {
                            iBinarySearch = Arrays.binarySearch(filterStatsArr, 0, i13, filterStatsValueAt, comparator);
                        } else {
                            iBinarySearch = 0;
                        }
                        if (iBinarySearch < 0) {
                            iBinarySearch = (-iBinarySearch) - 1;
                        }
                        if (iBinarySearch >= filterStatsArr.length) {
                            arrayMap = arrayMapValueAt;
                        } else {
                            int length = (filterStatsArr.length - iBinarySearch) - 1;
                            if (length > 0) {
                                arrayMap = arrayMapValueAt;
                                System.arraycopy(filterStatsArr, iBinarySearch, filterStatsArr, iBinarySearch + 1, length);
                            } else {
                                arrayMap = arrayMapValueAt;
                            }
                            filterStatsArr[iBinarySearch] = filterStatsValueAt;
                            if (i13 < filterStatsArr.length) {
                                i13++;
                            }
                        }
                        i14++;
                        arrayMapValueAt = arrayMap;
                    }
                    i12++;
                    i11 = i13;
                }
                i9++;
                i10 = i11;
            }
            if (i10 > 0) {
                printWriter.println("  Top Alarms:");
                for (int i15 = 0; i15 < i10; i15++) {
                    FilterStats filterStats = filterStatsArr[i15];
                    printWriter.print("    ");
                    if (filterStats.nesting > 0) {
                        printWriter.print("*ACTIVE* ");
                    }
                    TimeUtils.formatDuration(filterStats.aggregateTime, printWriter);
                    printWriter.print(" running, ");
                    printWriter.print(filterStats.numWakeup);
                    printWriter.print(" wakeups, ");
                    printWriter.print(filterStats.count);
                    printWriter.print(" alarms: ");
                    UserHandle.formatUid(printWriter, filterStats.mBroadcastStats.mUid);
                    printWriter.print(":");
                    printWriter.print(filterStats.mBroadcastStats.mPackageName);
                    printWriter.println();
                    printWriter.print("      ");
                    printWriter.print(filterStats.mTag);
                    printWriter.println();
                }
            }
            printWriter.println(" ");
            printWriter.println("  Alarm Stats:");
            ArrayList arrayList = new ArrayList();
            for (int i16 = 0; i16 < this.mBroadcastStats.size(); i16++) {
                ArrayMap<String, BroadcastStats> arrayMapValueAt2 = this.mBroadcastStats.valueAt(i16);
                for (int i17 = 0; i17 < arrayMapValueAt2.size(); i17++) {
                    BroadcastStats broadcastStatsValueAt2 = arrayMapValueAt2.valueAt(i17);
                    printWriter.print("  ");
                    if (broadcastStatsValueAt2.nesting > 0) {
                        printWriter.print("*ACTIVE* ");
                    }
                    UserHandle.formatUid(printWriter, broadcastStatsValueAt2.mUid);
                    printWriter.print(":");
                    printWriter.print(broadcastStatsValueAt2.mPackageName);
                    printWriter.print(" ");
                    TimeUtils.formatDuration(broadcastStatsValueAt2.aggregateTime, printWriter);
                    printWriter.print(" running, ");
                    printWriter.print(broadcastStatsValueAt2.numWakeup);
                    printWriter.println(" wakeups:");
                    arrayList.clear();
                    for (int i18 = 0; i18 < broadcastStatsValueAt2.filterStats.size(); i18++) {
                        arrayList.add(broadcastStatsValueAt2.filterStats.valueAt(i18));
                    }
                    Collections.sort(arrayList, comparator);
                    for (int i19 = 0; i19 < arrayList.size(); i19++) {
                        FilterStats filterStats2 = (FilterStats) arrayList.get(i19);
                        printWriter.print("    ");
                        if (filterStats2.nesting > 0) {
                            printWriter.print("*ACTIVE* ");
                        }
                        TimeUtils.formatDuration(filterStats2.aggregateTime, printWriter);
                        printWriter.print(" ");
                        printWriter.print(filterStats2.numWakeup);
                        printWriter.print(" wakes ");
                        printWriter.print(filterStats2.count);
                        printWriter.print(" alarms, last ");
                        TimeUtils.formatDuration(filterStats2.lastTime, jElapsedRealtime, printWriter);
                        printWriter.println(":");
                        printWriter.print("      ");
                        printWriter.print(filterStats2.mTag);
                        printWriter.println();
                    }
                }
            }
            printWriter.println();
            this.mStatLogger.dump(printWriter, "  ");
        }
    }

    void dumpProto(FileDescriptor fileDescriptor) {
        int iBinarySearch;
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        synchronized (this.mLock) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            protoOutputStream.write(1112396529665L, jCurrentTimeMillis);
            protoOutputStream.write(1112396529666L, jElapsedRealtime);
            long j = 1112396529667L;
            protoOutputStream.write(1112396529667L, this.mLastTimeChangeClockTime);
            protoOutputStream.write(1112396529668L, this.mLastTimeChangeRealtime);
            this.mConstants.dumpProto(protoOutputStream, 1146756268037L);
            if (this.mAppStateTracker != null) {
                this.mAppStateTracker.dumpProto(protoOutputStream, 1146756268038L);
            }
            protoOutputStream.write(1133871366151L, this.mInteractive);
            if (!this.mInteractive) {
                protoOutputStream.write(1112396529672L, jElapsedRealtime - this.mNonInteractiveStartTime);
                protoOutputStream.write(1112396529673L, currentNonWakeupFuzzLocked(jElapsedRealtime));
                protoOutputStream.write(1112396529674L, jElapsedRealtime - this.mLastAlarmDeliveryTime);
                protoOutputStream.write(1112396529675L, jElapsedRealtime - this.mNextNonWakeupDeliveryTime);
            }
            protoOutputStream.write(1112396529676L, this.mNextNonWakeup - jElapsedRealtime);
            protoOutputStream.write(1112396529677L, this.mNextWakeup - jElapsedRealtime);
            protoOutputStream.write(1112396529678L, jElapsedRealtime - this.mLastWakeup);
            protoOutputStream.write(1112396529679L, jElapsedRealtime - this.mLastWakeupSet);
            protoOutputStream.write(1112396529680L, this.mNumTimeChanged);
            TreeSet treeSet = new TreeSet();
            int size = this.mNextAlarmClockForUser.size();
            for (int i = 0; i < size; i++) {
                treeSet.add(Integer.valueOf(this.mNextAlarmClockForUser.keyAt(i)));
            }
            int size2 = this.mPendingSendNextAlarmClockChangedForUser.size();
            for (int i2 = 0; i2 < size2; i2++) {
                treeSet.add(Integer.valueOf(this.mPendingSendNextAlarmClockChangedForUser.keyAt(i2)));
            }
            Iterator it = treeSet.iterator();
            while (it.hasNext()) {
                int iIntValue = ((Integer) it.next()).intValue();
                AlarmManager.AlarmClockInfo alarmClockInfo = this.mNextAlarmClockForUser.get(iIntValue);
                long triggerTime = alarmClockInfo != null ? alarmClockInfo.getTriggerTime() : 0L;
                boolean z = this.mPendingSendNextAlarmClockChangedForUser.get(iIntValue);
                long jStart = protoOutputStream.start(2246267895826L);
                protoOutputStream.write(1120986464257L, iIntValue);
                protoOutputStream.write(1133871366146L, z);
                protoOutputStream.write(1112396529667L, triggerTime);
                protoOutputStream.end(jStart);
                j = 1112396529667L;
            }
            Iterator<Batch> it2 = this.mAlarmBatches.iterator();
            while (it2.hasNext()) {
                it2.next().writeToProto(protoOutputStream, 2246267895827L, jElapsedRealtime, jCurrentTimeMillis);
                j = 1112396529667L;
            }
            for (int i3 = 0; i3 < this.mPendingBackgroundAlarms.size(); i3++) {
                ArrayList<Alarm> arrayListValueAt = this.mPendingBackgroundAlarms.valueAt(i3);
                if (arrayListValueAt != null) {
                    for (Iterator<Alarm> it3 = arrayListValueAt.iterator(); it3.hasNext(); it3 = it3) {
                        it3.next().writeToProto(protoOutputStream, 2246267895828L, jElapsedRealtime, jCurrentTimeMillis);
                    }
                }
            }
            if (this.mPendingIdleUntil != null) {
                this.mPendingIdleUntil.writeToProto(protoOutputStream, 1146756268053L, jElapsedRealtime, jCurrentTimeMillis);
            }
            Iterator<Alarm> it4 = this.mPendingWhileIdleAlarms.iterator();
            while (it4.hasNext()) {
                it4.next().writeToProto(protoOutputStream, 2246267895830L, jElapsedRealtime, jCurrentTimeMillis);
            }
            if (this.mNextWakeFromIdle != null) {
                this.mNextWakeFromIdle.writeToProto(protoOutputStream, 1146756268055L, jElapsedRealtime, jCurrentTimeMillis);
            }
            Iterator<Alarm> it5 = this.mPendingNonWakeupAlarms.iterator();
            while (it5.hasNext()) {
                it5.next().writeToProto(protoOutputStream, 2246267895832L, jElapsedRealtime, jCurrentTimeMillis);
            }
            protoOutputStream.write(1120986464281L, this.mNumDelayedAlarms);
            protoOutputStream.write(1112396529690L, this.mTotalDelayTime);
            protoOutputStream.write(1112396529691L, this.mMaxDelayTime);
            protoOutputStream.write(1112396529692L, this.mNonInteractiveTime);
            protoOutputStream.write(1120986464285L, this.mBroadcastRefCount);
            protoOutputStream.write(1120986464286L, this.mSendCount);
            protoOutputStream.write(1120986464287L, this.mSendFinishCount);
            protoOutputStream.write(1120986464288L, this.mListenerCount);
            protoOutputStream.write(1120986464289L, this.mListenerFinishCount);
            Iterator<InFlight> it6 = this.mInFlight.iterator();
            while (it6.hasNext()) {
                it6.next().writeToProto(protoOutputStream, 2246267895842L);
            }
            for (int i4 = 0; i4 < this.mLastAllowWhileIdleDispatch.size(); i4++) {
                long jStart2 = protoOutputStream.start(2246267895844L);
                int iKeyAt = this.mLastAllowWhileIdleDispatch.keyAt(i4);
                long jValueAt = this.mLastAllowWhileIdleDispatch.valueAt(i4);
                protoOutputStream.write(1120986464257L, iKeyAt);
                protoOutputStream.write(1112396529666L, jValueAt);
                protoOutputStream.write(1112396529667L, jValueAt + getWhileIdleMinIntervalLocked(iKeyAt));
                protoOutputStream.end(jStart2);
            }
            for (int i5 = 0; i5 < this.mUseAllowWhileIdleShortTime.size(); i5++) {
                if (this.mUseAllowWhileIdleShortTime.valueAt(i5)) {
                    protoOutputStream.write(2220498092067L, this.mUseAllowWhileIdleShortTime.keyAt(i5));
                }
            }
            this.mLog.writeToProto(protoOutputStream, 1146756268069L);
            FilterStats[] filterStatsArr = new FilterStats[10];
            Comparator<FilterStats> comparator = new Comparator<FilterStats>() {
                @Override
                public int compare(FilterStats filterStats, FilterStats filterStats2) {
                    if (filterStats.aggregateTime < filterStats2.aggregateTime) {
                        return 1;
                    }
                    if (filterStats.aggregateTime > filterStats2.aggregateTime) {
                        return -1;
                    }
                    return 0;
                }
            };
            int i6 = 0;
            int i7 = 0;
            while (i6 < this.mBroadcastStats.size()) {
                ArrayMap<String, BroadcastStats> arrayMapValueAt = this.mBroadcastStats.valueAt(i6);
                int i8 = i7;
                int i9 = 0;
                while (i9 < arrayMapValueAt.size()) {
                    BroadcastStats broadcastStatsValueAt = arrayMapValueAt.valueAt(i9);
                    int i10 = i8;
                    for (int i11 = 0; i11 < broadcastStatsValueAt.filterStats.size(); i11++) {
                        FilterStats filterStatsValueAt = broadcastStatsValueAt.filterStats.valueAt(i11);
                        if (i10 > 0) {
                            iBinarySearch = Arrays.binarySearch(filterStatsArr, 0, i10, filterStatsValueAt, comparator);
                        } else {
                            iBinarySearch = 0;
                        }
                        if (iBinarySearch < 0) {
                            iBinarySearch = (-iBinarySearch) - 1;
                        }
                        if (iBinarySearch < filterStatsArr.length) {
                            int length = (filterStatsArr.length - iBinarySearch) - 1;
                            if (length > 0) {
                                System.arraycopy(filterStatsArr, iBinarySearch, filterStatsArr, iBinarySearch + 1, length);
                            }
                            filterStatsArr[iBinarySearch] = filterStatsValueAt;
                            if (i10 < filterStatsArr.length) {
                                i10++;
                            }
                        }
                    }
                    i9++;
                    i8 = i10;
                }
                i6++;
                i7 = i8;
            }
            for (int i12 = 0; i12 < i7; i12++) {
                long jStart3 = protoOutputStream.start(2246267895846L);
                FilterStats filterStats = filterStatsArr[i12];
                protoOutputStream.write(1120986464257L, filterStats.mBroadcastStats.mUid);
                protoOutputStream.write(1138166333442L, filterStats.mBroadcastStats.mPackageName);
                filterStats.writeToProto(protoOutputStream, 1146756268035L);
                protoOutputStream.end(jStart3);
            }
            ArrayList arrayList = new ArrayList();
            for (int i13 = 0; i13 < this.mBroadcastStats.size(); i13++) {
                ArrayMap<String, BroadcastStats> arrayMapValueAt2 = this.mBroadcastStats.valueAt(i13);
                for (int i14 = 0; i14 < arrayMapValueAt2.size(); i14++) {
                    long jStart4 = protoOutputStream.start(2246267895847L);
                    BroadcastStats broadcastStatsValueAt2 = arrayMapValueAt2.valueAt(i14);
                    broadcastStatsValueAt2.writeToProto(protoOutputStream, 1146756268033L);
                    arrayList.clear();
                    for (int i15 = 0; i15 < broadcastStatsValueAt2.filterStats.size(); i15++) {
                        arrayList.add(broadcastStatsValueAt2.filterStats.valueAt(i15));
                    }
                    Collections.sort(arrayList, comparator);
                    Iterator it7 = arrayList.iterator();
                    while (it7.hasNext()) {
                        ((FilterStats) it7.next()).writeToProto(protoOutputStream, 2246267895810L);
                    }
                    protoOutputStream.end(jStart4);
                }
            }
        }
        protoOutputStream.flush();
    }

    private void logBatchesLocked(SimpleDateFormat simpleDateFormat) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);
        PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
        long jCurrentTimeMillis = System.currentTimeMillis();
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        int size = this.mAlarmBatches.size();
        for (int i = 0; i < size; i++) {
            Batch batch = this.mAlarmBatches.get(i);
            printWriter.append((CharSequence) "Batch ");
            printWriter.print(i);
            printWriter.append((CharSequence) ": ");
            printWriter.println(batch);
            dumpAlarmList(printWriter, batch.alarms, "  ", jElapsedRealtime, jCurrentTimeMillis, simpleDateFormat);
            printWriter.flush();
            Slog.v(TAG, byteArrayOutputStream.toString());
            byteArrayOutputStream.reset();
        }
    }

    private boolean validateConsistencyLocked() {
        if (DEBUG_VALIDATE) {
            int size = this.mAlarmBatches.size();
            long j = Long.MIN_VALUE;
            for (int i = 0; i < size; i++) {
                Batch batch = this.mAlarmBatches.get(i);
                if (batch.start >= j) {
                    j = batch.start;
                } else {
                    Slog.e(TAG, "CONSISTENCY FAILURE: Batch " + i + " is out of order");
                    logBatchesLocked(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private Batch findFirstWakeupBatchLocked() {
        int size = this.mAlarmBatches.size();
        for (int i = 0; i < size; i++) {
            Batch batch = this.mAlarmBatches.get(i);
            if (batch.hasWakeups()) {
                return batch;
            }
        }
        return null;
    }

    long getNextWakeFromIdleTimeImpl() {
        long j;
        synchronized (this.mLock) {
            j = this.mNextWakeFromIdle != null ? this.mNextWakeFromIdle.whenElapsed : JobStatus.NO_LATEST_RUNTIME;
        }
        return j;
    }

    AlarmManager.AlarmClockInfo getNextAlarmClockImpl(int i) {
        AlarmManager.AlarmClockInfo alarmClockInfo;
        synchronized (this.mLock) {
            alarmClockInfo = this.mNextAlarmClockForUser.get(i);
        }
        return alarmClockInfo;
    }

    private void updateNextAlarmClockLocked() {
        if (!this.mNextAlarmClockMayChange) {
            return;
        }
        this.mNextAlarmClockMayChange = false;
        SparseArray<AlarmManager.AlarmClockInfo> sparseArray = this.mTmpSparseAlarmClockArray;
        sparseArray.clear();
        int size = this.mAlarmBatches.size();
        for (int i = 0; i < size; i++) {
            ArrayList<Alarm> arrayList = this.mAlarmBatches.get(i).alarms;
            int size2 = arrayList.size();
            for (int i2 = 0; i2 < size2; i2++) {
                Alarm alarm = arrayList.get(i2);
                if (alarm.alarmClock != null) {
                    int userId = UserHandle.getUserId(alarm.uid);
                    AlarmManager.AlarmClockInfo alarmClockInfo = this.mNextAlarmClockForUser.get(userId);
                    if (DEBUG_ALARM_CLOCK) {
                        Log.v(TAG, "Found AlarmClockInfo " + alarm.alarmClock + " at " + formatNextAlarm(getContext(), alarm.alarmClock, userId) + " for user " + userId);
                    }
                    if (sparseArray.get(userId) == null) {
                        sparseArray.put(userId, alarm.alarmClock);
                    } else if (alarm.alarmClock.equals(alarmClockInfo) && alarmClockInfo.getTriggerTime() <= sparseArray.get(userId).getTriggerTime()) {
                        sparseArray.put(userId, alarmClockInfo);
                    }
                }
            }
        }
        int size3 = sparseArray.size();
        for (int i3 = 0; i3 < size3; i3++) {
            AlarmManager.AlarmClockInfo alarmClockInfoValueAt = sparseArray.valueAt(i3);
            int iKeyAt = sparseArray.keyAt(i3);
            if (!alarmClockInfoValueAt.equals(this.mNextAlarmClockForUser.get(iKeyAt))) {
                updateNextAlarmInfoForUserLocked(iKeyAt, alarmClockInfoValueAt);
            }
        }
        for (int size4 = this.mNextAlarmClockForUser.size() - 1; size4 >= 0; size4--) {
            int iKeyAt2 = this.mNextAlarmClockForUser.keyAt(size4);
            if (sparseArray.get(iKeyAt2) == null) {
                updateNextAlarmInfoForUserLocked(iKeyAt2, null);
            }
        }
    }

    private void updateNextAlarmInfoForUserLocked(int i, AlarmManager.AlarmClockInfo alarmClockInfo) {
        if (alarmClockInfo != null) {
            if (DEBUG_ALARM_CLOCK) {
                Log.v(TAG, "Next AlarmClockInfoForUser(" + i + "): " + formatNextAlarm(getContext(), alarmClockInfo, i));
            }
            this.mNextAlarmClockForUser.put(i, alarmClockInfo);
        } else {
            if (DEBUG_ALARM_CLOCK) {
                Log.v(TAG, "Next AlarmClockInfoForUser(" + i + "): None");
            }
            this.mNextAlarmClockForUser.remove(i);
        }
        this.mPendingSendNextAlarmClockChangedForUser.put(i, true);
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessage(2);
    }

    private void sendNextAlarmClockChanged() {
        int i;
        SparseArray<AlarmManager.AlarmClockInfo> sparseArray = this.mHandlerSparseAlarmClockArray;
        sparseArray.clear();
        synchronized (this.mLock) {
            int size = this.mPendingSendNextAlarmClockChangedForUser.size();
            for (int i2 = 0; i2 < size; i2++) {
                int iKeyAt = this.mPendingSendNextAlarmClockChangedForUser.keyAt(i2);
                sparseArray.append(iKeyAt, this.mNextAlarmClockForUser.get(iKeyAt));
            }
            this.mPendingSendNextAlarmClockChangedForUser.clear();
        }
        int size2 = sparseArray.size();
        for (i = 0; i < size2; i++) {
            int iKeyAt2 = sparseArray.keyAt(i);
            Settings.System.putStringForUser(getContext().getContentResolver(), "next_alarm_formatted", formatNextAlarm(getContext(), sparseArray.valueAt(i), iKeyAt2), iKeyAt2);
            getContext().sendBroadcastAsUser(NEXT_ALARM_CLOCK_CHANGED_INTENT, new UserHandle(iKeyAt2));
        }
    }

    private static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo alarmClockInfo, int i) {
        return alarmClockInfo == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context, i) ? "EHm" : "Ehma"), alarmClockInfo.getTriggerTime()).toString();
    }

    void rescheduleKernelAlarmsLocked() {
        long j;
        if (this.mAlarmBatches.size() > 0) {
            Batch batchFindFirstWakeupBatchLocked = findFirstWakeupBatchLocked();
            Batch batch = this.mAlarmBatches.get(0);
            if (batchFindFirstWakeupBatchLocked != null) {
                this.mNextWakeup = batchFindFirstWakeupBatchLocked.start;
                this.mLastWakeupSet = SystemClock.elapsedRealtime();
                setLocked(2, batchFindFirstWakeupBatchLocked.start);
            }
            if (batch != batchFindFirstWakeupBatchLocked) {
                j = batch.start;
            } else {
                j = 0;
            }
        }
        if (this.mPendingNonWakeupAlarms.size() > 0 && (j == 0 || this.mNextNonWakeupDeliveryTime < j)) {
            j = this.mNextNonWakeupDeliveryTime;
        }
        if (j != 0) {
            this.mNextNonWakeup = j;
            setLocked(3, j);
        }
    }

    private void removeLocked(final PendingIntent pendingIntent, final IAlarmListener iAlarmListener) {
        if (pendingIntent == null && iAlarmListener == null) {
            if (localLOGV) {
                Slog.w(TAG, "requested remove() of null operation", new RuntimeException("here"));
                return;
            }
            return;
        }
        Predicate<Alarm> predicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((AlarmManagerService.Alarm) obj).matches(pendingIntent, iAlarmListener);
            }
        };
        boolean z = false;
        boolean zRemove = false;
        for (int size = this.mAlarmBatches.size() - 1; size >= 0; size--) {
            Batch batch = this.mAlarmBatches.get(size);
            zRemove |= batch.remove(predicate);
            if (batch.size() == 0) {
                this.mAlarmBatches.remove(size);
            }
        }
        for (int size2 = this.mPendingWhileIdleAlarms.size() - 1; size2 >= 0; size2--) {
            if (this.mPendingWhileIdleAlarms.get(size2).matches(pendingIntent, iAlarmListener)) {
                this.mPendingWhileIdleAlarms.remove(size2);
            }
        }
        for (int size3 = this.mPendingBackgroundAlarms.size() - 1; size3 >= 0; size3--) {
            ArrayList<Alarm> arrayListValueAt = this.mPendingBackgroundAlarms.valueAt(size3);
            for (int size4 = arrayListValueAt.size() - 1; size4 >= 0; size4--) {
                if (arrayListValueAt.get(size4).matches(pendingIntent, iAlarmListener)) {
                    arrayListValueAt.remove(size4);
                }
            }
            if (arrayListValueAt.size() == 0) {
                this.mPendingBackgroundAlarms.removeAt(size3);
            }
        }
        if (zRemove) {
            if (DEBUG_BATCH) {
                Slog.v(TAG, "remove(operation) changed bounds; rebatching");
            }
            if (this.mPendingIdleUntil != null && this.mPendingIdleUntil.matches(pendingIntent, iAlarmListener)) {
                this.mPendingIdleUntil = null;
                z = true;
            }
            if (this.mNextWakeFromIdle != null && this.mNextWakeFromIdle.matches(pendingIntent, iAlarmListener)) {
                this.mNextWakeFromIdle = null;
            }
            if (this.mAlarmBatches.size() < 300) {
                rebatchAllAlarmsLocked(true);
            } else if (DEBUG_BATCH) {
                Slog.d(TAG, "mAlarmBatches.size() is larger than 300 , do not rebatch");
            }
            if (z) {
                restorePendingWhileIdleAlarmsLocked();
            }
            updateNextAlarmClockLocked();
        }
    }

    void removeLocked(final int i) {
        if (i == 1000) {
            Slog.wtf(TAG, "removeLocked: Shouldn't for UID=" + i);
            return;
        }
        boolean zRemove = false;
        Predicate<Alarm> predicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return AlarmManagerService.lambda$removeLocked$1(i, (AlarmManagerService.Alarm) obj);
            }
        };
        for (int size = this.mAlarmBatches.size() - 1; size >= 0; size--) {
            Batch batch = this.mAlarmBatches.get(size);
            zRemove |= batch.remove(predicate);
            if (batch.size() == 0) {
                this.mAlarmBatches.remove(size);
            }
        }
        for (int size2 = this.mPendingWhileIdleAlarms.size() - 1; size2 >= 0; size2--) {
            if (this.mPendingWhileIdleAlarms.get(size2).uid == i) {
                this.mPendingWhileIdleAlarms.remove(size2);
            }
        }
        for (int size3 = this.mPendingBackgroundAlarms.size() - 1; size3 >= 0; size3--) {
            ArrayList<Alarm> arrayListValueAt = this.mPendingBackgroundAlarms.valueAt(size3);
            for (int size4 = arrayListValueAt.size() - 1; size4 >= 0; size4--) {
                if (arrayListValueAt.get(size4).uid == i) {
                    arrayListValueAt.remove(size4);
                }
            }
            if (arrayListValueAt.size() == 0) {
                this.mPendingBackgroundAlarms.removeAt(size3);
            }
        }
        if (zRemove) {
            if (DEBUG_BATCH) {
                Slog.v(TAG, "remove(uid) changed bounds; rebatching");
            }
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    static boolean lambda$removeLocked$1(int i, Alarm alarm) {
        return alarm.uid == i;
    }

    void removeLocked(final String str) {
        if (str == null) {
            if (localLOGV) {
                Slog.w(TAG, "requested remove() of null packageName", new RuntimeException("here"));
                return;
            }
            return;
        }
        boolean zRemove = false;
        Predicate<Alarm> predicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((AlarmManagerService.Alarm) obj).matches(str);
            }
        };
        boolean zHaveBatchesTimeTickAlarm = haveBatchesTimeTickAlarm(this.mAlarmBatches);
        for (int size = this.mAlarmBatches.size() - 1; size >= 0; size--) {
            Batch batch = this.mAlarmBatches.get(size);
            zRemove |= batch.remove(predicate);
            if (batch.size() == 0) {
                this.mAlarmBatches.remove(size);
            }
        }
        boolean zHaveBatchesTimeTickAlarm2 = haveBatchesTimeTickAlarm(this.mAlarmBatches);
        if (zHaveBatchesTimeTickAlarm != zHaveBatchesTimeTickAlarm2) {
            Slog.wtf(TAG, "removeLocked: hasTick changed from " + zHaveBatchesTimeTickAlarm + " to " + zHaveBatchesTimeTickAlarm2);
        }
        for (int size2 = this.mPendingWhileIdleAlarms.size() - 1; size2 >= 0; size2--) {
            if (this.mPendingWhileIdleAlarms.get(size2).matches(str)) {
                this.mPendingWhileIdleAlarms.remove(size2);
            }
        }
        for (int size3 = this.mPendingBackgroundAlarms.size() - 1; size3 >= 0; size3--) {
            ArrayList<Alarm> arrayListValueAt = this.mPendingBackgroundAlarms.valueAt(size3);
            for (int size4 = arrayListValueAt.size() - 1; size4 >= 0; size4--) {
                if (arrayListValueAt.get(size4).matches(str)) {
                    arrayListValueAt.remove(size4);
                }
            }
            if (arrayListValueAt.size() == 0) {
                this.mPendingBackgroundAlarms.removeAt(size3);
            }
        }
        if (zRemove) {
            if (DEBUG_BATCH) {
                Slog.v(TAG, "remove(package) changed bounds; rebatching");
            }
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    void removeForStoppedLocked(final int i) {
        if (i == 1000) {
            Slog.wtf(TAG, "removeForStoppedLocked: Shouldn't for UID=" + i);
            return;
        }
        boolean zRemove = false;
        Predicate<Alarm> predicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return AlarmManagerService.lambda$removeForStoppedLocked$3(i, (AlarmManagerService.Alarm) obj);
            }
        };
        for (int size = this.mAlarmBatches.size() - 1; size >= 0; size--) {
            Batch batch = this.mAlarmBatches.get(size);
            zRemove |= batch.remove(predicate);
            if (batch.size() == 0) {
                this.mAlarmBatches.remove(size);
            }
        }
        for (int size2 = this.mPendingWhileIdleAlarms.size() - 1; size2 >= 0; size2--) {
            if (this.mPendingWhileIdleAlarms.get(size2).uid == i) {
                this.mPendingWhileIdleAlarms.remove(size2);
            }
        }
        for (int size3 = this.mPendingBackgroundAlarms.size() - 1; size3 >= 0; size3--) {
            if (this.mPendingBackgroundAlarms.keyAt(size3) == i) {
                this.mPendingBackgroundAlarms.removeAt(size3);
            }
        }
        if (zRemove) {
            if (DEBUG_BATCH) {
                Slog.v(TAG, "remove(package) changed bounds; rebatching");
            }
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    static boolean lambda$removeForStoppedLocked$3(int i, Alarm alarm) {
        try {
            if (alarm.uid != i) {
                return false;
            }
            if (ActivityManager.getService().isAppStartModeDisabled(i, alarm.packageName)) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    void removeUserLocked(final int i) {
        if (i == 0) {
            Slog.wtf(TAG, "removeForStoppedLocked: Shouldn't for user=" + i);
            return;
        }
        boolean zRemove = false;
        Predicate<Alarm> predicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return AlarmManagerService.lambda$removeUserLocked$4(i, (AlarmManagerService.Alarm) obj);
            }
        };
        for (int size = this.mAlarmBatches.size() - 1; size >= 0; size--) {
            Batch batch = this.mAlarmBatches.get(size);
            zRemove |= batch.remove(predicate);
            if (batch.size() == 0) {
                this.mAlarmBatches.remove(size);
            }
        }
        for (int size2 = this.mPendingWhileIdleAlarms.size() - 1; size2 >= 0; size2--) {
            if (UserHandle.getUserId(this.mPendingWhileIdleAlarms.get(size2).creatorUid) == i) {
                this.mPendingWhileIdleAlarms.remove(size2);
            }
        }
        for (int size3 = this.mPendingBackgroundAlarms.size() - 1; size3 >= 0; size3--) {
            if (UserHandle.getUserId(this.mPendingBackgroundAlarms.keyAt(size3)) == i) {
                this.mPendingBackgroundAlarms.removeAt(size3);
            }
        }
        for (int size4 = this.mLastAllowWhileIdleDispatch.size() - 1; size4 >= 0; size4--) {
            if (UserHandle.getUserId(this.mLastAllowWhileIdleDispatch.keyAt(size4)) == i) {
                this.mLastAllowWhileIdleDispatch.removeAt(size4);
            }
        }
        if (zRemove) {
            if (DEBUG_BATCH) {
                Slog.v(TAG, "remove(user) changed bounds; rebatching");
            }
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    static boolean lambda$removeUserLocked$4(int i, Alarm alarm) {
        return UserHandle.getUserId(alarm.creatorUid) == i;
    }

    void interactiveStateChangedLocked(boolean z) {
        if (this.mInteractive != z) {
            this.mInteractive = z;
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (z) {
                if (this.mPendingNonWakeupAlarms.size() > 0) {
                    long j = jElapsedRealtime - this.mStartCurrentDelayTime;
                    this.mTotalDelayTime += j;
                    if (this.mMaxDelayTime < j) {
                        this.mMaxDelayTime = j;
                    }
                    deliverAlarmsLocked(this.mPendingNonWakeupAlarms, jElapsedRealtime);
                    this.mPendingNonWakeupAlarms.clear();
                }
                if (this.mNonInteractiveStartTime > 0) {
                    long j2 = jElapsedRealtime - this.mNonInteractiveStartTime;
                    if (j2 > this.mNonInteractiveTime) {
                        this.mNonInteractiveTime = j2;
                        return;
                    }
                    return;
                }
                return;
            }
            this.mNonInteractiveStartTime = jElapsedRealtime;
        }
    }

    boolean lookForPackageLocked(String str) {
        for (int i = 0; i < this.mAlarmBatches.size(); i++) {
            if (this.mAlarmBatches.get(i).hasPackage(str)) {
                return true;
            }
        }
        for (int i2 = 0; i2 < this.mPendingWhileIdleAlarms.size(); i2++) {
            if (this.mPendingWhileIdleAlarms.get(i2).matches(str)) {
                return true;
            }
        }
        return false;
    }

    private void setLocked(int i, long j) {
        long j2;
        long j3 = 0;
        if (this.mNativeData != 0 && this.mNativeData != -1) {
            if (j >= 0) {
                j3 = j / 1000;
                j2 = 1000 * (j % 1000) * 1000;
            } else {
                j2 = 0;
            }
            int i2 = set(this.mNativeData, i, j3, j2);
            if (i2 != 0) {
                Slog.wtf(TAG, "Unable to set kernel alarm, now=" + SystemClock.elapsedRealtime() + " type=" + i + " when=" + j + " @ (" + j3 + "," + j2 + "), ret = " + i2 + " = " + Os.strerror(i2));
                return;
            }
            return;
        }
        Message messageObtain = Message.obtain();
        messageObtain.what = 1;
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageAtTime(messageObtain, j);
    }

    private static final void dumpAlarmList(PrintWriter printWriter, ArrayList<Alarm> arrayList, String str, String str2, long j, long j2, SimpleDateFormat simpleDateFormat) {
        for (int size = arrayList.size() - 1; size >= 0; size += -1) {
            Alarm alarm = arrayList.get(size);
            printWriter.print(str);
            printWriter.print(str2);
            printWriter.print(" #");
            printWriter.print(size);
            printWriter.print(": ");
            printWriter.println(alarm);
            alarm.dump(printWriter, str + "  ", j, j2, simpleDateFormat);
        }
    }

    private static final String labelForType(int i) {
        switch (i) {
            case 0:
                return "RTC_WAKEUP";
            case 1:
                return "RTC";
            case 2:
                return "ELAPSED_WAKEUP";
            case 3:
                return "ELAPSED";
            default:
                return "--unknown--";
        }
    }

    private static final void dumpAlarmList(PrintWriter printWriter, ArrayList<Alarm> arrayList, String str, long j, long j2, SimpleDateFormat simpleDateFormat) {
        for (int size = arrayList.size() - 1; size >= 0; size += -1) {
            Alarm alarm = arrayList.get(size);
            String strLabelForType = labelForType(alarm.type);
            printWriter.print(str);
            printWriter.print(strLabelForType);
            printWriter.print(" #");
            printWriter.print(size);
            printWriter.print(": ");
            printWriter.println(alarm);
            alarm.dump(printWriter, str + "  ", j, j2, simpleDateFormat);
        }
    }

    private boolean isBackgroundRestricted(Alarm alarm) {
        boolean z = (alarm.flags & 4) != 0;
        if (alarm.alarmClock != null) {
            return false;
        }
        if (alarm.operation != null) {
            if (alarm.operation.isActivity()) {
                return false;
            }
            if (alarm.operation.isForegroundService()) {
                z = true;
            }
        }
        return this.mAppStateTracker != null && this.mAppStateTracker.areAlarmsRestricted(alarm.creatorUid, alarm.sourcePackage, z);
    }

    private long getWhileIdleMinIntervalLocked(int i) {
        boolean z = false;
        boolean z2 = this.mPendingIdleUntil != null;
        if (this.mAppStateTracker != null && this.mAppStateTracker.isForceAllAppsStandbyEnabled()) {
            z = true;
        }
        if (!z2 && !z) {
            return this.mConstants.ALLOW_WHILE_IDLE_SHORT_TIME;
        }
        if (z2) {
            return this.mConstants.ALLOW_WHILE_IDLE_LONG_TIME;
        }
        if (this.mUseAllowWhileIdleShortTime.get(i)) {
            return this.mConstants.ALLOW_WHILE_IDLE_SHORT_TIME;
        }
        return this.mConstants.ALLOW_WHILE_IDLE_LONG_TIME;
    }

    boolean triggerAlarmsLocked(ArrayList<Alarm> arrayList, long j, long j2) {
        int i;
        Alarm alarm;
        int i2;
        Batch batch;
        boolean z;
        ?? r1;
        ?? r2;
        Alarm alarm2;
        ?? r15 = this;
        ArrayList<Alarm> arrayList2 = arrayList;
        boolean z2 = false;
        boolean z3 = false;
        while (true) {
            ?? r12 = 1;
            if (r15.mAlarmBatches.size() <= 0) {
                break;
            }
            Batch batch2 = r15.mAlarmBatches.get(z2 ? 1 : 0);
            if (batch2.start > j) {
                break;
            }
            r15.mAlarmBatches.remove(z2 ? 1 : 0);
            int size = batch2.size();
            boolean z4 = z3;
            int i3 = z2 ? 1 : 0;
            ?? r152 = r15;
            while (i3 < size) {
                Alarm alarm3 = batch2.get(i3);
                if ((alarm3.flags & 4) != 0) {
                    long j3 = r152.mLastAllowWhileIdleDispatch.get(alarm3.creatorUid, -1L);
                    long whileIdleMinIntervalLocked = r152.getWhileIdleMinIntervalLocked(alarm3.creatorUid) + j3;
                    if (j3 >= 0 && j < whileIdleMinIntervalLocked) {
                        alarm3.whenElapsed = whileIdleMinIntervalLocked;
                        alarm3.expectedWhenElapsed = whileIdleMinIntervalLocked;
                        if (alarm3.maxWhenElapsed < whileIdleMinIntervalLocked) {
                            alarm3.maxWhenElapsed = whileIdleMinIntervalLocked;
                        }
                        alarm3.expectedMaxWhenElapsed = alarm3.maxWhenElapsed;
                        r152.setImplLocked(alarm3, r12, z2);
                    } else if (r152.isBackgroundRestricted(alarm3)) {
                        if (DEBUG_BG_LIMIT) {
                            Slog.d(TAG, "Deferring alarm " + alarm3 + " due to user forced app standby");
                        }
                        ArrayList<Alarm> arrayList3 = r152.mPendingBackgroundAlarms.get(alarm3.creatorUid);
                        if (arrayList3 == null) {
                            arrayList3 = new ArrayList<>();
                            r152.mPendingBackgroundAlarms.put(alarm3.creatorUid, arrayList3);
                        }
                        arrayList3.add(alarm3);
                    } else {
                        alarm3.count = r12;
                        arrayList2.add(alarm3);
                        if ((alarm3.flags & 2) != 0) {
                            EventLogTags.writeDeviceIdleWakeFromIdle(r152.mPendingIdleUntil != null ? r12 : z2 ? 1 : 0, alarm3.statsTag);
                        }
                        if (r152.mPendingIdleUntil == alarm3) {
                            r152.mPendingIdleUntil = null;
                            r152.rebatchAllAlarmsLocked(z2);
                            restorePendingWhileIdleAlarmsLocked();
                        }
                        if (r152.mNextWakeFromIdle == alarm3) {
                            r152.mNextWakeFromIdle = null;
                            r152.rebatchAllAlarmsLocked(z2);
                        }
                        if (alarm3.repeatInterval > 0) {
                            alarm3.count = (int) (((long) alarm3.count) + ((j - alarm3.expectedWhenElapsed) / alarm3.repeatInterval));
                            long j4 = ((long) alarm3.count) * alarm3.repeatInterval;
                            long j5 = alarm3.whenElapsed + j4;
                            if (supportAlarmGrouping()) {
                                i = i3;
                                i2 = size;
                                batch = batch2;
                                r152.getMaxTriggerTimeforAlarmGrouping(alarm3.type, j5, j5, alarm3.windowLength, alarm3.repeatInterval, alarm3.operation, alarm3);
                                alarm2 = alarm3;
                            } else {
                                i = i3;
                                i2 = size;
                                batch = batch2;
                                alarm2 = alarm3;
                                maxTriggerTime(j, j5, alarm2.repeatInterval);
                            }
                            alarm2.needGrouping = r12;
                            int i4 = alarm2.type;
                            long j6 = alarm2.when + j4;
                            long j7 = alarm2.windowLength;
                            long jMaxTriggerTime = maxTriggerTime(j, j5, alarm2.repeatInterval);
                            long j8 = alarm2.repeatInterval;
                            PendingIntent pendingIntent = alarm2.operation;
                            int i5 = alarm2.flags;
                            WorkSource workSource = alarm2.workSource;
                            AlarmManager.AlarmClockInfo alarmClockInfo = alarm2.alarmClock;
                            int i6 = alarm2.uid;
                            String str = alarm2.packageName;
                            boolean z5 = alarm2.needGrouping;
                            alarm = alarm2;
                            z = z2 ? 1 : 0;
                            r152.setImplLocked(i4, j6, j5, j7, jMaxTriggerTime, j8, pendingIntent, null, null, i5, true, workSource, alarmClockInfo, i6, str, z5);
                        } else {
                            i = i3;
                            alarm = alarm3;
                            i2 = size;
                            batch = batch2;
                            z = z2 ? 1 : 0;
                        }
                        Alarm alarm4 = alarm;
                        if (alarm4.wakeup) {
                            z4 = true;
                        }
                        if (alarm4.alarmClock == null) {
                            r1 = this;
                            r2 = 1;
                        } else {
                            r1 = this;
                            r2 = 1;
                            r1.mNextAlarmClockMayChange = true;
                        }
                    }
                    i = i3;
                    i2 = size;
                    batch = batch2;
                    r2 = r12;
                    z = z2 ? 1 : 0;
                    r1 = r152;
                }
                i3 = i + 1;
                arrayList2 = arrayList;
                r152 = r1;
                r12 = r2;
                size = i2;
                batch2 = batch;
                z2 = z;
            }
            boolean z6 = z2 ? 1 : 0;
            arrayList2 = arrayList;
            z3 = z4;
            r15 = r152;
        }
        boolean z7 = z2 ? 1 : 0;
        ?? r13 = r15;
        r13.mCurrentSeq++;
        calculateDeliveryPriorities(arrayList);
        Collections.sort(arrayList, r13.mAlarmDispatchComparator);
        if (localLOGV) {
            for (int i7 = z7 ? 1 : 0; i7 < arrayList.size(); i7++) {
                Slog.v(TAG, "Triggering alarm #" + i7 + ": " + arrayList.get(i7));
            }
        }
        return z3;
    }

    public static class IncreasingTimeOrder implements Comparator<Alarm> {
        @Override
        public int compare(Alarm alarm, Alarm alarm2) {
            long j = alarm.when;
            long j2 = alarm2.when;
            if (j > j2) {
                return 1;
            }
            if (j < j2) {
                return -1;
            }
            return 0;
        }
    }

    @VisibleForTesting
    public static class Alarm {
        public final AlarmManager.AlarmClockInfo alarmClock;
        public int count;
        public final int creatorUid;
        public long expectedMaxWhenElapsed;
        public long expectedWhenElapsed;
        public final int flags;
        public final IAlarmListener listener;
        public final String listenerTag;
        public long maxWhenElapsed;
        public boolean needGrouping;
        public final PendingIntent operation;
        public final long origWhen;
        public final String packageName;
        public PriorityClass priorityClass;
        public long repeatInterval;
        public final String sourcePackage;
        public final String statsTag;
        public final int type;
        public final int uid;
        public final boolean wakeup;
        public long when;
        public long whenElapsed;
        public long windowLength;
        public final WorkSource workSource;

        public Alarm(int i, long j, long j2, long j3, long j4, long j5, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str, WorkSource workSource, int i2, AlarmManager.AlarmClockInfo alarmClockInfo, int i3, String str2, boolean z) {
            this.type = i;
            this.origWhen = j;
            this.wakeup = i == 2 || i == 0;
            this.when = j;
            this.whenElapsed = j2;
            this.expectedWhenElapsed = j2;
            this.windowLength = j3;
            long jClampPositive = AlarmManagerService.clampPositive(j4);
            this.expectedMaxWhenElapsed = jClampPositive;
            this.maxWhenElapsed = jClampPositive;
            this.repeatInterval = j5;
            this.operation = pendingIntent;
            this.listener = iAlarmListener;
            this.listenerTag = str;
            this.statsTag = makeTag(pendingIntent, str, i);
            this.workSource = workSource;
            this.flags = i2;
            this.alarmClock = alarmClockInfo;
            this.uid = i3;
            this.packageName = str2;
            this.sourcePackage = this.operation != null ? this.operation.getCreatorPackage() : this.packageName;
            this.needGrouping = z;
            this.creatorUid = this.operation != null ? this.operation.getCreatorUid() : this.uid;
        }

        public static String makeTag(PendingIntent pendingIntent, String str, int i) {
            String str2 = (i == 2 || i == 0) ? "*walarm*:" : "*alarm*:";
            if (pendingIntent != null) {
                return pendingIntent.getTag(str2);
            }
            return str2 + str;
        }

        public WakeupEvent makeWakeupEvent(long j) {
            String action;
            int i = this.creatorUid;
            if (this.operation != null) {
                action = this.operation.getIntent().getAction();
            } else {
                action = "<listener>:" + this.listenerTag;
            }
            return new WakeupEvent(j, i, action);
        }

        public boolean matches(PendingIntent pendingIntent, IAlarmListener iAlarmListener) {
            if (this.operation != null) {
                return this.operation.equals(pendingIntent);
            }
            return iAlarmListener != null && this.listener.asBinder().equals(iAlarmListener.asBinder());
        }

        public boolean matches(String str) {
            return str.equals(this.sourcePackage);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Alarm{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" type ");
            sb.append(this.type);
            sb.append(" when ");
            sb.append(this.when);
            sb.append(" ");
            sb.append(this.sourcePackage);
            sb.append('}');
            return sb.toString();
        }

        public void dump(PrintWriter printWriter, String str, long j, long j2, SimpleDateFormat simpleDateFormat) {
            boolean z = true;
            if (this.type != 1 && this.type != 0) {
                z = false;
            }
            printWriter.print(str);
            printWriter.print("tag=");
            printWriter.println(this.statsTag);
            printWriter.print(str);
            printWriter.print("type=");
            printWriter.print(this.type);
            printWriter.print(" expectedWhenElapsed=");
            TimeUtils.formatDuration(this.expectedWhenElapsed, j, printWriter);
            printWriter.print(" expectedMaxWhenElapsed=");
            TimeUtils.formatDuration(this.expectedMaxWhenElapsed, j, printWriter);
            printWriter.print(" whenElapsed=");
            TimeUtils.formatDuration(this.whenElapsed, j, printWriter);
            printWriter.print(" maxWhenElapsed=");
            TimeUtils.formatDuration(this.maxWhenElapsed, j, printWriter);
            printWriter.print(" when=");
            if (z) {
                printWriter.print(simpleDateFormat.format(new Date(this.when)));
            } else {
                TimeUtils.formatDuration(this.when, j, printWriter);
            }
            printWriter.println();
            printWriter.print(str);
            printWriter.print("window=");
            TimeUtils.formatDuration(this.windowLength, printWriter);
            printWriter.print(" repeatInterval=");
            printWriter.print(this.repeatInterval);
            printWriter.print(" count=");
            printWriter.print(this.count);
            printWriter.print(" flags=0x");
            printWriter.println(Integer.toHexString(this.flags));
            if (this.alarmClock != null) {
                printWriter.print(str);
                printWriter.println("Alarm clock:");
                printWriter.print(str);
                printWriter.print("  triggerTime=");
                printWriter.println(simpleDateFormat.format(new Date(this.alarmClock.getTriggerTime())));
                printWriter.print(str);
                printWriter.print("  showIntent=");
                printWriter.println(this.alarmClock.getShowIntent());
            }
            printWriter.print(str);
            printWriter.print("operation=");
            printWriter.println(this.operation);
            if (this.listener != null) {
                printWriter.print(str);
                printWriter.print("listener=");
                printWriter.println(this.listener.asBinder());
            }
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j, long j2, long j3) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1138166333441L, this.statsTag);
            protoOutputStream.write(1159641169922L, this.type);
            protoOutputStream.write(1112396529667L, this.whenElapsed - j2);
            protoOutputStream.write(1112396529668L, this.windowLength);
            protoOutputStream.write(1112396529669L, this.repeatInterval);
            protoOutputStream.write(1120986464262L, this.count);
            protoOutputStream.write(1120986464263L, this.flags);
            if (this.alarmClock != null) {
                this.alarmClock.writeToProto(protoOutputStream, 1146756268040L);
            }
            if (this.operation != null) {
                this.operation.writeToProto(protoOutputStream, 1146756268041L);
            }
            if (this.listener != null) {
                protoOutputStream.write(1138166333450L, this.listener.asBinder().toString());
            }
            protoOutputStream.end(jStart);
        }
    }

    void recordWakeupAlarms(ArrayList<Batch> arrayList, long j, long j2) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            Batch batch = arrayList.get(i);
            if (batch.start <= j) {
                int size2 = batch.alarms.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    this.mRecentWakeups.add(batch.alarms.get(i2).makeWakeupEvent(j2));
                }
            } else {
                return;
            }
        }
    }

    long currentNonWakeupFuzzLocked(long j) {
        long j2 = j - this.mNonInteractiveStartTime;
        if (j2 < BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
            return JobStatus.DEFAULT_TRIGGER_MAX_DELAY;
        }
        if (j2 < BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS) {
            return 900000L;
        }
        return AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT;
    }

    static int fuzzForDuration(long j) {
        if (j < 900000) {
            return (int) j;
        }
        if (j < 5400000) {
            return 900000;
        }
        return 1800000;
    }

    boolean checkAllowNonWakeupDelayLocked(long j) {
        if (this.mInteractive || isWFDConnected() || this.mLastAlarmDeliveryTime <= 0) {
            return false;
        }
        return (this.mPendingNonWakeupAlarms.size() <= 0 || this.mNextNonWakeupDeliveryTime >= j) && j - this.mLastAlarmDeliveryTime <= currentNonWakeupFuzzLocked(j);
    }

    void deliverAlarmsLocked(ArrayList<Alarm> arrayList, long j) {
        this.mLastAlarmDeliveryTime = j;
        resetneedRebatchForRepeatingAlarm();
        MtkDataShaping.openLteGateByDataShaping(arrayList);
        for (int i = 0; i < arrayList.size(); i++) {
            Alarm alarm = arrayList.get(i);
            boolean z = (alarm.flags & 4) != 0;
            if (alarm.wakeup) {
                Trace.traceBegin(131072L, "Dispatch wakeup alarm to " + alarm.packageName);
            } else {
                Trace.traceBegin(131072L, "Dispatch non-wakeup alarm to " + alarm.packageName);
            }
            updatePoweroffAlarmtoNowRtc();
            if (freePplCheck(arrayList, j)) {
                break;
            }
            try {
                if (localLOGV) {
                    Slog.v(TAG, "sending alarm " + alarm);
                }
                updateWakeupAlarmLog(alarm);
                ActivityManager.noteAlarmStart(alarm.operation, alarm.workSource, alarm.uid, alarm.statsTag);
                this.mDeliveryTracker.deliverLocked(alarm, j, z);
            } catch (RuntimeException e) {
                Slog.w(TAG, "Failure sending alarm.", e);
            }
            Trace.traceEnd(131072L);
        }
        if (needRebatchForRepeatingAlarm()) {
            if (localLOGV) {
                Slog.v(TAG, " deliverAlarmsLocked removeInvalidAlarmLocked then rebatch ");
            }
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    private boolean isExemptFromAppStandby(Alarm alarm) {
        return (alarm.alarmClock == null && !UserHandle.isCore(alarm.creatorUid) && (alarm.flags & 8) == 0) ? false : true;
    }

    private class AlarmThread extends Thread {
        public AlarmThread() {
            super(AlarmManagerService.TAG);
        }

        @Override
        public void run() throws Throwable {
            long j;
            long j2;
            ArrayList<Alarm> arrayList = new ArrayList<>();
            while (true) {
                int iWaitForAlarm = AlarmManagerService.this.waitForAlarm(AlarmManagerService.this.mNativeData);
                long jCurrentTimeMillis = System.currentTimeMillis();
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.mLastWakeup = jElapsedRealtime;
                }
                arrayList.clear();
                if ((iWaitForAlarm & 65536) != 0) {
                    synchronized (AlarmManagerService.this.mLock) {
                        j = AlarmManagerService.this.mLastTimeChangeClockTime;
                        j2 = (jElapsedRealtime - AlarmManagerService.this.mLastTimeChangeRealtime) + j;
                    }
                    if (j == 0 || jCurrentTimeMillis < j2 - 1000 || jCurrentTimeMillis > j2 + 1000) {
                        if (AlarmManagerService.DEBUG_BATCH) {
                            Slog.v(AlarmManagerService.TAG, "Time changed notification from kernel; rebatching");
                        }
                        AlarmManagerService.this.removeImpl(AlarmManagerService.this.mTimeTickSender);
                        AlarmManagerService.this.removeImpl(AlarmManagerService.this.mDateChangeSender);
                        AlarmManagerService.this.rebatchAllAlarms();
                        AlarmManagerService.this.mClockReceiver.scheduleTimeTickEvent();
                        AlarmManagerService.this.mClockReceiver.scheduleDateChangedEvent();
                        synchronized (AlarmManagerService.this.mLock) {
                            AlarmManagerService.this.mNumTimeChanged++;
                            AlarmManagerService.this.mLastTimeChangeClockTime = jCurrentTimeMillis;
                            AlarmManagerService.this.mLastTimeChangeRealtime = jElapsedRealtime;
                        }
                        Intent intent = new Intent("android.intent.action.TIME_SET");
                        intent.addFlags(622854144);
                        AlarmManagerService.this.getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
                        iWaitForAlarm |= 5;
                    }
                }
                if (iWaitForAlarm != 65536) {
                    synchronized (AlarmManagerService.this.mLock) {
                        if (AlarmManagerService.localLOGV) {
                            Slog.v(AlarmManagerService.TAG, "Checking for alarms... rtc=" + jCurrentTimeMillis + ", elapsed=" + jElapsedRealtime);
                        }
                        AlarmManagerService.this.mLastTrigger = jElapsedRealtime;
                        if (!AlarmManagerService.this.triggerAlarmsLocked(arrayList, jElapsedRealtime, jCurrentTimeMillis) && AlarmManagerService.this.checkAllowNonWakeupDelayLocked(jElapsedRealtime)) {
                            if (AlarmManagerService.this.mPendingNonWakeupAlarms.size() == 0) {
                                AlarmManagerService.this.mStartCurrentDelayTime = jElapsedRealtime;
                                AlarmManagerService.this.mNextNonWakeupDeliveryTime = jElapsedRealtime + ((AlarmManagerService.this.currentNonWakeupFuzzLocked(jElapsedRealtime) * 3) / 2);
                            }
                            AlarmManagerService.this.mPendingNonWakeupAlarms.addAll(arrayList);
                            AlarmManagerService.this.mNumDelayedAlarms += arrayList.size();
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        } else {
                            if (AlarmManagerService.this.mPendingNonWakeupAlarms.size() > 0) {
                                AlarmManagerService.this.calculateDeliveryPriorities(AlarmManagerService.this.mPendingNonWakeupAlarms);
                                arrayList.addAll(AlarmManagerService.this.mPendingNonWakeupAlarms);
                                Collections.sort(arrayList, AlarmManagerService.this.mAlarmDispatchComparator);
                                long j3 = jElapsedRealtime - AlarmManagerService.this.mStartCurrentDelayTime;
                                AlarmManagerService.this.mTotalDelayTime += j3;
                                if (AlarmManagerService.this.mMaxDelayTime < j3) {
                                    AlarmManagerService.this.mMaxDelayTime = j3;
                                }
                                AlarmManagerService.this.mPendingNonWakeupAlarms.clear();
                            }
                            ArraySet<Pair<String, Integer>> arraySet = new ArraySet<>();
                            for (int i = 0; i < arrayList.size(); i++) {
                                Alarm alarm = arrayList.get(i);
                                if (!AlarmManagerService.this.isExemptFromAppStandby(alarm)) {
                                    arraySet.add(Pair.create(alarm.sourcePackage, Integer.valueOf(UserHandle.getUserId(alarm.creatorUid))));
                                }
                            }
                            AlarmManagerService.this.deliverAlarmsLocked(arrayList, jElapsedRealtime);
                            AlarmManagerService.this.reorderAlarmsBasedOnStandbyBuckets(arraySet);
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        }
                    }
                } else {
                    synchronized (AlarmManagerService.this.mLock) {
                        AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                    }
                }
            }
        }
    }

    protected void setWakelockWorkSource(PendingIntent pendingIntent, WorkSource workSource, int i, String str, int i2, boolean z) {
        try {
            boolean z2 = pendingIntent == this.mTimeTickSender;
            this.mWakeLock.setUnimportantForLogging(z2);
            if (!z && !this.mLastWakeLockUnimportantForLogging) {
                this.mWakeLock.setHistoryTag(null);
            } else {
                this.mWakeLock.setHistoryTag(str);
            }
            this.mLastWakeLockUnimportantForLogging = z2;
        } catch (Exception e) {
        }
        if (workSource != null) {
            this.mWakeLock.setWorkSource(workSource);
            return;
        }
        if (i2 < 0) {
            i2 = ActivityManager.getService().getUidForIntentSender(pendingIntent.getTarget());
        }
        if (i2 >= 0) {
            this.mWakeLock.setWorkSource(new WorkSource(i2));
            return;
        }
        this.mWakeLock.setWorkSource(null);
    }

    private class AlarmHandler extends Handler {
        public static final int ALARM_EVENT = 1;
        public static final int APP_STANDBY_BUCKET_CHANGED = 5;
        public static final int APP_STANDBY_PAROLE_CHANGED = 6;
        public static final int LISTENER_TIMEOUT = 3;
        public static final int REMOVE_FOR_STOPPED = 7;
        public static final int REPORT_ALARMS_ACTIVE = 4;
        public static final int SEND_NEXT_ALARM_CLOCK_CHANGED = 2;

        public AlarmHandler() {
        }

        public void postRemoveForStopped(int i) {
            obtainMessage(7, i, 0).sendToTarget();
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    ArrayList<Alarm> arrayList = new ArrayList<>();
                    synchronized (AlarmManagerService.this.mLock) {
                        AlarmManagerService.this.triggerAlarmsLocked(arrayList, SystemClock.elapsedRealtime(), System.currentTimeMillis());
                        AlarmManagerService.this.updateNextAlarmClockLocked();
                        break;
                    }
                    for (int i = 0; i < arrayList.size(); i++) {
                        Alarm alarm = arrayList.get(i);
                        try {
                            alarm.operation.send();
                        } catch (PendingIntent.CanceledException e) {
                            if (alarm.repeatInterval > 0) {
                                AlarmManagerService.this.removeImpl(alarm.operation);
                            }
                        }
                    }
                    return;
                case 2:
                    AlarmManagerService.this.sendNextAlarmClockChanged();
                    return;
                case 3:
                    AlarmManagerService.this.mDeliveryTracker.alarmTimedOut((IBinder) message.obj);
                    return;
                case 4:
                    if (AlarmManagerService.this.mLocalDeviceIdleController != null) {
                        AlarmManagerService.this.mLocalDeviceIdleController.setAlarmsActive(message.arg1 != 0);
                        return;
                    }
                    return;
                case 5:
                    synchronized (AlarmManagerService.this.mLock) {
                        ArraySet<Pair<String, Integer>> arraySet = new ArraySet<>();
                        arraySet.add(Pair.create((String) message.obj, Integer.valueOf(message.arg1)));
                        if (AlarmManagerService.this.reorderAlarmsBasedOnStandbyBuckets(arraySet)) {
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        }
                        break;
                    }
                    return;
                case 6:
                    synchronized (AlarmManagerService.this.mLock) {
                        AlarmManagerService.this.mAppStandbyParole = ((Boolean) message.obj).booleanValue();
                        if (AlarmManagerService.this.reorderAlarmsBasedOnStandbyBuckets(null)) {
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        }
                        break;
                    }
                    return;
                case 7:
                    synchronized (AlarmManagerService.this.mLock) {
                        AlarmManagerService.this.removeForStoppedLocked(message.arg1);
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    class ClockReceiver extends BroadcastReceiver {
        public ClockReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.TIME_TICK");
            intentFilter.addAction("android.intent.action.DATE_CHANGED");
            AlarmManagerService.this.getContext().registerReceiver(this, intentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) throws Throwable {
            if (intent.getAction().equals("android.intent.action.TIME_TICK")) {
                if (AlarmManagerService.DEBUG_BATCH) {
                    Slog.v(AlarmManagerService.TAG, "Received TIME_TICK alarm; rescheduling");
                }
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.mLastTickReceived = System.currentTimeMillis();
                }
                scheduleTimeTickEvent();
                return;
            }
            if (intent.getAction().equals("android.intent.action.DATE_CHANGED")) {
                AlarmManagerService.this.setKernelTimezone(AlarmManagerService.this.mNativeData, -(TimeZone.getTimeZone(SystemProperties.get(AlarmManagerService.TIMEZONE_PROPERTY)).getOffset(System.currentTimeMillis()) / 60000));
                scheduleDateChangedEvent();
            }
        }

        public void scheduleTimeTickEvent() throws Throwable {
            long jCurrentTimeMillis = System.currentTimeMillis();
            AlarmManagerService.this.setImpl(3, ((60000 * ((jCurrentTimeMillis / 60000) + 1)) - jCurrentTimeMillis) + SystemClock.elapsedRealtime(), 0L, 0L, AlarmManagerService.this.mTimeTickSender, null, null, 1, null, null, Process.myUid(), PackageManagerService.PLATFORM_PACKAGE_NAME);
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.mLastTickSet = jCurrentTimeMillis;
            }
        }

        public void scheduleDateChangedEvent() throws Throwable {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(11, 0);
            calendar.set(12, 0);
            calendar.set(13, 0);
            calendar.set(14, 0);
            calendar.add(5, 1);
            AlarmManagerService.this.setImpl(1, calendar.getTimeInMillis(), 0L, 0L, AlarmManagerService.this.mDateChangeSender, null, null, 1, null, null, Process.myUid(), PackageManagerService.PLATFORM_PACKAGE_NAME);
        }
    }

    class InteractiveStateReceiver extends BroadcastReceiver {
        public InteractiveStateReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            intentFilter.setPriority(1000);
            AlarmManagerService.this.getContext().registerReceiver(this, intentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.interactiveStateChangedLocked("android.intent.action.SCREEN_ON".equals(intent.getAction()));
            }
        }
    }

    class UninstallReceiver extends BroadcastReceiver {
        public UninstallReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
            intentFilter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
            intentFilter.addDataScheme(com.android.server.pm.Settings.ATTR_PACKAGE);
            AlarmManagerService.this.getContext().registerReceiver(this, intentFilter);
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            intentFilter2.addAction("android.intent.action.USER_STOPPED");
            intentFilter2.addAction("android.intent.action.UID_REMOVED");
            AlarmManagerService.this.getContext().registerReceiver(this, intentFilter2);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String schemeSpecificPart;
            int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
            synchronized (AlarmManagerService.this.mLock) {
                String action = intent.getAction();
                String[] stringArrayExtra = null;
                int i = 0;
                if ("android.intent.action.QUERY_PACKAGE_RESTART".equals(action)) {
                    String[] stringArrayExtra2 = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                    int length = stringArrayExtra2.length;
                    while (i < length) {
                        if (!AlarmManagerService.this.lookForPackageLocked(stringArrayExtra2[i])) {
                            i++;
                        } else {
                            setResultCode(-1);
                            return;
                        }
                    }
                    return;
                }
                if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                    stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    int intExtra2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (intExtra2 >= 0) {
                        AlarmManagerService.this.removeUserLocked(intExtra2);
                        for (int size = AlarmManagerService.this.mLastAlarmDeliveredForPackage.size() - 1; size >= 0; size--) {
                            if (((Integer) ((Pair) AlarmManagerService.this.mLastAlarmDeliveredForPackage.keyAt(size)).second).intValue() == intExtra2) {
                                AlarmManagerService.this.mLastAlarmDeliveredForPackage.removeAt(size);
                            }
                        }
                    }
                } else if ("android.intent.action.UID_REMOVED".equals(action)) {
                    if (intExtra >= 0) {
                        AlarmManagerService.this.mLastAllowWhileIdleDispatch.delete(intExtra);
                        AlarmManagerService.this.mUseAllowWhileIdleShortTime.delete(intExtra);
                    }
                } else {
                    if ("android.intent.action.PACKAGE_REMOVED".equals(action) && intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        return;
                    }
                    Uri data = intent.getData();
                    if (data != null && (schemeSpecificPart = data.getSchemeSpecificPart()) != null) {
                        stringArrayExtra = new String[]{schemeSpecificPart};
                    }
                }
                if (stringArrayExtra != null && stringArrayExtra.length > 0) {
                    for (int size2 = AlarmManagerService.this.mLastAlarmDeliveredForPackage.size() - 1; size2 >= 0; size2--) {
                        Pair pair = (Pair) AlarmManagerService.this.mLastAlarmDeliveredForPackage.keyAt(size2);
                        if (ArrayUtils.contains(stringArrayExtra, (String) pair.first) && ((Integer) pair.second).intValue() == UserHandle.getUserId(intExtra)) {
                            AlarmManagerService.this.mLastAlarmDeliveredForPackage.removeAt(size2);
                        }
                    }
                    int length2 = stringArrayExtra.length;
                    while (i < length2) {
                        String str = stringArrayExtra[i];
                        if (intExtra >= 0) {
                            if (intExtra == 1000 && AlarmManagerService.DEBUG_ALARM_CLOCK) {
                                Slog.d(AlarmManagerService.TAG, "removeLocked: intent" + intent);
                                Slog.d(AlarmManagerService.TAG, "package:" + str);
                            }
                            AlarmManagerService.this.removeLocked(intExtra);
                        } else {
                            AlarmManagerService.this.removeLocked(str);
                        }
                        AlarmManagerService.this.mPriorities.remove(str);
                        for (int size3 = AlarmManagerService.this.mBroadcastStats.size() - 1; size3 >= 0; size3--) {
                            ArrayMap<String, BroadcastStats> arrayMapValueAt = AlarmManagerService.this.mBroadcastStats.valueAt(size3);
                            if (arrayMapValueAt.remove(str) != null && arrayMapValueAt.size() <= 0) {
                                AlarmManagerService.this.mBroadcastStats.removeAt(size3);
                            }
                        }
                        i++;
                    }
                }
            }
        }
    }

    final class UidObserver extends IUidObserver.Stub {
        UidObserver() {
        }

        public void onUidStateChanged(int i, int i2, long j) {
        }

        public void onUidGone(int i, boolean z) {
            if (z) {
                AlarmManagerService.this.mHandler.postRemoveForStopped(i);
            }
        }

        public void onUidActive(int i) {
        }

        public void onUidIdle(int i, boolean z) {
            if (z) {
                AlarmManagerService.this.mHandler.postRemoveForStopped(i);
            }
        }

        public void onUidCachedChanged(int i, boolean z) {
        }
    }

    final class AppStandbyTracker extends UsageStatsManagerInternal.AppIdleStateChangeListener {
        AppStandbyTracker() {
        }

        public void onAppIdleStateChanged(String str, int i, boolean z, int i2, int i3) {
            if (AlarmManagerService.DEBUG_STANDBY) {
                Slog.d(AlarmManagerService.TAG, "Package " + str + " for user " + i + " now in bucket " + i2);
            }
            AlarmManagerService.this.mHandler.removeMessages(5);
            AlarmManagerService.this.mHandler.obtainMessage(5, i, -1, str).sendToTarget();
        }

        public void onParoleStateChanged(boolean z) {
            if (AlarmManagerService.DEBUG_STANDBY) {
                StringBuilder sb = new StringBuilder();
                sb.append("Global parole state now ");
                sb.append(z ? "ON" : "OFF");
                Slog.d(AlarmManagerService.TAG, sb.toString());
            }
            AlarmManagerService.this.mHandler.removeMessages(5);
            AlarmManagerService.this.mHandler.removeMessages(6);
            AlarmManagerService.this.mHandler.obtainMessage(6, Boolean.valueOf(z)).sendToTarget();
        }
    }

    private final BroadcastStats getStatsLocked(PendingIntent pendingIntent) {
        return getStatsLocked(pendingIntent.getCreatorUid(), pendingIntent.getCreatorPackage());
    }

    private final BroadcastStats getStatsLocked(int i, String str) {
        ArrayMap<String, BroadcastStats> arrayMap = this.mBroadcastStats.get(i);
        if (arrayMap == null) {
            arrayMap = new ArrayMap<>();
            this.mBroadcastStats.put(i, arrayMap);
        }
        BroadcastStats broadcastStats = arrayMap.get(str);
        if (broadcastStats == null) {
            BroadcastStats broadcastStats2 = new BroadcastStats(i, str);
            arrayMap.put(str, broadcastStats2);
            return broadcastStats2;
        }
        return broadcastStats;
    }

    class DeliveryTracker extends IAlarmCompleteListener.Stub implements PendingIntent.OnFinished {
        DeliveryTracker() {
        }

        private InFlight removeLocked(PendingIntent pendingIntent, Intent intent) {
            for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                if (AlarmManagerService.this.mInFlight.get(i).mPendingIntent == pendingIntent) {
                    return AlarmManagerService.this.mInFlight.remove(i);
                }
            }
            AlarmManagerService.this.mLog.w("No in-flight alarm for " + pendingIntent + " " + intent);
            return null;
        }

        private InFlight removeLocked(IBinder iBinder) {
            for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                if (AlarmManagerService.this.mInFlight.get(i).mListener == iBinder) {
                    return AlarmManagerService.this.mInFlight.remove(i);
                }
            }
            AlarmManagerService.this.mLog.w("No in-flight alarm for listener " + iBinder);
            return null;
        }

        private void updateStatsLocked(InFlight inFlight) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            BroadcastStats broadcastStats = inFlight.mBroadcastStats;
            broadcastStats.nesting--;
            if (broadcastStats.nesting <= 0) {
                broadcastStats.nesting = 0;
                broadcastStats.aggregateTime += jElapsedRealtime - broadcastStats.startTime;
            }
            FilterStats filterStats = inFlight.mFilterStats;
            filterStats.nesting--;
            if (filterStats.nesting <= 0) {
                filterStats.nesting = 0;
                filterStats.aggregateTime += jElapsedRealtime - filterStats.startTime;
            }
            ActivityManager.noteAlarmFinish(inFlight.mPendingIntent, inFlight.mWorkSource, inFlight.mUid, inFlight.mTag);
        }

        private void updateTrackingLocked(InFlight inFlight) {
            if (inFlight != null) {
                updateStatsLocked(inFlight);
            }
            AlarmManagerService alarmManagerService = AlarmManagerService.this;
            alarmManagerService.mBroadcastRefCount--;
            if (AlarmManagerService.DEBUG_WAKELOCK) {
                Slog.d(AlarmManagerService.TAG, "mBroadcastRefCount -> " + AlarmManagerService.this.mBroadcastRefCount);
            }
            if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                AlarmManagerService.this.mHandler.obtainMessage(4, 0).sendToTarget();
                AlarmManagerService.this.mWakeLock.release();
                if (AlarmManagerService.this.mInFlight.size() > 0) {
                    AlarmManagerService.this.mLog.w("Finished all dispatches with " + AlarmManagerService.this.mInFlight.size() + " remaining inflights");
                    for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                        AlarmManagerService.this.mLog.w("  Remaining #" + i + ": " + AlarmManagerService.this.mInFlight.get(i));
                    }
                    AlarmManagerService.this.mInFlight.clear();
                    return;
                }
                return;
            }
            if (AlarmManagerService.this.mInFlight.size() > 0) {
                InFlight inFlight2 = AlarmManagerService.this.mInFlight.get(0);
                AlarmManagerService.this.setWakelockWorkSource(inFlight2.mPendingIntent, inFlight2.mWorkSource, inFlight2.mAlarmType, inFlight2.mTag, -1, false);
            } else {
                AlarmManagerService.this.mLog.w("Alarm wakelock still held but sent queue empty");
                AlarmManagerService.this.mWakeLock.setWorkSource(null);
            }
        }

        public void alarmComplete(IBinder iBinder) {
            if (iBinder == null) {
                AlarmManagerService.this.mLog.w("Invalid alarmComplete: uid=" + Binder.getCallingUid() + " pid=" + Binder.getCallingPid());
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.mHandler.removeMessages(3, iBinder);
                    InFlight inFlightRemoveLocked = removeLocked(iBinder);
                    if (inFlightRemoveLocked != null) {
                        if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                            Slog.i(AlarmManagerService.TAG, "alarmComplete() from " + iBinder);
                        }
                        updateTrackingLocked(inFlightRemoveLocked);
                        AlarmManagerService.access$2008(AlarmManagerService.this);
                    } else if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Late alarmComplete() from " + iBinder);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String str, Bundle bundle) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.access$2108(AlarmManagerService.this);
                updateTrackingLocked(removeLocked(pendingIntent, intent));
            }
        }

        public void alarmTimedOut(IBinder iBinder) {
            synchronized (AlarmManagerService.this.mLock) {
                InFlight inFlightRemoveLocked = removeLocked(iBinder);
                if (inFlightRemoveLocked != null) {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Alarm listener " + iBinder + " timed out in delivery");
                    }
                    updateTrackingLocked(inFlightRemoveLocked);
                    AlarmManagerService.access$2008(AlarmManagerService.this);
                } else {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Spurious timeout of listener " + iBinder);
                    }
                    AlarmManagerService.this.mLog.w("Spurious timeout of listener " + iBinder);
                }
            }
        }

        @GuardedBy("mLock")
        public void deliverLocked(Alarm alarm, long j, boolean z) {
            InFlight inFlight;
            Alarm alarm2;
            if (alarm.operation != null) {
                AlarmManagerService.access$2208(AlarmManagerService.this);
                if (alarm.priorityClass.priority == 0) {
                    AlarmManagerService.this.mLastTickIssued = j;
                }
                try {
                    alarm.operation.send(AlarmManagerService.this.getContext(), 0, AlarmManagerService.this.mBackgroundIntent.putExtra("android.intent.extra.ALARM_COUNT", alarm.count), AlarmManagerService.this.mDeliveryTracker, AlarmManagerService.this.mHandler, null, z ? AlarmManagerService.this.mIdleOptions : null);
                } catch (PendingIntent.CanceledException e) {
                    if (alarm.operation == AlarmManagerService.this.mTimeTickSender) {
                        Slog.wtf(AlarmManagerService.TAG, "mTimeTickSender canceled");
                    }
                    if (alarm.repeatInterval > 0) {
                        AlarmManagerService.this.removeInvalidAlarmLocked(alarm.operation, alarm.listener);
                    }
                    AlarmManagerService.access$2108(AlarmManagerService.this);
                    return;
                }
            } else {
                AlarmManagerService.access$2408(AlarmManagerService.this);
                try {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.v(AlarmManagerService.TAG, "Alarm to uid=" + alarm.uid + " listener=" + alarm.listener.asBinder());
                    }
                    alarm.listener.doAlarm(this);
                    AlarmManagerService.this.mHandler.sendMessageDelayed(AlarmManagerService.this.mHandler.obtainMessage(3, alarm.listener.asBinder()), AlarmManagerService.this.mConstants.LISTENER_TIMEOUT);
                } catch (Exception e2) {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Alarm undeliverable to listener " + alarm.listener.asBinder(), e2);
                    }
                    AlarmManagerService.access$2008(AlarmManagerService.this);
                    return;
                }
            }
            if (AlarmManagerService.DEBUG_WAKELOCK) {
                Slog.d(AlarmManagerService.TAG, "mBroadcastRefCount -> " + (AlarmManagerService.this.mBroadcastRefCount + 1));
            }
            if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                AlarmManagerService.this.setWakelockWorkSource(alarm.operation, alarm.workSource, alarm.type, alarm.statsTag, alarm.operation == null ? alarm.uid : -1, true);
                AlarmManagerService.this.mWakeLock.acquire();
                AlarmManagerService.this.mHandler.obtainMessage(4, 1).sendToTarget();
            }
            InFlight inFlight2 = new InFlight(AlarmManagerService.this, alarm.operation, alarm.listener, alarm.workSource, alarm.uid, alarm.packageName, alarm.type, alarm.statsTag, j);
            AlarmManagerService.this.mInFlight.add(inFlight2);
            AlarmManagerService.this.mBroadcastRefCount++;
            if (z) {
                inFlight = inFlight2;
                alarm2 = alarm;
                AlarmManagerService.this.mLastAllowWhileIdleDispatch.put(alarm2.creatorUid, j);
                if (AlarmManagerService.this.mAppStateTracker == null || AlarmManagerService.this.mAppStateTracker.isUidInForeground(alarm2.creatorUid)) {
                    AlarmManagerService.this.mUseAllowWhileIdleShortTime.put(alarm2.creatorUid, true);
                } else {
                    AlarmManagerService.this.mUseAllowWhileIdleShortTime.put(alarm2.creatorUid, false);
                }
            } else {
                inFlight = inFlight2;
                alarm2 = alarm;
            }
            if (!AlarmManagerService.this.isExemptFromAppStandby(alarm2)) {
                AlarmManagerService.this.mLastAlarmDeliveredForPackage.put(Pair.create(alarm2.sourcePackage, Integer.valueOf(UserHandle.getUserId(alarm2.creatorUid))), Long.valueOf(j));
            }
            BroadcastStats broadcastStats = inFlight.mBroadcastStats;
            broadcastStats.count++;
            if (broadcastStats.nesting != 0) {
                broadcastStats.nesting++;
            } else {
                broadcastStats.nesting = 1;
                broadcastStats.startTime = j;
            }
            FilterStats filterStats = inFlight.mFilterStats;
            filterStats.count++;
            if (filterStats.nesting != 0) {
                filterStats.nesting++;
            } else {
                filterStats.nesting = 1;
                filterStats.startTime = j;
            }
            if (alarm2.type == 2 || alarm2.type == 0) {
                broadcastStats.numWakeup++;
                filterStats.numWakeup++;
                ActivityManager.noteWakeupAlarm(alarm2.operation, alarm2.workSource, alarm2.uid, alarm2.packageName, alarm2.statsTag);
            }
        }
    }

    private class ShellCmd extends ShellCommand {
        private ShellCmd() {
        }

        IAlarmManager getBinderService() {
            return IAlarmManager.Stub.asInterface(AlarmManagerService.this.mService);
        }

        public int onCommand(String str) {
            byte b;
            if (str == null) {
                return handleDefaultCommands(str);
            }
            PrintWriter outPrintWriter = getOutPrintWriter();
            try {
                int iHashCode = str.hashCode();
                if (iHashCode != 1369384280) {
                    b = (iHashCode == 2023087364 && str.equals("set-timezone")) ? (byte) 1 : (byte) -1;
                } else if (str.equals("set-time")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        if (getBinderService().setTime(Long.parseLong(getNextArgRequired()))) {
                        }
                        break;
                    case 1:
                        getBinderService().setTimeZone(getNextArgRequired());
                        break;
                }
            } catch (Exception e) {
                outPrintWriter.println(e);
                return -1;
            }
            return handleDefaultCommands(str);
        }

        public void onHelp() {
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("Alarm manager service (alarm) commands:");
            outPrintWriter.println("  help");
            outPrintWriter.println("    Print this help text.");
            outPrintWriter.println("  set-time TIME");
            outPrintWriter.println("    Set the system clock time to TIME where TIME is milliseconds");
            outPrintWriter.println("    since the Epoch.");
            outPrintWriter.println("  set-timezone TZ");
            outPrintWriter.println("    Set the system timezone to TZ where TZ is an Olson id.");
        }
    }
}
