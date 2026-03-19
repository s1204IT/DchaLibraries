package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.IAlarmCompleteListener;
import android.app.IAlarmListener;
import android.app.IAlarmManager;
import android.app.IUidObserver;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.ArrayMap;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import com.android.internal.util.LocalLog;
import com.android.server.DeviceIdleController;
import com.android.server.am.ProcessList;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.usage.UnixCalendar;
import com.mediatek.amplus.AlarmManagerPlus;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.datashaping.DataShapingUtils;
import com.mediatek.datashaping.IDataShapingManager;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
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

class AlarmManagerService extends SystemService {
    static final int ALARM_EVENT = 1;
    static final String ClockReceiver_TAG = "ClockReceiver";
    static final boolean DEBUG_ALARM_CLOCK;
    static boolean DEBUG_BATCH = false;
    static final boolean DEBUG_LISTENER_CALLBACK;
    static boolean DEBUG_VALIDATE = false;
    private static final int ELAPSED_REALTIME_MASK = 8;
    private static final int ELAPSED_REALTIME_WAKEUP_MASK = 4;
    static final int IS_WAKEUP_MASK = 5;
    static final long MIN_FUZZABLE_INTERVAL = 10000;
    private static final Intent NEXT_ALARM_CLOCK_CHANGED_INTENT;
    static final int PRIO_NORMAL = 2;
    static final int PRIO_TICK = 0;
    static final int PRIO_WAKEUP = 1;
    static final boolean RECORD_ALARMS_IN_HISTORY = true;
    static final boolean RECORD_DEVICE_IDLE_ALARMS = false;
    private static final int RTC_MASK = 2;
    private static final int RTC_WAKEUP_MASK = 1;
    static final String TAG = "AlarmManager";
    static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    static final int TIME_CHANGED_MASK = 65536;
    static final int TYPE_NONWAKEUP_MASK = 1;
    static final boolean WAKEUP_STATS = false;
    static boolean localLOGV = false;
    private static int mAlarmMode;
    private static boolean mSupportAlarmGrouping;
    static final BatchTimeOrder sBatchOrder;
    static final IncreasingTimeOrder sIncreasingTimeOrder;
    final long RECENT_WAKEUP_PERIOD;
    private IDataShapingManager dataShapingManager;
    final ArrayList<Batch> mAlarmBatches;
    final Comparator<Alarm> mAlarmDispatchComparator;
    private ArrayList<String> mAlarmIconPackageList;
    final ArrayList<IdleDispatchEntry> mAllowWhileIdleDispatches;
    long mAllowWhileIdleMinTime;
    private AlarmManagerPlus mAmPlus;
    AppOpsManager mAppOps;
    private final Intent mBackgroundIntent;
    int mBroadcastRefCount;
    final SparseArray<ArrayMap<String, BroadcastStats>> mBroadcastStats;
    ClockReceiver mClockReceiver;
    final Constants mConstants;
    int mCurrentSeq;
    private boolean mDMEnable;
    private Object mDMLock;
    private DMReceiver mDMReceiver;
    PendingIntent mDateChangeSender;
    final DeliveryTracker mDeliveryTracker;
    int[] mDeviceIdleUserWhitelist;
    private ArrayList<PendingIntent> mDmFreeList;
    private ArrayList<Alarm> mDmResendList;
    final AlarmHandler mHandler;
    private final SparseArray<AlarmManager.AlarmClockInfo> mHandlerSparseAlarmClockArray;
    private boolean mIPOShutdown;
    Bundle mIdleOptions;
    ArrayList<InFlight> mInFlight;
    boolean mInteractive;
    InteractiveStateReceiver mInteractiveStateReceiver;
    long mLastAlarmDeliveryTime;
    final SparseLongArray mLastAllowWhileIdleDispatch;
    long mLastTimeChangeClockTime;
    long mLastTimeChangeRealtime;
    boolean mLastWakeLockUnimportantForLogging;
    private long mLastWakeup;
    private long mLastWakeupSet;
    DeviceIdleController.LocalService mLocalDeviceIdleController;
    final Object mLock;
    final LocalLog mLog;
    long mMaxDelayTime;
    long mNativeData;
    private boolean mNeedGrouping;
    boolean mNeedRebatchForRepeatingAlarm;
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
    private boolean mPPLEnable;
    Alarm mPendingIdleUntil;
    ArrayList<Alarm> mPendingNonWakeupAlarms;
    private final SparseBooleanArray mPendingSendNextAlarmClockChangedForUser;
    ArrayList<Alarm> mPendingWhileIdleAlarms;
    private Object mPowerOffAlarmLock;
    private final ArrayList<Alarm> mPoweroffAlarms;
    final HashMap<String, PriorityClass> mPriorities;
    Random mRandom;
    final LinkedList<WakeupEvent> mRecentWakeups;
    private final IBinder mService;
    long mStartCurrentDelayTime;
    PendingIntent mTimeTickSender;
    private final SparseArray<AlarmManager.AlarmClockInfo> mTmpSparseAlarmClockArray;
    long mTotalDelayTime;
    private UninstallReceiver mUninstallReceiver;
    private Object mWaitThreadlock;
    PowerManager.WakeLock mWakeLock;

    private native boolean bootFromAlarm(int i);

    private native void close(long j);

    private native long init();

    private native void set(long j, int i, long j2, long j3);

    private native int setKernelTime(long j, long j2);

    private native int setKernelTimezone(long j, int i);

    private native int waitForAlarm(long j);

    static {
        DEBUG_BATCH = localLOGV;
        DEBUG_VALIDATE = localLOGV;
        DEBUG_ALARM_CLOCK = localLOGV;
        DEBUG_LISTENER_CALLBACK = localLOGV;
        sIncreasingTimeOrder = new IncreasingTimeOrder();
        NEXT_ALARM_CLOCK_CHANGED_INTENT = new Intent("android.app.action.NEXT_ALARM_CLOCK_CHANGED").addFlags(536870912);
        mAlarmMode = 2;
        mSupportAlarmGrouping = false;
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
        private static final long DEFAULT_MIN_FUTURITY = 5000;
        private static final long DEFAULT_MIN_INTERVAL = 60000;
        private static final String KEY_ALLOW_WHILE_IDLE_LONG_TIME = "allow_while_idle_long_time";
        private static final String KEY_ALLOW_WHILE_IDLE_SHORT_TIME = "allow_while_idle_short_time";
        private static final String KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION = "allow_while_idle_whitelist_duration";
        private static final String KEY_LISTENER_TIMEOUT = "listener_timeout";
        private static final String KEY_MIN_FUTURITY = "min_futurity";
        private static final String KEY_MIN_INTERVAL = "min_interval";
        public long ALLOW_WHILE_IDLE_LONG_TIME;
        public long ALLOW_WHILE_IDLE_SHORT_TIME;
        public long ALLOW_WHILE_IDLE_WHITELIST_DURATION;
        public long LISTENER_TIMEOUT;
        public long MIN_FUTURITY;
        public long MIN_INTERVAL;
        private long mLastAllowWhileIdleWhitelistDuration;
        private final KeyValueListParser mParser;
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            this.MIN_FUTURITY = DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
            this.MIN_INTERVAL = DEFAULT_MIN_INTERVAL;
            this.ALLOW_WHILE_IDLE_SHORT_TIME = DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
            this.ALLOW_WHILE_IDLE_LONG_TIME = DEFAULT_ALLOW_WHILE_IDLE_LONG_TIME;
            this.ALLOW_WHILE_IDLE_WHITELIST_DURATION = 10000L;
            this.LISTENER_TIMEOUT = DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
            this.mParser = new KeyValueListParser(',');
            this.mLastAllowWhileIdleWhitelistDuration = -1L;
            updateAllowWhileIdleMinTimeLocked();
            updateAllowWhileIdleWhitelistDurationLocked();
        }

