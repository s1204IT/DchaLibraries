package com.android.settings.fuelgauge.batterytip;

import android.app.StatsManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;
import java.util.concurrent.TimeUnit;

public class AnomalyConfigJobService extends JobService {
    static final long CONFIG_UPDATE_FREQUENCY_MS = TimeUnit.DAYS.toMillis(1);

    public static void scheduleConfigUpdate(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        JobInfo.Builder persisted = new JobInfo.Builder(R.integer.job_anomaly_config_update, new ComponentName(context, (Class<?>) AnomalyConfigJobService.class)).setPeriodic(CONFIG_UPDATE_FREQUENCY_MS).setRequiresDeviceIdle(true).setRequiresCharging(true).setPersisted(true);
        if (jobScheduler.getPendingJob(R.integer.job_anomaly_config_update) == null && jobScheduler.schedule(persisted.build()) != 1) {
            Log.i("AnomalyConfigJobService", "Anomaly config update job service schedule failed.");
        }
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public final void run() {
                AnomalyConfigJobService.lambda$onStartJob$0(this.f$0, jobParameters);
            }
        });
        return true;
    }

    public static void lambda$onStartJob$0(AnomalyConfigJobService anomalyConfigJobService, JobParameters jobParameters) {
        StatsManager statsManager = (StatsManager) anomalyConfigJobService.getSystemService(StatsManager.class);
        anomalyConfigJobService.checkAnomalyConfig(statsManager);
        try {
            BatteryTipUtils.uploadAnomalyPendingIntent(anomalyConfigJobService, statsManager);
        } catch (StatsManager.StatsUnavailableException e) {
            Log.w("AnomalyConfigJobService", "Failed to uploadAnomalyPendingIntent.", e);
        }
        anomalyConfigJobService.jobFinished(jobParameters, false);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    synchronized void checkAnomalyConfig(StatsManager statsManager) {
        SharedPreferences sharedPreferences = getSharedPreferences("anomaly_pref", 0);
        int i = sharedPreferences.getInt("anomaly_config_version", 0);
        int i2 = Settings.Global.getInt(getContentResolver(), "anomaly_config_version", 0);
        String string = Settings.Global.getString(getContentResolver(), "anomaly_config");
        Log.i("AnomalyConfigJobService", "CurrentVersion: " + i + " new version: " + i2);
        if (i2 > i) {
            try {
                statsManager.removeConfig(1L);
            } catch (StatsManager.StatsUnavailableException e) {
                Log.i("AnomalyConfigJobService", "When updating anomaly config, failed to first remove the old config 1", e);
            }
            if (!TextUtils.isEmpty(string)) {
                try {
                    statsManager.addConfig(1L, Base64.decode(string, 0));
                    Log.i("AnomalyConfigJobService", "Upload the anomaly config. configKey: 1");
                    SharedPreferences.Editor editorEdit = sharedPreferences.edit();
                    editorEdit.putInt("anomaly_config_version", i2);
                    editorEdit.commit();
                } catch (StatsManager.StatsUnavailableException e2) {
                    Log.i("AnomalyConfigJobService", "Upload of anomaly config failed for configKey 1", e2);
                } catch (IllegalArgumentException e3) {
                    Log.e("AnomalyConfigJobService", "Anomaly raw config is in wrong format", e3);
                }
            }
        }
    }
}
