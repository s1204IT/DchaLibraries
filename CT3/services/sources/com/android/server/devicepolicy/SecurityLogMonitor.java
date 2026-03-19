package com.android.server.devicepolicy;

import android.app.admin.SecurityLog;
import android.os.Process;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SecurityLogMonitor implements Runnable {
    private static final int BUFFER_ENTRIES_MAXIMUM_LEVEL = 10240;
    private static final int BUFFER_ENTRIES_NOTIFICATION_LEVEL = 1024;
    private static final boolean DEBUG = false;
    private static final String TAG = "SecurityLogMonitor";
    private final DevicePolicyManagerService mService;
    private static final long RATE_LIMIT_INTERVAL_MILLISECONDS = TimeUnit.HOURS.toMillis(2);
    private static final long POLLING_INTERVAL_MILLISECONDS = TimeUnit.MINUTES.toMillis(1);
    private final Lock mLock = new ReentrantLock();

    @GuardedBy("mLock")
    private Thread mMonitorThread = null;

    @GuardedBy("mLock")
    private ArrayList<SecurityLog.SecurityEvent> mPendingLogs = new ArrayList<>();

    @GuardedBy("mLock")
    private boolean mAllowedToRetrieve = false;

    @GuardedBy("mLock")
    private long mNextAllowedRetrivalTimeMillis = -1;

    SecurityLogMonitor(DevicePolicyManagerService service) {
        this.mService = service;
    }

    void start() {
        this.mLock.lock();
        try {
            if (this.mMonitorThread == null) {
                this.mPendingLogs = new ArrayList<>();
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrivalTimeMillis = -1L;
                this.mMonitorThread = new Thread(this);
                this.mMonitorThread.start();
            }
        } finally {
            this.mLock.unlock();
        }
    }

    void stop() {
        this.mLock.lock();
        try {
            if (this.mMonitorThread != null) {
                this.mMonitorThread.interrupt();
                try {
                    this.mMonitorThread.join(TimeUnit.SECONDS.toMillis(5L));
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for thread to stop", e);
                }
                this.mPendingLogs = new ArrayList<>();
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrivalTimeMillis = -1L;
                this.mMonitorThread = null;
            }
        } finally {
            this.mLock.unlock();
        }
    }

    List<SecurityLog.SecurityEvent> retrieveLogs() {
        this.mLock.lock();
        try {
            if (this.mAllowedToRetrieve) {
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrivalTimeMillis = System.currentTimeMillis() + RATE_LIMIT_INTERVAL_MILLISECONDS;
                List<SecurityLog.SecurityEvent> result = this.mPendingLogs;
                this.mPendingLogs = new ArrayList<>();
                return result;
            }
            return null;
        } finally {
            this.mLock.unlock();
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(10);
        ArrayList<SecurityLog.SecurityEvent> logs = new ArrayList<>();
        long lastLogTimestampNanos = -1;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(POLLING_INTERVAL_MILLISECONDS);
                if (lastLogTimestampNanos < 0) {
                    SecurityLog.readEvents(logs);
                } else {
                    SecurityLog.readEventsSince(1 + lastLogTimestampNanos, logs);
                }
                if (!logs.isEmpty()) {
                    this.mLock.lockInterruptibly();
                    try {
                        this.mPendingLogs.addAll(logs);
                        if (this.mPendingLogs.size() > BUFFER_ENTRIES_MAXIMUM_LEVEL) {
                            this.mPendingLogs = new ArrayList<>(this.mPendingLogs.subList(this.mPendingLogs.size() - 5120, this.mPendingLogs.size()));
                        }
                        this.mLock.unlock();
                        lastLogTimestampNanos = logs.get(logs.size() - 1).getTimeNanos();
                        logs.clear();
                    } catch (Throwable th) {
                        this.mLock.unlock();
                        throw th;
                    }
                }
                notifyDeviceOwnerIfNeeded();
            } catch (IOException e) {
                Log.e(TAG, "Failed to read security log", e);
            } catch (InterruptedException e2) {
                Log.i(TAG, "Thread interrupted, exiting.", e2);
                return;
            }
        }
    }

    private void notifyDeviceOwnerIfNeeded() throws InterruptedException {
        boolean allowToRetrieveNow = false;
        this.mLock.lockInterruptibly();
        try {
            int logSize = this.mPendingLogs.size();
            if (logSize >= 1024) {
                allowToRetrieveNow = true;
            } else if (logSize > 0 && (this.mNextAllowedRetrivalTimeMillis == -1 || System.currentTimeMillis() >= this.mNextAllowedRetrivalTimeMillis)) {
                allowToRetrieveNow = true;
            }
            boolean shouldNotifyDO = !this.mAllowedToRetrieve ? allowToRetrieveNow : false;
            this.mAllowedToRetrieve = allowToRetrieveNow;
            if (!shouldNotifyDO) {
                return;
            }
            this.mService.sendDeviceOwnerCommand("android.app.action.SECURITY_LOGS_AVAILABLE", null);
        } finally {
            this.mLock.unlock();
        }
    }
}
