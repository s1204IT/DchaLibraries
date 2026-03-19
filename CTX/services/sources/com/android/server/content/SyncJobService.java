package com.android.server.content;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import com.android.internal.annotations.GuardedBy;
import com.android.server.slice.SliceClientPermissions;

public class SyncJobService extends JobService {
    public static final String EXTRA_MESSENGER = "messenger";
    private static final String TAG = "SyncManager";
    private Messenger mMessenger;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final SparseArray<JobParameters> mJobParamsMap = new SparseArray<>();

    @GuardedBy("mLock")
    private final SparseBooleanArray mStartedSyncs = new SparseBooleanArray();

    @GuardedBy("mLock")
    private final SparseLongArray mJobStartUptimes = new SparseLongArray();
    private final SyncLogger mLogger = SyncLogger.getInstance();

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        this.mMessenger = (Messenger) intent.getParcelableExtra(EXTRA_MESSENGER);
        Message messageObtain = Message.obtain();
        messageObtain.what = 7;
        messageObtain.obj = this;
        sendMessage(messageObtain);
        return 2;
    }

    private void sendMessage(Message message) {
        if (this.mMessenger == null) {
            Slog.e("SyncManager", "Messenger not initialized.");
            return;
        }
        try {
            this.mMessenger.send(message);
        } catch (RemoteException e) {
            Slog.e("SyncManager", e.toString());
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        this.mLogger.purgeOldLogs();
        boolean zIsLoggable = Log.isLoggable("SyncManager", 2);
        synchronized (this.mLock) {
            int jobId = jobParameters.getJobId();
            this.mJobParamsMap.put(jobId, jobParameters);
            this.mStartedSyncs.delete(jobId);
            this.mJobStartUptimes.put(jobId, SystemClock.uptimeMillis());
        }
        Message messageObtain = Message.obtain();
        messageObtain.what = 10;
        SyncOperation syncOperationMaybeCreateFromJobExtras = SyncOperation.maybeCreateFromJobExtras(jobParameters.getExtras());
        this.mLogger.log("onStartJob() jobid=", Integer.valueOf(jobParameters.getJobId()), " op=", syncOperationMaybeCreateFromJobExtras);
        if (syncOperationMaybeCreateFromJobExtras == null) {
            Slog.e("SyncManager", "Got invalid job " + jobParameters.getJobId());
            return false;
        }
        if (zIsLoggable) {
            Slog.v("SyncManager", "Got start job message " + syncOperationMaybeCreateFromJobExtras.target);
        }
        messageObtain.obj = syncOperationMaybeCreateFromJobExtras;
        sendMessage(messageObtain);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "onStopJob called " + jobParameters.getJobId() + ", reason: " + jobParameters.getStopReason());
        }
        boolean z = SyncManager.readyToSync();
        this.mLogger.log("onStopJob() ", this.mLogger.jobParametersToString(jobParameters), " readyToSync=", Boolean.valueOf(z));
        synchronized (this.mLock) {
            int jobId = jobParameters.getJobId();
            this.mJobParamsMap.remove(jobId);
            long j = this.mJobStartUptimes.get(jobId);
            long jUptimeMillis = SystemClock.uptimeMillis();
            long j2 = jUptimeMillis - j;
            if (j == 0) {
                wtf("Job " + jobId + " start uptime not found:  params=" + jobParametersToString(jobParameters));
            } else if (j2 > 60000 && z && !this.mStartedSyncs.get(jobId)) {
                wtf("Job " + jobId + " didn't start:  startUptime=" + j + " nowUptime=" + jUptimeMillis + " params=" + jobParametersToString(jobParameters));
            }
            this.mStartedSyncs.delete(jobId);
            this.mJobStartUptimes.delete(jobId);
        }
        Message messageObtain = Message.obtain();
        messageObtain.what = 11;
        messageObtain.obj = SyncOperation.maybeCreateFromJobExtras(jobParameters.getExtras());
        if (messageObtain.obj == null) {
            return false;
        }
        messageObtain.arg1 = jobParameters.getStopReason() != 0 ? 1 : 0;
        messageObtain.arg2 = jobParameters.getStopReason() != 3 ? 0 : 1;
        sendMessage(messageObtain);
        return false;
    }

    public void callJobFinished(int i, boolean z, String str) {
        synchronized (this.mLock) {
            JobParameters jobParameters = this.mJobParamsMap.get(i);
            this.mLogger.log("callJobFinished()", " jobid=", Integer.valueOf(i), " needsReschedule=", Boolean.valueOf(z), " ", this.mLogger.jobParametersToString(jobParameters), " why=", str);
            if (jobParameters != null) {
                jobFinished(jobParameters, z);
                this.mJobParamsMap.remove(i);
            } else {
                Slog.e("SyncManager", "Job params not found for " + String.valueOf(i));
            }
        }
    }

    public void markSyncStarted(int i) {
        synchronized (this.mLock) {
            this.mStartedSyncs.put(i, true);
        }
    }

    public static String jobParametersToString(JobParameters jobParameters) {
        if (jobParameters == null) {
            return "job:null";
        }
        return "job:#" + jobParameters.getJobId() + ":sr=[" + jobParameters.getStopReason() + SliceClientPermissions.SliceAuthority.DELIMITER + jobParameters.getDebugStopReason() + "]:" + SyncOperation.maybeCreateFromJobExtras(jobParameters.getExtras());
    }

    private void wtf(String str) {
        this.mLogger.log(str);
        Slog.wtf("SyncManager", str);
    }
}
