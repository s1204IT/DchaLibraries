package com.android.server.job;

import android.app.ActivityManager;
import android.app.job.IJobCallback;
import android.app.job.IJobService;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobWorkItem;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;

public final class JobServiceContext implements ServiceConnection {
    public static final long EXECUTING_TIMESLICE_MILLIS = 600000;
    private static final int MSG_TIMEOUT = 0;
    public static final int NO_PREFERRED_UID = -1;
    private static final long OP_BIND_TIMEOUT_MILLIS = 18000;
    private static final long OP_TIMEOUT_MILLIS = 8000;
    private static final String TAG = "JobServiceContext";
    static final int VERB_BINDING = 0;
    static final int VERB_EXECUTING = 2;
    static final int VERB_FINISHED = 4;
    static final int VERB_STARTING = 1;
    static final int VERB_STOPPING = 3;

    @GuardedBy("mLock")
    private boolean mAvailable;
    private final IBatteryStats mBatteryStats;
    private final Handler mCallbackHandler;
    private boolean mCancelled;
    private final JobCompletedListener mCompletedListener;
    private final Context mContext;
    private long mExecutionStartTimeElapsed;
    private final JobPackageTracker mJobPackageTracker;
    private final Object mLock;
    private JobParameters mParams;
    private int mPreferredUid;
    private JobCallback mRunningCallback;
    private JobStatus mRunningJob;
    public String mStoppedReason;
    public long mStoppedTime;
    private long mTimeoutElapsed;

    @VisibleForTesting
    int mVerb;
    private PowerManager.WakeLock mWakeLock;
    IJobService service;
    private static final boolean DEBUG = JobSchedulerService.DEBUG;
    private static final boolean DEBUG_STANDBY = JobSchedulerService.DEBUG_STANDBY;
    private static final String[] VERB_STRINGS = {"VERB_BINDING", "VERB_STARTING", "VERB_EXECUTING", "VERB_STOPPING", "VERB_FINISHED"};

    final class JobCallback extends IJobCallback.Stub {
        public String mStoppedReason;
        public long mStoppedTime;

        JobCallback() {
        }

        public void acknowledgeStartMessage(int i, boolean z) {
            JobServiceContext.this.doAcknowledgeStartMessage(this, i, z);
        }

        public void acknowledgeStopMessage(int i, boolean z) {
            JobServiceContext.this.doAcknowledgeStopMessage(this, i, z);
        }

        public JobWorkItem dequeueWork(int i) {
            return JobServiceContext.this.doDequeueWork(this, i);
        }

        public boolean completeWork(int i, int i2) {
            return JobServiceContext.this.doCompleteWork(this, i, i2);
        }

        public void jobFinished(int i, boolean z) {
            JobServiceContext.this.doJobFinished(this, i, z);
        }
    }

    JobServiceContext(JobSchedulerService jobSchedulerService, IBatteryStats iBatteryStats, JobPackageTracker jobPackageTracker, Looper looper) {
        this(jobSchedulerService.getContext(), jobSchedulerService.getLock(), iBatteryStats, jobPackageTracker, jobSchedulerService, looper);
    }

    @VisibleForTesting
    JobServiceContext(Context context, Object obj, IBatteryStats iBatteryStats, JobPackageTracker jobPackageTracker, JobCompletedListener jobCompletedListener, Looper looper) {
        this.mContext = context;
        this.mLock = obj;
        this.mBatteryStats = iBatteryStats;
        this.mJobPackageTracker = jobPackageTracker;
        this.mCallbackHandler = new JobServiceHandler(looper);
        this.mCompletedListener = jobCompletedListener;
        this.mAvailable = true;
        this.mVerb = 4;
        this.mPreferredUid = -1;
    }

