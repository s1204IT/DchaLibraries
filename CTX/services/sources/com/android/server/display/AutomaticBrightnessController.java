package com.android.server.display;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Trace;
import android.util.EventLog;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.EventLogTags;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;

class AutomaticBrightnessController {
    private static final int AMBIENT_LIGHT_LONG_HORIZON_MILLIS = 10000;
    private static final long AMBIENT_LIGHT_PREDICTION_TIME_MILLIS = 100;
    private static final int AMBIENT_LIGHT_SHORT_HORIZON_MILLIS = 2000;
    private static final int BRIGHTNESS_ADJUSTMENT_SAMPLE_DEBOUNCE_MILLIS = 10000;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PRETEND_LIGHT_SENSOR_ABSENT = false;
    private static final int MSG_BRIGHTNESS_ADJUSTMENT_SAMPLE = 2;
    private static final int MSG_INVALIDATE_SHORT_TERM_MODEL = 3;
    private static final int MSG_UPDATE_AMBIENT_LUX = 1;
    private static final int SHORT_TERM_MODEL_TIMEOUT_MILLIS = 30000;
    private static final String TAG = "AutomaticBrightnessController";
    private static final boolean USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT = true;
    private AmbientLightRingBuffer mAmbientLightRingBuffer;
    private float mAmbientLux;
    private boolean mAmbientLuxValid;
    private final long mBrighteningLightDebounceConfig;
    private float mBrighteningLuxThreshold;
    private int mBrightnessAdjustmentSampleOldBrightness;
    private float mBrightnessAdjustmentSampleOldLux;
    private boolean mBrightnessAdjustmentSamplePending;
    private final BrightnessMappingStrategy mBrightnessMapper;
    private final Callbacks mCallbacks;
    private final long mDarkeningLightDebounceConfig;
    private float mDarkeningLuxThreshold;
    private final float mDozeScaleFactor;
    private AutomaticBrightnessHandler mHandler;
    private final HysteresisLevels mHysteresisLevels;
    private final int mInitialLightSensorRate;
    private float mLastObservedLux;
    private long mLastObservedLuxTime;
    private final Sensor mLightSensor;
    private long mLightSensorEnableTime;
    private boolean mLightSensorEnabled;
    private int mLightSensorWarmUpTimeConfig;
    private final int mNormalLightSensorRate;
    private int mRecentLightSamples;
    private final boolean mResetAmbientLuxAfterWarmUpConfig;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private final SensorManager mSensorManager;
    private int mScreenAutoBrightness = -1;
    private int mDisplayPolicy = 0;
    private float SHORT_TERM_MODEL_THRESHOLD_RATIO = 0.6f;
    private final SensorEventListener mLightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (AutomaticBrightnessController.this.mLightSensorEnabled) {
                AutomaticBrightnessController.this.handleLightSensorEvent(SystemClock.uptimeMillis(), sensorEvent.values[0]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    private int mCurrentLightSensorRate = -1;
    private final int mAmbientLightHorizon = 10000;
    private final int mWeightingIntercept = 10000;
    private boolean mShortTermModelValid = true;
    private float mShortTermModelAnchor = -1.0f;

    interface Callbacks {
        void updateBrightness();
    }

    public AutomaticBrightnessController(Callbacks callbacks, Looper looper, SensorManager sensorManager, BrightnessMappingStrategy brightnessMappingStrategy, int i, int i2, int i3, float f, int i4, int i5, long j, long j2, boolean z, HysteresisLevels hysteresisLevels) {
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mBrightnessMapper = brightnessMappingStrategy;
        this.mScreenBrightnessRangeMinimum = i2;
        this.mScreenBrightnessRangeMaximum = i3;
        this.mLightSensorWarmUpTimeConfig = i;
        this.mDozeScaleFactor = f;
        this.mNormalLightSensorRate = i4;
        this.mInitialLightSensorRate = i5;
        this.mBrighteningLightDebounceConfig = j;
        this.mDarkeningLightDebounceConfig = j2;
        this.mResetAmbientLuxAfterWarmUpConfig = z;
        this.mHysteresisLevels = hysteresisLevels;
        this.mHandler = new AutomaticBrightnessHandler(looper);
        this.mAmbientLightRingBuffer = new AmbientLightRingBuffer(this.mNormalLightSensorRate, this.mAmbientLightHorizon);
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
    }

    public int getAutomaticScreenBrightness() {
        if (!this.mAmbientLuxValid) {
            return -1;
        }
        if (this.mDisplayPolicy == 1) {
            return (int) (this.mScreenAutoBrightness * this.mDozeScaleFactor);
        }
        return this.mScreenAutoBrightness;
    }

    public float getAutomaticScreenBrightnessAdjustment() {
        return this.mBrightnessMapper.getAutoBrightnessAdjustment();
    }

    public void configure(boolean z, BrightnessConfiguration brightnessConfiguration, float f, boolean z2, float f2, boolean z3, int i) {
        boolean z4 = i == 1;
        boolean brightnessConfiguration2 = setBrightnessConfiguration(brightnessConfiguration) | setDisplayPolicy(i);
        if (z3) {
            brightnessConfiguration2 |= setAutoBrightnessAdjustment(f2);
        }
        if (z2 && z) {
            brightnessConfiguration2 |= setScreenBrightnessByUser(f);
        }
        if ((z2 || z3) && z && !z4) {
            prepareBrightnessAdjustmentSample();
        }
        if (setLightSensorEnabled(z && !z4) | brightnessConfiguration2) {
            updateAutoBrightness(false);
        }
    }

    public boolean hasUserDataPoints() {
        return this.mBrightnessMapper.hasUserDataPoints();
    }

    public boolean isDefaultConfig() {
        return this.mBrightnessMapper.isDefaultConfig();
    }

    public BrightnessConfiguration getDefaultConfig() {
        return this.mBrightnessMapper.getDefaultConfig();
    }

    private boolean setDisplayPolicy(int i) {
        if (this.mDisplayPolicy == i) {
            return false;
        }
        int i2 = this.mDisplayPolicy;
        this.mDisplayPolicy = i;
        if (!isInteractivePolicy(i) && isInteractivePolicy(i2)) {
            this.mHandler.sendEmptyMessageDelayed(3, 30000L);
            return true;
        }
        if (isInteractivePolicy(i) && !isInteractivePolicy(i2)) {
            this.mHandler.removeMessages(3);
            return true;
        }
        return true;
    }

    private static boolean isInteractivePolicy(int i) {
        return i == 3 || i == 2 || i == 4;
    }

    private boolean setScreenBrightnessByUser(float f) {
        if (!this.mAmbientLuxValid) {
            return false;
        }
        this.mBrightnessMapper.addUserDataPoint(this.mAmbientLux, f);
        this.mShortTermModelValid = true;
        this.mShortTermModelAnchor = this.mAmbientLux;
        return true;
    }

    public void resetShortTermModel() {
        this.mBrightnessMapper.clearUserDataPoints();
        this.mShortTermModelValid = true;
        this.mShortTermModelAnchor = -1.0f;
    }

    private void invalidateShortTermModel() {
        this.mShortTermModelValid = false;
    }

    public boolean setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration) {
        if (this.mBrightnessMapper.setBrightnessConfiguration(brightnessConfiguration)) {
            resetShortTermModel();
            return true;
        }
        return false;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println();
        printWriter.println("Automatic Brightness Controller Configuration:");
        printWriter.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        printWriter.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        printWriter.println("  mDozeScaleFactor=" + this.mDozeScaleFactor);
        printWriter.println("  mInitialLightSensorRate=" + this.mInitialLightSensorRate);
        printWriter.println("  mNormalLightSensorRate=" + this.mNormalLightSensorRate);
        printWriter.println("  mLightSensorWarmUpTimeConfig=" + this.mLightSensorWarmUpTimeConfig);
        printWriter.println("  mBrighteningLightDebounceConfig=" + this.mBrighteningLightDebounceConfig);
        printWriter.println("  mDarkeningLightDebounceConfig=" + this.mDarkeningLightDebounceConfig);
        printWriter.println("  mResetAmbientLuxAfterWarmUpConfig=" + this.mResetAmbientLuxAfterWarmUpConfig);
        printWriter.println("  mAmbientLightHorizon=" + this.mAmbientLightHorizon);
        printWriter.println("  mWeightingIntercept=" + this.mWeightingIntercept);
        printWriter.println();
        printWriter.println("Automatic Brightness Controller State:");
        printWriter.println("  mLightSensor=" + this.mLightSensor);
        printWriter.println("  mLightSensorEnabled=" + this.mLightSensorEnabled);
        printWriter.println("  mLightSensorEnableTime=" + TimeUtils.formatUptime(this.mLightSensorEnableTime));
        printWriter.println("  mCurrentLightSensorRate=" + this.mCurrentLightSensorRate);
        printWriter.println("  mAmbientLux=" + this.mAmbientLux);
        printWriter.println("  mAmbientLuxValid=" + this.mAmbientLuxValid);
        printWriter.println("  mBrighteningLuxThreshold=" + this.mBrighteningLuxThreshold);
        printWriter.println("  mDarkeningLuxThreshold=" + this.mDarkeningLuxThreshold);
        printWriter.println("  mLastObservedLux=" + this.mLastObservedLux);
        printWriter.println("  mLastObservedLuxTime=" + TimeUtils.formatUptime(this.mLastObservedLuxTime));
        printWriter.println("  mRecentLightSamples=" + this.mRecentLightSamples);
        printWriter.println("  mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer);
        printWriter.println("  mScreenAutoBrightness=" + this.mScreenAutoBrightness);
        printWriter.println("  mDisplayPolicy=" + DisplayManagerInternal.DisplayPowerRequest.policyToString(this.mDisplayPolicy));
        printWriter.println("  mShortTermModelAnchor=" + this.mShortTermModelAnchor);
        printWriter.println("  mShortTermModelValid=" + this.mShortTermModelValid);
        printWriter.println("  mBrightnessAdjustmentSamplePending=" + this.mBrightnessAdjustmentSamplePending);
        printWriter.println("  mBrightnessAdjustmentSampleOldLux=" + this.mBrightnessAdjustmentSampleOldLux);
        printWriter.println("  mBrightnessAdjustmentSampleOldBrightness=" + this.mBrightnessAdjustmentSampleOldBrightness);
        printWriter.println("  mShortTermModelValid=" + this.mShortTermModelValid);
        printWriter.println();
        this.mBrightnessMapper.dump(printWriter);
        printWriter.println();
        this.mHysteresisLevels.dump(printWriter);
    }

    private boolean setLightSensorEnabled(boolean z) {
        if (z) {
            if (!this.mLightSensorEnabled) {
                this.mLightSensorEnabled = true;
                this.mLightSensorEnableTime = SystemClock.uptimeMillis();
                this.mCurrentLightSensorRate = this.mInitialLightSensorRate;
                this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, this.mCurrentLightSensorRate * 1000, this.mHandler);
                return true;
            }
        } else if (this.mLightSensorEnabled) {
            this.mLightSensorEnabled = false;
            this.mAmbientLuxValid = !this.mResetAmbientLuxAfterWarmUpConfig;
            this.mRecentLightSamples = 0;
            this.mAmbientLightRingBuffer.clear();
            this.mCurrentLightSensorRate = -1;
            this.mHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
        }
        return false;
    }

    private void handleLightSensorEvent(long j, float f) {
        Trace.traceCounter(131072L, "ALS", (int) f);
        this.mHandler.removeMessages(1);
        if (this.mAmbientLightRingBuffer.size() == 0) {
            adjustLightSensorRate(this.mNormalLightSensorRate);
        }
        applyLightSensorMeasurement(j, f);
        updateAmbientLux(j);
    }

    private void applyLightSensorMeasurement(long j, float f) {
        this.mRecentLightSamples++;
        this.mAmbientLightRingBuffer.prune(j - ((long) this.mAmbientLightHorizon));
        this.mAmbientLightRingBuffer.push(j, f);
        this.mLastObservedLux = f;
        this.mLastObservedLuxTime = j;
    }

    private void adjustLightSensorRate(int i) {
        if (i != this.mCurrentLightSensorRate) {
            this.mCurrentLightSensorRate = i;
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, i * 1000, this.mHandler);
        }
    }

