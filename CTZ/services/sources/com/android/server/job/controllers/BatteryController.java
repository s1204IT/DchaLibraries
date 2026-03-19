package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManagerInternal;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.storage.DeviceStorageMonitorService;
import java.util.function.Predicate;

public final class BatteryController extends StateController {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Battery";
    private ChargingTracker mChargeTracker;
    private final ArraySet<JobStatus> mTrackedTasks;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    @VisibleForTesting
    public ChargingTracker getTracker() {
        return this.mChargeTracker;
    }

    public BatteryController(JobSchedulerService jobSchedulerService) {
        super(jobSchedulerService);
        this.mTrackedTasks = new ArraySet<>();
        this.mChargeTracker = new ChargingTracker();
        this.mChargeTracker.startTracking();
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if (jobStatus.hasPowerConstraint()) {
            this.mTrackedTasks.add(jobStatus);
            jobStatus.setTrackingController(1);
            jobStatus.setChargingConstraintSatisfied(this.mChargeTracker.isOnStablePower());
            jobStatus.setBatteryNotLowConstraintSatisfied(this.mChargeTracker.isBatteryNotLow());
        }
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
        if (jobStatus.clearTrackingController(1)) {
            this.mTrackedTasks.remove(jobStatus);
        }
    }

    private void maybeReportNewChargingStateLocked() {
        boolean zIsOnStablePower = this.mChargeTracker.isOnStablePower();
        boolean zIsBatteryNotLow = this.mChargeTracker.isBatteryNotLow();
        if (DEBUG) {
            Slog.d(TAG, "maybeReportNewChargingStateLocked: " + zIsOnStablePower);
        }
        boolean z = false;
        for (int size = this.mTrackedTasks.size() - 1; size >= 0; size--) {
            JobStatus jobStatusValueAt = this.mTrackedTasks.valueAt(size);
            if (jobStatusValueAt.setChargingConstraintSatisfied(zIsOnStablePower) != zIsOnStablePower) {
                z = true;
            }
            if (jobStatusValueAt.setBatteryNotLowConstraintSatisfied(zIsBatteryNotLow) != zIsBatteryNotLow) {
                z = true;
            }
        }
        if (zIsOnStablePower || zIsBatteryNotLow) {
            this.mStateChangedListener.onRunJobNow(null);
        } else if (z) {
            this.mStateChangedListener.onControllerStateChanged();
        }
    }

    public final class ChargingTracker extends BroadcastReceiver {
        private boolean mBatteryHealthy;
        private boolean mCharging;
        private int mLastBatterySeq = -1;
        private BroadcastReceiver mMonitor;

        public ChargingTracker() {
        }

        public void startTracking() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.BATTERY_LOW");
            intentFilter.addAction("android.intent.action.BATTERY_OKAY");
            intentFilter.addAction("android.os.action.CHARGING");
            intentFilter.addAction("android.os.action.DISCHARGING");
            BatteryController.this.mContext.registerReceiver(this, intentFilter);
            BatteryManagerInternal batteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            this.mBatteryHealthy = !batteryManagerInternal.getBatteryLevelLow();
            this.mCharging = batteryManagerInternal.isPowered(7);
        }

        public void setMonitorBatteryLocked(boolean z) {
            if (z) {
                if (this.mMonitor == null) {
                    this.mMonitor = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            ChargingTracker.this.onReceive(context, intent);
                        }
                    };
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
                    BatteryController.this.mContext.registerReceiver(this.mMonitor, intentFilter);
                    return;
                }
                return;
            }
            if (this.mMonitor != null) {
                BatteryController.this.mContext.unregisterReceiver(this.mMonitor);
                this.mMonitor = null;
            }
        }

        public boolean isOnStablePower() {
            return this.mCharging && this.mBatteryHealthy;
        }

        public boolean isBatteryNotLow() {
            return this.mBatteryHealthy;
        }

        public boolean isMonitoring() {
            return this.mMonitor != null;
        }

        public int getSeq() {
            return this.mLastBatterySeq;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            onReceiveInternal(intent);
        }

        @VisibleForTesting
        public void onReceiveInternal(Intent intent) {
            synchronized (BatteryController.this.mLock) {
                String action = intent.getAction();
                if ("android.intent.action.BATTERY_LOW".equals(action)) {
                    if (BatteryController.DEBUG) {
                        Slog.d(BatteryController.TAG, "Battery life too low to do work. @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    this.mBatteryHealthy = false;
                    BatteryController.this.maybeReportNewChargingStateLocked();
                } else if ("android.intent.action.BATTERY_OKAY".equals(action)) {
                    if (BatteryController.DEBUG) {
                        Slog.d(BatteryController.TAG, "Battery life healthy enough to do work. @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    this.mBatteryHealthy = true;
                    BatteryController.this.maybeReportNewChargingStateLocked();
                } else if ("android.os.action.CHARGING".equals(action)) {
                    if (BatteryController.DEBUG) {
                        Slog.d(BatteryController.TAG, "Received charging intent, fired @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    this.mCharging = true;
                    BatteryController.this.maybeReportNewChargingStateLocked();
                } else if ("android.os.action.DISCHARGING".equals(action)) {
                    if (BatteryController.DEBUG) {
                        Slog.d(BatteryController.TAG, "Disconnected from power.");
                    }
                    this.mCharging = false;
                    BatteryController.this.maybeReportNewChargingStateLocked();
                }
                this.mLastBatterySeq = intent.getIntExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mLastBatterySeq);
            }
        }
    }

    @Override
    public void dumpControllerStateLocked(IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate) {
        indentingPrintWriter.println("Stable power: " + this.mChargeTracker.isOnStablePower());
        indentingPrintWriter.println("Not low: " + this.mChargeTracker.isBatteryNotLow());
        if (this.mChargeTracker.isMonitoring()) {
            indentingPrintWriter.print("MONITORING: seq=");
            indentingPrintWriter.println(this.mChargeTracker.getSeq());
        }
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
        long jStart2 = protoOutputStream.start(1146756268034L);
        protoOutputStream.write(1133871366145L, this.mChargeTracker.isOnStablePower());
        protoOutputStream.write(1133871366146L, this.mChargeTracker.isBatteryNotLow());
        protoOutputStream.write(1133871366147L, this.mChargeTracker.isMonitoring());
        protoOutputStream.write(1120986464260L, this.mChargeTracker.getSeq());
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus jobStatusValueAt = this.mTrackedTasks.valueAt(i);
            if (predicate.test(jobStatusValueAt)) {
                long jStart3 = protoOutputStream.start(2246267895813L);
                jobStatusValueAt.writeToShortProto(protoOutputStream, 1146756268033L);
                protoOutputStream.write(1120986464258L, jobStatusValueAt.getSourceUid());
                protoOutputStream.end(jStart3);
            }
        }
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }
}
