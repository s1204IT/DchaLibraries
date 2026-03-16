package com.android.server.display;

import android.R;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
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
import com.android.server.lights.LightsManager;
import java.io.PrintWriter;

final class DisplayPowerController implements AutomaticBrightnessController.Callbacks {
    static final boolean $assertionsDisabled;
    private static final int BRIGHTNESS_RAMP_RATE_FAST = 200;
    private static final int BRIGHTNESS_RAMP_RATE_SLOW = 40;
    private static final int COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 400;
    private static final int COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 250;
    private static boolean DEBUG = false;
    private static final boolean DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT = false;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 2;
    private static final int MSG_SCREEN_ON_UNBLOCKED = 3;
    private static final int MSG_UPDATE_POWER_STATE = 1;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_SENSOR_NEGATIVE_DEBOUNCE_DELAY = 250;
    private static final int PROXIMITY_SENSOR_POSITIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int SCREEN_DIM_MINIMUM_REDUCTION = 10;
    private static final String SCREEN_ON_BLOCKED_TRACE_NAME = "Screen on blocked";
    private static final String TAG = "DisplayPowerController";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final boolean USE_COLOR_FADE_ON_ANIMATION = false;
    private final boolean mAllowAutoBrightnessWhileDozingConfig;
    private boolean mAppliedAutoBrightness;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    private final DisplayBlanker mBlanker;
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
    private final int mScreenBrightnessDarkConfig;
    private final int mScreenBrightnessDimConfig;
    private final int mScreenBrightnessDozeConfig;
    private RampAnimator<DisplayPowerState> mScreenBrightnessRampAnimator;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private boolean mScreenOffBecauseOfProximity;
    private long mScreenOnBlockStartRealTime;
    private final SensorManager mSensorManager;
    private boolean mUnfinishedBusiness;
    private boolean mUseSoftwareAutoBrightnessConfig;
    private boolean mWaitingForNegativeProximity;
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
            boolean positive = DisplayPowerController.DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
            if (DisplayPowerController.this.mProximitySensorEnabled) {
                long time = SystemClock.uptimeMillis();
                float distance = event.values[0];
                if (distance >= 0.0f && distance < DisplayPowerController.this.mProximityThreshold) {
                    positive = true;
                }
                DisplayPowerController.this.handleProximitySensorEvent(time, positive);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final IBatteryStats mBatteryStats = BatteryStatsService.getService();
    private final LightsManager mLights = (LightsManager) LocalServices.getService(LightsManager.class);
    private final WindowManagerPolicy mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);

    static {
        $assertionsDisabled = !DisplayPowerController.class.desiredAssertionStatus();
        DEBUG = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
    }

