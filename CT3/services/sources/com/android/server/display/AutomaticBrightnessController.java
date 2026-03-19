package com.android.server.display;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import android.util.TimeUtils;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.twilight.TwilightListener;
import com.android.server.twilight.TwilightManager;
import com.android.server.twilight.TwilightState;
import java.io.PrintWriter;

class AutomaticBrightnessController {
    private static final long AMBIENT_LIGHT_PREDICTION_TIME_MILLIS = 100;
    private static final float BRIGHTENING_LIGHT_HYSTERESIS = 0.1f;
    private static final int BRIGHTNESS_ADJUSTMENT_SAMPLE_DEBOUNCE_MILLIS = 10000;
    private static final float DARKENING_LIGHT_HYSTERESIS = 0.2f;
    private static final boolean DEBUG = "eng".equals(Build.TYPE);
    private static final boolean DEBUG_PRETEND_LIGHT_SENSOR_ABSENT = false;
    private static final int MSG_BRIGHTNESS_ADJUSTMENT_SAMPLE = 2;
    private static final int MSG_UPDATE_AMBIENT_LUX = 1;
    private static final int MSG_UPDATE_RUNTIME_CONFIG = 3;
    private static final long MTK_AAL_AMBIENT_LIGHT_HORIZON = 500;
    private static final String TAG = "AutomaticBrightnessController";
    private static final float TWILIGHT_ADJUSTMENT_MAX_GAMMA = 1.0f;
    private static final boolean USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT = true;
    private final int mAmbientLightHorizon;
    private AmbientLightRingBuffer mAmbientLightRingBuffer;
    private float mAmbientLux;
    private boolean mAmbientLuxValid;
    private final long mBrighteningLightDebounceConfig;
    private float mBrighteningLuxThreshold;
    private float mBrightnessAdjustmentSampleOldAdjustment;
    private int mBrightnessAdjustmentSampleOldBrightness;
    private float mBrightnessAdjustmentSampleOldGamma;
    private float mBrightnessAdjustmentSampleOldLux;
    private boolean mBrightnessAdjustmentSamplePending;
    private final Callbacks mCallbacks;
    private final long mDarkeningLightDebounceConfig;
    private float mDarkeningLuxThreshold;
    private final float mDozeScaleFactor;
    private boolean mDozing;
    private AutomaticBrightnessHandler mHandler;
    private AmbientLightRingBuffer mInitialHorizonAmbientLightRingBuffer;
    private float mLastObservedLux;
    private long mLastObservedLuxTime;
    private final Sensor mLightSensor;
    private long mLightSensorEnableTime;
    private boolean mLightSensorEnabled;
    private final int mLightSensorRate;
    private int mLightSensorWarmUpTimeConfig;
    private int mRecentLightSamples;
    private final boolean mResetAmbientLuxAfterWarmUpConfig;
    private float mScreenAutoBrightnessAdjustmentMaxGamma;
    private Spline mScreenAutoBrightnessSpline;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private final SensorManager mSensorManager;
    private boolean mUseTwilight;
    private final int mWeightingIntercept;
    private int mScreenAutoBrightness = -1;
    private float mScreenAutoBrightnessAdjustment = 0.0f;
    private float mLastScreenAutoBrightnessGamma = TWILIGHT_ADJUSTMENT_MAX_GAMMA;
    private final SensorEventListener mLightSensorListener = new SensorEventListener() {
        private long mPrevLogTime = 0;
        private float mPrevLogLux = 0.0f;

        @Override
        public void onSensorChanged(SensorEvent event) {
            Slog.d(AutomaticBrightnessController.TAG, "onSensorChanged: mLightSensorEnabled=" + AutomaticBrightnessController.this.mLightSensorEnabled + ", mAmbientLuxValid=" + AutomaticBrightnessController.this.mAmbientLuxValid + ", lux=" + (AutomaticBrightnessController.this.mLightSensorEnabled ? Float.valueOf(event.values[0]) : "null"));
            if (AutomaticBrightnessController.this.mLightSensorEnabled) {
                long time = SystemClock.uptimeMillis();
                float lux = event.values[0];
                AutomaticBrightnessController.this.handleLightSensorEvent(time, lux);
                if (time - this.mPrevLogTime >= 500 || this.mPrevLogLux * 1.2f <= lux || lux * 1.2f <= this.mPrevLogLux) {
                    this.mPrevLogTime = time;
                    this.mPrevLogLux = lux;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final TwilightListener mTwilightListener = new TwilightListener() {
        @Override
        public void onTwilightStateChanged() {
            AutomaticBrightnessController.this.updateAutoBrightness(true);
        }
    };
    private final RuntimeConfig mRuntimeConfig = new RuntimeConfig();
    private final TwilightManager mTwilight = (TwilightManager) LocalServices.getService(TwilightManager.class);

    interface Callbacks {
        void updateBrightness();
    }

    public AutomaticBrightnessController(Callbacks callbacks, Looper looper, SensorManager sensorManager, Spline autoBrightnessSpline, int lightSensorWarmUpTime, int brightnessMin, int brightnessMax, float dozeScaleFactor, int lightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, boolean resetAmbientLuxAfterWarmUpConfig, int ambientLightHorizon, float autoBrightnessAdjustmentMaxGamma) {
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mScreenAutoBrightnessSpline = autoBrightnessSpline;
        this.mScreenBrightnessRangeMinimum = brightnessMin;
        this.mScreenBrightnessRangeMaximum = brightnessMax;
        this.mLightSensorWarmUpTimeConfig = lightSensorWarmUpTime;
        this.mDozeScaleFactor = dozeScaleFactor;
        this.mLightSensorRate = lightSensorRate;
        this.mBrighteningLightDebounceConfig = brighteningLightDebounceConfig;
        this.mDarkeningLightDebounceConfig = darkeningLightDebounceConfig;
        this.mResetAmbientLuxAfterWarmUpConfig = resetAmbientLuxAfterWarmUpConfig;
        this.mAmbientLightHorizon = ambientLightHorizon;
        this.mWeightingIntercept = ambientLightHorizon;
        this.mScreenAutoBrightnessAdjustmentMaxGamma = autoBrightnessAdjustmentMaxGamma;
        this.mHandler = new AutomaticBrightnessHandler(looper);
        this.mAmbientLightRingBuffer = new AmbientLightRingBuffer(this.mLightSensorRate, this.mAmbientLightHorizon);
        this.mInitialHorizonAmbientLightRingBuffer = new AmbientLightRingBuffer(this.mLightSensorRate, this.mAmbientLightHorizon);
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
    }

    public int getAutomaticScreenBrightness() {
        if (DEBUG) {
            Slog.d(TAG, "getAutomaticScreenBrightness: brightness=" + this.mScreenAutoBrightness + ", dozing=" + this.mDozing + ", factor=" + this.mDozeScaleFactor);
        }
        return this.mDozing ? (int) (this.mScreenAutoBrightness * this.mDozeScaleFactor) : this.mScreenAutoBrightness;
    }

    public void configure(boolean enable, float adjustment, boolean dozing, boolean userInitiatedChange, boolean useTwilight) {
        this.mDozing = dozing;
        boolean changed = setLightSensorEnabled(enable && !dozing);
        if (changed | setScreenAutoBrightnessAdjustment(adjustment) | setUseTwilight(useTwilight)) {
            updateAutoBrightness(false);
        }
        if (!enable || dozing || !userInitiatedChange) {
            return;
        }
        prepareBrightnessAdjustmentSample();
    }

    private boolean setUseTwilight(boolean useTwilight) {
        if (this.mUseTwilight == useTwilight) {
            return false;
        }
        if (useTwilight) {
            this.mTwilight.registerListener(this.mTwilightListener, this.mHandler);
        } else {
            this.mTwilight.unregisterListener(this.mTwilightListener);
        }
        this.mUseTwilight = useTwilight;
        return true;
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Automatic Brightness Controller Configuration:");
        pw.println("  mScreenAutoBrightnessSpline=" + this.mScreenAutoBrightnessSpline);
        pw.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        pw.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        pw.println("  mLightSensorWarmUpTimeConfig=" + this.mLightSensorWarmUpTimeConfig);
        pw.println("  mBrighteningLightDebounceConfig=" + this.mBrighteningLightDebounceConfig);
        pw.println("  mDarkeningLightDebounceConfig=" + this.mDarkeningLightDebounceConfig);
        pw.println("  mResetAmbientLuxAfterWarmUpConfig=" + this.mResetAmbientLuxAfterWarmUpConfig);
        pw.println();
        pw.println("Automatic Brightness Controller State:");
        pw.println("  mLightSensor=" + this.mLightSensor);
        pw.println("  mTwilight.getCurrentState()=" + this.mTwilight.getCurrentState());
        pw.println("  mLightSensorEnabled=" + this.mLightSensorEnabled);
        pw.println("  mLightSensorEnableTime=" + TimeUtils.formatUptime(this.mLightSensorEnableTime));
        pw.println("  mAmbientLux=" + this.mAmbientLux);
        pw.println("  mAmbientLightHorizon=" + this.mAmbientLightHorizon);
        pw.println("  mBrighteningLuxThreshold=" + this.mBrighteningLuxThreshold);
        pw.println("  mDarkeningLuxThreshold=" + this.mDarkeningLuxThreshold);
        pw.println("  mLastObservedLux=" + this.mLastObservedLux);
        pw.println("  mLastObservedLuxTime=" + TimeUtils.formatUptime(this.mLastObservedLuxTime));
        pw.println("  mRecentLightSamples=" + this.mRecentLightSamples);
        pw.println("  mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer);
        pw.println("  mInitialHorizonAmbientLightRingBuffer=" + this.mInitialHorizonAmbientLightRingBuffer);
        pw.println("  mScreenAutoBrightness=" + this.mScreenAutoBrightness);
        pw.println("  mScreenAutoBrightnessAdjustment=" + this.mScreenAutoBrightnessAdjustment);
        pw.println("  mScreenAutoBrightnessAdjustmentMaxGamma=" + this.mScreenAutoBrightnessAdjustmentMaxGamma);
        pw.println("  mLastScreenAutoBrightnessGamma=" + this.mLastScreenAutoBrightnessGamma);
        pw.println("  mDozing=" + this.mDozing);
    }

    private boolean setLightSensorEnabled(boolean enable) {
        if (enable != this.mLightSensorEnabled) {
            Slog.d(TAG, "setLightSensorEnabled: enable=" + enable + ", mLightSensorEnabled=" + this.mLightSensorEnabled + ", mAmbientLuxValid=" + this.mAmbientLuxValid + ", mResetAmbientLuxAfterWarmUpConfig=" + this.mResetAmbientLuxAfterWarmUpConfig);
        }
        if (enable) {
            if (!this.mLightSensorEnabled) {
                this.mLightSensorEnabled = true;
                this.mLightSensorEnableTime = SystemClock.uptimeMillis();
                this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, this.mLightSensorRate * 1000, this.mHandler);
                return true;
            }
        } else if (this.mLightSensorEnabled) {
            this.mLightSensorEnabled = false;
            this.mAmbientLuxValid = !this.mResetAmbientLuxAfterWarmUpConfig;
            this.mRecentLightSamples = 0;
            this.mAmbientLightRingBuffer.clear();
            this.mInitialHorizonAmbientLightRingBuffer.clear();
            this.mHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
        }
        return false;
    }

    private void handleLightSensorEvent(long time, float lux) {
        this.mHandler.removeMessages(1);
        applyLightSensorMeasurement(time, lux);
        updateAmbientLux(time);
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mRecentLightSamples++;
        if (time <= this.mLightSensorEnableTime + ((long) this.mAmbientLightHorizon)) {
            this.mInitialHorizonAmbientLightRingBuffer.push(time, lux);
        }
        this.mAmbientLightRingBuffer.prune(time - ((long) this.mAmbientLightHorizon));
        this.mAmbientLightRingBuffer.push(time, lux);
        this.mLastObservedLux = lux;
        this.mLastObservedLuxTime = time;
    }

    private boolean setScreenAutoBrightnessAdjustment(float adjustment) {
        if (adjustment != this.mScreenAutoBrightnessAdjustment) {
            this.mScreenAutoBrightnessAdjustment = adjustment;
            return true;
        }
        return false;
    }

    private void setAmbientLux(float lux) {
        this.mAmbientLux = lux;
        this.mBrighteningLuxThreshold = this.mAmbientLux * 1.1f;
        this.mDarkeningLuxThreshold = this.mAmbientLux * 0.8f;
        DisplayPowerController.nativeSetDebouncedAmbientLight((int) lux);
    }

    private float calculateAmbientLux(long now) {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "calculateAmbientLux: No ambient light readings available");
            return -1.0f;
        }
        float sum = 0.0f;
        float totalWeight = 0.0f;
        long endTime = AMBIENT_LIGHT_PREDICTION_TIME_MILLIS;
        for (int i = N - 1; i >= 0; i--) {
            long startTime = this.mAmbientLightRingBuffer.getTime(i) - now;
            if (DisplayPowerController.MTK_AAL_SUPPORT && startTime < -500 && totalWeight > 0.0f) {
                break;
            }
            float weight = calculateWeight(startTime, endTime);
            this.mAmbientLightRingBuffer.getLux(i);
            totalWeight += weight;
            sum += this.mAmbientLightRingBuffer.getLux(i) * weight;
            endTime = startTime;
        }
        if (DEBUG) {
            Slog.d(TAG, "calculateAmbientLux: totalWeight=" + totalWeight + ", newAmbientLux=" + (sum / totalWeight));
        }
        return sum / totalWeight;
    }

    private float calculateWeight(long startDelta, long endDelta) {
        return weightIntegral(endDelta) - weightIntegral(startDelta);
    }

    private float weightIntegral(long x) {
        return x * ((x * 0.5f) + this.mWeightingIntercept);
    }

    private long nextAmbientLightBrighteningTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) > this.mBrighteningLuxThreshold; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return this.mBrighteningLightDebounceConfig + earliestValidTime;
    }

    private long nextAmbientLightDarkeningTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) < this.mDarkeningLuxThreshold; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return this.mDarkeningLightDebounceConfig + earliestValidTime;
    }

    private void updateAmbientLux() {
        long time = SystemClock.uptimeMillis();
        this.mAmbientLightRingBuffer.prune(time - ((long) this.mAmbientLightHorizon));
        updateAmbientLux(time);
    }

    private void updateAmbientLux(long time) {
        if (DEBUG) {
            Slog.d(TAG, "updateAmbientLux: mAmbientLuxValid=" + this.mAmbientLuxValid + ", AAL=" + DisplayPowerController.MTK_AAL_SUPPORT);
        }
        if (!this.mAmbientLuxValid) {
            long timeWhenSensorWarmedUp = ((long) this.mLightSensorWarmUpTimeConfig) + this.mLightSensorEnableTime;
            if (time < timeWhenSensorWarmedUp) {
                if (DEBUG) {
                    Slog.d(TAG, "updateAmbientLux: Sensor not  ready yet: time=" + time + ", timeWhenSensorWarmedUp=" + timeWhenSensorWarmedUp);
                }
                this.mHandler.sendEmptyMessageAtTime(1, timeWhenSensorWarmedUp);
                return;
            } else {
                setAmbientLux(calculateAmbientLux(time));
                this.mAmbientLuxValid = true;
                if (DEBUG) {
                    Slog.d(TAG, "updateAmbientLux: Initializing: mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer + ", mAmbientLux=" + this.mAmbientLux);
                }
                updateAutoBrightness(true);
            }
        }
        long nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
        long nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        float ambientLux = calculateAmbientLux(time);
        if (DisplayPowerController.MTK_AAL_SUPPORT) {
            Slog.d(TAG, "updateAmbientLux: ambientLux=" + ambientLux + ", timeToBrighten=" + (nextBrightenTransition - time) + ", timeToDarken=" + (nextDarkenTransition - time) + ", current=" + this.mAmbientLux);
        }
        if ((ambientLux >= this.mBrighteningLuxThreshold && nextBrightenTransition <= time) || (ambientLux <= this.mDarkeningLuxThreshold && nextDarkenTransition <= time)) {
            setAmbientLux(ambientLux);
            if (DEBUG) {
                Slog.d(TAG, "updateAmbientLux: " + (ambientLux > this.mAmbientLux ? "Brightened" : "Darkened") + ": mBrighteningLuxThreshold=" + this.mBrighteningLuxThreshold + ", mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer + ", mAmbientLux=" + this.mAmbientLux);
            }
            updateAutoBrightness(true);
            nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
            nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        }
        long nextTransitionTime = Math.min(nextDarkenTransition, nextBrightenTransition);
        if (nextTransitionTime <= time) {
            nextTransitionTime = time + ((long) this.mLightSensorRate);
        }
        if (DEBUG) {
            Slog.d(TAG, "updateAmbientLux: Scheduling ambient lux update for " + nextTransitionTime + TimeUtils.formatUptime(nextTransitionTime));
        }
        this.mHandler.sendEmptyMessageAtTime(1, nextTransitionTime);
    }

    private void updateAutoBrightness(boolean sendUpdate) {
        TwilightState state;
        if (this.mAmbientLuxValid) {
            float value = this.mScreenAutoBrightnessSpline.interpolate(this.mAmbientLux);
            float gamma = TWILIGHT_ADJUSTMENT_MAX_GAMMA;
            if (this.mScreenAutoBrightnessAdjustment != 0.0f) {
                float adjGamma = MathUtils.pow(this.mScreenAutoBrightnessAdjustmentMaxGamma, Math.min(TWILIGHT_ADJUSTMENT_MAX_GAMMA, Math.max(-1.0f, -this.mScreenAutoBrightnessAdjustment)));
                gamma = TWILIGHT_ADJUSTMENT_MAX_GAMMA * adjGamma;
                if (DEBUG) {
                    Slog.d(TAG, "updateAutoBrightness: adjGamma=" + adjGamma);
                }
            }
            if (this.mUseTwilight && (state = this.mTwilight.getCurrentState()) != null && state.isNight()) {
                System.currentTimeMillis();
                gamma *= (state.getAmount() * TWILIGHT_ADJUSTMENT_MAX_GAMMA) + TWILIGHT_ADJUSTMENT_MAX_GAMMA;
                if (DEBUG) {
                    Slog.d(TAG, "updateAutoBrightness: twilight amount=" + state.getAmount());
                }
            }
            if (gamma != TWILIGHT_ADJUSTMENT_MAX_GAMMA) {
                value = MathUtils.pow(value, gamma);
                if (DEBUG) {
                    Slog.d(TAG, "updateAutoBrightness: gamma=" + gamma + ", in=" + value + ", out=" + value);
                }
            }
            int newScreenAutoBrightness = clampScreenBrightness(Math.round(255.0f * value));
            if (this.mScreenAutoBrightness != newScreenAutoBrightness) {
                if (DEBUG) {
                    Slog.d(TAG, "updateAutoBrightness: mScreenAutoBrightness=" + this.mScreenAutoBrightness + ", newScreenAutoBrightness=" + newScreenAutoBrightness + ", ambientLux=" + this.mAmbientLux);
                }
                this.mScreenAutoBrightness = newScreenAutoBrightness;
                this.mLastScreenAutoBrightnessGamma = gamma;
                if (sendUpdate) {
                    this.mCallbacks.updateBrightness();
                }
            }
        }
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void prepareBrightnessAdjustmentSample() {
        if (!this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = true;
            this.mBrightnessAdjustmentSampleOldAdjustment = this.mScreenAutoBrightnessAdjustment;
            this.mBrightnessAdjustmentSampleOldLux = this.mAmbientLuxValid ? this.mAmbientLux : -1.0f;
            this.mBrightnessAdjustmentSampleOldBrightness = this.mScreenAutoBrightness;
            this.mBrightnessAdjustmentSampleOldGamma = this.mLastScreenAutoBrightnessGamma;
        } else {
            this.mHandler.removeMessages(2);
        }
        this.mHandler.sendEmptyMessageDelayed(2, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private void cancelBrightnessAdjustmentSample() {
        if (!this.mBrightnessAdjustmentSamplePending) {
            return;
        }
        this.mBrightnessAdjustmentSamplePending = false;
        this.mHandler.removeMessages(2);
    }

    private void collectBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            if (!this.mAmbientLuxValid || this.mScreenAutoBrightness < 0) {
                return;
            }
            if (DEBUG) {
                Slog.d(TAG, "Auto-brightness adjustment changed by user: adj=" + this.mScreenAutoBrightnessAdjustment + ", lux=" + this.mAmbientLux + ", brightness=" + this.mScreenAutoBrightness + ", gamma=" + this.mLastScreenAutoBrightnessGamma + ", ring=" + this.mAmbientLightRingBuffer);
            }
            EventLog.writeEvent(EventLogTags.AUTO_BRIGHTNESS_ADJ, Float.valueOf(this.mBrightnessAdjustmentSampleOldAdjustment), Float.valueOf(this.mBrightnessAdjustmentSampleOldLux), Integer.valueOf(this.mBrightnessAdjustmentSampleOldBrightness), Float.valueOf(this.mBrightnessAdjustmentSampleOldGamma), Float.valueOf(this.mScreenAutoBrightnessAdjustment), Float.valueOf(this.mAmbientLux), Integer.valueOf(this.mScreenAutoBrightness), Float.valueOf(this.mLastScreenAutoBrightnessGamma));
        }
    }

    private final class AutomaticBrightnessHandler extends Handler {
        public AutomaticBrightnessHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AutomaticBrightnessController.this.updateAmbientLux();
                    break;
                case 2:
                    AutomaticBrightnessController.this.collectBrightnessAdjustmentSample();
                    break;
                case 3:
                    AutomaticBrightnessController.this.updateRuntimeConfigInternal();
                    break;
            }
        }
    }

    private static final class AmbientLightRingBuffer {
        private static final float BUFFER_SLACK = 1.5f;
        private int mCapacity;
        private int mCount;
        private int mEnd;
        private float[] mRingLux;
        private long[] mRingTime;
        private int mStart;

        public AmbientLightRingBuffer(long lightSensorRate, int ambientLightHorizon) {
            this.mCapacity = (int) Math.ceil((ambientLightHorizon * BUFFER_SLACK) / lightSensorRate);
            this.mRingLux = new float[this.mCapacity];
            this.mRingTime = new long[this.mCapacity];
        }

        public float getLux(int index) {
            return this.mRingLux[offsetOf(index)];
        }

        public long getTime(int index) {
            return this.mRingTime[offsetOf(index)];
        }

        public void push(long time, float lux) {
            int next = this.mEnd;
            if (this.mCount == this.mCapacity) {
                int newSize = this.mCapacity * 2;
                float[] newRingLux = new float[newSize];
                long[] newRingTime = new long[newSize];
                int length = this.mCapacity - this.mStart;
                System.arraycopy(this.mRingLux, this.mStart, newRingLux, 0, length);
                System.arraycopy(this.mRingTime, this.mStart, newRingTime, 0, length);
                if (this.mStart != 0) {
                    System.arraycopy(this.mRingLux, 0, newRingLux, length, this.mStart);
                    System.arraycopy(this.mRingTime, 0, newRingTime, length, this.mStart);
                }
                this.mRingLux = newRingLux;
                this.mRingTime = newRingTime;
                next = this.mCapacity;
                this.mCapacity = newSize;
                this.mStart = 0;
            }
            this.mRingTime[next] = time;
            this.mRingLux[next] = lux;
            this.mEnd = next + 1;
            if (this.mEnd == this.mCapacity) {
                this.mEnd = 0;
            }
            this.mCount++;
        }

        public void prune(long horizon) {
            if (this.mCount == 0) {
                return;
            }
            while (this.mCount > 1) {
                int next = this.mStart + 1;
                if (next >= this.mCapacity) {
                    next -= this.mCapacity;
                }
                if (this.mRingTime[next] > horizon) {
                    break;
                }
                this.mStart = next;
                this.mCount--;
            }
            if (this.mRingTime[this.mStart] >= horizon) {
                return;
            }
            this.mRingTime[this.mStart] = horizon;
        }

        public int size() {
            return this.mCount;
        }

        public void clear() {
            this.mStart = 0;
            this.mEnd = 0;
            this.mCount = 0;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append('[');
            for (int i = 0; i < this.mCount; i++) {
                long next = i + 1 < this.mCount ? getTime(i + 1) : SystemClock.uptimeMillis();
                if (i != 0) {
                    buf.append(", ");
                }
                buf.append(getLux(i));
                buf.append(" / ");
                buf.append(next - getTime(i));
                buf.append("ms");
            }
            buf.append(']');
            return buf.toString();
        }

        private int offsetOf(int index) {
            if (index >= this.mCount || index < 0) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            int index2 = index + this.mStart;
            if (index2 >= this.mCapacity) {
                return index2 - this.mCapacity;
            }
            return index2;
        }
    }

    private class RuntimeConfig {
        public Spline mScreenAutoBrightnessSpline = null;

        public RuntimeConfig() {
        }
    }

    public void setScreenAutoBrightnessSpline(Spline spline) {
        this.mRuntimeConfig.mScreenAutoBrightnessSpline = spline;
    }

    public void updateRuntimeConfig() {
        this.mHandler.sendEmptyMessage(3);
    }

    private void updateRuntimeConfigInternal() {
        this.mScreenAutoBrightnessSpline = this.mRuntimeConfig.mScreenAutoBrightnessSpline;
        updateAutoBrightness(true);
    }
}
