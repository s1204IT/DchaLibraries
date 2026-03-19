package com.android.server.job;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.IUidObserver;
import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobWorkItem;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryStatsInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.AppStateTracker;
import com.android.server.DeviceIdleController;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.BackgroundJobsController;
import com.android.server.job.controllers.BatteryController;
import com.android.server.job.controllers.ConnectivityController;
import com.android.server.job.controllers.ContentObserverController;
import com.android.server.job.controllers.DeviceIdleJobsController;
import com.android.server.job.controllers.IdleController;
import com.android.server.job.controllers.JobStatus;
import com.android.server.job.controllers.StateController;
import com.android.server.job.controllers.StorageController;
import com.android.server.job.controllers.TimeController;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.PriorityDump;
import com.mediatek.server.powerhal.PowerHalManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import libcore.util.EmptyArray;

public class JobSchedulerService extends SystemService implements StateChangedListener, JobCompletedListener {
    static final int ACTIVE_INDEX = 0;
    public static final boolean DEBUG_STANDBY;
    private static final boolean ENFORCE_MAX_JOBS = true;
    static final int FREQUENT_INDEX = 2;
    static final String HEARTBEAT_TAG = "*job.heartbeat*";
    private static final int MAX_JOBS_PER_APP = 100;
    private static final int MAX_JOB_CONTEXTS_COUNT = 16;
    static final int MSG_CHECK_JOB = 1;
    static final int MSG_CHECK_JOB_GREEDY = 3;
    static final int MSG_JOB_EXPIRED = 0;
    static final int MSG_STOP_JOB = 2;
    static final int MSG_UID_ACTIVE = 6;
    static final int MSG_UID_GONE = 5;
    static final int MSG_UID_IDLE = 7;
    static final int MSG_UID_STATE_CHANGED = 4;
    static final int NEVER_INDEX = 4;
    static final int RARE_INDEX = 3;
    static final int WORKING_INDEX = 1;
    static final Comparator<JobStatus> mEnqueueTimeComparator;

    @VisibleForTesting
    public static Clock sElapsedRealtimeClock;

    @VisibleForTesting
    public static Clock sSystemClock;

    @VisibleForTesting
    public static Clock sUptimeMillisClock;
    final List<JobServiceContext> mActiveServices;
    ActivityManagerInternal mActivityManagerInternal;
    AppStateTracker mAppStateTracker;
    final SparseIntArray mBackingUpUids;
    private final BatteryController mBatteryController;
    IBatteryStats mBatteryStats;
    private final BroadcastReceiver mBroadcastReceiver;
    final Constants mConstants;
    final ConstantsObserver mConstantsObserver;
    private final List<StateController> mControllers;
    private final DeviceIdleJobsController mDeviceIdleJobsController;
    final JobHandler mHandler;
    long mHeartbeat;
    final HeartbeatAlarmListener mHeartbeatAlarm;
    volatile boolean mInParole;
    private final Predicate<Integer> mIsUidActivePredicate;
    final JobPackageTracker mJobPackageTracker;
    final JobSchedulerStub mJobSchedulerStub;
    private final Runnable mJobTimeUpdater;
    final JobStore mJobs;
    long mLastHeartbeatTime;
    final SparseArray<HashMap<String, Long>> mLastJobHeartbeats;
    DeviceIdleController.LocalService mLocalDeviceIdleController;
    PackageManagerInternal mLocalPM;
    final Object mLock;
    int mMaxActiveJobs;
    private final MaybeReadyJobQueueFunctor mMaybeQueueFunctor;
    final long[] mNextBucketHeartbeat;
    final ArrayList<JobStatus> mPendingJobs;
    private final ReadyJobQueueFunctor mReadyQueueFunctor;
    boolean mReadyToRock;
    boolean mReportedActive;
    final StandbyTracker mStandbyTracker;
    int[] mStartedUsers;
    private final StorageController mStorageController;
    private final BroadcastReceiver mTimeSetReceiver;
    boolean[] mTmpAssignAct;
    JobStatus[] mTmpAssignContextIdToJobMap;
    int[] mTmpAssignPreferredUidForContext;
    private final IUidObserver mUidObserver;
    final SparseIntArray mUidPriorityOverride;
    final UsageStatsManagerInternal mUsageStats;
    public static final String TAG = "JobScheduler";
    public static final boolean DEBUG = Log.isLoggable(TAG, 3);

