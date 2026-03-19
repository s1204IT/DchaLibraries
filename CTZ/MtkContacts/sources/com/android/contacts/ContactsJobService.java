package com.android.contacts;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class ContactsJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (jobParameters.getJobId() == 1) {
            DynamicShortcuts.updateFromJob(this, jobParameters);
            return true;
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
