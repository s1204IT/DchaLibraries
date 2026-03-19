package com.android.server.policy;

import android.R;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.PrintWriter;

public abstract class WindowOrientationListener {
    private static final boolean LOG = SystemProperties.getBoolean("debug.orientation.log", false);
    private static final String TAG = "WindowOrientationListener";
    private static final boolean USE_GRAVITY_SENSOR = false;
    private int mCurrentRotation;
    private boolean mEnabled;
    private Handler mHandler;
    private final Object mLock;
    private OrientationJudge mOrientationJudge;
    private int mRate;
    private Sensor mSensor;
    private SensorManager mSensorManager;
    private String mSensorType;

    public abstract void onProposedRotationChanged(int i);

    public WindowOrientationListener(Context context, Handler handler) {
        this(context, handler, 2);
    }

    private WindowOrientationListener(Context context, Handler handler, int rate) {
        this.mCurrentRotation = -1;
        this.mLock = new Object();
        this.mHandler = handler;
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mRate = rate;
        this.mSensor = this.mSensorManager.getDefaultSensor(27);
        if (this.mSensor != null) {
            this.mOrientationJudge = new OrientationSensorJudge();
        }
        if (this.mOrientationJudge == null) {
            this.mSensor = this.mSensorManager.getDefaultSensor(1);
            if (this.mSensor != null) {
                this.mOrientationJudge = new AccelSensorJudge(context);
            }
        }
        Slog.d(TAG, "ctor: " + this);
    }

    public void enable() {
        synchronized (this.mLock) {
            if (this.mSensor == null) {
                Slog.w(TAG, "Cannot detect sensors. Not enabled");
                return;
            }
            if (!this.mEnabled) {
                Slog.d(TAG, "WindowOrientationListener enabled");
                this.mOrientationJudge.resetLocked();
                this.mSensorManager.registerListener(this.mOrientationJudge, this.mSensor, this.mRate, this.mHandler);
                this.mEnabled = true;
            }
        }
    }

    public void disable() {
        synchronized (this.mLock) {
            if (this.mSensor == null) {
                Slog.w(TAG, "Cannot detect sensors. Invalid disable");
                return;
            }
            if (this.mEnabled) {
                Slog.d(TAG, "WindowOrientationListener disabled");
                this.mSensorManager.unregisterListener(this.mOrientationJudge);
                this.mEnabled = false;
            }
        }
    }

    public void onTouchStart() {
        synchronized (this.mLock) {
            if (this.mOrientationJudge != null) {
                this.mOrientationJudge.onTouchStartLocked();
            }
        }
    }

    public void onTouchEnd() {
        long whenElapsedNanos = SystemClock.elapsedRealtimeNanos();
        synchronized (this.mLock) {
            if (this.mOrientationJudge != null) {
                this.mOrientationJudge.onTouchEndLocked(whenElapsedNanos);
            }
        }
    }

    public void setCurrentRotation(int rotation) {
        synchronized (this.mLock) {
            this.mCurrentRotation = rotation;
        }
    }

    public int getProposedRotation() {
        synchronized (this.mLock) {
            if (this.mEnabled) {
                return this.mOrientationJudge.getProposedRotationLocked();
            }
            return -1;
        }
    }

