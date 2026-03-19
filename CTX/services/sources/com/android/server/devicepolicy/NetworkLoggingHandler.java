package com.android.server.devicepolicy;

import android.app.AlarmManager;
import android.app.admin.NetworkEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.LongSparseArray;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class NetworkLoggingHandler extends Handler {
    private static final long BATCH_FINALIZATION_TIMEOUT_ALARM_INTERVAL_MS = 1800000;
    private static final long BATCH_FINALIZATION_TIMEOUT_MS = 5400000;

    @VisibleForTesting
    static final int LOG_NETWORK_EVENT_MSG = 1;
    private static final int MAX_BATCHES = 5;
    private static final int MAX_EVENTS_PER_BATCH = 1200;
    static final String NETWORK_EVENT_KEY = "network_event";
    private static final String NETWORK_LOGGING_TIMEOUT_ALARM_TAG = "NetworkLogging.batchTimeout";
    private static final long RETRIEVED_BATCH_DISCARD_DELAY_MS = 300000;
    private static final String TAG = NetworkLoggingHandler.class.getSimpleName();
    private final AlarmManager mAlarmManager;
    private final AlarmManager.OnAlarmListener mBatchTimeoutAlarmListener;

    @GuardedBy("this")
    private final LongSparseArray<ArrayList<NetworkEvent>> mBatches;

    @GuardedBy("this")
    private long mCurrentBatchToken;
    private final DevicePolicyManagerService mDpm;
    private long mId;

    @GuardedBy("this")
    private long mLastRetrievedBatchToken;

    @GuardedBy("this")
    private ArrayList<NetworkEvent> mNetworkEvents;

    @GuardedBy("this")
    private boolean mPaused;

    NetworkLoggingHandler(Looper looper, DevicePolicyManagerService devicePolicyManagerService) {
        this(looper, devicePolicyManagerService, 0L);
    }

    @VisibleForTesting
    NetworkLoggingHandler(Looper looper, DevicePolicyManagerService devicePolicyManagerService, long j) {
        super(looper);
        this.mBatchTimeoutAlarmListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                Bundle bundleFinalizeBatchAndBuildDeviceOwnerMessageLocked;
                Slog.d(NetworkLoggingHandler.TAG, "Received a batch finalization timeout alarm, finalizing " + NetworkLoggingHandler.this.mNetworkEvents.size() + " pending events.");
                synchronized (NetworkLoggingHandler.this) {
                    bundleFinalizeBatchAndBuildDeviceOwnerMessageLocked = NetworkLoggingHandler.this.finalizeBatchAndBuildDeviceOwnerMessageLocked();
                }
                if (bundleFinalizeBatchAndBuildDeviceOwnerMessageLocked != null) {
                    NetworkLoggingHandler.this.notifyDeviceOwner(bundleFinalizeBatchAndBuildDeviceOwnerMessageLocked);
                }
            }
        };
        this.mNetworkEvents = new ArrayList<>();
        this.mBatches = new LongSparseArray<>(5);
        this.mPaused = false;
        this.mDpm = devicePolicyManagerService;
        this.mAlarmManager = this.mDpm.mInjector.getAlarmManager();
        this.mId = j;
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == 1) {
            NetworkEvent networkEvent = (NetworkEvent) message.getData().getParcelable(NETWORK_EVENT_KEY);
            if (networkEvent != null) {
                Bundle bundleFinalizeBatchAndBuildDeviceOwnerMessageLocked = null;
                synchronized (this) {
                    this.mNetworkEvents.add(networkEvent);
                    if (this.mNetworkEvents.size() >= MAX_EVENTS_PER_BATCH) {
                        bundleFinalizeBatchAndBuildDeviceOwnerMessageLocked = finalizeBatchAndBuildDeviceOwnerMessageLocked();
                    }
                }
                if (bundleFinalizeBatchAndBuildDeviceOwnerMessageLocked != null) {
                    notifyDeviceOwner(bundleFinalizeBatchAndBuildDeviceOwnerMessageLocked);
                    return;
                }
                return;
            }
            return;
        }
        Slog.d(TAG, "NetworkLoggingHandler received an unknown of message.");
    }

    void scheduleBatchFinalization() {
        this.mAlarmManager.setWindow(2, SystemClock.elapsedRealtime() + BATCH_FINALIZATION_TIMEOUT_MS, 1800000L, NETWORK_LOGGING_TIMEOUT_ALARM_TAG, this.mBatchTimeoutAlarmListener, this);
        Slog.d(TAG, "Scheduled a new batch finalization alarm 5400000ms from now.");
    }

    synchronized void pause() {
        Slog.d(TAG, "Paused network logging");
        this.mPaused = true;
    }

    void resume() {
        Bundle bundleBuildDeviceOwnerMessageLocked;
        synchronized (this) {
            if (!this.mPaused) {
                Slog.d(TAG, "Attempted to resume network logging, but logging is not paused.");
                return;
            }
            Slog.d(TAG, "Resumed network logging. Current batch=" + this.mCurrentBatchToken + ", LastRetrievedBatch=" + this.mLastRetrievedBatchToken);
            this.mPaused = false;
            if (this.mBatches.size() > 0 && this.mLastRetrievedBatchToken != this.mCurrentBatchToken) {
                scheduleBatchFinalization();
                bundleBuildDeviceOwnerMessageLocked = buildDeviceOwnerMessageLocked();
            } else {
                bundleBuildDeviceOwnerMessageLocked = null;
            }
            if (bundleBuildDeviceOwnerMessageLocked != null) {
                notifyDeviceOwner(bundleBuildDeviceOwnerMessageLocked);
            }
        }
    }

    synchronized void discardLogs() {
        this.mBatches.clear();
        this.mNetworkEvents = new ArrayList<>();
        Slog.d(TAG, "Discarded all network logs");
    }

    @GuardedBy("this")
    private Bundle finalizeBatchAndBuildDeviceOwnerMessageLocked() {
        Bundle bundleBuildDeviceOwnerMessageLocked;
        if (this.mNetworkEvents.size() > 0) {
            Iterator<NetworkEvent> it = this.mNetworkEvents.iterator();
            while (it.hasNext()) {
                it.next().setId(this.mId);
                if (this.mId == JobStatus.NO_LATEST_RUNTIME) {
                    Slog.i(TAG, "Reached maximum id value; wrapping around ." + this.mCurrentBatchToken);
                    this.mId = 0L;
                } else {
                    this.mId++;
                }
            }
            if (this.mBatches.size() >= 5) {
                this.mBatches.removeAt(0);
            }
            this.mCurrentBatchToken++;
            this.mBatches.append(this.mCurrentBatchToken, this.mNetworkEvents);
            this.mNetworkEvents = new ArrayList<>();
            if (!this.mPaused) {
                bundleBuildDeviceOwnerMessageLocked = buildDeviceOwnerMessageLocked();
            }
            scheduleBatchFinalization();
            return bundleBuildDeviceOwnerMessageLocked;
        }
        Slog.d(TAG, "Was about to finalize the batch, but there were no events to send to the DPC, the batchToken of last available batch: " + this.mCurrentBatchToken);
        bundleBuildDeviceOwnerMessageLocked = null;
        scheduleBatchFinalization();
        return bundleBuildDeviceOwnerMessageLocked;
    }

    @GuardedBy("this")
    private Bundle buildDeviceOwnerMessageLocked() {
        Bundle bundle = new Bundle();
        int size = this.mBatches.valueAt(this.mBatches.size() - 1).size();
        bundle.putLong("android.app.extra.EXTRA_NETWORK_LOGS_TOKEN", this.mCurrentBatchToken);
        bundle.putInt("android.app.extra.EXTRA_NETWORK_LOGS_COUNT", size);
        return bundle;
    }

    private void notifyDeviceOwner(Bundle bundle) {
        Slog.d(TAG, "Sending network logging batch broadcast to device owner, batchToken: " + bundle.getLong("android.app.extra.EXTRA_NETWORK_LOGS_TOKEN", -1L));
        if (Thread.holdsLock(this)) {
            Slog.wtfStack(TAG, "Shouldn't be called with NetworkLoggingHandler lock held");
        } else {
            this.mDpm.sendDeviceOwnerCommand("android.app.action.NETWORK_LOGS_AVAILABLE", bundle);
        }
    }

    synchronized List<NetworkEvent> retrieveFullLogBatch(final long j) {
        int iIndexOfKey = this.mBatches.indexOfKey(j);
        if (iIndexOfKey < 0) {
            return null;
        }
        postDelayed(new Runnable() {
            @Override
            public final void run() {
                NetworkLoggingHandler.lambda$retrieveFullLogBatch$0(this.f$0, j);
            }
        }, 300000L);
        this.mLastRetrievedBatchToken = j;
        return this.mBatches.valueAt(iIndexOfKey);
    }

    public static void lambda$retrieveFullLogBatch$0(NetworkLoggingHandler networkLoggingHandler, long j) {
        synchronized (networkLoggingHandler) {
            while (networkLoggingHandler.mBatches.size() > 0 && networkLoggingHandler.mBatches.keyAt(0) <= j) {
                networkLoggingHandler.mBatches.removeAt(0);
            }
        }
    }
}