    private boolean setAutoBrightnessAdjustment(float f) {
        return this.mBrightnessMapper.setAutoBrightnessAdjustment(f);
    }

    private void setAmbientLux(float f) {
        if (f < 0.0f) {
            Slog.w(TAG, "Ambient lux was negative, ignoring and setting to 0");
            f = 0.0f;
        }
        this.mAmbientLux = f;
        this.mBrighteningLuxThreshold = this.mHysteresisLevels.getBrighteningThreshold(f);
        this.mDarkeningLuxThreshold = this.mHysteresisLevels.getDarkeningThreshold(f);
        if (!this.mShortTermModelValid && this.mShortTermModelAnchor != -1.0f) {
            float f2 = this.mShortTermModelAnchor - (this.mShortTermModelAnchor * this.SHORT_TERM_MODEL_THRESHOLD_RATIO);
            float f3 = this.mShortTermModelAnchor + (this.mShortTermModelAnchor * this.SHORT_TERM_MODEL_THRESHOLD_RATIO);
            if (f2 < this.mAmbientLux && this.mAmbientLux < f3) {
                this.mShortTermModelValid = true;
                return;
            }
            Slog.d(TAG, "ShortTermModel: reset data, ambient lux is " + this.mAmbientLux + "(" + f2 + ", " + f3 + ")");
            resetShortTermModel();
        }
    }

