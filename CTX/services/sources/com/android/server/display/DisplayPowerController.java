package com.android.server.display;

import android.R;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.Display;
import com.android.internal.app.IBatteryStats;
import com.android.server.LocalServices;
import com.android.server.am.BatteryStatsService;
import com.android.server.display.AutomaticBrightnessController;
import com.android.server.display.RampAnimator;
import com.android.server.policy.WindowManagerPolicy;
import java.io.PrintWriter;

final class DisplayPowerController implements AutomaticBrightnessController.Callbacks {
    static final boolean $assertionsDisabled = false;
    private static final int COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 400;
    private static final int COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 250;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT = false;
    private static final int MSG_CONFIGURE_BRIGHTNESS = 5;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 2;
    private static final int MSG_SCREEN_OFF_UNBLOCKED = 4;
    private static final int MSG_SCREEN_ON_UNBLOCKED = 3;
    private static final int MSG_SET_TEMPORARY_AUTO_BRIGHTNESS_ADJUSTMENT = 7;
    private static final int MSG_SET_TEMPORARY_BRIGHTNESS = 6;
    private static final int MSG_UPDATE_POWER_STATE = 1;
    private static final int MTK_COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 100;
    private static final boolean MTK_DEBUG = "eng".equals(Build.TYPE);
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_SENSOR_NEGATIVE_DEBOUNCE_DELAY = 250;
    private static final int PROXIMITY_SENSOR_POSITIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int RAMP_STATE_SKIP_AUTOBRIGHT = 2;
    private static final int RAMP_STATE_SKIP_INITIAL = 1;
    private static final int RAMP_STATE_SKIP_NONE = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_OFF = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_ON = 2;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_OFF = 3;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_ON = 1;
    private static final int SCREEN_DIM_MINIMUM_REDUCTION = 10;
    private static final String SCREEN_OFF_BLOCKED_TRACE_NAME = "Screen off blocked";
    private static final String SCREEN_ON_BLOCKED_TRACE_NAME = "Screen on blocked";
    private static final String TAG = "DisplayPowerController";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final boolean USE_COLOR_FADE_ON_ANIMATION = false;
    private final boolean mAllowAutoBrightnessWhileDozingConfig;
    private boolean mAppliedAutoBrightness;
    private boolean mAppliedBrightnessBoost;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private boolean mAppliedScreenBrightnessOverride;
    private boolean mAppliedTemporaryAutoBrightnessAdjustment;
    private boolean mAppliedTemporaryBrightness;
    private float mAutoBrightnessAdjustment;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    private final DisplayBlanker mBlanker;
    private boolean mBrightnessBucketsInDozeConfig;
    private BrightnessConfiguration mBrightnessConfiguration;
    private BrightnessMappingStrategy mBrightnessMapper;
    private final int mBrightnessRampRateFast;
    private final int mBrightnessRampRateSlow;
    private final BrightnessTracker mBrightnessTracker;
    private final DisplayManagerInternal.DisplayPowerCallbacks mCallbacks;
    private final boolean mColorFadeEnabled;
    private boolean mColorFadeFadesConfig;
    private ObjectAnimator mColorFadeOffAnimator;
    private ObjectAnimator mColorFadeOnAnimator;
    private final Context mContext;
    private int mCurrentScreenBrightnessSetting;
    private boolean mDisplayBlanksAfterDozeConfig;
    private boolean mDisplayReadyLocked;
    private boolean mDozing;
    private final DisplayControllerHandler mHandler;
    private int mInitialAutoBrightness;
    private int mLastUserSetScreenBrightness;
    private float mPendingAutoBrightnessAdjustment;
    private boolean mPendingRequestChangedLocked;
    private DisplayManagerInternal.DisplayPowerRequest mPendingRequestLocked;
    private int mPendingScreenBrightnessSetting;
    private boolean mPendingScreenOff;
    private ScreenOffUnblocker mPendingScreenOffUnblocker;
    private ScreenOnUnblocker mPendingScreenOnUnblocker;
    private boolean mPendingUpdatePowerStateLocked;
    private boolean mPendingWaitForNegativeProximityLocked;
    private DisplayManagerInternal.DisplayPowerRequest mPowerRequest;
    private DisplayPowerState mPowerState;
    private Sensor mProximitySensor;
    private boolean mProximitySensorEnabled;
    private float mProximityThreshold;
    private int mReportedScreenStateToPolicy;
    private final int mScreenBrightnessDefault;
    private final int mScreenBrightnessDimConfig;
    private final int mScreenBrightnessDozeConfig;
    private int mScreenBrightnessForVr;
    private final int mScreenBrightnessForVrDefault;
    private final int mScreenBrightnessForVrRangeMaximum;
    private final int mScreenBrightnessForVrRangeMinimum;
    private RampAnimator<DisplayPowerState> mScreenBrightnessRampAnimator;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private boolean mScreenOffBecauseOfProximity;
    private long mScreenOffBlockStartRealTime;
    private long mScreenOnBlockStartRealTime;
    private final SensorManager mSensorManager;
    private final SettingsObserver mSettingsObserver;
    private final boolean mSkipScreenOnBrightnessRamp;
    private float mTemporaryAutoBrightnessAdjustment;
    private int mTemporaryScreenBrightness;
    private boolean mUnfinishedBusiness;
    private boolean mUseSoftwareAutoBrightnessConfig;
    private boolean mWaitingForNegativeProximity;
    private final Object mLock = new Object();
    private int mProximity = -1;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private int mSkipRampState = 0;
    private final Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            DisplayPowerController.this.sendUpdatePowerState();
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }
    };
    private final RampAnimator.Listener mRampAnimatorListener = new RampAnimator.Listener() {
        @Override
        public void onAnimationEnd() {
            DisplayPowerController.this.sendUpdatePowerState();
        }
    };
    private final Runnable mCleanListener = new Runnable() {
        @Override
        public void run() {
            DisplayPowerController.this.sendUpdatePowerState();
        }
    };
    private final Runnable mOnStateChangedRunnable = new Runnable() {
        @Override
        public void run() {
            DisplayPowerController.this.mCallbacks.onStateChanged();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnProximityPositiveRunnable = new Runnable() {
        @Override
        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityPositive();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnProximityNegativeRunnable = new Runnable() {
        @Override
        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityNegative();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (DisplayPowerController.this.mProximitySensorEnabled) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                boolean z = false;
                float f = sensorEvent.values[0];
                if (f >= 0.0f && f < DisplayPowerController.this.mProximityThreshold) {
                    z = true;
                }
                DisplayPowerController.this.handleProximitySensorEvent(jUptimeMillis, z);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    private final IBatteryStats mBatteryStats = BatteryStatsService.getService();
    private final WindowManagerPolicy mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);

    public DisplayPowerController(Context context, DisplayManagerInternal.DisplayPowerCallbacks displayPowerCallbacks, Handler handler, SensorManager sensorManager, DisplayBlanker displayBlanker) {
        Resources resources;
        DisplayPowerController displayPowerController;
        int i;
        this.mHandler = new DisplayControllerHandler(handler.getLooper());
        this.mBrightnessTracker = new BrightnessTracker(context, null);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mCallbacks = displayPowerCallbacks;
        this.mSensorManager = sensorManager;
        this.mBlanker = displayBlanker;
        this.mContext = context;
        Resources resources2 = context.getResources();
        int iClampAbsoluteBrightness = clampAbsoluteBrightness(resources2.getInteger(R.integer.config_dozeWakeLockScreenDebounce));
        this.mScreenBrightnessDozeConfig = clampAbsoluteBrightness(resources2.getInteger(R.integer.config_doubleTapPowerGestureMode));
        this.mScreenBrightnessDimConfig = clampAbsoluteBrightness(resources2.getInteger(R.integer.config_doubleTapOnHomeBehavior));
        this.mScreenBrightnessRangeMinimum = Math.min(iClampAbsoluteBrightness, this.mScreenBrightnessDimConfig);
        this.mScreenBrightnessRangeMaximum = clampAbsoluteBrightness(resources2.getInteger(R.integer.config_downloadDataDirSize));
        this.mScreenBrightnessDefault = clampAbsoluteBrightness(resources2.getInteger(R.integer.config_downloadDataDirLowSpaceThreshold));
        this.mScreenBrightnessForVrRangeMinimum = clampAbsoluteBrightness(resources2.getInteger(R.integer.config_doublelineClockDefault));
        this.mScreenBrightnessForVrRangeMaximum = clampAbsoluteBrightness(resources2.getInteger(R.integer.config_doubleTapTimeoutMillis));
        this.mScreenBrightnessForVrDefault = clampAbsoluteBrightness(resources2.getInteger(R.integer.config_doubleTapPowerGestureMultiTargetDefaultAction));
        this.mUseSoftwareAutoBrightnessConfig = resources2.getBoolean(R.^attr-private.borderTop);
        this.mAllowAutoBrightnessWhileDozingConfig = resources2.getBoolean(R.^attr-private.actionModePopupWindowStyle);
        this.mBrightnessRampRateFast = resources2.getInteger(R.integer.config_app_exit_info_history_list_size);
        this.mBrightnessRampRateSlow = resources2.getInteger(R.integer.config_attentionMaximumExtension);
        this.mSkipScreenOnBrightnessRamp = resources2.getBoolean(R.^attr-private.needsDefaultBackgrounds);
        if (this.mUseSoftwareAutoBrightnessConfig) {
            float fraction = resources2.getFraction(R.fraction.config_maximumScreenDimRatio, 1, 1);
            HysteresisLevels hysteresisLevels = new HysteresisLevels(resources2.getIntArray(R.array.config_bg_current_drain_high_threshold_to_bg_restricted), resources2.getIntArray(R.array.config_bg_current_drain_high_threshold_to_restricted_bucket), resources2.getIntArray(R.array.config_bg_current_drain_threshold_to_bg_restricted));
            long integer = resources2.getInteger(R.integer.bugreport_state_unknown);
            long integer2 = resources2.getInteger(R.integer.button_pressed_animation_delay);
            boolean z = resources2.getBoolean(R.^attr-private.backgroundRight);
            int integer3 = resources2.getInteger(R.integer.config_datause_threshold_bytes);
            int integer4 = resources2.getInteger(R.integer.config_MaxConcurrentDownloadsAllowed);
            int integer5 = resources2.getInteger(R.integer.button_pressed_animation_duration);
            if (integer5 != -1) {
                if (integer5 > integer4) {
                    Slog.w(TAG, "Expected config_autoBrightnessInitialLightSensorRate (" + integer5 + ") to be less than or equal to config_autoBrightnessLightSensorRate (" + integer4 + ").");
                }
                i = integer5;
            } else {
                i = integer4;
            }
            this.mBrightnessMapper = BrightnessMappingStrategy.create(resources2);
            if (this.mBrightnessMapper != null) {
                resources = resources2;
                displayPowerController = this;
                displayPowerController.mAutomaticBrightnessController = new AutomaticBrightnessController(this, handler.getLooper(), sensorManager, this.mBrightnessMapper, integer3, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum, fraction, integer4, i, integer, integer2, z, hysteresisLevels);
            } else {
                resources = resources2;
                displayPowerController = this;
                displayPowerController.mUseSoftwareAutoBrightnessConfig = false;
            }
        } else {
            resources = resources2;
            displayPowerController = this;
        }
        displayPowerController.mColorFadeEnabled = !ActivityManager.isLowRamDeviceStatic();
        Resources resources3 = resources;
        displayPowerController.mColorFadeFadesConfig = resources3.getBoolean(R.^attr-private.backgroundLeft);
        displayPowerController.mDisplayBlanksAfterDozeConfig = resources3.getBoolean(R.^attr-private.dropdownListPreferredItemHeight);
        displayPowerController.mBrightnessBucketsInDozeConfig = resources3.getBoolean(R.^attr-private.emergencyInstaller);
        displayPowerController.mProximitySensor = displayPowerController.mSensorManager.getDefaultSensor(8);
        if (displayPowerController.mProximitySensor != null) {
            displayPowerController.mProximityThreshold = Math.min(displayPowerController.mProximitySensor.getMaximumRange(), 5.0f);
        }
        displayPowerController.mCurrentScreenBrightnessSetting = getScreenBrightnessSetting();
        displayPowerController.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
        displayPowerController.mAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
        displayPowerController.mTemporaryScreenBrightness = -1;
        displayPowerController.mPendingScreenBrightnessSetting = -1;
        displayPowerController.mTemporaryAutoBrightnessAdjustment = Float.NaN;
        displayPowerController.mPendingAutoBrightnessAdjustment = Float.NaN;
    }

    public boolean isProximitySensorAvailable() {
        return this.mProximitySensor != null;
    }

    public ParceledListSlice<BrightnessChangeEvent> getBrightnessEvents(int i, boolean z) {
        return this.mBrightnessTracker.getEvents(i, z);
    }

    public void onSwitchUser(int i) {
        handleSettingsChange(true);
        this.mBrightnessTracker.onSwitchUser(i);
    }

    public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats(int i) {
        return this.mBrightnessTracker.getAmbientBrightnessStats(i);
    }

    public void persistBrightnessTrackerState() {
        this.mBrightnessTracker.persistBrightnessTrackerState();
    }

    public boolean requestPowerState(DisplayManagerInternal.DisplayPowerRequest displayPowerRequest, boolean z) {
        boolean z2;
        boolean z3;
        synchronized (this.mLock) {
            if (z) {
                try {
                    if (!this.mPendingWaitForNegativeProximityLocked) {
                        this.mPendingWaitForNegativeProximityLocked = true;
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (this.mPendingRequestLocked == null) {
                this.mPendingRequestLocked = new DisplayManagerInternal.DisplayPowerRequest(displayPowerRequest);
            } else {
                if (!this.mPendingRequestLocked.equals(displayPowerRequest)) {
                    this.mPendingRequestLocked.copyFrom(displayPowerRequest);
                }
                if (z2) {
                    this.mDisplayReadyLocked = false;
                }
                if (z2 && !this.mPendingRequestChangedLocked) {
                    this.mPendingRequestChangedLocked = true;
                    sendUpdatePowerStateLocked();
                }
                if (MTK_DEBUG && z2) {
                    Slog.d(TAG, "requestPowerState: " + displayPowerRequest + ", waitForNegativeProximity=" + z + ", changed=" + z2);
                }
                z3 = this.mDisplayReadyLocked;
            }
            z2 = true;
            if (z2) {
            }
            if (z2) {
                this.mPendingRequestChangedLocked = true;
                sendUpdatePowerStateLocked();
            }
            if (MTK_DEBUG) {
                Slog.d(TAG, "requestPowerState: " + displayPowerRequest + ", waitForNegativeProximity=" + z + ", changed=" + z2);
            }
            z3 = this.mDisplayReadyLocked;
        }
        return z3;
    }

    public BrightnessConfiguration getDefaultBrightnessConfiguration() {
        return this.mAutomaticBrightnessController.getDefaultConfig();
    }

    private void sendUpdatePowerState() {
        synchronized (this.mLock) {
            sendUpdatePowerStateLocked();
        }
    }

    private void sendUpdatePowerStateLocked() {
        if (!this.mPendingUpdatePowerStateLocked) {
            this.mPendingUpdatePowerStateLocked = true;
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
        }
    }

    private void initialize() {
        this.mPowerState = new DisplayPowerState(this.mBlanker, this.mColorFadeEnabled ? new ColorFade(0) : null);
        if (this.mColorFadeEnabled) {
            this.mColorFadeOnAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, 0.0f, 1.0f);
            this.mColorFadeOnAnimator.setDuration(250L);
            this.mColorFadeOnAnimator.addListener(this.mAnimatorListener);
            this.mColorFadeOffAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, 1.0f, 0.0f);
            this.mColorFadeOffAnimator.setDuration(100L);
            this.mColorFadeOffAnimator.addListener(this.mAnimatorListener);
        }
        this.mScreenBrightnessRampAnimator = new RampAnimator<>(this.mPowerState, DisplayPowerState.SCREEN_BRIGHTNESS);
        this.mScreenBrightnessRampAnimator.setListener(this.mRampAnimatorListener);
        try {
            this.mBatteryStats.noteScreenState(this.mPowerState.getScreenState());
            this.mBatteryStats.noteScreenBrightness(this.mPowerState.getScreenBrightness());
        } catch (RemoteException e) {
        }
        float fConvertToNits = convertToNits(this.mPowerState.getScreenBrightness());
        if (fConvertToNits >= 0.0f) {
            this.mBrightnessTracker.start(fConvertToNits);
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness_for_vr"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_auto_brightness_adj"), false, this.mSettingsObserver, -1);
    }

    private void updatePowerState() {
        boolean z;
        boolean z2;
        int iMax;
        int i;
        float f;
        boolean z3;
        boolean z4;
        boolean z5;
        float automaticScreenBrightnessAdjustment;
        synchronized (this.mLock) {
            this.mPendingUpdatePowerStateLocked = false;
            if (this.mPendingRequestLocked == null) {
                return;
            }
            if (this.mPowerRequest == null) {
                this.mPowerRequest = new DisplayManagerInternal.DisplayPowerRequest(this.mPendingRequestLocked);
                this.mWaitingForNegativeProximity = this.mPendingWaitForNegativeProximityLocked;
                this.mPendingWaitForNegativeProximityLocked = false;
                this.mPendingRequestChangedLocked = false;
                z = true;
            } else {
                if (this.mPendingRequestChangedLocked) {
                    this.mPowerRequest.copyFrom(this.mPendingRequestLocked);
                    this.mWaitingForNegativeProximity |= this.mPendingWaitForNegativeProximityLocked;
                    this.mPendingWaitForNegativeProximityLocked = false;
                    this.mPendingRequestChangedLocked = false;
                    this.mDisplayReadyLocked = false;
                }
                z = false;
            }
            boolean z6 = !this.mDisplayReadyLocked;
            if (z) {
                initialize();
            }
            int i2 = this.mPowerRequest.policy;
            if (i2 != 4) {
                switch (i2) {
                    case 0:
                        i = 1;
                        z2 = true;
                        iMax = -1;
                        break;
                    case 1:
                        i = this.mPowerRequest.dozeScreenState != 0 ? this.mPowerRequest.dozeScreenState : 3;
                        if (!this.mAllowAutoBrightnessWhileDozingConfig) {
                            iMax = this.mPowerRequest.dozeScreenBrightness;
                            z2 = false;
                        } else {
                            z2 = false;
                            iMax = -1;
                        }
                        break;
                    default:
                        z2 = false;
                        iMax = -1;
                        i = 2;
                        break;
                }
            } else {
                z2 = false;
                iMax = -1;
                i = 5;
            }
            if (this.mProximitySensor != null) {
                if (this.mPowerRequest.useProximitySensor && i != 1) {
                    setProximitySensorEnabled(true);
                    if (!this.mScreenOffBecauseOfProximity && this.mProximity == 1) {
                        this.mScreenOffBecauseOfProximity = true;
                        sendOnProximityPositiveWithWakelock();
                    }
                } else if (this.mWaitingForNegativeProximity && this.mScreenOffBecauseOfProximity && this.mProximity == 1 && i != 1) {
                    setProximitySensorEnabled(true);
                } else {
                    setProximitySensorEnabled(false);
                    this.mWaitingForNegativeProximity = false;
                }
                if (this.mScreenOffBecauseOfProximity && this.mProximity != 1) {
                    this.mScreenOffBecauseOfProximity = false;
                    sendOnProximityNegativeWithWakelock();
                }
            } else {
                this.mWaitingForNegativeProximity = false;
            }
            if (this.mScreenOffBecauseOfProximity) {
                i = 1;
            }
            int screenState = this.mPowerState.getScreenState();
            animateScreenStateChange(i, z2);
            int screenState2 = this.mPowerState.getScreenState();
            if (screenState2 == 1) {
                iMax = 0;
            }
            if (screenState2 == 5) {
                iMax = this.mScreenBrightnessForVr;
            }
            if (iMax >= 0 || this.mPowerRequest.screenBrightnessOverride <= 0) {
                this.mAppliedScreenBrightnessOverride = false;
            } else {
                iMax = this.mPowerRequest.screenBrightnessOverride;
                this.mAppliedScreenBrightnessOverride = true;
            }
            boolean z7 = this.mPowerRequest.useAutoBrightness && (screenState2 == 2 || (this.mAllowAutoBrightnessWhileDozingConfig && Display.isDozeState(screenState2))) && iMax < 0 && this.mAutomaticBrightnessController != null;
            boolean zUpdateUserSetScreenBrightness = updateUserSetScreenBrightness();
            if (zUpdateUserSetScreenBrightness) {
                this.mTemporaryScreenBrightness = -1;
            }
            if (this.mTemporaryScreenBrightness > 0) {
                iMax = this.mTemporaryScreenBrightness;
                this.mAppliedTemporaryBrightness = true;
            } else {
                this.mAppliedTemporaryBrightness = false;
            }
            boolean zUpdateAutoBrightnessAdjustment = updateAutoBrightnessAdjustment();
            if (zUpdateAutoBrightnessAdjustment) {
                this.mTemporaryAutoBrightnessAdjustment = Float.NaN;
            }
            if (Float.isNaN(this.mTemporaryAutoBrightnessAdjustment)) {
                f = this.mAutoBrightnessAdjustment;
                this.mAppliedTemporaryAutoBrightnessAdjustment = false;
            } else {
                f = this.mTemporaryAutoBrightnessAdjustment;
                this.mAppliedTemporaryAutoBrightnessAdjustment = true;
            }
            float f2 = f;
            if (!this.mPowerRequest.boostScreenBrightness || iMax == 0) {
                this.mAppliedBrightnessBoost = false;
            } else {
                iMax = 255;
                this.mAppliedBrightnessBoost = true;
            }
            boolean z8 = iMax < 0 && (zUpdateAutoBrightnessAdjustment || zUpdateUserSetScreenBrightness);
            if (this.mAutomaticBrightnessController != null) {
                boolean zHasUserDataPoints = this.mAutomaticBrightnessController.hasUserDataPoints();
                z3 = z8;
                this.mAutomaticBrightnessController.configure(z7, this.mBrightnessConfiguration, this.mLastUserSetScreenBrightness / 255.0f, zUpdateUserSetScreenBrightness, f2, zUpdateAutoBrightnessAdjustment, this.mPowerRequest.policy);
                z4 = zHasUserDataPoints;
            } else {
                z3 = z8;
                z4 = false;
            }
            if (iMax < 0) {
                if (z7) {
                    iMax = this.mAutomaticBrightnessController.getAutomaticScreenBrightness();
                    automaticScreenBrightnessAdjustment = this.mAutomaticBrightnessController.getAutomaticScreenBrightnessAdjustment();
                } else {
                    automaticScreenBrightnessAdjustment = f2;
                }
                if (iMax >= 0) {
                    iMax = clampScreenBrightness(iMax);
                    z5 = this.mAppliedAutoBrightness && !zUpdateAutoBrightnessAdjustment;
                    putScreenBrightnessSetting(iMax);
                    this.mAppliedAutoBrightness = true;
                } else {
                    this.mAppliedAutoBrightness = false;
                    z5 = false;
                }
                if (f2 != automaticScreenBrightnessAdjustment) {
                    putAutoBrightnessAdjustmentSetting(automaticScreenBrightnessAdjustment);
                }
            } else {
                this.mAppliedAutoBrightness = false;
                z5 = false;
            }
            if (iMax < 0 && Display.isDozeState(screenState2)) {
                iMax = this.mScreenBrightnessDozeConfig;
            }
            if (iMax < 0) {
                iMax = clampScreenBrightness(this.mCurrentScreenBrightnessSetting);
            }
            if (this.mPowerRequest.policy == 2) {
                if (iMax > this.mScreenBrightnessRangeMinimum) {
                    iMax = Math.max(Math.min(iMax - 10, this.mScreenBrightnessDimConfig), this.mScreenBrightnessRangeMinimum);
                }
                if (!this.mAppliedDimming) {
                    z5 = false;
                }
                this.mAppliedDimming = true;
            } else if (this.mAppliedDimming) {
                this.mAppliedDimming = false;
                z5 = false;
            }
            if (this.mPowerRequest.lowPowerMode) {
                if (iMax > this.mScreenBrightnessRangeMinimum) {
                    iMax = Math.max((int) (iMax * Math.min(this.mPowerRequest.screenLowPowerBrightnessFactor, 1.0f)), this.mScreenBrightnessRangeMinimum);
                }
                if (!this.mAppliedLowPower) {
                    z5 = false;
                }
                this.mAppliedLowPower = true;
            } else if (this.mAppliedLowPower) {
                this.mAppliedLowPower = false;
                z5 = false;
            }
            if (!this.mPendingScreenOff) {
                if (this.mSkipScreenOnBrightnessRamp) {
                    if (screenState2 != 2) {
                        this.mSkipRampState = 0;
                    } else if (this.mSkipRampState == 0 && this.mDozing) {
                        this.mInitialAutoBrightness = iMax;
                        this.mSkipRampState = 1;
                    } else if (this.mSkipRampState == 1 && this.mUseSoftwareAutoBrightnessConfig && iMax != this.mInitialAutoBrightness) {
                        this.mSkipRampState = 2;
                    } else if (this.mSkipRampState == 2) {
                        this.mSkipRampState = 0;
                    }
                }
                boolean z9 = screenState2 == 5 || screenState == 5;
                boolean z10 = screenState2 == 2 && this.mSkipRampState != 0;
                boolean z11 = Display.isDozeState(screenState2) && this.mBrightnessBucketsInDozeConfig;
                boolean z12 = this.mColorFadeEnabled && this.mPowerState.getColorFadeLevel() == 1.0f;
                boolean z13 = this.mAppliedTemporaryBrightness || this.mAppliedTemporaryAutoBrightnessAdjustment;
                if (z10 || z11 || z9 || !z12 || z13) {
                    animateScreenBrightness(iMax, 0);
                } else {
                    animateScreenBrightness(iMax, z5 ? this.mBrightnessRampRateSlow : this.mBrightnessRampRateFast);
                }
                if (!z13) {
                    notifyBrightnessChanged(iMax, z3, z4);
                }
            }
            boolean z14 = this.mPendingScreenOnUnblocker == null && !(this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) && this.mPowerState.waitUntilClean(this.mCleanListener);
            boolean z15 = z14 && !this.mScreenBrightnessRampAnimator.isAnimating();
            if (z14 && screenState2 != 1 && this.mReportedScreenStateToPolicy == 1) {
                setReportedScreenState(2);
                this.mWindowManagerPolicy.screenTurnedOn();
            }
            if (!z15 && !this.mUnfinishedBusiness) {
                this.mCallbacks.acquireSuspendBlocker();
                this.mUnfinishedBusiness = true;
            }
            if (z14 && z6) {
                synchronized (this.mLock) {
                    if (!this.mPendingRequestChangedLocked) {
                        this.mDisplayReadyLocked = true;
                    }
                }
                sendOnStateChangedWithWakelock();
            }
            if (z15 && this.mUnfinishedBusiness) {
                this.mUnfinishedBusiness = false;
                this.mCallbacks.releaseSuspendBlocker();
            }
            this.mDozing = screenState2 != 2;
        }
    }

    @Override
    public void updateBrightness() {
        sendUpdatePowerState();
    }

    public void setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration) {
        this.mHandler.obtainMessage(5, brightnessConfiguration).sendToTarget();
    }

    public void setTemporaryBrightness(int i) {
        this.mHandler.obtainMessage(6, i, 0).sendToTarget();
    }

    public void setTemporaryAutoBrightnessAdjustment(float f) {
        this.mHandler.obtainMessage(7, Float.floatToIntBits(f), 0).sendToTarget();
    }

    private void blockScreenOn() {
        if (this.mPendingScreenOnUnblocker == null) {
            Trace.asyncTraceBegin(131072L, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOnUnblocker = new ScreenOnUnblocker();
            this.mScreenOnBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "Blocking screen on until initial contents have been drawn.");
        }
    }

    private void unblockScreenOn() {
        if (this.mPendingScreenOnUnblocker != null) {
            this.mPendingScreenOnUnblocker = null;
            Slog.i(TAG, "Unblocked screen on after " + (SystemClock.elapsedRealtime() - this.mScreenOnBlockStartRealTime) + " ms");
            Trace.asyncTraceEnd(131072L, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
        }
    }

    private void blockScreenOff() {
        if (this.mPendingScreenOffUnblocker == null) {
            Trace.asyncTraceBegin(131072L, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOffUnblocker = new ScreenOffUnblocker();
            this.mScreenOffBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "Blocking screen off");
        }
    }

    private void unblockScreenOff() {
        if (this.mPendingScreenOffUnblocker != null) {
            this.mPendingScreenOffUnblocker = null;
            Slog.i(TAG, "Unblocked screen off after " + (SystemClock.elapsedRealtime() - this.mScreenOffBlockStartRealTime) + " ms");
            Trace.asyncTraceEnd(131072L, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
        }
    }

    private boolean setScreenState(int i) {
        return setScreenState(i, false);
    }

    private boolean setScreenState(int i, boolean z) {
        boolean z2 = i == 1;
        if (this.mPowerState.getScreenState() != i) {
            if (z2 && !this.mScreenOffBecauseOfProximity) {
                if (this.mReportedScreenStateToPolicy == 2) {
                    setReportedScreenState(3);
                    blockScreenOff();
                    this.mWindowManagerPolicy.screenTurningOff(this.mPendingScreenOffUnblocker);
                    unblockScreenOff();
                } else if (this.mPendingScreenOffUnblocker != null) {
                    return false;
                }
            }
            if (!z) {
                Trace.traceCounter(131072L, "ScreenState", i);
                this.mPowerState.setScreenState(i);
                try {
                    this.mBatteryStats.noteScreenState(i);
                } catch (RemoteException e) {
                }
            }
        }
        if (z2 && this.mReportedScreenStateToPolicy != 0 && !this.mScreenOffBecauseOfProximity) {
            setReportedScreenState(0);
            unblockScreenOn();
            this.mWindowManagerPolicy.screenTurnedOff();
        } else if (!z2 && this.mReportedScreenStateToPolicy == 3) {
            unblockScreenOff();
            this.mWindowManagerPolicy.screenTurnedOff();
            setReportedScreenState(0);
        }
        if (!z2 && this.mReportedScreenStateToPolicy == 0) {
            setReportedScreenState(1);
            if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                blockScreenOn();
            } else {
                unblockScreenOn();
            }
            this.mWindowManagerPolicy.screenTurningOn(this.mPendingScreenOnUnblocker);
        }
        return this.mPendingScreenOnUnblocker == null;
    }

    private void setReportedScreenState(int i) {
        Trace.traceCounter(131072L, "ReportedScreenStateToPolicy", i);
        this.mReportedScreenStateToPolicy = i;
    }

    private int clampScreenBrightnessForVr(int i) {
        return MathUtils.constrain(i, this.mScreenBrightnessForVrRangeMinimum, this.mScreenBrightnessForVrRangeMaximum);
    }

    private int clampScreenBrightness(int i) {
        return MathUtils.constrain(i, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void animateScreenBrightness(int i, int i2) {
        if (this.mScreenBrightnessRampAnimator.animateTo(i, i2)) {
            Trace.traceCounter(131072L, "TargetScreenBrightness", i);
            try {
                this.mBatteryStats.noteScreenBrightness(i);
            } catch (RemoteException e) {
            }
        }
    }

    private void animateScreenStateChange(int i, boolean z) {
        if (this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) {
            if (i != 2) {
                return;
            } else {
                this.mPendingScreenOff = false;
            }
        }
        if (this.mDisplayBlanksAfterDozeConfig && Display.isDozeState(this.mPowerState.getScreenState()) && !Display.isDozeState(i)) {
            this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 0);
            if (this.mColorFadeOffAnimator != null) {
                this.mColorFadeOffAnimator.end();
            }
            setScreenState(1, i != 1);
        }
        if (this.mPendingScreenOff && i != 1) {
            setScreenState(1);
            this.mPendingScreenOff = false;
            this.mPowerState.dismissColorFadeResources();
        }
        if (i == 2) {
            if (setScreenState(2)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
                return;
            }
            return;
        }
        if (i == 5) {
            if ((!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() != 2) && setScreenState(5)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
                return;
            }
            return;
        }
        if (i == 3) {
            if ((!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() != 2) && setScreenState(3)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
                return;
            }
            return;
        }
        if (i == 4) {
            if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 4) {
                if (this.mPowerState.getScreenState() != 4) {
                    if (!setScreenState(3)) {
                        return;
                    } else {
                        setScreenState(4);
                    }
                }
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
                return;
            }
            return;
        }
        if (i == 6) {
            if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 6) {
                if (this.mPowerState.getScreenState() != 6) {
                    if (!setScreenState(2)) {
                        return;
                    } else {
                        setScreenState(6);
                    }
                }
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
                return;
            }
            return;
        }
        this.mPendingScreenOff = true;
        if (!this.mColorFadeEnabled) {
            this.mPowerState.setColorFadeLevel(0.0f);
        }
        if (this.mPowerState.getColorFadeLevel() == 0.0f) {
            setScreenState(1);
            this.mPendingScreenOff = false;
            this.mPowerState.dismissColorFadeResources();
        } else {
            if (z) {
                if (this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 1) && this.mPowerState.getScreenState() != 1) {
                    this.mColorFadeOffAnimator.start();
                    return;
                }
            }
            this.mColorFadeOffAnimator.end();
        }
    }

    private void setProximitySensorEnabled(boolean z) {
        if (z) {
            if (!this.mProximitySensorEnabled) {
                this.mProximitySensorEnabled = true;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mHandler);
                return;
            }
            return;
        }
        if (this.mProximitySensorEnabled) {
            this.mProximitySensorEnabled = false;
            this.mProximity = -1;
            this.mPendingProximity = -1;
            this.mHandler.removeMessages(2);
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            clearPendingProximityDebounceTime();
        }
    }

    private void handleProximitySensorEvent(long j, boolean z) {
        if (this.mProximitySensorEnabled) {
            if (this.mPendingProximity == 0 && !z) {
                return;
            }
            if (this.mPendingProximity == 1 && z) {
                return;
            }
            this.mHandler.removeMessages(2);
            if (z) {
                this.mPendingProximity = 1;
                setPendingProximityDebounceTime(j + 0);
            } else {
                this.mPendingProximity = 0;
                setPendingProximityDebounceTime(j + 250);
            }
            debounceProximitySensor();
        }
    }

    private void debounceProximitySensor() {
        if (this.mProximitySensorEnabled && this.mPendingProximity != -1 && this.mPendingProximityDebounceTime >= 0) {
            if (this.mPendingProximityDebounceTime <= SystemClock.uptimeMillis()) {
                this.mProximity = this.mPendingProximity;
                updatePowerState();
                clearPendingProximityDebounceTime();
            } else {
                this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(2), this.mPendingProximityDebounceTime);
            }
        }
    }

    private void clearPendingProximityDebounceTime() {
        if (this.mPendingProximityDebounceTime >= 0) {
            this.mPendingProximityDebounceTime = -1L;
            this.mCallbacks.releaseSuspendBlocker();
        }
    }

    private void setPendingProximityDebounceTime(long j) {
        if (this.mPendingProximityDebounceTime < 0) {
            this.mCallbacks.acquireSuspendBlocker();
        }
        this.mPendingProximityDebounceTime = j;
    }

    private void sendOnStateChangedWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnStateChangedRunnable);
    }

    private void handleSettingsChange(boolean z) {
        this.mPendingScreenBrightnessSetting = getScreenBrightnessSetting();
        if (z) {
            this.mCurrentScreenBrightnessSetting = this.mPendingScreenBrightnessSetting;
            if (this.mAutomaticBrightnessController != null) {
                this.mAutomaticBrightnessController.resetShortTermModel();
            }
        }
        this.mPendingAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
        this.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
        sendUpdatePowerState();
    }

    private float getAutoBrightnessAdjustmentSetting() {
        float floatForUser = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", 0.0f, -2);
        if (Float.isNaN(floatForUser)) {
            return 0.0f;
        }
        return clampAutoBrightnessAdjustment(floatForUser);
    }

    private int getScreenBrightnessSetting() {
        return clampAbsoluteBrightness(Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mScreenBrightnessDefault, -2));
    }

    private int getScreenBrightnessForVrSetting() {
        return clampScreenBrightnessForVr(Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_for_vr", this.mScreenBrightnessForVrDefault, -2));
    }

    private void putScreenBrightnessSetting(int i) {
        this.mCurrentScreenBrightnessSetting = i;
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", i, -2);
    }

    private void putAutoBrightnessAdjustmentSetting(float f) {
        this.mAutoBrightnessAdjustment = f;
        Settings.System.putFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", f, -2);
    }

    private boolean updateAutoBrightnessAdjustment() {
        if (Float.isNaN(this.mPendingAutoBrightnessAdjustment)) {
            return false;
        }
        if (this.mAutoBrightnessAdjustment == this.mPendingAutoBrightnessAdjustment) {
            this.mPendingAutoBrightnessAdjustment = Float.NaN;
            return false;
        }
        this.mAutoBrightnessAdjustment = this.mPendingAutoBrightnessAdjustment;
        this.mPendingAutoBrightnessAdjustment = Float.NaN;
        return true;
    }

    private boolean updateUserSetScreenBrightness() {
        if (this.mPendingScreenBrightnessSetting < 0) {
            return false;
        }
        if (this.mCurrentScreenBrightnessSetting == this.mPendingScreenBrightnessSetting) {
            this.mPendingScreenBrightnessSetting = -1;
            return false;
        }
        this.mCurrentScreenBrightnessSetting = this.mPendingScreenBrightnessSetting;
        this.mLastUserSetScreenBrightness = this.mPendingScreenBrightnessSetting;
        this.mPendingScreenBrightnessSetting = -1;
        return true;
    }

    private void notifyBrightnessChanged(int i, boolean z, boolean z2) {
        float f;
        float fConvertToNits = convertToNits(i);
        if (this.mPowerRequest.useAutoBrightness && fConvertToNits >= 0.0f && this.mAutomaticBrightnessController != null) {
            if (this.mPowerRequest.lowPowerMode) {
                f = this.mPowerRequest.screenLowPowerBrightnessFactor;
            } else {
                f = 1.0f;
            }
            this.mBrightnessTracker.notifyBrightnessChanged(fConvertToNits, z, f, z2, this.mAutomaticBrightnessController.isDefaultConfig());
        }
    }

    private float convertToNits(int i) {
        if (this.mBrightnessMapper != null) {
            return this.mBrightnessMapper.convertToNits(i);
        }
        return -1.0f;
    }

    private void sendOnProximityPositiveWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityPositiveRunnable);
    }

    private void sendOnProximityNegativeWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityNegativeRunnable);
    }

    public void dump(final PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println();
            printWriter.println("Display Power Controller Locked State:");
            printWriter.println("  mDisplayReadyLocked=" + this.mDisplayReadyLocked);
            printWriter.println("  mPendingRequestLocked=" + this.mPendingRequestLocked);
            printWriter.println("  mPendingRequestChangedLocked=" + this.mPendingRequestChangedLocked);
            printWriter.println("  mPendingWaitForNegativeProximityLocked=" + this.mPendingWaitForNegativeProximityLocked);
            printWriter.println("  mPendingUpdatePowerStateLocked=" + this.mPendingUpdatePowerStateLocked);
        }
        printWriter.println();
        printWriter.println("Display Power Controller Configuration:");
        printWriter.println("  mScreenBrightnessDozeConfig=" + this.mScreenBrightnessDozeConfig);
        printWriter.println("  mScreenBrightnessDimConfig=" + this.mScreenBrightnessDimConfig);
        printWriter.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        printWriter.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        printWriter.println("  mScreenBrightnessDefault=" + this.mScreenBrightnessDefault);
        printWriter.println("  mScreenBrightnessForVrRangeMinimum=" + this.mScreenBrightnessForVrRangeMinimum);
        printWriter.println("  mScreenBrightnessForVrRangeMaximum=" + this.mScreenBrightnessForVrRangeMaximum);
        printWriter.println("  mScreenBrightnessForVrDefault=" + this.mScreenBrightnessForVrDefault);
        printWriter.println("  mUseSoftwareAutoBrightnessConfig=" + this.mUseSoftwareAutoBrightnessConfig);
        printWriter.println("  mAllowAutoBrightnessWhileDozingConfig=" + this.mAllowAutoBrightnessWhileDozingConfig);
        printWriter.println("  mBrightnessRampRateFast=" + this.mBrightnessRampRateFast);
        printWriter.println("  mBrightnessRampRateSlow=" + this.mBrightnessRampRateSlow);
        printWriter.println("  mSkipScreenOnBrightnessRamp=" + this.mSkipScreenOnBrightnessRamp);
        printWriter.println("  mColorFadeFadesConfig=" + this.mColorFadeFadesConfig);
        printWriter.println("  mColorFadeEnabled=" + this.mColorFadeEnabled);
        printWriter.println("  mDisplayBlanksAfterDozeConfig=" + this.mDisplayBlanksAfterDozeConfig);
        printWriter.println("  mBrightnessBucketsInDozeConfig=" + this.mBrightnessBucketsInDozeConfig);
        this.mHandler.runWithScissors(new Runnable() {
            @Override
            public void run() {
                DisplayPowerController.this.dumpLocal(printWriter);
            }
        }, 1000L);
    }

    private void dumpLocal(PrintWriter printWriter) {
        printWriter.println();
        printWriter.println("Display Power Controller Thread State:");
        printWriter.println("  mPowerRequest=" + this.mPowerRequest);
        printWriter.println("  mUnfinishedBusiness=" + this.mUnfinishedBusiness);
        printWriter.println("  mWaitingForNegativeProximity=" + this.mWaitingForNegativeProximity);
        printWriter.println("  mProximitySensor=" + this.mProximitySensor);
        printWriter.println("  mProximitySensorEnabled=" + this.mProximitySensorEnabled);
        printWriter.println("  mProximityThreshold=" + this.mProximityThreshold);
        printWriter.println("  mProximity=" + proximityToString(this.mProximity));
        printWriter.println("  mPendingProximity=" + proximityToString(this.mPendingProximity));
        printWriter.println("  mPendingProximityDebounceTime=" + TimeUtils.formatUptime(this.mPendingProximityDebounceTime));
        printWriter.println("  mScreenOffBecauseOfProximity=" + this.mScreenOffBecauseOfProximity);
        printWriter.println("  mLastUserSetScreenBrightness=" + this.mLastUserSetScreenBrightness);
        printWriter.println("  mCurrentScreenBrightnessSetting=" + this.mCurrentScreenBrightnessSetting);
        printWriter.println("  mPendingScreenBrightnessSetting=" + this.mPendingScreenBrightnessSetting);
        printWriter.println("  mTemporaryScreenBrightness=" + this.mTemporaryScreenBrightness);
        printWriter.println("  mAutoBrightnessAdjustment=" + this.mAutoBrightnessAdjustment);
        printWriter.println("  mTemporaryAutoBrightnessAdjustment=" + this.mTemporaryAutoBrightnessAdjustment);
        printWriter.println("  mPendingAutoBrightnessAdjustment=" + this.mPendingAutoBrightnessAdjustment);
        printWriter.println("  mScreenBrightnessForVr=" + this.mScreenBrightnessForVr);
        printWriter.println("  mAppliedAutoBrightness=" + this.mAppliedAutoBrightness);
        printWriter.println("  mAppliedDimming=" + this.mAppliedDimming);
        printWriter.println("  mAppliedLowPower=" + this.mAppliedLowPower);
        printWriter.println("  mAppliedScreenBrightnessOverride=" + this.mAppliedScreenBrightnessOverride);
        printWriter.println("  mAppliedTemporaryBrightness=" + this.mAppliedTemporaryBrightness);
        printWriter.println("  mDozing=" + this.mDozing);
        printWriter.println("  mSkipRampState=" + skipRampStateToString(this.mSkipRampState));
        printWriter.println("  mInitialAutoBrightness=" + this.mInitialAutoBrightness);
        printWriter.println("  mScreenOnBlockStartRealTime=" + this.mScreenOnBlockStartRealTime);
        printWriter.println("  mScreenOffBlockStartRealTime=" + this.mScreenOffBlockStartRealTime);
        printWriter.println("  mPendingScreenOnUnblocker=" + this.mPendingScreenOnUnblocker);
        printWriter.println("  mPendingScreenOffUnblocker=" + this.mPendingScreenOffUnblocker);
        printWriter.println("  mPendingScreenOff=" + this.mPendingScreenOff);
        printWriter.println("  mReportedToPolicy=" + reportedToPolicyToString(this.mReportedScreenStateToPolicy));
        if (this.mScreenBrightnessRampAnimator != null) {
            printWriter.println("  mScreenBrightnessRampAnimator.isAnimating()=" + this.mScreenBrightnessRampAnimator.isAnimating());
        }
        if (this.mColorFadeOnAnimator != null) {
            printWriter.println("  mColorFadeOnAnimator.isStarted()=" + this.mColorFadeOnAnimator.isStarted());
        }
        if (this.mColorFadeOffAnimator != null) {
            printWriter.println("  mColorFadeOffAnimator.isStarted()=" + this.mColorFadeOffAnimator.isStarted());
        }
        if (this.mPowerState != null) {
            this.mPowerState.dump(printWriter);
        }
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.dump(printWriter);
        }
        if (this.mBrightnessTracker != null) {
            printWriter.println();
            this.mBrightnessTracker.dump(printWriter);
        }
    }

    private static String proximityToString(int i) {
        switch (i) {
            case -1:
                return "Unknown";
            case 0:
                return "Negative";
            case 1:
                return "Positive";
            default:
                return Integer.toString(i);
        }
    }

    private static String reportedToPolicyToString(int i) {
        switch (i) {
            case 0:
                return "REPORTED_TO_POLICY_SCREEN_OFF";
            case 1:
                return "REPORTED_TO_POLICY_SCREEN_TURNING_ON";
            case 2:
                return "REPORTED_TO_POLICY_SCREEN_ON";
            default:
                return Integer.toString(i);
        }
    }

    private static String skipRampStateToString(int i) {
        switch (i) {
            case 0:
                return "RAMP_STATE_SKIP_NONE";
            case 1:
                return "RAMP_STATE_SKIP_INITIAL";
            case 2:
                return "RAMP_STATE_SKIP_AUTOBRIGHT";
            default:
                return Integer.toString(i);
        }
    }

    private static int clampAbsoluteBrightness(int i) {
        return MathUtils.constrain(i, 0, 255);
    }

    private static float clampAutoBrightnessAdjustment(float f) {
        return MathUtils.constrain(f, -1.0f, 1.0f);
    }

    private final class DisplayControllerHandler extends Handler {
        public DisplayControllerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    DisplayPowerController.this.updatePowerState();
                    break;
                case 2:
                    DisplayPowerController.this.debounceProximitySensor();
                    break;
                case 3:
                    if (DisplayPowerController.this.mPendingScreenOnUnblocker == message.obj) {
                        DisplayPowerController.this.unblockScreenOn();
                        DisplayPowerController.this.updatePowerState();
                    }
                    break;
                case 4:
                    if (DisplayPowerController.this.mPendingScreenOffUnblocker == message.obj) {
                        DisplayPowerController.this.unblockScreenOff();
                        DisplayPowerController.this.updatePowerState();
                    }
                    break;
                case 5:
                    DisplayPowerController.this.mBrightnessConfiguration = (BrightnessConfiguration) message.obj;
                    DisplayPowerController.this.updatePowerState();
                    break;
                case 6:
                    DisplayPowerController.this.mTemporaryScreenBrightness = message.arg1;
                    DisplayPowerController.this.updatePowerState();
                    break;
                case 7:
                    DisplayPowerController.this.mTemporaryAutoBrightnessAdjustment = Float.intBitsToFloat(message.arg1);
                    DisplayPowerController.this.updatePowerState();
                    break;
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            DisplayPowerController.this.handleSettingsChange(false);
        }
    }

    private final class ScreenOnUnblocker implements WindowManagerPolicy.ScreenOnListener {
        private ScreenOnUnblocker() {
        }

        @Override
        public void onScreenOn() {
            DisplayPowerController.this.mHandler.sendMessage(DisplayPowerController.this.mHandler.obtainMessage(3, this));
        }
    }

    private final class ScreenOffUnblocker implements WindowManagerPolicy.ScreenOffListener {
        private ScreenOffUnblocker() {
        }

        @Override
        public void onScreenOff() {
            DisplayPowerController.this.mHandler.sendMessage(DisplayPowerController.this.mHandler.obtainMessage(4, this));
        }
    }
}
