package com.android.server.location;

import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

public class GnssBatchingProvider {
    private boolean mEnabled;
    private final GnssBatchingProviderNative mNative;
    private long mPeriodNanos;
    private boolean mStarted;
    private boolean mWakeOnFifoFull;
    private static final String TAG = "GnssBatchingProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    private static native void native_cleanup_batching();

    private static native void native_flush_batch();

    private static native int native_get_batch_size();

    private static native boolean native_init_batching();

    private static native boolean native_start_batch(long j, boolean z);

    private static native boolean native_stop_batch();

    GnssBatchingProvider() {
        this(new GnssBatchingProviderNative());
    }

    @VisibleForTesting
    GnssBatchingProvider(GnssBatchingProviderNative gnssBatchingProviderNative) {
        this.mNative = gnssBatchingProviderNative;
    }

    public int getBatchSize() {
        return this.mNative.getBatchSize();
    }

    public void enable() {
        this.mEnabled = this.mNative.initBatching();
        if (!this.mEnabled) {
            Log.e(TAG, "Failed to initialize GNSS batching");
        }
    }

    public boolean start(long j, boolean z) {
        if (!this.mEnabled) {
            throw new IllegalStateException();
        }
        if (j <= 0) {
            Log.e(TAG, "Invalid periodNanos " + j + " in batching request, not started");
            return false;
        }
        this.mStarted = this.mNative.startBatch(j, z);
        if (this.mStarted) {
            this.mPeriodNanos = j;
            this.mWakeOnFifoFull = z;
        }
        return this.mStarted;
    }

    public void flush() {
        if (!this.mStarted) {
            Log.w(TAG, "Cannot flush since GNSS batching has not started.");
        } else {
            this.mNative.flushBatch();
        }
    }

    public boolean stop() {
        boolean zStopBatch = this.mNative.stopBatch();
        if (zStopBatch) {
            this.mStarted = false;
        }
        return zStopBatch;
    }

    public void disable() {
        stop();
        this.mNative.cleanupBatching();
        this.mEnabled = false;
    }

    void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        if (this.mStarted) {
            this.mNative.startBatch(this.mPeriodNanos, this.mWakeOnFifoFull);
        }
    }

    @VisibleForTesting
    static class GnssBatchingProviderNative {
        GnssBatchingProviderNative() {
        }

        public int getBatchSize() {
            return GnssBatchingProvider.native_get_batch_size();
        }

        public boolean startBatch(long j, boolean z) {
            return GnssBatchingProvider.native_start_batch(j, z);
        }

        public void flushBatch() {
            GnssBatchingProvider.native_flush_batch();
        }

        public boolean stopBatch() {
            return GnssBatchingProvider.native_stop_batch();
        }

        public boolean initBatching() {
            return GnssBatchingProvider.native_init_batching();
        }

        public void cleanupBatching() {
            GnssBatchingProvider.native_cleanup_batching();
        }
    }
}
