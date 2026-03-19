package com.android.server.timezone;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;

public final class TimeZoneUpdateIdler extends JobService {
    private static final String TAG = "timezone.TimeZoneUpdateIdler";
    private static final int TIME_ZONE_UPDATE_IDLE_JOB_ID = 27042305;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        RulesManagerService rulesManagerService = (RulesManagerService) LocalServices.getService(RulesManagerService.class);
        Slog.d(TAG, "onStartJob() called");
        rulesManagerService.notifyIdle();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        boolean z = jobParameters.getStopReason() != 0;
        Slog.d(TAG, "onStopJob() called: Reschedule=" + z);
        return z;
    }

    public static void schedule(Context context, long j) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo.Builder minimumLatency = new JobInfo.Builder(TIME_ZONE_UPDATE_IDLE_JOB_ID, new ComponentName(context, (Class<?>) TimeZoneUpdateIdler.class)).setRequiresDeviceIdle(true).setRequiresCharging(true).setMinimumLatency(j);
        Slog.d(TAG, "schedule() called: minimumDelayMillis=" + j);
        jobScheduler.schedule(minimumLatency.build());
    }

    public static void unschedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        Slog.d(TAG, "unschedule() called");
        jobScheduler.cancel(TIME_ZONE_UPDATE_IDLE_JOB_ID);
    }
}
