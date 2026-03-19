package com.android.server.job.controllers;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;

public class IdleController extends StateController {
    private static final String TAG = "IdleController";
    private static volatile IdleController sController;
    private static Object sCreationLock = new Object();
    IdlenessTracker mIdleTracker;
    private long mIdleWindowSlop;
    private long mInactivityIdleThreshold;
    final ArrayList<JobStatus> mTrackedTasks;

    public static IdleController get(JobSchedulerService service) {
        IdleController idleController;
        synchronized (sCreationLock) {
            if (sController == null) {
                sController = new IdleController(service, service.getContext(), service.getLock());
            }
            idleController = sController;
        }
        return idleController;
    }

    private IdleController(StateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        this.mTrackedTasks = new ArrayList<>();
        initIdleStateTracking();
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus taskStatus, JobStatus lastJob) {
        if (!taskStatus.hasIdleConstraint()) {
            return;
        }
        this.mTrackedTasks.add(taskStatus);
        taskStatus.setIdleConstraintSatisfied(this.mIdleTracker.isIdle());
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus taskStatus, JobStatus incomingJob, boolean forUpdate) {
        this.mTrackedTasks.remove(taskStatus);
    }

    void reportNewIdleState(boolean isIdle) {
        synchronized (this.mLock) {
            for (JobStatus task : this.mTrackedTasks) {
                task.setIdleConstraintSatisfied(isIdle);
            }
        }
        this.mStateChangedListener.onControllerStateChanged();
    }

    private void initIdleStateTracking() {
        this.mInactivityIdleThreshold = this.mContext.getResources().getInteger(R.integer.config_fingerprintMaxTemplatesPerUser);
        this.mIdleWindowSlop = this.mContext.getResources().getInteger(R.integer.config_fixedRefreshRateInHighZone);
        this.mIdleTracker = new IdlenessTracker();
        this.mIdleTracker.startTracking();
    }

    class IdlenessTracker extends BroadcastReceiver {
        private AlarmManager mAlarm;
        boolean mIdle;
        private PendingIntent mIdleTriggerIntent;
        boolean mScreenOn;

        public IdlenessTracker() {
            this.mAlarm = (AlarmManager) IdleController.this.mContext.getSystemService("alarm");
            Intent intent = new Intent(ActivityManagerService.ACTION_TRIGGER_IDLE).setPackage("android").setFlags(1073741824);
            this.mIdleTriggerIntent = PendingIntent.getBroadcast(IdleController.this.mContext, 0, intent, 0);
            this.mIdle = false;
            this.mScreenOn = true;
        }

        public boolean isIdle() {
            return this.mIdle;
        }

        public void startTracking() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.DREAMING_STARTED");
            filter.addAction("android.intent.action.DREAMING_STOPPED");
            filter.addAction(ActivityManagerService.ACTION_TRIGGER_IDLE);
            IdleController.this.mContext.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_ON") || action.equals("android.intent.action.DREAMING_STOPPED")) {
                if (IdleController.DEBUG) {
                    Slog.v(IdleController.TAG, "exiting idle : " + action);
                }
                this.mScreenOn = true;
                this.mAlarm.cancel(this.mIdleTriggerIntent);
                if (!this.mIdle) {
                    return;
                }
                this.mIdle = false;
                IdleController.this.reportNewIdleState(this.mIdle);
                return;
            }
            if (action.equals("android.intent.action.SCREEN_OFF") || action.equals("android.intent.action.DREAMING_STARTED")) {
                long nowElapsed = SystemClock.elapsedRealtime();
                long when = nowElapsed + IdleController.this.mInactivityIdleThreshold;
                if (IdleController.DEBUG) {
                    Slog.v(IdleController.TAG, "Scheduling idle : " + action + " now:" + nowElapsed + " when=" + when);
                }
                this.mScreenOn = false;
                this.mAlarm.setWindow(2, when, IdleController.this.mIdleWindowSlop, this.mIdleTriggerIntent);
                return;
            }
            if (!action.equals(ActivityManagerService.ACTION_TRIGGER_IDLE) || this.mIdle || this.mScreenOn) {
                return;
            }
            if (IdleController.DEBUG) {
                Slog.v(IdleController.TAG, "Idle trigger fired @ " + SystemClock.elapsedRealtime());
            }
            this.mIdle = true;
            IdleController.this.reportNewIdleState(this.mIdle);
        }
    }

    @Override
    public void dumpControllerStateLocked(PrintWriter pw, int filterUid) {
        pw.print("Idle: ");
        pw.println(this.mIdleTracker.isIdle() ? "true" : "false");
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
