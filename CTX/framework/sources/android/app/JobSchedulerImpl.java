package android.app;

import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.os.RemoteException;
import java.util.List;

public class JobSchedulerImpl extends JobScheduler {
    IJobScheduler mBinder;

    JobSchedulerImpl(IJobScheduler iJobScheduler) {
        this.mBinder = iJobScheduler;
    }

    @Override
    public int schedule(JobInfo jobInfo) {
        try {
            return this.mBinder.schedule(jobInfo);
        } catch (RemoteException e) {
            return 0;
        }
    }

    @Override
    public int enqueue(JobInfo jobInfo, JobWorkItem jobWorkItem) {
        try {
            return this.mBinder.enqueue(jobInfo, jobWorkItem);
        } catch (RemoteException e) {
            return 0;
        }
    }

    @Override
    public int scheduleAsPackage(JobInfo jobInfo, String str, int i, String str2) {
        try {
            return this.mBinder.scheduleAsPackage(jobInfo, str, i, str2);
        } catch (RemoteException e) {
            return 0;
        }
    }

    @Override
    public void cancel(int i) {
        try {
            this.mBinder.cancel(i);
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
    public JobInfo getPendingJob(int i) {
        try {
            return this.mBinder.getPendingJob(i);
        } catch (RemoteException e) {
            return null;
        }
    }
}
