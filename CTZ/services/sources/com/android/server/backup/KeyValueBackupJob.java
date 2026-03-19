package com.android.server.backup;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.util.Random;

public class KeyValueBackupJob extends JobService {
    private static final int JOB_ID = 20537;
    private static final long MAX_DEFERRAL = 86400000;
    private static final String TAG = "KeyValueBackupJob";
    private static ComponentName sKeyValueJobService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, KeyValueBackupJob.class.getName());
    private static boolean sScheduled = false;
    private static long sNextScheduled = 0;

    public static void schedule(Context context, BackupManagerConstants backupManagerConstants) {
        schedule(context, 0L, backupManagerConstants);
    }

    public static void schedule(Context context, long j, BackupManagerConstants backupManagerConstants) {
        long keyValueBackupIntervalMilliseconds;
        long keyValueBackupFuzzMilliseconds;
        int keyValueBackupRequiredNetworkType;
        boolean keyValueBackupRequireCharging;
        synchronized (KeyValueBackupJob.class) {
            if (sScheduled) {
                return;
            }
            synchronized (backupManagerConstants) {
                keyValueBackupIntervalMilliseconds = backupManagerConstants.getKeyValueBackupIntervalMilliseconds();
                keyValueBackupFuzzMilliseconds = backupManagerConstants.getKeyValueBackupFuzzMilliseconds();
                keyValueBackupRequiredNetworkType = backupManagerConstants.getKeyValueBackupRequiredNetworkType();
                keyValueBackupRequireCharging = backupManagerConstants.getKeyValueBackupRequireCharging();
            }
            if (j <= 0) {
                j = ((long) new Random().nextInt((int) keyValueBackupFuzzMilliseconds)) + keyValueBackupIntervalMilliseconds;
            }
            Slog.v(TAG, "Scheduling k/v pass in " + ((j / 1000) / 60) + " minutes");
            ((JobScheduler) context.getSystemService("jobscheduler")).schedule(new JobInfo.Builder(JOB_ID, sKeyValueJobService).setMinimumLatency(j).setRequiredNetworkType(keyValueBackupRequiredNetworkType).setRequiresCharging(keyValueBackupRequireCharging).setOverrideDeadline(86400000L).build());
            sNextScheduled = System.currentTimeMillis() + j;
            sScheduled = true;
        }
    }

    public static void cancel(Context context) {
        synchronized (KeyValueBackupJob.class) {
            ((JobScheduler) context.getSystemService("jobscheduler")).cancel(JOB_ID);
            sNextScheduled = 0L;
            sScheduled = false;
        }
    }

    public static long nextScheduled() {
        long j;
        synchronized (KeyValueBackupJob.class) {
            j = sNextScheduled;
        }
        return j;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        synchronized (KeyValueBackupJob.class) {
            sNextScheduled = 0L;
            sScheduled = false;
        }
        try {
            BackupManagerService.getInstance().backupNow();
        } catch (RemoteException e) {
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
