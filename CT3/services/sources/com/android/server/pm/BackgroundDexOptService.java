package com.android.server.pm;

import android.R;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundDexOptService extends JobService {
    static final int JOB_IDLE_OPTIMIZE = 800;
    static final int JOB_POST_BOOT_UPDATE = 801;
    static final long RETRY_LATENCY = 14400000;
    static final String TAG = "BackgroundDexOptService";
    private static ComponentName sDexoptServiceName = new ComponentName("android", BackgroundDexOptService.class.getName());
    static final ArraySet<String> sFailedPackageNames = new ArraySet<>();
    final AtomicBoolean mAbortPostBootUpdate = new AtomicBoolean(false);
    final AtomicBoolean mAbortIdleOptimization = new AtomicBoolean(false);
    final AtomicBoolean mExitPostBootUpdate = new AtomicBoolean(false);

    public static void schedule(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        js.schedule(new JobInfo.Builder(JOB_POST_BOOT_UPDATE, sDexoptServiceName).setMinimumLatency(TimeUnit.MINUTES.toMillis(1L)).setOverrideDeadline(TimeUnit.MINUTES.toMillis(1L)).build());
        js.schedule(new JobInfo.Builder(JOB_IDLE_OPTIMIZE, sDexoptServiceName).setRequiresDeviceIdle(true).setRequiresCharging(true).setPeriodic(TimeUnit.DAYS.toMillis(1L)).build());
        if (!PackageManagerService.DEBUG_DEXOPT) {
            return;
        }
        Log.i(TAG, "Jobs scheduled");
    }

    public static void notifyPackageChanged(String packageName) {
        synchronized (sFailedPackageNames) {
            sFailedPackageNames.remove(packageName);
        }
    }

    private int getBatteryLevel() {
        IntentFilter filter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
        Intent intent = registerReceiver(null, filter);
        int level = intent.getIntExtra("level", -1);
        int scale = intent.getIntExtra("scale", -1);
        if (level < 0 || scale <= 0) {
            return 0;
        }
        return (level * 100) / scale;
    }

    private boolean runPostBootUpdate(final JobParameters jobParams, final PackageManagerService pm, final ArraySet<String> pkgs) {
        if (this.mExitPostBootUpdate.get()) {
            return false;
        }
        final int lowBatteryThreshold = getResources().getInteger(R.integer.config_debugSystemServerPssThresholdBytes);
        this.mAbortPostBootUpdate.set(false);
        new Thread("BackgroundDexOptService_PostBootUpdate") {
            @Override
            public void run() {
                for (String pkg : pkgs) {
                    if (BackgroundDexOptService.this.mAbortPostBootUpdate.get()) {
                        return;
                    }
                    if (BackgroundDexOptService.this.mExitPostBootUpdate.get() || BackgroundDexOptService.this.getBatteryLevel() < lowBatteryThreshold) {
                        break;
                    }
                    if (PackageManagerService.DEBUG_DEXOPT) {
                        Log.i(BackgroundDexOptService.TAG, "Updating package " + pkg);
                    }
                    pm.performDexOpt(pkg, false, 1, false);
                }
                BackgroundDexOptService.this.jobFinished(jobParams, false);
            }
        }.start();
        return true;
    }

    private boolean runIdleOptimization(final JobParameters jobParams, final PackageManagerService pm, final ArraySet<String> pkgs) {
        this.mExitPostBootUpdate.set(true);
        this.mAbortIdleOptimization.set(false);
        new Thread("BackgroundDexOptService_IdleOptimization") {
            @Override
            public void run() {
                for (String pkg : pkgs) {
                    if (BackgroundDexOptService.this.mAbortIdleOptimization.get()) {
                        return;
                    }
                    if (!BackgroundDexOptService.sFailedPackageNames.contains(pkg)) {
                        synchronized (BackgroundDexOptService.sFailedPackageNames) {
                            BackgroundDexOptService.sFailedPackageNames.add(pkg);
                        }
                        if (pm.performDexOpt(pkg, true, 3, false)) {
                            synchronized (BackgroundDexOptService.sFailedPackageNames) {
                                BackgroundDexOptService.sFailedPackageNames.remove(pkg);
                            }
                        } else {
                            continue;
                        }
                    }
                }
                BackgroundDexOptService.this.jobFinished(jobParams, false);
            }
        }.start();
        return true;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        if (PackageManagerService.DEBUG_DEXOPT) {
            Log.i(TAG, "onStartJob");
        }
        PackageManagerService pm = (PackageManagerService) ServiceManager.getService("package");
        if (pm.isStorageLow()) {
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i(TAG, "Low storage, skipping this run");
            }
            return false;
        }
        ArraySet<String> pkgs = pm.getOptimizablePackages();
        if (pkgs == null || pkgs.isEmpty()) {
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i(TAG, "No packages to optimize");
            }
            return false;
        }
        if (params.getJobId() == JOB_POST_BOOT_UPDATE) {
            return runPostBootUpdate(params, pm, pkgs);
        }
        return runIdleOptimization(params, pm, pkgs);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (PackageManagerService.DEBUG_DEXOPT) {
            Log.i(TAG, "onStopJob");
        }
        if (params.getJobId() == JOB_POST_BOOT_UPDATE) {
            this.mAbortPostBootUpdate.set(true);
            return false;
        }
        this.mAbortIdleOptimization.set(true);
        return false;
    }
}
