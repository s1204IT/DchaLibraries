package com.android.settings.fuelgauge.batterytip;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;
import java.util.concurrent.TimeUnit;

public class AnomalyCleanupJobService extends JobService {
    static final long CLEAN_UP_FREQUENCY_MS = TimeUnit.DAYS.toMillis(1);

    public static void scheduleCleanUp(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        JobInfo.Builder persisted = new JobInfo.Builder(R.integer.job_anomaly_clean_up, new ComponentName(context, (Class<?>) AnomalyCleanupJobService.class)).setPeriodic(CLEAN_UP_FREQUENCY_MS).setRequiresDeviceIdle(true).setRequiresCharging(true).setPersisted(true);
        if (jobScheduler.getPendingJob(R.integer.job_anomaly_clean_up) == null && jobScheduler.schedule(persisted.build()) != 1) {
            Log.i("AnomalyCleanUpJobService", "Anomaly clean up job service schedule failed.");
        }
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        final BatteryDatabaseManager batteryDatabaseManager = BatteryDatabaseManager.getInstance(this);
        final BatteryTipPolicy batteryTipPolicy = new BatteryTipPolicy(this);
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public final void run() {
                AnomalyCleanupJobService.lambda$onStartJob$0(this.f$0, batteryDatabaseManager, batteryTipPolicy, jobParameters);
            }
        });
        return true;
    }

    public static void lambda$onStartJob$0(AnomalyCleanupJobService anomalyCleanupJobService, BatteryDatabaseManager batteryDatabaseManager, BatteryTipPolicy batteryTipPolicy, JobParameters jobParameters) {
        batteryDatabaseManager.deleteAllAnomaliesBeforeTimeStamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(batteryTipPolicy.dataHistoryRetainDay));
        anomalyCleanupJobService.jobFinished(jobParameters, false);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
