package com.android.server.job.controllers;

import android.app.usage.UsageStatsManagerInternal;
import android.content.Context;
import android.os.UserHandle;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.JobStore;
import java.io.PrintWriter;

public class AppIdleController extends StateController {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AppIdleController";
    private static volatile AppIdleController sController;
    private static Object sCreationLock = new Object();
    boolean mAppIdleParoleOn;
    private boolean mInitializedParoleOn;
    private final JobSchedulerService mJobSchedulerService;
    private final UsageStatsManagerInternal mUsageStatsInternal;

    final class GlobalUpdateFunc implements JobStore.JobStatusFunctor {
        boolean mChanged;

        GlobalUpdateFunc() {
        }

        @Override
        public void process(JobStatus jobStatus) {
            String packageName = jobStatus.getSourcePackageName();
            boolean appIdle = !AppIdleController.this.mAppIdleParoleOn ? AppIdleController.this.mUsageStatsInternal.isAppIdle(packageName, jobStatus.getSourceUid(), jobStatus.getSourceUserId()) : false;
            if (!jobStatus.setAppNotIdleConstraintSatisfied(appIdle ? false : true)) {
                return;
            }
            this.mChanged = true;
        }
    }

    static final class PackageUpdateFunc implements JobStore.JobStatusFunctor {
        boolean mChanged;
        final boolean mIdle;
        final String mPackage;
        final int mUserId;

        PackageUpdateFunc(int userId, String pkg, boolean idle) {
            this.mUserId = userId;
            this.mPackage = pkg;
            this.mIdle = idle;
        }

        @Override
        public void process(JobStatus jobStatus) {
            if (jobStatus.getSourcePackageName().equals(this.mPackage) && jobStatus.getSourceUserId() == this.mUserId) {
                if (!jobStatus.setAppNotIdleConstraintSatisfied(!this.mIdle)) {
                    return;
                }
                this.mChanged = true;
            }
        }
    }

    public static AppIdleController get(JobSchedulerService service) {
        AppIdleController appIdleController;
        synchronized (sCreationLock) {
            if (sController == null) {
                sController = new AppIdleController(service, service.getContext(), service.getLock());
            }
            appIdleController = sController;
        }
        return appIdleController;
    }

    private AppIdleController(JobSchedulerService service, Context context, Object lock) {
        super(service, context, lock);
        this.mJobSchedulerService = service;
        this.mUsageStatsInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        this.mAppIdleParoleOn = true;
        this.mUsageStatsInternal.addAppIdleStateChangeListener(new AppIdleStateChangeListener(this, null));
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        if (!this.mInitializedParoleOn) {
            this.mInitializedParoleOn = true;
            this.mAppIdleParoleOn = this.mUsageStatsInternal.isAppIdleParoleOn();
        }
        String packageName = jobStatus.getSourcePackageName();
        boolean appIdle = !this.mAppIdleParoleOn ? this.mUsageStatsInternal.isAppIdle(packageName, jobStatus.getSourceUid(), jobStatus.getSourceUserId()) : false;
        jobStatus.setAppNotIdleConstraintSatisfied(appIdle ? false : true);
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
    }

    @Override
    public void dumpControllerStateLocked(final PrintWriter pw, final int filterUid) {
        pw.print("AppIdle: parole on = ");
        pw.println(this.mAppIdleParoleOn);
        this.mJobSchedulerService.getJobStore().forEachJob(new JobStore.JobStatusFunctor() {
            @Override
            public void process(JobStatus jobStatus) {
                if (!jobStatus.shouldDump(filterUid)) {
                    return;
                }
                pw.print("  #");
                jobStatus.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, jobStatus.getSourceUid());
                pw.print(": ");
                pw.print(jobStatus.getSourcePackageName());
                if ((jobStatus.satisfiedConstraints & 64) != 0) {
                    pw.println(" RUNNABLE");
                } else {
                    pw.println(" WAITING");
                }
            }
        });
    }

    void setAppIdleParoleOn(boolean isAppIdleParoleOn) {
        boolean changed = false;
        synchronized (this.mLock) {
            if (this.mAppIdleParoleOn == isAppIdleParoleOn) {
                return;
            }
            this.mAppIdleParoleOn = isAppIdleParoleOn;
            GlobalUpdateFunc update = new GlobalUpdateFunc();
            this.mJobSchedulerService.getJobStore().forEachJob(update);
            if (update.mChanged) {
                changed = true;
            }
            if (!changed) {
                return;
            }
            this.mStateChangedListener.onControllerStateChanged();
        }
    }

    private class AppIdleStateChangeListener extends UsageStatsManagerInternal.AppIdleStateChangeListener {
        AppIdleStateChangeListener(AppIdleController this$0, AppIdleStateChangeListener appIdleStateChangeListener) {
            this();
        }

        private AppIdleStateChangeListener() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle) {
            boolean changed = false;
            synchronized (AppIdleController.this.mLock) {
                if (AppIdleController.this.mAppIdleParoleOn) {
                    return;
                }
                PackageUpdateFunc update = new PackageUpdateFunc(userId, packageName, idle);
                AppIdleController.this.mJobSchedulerService.getJobStore().forEachJob(update);
                if (update.mChanged) {
                    changed = true;
                }
                if (!changed) {
                    return;
                }
                AppIdleController.this.mStateChangedListener.onControllerStateChanged();
            }
        }

        public void onParoleStateChanged(boolean isParoleOn) {
            AppIdleController.this.setAppIdleParoleOn(isParoleOn);
        }
    }
}
