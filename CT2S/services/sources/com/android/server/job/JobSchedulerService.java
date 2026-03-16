package com.android.server.job;

import android.app.AppGlobals;
import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.app.IBatteryStats;
import com.android.server.SystemService;
import com.android.server.job.controllers.BatteryController;
import com.android.server.job.controllers.ConnectivityController;
import com.android.server.job.controllers.IdleController;
import com.android.server.job.controllers.JobStatus;
import com.android.server.job.controllers.StateController;
import com.android.server.job.controllers.TimeController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JobSchedulerService extends SystemService implements StateChangedListener, JobCompletedListener {
    static final boolean DEBUG = false;
    private static final int MAX_JOB_CONTEXTS_COUNT = 3;
    static final int MIN_CHARGING_COUNT = 1;
    static final int MIN_CONNECTIVITY_COUNT = 2;
    static final int MIN_IDLE_COUNT = 1;
    static final int MIN_READY_JOBS_COUNT = 2;
    static final int MSG_CHECK_JOB = 1;
    static final int MSG_JOB_EXPIRED = 0;
    static final String TAG = "JobSchedulerService";
    final List<JobServiceContext> mActiveServices;
    IBatteryStats mBatteryStats;
    private final BroadcastReceiver mBroadcastReceiver;
    List<StateController> mControllers;
    final JobHandler mHandler;
    final JobSchedulerStub mJobSchedulerStub;
    final JobStore mJobs;
    final ArrayList<JobStatus> mPendingJobs;
    boolean mReadyToRock;
    final ArrayList<Integer> mStartedUsers;

    @Override
    public void onStartUser(int userHandle) {
        this.mStartedUsers.add(Integer.valueOf(userHandle));
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    @Override
    public void onStopUser(int userHandle) {
        this.mStartedUsers.remove(Integer.valueOf(userHandle));
    }

    public int schedule(JobInfo job, int uId) {
        JobStatus jobStatus = new JobStatus(job, uId);
        cancelJob(uId, job.getId());
        startTrackingJob(jobStatus);
        this.mHandler.obtainMessage(1).sendToTarget();
        return 1;
    }

    public List<JobInfo> getPendingJobs(int uid) {
        ArrayList<JobInfo> outList = new ArrayList<>();
        synchronized (this.mJobs) {
            ArraySet<JobStatus> jobs = this.mJobs.getJobs();
            for (int i = 0; i < jobs.size(); i++) {
                JobStatus job = jobs.valueAt(i);
                if (job.getUid() == uid) {
                    outList.add(job.getJob());
                }
            }
        }
        return outList;
    }

    private void cancelJobsForUser(int userHandle) {
        List<JobStatus> jobsForUser;
        synchronized (this.mJobs) {
            jobsForUser = this.mJobs.getJobsByUser(userHandle);
        }
        for (int i = 0; i < jobsForUser.size(); i++) {
            JobStatus toRemove = jobsForUser.get(i);
            cancelJobImpl(toRemove);
        }
    }

    public void cancelJobsForUid(int uid) {
        List<JobStatus> jobsForUid;
        synchronized (this.mJobs) {
            jobsForUid = this.mJobs.getJobsByUid(uid);
        }
        for (int i = 0; i < jobsForUid.size(); i++) {
            JobStatus toRemove = jobsForUid.get(i);
            cancelJobImpl(toRemove);
        }
    }

    public void cancelJob(int uid, int jobId) {
        JobStatus toCancel;
        synchronized (this.mJobs) {
            toCancel = this.mJobs.getJobByUidAndJobId(uid, jobId);
        }
        if (toCancel != null) {
            cancelJobImpl(toCancel);
        }
    }

    private void cancelJobImpl(JobStatus cancelled) {
        stopTrackingJob(cancelled);
        synchronized (this.mJobs) {
            this.mPendingJobs.remove(cancelled);
            stopJobOnServiceContextLocked(cancelled);
        }
    }

    public JobSchedulerService(Context context) {
        super(context);
        this.mActiveServices = new ArrayList();
        this.mPendingJobs = new ArrayList<>();
        this.mStartedUsers = new ArrayList<>();
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Slog.d(JobSchedulerService.TAG, "Receieved: " + intent.getAction());
                if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                    if (!intent.getBooleanExtra("android.intent.extra.REPLACING", JobSchedulerService.DEBUG)) {
                        int uidRemoved = intent.getIntExtra("android.intent.extra.UID", -1);
                        JobSchedulerService.this.cancelJobsForUid(uidRemoved);
                        return;
                    }
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    JobSchedulerService.this.cancelJobsForUser(userId);
                }
            }
        };
        this.mControllers = new ArrayList();
        this.mControllers.add(ConnectivityController.get(this));
        this.mControllers.add(TimeController.get(this));
        this.mControllers.add(IdleController.get(this));
        this.mControllers.add(BatteryController.get(this));
        this.mHandler = new JobHandler(context.getMainLooper());
        this.mJobSchedulerStub = new JobSchedulerStub();
        this.mJobs = JobStore.initAndGet(this);
    }

    @Override
    public void onStart() {
        publishBinderService("jobscheduler", this.mJobSchedulerStub);
    }

    @Override
    public void onBootPhase(int phase) {
        if (500 == phase) {
            IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_REMOVED");
            filter.addDataScheme("package");
            getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
            IntentFilter userFilter = new IntentFilter("android.intent.action.USER_REMOVED");
            getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, userFilter, null, null);
            return;
        }
        if (phase == 600) {
            synchronized (this.mJobs) {
                this.mReadyToRock = true;
                this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                for (int i = 0; i < 3; i++) {
                    this.mActiveServices.add(new JobServiceContext(this, this.mBatteryStats, getContext().getMainLooper()));
                }
                ArraySet<JobStatus> jobs = this.mJobs.getJobs();
                for (int i2 = 0; i2 < jobs.size(); i2++) {
                    JobStatus job = jobs.valueAt(i2);
                    for (int controller = 0; controller < this.mControllers.size(); controller++) {
                        this.mControllers.get(controller).maybeStartTrackingJob(job);
                    }
                }
                this.mHandler.obtainMessage(1).sendToTarget();
            }
        }
    }

    private void startTrackingJob(JobStatus jobStatus) {
        boolean update;
        boolean rocking;
        synchronized (this.mJobs) {
            update = this.mJobs.add(jobStatus);
            rocking = this.mReadyToRock;
        }
        if (rocking) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                StateController controller = this.mControllers.get(i);
                if (update) {
                    controller.maybeStopTrackingJob(jobStatus);
                }
                controller.maybeStartTrackingJob(jobStatus);
            }
        }
    }

    private boolean stopTrackingJob(JobStatus jobStatus) {
        boolean removed;
        boolean rocking;
        synchronized (this.mJobs) {
            removed = this.mJobs.remove(jobStatus);
            rocking = this.mReadyToRock;
        }
        if (removed && rocking) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                StateController controller = this.mControllers.get(i);
                controller.maybeStopTrackingJob(jobStatus);
            }
        }
        return removed;
    }

    private boolean stopJobOnServiceContextLocked(JobStatus job) {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext jsc = this.mActiveServices.get(i);
            JobStatus executing = jsc.getRunningJob();
            if (executing != null && executing.matches(job.getUid(), job.getJobId())) {
                jsc.cancelExecutingJob();
                return true;
            }
        }
        return DEBUG;
    }

    private boolean isCurrentlyActiveLocked(JobStatus job) {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext serviceContext = this.mActiveServices.get(i);
            JobStatus running = serviceContext.getRunningJob();
            if (running != null && running.matches(job.getUid(), job.getJobId())) {
                return true;
            }
        }
        return DEBUG;
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
                delayMillis = (long) Math.scalb(initialBackoffMillis, backoffAttempts - 1);
                break;
        }
        return new JobStatus(failureToReschedule, elapsedNowMillis + Math.min(delayMillis, 18000000L), JobStatus.NO_LATEST_RUNTIME, backoffAttempts);
    }

    private JobStatus getRescheduleJobForPeriodic(JobStatus periodicToReschedule) {
        long elapsedNow = SystemClock.elapsedRealtime();
        long runEarly = Math.max(periodicToReschedule.getLatestRunTimeElapsed() - elapsedNow, 0L);
        long newEarliestRunTimeElapsed = elapsedNow + runEarly;
        long period = periodicToReschedule.getJob().getIntervalMillis();
        long newLatestRuntimeElapsed = newEarliestRunTimeElapsed + period;
        return new JobStatus(periodicToReschedule, newEarliestRunTimeElapsed, newLatestRuntimeElapsed, 0);
    }

    @Override
    public void onJobCompleted(JobStatus jobStatus, boolean needsReschedule) {
        if (stopTrackingJob(jobStatus)) {
            if (needsReschedule) {
                JobStatus rescheduled = getRescheduleJobForFailure(jobStatus);
                startTrackingJob(rescheduled);
            } else if (jobStatus.getJob().isPeriodic()) {
                JobStatus rescheduledPeriodic = getRescheduleJobForPeriodic(jobStatus);
                startTrackingJob(rescheduledPeriodic);
            }
            this.mHandler.obtainMessage(1).sendToTarget();
        }
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
        public JobHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            synchronized (JobSchedulerService.this.mJobs) {
                if (JobSchedulerService.this.mReadyToRock) {
                    switch (message.what) {
                        case 0:
                            synchronized (JobSchedulerService.this.mJobs) {
                                JobStatus runNow = (JobStatus) message.obj;
                                if (runNow != null && !JobSchedulerService.this.mPendingJobs.contains(runNow) && JobSchedulerService.this.mJobs.containsJob(runNow)) {
                                    JobSchedulerService.this.mPendingJobs.add(runNow);
                                }
                                queueReadyJobsForExecutionLockedH();
                                break;
                            }
                            break;
                        case 1:
                            synchronized (JobSchedulerService.this.mJobs) {
                                maybeQueueReadyJobsForExecutionLockedH();
                                break;
                            }
                            break;
                    }
                    maybeRunPendingJobsH();
                    removeMessages(1);
                }
            }
        }

        private void queueReadyJobsForExecutionLockedH() {
            ArraySet<JobStatus> jobs = JobSchedulerService.this.mJobs.getJobs();
            for (int i = 0; i < jobs.size(); i++) {
                JobStatus job = jobs.valueAt(i);
                if (isReadyToBeExecutedLocked(job)) {
                    JobSchedulerService.this.mPendingJobs.add(job);
                } else if (isReadyToBeCancelledLocked(job)) {
                    JobSchedulerService.this.stopJobOnServiceContextLocked(job);
                }
            }
        }

        private void maybeQueueReadyJobsForExecutionLockedH() {
            int chargingCount = 0;
            int idleCount = 0;
            int backoffCount = 0;
            int connectivityCount = 0;
            List<JobStatus> runnableJobs = new ArrayList<>();
            ArraySet<JobStatus> jobs = JobSchedulerService.this.mJobs.getJobs();
            for (int i = 0; i < jobs.size(); i++) {
                JobStatus job = jobs.valueAt(i);
                if (isReadyToBeExecutedLocked(job)) {
                    if (job.getNumFailures() > 0) {
                        backoffCount++;
                    }
                    if (job.hasIdleConstraint()) {
                        idleCount++;
                    }
                    if (job.hasConnectivityConstraint() || job.hasUnmeteredConstraint()) {
                        connectivityCount++;
                    }
                    if (job.hasChargingConstraint()) {
                        chargingCount++;
                    }
                    runnableJobs.add(job);
                } else if (isReadyToBeCancelledLocked(job)) {
                    JobSchedulerService.this.stopJobOnServiceContextLocked(job);
                }
            }
            if (backoffCount > 0 || idleCount >= 1 || connectivityCount >= 2 || chargingCount >= 1 || runnableJobs.size() >= 2) {
                for (int i2 = 0; i2 < runnableJobs.size(); i2++) {
                    JobSchedulerService.this.mPendingJobs.add(runnableJobs.get(i2));
                }
            }
        }

        private boolean isReadyToBeExecutedLocked(JobStatus job) {
            boolean jobReady = job.isReady();
            boolean jobPending = JobSchedulerService.this.mPendingJobs.contains(job);
            boolean jobActive = JobSchedulerService.this.isCurrentlyActiveLocked(job);
            boolean userRunning = JobSchedulerService.this.mStartedUsers.contains(Integer.valueOf(job.getUserId()));
            if (!userRunning || !jobReady || jobPending || jobActive) {
                return JobSchedulerService.DEBUG;
            }
            return true;
        }

        private boolean isReadyToBeCancelledLocked(JobStatus job) {
            if (job.isReady() || !JobSchedulerService.this.isCurrentlyActiveLocked(job)) {
                return JobSchedulerService.DEBUG;
            }
            return true;
        }

        private void maybeRunPendingJobsH() {
            synchronized (JobSchedulerService.this.mJobs) {
                Iterator<JobStatus> it = JobSchedulerService.this.mPendingJobs.iterator();
                while (it.hasNext()) {
                    JobStatus nextPending = it.next();
                    JobServiceContext availableContext = null;
                    int i = 0;
                    while (true) {
                        if (i < JobSchedulerService.this.mActiveServices.size()) {
                            JobServiceContext jsc = JobSchedulerService.this.mActiveServices.get(i);
                            JobStatus running = jsc.getRunningJob();
                            if (running != null && running.matches(nextPending.getUid(), nextPending.getJobId())) {
                                availableContext = null;
                                break;
                            } else {
                                if (jsc.isAvailable()) {
                                    availableContext = jsc;
                                }
                                i++;
                            }
                        } else {
                            break;
                        }
                    }
                    if (availableContext != null) {
                        if (!availableContext.executeRunnableJob(nextPending)) {
                            JobSchedulerService.this.mJobs.remove(nextPending);
                        }
                        it.remove();
                    }
                }
            }
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
                ServiceInfo si = pm.getServiceInfo(service, 0, UserHandle.getUserId(uid));
                if (si == null) {
                    throw new IllegalArgumentException("No such service " + service);
                }
                if (si.applicationInfo.uid != uid) {
                    throw new IllegalArgumentException("uid " + uid + " cannot schedule job in " + service.getPackageName());
                }
                if (!"android.permission.BIND_JOB_SERVICE".equals(si.permission)) {
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
                    canPersist = result == 0 ? true : JobSchedulerService.DEBUG;
                    this.mPersistCache.put(uid, Boolean.valueOf(canPersist));
                }
            }
            return canPersist;
        }

        public int schedule(JobInfo job) throws RemoteException {
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

        public List<JobInfo> getAllPendingJobs() throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.getPendingJobs(uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void cancelAll() throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.cancelJobsForUid(uid);
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
                JobSchedulerService.this.dumpInternal(pw);
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }
    }

    void dumpInternal(PrintWriter pw) {
        long now = SystemClock.elapsedRealtime();
        synchronized (this.mJobs) {
            pw.print("Started users: ");
            for (int i = 0; i < this.mStartedUsers.size(); i++) {
                pw.print("u" + this.mStartedUsers.get(i) + " ");
            }
            pw.println();
            pw.println("Registered jobs:");
            if (this.mJobs.size() > 0) {
                ArraySet<JobStatus> jobs = this.mJobs.getJobs();
                for (int i2 = 0; i2 < jobs.size(); i2++) {
                    JobStatus job = jobs.valueAt(i2);
                    job.dump(pw, "  ");
                }
            } else {
                pw.println("  None.");
            }
            for (int i3 = 0; i3 < this.mControllers.size(); i3++) {
                pw.println();
                this.mControllers.get(i3).dumpControllerState(pw);
            }
            pw.println();
            pw.println("Pending:");
            for (int i4 = 0; i4 < this.mPendingJobs.size(); i4++) {
                pw.println(this.mPendingJobs.get(i4).hashCode());
            }
            pw.println();
            pw.println("Active jobs:");
            for (int i5 = 0; i5 < this.mActiveServices.size(); i5++) {
                JobServiceContext jsc = this.mActiveServices.get(i5);
                if (!jsc.isAvailable()) {
                    long timeout = jsc.getTimeoutElapsed();
                    pw.print("Running for: ");
                    pw.print((now - jsc.getExecutionStartTimeElapsed()) / 1000);
                    pw.print("s timeout=");
                    pw.print(timeout);
                    pw.print(" fromnow=");
                    pw.println(timeout - now);
                    jsc.getRunningJob().dump(pw, "  ");
                }
            }
            pw.println();
            pw.print("mReadyToRock=");
            pw.println(this.mReadyToRock);
        }
        pw.println();
    }
}
