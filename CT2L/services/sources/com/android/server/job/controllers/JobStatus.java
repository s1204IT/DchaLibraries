package com.android.server.job.controllers;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.format.DateUtils;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobStatus {
    public static final long NO_EARLIEST_RUNTIME = 0;
    public static final long NO_LATEST_RUNTIME = Long.MAX_VALUE;
    final AtomicBoolean chargingConstraintSatisfied;
    final AtomicBoolean connectivityConstraintSatisfied;
    final AtomicBoolean deadlineConstraintSatisfied;
    private long earliestRunTimeElapsedMillis;
    final AtomicBoolean idleConstraintSatisfied;
    final JobInfo job;
    private long latestRunTimeElapsedMillis;
    final String name;
    private final int numFailures;
    final String tag;
    final AtomicBoolean timeDelayConstraintSatisfied;
    final int uId;
    final AtomicBoolean unmeteredConstraintSatisfied;

    public int getServiceToken() {
        return this.uId;
    }

    private JobStatus(JobInfo job, int uId, int numFailures) {
        this.chargingConstraintSatisfied = new AtomicBoolean();
        this.timeDelayConstraintSatisfied = new AtomicBoolean();
        this.deadlineConstraintSatisfied = new AtomicBoolean();
        this.idleConstraintSatisfied = new AtomicBoolean();
        this.unmeteredConstraintSatisfied = new AtomicBoolean();
        this.connectivityConstraintSatisfied = new AtomicBoolean();
        this.job = job;
        this.uId = uId;
        this.name = job.getService().flattenToShortString();
        this.tag = "*job*/" + this.name;
        this.numFailures = numFailures;
    }

    public JobStatus(JobInfo job, int uId) {
        this(job, uId, 0);
        long elapsedNow = SystemClock.elapsedRealtime();
        if (job.isPeriodic()) {
            this.earliestRunTimeElapsedMillis = elapsedNow;
            this.latestRunTimeElapsedMillis = job.getIntervalMillis() + elapsedNow;
        } else {
            this.earliestRunTimeElapsedMillis = job.hasEarlyConstraint() ? job.getMinLatencyMillis() + elapsedNow : 0L;
            this.latestRunTimeElapsedMillis = job.hasLateConstraint() ? job.getMaxExecutionDelayMillis() + elapsedNow : NO_LATEST_RUNTIME;
        }
    }

    public JobStatus(JobInfo job, int uId, long earliestRunTimeElapsedMillis, long latestRunTimeElapsedMillis) {
        this(job, uId, 0);
        this.earliestRunTimeElapsedMillis = earliestRunTimeElapsedMillis;
        this.latestRunTimeElapsedMillis = latestRunTimeElapsedMillis;
    }

    public JobStatus(JobStatus rescheduling, long newEarliestRuntimeElapsedMillis, long newLatestRuntimeElapsedMillis, int backoffAttempt) {
        this(rescheduling.job, rescheduling.getUid(), backoffAttempt);
        this.earliestRunTimeElapsedMillis = newEarliestRuntimeElapsedMillis;
        this.latestRunTimeElapsedMillis = newLatestRuntimeElapsedMillis;
    }

    public JobInfo getJob() {
        return this.job;
    }

    public int getJobId() {
        return this.job.getId();
    }

    public int getNumFailures() {
        return this.numFailures;
    }

    public ComponentName getServiceComponent() {
        return this.job.getService();
    }

    public int getUserId() {
        return UserHandle.getUserId(this.uId);
    }

    public int getUid() {
        return this.uId;
    }

    public String getName() {
        return this.name;
    }

    public String getTag() {
        return this.tag;
    }

    public PersistableBundle getExtras() {
        return this.job.getExtras();
    }

    public boolean hasConnectivityConstraint() {
        return this.job.getNetworkType() == 1;
    }

    public boolean hasUnmeteredConstraint() {
        return this.job.getNetworkType() == 2;
    }

    public boolean hasChargingConstraint() {
        return this.job.isRequireCharging();
    }

    public boolean hasTimingDelayConstraint() {
        return this.earliestRunTimeElapsedMillis != 0;
    }

    public boolean hasDeadlineConstraint() {
        return this.latestRunTimeElapsedMillis != NO_LATEST_RUNTIME;
    }

    public boolean hasIdleConstraint() {
        return this.job.isRequireDeviceIdle();
    }

    public boolean isPersisted() {
        return this.job.isPersisted();
    }

    public long getEarliestRunTime() {
        return this.earliestRunTimeElapsedMillis;
    }

    public long getLatestRunTimeElapsed() {
        return this.latestRunTimeElapsedMillis;
    }

    public synchronized boolean isReady() {
        boolean z;
        if (isConstraintsSatisfied()) {
            z = true;
        } else {
            if (hasDeadlineConstraint()) {
                if (this.deadlineConstraintSatisfied.get()) {
                }
            }
            z = false;
        }
        return z;
    }

    public synchronized boolean isConstraintsSatisfied() {
        boolean z;
        if ((!hasChargingConstraint() || this.chargingConstraintSatisfied.get()) && ((!hasTimingDelayConstraint() || this.timeDelayConstraintSatisfied.get()) && ((!hasConnectivityConstraint() || this.connectivityConstraintSatisfied.get()) && (!hasUnmeteredConstraint() || this.unmeteredConstraintSatisfied.get())))) {
            if (hasIdleConstraint()) {
                if (!this.idleConstraintSatisfied.get()) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
    }

    public boolean matches(int uid, int jobId) {
        return this.job.getId() == jobId && this.uId == uid;
    }

    public String toString() {
        return String.valueOf(hashCode()).substring(0, 3) + "..:[" + this.job.getService() + ",jId=" + this.job.getId() + ",u" + getUserId() + ",R=(" + formatRunTime(this.earliestRunTimeElapsedMillis, 0L) + "," + formatRunTime(this.latestRunTimeElapsedMillis, NO_LATEST_RUNTIME) + "),N=" + this.job.getNetworkType() + ",C=" + this.job.isRequireCharging() + ",I=" + this.job.isRequireDeviceIdle() + ",F=" + this.numFailures + ",P=" + this.job.isPersisted() + (isReady() ? "(READY)" : "") + "]";
    }

    private String formatRunTime(long runtime, long defaultValue) {
        if (runtime == defaultValue) {
            return "none";
        }
        long elapsedNow = SystemClock.elapsedRealtime();
        long nextRuntime = runtime - elapsedNow;
        if (nextRuntime > 0) {
            return DateUtils.formatElapsedTime(nextRuntime / 1000);
        }
        return "-" + DateUtils.formatElapsedTime(nextRuntime / (-1000));
    }

    public String toShortString() {
        return this.job.getService().flattenToShortString() + " jId=" + this.job.getId() + ", u" + getUserId();
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println(toString());
    }
}
