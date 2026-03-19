package com.android.server.power;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;

final class WirelessChargerDetector {
    private static final boolean DEBUG = false;
    private static final double MAX_GRAVITY = 10.806650161743164d;
    private static final double MIN_GRAVITY = 8.806650161743164d;
    private static final int MIN_SAMPLES = 3;
    private static final double MOVEMENT_ANGLE_COS_THRESHOLD = Math.cos(0.08726646259971647d);
    private static final int SAMPLING_INTERVAL_MILLIS = 50;
    private static final long SETTLE_TIME_MILLIS = 800;
    private static final String TAG = "WirelessChargerDetector";
    private boolean mAtRest;
    private boolean mDetectionInProgress;
    private long mDetectionStartTime;
    private float mFirstSampleX;
    private float mFirstSampleY;
    private float mFirstSampleZ;
    private Sensor mGravitySensor;
    private final Handler mHandler;
    private float mLastSampleX;
    private float mLastSampleY;
    private float mLastSampleZ;
    private int mMovingSamples;
    private boolean mMustUpdateRestPosition;
    private boolean mPoweredWirelessly;
    private float mRestX;
    private float mRestY;
    private float mRestZ;
    private final SensorManager mSensorManager;
    private final SuspendBlocker mSuspendBlocker;
    private int mTotalSamples;
    private final Object mLock = new Object();
    private final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            synchronized (WirelessChargerDetector.this.mLock) {
                WirelessChargerDetector.this.processSampleLocked(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    private final Runnable mSensorTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (WirelessChargerDetector.this.mLock) {
                WirelessChargerDetector.this.finishDetectionLocked();
            }
        }
    };

    public WirelessChargerDetector(SensorManager sensorManager, SuspendBlocker suspendBlocker, Handler handler) {
        this.mSensorManager = sensorManager;
        this.mSuspendBlocker = suspendBlocker;
        this.mHandler = handler;
        this.mGravitySensor = sensorManager.getDefaultSensor(9);
    }