    boolean executeRunnableJob(JobStatus jobStatus) {
        Uri[] uriArr;
        String[] strArr;
        synchronized (this.mLock) {
            if (!this.mAvailable) {
                Slog.e(TAG, "Starting new runnable but context is unavailable > Error.");
                return false;
            }
            this.mPreferredUid = -1;
            this.mRunningJob = jobStatus;
            this.mRunningCallback = new JobCallback();
            boolean z = jobStatus.hasDeadlineConstraint() && jobStatus.getLatestRunTimeElapsed() < JobSchedulerService.sElapsedRealtimeClock.millis();
            if (jobStatus.changedUris != null) {
                Uri[] uriArr2 = new Uri[jobStatus.changedUris.size()];
                jobStatus.changedUris.toArray(uriArr2);
                uriArr = uriArr2;
            } else {
                uriArr = null;
            }
            if (jobStatus.changedAuthorities != null) {
                strArr = new String[jobStatus.changedAuthorities.size()];
                jobStatus.changedAuthorities.toArray(strArr);
            } else {
                strArr = null;
            }
            JobInfo job = jobStatus.getJob();
            this.mParams = new JobParameters(this.mRunningCallback, jobStatus.getJobId(), job.getExtras(), job.getTransientExtras(), job.getClipData(), job.getClipGrantFlags(), z, uriArr, strArr, jobStatus.network);
            this.mExecutionStartTimeElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            long whenStandbyDeferred = jobStatus.getWhenStandbyDeferred();
            if (whenStandbyDeferred > 0) {
                long j = this.mExecutionStartTimeElapsed - whenStandbyDeferred;
                EventLog.writeEvent(EventLogTags.JOB_DEFERRED_EXECUTION, j);
                if (DEBUG_STANDBY) {
                    StringBuilder sb = new StringBuilder(128);
                    sb.append("Starting job deferred for standby by ");
                    TimeUtils.formatDuration(j, sb);
                    sb.append(" ms : ");
                    sb.append(jobStatus.toShortString());
                    Slog.v(TAG, sb.toString());
                }
            }
            jobStatus.clearPersistedUtcTimes();
            this.mVerb = 0;
            scheduleOpTimeOutLocked();
            if (this.mContext.bindServiceAsUser(new Intent().setComponent(jobStatus.getServiceComponent()), this, 5, new UserHandle(jobStatus.getUserId()))) {
                this.mJobPackageTracker.noteActive(jobStatus);
                try {
                    this.mBatteryStats.noteJobStart(jobStatus.getBatteryName(), jobStatus.getSourceUid());
                } catch (RemoteException e) {
                }
                String sourcePackageName = jobStatus.getSourcePackageName();
                int sourceUserId = jobStatus.getSourceUserId();
                ((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class)).setLastJobRunTime(sourcePackageName, sourceUserId, this.mExecutionStartTimeElapsed);
                ((JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class)).noteJobStart(sourcePackageName, sourceUserId);
                this.mAvailable = false;
                this.mStoppedReason = null;
                this.mStoppedTime = 0L;
                return true;
            }
            if (DEBUG) {
                Slog.d(TAG, jobStatus.getServiceComponent().getShortClassName() + " unavailable.");
            }
            this.mRunningJob = null;
            this.mRunningCallback = null;
            this.mParams = null;
            this.mExecutionStartTimeElapsed = 0L;
            this.mVerb = 4;
            removeOpTimeOutLocked();
            return false;
        }
    }

    JobStatus getRunningJobLocked() {
        return this.mRunningJob;
    }

    private String getRunningJobNameLocked() {
        return this.mRunningJob != null ? this.mRunningJob.toShortString() : "<null>";
    }

    @GuardedBy("mLock")
    void cancelExecutingJobLocked(int i, String str) {
        doCancelLocked(i, str);
    }

    @GuardedBy("mLock")
    void preemptExecutingJobLocked() {
        doCancelLocked(2, "cancelled due to preemption");
    }

    int getPreferredUid() {
        return this.mPreferredUid;
    }

    void clearPreferredUid() {
        this.mPreferredUid = -1;
    }

    long getExecutionStartTimeElapsed() {
        return this.mExecutionStartTimeElapsed;
    }

    long getTimeoutElapsed() {
        return this.mTimeoutElapsed;
    }