    static {
        DEBUG_STANDBY = DEBUG;
        sSystemClock = Clock.systemUTC();
        sUptimeMillisClock = SystemClock.uptimeMillisClock();
        sElapsedRealtimeClock = SystemClock.elapsedRealtimeClock();
        mEnqueueTimeComparator = new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return JobSchedulerService.lambda$static$0((JobStatus) obj, (JobStatus) obj2);
            }
        };
    }

    private class ConstantsObserver extends ContentObserver {
        private ContentResolver mResolver;

        public ConstantsObserver(Handler handler) {
            super(handler);
        }

        public void start(ContentResolver contentResolver) {
            this.mResolver = contentResolver;
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("job_scheduler_constants"), false, this);
            updateConstants();
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (JobSchedulerService.this.mLock) {
                try {
                    JobSchedulerService.this.mConstants.updateConstantsLocked(Settings.Global.getString(this.mResolver, "job_scheduler_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(JobSchedulerService.TAG, "Bad jobscheduler settings", e);
                }
            }
            JobSchedulerService.this.setNextHeartbeatAlarm();
        }
    }

    public static class Constants {
        private static final int DEFAULT_BG_CRITICAL_JOB_COUNT = 1;
        private static final int DEFAULT_BG_LOW_JOB_COUNT = 1;
        private static final int DEFAULT_BG_MODERATE_JOB_COUNT = 4;
        private static final int DEFAULT_BG_NORMAL_JOB_COUNT = 6;
        private static final float DEFAULT_CONN_CONGESTION_DELAY_FRAC = 0.5f;
        private static final float DEFAULT_CONN_PREFETCH_RELAX_FRAC = 0.5f;
        private static final int DEFAULT_FG_JOB_COUNT = 4;
        private static final float DEFAULT_HEAVY_USE_FACTOR = 0.9f;
        private static final int DEFAULT_MAX_STANDARD_RESCHEDULE_COUNT = Integer.MAX_VALUE;
        private static final int DEFAULT_MAX_WORK_RESCHEDULE_COUNT = Integer.MAX_VALUE;
        private static final int DEFAULT_MIN_BATTERY_NOT_LOW_COUNT = 1;
        private static final int DEFAULT_MIN_CHARGING_COUNT = 1;
        private static final int DEFAULT_MIN_CONNECTIVITY_COUNT = 1;
        private static final int DEFAULT_MIN_CONTENT_COUNT = 1;
        private static final long DEFAULT_MIN_EXP_BACKOFF_TIME = 10000;
        private static final int DEFAULT_MIN_IDLE_COUNT = 1;
        private static final long DEFAULT_MIN_LINEAR_BACKOFF_TIME = 10000;
        private static final int DEFAULT_MIN_READY_JOBS_COUNT = 1;
        private static final int DEFAULT_MIN_STORAGE_NOT_LOW_COUNT = 1;
        private static final float DEFAULT_MODERATE_USE_FACTOR = 0.5f;
        private static final int DEFAULT_STANDBY_FREQUENT_BEATS = 43;
        private static final long DEFAULT_STANDBY_HEARTBEAT_TIME = 660000;
        private static final int DEFAULT_STANDBY_RARE_BEATS = 130;
        private static final int DEFAULT_STANDBY_WORKING_BEATS = 11;
        private static final String KEY_BG_CRITICAL_JOB_COUNT = "bg_critical_job_count";
        private static final String KEY_BG_LOW_JOB_COUNT = "bg_low_job_count";
        private static final String KEY_BG_MODERATE_JOB_COUNT = "bg_moderate_job_count";
        private static final String KEY_BG_NORMAL_JOB_COUNT = "bg_normal_job_count";
        private static final String KEY_CONN_CONGESTION_DELAY_FRAC = "conn_congestion_delay_frac";
        private static final String KEY_CONN_PREFETCH_RELAX_FRAC = "conn_prefetch_relax_frac";
        private static final String KEY_FG_JOB_COUNT = "fg_job_count";
        private static final String KEY_HEAVY_USE_FACTOR = "heavy_use_factor";
        private static final String KEY_MAX_STANDARD_RESCHEDULE_COUNT = "max_standard_reschedule_count";
        private static final String KEY_MAX_WORK_RESCHEDULE_COUNT = "max_work_reschedule_count";
        private static final String KEY_MIN_BATTERY_NOT_LOW_COUNT = "min_battery_not_low_count";
        private static final String KEY_MIN_CHARGING_COUNT = "min_charging_count";
        private static final String KEY_MIN_CONNECTIVITY_COUNT = "min_connectivity_count";
        private static final String KEY_MIN_CONTENT_COUNT = "min_content_count";
        private static final String KEY_MIN_EXP_BACKOFF_TIME = "min_exp_backoff_time";
        private static final String KEY_MIN_IDLE_COUNT = "min_idle_count";
        private static final String KEY_MIN_LINEAR_BACKOFF_TIME = "min_linear_backoff_time";
        private static final String KEY_MIN_READY_JOBS_COUNT = "min_ready_jobs_count";
        private static final String KEY_MIN_STORAGE_NOT_LOW_COUNT = "min_storage_not_low_count";
        private static final String KEY_MODERATE_USE_FACTOR = "moderate_use_factor";
        private static final String KEY_STANDBY_FREQUENT_BEATS = "standby_frequent_beats";
        private static final String KEY_STANDBY_HEARTBEAT_TIME = "standby_heartbeat_time";
        private static final String KEY_STANDBY_RARE_BEATS = "standby_rare_beats";
        private static final String KEY_STANDBY_WORKING_BEATS = "standby_working_beats";
        int MIN_IDLE_COUNT = 1;
        int MIN_CHARGING_COUNT = 1;
        int MIN_BATTERY_NOT_LOW_COUNT = 1;
        int MIN_STORAGE_NOT_LOW_COUNT = 1;
        int MIN_CONNECTIVITY_COUNT = 1;
        int MIN_CONTENT_COUNT = 1;
        int MIN_READY_JOBS_COUNT = 1;
        float HEAVY_USE_FACTOR = DEFAULT_HEAVY_USE_FACTOR;
        float MODERATE_USE_FACTOR = 0.5f;
        int FG_JOB_COUNT = 4;
        int BG_NORMAL_JOB_COUNT = 6;
        int BG_MODERATE_JOB_COUNT = 4;
        int BG_LOW_JOB_COUNT = 1;
        int BG_CRITICAL_JOB_COUNT = 1;
        int MAX_STANDARD_RESCHEDULE_COUNT = Integer.MAX_VALUE;
        int MAX_WORK_RESCHEDULE_COUNT = Integer.MAX_VALUE;
        long MIN_LINEAR_BACKOFF_TIME = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        long MIN_EXP_BACKOFF_TIME = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        long STANDBY_HEARTBEAT_TIME = DEFAULT_STANDBY_HEARTBEAT_TIME;
        final int[] STANDBY_BEATS = {0, 11, 43, DEFAULT_STANDBY_RARE_BEATS};
        public float CONN_CONGESTION_DELAY_FRAC = 0.5f;
        public float CONN_PREFETCH_RELAX_FRAC = 0.5f;
        private final KeyValueListParser mParser = new KeyValueListParser(',');

        void updateConstantsLocked(String str) {
            try {
                this.mParser.setString(str);
            } catch (Exception e) {
                Slog.e(JobSchedulerService.TAG, "Bad jobscheduler settings", e);
            }
            this.MIN_IDLE_COUNT = this.mParser.getInt(KEY_MIN_IDLE_COUNT, 1);
            this.MIN_CHARGING_COUNT = this.mParser.getInt(KEY_MIN_CHARGING_COUNT, 1);
            this.MIN_BATTERY_NOT_LOW_COUNT = this.mParser.getInt(KEY_MIN_BATTERY_NOT_LOW_COUNT, 1);
            this.MIN_STORAGE_NOT_LOW_COUNT = this.mParser.getInt(KEY_MIN_STORAGE_NOT_LOW_COUNT, 1);
            this.MIN_CONNECTIVITY_COUNT = this.mParser.getInt(KEY_MIN_CONNECTIVITY_COUNT, 1);
            this.MIN_CONTENT_COUNT = this.mParser.getInt(KEY_MIN_CONTENT_COUNT, 1);
            this.MIN_READY_JOBS_COUNT = this.mParser.getInt(KEY_MIN_READY_JOBS_COUNT, 1);
            this.HEAVY_USE_FACTOR = this.mParser.getFloat(KEY_HEAVY_USE_FACTOR, DEFAULT_HEAVY_USE_FACTOR);
            this.MODERATE_USE_FACTOR = this.mParser.getFloat(KEY_MODERATE_USE_FACTOR, 0.5f);
            this.FG_JOB_COUNT = this.mParser.getInt(KEY_FG_JOB_COUNT, 4);
            this.BG_NORMAL_JOB_COUNT = this.mParser.getInt(KEY_BG_NORMAL_JOB_COUNT, 6);
            if (this.FG_JOB_COUNT + this.BG_NORMAL_JOB_COUNT > 16) {
                this.BG_NORMAL_JOB_COUNT = 16 - this.FG_JOB_COUNT;
            }
            this.BG_MODERATE_JOB_COUNT = this.mParser.getInt(KEY_BG_MODERATE_JOB_COUNT, 4);
            if (this.FG_JOB_COUNT + this.BG_MODERATE_JOB_COUNT > 16) {
                this.BG_MODERATE_JOB_COUNT = 16 - this.FG_JOB_COUNT;
            }
            this.BG_LOW_JOB_COUNT = this.mParser.getInt(KEY_BG_LOW_JOB_COUNT, 1);
            if (this.FG_JOB_COUNT + this.BG_LOW_JOB_COUNT > 16) {
                this.BG_LOW_JOB_COUNT = 16 - this.FG_JOB_COUNT;
            }
            this.BG_CRITICAL_JOB_COUNT = this.mParser.getInt(KEY_BG_CRITICAL_JOB_COUNT, 1);
            if (this.FG_JOB_COUNT + this.BG_CRITICAL_JOB_COUNT > 16) {
                this.BG_CRITICAL_JOB_COUNT = 16 - this.FG_JOB_COUNT;
            }
            this.MAX_STANDARD_RESCHEDULE_COUNT = this.mParser.getInt(KEY_MAX_STANDARD_RESCHEDULE_COUNT, Integer.MAX_VALUE);
            this.MAX_WORK_RESCHEDULE_COUNT = this.mParser.getInt(KEY_MAX_WORK_RESCHEDULE_COUNT, Integer.MAX_VALUE);
            this.MIN_LINEAR_BACKOFF_TIME = this.mParser.getDurationMillis(KEY_MIN_LINEAR_BACKOFF_TIME, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            this.MIN_EXP_BACKOFF_TIME = this.mParser.getDurationMillis(KEY_MIN_EXP_BACKOFF_TIME, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            this.STANDBY_HEARTBEAT_TIME = this.mParser.getDurationMillis(KEY_STANDBY_HEARTBEAT_TIME, DEFAULT_STANDBY_HEARTBEAT_TIME);
            this.STANDBY_BEATS[1] = this.mParser.getInt(KEY_STANDBY_WORKING_BEATS, 11);
            this.STANDBY_BEATS[2] = this.mParser.getInt(KEY_STANDBY_FREQUENT_BEATS, 43);
            this.STANDBY_BEATS[3] = this.mParser.getInt(KEY_STANDBY_RARE_BEATS, DEFAULT_STANDBY_RARE_BEATS);
            this.CONN_CONGESTION_DELAY_FRAC = this.mParser.getFloat(KEY_CONN_CONGESTION_DELAY_FRAC, 0.5f);
            this.CONN_PREFETCH_RELAX_FRAC = this.mParser.getFloat(KEY_CONN_PREFETCH_RELAX_FRAC, 0.5f);
        }

        void dump(IndentingPrintWriter indentingPrintWriter) {
            indentingPrintWriter.println("Settings:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.printPair(KEY_MIN_IDLE_COUNT, Integer.valueOf(this.MIN_IDLE_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MIN_CHARGING_COUNT, Integer.valueOf(this.MIN_CHARGING_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MIN_BATTERY_NOT_LOW_COUNT, Integer.valueOf(this.MIN_BATTERY_NOT_LOW_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MIN_STORAGE_NOT_LOW_COUNT, Integer.valueOf(this.MIN_STORAGE_NOT_LOW_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MIN_CONNECTIVITY_COUNT, Integer.valueOf(this.MIN_CONNECTIVITY_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MIN_CONTENT_COUNT, Integer.valueOf(this.MIN_CONTENT_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MIN_READY_JOBS_COUNT, Integer.valueOf(this.MIN_READY_JOBS_COUNT)).println();
            indentingPrintWriter.printPair(KEY_HEAVY_USE_FACTOR, Float.valueOf(this.HEAVY_USE_FACTOR)).println();
            indentingPrintWriter.printPair(KEY_MODERATE_USE_FACTOR, Float.valueOf(this.MODERATE_USE_FACTOR)).println();
            indentingPrintWriter.printPair(KEY_FG_JOB_COUNT, Integer.valueOf(this.FG_JOB_COUNT)).println();
            indentingPrintWriter.printPair(KEY_BG_NORMAL_JOB_COUNT, Integer.valueOf(this.BG_NORMAL_JOB_COUNT)).println();
            indentingPrintWriter.printPair(KEY_BG_MODERATE_JOB_COUNT, Integer.valueOf(this.BG_MODERATE_JOB_COUNT)).println();
            indentingPrintWriter.printPair(KEY_BG_LOW_JOB_COUNT, Integer.valueOf(this.BG_LOW_JOB_COUNT)).println();
            indentingPrintWriter.printPair(KEY_BG_CRITICAL_JOB_COUNT, Integer.valueOf(this.BG_CRITICAL_JOB_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MAX_STANDARD_RESCHEDULE_COUNT, Integer.valueOf(this.MAX_STANDARD_RESCHEDULE_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MAX_WORK_RESCHEDULE_COUNT, Integer.valueOf(this.MAX_WORK_RESCHEDULE_COUNT)).println();
            indentingPrintWriter.printPair(KEY_MIN_LINEAR_BACKOFF_TIME, Long.valueOf(this.MIN_LINEAR_BACKOFF_TIME)).println();
            indentingPrintWriter.printPair(KEY_MIN_EXP_BACKOFF_TIME, Long.valueOf(this.MIN_EXP_BACKOFF_TIME)).println();
            indentingPrintWriter.printPair(KEY_STANDBY_HEARTBEAT_TIME, Long.valueOf(this.STANDBY_HEARTBEAT_TIME)).println();
            indentingPrintWriter.print("standby_beats={");
            indentingPrintWriter.print(this.STANDBY_BEATS[0]);
            for (int i = 1; i < this.STANDBY_BEATS.length; i++) {
                indentingPrintWriter.print(", ");
                indentingPrintWriter.print(this.STANDBY_BEATS[i]);
            }
            indentingPrintWriter.println('}');
            indentingPrintWriter.printPair(KEY_CONN_CONGESTION_DELAY_FRAC, Float.valueOf(this.CONN_CONGESTION_DELAY_FRAC)).println();
            indentingPrintWriter.printPair(KEY_CONN_PREFETCH_RELAX_FRAC, Float.valueOf(this.CONN_PREFETCH_RELAX_FRAC)).println();
            indentingPrintWriter.decreaseIndent();
        }

        void dump(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1120986464257L, this.MIN_IDLE_COUNT);
            protoOutputStream.write(1120986464258L, this.MIN_CHARGING_COUNT);
            protoOutputStream.write(1120986464259L, this.MIN_BATTERY_NOT_LOW_COUNT);
            protoOutputStream.write(1120986464260L, this.MIN_STORAGE_NOT_LOW_COUNT);
            protoOutputStream.write(1120986464261L, this.MIN_CONNECTIVITY_COUNT);
            protoOutputStream.write(1120986464262L, this.MIN_CONTENT_COUNT);
            protoOutputStream.write(1120986464263L, this.MIN_READY_JOBS_COUNT);
            protoOutputStream.write(1103806595080L, this.HEAVY_USE_FACTOR);
            protoOutputStream.write(1103806595081L, this.MODERATE_USE_FACTOR);
            protoOutputStream.write(1120986464266L, this.FG_JOB_COUNT);
            protoOutputStream.write(1120986464267L, this.BG_NORMAL_JOB_COUNT);
            protoOutputStream.write(1120986464268L, this.BG_MODERATE_JOB_COUNT);
            protoOutputStream.write(1120986464269L, this.BG_LOW_JOB_COUNT);
            protoOutputStream.write(1120986464270L, this.BG_CRITICAL_JOB_COUNT);
            protoOutputStream.write(1120986464271L, this.MAX_STANDARD_RESCHEDULE_COUNT);
            protoOutputStream.write(1120986464272L, this.MAX_WORK_RESCHEDULE_COUNT);
            protoOutputStream.write(1112396529681L, this.MIN_LINEAR_BACKOFF_TIME);
            protoOutputStream.write(1112396529682L, this.MIN_EXP_BACKOFF_TIME);
            protoOutputStream.write(1112396529683L, this.STANDBY_HEARTBEAT_TIME);
            for (int i : this.STANDBY_BEATS) {
                protoOutputStream.write(2220498092052L, i);
            }
            protoOutputStream.write(1103806595093L, this.CONN_CONGESTION_DELAY_FRAC);
            protoOutputStream.write(1103806595094L, this.CONN_PREFETCH_RELAX_FRAC);
            protoOutputStream.end(jStart);
        }
    }

    static int lambda$static$0(JobStatus jobStatus, JobStatus jobStatus2) {
        if (jobStatus.enqueueTime < jobStatus2.enqueueTime) {
            return -1;
        }
        return jobStatus.enqueueTime > jobStatus2.enqueueTime ? 1 : 0;
    }

    static <T> void addOrderedItem(ArrayList<T> arrayList, T t, Comparator<T> comparator) {
        int iBinarySearch = Collections.binarySearch(arrayList, t, comparator);
        if (iBinarySearch < 0) {
            iBinarySearch = ~iBinarySearch;
        }
        arrayList.add(iBinarySearch, t);
    }

    private String getPackageName(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            return data.getSchemeSpecificPart();
        }
        return null;
    }

    public Context getTestableContext() {
        return getContext();
    }

    public Object getLock() {
        return this.mLock;
    }

    public JobStore getJobStore() {
        return this.mJobs;
    }

    public Constants getConstants() {
        return this.mConstants;
    }

    @Override
    public void onStartUser(int i) {
        this.mStartedUsers = ArrayUtils.appendInt(this.mStartedUsers, i);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    @Override
    public void onUnlockUser(int i) {
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    @Override
    public void onStopUser(int i) {
        this.mStartedUsers = ArrayUtils.removeInt(this.mStartedUsers, i);
    }

    private boolean isUidActive(int i) {
        return this.mAppStateTracker.isUidActiveSynced(i);
    }

    public int scheduleAsPackage(JobInfo jobInfo, JobWorkItem jobWorkItem, int i, String str, int i2, String str2) {
        try {
            if (ActivityManager.getService().isAppStartModeDisabled(i, jobInfo.getService().getPackageName())) {
                Slog.w(TAG, "Not scheduling job " + i + ":" + jobInfo.toString() + " -- package not allowed to start");
                return 0;
            }
        } catch (RemoteException e) {
        }
        synchronized (this.mLock) {
            JobStatus jobByUidAndJobId = this.mJobs.getJobByUidAndJobId(i, jobInfo.getId());
            if (jobWorkItem != null && jobByUidAndJobId != null && jobByUidAndJobId.getJob().equals(jobInfo)) {
                jobByUidAndJobId.enqueueWorkLocked(ActivityManager.getService(), jobWorkItem);
                jobByUidAndJobId.maybeAddForegroundExemption(this.mIsUidActivePredicate);
                return 1;
            }
            JobStatus jobStatusCreateFromJobInfo = JobStatus.createFromJobInfo(jobInfo, i, str, i2, str2);
            jobStatusCreateFromJobInfo.maybeAddForegroundExemption(this.mIsUidActivePredicate);
            if (DEBUG) {
                Slog.d(TAG, "SCHEDULE: " + jobStatusCreateFromJobInfo.toShortString());
            }
            if (str == null && this.mJobs.countJobsForUid(i) > 100) {
                Slog.w(TAG, "Too many jobs for uid " + i);
                throw new IllegalStateException("Apps may not schedule more than 100 distinct jobs");
            }
            jobStatusCreateFromJobInfo.prepareLocked(ActivityManager.getService());
            if (jobWorkItem != null) {
                jobStatusCreateFromJobInfo.enqueueWorkLocked(ActivityManager.getService(), jobWorkItem);
            }
            if (jobByUidAndJobId != null) {
                cancelJobImplLocked(jobByUidAndJobId, jobStatusCreateFromJobInfo, "job rescheduled by app");
            } else {
                startTrackingJobLocked(jobStatusCreateFromJobInfo, null);
            }
            StatsLog.write_non_chained(8, i, null, jobStatusCreateFromJobInfo.getBatteryName(), 2, 0);
            if (isReadyToBeExecutedLocked(jobStatusCreateFromJobInfo)) {
                this.mJobPackageTracker.notePending(jobStatusCreateFromJobInfo);
                addOrderedItem(this.mPendingJobs, jobStatusCreateFromJobInfo, mEnqueueTimeComparator);
                maybeRunPendingJobsLocked();
            }
            return 1;
        }
    }

    public List<JobInfo> getPendingJobs(int i) {
        ArrayList arrayList;
        synchronized (this.mLock) {
            List<JobStatus> jobsByUid = this.mJobs.getJobsByUid(i);
            arrayList = new ArrayList(jobsByUid.size());
            for (int size = jobsByUid.size() - 1; size >= 0; size--) {
                arrayList.add(jobsByUid.get(size).getJob());
            }
        }
        return arrayList;
    }

    public JobInfo getPendingJob(int i, int i2) {
        synchronized (this.mLock) {
            List<JobStatus> jobsByUid = this.mJobs.getJobsByUid(i);
            for (int size = jobsByUid.size() - 1; size >= 0; size--) {
                JobStatus jobStatus = jobsByUid.get(size);
                if (jobStatus.getJobId() == i2) {
                    return jobStatus.getJob();
                }
            }
            return null;
        }
    }

    void cancelJobsForUser(int i) {
        synchronized (this.mLock) {
            List<JobStatus> jobsByUser = this.mJobs.getJobsByUser(i);
            for (int i2 = 0; i2 < jobsByUser.size(); i2++) {
                cancelJobImplLocked(jobsByUser.get(i2), null, "user removed");
            }
        }
    }

    private void cancelJobsForNonExistentUsers() {
        UserManagerInternal userManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        synchronized (this.mLock) {
            this.mJobs.removeJobsOfNonUsers(userManagerInternal.getUserIds());
        }
    }

    void cancelJobsForPackageAndUid(String str, int i, String str2) {
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str)) {
            Slog.wtfStack(TAG, "Can't cancel all jobs for system package");
            return;
        }
        synchronized (this.mLock) {
            List<JobStatus> jobsByUid = this.mJobs.getJobsByUid(i);
            for (int size = jobsByUid.size() - 1; size >= 0; size--) {
                JobStatus jobStatus = jobsByUid.get(size);
                if (jobStatus.getSourcePackageName().equals(str)) {
                    cancelJobImplLocked(jobStatus, null, str2);
                }
            }
        }
    }

    public boolean cancelJobsForUid(int i, String str) {
        boolean z;
        int i2 = 0;
        if (i == 1000) {
            Slog.wtfStack(TAG, "Can't cancel all jobs for system uid");
            return false;
        }
        synchronized (this.mLock) {
            List<JobStatus> jobsByUid = this.mJobs.getJobsByUid(i);
            z = false;
            while (i2 < jobsByUid.size()) {
                cancelJobImplLocked(jobsByUid.get(i2), null, str);
                i2++;
                z = true;
            }
        }
        return z;
    }

    public boolean cancelJob(int i, int i2, int i3) {
        boolean z;
        synchronized (this.mLock) {
            JobStatus jobByUidAndJobId = this.mJobs.getJobByUidAndJobId(i, i2);
            if (jobByUidAndJobId != null) {
                cancelJobImplLocked(jobByUidAndJobId, null, "cancel() called by app, callingUid=" + i3 + " uid=" + i + " jobId=" + i2);
            }
            z = jobByUidAndJobId != null;
        }
        return z;
    }

    private void cancelJobImplLocked(JobStatus jobStatus, JobStatus jobStatus2, String str) {
        if (DEBUG) {
            Slog.d(TAG, "CANCEL: " + jobStatus.toShortString());
        }
        jobStatus.unprepareLocked(ActivityManager.getService());
        stopTrackingJobLocked(jobStatus, jobStatus2, true);
        if (this.mPendingJobs.remove(jobStatus)) {
            this.mJobPackageTracker.noteNonpending(jobStatus);
        }
        stopJobOnServiceContextLocked(jobStatus, 0, str);
        if (jobStatus2 != null) {
            if (DEBUG) {
                Slog.i(TAG, "Tracking replacement job " + jobStatus2.toShortString());
            }
            startTrackingJobLocked(jobStatus2, jobStatus);
        }
        reportActiveLocked();
    }

    void updateUidState(int i, int i2) {
        synchronized (this.mLock) {
            try {
                if (i2 == 2) {
                    this.mUidPriorityOverride.put(i, 40);
                } else if (i2 <= 4) {
                    this.mUidPriorityOverride.put(i, 30);
                } else {
                    this.mUidPriorityOverride.delete(i);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override
    public void onDeviceIdleStateChanged(boolean z) {
        synchronized (this.mLock) {
            if (z) {
                for (int i = 0; i < this.mActiveServices.size(); i++) {
                    JobServiceContext jobServiceContext = this.mActiveServices.get(i);
                    JobStatus runningJobLocked = jobServiceContext.getRunningJobLocked();
                    if (runningJobLocked != null && (runningJobLocked.getFlags() & 1) == 0) {
                        jobServiceContext.cancelExecutingJobLocked(4, "cancelled due to doze");
                    }
                }
            } else if (this.mReadyToRock) {
                if (this.mLocalDeviceIdleController != null && !this.mReportedActive) {
                    this.mReportedActive = true;
                    this.mLocalDeviceIdleController.setJobsActive(true);
                }
                this.mHandler.obtainMessage(1).sendToTarget();
            }
        }
    }

    void reportActiveLocked() {
        int i = 0;
        boolean z = this.mPendingJobs.size() > 0;
        if (this.mPendingJobs.size() <= 0) {
            while (true) {
                if (i < this.mActiveServices.size()) {
                    JobStatus runningJobLocked = this.mActiveServices.get(i).getRunningJobLocked();
                    if (runningJobLocked == null || (runningJobLocked.getJob().getFlags() & 1) != 0 || runningJobLocked.dozeWhitelisted || runningJobLocked.uidActive) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        if (this.mReportedActive != z) {
            this.mReportedActive = z;
            if (this.mLocalDeviceIdleController != null) {
                this.mLocalDeviceIdleController.setJobsActive(z);
            }
        }
    }

    void reportAppUsage(String str, int i) {
    }

    public JobSchedulerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mJobPackageTracker = new JobPackageTracker();
        this.mActiveServices = new ArrayList();
        this.mPendingJobs = new ArrayList<>();
        this.mStartedUsers = EmptyArray.INT;
        this.mMaxActiveJobs = 1;
        this.mUidPriorityOverride = new SparseIntArray();
        this.mBackingUpUids = new SparseIntArray();
        this.mNextBucketHeartbeat = new long[]{0, 0, 0, 0, JobStatus.NO_LATEST_RUNTIME};
        this.mHeartbeat = 0L;
        this.mLastHeartbeatTime = sElapsedRealtimeClock.millis();
        this.mLastJobHeartbeats = new SparseArray<>();
        this.mHeartbeatAlarm = new HeartbeatAlarmListener();
        this.mTmpAssignContextIdToJobMap = new JobStatus[16];
        this.mTmpAssignAct = new boolean[16];
        this.mTmpAssignPreferredUidForContext = new int[16];
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                List<JobStatus> jobsByUid;
                String action = intent.getAction();
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "Receieved: " + action);
                }
                String packageName = JobSchedulerService.this.getPackageName(intent);
                int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
                if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                    if (packageName != null && intExtra != -1) {
                        String[] stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                        if (stringArrayExtra != null) {
                            for (String str : stringArrayExtra) {
                                if (str.equals(packageName)) {
                                    if (JobSchedulerService.DEBUG) {
                                        Slog.d(JobSchedulerService.TAG, "Package state change: " + packageName);
                                    }
                                    try {
                                        int userId = UserHandle.getUserId(intExtra);
                                        int applicationEnabledSetting = AppGlobals.getPackageManager().getApplicationEnabledSetting(packageName, userId);
                                        if (applicationEnabledSetting == 2 || applicationEnabledSetting == 3) {
                                            if (JobSchedulerService.DEBUG) {
                                                Slog.d(JobSchedulerService.TAG, "Removing jobs for package " + packageName + " in user " + userId);
                                            }
                                            JobSchedulerService.this.cancelJobsForPackageAndUid(packageName, intExtra, "app disabled");
                                            return;
                                        }
                                        return;
                                    } catch (RemoteException | IllegalArgumentException e) {
                                        return;
                                    }
                                }
                            }
                            return;
                        }
                        return;
                    }
                    Slog.w(JobSchedulerService.TAG, "PACKAGE_CHANGED for " + packageName + " / uid " + intExtra);
                    return;
                }
                if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        int intExtra2 = intent.getIntExtra("android.intent.extra.UID", -1);
                        if (JobSchedulerService.DEBUG) {
                            Slog.d(JobSchedulerService.TAG, "Removing jobs for uid: " + intExtra2);
                        }
                        JobSchedulerService.this.cancelJobsForPackageAndUid(packageName, intExtra2, "app uninstalled");
                        return;
                    }
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    int intExtra3 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Removing jobs for user: " + intExtra3);
                    }
                    JobSchedulerService.this.cancelJobsForUser(intExtra3);
                    return;
                }
                if ("android.intent.action.QUERY_PACKAGE_RESTART".equals(action)) {
                    if (intExtra != -1) {
                        synchronized (JobSchedulerService.this.mLock) {
                            jobsByUid = JobSchedulerService.this.mJobs.getJobsByUid(intExtra);
                        }
                        for (int size = jobsByUid.size() - 1; size >= 0; size--) {
                            if (jobsByUid.get(size).getSourcePackageName().equals(packageName)) {
                                if (JobSchedulerService.DEBUG) {
                                    Slog.d(JobSchedulerService.TAG, "Restart query: package " + packageName + " at uid " + intExtra + " has jobs");
                                }
                                setResultCode(-1);
                                return;
                            }
                        }
                        return;
                    }
                    return;
                }
                if ("android.intent.action.PACKAGE_RESTARTED".equals(action) && intExtra != -1) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Removing jobs for pkg " + packageName + " at uid " + intExtra);
                    }
                    JobSchedulerService.this.cancelJobsForPackageAndUid(packageName, intExtra, "app force stopped");
                }
            }
        };
        this.mUidObserver = new IUidObserver.Stub() {
            public void onUidStateChanged(int i, int i2, long j) {
                JobSchedulerService.this.mHandler.obtainMessage(4, i, i2).sendToTarget();
            }

            public void onUidGone(int i, boolean z) {
                JobSchedulerService.this.mHandler.obtainMessage(5, i, z ? 1 : 0).sendToTarget();
            }

            public void onUidActive(int i) throws RemoteException {
                JobSchedulerService.this.mHandler.obtainMessage(6, i, 0).sendToTarget();
            }

            public void onUidIdle(int i, boolean z) {
                JobSchedulerService.this.mHandler.obtainMessage(7, i, z ? 1 : 0).sendToTarget();
            }

            public void onUidCachedChanged(int i, boolean z) {
            }
        };
        this.mIsUidActivePredicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return this.f$0.isUidActive(((Integer) obj).intValue());
            }
        };
        this.mTimeSetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.TIME_SET".equals(intent.getAction()) && JobSchedulerService.this.mJobs.clockNowValidToInflate(JobSchedulerService.sSystemClock.millis())) {
                    Slog.i(JobSchedulerService.TAG, "RTC now valid; recalculating persisted job windows");
                    context2.unregisterReceiver(this);
                    FgThread.getHandler().post(JobSchedulerService.this.mJobTimeUpdater);
                }
            }
        };
        this.mJobTimeUpdater = new Runnable() {
            @Override
            public final void run() {
                JobSchedulerService.lambda$new$1(this.f$0);
            }
        };
        this.mReadyQueueFunctor = new ReadyJobQueueFunctor();
        this.mMaybeQueueFunctor = new MaybeReadyJobQueueFunctor();
        this.mLocalPM = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) Preconditions.checkNotNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        this.mHandler = new JobHandler(context.getMainLooper());
        this.mConstants = new Constants();
        this.mConstantsObserver = new ConstantsObserver(this.mHandler);
        this.mJobSchedulerStub = new JobSchedulerStub();
        this.mStandbyTracker = new StandbyTracker();
        this.mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        this.mUsageStats.addAppIdleStateChangeListener(this.mStandbyTracker);
        publishLocalService(JobSchedulerInternal.class, new LocalService());
        this.mJobs = JobStore.initAndGet(this);
        this.mControllers = new ArrayList();
        this.mControllers.add(new ConnectivityController(this));
        this.mControllers.add(new TimeController(this));
        this.mControllers.add(new IdleController(this));
        this.mBatteryController = new BatteryController(this);
        this.mControllers.add(this.mBatteryController);
        this.mStorageController = new StorageController(this);
        this.mControllers.add(this.mStorageController);
        this.mControllers.add(new BackgroundJobsController(this));
        this.mControllers.add(new ContentObserverController(this));
        this.mDeviceIdleJobsController = new DeviceIdleJobsController(this);
        this.mControllers.add(this.mDeviceIdleJobsController);
        if (!this.mJobs.jobTimesInflatedValid()) {
            Slog.w(TAG, "!!! RTC not yet good; tracking time updates for job scheduling");
            context.registerReceiver(this.mTimeSetReceiver, new IntentFilter("android.intent.action.TIME_SET"));
        }
    }

    public static void lambda$new$1(JobSchedulerService jobSchedulerService) {
        ArrayList<JobStatus> arrayList = new ArrayList<>();
        ArrayList<JobStatus> arrayList2 = new ArrayList<>();
        synchronized (jobSchedulerService.mLock) {
            jobSchedulerService.getJobStore().getRtcCorrectedJobsLocked(arrayList2, arrayList);
            int size = arrayList2.size();
            for (int i = 0; i < size; i++) {
                JobStatus jobStatus = arrayList.get(i);
                JobStatus jobStatus2 = arrayList2.get(i);
                if (DEBUG) {
                    Slog.v(TAG, "  replacing " + jobStatus + " with " + jobStatus2);
                }
                jobSchedulerService.cancelJobImplLocked(jobStatus, jobStatus2, "deferred rtc calculation");
            }
        }
    }

    @Override
    public void onStart() {
        publishBinderService("jobscheduler", this.mJobSchedulerStub);
    }

    @Override
    public void onBootPhase(int i) {
        if (500 != i) {
            if (i == 600) {
                synchronized (this.mLock) {
                    this.mReadyToRock = true;
                    this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                    this.mLocalDeviceIdleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
                    for (int i2 = 0; i2 < 16; i2++) {
                        this.mActiveServices.add(new JobServiceContext(this, this.mBatteryStats, this.mJobPackageTracker, getContext().getMainLooper()));
                    }
                    this.mJobs.forEachJob(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            JobSchedulerService.lambda$onBootPhase$2(this.f$0, (JobStatus) obj);
                        }
                    });
                    this.mHandler.obtainMessage(1).sendToTarget();
                }
                return;
            }
            return;
        }
        this.mConstantsObserver.start(getContext().getContentResolver());
        this.mAppStateTracker = (AppStateTracker) Preconditions.checkNotNull((AppStateTracker) LocalServices.getService(AppStateTracker.class));
        setNextHeartbeatAlarm();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
        intentFilter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
        intentFilter.addDataScheme(com.android.server.pm.Settings.ATTR_PACKAGE);
        getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_REMOVED"), null, null);
        try {
            ActivityManager.getService().registerUidObserver(this.mUidObserver, 15, -1, (String) null);
        } catch (RemoteException e) {
        }
        cancelJobsForNonExistentUsers();
    }

    public static void lambda$onBootPhase$2(JobSchedulerService jobSchedulerService, JobStatus jobStatus) {
        for (int i = 0; i < jobSchedulerService.mControllers.size(); i++) {
            jobSchedulerService.mControllers.get(i).maybeStartTrackingJobLocked(jobStatus, null);
        }
    }

    private void startTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if (!jobStatus.isPreparedLocked()) {
            Slog.wtf(TAG, "Not yet prepared when started tracking: " + jobStatus);
        }
        jobStatus.enqueueTime = sElapsedRealtimeClock.millis();
        boolean zAdd = this.mJobs.add(jobStatus);
        if (this.mReadyToRock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                StateController stateController = this.mControllers.get(i);
                if (zAdd) {
                    stateController.maybeStopTrackingJobLocked(jobStatus, null, true);
                }
                stateController.maybeStartTrackingJobLocked(jobStatus, jobStatus2);
            }
        }
    }

    private boolean stopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
        jobStatus.stopTrackingJobLocked(ActivityManager.getService(), jobStatus2);
        boolean zRemove = this.mJobs.remove(jobStatus, z);
        if (zRemove && this.mReadyToRock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                this.mControllers.get(i).maybeStopTrackingJobLocked(jobStatus, jobStatus2, false);
            }
        }
        return zRemove;
    }

    private boolean stopJobOnServiceContextLocked(JobStatus jobStatus, int i, String str) {
        for (int i2 = 0; i2 < this.mActiveServices.size(); i2++) {
            JobServiceContext jobServiceContext = this.mActiveServices.get(i2);
            JobStatus runningJobLocked = jobServiceContext.getRunningJobLocked();
            if (runningJobLocked != null && runningJobLocked.matches(jobStatus.getUid(), jobStatus.getJobId())) {
                jobServiceContext.cancelExecutingJobLocked(i, str);
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentlyActiveLocked(JobStatus jobStatus) {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobStatus runningJobLocked = this.mActiveServices.get(i).getRunningJobLocked();
            if (runningJobLocked != null && runningJobLocked.matches(jobStatus.getUid(), jobStatus.getJobId())) {
                return true;
            }
        }
        return false;
    }

    void noteJobsPending(List<JobStatus> list) {
        for (int size = list.size() - 1; size >= 0; size--) {
            this.mJobPackageTracker.notePending(list.get(size));
        }
    }

    void noteJobsNonpending(List<JobStatus> list) {
        for (int size = list.size() - 1; size >= 0; size--) {
            this.mJobPackageTracker.noteNonpending(list.get(size));
        }
    }

    private JobStatus getRescheduleJobForFailureLocked(JobStatus jobStatus) {
        long jScalb;
        long jMillis = sElapsedRealtimeClock.millis();
        JobInfo job = jobStatus.getJob();
        long initialBackoffMillis = job.getInitialBackoffMillis();
        int numFailures = jobStatus.getNumFailures() + 1;
        if (jobStatus.hasWorkLocked()) {
            if (numFailures > this.mConstants.MAX_WORK_RESCHEDULE_COUNT) {
                Slog.w(TAG, "Not rescheduling " + jobStatus + ": attempt #" + numFailures + " > work limit " + this.mConstants.MAX_STANDARD_RESCHEDULE_COUNT);
                return null;
            }
        } else if (numFailures > this.mConstants.MAX_STANDARD_RESCHEDULE_COUNT) {
            Slog.w(TAG, "Not rescheduling " + jobStatus + ": attempt #" + numFailures + " > std limit " + this.mConstants.MAX_STANDARD_RESCHEDULE_COUNT);
            return null;
        }
        switch (job.getBackoffPolicy()) {
            case 0:
                if (initialBackoffMillis < this.mConstants.MIN_LINEAR_BACKOFF_TIME) {
                    initialBackoffMillis = this.mConstants.MIN_LINEAR_BACKOFF_TIME;
                }
                jScalb = initialBackoffMillis * ((long) numFailures);
                break;
            default:
                if (DEBUG) {
                    Slog.v(TAG, "Unrecognised back-off policy, defaulting to exponential.");
                    break;
                }
            case 1:
                if (initialBackoffMillis < this.mConstants.MIN_EXP_BACKOFF_TIME) {
                    initialBackoffMillis = this.mConstants.MIN_EXP_BACKOFF_TIME;
                }
                jScalb = (long) Math.scalb(initialBackoffMillis, numFailures - 1);
                break;
        }
        JobStatus jobStatus2 = new JobStatus(jobStatus, getCurrentHeartbeat(), jMillis + Math.min(jScalb, 18000000L), JobStatus.NO_LATEST_RUNTIME, numFailures, jobStatus.getLastSuccessfulRunTime(), sSystemClock.millis());
        for (int i = 0; i < this.mControllers.size(); i++) {
            this.mControllers.get(i).rescheduleForFailureLocked(jobStatus2, jobStatus);
        }
        return jobStatus2;
    }

    private JobStatus getRescheduleJobForPeriodic(JobStatus jobStatus) {
        long jMillis = sElapsedRealtimeClock.millis();
        long jMax = jobStatus.hasDeadlineConstraint() ? Math.max(jobStatus.getLatestRunTimeElapsed() - jMillis, 0L) : 0L;
        long flexMillis = jobStatus.getJob().getFlexMillis();
        long intervalMillis = jMillis + jMax + jobStatus.getJob().getIntervalMillis();
        long j = intervalMillis - flexMillis;
        if (DEBUG) {
            Slog.v(TAG, "Rescheduling executed periodic. New execution window [" + (j / 1000) + ", " + (intervalMillis / 1000) + "]s");
        }
        return new JobStatus(jobStatus, getCurrentHeartbeat(), j, intervalMillis, 0, sSystemClock.millis(), jobStatus.getLastFailedRunTime());
    }

    long heartbeatWhenJobsLastRun(String str, int i) {
        long jLongValue;
        boolean z;
        long j = -this.mConstants.STANDBY_BEATS[3];
        synchronized (this.mLock) {
            HashMap<String, Long> map = this.mLastJobHeartbeats.get(i);
            if (map != null) {
                jLongValue = map.getOrDefault(str, Long.valueOf(JobStatus.NO_LATEST_RUNTIME)).longValue();
                if (jLongValue < JobStatus.NO_LATEST_RUNTIME) {
                    z = true;
                } else {
                    jLongValue = j;
                    z = false;
                }
                if (!z) {
                    long timeSinceLastJobRun = this.mUsageStats.getTimeSinceLastJobRun(str, i);
                    if (timeSinceLastJobRun < JobStatus.NO_LATEST_RUNTIME) {
                        jLongValue = this.mHeartbeat - (timeSinceLastJobRun / this.mConstants.STANDBY_HEARTBEAT_TIME);
                    }
                    setLastJobHeartbeatLocked(str, i, jLongValue);
                }
            }
        }
        if (DEBUG_STANDBY) {
            Slog.v(TAG, "Last job heartbeat " + jLongValue + " for " + str + SliceClientPermissions.SliceAuthority.DELIMITER + i);
        }
        return jLongValue;
    }

    long heartbeatWhenJobsLastRun(JobStatus jobStatus) {
        return heartbeatWhenJobsLastRun(jobStatus.getSourcePackageName(), jobStatus.getSourceUserId());
    }

    void setLastJobHeartbeatLocked(String str, int i, long j) {
        HashMap<String, Long> map = this.mLastJobHeartbeats.get(i);
        if (map == null) {
            map = new HashMap<>();
            this.mLastJobHeartbeats.put(i, map);
        }
        map.put(str, Long.valueOf(j));
    }

    @Override
    public void onJobCompletedLocked(JobStatus jobStatus, boolean z) {
        if (DEBUG) {
            Slog.d(TAG, "Completed " + jobStatus + ", reschedule=" + z);
        }
        JobStatus rescheduleJobForFailureLocked = z ? getRescheduleJobForFailureLocked(jobStatus) : null;
        if (!stopTrackingJobLocked(jobStatus, rescheduleJobForFailureLocked, !jobStatus.getJob().isPeriodic())) {
            if (DEBUG) {
                Slog.d(TAG, "Could not find job to remove. Was job removed while executing?");
            }
            this.mHandler.obtainMessage(3).sendToTarget();
            return;
        }
        if (rescheduleJobForFailureLocked != null) {
            try {
                rescheduleJobForFailureLocked.prepareLocked(ActivityManager.getService());
            } catch (SecurityException e) {
                Slog.w(TAG, "Unable to regrant job permissions for " + rescheduleJobForFailureLocked);
            }
            startTrackingJobLocked(rescheduleJobForFailureLocked, jobStatus);
        } else if (jobStatus.getJob().isPeriodic()) {
            JobStatus rescheduleJobForPeriodic = getRescheduleJobForPeriodic(jobStatus);
            try {
                rescheduleJobForPeriodic.prepareLocked(ActivityManager.getService());
            } catch (SecurityException e2) {
                Slog.w(TAG, "Unable to regrant job permissions for " + rescheduleJobForPeriodic);
            }
            startTrackingJobLocked(rescheduleJobForPeriodic, jobStatus);
        }
        jobStatus.unprepareLocked(ActivityManager.getService());
        reportActiveLocked();
        this.mHandler.obtainMessage(3).sendToTarget();
    }

    @Override
    public void onControllerStateChanged() {
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    @Override
    public void onRunJobNow(JobStatus jobStatus) {
        this.mHandler.obtainMessage(0, jobStatus).sendToTarget();
    }

    private final class JobHandler extends Handler {
        public JobHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            synchronized (JobSchedulerService.this.mLock) {
                if (JobSchedulerService.this.mReadyToRock) {
                    boolean z = true;
                    switch (message.what) {
                        case 0:
                            JobStatus jobStatus = (JobStatus) message.obj;
                            if (jobStatus == null || !JobSchedulerService.this.isReadyToBeExecutedLocked(jobStatus)) {
                                JobSchedulerService.this.queueReadyJobsForExecutionLocked();
                            } else {
                                JobSchedulerService.this.mJobPackageTracker.notePending(jobStatus);
                                JobSchedulerService.addOrderedItem(JobSchedulerService.this.mPendingJobs, jobStatus, JobSchedulerService.mEnqueueTimeComparator);
                            }
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                        case 1:
                            removeMessages(1);
                            if (JobSchedulerService.this.mReportedActive) {
                                JobSchedulerService.this.queueReadyJobsForExecutionLocked();
                            } else {
                                JobSchedulerService.this.maybeQueueReadyJobsForExecutionLocked();
                            }
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                        case 2:
                            JobSchedulerService.this.cancelJobImplLocked((JobStatus) message.obj, null, "app no longer allowed to run");
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                        case 3:
                            JobSchedulerService.this.queueReadyJobsForExecutionLocked();
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                        case 4:
                            JobSchedulerService.this.updateUidState(message.arg1, message.arg2);
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                        case 5:
                            int i = message.arg1;
                            if (message.arg2 == 0) {
                                z = false;
                            }
                            JobSchedulerService.this.updateUidState(i, 18);
                            if (z) {
                                JobSchedulerService.this.cancelJobsForUid(i, "uid gone");
                            }
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(i, false);
                                break;
                            }
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                        case 6:
                            int i2 = message.arg1;
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(i2, true);
                                break;
                            }
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                        case 7:
                            int i3 = message.arg1;
                            if (message.arg2 == 0) {
                                z = false;
                            }
                            if (z) {
                                JobSchedulerService.this.cancelJobsForUid(i3, "app uid idle");
                            }
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(i3, false);
                                break;
                            }
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                        default:
                            JobSchedulerService.this.maybeRunPendingJobsLocked();
                            return;
                    }
                }
            }
        }
    }

    private void stopNonReadyActiveJobsLocked() {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext jobServiceContext = this.mActiveServices.get(i);
            JobStatus runningJobLocked = jobServiceContext.getRunningJobLocked();
            if (runningJobLocked != null && !runningJobLocked.isReady()) {
                jobServiceContext.cancelExecutingJobLocked(1, "cancelled due to unsatisfied constraints");
            }
        }
    }

    private void queueReadyJobsForExecutionLocked() {
        if (DEBUG) {
            Slog.d(TAG, "queuing all ready jobs for execution:");
        }
        noteJobsNonpending(this.mPendingJobs);
        this.mPendingJobs.clear();
        stopNonReadyActiveJobsLocked();
        this.mJobs.forEachJob(this.mReadyQueueFunctor);
        this.mReadyQueueFunctor.postProcess();
        if (DEBUG) {
            int size = this.mPendingJobs.size();
            if (size == 0) {
                Slog.d(TAG, "No jobs pending.");
                return;
            }
            Slog.d(TAG, size + " jobs queued.");
        }
    }

    final class ReadyJobQueueFunctor implements Consumer<JobStatus> {
        ArrayList<JobStatus> newReadyJobs;

        ReadyJobQueueFunctor() {
        }

        @Override
        public void accept(JobStatus jobStatus) {
            if (JobSchedulerService.this.isReadyToBeExecutedLocked(jobStatus)) {
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "    queued " + jobStatus.toShortString());
                }
                if (this.newReadyJobs == null) {
                    this.newReadyJobs = new ArrayList<>();
                }
                this.newReadyJobs.add(jobStatus);
            }
        }

        public void postProcess() {
            if (this.newReadyJobs != null) {
                JobSchedulerService.this.noteJobsPending(this.newReadyJobs);
                JobSchedulerService.this.mPendingJobs.addAll(this.newReadyJobs);
                if (JobSchedulerService.this.mPendingJobs.size() > 1) {
                    JobSchedulerService.this.mPendingJobs.sort(JobSchedulerService.mEnqueueTimeComparator);
                }
            }
            this.newReadyJobs = null;
        }
    }

    final class MaybeReadyJobQueueFunctor implements Consumer<JobStatus> {
        int backoffCount;
        int batteryNotLowCount;
        int chargingCount;
        int connectivityCount;
        int contentCount;
        int idleCount;
        List<JobStatus> runnableJobs;
        int storageNotLowCount;

        public MaybeReadyJobQueueFunctor() {
            reset();
        }

        @Override
        public void accept(JobStatus jobStatus) {
            if (JobSchedulerService.this.isReadyToBeExecutedLocked(jobStatus)) {
                try {
                    if (ActivityManager.getService().isAppStartModeDisabled(jobStatus.getUid(), jobStatus.getJob().getService().getPackageName())) {
                        Slog.w(JobSchedulerService.TAG, "Aborting job " + jobStatus.getUid() + ":" + jobStatus.getJob().toString() + " -- package not allowed to start");
                        JobSchedulerService.this.mHandler.obtainMessage(2, jobStatus).sendToTarget();
                        return;
                    }
                } catch (RemoteException e) {
                }
                if (jobStatus.getNumFailures() > 0) {
                    this.backoffCount++;
                }
                if (jobStatus.hasIdleConstraint()) {
                    this.idleCount++;
                }
                if (jobStatus.hasConnectivityConstraint()) {
                    this.connectivityCount++;
                }
                if (jobStatus.hasChargingConstraint()) {
                    this.chargingCount++;
                }
                if (jobStatus.hasBatteryNotLowConstraint()) {
                    this.batteryNotLowCount++;
                }
                if (jobStatus.hasStorageNotLowConstraint()) {
                    this.storageNotLowCount++;
                }
                if (jobStatus.hasContentTriggerConstraint()) {
                    this.contentCount++;
                }
                if (this.runnableJobs == null) {
                    this.runnableJobs = new ArrayList();
                }
                this.runnableJobs.add(jobStatus);
            }
        }

        public void postProcess() {
            if (this.backoffCount > 0 || this.idleCount >= JobSchedulerService.this.mConstants.MIN_IDLE_COUNT || this.connectivityCount >= JobSchedulerService.this.mConstants.MIN_CONNECTIVITY_COUNT || this.chargingCount >= JobSchedulerService.this.mConstants.MIN_CHARGING_COUNT || this.batteryNotLowCount >= JobSchedulerService.this.mConstants.MIN_BATTERY_NOT_LOW_COUNT || this.storageNotLowCount >= JobSchedulerService.this.mConstants.MIN_STORAGE_NOT_LOW_COUNT || this.contentCount >= JobSchedulerService.this.mConstants.MIN_CONTENT_COUNT || (this.runnableJobs != null && this.runnableJobs.size() >= JobSchedulerService.this.mConstants.MIN_READY_JOBS_COUNT)) {
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "maybeQueueReadyJobsForExecutionLocked: Running jobs.");
                }
                JobSchedulerService.this.noteJobsPending(this.runnableJobs);
                JobSchedulerService.this.mPendingJobs.addAll(this.runnableJobs);
                if (JobSchedulerService.this.mPendingJobs.size() > 1) {
                    JobSchedulerService.this.mPendingJobs.sort(JobSchedulerService.mEnqueueTimeComparator);
                }
            } else if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "maybeQueueReadyJobsForExecutionLocked: Not running anything.");
            }
            reset();
        }

        private void reset() {
            this.chargingCount = 0;
            this.idleCount = 0;
            this.backoffCount = 0;
            this.connectivityCount = 0;
            this.batteryNotLowCount = 0;
            this.storageNotLowCount = 0;
            this.contentCount = 0;
            this.runnableJobs = null;
        }
    }

    private void maybeQueueReadyJobsForExecutionLocked() {
        if (DEBUG) {
            Slog.d(TAG, "Maybe queuing ready jobs...");
        }
        noteJobsNonpending(this.mPendingJobs);
        this.mPendingJobs.clear();
        stopNonReadyActiveJobsLocked();
        this.mJobs.forEachJob(this.mMaybeQueueFunctor);
        this.mMaybeQueueFunctor.postProcess();
    }

    class HeartbeatAlarmListener implements AlarmManager.OnAlarmListener {
        HeartbeatAlarmListener() {
        }

        @Override
        public void onAlarm() {
            synchronized (JobSchedulerService.this.mLock) {
                long jMillis = (JobSchedulerService.sElapsedRealtimeClock.millis() - JobSchedulerService.this.mLastHeartbeatTime) / JobSchedulerService.this.mConstants.STANDBY_HEARTBEAT_TIME;
                if (jMillis > 0) {
                    JobSchedulerService.this.mLastHeartbeatTime += JobSchedulerService.this.mConstants.STANDBY_HEARTBEAT_TIME * jMillis;
                    JobSchedulerService.this.advanceHeartbeatLocked(jMillis);
                }
            }
            JobSchedulerService.this.setNextHeartbeatAlarm();
        }
    }

    void advanceHeartbeatLocked(long j) {
        this.mHeartbeat += j;
        if (DEBUG_STANDBY) {
            Slog.v(TAG, "Advancing standby heartbeat by " + j + " to " + this.mHeartbeat);
        }
        boolean z = false;
        for (int i = 1; i < this.mNextBucketHeartbeat.length - 1; i++) {
            if (this.mHeartbeat >= this.mNextBucketHeartbeat[i]) {
                z = true;
            }
            while (this.mHeartbeat > this.mNextBucketHeartbeat[i]) {
                long[] jArr = this.mNextBucketHeartbeat;
                jArr[i] = jArr[i] + ((long) this.mConstants.STANDBY_BEATS[i]);
            }
            if (DEBUG_STANDBY) {
                Slog.v(TAG, "   Bucket " + i + " next heartbeat " + this.mNextBucketHeartbeat[i]);
            }
        }
        if (z) {
            if (DEBUG_STANDBY) {
                Slog.v(TAG, "Hit bucket boundary; reevaluating job runnability");
            }
            this.mHandler.obtainMessage(1).sendToTarget();
        }
    }

    void setNextHeartbeatAlarm() {
        long j;
        synchronized (this.mLock) {
            j = this.mConstants.STANDBY_HEARTBEAT_TIME;
        }
        long jMillis = sElapsedRealtimeClock.millis();
        long j2 = ((jMillis + j) / j) * j;
        if (DEBUG_STANDBY) {
            Slog.i(TAG, "Setting heartbeat alarm for " + j2 + " = " + TimeUtils.formatDuration(j2 - jMillis));
        }
        ((AlarmManager) getContext().getSystemService("alarm")).setExact(3, j2, HEARTBEAT_TAG, this.mHeartbeatAlarm, this.mHandler);
    }

    private boolean isReadyToBeExecutedLocked(JobStatus jobStatus) {
        boolean zIsReady = jobStatus.isReady();
        if (DEBUG) {
            Slog.v(TAG, "isReadyToBeExecutedLocked: " + jobStatus.toShortString() + " ready=" + zIsReady);
        }
        if (!zIsReady) {
            if (jobStatus.getSourcePackageName().equals("android.jobscheduler.cts.jobtestapp")) {
                Slog.v(TAG, "    NOT READY: " + jobStatus);
            }
            return false;
        }
        boolean zContainsJob = this.mJobs.containsJob(jobStatus);
        int userId = jobStatus.getUserId();
        boolean zContains = ArrayUtils.contains(this.mStartedUsers, userId);
        if (DEBUG) {
            Slog.v(TAG, "isReadyToBeExecutedLocked: " + jobStatus.toShortString() + " exists=" + zContainsJob + " userStarted=" + zContains);
        }
        if (!zContainsJob || !zContains) {
            return false;
        }
        boolean zContains2 = this.mPendingJobs.contains(jobStatus);
        boolean zIsCurrentlyActiveLocked = isCurrentlyActiveLocked(jobStatus);
        if (DEBUG) {
            Slog.v(TAG, "isReadyToBeExecutedLocked: " + jobStatus.toShortString() + " pending=" + zContains2 + " active=" + zIsCurrentlyActiveLocked);
        }
        if (zContains2 || zIsCurrentlyActiveLocked) {
            return false;
        }
        if (DEBUG_STANDBY) {
            Slog.v(TAG, "isReadyToBeExecutedLocked: " + jobStatus.toShortString() + " parole=" + this.mInParole + " active=" + jobStatus.uidActive + " exempt=" + jobStatus.getJob().isExemptedFromAppStandby());
        }
        if (!this.mInParole && !jobStatus.uidActive && !jobStatus.getJob().isExemptedFromAppStandby()) {
            int standbyBucket = jobStatus.getStandbyBucket();
            if (DEBUG_STANDBY) {
                Slog.v(TAG, "  bucket=" + standbyBucket + " heartbeat=" + this.mHeartbeat + " next=" + this.mNextBucketHeartbeat[standbyBucket]);
            }
            if (this.mHeartbeat < this.mNextBucketHeartbeat[standbyBucket]) {
                long jHeartbeatWhenJobsLastRun = heartbeatWhenJobsLastRun(jobStatus);
                if (standbyBucket >= this.mConstants.STANDBY_BEATS.length || (this.mHeartbeat > jHeartbeatWhenJobsLastRun && this.mHeartbeat < ((long) this.mConstants.STANDBY_BEATS[standbyBucket]) + jHeartbeatWhenJobsLastRun)) {
                    if (jobStatus.getWhenStandbyDeferred() == 0) {
                        if (DEBUG_STANDBY) {
                            Slog.v(TAG, "Bucket deferral: " + this.mHeartbeat + " < " + (jHeartbeatWhenJobsLastRun + ((long) this.mConstants.STANDBY_BEATS[standbyBucket])) + " for " + jobStatus);
                        }
                        jobStatus.setWhenStandbyDeferred(sElapsedRealtimeClock.millis());
                    }
                    return false;
                }
                if (DEBUG_STANDBY) {
                    Slog.v(TAG, "Bucket deferred job aged into runnability at " + this.mHeartbeat + " : " + jobStatus);
                }
            }
        }
        try {
            boolean z = AppGlobals.getPackageManager().getServiceInfo(jobStatus.getServiceComponent(), 268435456, userId) != null;
            if (DEBUG) {
                Slog.v(TAG, "isReadyToBeExecutedLocked: " + jobStatus.toShortString() + " componentPresent=" + z);
            }
            return z;
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    private void maybeRunPendingJobsLocked() {
        if (DEBUG) {
            Slog.d(TAG, "pending queue: " + this.mPendingJobs.size() + " jobs.");
        }
        assignJobsToContextsLocked();
        reportActiveLocked();
    }

    private int adjustJobPriority(int i, JobStatus jobStatus) {
        if (i < 40) {
            float loadFactor = this.mJobPackageTracker.getLoadFactor(jobStatus);
            if (loadFactor >= this.mConstants.HEAVY_USE_FACTOR) {
                return i - 80;
            }
            if (loadFactor >= this.mConstants.MODERATE_USE_FACTOR) {
                return i - 40;
            }
            return i;
        }
        return i;
    }

    private int evaluateJobPriorityLocked(JobStatus jobStatus) {
        int priority = jobStatus.getPriority();
        if (priority >= 30) {
            return adjustJobPriority(priority, jobStatus);
        }
        int i = this.mUidPriorityOverride.get(jobStatus.getSourceUid(), 0);
        if (i != 0) {
            return adjustJobPriority(i, jobStatus);
        }
        return adjustJobPriority(priority, jobStatus);
    }

    private void assignJobsToContextsLocked() {
        int memoryTrimLevel;
        boolean z;
        if (DEBUG) {
            Slog.d(TAG, printPendingQueue());
        }
        int i = 0;
        try {
            memoryTrimLevel = ActivityManager.getService().getMemoryTrimLevel();
        } catch (RemoteException e) {
            memoryTrimLevel = 0;
        }
        switch (memoryTrimLevel) {
            case 1:
                this.mMaxActiveJobs = this.mConstants.BG_MODERATE_JOB_COUNT;
                break;
            case 2:
                this.mMaxActiveJobs = this.mConstants.BG_LOW_JOB_COUNT;
                break;
            case 3:
                this.mMaxActiveJobs = this.mConstants.BG_CRITICAL_JOB_COUNT;
                break;
            default:
                this.mMaxActiveJobs = this.mConstants.BG_NORMAL_JOB_COUNT;
                break;
        }
        JobStatus[] jobStatusArr = this.mTmpAssignContextIdToJobMap;
        boolean[] zArr = this.mTmpAssignAct;
        int[] iArr = this.mTmpAssignPreferredUidForContext;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (true) {
            int i5 = 40;
            int i6 = 16;
            if (i2 < 16) {
                JobServiceContext jobServiceContext = this.mActiveServices.get(i2);
                JobStatus runningJobLocked = jobServiceContext.getRunningJobLocked();
                jobStatusArr[i2] = runningJobLocked;
                if (runningJobLocked != null) {
                    i3++;
                    if (runningJobLocked.lastEvaluatedPriority >= 40) {
                        i4++;
                    }
                }
                zArr[i2] = false;
                iArr[i2] = jobServiceContext.getPreferredUid();
                i2++;
            } else {
                if (DEBUG) {
                    Slog.d(TAG, printContextIdToJobMap(jobStatusArr, "running jobs initial"));
                }
                int i7 = 0;
                while (i7 < this.mPendingJobs.size()) {
                    JobStatus jobStatus = this.mPendingJobs.get(i7);
                    if (findJobContextIdFromMap(jobStatus, jobStatusArr) == -1) {
                        int iEvaluateJobPriorityLocked = evaluateJobPriorityLocked(jobStatus);
                        jobStatus.lastEvaluatedPriority = iEvaluateJobPriorityLocked;
                        int i8 = -1;
                        int i9 = i;
                        int i10 = Integer.MAX_VALUE;
                        while (true) {
                            if (i9 < i6) {
                                JobStatus jobStatus2 = jobStatusArr[i9];
                                int i11 = iArr[i9];
                                if (jobStatus2 == null) {
                                    if ((i3 < this.mMaxActiveJobs || (iEvaluateJobPriorityLocked >= i5 && i4 < this.mConstants.FG_JOB_COUNT)) && (i11 == jobStatus.getUid() || i11 == -1)) {
                                    }
                                } else if (jobStatus2.getUid() == jobStatus.getUid() && evaluateJobPriorityLocked(jobStatus2) < jobStatus.lastEvaluatedPriority && i10 > jobStatus.lastEvaluatedPriority) {
                                    i10 = jobStatus.lastEvaluatedPriority;
                                    i8 = i9;
                                }
                                i9++;
                                i5 = 40;
                                i6 = 16;
                            } else {
                                i9 = i8;
                            }
                        }
                        if (i9 == -1) {
                            i5 = 40;
                        } else {
                            jobStatusArr[i9] = jobStatus;
                            zArr[i9] = true;
                            i3++;
                            i5 = 40;
                            if (iEvaluateJobPriorityLocked >= 40) {
                                i4++;
                            }
                        }
                    }
                    i7++;
                    i = 0;
                    i6 = 16;
                }
                if (DEBUG) {
                    Slog.d(TAG, printContextIdToJobMap(jobStatusArr, "running jobs final"));
                }
                this.mJobPackageTracker.noteConcurrency(i3, i4);
                for (int i12 = 0; i12 < 16; i12++) {
                    if (zArr[i12]) {
                        if (this.mActiveServices.get(i12).getRunningJobLocked() != null) {
                            if (DEBUG) {
                                Slog.d(TAG, "preempting job: " + this.mActiveServices.get(i12).getRunningJobLocked());
                            }
                            this.mActiveServices.get(i12).preemptExecutingJobLocked();
                            z = true;
                        } else {
                            JobStatus jobStatus3 = jobStatusArr[i12];
                            if (DEBUG) {
                                Slog.d(TAG, "About to run job on context " + String.valueOf(i12) + ", job: " + jobStatus3);
                            }
                            for (int i13 = 0; i13 < this.mControllers.size(); i13++) {
                                this.mControllers.get(i13).prepareForExecutionLocked(jobStatus3);
                            }
                            if (!this.mActiveServices.get(i12).executeRunnableJob(jobStatus3)) {
                                Slog.d(TAG, "Error executing " + jobStatus3);
                            }
                            if (this.mPendingJobs.remove(jobStatus3)) {
                                this.mJobPackageTracker.noteNonpending(jobStatus3);
                            }
                            z = false;
                        }
                    } else {
                        z = false;
                    }
                    if (!z) {
                        this.mActiveServices.get(i12).clearPreferredUid();
                    }
                }
                return;
            }
        }
    }

    int findJobContextIdFromMap(JobStatus jobStatus, JobStatus[] jobStatusArr) {
        for (int i = 0; i < jobStatusArr.length; i++) {
            if (jobStatusArr[i] != null && jobStatusArr[i].matches(jobStatus.getUid(), jobStatus.getJobId())) {
                return i;
            }
        }
        return -1;
    }

    final class LocalService implements JobSchedulerInternal {
        LocalService() {
        }

        @Override
        public long currentHeartbeat() {
            return JobSchedulerService.this.getCurrentHeartbeat();
        }

        @Override
        public long nextHeartbeatForBucket(int i) {
            long j;
            synchronized (JobSchedulerService.this.mLock) {
                j = JobSchedulerService.this.mNextBucketHeartbeat[i];
            }
            return j;
        }

        @Override
        public long baseHeartbeatForApp(String str, int i, int i2) {
            if (i2 == 0 || i2 >= JobSchedulerService.this.mConstants.STANDBY_BEATS.length) {
                if (JobSchedulerService.DEBUG_STANDBY) {
                    Slog.v(JobSchedulerService.TAG, "Base heartbeat forced ZERO for new job in " + str + SliceClientPermissions.SliceAuthority.DELIMITER + i);
                    return 0L;
                }
                return 0L;
            }
            long jHeartbeatWhenJobsLastRun = JobSchedulerService.this.heartbeatWhenJobsLastRun(str, i);
            if (JobSchedulerService.DEBUG_STANDBY) {
                Slog.v(JobSchedulerService.TAG, "Base heartbeat " + jHeartbeatWhenJobsLastRun + " for new job in " + str + SliceClientPermissions.SliceAuthority.DELIMITER + i);
            }
            return jHeartbeatWhenJobsLastRun;
        }

        @Override
        public void noteJobStart(String str, int i) {
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.setLastJobHeartbeatLocked(str, i, JobSchedulerService.this.mHeartbeat);
            }
        }

        @Override
        public List<JobInfo> getSystemScheduledPendingJobs() {
            final ArrayList arrayList;
            synchronized (JobSchedulerService.this.mLock) {
                arrayList = new ArrayList();
                JobSchedulerService.this.mJobs.forEachJob(1000, new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        JobSchedulerService.LocalService.lambda$getSystemScheduledPendingJobs$0(this.f$0, arrayList, (JobStatus) obj);
                    }
                });
            }
            return arrayList;
        }

        public static void lambda$getSystemScheduledPendingJobs$0(LocalService localService, List list, JobStatus jobStatus) {
            if (jobStatus.getJob().isPeriodic() || !JobSchedulerService.this.isCurrentlyActiveLocked(jobStatus)) {
                list.add(jobStatus.getJob());
            }
        }

        @Override
        public void cancelJobsForUid(int i, String str) {
            JobSchedulerService.this.cancelJobsForUid(i, str);
        }

        @Override
        public void addBackingUpUid(int i) {
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mBackingUpUids.put(i, i);
            }
        }

        @Override
        public void removeBackingUpUid(int i) {
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mBackingUpUids.delete(i);
                if (JobSchedulerService.this.mJobs.countJobsForUid(i) > 0) {
                    JobSchedulerService.this.mHandler.obtainMessage(1).sendToTarget();
                }
            }
        }

        @Override
        public void clearAllBackingUpUids() {
            synchronized (JobSchedulerService.this.mLock) {
                if (JobSchedulerService.this.mBackingUpUids.size() > 0) {
                    JobSchedulerService.this.mBackingUpUids.clear();
                    JobSchedulerService.this.mHandler.obtainMessage(1).sendToTarget();
                }
            }
        }

        @Override
        public void reportAppUsage(String str, int i) {
            JobSchedulerService.this.reportAppUsage(str, i);
        }

        @Override
        public JobSchedulerInternal.JobStorePersistStats getPersistStats() {
            JobSchedulerInternal.JobStorePersistStats jobStorePersistStats;
            synchronized (JobSchedulerService.this.mLock) {
                jobStorePersistStats = new JobSchedulerInternal.JobStorePersistStats(JobSchedulerService.this.mJobs.getPersistStats());
            }
            return jobStorePersistStats;
        }
    }

    final class StandbyTracker extends UsageStatsManagerInternal.AppIdleStateChangeListener {
        StandbyTracker() {
        }

        public void onAppIdleStateChanged(final String str, int i, boolean z, int i2, int i3) {
            final int packageUid = JobSchedulerService.this.mLocalPM.getPackageUid(str, 8192, i);
            if (packageUid < 0) {
                if (JobSchedulerService.DEBUG_STANDBY) {
                    Slog.i(JobSchedulerService.TAG, "App idle state change for unknown app " + str + SliceClientPermissions.SliceAuthority.DELIMITER + i);
                    return;
                }
                return;
            }
            final int iStandbyBucketToBucketIndex = JobSchedulerService.standbyBucketToBucketIndex(i2);
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    JobSchedulerService.StandbyTracker.lambda$onAppIdleStateChanged$1(this.f$0, packageUid, iStandbyBucketToBucketIndex, str);
                }
            });
        }

        public static void lambda$onAppIdleStateChanged$1(StandbyTracker standbyTracker, int i, final int i2, final String str) {
            if (JobSchedulerService.DEBUG_STANDBY) {
                Slog.i(JobSchedulerService.TAG, "Moving uid " + i + " to bucketIndex " + i2);
            }
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mJobs.forEachJobForSourceUid(i, new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        JobSchedulerService.StandbyTracker.lambda$onAppIdleStateChanged$0(str, i2, (JobStatus) obj);
                    }
                });
                JobSchedulerService.this.onControllerStateChanged();
            }
        }

        static void lambda$onAppIdleStateChanged$0(String str, int i, JobStatus jobStatus) {
            if (str.equals(jobStatus.getSourcePackageName())) {
                jobStatus.setStandbyBucket(i);
            }
        }

        public void onParoleStateChanged(boolean z) {
            if (JobSchedulerService.DEBUG_STANDBY) {
                StringBuilder sb = new StringBuilder();
                sb.append("Global parole state now ");
                sb.append(z ? "ON" : "OFF");
                Slog.i(JobSchedulerService.TAG, sb.toString());
            }
            JobSchedulerService.this.mInParole = z;
        }

        public void onUserInteractionStarted(String str, int i) {
            int packageUid = JobSchedulerService.this.mLocalPM.getPackageUid(str, 8192, i);
            if (packageUid < 0) {
                return;
            }
            long timeSinceLastJobRun = JobSchedulerService.this.mUsageStats.getTimeSinceLastJobRun(str, i);
            if (timeSinceLastJobRun > 172800000) {
                timeSinceLastJobRun = 0;
            }
            DeferredJobCounter deferredJobCounter = new DeferredJobCounter();
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mJobs.forEachJobForSourceUid(packageUid, deferredJobCounter);
            }
            if (deferredJobCounter.numDeferred() > 0 || timeSinceLastJobRun > 0) {
                ((BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class)).noteJobsDeferred(packageUid, deferredJobCounter.numDeferred(), timeSinceLastJobRun);
            }
        }
    }

    static class DeferredJobCounter implements Consumer<JobStatus> {
        private int mDeferred = 0;

        DeferredJobCounter() {
        }

        public int numDeferred() {
            return this.mDeferred;
        }

        @Override
        public void accept(JobStatus jobStatus) {
            if (jobStatus.getWhenStandbyDeferred() > 0) {
                this.mDeferred++;
            }
        }
    }

    public static int standbyBucketToBucketIndex(int i) {
        if (i == 50) {
            return 4;
        }
        if (i > 30) {
            return 3;
        }
        if (i > 20) {
            return 2;
        }
        return i > 10 ? 1 : 0;
    }

    public static int standbyBucketForPackage(String str, int i, long j) {
        int appStandbyBucket;
        UsageStatsManagerInternal usageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        if (usageStatsManagerInternal != null) {
            appStandbyBucket = usageStatsManagerInternal.getAppStandbyBucket(str, i, j);
        } else {
            appStandbyBucket = 0;
        }
        int iStandbyBucketToBucketIndex = standbyBucketToBucketIndex(appStandbyBucket);
        if (DEBUG_STANDBY) {
            Slog.v(TAG, str + SliceClientPermissions.SliceAuthority.DELIMITER + i + " standby bucket index: " + iStandbyBucketToBucketIndex);
        }
        return iStandbyBucketToBucketIndex;
    }

    final class JobSchedulerStub extends IJobScheduler.Stub {
        private final SparseArray<Boolean> mPersistCache = new SparseArray<>();

        JobSchedulerStub() {
        }

        private void enforceValidJobRequest(int i, JobInfo jobInfo) {
            IPackageManager packageManager = AppGlobals.getPackageManager();
            ComponentName service = jobInfo.getService();
            try {
                ServiceInfo serviceInfo = packageManager.getServiceInfo(service, 786432, UserHandle.getUserId(i));
                if (serviceInfo == null) {
                    throw new IllegalArgumentException("No such service " + service);
                }
                if (serviceInfo.applicationInfo.uid != i) {
                    throw new IllegalArgumentException("uid " + i + " cannot schedule job in " + service.getPackageName());
                }
                if (!"android.permission.BIND_JOB_SERVICE".equals(serviceInfo.permission)) {
                    throw new IllegalArgumentException("Scheduled service " + service + " does not require android.permission.BIND_JOB_SERVICE permission");
                }
            } catch (RemoteException e) {
            }
        }

        private boolean canPersistJobs(int i, int i2) {
            boolean zBooleanValue;
            synchronized (this.mPersistCache) {
                Boolean bool = this.mPersistCache.get(i2);
                if (bool != null) {
                    zBooleanValue = bool.booleanValue();
                } else {
                    zBooleanValue = JobSchedulerService.this.getContext().checkPermission("android.permission.RECEIVE_BOOT_COMPLETED", i, i2) == 0;
                    this.mPersistCache.put(i2, Boolean.valueOf(zBooleanValue));
                }
            }
            return zBooleanValue;
        }

        private void validateJobFlags(JobInfo jobInfo, int i) {
            if ((jobInfo.getFlags() & 1) != 0) {
                JobSchedulerService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", JobSchedulerService.TAG);
            }
            if ((jobInfo.getFlags() & 8) != 0) {
                if (i != 1000) {
                    throw new SecurityException("Job has invalid flags");
                }
                if (jobInfo.isPeriodic()) {
                    Slog.wtf(JobSchedulerService.TAG, "Periodic jobs mustn't have FLAG_EXEMPT_FROM_APP_STANDBY. Job=" + jobInfo);
                }
            }
        }

        public int schedule(JobInfo jobInfo) throws RemoteException {
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Scheduling job: " + jobInfo.toString());
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            enforceValidJobRequest(callingUid, jobInfo);
            if (jobInfo.isPersisted() && !canPersistJobs(callingPid, callingUid)) {
                throw new IllegalArgumentException("Error: requested job be persisted without holding RECEIVE_BOOT_COMPLETED permission.");
            }
            validateJobFlags(jobInfo, callingUid);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.scheduleAsPackage(jobInfo, null, callingUid, null, userId, null);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int enqueue(JobInfo jobInfo, JobWorkItem jobWorkItem) throws RemoteException {
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Enqueueing job: " + jobInfo.toString() + " work: " + jobWorkItem);
            }
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            enforceValidJobRequest(callingUid, jobInfo);
            if (jobInfo.isPersisted()) {
                throw new IllegalArgumentException("Can't enqueue work for persisted jobs");
            }
            if (jobWorkItem == null) {
                throw new NullPointerException("work is null");
            }
            validateJobFlags(jobInfo, callingUid);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.scheduleAsPackage(jobInfo, jobWorkItem, callingUid, null, userId, null);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int scheduleAsPackage(JobInfo jobInfo, String str, int i, String str2) throws RemoteException {
            int callingUid = Binder.getCallingUid();
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Caller uid " + callingUid + " scheduling job: " + jobInfo.toString() + " on behalf of " + str + SliceClientPermissions.SliceAuthority.DELIMITER);
            }
            if (str == null) {
                throw new NullPointerException("Must specify a package for scheduleAsPackage()");
            }
            if (JobSchedulerService.this.getContext().checkCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS") != 0) {
                throw new SecurityException("Caller uid " + callingUid + " not permitted to schedule jobs for other apps");
            }
            validateJobFlags(jobInfo, callingUid);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.scheduleAsPackage(jobInfo, null, callingUid, str, i, str2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<JobInfo> getAllPendingJobs() throws RemoteException {
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.getPendingJobs(callingUid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public JobInfo getPendingJob(int i) throws RemoteException {
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.getPendingJob(callingUid, i);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void cancelAll() throws RemoteException {
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.cancelJobsForUid(callingUid, "cancelAll() called by app, callingUid=" + callingUid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void cancel(int i) throws RemoteException {
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.cancelJob(callingUid, i, callingUid);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            boolean z;
            if (DumpUtils.checkDumpAndUsageStatsPermission(JobSchedulerService.this.getContext(), JobSchedulerService.TAG, printWriter)) {
                int packageUid = -1;
                if (!ArrayUtils.isEmpty(strArr)) {
                    int i = 0;
                    z = false;
                    while (true) {
                        if (i >= strArr.length) {
                            break;
                        }
                        String str = strArr[i];
                        if ("-h".equals(str)) {
                            JobSchedulerService.dumpHelp(printWriter);
                            return;
                        }
                        if (!"-a".equals(str)) {
                            if (!PriorityDump.PROTO_ARG.equals(str)) {
                                if (str.length() > 0 && str.charAt(0) == '-') {
                                    printWriter.println("Unknown option: " + str);
                                    return;
                                }
                            } else {
                                z = true;
                            }
                        }
                        i++;
                    }
                } else {
                    z = false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (z) {
                        JobSchedulerService.this.dumpInternalProto(fileDescriptor, packageUid);
                    } else {
                        JobSchedulerService.this.dumpInternal(new IndentingPrintWriter(printWriter, "  "), packageUid);
                    }
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        }

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
            new JobSchedulerShellCommand(JobSchedulerService.this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }
    }

    int executeRunCommand(String str, int i, int i2, boolean z) {
        if (DEBUG) {
            Slog.v(TAG, "executeRunCommand(): " + str + SliceClientPermissions.SliceAuthority.DELIMITER + i + " " + i2 + " f=" + z);
        }
        try {
            IPackageManager packageManager = AppGlobals.getPackageManager();
            if (i == -1) {
                i = 0;
            }
            int packageUid = packageManager.getPackageUid(str, 0, i);
            if (packageUid < 0) {
                return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            synchronized (this.mLock) {
                JobStatus jobByUidAndJobId = this.mJobs.getJobByUidAndJobId(packageUid, i2);
                if (jobByUidAndJobId == null) {
                    return JobSchedulerShellCommand.CMD_ERR_NO_JOB;
                }
                jobByUidAndJobId.overrideState = z ? 2 : 1;
                if (!jobByUidAndJobId.isConstraintsSatisfied()) {
                    jobByUidAndJobId.overrideState = 0;
                    return JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS;
                }
                queueReadyJobsForExecutionLocked();
                maybeRunPendingJobsLocked();
            }
        } catch (RemoteException e) {
        }
        return 0;
    }

    int executeTimeoutCommand(PrintWriter printWriter, String str, int i, boolean z, int i2) {
        String str2;
        int i3;
        int i4;
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("executeTimeoutCommand(): ");
            str2 = str;
            sb.append(str2);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            i3 = i;
            sb.append(i3);
            sb.append(" ");
            i4 = i2;
            sb.append(i4);
            Slog.v(TAG, sb.toString());
        } else {
            str2 = str;
            i3 = i;
            i4 = i2;
        }
        synchronized (this.mLock) {
            boolean z2 = false;
            for (int i5 = 0; i5 < this.mActiveServices.size(); i5++) {
                JobServiceContext jobServiceContext = this.mActiveServices.get(i5);
                JobStatus runningJobLocked = jobServiceContext.getRunningJobLocked();
                if (jobServiceContext.timeoutIfExecutingLocked(str2, i3, z, i4, "shell")) {
                    printWriter.print("Timing out: ");
                    runningJobLocked.printUniqueId(printWriter);
                    printWriter.print(" ");
                    printWriter.println(runningJobLocked.getServiceComponent().flattenToShortString());
                    z2 = true;
                }
            }
            if (!z2) {
                printWriter.println("No matching executing jobs found.");
            }
        }
        return 0;
    }

    int executeCancelCommand(PrintWriter printWriter, String str, int i, boolean z, int i2) {
        if (DEBUG) {
            Slog.v(TAG, "executeCancelCommand(): " + str + SliceClientPermissions.SliceAuthority.DELIMITER + i + " " + i2);
        }
        int packageUid = -1;
        try {
            packageUid = AppGlobals.getPackageManager().getPackageUid(str, 0, i);
        } catch (RemoteException e) {
        }
        if (packageUid < 0) {
            printWriter.println("Package " + str + " not found.");
            return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        }
        if (!z) {
            printWriter.println("Canceling all jobs for " + str + " in user " + i);
            if (!cancelJobsForUid(packageUid, "cancel shell command for package")) {
                printWriter.println("No matching jobs found.");
            }
        } else {
            printWriter.println("Canceling job " + str + "/#" + i2 + " in user " + i);
            if (!cancelJob(packageUid, i2, PowerHalManager.ROTATE_BOOST_TIME)) {
                printWriter.println("No matching job found.");
            }
        }
        return 0;
    }

    void setMonitorBattery(boolean z) {
        synchronized (this.mLock) {
            if (this.mBatteryController != null) {
                this.mBatteryController.getTracker().setMonitorBatteryLocked(z);
            }
        }
    }

    int getBatterySeq() {
        int seq;
        synchronized (this.mLock) {
            seq = this.mBatteryController != null ? this.mBatteryController.getTracker().getSeq() : -1;
        }
        return seq;
    }

    boolean getBatteryCharging() {
        boolean zIsOnStablePower;
        synchronized (this.mLock) {
            zIsOnStablePower = this.mBatteryController != null ? this.mBatteryController.getTracker().isOnStablePower() : false;
        }
        return zIsOnStablePower;
    }

    boolean getBatteryNotLow() {
        boolean zIsBatteryNotLow;
        synchronized (this.mLock) {
            zIsBatteryNotLow = this.mBatteryController != null ? this.mBatteryController.getTracker().isBatteryNotLow() : false;
        }
        return zIsBatteryNotLow;
    }

    int getStorageSeq() {
        int seq;
        synchronized (this.mLock) {
            seq = this.mStorageController != null ? this.mStorageController.getTracker().getSeq() : -1;
        }
        return seq;
    }

    boolean getStorageNotLow() {
        boolean zIsStorageNotLow;
        synchronized (this.mLock) {
            zIsStorageNotLow = this.mStorageController != null ? this.mStorageController.getTracker().isStorageNotLow() : false;
        }
        return zIsStorageNotLow;
    }

    long getCurrentHeartbeat() {
        long j;
        synchronized (this.mLock) {
            j = this.mHeartbeat;
        }
        return j;
    }

    int getJobState(PrintWriter printWriter, String str, int i, int i2) {
        boolean z;
        boolean z2;
        try {
            IPackageManager packageManager = AppGlobals.getPackageManager();
            if (i == -1) {
                i = 0;
            }
            int packageUid = packageManager.getPackageUid(str, 0, i);
            if (packageUid < 0) {
                printWriter.print("unknown(");
                printWriter.print(str);
                printWriter.println(")");
                return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            synchronized (this.mLock) {
                JobStatus jobByUidAndJobId = this.mJobs.getJobByUidAndJobId(packageUid, i2);
                if (DEBUG) {
                    Slog.d(TAG, "get-job-state " + packageUid + SliceClientPermissions.SliceAuthority.DELIMITER + i2 + ": " + jobByUidAndJobId);
                }
                if (jobByUidAndJobId == null) {
                    printWriter.print("unknown(");
                    UserHandle.formatUid(printWriter, packageUid);
                    printWriter.print("/jid");
                    printWriter.print(i2);
                    printWriter.println(")");
                    return JobSchedulerShellCommand.CMD_ERR_NO_JOB;
                }
                if (!this.mPendingJobs.contains(jobByUidAndJobId)) {
                    z = false;
                } else {
                    printWriter.print("pending");
                    z = true;
                }
                if (isCurrentlyActiveLocked(jobByUidAndJobId)) {
                    if (z) {
                        printWriter.print(" ");
                    }
                    printWriter.println("active");
                    z = true;
                }
                if (!ArrayUtils.contains(this.mStartedUsers, jobByUidAndJobId.getUserId())) {
                    if (z) {
                        printWriter.print(" ");
                    }
                    printWriter.println("user-stopped");
                    z = true;
                }
                if (this.mBackingUpUids.indexOfKey(jobByUidAndJobId.getSourceUid()) >= 0) {
                    if (z) {
                        printWriter.print(" ");
                    }
                    printWriter.println("backing-up");
                    z = true;
                }
                try {
                    z2 = AppGlobals.getPackageManager().getServiceInfo(jobByUidAndJobId.getServiceComponent(), 268435456, jobByUidAndJobId.getUserId()) != null;
                } catch (RemoteException e) {
                    z2 = false;
                }
                if (!z2) {
                    if (z) {
                        printWriter.print(" ");
                    }
                    printWriter.println("no-component");
                    z = true;
                }
                if (jobByUidAndJobId.isReady()) {
                    if (z) {
                        printWriter.print(" ");
                    }
                    printWriter.println("ready");
                    z = true;
                }
                if (!z) {
                    printWriter.print("waiting");
                }
                printWriter.println();
            }
        } catch (RemoteException e2) {
        }
        return 0;
    }

    int executeHeartbeatCommand(PrintWriter printWriter, int i) {
        if (i < 1) {
            printWriter.println(getCurrentHeartbeat());
            return 0;
        }
        printWriter.print("Advancing standby heartbeat by ");
        printWriter.println(i);
        synchronized (this.mLock) {
            advanceHeartbeatLocked(i);
        }
        return 0;
    }

    void triggerDockState(boolean z) {
        Intent intent;
        if (z) {
            intent = new Intent("android.intent.action.DOCK_IDLE");
        } else {
            intent = new Intent("android.intent.action.DOCK_ACTIVE");
        }
        intent.setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME);
        intent.addFlags(1342177280);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private String printContextIdToJobMap(JobStatus[] jobStatusArr, String str) {
        StringBuilder sb = new StringBuilder(str + ": ");
        for (int i = 0; i < jobStatusArr.length; i++) {
            sb.append("(");
            int uid = -1;
            sb.append(jobStatusArr[i] == null ? -1 : jobStatusArr[i].getJobId());
            if (jobStatusArr[i] != null) {
                uid = jobStatusArr[i].getUid();
            }
            sb.append(uid);
            sb.append(")");
        }
        return sb.toString();
    }

    private String printPendingQueue() {
        StringBuilder sb = new StringBuilder("Pending queue: ");
        for (JobStatus jobStatus : this.mPendingJobs) {
            sb.append("(");
            sb.append(jobStatus.getJob().getId());
            sb.append(", ");
            sb.append(jobStatus.getUid());
            sb.append(") ");
        }
        return sb.toString();
    }

    static void dumpHelp(PrintWriter printWriter) {
        printWriter.println("Job Scheduler (jobscheduler) dump options:");
        printWriter.println("  [-h] [package] ...");
        printWriter.println("    -h: print this help");
        printWriter.println("  [package] is an optional package name to limit the output to.");
    }

    private static void sortJobs(List<JobStatus> list) {
        Collections.sort(list, new Comparator<JobStatus>() {
            @Override
            public int compare(JobStatus jobStatus, JobStatus jobStatus2) {
                int uid = jobStatus.getUid();
                int uid2 = jobStatus2.getUid();
                int jobId = jobStatus.getJobId();
                int jobId2 = jobStatus2.getJobId();
                if (uid != uid2) {
                    return uid < uid2 ? -1 : 1;
                }
                if (jobId < jobId2) {
                    return -1;
                }
                return jobId > jobId2 ? 1 : 0;
            }
        });
    }

    void dumpInternal(IndentingPrintWriter indentingPrintWriter, int i) {
        boolean z;
        final int appId = UserHandle.getAppId(i);
        long jMillis = sElapsedRealtimeClock.millis();
        long jMillis2 = sUptimeMillisClock.millis();
        Predicate<JobStatus> predicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return JobSchedulerService.lambda$dumpInternal$3(appId, (JobStatus) obj);
            }
        };
        synchronized (this.mLock) {
            this.mConstants.dump(indentingPrintWriter);
            indentingPrintWriter.println();
            indentingPrintWriter.println("  Heartbeat:");
            indentingPrintWriter.print("    Current:    ");
            indentingPrintWriter.println(this.mHeartbeat);
            indentingPrintWriter.println("    Next");
            indentingPrintWriter.print("      ACTIVE:   ");
            indentingPrintWriter.println(this.mNextBucketHeartbeat[0]);
            indentingPrintWriter.print("      WORKING:  ");
            boolean z2 = true;
            indentingPrintWriter.println(this.mNextBucketHeartbeat[1]);
            indentingPrintWriter.print("      FREQUENT: ");
            indentingPrintWriter.println(this.mNextBucketHeartbeat[2]);
            indentingPrintWriter.print("      RARE:     ");
            indentingPrintWriter.println(this.mNextBucketHeartbeat[3]);
            indentingPrintWriter.print("    Last heartbeat: ");
            TimeUtils.formatDuration(this.mLastHeartbeatTime, jMillis, indentingPrintWriter);
            indentingPrintWriter.println();
            indentingPrintWriter.print("    Next heartbeat: ");
            TimeUtils.formatDuration(this.mLastHeartbeatTime + this.mConstants.STANDBY_HEARTBEAT_TIME, jMillis, indentingPrintWriter);
            indentingPrintWriter.println();
            indentingPrintWriter.print("    In parole?: ");
            indentingPrintWriter.print(this.mInParole);
            indentingPrintWriter.println();
            indentingPrintWriter.println();
            indentingPrintWriter.println("Started users: " + Arrays.toString(this.mStartedUsers));
            indentingPrintWriter.print("Registered ");
            indentingPrintWriter.print(this.mJobs.size());
            indentingPrintWriter.println(" jobs:");
            if (this.mJobs.size() > 0) {
                List<JobStatus> allJobs = this.mJobs.mJobSet.getAllJobs();
                sortJobs(allJobs);
                Iterator<JobStatus> it = allJobs.iterator();
                while (it.hasNext()) {
                    JobStatus next = it.next();
                    indentingPrintWriter.print("  JOB #");
                    next.printUniqueId(indentingPrintWriter);
                    indentingPrintWriter.print(": ");
                    indentingPrintWriter.println(next.toShortStringExceptUniqueId());
                    if (predicate.test(next)) {
                        Iterator<JobStatus> it2 = it;
                        next.dump((PrintWriter) indentingPrintWriter, "    ", true, jMillis);
                        indentingPrintWriter.print("    Last run heartbeat: ");
                        indentingPrintWriter.print(heartbeatWhenJobsLastRun(next));
                        indentingPrintWriter.println();
                        indentingPrintWriter.print("    Ready: ");
                        indentingPrintWriter.print(isReadyToBeExecutedLocked(next));
                        indentingPrintWriter.print(" (job=");
                        indentingPrintWriter.print(next.isReady());
                        indentingPrintWriter.print(" user=");
                        indentingPrintWriter.print(ArrayUtils.contains(this.mStartedUsers, next.getUserId()));
                        indentingPrintWriter.print(" !pending=");
                        indentingPrintWriter.print(!this.mPendingJobs.contains(next));
                        indentingPrintWriter.print(" !active=");
                        indentingPrintWriter.print(!isCurrentlyActiveLocked(next));
                        indentingPrintWriter.print(" !backingup=");
                        indentingPrintWriter.print(this.mBackingUpUids.indexOfKey(next.getSourceUid()) < 0);
                        indentingPrintWriter.print(" comp=");
                        try {
                            z = AppGlobals.getPackageManager().getServiceInfo(next.getServiceComponent(), 268435456, next.getUserId()) != null;
                        } catch (RemoteException e) {
                            z = false;
                        }
                        indentingPrintWriter.print(z);
                        indentingPrintWriter.println(")");
                        it = it2;
                    }
                }
            } else {
                indentingPrintWriter.println("  None.");
            }
            for (int i2 = 0; i2 < this.mControllers.size(); i2++) {
                indentingPrintWriter.println();
                indentingPrintWriter.println(this.mControllers.get(i2).getClass().getSimpleName() + ":");
                indentingPrintWriter.increaseIndent();
                this.mControllers.get(i2).dumpControllerStateLocked(indentingPrintWriter, predicate);
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.println();
            indentingPrintWriter.println("Uid priority overrides:");
            for (int i3 = 0; i3 < this.mUidPriorityOverride.size(); i3++) {
                int iKeyAt = this.mUidPriorityOverride.keyAt(i3);
                if (appId == -1 || appId == UserHandle.getAppId(iKeyAt)) {
                    indentingPrintWriter.print("  ");
                    indentingPrintWriter.print(UserHandle.formatUid(iKeyAt));
                    indentingPrintWriter.print(": ");
                    indentingPrintWriter.println(this.mUidPriorityOverride.valueAt(i3));
                }
            }
            if (this.mBackingUpUids.size() > 0) {
                indentingPrintWriter.println();
                indentingPrintWriter.println("Backing up uids:");
                for (int i4 = 0; i4 < this.mBackingUpUids.size(); i4++) {
                    int iKeyAt2 = this.mBackingUpUids.keyAt(i4);
                    if (appId == -1 || appId == UserHandle.getAppId(iKeyAt2)) {
                        if (z2) {
                            indentingPrintWriter.print("  ");
                            z2 = false;
                        } else {
                            indentingPrintWriter.print(", ");
                        }
                        indentingPrintWriter.print(UserHandle.formatUid(iKeyAt2));
                    }
                }
                indentingPrintWriter.println();
            }
            indentingPrintWriter.println();
            this.mJobPackageTracker.dump((PrintWriter) indentingPrintWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, appId);
            indentingPrintWriter.println();
            if (this.mJobPackageTracker.dumpHistory((PrintWriter) indentingPrintWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, appId)) {
                indentingPrintWriter.println();
            }
            indentingPrintWriter.println("Pending queue:");
            for (int i5 = 0; i5 < this.mPendingJobs.size(); i5++) {
                JobStatus jobStatus = this.mPendingJobs.get(i5);
                indentingPrintWriter.print("  Pending #");
                indentingPrintWriter.print(i5);
                indentingPrintWriter.print(": ");
                indentingPrintWriter.println(jobStatus.toShortString());
                jobStatus.dump((PrintWriter) indentingPrintWriter, "    ", false, jMillis);
                int iEvaluateJobPriorityLocked = evaluateJobPriorityLocked(jobStatus);
                if (iEvaluateJobPriorityLocked != 0) {
                    indentingPrintWriter.print("    Evaluated priority: ");
                    indentingPrintWriter.println(iEvaluateJobPriorityLocked);
                }
                indentingPrintWriter.print("    Tag: ");
                indentingPrintWriter.println(jobStatus.getTag());
                indentingPrintWriter.print("    Enq: ");
                TimeUtils.formatDuration(jobStatus.madePending - jMillis2, indentingPrintWriter);
                indentingPrintWriter.println();
            }
            indentingPrintWriter.println();
            indentingPrintWriter.println("Active jobs:");
            for (int i6 = 0; i6 < this.mActiveServices.size(); i6++) {
                JobServiceContext jobServiceContext = this.mActiveServices.get(i6);
                indentingPrintWriter.print("  Slot #");
                indentingPrintWriter.print(i6);
                indentingPrintWriter.print(": ");
                JobStatus runningJobLocked = jobServiceContext.getRunningJobLocked();
                if (runningJobLocked == null) {
                    if (jobServiceContext.mStoppedReason != null) {
                        indentingPrintWriter.print("inactive since ");
                        TimeUtils.formatDuration(jobServiceContext.mStoppedTime, jMillis, indentingPrintWriter);
                        indentingPrintWriter.print(", stopped because: ");
                        indentingPrintWriter.println(jobServiceContext.mStoppedReason);
                    } else {
                        indentingPrintWriter.println("inactive");
                    }
                } else {
                    indentingPrintWriter.println(runningJobLocked.toShortString());
                    indentingPrintWriter.print("    Running for: ");
                    TimeUtils.formatDuration(jMillis - jobServiceContext.getExecutionStartTimeElapsed(), indentingPrintWriter);
                    indentingPrintWriter.print(", timeout at: ");
                    TimeUtils.formatDuration(jobServiceContext.getTimeoutElapsed() - jMillis, indentingPrintWriter);
                    indentingPrintWriter.println();
                    runningJobLocked.dump((PrintWriter) indentingPrintWriter, "    ", false, jMillis);
                    int iEvaluateJobPriorityLocked2 = evaluateJobPriorityLocked(jobServiceContext.getRunningJobLocked());
                    if (iEvaluateJobPriorityLocked2 != 0) {
                        indentingPrintWriter.print("    Evaluated priority: ");
                        indentingPrintWriter.println(iEvaluateJobPriorityLocked2);
                    }
                    indentingPrintWriter.print("    Active at ");
                    TimeUtils.formatDuration(runningJobLocked.madeActive - jMillis2, indentingPrintWriter);
                    indentingPrintWriter.print(", pending for ");
                    TimeUtils.formatDuration(runningJobLocked.madeActive - runningJobLocked.madePending, indentingPrintWriter);
                    indentingPrintWriter.println();
                }
            }
            if (i == -1) {
                indentingPrintWriter.println();
                indentingPrintWriter.print("mReadyToRock=");
                indentingPrintWriter.println(this.mReadyToRock);
                indentingPrintWriter.print("mReportedActive=");
                indentingPrintWriter.println(this.mReportedActive);
                indentingPrintWriter.print("mMaxActiveJobs=");
                indentingPrintWriter.println(this.mMaxActiveJobs);
            }
            indentingPrintWriter.println();
            indentingPrintWriter.print("PersistStats: ");
            indentingPrintWriter.println(this.mJobs.getPersistStats());
        }
        indentingPrintWriter.println();
    }

    static boolean lambda$dumpInternal$3(int i, JobStatus jobStatus) {
        return i == -1 || UserHandle.getAppId(jobStatus.getUid()) == i || UserHandle.getAppId(jobStatus.getSourceUid()) == i;
    }

    void dumpInternalProto(FileDescriptor fileDescriptor, int i) throws Throwable {
        long j;
        boolean z;
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        final int appId = UserHandle.getAppId(i);
        long jMillis = sElapsedRealtimeClock.millis();
        long jMillis2 = sUptimeMillisClock.millis();
        Predicate<JobStatus> predicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return JobSchedulerService.lambda$dumpInternalProto$4(appId, (JobStatus) obj);
            }
        };
        Object obj = this.mLock;
        synchronized (obj) {
            try {
                try {
                    this.mConstants.dump(protoOutputStream, 1146756268033L);
                    protoOutputStream.write(1120986464270L, this.mHeartbeat);
                    protoOutputStream.write(2220498092047L, this.mNextBucketHeartbeat[0]);
                    protoOutputStream.write(2220498092047L, this.mNextBucketHeartbeat[1]);
                    protoOutputStream.write(2220498092047L, this.mNextBucketHeartbeat[2]);
                    protoOutputStream.write(2220498092047L, this.mNextBucketHeartbeat[3]);
                    protoOutputStream.write(1112396529680L, this.mLastHeartbeatTime - jMillis2);
                    protoOutputStream.write(1112396529681L, (this.mLastHeartbeatTime + this.mConstants.STANDBY_HEARTBEAT_TIME) - jMillis2);
                    protoOutputStream.write(1133871366162L, this.mInParole);
                    for (int i2 : this.mStartedUsers) {
                        protoOutputStream.write(2220498092034L, i2);
                    }
                    if (this.mJobs.size() > 0) {
                        List<JobStatus> allJobs = this.mJobs.mJobSet.getAllJobs();
                        sortJobs(allJobs);
                        Iterator<JobStatus> it = allJobs.iterator();
                        while (it.hasNext()) {
                            JobStatus next = it.next();
                            long jStart = protoOutputStream.start(2246267895811L);
                            next.writeToShortProto(protoOutputStream, 1146756268033L);
                            if (predicate.test(next)) {
                                long j2 = jMillis2;
                                Object obj2 = obj;
                                Iterator<JobStatus> it2 = it;
                                next.dump(protoOutputStream, 1146756268034L, true, jMillis);
                                protoOutputStream.write(1133871366147L, next.isReady());
                                protoOutputStream.write(1133871366148L, ArrayUtils.contains(this.mStartedUsers, next.getUserId()));
                                protoOutputStream.write(1133871366149L, this.mPendingJobs.contains(next));
                                protoOutputStream.write(1133871366150L, isCurrentlyActiveLocked(next));
                                protoOutputStream.write(1133871366151L, this.mBackingUpUids.indexOfKey(next.getSourceUid()) >= 0);
                                try {
                                    z = AppGlobals.getPackageManager().getServiceInfo(next.getServiceComponent(), 268435456, next.getUserId()) != null;
                                } catch (RemoteException e) {
                                    z = false;
                                }
                                protoOutputStream.write(1133871366152L, z);
                                protoOutputStream.write(1112396529673L, heartbeatWhenJobsLastRun(next));
                                protoOutputStream.end(jStart);
                                obj = obj2;
                                it = it2;
                                jMillis2 = j2;
                            }
                        }
                    }
                    Object obj3 = obj;
                    long j3 = jMillis2;
                    Iterator<StateController> it3 = this.mControllers.iterator();
                    while (it3.hasNext()) {
                        it3.next().dumpControllerStateLocked(protoOutputStream, 2246267895812L, predicate);
                    }
                    for (int i3 = 0; i3 < this.mUidPriorityOverride.size(); i3++) {
                        int iKeyAt = this.mUidPriorityOverride.keyAt(i3);
                        if (appId == -1 || appId == UserHandle.getAppId(iKeyAt)) {
                            long jStart2 = protoOutputStream.start(2246267895813L);
                            protoOutputStream.write(1120986464257L, iKeyAt);
                            protoOutputStream.write(1172526071810L, this.mUidPriorityOverride.valueAt(i3));
                            protoOutputStream.end(jStart2);
                        }
                    }
                    for (int i4 = 0; i4 < this.mBackingUpUids.size(); i4++) {
                        int iKeyAt2 = this.mBackingUpUids.keyAt(i4);
                        if (appId == -1 || appId == UserHandle.getAppId(iKeyAt2)) {
                            protoOutputStream.write(2220498092038L, iKeyAt2);
                        }
                    }
                    this.mJobPackageTracker.dump(protoOutputStream, 1146756268040L, appId);
                    this.mJobPackageTracker.dumpHistory(protoOutputStream, 1146756268039L, appId);
                    for (JobStatus jobStatus : this.mPendingJobs) {
                        long jStart3 = protoOutputStream.start(2246267895817L);
                        jobStatus.writeToShortProto(protoOutputStream, 1146756268033L);
                        jobStatus.dump(protoOutputStream, 1146756268034L, false, jMillis);
                        int iEvaluateJobPriorityLocked = evaluateJobPriorityLocked(jobStatus);
                        if (iEvaluateJobPriorityLocked != 0) {
                            protoOutputStream.write(1172526071811L, iEvaluateJobPriorityLocked);
                        }
                        protoOutputStream.write(1112396529668L, j3 - jobStatus.madePending);
                        protoOutputStream.end(jStart3);
                    }
                    for (JobServiceContext jobServiceContext : this.mActiveServices) {
                        long jStart4 = protoOutputStream.start(2246267895818L);
                        JobStatus runningJobLocked = jobServiceContext.getRunningJobLocked();
                        if (runningJobLocked == null) {
                            long jStart5 = protoOutputStream.start(1146756268033L);
                            j = jStart4;
                            protoOutputStream.write(1112396529665L, jMillis - jobServiceContext.mStoppedTime);
                            if (jobServiceContext.mStoppedReason != null) {
                                protoOutputStream.write(1138166333442L, jobServiceContext.mStoppedReason);
                            }
                            protoOutputStream.end(jStart5);
                        } else {
                            j = jStart4;
                            long jStart6 = protoOutputStream.start(1146756268034L);
                            runningJobLocked.writeToShortProto(protoOutputStream, 1146756268033L);
                            protoOutputStream.write(1112396529666L, jMillis - jobServiceContext.getExecutionStartTimeElapsed());
                            protoOutputStream.write(1112396529667L, jobServiceContext.getTimeoutElapsed() - jMillis);
                            runningJobLocked.dump(protoOutputStream, 1146756268036L, false, jMillis);
                            int iEvaluateJobPriorityLocked2 = evaluateJobPriorityLocked(jobServiceContext.getRunningJobLocked());
                            if (iEvaluateJobPriorityLocked2 != 0) {
                                protoOutputStream.write(1172526071813L, iEvaluateJobPriorityLocked2);
                            }
                            protoOutputStream.write(1112396529670L, j3 - runningJobLocked.madeActive);
                            protoOutputStream.write(1112396529671L, runningJobLocked.madeActive - runningJobLocked.madePending);
                            protoOutputStream.end(jStart6);
                        }
                        protoOutputStream.end(j);
                    }
                    if (i == -1) {
                        protoOutputStream.write(1133871366155L, this.mReadyToRock);
                        protoOutputStream.write(1133871366156L, this.mReportedActive);
                        protoOutputStream.write(1120986464269L, this.mMaxActiveJobs);
                    }
                    protoOutputStream.flush();
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                Object obj4 = obj;
                throw th;
            }
        }
    }

    static boolean lambda$dumpInternalProto$4(int i, JobStatus jobStatus) {
        return i == -1 || UserHandle.getAppId(jobStatus.getUid()) == i || UserHandle.getAppId(jobStatus.getSourceUid()) == i;
    }
}
