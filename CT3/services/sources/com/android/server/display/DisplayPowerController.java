package com.android.server.display;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.dhcp.DhcpPacket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import android.util.TimeUtils;
import android.view.WindowManagerPolicy;
import com.android.internal.app.IBatteryStats;
import com.android.server.LocalServices;
import com.android.server.am.BatteryStatsService;
import com.android.server.display.AutomaticBrightnessController;
import com.android.server.display.RampAnimator;
import com.mediatek.internal.R;
import java.io.PrintWriter;

final class DisplayPowerController implements AutomaticBrightnessController.Callbacks {

    static final boolean f7assertionsDisabled;
    private static final int BRIGHTNESS_RAMP_RATE_SLOW = 40;
    private static final int COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 400;
    private static final int COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 250;
    private static boolean DEBUG = false;
    private static final boolean DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT = false;
    private static final long LOW_DIMMING_PROTECTION_DURATION = 5000;
    private static final boolean LOW_DIMMING_PROTECTION_SUPPORT;
    private static final int LOW_DIMMING_PROTECTION_THRESHOLD = 40;
    private static final int MSG_PROTECT_LOW_DIMMING = 100;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 2;
    private static final int MSG_SCREEN_ON_UNBLOCKED = 3;
    private static final int MSG_UPDATE_POWER_STATE = 1;
    private static final String MTK_AAL_LOW_DIMMING_PROTECTION_TRIGGERED = "com.mediatek.aal.low_dimming_protection_triggered";
    public static final boolean MTK_AAL_RUNTIME_TUNING_SUPPORT;
    public static final boolean MTK_AAL_SUPPORT;
    private static final String MTK_AAL_UPDATE_CONFIG_ACTION = "com.mediatek.aal.update_config";
    public static final boolean MTK_ULTRA_DIMMING_SUPPORT;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_SENSOR_NEGATIVE_DEBOUNCE_DELAY = 250;
    private static final int PROXIMITY_SENSOR_POSITIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int REPORTED_TO_POLICY_SCREEN_OFF = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_ON = 2;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_ON = 1;
    private static final int SCREEN_DIM_MINIMUM_REDUCTION = 10;
    private static final String SCREEN_ON_BLOCKED_TRACE_NAME = "Screen on blocked";
    private static final String TAG = "DisplayPowerController";
    private static final int TUNING_ALI2BLI_CURVE = 1;
    private static final int TUNING_ALI2BLI_CURVE_LENGTH = 0;
    private static final int TUNING_BLI_RAMP_RATE_BRIGHTEN = 2;
    private static final int TUNING_BLI_RAMP_RATE_DARKEN = 3;
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final boolean USE_COLOR_FADE_ON_ANIMATION = false;
    private final boolean mAllowAutoBrightnessWhileDozingConfig;
    private boolean mAppliedAutoBrightness;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    private final DisplayBlanker mBlanker;
    private final int mBrightnessRampRateFast;
    private final DisplayManagerInternal.DisplayPowerCallbacks mCallbacks;
    private boolean mColorFadeFadesConfig;
    private ObjectAnimator mColorFadeOffAnimator;
    private ObjectAnimator mColorFadeOnAnimator;
    private final Context mContext;
    private boolean mDisplayReadyLocked;
    private final DisplayControllerHandler mHandler;
    private boolean mPendingRequestChangedLocked;
    private DisplayManagerInternal.DisplayPowerRequest mPendingRequestLocked;
    private boolean mPendingScreenOff;
    private ScreenOnUnblocker mPendingScreenOnUnblocker;
    private boolean mPendingUpdatePowerStateLocked;
    private boolean mPendingWaitForNegativeProximityLocked;
    private DisplayManagerInternal.DisplayPowerRequest mPowerRequest;
    private DisplayPowerState mPowerState;
    private Sensor mProximitySensor;
    private boolean mProximitySensorEnabled;
    private float mProximityThreshold;
    private int mReportedScreenStateToPolicy;
    private final int mScreenBrightnessDarkConfig;
    private final int mScreenBrightnessDimConfig;
    private final int mScreenBrightnessDozeConfig;
    private RampAnimator<DisplayPowerState> mScreenBrightnessRampAnimator;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private boolean mScreenOffBecauseOfProximity;
    private long mScreenOnBlockStartRealTime;
    private final SensorManager mSensorManager;
    private int[] mTuningInitCurve;
    private boolean mUnfinishedBusiness;
    private boolean mUseSoftwareAutoBrightnessConfig;
    private boolean mWaitingForNegativeProximity;
    private int BRIGHTNESS_RAMP_RATE_BRIGHTEN = 40;
    private int BRIGHTNESS_RAMP_RATE_DARKEN = 40;
    private final Object mLock = new Object();
    private int mProximity = -1;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private final Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            DisplayPowerController.this.sendUpdatePowerState();
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };
    private final RampAnimator.Listener mRampAnimatorListener = new RampAnimator.Listener() {
        @Override
        public void onAnimationEnd() {
            DisplayPowerController.this.mTuningQuicklyApply = false;
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
        public void onSensorChanged(SensorEvent event) {
            if (!DisplayPowerController.this.mProximitySensorEnabled) {
                return;
            }
            long time = SystemClock.uptimeMillis();
            float distance = event.values[0];
            boolean positive = distance >= 0.0f && distance < DisplayPowerController.this.mProximityThreshold;
            DisplayPowerController.this.handleProximitySensorEvent(time, positive);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private int mTuningAli2BliSerial = 0;
    private int mTuningBliBrightenSerial = -1;
    private int mTuningBliDarkenSerial = -1;
    private boolean mTuningQuicklyApply = false;
    private boolean mLowDimmingProtectionEnabled = false;
    private int mLowDimmingProtectionTriggerBrightness = 0;
    private final IBatteryStats mBatteryStats = BatteryStatsService.getService();
    private final WindowManagerPolicy mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);

    private static native int nativeGetTuningInt(int i);

    private static native void nativeGetTuningIntArray(int i, int[] iArr);

    private static native int nativeGetTuningSerial(int i);

    private static native boolean nativeRuntimeTuningIsSupported();

    public static native void nativeSetDebouncedAmbientLight(int i);

    private static native int nativeSetTuningInt(int i, int i2);

    private static native int nativeSetTuningIntArray(int i, int[] iArr);

    static {
        f7assertionsDisabled = !DisplayPowerController.class.desiredAssertionStatus();
        DEBUG = true;
        MTK_AAL_SUPPORT = SystemProperties.get("ro.mtk_aal_support").equals("1");
        MTK_ULTRA_DIMMING_SUPPORT = PowerManager.MTK_ULTRA_DIMMING_SUPPORT;
        MTK_AAL_RUNTIME_TUNING_SUPPORT = nativeRuntimeTuningIsSupported();
        LOW_DIMMING_PROTECTION_SUPPORT = MTK_ULTRA_DIMMING_SUPPORT;
    }

    public DisplayPowerController(Context context, DisplayManagerInternal.DisplayPowerCallbacks callbacks, Handler handler, SensorManager sensorManager, DisplayBlanker blanker) {
        this.mTuningInitCurve = null;
        this.mHandler = new DisplayControllerHandler(handler.getLooper());
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mBlanker = blanker;
        this.mContext = context;
        Resources resources = context.getResources();
        boolean screenBrightnessVirtualValues = MTK_ULTRA_DIMMING_SUPPORT ? resources.getBoolean(R.bool.config_screenBrightnessVirtualValues) : false;
        int screenBrightnessSettingMinimum = clampAbsoluteBrightness(resources.getInteger(android.R.integer.config_defaultNightDisplayAutoMode), screenBrightnessVirtualValues);
        screenBrightnessSettingMinimum = MTK_ULTRA_DIMMING_SUPPORT ? 1 : screenBrightnessSettingMinimum;
        this.mScreenBrightnessDozeConfig = clampAbsoluteBrightness(resources.getInteger(android.R.integer.config_defaultNightMode), screenBrightnessVirtualValues);
        this.mScreenBrightnessDimConfig = clampAbsoluteBrightness(resources.getInteger(android.R.integer.config_defaultPictureInPictureGravity), screenBrightnessVirtualValues);
        this.mScreenBrightnessDarkConfig = clampAbsoluteBrightness(resources.getInteger(android.R.integer.config_defaultPreventScreenTimeoutForMillis), screenBrightnessVirtualValues);
        if (this.mScreenBrightnessDarkConfig > this.mScreenBrightnessDimConfig) {
            Slog.w(TAG, "Expected config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") to be less than or equal to config_screenBrightnessDim (" + this.mScreenBrightnessDimConfig + ").");
        }
        if (this.mScreenBrightnessDarkConfig > this.mScreenBrightnessDimConfig) {
            Slog.w(TAG, "Expected config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") to be less than or equal to config_screenBrightnessSettingMinimum (" + screenBrightnessSettingMinimum + ").");
        }
        int screenBrightnessRangeMinimum = Math.min(Math.min(screenBrightnessSettingMinimum, this.mScreenBrightnessDimConfig), this.mScreenBrightnessDarkConfig);
        this.mScreenBrightnessRangeMaximum = DhcpPacket.MAX_OPTION_LEN;
        this.mUseSoftwareAutoBrightnessConfig = resources.getBoolean(android.R.^attr-private.calendarViewMode);
        this.mAllowAutoBrightnessWhileDozingConfig = resources.getBoolean(android.R.^attr-private.errorColor);
        this.mBrightnessRampRateFast = resources.getInteger(android.R.integer.config_burnInProtectionMaxVerticalOffset);
        int lightSensorRate = resources.getInteger(android.R.integer.config_defaultNotificationVibrationIntensity);
        long brighteningLightDebounce = resources.getInteger(android.R.integer.config_defaultNotificationLedOff);
        long darkeningLightDebounce = resources.getInteger(android.R.integer.config_defaultNotificationLedOn);
        boolean autoBrightnessResetAmbientLuxAfterWarmUp = resources.getBoolean(android.R.^attr-private.errorMessageAboveBackground);
        int ambientLightHorizon = resources.getInteger(android.R.integer.config_defaultPeakRefreshRate);
        float autoBrightnessAdjustmentMaxGamma = resources.getFraction(android.R.fraction.config_biometricNotificationFrrThreshold, 1, 1);
        if (this.mUseSoftwareAutoBrightnessConfig) {
            int[] lux = resources.getIntArray(android.R.array.config_biometric_protected_package_names);
            int[] screenBrightness = resources.getIntArray(android.R.array.config_biometric_sensors);
            int lightSensorWarmUpTimeConfig = resources.getInteger(android.R.integer.config_defaultRefreshRate);
            float dozeScaleFactor = resources.getFraction(android.R.fraction.config_dimBehindFadeDuration, 1, 1);
            Spline screenAutoBrightnessSpline = createAutoBrightnessSpline(lux, screenBrightness, screenBrightnessVirtualValues);
            if (screenAutoBrightnessSpline == null) {
                Slog.e(TAG, "Error in config.xml.  config_autoBrightnessLcdBacklightValues (size " + screenBrightness.length + ") must be monotic and have exactly one more entry than config_autoBrightnessLevels (size " + lux.length + ") which must be strictly increasing.  Auto-brightness will be disabled.");
                this.mUseSoftwareAutoBrightnessConfig = false;
            } else {
                int bottom = clampAbsoluteBrightness(screenBrightness[0], screenBrightnessVirtualValues);
                if (this.mScreenBrightnessDarkConfig > bottom) {
                    Slog.w(TAG, "config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") should be less than or equal to the first value of config_autoBrightnessLcdBacklightValues (" + bottom + ").");
                }
                screenBrightnessRangeMinimum = bottom < screenBrightnessRangeMinimum ? bottom : screenBrightnessRangeMinimum;
                this.mAutomaticBrightnessController = new AutomaticBrightnessController(this, handler.getLooper(), sensorManager, screenAutoBrightnessSpline, lightSensorWarmUpTimeConfig, screenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum, dozeScaleFactor, lightSensorRate, brighteningLightDebounce, darkeningLightDebounce, autoBrightnessResetAmbientLuxAfterWarmUp, ambientLightHorizon, autoBrightnessAdjustmentMaxGamma);
                if (MTK_AAL_SUPPORT && lux.length + 1 == screenBrightness.length) {
                    int[] curve = new int[lux.length + 1 + screenBrightness.length];
                    curve[0] = 0;
                    System.arraycopy(lux, 0, curve, 1, lux.length);
                    System.arraycopy(screenBrightness, 0, curve, lux.length + 1, screenBrightness.length);
                    this.mTuningInitCurve = curve;
                }
            }
        }
        this.mScreenBrightnessRangeMinimum = screenBrightnessRangeMinimum;
        this.mColorFadeFadesConfig = resources.getBoolean(android.R.^attr-private.colorAccentPrimaryVariant);
        this.mProximitySensor = this.mSensorManager.getDefaultSensor(8);
        if (this.mProximitySensor != null) {
            this.mProximityThreshold = Math.min(this.mProximitySensor.getMaximumRange(), TYPICAL_PROXIMITY_THRESHOLD);
        }
    }

    public boolean isProximitySensorAvailable() {
        return this.mProximitySensor != null;
    }

    public void setIPOScreenOnDelay(int msec) {
        this.mPowerState.setIPOScreenOnDelay(msec);
    }

    public boolean requestPowerState(DisplayManagerInternal.DisplayPowerRequest request, boolean waitForNegativeProximity) {
        boolean z;
        synchronized (this.mLock) {
            boolean changed = false;
            if (waitForNegativeProximity) {
                if (!this.mPendingWaitForNegativeProximityLocked) {
                    this.mPendingWaitForNegativeProximityLocked = true;
                    changed = true;
                }
                if (this.mPendingRequestLocked != null) {
                    this.mPendingRequestLocked = new DisplayManagerInternal.DisplayPowerRequest(request);
                    changed = true;
                } else if (!this.mPendingRequestLocked.equals(request)) {
                    this.mPendingRequestLocked.copyFrom(request);
                    changed = true;
                }
                if (changed) {
                    this.mDisplayReadyLocked = false;
                }
                if (changed && !this.mPendingRequestChangedLocked) {
                    this.mPendingRequestChangedLocked = true;
                    sendUpdatePowerStateLocked();
                }
                if (DEBUG && changed) {
                    Slog.d(TAG, "requestPowerState: " + request + ", waitForNegativeProximity=" + waitForNegativeProximity + ", changed=" + changed);
                }
                z = this.mDisplayReadyLocked;
            } else {
                if (this.mPendingRequestLocked != null) {
                }
                if (changed) {
                }
                if (changed) {
                    this.mPendingRequestChangedLocked = true;
                    sendUpdatePowerStateLocked();
                }
                if (DEBUG) {
                    Slog.d(TAG, "requestPowerState: " + request + ", waitForNegativeProximity=" + waitForNegativeProximity + ", changed=" + changed);
                }
                z = this.mDisplayReadyLocked;
            }
        }
        return z;
    }

    private void sendUpdatePowerState() {
        synchronized (this.mLock) {
            sendUpdatePowerStateLocked();
        }
    }

    private void sendUpdatePowerStateLocked() {
        if (this.mPendingUpdatePowerStateLocked) {
            return;
        }
        this.mPendingUpdatePowerStateLocked = true;
        Message msg = this.mHandler.obtainMessage(1);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    private void initialize() {
        this.mPowerState = new DisplayPowerState(this.mBlanker, new ColorFade(0));
        this.mColorFadeOnAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, 0.0f, 1.0f);
        this.mColorFadeOnAnimator.setDuration(250L);
        this.mColorFadeOnAnimator.addListener(this.mAnimatorListener);
        this.mColorFadeOffAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, 1.0f, 0.0f);
        this.mColorFadeOffAnimator.setDuration(400L);
        this.mColorFadeOffAnimator.addListener(this.mAnimatorListener);
        this.mScreenBrightnessRampAnimator = new RampAnimator<>(this.mPowerState, DisplayPowerState.SCREEN_BRIGHTNESS);
        this.mScreenBrightnessRampAnimator.setListener(this.mRampAnimatorListener);
        try {
            this.mBatteryStats.noteScreenState(this.mPowerState.getScreenState());
            this.mBatteryStats.noteScreenBrightness(this.mPowerState.getScreenBrightness());
        } catch (RemoteException e) {
        }
    }

    private void updatePowerState() {
        int state;
        boolean mustInitialize = false;
        boolean autoBrightnessAdjustmentChanged = false;
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
                mustInitialize = true;
            } else if (this.mPendingRequestChangedLocked) {
                autoBrightnessAdjustmentChanged = this.mPowerRequest.screenAutoBrightnessAdjustment != this.mPendingRequestLocked.screenAutoBrightnessAdjustment;
                this.mPowerRequest.copyFrom(this.mPendingRequestLocked);
                this.mWaitingForNegativeProximity |= this.mPendingWaitForNegativeProximityLocked;
                this.mPendingWaitForNegativeProximityLocked = false;
                this.mPendingRequestChangedLocked = false;
                this.mDisplayReadyLocked = false;
            }
            boolean mustNotify = !this.mDisplayReadyLocked;
            Slog.d(TAG, "updatePowerState: " + this.mPowerRequest + ", mWaitingForNegativeProximity=" + this.mWaitingForNegativeProximity + ", autoBrightnessAdjustmentChanged=" + autoBrightnessAdjustmentChanged + ", mustNotify=" + mustNotify);
            if (mustInitialize) {
                initialize();
                if (MTK_AAL_RUNTIME_TUNING_SUPPORT) {
                    IntentFilter filter = new IntentFilter(MTK_AAL_UPDATE_CONFIG_ACTION);
                    this.mContext.registerReceiver(new UpdateConfigReceiver(this, null), filter);
                    writeInitConfig();
                }
            }
            if (MTK_AAL_RUNTIME_TUNING_SUPPORT) {
                updateRuntimeConfig();
            }
            int brightness = -1;
            boolean performScreenOffTransition = false;
            switch (this.mPowerRequest.policy) {
                case 0:
                    state = 1;
                    performScreenOffTransition = true;
                    break;
                case 1:
                    state = this.mPowerRequest.dozeScreenState != 0 ? this.mPowerRequest.dozeScreenState : 3;
                    if (!this.mAllowAutoBrightnessWhileDozingConfig) {
                        brightness = this.mPowerRequest.dozeScreenBrightness;
                    }
                    break;
                default:
                    state = 2;
                    break;
            }
            if (!f7assertionsDisabled) {
                if (!(state != 0)) {
                    throw new AssertionError();
                }
            }
            if (this.mProximitySensor != null) {
                if (this.mPowerRequest.useProximitySensor && state != 1) {
                    setProximitySensorEnabled(true);
                    if (!this.mScreenOffBecauseOfProximity && this.mProximity == 1) {
                        this.mScreenOffBecauseOfProximity = true;
                        sendOnProximityPositiveWithWakelock();
                    }
                } else if (this.mWaitingForNegativeProximity && this.mScreenOffBecauseOfProximity && this.mProximity == 1 && state != 1) {
                    setProximitySensorEnabled(true);
                } else {
                    if (this.mPowerRequest.useProximitySensor) {
                        if (this.mScreenOffBecauseOfProximity) {
                            this.mProximity = -1;
                        }
                        setProximitySensorEnabled(true);
                    } else {
                        setProximitySensorEnabled(false);
                    }
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
                state = 1;
            }
            animateScreenStateChange(state, performScreenOffTransition);
            int state2 = this.mPowerState.getScreenState();
            if (state2 == 1) {
                brightness = 0;
            }
            boolean autoBrightnessEnabled = false;
            if (this.mAutomaticBrightnessController != null) {
                boolean autoBrightnessEnabledInDoze = this.mAllowAutoBrightnessWhileDozingConfig ? state2 == 3 || state2 == 4 : false;
                autoBrightnessEnabled = this.mPowerRequest.useAutoBrightness && (state2 == 2 || autoBrightnessEnabledInDoze) && brightness < 0;
                boolean z = autoBrightnessAdjustmentChanged ? this.mPowerRequest.brightnessSetByUser : false;
                Slog.d(TAG, "ABC configure: enabledInDoze=" + this.mAllowAutoBrightnessWhileDozingConfig + ", lowDimmingProtectionEnabled=" + this.mLowDimmingProtectionEnabled + ", adjustment=" + this.mPowerRequest.screenAutoBrightnessAdjustment + ", state=" + state2 + ", brightness=" + brightness);
                this.mAutomaticBrightnessController.configure(!autoBrightnessEnabled ? this.mLowDimmingProtectionEnabled : true, this.mPowerRequest.screenAutoBrightnessAdjustment, state2 != 2, z, this.mPowerRequest.useTwilight);
            }
            if (this.mPowerRequest.boostScreenBrightness && brightness != 0) {
                brightness = DhcpPacket.MAX_OPTION_LEN;
            }
            boolean slowChange = false;
            if (brightness < 0) {
                if (autoBrightnessEnabled) {
                    brightness = this.mAutomaticBrightnessController.getAutomaticScreenBrightness();
                }
                if (brightness >= 0) {
                    brightness = clampScreenBrightness(brightness);
                    if (this.mAppliedAutoBrightness && !autoBrightnessAdjustmentChanged) {
                        slowChange = true;
                    }
                    this.mAppliedAutoBrightness = true;
                } else {
                    this.mAppliedAutoBrightness = false;
                }
            } else {
                this.mAppliedAutoBrightness = false;
            }
            if (brightness < 0 && (state2 == 3 || state2 == 4)) {
                brightness = this.mScreenBrightnessDozeConfig;
            }
            if (brightness < 0) {
                brightness = clampScreenBrightness(this.mPowerRequest.screenBrightness);
                if (LOW_DIMMING_PROTECTION_SUPPORT) {
                    if (!isScreenStateBright()) {
                        this.mLowDimmingProtectionTriggerBrightness = 0;
                    }
                    this.mLowDimmingProtectionEnabled = brightness < 40;
                    this.mAutomaticBrightnessController.configure(!autoBrightnessEnabled ? this.mLowDimmingProtectionEnabled : true, this.mPowerRequest.screenAutoBrightnessAdjustment, state2 != 2, autoBrightnessAdjustmentChanged ? this.mPowerRequest.brightnessSetByUser : false, this.mPowerRequest.useTwilight);
                    int minBrightness = protectedMinimumBrightness();
                    if (getBrightnessSetting() == this.mPowerRequest.screenBrightness && brightness < minBrightness && brightness != this.mLowDimmingProtectionTriggerBrightness && isScreenStateBright()) {
                        long current = SystemClock.uptimeMillis();
                        this.mHandler.removeMessages(100);
                        this.mHandler.sendEmptyMessageAtTime(100, 5000 + current);
                        Slog.d(TAG, "Trigger low dimming protection : " + brightness + " < " + minBrightness + ", auto = " + this.mAutomaticBrightnessController.getAutomaticScreenBrightness());
                        this.mLowDimmingProtectionTriggerBrightness = brightness;
                    }
                }
            }
            if (this.mPowerRequest.policy == 2) {
                if (brightness > this.mScreenBrightnessRangeMinimum) {
                    brightness = Math.max(Math.min(brightness + (-10) > 0 ? brightness - 10 : brightness / 2, this.mScreenBrightnessDimConfig), this.mScreenBrightnessRangeMinimum);
                }
                if (!this.mAppliedDimming) {
                    slowChange = false;
                }
                this.mAppliedDimming = true;
            } else if (this.mAppliedDimming) {
                slowChange = false;
                this.mAppliedDimming = false;
            }
            if (this.mPowerRequest.lowPowerMode) {
                if (brightness > this.mScreenBrightnessRangeMinimum) {
                    brightness = Math.max(brightness / 2, this.mScreenBrightnessRangeMinimum);
                }
                if (!this.mAppliedLowPower) {
                    slowChange = false;
                }
                this.mAppliedLowPower = true;
            } else if (this.mAppliedLowPower) {
                slowChange = false;
                this.mAppliedLowPower = false;
            }
            if (!this.mPendingScreenOff && (state2 == 2 || state2 == 3)) {
                int rate = 40;
                if (MTK_AAL_SUPPORT) {
                    if (this.mTuningQuicklyApply) {
                        slowChange = false;
                    }
                    rate = this.mPowerState.getScreenBrightness() < brightness ? this.BRIGHTNESS_RAMP_RATE_BRIGHTEN : this.BRIGHTNESS_RAMP_RATE_DARKEN;
                }
                if (!slowChange) {
                    rate = this.mBrightnessRampRateFast;
                }
                animateScreenBrightness(brightness, rate);
            }
            boolean ready = (this.mPendingScreenOnUnblocker != null || this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted()) ? false : this.mPowerState.waitUntilClean(this.mCleanListener);
            boolean finished = ready && !this.mScreenBrightnessRampAnimator.isAnimating();
            if (ready && state2 != 1 && this.mReportedScreenStateToPolicy == 1) {
                this.mReportedScreenStateToPolicy = 2;
                this.mWindowManagerPolicy.screenTurnedOn();
            }
            if (!finished && !this.mUnfinishedBusiness) {
                if (DEBUG) {
                    Slog.d(TAG, "Unfinished business...");
                }
                this.mCallbacks.acquireSuspendBlocker();
                this.mUnfinishedBusiness = true;
            }
            if (ready && mustNotify) {
                synchronized (this.mLock) {
                    if (!this.mPendingRequestChangedLocked) {
                        this.mDisplayReadyLocked = true;
                        if (DEBUG) {
                            Slog.d(TAG, "Display ready!");
                        }
                    }
                }
                sendOnStateChangedWithWakelock();
            }
            if (finished && this.mUnfinishedBusiness) {
                if (DEBUG) {
                    Slog.d(TAG, "Finished business...");
                }
                this.mUnfinishedBusiness = false;
                this.mCallbacks.releaseSuspendBlocker();
            }
        }
    }

    @Override
    public void updateBrightness() {
        sendUpdatePowerState();
    }

    private void blockScreenOn() {
        ScreenOnUnblocker screenOnUnblocker = null;
        if (this.mPendingScreenOnUnblocker != null) {
            return;
        }
        Trace.asyncTraceBegin(524288L, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
        this.mPendingScreenOnUnblocker = new ScreenOnUnblocker(this, screenOnUnblocker);
        this.mScreenOnBlockStartRealTime = SystemClock.elapsedRealtime();
        Slog.i(TAG, "Blocking screen on until initial contents have been drawn.");
    }

    private void unblockScreenOn() {
        if (this.mPendingScreenOnUnblocker == null) {
            return;
        }
        this.mPendingScreenOnUnblocker = null;
        long delay = SystemClock.elapsedRealtime() - this.mScreenOnBlockStartRealTime;
        Slog.i(TAG, "Unblocked screen on after " + delay + " ms");
        Trace.asyncTraceEnd(524288L, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
    }

    private boolean setScreenState(int state) {
        if (this.mPowerState.getScreenState() != state) {
            if (this.mPowerState.getScreenState() != 1) {
            }
            this.mPowerState.setScreenState(state);
            try {
                this.mBatteryStats.noteScreenState(state);
            } catch (RemoteException e) {
            }
        }
        boolean isOff = state == 1;
        if (isOff && this.mReportedScreenStateToPolicy != 0 && !this.mScreenOffBecauseOfProximity) {
            this.mReportedScreenStateToPolicy = 0;
            unblockScreenOn();
            this.mWindowManagerPolicy.screenTurnedOff();
        } else if (!isOff && this.mReportedScreenStateToPolicy == 0) {
            this.mReportedScreenStateToPolicy = 1;
            if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                blockScreenOn();
            } else {
                unblockScreenOn();
            }
            this.mWindowManagerPolicy.screenTurningOn(this.mPendingScreenOnUnblocker);
        }
        return this.mPendingScreenOnUnblocker == null;
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void animateScreenBrightness(int target, int rate) {
        if (DEBUG) {
            Slog.d(TAG, "Animating brightness: target=" + target + ", rate=" + rate);
        }
        if (!this.mScreenBrightnessRampAnimator.animateTo(target, rate)) {
            return;
        }
        try {
            this.mBatteryStats.noteScreenBrightness(target);
        } catch (RemoteException e) {
        }
    }

    private void animateScreenStateChange(int target, boolean performScreenOffTransition) {
        if (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted()) {
            if (target != 2) {
                return;
            } else {
                this.mPendingScreenOff = false;
            }
        }
        if (this.mPendingScreenOff && target != 1) {
            setScreenState(1);
            this.mPendingScreenOff = false;
            this.mPowerState.dismissColorFadeResources();
        }
        if (target == 2) {
            if (!setScreenState(2)) {
                return;
            }
            this.mPowerState.setColorFadeLevel(1.0f);
            this.mPowerState.dismissColorFade();
            return;
        }
        if (target == 3) {
            if ((this.mScreenBrightnessRampAnimator.isAnimating() && this.mPowerState.getScreenState() == 2) || !setScreenState(3)) {
                return;
            }
            this.mPowerState.setColorFadeLevel(1.0f);
            this.mPowerState.dismissColorFade();
            return;
        }
        if (target == 4) {
            if (this.mScreenBrightnessRampAnimator.isAnimating() && this.mPowerState.getScreenState() != 4) {
                return;
            }
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
        this.mPendingScreenOff = true;
        if (this.mPowerState.getColorFadeLevel() == 0.0f) {
            setScreenState(1);
            this.mPendingScreenOff = false;
            this.mPowerState.dismissColorFadeResources();
        } else {
            if (performScreenOffTransition) {
                if (this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 1) && this.mPowerState.getScreenState() != 1) {
                    this.mColorFadeOffAnimator.start();
                    return;
                }
            }
            this.mColorFadeOffAnimator.end();
        }
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable) {
            if (this.mProximitySensorEnabled) {
                return;
            }
            if (DEBUG) {
                Slog.d(TAG, "setProximitySensorEnabled : True");
            }
            this.mProximitySensorEnabled = true;
            this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mHandler);
            return;
        }
        if (!this.mProximitySensorEnabled) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "setProximitySensorEnabled : False");
        }
        this.mProximitySensorEnabled = false;
        this.mProximity = -1;
        this.mPendingProximity = -1;
        this.mHandler.removeMessages(2);
        this.mSensorManager.unregisterListener(this.mProximitySensorListener);
        clearPendingProximityDebounceTime();
    }

    private void handleProximitySensorEvent(long time, boolean positive) {
        if (!this.mProximitySensorEnabled) {
            return;
        }
        if (this.mPendingProximity == 0 && !positive) {
            return;
        }
        if (this.mPendingProximity == 1 && positive) {
            return;
        }
        this.mHandler.removeMessages(2);
        if (positive) {
            this.mPendingProximity = 1;
            setPendingProximityDebounceTime(0 + time);
        } else {
            this.mPendingProximity = 0;
            setPendingProximityDebounceTime(250 + time);
        }
        debounceProximitySensor();
    }

    private void debounceProximitySensor() {
        if (!this.mProximitySensorEnabled || this.mPendingProximity == -1 || this.mPendingProximityDebounceTime < 0) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (this.mPendingProximityDebounceTime <= now) {
            this.mProximity = this.mPendingProximity;
            updatePowerState();
            clearPendingProximityDebounceTime();
        } else {
            Message msg = this.mHandler.obtainMessage(2);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageAtTime(msg, this.mPendingProximityDebounceTime);
        }
    }

    private void clearPendingProximityDebounceTime() {
        if (this.mPendingProximityDebounceTime < 0) {
            return;
        }
        this.mPendingProximityDebounceTime = -1L;
        this.mCallbacks.releaseSuspendBlocker();
    }

    private void setPendingProximityDebounceTime(long debounceTime) {
        if (this.mPendingProximityDebounceTime < 0) {
            this.mCallbacks.acquireSuspendBlocker();
        }
        this.mPendingProximityDebounceTime = debounceTime;
    }

    private void sendOnStateChangedWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnStateChangedRunnable);
    }

    private void sendOnProximityPositiveWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityPositiveRunnable);
    }

    private void sendOnProximityNegativeWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityNegativeRunnable);
    }

    public void dump(final PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println();
            pw.println("Display Power Controller Locked State:");
            pw.println("  mDisplayReadyLocked=" + this.mDisplayReadyLocked);
            pw.println("  mPendingRequestLocked=" + this.mPendingRequestLocked);
            pw.println("  mPendingRequestChangedLocked=" + this.mPendingRequestChangedLocked);
            pw.println("  mPendingWaitForNegativeProximityLocked=" + this.mPendingWaitForNegativeProximityLocked);
            pw.println("  mPendingUpdatePowerStateLocked=" + this.mPendingUpdatePowerStateLocked);
        }
        pw.println();
        pw.println("Display Power Controller Configuration:");
        pw.println("  mScreenBrightnessDozeConfig=" + this.mScreenBrightnessDozeConfig);
        pw.println("  mScreenBrightnessDimConfig=" + this.mScreenBrightnessDimConfig);
        pw.println("  mScreenBrightnessDarkConfig=" + this.mScreenBrightnessDarkConfig);
        pw.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        pw.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        pw.println("  mUseSoftwareAutoBrightnessConfig=" + this.mUseSoftwareAutoBrightnessConfig);
        pw.println("  mAllowAutoBrightnessWhileDozingConfig=" + this.mAllowAutoBrightnessWhileDozingConfig);
        pw.println("  mColorFadeFadesConfig=" + this.mColorFadeFadesConfig);
        this.mHandler.runWithScissors(new Runnable() {
            @Override
            public void run() {
                DisplayPowerController.this.dumpLocal(pw);
            }
        }, 1000L);
    }

    private void dumpLocal(PrintWriter pw) {
        pw.println();
        pw.println("Display Power Controller Thread State:");
        pw.println("  mPowerRequest=" + this.mPowerRequest);
        pw.println("  mWaitingForNegativeProximity=" + this.mWaitingForNegativeProximity);
        pw.println("  mProximitySensor=" + this.mProximitySensor);
        pw.println("  mProximitySensorEnabled=" + this.mProximitySensorEnabled);
        pw.println("  mProximityThreshold=" + this.mProximityThreshold);
        pw.println("  mProximity=" + proximityToString(this.mProximity));
        pw.println("  mPendingProximity=" + proximityToString(this.mPendingProximity));
        pw.println("  mPendingProximityDebounceTime=" + TimeUtils.formatUptime(this.mPendingProximityDebounceTime));
        pw.println("  mScreenOffBecauseOfProximity=" + this.mScreenOffBecauseOfProximity);
        pw.println("  mAppliedAutoBrightness=" + this.mAppliedAutoBrightness);
        pw.println("  mAppliedDimming=" + this.mAppliedDimming);
        pw.println("  mAppliedLowPower=" + this.mAppliedLowPower);
        pw.println("  mPendingScreenOnUnblocker=" + this.mPendingScreenOnUnblocker);
        pw.println("  mPendingScreenOff=" + this.mPendingScreenOff);
        pw.println("  mReportedToPolicy=" + reportedToPolicyToString(this.mReportedScreenStateToPolicy));
        if (this.mScreenBrightnessRampAnimator != null) {
            pw.println("  mScreenBrightnessRampAnimator.isAnimating()=" + this.mScreenBrightnessRampAnimator.isAnimating());
        }
        if (this.mColorFadeOnAnimator != null) {
            pw.println("  mColorFadeOnAnimator.isStarted()=" + this.mColorFadeOnAnimator.isStarted());
        }
        if (this.mColorFadeOffAnimator != null) {
            pw.println("  mColorFadeOffAnimator.isStarted()=" + this.mColorFadeOffAnimator.isStarted());
        }
        if (this.mPowerState != null) {
            this.mPowerState.dump(pw);
        }
        if (this.mAutomaticBrightnessController == null) {
            return;
        }
        this.mAutomaticBrightnessController.dump(pw);
    }

    private static String proximityToString(int state) {
        switch (state) {
            case -1:
                return "Unknown";
            case 0:
                return "Negative";
            case 1:
                return "Positive";
            default:
                return Integer.toString(state);
        }
    }

    private static String reportedToPolicyToString(int state) {
        switch (state) {
            case 0:
                return "REPORTED_TO_POLICY_SCREEN_OFF";
            case 1:
                return "REPORTED_TO_POLICY_SCREEN_TURNING_ON";
            case 2:
                return "REPORTED_TO_POLICY_SCREEN_ON";
            default:
                return Integer.toString(state);
        }
    }

    private static Spline createAutoBrightnessSpline(int[] lux, int[] brightness, boolean virtualValues) {
        try {
            int n = brightness.length;
            float[] x = new float[n];
            float[] y = new float[n];
            y[0] = normalizeAbsoluteBrightness(brightness[0], virtualValues);
            for (int i = 1; i < n; i++) {
                x[i] = lux[i - 1];
                y[i] = normalizeAbsoluteBrightness(brightness[i], virtualValues);
            }
            Spline spline = Spline.createSpline(x, y);
            if (DEBUG) {
                Slog.d(TAG, "Auto-brightness spline: " + spline);
                for (float v = 1.0f; v < lux[lux.length - 1] * 1.25f; v *= 1.25f) {
                    Slog.d(TAG, String.format("  %7.1f: %7.1f", Float.valueOf(v), Float.valueOf(spline.interpolate(v))));
                }
            }
            return spline;
        } catch (IllegalArgumentException ex) {
            Slog.e(TAG, "Could not create auto-brightness spline.", ex);
            return null;
        }
    }

    private static float normalizeAbsoluteBrightness(int value, boolean virtualValue) {
        return clampAbsoluteBrightness(value, virtualValue) / 255.0f;
    }

    private static int clampAbsoluteBrightness(int value, boolean virtualValue) {
        if (!virtualValue) {
            value = PowerManager.dimmingPhysicalToVirtual(value);
        }
        return MathUtils.constrain(value, 0, DhcpPacket.MAX_OPTION_LEN);
    }

    private final class DisplayControllerHandler extends Handler {
        public DisplayControllerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DisplayPowerController.this.updatePowerState();
                    break;
                case 2:
                    DisplayPowerController.this.debounceProximitySensor();
                    break;
                case 3:
                    if (DisplayPowerController.this.mPendingScreenOnUnblocker == msg.obj) {
                        DisplayPowerController.this.unblockScreenOn();
                        DisplayPowerController.this.updatePowerState();
                    }
                    break;
                case 100:
                    DisplayPowerController.this.handleProtectLowDimming();
                    break;
            }
        }
    }

    private final class ScreenOnUnblocker implements WindowManagerPolicy.ScreenOnListener {
        ScreenOnUnblocker(DisplayPowerController this$0, ScreenOnUnblocker screenOnUnblocker) {
            this();
        }

        private ScreenOnUnblocker() {
        }

        public void onScreenOn() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(3, this);
            msg.setAsynchronous(true);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    private void writeInitConfig() {
        synchronized (this.mLock) {
            if (this.mTuningInitCurve != null) {
                this.mTuningAli2BliSerial = nativeSetTuningIntArray(1, this.mTuningInitCurve);
                this.mTuningInitCurve = null;
            }
            if (this.mTuningBliBrightenSerial == -1) {
                this.mTuningBliBrightenSerial = nativeSetTuningInt(2, this.BRIGHTNESS_RAMP_RATE_BRIGHTEN);
            }
            if (this.mTuningBliDarkenSerial == -1) {
                this.mTuningBliDarkenSerial = nativeSetTuningInt(3, this.BRIGHTNESS_RAMP_RATE_DARKEN);
            }
        }
    }

    private void updateRuntimeConfig() {
        synchronized (this.mLock) {
            boolean updated = false;
            int serial = nativeGetTuningSerial(1);
            if (serial != this.mTuningAli2BliSerial) {
                int length = nativeGetTuningInt(0);
                if (length > 0) {
                    int[] curve = new int[length * 2];
                    nativeGetTuningIntArray(1, curve);
                    if (curve[0] != 0) {
                        Slog.w(TAG, "AALRuntimeTuning: lux[0] is not 0: " + curve[0]);
                    }
                    int[] lux = new int[length - 1];
                    int[] brightness = new int[length];
                    System.arraycopy(curve, 1, lux, 0, length - 1);
                    System.arraycopy(curve, length, brightness, 0, length);
                    Spline spline = createAutoBrightnessSpline(lux, brightness, false);
                    if (spline != null) {
                        this.mAutomaticBrightnessController.setScreenAutoBrightnessSpline(spline);
                        this.mTuningAli2BliSerial = serial;
                        updated = true;
                        Slog.d(TAG, "AALRuntimeTuning: curve updated. Length = " + length + ", serial = " + serial);
                        Slog.d(TAG, "AALRuntimeTuning: lux = " + curve[0] + "-" + lux[lux.length - 1] + " brightness = " + brightness[0] + "-" + brightness[brightness.length - 1]);
                    } else {
                        Slog.e(TAG, "AALRuntimeTuning: invalid curve is given.");
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("AALRuntimeTuning: curve = ");
                        for (int i = 0; i < length; i++) {
                            buffer.append(curve[i]);
                            buffer.append(":");
                            buffer.append(curve[length + i]);
                            buffer.append(" ");
                        }
                        Slog.e(TAG, buffer.toString());
                    }
                } else {
                    Slog.e(TAG, "AALRuntimeTuning: invalid curve length: " + length);
                }
            }
            if (updated) {
                this.mAutomaticBrightnessController.updateRuntimeConfig();
                this.mTuningQuicklyApply = true;
            }
            int serial2 = nativeGetTuningSerial(2);
            if (serial2 != this.mTuningBliBrightenSerial) {
                this.BRIGHTNESS_RAMP_RATE_BRIGHTEN = nativeGetTuningInt(2);
                if (this.BRIGHTNESS_RAMP_RATE_BRIGHTEN <= 0) {
                    this.BRIGHTNESS_RAMP_RATE_BRIGHTEN = 40;
                }
                this.mTuningBliBrightenSerial = serial2;
                Slog.d(TAG, "AALRuntimeTuning: brighten = " + this.BRIGHTNESS_RAMP_RATE_BRIGHTEN + ", serial = " + serial2);
            }
            int serial3 = nativeGetTuningSerial(3);
            if (serial3 != this.mTuningBliDarkenSerial) {
                this.BRIGHTNESS_RAMP_RATE_DARKEN = nativeGetTuningInt(3);
                if (this.BRIGHTNESS_RAMP_RATE_DARKEN <= 0) {
                    this.BRIGHTNESS_RAMP_RATE_DARKEN = 40;
                }
                this.mTuningBliDarkenSerial = serial3;
                Slog.d(TAG, "AALRuntimeTuning: darken = " + this.BRIGHTNESS_RAMP_RATE_DARKEN + ", serial = " + serial3);
            }
        }
    }

    private class UpdateConfigReceiver extends BroadcastReceiver {
        UpdateConfigReceiver(DisplayPowerController this$0, UpdateConfigReceiver updateConfigReceiver) {
            this();
        }

        private UpdateConfigReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Slog.d(DisplayPowerController.TAG, "AALRuntimeTuning: UpdateConfigReceiver received an intent.");
            DisplayPowerController.this.sendUpdatePowerState();
        }
    }

    private boolean isScreenStateBright() {
        return this.mPowerRequest.policy == 3;
    }

    private int protectedMinimumBrightness() {
        int autoBrightness = this.mAutomaticBrightnessController.getAutomaticScreenBrightness();
        return autoBrightness < 80 ? 1 : 40;
    }

    private int getBrightnessSetting() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mPowerRequest.screenBrightness, -2);
    }

    private void handleProtectLowDimming() {
        if (!this.mAppliedAutoBrightness) {
            int minBrightness = protectedMinimumBrightness();
            if (isScreenStateBright() && !this.mPowerRequest.useAutoBrightness && getBrightnessSetting() == this.mPowerRequest.screenBrightness && this.mPowerRequest.screenBrightness < 40 && this.mPowerRequest.screenBrightness < minBrightness) {
                Slog.d(TAG, "Low dimming protection : " + this.mPowerRequest.screenBrightness + " -> " + minBrightness + ", auto = " + this.mAutomaticBrightnessController.getAutomaticScreenBrightness());
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", minBrightness, -2);
                Intent intent = new Intent(MTK_AAL_LOW_DIMMING_PROTECTION_TRIGGERED);
                intent.addFlags(67108864);
                this.mContext.sendBroadcast(intent);
            }
        }
        this.mLowDimmingProtectionTriggerBrightness = 0;
    }
}
