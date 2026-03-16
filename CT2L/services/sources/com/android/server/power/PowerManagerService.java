package com.android.server.power;

import android.R;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.SystemSensorManager;
import android.hardware.display.DisplayManagerInternal;
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
import android.os.WorkSource;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.Display;
import android.view.WindowManagerPolicy;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BackgroundThread;
import com.android.server.EventLogTags;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.am.BatteryStatsService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.wm.WindowManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import libcore.util.Objects;

public final class PowerManagerService extends SystemService implements Watchdog.Monitor {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SPEW = false;
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT = 15000;
    private static final int DEFAULT_SLEEP_TIMEOUT = -1;
    private static final int DIRTY_ACTUAL_DISPLAY_POWER_STATE_UPDATED = 8;
    private static final int DIRTY_BATTERY_STATE = 256;
    private static final int DIRTY_BOOT_COMPLETED = 16;
    private static final int DIRTY_DOCK_STATE = 1024;
    private static final int DIRTY_IS_POWERED = 64;
    private static final int DIRTY_PROXIMITY_POSITIVE = 512;
    private static final int DIRTY_SCREEN_BRIGHTNESS_BOOST = 2048;
    private static final int DIRTY_SETTINGS = 32;
    private static final int DIRTY_STAY_ON = 128;
    private static final int DIRTY_USER_ACTIVITY = 4;
    private static final int DIRTY_WAKEFULNESS = 2;
    private static final int DIRTY_WAKE_LOCKS = 1;
    private static final int MSG_SANDMAN = 2;
    private static final int MSG_SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 3;
    private static final int MSG_USER_ACTIVITY_TIMEOUT = 1;
    private static final int POWER_HINT_INTERACTION = 2;
    private static final int POWER_HINT_LOW_POWER = 5;
    private static final int SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 5000;
    private static final String TAG = "PowerManagerService";
    private static final int USER_ACTIVITY_SCREEN_BRIGHT = 1;
    private static final int USER_ACTIVITY_SCREEN_DIM = 2;
    private static final int USER_ACTIVITY_SCREEN_DREAM = 4;
    private static final int WAKE_LOCK_BUTTON_BRIGHT = 8;
    private static final int WAKE_LOCK_CPU = 1;
    private static final int WAKE_LOCK_DOZE = 64;
    private static final int WAKE_LOCK_PROXIMITY_SCREEN_OFF = 16;
    private static final int WAKE_LOCK_SCREEN_BRIGHT = 2;
    private static final int WAKE_LOCK_SCREEN_DIM = 4;
    private static final int WAKE_LOCK_STAY_AWAKE = 32;
    private IAppOpsService mAppOps;
    private Light mAttentionLight;
    private boolean mAutoLowPowerModeConfigured;
    private boolean mAutoLowPowerModeSnoozing;
    private int mBatteryLevel;
    private boolean mBatteryLevelLow;
    private int mBatteryLevelWhenDreamStarted;
    private BatteryManagerInternal mBatteryManagerInternal;
    private IBatteryStats mBatteryStats;
    private boolean mBootCompleted;
    private final Context mContext;
    private boolean mDecoupleHalAutoSuspendModeFromDisplayConfig;
    private boolean mDecoupleHalInteractiveModeFromDisplayConfig;
    private int mDirty;
    private DisplayManagerInternal mDisplayManagerInternal;
    private final DisplayManagerInternal.DisplayPowerCallbacks mDisplayPowerCallbacks;
    private final DisplayManagerInternal.DisplayPowerRequest mDisplayPowerRequest;
    private boolean mDisplayReady;
    private final SuspendBlocker mDisplaySuspendBlocker;
    private int mDockState;
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
    private boolean mIsPowered;
    private long mLastInteractivePowerHintTime;
    private long mLastScreenBrightnessBoostTime;
    private long mLastSleepTime;
    private long mLastUserActivityTime;
    private long mLastUserActivityTimeNoChangeLights;
    private long mLastWakeTime;
    private long mLastWarningAboutUserActivityPermission;
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
    private int mPlugType;
    private int mPlugType_all;
    private WindowManagerPolicy mPolicy;
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
    private int mSleepTimeoutSetting;
    private boolean mStayOn;
    private int mStayOnWhilePluggedInSetting;
    private final ArrayList<SuspendBlocker> mSuspendBlockers;
    private boolean mSuspendWhenScreenOffDueToProximityConfig;
    private boolean mSystemReady;
    private float mTemporaryScreenAutoBrightnessAdjustmentSettingOverride;
    private int mTemporaryScreenBrightnessSettingOverride;
    private boolean mTheaterModeEnabled;
    private int mUserActivitySummary;
    private long mUserActivityTimeoutOverrideFromWindowManager;
    private int mWakeLockSummary;
    private final SuspendBlocker mWakeLockSuspendBlocker;
    private final ArrayList<WakeLock> mWakeLocks;
    private boolean mWakeUpWhenPluggedOrUnpluggedConfig;
    private boolean mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig;
    private int mWakefulness;
    private boolean mWakefulnessChanging;
    private WirelessChargerDetector mWirelessChargerDetector;