    public DisplayPowerController(Context context, DisplayManagerInternal.DisplayPowerCallbacks callbacks, Handler handler, SensorManager sensorManager, DisplayBlanker blanker) {
        this.mHandler = new DisplayControllerHandler(handler.getLooper());
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mBlanker = blanker;
        this.mContext = context;
        Resources resources = context.getResources();
        int screenBrightnessSettingMinimum = clampAbsoluteBrightness(resources.getInteger(R.integer.config_defaultActionModeHideDurationMillis));
        this.mScreenBrightnessDozeConfig = clampAbsoluteBrightness(resources.getInteger(R.integer.config_defaultBinderHeavyHitterAutoSamplerBatchSize));
        this.mScreenBrightnessDimConfig = clampAbsoluteBrightness(resources.getInteger(R.integer.config_defaultBinderHeavyHitterWatcherBatchSize));
        this.mScreenBrightnessDarkConfig = clampAbsoluteBrightness(resources.getInteger(R.integer.config_defaultDisplayDefaultColorMode));
        if (this.mScreenBrightnessDarkConfig > this.mScreenBrightnessDimConfig) {
            Slog.w(TAG, "Expected config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") to be less than or equal to config_screenBrightnessDim (" + this.mScreenBrightnessDimConfig + ").");
        }
        if (this.mScreenBrightnessDarkConfig > this.mScreenBrightnessDimConfig) {
            Slog.w(TAG, "Expected config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") to be less than or equal to config_screenBrightnessSettingMinimum (" + screenBrightnessSettingMinimum + ").");
        }
        int screenBrightnessRangeMinimum = Math.min(Math.min(screenBrightnessSettingMinimum, this.mScreenBrightnessDimConfig), this.mScreenBrightnessDarkConfig);
        this.mScreenBrightnessRangeMaximum = 255;
        this.mUseSoftwareAutoBrightnessConfig = resources.getBoolean(R.^attr-private.borderRight);
        this.mAllowAutoBrightnessWhileDozingConfig = resources.getBoolean(R.^attr-private.dreamActivityCloseExitAnimation);
        if (this.mUseSoftwareAutoBrightnessConfig) {
            int[] lux = resources.getIntArray(R.array.config_bg_current_drain_high_threshold_to_bg_restricted);
            int[] screenBrightness = resources.getIntArray(R.array.config_bg_current_drain_high_threshold_to_restricted_bucket);
            int lightSensorWarmUpTimeConfig = resources.getInteger(R.integer.config_defaultHapticFeedbackIntensity);
            float dozeScaleFactor = resources.getFraction(R.fraction.config_biometricNotificationFrrThreshold, 1, 1);
            Spline screenAutoBrightnessSpline = createAutoBrightnessSpline(lux, screenBrightness);
            if (screenAutoBrightnessSpline == null) {
                Slog.e(TAG, "Error in config.xml.  config_autoBrightnessLcdBacklightValues (size " + screenBrightness.length + ") must be monotic and have exactly one more entry than config_autoBrightnessLevels (size " + lux.length + ") which must be strictly increasing.  Auto-brightness will be disabled.");
                this.mUseSoftwareAutoBrightnessConfig = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
            } else {
                int bottom = clampAbsoluteBrightness(screenBrightness[0]);
                if (this.mScreenBrightnessDarkConfig > bottom) {
                    Slog.w(TAG, "config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") should be less than or equal to the first value of config_autoBrightnessLcdBacklightValues (" + bottom + ").");
                }
                screenBrightnessRangeMinimum = bottom < screenBrightnessRangeMinimum ? bottom : screenBrightnessRangeMinimum;
                this.mAutomaticBrightnessController = new AutomaticBrightnessController(this, handler.getLooper(), sensorManager, screenAutoBrightnessSpline, lightSensorWarmUpTimeConfig, screenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum, dozeScaleFactor);
            }
        }
        this.mScreenBrightnessRangeMinimum = screenBrightnessRangeMinimum;
        this.mColorFadeFadesConfig = resources.getBoolean(R.^attr-private.checkMarkGravity);
        this.mProximitySensor = this.mSensorManager.getDefaultSensor(8);
        if (this.mProximitySensor != null) {
            this.mProximityThreshold = Math.min(this.mProximitySensor.getMaximumRange(), TYPICAL_PROXIMITY_THRESHOLD);
        }
    }

    public boolean isProximitySensorAvailable() {
        if (this.mProximitySensor != null) {
            return true;
        }
        return DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
    }

