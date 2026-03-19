package com.android.server.backup;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import java.util.Random;

public class KeyValueBackupJob extends JobService {
    static final long BATCH_INTERVAL = 14400000;
    private static final int FUZZ_MILLIS = 600000;
    private static final int JOB_ID = 20537;
    private static final long MAX_DEFERRAL = 86400000;
    private static final String TAG = "KeyValueBackupJob";
    private static ComponentName sKeyValueJobService = new ComponentName("android", KeyValueBackupJob.class.getName());
    private static boolean sScheduled = false;
    private static long sNextScheduled = 0;

    public static void schedule(Context ctx) {
        schedule(ctx, 0L);
    }

    public static void schedule(Context ctx, long delay) {
        synchronized (KeyValueBackupJob.class) {
            if (!sScheduled) {
                if (delay <= 0) {
                    delay = BATCH_INTERVAL + ((long) new Random().nextInt(600000));
                }
                Slog.v(TAG, "Scheduling k/v pass in " + ((delay / 1000) / 60) + " minutes");
                JobScheduler js = (JobScheduler) ctx.getSystemService("jobscheduler");
                JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, sKeyValueJobService).setMinimumLatency(delay).setRequiredNetworkType(1).setRequiresCharging(true).setOverrideDeadline(86400000L);
                js.schedule(builder.build());
                sNextScheduled = System.currentTimeMillis() + delay;
                sScheduled = true;
            }
        }
    }

    public static void cancel(Context ctx) {
        synchronized (KeyValueBackupJob.class) {
            JobScheduler js = (JobScheduler) ctx.getSystemService("jobscheduler");
            js.cancel(JOB_ID);
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
    public boolean onStartJob(JobParameters params) {
        synchronized (KeyValueBackupJob.class) {
            sNextScheduled = 0L;
            sScheduled = false;
        }
        Trampoline service = BackupManagerService.getInstance();
        try {
            service.backupNow();
        } catch (RemoteException e) {
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
