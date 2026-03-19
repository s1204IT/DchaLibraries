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
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.wm.WindowManagerDebugger;
import java.io.PrintWriter;

public abstract class WindowOrientationListener {
    private static final int DEFAULT_BATCH_LATENCY = 100000;
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
    private WindowManagerDebugger mWindowManagerDebugger;

    public abstract void onProposedRotationChanged(int i);

    public WindowOrientationListener(Context context, Handler handler) {
        this(context, handler, 2);
    }

    private WindowOrientationListener(Context context, Handler handler, int i) {
        this.mCurrentRotation = -1;
        this.mLock = new Object();
        this.mWindowManagerDebugger = MtkSystemServiceFactory.getInstance().makeWindowManagerDebugger();
        this.mHandler = handler;
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mRate = i;
        Sensor sensor = null;
        Sensor sensor2 = null;
        for (Sensor sensor3 : this.mSensorManager.getSensorList(27)) {
            if (sensor3.isWakeUpSensor()) {
                sensor = sensor3;
            } else {
                sensor2 = sensor3;
            }
        }
        if (sensor != null) {
            this.mSensor = sensor;
        } else {
            this.mSensor = sensor2;
        }
        if (this.mSensor != null) {
            this.mOrientationJudge = new OrientationSensorJudge();
        }
        if (this.mOrientationJudge == null) {
            this.mSensor = this.mSensorManager.getDefaultSensor(1);
            if (this.mSensor != null) {
                this.mOrientationJudge = new AccelSensorJudge(context);
            }
        }
        WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
        if (WindowManagerDebugger.WMS_DEBUG_USER) {
            Slog.d(TAG, "ctor: " + this);
        }
    }

    public void enable() {
        enable(true);
    }

    public void enable(boolean z) {
        synchronized (this.mLock) {
            if (this.mSensor == null) {
                Slog.w(TAG, "Cannot detect sensors. Not enabled");
                return;
            }
            if (this.mEnabled) {
                return;
            }
            if (!LOG) {
                WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
                if (WindowManagerDebugger.WMS_DEBUG_USER) {
                    Slog.d(TAG, "WindowOrientationListener enabled clearCurrentRotation=" + z);
                }
            }
            this.mOrientationJudge.resetLocked(z);
            if (this.mSensor.getType() == 1) {
                this.mSensorManager.registerListener(this.mOrientationJudge, this.mSensor, this.mRate, DEFAULT_BATCH_LATENCY, this.mHandler);
            } else {
                this.mSensorManager.registerListener(this.mOrientationJudge, this.mSensor, this.mRate, this.mHandler);
            }
            this.mEnabled = true;
        }
    }