    public boolean requestPowerState(DisplayManagerInternal.DisplayPowerRequest request, boolean waitForNegativeProximity) {
        boolean z;
        if (DEBUG) {
            Slog.d(TAG, "requestPowerState: " + request + ", waitForNegativeProximity=" + waitForNegativeProximity);
        }
        synchronized (this.mLock) {
            boolean changed = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
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
                    this.mDisplayReadyLocked = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                }
                if (changed && !this.mPendingRequestChangedLocked) {
                    this.mPendingRequestChangedLocked = true;
                    sendUpdatePowerStateLocked();
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
        if (!this.mPendingUpdatePowerStateLocked) {
            this.mPendingUpdatePowerStateLocked = true;
            Message msg = this.mHandler.obtainMessage(1);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    private void initialize() {
        this.mPowerState = new DisplayPowerState(this.mBlanker, this.mLights.getLight(0), new ColorFade(0));
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
        boolean mustInitialize = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
        boolean autoBrightnessAdjustmentChanged = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
        synchronized (this.mLock) {
            this.mPendingUpdatePowerStateLocked = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
            if (this.mPendingRequestLocked != null) {
                if (this.mPowerRequest == null) {
                    this.mPowerRequest = new DisplayManagerInternal.DisplayPowerRequest(this.mPendingRequestLocked);
                    this.mWaitingForNegativeProximity = this.mPendingWaitForNegativeProximityLocked;
                    this.mPendingWaitForNegativeProximityLocked = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    this.mPendingRequestChangedLocked = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    mustInitialize = true;
                } else if (this.mPendingRequestChangedLocked) {
                    autoBrightnessAdjustmentChanged = this.mPowerRequest.screenAutoBrightnessAdjustment != this.mPendingRequestLocked.screenAutoBrightnessAdjustment ? true : DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    this.mPowerRequest.copyFrom(this.mPendingRequestLocked);
                    this.mWaitingForNegativeProximity |= this.mPendingWaitForNegativeProximityLocked;
                    this.mPendingWaitForNegativeProximityLocked = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    this.mPendingRequestChangedLocked = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    this.mDisplayReadyLocked = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                }
                boolean mustNotify = !this.mDisplayReadyLocked ? true : DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                if (mustInitialize) {
                    initialize();
                }
                int brightness = -1;
                boolean performScreenOffTransition = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                switch (this.mPowerRequest.policy) {
                    case 0:
                        state = 1;
                        performScreenOffTransition = true;
                        break;
                    case 1:
                        if (this.mPowerRequest.dozeScreenState != 0) {
                            state = this.mPowerRequest.dozeScreenState;
                        } else {
                            state = 3;
                        }
                        if (!this.mAllowAutoBrightnessWhileDozingConfig) {
                            brightness = this.mPowerRequest.dozeScreenBrightness;
                        }
                        break;
                    default:
                        state = 2;
                        break;
                }
                if (!$assertionsDisabled && state == 0) {
                    throw new AssertionError();
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
                        setProximitySensorEnabled(DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT);
                        this.mWaitingForNegativeProximity = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    }
                    if (this.mScreenOffBecauseOfProximity && this.mProximity != 1) {
                        this.mScreenOffBecauseOfProximity = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                        sendOnProximityNegativeWithWakelock();
                    }
                } else {
                    this.mWaitingForNegativeProximity = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                }
                if (this.mScreenOffBecauseOfProximity) {
                    state = 1;
                }
                animateScreenStateChange(state, performScreenOffTransition);
                int state2 = this.mPowerState.getScreenState();
                if (state2 == 1) {
                    brightness = 0;
                }
                boolean autoBrightnessEnabled = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                if (this.mAutomaticBrightnessController != null) {
                    boolean autoBrightnessEnabledInDoze = (this.mAllowAutoBrightnessWhileDozingConfig && (state2 == 3 || state2 == 4)) ? true : DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    autoBrightnessEnabled = (!this.mPowerRequest.useAutoBrightness || !(state2 == 2 || autoBrightnessEnabledInDoze) || brightness >= 0) ? DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT : true;
                    this.mAutomaticBrightnessController.configure(autoBrightnessEnabled, this.mPowerRequest.screenAutoBrightnessAdjustment, state2 != 2 ? true : DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT);
                }
                if (this.mPowerRequest.boostScreenBrightness && brightness != 0) {
                    brightness = 255;
                }
                boolean slowChange = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
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
                        this.mAppliedAutoBrightness = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    }
                } else {
                    this.mAppliedAutoBrightness = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                }
                if (brightness < 0 && (state2 == 3 || state2 == 4)) {
                    brightness = this.mScreenBrightnessDozeConfig;
                }
                if (brightness < 0) {
                    brightness = clampScreenBrightness(this.mPowerRequest.screenBrightness);
                }
                if (this.mPowerRequest.policy == 2) {
                    if (brightness > this.mScreenBrightnessRangeMinimum) {
                        brightness = Math.max(Math.min(brightness - 10, this.mScreenBrightnessDimConfig), this.mScreenBrightnessRangeMinimum);
                    }
                    if (!this.mAppliedDimming) {
                        slowChange = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    }
                    this.mAppliedDimming = true;
                }
                if (this.mPowerRequest.lowPowerMode) {
                    if (brightness > this.mScreenBrightnessRangeMinimum) {
                        brightness = Math.max(brightness / 2, this.mScreenBrightnessRangeMinimum);
                    }
                    if (!this.mAppliedLowPower) {
                        slowChange = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    }
                    this.mAppliedLowPower = true;
                }
                if (!this.mPendingScreenOff) {
                    if (state2 == 2 || state2 == 3) {
                        animateScreenBrightness(brightness, slowChange ? 40 : BRIGHTNESS_RAMP_RATE_FAST);
                    } else {
                        animateScreenBrightness(brightness, 0);
                    }
                }
                boolean ready = (this.mPendingScreenOnUnblocker != null || this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted() || !this.mPowerState.waitUntilClean(this.mCleanListener)) ? DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT : true;
                boolean finished = (!ready || this.mScreenBrightnessRampAnimator.isAnimating()) ? DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT : true;
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
                    this.mUnfinishedBusiness = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                    this.mCallbacks.releaseSuspendBlocker();
                }
            }
        }
    }

    @Override
    public void updateBrightness() {
        sendUpdatePowerState();
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
            long delay = SystemClock.elapsedRealtime() - this.mScreenOnBlockStartRealTime;
            Slog.i(TAG, "Unblocked screen on after " + delay + " ms");
            Trace.asyncTraceEnd(131072L, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
        }
    }

    private boolean setScreenState(int state) {
        if (this.mPowerState.getScreenState() != state) {
            boolean wasOn = this.mPowerState.getScreenState() != 1;
            this.mPowerState.setScreenState(state);
            try {
                this.mBatteryStats.noteScreenState(state);
            } catch (RemoteException e) {
            }
            boolean isOn = state != 1;
            if (wasOn && !isOn) {
                unblockScreenOn();
                this.mWindowManagerPolicy.screenTurnedOff();
            } else if (!wasOn && isOn) {
                if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                    blockScreenOn();
                } else {
                    unblockScreenOn();
                }
                this.mWindowManagerPolicy.screenTurningOn(this.mPendingScreenOnUnblocker);
            }
        }
        if (this.mPendingScreenOnUnblocker == null) {
            return true;
        }
        return DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void animateScreenBrightness(int target, int rate) {
        if (DEBUG) {
            Slog.d(TAG, "Animating brightness: target=" + target + ", rate=" + rate);
        }
        if (this.mScreenBrightnessRampAnimator.animateTo(target, rate)) {
            try {
                this.mBatteryStats.noteScreenBrightness(target);
            } catch (RemoteException e) {
            }
        }
    }

    private void animateScreenStateChange(int target, boolean performScreenOffTransition) {
        if (!this.mColorFadeOnAnimator.isStarted() && !this.mColorFadeOffAnimator.isStarted()) {
            if (this.mPendingScreenOff && target != 1) {
                setScreenState(1);
                this.mPendingScreenOff = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
            }
            if (target == 2) {
                if (setScreenState(2)) {
                    this.mPowerState.setColorFadeLevel(1.0f);
                    this.mPowerState.dismissColorFade();
                    return;
                }
                return;
            }
            if (target == 3) {
                if ((!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() != 2) && setScreenState(3)) {
                    this.mPowerState.setColorFadeLevel(1.0f);
                    this.mPowerState.dismissColorFade();
                    return;
                }
                return;
            }
            if (target == 4) {
                if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 4) {
                    if (this.mPowerState.getScreenState() != 4) {
                        if (setScreenState(3)) {
                            setScreenState(4);
                        } else {
                            return;
                        }
                    }
                    this.mPowerState.setColorFadeLevel(1.0f);
                    this.mPowerState.dismissColorFade();
                    return;
                }
                return;
            }
            this.mPendingScreenOff = true;
            if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                setScreenState(1);
                this.mPendingScreenOff = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
                return;
            }
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
            if (!this.mProximitySensorEnabled) {
                this.mProximitySensorEnabled = true;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mHandler);
                return;
            }
            return;
        }
        if (this.mProximitySensorEnabled) {
            this.mProximitySensorEnabled = DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT;
            this.mProximity = -1;
            this.mPendingProximity = -1;
            this.mHandler.removeMessages(2);
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            clearPendingProximityDebounceTime();
        }
    }

    private void handleProximitySensorEvent(long time, boolean positive) {
        if (this.mProximitySensorEnabled) {
            if (this.mPendingProximity != 0 || positive) {
                if (this.mPendingProximity != 1 || !positive) {
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
            }
        }
    }

    private void debounceProximitySensor() {
        if (this.mProximitySensorEnabled && this.mPendingProximity != -1 && this.mPendingProximityDebounceTime >= 0) {
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
    }

    private void clearPendingProximityDebounceTime() {
        if (this.mPendingProximityDebounceTime >= 0) {
            this.mPendingProximityDebounceTime = -1L;
            this.mCallbacks.releaseSuspendBlocker();
        }
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
        pw.println("  mScreenBrightnessRampAnimator.isAnimating()=" + this.mScreenBrightnessRampAnimator.isAnimating());
        if (this.mColorFadeOnAnimator != null) {
            pw.println("  mColorFadeOnAnimator.isStarted()=" + this.mColorFadeOnAnimator.isStarted());
        }
        if (this.mColorFadeOffAnimator != null) {
            pw.println("  mColorFadeOffAnimator.isStarted()=" + this.mColorFadeOffAnimator.isStarted());
        }
        if (this.mPowerState != null) {
            this.mPowerState.dump(pw);
        }
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.dump(pw);
        }
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

    private static Spline createAutoBrightnessSpline(int[] lux, int[] brightness) {
        try {
            int n = brightness.length;
            float[] x = new float[n];
            float[] y = new float[n];
            y[0] = normalizeAbsoluteBrightness(brightness[0]);
            for (int i = 1; i < n; i++) {
                x[i] = lux[i - 1];
                y[i] = normalizeAbsoluteBrightness(brightness[i]);
            }
            Spline spline = Spline.createSpline(x, y);
            if (!DEBUG) {
                return spline;
            }
            Slog.d(TAG, "Auto-brightness spline: " + spline);
            for (float v = 1.0f; v < lux[lux.length - 1] * 1.25f; v *= 1.25f) {
                Slog.d(TAG, String.format("  %7.1f: %7.1f", Float.valueOf(v), Float.valueOf(spline.interpolate(v))));
            }
            return spline;
        } catch (IllegalArgumentException ex) {
            Slog.e(TAG, "Could not create auto-brightness spline.", ex);
            return null;
        }
    }

    private static float normalizeAbsoluteBrightness(int value) {
        return clampAbsoluteBrightness(value) / 255.0f;
    }

    private static int clampAbsoluteBrightness(int value) {
        return MathUtils.constrain(value, 0, 255);
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
            }
        }
    }

    private final class ScreenOnUnblocker implements WindowManagerPolicy.ScreenOnListener {
        private ScreenOnUnblocker() {
        }

        public void onScreenOn() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(3, this);
            msg.setAsynchronous(true);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }
}
