package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.JobSchedulerService;
import com.android.server.storage.DeviceStorageMonitorService;
import java.util.function.Predicate;

public final class StorageController extends StateController {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Storage";
    private final StorageTracker mStorageTracker;
    private final ArraySet<JobStatus> mTrackedTasks;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    @VisibleForTesting
    public StorageTracker getTracker() {
        return this.mStorageTracker;
    }

    public StorageController(JobSchedulerService jobSchedulerService) {
        super(jobSchedulerService);
        this.mTrackedTasks = new ArraySet<>();
        this.mStorageTracker = new StorageTracker();
        this.mStorageTracker.startTracking();
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if (jobStatus.hasStorageNotLowConstraint()) {
            this.mTrackedTasks.add(jobStatus);
            jobStatus.setTrackingController(16);
            jobStatus.setStorageNotLowConstraintSatisfied(this.mStorageTracker.isStorageNotLow());
        }
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
        if (jobStatus.clearTrackingController(16)) {
            this.mTrackedTasks.remove(jobStatus);
        }
    }

    private void maybeReportNewStorageState() {
        boolean z;
        boolean zIsStorageNotLow = this.mStorageTracker.isStorageNotLow();
        synchronized (this.mLock) {
            z = false;
            for (int size = this.mTrackedTasks.size() - 1; size >= 0; size--) {
                if (this.mTrackedTasks.valueAt(size).setStorageNotLowConstraintSatisfied(zIsStorageNotLow) != zIsStorageNotLow) {
                    z = true;
                }
            }
        }
        if (z) {
            this.mStateChangedListener.onControllerStateChanged();
        }
        if (zIsStorageNotLow) {
            this.mStateChangedListener.onRunJobNow(null);
        }
    }

    public final class StorageTracker extends BroadcastReceiver {
        private int mLastStorageSeq = -1;
        private boolean mStorageLow;

        public StorageTracker() {
        }

        public void startTracking() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
            intentFilter.addAction("android.intent.action.DEVICE_STORAGE_OK");
            StorageController.this.mContext.registerReceiver(this, intentFilter);
        }

        public boolean isStorageNotLow() {
            return !this.mStorageLow;
        }

        public int getSeq() {
            return this.mLastStorageSeq;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            onReceiveInternal(intent);
        }

        @VisibleForTesting
        public void onReceiveInternal(Intent intent) {
            String action = intent.getAction();
            this.mLastStorageSeq = intent.getIntExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mLastStorageSeq);
            if ("android.intent.action.DEVICE_STORAGE_LOW".equals(action)) {
                if (StorageController.DEBUG) {
                    Slog.d(StorageController.TAG, "Available storage too low to do work. @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                }
                this.mStorageLow = true;
                return;
            }
            if ("android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                if (StorageController.DEBUG) {
                    Slog.d(StorageController.TAG, "Available stoage high enough to do work. @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                }
                this.mStorageLow = false;
                StorageController.this.maybeReportNewStorageState();
            }
        }
    }

    @Override
    public void dumpControllerStateLocked(IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate) {
        indentingPrintWriter.println("Not low: " + this.mStorageTracker.isStorageNotLow());
        indentingPrintWriter.println("Sequence: " + this.mStorageTracker.getSeq());
        indentingPrintWriter.println();
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus jobStatusValueAt = this.mTrackedTasks.valueAt(i);
            if (predicate.test(jobStatusValueAt)) {
                indentingPrintWriter.print("#");
                jobStatusValueAt.printUniqueId(indentingPrintWriter);
                indentingPrintWriter.print(" from ");
                UserHandle.formatUid(indentingPrintWriter, jobStatusValueAt.getSourceUid());
                indentingPrintWriter.println();
            }
        }
    }

    @Override
    public void dumpControllerStateLocked(ProtoOutputStream protoOutputStream, long j, Predicate<JobStatus> predicate) {
        long jStart = protoOutputStream.start(j);
        long jStart2 = protoOutputStream.start(1146756268039L);
        protoOutputStream.write(1133871366145L, this.mStorageTracker.isStorageNotLow());
        protoOutputStream.write(1120986464258L, this.mStorageTracker.getSeq());
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus jobStatusValueAt = this.mTrackedTasks.valueAt(i);
            if (predicate.test(jobStatusValueAt)) {
                long jStart3 = protoOutputStream.start(2246267895811L);
                jobStatusValueAt.writeToShortProto(protoOutputStream, 1146756268033L);
                protoOutputStream.write(1120986464258L, jobStatusValueAt.getSourceUid());
                protoOutputStream.end(jStart3);
            }
        }
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }
}
