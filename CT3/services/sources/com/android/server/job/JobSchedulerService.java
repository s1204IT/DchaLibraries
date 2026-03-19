package com.android.server.job;

import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.IUidObserver;
import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.ArrayUtils;
import com.android.server.DeviceIdleController;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.job.JobStore;
import com.android.server.job.controllers.AppIdleController;
import com.android.server.job.controllers.BatteryController;
import com.android.server.job.controllers.ConnectivityController;
import com.android.server.job.controllers.ContentObserverController;
import com.android.server.job.controllers.DeviceIdleJobsController;
import com.android.server.job.controllers.IdleController;
import com.android.server.job.controllers.JobStatus;
import com.android.server.job.controllers.StateController;
import com.android.server.job.controllers.TimeController;
import com.android.server.pm.PackageManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import libcore.util.EmptyArray;

public final class JobSchedulerService extends SystemService implements StateChangedListener, JobCompletedListener {
    public static final boolean DEBUG = SystemProperties.getBoolean("log.tag.JobScheduler", false);
    private static final boolean ENFORCE_MAX_JOBS = true;
    private static final int MAX_JOBS_PER_APP = 100;
    private static final int MAX_JOB_CONTEXTS_COUNT = 16;
    static final int MSG_CHECK_JOB = 1;
    static final int MSG_CHECK_JOB_GREEDY = 3;
    static final int MSG_JOB_EXPIRED = 0;
    static final int MSG_STOP_JOB = 2;
    static final String TAG = "JobSchedulerService";
    final List<JobServiceContext> mActiveServices;
    IBatteryStats mBatteryStats;
    private final BroadcastReceiver mBroadcastReceiver;
    final Constants mConstants;
    List<StateController> mControllers;
    final JobHandler mHandler;
    final JobPackageTracker mJobPackageTracker;
    final JobSchedulerStub mJobSchedulerStub;
    final JobStore mJobs;
    DeviceIdleController.LocalService mLocalDeviceIdleController;
    final Object mLock;
    int mMaxActiveJobs;
    final ArrayList<JobStatus> mPendingJobs;
    PowerManager mPowerManager;
    boolean mReadyToRock;
    boolean mReportedActive;
    int[] mStartedUsers;
    boolean[] mTmpAssignAct;
    JobStatus[] mTmpAssignContextIdToJobMap;
    int[] mTmpAssignPreferredUidForContext;
    private final IUidObserver mUidObserver;
    final SparseIntArray mUidPriorityOverride;

    private final class Constants extends ContentObserver {
        private static final int DEFAULT_BG_CRITICAL_JOB_COUNT = 1;
        private static final int DEFAULT_BG_LOW_JOB_COUNT = 2;
        private static final int DEFAULT_BG_MODERATE_JOB_COUNT = 4;
        private static final int DEFAULT_BG_NORMAL_JOB_COUNT = 6;
        private static final int DEFAULT_FG_JOB_COUNT = 4;
        private static final float DEFAULT_HEAVY_USE_FACTOR = 0.9f;
        private static final int DEFAULT_MIN_CHARGING_COUNT = 1;
        private static final int DEFAULT_MIN_CONNECTIVITY_COUNT = 1;
        private static final int DEFAULT_MIN_CONTENT_COUNT = 1;
        private static final int DEFAULT_MIN_IDLE_COUNT = 1;
        private static final int DEFAULT_MIN_READY_JOBS_COUNT = 1;
        private static final float DEFAULT_MODERATE_USE_FACTOR = 0.5f;
        private static final String KEY_BG_CRITICAL_JOB_COUNT = "bg_critical_job_count";
        private static final String KEY_BG_LOW_JOB_COUNT = "bg_low_job_count";
        private static final String KEY_BG_MODERATE_JOB_COUNT = "bg_moderate_job_count";
        private static final String KEY_BG_NORMAL_JOB_COUNT = "bg_normal_job_count";
        private static final String KEY_FG_JOB_COUNT = "fg_job_count";
        private static final String KEY_HEAVY_USE_FACTOR = "heavy_use_factor";
        private static final String KEY_MIN_CHARGING_COUNT = "min_charging_count";
        private static final String KEY_MIN_CONNECTIVITY_COUNT = "min_connectivity_count";
        private static final String KEY_MIN_CONTENT_COUNT = "min_content_count";
        private static final String KEY_MIN_IDLE_COUNT = "min_idle_count";
        private static final String KEY_MIN_READY_JOBS_COUNT = "min_ready_jobs_count";
        private static final String KEY_MODERATE_USE_FACTOR = "moderate_use_factor";
        int BG_CRITICAL_JOB_COUNT;
        int BG_LOW_JOB_COUNT;
        int BG_MODERATE_JOB_COUNT;
        int BG_NORMAL_JOB_COUNT;
        int FG_JOB_COUNT;
        float HEAVY_USE_FACTOR;
        int MIN_CHARGING_COUNT;
        int MIN_CONNECTIVITY_COUNT;
        int MIN_CONTENT_COUNT;
        int MIN_IDLE_COUNT;
        int MIN_READY_JOBS_COUNT;
        float MODERATE_USE_FACTOR;
        private final KeyValueListParser mParser;
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            this.MIN_IDLE_COUNT = 1;
            this.MIN_CHARGING_COUNT = 1;
            this.MIN_CONNECTIVITY_COUNT = 1;
            this.MIN_CONTENT_COUNT = 1;
            this.MIN_READY_JOBS_COUNT = 1;
            this.HEAVY_USE_FACTOR = DEFAULT_HEAVY_USE_FACTOR;
            this.MODERATE_USE_FACTOR = 0.5f;
            this.FG_JOB_COUNT = 4;
            this.BG_NORMAL_JOB_COUNT = 6;
            this.BG_MODERATE_JOB_COUNT = 4;
            this.BG_LOW_JOB_COUNT = 2;
            this.BG_CRITICAL_JOB_COUNT = 1;
            this.mParser = new KeyValueListParser(',');
        }

