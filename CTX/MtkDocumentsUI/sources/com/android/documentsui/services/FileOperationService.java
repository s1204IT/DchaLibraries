package com.android.documentsui.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.UserManager;
import android.util.Log;
import com.android.documentsui.R;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.services.Job;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class FileOperationService extends Service implements Job.Listener {
    static final boolean $assertionsDisabled = false;
    Intent deleteUpdateIntent;
    ExecutorService deletionExecutor;
    ExecutorService executor;
    Features features;
    ForegroundManager foregroundManager;
    Handler handler;
    private int mLastServiceId;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    NotificationManager notificationManager;
    private final Map<String, JobRecord> mJobs = new HashMap();
    private final AtomicReference<Job> mForegroundJob = new AtomicReference<>();
    private Runnable sendUpdatesToUI = new Runnable() {
        @Override
        public void run() {
            Log.d("FileOperationService", "entered sendUpdatesToUI");
            FileOperationService.this.sendBroadcast(FileOperationService.this.deleteUpdateIntent);
        }
    };

    interface ForegroundManager {
        void startForeground(int i, Notification notification);

        void stopForeground(boolean z);
    }

    @Override
    public void onCreate() {
        if (this.executor == null) {
            this.executor = Executors.newFixedThreadPool(2);
        }
        if (this.deletionExecutor == null) {
            this.deletionExecutor = Executors.newCachedThreadPool();
        }
        if (this.handler == null) {
            this.handler = new Handler();
        }
        if (this.foregroundManager == null) {
            this.foregroundManager = createForegroundManager(this);
        }
        if (this.notificationManager == null) {
            this.notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        }
        this.features = new Features.RuntimeFeatures(getResources(), UserManager.get(this));
        setUpNotificationChannel();
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "Created.");
        }
        this.mPowerManager = (PowerManager) getSystemService(PowerManager.class);
    }

    private void setUpNotificationChannel() {
        if (this.features.isNotificationChannelEnabled()) {
            this.notificationManager.createNotificationChannel(new NotificationChannel("channel_id", getString(R.string.app_label), 2));
        }
    }

    @Override
    public void onDestroy() {
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "Shutting down executor.");
        }
        List<Runnable> listShutdownNow = this.executor.shutdownNow();
        List<Runnable> listShutdownNow2 = this.deletionExecutor.shutdownNow();
        ArrayList arrayList = new ArrayList(listShutdownNow.size() + listShutdownNow2.size());
        arrayList.addAll(listShutdownNow);
        arrayList.addAll(listShutdownNow2);
        if (!arrayList.isEmpty()) {
            Log.w("FileOperationService", "Shutting down, but executor reports running jobs: " + arrayList);
        }
        this.executor = null;
        this.deletionExecutor = null;
        this.handler = null;
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "Destroyed.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        String stringExtra = intent.getStringExtra("com.android.documentsui.JOB_ID");
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "onStartCommand: " + stringExtra + " with serviceId " + i2);
        }
        if (intent.hasExtra("com.android.documentsui.CANCEL")) {
            handleCancel(intent);
        } else {
            handleOperation(stringExtra, (FileOperation) intent.getParcelableExtra("com.android.documentsui.OPERATION"));
        }
        this.mLastServiceId = i2;
        return 2;
    }

    @Override
    public void onTaskRemoved(Intent intent) {
        Log.d("FileOperationService", "onTaskRemoved");
        super.onTaskRemoved(intent);
        this.notificationManager.cancelAll();
        stopSelf();
    }

    private void handleOperation(String str, FileOperation fileOperation) {
        synchronized (this.mJobs) {
            if (this.mWakeLock == null) {
                this.mWakeLock = this.mPowerManager.newWakeLock(1, "FileOperationService");
                this.mWakeLock.setReferenceCounted(false);
            }
            if (this.mJobs.containsKey(str)) {
                Log.w("FileOperationService", "Duplicate job id: " + str + ". Ignoring job request for operation: " + fileOperation + ".");
                return;
            }
            Job jobCreateJob = fileOperation.createJob(this, this, str, this.features);
            if (jobCreateJob == null) {
                return;
            }
            if (SharedMinimal.DEBUG) {
                Log.d("FileOperationService", "Scheduling job " + jobCreateJob.id + ".");
            }
            this.mJobs.put(str, new JobRecord(jobCreateJob, getExecutorService(fileOperation.getOpType()).submit(jobCreateJob)));
            this.mWakeLock.acquire();
        }
    }

    private void handleCancel(Intent intent) {
        String stringExtra = intent.getStringExtra("com.android.documentsui.JOB_ID");
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "handleCancel: " + stringExtra);
        }
        synchronized (this.mJobs) {
            JobRecord jobRecord = this.mJobs.get(stringExtra);
            if (jobRecord != null) {
                jobRecord.job.cancel();
            }
        }
        this.notificationManager.cancel(stringExtra, 0);
    }

    private ExecutorService getExecutorService(int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
                return this.executor;
            case 5:
                return this.deletionExecutor;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void deleteJob(Job job) {
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "deleteJob: " + job.id);
        }
        this.mWakeLock.release();
        if (!this.mWakeLock.isHeld()) {
            this.mWakeLock = null;
        }
        this.mJobs.remove(job.id).job.cleanup();
    }

    private void shutdown() {
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "Shutting down. Last serviceId was " + this.mLastServiceId);
        }
        boolean zStopSelfResult = stopSelfResult(this.mLastServiceId);
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "Stopping service: " + zStopSelfResult);
        }
        if (!zStopSelfResult) {
            Log.w("FileOperationService", "Service should be stopping, but reports otherwise.");
        }
    }

    boolean holdsWakeLock() {
        return this.mWakeLock != null && this.mWakeLock.isHeld();
    }

    @Override
    public void onStart(Job job) {
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "onStart: " + job.id);
        }
        Notification setupNotification = job.getSetupNotification();
        if (this.mForegroundJob.compareAndSet(null, job)) {
            if (SharedMinimal.DEBUG) {
                Log.d("FileOperationService", "Set foreground job to " + job.id);
            }
            this.foregroundManager.startForeground(0, setupNotification);
        }
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "Posting notification for " + job.id);
        }
        this.notificationManager.notify(job.id, 0, setupNotification);
        new JobMonitor(job, this.notificationManager, this.handler, this.mJobs).start();
    }

    @Override
    public void onFinished(final Job job) {
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "onFinished: " + job.id);
        }
        synchronized (this.mJobs) {
            deleteJob(job);
            updateForegroundState(job);
            this.handler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.cleanUpNotification(job);
                }
            });
            if (this.mJobs.isEmpty()) {
                this.handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.shutdown();
                    }
                });
            }
            Log.d("FileOperationService", "onFinished: broadcast");
            this.deleteUpdateIntent = new Intent("message");
            this.handler.removeCallbacks(this.sendUpdatesToUI);
            this.handler.post(this.sendUpdatesToUI);
        }
    }

    private void updateForegroundState(Job job) {
        Notification progressNotification;
        Job job2 = this.mJobs.isEmpty() ? null : this.mJobs.values().iterator().next().job;
        if (this.mForegroundJob.compareAndSet(job, job2)) {
            if (job2 == null) {
                if (SharedMinimal.DEBUG) {
                    Log.d("FileOperationService", "Stop foreground");
                }
                this.foregroundManager.stopForeground(true);
                return;
            }
            if (SharedMinimal.DEBUG) {
                Log.d("FileOperationService", "Switch foreground job to " + job2.id);
            }
            if (job2.getState() == 1) {
                progressNotification = job2.getSetupNotification();
            } else {
                progressNotification = job2.getProgressNotification();
            }
            this.foregroundManager.startForeground(0, progressNotification);
            this.notificationManager.notify(job2.id, 0, progressNotification);
        }
    }

    private void cleanUpNotification(Job job) {
        if (SharedMinimal.DEBUG) {
            Log.d("FileOperationService", "Canceling notification for " + job.id);
        }
        this.notificationManager.cancel(job.id, 0);
        if (job.hasFailures()) {
            if (!job.failedUris.isEmpty()) {
                Log.e("FileOperationService", "Job failed to resolve uris: " + job.failedUris + ".");
            }
            if (!job.failedDocs.isEmpty()) {
                Log.e("FileOperationService", "Job failed to process docs: " + job.failedDocs + ".");
            }
            this.notificationManager.notify(job.id, 1, job.getFailureNotification());
        }
        if (job.hasWarnings()) {
            if (SharedMinimal.DEBUG) {
                Log.d("FileOperationService", "Job finished with warnings.");
            }
            this.notificationManager.notify(job.id, 2, job.getWarningNotification());
        }
    }

    private static final class JobRecord {
        private final Future<?> future;
        private final Job job;

        public JobRecord(Job job, Future<?> future) {
            this.job = job;
            this.future = future;
        }
    }

    private static final class JobMonitor implements Runnable {
        private final Handler mHandler;
        private final Job mJob;
        private final Object mJobsLock;
        private final NotificationManager mNotificationManager;

        private JobMonitor(Job job, NotificationManager notificationManager, Handler handler, Object obj) {
            this.mJob = job;
            this.mNotificationManager = notificationManager;
            this.mHandler = handler;
            this.mJobsLock = obj;
        }

        private void start() {
            this.mHandler.post(this);
        }

        @Override
        public void run() {
            synchronized (this.mJobsLock) {
                if (this.mJob.isFinished()) {
                    Log.d("FileOperationService", "JobMonitor removeNotification");
                    this.mNotificationManager.cancel(this.mJob.id, 0);
                } else {
                    if (this.mJob.getState() == 2) {
                        this.mNotificationManager.notify(this.mJob.id, 0, this.mJob.getProgressNotification());
                    }
                    this.mHandler.postDelayed(this, 500L);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static ForegroundManager createForegroundManager(final Service service) {
        return new ForegroundManager() {
            @Override
            public void startForeground(int i, Notification notification) {
                service.startForeground(i, notification);
            }

            @Override
            public void stopForeground(boolean z) {
                service.stopForeground(z);
            }
        };
    }
}
