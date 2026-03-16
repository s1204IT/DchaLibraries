package com.android.ex.camera2.portability;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import com.android.ex.camera2.portability.debug.Log;
import java.util.LinkedList;
import java.util.Queue;

public class DispatchThread extends Thread {
    private static final long MAX_MESSAGE_QUEUE_LENGTH = 256;
    private static final Log.Tag TAG = new Log.Tag("DispatchThread");
    private Handler mCameraHandler;
    private HandlerThread mCameraHandlerThread;
    private Boolean mIsEnded;
    private final Queue<Runnable> mJobQueue;

    public DispatchThread(Handler cameraHandler, HandlerThread cameraHandlerThread) {
        super("Camera Job Dispatch Thread");
        this.mJobQueue = new LinkedList();
        this.mIsEnded = new Boolean(false);
        this.mCameraHandler = cameraHandler;
        this.mCameraHandlerThread = cameraHandlerThread;
    }

    public void runJob(Runnable job) {
        if (isEnded()) {
            throw new IllegalStateException("Trying to run job on interrupted dispatcher thread");
        }
        synchronized (this.mJobQueue) {
            if (this.mJobQueue.size() == 256) {
                throw new RuntimeException("Camera master thread job queue full");
            }
            this.mJobQueue.add(job);
            this.mJobQueue.notifyAll();
        }
    }

    public void runJobSync(Runnable job, Object waitLock, long timeoutMs, String jobMsg) {
        String timeoutMsg = "Timeout waiting " + timeoutMs + "ms for " + jobMsg;
        synchronized (waitLock) {
            long timeoutBound = SystemClock.uptimeMillis() + timeoutMs;
            try {
                runJob(job);
                waitLock.wait(timeoutMs);
                if (SystemClock.uptimeMillis() > timeoutBound) {
                    throw new IllegalStateException(timeoutMsg);
                }
            } catch (InterruptedException e) {
                if (SystemClock.uptimeMillis() > timeoutBound) {
                    throw new IllegalStateException(timeoutMsg);
                }
            }
        }
    }

    public void end() {
        synchronized (this.mIsEnded) {
            this.mIsEnded = true;
        }
        synchronized (this.mJobQueue) {
            this.mJobQueue.notifyAll();
        }
    }

    private boolean isEnded() {
        boolean zBooleanValue;
        synchronized (this.mIsEnded) {
            zBooleanValue = this.mIsEnded.booleanValue();
        }
        return zBooleanValue;
    }

    @Override
    public void run() {
        Runnable job;
        while (true) {
            synchronized (this.mJobQueue) {
                while (this.mJobQueue.size() == 0 && !isEnded()) {
                    try {
                        this.mJobQueue.wait();
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Dispatcher thread wait() interrupted, exiting");
                    }
                }
                job = this.mJobQueue.poll();
            }
            if (job == null) {
                if (isEnded()) {
                    this.mCameraHandlerThread.quitSafely();
                    return;
                }
            } else {
                job.run();
                synchronized (this) {
                    this.mCameraHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (DispatchThread.this) {
                                DispatchThread.this.notifyAll();
                            }
                        }
                    });
                    try {
                        wait();
                    } catch (InterruptedException e2) {
                    }
                }
            }
        }
    }
}
