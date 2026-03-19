package com.android.server.job.controllers;

import android.app.AlarmManager;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.JobSchedulerService;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;

public final class TimeController extends StateController {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Time";
    private final String DEADLINE_TAG;
    private final String DELAY_TAG;
    private AlarmManager mAlarmService;
    private final boolean mChainedAttributionEnabled;
    private final AlarmManager.OnAlarmListener mDeadlineExpiredListener;
    private long mNextDelayExpiredElapsedMillis;
    private final AlarmManager.OnAlarmListener mNextDelayExpiredListener;
    private long mNextJobExpiredElapsedMillis;
    private final List<JobStatus> mTrackedJobs;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public TimeController(JobSchedulerService jobSchedulerService) {
        super(jobSchedulerService);
        this.DEADLINE_TAG = "*job.deadline*";
        this.DELAY_TAG = "*job.delay*";
        this.mAlarmService = null;
        this.mTrackedJobs = new LinkedList();
        this.mDeadlineExpiredListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                if (TimeController.DEBUG) {
                    Slog.d(TimeController.TAG, "Deadline-expired alarm fired");
                }
                TimeController.this.checkExpiredDeadlinesAndResetAlarm();
            }
        };
        this.mNextDelayExpiredListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                if (TimeController.DEBUG) {
                    Slog.d(TimeController.TAG, "Delay-expired alarm fired");
                }
                TimeController.this.checkExpiredDelaysAndResetAlarm();
            }
        };
        this.mNextJobExpiredElapsedMillis = JobStatus.NO_LATEST_RUNTIME;
        this.mNextDelayExpiredElapsedMillis = JobStatus.NO_LATEST_RUNTIME;
        this.mChainedAttributionEnabled = WorkSource.isChainedBatteryAttributionEnabled(this.mContext);
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if (jobStatus.hasTimingDelayConstraint() || jobStatus.hasDeadlineConstraint()) {
            boolean z = false;
            maybeStopTrackingJobLocked(jobStatus, null, false);
            long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (jobStatus.hasDeadlineConstraint() && evaluateDeadlineConstraint(jobStatus, jMillis)) {
                return;
            }
            if (jobStatus.hasTimingDelayConstraint() && evaluateTimingDelayConstraint(jobStatus, jMillis) && !jobStatus.hasDeadlineConstraint()) {
                return;
            }
            ListIterator<JobStatus> listIterator = this.mTrackedJobs.listIterator(this.mTrackedJobs.size());
            while (true) {
                if (!listIterator.hasPrevious()) {
                    break;
                } else if (listIterator.previous().getLatestRunTimeElapsed() < jobStatus.getLatestRunTimeElapsed()) {
                    z = true;
                    break;
                }
            }
            if (z) {
                listIterator.next();
            }
            listIterator.add(jobStatus);
            jobStatus.setTrackingController(32);
            boolean zHasTimingDelayConstraint = jobStatus.hasTimingDelayConstraint();
            long latestRunTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
            long earliestRunTime = zHasTimingDelayConstraint ? jobStatus.getEarliestRunTime() : Long.MAX_VALUE;
            if (jobStatus.hasDeadlineConstraint()) {
                latestRunTimeElapsed = jobStatus.getLatestRunTimeElapsed();
            }
            maybeUpdateAlarmsLocked(earliestRunTime, latestRunTimeElapsed, deriveWorkSource(jobStatus.getSourceUid(), jobStatus.getSourcePackageName()));
        }
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
        if (jobStatus.clearTrackingController(32) && this.mTrackedJobs.remove(jobStatus)) {
            checkExpiredDelaysAndResetAlarm();
            checkExpiredDeadlinesAndResetAlarm();
        }
    }

    private boolean canStopTrackingJobLocked(JobStatus jobStatus) {
        return ((jobStatus.hasTimingDelayConstraint() && (jobStatus.satisfiedConstraints & Integer.MIN_VALUE) == 0) || (jobStatus.hasDeadlineConstraint() && (jobStatus.satisfiedConstraints & 1073741824) == 0)) ? false : true;
    }

    private void ensureAlarmServiceLocked() {
        if (this.mAlarmService == null) {
            this.mAlarmService = (AlarmManager) this.mContext.getSystemService("alarm");
        }
    }

    private void checkExpiredDeadlinesAndResetAlarm() {
        synchronized (this.mLock) {
            long latestRunTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
            int sourceUid = 0;
            String sourcePackageName = null;
            long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
            Iterator<JobStatus> it = this.mTrackedJobs.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                JobStatus next = it.next();
                if (next.hasDeadlineConstraint()) {
                    if (evaluateDeadlineConstraint(next, jMillis)) {
                        this.mStateChangedListener.onRunJobNow(next);
                        it.remove();
                    } else {
                        latestRunTimeElapsed = next.getLatestRunTimeElapsed();
                        sourceUid = next.getSourceUid();
                        sourcePackageName = next.getSourcePackageName();
                        break;
                    }
                }
            }
            setDeadlineExpiredAlarmLocked(latestRunTimeElapsed, deriveWorkSource(sourceUid, sourcePackageName));
        }
    }

    private boolean evaluateDeadlineConstraint(JobStatus jobStatus, long j) {
        if (jobStatus.getLatestRunTimeElapsed() <= j) {
            if (jobStatus.hasTimingDelayConstraint()) {
                jobStatus.setTimingDelayConstraintSatisfied(true);
            }
            jobStatus.setDeadlineConstraintSatisfied(true);
            return true;
        }
        return false;
    }

    private void checkExpiredDelaysAndResetAlarm() {
        synchronized (this.mLock) {
            long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
            long j = JobStatus.NO_LATEST_RUNTIME;
            Iterator<JobStatus> it = this.mTrackedJobs.iterator();
            boolean z = false;
            String sourcePackageName = null;
            int sourceUid = 0;
            while (it.hasNext()) {
                JobStatus next = it.next();
                if (next.hasTimingDelayConstraint()) {
                    if (evaluateTimingDelayConstraint(next, jMillis)) {
                        if (canStopTrackingJobLocked(next)) {
                            it.remove();
                        }
                        if (next.isReady()) {
                            z = true;
                        }
                    } else if (!next.isConstraintSatisfied(Integer.MIN_VALUE)) {
                        long earliestRunTime = next.getEarliestRunTime();
                        if (j > earliestRunTime) {
                            sourceUid = next.getSourceUid();
                            sourcePackageName = next.getSourcePackageName();
                            j = earliestRunTime;
                        }
                    }
                }
            }
            if (z) {
                this.mStateChangedListener.onControllerStateChanged();
            }
            setDelayExpiredAlarmLocked(j, deriveWorkSource(sourceUid, sourcePackageName));
        }
    }

    private WorkSource deriveWorkSource(int i, String str) {
        if (!this.mChainedAttributionEnabled) {
            return str == null ? new WorkSource(i) : new WorkSource(i, str);
        }
        WorkSource workSource = new WorkSource();
        workSource.createWorkChain().addNode(i, str).addNode(1000, JobSchedulerService.TAG);
        return workSource;
    }

    private boolean evaluateTimingDelayConstraint(JobStatus jobStatus, long j) {
        if (jobStatus.getEarliestRunTime() <= j) {
            jobStatus.setTimingDelayConstraintSatisfied(true);
            return true;
        }
        return false;
    }

    private void maybeUpdateAlarmsLocked(long j, long j2, WorkSource workSource) {
        if (j < this.mNextDelayExpiredElapsedMillis) {
            setDelayExpiredAlarmLocked(j, workSource);
        }
        if (j2 < this.mNextJobExpiredElapsedMillis) {
            setDeadlineExpiredAlarmLocked(j2, workSource);
        }
    }

    private void setDelayExpiredAlarmLocked(long j, WorkSource workSource) {
        this.mNextDelayExpiredElapsedMillis = maybeAdjustAlarmTime(j);
        updateAlarmWithListenerLocked("*job.delay*", this.mNextDelayExpiredListener, this.mNextDelayExpiredElapsedMillis, workSource);
    }

    private void setDeadlineExpiredAlarmLocked(long j, WorkSource workSource) {
        this.mNextJobExpiredElapsedMillis = maybeAdjustAlarmTime(j);
        updateAlarmWithListenerLocked("*job.deadline*", this.mDeadlineExpiredListener, this.mNextJobExpiredElapsedMillis, workSource);
    }

    private long maybeAdjustAlarmTime(long j) {
        long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (j < jMillis) {
            return jMillis;
        }
        return j;
    }

    private void updateAlarmWithListenerLocked(String str, AlarmManager.OnAlarmListener onAlarmListener, long j, WorkSource workSource) {
        String str2;
        ensureAlarmServiceLocked();
        if (j == JobStatus.NO_LATEST_RUNTIME) {
            this.mAlarmService.cancel(onAlarmListener);
            return;
        }
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("Setting ");
            str2 = str;
            sb.append(str2);
            sb.append(" for: ");
            sb.append(j);
            Slog.d(TAG, sb.toString());
        } else {
            str2 = str;
        }
        this.mAlarmService.set(2, j, -1L, 0L, str2, onAlarmListener, null, workSource);
    }

    @Override
    public void dumpControllerStateLocked(IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate) {
        long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        indentingPrintWriter.println("Elapsed clock: " + jMillis);
        indentingPrintWriter.print("Next delay alarm in ");
        TimeUtils.formatDuration(this.mNextDelayExpiredElapsedMillis, jMillis, indentingPrintWriter);
        indentingPrintWriter.println();
        indentingPrintWriter.print("Next deadline alarm in ");
        TimeUtils.formatDuration(this.mNextJobExpiredElapsedMillis, jMillis, indentingPrintWriter);
        indentingPrintWriter.println();
        indentingPrintWriter.println();
        for (JobStatus jobStatus : this.mTrackedJobs) {
            if (predicate.test(jobStatus)) {
                indentingPrintWriter.print("#");
                jobStatus.printUniqueId(indentingPrintWriter);
                indentingPrintWriter.print(" from ");
                UserHandle.formatUid(indentingPrintWriter, jobStatus.getSourceUid());
                indentingPrintWriter.print(": Delay=");
                if (jobStatus.hasTimingDelayConstraint()) {
                    TimeUtils.formatDuration(jobStatus.getEarliestRunTime(), jMillis, indentingPrintWriter);
                } else {
                    indentingPrintWriter.print("N/A");
                }
                indentingPrintWriter.print(", Deadline=");
                if (jobStatus.hasDeadlineConstraint()) {
                    TimeUtils.formatDuration(jobStatus.getLatestRunTimeElapsed(), jMillis, indentingPrintWriter);
                } else {
                    indentingPrintWriter.print("N/A");
                }
                indentingPrintWriter.println();
            }
        }
    }

    @Override
    public void dumpControllerStateLocked(ProtoOutputStream protoOutputStream, long j, Predicate<JobStatus> predicate) {
        long jStart = protoOutputStream.start(j);
        long jStart2 = protoOutputStream.start(1146756268040L);
        long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        protoOutputStream.write(1112396529665L, jMillis);
        protoOutputStream.write(1112396529666L, this.mNextDelayExpiredElapsedMillis - jMillis);
        protoOutputStream.write(1112396529667L, this.mNextJobExpiredElapsedMillis - jMillis);
        for (JobStatus jobStatus : this.mTrackedJobs) {
            if (predicate.test(jobStatus)) {
                long jStart3 = protoOutputStream.start(2246267895812L);
                jobStatus.writeToShortProto(protoOutputStream, 1146756268033L);
                protoOutputStream.write(1133871366147L, jobStatus.hasTimingDelayConstraint());
                protoOutputStream.write(1112396529668L, jobStatus.getEarliestRunTime() - jMillis);
                protoOutputStream.write(1133871366149L, jobStatus.hasDeadlineConstraint());
                protoOutputStream.write(1112396529670L, jobStatus.getLatestRunTimeElapsed() - jMillis);
                protoOutputStream.end(jStart3);
            }
        }
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }
}
