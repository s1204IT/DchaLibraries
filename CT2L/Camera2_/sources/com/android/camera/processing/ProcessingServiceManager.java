package com.android.camera.processing;

import android.content.Context;
import android.content.Intent;
import com.android.camera.debug.Log;
import java.util.LinkedList;

public class ProcessingServiceManager {
    private static final Log.Tag TAG = new Log.Tag("ProcessingSvcMgr");
    private static ProcessingServiceManager sInstance;
    private final Context mAppContext;
    private final LinkedList<ProcessingTask> mQueue = new LinkedList<>();
    private volatile boolean mServiceRunning = false;
    private boolean mHoldProcessing = false;

    public static void initSingleton(Context appContext) {
        sInstance = new ProcessingServiceManager(appContext);
    }

    public static ProcessingServiceManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("initSingleton() not yet called.");
        }
        return sInstance;
    }

    private ProcessingServiceManager(Context context) {
        this.mAppContext = context;
    }

    public synchronized void enqueueTask(ProcessingTask task) {
        this.mQueue.add(task);
        Log.d(TAG, "Task added. Queue size now: " + this.mQueue.size());
        if (!this.mServiceRunning && !this.mHoldProcessing) {
            startService();
        }
    }

    public synchronized ProcessingTask popNextSession() {
        ProcessingTask processingTaskRemove;
        if (!this.mQueue.isEmpty() && !this.mHoldProcessing) {
            Log.d(TAG, "Popping a session. Remaining: " + (this.mQueue.size() - 1));
            processingTaskRemove = this.mQueue.remove();
        } else {
            Log.d(TAG, "Popping null. On hold? " + this.mHoldProcessing);
            this.mServiceRunning = false;
            processingTaskRemove = null;
        }
        return processingTaskRemove;
    }

    public synchronized boolean isRunningOrHasItems() {
        boolean z;
        if (!this.mServiceRunning) {
            z = !this.mQueue.isEmpty();
        }
        return z;
    }

    public synchronized boolean suspendProcessing() {
        boolean z = true;
        synchronized (this) {
            if (!isRunningOrHasItems()) {
                Log.d(TAG, "Suspend processing");
                this.mHoldProcessing = true;
            } else {
                Log.d(TAG, "Not able to suspend processing.");
                z = false;
            }
        }
        return z;
    }

    public synchronized void resumeProcessing() {
        Log.d(TAG, "Resume processing. Queue size: " + this.mQueue.size());
        if (this.mHoldProcessing) {
            this.mHoldProcessing = false;
            if (!this.mQueue.isEmpty()) {
                startService();
            }
        }
    }

    private void startService() {
        this.mAppContext.startService(new Intent(this.mAppContext, (Class<?>) ProcessingService.class));
        this.mServiceRunning = true;
    }
}
