package android.app;

import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.os.RemoteException;
import java.util.List;

public class JobSchedulerImpl extends JobScheduler {
    IJobScheduler mBinder;

    JobSchedulerImpl(IJobScheduler binder) {
        this.mBinder = binder;
    }

    @Override
    public int schedule(JobInfo job) {
        try {
            return this.mBinder.schedule(job);
        } catch (RemoteException e) {
            return 0;
        }
    }

    @Override
    public int scheduleAsPackage(JobInfo job, String packageName, int userId, String tag) {
        try {
            return this.mBinder.scheduleAsPackage(job, packageName, userId, tag);
        } catch (RemoteException e) {
            return 0;
        }
    }

    @Override
    public void cancel(int jobId) {
        try {
            this.mBinder.cancel(jobId);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void cancelAll() {
        try {
            this.mBinder.cancelAll();
        } catch (RemoteException e) {
        }
    }

    @Override
    public List<JobInfo> getAllPendingJobs() {
        try {
            return this.mBinder.getAllPendingJobs();
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public JobInfo getPendingJob(int jobId) {
        try {
            return this.mBinder.getPendingJob(jobId);
        } catch (RemoteException e) {
            return null;
        }
    }
}
