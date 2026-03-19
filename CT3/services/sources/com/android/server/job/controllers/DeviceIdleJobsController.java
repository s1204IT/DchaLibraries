package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.UserHandle;
import com.android.internal.util.ArrayUtils;
import com.android.server.DeviceIdleController;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.JobStore;
import java.io.PrintWriter;

public class DeviceIdleJobsController extends StateController {
    private static final boolean LOG_DEBUG = false;
    private static final String LOG_TAG = "DeviceIdleJobsController";
    private static DeviceIdleJobsController sController;
    private static Object sCreationLock = new Object();
    private final BroadcastReceiver mBroadcastReceiver;
    private boolean mDeviceIdleMode;
    private int[] mDeviceIdleWhitelistAppIds;
    private final JobSchedulerService mJobSchedulerService;
    private final DeviceIdleController.LocalService mLocalDeviceIdleController;
    private final PowerManager mPowerManager;
    final JobStore.JobStatusFunctor mUpdateFunctor;

    public static DeviceIdleJobsController get(JobSchedulerService service) {
        DeviceIdleJobsController deviceIdleJobsController;
        synchronized (sCreationLock) {
            if (sController == null) {
                sController = new DeviceIdleJobsController(service, service.getContext(), service.getLock());
            }
            deviceIdleJobsController = sController;
        }
        return deviceIdleJobsController;
    }

    private DeviceIdleJobsController(JobSchedulerService jobSchedulerService, Context context, Object lock) {
        super(jobSchedulerService, context, lock);
        this.mUpdateFunctor = new JobStore.JobStatusFunctor() {
            @Override
            public void process(JobStatus jobStatus) {
                DeviceIdleJobsController.this.updateTaskStateLocked(jobStatus);
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                boolean zIsLightDeviceIdleMode;
                String action = intent.getAction();
                if ("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED".equals(action) || "android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(action)) {
                    DeviceIdleJobsController deviceIdleJobsController = DeviceIdleJobsController.this;
                    if (DeviceIdleJobsController.this.mPowerManager != null) {
                        if (DeviceIdleJobsController.this.mPowerManager.isDeviceIdleMode()) {
                            zIsLightDeviceIdleMode = true;
                        } else {
                            zIsLightDeviceIdleMode = DeviceIdleJobsController.this.mPowerManager.isLightDeviceIdleMode();
                        }
                    } else {
                        zIsLightDeviceIdleMode = false;
                    }
                    deviceIdleJobsController.updateIdleMode(zIsLightDeviceIdleMode);
                    return;
                }
                if (!"android.os.action.POWER_SAVE_WHITELIST_CHANGED".equals(action)) {
                    return;
                }
                DeviceIdleJobsController.this.updateWhitelist();
            }
        };
        this.mJobSchedulerService = jobSchedulerService;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mLocalDeviceIdleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        filter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
    }

    void updateIdleMode(boolean enabled) {
        boolean changed = false;
        if (this.mDeviceIdleWhitelistAppIds == null) {
            updateWhitelist();
        }
        synchronized (this.mLock) {
            if (this.mDeviceIdleMode != enabled) {
                changed = true;
            }
            this.mDeviceIdleMode = enabled;
            this.mJobSchedulerService.getJobStore().forEachJob(this.mUpdateFunctor);
        }
        if (!changed) {
            return;
        }
        this.mStateChangedListener.onDeviceIdleStateChanged(enabled);
    }

    void updateWhitelist() {
        synchronized (this.mLock) {
            if (this.mLocalDeviceIdleController != null) {
                this.mDeviceIdleWhitelistAppIds = this.mLocalDeviceIdleController.getPowerSaveWhitelistUserAppIds();
            }
        }
    }

    boolean isWhitelistedLocked(JobStatus job) {
        if (this.mDeviceIdleWhitelistAppIds != null && ArrayUtils.contains(this.mDeviceIdleWhitelistAppIds, UserHandle.getAppId(job.getSourceUid()))) {
            return true;
        }
        return false;
    }

    private void updateTaskStateLocked(JobStatus task) {
        boolean whitelisted = isWhitelistedLocked(task);
        boolean enableTask = this.mDeviceIdleMode ? whitelisted : true;
        task.setDeviceNotDozingConstraintSatisfied(enableTask, whitelisted);
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        synchronized (this.mLock) {
            updateTaskStateLocked(jobStatus);
        }
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
    }

    @Override
    public void dumpControllerStateLocked(final PrintWriter pw, final int filterUid) {
        pw.println(LOG_TAG);
        this.mJobSchedulerService.getJobStore().forEachJob(new JobStore.JobStatusFunctor() {
            @Override
            public void process(JobStatus jobStatus) {
                if (!jobStatus.shouldDump(filterUid)) {
                    return;
                }
                pw.print("  #");
                jobStatus.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, jobStatus.getSourceUid());
                pw.print(": ");
                pw.print(jobStatus.getSourcePackageName());
                pw.print((jobStatus.satisfiedConstraints & 256) != 0 ? " RUNNABLE" : " WAITING");
                if (jobStatus.dozeWhitelisted) {
                    pw.print(" WHITELISTED");
                }
                pw.println();
            }
        });
    }
}
