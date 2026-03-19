package com.android.server.job.controllers;

import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.AppStateTracker;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.JobStore;
import com.android.server.pm.DumpState;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class BackgroundJobsController extends StateController {
    private static final boolean DEBUG;
    static final int KNOWN_ACTIVE = 1;
    static final int KNOWN_INACTIVE = 2;
    private static final String TAG = "JobScheduler.Background";
    static final int UNKNOWN = 0;
    private final AppStateTracker mAppStateTracker;
    private final AppStateTracker.Listener mForceAppStandbyListener;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public BackgroundJobsController(JobSchedulerService jobSchedulerService) {
        super(jobSchedulerService);
        this.mForceAppStandbyListener = new AppStateTracker.Listener() {
            @Override
            public void updateAllJobs() {
                synchronized (BackgroundJobsController.this.mLock) {
                    BackgroundJobsController.this.updateAllJobRestrictionsLocked();
                }
            }

            @Override
            public void updateJobsForUid(int i, boolean z) {
                synchronized (BackgroundJobsController.this.mLock) {
                    BackgroundJobsController.this.updateJobRestrictionsForUidLocked(i, z);
                }
            }

            @Override
            public void updateJobsForUidPackage(int i, String str, boolean z) {
                synchronized (BackgroundJobsController.this.mLock) {
                    BackgroundJobsController.this.updateJobRestrictionsForUidLocked(i, z);
                }
            }
        };
        this.mAppStateTracker = (AppStateTracker) Preconditions.checkNotNull((AppStateTracker) LocalServices.getService(AppStateTracker.class));
        this.mAppStateTracker.addListener(this.mForceAppStandbyListener);
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        updateSingleJobRestrictionLocked(jobStatus, 0);
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
    }

    @Override
    public void dumpControllerStateLocked(final IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate) {
        this.mAppStateTracker.dump(indentingPrintWriter);
        indentingPrintWriter.println();
        this.mService.getJobStore().forEachJob(predicate, new Consumer() {
            @Override
            public final void accept(Object obj) {
                BackgroundJobsController.lambda$dumpControllerStateLocked$0(this.f$0, indentingPrintWriter, (JobStatus) obj);
            }
        });
    }

    public static void lambda$dumpControllerStateLocked$0(BackgroundJobsController backgroundJobsController, IndentingPrintWriter indentingPrintWriter, JobStatus jobStatus) {
        int sourceUid = jobStatus.getSourceUid();
        String sourcePackageName = jobStatus.getSourcePackageName();
        indentingPrintWriter.print("#");
        jobStatus.printUniqueId(indentingPrintWriter);
        indentingPrintWriter.print(" from ");
        UserHandle.formatUid(indentingPrintWriter, sourceUid);
        indentingPrintWriter.print(backgroundJobsController.mAppStateTracker.isUidActive(sourceUid) ? " active" : " idle");
        if (backgroundJobsController.mAppStateTracker.isUidPowerSaveWhitelisted(sourceUid) || backgroundJobsController.mAppStateTracker.isUidTempPowerSaveWhitelisted(sourceUid)) {
            indentingPrintWriter.print(", whitelisted");
        }
        indentingPrintWriter.print(": ");
        indentingPrintWriter.print(sourcePackageName);
        indentingPrintWriter.print(" [RUN_ANY_IN_BACKGROUND ");
        indentingPrintWriter.print(backgroundJobsController.mAppStateTracker.isRunAnyInBackgroundAppOpsAllowed(sourceUid, sourcePackageName) ? "allowed]" : "disallowed]");
        if ((jobStatus.satisfiedConstraints & DumpState.DUMP_CHANGES) != 0) {
            indentingPrintWriter.println(" RUNNABLE");
        } else {
            indentingPrintWriter.println(" WAITING");
        }
    }

    @Override
    public void dumpControllerStateLocked(final ProtoOutputStream protoOutputStream, long j, Predicate<JobStatus> predicate) {
        long jStart = protoOutputStream.start(j);
        long jStart2 = protoOutputStream.start(1146756268033L);
        this.mAppStateTracker.dumpProto(protoOutputStream, 1146756268033L);
        this.mService.getJobStore().forEachJob(predicate, new Consumer() {
            @Override
            public final void accept(Object obj) {
                BackgroundJobsController.lambda$dumpControllerStateLocked$1(this.f$0, protoOutputStream, (JobStatus) obj);
            }
        });
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }

    public static void lambda$dumpControllerStateLocked$1(BackgroundJobsController backgroundJobsController, ProtoOutputStream protoOutputStream, JobStatus jobStatus) {
        long jStart = protoOutputStream.start(2246267895810L);
        jobStatus.writeToShortProto(protoOutputStream, 1146756268033L);
        int sourceUid = jobStatus.getSourceUid();
        protoOutputStream.write(1120986464258L, sourceUid);
        String sourcePackageName = jobStatus.getSourcePackageName();
        protoOutputStream.write(1138166333443L, sourcePackageName);
        protoOutputStream.write(1133871366148L, backgroundJobsController.mAppStateTracker.isUidActive(sourceUid));
        protoOutputStream.write(1133871366149L, backgroundJobsController.mAppStateTracker.isUidPowerSaveWhitelisted(sourceUid) || backgroundJobsController.mAppStateTracker.isUidTempPowerSaveWhitelisted(sourceUid));
        protoOutputStream.write(1133871366150L, backgroundJobsController.mAppStateTracker.isRunAnyInBackgroundAppOpsAllowed(sourceUid, sourcePackageName));
        protoOutputStream.write(1133871366151L, (jobStatus.satisfiedConstraints & DumpState.DUMP_CHANGES) != 0);
        protoOutputStream.end(jStart);
    }

    private void updateAllJobRestrictionsLocked() {
        updateJobRestrictionsLocked(-1, 0);
    }

    private void updateJobRestrictionsForUidLocked(int i, boolean z) {
        updateJobRestrictionsLocked(i, z ? 1 : 2);
    }

    private void updateJobRestrictionsLocked(int i, int i2) {
        UpdateJobFunctor updateJobFunctor = new UpdateJobFunctor(i2);
        long jElapsedRealtimeNanos = DEBUG ? SystemClock.elapsedRealtimeNanos() : 0L;
        JobStore jobStore = this.mService.getJobStore();
        if (i > 0) {
            jobStore.forEachJobForSourceUid(i, updateJobFunctor);
        } else {
            jobStore.forEachJob(updateJobFunctor);
        }
        long jElapsedRealtimeNanos2 = DEBUG ? SystemClock.elapsedRealtimeNanos() - jElapsedRealtimeNanos : 0L;
        if (DEBUG) {
            Slog.d(TAG, String.format("Job status updated: %d/%d checked/total jobs, %d us", Integer.valueOf(updateJobFunctor.mCheckedCount), Integer.valueOf(updateJobFunctor.mTotalCount), Long.valueOf(jElapsedRealtimeNanos2 / 1000)));
        }
        if (updateJobFunctor.mChanged) {
            this.mStateChangedListener.onControllerStateChanged();
        }
    }

    boolean updateSingleJobRestrictionLocked(JobStatus jobStatus, int i) {
        boolean zIsUidActive;
        int sourceUid = jobStatus.getSourceUid();
        boolean z = !this.mAppStateTracker.areJobsRestricted(sourceUid, jobStatus.getSourcePackageName(), (jobStatus.getInternalFlags() & 1) != 0);
        if (i == 0) {
            zIsUidActive = this.mAppStateTracker.isUidActive(sourceUid);
        } else {
            zIsUidActive = i == 1;
        }
        return jobStatus.setUidActive(zIsUidActive) | jobStatus.setBackgroundNotRestrictedConstraintSatisfied(z);
    }

    private final class UpdateJobFunctor implements Consumer<JobStatus> {
        final int activeState;
        boolean mChanged = false;
        int mTotalCount = 0;
        int mCheckedCount = 0;

        public UpdateJobFunctor(int i) {
            this.activeState = i;
        }

        @Override
        public void accept(JobStatus jobStatus) {
            this.mTotalCount++;
            this.mCheckedCount++;
            if (BackgroundJobsController.this.updateSingleJobRestrictionLocked(jobStatus, this.activeState)) {
                this.mChanged = true;
            }
        }
    }
}