    public void disable() {
        synchronized (this.mLock) {
            if (this.mSensor == null) {
                Slog.w(TAG, "Cannot detect sensors. Invalid disable");
                return;
            }
            if (this.mEnabled) {
                if (!LOG) {
                    WindowManagerDebugger windowManagerDebugger = this.mWindowManagerDebugger;
                    if (WindowManagerDebugger.WMS_DEBUG_USER) {
                        Slog.d(TAG, "WindowOrientationListener disabled");
                    }
                    this.mSensorManager.unregisterListener(this.mOrientationJudge);
                    this.mEnabled = false;
                }
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
        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        synchronized (this.mLock) {
            if (this.mOrientationJudge != null) {
                this.mOrientationJudge.onTouchEndLocked(jElapsedRealtimeNanos);
            }
        }
    }

    public void setCurrentRotation(int i) {
        synchronized (this.mLock) {
            this.mCurrentRotation = i;
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

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        synchronized (this.mLock) {
            protoOutputStream.write(1133871366145L, this.mEnabled);
            protoOutputStream.write(1159641169922L, this.mCurrentRotation);
        }
        protoOutputStream.end(jStart);
    }

    public void dump(PrintWriter printWriter, String str) {
        synchronized (this.mLock) {
            printWriter.println(str + TAG);
            String str2 = str + "  ";
            printWriter.println(str2 + "mEnabled=" + this.mEnabled);
            printWriter.println(str2 + "mCurrentRotation=" + Surface.rotationToString(this.mCurrentRotation));
            printWriter.println(str2 + "mSensorType=" + this.mSensorType);
            printWriter.println(str2 + "mSensor=" + this.mSensor);
            printWriter.println(str2 + "mRate=" + this.mRate);
            if (this.mOrientationJudge != null) {
                this.mOrientationJudge.dumpLocked(printWriter, str2);
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

        public abstract void resetLocked(boolean z);

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
            this.mTiltHistory = new float[200];
            this.mTiltHistoryTimestampNanos = new long[200];
            int[] intArray = context.getResources().getIntArray(R.array.config_autoBrightnessButtonBacklightValues);
            if (intArray.length == 8) {
                for (int i = 0; i < 4; i++) {
                    int i2 = i * 2;
                    int i3 = intArray[i2];
                    int i4 = intArray[i2 + 1];
                    if (i3 >= -90 && i3 <= i4 && i4 <= 90) {
                        this.mTiltToleranceConfig[i][0] = i3;
                        this.mTiltToleranceConfig[i][1] = i4;
                    } else {
                        Slog.wtf(WindowOrientationListener.TAG, "config_autoRotationTiltTolerance contains invalid range: min=" + i3 + ", max=" + i4);
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
        public void dumpLocked(PrintWriter printWriter, String str) {
            printWriter.println(str + "AccelSensorJudge");
            String str2 = str + "  ";
            printWriter.println(str2 + "mProposedRotation=" + this.mProposedRotation);
            printWriter.println(str2 + "mPredictedRotation=" + this.mPredictedRotation);
            printWriter.println(str2 + "mLastFilteredX=" + this.mLastFilteredX);
            printWriter.println(str2 + "mLastFilteredY=" + this.mLastFilteredY);
            printWriter.println(str2 + "mLastFilteredZ=" + this.mLastFilteredZ);
            printWriter.println(str2 + "mLastFilteredTimestampNanos=" + this.mLastFilteredTimestampNanos + " (" + ((SystemClock.elapsedRealtimeNanos() - this.mLastFilteredTimestampNanos) * 1.0E-6f) + "ms ago)");
            StringBuilder sb = new StringBuilder();
            sb.append(str2);
            sb.append("mTiltHistory={last: ");
            sb.append(getLastTiltLocked());
            sb.append("}");
            printWriter.println(sb.toString());
            printWriter.println(str2 + "mFlat=" + this.mFlat);
            printWriter.println(str2 + "mSwinging=" + this.mSwinging);
            printWriter.println(str2 + "mAccelerating=" + this.mAccelerating);
            printWriter.println(str2 + "mOverhead=" + this.mOverhead);
            printWriter.println(str2 + "mTouched=" + this.mTouched);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str2);
            sb2.append("mTiltToleranceConfig=[");
            printWriter.print(sb2.toString());
            for (int i = 0; i < 4; i++) {
                if (i != 0) {
                    printWriter.print(", ");
                }
                printWriter.print("[");
                printWriter.print(this.mTiltToleranceConfig[i][0]);
                printWriter.print(", ");
                printWriter.print(this.mTiltToleranceConfig[i][1]);
                printWriter.print("]");
            }
            printWriter.println("]");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            Object[] objArr;
            boolean z;
            boolean z2;
            int i;
            int i2;
            boolean z3;
            boolean z4;
            synchronized (WindowOrientationListener.this.mLock) {
                boolean z5 = false;
                int i3 = 0;
                float f = sensorEvent.values[0];
                float f2 = sensorEvent.values[1];
                float f3 = sensorEvent.values[2];
                if (WindowOrientationListener.LOG) {
                    Slog.v(WindowOrientationListener.TAG, "Raw acceleration vector: x=" + f + ", y=" + f2 + ", z=" + f3 + ", magnitude=" + Math.sqrt((f * f) + (f2 * f2) + (f3 * f3)));
                }
                long j = sensorEvent.timestamp;
                long j2 = this.mLastFilteredTimestampNanos;
                float f4 = (j - j2) * 1.0E-6f;
                if (j < j2 || j > j2 + 1000000000 || (f == 0.0f && f2 == 0.0f && f3 == 0.0f)) {
                    if (WindowOrientationListener.LOG) {
                        Slog.v(WindowOrientationListener.TAG, "Resetting orientation listener.");
                    }
                    resetLocked(true);
                    objArr = true;
                } else {
                    float f5 = f4 / (FILTER_TIME_CONSTANT_MS + f4);
                    f = ((f - this.mLastFilteredX) * f5) + this.mLastFilteredX;
                    f2 = ((f2 - this.mLastFilteredY) * f5) + this.mLastFilteredY;
                    f3 = this.mLastFilteredZ + (f5 * (f3 - this.mLastFilteredZ));
                    if (WindowOrientationListener.LOG) {
                        Slog.v(WindowOrientationListener.TAG, "Filtered acceleration vector: x=" + f + ", y=" + f2 + ", z=" + f3 + ", magnitude=" + Math.sqrt((f * f) + (f2 * f2) + (f3 * f3)));
                    }
                    objArr = false;
                }
                this.mLastFilteredTimestampNanos = j;
                this.mLastFilteredX = f;
                this.mLastFilteredY = f2;
                this.mLastFilteredZ = f3;
                if (objArr == false) {
                    float fSqrt = (float) Math.sqrt((f * f) + (f2 * f2) + (f3 * f3));
                    if (fSqrt < 1.0f) {
                        if (WindowOrientationListener.LOG) {
                            Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, magnitude too close to zero.");
                        }
                        clearPredictedRotationLocked();
                        z = false;
                        z2 = false;
                        this.mFlat = z5;
                        this.mSwinging = z;
                        this.mAccelerating = z2;
                        i = this.mProposedRotation;
                        if (this.mPredictedRotation >= 0 || isPredictedRotationAcceptableLocked(j)) {
                            this.mProposedRotation = this.mPredictedRotation;
                        }
                        i2 = this.mProposedRotation;
                        if (WindowOrientationListener.LOG) {
                            Slog.v(WindowOrientationListener.TAG, "Result: currentRotation=" + WindowOrientationListener.this.mCurrentRotation + ", proposedRotation=" + i2 + ", predictedRotation=" + this.mPredictedRotation + ", timeDeltaMS=" + f4 + ", isAccelerating=" + z2 + ", isFlat=" + z5 + ", isSwinging=" + z + ", isOverhead=" + this.mOverhead + ", isTouched=" + this.mTouched + ", timeUntilSettledMS=" + remainingMS(j, this.mPredictedRotationTimestampNanos + PROPOSAL_SETTLE_TIME_NANOS) + ", timeUntilAccelerationDelayExpiredMS=" + remainingMS(j, this.mAccelerationTimestampNanos + 500000000) + ", timeUntilFlatDelayExpiredMS=" + remainingMS(j, this.mFlatTimestampNanos + 500000000) + ", timeUntilSwingDelayExpiredMS=" + remainingMS(j, this.mSwingTimestampNanos + 300000000) + ", timeUntilTouchDelayExpiredMS=" + remainingMS(j, this.mTouchEndedTimestampNanos + 500000000));
                        }
                    } else {
                        if (isAcceleratingLocked(fSqrt)) {
                            this.mAccelerationTimestampNanos = j;
                            z2 = true;
                        } else {
                            z2 = false;
                        }
                        int iRound = (int) Math.round(Math.asin(f3 / fSqrt) * 57.295780181884766d);
                        float f6 = iRound;
                        addTiltHistoryEntryLocked(j, f6);
                        if (isFlatLocked(j)) {
                            this.mFlatTimestampNanos = j;
                            z3 = true;
                        } else {
                            z3 = false;
                        }
                        if (isSwingingLocked(j, f6)) {
                            this.mSwingTimestampNanos = j;
                            z = true;
                        } else {
                            z = false;
                        }
                        if (iRound <= TILT_OVERHEAD_ENTER) {
                            this.mOverhead = true;
                        } else if (iRound >= TILT_OVERHEAD_EXIT) {
                            this.mOverhead = false;
                        }
                        if (this.mOverhead) {
                            if (WindowOrientationListener.LOG) {
                                Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, device is overhead: tiltAngle=" + iRound);
                            }
                            clearPredictedRotationLocked();
                        } else if (Math.abs(iRound) > 80) {
                            if (WindowOrientationListener.LOG) {
                                Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, tilt angle too high: tiltAngle=" + iRound);
                            }
                            clearPredictedRotationLocked();
                        } else {
                            z4 = z3;
                            int iRound2 = (int) Math.round((-Math.atan2(-f, f2)) * 57.295780181884766d);
                            if (iRound2 < 0) {
                                iRound2 += 360;
                            }
                            int i4 = (iRound2 + 45) / 90;
                            if (i4 != 4) {
                                i3 = i4;
                            }
                            if (!isTiltAngleAcceptableLocked(i3, iRound) || !isOrientationAngleAcceptableLocked(i3, iRound2)) {
                                if (WindowOrientationListener.LOG) {
                                    Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, no predicted rotation: tiltAngle=" + iRound + ", orientationAngle=" + iRound2);
                                }
                                clearPredictedRotationLocked();
                            } else {
                                updatePredictedRotationLocked(j, i3);
                                if (WindowOrientationListener.LOG) {
                                    Slog.v(WindowOrientationListener.TAG, "Predicted: tiltAngle=" + iRound + ", orientationAngle=" + iRound2 + ", predictedRotation=" + this.mPredictedRotation + ", predictedRotationAgeMS=" + ((j - this.mPredictedRotationTimestampNanos) * 1.0E-6f));
                                }
                            }
                            z5 = z4;
                            this.mFlat = z5;
                            this.mSwinging = z;
                            this.mAccelerating = z2;
                            i = this.mProposedRotation;
                            if (this.mPredictedRotation >= 0) {
                                this.mProposedRotation = this.mPredictedRotation;
                                i2 = this.mProposedRotation;
                                if (WindowOrientationListener.LOG) {
                                }
                            }
                        }
                        z4 = z3;
                        z5 = z4;
                        this.mFlat = z5;
                        this.mSwinging = z;
                        this.mAccelerating = z2;
                        i = this.mProposedRotation;
                        if (this.mPredictedRotation >= 0) {
                        }
                    }
                } else {
                    z = false;
                    z2 = false;
                    this.mFlat = z5;
                    this.mSwinging = z;
                    this.mAccelerating = z2;
                    i = this.mProposedRotation;
                    if (this.mPredictedRotation >= 0) {
                    }
                }
            }
            if (i2 != i && i2 >= 0) {
                if (!WindowOrientationListener.LOG) {
                    WindowManagerDebugger unused = WindowOrientationListener.this.mWindowManagerDebugger;
                    if (WindowManagerDebugger.WMS_DEBUG_USER) {
                        Slog.v(WindowOrientationListener.TAG, "Proposed rotation changed!  proposedRotation=" + i2 + ", oldProposedRotation=" + i);
                    }
                }
                WindowOrientationListener.this.onProposedRotationChanged(i2);
            }
        }

        @Override
        public void onTouchStartLocked() {
            this.mTouched = true;
        }

        @Override
        public void onTouchEndLocked(long j) {
            this.mTouched = false;
            this.mTouchEndedTimestampNanos = j;
        }

        @Override
        public void resetLocked(boolean z) {
            this.mLastFilteredTimestampNanos = Long.MIN_VALUE;
            if (z) {
                this.mProposedRotation = -1;
            }
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

        private boolean isTiltAngleAcceptableLocked(int i, int i2) {
            return i2 >= this.mTiltToleranceConfig[i][0] && i2 <= this.mTiltToleranceConfig[i][1];
        }

        private boolean isOrientationAngleAcceptableLocked(int i, int i2) {
            int i3 = WindowOrientationListener.this.mCurrentRotation;
            if (i3 >= 0) {
                if (i == i3 || i == (i3 + 1) % 4) {
                    int i4 = ((i * 90) - 45) + 22;
                    if (i == 0) {
                        if (i2 >= 315 && i2 < i4 + 360) {
                            return false;
                        }
                    } else if (i2 < i4) {
                        return false;
                    }
                }
                if (i == i3 || i == (i3 + 3) % 4) {
                    int i5 = ((i * 90) + 45) - 22;
                    return i == 0 ? i2 > 45 || i2 <= i5 : i2 <= i5;
                }
                return true;
            }
            return true;
        }

        private boolean isPredictedRotationAcceptableLocked(long j) {
            return j >= this.mPredictedRotationTimestampNanos + PROPOSAL_SETTLE_TIME_NANOS && j >= this.mFlatTimestampNanos + 500000000 && j >= this.mSwingTimestampNanos + 300000000 && j >= this.mAccelerationTimestampNanos + 500000000 && !this.mTouched && j >= this.mTouchEndedTimestampNanos + 500000000;
        }

        private void clearPredictedRotationLocked() {
            this.mPredictedRotation = -1;
            this.mPredictedRotationTimestampNanos = Long.MIN_VALUE;
        }

        private void updatePredictedRotationLocked(long j, int i) {
            if (this.mPredictedRotation != i) {
                this.mPredictedRotation = i;
                this.mPredictedRotationTimestampNanos = j;
            }
        }

        private boolean isAcceleratingLocked(float f) {
            return f < MIN_ACCELERATION_MAGNITUDE || f > MAX_ACCELERATION_MAGNITUDE;
        }

        private void clearTiltHistoryLocked() {
            this.mTiltHistoryTimestampNanos[0] = Long.MIN_VALUE;
            this.mTiltHistoryIndex = 1;
        }

        private void addTiltHistoryEntryLocked(long j, float f) {
            this.mTiltHistory[this.mTiltHistoryIndex] = f;
            this.mTiltHistoryTimestampNanos[this.mTiltHistoryIndex] = j;
            this.mTiltHistoryIndex = (this.mTiltHistoryIndex + 1) % 200;
            this.mTiltHistoryTimestampNanos[this.mTiltHistoryIndex] = Long.MIN_VALUE;
        }

        private boolean isFlatLocked(long j) {
            int iNextTiltHistoryIndexLocked = this.mTiltHistoryIndex;
            do {
                iNextTiltHistoryIndexLocked = nextTiltHistoryIndexLocked(iNextTiltHistoryIndexLocked);
                if (iNextTiltHistoryIndexLocked < 0 || this.mTiltHistory[iNextTiltHistoryIndexLocked] < FLAT_ANGLE) {
                    return false;
                }
            } while (this.mTiltHistoryTimestampNanos[iNextTiltHistoryIndexLocked] + 1000000000 > j);
            return true;
        }

        private boolean isSwingingLocked(long j, float f) {
            int iNextTiltHistoryIndexLocked = this.mTiltHistoryIndex;
            do {
                iNextTiltHistoryIndexLocked = nextTiltHistoryIndexLocked(iNextTiltHistoryIndexLocked);
                if (iNextTiltHistoryIndexLocked < 0 || this.mTiltHistoryTimestampNanos[iNextTiltHistoryIndexLocked] + 300000000 < j) {
                    return false;
                }
            } while (this.mTiltHistory[iNextTiltHistoryIndexLocked] + SWING_AWAY_ANGLE_DELTA > f);
            return true;
        }

        private int nextTiltHistoryIndexLocked(int i) {
            if (i == 0) {
                i = 200;
            }
            int i2 = i - 1;
            if (this.mTiltHistoryTimestampNanos[i2] != Long.MIN_VALUE) {
                return i2;
            }
            return -1;
        }

        private float getLastTiltLocked() {
            int iNextTiltHistoryIndexLocked = nextTiltHistoryIndexLocked(this.mTiltHistoryIndex);
            if (iNextTiltHistoryIndexLocked >= 0) {
                return this.mTiltHistory[iNextTiltHistoryIndexLocked];
            }
            return Float.NaN;
        }

        private float remainingMS(long j, long j2) {
            if (j >= j2) {
                return 0.0f;
            }
            return (j2 - j) * 1.0E-6f;
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
                    int iEvaluateRotationChangeLocked;
                    synchronized (WindowOrientationListener.this.mLock) {
                        OrientationSensorJudge.this.mRotationEvaluationScheduled = false;
                        iEvaluateRotationChangeLocked = OrientationSensorJudge.this.evaluateRotationChangeLocked();
                    }
                    if (iEvaluateRotationChangeLocked >= 0) {
                        WindowManagerDebugger unused = WindowOrientationListener.this.mWindowManagerDebugger;
                        if (WindowManagerDebugger.WMS_DEBUG_USER) {
                            Slog.v(WindowOrientationListener.TAG, "Proposed rotation changed!  newRotation=" + iEvaluateRotationChangeLocked);
                        }
                        WindowOrientationListener.this.onProposedRotationChanged(iEvaluateRotationChangeLocked);
                    }
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
        public void onTouchEndLocked(long j) {
            this.mTouching = false;
            this.mTouchEndedTimestampNanos = j;
            if (this.mDesiredRotation != this.mProposedRotation) {
                scheduleRotationEvaluationIfNecessaryLocked(SystemClock.elapsedRealtimeNanos());
            }
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            int iEvaluateRotationChangeLocked;
            synchronized (WindowOrientationListener.this.mLock) {
                this.mDesiredRotation = (int) sensorEvent.values[0];
                iEvaluateRotationChangeLocked = evaluateRotationChangeLocked();
            }
            if (iEvaluateRotationChangeLocked >= 0) {
                WindowManagerDebugger unused = WindowOrientationListener.this.mWindowManagerDebugger;
                if (WindowManagerDebugger.WMS_DEBUG_USER) {
                    Slog.v(WindowOrientationListener.TAG, "Proposed rotation changed!  mDesiredRotation=" + this.mDesiredRotation + ", newRotation=" + iEvaluateRotationChangeLocked);
                }
                WindowOrientationListener.this.onProposedRotationChanged(iEvaluateRotationChangeLocked);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        @Override
        public void dumpLocked(PrintWriter printWriter, String str) {
            printWriter.println(str + "OrientationSensorJudge");
            String str2 = str + "  ";
            printWriter.println(str2 + "mDesiredRotation=" + Surface.rotationToString(this.mDesiredRotation));
            printWriter.println(str2 + "mProposedRotation=" + Surface.rotationToString(this.mProposedRotation));
            printWriter.println(str2 + "mTouching=" + this.mTouching);
            printWriter.println(str2 + "mTouchEndedTimestampNanos=" + this.mTouchEndedTimestampNanos);
        }

        @Override
        public void resetLocked(boolean z) {
            if (z) {
                this.mProposedRotation = -1;
                this.mDesiredRotation = -1;
            }
            this.mTouching = false;
            this.mTouchEndedTimestampNanos = Long.MIN_VALUE;
            unscheduleRotationEvaluationLocked();
        }

        public int evaluateRotationChangeLocked() {
            unscheduleRotationEvaluationLocked();
            if (this.mDesiredRotation == this.mProposedRotation) {
                return -1;
            }
            long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
            if (isDesiredRotationAcceptableLocked(jElapsedRealtimeNanos)) {
                this.mProposedRotation = this.mDesiredRotation;
                return this.mProposedRotation;
            }
            scheduleRotationEvaluationIfNecessaryLocked(jElapsedRealtimeNanos);
            return -1;
        }

        private boolean isDesiredRotationAcceptableLocked(long j) {
            return !this.mTouching && j >= this.mTouchEndedTimestampNanos + 500000000;
        }

        private void scheduleRotationEvaluationIfNecessaryLocked(long j) {
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
                if (j >= this.mTouchEndedTimestampNanos + 500000000) {
                    if (WindowOrientationListener.LOG) {
                        Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, already past the next possible time of rotation.");
                    }
                } else {
                    WindowOrientationListener.this.mHandler.postDelayed(this.mRotationEvaluator, (long) Math.ceil((r0 - j) * 1.0E-6f));
                    this.mRotationEvaluationScheduled = true;
                }
            }
        }

        private void unscheduleRotationEvaluationLocked() {
            if (this.mRotationEvaluationScheduled) {
                WindowOrientationListener.this.mHandler.removeCallbacks(this.mRotationEvaluator);
                this.mRotationEvaluationScheduled = false;
            }
        }
    }
}
