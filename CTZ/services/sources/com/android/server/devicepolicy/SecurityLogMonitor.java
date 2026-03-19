package com.android.server.devicepolicy;

import android.app.admin.SecurityLog;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.job.controllers.JobStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SecurityLogMonitor implements Runnable {
    private static final int BUFFER_ENTRIES_CRITICAL_LEVEL = 9216;
    private static final int BUFFER_ENTRIES_MAXIMUM_LEVEL = 10240;

    @VisibleForTesting
    static final int BUFFER_ENTRIES_NOTIFICATION_LEVEL = 1024;
    private static final boolean DEBUG = false;
    private static final String TAG = "SecurityLogMonitor";

    @GuardedBy("mLock")
    private boolean mAllowedToRetrieve;

    @GuardedBy("mLock")
    private boolean mCriticalLevelLogged;
    private final Semaphore mForceSemaphore;

    @GuardedBy("mLock")
    private long mId;
    private long mLastEventNanos;
    private final ArrayList<SecurityLog.SecurityEvent> mLastEvents;

    @GuardedBy("mForceSemaphore")
    private long mLastForceNanos;
    private final Lock mLock;

    @GuardedBy("mLock")
    private Thread mMonitorThread;

    @GuardedBy("mLock")
    private long mNextAllowedRetrievalTimeMillis;

    @GuardedBy("mLock")
    private boolean mPaused;

    @GuardedBy("mLock")
    private ArrayList<SecurityLog.SecurityEvent> mPendingLogs;
    private final DevicePolicyManagerService mService;
    private static final long RATE_LIMIT_INTERVAL_MS = TimeUnit.HOURS.toMillis(2);
    private static final long BROADCAST_RETRY_INTERVAL_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long POLLING_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long OVERLAP_NS = TimeUnit.SECONDS.toNanos(3);
    private static final long FORCE_FETCH_THROTTLE_NS = TimeUnit.SECONDS.toNanos(10);

    SecurityLogMonitor(DevicePolicyManagerService devicePolicyManagerService) {
        this(devicePolicyManagerService, 0L);
    }

    @VisibleForTesting
    SecurityLogMonitor(DevicePolicyManagerService devicePolicyManagerService, long j) {
        this.mLock = new ReentrantLock();
        this.mMonitorThread = null;
        this.mPendingLogs = new ArrayList<>();
        this.mAllowedToRetrieve = false;
        this.mCriticalLevelLogged = false;
        this.mLastEvents = new ArrayList<>();
        this.mLastEventNanos = -1L;
        this.mNextAllowedRetrievalTimeMillis = -1L;
        this.mPaused = false;
        this.mForceSemaphore = new Semaphore(0);
        this.mLastForceNanos = 0L;
        this.mService = devicePolicyManagerService;
        this.mId = j;
        this.mLastForceNanos = System.nanoTime();
    }

    void start() {
        Slog.i(TAG, "Starting security logging.");
        SecurityLog.writeEvent(210011, new Object[0]);
        this.mLock.lock();
        try {
            if (this.mMonitorThread == null) {
                this.mPendingLogs = new ArrayList<>();
                this.mCriticalLevelLogged = false;
                this.mId = 0L;
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrievalTimeMillis = -1L;
                this.mPaused = false;
                this.mMonitorThread = new Thread(this);
                this.mMonitorThread.start();
            }
        } finally {
            this.mLock.unlock();
        }
    }

    void stop() {
        Slog.i(TAG, "Stopping security logging.");
        SecurityLog.writeEvent(210012, new Object[0]);
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
                this.mId = 0L;
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrievalTimeMillis = -1L;
                this.mPaused = false;
                this.mMonitorThread = null;
            }
        } finally {
            this.mLock.unlock();
        }
    }

    void pause() {
        Slog.i(TAG, "Paused.");
        this.mLock.lock();
        this.mPaused = true;
        this.mAllowedToRetrieve = false;
        this.mLock.unlock();
    }

    void resume() {
        this.mLock.lock();
        try {
            if (!this.mPaused) {
                Log.d(TAG, "Attempted to resume, but logging is not paused.");
                return;
            }
            this.mPaused = false;
            this.mAllowedToRetrieve = false;
            this.mLock.unlock();
            Slog.i(TAG, "Resumed.");
            try {
                notifyDeviceOwnerIfNeeded(false);
            } catch (InterruptedException e) {
                Log.w(TAG, "Thread interrupted.", e);
            }
        } finally {
            this.mLock.unlock();
        }
    }

    void discardLogs() {
        this.mLock.lock();
        this.mAllowedToRetrieve = false;
        this.mPendingLogs = new ArrayList<>();
        this.mCriticalLevelLogged = false;
        this.mLock.unlock();
        Slog.i(TAG, "Discarded all logs.");
    }

    List<SecurityLog.SecurityEvent> retrieveLogs() {
        this.mLock.lock();
        try {
            if (this.mAllowedToRetrieve) {
                this.mAllowedToRetrieve = false;
                this.mNextAllowedRetrievalTimeMillis = SystemClock.elapsedRealtime() + RATE_LIMIT_INTERVAL_MS;
                ArrayList<SecurityLog.SecurityEvent> arrayList = this.mPendingLogs;
                this.mPendingLogs = new ArrayList<>();
                this.mCriticalLevelLogged = false;
                return arrayList;
            }
            return null;
        } finally {
            this.mLock.unlock();
        }
    }

    private void getNextBatch(ArrayList<SecurityLog.SecurityEvent> arrayList) throws IOException {
        if (this.mLastEventNanos < 0) {
            SecurityLog.readEvents(arrayList);
        } else {
            SecurityLog.readEventsSince(this.mLastEvents.isEmpty() ? this.mLastEventNanos : Math.max(0L, this.mLastEventNanos - OVERLAP_NS), arrayList);
        }
        int i = 0;
        while (i < arrayList.size() - 1) {
            long timeNanos = arrayList.get(i).getTimeNanos();
            i++;
            if (timeNanos > arrayList.get(i).getTimeNanos()) {
                arrayList.sort(new Comparator() {
                    @Override
                    public final int compare(Object obj, Object obj2) {
                        return Long.signum(((SecurityLog.SecurityEvent) obj).getTimeNanos() - ((SecurityLog.SecurityEvent) obj2).getTimeNanos());
                    }
                });
                return;
            }
        }
    }

    private void saveLastEvents(ArrayList<SecurityLog.SecurityEvent> arrayList) {
        this.mLastEvents.clear();
        if (arrayList.isEmpty()) {
            return;
        }
        this.mLastEventNanos = arrayList.get(arrayList.size() - 1).getTimeNanos();
        int size = arrayList.size() - 2;
        while (size >= 0 && this.mLastEventNanos - arrayList.get(size).getTimeNanos() < OVERLAP_NS) {
            size--;
        }
        this.mLastEvents.addAll(arrayList.subList(size + 1, arrayList.size()));
    }

    @GuardedBy("mLock")
    private void mergeBatchLocked(ArrayList<SecurityLog.SecurityEvent> arrayList) {
        this.mPendingLogs.ensureCapacity(this.mPendingLogs.size() + arrayList.size());
        int i = 0;
        int i2 = 0;
        while (i < this.mLastEvents.size() && i2 < arrayList.size()) {
            SecurityLog.SecurityEvent securityEvent = arrayList.get(i2);
            long timeNanos = securityEvent.getTimeNanos();
            if (timeNanos > this.mLastEventNanos) {
                break;
            }
            SecurityLog.SecurityEvent securityEvent2 = this.mLastEvents.get(i);
            long timeNanos2 = securityEvent2.getTimeNanos();
            if (timeNanos2 > timeNanos) {
                assignLogId(securityEvent);
                this.mPendingLogs.add(securityEvent);
                i2++;
            } else if (timeNanos2 < timeNanos) {
                i++;
            } else {
                if (!securityEvent2.equals(securityEvent)) {
                    assignLogId(securityEvent);
                    this.mPendingLogs.add(securityEvent);
                }
                i++;
                i2++;
            }
        }
        List<SecurityLog.SecurityEvent> listSubList = arrayList.subList(i2, arrayList.size());
        Iterator<SecurityLog.SecurityEvent> it = listSubList.iterator();
        while (it.hasNext()) {
            assignLogId(it.next());
        }
        this.mPendingLogs.addAll(listSubList);
        checkCriticalLevel();
        if (this.mPendingLogs.size() > BUFFER_ENTRIES_MAXIMUM_LEVEL) {
            this.mPendingLogs = new ArrayList<>(this.mPendingLogs.subList(this.mPendingLogs.size() - 5120, this.mPendingLogs.size()));
            this.mCriticalLevelLogged = false;
            Slog.i(TAG, "Pending logs buffer full. Discarding old logs.");
        }
    }

    @GuardedBy("mLock")
    private void checkCriticalLevel() {
        if (SecurityLog.isLoggingEnabled() && this.mPendingLogs.size() >= BUFFER_ENTRIES_CRITICAL_LEVEL && !this.mCriticalLevelLogged) {
            this.mCriticalLevelLogged = true;
            SecurityLog.writeEvent(210015, new Object[0]);
        }
    }

    @GuardedBy("mLock")
    private void assignLogId(SecurityLog.SecurityEvent securityEvent) {
        securityEvent.setId(this.mId);
        if (this.mId == JobStatus.NO_LATEST_RUNTIME) {
            Slog.i(TAG, "Reached maximum id value; wrapping around.");
            this.mId = 0L;
        } else {
            this.mId++;
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(10);
        ArrayList<SecurityLog.SecurityEvent> arrayList = new ArrayList<>();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                boolean zTryAcquire = this.mForceSemaphore.tryAcquire(POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS);
                getNextBatch(arrayList);
                this.mLock.lockInterruptibly();
                try {
                    mergeBatchLocked(arrayList);
                    this.mLock.unlock();
                    saveLastEvents(arrayList);
                    arrayList.clear();
                    notifyDeviceOwnerIfNeeded(zTryAcquire);
                } catch (Throwable th) {
                    this.mLock.unlock();
                    throw th;
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to read security log", e);
            } catch (InterruptedException e2) {
                Log.i(TAG, "Thread interrupted, exiting.", e2);
            }
        }
        this.mLastEvents.clear();
        if (this.mLastEventNanos != -1) {
            this.mLastEventNanos++;
        }
        Slog.i(TAG, "MonitorThread exit.");
    }

    private void notifyDeviceOwnerIfNeeded(boolean z) throws InterruptedException {
        this.mLock.lockInterruptibly();
        try {
            if (this.mPaused) {
                return;
            }
            int size = this.mPendingLogs.size();
            boolean z2 = (size >= 1024 || (z && size > 0)) && !this.mAllowedToRetrieve;
            if (size > 0 && SystemClock.elapsedRealtime() >= this.mNextAllowedRetrievalTimeMillis) {
                z2 = true;
            }
            if (z2) {
                this.mAllowedToRetrieve = true;
                this.mNextAllowedRetrievalTimeMillis = SystemClock.elapsedRealtime() + BROADCAST_RETRY_INTERVAL_MS;
            }
            if (z2) {
                Slog.i(TAG, "notify DO");
                this.mService.sendDeviceOwnerCommand("android.app.action.SECURITY_LOGS_AVAILABLE", null);
            }
        } finally {
            this.mLock.unlock();
        }
    }

    public long forceLogs() {
        long jNanoTime = System.nanoTime();
        synchronized (this.mForceSemaphore) {
            long j = (this.mLastForceNanos + FORCE_FETCH_THROTTLE_NS) - jNanoTime;
            if (j > 0) {
                return TimeUnit.NANOSECONDS.toMillis(j) + 1;
            }
            this.mLastForceNanos = jNanoTime;
            if (this.mForceSemaphore.availablePermits() == 0) {
                this.mForceSemaphore.release();
            }
            return 0L;
        }
    }
}
