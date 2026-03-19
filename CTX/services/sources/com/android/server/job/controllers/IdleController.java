package com.android.server.job.controllers;

import android.R;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.am.ActivityManagerService;
import com.android.server.job.JobSchedulerService;
import java.util.function.Predicate;

public final class IdleController extends StateController {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Idle";
    IdlenessTracker mIdleTracker;
    private long mIdleWindowSlop;
    private long mInactivityIdleThreshold;
    final ArraySet<JobStatus> mTrackedTasks;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public IdleController(JobSchedulerService jobSchedulerService) {
        super(jobSchedulerService);
        this.mTrackedTasks = new ArraySet<>();
        initIdleStateTracking();
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if (jobStatus.hasIdleConstraint()) {
            this.mTrackedTasks.add(jobStatus);
            jobStatus.setTrackingController(8);
            jobStatus.setIdleConstraintSatisfied(this.mIdleTracker.isIdle());
        }
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
        if (jobStatus.clearTrackingController(8)) {
            this.mTrackedTasks.remove(jobStatus);
        }
    }

    void reportNewIdleState(boolean z) {
        synchronized (this.mLock) {
            for (int size = this.mTrackedTasks.size() - 1; size >= 0; size--) {
                this.mTrackedTasks.valueAt(size).setIdleConstraintSatisfied(z);
            }
        }
        this.mStateChangedListener.onControllerStateChanged();
    }

    private void initIdleStateTracking() {
        this.mInactivityIdleThreshold = this.mContext.getResources().getInteger(R.integer.config_customizedMaxCachedProcesses);
        this.mIdleWindowSlop = this.mContext.getResources().getInteger(R.integer.config_cursorWindowSize);
        this.mIdleTracker = new IdlenessTracker();
        this.mIdleTracker.startTracking();
    }

    final class IdlenessTracker extends BroadcastReceiver {
        private AlarmManager mAlarm;
        private AlarmManager.OnAlarmListener mIdleAlarmListener = new AlarmManager.OnAlarmListener() {
            @Override
            public final void onAlarm() {
                this.f$0.handleIdleTrigger();
            }
        };
        private boolean mIdle = false;
        private boolean mScreenOn = true;
        private boolean mDockIdle = false;

        public IdlenessTracker() {
            this.mAlarm = (AlarmManager) IdleController.this.mContext.getSystemService("alarm");
        }

        public boolean isIdle() {
            return this.mIdle;
        }

        public void startTracking() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.intent.action.DREAMING_STARTED");
            intentFilter.addAction("android.intent.action.DREAMING_STOPPED");
            intentFilter.addAction(ActivityManagerService.ACTION_TRIGGER_IDLE);
            intentFilter.addAction("android.intent.action.DOCK_IDLE");
            intentFilter.addAction("android.intent.action.DOCK_ACTIVE");
            IdleController.this.mContext.registerReceiver(this, intentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_ON") || action.equals("android.intent.action.DREAMING_STOPPED") || action.equals("android.intent.action.DOCK_ACTIVE")) {
                if (action.equals("android.intent.action.DOCK_ACTIVE")) {
                    if (!this.mScreenOn) {
                        return;
                    } else {
                        this.mDockIdle = false;
                    }
                } else {
                    this.mScreenOn = true;
                    this.mDockIdle = false;
                }
                if (IdleController.DEBUG) {
                    Slog.v(IdleController.TAG, "exiting idle : " + action);
                }
                this.mAlarm.cancel(this.mIdleAlarmListener);
                if (this.mIdle) {
                    this.mIdle = false;
                    IdleController.this.reportNewIdleState(this.mIdle);
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.SCREEN_OFF") || action.equals("android.intent.action.DREAMING_STARTED") || action.equals("android.intent.action.DOCK_IDLE")) {
                if (action.equals("android.intent.action.DOCK_IDLE")) {
                    if (!this.mScreenOn) {
                        return;
                    } else {
                        this.mDockIdle = true;
                    }
                } else {
                    this.mScreenOn = false;
                    this.mDockIdle = false;
                }
                long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
                long j = jMillis + IdleController.this.mInactivityIdleThreshold;
                if (IdleController.DEBUG) {
                    Slog.v(IdleController.TAG, "Scheduling idle : " + action + " now:" + jMillis + " when=" + j);
                }
                this.mAlarm.setWindow(2, j, IdleController.this.mIdleWindowSlop, "JS idleness", this.mIdleAlarmListener, (Handler) null);
                return;
            }
            if (action.equals(ActivityManagerService.ACTION_TRIGGER_IDLE)) {
                handleIdleTrigger();
            }
        }

        private void handleIdleTrigger() {
            if (this.mIdle || (this.mScreenOn && !this.mDockIdle)) {
                if (IdleController.DEBUG) {
                    Slog.v(IdleController.TAG, "TRIGGER_IDLE received but not changing state; idle=" + this.mIdle + " screen=" + this.mScreenOn);
                    return;
                }
                return;
            }
            if (IdleController.DEBUG) {
                Slog.v(IdleController.TAG, "Idle trigger fired @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
            }
            this.mIdle = true;
            IdleController.this.reportNewIdleState(this.mIdle);
        }
    }

    @Override
    public void dumpControllerStateLocked(IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate) {
        indentingPrintWriter.println("Currently idle: " + this.mIdleTracker.isIdle());
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
        long jStart2 = protoOutputStream.start(1146756268038L);
        protoOutputStream.write(1133871366145L, this.mIdleTracker.isIdle());
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus jobStatusValueAt = this.mTrackedTasks.valueAt(i);
            if (predicate.test(jobStatusValueAt)) {
                long jStart3 = protoOutputStream.start(2246267895810L);
                jobStatusValueAt.writeToShortProto(protoOutputStream, 1146756268033L);
                protoOutputStream.write(1120986464258L, jobStatusValueAt.getSourceUid());
                protoOutputStream.end(jStart3);
            }
        }
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }
}
