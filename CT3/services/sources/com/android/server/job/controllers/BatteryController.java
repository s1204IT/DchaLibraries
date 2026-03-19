package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManagerInternal;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BatteryController extends StateController {
    private static final String TAG = "JobScheduler.Batt";
    private static volatile BatteryController sController;
    private static final Object sCreationLock = new Object();
    private ChargingTracker mChargeTracker;
    private List<JobStatus> mTrackedTasks;

    public static BatteryController get(JobSchedulerService taskManagerService) {
        synchronized (sCreationLock) {
            if (sController == null) {
                sController = new BatteryController(taskManagerService, taskManagerService.getContext(), taskManagerService.getLock());
            }
        }
        return sController;
    }

    public ChargingTracker getTracker() {
        return this.mChargeTracker;
    }

    public static BatteryController getForTesting(StateChangedListener stateChangedListener, Context context) {
        return new BatteryController(stateChangedListener, context, new Object());
    }

    private BatteryController(StateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        this.mTrackedTasks = new ArrayList();
        this.mChargeTracker = new ChargingTracker();
        this.mChargeTracker.startTracking();
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus taskStatus, JobStatus lastJob) {
        boolean isOnStablePower = this.mChargeTracker.isOnStablePower();
        if (!taskStatus.hasChargingConstraint()) {
            return;
        }
        this.mTrackedTasks.add(taskStatus);
        taskStatus.setChargingConstraintSatisfied(isOnStablePower);
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus taskStatus, JobStatus incomingJob, boolean forUpdate) {
        if (!taskStatus.hasChargingConstraint()) {
            return;
        }
        this.mTrackedTasks.remove(taskStatus);
    }

    private void maybeReportNewChargingState() {
        boolean stablePower = this.mChargeTracker.isOnStablePower();
        if (DEBUG) {
            Slog.d(TAG, "maybeReportNewChargingState: " + stablePower);
        }
        boolean reportChange = false;
        synchronized (this.mLock) {
            for (JobStatus ts : this.mTrackedTasks) {
                boolean previous = ts.setChargingConstraintSatisfied(stablePower);
                if (previous != stablePower) {
                    reportChange = true;
                }
            }
        }
        if (reportChange) {
            this.mStateChangedListener.onControllerStateChanged();
        }
        if (!stablePower) {
            return;
        }
        this.mStateChangedListener.onRunJobNow(null);
    }

    public class ChargingTracker extends BroadcastReceiver {
        private boolean mBatteryHealthy;
        private boolean mCharging;

        public ChargingTracker() {
        }

        public void startTracking() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_LOW");
            filter.addAction("android.intent.action.BATTERY_OKAY");
            filter.addAction("android.os.action.CHARGING");
            filter.addAction("android.os.action.DISCHARGING");
            BatteryController.this.mContext.registerReceiver(this, filter);
            BatteryManagerInternal batteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            this.mBatteryHealthy = !batteryManagerInternal.getBatteryLevelLow();
            this.mCharging = batteryManagerInternal.isPowered(7);
        }

        boolean isOnStablePower() {
            if (this.mCharging) {
                return this.mBatteryHealthy;
            }
            return false;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            onReceiveInternal(intent);
        }

        public void onReceiveInternal(Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.BATTERY_LOW".equals(action)) {
                if (BatteryController.DEBUG) {
                    Slog.d(BatteryController.TAG, "Battery life too low to do work. @ " + SystemClock.elapsedRealtime());
                }
                this.mBatteryHealthy = false;
                return;
            }
            if ("android.intent.action.BATTERY_OKAY".equals(action)) {
                if (BatteryController.DEBUG) {
                    Slog.d(BatteryController.TAG, "Battery life healthy enough to do work. @ " + SystemClock.elapsedRealtime());
                }
                this.mBatteryHealthy = true;
                BatteryController.this.maybeReportNewChargingState();
                return;
            }
            if ("android.os.action.CHARGING".equals(action)) {
                if (BatteryController.DEBUG) {
                    Slog.d(BatteryController.TAG, "Received charging intent, fired @ " + SystemClock.elapsedRealtime());
                }
                this.mCharging = true;
                BatteryController.this.maybeReportNewChargingState();
                return;
            }
            if (!"android.os.action.DISCHARGING".equals(action)) {
                return;
            }
            if (BatteryController.DEBUG) {
                Slog.d(BatteryController.TAG, "Disconnected from power.");
            }
            this.mCharging = false;
            BatteryController.this.maybeReportNewChargingState();
        }
    }

    @Override
    public void dumpControllerStateLocked(PrintWriter pw, int filterUid) {
        pw.print("Battery: stable power = ");
        pw.println(this.mChargeTracker.isOnStablePower());
        pw.print("Tracking ");
        pw.print(this.mTrackedTasks.size());
        pw.println(":");
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus js = this.mTrackedTasks.get(i);
            if (js.shouldDump(filterUid)) {
                pw.print("  #");
                js.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, js.getSourceUid());
                pw.println();
            }
        }
    }
}
