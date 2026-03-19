package com.android.documentsui.selection;

import android.support.v4.util.Preconditions;

public final class ContentLock {
    private Runnable mCallback;
    private int mLocks = 0;

    public synchronized void block() {
        this.mLocks++;
    }

    public synchronized void unblock() {
        Preconditions.checkState(this.mLocks > 0);
        this.mLocks--;
        if (this.mLocks == 0 && this.mCallback != null) {
            this.mCallback.run();
            this.mCallback = null;
        }
    }

    public synchronized void runWhenUnlocked(Runnable runnable) {
        if (this.mLocks == 0) {
            runnable.run();
        } else {
            this.mCallback = runnable;
        }
    }

    synchronized boolean isLocked() {
        return this.mLocks > 0;
    }

    final void checkLocked() {
        Preconditions.checkState(isLocked());
    }

    final void checkUnlocked() {
        Preconditions.checkState(!isLocked());
    }
}
