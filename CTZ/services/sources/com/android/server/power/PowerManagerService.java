package com.android.server.power;

import android.R;
import android.app.ActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.SystemSensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.metrics.LogMaker;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.BatteryManager;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.KeyValueListParser;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.EventLogTags;
import com.android.server.LockGuard;
import com.android.server.RescueParty;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.am.BatteryStatsService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.batterysaver.BatterySaverController;
import com.android.server.power.batterysaver.BatterySaverStateMachine;
import com.android.server.power.batterysaver.BatterySavingStats;
import com.android.server.utils.PriorityDump;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public final class PowerManagerService extends SystemService implements Watchdog.Monitor {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SPEW = false;
    private static final int DEFAULT_DOUBLE_TAP_TO_WAKE = 0;
    private static final int DEFAULT_SCREEN_DIM_TIMEOUT = -1;
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT = 15000;
    private static final int DEFAULT_SLEEP_TIMEOUT = -1;
    private static final int DIRTY_ACTUAL_DISPLAY_POWER_STATE_UPDATED = 8;
    private static final int DIRTY_BATTERY_STATE = 256;
    private static final int DIRTY_BOOT_COMPLETED = 16;
    private static final int DIRTY_DOCK_STATE = 1024;
    private static final int DIRTY_IS_POWERED = 64;
    private static final int DIRTY_PROXIMITY_POSITIVE = 512;
    private static final int DIRTY_QUIESCENT = 4096;
    private static final int DIRTY_SCREEN_BRIGHTNESS_BOOST = 2048;
    private static final int DIRTY_SETTINGS = 32;
    private static final int DIRTY_STAY_ON = 128;
    private static final int DIRTY_USER_ACTIVITY = 4;
    private static final int DIRTY_VR_MODE_CHANGED = 8192;
    private static final int DIRTY_WAKEFULNESS = 2;
    private static final int DIRTY_WAKE_LOCKS = 1;
    private static final int HALT_MODE_REBOOT = 1;
    private static final int HALT_MODE_REBOOT_SAFE_MODE = 2;
    private static final int HALT_MODE_SHUTDOWN = 0;
    private static final String LAST_REBOOT_PROPERTY = "persist.sys.boot.reason";
    static final long MIN_LONG_WAKE_CHECK_INTERVAL = 60000;
    private static final int MSG_CHECK_FOR_LONG_WAKELOCKS = 4;
    private static final int MSG_SANDMAN = 2;
    private static final int MSG_SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 3;
    private static final int MSG_USER_ACTIVITY_TIMEOUT = 1;
    private static final int POWER_FEATURE_DOUBLE_TAP_TO_WAKE = 1;
    private static final String REASON_BATTERY_THERMAL_STATE = "shutdown,thermal,battery";
    private static final String REASON_LOW_BATTERY = "shutdown,battery";
    private static final String REASON_REBOOT = "reboot";
    private static final String REASON_SHUTDOWN = "shutdown";
    private static final String REASON_THERMAL_SHUTDOWN = "shutdown,thermal";
    private static final String REASON_USERREQUESTED = "shutdown,userrequested";
    private static final int SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 5000;
    private static final int SCREEN_ON_LATENCY_WARNING_MS = 200;
    private static final String SYSTEM_PROPERTY_QUIESCENT = "ro.boot.quiescent";
    private static final String SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED = "sys.retaildemo.enabled";
    private static final String TAG = "PowerManagerService";
    private static final String TRACE_SCREEN_ON = "Screen turning on";
    private static final int USER_ACTIVITY_SCREEN_BRIGHT = 1;
    private static final int USER_ACTIVITY_SCREEN_DIM = 2;
    private static final int USER_ACTIVITY_SCREEN_DREAM = 4;
    private static final int WAKE_LOCK_BUTTON_BRIGHT = 8;
    private static final int WAKE_LOCK_CPU = 1;
    private static final int WAKE_LOCK_DOZE = 64;
    private static final int WAKE_LOCK_DRAW = 128;
    private static final int WAKE_LOCK_PROXIMITY_SCREEN_OFF = 16;
    private static final int WAKE_LOCK_SCREEN_BRIGHT = 2;
    private static final int WAKE_LOCK_SCREEN_DIM = 4;
    private static final int WAKE_LOCK_STAY_AWAKE = 32;
    private static boolean sQuiescent;
    private boolean mAlwaysOnEnabled;
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    private IAppOpsService mAppOps;
    private Light mAttentionLight;
    private int mBatteryLevel;
    private boolean mBatteryLevelLow;
    private int mBatteryLevelWhenDreamStarted;
    private BatteryManagerInternal mBatteryManagerInternal;
    private final BatterySaverController mBatterySaverController;
    private final BatterySaverPolicy mBatterySaverPolicy;
    private final BatterySaverStateMachine mBatterySaverStateMachine;
    private final BatterySavingStats mBatterySavingStats;
    private IBatteryStats mBatteryStats;
    private boolean mBootCompleted;
    private Runnable[] mBootCompletedRunnables;
    final Constants mConstants;
    private final Context mContext;
    private boolean mDecoupleHalAutoSuspendModeFromDisplayConfig;
    private boolean mDecoupleHalInteractiveModeFromDisplayConfig;
    private boolean mDeviceIdleMode;
    int[] mDeviceIdleTempWhitelist;
    int[] mDeviceIdleWhitelist;
    private int mDirty;
    private DisplayManagerInternal mDisplayManagerInternal;
    private final DisplayManagerInternal.DisplayPowerCallbacks mDisplayPowerCallbacks;
    private final DisplayManagerInternal.DisplayPowerRequest mDisplayPowerRequest;
    private boolean mDisplayReady;
    private final SuspendBlocker mDisplaySuspendBlocker;
    private int mDockState;
    private boolean mDoubleTapWakeEnabled;
    private boolean mDozeAfterScreenOff;
    private int mDozeScreenBrightnessOverrideFromDreamManager;
    private int mDozeScreenStateOverrideFromDreamManager;
    private boolean mDrawWakeLockOverrideFromSidekick;
    private DreamManagerInternal mDreamManager;
    private boolean mDreamsActivateOnDockSetting;
    private boolean mDreamsActivateOnSleepSetting;
    private boolean mDreamsActivatedOnDockByDefaultConfig;
    private boolean mDreamsActivatedOnSleepByDefaultConfig;
    private int mDreamsBatteryLevelDrainCutoffConfig;
    private int mDreamsBatteryLevelMinimumWhenNotPoweredConfig;
    private int mDreamsBatteryLevelMinimumWhenPoweredConfig;
    private boolean mDreamsEnabledByDefaultConfig;
    private boolean mDreamsEnabledOnBatteryConfig;
    private boolean mDreamsEnabledSetting;
    private boolean mDreamsSupportedConfig;
    private int mForegroundProfile;
    private boolean mHalAutoSuspendModeEnabled;
    private boolean mHalInteractiveModeEnabled;
    private final PowerManagerHandler mHandler;
    private final ServiceThread mHandlerThread;
    private boolean mHoldingDisplaySuspendBlocker;
    private boolean mHoldingWakeLockSuspendBlocker;
    private boolean mIsPowered;
    private boolean mIsVrModeEnabled;
    boolean mKeepAwake;
    private long mLastInteractivePowerHintTime;
    private long mLastScreenBrightnessBoostTime;
    private long mLastSleepTime;
    private long mLastUserActivityTime;
    private long mLastUserActivityTimeNoChangeLights;
    private long mLastWakeTime;
    private long mLastWarningAboutUserActivityPermission;
    private boolean mLightDeviceIdleMode;
    private LightsManager mLightsManager;
    private final Object mLock;
    private long mMaximumScreenDimDurationConfig;
    private float mMaximumScreenDimRatioConfig;
    private long mMaximumScreenOffTimeoutFromDeviceAdmin;
    private long mMinimumScreenOffTimeoutConfig;
    private Notifier mNotifier;
    private long mNotifyLongDispatched;
    private long mNotifyLongNextCheck;
    private long mNotifyLongScheduled;
    private long mOverriddenTimeout;
    private int mPlugType;
    private WindowManagerPolicy mPolicy;
    private final SparseArray<ProfilePowerState> mProfilePowerState;
    private boolean mProximityPositive;
    private boolean mRequestWaitForNegativeProximity;
    private boolean mSandmanScheduled;
    private boolean mSandmanSummoned;
    private boolean mScreenBrightnessBoostInProgress;
    private int mScreenBrightnessModeSetting;
    private int mScreenBrightnessOverrideFromWindowManager;
    private int mScreenBrightnessSetting;
    private int mScreenBrightnessSettingDefault;
    private int mScreenBrightnessSettingMaximum;
    private int mScreenBrightnessSettingMinimum;
    private long mScreenDimTimeoutSetting;
    private long mScreenOffTimeoutSetting;
    private SettingsObserver mSettingsObserver;
    private boolean mShutdownFlag;
    private long mSleepTimeoutSetting;
    private boolean mStayOn;
    private int mStayOnWhilePluggedInSetting;
    private boolean mSupportsDoubleTapWakeConfig;
    private final ArrayList<SuspendBlocker> mSuspendBlockers;
    private boolean mSuspendWhenScreenOffDueToProximityConfig;
    private boolean mSystemReady;
    private boolean mTheaterModeEnabled;
    private final SparseArray<UidState> mUidState;
    private boolean mUidsChanged;
    private boolean mUidsChanging;
    private int mUserActivitySummary;
    private long mUserActivityTimeoutOverrideFromWindowManager;
    private boolean mUserInactiveOverrideFromWindowManager;
    private final IVrStateCallbacks mVrStateCallbacks;
    private int mWakeLockSummary;
    private final SuspendBlocker mWakeLockSuspendBlocker;
    private final ArrayList<WakeLock> mWakeLocks;
    private boolean mWakeUpWhenPluggedOrUnpluggedConfig;
    private boolean mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig;
    private int mWakefulness;
    private boolean mWakefulnessChanging;
    private WirelessChargerDetector mWirelessChargerDetector;

    @Retention(RetentionPolicy.SOURCE)
    public @interface HaltMode {
    }

    private static native void nativeAcquireSuspendBlocker(String str);

    private native void nativeInit();

    private static native void nativeReleaseSuspendBlocker(String str);

    private static native void nativeSendPowerHint(int i, int i2);

    private static native void nativeSetAutoSuspend(boolean z);

    private static native void nativeSetFeature(int i, int i2);

    private static native void nativeSetInteractive(boolean z);

    static int access$1076(PowerManagerService powerManagerService, int i) {
        int i2 = i | powerManagerService.mDirty;
        powerManagerService.mDirty = i2;
        return i2;
    }

    private final class ForegroundProfileObserver extends SynchronousUserSwitchObserver {
        private ForegroundProfileObserver() {
        }

        public void onUserSwitching(int i) throws RemoteException {
        }

        public void onForegroundProfileSwitch(int i) throws RemoteException {
            long jUptimeMillis = SystemClock.uptimeMillis();
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.mForegroundProfile = i;
                PowerManagerService.this.maybeUpdateForegroundProfileLastActivityLocked(jUptimeMillis);
            }
        }
    }

    private static final class ProfilePowerState {
        long mLastUserActivityTime = SystemClock.uptimeMillis();
        boolean mLockingNotified;
        long mScreenOffTimeout;
        final int mUserId;
        int mWakeLockSummary;

        public ProfilePowerState(int i, long j) {
            this.mUserId = i;
            this.mScreenOffTimeout = j;
        }
    }

    private final class Constants extends ContentObserver {
        private static final boolean DEFAULT_NO_CACHED_WAKE_LOCKS = true;
        private static final String KEY_NO_CACHED_WAKE_LOCKS = "no_cached_wake_locks";
        public boolean NO_CACHED_WAKE_LOCKS;
        private final KeyValueListParser mParser;
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            this.NO_CACHED_WAKE_LOCKS = true;
            this.mParser = new KeyValueListParser(',');
        }

        public void start(ContentResolver contentResolver) {
            this.mResolver = contentResolver;
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("power_manager_constants"), false, this);
            updateConstants();
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (PowerManagerService.this.mLock) {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, "power_manager_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(PowerManagerService.TAG, "Bad alarm manager settings", e);
                }
                this.NO_CACHED_WAKE_LOCKS = this.mParser.getBoolean(KEY_NO_CACHED_WAKE_LOCKS, true);
            }
        }

        void dump(PrintWriter printWriter) {
            printWriter.println("  Settings power_manager_constants:");
            printWriter.print("    ");
            printWriter.print(KEY_NO_CACHED_WAKE_LOCKS);
            printWriter.print("=");
            printWriter.println(this.NO_CACHED_WAKE_LOCKS);
        }

        void dumpProto(ProtoOutputStream protoOutputStream) {
            long jStart = protoOutputStream.start(1146756268033L);
            protoOutputStream.write(1133871366145L, this.NO_CACHED_WAKE_LOCKS);
            protoOutputStream.end(jStart);
        }
    }

    public PowerManagerService(Context context) {
        super(context);
        this.mLock = LockGuard.installNewLock(1);
        this.mSuspendBlockers = new ArrayList<>();
        this.mWakeLocks = new ArrayList<>();
        this.mDisplayPowerRequest = new DisplayManagerInternal.DisplayPowerRequest();
        this.mDockState = 0;
        this.mMaximumScreenOffTimeoutFromDeviceAdmin = JobStatus.NO_LATEST_RUNTIME;
        this.mScreenBrightnessOverrideFromWindowManager = -1;
        this.mOverriddenTimeout = -1L;
        this.mUserActivityTimeoutOverrideFromWindowManager = -1L;
        this.mDozeScreenStateOverrideFromDreamManager = 0;
        this.mDozeScreenBrightnessOverrideFromDreamManager = -1;
        this.mLastWarningAboutUserActivityPermission = Long.MIN_VALUE;
        this.mShutdownFlag = false;
        this.mDeviceIdleWhitelist = new int[0];
        this.mDeviceIdleTempWhitelist = new int[0];
        this.mUidState = new SparseArray<>();
        this.mProfilePowerState = new SparseArray<>();
        this.mDisplayPowerCallbacks = new DisplayManagerInternal.DisplayPowerCallbacks() {
            private int mDisplayState = 0;

            public void onStateChanged() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.access$1076(PowerManagerService.this, 8);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onProximityPositive() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mProximityPositive = true;
                    PowerManagerService.access$1076(PowerManagerService.this, 512);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onProximityNegative() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mProximityPositive = false;
                    PowerManagerService.access$1076(PowerManagerService.this, 512);
                    PowerManagerService.this.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onDisplayStateChange(int i) {
                synchronized (PowerManagerService.this.mLock) {
                    if (this.mDisplayState != i) {
                        this.mDisplayState = i;
                        if (i == 1) {
                            if (!PowerManagerService.this.mDecoupleHalInteractiveModeFromDisplayConfig) {
                                PowerManagerService.this.setHalInteractiveModeLocked(false);
                            }
                            if (!PowerManagerService.this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
                                PowerManagerService.this.setHalAutoSuspendModeLocked(true);
                            }
                        } else {
                            if (!PowerManagerService.this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
                                PowerManagerService.this.setHalAutoSuspendModeLocked(false);
                            }
                            if (!PowerManagerService.this.mDecoupleHalInteractiveModeFromDisplayConfig) {
                                PowerManagerService.this.setHalInteractiveModeLocked(true);
                            }
                        }
                    }
                }
            }

            public void acquireSuspendBlocker() {
                PowerManagerService.this.mDisplaySuspendBlocker.acquire();
            }

            public void releaseSuspendBlocker() {
                PowerManagerService.this.mDisplaySuspendBlocker.release();
            }

            public String toString() {
                String str;
                synchronized (this) {
                    str = "state=" + Display.stateToString(this.mDisplayState);
                }
                return str;
            }
        };
        this.mVrStateCallbacks = new IVrStateCallbacks.Stub() {
            public void onVrStateChanged(boolean z) {
                PowerManagerService.this.powerHintInternal(7, z ? 1 : 0);
                synchronized (PowerManagerService.this.mLock) {
                    if (PowerManagerService.this.mIsVrModeEnabled != z) {
                        PowerManagerService.this.setVrModeEnabled(z);
                        PowerManagerService.access$1076(PowerManagerService.this, 8192);
                        PowerManagerService.this.updatePowerStateLocked();
                    }
                }
            }
        };
        this.mKeepAwake = false;
        this.mContext = context;
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new PowerManagerHandler(this.mHandlerThread.getLooper());
        this.mConstants = new Constants(this.mHandler);
        this.mAmbientDisplayConfiguration = new AmbientDisplayConfiguration(this.mContext);
        this.mBatterySavingStats = new BatterySavingStats(this.mLock);
        this.mBatterySaverPolicy = new BatterySaverPolicy(this.mLock, this.mContext, this.mBatterySavingStats);
        this.mBatterySaverController = new BatterySaverController(this.mLock, this.mContext, BackgroundThread.get().getLooper(), this.mBatterySaverPolicy, this.mBatterySavingStats);
        this.mBatterySaverStateMachine = new BatterySaverStateMachine(this.mLock, this.mContext, this.mBatterySaverController);
        synchronized (this.mLock) {
            this.mWakeLockSuspendBlocker = createSuspendBlockerLocked("PowerManagerService.WakeLocks");
            this.mDisplaySuspendBlocker = createSuspendBlockerLocked("PowerManagerService.Display");
            this.mDisplaySuspendBlocker.acquire();
            this.mHoldingDisplaySuspendBlocker = true;
            this.mHalAutoSuspendModeEnabled = false;
            this.mHalInteractiveModeEnabled = true;
            this.mWakefulness = 1;
            sQuiescent = SystemProperties.get(SYSTEM_PROPERTY_QUIESCENT, "0").equals("1");
            nativeInit();
            nativeSetAutoSuspend(false);
            nativeSetInteractive(true);
            nativeSetFeature(1, 0);
        }
    }

    @VisibleForTesting
    PowerManagerService(Context context, BatterySaverPolicy batterySaverPolicy) {
        super(context);
        this.mLock = LockGuard.installNewLock(1);
        this.mSuspendBlockers = new ArrayList<>();
        this.mWakeLocks = new ArrayList<>();
        this.mDisplayPowerRequest = new DisplayManagerInternal.DisplayPowerRequest();
        this.mDockState = 0;
        this.mMaximumScreenOffTimeoutFromDeviceAdmin = JobStatus.NO_LATEST_RUNTIME;
        this.mScreenBrightnessOverrideFromWindowManager = -1;
        this.mOverriddenTimeout = -1L;
        this.mUserActivityTimeoutOverrideFromWindowManager = -1L;
        this.mDozeScreenStateOverrideFromDreamManager = 0;
        this.mDozeScreenBrightnessOverrideFromDreamManager = -1;
        this.mLastWarningAboutUserActivityPermission = Long.MIN_VALUE;
        this.mShutdownFlag = false;
        this.mDeviceIdleWhitelist = new int[0];
        this.mDeviceIdleTempWhitelist = new int[0];
        this.mUidState = new SparseArray<>();
        this.mProfilePowerState = new SparseArray<>();
        this.mDisplayPowerCallbacks = new DisplayManagerInternal.DisplayPowerCallbacks() {
            private int mDisplayState = 0;

            public void onStateChanged() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.access$1076(PowerManagerService.this, 8);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onProximityPositive() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mProximityPositive = true;
                    PowerManagerService.access$1076(PowerManagerService.this, 512);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onProximityNegative() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mProximityPositive = false;
                    PowerManagerService.access$1076(PowerManagerService.this, 512);
                    PowerManagerService.this.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onDisplayStateChange(int i) {
                synchronized (PowerManagerService.this.mLock) {
                    if (this.mDisplayState != i) {
                        this.mDisplayState = i;
                        if (i == 1) {
                            if (!PowerManagerService.this.mDecoupleHalInteractiveModeFromDisplayConfig) {
                                PowerManagerService.this.setHalInteractiveModeLocked(false);
                            }
                            if (!PowerManagerService.this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
                                PowerManagerService.this.setHalAutoSuspendModeLocked(true);
                            }
                        } else {
                            if (!PowerManagerService.this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
                                PowerManagerService.this.setHalAutoSuspendModeLocked(false);
                            }
                            if (!PowerManagerService.this.mDecoupleHalInteractiveModeFromDisplayConfig) {
                                PowerManagerService.this.setHalInteractiveModeLocked(true);
                            }
                        }
                    }
                }
            }

            public void acquireSuspendBlocker() {
                PowerManagerService.this.mDisplaySuspendBlocker.acquire();
            }

            public void releaseSuspendBlocker() {
                PowerManagerService.this.mDisplaySuspendBlocker.release();
            }

            public String toString() {
                String str;
                synchronized (this) {
                    str = "state=" + Display.stateToString(this.mDisplayState);
                }
                return str;
            }
        };
        this.mVrStateCallbacks = new IVrStateCallbacks.Stub() {
            public void onVrStateChanged(boolean z) {
                PowerManagerService.this.powerHintInternal(7, z ? 1 : 0);
                synchronized (PowerManagerService.this.mLock) {
                    if (PowerManagerService.this.mIsVrModeEnabled != z) {
                        PowerManagerService.this.setVrModeEnabled(z);
                        PowerManagerService.access$1076(PowerManagerService.this, 8192);
                        PowerManagerService.this.updatePowerStateLocked();
                    }
                }
            }
        };
        this.mKeepAwake = false;
        this.mContext = context;
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new PowerManagerHandler(this.mHandlerThread.getLooper());
        this.mConstants = new Constants(this.mHandler);
        this.mAmbientDisplayConfiguration = new AmbientDisplayConfiguration(this.mContext);
        this.mDisplaySuspendBlocker = null;
        this.mWakeLockSuspendBlocker = null;
        this.mBatterySavingStats = new BatterySavingStats(this.mLock);
        this.mBatterySaverPolicy = batterySaverPolicy;
        this.mBatterySaverController = new BatterySaverController(this.mLock, context, BackgroundThread.getHandler().getLooper(), batterySaverPolicy, this.mBatterySavingStats);
        this.mBatterySaverStateMachine = new BatterySaverStateMachine(this.mLock, this.mContext, this.mBatterySaverController);
    }

    @Override
    public void onStart() {
        publishBinderService("power", new BinderService());
        publishLocalService(PowerManagerInternal.class, new LocalService());
        Watchdog.getInstance().addMonitor(this);
        Watchdog.getInstance().addThread(this.mHandler);
    }

    @Override
    public void onBootPhase(int i) {
        synchronized (this.mLock) {
            try {
                if (i == 600) {
                    incrementBootCount();
                } else if (i == 1000) {
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    this.mBootCompleted = true;
                    this.mDirty |= 16;
                    this.mBatterySaverStateMachine.onBootCompleted();
                    userActivityNoUpdateLocked(jUptimeMillis, 0, 0, 1000);
                    updatePowerStateLocked();
                    if (!ArrayUtils.isEmpty(this.mBootCompletedRunnables)) {
                        Slog.d(TAG, "Posting " + this.mBootCompletedRunnables.length + " delayed runnables");
                        for (Runnable runnable : this.mBootCompletedRunnables) {
                            BackgroundThread.getHandler().post(runnable);
                        }
                    }
                    this.mBootCompletedRunnables = null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void systemReady(IAppOpsService iAppOpsService) {
        synchronized (this.mLock) {
            this.mSystemReady = true;
            this.mAppOps = iAppOpsService;
            this.mDreamManager = (DreamManagerInternal) getLocalService(DreamManagerInternal.class);
            this.mDisplayManagerInternal = (DisplayManagerInternal) getLocalService(DisplayManagerInternal.class);
            this.mPolicy = (WindowManagerPolicy) getLocalService(WindowManagerPolicy.class);
            this.mBatteryManagerInternal = (BatteryManagerInternal) getLocalService(BatteryManagerInternal.class);
            PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mScreenBrightnessSettingMinimum = powerManager.getMinimumScreenBrightnessSetting();
            this.mScreenBrightnessSettingMaximum = powerManager.getMaximumScreenBrightnessSetting();
            this.mScreenBrightnessSettingDefault = powerManager.getDefaultScreenBrightnessSetting();
            SystemSensorManager systemSensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
            this.mBatteryStats = BatteryStatsService.getService();
            this.mNotifier = new Notifier(Looper.getMainLooper(), this.mContext, this.mBatteryStats, createSuspendBlockerLocked("PowerManagerService.Broadcasts"), this.mPolicy);
            this.mWirelessChargerDetector = new WirelessChargerDetector(systemSensorManager, createSuspendBlockerLocked("PowerManagerService.WirelessChargerDetector"), this.mHandler);
            this.mSettingsObserver = new SettingsObserver(this.mHandler);
            this.mLightsManager = (LightsManager) getLocalService(LightsManager.class);
            this.mAttentionLight = this.mLightsManager.getLight(5);
            this.mDisplayManagerInternal.initPowerManagement(this.mDisplayPowerCallbacks, this.mHandler, systemSensorManager);
            try {
                ActivityManager.getService().registerUserSwitchObserver(new ForegroundProfileObserver(), TAG);
            } catch (RemoteException e) {
            }
            readConfigurationLocked();
            updateSettingsLocked();
            this.mDirty |= 256;
            updatePowerStateLocked();
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mConstants.start(contentResolver);
        this.mBatterySaverController.systemReady();
        this.mBatterySaverPolicy.systemReady();
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_enabled"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_activate_on_sleep"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_activate_on_dock"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.System.getUriFor("screen_off_timeout"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.System.getUriFor("screen_dim_timeout"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("sleep_timeout"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("stay_on_while_plugged_in"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.System.getUriFor("screen_auto_brightness_adj"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("theater_mode_on"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("doze_always_on"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("double_tap_to_wake"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("device_demo_mode"), false, this.mSettingsObserver, 0);
        IVrManager binderService = getBinderService("vrmanager");
        if (binderService != null) {
            try {
                binderService.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e2) {
                Slog.e(TAG, "Failed to register VR mode state listener: " + e2);
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.setPriority(1000);
        this.mContext.registerReceiver(new BatteryReceiver(), intentFilter, null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.DREAMING_STARTED");
        intentFilter2.addAction("android.intent.action.DREAMING_STOPPED");
        this.mContext.registerReceiver(new DreamReceiver(), intentFilter2, null, this.mHandler);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(new UserSwitchedReceiver(), intentFilter3, null, this.mHandler);
        IntentFilter intentFilter4 = new IntentFilter();
        intentFilter4.addAction("android.intent.action.DOCK_EVENT");
        this.mContext.registerReceiver(new DockReceiver(), intentFilter4, null, this.mHandler);
    }

    private void readConfigurationLocked() {
        Resources resources = this.mContext.getResources();
        this.mDecoupleHalAutoSuspendModeFromDisplayConfig = resources.getBoolean(R.^attr-private.magnifierHeight);
        this.mDecoupleHalInteractiveModeFromDisplayConfig = resources.getBoolean(R.^attr-private.magnifierHorizontalOffset);
        this.mWakeUpWhenPluggedOrUnpluggedConfig = resources.getBoolean(R.^attr-private.pointerIconSpotTouch);
        this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig = resources.getBoolean(R.^attr-private.autofillDatasetPickerMaxHeight);
        this.mSuspendWhenScreenOffDueToProximityConfig = resources.getBoolean(R.^attr-private.pointerIconCrosshair);
        this.mDreamsSupportedConfig = resources.getBoolean(R.^attr-private.fadedHeight);
        this.mDreamsEnabledByDefaultConfig = resources.getBoolean(R.^attr-private.expandActivityOverflowButtonDrawable);
        this.mDreamsActivatedOnSleepByDefaultConfig = resources.getBoolean(R.^attr-private.errorMessageBackground);
        this.mDreamsActivatedOnDockByDefaultConfig = resources.getBoolean(R.^attr-private.errorMessageAboveBackground);
        this.mDreamsEnabledOnBatteryConfig = resources.getBoolean(R.^attr-private.externalRouteEnabledDrawable);
        this.mDreamsBatteryLevelMinimumWhenPoweredConfig = resources.getInteger(R.integer.config_cameraLiftTriggerSensorType);
        this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig = resources.getInteger(R.integer.config_cameraLaunchGestureSensorType);
        this.mDreamsBatteryLevelDrainCutoffConfig = resources.getInteger(R.integer.config_burnInProtectionMinVerticalOffset);
        this.mDozeAfterScreenOff = resources.getBoolean(R.^attr-private.enableSubtitle);
        this.mMinimumScreenOffTimeoutConfig = resources.getInteger(R.integer.config_defaultPeakRefreshRate);
        this.mMaximumScreenDimDurationConfig = resources.getInteger(R.integer.config_defaultNightDisplayCustomStartTime);
        this.mMaximumScreenDimRatioConfig = resources.getFraction(R.fraction.config_dimBehindFadeDuration, 1, 1);
        this.mSupportsDoubleTapWakeConfig = resources.getBoolean(R.^attr-private.panelMenuListTheme);
    }

    private void updateSettingsLocked() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mDreamsEnabledSetting = Settings.Secure.getIntForUser(contentResolver, "screensaver_enabled", this.mDreamsEnabledByDefaultConfig ? 1 : 0, -2) != 0;
        this.mDreamsActivateOnSleepSetting = Settings.Secure.getIntForUser(contentResolver, "screensaver_activate_on_sleep", this.mDreamsActivatedOnSleepByDefaultConfig ? 1 : 0, -2) != 0;
        this.mDreamsActivateOnDockSetting = Settings.Secure.getIntForUser(contentResolver, "screensaver_activate_on_dock", this.mDreamsActivatedOnDockByDefaultConfig ? 1 : 0, -2) != 0;
        this.mScreenOffTimeoutSetting = Settings.System.getIntForUser(contentResolver, "screen_off_timeout", 15000, -2);
        this.mScreenDimTimeoutSetting = Settings.System.getIntForUser(contentResolver, "screen_dim_timeout", -1, -2);
        this.mSleepTimeoutSetting = Settings.Secure.getIntForUser(contentResolver, "sleep_timeout", -1, -2);
        this.mStayOnWhilePluggedInSetting = Settings.Global.getInt(contentResolver, "stay_on_while_plugged_in", 1);
        this.mTheaterModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
        this.mAlwaysOnEnabled = this.mAmbientDisplayConfiguration.alwaysOnEnabled(-2);
        if (this.mSupportsDoubleTapWakeConfig) {
            boolean z = Settings.Secure.getIntForUser(contentResolver, "double_tap_to_wake", 0, -2) != 0;
            if (z != this.mDoubleTapWakeEnabled) {
                this.mDoubleTapWakeEnabled = z;
                nativeSetFeature(1, this.mDoubleTapWakeEnabled ? 1 : 0);
            }
        }
        String str = UserManager.isDeviceInDemoMode(this.mContext) ? "1" : "0";
        if (!str.equals(SystemProperties.get(SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED))) {
            SystemProperties.set(SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED, str);
        }
        this.mScreenBrightnessModeSetting = Settings.System.getIntForUser(contentResolver, "screen_brightness_mode", 0, -2);
        this.mDirty |= 32;
    }

    private void postAfterBootCompleted(Runnable runnable) {
        if (this.mBootCompleted) {
            BackgroundThread.getHandler().post(runnable);
        } else {
            Slog.d(TAG, "Delaying runnable until system is booted");
            this.mBootCompletedRunnables = (Runnable[]) ArrayUtils.appendElement(Runnable.class, this.mBootCompletedRunnables, runnable);
        }
    }

    private void handleSettingsChangedLocked() {
        updateSettingsLocked();
        updatePowerStateLocked();
    }

    private void acquireWakeLockInternal(IBinder iBinder, int i, String str, String str2, WorkSource workSource, String str3, int i2, int i3) {
        WakeLock wakeLock;
        synchronized (this.mLock) {
            int iFindWakeLockIndexLocked = findWakeLockIndexLocked(iBinder);
            boolean z = false;
            if (iFindWakeLockIndexLocked >= 0) {
                wakeLock = this.mWakeLocks.get(iFindWakeLockIndexLocked);
                if (!wakeLock.hasSameProperties(i, str, workSource, i2, i3)) {
                    notifyWakeLockChangingLocked(wakeLock, i, str, str2, i2, i3, workSource, str3);
                    wakeLock.updateProperties(i, str, str2, workSource, str3, i2, i3);
                }
            } else {
                UidState uidState = this.mUidState.get(i2);
                if (uidState == null) {
                    uidState = new UidState(i2);
                    uidState.mProcState = 19;
                    this.mUidState.put(i2, uidState);
                }
                UidState uidState2 = uidState;
                uidState2.mNumWakeLocks++;
                WakeLock wakeLock2 = new WakeLock(iBinder, i, str, str2, workSource, str3, i2, i3, uidState2);
                try {
                    iBinder.linkToDeath(wakeLock2, 0);
                    this.mWakeLocks.add(wakeLock2);
                    setWakeLockDisabledStateLocked(wakeLock2);
                    wakeLock = wakeLock2;
                    z = true;
                } catch (RemoteException e) {
                    throw new IllegalArgumentException("Wake lock is already dead.");
                }
            }
            applyWakeLockFlagsOnAcquireLocked(wakeLock, i2);
            this.mDirty |= 1;
            updatePowerStateLocked();
            if (z) {
                notifyWakeLockAcquiredLocked(wakeLock);
            }
        }
    }

    private static boolean isScreenLock(WakeLock wakeLock) {
        int i = wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
        if (i == 6 || i == 10 || i == 26) {
            return true;
        }
        return false;
    }

    private void applyWakeLockFlagsOnAcquireLocked(WakeLock wakeLock, int i) {
        String name;
        int i2;
        if ((wakeLock.mFlags & 268435456) != 0 && isScreenLock(wakeLock)) {
            if (wakeLock.mWorkSource != null && wakeLock.mWorkSource.getName(0) != null) {
                name = wakeLock.mWorkSource.getName(0);
                i2 = wakeLock.mWorkSource.get(0);
            } else {
                name = wakeLock.mPackageName;
                i2 = wakeLock.mWorkSource != null ? wakeLock.mWorkSource.get(0) : wakeLock.mOwnerUid;
            }
            int i3 = i2;
            wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), wakeLock.mTag, i3, name, i3);
        }
    }

    private void releaseWakeLockInternal(IBinder iBinder, int i) {
        synchronized (this.mLock) {
            int iFindWakeLockIndexLocked = findWakeLockIndexLocked(iBinder);
            if (iFindWakeLockIndexLocked < 0) {
                return;
            }
            WakeLock wakeLock = this.mWakeLocks.get(iFindWakeLockIndexLocked);
            if ((i & 1) != 0) {
                this.mRequestWaitForNegativeProximity = true;
            }
            wakeLock.mLock.unlinkToDeath(wakeLock, 0);
            removeWakeLockLocked(wakeLock, iFindWakeLockIndexLocked);
        }
    }

    private void handleWakeLockDeath(WakeLock wakeLock) {
        synchronized (this.mLock) {
            int iIndexOf = this.mWakeLocks.indexOf(wakeLock);
            if (iIndexOf < 0) {
                return;
            }
            removeWakeLockLocked(wakeLock, iIndexOf);
        }
    }

    private void removeWakeLockLocked(WakeLock wakeLock, int i) {
        this.mWakeLocks.remove(i);
        UidState uidState = wakeLock.mUidState;
        uidState.mNumWakeLocks--;
        if (uidState.mNumWakeLocks <= 0 && uidState.mProcState == 19) {
            this.mUidState.remove(uidState.mUid);
        }
        notifyWakeLockReleasedLocked(wakeLock);
        applyWakeLockFlagsOnReleaseLocked(wakeLock);
        this.mDirty |= 1;
        updatePowerStateLocked();
    }

    private void applyWakeLockFlagsOnReleaseLocked(WakeLock wakeLock) {
        if ((wakeLock.mFlags & 536870912) != 0 && isScreenLock(wakeLock)) {
            userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 1, wakeLock.mOwnerUid);
        }
    }

    private void updateWakeLockWorkSourceInternal(IBinder iBinder, WorkSource workSource, String str, int i) {
        synchronized (this.mLock) {
            int iFindWakeLockIndexLocked = findWakeLockIndexLocked(iBinder);
            if (iFindWakeLockIndexLocked < 0) {
                throw new IllegalArgumentException("Wake lock not active: " + iBinder + " from uid " + i);
            }
            WakeLock wakeLock = this.mWakeLocks.get(iFindWakeLockIndexLocked);
            if (!wakeLock.hasSameWorkSource(workSource)) {
                notifyWakeLockChangingLocked(wakeLock, wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, workSource, str);
                wakeLock.mHistoryTag = str;
                wakeLock.updateWorkSource(workSource);
            }
        }
    }

    private int findWakeLockIndexLocked(IBinder iBinder) {
        int size = this.mWakeLocks.size();
        for (int i = 0; i < size; i++) {
            if (this.mWakeLocks.get(i).mLock == iBinder) {
                return i;
            }
        }
        return -1;
    }

    private void notifyWakeLockAcquiredLocked(WakeLock wakeLock) {
        if (this.mSystemReady && !wakeLock.mDisabled) {
            wakeLock.mNotifiedAcquired = true;
            this.mNotifier.onWakeLockAcquired(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
            restartNofifyLongTimerLocked(wakeLock);
        }
    }

    private void enqueueNotifyLongMsgLocked(long j) {
        this.mNotifyLongScheduled = j;
        Message messageObtainMessage = this.mHandler.obtainMessage(4);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessageAtTime(messageObtainMessage, j);
    }

    private void restartNofifyLongTimerLocked(WakeLock wakeLock) {
        wakeLock.mAcquireTime = SystemClock.uptimeMillis();
        if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 1 && this.mNotifyLongScheduled == 0) {
            enqueueNotifyLongMsgLocked(wakeLock.mAcquireTime + 60000);
        }
    }

    private void notifyWakeLockLongStartedLocked(WakeLock wakeLock) {
        if (this.mSystemReady && !wakeLock.mDisabled) {
            wakeLock.mNotifiedLong = true;
            this.mNotifier.onLongPartialWakeLockStart(wakeLock.mTag, wakeLock.mOwnerUid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
        }
    }

    private void notifyWakeLockLongFinishedLocked(WakeLock wakeLock) {
        if (wakeLock.mNotifiedLong) {
            wakeLock.mNotifiedLong = false;
            this.mNotifier.onLongPartialWakeLockFinish(wakeLock.mTag, wakeLock.mOwnerUid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
        }
    }

    private void notifyWakeLockChangingLocked(WakeLock wakeLock, int i, String str, String str2, int i2, int i3, WorkSource workSource, String str3) {
        if (this.mSystemReady && wakeLock.mNotifiedAcquired) {
            this.mNotifier.onWakeLockChanging(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag, i, str, str2, i2, i3, workSource, str3);
            notifyWakeLockLongFinishedLocked(wakeLock);
            restartNofifyLongTimerLocked(wakeLock);
        }
    }

    private void notifyWakeLockReleasedLocked(WakeLock wakeLock) {
        if (this.mSystemReady && wakeLock.mNotifiedAcquired) {
            wakeLock.mNotifiedAcquired = false;
            wakeLock.mAcquireTime = 0L;
            this.mNotifier.onWakeLockReleased(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
            notifyWakeLockLongFinishedLocked(wakeLock);
        }
    }

    private boolean isWakeLockLevelSupportedInternal(int i) {
        synchronized (this.mLock) {
            boolean z = true;
            try {
                if (i != 1 && i != 6 && i != 10 && i != 26) {
                    if (i == 32) {
                        if (!this.mSystemReady || !this.mDisplayManagerInternal.isProximitySensorAvailable()) {
                            z = false;
                        }
                        return z;
                    }
                    if (i != 64 && i != 128) {
                        return false;
                    }
                }
                return true;
            } finally {
            }
        }
    }

    private void userActivityFromNative(long j, int i, int i2) {
        userActivityInternal(j, i, i2, 1000);
    }

    private void userActivityInternal(long j, int i, int i2, int i3) {
        synchronized (this.mLock) {
            if (userActivityNoUpdateLocked(j, i, i2, i3)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean userActivityNoUpdateLocked(long j, int i, int i2, int i3) {
        if (j < this.mLastSleepTime || j < this.mLastWakeTime || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072L, "userActivity");
        try {
            if (j > this.mLastInteractivePowerHintTime) {
                powerHintInternal(2, 0);
                this.mLastInteractivePowerHintTime = j;
            }
            this.mNotifier.onUserActivity(i, i3);
            if (this.mUserInactiveOverrideFromWindowManager) {
                this.mUserInactiveOverrideFromWindowManager = false;
                this.mOverriddenTimeout = -1L;
            }
            if (this.mWakefulness != 0 && this.mWakefulness != 3 && (i2 & 2) == 0) {
                maybeUpdateForegroundProfileLastActivityLocked(j);
                if ((i2 & 1) != 0) {
                    if (j > this.mLastUserActivityTimeNoChangeLights && j > this.mLastUserActivityTime) {
                        this.mLastUserActivityTimeNoChangeLights = j;
                        this.mDirty |= 4;
                        if (i == 1) {
                            this.mDirty |= 4096;
                        }
                        return true;
                    }
                } else if (j > this.mLastUserActivityTime) {
                    this.mLastUserActivityTime = j;
                    this.mDirty |= 4;
                    if (i == 1) {
                        this.mDirty |= 4096;
                    }
                    return true;
                }
                return false;
            }
            return false;
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    private void maybeUpdateForegroundProfileLastActivityLocked(long j) {
        ProfilePowerState profilePowerState = this.mProfilePowerState.get(this.mForegroundProfile);
        if (profilePowerState != null && j > profilePowerState.mLastUserActivityTime) {
            profilePowerState.mLastUserActivityTime = j;
        }
    }

    private void wakeUpInternal(long j, String str, int i, String str2, int i2) {
        synchronized (this.mLock) {
            if (wakeUpNoUpdateLocked(j, str, i, str2, i2)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean wakeUpNoUpdateLocked(long j, String str, int i, String str2, int i2) {
        if (j < this.mLastSleepTime || this.mWakefulness == 1 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.asyncTraceBegin(131072L, TRACE_SCREEN_ON, 0);
        Trace.traceBegin(131072L, "wakeUp");
        try {
            int i3 = this.mWakefulness;
            if (i3 == 0) {
                Slog.i(TAG, "Waking up from sleep (uid=" + i + " reason=" + str + ")...");
            } else {
                switch (i3) {
                    case 2:
                        Slog.i(TAG, "Waking up from dream (uid=" + i + " reason=" + str + ")...");
                        break;
                    case 3:
                        Slog.i(TAG, "Waking up from dozing (uid=" + i + " reason=" + str + ")...");
                        break;
                }
            }
            this.mLastWakeTime = j;
            setWakefulnessLocked(1, 0);
            this.mNotifier.onWakeUp(str, i, str2, i2);
            userActivityNoUpdateLocked(j, 0, 0, i);
            return true;
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    private void goToSleepInternal(long j, int i, int i2, int i3) {
        synchronized (this.mLock) {
            if (goToSleepNoUpdateLocked(j, i, i2, i3)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean goToSleepNoUpdateLocked(long j, int i, int i2, int i3) {
        if (i == 8) {
            this.mDirty |= 32;
            this.mShutdownFlag = true;
            Slog.d(TAG, "go to sleep due to quick shutdown");
            return true;
        }
        if (j < this.mLastWakeTime || this.mKeepAwake || this.mWakefulness == 0 || this.mWakefulness == 3 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072L, "goToSleep");
        try {
            switch (i) {
                case 1:
                    Slog.i(TAG, "Going to sleep due to device administration policy (uid " + i3 + ")...");
                    break;
                case 2:
                    Slog.i(TAG, "Going to sleep due to screen timeout (uid " + i3 + ")...");
                    break;
                case 3:
                    Slog.i(TAG, "Going to sleep due to lid switch (uid " + i3 + ")...");
                    break;
                case 4:
                    Slog.i(TAG, "Going to sleep due to power button (uid " + i3 + ")...");
                    break;
                case 5:
                    Slog.i(TAG, "Going to sleep due to HDMI standby (uid " + i3 + ")...");
                    break;
                case 6:
                    Slog.i(TAG, "Going to sleep due to sleep button (uid " + i3 + ")...");
                    break;
                case 7:
                    Slog.i(TAG, "Going to sleep by an accessibility service request (uid " + i3 + ")...");
                    break;
                default:
                    Slog.i(TAG, "Going to sleep by application request (uid " + i3 + ")...");
                    i = 0;
                    break;
            }
            this.mLastSleepTime = j;
            this.mSandmanSummoned = true;
            setWakefulnessLocked(3, i);
            int size = this.mWakeLocks.size();
            int i4 = 0;
            for (int i5 = 0; i5 < size; i5++) {
                int i6 = this.mWakeLocks.get(i5).mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
                if (i6 == 6 || i6 == 10 || i6 == 26) {
                    i4++;
                }
            }
            EventLogTags.writePowerSleepRequested(i4);
            if ((i2 & 1) != 0) {
                reallyGoToSleepNoUpdateLocked(j, i3);
            }
            return true;
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    private void napInternal(long j, int i) {
        synchronized (this.mLock) {
            if (napNoUpdateLocked(j, i)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean napNoUpdateLocked(long j, int i) {
        if (j < this.mLastWakeTime || this.mWakefulness != 1 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072L, "nap");
        try {
            Slog.i(TAG, "Nap time (uid " + i + ")...");
            this.mSandmanSummoned = true;
            setWakefulnessLocked(2, 0);
            return true;
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    private boolean reallyGoToSleepNoUpdateLocked(long j, int i) {
        if (j < this.mLastWakeTime || this.mWakefulness == 0 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072L, "reallyGoToSleep");
        try {
            Slog.i(TAG, "Sleeping (uid " + i + ")...");
            setWakefulnessLocked(0, 2);
            Trace.traceEnd(131072L);
            return true;
        } catch (Throwable th) {
            Trace.traceEnd(131072L);
            throw th;
        }
    }

    @VisibleForTesting
    void setWakefulnessLocked(int i, int i2) {
        if (this.mWakefulness != i) {
            this.mWakefulness = i;
            this.mWakefulnessChanging = true;
            this.mDirty |= 2;
            if (this.mNotifier != null) {
                this.mNotifier.onWakefulnessChangeStarted(i, i2);
            }
        }
    }

    private void logSleepTimeoutRecapturedLocked() {
        long jUptimeMillis = this.mOverriddenTimeout - SystemClock.uptimeMillis();
        if (jUptimeMillis >= 0) {
            EventLogTags.writePowerSoftSleepRequested(jUptimeMillis);
            this.mOverriddenTimeout = -1L;
        }
    }

    private void logScreenOn() {
        Trace.asyncTraceEnd(131072L, TRACE_SCREEN_ON, 0);
        int iUptimeMillis = (int) (SystemClock.uptimeMillis() - this.mLastWakeTime);
        LogMaker logMaker = new LogMaker(198);
        logMaker.setType(1);
        logMaker.setSubtype(0);
        logMaker.setLatency(iUptimeMillis);
        MetricsLogger.action(logMaker);
        EventLogTags.writePowerScreenState(1, 0, 0L, 0, iUptimeMillis);
        if (iUptimeMillis >= 200) {
            Slog.w(TAG, "Screen on took " + iUptimeMillis + " ms");
        }
    }

    private void finishWakefulnessChangeIfNeededLocked() {
        if (this.mWakefulnessChanging && this.mDisplayReady) {
            if (this.mWakefulness == 3 && (this.mWakeLockSummary & 64) == 0) {
                return;
            }
            if (this.mWakefulness == 3 || this.mWakefulness == 0) {
                logSleepTimeoutRecapturedLocked();
            }
            if (this.mWakefulness == 1) {
                logScreenOn();
            }
            this.mWakefulnessChanging = false;
            this.mNotifier.onWakefulnessChangeFinished();
        }
    }

    private void updatePowerStateLocked() {
        int i;
        if (!this.mSystemReady || this.mDirty == 0) {
            return;
        }
        if (!Thread.holdsLock(this.mLock)) {
            Slog.wtf(TAG, "Power manager lock was not held when calling updatePowerStateLocked");
        }
        Trace.traceBegin(131072L, "updatePowerState");
        try {
            updateIsPoweredLocked(this.mDirty);
            updateStayOnLocked(this.mDirty);
            updateScreenBrightnessBoostLocked(this.mDirty);
            long jUptimeMillis = SystemClock.uptimeMillis();
            int i2 = 0;
            do {
                i = this.mDirty;
                i2 |= i;
                this.mDirty = 0;
                updateWakeLockSummaryLocked(i);
                updateUserActivitySummaryLocked(jUptimeMillis, i);
            } while (updateWakefulnessLocked(i));
            updateProfilesLocked(jUptimeMillis);
            updateDreamLocked(i2, updateDisplayPowerStateLocked(i2));
            finishWakefulnessChangeIfNeededLocked();
            updateSuspendBlockerLocked();
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    private void updateProfilesLocked(long j) {
        int size = this.mProfilePowerState.size();
        for (int i = 0; i < size; i++) {
            ProfilePowerState profilePowerStateValueAt = this.mProfilePowerState.valueAt(i);
            if (isProfileBeingKeptAwakeLocked(profilePowerStateValueAt, j)) {
                profilePowerStateValueAt.mLockingNotified = false;
            } else if (!profilePowerStateValueAt.mLockingNotified) {
                profilePowerStateValueAt.mLockingNotified = true;
                this.mNotifier.onProfileTimeout(profilePowerStateValueAt.mUserId);
            }
        }
    }

    private boolean isProfileBeingKeptAwakeLocked(ProfilePowerState profilePowerState, long j) {
        return profilePowerState.mLastUserActivityTime + profilePowerState.mScreenOffTimeout > j || (profilePowerState.mWakeLockSummary & 32) != 0 || (this.mProximityPositive && (profilePowerState.mWakeLockSummary & 16) != 0);
    }

    private void updateIsPoweredLocked(int i) {
        if ((i & 256) != 0) {
            boolean z = this.mIsPowered;
            int i2 = this.mPlugType;
            boolean z2 = this.mBatteryLevelLow;
            this.mIsPowered = this.mBatteryManagerInternal.isPowered(7);
            this.mPlugType = this.mBatteryManagerInternal.getPlugType();
            this.mBatteryLevel = this.mBatteryManagerInternal.getBatteryLevel();
            this.mBatteryLevelLow = this.mBatteryManagerInternal.getBatteryLevelLow();
            if (z != this.mIsPowered || (i2 != this.mPlugType && (i2 == 0 || this.mPlugType == 0))) {
                this.mDirty |= 64;
                boolean zUpdate = this.mWirelessChargerDetector.update(this.mIsPowered, this.mPlugType);
                long jUptimeMillis = SystemClock.uptimeMillis();
                if (shouldWakeUpWhenPluggedOrUnpluggedLocked(z, i2, zUpdate)) {
                    wakeUpNoUpdateLocked(jUptimeMillis, "android.server.power:POWER", 1000, this.mContext.getOpPackageName(), 1000);
                }
                userActivityNoUpdateLocked(jUptimeMillis, 0, 0, 1000);
                if (this.mBootCompleted) {
                    if (this.mIsPowered && !BatteryManager.isPlugWired(i2) && BatteryManager.isPlugWired(this.mPlugType)) {
                        this.mNotifier.onWiredChargingStarted();
                    } else if (zUpdate) {
                        this.mNotifier.onWirelessChargingStarted(this.mBatteryLevel);
                    }
                }
            }
            this.mBatterySaverStateMachine.setBatteryStatus(this.mIsPowered, this.mBatteryLevel, this.mBatteryLevelLow);
        }
    }

    private boolean shouldWakeUpWhenPluggedOrUnpluggedLocked(boolean z, int i, boolean z2) {
        if (!this.mWakeUpWhenPluggedOrUnpluggedConfig) {
            return false;
        }
        if (z && !this.mIsPowered && i == 4) {
            return false;
        }
        if (!z && this.mIsPowered && this.mPlugType == 4 && !z2) {
            return false;
        }
        if (this.mIsPowered && this.mWakefulness == 2) {
            return false;
        }
        if (!this.mTheaterModeEnabled || this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig) {
            return (this.mAlwaysOnEnabled && this.mWakefulness == 3) ? false : true;
        }
        return false;
    }

    private void updateStayOnLocked(int i) {
        if ((i & 288) != 0) {
            boolean z = this.mStayOn;
            if (this.mStayOnWhilePluggedInSetting != 0 && !isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
                this.mStayOn = this.mBatteryManagerInternal.isPowered(this.mStayOnWhilePluggedInSetting);
            } else {
                this.mStayOn = false;
            }
            if (this.mStayOn != z) {
                this.mDirty |= 128;
            }
        }
    }

    private void updateWakeLockSummaryLocked(int i) {
        if ((i & 3) != 0) {
            this.mWakeLockSummary = 0;
            int size = this.mProfilePowerState.size();
            for (int i2 = 0; i2 < size; i2++) {
                this.mProfilePowerState.valueAt(i2).mWakeLockSummary = 0;
            }
            int size2 = this.mWakeLocks.size();
            for (int i3 = 0; i3 < size2; i3++) {
                WakeLock wakeLock = this.mWakeLocks.get(i3);
                int wakeLockSummaryFlags = getWakeLockSummaryFlags(wakeLock);
                this.mWakeLockSummary |= wakeLockSummaryFlags;
                for (int i4 = 0; i4 < size; i4++) {
                    ProfilePowerState profilePowerStateValueAt = this.mProfilePowerState.valueAt(i4);
                    if (wakeLockAffectsUser(wakeLock, profilePowerStateValueAt.mUserId)) {
                        profilePowerStateValueAt.mWakeLockSummary |= wakeLockSummaryFlags;
                    }
                }
            }
            this.mWakeLockSummary = adjustWakeLockSummaryLocked(this.mWakeLockSummary);
            for (int i5 = 0; i5 < size; i5++) {
                ProfilePowerState profilePowerStateValueAt2 = this.mProfilePowerState.valueAt(i5);
                profilePowerStateValueAt2.mWakeLockSummary = adjustWakeLockSummaryLocked(profilePowerStateValueAt2.mWakeLockSummary);
            }
        }
    }

    private int adjustWakeLockSummaryLocked(int i) {
        if (this.mWakefulness != 3) {
            i &= -193;
        }
        if (this.mWakefulness == 0 || (i & 64) != 0) {
            i &= -15;
            if (this.mWakefulness == 0) {
                i &= -17;
            }
        }
        if ((i & 6) != 0) {
            if (this.mWakefulness == 1) {
                i |= 33;
            } else if (this.mWakefulness == 2) {
                i |= 1;
            }
        }
        if ((i & 128) != 0) {
            return i | 1;
        }
        return i;
    }

    private int getWakeLockSummaryFlags(WakeLock wakeLock) {
        int i = wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
        if (i == 1) {
            return !wakeLock.mDisabled ? 1 : 0;
        }
        if (i == 6) {
            return 4;
        }
        if (i == 10) {
            return 2;
        }
        if (i == 26) {
            return 10;
        }
        if (i == 32) {
            return 16;
        }
        if (i != 64) {
            return i != 128 ? 0 : 128;
        }
        return 64;
    }

    private boolean wakeLockAffectsUser(WakeLock wakeLock, int i) {
        if (wakeLock.mWorkSource != null) {
            for (int i2 = 0; i2 < wakeLock.mWorkSource.size(); i2++) {
                if (i == UserHandle.getUserId(wakeLock.mWorkSource.get(i2))) {
                    return true;
                }
            }
            ArrayList workChains = wakeLock.mWorkSource.getWorkChains();
            if (workChains != null) {
                for (int i3 = 0; i3 < workChains.size(); i3++) {
                    if (i == UserHandle.getUserId(((WorkSource.WorkChain) workChains.get(i3)).getAttributionUid())) {
                        return true;
                    }
                }
            }
        }
        return i == UserHandle.getUserId(wakeLock.mOwnerUid);
    }

    void checkForLongWakeLocks() {
        synchronized (this.mLock) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            this.mNotifyLongDispatched = jUptimeMillis;
            long j = jUptimeMillis - 60000;
            int size = this.mWakeLocks.size();
            long j2 = Long.MAX_VALUE;
            for (int i = 0; i < size; i++) {
                WakeLock wakeLock = this.mWakeLocks.get(i);
                if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 1 && wakeLock.mNotifiedAcquired && !wakeLock.mNotifiedLong) {
                    if (wakeLock.mAcquireTime >= j) {
                        long j3 = wakeLock.mAcquireTime + 60000;
                        if (j3 < j2) {
                            j2 = j3;
                        }
                    } else {
                        notifyWakeLockLongStartedLocked(wakeLock);
                    }
                }
            }
            this.mNotifyLongScheduled = 0L;
            this.mHandler.removeMessages(4);
            if (j2 != JobStatus.NO_LATEST_RUNTIME) {
                this.mNotifyLongNextCheck = j2;
                enqueueNotifyLongMsgLocked(j2);
            } else {
                this.mNotifyLongNextCheck = 0L;
            }
        }
    }

    private void updateUserActivitySummaryLocked(long j, int i) {
        long jMin;
        long j2;
        int i2;
        if ((i & 39) != 0) {
            this.mHandler.removeMessages(1);
            if (this.mWakefulness == 1 || this.mWakefulness == 2 || this.mWakefulness == 3) {
                long sleepTimeoutLocked = getSleepTimeoutLocked();
                long screenOffTimeoutLocked = getScreenOffTimeoutLocked(sleepTimeoutLocked);
                long screenDimDurationLocked = getScreenDimDurationLocked(screenOffTimeoutLocked);
                boolean z = this.mUserInactiveOverrideFromWindowManager;
                long nextProfileTimeoutLocked = getNextProfileTimeoutLocked(j);
                this.mUserActivitySummary = 0;
                if (this.mLastUserActivityTime >= this.mLastWakeTime) {
                    long j3 = (this.mLastUserActivityTime + screenOffTimeoutLocked) - screenDimDurationLocked;
                    if (j < j3) {
                        this.mUserActivitySummary = 1;
                        jMin = j3;
                    } else {
                        jMin = this.mLastUserActivityTime + screenOffTimeoutLocked;
                        if (j < jMin) {
                            this.mUserActivitySummary = 2;
                        }
                    }
                } else {
                    jMin = 0;
                }
                if (this.mUserActivitySummary == 0) {
                    j2 = sleepTimeoutLocked;
                    if (this.mLastUserActivityTimeNoChangeLights >= this.mLastWakeTime) {
                        jMin = this.mLastUserActivityTimeNoChangeLights + screenOffTimeoutLocked;
                        if (j < jMin) {
                            if (this.mDisplayPowerRequest.policy == 3 || this.mDisplayPowerRequest.policy == 4) {
                                this.mUserActivitySummary = 1;
                            } else if (this.mDisplayPowerRequest.policy == 2) {
                                this.mUserActivitySummary = 2;
                            }
                        }
                    }
                } else {
                    j2 = sleepTimeoutLocked;
                }
                if (this.mUserActivitySummary != 0) {
                    i2 = 4;
                } else if (j2 >= 0) {
                    long jMax = Math.max(this.mLastUserActivityTime, this.mLastUserActivityTimeNoChangeLights);
                    if (jMax >= this.mLastWakeTime) {
                        long j4 = jMax + j2;
                        if (j < j4) {
                            i2 = 4;
                            this.mUserActivitySummary = 4;
                        } else {
                            i2 = 4;
                        }
                        jMin = j4;
                    } else {
                        i2 = 4;
                    }
                } else {
                    i2 = 4;
                    this.mUserActivitySummary = 4;
                    jMin = -1;
                }
                if (this.mUserActivitySummary != i2 && z) {
                    if ((this.mUserActivitySummary & 3) != 0 && jMin >= j && this.mOverriddenTimeout == -1) {
                        this.mOverriddenTimeout = jMin;
                    }
                    this.mUserActivitySummary = 4;
                    jMin = -1;
                }
                if (nextProfileTimeoutLocked > 0) {
                    jMin = Math.min(jMin, nextProfileTimeoutLocked);
                }
                if (this.mUserActivitySummary != 0 && jMin >= 0) {
                    scheduleUserInactivityTimeout(jMin);
                    return;
                }
                return;
            }
            this.mUserActivitySummary = 0;
        }
    }

    private void scheduleUserInactivityTimeout(long j) {
        Message messageObtainMessage = this.mHandler.obtainMessage(1);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessageAtTime(messageObtainMessage, j);
    }

    private long getNextProfileTimeoutLocked(long j) {
        int size = this.mProfilePowerState.size();
        long j2 = -1;
        for (int i = 0; i < size; i++) {
            ProfilePowerState profilePowerStateValueAt = this.mProfilePowerState.valueAt(i);
            long j3 = profilePowerStateValueAt.mLastUserActivityTime + profilePowerStateValueAt.mScreenOffTimeout;
            if (j3 > j && (j2 == -1 || j3 < j2)) {
                j2 = j3;
            }
        }
        return j2;
    }

    private void handleUserActivityTimeout() {
        synchronized (this.mLock) {
            this.mDirty |= 4;
            updatePowerStateLocked();
        }
    }

    private long getSleepTimeoutLocked() {
        long j = this.mSleepTimeoutSetting;
        if (j <= 0) {
            return -1L;
        }
        return Math.max(j, this.mMinimumScreenOffTimeoutConfig);
    }

    private long getScreenOffTimeoutLocked(long j) {
        long jMin = this.mScreenOffTimeoutSetting;
        if (isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
            jMin = Math.min(jMin, this.mMaximumScreenOffTimeoutFromDeviceAdmin);
        }
        if (this.mUserActivityTimeoutOverrideFromWindowManager >= 0) {
            jMin = Math.min(jMin, this.mUserActivityTimeoutOverrideFromWindowManager);
        }
        if (j >= 0) {
            jMin = Math.min(jMin, j);
        }
        return Math.max(jMin, this.mMinimumScreenOffTimeoutConfig);
    }

    private long getScreenDimDurationLocked(long j) {
        if (this.mScreenDimTimeoutSetting > 0) {
            return Math.max(j - this.mScreenDimTimeoutSetting, 0L);
        }
        return Math.min(this.mMaximumScreenDimDurationConfig, (long) (j * this.mMaximumScreenDimRatioConfig));
    }

    private boolean updateWakefulnessLocked(int i) {
        if ((i & 1687) != 0 && this.mWakefulness == 1 && isItBedTimeYetLocked()) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (shouldNapAtBedTimeLocked()) {
                return napNoUpdateLocked(jUptimeMillis, 1000);
            }
            return goToSleepNoUpdateLocked(jUptimeMillis, 2, 0, 1000);
        }
        return false;
    }

    private boolean shouldNapAtBedTimeLocked() {
        return this.mDreamsActivateOnSleepSetting || (this.mDreamsActivateOnDockSetting && this.mDockState != 0);
    }

    private boolean isItBedTimeYetLocked() {
        return this.mBootCompleted && !isBeingKeptAwakeLocked();
    }

    private boolean isBeingKeptAwakeLocked() {
        return this.mStayOn || this.mProximityPositive || (this.mWakeLockSummary & 32) != 0 || (this.mUserActivitySummary & 3) != 0 || this.mScreenBrightnessBoostInProgress;
    }

    private void updateDreamLocked(int i, boolean z) {
        if (((i & 1015) != 0 || z) && this.mDisplayReady) {
            scheduleSandmanLocked();
        }
    }

    private void scheduleSandmanLocked() {
        if (!this.mSandmanScheduled) {
            this.mSandmanScheduled = true;
            Message messageObtainMessage = this.mHandler.obtainMessage(2);
            messageObtainMessage.setAsynchronous(true);
            this.mHandler.sendMessage(messageObtainMessage);
        }
    }

    private void handleSandman() {
        int i;
        boolean z;
        boolean zIsDreaming;
        synchronized (this.mLock) {
            this.mSandmanScheduled = false;
            i = this.mWakefulness;
            if (this.mSandmanSummoned && this.mDisplayReady) {
                z = canDreamLocked() || canDozeLocked();
                this.mSandmanSummoned = false;
            } else {
                z = false;
            }
        }
        if (this.mDreamManager != null) {
            if (z) {
                this.mDreamManager.stopDream(false);
                this.mDreamManager.startDream(i == 3);
            }
            zIsDreaming = this.mDreamManager.isDreaming();
        } else {
            zIsDreaming = false;
        }
        synchronized (this.mLock) {
            if (z && zIsDreaming) {
                this.mBatteryLevelWhenDreamStarted = this.mBatteryLevel;
                if (i == 3) {
                    Slog.i(TAG, "Dozing...");
                } else {
                    Slog.i(TAG, "Dreaming...");
                }
            }
            if (!this.mSandmanSummoned && this.mWakefulness == i) {
                if (i == 2) {
                    if (zIsDreaming && canDreamLocked()) {
                        if (this.mDreamsBatteryLevelDrainCutoffConfig < 0 || this.mBatteryLevel >= this.mBatteryLevelWhenDreamStarted - this.mDreamsBatteryLevelDrainCutoffConfig || isBeingKeptAwakeLocked()) {
                            return;
                        }
                        Slog.i(TAG, "Stopping dream because the battery appears to be draining faster than it is charging.  Battery level when dream started: " + this.mBatteryLevelWhenDreamStarted + "%.  Battery level now: " + this.mBatteryLevel + "%.");
                    }
                    if (isItBedTimeYetLocked()) {
                        goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 2, 0, 1000);
                        updatePowerStateLocked();
                    } else {
                        wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), "android.server.power:DREAM", 1000, this.mContext.getOpPackageName(), 1000);
                        updatePowerStateLocked();
                    }
                } else if (i == 3) {
                    if (zIsDreaming) {
                        return;
                    }
                    reallyGoToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 1000);
                    updatePowerStateLocked();
                }
                if (zIsDreaming) {
                    this.mDreamManager.stopDream(false);
                }
            }
        }
    }

    private boolean canDreamLocked() {
        if (this.mWakefulness != 2 || !this.mDreamsSupportedConfig || !this.mDreamsEnabledSetting || !this.mDisplayPowerRequest.isBrightOrDim() || this.mDisplayPowerRequest.isVr() || (this.mUserActivitySummary & 7) == 0 || !this.mBootCompleted) {
            return false;
        }
        if (!isBeingKeptAwakeLocked()) {
            if (!this.mIsPowered && !this.mDreamsEnabledOnBatteryConfig) {
                return false;
            }
            if (this.mIsPowered || this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig < 0 || this.mBatteryLevel >= this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig) {
                return !this.mIsPowered || this.mDreamsBatteryLevelMinimumWhenPoweredConfig < 0 || this.mBatteryLevel >= this.mDreamsBatteryLevelMinimumWhenPoweredConfig;
            }
            return false;
        }
        return true;
    }

    private boolean canDozeLocked() {
        return this.mWakefulness == 3;
    }

    private boolean updateDisplayPowerStateLocked(int i) {
        boolean z;
        int i2;
        boolean z2 = this.mDisplayReady;
        if ((i & 14399) != 0) {
            this.mDisplayPowerRequest.policy = getDesiredScreenPolicyLocked();
            if (!this.mBootCompleted) {
                i2 = this.mScreenBrightnessSettingDefault;
            } else if (isValidBrightness(this.mScreenBrightnessOverrideFromWindowManager)) {
                i2 = this.mScreenBrightnessOverrideFromWindowManager;
            } else {
                z = this.mScreenBrightnessModeSetting == 1;
                i2 = -1;
                this.mDisplayPowerRequest.screenBrightnessOverride = i2;
                this.mDisplayPowerRequest.useAutoBrightness = z;
                this.mDisplayPowerRequest.useProximitySensor = shouldUseProximitySensorLocked();
                this.mDisplayPowerRequest.boostScreenBrightness = shouldBoostScreenBrightness();
                updatePowerRequestFromBatterySaverPolicy(this.mDisplayPowerRequest);
                if (this.mDisplayPowerRequest.policy != 1) {
                    this.mDisplayPowerRequest.dozeScreenState = this.mDozeScreenStateOverrideFromDreamManager;
                    if ((this.mWakeLockSummary & 128) != 0 && !this.mDrawWakeLockOverrideFromSidekick) {
                        if (this.mDisplayPowerRequest.dozeScreenState == 4) {
                            this.mDisplayPowerRequest.dozeScreenState = 3;
                        }
                        if (this.mDisplayPowerRequest.dozeScreenState == 6) {
                            this.mDisplayPowerRequest.dozeScreenState = 2;
                        }
                    }
                    this.mDisplayPowerRequest.dozeScreenBrightness = this.mDozeScreenBrightnessOverrideFromDreamManager;
                } else {
                    this.mDisplayPowerRequest.dozeScreenState = 0;
                    this.mDisplayPowerRequest.dozeScreenBrightness = -1;
                }
                this.mDisplayReady = this.mDisplayManagerInternal.requestPowerState(this.mDisplayPowerRequest, this.mRequestWaitForNegativeProximity);
                this.mRequestWaitForNegativeProximity = false;
                if ((i & 4096) != 0) {
                    sQuiescent = false;
                }
            }
            z = false;
            this.mDisplayPowerRequest.screenBrightnessOverride = i2;
            this.mDisplayPowerRequest.useAutoBrightness = z;
            this.mDisplayPowerRequest.useProximitySensor = shouldUseProximitySensorLocked();
            this.mDisplayPowerRequest.boostScreenBrightness = shouldBoostScreenBrightness();
            updatePowerRequestFromBatterySaverPolicy(this.mDisplayPowerRequest);
            if (this.mDisplayPowerRequest.policy != 1) {
            }
            this.mDisplayReady = this.mDisplayManagerInternal.requestPowerState(this.mDisplayPowerRequest, this.mRequestWaitForNegativeProximity);
            this.mRequestWaitForNegativeProximity = false;
            if ((i & 4096) != 0) {
            }
        }
        return this.mDisplayReady && !z2;
    }

    private void updateScreenBrightnessBoostLocked(int i) {
        if ((i & 2048) != 0 && this.mScreenBrightnessBoostInProgress) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            this.mHandler.removeMessages(3);
            if (this.mLastScreenBrightnessBoostTime > this.mLastSleepTime) {
                long j = this.mLastScreenBrightnessBoostTime + 5000;
                if (j > jUptimeMillis) {
                    Message messageObtainMessage = this.mHandler.obtainMessage(3);
                    messageObtainMessage.setAsynchronous(true);
                    this.mHandler.sendMessageAtTime(messageObtainMessage, j);
                    return;
                }
            }
            this.mScreenBrightnessBoostInProgress = false;
            this.mNotifier.onScreenBrightnessBoostChanged();
            userActivityNoUpdateLocked(jUptimeMillis, 0, 0, 1000);
        }
    }

    private boolean shouldBoostScreenBrightness() {
        return !this.mIsVrModeEnabled && this.mScreenBrightnessBoostInProgress;
    }

    private static boolean isValidBrightness(int i) {
        return i >= 0 && i <= 255;
    }

    @VisibleForTesting
    int getDesiredScreenPolicyLocked() {
        if (this.mWakefulness == 0 || sQuiescent || this.mShutdownFlag) {
            return 0;
        }
        if (this.mWakefulness == 3) {
            if ((this.mWakeLockSummary & 64) != 0) {
                return 1;
            }
            if (this.mDozeAfterScreenOff) {
                return 0;
            }
        }
        if (this.mIsVrModeEnabled) {
            return 4;
        }
        return ((this.mWakeLockSummary & 2) == 0 && (this.mUserActivitySummary & 1) == 0 && this.mBootCompleted && !this.mScreenBrightnessBoostInProgress) ? 2 : 3;
    }

    private boolean shouldUseProximitySensorLocked() {
        return (this.mIsVrModeEnabled || (this.mWakeLockSummary & 16) == 0) ? false : true;
    }

    private void updateSuspendBlockerLocked() {
        boolean z = (this.mWakeLockSummary & 1) != 0;
        boolean zNeedDisplaySuspendBlockerLocked = needDisplaySuspendBlockerLocked();
        boolean z2 = !zNeedDisplaySuspendBlockerLocked;
        boolean zIsBrightOrDim = this.mDisplayPowerRequest.isBrightOrDim();
        if (!z2 && this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            setHalAutoSuspendModeLocked(false);
        }
        if (z && !this.mHoldingWakeLockSuspendBlocker) {
            this.mWakeLockSuspendBlocker.acquire();
            this.mHoldingWakeLockSuspendBlocker = true;
        }
        if (zNeedDisplaySuspendBlockerLocked && !this.mHoldingDisplaySuspendBlocker) {
            this.mDisplaySuspendBlocker.acquire();
            this.mHoldingDisplaySuspendBlocker = true;
        }
        if (this.mDecoupleHalInteractiveModeFromDisplayConfig && (zIsBrightOrDim || this.mDisplayReady)) {
            setHalInteractiveModeLocked(zIsBrightOrDim);
        }
        if (!z && this.mHoldingWakeLockSuspendBlocker) {
            this.mWakeLockSuspendBlocker.release();
            this.mHoldingWakeLockSuspendBlocker = false;
        }
        if (!zNeedDisplaySuspendBlockerLocked && this.mHoldingDisplaySuspendBlocker) {
            this.mDisplaySuspendBlocker.release();
            this.mHoldingDisplaySuspendBlocker = false;
        }
        if (z2 && this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            setHalAutoSuspendModeLocked(true);
        }
    }

    private boolean needDisplaySuspendBlockerLocked() {
        if (this.mDisplayReady) {
            return (this.mDisplayPowerRequest.isBrightOrDim() && !(this.mDisplayPowerRequest.useProximitySensor && this.mProximityPositive && this.mSuspendWhenScreenOffDueToProximityConfig)) || this.mScreenBrightnessBoostInProgress;
        }
        return true;
    }

    private void setHalAutoSuspendModeLocked(boolean z) {
        if (z != this.mHalAutoSuspendModeEnabled) {
            this.mHalAutoSuspendModeEnabled = z;
            Trace.traceBegin(131072L, "setHalAutoSuspend(" + z + ")");
            try {
                nativeSetAutoSuspend(z);
            } finally {
                Trace.traceEnd(131072L);
            }
        }
    }

    private void setHalInteractiveModeLocked(boolean z) {
        if (z != this.mHalInteractiveModeEnabled) {
            this.mHalInteractiveModeEnabled = z;
            Trace.traceBegin(131072L, "setHalInteractive(" + z + ")");
            try {
                nativeSetInteractive(z);
            } finally {
                Trace.traceEnd(131072L);
            }
        }
    }

    private boolean isInteractiveInternal() {
        boolean zIsInteractive;
        synchronized (this.mLock) {
            zIsInteractive = PowerManagerInternal.isInteractive(this.mWakefulness);
        }
        return zIsInteractive;
    }

    private boolean setLowPowerModeInternal(boolean z) {
        synchronized (this.mLock) {
            if (this.mIsPowered) {
                return false;
            }
            this.mBatterySaverStateMachine.setBatterySaverEnabledManually(z);
            return true;
        }
    }

    boolean isDeviceIdleModeInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceIdleMode;
        }
        return z;
    }

    boolean isLightDeviceIdleModeInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mLightDeviceIdleMode;
        }
        return z;
    }

    private void handleBatteryStateChangedLocked() {
        this.mDirty |= 256;
        updatePowerStateLocked();
    }

    private void shutdownOrRebootInternal(final int i, final boolean z, final String str, boolean z2) {
        if (this.mHandler == null || !this.mSystemReady) {
            if (RescueParty.isAttemptingFactoryReset()) {
                lowLevelReboot(str);
            } else {
                throw new IllegalStateException("Too early to call shutdown() or reboot()");
            }
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (i == 2) {
                        ShutdownThread.rebootSafeMode(PowerManagerService.this.getUiContext(), z);
                    } else if (i == 1) {
                        ShutdownThread.reboot(PowerManagerService.this.getUiContext(), str, z);
                    } else {
                        ShutdownThread.shutdown(PowerManagerService.this.getUiContext(), str, z);
                    }
                }
            }
        };
        Message messageObtain = Message.obtain(UiThread.getHandler(), runnable);
        messageObtain.setAsynchronous(true);
        UiThread.getHandler().sendMessage(messageObtain);
        if (z2) {
            synchronized (runnable) {
                while (true) {
                    try {
                        runnable.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    private void crashInternal(final String str) {
        Thread thread = new Thread("PowerManagerService.crash()") {
            @Override
            public void run() {
                throw new RuntimeException(str);
            }
        };
        try {
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            Slog.wtf(TAG, e);
        }
    }

    @VisibleForTesting
    void updatePowerRequestFromBatterySaverPolicy(DisplayManagerInternal.DisplayPowerRequest displayPowerRequest) {
        PowerSaveState batterySaverPolicy = this.mBatterySaverPolicy.getBatterySaverPolicy(7, this.mBatterySaverController.isEnabled());
        displayPowerRequest.lowPowerMode = batterySaverPolicy.batterySaverEnabled;
        displayPowerRequest.screenLowPowerBrightnessFactor = batterySaverPolicy.brightnessFactor;
    }

    void setStayOnSettingInternal(int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", i);
    }

    void setMaximumScreenOffTimeoutFromDeviceAdminInternal(int i, long j) {
        if (i < 0) {
            Slog.wtf(TAG, "Attempt to set screen off timeout for invalid user: " + i);
            return;
        }
        synchronized (this.mLock) {
            try {
                if (i == 0) {
                    this.mMaximumScreenOffTimeoutFromDeviceAdmin = j;
                } else if (j == JobStatus.NO_LATEST_RUNTIME || j == 0) {
                    this.mProfilePowerState.delete(i);
                } else {
                    ProfilePowerState profilePowerState = this.mProfilePowerState.get(i);
                    if (profilePowerState != null) {
                        profilePowerState.mScreenOffTimeout = j;
                    } else {
                        this.mProfilePowerState.put(i, new ProfilePowerState(i, j));
                        this.mDirty |= 1;
                    }
                }
                this.mDirty |= 32;
                updatePowerStateLocked();
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    boolean setDeviceIdleModeInternal(boolean z) {
        synchronized (this.mLock) {
            if (this.mDeviceIdleMode == z) {
                return false;
            }
            this.mDeviceIdleMode = z;
            updateWakeLockDisabledStatesLocked();
            if (z) {
                EventLogTags.writeDeviceIdleOnPhase("power");
                return true;
            }
            EventLogTags.writeDeviceIdleOffPhase("power");
            return true;
        }
    }

    boolean setLightDeviceIdleModeInternal(boolean z) {
        synchronized (this.mLock) {
            if (this.mLightDeviceIdleMode != z) {
                this.mLightDeviceIdleMode = z;
                return true;
            }
            return false;
        }
    }

    void setDeviceIdleWhitelistInternal(int[] iArr) {
        synchronized (this.mLock) {
            this.mDeviceIdleWhitelist = iArr;
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void setDeviceIdleTempWhitelistInternal(int[] iArr) {
        synchronized (this.mLock) {
            this.mDeviceIdleTempWhitelist = iArr;
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void startUidChangesInternal() {
        synchronized (this.mLock) {
            this.mUidsChanging = true;
        }
    }

    void finishUidChangesInternal() {
        synchronized (this.mLock) {
            this.mUidsChanging = false;
            if (this.mUidsChanged) {
                updateWakeLockDisabledStatesLocked();
                this.mUidsChanged = false;
            }
        }
    }

    private void handleUidStateChangeLocked() {
        if (this.mUidsChanging) {
            this.mUidsChanged = true;
        } else {
            updateWakeLockDisabledStatesLocked();
        }
    }

    void updateUidProcStateInternal(int i, int i2) {
        synchronized (this.mLock) {
            UidState uidState = this.mUidState.get(i);
            if (uidState == null) {
                uidState = new UidState(i);
                this.mUidState.put(i, uidState);
            }
            boolean z = uidState.mProcState <= 10;
            uidState.mProcState = i2;
            if (uidState.mNumWakeLocks > 0) {
                if (this.mDeviceIdleMode) {
                    handleUidStateChangeLocked();
                } else if (!uidState.mActive) {
                    if (z != (i2 <= 10)) {
                        handleUidStateChangeLocked();
                    }
                }
            }
        }
    }

    void uidGoneInternal(int i) {
        synchronized (this.mLock) {
            int iIndexOfKey = this.mUidState.indexOfKey(i);
            if (iIndexOfKey >= 0) {
                UidState uidStateValueAt = this.mUidState.valueAt(iIndexOfKey);
                uidStateValueAt.mProcState = 19;
                uidStateValueAt.mActive = false;
                this.mUidState.removeAt(iIndexOfKey);
                if (this.mDeviceIdleMode && uidStateValueAt.mNumWakeLocks > 0) {
                    handleUidStateChangeLocked();
                }
            }
        }
    }

    void uidActiveInternal(int i) {
        synchronized (this.mLock) {
            UidState uidState = this.mUidState.get(i);
            if (uidState == null) {
                uidState = new UidState(i);
                uidState.mProcState = 18;
                this.mUidState.put(i, uidState);
            }
            uidState.mActive = true;
            if (uidState.mNumWakeLocks > 0) {
                handleUidStateChangeLocked();
            }
        }
    }

    void uidIdleInternal(int i) {
        synchronized (this.mLock) {
            UidState uidState = this.mUidState.get(i);
            if (uidState != null) {
                uidState.mActive = false;
                if (uidState.mNumWakeLocks > 0) {
                    handleUidStateChangeLocked();
                }
            }
        }
    }

    private void updateWakeLockDisabledStatesLocked() {
        int size = this.mWakeLocks.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            WakeLock wakeLock = this.mWakeLocks.get(i);
            if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 1 && setWakeLockDisabledStateLocked(wakeLock)) {
                if (wakeLock.mDisabled) {
                    notifyWakeLockReleasedLocked(wakeLock);
                } else {
                    notifyWakeLockAcquiredLocked(wakeLock);
                }
                z = true;
            }
        }
        if (z) {
            this.mDirty |= 1;
            updatePowerStateLocked();
        }
    }

    private boolean setWakeLockDisabledStateLocked(WakeLock wakeLock) {
        boolean z;
        if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 1) {
            int appId = UserHandle.getAppId(wakeLock.mOwnerUid);
            if (appId >= 10000) {
                z = this.mConstants.NO_CACHED_WAKE_LOCKS && !wakeLock.mUidState.mActive && wakeLock.mUidState.mProcState != 19 && wakeLock.mUidState.mProcState > 10;
                if (this.mDeviceIdleMode) {
                    UidState uidState = wakeLock.mUidState;
                    if (Arrays.binarySearch(this.mDeviceIdleWhitelist, appId) < 0 && Arrays.binarySearch(this.mDeviceIdleTempWhitelist, appId) < 0 && uidState.mProcState != 19 && uidState.mProcState > 4) {
                        z = true;
                    }
                }
            } else {
                z = false;
            }
            if (wakeLock.mDisabled != z) {
                wakeLock.mDisabled = z;
                return true;
            }
        }
        return false;
    }

    private boolean isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked() {
        return this.mMaximumScreenOffTimeoutFromDeviceAdmin >= 0 && this.mMaximumScreenOffTimeoutFromDeviceAdmin < JobStatus.NO_LATEST_RUNTIME;
    }

    private void setAttentionLightInternal(boolean z, int i) {
        synchronized (this.mLock) {
            if (this.mSystemReady) {
                this.mAttentionLight.setFlashing(i, 2, z ? 3 : 0, 0);
            }
        }
    }

    private void setDozeAfterScreenOffInternal(boolean z) {
        synchronized (this.mLock) {
            this.mDozeAfterScreenOff = z;
        }
    }

    private void boostScreenBrightnessInternal(long j, int i) {
        synchronized (this.mLock) {
            if (this.mSystemReady && this.mWakefulness != 0 && j >= this.mLastScreenBrightnessBoostTime) {
                Slog.i(TAG, "Brightness boost activated (uid " + i + ")...");
                this.mLastScreenBrightnessBoostTime = j;
                if (!this.mScreenBrightnessBoostInProgress) {
                    this.mScreenBrightnessBoostInProgress = true;
                    this.mNotifier.onScreenBrightnessBoostChanged();
                }
                this.mDirty |= 2048;
                userActivityNoUpdateLocked(j, 0, 0, i);
                updatePowerStateLocked();
            }
        }
    }

    private boolean isScreenBrightnessBoostedInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mScreenBrightnessBoostInProgress;
        }
        return z;
    }

    private void handleScreenBrightnessBoostTimeout() {
        synchronized (this.mLock) {
            this.mDirty |= 2048;
            updatePowerStateLocked();
        }
    }

    private void setScreenBrightnessOverrideFromWindowManagerInternal(int i) {
        synchronized (this.mLock) {
            if (this.mScreenBrightnessOverrideFromWindowManager != i) {
                this.mScreenBrightnessOverrideFromWindowManager = i;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setUserInactiveOverrideFromWindowManagerInternal() {
        synchronized (this.mLock) {
            this.mUserInactiveOverrideFromWindowManager = true;
            this.mDirty |= 4;
            updatePowerStateLocked();
        }
    }

    private void setUserActivityTimeoutOverrideFromWindowManagerInternal(long j) {
        synchronized (this.mLock) {
            if (this.mUserActivityTimeoutOverrideFromWindowManager != j) {
                this.mUserActivityTimeoutOverrideFromWindowManager = j;
                EventLogTags.writeUserActivityTimeoutOverride(j);
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setDozeOverrideFromDreamManagerInternal(int i, int i2) {
        synchronized (this.mLock) {
            if (this.mDozeScreenStateOverrideFromDreamManager != i || this.mDozeScreenBrightnessOverrideFromDreamManager != i2) {
                this.mDozeScreenStateOverrideFromDreamManager = i;
                this.mDozeScreenBrightnessOverrideFromDreamManager = i2;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setDrawWakeLockOverrideFromSidekickInternal(boolean z) {
        synchronized (this.mLock) {
            if (this.mDrawWakeLockOverrideFromSidekick != z) {
                this.mDrawWakeLockOverrideFromSidekick = z;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    @VisibleForTesting
    void setVrModeEnabled(boolean z) {
        this.mIsVrModeEnabled = z;
    }

    private void powerHintInternal(int i, int i2) {
        if (i == 8 && i2 == 1 && this.mBatterySaverController.isLaunchBoostDisabled()) {
            return;
        }
        nativeSendPowerHint(i, i2);
    }

    public static void lowLevelShutdown(String str) {
        if (str == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        SystemProperties.set("sys.powerctl", "shutdown," + str);
    }

    public static void lowLevelReboot(String str) {
        if (str == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        if (str.equals("quiescent")) {
            sQuiescent = true;
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else if (str.endsWith(",quiescent")) {
            sQuiescent = true;
            str = str.substring(0, (str.length() - "quiescent".length()) - 1);
        }
        if (str.equals("recovery") || str.equals("recovery-update")) {
            str = "recovery";
        }
        if (sQuiescent) {
            str = str + ",quiescent";
        }
        SystemProperties.set("sys.powerctl", "reboot," + str);
        try {
            Thread.sleep(20000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Slog.wtf(TAG, "Unexpected return from lowLevelReboot!");
    }

    @Override
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    private void dumpInternal(PrintWriter printWriter) {
        WirelessChargerDetector wirelessChargerDetector;
        printWriter.println("POWER MANAGER (dumpsys power)\n");
        synchronized (this.mLock) {
            printWriter.println("Power Manager State:");
            this.mConstants.dump(printWriter);
            printWriter.println("  mDirty=0x" + Integer.toHexString(this.mDirty));
            printWriter.println("  mWakefulness=" + PowerManagerInternal.wakefulnessToString(this.mWakefulness));
            printWriter.println("  mWakefulnessChanging=" + this.mWakefulnessChanging);
            printWriter.println("  mIsPowered=" + this.mIsPowered);
            printWriter.println("  mPlugType=" + this.mPlugType);
            printWriter.println("  mBatteryLevel=" + this.mBatteryLevel);
            printWriter.println("  mBatteryLevelWhenDreamStarted=" + this.mBatteryLevelWhenDreamStarted);
            printWriter.println("  mDockState=" + this.mDockState);
            printWriter.println("  mStayOn=" + this.mStayOn);
            printWriter.println("  mProximityPositive=" + this.mProximityPositive);
            printWriter.println("  mBootCompleted=" + this.mBootCompleted);
            printWriter.println("  mSystemReady=" + this.mSystemReady);
            printWriter.println("  mHalAutoSuspendModeEnabled=" + this.mHalAutoSuspendModeEnabled);
            printWriter.println("  mHalInteractiveModeEnabled=" + this.mHalInteractiveModeEnabled);
            printWriter.println("  mWakeLockSummary=0x" + Integer.toHexString(this.mWakeLockSummary));
            printWriter.print("  mNotifyLongScheduled=");
            if (this.mNotifyLongScheduled == 0) {
                printWriter.print("(none)");
            } else {
                TimeUtils.formatDuration(this.mNotifyLongScheduled, SystemClock.uptimeMillis(), printWriter);
            }
            printWriter.println();
            printWriter.print("  mNotifyLongDispatched=");
            if (this.mNotifyLongDispatched == 0) {
                printWriter.print("(none)");
            } else {
                TimeUtils.formatDuration(this.mNotifyLongDispatched, SystemClock.uptimeMillis(), printWriter);
            }
            printWriter.println();
            printWriter.print("  mNotifyLongNextCheck=");
            if (this.mNotifyLongNextCheck == 0) {
                printWriter.print("(none)");
            } else {
                TimeUtils.formatDuration(this.mNotifyLongNextCheck, SystemClock.uptimeMillis(), printWriter);
            }
            printWriter.println();
            printWriter.println("  mUserActivitySummary=0x" + Integer.toHexString(this.mUserActivitySummary));
            printWriter.println("  mRequestWaitForNegativeProximity=" + this.mRequestWaitForNegativeProximity);
            printWriter.println("  mSandmanScheduled=" + this.mSandmanScheduled);
            printWriter.println("  mSandmanSummoned=" + this.mSandmanSummoned);
            printWriter.println("  mBatteryLevelLow=" + this.mBatteryLevelLow);
            printWriter.println("  mLightDeviceIdleMode=" + this.mLightDeviceIdleMode);
            printWriter.println("  mDeviceIdleMode=" + this.mDeviceIdleMode);
            printWriter.println("  mDeviceIdleWhitelist=" + Arrays.toString(this.mDeviceIdleWhitelist));
            printWriter.println("  mDeviceIdleTempWhitelist=" + Arrays.toString(this.mDeviceIdleTempWhitelist));
            printWriter.println("  mLastWakeTime=" + TimeUtils.formatUptime(this.mLastWakeTime));
            printWriter.println("  mLastSleepTime=" + TimeUtils.formatUptime(this.mLastSleepTime));
            printWriter.println("  mLastUserActivityTime=" + TimeUtils.formatUptime(this.mLastUserActivityTime));
            printWriter.println("  mLastUserActivityTimeNoChangeLights=" + TimeUtils.formatUptime(this.mLastUserActivityTimeNoChangeLights));
            printWriter.println("  mLastInteractivePowerHintTime=" + TimeUtils.formatUptime(this.mLastInteractivePowerHintTime));
            printWriter.println("  mLastScreenBrightnessBoostTime=" + TimeUtils.formatUptime(this.mLastScreenBrightnessBoostTime));
            printWriter.println("  mScreenBrightnessBoostInProgress=" + this.mScreenBrightnessBoostInProgress);
            printWriter.println("  mDisplayReady=" + this.mDisplayReady);
            printWriter.println("  mHoldingWakeLockSuspendBlocker=" + this.mHoldingWakeLockSuspendBlocker);
            printWriter.println("  mHoldingDisplaySuspendBlocker=" + this.mHoldingDisplaySuspendBlocker);
            printWriter.println();
            printWriter.println("Settings and Configuration:");
            printWriter.println("  mDecoupleHalAutoSuspendModeFromDisplayConfig=" + this.mDecoupleHalAutoSuspendModeFromDisplayConfig);
            printWriter.println("  mDecoupleHalInteractiveModeFromDisplayConfig=" + this.mDecoupleHalInteractiveModeFromDisplayConfig);
            printWriter.println("  mWakeUpWhenPluggedOrUnpluggedConfig=" + this.mWakeUpWhenPluggedOrUnpluggedConfig);
            printWriter.println("  mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig=" + this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig);
            printWriter.println("  mTheaterModeEnabled=" + this.mTheaterModeEnabled);
            printWriter.println("  mSuspendWhenScreenOffDueToProximityConfig=" + this.mSuspendWhenScreenOffDueToProximityConfig);
            printWriter.println("  mDreamsSupportedConfig=" + this.mDreamsSupportedConfig);
            printWriter.println("  mDreamsEnabledByDefaultConfig=" + this.mDreamsEnabledByDefaultConfig);
            printWriter.println("  mDreamsActivatedOnSleepByDefaultConfig=" + this.mDreamsActivatedOnSleepByDefaultConfig);
            printWriter.println("  mDreamsActivatedOnDockByDefaultConfig=" + this.mDreamsActivatedOnDockByDefaultConfig);
            printWriter.println("  mDreamsEnabledOnBatteryConfig=" + this.mDreamsEnabledOnBatteryConfig);
            printWriter.println("  mDreamsBatteryLevelMinimumWhenPoweredConfig=" + this.mDreamsBatteryLevelMinimumWhenPoweredConfig);
            printWriter.println("  mDreamsBatteryLevelMinimumWhenNotPoweredConfig=" + this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig);
            printWriter.println("  mDreamsBatteryLevelDrainCutoffConfig=" + this.mDreamsBatteryLevelDrainCutoffConfig);
            printWriter.println("  mDreamsEnabledSetting=" + this.mDreamsEnabledSetting);
            printWriter.println("  mDreamsActivateOnSleepSetting=" + this.mDreamsActivateOnSleepSetting);
            printWriter.println("  mDreamsActivateOnDockSetting=" + this.mDreamsActivateOnDockSetting);
            printWriter.println("  mDozeAfterScreenOff=" + this.mDozeAfterScreenOff);
            printWriter.println("  mMinimumScreenOffTimeoutConfig=" + this.mMinimumScreenOffTimeoutConfig);
            printWriter.println("  mMaximumScreenDimDurationConfig=" + this.mMaximumScreenDimDurationConfig);
            printWriter.println("  mMaximumScreenDimRatioConfig=" + this.mMaximumScreenDimRatioConfig);
            printWriter.println("  mScreenOffTimeoutSetting=" + this.mScreenOffTimeoutSetting);
            printWriter.println("  mSleepTimeoutSetting=" + this.mSleepTimeoutSetting);
            printWriter.println("  mMaximumScreenOffTimeoutFromDeviceAdmin=" + this.mMaximumScreenOffTimeoutFromDeviceAdmin + " (enforced=" + isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked() + ")");
            StringBuilder sb = new StringBuilder();
            sb.append("  mStayOnWhilePluggedInSetting=");
            sb.append(this.mStayOnWhilePluggedInSetting);
            printWriter.println(sb.toString());
            printWriter.println("  mScreenBrightnessSetting=" + this.mScreenBrightnessSetting);
            printWriter.println("  mScreenBrightnessModeSetting=" + this.mScreenBrightnessModeSetting);
            printWriter.println("  mScreenBrightnessOverrideFromWindowManager=" + this.mScreenBrightnessOverrideFromWindowManager);
            printWriter.println("  mUserActivityTimeoutOverrideFromWindowManager=" + this.mUserActivityTimeoutOverrideFromWindowManager);
            printWriter.println("  mUserInactiveOverrideFromWindowManager=" + this.mUserInactiveOverrideFromWindowManager);
            printWriter.println("  mDozeScreenStateOverrideFromDreamManager=" + this.mDozeScreenStateOverrideFromDreamManager);
            printWriter.println("  mDrawWakeLockOverrideFromSidekick=" + this.mDrawWakeLockOverrideFromSidekick);
            printWriter.println("  mDozeScreenBrightnessOverrideFromDreamManager=" + this.mDozeScreenBrightnessOverrideFromDreamManager);
            printWriter.println("  mScreenBrightnessSettingMinimum=" + this.mScreenBrightnessSettingMinimum);
            printWriter.println("  mScreenBrightnessSettingMaximum=" + this.mScreenBrightnessSettingMaximum);
            printWriter.println("  mScreenBrightnessSettingDefault=" + this.mScreenBrightnessSettingDefault);
            printWriter.println("  mDoubleTapWakeEnabled=" + this.mDoubleTapWakeEnabled);
            printWriter.println("  mIsVrModeEnabled=" + this.mIsVrModeEnabled);
            printWriter.println("  mForegroundProfile=" + this.mForegroundProfile);
            long sleepTimeoutLocked = getSleepTimeoutLocked();
            long screenOffTimeoutLocked = getScreenOffTimeoutLocked(sleepTimeoutLocked);
            long screenDimDurationLocked = getScreenDimDurationLocked(screenOffTimeoutLocked);
            printWriter.println();
            printWriter.println("Sleep timeout: " + sleepTimeoutLocked + " ms");
            printWriter.println("Screen off timeout: " + screenOffTimeoutLocked + " ms");
            printWriter.println("Screen dim duration: " + screenDimDurationLocked + " ms");
            printWriter.println();
            printWriter.print("UID states (changing=");
            printWriter.print(this.mUidsChanging);
            printWriter.print(" changed=");
            printWriter.print(this.mUidsChanged);
            printWriter.println("):");
            for (int i = 0; i < this.mUidState.size(); i++) {
                UidState uidStateValueAt = this.mUidState.valueAt(i);
                printWriter.print("  UID ");
                UserHandle.formatUid(printWriter, this.mUidState.keyAt(i));
                printWriter.print(": ");
                if (uidStateValueAt.mActive) {
                    printWriter.print("  ACTIVE ");
                } else {
                    printWriter.print("INACTIVE ");
                }
                printWriter.print(" count=");
                printWriter.print(uidStateValueAt.mNumWakeLocks);
                printWriter.print(" state=");
                printWriter.println(uidStateValueAt.mProcState);
            }
            printWriter.println();
            printWriter.println("Looper state:");
            this.mHandler.getLooper().dump(new PrintWriterPrinter(printWriter), "  ");
            printWriter.println();
            printWriter.println("Wake Locks: size=" + this.mWakeLocks.size());
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                printWriter.println("  " + it.next());
            }
            printWriter.println();
            printWriter.println("Suspend Blockers: size=" + this.mSuspendBlockers.size());
            Iterator<SuspendBlocker> it2 = this.mSuspendBlockers.iterator();
            while (it2.hasNext()) {
                printWriter.println("  " + it2.next());
            }
            printWriter.println();
            printWriter.println("Display Power: " + this.mDisplayPowerCallbacks);
            this.mBatterySaverPolicy.dump(printWriter);
            this.mBatterySaverStateMachine.dump(printWriter);
            printWriter.println();
            int size = this.mProfilePowerState.size();
            printWriter.println("Profile power states: size=" + size);
            for (int i2 = 0; i2 < size; i2++) {
                ProfilePowerState profilePowerStateValueAt = this.mProfilePowerState.valueAt(i2);
                printWriter.print("  mUserId=");
                printWriter.print(profilePowerStateValueAt.mUserId);
                printWriter.print(" mScreenOffTimeout=");
                printWriter.print(profilePowerStateValueAt.mScreenOffTimeout);
                printWriter.print(" mWakeLockSummary=");
                printWriter.print(profilePowerStateValueAt.mWakeLockSummary);
                printWriter.print(" mLastUserActivityTime=");
                printWriter.print(profilePowerStateValueAt.mLastUserActivityTime);
                printWriter.print(" mLockingNotified=");
                printWriter.println(profilePowerStateValueAt.mLockingNotified);
            }
            wirelessChargerDetector = this.mWirelessChargerDetector;
        }
        if (wirelessChargerDetector != null) {
            wirelessChargerDetector.dump(printWriter);
        }
    }

    private void dumpProto(FileDescriptor fileDescriptor) {
        WirelessChargerDetector wirelessChargerDetector;
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        synchronized (this.mLock) {
            this.mConstants.dumpProto(protoOutputStream);
            protoOutputStream.write(1120986464258L, this.mDirty);
            protoOutputStream.write(1159641169923L, this.mWakefulness);
            protoOutputStream.write(1133871366148L, this.mWakefulnessChanging);
            protoOutputStream.write(1133871366149L, this.mIsPowered);
            protoOutputStream.write(1159641169926L, this.mPlugType);
            protoOutputStream.write(1120986464263L, this.mBatteryLevel);
            protoOutputStream.write(1120986464264L, this.mBatteryLevelWhenDreamStarted);
            protoOutputStream.write(1159641169929L, this.mDockState);
            protoOutputStream.write(1133871366154L, this.mStayOn);
            protoOutputStream.write(1133871366155L, this.mProximityPositive);
            protoOutputStream.write(1133871366156L, this.mBootCompleted);
            protoOutputStream.write(1133871366157L, this.mSystemReady);
            protoOutputStream.write(1133871366158L, this.mHalAutoSuspendModeEnabled);
            protoOutputStream.write(1133871366159L, this.mHalInteractiveModeEnabled);
            long jStart = protoOutputStream.start(1146756268048L);
            protoOutputStream.write(1133871366145L, (this.mWakeLockSummary & 1) != 0);
            protoOutputStream.write(1133871366146L, (this.mWakeLockSummary & 2) != 0);
            protoOutputStream.write(1133871366147L, (this.mWakeLockSummary & 4) != 0);
            protoOutputStream.write(1133871366148L, (this.mWakeLockSummary & 8) != 0);
            protoOutputStream.write(1133871366149L, (this.mWakeLockSummary & 16) != 0);
            protoOutputStream.write(1133871366150L, (this.mWakeLockSummary & 32) != 0);
            protoOutputStream.write(1133871366151L, (this.mWakeLockSummary & 64) != 0);
            protoOutputStream.write(1133871366152L, (this.mWakeLockSummary & 128) != 0);
            protoOutputStream.end(jStart);
            protoOutputStream.write(1112396529681L, this.mNotifyLongScheduled);
            protoOutputStream.write(1112396529682L, this.mNotifyLongDispatched);
            protoOutputStream.write(1112396529683L, this.mNotifyLongNextCheck);
            long jStart2 = protoOutputStream.start(1146756268052L);
            protoOutputStream.write(1133871366145L, (this.mUserActivitySummary & 1) != 0);
            protoOutputStream.write(1133871366146L, (this.mUserActivitySummary & 2) != 0);
            protoOutputStream.write(1133871366147L, (this.mUserActivitySummary & 4) != 0);
            protoOutputStream.end(jStart2);
            protoOutputStream.write(1133871366165L, this.mRequestWaitForNegativeProximity);
            protoOutputStream.write(1133871366166L, this.mSandmanScheduled);
            protoOutputStream.write(1133871366167L, this.mSandmanSummoned);
            protoOutputStream.write(1133871366168L, this.mBatteryLevelLow);
            protoOutputStream.write(1133871366169L, this.mLightDeviceIdleMode);
            protoOutputStream.write(1133871366170L, this.mDeviceIdleMode);
            for (int i : this.mDeviceIdleWhitelist) {
                protoOutputStream.write(2220498092059L, i);
            }
            for (int i2 : this.mDeviceIdleTempWhitelist) {
                protoOutputStream.write(2220498092060L, i2);
            }
            protoOutputStream.write(1112396529693L, this.mLastWakeTime);
            protoOutputStream.write(1112396529694L, this.mLastSleepTime);
            protoOutputStream.write(1112396529695L, this.mLastUserActivityTime);
            protoOutputStream.write(1112396529696L, this.mLastUserActivityTimeNoChangeLights);
            protoOutputStream.write(1112396529697L, this.mLastInteractivePowerHintTime);
            protoOutputStream.write(1112396529698L, this.mLastScreenBrightnessBoostTime);
            protoOutputStream.write(1133871366179L, this.mScreenBrightnessBoostInProgress);
            protoOutputStream.write(1133871366180L, this.mDisplayReady);
            protoOutputStream.write(1133871366181L, this.mHoldingWakeLockSuspendBlocker);
            protoOutputStream.write(1133871366182L, this.mHoldingDisplaySuspendBlocker);
            long jStart3 = protoOutputStream.start(1146756268071L);
            protoOutputStream.write(1133871366145L, this.mDecoupleHalAutoSuspendModeFromDisplayConfig);
            protoOutputStream.write(1133871366146L, this.mDecoupleHalInteractiveModeFromDisplayConfig);
            protoOutputStream.write(1133871366147L, this.mWakeUpWhenPluggedOrUnpluggedConfig);
            protoOutputStream.write(1133871366148L, this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig);
            protoOutputStream.write(1133871366149L, this.mTheaterModeEnabled);
            protoOutputStream.write(1133871366150L, this.mSuspendWhenScreenOffDueToProximityConfig);
            protoOutputStream.write(1133871366151L, this.mDreamsSupportedConfig);
            protoOutputStream.write(1133871366152L, this.mDreamsEnabledByDefaultConfig);
            protoOutputStream.write(1133871366153L, this.mDreamsActivatedOnSleepByDefaultConfig);
            protoOutputStream.write(1133871366154L, this.mDreamsActivatedOnDockByDefaultConfig);
            protoOutputStream.write(1133871366155L, this.mDreamsEnabledOnBatteryConfig);
            protoOutputStream.write(1172526071820L, this.mDreamsBatteryLevelMinimumWhenPoweredConfig);
            protoOutputStream.write(1172526071821L, this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig);
            protoOutputStream.write(1172526071822L, this.mDreamsBatteryLevelDrainCutoffConfig);
            protoOutputStream.write(1133871366159L, this.mDreamsEnabledSetting);
            protoOutputStream.write(1133871366160L, this.mDreamsActivateOnSleepSetting);
            protoOutputStream.write(1133871366161L, this.mDreamsActivateOnDockSetting);
            protoOutputStream.write(1133871366162L, this.mDozeAfterScreenOff);
            protoOutputStream.write(1120986464275L, this.mMinimumScreenOffTimeoutConfig);
            protoOutputStream.write(1120986464276L, this.mMaximumScreenDimDurationConfig);
            protoOutputStream.write(1108101562389L, this.mMaximumScreenDimRatioConfig);
            protoOutputStream.write(1120986464278L, this.mScreenOffTimeoutSetting);
            protoOutputStream.write(1172526071831L, this.mSleepTimeoutSetting);
            protoOutputStream.write(1120986464280L, Math.min(this.mMaximumScreenOffTimeoutFromDeviceAdmin, 2147483647L));
            protoOutputStream.write(1133871366169L, isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked());
            long jStart4 = protoOutputStream.start(1146756268058L);
            protoOutputStream.write(1133871366145L, (this.mStayOnWhilePluggedInSetting & 1) != 0);
            protoOutputStream.write(1133871366146L, (this.mStayOnWhilePluggedInSetting & 2) != 0);
            protoOutputStream.write(1133871366147L, (this.mStayOnWhilePluggedInSetting & 4) != 0);
            protoOutputStream.end(jStart4);
            protoOutputStream.write(1159641169947L, this.mScreenBrightnessModeSetting);
            protoOutputStream.write(1172526071836L, this.mScreenBrightnessOverrideFromWindowManager);
            protoOutputStream.write(1176821039133L, this.mUserActivityTimeoutOverrideFromWindowManager);
            protoOutputStream.write(1133871366174L, this.mUserInactiveOverrideFromWindowManager);
            protoOutputStream.write(1159641169951L, this.mDozeScreenStateOverrideFromDreamManager);
            protoOutputStream.write(1133871366180L, this.mDrawWakeLockOverrideFromSidekick);
            protoOutputStream.write(1108101562400L, this.mDozeScreenBrightnessOverrideFromDreamManager);
            long jStart5 = protoOutputStream.start(1146756268065L);
            protoOutputStream.write(1120986464257L, this.mScreenBrightnessSettingMinimum);
            protoOutputStream.write(1120986464258L, this.mScreenBrightnessSettingMaximum);
            protoOutputStream.write(1120986464259L, this.mScreenBrightnessSettingDefault);
            protoOutputStream.end(jStart5);
            protoOutputStream.write(1133871366178L, this.mDoubleTapWakeEnabled);
            protoOutputStream.write(1133871366179L, this.mIsVrModeEnabled);
            protoOutputStream.end(jStart3);
            long sleepTimeoutLocked = getSleepTimeoutLocked();
            long screenOffTimeoutLocked = getScreenOffTimeoutLocked(sleepTimeoutLocked);
            long screenDimDurationLocked = getScreenDimDurationLocked(screenOffTimeoutLocked);
            protoOutputStream.write(1172526071848L, sleepTimeoutLocked);
            protoOutputStream.write(1120986464297L, screenOffTimeoutLocked);
            protoOutputStream.write(1120986464298L, screenDimDurationLocked);
            protoOutputStream.write(1133871366187L, this.mUidsChanging);
            protoOutputStream.write(1133871366188L, this.mUidsChanged);
            for (int i3 = 0; i3 < this.mUidState.size(); i3++) {
                UidState uidStateValueAt = this.mUidState.valueAt(i3);
                long jStart6 = protoOutputStream.start(2246267895853L);
                int iKeyAt = this.mUidState.keyAt(i3);
                protoOutputStream.write(1120986464257L, iKeyAt);
                protoOutputStream.write(1138166333442L, UserHandle.formatUid(iKeyAt));
                protoOutputStream.write(1133871366147L, uidStateValueAt.mActive);
                protoOutputStream.write(1120986464260L, uidStateValueAt.mNumWakeLocks);
                protoOutputStream.write(1159641169925L, ActivityManager.processStateAmToProto(uidStateValueAt.mProcState));
                protoOutputStream.end(jStart6);
            }
            this.mBatterySaverStateMachine.dumpProto(protoOutputStream, 1146756268082L);
            this.mHandler.getLooper().writeToProto(protoOutputStream, 1146756268078L);
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                it.next().writeToProto(protoOutputStream, 2246267895855L);
            }
            Iterator<SuspendBlocker> it2 = this.mSuspendBlockers.iterator();
            while (it2.hasNext()) {
                it2.next().writeToProto(protoOutputStream, 2246267895856L);
            }
            wirelessChargerDetector = this.mWirelessChargerDetector;
        }
        if (wirelessChargerDetector != null) {
            wirelessChargerDetector.writeToProto(protoOutputStream, 1146756268081L);
        }
        protoOutputStream.flush();
    }

    private SuspendBlocker createSuspendBlockerLocked(String str) {
        SuspendBlockerImpl suspendBlockerImpl = new SuspendBlockerImpl(str);
        this.mSuspendBlockers.add(suspendBlockerImpl);
        return suspendBlockerImpl;
    }

    private void incrementBootCount() {
        int i;
        synchronized (this.mLock) {
            try {
                i = Settings.Global.getInt(getContext().getContentResolver(), "boot_count");
            } catch (Settings.SettingNotFoundException e) {
                i = 0;
            }
            Settings.Global.putInt(getContext().getContentResolver(), "boot_count", i + 1);
        }
    }

    private static WorkSource copyWorkSource(WorkSource workSource) {
        if (workSource != null) {
            return new WorkSource(workSource);
        }
        return null;
    }

    private final class BatteryReceiver extends BroadcastReceiver {
        private BatteryReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.handleBatteryStateChangedLocked();
            }
        }
    }

    private final class DreamReceiver extends BroadcastReceiver {
        private DreamReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.scheduleSandmanLocked();
            }
        }
    }

    private final class UserSwitchedReceiver extends BroadcastReceiver {
        private UserSwitchedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.handleSettingsChangedLocked();
            }
        }
    }

    private final class DockReceiver extends BroadcastReceiver {
        private DockReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                int intExtra = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                if (PowerManagerService.this.mDockState != intExtra) {
                    PowerManagerService.this.mDockState = intExtra;
                    PowerManagerService.access$1076(PowerManagerService.this, 1024);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.handleSettingsChangedLocked();
            }
        }
    }

    private final class PowerManagerHandler extends Handler {
        public PowerManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    PowerManagerService.this.handleUserActivityTimeout();
                    break;
                case 2:
                    PowerManagerService.this.handleSandman();
                    break;
                case 3:
                    PowerManagerService.this.handleScreenBrightnessBoostTimeout();
                    break;
                case 4:
                    PowerManagerService.this.checkForLongWakeLocks();
                    break;
            }
        }
    }

    private final class WakeLock implements IBinder.DeathRecipient {
        public long mAcquireTime;
        public boolean mDisabled;
        public int mFlags;
        public String mHistoryTag;
        public final IBinder mLock;
        public boolean mNotifiedAcquired;
        public boolean mNotifiedLong;
        public final int mOwnerPid;
        public final int mOwnerUid;
        public final String mPackageName;
        public String mTag;
        public final UidState mUidState;
        public WorkSource mWorkSource;

        public WakeLock(IBinder iBinder, int i, String str, String str2, WorkSource workSource, String str3, int i2, int i3, UidState uidState) {
            this.mLock = iBinder;
            this.mFlags = i;
            this.mTag = str;
            this.mPackageName = str2;
            this.mWorkSource = PowerManagerService.copyWorkSource(workSource);
            this.mHistoryTag = str3;
            this.mOwnerUid = i2;
            this.mOwnerPid = i3;
            this.mUidState = uidState;
        }

        @Override
        public void binderDied() {
            PowerManagerService.this.handleWakeLockDeath(this);
        }

        public boolean hasSameProperties(int i, String str, WorkSource workSource, int i2, int i3) {
            return this.mFlags == i && this.mTag.equals(str) && hasSameWorkSource(workSource) && this.mOwnerUid == i2 && this.mOwnerPid == i3;
        }

        public void updateProperties(int i, String str, String str2, WorkSource workSource, String str3, int i2, int i3) {
            if (!this.mPackageName.equals(str2)) {
                throw new IllegalStateException("Existing wake lock package name changed: " + this.mPackageName + " to " + str2);
            }
            if (this.mOwnerUid != i2) {
                throw new IllegalStateException("Existing wake lock uid changed: " + this.mOwnerUid + " to " + i2);
            }
            if (this.mOwnerPid != i3) {
                throw new IllegalStateException("Existing wake lock pid changed: " + this.mOwnerPid + " to " + i3);
            }
            this.mFlags = i;
            this.mTag = str;
            updateWorkSource(workSource);
            this.mHistoryTag = str3;
        }

        public boolean hasSameWorkSource(WorkSource workSource) {
            return Objects.equals(this.mWorkSource, workSource);
        }

        public void updateWorkSource(WorkSource workSource) {
            this.mWorkSource = PowerManagerService.copyWorkSource(workSource);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getLockLevelString());
            sb.append(" '");
            sb.append(this.mTag);
            sb.append("'");
            sb.append(getLockFlagsString());
            if (this.mDisabled) {
                sb.append(" DISABLED");
            }
            if (this.mNotifiedAcquired) {
                sb.append(" ACQ=");
                TimeUtils.formatDuration(this.mAcquireTime - SystemClock.uptimeMillis(), sb);
            }
            if (this.mNotifiedLong) {
                sb.append(" LONG");
            }
            sb.append(" (uid=");
            sb.append(this.mOwnerUid);
            if (this.mOwnerPid != 0) {
                sb.append(" pid=");
                sb.append(this.mOwnerPid);
            }
            if (this.mWorkSource != null) {
                sb.append(" ws=");
                sb.append(this.mWorkSource);
            }
            sb.append(")");
            return sb.toString();
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1159641169921L, this.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI);
            protoOutputStream.write(1138166333442L, this.mTag);
            long jStart2 = protoOutputStream.start(1146756268035L);
            protoOutputStream.write(1133871366145L, (this.mFlags & 268435456) != 0);
            protoOutputStream.write(1133871366146L, (this.mFlags & 536870912) != 0);
            protoOutputStream.end(jStart2);
            protoOutputStream.write(1133871366148L, this.mDisabled);
            if (this.mNotifiedAcquired) {
                protoOutputStream.write(1112396529669L, this.mAcquireTime);
            }
            protoOutputStream.write(1133871366150L, this.mNotifiedLong);
            protoOutputStream.write(1120986464263L, this.mOwnerUid);
            protoOutputStream.write(1120986464264L, this.mOwnerPid);
            if (this.mWorkSource != null) {
                this.mWorkSource.writeToProto(protoOutputStream, 1146756268041L);
            }
            protoOutputStream.end(jStart);
        }

        private String getLockLevelString() {
            int i = this.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
            if (i == 1) {
                return "PARTIAL_WAKE_LOCK             ";
            }
            if (i == 6) {
                return "SCREEN_DIM_WAKE_LOCK          ";
            }
            if (i == 10) {
                return "SCREEN_BRIGHT_WAKE_LOCK       ";
            }
            if (i == 26) {
                return "FULL_WAKE_LOCK                ";
            }
            if (i == 32) {
                return "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
            }
            if (i == 64) {
                return "DOZE_WAKE_LOCK                ";
            }
            if (i == 128) {
                return "DRAW_WAKE_LOCK                ";
            }
            return "???                           ";
        }

        private String getLockFlagsString() {
            String str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if ((this.mFlags & 268435456) != 0) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + " ACQUIRE_CAUSES_WAKEUP";
            }
            if ((this.mFlags & 536870912) != 0) {
                return str + " ON_AFTER_RELEASE";
            }
            return str;
        }
    }

    private final class SuspendBlockerImpl implements SuspendBlocker {
        private final String mName;
        private int mReferenceCount;
        private final String mTraceName;

        public SuspendBlockerImpl(String str) {
            this.mName = str;
            this.mTraceName = "SuspendBlocker (" + str + ")";
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mReferenceCount != 0) {
                    Slog.wtf(PowerManagerService.TAG, "Suspend blocker \"" + this.mName + "\" was finalized without being released!");
                    this.mReferenceCount = 0;
                    PowerManagerService.nativeReleaseSuspendBlocker(this.mName);
                    Trace.asyncTraceEnd(131072L, this.mTraceName, 0);
                }
            } finally {
                super.finalize();
            }
        }

        @Override
        public void acquire() {
            synchronized (this) {
                this.mReferenceCount++;
                if (this.mReferenceCount == 1) {
                    Trace.asyncTraceBegin(131072L, this.mTraceName, 0);
                    PowerManagerService.nativeAcquireSuspendBlocker(this.mName);
                }
            }
        }

        @Override
        public void release() {
            synchronized (this) {
                this.mReferenceCount--;
                if (this.mReferenceCount == 0) {
                    PowerManagerService.nativeReleaseSuspendBlocker(this.mName);
                    Trace.asyncTraceEnd(131072L, this.mTraceName, 0);
                } else if (this.mReferenceCount < 0) {
                    Slog.wtf(PowerManagerService.TAG, "Suspend blocker \"" + this.mName + "\" was released without being acquired!", new Throwable());
                    this.mReferenceCount = 0;
                }
            }
        }

        public String toString() {
            String str;
            synchronized (this) {
                str = this.mName + ": ref count=" + this.mReferenceCount;
            }
            return str;
        }

        @Override
        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            synchronized (this) {
                protoOutputStream.write(1138166333441L, this.mName);
                protoOutputStream.write(1120986464258L, this.mReferenceCount);
            }
            protoOutputStream.end(jStart);
        }
    }

    static final class UidState {
        boolean mActive;
        int mNumWakeLocks;
        int mProcState;
        final int mUid;

        UidState(int i) {
            this.mUid = i;
        }
    }

    private final class BinderService extends IPowerManager.Stub {
        private BinderService() {
        }

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
            new PowerManagerShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }

        public void acquireWakeLockWithUid(IBinder iBinder, int i, String str, String str2, int i2) {
            if (i2 < 0) {
                i2 = Binder.getCallingUid();
            }
            acquireWakeLock(iBinder, i, str, str2, new WorkSource(i2), null);
        }

        public void powerHint(int i, int i2) {
            if (PowerManagerService.this.mSystemReady) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                PowerManagerService.this.powerHintInternal(i, i2);
            }
        }

        public void acquireWakeLock(IBinder iBinder, int i, String str, String str2, WorkSource workSource, String str3) {
            if (iBinder == null) {
                throw new IllegalArgumentException("lock must not be null");
            }
            if (str2 == null) {
                throw new IllegalArgumentException("packageName must not be null");
            }
            PowerManager.validateWakeLockParameters(i, str);
            WorkSource workSource2 = null;
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
            if ((i & 64) != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            }
            if (workSource != null && !workSource.isEmpty()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
                workSource2 = workSource;
            }
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.acquireWakeLockInternal(iBinder, i, str, str2, workSource2, str3, callingUid, callingPid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void releaseWakeLock(IBinder iBinder, int i) {
            if (iBinder != null) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.releaseWakeLockInternal(iBinder, i);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("lock must not be null");
        }

        public void updateWakeLockUids(IBinder iBinder, int[] iArr) {
            WorkSource workSource;
            if (iArr != null) {
                workSource = new WorkSource();
                for (int i : iArr) {
                    workSource.add(i);
                }
            } else {
                workSource = null;
            }
            updateWakeLockWorkSource(iBinder, workSource, null);
        }

        public void updateWakeLockWorkSource(IBinder iBinder, WorkSource workSource, String str) {
            if (iBinder != null) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
                if (workSource != null && !workSource.isEmpty()) {
                    PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
                } else {
                    workSource = null;
                }
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.updateWakeLockWorkSourceInternal(iBinder, workSource, str, callingUid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("lock must not be null");
        }

        public boolean isWakeLockLevelSupported(int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isWakeLockLevelSupportedInternal(i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void userActivity(long j, int i, int i2) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") == 0 || PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.USER_ACTIVITY") == 0) {
                if (j > jUptimeMillis) {
                    throw new IllegalArgumentException("event time must not be in the future");
                }
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.userActivityInternal(j, i, i2, callingUid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            synchronized (PowerManagerService.this.mLock) {
                if (jUptimeMillis >= PowerManagerService.this.mLastWarningAboutUserActivityPermission + BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
                    PowerManagerService.this.mLastWarningAboutUserActivityPermission = jUptimeMillis;
                    Slog.w(PowerManagerService.TAG, "Ignoring call to PowerManager.userActivity() because the caller does not have DEVICE_POWER or USER_ACTIVITY permission.  Please fix your app!   pid=" + Binder.getCallingPid() + " uid=" + Binder.getCallingUid());
                }
            }
        }

        public void wakeUp(long j, String str, String str2) {
            if (j <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.wakeUpInternal(j, str, callingUid, str2, callingUid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("event time must not be in the future");
        }

        public void goToSleep(long j, int i, int i2) {
            if (j <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.goToSleepInternal(j, i, i2, callingUid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("event time must not be in the future");
        }

        public void nap(long j) {
            if (j <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.napInternal(j, callingUid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("event time must not be in the future");
        }

        public boolean isInteractive() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isInteractiveInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isPowerSaveMode() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mBatterySaverController.isEnabled();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public PowerSaveState getPowerSaveState(int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mBatterySaverPolicy.getBatterySaverPolicy(i, PowerManagerService.this.mBatterySaverController.isEnabled());
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean setPowerSaveMode(boolean z) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.setLowPowerModeInternal(z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isDeviceIdleMode() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isDeviceIdleModeInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isLightDeviceIdleMode() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isLightDeviceIdleModeInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int getLastShutdownReason() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.getLastShutdownReasonInternal(PowerManagerService.LAST_REBOOT_PROPERTY);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void reboot(boolean z, String str, boolean z2) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            if ("recovery".equals(str) || "recovery-update".equals(str)) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(1, z, str, z2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void rebootSafeMode(boolean z, boolean z2) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(2, z, "safemode", z2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void shutdown(boolean z, String str, boolean z2) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(0, z, str, z2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void crash(String str) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.crashInternal(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setStayOnSetting(int i) {
            int callingUid = Binder.getCallingUid();
            if (callingUid != 0 && !Settings.checkAndNoteWriteSettingsOperation(PowerManagerService.this.mContext, callingUid, Settings.getPackageNameForUid(PowerManagerService.this.mContext, callingUid), true)) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setStayOnSettingInternal(i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setAttentionLight(boolean z, int i) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setAttentionLightInternal(z, i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setDozeAfterScreenOff(boolean z) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setDozeAfterScreenOffInternal(z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void boostScreenBrightness(long j) {
            if (j <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int callingUid = Binder.getCallingUid();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.boostScreenBrightnessInternal(j, callingUid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("event time must not be in the future");
        }

        public boolean isScreenBrightnessBoosted() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isScreenBrightnessBoostedInternal();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(PowerManagerService.this.mContext, PowerManagerService.TAG, printWriter)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                boolean z = false;
                for (String str : strArr) {
                    if (str.equals(PriorityDump.PROTO_ARG)) {
                        z = true;
                    }
                }
                try {
                    if (z) {
                        PowerManagerService.this.dumpProto(fileDescriptor);
                    } else {
                        PowerManagerService.this.dumpInternal(printWriter);
                    }
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        }

        public void setKeepAwake(boolean z) {
            PowerManagerService.this.setKeepAwakeInternal(z);
        }

        public boolean getKeepAwake() {
            return PowerManagerService.this.getKeepAwakeInternal();
        }
    }

    @VisibleForTesting
    int getLastShutdownReasonInternal(String str) {
        String str2 = SystemProperties.get(str);
        if (str2 == null) {
            return 0;
        }
        switch (str2) {
        }
        return 0;
    }

    private final class LocalService extends PowerManagerInternal {
        private LocalService() {
        }

        public void setScreenBrightnessOverrideFromWindowManager(int i) {
            if (i < -1 || i > 255) {
                i = -1;
            }
            PowerManagerService.this.setScreenBrightnessOverrideFromWindowManagerInternal(i);
        }

        public void setDozeOverrideFromDreamManager(int i, int i2) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    break;
                default:
                    i = 0;
                    break;
            }
            if (i2 < -1 || i2 > 255) {
                i2 = -1;
            }
            PowerManagerService.this.setDozeOverrideFromDreamManagerInternal(i, i2);
        }

        public void setUserInactiveOverrideFromWindowManager() {
            PowerManagerService.this.setUserInactiveOverrideFromWindowManagerInternal();
        }

        public void setUserActivityTimeoutOverrideFromWindowManager(long j) {
            PowerManagerService.this.setUserActivityTimeoutOverrideFromWindowManagerInternal(j);
        }

        public void setDrawWakeLockOverrideFromSidekick(boolean z) {
            PowerManagerService.this.setDrawWakeLockOverrideFromSidekickInternal(z);
        }

        public void setMaximumScreenOffTimeoutFromDeviceAdmin(int i, long j) {
            PowerManagerService.this.setMaximumScreenOffTimeoutFromDeviceAdminInternal(i, j);
        }

        public PowerSaveState getLowPowerState(int i) {
            return PowerManagerService.this.mBatterySaverPolicy.getBatterySaverPolicy(i, PowerManagerService.this.mBatterySaverController.isEnabled());
        }

        public void registerLowPowerModeObserver(PowerManagerInternal.LowPowerModeListener lowPowerModeListener) {
            PowerManagerService.this.mBatterySaverController.addListener(lowPowerModeListener);
        }

        public boolean setDeviceIdleMode(boolean z) {
            return PowerManagerService.this.setDeviceIdleModeInternal(z);
        }

        public boolean setLightDeviceIdleMode(boolean z) {
            return PowerManagerService.this.setLightDeviceIdleModeInternal(z);
        }

        public void setDeviceIdleWhitelist(int[] iArr) {
            PowerManagerService.this.setDeviceIdleWhitelistInternal(iArr);
        }

        public void setDeviceIdleTempWhitelist(int[] iArr) {
            PowerManagerService.this.setDeviceIdleTempWhitelistInternal(iArr);
        }

        public void startUidChanges() {
            PowerManagerService.this.startUidChangesInternal();
        }

        public void finishUidChanges() {
            PowerManagerService.this.finishUidChangesInternal();
        }

        public void updateUidProcState(int i, int i2) {
            PowerManagerService.this.updateUidProcStateInternal(i, i2);
        }

        public void uidGone(int i) {
            PowerManagerService.this.uidGoneInternal(i);
        }

        public void uidActive(int i) {
            PowerManagerService.this.uidActiveInternal(i);
        }

        public void uidIdle(int i) {
            PowerManagerService.this.uidIdleInternal(i);
        }

        public void powerHint(int i, int i2) {
            PowerManagerService.this.powerHintInternal(i, i2);
        }
    }

    private void setKeepAwakeInternal(boolean z) {
        this.mKeepAwake = z;
        this.mDirty |= 2;
    }

    private boolean getKeepAwakeInternal() {
        return this.mKeepAwake;
    }
}
