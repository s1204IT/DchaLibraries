package com.android.server.backup;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import com.android.server.pm.PackageManagerService;

public class FullBackupJob extends JobService {
    private static final boolean DEBUG = true;
    private static final int JOB_ID = 20536;
    private static final String TAG = "FullBackupJob";
    private static ComponentName sIdleService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, FullBackupJob.class.getName());
    JobParameters mParams;

    public static void schedule(Context context, long j, BackupManagerConstants backupManagerConstants) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, sIdleService);
        synchronized (backupManagerConstants) {
            builder.setRequiresDeviceIdle(true).setRequiredNetworkType(backupManagerConstants.getFullBackupRequiredNetworkType()).setRequiresCharging(backupManagerConstants.getFullBackupRequireCharging());
        }
        if (j > 0) {
            builder.setMinimumLatency(j);
        }
        jobScheduler.schedule(builder.build());
    }

    public void finishBackupPass() {
        if (this.mParams != null) {
            jobFinished(this.mParams, false);
            this.mParams = null;
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        this.mParams = jobParameters;
        return BackupManagerService.getInstance().beginFullBackup(this);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (this.mParams != null) {
            this.mParams = null;
            BackupManagerService.getInstance().endFullBackup();
            return false;
        }
        return false;
    }
}
