package android.app.job;

import android.app.Service;
import android.app.job.IJobService;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;

public abstract class JobService extends Service {
    public static final String PERMISSION_BIND = "android.permission.BIND_JOB_SERVICE";
    private static final String TAG = "JobService";

    @GuardedBy("mHandlerLock")
    JobHandler mHandler;
    private final int MSG_EXECUTE_JOB = 0;
    private final int MSG_STOP_JOB = 1;
    private final int MSG_JOB_FINISHED = 2;
    private final Object mHandlerLock = new Object();
    IJobService mBinder = new IJobService.Stub() {
        @Override
        public void startJob(JobParameters jobParams) {
            JobService.this.ensureHandler();
            Message m = Message.obtain(JobService.this.mHandler, 0, jobParams);
            m.sendToTarget();
        }

        @Override
        public void stopJob(JobParameters jobParams) {
            JobService.this.ensureHandler();
            Message m = Message.obtain(JobService.this.mHandler, 1, jobParams);
            m.sendToTarget();
        }
    };

    public abstract boolean onStartJob(JobParameters jobParameters);

    public abstract boolean onStopJob(JobParameters jobParameters);

    void ensureHandler() {
        synchronized (this.mHandlerLock) {
            if (this.mHandler == null) {
                this.mHandler = new JobHandler(getMainLooper());
            }
        }
    }

    class JobHandler extends Handler {
        JobHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            JobParameters params = (JobParameters) msg.obj;
            switch (msg.what) {
                case 0:
                    try {
                        boolean workOngoing = JobService.this.onStartJob(params);
                        ackStartMessage(params, workOngoing);
                        return;
                    } catch (Exception e) {
                        Log.e(JobService.TAG, "Error while executing job: " + params.getJobId());
                        throw new RuntimeException(e);
                    }
                case 1:
                    try {
                        boolean ret = JobService.this.onStopJob(params);
                        ackStopMessage(params, ret);
                        return;
                    } catch (Exception e2) {
                        Log.e(JobService.TAG, "Application unable to handle onStopJob.", e2);
                        throw new RuntimeException(e2);
                    }
                case 2:
                    boolean needsReschedule = msg.arg2 == 1;
                    IJobCallback callback = params.getCallback();
                    if (callback != null) {
                        try {
                            callback.jobFinished(params.getJobId(), needsReschedule);
                            return;
                        } catch (RemoteException e3) {
                            Log.e(JobService.TAG, "Error reporting job finish to system: binder has goneaway.");
                            return;
                        }
                    }
                    Log.e(JobService.TAG, "finishJob() called for a nonexistent job id.");
                    return;
                default:
                    Log.e(JobService.TAG, "Unrecognised message received.");
                    return;
            }
        }

        private void ackStartMessage(JobParameters params, boolean workOngoing) {
            IJobCallback callback = params.getCallback();
            int jobId = params.getJobId();
            if (callback != null) {
                try {
                    callback.acknowledgeStartMessage(jobId, workOngoing);
                } catch (RemoteException e) {
                    Log.e(JobService.TAG, "System unreachable for starting job.");
                }
            } else if (Log.isLoggable(JobService.TAG, 3)) {
                Log.d(JobService.TAG, "Attempting to ack a job that has already been processed.");
            }
        }

        private void ackStopMessage(JobParameters params, boolean reschedule) {
            IJobCallback callback = params.getCallback();
            int jobId = params.getJobId();
            if (callback != null) {
                try {
                    callback.acknowledgeStopMessage(jobId, reschedule);
                } catch (RemoteException e) {
                    Log.e(JobService.TAG, "System unreachable for stopping job.");
                }
            } else if (Log.isLoggable(JobService.TAG, 3)) {
                Log.d(JobService.TAG, "Attempting to ack a job that has already been processed.");
            }
        }
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return this.mBinder.asBinder();
    }

    public final void jobFinished(JobParameters params, boolean needsReschedule) {
        ensureHandler();
        Message m = Message.obtain(this.mHandler, 2, params);
        m.arg2 = needsReschedule ? 1 : 0;
        m.sendToTarget();
    }
}