    public boolean canDetectOrientation() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSensor != null;
        }
        return z;
    }

    public void dump(PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            pw.println(prefix + TAG);
            String prefix2 = prefix + "  ";
            pw.println(prefix2 + "mEnabled=" + this.mEnabled);
            pw.println(prefix2 + "mCurrentRotation=" + this.mCurrentRotation);
            pw.println(prefix2 + "mSensorType=" + this.mSensorType);
            pw.println(prefix2 + "mSensor=" + this.mSensor);
            pw.println(prefix2 + "mRate=" + this.mRate);
            if (this.mOrientationJudge != null) {
                this.mOrientationJudge.dumpLocked(pw, prefix2);
            }
        }
    }

    abstract class OrientationJudge implements SensorEventListener {
        protected static final float MILLIS_PER_NANO = 1.0E-6f;
        protected static final long NANOS_PER_MS = 1000000;
        protected static final long PROPOSAL_MIN_TIME_SINCE_TOUCH_END_NANOS = 500000000;

        public abstract void dumpLocked(PrintWriter printWriter, String str);

        public abstract int getProposedRotationLocked();

        @Override
        public abstract void onAccuracyChanged(Sensor sensor, int i);

        @Override
        public abstract void onSensorChanged(SensorEvent sensorEvent);

        public abstract void onTouchEndLocked(long j);

        public abstract void onTouchStartLocked();

        public abstract void resetLocked();

        OrientationJudge() {
        }
    }

    final class AccelSensorJudge extends OrientationJudge {
        private static final float ACCELERATION_TOLERANCE = 4.0f;
        private static final int ACCELEROMETER_DATA_X = 0;
        private static final int ACCELEROMETER_DATA_Y = 1;
        private static final int ACCELEROMETER_DATA_Z = 2;
        private static final int ADJACENT_ORIENTATION_ANGLE_GAP = 45;
        private static final float FILTER_TIME_CONSTANT_MS = 200.0f;
        private static final float FLAT_ANGLE = 80.0f;
        private static final long FLAT_TIME_NANOS = 1000000000;
        private static final float MAX_ACCELERATION_MAGNITUDE = 13.80665f;
        private static final long MAX_FILTER_DELTA_TIME_NANOS = 1000000000;
        private static final int MAX_TILT = 80;
        private static final float MIN_ACCELERATION_MAGNITUDE = 5.80665f;
        private static final float NEAR_ZERO_MAGNITUDE = 1.0f;
        private static final long PROPOSAL_MIN_TIME_SINCE_ACCELERATION_ENDED_NANOS = 500000000;
        private static final long PROPOSAL_MIN_TIME_SINCE_FLAT_ENDED_NANOS = 500000000;
        private static final long PROPOSAL_MIN_TIME_SINCE_SWING_ENDED_NANOS = 300000000;
        private static final long PROPOSAL_SETTLE_TIME_NANOS = 40000000;
        private static final float RADIANS_TO_DEGREES = 57.29578f;
        private static final float SWING_AWAY_ANGLE_DELTA = 20.0f;
        private static final long SWING_TIME_NANOS = 300000000;
        private static final int TILT_HISTORY_SIZE = 200;
        private static final int TILT_OVERHEAD_ENTER = -40;
        private static final int TILT_OVERHEAD_EXIT = -15;
        private boolean mAccelerating;
        private long mAccelerationTimestampNanos;
        private boolean mFlat;
        private long mFlatTimestampNanos;
        private long mLastFilteredTimestampNanos;
        private float mLastFilteredX;
        private float mLastFilteredY;
        private float mLastFilteredZ;
        private boolean mOverhead;
        private int mPredictedRotation;
        private long mPredictedRotationTimestampNanos;
        private int mProposedRotation;
        private long mSwingTimestampNanos;
        private boolean mSwinging;
        private float[] mTiltHistory;
        private int mTiltHistoryIndex;
        private long[] mTiltHistoryTimestampNanos;
        private final int[][] mTiltToleranceConfig;
        private long mTouchEndedTimestampNanos;
        private boolean mTouched;

        public AccelSensorJudge(Context context) {
            super();
            this.mTiltToleranceConfig = new int[][]{new int[]{-25, 70}, new int[]{-25, 65}, new int[]{-25, 60}, new int[]{-25, 65}};
            this.mTouchEndedTimestampNanos = Long.MIN_VALUE;
            this.mTiltHistory = new float[TILT_HISTORY_SIZE];
            this.mTiltHistoryTimestampNanos = new long[TILT_HISTORY_SIZE];
            int[] tiltTolerance = context.getResources().getIntArray(R.array.config_autoRotationTiltTolerance);
            if (tiltTolerance.length == 8) {
                for (int i = 0; i < 4; i++) {
                    int min = tiltTolerance[i * 2];
                    int max = tiltTolerance[(i * 2) + 1];
                    if (min >= -90 && min <= max && max <= 90) {
                        this.mTiltToleranceConfig[i][0] = min;
                        this.mTiltToleranceConfig[i][1] = max;
                    } else {
                        Slog.wtf(WindowOrientationListener.TAG, "config_autoRotationTiltTolerance contains invalid range: min=" + min + ", max=" + max);
                    }
                }
                return;
            }
            Slog.wtf(WindowOrientationListener.TAG, "config_autoRotationTiltTolerance should have exactly 8 elements");
        }

        @Override
        public int getProposedRotationLocked() {
            return this.mProposedRotation;
        }

        @Override
        public void dumpLocked(PrintWriter pw, String prefix) {
            pw.println(prefix + "AccelSensorJudge");
            String prefix2 = prefix + "  ";
            pw.println(prefix2 + "mProposedRotation=" + this.mProposedRotation);
            pw.println(prefix2 + "mPredictedRotation=" + this.mPredictedRotation);
            pw.println(prefix2 + "mLastFilteredX=" + this.mLastFilteredX);
            pw.println(prefix2 + "mLastFilteredY=" + this.mLastFilteredY);
            pw.println(prefix2 + "mLastFilteredZ=" + this.mLastFilteredZ);
            long delta = SystemClock.elapsedRealtimeNanos() - this.mLastFilteredTimestampNanos;
            pw.println(prefix2 + "mLastFilteredTimestampNanos=" + this.mLastFilteredTimestampNanos + " (" + (delta * 1.0E-6f) + "ms ago)");
            pw.println(prefix2 + "mTiltHistory={last: " + getLastTiltLocked() + "}");
            pw.println(prefix2 + "mFlat=" + this.mFlat);
            pw.println(prefix2 + "mSwinging=" + this.mSwinging);
            pw.println(prefix2 + "mAccelerating=" + this.mAccelerating);
            pw.println(prefix2 + "mOverhead=" + this.mOverhead);
            pw.println(prefix2 + "mTouched=" + this.mTouched);
            pw.print(prefix2 + "mTiltToleranceConfig=[");
            for (int i = 0; i < 4; i++) {
                if (i != 0) {
                    pw.print(", ");
                }
                pw.print("[");
                pw.print(this.mTiltToleranceConfig[i][0]);
                pw.print(", ");
                pw.print(this.mTiltToleranceConfig[i][1]);
                pw.print("]");
            }
            pw.println("]");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            boolean skipSample;
            int oldProposedRotation;
            int proposedRotation;
            synchronized (WindowOrientationListener.this.mLock) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                if (WindowOrientationListener.LOG) {
                    Slog.v(WindowOrientationListener.TAG, "Raw acceleration vector: x=" + x + ", y=" + y + ", z=" + z + ", magnitude=" + Math.sqrt((x * x) + (y * y) + (z * z)));
                }
                long now = event.timestamp;
                long then = this.mLastFilteredTimestampNanos;
                float timeDeltaMS = (now - then) * 1.0E-6f;
                if (now < then || now > 1000000000 + then || (x == 0.0f && y == 0.0f && z == 0.0f)) {
                    if (WindowOrientationListener.LOG) {
                        Slog.v(WindowOrientationListener.TAG, "Resetting orientation listener.");
                    }
                    resetLocked();
                    skipSample = true;
                } else {
                    float alpha = timeDeltaMS / (FILTER_TIME_CONSTANT_MS + timeDeltaMS);
                    x = ((x - this.mLastFilteredX) * alpha) + this.mLastFilteredX;
                    y = ((y - this.mLastFilteredY) * alpha) + this.mLastFilteredY;
                    z = ((z - this.mLastFilteredZ) * alpha) + this.mLastFilteredZ;
                    if (WindowOrientationListener.LOG) {
                        Slog.v(WindowOrientationListener.TAG, "Filtered acceleration vector: x=" + x + ", y=" + y + ", z=" + z + ", magnitude=" + Math.sqrt((x * x) + (y * y) + (z * z)));
                    }
                    skipSample = false;
                }
                this.mLastFilteredTimestampNanos = now;
                this.mLastFilteredX = x;
                this.mLastFilteredY = y;
                this.mLastFilteredZ = z;
                boolean isAccelerating = false;
                boolean isFlat = false;
                boolean isSwinging = false;
                if (!skipSample) {
                    float magnitude = (float) Math.sqrt((x * x) + (y * y) + (z * z));
                    if (magnitude < NEAR_ZERO_MAGNITUDE) {
                        if (WindowOrientationListener.LOG) {
                            Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, magnitude too close to zero.");
                        }
                        clearPredictedRotationLocked();
                    } else {
                        if (isAcceleratingLocked(magnitude)) {
                            isAccelerating = true;
                            this.mAccelerationTimestampNanos = now;
                        }
                        int tiltAngle = (int) Math.round(Math.asin(z / magnitude) * 57.295780181884766d);
                        addTiltHistoryEntryLocked(now, tiltAngle);
                        if (isFlatLocked(now)) {
                            isFlat = true;
                            this.mFlatTimestampNanos = now;
                        }
                        if (isSwingingLocked(now, tiltAngle)) {
                            isSwinging = true;
                            this.mSwingTimestampNanos = now;
                        }
                        if (tiltAngle <= TILT_OVERHEAD_ENTER) {
                            this.mOverhead = true;
                        } else if (tiltAngle >= TILT_OVERHEAD_EXIT) {
                            this.mOverhead = false;
                        }
                        if (this.mOverhead) {
                            if (WindowOrientationListener.LOG) {
                                Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, device is overhead: tiltAngle=" + tiltAngle);
                            }
                            clearPredictedRotationLocked();
                        } else if (Math.abs(tiltAngle) > 80) {
                            if (WindowOrientationListener.LOG) {
                                Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, tilt angle too high: tiltAngle=" + tiltAngle);
                            }
                            clearPredictedRotationLocked();
                        } else {
                            int orientationAngle = (int) Math.round((-Math.atan2(-x, y)) * 57.295780181884766d);
                            if (orientationAngle < 0) {
                                orientationAngle += 360;
                            }
                            int nearestRotation = (orientationAngle + ADJACENT_ORIENTATION_ANGLE_GAP) / 90;
                            if (nearestRotation == 4) {
                                nearestRotation = 0;
                            }
                            if (isTiltAngleAcceptableLocked(nearestRotation, tiltAngle) && isOrientationAngleAcceptableLocked(nearestRotation, orientationAngle)) {
                                updatePredictedRotationLocked(now, nearestRotation);
                                if (WindowOrientationListener.LOG) {
                                    Slog.v(WindowOrientationListener.TAG, "Predicted: tiltAngle=" + tiltAngle + ", orientationAngle=" + orientationAngle + ", predictedRotation=" + this.mPredictedRotation + ", predictedRotationAgeMS=" + ((now - this.mPredictedRotationTimestampNanos) * 1.0E-6f));
                                }
                            } else {
                                if (WindowOrientationListener.LOG) {
                                    Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, no predicted rotation: tiltAngle=" + tiltAngle + ", orientationAngle=" + orientationAngle);
                                }
                                clearPredictedRotationLocked();
                            }
                        }
                    }
                }
                this.mFlat = isFlat;
                this.mSwinging = isSwinging;
                this.mAccelerating = isAccelerating;
                oldProposedRotation = this.mProposedRotation;
                if (this.mPredictedRotation < 0 || isPredictedRotationAcceptableLocked(now)) {
                    this.mProposedRotation = this.mPredictedRotation;
                }
                proposedRotation = this.mProposedRotation;
                if (WindowOrientationListener.LOG) {
                    Slog.v(WindowOrientationListener.TAG, "Result: currentRotation=" + WindowOrientationListener.this.mCurrentRotation + ", proposedRotation=" + proposedRotation + ", predictedRotation=" + this.mPredictedRotation + ", timeDeltaMS=" + timeDeltaMS + ", isAccelerating=" + isAccelerating + ", isFlat=" + isFlat + ", isSwinging=" + isSwinging + ", isOverhead=" + this.mOverhead + ", isTouched=" + this.mTouched + ", timeUntilSettledMS=" + remainingMS(now, this.mPredictedRotationTimestampNanos + PROPOSAL_SETTLE_TIME_NANOS) + ", timeUntilAccelerationDelayExpiredMS=" + remainingMS(now, this.mAccelerationTimestampNanos + 500000000) + ", timeUntilFlatDelayExpiredMS=" + remainingMS(now, this.mFlatTimestampNanos + 500000000) + ", timeUntilSwingDelayExpiredMS=" + remainingMS(now, this.mSwingTimestampNanos + 300000000) + ", timeUntilTouchDelayExpiredMS=" + remainingMS(now, this.mTouchEndedTimestampNanos + 500000000));
                }
            }
            if (proposedRotation == oldProposedRotation || proposedRotation < 0) {
                return;
            }
            Slog.v(WindowOrientationListener.TAG, "Proposed rotation changed!  proposedRotation=" + proposedRotation + ", oldProposedRotation=" + oldProposedRotation);
            WindowOrientationListener.this.onProposedRotationChanged(proposedRotation);
        }

        @Override
        public void onTouchStartLocked() {
            this.mTouched = true;
        }

        @Override
        public void onTouchEndLocked(long whenElapsedNanos) {
            this.mTouched = false;
            this.mTouchEndedTimestampNanos = whenElapsedNanos;
        }

        @Override
        public void resetLocked() {
            this.mLastFilteredTimestampNanos = Long.MIN_VALUE;
            this.mProposedRotation = -1;
            this.mFlatTimestampNanos = Long.MIN_VALUE;
            this.mFlat = false;
            this.mSwingTimestampNanos = Long.MIN_VALUE;
            this.mSwinging = false;
            this.mAccelerationTimestampNanos = Long.MIN_VALUE;
            this.mAccelerating = false;
            this.mOverhead = false;
            clearPredictedRotationLocked();
            clearTiltHistoryLocked();
        }

        private boolean isTiltAngleAcceptableLocked(int rotation, int tiltAngle) {
            return tiltAngle >= this.mTiltToleranceConfig[rotation][0] && tiltAngle <= this.mTiltToleranceConfig[rotation][1];
        }

        private boolean isOrientationAngleAcceptableLocked(int rotation, int orientationAngle) {
            int currentRotation = WindowOrientationListener.this.mCurrentRotation;
            if (currentRotation >= 0) {
                if (rotation == currentRotation || rotation == (currentRotation + 1) % 4) {
                    int lowerBound = ((rotation * 90) - 45) + 22;
                    if (rotation == 0) {
                        if (orientationAngle >= 315 && orientationAngle < lowerBound + 360) {
                            return false;
                        }
                    } else if (orientationAngle < lowerBound) {
                        return false;
                    }
                }
                if (rotation == currentRotation || rotation == (currentRotation + 3) % 4) {
                    int upperBound = ((rotation * 90) + ADJACENT_ORIENTATION_ANGLE_GAP) - 22;
                    return rotation == 0 ? orientationAngle > ADJACENT_ORIENTATION_ANGLE_GAP || orientationAngle <= upperBound : orientationAngle <= upperBound;
                }
                return true;
            }
            return true;
        }

        private boolean isPredictedRotationAcceptableLocked(long now) {
            return now >= this.mPredictedRotationTimestampNanos + PROPOSAL_SETTLE_TIME_NANOS && now >= this.mFlatTimestampNanos + 500000000 && now >= this.mSwingTimestampNanos + 300000000 && now >= this.mAccelerationTimestampNanos + 500000000 && !this.mTouched && now >= this.mTouchEndedTimestampNanos + 500000000;
        }

        private void clearPredictedRotationLocked() {
            this.mPredictedRotation = -1;
            this.mPredictedRotationTimestampNanos = Long.MIN_VALUE;
        }

        private void updatePredictedRotationLocked(long now, int rotation) {
            if (this.mPredictedRotation == rotation) {
                return;
            }
            this.mPredictedRotation = rotation;
            this.mPredictedRotationTimestampNanos = now;
        }

        private boolean isAcceleratingLocked(float magnitude) {
            return magnitude < MIN_ACCELERATION_MAGNITUDE || magnitude > MAX_ACCELERATION_MAGNITUDE;
        }

        private void clearTiltHistoryLocked() {
            this.mTiltHistoryTimestampNanos[0] = Long.MIN_VALUE;
            this.mTiltHistoryIndex = 1;
        }

        private void addTiltHistoryEntryLocked(long now, float tilt) {
            this.mTiltHistory[this.mTiltHistoryIndex] = tilt;
            this.mTiltHistoryTimestampNanos[this.mTiltHistoryIndex] = now;
            this.mTiltHistoryIndex = (this.mTiltHistoryIndex + 1) % TILT_HISTORY_SIZE;
            this.mTiltHistoryTimestampNanos[this.mTiltHistoryIndex] = Long.MIN_VALUE;
        }

        private boolean isFlatLocked(long now) {
            int i = this.mTiltHistoryIndex;
            do {
                i = nextTiltHistoryIndexLocked(i);
                if (i < 0 || this.mTiltHistory[i] < FLAT_ANGLE) {
                    return false;
                }
            } while (this.mTiltHistoryTimestampNanos[i] + 1000000000 > now);
            return true;
        }

        private boolean isSwingingLocked(long now, float tilt) {
            int i = this.mTiltHistoryIndex;
            do {
                i = nextTiltHistoryIndexLocked(i);
                if (i < 0 || this.mTiltHistoryTimestampNanos[i] + 300000000 < now) {
                    return false;
                }
            } while (this.mTiltHistory[i] + SWING_AWAY_ANGLE_DELTA > tilt);
            return true;
        }

        private int nextTiltHistoryIndexLocked(int index) {
            if (index == 0) {
                index = TILT_HISTORY_SIZE;
            }
            int index2 = index - 1;
            if (this.mTiltHistoryTimestampNanos[index2] != Long.MIN_VALUE) {
                return index2;
            }
            return -1;
        }

        private float getLastTiltLocked() {
            int index = nextTiltHistoryIndexLocked(this.mTiltHistoryIndex);
            if (index >= 0) {
                return this.mTiltHistory[index];
            }
            return Float.NaN;
        }

        private float remainingMS(long now, long until) {
            if (now >= until) {
                return 0.0f;
            }
            return (until - now) * 1.0E-6f;
        }
    }

    final class OrientationSensorJudge extends OrientationJudge {
        private int mDesiredRotation;
        private int mProposedRotation;
        private boolean mRotationEvaluationScheduled;
        private Runnable mRotationEvaluator;
        private long mTouchEndedTimestampNanos;
        private boolean mTouching;

        OrientationSensorJudge() {
            super();
            this.mTouchEndedTimestampNanos = Long.MIN_VALUE;
            this.mProposedRotation = -1;
            this.mDesiredRotation = -1;
            this.mRotationEvaluator = new Runnable() {
                @Override
                public void run() {
                    int newRotation;
                    synchronized (WindowOrientationListener.this.mLock) {
                        OrientationSensorJudge.this.mRotationEvaluationScheduled = false;
                        newRotation = OrientationSensorJudge.this.evaluateRotationChangeLocked();
                    }
                    if (newRotation < 0) {
                        return;
                    }
                    WindowOrientationListener.this.onProposedRotationChanged(newRotation);
                }
            };
        }

        @Override
        public int getProposedRotationLocked() {
            return this.mProposedRotation;
        }

        @Override
        public void onTouchStartLocked() {
            this.mTouching = true;
        }

        @Override
        public void onTouchEndLocked(long whenElapsedNanos) {
            this.mTouching = false;
            this.mTouchEndedTimestampNanos = whenElapsedNanos;
            if (this.mDesiredRotation == this.mProposedRotation) {
                return;
            }
            long now = SystemClock.elapsedRealtimeNanos();
            scheduleRotationEvaluationIfNecessaryLocked(now);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            int newRotation;
            synchronized (WindowOrientationListener.this.mLock) {
                this.mDesiredRotation = (int) event.values[0];
                newRotation = evaluateRotationChangeLocked();
            }
            if (newRotation < 0) {
                return;
            }
            WindowOrientationListener.this.onProposedRotationChanged(newRotation);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void dumpLocked(PrintWriter pw, String prefix) {
            pw.println(prefix + "OrientationSensorJudge");
            String prefix2 = prefix + "  ";
            pw.println(prefix2 + "mDesiredRotation=" + this.mDesiredRotation);
            pw.println(prefix2 + "mProposedRotation=" + this.mProposedRotation);
            pw.println(prefix2 + "mTouching=" + this.mTouching);
            pw.println(prefix2 + "mTouchEndedTimestampNanos=" + this.mTouchEndedTimestampNanos);
        }

        @Override
        public void resetLocked() {
            this.mProposedRotation = -1;
            this.mDesiredRotation = -1;
            this.mTouching = false;
            this.mTouchEndedTimestampNanos = Long.MIN_VALUE;
            unscheduleRotationEvaluationLocked();
        }

        public int evaluateRotationChangeLocked() {
            unscheduleRotationEvaluationLocked();
            if (this.mDesiredRotation == this.mProposedRotation) {
                return -1;
            }
            long now = SystemClock.elapsedRealtimeNanos();
            if (isDesiredRotationAcceptableLocked(now)) {
                this.mProposedRotation = this.mDesiredRotation;
                return this.mProposedRotation;
            }
            scheduleRotationEvaluationIfNecessaryLocked(now);
            return -1;
        }

        private boolean isDesiredRotationAcceptableLocked(long now) {
            return !this.mTouching && now >= this.mTouchEndedTimestampNanos + 500000000;
        }

        private void scheduleRotationEvaluationIfNecessaryLocked(long now) {
            if (this.mRotationEvaluationScheduled || this.mDesiredRotation == this.mProposedRotation) {
                if (WindowOrientationListener.LOG) {
                    Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, an evaluation is already scheduled or is unnecessary.");
                }
            } else {
                if (this.mTouching) {
                    if (WindowOrientationListener.LOG) {
                        Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, user is still touching the screen.");
                        return;
                    }
                    return;
                }
                long timeOfNextPossibleRotationNanos = this.mTouchEndedTimestampNanos + 500000000;
                if (now >= timeOfNextPossibleRotationNanos) {
                    if (WindowOrientationListener.LOG) {
                        Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, already past the next possible time of rotation.");
                    }
                } else {
                    long delayMs = (long) Math.ceil((timeOfNextPossibleRotationNanos - now) * 1.0E-6f);
                    WindowOrientationListener.this.mHandler.postDelayed(this.mRotationEvaluator, delayMs);
                    this.mRotationEvaluationScheduled = true;
                }
            }
        }

        private void unscheduleRotationEvaluationLocked() {
            if (!this.mRotationEvaluationScheduled) {
                return;
            }
            WindowOrientationListener.this.mHandler.removeCallbacks(this.mRotationEvaluator);
            this.mRotationEvaluationScheduled = false;
        }
    }
}
