package com.android.server.job.controllers;

import android.app.AlarmManager;
import android.content.Context;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateChangedListener;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class TimeController extends StateController {
    private static final String TAG = "JobScheduler.Time";
    private static TimeController mSingleton;
    private final String DEADLINE_TAG;
    private final String DELAY_TAG;
    private AlarmManager mAlarmService;
    private final AlarmManager.OnAlarmListener mDeadlineExpiredListener;
    private long mNextDelayExpiredElapsedMillis;
    private final AlarmManager.OnAlarmListener mNextDelayExpiredListener;
    private long mNextJobExpiredElapsedMillis;
    private final List<JobStatus> mTrackedJobs;

    public static synchronized TimeController get(JobSchedulerService jms) {
        if (mSingleton == null) {
            mSingleton = new TimeController(jms, jms.getContext(), jms.getLock());
        }
        return mSingleton;
    }

    private TimeController(StateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
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
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus job, JobStatus lastJob) {
        long latestRunTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
        if (!job.hasTimingDelayConstraint() && !job.hasDeadlineConstraint()) {
            return;
        }
        maybeStopTrackingJobLocked(job, null, false);
        boolean isInsert = false;
        ListIterator<JobStatus> it = this.mTrackedJobs.listIterator(this.mTrackedJobs.size());
        while (true) {
            if (!it.hasPrevious()) {
                break;
            }
            JobStatus ts = it.previous();
            if (ts.getLatestRunTimeElapsed() < job.getLatestRunTimeElapsed()) {
                isInsert = true;
                break;
            }
        }
        if (isInsert) {
            it.next();
        }
        it.add(job);
        long earliestRunTime = job.hasTimingDelayConstraint() ? job.getEarliestRunTime() : Long.MAX_VALUE;
        if (job.hasDeadlineConstraint()) {
            latestRunTimeElapsed = job.getLatestRunTimeElapsed();
        }
        maybeUpdateAlarmsLocked(earliestRunTime, latestRunTimeElapsed, job.getSourceUid());
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus job, JobStatus incomingJob, boolean forUpdate) {
        if (!this.mTrackedJobs.remove(job)) {
            return;
        }
        checkExpiredDelaysAndResetAlarm();
        checkExpiredDeadlinesAndResetAlarm();
    }

    private boolean canStopTrackingJobLocked(JobStatus job) {
        boolean z = true;
        if (job.hasTimingDelayConstraint() && (job.satisfiedConstraints & 2) == 0) {
            return false;
        }
        if (job.hasDeadlineConstraint() && (job.satisfiedConstraints & 4) == 0) {
            z = false;
        }
        return z;
    }

    private void ensureAlarmServiceLocked() {
        if (this.mAlarmService != null) {
            return;
        }
        this.mAlarmService = (AlarmManager) this.mContext.getSystemService("alarm");
    }

    private void checkExpiredDeadlinesAndResetAlarm() {
        synchronized (this.mLock) {
            long nextExpiryTime = JobStatus.NO_LATEST_RUNTIME;
            int nextExpiryUid = 0;
            long nowElapsedMillis = SystemClock.elapsedRealtime();
            Iterator<JobStatus> it = this.mTrackedJobs.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                JobStatus job = it.next();
                if (job.hasDeadlineConstraint()) {
                    long jobDeadline = job.getLatestRunTimeElapsed();
                    if (jobDeadline <= nowElapsedMillis) {
                        if (job.hasTimingDelayConstraint()) {
                            job.setTimingDelayConstraintSatisfied(true);
                        }
                        job.setDeadlineConstraintSatisfied(true);
                        this.mStateChangedListener.onRunJobNow(job);
                        it.remove();
                    } else {
                        nextExpiryTime = jobDeadline;
                        nextExpiryUid = job.getSourceUid();
                        break;
                    }
                }
            }
            setDeadlineExpiredAlarmLocked(nextExpiryTime, nextExpiryUid);
        }
    }

    private void checkExpiredDelaysAndResetAlarm() {
        synchronized (this.mLock) {
            long nowElapsedMillis = SystemClock.elapsedRealtime();
            long nextDelayTime = JobStatus.NO_LATEST_RUNTIME;
            int nextDelayUid = 0;
            boolean ready = false;
            Iterator<JobStatus> it = this.mTrackedJobs.iterator();
            while (it.hasNext()) {
                JobStatus job = it.next();
                if (job.hasTimingDelayConstraint()) {
                    long jobDelayTime = job.getEarliestRunTime();
                    if (jobDelayTime <= nowElapsedMillis) {
                        job.setTimingDelayConstraintSatisfied(true);
                        if (canStopTrackingJobLocked(job)) {
                            it.remove();
                        }
                        if (job.isReady()) {
                            ready = true;
                        }
                    } else if (!job.isConstraintSatisfied(2) && nextDelayTime > jobDelayTime) {
                        nextDelayTime = jobDelayTime;
                        nextDelayUid = job.getSourceUid();
                    }
                }
            }
            if (ready) {
                this.mStateChangedListener.onControllerStateChanged();
            }
            setDelayExpiredAlarmLocked(nextDelayTime, nextDelayUid);
        }
    }

    private void maybeUpdateAlarmsLocked(long delayExpiredElapsed, long deadlineExpiredElapsed, int uid) {
        if (delayExpiredElapsed < this.mNextDelayExpiredElapsedMillis) {
            setDelayExpiredAlarmLocked(delayExpiredElapsed, uid);
        }
        if (deadlineExpiredElapsed >= this.mNextJobExpiredElapsedMillis) {
            return;
        }
        setDeadlineExpiredAlarmLocked(deadlineExpiredElapsed, uid);
    }

    private void setDelayExpiredAlarmLocked(long alarmTimeElapsedMillis, int uid) {
        this.mNextDelayExpiredElapsedMillis = maybeAdjustAlarmTime(alarmTimeElapsedMillis);
        updateAlarmWithListenerLocked("*job.delay*", this.mNextDelayExpiredListener, this.mNextDelayExpiredElapsedMillis, uid);
    }

    private void setDeadlineExpiredAlarmLocked(long alarmTimeElapsedMillis, int uid) {
        this.mNextJobExpiredElapsedMillis = maybeAdjustAlarmTime(alarmTimeElapsedMillis);
        updateAlarmWithListenerLocked("*job.deadline*", this.mDeadlineExpiredListener, this.mNextJobExpiredElapsedMillis, uid);
    }

    private long maybeAdjustAlarmTime(long proposedAlarmTimeElapsedMillis) {
        long earliestWakeupTimeElapsed = SystemClock.elapsedRealtime();
        if (proposedAlarmTimeElapsedMillis < earliestWakeupTimeElapsed) {
            return earliestWakeupTimeElapsed;
        }
        return proposedAlarmTimeElapsedMillis;
    }

    private void updateAlarmWithListenerLocked(String tag, AlarmManager.OnAlarmListener listener, long alarmTimeElapsed, int uid) {
        ensureAlarmServiceLocked();
        if (alarmTimeElapsed == JobStatus.NO_LATEST_RUNTIME) {
            this.mAlarmService.cancel(listener);
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "Setting " + tag + " for: " + alarmTimeElapsed);
        }
        this.mAlarmService.set(2, alarmTimeElapsed, -1L, 0L, tag, listener, null, new WorkSource(uid));
    }

    @Override
    public void dumpControllerStateLocked(PrintWriter pw, int filterUid) {
        long nowElapsed = SystemClock.elapsedRealtime();
        pw.print("Alarms: now=");
        pw.print(SystemClock.elapsedRealtime());
        pw.println();
        pw.print("Next delay alarm in ");
        TimeUtils.formatDuration(this.mNextDelayExpiredElapsedMillis, nowElapsed, pw);
        pw.println();
        pw.print("Next deadline alarm in ");
        TimeUtils.formatDuration(this.mNextJobExpiredElapsedMillis, nowElapsed, pw);
        pw.println();
        pw.print("Tracking ");
        pw.print(this.mTrackedJobs.size());
        pw.println(":");
        for (JobStatus ts : this.mTrackedJobs) {
            if (ts.shouldDump(filterUid)) {
                pw.print("  #");
                ts.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, ts.getSourceUid());
                pw.print(": Delay=");
                if (ts.hasTimingDelayConstraint()) {
                    TimeUtils.formatDuration(ts.getEarliestRunTime(), nowElapsed, pw);
                } else {
                    pw.print("N/A");
                }
                pw.print(", Deadline=");
                if (ts.hasDeadlineConstraint()) {
                    TimeUtils.formatDuration(ts.getLatestRunTimeElapsed(), nowElapsed, pw);
                } else {
                    pw.print("N/A");
                }
                pw.println();
            }
        }
    }
}
