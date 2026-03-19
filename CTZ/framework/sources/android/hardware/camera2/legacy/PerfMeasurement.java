package android.hardware.camera2.legacy;

import android.os.SystemClock;
import android.provider.SettingsStringUtil;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

class PerfMeasurement {
    public static final int DEFAULT_MAX_QUERIES = 3;
    private static final long FAILED_TIMING = -2;
    private static final long NO_DURATION_YET = -1;
    private static final String TAG = "PerfMeasurement";
    private ArrayList<Long> mCollectedCpuDurations;
    private ArrayList<Long> mCollectedGpuDurations;
    private ArrayList<Long> mCollectedTimestamps;
    private int mCompletedQueryCount;
    private Queue<Long> mCpuDurationsQueue;
    private final long mNativeContext;
    private long mStartTimeNs;
    private Queue<Long> mTimestampQueue;

    private static native long nativeCreateContext(int i);

    private static native void nativeDeleteContext(long j);

    protected static native long nativeGetNextGlDuration(long j);

    private static native boolean nativeQuerySupport();

    protected static native void nativeStartGlTimer(long j);

    protected static native void nativeStopGlTimer(long j);

    public PerfMeasurement() {
        this.mCompletedQueryCount = 0;
        this.mCollectedGpuDurations = new ArrayList<>();
        this.mCollectedCpuDurations = new ArrayList<>();
        this.mCollectedTimestamps = new ArrayList<>();
        this.mTimestampQueue = new LinkedList();
        this.mCpuDurationsQueue = new LinkedList();
        this.mNativeContext = nativeCreateContext(3);
    }

    public PerfMeasurement(int i) {
        this.mCompletedQueryCount = 0;
        this.mCollectedGpuDurations = new ArrayList<>();
        this.mCollectedCpuDurations = new ArrayList<>();
        this.mCollectedTimestamps = new ArrayList<>();
        this.mTimestampQueue = new LinkedList();
        this.mCpuDurationsQueue = new LinkedList();
        if (i < 1) {
            throw new IllegalArgumentException("maxQueries is less than 1");
        }
        this.mNativeContext = nativeCreateContext(i);
    }

    public static boolean isGlTimingSupported() {
        return nativeQuerySupport();
    }

    public void dumpPerformanceData(String str) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(str));
            try {
                bufferedWriter.write("timestamp gpu_duration cpu_duration\n");
                for (int i = 0; i < this.mCollectedGpuDurations.size(); i++) {
                    bufferedWriter.write(String.format("%d %d %d\n", this.mCollectedTimestamps.get(i), this.mCollectedGpuDurations.get(i), this.mCollectedCpuDurations.get(i)));
                }
                this.mCollectedTimestamps.clear();
                this.mCollectedGpuDurations.clear();
                this.mCollectedCpuDurations.clear();
                bufferedWriter.close();
            } finally {
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing data dump to " + str + SettingsStringUtil.DELIMITER + e);
        }
    }

    public void startTimer() {
        nativeStartGlTimer(this.mNativeContext);
        this.mStartTimeNs = SystemClock.elapsedRealtimeNanos();
    }

    public void stopTimer() {
        long jLongValue;
        this.mCpuDurationsQueue.add(Long.valueOf(SystemClock.elapsedRealtimeNanos() - this.mStartTimeNs));
        nativeStopGlTimer(this.mNativeContext);
        long nextGlDuration = getNextGlDuration();
        if (nextGlDuration > 0) {
            this.mCollectedGpuDurations.add(Long.valueOf(nextGlDuration));
            ArrayList<Long> arrayList = this.mCollectedTimestamps;
            if (this.mTimestampQueue.isEmpty()) {
                jLongValue = -1;
            } else {
                jLongValue = this.mTimestampQueue.poll().longValue();
            }
            arrayList.add(Long.valueOf(jLongValue));
            this.mCollectedCpuDurations.add(Long.valueOf(this.mCpuDurationsQueue.isEmpty() ? -1L : this.mCpuDurationsQueue.poll().longValue()));
        }
        if (nextGlDuration == -2) {
            if (!this.mTimestampQueue.isEmpty()) {
                this.mTimestampQueue.poll();
            }
            if (!this.mCpuDurationsQueue.isEmpty()) {
                this.mCpuDurationsQueue.poll();
            }
        }
    }

    public void addTimestamp(long j) {
        this.mTimestampQueue.add(Long.valueOf(j));
    }

    private long getNextGlDuration() {
        long jNativeGetNextGlDuration = nativeGetNextGlDuration(this.mNativeContext);
        if (jNativeGetNextGlDuration > 0) {
            this.mCompletedQueryCount++;
        }
        return jNativeGetNextGlDuration;
    }

    public int getCompletedQueryCount() {
        return this.mCompletedQueryCount;
    }

    protected void finalize() {
        nativeDeleteContext(this.mNativeContext);
    }
}
