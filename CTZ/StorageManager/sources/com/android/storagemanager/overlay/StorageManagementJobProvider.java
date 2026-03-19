package com.android.storagemanager.overlay;

import android.app.job.JobParameters;
import android.content.Context;

public interface StorageManagementJobProvider {
    long getDisableThresholdMillis(Context context);

    boolean onStartJob(Context context, JobParameters jobParameters, int i);

    boolean onStopJob(Context context, JobParameters jobParameters);
}
