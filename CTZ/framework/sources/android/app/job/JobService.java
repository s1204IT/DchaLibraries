package android.app.job;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public abstract class JobService extends Service {
    public static final String PERMISSION_BIND = "android.permission.BIND_JOB_SERVICE";
    private static final String TAG = "JobService";
    private JobServiceEngine mEngine;

    public abstract boolean onStartJob(JobParameters jobParameters);

    public abstract boolean onStopJob(JobParameters jobParameters);

    @Override
    public final IBinder onBind(Intent intent) {
        if (this.mEngine == null) {
            this.mEngine = new JobServiceEngine(this) {
                @Override
                public boolean onStartJob(JobParameters jobParameters) {
                    return JobService.this.onStartJob(jobParameters);
                }

                @Override
                public boolean onStopJob(JobParameters jobParameters) {
                    return JobService.this.onStopJob(jobParameters);
                }
            };
        }
        return this.mEngine.getBinder();
    }

    public final void jobFinished(JobParameters jobParameters, boolean z) {
        this.mEngine.jobFinished(jobParameters, z);
    }
}
