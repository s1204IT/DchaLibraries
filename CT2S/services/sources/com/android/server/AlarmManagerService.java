package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.IAlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import com.android.internal.util.LocalLog;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
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
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

class AlarmManagerService extends SystemService {
    static final int ALARM_EVENT = 1;
    static final String ClockReceiver_TAG = "ClockReceiver";
    static final boolean DEBUG_ALARM_CLOCK = false;
    static final boolean DEBUG_BATCH = false;
    static final boolean DEBUG_VALIDATE = false;
    private static final int ELAPSED_REALTIME_MASK = 8;
    private static final int ELAPSED_REALTIME_WAKEUP_MASK = 4;
    static final int IS_WAKEUP_MASK = 5;
    private static final long LATE_ALARM_THRESHOLD = 10000;
    private static final long MIN_FUTURITY = 5000;
    static final long MIN_FUZZABLE_INTERVAL = 10000;
    private static final long MIN_INTERVAL = 60000;
    static final int PRIO_NORMAL = 2;
    static final int PRIO_TICK = 0;
    static final int PRIO_WAKEUP = 1;
    private static final int RTC_MASK = 2;
    private static final int RTC_WAKEUP_MASK = 1;
    static final String TAG = "AlarmManager";
    static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    static final int TIME_CHANGED_MASK = 65536;
    static final int TYPE_NONWAKEUP_MASK = 1;
    static final boolean WAKEUP_STATS = false;
    static final boolean localLOGV = false;
    final long RECENT_WAKEUP_PERIOD;
    final ArrayList<Batch> mAlarmBatches;
    final Comparator<Alarm> mAlarmDispatchComparator;
    int mBroadcastRefCount;
    final SparseArray<ArrayMap<String, BroadcastStats>> mBroadcastStats;
    ClockReceiver mClockReceiver;
    int mCurrentSeq;
    PendingIntent mDateChangeSender;
    final AlarmHandler mHandler;
    private final SparseArray<AlarmManager.AlarmClockInfo> mHandlerSparseAlarmClockArray;
    ArrayList<InFlight> mInFlight;
    boolean mInteractive;
    InteractiveStateReceiver mInteractiveStateReceiver;
    long mLastAlarmDeliveryTime;
    boolean mLastWakeLockUnimportantForLogging;
    final Object mLock;
    final LocalLog mLog;
    long mMaxDelayTime;
    long mNativeData;
    private final SparseArray<AlarmManager.AlarmClockInfo> mNextAlarmClockForUser;
    private boolean mNextAlarmClockMayChange;
    private long mNextNonWakeup;
    long mNextNonWakeupDeliveryTime;
    private long mNextPowerOn;
    private long mNextWakeup;
    long mNonInteractiveStartTime;
    long mNonInteractiveTime;
    int mNumDelayedAlarms;
    int mNumTimeChanged;
    ArrayList<Alarm> mPendingNonWakeupAlarms;
    private final SparseBooleanArray mPendingSendNextAlarmClockChangedForUser;
    final HashMap<String, PriorityClass> mPriorities;
    final LinkedList<WakeupEvent> mRecentWakeups;
    final ResultReceiver mResultReceiver;
    private final IBinder mService;
    long mStartCurrentDelayTime;
    PendingIntent mTimeTickSender;
    private final SparseArray<AlarmManager.AlarmClockInfo> mTmpSparseAlarmClockArray;
    long mTotalDelayTime;
    private UninstallReceiver mUninstallReceiver;
    PowerManager.WakeLock mWakeLock;
    static final Intent mBackgroundIntent = new Intent().addFlags(4);
    static final IncreasingTimeOrder sIncreasingTimeOrder = new IncreasingTimeOrder();
    private static final Intent NEXT_ALARM_CLOCK_CHANGED_INTENT = new Intent("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
    static final BatchTimeOrder sBatchOrder = new BatchTimeOrder();

    private native void clear(int i);

    private native void close(long j);

    private native long init();

    private native void set(long j, int i, long j2, long j3);

    private native int setKernelTime(long j, long j2);

    private native int setKernelTimezone(long j, int i);

    private native int waitForAlarm(long j);

    class PriorityClass {
        int priority = 2;
        int seq;

        PriorityClass() {
            this.seq = AlarmManagerService.this.mCurrentSeq - 1;
        }
    }

    class WakeupEvent {
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
        boolean standalone;
        long start;
        long startInMillis;

        Batch() {
            this.alarms = new ArrayList<>();
            this.start = 0L;
            this.startInMillis = 0L;
            this.end = JobStatus.NO_LATEST_RUNTIME;
        }

        Batch(Alarm seed) {
            this.alarms = new ArrayList<>();
            this.start = seed.whenElapsed;
            this.startInMillis = seed.when;
            this.end = seed.maxWhen;
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
            if (alarm.whenElapsed > this.start) {
                this.start = alarm.whenElapsed;
                this.startInMillis = alarm.when;
                newStart = true;
            }
            if (alarm.maxWhen < this.end) {
                this.end = alarm.maxWhen;
            }
            return newStart;
        }

        boolean remove(PendingIntent operation) {
            boolean didRemove = false;
            long newStart = 0;
            long newStartInMillis = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int i = 0;
            while (i < this.alarms.size()) {
                Alarm alarm = this.alarms.get(i);
                if (alarm.operation.equals(operation)) {
                    this.alarms.remove(i);
                    didRemove = true;
                    if (alarm.alarmClock != null) {
                        AlarmManagerService.this.mNextAlarmClockMayChange = true;
                    }
                } else {
                    if (alarm.whenElapsed > newStart) {
                        newStart = alarm.whenElapsed;
                        newStartInMillis = alarm.when;
                    }
                    if (alarm.maxWhen < newEnd) {
                        newEnd = alarm.maxWhen;
                    }
                    i++;
                }
            }
            if (didRemove) {
                this.start = newStart;
                this.startInMillis = newStartInMillis;
                this.end = newEnd;
            }
            return didRemove;
        }

        boolean remove(String packageName) {
            boolean didRemove = false;
            long newStart = 0;
            long newStartInMillis = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int i = 0;
            while (i < this.alarms.size()) {
                Alarm alarm = this.alarms.get(i);
                if (alarm.operation.getTargetPackage().equals(packageName)) {
                    this.alarms.remove(i);
                    didRemove = true;
                    if (alarm.alarmClock != null) {
                        AlarmManagerService.this.mNextAlarmClockMayChange = true;
                    }
                } else {
                    if (alarm.whenElapsed > newStart) {
                        newStart = alarm.whenElapsed;
                        newStartInMillis = alarm.when;
                    }
                    if (alarm.maxWhen < newEnd) {
                        newEnd = alarm.maxWhen;
                    }
                    i++;
                }
            }
            if (didRemove) {
                this.start = newStart;
                this.startInMillis = newStartInMillis;
                this.end = newEnd;
            }
            return didRemove;
        }

        boolean remove(int userHandle) {
            boolean didRemove = false;
            long newStart = 0;
            long newStartInMillis = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int i = 0;
            while (i < this.alarms.size()) {
                Alarm alarm = this.alarms.get(i);
                if (UserHandle.getUserId(alarm.operation.getCreatorUid()) == userHandle) {
                    this.alarms.remove(i);
                    didRemove = true;
                    if (alarm.alarmClock != null) {
                        AlarmManagerService.this.mNextAlarmClockMayChange = true;
                    }
                } else {
                    if (alarm.whenElapsed > newStart) {
                        newStart = alarm.whenElapsed;
                        newStartInMillis = alarm.when;
                    }
                    if (alarm.maxWhen < newEnd) {
                        newEnd = alarm.maxWhen;
                    }
                    i++;
                }
            }
            if (didRemove) {
                this.start = newStart;
                this.startInMillis = newStartInMillis;
                this.end = newEnd;
            }
            return didRemove;
        }

        boolean hasPackage(String packageName) {
            int N = this.alarms.size();
            for (int i = 0; i < N; i++) {
                Alarm a = this.alarms.get(i);
                if (a.operation.getTargetPackage().equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        boolean hasWakeups() {
            int N = this.alarms.size();
            for (int i = 0; i < N; i++) {
                Alarm a = this.alarms.get(i);
                if ((a.type & 1) == 0 || a.type == 5) {
                    return true;
                }
            }
            return false;
        }

        boolean hasPowerOn() {
            int N = this.alarms.size();
            for (int i = 0; i < N; i++) {
                Alarm a = this.alarms.get(i);
                if (a.type == 5) {
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
            if (this.standalone) {
                b.append(" STANDALONE");
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
            if (when1 - when2 > 0) {
                return 1;
            }
            if (when1 - when2 < 0) {
                return -1;
            }
            return 0;
        }
    }

    void calculateDeliveryPriorities(ArrayList<Alarm> alarms) {
        int alarmPrio;
        int N = alarms.size();
        for (int i = 0; i < N; i++) {
            Alarm a = alarms.get(i);
            if ("android.intent.action.TIME_TICK".equals(a.operation.getIntent().getAction())) {
                alarmPrio = 0;
            } else if (a.wakeup) {
                alarmPrio = 1;
            } else {
                alarmPrio = 2;
            }
            PriorityClass packagePrio = a.priorityClass;
            if (packagePrio == null) {
                packagePrio = this.mPriorities.get(a.operation.getCreatorPackage());
            }
            if (packagePrio == null) {
                packagePrio = new PriorityClass();
                a.priorityClass = packagePrio;
                this.mPriorities.put(a.operation.getCreatorPackage(), packagePrio);
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
        this.mLog = new LocalLog(TAG);
        this.mLock = new Object();
        this.mBroadcastRefCount = 0;
        this.mPendingNonWakeupAlarms = new ArrayList<>();
        this.mInFlight = new ArrayList<>();
        this.mHandler = new AlarmHandler();
        this.mResultReceiver = new ResultReceiver();
        this.mInteractive = true;
        this.mNextAlarmClockForUser = new SparseArray<>();
        this.mTmpSparseAlarmClockArray = new SparseArray<>();
        this.mPendingSendNextAlarmClockChangedForUser = new SparseBooleanArray();
        this.mHandlerSparseAlarmClockArray = new SparseArray<>();
        this.mPriorities = new HashMap<>();
        this.mCurrentSeq = 0;
        this.mRecentWakeups = new LinkedList<>();
        this.RECENT_WAKEUP_PERIOD = 86400000L;
        this.mAlarmDispatchComparator = new Comparator<Alarm>() {
            @Override
            public int compare(Alarm lhs, Alarm rhs) {
                if (lhs.priorityClass.priority < rhs.priorityClass.priority) {
                    return -1;
                }
                if (lhs.priorityClass.priority > rhs.priorityClass.priority) {
                    return 1;
                }
                if (lhs.whenElapsed >= rhs.whenElapsed) {
                    return lhs.whenElapsed > rhs.whenElapsed ? 1 : 0;
                }
                return -1;
            }
        };
        this.mAlarmBatches = new ArrayList<>();
        this.mBroadcastStats = new SparseArray<>();
        this.mNumDelayedAlarms = 0;
        this.mTotalDelayTime = 0L;
        this.mMaxDelayTime = 0L;
        this.mService = new IAlarmManager.Stub() {
            public void set(int type, long triggerAtTime, long windowLength, long interval, PendingIntent operation, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock) {
                if (workSource != null) {
                    AlarmManagerService.this.getContext().enforceCallingPermission("android.permission.UPDATE_DEVICE_STATS", "AlarmManager.set");
                }
                AlarmManagerService.this.setImpl(type, triggerAtTime, windowLength, interval, operation, windowLength == 0, workSource, alarmClock);
            }

            public boolean setTime(long millis) {
                AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "setTime");
                if (AlarmManagerService.this.mNativeData == 0) {
                    Slog.w(AlarmManagerService.TAG, "Not setting time since no alarm driver is available.");
                } else {
                    synchronized (AlarmManagerService.this.mLock) {
                        z = AlarmManagerService.this.setKernelTime(AlarmManagerService.this.mNativeData, millis) == 0;
                    }
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

            public void remove(PendingIntent operation) {
                AlarmManagerService.this.removeImpl(operation);
            }

            public AlarmManager.AlarmClockInfo getNextAlarmClock(int userId) {
                return AlarmManagerService.this.getNextAlarmClockImpl(ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "getNextAlarmClock", null));
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (AlarmManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                    pw.println("Permission Denial: can't dump AlarmManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                } else {
                    AlarmManagerService.this.dumpImpl(pw);
                }
            }
        };
        this.mNextPowerOn = 0L;
    }

    static long convertToElapsed(long when, int type) {
        boolean isRtc = true;
        if (type != 1 && type != 0 && type != 5) {
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
        return ((long) (0.75d * futurity)) + triggerAtTime;
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
            if (!b.standalone && b.canHold(whenElapsed, maxWhen)) {
                return i;
            }
        }
        return -1;
    }

    void rebatchAllAlarms() {
        synchronized (this.mLock) {
            rebatchAllAlarmsLocked(true);
        }
    }

    void rebatchAllAlarmsLocked(boolean doValidate) {
        long maxElapsed;
        ArrayList<Batch> oldSet = (ArrayList) this.mAlarmBatches.clone();
        this.mAlarmBatches.clear();
        long nowElapsed = SystemClock.elapsedRealtime();
        int oldBatches = oldSet.size();
        for (int batchNum = 0; batchNum < oldBatches; batchNum++) {
            Batch batch = oldSet.get(batchNum);
            int N = batch.size();
            for (int i = 0; i < N; i++) {
                Alarm a = batch.get(i);
                long whenElapsed = convertToElapsed(a.when, a.type);
                if (a.whenElapsed == a.maxWhen) {
                    maxElapsed = whenElapsed;
                } else {
                    maxElapsed = a.windowLength > 0 ? whenElapsed + a.windowLength : maxTriggerTime(nowElapsed, whenElapsed, a.repeatInterval);
                }
                setImplLocked(a.type, a.when, whenElapsed, a.windowLength, maxElapsed, a.repeatInterval, a.operation, batch.standalone, doValidate, a.workSource, a.alarmClock, a.userId);
            }
        }
    }

    static final class InFlight extends Intent {
        final int mAlarmType;
        final BroadcastStats mBroadcastStats;
        final FilterStats mFilterStats;
        final PendingIntent mPendingIntent;
        final String mTag;
        final WorkSource mWorkSource;

        InFlight(AlarmManagerService service, PendingIntent pendingIntent, WorkSource workSource, int alarmType, String tag) {
            this.mPendingIntent = pendingIntent;
            this.mWorkSource = workSource;
            this.mTag = tag;
            this.mBroadcastStats = service.getStatsLocked(pendingIntent);
            FilterStats fs = this.mBroadcastStats.filterStats.get(this.mTag);
            if (fs == null) {
                fs = new FilterStats(this.mBroadcastStats, this.mTag);
                this.mBroadcastStats.filterStats.put(this.mTag, fs);
            }
            this.mFilterStats = fs;
            this.mAlarmType = alarmType;
        }
    }

    static final class FilterStats {
        long aggregateTime;
        int count;
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
        setTimeZoneImpl(SystemProperties.get(TIMEZONE_PROPERTY));
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
        if (this.mNativeData != 0) {
            AlarmThread waitThread = new AlarmThread();
            waitThread.start();
        } else {
            Slog.w(TAG, "Failed to open alarm driver. Falling back to a handler.");
        }
        publishBinderService("alarm", this.mService);
    }

    protected void finalize() throws Throwable {
        try {
            close(this.mNativeData);
        } finally {
            super.finalize();
        }
    }

    void setTimeZoneImpl(String tz) {
        if (!TextUtils.isEmpty(tz)) {
            TimeZone zone = TimeZone.getTimeZone(tz);
            boolean timeZoneWasChanged = false;
            synchronized (this) {
                String current = SystemProperties.get(TIMEZONE_PROPERTY);
                if (current == null || !current.equals(zone.getID())) {
                    timeZoneWasChanged = true;
                    SystemProperties.set(TIMEZONE_PROPERTY, zone.getID());
                }
                int gmtOffset = zone.getOffset(System.currentTimeMillis());
                setKernelTimezone(this.mNativeData, -(gmtOffset / 60000));
            }
            TimeZone.setDefault(null);
            if (timeZoneWasChanged) {
                Intent intent = new Intent("android.intent.action.TIMEZONE_CHANGED");
                intent.addFlags(536870912);
                intent.addFlags(67108864);
                intent.putExtra("time-zone", zone.getID());
                getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        }
    }

    void removeImpl(PendingIntent operation) {
        if (operation != null) {
            synchronized (this.mLock) {
                removeLocked(operation);
            }
        }
    }

    void setImpl(int type, long triggerAtTime, long windowLength, long interval, PendingIntent operation, boolean isStandalone, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock) {
        long maxElapsed;
        if (operation == null) {
            Slog.w(TAG, "set/setRepeating ignored because there is no intent");
            return;
        }
        if (windowLength > 43200000) {
            Slog.w(TAG, "Window length " + windowLength + "ms suspiciously long; limiting to 1 hour");
            windowLength = 3600000;
        }
        if (interval > 0 && interval < MIN_INTERVAL) {
            Slog.w(TAG, "Suspiciously short interval " + interval + " millis; expanding to 60 seconds");
            interval = MIN_INTERVAL;
        }
        if (type < 0 || type > 5) {
            throw new IllegalArgumentException("Invalid alarm type " + type);
        }
        if (triggerAtTime < 0) {
            long who = Binder.getCallingUid();
            long what = Binder.getCallingPid();
            Slog.w(TAG, "Invalid alarm trigger time! " + triggerAtTime + " from uid=" + who + " pid=" + what);
            triggerAtTime = 0;
        }
        long nowElapsed = SystemClock.elapsedRealtime();
        long nominalTrigger = convertToElapsed(triggerAtTime, type);
        long minTrigger = nowElapsed + MIN_FUTURITY;
        long triggerElapsed = nominalTrigger > minTrigger ? nominalTrigger : minTrigger;
        if (windowLength == 0) {
            maxElapsed = triggerElapsed;
        } else if (windowLength < 0) {
            maxElapsed = maxTriggerTime(nowElapsed, triggerElapsed, interval);
        } else {
            maxElapsed = triggerElapsed + windowLength;
        }
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            setImplLocked(type, triggerAtTime, triggerElapsed, windowLength, maxElapsed, interval, operation, isStandalone, true, workSource, alarmClock, userId);
        }
    }

    private void setImplLocked(int type, long when, long whenElapsed, long windowLength, long maxWhen, long interval, PendingIntent operation, boolean isStandalone, boolean doValidate, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock, int userId) {
        Alarm a = new Alarm(type, when, whenElapsed, windowLength, maxWhen, interval, operation, workSource, alarmClock, userId);
        removeLocked(operation);
        int whichBatch = isStandalone ? -1 : attemptCoalesceLocked(whenElapsed, maxWhen);
        if (whichBatch < 0) {
            Batch batch = new Batch(a);
            batch.standalone = isStandalone;
            addBatchLocked(this.mAlarmBatches, batch);
        } else {
            Batch batch2 = this.mAlarmBatches.get(whichBatch);
            if (batch2.add(a)) {
                this.mAlarmBatches.remove(whichBatch);
                addBatchLocked(this.mAlarmBatches, batch2);
            }
        }
        if (alarmClock != null) {
            this.mNextAlarmClockMayChange = true;
            updateNextAlarmClockLocked();
        }
        rescheduleKernelAlarmsLocked();
    }

    void dumpImpl(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Current Alarm Manager state:");
            long nowRTC = System.currentTimeMillis();
            long nowELAPSED = SystemClock.elapsedRealtime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            pw.print("nowRTC=");
            pw.print(nowRTC);
            pw.print("=");
            pw.print(sdf.format(new Date(nowRTC)));
            pw.print(" nowELAPSED=");
            TimeUtils.formatDuration(nowELAPSED, pw);
            pw.println();
            if (!this.mInteractive) {
                pw.print("Time since non-interactive: ");
                TimeUtils.formatDuration(nowELAPSED - this.mNonInteractiveStartTime, pw);
                pw.println();
                pw.print("Max wakeup delay: ");
                TimeUtils.formatDuration(currentNonWakeupFuzzLocked(nowELAPSED), pw);
                pw.println();
                pw.print("Time since last dispatch: ");
                TimeUtils.formatDuration(nowELAPSED - this.mLastAlarmDeliveryTime, pw);
                pw.println();
                pw.print("Next non-wakeup delivery time: ");
                TimeUtils.formatDuration(nowELAPSED - this.mNextNonWakeupDeliveryTime, pw);
                pw.println();
            }
            long nextWakeupRTC = this.mNextWakeup + (nowRTC - nowELAPSED);
            long nextNonWakeupRTC = this.mNextNonWakeup + (nowRTC - nowELAPSED);
            pw.print("Next non-wakeup alarm: ");
            TimeUtils.formatDuration(this.mNextNonWakeup, nowELAPSED, pw);
            pw.print(" = ");
            pw.println(sdf.format(new Date(nextNonWakeupRTC)));
            pw.print("Next wakeup: ");
            TimeUtils.formatDuration(this.mNextWakeup, nowELAPSED, pw);
            pw.print(" = ");
            pw.println(sdf.format(new Date(nextWakeupRTC)));
            pw.print("Num time change events: ");
            pw.println(this.mNumTimeChanged);
            if (this.mAlarmBatches.size() > 0) {
                pw.println();
                pw.print("Pending alarm batches: ");
                pw.println(this.mAlarmBatches.size());
                for (Batch b : this.mAlarmBatches) {
                    pw.print(b);
                    pw.println(':');
                    dumpAlarmList(pw, b.alarms, "  ", nowELAPSED, nowRTC, sdf);
                }
            }
            pw.println();
            pw.print("Past-due non-wakeup alarms: ");
            if (this.mPendingNonWakeupAlarms.size() > 0) {
                pw.println(this.mPendingNonWakeupAlarms.size());
                dumpAlarmList(pw, this.mPendingNonWakeupAlarms, "  ", nowELAPSED, nowRTC, sdf);
            } else {
                pw.println("(none)");
            }
            pw.print("  Number of delayed alarms: ");
            pw.print(this.mNumDelayedAlarms);
            pw.print(", total delay time: ");
            TimeUtils.formatDuration(this.mTotalDelayTime, pw);
            pw.println();
            pw.print("  Max delay time: ");
            TimeUtils.formatDuration(this.mMaxDelayTime, pw);
            pw.print(", max non-interactive time: ");
            TimeUtils.formatDuration(this.mNonInteractiveTime, pw);
            pw.println();
            pw.println();
            pw.print("  Broadcast ref count: ");
            pw.println(this.mBroadcastRefCount);
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
                for (int i = 0; i < len; i++) {
                    FilterStats fs2 = topFilters[i];
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
                    for (int i2 = 0; i2 < tmpFilters.size(); i2++) {
                        FilterStats fs3 = tmpFilters.get(i2);
                        pw.print("    ");
                        if (fs3.nesting > 0) {
                            pw.print("*ACTIVE* ");
                        }
                        TimeUtils.formatDuration(fs3.aggregateTime, pw);
                        pw.print(" ");
                        pw.print(fs3.numWakeup);
                        pw.print(" wakes ");
                        pw.print(fs3.count);
                        pw.print(" alarms: ");
                        pw.print(fs3.mTag);
                        pw.println();
                    }
                }
            }
        }
    }

    private void logBatchesLocked(SimpleDateFormat sdf) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(PackageManagerService.DumpState.DUMP_KEYSETS);
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

    private AlarmManager.AlarmClockInfo getNextAlarmClockImpl(int userId) {
        AlarmManager.AlarmClockInfo alarmClockInfo;
        synchronized (this.mLock) {
            alarmClockInfo = this.mNextAlarmClockForUser.get(userId);
        }
        return alarmClockInfo;
    }

    private void updateNextAlarmClockLocked() {
        if (this.mNextAlarmClockMayChange) {
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
                        int userId = a.userId;
                        if (nextForUser.get(userId) == null) {
                            nextForUser.put(userId, a.alarmClock);
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
    }

    private void updateNextAlarmInfoForUserLocked(int userId, AlarmManager.AlarmClockInfo alarmClock) {
        if (alarmClock != null) {
            this.mNextAlarmClockForUser.put(userId, alarmClock);
        } else {
            this.mNextAlarmClockForUser.remove(userId);
        }
        this.mPendingSendNextAlarmClockChangedForUser.put(userId, true);
        this.mHandler.removeMessages(4);
        this.mHandler.sendEmptyMessage(4);
    }

    private void sendNextAlarmClockChanged() {
        SparseArray<AlarmManager.AlarmClockInfo> pendingUsers = this.mHandlerSparseAlarmClockArray;
        pendingUsers.clear();
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
    }

    private static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info, int userId) {
        String skeleton = DateFormat.is24HourFormat(context, userId) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return info == null ? "" : DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private Batch findFirstPowerOnBatchLocked() {
        int N = this.mAlarmBatches.size();
        for (int i = 0; i < N; i++) {
            Batch b = this.mAlarmBatches.get(i);
            if (b.hasPowerOn()) {
                return b;
            }
        }
        return null;
    }

    void rescheduleKernelAlarmsLocked() {
        long nextNonWakeup = 0;
        if (this.mAlarmBatches.size() > 0) {
            Batch firstWakeup = findFirstWakeupBatchLocked();
            Batch firstPowerOn = findFirstPowerOnBatchLocked();
            Batch firstBatch = this.mAlarmBatches.get(0);
            if (firstPowerOn != null && this.mNextPowerOn != firstPowerOn.start) {
                this.mNextPowerOn = firstPowerOn.start;
                setLocked(5, firstPowerOn.startInMillis);
            }
            if (firstWakeup != null) {
                this.mNextWakeup = firstWakeup.start;
                setLocked(2, firstWakeup.start);
            }
            if (firstBatch != firstWakeup) {
                nextNonWakeup = firstBatch.start;
            }
        }
        if (this.mPendingNonWakeupAlarms.size() > 0 && (nextNonWakeup == 0 || this.mNextNonWakeupDeliveryTime < nextNonWakeup)) {
            nextNonWakeup = this.mNextNonWakeupDeliveryTime;
        }
        if (nextNonWakeup != 0) {
            this.mNextNonWakeup = nextNonWakeup;
            setLocked(3, nextNonWakeup);
        }
    }

    private void removeLocked(PendingIntent operation) {
        boolean didRemove = false;
        boolean clearPowerOnRtc = false;
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = this.mAlarmBatches.get(i);
            didRemove |= b.remove(operation);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            } else {
                clearPowerOnRtc |= b.hasPowerOn();
            }
        }
        if (!clearPowerOnRtc) {
            clear(5);
            this.mNextPowerOn = 0L;
        }
        if (didRemove) {
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
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
        if (didRemove) {
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
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
        if (didRemove) {
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    void interactiveStateChangedLocked(boolean interactive) {
        if (this.mInteractive != interactive) {
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
                if (this.mNonInteractiveStartTime > 0) {
                    long dur = nowELAPSED - this.mNonInteractiveStartTime;
                    if (dur > this.mNonInteractiveTime) {
                        this.mNonInteractiveTime = dur;
                        return;
                    }
                    return;
                }
                return;
            }
            this.mNonInteractiveStartTime = nowELAPSED;
        }
    }

    boolean lookForPackageLocked(String packageName) {
        for (int i = 0; i < this.mAlarmBatches.size(); i++) {
            Batch b = this.mAlarmBatches.get(i);
            if (b.hasPackage(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void setLocked(int type, long when) {
        long alarmSeconds;
        long alarmNanoseconds;
        if (this.mNativeData != 0) {
            if (when < 0) {
                alarmSeconds = 0;
                alarmNanoseconds = 0;
            } else {
                alarmSeconds = when / 1000;
                alarmNanoseconds = (when % 1000) * 1000 * 1000;
            }
            set(this.mNativeData, type, alarmSeconds, alarmNanoseconds);
            return;
        }
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
            case 4:
            default:
                return "--unknown--";
            case 5:
                return "RTC_POWERON";
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
                alarm.count = 1;
                triggerList.add(alarm);
                if (alarm.repeatInterval > 0) {
                    alarm.count = (int) (((long) alarm.count) + ((nowELAPSED - alarm.whenElapsed) / alarm.repeatInterval));
                    long delta = ((long) alarm.count) * alarm.repeatInterval;
                    long nextElapsed = alarm.whenElapsed + delta;
                    setImplLocked(alarm.type, alarm.when + delta, nextElapsed, alarm.windowLength, maxTriggerTime(nowELAPSED, nextElapsed, alarm.repeatInterval), alarm.repeatInterval, alarm.operation, batch.standalone, true, alarm.workSource, alarm.alarmClock, alarm.userId);
                }
                if (alarm.wakeup) {
                    hasWakeup = true;
                }
                if (alarm.alarmClock != null) {
                    this.mNextAlarmClockMayChange = true;
                }
            }
        }
        this.mCurrentSeq++;
        calculateDeliveryPriorities(triggerList);
        Collections.sort(triggerList, this.mAlarmDispatchComparator);
        return hasWakeup;
    }

    public static class IncreasingTimeOrder implements Comparator<Alarm> {
        @Override
        public int compare(Alarm a1, Alarm a2) {
            long when1 = a1.when;
            long when2 = a2.when;
            if (when1 - when2 > 0) {
                return 1;
            }
            if (when1 - when2 < 0) {
                return -1;
            }
            return 0;
        }
    }

    private static class Alarm {
        public final AlarmManager.AlarmClockInfo alarmClock;
        public int count;
        public long maxWhen;
        public final PendingIntent operation;
        public PriorityClass priorityClass;
        public long repeatInterval;
        public final String tag;
        public final int type;
        public final int userId;
        public final boolean wakeup;
        public long when;
        public long whenElapsed;
        public long windowLength;
        public final WorkSource workSource;

        public Alarm(int _type, long _when, long _whenElapsed, long _windowLength, long _maxWhen, long _interval, PendingIntent _op, WorkSource _ws, AlarmManager.AlarmClockInfo _info, int _userId) {
            this.type = _type;
            this.wakeup = _type == 2 || _type == 0;
            this.when = _when;
            this.whenElapsed = _whenElapsed;
            this.windowLength = _windowLength;
            this.maxWhen = _maxWhen;
            this.repeatInterval = _interval;
            this.operation = _op;
            this.tag = makeTag(_op, _type);
            this.workSource = _ws;
            this.alarmClock = _info;
            this.userId = _userId;
        }

        public static String makeTag(PendingIntent pi, int type) {
            return pi.getTag((type == 2 || type == 0) ? "*walarm*:" : "*alarm*:");
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
            sb.append(this.operation.getTargetPackage());
            sb.append('}');
            return sb.toString();
        }

        public void dump(PrintWriter pw, String prefix, long nowRTC, long nowELAPSED, SimpleDateFormat sdf) {
            boolean isRtc = true;
            if (this.type != 1 && this.type != 0) {
                isRtc = false;
            }
            pw.print(prefix);
            pw.print("tag=");
            pw.println(this.tag);
            pw.print(prefix);
            pw.print("type=");
            pw.print(this.type);
            pw.print(" whenElapsed=");
            TimeUtils.formatDuration(this.whenElapsed, nowELAPSED, pw);
            if (isRtc) {
                pw.print(" when=");
                pw.print(sdf.format(new Date(this.when)));
            } else {
                pw.print(" when=");
                TimeUtils.formatDuration(this.when, nowELAPSED, pw);
            }
            pw.println();
            pw.print(prefix);
            pw.print("window=");
            pw.print(this.windowLength);
            pw.print(" repeatInterval=");
            pw.print(this.repeatInterval);
            pw.print(" count=");
            pw.println(this.count);
            pw.print(prefix);
            pw.print("operation=");
            pw.println(this.operation);
        }
    }

    void recordWakeupAlarms(ArrayList<Batch> batches, long nowELAPSED, long nowRTC) {
        int numBatches = batches.size();
        for (int nextBatch = 0; nextBatch < numBatches; nextBatch++) {
            Batch b = batches.get(nextBatch);
            if (b.start <= nowELAPSED) {
                int numAlarms = b.alarms.size();
                for (int nextAlarm = 0; nextAlarm < numAlarms; nextAlarm++) {
                    Alarm a = b.alarms.get(nextAlarm);
                    WakeupEvent e = new WakeupEvent(nowRTC, a.operation.getCreatorUid(), a.operation.getIntent().getAction());
                    this.mRecentWakeups.add(e);
                }
            } else {
                return;
            }
        }
    }

    long currentNonWakeupFuzzLocked(long nowELAPSED) {
        long timeSinceOn = nowELAPSED - this.mNonInteractiveStartTime;
        if (timeSinceOn < 300000) {
            return 120000L;
        }
        if (timeSinceOn < 1800000) {
            return 900000L;
        }
        return 3600000L;
    }

    boolean checkAllowNonWakeupDelayLocked(long nowELAPSED) {
        if (this.mInteractive || this.mLastAlarmDeliveryTime <= 0) {
            return false;
        }
        if (this.mPendingNonWakeupAlarms.size() > 0 && this.mNextNonWakeupDeliveryTime > nowELAPSED) {
            return false;
        }
        long timeSinceLast = nowELAPSED - this.mLastAlarmDeliveryTime;
        return timeSinceLast <= currentNonWakeupFuzzLocked(nowELAPSED);
    }

    void deliverAlarmsLocked(ArrayList<Alarm> triggerList, long nowELAPSED) {
        this.mLastAlarmDeliveryTime = nowELAPSED;
        for (int i = 0; i < triggerList.size(); i++) {
            Alarm alarm = triggerList.get(i);
            try {
                alarm.operation.send(getContext(), 0, mBackgroundIntent.putExtra("android.intent.extra.ALARM_COUNT", alarm.count), this.mResultReceiver, this.mHandler);
                if (this.mBroadcastRefCount == 0) {
                    setWakelockWorkSource(alarm.operation, alarm.workSource, alarm.type, alarm.tag, true);
                    this.mWakeLock.acquire();
                }
                InFlight inflight = new InFlight(this, alarm.operation, alarm.workSource, alarm.type, alarm.tag);
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
                if (alarm.type == 2 || alarm.type == 0 || alarm.type == 5) {
                    bs.numWakeup++;
                    fs.numWakeup++;
                    if (alarm.workSource != null && alarm.workSource.size() > 0) {
                        for (int wi = 0; wi < alarm.workSource.size(); wi++) {
                            ActivityManagerNative.noteWakeupAlarm(alarm.operation, alarm.workSource.get(wi), alarm.workSource.getName(wi));
                        }
                    } else {
                        ActivityManagerNative.noteWakeupAlarm(alarm.operation, -1, (String) null);
                    }
                }
            } catch (PendingIntent.CanceledException e) {
                if (alarm.repeatInterval > 0) {
                    removeImpl(alarm.operation);
                }
            } catch (RuntimeException e2) {
                Slog.w(TAG, "Failure sending alarm.", e2);
            }
        }
    }

    private class AlarmThread extends Thread {
        public AlarmThread() {
            super(AlarmManagerService.TAG);
        }

        @Override
        public void run() {
            ArrayList<Alarm> triggerList = new ArrayList<>();
            while (true) {
                int result = AlarmManagerService.this.waitForAlarm(AlarmManagerService.this.mNativeData);
                triggerList.clear();
                if ((AlarmManagerService.TIME_CHANGED_MASK & result) != 0) {
                    AlarmManagerService.this.removeImpl(AlarmManagerService.this.mTimeTickSender);
                    AlarmManagerService.this.rebatchAllAlarms();
                    AlarmManagerService.this.mClockReceiver.scheduleTimeTickEvent();
                    synchronized (AlarmManagerService.this.mLock) {
                        AlarmManagerService.this.mNumTimeChanged++;
                    }
                    Intent intent = new Intent("android.intent.action.TIME_SET");
                    intent.addFlags(603979776);
                    AlarmManagerService.this.getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
                }
                synchronized (AlarmManagerService.this.mLock) {
                    long nowRTC = System.currentTimeMillis();
                    long nowELAPSED = SystemClock.elapsedRealtime();
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
            }
        }
    }

    void setWakelockWorkSource(PendingIntent pi, WorkSource ws, int type, String tag, boolean first) {
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
        int uid = ActivityManagerNative.getDefault().getUidForIntentSender(pi.getTarget());
        if (uid >= 0) {
            this.mWakeLock.setWorkSource(new WorkSource(uid));
            return;
        }
        this.mWakeLock.setWorkSource(null);
    }

    private class AlarmHandler extends Handler {
        public static final int ALARM_EVENT = 1;
        public static final int DATE_CHANGE_EVENT = 3;
        public static final int MINUTE_CHANGE_EVENT = 2;
        public static final int SEND_NEXT_ALARM_CLOCK_CHANGED = 4;

        public AlarmHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
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
            }
            if (msg.what == 4) {
                AlarmManagerService.this.sendNextAlarmClockChanged();
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
                scheduleTimeTickEvent();
            } else if (intent.getAction().equals("android.intent.action.DATE_CHANGED")) {
                TimeZone zone = TimeZone.getTimeZone(SystemProperties.get(AlarmManagerService.TIMEZONE_PROPERTY));
                int gmtOffset = zone.getOffset(System.currentTimeMillis());
                AlarmManagerService.this.setKernelTimezone(AlarmManagerService.this.mNativeData, -(gmtOffset / 60000));
                scheduleDateChangedEvent();
            }
        }

        public void scheduleTimeTickEvent() {
            long currentTime = System.currentTimeMillis();
            long nextTime = AlarmManagerService.MIN_INTERVAL * ((currentTime / AlarmManagerService.MIN_INTERVAL) + 1);
            long tickEventDelay = nextTime - currentTime;
            AlarmManagerService.this.setImpl(3, SystemClock.elapsedRealtime() + tickEventDelay, 0L, 0L, AlarmManagerService.this.mTimeTickSender, true, null, null);
        }

        public void scheduleDateChangedEvent() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(10, 0);
            calendar.set(12, 0);
            calendar.set(13, 0);
            calendar.set(14, 0);
            calendar.add(5, 1);
            AlarmManagerService.this.setImpl(1, calendar.getTimeInMillis(), 0L, 0L, AlarmManagerService.this.mDateChangeSender, true, null, null);
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
            AlarmManagerService.this.getContext().registerReceiver(this, sdFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String pkg;
            synchronized (AlarmManagerService.this.mLock) {
                String action = intent.getAction();
                String[] pkgList = null;
                if ("android.intent.action.QUERY_PACKAGE_RESTART".equals(action)) {
                    for (String packageName : intent.getStringArrayExtra("android.intent.extra.PACKAGES")) {
                        if (AlarmManagerService.this.lookForPackageLocked(packageName)) {
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
                } else if (!"android.intent.action.PACKAGE_REMOVED".equals(action) || !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    Uri data = intent.getData();
                    if (data != null && (pkg = data.getSchemeSpecificPart()) != null) {
                        pkgList = new String[]{pkg};
                    }
                } else {
                    return;
                }
                if (pkgList != null && pkgList.length > 0) {
                    String[] arr$ = pkgList;
                    for (String pkg2 : arr$) {
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

    private final BroadcastStats getStatsLocked(PendingIntent pi) {
        String pkg = pi.getCreatorPackage();
        int uid = pi.getCreatorUid();
        ArrayMap<String, BroadcastStats> uidStats = this.mBroadcastStats.get(uid);
        if (uidStats == null) {
            uidStats = new ArrayMap<>();
            this.mBroadcastStats.put(uid, uidStats);
        }
        BroadcastStats bs = uidStats.get(pkg);
        if (bs == null) {
            BroadcastStats bs2 = new BroadcastStats(uid, pkg);
            uidStats.put(pkg, bs2);
            return bs2;
        }
        return bs;
    }

    class ResultReceiver implements PendingIntent.OnFinished {
        ResultReceiver() {
        }

        @Override
        public void onSendFinished(PendingIntent pi, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            synchronized (AlarmManagerService.this.mLock) {
                InFlight inflight = null;
                int i = 0;
                while (true) {
                    if (i >= AlarmManagerService.this.mInFlight.size()) {
                        break;
                    }
                    if (AlarmManagerService.this.mInFlight.get(i).mPendingIntent != pi) {
                        i++;
                    } else {
                        inflight = AlarmManagerService.this.mInFlight.remove(i);
                        break;
                    }
                }
                if (inflight != null) {
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
                } else {
                    AlarmManagerService.this.mLog.w("No in-flight alarm for " + pi + " " + intent);
                }
                AlarmManagerService alarmManagerService = AlarmManagerService.this;
                alarmManagerService.mBroadcastRefCount--;
                if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                    AlarmManagerService.this.mWakeLock.release();
                    if (AlarmManagerService.this.mInFlight.size() > 0) {
                        AlarmManagerService.this.mLog.w("Finished all broadcasts with " + AlarmManagerService.this.mInFlight.size() + " remaining inflights");
                        for (int i2 = 0; i2 < AlarmManagerService.this.mInFlight.size(); i2++) {
                            AlarmManagerService.this.mLog.w("  Remaining #" + i2 + ": " + AlarmManagerService.this.mInFlight.get(i2));
                        }
                        AlarmManagerService.this.mInFlight.clear();
                    }
                } else if (AlarmManagerService.this.mInFlight.size() > 0) {
                    InFlight inFlight = AlarmManagerService.this.mInFlight.get(0);
                    AlarmManagerService.this.setWakelockWorkSource(inFlight.mPendingIntent, inFlight.mWorkSource, inFlight.mAlarmType, inFlight.mTag, false);
                } else {
                    AlarmManagerService.this.mLog.w("Alarm wakelock still held but sent queue empty");
                    AlarmManagerService.this.mWakeLock.setWorkSource(null);
                }
            }
        }
    }
}
