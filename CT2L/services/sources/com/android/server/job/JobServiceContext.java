package com.android.server.job;

import android.app.ActivityManager;
import android.app.job.IJobCallback;
import android.app.job.IJobService;
import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.server.job.controllers.JobStatus;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobServiceContext extends IJobCallback.Stub implements ServiceConnection {
    private static final boolean DEBUG = false;
    private static final long EXECUTING_TIMESLICE_MILLIS = 60000;
    private static final int MSG_CALLBACK = 1;
    private static final int MSG_CANCEL = 3;
    private static final int MSG_SERVICE_BOUND = 2;
    private static final int MSG_SHUTDOWN_EXECUTION = 4;
    private static final int MSG_TIMEOUT = 0;
    private static final long OP_TIMEOUT_MILLIS = 8000;
    private static final String TAG = "JobServiceContext";
    static final int VERB_BINDING = 0;
    static final int VERB_EXECUTING = 2;
    static final int VERB_STARTING = 1;
    static final int VERB_STOPPING = 3;
    private static final String[] VERB_STRINGS;
    private static final int defaultMaxActiveJobsPerService;

    @GuardedBy("mLock")
    private boolean mAvailable;
    private final IBatteryStats mBatteryStats;
    private final Handler mCallbackHandler;
    private AtomicBoolean mCancelled;
    private final JobCompletedListener mCompletedListener;
    private final Context mContext;
    private long mExecutionStartTimeElapsed;
    private final Object mLock;
    private JobParameters mParams;
    private JobStatus mRunningJob;
    private long mTimeoutElapsed;
    int mVerb;
    private PowerManager.WakeLock mWakeLock;
    IJobService service;

    static {
        defaultMaxActiveJobsPerService = ActivityManager.isLowRamDeviceStatic() ? 1 : 3;
        VERB_STRINGS = new String[]{"VERB_BINDING", "VERB_STARTING", "VERB_EXECUTING", "VERB_STOPPING"};
    }

    JobServiceContext(JobSchedulerService service, IBatteryStats batteryStats, Looper looper) {
        this(service.getContext(), batteryStats, service, looper);
    }

    JobServiceContext(Context context, IBatteryStats batteryStats, JobCompletedListener completedListener, Looper looper) {
        this.mCancelled = new AtomicBoolean();
        this.mLock = new Object();
        this.mContext = context;
        this.mBatteryStats = batteryStats;
        this.mCallbackHandler = new JobServiceHandler(looper);
        this.mCompletedListener = completedListener;
        this.mAvailable = true;
    }

    boolean executeRunnableJob(JobStatus job) {
        synchronized (this.mLock) {
            if (!this.mAvailable) {
                Slog.e(TAG, "Starting new runnable but context is unavailable > Error.");
                return DEBUG;
            }
            this.mRunningJob = job;
            this.mParams = new JobParameters(this, job.getJobId(), job.getExtras(), !job.isConstraintsSatisfied());
            this.mExecutionStartTimeElapsed = SystemClock.elapsedRealtime();
            this.mVerb = 0;
            scheduleOpTimeOut();
            Intent intent = new Intent().setComponent(job.getServiceComponent());
            boolean binding = this.mContext.bindServiceAsUser(intent, this, 5, new UserHandle(job.getUserId()));
            if (!binding) {
                this.mRunningJob = null;
                this.mParams = null;
                this.mExecutionStartTimeElapsed = 0L;
                removeOpTimeOut();
                return DEBUG;
            }
            try {
                this.mBatteryStats.noteJobStart(job.getName(), job.getUid());
            } catch (RemoteException e) {
            }
            this.mAvailable = DEBUG;
            return true;
        }
    }

    JobStatus getRunningJob() {
        JobStatus jobStatus;
        synchronized (this.mLock) {
            jobStatus = this.mRunningJob;
        }
        return jobStatus;
    }

    void cancelExecutingJob() {
        this.mCallbackHandler.obtainMessage(3).sendToTarget();
    }

    boolean isAvailable() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mAvailable;
        }
        return z;
    }

    long getExecutionStartTimeElapsed() {
        return this.mExecutionStartTimeElapsed;
    }

    long getTimeoutElapsed() {
        return this.mTimeoutElapsed;
    }

    public void jobFinished(int jobId, boolean reschedule) {
        if (!verifyCallingUid()) {
            return;
        }
        this.mCallbackHandler.obtainMessage(1, jobId, reschedule ? 1 : 0).sendToTarget();
    }

    public void acknowledgeStopMessage(int jobId, boolean reschedule) {
        if (!verifyCallingUid()) {
            return;
        }
        this.mCallbackHandler.obtainMessage(1, jobId, reschedule ? 1 : 0).sendToTarget();
    }

    public void acknowledgeStartMessage(int jobId, boolean ongoing) {
        if (!verifyCallingUid()) {
            return;
        }
        this.mCallbackHandler.obtainMessage(1, jobId, ongoing ? 1 : 0).sendToTarget();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (!name.equals(this.mRunningJob.getServiceComponent())) {
            this.mCallbackHandler.obtainMessage(4).sendToTarget();
            return;
        }
        this.service = IJobService.Stub.asInterface(service);
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, this.mRunningJob.getTag());
        this.mWakeLock.setWorkSource(new WorkSource(this.mRunningJob.getUid()));
        this.mWakeLock.setReferenceCounted(DEBUG);
        this.mWakeLock.acquire();
        this.mCallbackHandler.obtainMessage(2).sendToTarget();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        this.mCallbackHandler.obtainMessage(4).sendToTarget();
    }

    private boolean verifyCallingUid() {
        if (this.mRunningJob == null || Binder.getCallingUid() != this.mRunningJob.getUid()) {
            return DEBUG;
        }
        return true;
    }

    private class JobServiceHandler extends Handler {
        JobServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleOpTimeoutH();
                    break;
                case 1:
                    JobServiceContext.this.removeOpTimeOut();
                    if (JobServiceContext.this.mVerb == 1) {
                        boolean workOngoing = message.arg2 == 1;
                        handleStartedH(workOngoing);
                    } else if (JobServiceContext.this.mVerb == 2 || JobServiceContext.this.mVerb == 3) {
                        boolean reschedule = message.arg2 == 1;
                        handleFinishedH(reschedule);
                    }
                    break;
                case 2:
                    JobServiceContext.this.removeOpTimeOut();
                    handleServiceBoundH();
                    break;
                case 3:
                    handleCancelH();
                    break;
                case 4:
                    closeAndCleanupJobH(true);
                    break;
                default:
                    Slog.e(JobServiceContext.TAG, "Unrecognised message: " + message);
                    break;
            }
        }

        private void handleServiceBoundH() {
            if (JobServiceContext.this.mVerb == 0) {
                if (JobServiceContext.this.mCancelled.get()) {
                    closeAndCleanupJobH(true);
                    return;
                }
                try {
                    JobServiceContext.this.mVerb = 1;
                    JobServiceContext.this.scheduleOpTimeOut();
                    JobServiceContext.this.service.startJob(JobServiceContext.this.mParams);
                    return;
                } catch (RemoteException e) {
                    Slog.e(JobServiceContext.TAG, "Error sending onStart message to '" + JobServiceContext.this.mRunningJob.getServiceComponent().getShortClassName() + "' ", e);
                    return;
                }
            }
            Slog.e(JobServiceContext.TAG, "Sending onStartJob for a job that isn't pending. " + JobServiceContext.VERB_STRINGS[JobServiceContext.this.mVerb]);
            closeAndCleanupJobH(JobServiceContext.DEBUG);
        }

        private void handleStartedH(boolean workOngoing) {
            switch (JobServiceContext.this.mVerb) {
                case 1:
                    JobServiceContext.this.mVerb = 2;
                    if (workOngoing) {
                        if (!JobServiceContext.this.mCancelled.get()) {
                            JobServiceContext.this.scheduleOpTimeOut();
                        } else {
                            handleCancelH();
                        }
                    } else {
                        handleFinishedH(JobServiceContext.DEBUG);
                    }
                    break;
                default:
                    Slog.e(JobServiceContext.TAG, "Handling started job but job wasn't starting! Was " + JobServiceContext.VERB_STRINGS[JobServiceContext.this.mVerb] + ".");
                    break;
            }
        }

        private void handleFinishedH(boolean reschedule) {
            switch (JobServiceContext.this.mVerb) {
                case 2:
                case 3:
                    closeAndCleanupJobH(reschedule);
                    break;
                default:
                    Slog.e(JobServiceContext.TAG, "Got an execution complete message for a job that wasn't beingexecuted. Was " + JobServiceContext.VERB_STRINGS[JobServiceContext.this.mVerb] + ".");
                    break;
            }
        }

        private void handleCancelH() {
            if (JobServiceContext.this.mRunningJob != null) {
                switch (JobServiceContext.this.mVerb) {
                    case 0:
                    case 1:
                        JobServiceContext.this.mCancelled.set(true);
                        break;
                    case 2:
                        if (!hasMessages(1)) {
                            sendStopMessageH();
                        }
                        break;
                    case 3:
                        break;
                    default:
                        Slog.e(JobServiceContext.TAG, "Cancelling a job without a valid verb: " + JobServiceContext.this.mVerb);
                        break;
                }
            }
        }

        private void handleOpTimeoutH() {
            switch (JobServiceContext.this.mVerb) {
                case 0:
                    Slog.e(JobServiceContext.TAG, "Time-out while trying to bind " + JobServiceContext.this.mRunningJob.toShortString() + ", dropping.");
                    closeAndCleanupJobH(JobServiceContext.DEBUG);
                    break;
                case 1:
                    Slog.e(JobServiceContext.TAG, "No response from client for onStartJob '" + JobServiceContext.this.mRunningJob.toShortString());
                    closeAndCleanupJobH(JobServiceContext.DEBUG);
                    break;
                case 2:
                    Slog.i(JobServiceContext.TAG, "Client timed out while executing (no jobFinished received). sending onStop. " + JobServiceContext.this.mRunningJob.toShortString());
                    sendStopMessageH();
                    break;
                case 3:
                    Slog.e(JobServiceContext.TAG, "No response from client for onStopJob, '" + JobServiceContext.this.mRunningJob.toShortString());
                    closeAndCleanupJobH(true);
                    break;
                default:
                    Slog.e(JobServiceContext.TAG, "Handling timeout for an invalid job state: " + JobServiceContext.this.mRunningJob.toShortString() + ", dropping.");
                    closeAndCleanupJobH(JobServiceContext.DEBUG);
                    break;
            }
        }

        private void sendStopMessageH() {
            JobServiceContext.this.removeOpTimeOut();
            if (JobServiceContext.this.mVerb != 2) {
                Slog.e(JobServiceContext.TAG, "Sending onStopJob for a job that isn't started. " + JobServiceContext.this.mRunningJob);
                closeAndCleanupJobH(JobServiceContext.DEBUG);
                return;
            }
            try {
                JobServiceContext.this.mVerb = 3;
                JobServiceContext.this.scheduleOpTimeOut();
                JobServiceContext.this.service.stopJob(JobServiceContext.this.mParams);
            } catch (RemoteException e) {
                Slog.e(JobServiceContext.TAG, "Error sending onStopJob to client.", e);
                closeAndCleanupJobH(JobServiceContext.DEBUG);
            }
        }

        private void closeAndCleanupJobH(boolean reschedule) {
            JobStatus completedJob = JobServiceContext.this.mRunningJob;
            synchronized (JobServiceContext.this.mLock) {
                try {
                    JobServiceContext.this.mBatteryStats.noteJobFinish(JobServiceContext.this.mRunningJob.getName(), JobServiceContext.this.mRunningJob.getUid());
                } catch (RemoteException e) {
                }
                if (JobServiceContext.this.mWakeLock != null) {
                    JobServiceContext.this.mWakeLock.release();
                }
                JobServiceContext.this.mContext.unbindService(JobServiceContext.this);
                JobServiceContext.this.mWakeLock = null;
                JobServiceContext.this.mRunningJob = null;
                JobServiceContext.this.mParams = null;
                JobServiceContext.this.mVerb = -1;
                JobServiceContext.this.mCancelled.set(JobServiceContext.DEBUG);
                JobServiceContext.this.service = null;
                JobServiceContext.this.mAvailable = true;
            }
            JobServiceContext.this.removeOpTimeOut();
            removeMessages(1);
            removeMessages(2);
            removeMessages(3);
            removeMessages(4);
            JobServiceContext.this.mCompletedListener.onJobCompleted(completedJob, reschedule);
        }
    }

    private void scheduleOpTimeOut() {
        removeOpTimeOut();
        long timeoutMillis = this.mVerb == 2 ? EXECUTING_TIMESLICE_MILLIS : OP_TIMEOUT_MILLIS;
        Message m = this.mCallbackHandler.obtainMessage(0);
        this.mCallbackHandler.sendMessageDelayed(m, timeoutMillis);
        this.mTimeoutElapsed = SystemClock.elapsedRealtime() + timeoutMillis;
    }

    private void removeOpTimeOut() {
        this.mCallbackHandler.removeMessages(0);
    }
}
