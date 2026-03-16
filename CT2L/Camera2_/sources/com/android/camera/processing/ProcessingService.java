package com.android.camera.processing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import com.android.camera.app.CameraApp;
import com.android.camera.app.CameraServices;
import com.android.camera.debug.Log;
import com.android.camera.session.CaptureSession;
import com.android.camera.session.CaptureSessionManager;
import com.android.camera2.R;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProcessingService extends Service implements CaptureSession.ProgressListener {
    public static final String ACTION_PAUSE_PROCESSING_SERVICE = "com.android.camera.processing.PAUSE";
    public static final String ACTION_RESUME_PROCESSING_SERVICE = "com.android.camera.processing.RESUME";
    private static final int CAMERA_NOTIFICATION_ID = 2;
    private static final Log.Tag TAG = new Log.Tag("ProcessingService");
    private static final int THREAD_PRIORITY = -4;
    private ProcessingTask mCurrentTask;
    private Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private ProcessingServiceManager mProcessingServiceManager;
    private Thread mProcessingThread;
    private CaptureSessionManager mSessionManager;
    private PowerManager.WakeLock mWakeLock;
    private final ServiceController mServiceController = new ServiceController();
    private volatile boolean mPaused = false;
    private final Lock mSuspendStatusLock = new ReentrantLock();

    public class ServiceController extends BroadcastReceiver {
        public ServiceController() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ProcessingService.ACTION_PAUSE_PROCESSING_SERVICE) {
                ProcessingService.this.pause();
            } else if (intent.getAction() == ProcessingService.ACTION_RESUME_PROCESSING_SERVICE) {
                ProcessingService.this.resume();
            }
        }
    }

    @Override
    public void onCreate() {
        this.mProcessingServiceManager = ProcessingServiceManager.getInstance();
        this.mSessionManager = getServices().getCaptureSessionManager();
        PowerManager powerManager = (PowerManager) getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, TAG.toString());
        this.mWakeLock.acquire();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PAUSE_PROCESSING_SERVICE);
        intentFilter.addAction(ACTION_RESUME_PROCESSING_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mServiceController, intentFilter);
        this.mNotificationBuilder = createInProgressNotificationBuilder();
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Shutting down");
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mServiceController);
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting in foreground.");
        startForeground(2, this.mNotificationBuilder.build());
        asyncProcessAllTasksAndShutdown();
        return 1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void pause() {
        Log.d(TAG, "Pausing");
        try {
            this.mSuspendStatusLock.lock();
            this.mPaused = true;
            if (this.mCurrentTask != null) {
                this.mCurrentTask.suspend();
            }
        } finally {
            this.mSuspendStatusLock.unlock();
        }
    }

    private void resume() {
        Log.d(TAG, "Resuming");
        try {
            this.mSuspendStatusLock.lock();
            this.mPaused = false;
            if (this.mCurrentTask != null) {
                this.mCurrentTask.resume();
            }
        } finally {
            this.mSuspendStatusLock.unlock();
        }
    }

    private void asyncProcessAllTasksAndShutdown() {
        if (this.mProcessingThread == null) {
            this.mProcessingThread = new Thread("CameraProcessingThread") {
                @Override
                public void run() {
                    Process.setThreadPriority(ProcessingService.THREAD_PRIORITY);
                    while (true) {
                        ProcessingTask task = ProcessingService.this.mProcessingServiceManager.popNextSession();
                        if (task != null) {
                            ProcessingService.this.mCurrentTask = task;
                            try {
                                ProcessingService.this.mSuspendStatusLock.lock();
                                if (ProcessingService.this.mPaused) {
                                    ProcessingService.this.mCurrentTask.suspend();
                                }
                                ProcessingService.this.mSuspendStatusLock.unlock();
                                ProcessingService.this.processAndNotify(task);
                            } catch (Throwable th) {
                                ProcessingService.this.mSuspendStatusLock.unlock();
                                throw th;
                            }
                        } else {
                            ProcessingService.this.stopSelf();
                            return;
                        }
                    }
                }
            };
            this.mProcessingThread.start();
        }
    }

    void processAndNotify(ProcessingTask task) {
        if (task == null) {
            Log.e(TAG, "Reference to ProcessingTask is null");
            return;
        }
        CaptureSession session = task.getSession();
        if (session == null) {
            session = this.mSessionManager.createNewSession(task.getName(), 0L, task.getLocation());
        }
        resetNotification();
        session.addProgressListener(this);
        System.gc();
        Log.d(TAG, "Processing start");
        task.process(this, getServices(), session);
        Log.d(TAG, "Processing done");
    }

    private void resetNotification() {
        this.mNotificationBuilder.setContentText("…").setProgress(100, 0, false);
        postNotification();
    }

    private CameraServices getServices() {
        return (CameraApp) getApplication();
    }

    private void postNotification() {
        this.mNotificationManager.notify(2, this.mNotificationBuilder.build());
    }

    private Notification.Builder createInProgressNotificationBuilder() {
        return new Notification.Builder(this).setSmallIcon(R.drawable.ic_notification).setWhen(System.currentTimeMillis()).setOngoing(true).setContentTitle(getText(R.string.app_name));
    }

    @Override
    public void onProgressChanged(int progress) {
        this.mNotificationBuilder.setProgress(100, progress, false);
        postNotification();
    }

    @Override
    public void onStatusMessageChanged(CharSequence message) {
        this.mNotificationBuilder.setContentText(message);
        postNotification();
    }
}
