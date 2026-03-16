package com.android.server.job.controllers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManagerInternal;
import android.os.SystemClock;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BatteryController extends StateController {
    private static final String ACTION_CHARGING_STABLE = "com.android.server.task.controllers.BatteryController.ACTION_CHARGING_STABLE";
    private static final long STABLE_CHARGING_THRESHOLD_MILLIS = 120000;
    private static final String TAG = "JobScheduler.Batt";
    private static volatile BatteryController sController;
    private static final Object sCreationLock = new Object();
    private ChargingTracker mChargeTracker;
    private List<JobStatus> mTrackedTasks;

    public static BatteryController get(JobSchedulerService taskManagerService) {
        synchronized (sCreationLock) {
            if (sController == null) {
                sController = new BatteryController(taskManagerService, taskManagerService.getContext());
            }
        }
        return sController;
    }

    public ChargingTracker getTracker() {
        return this.mChargeTracker;
    }

    public static BatteryController getForTesting(StateChangedListener stateChangedListener, Context context) {
        return new BatteryController(stateChangedListener, context);
    }

    private BatteryController(StateChangedListener stateChangedListener, Context context) {
        super(stateChangedListener, context);
        this.mTrackedTasks = new ArrayList();
        this.mChargeTracker = new ChargingTracker();
        this.mChargeTracker.startTracking();
    }

    @Override
    public void maybeStartTrackingJob(JobStatus taskStatus) {
        boolean isOnStablePower = this.mChargeTracker.isOnStablePower();
        if (taskStatus.hasChargingConstraint()) {
            synchronized (this.mTrackedTasks) {
                this.mTrackedTasks.add(taskStatus);
                taskStatus.chargingConstraintSatisfied.set(isOnStablePower);
            }
        }
        if (isOnStablePower) {
            this.mChargeTracker.setStableChargingAlarm();
        }
    }

    @Override
    public void maybeStopTrackingJob(JobStatus taskStatus) {
        if (taskStatus.hasChargingConstraint()) {
            synchronized (this.mTrackedTasks) {
                this.mTrackedTasks.remove(taskStatus);
            }
        }
    }

    private void maybeReportNewChargingState() {
        boolean stablePower = this.mChargeTracker.isOnStablePower();
        boolean reportChange = false;
        synchronized (this.mTrackedTasks) {
            for (JobStatus ts : this.mTrackedTasks) {
                boolean previous = ts.chargingConstraintSatisfied.getAndSet(stablePower);
                if (previous != stablePower) {
                    reportChange = true;
                }
            }
        }
        if (reportChange) {
            this.mStateChangedListener.onControllerStateChanged();
        }
        if (stablePower) {
            this.mStateChangedListener.onRunJobNow(null);
        }
    }

    public class ChargingTracker extends BroadcastReceiver {
        private final AlarmManager mAlarm;
        private boolean mBatteryHealthy;
        private boolean mCharging;
        private final PendingIntent mStableChargingTriggerIntent;

        public ChargingTracker() {
            this.mAlarm = (AlarmManager) BatteryController.this.mContext.getSystemService("alarm");
            Intent intent = new Intent(BatteryController.ACTION_CHARGING_STABLE);
            this.mStableChargingTriggerIntent = PendingIntent.getBroadcast(BatteryController.this.mContext, 0, intent, 0);
        }

        public void startTracking() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_LOW");
            filter.addAction("android.intent.action.BATTERY_OKAY");
            filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
            filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
            filter.addAction(BatteryController.ACTION_CHARGING_STABLE);
            BatteryController.this.mContext.registerReceiver(this, filter);
            BatteryManagerInternal batteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            this.mBatteryHealthy = !batteryManagerInternal.getBatteryLevelLow();
            this.mCharging = batteryManagerInternal.isPowered(7);
        }

        boolean isOnStablePower() {
            return this.mCharging && this.mBatteryHealthy;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            onReceiveInternal(intent);
        }

        public void onReceiveInternal(Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.BATTERY_LOW".equals(action)) {
                this.mBatteryHealthy = false;
                return;
            }
            if ("android.intent.action.BATTERY_OKAY".equals(action)) {
                this.mBatteryHealthy = true;
                BatteryController.this.maybeReportNewChargingState();
                return;
            }
            if ("android.intent.action.ACTION_POWER_CONNECTED".equals(action)) {
                setStableChargingAlarm();
                this.mCharging = true;
            } else if ("android.intent.action.ACTION_POWER_DISCONNECTED".equals(action)) {
                this.mAlarm.cancel(this.mStableChargingTriggerIntent);
                this.mCharging = false;
                BatteryController.this.maybeReportNewChargingState();
            } else if (BatteryController.ACTION_CHARGING_STABLE.equals(action) && this.mCharging) {
                BatteryController.this.maybeReportNewChargingState();
            }
        }

        void setStableChargingAlarm() {
            long alarmTriggerElapsed = SystemClock.elapsedRealtime() + BatteryController.STABLE_CHARGING_THRESHOLD_MILLIS;
            this.mAlarm.set(2, alarmTriggerElapsed, this.mStableChargingTriggerIntent);
        }
    }

    @Override
    public void dumpControllerState(PrintWriter pw) {
        pw.println("Batt.");
        pw.println("Stable power: " + this.mChargeTracker.isOnStablePower());
        synchronized (this.mTrackedTasks) {
            Iterator<JobStatus> it = this.mTrackedTasks.iterator();
            if (it.hasNext()) {
                pw.print(String.valueOf(it.next().hashCode()));
            }
            while (it.hasNext()) {
                pw.print("," + String.valueOf(it.next().hashCode()));
            }
            pw.println();
        }
    }
}