    private float calculateAmbientLux(long j, long j2) {
        int i;
        int size = this.mAmbientLightRingBuffer.size();
        if (size == 0) {
            Slog.e(TAG, "calculateAmbientLux: No ambient light readings available");
            return -1.0f;
        }
        long j3 = j - j2;
        int i2 = 0;
        int i3 = 0;
        while (true) {
            i = size - 1;
            if (i2 >= i) {
                break;
            }
            i2++;
            if (this.mAmbientLightRingBuffer.getTime(i2) > j3) {
                break;
            }
            i3++;
        }
        float lux = 0.0f;
        long j4 = 100;
        float f = 0.0f;
        while (i >= i3) {
            long time = this.mAmbientLightRingBuffer.getTime(i);
            if (i == i3 && time < j3) {
                time = j3;
            }
            long j5 = time - j;
            float fCalculateWeight = calculateWeight(j5, j4);
            f += fCalculateWeight;
            lux += this.mAmbientLightRingBuffer.getLux(i) * fCalculateWeight;
            i--;
            j4 = j5;
        }
        return lux / f;
    }

    private float calculateWeight(long j, long j2) {
        return weightIntegral(j2) - weightIntegral(j);
    }

    private float weightIntegral(long j) {
        float f = j;
        return f * ((0.5f * f) + this.mWeightingIntercept);
    }

