package com.android.settings.fuelgauge.batterytip;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StatsDimensionsValue;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;
import com.android.settingslib.utils.ThreadUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AnomalyDetectionJobService extends JobService {
    static final long MAX_DELAY_MS = TimeUnit.MINUTES.toMillis(30);
    static final int STATSD_UID_FILED = 1;
    static final int UID_NULL = -1;
    private final Object mLock = new Object();
    boolean mIsJobCanceled = false;

    public static void scheduleAnomalyDetection(Context context, Intent intent) {
        if (((JobScheduler) context.getSystemService(JobScheduler.class)).enqueue(new JobInfo.Builder(R.integer.job_anomaly_detection, new ComponentName(context, (Class<?>) AnomalyDetectionJobService.class)).setOverrideDeadline(MAX_DELAY_MS).build(), new JobWorkItem(intent)) != 1) {
            Log.i("AnomalyDetectionService", "Anomaly detection job service enqueue failed.");
        }
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        synchronized (this.mLock) {
            this.mIsJobCanceled = false;
        }
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public final void run() {
                AnomalyDetectionJobService.lambda$onStartJob$0(this.f$0, jobParameters);
            }
        });
        return true;
    }

    public static void lambda$onStartJob$0(AnomalyDetectionJobService anomalyDetectionJobService, JobParameters jobParameters) {
        BatteryDatabaseManager batteryDatabaseManager = BatteryDatabaseManager.getInstance(anomalyDetectionJobService);
        BatteryTipPolicy batteryTipPolicy = new BatteryTipPolicy(anomalyDetectionJobService);
        BatteryUtils batteryUtils = BatteryUtils.getInstance(anomalyDetectionJobService);
        ContentResolver contentResolver = anomalyDetectionJobService.getContentResolver();
        UserManager userManager = (UserManager) anomalyDetectionJobService.getSystemService(UserManager.class);
        PowerWhitelistBackend powerWhitelistBackend = PowerWhitelistBackend.getInstance(anomalyDetectionJobService);
        PowerUsageFeatureProvider powerUsageFeatureProvider = FeatureFactory.getFactory(anomalyDetectionJobService).getPowerUsageFeatureProvider(anomalyDetectionJobService);
        MetricsFeatureProvider metricsFeatureProvider = FeatureFactory.getFactory(anomalyDetectionJobService).getMetricsFeatureProvider();
        JobWorkItem jobWorkItemDequeueWork = anomalyDetectionJobService.dequeueWork(jobParameters);
        while (jobWorkItemDequeueWork != null) {
            anomalyDetectionJobService.saveAnomalyToDatabase(anomalyDetectionJobService, userManager, batteryDatabaseManager, batteryUtils, batteryTipPolicy, powerWhitelistBackend, contentResolver, powerUsageFeatureProvider, metricsFeatureProvider, jobWorkItemDequeueWork.getIntent().getExtras());
            anomalyDetectionJobService.completeWork(jobParameters, jobWorkItemDequeueWork);
            jobWorkItemDequeueWork = anomalyDetectionJobService.dequeueWork(jobParameters);
            batteryDatabaseManager = batteryDatabaseManager;
        }
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        synchronized (this.mLock) {
            this.mIsJobCanceled = true;
        }
        return true;
    }

    void saveAnomalyToDatabase(Context context, UserManager userManager, BatteryDatabaseManager batteryDatabaseManager, BatteryUtils batteryUtils, BatteryTipPolicy batteryTipPolicy, PowerWhitelistBackend powerWhitelistBackend, ContentResolver contentResolver, PowerUsageFeatureProvider powerUsageFeatureProvider, MetricsFeatureProvider metricsFeatureProvider, Bundle bundle) {
        boolean z;
        int i;
        int i2;
        long j;
        StatsDimensionsValue statsDimensionsValue = (StatsDimensionsValue) bundle.getParcelable("android.app.extra.STATS_DIMENSIONS_VALUE");
        long j2 = bundle.getLong("key_anomaly_timestamp", System.currentTimeMillis());
        ArrayList<String> stringArrayList = bundle.getStringArrayList("android.app.extra.STATS_BROADCAST_SUBSCRIBER_COOKIES");
        AnomalyInfo anomalyInfo = new AnomalyInfo(!ArrayUtils.isEmpty(stringArrayList) ? stringArrayList.get(0) : "");
        Log.i("AnomalyDetectionService", "Extra stats value: " + statsDimensionsValue.toString());
        try {
            int iExtractUidFromStatsDimensionsValue = extractUidFromStatsDimensionsValue(statsDimensionsValue);
            if (!powerUsageFeatureProvider.isSmartBatterySupported()) {
                if (Settings.Global.getInt(contentResolver, "app_auto_restriction_enabled", 1) == 1) {
                }
            } else {
                z = Settings.Global.getInt(contentResolver, "adaptive_battery_management_enabled", 1) == 1;
            }
            String packageName = batteryUtils.getPackageName(iExtractUidFromStatsDimensionsValue);
            long appLongVersionCode = batteryUtils.getAppLongVersionCode(packageName);
            if (batteryUtils.shouldHideAnomaly(powerWhitelistBackend, iExtractUidFromStatsDimensionsValue, anomalyInfo)) {
                metricsFeatureProvider.action(context, 1387, packageName, Pair.create(833, anomalyInfo.anomalyType), Pair.create(1389, Long.valueOf(appLongVersionCode)));
                return;
            }
            if (z && anomalyInfo.autoRestriction) {
                batteryUtils.setForceAppStandby(iExtractUidFromStatsDimensionsValue, packageName, 1);
                i = 2;
                i2 = 1389;
                j = appLongVersionCode;
                batteryDatabaseManager.insertAnomaly(iExtractUidFromStatsDimensionsValue, packageName, anomalyInfo.anomalyType.intValue(), 2, j2);
            } else {
                i = 2;
                i2 = 1389;
                j = appLongVersionCode;
                batteryDatabaseManager.insertAnomaly(iExtractUidFromStatsDimensionsValue, packageName, anomalyInfo.anomalyType.intValue(), 0, j2);
            }
            Pair<Integer, Object>[] pairArr = new Pair[i];
            pairArr[0] = Pair.create(1366, anomalyInfo.anomalyType);
            pairArr[1] = Pair.create(Integer.valueOf(i2), Long.valueOf(j));
            metricsFeatureProvider.action(context, 1367, packageName, pairArr);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            Log.e("AnomalyDetectionService", "Parse stats dimensions value error.", e);
        }
    }

    int extractUidFromStatsDimensionsValue(StatsDimensionsValue statsDimensionsValue) {
        if (statsDimensionsValue == null) {
            return UID_NULL;
        }
        if (statsDimensionsValue.isValueType(3) && statsDimensionsValue.getField() == 1) {
            return statsDimensionsValue.getIntValue();
        }
        if (statsDimensionsValue.isValueType(7)) {
            List tupleValueList = statsDimensionsValue.getTupleValueList();
            int size = tupleValueList.size();
            for (int i = 0; i < size; i++) {
                int iExtractUidFromStatsDimensionsValue = extractUidFromStatsDimensionsValue((StatsDimensionsValue) tupleValueList.get(i));
                if (iExtractUidFromStatsDimensionsValue != UID_NULL) {
                    return iExtractUidFromStatsDimensionsValue;
                }
            }
        }
        return UID_NULL;
    }

    JobWorkItem dequeueWork(JobParameters jobParameters) {
        synchronized (this.mLock) {
            if (this.mIsJobCanceled) {
                return null;
            }
            return jobParameters.dequeueWork();
        }
    }

    void completeWork(JobParameters jobParameters, JobWorkItem jobWorkItem) {
        synchronized (this.mLock) {
            if (this.mIsJobCanceled) {
                return;
            }
            jobParameters.completeWork(jobWorkItem);
        }
    }
}
