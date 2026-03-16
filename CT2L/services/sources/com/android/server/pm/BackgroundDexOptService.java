package com.android.server.pm;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundDexOptService extends JobService {
    static final int BACKGROUND_DEXOPT_JOB = 800;
    static final String TAG = "BackgroundDexOptService";
    private static ComponentName sDexoptServiceName = new ComponentName("android", BackgroundDexOptService.class.getName());
    static final ArraySet<String> sFailedPackageNames = new ArraySet<>();
    final AtomicBoolean mIdleTime = new AtomicBoolean(false);

    public static void schedule(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo job = new JobInfo.Builder(BACKGROUND_DEXOPT_JOB, sDexoptServiceName).setRequiresDeviceIdle(true).setRequiresCharging(true).build();
        js.schedule(job);
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        final ArraySet<String> pkgs;
        Log.i(TAG, "onIdleStart");
        final PackageManagerService pm = (PackageManagerService) ServiceManager.getService("package");
        if (pm.isStorageLow() || (pkgs = pm.getPackagesThatNeedDexOpt()) == null) {
            return false;
        }
        this.mIdleTime.set(true);
        new Thread("BackgroundDexOptService_DexOpter") {
            @Override
            public void run() {
                for (String pkg : pkgs) {
                    if (!BackgroundDexOptService.this.mIdleTime.get()) {
                        BackgroundDexOptService.schedule(BackgroundDexOptService.this);
                        return;
                    } else if (!BackgroundDexOptService.sFailedPackageNames.contains(pkg) && !pm.performDexOpt(pkg, null, true)) {
                        BackgroundDexOptService.sFailedPackageNames.add(pkg);
                    }
                }
                BackgroundDexOptService.this.jobFinished(params, false);
            }
        }.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "onIdleStop");
        this.mIdleTime.set(false);
        return false;
    }
}
