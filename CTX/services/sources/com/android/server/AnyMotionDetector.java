package com.android.server;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;

public class AnyMotionDetector {
    private static final long ACCELEROMETER_DATA_TIMEOUT_MILLIS = 3000;
    private static final boolean DEBUG = false;
    private static final long ORIENTATION_MEASUREMENT_DURATION_MILLIS = 2500;
    private static final long ORIENTATION_MEASUREMENT_INTERVAL_MILLIS = 5000;
    public static final int RESULT_MOVED = 1;
    public static final int RESULT_STATIONARY = 0;
    public static final int RESULT_UNKNOWN = -1;
    private static final int SAMPLING_INTERVAL_MILLIS = 40;
    private static final int STALE_MEASUREMENT_TIMEOUT_MILLIS = 120000;
    private static final int STATE_ACTIVE = 1;
    private static final int STATE_INACTIVE = 0;
    private static final String TAG = "AnyMotionDetector";
    private static final long WAKELOCK_TIMEOUT_MILLIS = 30000;
    private Sensor mAccelSensor;
    private DeviceIdleCallback mCallback;
    private final Handler mHandler;
    private boolean mMeasurementInProgress;
    private boolean mMeasurementTimeoutIsActive;
    private int mNumSufficientSamples;
    private RunningSignalStats mRunningStats;
    private SensorManager mSensorManager;
    private boolean mSensorRestartIsActive;
    private int mState;
    private final float mThresholdAngle;
    private PowerManager.WakeLock mWakeLock;
    private boolean mWakelockTimeoutIsActive;
    private final float THRESHOLD_ENERGY = 5.0f;
    private final Object mLock = new Object();
    private Vector3 mCurrentGravityVector = null;
    private Vector3 mPreviousGravityVector = null;
    private final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            int iStopOrientationMeasurementLocked;
            synchronized (AnyMotionDetector.this.mLock) {
                AnyMotionDetector.this.mRunningStats.accumulate(new Vector3(SystemClock.elapsedRealtime(), sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
                if (AnyMotionDetector.this.mRunningStats.getSampleCount() >= AnyMotionDetector.this.mNumSufficientSamples) {
                    iStopOrientationMeasurementLocked = AnyMotionDetector.this.stopOrientationMeasurementLocked();
                } else {
                    iStopOrientationMeasurementLocked = -1;
                }
            }
            if (iStopOrientationMeasurementLocked != -1) {
                AnyMotionDetector.this.mHandler.removeCallbacks(AnyMotionDetector.this.mWakelockTimeout);
                AnyMotionDetector.this.mWakelockTimeoutIsActive = false;
                AnyMotionDetector.this.mCallback.onAnyMotionResult(iStopOrientationMeasurementLocked);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    private final Runnable mSensorRestart = new Runnable() {
        @Override
        public void run() {
            synchronized (AnyMotionDetector.this.mLock) {
                if (AnyMotionDetector.this.mSensorRestartIsActive) {
                    AnyMotionDetector.this.mSensorRestartIsActive = false;
                    AnyMotionDetector.this.startOrientationMeasurementLocked();
                }
            }
        }
    };
    private final Runnable mMeasurementTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (AnyMotionDetector.this.mLock) {
                if (AnyMotionDetector.this.mMeasurementTimeoutIsActive) {
                    AnyMotionDetector.this.mMeasurementTimeoutIsActive = false;
                    int iStopOrientationMeasurementLocked = AnyMotionDetector.this.stopOrientationMeasurementLocked();
                    if (iStopOrientationMeasurementLocked != -1) {
                        AnyMotionDetector.this.mHandler.removeCallbacks(AnyMotionDetector.this.mWakelockTimeout);
                        AnyMotionDetector.this.mWakelockTimeoutIsActive = false;
                        AnyMotionDetector.this.mCallback.onAnyMotionResult(iStopOrientationMeasurementLocked);
                    }
                }
            }
        }
    };
    private final Runnable mWakelockTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (AnyMotionDetector.this.mLock) {
                if (AnyMotionDetector.this.mWakelockTimeoutIsActive) {
                    AnyMotionDetector.this.mWakelockTimeoutIsActive = false;
                    AnyMotionDetector.this.stop();
                }
            }
        }
    };

    interface DeviceIdleCallback {
        void onAnyMotionResult(int i);
    }

    public AnyMotionDetector(PowerManager powerManager, Handler handler, SensorManager sensorManager, DeviceIdleCallback deviceIdleCallback, float f) {
        this.mCallback = null;
        synchronized (this.mLock) {
            this.mWakeLock = powerManager.newWakeLock(1, TAG);
            this.mWakeLock.setReferenceCounted(false);
            this.mHandler = handler;
            this.mSensorManager = sensorManager;
            this.mAccelSensor = this.mSensorManager.getDefaultSensor(1);
            this.mMeasurementInProgress = false;
            this.mMeasurementTimeoutIsActive = false;
            this.mWakelockTimeoutIsActive = false;
            this.mSensorRestartIsActive = false;
            this.mState = 0;
            this.mCallback = deviceIdleCallback;
            this.mThresholdAngle = f;
            this.mRunningStats = new RunningSignalStats();
            this.mNumSufficientSamples = (int) Math.ceil(62.5d);
        }
    }

    public void checkForAnyMotion() {
        if (this.mState != 1) {
            synchronized (this.mLock) {
                this.mState = 1;
                this.mCurrentGravityVector = null;
                this.mPreviousGravityVector = null;
                this.mWakeLock.acquire();
                this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, this.mWakelockTimeout), 30000L);
                this.mWakelockTimeoutIsActive = true;
                startOrientationMeasurementLocked();
            }
        }
    }

    public void stop() {
        synchronized (this.mLock) {
            if (this.mState == 1) {
                this.mState = 0;
            }
            this.mHandler.removeCallbacks(this.mMeasurementTimeout);
            this.mHandler.removeCallbacks(this.mSensorRestart);
            this.mMeasurementTimeoutIsActive = false;
            this.mSensorRestartIsActive = false;
            if (this.mMeasurementInProgress) {
                this.mMeasurementInProgress = false;
                this.mSensorManager.unregisterListener(this.mListener);
            }
            this.mCurrentGravityVector = null;
            this.mPreviousGravityVector = null;
            if (this.mWakeLock.isHeld()) {
                this.mHandler.removeCallbacks(this.mWakelockTimeout);
                this.mWakelockTimeoutIsActive = false;
                this.mWakeLock.release();
            }
        }
    }

    private void startOrientationMeasurementLocked() {
        if (!this.mMeasurementInProgress && this.mAccelSensor != null) {
            if (this.mSensorManager.registerListener(this.mListener, this.mAccelSensor, EventLogTags.VOLUME_CHANGED)) {
                this.mMeasurementInProgress = true;
                this.mRunningStats.reset();
            }
            this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, this.mMeasurementTimeout), ACCELEROMETER_DATA_TIMEOUT_MILLIS);
            this.mMeasurementTimeoutIsActive = true;
        }
    }

    private int stopOrientationMeasurementLocked() {
        if (!this.mMeasurementInProgress) {
            return -1;
        }
        this.mHandler.removeCallbacks(this.mMeasurementTimeout);
        this.mMeasurementTimeoutIsActive = false;
        this.mSensorManager.unregisterListener(this.mListener);
        this.mMeasurementInProgress = false;
        this.mPreviousGravityVector = this.mCurrentGravityVector;
        this.mCurrentGravityVector = this.mRunningStats.getRunningAverage();
        if (this.mRunningStats.getSampleCount() == 0) {
            Slog.w(TAG, "No accelerometer data acquired for orientation measurement.");
        }
        this.mRunningStats.reset();
        int stationaryStatus = getStationaryStatus();
        if (stationaryStatus != -1) {
            if (this.mWakeLock.isHeld()) {
                this.mHandler.removeCallbacks(this.mWakelockTimeout);
                this.mWakelockTimeoutIsActive = false;
                this.mWakeLock.release();
            }
            this.mState = 0;
        } else {
            this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, this.mSensorRestart), ORIENTATION_MEASUREMENT_INTERVAL_MILLIS);
            this.mSensorRestartIsActive = true;
        }
        return stationaryStatus;
    }

    public int getStationaryStatus() {
        if (this.mPreviousGravityVector == null || this.mCurrentGravityVector == null) {
            return -1;
        }
        float fAngleBetween = this.mPreviousGravityVector.normalized().angleBetween(this.mCurrentGravityVector.normalized());
        if (fAngleBetween >= this.mThresholdAngle || this.mRunningStats.getEnergy() >= 5.0f) {
            return (!Float.isNaN(fAngleBetween) && this.mCurrentGravityVector.timeMillisSinceBoot - this.mPreviousGravityVector.timeMillisSinceBoot > JobStatus.DEFAULT_TRIGGER_MAX_DELAY) ? -1 : 1;
        }
        return 0;
    }

    public static final class Vector3 {
        public long timeMillisSinceBoot;
        public float x;
        public float y;
        public float z;

        public Vector3(long j, float f, float f2, float f3) {
            this.timeMillisSinceBoot = j;
            this.x = f;
            this.y = f2;
            this.z = f3;
        }

        public float norm() {
            return (float) Math.sqrt(dotProduct(this));
        }

        public Vector3 normalized() {
            float fNorm = norm();
            return new Vector3(this.timeMillisSinceBoot, this.x / fNorm, this.y / fNorm, this.z / fNorm);
        }

        public float angleBetween(Vector3 vector3) {
            float fAbs = Math.abs((float) Math.toDegrees(Math.atan2(cross(vector3).norm(), dotProduct(vector3))));
            Slog.d(AnyMotionDetector.TAG, "angleBetween: this = " + toString() + ", other = " + vector3.toString() + ", degrees = " + fAbs);
            return fAbs;
        }

        public Vector3 cross(Vector3 vector3) {
            return new Vector3(vector3.timeMillisSinceBoot, (this.y * vector3.z) - (this.z * vector3.y), (this.z * vector3.x) - (this.x * vector3.z), (this.x * vector3.y) - (this.y * vector3.x));
        }

        public String toString() {
            return (((BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + "timeMillisSinceBoot=" + this.timeMillisSinceBoot) + " | x=" + this.x) + ", y=" + this.y) + ", z=" + this.z;
        }

        public float dotProduct(Vector3 vector3) {
            return (this.x * vector3.x) + (this.y * vector3.y) + (this.z * vector3.z);
        }

        public Vector3 times(float f) {
            return new Vector3(this.timeMillisSinceBoot, this.x * f, this.y * f, this.z * f);
        }

        public Vector3 plus(Vector3 vector3) {
            return new Vector3(vector3.timeMillisSinceBoot, vector3.x + this.x, vector3.y + this.y, this.z + vector3.z);
        }

        public Vector3 minus(Vector3 vector3) {
            return new Vector3(vector3.timeMillisSinceBoot, this.x - vector3.x, this.y - vector3.y, this.z - vector3.z);
        }
    }

    private static class RunningSignalStats {
        Vector3 currentVector;
        float energy;
        Vector3 previousVector;
        Vector3 runningSum;
        int sampleCount;

        public RunningSignalStats() {
            reset();
        }

        public void reset() {
            this.previousVector = null;
            this.currentVector = null;
            this.runningSum = new Vector3(0L, 0.0f, 0.0f, 0.0f);
            this.energy = 0.0f;
            this.sampleCount = 0;
        }

        public void accumulate(Vector3 vector3) {
            if (vector3 == null) {
                return;
            }
            this.sampleCount++;
            this.runningSum = this.runningSum.plus(vector3);
            this.previousVector = this.currentVector;
            this.currentVector = vector3;
            if (this.previousVector != null) {
                Vector3 vector3Minus = this.currentVector.minus(this.previousVector);
                this.energy += (vector3Minus.x * vector3Minus.x) + (vector3Minus.y * vector3Minus.y) + (vector3Minus.z * vector3Minus.z);
            }
        }

        public Vector3 getRunningAverage() {
            if (this.sampleCount > 0) {
                return this.runningSum.times(1.0f / this.sampleCount);
            }
            return null;
        }

        public float getEnergy() {
            return this.energy;
        }

        public int getSampleCount() {
            return this.sampleCount;
        }

        public String toString() {
            String string = this.currentVector == null ? "null" : this.currentVector.toString();
            return (((BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + "previousVector = " + (this.previousVector == null ? "null" : this.previousVector.toString())) + ", currentVector = " + string) + ", sampleCount = " + this.sampleCount) + ", energy = " + this.energy;
        }
    }
}
