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
import android.os.Environment;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.PinnerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundDexOptService extends JobService {
    private static final boolean DEBUG = false;
    private static final int JOB_IDLE_OPTIMIZE = 800;
    private static final int JOB_POST_BOOT_UPDATE = 801;
    private static final int LOW_THRESHOLD_MULTIPLIER_FOR_DOWNGRADE = 2;
    private static final int OPTIMIZE_ABORT_BY_JOB_SCHEDULER = 2;
    private static final int OPTIMIZE_ABORT_NO_SPACE_LEFT = 3;
    private static final int OPTIMIZE_CONTINUE = 1;
    private static final int OPTIMIZE_PROCESSED = 0;
    private static final String TAG = "BackgroundDexOptService";
    private static final long IDLE_OPTIMIZATION_PERIOD = TimeUnit.DAYS.toMillis(1);
    private static ComponentName sDexoptServiceName = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, BackgroundDexOptService.class.getName());
    static final ArraySet<String> sFailedPackageNamesPrimary = new ArraySet<>();
    static final ArraySet<String> sFailedPackageNamesSecondary = new ArraySet<>();
    private static final long mDowngradeUnusedAppsThresholdInMillis = getDowngradeUnusedAppsThresholdInMillis();
    private final AtomicBoolean mAbortPostBootUpdate = new AtomicBoolean(false);
    private final AtomicBoolean mAbortIdleOptimization = new AtomicBoolean(false);
    private final AtomicBoolean mExitPostBootUpdate = new AtomicBoolean(false);
    private final File mDataDir = Environment.getDataDirectory();

    public static void schedule(Context context) {
        if (isBackgroundDexoptDisabled()) {
            return;
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        jobScheduler.schedule(new JobInfo.Builder(JOB_POST_BOOT_UPDATE, sDexoptServiceName).setMinimumLatency(TimeUnit.MINUTES.toMillis(1L)).setOverrideDeadline(TimeUnit.MINUTES.toMillis(1L)).build());
        jobScheduler.schedule(new JobInfo.Builder(JOB_IDLE_OPTIMIZE, sDexoptServiceName).setRequiresDeviceIdle(true).setRequiresCharging(true).setPeriodic(IDLE_OPTIMIZATION_PERIOD).build());
        if (PackageManagerService.DEBUG_DEXOPT) {
            Log.i(TAG, "Jobs scheduled");
        }
    }

    public static void notifyPackageChanged(String str) {
        synchronized (sFailedPackageNamesPrimary) {
            sFailedPackageNamesPrimary.remove(str);
        }
        synchronized (sFailedPackageNamesSecondary) {
            sFailedPackageNamesSecondary.remove(str);
        }
    }

    private int getBatteryLevel() {
        Intent intentRegisterReceiver = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        int intExtra = intentRegisterReceiver.getIntExtra("level", -1);
        int intExtra2 = intentRegisterReceiver.getIntExtra("scale", -1);
        if (!intentRegisterReceiver.getBooleanExtra("present", true)) {
            return 100;
        }
        if (intExtra < 0 || intExtra2 <= 0) {
            return 0;
        }
        return (100 * intExtra) / intExtra2;
    }

    private long getLowStorageThreshold(Context context) {
        long storageLowBytes = StorageManager.from(context).getStorageLowBytes(this.mDataDir);
        if (storageLowBytes == 0) {
            Log.e(TAG, "Invalid low storage threshold");
        }
        return storageLowBytes;
    }

    private boolean runPostBootUpdate(final JobParameters jobParameters, final PackageManagerService packageManagerService, final ArraySet<String> arraySet) {
        if (this.mExitPostBootUpdate.get()) {
            return false;
        }
        new Thread("BackgroundDexOptService_PostBootUpdate") {
            @Override
            public void run() {
                BackgroundDexOptService.this.postBootUpdate(jobParameters, packageManagerService, arraySet);
            }
        }.start();
        return true;
    }

    private void postBootUpdate(JobParameters jobParameters, PackageManagerService packageManagerService, ArraySet<String> arraySet) {
        int integer = getResources().getInteger(R.integer.config_defaultBinderHeavyHitterWatcherBatchSize);
        long lowStorageThreshold = getLowStorageThreshold(this);
        this.mAbortPostBootUpdate.set(false);
        ArraySet<String> arraySet2 = new ArraySet<>();
        Iterator<String> it = arraySet.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            String next = it.next();
            if (this.mAbortPostBootUpdate.get()) {
                return;
            }
            if (this.mExitPostBootUpdate.get() || getBatteryLevel() < integer) {
                break;
            }
            long usableSpace = this.mDataDir.getUsableSpace();
            if (usableSpace < lowStorageThreshold) {
                Log.w(TAG, "Aborting background dex opt job due to low storage: " + usableSpace);
                break;
            }
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i(TAG, "Updating package " + next);
            }
            if (packageManagerService.performDexOptWithStatus(new DexoptOptions(next, 1, 4)) == 1) {
                arraySet2.add(next);
            }
        }
        notifyPinService(arraySet2);
        jobFinished(jobParameters, false);
    }

    private boolean runIdleOptimization(final JobParameters jobParameters, final PackageManagerService packageManagerService, final ArraySet<String> arraySet) {
        new Thread("BackgroundDexOptService_IdleOptimization") {
            @Override
            public void run() {
                if (BackgroundDexOptService.this.idleOptimization(packageManagerService, arraySet, BackgroundDexOptService.this) != 2) {
                    Log.w(BackgroundDexOptService.TAG, "Idle optimizations aborted because of space constraints.");
                    BackgroundDexOptService.this.jobFinished(jobParameters, false);
                }
            }
        }.start();
        return true;
    }

    private int idleOptimization(PackageManagerService packageManagerService, ArraySet<String> arraySet, Context context) {
        Log.i(TAG, "Performing idle optimizations");
        this.mExitPostBootUpdate.set(true);
        this.mAbortIdleOptimization.set(false);
        long lowStorageThreshold = getLowStorageThreshold(context);
        int iOptimizePackages = optimizePackages(packageManagerService, arraySet, lowStorageThreshold, true, sFailedPackageNamesPrimary);
        if (iOptimizePackages != 2 && SystemProperties.getBoolean("dalvik.vm.dexopt.secondary", false)) {
            int iReconcileSecondaryDexFiles = reconcileSecondaryDexFiles(packageManagerService.getDexManager());
            if (iReconcileSecondaryDexFiles == 2) {
                return iReconcileSecondaryDexFiles;
            }
            return optimizePackages(packageManagerService, arraySet, lowStorageThreshold, false, sFailedPackageNamesSecondary);
        }
        return iOptimizePackages;
    }

    private int optimizePackages(PackageManagerService packageManagerService, ArraySet<String> arraySet, long j, boolean z, ArraySet<String> arraySet2) {
        boolean z2;
        boolean zPerformDexOpt;
        ArraySet<String> arraySet3 = new ArraySet<>();
        Set<String> unusedPackages = packageManagerService.getUnusedPackages(mDowngradeUnusedAppsThresholdInMillis);
        boolean zShouldDowngrade = shouldDowngrade(2 * j);
        for (String str : arraySet) {
            int iAbortIdleOptimizations = abortIdleOptimizations(j);
            if (iAbortIdleOptimizations == 2) {
                return iAbortIdleOptimizations;
            }
            synchronized (arraySet2) {
                if (!arraySet2.contains(str)) {
                    int i = 3;
                    if (unusedPackages.contains(str) && zShouldDowngrade) {
                        if (z && !packageManagerService.canHaveOatDir(str)) {
                            packageManagerService.deleteOatArtifactsOfPackage(str);
                        } else {
                            z2 = true;
                            i = 5;
                            synchronized (arraySet2) {
                            }
                        }
                    } else if (iAbortIdleOptimizations != 3) {
                        z2 = false;
                        synchronized (arraySet2) {
                            arraySet2.add(str);
                        }
                        int i2 = (z2 ? 32 : 0) | 5 | 512;
                        if (z) {
                            int iPerformDexOptWithStatus = packageManagerService.performDexOptWithStatus(new DexoptOptions(str, i, i2));
                            zPerformDexOpt = iPerformDexOptWithStatus != -1;
                            if (iPerformDexOptWithStatus == 1) {
                                arraySet3.add(str);
                            }
                        } else {
                            zPerformDexOpt = packageManagerService.performDexOpt(new DexoptOptions(str, i, i2 | 8));
                        }
                        if (zPerformDexOpt) {
                            synchronized (arraySet2) {
                                arraySet2.remove(str);
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        notifyPinService(arraySet3);
        return 0;
    }

    private int reconcileSecondaryDexFiles(DexManager dexManager) {
        for (String str : dexManager.getAllPackagesWithSecondaryDexFiles()) {
            if (this.mAbortIdleOptimization.get()) {
                return 2;
            }
            dexManager.reconcileSecondaryDexFiles(str);
        }
        return 0;
    }

    private int abortIdleOptimizations(long j) {
        if (this.mAbortIdleOptimization.get()) {
            return 2;
        }
        long usableSpace = this.mDataDir.getUsableSpace();
        if (usableSpace < j) {
            Log.w(TAG, "Aborting background dex opt job due to low storage: " + usableSpace);
            return 3;
        }
        return 1;
    }

    private boolean shouldDowngrade(long j) {
        if (this.mDataDir.getUsableSpace() < j) {
            return true;
        }
        return false;
    }

    public static boolean runIdleOptimizationsNow(PackageManagerService packageManagerService, Context context, List<String> list) {
        ArraySet<String> arraySet;
        BackgroundDexOptService backgroundDexOptService = new BackgroundDexOptService();
        if (list == null) {
            arraySet = packageManagerService.getOptimizablePackages();
        } else {
            arraySet = new ArraySet<>(list);
        }
        return backgroundDexOptService.idleOptimization(packageManagerService, arraySet, context) == 0;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (PackageManagerService.DEBUG_DEXOPT) {
            Log.i(TAG, "onStartJob");
        }
        PackageManagerService packageManagerService = (PackageManagerService) ServiceManager.getService(Settings.ATTR_PACKAGE);
        if (packageManagerService.isStorageLow()) {
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i(TAG, "Low storage, skipping this run");
            }
            return false;
        }
        ArraySet<String> optimizablePackages = packageManagerService.getOptimizablePackages();
        if (optimizablePackages.isEmpty()) {
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i(TAG, "No packages to optimize");
            }
            return false;
        }
        if (jobParameters.getJobId() == JOB_POST_BOOT_UPDATE) {
            return runPostBootUpdate(jobParameters, packageManagerService, optimizablePackages);
        }
        return runIdleOptimization(jobParameters, packageManagerService, optimizablePackages);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (PackageManagerService.DEBUG_DEXOPT) {
            Log.i(TAG, "onStopJob");
        }
        if (jobParameters.getJobId() == JOB_POST_BOOT_UPDATE) {
            this.mAbortPostBootUpdate.set(true);
            return false;
        }
        this.mAbortIdleOptimization.set(true);
        return true;
    }

    private void notifyPinService(ArraySet<String> arraySet) {
        PinnerService pinnerService = (PinnerService) LocalServices.getService(PinnerService.class);
        if (pinnerService != null) {
            Log.i(TAG, "Pinning optimized code " + arraySet);
            pinnerService.update(arraySet);
        }
    }

    private static long getDowngradeUnusedAppsThresholdInMillis() {
        String str = SystemProperties.get("pm.dexopt.downgrade_after_inactive_days");
        if (str == null || str.isEmpty()) {
            Log.w(TAG, "SysProp pm.dexopt.downgrade_after_inactive_days not set");
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return TimeUnit.DAYS.toMillis(Long.parseLong(str));
    }

    private static boolean isBackgroundDexoptDisabled() {
        return SystemProperties.getBoolean("pm.dexopt.disable_bg_dexopt", false);
    }
}