    @GuardedBy("mLock")
    boolean timeoutIfExecutingLocked(String str, int i, boolean z, int i2, String str2) {
        JobStatus runningJobLocked = getRunningJobLocked();
        if (runningJobLocked == null) {
            return false;
        }
        if (i == -1 || i == runningJobLocked.getUserId()) {
            if (str == null || str.equals(runningJobLocked.getSourcePackageName())) {
                if ((!z || i2 == runningJobLocked.getJobId()) && this.mVerb == 2) {
                    this.mParams.setStopReason(3, str2);
                    sendStopMessageLocked("force timeout from shell");
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    void doJobFinished(JobCallback jobCallback, int i, boolean z) {
        doCallback(jobCallback, z, "app called jobFinished");
    }

    void doAcknowledgeStopMessage(JobCallback jobCallback, int i, boolean z) {
        doCallback(jobCallback, z, null);
    }

    void doAcknowledgeStartMessage(JobCallback jobCallback, int i, boolean z) {
        doCallback(jobCallback, z, "finished start");
    }

    JobWorkItem doDequeueWork(JobCallback jobCallback, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                assertCallerLocked(jobCallback);
                if (this.mVerb != 3 && this.mVerb != 4) {
                    JobWorkItem jobWorkItemDequeueWorkLocked = this.mRunningJob.dequeueWorkLocked();
                    if (jobWorkItemDequeueWorkLocked == null && !this.mRunningJob.hasExecutingWorkLocked()) {
                        doCallbackLocked(false, "last work dequeued");
                    }
                    return jobWorkItemDequeueWorkLocked;
                }
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    boolean doCompleteWork(JobCallback jobCallback, int i, int i2) {
        boolean zCompleteWorkLocked;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                assertCallerLocked(jobCallback);
                zCompleteWorkLocked = this.mRunningJob.completeWorkLocked(ActivityManager.getService(), i2);
            }
            return zCompleteWorkLocked;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this.mLock) {
            JobStatus jobStatus = this.mRunningJob;
            if (jobStatus != null && componentName.equals(jobStatus.getServiceComponent())) {
                this.service = IJobService.Stub.asInterface(iBinder);
                PowerManager.WakeLock wakeLockNewWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, jobStatus.getTag());
                wakeLockNewWakeLock.setWorkSource(deriveWorkSource(jobStatus));
                wakeLockNewWakeLock.setReferenceCounted(false);
                wakeLockNewWakeLock.acquire();
                if (this.mWakeLock != null) {
                    Slog.w(TAG, "Bound new job " + jobStatus + " but live wakelock " + this.mWakeLock + " tag=" + this.mWakeLock.getTag());
                    this.mWakeLock.release();
                }
                this.mWakeLock = wakeLockNewWakeLock;
                doServiceBoundLocked();
                return;
            }
            closeAndCleanupJobLocked(true, "connected for different component");
        }
    }

    private WorkSource deriveWorkSource(JobStatus jobStatus) {
        int sourceUid = jobStatus.getSourceUid();
        if (WorkSource.isChainedBatteryAttributionEnabled(this.mContext)) {
            WorkSource workSource = new WorkSource();
            workSource.createWorkChain().addNode(sourceUid, (String) null).addNode(1000, JobSchedulerService.TAG);
            return workSource;
        }
        return new WorkSource(sourceUid);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        synchronized (this.mLock) {
            closeAndCleanupJobLocked(true, "unexpectedly disconnected");
        }
    }

    private boolean verifyCallerLocked(JobCallback jobCallback) {
        if (this.mRunningCallback != jobCallback) {
            if (DEBUG) {
                Slog.d(TAG, "Stale callback received, ignoring.");
                return false;
            }
            return false;
        }
        return true;
    }

    private void assertCallerLocked(JobCallback jobCallback) {
        if (!verifyCallerLocked(jobCallback)) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Caller no longer running");
            if (jobCallback.mStoppedReason != null) {
                sb.append(", last stopped ");
                TimeUtils.formatDuration(JobSchedulerService.sElapsedRealtimeClock.millis() - jobCallback.mStoppedTime, sb);
                sb.append(" because: ");
                sb.append(jobCallback.mStoppedReason);
            }
            throw new SecurityException(sb.toString());
        }
    }

    private class JobServiceHandler extends Handler {
        JobServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                synchronized (JobServiceContext.this.mLock) {
                    if (message.obj == JobServiceContext.this.mRunningCallback) {
                        JobServiceContext.this.handleOpTimeoutLocked();
                    } else {
                        JobCallback jobCallback = (JobCallback) message.obj;
                        StringBuilder sb = new StringBuilder(128);
                        sb.append("Ignoring timeout of no longer active job");
                        if (jobCallback.mStoppedReason != null) {
                            sb.append(", stopped ");
                            TimeUtils.formatDuration(JobSchedulerService.sElapsedRealtimeClock.millis() - jobCallback.mStoppedTime, sb);
                            sb.append(" because: ");
                            sb.append(jobCallback.mStoppedReason);
                        }
                        Slog.w(JobServiceContext.TAG, sb.toString());
                    }
                }
                return;
            }
            Slog.e(JobServiceContext.TAG, "Unrecognised message: " + message);
        }
    }

    @GuardedBy("mLock")
    void doServiceBoundLocked() {
        removeOpTimeOutLocked();
        handleServiceBoundLocked();
    }

    void doCallback(JobCallback jobCallback, boolean z, String str) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (!verifyCallerLocked(jobCallback)) {
                    return;
                }
                doCallbackLocked(z, str);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @GuardedBy("mLock")
    void doCallbackLocked(boolean z, String str) {
        if (DEBUG) {
            Slog.d(TAG, "doCallback of : " + this.mRunningJob + " v:" + VERB_STRINGS[this.mVerb]);
        }
        removeOpTimeOutLocked();
        if (this.mVerb == 1) {
            handleStartedLocked(z);
            return;
        }
        if (this.mVerb == 2 || this.mVerb == 3) {
            handleFinishedLocked(z, str);
        } else if (DEBUG) {
            Slog.d(TAG, "Unrecognised callback: " + this.mRunningJob);
        }
    }

    @GuardedBy("mLock")
    void doCancelLocked(int i, String str) {
        if (this.mVerb == 4) {
            if (DEBUG) {
                Slog.d(TAG, "Trying to process cancel for torn-down context, ignoring.");
            }
        } else {
            this.mParams.setStopReason(i, str);
            if (i == 2) {
                this.mPreferredUid = this.mRunningJob != null ? this.mRunningJob.getUid() : -1;
            }
            handleCancelLocked(str);
        }
    }

    @GuardedBy("mLock")
    private void handleServiceBoundLocked() {
        if (DEBUG) {
            Slog.d(TAG, "handleServiceBound for " + getRunningJobNameLocked());
        }
        if (this.mVerb != 0) {
            Slog.e(TAG, "Sending onStartJob for a job that isn't pending. " + VERB_STRINGS[this.mVerb]);
            closeAndCleanupJobLocked(false, "started job not pending");
            return;
        }
        if (this.mCancelled) {
            if (DEBUG) {
                Slog.d(TAG, "Job cancelled while waiting for bind to complete. " + this.mRunningJob);
            }
            closeAndCleanupJobLocked(true, "cancelled while waiting for bind");
            return;
        }
        try {
            this.mVerb = 1;
            scheduleOpTimeOutLocked();
            this.service.startJob(this.mParams);
        } catch (Exception e) {
            Slog.e(TAG, "Error sending onStart message to '" + this.mRunningJob.getServiceComponent().getShortClassName() + "' ", e);
        }
    }

    @GuardedBy("mLock")
    private void handleStartedLocked(boolean z) {
        if (this.mVerb == 1) {
            this.mVerb = 2;
            if (!z) {
                handleFinishedLocked(false, "onStartJob returned false");
                return;
            } else {
                if (this.mCancelled) {
                    if (DEBUG) {
                        Slog.d(TAG, "Job cancelled while waiting for onStartJob to complete.");
                    }
                    handleCancelLocked(null);
                    return;
                }
                scheduleOpTimeOutLocked();
                return;
            }
        }
        Slog.e(TAG, "Handling started job but job wasn't starting! Was " + VERB_STRINGS[this.mVerb] + ".");
    }

    @GuardedBy("mLock")
    private void handleFinishedLocked(boolean z, String str) {
        switch (this.mVerb) {
            case 2:
            case 3:
                closeAndCleanupJobLocked(z, str);
                break;
            default:
                Slog.e(TAG, "Got an execution complete message for a job that wasn't beingexecuted. Was " + VERB_STRINGS[this.mVerb] + ".");
                break;
        }
    }

    @GuardedBy("mLock")
    private void handleCancelLocked(String str) {
        if (JobSchedulerService.DEBUG) {
            Slog.d(TAG, "Handling cancel for: " + this.mRunningJob.getJobId() + " " + VERB_STRINGS[this.mVerb]);
        }
        switch (this.mVerb) {
            case 0:
            case 1:
                this.mCancelled = true;
                applyStoppedReasonLocked(str);
                break;
            case 2:
                sendStopMessageLocked(str);
                break;
            case 3:
                break;
            default:
                Slog.e(TAG, "Cancelling a job without a valid verb: " + this.mVerb);
                break;
        }
    }

    @GuardedBy("mLock")
    private void handleOpTimeoutLocked() {
        switch (this.mVerb) {
            case 0:
                Slog.w(TAG, "Time-out while trying to bind " + getRunningJobNameLocked() + ", dropping.");
                closeAndCleanupJobLocked(false, "timed out while binding");
                break;
            case 1:
                Slog.w(TAG, "No response from client for onStartJob " + getRunningJobNameLocked());
                closeAndCleanupJobLocked(false, "timed out while starting");
                break;
            case 2:
                Slog.i(TAG, "Client timed out while executing (no jobFinished received), sending onStop: " + getRunningJobNameLocked());
                this.mParams.setStopReason(3, "client timed out");
                sendStopMessageLocked("timeout while executing");
                break;
            case 3:
                Slog.w(TAG, "No response from client for onStopJob " + getRunningJobNameLocked());
                closeAndCleanupJobLocked(true, "timed out while stopping");
                break;
            default:
                Slog.e(TAG, "Handling timeout for an invalid job state: " + getRunningJobNameLocked() + ", dropping.");
                closeAndCleanupJobLocked(false, "invalid timeout");
                break;
        }
    }

    @GuardedBy("mLock")
    private void sendStopMessageLocked(String str) {
        removeOpTimeOutLocked();
        if (this.mVerb != 2) {
            Slog.e(TAG, "Sending onStopJob for a job that isn't started. " + this.mRunningJob);
            closeAndCleanupJobLocked(false, str);
            return;
        }
        try {
            applyStoppedReasonLocked(str);
            this.mVerb = 3;
            scheduleOpTimeOutLocked();
            this.service.stopJob(this.mParams);
        } catch (RemoteException e) {
            Slog.e(TAG, "Error sending onStopJob to client.", e);
            closeAndCleanupJobLocked(true, "host crashed when trying to stop");
        }
    }

    @GuardedBy("mLock")
    private void closeAndCleanupJobLocked(boolean z, String str) {
        if (this.mVerb == 4) {
            return;
        }
        applyStoppedReasonLocked(str);
        JobStatus jobStatus = this.mRunningJob;
        this.mJobPackageTracker.noteInactive(jobStatus, this.mParams.getStopReason(), str);
        try {
            this.mBatteryStats.noteJobFinish(this.mRunningJob.getBatteryName(), this.mRunningJob.getSourceUid(), this.mParams.getStopReason());
        } catch (RemoteException e) {
        }
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
        this.mContext.unbindService(this);
        this.mWakeLock = null;
        this.mRunningJob = null;
        this.mRunningCallback = null;
        this.mParams = null;
        this.mVerb = 4;
        this.mCancelled = false;
        this.service = null;
        this.mAvailable = true;
        removeOpTimeOutLocked();
        this.mCompletedListener.onJobCompletedLocked(jobStatus, z);
    }

    private void applyStoppedReasonLocked(String str) {
        if (str != null && this.mStoppedReason == null) {
            this.mStoppedReason = str;
            this.mStoppedTime = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (this.mRunningCallback != null) {
                this.mRunningCallback.mStoppedReason = this.mStoppedReason;
                this.mRunningCallback.mStoppedTime = this.mStoppedTime;
            }
        }
    }

    private void scheduleOpTimeOutLocked() {
        long j;
        removeOpTimeOutLocked();
        int i = this.mVerb;
        if (i == 0) {
            j = OP_BIND_TIMEOUT_MILLIS;
        } else if (i == 2) {
            j = 600000;
        } else {
            j = OP_TIMEOUT_MILLIS;
        }
        if (DEBUG) {
            Slog.d(TAG, "Scheduling time out for '" + this.mRunningJob.getServiceComponent().getShortClassName() + "' jId: " + this.mParams.getJobId() + ", in " + (j / 1000) + " s");
        }
        this.mCallbackHandler.sendMessageDelayed(this.mCallbackHandler.obtainMessage(0, this.mRunningCallback), j);
        this.mTimeoutElapsed = JobSchedulerService.sElapsedRealtimeClock.millis() + j;
    }

    private void removeOpTimeOutLocked() {
        this.mCallbackHandler.removeMessages(0);
    }
}