        public void start(ContentResolver resolver) {
            this.mResolver = resolver;
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("alarm_manager_constants"), false, this);
            updateConstants();
        }

        public void updateAllowWhileIdleMinTimeLocked() {
            AlarmManagerService.this.mAllowWhileIdleMinTime = AlarmManagerService.this.mPendingIdleUntil != null ? this.ALLOW_WHILE_IDLE_LONG_TIME : this.ALLOW_WHILE_IDLE_SHORT_TIME;
        }

        public void updateAllowWhileIdleWhitelistDurationLocked() {
            if (this.mLastAllowWhileIdleWhitelistDuration == this.ALLOW_WHILE_IDLE_WHITELIST_DURATION) {
                return;
            }
            this.mLastAllowWhileIdleWhitelistDuration = this.ALLOW_WHILE_IDLE_WHITELIST_DURATION;
            BroadcastOptions opts = BroadcastOptions.makeBasic();
            opts.setTemporaryAppWhitelistDuration(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION);
            AlarmManagerService.this.mIdleOptions = opts.toBundle();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (AlarmManagerService.this.mLock) {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, "alarm_manager_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(AlarmManagerService.TAG, "Bad device idle settings", e);
                }
                this.MIN_FUTURITY = this.mParser.getLong(KEY_MIN_FUTURITY, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                this.MIN_INTERVAL = this.mParser.getLong(KEY_MIN_INTERVAL, DEFAULT_MIN_INTERVAL);
                this.ALLOW_WHILE_IDLE_SHORT_TIME = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_SHORT_TIME, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                this.ALLOW_WHILE_IDLE_LONG_TIME = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_LONG_TIME, DEFAULT_ALLOW_WHILE_IDLE_LONG_TIME);
                this.ALLOW_WHILE_IDLE_WHITELIST_DURATION = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION, 10000L);
                this.LISTENER_TIMEOUT = this.mParser.getLong(KEY_LISTENER_TIMEOUT, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                updateAllowWhileIdleMinTimeLocked();
                updateAllowWhileIdleWhitelistDurationLocked();
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings:");
            pw.print("    ");
            pw.print(KEY_MIN_FUTURITY);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_FUTURITY, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_INTERVAL);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_INTERVAL, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LISTENER_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LISTENER_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_ALLOW_WHILE_IDLE_SHORT_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_SHORT_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_ALLOW_WHILE_IDLE_LONG_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_LONG_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION, pw);
            pw.println();
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

        public WakeupEvent(long theTime, int theUid, String theAction) {
            this.when = theTime;
            this.uid = theUid;
            this.action = theAction;
        }
    }

    final class Batch {
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

        Batch(Alarm seed) {
            this.alarms = new ArrayList<>();
            this.start = seed.whenElapsed;
            this.end = seed.maxWhenElapsed;
            this.flags = seed.flags;
            this.alarms.add(seed);
        }

        int size() {
            return this.alarms.size();
        }

        Alarm get(int index) {
            return this.alarms.get(index);
        }

        boolean canHold(long whenElapsed, long maxWhen) {
            return this.end >= whenElapsed && this.start <= maxWhen;
        }

        boolean add(Alarm alarm) {
            boolean newStart = false;
            int index = Collections.binarySearch(this.alarms, alarm, AlarmManagerService.sIncreasingTimeOrder);
            if (index < 0) {
                index = (0 - index) - 1;
            }
            this.alarms.add(index, alarm);
            if (AlarmManagerService.DEBUG_BATCH) {
                Slog.v(AlarmManagerService.TAG, "Adding " + alarm + " to " + this);
            }
            if (alarm.whenElapsed > this.start) {
                this.start = alarm.whenElapsed;
                newStart = true;
            }
            if (alarm.maxWhenElapsed < this.end) {
                this.end = alarm.maxWhenElapsed;
            }
            this.flags |= alarm.flags;
            if (AlarmManagerService.DEBUG_BATCH) {
                Slog.v(AlarmManagerService.TAG, "    => now " + this);
            }
            return newStart;
        }

        boolean remove(PendingIntent operation, IAlarmListener listener) {
            if (operation == null && listener == null) {
                if (AlarmManagerService.localLOGV) {
                    Slog.w(AlarmManagerService.TAG, "requested remove() of null operation", new RuntimeException("here"));
                    return false;
                }
                return false;
            }
            boolean didRemove = false;
            long newStart = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int newFlags = 0;
            int i = 0;
            while (i < this.alarms.size()) {
                Alarm alarm = this.alarms.get(i);
                if (alarm.matches(operation, listener)) {
                    this.alarms.remove(i);
                    didRemove = true;
                    if (alarm.alarmClock != null) {
                        AlarmManagerService.this.mNextAlarmClockMayChange = true;
                    }
                } else {
                    if (alarm.whenElapsed > newStart) {
                        newStart = alarm.whenElapsed;
                    }
                    if (alarm.maxWhenElapsed < newEnd) {
                        newEnd = alarm.maxWhenElapsed;
                    }
                    newFlags |= alarm.flags;
                    i++;
                }
            }
            if (didRemove) {
                this.start = newStart;
                this.end = newEnd;
                this.flags = newFlags;
            }
            return didRemove;
        }

        boolean remove(String packageName) {
            if (packageName == null) {
                if (AlarmManagerService.localLOGV) {
                    Slog.w(AlarmManagerService.TAG, "requested remove() of null packageName", new RuntimeException("here"));
                }
                return false;
            }
            boolean didRemove = false;
            long newStart = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int newFlags = 0;
            for (int i = this.alarms.size() - 1; i >= 0; i--) {
                Alarm alarm = this.alarms.get(i);
                if (alarm.matches(packageName)) {
                    this.alarms.remove(i);
                    didRemove = true;
                    if (alarm.alarmClock != null) {
                        AlarmManagerService.this.mNextAlarmClockMayChange = true;
                    }
                } else {
                    if (alarm.whenElapsed > newStart) {
                        newStart = alarm.whenElapsed;
                    }
                    if (alarm.maxWhenElapsed < newEnd) {
                        newEnd = alarm.maxWhenElapsed;
                    }
                    newFlags |= alarm.flags;
                }
            }
            if (didRemove) {
                this.start = newStart;
                this.end = newEnd;
                this.flags = newFlags;
            }
            return didRemove;
        }

        boolean removeForStopped(int uid) {
            boolean didRemove = false;
            long newStart = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int newFlags = 0;
            for (int i = this.alarms.size() - 1; i >= 0; i--) {
                Alarm alarm = this.alarms.get(i);
                try {
                    if (alarm.uid == uid && ActivityManagerNative.getDefault().getAppStartMode(uid, alarm.packageName) == 2) {
                        this.alarms.remove(i);
                        didRemove = true;
                        if (alarm.alarmClock != null) {
                            AlarmManagerService.this.mNextAlarmClockMayChange = true;
                        }
                    } else {
                        if (alarm.whenElapsed > newStart) {
                            newStart = alarm.whenElapsed;
                        }
                        if (alarm.maxWhenElapsed < newEnd) {
                            newEnd = alarm.maxWhenElapsed;
                        }
                        newFlags |= alarm.flags;
                    }
                } catch (RemoteException e) {
                }
            }
            if (didRemove) {
                this.start = newStart;
                this.end = newEnd;
                this.flags = newFlags;
            }
            return didRemove;
        }

        boolean remove(int userHandle) {
            boolean didRemove = false;
            long newStart = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int i = 0;
            while (i < this.alarms.size()) {
                Alarm alarm = this.alarms.get(i);
                if (UserHandle.getUserId(alarm.creatorUid) == userHandle) {
                    this.alarms.remove(i);
                    didRemove = true;
                    if (alarm.alarmClock != null) {
                        AlarmManagerService.this.mNextAlarmClockMayChange = true;
                    }
                } else {
                    if (alarm.whenElapsed > newStart) {
                        newStart = alarm.whenElapsed;
                    }
                    if (alarm.maxWhenElapsed < newEnd) {
                        newEnd = alarm.maxWhenElapsed;
                    }
                    i++;
                }
            }
            if (didRemove) {
                this.start = newStart;
                this.end = newEnd;
            }
            return didRemove;
        }

        boolean hasPackage(String packageName) {
            int N = this.alarms.size();
            for (int i = 0; i < N; i++) {
                Alarm a = this.alarms.get(i);
                if (a.matches(packageName)) {
                    return true;
                }
            }
            return false;
        }

        boolean hasWakeups() {
            int N = this.alarms.size();
            for (int i = 0; i < N; i++) {
                Alarm a = this.alarms.get(i);
                if ((a.type & 1) == 0) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder b = new StringBuilder(40);
            b.append("Batch{");
            b.append(Integer.toHexString(hashCode()));
            b.append(" num=");
            b.append(size());
            b.append(" start=");
            b.append(this.start);
            b.append(" end=");
            b.append(this.end);
            if (this.flags != 0) {
                b.append(" flgs=0x");
                b.append(Integer.toHexString(this.flags));
            }
            b.append('}');
            return b.toString();
        }
    }

    static class BatchTimeOrder implements Comparator<Batch> {
        BatchTimeOrder() {
        }

        @Override
        public int compare(Batch b1, Batch b2) {
            long when1 = b1.start;
            long when2 = b2.start;
            if (when1 > when2) {
                return 1;
            }
            if (when1 < when2) {
                return -1;
            }
            return 0;
        }
    }

    void calculateDeliveryPriorities(ArrayList<Alarm> alarms) {
        int alarmPrio;
        String alarmPackage;
        int N = alarms.size();
        for (int i = 0; i < N; i++) {
            Alarm a = alarms.get(i);
            if (a.operation != null && "android.intent.action.TIME_TICK".equals(a.operation.getIntent().getAction())) {
                alarmPrio = 0;
            } else if (a.wakeup) {
                alarmPrio = 1;
            } else {
                alarmPrio = 2;
            }
            PriorityClass packagePrio = a.priorityClass;
            if (a.operation != null) {
                alarmPackage = a.operation.getCreatorPackage();
            } else {
                alarmPackage = a.packageName;
            }
            if (packagePrio == null) {
                packagePrio = this.mPriorities.get(alarmPackage);
            }
            if (packagePrio == null) {
                packagePrio = new PriorityClass();
                a.priorityClass = packagePrio;
                this.mPriorities.put(alarmPackage, packagePrio);
            }
            a.priorityClass = packagePrio;
            if (packagePrio.seq != this.mCurrentSeq) {
                packagePrio.priority = alarmPrio;
                packagePrio.seq = this.mCurrentSeq;
            } else if (alarmPrio < packagePrio.priority) {
                packagePrio.priority = alarmPrio;
            }
        }
    }

    public AlarmManagerService(Context context) {
        super(context);
        this.mBackgroundIntent = new Intent().addFlags(4);
        this.mLog = new LocalLog(TAG);
        this.mLock = new Object();
        this.mBroadcastRefCount = 0;
        this.mPendingNonWakeupAlarms = new ArrayList<>();
        this.mInFlight = new ArrayList<>();
        this.mHandler = new AlarmHandler();
        this.mDeliveryTracker = new DeliveryTracker();
        this.mInteractive = true;
        this.mNeedRebatchForRepeatingAlarm = false;
        this.mDeviceIdleUserWhitelist = new int[0];
        this.mLastAllowWhileIdleDispatch = new SparseLongArray();
        this.mAllowWhileIdleDispatches = new ArrayList<>();
        this.mNextAlarmClockForUser = new SparseArray<>();
        this.mTmpSparseAlarmClockArray = new SparseArray<>();
        this.mPendingSendNextAlarmClockChangedForUser = new SparseBooleanArray();
        this.mHandlerSparseAlarmClockArray = new SparseArray<>();
        this.mDMReceiver = null;
        this.mDMEnable = true;
        this.mPPLEnable = true;
        this.mDMLock = new Object();
        this.mDmFreeList = null;
        this.mAlarmIconPackageList = null;
        this.mDmResendList = null;
        this.mNeedGrouping = true;
        this.mPriorities = new HashMap<>();
        this.mCurrentSeq = 0;
        this.mRecentWakeups = new LinkedList<>();
        this.RECENT_WAKEUP_PERIOD = UnixCalendar.DAY_IN_MILLIS;
        this.mAlarmDispatchComparator = new Comparator<Alarm>() {
            @Override
            public int compare(Alarm lhs, Alarm rhs) {
                if (lhs.priorityClass.priority < rhs.priorityClass.priority) {
                    return -1;
                }
                if (lhs.priorityClass.priority > rhs.priorityClass.priority) {
                    return 1;
                }
                if (lhs.whenElapsed < rhs.whenElapsed) {
                    return -1;
                }
                return lhs.whenElapsed > rhs.whenElapsed ? 1 : 0;
            }
        };
        this.mAlarmBatches = new ArrayList<>();
        this.mPendingIdleUntil = null;
        this.mNextWakeFromIdle = null;
        this.mPendingWhileIdleAlarms = new ArrayList<>();
        this.mWaitThreadlock = new Object();
        this.mIPOShutdown = false;
        this.mPowerOffAlarmLock = new Object();
        this.mPoweroffAlarms = new ArrayList<>();
        this.mBroadcastStats = new SparseArray<>();
        this.mNumDelayedAlarms = 0;
        this.mTotalDelayTime = 0L;
        this.mMaxDelayTime = 0L;
        this.mService = new IAlarmManager.Stub() {
            public void set(String callingPackage, int type, long triggerAtTime, long windowLength, long interval, int flags, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock) {
                int callingUid = Binder.getCallingUid();
                AlarmManagerService.this.mAppOps.checkPackage(callingUid, callingPackage);
                if (interval != 0 && directReceiver != null) {
                    throw new IllegalArgumentException("Repeating alarms cannot use AlarmReceivers");
                }
                if (workSource != null) {
                    AlarmManagerService.this.getContext().enforcePermission("android.permission.UPDATE_DEVICE_STATS", Binder.getCallingPid(), callingUid, "AlarmManager.set");
                }
                int flags2 = flags & (-11);
                if (callingUid != 1000) {
                    flags2 &= -17;
                }
                if (windowLength == 0) {
                    flags2 |= 1;
                }
                if (alarmClock != null) {
                    flags2 |= 3;
                } else if (workSource == null && (callingUid < 10000 || Arrays.binarySearch(AlarmManagerService.this.mDeviceIdleUserWhitelist, UserHandle.getAppId(callingUid)) >= 0)) {
                    flags2 = (flags2 | 8) & (-5);
                }
                AlarmManagerService.this.setImpl(type, triggerAtTime, windowLength, interval, operation, directReceiver, listenerTag, flags2, workSource, alarmClock, callingUid, callingPackage);
            }

            public boolean setTime(long millis) {
                boolean z;
                AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "setTime");
                if (AlarmManagerService.this.mNativeData == 0 || AlarmManagerService.this.mNativeData == -1) {
                    Slog.w(AlarmManagerService.TAG, "Not setting time since no alarm driver is available.");
                    return false;
                }
                synchronized (AlarmManagerService.this.mLock) {
                    Slog.d(AlarmManagerService.TAG, "setKernelTime  setTime = " + millis);
                    z = AlarmManagerService.this.setKernelTime(AlarmManagerService.this.mNativeData, millis) == 0;
                }
                return z;
            }

            public void setTimeZone(String tz) {
                AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME_ZONE", "setTimeZone");
                long oldId = Binder.clearCallingIdentity();
                try {
                    AlarmManagerService.this.setTimeZoneImpl(tz);
                } finally {
                    Binder.restoreCallingIdentity(oldId);
                }
            }

            public void remove(PendingIntent operation, IAlarmListener listener) {
                if (operation == null && listener == null) {
                    Slog.w(AlarmManagerService.TAG, "remove() with no intent or listener");
                    return;
                }
                synchronized (AlarmManagerService.this.mLock) {
                    Slog.d(AlarmManagerService.TAG, "manual remove option = " + operation);
                    AlarmManagerService.this.removeLocked(operation, listener);
                }
            }

            public long getNextWakeFromIdleTime() {
                return AlarmManagerService.this.getNextWakeFromIdleTimeImpl();
            }

            public void cancelPoweroffAlarm(String name) {
                AlarmManagerService.this.cancelPoweroffAlarmImpl(name);
            }

            public void removeFromAms(String packageName) {
                AlarmManagerService.this.removeFromAmsImpl(packageName);
            }

            public boolean lookForPackageFromAms(String packageName) {
                return AlarmManagerService.this.lookForPackageFromAmsImpl(packageName);
            }

            public AlarmManager.AlarmClockInfo getNextAlarmClock(int userId) {
                return AlarmManagerService.this.getNextAlarmClockImpl(ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "getNextAlarmClock", null));
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                String opt;
                if (AlarmManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                    pw.println("Permission Denial: can't dump AlarmManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                    return;
                }
                int opti = 0;
                while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
                    opti++;
                    if ("-h".equals(opt)) {
                        pw.println("alarm manager dump options:");
                        pw.println("  log  [on/off]");
                        pw.println("  Example:");
                        pw.println("  $adb shell dumpsys alarm log on");
                        pw.println("  $adb shell dumpsys alarm log off");
                        return;
                    }
                    pw.println("Unknown argument: " + opt + "; use -h for help");
                }
                if (opti < args.length) {
                    String cmd = args[opti];
                    int opti2 = opti + 1;
                    if ("log".equals(cmd)) {
                        AlarmManagerService.this.configLogTag(pw, args, opti2);
                        return;
                    }
                }
                AlarmManagerService.this.dumpImpl(pw, args);
            }
        };
        this.mConstants = new Constants(this.mHandler);
    }

    static long convertToElapsed(long when, int type) {
        boolean isRtc = true;
        if (type != 1 && type != 0) {
            isRtc = false;
        }
        if (isRtc) {
            return when - (System.currentTimeMillis() - SystemClock.elapsedRealtime());
        }
        return when;
    }

    static long maxTriggerTime(long now, long triggerAtTime, long interval) {
        long futurity = interval == 0 ? triggerAtTime - now : interval;
        if (futurity < 10000) {
            futurity = 0;
        }
        return ((long) (futurity * 0.75d)) + triggerAtTime;
    }

    static boolean addBatchLocked(ArrayList<Batch> list, Batch newBatch) {
        int index = Collections.binarySearch(list, newBatch, sBatchOrder);
        if (index < 0) {
            index = (0 - index) - 1;
        }
        list.add(index, newBatch);
        return index == 0;
    }

    int attemptCoalesceLocked(long whenElapsed, long maxWhen) {
        int N = this.mAlarmBatches.size();
        for (int i = 0; i < N; i++) {
            Batch b = this.mAlarmBatches.get(i);
            if (mSupportAlarmGrouping && this.mAmPlus != null) {
                if (b.canHold(whenElapsed, maxWhen)) {
                    return i;
                }
            } else if ((b.flags & 1) == 0 && b.canHold(whenElapsed, maxWhen) && b.canHold(whenElapsed, maxWhen)) {
                return i;
            }
        }
        return -2;
    }

    void rebatchAllAlarms() {
        synchronized (this.mLock) {
            rebatchAllAlarmsLocked(true);
        }
    }

    void rebatchAllAlarmsLocked(boolean doValidate) {
        ArrayList<Batch> oldSet = (ArrayList) this.mAlarmBatches.clone();
        this.mAlarmBatches.clear();
        Alarm oldPendingIdleUntil = this.mPendingIdleUntil;
        long nowElapsed = SystemClock.elapsedRealtime();
        int oldBatches = oldSet.size();
        if (DEBUG_BATCH) {
            Slog.d(TAG, "rebatchAllAlarmsLocked begin oldBatches count = " + oldBatches);
        }
        for (int batchNum = 0; batchNum < oldBatches; batchNum++) {
            Batch batch = oldSet.get(batchNum);
            int N = batch.size();
            if (DEBUG_BATCH) {
                Slog.d(TAG, "rebatchAllAlarmsLocked  batch.size() = " + batch.size());
            }
            for (int i = 0; i < N; i++) {
                reAddAlarmLocked(batch.get(i), nowElapsed, doValidate);
            }
        }
        if (oldPendingIdleUntil != null && oldPendingIdleUntil != this.mPendingIdleUntil) {
            Slog.wtf(TAG, "Rebatching: idle until changed from " + oldPendingIdleUntil + " to " + this.mPendingIdleUntil);
            if (this.mPendingIdleUntil == null) {
                restorePendingWhileIdleAlarmsLocked();
            }
        }
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
    }

    void reAddAlarmLocked(Alarm a, long nowElapsed, boolean doValidate) {
        long maxElapsed;
        a.when = a.origWhen;
        long whenElapsed = convertToElapsed(a.when, a.type);
        if (mSupportAlarmGrouping && this.mAmPlus != null) {
            maxElapsed = this.mAmPlus.getMaxTriggerTime(a.type, whenElapsed, a.windowLength, a.repeatInterval, a.operation, mAlarmMode, true);
            if (maxElapsed < 0) {
                maxElapsed = 0 - maxElapsed;
                a.needGrouping = false;
            } else {
                a.needGrouping = true;
            }
        } else if (a.windowLength == 0) {
            maxElapsed = whenElapsed;
        } else if (a.windowLength < 0) {
            maxElapsed = maxTriggerTime(nowElapsed, whenElapsed, a.repeatInterval);
            a.windowLength = maxElapsed - whenElapsed;
        } else {
            maxElapsed = whenElapsed + a.windowLength;
        }
        a.whenElapsed = whenElapsed;
        a.maxWhenElapsed = maxElapsed;
        if (DEBUG_BATCH) {
            Slog.d(TAG, "reAddAlarmLocked a.whenElapsed  = " + a.whenElapsed + " a.maxWhenElapsed = " + a.maxWhenElapsed);
        }
        setImplLocked(a, true, doValidate);
    }

    void restorePendingWhileIdleAlarmsLocked() {
        if (this.mPendingWhileIdleAlarms.size() > 0) {
            ArrayList<Alarm> alarms = this.mPendingWhileIdleAlarms;
            this.mPendingWhileIdleAlarms = new ArrayList<>();
            long nowElapsed = SystemClock.elapsedRealtime();
            for (int i = alarms.size() - 1; i >= 0; i--) {
                Alarm a = alarms.get(i);
                reAddAlarmLocked(a, nowElapsed, false);
            }
        }
        this.mConstants.updateAllowWhileIdleMinTimeLocked();
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
        try {
            this.mTimeTickSender.send();
        } catch (PendingIntent.CanceledException e) {
        }
    }

    static final class InFlight {
        final int mAlarmType;
        final BroadcastStats mBroadcastStats;
        final FilterStats mFilterStats;
        final IBinder mListener;
        final PendingIntent mPendingIntent;
        final String mTag;
        final int mUid;
        final WorkSource mWorkSource;

        InFlight(AlarmManagerService service, PendingIntent pendingIntent, IAlarmListener listener, WorkSource workSource, int uid, String alarmPkg, int alarmType, String tag, long nowELAPSED) {
            BroadcastStats statsLocked;
            this.mPendingIntent = pendingIntent;
            this.mListener = listener != null ? listener.asBinder() : null;
            this.mWorkSource = workSource;
            this.mUid = uid;
            this.mTag = tag;
            if (pendingIntent != null) {
                statsLocked = service.getStatsLocked(pendingIntent);
            } else {
                statsLocked = service.getStatsLocked(uid, alarmPkg);
            }
            this.mBroadcastStats = statsLocked;
            FilterStats fs = this.mBroadcastStats.filterStats.get(this.mTag);
            if (fs == null) {
                fs = new FilterStats(this.mBroadcastStats, this.mTag);
                this.mBroadcastStats.filterStats.put(this.mTag, fs);
            }
            fs.lastTime = nowELAPSED;
            this.mFilterStats = fs;
            this.mAlarmType = alarmType;
        }
    }

    static final class FilterStats {
        long aggregateTime;
        int count;
        long lastTime;
        final BroadcastStats mBroadcastStats;
        final String mTag;
        int nesting;
        int numWakeup;
        long startTime;

        FilterStats(BroadcastStats broadcastStats, String tag) {
            this.mBroadcastStats = broadcastStats;
            this.mTag = tag;
        }
    }

    static final class BroadcastStats {
        long aggregateTime;
        int count;
        final ArrayMap<String, FilterStats> filterStats = new ArrayMap<>();
        final String mPackageName;
        final int mUid;
        int nesting;
        int numWakeup;
        long startTime;

        BroadcastStats(int uid, String packageName) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }
    }

    @Override
    public void onStart() {
        this.mNativeData = init();
        this.mNextNonWakeup = 0L;
        this.mNextWakeup = 0L;
        if (SystemProperties.get("ro.mtk_bg_power_saving_support").equals("1")) {
            mSupportAlarmGrouping = true;
        }
        setTimeZoneImpl(SystemProperties.get(TIMEZONE_PROPERTY));
        if (mSupportAlarmGrouping && this.mAmPlus == null) {
            try {
                this.mAmPlus = new AlarmManagerPlus(getContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PowerManager pm = (PowerManager) getContext().getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, "*alarm*");
        this.mTimeTickSender = PendingIntent.getBroadcastAsUser(getContext(), 0, new Intent("android.intent.action.TIME_TICK").addFlags(1342177280), 0, UserHandle.ALL);
        Intent intent = new Intent("android.intent.action.DATE_CHANGED");
        intent.addFlags(536870912);
        this.mDateChangeSender = PendingIntent.getBroadcastAsUser(getContext(), 0, intent, 67108864, UserHandle.ALL);
        this.mClockReceiver = new ClockReceiver();
        this.mClockReceiver.scheduleTimeTickEvent();
        this.mClockReceiver.scheduleDateChangedEvent();
        this.mInteractiveStateReceiver = new InteractiveStateReceiver();
        this.mUninstallReceiver = new UninstallReceiver();
        this.mAlarmIconPackageList = new ArrayList<>();
        this.mAlarmIconPackageList.add("com.android.deskclock");
        try {
            IBinder binder = ServiceManager.getService("DmAgent");
            if (binder != null) {
                DmAgent agent = DmAgent.Stub.asInterface(binder);
                boolean locked = agent.isLockFlagSet();
                Slog.i(TAG, "dm state lock is " + locked);
                this.mDMEnable = !locked;
            } else {
                Slog.e(TAG, "dm binder is null!");
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "remote error");
        }
        this.mDMReceiver = new DMReceiver();
        this.mDmFreeList = new ArrayList<>();
        this.mDmFreeList.add(this.mTimeTickSender);
        this.mDmFreeList.add(this.mDateChangeSender);
        this.mDmResendList = new ArrayList<>();
        if (this.mNativeData != 0) {
            AlarmThread waitThread = new AlarmThread();
            waitThread.start();
        } else {
            Slog.w(TAG, "Failed to open alarm driver. Falling back to a handler.");
        }
        try {
            ActivityManagerNative.getDefault().registerUidObserver(new UidObserver(), 4);
        } catch (RemoteException e3) {
        }
        if (SystemProperties.get("ro.mtk_ipo_support").equals("1")) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.ACTION_BOOT_IPO");
            filter.addAction("android.intent.action.ACTION_SHUTDOWN");
            filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
            getContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent2) {
                    if ("android.intent.action.ACTION_SHUTDOWN".equals(intent2.getAction()) || "android.intent.action.ACTION_SHUTDOWN_IPO".equals(intent2.getAction())) {
                        AlarmManagerService.this.shutdownCheckPoweroffAlarm();
                        AlarmManagerService.this.mIPOShutdown = true;
                        if (AlarmManagerService.this.mNativeData == -1 || !"android.intent.action.ACTION_SHUTDOWN_IPO".equals(intent2.getAction())) {
                            return;
                        }
                        Slog.d(AlarmManagerService.TAG, "receive ACTION_SHUTDOWN_IPO , so close the fd ");
                        AlarmManagerService.this.close(AlarmManagerService.this.mNativeData);
                        AlarmManagerService.this.mNativeData = -1L;
                        return;
                    }
                    if (!"android.intent.action.ACTION_BOOT_IPO".equals(intent2.getAction())) {
                        return;
                    }
                    AlarmManagerService.this.mIPOShutdown = false;
                    AlarmManagerService.this.mNativeData = AlarmManagerService.this.init();
                    AlarmManagerService.this.mNextWakeup = AlarmManagerService.this.mNextNonWakeup = 0L;
                    Intent timeChangeIntent = new Intent("android.intent.action.TIME_SET");
                    timeChangeIntent.addFlags(536870912);
                    context.sendBroadcast(timeChangeIntent);
                    AlarmManagerService.this.mClockReceiver.scheduleTimeTickEvent();
                    AlarmManagerService.this.mClockReceiver.scheduleDateChangedEvent();
                    synchronized (AlarmManagerService.this.mWaitThreadlock) {
                        AlarmManagerService.this.mWaitThreadlock.notify();
                    }
                }
            }, filter);
        }
        publishBinderService("alarm", this.mService);
        publishLocalService(LocalService.class, new LocalService());
    }

    public boolean bootFromPoweroffAlarm() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        return bootReason != null && bootReason.equals("1");
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 500) {
            return;
        }
        this.mConstants.start(getContext().getContentResolver());
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mLocalDeviceIdleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
        this.dataShapingManager = ServiceManager.getService("data_shaping");
    }

    protected void finalize() throws Throwable {
        try {
            close(this.mNativeData);
        } finally {
            super.finalize();
        }
    }

    void setTimeZoneImpl(String tz) {
        if (TextUtils.isEmpty(tz)) {
            return;
        }
        TimeZone zone = TimeZone.getTimeZone(tz);
        boolean timeZoneWasChanged = false;
        synchronized (this) {
            String current = SystemProperties.get(TIMEZONE_PROPERTY);
            if (current == null || !current.equals(zone.getID())) {
                if (localLOGV) {
                    Slog.v(TAG, "timezone changed: " + current + ", new=" + zone.getID());
                }
                timeZoneWasChanged = true;
                SystemProperties.set(TIMEZONE_PROPERTY, zone.getID());
            }
            int gmtOffset = zone.getOffset(System.currentTimeMillis());
            setKernelTimezone(this.mNativeData, -(gmtOffset / 60000));
        }
        TimeZone.setDefault(null);
        if (!timeZoneWasChanged) {
            return;
        }
        Intent intent = new Intent("android.intent.action.TIMEZONE_CHANGED");
        intent.addFlags(536870912);
        intent.putExtra("time-zone", zone.getID());
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    void removeImpl(PendingIntent operation) {
        if (operation == null) {
            return;
        }
        synchronized (this.mLock) {
            removeLocked(operation, null);
        }
    }

    void setImpl(int type, long triggerAtTime, long windowLength, long interval, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, int flags, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock, int callingUid, String callingPackage) {
        long maxElapsed;
        if ((operation == null && directReceiver == null) || (operation != null && directReceiver != null)) {
            Slog.w(TAG, "Alarms must either supply a PendingIntent or an AlarmReceiver");
            return;
        }
        if (this.mIPOShutdown && this.mNativeData == -1) {
            Slog.w(TAG, "IPO Shutdown so drop the alarm");
            return;
        }
        if (windowLength > 43200000) {
            Slog.w(TAG, "Window length " + windowLength + "ms suspiciously long; limiting to 1 hour");
            windowLength = 3600000;
        }
        long minInterval = this.mConstants.MIN_INTERVAL;
        if (interval > 0 && interval < minInterval) {
            Slog.w(TAG, "Suspiciously short interval " + interval + " millis; expanding to " + (minInterval / 1000) + " seconds");
            interval = minInterval;
        }
        if (triggerAtTime < 0) {
            long what = Binder.getCallingPid();
            Slog.w(TAG, "Invalid alarm trigger time! " + triggerAtTime + " from uid=" + callingUid + " pid=" + what);
            triggerAtTime = 0;
        }
        if (type == 7 || type == 8) {
            if (this.mNativeData == -1) {
                Slog.w(TAG, "alarm driver not open ,return!");
                return;
            }
            Slog.d(TAG, "alarm set type 7 8, package name " + operation.getTargetPackage());
            operation.getTargetPackage();
            long nowTime = System.currentTimeMillis();
            if (triggerAtTime < nowTime) {
                Slog.w(TAG, "power off alarm set time is wrong! nowTime = " + nowTime + " ; triggerAtTime = " + triggerAtTime);
                return;
            }
            synchronized (this.mPowerOffAlarmLock) {
                removePoweroffAlarmLocked(operation.getTargetPackage());
                int poweroffAlarmUserId = UserHandle.getCallingUserId();
                Alarm alarm = new Alarm(type, triggerAtTime, 0L, 0L, 0L, interval, operation, directReceiver, listenerTag, workSource, 0, alarmClock, poweroffAlarmUserId, callingPackage, true);
                addPoweroffAlarmLocked(alarm);
                if (this.mPoweroffAlarms.size() > 0) {
                    resetPoweroffAlarm(this.mPoweroffAlarms.get(0));
                }
            }
            type = 0;
        }
        long nowElapsed = SystemClock.elapsedRealtime();
        long nominalTrigger = convertToElapsed(triggerAtTime, type);
        long minTrigger = nowElapsed + this.mConstants.MIN_FUTURITY;
        long triggerElapsed = nominalTrigger > minTrigger ? nominalTrigger : minTrigger;
        if (mSupportAlarmGrouping && this.mAmPlus != null) {
            maxElapsed = this.mAmPlus.getMaxTriggerTime(type, triggerElapsed, windowLength, interval, operation, mAlarmMode, true);
            if (maxElapsed < 0) {
                maxElapsed = 0 - maxElapsed;
                this.mNeedGrouping = false;
            } else {
                this.mNeedGrouping = true;
            }
        } else if (windowLength == 0) {
            maxElapsed = triggerElapsed;
        } else if (windowLength < 0) {
            maxElapsed = maxTriggerTime(nowElapsed, triggerElapsed, interval);
            windowLength = maxElapsed - triggerElapsed;
        } else {
            maxElapsed = triggerElapsed + windowLength;
        }
        synchronized (this.mLock) {
            if (operation == null) {
                Slog.v(TAG, "APP set with listener(" + listenerTag + ") : type=" + type + " triggerAtTime=" + triggerAtTime + " win=" + windowLength + " tElapsed=" + triggerElapsed + " maxElapsed=" + maxElapsed + " interval=" + interval + " flags=0x" + Integer.toHexString(flags));
            } else {
                Slog.v(TAG, "APP set(" + operation + ") : type=" + type + " triggerAtTime=" + triggerAtTime + " win=" + windowLength + " tElapsed=" + triggerElapsed + " maxElapsed=" + maxElapsed + " interval=" + interval + " flags=0x" + Integer.toHexString(flags));
            }
            setImplLocked(type, triggerAtTime, triggerElapsed, windowLength, maxElapsed, interval, operation, directReceiver, listenerTag, flags, true, workSource, alarmClock, callingUid, callingPackage, this.mNeedGrouping);
        }
    }

    private void setImplLocked(int type, long when, long whenElapsed, long windowLength, long maxWhen, long interval, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, int flags, boolean doValidate, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock, int callingUid, String callingPackage, boolean mNeedGrouping) {
        Alarm a = new Alarm(type, when, whenElapsed, windowLength, maxWhen, interval, operation, directReceiver, listenerTag, workSource, flags, alarmClock, callingUid, callingPackage, mNeedGrouping);
        try {
            if (ActivityManagerNative.getDefault().getAppStartMode(callingUid, callingPackage) == 2) {
                Slog.w(TAG, "Not setting alarm from " + callingUid + ":" + a + " -- package not allowed to start");
                return;
            }
        } catch (RemoteException e) {
        }
        removeLocked(operation, directReceiver);
        setImplLocked(a, false, doValidate);
    }

    private void setImplLocked(Alarm a, boolean rebatching, boolean doValidate) {
        int whichBatch;
        if ((a.flags & 16) != 0) {
            if (this.mNextWakeFromIdle != null && a.whenElapsed > this.mNextWakeFromIdle.whenElapsed) {
                long j = this.mNextWakeFromIdle.whenElapsed;
                a.maxWhenElapsed = j;
                a.whenElapsed = j;
                a.when = j;
            }
            long nowElapsed = SystemClock.elapsedRealtime();
            int fuzz = fuzzForDuration(a.whenElapsed - nowElapsed);
            if (fuzz > 0) {
                if (this.mRandom == null) {
                    this.mRandom = new Random();
                }
                int delta = this.mRandom.nextInt(fuzz);
                a.whenElapsed -= (long) delta;
                long j2 = a.whenElapsed;
                a.maxWhenElapsed = j2;
                a.when = j2;
            }
        } else if (this.mPendingIdleUntil != null && (a.flags & 14) == 0) {
            this.mPendingWhileIdleAlarms.add(a);
            return;
        }
        if (DEBUG_BATCH) {
            Slog.d(TAG, "a.whenElapsed =" + a.whenElapsed + " a.needGrouping= " + a.needGrouping + "  a.flags= " + a.flags);
        }
        if (!mSupportAlarmGrouping || this.mAmPlus == null) {
            Slog.d(TAG, "default path for whichBatch");
            whichBatch = (a.flags & 1) != 0 ? -1 : attemptCoalesceLocked(a.whenElapsed, a.maxWhenElapsed);
        } else {
            whichBatch = !a.needGrouping ? -1 : attemptCoalesceLocked(a.whenElapsed, a.maxWhenElapsed);
        }
        if (DEBUG_BATCH || Build.TYPE.equals("eng")) {
            Slog.d(TAG, " whichBatch = " + whichBatch);
        }
        if (whichBatch < 0) {
            addBatchLocked(this.mAlarmBatches, new Batch(a));
        } else {
            Batch batch = this.mAlarmBatches.get(whichBatch);
            if (DEBUG_BATCH) {
                Slog.d(TAG, " alarm = " + a + " add to " + batch);
            }
            if (batch.add(a)) {
                this.mAlarmBatches.remove(whichBatch);
                addBatchLocked(this.mAlarmBatches, batch);
            }
        }
        if (a.alarmClock != null) {
            this.mNextAlarmClockMayChange = true;
        }
        boolean needRebatch = false;
        if ((a.flags & 16) != 0) {
            this.mPendingIdleUntil = a;
            this.mConstants.updateAllowWhileIdleMinTimeLocked();
            needRebatch = true;
        } else if ((a.flags & 2) != 0 && (this.mNextWakeFromIdle == null || this.mNextWakeFromIdle.whenElapsed > a.whenElapsed)) {
            this.mNextWakeFromIdle = a;
            if (this.mPendingIdleUntil != null) {
                needRebatch = true;
            }
        }
        if (rebatching) {
            return;
        }
        if (DEBUG_VALIDATE && doValidate && !validateConsistencyLocked()) {
            Slog.v(TAG, "Tipping-point operation: type=" + a.type + " when=" + a.when + " when(hex)=" + Long.toHexString(a.when) + " whenElapsed=" + a.whenElapsed + " maxWhenElapsed=" + a.maxWhenElapsed + " interval=" + a.repeatInterval + " op=" + a.operation + " flags=0x" + Integer.toHexString(a.flags));
            rebatchAllAlarmsLocked(false);
            needRebatch = false;
        }
        if (needRebatch) {
            rebatchAllAlarmsLocked(false);
        }
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
    }

    public final class LocalService {
        public LocalService() {
        }

        public void setDeviceIdleUserWhitelist(int[] appids) {
            AlarmManagerService.this.setDeviceIdleUserWhitelistImpl(appids);
        }
    }

    protected void configLogTag(PrintWriter pw, String[] args, int opti) {
        if (opti >= args.length) {
            pw.println("  Invalid argument!");
            return;
        }
        if ("on".equals(args[opti])) {
            localLOGV = true;
            DEBUG_BATCH = true;
            DEBUG_VALIDATE = true;
            return;
        }
        if ("off".equals(args[opti])) {
            localLOGV = false;
            DEBUG_BATCH = false;
            DEBUG_VALIDATE = false;
        } else if ("0".equals(args[opti])) {
            mAlarmMode = 0;
            Slog.v(TAG, "mAlarmMode = " + mAlarmMode);
        } else if ("1".equals(args[opti])) {
            mAlarmMode = 1;
            Slog.v(TAG, "mAlarmMode = " + mAlarmMode);
        } else if ("2".equals(args[opti])) {
            mAlarmMode = 2;
            Slog.v(TAG, "mAlarmMode = " + mAlarmMode);
        } else {
            pw.println("  Invalid argument!");
        }
    }

    void dumpImpl(PrintWriter pw, String[] args) {
        String opt;
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
            opti++;
            if ("-h".equals(opt)) {
                pw.println("alarm manager dump options:");
                pw.println("  log  [on/off]");
                pw.println("  Example:");
                pw.println("  $adb shell dumpsys alarm log on");
                pw.println("  $adb shell dumpsys alarm log off");
                return;
            }
            pw.println("Unknown argument: " + opt + "; use -h for help");
        }
        if (opti < args.length) {
            String cmd = args[opti];
            int opti2 = opti + 1;
            if ("log".equals(cmd)) {
                configLogTag(pw, args, opti2);
                return;
            }
        }
        synchronized (this.mLock) {
            pw.println("Current Alarm Manager state:");
            this.mConstants.dump(pw);
            pw.println();
            long nowRTC = System.currentTimeMillis();
            long nowELAPSED = SystemClock.elapsedRealtime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            pw.print("  nowRTC=");
            pw.print(nowRTC);
            pw.print("=");
            pw.print(sdf.format(new Date(nowRTC)));
            pw.print(" nowELAPSED=");
            pw.print(nowELAPSED);
            pw.println();
            pw.print("  mLastTimeChangeClockTime=");
            pw.print(this.mLastTimeChangeClockTime);
            pw.print("=");
            pw.println(sdf.format(new Date(this.mLastTimeChangeClockTime)));
            pw.print("  mLastTimeChangeRealtime=");
            TimeUtils.formatDuration(this.mLastTimeChangeRealtime, pw);
            pw.println();
            if (!this.mInteractive) {
                pw.print("  Time since non-interactive: ");
                TimeUtils.formatDuration(nowELAPSED - this.mNonInteractiveStartTime, pw);
                pw.println();
                pw.print("  Max wakeup delay: ");
                TimeUtils.formatDuration(currentNonWakeupFuzzLocked(nowELAPSED), pw);
                pw.println();
                pw.print("  Time since last dispatch: ");
                TimeUtils.formatDuration(nowELAPSED - this.mLastAlarmDeliveryTime, pw);
                pw.println();
                pw.print("  Next non-wakeup delivery time: ");
                TimeUtils.formatDuration(nowELAPSED - this.mNextNonWakeupDeliveryTime, pw);
                pw.println();
            }
            long nextWakeupRTC = this.mNextWakeup + (nowRTC - nowELAPSED);
            long nextNonWakeupRTC = this.mNextNonWakeup + (nowRTC - nowELAPSED);
            pw.print("  Next non-wakeup alarm: ");
            TimeUtils.formatDuration(this.mNextNonWakeup, nowELAPSED, pw);
            pw.print(" = ");
            pw.println(sdf.format(new Date(nextNonWakeupRTC)));
            pw.print("  Next wakeup: ");
            TimeUtils.formatDuration(this.mNextWakeup, nowELAPSED, pw);
            pw.print(" = ");
            pw.println(sdf.format(new Date(nextWakeupRTC)));
            pw.print("  Last wakeup: ");
            TimeUtils.formatDuration(this.mLastWakeup, nowELAPSED, pw);
            pw.print(" set at ");
            TimeUtils.formatDuration(this.mLastWakeupSet, nowELAPSED, pw);
            pw.println();
            pw.print("  Num time change events: ");
            pw.println(this.mNumTimeChanged);
            pw.println("  mDeviceIdleUserWhitelist=" + Arrays.toString(this.mDeviceIdleUserWhitelist));
            pw.println();
            pw.println("  Next alarm clock information: ");
            TreeSet<Integer> users = new TreeSet<>();
            for (int i = 0; i < this.mNextAlarmClockForUser.size(); i++) {
                users.add(Integer.valueOf(this.mNextAlarmClockForUser.keyAt(i)));
            }
            for (int i2 = 0; i2 < this.mPendingSendNextAlarmClockChangedForUser.size(); i2++) {
                users.add(Integer.valueOf(this.mPendingSendNextAlarmClockChangedForUser.keyAt(i2)));
            }
            Iterator user$iterator = users.iterator();
            while (user$iterator.hasNext()) {
                int user = ((Integer) user$iterator.next()).intValue();
                AlarmManager.AlarmClockInfo next = this.mNextAlarmClockForUser.get(user);
                long time = next != null ? next.getTriggerTime() : 0L;
                boolean pendingSend = this.mPendingSendNextAlarmClockChangedForUser.get(user);
                pw.print("    user:");
                pw.print(user);
                pw.print(" pendingSend:");
                pw.print(pendingSend);
                pw.print(" time:");
                pw.print(time);
                if (time > 0) {
                    pw.print(" = ");
                    pw.print(sdf.format(new Date(time)));
                    pw.print(" = ");
                    TimeUtils.formatDuration(time, nowRTC, pw);
                }
                pw.println();
            }
            if (this.mAlarmBatches.size() > 0) {
                pw.println();
                pw.print("  Pending alarm batches: ");
                pw.println(this.mAlarmBatches.size());
                for (Batch b : this.mAlarmBatches) {
                    pw.print(b);
                    pw.println(':');
                    dumpAlarmList(pw, b.alarms, "    ", nowELAPSED, nowRTC, sdf);
                }
            }
            if (this.mPendingIdleUntil != null || this.mPendingWhileIdleAlarms.size() > 0) {
                pw.println();
                pw.println("    Idle mode state:");
                pw.print("      Idling until: ");
                if (this.mPendingIdleUntil != null) {
                    pw.println(this.mPendingIdleUntil);
                    this.mPendingIdleUntil.dump(pw, "        ", nowRTC, nowELAPSED, sdf);
                } else {
                    pw.println("null");
                }
                pw.println("      Pending alarms:");
                dumpAlarmList(pw, this.mPendingWhileIdleAlarms, "      ", nowELAPSED, nowRTC, sdf);
            }
            if (this.mNextWakeFromIdle != null) {
                pw.println();
                pw.print("  Next wake from idle: ");
                pw.println(this.mNextWakeFromIdle);
                this.mNextWakeFromIdle.dump(pw, "    ", nowRTC, nowELAPSED, sdf);
            }
            pw.println();
            pw.print("  Past-due non-wakeup alarms: ");
            if (this.mPendingNonWakeupAlarms.size() > 0) {
                pw.println(this.mPendingNonWakeupAlarms.size());
                dumpAlarmList(pw, this.mPendingNonWakeupAlarms, "    ", nowELAPSED, nowRTC, sdf);
            } else {
                pw.println("(none)");
            }
            pw.print("    Number of delayed alarms: ");
            pw.print(this.mNumDelayedAlarms);
            pw.print(", total delay time: ");
            TimeUtils.formatDuration(this.mTotalDelayTime, pw);
            pw.println();
            pw.print("    Max delay time: ");
            TimeUtils.formatDuration(this.mMaxDelayTime, pw);
            pw.print(", max non-interactive time: ");
            TimeUtils.formatDuration(this.mNonInteractiveTime, pw);
            pw.println();
            pw.println();
            pw.print("  Broadcast ref count: ");
            pw.println(this.mBroadcastRefCount);
            pw.println();
            if (this.mInFlight.size() > 0) {
                pw.println("Outstanding deliveries:");
                for (int i3 = 0; i3 < this.mInFlight.size(); i3++) {
                    pw.print("   #");
                    pw.print(i3);
                    pw.print(": ");
                    pw.println(this.mInFlight.get(i3));
                }
                pw.println();
            }
            pw.print("  mAllowWhileIdleMinTime=");
            TimeUtils.formatDuration(this.mAllowWhileIdleMinTime, pw);
            pw.println();
            if (this.mLastAllowWhileIdleDispatch.size() > 0) {
                pw.println("  Last allow while idle dispatch times:");
                for (int i4 = 0; i4 < this.mLastAllowWhileIdleDispatch.size(); i4++) {
                    pw.print("  UID ");
                    UserHandle.formatUid(pw, this.mLastAllowWhileIdleDispatch.keyAt(i4));
                    pw.print(": ");
                    TimeUtils.formatDuration(this.mLastAllowWhileIdleDispatch.valueAt(i4), nowELAPSED, pw);
                    pw.println();
                }
            }
            pw.println();
            if (this.mLog.dump(pw, "  Recent problems", "    ")) {
                pw.println();
            }
            FilterStats[] topFilters = new FilterStats[10];
            Comparator<FilterStats> comparator = new Comparator<FilterStats>() {
                @Override
                public int compare(FilterStats lhs, FilterStats rhs) {
                    if (lhs.aggregateTime < rhs.aggregateTime) {
                        return 1;
                    }
                    if (lhs.aggregateTime > rhs.aggregateTime) {
                        return -1;
                    }
                    return 0;
                }
            };
            int len = 0;
            for (int iu = 0; iu < this.mBroadcastStats.size(); iu++) {
                ArrayMap<String, BroadcastStats> uidStats = this.mBroadcastStats.valueAt(iu);
                for (int ip = 0; ip < uidStats.size(); ip++) {
                    BroadcastStats bs = uidStats.valueAt(ip);
                    for (int is = 0; is < bs.filterStats.size(); is++) {
                        FilterStats fs = bs.filterStats.valueAt(is);
                        int pos = len > 0 ? Arrays.binarySearch(topFilters, 0, len, fs, comparator) : 0;
                        if (pos < 0) {
                            pos = (-pos) - 1;
                        }
                        if (pos < topFilters.length) {
                            int copylen = (topFilters.length - pos) - 1;
                            if (copylen > 0) {
                                System.arraycopy(topFilters, pos, topFilters, pos + 1, copylen);
                            }
                            topFilters[pos] = fs;
                            if (len < topFilters.length) {
                                len++;
                            }
                        }
                    }
                }
            }
            if (len > 0) {
                pw.println("  Top Alarms:");
                for (int i5 = 0; i5 < len; i5++) {
                    FilterStats fs2 = topFilters[i5];
                    pw.print("    ");
                    if (fs2.nesting > 0) {
                        pw.print("*ACTIVE* ");
                    }
                    TimeUtils.formatDuration(fs2.aggregateTime, pw);
                    pw.print(" running, ");
                    pw.print(fs2.numWakeup);
                    pw.print(" wakeups, ");
                    pw.print(fs2.count);
                    pw.print(" alarms: ");
                    UserHandle.formatUid(pw, fs2.mBroadcastStats.mUid);
                    pw.print(":");
                    pw.print(fs2.mBroadcastStats.mPackageName);
                    pw.println();
                    pw.print("      ");
                    pw.print(fs2.mTag);
                    pw.println();
                }
            }
            pw.println(" ");
            pw.println("  Alarm Stats:");
            ArrayList<FilterStats> tmpFilters = new ArrayList<>();
            for (int iu2 = 0; iu2 < this.mBroadcastStats.size(); iu2++) {
                ArrayMap<String, BroadcastStats> uidStats2 = this.mBroadcastStats.valueAt(iu2);
                for (int ip2 = 0; ip2 < uidStats2.size(); ip2++) {
                    BroadcastStats bs2 = uidStats2.valueAt(ip2);
                    pw.print("  ");
                    if (bs2.nesting > 0) {
                        pw.print("*ACTIVE* ");
                    }
                    UserHandle.formatUid(pw, bs2.mUid);
                    pw.print(":");
                    pw.print(bs2.mPackageName);
                    pw.print(" ");
                    TimeUtils.formatDuration(bs2.aggregateTime, pw);
                    pw.print(" running, ");
                    pw.print(bs2.numWakeup);
                    pw.println(" wakeups:");
                    tmpFilters.clear();
                    for (int is2 = 0; is2 < bs2.filterStats.size(); is2++) {
                        tmpFilters.add(bs2.filterStats.valueAt(is2));
                    }
                    Collections.sort(tmpFilters, comparator);
                    for (int i6 = 0; i6 < tmpFilters.size(); i6++) {
                        FilterStats fs3 = tmpFilters.get(i6);
                        pw.print("    ");
                        if (fs3.nesting > 0) {
                            pw.print("*ACTIVE* ");
                        }
                        TimeUtils.formatDuration(fs3.aggregateTime, pw);
                        pw.print(" ");
                        pw.print(fs3.numWakeup);
                        pw.print(" wakes ");
                        pw.print(fs3.count);
                        pw.print(" alarms, last ");
                        TimeUtils.formatDuration(fs3.lastTime, nowELAPSED, pw);
                        pw.println(":");
                        pw.print("      ");
                        pw.print(fs3.mTag);
                        pw.println();
                    }
                }
            }
        }
    }

    private void logBatchesLocked(SimpleDateFormat sdf) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(PackageManagerService.DumpState.DUMP_VERIFIERS);
        PrintWriter pw = new PrintWriter(bs);
        long nowRTC = System.currentTimeMillis();
        long nowELAPSED = SystemClock.elapsedRealtime();
        int NZ = this.mAlarmBatches.size();
        for (int iz = 0; iz < NZ; iz++) {
            Batch bz = this.mAlarmBatches.get(iz);
            pw.append("Batch ");
            pw.print(iz);
            pw.append(": ");
            pw.println(bz);
            dumpAlarmList(pw, bz.alarms, "  ", nowELAPSED, nowRTC, sdf);
            pw.flush();
            Slog.v(TAG, bs.toString());
            bs.reset();
        }
    }

    private boolean validateConsistencyLocked() {
        if (DEBUG_VALIDATE) {
            long lastTime = Long.MIN_VALUE;
            int N = this.mAlarmBatches.size();
            for (int i = 0; i < N; i++) {
                Batch b = this.mAlarmBatches.get(i);
                if (b.start >= lastTime) {
                    lastTime = b.start;
                } else {
                    Slog.e(TAG, "CONSISTENCY FAILURE: Batch " + i + " is out of order");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    logBatchesLocked(sdf);
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private Batch findFirstWakeupBatchLocked() {
        int N = this.mAlarmBatches.size();
        for (int i = 0; i < N; i++) {
            Batch b = this.mAlarmBatches.get(i);
            if (b.hasWakeups()) {
                return b;
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

    void setDeviceIdleUserWhitelistImpl(int[] appids) {
        synchronized (this.mLock) {
            this.mDeviceIdleUserWhitelist = appids;
        }
    }

    AlarmManager.AlarmClockInfo getNextAlarmClockImpl(int userId) {
        AlarmManager.AlarmClockInfo alarmClockInfo;
        Slog.d(TAG, "getNextAlarmClockImpl is called before Lock ");
        synchronized (this.mLock) {
            Slog.d(TAG, "getNextAlarmClockImpl is called in Lock ");
            alarmClockInfo = this.mNextAlarmClockForUser.get(userId);
        }
        return alarmClockInfo;
    }

    private void updateNextAlarmClockLocked() {
        if (!this.mNextAlarmClockMayChange) {
            return;
        }
        this.mNextAlarmClockMayChange = false;
        SparseArray<AlarmManager.AlarmClockInfo> nextForUser = this.mTmpSparseAlarmClockArray;
        nextForUser.clear();
        int N = this.mAlarmBatches.size();
        for (int i = 0; i < N; i++) {
            ArrayList<Alarm> alarms = this.mAlarmBatches.get(i).alarms;
            int M = alarms.size();
            for (int j = 0; j < M; j++) {
                Alarm a = alarms.get(j);
                if (a.alarmClock != null) {
                    int userId = UserHandle.getUserId(a.uid);
                    AlarmManager.AlarmClockInfo current = this.mNextAlarmClockForUser.get(userId);
                    if (DEBUG_ALARM_CLOCK) {
                        Log.v(TAG, "Found AlarmClockInfo " + a.alarmClock + " at " + formatNextAlarm(getContext(), a.alarmClock, userId) + " for user " + userId);
                    }
                    if (nextForUser.get(userId) == null) {
                        nextForUser.put(userId, a.alarmClock);
                    } else if (a.alarmClock.equals(current) && current.getTriggerTime() <= nextForUser.get(userId).getTriggerTime()) {
                        nextForUser.put(userId, current);
                    }
                }
            }
        }
        int NN = nextForUser.size();
        for (int i2 = 0; i2 < NN; i2++) {
            AlarmManager.AlarmClockInfo newAlarm = nextForUser.valueAt(i2);
            int userId2 = nextForUser.keyAt(i2);
            AlarmManager.AlarmClockInfo currentAlarm = this.mNextAlarmClockForUser.get(userId2);
            if (!newAlarm.equals(currentAlarm)) {
                updateNextAlarmInfoForUserLocked(userId2, newAlarm);
            }
        }
        int NNN = this.mNextAlarmClockForUser.size();
        for (int i3 = NNN - 1; i3 >= 0; i3--) {
            int userId3 = this.mNextAlarmClockForUser.keyAt(i3);
            if (nextForUser.get(userId3) == null) {
                updateNextAlarmInfoForUserLocked(userId3, null);
            }
        }
    }

    private void updateNextAlarmInfoForUserLocked(int userId, AlarmManager.AlarmClockInfo alarmClock) {
        if (alarmClock != null) {
            if (DEBUG_ALARM_CLOCK) {
                Log.v(TAG, "Next AlarmClockInfoForUser(" + userId + "): " + formatNextAlarm(getContext(), alarmClock, userId));
            }
            this.mNextAlarmClockForUser.put(userId, alarmClock);
        } else {
            if (DEBUG_ALARM_CLOCK) {
                Log.v(TAG, "Next AlarmClockInfoForUser(" + userId + "): None");
            }
            this.mNextAlarmClockForUser.remove(userId);
        }
        this.mPendingSendNextAlarmClockChangedForUser.put(userId, true);
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessage(2);
    }

    private void sendNextAlarmClockChanged() {
        SparseArray<AlarmManager.AlarmClockInfo> pendingUsers = this.mHandlerSparseAlarmClockArray;
        pendingUsers.clear();
        Slog.w(TAG, "sendNextAlarmClockChanged begin");
        synchronized (this.mLock) {
            int N = this.mPendingSendNextAlarmClockChangedForUser.size();
            for (int i = 0; i < N; i++) {
                int userId = this.mPendingSendNextAlarmClockChangedForUser.keyAt(i);
                pendingUsers.append(userId, this.mNextAlarmClockForUser.get(userId));
            }
            this.mPendingSendNextAlarmClockChangedForUser.clear();
        }
        int N2 = pendingUsers.size();
        for (int i2 = 0; i2 < N2; i2++) {
            int userId2 = pendingUsers.keyAt(i2);
            AlarmManager.AlarmClockInfo alarmClock = pendingUsers.valueAt(i2);
            Settings.System.putStringForUser(getContext().getContentResolver(), "next_alarm_formatted", formatNextAlarm(getContext(), alarmClock, userId2), userId2);
            getContext().sendBroadcastAsUser(NEXT_ALARM_CLOCK_CHANGED_INTENT, new UserHandle(userId2));
        }
        Slog.w(TAG, "sendNextAlarmClockChanged end");
    }

    private static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info, int userId) {
        String skeleton = DateFormat.is24HourFormat(context, userId) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return info == null ? "" : DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    void rescheduleKernelAlarmsLocked() {
        if (this.mIPOShutdown && this.mNativeData == -1) {
            Slog.w(TAG, "IPO Shutdown so drop the repeating alarm");
            return;
        }
        long nextNonWakeup = 0;
        if (this.mAlarmBatches.size() > 0) {
            Batch firstWakeup = findFirstWakeupBatchLocked();
            Batch firstBatch = this.mAlarmBatches.get(0);
            if (firstWakeup != null && this.mNextWakeup != firstWakeup.start) {
                this.mNextWakeup = firstWakeup.start;
                this.mLastWakeupSet = SystemClock.elapsedRealtime();
                setLocked(2, firstWakeup.start);
            }
            if (firstBatch != firstWakeup) {
                nextNonWakeup = firstBatch.start;
            }
        }
        if (this.mPendingNonWakeupAlarms.size() > 0 && (nextNonWakeup == 0 || this.mNextNonWakeupDeliveryTime < nextNonWakeup)) {
            nextNonWakeup = this.mNextNonWakeupDeliveryTime;
        }
        if (nextNonWakeup == 0 || this.mNextNonWakeup == nextNonWakeup) {
            return;
        }
        this.mNextNonWakeup = nextNonWakeup;
        setLocked(3, nextNonWakeup);
    }

    private void removeLocked(PendingIntent operation, IAlarmListener directReceiver) {
        boolean didRemove = false;
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = this.mAlarmBatches.get(i);
            didRemove |= b.remove(operation, directReceiver);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (int i2 = this.mPendingWhileIdleAlarms.size() - 1; i2 >= 0; i2--) {
            if (this.mPendingWhileIdleAlarms.get(i2).matches(operation, directReceiver)) {
                this.mPendingWhileIdleAlarms.remove(i2);
            }
        }
        if (!didRemove) {
            return;
        }
        Slog.d(TAG, "remove(operation) changed bounds; rebatching operation = " + operation);
        boolean restorePending = false;
        if (this.mPendingIdleUntil != null && this.mPendingIdleUntil.matches(operation, directReceiver)) {
            this.mPendingIdleUntil = null;
            restorePending = true;
        }
        if (this.mNextWakeFromIdle != null && this.mNextWakeFromIdle.matches(operation, directReceiver)) {
            this.mNextWakeFromIdle = null;
        }
        if (this.mAlarmBatches.size() < 300) {
            rebatchAllAlarmsLocked(true);
        } else {
            Slog.d(TAG, "mAlarmBatches.size() is larger than 300 , do not rebatch");
        }
        if (restorePending) {
            restorePendingWhileIdleAlarmsLocked();
        }
        updateNextAlarmClockLocked();
    }

    void removeLocked(String packageName) {
        boolean didRemove = false;
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = this.mAlarmBatches.get(i);
            didRemove |= b.remove(packageName);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (int i2 = this.mPendingWhileIdleAlarms.size() - 1; i2 >= 0; i2--) {
            Alarm a = this.mPendingWhileIdleAlarms.get(i2);
            if (a.matches(packageName)) {
                this.mPendingWhileIdleAlarms.remove(i2);
            }
        }
        if (!didRemove) {
            return;
        }
        Slog.v(TAG, "remove(package) changed bounds; rebatching");
        rebatchAllAlarmsLocked(true);
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
    }

    void removeForStoppedLocked(int uid) {
        boolean didRemove = false;
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = this.mAlarmBatches.get(i);
            didRemove |= b.removeForStopped(uid);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (int i2 = this.mPendingWhileIdleAlarms.size() - 1; i2 >= 0; i2--) {
            Alarm a = this.mPendingWhileIdleAlarms.get(i2);
            try {
                if (a.uid == uid && ActivityManagerNative.getDefault().getAppStartMode(uid, a.packageName) == 2) {
                    this.mPendingWhileIdleAlarms.remove(i2);
                }
            } catch (RemoteException e) {
            }
        }
        if (!didRemove) {
            return;
        }
        if (DEBUG_BATCH) {
            Slog.v(TAG, "remove(package) changed bounds; rebatching");
        }
        rebatchAllAlarmsLocked(true);
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
    }

    boolean removeInvalidAlarmLocked(PendingIntent operation, IAlarmListener listener) {
        boolean didRemove = false;
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = this.mAlarmBatches.get(i);
            didRemove |= b.remove(operation, listener);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        return didRemove;
    }

    void removeUserLocked(int userHandle) {
        boolean didRemove = false;
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = this.mAlarmBatches.get(i);
            didRemove |= b.remove(userHandle);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (int i2 = this.mPendingWhileIdleAlarms.size() - 1; i2 >= 0; i2--) {
            if (UserHandle.getUserId(this.mPendingWhileIdleAlarms.get(i2).creatorUid) == userHandle) {
                this.mPendingWhileIdleAlarms.remove(i2);
            }
        }
        for (int i3 = this.mLastAllowWhileIdleDispatch.size() - 1; i3 >= 0; i3--) {
            if (UserHandle.getUserId(this.mLastAllowWhileIdleDispatch.keyAt(i3)) == userHandle) {
                this.mLastAllowWhileIdleDispatch.removeAt(i3);
            }
        }
        if (!didRemove) {
            return;
        }
        if (DEBUG_BATCH) {
            Slog.v(TAG, "remove(user) changed bounds; rebatching");
        }
        rebatchAllAlarmsLocked(true);
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
    }

    void interactiveStateChangedLocked(boolean interactive) {
        if (this.mInteractive == interactive) {
            return;
        }
        this.mInteractive = interactive;
        long nowELAPSED = SystemClock.elapsedRealtime();
        if (interactive) {
            if (this.mPendingNonWakeupAlarms.size() > 0) {
                long thisDelayTime = nowELAPSED - this.mStartCurrentDelayTime;
                this.mTotalDelayTime += thisDelayTime;
                if (this.mMaxDelayTime < thisDelayTime) {
                    this.mMaxDelayTime = thisDelayTime;
                }
                deliverAlarmsLocked(this.mPendingNonWakeupAlarms, nowELAPSED);
                this.mPendingNonWakeupAlarms.clear();
            }
            if (this.mNonInteractiveStartTime <= 0) {
                return;
            }
            long dur = nowELAPSED - this.mNonInteractiveStartTime;
            if (dur <= this.mNonInteractiveTime) {
                return;
            }
            this.mNonInteractiveTime = dur;
            return;
        }
        this.mNonInteractiveStartTime = nowELAPSED;
    }

    boolean lookForPackageLocked(String packageName) {
        for (int i = 0; i < this.mAlarmBatches.size(); i++) {
            Batch b = this.mAlarmBatches.get(i);
            if (b.hasPackage(packageName)) {
                return true;
            }
        }
        for (int i2 = 0; i2 < this.mPendingWhileIdleAlarms.size(); i2++) {
            Alarm a = this.mPendingWhileIdleAlarms.get(i2);
            if (a.matches(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void setLocked(int type, long when) {
        long alarmSeconds;
        long alarmNanoseconds;
        if (this.mNativeData != 0 && this.mNativeData != -1) {
            if (when < 0) {
                alarmSeconds = 0;
                alarmNanoseconds = 0;
            } else {
                alarmSeconds = when / 1000;
                alarmNanoseconds = (when % 1000) * 1000 * 1000;
            }
            Slog.d(TAG, "set alarm to RTC " + when);
            set(this.mNativeData, type, alarmSeconds, alarmNanoseconds);
            return;
        }
        Slog.d(TAG, "the mNativeData from RTC is abnormal,  mNativeData = " + this.mNativeData);
        Message msg = Message.obtain();
        msg.what = 1;
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageAtTime(msg, when);
    }

    private static final void dumpAlarmList(PrintWriter pw, ArrayList<Alarm> list, String prefix, String label, long nowRTC, long nowELAPSED, SimpleDateFormat sdf) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Alarm a = list.get(i);
            pw.print(prefix);
            pw.print(label);
            pw.print(" #");
            pw.print(i);
            pw.print(": ");
            pw.println(a);
            a.dump(pw, prefix + "  ", nowRTC, nowELAPSED, sdf);
        }
    }

    private static final String labelForType(int type) {
        switch (type) {
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

    private static final void dumpAlarmList(PrintWriter pw, ArrayList<Alarm> list, String prefix, long nowELAPSED, long nowRTC, SimpleDateFormat sdf) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Alarm a = list.get(i);
            String label = labelForType(a.type);
            pw.print(prefix);
            pw.print(label);
            pw.print(" #");
            pw.print(i);
            pw.print(": ");
            pw.println(a);
            a.dump(pw, prefix + "  ", nowRTC, nowELAPSED, sdf);
        }
    }

    boolean triggerAlarmsLocked(ArrayList<Alarm> triggerList, long nowELAPSED, long nowRTC) {
        long maxElapsed;
        boolean hasWakeup = false;
        while (this.mAlarmBatches.size() > 0) {
            Batch batch = this.mAlarmBatches.get(0);
            if (batch.start > nowELAPSED) {
                break;
            }
            this.mAlarmBatches.remove(0);
            int N = batch.size();
            for (int i = 0; i < N; i++) {
                Alarm alarm = batch.get(i);
                if ((alarm.flags & 4) != 0) {
                    long lastTime = this.mLastAllowWhileIdleDispatch.get(alarm.uid, 0L);
                    long minTime = lastTime + this.mAllowWhileIdleMinTime;
                    if (nowELAPSED < minTime) {
                        alarm.whenElapsed = minTime;
                        if (alarm.maxWhenElapsed < minTime) {
                            alarm.maxWhenElapsed = minTime;
                        }
                        setImplLocked(alarm, true, false);
                    } else {
                        alarm.count = 1;
                        triggerList.add(alarm);
                        if ((alarm.flags & 2) != 0) {
                            EventLogTags.writeDeviceIdleWakeFromIdle(this.mPendingIdleUntil != null ? 1 : 0, alarm.statsTag);
                        }
                        if (this.mPendingIdleUntil == alarm) {
                            this.mPendingIdleUntil = null;
                            rebatchAllAlarmsLocked(false);
                            restorePendingWhileIdleAlarmsLocked();
                        }
                        if (this.mNextWakeFromIdle == alarm) {
                            this.mNextWakeFromIdle = null;
                            rebatchAllAlarmsLocked(false);
                        }
                        if (alarm.repeatInterval > 0) {
                            alarm.count = (int) (((long) alarm.count) + ((nowELAPSED - alarm.whenElapsed) / alarm.repeatInterval));
                            long delta = ((long) alarm.count) * alarm.repeatInterval;
                            long nextElapsed = alarm.whenElapsed + delta;
                            if (mSupportAlarmGrouping && this.mAmPlus != null) {
                                maxElapsed = this.mAmPlus.getMaxTriggerTime(alarm.type, nextElapsed, alarm.windowLength, alarm.repeatInterval, alarm.operation, mAlarmMode, true);
                            } else {
                                maxElapsed = maxTriggerTime(nowELAPSED, nextElapsed, alarm.repeatInterval);
                            }
                            alarm.needGrouping = true;
                            setImplLocked(alarm.type, alarm.when + delta, nextElapsed, alarm.windowLength, maxElapsed, alarm.repeatInterval, alarm.operation, null, null, alarm.flags, true, alarm.workSource, alarm.alarmClock, alarm.uid, alarm.packageName, alarm.needGrouping);
                        }
                        if (alarm.wakeup) {
                            hasWakeup = true;
                        }
                        if (alarm.alarmClock != null) {
                            this.mNextAlarmClockMayChange = true;
                        }
                    }
                }
            }
        }
        this.mCurrentSeq++;
        calculateDeliveryPriorities(triggerList);
        Collections.sort(triggerList, this.mAlarmDispatchComparator);
        if (localLOGV) {
            for (int i2 = 0; i2 < triggerList.size(); i2++) {
                Slog.v(TAG, "Triggering alarm #" + i2 + ": " + triggerList.get(i2));
            }
        }
        return hasWakeup;
    }

    public static class IncreasingTimeOrder implements Comparator<Alarm> {
        @Override
        public int compare(Alarm a1, Alarm a2) {
            long when1 = a1.when;
            long when2 = a2.when;
            if (when1 > when2) {
                return 1;
            }
            if (when1 < when2) {
                return -1;
            }
            return 0;
        }
    }

    private static class Alarm {
        public final AlarmManager.AlarmClockInfo alarmClock;
        public int count;
        public final int creatorUid;
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
        public final String statsTag;
        public final int type;
        public final int uid;
        public final boolean wakeup;
        public long when;
        public long whenElapsed;
        public long windowLength;
        public final WorkSource workSource;

        public Alarm(int _type, long _when, long _whenElapsed, long _windowLength, long _maxWhen, long _interval, PendingIntent _op, IAlarmListener _rec, String _listenerTag, WorkSource _ws, int _flags, AlarmManager.AlarmClockInfo _info, int _uid, String _pkgName, boolean mNeedGrouping) {
            this.type = _type;
            this.origWhen = _when;
            boolean z = _type == 2 || _type == 0;
            this.wakeup = z;
            this.when = _when;
            this.whenElapsed = _whenElapsed;
            this.windowLength = _windowLength;
            this.maxWhenElapsed = _maxWhen;
            this.repeatInterval = _interval;
            this.operation = _op;
            this.listener = _rec;
            this.listenerTag = _listenerTag;
            this.statsTag = makeTag(_op, _listenerTag, _type);
            this.workSource = _ws;
            this.flags = _flags;
            this.alarmClock = _info;
            this.uid = _uid;
            this.packageName = _pkgName;
            this.needGrouping = mNeedGrouping;
            this.creatorUid = this.operation != null ? this.operation.getCreatorUid() : this.uid;
        }

        public static String makeTag(PendingIntent pi, String tag, int type) {
            String alarmString = (type == 2 || type == 0) ? "*walarm*:" : "*alarm*:";
            return pi != null ? pi.getTag(alarmString) : alarmString + tag;
        }

        public WakeupEvent makeWakeupEvent(long nowRTC) {
            String action;
            int i = this.creatorUid;
            if (this.operation != null) {
                action = this.operation.getIntent().getAction();
            } else {
                action = "<listener>:" + this.listenerTag;
            }
            return new WakeupEvent(nowRTC, i, action);
        }

        public boolean matches(PendingIntent pi, IAlarmListener rec) {
            if (this.operation != null) {
                return this.operation.equals(pi);
            }
            if (rec != null) {
                return this.listener.asBinder().equals(rec.asBinder());
            }
            return false;
        }

        public boolean matches(String packageName) {
            if (this.operation != null) {
                return packageName.equals(this.operation.getTargetPackage());
            }
            return packageName.equals(this.packageName);
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
            if (this.operation != null) {
                sb.append(this.operation.getTargetPackage());
            } else {
                sb.append(this.packageName);
            }
            sb.append('}');
            return sb.toString();
        }

        public void dump(PrintWriter pw, String prefix, long nowRTC, long nowELAPSED, SimpleDateFormat sdf) {
            boolean isRtc = this.type == 1 || this.type == 0;
            pw.print(prefix);
            pw.print("tag=");
            pw.println(this.statsTag);
            pw.print(prefix);
            pw.print("type=");
            pw.print(this.type);
            pw.print(" whenElapsed=");
            TimeUtils.formatDuration(this.whenElapsed, nowELAPSED, pw);
            pw.print(" when=");
            if (isRtc) {
                pw.print(sdf.format(new Date(this.when)));
            } else {
                TimeUtils.formatDuration(this.when, nowELAPSED, pw);
            }
            pw.println();
            pw.print(prefix);
            pw.print("window=");
            TimeUtils.formatDuration(this.windowLength, pw);
            pw.print(" repeatInterval=");
            pw.print(this.repeatInterval);
            pw.print(" count=");
            pw.print(this.count);
            pw.print(" flags=0x");
            pw.println(Integer.toHexString(this.flags));
            if (this.alarmClock != null) {
                pw.print(prefix);
                pw.println("Alarm clock:");
                pw.print(prefix);
                pw.print("  triggerTime=");
                pw.println(sdf.format(new Date(this.alarmClock.getTriggerTime())));
                pw.print(prefix);
                pw.print("  showIntent=");
                pw.println(this.alarmClock.getShowIntent());
            }
            pw.print(prefix);
            pw.print("operation=");
            pw.println(this.operation);
            if (this.listener == null) {
                return;
            }
            pw.print(prefix);
            pw.print("listener=");
            pw.println(this.listener.asBinder());
        }
    }

    void recordWakeupAlarms(ArrayList<Batch> batches, long nowELAPSED, long nowRTC) {
        int numBatches = batches.size();
        for (int nextBatch = 0; nextBatch < numBatches; nextBatch++) {
            Batch b = batches.get(nextBatch);
            if (b.start > nowELAPSED) {
                return;
            }
            int numAlarms = b.alarms.size();
            for (int nextAlarm = 0; nextAlarm < numAlarms; nextAlarm++) {
                Alarm a = b.alarms.get(nextAlarm);
                this.mRecentWakeups.add(a.makeWakeupEvent(nowRTC));
            }
        }
    }

    long currentNonWakeupFuzzLocked(long nowELAPSED) {
        long j = nowELAPSED - this.mNonInteractiveStartTime;
        return 0L;
    }

    static int fuzzForDuration(long duration) {
        if (duration < 900000) {
            return (int) duration;
        }
        if (duration < 5400000) {
            return 900000;
        }
        return ProcessList.PSS_MAX_INTERVAL;
    }

    boolean checkAllowNonWakeupDelayLocked(long nowELAPSED) {
        if (this.mInteractive || this.mLastAlarmDeliveryTime <= 0) {
            return false;
        }
        if (this.mPendingNonWakeupAlarms.size() > 0 && this.mNextNonWakeupDeliveryTime < nowELAPSED) {
            return false;
        }
        long timeSinceLast = nowELAPSED - this.mLastAlarmDeliveryTime;
        return timeSinceLast <= currentNonWakeupFuzzLocked(nowELAPSED);
    }

    void deliverAlarmsLocked(ArrayList<Alarm> triggerList, long nowELAPSED) {
        this.mLastAlarmDeliveryTime = nowELAPSED;
        long nowRTC = System.currentTimeMillis();
        this.mNeedRebatchForRepeatingAlarm = false;
        boolean openLteGateSuccess = false;
        if (this.dataShapingManager != null) {
            try {
                openLteGateSuccess = this.dataShapingManager.openLteDataUpLinkGate(false);
            } catch (Exception e) {
                Log.e(TAG, "Error openLteDataUpLinkGate false" + e);
            }
        } else {
            Slog.v(TAG, "dataShapingManager is null");
        }
        Slog.v(TAG, "openLteGateSuccess = " + openLteGateSuccess);
        int i = 0;
        while (i < triggerList.size()) {
            Alarm alarm = triggerList.get(i);
            boolean allowWhileIdle = (alarm.flags & 4) != 0;
            updatePoweroffAlarm(nowRTC);
            synchronized (this.mDMLock) {
                if (!this.mDMEnable || !this.mPPLEnable) {
                    FreeDmIntent(triggerList, this.mDmFreeList, nowELAPSED, this.mDmResendList);
                }
            }
        }
        if (this.mNeedRebatchForRepeatingAlarm) {
            Slog.v(TAG, " deliverAlarmsLocked removeInvalidAlarmLocked then rebatch ");
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
            return;
        }
        return;
        i++;
    }

    private class AlarmThread extends Thread {
        public AlarmThread() {
            super(AlarmManagerService.TAG);
        }

        @Override
        public void run() {
            long lastTimeChangeClockTime;
            long expectedClockTime;
            ArrayList<Alarm> triggerList = new ArrayList<>();
            while (true) {
                if (SystemProperties.get("ro.mtk_ipo_support").equals("1") && AlarmManagerService.this.mIPOShutdown) {
                    try {
                        if (AlarmManagerService.this.mNativeData != -1) {
                            synchronized (AlarmManagerService.this.mLock) {
                                AlarmManagerService.this.mAlarmBatches.clear();
                            }
                        }
                        synchronized (AlarmManagerService.this.mWaitThreadlock) {
                            AlarmManagerService.this.mWaitThreadlock.wait();
                        }
                    } catch (InterruptedException e) {
                        Slog.v(AlarmManagerService.TAG, "InterruptedException ");
                    }
                }
                int result = AlarmManagerService.this.waitForAlarm(AlarmManagerService.this.mNativeData);
                AlarmManagerService.this.mLastWakeup = SystemClock.elapsedRealtime();
                triggerList.clear();
                long nowRTC = System.currentTimeMillis();
                long nowELAPSED = SystemClock.elapsedRealtime();
                if ((65536 & result) != 0) {
                    synchronized (AlarmManagerService.this.mLock) {
                        lastTimeChangeClockTime = AlarmManagerService.this.mLastTimeChangeClockTime;
                        expectedClockTime = lastTimeChangeClockTime + (nowELAPSED - AlarmManagerService.this.mLastTimeChangeRealtime);
                    }
                    if (lastTimeChangeClockTime == 0 || nowRTC < expectedClockTime - 500 || nowRTC > 500 + expectedClockTime) {
                        if (AlarmManagerService.DEBUG_BATCH) {
                            Slog.v(AlarmManagerService.TAG, "Time changed notification from kernel; rebatching");
                        }
                        AlarmManagerService.this.removeImpl(AlarmManagerService.this.mTimeTickSender);
                        AlarmManagerService.this.rebatchAllAlarms();
                        AlarmManagerService.this.mClockReceiver.scheduleTimeTickEvent();
                        synchronized (AlarmManagerService.this.mLock) {
                            AlarmManagerService.this.mNumTimeChanged++;
                            AlarmManagerService.this.mLastTimeChangeClockTime = nowRTC;
                            AlarmManagerService.this.mLastTimeChangeRealtime = nowELAPSED;
                        }
                        Intent intent = new Intent("android.intent.action.TIME_SET");
                        intent.addFlags(603979776);
                        AlarmManagerService.this.getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
                        result |= 5;
                    }
                }
                if (result != 65536) {
                    synchronized (AlarmManagerService.this.mLock) {
                        if (AlarmManagerService.localLOGV) {
                            Slog.v(AlarmManagerService.TAG, "Checking for alarms... rtc=" + nowRTC + ", elapsed=" + nowELAPSED);
                        }
                        boolean hasWakeup = AlarmManagerService.this.triggerAlarmsLocked(triggerList, nowELAPSED, nowRTC);
                        if (!hasWakeup && AlarmManagerService.this.checkAllowNonWakeupDelayLocked(nowELAPSED)) {
                            if (AlarmManagerService.this.mPendingNonWakeupAlarms.size() == 0) {
                                AlarmManagerService.this.mStartCurrentDelayTime = nowELAPSED;
                                AlarmManagerService.this.mNextNonWakeupDeliveryTime = ((AlarmManagerService.this.currentNonWakeupFuzzLocked(nowELAPSED) * 3) / 2) + nowELAPSED;
                            }
                            AlarmManagerService.this.mPendingNonWakeupAlarms.addAll(triggerList);
                            AlarmManagerService.this.mNumDelayedAlarms += triggerList.size();
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        } else {
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                            if (AlarmManagerService.this.mPendingNonWakeupAlarms.size() > 0) {
                                AlarmManagerService.this.calculateDeliveryPriorities(AlarmManagerService.this.mPendingNonWakeupAlarms);
                                triggerList.addAll(AlarmManagerService.this.mPendingNonWakeupAlarms);
                                Collections.sort(triggerList, AlarmManagerService.this.mAlarmDispatchComparator);
                                long thisDelayTime = nowELAPSED - AlarmManagerService.this.mStartCurrentDelayTime;
                                AlarmManagerService.this.mTotalDelayTime += thisDelayTime;
                                if (AlarmManagerService.this.mMaxDelayTime < thisDelayTime) {
                                    AlarmManagerService.this.mMaxDelayTime = thisDelayTime;
                                }
                                AlarmManagerService.this.mPendingNonWakeupAlarms.clear();
                            }
                            AlarmManagerService.this.deliverAlarmsLocked(triggerList, nowELAPSED);
                        }
                    }
                } else {
                    AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                }
            }
        }
    }

    void setWakelockWorkSource(PendingIntent pi, WorkSource ws, int type, String tag, int knownUid, boolean first) {
        try {
            boolean unimportant = pi == this.mTimeTickSender;
            this.mWakeLock.setUnimportantForLogging(unimportant);
            if (first || this.mLastWakeLockUnimportantForLogging) {
                this.mWakeLock.setHistoryTag(tag);
            } else {
                this.mWakeLock.setHistoryTag(null);
            }
            this.mLastWakeLockUnimportantForLogging = unimportant;
        } catch (Exception e) {
        }
        if (ws != null) {
            this.mWakeLock.setWorkSource(ws);
            return;
        }
        int uid = knownUid >= 0 ? knownUid : ActivityManagerNative.getDefault().getUidForIntentSender(pi.getTarget());
        if (uid >= 0) {
            this.mWakeLock.setWorkSource(new WorkSource(uid));
            return;
        }
        this.mWakeLock.setWorkSource(null);
    }

    private class AlarmHandler extends Handler {
        public static final int ALARM_EVENT = 1;
        public static final int LISTENER_TIMEOUT = 3;
        public static final int REPORT_ALARMS_ACTIVE = 4;
        public static final int SEND_NEXT_ALARM_CLOCK_CHANGED = 2;

        public AlarmHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ArrayList<Alarm> triggerList = new ArrayList<>();
                    synchronized (AlarmManagerService.this.mLock) {
                        long nowRTC = System.currentTimeMillis();
                        long nowELAPSED = SystemClock.elapsedRealtime();
                        AlarmManagerService.this.triggerAlarmsLocked(triggerList, nowELAPSED, nowRTC);
                        AlarmManagerService.this.updateNextAlarmClockLocked();
                    }
                    for (int i = 0; i < triggerList.size(); i++) {
                        Alarm alarm = triggerList.get(i);
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
                    AlarmManagerService.this.mDeliveryTracker.alarmTimedOut((IBinder) msg.obj);
                    return;
                case 4:
                    if (AlarmManagerService.this.mLocalDeviceIdleController == null) {
                        return;
                    }
                    AlarmManagerService.this.mLocalDeviceIdleController.setAlarmsActive(msg.arg1 != 0);
                    return;
                default:
                    return;
            }
        }
    }

    class ClockReceiver extends BroadcastReceiver {
        public ClockReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.DATE_CHANGED");
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.TIME_TICK")) {
                if (AlarmManagerService.DEBUG_BATCH) {
                    Slog.v(AlarmManagerService.TAG, "Received TIME_TICK alarm; rescheduling");
                }
                Slog.v(AlarmManagerService.TAG, "mSupportAlarmGrouping = " + AlarmManagerService.mSupportAlarmGrouping + "  mAmPlus = " + AlarmManagerService.this.mAmPlus);
                scheduleTimeTickEvent();
                return;
            }
            if (!intent.getAction().equals("android.intent.action.DATE_CHANGED")) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(SystemProperties.get(AlarmManagerService.TIMEZONE_PROPERTY));
            int gmtOffset = zone.getOffset(System.currentTimeMillis());
            AlarmManagerService.this.setKernelTimezone(AlarmManagerService.this.mNativeData, -(gmtOffset / 60000));
            scheduleDateChangedEvent();
        }

        public void scheduleTimeTickEvent() {
            long currentTime = System.currentTimeMillis();
            long nextTime = 60000 * ((currentTime / 60000) + 1);
            long tickEventDelay = nextTime - currentTime;
            AlarmManagerService.this.setImpl(3, SystemClock.elapsedRealtime() + tickEventDelay, 0L, 0L, AlarmManagerService.this.mTimeTickSender, null, null, 1, null, null, Process.myUid(), "android");
        }

        public void scheduleDateChangedEvent() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(10, 0);
            calendar.set(12, 0);
            calendar.set(13, 0);
            calendar.set(14, 0);
            calendar.add(5, 1);
            AlarmManagerService.this.setImpl(1, calendar.getTimeInMillis(), 0L, 0L, AlarmManagerService.this.mDateChangeSender, null, null, 1, null, null, Process.myUid(), "android");
        }
    }

    class InteractiveStateReceiver extends BroadcastReceiver {
        public InteractiveStateReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.setPriority(1000);
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
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
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_RESTARTED");
            filter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
            filter.addDataScheme("package");
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            sdFilter.addAction("android.intent.action.USER_STOPPED");
            sdFilter.addAction("android.intent.action.UID_REMOVED");
            AlarmManagerService.this.getContext().registerReceiver(this, sdFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String pkg;
            synchronized (AlarmManagerService.this.mLock) {
                Slog.d(AlarmManagerService.TAG, "UninstallReceiver  action = " + intent.getAction());
                String action = intent.getAction();
                String[] pkgList = null;
                if ("android.intent.action.QUERY_PACKAGE_RESTART".equals(action)) {
                    for (String packageName : intent.getStringArrayExtra("android.intent.extra.PACKAGES")) {
                        if (AlarmManagerService.this.lookForPackageLocked(packageName) && !"android".equals(packageName)) {
                            setResultCode(-1);
                            return;
                        }
                    }
                    return;
                }
                if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                    pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    int userHandle = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userHandle >= 0) {
                        AlarmManagerService.this.removeUserLocked(userHandle);
                    }
                } else if ("android.intent.action.UID_REMOVED".equals(action)) {
                    int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                    if (uid >= 0) {
                        AlarmManagerService.this.mLastAllowWhileIdleDispatch.delete(uid);
                    }
                } else {
                    if ("android.intent.action.PACKAGE_REMOVED".equals(action) && intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        return;
                    }
                    Uri data = intent.getData();
                    if (data != null && (pkg = data.getSchemeSpecificPart()) != null) {
                        pkgList = new String[]{pkg};
                    }
                }
                if (pkgList != null && pkgList.length > 0) {
                    for (String pkg2 : pkgList) {
                        if (!"android".equals(pkg2)) {
                            AlarmManagerService.this.removeLocked(pkg2);
                            AlarmManagerService.this.mPriorities.remove(pkg2);
                            for (int i = AlarmManagerService.this.mBroadcastStats.size() - 1; i >= 0; i--) {
                                ArrayMap<String, BroadcastStats> uidStats = AlarmManagerService.this.mBroadcastStats.valueAt(i);
                                if (uidStats.remove(pkg2) != null && uidStats.size() <= 0) {
                                    AlarmManagerService.this.mBroadcastStats.removeAt(i);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    final class UidObserver extends IUidObserver.Stub {
        UidObserver() {
        }

        public void onUidStateChanged(int uid, int procState) throws RemoteException {
        }

        public void onUidGone(int uid) throws RemoteException {
        }

        public void onUidActive(int uid) throws RemoteException {
        }

        public void onUidIdle(int uid) throws RemoteException {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.removeForStoppedLocked(uid);
            }
        }
    }

    private final BroadcastStats getStatsLocked(PendingIntent pi) {
        String pkg = pi.getCreatorPackage();
        int uid = pi.getCreatorUid();
        return getStatsLocked(uid, pkg);
    }

    private final BroadcastStats getStatsLocked(int uid, String pkgName) {
        ArrayMap<String, BroadcastStats> uidStats = this.mBroadcastStats.get(uid);
        if (uidStats == null) {
            uidStats = new ArrayMap<>();
            this.mBroadcastStats.put(uid, uidStats);
        }
        BroadcastStats bs = uidStats.get(pkgName);
        if (bs == null) {
            BroadcastStats bs2 = new BroadcastStats(uid, pkgName);
            uidStats.put(pkgName, bs2);
            return bs2;
        }
        return bs;
    }

    class DeliveryTracker extends IAlarmCompleteListener.Stub implements PendingIntent.OnFinished {
        DeliveryTracker() {
        }

        private InFlight removeLocked(PendingIntent pi, Intent intent) {
            for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                if (AlarmManagerService.this.mInFlight.get(i).mPendingIntent == pi) {
                    return AlarmManagerService.this.mInFlight.remove(i);
                }
            }
            AlarmManagerService.this.mLog.w("No in-flight alarm for " + pi + " " + intent);
            return null;
        }

        private InFlight removeLocked(IBinder listener) {
            for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                if (AlarmManagerService.this.mInFlight.get(i).mListener == listener) {
                    return AlarmManagerService.this.mInFlight.remove(i);
                }
            }
            AlarmManagerService.this.mLog.w("No in-flight alarm for listener " + listener);
            return null;
        }

        private void updateStatsLocked(InFlight inflight) {
            long nowELAPSED = SystemClock.elapsedRealtime();
            BroadcastStats bs = inflight.mBroadcastStats;
            bs.nesting--;
            if (bs.nesting <= 0) {
                bs.nesting = 0;
                bs.aggregateTime += nowELAPSED - bs.startTime;
            }
            FilterStats fs = inflight.mFilterStats;
            fs.nesting--;
            if (fs.nesting <= 0) {
                fs.nesting = 0;
                fs.aggregateTime += nowELAPSED - fs.startTime;
            }
            if (inflight.mWorkSource != null && inflight.mWorkSource.size() > 0) {
                for (int wi = 0; wi < inflight.mWorkSource.size(); wi++) {
                    ActivityManagerNative.noteAlarmFinish(inflight.mPendingIntent, inflight.mWorkSource.get(wi), inflight.mTag);
                }
                return;
            }
            ActivityManagerNative.noteAlarmFinish(inflight.mPendingIntent, inflight.mUid, inflight.mTag);
        }

        private void updateTrackingLocked(InFlight inflight) {
            if (inflight != null) {
                updateStatsLocked(inflight);
            }
            AlarmManagerService alarmManagerService = AlarmManagerService.this;
            alarmManagerService.mBroadcastRefCount--;
            if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                AlarmManagerService.this.mHandler.obtainMessage(4, 0).sendToTarget();
                AlarmManagerService.this.mWakeLock.release();
                if (AlarmManagerService.this.mInFlight.size() <= 0) {
                    return;
                }
                AlarmManagerService.this.mLog.w("Finished all dispatches with " + AlarmManagerService.this.mInFlight.size() + " remaining inflights");
                for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                    AlarmManagerService.this.mLog.w("  Remaining #" + i + ": " + AlarmManagerService.this.mInFlight.get(i));
                }
                AlarmManagerService.this.mInFlight.clear();
                return;
            }
            if (AlarmManagerService.this.mInFlight.size() > 0) {
                InFlight inFlight = AlarmManagerService.this.mInFlight.get(0);
                AlarmManagerService.this.setWakelockWorkSource(inFlight.mPendingIntent, inFlight.mWorkSource, inFlight.mAlarmType, inFlight.mTag, -1, false);
            } else {
                AlarmManagerService.this.mLog.w("Alarm wakelock still held but sent queue empty");
                AlarmManagerService.this.mWakeLock.setWorkSource(null);
            }
        }

        public void alarmComplete(IBinder who) {
            if (who == null) {
                Slog.w(AlarmManagerService.TAG, "Invalid alarmComplete: uid=" + Binder.getCallingUid() + " pid=" + Binder.getCallingPid());
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.mHandler.removeMessages(3, who);
                    InFlight inflight = removeLocked(who);
                    if (inflight != null) {
                        if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                            Slog.i(AlarmManagerService.TAG, "alarmComplete() from " + who);
                        }
                        updateTrackingLocked(inflight);
                    } else if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Late alarmComplete() from " + who);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public void onSendFinished(PendingIntent pi, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            Slog.d(AlarmManagerService.TAG, "onSendFinished begin");
            synchronized (AlarmManagerService.this.mLock) {
                updateTrackingLocked(removeLocked(pi, intent));
            }
        }

        public void alarmTimedOut(IBinder who) {
            synchronized (AlarmManagerService.this.mLock) {
                InFlight inflight = removeLocked(who);
                if (inflight != null) {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Alarm listener " + who + " timed out in delivery");
                    }
                    updateTrackingLocked(inflight);
                } else if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                    Slog.i(AlarmManagerService.TAG, "Spurious timeout of listener " + who);
                }
            }
        }

        public void deliverLocked(Alarm alarm, long nowELAPSED, boolean allowWhileIdle) {
            if (alarm.operation != null) {
                try {
                    alarm.operation.send(AlarmManagerService.this.getContext(), 0, AlarmManagerService.this.mBackgroundIntent.putExtra("android.intent.extra.ALARM_COUNT", alarm.count), AlarmManagerService.this.mDeliveryTracker, AlarmManagerService.this.mHandler, null, allowWhileIdle ? AlarmManagerService.this.mIdleOptions : null);
                    Slog.v(AlarmManagerService.TAG, "sending alarm " + alarm + " success");
                } catch (PendingIntent.CanceledException e) {
                    if (alarm.repeatInterval > 0) {
                        AlarmManagerService.this.mNeedRebatchForRepeatingAlarm = !AlarmManagerService.this.removeInvalidAlarmLocked(alarm.operation, alarm.listener) ? AlarmManagerService.this.mNeedRebatchForRepeatingAlarm : true;
                        return;
                    }
                    return;
                }
            } else {
                try {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.v(AlarmManagerService.TAG, "Alarm to uid=" + alarm.uid + " listener=" + alarm.listener.asBinder());
                    }
                    alarm.listener.doAlarm(this);
                    AlarmManagerService.this.mHandler.sendMessageDelayed(AlarmManagerService.this.mHandler.obtainMessage(3, alarm.listener.asBinder()), AlarmManagerService.this.mConstants.LISTENER_TIMEOUT);
                } catch (Exception e2) {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Alarm undeliverable to listener " + alarm.listener.asBinder(), e2);
                        return;
                    }
                    return;
                }
            }
            if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                AlarmManagerService.this.setWakelockWorkSource(alarm.operation, alarm.workSource, alarm.type, alarm.statsTag, alarm.operation == null ? alarm.uid : -1, true);
                AlarmManagerService.this.mWakeLock.acquire();
                AlarmManagerService.this.mHandler.obtainMessage(4, 1).sendToTarget();
            }
            InFlight inflight = new InFlight(AlarmManagerService.this, alarm.operation, alarm.listener, alarm.workSource, alarm.uid, alarm.packageName, alarm.type, alarm.statsTag, nowELAPSED);
            AlarmManagerService.this.mInFlight.add(inflight);
            AlarmManagerService.this.mBroadcastRefCount++;
            if (allowWhileIdle) {
                AlarmManagerService.this.mLastAllowWhileIdleDispatch.put(alarm.uid, nowELAPSED);
            }
            BroadcastStats bs = inflight.mBroadcastStats;
            bs.count++;
            if (bs.nesting == 0) {
                bs.nesting = 1;
                bs.startTime = nowELAPSED;
            } else {
                bs.nesting++;
            }
            FilterStats fs = inflight.mFilterStats;
            fs.count++;
            if (fs.nesting == 0) {
                fs.nesting = 1;
                fs.startTime = nowELAPSED;
            } else {
                fs.nesting++;
            }
            if (alarm.type == 2 || alarm.type == 0) {
                bs.numWakeup++;
                fs.numWakeup++;
                if (alarm.workSource == null || alarm.workSource.size() <= 0) {
                    ActivityManagerNative.noteWakeupAlarm(alarm.operation, alarm.uid, alarm.packageName, alarm.statsTag);
                    return;
                }
                for (int wi = 0; wi < alarm.workSource.size(); wi++) {
                    String wsName = alarm.workSource.getName(wi);
                    PendingIntent pendingIntent = alarm.operation;
                    int i = alarm.workSource.get(wi);
                    if (wsName == null) {
                        wsName = alarm.packageName;
                    }
                    ActivityManagerNative.noteWakeupAlarm(pendingIntent, i, wsName, alarm.statsTag);
                }
            }
        }
    }

    class DMReceiver extends BroadcastReceiver {
        public DMReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(NotificationManagerService.OMADM_LAWMO_LOCK);
            filter.addAction(NotificationManagerService.OMADM_LAWMO_UNLOCK);
            filter.addAction(NotificationManagerService.PPL_LOCK);
            filter.addAction(NotificationManagerService.PPL_UNLOCK);
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(NotificationManagerService.OMADM_LAWMO_LOCK)) {
                AlarmManagerService.this.mDMEnable = false;
                return;
            }
            if (action.equals(NotificationManagerService.OMADM_LAWMO_UNLOCK)) {
                AlarmManagerService.this.mDMEnable = true;
                AlarmManagerService.this.enableDm();
            } else if (action.equals(NotificationManagerService.PPL_LOCK)) {
                AlarmManagerService.this.mPPLEnable = false;
            } else {
                if (!action.equals(NotificationManagerService.PPL_UNLOCK)) {
                    return;
                }
                AlarmManagerService.this.mPPLEnable = true;
                AlarmManagerService.this.enableDm();
            }
        }
    }

    public int enableDm() {
        synchronized (this.mDMLock) {
            if (this.mDMEnable && this.mPPLEnable) {
                resendDmPendingList(this.mDmResendList);
                this.mDmResendList = null;
                this.mDmResendList = new ArrayList<>();
            }
        }
        return -1;
    }

    private void FreeDmIntent(ArrayList<Alarm> triggerList, ArrayList<PendingIntent> mDmFreeList, long nowELAPSED, ArrayList<Alarm> resendList) {
        for (Alarm alarm : triggerList) {
            boolean isFreeIntent = false;
            if (alarm.operation == null) {
                Slog.v(TAG, "FreeDmIntent skip with null operation APP listener(" + alarm.listenerTag + ") : type = " + alarm.type + " triggerAtTime = " + alarm.when);
            } else {
                int i = 0;
                while (true) {
                    try {
                        if (i >= mDmFreeList.size()) {
                            break;
                        }
                        if (alarm.operation.equals(mDmFreeList.get(i))) {
                            if (localLOGV) {
                                Slog.v(TAG, "sending alarm " + alarm);
                            }
                            alarm.operation.send(getContext(), 0, this.mBackgroundIntent.putExtra("android.intent.extra.ALARM_COUNT", alarm.count), this.mDeliveryTracker, this.mHandler);
                            if (this.mBroadcastRefCount == 0) {
                                setWakelockWorkSource(alarm.operation, alarm.workSource, alarm.type, alarm.statsTag, alarm.uid, true);
                                this.mWakeLock.acquire();
                            }
                            InFlight inflight = new InFlight(this, alarm.operation, alarm.listener, alarm.workSource, alarm.uid, alarm.packageName, alarm.type, alarm.statsTag, 0L);
                            this.mInFlight.add(inflight);
                            this.mBroadcastRefCount++;
                            BroadcastStats bs = inflight.mBroadcastStats;
                            bs.count++;
                            if (bs.nesting == 0) {
                                bs.nesting = 1;
                                bs.startTime = nowELAPSED;
                            } else {
                                bs.nesting++;
                            }
                            FilterStats fs = inflight.mFilterStats;
                            fs.count++;
                            if (fs.nesting == 0) {
                                fs.nesting = 1;
                                fs.startTime = nowELAPSED;
                            } else {
                                fs.nesting++;
                            }
                            if (alarm.type == 2 || alarm.type == 0) {
                                bs.numWakeup++;
                                fs.numWakeup++;
                            }
                            isFreeIntent = true;
                        } else {
                            i++;
                        }
                    } catch (PendingIntent.CanceledException e) {
                        if (alarm.repeatInterval > 0) {
                        }
                    }
                }
                if (!isFreeIntent) {
                    resendList.add(alarm);
                }
            }
        }
    }

    private void resendDmPendingList(ArrayList<Alarm> DmResendList) {
        for (Alarm alarm : DmResendList) {
            if (alarm.operation == null) {
                Slog.v(TAG, "resendDmPendingList skip with null operation, APP listener(" + alarm.listenerTag + ") : type = " + alarm.type + " triggerAtTime = " + alarm.when);
            } else {
                try {
                    if (localLOGV) {
                        Slog.v(TAG, "sending alarm " + alarm);
                    }
                    alarm.operation.send(getContext(), 0, this.mBackgroundIntent.putExtra("android.intent.extra.ALARM_COUNT", alarm.count), this.mDeliveryTracker, this.mHandler);
                    if (this.mBroadcastRefCount == 0) {
                        setWakelockWorkSource(alarm.operation, alarm.workSource, alarm.type, alarm.statsTag, alarm.uid, true);
                        this.mWakeLock.acquire();
                    }
                    InFlight inflight = new InFlight(this, alarm.operation, alarm.listener, alarm.workSource, alarm.uid, alarm.packageName, alarm.type, alarm.statsTag, 0L);
                    this.mInFlight.add(inflight);
                    this.mBroadcastRefCount++;
                    BroadcastStats bs = inflight.mBroadcastStats;
                    bs.count++;
                    if (bs.nesting == 0) {
                        bs.nesting = 1;
                        bs.startTime = SystemClock.elapsedRealtime();
                    } else {
                        bs.nesting++;
                    }
                    FilterStats fs = inflight.mFilterStats;
                    fs.count++;
                    if (fs.nesting == 0) {
                        fs.nesting = 1;
                        fs.startTime = SystemClock.elapsedRealtime();
                    } else {
                        fs.nesting++;
                    }
                    if (alarm.type == 2 || alarm.type == 0) {
                        bs.numWakeup++;
                        fs.numWakeup++;
                    }
                } catch (PendingIntent.CanceledException e) {
                    if (alarm.repeatInterval > 0) {
                    }
                }
            }
        }
    }

    private boolean isBootFromAlarm(int fd) {
        return bootFromAlarm(fd);
    }

    private void updatePoweroffAlarm(long nowRTC) {
        synchronized (this.mPowerOffAlarmLock) {
            if (this.mPoweroffAlarms.size() == 0) {
                return;
            }
            if (this.mPoweroffAlarms.get(0).when > nowRTC) {
                return;
            }
            Iterator<Alarm> it = this.mPoweroffAlarms.iterator();
            while (it.hasNext()) {
                Alarm alarm = it.next();
                if (alarm.when > nowRTC) {
                    break;
                }
                Slog.w(TAG, "power off alarm update deleted");
                it.remove();
            }
            if (this.mPoweroffAlarms.size() > 0) {
                resetPoweroffAlarm(this.mPoweroffAlarms.get(0));
            }
        }
    }

    private int addPoweroffAlarmLocked(Alarm alarm) {
        ArrayList<Alarm> alarmList = this.mPoweroffAlarms;
        int index = Collections.binarySearch(alarmList, alarm, sIncreasingTimeOrder);
        if (index < 0) {
            index = (0 - index) - 1;
        }
        if (localLOGV) {
            Slog.v(TAG, "Adding alarm " + alarm + " at " + index);
        }
        alarmList.add(index, alarm);
        if (localLOGV) {
            Slog.v(TAG, "alarms: " + alarmList.size() + " type: " + alarm.type);
            int position = 0;
            for (Alarm a : alarmList) {
                Time time = new Time();
                time.set(a.when);
                String timeStr = time.format("%b %d %I:%M:%S %p");
                Slog.v(TAG, position + ": " + timeStr + " " + a.operation.getTargetPackage());
                position++;
            }
        }
        return index;
    }

    private void removePoweroffAlarmLocked(String packageName) {
        ArrayList<Alarm> alarmList = this.mPoweroffAlarms;
        if (alarmList.size() <= 0) {
            return;
        }
        Iterator<Alarm> it = alarmList.iterator();
        while (it.hasNext()) {
            Alarm alarm = it.next();
            if (alarm.operation.getTargetPackage().equals(packageName)) {
                it.remove();
            }
        }
    }

    private void resetPoweroffAlarm(Alarm alarm) {
        String setPackageName = alarm.operation.getTargetPackage();
        long latestTime = alarm.when;
        if (this.mNativeData != 0 && this.mNativeData != -1) {
            if (setPackageName.equals("com.android.deskclock")) {
                Slog.i(TAG, "mBootPackage = " + setPackageName + " set Prop 1");
                SystemProperties.set("persist.sys.bootpackage", "1");
                set(this.mNativeData, 6, latestTime / 1000, (latestTime % 1000) * 1000 * 1000);
            } else if (setPackageName.equals("com.mediatek.schpwronoff") || setPackageName.equals("com.mediatek.poweronofftest")) {
                Slog.i(TAG, "mBootPackage = " + setPackageName + " set Prop 2");
                SystemProperties.set("persist.sys.bootpackage", "2");
                set(this.mNativeData, 7, latestTime / 1000, (latestTime % 1000) * 1000 * 1000);
            } else {
                Slog.w(TAG, "unknown package (" + setPackageName + ") to set power off alarm");
            }
            Slog.i(TAG, "reset power off alarm is " + setPackageName);
            SystemProperties.set("sys.power_off_alarm", Long.toString(latestTime / 1000));
            return;
        }
        Slog.i(TAG, " do not set alarm to RTC when fd close ");
    }

    public void cancelPoweroffAlarmImpl(String name) {
        Slog.i(TAG, "remove power off alarm pacakge name " + name);
        synchronized (this.mPowerOffAlarmLock) {
            removePoweroffAlarmLocked(name);
            String bootReason = SystemProperties.get("persist.sys.bootpackage");
            if (bootReason != null && this.mNativeData != 0 && this.mNativeData != -1) {
                if (bootReason.equals("1") && name.equals("com.android.deskclock")) {
                    set(this.mNativeData, 6, 0L, 0L);
                    SystemProperties.set("sys.power_off_alarm", Long.toString(0L));
                } else if (bootReason.equals("2") && (name.equals("com.mediatek.schpwronoff") || name.equals("com.mediatek.poweronofftest"))) {
                    set(this.mNativeData, 7, 0L, 0L);
                    SystemProperties.set("sys.power_off_alarm", Long.toString(0L));
                }
            }
            if (this.mPoweroffAlarms.size() > 0) {
                resetPoweroffAlarm(this.mPoweroffAlarms.get(0));
            }
        }
    }

    private void shutdownCheckPoweroffAlarm() {
        Slog.i(TAG, "into shutdownCheckPoweroffAlarm()!!");
        long nowTime = System.currentTimeMillis();
        synchronized (this.mPowerOffAlarmLock) {
            ArrayList<Alarm> mTempPoweroffAlarms = new ArrayList<>();
            for (Alarm alarm : this.mPoweroffAlarms) {
                long latestTime = alarm.when;
                alarm.operation.getTargetPackage();
                if (latestTime - 30000 <= nowTime) {
                    Slog.i(TAG, "get target latestTime < 30S!!");
                    mTempPoweroffAlarms.add(alarm);
                }
            }
            for (Alarm alarm2 : mTempPoweroffAlarms) {
                long latestTime2 = alarm2.when;
                if (this.mNativeData != 0 && this.mNativeData != -1) {
                    set(this.mNativeData, alarm2.type, latestTime2 / 1000, (latestTime2 % 1000) * 1000 * 1000);
                }
            }
        }
        Slog.i(TAG, "away shutdownCheckPoweroffAlarm()!!");
    }

    public void removeFromAmsImpl(String packageName) {
        if (packageName == null) {
            return;
        }
        synchronized (this.mLock) {
            removeLocked(packageName);
        }
    }

    public boolean lookForPackageFromAmsImpl(String packageName) {
        boolean zLookForPackageLocked;
        if (packageName == null) {
            return false;
        }
        synchronized (this.mLock) {
            zLookForPackageLocked = lookForPackageLocked(packageName);
        }
        return zLookForPackageLocked;
    }
}