    private long nextAmbientLightBrighteningTransition(long j) {
        for (int size = this.mAmbientLightRingBuffer.size() - 1; size >= 0 && this.mAmbientLightRingBuffer.getLux(size) > this.mBrighteningLuxThreshold; size--) {
            j = this.mAmbientLightRingBuffer.getTime(size);
        }
        return j + this.mBrighteningLightDebounceConfig;
    }

    private long nextAmbientLightDarkeningTransition(long j) {
        for (int size = this.mAmbientLightRingBuffer.size() - 1; size >= 0 && this.mAmbientLightRingBuffer.getLux(size) < this.mDarkeningLuxThreshold; size--) {
            j = this.mAmbientLightRingBuffer.getTime(size);
        }
        return j + this.mDarkeningLightDebounceConfig;
    }

    private void updateAmbientLux() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.mAmbientLightRingBuffer.prune(jUptimeMillis - ((long) this.mAmbientLightHorizon));
        updateAmbientLux(jUptimeMillis);
    }

    private void updateAmbientLux(long j) {
        if (!this.mAmbientLuxValid) {
            long j2 = ((long) this.mLightSensorWarmUpTimeConfig) + this.mLightSensorEnableTime;
            if (j < j2) {
                this.mHandler.sendEmptyMessageAtTime(1, j2);
                return;
            } else {
                setAmbientLux(calculateAmbientLux(j, 2000L));
                this.mAmbientLuxValid = true;
                updateAutoBrightness(true);
            }
        }
        long jNextAmbientLightBrighteningTransition = nextAmbientLightBrighteningTransition(j);
        long jNextAmbientLightDarkeningTransition = nextAmbientLightDarkeningTransition(j);
        float fCalculateAmbientLux = calculateAmbientLux(j, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        float fCalculateAmbientLux2 = calculateAmbientLux(j, 2000L);
        if ((fCalculateAmbientLux >= this.mBrighteningLuxThreshold && fCalculateAmbientLux2 >= this.mBrighteningLuxThreshold && jNextAmbientLightBrighteningTransition <= j) || (fCalculateAmbientLux <= this.mDarkeningLuxThreshold && fCalculateAmbientLux2 <= this.mDarkeningLuxThreshold && jNextAmbientLightDarkeningTransition <= j)) {
            setAmbientLux(fCalculateAmbientLux2);
            updateAutoBrightness(true);
            jNextAmbientLightBrighteningTransition = nextAmbientLightBrighteningTransition(j);
            jNextAmbientLightDarkeningTransition = nextAmbientLightDarkeningTransition(j);
        }
        long jMin = Math.min(jNextAmbientLightDarkeningTransition, jNextAmbientLightBrighteningTransition);
        if (jMin <= j) {
            jMin = ((long) this.mNormalLightSensorRate) + j;
        }
        this.mHandler.sendEmptyMessageAtTime(1, jMin);
    }

    private void updateAutoBrightness(boolean z) {
        int iClampScreenBrightness;
        if (this.mAmbientLuxValid && this.mScreenAutoBrightness != (iClampScreenBrightness = clampScreenBrightness(Math.round(this.mBrightnessMapper.getBrightness(this.mAmbientLux) * 255.0f)))) {
            this.mScreenAutoBrightness = iClampScreenBrightness;
            if (z) {
                this.mCallbacks.updateBrightness();
            }
        }
    }

    private int clampScreenBrightness(int i) {
        return MathUtils.constrain(i, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void prepareBrightnessAdjustmentSample() {
        if (!this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = true;
            this.mBrightnessAdjustmentSampleOldLux = this.mAmbientLuxValid ? this.mAmbientLux : -1.0f;
            this.mBrightnessAdjustmentSampleOldBrightness = this.mScreenAutoBrightness;
        } else {
            this.mHandler.removeMessages(2);
        }
        this.mHandler.sendEmptyMessageDelayed(2, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private void cancelBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            this.mHandler.removeMessages(2);
        }
    }

    private void collectBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            if (this.mAmbientLuxValid && this.mScreenAutoBrightness >= 0) {
                EventLog.writeEvent(EventLogTags.AUTO_BRIGHTNESS_ADJ, Float.valueOf(this.mBrightnessAdjustmentSampleOldLux), Integer.valueOf(this.mBrightnessAdjustmentSampleOldBrightness), Float.valueOf(this.mAmbientLux), Integer.valueOf(this.mScreenAutoBrightness));
            }
        }
    }

    private final class AutomaticBrightnessHandler extends Handler {
        public AutomaticBrightnessHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    AutomaticBrightnessController.this.updateAmbientLux();
                    break;
                case 2:
                    AutomaticBrightnessController.this.collectBrightnessAdjustmentSample();
                    break;
                case 3:
                    AutomaticBrightnessController.this.invalidateShortTermModel();
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

        public AmbientLightRingBuffer(long j, int i) {
            this.mCapacity = (int) Math.ceil((i * BUFFER_SLACK) / j);
            this.mRingLux = new float[this.mCapacity];
            this.mRingTime = new long[this.mCapacity];
        }

        public float getLux(int i) {
            return this.mRingLux[offsetOf(i)];
        }

        public long getTime(int i) {
            return this.mRingTime[offsetOf(i)];
        }

        public void push(long j, float f) {
            int i = this.mEnd;
            if (this.mCount == this.mCapacity) {
                int i2 = this.mCapacity * 2;
                float[] fArr = new float[i2];
                long[] jArr = new long[i2];
                int i3 = this.mCapacity - this.mStart;
                System.arraycopy(this.mRingLux, this.mStart, fArr, 0, i3);
                System.arraycopy(this.mRingTime, this.mStart, jArr, 0, i3);
                if (this.mStart != 0) {
                    System.arraycopy(this.mRingLux, 0, fArr, i3, this.mStart);
                    System.arraycopy(this.mRingTime, 0, jArr, i3, this.mStart);
                }
                this.mRingLux = fArr;
                this.mRingTime = jArr;
                int i4 = this.mCapacity;
                this.mCapacity = i2;
                this.mStart = 0;
                i = i4;
            }
            this.mRingTime[i] = j;
            this.mRingLux[i] = f;
            this.mEnd = i + 1;
            if (this.mEnd == this.mCapacity) {
                this.mEnd = 0;
            }
            this.mCount++;
        }

        public void prune(long j) {
            if (this.mCount == 0) {
                return;
            }
            while (this.mCount > 1) {
                int i = this.mStart + 1;
                if (i >= this.mCapacity) {
                    i -= this.mCapacity;
                }
                if (this.mRingTime[i] > j) {
                    break;
                }
                this.mStart = i;
                this.mCount--;
            }
            if (this.mRingTime[this.mStart] < j) {
                this.mRingTime[this.mStart] = j;
            }
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
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append('[');
            int i = 0;
            while (i < this.mCount) {
                int i2 = i + 1;
                long time = i2 < this.mCount ? getTime(i2) : SystemClock.uptimeMillis();
                if (i != 0) {
                    stringBuffer.append(", ");
                }
                stringBuffer.append(getLux(i));
                stringBuffer.append(" / ");
                stringBuffer.append(time - getTime(i));
                stringBuffer.append("ms");
                i = i2;
            }
            stringBuffer.append(']');
            return stringBuffer.toString();
        }

        private int offsetOf(int i) {
            if (i >= this.mCount || i < 0) {
                throw new ArrayIndexOutOfBoundsException(i);
            }
            int i2 = i + this.mStart;
            if (i2 >= this.mCapacity) {
                return i2 - this.mCapacity;
            }
            return i2;
        }
    }
}
