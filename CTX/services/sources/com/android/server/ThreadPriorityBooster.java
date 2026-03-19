package com.android.server;

import android.os.Process;

public class ThreadPriorityBooster {
    private static final boolean ENABLE_LOCK_GUARD = false;
    private volatile int mBoostToPriority;
    private final int mLockGuardIndex;
    private final ThreadLocal<PriorityState> mThreadState = new ThreadLocal<PriorityState>() {
        @Override
        protected PriorityState initialValue() {
            return new PriorityState();
        }
    };

    public ThreadPriorityBooster(int i, int i2) {
        this.mBoostToPriority = i;
        this.mLockGuardIndex = i2;
    }

    public void boost() {
        int iMyTid = Process.myTid();
        int threadPriority = Process.getThreadPriority(iMyTid);
        PriorityState priorityState = this.mThreadState.get();
        if (priorityState.regionCounter == 0) {
            priorityState.prevPriority = threadPriority;
            if (threadPriority > this.mBoostToPriority) {
                Process.setThreadPriority(iMyTid, this.mBoostToPriority);
            }
        }
        priorityState.regionCounter++;
    }

    public void reset() {
        PriorityState priorityState = this.mThreadState.get();
        priorityState.regionCounter--;
        int threadPriority = Process.getThreadPriority(Process.myTid());
        if (priorityState.regionCounter == 0 && priorityState.prevPriority != threadPriority) {
            Process.setThreadPriority(Process.myTid(), priorityState.prevPriority);
        }
    }

    protected void setBoostToPriority(int i) {
        this.mBoostToPriority = i;
        PriorityState priorityState = this.mThreadState.get();
        int iMyTid = Process.myTid();
        int threadPriority = Process.getThreadPriority(iMyTid);
        if (priorityState.regionCounter != 0 && threadPriority != i) {
            Process.setThreadPriority(iMyTid, i);
        }
    }

    private static class PriorityState {
        int prevPriority;
        int regionCounter;

        private PriorityState() {
        }
    }
}