    private static native void nativeAcquireSuspendBlocker(String str);

    private native void nativeInit();

    private static native void nativeReleaseSuspendBlocker(String str);

    private static native void nativeSendPowerHint(int i, int i2);

    private static native void nativeSetAutoSuspend(boolean z);

    private static native void nativeSetInteractive(boolean z);

    static int access$1076(PowerManagerService x0, int x1) {
        int i = x0.mDirty | x1;
        x0.mDirty = i;
        return i;
    }

    public PowerManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mSuspendBlockers = new ArrayList<>();
        this.mWakeLocks = new ArrayList<>();
        this.mDisplayPowerRequest = new DisplayManagerInternal.DisplayPowerRequest();
        this.mDockState = 0;
        this.mMaximumScreenOffTimeoutFromDeviceAdmin = Integer.MAX_VALUE;
        this.mScreenBrightnessOverrideFromWindowManager = -1;
        this.mUserActivityTimeoutOverrideFromWindowManager = -1L;
        this.mTemporaryScreenBrightnessSettingOverride = -1;
        this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride = Float.NaN;
        this.mDozeScreenStateOverrideFromDreamManager = 0;
        this.mDozeScreenBrightnessOverrideFromDreamManager = -1;
        this.mLastWarningAboutUserActivityPermission = Long.MIN_VALUE;
        this.mLowPowerModeListeners = new ArrayList<>();
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
        }
    }

    @Override
    public void onStart() {
        publishBinderService("power", new BinderService());
        publishLocalService(PowerManagerInternal.class, new LocalService());
        Watchdog.getInstance().addMonitor(this);
        Watchdog.getInstance().addThread(this.mHandler);
    }

    @Override
    public void onBootPhase(int phase) {
        synchronized (this.mLock) {
            if (phase == 1000) {
                long now = SystemClock.uptimeMillis();
                this.mBootCompleted = true;
                this.mDirty |= 16;
                userActivityNoUpdateLocked(now, 0, 0, 1000);
                updatePowerStateLocked();
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
            this.mDisplayManagerInternal.initPowerManagement(this.mDisplayPowerCallbacks, this.mHandler, systemSensorManager);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            filter.setPriority(1000);
            this.mContext.registerReceiver(new BatteryReceiver(), filter, null, this.mHandler);
            IntentFilter filter2 = new IntentFilter();
            filter2.addAction("android.intent.action.DREAMING_STARTED");
            filter2.addAction("android.intent.action.DREAMING_STOPPED");
            this.mContext.registerReceiver(new DreamReceiver(), filter2, null, this.mHandler);
            IntentFilter filter3 = new IntentFilter();
            filter3.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiver(new UserSwitchedReceiver(), filter3, null, this.mHandler);
            IntentFilter filter4 = new IntentFilter();
            filter4.addAction("android.intent.action.DOCK_EVENT");
            this.mContext.registerReceiver(new DockReceiver(), filter4, null, this.mHandler);
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
            readConfigurationLocked();
            updateSettingsLocked();
            this.mDirty |= 256;
            updatePowerStateLocked();
        }
    }

    private void readConfigurationLocked() {
        Resources resources = this.mContext.getResources();
        this.mDecoupleHalAutoSuspendModeFromDisplayConfig = resources.getBoolean(R.^attr-private.headerRemoveIconIfEmpty);
        this.mDecoupleHalInteractiveModeFromDisplayConfig = resources.getBoolean(R.^attr-private.headerTextColor);
        this.mWakeUpWhenPluggedOrUnpluggedConfig = resources.getBoolean(R.^attr-private.buttonPanelSideLayout);
        this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig = resources.getBoolean(R.^attr-private.closeItemLayout);
        this.mSuspendWhenScreenOffDueToProximityConfig = resources.getBoolean(R.^attr-private.dayHighlightColor);
        this.mDreamsSupportedConfig = resources.getBoolean(R.^attr-private.gestureOverlayViewStyle);
        this.mDreamsEnabledByDefaultConfig = resources.getBoolean(R.^attr-private.glowDot);
        this.mDreamsActivatedOnSleepByDefaultConfig = resources.getBoolean(R.^attr-private.glyphMap);
        this.mDreamsActivatedOnDockByDefaultConfig = resources.getBoolean(R.^attr-private.glyphDrawable);
        this.mDreamsEnabledOnBatteryConfig = resources.getBoolean(R.^attr-private.hasRoundedCorners);
        this.mDreamsBatteryLevelMinimumWhenPoweredConfig = resources.getInteger(R.integer.config_defaultVibrationAmplitude);
        this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig = resources.getInteger(R.integer.config_default_cellular_usage_setting);
        this.mDreamsBatteryLevelDrainCutoffConfig = resources.getInteger(R.integer.config_delay_for_ims_dereg_millis);
        this.mDozeAfterScreenOffConfig = resources.getBoolean(R.^attr-private.headerLayout);
        this.mMinimumScreenOffTimeoutConfig = resources.getInteger(R.integer.config_demo_pointing_aligned_duration_millis);
        this.mMaximumScreenDimDurationConfig = resources.getInteger(R.integer.config_demo_pointing_not_aligned_duration_millis);
        this.mMaximumScreenDimRatioConfig = resources.getFraction(R.fraction.config_dimBehindFadeDuration, 1, 1);
    }

    private void updateSettingsLocked() {
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mDreamsEnabledSetting = Settings.Secure.getIntForUser(resolver, "screensaver_enabled", this.mDreamsEnabledByDefaultConfig ? 1 : 0, -2) != 0;
        this.mDreamsActivateOnSleepSetting = Settings.Secure.getIntForUser(resolver, "screensaver_activate_on_sleep", this.mDreamsActivatedOnSleepByDefaultConfig ? 1 : 0, -2) != 0;
        this.mDreamsActivateOnDockSetting = Settings.Secure.getIntForUser(resolver, "screensaver_activate_on_dock", this.mDreamsActivatedOnDockByDefaultConfig ? 1 : 0, -2) != 0;
        this.mScreenOffTimeoutSetting = Settings.System.getIntForUser(resolver, "screen_off_timeout", 15000, -2);
        this.mScreenDimTimeoutSetting = Settings.System.getIntForUser(resolver, "screen_dim_timeout", 15000, -2);
        this.mSleepTimeoutSetting = Settings.Secure.getIntForUser(resolver, "sleep_timeout", -1, -2);
        this.mStayOnWhilePluggedInSetting = Settings.Global.getInt(resolver, "stay_on_while_plugged_in", 1);
        this.mTheaterModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
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
        boolean lowPowerModeEnabled = Settings.Global.getInt(resolver, "low_power", 0) != 0;
        boolean autoLowPowerModeConfigured = Settings.Global.getInt(resolver, "low_power_trigger_level", 0) != 0;
        if (lowPowerModeEnabled != this.mLowPowerModeSetting || autoLowPowerModeConfigured != this.mAutoLowPowerModeConfigured) {
            this.mLowPowerModeSetting = lowPowerModeEnabled;
            this.mAutoLowPowerModeConfigured = autoLowPowerModeConfigured;
            updateLowPowerModeLocked();
        }
        this.mDirty |= 32;
    }

    void updateLowPowerModeLocked() {
        if (this.mIsPowered && this.mLowPowerModeSetting) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "low_power", 0);
            this.mLowPowerModeSetting = false;
        }
        boolean autoLowPowerModeEnabled = !this.mIsPowered && this.mAutoLowPowerModeConfigured && !this.mAutoLowPowerModeSnoozing && this.mBatteryLevelLow;
        final boolean lowPowerModeEnabled = this.mLowPowerModeSetting || autoLowPowerModeEnabled;
        if (this.mLowPowerModeEnabled != lowPowerModeEnabled) {
            this.mLowPowerModeEnabled = lowPowerModeEnabled;
            powerHintInternal(5, lowPowerModeEnabled ? 1 : 0);
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<PowerManagerInternal.LowPowerModeListener> listeners;
                    PowerManagerService.this.mContext.sendBroadcast(new Intent("android.os.action.POWER_SAVE_MODE_CHANGING").putExtra("mode", PowerManagerService.this.mLowPowerModeEnabled).addFlags(1073741824));
                    synchronized (PowerManagerService.this.mLock) {
                        listeners = new ArrayList<>(PowerManagerService.this.mLowPowerModeListeners);
                    }
                    for (int i = 0; i < listeners.size(); i++) {
                        listeners.get(i).onLowPowerModeChanged(lowPowerModeEnabled);
                    }
                    Intent intent = new Intent("android.os.action.POWER_SAVE_MODE_CHANGED");
                    intent.addFlags(1073741824);
                    PowerManagerService.this.mContext.sendBroadcast(intent);
                }
            });
        }
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
                    notifyAcquire = true;
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
        if ((wakeLock.mFlags & 268435456) != 0 && isScreenLock(wakeLock)) {
            wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), uid);
        }
    }

    private void releaseWakeLockInternal(IBinder lock, int flags) {
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index >= 0) {
                WakeLock wakeLock = this.mWakeLocks.get(index);
                if ((flags & 1) != 0) {
                    this.mRequestWaitForNegativeProximity = true;
                }
                wakeLock.mLock.unlinkToDeath(wakeLock, 0);
                removeWakeLockLocked(wakeLock, index);
            }
        }
    }

    private void handleWakeLockDeath(WakeLock wakeLock) {
        synchronized (this.mLock) {
            int index = this.mWakeLocks.indexOf(wakeLock);
            if (index >= 0) {
                removeWakeLockLocked(wakeLock, index);
            }
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
        if ((wakeLock.mFlags & 536870912) != 0 && isScreenLock(wakeLock)) {
            userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 1, wakeLock.mOwnerUid);
        }
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
        if (this.mSystemReady) {
            wakeLock.mNotifiedAcquired = true;
            this.mNotifier.onWakeLockAcquired(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
        }
    }

    private void notifyWakeLockChangingLocked(WakeLock wakeLock, int flags, String tag, String packageName, int uid, int pid, WorkSource ws, String historyTag) {
        if (this.mSystemReady && wakeLock.mNotifiedAcquired) {
            this.mNotifier.onWakeLockChanging(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag, flags, tag, packageName, uid, pid, ws, historyTag);
        }
    }

    private void notifyWakeLockReleasedLocked(WakeLock wakeLock) {
        if (this.mSystemReady && wakeLock.mNotifiedAcquired) {
            wakeLock.mNotifiedAcquired = false;
            this.mNotifier.onWakeLockReleased(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
        }
    }

    private boolean isWakeLockLevelSupportedInternal(int level) {
        synchronized (this.mLock) {
            switch (level) {
                case 1:
                case 6:
                case 10:
                case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                case 64:
                    return true;
                case 32:
                    return this.mSystemReady && this.mDisplayManagerInternal.isProximitySensorAvailable();
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
        Trace.traceBegin(131072L, "userActivity");
        try {
            if (eventTime > this.mLastInteractivePowerHintTime) {
                powerHintInternal(2, 0);
                this.mLastInteractivePowerHintTime = eventTime;
            }
            this.mNotifier.onUserActivity(event, uid);
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
                return true;
            }
            return false;
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    private void wakeUpInternal(long eventTime, int uid) {
        synchronized (this.mLock) {
            if (wakeUpNoUpdateLocked(eventTime, uid)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean wakeUpNoUpdateLocked(long eventTime, int uid) {
        if (eventTime < this.mLastSleepTime || this.mWakefulness == 1 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072L, "wakeUp");
        try {
            switch (this.mWakefulness) {
                case 0:
                    Slog.i(TAG, "Waking up from sleep (uid " + uid + ")...");
                    break;
                case 2:
                    Slog.i(TAG, "Waking up from dream (uid " + uid + ")...");
                    break;
                case 3:
                    Slog.i(TAG, "Waking up from dozing (uid " + uid + ")...");
                    break;
            }
            this.mLastWakeTime = eventTime;
            setWakefulnessLocked(1, 0);
            userActivityNoUpdateLocked(eventTime, 0, 0, uid);
            return true;
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    private void goToSleepInternal(long eventTime, int reason, int flags, int uid) {
        synchronized (this.mLock) {
            if (goToSleepNoUpdateLocked(eventTime, reason, flags, uid)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean goToSleepNoUpdateLocked(long eventTime, int reason, int flags, int uid) {
        if (eventTime < this.mLastWakeTime || this.mWakefulness == 0 || this.mWakefulness == 3 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072L, "goToSleep");
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
                default:
                    Slog.i(TAG, "Going to sleep by application request (uid " + uid + ")...");
                    reason = 0;
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
            if ((flags & 1) != 0) {
                reallyGoToSleepNoUpdateLocked(eventTime, uid);
            }
            Trace.traceEnd(131072L);
            return true;
        } catch (Throwable th) {
            Trace.traceEnd(131072L);
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
        Trace.traceBegin(131072L, "nap");
        try {
            Slog.i(TAG, "Nap time (uid " + uid + ")...");
            this.mSandmanSummoned = true;
            setWakefulnessLocked(2, 0);
            return true;
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    private boolean reallyGoToSleepNoUpdateLocked(long eventTime, int uid) {
        if (eventTime < this.mLastWakeTime || this.mWakefulness == 0 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072L, "reallyGoToSleep");
        try {
            Slog.i(TAG, "Sleeping (uid " + uid + ")...");
            setWakefulnessLocked(0, 2);
            Trace.traceEnd(131072L);
            return true;
        } catch (Throwable th) {
            Trace.traceEnd(131072L);
            throw th;
        }
    }

    private void setWakefulnessLocked(int wakefulness, int reason) {
        if (this.mWakefulness != wakefulness) {
            finishWakefulnessChangeLocked();
            this.mWakefulness = wakefulness;
            this.mWakefulnessChanging = true;
            this.mDirty |= 2;
            this.mNotifier.onWakefulnessChangeStarted(wakefulness, reason);
        }
    }

    private void finishWakefulnessChangeLocked() {
        if (this.mWakefulnessChanging) {
            this.mNotifier.onWakefulnessChangeFinished(this.mWakefulness);
            this.mWakefulnessChanging = false;
        }
    }

    private void updatePowerStateLocked() {
        int dirtyPhase1;
        if (this.mSystemReady && this.mDirty != 0) {
            if (!Thread.holdsLock(this.mLock)) {
                Slog.wtf(TAG, "Power manager lock was not held when calling updatePowerStateLocked");
            }
            Trace.traceBegin(131072L, "updatePowerState");
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
                if (this.mDisplayReady) {
                    finishWakefulnessChangeLocked();
                }
                updateSuspendBlockerLocked();
            } finally {
                Trace.traceEnd(131072L);
            }
        }
    }

    private void updateIsPoweredLocked(int dirty) {
        if ((dirty & 256) != 0) {
            boolean wasPowered = this.mIsPowered;
            int oldPlugType = this.mPlugType;
            int oldPlugType_all = this.mPlugType_all;
            boolean oldLevelLow = this.mBatteryLevelLow;
            this.mIsPowered = this.mBatteryManagerInternal.isPowered(7);
            this.mPlugType = this.mBatteryManagerInternal.getPlugType();
            this.mPlugType_all = this.mBatteryManagerInternal.getPlugType_all();
            this.mBatteryLevel = this.mBatteryManagerInternal.getBatteryLevel();
            this.mBatteryLevelLow = this.mBatteryManagerInternal.getBatteryLevelLow();
            if (wasPowered != this.mIsPowered || oldPlugType_all != this.mPlugType_all) {
                this.mDirty |= 64;
                boolean dockedOnWirelessCharger = this.mWirelessChargerDetector.update(this.mIsPowered, this.mPlugType, this.mBatteryLevel);
                long now = SystemClock.uptimeMillis();
                if (shouldWakeUpWhenPluggedOrUnpluggedLocked(wasPowered, oldPlugType, dockedOnWirelessCharger)) {
                    wakeUpNoUpdateLocked(now, 1000);
                }
                userActivityNoUpdateLocked(now, 0, 0, 1000);
                if (dockedOnWirelessCharger) {
                    this.mNotifier.onWirelessChargingStarted();
                }
            }
            if (wasPowered != this.mIsPowered || oldLevelLow != this.mBatteryLevelLow) {
                if (oldLevelLow != this.mBatteryLevelLow && !this.mBatteryLevelLow) {
                    this.mAutoLowPowerModeSnoozing = false;
                }
                updateLowPowerModeLocked();
            }
        }
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
        if ((dirty & 288) != 0) {
            boolean wasStayOn = this.mStayOn;
            if (this.mStayOnWhilePluggedInSetting != 0 && !isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
                this.mStayOn = this.mBatteryManagerInternal.isPowered(this.mStayOnWhilePluggedInSetting);
            } else {
                this.mStayOn = false;
            }
            if (this.mStayOn != wasStayOn) {
                this.mDirty |= 128;
            }
        }
    }

    private void updateWakeLockSummaryLocked(int dirty) {
        if ((dirty & 3) != 0) {
            this.mWakeLockSummary = 0;
            int numWakeLocks = this.mWakeLocks.size();
            for (int i = 0; i < numWakeLocks; i++) {
                WakeLock wakeLock = this.mWakeLocks.get(i);
                switch (wakeLock.mFlags & 65535) {
                    case 1:
                        this.mWakeLockSummary |= 1;
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
                }
            }
            if (this.mWakefulness != 3) {
                this.mWakeLockSummary &= -65;
            }
            if (this.mWakefulness == 0 || (this.mWakeLockSummary & 64) != 0) {
                this.mWakeLockSummary &= -15;
                if (this.mWakefulness == 0) {
                    this.mWakeLockSummary &= -17;
                }
            }
            if ((this.mWakeLockSummary & 6) != 0) {
                if (this.mWakefulness == 1) {
                    this.mWakeLockSummary |= 33;
                } else if (this.mWakefulness == 2) {
                    this.mWakeLockSummary |= 1;
                }
            }
        }
    }

    private void updateUserActivitySummaryLocked(long now, int dirty) {
        if ((dirty & 39) != 0) {
            this.mHandler.removeMessages(1);
            long nextTimeout = 0;
            if (this.mWakefulness == 1 || this.mWakefulness == 2 || this.mWakefulness == 3) {
                int sleepTimeout = getSleepTimeoutLocked();
                int screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout);
                int screenDimDuration = getScreenDimDurationLocked(screenOffTimeout);
                this.mUserActivitySummary = 0;
                if (this.mLastUserActivityTime >= this.mLastWakeTime) {
                    nextTimeout = (this.mLastUserActivityTime + ((long) screenOffTimeout)) - ((long) screenDimDuration);
                    if (now < nextTimeout) {
                        this.mUserActivitySummary = 1;
                    } else {
                        nextTimeout = this.mLastUserActivityTime + ((long) screenOffTimeout);
                        if (now < nextTimeout) {
                            this.mUserActivitySummary = 2;
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
                if (this.mUserActivitySummary != 0 && nextTimeout >= 0) {
                    Message msg = this.mHandler.obtainMessage(1);
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageAtTime(msg, nextTimeout);
                    return;
                }
                return;
            }
            this.mUserActivitySummary = 0;
        }
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
        return Math.max(timeout, this.mMinimumScreenOffTimeoutConfig);
    }

    private int getScreenDimDurationLocked(int screenOffTimeout) {
        return Math.max(screenOffTimeout - this.mScreenDimTimeoutSetting, 0);
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
        return this.mDreamsActivateOnSleepSetting || (this.mDreamsActivateOnDockSetting && this.mDockState != 0);
    }

    private boolean isItBedTimeYetLocked() {
        return this.mBootCompleted && !isBeingKeptAwakeLocked();
    }

    private boolean isBeingKeptAwakeLocked() {
        return this.mStayOn || this.mProximityPositive || (this.mWakeLockSummary & 32) != 0 || (this.mUserActivitySummary & 3) != 0 || this.mScreenBrightnessBoostInProgress;
    }

    private void updateDreamLocked(int dirty, boolean displayBecameReady) {
        if (((dirty & 1015) != 0 || displayBecameReady) && this.mDisplayReady) {
            scheduleSandmanLocked();
        }
    }

    private void scheduleSandmanLocked() {
        if (!this.mSandmanScheduled) {
            this.mSandmanScheduled = true;
            Message msg = this.mHandler.obtainMessage(2);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    private void handleSandman() {
        int wakefulness;
        boolean startDreaming;
        boolean isDreaming;
        synchronized (this.mLock) {
            this.mSandmanScheduled = false;
            wakefulness = this.mWakefulness;
            if (this.mSandmanSummoned && this.mDisplayReady) {
                startDreaming = canDreamLocked() || canDozeLocked();
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
            isDreaming = this.mDreamManager.isDreaming();
        } else {
            isDreaming = false;
        }
        synchronized (this.mLock) {
            if (startDreaming && isDreaming) {
                this.mBatteryLevelWhenDreamStarted = this.mBatteryLevel;
                if (wakefulness == 3) {
                    Slog.i(TAG, "Dozing...");
                } else {
                    Slog.i(TAG, "Dreaming...");
                }
            }
            if (!this.mSandmanSummoned && this.mWakefulness == wakefulness) {
                if (wakefulness == 2) {
                    if (isDreaming && canDreamLocked()) {
                        if (this.mDreamsBatteryLevelDrainCutoffConfig >= 0 && this.mBatteryLevel < this.mBatteryLevelWhenDreamStarted - this.mDreamsBatteryLevelDrainCutoffConfig && !isBeingKeptAwakeLocked()) {
                            Slog.i(TAG, "Stopping dream because the battery appears to be draining faster than it is charging.  Battery level when dream started: " + this.mBatteryLevelWhenDreamStarted + "%.  Battery level now: " + this.mBatteryLevel + "%.");
                        } else {
                            return;
                        }
                    }
                    if (isItBedTimeYetLocked()) {
                        goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 2, 0, 1000);
                        updatePowerStateLocked();
                    } else {
                        wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), 1000);
                        updatePowerStateLocked();
                    }
                } else if (wakefulness == 3) {
                    if (!isDreaming) {
                        reallyGoToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 1000);
                        updatePowerStateLocked();
                    } else {
                        return;
                    }
                }
                if (isDreaming) {
                    this.mDreamManager.stopDream(false);
                }
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
            if (!this.mIsPowered && this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig >= 0 && this.mBatteryLevel < this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig) {
                return false;
            }
            if (this.mIsPowered && this.mDreamsBatteryLevelMinimumWhenPoweredConfig >= 0 && this.mBatteryLevel < this.mDreamsBatteryLevelMinimumWhenPoweredConfig) {
                return false;
            }
        }
        return true;
    }

    private boolean canDozeLocked() {
        return this.mWakefulness == 3;
    }

    private boolean updateDisplayPowerStateLocked(int dirty) {
        boolean oldDisplayReady = this.mDisplayReady;
        if ((dirty & 2111) != 0) {
            this.mDisplayPowerRequest.policy = getDesiredScreenPolicyLocked();
            int screenBrightness = this.mScreenBrightnessSettingDefault;
            float screenAutoBrightnessAdjustment = 0.0f;
            boolean autoBrightness = this.mScreenBrightnessModeSetting == 1;
            if (isValidBrightness(this.mScreenBrightnessOverrideFromWindowManager)) {
                screenBrightness = this.mScreenBrightnessOverrideFromWindowManager;
                autoBrightness = false;
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
            int screenBrightness2 = Math.max(Math.min(screenBrightness, this.mScreenBrightnessSettingMaximum), this.mScreenBrightnessSettingMinimum);
            float screenAutoBrightnessAdjustment2 = Math.max(Math.min(screenAutoBrightnessAdjustment, 1.0f), -1.0f);
            this.mDisplayPowerRequest.screenBrightness = screenBrightness2;
            this.mDisplayPowerRequest.screenAutoBrightnessAdjustment = screenAutoBrightnessAdjustment2;
            this.mDisplayPowerRequest.useAutoBrightness = autoBrightness;
            this.mDisplayPowerRequest.useProximitySensor = shouldUseProximitySensorLocked();
            this.mDisplayPowerRequest.lowPowerMode = this.mLowPowerModeEnabled;
            this.mDisplayPowerRequest.boostScreenBrightness = this.mScreenBrightnessBoostInProgress;
            if (this.mDisplayPowerRequest.policy == 1) {
                this.mDisplayPowerRequest.dozeScreenState = this.mDozeScreenStateOverrideFromDreamManager;
                this.mDisplayPowerRequest.dozeScreenBrightness = this.mDozeScreenBrightnessOverrideFromDreamManager;
            } else {
                this.mDisplayPowerRequest.dozeScreenState = 0;
                this.mDisplayPowerRequest.dozeScreenBrightness = -1;
            }
            this.mDisplayReady = this.mDisplayManagerInternal.requestPowerState(this.mDisplayPowerRequest, this.mRequestWaitForNegativeProximity);
            this.mRequestWaitForNegativeProximity = false;
        }
        return this.mDisplayReady && !oldDisplayReady;
    }

    private void updateScreenBrightnessBoostLocked(int dirty) {
        if ((dirty & 2048) != 0 && this.mScreenBrightnessBoostInProgress) {
            long now = SystemClock.uptimeMillis();
            this.mHandler.removeMessages(3);
            if (this.mLastScreenBrightnessBoostTime > this.mLastSleepTime) {
                long boostTimeout = this.mLastScreenBrightnessBoostTime + 5000;
                if (boostTimeout > now) {
                    Message msg = this.mHandler.obtainMessage(3);
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageAtTime(msg, boostTimeout);
                    return;
                }
            }
            this.mScreenBrightnessBoostInProgress = false;
            userActivityNoUpdateLocked(now, 0, 0, 1000);
        }
    }

    private static boolean isValidBrightness(int value) {
        return value >= 0 && value <= 255;
    }

    private static boolean isValidAutoBrightnessAdjustment(float value) {
        return value >= -1.0f && value <= 1.0f;
    }

    private int getDesiredScreenPolicyLocked() {
        if (this.mWakefulness == 0) {
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
        return ((this.mWakeLockSummary & 2) == 0 && (this.mUserActivitySummary & 1) == 0 && this.mBootCompleted && !this.mScreenBrightnessBoostInProgress) ? 2 : 3;
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
        if (autoSuspend && this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            setHalAutoSuspendModeLocked(true);
        }
    }

    private boolean needDisplaySuspendBlockerLocked() {
        if (this.mDisplayReady) {
            return (this.mDisplayPowerRequest.isBrightOrDim() && !(this.mDisplayPowerRequest.useProximitySensor && this.mProximityPositive && this.mSuspendWhenScreenOffDueToProximityConfig)) || this.mScreenBrightnessBoostInProgress;
        }
        return true;
    }

    private void setHalAutoSuspendModeLocked(boolean enable) {
        if (enable != this.mHalAutoSuspendModeEnabled) {
            this.mHalAutoSuspendModeEnabled = enable;
            Trace.traceBegin(131072L, "setHalAutoSuspend(" + enable + ")");
            try {
                nativeSetAutoSuspend(enable);
            } finally {
                Trace.traceEnd(131072L);
            }
        }
    }

    private void setHalInteractiveModeLocked(boolean enable) {
        if (enable != this.mHalInteractiveModeEnabled) {
            this.mHalInteractiveModeEnabled = enable;
            Trace.traceBegin(131072L, "setHalInteractive(" + enable + ")");
            try {
                nativeSetInteractive(enable);
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

    private boolean isLowPowerModeInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mLowPowerModeEnabled;
        }
        return z;
    }

    private boolean setLowPowerModeInternal(boolean mode) {
        synchronized (this.mLock) {
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

    private void handleBatteryStateChangedLocked() {
        this.mDirty |= 256;
        updatePowerStateLocked();
    }

    private void shutdownOrRebootInternal(final boolean shutdown, final boolean confirm, final String reason, boolean wait) {
        if (this.mHandler == null || !this.mSystemReady) {
            throw new IllegalStateException("Too early to call shutdown() or reboot()");
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (shutdown) {
                        ShutdownThread.shutdown(PowerManagerService.this.mContext, confirm);
                    } else {
                        ShutdownThread.reboot(PowerManagerService.this.mContext, reason, confirm);
                    }
                }
            }
        };
        Message msg = Message.obtain(this.mHandler, runnable);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
        if (wait) {
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

    private void setStayOnSettingInternal(int val) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", val);
    }

    private void setMaximumScreenOffTimeoutFromDeviceAdminInternal(int timeMs) {
        synchronized (this.mLock) {
            this.mMaximumScreenOffTimeoutFromDeviceAdmin = timeMs;
            this.mDirty |= 32;
            updatePowerStateLocked();
        }
    }

    private boolean isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked() {
        return this.mMaximumScreenOffTimeoutFromDeviceAdmin >= 0 && this.mMaximumScreenOffTimeoutFromDeviceAdmin < Integer.MAX_VALUE;
    }

    private void setAttentionLightInternal(boolean on, int color) {
        synchronized (this.mLock) {
            if (this.mSystemReady) {
                Light light = this.mAttentionLight;
                light.setFlashing(color, 2, on ? 3 : 0, 0);
            }
        }
    }

    private void boostScreenBrightnessInternal(long eventTime, int uid) {
        synchronized (this.mLock) {
            if (this.mSystemReady && this.mWakefulness != 0 && eventTime >= this.mLastScreenBrightnessBoostTime) {
                Slog.i(TAG, "Brightness boost activated (uid " + uid + ")...");
                this.mLastScreenBrightnessBoostTime = eventTime;
                this.mScreenBrightnessBoostInProgress = true;
                this.mDirty |= 2048;
                userActivityNoUpdateLocked(eventTime, 0, 0, uid);
                updatePowerStateLocked();
            }
        }
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
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setUserActivityTimeoutOverrideFromWindowManagerInternal(long timeoutMillis) {
        synchronized (this.mLock) {
            if (this.mUserActivityTimeoutOverrideFromWindowManager != timeoutMillis) {
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

    public static void lowLevelShutdown() {
        SystemProperties.set("sys.powerctl", "shutdown");
    }

    public static void lowLevelReboot(String reason) {
        long duration;
        if (reason == null) {
            reason = "";
        }
        if (reason.equals("recovery")) {
            SystemProperties.set("ctl.start", "pre-recovery");
            duration = 300000;
        } else {
            SystemProperties.set("sys.powerctl", "reboot," + reason);
            duration = 20000;
        }
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
            pw.println("  mTemporaryScreenBrightnessSettingOverride=" + this.mTemporaryScreenBrightnessSettingOverride);
            pw.println("  mTemporaryScreenAutoBrightnessAdjustmentSettingOverride=" + this.mTemporaryScreenAutoBrightnessAdjustmentSettingOverride);
            pw.println("  mDozeScreenStateOverrideFromDreamManager=" + this.mDozeScreenStateOverrideFromDreamManager);
            pw.println("  mDozeScreenBrightnessOverrideFromDreamManager=" + this.mDozeScreenBrightnessOverrideFromDreamManager);
            pw.println("  mScreenBrightnessSettingMinimum=" + this.mScreenBrightnessSettingMinimum);
            pw.println("  mScreenBrightnessSettingMaximum=" + this.mScreenBrightnessSettingMaximum);
            pw.println("  mScreenBrightnessSettingDefault=" + this.mScreenBrightnessSettingDefault);
            int sleepTimeout = getSleepTimeoutLocked();
            int screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout);
            int screenDimDuration = getScreenDimDurationLocked(screenOffTimeout);
            pw.println();
            pw.println("Sleep timeout: " + sleepTimeout + " ms");
            pw.println("Screen off timeout: " + screenOffTimeout + " ms");
            pw.println("Screen dim duration: " + screenDimDuration + " ms");
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
                int dockState = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                if (PowerManagerService.this.mDockState != dockState) {
                    PowerManagerService.this.mDockState = dockState;
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
        public int mFlags;
        public String mHistoryTag;
        public final IBinder mLock;
        public boolean mNotifiedAcquired;
        public final int mOwnerPid;
        public final int mOwnerUid;
        public final String mPackageName;
        public String mTag;
        public WorkSource mWorkSource;

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
            return getLockLevelString() + " '" + this.mTag + "'" + getLockFlagsString() + " (uid=" + this.mOwnerUid + ", pid=" + this.mOwnerPid + ", ws=" + this.mWorkSource + ")";
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
    }

    private final class BinderService extends IPowerManager.Stub {
        private BinderService() {
        }

        public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uid) {
            if (uid < 0) {
                uid = Binder.getCallingUid();
            }
            acquireWakeLock(lock, flags, tag, packageName, new WorkSource(uid), null);
        }

        public void powerHint(int hintId, int data) {
            if (PowerManagerService.this.mSystemReady) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                PowerManagerService.this.powerHintInternal(hintId, data);
            }
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
            if (lock != null) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.releaseWakeLockInternal(lock, flags);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            throw new IllegalArgumentException("lock must not be null");
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
            if (lock != null) {
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
                    return;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            throw new IllegalArgumentException("lock must not be null");
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
            if (eventTime > SystemClock.uptimeMillis()) {
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

        public void wakeUp(long eventTime) {
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.wakeUpInternal(eventTime, uid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            throw new IllegalArgumentException("event time must not be in the future");
        }

        public void goToSleep(long eventTime, int reason, int flags) {
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.goToSleepInternal(eventTime, reason, flags, uid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            throw new IllegalArgumentException("event time must not be in the future");
        }

        public void nap(long eventTime) {
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.napInternal(eventTime, uid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            throw new IllegalArgumentException("event time must not be in the future");
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

        public void reboot(boolean confirm, String reason, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            if ("recovery".equals(reason)) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(false, confirm, reason, wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void shutdown(boolean confirm, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(true, confirm, null, wait);
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
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SETTINGS", null);
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
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.boostScreenBrightnessInternal(eventTime, uid);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            throw new IllegalArgumentException("event time must not be in the future");
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
    }

    private final class LocalService extends PowerManagerInternal {
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
    }
}
