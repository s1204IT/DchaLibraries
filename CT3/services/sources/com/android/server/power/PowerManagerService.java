package com.android.server.power;

import android.R;
import android.annotation.IntDef;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.SystemSensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.WifiDisplayStatus;
import android.net.Uri;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.view.Display;
import android.view.WindowManagerPolicy;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.server.EventLogTags;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.am.BatteryStatsService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.notification.ZenModeHelper;
import com.android.server.vr.VrManagerService;
import com.android.server.wm.WindowManagerService;
import com.mediatek.datashaping.DataShapingUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import libcore.util.Objects;

public final class PowerManagerService extends SystemService implements Watchdog.Monitor {
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_SPEW = false;
    private static final int DEFAULT_DOUBLE_TAP_TO_WAKE = 0;
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT = 15000;
    private static final int DEFAULT_SLEEP_TIMEOUT = -1;
    private static final int DIRTY_ACTUAL_DISPLAY_POWER_STATE_UPDATED = 8;
    private static final int DIRTY_BATTERY_STATE = 256;
    private static final int DIRTY_BOOT_COMPLETED = 16;
    private static final int DIRTY_BOOT_IPO = 4096;
    private static final int DIRTY_DOCK_STATE = 1024;
    private static final int DIRTY_IS_POWERED = 64;
    private static final int DIRTY_PROXIMITY_POSITIVE = 512;
    private static final int DIRTY_SCREEN_BRIGHTNESS_BOOST = 2048;
    private static final int DIRTY_SD_STATE = 8192;
    private static final int DIRTY_SETTINGS = 32;
    private static final int DIRTY_STAY_ON = 128;
    private static final int DIRTY_USER_ACTIVITY = 4;
    private static final int DIRTY_WAKEFULNESS = 2;
    private static final int DIRTY_WAKE_LOCKS = 1;
    private static final int HALT_MODE_REBOOT = 1;
    private static final int HALT_MODE_REBOOT_SAFE_MODE = 2;
    private static final int HALT_MODE_SHUTDOWN = 0;
    public static final String IPO_BOOT = "android.intent.action.ACTION_BOOT_IPO";
    public static final String IPO_PREBOOT = "android.intent.action.ACTION_PREBOOT_IPO";
    private static final float MAXIMUM_SCREEN_BUTTON_RATIO = 0.3f;
    private static final int MSG_SANDMAN = 2;
    private static final int MSG_SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 3;
    private static final int MSG_USER_ACTIVITY_TIMEOUT = 1;
    private static final int POWER_FEATURE_DOUBLE_TAP_TO_WAKE = 1;
    private static final int POWER_HINT_LOW_POWER = 5;
    private static final int POWER_HINT_VR_MODE = 7;
    private static final int SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 5000;
    private static final int SCREEN_BUTTON_LIGHT_DURATION = 8000;
    private static final String TAG = "PowerManagerService";
    private static final int USER_ACTIVITY_BUTTON_BRIGHT = 8;
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
    private IAppOpsService mAppOps;
    private Light mAttentionLight;
    private boolean mAutoLowPowerModeConfigured;
    private boolean mAutoLowPowerModeSnoozing;
    private Light mBacklight;
    private int mBatteryLevel;
    private boolean mBatteryLevelLow;
    private int mBatteryLevelWhenDreamStarted;
    private BatteryManagerInternal mBatteryManagerInternal;
    private IBatteryStats mBatteryStats;
    private boolean mBootCompleted;
    private Runnable[] mBootCompletedRunnables;
    private boolean mBrightnessUseTwilight;
    private Light mButtonLight;
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
    private boolean mDozeAfterScreenOffConfig;
    private int mDozeScreenBrightnessOverrideFromDreamManager;
    private int mDozeScreenStateOverrideFromDreamManager;
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
    private boolean mHalAutoSuspendModeEnabled;
    private boolean mHalInteractiveModeEnabled;
    private final PowerManagerHandler mHandler;
    private final ServiceThread mHandlerThread;
    private boolean mHoldingDisplaySuspendBlocker;
    private boolean mHoldingWakeLockSuspendBlocker;
    private boolean mIPOShutdown;
    private boolean mIsPowered;
    private long mLastInteractivePowerHintTime;
    private long mLastScreenBrightnessBoostTime;
    private long mLastSleepTime;
    private long mLastUserActivityButtonTime;
    private long mLastUserActivityTime;
    private long mLastUserActivityTimeNoChangeLights;
    private long mLastWakeTime;
    private long mLastWarningAboutUserActivityPermission;
    private boolean mLightDeviceIdleMode;
    private LightsManager mLightsManager;
    private final Object mLock;
    private boolean mLowPowerModeEnabled;
    private final ArrayList<PowerManagerInternal.LowPowerModeListener> mLowPowerModeListeners;
    private boolean mLowPowerModeSetting;
    private int mMaximumScreenDimDurationConfig;
    private float mMaximumScreenDimRatioConfig;
    private int mMaximumScreenOffTimeoutFromDeviceAdmin;
    private int mMinimumScreenOffTimeoutConfig;
    private Notifier mNotifier;
    private long mOverriddenTimeout;
    private int mPlugType;
    private WindowManagerPolicy mPolicy;
    private boolean mPreWakeUpWhenPluggedOrUnpluggedConfig;
    private boolean mProximityPositive;
    private boolean mRequestWaitForNegativeProximity;
    private boolean mSandmanScheduled;
    private boolean mSandmanSummoned;
    private float mScreenAutoBrightnessAdjustmentSetting;
    private boolean mScreenBrightnessBoostInProgress;
    private int mScreenBrightnessModeSetting;
    private int mScreenBrightnessOverrideFromWindowManager;
    private int mScreenBrightnessSetting;
    private int mScreenBrightnessSettingDefault;
    private int mScreenBrightnessSettingMaximum;
    private int mScreenBrightnessSettingMinimum;
    private int mScreenDimTimeoutSetting;
    private int mScreenOffTimeoutSetting;
    private SettingsObserver mSettingsObserver;
    private boolean mShutdownFlag;
    private int mSleepTimeoutSetting;
    private boolean mStayOn;
    private int mStayOnWhilePluggedInSetting;
    private boolean mStayOnWithoutDim;
    private boolean mSupportsDoubleTapWakeConfig;
    private final ArrayList<SuspendBlocker> mSuspendBlockers;
    private boolean mSuspendWhenScreenOffDueToProximityConfig;
    private boolean mSystemReady;
    private float mTemporaryScreenAutoBrightnessAdjustmentSettingOverride;
    private int mTemporaryScreenBrightnessSettingOverride;
    private boolean mTheaterModeEnabled;
    private final SparseIntArray mUidState;
    private int mUserActivitySummary;
    private boolean mUserActivityTimeoutMin;
    private int mUserActivityTimeoutOverrideFromCMD;
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
    private boolean mWfdEnabled;
    private boolean mWfdShouldBypass;
    private WirelessChargerDetector mWirelessChargerDetector;

    @IntDef({0, ZenModeHelper.SUPPRESSED_EFFECT_NOTIFICATIONS, ZenModeHelper.SUPPRESSED_EFFECT_CALLS})
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

