package com.android.server.wm;

import android.os.Process;
import com.android.internal.annotations.GuardedBy;
import com.android.server.AnimationThread;
import com.android.server.ThreadPriorityBooster;

class WindowManagerThreadPriorityBooster extends ThreadPriorityBooster {
    private final int mAnimationThreadId;

    @GuardedBy("mLock")
    private boolean mAppTransitionRunning;

    @GuardedBy("mLock")
    private boolean mBoundsAnimationRunning;
    private final Object mLock;
    private final int mSurfaceAnimationThreadId;

    WindowManagerThreadPriorityBooster() {
        super(-4, 5);
        this.mLock = new Object();
        this.mAnimationThreadId = AnimationThread.get().getThreadId();
        this.mSurfaceAnimationThreadId = SurfaceAnimationThread.get().getThreadId();
    }

    @Override
    public void boost() {
        int iMyTid = Process.myTid();
        if (iMyTid == this.mAnimationThreadId || iMyTid == this.mSurfaceAnimationThreadId) {
            return;
        }
        super.boost();
    }

    @Override
    public void reset() {
        int iMyTid = Process.myTid();
        if (iMyTid == this.mAnimationThreadId || iMyTid == this.mSurfaceAnimationThreadId) {
            return;
        }
        super.reset();
    }

    void setAppTransitionRunning(boolean z) {
        synchronized (this.mLock) {
            if (this.mAppTransitionRunning != z) {
                this.mAppTransitionRunning = z;
                updatePriorityLocked();
            }
        }
    }

    void setBoundsAnimationRunning(boolean z) {
        synchronized (this.mLock) {
            if (this.mBoundsAnimationRunning != z) {
                this.mBoundsAnimationRunning = z;
                updatePriorityLocked();
            }
        }
    }

    @GuardedBy("mLock")
    private void updatePriorityLocked() {
        int i = (this.mAppTransitionRunning || this.mBoundsAnimationRunning) ? -10 : -4;
        setBoostToPriority(i);
        Process.setThreadPriority(this.mAnimationThreadId, i);
        Process.setThreadPriority(this.mSurfaceAnimationThreadId, i);
    }
}
