package android.app.job;

import android.app.Service;
import android.app.job.IJobService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import java.lang.ref.WeakReference;

public abstract class JobServiceEngine {
    private static final int MSG_EXECUTE_JOB = 0;
    private static final int MSG_JOB_FINISHED = 2;
    private static final int MSG_STOP_JOB = 1;
    private static final String TAG = "JobServiceEngine";
    private final IJobService mBinder = new JobInterface(this);
    JobHandler mHandler;

    public abstract boolean onStartJob(JobParameters jobParameters);

    public abstract boolean onStopJob(JobParameters jobParameters);

    static final class JobInterface extends IJobService.Stub {
        final WeakReference<JobServiceEngine> mService;

        JobInterface(JobServiceEngine jobServiceEngine) {
            this.mService = new WeakReference<>(jobServiceEngine);
        }

        @Override
        public void startJob(JobParameters jobParameters) throws RemoteException {
            JobServiceEngine jobServiceEngine = this.mService.get();
            if (jobServiceEngine != null) {
                Message.obtain(jobServiceEngine.mHandler, 0, jobParameters).sendToTarget();
            }
        }

        @Override
        public void stopJob(JobParameters jobParameters) throws RemoteException {
            JobServiceEngine jobServiceEngine = this.mService.get();
            if (jobServiceEngine != null) {
                Message.obtain(jobServiceEngine.mHandler, 1, jobParameters).sendToTarget();
            }
        }
    }

    class JobHandler extends Handler {
        JobHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            JobParameters jobParameters = (JobParameters) message.obj;
            switch (message.what) {
                case 0:
                    try {
                        ackStartMessage(jobParameters, JobServiceEngine.this.onStartJob(jobParameters));
                        return;
                    } catch (Exception e) {
                        Log.e(JobServiceEngine.TAG, "Error while executing job: " + jobParameters.getJobId());
                        throw new RuntimeException(e);
                    }
                case 1:
                    try {
                        ackStopMessage(jobParameters, JobServiceEngine.this.onStopJob(jobParameters));
                        return;
                    } catch (Exception e2) {
                        Log.e(JobServiceEngine.TAG, "Application unable to handle onStopJob.", e2);
                        throw new RuntimeException(e2);
                    }
                case 2:
                    boolean z = message.arg2 == 1;
                    IJobCallback callback = jobParameters.getCallback();
                    if (callback != null) {
                        try {
                            callback.jobFinished(jobParameters.getJobId(), z);
                            return;
                        } catch (RemoteException e3) {
                            Log.e(JobServiceEngine.TAG, "Error reporting job finish to system: binder has goneaway.");
                            return;
                        }
                    }
                    Log.e(JobServiceEngine.TAG, "finishJob() called for a nonexistent job id.");
                    return;
                default:
                    Log.e(JobServiceEngine.TAG, "Unrecognised message received.");
                    return;
            }
        }

        private void ackStartMessage(JobParameters jobParameters, boolean z) {
            IJobCallback callback = jobParameters.getCallback();
            int jobId = jobParameters.getJobId();
            if (callback != null) {
                try {
                    callback.acknowledgeStartMessage(jobId, z);
                } catch (RemoteException e) {
                    Log.e(JobServiceEngine.TAG, "System unreachable for starting job.");
                }
            } else if (Log.isLoggable(JobServiceEngine.TAG, 3)) {
                Log.d(JobServiceEngine.TAG, "Attempting to ack a job that has already been processed.");
            }
        }

        private void ackStopMessage(JobParameters jobParameters, boolean z) {
            IJobCallback callback = jobParameters.getCallback();
            int jobId = jobParameters.getJobId();
            if (callback != null) {
                try {
                    callback.acknowledgeStopMessage(jobId, z);
                } catch (RemoteException e) {
                    Log.e(JobServiceEngine.TAG, "System unreachable for stopping job.");
                }
            } else if (Log.isLoggable(JobServiceEngine.TAG, 3)) {
                Log.d(JobServiceEngine.TAG, "Attempting to ack a job that has already been processed.");
            }
        }
    }

    public JobServiceEngine(Service service) {
        this.mHandler = new JobHandler(service.getMainLooper());
    }

    public final IBinder getBinder() {
        return this.mBinder.asBinder();
    }

    public void jobFinished(JobParameters jobParameters, boolean z) {
        if (jobParameters == null) {
            throw new NullPointerException("params");
        }
        Message messageObtain = Message.obtain(this.mHandler, 2, jobParameters);
        messageObtain.arg2 = z ? 1 : 0;
        messageObtain.sendToTarget();
    }
}
