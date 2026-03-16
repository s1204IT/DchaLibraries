package com.android.server.display;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import android.util.TimeUtils;
import com.android.server.LocalServices;
import com.android.server.twilight.TwilightListener;
import com.android.server.twilight.TwilightManager;
import com.android.server.twilight.TwilightState;
import java.io.PrintWriter;
import java.util.Arrays;

class AutomaticBrightnessController {
    private static final int AMBIENT_LIGHT_HORIZON = 10000;
    private static final long AMBIENT_LIGHT_PREDICTION_TIME_MILLIS = 100;
    private static final long BRIGHTENING_LIGHT_DEBOUNCE = 4000;
    private static final float BRIGHTENING_LIGHT_HYSTERESIS = 0.1f;
    private static final long DARKENING_LIGHT_DEBOUNCE = 8000;
    private static final float DARKENING_LIGHT_HYSTERESIS = 0.2f;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PRETEND_LIGHT_SENSOR_ABSENT = false;
    private static final int LIGHT_SENSOR_RATE_MILLIS = 1000;
    private static final int MSG_UPDATE_AMBIENT_LUX = 1;
    private static final float SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT_MAX_GAMMA = 3.0f;
    private static final String TAG = "AutomaticBrightnessController";
    private static final float TWILIGHT_ADJUSTMENT_MAX_GAMMA = 1.5f;
    private static final long TWILIGHT_ADJUSTMENT_TIME = 7200000;
    private static final boolean USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT = true;
    private static final boolean USE_TWILIGHT_ADJUSTMENT = PowerManager.useTwilightAdjustmentFeature();
    private static final int WEIGHTING_INTERCEPT = 10000;
    private float mAmbientLux;
    private boolean mAmbientLuxValid;
    private float mBrighteningLuxThreshold;
    private final Callbacks mCallbacks;
    private float mDarkeningLuxThreshold;
    private final float mDozeScaleFactor;
    private boolean mDozing;
    private AutomaticBrightnessHandler mHandler;
    private float mLastObservedLux;
    private long mLastObservedLuxTime;
    private final Sensor mLightSensor;
    private long mLightSensorEnableTime;
    private boolean mLightSensorEnabled;
    private int mLightSensorWarmUpTimeConfig;
    private int mRecentLightSamples;
    private final Spline mScreenAutoBrightnessSpline;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private final SensorManager mSensorManager;
    private int mScreenAutoBrightness = -1;
    private float mScreenAutoBrightnessAdjustment = 0.0f;
    private float mLastScreenAutoBrightnessGamma = 1.0f;
    private final SensorEventListener mLightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (AutomaticBrightnessController.this.mLightSensorEnabled) {
                long time = SystemClock.uptimeMillis();
                float lux = event.values[0];
                AutomaticBrightnessController.this.handleLightSensorEvent(time, lux);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final TwilightListener mTwilightListener = new TwilightListener() {
        @Override
        public void onTwilightStateChanged() {
            AutomaticBrightnessController.this.updateAutoBrightness(AutomaticBrightnessController.USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
        }
    };
    private final TwilightManager mTwilight = (TwilightManager) LocalServices.getService(TwilightManager.class);
    private AmbientLightRingBuffer mAmbientLightRingBuffer = new AmbientLightRingBuffer();

    interface Callbacks {
        void updateBrightness();
    }

    public AutomaticBrightnessController(Callbacks callbacks, Looper looper, SensorManager sensorManager, Spline autoBrightnessSpline, int lightSensorWarmUpTime, int brightnessMin, int brightnessMax, float dozeScaleFactor) {
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mScreenAutoBrightnessSpline = autoBrightnessSpline;
        this.mScreenBrightnessRangeMinimum = brightnessMin;
        this.mScreenBrightnessRangeMaximum = brightnessMax;
        this.mLightSensorWarmUpTimeConfig = lightSensorWarmUpTime;
        this.mDozeScaleFactor = dozeScaleFactor;
        this.mHandler = new AutomaticBrightnessHandler(looper);
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
        if (USE_TWILIGHT_ADJUSTMENT) {
            this.mTwilight.registerListener(this.mTwilightListener, this.mHandler);
        }
    }

    public int getAutomaticScreenBrightness() {
        return this.mDozing ? (int) (this.mScreenAutoBrightness * this.mDozeScaleFactor) : this.mScreenAutoBrightness;
    }

    public void configure(boolean enable, float adjustment, boolean dozing) {
        this.mDozing = dozing;
        boolean changed = setLightSensorEnabled((!enable || dozing) ? false : USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
        if (changed | setScreenAutoBrightnessAdjustment(adjustment)) {
            updateAutoBrightness(false);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Automatic Brightness Controller Configuration:");
        pw.println("  mScreenAutoBrightnessSpline=" + this.mScreenAutoBrightnessSpline);
        pw.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        pw.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        pw.println("  mLightSensorWarmUpTimeConfig=" + this.mLightSensorWarmUpTimeConfig);
        pw.println();
        pw.println("Automatic Brightness Controller State:");
        pw.println("  mLightSensor=" + this.mLightSensor);
        pw.println("  mTwilight.getCurrentState()=" + this.mTwilight.getCurrentState());
        pw.println("  mLightSensorEnabled=" + this.mLightSensorEnabled);
        pw.println("  mLightSensorEnableTime=" + TimeUtils.formatUptime(this.mLightSensorEnableTime));
        pw.println("  mAmbientLux=" + this.mAmbientLux);
        pw.println("  mBrighteningLuxThreshold=" + this.mBrighteningLuxThreshold);
        pw.println("  mDarkeningLuxThreshold=" + this.mDarkeningLuxThreshold);
        pw.println("  mLastObservedLux=" + this.mLastObservedLux);
        pw.println("  mLastObservedLuxTime=" + TimeUtils.formatUptime(this.mLastObservedLuxTime));
        pw.println("  mRecentLightSamples=" + this.mRecentLightSamples);
        pw.println("  mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer);
        pw.println("  mScreenAutoBrightness=" + this.mScreenAutoBrightness);
        pw.println("  mScreenAutoBrightnessAdjustment=" + this.mScreenAutoBrightnessAdjustment);
        pw.println("  mLastScreenAutoBrightnessGamma=" + this.mLastScreenAutoBrightnessGamma);
        pw.println("  mDozing=" + this.mDozing);
    }

    private boolean setLightSensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mLightSensorEnabled) {
                this.mLightSensorEnabled = USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT;
                this.mLightSensorEnableTime = SystemClock.uptimeMillis();
                this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, 1000000, this.mHandler);
                return USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT;
            }
        } else if (this.mLightSensorEnabled) {
            this.mLightSensorEnabled = false;
            this.mAmbientLuxValid = false;
            this.mRecentLightSamples = 0;
            this.mAmbientLightRingBuffer.clear();
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
        this.mAmbientLightRingBuffer.prune(time - 10000);
        this.mAmbientLightRingBuffer.push(time, lux);
        this.mLastObservedLux = lux;
        this.mLastObservedLuxTime = time;
    }

    private boolean setScreenAutoBrightnessAdjustment(float adjustment) {
        if (adjustment == this.mScreenAutoBrightnessAdjustment) {
            return false;
        }
        this.mScreenAutoBrightnessAdjustment = adjustment;
        return USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT;
    }

    private void setAmbientLux(float lux) {
        this.mAmbientLux = lux;
        this.mBrighteningLuxThreshold = this.mAmbientLux * 1.1f;
        this.mDarkeningLuxThreshold = this.mAmbientLux * 0.8f;
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
            float weight = calculateWeight(startTime, endTime);
            this.mAmbientLightRingBuffer.getLux(i);
            totalWeight += weight;
            sum += this.mAmbientLightRingBuffer.getLux(i) * weight;
            endTime = startTime;
        }
        return sum / totalWeight;
    }

    private static float calculateWeight(long startDelta, long endDelta) {
        return weightIntegral(endDelta) - weightIntegral(startDelta);
    }

    private static float weightIntegral(long x) {
        return x * ((x * 0.5f) + 10000.0f);
    }

    private long nextAmbientLightBrighteningTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) > this.mBrighteningLuxThreshold; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return BRIGHTENING_LIGHT_DEBOUNCE + earliestValidTime;
    }

    private long nextAmbientLightDarkeningTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) < this.mDarkeningLuxThreshold; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return DARKENING_LIGHT_DEBOUNCE + earliestValidTime;
    }

    private void updateAmbientLux() {
        long time = SystemClock.uptimeMillis();
        this.mAmbientLightRingBuffer.prune(time - 10000);
        updateAmbientLux(time);
    }

    private void updateAmbientLux(long time) {
        if (!this.mAmbientLuxValid) {
            long timeWhenSensorWarmedUp = ((long) this.mLightSensorWarmUpTimeConfig) + this.mLightSensorEnableTime;
            if (time < timeWhenSensorWarmedUp) {
                this.mHandler.sendEmptyMessageAtTime(1, timeWhenSensorWarmedUp);
                return;
            } else {
                setAmbientLux(calculateAmbientLux(time));
                this.mAmbientLuxValid = USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT;
                updateAutoBrightness(USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
            }
        }
        long nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
        long nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        float ambientLux = calculateAmbientLux(time);
        if ((ambientLux >= this.mBrighteningLuxThreshold && nextBrightenTransition <= time) || (ambientLux <= this.mDarkeningLuxThreshold && nextDarkenTransition <= time)) {
            setAmbientLux(ambientLux);
            updateAutoBrightness(USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
            nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
            nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        }
        long nextTransitionTime = Math.min(nextDarkenTransition, nextBrightenTransition);
        if (nextTransitionTime <= time) {
            nextTransitionTime = time + 1000;
        }
        this.mHandler.sendEmptyMessageAtTime(1, nextTransitionTime);
    }

    private void updateAutoBrightness(boolean sendUpdate) {
        TwilightState state;
        if (this.mAmbientLuxValid) {
            float value = this.mScreenAutoBrightnessSpline.interpolate(this.mAmbientLux);
            float gamma = 1.0f;
            if (this.mScreenAutoBrightnessAdjustment != 0.0f) {
                float adjGamma = MathUtils.pow(SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT_MAX_GAMMA, Math.min(1.0f, Math.max(-1.0f, -this.mScreenAutoBrightnessAdjustment)));
                gamma = 1.0f * adjGamma;
            }
            if (USE_TWILIGHT_ADJUSTMENT && (state = this.mTwilight.getCurrentState()) != null && state.isNight()) {
                long now = System.currentTimeMillis();
                float earlyGamma = getTwilightGamma(now, state.getYesterdaySunset(), state.getTodaySunrise());
                float lateGamma = getTwilightGamma(now, state.getTodaySunset(), state.getTomorrowSunrise());
                gamma *= earlyGamma * lateGamma;
            }
            if (gamma != 1.0f) {
                value = MathUtils.pow(value, gamma);
            }
            int newScreenAutoBrightness = clampScreenBrightness(Math.round(255.0f * value));
            if (this.mScreenAutoBrightness != newScreenAutoBrightness) {
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

    private static float getTwilightGamma(long now, long lastSunset, long nextSunrise) {
        if (lastSunset < 0 || nextSunrise < 0 || now < lastSunset || now > nextSunrise) {
            return 1.0f;
        }
        if (now < lastSunset + TWILIGHT_ADJUSTMENT_TIME) {
            return MathUtils.lerp(1.0f, TWILIGHT_ADJUSTMENT_MAX_GAMMA, (now - lastSunset) / 7200000.0f);
        }
        return now > nextSunrise - TWILIGHT_ADJUSTMENT_TIME ? MathUtils.lerp(1.0f, TWILIGHT_ADJUSTMENT_MAX_GAMMA, (nextSunrise - now) / 7200000.0f) : TWILIGHT_ADJUSTMENT_MAX_GAMMA;
    }

    private final class AutomaticBrightnessHandler extends Handler {
        public AutomaticBrightnessHandler(Looper looper) {
            super(looper, null, AutomaticBrightnessController.USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AutomaticBrightnessController.this.updateAmbientLux();
                    break;
            }
        }
    }

    private static final class AmbientLightRingBuffer {
        private static final float BUFFER_SLACK = 1.5f;
        private static final int DEFAULT_CAPACITY = (int) Math.ceil(15.0d);
        private int mCapacity;
        private int mCount;
        private int mEnd;
        private float[] mRingLux;
        private long[] mRingTime;
        private int mStart;

        public AmbientLightRingBuffer() {
            this(DEFAULT_CAPACITY);
        }

        public AmbientLightRingBuffer(int initialCapacity) {
            this.mCapacity = initialCapacity;
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
            if (this.mCount != 0) {
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
                if (this.mRingTime[this.mStart] < horizon) {
                    this.mRingTime[this.mStart] = horizon;
                }
            }
        }

        public int size() {
            return this.mCount;
        }

        public boolean isEmpty() {
            if (this.mCount == 0) {
                return AutomaticBrightnessController.USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT;
            }
            return false;
        }

        public void clear() {
            this.mStart = 0;
            this.mEnd = 0;
            this.mCount = 0;
        }

        public String toString() {
            int length = this.mCapacity - this.mStart;
            float[] lux = new float[this.mCount];
            long[] time = new long[this.mCount];
            if (this.mCount <= length) {
                System.arraycopy(this.mRingLux, this.mStart, lux, 0, this.mCount);
                System.arraycopy(this.mRingTime, this.mStart, time, 0, this.mCount);
            } else {
                System.arraycopy(this.mRingLux, this.mStart, lux, 0, length);
                System.arraycopy(this.mRingLux, 0, lux, length, this.mCount - length);
                System.arraycopy(this.mRingTime, this.mStart, time, 0, length);
                System.arraycopy(this.mRingTime, 0, time, length, this.mCount - length);
            }
            return "AmbientLightRingBuffer{mCapacity=" + this.mCapacity + ", mStart=" + this.mStart + ", mEnd=" + this.mEnd + ", mCount=" + this.mCount + ", mRingLux=" + Arrays.toString(lux) + ", mRingTime=" + Arrays.toString(time) + "}";
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
}
