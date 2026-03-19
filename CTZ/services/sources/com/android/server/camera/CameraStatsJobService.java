package com.android.server.camera;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerService;
import java.util.concurrent.TimeUnit;

public class CameraStatsJobService extends JobService {
    private static final int CAMERA_REPORTING_JOB_ID = 13254266;
    private static final String TAG = "CameraStatsJobService";
    private static ComponentName sCameraStatsJobServiceName = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, CameraStatsJobService.class.getName());

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        CameraServiceProxy cameraServiceProxy = (CameraServiceProxy) LocalServices.getService(CameraServiceProxy.class);
        if (cameraServiceProxy == null) {
            Slog.w(TAG, "Can't collect camera usage stats - no camera service proxy found");
            return false;
        }
        cameraServiceProxy.dumpUsageEvents();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public static void schedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        if (jobScheduler == null) {
            Slog.e(TAG, "Can't collect camera usage stats - no Job Scheduler");
        } else {
            jobScheduler.schedule(new JobInfo.Builder(CAMERA_REPORTING_JOB_ID, sCameraStatsJobServiceName).setMinimumLatency(TimeUnit.DAYS.toMillis(1L)).setRequiresDeviceIdle(true).build());
        }
    }
}