    public PowerManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mSuspendBlockers = new ArrayList<>();
        this.mWakeLocks = new ArrayList<>();
        this.mDisplayPowerRequest = new DisplayManagerInternal.DisplayPowerRequest();
        this.mDockState = 0;
        this.mWfdShouldBypass = false;
        this.mWfdEnabled = false;
        this.mMaximumScreenOffTimeoutFromDeviceAdmin = Integer.MAX_VALUE;
        this.mStayOnWithoutDim = false;
        this.mScreenBrightnessOverrideFromWindowManager = -1;
        this.mOverriddenTimeout = -1L;
        this.mUserActivityTimeoutOverrideFromWindowManager = -1L;
        this.mTemporaryScreenBrightnessSettingOverride = -1;
        this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride = Float.NaN;
        this.mDozeScreenStateOverrideFromDreamManager = 0;
        this.mDozeScreenBrightnessOverrideFromDreamManager = -1;
        this.mLastWarningAboutUserActivityPermission = Long.MIN_VALUE;
        this.mIPOShutdown = false;
        this.mShutdownFlag = false;
        this.mUserActivityTimeoutMin = false;
        this.mUserActivityTimeoutOverrideFromCMD = 10;
        this.mDeviceIdleWhitelist = new int[0];
        this.mDeviceIdleTempWhitelist = new int[0];
        this.mUidState = new SparseIntArray();
        this.mLowPowerModeListeners = new ArrayList<>();
        this.mDisplayPowerCallbacks = new DisplayManagerInternal.DisplayPowerCallbacks() {
            private int mDisplayState = 0;

            public void onStateChanged() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mDirty |= 8;
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onProximityPositive() {
                synchronized (PowerManagerService.this.mLock) {
                    Slog.i(PowerManagerService.TAG, "onProximityPositive");
                    PowerManagerService.this.mProximityPositive = true;
                    PowerManagerService.this.mDirty |= 512;
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onProximityNegative() {
                synchronized (PowerManagerService.this.mLock) {
                    Slog.i(PowerManagerService.TAG, "onProximityNegative");
                    PowerManagerService.this.mProximityPositive = false;
                    PowerManagerService.this.mDirty |= 512;
                    PowerManagerService.this.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
                    PowerManagerService.this.wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), "android.server.power:POWER", 1000, PowerManagerService.this.mContext.getOpPackageName(), 1000);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onDisplayStateChange(int state) {
                synchronized (PowerManagerService.this.mLock) {
                    if (this.mDisplayState != state) {
                        this.mDisplayState = state;
                        if (state == 1) {
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
            public void onVrStateChanged(boolean enabled) {
                PowerManagerService.this.powerHintInternal(7, enabled ? 1 : 0);
            }
        };
        this.mContext = context;
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new PowerManagerHandler(this.mHandlerThread.getLooper());
        synchronized (this.mLock) {
            this.mWakeLockSuspendBlocker = createSuspendBlockerLocked("PowerManagerService.WakeLocks");
            this.mDisplaySuspendBlocker = createSuspendBlockerLocked("PowerManagerService.Display");
            this.mDisplaySuspendBlocker.acquire();
            this.mHoldingDisplaySuspendBlocker = true;
            this.mHalAutoSuspendModeEnabled = false;
            this.mHalInteractiveModeEnabled = true;
            this.mWakefulness = 1;
            nativeInit();
            nativeSetAutoSuspend(false);
            nativeSetInteractive(true);
            nativeSetFeature(1, 0);
        }
    }

    @Override
    public void onStart() {
        publishBinderService("power", new BinderService(this, null));
        publishLocalService(PowerManagerInternal.class, new LocalService(this, 0 == true ? 1 : 0));
        Watchdog.getInstance().addMonitor(this);
        Watchdog.getInstance().addThread(this.mHandler);
    }

    @Override
    public void onBootPhase(int phase) {
        synchronized (this.mLock) {
            if (phase == 600) {
                incrementBootCount();
            } else if (phase == 1000) {
                long now = SystemClock.uptimeMillis();
                this.mBootCompleted = true;
                this.mDirty |= 16;
                userActivityNoUpdateLocked(now, 0, 0, 1000);
                updatePowerStateLocked();
                if (!ArrayUtils.isEmpty(this.mBootCompletedRunnables)) {
                    Slog.d(TAG, "Posting " + this.mBootCompletedRunnables.length + " delayed runnables");
                    for (Runnable r : this.mBootCompletedRunnables) {
                        BackgroundThread.getHandler().post(r);
                    }
                }
                this.mBootCompletedRunnables = null;
            }
        }
    }

    public void systemReady(IAppOpsService appOps) {
        synchronized (this.mLock) {
            this.mSystemReady = true;
            this.mAppOps = appOps;
            this.mDreamManager = (DreamManagerInternal) getLocalService(DreamManagerInternal.class);
            this.mDisplayManagerInternal = (DisplayManagerInternal) getLocalService(DisplayManagerInternal.class);
            this.mPolicy = (WindowManagerPolicy) getLocalService(WindowManagerPolicy.class);
            this.mBatteryManagerInternal = (BatteryManagerInternal) getLocalService(BatteryManagerInternal.class);
            PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
            this.mScreenBrightnessSettingMinimum = pm.getMinimumScreenBrightnessSetting();
            this.mScreenBrightnessSettingMaximum = pm.getMaximumScreenBrightnessSetting();
            this.mScreenBrightnessSettingDefault = pm.getDefaultScreenBrightnessSetting();
            SystemSensorManager systemSensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
            this.mBatteryStats = BatteryStatsService.getService();
            this.mNotifier = new Notifier(Looper.getMainLooper(), this.mContext, this.mBatteryStats, this.mAppOps, createSuspendBlockerLocked("PowerManagerService.Broadcasts"), this.mPolicy);
            this.mWirelessChargerDetector = new WirelessChargerDetector(systemSensorManager, createSuspendBlockerLocked("PowerManagerService.WirelessChargerDetector"), this.mHandler);
            this.mSettingsObserver = new SettingsObserver(this.mHandler);
            this.mLightsManager = (LightsManager) getLocalService(LightsManager.class);
            this.mAttentionLight = this.mLightsManager.getLight(5);
            this.mButtonLight = this.mLightsManager.getLight(2);
            this.mBacklight = this.mLightsManager.getLight(0);
            this.mDisplayManagerInternal.initPowerManagement(this.mDisplayPowerCallbacks, this.mHandler, systemSensorManager);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            filter.setPriority(1000);
            this.mContext.registerReceiver(new BatteryReceiver(this, null), filter, null, this.mHandler);
            IntentFilter filter2 = new IntentFilter();
            filter2.addAction("android.intent.action.DREAMING_STARTED");
            filter2.addAction("android.intent.action.DREAMING_STOPPED");
            this.mContext.registerReceiver(new DreamReceiver(this, null), filter2, null, this.mHandler);
            IntentFilter filter3 = new IntentFilter();
            filter3.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiver(new UserSwitchedReceiver(this, null), filter3, null, this.mHandler);
            IntentFilter filter4 = new IntentFilter();
            filter4.addAction("android.intent.action.DOCK_EVENT");
            this.mContext.registerReceiver(new DockReceiver(this, null), filter4, null, this.mHandler);
            if (SystemProperties.get("ro.mtk_ipo_support").equals("1")) {
                IntentFilter filter5 = new IntentFilter();
                filter5.addAction("android.intent.action.ACTION_BOOT_IPO");
                filter5.addAction("android.intent.action.ACTION_PREBOOT_IPO");
                this.mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        synchronized (PowerManagerService.this.mLock) {
                            if ("android.intent.action.ACTION_PREBOOT_IPO".equals(intent.getAction())) {
                                Slog.d(PowerManagerService.TAG, "PREBOOT_IPO");
                                PowerManagerService.this.mStayOnWithoutDim = true;
                                PowerManagerService.this.mIPOShutdown = false;
                                PowerManagerService.this.mDirty |= 4096;
                            } else if ("android.intent.action.ACTION_BOOT_IPO".equals(intent.getAction())) {
                                Slog.d(PowerManagerService.TAG, "IPO_BOOT");
                                PowerManagerService.this.mStayOnWithoutDim = false;
                                PowerManagerService.this.mIPOShutdown = false;
                                PowerManagerService.this.mDirty |= 4096;
                                PowerManagerService.this.mDisplayManagerInternal.setIPOScreenOnDelay(0);
                                if (PowerManagerService.this.mWakefulness != 1) {
                                    PowerManagerService.this.wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), "android.server.power:POWER", 1000, PowerManagerService.this.mContext.getOpPackageName(), 1000);
                                } else {
                                    PowerManagerService.this.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
                                }
                            }
                            PowerManagerService.this.updatePowerStateLocked();
                        }
                    }
                }, filter5, null, this.mHandler);
                IntentFilter filter6 = new IntentFilter();
                filter6.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
                this.mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Slog.d(PowerManagerService.TAG, "ACTION_SHUTDOWN_IPO.");
                        PowerManagerService.this.mIPOShutdown = true;
                    }
                }, filter6, null, this.mHandler);
                IntentFilter filter7 = new IntentFilter();
                filter7.addAction("android.intent.action.normal.shutdown");
                this.mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (PowerManagerService.this.mBootCompleted) {
                            return;
                        }
                        Slog.d(PowerManagerService.TAG, "set mBootCompleted as true");
                        PowerManagerService.this.mBootCompleted = true;
                    }
                }, filter7, null, this.mHandler);
            }
            this.mContext.registerReceiver(new WifiDisplayStatusChangedReceiver(this, null), new IntentFilter("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED"), null, this.mHandler);
            IntentFilter filter8 = new IntentFilter();
            filter8.addAction("com.mediatek.SCREEN_TIMEOUT_MINIMUM");
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Slog.d(PowerManagerService.TAG, "SCREEN_TIMEOUT_MINIMUM.");
                    PowerManagerService.this.mPreWakeUpWhenPluggedOrUnpluggedConfig = PowerManagerService.this.mWakeUpWhenPluggedOrUnpluggedConfig;
                    PowerManagerService.this.mWakeUpWhenPluggedOrUnpluggedConfig = false;
                    PowerManagerService.this.mUserActivityTimeoutMin = true;
                }
            }, filter8, null, this.mHandler);
            IntentFilter filter9 = new IntentFilter();
            filter9.addAction("com.mediatek.SCREEN_TIMEOUT_NORMAL");
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Slog.d(PowerManagerService.TAG, "SCREEN_TIMEOUT_NORMAL.");
                    PowerManagerService.this.mWakeUpWhenPluggedOrUnpluggedConfig = PowerManagerService.this.mPreWakeUpWhenPluggedOrUnpluggedConfig;
                    PowerManagerService.this.mUserActivityTimeoutMin = false;
                }
            }, filter9, null, this.mHandler);
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_enabled"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_activate_on_sleep"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_activate_on_dock"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("screen_off_timeout"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("screen_dim_timeout"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("sleep_timeout"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("stay_on_while_plugged_in"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("screen_brightness"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("screen_auto_brightness_adj"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("low_power"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("low_power_trigger_level"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("theater_mode_on"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("double_tap_to_wake"), false, this.mSettingsObserver, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("brightness_use_twilight"), false, this.mSettingsObserver, -1);
            IVrManager vrManager = getBinderService(VrManagerService.VR_MANAGER_BINDER_SERVICE);
            try {
                vrManager.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to register VR mode state listener: " + e);
            }
            Slog.d(TAG, "system ready!");
            readConfigurationLocked();
            updateSettingsLocked();
            this.mDirty |= 256;
            updatePowerStateLocked();
        }
    }

    private void readConfigurationLocked() {
        Resources resources = this.mContext.getResources();
        this.mDecoupleHalAutoSuspendModeFromDisplayConfig = resources.getBoolean(R.^attr-private.internalMinHeight);
        this.mDecoupleHalInteractiveModeFromDisplayConfig = resources.getBoolean(R.^attr-private.internalMinWidth);
        this.mWakeUpWhenPluggedOrUnpluggedConfig = resources.getBoolean(R.^attr-private.clickColor);
        this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig = resources.getBoolean(R.^attr-private.colorAccentSecondaryVariant);
        this.mSuspendWhenScreenOffDueToProximityConfig = resources.getBoolean(R.^attr-private.dotColor);
        this.mDreamsSupportedConfig = resources.getBoolean(R.^attr-private.iconfactoryIconSize);
        this.mDreamsEnabledByDefaultConfig = resources.getBoolean(R.^attr-private.ignoreOffsetTopLimit);
        this.mDreamsActivatedOnSleepByDefaultConfig = resources.getBoolean(R.^attr-private.internalLayout);
        this.mDreamsActivatedOnDockByDefaultConfig = resources.getBoolean(R.^attr-private.initialActivityCount);
        this.mDreamsEnabledOnBatteryConfig = resources.getBoolean(R.^attr-private.internalMaxHeight);
        this.mDreamsBatteryLevelMinimumWhenPoweredConfig = resources.getInteger(R.integer.config_displayWhiteBalanceTransitionTime);
        this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig = resources.getInteger(R.integer.config_displayWhiteBalanceTransitionTimeDecrease);
        this.mDreamsBatteryLevelDrainCutoffConfig = resources.getInteger(R.integer.config_displayWhiteBalanceTransitionTimeIncrease);
        this.mDozeAfterScreenOffConfig = resources.getBoolean(R.^attr-private.internalMaxWidth);
        this.mMinimumScreenOffTimeoutConfig = resources.getInteger(R.integer.config_dockedStackDividerSnapMode);
        this.mMaximumScreenDimDurationConfig = resources.getInteger(R.integer.config_doublePressOnPowerBehavior);
        this.mMaximumScreenDimRatioConfig = resources.getFraction(R.fraction.config_maximumScreenDimRatio, 1, 1);
        this.mSupportsDoubleTapWakeConfig = resources.getBoolean(R.^attr-private.notificationHeaderIconSize);
    }

    private void updateSettingsLocked() {
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mDreamsEnabledSetting = Settings.Secure.getIntForUser(resolver, "screensaver_enabled", this.mDreamsEnabledByDefaultConfig ? 1 : 0, -2) != 0;
        this.mDreamsActivateOnSleepSetting = Settings.Secure.getIntForUser(resolver, "screensaver_activate_on_sleep", this.mDreamsActivatedOnSleepByDefaultConfig ? 1 : 0, -2) != 0;
        this.mDreamsActivateOnDockSetting = Settings.Secure.getIntForUser(resolver, "screensaver_activate_on_dock", this.mDreamsActivatedOnDockByDefaultConfig ? 1 : 0, -2) != 0;
        this.mScreenOffTimeoutSetting = Settings.System.getIntForUser(resolver, "screen_off_timeout", 15000, -2);
        this.mScreenDimTimeoutSetting = Settings.System.getIntForUser(resolver, "screen_dim_timeout", -1, -2);
        this.mSleepTimeoutSetting = Settings.Secure.getIntForUser(resolver, "sleep_timeout", -1, -2);
        this.mStayOnWhilePluggedInSetting = Settings.Global.getInt(resolver, "stay_on_while_plugged_in", 1);
        this.mTheaterModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
        if (this.mSupportsDoubleTapWakeConfig) {
            boolean doubleTapWakeEnabled = Settings.Secure.getIntForUser(resolver, "double_tap_to_wake", 0, -2) != 0;
            if (doubleTapWakeEnabled != this.mDoubleTapWakeEnabled) {
                this.mDoubleTapWakeEnabled = doubleTapWakeEnabled;
                nativeSetFeature(1, this.mDoubleTapWakeEnabled ? 1 : 0);
            }
        }
        int oldScreenBrightnessSetting = this.mScreenBrightnessSetting;
        this.mScreenBrightnessSetting = Settings.System.getIntForUser(resolver, "screen_brightness", this.mScreenBrightnessSettingDefault, -2);
        if (oldScreenBrightnessSetting != this.mScreenBrightnessSetting) {
            this.mTemporaryScreenBrightnessSettingOverride = -1;
        }
        float oldScreenAutoBrightnessAdjustmentSetting = this.mScreenAutoBrightnessAdjustmentSetting;
        this.mScreenAutoBrightnessAdjustmentSetting = Settings.System.getFloatForUser(resolver, "screen_auto_brightness_adj", 0.0f, -2);
        if (oldScreenAutoBrightnessAdjustmentSetting != this.mScreenAutoBrightnessAdjustmentSetting) {
            this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride = Float.NaN;
        }
        this.mScreenBrightnessModeSetting = Settings.System.getIntForUser(resolver, "screen_brightness_mode", 0, -2);
        this.mBrightnessUseTwilight = Settings.Secure.getIntForUser(resolver, "brightness_use_twilight", 0, -2) != 0;
        boolean lowPowerModeEnabled = Settings.Global.getInt(resolver, "low_power", 0) != 0;
        boolean autoLowPowerModeConfigured = Settings.Global.getInt(resolver, "low_power_trigger_level", 0) != 0;
        if (lowPowerModeEnabled != this.mLowPowerModeSetting || autoLowPowerModeConfigured != this.mAutoLowPowerModeConfigured) {
            this.mLowPowerModeSetting = lowPowerModeEnabled;
            this.mAutoLowPowerModeConfigured = autoLowPowerModeConfigured;
            updateLowPowerModeLocked();
        }
        this.mDirty |= 32;
    }

    private void postAfterBootCompleted(Runnable r) {
        if (this.mBootCompleted) {
            BackgroundThread.getHandler().post(r);
        } else {
            Slog.d(TAG, "Delaying runnable until system is booted");
            this.mBootCompletedRunnables = (Runnable[]) ArrayUtils.appendElement(Runnable.class, this.mBootCompletedRunnables, r);
        }
    }

    private void updateLowPowerModeLocked() {
        boolean z;
        if (this.mIsPowered && this.mLowPowerModeSetting) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "low_power", 0);
            this.mLowPowerModeSetting = false;
        }
        if (this.mIsPowered || !this.mAutoLowPowerModeConfigured || this.mAutoLowPowerModeSnoozing) {
            z = false;
        } else {
            z = this.mBatteryLevelLow;
        }
        final boolean z2 = !this.mLowPowerModeSetting ? z : true;
        if (this.mLowPowerModeEnabled == z2) {
            return;
        }
        this.mLowPowerModeEnabled = z2;
        powerHintInternal(5, z2 ? 1 : 0);
        postAfterBootCompleted(new Runnable() {
            @Override
            public void run() {
                ArrayList<PowerManagerInternal.LowPowerModeListener> listeners;
                PowerManagerService.this.mContext.sendBroadcast(new Intent("android.os.action.POWER_SAVE_MODE_CHANGING").putExtra("mode", PowerManagerService.this.mLowPowerModeEnabled).addFlags(1073741824));
                synchronized (PowerManagerService.this.mLock) {
                    listeners = new ArrayList<>(PowerManagerService.this.mLowPowerModeListeners);
                }
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onLowPowerModeChanged(z2);
                }
                Intent intent = new Intent("android.os.action.POWER_SAVE_MODE_CHANGED");
                intent.addFlags(1073741824);
                PowerManagerService.this.mContext.sendBroadcast(intent);
                PowerManagerService.this.mContext.sendBroadcastAsUser(new Intent("android.os.action.POWER_SAVE_MODE_CHANGED_INTERNAL"), UserHandle.ALL, "android.permission.DEVICE_POWER");
            }
        });
    }

    private void handleSettingsChangedLocked() {
        updateSettingsLocked();
        updatePowerStateLocked();
    }

    private void acquireWakeLockInternal(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag, int uid, int pid) {
        WakeLock wakeLock;
        boolean notifyAcquire;
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index >= 0) {
                wakeLock = this.mWakeLocks.get(index);
                if (!wakeLock.hasSameProperties(flags, tag, ws, uid, pid)) {
                    notifyWakeLockChangingLocked(wakeLock, flags, tag, packageName, uid, pid, ws, historyTag);
                    wakeLock.updateProperties(flags, tag, packageName, ws, historyTag, uid, pid);
                }
                notifyAcquire = false;
            } else {
                wakeLock = new WakeLock(lock, flags, tag, packageName, ws, historyTag, uid, pid);
                try {
                    lock.linkToDeath(wakeLock, 0);
                    this.mWakeLocks.add(wakeLock);
                    setWakeLockDisabledStateLocked(wakeLock);
                    notifyAcquire = true;
                    wakeLock.mActiveSince = SystemClock.uptimeMillis();
                } catch (RemoteException e) {
                    throw new IllegalArgumentException("Wake lock is already dead.");
                }
            }
            applyWakeLockFlagsOnAcquireLocked(wakeLock, uid);
            this.mDirty |= 1;
            updatePowerStateLocked();
            if (notifyAcquire) {
                notifyWakeLockAcquiredLocked(wakeLock);
            }
        }
    }

    private static boolean isScreenLock(WakeLock wakeLock) {
        switch (wakeLock.mFlags & 65535) {
            case 6:
            case 10:
            case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                return true;
            default:
                return false;
        }
    }

    private void applyWakeLockFlagsOnAcquireLocked(WakeLock wakeLock, int uid) {
        String opPackageName;
        int opUid;
        if (this.mIPOShutdown || (wakeLock.mFlags & 268435456) == 0 || !isScreenLock(wakeLock)) {
            return;
        }
        if (wakeLock.mWorkSource != null && wakeLock.mWorkSource.getName(0) != null) {
            opPackageName = wakeLock.mWorkSource.getName(0);
            opUid = wakeLock.mWorkSource.get(0);
        } else {
            opPackageName = wakeLock.mPackageName;
            opUid = wakeLock.mWorkSource != null ? wakeLock.mWorkSource.get(0) : wakeLock.mOwnerUid;
        }
        wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), wakeLock.mTag, opUid, opPackageName, opUid);
    }

    private void releaseWakeLockInternal(IBinder lock, int flags) {
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index < 0) {
                return;
            }
            WakeLock wakeLock = this.mWakeLocks.get(index);
            wakeLock.mTotalTime = SystemClock.uptimeMillis() - wakeLock.mActiveSince;
            if ((flags & 1) != 0) {
                this.mRequestWaitForNegativeProximity = true;
            }
            wakeLock.mLock.unlinkToDeath(wakeLock, 0);
            removeWakeLockLocked(wakeLock, index);
        }
    }

    private void handleWakeLockDeath(WakeLock wakeLock) {
        synchronized (this.mLock) {
            int index = this.mWakeLocks.indexOf(wakeLock);
            if (index < 0) {
                return;
            }
            removeWakeLockLocked(wakeLock, index);
        }
    }

    private void removeWakeLockLocked(WakeLock wakeLock, int index) {
        this.mWakeLocks.remove(index);
        notifyWakeLockReleasedLocked(wakeLock);
        applyWakeLockFlagsOnReleaseLocked(wakeLock);
        this.mDirty |= 1;
        updatePowerStateLocked();
    }

    private void applyWakeLockFlagsOnReleaseLocked(WakeLock wakeLock) {
        if ((wakeLock.mFlags & 536870912) == 0 || !isScreenLock(wakeLock)) {
            return;
        }
        userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 1, wakeLock.mOwnerUid);
    }

    private void updateWakeLockWorkSourceInternal(IBinder lock, WorkSource ws, String historyTag, int callingUid) {
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index < 0) {
                throw new IllegalArgumentException("Wake lock not active: " + lock + " from uid " + callingUid);
            }
            WakeLock wakeLock = this.mWakeLocks.get(index);
            if (!wakeLock.hasSameWorkSource(ws)) {
                notifyWakeLockChangingLocked(wakeLock, wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, ws, historyTag);
                wakeLock.mHistoryTag = historyTag;
                wakeLock.updateWorkSource(ws);
            }
        }
    }

    private int findWakeLockIndexLocked(IBinder lock) {
        int count = this.mWakeLocks.size();
        for (int i = 0; i < count; i++) {
            if (this.mWakeLocks.get(i).mLock == lock) {
                return i;
            }
        }
        return -1;
    }

    private void notifyWakeLockAcquiredLocked(WakeLock wakeLock) {
        if (!this.mSystemReady || wakeLock.mDisabled) {
            return;
        }
        wakeLock.mNotifiedAcquired = true;
        this.mNotifier.onWakeLockAcquired(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
    }

    private void notifyWakeLockChangingLocked(WakeLock wakeLock, int flags, String tag, String packageName, int uid, int pid, WorkSource ws, String historyTag) {
        if (!this.mSystemReady || !wakeLock.mNotifiedAcquired) {
            return;
        }
        this.mNotifier.onWakeLockChanging(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag, flags, tag, packageName, uid, pid, ws, historyTag);
    }

    private void notifyWakeLockReleasedLocked(WakeLock wakeLock) {
        if (!this.mSystemReady || !wakeLock.mNotifiedAcquired) {
            return;
        }
        wakeLock.mNotifiedAcquired = false;
        this.mNotifier.onWakeLockReleased(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
    }

    private void dumpWakeLockLocked() {
        int numWakeLocks = this.mWakeLocks.size();
        if (numWakeLocks > 0) {
            Slog.d(TAG, "wakelock list dump: mLocks.size=" + numWakeLocks + ":");
            for (int i = 0; i < numWakeLocks; i++) {
                WakeLock wakeLock = this.mWakeLocks.get(i);
                String type = "";
                switch (wakeLock.mFlags & 65535) {
                    case 1:
                        type = "PARTIAL_WAKE_LOCK";
                        break;
                    case 6:
                        type = "SCREEN_DIM_WAKE_LOCK";
                        break;
                    case 10:
                        type = "SCREEN_BRIGHT_WAKE_LOCK";
                        break;
                    case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                        type = "FULL_WAKE_LOCK";
                        break;
                    case 32:
                        type = "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
                        break;
                    case 64:
                        type = "DOZE_WAKE_LOCK";
                        break;
                }
                long total_time = SystemClock.uptimeMillis() - wakeLock.mActiveSince;
                Slog.d(TAG, "No." + i + ": " + type + " '" + wakeLock.mTag + "'activated(flags=" + wakeLock.mFlags + ", uid=" + wakeLock.mOwnerUid + ", pid=" + wakeLock.mOwnerPid + ") total=" + total_time + "ms)");
            }
        }
    }

    private boolean canBrightnessOverRange() {
        if (this.mWfdEnabled) {
            return true;
        }
        return false;
    }

    private boolean isWakeLockLevelSupportedInternal(int level) {
        boolean zIsProximitySensorAvailable = false;
        synchronized (this.mLock) {
            switch (level) {
                case 1:
                case 6:
                case 10:
                case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                case 64:
                case 128:
                    return true;
                case 32:
                    if (this.mSystemReady) {
                        zIsProximitySensorAvailable = this.mDisplayManagerInternal.isProximitySensorAvailable();
                        break;
                    }
                    return zIsProximitySensorAvailable;
                default:
                    return false;
            }
        }
    }

    private void userActivityFromNative(long eventTime, int event, int flags) {
        userActivityInternal(eventTime, event, flags, 1000);
    }

    private void userActivityInternal(long eventTime, int event, int flags, int uid) {
        synchronized (this.mLock) {
            if (userActivityNoUpdateLocked(eventTime, event, flags, uid)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean userActivityNoUpdateLocked(long eventTime, int event, int flags, int uid) {
        if (eventTime < this.mLastSleepTime || eventTime < this.mLastWakeTime || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(524288L, "userActivity");
        try {
            if (eventTime > this.mLastInteractivePowerHintTime) {
                powerHintInternal(2, 0);
                this.mLastInteractivePowerHintTime = eventTime;
            }
            this.mNotifier.onUserActivity(event, uid);
            if (this.mUserInactiveOverrideFromWindowManager) {
                this.mUserInactiveOverrideFromWindowManager = false;
                this.mOverriddenTimeout = -1L;
            }
            if (this.mWakefulness == 0 || this.mWakefulness == 3 || (flags & 2) != 0) {
                return false;
            }
            if ((flags & 1) != 0) {
                if (eventTime > this.mLastUserActivityTimeNoChangeLights && eventTime > this.mLastUserActivityTime) {
                    this.mLastUserActivityTimeNoChangeLights = eventTime;
                    this.mDirty |= 4;
                    return true;
                }
            } else if (eventTime > this.mLastUserActivityTime) {
                this.mLastUserActivityTime = eventTime;
                this.mDirty |= 4;
                if (event == 1) {
                    this.mLastUserActivityButtonTime = eventTime;
                }
                return true;
            }
            return false;
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    private void wakeUpInternal(long eventTime, String reason, int uid, String opPackageName, int opUid) {
        synchronized (this.mLock) {
            if (this.mIPOShutdown && reason != "shutdown") {
                return;
            }
            if (wakeUpNoUpdateLocked(eventTime, reason, uid, opPackageName, opUid)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean wakeUpNoUpdateLocked(long eventTime, String reason, int reasonUid, String opPackageName, int opUid) {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (StackTraceElement element : stack) {
            Slog.d(TAG, "   |----" + element.toString());
        }
        if (reason == "shutdown") {
            synchronized (this.mLock) {
                this.mShutdownFlag = false;
                Slog.d(TAG, "mShutdownFlag = " + this.mShutdownFlag);
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
            return true;
        }
        if (eventTime < this.mLastSleepTime || this.mWakefulness == 1 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(524288L, "wakeUp");
        try {
            switch (this.mWakefulness) {
                case 0:
                    Slog.i(TAG, "Waking up from sleep (uid " + reasonUid + ")...");
                    break;
                case 2:
                    Slog.i(TAG, "Waking up from dream (uid " + reasonUid + ")...");
                    break;
                case 3:
                    Slog.i(TAG, "Waking up from dozing (uid " + reasonUid + ")...");
                    break;
            }
            this.mLastWakeTime = eventTime;
            setWakefulnessLocked(1, 0);
            this.mNotifier.onWakeUp(reason, reasonUid, opPackageName, opUid);
            userActivityNoUpdateLocked(eventTime, 0, 0, reasonUid);
            Trace.traceEnd(524288L);
            return true;
        } catch (Throwable th) {
            Trace.traceEnd(524288L);
            throw th;
        }
    }

    private void goToSleepInternal(long eventTime, int reason, int flags, int uid) {
        synchronized (this.mLock) {
            if (this.mProximityPositive && reason == 4) {
                Slog.d(TAG, "Proximity positive sleep and force wakeup by power button");
                this.mDirty |= 2;
                this.mWakefulness = 0;
                updatePowerStateLocked();
                return;
            }
            if (goToSleepNoUpdateLocked(eventTime, reason, flags, uid)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean goToSleepNoUpdateLocked(long eventTime, int reason, int flags, int uid) {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (StackTraceElement element : stack) {
            Slog.d(TAG, " \t|----" + element.toString());
        }
        if (reason == 7) {
            this.mDirty |= 32;
            this.mShutdownFlag = true;
            Slog.d(TAG, "mShutdownFlag = " + this.mShutdownFlag);
            return true;
        }
        if (eventTime < this.mLastWakeTime || this.mWakefulness == 0 || this.mWakefulness == 3 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(524288L, "goToSleep");
        try {
            switch (reason) {
                case 1:
                    Slog.i(TAG, "Going to sleep due to device administration policy (uid " + uid + ")...");
                    break;
                case 2:
                    Slog.i(TAG, "Going to sleep due to screen timeout (uid " + uid + ")...");
                    break;
                case 3:
                    Slog.i(TAG, "Going to sleep due to lid switch (uid " + uid + ")...");
                    break;
                case 4:
                    Slog.i(TAG, "Going to sleep due to power button (uid " + uid + ")...");
                    break;
                case 5:
                    Slog.i(TAG, "Going to sleep due to HDMI standby (uid " + uid + ")...");
                    break;
                case 6:
                    Slog.i(TAG, "Going to sleep due to sleep button (uid " + uid + ")...");
                    break;
                case 7:
                default:
                    Slog.i(TAG, "Going to sleep by application request (uid " + uid + ")...");
                    reason = 0;
                    break;
                case 8:
                    Slog.i(TAG, "Going to sleep due to proximity (uid " + uid + ")...");
                    break;
            }
            this.mLastSleepTime = eventTime;
            this.mSandmanSummoned = true;
            setWakefulnessLocked(3, reason);
            int numWakeLocksCleared = 0;
            int numWakeLocks = this.mWakeLocks.size();
            for (int i = 0; i < numWakeLocks; i++) {
                WakeLock wakeLock = this.mWakeLocks.get(i);
                switch (wakeLock.mFlags & 65535) {
                    case 6:
                    case 10:
                    case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                        numWakeLocksCleared++;
                        break;
                }
            }
            EventLog.writeEvent(EventLogTags.POWER_SLEEP_REQUESTED, numWakeLocksCleared);
            dumpWakeLockLocked();
            if ((flags & 1) != 0) {
                reallyGoToSleepNoUpdateLocked(eventTime, uid);
            }
            Trace.traceEnd(524288L);
            return true;
        } catch (Throwable th) {
            Trace.traceEnd(524288L);
            throw th;
        }
    }

    private void napInternal(long eventTime, int uid) {
        synchronized (this.mLock) {
            if (napNoUpdateLocked(eventTime, uid)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean napNoUpdateLocked(long eventTime, int uid) {
        if (eventTime < this.mLastWakeTime || this.mWakefulness != 1 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(524288L, "nap");
        try {
            Slog.i(TAG, "Nap time (uid " + uid + ")...");
            this.mSandmanSummoned = true;
            setWakefulnessLocked(2, 0);
            return true;
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    private boolean reallyGoToSleepNoUpdateLocked(long eventTime, int uid) {
        if (eventTime < this.mLastWakeTime || this.mWakefulness == 0 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(524288L, "reallyGoToSleep");
        try {
            Slog.i(TAG, "Sleeping (uid " + uid + ")...");
            setWakefulnessLocked(0, 2);
            Trace.traceEnd(524288L);
            return true;
        } catch (Throwable th) {
            Trace.traceEnd(524288L);
            throw th;
        }
    }

    private void setWakefulnessLocked(int wakefulness, int reason) {
        if (this.mWakefulness == wakefulness) {
            return;
        }
        this.mWakefulness = wakefulness;
        this.mWakefulnessChanging = true;
        this.mDirty |= 2;
        this.mNotifier.onWakefulnessChangeStarted(wakefulness, reason);
    }

    private void logSleepTimeoutRecapturedLocked() {
        long now = SystemClock.uptimeMillis();
        long savedWakeTimeMs = this.mOverriddenTimeout - now;
        if (savedWakeTimeMs < 0) {
            return;
        }
        EventLog.writeEvent(EventLogTags.POWER_SOFT_SLEEP_REQUESTED, savedWakeTimeMs);
        this.mOverriddenTimeout = -1L;
    }

    private void finishWakefulnessChangeIfNeededLocked() {
        if (!this.mWakefulnessChanging || !this.mDisplayReady) {
            return;
        }
        if (this.mWakefulness == 3 && (this.mWakeLockSummary & 64) == 0) {
            return;
        }
        if (this.mWakefulness == 3 || this.mWakefulness == 0) {
            logSleepTimeoutRecapturedLocked();
        }
        this.mWakefulnessChanging = false;
        this.mNotifier.onWakefulnessChangeFinished();
    }

    private void updatePowerStateLocked() {
        int dirtyPhase1;
        if (!this.mSystemReady || this.mDirty == 0) {
            return;
        }
        if (!Thread.holdsLock(this.mLock)) {
            Slog.wtf(TAG, "Power manager lock was not held when calling updatePowerStateLocked");
        }
        Trace.traceBegin(524288L, "updatePowerState");
        try {
            updateIsPoweredLocked(this.mDirty);
            updateStayOnLocked(this.mDirty);
            updateScreenBrightnessBoostLocked(this.mDirty);
            long now = SystemClock.uptimeMillis();
            int dirtyPhase2 = 0;
            do {
                dirtyPhase1 = this.mDirty;
                dirtyPhase2 |= dirtyPhase1;
                this.mDirty = 0;
                updateWakeLockSummaryLocked(dirtyPhase1);
                updateUserActivitySummaryLocked(now, dirtyPhase1);
            } while (updateWakefulnessLocked(dirtyPhase1));
            boolean displayBecameReady = updateDisplayPowerStateLocked(dirtyPhase2);
            updateDreamLocked(dirtyPhase2, displayBecameReady);
            finishWakefulnessChangeIfNeededLocked();
            updateSuspendBlockerLocked();
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    private void updateIsPoweredLocked(int dirty) {
        if ((dirty & 256) == 0) {
            return;
        }
        boolean wasPowered = this.mIsPowered;
        int oldPlugType = this.mPlugType;
        boolean oldLevelLow = this.mBatteryLevelLow;
        this.mIsPowered = this.mBatteryManagerInternal.isPowered(7);
        this.mPlugType = this.mBatteryManagerInternal.getPlugType();
        this.mBatteryLevel = this.mBatteryManagerInternal.getBatteryLevel();
        this.mBatteryLevelLow = this.mBatteryManagerInternal.getBatteryLevelLow();
        if (wasPowered != this.mIsPowered || oldPlugType != this.mPlugType) {
            this.mDirty |= 64;
            boolean dockedOnWirelessCharger = this.mWirelessChargerDetector.update(this.mIsPowered, this.mPlugType, this.mBatteryLevel);
            long now = SystemClock.uptimeMillis();
            if (shouldWakeUpWhenPluggedOrUnpluggedLocked(wasPowered, oldPlugType, dockedOnWirelessCharger)) {
                wakeUpNoUpdateLocked(now, "android.server.power:POWER", 1000, this.mContext.getOpPackageName(), 1000);
            }
            userActivityNoUpdateLocked(now, 0, 0, 1000);
            if (dockedOnWirelessCharger) {
                this.mNotifier.onWirelessChargingStarted();
            }
        }
        if (wasPowered == this.mIsPowered && oldLevelLow == this.mBatteryLevelLow) {
            return;
        }
        if (oldLevelLow != this.mBatteryLevelLow && !this.mBatteryLevelLow) {
            this.mAutoLowPowerModeSnoozing = false;
        }
        updateLowPowerModeLocked();
    }

    private boolean shouldWakeUpWhenPluggedOrUnpluggedLocked(boolean wasPowered, int oldPlugType, boolean dockedOnWirelessCharger) {
        if (!this.mWakeUpWhenPluggedOrUnpluggedConfig) {
            return false;
        }
        if (wasPowered && !this.mIsPowered && oldPlugType == 4) {
            return false;
        }
        if (!wasPowered && this.mIsPowered && this.mPlugType == 4 && !dockedOnWirelessCharger) {
            return false;
        }
        if (this.mIsPowered && this.mWakefulness == 2) {
            return false;
        }
        return !this.mTheaterModeEnabled || this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig;
    }

    private void updateStayOnLocked(int dirty) {
        if ((dirty & 288) == 0) {
            return;
        }
        boolean wasStayOn = this.mStayOn;
        if (this.mStayOnWhilePluggedInSetting != 0 && !isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
            this.mStayOn = this.mBatteryManagerInternal.isPowered(this.mStayOnWhilePluggedInSetting);
        } else {
            this.mStayOn = false;
        }
        if (this.mStayOn == wasStayOn) {
            return;
        }
        this.mDirty |= 128;
    }

    private void updateWakeLockSummaryLocked(int dirty) {
        if ((dirty & 3) == 0) {
            return;
        }
        this.mWakeLockSummary = 0;
        int numWakeLocks = this.mWakeLocks.size();
        for (int i = 0; i < numWakeLocks; i++) {
            WakeLock wakeLock = this.mWakeLocks.get(i);
            switch (wakeLock.mFlags & 65535) {
                case 1:
                    if (!wakeLock.mDisabled) {
                        this.mWakeLockSummary |= 1;
                    }
                    break;
                case 6:
                    this.mWakeLockSummary |= 4;
                    break;
                case 10:
                    this.mWakeLockSummary |= 2;
                    break;
                case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                    this.mWakeLockSummary |= 10;
                    break;
                case 32:
                    this.mWakeLockSummary |= 16;
                    break;
                case 64:
                    this.mWakeLockSummary |= 64;
                    break;
                case 128:
                    this.mWakeLockSummary |= 128;
                    break;
            }
        }
        if (this.mWakefulness != 3) {
            this.mWakeLockSummary &= -193;
        }
        if (this.mWakefulness == 0 || (this.mWakeLockSummary & 64) != 0) {
            this.mWakeLockSummary &= -15;
            if (this.mWakefulness == 0) {
            }
        }
        if ((this.mWakeLockSummary & 6) != 0) {
            if (this.mWakefulness == 1) {
                this.mWakeLockSummary |= 33;
            } else if (this.mWakefulness == 2) {
                this.mWakeLockSummary |= 1;
            }
        }
        if ((this.mWakeLockSummary & 128) == 0) {
            return;
        }
        this.mWakeLockSummary |= 1;
    }

    private boolean bypassUserActivityTimeout() {
        if (this.mStayOnWithoutDim) {
            Slog.d(TAG, "bypass UserActivityTimeout because of setting");
            return true;
        }
        if (!SystemProperties.getBoolean("persist.keep.awake", false)) {
            return false;
        }
        Slog.d(TAG, "bypass UserActivityTimeout because of system property request");
        return true;
    }

    private void updateUserActivitySummaryLocked(long now, int dirty) {
        if ((dirty & 39) == 0) {
            return;
        }
        this.mHandler.removeMessages(1);
        long nextTimeout = 0;
        if (this.mWakefulness == 1 || this.mWakefulness == 2 || this.mWakefulness == 3) {
            int sleepTimeout = getSleepTimeoutLocked();
            int screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout);
            int screenDimDuration = getScreenDimDurationLocked(screenOffTimeout);
            boolean userInactiveOverride = this.mUserInactiveOverrideFromWindowManager;
            int screenButtonLightDuration = getButtonLightDurationLocked(screenOffTimeout);
            this.mUserActivitySummary = 0;
            if (this.mLastUserActivityTime >= this.mLastWakeTime) {
                if (this.mLastUserActivityButtonTime >= this.mLastWakeTime && now < this.mLastUserActivityButtonTime + ((long) screenButtonLightDuration)) {
                    this.mUserActivitySummary |= 8;
                    this.mUserActivitySummary |= 1;
                    nextTimeout = this.mLastUserActivityButtonTime + ((long) screenButtonLightDuration);
                } else if (now < (this.mLastUserActivityTime + ((long) screenOffTimeout)) - ((long) screenDimDuration)) {
                    nextTimeout = (this.mLastUserActivityTime + ((long) screenOffTimeout)) - ((long) screenDimDuration);
                    this.mUserActivitySummary |= 1;
                } else {
                    nextTimeout = this.mLastUserActivityTime + ((long) screenOffTimeout);
                    if (now < nextTimeout) {
                        this.mUserActivitySummary |= 2;
                    }
                }
            }
            if (this.mUserActivitySummary == 0 && this.mLastUserActivityTimeNoChangeLights >= this.mLastWakeTime) {
                nextTimeout = this.mLastUserActivityTimeNoChangeLights + ((long) screenOffTimeout);
                if (now < nextTimeout) {
                    if (this.mDisplayPowerRequest.policy == 3) {
                        this.mUserActivitySummary = 1;
                    } else if (this.mDisplayPowerRequest.policy == 2) {
                        this.mUserActivitySummary = 2;
                    }
                }
            }
            if (this.mUserActivitySummary == 0) {
                if (sleepTimeout >= 0) {
                    long anyUserActivity = Math.max(this.mLastUserActivityTime, this.mLastUserActivityTimeNoChangeLights);
                    if (anyUserActivity >= this.mLastWakeTime) {
                        nextTimeout = anyUserActivity + ((long) sleepTimeout);
                        if (now < nextTimeout) {
                            this.mUserActivitySummary = 4;
                        }
                    }
                } else {
                    this.mUserActivitySummary = 4;
                    nextTimeout = -1;
                }
            }
            if (this.mUserActivitySummary != 4 && userInactiveOverride) {
                if ((this.mUserActivitySummary & 3) != 0 && nextTimeout >= now && this.mOverriddenTimeout == -1) {
                    this.mOverriddenTimeout = nextTimeout;
                }
                this.mUserActivitySummary = 4;
                nextTimeout = -1;
            }
            if (this.mUserActivitySummary == 0 || nextTimeout < 0) {
                return;
            }
            Message msg = this.mHandler.obtainMessage(1);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageAtTime(msg, nextTimeout);
            return;
        }
        this.mUserActivitySummary = 0;
    }

    private void handleUserActivityTimeout() {
        synchronized (this.mLock) {
            this.mDirty |= 4;
            updatePowerStateLocked();
        }
    }

    private int getSleepTimeoutLocked() {
        int timeout = this.mSleepTimeoutSetting;
        if (timeout <= 0) {
            return -1;
        }
        return Math.max(timeout, this.mMinimumScreenOffTimeoutConfig);
    }

    private int getScreenOffTimeoutLocked(int sleepTimeout) {
        int timeout = this.mScreenOffTimeoutSetting;
        if (isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
            timeout = Math.min(timeout, this.mMaximumScreenOffTimeoutFromDeviceAdmin);
        }
        if (this.mUserActivityTimeoutOverrideFromWindowManager >= 0) {
            timeout = (int) Math.min(timeout, this.mUserActivityTimeoutOverrideFromWindowManager);
        }
        if (sleepTimeout >= 0) {
            timeout = Math.min(timeout, sleepTimeout);
        }
        if (this.mUserActivityTimeoutMin) {
            timeout = Math.min(timeout, this.mUserActivityTimeoutOverrideFromCMD);
        }
        return Math.max(timeout, this.mMinimumScreenOffTimeoutConfig);
    }

    private int getScreenDimDurationLocked(int screenOffTimeout) {
        if (this.mScreenDimTimeoutSetting > 0) {
            return Math.max(screenOffTimeout - this.mScreenDimTimeoutSetting, 0);
        }
        return Math.min(this.mMaximumScreenDimDurationConfig, (int) (screenOffTimeout * this.mMaximumScreenDimRatioConfig));
    }

    private int getButtonLightDurationLocked(int screenOffTimeout) {
        return Math.min(SCREEN_BUTTON_LIGHT_DURATION, (int) (screenOffTimeout * MAXIMUM_SCREEN_BUTTON_RATIO));
    }

    private boolean updateWakefulnessLocked(int dirty) {
        if ((dirty & 1687) == 0 || this.mWakefulness != 1 || !isItBedTimeYetLocked()) {
            return false;
        }
        long time = SystemClock.uptimeMillis();
        if (shouldNapAtBedTimeLocked()) {
            boolean changed = napNoUpdateLocked(time, 1000);
            return changed;
        }
        boolean changed2 = goToSleepNoUpdateLocked(time, 2, 0, 1000);
        return changed2;
    }

    private boolean shouldNapAtBedTimeLocked() {
        if (this.mDreamsActivateOnSleepSetting) {
            return true;
        }
        return this.mDreamsActivateOnDockSetting && this.mDockState != 0;
    }

    private boolean isItBedTimeYetLocked() {
        return (!this.mBootCompleted || isBeingKeptAwakeLocked() || this.mIPOShutdown) ? false : true;
    }

    private boolean isBeingKeptAwakeLocked() {
        if (this.mStayOn || this.mProximityPositive || (this.mWakeLockSummary & 32) != 0 || (this.mUserActivitySummary & 3) != 0 || this.mScreenBrightnessBoostInProgress) {
            return true;
        }
        return bypassUserActivityTimeout();
    }

    private void updateDreamLocked(int dirty, boolean displayBecameReady) {
        if (((dirty & 13303) == 0 && !displayBecameReady) || !this.mDisplayReady) {
            return;
        }
        scheduleSandmanLocked();
    }

    private void scheduleSandmanLocked() {
        if (this.mSandmanScheduled) {
            return;
        }
        this.mSandmanScheduled = true;
        Message msg = this.mHandler.obtainMessage(2);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    private void handleSandman() {
        int wakefulness;
        boolean startDreaming;
        boolean zIsDreaming;
        synchronized (this.mLock) {
            this.mSandmanScheduled = false;
            wakefulness = this.mWakefulness;
            if (this.mSandmanSummoned && this.mDisplayReady) {
                startDreaming = !canDreamLocked() ? canDozeLocked() : true;
                this.mSandmanSummoned = false;
            } else {
                startDreaming = false;
            }
        }
        if (this.mDreamManager != null) {
            if (startDreaming) {
                this.mDreamManager.stopDream(false);
                this.mDreamManager.startDream(wakefulness == 3);
            }
            zIsDreaming = this.mDreamManager.isDreaming();
        } else {
            zIsDreaming = false;
        }
        synchronized (this.mLock) {
            if (startDreaming && zIsDreaming) {
                this.mBatteryLevelWhenDreamStarted = this.mBatteryLevel;
                if (wakefulness == 3) {
                    Slog.i(TAG, "Dozing...");
                } else {
                    Slog.i(TAG, "Dreaming...");
                }
            }
            if (this.mSandmanSummoned || this.mWakefulness != wakefulness) {
                return;
            }
            if (wakefulness == 2) {
                if (zIsDreaming && canDreamLocked()) {
                    if (this.mDreamsBatteryLevelDrainCutoffConfig < 0 || this.mBatteryLevel >= this.mBatteryLevelWhenDreamStarted - this.mDreamsBatteryLevelDrainCutoffConfig || isBeingKeptAwakeLocked()) {
                        return;
                    } else {
                        Slog.i(TAG, "Stopping dream because the battery appears to be draining faster than it is charging.  Battery level when dream started: " + this.mBatteryLevelWhenDreamStarted + "%.  Battery level now: " + this.mBatteryLevel + "%.");
                    }
                }
                if (isItBedTimeYetLocked()) {
                    Slog.i(TAG, "handleSandman: Bed time and goToSleepNoUpdateLocked");
                    goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 2, 0, 1000);
                    updatePowerStateLocked();
                } else {
                    Slog.i(TAG, "handleSandman: time to wakeUpNoUpdateLocked");
                    wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), "android.server.power:DREAM", 1000, this.mContext.getOpPackageName(), 1000);
                    updatePowerStateLocked();
                }
            } else if (wakefulness == 3) {
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

    private boolean canDreamLocked() {
        if (this.mWakefulness != 2 || !this.mDreamsSupportedConfig || !this.mDreamsEnabledSetting || !this.mDisplayPowerRequest.isBrightOrDim() || (this.mUserActivitySummary & 7) == 0 || !this.mBootCompleted) {
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

    private boolean updateDisplayPowerStateLocked(int dirty) {
        boolean oldDisplayReady = this.mDisplayReady;
        if ((dirty & 6207) != 0) {
            this.mDisplayPowerRequest.policy = getDesiredScreenPolicyLocked();
            boolean brightnessSetByUser = true;
            int screenBrightness = this.mScreenBrightnessSettingDefault;
            float screenAutoBrightnessAdjustment = 0.0f;
            boolean autoBrightness = (this.mScreenBrightnessModeSetting & 1) > 0;
            if (isValidBrightness(this.mScreenBrightnessOverrideFromWindowManager)) {
                screenBrightness = this.mScreenBrightnessOverrideFromWindowManager;
                autoBrightness = false;
                brightnessSetByUser = false;
            } else if (isValidBrightness(this.mTemporaryScreenBrightnessSettingOverride)) {
                screenBrightness = this.mTemporaryScreenBrightnessSettingOverride;
            } else if (isValidBrightness(this.mScreenBrightnessSetting)) {
                screenBrightness = this.mScreenBrightnessSetting;
            }
            if (autoBrightness) {
                screenBrightness = this.mScreenBrightnessSettingDefault;
                if (isValidAutoBrightnessAdjustment(this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride)) {
                    screenAutoBrightnessAdjustment = this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride;
                } else if (isValidAutoBrightnessAdjustment(this.mScreenAutoBrightnessAdjustmentSetting)) {
                    screenAutoBrightnessAdjustment = this.mScreenAutoBrightnessAdjustmentSetting;
                }
            }
            if (!canBrightnessOverRange()) {
                screenBrightness = Math.max(Math.min(screenBrightness, this.mScreenBrightnessSettingMaximum), this.mScreenBrightnessSettingMinimum);
            }
            float screenAutoBrightnessAdjustment2 = Math.max(Math.min(screenAutoBrightnessAdjustment, 1.0f), -1.0f);
            this.mDisplayPowerRequest.screenBrightness = screenBrightness;
            this.mDisplayPowerRequest.screenAutoBrightnessAdjustment = screenAutoBrightnessAdjustment2;
            this.mDisplayPowerRequest.brightnessSetByUser = brightnessSetByUser;
            this.mDisplayPowerRequest.useAutoBrightness = autoBrightness;
            this.mDisplayPowerRequest.useProximitySensor = shouldUseProximitySensorLocked();
            this.mDisplayPowerRequest.lowPowerMode = this.mLowPowerModeEnabled;
            this.mDisplayPowerRequest.boostScreenBrightness = this.mScreenBrightnessBoostInProgress;
            this.mDisplayPowerRequest.useTwilight = this.mBrightnessUseTwilight;
            if (this.mDisplayPowerRequest.policy == 1) {
                this.mDisplayPowerRequest.dozeScreenState = this.mDozeScreenStateOverrideFromDreamManager;
                if (this.mDisplayPowerRequest.dozeScreenState == 4 && (this.mWakeLockSummary & 128) != 0) {
                    this.mDisplayPowerRequest.dozeScreenState = 3;
                }
                this.mDisplayPowerRequest.dozeScreenBrightness = this.mDozeScreenBrightnessOverrideFromDreamManager;
            } else {
                this.mDisplayPowerRequest.dozeScreenState = 0;
                this.mDisplayPowerRequest.dozeScreenBrightness = -1;
            }
            this.mDisplayReady = this.mDisplayManagerInternal.requestPowerState(this.mDisplayPowerRequest, this.mRequestWaitForNegativeProximity);
            this.mRequestWaitForNegativeProximity = false;
            if (this.mDisplayPowerRequest.policy == 3 && this.mWakefulness == 1 && !this.mIPOShutdown && !this.mShutdownFlag) {
                if ((this.mWakeLockSummary & 8) != 0 || (this.mUserActivitySummary & 8) != 0) {
                    this.mButtonLight.setBrightness(screenBrightness);
                } else {
                    this.mButtonLight.turnOff();
                }
            } else {
                this.mButtonLight.turnOff();
            }
        }
        return this.mDisplayReady && !oldDisplayReady;
    }

    private void updateScreenBrightnessBoostLocked(int dirty) {
        if ((dirty & 2048) == 0 || !this.mScreenBrightnessBoostInProgress) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        this.mHandler.removeMessages(3);
        if (this.mLastScreenBrightnessBoostTime > this.mLastSleepTime) {
            long boostTimeout = this.mLastScreenBrightnessBoostTime + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
            if (boostTimeout > now) {
                Message msg = this.mHandler.obtainMessage(3);
                msg.setAsynchronous(true);
                this.mHandler.sendMessageAtTime(msg, boostTimeout);
                return;
            }
        }
        this.mScreenBrightnessBoostInProgress = false;
        this.mNotifier.onScreenBrightnessBoostChanged();
        userActivityNoUpdateLocked(now, 0, 0, 1000);
    }

    private static boolean isValidBrightness(int value) {
        return value >= 0 && value <= 255;
    }

    private static boolean isValidAutoBrightnessAdjustment(float value) {
        return value >= -1.0f && value <= 1.0f;
    }

    private int getDesiredScreenPolicyLocked() {
        if (this.mWakefulness == 0 || this.mShutdownFlag) {
            return 0;
        }
        if (this.mWakefulness == 3) {
            if ((this.mWakeLockSummary & 64) != 0) {
                return 1;
            }
            if (this.mDozeAfterScreenOffConfig) {
                return 0;
            }
        }
        return ((this.mWakeLockSummary & 2) == 0 && (this.mUserActivitySummary & 1) == 0 && this.mBootCompleted && !this.mScreenBrightnessBoostInProgress && !bypassUserActivityTimeout()) ? 2 : 3;
    }

    private boolean shouldUseProximitySensorLocked() {
        return (this.mWakeLockSummary & 16) != 0;
    }

    private void updateSuspendBlockerLocked() {
        boolean needWakeLockSuspendBlocker = (this.mWakeLockSummary & 1) != 0;
        boolean needDisplaySuspendBlocker = needDisplaySuspendBlockerLocked();
        boolean autoSuspend = !needDisplaySuspendBlocker;
        boolean interactive = this.mDisplayPowerRequest.isBrightOrDim();
        if (!autoSuspend && this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            setHalAutoSuspendModeLocked(false);
        }
        if (needWakeLockSuspendBlocker && !this.mHoldingWakeLockSuspendBlocker) {
            this.mWakeLockSuspendBlocker.acquire();
            this.mHoldingWakeLockSuspendBlocker = true;
        }
        if (needDisplaySuspendBlocker && !this.mHoldingDisplaySuspendBlocker) {
            this.mDisplaySuspendBlocker.acquire();
            this.mHoldingDisplaySuspendBlocker = true;
        }
        if (this.mDecoupleHalInteractiveModeFromDisplayConfig && (interactive || this.mDisplayReady)) {
            setHalInteractiveModeLocked(interactive);
        }
        if (!needWakeLockSuspendBlocker && this.mHoldingWakeLockSuspendBlocker) {
            this.mWakeLockSuspendBlocker.release();
            this.mHoldingWakeLockSuspendBlocker = false;
        }
        if (!needDisplaySuspendBlocker && this.mHoldingDisplaySuspendBlocker) {
            this.mDisplaySuspendBlocker.release();
            this.mHoldingDisplaySuspendBlocker = false;
        }
        if (!autoSuspend || !this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            return;
        }
        setHalAutoSuspendModeLocked(true);
    }

    private boolean needDisplaySuspendBlockerLocked() {
        if (this.mDisplayReady) {
            return (this.mDisplayPowerRequest.isBrightOrDim() && !(this.mDisplayPowerRequest.useProximitySensor && this.mProximityPositive && this.mSuspendWhenScreenOffDueToProximityConfig)) || this.mScreenBrightnessBoostInProgress;
        }
        return true;
    }

    private void setHalAutoSuspendModeLocked(boolean enable) {
        if (enable == this.mHalAutoSuspendModeEnabled) {
            return;
        }
        Slog.d(TAG, "Setting HAL auto-suspend mode to " + enable);
        this.mHalAutoSuspendModeEnabled = enable;
        Trace.traceBegin(524288L, "setHalAutoSuspend(" + enable + ")");
        try {
            nativeSetAutoSuspend(enable);
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    private void setHalInteractiveModeLocked(boolean enable) {
        if (enable == this.mHalInteractiveModeEnabled) {
            return;
        }
        Slog.d(TAG, "Setting HAL interactive mode to " + enable);
        this.mHalInteractiveModeEnabled = enable;
        Trace.traceBegin(524288L, "setHalInteractive(" + enable + ")");
        try {
            nativeSetInteractive(enable);
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    private boolean isInteractiveInternal() {
        boolean zIsInteractive;
        synchronized (this.mLock) {
            zIsInteractive = PowerManagerInternal.isInteractive(this.mWakefulness);
        }
        return zIsInteractive;
    }

    private boolean isLowPowerModeInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mLowPowerModeEnabled;
        }
        return z;
    }

    private boolean setLowPowerModeInternal(boolean mode) {
        synchronized (this.mLock) {
            Slog.d(TAG, "setLowPowerModeInternal " + mode + " mIsPowered=" + this.mIsPowered);
            if (this.mIsPowered) {
                return false;
            }
            Settings.Global.putInt(this.mContext.getContentResolver(), "low_power", mode ? 1 : 0);
            this.mLowPowerModeSetting = mode;
            if (this.mAutoLowPowerModeConfigured && this.mBatteryLevelLow) {
                if (mode && this.mAutoLowPowerModeSnoozing) {
                    this.mAutoLowPowerModeSnoozing = false;
                } else if (!mode && !this.mAutoLowPowerModeSnoozing) {
                    this.mAutoLowPowerModeSnoozing = true;
                }
            }
            updateLowPowerModeLocked();
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

    private void shutdownOrRebootInternal(final int haltMode, final boolean confirm, final String reason, boolean wait) {
        if (this.mHandler == null || !this.mSystemReady) {
            throw new IllegalStateException("Too early to call shutdown() or reboot()");
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (haltMode == 2) {
                        ShutdownThread.rebootSafeMode(PowerManagerService.this.mContext, confirm);
                    } else if (haltMode == 1) {
                        ShutdownThread.reboot(PowerManagerService.this.mContext, reason, confirm);
                    } else {
                        ShutdownThread.shutdown(PowerManagerService.this.mContext, reason, confirm);
                    }
                }
            }
        };
        Message msg = Message.obtain(this.mHandler, runnable);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
        if (!wait) {
            return;
        }
        synchronized (runnable) {
            while (true) {
                try {
                    runnable.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void crashInternal(final String message) {
        Thread t = new Thread("PowerManagerService.crash()") {
            @Override
            public void run() {
                throw new RuntimeException(message);
            }
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            Slog.wtf(TAG, e);
        }
    }

    void setStayOnSettingInternal(int val) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", val);
    }

    void setMaximumScreenOffTimeoutFromDeviceAdminInternal(int timeMs) {
        synchronized (this.mLock) {
            this.mMaximumScreenOffTimeoutFromDeviceAdmin = timeMs;
            this.mDirty |= 32;
            updatePowerStateLocked();
        }
    }

    boolean setDeviceIdleModeInternal(boolean enabled) {
        synchronized (this.mLock) {
            if (this.mDeviceIdleMode != enabled) {
                this.mDeviceIdleMode = enabled;
                updateWakeLockDisabledStatesLocked();
                if (enabled) {
                    EventLogTags.writeDeviceIdleOnPhase("power");
                } else {
                    EventLogTags.writeDeviceIdleOffPhase("power");
                }
                return true;
            }
            return false;
        }
    }

    boolean setLightDeviceIdleModeInternal(boolean enabled) {
        synchronized (this.mLock) {
            if (this.mLightDeviceIdleMode != enabled) {
                this.mLightDeviceIdleMode = enabled;
                return true;
            }
            return false;
        }
    }

    void setDeviceIdleWhitelistInternal(int[] appids) {
        synchronized (this.mLock) {
            this.mDeviceIdleWhitelist = appids;
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void setDeviceIdleTempWhitelistInternal(int[] appids) {
        synchronized (this.mLock) {
            this.mDeviceIdleTempWhitelist = appids;
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void updateUidProcStateInternal(int uid, int procState) {
        synchronized (this.mLock) {
            this.mUidState.put(uid, procState);
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void uidGoneInternal(int uid) {
        synchronized (this.mLock) {
            this.mUidState.delete(uid);
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    private void updateWakeLockDisabledStatesLocked() {
        boolean changed = false;
        int numWakeLocks = this.mWakeLocks.size();
        for (int i = 0; i < numWakeLocks; i++) {
            WakeLock wakeLock = this.mWakeLocks.get(i);
            if ((wakeLock.mFlags & 65535) == 1 && setWakeLockDisabledStateLocked(wakeLock)) {
                changed = true;
                if (wakeLock.mDisabled) {
                    notifyWakeLockReleasedLocked(wakeLock);
                } else {
                    notifyWakeLockAcquiredLocked(wakeLock);
                }
            }
        }
        if (!changed) {
            return;
        }
        this.mDirty |= 1;
        updatePowerStateLocked();
    }

    private boolean setWakeLockDisabledStateLocked(WakeLock wakeLock) {
        int appid;
        if ((wakeLock.mFlags & 65535) == 1) {
            boolean disabled = false;
            if (this.mDeviceIdleMode && (appid = UserHandle.getAppId(wakeLock.mOwnerUid)) >= 10000 && Arrays.binarySearch(this.mDeviceIdleWhitelist, appid) < 0 && Arrays.binarySearch(this.mDeviceIdleTempWhitelist, appid) < 0 && this.mUidState.get(wakeLock.mOwnerUid, 16) > 4) {
                disabled = true;
            }
            if (wakeLock.mDisabled != disabled) {
                wakeLock.mDisabled = disabled;
                return true;
            }
        }
        return false;
    }

    private boolean isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked() {
        return this.mMaximumScreenOffTimeoutFromDeviceAdmin >= 0 && this.mMaximumScreenOffTimeoutFromDeviceAdmin < Integer.MAX_VALUE;
    }

    private void setAttentionLightInternal(boolean on, int color) {
        synchronized (this.mLock) {
            if (!this.mSystemReady) {
                return;
            }
            Light light = this.mAttentionLight;
            light.setFlashing(color, 2, on ? 3 : 0, 0);
        }
    }

    private void boostScreenBrightnessInternal(long eventTime, int uid) {
        synchronized (this.mLock) {
            if (!this.mSystemReady || this.mWakefulness == 0 || eventTime < this.mLastScreenBrightnessBoostTime) {
                return;
            }
            Slog.i(TAG, "Brightness boost activated (uid " + uid + ")...");
            this.mLastScreenBrightnessBoostTime = eventTime;
            if (!this.mScreenBrightnessBoostInProgress) {
                this.mScreenBrightnessBoostInProgress = true;
                this.mNotifier.onScreenBrightnessBoostChanged();
            }
            this.mDirty |= 2048;
            userActivityNoUpdateLocked(eventTime, 0, 0, uid);
            updatePowerStateLocked();
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

    private void setScreenBrightnessOverrideFromWindowManagerInternal(int brightness) {
        synchronized (this.mLock) {
            if (this.mScreenBrightnessOverrideFromWindowManager != brightness) {
                this.mScreenBrightnessOverrideFromWindowManager = brightness;
                Slog.d(TAG, "mScreenBrightnessOverrideFromWindowManager = " + brightness);
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

    private void setUserActivityTimeoutOverrideFromWindowManagerInternal(long timeoutMillis) {
        synchronized (this.mLock) {
            if (this.mUserActivityTimeoutOverrideFromWindowManager != timeoutMillis) {
                Slog.d(TAG, "UA TimeoutOverrideFromWindowManagerInternal = " + timeoutMillis);
                this.mUserActivityTimeoutOverrideFromWindowManager = timeoutMillis;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setTemporaryScreenBrightnessSettingOverrideInternal(int brightness) {
        synchronized (this.mLock) {
            if (this.mTemporaryScreenBrightnessSettingOverride != brightness) {
                this.mTemporaryScreenBrightnessSettingOverride = brightness;
                Slog.d(TAG, "mTemporaryScreenBrightnessSettingOverride = " + brightness);
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setTemporaryScreenAutoBrightnessAdjustmentSettingOverrideInternal(float adj) {
        synchronized (this.mLock) {
            if (this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride != adj) {
                this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride = adj;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setDozeOverrideFromDreamManagerInternal(int screenState, int screenBrightness) {
        synchronized (this.mLock) {
            if (this.mDozeScreenStateOverrideFromDreamManager != screenState || this.mDozeScreenBrightnessOverrideFromDreamManager != screenBrightness) {
                this.mDozeScreenStateOverrideFromDreamManager = screenState;
                this.mDozeScreenBrightnessOverrideFromDreamManager = screenBrightness;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void powerHintInternal(int hintId, int data) {
        nativeSendPowerHint(hintId, data);
    }

    public static void lowLevelShutdown(String reason) {
        if (reason == null) {
            reason = "";
        }
        SystemProperties.set("sys.powerctl", "shutdown," + reason);
    }

    public static void lowLevelReboot(String reason) {
        if (reason == null) {
            reason = "";
        }
        if (reason.equals("recovery") || reason.equals("recovery-update")) {
            SystemProperties.set("sys.powerctl", "reboot,recovery");
        } else {
            SystemProperties.set("sys.powerctl", "reboot," + reason);
        }
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

    private void dumpInternal(PrintWriter pw) {
        WirelessChargerDetector wcd;
        pw.println("POWER MANAGER (dumpsys power)\n");
        synchronized (this.mLock) {
            pw.println("Power Manager State:");
            pw.println("  mDirty=0x" + Integer.toHexString(this.mDirty));
            pw.println("  mWakefulness=" + PowerManagerInternal.wakefulnessToString(this.mWakefulness));
            pw.println("  mWakefulnessChanging=" + this.mWakefulnessChanging);
            pw.println("  mIsPowered=" + this.mIsPowered);
            pw.println("  mPlugType=" + this.mPlugType);
            pw.println("  mBatteryLevel=" + this.mBatteryLevel);
            pw.println("  mBatteryLevelWhenDreamStarted=" + this.mBatteryLevelWhenDreamStarted);
            pw.println("  mDockState=" + this.mDockState);
            pw.println("  mStayOn=" + this.mStayOn);
            pw.println("  mProximityPositive=" + this.mProximityPositive);
            pw.println("  mBootCompleted=" + this.mBootCompleted);
            pw.println("  mSystemReady=" + this.mSystemReady);
            pw.println("  mHalAutoSuspendModeEnabled=" + this.mHalAutoSuspendModeEnabled);
            pw.println("  mHalInteractiveModeEnabled=" + this.mHalInteractiveModeEnabled);
            pw.println("  mWakeLockSummary=0x" + Integer.toHexString(this.mWakeLockSummary));
            pw.println("  mUserActivitySummary=0x" + Integer.toHexString(this.mUserActivitySummary));
            pw.println("  mRequestWaitForNegativeProximity=" + this.mRequestWaitForNegativeProximity);
            pw.println("  mSandmanScheduled=" + this.mSandmanScheduled);
            pw.println("  mSandmanSummoned=" + this.mSandmanSummoned);
            pw.println("  mLowPowerModeEnabled=" + this.mLowPowerModeEnabled);
            pw.println("  mBatteryLevelLow=" + this.mBatteryLevelLow);
            pw.println("  mLightDeviceIdleMode=" + this.mLightDeviceIdleMode);
            pw.println("  mDeviceIdleMode=" + this.mDeviceIdleMode);
            pw.println("  mDeviceIdleWhitelist=" + Arrays.toString(this.mDeviceIdleWhitelist));
            pw.println("  mDeviceIdleTempWhitelist=" + Arrays.toString(this.mDeviceIdleTempWhitelist));
            pw.println("  mLastWakeTime=" + TimeUtils.formatUptime(this.mLastWakeTime));
            pw.println("  mLastSleepTime=" + TimeUtils.formatUptime(this.mLastSleepTime));
            pw.println("  mLastUserActivityTime=" + TimeUtils.formatUptime(this.mLastUserActivityTime));
            pw.println("  mLastUserActivityTimeNoChangeLights=" + TimeUtils.formatUptime(this.mLastUserActivityTimeNoChangeLights));
            pw.println("  mLastInteractivePowerHintTime=" + TimeUtils.formatUptime(this.mLastInteractivePowerHintTime));
            pw.println("  mLastScreenBrightnessBoostTime=" + TimeUtils.formatUptime(this.mLastScreenBrightnessBoostTime));
            pw.println("  mScreenBrightnessBoostInProgress=" + this.mScreenBrightnessBoostInProgress);
            pw.println("  mDisplayReady=" + this.mDisplayReady);
            pw.println("  mHoldingWakeLockSuspendBlocker=" + this.mHoldingWakeLockSuspendBlocker);
            pw.println("  mHoldingDisplaySuspendBlocker=" + this.mHoldingDisplaySuspendBlocker);
            pw.println();
            pw.println("Settings and Configuration:");
            pw.println("  mDecoupleHalAutoSuspendModeFromDisplayConfig=" + this.mDecoupleHalAutoSuspendModeFromDisplayConfig);
            pw.println("  mDecoupleHalInteractiveModeFromDisplayConfig=" + this.mDecoupleHalInteractiveModeFromDisplayConfig);
            pw.println("  mWakeUpWhenPluggedOrUnpluggedConfig=" + this.mWakeUpWhenPluggedOrUnpluggedConfig);
            pw.println("  mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig=" + this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig);
            pw.println("  mTheaterModeEnabled=" + this.mTheaterModeEnabled);
            pw.println("  mSuspendWhenScreenOffDueToProximityConfig=" + this.mSuspendWhenScreenOffDueToProximityConfig);
            pw.println("  mDreamsSupportedConfig=" + this.mDreamsSupportedConfig);
            pw.println("  mDreamsEnabledByDefaultConfig=" + this.mDreamsEnabledByDefaultConfig);
            pw.println("  mDreamsActivatedOnSleepByDefaultConfig=" + this.mDreamsActivatedOnSleepByDefaultConfig);
            pw.println("  mDreamsActivatedOnDockByDefaultConfig=" + this.mDreamsActivatedOnDockByDefaultConfig);
            pw.println("  mDreamsEnabledOnBatteryConfig=" + this.mDreamsEnabledOnBatteryConfig);
            pw.println("  mDreamsBatteryLevelMinimumWhenPoweredConfig=" + this.mDreamsBatteryLevelMinimumWhenPoweredConfig);
            pw.println("  mDreamsBatteryLevelMinimumWhenNotPoweredConfig=" + this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig);
            pw.println("  mDreamsBatteryLevelDrainCutoffConfig=" + this.mDreamsBatteryLevelDrainCutoffConfig);
            pw.println("  mDreamsEnabledSetting=" + this.mDreamsEnabledSetting);
            pw.println("  mDreamsActivateOnSleepSetting=" + this.mDreamsActivateOnSleepSetting);
            pw.println("  mDreamsActivateOnDockSetting=" + this.mDreamsActivateOnDockSetting);
            pw.println("  mDozeAfterScreenOffConfig=" + this.mDozeAfterScreenOffConfig);
            pw.println("  mLowPowerModeSetting=" + this.mLowPowerModeSetting);
            pw.println("  mAutoLowPowerModeConfigured=" + this.mAutoLowPowerModeConfigured);
            pw.println("  mAutoLowPowerModeSnoozing=" + this.mAutoLowPowerModeSnoozing);
            pw.println("  mMinimumScreenOffTimeoutConfig=" + this.mMinimumScreenOffTimeoutConfig);
            pw.println("  mMaximumScreenDimDurationConfig=" + this.mMaximumScreenDimDurationConfig);
            pw.println("  mMaximumScreenDimRatioConfig=" + this.mMaximumScreenDimRatioConfig);
            pw.println("  mScreenOffTimeoutSetting=" + this.mScreenOffTimeoutSetting);
            pw.println("  mSleepTimeoutSetting=" + this.mSleepTimeoutSetting);
            pw.println("  mMaximumScreenOffTimeoutFromDeviceAdmin=" + this.mMaximumScreenOffTimeoutFromDeviceAdmin + " (enforced=" + isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked() + ")");
            pw.println("  mStayOnWhilePluggedInSetting=" + this.mStayOnWhilePluggedInSetting);
            pw.println("  mScreenBrightnessSetting=" + this.mScreenBrightnessSetting);
            pw.println("  mScreenAutoBrightnessAdjustmentSetting=" + this.mScreenAutoBrightnessAdjustmentSetting);
            pw.println("  mScreenBrightnessModeSetting=" + this.mScreenBrightnessModeSetting);
            pw.println("  mScreenBrightnessOverrideFromWindowManager=" + this.mScreenBrightnessOverrideFromWindowManager);
            pw.println("  mUserActivityTimeoutOverrideFromWindowManager=" + this.mUserActivityTimeoutOverrideFromWindowManager);
            pw.println("  mUserInactiveOverrideFromWindowManager=" + this.mUserInactiveOverrideFromWindowManager);
            pw.println("  mTemporaryScreenBrightnessSettingOverride=" + this.mTemporaryScreenBrightnessSettingOverride);
            pw.println("  mTemporaryScreenAutoBrightnessAdjustmentSettingOverride=" + this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride);
            pw.println("  mDozeScreenStateOverrideFromDreamManager=" + this.mDozeScreenStateOverrideFromDreamManager);
            pw.println("  mDozeScreenBrightnessOverrideFromDreamManager=" + this.mDozeScreenBrightnessOverrideFromDreamManager);
            pw.println("  mScreenBrightnessSettingMinimum=" + this.mScreenBrightnessSettingMinimum);
            pw.println("  mScreenBrightnessSettingMaximum=" + this.mScreenBrightnessSettingMaximum);
            pw.println("  mScreenBrightnessSettingDefault=" + this.mScreenBrightnessSettingDefault);
            pw.println("  mDoubleTapWakeEnabled=" + this.mDoubleTapWakeEnabled);
            int sleepTimeout = getSleepTimeoutLocked();
            int screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout);
            int screenDimDuration = getScreenDimDurationLocked(screenOffTimeout);
            pw.println();
            pw.println("Sleep timeout: " + sleepTimeout + " ms");
            pw.println("Screen off timeout: " + screenOffTimeout + " ms");
            pw.println("Screen dim duration: " + screenDimDuration + " ms");
            pw.println();
            pw.println("UID states:");
            for (int i = 0; i < this.mUidState.size(); i++) {
                pw.print("  UID ");
                UserHandle.formatUid(pw, this.mUidState.keyAt(i));
                pw.print(": ");
                pw.println(this.mUidState.valueAt(i));
            }
            pw.println();
            pw.println("Wake Locks: size=" + this.mWakeLocks.size());
            for (WakeLock wl : this.mWakeLocks) {
                pw.println("  " + wl);
            }
            pw.println();
            pw.println("Suspend Blockers: size=" + this.mSuspendBlockers.size());
            for (SuspendBlocker sb : this.mSuspendBlockers) {
                pw.println("  " + sb);
            }
            pw.println();
            pw.println("Display Power: " + this.mDisplayPowerCallbacks);
            wcd = this.mWirelessChargerDetector;
        }
        if (wcd != null) {
            wcd.dump(pw);
        }
    }

    private SuspendBlocker createSuspendBlockerLocked(String name) {
        SuspendBlocker suspendBlocker = new SuspendBlockerImpl(name);
        this.mSuspendBlockers.add(suspendBlocker);
        return suspendBlocker;
    }

    private void incrementBootCount() {
        int count;
        synchronized (this.mLock) {
            try {
                count = Settings.Global.getInt(getContext().getContentResolver(), "boot_count");
            } catch (Settings.SettingNotFoundException e) {
                count = 0;
            }
            Settings.Global.putInt(getContext().getContentResolver(), "boot_count", count + 1);
        }
    }

    private static WorkSource copyWorkSource(WorkSource workSource) {
        if (workSource != null) {
            return new WorkSource(workSource);
        }
        return null;
    }

    private final class BatteryReceiver extends BroadcastReceiver {
        BatteryReceiver(PowerManagerService this$0, BatteryReceiver batteryReceiver) {
            this();
        }

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
        DreamReceiver(PowerManagerService this$0, DreamReceiver dreamReceiver) {
            this();
        }

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
        UserSwitchedReceiver(PowerManagerService this$0, UserSwitchedReceiver userSwitchedReceiver) {
            this();
        }

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
        DockReceiver(PowerManagerService this$0, DockReceiver dockReceiver) {
            this();
        }

        private DockReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                int dockState = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                if (PowerManagerService.this.mDockState != dockState) {
                    PowerManagerService.this.mDockState = dockState;
                    PowerManagerService.this.mDirty |= 1024;
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }
        }
    }

    private final class WifiDisplayStatusChangedReceiver extends BroadcastReceiver {
        WifiDisplayStatusChangedReceiver(PowerManagerService this$0, WifiDisplayStatusChangedReceiver wifiDisplayStatusChangedReceiver) {
            this();
        }

        private WifiDisplayStatusChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                if (intent.getAction().equals("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED")) {
                    WifiDisplayStatus wfdStatus = intent.getParcelableExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS");
                    PowerManagerService.this.mWfdEnabled = 2 == wfdStatus.getActiveDisplayState();
                    Slog.d(PowerManagerService.TAG, "<<<<< WifiDisplayStatusChangedReceiver >>>>> mWfdEnabled = " + PowerManagerService.this.mWfdEnabled);
                }
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
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
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PowerManagerService.this.handleUserActivityTimeout();
                    break;
                case 2:
                    PowerManagerService.this.handleSandman();
                    break;
                case 3:
                    PowerManagerService.this.handleScreenBrightnessBoostTimeout();
                    break;
            }
        }
    }

    private final class WakeLock implements IBinder.DeathRecipient {
        public boolean mDisabled;
        public int mFlags;
        public String mHistoryTag;
        public final IBinder mLock;
        public boolean mNotifiedAcquired;
        public final int mOwnerPid;
        public final int mOwnerUid;
        public final String mPackageName;
        public String mTag;
        public WorkSource mWorkSource;
        public long mActiveSince = 0;
        public long mTotalTime = 0;

        public WakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource workSource, String historyTag, int ownerUid, int ownerPid) {
            this.mLock = lock;
            this.mFlags = flags;
            this.mTag = tag;
            this.mPackageName = packageName;
            this.mWorkSource = PowerManagerService.copyWorkSource(workSource);
            this.mHistoryTag = historyTag;
            this.mOwnerUid = ownerUid;
            this.mOwnerPid = ownerPid;
        }

        @Override
        public void binderDied() {
            PowerManagerService.this.handleWakeLockDeath(this);
        }

        public boolean hasSameProperties(int flags, String tag, WorkSource workSource, int ownerUid, int ownerPid) {
            return this.mFlags == flags && this.mTag.equals(tag) && hasSameWorkSource(workSource) && this.mOwnerUid == ownerUid && this.mOwnerPid == ownerPid;
        }

        public void updateProperties(int flags, String tag, String packageName, WorkSource workSource, String historyTag, int ownerUid, int ownerPid) {
            if (!this.mPackageName.equals(packageName)) {
                throw new IllegalStateException("Existing wake lock package name changed: " + this.mPackageName + " to " + packageName);
            }
            if (this.mOwnerUid != ownerUid) {
                throw new IllegalStateException("Existing wake lock uid changed: " + this.mOwnerUid + " to " + ownerUid);
            }
            if (this.mOwnerPid != ownerPid) {
                throw new IllegalStateException("Existing wake lock pid changed: " + this.mOwnerPid + " to " + ownerPid);
            }
            this.mFlags = flags;
            this.mTag = tag;
            updateWorkSource(workSource);
            this.mHistoryTag = historyTag;
        }

        public boolean hasSameWorkSource(WorkSource workSource) {
            return Objects.equal(this.mWorkSource, workSource);
        }

        public void updateWorkSource(WorkSource workSource) {
            this.mWorkSource = PowerManagerService.copyWorkSource(workSource);
        }

        public String toString() {
            return getLockLevelString() + " '" + this.mTag + "'" + getLockFlagsString() + (this.mDisabled ? " DISABLED" : "") + " (uid=" + this.mOwnerUid + ", pid=" + this.mOwnerPid + ", ws=" + this.mWorkSource + ")";
        }

        private String getLockLevelString() {
            switch (this.mFlags & 65535) {
                case 1:
                    return "PARTIAL_WAKE_LOCK             ";
                case 6:
                    return "SCREEN_DIM_WAKE_LOCK          ";
                case 10:
                    return "SCREEN_BRIGHT_WAKE_LOCK       ";
                case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                    return "FULL_WAKE_LOCK                ";
                case 32:
                    return "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
                case 64:
                    return "DOZE_WAKE_LOCK                ";
                case 128:
                    return "DRAW_WAKE_LOCK                ";
                default:
                    return "???                           ";
            }
        }

        private String getLockFlagsString() {
            String result = (this.mFlags & 268435456) != 0 ? " ACQUIRE_CAUSES_WAKEUP" : "";
            if ((this.mFlags & 536870912) != 0) {
                return result + " ON_AFTER_RELEASE";
            }
            return result;
        }
    }

    private final class SuspendBlockerImpl implements SuspendBlocker {
        private final String mName;
        private int mReferenceCount;
        private final String mTraceName;

        public SuspendBlockerImpl(String name) {
            this.mName = name;
            this.mTraceName = "SuspendBlocker (" + name + ")";
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mReferenceCount != 0) {
                    Slog.wtf(PowerManagerService.TAG, "Suspend blocker \"" + this.mName + "\" was finalized without being released!");
                    this.mReferenceCount = 0;
                    PowerManagerService.nativeReleaseSuspendBlocker(this.mName);
                    Trace.asyncTraceEnd(524288L, this.mTraceName, 0);
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
                    Trace.asyncTraceBegin(524288L, this.mTraceName, 0);
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
                    Trace.asyncTraceEnd(524288L, this.mTraceName, 0);
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
    }

    private final class BinderService extends IPowerManager.Stub {
        BinderService(PowerManagerService this$0, BinderService binderService) {
            this();
        }

        private BinderService() {
        }

        public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uid) {
            if (uid < 0) {
                uid = Binder.getCallingUid();
            }
            acquireWakeLock(lock, flags, tag, packageName, new WorkSource(uid), null);
        }

        public void powerHint(int hintId, int data) {
            if (!PowerManagerService.this.mSystemReady) {
                return;
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.powerHintInternal(hintId, data);
        }

        public void acquireWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag) {
            if (lock == null) {
                throw new IllegalArgumentException("lock must not be null");
            }
            if (packageName == null) {
                throw new IllegalArgumentException("packageName must not be null");
            }
            PowerManager.validateWakeLockParameters(flags, tag);
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
            if ((flags & 64) != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            }
            if (ws != null && ws.size() != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
            } else {
                ws = null;
            }
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.acquireWakeLockInternal(lock, flags, tag, packageName, ws, historyTag, uid, pid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void releaseWakeLock(IBinder lock, int flags) {
            if (lock == null) {
                throw new IllegalArgumentException("lock must not be null");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.releaseWakeLockInternal(lock, flags);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void updateWakeLockUids(IBinder lock, int[] uids) {
            WorkSource ws = null;
            if (uids != null) {
                ws = new WorkSource();
                for (int i : uids) {
                    ws.add(i);
                }
            }
            updateWakeLockWorkSource(lock, ws, null);
        }

        public void updateWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag) {
            if (lock == null) {
                throw new IllegalArgumentException("lock must not be null");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
            if (ws != null && ws.size() != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
            } else {
                ws = null;
            }
            int callingUid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.updateWakeLockWorkSourceInternal(lock, ws, historyTag, callingUid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isWakeLockLevelSupported(int level) {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isWakeLockLevelSupportedInternal(level);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void userActivity(long eventTime, int event, int flags) {
            long now = SystemClock.uptimeMillis();
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0 && PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.USER_ACTIVITY") != 0) {
                synchronized (PowerManagerService.this.mLock) {
                    if (now >= PowerManagerService.this.mLastWarningAboutUserActivityPermission + 300000) {
                        PowerManagerService.this.mLastWarningAboutUserActivityPermission = now;
                        Slog.w(PowerManagerService.TAG, "Ignoring call to PowerManager.userActivity() because the caller does not have DEVICE_POWER or USER_ACTIVITY permission.  Please fix your app!   pid=" + Binder.getCallingPid() + " uid=" + Binder.getCallingUid());
                    }
                }
                return;
            }
            if (eventTime > now) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.userActivityInternal(eventTime, event, flags, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void wakeUp(long eventTime, String reason, String opPackageName) {
            if (eventTime > SystemClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.wakeUpInternal(eventTime, reason, uid, opPackageName, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void goToSleep(long eventTime, int reason, int flags) {
            if (eventTime > SystemClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.goToSleepInternal(eventTime, reason, flags, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void nap(long eventTime) {
            if (eventTime > SystemClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.napInternal(eventTime, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isInteractive() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isInteractiveInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isPowerSaveMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isLowPowerModeInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setPowerSaveMode(boolean mode) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.setLowPowerModeInternal(mode);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isDeviceIdleMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isDeviceIdleModeInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isLightDeviceIdleMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isLightDeviceIdleModeInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void reboot(boolean confirm, String reason, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            if ("recovery".equals(reason) || "recovery-update".equals(reason)) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(1, confirm, reason, wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void rebootSafeMode(boolean confirm, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(2, confirm, "safemode", wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void shutdown(boolean confirm, String reason, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(0, confirm, reason, wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void crash(String message) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.crashInternal(message);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setStayOnSetting(int val) {
            int uid = Binder.getCallingUid();
            if (uid != 0 && !Settings.checkAndNoteWriteSettingsOperation(PowerManagerService.this.mContext, uid, Settings.getPackageNameForUid(PowerManagerService.this.mContext, uid), true)) {
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setStayOnSettingInternal(val);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setTemporaryScreenBrightnessSettingOverride(int brightness) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setTemporaryScreenBrightnessSettingOverrideInternal(brightness);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(float adj) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setTemporaryScreenAutoBrightnessAdjustmentSettingOverrideInternal(adj);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setAttentionLight(boolean on, int color) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setAttentionLightInternal(on, color);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void boostScreenBrightness(long eventTime) {
            if (eventTime > SystemClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.boostScreenBrightnessInternal(eventTime, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isScreenBrightnessBoosted() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isScreenBrightnessBoostedInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump PowerManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.dumpInternal(pw);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void startBacklight(int delay_msec) {
            synchronized (PowerManagerService.this.mLock) {
                if (SystemProperties.get("ro.mtk_ipo_support").equals("1")) {
                    Slog.d(PowerManagerService.TAG, "startBacklight");
                    PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                    long ident = Binder.clearCallingIdentity();
                    try {
                        PowerManagerService.this.mDisplayManagerInternal.setIPOScreenOnDelay(delay_msec);
                        PowerManagerService.this.wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), "android.server.power:POWER", 1000, PowerManagerService.this.mContext.getOpPackageName(), 1000);
                        PowerManagerService.this.updatePowerStateLocked();
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                } else {
                    Slog.d(PowerManagerService.TAG, "skip startBacklight because MTK_IPO_SUPPORT not enabled");
                }
            }
        }

        public void stopBacklight() {
            synchronized (PowerManagerService.this.mLock) {
                if (SystemProperties.get("ro.mtk_ipo_support").equals("1")) {
                    Slog.d(PowerManagerService.TAG, "stopBacklight");
                    PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                    long ident = Binder.clearCallingIdentity();
                    try {
                        PowerManagerService.this.mDisplayManagerInternal.setIPOScreenOnDelay(0);
                        PowerManagerService.this.goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
                        PowerManagerService.this.updatePowerStateLocked();
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                } else {
                    Slog.d(PowerManagerService.TAG, "skip stopBacklight because MTK_IPO_SUPPORT not enabled");
                }
            }
        }

        public void setBacklightOffForWfd(boolean enable) {
            if (enable) {
                Slog.d(PowerManagerService.TAG, "setBacklightOffForWfd true");
                PowerManagerService.this.mBacklight.setBrightness(0);
            } else if (!PowerManagerService.this.mWfdShouldBypass) {
                Slog.d(PowerManagerService.TAG, "setBacklightOffForWfd false");
                PowerManagerService.this.mBacklight.setBrightness(PowerManagerService.this.mScreenBrightnessSetting);
            } else {
                Slog.d(PowerManagerService.TAG, "setBacklightOffForWfd false ignored due to screen is off by power key");
            }
        }
    }

    private final class LocalService extends PowerManagerInternal {
        LocalService(PowerManagerService this$0, LocalService localService) {
            this();
        }

        private LocalService() {
        }

        public void setScreenBrightnessOverrideFromWindowManager(int screenBrightness) {
            if (screenBrightness < -1 || screenBrightness > 255) {
                screenBrightness = -1;
            }
            PowerManagerService.this.setScreenBrightnessOverrideFromWindowManagerInternal(screenBrightness);
        }

        public void setButtonBrightnessOverrideFromWindowManager(int screenBrightness) {
        }

        public void setDozeOverrideFromDreamManager(int screenState, int screenBrightness) {
            switch (screenState) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    break;
                default:
                    screenState = 0;
                    break;
            }
            if (screenBrightness < -1 || screenBrightness > 255) {
                screenBrightness = -1;
            }
            PowerManagerService.this.setDozeOverrideFromDreamManagerInternal(screenState, screenBrightness);
        }

        public void setUserInactiveOverrideFromWindowManager() {
            PowerManagerService.this.setUserInactiveOverrideFromWindowManagerInternal();
        }

        public void setUserActivityTimeoutOverrideFromWindowManager(long timeoutMillis) {
            PowerManagerService.this.setUserActivityTimeoutOverrideFromWindowManagerInternal(timeoutMillis);
        }

        public void setMaximumScreenOffTimeoutFromDeviceAdmin(int timeMs) {
            PowerManagerService.this.setMaximumScreenOffTimeoutFromDeviceAdminInternal(timeMs);
        }

        public boolean getLowPowerModeEnabled() {
            boolean z;
            synchronized (PowerManagerService.this.mLock) {
                z = PowerManagerService.this.mLowPowerModeEnabled;
            }
            return z;
        }

        public void registerLowPowerModeObserver(PowerManagerInternal.LowPowerModeListener listener) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.mLowPowerModeListeners.add(listener);
            }
        }

        public boolean setDeviceIdleMode(boolean enabled) {
            return PowerManagerService.this.setDeviceIdleModeInternal(enabled);
        }

        public boolean setLightDeviceIdleMode(boolean enabled) {
            return PowerManagerService.this.setLightDeviceIdleModeInternal(enabled);
        }

        public void setDeviceIdleWhitelist(int[] appids) {
            PowerManagerService.this.setDeviceIdleWhitelistInternal(appids);
        }

        public void setDeviceIdleTempWhitelist(int[] appids) {
            PowerManagerService.this.setDeviceIdleTempWhitelistInternal(appids);
        }

        public void updateUidProcState(int uid, int procState) {
            PowerManagerService.this.updateUidProcStateInternal(uid, procState);
        }

        public void uidGone(int uid) {
            PowerManagerService.this.uidGoneInternal(uid);
        }

        public void powerHint(int hintId, int data) {
            PowerManagerService.this.powerHintInternal(hintId, data);
        }
    }
}
