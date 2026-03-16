package com.android.internal.os;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.pm.PackageManager;
import android.mtp.MtpConstants;
import android.net.ConnectivityManager;
import android.net.NetworkStats;
import android.net.wifi.WifiManager;
import android.os.BadParcelableException;
import android.os.BatteryStats;
import android.os.Build;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.powerhint.PowerHintManager;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LogWriter;
import android.util.MutableInt;
import android.util.Printer;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.view.View;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.JournaledFile;
import com.android.server.NetworkManagementSocketTagger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public final class BatteryStatsImpl extends BatteryStats {
    private static final int BATTERY_PLUGGED_NONE = 0;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_HISTORY = false;
    static final long DELAY_UPDATE_WAKELOCKS = 5000;
    static final int DELTA_BATTERY_LEVEL_FLAG = 524288;
    static final int DELTA_EVENT_FLAG = 8388608;
    static final int DELTA_STATE2_FLAG = 2097152;
    static final int DELTA_STATE_FLAG = 1048576;
    static final int DELTA_STATE_MASK = -16777216;
    static final int DELTA_TIME_ABS = 524285;
    static final int DELTA_TIME_INT = 524286;
    static final int DELTA_TIME_LONG = 524287;
    static final int DELTA_TIME_MASK = 524287;
    static final int DELTA_WAKELOCK_FLAG = 4194304;
    private static final int MAGIC = -1166707595;
    static final int MAX_HISTORY_BUFFER = 262144;
    private static final int MAX_HISTORY_ITEMS = 2000;
    static final int MAX_LEVEL_STEPS = 200;
    static final int MAX_MAX_HISTORY_BUFFER = 327680;
    private static final int MAX_MAX_HISTORY_ITEMS = 3000;
    private static final int MAX_WAKELOCKS_PER_UID = 100;
    static final int MSG_REPORT_POWER_CHANGE = 2;
    static final int MSG_UPDATE_WAKELOCKS = 1;
    static final int NET_UPDATE_ALL = 65535;
    static final int NET_UPDATE_MOBILE = 1;
    static final int NET_UPDATE_WIFI = 2;
    static final int STATE_BATTERY_HEALTH_MASK = 7;
    static final int STATE_BATTERY_HEALTH_SHIFT = 26;
    static final int STATE_BATTERY_PLUG_MASK = 3;
    static final int STATE_BATTERY_PLUG_SHIFT = 24;
    static final int STATE_BATTERY_STATUS_MASK = 7;
    static final int STATE_BATTERY_STATUS_SHIFT = 29;
    private static final String TAG = "BatteryStatsImpl";
    private static final boolean USE_OLD_HISTORY = false;
    private static final int VERSION = 116;
    private static int sNumSpeedSteps;
    final BatteryStats.HistoryEventTracker mActiveEvents;
    int mAudioOnNesting;
    StopwatchTimer mAudioOnTimer;
    final ArrayList<StopwatchTimer> mAudioTurnedOnTimers;
    boolean mBluetoothOn;
    StopwatchTimer mBluetoothOnTimer;
    private int mBluetoothPingCount;
    private int mBluetoothPingStart;
    int mBluetoothState;
    final StopwatchTimer[] mBluetoothStateTimer;
    BluetoothHeadset mBtHeadset;
    private BatteryCallback mCallback;
    int mChangedStates;
    int mChangedStates2;
    final long[] mChargeStepDurations;
    public final AtomicFile mCheckinFile;
    private NetworkStats mCurMobileSnapshot;
    int mCurStepMode;
    private NetworkStats mCurWifiSnapshot;
    int mCurrentBatteryLevel;
    int mDischargeAmountScreenOff;
    int mDischargeAmountScreenOffSinceCharge;
    int mDischargeAmountScreenOn;
    int mDischargeAmountScreenOnSinceCharge;
    int mDischargeCurrentLevel;
    int mDischargePlugLevel;
    int mDischargeScreenOffUnplugLevel;
    int mDischargeScreenOnUnplugLevel;
    int mDischargeStartLevel;
    final long[] mDischargeStepDurations;
    int mDischargeUnplugLevel;
    boolean mDistributeWakelockCpu;
    String mEndPlatformVersion;
    private final JournaledFile mFile;
    boolean mFlashlightOn;
    StopwatchTimer mFlashlightOnTimer;
    final ArrayList<StopwatchTimer> mFullTimers;
    final ArrayList<StopwatchTimer> mFullWifiLockTimers;
    boolean mGlobalWifiRunning;
    StopwatchTimer mGlobalWifiRunningTimer;
    int mGpsNesting;
    public final MyHandler mHandler;
    boolean mHaveBatteryLevel;
    int mHighDischargeAmountSinceCharge;
    BatteryStats.HistoryItem mHistory;
    final BatteryStats.HistoryItem mHistoryAddTmp;
    long mHistoryBaseTime;
    final Parcel mHistoryBuffer;
    int mHistoryBufferLastPos;
    BatteryStats.HistoryItem mHistoryCache;
    final BatteryStats.HistoryItem mHistoryCur;
    BatteryStats.HistoryItem mHistoryEnd;
    private BatteryStats.HistoryItem mHistoryIterator;
    BatteryStats.HistoryItem mHistoryLastEnd;
    final BatteryStats.HistoryItem mHistoryLastLastWritten;
    final BatteryStats.HistoryItem mHistoryLastWritten;
    boolean mHistoryOverflow;
    final BatteryStats.HistoryItem mHistoryReadTmp;
    final HashMap<BatteryStats.HistoryTag, Integer> mHistoryTagPool;
    int mInitStepMode;
    private String mInitialAcquireWakeName;
    private int mInitialAcquireWakeUid;
    boolean mInteractive;
    StopwatchTimer mInteractiveTimer;
    final SparseIntArray mIsolatedUids;
    private boolean mIteratingHistory;
    private final HashMap<String, SamplingTimer> mKernelWakelockStats;
    int mLastChargeStepLevel;
    long mLastChargeStepTime;
    int mLastDischargeStepLevel;
    long mLastDischargeStepTime;
    long mLastHistoryElapsedRealtime;
    private NetworkStats mLastMobileSnapshot;
    final ArrayList<StopwatchTimer> mLastPartialTimers;
    long mLastRecordedClockRealtime;
    long mLastRecordedClockTime;
    String mLastWakeupReason;
    long mLastWakeupUptimeMs;
    private NetworkStats mLastWifiSnapshot;
    long mLastWriteTime;
    private int mLoadedNumConnectivityChange;
    int mLowDischargeAmountSinceCharge;
    boolean mLowPowerModeEnabled;
    StopwatchTimer mLowPowerModeEnabledTimer;
    int mMaxChargeStepLevel;
    int mMinDischargeStepLevel;

    @GuardedBy("this")
    private String[] mMobileIfaces;
    LongSamplingCounter mMobileRadioActiveAdjustedTime;
    StopwatchTimer mMobileRadioActivePerAppTimer;
    long mMobileRadioActiveStartTime;
    StopwatchTimer mMobileRadioActiveTimer;
    LongSamplingCounter mMobileRadioActiveUnknownCount;
    LongSamplingCounter mMobileRadioActiveUnknownTime;
    int mMobileRadioPowerState;
    int mModStepMode;
    final LongSamplingCounter[] mNetworkByteActivityCounters;
    final LongSamplingCounter[] mNetworkPacketActivityCounters;
    private final NetworkStatsFactory mNetworkStatsFactory;
    int mNextHistoryTagIdx;
    boolean mNoAutoReset;
    int mNumChargeStepDurations;
    private int mNumConnectivityChange;
    int mNumDischargeStepDurations;
    int mNumHistoryItems;
    int mNumHistoryTagChars;
    boolean mOnBattery;
    boolean mOnBatteryInternal;
    final TimeBase mOnBatteryScreenOffTimeBase;
    final TimeBase mOnBatteryTimeBase;
    final ArrayList<StopwatchTimer> mPartialTimers;
    Parcel mPendingWrite;
    int mPhoneDataConnectionType;
    final StopwatchTimer[] mPhoneDataConnectionsTimer;
    boolean mPhoneOn;
    StopwatchTimer mPhoneOnTimer;
    private int mPhoneServiceState;
    private int mPhoneServiceStateRaw;
    StopwatchTimer mPhoneSignalScanningTimer;
    int mPhoneSignalStrengthBin;
    int mPhoneSignalStrengthBinRaw;
    final StopwatchTimer[] mPhoneSignalStrengthsTimer;
    private int mPhoneSimStateRaw;
    private final Map<String, KernelWakelockStats> mProcWakelockFileStats;
    private final long[] mProcWakelocksData;
    private final String[] mProcWakelocksName;
    int mReadHistoryChars;
    String[] mReadHistoryStrings;
    int[] mReadHistoryUids;
    private boolean mReadOverflow;
    long mRealtime;
    long mRealtimeStart;
    boolean mRecordAllHistory;
    boolean mRecordingHistory;
    int mScreenBrightnessBin;
    final StopwatchTimer[] mScreenBrightnessTimer;
    StopwatchTimer mScreenOnTimer;
    int mScreenState;
    int mSensorNesting;
    final SparseArray<ArrayList<StopwatchTimer>> mSensorTimers;
    boolean mShuttingDown;
    long mStartClockTime;
    int mStartCount;
    String mStartPlatformVersion;
    private NetworkStats mTmpNetworkStats;
    private final NetworkStats.Entry mTmpNetworkStatsEntry;
    long mTrackRunningHistoryElapsedRealtime;
    long mTrackRunningHistoryUptime;
    final SparseArray<Uid> mUidStats;
    private int mUnpluggedNumConnectivityChange;
    long mUptime;
    long mUptimeStart;
    int mVideoOnNesting;
    StopwatchTimer mVideoOnTimer;
    final ArrayList<StopwatchTimer> mVideoTurnedOnTimers;
    boolean mWakeLockImportant;
    int mWakeLockNesting;
    private final HashMap<String, SamplingTimer> mWakeupReasonStats;
    final SparseArray<ArrayList<StopwatchTimer>> mWifiBatchedScanTimers;
    int mWifiFullLockNesting;

    @GuardedBy("this")
    private String[] mWifiIfaces;
    int mWifiMulticastNesting;
    final ArrayList<StopwatchTimer> mWifiMulticastTimers;
    boolean mWifiOn;
    StopwatchTimer mWifiOnTimer;
    final ArrayList<StopwatchTimer> mWifiRunningTimers;
    int mWifiScanNesting;
    final ArrayList<StopwatchTimer> mWifiScanTimers;
    int mWifiSignalStrengthBin;
    final StopwatchTimer[] mWifiSignalStrengthsTimer;
    int mWifiState;
    final StopwatchTimer[] mWifiStateTimer;
    int mWifiSupplState;
    final StopwatchTimer[] mWifiSupplStateTimer;
    final ArrayList<StopwatchTimer> mWindowTimers;
    final ReentrantLock mWriteLock;
    private static int sKernelWakelockUpdateVersion = 0;
    private static final int[] PROC_WAKELOCKS_FORMAT = {5129, MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE, 9, 9, 9, MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE};
    private static final int[] WAKEUP_SOURCES_FORMAT = {4105, 8457, R.styleable.Theme_textColorSearchUrl, R.styleable.Theme_textColorSearchUrl, R.styleable.Theme_textColorSearchUrl, R.styleable.Theme_textColorSearchUrl, 8457};
    public static final Parcelable.Creator<BatteryStatsImpl> CREATOR = new Parcelable.Creator<BatteryStatsImpl>() {
        @Override
        public BatteryStatsImpl createFromParcel(Parcel in) {
            return new BatteryStatsImpl(in);
        }

        @Override
        public BatteryStatsImpl[] newArray(int size) {
            return new BatteryStatsImpl[size];
        }
    };

    public interface BatteryCallback {
        void batteryNeedsCpuUpdate();

        void batteryPowerChanged(boolean z);
    }

    public interface TimeBaseObs {
        void onTimeStarted(long j, long j2, long j3);

        void onTimeStopped(long j, long j2, long j3);
    }

    final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            BatteryCallback cb = BatteryStatsImpl.this.mCallback;
            switch (msg.what) {
                case 1:
                    if (cb != null) {
                        cb.batteryNeedsCpuUpdate();
                    }
                    break;
                case 2:
                    if (cb != null) {
                        cb.batteryPowerChanged(msg.arg1 != 0);
                    }
                    break;
            }
        }
    }

    @Override
    public Map<String, ? extends Timer> getKernelWakelockStats() {
        return this.mKernelWakelockStats;
    }

    @Override
    public Map<String, ? extends Timer> getWakeupReasonStats() {
        return this.mWakeupReasonStats;
    }

    public BatteryStatsImpl() {
        this.mIsolatedUids = new SparseIntArray();
        this.mUidStats = new SparseArray<>();
        this.mPartialTimers = new ArrayList<>();
        this.mFullTimers = new ArrayList<>();
        this.mWindowTimers = new ArrayList<>();
        this.mSensorTimers = new SparseArray<>();
        this.mWifiRunningTimers = new ArrayList<>();
        this.mFullWifiLockTimers = new ArrayList<>();
        this.mWifiMulticastTimers = new ArrayList<>();
        this.mWifiScanTimers = new ArrayList<>();
        this.mWifiBatchedScanTimers = new SparseArray<>();
        this.mAudioTurnedOnTimers = new ArrayList<>();
        this.mVideoTurnedOnTimers = new ArrayList<>();
        this.mLastPartialTimers = new ArrayList<>();
        this.mOnBatteryTimeBase = new TimeBase();
        this.mOnBatteryScreenOffTimeBase = new TimeBase();
        this.mActiveEvents = new BatteryStats.HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryBuffer = Parcel.obtain();
        this.mHistoryLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryLastLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryReadTmp = new BatteryStats.HistoryItem();
        this.mHistoryAddTmp = new BatteryStats.HistoryItem();
        this.mHistoryTagPool = new HashMap<>();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mHistoryOverflow = false;
        this.mLastHistoryElapsedRealtime = 0L;
        this.mTrackRunningHistoryElapsedRealtime = 0L;
        this.mTrackRunningHistoryUptime = 0L;
        this.mHistoryCur = new BatteryStats.HistoryItem();
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[17];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[4];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[4];
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mBluetoothState = -1;
        this.mBluetoothStateTimer = new StopwatchTimer[4];
        this.mMobileRadioPowerState = DataConnectionRealTimeInfo.DC_POWER_STATE_LOW;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepDurations = new long[200];
        this.mChargeStepDurations = new long[200];
        this.mLastWriteTime = 0L;
        this.mBluetoothPingStart = -1;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mKernelWakelockStats = new HashMap<>();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0L;
        this.mWakeupReasonStats = new HashMap<>();
        this.mProcWakelocksName = new String[3];
        this.mProcWakelocksData = new long[3];
        this.mProcWakelockFileStats = new HashMap();
        this.mNetworkStatsFactory = new NetworkStatsFactory();
        this.mCurMobileSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mLastMobileSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mCurWifiSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mLastWifiSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mTmpNetworkStatsEntry = new NetworkStats.Entry();
        this.mMobileIfaces = new String[0];
        this.mWifiIfaces = new String[0];
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        this.mFile = null;
        this.mCheckinFile = null;
        this.mHandler = null;
        clearHistoryLocked();
    }

    static class TimeBase {
        private final ArrayList<TimeBaseObs> mObservers = new ArrayList<>();
        private long mPastRealtime;
        private long mPastUptime;
        private long mRealtime;
        private long mRealtimeStart;
        private boolean mRunning;
        private long mUnpluggedRealtime;
        private long mUnpluggedUptime;
        private long mUptime;
        private long mUptimeStart;

        TimeBase() {
        }

        public void dump(PrintWriter pw, String prefix) {
            StringBuilder sb = new StringBuilder(128);
            pw.print(prefix);
            pw.print("mRunning=");
            pw.println(this.mRunning);
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mUptime=");
            BatteryStats.formatTimeMs(sb, this.mUptime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mRealtime=");
            BatteryStats.formatTimeMs(sb, this.mRealtime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPastUptime=");
            BatteryStats.formatTimeMs(sb, this.mPastUptime / 1000);
            sb.append("mUptimeStart=");
            BatteryStats.formatTimeMs(sb, this.mUptimeStart / 1000);
            sb.append("mUnpluggedUptime=");
            BatteryStats.formatTimeMs(sb, this.mUnpluggedUptime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPastRealtime=");
            BatteryStats.formatTimeMs(sb, this.mPastRealtime / 1000);
            sb.append("mRealtimeStart=");
            BatteryStats.formatTimeMs(sb, this.mRealtimeStart / 1000);
            sb.append("mUnpluggedRealtime=");
            BatteryStats.formatTimeMs(sb, this.mUnpluggedRealtime / 1000);
            pw.println(sb.toString());
        }

        public void add(TimeBaseObs observer) {
            this.mObservers.add(observer);
        }

        public void remove(TimeBaseObs observer) {
            if (!this.mObservers.remove(observer)) {
                Slog.wtf(BatteryStatsImpl.TAG, "Removed unknown observer: " + observer);
            }
        }

        public void init(long uptime, long realtime) {
            this.mRealtime = 0L;
            this.mUptime = 0L;
            this.mPastUptime = 0L;
            this.mPastRealtime = 0L;
            this.mUptimeStart = uptime;
            this.mRealtimeStart = realtime;
            this.mUnpluggedUptime = getUptime(this.mUptimeStart);
            this.mUnpluggedRealtime = getRealtime(this.mRealtimeStart);
        }

        public void reset(long uptime, long realtime) {
            if (!this.mRunning) {
                this.mPastUptime = 0L;
                this.mPastRealtime = 0L;
            } else {
                this.mUptimeStart = uptime;
                this.mRealtimeStart = realtime;
                this.mUnpluggedUptime = getUptime(uptime);
                this.mUnpluggedRealtime = getRealtime(realtime);
            }
        }

        public long computeUptime(long curTime, int which) {
            switch (which) {
                case 0:
                    return this.mUptime + getUptime(curTime);
                case 1:
                    return getUptime(curTime);
                case 2:
                    return getUptime(curTime) - this.mUnpluggedUptime;
                default:
                    return 0L;
            }
        }

        public long computeRealtime(long curTime, int which) {
            switch (which) {
                case 0:
                    return this.mRealtime + getRealtime(curTime);
                case 1:
                    return getRealtime(curTime);
                case 2:
                    return getRealtime(curTime) - this.mUnpluggedRealtime;
                default:
                    return 0L;
            }
        }

        public long getUptime(long curTime) {
            long time = this.mPastUptime;
            if (this.mRunning) {
                return time + (curTime - this.mUptimeStart);
            }
            return time;
        }

        public long getRealtime(long curTime) {
            long time = this.mPastRealtime;
            if (this.mRunning) {
                return time + (curTime - this.mRealtimeStart);
            }
            return time;
        }

        public long getUptimeStart() {
            return this.mUptimeStart;
        }

        public long getRealtimeStart() {
            return this.mRealtimeStart;
        }

        public boolean isRunning() {
            return this.mRunning;
        }

        public boolean setRunning(boolean running, long uptime, long realtime) {
            if (this.mRunning == running) {
                return false;
            }
            this.mRunning = running;
            if (running) {
                this.mUptimeStart = uptime;
                this.mRealtimeStart = realtime;
                long batteryUptime = getUptime(uptime);
                this.mUnpluggedUptime = batteryUptime;
                long batteryRealtime = getRealtime(realtime);
                this.mUnpluggedRealtime = batteryRealtime;
                for (int i = this.mObservers.size() - 1; i >= 0; i--) {
                    this.mObservers.get(i).onTimeStarted(realtime, batteryUptime, batteryRealtime);
                }
            } else {
                this.mPastUptime += uptime - this.mUptimeStart;
                this.mPastRealtime += realtime - this.mRealtimeStart;
                long batteryUptime2 = getUptime(uptime);
                long batteryRealtime2 = getRealtime(realtime);
                for (int i2 = this.mObservers.size() - 1; i2 >= 0; i2--) {
                    this.mObservers.get(i2).onTimeStopped(realtime, batteryUptime2, batteryRealtime2);
                }
            }
            return true;
        }

        public void readSummaryFromParcel(Parcel in) {
            this.mUptime = in.readLong();
            this.mRealtime = in.readLong();
        }

        public void writeSummaryToParcel(Parcel out, long uptime, long realtime) {
            out.writeLong(computeUptime(uptime, 0));
            out.writeLong(computeRealtime(realtime, 0));
        }

        public void readFromParcel(Parcel in) {
            this.mRunning = false;
            this.mUptime = in.readLong();
            this.mPastUptime = in.readLong();
            this.mUptimeStart = in.readLong();
            this.mRealtime = in.readLong();
            this.mPastRealtime = in.readLong();
            this.mRealtimeStart = in.readLong();
            this.mUnpluggedUptime = in.readLong();
            this.mUnpluggedRealtime = in.readLong();
        }

        public void writeToParcel(Parcel out, long uptime, long realtime) {
            long runningUptime = getUptime(uptime);
            long runningRealtime = getRealtime(realtime);
            out.writeLong(this.mUptime);
            out.writeLong(runningUptime);
            out.writeLong(this.mUptimeStart);
            out.writeLong(this.mRealtime);
            out.writeLong(runningRealtime);
            out.writeLong(this.mRealtimeStart);
            out.writeLong(this.mUnpluggedUptime);
            out.writeLong(this.mUnpluggedRealtime);
        }
    }

    public static class Counter extends BatteryStats.Counter implements TimeBaseObs {
        final AtomicInteger mCount = new AtomicInteger();
        int mLastCount;
        int mLoadedCount;
        int mPluggedCount;
        final TimeBase mTimeBase;
        int mUnpluggedCount;

        Counter(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mPluggedCount = in.readInt();
            this.mCount.set(this.mPluggedCount);
            this.mLoadedCount = in.readInt();
            this.mLastCount = 0;
            this.mUnpluggedCount = in.readInt();
            timeBase.add(this);
        }

        Counter(TimeBase timeBase) {
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public void writeToParcel(Parcel out) {
            out.writeInt(this.mCount.get());
            out.writeInt(this.mLoadedCount);
            out.writeInt(this.mUnpluggedCount);
        }

        @Override
        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mUnpluggedCount = this.mPluggedCount;
            this.mCount.set(this.mPluggedCount);
        }

        @Override
        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mPluggedCount = this.mCount.get();
        }

        public static void writeCounterToParcel(Parcel out, Counter counter) {
            if (counter == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                counter.writeToParcel(out);
            }
        }

        @Override
        public int getCountLocked(int which) {
            int val = this.mCount.get();
            if (which == 2) {
                return val - this.mUnpluggedCount;
            }
            if (which != 0) {
                return val - this.mLoadedCount;
            }
            return val;
        }

        @Override
        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCount=" + this.mCount.get() + " mLoadedCount=" + this.mLoadedCount + " mLastCount=" + this.mLastCount + " mUnpluggedCount=" + this.mUnpluggedCount + " mPluggedCount=" + this.mPluggedCount);
        }

        void stepAtomic() {
            this.mCount.incrementAndGet();
        }

        void reset(boolean detachIfReset) {
            this.mCount.set(0);
            this.mUnpluggedCount = 0;
            this.mPluggedCount = 0;
            this.mLastCount = 0;
            this.mLoadedCount = 0;
            if (detachIfReset) {
                detach();
            }
        }

        void detach() {
            this.mTimeBase.remove(this);
        }

        void writeSummaryFromParcelLocked(Parcel out) {
            int count = this.mCount.get();
            out.writeInt(count);
        }

        void readSummaryFromParcelLocked(Parcel in) {
            this.mLoadedCount = in.readInt();
            this.mCount.set(this.mLoadedCount);
            this.mLastCount = 0;
            int i = this.mLoadedCount;
            this.mPluggedCount = i;
            this.mUnpluggedCount = i;
        }
    }

    public static class SamplingCounter extends Counter {
        SamplingCounter(TimeBase timeBase, Parcel in) {
            super(timeBase, in);
        }

        SamplingCounter(TimeBase timeBase) {
            super(timeBase);
        }

        public void addCountAtomic(long count) {
            this.mCount.addAndGet((int) count);
        }
    }

    public static class LongSamplingCounter extends BatteryStats.LongCounter implements TimeBaseObs {
        long mCount;
        long mLastCount;
        long mLoadedCount;
        long mPluggedCount;
        final TimeBase mTimeBase;
        long mUnpluggedCount;

        LongSamplingCounter(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mPluggedCount = in.readLong();
            this.mCount = this.mPluggedCount;
            this.mLoadedCount = in.readLong();
            this.mLastCount = 0L;
            this.mUnpluggedCount = in.readLong();
            timeBase.add(this);
        }

        LongSamplingCounter(TimeBase timeBase) {
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public void writeToParcel(Parcel out) {
            out.writeLong(this.mCount);
            out.writeLong(this.mLoadedCount);
            out.writeLong(this.mUnpluggedCount);
        }

        @Override
        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mUnpluggedCount = this.mPluggedCount;
            this.mCount = this.mPluggedCount;
        }

        @Override
        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mPluggedCount = this.mCount;
        }

        @Override
        public long getCountLocked(int which) {
            long val = this.mCount;
            if (which == 2) {
                return val - this.mUnpluggedCount;
            }
            if (which != 0) {
                return val - this.mLoadedCount;
            }
            return val;
        }

        @Override
        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCount=" + this.mCount + " mLoadedCount=" + this.mLoadedCount + " mLastCount=" + this.mLastCount + " mUnpluggedCount=" + this.mUnpluggedCount + " mPluggedCount=" + this.mPluggedCount);
        }

        void addCountLocked(long count) {
            this.mCount += count;
        }

        void reset(boolean detachIfReset) {
            this.mCount = 0L;
            this.mUnpluggedCount = 0L;
            this.mPluggedCount = 0L;
            this.mLastCount = 0L;
            this.mLoadedCount = 0L;
            if (detachIfReset) {
                detach();
            }
        }

        void detach() {
            this.mTimeBase.remove(this);
        }

        void writeSummaryFromParcelLocked(Parcel out) {
            out.writeLong(this.mCount);
        }

        void readSummaryFromParcelLocked(Parcel in) {
            this.mLoadedCount = in.readLong();
            this.mCount = this.mLoadedCount;
            this.mLastCount = 0L;
            long j = this.mLoadedCount;
            this.mPluggedCount = j;
            this.mUnpluggedCount = j;
        }
    }

    public static abstract class Timer extends BatteryStats.Timer implements TimeBaseObs {
        int mCount;
        int mLastCount;
        long mLastTime;
        int mLoadedCount;
        long mLoadedTime;
        final TimeBase mTimeBase;
        long mTotalTime;
        final int mType;
        int mUnpluggedCount;
        long mUnpluggedTime;

        protected abstract int computeCurrentCountLocked();

        protected abstract long computeRunTimeLocked(long j);

        Timer(int type, TimeBase timeBase, Parcel in) {
            this.mType = type;
            this.mTimeBase = timeBase;
            this.mCount = in.readInt();
            this.mLoadedCount = in.readInt();
            this.mLastCount = 0;
            this.mUnpluggedCount = in.readInt();
            this.mTotalTime = in.readLong();
            this.mLoadedTime = in.readLong();
            this.mLastTime = 0L;
            this.mUnpluggedTime = in.readLong();
            timeBase.add(this);
        }

        Timer(int type, TimeBase timeBase) {
            this.mType = type;
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        boolean reset(boolean detachIfReset) {
            this.mLastTime = 0L;
            this.mLoadedTime = 0L;
            this.mTotalTime = 0L;
            this.mLastCount = 0;
            this.mLoadedCount = 0;
            this.mCount = 0;
            if (detachIfReset) {
                detach();
                return true;
            }
            return true;
        }

        void detach() {
            this.mTimeBase.remove(this);
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            out.writeInt(this.mCount);
            out.writeInt(this.mLoadedCount);
            out.writeInt(this.mUnpluggedCount);
            out.writeLong(computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs)));
            out.writeLong(this.mLoadedTime);
            out.writeLong(this.mUnpluggedTime);
        }

        public void onTimeStarted(long elapsedRealtime, long timeBaseUptime, long baseRealtime) {
            this.mUnpluggedTime = computeRunTimeLocked(baseRealtime);
            this.mUnpluggedCount = this.mCount;
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mTotalTime = computeRunTimeLocked(baseRealtime);
            this.mCount = computeCurrentCountLocked();
        }

        public static void writeTimerToParcel(Parcel out, Timer timer, long elapsedRealtimeUs) {
            if (timer == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                timer.writeToParcel(out, elapsedRealtimeUs);
            }
        }

        @Override
        public long getTotalTimeLocked(long elapsedRealtimeUs, int which) {
            long val = computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs));
            if (which == 2) {
                return val - this.mUnpluggedTime;
            }
            if (which != 0) {
                return val - this.mLoadedTime;
            }
            return val;
        }

        @Override
        public int getCountLocked(int which) {
            int val = computeCurrentCountLocked();
            if (which == 2) {
                return val - this.mUnpluggedCount;
            }
            if (which != 0) {
                return val - this.mLoadedCount;
            }
            return val;
        }

        @Override
        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCount=" + this.mCount + " mLoadedCount=" + this.mLoadedCount + " mLastCount=" + this.mLastCount + " mUnpluggedCount=" + this.mUnpluggedCount);
            pw.println(prefix + "mTotalTime=" + this.mTotalTime + " mLoadedTime=" + this.mLoadedTime);
            pw.println(prefix + "mLastTime=" + this.mLastTime + " mUnpluggedTime=" + this.mUnpluggedTime);
        }

        void writeSummaryFromParcelLocked(Parcel out, long elapsedRealtimeUs) {
            long runTime = computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs));
            out.writeLong(runTime);
            out.writeInt(this.mCount);
        }

        void readSummaryFromParcelLocked(Parcel in) {
            long j = in.readLong();
            this.mLoadedTime = j;
            this.mTotalTime = j;
            this.mLastTime = 0L;
            this.mUnpluggedTime = this.mTotalTime;
            int i = in.readInt();
            this.mLoadedCount = i;
            this.mCount = i;
            this.mLastCount = 0;
            this.mUnpluggedCount = this.mCount;
        }
    }

    public static final class SamplingTimer extends Timer {
        int mCurrentReportedCount;
        long mCurrentReportedTotalTime;
        boolean mTimeBaseRunning;
        boolean mTrackingReportedValues;
        int mUnpluggedReportedCount;
        long mUnpluggedReportedTotalTime;
        int mUpdateVersion;

        SamplingTimer(TimeBase timeBase, Parcel in) {
            super(0, timeBase, in);
            this.mCurrentReportedCount = in.readInt();
            this.mUnpluggedReportedCount = in.readInt();
            this.mCurrentReportedTotalTime = in.readLong();
            this.mUnpluggedReportedTotalTime = in.readLong();
            this.mTrackingReportedValues = in.readInt() == 1;
            this.mTimeBaseRunning = timeBase.isRunning();
        }

        SamplingTimer(TimeBase timeBase, boolean trackReportedValues) {
            super(0, timeBase);
            this.mTrackingReportedValues = trackReportedValues;
            this.mTimeBaseRunning = timeBase.isRunning();
        }

        public void setStale() {
            this.mTrackingReportedValues = false;
            this.mUnpluggedReportedTotalTime = 0L;
            this.mUnpluggedReportedCount = 0;
        }

        public void setUpdateVersion(int version) {
            this.mUpdateVersion = version;
        }

        public int getUpdateVersion() {
            return this.mUpdateVersion;
        }

        public void updateCurrentReportedCount(int count) {
            if (this.mTimeBaseRunning && this.mUnpluggedReportedCount == 0) {
                this.mUnpluggedReportedCount = count;
                this.mTrackingReportedValues = true;
            }
            this.mCurrentReportedCount = count;
        }

        public void addCurrentReportedCount(int delta) {
            updateCurrentReportedCount(this.mCurrentReportedCount + delta);
        }

        public void updateCurrentReportedTotalTime(long totalTime) {
            if (this.mTimeBaseRunning && this.mUnpluggedReportedTotalTime == 0) {
                this.mUnpluggedReportedTotalTime = totalTime;
                this.mTrackingReportedValues = true;
            }
            this.mCurrentReportedTotalTime = totalTime;
        }

        public void addCurrentReportedTotalTime(long delta) {
            updateCurrentReportedTotalTime(this.mCurrentReportedTotalTime + delta);
        }

        @Override
        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            super.onTimeStarted(elapsedRealtime, baseUptime, baseRealtime);
            if (this.mTrackingReportedValues) {
                this.mUnpluggedReportedTotalTime = this.mCurrentReportedTotalTime;
                this.mUnpluggedReportedCount = this.mCurrentReportedCount;
            }
            this.mTimeBaseRunning = true;
        }

        @Override
        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            super.onTimeStopped(elapsedRealtime, baseUptime, baseRealtime);
            this.mTimeBaseRunning = false;
        }

        @Override
        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            pw.println(prefix + "mCurrentReportedCount=" + this.mCurrentReportedCount + " mUnpluggedReportedCount=" + this.mUnpluggedReportedCount + " mCurrentReportedTotalTime=" + this.mCurrentReportedTotalTime + " mUnpluggedReportedTotalTime=" + this.mUnpluggedReportedTotalTime);
        }

        @Override
        protected long computeRunTimeLocked(long curBatteryRealtime) {
            return ((this.mTimeBaseRunning && this.mTrackingReportedValues) ? this.mCurrentReportedTotalTime - this.mUnpluggedReportedTotalTime : 0L) + this.mTotalTime;
        }

        @Override
        protected int computeCurrentCountLocked() {
            return ((this.mTimeBaseRunning && this.mTrackingReportedValues) ? this.mCurrentReportedCount - this.mUnpluggedReportedCount : 0) + this.mCount;
        }

        @Override
        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeInt(this.mCurrentReportedCount);
            out.writeInt(this.mUnpluggedReportedCount);
            out.writeLong(this.mCurrentReportedTotalTime);
            out.writeLong(this.mUnpluggedReportedTotalTime);
            out.writeInt(this.mTrackingReportedValues ? 1 : 0);
        }

        @Override
        boolean reset(boolean detachIfReset) {
            super.reset(detachIfReset);
            setStale();
            return true;
        }

        @Override
        void writeSummaryFromParcelLocked(Parcel out, long batteryRealtime) {
            super.writeSummaryFromParcelLocked(out, batteryRealtime);
            out.writeLong(this.mCurrentReportedTotalTime);
            out.writeInt(this.mCurrentReportedCount);
            out.writeInt(this.mTrackingReportedValues ? 1 : 0);
        }

        @Override
        void readSummaryFromParcelLocked(Parcel in) {
            super.readSummaryFromParcelLocked(in);
            long j = in.readLong();
            this.mCurrentReportedTotalTime = j;
            this.mUnpluggedReportedTotalTime = j;
            int i = in.readInt();
            this.mCurrentReportedCount = i;
            this.mUnpluggedReportedCount = i;
            this.mTrackingReportedValues = in.readInt() == 1;
        }
    }

    public static final class BatchTimer extends Timer {
        boolean mInDischarge;
        long mLastAddedDuration;
        long mLastAddedTime;
        final Uid mUid;

        BatchTimer(Uid uid, int type, TimeBase timeBase, Parcel in) {
            super(type, timeBase, in);
            this.mUid = uid;
            this.mLastAddedTime = in.readLong();
            this.mLastAddedDuration = in.readLong();
            this.mInDischarge = timeBase.isRunning();
        }

        BatchTimer(Uid uid, int type, TimeBase timeBase) {
            super(type, timeBase);
            this.mUid = uid;
            this.mInDischarge = timeBase.isRunning();
        }

        @Override
        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeLong(this.mLastAddedTime);
            out.writeLong(this.mLastAddedDuration);
        }

        @Override
        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            recomputeLastDuration(SystemClock.elapsedRealtime() * 1000, false);
            this.mInDischarge = false;
            super.onTimeStopped(elapsedRealtime, baseUptime, baseRealtime);
        }

        @Override
        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            recomputeLastDuration(elapsedRealtime, false);
            this.mInDischarge = true;
            if (this.mLastAddedTime == elapsedRealtime) {
                this.mTotalTime += this.mLastAddedDuration;
            }
            super.onTimeStarted(elapsedRealtime, baseUptime, baseRealtime);
        }

        @Override
        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            pw.println(prefix + "mLastAddedTime=" + this.mLastAddedTime + " mLastAddedDuration=" + this.mLastAddedDuration);
        }

        private long computeOverage(long curTime) {
            if (this.mLastAddedTime > 0) {
                return (this.mLastTime + this.mLastAddedDuration) - curTime;
            }
            return 0L;
        }

        private void recomputeLastDuration(long curTime, boolean abort) {
            long overage = computeOverage(curTime);
            if (overage > 0) {
                if (this.mInDischarge) {
                    this.mTotalTime -= overage;
                }
                if (abort) {
                    this.mLastAddedTime = 0L;
                } else {
                    this.mLastAddedTime = curTime;
                    this.mLastAddedDuration -= overage;
                }
            }
        }

        public void addDuration(BatteryStatsImpl stats, long durationMillis) {
            long now = SystemClock.elapsedRealtime() * 1000;
            recomputeLastDuration(now, true);
            this.mLastAddedTime = now;
            this.mLastAddedDuration = durationMillis * 1000;
            if (this.mInDischarge) {
                this.mTotalTime += this.mLastAddedDuration;
                this.mCount++;
            }
        }

        public void abortLastDuration(BatteryStatsImpl stats) {
            long now = SystemClock.elapsedRealtime() * 1000;
            recomputeLastDuration(now, true);
        }

        @Override
        protected int computeCurrentCountLocked() {
            return this.mCount;
        }

        @Override
        protected long computeRunTimeLocked(long curBatteryRealtime) {
            long overage = computeOverage(SystemClock.elapsedRealtime() * 1000);
            if (overage <= 0) {
                return this.mTotalTime;
            }
            this.mTotalTime = overage;
            return overage;
        }

        @Override
        boolean reset(boolean detachIfReset) {
            long now = SystemClock.elapsedRealtime() * 1000;
            recomputeLastDuration(now, true);
            boolean stillActive = this.mLastAddedTime == now;
            super.reset(!stillActive && detachIfReset);
            return !stillActive;
        }
    }

    public static final class StopwatchTimer extends Timer {
        long mAcquireTime;
        boolean mInList;
        int mNesting;
        long mTimeout;
        final ArrayList<StopwatchTimer> mTimerPool;
        final Uid mUid;
        long mUpdateTime;

        StopwatchTimer(Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, Parcel in) {
            super(type, timeBase, in);
            this.mUid = uid;
            this.mTimerPool = timerPool;
            this.mUpdateTime = in.readLong();
        }

        StopwatchTimer(Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase) {
            super(type, timeBase);
            this.mUid = uid;
            this.mTimerPool = timerPool;
        }

        void setTimeout(long timeout) {
            this.mTimeout = timeout;
        }

        @Override
        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeLong(this.mUpdateTime);
        }

        @Override
        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            if (this.mNesting > 0) {
                super.onTimeStopped(elapsedRealtime, baseUptime, baseRealtime);
                this.mUpdateTime = baseRealtime;
            }
        }

        @Override
        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            pw.println(prefix + "mNesting=" + this.mNesting + " mUpdateTime=" + this.mUpdateTime + " mAcquireTime=" + this.mAcquireTime);
        }

        void startRunningLocked(long elapsedRealtimeMs) {
            int i = this.mNesting;
            this.mNesting = i + 1;
            if (i == 0) {
                long batteryRealtime = this.mTimeBase.getRealtime(1000 * elapsedRealtimeMs);
                this.mUpdateTime = batteryRealtime;
                if (this.mTimerPool != null) {
                    refreshTimersLocked(batteryRealtime, this.mTimerPool, null);
                    this.mTimerPool.add(this);
                }
                this.mCount++;
                this.mAcquireTime = this.mTotalTime;
            }
        }

        boolean isRunningLocked() {
            return this.mNesting > 0;
        }

        long checkpointRunningLocked(long elapsedRealtimeMs) {
            if (this.mNesting > 0) {
                long batteryRealtime = this.mTimeBase.getRealtime(1000 * elapsedRealtimeMs);
                if (this.mTimerPool != null) {
                    return refreshTimersLocked(batteryRealtime, this.mTimerPool, this);
                }
                long heldTime = batteryRealtime - this.mUpdateTime;
                this.mUpdateTime = batteryRealtime;
                this.mTotalTime += heldTime;
                return heldTime;
            }
            return 0L;
        }

        void stopRunningLocked(long elapsedRealtimeMs) {
            if (this.mNesting != 0) {
                int i = this.mNesting - 1;
                this.mNesting = i;
                if (i == 0) {
                    long batteryRealtime = this.mTimeBase.getRealtime(1000 * elapsedRealtimeMs);
                    if (this.mTimerPool != null) {
                        refreshTimersLocked(batteryRealtime, this.mTimerPool, null);
                        this.mTimerPool.remove(this);
                    } else {
                        this.mNesting = 1;
                        this.mTotalTime = computeRunTimeLocked(batteryRealtime);
                        this.mNesting = 0;
                    }
                    if (this.mTotalTime == this.mAcquireTime) {
                        this.mCount--;
                    }
                }
            }
        }

        void stopAllRunningLocked(long elapsedRealtimeMs) {
            if (this.mNesting > 0) {
                this.mNesting = 1;
                stopRunningLocked(elapsedRealtimeMs);
            }
        }

        private static long refreshTimersLocked(long batteryRealtime, ArrayList<StopwatchTimer> pool, StopwatchTimer self) {
            long selfTime = 0;
            int N = pool.size();
            for (int i = N - 1; i >= 0; i--) {
                StopwatchTimer t = pool.get(i);
                long heldTime = batteryRealtime - t.mUpdateTime;
                if (heldTime > 0) {
                    long myTime = heldTime / ((long) N);
                    if (t == self) {
                        selfTime = myTime;
                    }
                    t.mTotalTime += myTime;
                }
                t.mUpdateTime = batteryRealtime;
            }
            return selfTime;
        }

        @Override
        protected long computeRunTimeLocked(long curBatteryRealtime) {
            long size = 0;
            if (this.mTimeout > 0 && curBatteryRealtime > this.mUpdateTime + this.mTimeout) {
                curBatteryRealtime = this.mUpdateTime + this.mTimeout;
            }
            long j = this.mTotalTime;
            if (this.mNesting > 0) {
                size = (curBatteryRealtime - this.mUpdateTime) / ((long) (this.mTimerPool != null ? this.mTimerPool.size() : 1));
            }
            return size + j;
        }

        @Override
        protected int computeCurrentCountLocked() {
            return this.mCount;
        }

        @Override
        boolean reset(boolean detachIfReset) {
            boolean canDetach = this.mNesting <= 0;
            super.reset(canDetach && detachIfReset);
            if (this.mNesting > 0) {
                this.mUpdateTime = this.mTimeBase.getRealtime(SystemClock.elapsedRealtime() * 1000);
            }
            this.mAcquireTime = this.mTotalTime;
            return canDetach;
        }

        @Override
        void detach() {
            super.detach();
            if (this.mTimerPool != null) {
                this.mTimerPool.remove(this);
            }
        }

        @Override
        void readSummaryFromParcelLocked(Parcel in) {
            super.readSummaryFromParcelLocked(in);
            this.mNesting = 0;
        }
    }

    public abstract class OverflowArrayMap<T> {
        private static final String OVERFLOW_NAME = "*overflow*";
        ArrayMap<String, MutableInt> mActiveOverflow;
        T mCurOverflow;
        final ArrayMap<String, T> mMap = new ArrayMap<>();

        public abstract T instantiateObject();

        public OverflowArrayMap() {
        }

        public ArrayMap<String, T> getMap() {
            return this.mMap;
        }

        public void clear() {
            this.mMap.clear();
            this.mCurOverflow = null;
            this.mActiveOverflow = null;
        }

        public void add(String name, T obj) {
            this.mMap.put(name, obj);
            if (OVERFLOW_NAME.equals(name)) {
                this.mCurOverflow = obj;
            }
        }

        public void cleanup() {
            if (this.mActiveOverflow != null && this.mActiveOverflow.size() == 0) {
                this.mActiveOverflow = null;
            }
            if (this.mActiveOverflow == null) {
                if (this.mMap.containsKey(OVERFLOW_NAME)) {
                    Slog.wtf(BatteryStatsImpl.TAG, "Cleaning up with no active overflow, but have overflow entry " + this.mMap.get(OVERFLOW_NAME));
                    this.mMap.remove(OVERFLOW_NAME);
                }
                this.mCurOverflow = null;
                return;
            }
            if (this.mCurOverflow == null || !this.mMap.containsKey(OVERFLOW_NAME)) {
                Slog.wtf(BatteryStatsImpl.TAG, "Cleaning up with active overflow, but no overflow entry: cur=" + this.mCurOverflow + " map=" + this.mMap.get(OVERFLOW_NAME));
            }
        }

        public T startObject(String name) {
            MutableInt over;
            T obj = this.mMap.get(name);
            if (obj != null) {
                return obj;
            }
            if (this.mActiveOverflow != null && (over = this.mActiveOverflow.get(name)) != null) {
                T obj2 = this.mCurOverflow;
                if (obj2 == null) {
                    Slog.wtf(BatteryStatsImpl.TAG, "Have active overflow " + name + " but null overflow");
                    obj2 = instantiateObject();
                    this.mCurOverflow = obj2;
                    this.mMap.put(OVERFLOW_NAME, obj2);
                }
                over.value++;
                return obj2;
            }
            int N = this.mMap.size();
            if (N >= 100) {
                T obj3 = this.mCurOverflow;
                if (obj3 == null) {
                    obj3 = instantiateObject();
                    this.mCurOverflow = obj3;
                    this.mMap.put(OVERFLOW_NAME, obj3);
                }
                if (this.mActiveOverflow == null) {
                    this.mActiveOverflow = new ArrayMap<>();
                }
                this.mActiveOverflow.put(name, new MutableInt(1));
                return obj3;
            }
            T obj4 = instantiateObject();
            this.mMap.put(name, obj4);
            return obj4;
        }

        public T stopObject(String name) {
            MutableInt over;
            T obj;
            T obj2 = this.mMap.get(name);
            if (obj2 != null) {
                return obj2;
            }
            if (this.mActiveOverflow != null && (over = this.mActiveOverflow.get(name)) != null && (obj = this.mCurOverflow) != null) {
                over.value--;
                if (over.value <= 0) {
                    this.mActiveOverflow.remove(name);
                }
                return obj;
            }
            Slog.wtf(BatteryStatsImpl.TAG, "Unable to find object for " + name + " mapsize=" + this.mMap.size() + " activeoverflow=" + this.mActiveOverflow + " curoverflow=" + this.mCurOverflow);
            return null;
        }
    }

    public SamplingTimer getWakeupReasonTimerLocked(String name) {
        SamplingTimer timer = this.mWakeupReasonStats.get(name);
        if (timer == null) {
            SamplingTimer timer2 = new SamplingTimer(this.mOnBatteryTimeBase, true);
            this.mWakeupReasonStats.put(name, timer2);
            return timer2;
        }
        return timer;
    }

    private final Map<String, KernelWakelockStats> readKernelWakelockStats() {
        FileInputStream is;
        byte[] buffer = new byte[8192];
        boolean wakeup_sources = false;
        try {
            try {
                is = new FileInputStream("/proc/wakelocks");
            } catch (FileNotFoundException e) {
                try {
                    is = new FileInputStream("/d/wakeup_sources");
                    wakeup_sources = true;
                } catch (FileNotFoundException e2) {
                    return null;
                }
            }
            int len = is.read(buffer);
            is.close();
            if (len > 0) {
                int i = 0;
                while (true) {
                    if (i >= len) {
                        break;
                    }
                    if (buffer[i] != 0) {
                        i++;
                    } else {
                        len = i;
                        break;
                    }
                }
            }
            return parseProcWakelocks(buffer, len, wakeup_sources);
        } catch (IOException e3) {
            return null;
        }
    }

    private final Map<String, KernelWakelockStats> parseProcWakelocks(byte[] wlBuffer, int len, boolean wakeup_sources) {
        Map<String, KernelWakelockStats> m;
        long totalTime;
        int numUpdatedWlNames = 0;
        int i = 0;
        while (i < len && wlBuffer[i] != 10 && wlBuffer[i] != 0) {
            i++;
        }
        int endIndex = i + 1;
        int startIndex = endIndex;
        synchronized (this) {
            m = this.mProcWakelockFileStats;
            sKernelWakelockUpdateVersion++;
            while (true) {
                if (endIndex < len) {
                    int endIndex2 = startIndex;
                    while (endIndex2 < len && wlBuffer[endIndex2] != 10 && wlBuffer[endIndex2] != 0) {
                        endIndex2++;
                    }
                    endIndex = endIndex2 + 1;
                    if (endIndex >= len - 1) {
                        break;
                    }
                    String[] nameStringArray = this.mProcWakelocksName;
                    long[] wlData = this.mProcWakelocksData;
                    for (int j = startIndex; j < endIndex; j++) {
                        if ((wlBuffer[j] & 128) != 0) {
                            wlBuffer[j] = 63;
                        }
                    }
                    boolean parsed = Process.parseProcLine(wlBuffer, startIndex, endIndex, wakeup_sources ? WAKEUP_SOURCES_FORMAT : PROC_WAKELOCKS_FORMAT, nameStringArray, wlData, null);
                    String name = nameStringArray[0];
                    int count = (int) wlData[1];
                    if (wakeup_sources) {
                        totalTime = wlData[2] * 1000;
                    } else {
                        totalTime = (wlData[2] + 500) / 1000;
                    }
                    if (parsed && name.length() > 0) {
                        if (!m.containsKey(name)) {
                            m.put(name, new KernelWakelockStats(count, totalTime, sKernelWakelockUpdateVersion));
                            numUpdatedWlNames++;
                        } else {
                            KernelWakelockStats kwlStats = m.get(name);
                            if (kwlStats.mVersion == sKernelWakelockUpdateVersion) {
                                kwlStats.mCount += count;
                                kwlStats.mTotalTime += totalTime;
                            } else {
                                kwlStats.mCount = count;
                                kwlStats.mTotalTime = totalTime;
                                kwlStats.mVersion = sKernelWakelockUpdateVersion;
                                numUpdatedWlNames++;
                            }
                        }
                    }
                    startIndex = endIndex;
                } else if (m.size() != numUpdatedWlNames) {
                    Iterator<KernelWakelockStats> itr = m.values().iterator();
                    while (itr.hasNext()) {
                        if (itr.next().mVersion != sKernelWakelockUpdateVersion) {
                            itr.remove();
                        }
                    }
                }
            }
        }
        return m;
    }

    private class KernelWakelockStats {
        public int mCount;
        public long mTotalTime;
        public int mVersion;

        KernelWakelockStats(int count, long totalTime, int version) {
            this.mCount = count;
            this.mTotalTime = totalTime;
            this.mVersion = version;
        }
    }

    public SamplingTimer getKernelWakelockTimerLocked(String name) {
        SamplingTimer kwlt = this.mKernelWakelockStats.get(name);
        if (kwlt == null) {
            SamplingTimer kwlt2 = new SamplingTimer(this.mOnBatteryScreenOffTimeBase, true);
            this.mKernelWakelockStats.put(name, kwlt2);
            return kwlt2;
        }
        return kwlt;
    }

    private int getCurrentBluetoothPingCount() {
        if (this.mBtHeadset != null) {
            List<BluetoothDevice> deviceList = this.mBtHeadset.getConnectedDevices();
            if (deviceList.size() > 0) {
                return this.mBtHeadset.getBatteryUsageHint(deviceList.get(0));
            }
        }
        return -1;
    }

    @Override
    public int getBluetoothPingCount() {
        if (this.mBluetoothPingStart == -1) {
            return this.mBluetoothPingCount;
        }
        if (this.mBtHeadset != null) {
            return getCurrentBluetoothPingCount() - this.mBluetoothPingStart;
        }
        return 0;
    }

    public void setBtHeadset(BluetoothHeadset headset) {
        if (headset != null && this.mBtHeadset == null && isOnBattery() && this.mBluetoothPingStart == -1) {
            this.mBluetoothPingStart = getCurrentBluetoothPingCount();
        }
        this.mBtHeadset = headset;
    }

    private int writeHistoryTag(BatteryStats.HistoryTag tag) {
        Integer idxObj = this.mHistoryTagPool.get(tag);
        if (idxObj != null) {
            return idxObj.intValue();
        }
        int idx = this.mNextHistoryTagIdx;
        BatteryStats.HistoryTag key = new BatteryStats.HistoryTag();
        key.setTo(tag);
        tag.poolIdx = idx;
        this.mHistoryTagPool.put(key, Integer.valueOf(idx));
        this.mNextHistoryTagIdx++;
        this.mNumHistoryTagChars += key.string.length() + 1;
        return idx;
    }

    private void readHistoryTag(int index, BatteryStats.HistoryTag tag) {
        tag.string = this.mReadHistoryStrings[index];
        tag.uid = this.mReadHistoryUids[index];
        tag.poolIdx = index;
    }

    public void writeHistoryDelta(Parcel dest, BatteryStats.HistoryItem cur, BatteryStats.HistoryItem last) {
        int deltaTimeToken;
        int wakeLockIndex;
        int wakeReasonIndex;
        if (last == null || cur.cmd != 0) {
            dest.writeInt(DELTA_TIME_ABS);
            cur.writeToParcel(dest, 0);
            return;
        }
        long deltaTime = cur.time - last.time;
        int lastBatteryLevelInt = buildBatteryLevelInt(last);
        int lastStateInt = buildStateInt(last);
        if (deltaTime < 0 || deltaTime > 2147483647L) {
            deltaTimeToken = 524287;
        } else if (deltaTime >= 524285) {
            deltaTimeToken = DELTA_TIME_INT;
        } else {
            deltaTimeToken = (int) deltaTime;
        }
        int firstToken = deltaTimeToken | (cur.states & (-16777216));
        int batteryLevelInt = buildBatteryLevelInt(cur);
        boolean batteryLevelIntChanged = batteryLevelInt != lastBatteryLevelInt;
        if (batteryLevelIntChanged) {
            firstToken |= 524288;
        }
        int stateInt = buildStateInt(cur);
        boolean stateIntChanged = stateInt != lastStateInt;
        if (stateIntChanged) {
            firstToken |= 1048576;
        }
        boolean state2IntChanged = cur.states2 != last.states2;
        if (state2IntChanged) {
            firstToken |= 2097152;
        }
        if (cur.wakelockTag != null || cur.wakeReasonTag != null) {
            firstToken |= 4194304;
        }
        if (cur.eventCode != 0) {
            firstToken |= 8388608;
        }
        dest.writeInt(firstToken);
        if (deltaTimeToken >= DELTA_TIME_INT) {
            if (deltaTimeToken == DELTA_TIME_INT) {
                dest.writeInt((int) deltaTime);
            } else {
                dest.writeLong(deltaTime);
            }
        }
        if (batteryLevelIntChanged) {
            dest.writeInt(batteryLevelInt);
        }
        if (stateIntChanged) {
            dest.writeInt(stateInt);
        }
        if (state2IntChanged) {
            dest.writeInt(cur.states2);
        }
        if (cur.wakelockTag != null || cur.wakeReasonTag != null) {
            if (cur.wakelockTag != null) {
                wakeLockIndex = writeHistoryTag(cur.wakelockTag);
            } else {
                wakeLockIndex = 65535;
            }
            if (cur.wakeReasonTag != null) {
                wakeReasonIndex = writeHistoryTag(cur.wakeReasonTag);
            } else {
                wakeReasonIndex = 65535;
            }
            dest.writeInt((wakeReasonIndex << 16) | wakeLockIndex);
        }
        if (cur.eventCode != 0) {
            int index = writeHistoryTag(cur.eventTag);
            int codeAndIndex = (cur.eventCode & 65535) | (index << 16);
            dest.writeInt(codeAndIndex);
        }
    }

    private int buildBatteryLevelInt(BatteryStats.HistoryItem h) {
        return ((h.batteryLevel << 25) & (-33554432)) | ((h.batteryTemperature << 14) & 33538048) | (h.batteryVoltage & 16383);
    }

    private int buildStateInt(BatteryStats.HistoryItem h) {
        int plugType = 0;
        if ((h.batteryPlugType & 1) != 0) {
            plugType = 1;
        } else if ((h.batteryPlugType & 2) != 0) {
            plugType = 2;
        } else if ((h.batteryPlugType & 4) != 0) {
            plugType = 3;
        }
        return ((h.batteryStatus & 7) << 29) | ((h.batteryHealth & 7) << 26) | ((plugType & 3) << 24) | (h.states & 16777215);
    }

    public void readHistoryDelta(Parcel src, BatteryStats.HistoryItem cur) {
        int firstToken = src.readInt();
        int deltaTimeToken = firstToken & 524287;
        cur.cmd = (byte) 0;
        cur.numReadInts = 1;
        if (deltaTimeToken < DELTA_TIME_ABS) {
            cur.time += (long) deltaTimeToken;
        } else if (deltaTimeToken == DELTA_TIME_ABS) {
            cur.time = src.readLong();
            cur.numReadInts += 2;
            cur.readFromParcel(src);
            return;
        } else if (deltaTimeToken == DELTA_TIME_INT) {
            int delta = src.readInt();
            cur.time += (long) delta;
            cur.numReadInts++;
        } else {
            long delta2 = src.readLong();
            cur.time += delta2;
            cur.numReadInts += 2;
        }
        if ((524288 & firstToken) != 0) {
            int batteryLevelInt = src.readInt();
            cur.batteryLevel = (byte) ((batteryLevelInt >> 25) & 127);
            cur.batteryTemperature = (short) ((batteryLevelInt << 7) >> 21);
            cur.batteryVoltage = (char) (batteryLevelInt & View.PUBLIC_STATUS_BAR_VISIBILITY_MASK);
            cur.numReadInts++;
        }
        if ((1048576 & firstToken) != 0) {
            int stateInt = src.readInt();
            cur.states = ((-16777216) & firstToken) | (16777215 & stateInt);
            cur.batteryStatus = (byte) ((stateInt >> 29) & 7);
            cur.batteryHealth = (byte) ((stateInt >> 26) & 7);
            cur.batteryPlugType = (byte) ((stateInt >> 24) & 3);
            switch (cur.batteryPlugType) {
                case 1:
                    cur.batteryPlugType = (byte) 1;
                    break;
                case 2:
                    cur.batteryPlugType = (byte) 2;
                    break;
                case 3:
                    cur.batteryPlugType = (byte) 4;
                    break;
            }
            cur.numReadInts++;
        } else {
            cur.states = ((-16777216) & firstToken) | (cur.states & 16777215);
        }
        if ((2097152 & firstToken) != 0) {
            cur.states2 = src.readInt();
        }
        if ((4194304 & firstToken) != 0) {
            int indexes = src.readInt();
            int wakeLockIndex = indexes & 65535;
            int wakeReasonIndex = (indexes >> 16) & 65535;
            if (wakeLockIndex != 65535) {
                cur.wakelockTag = cur.localWakelockTag;
                readHistoryTag(wakeLockIndex, cur.wakelockTag);
            } else {
                cur.wakelockTag = null;
            }
            if (wakeReasonIndex != 65535) {
                cur.wakeReasonTag = cur.localWakeReasonTag;
                readHistoryTag(wakeReasonIndex, cur.wakeReasonTag);
            } else {
                cur.wakeReasonTag = null;
            }
            cur.numReadInts++;
        } else {
            cur.wakelockTag = null;
            cur.wakeReasonTag = null;
        }
        if ((8388608 & firstToken) != 0) {
            cur.eventTag = cur.localEventTag;
            int codeAndIndex = src.readInt();
            cur.eventCode = 65535 & codeAndIndex;
            int index = (codeAndIndex >> 16) & 65535;
            readHistoryTag(index, cur.eventTag);
            cur.numReadInts++;
            return;
        }
        cur.eventCode = 0;
    }

    @Override
    public void commitCurrentHistoryBatchLocked() {
        this.mHistoryLastWritten.cmd = (byte) -1;
    }

    void addHistoryBufferLocked(long elapsedRealtimeMs, long uptimeMs, BatteryStats.HistoryItem cur) {
        if (this.mHaveBatteryLevel && this.mRecordingHistory) {
            long timeDiff = (this.mHistoryBaseTime + elapsedRealtimeMs) - this.mHistoryLastWritten.time;
            int diffStates = this.mHistoryLastWritten.states ^ cur.states;
            int diffStates2 = this.mHistoryLastWritten.states2 ^ cur.states2;
            int lastDiffStates = this.mHistoryLastWritten.states ^ this.mHistoryLastLastWritten.states;
            int lastDiffStates2 = this.mHistoryLastWritten.states2 ^ this.mHistoryLastLastWritten.states2;
            if (this.mHistoryBufferLastPos >= 0 && this.mHistoryLastWritten.cmd == 0 && timeDiff < 1000 && (diffStates & lastDiffStates) == 0 && (diffStates2 & lastDiffStates2) == 0 && ((this.mHistoryLastWritten.wakelockTag == null || cur.wakelockTag == null) && ((this.mHistoryLastWritten.wakeReasonTag == null || cur.wakeReasonTag == null) && ((this.mHistoryLastWritten.eventCode == 0 || cur.eventCode == 0) && this.mHistoryLastWritten.batteryLevel == cur.batteryLevel && this.mHistoryLastWritten.batteryStatus == cur.batteryStatus && this.mHistoryLastWritten.batteryHealth == cur.batteryHealth && this.mHistoryLastWritten.batteryPlugType == cur.batteryPlugType && this.mHistoryLastWritten.batteryTemperature == cur.batteryTemperature && this.mHistoryLastWritten.batteryVoltage == cur.batteryVoltage)))) {
                this.mHistoryBuffer.setDataSize(this.mHistoryBufferLastPos);
                this.mHistoryBuffer.setDataPosition(this.mHistoryBufferLastPos);
                this.mHistoryBufferLastPos = -1;
                elapsedRealtimeMs = this.mHistoryLastWritten.time - this.mHistoryBaseTime;
                if (this.mHistoryLastWritten.wakelockTag != null) {
                    cur.wakelockTag = cur.localWakelockTag;
                    cur.wakelockTag.setTo(this.mHistoryLastWritten.wakelockTag);
                }
                if (this.mHistoryLastWritten.wakeReasonTag != null) {
                    cur.wakeReasonTag = cur.localWakeReasonTag;
                    cur.wakeReasonTag.setTo(this.mHistoryLastWritten.wakeReasonTag);
                }
                if (this.mHistoryLastWritten.eventCode != 0) {
                    cur.eventCode = this.mHistoryLastWritten.eventCode;
                    cur.eventTag = cur.localEventTag;
                    cur.eventTag.setTo(this.mHistoryLastWritten.eventTag);
                }
                this.mHistoryLastWritten.setTo(this.mHistoryLastLastWritten);
            }
            int dataSize = this.mHistoryBuffer.dataSize();
            if (dataSize >= 262144) {
                if (!this.mHistoryOverflow) {
                    this.mHistoryOverflow = true;
                    addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 0, cur);
                    addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 6, cur);
                    return;
                } else {
                    if (this.mHistoryLastWritten.batteryLevel != cur.batteryLevel || (dataSize < 327680 && ((this.mHistoryLastWritten.states ^ cur.states) & BatteryStats.HistoryItem.MOST_INTERESTING_STATES) != 0 && ((this.mHistoryLastWritten.states2 ^ cur.states2) & BatteryStats.HistoryItem.MOST_INTERESTING_STATES2) != 0)) {
                        addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 0, cur);
                        return;
                    }
                    return;
                }
            }
            if (dataSize == 0) {
                cur.currentTime = System.currentTimeMillis();
                this.mLastRecordedClockTime = cur.currentTime;
                this.mLastRecordedClockRealtime = elapsedRealtimeMs;
                addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 7, cur);
            }
            addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 0, cur);
        }
    }

    private void addHistoryBufferLocked(long elapsedRealtimeMs, long uptimeMs, byte cmd, BatteryStats.HistoryItem cur) {
        if (this.mIteratingHistory) {
            throw new IllegalStateException("Can't do this while iterating history!");
        }
        this.mHistoryBufferLastPos = this.mHistoryBuffer.dataPosition();
        this.mHistoryLastLastWritten.setTo(this.mHistoryLastWritten);
        this.mHistoryLastWritten.setTo(this.mHistoryBaseTime + elapsedRealtimeMs, cmd, cur);
        writeHistoryDelta(this.mHistoryBuffer, this.mHistoryLastWritten, this.mHistoryLastLastWritten);
        this.mLastHistoryElapsedRealtime = elapsedRealtimeMs;
        cur.wakelockTag = null;
        cur.wakeReasonTag = null;
        cur.eventCode = 0;
        cur.eventTag = null;
    }

    void addHistoryRecordLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mTrackRunningHistoryElapsedRealtime != 0) {
            long diffElapsed = elapsedRealtimeMs - this.mTrackRunningHistoryElapsedRealtime;
            long diffUptime = uptimeMs - this.mTrackRunningHistoryUptime;
            if (diffUptime < diffElapsed - 20) {
                long wakeElapsedTime = elapsedRealtimeMs - (diffElapsed - diffUptime);
                this.mHistoryAddTmp.setTo(this.mHistoryLastWritten);
                this.mHistoryAddTmp.wakelockTag = null;
                this.mHistoryAddTmp.wakeReasonTag = null;
                this.mHistoryAddTmp.eventCode = 0;
                this.mHistoryAddTmp.states &= Integer.MAX_VALUE;
                addHistoryRecordInnerLocked(wakeElapsedTime, uptimeMs, this.mHistoryAddTmp);
            }
        }
        this.mHistoryCur.states |= Integer.MIN_VALUE;
        this.mTrackRunningHistoryElapsedRealtime = elapsedRealtimeMs;
        this.mTrackRunningHistoryUptime = uptimeMs;
        addHistoryRecordInnerLocked(elapsedRealtimeMs, uptimeMs, this.mHistoryCur);
    }

    void addHistoryRecordInnerLocked(long elapsedRealtimeMs, long uptimeMs, BatteryStats.HistoryItem cur) {
        addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, cur);
    }

    void addHistoryEventLocked(long elapsedRealtimeMs, long uptimeMs, int code, String name, int uid) {
        this.mHistoryCur.eventCode = code;
        this.mHistoryCur.eventTag = this.mHistoryCur.localEventTag;
        this.mHistoryCur.eventTag.string = name;
        this.mHistoryCur.eventTag.uid = uid;
        addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
    }

    void addHistoryRecordLocked(long elapsedRealtimeMs, long uptimeMs, byte cmd, BatteryStats.HistoryItem cur) {
        BatteryStats.HistoryItem rec = this.mHistoryCache;
        if (rec != null) {
            this.mHistoryCache = rec.next;
        } else {
            rec = new BatteryStats.HistoryItem();
        }
        rec.setTo(this.mHistoryBaseTime + elapsedRealtimeMs, cmd, cur);
        addHistoryRecordLocked(rec);
    }

    void addHistoryRecordLocked(BatteryStats.HistoryItem rec) {
        this.mNumHistoryItems++;
        rec.next = null;
        this.mHistoryLastEnd = this.mHistoryEnd;
        if (this.mHistoryEnd != null) {
            this.mHistoryEnd.next = rec;
            this.mHistoryEnd = rec;
        } else {
            this.mHistoryEnd = rec;
            this.mHistory = rec;
        }
    }

    void clearHistoryLocked() {
        this.mHistoryBaseTime = 0L;
        this.mLastHistoryElapsedRealtime = 0L;
        this.mTrackRunningHistoryElapsedRealtime = 0L;
        this.mTrackRunningHistoryUptime = 0L;
        this.mHistoryBuffer.setDataSize(0);
        this.mHistoryBuffer.setDataPosition(0);
        this.mHistoryBuffer.setDataCapacity(131072);
        this.mHistoryLastLastWritten.clear();
        this.mHistoryLastWritten.clear();
        this.mHistoryTagPool.clear();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mHistoryOverflow = false;
        this.mLastRecordedClockTime = 0L;
        this.mLastRecordedClockRealtime = 0L;
    }

    public void updateTimeBasesLocked(boolean unplugged, boolean screenOff, long uptime, long realtime) {
        if (this.mOnBatteryTimeBase.setRunning(unplugged, uptime, realtime)) {
            if (unplugged) {
                this.mBluetoothPingStart = getCurrentBluetoothPingCount();
                this.mBluetoothPingCount = 0;
            } else {
                this.mBluetoothPingCount = getBluetoothPingCount();
                this.mBluetoothPingStart = -1;
            }
        }
        boolean unpluggedScreenOff = unplugged && screenOff;
        if (unpluggedScreenOff != this.mOnBatteryScreenOffTimeBase.isRunning()) {
            updateKernelWakelocksLocked();
            requestWakelockCpuUpdate();
            if (!unpluggedScreenOff) {
                this.mDistributeWakelockCpu = true;
            }
            this.mOnBatteryScreenOffTimeBase.setRunning(unpluggedScreenOff, uptime, realtime);
        }
    }

    public void addIsolatedUidLocked(int isolatedUid, int appUid) {
        this.mIsolatedUids.put(isolatedUid, appUid);
    }

    public void removeIsolatedUidLocked(int isolatedUid, int appUid) {
        int curUid = this.mIsolatedUids.get(isolatedUid, -1);
        if (curUid == appUid) {
            this.mIsolatedUids.delete(isolatedUid);
        }
    }

    public int mapUid(int uid) {
        int isolated = this.mIsolatedUids.get(uid, -1);
        return isolated > 0 ? isolated : uid;
    }

    public void noteEventLocked(int code, String name, int uid) {
        int uid2 = mapUid(uid);
        if (this.mActiveEvents.updateState(code, name, uid2, 0)) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            addHistoryEventLocked(elapsedRealtime, uptime, code, name, uid2);
        }
    }

    public void noteCurrentTimeChangedLocked() {
        long currentTime = System.currentTimeMillis();
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (isStartClockTimeValid() && this.mLastRecordedClockTime != 0) {
            long expectedClockTime = this.mLastRecordedClockTime + (elapsedRealtime - this.mLastRecordedClockRealtime);
            if (currentTime >= expectedClockTime - 500 && currentTime <= expectedClockTime + 500) {
                return;
            }
        }
        recordCurrentTimeChangeLocked(currentTime, elapsedRealtime, uptime);
        if (isStartClockTimeValid()) {
            this.mStartClockTime = currentTime;
        }
    }

    public void noteProcessStartLocked(String name, int uid) {
        int uid2 = mapUid(uid);
        if (isOnBattery()) {
            Uid u = getUidStatsLocked(uid2);
            u.getProcessStatsLocked(name).incStartsLocked();
        }
        if (this.mActiveEvents.updateState(32769, name, uid2, 0) && this.mRecordAllHistory) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            addHistoryEventLocked(elapsedRealtime, uptime, 32769, name, uid2);
        }
    }

    public void noteProcessCrashLocked(String name, int uid) {
        int uid2 = mapUid(uid);
        if (isOnBattery()) {
            Uid u = getUidStatsLocked(uid2);
            u.getProcessStatsLocked(name).incNumCrashesLocked();
        }
    }

    public void noteProcessAnrLocked(String name, int uid) {
        int uid2 = mapUid(uid);
        if (isOnBattery()) {
            Uid u = getUidStatsLocked(uid2);
            u.getProcessStatsLocked(name).incNumAnrsLocked();
        }
    }

    public void noteProcessStateLocked(String name, int uid, int state) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        getUidStatsLocked(uid2).updateProcessStateLocked(name, state, elapsedRealtime);
    }

    public void noteProcessFinishLocked(String name, int uid) {
        int uid2 = mapUid(uid);
        if (this.mActiveEvents.updateState(16385, name, uid2, 0)) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            getUidStatsLocked(uid2).updateProcessStateLocked(name, 3, elapsedRealtime);
            if (this.mRecordAllHistory) {
                addHistoryEventLocked(elapsedRealtime, uptime, 16385, name, uid2);
            }
        }
    }

    public void noteSyncStartLocked(String name, int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        getUidStatsLocked(uid2).noteStartSyncLocked(name, elapsedRealtime);
        if (this.mActiveEvents.updateState(32772, name, uid2, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 32772, name, uid2);
        }
    }

    public void noteSyncFinishLocked(String name, int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        getUidStatsLocked(uid2).noteStopSyncLocked(name, elapsedRealtime);
        if (this.mActiveEvents.updateState(16388, name, uid2, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 16388, name, uid2);
        }
    }

    public void noteJobStartLocked(String name, int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        getUidStatsLocked(uid2).noteStartJobLocked(name, elapsedRealtime);
        if (this.mActiveEvents.updateState(32774, name, uid2, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 32774, name, uid2);
        }
    }

    public void noteJobFinishLocked(String name, int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        getUidStatsLocked(uid2).noteStopJobLocked(name, elapsedRealtime);
        if (this.mActiveEvents.updateState(16390, name, uid2, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 16390, name, uid2);
        }
    }

    private void requestWakelockCpuUpdate() {
        if (!this.mHandler.hasMessages(1)) {
            Message m = this.mHandler.obtainMessage(1);
            this.mHandler.sendMessageDelayed(m, 5000L);
        }
    }

    public void setRecordAllHistoryLocked(boolean enabled) {
        this.mRecordAllHistory = enabled;
        if (!enabled) {
            this.mActiveEvents.removeEvents(5);
            HashMap<String, SparseIntArray> active = this.mActiveEvents.getStateForEvent(1);
            if (active != null) {
                long mSecRealtime = SystemClock.elapsedRealtime();
                long mSecUptime = SystemClock.uptimeMillis();
                for (Map.Entry<String, SparseIntArray> ent : active.entrySet()) {
                    SparseIntArray uids = ent.getValue();
                    for (int j = 0; j < uids.size(); j++) {
                        addHistoryEventLocked(mSecRealtime, mSecUptime, 16385, ent.getKey(), uids.keyAt(j));
                    }
                }
                return;
            }
            return;
        }
        HashMap<String, SparseIntArray> active2 = this.mActiveEvents.getStateForEvent(1);
        if (active2 != null) {
            long mSecRealtime2 = SystemClock.elapsedRealtime();
            long mSecUptime2 = SystemClock.uptimeMillis();
            for (Map.Entry<String, SparseIntArray> ent2 : active2.entrySet()) {
                SparseIntArray uids2 = ent2.getValue();
                for (int j2 = 0; j2 < uids2.size(); j2++) {
                    addHistoryEventLocked(mSecRealtime2, mSecUptime2, 32769, ent2.getKey(), uids2.keyAt(j2));
                }
            }
        }
    }

    public void setNoAutoReset(boolean enabled) {
        this.mNoAutoReset = enabled;
    }

    public void noteStartWakeLocked(int uid, int pid, String name, String historyName, int type, boolean unimportantForLogging, long elapsedRealtime, long uptime) {
        int uid2 = mapUid(uid);
        if (type == 0) {
            aggregateLastWakeupUptimeLocked(uptime);
            if (historyName == null) {
                historyName = name;
            }
            if (this.mRecordAllHistory && this.mActiveEvents.updateState(32773, historyName, uid2, 0)) {
                addHistoryEventLocked(elapsedRealtime, uptime, 32773, historyName, uid2);
            }
            if (this.mWakeLockNesting == 0) {
                this.mHistoryCur.states |= 1073741824;
                this.mHistoryCur.wakelockTag = this.mHistoryCur.localWakelockTag;
                BatteryStats.HistoryTag historyTag = this.mHistoryCur.wakelockTag;
                this.mInitialAcquireWakeName = historyName;
                historyTag.string = historyName;
                BatteryStats.HistoryTag historyTag2 = this.mHistoryCur.wakelockTag;
                this.mInitialAcquireWakeUid = uid2;
                historyTag2.uid = uid2;
                this.mWakeLockImportant = !unimportantForLogging;
                addHistoryRecordLocked(elapsedRealtime, uptime);
            } else if (!this.mWakeLockImportant && !unimportantForLogging && this.mHistoryLastWritten.cmd == 0) {
                if (this.mHistoryLastWritten.wakelockTag != null) {
                    this.mHistoryLastWritten.wakelockTag = null;
                    this.mHistoryCur.wakelockTag = this.mHistoryCur.localWakelockTag;
                    BatteryStats.HistoryTag historyTag3 = this.mHistoryCur.wakelockTag;
                    this.mInitialAcquireWakeName = historyName;
                    historyTag3.string = historyName;
                    BatteryStats.HistoryTag historyTag4 = this.mHistoryCur.wakelockTag;
                    this.mInitialAcquireWakeUid = uid2;
                    historyTag4.uid = uid2;
                    addHistoryRecordLocked(elapsedRealtime, uptime);
                }
                this.mWakeLockImportant = true;
            }
            this.mWakeLockNesting++;
        }
        if (uid2 >= 0) {
            requestWakelockCpuUpdate();
            getUidStatsLocked(uid2).noteStartWakeLocked(pid, name, type, elapsedRealtime);
        }
    }

    public void noteStopWakeLocked(int uid, int pid, String name, String historyName, int type, long elapsedRealtime, long uptime) {
        int uid2 = mapUid(uid);
        if (type == 0) {
            this.mWakeLockNesting--;
            if (this.mRecordAllHistory) {
                if (historyName == null) {
                    historyName = name;
                }
                if (this.mActiveEvents.updateState(16389, historyName, uid2, 0)) {
                    addHistoryEventLocked(elapsedRealtime, uptime, 16389, historyName, uid2);
                }
            }
            if (this.mWakeLockNesting == 0) {
                this.mHistoryCur.states &= -1073741825;
                this.mInitialAcquireWakeName = null;
                this.mInitialAcquireWakeUid = -1;
                addHistoryRecordLocked(elapsedRealtime, uptime);
            }
        }
        if (uid2 >= 0) {
            requestWakelockCpuUpdate();
            getUidStatsLocked(uid2).noteStopWakeLocked(pid, name, type, elapsedRealtime);
        }
    }

    public void noteStartWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, boolean unimportantForLogging) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteStartWakeLocked(ws.get(i), pid, name, historyName, type, unimportantForLogging, elapsedRealtime, uptime);
        }
    }

    public void noteChangeWakelockFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, WorkSource newWs, int newPid, String newName, String newHistoryName, int newType, boolean newUnimportantForLogging) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        int NN = newWs.size();
        for (int i = 0; i < NN; i++) {
            noteStartWakeLocked(newWs.get(i), newPid, newName, newHistoryName, newType, newUnimportantForLogging, elapsedRealtime, uptime);
        }
        int NO = ws.size();
        for (int i2 = 0; i2 < NO; i2++) {
            noteStopWakeLocked(ws.get(i2), pid, name, historyName, type, elapsedRealtime, uptime);
        }
    }

    public void noteStopWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteStopWakeLocked(ws.get(i), pid, name, historyName, type, elapsedRealtime, uptime);
        }
    }

    void aggregateLastWakeupUptimeLocked(long uptimeMs) {
        if (this.mLastWakeupReason != null) {
            long deltaUptime = uptimeMs - this.mLastWakeupUptimeMs;
            SamplingTimer timer = getWakeupReasonTimerLocked(this.mLastWakeupReason);
            timer.addCurrentReportedCount(1);
            timer.addCurrentReportedTotalTime(1000 * deltaUptime);
            this.mLastWakeupReason = null;
        }
    }

    public void noteWakeupReasonLocked(String reason) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        aggregateLastWakeupUptimeLocked(uptime);
        this.mHistoryCur.wakeReasonTag = this.mHistoryCur.localWakeReasonTag;
        this.mHistoryCur.wakeReasonTag.string = reason;
        this.mHistoryCur.wakeReasonTag.uid = 0;
        this.mLastWakeupReason = reason;
        this.mLastWakeupUptimeMs = uptime;
        addHistoryRecordLocked(elapsedRealtime, uptime);
    }

    public int startAddingCpuLocked() {
        Uid uid;
        this.mHandler.removeMessages(1);
        int N = this.mPartialTimers.size();
        if (N == 0) {
            this.mLastPartialTimers.clear();
            this.mDistributeWakelockCpu = false;
            return 0;
        }
        if (!this.mOnBatteryScreenOffTimeBase.isRunning() && !this.mDistributeWakelockCpu) {
            return 0;
        }
        this.mDistributeWakelockCpu = false;
        for (int i = 0; i < N; i++) {
            StopwatchTimer st = this.mPartialTimers.get(i);
            if (st.mInList && (uid = st.mUid) != null && uid.mUid != 1000) {
                return 50;
            }
        }
        return 0;
    }

    public void finishAddingCpuLocked(int perc, int utime, int stime, long[] cpuSpeedTimes) {
        Uid uid;
        Uid uid2;
        Uid uid3;
        int N = this.mPartialTimers.size();
        if (perc != 0) {
            int num = 0;
            for (int i = 0; i < N; i++) {
                StopwatchTimer st = this.mPartialTimers.get(i);
                if (st.mInList && (uid3 = st.mUid) != null && uid3.mUid != 1000) {
                    num++;
                }
            }
            if (num != 0) {
                for (int i2 = 0; i2 < N; i2++) {
                    StopwatchTimer st2 = this.mPartialTimers.get(i2);
                    if (st2.mInList && (uid2 = st2.mUid) != null && uid2.mUid != 1000) {
                        int myUTime = utime / num;
                        int mySTime = stime / num;
                        utime -= myUTime;
                        stime -= mySTime;
                        num--;
                        Uid.Proc proc = uid2.getProcessStatsLocked("*wakelock*");
                        proc.addCpuTimeLocked(myUTime, mySTime);
                        proc.addSpeedStepTimes(cpuSpeedTimes);
                    }
                }
            }
            if ((utime != 0 || stime != 0) && (uid = getUidStatsLocked(1000)) != null) {
                Uid.Proc proc2 = uid.getProcessStatsLocked("*lost*");
                proc2.addCpuTimeLocked(utime, stime);
                proc2.addSpeedStepTimes(cpuSpeedTimes);
            }
        }
        int NL = this.mLastPartialTimers.size();
        boolean diff = N != NL;
        for (int i3 = 0; i3 < NL && !diff; i3++) {
            diff |= this.mPartialTimers.get(i3) != this.mLastPartialTimers.get(i3);
        }
        if (!diff) {
            for (int i4 = 0; i4 < NL; i4++) {
                this.mPartialTimers.get(i4).mInList = true;
            }
            return;
        }
        for (int i5 = 0; i5 < NL; i5++) {
            this.mLastPartialTimers.get(i5).mInList = false;
        }
        this.mLastPartialTimers.clear();
        for (int i6 = 0; i6 < N; i6++) {
            StopwatchTimer st3 = this.mPartialTimers.get(i6);
            st3.mInList = true;
            this.mLastPartialTimers.add(st3);
        }
    }

    public void noteProcessDiedLocked(int uid, int pid) {
        Uid u = this.mUidStats.get(mapUid(uid));
        if (u != null) {
            u.mPids.remove(pid);
        }
    }

    public long getProcessWakeTime(int uid, int pid, long realtime) {
        BatteryStats.Uid.Pid p;
        Uid u = this.mUidStats.get(mapUid(uid));
        if (u == null || (p = u.mPids.get(pid)) == null) {
            return 0L;
        }
        return (p.mWakeNesting > 0 ? realtime - p.mWakeStartMs : 0L) + p.mWakeSumMs;
    }

    public void reportExcessiveWakeLocked(int uid, String proc, long overTime, long usedTime) {
        Uid u = this.mUidStats.get(mapUid(uid));
        if (u != null) {
            u.reportExcessiveWakeLocked(proc, overTime, usedTime);
        }
    }

    public void reportExcessiveCpuLocked(int uid, String proc, long overTime, long usedTime) {
        Uid u = this.mUidStats.get(mapUid(uid));
        if (u != null) {
            u.reportExcessiveCpuLocked(proc, overTime, usedTime);
        }
    }

    public void noteStartSensorLocked(int uid, int sensor) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mSensorNesting == 0) {
            this.mHistoryCur.states |= 8388608;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mSensorNesting++;
        getUidStatsLocked(uid2).noteStartSensor(sensor, elapsedRealtime);
    }

    public void noteStopSensorLocked(int uid, int sensor) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        this.mSensorNesting--;
        if (this.mSensorNesting == 0) {
            this.mHistoryCur.states &= -8388609;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid2).noteStopSensor(sensor, elapsedRealtime);
    }

    public void noteStartGpsLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mGpsNesting == 0) {
            this.mHistoryCur.states |= 536870912;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mGpsNesting++;
        getUidStatsLocked(uid2).noteStartGps(elapsedRealtime);
    }

    public void noteStopGpsLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        this.mGpsNesting--;
        if (this.mGpsNesting == 0) {
            this.mHistoryCur.states &= -536870913;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid2).noteStopGps(elapsedRealtime);
    }

    public void noteScreenStateLocked(int state) {
        if (this.mScreenState != state) {
            int oldState = this.mScreenState;
            this.mScreenState = state;
            if (state != 0) {
                int stepState = state - 1;
                if (stepState < 4) {
                    this.mModStepMode |= (this.mCurStepMode & 3) ^ stepState;
                    this.mCurStepMode = (this.mCurStepMode & (-4)) | stepState;
                } else {
                    Slog.wtf(TAG, "Unexpected screen state: " + state);
                }
            }
            if (state == 2) {
                long elapsedRealtime = SystemClock.elapsedRealtime();
                long uptime = SystemClock.uptimeMillis();
                this.mHistoryCur.states |= 1048576;
                addHistoryRecordLocked(elapsedRealtime, uptime);
                this.mScreenOnTimer.startRunningLocked(elapsedRealtime);
                if (this.mScreenBrightnessBin >= 0) {
                    this.mScreenBrightnessTimer[this.mScreenBrightnessBin].startRunningLocked(elapsedRealtime);
                }
                updateTimeBasesLocked(this.mOnBatteryTimeBase.isRunning(), false, SystemClock.uptimeMillis() * 1000, 1000 * elapsedRealtime);
                noteStartWakeLocked(-1, -1, PowerHintManager.HINT_SCREEN, null, 0, false, elapsedRealtime, uptime);
                if (this.mOnBatteryInternal) {
                    updateDischargeScreenLevelsLocked(false, true);
                    return;
                }
                return;
            }
            if (oldState == 2) {
                long elapsedRealtime2 = SystemClock.elapsedRealtime();
                long uptime2 = SystemClock.uptimeMillis();
                this.mHistoryCur.states &= -1048577;
                addHistoryRecordLocked(elapsedRealtime2, uptime2);
                this.mScreenOnTimer.stopRunningLocked(elapsedRealtime2);
                if (this.mScreenBrightnessBin >= 0) {
                    this.mScreenBrightnessTimer[this.mScreenBrightnessBin].stopRunningLocked(elapsedRealtime2);
                }
                noteStopWakeLocked(-1, -1, PowerHintManager.HINT_SCREEN, PowerHintManager.HINT_SCREEN, 0, elapsedRealtime2, uptime2);
                updateTimeBasesLocked(this.mOnBatteryTimeBase.isRunning(), true, SystemClock.uptimeMillis() * 1000, 1000 * elapsedRealtime2);
                if (this.mOnBatteryInternal) {
                    updateDischargeScreenLevelsLocked(true, false);
                }
            }
        }
    }

    public void noteScreenBrightnessLocked(int brightness) {
        int bin = brightness / 51;
        if (bin < 0) {
            bin = 0;
        } else if (bin >= 5) {
            bin = 4;
        }
        if (this.mScreenBrightnessBin != bin) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states = (this.mHistoryCur.states & (-8)) | (bin << 0);
            addHistoryRecordLocked(elapsedRealtime, uptime);
            if (this.mScreenState == 2) {
                if (this.mScreenBrightnessBin >= 0) {
                    this.mScreenBrightnessTimer[this.mScreenBrightnessBin].stopRunningLocked(elapsedRealtime);
                }
                this.mScreenBrightnessTimer[bin].startRunningLocked(elapsedRealtime);
            }
            this.mScreenBrightnessBin = bin;
        }
    }

    public void noteUserActivityLocked(int uid, int event) {
        if (this.mOnBatteryInternal) {
            getUidStatsLocked(mapUid(uid)).noteUserActivityLocked(event);
        }
    }

    public void noteInteractiveLocked(boolean interactive) {
        if (this.mInteractive != interactive) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mInteractive = interactive;
            if (interactive) {
                this.mInteractiveTimer.startRunningLocked(elapsedRealtime);
            } else {
                this.mInteractiveTimer.stopRunningLocked(elapsedRealtime);
            }
        }
    }

    public void noteConnectivityChangedLocked(int type, String extra) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        addHistoryEventLocked(elapsedRealtime, uptime, 9, extra, type);
        this.mNumConnectivityChange++;
    }

    public void noteMobileRadioPowerState(int powerState, long timestampNs) {
        long realElapsedRealtimeMs;
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mMobileRadioPowerState != powerState) {
            boolean active = powerState == DataConnectionRealTimeInfo.DC_POWER_STATE_MEDIUM || powerState == DataConnectionRealTimeInfo.DC_POWER_STATE_HIGH;
            if (active) {
                realElapsedRealtimeMs = elapsedRealtime;
                this.mMobileRadioActiveStartTime = elapsedRealtime;
                this.mHistoryCur.states |= 33554432;
            } else {
                realElapsedRealtimeMs = timestampNs / TimeUtils.NANOS_PER_MS;
                long lastUpdateTimeMs = this.mMobileRadioActiveStartTime;
                if (realElapsedRealtimeMs < lastUpdateTimeMs) {
                    Slog.wtf(TAG, "Data connection inactive timestamp " + realElapsedRealtimeMs + " is before start time " + lastUpdateTimeMs);
                    realElapsedRealtimeMs = elapsedRealtime;
                } else if (realElapsedRealtimeMs < elapsedRealtime) {
                    this.mMobileRadioActiveAdjustedTime.addCountLocked(elapsedRealtime - realElapsedRealtimeMs);
                }
                this.mHistoryCur.states &= -33554433;
            }
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mMobileRadioPowerState = powerState;
            if (active) {
                this.mMobileRadioActiveTimer.startRunningLocked(elapsedRealtime);
                this.mMobileRadioActivePerAppTimer.startRunningLocked(elapsedRealtime);
            } else {
                this.mMobileRadioActiveTimer.stopRunningLocked(realElapsedRealtimeMs);
                updateNetworkActivityLocked(1, realElapsedRealtimeMs);
                this.mMobileRadioActivePerAppTimer.stopRunningLocked(realElapsedRealtimeMs);
            }
        }
    }

    public void noteLowPowerMode(boolean enabled) {
        if (this.mLowPowerModeEnabled != enabled) {
            int stepState = enabled ? 4 : 0;
            this.mModStepMode |= (this.mCurStepMode & 4) ^ stepState;
            this.mCurStepMode = (this.mCurStepMode & (-5)) | stepState;
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mLowPowerModeEnabled = enabled;
            if (enabled) {
                this.mHistoryCur.states2 |= Integer.MIN_VALUE;
                this.mLowPowerModeEnabledTimer.startRunningLocked(elapsedRealtime);
            } else {
                this.mHistoryCur.states2 &= Integer.MAX_VALUE;
                this.mLowPowerModeEnabledTimer.stopRunningLocked(elapsedRealtime);
            }
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
    }

    public void notePhoneOnLocked() {
        if (!this.mPhoneOn) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states |= 262144;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mPhoneOn = true;
            this.mPhoneOnTimer.startRunningLocked(elapsedRealtime);
        }
    }

    public void notePhoneOffLocked() {
        if (this.mPhoneOn) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states &= -262145;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mPhoneOn = false;
            this.mPhoneOnTimer.stopRunningLocked(elapsedRealtime);
        }
    }

    void stopAllPhoneSignalStrengthTimersLocked(int except) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        for (int i = 0; i < 5; i++) {
            if (i != except) {
                while (this.mPhoneSignalStrengthsTimer[i].isRunningLocked()) {
                    this.mPhoneSignalStrengthsTimer[i].stopRunningLocked(elapsedRealtime);
                }
            }
        }
    }

    private int fixPhoneServiceState(int state, int signalBin) {
        if (this.mPhoneSimStateRaw == 1 && state == 1 && signalBin > 0) {
            return 0;
        }
        return state;
    }

    private void updateAllPhoneStateLocked(int state, int simState, int strengthBin) {
        boolean scanning = false;
        boolean newHistory = false;
        this.mPhoneServiceStateRaw = state;
        this.mPhoneSimStateRaw = simState;
        this.mPhoneSignalStrengthBinRaw = strengthBin;
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (simState == 1 && state == 1 && strengthBin > 0) {
            state = 0;
        }
        if (state == 3) {
            strengthBin = -1;
        } else if (state != 0 && state == 1) {
            scanning = true;
            strengthBin = 0;
            if (!this.mPhoneSignalScanningTimer.isRunningLocked()) {
                this.mHistoryCur.states |= 2097152;
                newHistory = true;
                this.mPhoneSignalScanningTimer.startRunningLocked(elapsedRealtime);
            }
        }
        if (!scanning && this.mPhoneSignalScanningTimer.isRunningLocked()) {
            this.mHistoryCur.states &= -2097153;
            newHistory = true;
            this.mPhoneSignalScanningTimer.stopRunningLocked(elapsedRealtime);
        }
        if (this.mPhoneServiceState != state) {
            this.mHistoryCur.states = (this.mHistoryCur.states & (-449)) | (state << 6);
            newHistory = true;
            this.mPhoneServiceState = state;
        }
        if (this.mPhoneSignalStrengthBin != strengthBin) {
            if (this.mPhoneSignalStrengthBin >= 0) {
                this.mPhoneSignalStrengthsTimer[this.mPhoneSignalStrengthBin].stopRunningLocked(elapsedRealtime);
            }
            if (strengthBin >= 0) {
                if (!this.mPhoneSignalStrengthsTimer[strengthBin].isRunningLocked()) {
                    this.mPhoneSignalStrengthsTimer[strengthBin].startRunningLocked(elapsedRealtime);
                }
                this.mHistoryCur.states = (this.mHistoryCur.states & (-57)) | (strengthBin << 3);
                newHistory = true;
            } else {
                stopAllPhoneSignalStrengthTimersLocked(-1);
            }
            this.mPhoneSignalStrengthBin = strengthBin;
        }
        if (newHistory) {
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
    }

    public void notePhoneStateLocked(int state, int simState) {
        updateAllPhoneStateLocked(state, simState, this.mPhoneSignalStrengthBinRaw);
    }

    public void notePhoneSignalStrengthLocked(SignalStrength signalStrength) {
        int bin = signalStrength.getLevel();
        updateAllPhoneStateLocked(this.mPhoneServiceStateRaw, this.mPhoneSimStateRaw, bin);
    }

    public void notePhoneDataConnectionStateLocked(int dataType, boolean hasData) {
        int bin = 0;
        if (hasData) {
            switch (dataType) {
                case 1:
                    bin = 1;
                    break;
                case 2:
                    bin = 2;
                    break;
                case 3:
                    bin = 3;
                    break;
                case 4:
                    bin = 4;
                    break;
                case 5:
                    bin = 5;
                    break;
                case 6:
                    bin = 6;
                    break;
                case 7:
                    bin = 7;
                    break;
                case 8:
                    bin = 8;
                    break;
                case 9:
                    bin = 9;
                    break;
                case 10:
                    bin = 10;
                    break;
                case 11:
                    bin = 11;
                    break;
                case 12:
                    bin = 12;
                    break;
                case 13:
                    bin = 13;
                    break;
                case 14:
                    bin = 14;
                    break;
                case 15:
                    bin = 15;
                    break;
                default:
                    bin = 16;
                    break;
            }
        }
        if (this.mPhoneDataConnectionType != bin) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states = (this.mHistoryCur.states & (-15873)) | (bin << 9);
            addHistoryRecordLocked(elapsedRealtime, uptime);
            if (this.mPhoneDataConnectionType >= 0) {
                this.mPhoneDataConnectionsTimer[this.mPhoneDataConnectionType].stopRunningLocked(elapsedRealtime);
            }
            this.mPhoneDataConnectionType = bin;
            this.mPhoneDataConnectionsTimer[bin].startRunningLocked(elapsedRealtime);
        }
    }

    public void noteWifiOnLocked() {
        if (!this.mWifiOn) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states2 |= 268435456;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mWifiOn = true;
            this.mWifiOnTimer.startRunningLocked(elapsedRealtime);
        }
    }

    public void noteWifiOffLocked() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mWifiOn) {
            this.mHistoryCur.states2 &= -268435457;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mWifiOn = false;
            this.mWifiOnTimer.stopRunningLocked(elapsedRealtime);
        }
    }

    public void noteAudioOnLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mAudioOnNesting == 0) {
            this.mHistoryCur.states |= 4194304;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mAudioOnTimer.startRunningLocked(elapsedRealtime);
        }
        this.mAudioOnNesting++;
        getUidStatsLocked(uid2).noteAudioTurnedOnLocked(elapsedRealtime);
    }

    public void noteAudioOffLocked(int uid) {
        if (this.mAudioOnNesting != 0) {
            int uid2 = mapUid(uid);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            int i = this.mAudioOnNesting - 1;
            this.mAudioOnNesting = i;
            if (i == 0) {
                this.mHistoryCur.states &= -4194305;
                addHistoryRecordLocked(elapsedRealtime, uptime);
                this.mAudioOnTimer.stopRunningLocked(elapsedRealtime);
            }
            getUidStatsLocked(uid2).noteAudioTurnedOffLocked(elapsedRealtime);
        }
    }

    public void noteVideoOnLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mVideoOnNesting == 0) {
            this.mHistoryCur.states2 |= 1073741824;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mVideoOnTimer.startRunningLocked(elapsedRealtime);
        }
        this.mVideoOnNesting++;
        getUidStatsLocked(uid2).noteVideoTurnedOnLocked(elapsedRealtime);
    }

    public void noteVideoOffLocked(int uid) {
        if (this.mVideoOnNesting != 0) {
            int uid2 = mapUid(uid);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            int i = this.mVideoOnNesting - 1;
            this.mVideoOnNesting = i;
            if (i == 0) {
                this.mHistoryCur.states2 &= -1073741825;
                addHistoryRecordLocked(elapsedRealtime, uptime);
                this.mVideoOnTimer.stopRunningLocked(elapsedRealtime);
            }
            getUidStatsLocked(uid2).noteVideoTurnedOffLocked(elapsedRealtime);
        }
    }

    public void noteResetAudioLocked() {
        if (this.mAudioOnNesting > 0) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mAudioOnNesting = 0;
            this.mHistoryCur.states &= -4194305;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mAudioOnTimer.stopAllRunningLocked(elapsedRealtime);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                Uid uid = this.mUidStats.valueAt(i);
                uid.noteResetAudioLocked(elapsedRealtime);
            }
        }
    }

    public void noteResetVideoLocked() {
        if (this.mVideoOnNesting > 0) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mAudioOnNesting = 0;
            this.mHistoryCur.states2 &= -1073741825;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mVideoOnTimer.stopAllRunningLocked(elapsedRealtime);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                Uid uid = this.mUidStats.valueAt(i);
                uid.noteResetVideoLocked(elapsedRealtime);
            }
        }
    }

    public void noteActivityResumedLocked(int uid) {
        getUidStatsLocked(mapUid(uid)).noteActivityResumedLocked(SystemClock.elapsedRealtime());
    }

    public void noteActivityPausedLocked(int uid) {
        getUidStatsLocked(mapUid(uid)).noteActivityPausedLocked(SystemClock.elapsedRealtime());
    }

    public void noteVibratorOnLocked(int uid, long durationMillis) {
        getUidStatsLocked(mapUid(uid)).noteVibratorOnLocked(durationMillis);
    }

    public void noteVibratorOffLocked(int uid) {
        getUidStatsLocked(mapUid(uid)).noteVibratorOffLocked();
    }

    public void noteFlashlightOnLocked() {
        if (!this.mFlashlightOn) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states2 |= 134217728;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mFlashlightOn = true;
            this.mFlashlightOnTimer.startRunningLocked(elapsedRealtime);
        }
    }

    public void noteFlashlightOffLocked() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mFlashlightOn) {
            this.mHistoryCur.states2 &= -134217729;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mFlashlightOn = false;
            this.mFlashlightOnTimer.stopRunningLocked(elapsedRealtime);
        }
    }

    public void noteWifiRunningLocked(WorkSource ws) {
        if (!this.mGlobalWifiRunning) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states2 |= 536870912;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mGlobalWifiRunning = true;
            this.mGlobalWifiRunningTimer.startRunningLocked(elapsedRealtime);
            int N = ws.size();
            for (int i = 0; i < N; i++) {
                int uid = mapUid(ws.get(i));
                getUidStatsLocked(uid).noteWifiRunningLocked(elapsedRealtime);
            }
            return;
        }
        Log.w(TAG, "noteWifiRunningLocked -- called while WIFI running");
    }

    public void noteWifiRunningChangedLocked(WorkSource oldWs, WorkSource newWs) {
        if (this.mGlobalWifiRunning) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            int N = oldWs.size();
            for (int i = 0; i < N; i++) {
                int uid = mapUid(oldWs.get(i));
                getUidStatsLocked(uid).noteWifiStoppedLocked(elapsedRealtime);
            }
            int N2 = newWs.size();
            for (int i2 = 0; i2 < N2; i2++) {
                int uid2 = mapUid(newWs.get(i2));
                getUidStatsLocked(uid2).noteWifiRunningLocked(elapsedRealtime);
            }
            return;
        }
        Log.w(TAG, "noteWifiRunningChangedLocked -- called while WIFI not running");
    }

    public void noteWifiStoppedLocked(WorkSource ws) {
        if (this.mGlobalWifiRunning) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states2 &= -536870913;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mGlobalWifiRunning = false;
            this.mGlobalWifiRunningTimer.stopRunningLocked(elapsedRealtime);
            int N = ws.size();
            for (int i = 0; i < N; i++) {
                int uid = mapUid(ws.get(i));
                getUidStatsLocked(uid).noteWifiStoppedLocked(elapsedRealtime);
            }
            return;
        }
        Log.w(TAG, "noteWifiStoppedLocked -- called while WIFI not running");
    }

    public void noteWifiStateLocked(int wifiState, String accessPoint) {
        if (this.mWifiState != wifiState) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            if (this.mWifiState >= 0) {
                this.mWifiStateTimer[this.mWifiState].stopRunningLocked(elapsedRealtime);
            }
            this.mWifiState = wifiState;
            this.mWifiStateTimer[wifiState].startRunningLocked(elapsedRealtime);
        }
    }

    public void noteWifiSupplicantStateChangedLocked(int supplState, boolean failedAuth) {
        if (this.mWifiSupplState != supplState) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            if (this.mWifiSupplState >= 0) {
                this.mWifiSupplStateTimer[this.mWifiSupplState].stopRunningLocked(elapsedRealtime);
            }
            this.mWifiSupplState = supplState;
            this.mWifiSupplStateTimer[supplState].startRunningLocked(elapsedRealtime);
            this.mHistoryCur.states2 = (this.mHistoryCur.states2 & (-16)) | (supplState << 0);
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
    }

    void stopAllWifiSignalStrengthTimersLocked(int except) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        for (int i = 0; i < 5; i++) {
            if (i != except) {
                while (this.mWifiSignalStrengthsTimer[i].isRunningLocked()) {
                    this.mWifiSignalStrengthsTimer[i].stopRunningLocked(elapsedRealtime);
                }
            }
        }
    }

    public void noteWifiRssiChangedLocked(int newRssi) {
        int strengthBin = WifiManager.calculateSignalLevel(newRssi, 5);
        if (this.mWifiSignalStrengthBin != strengthBin) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            if (this.mWifiSignalStrengthBin >= 0) {
                this.mWifiSignalStrengthsTimer[this.mWifiSignalStrengthBin].stopRunningLocked(elapsedRealtime);
            }
            if (strengthBin >= 0) {
                if (!this.mWifiSignalStrengthsTimer[strengthBin].isRunningLocked()) {
                    this.mWifiSignalStrengthsTimer[strengthBin].startRunningLocked(elapsedRealtime);
                }
                this.mHistoryCur.states2 = (this.mHistoryCur.states2 & PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS) | (strengthBin << 4);
                addHistoryRecordLocked(elapsedRealtime, uptime);
            } else {
                stopAllWifiSignalStrengthTimersLocked(-1);
            }
            this.mWifiSignalStrengthBin = strengthBin;
        }
    }

    public void noteBluetoothOnLocked() {
        if (!this.mBluetoothOn) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states |= 65536;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mBluetoothOn = true;
            this.mBluetoothOnTimer.startRunningLocked(elapsedRealtime);
        }
    }

    public void noteBluetoothOffLocked() {
        if (this.mBluetoothOn) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            this.mHistoryCur.states &= -65537;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mBluetoothOn = false;
            this.mBluetoothOnTimer.stopRunningLocked(elapsedRealtime);
        }
    }

    public void noteBluetoothStateLocked(int bluetoothState) {
        if (this.mBluetoothState != bluetoothState) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            if (this.mBluetoothState >= 0) {
                this.mBluetoothStateTimer[this.mBluetoothState].stopRunningLocked(elapsedRealtime);
            }
            this.mBluetoothState = bluetoothState;
            this.mBluetoothStateTimer[bluetoothState].startRunningLocked(elapsedRealtime);
        }
    }

    public void noteFullWifiLockAcquiredLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mWifiFullLockNesting == 0) {
            this.mHistoryCur.states |= 268435456;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mWifiFullLockNesting++;
        getUidStatsLocked(uid2).noteFullWifiLockAcquiredLocked(elapsedRealtime);
    }

    public void noteFullWifiLockReleasedLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        this.mWifiFullLockNesting--;
        if (this.mWifiFullLockNesting == 0) {
            this.mHistoryCur.states &= -268435457;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid2).noteFullWifiLockReleasedLocked(elapsedRealtime);
    }

    public void noteWifiScanStartedLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mWifiScanNesting == 0) {
            this.mHistoryCur.states |= 134217728;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mWifiScanNesting++;
        getUidStatsLocked(uid2).noteWifiScanStartedLocked(elapsedRealtime);
    }

    public void noteWifiScanStoppedLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        this.mWifiScanNesting--;
        if (this.mWifiScanNesting == 0) {
            this.mHistoryCur.states &= -134217729;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid2).noteWifiScanStoppedLocked(elapsedRealtime);
    }

    public void noteWifiBatchedScanStartedLocked(int uid, int csph) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        getUidStatsLocked(uid2).noteWifiBatchedScanStartedLocked(csph, elapsedRealtime);
    }

    public void noteWifiBatchedScanStoppedLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        getUidStatsLocked(uid2).noteWifiBatchedScanStoppedLocked(elapsedRealtime);
    }

    public void noteWifiMulticastEnabledLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (this.mWifiMulticastNesting == 0) {
            this.mHistoryCur.states |= 67108864;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mWifiMulticastNesting++;
        getUidStatsLocked(uid2).noteWifiMulticastEnabledLocked(elapsedRealtime);
    }

    public void noteWifiMulticastDisabledLocked(int uid) {
        int uid2 = mapUid(uid);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        this.mWifiMulticastNesting--;
        if (this.mWifiMulticastNesting == 0) {
            this.mHistoryCur.states &= -67108865;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid2).noteWifiMulticastDisabledLocked(elapsedRealtime);
    }

    public void noteFullWifiLockAcquiredFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteFullWifiLockAcquiredLocked(ws.get(i));
        }
    }

    public void noteFullWifiLockReleasedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteFullWifiLockReleasedLocked(ws.get(i));
        }
    }

    public void noteWifiScanStartedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiScanStartedLocked(ws.get(i));
        }
    }

    public void noteWifiScanStoppedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiScanStoppedLocked(ws.get(i));
        }
    }

    public void noteWifiBatchedScanStartedFromSourceLocked(WorkSource ws, int csph) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiBatchedScanStartedLocked(ws.get(i), csph);
        }
    }

    public void noteWifiBatchedScanStoppedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiBatchedScanStoppedLocked(ws.get(i));
        }
    }

    public void noteWifiMulticastEnabledFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiMulticastEnabledLocked(ws.get(i));
        }
    }

    public void noteWifiMulticastDisabledFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiMulticastDisabledLocked(ws.get(i));
        }
    }

    private static String[] includeInStringArray(String[] array, String str) {
        if (ArrayUtils.indexOf(array, str) < 0) {
            String[] newArray = new String[array.length + 1];
            System.arraycopy(array, 0, newArray, 0, array.length);
            newArray[array.length] = str;
            return newArray;
        }
        return array;
    }

    private static String[] excludeFromStringArray(String[] array, String str) {
        int index = ArrayUtils.indexOf(array, str);
        if (index < 0) {
            return array;
        }
        String[] newArray = new String[array.length - 1];
        if (index > 0) {
            System.arraycopy(array, 0, newArray, 0, index);
        }
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, newArray, index, (array.length - index) - 1);
            return newArray;
        }
        return newArray;
    }

    public void noteNetworkInterfaceTypeLocked(String iface, int networkType) {
        if (!TextUtils.isEmpty(iface)) {
            if (ConnectivityManager.isNetworkTypeMobile(networkType)) {
                this.mMobileIfaces = includeInStringArray(this.mMobileIfaces, iface);
            } else {
                this.mMobileIfaces = excludeFromStringArray(this.mMobileIfaces, iface);
            }
            if (ConnectivityManager.isNetworkTypeWifi(networkType)) {
                this.mWifiIfaces = includeInStringArray(this.mWifiIfaces, iface);
            } else {
                this.mWifiIfaces = excludeFromStringArray(this.mWifiIfaces, iface);
            }
        }
    }

    public void noteNetworkStatsEnabledLocked() {
        updateNetworkActivityLocked(65535, SystemClock.elapsedRealtime());
    }

    @Override
    public long getScreenOnTime(long elapsedRealtimeUs, int which) {
        return this.mScreenOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getScreenOnCount(int which) {
        return this.mScreenOnTimer.getCountLocked(which);
    }

    @Override
    public long getScreenBrightnessTime(int brightnessBin, long elapsedRealtimeUs, int which) {
        return this.mScreenBrightnessTimer[brightnessBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public long getInteractiveTime(long elapsedRealtimeUs, int which) {
        return this.mInteractiveTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public long getLowPowerModeEnabledTime(long elapsedRealtimeUs, int which) {
        return this.mLowPowerModeEnabledTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getLowPowerModeEnabledCount(int which) {
        return this.mLowPowerModeEnabledTimer.getCountLocked(which);
    }

    @Override
    public int getNumConnectivityChange(int which) {
        int val = this.mNumConnectivityChange;
        if (which == 1) {
            return val - this.mLoadedNumConnectivityChange;
        }
        if (which == 2) {
            return val - this.mUnpluggedNumConnectivityChange;
        }
        return val;
    }

    @Override
    public long getPhoneOnTime(long elapsedRealtimeUs, int which) {
        return this.mPhoneOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getPhoneOnCount(int which) {
        return this.mPhoneOnTimer.getCountLocked(which);
    }

    @Override
    public long getPhoneSignalStrengthTime(int strengthBin, long elapsedRealtimeUs, int which) {
        return this.mPhoneSignalStrengthsTimer[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public long getPhoneSignalScanningTime(long elapsedRealtimeUs, int which) {
        return this.mPhoneSignalScanningTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getPhoneSignalStrengthCount(int strengthBin, int which) {
        return this.mPhoneSignalStrengthsTimer[strengthBin].getCountLocked(which);
    }

    @Override
    public long getPhoneDataConnectionTime(int dataType, long elapsedRealtimeUs, int which) {
        return this.mPhoneDataConnectionsTimer[dataType].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getPhoneDataConnectionCount(int dataType, int which) {
        return this.mPhoneDataConnectionsTimer[dataType].getCountLocked(which);
    }

    @Override
    public long getMobileRadioActiveTime(long elapsedRealtimeUs, int which) {
        return this.mMobileRadioActiveTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getMobileRadioActiveCount(int which) {
        return this.mMobileRadioActiveTimer.getCountLocked(which);
    }

    @Override
    public long getMobileRadioActiveAdjustedTime(int which) {
        return this.mMobileRadioActiveAdjustedTime.getCountLocked(which);
    }

    @Override
    public long getMobileRadioActiveUnknownTime(int which) {
        return this.mMobileRadioActiveUnknownTime.getCountLocked(which);
    }

    @Override
    public int getMobileRadioActiveUnknownCount(int which) {
        return (int) this.mMobileRadioActiveUnknownCount.getCountLocked(which);
    }

    @Override
    public long getWifiOnTime(long elapsedRealtimeUs, int which) {
        return this.mWifiOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public long getGlobalWifiRunningTime(long elapsedRealtimeUs, int which) {
        return this.mGlobalWifiRunningTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public long getWifiStateTime(int wifiState, long elapsedRealtimeUs, int which) {
        return this.mWifiStateTimer[wifiState].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getWifiStateCount(int wifiState, int which) {
        return this.mWifiStateTimer[wifiState].getCountLocked(which);
    }

    @Override
    public long getWifiSupplStateTime(int state, long elapsedRealtimeUs, int which) {
        return this.mWifiSupplStateTimer[state].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getWifiSupplStateCount(int state, int which) {
        return this.mWifiSupplStateTimer[state].getCountLocked(which);
    }

    @Override
    public long getWifiSignalStrengthTime(int strengthBin, long elapsedRealtimeUs, int which) {
        return this.mWifiSignalStrengthsTimer[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getWifiSignalStrengthCount(int strengthBin, int which) {
        return this.mWifiSignalStrengthsTimer[strengthBin].getCountLocked(which);
    }

    @Override
    public long getBluetoothOnTime(long elapsedRealtimeUs, int which) {
        return this.mBluetoothOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public long getBluetoothStateTime(int bluetoothState, long elapsedRealtimeUs, int which) {
        return this.mBluetoothStateTimer[bluetoothState].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public int getBluetoothStateCount(int bluetoothState, int which) {
        return this.mBluetoothStateTimer[bluetoothState].getCountLocked(which);
    }

    @Override
    public long getFlashlightOnTime(long elapsedRealtimeUs, int which) {
        return this.mFlashlightOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override
    public long getFlashlightOnCount(int which) {
        return this.mFlashlightOnTimer.getCountLocked(which);
    }

    @Override
    public long getNetworkActivityBytes(int type, int which) {
        if (type < 0 || type >= this.mNetworkByteActivityCounters.length) {
            return 0L;
        }
        return this.mNetworkByteActivityCounters[type].getCountLocked(which);
    }

    @Override
    public long getNetworkActivityPackets(int type, int which) {
        if (type < 0 || type >= this.mNetworkPacketActivityCounters.length) {
            return 0L;
        }
        return this.mNetworkPacketActivityCounters[type].getCountLocked(which);
    }

    boolean isStartClockTimeValid() {
        return this.mStartClockTime > 31536000000L;
    }

    @Override
    public long getStartClockTime() {
        if (!isStartClockTimeValid()) {
            this.mStartClockTime = System.currentTimeMillis();
            if (isStartClockTimeValid()) {
                recordCurrentTimeChangeLocked(this.mStartClockTime, SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
            }
        }
        return this.mStartClockTime;
    }

    @Override
    public String getStartPlatformVersion() {
        return this.mStartPlatformVersion;
    }

    @Override
    public String getEndPlatformVersion() {
        return this.mEndPlatformVersion;
    }

    @Override
    public int getParcelVersion() {
        return 116;
    }

    @Override
    public boolean getIsOnBattery() {
        return this.mOnBattery;
    }

    @Override
    public SparseArray<? extends BatteryStats.Uid> getUidStats() {
        return this.mUidStats;
    }

    public final class Uid extends BatteryStats.Uid {
        static final int NO_BATCHED_SCAN_STARTED = -1;
        static final int PROCESS_STATE_NONE = 3;
        StopwatchTimer mAudioTurnedOnTimer;
        StopwatchTimer mForegroundActivityTimer;
        boolean mFullWifiLockOut;
        StopwatchTimer mFullWifiLockTimer;
        LongSamplingCounter mMobileRadioActiveCount;
        LongSamplingCounter mMobileRadioActiveTime;
        LongSamplingCounter[] mNetworkByteActivityCounters;
        LongSamplingCounter[] mNetworkPacketActivityCounters;
        final int mUid;
        Counter[] mUserActivityCounters;
        BatchTimer mVibratorOnTimer;
        StopwatchTimer mVideoTurnedOnTimer;
        boolean mWifiMulticastEnabled;
        StopwatchTimer mWifiMulticastTimer;
        boolean mWifiRunning;
        StopwatchTimer mWifiRunningTimer;
        boolean mWifiScanStarted;
        StopwatchTimer mWifiScanTimer;
        int mWifiBatchedScanBinStarted = -1;
        int mProcessState = 3;
        final OverflowArrayMap<Wakelock> mWakelockStats = new OverflowArrayMap<Wakelock>() {
            {
                BatteryStatsImpl batteryStatsImpl = BatteryStatsImpl.this;
            }

            @Override
            public Wakelock instantiateObject() {
                return Uid.this.new Wakelock();
            }
        };
        final OverflowArrayMap<StopwatchTimer> mSyncStats = new OverflowArrayMap<StopwatchTimer>() {
            {
                BatteryStatsImpl batteryStatsImpl = BatteryStatsImpl.this;
            }

            @Override
            public StopwatchTimer instantiateObject() {
                return new StopwatchTimer(Uid.this, 13, null, BatteryStatsImpl.this.mOnBatteryTimeBase);
            }
        };
        final OverflowArrayMap<StopwatchTimer> mJobStats = new OverflowArrayMap<StopwatchTimer>() {
            {
                BatteryStatsImpl batteryStatsImpl = BatteryStatsImpl.this;
            }

            @Override
            public StopwatchTimer instantiateObject() {
                return new StopwatchTimer(Uid.this, 14, null, BatteryStatsImpl.this.mOnBatteryTimeBase);
            }
        };
        final SparseArray<Sensor> mSensorStats = new SparseArray<>();
        final ArrayMap<String, Proc> mProcessStats = new ArrayMap<>();
        final ArrayMap<String, Pkg> mPackageStats = new ArrayMap<>();
        final SparseArray<BatteryStats.Uid.Pid> mPids = new SparseArray<>();
        StopwatchTimer[] mWifiBatchedScanTimer = new StopwatchTimer[5];
        StopwatchTimer[] mProcessStateTimer = new StopwatchTimer[3];

        public Uid(int uid) {
            this.mUid = uid;
            this.mWifiRunningTimer = new StopwatchTimer(this, 4, BatteryStatsImpl.this.mWifiRunningTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
            this.mFullWifiLockTimer = new StopwatchTimer(this, 5, BatteryStatsImpl.this.mFullWifiLockTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
            this.mWifiScanTimer = new StopwatchTimer(this, 6, BatteryStatsImpl.this.mWifiScanTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
            this.mWifiMulticastTimer = new StopwatchTimer(this, 7, BatteryStatsImpl.this.mWifiMulticastTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
        }

        @Override
        public Map<String, ? extends BatteryStats.Uid.Wakelock> getWakelockStats() {
            return this.mWakelockStats.getMap();
        }

        @Override
        public Map<String, ? extends BatteryStats.Timer> getSyncStats() {
            return this.mSyncStats.getMap();
        }

        @Override
        public Map<String, ? extends BatteryStats.Timer> getJobStats() {
            return this.mJobStats.getMap();
        }

        @Override
        public SparseArray<? extends BatteryStats.Uid.Sensor> getSensorStats() {
            return this.mSensorStats;
        }

        @Override
        public Map<String, ? extends BatteryStats.Uid.Proc> getProcessStats() {
            return this.mProcessStats;
        }

        @Override
        public Map<String, ? extends BatteryStats.Uid.Pkg> getPackageStats() {
            return this.mPackageStats;
        }

        @Override
        public int getUid() {
            return this.mUid;
        }

        @Override
        public void noteWifiRunningLocked(long elapsedRealtimeMs) {
            if (!this.mWifiRunning) {
                this.mWifiRunning = true;
                if (this.mWifiRunningTimer == null) {
                    this.mWifiRunningTimer = new StopwatchTimer(this, 4, BatteryStatsImpl.this.mWifiRunningTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
                }
                this.mWifiRunningTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override
        public void noteWifiStoppedLocked(long elapsedRealtimeMs) {
            if (this.mWifiRunning) {
                this.mWifiRunning = false;
                this.mWifiRunningTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override
        public void noteFullWifiLockAcquiredLocked(long elapsedRealtimeMs) {
            if (!this.mFullWifiLockOut) {
                this.mFullWifiLockOut = true;
                if (this.mFullWifiLockTimer == null) {
                    this.mFullWifiLockTimer = new StopwatchTimer(this, 5, BatteryStatsImpl.this.mFullWifiLockTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
                }
                this.mFullWifiLockTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override
        public void noteFullWifiLockReleasedLocked(long elapsedRealtimeMs) {
            if (this.mFullWifiLockOut) {
                this.mFullWifiLockOut = false;
                this.mFullWifiLockTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override
        public void noteWifiScanStartedLocked(long elapsedRealtimeMs) {
            if (!this.mWifiScanStarted) {
                this.mWifiScanStarted = true;
                if (this.mWifiScanTimer == null) {
                    this.mWifiScanTimer = new StopwatchTimer(this, 6, BatteryStatsImpl.this.mWifiScanTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
                }
                this.mWifiScanTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override
        public void noteWifiScanStoppedLocked(long elapsedRealtimeMs) {
            if (this.mWifiScanStarted) {
                this.mWifiScanStarted = false;
                this.mWifiScanTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override
        public void noteWifiBatchedScanStartedLocked(int csph, long elapsedRealtimeMs) {
            int bin = 0;
            while (csph > 8 && bin < 5) {
                csph >>= 3;
                bin++;
            }
            if (this.mWifiBatchedScanBinStarted != bin) {
                if (this.mWifiBatchedScanBinStarted != -1) {
                    this.mWifiBatchedScanTimer[this.mWifiBatchedScanBinStarted].stopRunningLocked(elapsedRealtimeMs);
                }
                this.mWifiBatchedScanBinStarted = bin;
                if (this.mWifiBatchedScanTimer[bin] == null) {
                    makeWifiBatchedScanBin(bin, null);
                }
                this.mWifiBatchedScanTimer[bin].startRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override
        public void noteWifiBatchedScanStoppedLocked(long elapsedRealtimeMs) {
            if (this.mWifiBatchedScanBinStarted != -1) {
                this.mWifiBatchedScanTimer[this.mWifiBatchedScanBinStarted].stopRunningLocked(elapsedRealtimeMs);
                this.mWifiBatchedScanBinStarted = -1;
            }
        }

        @Override
        public void noteWifiMulticastEnabledLocked(long elapsedRealtimeMs) {
            if (!this.mWifiMulticastEnabled) {
                this.mWifiMulticastEnabled = true;
                if (this.mWifiMulticastTimer == null) {
                    this.mWifiMulticastTimer = new StopwatchTimer(this, 7, BatteryStatsImpl.this.mWifiMulticastTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
                }
                this.mWifiMulticastTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override
        public void noteWifiMulticastDisabledLocked(long elapsedRealtimeMs) {
            if (this.mWifiMulticastEnabled) {
                this.mWifiMulticastEnabled = false;
                this.mWifiMulticastTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createAudioTurnedOnTimerLocked() {
            if (this.mAudioTurnedOnTimer == null) {
                this.mAudioTurnedOnTimer = new StopwatchTimer(this, 15, BatteryStatsImpl.this.mAudioTurnedOnTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
            }
            return this.mAudioTurnedOnTimer;
        }

        public void noteAudioTurnedOnLocked(long elapsedRealtimeMs) {
            createAudioTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteAudioTurnedOffLocked(long elapsedRealtimeMs) {
            if (this.mAudioTurnedOnTimer != null) {
                this.mAudioTurnedOnTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetAudioLocked(long elapsedRealtimeMs) {
            if (this.mAudioTurnedOnTimer != null) {
                this.mAudioTurnedOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createVideoTurnedOnTimerLocked() {
            if (this.mVideoTurnedOnTimer == null) {
                this.mVideoTurnedOnTimer = new StopwatchTimer(this, 8, BatteryStatsImpl.this.mVideoTurnedOnTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
            }
            return this.mVideoTurnedOnTimer;
        }

        public void noteVideoTurnedOnLocked(long elapsedRealtimeMs) {
            createVideoTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteVideoTurnedOffLocked(long elapsedRealtimeMs) {
            if (this.mVideoTurnedOnTimer != null) {
                this.mVideoTurnedOnTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetVideoLocked(long elapsedRealtimeMs) {
            if (this.mVideoTurnedOnTimer != null) {
                this.mVideoTurnedOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createForegroundActivityTimerLocked() {
            if (this.mForegroundActivityTimer == null) {
                this.mForegroundActivityTimer = new StopwatchTimer(this, 10, null, BatteryStatsImpl.this.mOnBatteryTimeBase);
            }
            return this.mForegroundActivityTimer;
        }

        @Override
        public void noteActivityResumedLocked(long elapsedRealtimeMs) {
            createForegroundActivityTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        @Override
        public void noteActivityPausedLocked(long elapsedRealtimeMs) {
            if (this.mForegroundActivityTimer != null) {
                this.mForegroundActivityTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        void updateUidProcessStateLocked(int state, long elapsedRealtimeMs) {
            if (this.mProcessState != state) {
                if (this.mProcessState != 3) {
                    this.mProcessStateTimer[this.mProcessState].stopRunningLocked(elapsedRealtimeMs);
                }
                this.mProcessState = state;
                if (state != 3) {
                    if (this.mProcessStateTimer[state] == null) {
                        makeProcessState(state, null);
                    }
                    this.mProcessStateTimer[state].startRunningLocked(elapsedRealtimeMs);
                }
            }
        }

        public BatchTimer createVibratorOnTimerLocked() {
            if (this.mVibratorOnTimer == null) {
                this.mVibratorOnTimer = new BatchTimer(this, 9, BatteryStatsImpl.this.mOnBatteryTimeBase);
            }
            return this.mVibratorOnTimer;
        }

        public void noteVibratorOnLocked(long durationMillis) {
            createVibratorOnTimerLocked().addDuration(BatteryStatsImpl.this, durationMillis);
        }

        public void noteVibratorOffLocked() {
            if (this.mVibratorOnTimer != null) {
                this.mVibratorOnTimer.abortLastDuration(BatteryStatsImpl.this);
            }
        }

        @Override
        public long getWifiRunningTime(long elapsedRealtimeUs, int which) {
            if (this.mWifiRunningTimer == null) {
                return 0L;
            }
            return this.mWifiRunningTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override
        public long getFullWifiLockTime(long elapsedRealtimeUs, int which) {
            if (this.mFullWifiLockTimer == null) {
                return 0L;
            }
            return this.mFullWifiLockTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override
        public long getWifiScanTime(long elapsedRealtimeUs, int which) {
            if (this.mWifiScanTimer == null) {
                return 0L;
            }
            return this.mWifiScanTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override
        public long getWifiBatchedScanTime(int csphBin, long elapsedRealtimeUs, int which) {
            if (csphBin < 0 || csphBin >= 5 || this.mWifiBatchedScanTimer[csphBin] == null) {
                return 0L;
            }
            return this.mWifiBatchedScanTimer[csphBin].getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override
        public long getWifiMulticastTime(long elapsedRealtimeUs, int which) {
            if (this.mWifiMulticastTimer == null) {
                return 0L;
            }
            return this.mWifiMulticastTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override
        public long getAudioTurnedOnTime(long elapsedRealtimeUs, int which) {
            if (this.mAudioTurnedOnTimer == null) {
                return 0L;
            }
            return this.mAudioTurnedOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override
        public long getVideoTurnedOnTime(long elapsedRealtimeUs, int which) {
            if (this.mVideoTurnedOnTimer == null) {
                return 0L;
            }
            return this.mVideoTurnedOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override
        public Timer getForegroundActivityTimer() {
            return this.mForegroundActivityTimer;
        }

        void makeProcessState(int i, Parcel in) {
            if (i >= 0 && i < 3) {
                if (in == null) {
                    this.mProcessStateTimer[i] = new StopwatchTimer(this, 12, null, BatteryStatsImpl.this.mOnBatteryTimeBase);
                } else {
                    this.mProcessStateTimer[i] = new StopwatchTimer(this, 12, null, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                }
            }
        }

        @Override
        public long getProcessStateTime(int state, long elapsedRealtimeUs, int which) {
            if (state < 0 || state >= 3 || this.mProcessStateTimer[state] == null) {
                return 0L;
            }
            return this.mProcessStateTimer[state].getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override
        public Timer getVibratorOnTimer() {
            return this.mVibratorOnTimer;
        }

        @Override
        public void noteUserActivityLocked(int type) {
            if (this.mUserActivityCounters == null) {
                initUserActivityLocked();
            }
            if (type >= 0 && type < 3) {
                this.mUserActivityCounters[type].stepAtomic();
            } else {
                Slog.w(BatteryStatsImpl.TAG, "Unknown user activity type " + type + " was specified.", new Throwable());
            }
        }

        @Override
        public boolean hasUserActivity() {
            return this.mUserActivityCounters != null;
        }

        @Override
        public int getUserActivityCount(int type, int which) {
            if (this.mUserActivityCounters == null) {
                return 0;
            }
            return this.mUserActivityCounters[type].getCountLocked(which);
        }

        void makeWifiBatchedScanBin(int i, Parcel in) {
            if (i >= 0 && i < 5) {
                ArrayList<StopwatchTimer> collected = BatteryStatsImpl.this.mWifiBatchedScanTimers.get(i);
                if (collected == null) {
                    collected = new ArrayList<>();
                    BatteryStatsImpl.this.mWifiBatchedScanTimers.put(i, collected);
                }
                if (in == null) {
                    this.mWifiBatchedScanTimer[i] = new StopwatchTimer(this, 11, collected, BatteryStatsImpl.this.mOnBatteryTimeBase);
                } else {
                    this.mWifiBatchedScanTimer[i] = new StopwatchTimer(this, 11, collected, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                }
            }
        }

        void initUserActivityLocked() {
            this.mUserActivityCounters = new Counter[3];
            for (int i = 0; i < 3; i++) {
                this.mUserActivityCounters[i] = new Counter(BatteryStatsImpl.this.mOnBatteryTimeBase);
            }
        }

        void noteNetworkActivityLocked(int type, long deltaBytes, long deltaPackets) {
            if (this.mNetworkByteActivityCounters == null) {
                initNetworkActivityLocked();
            }
            if (type >= 0 && type < 4) {
                this.mNetworkByteActivityCounters[type].addCountLocked(deltaBytes);
                this.mNetworkPacketActivityCounters[type].addCountLocked(deltaPackets);
            } else {
                Slog.w(BatteryStatsImpl.TAG, "Unknown network activity type " + type + " was specified.", new Throwable());
            }
        }

        void noteMobileRadioActiveTimeLocked(long batteryUptime) {
            if (this.mNetworkByteActivityCounters == null) {
                initNetworkActivityLocked();
            }
            this.mMobileRadioActiveTime.addCountLocked(batteryUptime);
            this.mMobileRadioActiveCount.addCountLocked(1L);
        }

        @Override
        public boolean hasNetworkActivity() {
            return this.mNetworkByteActivityCounters != null;
        }

        @Override
        public long getNetworkActivityBytes(int type, int which) {
            if (this.mNetworkByteActivityCounters == null || type < 0 || type >= this.mNetworkByteActivityCounters.length) {
                return 0L;
            }
            return this.mNetworkByteActivityCounters[type].getCountLocked(which);
        }

        @Override
        public long getNetworkActivityPackets(int type, int which) {
            if (this.mNetworkPacketActivityCounters == null || type < 0 || type >= this.mNetworkPacketActivityCounters.length) {
                return 0L;
            }
            return this.mNetworkPacketActivityCounters[type].getCountLocked(which);
        }

        @Override
        public long getMobileRadioActiveTime(int which) {
            if (this.mMobileRadioActiveTime != null) {
                return this.mMobileRadioActiveTime.getCountLocked(which);
            }
            return 0L;
        }

        @Override
        public int getMobileRadioActiveCount(int which) {
            if (this.mMobileRadioActiveCount != null) {
                return (int) this.mMobileRadioActiveCount.getCountLocked(which);
            }
            return 0;
        }

        void initNetworkActivityLocked() {
            this.mNetworkByteActivityCounters = new LongSamplingCounter[4];
            this.mNetworkPacketActivityCounters = new LongSamplingCounter[4];
            for (int i = 0; i < 4; i++) {
                this.mNetworkByteActivityCounters[i] = new LongSamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase);
                this.mNetworkPacketActivityCounters[i] = new LongSamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase);
            }
            this.mMobileRadioActiveTime = new LongSamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase);
            this.mMobileRadioActiveCount = new LongSamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase);
        }

        boolean reset() {
            boolean active = false;
            if (this.mWifiRunningTimer != null) {
                boolean active2 = false | (!this.mWifiRunningTimer.reset(false));
                active = active2 | this.mWifiRunning;
            }
            if (this.mFullWifiLockTimer != null) {
                active = active | (!this.mFullWifiLockTimer.reset(false)) | this.mFullWifiLockOut;
            }
            if (this.mWifiScanTimer != null) {
                active = active | (!this.mWifiScanTimer.reset(false)) | this.mWifiScanStarted;
            }
            if (this.mWifiBatchedScanTimer != null) {
                for (int i = 0; i < 5; i++) {
                    if (this.mWifiBatchedScanTimer[i] != null) {
                        active |= !this.mWifiBatchedScanTimer[i].reset(false);
                    }
                }
                active |= this.mWifiBatchedScanBinStarted != -1;
            }
            if (this.mWifiMulticastTimer != null) {
                active = active | (!this.mWifiMulticastTimer.reset(false)) | this.mWifiMulticastEnabled;
            }
            if (this.mAudioTurnedOnTimer != null) {
                active |= !this.mAudioTurnedOnTimer.reset(false);
            }
            if (this.mVideoTurnedOnTimer != null) {
                active |= !this.mVideoTurnedOnTimer.reset(false);
            }
            if (this.mForegroundActivityTimer != null) {
                active |= !this.mForegroundActivityTimer.reset(false);
            }
            if (this.mProcessStateTimer != null) {
                for (int i2 = 0; i2 < 3; i2++) {
                    if (this.mProcessStateTimer[i2] != null) {
                        active |= !this.mProcessStateTimer[i2].reset(false);
                    }
                }
                active |= this.mProcessState != 3;
            }
            if (this.mVibratorOnTimer != null) {
                if (this.mVibratorOnTimer.reset(false)) {
                    this.mVibratorOnTimer.detach();
                    this.mVibratorOnTimer = null;
                } else {
                    active = true;
                }
            }
            if (this.mUserActivityCounters != null) {
                for (int i3 = 0; i3 < 3; i3++) {
                    this.mUserActivityCounters[i3].reset(false);
                }
            }
            if (this.mNetworkByteActivityCounters != null) {
                for (int i4 = 0; i4 < 4; i4++) {
                    this.mNetworkByteActivityCounters[i4].reset(false);
                    this.mNetworkPacketActivityCounters[i4].reset(false);
                }
                this.mMobileRadioActiveTime.reset(false);
                this.mMobileRadioActiveCount.reset(false);
            }
            ArrayMap<String, Wakelock> wakeStats = this.mWakelockStats.getMap();
            for (int iw = wakeStats.size() - 1; iw >= 0; iw--) {
                Wakelock wl = wakeStats.valueAt(iw);
                if (wl.reset()) {
                    wakeStats.removeAt(iw);
                } else {
                    active = true;
                }
            }
            this.mWakelockStats.cleanup();
            ArrayMap<String, StopwatchTimer> syncStats = this.mSyncStats.getMap();
            for (int is = syncStats.size() - 1; is >= 0; is--) {
                StopwatchTimer timer = syncStats.valueAt(is);
                if (timer.reset(false)) {
                    syncStats.removeAt(is);
                    timer.detach();
                } else {
                    active = true;
                }
            }
            this.mSyncStats.cleanup();
            ArrayMap<String, StopwatchTimer> jobStats = this.mJobStats.getMap();
            for (int ij = jobStats.size() - 1; ij >= 0; ij--) {
                StopwatchTimer timer2 = jobStats.valueAt(ij);
                if (timer2.reset(false)) {
                    jobStats.removeAt(ij);
                    timer2.detach();
                } else {
                    active = true;
                }
            }
            this.mJobStats.cleanup();
            for (int ise = this.mSensorStats.size() - 1; ise >= 0; ise--) {
                Sensor s = this.mSensorStats.valueAt(ise);
                if (s.reset()) {
                    this.mSensorStats.removeAt(ise);
                } else {
                    active = true;
                }
            }
            for (int ip = this.mProcessStats.size() - 1; ip >= 0; ip--) {
                Proc proc = this.mProcessStats.valueAt(ip);
                if (proc.mProcessState == 3) {
                    proc.detach();
                    this.mProcessStats.removeAt(ip);
                } else {
                    proc.reset();
                    active = true;
                }
            }
            if (this.mPids.size() > 0) {
                for (int i5 = this.mPids.size() - 1; i5 >= 0; i5--) {
                    BatteryStats.Uid.Pid pid = this.mPids.valueAt(i5);
                    if (pid.mWakeNesting > 0) {
                        active = true;
                    } else {
                        this.mPids.removeAt(i5);
                    }
                }
            }
            if (this.mPackageStats.size() > 0) {
                for (Map.Entry<String, Pkg> pkgEntry : this.mPackageStats.entrySet()) {
                    Pkg p = pkgEntry.getValue();
                    p.detach();
                    if (p.mServiceStats.size() > 0) {
                        for (Map.Entry<String, Pkg.Serv> servEntry : p.mServiceStats.entrySet()) {
                            servEntry.getValue().detach();
                        }
                    }
                }
                this.mPackageStats.clear();
            }
            if (!active) {
                if (this.mWifiRunningTimer != null) {
                    this.mWifiRunningTimer.detach();
                }
                if (this.mFullWifiLockTimer != null) {
                    this.mFullWifiLockTimer.detach();
                }
                if (this.mWifiScanTimer != null) {
                    this.mWifiScanTimer.detach();
                }
                for (int i6 = 0; i6 < 5; i6++) {
                    if (this.mWifiBatchedScanTimer[i6] != null) {
                        this.mWifiBatchedScanTimer[i6].detach();
                    }
                }
                if (this.mWifiMulticastTimer != null) {
                    this.mWifiMulticastTimer.detach();
                }
                if (this.mAudioTurnedOnTimer != null) {
                    this.mAudioTurnedOnTimer.detach();
                    this.mAudioTurnedOnTimer = null;
                }
                if (this.mVideoTurnedOnTimer != null) {
                    this.mVideoTurnedOnTimer.detach();
                    this.mVideoTurnedOnTimer = null;
                }
                if (this.mForegroundActivityTimer != null) {
                    this.mForegroundActivityTimer.detach();
                    this.mForegroundActivityTimer = null;
                }
                if (this.mUserActivityCounters != null) {
                    for (int i7 = 0; i7 < 3; i7++) {
                        this.mUserActivityCounters[i7].detach();
                    }
                }
                if (this.mNetworkByteActivityCounters != null) {
                    for (int i8 = 0; i8 < 4; i8++) {
                        this.mNetworkByteActivityCounters[i8].detach();
                        this.mNetworkPacketActivityCounters[i8].detach();
                    }
                }
                this.mPids.clear();
            }
            return !active;
        }

        void writeToParcelLocked(Parcel out, long elapsedRealtimeUs) {
            ArrayMap<String, Wakelock> wakeStats = this.mWakelockStats.getMap();
            int NW = wakeStats.size();
            out.writeInt(NW);
            for (int iw = 0; iw < NW; iw++) {
                out.writeString(wakeStats.keyAt(iw));
                Wakelock wakelock = wakeStats.valueAt(iw);
                wakelock.writeToParcelLocked(out, elapsedRealtimeUs);
            }
            ArrayMap<String, StopwatchTimer> syncStats = this.mSyncStats.getMap();
            int NS = syncStats.size();
            out.writeInt(NS);
            for (int is = 0; is < NS; is++) {
                out.writeString(syncStats.keyAt(is));
                StopwatchTimer timer = syncStats.valueAt(is);
                Timer.writeTimerToParcel(out, timer, elapsedRealtimeUs);
            }
            ArrayMap<String, StopwatchTimer> jobStats = this.mJobStats.getMap();
            int NJ = jobStats.size();
            out.writeInt(NJ);
            for (int ij = 0; ij < NJ; ij++) {
                out.writeString(jobStats.keyAt(ij));
                StopwatchTimer timer2 = jobStats.valueAt(ij);
                Timer.writeTimerToParcel(out, timer2, elapsedRealtimeUs);
            }
            int NSE = this.mSensorStats.size();
            out.writeInt(NSE);
            for (int ise = 0; ise < NSE; ise++) {
                out.writeInt(this.mSensorStats.keyAt(ise));
                Sensor sensor = this.mSensorStats.valueAt(ise);
                sensor.writeToParcelLocked(out, elapsedRealtimeUs);
            }
            int NP = this.mProcessStats.size();
            out.writeInt(NP);
            for (int ip = 0; ip < NP; ip++) {
                out.writeString(this.mProcessStats.keyAt(ip));
                Proc proc = this.mProcessStats.valueAt(ip);
                proc.writeToParcelLocked(out);
            }
            out.writeInt(this.mPackageStats.size());
            for (Map.Entry<String, Pkg> pkgEntry : this.mPackageStats.entrySet()) {
                out.writeString(pkgEntry.getKey());
                Pkg pkg = pkgEntry.getValue();
                pkg.writeToParcelLocked(out);
            }
            if (this.mWifiRunningTimer != null) {
                out.writeInt(1);
                this.mWifiRunningTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mFullWifiLockTimer != null) {
                out.writeInt(1);
                this.mFullWifiLockTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mWifiScanTimer != null) {
                out.writeInt(1);
                this.mWifiScanTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            for (int i = 0; i < 5; i++) {
                if (this.mWifiBatchedScanTimer[i] != null) {
                    out.writeInt(1);
                    this.mWifiBatchedScanTimer[i].writeToParcel(out, elapsedRealtimeUs);
                } else {
                    out.writeInt(0);
                }
            }
            if (this.mWifiMulticastTimer != null) {
                out.writeInt(1);
                this.mWifiMulticastTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mAudioTurnedOnTimer != null) {
                out.writeInt(1);
                this.mAudioTurnedOnTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mVideoTurnedOnTimer != null) {
                out.writeInt(1);
                this.mVideoTurnedOnTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mForegroundActivityTimer != null) {
                out.writeInt(1);
                this.mForegroundActivityTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            for (int i2 = 0; i2 < 3; i2++) {
                if (this.mProcessStateTimer[i2] != null) {
                    out.writeInt(1);
                    this.mProcessStateTimer[i2].writeToParcel(out, elapsedRealtimeUs);
                } else {
                    out.writeInt(0);
                }
            }
            if (this.mVibratorOnTimer != null) {
                out.writeInt(1);
                this.mVibratorOnTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mUserActivityCounters != null) {
                out.writeInt(1);
                for (int i3 = 0; i3 < 3; i3++) {
                    this.mUserActivityCounters[i3].writeToParcel(out);
                }
            } else {
                out.writeInt(0);
            }
            if (this.mNetworkByteActivityCounters != null) {
                out.writeInt(1);
                for (int i4 = 0; i4 < 4; i4++) {
                    this.mNetworkByteActivityCounters[i4].writeToParcel(out);
                    this.mNetworkPacketActivityCounters[i4].writeToParcel(out);
                }
                this.mMobileRadioActiveTime.writeToParcel(out);
                this.mMobileRadioActiveCount.writeToParcel(out);
                return;
            }
            out.writeInt(0);
        }

        void readFromParcelLocked(TimeBase timeBase, TimeBase screenOffTimeBase, Parcel in) {
            int numWakelocks = in.readInt();
            this.mWakelockStats.clear();
            for (int j = 0; j < numWakelocks; j++) {
                String wakelockName = in.readString();
                Wakelock wakelock = new Wakelock();
                wakelock.readFromParcelLocked(timeBase, screenOffTimeBase, in);
                this.mWakelockStats.add(wakelockName, wakelock);
            }
            int numSyncs = in.readInt();
            this.mSyncStats.clear();
            for (int j2 = 0; j2 < numSyncs; j2++) {
                String syncName = in.readString();
                if (in.readInt() != 0) {
                    this.mSyncStats.add(syncName, new StopwatchTimer(this, 13, null, timeBase, in));
                }
            }
            int numJobs = in.readInt();
            this.mJobStats.clear();
            for (int j3 = 0; j3 < numJobs; j3++) {
                String jobName = in.readString();
                if (in.readInt() != 0) {
                    this.mJobStats.add(jobName, new StopwatchTimer(this, 14, null, timeBase, in));
                }
            }
            int numSensors = in.readInt();
            this.mSensorStats.clear();
            for (int k = 0; k < numSensors; k++) {
                int sensorNumber = in.readInt();
                Sensor sensor = new Sensor(sensorNumber);
                sensor.readFromParcelLocked(BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                this.mSensorStats.put(sensorNumber, sensor);
            }
            int numProcs = in.readInt();
            this.mProcessStats.clear();
            for (int k2 = 0; k2 < numProcs; k2++) {
                String processName = in.readString();
                Proc proc = new Proc(processName);
                proc.readFromParcelLocked(in);
                this.mProcessStats.put(processName, proc);
            }
            int numPkgs = in.readInt();
            this.mPackageStats.clear();
            for (int l = 0; l < numPkgs; l++) {
                String packageName = in.readString();
                Pkg pkg = new Pkg();
                pkg.readFromParcelLocked(in);
                this.mPackageStats.put(packageName, pkg);
            }
            this.mWifiRunning = false;
            if (in.readInt() != 0) {
                this.mWifiRunningTimer = new StopwatchTimer(this, 4, BatteryStatsImpl.this.mWifiRunningTimers, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
            } else {
                this.mWifiRunningTimer = null;
            }
            this.mFullWifiLockOut = false;
            if (in.readInt() != 0) {
                this.mFullWifiLockTimer = new StopwatchTimer(this, 5, BatteryStatsImpl.this.mFullWifiLockTimers, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
            } else {
                this.mFullWifiLockTimer = null;
            }
            this.mWifiScanStarted = false;
            if (in.readInt() != 0) {
                this.mWifiScanTimer = new StopwatchTimer(this, 6, BatteryStatsImpl.this.mWifiScanTimers, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
            } else {
                this.mWifiScanTimer = null;
            }
            this.mWifiBatchedScanBinStarted = -1;
            for (int i = 0; i < 5; i++) {
                if (in.readInt() != 0) {
                    makeWifiBatchedScanBin(i, in);
                } else {
                    this.mWifiBatchedScanTimer[i] = null;
                }
            }
            this.mWifiMulticastEnabled = false;
            if (in.readInt() != 0) {
                this.mWifiMulticastTimer = new StopwatchTimer(this, 7, BatteryStatsImpl.this.mWifiMulticastTimers, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
            } else {
                this.mWifiMulticastTimer = null;
            }
            if (in.readInt() != 0) {
                this.mAudioTurnedOnTimer = new StopwatchTimer(this, 15, BatteryStatsImpl.this.mAudioTurnedOnTimers, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
            } else {
                this.mAudioTurnedOnTimer = null;
            }
            if (in.readInt() != 0) {
                this.mVideoTurnedOnTimer = new StopwatchTimer(this, 8, BatteryStatsImpl.this.mVideoTurnedOnTimers, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
            } else {
                this.mVideoTurnedOnTimer = null;
            }
            if (in.readInt() != 0) {
                this.mForegroundActivityTimer = new StopwatchTimer(this, 10, null, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
            } else {
                this.mForegroundActivityTimer = null;
            }
            this.mProcessState = 3;
            for (int i2 = 0; i2 < 3; i2++) {
                if (in.readInt() != 0) {
                    makeProcessState(i2, in);
                } else {
                    this.mProcessStateTimer[i2] = null;
                }
            }
            if (in.readInt() != 0) {
                this.mVibratorOnTimer = new BatchTimer(this, 9, BatteryStatsImpl.this.mOnBatteryTimeBase, in);
            } else {
                this.mVibratorOnTimer = null;
            }
            if (in.readInt() != 0) {
                this.mUserActivityCounters = new Counter[3];
                for (int i3 = 0; i3 < 3; i3++) {
                    this.mUserActivityCounters[i3] = new Counter(BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                }
            } else {
                this.mUserActivityCounters = null;
            }
            if (in.readInt() != 0) {
                this.mNetworkByteActivityCounters = new LongSamplingCounter[4];
                this.mNetworkPacketActivityCounters = new LongSamplingCounter[4];
                for (int i4 = 0; i4 < 4; i4++) {
                    this.mNetworkByteActivityCounters[i4] = new LongSamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                    this.mNetworkPacketActivityCounters[i4] = new LongSamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                }
                this.mMobileRadioActiveTime = new LongSamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                this.mMobileRadioActiveCount = new LongSamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                return;
            }
            this.mNetworkByteActivityCounters = null;
            this.mNetworkPacketActivityCounters = null;
        }

        public final class Wakelock extends BatteryStats.Uid.Wakelock {
            StopwatchTimer mTimerFull;
            StopwatchTimer mTimerPartial;
            StopwatchTimer mTimerWindow;

            public Wakelock() {
            }

            private StopwatchTimer readTimerFromParcel(int type, ArrayList<StopwatchTimer> pool, TimeBase timeBase, Parcel in) {
                if (in.readInt() == 0) {
                    return null;
                }
                return new StopwatchTimer(Uid.this, type, pool, timeBase, in);
            }

            boolean reset() {
                boolean wlactive = false;
                if (this.mTimerFull != null) {
                    wlactive = false | (!this.mTimerFull.reset(false));
                }
                if (this.mTimerPartial != null) {
                    wlactive |= !this.mTimerPartial.reset(false);
                }
                if (this.mTimerWindow != null) {
                    wlactive |= !this.mTimerWindow.reset(false);
                }
                if (!wlactive) {
                    if (this.mTimerFull != null) {
                        this.mTimerFull.detach();
                        this.mTimerFull = null;
                    }
                    if (this.mTimerPartial != null) {
                        this.mTimerPartial.detach();
                        this.mTimerPartial = null;
                    }
                    if (this.mTimerWindow != null) {
                        this.mTimerWindow.detach();
                        this.mTimerWindow = null;
                    }
                }
                return !wlactive;
            }

            void readFromParcelLocked(TimeBase timeBase, TimeBase screenOffTimeBase, Parcel in) {
                this.mTimerPartial = readTimerFromParcel(0, BatteryStatsImpl.this.mPartialTimers, screenOffTimeBase, in);
                this.mTimerFull = readTimerFromParcel(1, BatteryStatsImpl.this.mFullTimers, timeBase, in);
                this.mTimerWindow = readTimerFromParcel(2, BatteryStatsImpl.this.mWindowTimers, timeBase, in);
            }

            void writeToParcelLocked(Parcel out, long elapsedRealtimeUs) {
                Timer.writeTimerToParcel(out, this.mTimerPartial, elapsedRealtimeUs);
                Timer.writeTimerToParcel(out, this.mTimerFull, elapsedRealtimeUs);
                Timer.writeTimerToParcel(out, this.mTimerWindow, elapsedRealtimeUs);
            }

            @Override
            public Timer getWakeTime(int type) {
                switch (type) {
                    case 0:
                        return this.mTimerPartial;
                    case 1:
                        return this.mTimerFull;
                    case 2:
                        return this.mTimerWindow;
                    default:
                        throw new IllegalArgumentException("type = " + type);
                }
            }

            public StopwatchTimer getStopwatchTimer(int type) {
                switch (type) {
                    case 0:
                        StopwatchTimer t = this.mTimerPartial;
                        if (t == null) {
                            t = new StopwatchTimer(Uid.this, 0, BatteryStatsImpl.this.mPartialTimers, BatteryStatsImpl.this.mOnBatteryScreenOffTimeBase);
                            this.mTimerPartial = t;
                        }
                        return t;
                    case 1:
                        StopwatchTimer t2 = this.mTimerFull;
                        if (t2 == null) {
                            t2 = new StopwatchTimer(Uid.this, 1, BatteryStatsImpl.this.mFullTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
                            this.mTimerFull = t2;
                        }
                        return t2;
                    case 2:
                        StopwatchTimer t3 = this.mTimerWindow;
                        if (t3 == null) {
                            t3 = new StopwatchTimer(Uid.this, 2, BatteryStatsImpl.this.mWindowTimers, BatteryStatsImpl.this.mOnBatteryTimeBase);
                            this.mTimerWindow = t3;
                        }
                        return t3;
                    default:
                        throw new IllegalArgumentException("type=" + type);
                }
            }
        }

        public final class Sensor extends BatteryStats.Uid.Sensor {
            final int mHandle;
            StopwatchTimer mTimer;

            public Sensor(int handle) {
                this.mHandle = handle;
            }

            private StopwatchTimer readTimerFromParcel(TimeBase timeBase, Parcel in) {
                if (in.readInt() == 0) {
                    return null;
                }
                ArrayList<StopwatchTimer> pool = BatteryStatsImpl.this.mSensorTimers.get(this.mHandle);
                if (pool == null) {
                    pool = new ArrayList<>();
                    BatteryStatsImpl.this.mSensorTimers.put(this.mHandle, pool);
                }
                return new StopwatchTimer(Uid.this, 0, pool, timeBase, in);
            }

            boolean reset() {
                if (!this.mTimer.reset(true)) {
                    return false;
                }
                this.mTimer = null;
                return true;
            }

            void readFromParcelLocked(TimeBase timeBase, Parcel in) {
                this.mTimer = readTimerFromParcel(timeBase, in);
            }

            void writeToParcelLocked(Parcel out, long elapsedRealtimeUs) {
                Timer.writeTimerToParcel(out, this.mTimer, elapsedRealtimeUs);
            }

            @Override
            public Timer getSensorTime() {
                return this.mTimer;
            }

            @Override
            public int getHandle() {
                return this.mHandle;
            }
        }

        public final class Proc extends BatteryStats.Uid.Proc implements TimeBaseObs {
            ArrayList<BatteryStats.Uid.Proc.ExcessivePower> mExcessivePower;
            long mForegroundTime;
            long mLoadedForegroundTime;
            int mLoadedNumAnrs;
            int mLoadedNumCrashes;
            int mLoadedStarts;
            long mLoadedSystemTime;
            long mLoadedUserTime;
            final String mName;
            int mNumAnrs;
            int mNumCrashes;
            SamplingCounter[] mSpeedBins;
            int mStarts;
            long mSystemTime;
            long mUnpluggedForegroundTime;
            int mUnpluggedNumAnrs;
            int mUnpluggedNumCrashes;
            int mUnpluggedStarts;
            long mUnpluggedSystemTime;
            long mUnpluggedUserTime;
            long mUserTime;
            boolean mActive = true;
            int mProcessState = 3;

            Proc(String name) {
                this.mName = name;
                BatteryStatsImpl.this.mOnBatteryTimeBase.add(this);
                this.mSpeedBins = new SamplingCounter[BatteryStatsImpl.this.getCpuSpeedSteps()];
            }

            @Override
            public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
                this.mUnpluggedUserTime = this.mUserTime;
                this.mUnpluggedSystemTime = this.mSystemTime;
                this.mUnpluggedForegroundTime = this.mForegroundTime;
                this.mUnpluggedStarts = this.mStarts;
                this.mUnpluggedNumCrashes = this.mNumCrashes;
                this.mUnpluggedNumAnrs = this.mNumAnrs;
            }

            @Override
            public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            }

            void reset() {
                this.mForegroundTime = 0L;
                this.mSystemTime = 0L;
                this.mUserTime = 0L;
                this.mNumAnrs = 0;
                this.mNumCrashes = 0;
                this.mStarts = 0;
                this.mLoadedForegroundTime = 0L;
                this.mLoadedSystemTime = 0L;
                this.mLoadedUserTime = 0L;
                this.mLoadedNumAnrs = 0;
                this.mLoadedNumCrashes = 0;
                this.mLoadedStarts = 0;
                this.mUnpluggedForegroundTime = 0L;
                this.mUnpluggedSystemTime = 0L;
                this.mUnpluggedUserTime = 0L;
                this.mUnpluggedNumAnrs = 0;
                this.mUnpluggedNumCrashes = 0;
                this.mUnpluggedStarts = 0;
                for (int i = 0; i < this.mSpeedBins.length; i++) {
                    SamplingCounter c = this.mSpeedBins[i];
                    if (c != null) {
                        c.reset(false);
                    }
                }
                this.mExcessivePower = null;
            }

            void detach() {
                this.mActive = false;
                BatteryStatsImpl.this.mOnBatteryTimeBase.remove(this);
                for (int i = 0; i < this.mSpeedBins.length; i++) {
                    SamplingCounter c = this.mSpeedBins[i];
                    if (c != null) {
                        BatteryStatsImpl.this.mOnBatteryTimeBase.remove(c);
                        this.mSpeedBins[i] = null;
                    }
                }
            }

            @Override
            public int countExcessivePowers() {
                if (this.mExcessivePower != null) {
                    return this.mExcessivePower.size();
                }
                return 0;
            }

            @Override
            public BatteryStats.Uid.Proc.ExcessivePower getExcessivePower(int i) {
                if (this.mExcessivePower != null) {
                    return this.mExcessivePower.get(i);
                }
                return null;
            }

            public void addExcessiveWake(long overTime, long usedTime) {
                if (this.mExcessivePower == null) {
                    this.mExcessivePower = new ArrayList<>();
                }
                BatteryStats.Uid.Proc.ExcessivePower ew = new BatteryStats.Uid.Proc.ExcessivePower();
                ew.type = 1;
                ew.overTime = overTime;
                ew.usedTime = usedTime;
                this.mExcessivePower.add(ew);
            }

            public void addExcessiveCpu(long overTime, long usedTime) {
                if (this.mExcessivePower == null) {
                    this.mExcessivePower = new ArrayList<>();
                }
                BatteryStats.Uid.Proc.ExcessivePower ew = new BatteryStats.Uid.Proc.ExcessivePower();
                ew.type = 2;
                ew.overTime = overTime;
                ew.usedTime = usedTime;
                this.mExcessivePower.add(ew);
            }

            void writeExcessivePowerToParcelLocked(Parcel out) {
                if (this.mExcessivePower == null) {
                    out.writeInt(0);
                    return;
                }
                int N = this.mExcessivePower.size();
                out.writeInt(N);
                for (int i = 0; i < N; i++) {
                    BatteryStats.Uid.Proc.ExcessivePower ew = this.mExcessivePower.get(i);
                    out.writeInt(ew.type);
                    out.writeLong(ew.overTime);
                    out.writeLong(ew.usedTime);
                }
            }

            boolean readExcessivePowerFromParcelLocked(Parcel in) {
                int N = in.readInt();
                if (N == 0) {
                    this.mExcessivePower = null;
                    return true;
                }
                if (N > 10000) {
                    Slog.w(BatteryStatsImpl.TAG, "File corrupt: too many excessive power entries " + N);
                    return false;
                }
                this.mExcessivePower = new ArrayList<>();
                for (int i = 0; i < N; i++) {
                    BatteryStats.Uid.Proc.ExcessivePower ew = new BatteryStats.Uid.Proc.ExcessivePower();
                    ew.type = in.readInt();
                    ew.overTime = in.readLong();
                    ew.usedTime = in.readLong();
                    this.mExcessivePower.add(ew);
                }
                return true;
            }

            void writeToParcelLocked(Parcel out) {
                out.writeLong(this.mUserTime);
                out.writeLong(this.mSystemTime);
                out.writeLong(this.mForegroundTime);
                out.writeInt(this.mStarts);
                out.writeInt(this.mNumCrashes);
                out.writeInt(this.mNumAnrs);
                out.writeLong(this.mLoadedUserTime);
                out.writeLong(this.mLoadedSystemTime);
                out.writeLong(this.mLoadedForegroundTime);
                out.writeInt(this.mLoadedStarts);
                out.writeInt(this.mLoadedNumCrashes);
                out.writeInt(this.mLoadedNumAnrs);
                out.writeLong(this.mUnpluggedUserTime);
                out.writeLong(this.mUnpluggedSystemTime);
                out.writeLong(this.mUnpluggedForegroundTime);
                out.writeInt(this.mUnpluggedStarts);
                out.writeInt(this.mUnpluggedNumCrashes);
                out.writeInt(this.mUnpluggedNumAnrs);
                out.writeInt(this.mSpeedBins.length);
                for (int i = 0; i < this.mSpeedBins.length; i++) {
                    SamplingCounter c = this.mSpeedBins[i];
                    if (c != null) {
                        out.writeInt(1);
                        c.writeToParcel(out);
                    } else {
                        out.writeInt(0);
                    }
                }
                writeExcessivePowerToParcelLocked(out);
            }

            void readFromParcelLocked(Parcel in) {
                this.mUserTime = in.readLong();
                this.mSystemTime = in.readLong();
                this.mForegroundTime = in.readLong();
                this.mStarts = in.readInt();
                this.mNumCrashes = in.readInt();
                this.mNumAnrs = in.readInt();
                this.mLoadedUserTime = in.readLong();
                this.mLoadedSystemTime = in.readLong();
                this.mLoadedForegroundTime = in.readLong();
                this.mLoadedStarts = in.readInt();
                this.mLoadedNumCrashes = in.readInt();
                this.mLoadedNumAnrs = in.readInt();
                this.mUnpluggedUserTime = in.readLong();
                this.mUnpluggedSystemTime = in.readLong();
                this.mUnpluggedForegroundTime = in.readLong();
                this.mUnpluggedStarts = in.readInt();
                this.mUnpluggedNumCrashes = in.readInt();
                this.mUnpluggedNumAnrs = in.readInt();
                int bins = in.readInt();
                int steps = BatteryStatsImpl.this.getCpuSpeedSteps();
                if (bins >= steps) {
                    steps = bins;
                }
                this.mSpeedBins = new SamplingCounter[steps];
                for (int i = 0; i < bins; i++) {
                    if (in.readInt() != 0) {
                        this.mSpeedBins[i] = new SamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase, in);
                    }
                }
                readExcessivePowerFromParcelLocked(in);
            }

            public BatteryStatsImpl getBatteryStats() {
                return BatteryStatsImpl.this;
            }

            public void addCpuTimeLocked(int utime, int stime) {
                this.mUserTime += (long) utime;
                this.mSystemTime += (long) stime;
            }

            public void addForegroundTimeLocked(long ttime) {
                this.mForegroundTime += ttime;
            }

            public void incStartsLocked() {
                this.mStarts++;
            }

            public void incNumCrashesLocked() {
                this.mNumCrashes++;
            }

            public void incNumAnrsLocked() {
                this.mNumAnrs++;
            }

            @Override
            public boolean isActive() {
                return this.mActive;
            }

            @Override
            public long getUserTime(int which) {
                long val = this.mUserTime;
                if (which == 1) {
                    return val - this.mLoadedUserTime;
                }
                if (which == 2) {
                    return val - this.mUnpluggedUserTime;
                }
                return val;
            }

            @Override
            public long getSystemTime(int which) {
                long val = this.mSystemTime;
                if (which == 1) {
                    return val - this.mLoadedSystemTime;
                }
                if (which == 2) {
                    return val - this.mUnpluggedSystemTime;
                }
                return val;
            }

            @Override
            public long getForegroundTime(int which) {
                long val = this.mForegroundTime;
                if (which == 1) {
                    return val - this.mLoadedForegroundTime;
                }
                if (which == 2) {
                    return val - this.mUnpluggedForegroundTime;
                }
                return val;
            }

            @Override
            public int getStarts(int which) {
                int val = this.mStarts;
                if (which == 1) {
                    return val - this.mLoadedStarts;
                }
                if (which == 2) {
                    return val - this.mUnpluggedStarts;
                }
                return val;
            }

            @Override
            public int getNumCrashes(int which) {
                int val = this.mNumCrashes;
                if (which == 1) {
                    return val - this.mLoadedNumCrashes;
                }
                if (which == 2) {
                    return val - this.mUnpluggedNumCrashes;
                }
                return val;
            }

            @Override
            public int getNumAnrs(int which) {
                int val = this.mNumAnrs;
                if (which == 1) {
                    return val - this.mLoadedNumAnrs;
                }
                if (which == 2) {
                    return val - this.mUnpluggedNumAnrs;
                }
                return val;
            }

            public void addSpeedStepTimes(long[] values) {
                for (int i = 0; i < this.mSpeedBins.length && i < values.length; i++) {
                    long amt = values[i];
                    if (amt != 0) {
                        SamplingCounter c = this.mSpeedBins[i];
                        if (c == null) {
                            SamplingCounter[] samplingCounterArr = this.mSpeedBins;
                            c = new SamplingCounter(BatteryStatsImpl.this.mOnBatteryTimeBase);
                            samplingCounterArr[i] = c;
                        }
                        c.addCountAtomic(values[i]);
                    }
                }
            }

            @Override
            public long getTimeAtCpuSpeedStep(int speedStep, int which) {
                SamplingCounter c;
                if (speedStep >= this.mSpeedBins.length || (c = this.mSpeedBins[speedStep]) == null) {
                    return 0L;
                }
                return c.getCountLocked(which);
            }
        }

        public final class Pkg extends BatteryStats.Uid.Pkg implements TimeBaseObs {
            int mLastWakeups;
            int mLoadedWakeups;
            final HashMap<String, Serv> mServiceStats = new HashMap<>();
            int mUnpluggedWakeups;
            int mWakeups;

            Pkg() {
                BatteryStatsImpl.this.mOnBatteryScreenOffTimeBase.add(this);
            }

            @Override
            public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
                this.mUnpluggedWakeups = this.mWakeups;
            }

            @Override
            public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            }

            void detach() {
                BatteryStatsImpl.this.mOnBatteryScreenOffTimeBase.remove(this);
            }

            void readFromParcelLocked(Parcel in) {
                this.mWakeups = in.readInt();
                this.mLoadedWakeups = in.readInt();
                this.mLastWakeups = 0;
                this.mUnpluggedWakeups = in.readInt();
                int numServs = in.readInt();
                this.mServiceStats.clear();
                for (int m = 0; m < numServs; m++) {
                    String serviceName = in.readString();
                    Serv serv = new Serv();
                    this.mServiceStats.put(serviceName, serv);
                    serv.readFromParcelLocked(in);
                }
            }

            void writeToParcelLocked(Parcel out) {
                out.writeInt(this.mWakeups);
                out.writeInt(this.mLoadedWakeups);
                out.writeInt(this.mUnpluggedWakeups);
                out.writeInt(this.mServiceStats.size());
                for (Map.Entry<String, Serv> servEntry : this.mServiceStats.entrySet()) {
                    out.writeString(servEntry.getKey());
                    Serv serv = servEntry.getValue();
                    serv.writeToParcelLocked(out);
                }
            }

            @Override
            public Map<String, ? extends BatteryStats.Uid.Pkg.Serv> getServiceStats() {
                return this.mServiceStats;
            }

            @Override
            public int getWakeups(int which) {
                int val = this.mWakeups;
                if (which == 1) {
                    return val - this.mLoadedWakeups;
                }
                if (which == 2) {
                    return val - this.mUnpluggedWakeups;
                }
                return val;
            }

            public final class Serv extends BatteryStats.Uid.Pkg.Serv implements TimeBaseObs {
                int mLastLaunches;
                long mLastStartTime;
                int mLastStarts;
                boolean mLaunched;
                long mLaunchedSince;
                long mLaunchedTime;
                int mLaunches;
                int mLoadedLaunches;
                long mLoadedStartTime;
                int mLoadedStarts;
                boolean mRunning;
                long mRunningSince;
                long mStartTime;
                int mStarts;
                int mUnpluggedLaunches;
                long mUnpluggedStartTime;
                int mUnpluggedStarts;

                Serv() {
                    super();
                    BatteryStatsImpl.this.mOnBatteryTimeBase.add(this);
                }

                @Override
                public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
                    this.mUnpluggedStartTime = getStartTimeToNowLocked(baseUptime);
                    this.mUnpluggedStarts = this.mStarts;
                    this.mUnpluggedLaunches = this.mLaunches;
                }

                @Override
                public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
                }

                void detach() {
                    BatteryStatsImpl.this.mOnBatteryTimeBase.remove(this);
                }

                void readFromParcelLocked(Parcel in) {
                    this.mStartTime = in.readLong();
                    this.mRunningSince = in.readLong();
                    this.mRunning = in.readInt() != 0;
                    this.mStarts = in.readInt();
                    this.mLaunchedTime = in.readLong();
                    this.mLaunchedSince = in.readLong();
                    this.mLaunched = in.readInt() != 0;
                    this.mLaunches = in.readInt();
                    this.mLoadedStartTime = in.readLong();
                    this.mLoadedStarts = in.readInt();
                    this.mLoadedLaunches = in.readInt();
                    this.mLastStartTime = 0L;
                    this.mLastStarts = 0;
                    this.mLastLaunches = 0;
                    this.mUnpluggedStartTime = in.readLong();
                    this.mUnpluggedStarts = in.readInt();
                    this.mUnpluggedLaunches = in.readInt();
                }

                void writeToParcelLocked(Parcel out) {
                    out.writeLong(this.mStartTime);
                    out.writeLong(this.mRunningSince);
                    out.writeInt(this.mRunning ? 1 : 0);
                    out.writeInt(this.mStarts);
                    out.writeLong(this.mLaunchedTime);
                    out.writeLong(this.mLaunchedSince);
                    out.writeInt(this.mLaunched ? 1 : 0);
                    out.writeInt(this.mLaunches);
                    out.writeLong(this.mLoadedStartTime);
                    out.writeInt(this.mLoadedStarts);
                    out.writeInt(this.mLoadedLaunches);
                    out.writeLong(this.mUnpluggedStartTime);
                    out.writeInt(this.mUnpluggedStarts);
                    out.writeInt(this.mUnpluggedLaunches);
                }

                long getLaunchTimeToNowLocked(long batteryUptime) {
                    return !this.mLaunched ? this.mLaunchedTime : (this.mLaunchedTime + batteryUptime) - this.mLaunchedSince;
                }

                long getStartTimeToNowLocked(long batteryUptime) {
                    return !this.mRunning ? this.mStartTime : (this.mStartTime + batteryUptime) - this.mRunningSince;
                }

                public void startLaunchedLocked() {
                    if (!this.mLaunched) {
                        this.mLaunches++;
                        this.mLaunchedSince = BatteryStatsImpl.this.getBatteryUptimeLocked();
                        this.mLaunched = true;
                    }
                }

                public void stopLaunchedLocked() {
                    if (this.mLaunched) {
                        long time = BatteryStatsImpl.this.getBatteryUptimeLocked() - this.mLaunchedSince;
                        if (time > 0) {
                            this.mLaunchedTime += time;
                        } else {
                            this.mLaunches--;
                        }
                        this.mLaunched = false;
                    }
                }

                public void startRunningLocked() {
                    if (!this.mRunning) {
                        this.mStarts++;
                        this.mRunningSince = BatteryStatsImpl.this.getBatteryUptimeLocked();
                        this.mRunning = true;
                    }
                }

                public void stopRunningLocked() {
                    if (this.mRunning) {
                        long time = BatteryStatsImpl.this.getBatteryUptimeLocked() - this.mRunningSince;
                        if (time > 0) {
                            this.mStartTime += time;
                        } else {
                            this.mStarts--;
                        }
                        this.mRunning = false;
                    }
                }

                public BatteryStatsImpl getBatteryStats() {
                    return BatteryStatsImpl.this;
                }

                @Override
                public int getLaunches(int which) {
                    int val = this.mLaunches;
                    if (which == 1) {
                        return val - this.mLoadedLaunches;
                    }
                    if (which == 2) {
                        return val - this.mUnpluggedLaunches;
                    }
                    return val;
                }

                @Override
                public long getStartTime(long now, int which) {
                    long val = getStartTimeToNowLocked(now);
                    if (which == 1) {
                        return val - this.mLoadedStartTime;
                    }
                    if (which == 2) {
                        return val - this.mUnpluggedStartTime;
                    }
                    return val;
                }

                @Override
                public int getStarts(int which) {
                    int val = this.mStarts;
                    if (which == 1) {
                        return val - this.mLoadedStarts;
                    }
                    if (which == 2) {
                        return val - this.mUnpluggedStarts;
                    }
                    return val;
                }
            }

            public BatteryStatsImpl getBatteryStats() {
                return BatteryStatsImpl.this;
            }

            public void incWakeupsLocked() {
                this.mWakeups++;
            }

            final Serv newServiceStatsLocked() {
                return new Serv();
            }
        }

        public Proc getProcessStatsLocked(String name) {
            Proc ps = this.mProcessStats.get(name);
            if (ps == null) {
                Proc ps2 = new Proc(name);
                this.mProcessStats.put(name, ps2);
                return ps2;
            }
            return ps;
        }

        public void updateProcessStateLocked(String procName, int state, long elapsedRealtimeMs) {
            int procState;
            if (state <= 3) {
                procState = 0;
            } else if (state <= 8) {
                procState = 1;
            } else {
                procState = 2;
            }
            updateRealProcessStateLocked(procName, procState, elapsedRealtimeMs);
        }

        public void updateRealProcessStateLocked(String procName, int procState, long elapsedRealtimeMs) {
            boolean changed = true;
            Proc proc = getProcessStatsLocked(procName);
            if (proc.mProcessState != procState) {
                if (procState < proc.mProcessState) {
                    if (this.mProcessState <= procState) {
                        changed = false;
                    }
                } else if (this.mProcessState != proc.mProcessState) {
                    changed = false;
                }
                proc.mProcessState = procState;
                if (changed) {
                    int uidProcState = 3;
                    for (int ip = this.mProcessStats.size() - 1; ip >= 0; ip--) {
                        Proc proc2 = this.mProcessStats.valueAt(ip);
                        if (proc2.mProcessState < uidProcState) {
                            uidProcState = proc2.mProcessState;
                        }
                    }
                    updateUidProcessStateLocked(uidProcState, elapsedRealtimeMs);
                }
            }
        }

        @Override
        public SparseArray<? extends BatteryStats.Uid.Pid> getPidStats() {
            return this.mPids;
        }

        public BatteryStats.Uid.Pid getPidStatsLocked(int pid) {
            BatteryStats.Uid.Pid p = this.mPids.get(pid);
            if (p == null) {
                BatteryStats.Uid.Pid p2 = new BatteryStats.Uid.Pid();
                this.mPids.put(pid, p2);
                return p2;
            }
            return p;
        }

        public Pkg getPackageStatsLocked(String name) {
            Pkg ps = this.mPackageStats.get(name);
            if (ps == null) {
                Pkg ps2 = new Pkg();
                this.mPackageStats.put(name, ps2);
                return ps2;
            }
            return ps;
        }

        public Pkg.Serv getServiceStatsLocked(String pkg, String serv) {
            Pkg ps = getPackageStatsLocked(pkg);
            Pkg.Serv ss = ps.mServiceStats.get(serv);
            if (ss == null) {
                Pkg.Serv ss2 = ps.newServiceStatsLocked();
                ps.mServiceStats.put(serv, ss2);
                return ss2;
            }
            return ss;
        }

        public void readSyncSummaryFromParcelLocked(String name, Parcel in) {
            StopwatchTimer timer = this.mSyncStats.instantiateObject();
            timer.readSummaryFromParcelLocked(in);
            this.mSyncStats.add(name, timer);
        }

        public void readJobSummaryFromParcelLocked(String name, Parcel in) {
            StopwatchTimer timer = this.mJobStats.instantiateObject();
            timer.readSummaryFromParcelLocked(in);
            this.mJobStats.add(name, timer);
        }

        public void readWakeSummaryFromParcelLocked(String wlName, Parcel in) {
            Wakelock wl = new Wakelock();
            this.mWakelockStats.add(wlName, wl);
            if (in.readInt() != 0) {
                wl.getStopwatchTimer(1).readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                wl.getStopwatchTimer(0).readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                wl.getStopwatchTimer(2).readSummaryFromParcelLocked(in);
            }
        }

        public StopwatchTimer getSensorTimerLocked(int sensor, boolean create) {
            Sensor se = this.mSensorStats.get(sensor);
            if (se == null) {
                if (!create) {
                    return null;
                }
                se = new Sensor(sensor);
                this.mSensorStats.put(sensor, se);
            }
            StopwatchTimer t = se.mTimer;
            if (t == null) {
                ArrayList<StopwatchTimer> timers = BatteryStatsImpl.this.mSensorTimers.get(sensor);
                if (timers == null) {
                    timers = new ArrayList<>();
                    BatteryStatsImpl.this.mSensorTimers.put(sensor, timers);
                }
                StopwatchTimer t2 = new StopwatchTimer(this, 3, timers, BatteryStatsImpl.this.mOnBatteryTimeBase);
                se.mTimer = t2;
                return t2;
            }
            return t;
        }

        public void noteStartSyncLocked(String name, long elapsedRealtimeMs) {
            StopwatchTimer t = this.mSyncStats.startObject(name);
            if (t != null) {
                t.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStopSyncLocked(String name, long elapsedRealtimeMs) {
            StopwatchTimer t = this.mSyncStats.stopObject(name);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStartJobLocked(String name, long elapsedRealtimeMs) {
            StopwatchTimer t = this.mJobStats.startObject(name);
            if (t != null) {
                t.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStopJobLocked(String name, long elapsedRealtimeMs) {
            StopwatchTimer t = this.mJobStats.stopObject(name);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStartWakeLocked(int pid, String name, int type, long elapsedRealtimeMs) {
            Wakelock wl = this.mWakelockStats.startObject(name);
            if (wl != null) {
                wl.getStopwatchTimer(type).startRunningLocked(elapsedRealtimeMs);
            }
            if (pid >= 0 && type == 0) {
                BatteryStats.Uid.Pid p = getPidStatsLocked(pid);
                int i = p.mWakeNesting;
                p.mWakeNesting = i + 1;
                if (i == 0) {
                    p.mWakeStartMs = elapsedRealtimeMs;
                }
            }
        }

        public void noteStopWakeLocked(int pid, String name, int type, long elapsedRealtimeMs) {
            BatteryStats.Uid.Pid p;
            Wakelock wl = this.mWakelockStats.stopObject(name);
            if (wl != null) {
                wl.getStopwatchTimer(type).stopRunningLocked(elapsedRealtimeMs);
            }
            if (pid >= 0 && type == 0 && (p = this.mPids.get(pid)) != null && p.mWakeNesting > 0) {
                int i = p.mWakeNesting;
                p.mWakeNesting = i - 1;
                if (i == 1) {
                    p.mWakeSumMs += elapsedRealtimeMs - p.mWakeStartMs;
                    p.mWakeStartMs = 0L;
                }
            }
        }

        public void reportExcessiveWakeLocked(String proc, long overTime, long usedTime) {
            Proc p = getProcessStatsLocked(proc);
            if (p != null) {
                p.addExcessiveWake(overTime, usedTime);
            }
        }

        public void reportExcessiveCpuLocked(String proc, long overTime, long usedTime) {
            Proc p = getProcessStatsLocked(proc);
            if (p != null) {
                p.addExcessiveCpu(overTime, usedTime);
            }
        }

        public void noteStartSensor(int sensor, long elapsedRealtimeMs) {
            StopwatchTimer t = getSensorTimerLocked(sensor, true);
            if (t != null) {
                t.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStopSensor(int sensor, long elapsedRealtimeMs) {
            StopwatchTimer t = getSensorTimerLocked(sensor, false);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStartGps(long elapsedRealtimeMs) {
            StopwatchTimer t = getSensorTimerLocked(-10000, true);
            if (t != null) {
                t.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStopGps(long elapsedRealtimeMs) {
            StopwatchTimer t = getSensorTimerLocked(-10000, false);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public BatteryStatsImpl getBatteryStats() {
            return BatteryStatsImpl.this;
        }
    }

    public BatteryStatsImpl(File systemDir, Handler handler) {
        this.mIsolatedUids = new SparseIntArray();
        this.mUidStats = new SparseArray<>();
        this.mPartialTimers = new ArrayList<>();
        this.mFullTimers = new ArrayList<>();
        this.mWindowTimers = new ArrayList<>();
        this.mSensorTimers = new SparseArray<>();
        this.mWifiRunningTimers = new ArrayList<>();
        this.mFullWifiLockTimers = new ArrayList<>();
        this.mWifiMulticastTimers = new ArrayList<>();
        this.mWifiScanTimers = new ArrayList<>();
        this.mWifiBatchedScanTimers = new SparseArray<>();
        this.mAudioTurnedOnTimers = new ArrayList<>();
        this.mVideoTurnedOnTimers = new ArrayList<>();
        this.mLastPartialTimers = new ArrayList<>();
        this.mOnBatteryTimeBase = new TimeBase();
        this.mOnBatteryScreenOffTimeBase = new TimeBase();
        this.mActiveEvents = new BatteryStats.HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryBuffer = Parcel.obtain();
        this.mHistoryLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryLastLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryReadTmp = new BatteryStats.HistoryItem();
        this.mHistoryAddTmp = new BatteryStats.HistoryItem();
        this.mHistoryTagPool = new HashMap<>();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mHistoryOverflow = false;
        this.mLastHistoryElapsedRealtime = 0L;
        this.mTrackRunningHistoryElapsedRealtime = 0L;
        this.mTrackRunningHistoryUptime = 0L;
        this.mHistoryCur = new BatteryStats.HistoryItem();
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[17];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[4];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[4];
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mBluetoothState = -1;
        this.mBluetoothStateTimer = new StopwatchTimer[4];
        this.mMobileRadioPowerState = DataConnectionRealTimeInfo.DC_POWER_STATE_LOW;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepDurations = new long[200];
        this.mChargeStepDurations = new long[200];
        this.mLastWriteTime = 0L;
        this.mBluetoothPingStart = -1;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mKernelWakelockStats = new HashMap<>();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0L;
        this.mWakeupReasonStats = new HashMap<>();
        this.mProcWakelocksName = new String[3];
        this.mProcWakelocksData = new long[3];
        this.mProcWakelockFileStats = new HashMap();
        this.mNetworkStatsFactory = new NetworkStatsFactory();
        this.mCurMobileSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mLastMobileSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mCurWifiSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mLastWifiSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mTmpNetworkStatsEntry = new NetworkStats.Entry();
        this.mMobileIfaces = new String[0];
        this.mWifiIfaces = new String[0];
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        if (systemDir != null) {
            this.mFile = new JournaledFile(new File(systemDir, "batterystats.bin"), new File(systemDir, "batterystats.bin.tmp"));
        } else {
            this.mFile = null;
        }
        this.mCheckinFile = new AtomicFile(new File(systemDir, "batterystats-checkin.bin"));
        this.mHandler = new MyHandler(handler.getLooper());
        this.mStartCount++;
        this.mScreenOnTimer = new StopwatchTimer(null, -1, null, this.mOnBatteryTimeBase);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i] = new StopwatchTimer(null, (-100) - i, null, this.mOnBatteryTimeBase);
        }
        this.mInteractiveTimer = new StopwatchTimer(null, -9, null, this.mOnBatteryTimeBase);
        this.mLowPowerModeEnabledTimer = new StopwatchTimer(null, -2, null, this.mOnBatteryTimeBase);
        this.mPhoneOnTimer = new StopwatchTimer(null, -3, null, this.mOnBatteryTimeBase);
        for (int i2 = 0; i2 < 5; i2++) {
            this.mPhoneSignalStrengthsTimer[i2] = new StopwatchTimer(null, (-200) - i2, null, this.mOnBatteryTimeBase);
        }
        this.mPhoneSignalScanningTimer = new StopwatchTimer(null, -199, null, this.mOnBatteryTimeBase);
        for (int i3 = 0; i3 < 17; i3++) {
            this.mPhoneDataConnectionsTimer[i3] = new StopwatchTimer(null, (-300) - i3, null, this.mOnBatteryTimeBase);
        }
        for (int i4 = 0; i4 < 4; i4++) {
            this.mNetworkByteActivityCounters[i4] = new LongSamplingCounter(this.mOnBatteryTimeBase);
            this.mNetworkPacketActivityCounters[i4] = new LongSamplingCounter(this.mOnBatteryTimeBase);
        }
        this.mMobileRadioActiveTimer = new StopwatchTimer(null, -400, null, this.mOnBatteryTimeBase);
        this.mMobileRadioActivePerAppTimer = new StopwatchTimer(null, -401, null, this.mOnBatteryTimeBase);
        this.mMobileRadioActiveAdjustedTime = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mMobileRadioActiveUnknownTime = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mMobileRadioActiveUnknownCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mWifiOnTimer = new StopwatchTimer(null, -4, null, this.mOnBatteryTimeBase);
        this.mGlobalWifiRunningTimer = new StopwatchTimer(null, -5, null, this.mOnBatteryTimeBase);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5] = new StopwatchTimer(null, (-600) - i5, null, this.mOnBatteryTimeBase);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6] = new StopwatchTimer(null, (-700) - i6, null, this.mOnBatteryTimeBase);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7] = new StopwatchTimer(null, (-800) - i7, null, this.mOnBatteryTimeBase);
        }
        this.mBluetoothOnTimer = new StopwatchTimer(null, -6, null, this.mOnBatteryTimeBase);
        for (int i8 = 0; i8 < 4; i8++) {
            this.mBluetoothStateTimer[i8] = new StopwatchTimer(null, (-500) - i8, null, this.mOnBatteryTimeBase);
        }
        this.mAudioOnTimer = new StopwatchTimer(null, -7, null, this.mOnBatteryTimeBase);
        this.mVideoOnTimer = new StopwatchTimer(null, -8, null, this.mOnBatteryTimeBase);
        this.mFlashlightOnTimer = new StopwatchTimer(null, -9, null, this.mOnBatteryTimeBase);
        this.mOnBatteryInternal = false;
        this.mOnBattery = false;
        long uptime = SystemClock.uptimeMillis() * 1000;
        long realtime = SystemClock.elapsedRealtime() * 1000;
        initTimes(uptime, realtime);
        String str = Build.ID;
        this.mEndPlatformVersion = str;
        this.mStartPlatformVersion = str;
        this.mDischargeStartLevel = 0;
        this.mDischargeUnplugLevel = 0;
        this.mDischargePlugLevel = -1;
        this.mDischargeCurrentLevel = 0;
        this.mCurrentBatteryLevel = 0;
        initDischarge();
        clearHistoryLocked();
    }

    public BatteryStatsImpl(Parcel p) {
        this.mIsolatedUids = new SparseIntArray();
        this.mUidStats = new SparseArray<>();
        this.mPartialTimers = new ArrayList<>();
        this.mFullTimers = new ArrayList<>();
        this.mWindowTimers = new ArrayList<>();
        this.mSensorTimers = new SparseArray<>();
        this.mWifiRunningTimers = new ArrayList<>();
        this.mFullWifiLockTimers = new ArrayList<>();
        this.mWifiMulticastTimers = new ArrayList<>();
        this.mWifiScanTimers = new ArrayList<>();
        this.mWifiBatchedScanTimers = new SparseArray<>();
        this.mAudioTurnedOnTimers = new ArrayList<>();
        this.mVideoTurnedOnTimers = new ArrayList<>();
        this.mLastPartialTimers = new ArrayList<>();
        this.mOnBatteryTimeBase = new TimeBase();
        this.mOnBatteryScreenOffTimeBase = new TimeBase();
        this.mActiveEvents = new BatteryStats.HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryBuffer = Parcel.obtain();
        this.mHistoryLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryLastLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryReadTmp = new BatteryStats.HistoryItem();
        this.mHistoryAddTmp = new BatteryStats.HistoryItem();
        this.mHistoryTagPool = new HashMap<>();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mHistoryOverflow = false;
        this.mLastHistoryElapsedRealtime = 0L;
        this.mTrackRunningHistoryElapsedRealtime = 0L;
        this.mTrackRunningHistoryUptime = 0L;
        this.mHistoryCur = new BatteryStats.HistoryItem();
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[17];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[4];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[4];
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mBluetoothState = -1;
        this.mBluetoothStateTimer = new StopwatchTimer[4];
        this.mMobileRadioPowerState = DataConnectionRealTimeInfo.DC_POWER_STATE_LOW;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepDurations = new long[200];
        this.mChargeStepDurations = new long[200];
        this.mLastWriteTime = 0L;
        this.mBluetoothPingStart = -1;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mKernelWakelockStats = new HashMap<>();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0L;
        this.mWakeupReasonStats = new HashMap<>();
        this.mProcWakelocksName = new String[3];
        this.mProcWakelocksData = new long[3];
        this.mProcWakelockFileStats = new HashMap();
        this.mNetworkStatsFactory = new NetworkStatsFactory();
        this.mCurMobileSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mLastMobileSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mCurWifiSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mLastWifiSnapshot = new NetworkStats(SystemClock.elapsedRealtime(), 50);
        this.mTmpNetworkStatsEntry = new NetworkStats.Entry();
        this.mMobileIfaces = new String[0];
        this.mWifiIfaces = new String[0];
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        this.mFile = null;
        this.mCheckinFile = null;
        this.mHandler = null;
        clearHistoryLocked();
        readFromParcel(p);
    }

    public void setCallback(BatteryCallback cb) {
        this.mCallback = cb;
    }

    public void setNumSpeedSteps(int steps) {
        if (sNumSpeedSteps == 0) {
            sNumSpeedSteps = steps;
        }
    }

    public void setRadioScanningTimeout(long timeout) {
        if (this.mPhoneSignalScanningTimer != null) {
            this.mPhoneSignalScanningTimer.setTimeout(timeout);
        }
    }

    @Override
    public boolean startIteratingOldHistoryLocked() {
        BatteryStats.HistoryItem historyItem = this.mHistory;
        this.mHistoryIterator = historyItem;
        if (historyItem == null) {
            return false;
        }
        this.mHistoryBuffer.setDataPosition(0);
        this.mHistoryReadTmp.clear();
        this.mReadOverflow = false;
        this.mIteratingHistory = true;
        return true;
    }

    @Override
    public boolean getNextOldHistoryLocked(BatteryStats.HistoryItem out) {
        boolean end = this.mHistoryBuffer.dataPosition() >= this.mHistoryBuffer.dataSize();
        if (!end) {
            readHistoryDelta(this.mHistoryBuffer, this.mHistoryReadTmp);
            this.mReadOverflow = (this.mHistoryReadTmp.cmd == 6) | this.mReadOverflow;
        }
        BatteryStats.HistoryItem cur = this.mHistoryIterator;
        if (cur == null) {
            if (this.mReadOverflow || end) {
                return false;
            }
            Slog.w(TAG, "Old history ends before new history!");
            return false;
        }
        out.setTo(cur);
        this.mHistoryIterator = cur.next;
        if (!this.mReadOverflow) {
            if (end) {
                Slog.w(TAG, "New history ends before old history!");
            } else if (!out.same(this.mHistoryReadTmp)) {
                PrintWriter pw = new FastPrintWriter(new LogWriter(5, TAG));
                pw.println("Histories differ!");
                pw.println("Old history:");
                new BatteryStats.HistoryPrinter().printNextItem(pw, out, 0L, false, true);
                pw.println("New history:");
                new BatteryStats.HistoryPrinter().printNextItem(pw, this.mHistoryReadTmp, 0L, false, true);
                pw.flush();
            }
        }
        return true;
    }

    @Override
    public void finishIteratingOldHistoryLocked() {
        this.mIteratingHistory = false;
        this.mHistoryBuffer.setDataPosition(this.mHistoryBuffer.dataSize());
        this.mHistoryIterator = null;
    }

    @Override
    public int getHistoryTotalSize() {
        return 262144;
    }

    @Override
    public int getHistoryUsedSize() {
        return this.mHistoryBuffer.dataSize();
    }

    @Override
    public boolean startIteratingHistoryLocked() {
        if (this.mHistoryBuffer.dataSize() <= 0) {
            return false;
        }
        this.mHistoryBuffer.setDataPosition(0);
        this.mReadOverflow = false;
        this.mIteratingHistory = true;
        this.mReadHistoryStrings = new String[this.mHistoryTagPool.size()];
        this.mReadHistoryUids = new int[this.mHistoryTagPool.size()];
        this.mReadHistoryChars = 0;
        for (Map.Entry<BatteryStats.HistoryTag, Integer> ent : this.mHistoryTagPool.entrySet()) {
            BatteryStats.HistoryTag tag = ent.getKey();
            int idx = ent.getValue().intValue();
            this.mReadHistoryStrings[idx] = tag.string;
            this.mReadHistoryUids[idx] = tag.uid;
            this.mReadHistoryChars += tag.string.length() + 1;
        }
        return true;
    }

    @Override
    public int getHistoryStringPoolSize() {
        return this.mReadHistoryStrings.length;
    }

    @Override
    public int getHistoryStringPoolBytes() {
        return (this.mReadHistoryStrings.length * 12) + (this.mReadHistoryChars * 2);
    }

    @Override
    public String getHistoryTagPoolString(int index) {
        return this.mReadHistoryStrings[index];
    }

    @Override
    public int getHistoryTagPoolUid(int index) {
        return this.mReadHistoryUids[index];
    }

    @Override
    public boolean getNextHistoryLocked(BatteryStats.HistoryItem out) {
        int pos = this.mHistoryBuffer.dataPosition();
        if (pos == 0) {
            out.clear();
        }
        boolean end = pos >= this.mHistoryBuffer.dataSize();
        if (end) {
            return false;
        }
        long lastRealtime = out.time;
        long lastWalltime = out.currentTime;
        readHistoryDelta(this.mHistoryBuffer, out);
        if (out.cmd != 5 && out.cmd != 7 && lastWalltime != 0) {
            out.currentTime = (out.time - lastRealtime) + lastWalltime;
        }
        return true;
    }

    @Override
    public void finishIteratingHistoryLocked() {
        this.mIteratingHistory = false;
        this.mHistoryBuffer.setDataPosition(this.mHistoryBuffer.dataSize());
        this.mReadHistoryStrings = null;
    }

    @Override
    public long getHistoryBaseTime() {
        return this.mHistoryBaseTime;
    }

    @Override
    public int getStartCount() {
        return this.mStartCount;
    }

    public boolean isOnBattery() {
        return this.mOnBattery;
    }

    public boolean isScreenOn() {
        return this.mScreenState == 2;
    }

    void initTimes(long uptime, long realtime) {
        this.mStartClockTime = System.currentTimeMillis();
        this.mOnBatteryTimeBase.init(uptime, realtime);
        this.mOnBatteryScreenOffTimeBase.init(uptime, realtime);
        this.mRealtime = 0L;
        this.mUptime = 0L;
        this.mRealtimeStart = realtime;
        this.mUptimeStart = uptime;
    }

    void initDischarge() {
        this.mLowDischargeAmountSinceCharge = 0;
        this.mHighDischargeAmountSinceCharge = 0;
        this.mDischargeAmountScreenOn = 0;
        this.mDischargeAmountScreenOnSinceCharge = 0;
        this.mDischargeAmountScreenOff = 0;
        this.mDischargeAmountScreenOffSinceCharge = 0;
        this.mLastDischargeStepTime = -1L;
        this.mNumDischargeStepDurations = 0;
        this.mLastChargeStepTime = -1L;
        this.mNumChargeStepDurations = 0;
    }

    public void resetAllStatsCmdLocked() {
        resetAllStatsLocked();
        long mSecUptime = SystemClock.uptimeMillis();
        long uptime = mSecUptime * 1000;
        long mSecRealtime = SystemClock.elapsedRealtime();
        long realtime = mSecRealtime * 1000;
        this.mDischargeStartLevel = this.mHistoryCur.batteryLevel;
        pullPendingStateUpdatesLocked();
        addHistoryRecordLocked(mSecRealtime, mSecUptime);
        byte b = this.mHistoryCur.batteryLevel;
        this.mCurrentBatteryLevel = b;
        this.mDischargePlugLevel = b;
        this.mDischargeUnplugLevel = b;
        this.mDischargeCurrentLevel = b;
        this.mOnBatteryTimeBase.reset(uptime, realtime);
        this.mOnBatteryScreenOffTimeBase.reset(uptime, realtime);
        if ((this.mHistoryCur.states & 524288) == 0) {
            if (this.mScreenState == 2) {
                this.mDischargeScreenOnUnplugLevel = this.mHistoryCur.batteryLevel;
                this.mDischargeScreenOffUnplugLevel = 0;
            } else {
                this.mDischargeScreenOnUnplugLevel = 0;
                this.mDischargeScreenOffUnplugLevel = this.mHistoryCur.batteryLevel;
            }
            this.mDischargeAmountScreenOn = 0;
            this.mDischargeAmountScreenOff = 0;
        }
        initActiveHistoryEventsLocked(mSecRealtime, mSecUptime);
    }

    private void resetAllStatsLocked() {
        this.mStartCount = 0;
        initTimes(SystemClock.uptimeMillis() * 1000, SystemClock.elapsedRealtime() * 1000);
        this.mScreenOnTimer.reset(false);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].reset(false);
        }
        this.mInteractiveTimer.reset(false);
        this.mLowPowerModeEnabledTimer.reset(false);
        this.mPhoneOnTimer.reset(false);
        this.mAudioOnTimer.reset(false);
        this.mVideoOnTimer.reset(false);
        this.mFlashlightOnTimer.reset(false);
        for (int i2 = 0; i2 < 5; i2++) {
            this.mPhoneSignalStrengthsTimer[i2].reset(false);
        }
        this.mPhoneSignalScanningTimer.reset(false);
        for (int i3 = 0; i3 < 17; i3++) {
            this.mPhoneDataConnectionsTimer[i3].reset(false);
        }
        for (int i4 = 0; i4 < 4; i4++) {
            this.mNetworkByteActivityCounters[i4].reset(false);
            this.mNetworkPacketActivityCounters[i4].reset(false);
        }
        this.mMobileRadioActiveTimer.reset(false);
        this.mMobileRadioActivePerAppTimer.reset(false);
        this.mMobileRadioActiveAdjustedTime.reset(false);
        this.mMobileRadioActiveUnknownTime.reset(false);
        this.mMobileRadioActiveUnknownCount.reset(false);
        this.mWifiOnTimer.reset(false);
        this.mGlobalWifiRunningTimer.reset(false);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5].reset(false);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6].reset(false);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7].reset(false);
        }
        this.mBluetoothOnTimer.reset(false);
        for (int i8 = 0; i8 < 4; i8++) {
            this.mBluetoothStateTimer[i8].reset(false);
        }
        this.mUnpluggedNumConnectivityChange = 0;
        this.mLoadedNumConnectivityChange = 0;
        this.mNumConnectivityChange = 0;
        int i9 = 0;
        while (i9 < this.mUidStats.size()) {
            if (this.mUidStats.valueAt(i9).reset()) {
                this.mUidStats.remove(this.mUidStats.keyAt(i9));
                i9--;
            }
            i9++;
        }
        if (this.mKernelWakelockStats.size() > 0) {
            for (SamplingTimer timer : this.mKernelWakelockStats.values()) {
                this.mOnBatteryScreenOffTimeBase.remove(timer);
            }
            this.mKernelWakelockStats.clear();
        }
        if (this.mWakeupReasonStats.size() > 0) {
            for (SamplingTimer timer2 : this.mWakeupReasonStats.values()) {
                this.mOnBatteryTimeBase.remove(timer2);
            }
            this.mWakeupReasonStats.clear();
        }
        initDischarge();
        clearHistoryLocked();
    }

    private void initActiveHistoryEventsLocked(long elapsedRealtimeMs, long uptimeMs) {
        HashMap<String, SparseIntArray> active;
        for (int i = 0; i < 10; i++) {
            if ((this.mRecordAllHistory || i != 1) && (active = this.mActiveEvents.getStateForEvent(i)) != null) {
                for (Map.Entry<String, SparseIntArray> ent : active.entrySet()) {
                    SparseIntArray uids = ent.getValue();
                    for (int j = 0; j < uids.size(); j++) {
                        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, i, ent.getKey(), uids.keyAt(j));
                    }
                }
            }
        }
    }

    void updateDischargeScreenLevelsLocked(boolean oldScreenOn, boolean newScreenOn) {
        if (oldScreenOn) {
            int diff = this.mDischargeScreenOnUnplugLevel - this.mDischargeCurrentLevel;
            if (diff > 0) {
                this.mDischargeAmountScreenOn += diff;
                this.mDischargeAmountScreenOnSinceCharge += diff;
            }
        } else {
            int diff2 = this.mDischargeScreenOffUnplugLevel - this.mDischargeCurrentLevel;
            if (diff2 > 0) {
                this.mDischargeAmountScreenOff += diff2;
                this.mDischargeAmountScreenOffSinceCharge += diff2;
            }
        }
        if (newScreenOn) {
            this.mDischargeScreenOnUnplugLevel = this.mDischargeCurrentLevel;
            this.mDischargeScreenOffUnplugLevel = 0;
        } else {
            this.mDischargeScreenOnUnplugLevel = 0;
            this.mDischargeScreenOffUnplugLevel = this.mDischargeCurrentLevel;
        }
    }

    public void pullPendingStateUpdatesLocked() {
        updateKernelWakelocksLocked();
        updateNetworkActivityLocked(65535, SystemClock.elapsedRealtime());
        if (this.mOnBatteryInternal) {
            boolean screenOn = this.mScreenState == 2;
            updateDischargeScreenLevelsLocked(screenOn, screenOn);
        }
    }

    void setOnBatteryLocked(long mSecRealtime, long mSecUptime, boolean onBattery, int oldStatus, int level) {
        boolean doWrite = false;
        Message m = this.mHandler.obtainMessage(2);
        m.arg1 = onBattery ? 1 : 0;
        this.mHandler.sendMessage(m);
        long uptime = mSecUptime * 1000;
        long realtime = mSecRealtime * 1000;
        boolean screenOn = this.mScreenState == 2;
        if (onBattery) {
            boolean reset = false;
            if (!this.mNoAutoReset && (oldStatus == 5 || level >= 90 || ((this.mDischargeCurrentLevel < 20 && level >= 80) || (getHighDischargeAmountSinceCharge() >= 200 && this.mHistoryBuffer.dataSize() >= 262144)))) {
                Slog.i(TAG, "Resetting battery stats: level=" + level + " status=" + oldStatus + " dischargeLevel=" + this.mDischargeCurrentLevel + " lowAmount=" + getLowDischargeAmountSinceCharge() + " highAmount=" + getHighDischargeAmountSinceCharge());
                if (getLowDischargeAmountSinceCharge() >= 20) {
                    final Parcel parcel = Parcel.obtain();
                    writeSummaryToParcel(parcel, true);
                    BackgroundThread.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (BatteryStatsImpl.this.mCheckinFile) {
                                FileOutputStream stream = null;
                                try {
                                    try {
                                        stream = BatteryStatsImpl.this.mCheckinFile.startWrite();
                                        stream.write(parcel.marshall());
                                        stream.flush();
                                        FileUtils.sync(stream);
                                        stream.close();
                                        BatteryStatsImpl.this.mCheckinFile.finishWrite(stream);
                                    } catch (IOException e) {
                                        Slog.w("BatteryStats", "Error writing checkin battery statistics", e);
                                        BatteryStatsImpl.this.mCheckinFile.failWrite(stream);
                                        parcel.recycle();
                                    }
                                } finally {
                                    parcel.recycle();
                                }
                            }
                        }
                    });
                }
                doWrite = true;
                resetAllStatsLocked();
                this.mDischargeStartLevel = level;
                reset = true;
                this.mNumDischargeStepDurations = 0;
            }
            this.mOnBatteryInternal = onBattery;
            this.mOnBattery = onBattery;
            this.mLastDischargeStepLevel = level;
            this.mMinDischargeStepLevel = level;
            this.mLastDischargeStepTime = -1L;
            this.mInitStepMode = this.mCurStepMode;
            this.mModStepMode = 0;
            pullPendingStateUpdatesLocked();
            this.mHistoryCur.batteryLevel = (byte) level;
            this.mHistoryCur.states &= -524289;
            if (reset) {
                this.mRecordingHistory = true;
                startRecordingHistory(mSecRealtime, mSecUptime, reset);
            }
            addHistoryRecordLocked(mSecRealtime, mSecUptime);
            this.mDischargeUnplugLevel = level;
            this.mDischargeCurrentLevel = level;
            if (screenOn) {
                this.mDischargeScreenOnUnplugLevel = level;
                this.mDischargeScreenOffUnplugLevel = 0;
            } else {
                this.mDischargeScreenOnUnplugLevel = 0;
                this.mDischargeScreenOffUnplugLevel = level;
            }
            this.mDischargeAmountScreenOn = 0;
            this.mDischargeAmountScreenOff = 0;
            updateTimeBasesLocked(true, !screenOn, uptime, realtime);
        } else {
            this.mOnBatteryInternal = onBattery;
            this.mOnBattery = onBattery;
            pullPendingStateUpdatesLocked();
            this.mHistoryCur.batteryLevel = (byte) level;
            this.mHistoryCur.states |= 524288;
            addHistoryRecordLocked(mSecRealtime, mSecUptime);
            this.mDischargePlugLevel = level;
            this.mDischargeCurrentLevel = level;
            if (level < this.mDischargeUnplugLevel) {
                this.mLowDischargeAmountSinceCharge += (this.mDischargeUnplugLevel - level) - 1;
                this.mHighDischargeAmountSinceCharge += this.mDischargeUnplugLevel - level;
            }
            updateDischargeScreenLevelsLocked(screenOn, screenOn);
            updateTimeBasesLocked(false, !screenOn, uptime, realtime);
            this.mNumChargeStepDurations = 0;
            this.mLastChargeStepLevel = level;
            this.mMaxChargeStepLevel = level;
            this.mLastChargeStepTime = -1L;
            this.mInitStepMode = this.mCurStepMode;
            this.mModStepMode = 0;
        }
        if ((doWrite || this.mLastWriteTime + DateUtils.MINUTE_IN_MILLIS < mSecRealtime) && this.mFile != null) {
            writeAsyncLocked();
        }
    }

    private void startRecordingHistory(long elapsedRealtimeMs, long uptimeMs, boolean reset) {
        this.mRecordingHistory = true;
        this.mHistoryCur.currentTime = System.currentTimeMillis();
        this.mLastRecordedClockTime = this.mHistoryCur.currentTime;
        this.mLastRecordedClockRealtime = elapsedRealtimeMs;
        addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, reset ? (byte) 7 : (byte) 5, this.mHistoryCur);
        this.mHistoryCur.currentTime = 0L;
        if (reset) {
            initActiveHistoryEventsLocked(elapsedRealtimeMs, uptimeMs);
        }
    }

    private void recordCurrentTimeChangeLocked(long currentTime, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mRecordingHistory) {
            this.mHistoryCur.currentTime = currentTime;
            this.mLastRecordedClockTime = currentTime;
            this.mLastRecordedClockRealtime = elapsedRealtimeMs;
            addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 5, this.mHistoryCur);
            this.mHistoryCur.currentTime = 0L;
        }
    }

    private void recordShutdownLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mRecordingHistory) {
            this.mHistoryCur.currentTime = System.currentTimeMillis();
            this.mLastRecordedClockTime = this.mHistoryCur.currentTime;
            this.mLastRecordedClockRealtime = elapsedRealtimeMs;
            addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 8, this.mHistoryCur);
            this.mHistoryCur.currentTime = 0L;
        }
    }

    private static int addLevelSteps(long[] steps, int stepCount, long lastStepTime, int numStepLevels, long modeBits, long elapsedRealtime) {
        if (lastStepTime >= 0 && numStepLevels > 0) {
            long duration = elapsedRealtime - lastStepTime;
            for (int i = 0; i < numStepLevels; i++) {
                System.arraycopy(steps, 0, steps, 1, steps.length - 1);
                long thisDuration = duration / ((long) (numStepLevels - i));
                duration -= thisDuration;
                if (thisDuration > BatteryStats.STEP_LEVEL_TIME_MASK) {
                    thisDuration = BatteryStats.STEP_LEVEL_TIME_MASK;
                }
                steps[0] = thisDuration | modeBits;
            }
            int stepCount2 = stepCount + numStepLevels;
            if (stepCount2 > steps.length) {
                return steps.length;
            }
            return stepCount2;
        }
        return stepCount;
    }

    public void setBatteryState(int status, int health, int plugType, int level, int temp, int volt) {
        synchronized (this) {
            boolean onBattery = plugType == 0;
            long uptime = SystemClock.uptimeMillis();
            long elapsedRealtime = SystemClock.elapsedRealtime();
            int oldStatus = this.mHistoryCur.batteryStatus;
            if (!this.mHaveBatteryLevel) {
                this.mHaveBatteryLevel = true;
                if (onBattery == this.mOnBattery) {
                    if (onBattery) {
                        this.mHistoryCur.states &= -524289;
                    } else {
                        this.mHistoryCur.states |= 524288;
                    }
                }
                oldStatus = status;
            }
            if (onBattery) {
                this.mDischargeCurrentLevel = level;
                if (!this.mRecordingHistory) {
                    this.mRecordingHistory = true;
                    startRecordingHistory(elapsedRealtime, uptime, true);
                }
            } else if (level < 96 && !this.mRecordingHistory) {
                this.mRecordingHistory = true;
                startRecordingHistory(elapsedRealtime, uptime, true);
            }
            this.mCurrentBatteryLevel = level;
            if (this.mDischargePlugLevel < 0) {
                this.mDischargePlugLevel = level;
            }
            if (onBattery != this.mOnBattery) {
                this.mHistoryCur.batteryLevel = (byte) level;
                this.mHistoryCur.batteryStatus = (byte) status;
                this.mHistoryCur.batteryHealth = (byte) health;
                this.mHistoryCur.batteryPlugType = (byte) plugType;
                this.mHistoryCur.batteryTemperature = (short) temp;
                this.mHistoryCur.batteryVoltage = (char) volt;
                setOnBatteryLocked(elapsedRealtime, uptime, onBattery, oldStatus, level);
            } else {
                boolean changed = false;
                if (this.mHistoryCur.batteryLevel != level) {
                    this.mHistoryCur.batteryLevel = (byte) level;
                    changed = true;
                }
                if (this.mHistoryCur.batteryStatus != status) {
                    this.mHistoryCur.batteryStatus = (byte) status;
                    changed = true;
                }
                if (this.mHistoryCur.batteryHealth != health) {
                    this.mHistoryCur.batteryHealth = (byte) health;
                    changed = true;
                }
                if (this.mHistoryCur.batteryPlugType != plugType) {
                    this.mHistoryCur.batteryPlugType = (byte) plugType;
                    changed = true;
                }
                if (temp >= this.mHistoryCur.batteryTemperature + 10 || temp <= this.mHistoryCur.batteryTemperature - 10) {
                    this.mHistoryCur.batteryTemperature = (short) temp;
                    changed = true;
                }
                if (volt > this.mHistoryCur.batteryVoltage + 20 || volt < this.mHistoryCur.batteryVoltage - 20) {
                    this.mHistoryCur.batteryVoltage = (char) volt;
                    changed = true;
                }
                if (changed) {
                    addHistoryRecordLocked(elapsedRealtime, uptime);
                }
                long modeBits = (((long) this.mInitStepMode) << 48) | (((long) this.mModStepMode) << 56) | (((long) (level & 255)) << 40);
                if (onBattery) {
                    if (this.mLastDischargeStepLevel != level && this.mMinDischargeStepLevel > level) {
                        this.mNumDischargeStepDurations = addLevelSteps(this.mDischargeStepDurations, this.mNumDischargeStepDurations, this.mLastDischargeStepTime, this.mLastDischargeStepLevel - level, modeBits, elapsedRealtime);
                        this.mLastDischargeStepLevel = level;
                        this.mMinDischargeStepLevel = level;
                        this.mLastDischargeStepTime = elapsedRealtime;
                        this.mInitStepMode = this.mCurStepMode;
                        this.mModStepMode = 0;
                    }
                } else if (this.mLastChargeStepLevel != level && this.mMaxChargeStepLevel < level) {
                    this.mNumChargeStepDurations = addLevelSteps(this.mChargeStepDurations, this.mNumChargeStepDurations, this.mLastChargeStepTime, level - this.mLastChargeStepLevel, modeBits, elapsedRealtime);
                    this.mLastChargeStepLevel = level;
                    this.mMaxChargeStepLevel = level;
                    this.mLastChargeStepTime = elapsedRealtime;
                    this.mInitStepMode = this.mCurStepMode;
                    this.mModStepMode = 0;
                }
            }
            if (!onBattery && status == 5) {
                this.mRecordingHistory = false;
            }
        }
    }

    public void updateKernelWakelocksLocked() {
        Map<String, KernelWakelockStats> m = readKernelWakelockStats();
        if (m == null) {
            Slog.w(TAG, "Couldn't get kernel wake lock stats");
            return;
        }
        for (Map.Entry<String, KernelWakelockStats> ent : m.entrySet()) {
            String name = ent.getKey();
            KernelWakelockStats kws = ent.getValue();
            SamplingTimer kwlt = this.mKernelWakelockStats.get(name);
            if (kwlt == null) {
                kwlt = new SamplingTimer(this.mOnBatteryScreenOffTimeBase, true);
                this.mKernelWakelockStats.put(name, kwlt);
            }
            kwlt.updateCurrentReportedCount(kws.mCount);
            kwlt.updateCurrentReportedTotalTime(kws.mTotalTime);
            kwlt.setUpdateVersion(sKernelWakelockUpdateVersion);
        }
        if (m.size() != this.mKernelWakelockStats.size()) {
            Iterator<Map.Entry<String, SamplingTimer>> it = this.mKernelWakelockStats.entrySet().iterator();
            while (it.hasNext()) {
                SamplingTimer st = it.next().getValue();
                if (st.getUpdateVersion() != sKernelWakelockUpdateVersion) {
                    st.setStale();
                }
            }
        }
    }

    private void updateNetworkActivityLocked(int which, long elapsedRealtimeMs) {
        if (SystemProperties.getBoolean(NetworkManagementSocketTagger.PROP_QTAGUID_ENABLED, false)) {
            if ((which & 1) != 0 && this.mMobileIfaces.length > 0) {
                NetworkStats last = this.mCurMobileSnapshot;
                try {
                    NetworkStats snapshot = this.mNetworkStatsFactory.readNetworkStatsDetail(-1, this.mMobileIfaces, 0, this.mLastMobileSnapshot);
                    this.mCurMobileSnapshot = snapshot;
                    this.mLastMobileSnapshot = last;
                    if (this.mOnBatteryInternal) {
                        NetworkStats delta = NetworkStats.subtract(snapshot, last, null, null, this.mTmpNetworkStats);
                        this.mTmpNetworkStats = delta;
                        long radioTime = this.mMobileRadioActivePerAppTimer.checkpointRunningLocked(elapsedRealtimeMs);
                        long totalPackets = delta.getTotalPackets();
                        int size = delta.size();
                        for (int i = 0; i < size; i++) {
                            NetworkStats.Entry entry = delta.getValues(i, this.mTmpNetworkStatsEntry);
                            if (entry.rxBytes != 0 && entry.txBytes != 0) {
                                Uid u = getUidStatsLocked(mapUid(entry.uid));
                                u.noteNetworkActivityLocked(0, entry.rxBytes, entry.rxPackets);
                                u.noteNetworkActivityLocked(1, entry.txBytes, entry.txPackets);
                                if (radioTime > 0) {
                                    long appPackets = entry.rxPackets + entry.txPackets;
                                    long appRadioTime = (radioTime * appPackets) / totalPackets;
                                    u.noteMobileRadioActiveTimeLocked(appRadioTime);
                                    radioTime -= appRadioTime;
                                    totalPackets -= appPackets;
                                }
                                this.mNetworkByteActivityCounters[0].addCountLocked(entry.rxBytes);
                                this.mNetworkByteActivityCounters[1].addCountLocked(entry.txBytes);
                                this.mNetworkPacketActivityCounters[0].addCountLocked(entry.rxPackets);
                                this.mNetworkPacketActivityCounters[1].addCountLocked(entry.txPackets);
                            }
                        }
                        if (radioTime > 0) {
                            this.mMobileRadioActiveUnknownTime.addCountLocked(radioTime);
                            this.mMobileRadioActiveUnknownCount.addCountLocked(1L);
                        }
                    }
                } catch (IOException e) {
                    Log.wtf(TAG, "Failed to read mobile network stats", e);
                    return;
                }
            }
            if ((which & 2) != 0 && this.mWifiIfaces.length > 0) {
                NetworkStats last2 = this.mCurWifiSnapshot;
                try {
                    NetworkStats snapshot2 = this.mNetworkStatsFactory.readNetworkStatsDetail(-1, this.mWifiIfaces, 0, this.mLastWifiSnapshot);
                    this.mCurWifiSnapshot = snapshot2;
                    this.mLastWifiSnapshot = last2;
                    if (this.mOnBatteryInternal) {
                        NetworkStats delta2 = NetworkStats.subtract(snapshot2, last2, null, null, this.mTmpNetworkStats);
                        this.mTmpNetworkStats = delta2;
                        int size2 = delta2.size();
                        for (int i2 = 0; i2 < size2; i2++) {
                            NetworkStats.Entry entry2 = delta2.getValues(i2, this.mTmpNetworkStatsEntry);
                            if (entry2.rxBytes != 0 && entry2.txBytes != 0) {
                                Uid u2 = getUidStatsLocked(mapUid(entry2.uid));
                                u2.noteNetworkActivityLocked(2, entry2.rxBytes, entry2.rxPackets);
                                u2.noteNetworkActivityLocked(3, entry2.txBytes, entry2.txPackets);
                                this.mNetworkByteActivityCounters[2].addCountLocked(entry2.rxBytes);
                                this.mNetworkByteActivityCounters[3].addCountLocked(entry2.txBytes);
                                this.mNetworkPacketActivityCounters[2].addCountLocked(entry2.rxPackets);
                                this.mNetworkPacketActivityCounters[3].addCountLocked(entry2.txPackets);
                            }
                        }
                    }
                } catch (IOException e2) {
                    Log.wtf(TAG, "Failed to read wifi network stats", e2);
                }
            }
        }
    }

    public long getAwakeTimeBattery() {
        return computeBatteryUptime(getBatteryUptimeLocked(), 1);
    }

    public long getAwakeTimePlugged() {
        return (SystemClock.uptimeMillis() * 1000) - getAwakeTimeBattery();
    }

    @Override
    public long computeUptime(long curTime, int which) {
        switch (which) {
            case 0:
                return this.mUptime + (curTime - this.mUptimeStart);
            case 1:
                return curTime - this.mUptimeStart;
            case 2:
                return curTime - this.mOnBatteryTimeBase.getUptimeStart();
            default:
                return 0L;
        }
    }

    @Override
    public long computeRealtime(long curTime, int which) {
        switch (which) {
            case 0:
                return this.mRealtime + (curTime - this.mRealtimeStart);
            case 1:
                return curTime - this.mRealtimeStart;
            case 2:
                return curTime - this.mOnBatteryTimeBase.getRealtimeStart();
            default:
                return 0L;
        }
    }

    @Override
    public long computeBatteryUptime(long curTime, int which) {
        return this.mOnBatteryTimeBase.computeUptime(curTime, which);
    }

    @Override
    public long computeBatteryRealtime(long curTime, int which) {
        return this.mOnBatteryTimeBase.computeRealtime(curTime, which);
    }

    @Override
    public long computeBatteryScreenOffUptime(long curTime, int which) {
        return this.mOnBatteryScreenOffTimeBase.computeUptime(curTime, which);
    }

    @Override
    public long computeBatteryScreenOffRealtime(long curTime, int which) {
        return this.mOnBatteryScreenOffTimeBase.computeRealtime(curTime, which);
    }

    private long computeTimePerLevel(long[] steps, int numSteps) {
        if (numSteps <= 0) {
            return -1L;
        }
        long total = 0;
        for (int i = 0; i < numSteps; i++) {
            total += steps[i] & BatteryStats.STEP_LEVEL_TIME_MASK;
        }
        return total / ((long) numSteps);
    }

    @Override
    public long computeBatteryTimeRemaining(long curTime) {
        if (!this.mOnBattery || this.mNumDischargeStepDurations < 1) {
            return -1L;
        }
        long msPerLevel = computeTimePerLevel(this.mDischargeStepDurations, this.mNumDischargeStepDurations);
        if (msPerLevel > 0) {
            return ((long) this.mCurrentBatteryLevel) * msPerLevel * 1000;
        }
        return -1L;
    }

    @Override
    public int getNumDischargeStepDurations() {
        return this.mNumDischargeStepDurations;
    }

    @Override
    public long[] getDischargeStepDurationsArray() {
        return this.mDischargeStepDurations;
    }

    @Override
    public long computeChargeTimeRemaining(long curTime) {
        if (this.mOnBattery || this.mNumChargeStepDurations < 1) {
            return -1L;
        }
        long msPerLevel = computeTimePerLevel(this.mChargeStepDurations, this.mNumChargeStepDurations);
        if (msPerLevel > 0) {
            return ((long) (100 - this.mCurrentBatteryLevel)) * msPerLevel * 1000;
        }
        return -1L;
    }

    @Override
    public int getNumChargeStepDurations() {
        return this.mNumChargeStepDurations;
    }

    @Override
    public long[] getChargeStepDurationsArray() {
        return this.mChargeStepDurations;
    }

    long getBatteryUptimeLocked() {
        return this.mOnBatteryTimeBase.getUptime(SystemClock.uptimeMillis() * 1000);
    }

    @Override
    public long getBatteryUptime(long curTime) {
        return this.mOnBatteryTimeBase.getUptime(curTime);
    }

    @Override
    public long getBatteryRealtime(long curTime) {
        return this.mOnBatteryTimeBase.getRealtime(curTime);
    }

    @Override
    public int getDischargeStartLevel() {
        int dischargeStartLevelLocked;
        synchronized (this) {
            dischargeStartLevelLocked = getDischargeStartLevelLocked();
        }
        return dischargeStartLevelLocked;
    }

    public int getDischargeStartLevelLocked() {
        return this.mDischargeUnplugLevel;
    }

    @Override
    public int getDischargeCurrentLevel() {
        int dischargeCurrentLevelLocked;
        synchronized (this) {
            dischargeCurrentLevelLocked = getDischargeCurrentLevelLocked();
        }
        return dischargeCurrentLevelLocked;
    }

    public int getDischargeCurrentLevelLocked() {
        return this.mDischargeCurrentLevel;
    }

    @Override
    public int getLowDischargeAmountSinceCharge() {
        int val;
        synchronized (this) {
            val = this.mLowDischargeAmountSinceCharge;
            if (this.mOnBattery && this.mDischargeCurrentLevel < this.mDischargeUnplugLevel) {
                val += (this.mDischargeUnplugLevel - this.mDischargeCurrentLevel) - 1;
            }
        }
        return val;
    }

    @Override
    public int getHighDischargeAmountSinceCharge() {
        int val;
        synchronized (this) {
            val = this.mHighDischargeAmountSinceCharge;
            if (this.mOnBattery && this.mDischargeCurrentLevel < this.mDischargeUnplugLevel) {
                val += this.mDischargeUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    @Override
    public int getDischargeAmount(int which) {
        int dischargeAmount = which == 0 ? getHighDischargeAmountSinceCharge() : getDischargeStartLevel() - getDischargeCurrentLevel();
        if (dischargeAmount < 0) {
            return 0;
        }
        return dischargeAmount;
    }

    @Override
    public int getDischargeAmountScreenOn() {
        int val;
        synchronized (this) {
            val = this.mDischargeAmountScreenOn;
            if (this.mOnBattery && this.mScreenState == 2 && this.mDischargeCurrentLevel < this.mDischargeScreenOnUnplugLevel) {
                val += this.mDischargeScreenOnUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    @Override
    public int getDischargeAmountScreenOnSinceCharge() {
        int val;
        synchronized (this) {
            val = this.mDischargeAmountScreenOnSinceCharge;
            if (this.mOnBattery && this.mScreenState == 2 && this.mDischargeCurrentLevel < this.mDischargeScreenOnUnplugLevel) {
                val += this.mDischargeScreenOnUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    @Override
    public int getDischargeAmountScreenOff() {
        int val;
        synchronized (this) {
            val = this.mDischargeAmountScreenOff;
            if (this.mOnBattery && this.mScreenState != 2 && this.mDischargeCurrentLevel < this.mDischargeScreenOffUnplugLevel) {
                val += this.mDischargeScreenOffUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    @Override
    public int getDischargeAmountScreenOffSinceCharge() {
        int val;
        synchronized (this) {
            val = this.mDischargeAmountScreenOffSinceCharge;
            if (this.mOnBattery && this.mScreenState != 2 && this.mDischargeCurrentLevel < this.mDischargeScreenOffUnplugLevel) {
                val += this.mDischargeScreenOffUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    @Override
    public int getCpuSpeedSteps() {
        return sNumSpeedSteps;
    }

    public Uid getUidStatsLocked(int uid) {
        Uid u = this.mUidStats.get(uid);
        if (u == null) {
            Uid u2 = new Uid(uid);
            this.mUidStats.put(uid, u2);
            return u2;
        }
        return u;
    }

    public void removeUidStatsLocked(int uid) {
        this.mUidStats.remove(uid);
    }

    public Uid.Proc getProcessStatsLocked(int uid, String name) {
        Uid u = getUidStatsLocked(mapUid(uid));
        return u.getProcessStatsLocked(name);
    }

    public Uid.Pkg getPackageStatsLocked(int uid, String pkg) {
        Uid u = getUidStatsLocked(mapUid(uid));
        return u.getPackageStatsLocked(pkg);
    }

    public Uid.Pkg.Serv getServiceStatsLocked(int uid, String pkg, String name) {
        Uid u = getUidStatsLocked(mapUid(uid));
        return u.getServiceStatsLocked(pkg, name);
    }

    public void distributeWorkLocked(int which) {
        Uid wifiUid = this.mUidStats.get(1010);
        if (wifiUid != null) {
            long uSecTime = computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, which);
            for (int ip = wifiUid.mProcessStats.size() - 1; ip >= 0; ip--) {
                Uid.Proc proc = wifiUid.mProcessStats.valueAt(ip);
                long totalRunningTime = getGlobalWifiRunningTime(uSecTime, which);
                for (int i = 0; i < this.mUidStats.size(); i++) {
                    Uid uid = this.mUidStats.valueAt(i);
                    if (uid.mUid != 1010) {
                        long uidRunningTime = uid.getWifiRunningTime(uSecTime, which);
                        if (uidRunningTime > 0) {
                            Uid.Proc uidProc = uid.getProcessStatsLocked("*wifi*");
                            long time = (proc.getUserTime(which) * uidRunningTime) / totalRunningTime;
                            uidProc.mUserTime += time;
                            proc.mUserTime -= time;
                            long time2 = (proc.getSystemTime(which) * uidRunningTime) / totalRunningTime;
                            uidProc.mSystemTime += time2;
                            proc.mSystemTime -= time2;
                            long time3 = (proc.getForegroundTime(which) * uidRunningTime) / totalRunningTime;
                            uidProc.mForegroundTime += time3;
                            proc.mForegroundTime -= time3;
                            for (int sb = 0; sb < proc.mSpeedBins.length; sb++) {
                                SamplingCounter sc = proc.mSpeedBins[sb];
                                if (sc != null) {
                                    long time4 = (((long) sc.getCountLocked(which)) * uidRunningTime) / totalRunningTime;
                                    SamplingCounter uidSc = uidProc.mSpeedBins[sb];
                                    if (uidSc == null) {
                                        uidSc = new SamplingCounter(this.mOnBatteryTimeBase);
                                        uidProc.mSpeedBins[sb] = uidSc;
                                    }
                                    uidSc.mCount.addAndGet((int) time4);
                                    sc.mCount.addAndGet((int) (-time4));
                                }
                            }
                            totalRunningTime -= uidRunningTime;
                        }
                    }
                }
            }
        }
    }

    public void shutdownLocked() {
        recordShutdownLocked(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
        writeSyncLocked();
        this.mShuttingDown = true;
    }

    public void writeAsyncLocked() {
        writeLocked(false);
    }

    public void writeSyncLocked() {
        writeLocked(true);
    }

    void writeLocked(boolean sync) {
        if (this.mFile == null) {
            Slog.w("BatteryStats", "writeLocked: no file associated with this instance");
            return;
        }
        if (!this.mShuttingDown) {
            Parcel out = Parcel.obtain();
            writeSummaryToParcel(out, true);
            this.mLastWriteTime = SystemClock.elapsedRealtime();
            if (this.mPendingWrite != null) {
                this.mPendingWrite.recycle();
            }
            this.mPendingWrite = out;
            if (sync) {
                commitPendingDataToDisk();
            } else {
                BackgroundThread.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        BatteryStatsImpl.this.commitPendingDataToDisk();
                    }
                });
            }
        }
    }

    public void commitPendingDataToDisk() {
        synchronized (this) {
            Parcel next = this.mPendingWrite;
            this.mPendingWrite = null;
            if (next != null) {
                this.mWriteLock.lock();
                try {
                    FileOutputStream stream = new FileOutputStream(this.mFile.chooseForWrite());
                    stream.write(next.marshall());
                    stream.flush();
                    FileUtils.sync(stream);
                    stream.close();
                    this.mFile.commit();
                } catch (IOException e) {
                    Slog.w("BatteryStats", "Error writing battery statistics", e);
                    this.mFile.rollback();
                } finally {
                    next.recycle();
                    this.mWriteLock.unlock();
                }
            }
        }
    }

    public void readLocked() {
        if (this.mFile == null) {
            Slog.w("BatteryStats", "readLocked: no file associated with this instance");
            return;
        }
        this.mUidStats.clear();
        try {
            File file = this.mFile.chooseForRead();
            if (file.exists()) {
                FileInputStream stream = new FileInputStream(file);
                byte[] raw = BatteryStatsHelper.readFully(stream);
                Parcel in = Parcel.obtain();
                in.unmarshall(raw, 0, raw.length);
                in.setDataPosition(0);
                stream.close();
                readSummaryFromParcel(in);
            } else {
                return;
            }
        } catch (Exception e) {
            Slog.e("BatteryStats", "Error reading battery statistics", e);
        }
        this.mEndPlatformVersion = Build.ID;
        if (this.mHistoryBuffer.dataPosition() > 0) {
            this.mRecordingHistory = true;
            long elapsedRealtime = SystemClock.elapsedRealtime();
            long uptime = SystemClock.uptimeMillis();
            addHistoryBufferLocked(elapsedRealtime, uptime, (byte) 4, this.mHistoryCur);
            startRecordingHistory(elapsedRealtime, uptime, false);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    void readHistory(Parcel in, boolean andOldHistory) {
        long historyBaseTime = in.readLong();
        this.mHistoryBuffer.setDataSize(0);
        this.mHistoryBuffer.setDataPosition(0);
        this.mHistoryTagPool.clear();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        int numTags = in.readInt();
        for (int i = 0; i < numTags; i++) {
            int idx = in.readInt();
            String str = in.readString();
            int uid = in.readInt();
            BatteryStats.HistoryTag tag = new BatteryStats.HistoryTag();
            tag.string = str;
            tag.uid = uid;
            tag.poolIdx = idx;
            this.mHistoryTagPool.put(tag, Integer.valueOf(idx));
            if (idx >= this.mNextHistoryTagIdx) {
                this.mNextHistoryTagIdx = idx + 1;
            }
            this.mNumHistoryTagChars += tag.string.length() + 1;
        }
        int bufSize = in.readInt();
        int curPos = in.dataPosition();
        if (bufSize >= 983040) {
            Slog.w(TAG, "File corrupt: history data buffer too large " + bufSize);
        } else if ((bufSize & (-4)) != bufSize) {
            Slog.w(TAG, "File corrupt: history data buffer not aligned " + bufSize);
        } else {
            this.mHistoryBuffer.appendFrom(in, curPos, bufSize);
            in.setDataPosition(curPos + bufSize);
        }
        if (andOldHistory) {
            readOldHistory(in);
        }
        this.mHistoryBaseTime = historyBaseTime;
        if (this.mHistoryBaseTime > 0) {
            long oldnow = SystemClock.elapsedRealtime();
            this.mHistoryBaseTime = (this.mHistoryBaseTime - oldnow) + 1;
        }
    }

    void readOldHistory(Parcel in) {
    }

    void writeHistory(Parcel out, boolean inclData, boolean andOldHistory) {
        out.writeLong(this.mHistoryBaseTime + this.mLastHistoryElapsedRealtime);
        if (!inclData) {
            out.writeInt(0);
            out.writeInt(0);
            return;
        }
        out.writeInt(this.mHistoryTagPool.size());
        for (Map.Entry<BatteryStats.HistoryTag, Integer> ent : this.mHistoryTagPool.entrySet()) {
            BatteryStats.HistoryTag tag = ent.getKey();
            out.writeInt(ent.getValue().intValue());
            out.writeString(tag.string);
            out.writeInt(tag.uid);
        }
        out.writeInt(this.mHistoryBuffer.dataSize());
        out.appendFrom(this.mHistoryBuffer, 0, this.mHistoryBuffer.dataSize());
        if (andOldHistory) {
            writeOldHistory(out);
        }
    }

    void writeOldHistory(Parcel out) {
    }

    public void readSummaryFromParcel(Parcel in) {
        int version = in.readInt();
        if (version != 116) {
            Slog.w("BatteryStats", "readFromParcel: version got " + version + ", expected 116; erasing old stats");
            return;
        }
        readHistory(in, true);
        this.mStartCount = in.readInt();
        this.mUptime = in.readLong();
        this.mRealtime = in.readLong();
        this.mStartClockTime = in.readLong();
        this.mStartPlatformVersion = in.readString();
        this.mEndPlatformVersion = in.readString();
        this.mOnBatteryTimeBase.readSummaryFromParcel(in);
        this.mOnBatteryScreenOffTimeBase.readSummaryFromParcel(in);
        this.mDischargeUnplugLevel = in.readInt();
        this.mDischargePlugLevel = in.readInt();
        this.mDischargeCurrentLevel = in.readInt();
        this.mCurrentBatteryLevel = in.readInt();
        this.mLowDischargeAmountSinceCharge = in.readInt();
        this.mHighDischargeAmountSinceCharge = in.readInt();
        this.mDischargeAmountScreenOnSinceCharge = in.readInt();
        this.mDischargeAmountScreenOffSinceCharge = in.readInt();
        this.mNumDischargeStepDurations = in.readInt();
        in.readLongArray(this.mDischargeStepDurations);
        this.mNumChargeStepDurations = in.readInt();
        in.readLongArray(this.mChargeStepDurations);
        this.mStartCount++;
        this.mScreenState = 0;
        this.mScreenOnTimer.readSummaryFromParcelLocked(in);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].readSummaryFromParcelLocked(in);
        }
        this.mInteractive = false;
        this.mInteractiveTimer.readSummaryFromParcelLocked(in);
        this.mPhoneOn = false;
        this.mLowPowerModeEnabledTimer.readSummaryFromParcelLocked(in);
        this.mPhoneOnTimer.readSummaryFromParcelLocked(in);
        for (int i2 = 0; i2 < 5; i2++) {
            this.mPhoneSignalStrengthsTimer[i2].readSummaryFromParcelLocked(in);
        }
        this.mPhoneSignalScanningTimer.readSummaryFromParcelLocked(in);
        for (int i3 = 0; i3 < 17; i3++) {
            this.mPhoneDataConnectionsTimer[i3].readSummaryFromParcelLocked(in);
        }
        for (int i4 = 0; i4 < 4; i4++) {
            this.mNetworkByteActivityCounters[i4].readSummaryFromParcelLocked(in);
            this.mNetworkPacketActivityCounters[i4].readSummaryFromParcelLocked(in);
        }
        this.mMobileRadioPowerState = DataConnectionRealTimeInfo.DC_POWER_STATE_LOW;
        this.mMobileRadioActiveTimer.readSummaryFromParcelLocked(in);
        this.mMobileRadioActivePerAppTimer.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveAdjustedTime.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveUnknownTime.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveUnknownCount.readSummaryFromParcelLocked(in);
        this.mWifiOn = false;
        this.mWifiOnTimer.readSummaryFromParcelLocked(in);
        this.mGlobalWifiRunning = false;
        this.mGlobalWifiRunningTimer.readSummaryFromParcelLocked(in);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5].readSummaryFromParcelLocked(in);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6].readSummaryFromParcelLocked(in);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7].readSummaryFromParcelLocked(in);
        }
        this.mBluetoothOn = false;
        this.mBluetoothOnTimer.readSummaryFromParcelLocked(in);
        for (int i8 = 0; i8 < 4; i8++) {
            this.mBluetoothStateTimer[i8].readSummaryFromParcelLocked(in);
        }
        int i9 = in.readInt();
        this.mLoadedNumConnectivityChange = i9;
        this.mNumConnectivityChange = i9;
        this.mFlashlightOn = false;
        this.mFlashlightOnTimer.readSummaryFromParcelLocked(in);
        int NKW = in.readInt();
        if (NKW > 10000) {
            Slog.w(TAG, "File corrupt: too many kernel wake locks " + NKW);
            return;
        }
        for (int ikw = 0; ikw < NKW; ikw++) {
            if (in.readInt() != 0) {
                String kwltName = in.readString();
                getKernelWakelockTimerLocked(kwltName).readSummaryFromParcelLocked(in);
            }
        }
        int NWR = in.readInt();
        if (NWR > 10000) {
            Slog.w(TAG, "File corrupt: too many wakeup reasons " + NWR);
            return;
        }
        for (int iwr = 0; iwr < NWR; iwr++) {
            if (in.readInt() != 0) {
                String reasonName = in.readString();
                getWakeupReasonTimerLocked(reasonName).readSummaryFromParcelLocked(in);
            }
        }
        sNumSpeedSteps = in.readInt();
        if (sNumSpeedSteps < 0 || sNumSpeedSteps > 100) {
            throw new BadParcelableException("Bad speed steps in data: " + sNumSpeedSteps);
        }
        int NU = in.readInt();
        if (NU > 10000) {
            Slog.w(TAG, "File corrupt: too many uids " + NU);
            return;
        }
        for (int iu = 0; iu < NU; iu++) {
            int uid = in.readInt();
            Uid u = new Uid(uid);
            this.mUidStats.put(uid, u);
            u.mWifiRunning = false;
            if (in.readInt() != 0) {
                u.mWifiRunningTimer.readSummaryFromParcelLocked(in);
            }
            u.mFullWifiLockOut = false;
            if (in.readInt() != 0) {
                u.mFullWifiLockTimer.readSummaryFromParcelLocked(in);
            }
            u.mWifiScanStarted = false;
            if (in.readInt() != 0) {
                u.mWifiScanTimer.readSummaryFromParcelLocked(in);
            }
            u.mWifiBatchedScanBinStarted = -1;
            for (int i10 = 0; i10 < 5; i10++) {
                if (in.readInt() != 0) {
                    u.makeWifiBatchedScanBin(i10, null);
                    u.mWifiBatchedScanTimer[i10].readSummaryFromParcelLocked(in);
                }
            }
            u.mWifiMulticastEnabled = false;
            if (in.readInt() != 0) {
                u.mWifiMulticastTimer.readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createAudioTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createVideoTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createForegroundActivityTimerLocked().readSummaryFromParcelLocked(in);
            }
            u.mProcessState = 3;
            for (int i11 = 0; i11 < 3; i11++) {
                if (in.readInt() != 0) {
                    u.makeProcessState(i11, null);
                    u.mProcessStateTimer[i11].readSummaryFromParcelLocked(in);
                }
            }
            if (in.readInt() != 0) {
                u.createVibratorOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                if (u.mUserActivityCounters == null) {
                    u.initUserActivityLocked();
                }
                for (int i12 = 0; i12 < 3; i12++) {
                    u.mUserActivityCounters[i12].readSummaryFromParcelLocked(in);
                }
            }
            if (in.readInt() != 0) {
                if (u.mNetworkByteActivityCounters == null) {
                    u.initNetworkActivityLocked();
                }
                for (int i13 = 0; i13 < 4; i13++) {
                    u.mNetworkByteActivityCounters[i13].readSummaryFromParcelLocked(in);
                    u.mNetworkPacketActivityCounters[i13].readSummaryFromParcelLocked(in);
                }
                u.mMobileRadioActiveTime.readSummaryFromParcelLocked(in);
                u.mMobileRadioActiveCount.readSummaryFromParcelLocked(in);
            }
            int NW = in.readInt();
            if (NW > 100) {
                Slog.w(TAG, "File corrupt: too many wake locks " + NW);
                return;
            }
            for (int iw = 0; iw < NW; iw++) {
                String wlName = in.readString();
                u.readWakeSummaryFromParcelLocked(wlName, in);
            }
            int NS = in.readInt();
            if (NS > 100) {
                Slog.w(TAG, "File corrupt: too many syncs " + NS);
                return;
            }
            for (int is = 0; is < NS; is++) {
                String name = in.readString();
                u.readSyncSummaryFromParcelLocked(name, in);
            }
            int NJ = in.readInt();
            if (NJ > 100) {
                Slog.w(TAG, "File corrupt: too many job timers " + NJ);
                return;
            }
            for (int ij = 0; ij < NJ; ij++) {
                String name2 = in.readString();
                u.readJobSummaryFromParcelLocked(name2, in);
            }
            int NP = in.readInt();
            if (NP > 1000) {
                Slog.w(TAG, "File corrupt: too many sensors " + NP);
                return;
            }
            for (int is2 = 0; is2 < NP; is2++) {
                int seNumber = in.readInt();
                if (in.readInt() != 0) {
                    u.getSensorTimerLocked(seNumber, true).readSummaryFromParcelLocked(in);
                }
            }
            int NP2 = in.readInt();
            if (NP2 > 1000) {
                Slog.w(TAG, "File corrupt: too many processes " + NP2);
                return;
            }
            for (int ip = 0; ip < NP2; ip++) {
                String procName = in.readString();
                Uid.Proc p = u.getProcessStatsLocked(procName);
                long j = in.readLong();
                p.mLoadedUserTime = j;
                p.mUserTime = j;
                long j2 = in.readLong();
                p.mLoadedSystemTime = j2;
                p.mSystemTime = j2;
                long j3 = in.readLong();
                p.mLoadedForegroundTime = j3;
                p.mForegroundTime = j3;
                int i14 = in.readInt();
                p.mLoadedStarts = i14;
                p.mStarts = i14;
                int i15 = in.readInt();
                p.mLoadedNumCrashes = i15;
                p.mNumCrashes = i15;
                int i16 = in.readInt();
                p.mLoadedNumAnrs = i16;
                p.mNumAnrs = i16;
                int NSB = in.readInt();
                if (NSB > 100) {
                    Slog.w(TAG, "File corrupt: too many speed bins " + NSB);
                    return;
                }
                p.mSpeedBins = new SamplingCounter[NSB];
                for (int i17 = 0; i17 < NSB; i17++) {
                    if (in.readInt() != 0) {
                        p.mSpeedBins[i17] = new SamplingCounter(this.mOnBatteryTimeBase);
                        p.mSpeedBins[i17].readSummaryFromParcelLocked(in);
                    }
                }
                if (!p.readExcessivePowerFromParcelLocked(in)) {
                    return;
                }
            }
            int NP3 = in.readInt();
            if (NP3 > 10000) {
                Slog.w(TAG, "File corrupt: too many packages " + NP3);
                return;
            }
            for (int ip2 = 0; ip2 < NP3; ip2++) {
                String pkgName = in.readString();
                Uid.Pkg p2 = u.getPackageStatsLocked(pkgName);
                int i18 = in.readInt();
                p2.mLoadedWakeups = i18;
                p2.mWakeups = i18;
                int NS2 = in.readInt();
                if (NS2 > 1000) {
                    Slog.w(TAG, "File corrupt: too many services " + NS2);
                    return;
                }
                for (int is3 = 0; is3 < NS2; is3++) {
                    String servName = in.readString();
                    Uid.Pkg.Serv s = u.getServiceStatsLocked(pkgName, servName);
                    long j4 = in.readLong();
                    s.mLoadedStartTime = j4;
                    s.mStartTime = j4;
                    int i19 = in.readInt();
                    s.mLoadedStarts = i19;
                    s.mStarts = i19;
                    int i20 = in.readInt();
                    s.mLoadedLaunches = i20;
                    s.mLaunches = i20;
                }
            }
        }
    }

    public void writeSummaryToParcel(Parcel out, boolean inclHistory) {
        pullPendingStateUpdatesLocked();
        long startClockTime = getStartClockTime();
        long NOW_SYS = SystemClock.uptimeMillis() * 1000;
        long NOWREAL_SYS = SystemClock.elapsedRealtime() * 1000;
        out.writeInt(116);
        writeHistory(out, inclHistory, true);
        out.writeInt(this.mStartCount);
        out.writeLong(computeUptime(NOW_SYS, 0));
        out.writeLong(computeRealtime(NOWREAL_SYS, 0));
        out.writeLong(startClockTime);
        out.writeString(this.mStartPlatformVersion);
        out.writeString(this.mEndPlatformVersion);
        this.mOnBatteryTimeBase.writeSummaryToParcel(out, NOW_SYS, NOWREAL_SYS);
        this.mOnBatteryScreenOffTimeBase.writeSummaryToParcel(out, NOW_SYS, NOWREAL_SYS);
        out.writeInt(this.mDischargeUnplugLevel);
        out.writeInt(this.mDischargePlugLevel);
        out.writeInt(this.mDischargeCurrentLevel);
        out.writeInt(this.mCurrentBatteryLevel);
        out.writeInt(getLowDischargeAmountSinceCharge());
        out.writeInt(getHighDischargeAmountSinceCharge());
        out.writeInt(getDischargeAmountScreenOnSinceCharge());
        out.writeInt(getDischargeAmountScreenOffSinceCharge());
        out.writeInt(this.mNumDischargeStepDurations);
        out.writeLongArray(this.mDischargeStepDurations);
        out.writeInt(this.mNumChargeStepDurations);
        out.writeLongArray(this.mChargeStepDurations);
        this.mScreenOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        this.mInteractiveTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mLowPowerModeEnabledTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mPhoneOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (int i2 = 0; i2 < 5; i2++) {
            this.mPhoneSignalStrengthsTimer[i2].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        this.mPhoneSignalScanningTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (int i3 = 0; i3 < 17; i3++) {
            this.mPhoneDataConnectionsTimer[i3].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        for (int i4 = 0; i4 < 4; i4++) {
            this.mNetworkByteActivityCounters[i4].writeSummaryFromParcelLocked(out);
            this.mNetworkPacketActivityCounters[i4].writeSummaryFromParcelLocked(out);
        }
        this.mMobileRadioActiveTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mMobileRadioActivePerAppTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mMobileRadioActiveAdjustedTime.writeSummaryFromParcelLocked(out);
        this.mMobileRadioActiveUnknownTime.writeSummaryFromParcelLocked(out);
        this.mMobileRadioActiveUnknownCount.writeSummaryFromParcelLocked(out);
        this.mWifiOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mGlobalWifiRunningTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        this.mBluetoothOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (int i8 = 0; i8 < 4; i8++) {
            this.mBluetoothStateTimer[i8].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        out.writeInt(this.mNumConnectivityChange);
        this.mFlashlightOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        out.writeInt(this.mKernelWakelockStats.size());
        for (Map.Entry<String, SamplingTimer> ent : this.mKernelWakelockStats.entrySet()) {
            Timer kwlt = ent.getValue();
            if (kwlt != null) {
                out.writeInt(1);
                out.writeString(ent.getKey());
                kwlt.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mWakeupReasonStats.size());
        for (Map.Entry<String, SamplingTimer> ent2 : this.mWakeupReasonStats.entrySet()) {
            SamplingTimer timer = ent2.getValue();
            if (timer != null) {
                out.writeInt(1);
                out.writeString(ent2.getKey());
                timer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(sNumSpeedSteps);
        int NU = this.mUidStats.size();
        out.writeInt(NU);
        for (int iu = 0; iu < NU; iu++) {
            out.writeInt(this.mUidStats.keyAt(iu));
            Uid u = this.mUidStats.valueAt(iu);
            if (u.mWifiRunningTimer != null) {
                out.writeInt(1);
                u.mWifiRunningTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mFullWifiLockTimer != null) {
                out.writeInt(1);
                u.mFullWifiLockTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mWifiScanTimer != null) {
                out.writeInt(1);
                u.mWifiScanTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            for (int i9 = 0; i9 < 5; i9++) {
                if (u.mWifiBatchedScanTimer[i9] != null) {
                    out.writeInt(1);
                    u.mWifiBatchedScanTimer[i9].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
            }
            if (u.mWifiMulticastTimer != null) {
                out.writeInt(1);
                u.mWifiMulticastTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mAudioTurnedOnTimer != null) {
                out.writeInt(1);
                u.mAudioTurnedOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mVideoTurnedOnTimer != null) {
                out.writeInt(1);
                u.mVideoTurnedOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mForegroundActivityTimer != null) {
                out.writeInt(1);
                u.mForegroundActivityTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            for (int i10 = 0; i10 < 3; i10++) {
                if (u.mProcessStateTimer[i10] != null) {
                    out.writeInt(1);
                    u.mProcessStateTimer[i10].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
            }
            if (u.mVibratorOnTimer != null) {
                out.writeInt(1);
                u.mVibratorOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mUserActivityCounters == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                for (int i11 = 0; i11 < 3; i11++) {
                    u.mUserActivityCounters[i11].writeSummaryFromParcelLocked(out);
                }
            }
            if (u.mNetworkByteActivityCounters == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                for (int i12 = 0; i12 < 4; i12++) {
                    u.mNetworkByteActivityCounters[i12].writeSummaryFromParcelLocked(out);
                    u.mNetworkPacketActivityCounters[i12].writeSummaryFromParcelLocked(out);
                }
                u.mMobileRadioActiveTime.writeSummaryFromParcelLocked(out);
                u.mMobileRadioActiveCount.writeSummaryFromParcelLocked(out);
            }
            ArrayMap<String, Uid.Wakelock> wakeStats = u.mWakelockStats.getMap();
            int NW = wakeStats.size();
            out.writeInt(NW);
            for (int iw = 0; iw < NW; iw++) {
                out.writeString(wakeStats.keyAt(iw));
                Uid.Wakelock wl = wakeStats.valueAt(iw);
                if (wl.mTimerFull != null) {
                    out.writeInt(1);
                    wl.mTimerFull.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
                if (wl.mTimerPartial != null) {
                    out.writeInt(1);
                    wl.mTimerPartial.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
                if (wl.mTimerWindow != null) {
                    out.writeInt(1);
                    wl.mTimerWindow.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
            }
            ArrayMap<String, StopwatchTimer> syncStats = u.mSyncStats.getMap();
            int NS = syncStats.size();
            out.writeInt(NS);
            for (int is = 0; is < NS; is++) {
                out.writeString(syncStats.keyAt(is));
                syncStats.valueAt(is).writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            }
            ArrayMap<String, StopwatchTimer> jobStats = u.mJobStats.getMap();
            int NJ = jobStats.size();
            out.writeInt(NJ);
            for (int ij = 0; ij < NJ; ij++) {
                out.writeString(jobStats.keyAt(ij));
                jobStats.valueAt(ij).writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            }
            int NSE = u.mSensorStats.size();
            out.writeInt(NSE);
            for (int ise = 0; ise < NSE; ise++) {
                out.writeInt(u.mSensorStats.keyAt(ise));
                Uid.Sensor se = u.mSensorStats.valueAt(ise);
                if (se.mTimer != null) {
                    out.writeInt(1);
                    se.mTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
            }
            int NP = u.mProcessStats.size();
            out.writeInt(NP);
            for (int ip = 0; ip < NP; ip++) {
                out.writeString(u.mProcessStats.keyAt(ip));
                Uid.Proc ps = u.mProcessStats.valueAt(ip);
                out.writeLong(ps.mUserTime);
                out.writeLong(ps.mSystemTime);
                out.writeLong(ps.mForegroundTime);
                out.writeInt(ps.mStarts);
                out.writeInt(ps.mNumCrashes);
                out.writeInt(ps.mNumAnrs);
                int N = ps.mSpeedBins.length;
                out.writeInt(N);
                for (int i13 = 0; i13 < N; i13++) {
                    if (ps.mSpeedBins[i13] != null) {
                        out.writeInt(1);
                        ps.mSpeedBins[i13].writeSummaryFromParcelLocked(out);
                    } else {
                        out.writeInt(0);
                    }
                }
                ps.writeExcessivePowerToParcelLocked(out);
            }
            int NP2 = u.mPackageStats.size();
            out.writeInt(NP2);
            if (NP2 > 0) {
                for (Map.Entry<String, Uid.Pkg> ent3 : u.mPackageStats.entrySet()) {
                    out.writeString(ent3.getKey());
                    Uid.Pkg ps2 = ent3.getValue();
                    out.writeInt(ps2.mWakeups);
                    int NS2 = ps2.mServiceStats.size();
                    out.writeInt(NS2);
                    if (NS2 > 0) {
                        for (Map.Entry<String, Uid.Pkg.Serv> sent : ps2.mServiceStats.entrySet()) {
                            out.writeString(sent.getKey());
                            Uid.Pkg.Serv ss = sent.getValue();
                            long time = ss.getStartTimeToNowLocked(this.mOnBatteryTimeBase.getUptime(NOW_SYS));
                            out.writeLong(time);
                            out.writeInt(ss.mStarts);
                            out.writeInt(ss.mLaunches);
                        }
                    }
                }
            }
        }
    }

    public void readFromParcel(Parcel in) {
        readFromParcelLocked(in);
    }

    void readFromParcelLocked(Parcel in) {
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new ParcelFormatException("Bad magic number: #" + Integer.toHexString(magic));
        }
        readHistory(in, false);
        this.mStartCount = in.readInt();
        this.mStartClockTime = in.readLong();
        this.mStartPlatformVersion = in.readString();
        this.mEndPlatformVersion = in.readString();
        this.mUptime = in.readLong();
        this.mUptimeStart = in.readLong();
        this.mRealtime = in.readLong();
        this.mRealtimeStart = in.readLong();
        this.mOnBattery = in.readInt() != 0;
        this.mOnBatteryInternal = false;
        this.mOnBatteryTimeBase.readFromParcel(in);
        this.mOnBatteryScreenOffTimeBase.readFromParcel(in);
        this.mScreenState = 0;
        this.mScreenOnTimer = new StopwatchTimer(null, -1, null, this.mOnBatteryTimeBase, in);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i] = new StopwatchTimer(null, (-100) - i, null, this.mOnBatteryTimeBase, in);
        }
        this.mInteractive = false;
        this.mInteractiveTimer = new StopwatchTimer(null, -9, null, this.mOnBatteryTimeBase, in);
        this.mPhoneOn = false;
        this.mLowPowerModeEnabledTimer = new StopwatchTimer(null, -2, null, this.mOnBatteryTimeBase, in);
        this.mPhoneOnTimer = new StopwatchTimer(null, -3, null, this.mOnBatteryTimeBase, in);
        for (int i2 = 0; i2 < 5; i2++) {
            this.mPhoneSignalStrengthsTimer[i2] = new StopwatchTimer(null, (-200) - i2, null, this.mOnBatteryTimeBase, in);
        }
        this.mPhoneSignalScanningTimer = new StopwatchTimer(null, -199, null, this.mOnBatteryTimeBase, in);
        for (int i3 = 0; i3 < 17; i3++) {
            this.mPhoneDataConnectionsTimer[i3] = new StopwatchTimer(null, (-300) - i3, null, this.mOnBatteryTimeBase, in);
        }
        for (int i4 = 0; i4 < 4; i4++) {
            this.mNetworkByteActivityCounters[i4] = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
            this.mNetworkPacketActivityCounters[i4] = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        }
        this.mMobileRadioPowerState = DataConnectionRealTimeInfo.DC_POWER_STATE_LOW;
        this.mMobileRadioActiveTimer = new StopwatchTimer(null, -400, null, this.mOnBatteryTimeBase, in);
        this.mMobileRadioActivePerAppTimer = new StopwatchTimer(null, -401, null, this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveAdjustedTime = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveUnknownTime = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveUnknownCount = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mWifiOn = false;
        this.mWifiOnTimer = new StopwatchTimer(null, -4, null, this.mOnBatteryTimeBase, in);
        this.mGlobalWifiRunning = false;
        this.mGlobalWifiRunningTimer = new StopwatchTimer(null, -5, null, this.mOnBatteryTimeBase, in);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5] = new StopwatchTimer(null, (-600) - i5, null, this.mOnBatteryTimeBase, in);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6] = new StopwatchTimer(null, (-700) - i6, null, this.mOnBatteryTimeBase, in);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7] = new StopwatchTimer(null, (-800) - i7, null, this.mOnBatteryTimeBase, in);
        }
        this.mBluetoothOn = false;
        this.mBluetoothOnTimer = new StopwatchTimer(null, -6, null, this.mOnBatteryTimeBase, in);
        for (int i8 = 0; i8 < 4; i8++) {
            this.mBluetoothStateTimer[i8] = new StopwatchTimer(null, (-500) - i8, null, this.mOnBatteryTimeBase, in);
        }
        this.mNumConnectivityChange = in.readInt();
        this.mLoadedNumConnectivityChange = in.readInt();
        this.mUnpluggedNumConnectivityChange = in.readInt();
        this.mAudioOnNesting = 0;
        this.mAudioOnTimer = new StopwatchTimer(null, -7, null, this.mOnBatteryTimeBase);
        this.mVideoOnNesting = 0;
        this.mVideoOnTimer = new StopwatchTimer(null, -8, null, this.mOnBatteryTimeBase);
        this.mFlashlightOn = false;
        this.mFlashlightOnTimer = new StopwatchTimer(null, -9, null, this.mOnBatteryTimeBase, in);
        this.mDischargeUnplugLevel = in.readInt();
        this.mDischargePlugLevel = in.readInt();
        this.mDischargeCurrentLevel = in.readInt();
        this.mCurrentBatteryLevel = in.readInt();
        this.mLowDischargeAmountSinceCharge = in.readInt();
        this.mHighDischargeAmountSinceCharge = in.readInt();
        this.mDischargeAmountScreenOn = in.readInt();
        this.mDischargeAmountScreenOnSinceCharge = in.readInt();
        this.mDischargeAmountScreenOff = in.readInt();
        this.mDischargeAmountScreenOffSinceCharge = in.readInt();
        this.mNumDischargeStepDurations = in.readInt();
        in.readLongArray(this.mDischargeStepDurations);
        this.mNumChargeStepDurations = in.readInt();
        in.readLongArray(this.mChargeStepDurations);
        this.mLastWriteTime = in.readLong();
        this.mBluetoothPingCount = in.readInt();
        this.mBluetoothPingStart = -1;
        this.mKernelWakelockStats.clear();
        int NKW = in.readInt();
        for (int ikw = 0; ikw < NKW; ikw++) {
            if (in.readInt() != 0) {
                String wakelockName = in.readString();
                SamplingTimer kwlt = new SamplingTimer(this.mOnBatteryScreenOffTimeBase, in);
                this.mKernelWakelockStats.put(wakelockName, kwlt);
            }
        }
        this.mWakeupReasonStats.clear();
        int NWR = in.readInt();
        for (int iwr = 0; iwr < NWR; iwr++) {
            if (in.readInt() != 0) {
                String reasonName = in.readString();
                SamplingTimer timer = new SamplingTimer(this.mOnBatteryTimeBase, in);
                this.mWakeupReasonStats.put(reasonName, timer);
            }
        }
        this.mPartialTimers.clear();
        this.mFullTimers.clear();
        this.mWindowTimers.clear();
        this.mWifiRunningTimers.clear();
        this.mFullWifiLockTimers.clear();
        this.mWifiScanTimers.clear();
        this.mWifiBatchedScanTimers.clear();
        this.mWifiMulticastTimers.clear();
        this.mAudioTurnedOnTimers.clear();
        this.mVideoTurnedOnTimers.clear();
        sNumSpeedSteps = in.readInt();
        int numUids = in.readInt();
        this.mUidStats.clear();
        for (int i9 = 0; i9 < numUids; i9++) {
            int uid = in.readInt();
            Uid u = new Uid(uid);
            u.readFromParcelLocked(this.mOnBatteryTimeBase, this.mOnBatteryScreenOffTimeBase, in);
            this.mUidStats.append(uid, u);
        }
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        writeToParcelLocked(out, true, flags);
    }

    @Override
    public void writeToParcelWithoutUids(Parcel out, int flags) {
        writeToParcelLocked(out, false, flags);
    }

    void writeToParcelLocked(Parcel out, boolean inclUids, int flags) {
        pullPendingStateUpdatesLocked();
        long startClockTime = getStartClockTime();
        long uSecUptime = SystemClock.uptimeMillis() * 1000;
        long uSecRealtime = SystemClock.elapsedRealtime() * 1000;
        this.mOnBatteryTimeBase.getRealtime(uSecRealtime);
        this.mOnBatteryScreenOffTimeBase.getRealtime(uSecRealtime);
        out.writeInt(MAGIC);
        writeHistory(out, true, false);
        out.writeInt(this.mStartCount);
        out.writeLong(startClockTime);
        out.writeString(this.mStartPlatformVersion);
        out.writeString(this.mEndPlatformVersion);
        out.writeLong(this.mUptime);
        out.writeLong(this.mUptimeStart);
        out.writeLong(this.mRealtime);
        out.writeLong(this.mRealtimeStart);
        out.writeInt(this.mOnBattery ? 1 : 0);
        this.mOnBatteryTimeBase.writeToParcel(out, uSecUptime, uSecRealtime);
        this.mOnBatteryScreenOffTimeBase.writeToParcel(out, uSecUptime, uSecRealtime);
        this.mScreenOnTimer.writeToParcel(out, uSecRealtime);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].writeToParcel(out, uSecRealtime);
        }
        this.mInteractiveTimer.writeToParcel(out, uSecRealtime);
        this.mLowPowerModeEnabledTimer.writeToParcel(out, uSecRealtime);
        this.mPhoneOnTimer.writeToParcel(out, uSecRealtime);
        for (int i2 = 0; i2 < 5; i2++) {
            this.mPhoneSignalStrengthsTimer[i2].writeToParcel(out, uSecRealtime);
        }
        this.mPhoneSignalScanningTimer.writeToParcel(out, uSecRealtime);
        for (int i3 = 0; i3 < 17; i3++) {
            this.mPhoneDataConnectionsTimer[i3].writeToParcel(out, uSecRealtime);
        }
        for (int i4 = 0; i4 < 4; i4++) {
            this.mNetworkByteActivityCounters[i4].writeToParcel(out);
            this.mNetworkPacketActivityCounters[i4].writeToParcel(out);
        }
        this.mMobileRadioActiveTimer.writeToParcel(out, uSecRealtime);
        this.mMobileRadioActivePerAppTimer.writeToParcel(out, uSecRealtime);
        this.mMobileRadioActiveAdjustedTime.writeToParcel(out);
        this.mMobileRadioActiveUnknownTime.writeToParcel(out);
        this.mMobileRadioActiveUnknownCount.writeToParcel(out);
        this.mWifiOnTimer.writeToParcel(out, uSecRealtime);
        this.mGlobalWifiRunningTimer.writeToParcel(out, uSecRealtime);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5].writeToParcel(out, uSecRealtime);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6].writeToParcel(out, uSecRealtime);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7].writeToParcel(out, uSecRealtime);
        }
        this.mBluetoothOnTimer.writeToParcel(out, uSecRealtime);
        for (int i8 = 0; i8 < 4; i8++) {
            this.mBluetoothStateTimer[i8].writeToParcel(out, uSecRealtime);
        }
        out.writeInt(this.mNumConnectivityChange);
        out.writeInt(this.mLoadedNumConnectivityChange);
        out.writeInt(this.mUnpluggedNumConnectivityChange);
        this.mFlashlightOnTimer.writeToParcel(out, uSecRealtime);
        out.writeInt(this.mDischargeUnplugLevel);
        out.writeInt(this.mDischargePlugLevel);
        out.writeInt(this.mDischargeCurrentLevel);
        out.writeInt(this.mCurrentBatteryLevel);
        out.writeInt(this.mLowDischargeAmountSinceCharge);
        out.writeInt(this.mHighDischargeAmountSinceCharge);
        out.writeInt(this.mDischargeAmountScreenOn);
        out.writeInt(this.mDischargeAmountScreenOnSinceCharge);
        out.writeInt(this.mDischargeAmountScreenOff);
        out.writeInt(this.mDischargeAmountScreenOffSinceCharge);
        out.writeInt(this.mNumDischargeStepDurations);
        out.writeLongArray(this.mDischargeStepDurations);
        out.writeInt(this.mNumChargeStepDurations);
        out.writeLongArray(this.mChargeStepDurations);
        out.writeLong(this.mLastWriteTime);
        out.writeInt(getBluetoothPingCount());
        if (inclUids) {
            out.writeInt(this.mKernelWakelockStats.size());
            for (Map.Entry<String, SamplingTimer> ent : this.mKernelWakelockStats.entrySet()) {
                SamplingTimer kwlt = ent.getValue();
                if (kwlt != null) {
                    out.writeInt(1);
                    out.writeString(ent.getKey());
                    kwlt.writeToParcel(out, uSecRealtime);
                } else {
                    out.writeInt(0);
                }
            }
            out.writeInt(this.mWakeupReasonStats.size());
            for (Map.Entry<String, SamplingTimer> ent2 : this.mWakeupReasonStats.entrySet()) {
                SamplingTimer timer = ent2.getValue();
                if (timer != null) {
                    out.writeInt(1);
                    out.writeString(ent2.getKey());
                    timer.writeToParcel(out, uSecRealtime);
                } else {
                    out.writeInt(0);
                }
            }
        } else {
            out.writeInt(0);
        }
        out.writeInt(sNumSpeedSteps);
        if (inclUids) {
            int size = this.mUidStats.size();
            out.writeInt(size);
            for (int i9 = 0; i9 < size; i9++) {
                out.writeInt(this.mUidStats.keyAt(i9));
                Uid uid = this.mUidStats.valueAt(i9);
                uid.writeToParcelLocked(out, uSecRealtime);
            }
            return;
        }
        out.writeInt(0);
    }

    @Override
    public void prepareForDumpLocked() {
        pullPendingStateUpdatesLocked();
        getStartClockTime();
    }

    @Override
    public void dumpLocked(Context context, PrintWriter pw, int flags, int reqUid, long histStart) {
        super.dumpLocked(context, pw, flags, reqUid, histStart);
    }
}