    public void dump(PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println();
            printWriter.println("Wireless Charger Detector State:");
            printWriter.println("  mGravitySensor=" + this.mGravitySensor);
            printWriter.println("  mPoweredWirelessly=" + this.mPoweredWirelessly);
            printWriter.println("  mAtRest=" + this.mAtRest);
            printWriter.println("  mRestX=" + this.mRestX + ", mRestY=" + this.mRestY + ", mRestZ=" + this.mRestZ);
            StringBuilder sb = new StringBuilder();
            sb.append("  mDetectionInProgress=");
            sb.append(this.mDetectionInProgress);
            printWriter.println(sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("  mDetectionStartTime=");
            sb2.append(this.mDetectionStartTime == 0 ? "0 (never)" : TimeUtils.formatUptime(this.mDetectionStartTime));
            printWriter.println(sb2.toString());
            printWriter.println("  mMustUpdateRestPosition=" + this.mMustUpdateRestPosition);
            printWriter.println("  mTotalSamples=" + this.mTotalSamples);
            printWriter.println("  mMovingSamples=" + this.mMovingSamples);
            printWriter.println("  mFirstSampleX=" + this.mFirstSampleX + ", mFirstSampleY=" + this.mFirstSampleY + ", mFirstSampleZ=" + this.mFirstSampleZ);
            printWriter.println("  mLastSampleX=" + this.mLastSampleX + ", mLastSampleY=" + this.mLastSampleY + ", mLastSampleZ=" + this.mLastSampleZ);
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        synchronized (this.mLock) {
            protoOutputStream.write(1133871366145L, this.mPoweredWirelessly);
            protoOutputStream.write(1133871366146L, this.mAtRest);
            long jStart2 = protoOutputStream.start(1146756268035L);
            protoOutputStream.write(1108101562369L, this.mRestX);
            protoOutputStream.write(1108101562370L, this.mRestY);
            protoOutputStream.write(1108101562371L, this.mRestZ);
            protoOutputStream.end(jStart2);
            protoOutputStream.write(1133871366148L, this.mDetectionInProgress);
            protoOutputStream.write(1112396529669L, this.mDetectionStartTime);
            protoOutputStream.write(1133871366150L, this.mMustUpdateRestPosition);
            protoOutputStream.write(1120986464263L, this.mTotalSamples);
            protoOutputStream.write(1120986464264L, this.mMovingSamples);
            long jStart3 = protoOutputStream.start(1146756268041L);
            protoOutputStream.write(1108101562369L, this.mFirstSampleX);
            protoOutputStream.write(1108101562370L, this.mFirstSampleY);
            protoOutputStream.write(1108101562371L, this.mFirstSampleZ);
            protoOutputStream.end(jStart3);
            long jStart4 = protoOutputStream.start(1146756268042L);
            protoOutputStream.write(1108101562369L, this.mLastSampleX);
            protoOutputStream.write(1108101562370L, this.mLastSampleY);
            protoOutputStream.write(1108101562371L, this.mLastSampleZ);
            protoOutputStream.end(jStart4);
        }
        protoOutputStream.end(jStart);
    }

    public boolean update(boolean z, int i) {
        boolean z2;
        synchronized (this.mLock) {
            boolean z3 = this.mPoweredWirelessly;
            z2 = false;
            if (z && i == 4) {
                this.mPoweredWirelessly = true;
                this.mMustUpdateRestPosition = true;
                startDetectionLocked();
            } else {
                this.mPoweredWirelessly = false;
                if (this.mAtRest) {
                    if (i != 0 && i != 4) {
                        this.mMustUpdateRestPosition = false;
                        clearAtRestLocked();
                    } else {
                        startDetectionLocked();
                    }
                }
            }
            if (this.mPoweredWirelessly && !z3 && !this.mAtRest) {
                z2 = true;
            }
        }
        return z2;
    }

    private void startDetectionLocked() {
        if (!this.mDetectionInProgress && this.mGravitySensor != null && this.mSensorManager.registerListener(this.mListener, this.mGravitySensor, 50000)) {
            this.mSuspendBlocker.acquire();
            this.mDetectionInProgress = true;
            this.mDetectionStartTime = SystemClock.uptimeMillis();
            this.mTotalSamples = 0;
            this.mMovingSamples = 0;
            Message messageObtain = Message.obtain(this.mHandler, this.mSensorTimeout);
            messageObtain.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(messageObtain, SETTLE_TIME_MILLIS);
        }
    }

    private void finishDetectionLocked() {
        if (this.mDetectionInProgress) {
            this.mSensorManager.unregisterListener(this.mListener);
            this.mHandler.removeCallbacks(this.mSensorTimeout);
            if (this.mMustUpdateRestPosition) {
                clearAtRestLocked();
                if (this.mTotalSamples < 3) {
                    Slog.w(TAG, "Wireless charger detector is broken.  Only received " + this.mTotalSamples + " samples from the gravity sensor but we need at least 3 and we expect to see about 16 on average.");
                } else if (this.mMovingSamples == 0) {
                    this.mAtRest = true;
                    this.mRestX = this.mLastSampleX;
                    this.mRestY = this.mLastSampleY;
                    this.mRestZ = this.mLastSampleZ;
                }
                this.mMustUpdateRestPosition = false;
            }
            this.mDetectionInProgress = false;
            this.mSuspendBlocker.release();
        }
    }

    private void processSampleLocked(float f, float f2, float f3) {
        if (this.mDetectionInProgress) {
            this.mLastSampleX = f;
            this.mLastSampleY = f2;
            this.mLastSampleZ = f3;
            this.mTotalSamples++;
            if (this.mTotalSamples == 1) {
                this.mFirstSampleX = f;
                this.mFirstSampleY = f2;
                this.mFirstSampleZ = f3;
            } else if (hasMoved(this.mFirstSampleX, this.mFirstSampleY, this.mFirstSampleZ, f, f2, f3)) {
                this.mMovingSamples++;
            }
            if (this.mAtRest && hasMoved(this.mRestX, this.mRestY, this.mRestZ, f, f2, f3)) {
                clearAtRestLocked();
            }
        }
    }

    private void clearAtRestLocked() {
        this.mAtRest = false;
        this.mRestX = 0.0f;
        this.mRestY = 0.0f;
        this.mRestZ = 0.0f;
    }

    private static boolean hasMoved(float f, float f2, float f3, float f4, float f5, float f6) {
        double d = (f * f4) + (f2 * f5) + (f3 * f6);
        double dSqrt = Math.sqrt((f * f) + (f2 * f2) + (f3 * f3));
        double dSqrt2 = Math.sqrt((f4 * f4) + (f5 * f5) + (f6 * f6));
        return dSqrt < MIN_GRAVITY || dSqrt > MAX_GRAVITY || dSqrt2 < MIN_GRAVITY || dSqrt2 > MAX_GRAVITY || d < (dSqrt * dSqrt2) * MOVEMENT_ANGLE_COS_THRESHOLD;
    }
}