        public void start(ContentResolver resolver) {
            this.mResolver = resolver;
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("job_scheduler_constants"), false, this);
            updateConstants();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (JobSchedulerService.this.mLock) {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, "alarm_manager_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(JobSchedulerService.TAG, "Bad device idle settings", e);
                }
                this.MIN_IDLE_COUNT = this.mParser.getInt(KEY_MIN_IDLE_COUNT, 1);
                this.MIN_CHARGING_COUNT = this.mParser.getInt(KEY_MIN_CHARGING_COUNT, 1);
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
                this.BG_LOW_JOB_COUNT = this.mParser.getInt(KEY_BG_LOW_JOB_COUNT, 2);
                if (this.FG_JOB_COUNT + this.BG_LOW_JOB_COUNT > 16) {
                    this.BG_LOW_JOB_COUNT = 16 - this.FG_JOB_COUNT;
                }
                this.BG_CRITICAL_JOB_COUNT = this.mParser.getInt(KEY_BG_CRITICAL_JOB_COUNT, 1);
                if (this.FG_JOB_COUNT + this.BG_CRITICAL_JOB_COUNT > 16) {
                    this.BG_CRITICAL_JOB_COUNT = 16 - this.FG_JOB_COUNT;
                }
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings:");
            pw.print("    ");
            pw.print(KEY_MIN_IDLE_COUNT);
            pw.print("=");
            pw.print(this.MIN_IDLE_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_CHARGING_COUNT);
            pw.print("=");
            pw.print(this.MIN_CHARGING_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_CONNECTIVITY_COUNT);
            pw.print("=");
            pw.print(this.MIN_CONNECTIVITY_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_CONTENT_COUNT);
            pw.print("=");
            pw.print(this.MIN_CONTENT_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_READY_JOBS_COUNT);
            pw.print("=");
            pw.print(this.MIN_READY_JOBS_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_HEAVY_USE_FACTOR);
            pw.print("=");
            pw.print(this.HEAVY_USE_FACTOR);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MODERATE_USE_FACTOR);
            pw.print("=");
            pw.print(this.MODERATE_USE_FACTOR);
            pw.println();
            pw.print("    ");
            pw.print(KEY_FG_JOB_COUNT);
            pw.print("=");
            pw.print(this.FG_JOB_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_BG_NORMAL_JOB_COUNT);
            pw.print("=");
            pw.print(this.BG_NORMAL_JOB_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_BG_MODERATE_JOB_COUNT);
            pw.print("=");
            pw.print(this.BG_MODERATE_JOB_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_BG_LOW_JOB_COUNT);
            pw.print("=");
            pw.print(this.BG_LOW_JOB_COUNT);
            pw.println();
            pw.print("    ");
            pw.print(KEY_BG_CRITICAL_JOB_COUNT);
            pw.print("=");
            pw.print(this.BG_CRITICAL_JOB_COUNT);
            pw.println();
        }
    }

    private String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            return null;
        }
        String pkg = uri.getSchemeSpecificPart();
        return pkg;
    }

    public Object getLock() {
        return this.mLock;
    }

    public JobStore getJobStore() {
        return this.mJobs;
    }

    @Override
    public void onStartUser(int userHandle) {
        this.mStartedUsers = ArrayUtils.appendInt(this.mStartedUsers, userHandle);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    @Override
    public void onUnlockUser(int userHandle) {
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    @Override
    public void onStopUser(int userHandle) {
        this.mStartedUsers = ArrayUtils.removeInt(this.mStartedUsers, userHandle);
    }

    public int schedule(JobInfo job, int uId) {
        return scheduleAsPackage(job, uId, null, -1, null);
    }

    public int scheduleAsPackage(JobInfo job, int uId, String packageName, int userId, String tag) {
        JobStatus jobStatus = JobStatus.createFromJobInfo(job, uId, packageName, userId, tag);
        try {
            if (ActivityManagerNative.getDefault().getAppStartMode(uId, job.getService().getPackageName()) == 2) {
                Slog.w(TAG, "Not scheduling job " + uId + ":" + job.toString() + " -- package not allowed to start");
                return 0;
            }
        } catch (RemoteException e) {
        }
        if (DEBUG) {
            Slog.d(TAG, "SCHEDULE: " + jobStatus.toShortString());
        }
        synchronized (this.mLock) {
            if (packageName == null) {
                if (this.mJobs.countJobsForUid(uId) > 100) {
                    Slog.w(TAG, "Too many jobs for uid " + uId);
                    throw new IllegalStateException("Apps may not schedule more than 100 distinct jobs");
                }
            }
            JobStatus toCancel = this.mJobs.getJobByUidAndJobId(uId, job.getId());
            if (toCancel != null) {
                Slog.d(TAG, "new job pkgname = " + jobStatus.getSourcePackageName());
                Slog.d(TAG, "old job pkgname = " + toCancel.getSourcePackageName());
                if (jobStatus.getSourcePackageName().equals(toCancel.getSourcePackageName())) {
                    cancelJobImpl(toCancel, jobStatus);
                }
            }
            startTrackingJob(jobStatus, toCancel);
        }
        this.mHandler.obtainMessage(1).sendToTarget();
        return 1;
    }

    public List<JobInfo> getPendingJobs(int uid) {
        ArrayList<JobInfo> outList;
        synchronized (this.mLock) {
            List<JobStatus> jobs = this.mJobs.getJobsByUid(uid);
            outList = new ArrayList<>(jobs.size());
            for (int i = jobs.size() - 1; i >= 0; i--) {
                JobStatus job = jobs.get(i);
                outList.add(job.getJob());
            }
        }
        return outList;
    }

    public JobInfo getPendingJob(int uid, int jobId) {
        synchronized (this.mLock) {
            List<JobStatus> jobs = this.mJobs.getJobsByUid(uid);
            for (int i = jobs.size() - 1; i >= 0; i--) {
                JobStatus job = jobs.get(i);
                if (job.getJobId() == jobId) {
                    return job.getJob();
                }
            }
            return null;
        }
    }

    void cancelJobsForUser(int userHandle) {
        List<JobStatus> jobsForUser;
        synchronized (this.mLock) {
            jobsForUser = this.mJobs.getJobsByUser(userHandle);
        }
        for (int i = 0; i < jobsForUser.size(); i++) {
            JobStatus toRemove = jobsForUser.get(i);
            cancelJobImpl(toRemove, null);
        }
    }

    public void cancelJobsForUid(int uid, boolean forceAll) {
        List<JobStatus> jobsForUid;
        synchronized (this.mLock) {
            jobsForUid = this.mJobs.getJobsByUid(uid);
        }
        for (int i = 0; i < jobsForUid.size(); i++) {
            JobStatus toRemove = jobsForUid.get(i);
            if (!forceAll) {
                String packageName = toRemove.getServiceComponent().getPackageName();
                if (ActivityManagerNative.getDefault().getAppStartMode(uid, packageName) == 2) {
                    cancelJobImpl(toRemove, null);
                }
            }
        }
    }

    public void cancelJob(int uid, int jobId) {
        JobStatus toCancel;
        synchronized (this.mLock) {
            toCancel = this.mJobs.getJobByUidAndJobId(uid, jobId);
        }
        if (toCancel == null) {
            return;
        }
        cancelJobImpl(toCancel, null);
    }

    private void cancelJobImpl(JobStatus cancelled, JobStatus incomingJob) {
        if (DEBUG) {
            Slog.d(TAG, "CANCEL: " + cancelled.toShortString());
        }
        stopTrackingJob(cancelled, incomingJob, true);
        synchronized (this.mLock) {
            if (this.mPendingJobs.remove(cancelled)) {
                this.mJobPackageTracker.noteNonpending(cancelled);
            }
            stopJobOnServiceContextLocked(cancelled, 0);
            reportActive();
        }
    }

    void updateUidState(int uid, int procState) {
        synchronized (this.mLock) {
            if (procState == 2) {
                this.mUidPriorityOverride.put(uid, 40);
            } else if (procState <= 4) {
                this.mUidPriorityOverride.put(uid, 30);
            } else {
                this.mUidPriorityOverride.delete(uid);
            }
        }
    }

    @Override
    public void onDeviceIdleStateChanged(boolean deviceIdle) {
        synchronized (this.mLock) {
            if (deviceIdle) {
                for (int i = 0; i < this.mActiveServices.size(); i++) {
                    JobServiceContext jsc = this.mActiveServices.get(i);
                    JobStatus executing = jsc.getRunningJob();
                    if (executing != null && (executing.getFlags() & 1) == 0) {
                        jsc.cancelExecutingJob(4);
                    }
                }
            } else {
                if (this.mReadyToRock && this.mLocalDeviceIdleController != null && !this.mReportedActive) {
                    this.mReportedActive = true;
                    this.mLocalDeviceIdleController.setJobsActive(true);
                }
                this.mHandler.obtainMessage(1).sendToTarget();
            }
        }
    }

    void reportActive() {
        boolean active = this.mPendingJobs.size() > 0;
        if (this.mPendingJobs.size() <= 0) {
            int i = 0;
            while (true) {
                if (i >= this.mActiveServices.size()) {
                    break;
                }
                JobServiceContext jsc = this.mActiveServices.get(i);
                JobStatus job = jsc.getRunningJob();
                if (job == null || (job.getJob().getFlags() & 1) != 0 || job.dozeWhitelisted) {
                    i++;
                } else {
                    active = true;
                    break;
                }
            }
        }
        if (this.mReportedActive == active) {
            return;
        }
        this.mReportedActive = active;
        if (this.mLocalDeviceIdleController == null) {
            return;
        }
        this.mLocalDeviceIdleController.setJobsActive(active);
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
        this.mTmpAssignContextIdToJobMap = new JobStatus[16];
        this.mTmpAssignAct = new boolean[16];
        this.mTmpAssignPreferredUidForContext = new int[16];
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "Receieved: " + intent.getAction());
                }
                if ("android.intent.action.PACKAGE_CHANGED".equals(intent.getAction())) {
                    String pkgName = JobSchedulerService.this.getPackageName(intent);
                    int pkgUid = intent.getIntExtra("android.intent.extra.UID", -1);
                    if (pkgName != null && pkgUid != -1) {
                        String[] changedComponents = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                        if (changedComponents == null) {
                            return;
                        }
                        for (String component : changedComponents) {
                            if (component.equals(pkgName)) {
                                if (JobSchedulerService.DEBUG) {
                                    Slog.d(JobSchedulerService.TAG, "Package state change: " + pkgName);
                                }
                                try {
                                    int userId = UserHandle.getUserId(pkgUid);
                                    IPackageManager pm = AppGlobals.getPackageManager();
                                    int state = pm.getApplicationEnabledSetting(pkgName, userId);
                                    if (state != 2 && state != 3) {
                                        return;
                                    }
                                    if (JobSchedulerService.DEBUG) {
                                        Slog.d(JobSchedulerService.TAG, "Removing jobs for package " + pkgName + " in user " + userId);
                                    }
                                    JobSchedulerService.this.cancelJobsForUid(pkgUid, true);
                                    return;
                                } catch (RemoteException e) {
                                    return;
                                }
                            }
                        }
                        return;
                    }
                    Slog.w(JobSchedulerService.TAG, "PACKAGE_CHANGED for " + pkgName + " / uid " + pkgUid);
                    return;
                }
                if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                    if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        return;
                    }
                    int uidRemoved = intent.getIntExtra("android.intent.extra.UID", -1);
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Removing jobs for uid: " + uidRemoved);
                    }
                    JobSchedulerService.this.cancelJobsForUid(uidRemoved, true);
                    return;
                }
                if (!"android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                    return;
                }
                int userId2 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "Removing jobs for user: " + userId2);
                }
                JobSchedulerService.this.cancelJobsForUser(userId2);
            }
        };
        this.mUidObserver = new IUidObserver.Stub() {
            public void onUidStateChanged(int uid, int procState) throws RemoteException {
                JobSchedulerService.this.updateUidState(uid, procState);
            }

            public void onUidGone(int uid) throws RemoteException {
                JobSchedulerService.this.updateUidState(uid, 16);
            }

            public void onUidActive(int uid) throws RemoteException {
            }

            public void onUidIdle(int uid) throws RemoteException {
                JobSchedulerService.this.cancelJobsForUid(uid, false);
            }
        };
        this.mHandler = new JobHandler(context.getMainLooper());
        this.mConstants = new Constants(this.mHandler);
        this.mJobSchedulerStub = new JobSchedulerStub();
        this.mJobs = JobStore.initAndGet(this);
        this.mControllers = new ArrayList();
        this.mControllers.add(ConnectivityController.get(this));
        this.mControllers.add(TimeController.get(this));
        this.mControllers.add(IdleController.get(this));
        this.mControllers.add(BatteryController.get(this));
        this.mControllers.add(AppIdleController.get(this));
        this.mControllers.add(ContentObserverController.get(this));
        this.mControllers.add(DeviceIdleJobsController.get(this));
    }

    @Override
    public void onStart() {
        publishLocalService(JobSchedulerInternal.class, new LocalService());
        publishBinderService("jobscheduler", this.mJobSchedulerStub);
    }

    @Override
    public void onBootPhase(int phase) {
        if (500 == phase) {
            this.mConstants.start(getContext().getContentResolver());
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addDataScheme("package");
            getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
            IntentFilter userFilter = new IntentFilter("android.intent.action.USER_REMOVED");
            getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, userFilter, null, null);
            this.mPowerManager = (PowerManager) getContext().getSystemService("power");
            try {
                ActivityManagerNative.getDefault().registerUidObserver(this.mUidObserver, 7);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        if (phase != 600) {
            return;
        }
        synchronized (this.mLock) {
            this.mReadyToRock = true;
            this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
            this.mLocalDeviceIdleController = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class);
            for (int i = 0; i < 16; i++) {
                this.mActiveServices.add(new JobServiceContext(this, this.mBatteryStats, this.mJobPackageTracker, getContext().getMainLooper()));
            }
            this.mJobs.forEachJob(new JobStore.JobStatusFunctor() {
                @Override
                public void process(JobStatus job) {
                    for (int controller = 0; controller < JobSchedulerService.this.mControllers.size(); controller++) {
                        StateController sc = JobSchedulerService.this.mControllers.get(controller);
                        sc.maybeStartTrackingJobLocked(job, null);
                    }
                }
            });
            this.mHandler.obtainMessage(1).sendToTarget();
        }
    }

    private void startTrackingJob(JobStatus jobStatus, JobStatus lastJob) {
        synchronized (this.mLock) {
            boolean update = this.mJobs.add(jobStatus);
            if (this.mReadyToRock) {
                for (int i = 0; i < this.mControllers.size(); i++) {
                    StateController controller = this.mControllers.get(i);
                    if (update) {
                        controller.maybeStopTrackingJobLocked(jobStatus, null, true);
                    }
                    controller.maybeStartTrackingJobLocked(jobStatus, lastJob);
                }
            }
        }
    }

    private boolean stopTrackingJob(JobStatus jobStatus, JobStatus incomingJob, boolean writeBack) {
        boolean removed;
        synchronized (this.mLock) {
            removed = this.mJobs.remove(jobStatus, writeBack);
            if (removed && this.mReadyToRock) {
                for (int i = 0; i < this.mControllers.size(); i++) {
                    StateController controller = this.mControllers.get(i);
                    controller.maybeStopTrackingJobLocked(jobStatus, incomingJob, false);
                }
            }
        }
        return removed;
    }

    private boolean stopJobOnServiceContextLocked(JobStatus job, int reason) {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext jsc = this.mActiveServices.get(i);
            JobStatus executing = jsc.getRunningJob();
            if (executing != null && executing.matches(job.getUid(), job.getJobId())) {
                jsc.cancelExecutingJob(reason);
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentlyActiveLocked(JobStatus job) {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext serviceContext = this.mActiveServices.get(i);
            JobStatus running = serviceContext.getRunningJob();
            if (running != null && running.matches(job.getUid(), job.getJobId())) {
                return true;
            }
        }
        return false;
    }

    void noteJobsPending(List<JobStatus> jobs) {
        for (int i = jobs.size() - 1; i >= 0; i--) {
            JobStatus job = jobs.get(i);
            this.mJobPackageTracker.notePending(job);
        }
    }

    void noteJobsNonpending(List<JobStatus> jobs) {
        for (int i = jobs.size() - 1; i >= 0; i--) {
            JobStatus job = jobs.get(i);
            this.mJobPackageTracker.noteNonpending(job);
        }
    }

    private JobStatus getRescheduleJobForFailure(JobStatus failureToReschedule) {
        long delayMillis;
        long elapsedNowMillis = SystemClock.elapsedRealtime();
        JobInfo job = failureToReschedule.getJob();
        long initialBackoffMillis = job.getInitialBackoffMillis();
        int backoffAttempts = failureToReschedule.getNumFailures() + 1;
        switch (job.getBackoffPolicy()) {
            case 0:
                delayMillis = initialBackoffMillis * ((long) backoffAttempts);
                break;
            default:
                if (DEBUG) {
                    Slog.v(TAG, "Unrecognised back-off policy, defaulting to exponential.");
                    break;
                }
            case 1:
                delayMillis = (long) Math.scalb(initialBackoffMillis, backoffAttempts - 1);
                break;
        }
        JobStatus newJob = new JobStatus(failureToReschedule, elapsedNowMillis + Math.min(delayMillis, 18000000L), JobStatus.NO_LATEST_RUNTIME, backoffAttempts);
        for (int ic = 0; ic < this.mControllers.size(); ic++) {
            StateController controller = this.mControllers.get(ic);
            controller.rescheduleForFailure(newJob, failureToReschedule);
        }
        return newJob;
    }

    private JobStatus getRescheduleJobForPeriodic(JobStatus periodicToReschedule) {
        long elapsedNow = SystemClock.elapsedRealtime();
        long runEarly = periodicToReschedule.hasDeadlineConstraint() ? Math.max(periodicToReschedule.getLatestRunTimeElapsed() - elapsedNow, 0L) : 0L;
        long flex = periodicToReschedule.getJob().getFlexMillis();
        long period = periodicToReschedule.getJob().getIntervalMillis();
        long newLatestRuntimeElapsed = elapsedNow + runEarly + period;
        long newEarliestRunTimeElapsed = newLatestRuntimeElapsed - flex;
        if (DEBUG) {
            Slog.v(TAG, "Rescheduling executed periodic. New execution window [" + (newEarliestRunTimeElapsed / 1000) + ", " + (newLatestRuntimeElapsed / 1000) + "]s");
        }
        return new JobStatus(periodicToReschedule, newEarliestRunTimeElapsed, newLatestRuntimeElapsed, 0);
    }

    @Override
    public void onJobCompleted(JobStatus jobStatus, boolean needsReschedule) {
        if (DEBUG) {
            Slog.d(TAG, "Completed " + jobStatus + ", reschedule=" + needsReschedule);
        }
        if (!stopTrackingJob(jobStatus, null, !jobStatus.getJob().isPeriodic())) {
            if (DEBUG) {
                Slog.d(TAG, "Could not find job to remove. Was job removed while executing?");
            }
            this.mHandler.obtainMessage(3).sendToTarget();
            return;
        }
        if (needsReschedule) {
            JobStatus rescheduled = getRescheduleJobForFailure(jobStatus);
            startTrackingJob(rescheduled, jobStatus);
        } else if (jobStatus.getJob().isPeriodic()) {
            JobStatus rescheduledPeriodic = getRescheduleJobForPeriodic(jobStatus);
            startTrackingJob(rescheduledPeriodic, jobStatus);
        }
        reportActive();
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

    private class JobHandler extends Handler {
        private final MaybeReadyJobQueueFunctor mMaybeQueueFunctor;
        private final ReadyJobQueueFunctor mReadyQueueFunctor;

        public JobHandler(Looper looper) {
            super(looper);
            this.mReadyQueueFunctor = new ReadyJobQueueFunctor();
            this.mMaybeQueueFunctor = new MaybeReadyJobQueueFunctor();
        }

        @Override
        public void handleMessage(Message message) {
            Object obj;
            synchronized (JobSchedulerService.this.mLock) {
                if (!JobSchedulerService.this.mReadyToRock) {
                    return;
                }
                switch (message.what) {
                    case 0:
                        obj = JobSchedulerService.this.mLock;
                        synchronized (obj) {
                            JobStatus runNow = (JobStatus) message.obj;
                            if (runNow != null && !JobSchedulerService.this.mPendingJobs.contains(runNow) && JobSchedulerService.this.mJobs.containsJob(runNow)) {
                                JobSchedulerService.this.mJobPackageTracker.notePending(runNow);
                                JobSchedulerService.this.mPendingJobs.add(runNow);
                            }
                            queueReadyJobsForExecutionLockedH();
                            maybeRunPendingJobsH();
                            removeMessages(1);
                            return;
                        }
                    case 1:
                        obj = JobSchedulerService.this.mLock;
                        synchronized (obj) {
                            if (JobSchedulerService.this.mReportedActive) {
                                queueReadyJobsForExecutionLockedH();
                            } else {
                                maybeQueueReadyJobsForExecutionLockedH();
                            }
                            maybeRunPendingJobsH();
                            removeMessages(1);
                            return;
                        }
                    case 2:
                        JobSchedulerService.this.cancelJobImpl((JobStatus) message.obj, null);
                        maybeRunPendingJobsH();
                        removeMessages(1);
                        return;
                    case 3:
                        synchronized (JobSchedulerService.this.mLock) {
                            queueReadyJobsForExecutionLockedH();
                        }
                        maybeRunPendingJobsH();
                        removeMessages(1);
                        return;
                    default:
                        maybeRunPendingJobsH();
                        removeMessages(1);
                        return;
                }
            }
        }

        private void queueReadyJobsForExecutionLockedH() {
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "queuing all ready jobs for execution:");
            }
            JobSchedulerService.this.noteJobsNonpending(JobSchedulerService.this.mPendingJobs);
            JobSchedulerService.this.mPendingJobs.clear();
            JobSchedulerService.this.mJobs.forEachJob(this.mReadyQueueFunctor);
            this.mReadyQueueFunctor.postProcess();
            if (!JobSchedulerService.DEBUG) {
                return;
            }
            int queuedJobs = JobSchedulerService.this.mPendingJobs.size();
            if (queuedJobs == 0) {
                Slog.d(JobSchedulerService.TAG, "No jobs pending.");
            } else {
                Slog.d(JobSchedulerService.TAG, queuedJobs + " jobs queued.");
            }
        }

        class ReadyJobQueueFunctor implements JobStore.JobStatusFunctor {
            ArrayList<JobStatus> newReadyJobs;

            ReadyJobQueueFunctor() {
            }

            @Override
            public void process(JobStatus job) {
                if (JobHandler.this.isReadyToBeExecutedLocked(job)) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "    queued " + job.toShortString());
                    }
                    if (this.newReadyJobs == null) {
                        this.newReadyJobs = new ArrayList<>();
                    }
                    this.newReadyJobs.add(job);
                    return;
                }
                if (!JobHandler.this.areJobConstraintsNotSatisfiedLocked(job)) {
                    return;
                }
                JobSchedulerService.this.stopJobOnServiceContextLocked(job, 1);
            }

            public void postProcess() {
                if (this.newReadyJobs != null) {
                    JobSchedulerService.this.noteJobsPending(this.newReadyJobs);
                    JobSchedulerService.this.mPendingJobs.addAll(this.newReadyJobs);
                }
                this.newReadyJobs = null;
            }
        }

        class MaybeReadyJobQueueFunctor implements JobStore.JobStatusFunctor {
            int backoffCount;
            int chargingCount;
            int connectivityCount;
            int contentCount;
            int idleCount;
            List<JobStatus> runnableJobs;

            public MaybeReadyJobQueueFunctor() {
                reset();
            }

            @Override
            public void process(JobStatus job) {
                if (JobHandler.this.isReadyToBeExecutedLocked(job)) {
                    try {
                        if (ActivityManagerNative.getDefault().getAppStartMode(job.getUid(), job.getJob().getService().getPackageName()) == 2) {
                            Slog.w(JobSchedulerService.TAG, "Aborting job " + job.getUid() + ":" + job.getJob().toString() + " -- package not allowed to start");
                            JobSchedulerService.this.mHandler.obtainMessage(2, job).sendToTarget();
                            return;
                        }
                    } catch (RemoteException e) {
                    }
                    if (job.getNumFailures() > 0) {
                        this.backoffCount++;
                    }
                    if (job.hasIdleConstraint()) {
                        this.idleCount++;
                    }
                    if (job.hasConnectivityConstraint() || job.hasUnmeteredConstraint() || job.hasNotRoamingConstraint()) {
                        this.connectivityCount++;
                    }
                    if (job.hasChargingConstraint()) {
                        this.chargingCount++;
                    }
                    if (job.hasContentTriggerConstraint()) {
                        this.contentCount++;
                    }
                    if (this.runnableJobs == null) {
                        this.runnableJobs = new ArrayList();
                    }
                    this.runnableJobs.add(job);
                    return;
                }
                if (!JobHandler.this.areJobConstraintsNotSatisfiedLocked(job)) {
                    return;
                }
                JobSchedulerService.this.stopJobOnServiceContextLocked(job, 1);
            }

            public void postProcess() {
                if (this.backoffCount > 0 || this.idleCount >= JobSchedulerService.this.mConstants.MIN_IDLE_COUNT || this.connectivityCount >= JobSchedulerService.this.mConstants.MIN_CONNECTIVITY_COUNT || this.chargingCount >= JobSchedulerService.this.mConstants.MIN_CHARGING_COUNT || this.contentCount >= JobSchedulerService.this.mConstants.MIN_CONTENT_COUNT || (this.runnableJobs != null && this.runnableJobs.size() >= JobSchedulerService.this.mConstants.MIN_READY_JOBS_COUNT)) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "maybeQueueReadyJobsForExecutionLockedH: Running jobs.");
                    }
                    JobSchedulerService.this.noteJobsPending(this.runnableJobs);
                    JobSchedulerService.this.mPendingJobs.addAll(this.runnableJobs);
                } else if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "maybeQueueReadyJobsForExecutionLockedH: Not running anything.");
                }
                reset();
            }

            private void reset() {
                this.chargingCount = 0;
                this.idleCount = 0;
                this.backoffCount = 0;
                this.connectivityCount = 0;
                this.contentCount = 0;
                this.runnableJobs = null;
            }
        }

        private void maybeQueueReadyJobsForExecutionLockedH() {
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Maybe queuing ready jobs...");
            }
            JobSchedulerService.this.noteJobsNonpending(JobSchedulerService.this.mPendingJobs);
            JobSchedulerService.this.mPendingJobs.clear();
            JobSchedulerService.this.mJobs.forEachJob(this.mMaybeQueueFunctor);
            this.mMaybeQueueFunctor.postProcess();
        }

        private boolean isReadyToBeExecutedLocked(JobStatus job) {
            boolean jobReady = job.isReady();
            boolean jobPending = JobSchedulerService.this.mPendingJobs.contains(job);
            boolean jobActive = JobSchedulerService.this.isCurrentlyActiveLocked(job);
            int userId = job.getUserId();
            boolean userStarted = ArrayUtils.contains(JobSchedulerService.this.mStartedUsers, userId);
            try {
                boolean componentPresent = AppGlobals.getPackageManager().getServiceInfo(job.getServiceComponent(), 268435456, userId) != null;
                if (JobSchedulerService.DEBUG) {
                    Slog.v(JobSchedulerService.TAG, "isReadyToBeExecutedLocked: " + job.toShortString() + " ready=" + jobReady + " pending=" + jobPending + " active=" + jobActive + " userStarted=" + userStarted + " componentPresent=" + componentPresent);
                }
                return userStarted && componentPresent && jobReady && !jobPending && !jobActive;
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }

        private boolean areJobConstraintsNotSatisfiedLocked(JobStatus job) {
            if (job.isReady()) {
                return false;
            }
            return JobSchedulerService.this.isCurrentlyActiveLocked(job);
        }

        private void maybeRunPendingJobsH() {
            synchronized (JobSchedulerService.this.mLock) {
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "pending queue: " + JobSchedulerService.this.mPendingJobs.size() + " jobs.");
                }
                JobSchedulerService.this.assignJobsToContextsLocked();
                JobSchedulerService.this.reportActive();
            }
        }
    }

    private int adjustJobPriority(int curPriority, JobStatus job) {
        if (curPriority < 40) {
            float factor = this.mJobPackageTracker.getLoadFactor(job);
            if (factor >= this.mConstants.HEAVY_USE_FACTOR) {
                return curPriority - 80;
            }
            if (factor >= this.mConstants.MODERATE_USE_FACTOR) {
                return curPriority - 40;
            }
            return curPriority;
        }
        return curPriority;
    }

    private int evaluateJobPriorityLocked(JobStatus job) {
        int priority = job.getPriority();
        if (priority >= 30) {
            return adjustJobPriority(priority, job);
        }
        int override = this.mUidPriorityOverride.get(job.getSourceUid(), 0);
        if (override != 0) {
            return adjustJobPriority(override, job);
        }
        return adjustJobPriority(priority, job);
    }

    private void assignJobsToContextsLocked() {
        int memLevel;
        if (DEBUG) {
            Slog.d(TAG, printPendingQueue());
        }
        try {
            memLevel = ActivityManagerNative.getDefault().getMemoryTrimLevel();
        } catch (RemoteException e) {
            memLevel = 0;
        }
        switch (memLevel) {
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
        JobStatus[] contextIdToJobMap = this.mTmpAssignContextIdToJobMap;
        boolean[] act = this.mTmpAssignAct;
        int[] preferredUidForContext = this.mTmpAssignPreferredUidForContext;
        int numActive = 0;
        int numForeground = 0;
        for (int i = 0; i < 16; i++) {
            JobServiceContext js = this.mActiveServices.get(i);
            JobStatus status = js.getRunningJob();
            contextIdToJobMap[i] = status;
            if (status != null) {
                numActive++;
                if (status.lastEvaluatedPriority >= 40) {
                    numForeground++;
                }
            }
            act[i] = false;
            preferredUidForContext[i] = js.getPreferredUid();
        }
        if (DEBUG) {
            Slog.d(TAG, printContextIdToJobMap(contextIdToJobMap, "running jobs initial"));
        }
        for (int i2 = 0; i2 < this.mPendingJobs.size(); i2++) {
            JobStatus nextPending = this.mPendingJobs.get(i2);
            int jobRunningContext = findJobContextIdFromMap(nextPending, contextIdToJobMap);
            if (jobRunningContext == -1) {
                int priority = evaluateJobPriorityLocked(nextPending);
                nextPending.lastEvaluatedPriority = priority;
                int minPriority = Integer.MAX_VALUE;
                int minPriorityContextId = -1;
                for (int j = 0; j < 16; j++) {
                    JobStatus job = contextIdToJobMap[j];
                    int preferredUid = preferredUidForContext[j];
                    if (job == null) {
                        if ((numActive < this.mMaxActiveJobs || (priority >= 40 && numForeground < this.mConstants.FG_JOB_COUNT)) && (preferredUid == nextPending.getUid() || preferredUid == -1)) {
                            minPriorityContextId = j;
                            if (minPriorityContextId == -1) {
                                contextIdToJobMap[minPriorityContextId] = nextPending;
                                act[minPriorityContextId] = true;
                                numActive++;
                                if (priority >= 40) {
                                    numForeground++;
                                }
                            }
                        }
                    } else if (job.getUid() == nextPending.getUid() && evaluateJobPriorityLocked(job) < nextPending.lastEvaluatedPriority && minPriority > nextPending.lastEvaluatedPriority) {
                        minPriority = nextPending.lastEvaluatedPriority;
                        minPriorityContextId = j;
                    }
                }
                if (minPriorityContextId == -1) {
                }
            }
        }
        if (DEBUG) {
            Slog.d(TAG, printContextIdToJobMap(contextIdToJobMap, "running jobs final"));
        }
        this.mJobPackageTracker.noteConcurrency(numActive, numForeground);
        for (int i3 = 0; i3 < 16; i3++) {
            boolean preservePreferredUid = false;
            if (act[i3]) {
                if (this.mActiveServices.get(i3).getRunningJob() != null) {
                    if (DEBUG) {
                        Slog.d(TAG, "preempting job: " + this.mActiveServices.get(i3).getRunningJob());
                    }
                    this.mActiveServices.get(i3).preemptExecutingJob();
                    preservePreferredUid = true;
                } else {
                    JobStatus pendingJob = contextIdToJobMap[i3];
                    if (DEBUG) {
                        Slog.d(TAG, "About to run job on context " + String.valueOf(i3) + ", job: " + pendingJob);
                    }
                    for (int ic = 0; ic < this.mControllers.size(); ic++) {
                        this.mControllers.get(ic).prepareForExecutionLocked(pendingJob);
                    }
                    if (!this.mActiveServices.get(i3).executeRunnableJob(pendingJob)) {
                        Slog.d(TAG, "Error executing " + pendingJob);
                    }
                    if (this.mPendingJobs.remove(pendingJob)) {
                        this.mJobPackageTracker.noteNonpending(pendingJob);
                    }
                }
            }
            if (!preservePreferredUid) {
                this.mActiveServices.get(i3).clearPreferredUid();
            }
        }
    }

    int findJobContextIdFromMap(JobStatus jobStatus, JobStatus[] map) {
        for (int i = 0; i < map.length; i++) {
            if (map[i] != null && map[i].matches(jobStatus.getUid(), jobStatus.getJobId())) {
                return i;
            }
        }
        return -1;
    }

    final class LocalService implements JobSchedulerInternal {
        LocalService() {
        }

        @Override
        public List<JobInfo> getSystemScheduledPendingJobs() {
            final List<JobInfo> pendingJobs;
            synchronized (JobSchedulerService.this.mLock) {
                pendingJobs = new ArrayList<>();
                JobSchedulerService.this.mJobs.forEachJob(1000, new JobStore.JobStatusFunctor() {
                    @Override
                    public void process(JobStatus job) {
                        if (!job.getJob().isPeriodic() && JobSchedulerService.this.isCurrentlyActiveLocked(job)) {
                            return;
                        }
                        pendingJobs.add(job.getJob());
                    }
                });
            }
            return pendingJobs;
        }

        @Override
        public void cancelJobsForUid(int uid) {
            if (JobSchedulerService.DEBUG) {
                Slog.i(JobSchedulerService.TAG, "cancelJobsForUid by SYSTEM! uid = " + uid);
            }
            JobSchedulerService.this.cancelJobsForUid(uid, true);
        }
    }

    final class JobSchedulerStub extends IJobScheduler.Stub {
        private final SparseArray<Boolean> mPersistCache = new SparseArray<>();

        JobSchedulerStub() {
        }

        private void enforceValidJobRequest(int uid, JobInfo job) {
            IPackageManager pm = AppGlobals.getPackageManager();
            ComponentName service = job.getService();
            try {
                ServiceInfo si = pm.getServiceInfo(service, 786432, UserHandle.getUserId(uid));
                if (si == null) {
                    throw new IllegalArgumentException("No such service " + service);
                }
                if (si.applicationInfo.uid != uid) {
                    throw new IllegalArgumentException("uid " + uid + " cannot schedule job in " + service.getPackageName());
                }
                if ("android.permission.BIND_JOB_SERVICE".equals(si.permission)) {
                } else {
                    throw new IllegalArgumentException("Scheduled service " + service + " does not require android.permission.BIND_JOB_SERVICE permission");
                }
            } catch (RemoteException e) {
            }
        }

        private boolean canPersistJobs(int pid, int uid) {
            boolean canPersist;
            synchronized (this.mPersistCache) {
                Boolean cached = this.mPersistCache.get(uid);
                if (cached != null) {
                    canPersist = cached.booleanValue();
                } else {
                    int result = JobSchedulerService.this.getContext().checkPermission("android.permission.RECEIVE_BOOT_COMPLETED", pid, uid);
                    canPersist = result == 0;
                    this.mPersistCache.put(uid, Boolean.valueOf(canPersist));
                }
            }
            return canPersist;
        }

        public int schedule(JobInfo job) throws RemoteException {
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Scheduling job: " + job.toString());
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            enforceValidJobRequest(uid, job);
            if (job.isPersisted() && !canPersistJobs(pid, uid)) {
                throw new IllegalArgumentException("Error: requested job be persisted without holding RECEIVE_BOOT_COMPLETED permission.");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.schedule(job, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int scheduleAsPackage(JobInfo job, String packageName, int userId, String tag) throws RemoteException {
            int callerUid = Binder.getCallingUid();
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Caller uid " + callerUid + " scheduling job: " + job.toString() + " on behalf of " + packageName);
            }
            if (packageName == null) {
                throw new NullPointerException("Must specify a package for scheduleAsPackage()");
            }
            int mayScheduleForOthers = JobSchedulerService.this.getContext().checkCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS");
            if (mayScheduleForOthers != 0) {
                throw new SecurityException("Caller uid " + callerUid + " not permitted to schedule jobs for other apps");
            }
            if ((job.getFlags() & 1) != 0) {
                JobSchedulerService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", JobSchedulerService.TAG);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.scheduleAsPackage(job, callerUid, packageName, userId, tag);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public List<JobInfo> getAllPendingJobs() throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.getPendingJobs(uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public JobInfo getPendingJob(int jobId) throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.getPendingJob(uid, jobId);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void cancelAll() throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.cancelJobsForUid(uid, true);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void cancel(int jobId) throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.cancelJob(uid, jobId);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            JobSchedulerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DUMP", JobSchedulerService.TAG);
            long identityToken = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.dumpInternal(pw, args);
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) throws RemoteException {
            new JobSchedulerShellCommand(JobSchedulerService.this).exec(this, in, out, err, args, resultReceiver);
        }
    }

    int executeRunCommand(String pkgName, int userId, int jobId, boolean force) {
        if (DEBUG) {
            Slog.v(TAG, "executeRunCommand(): " + pkgName + "/" + userId + " " + jobId + " f=" + force);
        }
        try {
            int uid = AppGlobals.getPackageManager().getPackageUid(pkgName, 0, userId);
            if (uid < 0) {
                return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            synchronized (this.mLock) {
                JobStatus js = this.mJobs.getJobByUidAndJobId(uid, jobId);
                if (js == null) {
                    return JobSchedulerShellCommand.CMD_ERR_NO_JOB;
                }
                js.overrideState = force ? 2 : 1;
                if (!js.isConstraintsSatisfied()) {
                    js.overrideState = 0;
                    return JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS;
                }
                this.mHandler.obtainMessage(3).sendToTarget();
            }
        } catch (RemoteException e) {
        }
        return 0;
    }

    private String printContextIdToJobMap(JobStatus[] map, String initial) {
        StringBuilder s = new StringBuilder(initial + ": ");
        for (int i = 0; i < map.length; i++) {
            s.append("(").append(map[i] == null ? -1 : map[i].getJobId()).append(map[i] == null ? -1 : map[i].getUid()).append(")");
        }
        return s.toString();
    }

    private String printPendingQueue() {
        StringBuilder s = new StringBuilder("Pending queue: ");
        for (JobStatus js : this.mPendingJobs) {
            s.append("(").append(js.getJob().getId()).append(", ").append(js.getUid()).append(") ");
        }
        return s.toString();
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Job Scheduler (jobscheduler) dump options:");
        pw.println("  [-h] [package] ...");
        pw.println("    -h: print this help");
        pw.println("  [package] is an optional package name to limit the output to.");
    }

    void dumpInternal(PrintWriter pw, String[] args) {
        int filterUid = -1;
        if (!ArrayUtils.isEmpty(args)) {
            int opti = 0;
            while (true) {
                if (opti >= args.length) {
                    break;
                }
                String arg = args[opti];
                if ("-h".equals(arg)) {
                    dumpHelp(pw);
                    return;
                } else if (!"-a".equals(arg)) {
                    if (arg.length() > 0 && arg.charAt(0) == '-') {
                        pw.println("Unknown option: " + arg);
                        return;
                    }
                } else {
                    opti++;
                }
            }
            if (opti < args.length) {
                String pkg = args[opti];
                try {
                    filterUid = getContext().getPackageManager().getPackageUid(pkg, PackageManagerService.DumpState.DUMP_PREFERRED_XML);
                } catch (PackageManager.NameNotFoundException e) {
                    pw.println("Invalid package: " + pkg);
                    return;
                }
            }
        }
        int filterUidFinal = UserHandle.getAppId(filterUid);
        long now = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            this.mConstants.dump(pw);
            pw.println();
            pw.println("Started users: " + Arrays.toString(this.mStartedUsers));
            pw.print("Registered ");
            pw.print(this.mJobs.size());
            pw.println(" jobs:");
            if (this.mJobs.size() > 0) {
                List<JobStatus> jobs = this.mJobs.mJobSet.getAllJobs();
                Collections.sort(jobs, new Comparator<JobStatus>() {
                    @Override
                    public int compare(JobStatus o1, JobStatus o2) {
                        int uid1 = o1.getUid();
                        int uid2 = o2.getUid();
                        int id1 = o1.getJobId();
                        int id2 = o2.getJobId();
                        if (uid1 != uid2) {
                            return uid1 < uid2 ? -1 : 1;
                        }
                        if (id1 < id2) {
                            return -1;
                        }
                        return id1 > id2 ? 1 : 0;
                    }
                });
                for (JobStatus job : jobs) {
                    pw.print("  JOB #");
                    job.printUniqueId(pw);
                    pw.print(": ");
                    pw.println(job.toShortStringExceptUniqueId());
                    if (job.shouldDump(filterUidFinal)) {
                        job.dump(pw, "    ", true);
                        pw.print("    Ready: ");
                        pw.print(this.mHandler.isReadyToBeExecutedLocked(job));
                        pw.print(" (job=");
                        pw.print(job.isReady());
                        pw.print(" pending=");
                        pw.print(this.mPendingJobs.contains(job));
                        pw.print(" active=");
                        pw.print(isCurrentlyActiveLocked(job));
                        pw.print(" user=");
                        pw.print(ArrayUtils.contains(this.mStartedUsers, job.getUserId()));
                        pw.println(")");
                    }
                }
            } else {
                pw.println("  None.");
            }
            for (int i = 0; i < this.mControllers.size(); i++) {
                pw.println();
                this.mControllers.get(i).dumpControllerStateLocked(pw, filterUidFinal);
            }
            pw.println();
            pw.println("Uid priority overrides:");
            for (int i2 = 0; i2 < this.mUidPriorityOverride.size(); i2++) {
                int uid = this.mUidPriorityOverride.keyAt(i2);
                if (filterUidFinal == -1 || filterUidFinal == UserHandle.getAppId(uid)) {
                    pw.print("  ");
                    pw.print(UserHandle.formatUid(uid));
                    pw.print(": ");
                    pw.println(this.mUidPriorityOverride.valueAt(i2));
                }
            }
            pw.println();
            this.mJobPackageTracker.dump(pw, "", filterUidFinal);
            pw.println();
            if (this.mJobPackageTracker.dumpHistory(pw, "", filterUidFinal)) {
                pw.println();
            }
            pw.println("Pending queue:");
            for (int i3 = 0; i3 < this.mPendingJobs.size(); i3++) {
                JobStatus job2 = this.mPendingJobs.get(i3);
                pw.print("  Pending #");
                pw.print(i3);
                pw.print(": ");
                pw.println(job2.toShortString());
                job2.dump(pw, "    ", false);
                int priority = evaluateJobPriorityLocked(job2);
                if (priority != 0) {
                    pw.print("    Evaluated priority: ");
                    pw.println(priority);
                }
                pw.print("    Tag: ");
                pw.println(job2.getTag());
            }
            pw.println();
            pw.println("Active jobs:");
            for (int i4 = 0; i4 < this.mActiveServices.size(); i4++) {
                JobServiceContext jsc = this.mActiveServices.get(i4);
                pw.print("  Slot #");
                pw.print(i4);
                pw.print(": ");
                if (jsc.getRunningJob() == null) {
                    pw.println("inactive");
                } else {
                    pw.println(jsc.getRunningJob().toShortString());
                    pw.print("    Running for: ");
                    TimeUtils.formatDuration(now - jsc.getExecutionStartTimeElapsed(), pw);
                    pw.print(", timeout at: ");
                    TimeUtils.formatDuration(jsc.getTimeoutElapsed() - now, pw);
                    pw.println();
                    jsc.getRunningJob().dump(pw, "    ", false);
                    int priority2 = evaluateJobPriorityLocked(jsc.getRunningJob());
                    if (priority2 != 0) {
                        pw.print("    Evaluated priority: ");
                        pw.println(priority2);
                    }
                }
            }
            if (filterUid == -1) {
                pw.println();
                pw.print("mReadyToRock=");
                pw.println(this.mReadyToRock);
                pw.print("mReportedActive=");
                pw.println(this.mReportedActive);
                pw.print("mMaxActiveJobs=");
                pw.println(this.mMaxActiveJobs);
            }
        }
        pw.println();
    }
}
