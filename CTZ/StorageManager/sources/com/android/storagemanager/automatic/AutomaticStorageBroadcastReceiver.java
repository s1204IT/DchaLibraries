package com.android.storagemanager.automatic;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;

public class AutomaticStorageBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        ComponentName componentName = new ComponentName(context, (Class<?>) AutomaticStorageManagementJobService.class);
        jobScheduler.schedule(new JobInfo.Builder(0, componentName).setRequiresCharging(true).setRequiresDeviceIdle(true).setPeriodic(SystemProperties.getLong("debug.asm.period", 86400000L)).build());
    }
}
