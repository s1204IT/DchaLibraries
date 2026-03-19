package com.android.server.storage;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageStats;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.pm.PackageManagerService;
import com.android.server.storage.FileCollector;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiskStatsLoggingService extends JobService {
    public static final String DUMPSYS_CACHE_PATH = "/data/system/diskstats_cache.json";
    private static final int JOB_DISKSTATS_LOGGING = 1145656139;
    private static final String TAG = "DiskStatsLogService";
    private static ComponentName sDiskStatsLoggingService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, DiskStatsLoggingService.class.getName());

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (!isCharging(this) || !isDumpsysTaskEnabled(getContentResolver())) {
            jobFinished(jobParameters, true);
            return false;
        }
        VolumeInfo primaryStorageCurrentVolume = getPackageManager().getPrimaryStorageCurrentVolume();
        if (primaryStorageCurrentVolume == null) {
            return false;
        }
        AppCollector appCollector = new AppCollector(this, primaryStorageCurrentVolume);
        Environment.UserEnvironment userEnvironment = new Environment.UserEnvironment(UserHandle.myUserId());
        LogRunnable logRunnable = new LogRunnable();
        logRunnable.setDownloadsDirectory(userEnvironment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        logRunnable.setSystemSize(FileCollector.getSystemSize(this));
        logRunnable.setLogOutputFile(new File(DUMPSYS_CACHE_PATH));
        logRunnable.setAppCollector(appCollector);
        logRunnable.setJobService(this, jobParameters);
        logRunnable.setContext(this);
        AsyncTask.execute(logRunnable);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public static void schedule(Context context) {
        ((JobScheduler) context.getSystemService("jobscheduler")).schedule(new JobInfo.Builder(JOB_DISKSTATS_LOGGING, sDiskStatsLoggingService).setRequiresDeviceIdle(true).setRequiresCharging(true).setPeriodic(TimeUnit.DAYS.toMillis(1L)).build());
    }

    private static boolean isCharging(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService("batterymanager");
        if (batteryManager != null) {
            return batteryManager.isCharging();
        }
        return false;
    }

    @VisibleForTesting
    static boolean isDumpsysTaskEnabled(ContentResolver contentResolver) {
        return Settings.Global.getInt(contentResolver, "enable_diskstats_logging", 1) != 0;
    }

    @VisibleForTesting
    static class LogRunnable implements Runnable {
        private static final long TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);
        private AppCollector mCollector;
        private Context mContext;
        private File mDownloadsDirectory;
        private JobService mJobService;
        private File mOutputFile;
        private JobParameters mParams;
        private long mSystemSize;

        LogRunnable() {
        }

        public void setDownloadsDirectory(File file) {
            this.mDownloadsDirectory = file;
        }

        public void setAppCollector(AppCollector appCollector) {
            this.mCollector = appCollector;
        }

        public void setLogOutputFile(File file) {
            this.mOutputFile = file;
        }

        public void setSystemSize(long j) {
            this.mSystemSize = j;
        }

        public void setContext(Context context) {
            this.mContext = context;
        }

        public void setJobService(JobService jobService, JobParameters jobParameters) {
            this.mJobService = jobService;
            this.mParams = jobParameters;
        }

        @Override
        public void run() {
            boolean z = true;
            try {
                FileCollector.MeasurementResult measurementResult = FileCollector.getMeasurementResult(this.mContext);
                FileCollector.MeasurementResult measurementResult2 = FileCollector.getMeasurementResult(this.mDownloadsDirectory);
                List<PackageStats> packageStats = this.mCollector.getPackageStats(TIMEOUT_MILLIS);
                if (packageStats != null) {
                    z = false;
                    logToFile(measurementResult, measurementResult2, packageStats, this.mSystemSize);
                } else {
                    Log.w(DiskStatsLoggingService.TAG, "Timed out while fetching package stats.");
                }
                finishJob(z);
            } catch (IllegalStateException e) {
                Log.e(DiskStatsLoggingService.TAG, "Error while measuring storage", e);
                finishJob(true);
            }
        }

        private void logToFile(FileCollector.MeasurementResult measurementResult, FileCollector.MeasurementResult measurementResult2, List<PackageStats> list, long j) {
            DiskStatsFileLogger diskStatsFileLogger = new DiskStatsFileLogger(measurementResult, measurementResult2, list, j);
            try {
                this.mOutputFile.createNewFile();
                diskStatsFileLogger.dumpToFile(this.mOutputFile);
            } catch (IOException e) {
                Log.e(DiskStatsLoggingService.TAG, "Exception while writing opportunistic disk file cache.", e);
            }
        }

        private void finishJob(boolean z) {
            if (this.mJobService != null) {
                this.mJobService.jobFinished(this.mParams, z);
            }
        }
    }
}
