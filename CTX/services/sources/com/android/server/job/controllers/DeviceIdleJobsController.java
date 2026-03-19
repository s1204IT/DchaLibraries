package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.DeviceIdleController;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class DeviceIdleJobsController extends StateController {
    private static final long BACKGROUND_JOBS_DELAY = 3000;
    private static final boolean DEBUG;
    static final int PROCESS_BACKGROUND_JOBS = 1;
    private static final String TAG = "JobScheduler.DeviceIdle";
    private final ArraySet<JobStatus> mAllowInIdleJobs;
    private final BroadcastReceiver mBroadcastReceiver;
    private boolean mDeviceIdleMode;
    private final DeviceIdleUpdateFunctor mDeviceIdleUpdateFunctor;
    private int[] mDeviceIdleWhitelistAppIds;
    private final SparseBooleanArray mForegroundUids;
    private final DeviceIdleJobsDelayHandler mHandler;
    private final DeviceIdleController.LocalService mLocalDeviceIdleController;
    private final PowerManager mPowerManager;
    private int[] mPowerSaveTempWhitelistAppIds;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public DeviceIdleJobsController(JobSchedulerService jobSchedulerService) {
        super(jobSchedulerService);
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte b;
                String action = intent.getAction();
                int iHashCode = action.hashCode();
                boolean z = true;
                if (iHashCode != -712152692) {
                    if (iHashCode != -65633567) {
                        if (iHashCode != 498807504) {
                            b = (iHashCode == 870701415 && action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) ? (byte) 1 : (byte) -1;
                        } else if (action.equals("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED")) {
                            b = 0;
                        }
                    } else if (action.equals("android.os.action.POWER_SAVE_WHITELIST_CHANGED")) {
                        b = 2;
                    }
                } else if (action.equals("android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED")) {
                    b = 3;
                }
                switch (b) {
                    case 0:
                    case 1:
                        DeviceIdleJobsController deviceIdleJobsController = DeviceIdleJobsController.this;
                        if (DeviceIdleJobsController.this.mPowerManager == null || (!DeviceIdleJobsController.this.mPowerManager.isDeviceIdleMode() && !DeviceIdleJobsController.this.mPowerManager.isLightDeviceIdleMode())) {
                            z = false;
                        }
                        deviceIdleJobsController.updateIdleMode(z);
                        return;
                    case 2:
                        synchronized (DeviceIdleJobsController.this.mLock) {
                            DeviceIdleJobsController.this.mDeviceIdleWhitelistAppIds = DeviceIdleJobsController.this.mLocalDeviceIdleController.getPowerSaveWhitelistUserAppIds();
                            if (DeviceIdleJobsController.DEBUG) {
                                Slog.d(DeviceIdleJobsController.TAG, "Got whitelist " + Arrays.toString(DeviceIdleJobsController.this.mDeviceIdleWhitelistAppIds));
                            }
                            break;
                        }
                        return;
                    case 3:
                        synchronized (DeviceIdleJobsController.this.mLock) {
                            DeviceIdleJobsController.this.mPowerSaveTempWhitelistAppIds = DeviceIdleJobsController.this.mLocalDeviceIdleController.getPowerSaveTempWhitelistAppIds();
                            if (DeviceIdleJobsController.DEBUG) {
                                Slog.d(DeviceIdleJobsController.TAG, "Got temp whitelist " + Arrays.toString(DeviceIdleJobsController.this.mPowerSaveTempWhitelistAppIds));
                            }
                            boolean zUpdateTaskStateLocked = false;
                            for (int i = 0; i < DeviceIdleJobsController.this.mAllowInIdleJobs.size(); i++) {
                                zUpdateTaskStateLocked |= DeviceIdleJobsController.this.updateTaskStateLocked((JobStatus) DeviceIdleJobsController.this.mAllowInIdleJobs.valueAt(i));
                            }
                            if (zUpdateTaskStateLocked) {
                                DeviceIdleJobsController.this.mStateChangedListener.onControllerStateChanged();
                            }
                            break;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mHandler = new DeviceIdleJobsDelayHandler(this.mContext.getMainLooper());
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mLocalDeviceIdleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
        this.mDeviceIdleWhitelistAppIds = this.mLocalDeviceIdleController.getPowerSaveWhitelistUserAppIds();
        this.mPowerSaveTempWhitelistAppIds = this.mLocalDeviceIdleController.getPowerSaveTempWhitelistAppIds();
        this.mDeviceIdleUpdateFunctor = new DeviceIdleUpdateFunctor();
        this.mAllowInIdleJobs = new ArraySet<>();
        this.mForegroundUids = new SparseBooleanArray();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        intentFilter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
        intentFilter.addAction("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        intentFilter.addAction("android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    void updateIdleMode(boolean z) {
        boolean z2;
        synchronized (this.mLock) {
            z2 = this.mDeviceIdleMode != z;
            this.mDeviceIdleMode = z;
            if (DEBUG) {
                Slog.d(TAG, "mDeviceIdleMode=" + this.mDeviceIdleMode);
            }
            if (z) {
                this.mHandler.removeMessages(1);
                this.mService.getJobStore().forEachJob(this.mDeviceIdleUpdateFunctor);
            } else {
                for (int i = 0; i < this.mForegroundUids.size(); i++) {
                    if (this.mForegroundUids.valueAt(i)) {
                        this.mService.getJobStore().forEachJobForSourceUid(this.mForegroundUids.keyAt(i), this.mDeviceIdleUpdateFunctor);
                    }
                }
                this.mHandler.sendEmptyMessageDelayed(1, BACKGROUND_JOBS_DELAY);
            }
        }
        if (z2) {
            this.mStateChangedListener.onDeviceIdleStateChanged(z);
        }
    }

    public void setUidActiveLocked(int i, boolean z) {
        if (!(z != this.mForegroundUids.get(i))) {
            return;
        }
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("uid ");
            sb.append(i);
            sb.append(" going ");
            sb.append(z ? "active" : "inactive");
            Slog.d(TAG, sb.toString());
        }
        this.mForegroundUids.put(i, z);
        this.mDeviceIdleUpdateFunctor.mChanged = false;
        this.mService.getJobStore().forEachJobForSourceUid(i, this.mDeviceIdleUpdateFunctor);
        if (this.mDeviceIdleUpdateFunctor.mChanged) {
            this.mStateChangedListener.onControllerStateChanged();
        }
    }

    boolean isWhitelistedLocked(JobStatus jobStatus) {
        return Arrays.binarySearch(this.mDeviceIdleWhitelistAppIds, UserHandle.getAppId(jobStatus.getSourceUid())) >= 0;
    }

    boolean isTempWhitelistedLocked(JobStatus jobStatus) {
        return ArrayUtils.contains(this.mPowerSaveTempWhitelistAppIds, UserHandle.getAppId(jobStatus.getSourceUid()));
    }

    private boolean updateTaskStateLocked(JobStatus jobStatus) {
        boolean z = (jobStatus.getFlags() & 2) != 0 && (this.mForegroundUids.get(jobStatus.getSourceUid()) || isTempWhitelistedLocked(jobStatus));
        boolean zIsWhitelistedLocked = isWhitelistedLocked(jobStatus);
        return jobStatus.setDeviceNotDozingConstraintSatisfied(!this.mDeviceIdleMode || zIsWhitelistedLocked || z, zIsWhitelistedLocked);
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if ((jobStatus.getFlags() & 2) != 0) {
            this.mAllowInIdleJobs.add(jobStatus);
        }
        updateTaskStateLocked(jobStatus);
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
        if ((jobStatus.getFlags() & 2) != 0) {
            this.mAllowInIdleJobs.remove(jobStatus);
        }
    }

    @Override
    public void dumpControllerStateLocked(final IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate) {
        indentingPrintWriter.println("Idle mode: " + this.mDeviceIdleMode);
        indentingPrintWriter.println();
        this.mService.getJobStore().forEachJob(predicate, new Consumer() {
            @Override
            public final void accept(Object obj) {
                DeviceIdleJobsController.lambda$dumpControllerStateLocked$0(this.f$0, indentingPrintWriter, (JobStatus) obj);
            }
        });
    }

    public static void lambda$dumpControllerStateLocked$0(DeviceIdleJobsController deviceIdleJobsController, IndentingPrintWriter indentingPrintWriter, JobStatus jobStatus) {
        indentingPrintWriter.print("#");
        jobStatus.printUniqueId(indentingPrintWriter);
        indentingPrintWriter.print(" from ");
        UserHandle.formatUid(indentingPrintWriter, jobStatus.getSourceUid());
        indentingPrintWriter.print(": ");
        indentingPrintWriter.print(jobStatus.getSourcePackageName());
        indentingPrintWriter.print((jobStatus.satisfiedConstraints & 33554432) != 0 ? " RUNNABLE" : " WAITING");
        if (jobStatus.dozeWhitelisted) {
            indentingPrintWriter.print(" WHITELISTED");
        }
        if (deviceIdleJobsController.mAllowInIdleJobs.contains(jobStatus)) {
            indentingPrintWriter.print(" ALLOWED_IN_DOZE");
        }
        indentingPrintWriter.println();
    }

    @Override
    public void dumpControllerStateLocked(final ProtoOutputStream protoOutputStream, long j, Predicate<JobStatus> predicate) {
        long jStart = protoOutputStream.start(j);
        long jStart2 = protoOutputStream.start(1146756268037L);
        protoOutputStream.write(1133871366145L, this.mDeviceIdleMode);
        this.mService.getJobStore().forEachJob(predicate, new Consumer() {
            @Override
            public final void accept(Object obj) {
                DeviceIdleJobsController.lambda$dumpControllerStateLocked$1(this.f$0, protoOutputStream, (JobStatus) obj);
            }
        });
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }

    public static void lambda$dumpControllerStateLocked$1(DeviceIdleJobsController deviceIdleJobsController, ProtoOutputStream protoOutputStream, JobStatus jobStatus) {
        long jStart = protoOutputStream.start(2246267895810L);
        jobStatus.writeToShortProto(protoOutputStream, 1146756268033L);
        protoOutputStream.write(1120986464258L, jobStatus.getSourceUid());
        protoOutputStream.write(1138166333443L, jobStatus.getSourcePackageName());
        protoOutputStream.write(1133871366148L, (jobStatus.satisfiedConstraints & 33554432) != 0);
        protoOutputStream.write(1133871366149L, jobStatus.dozeWhitelisted);
        protoOutputStream.write(1133871366150L, deviceIdleJobsController.mAllowInIdleJobs.contains(jobStatus));
        protoOutputStream.end(jStart);
    }

    final class DeviceIdleUpdateFunctor implements Consumer<JobStatus> {
        boolean mChanged;

        DeviceIdleUpdateFunctor() {
        }

        @Override
        public void accept(JobStatus jobStatus) {
            this.mChanged = DeviceIdleJobsController.this.updateTaskStateLocked(jobStatus) | this.mChanged;
        }
    }

    final class DeviceIdleJobsDelayHandler extends Handler {
        public DeviceIdleJobsDelayHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                synchronized (DeviceIdleJobsController.this.mLock) {
                    DeviceIdleJobsController.this.mDeviceIdleUpdateFunctor.mChanged = false;
                    DeviceIdleJobsController.this.mService.getJobStore().forEachJob(DeviceIdleJobsController.this.mDeviceIdleUpdateFunctor);
                    if (DeviceIdleJobsController.this.mDeviceIdleUpdateFunctor.mChanged) {
                        DeviceIdleJobsController.this.mStateChangedListener.onControllerStateChanged();
                    }
                }
            }
        }
    }
}
