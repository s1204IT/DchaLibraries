package com.android.server.job.controllers;

import android.content.Context;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateChangedListener;
import java.util.function.Predicate;

public abstract class StateController {
    protected final JobSchedulerService.Constants mConstants;
    protected final Context mContext;
    protected final Object mLock;
    protected final JobSchedulerService mService;
    protected final StateChangedListener mStateChangedListener;

    public abstract void dumpControllerStateLocked(ProtoOutputStream protoOutputStream, long j, Predicate<JobStatus> predicate);

    public abstract void dumpControllerStateLocked(IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate);

    public abstract void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2);

    public abstract void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z);

    StateController(JobSchedulerService jobSchedulerService) {
        this.mService = jobSchedulerService;
        this.mStateChangedListener = jobSchedulerService;
        this.mContext = jobSchedulerService.getTestableContext();
        this.mLock = jobSchedulerService.getLock();
        this.mConstants = jobSchedulerService.getConstants();
    }

    public void prepareForExecutionLocked(JobStatus jobStatus) {
    }

    public void rescheduleForFailureLocked(JobStatus jobStatus, JobStatus jobStatus2) {
    }
}
