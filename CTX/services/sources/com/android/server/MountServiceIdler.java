package com.android.server;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.util.Calendar;

public class MountServiceIdler extends JobService {
    private static final String TAG = "MountServiceIdler";
    private Runnable mFinishCallback = new Runnable() {
        @Override
        public void run() {
            Slog.i(MountServiceIdler.TAG, "Got mount service completion callback");
            synchronized (MountServiceIdler.this.mFinishCallback) {
                if (MountServiceIdler.this.mStarted) {
                    MountServiceIdler.this.jobFinished(MountServiceIdler.this.mJobParams, false);
                    MountServiceIdler.this.mStarted = false;
                }
            }
            MountServiceIdler.scheduleIdlePass(MountServiceIdler.this);
        }
    };
    private JobParameters mJobParams;
    private boolean mStarted;
    private static ComponentName sIdleService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, MountServiceIdler.class.getName());
    private static int MOUNT_JOB_ID = 808;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        try {
            ActivityManager.getService().performIdleMaintenance();
        } catch (RemoteException e) {
        }
        this.mJobParams = jobParameters;
        StorageManagerService storageManagerService = StorageManagerService.sSelf;
        if (storageManagerService != null) {
            synchronized (this.mFinishCallback) {
                this.mStarted = true;
            }
            storageManagerService.runIdleMaint(this.mFinishCallback);
        }
        return storageManagerService != null;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        StorageManagerService storageManagerService = StorageManagerService.sSelf;
        if (storageManagerService != null) {
            storageManagerService.abortIdleMaint(this.mFinishCallback);
            synchronized (this.mFinishCallback) {
                this.mStarted = false;
            }
        }
        return false;
    }

    public static void scheduleIdlePass(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        long timeInMillis = tomorrowMidnight().getTimeInMillis() - System.currentTimeMillis();
        JobInfo.Builder builder = new JobInfo.Builder(MOUNT_JOB_ID, sIdleService);
        builder.setRequiresDeviceIdle(true);
        builder.setRequiresCharging(true);
        builder.setMinimumLatency(timeInMillis);
        jobScheduler.schedule(builder.build());
    }

    private static Calendar tomorrowMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(11, 3);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        calendar.add(5, 1);
        return calendar;
    }
}
