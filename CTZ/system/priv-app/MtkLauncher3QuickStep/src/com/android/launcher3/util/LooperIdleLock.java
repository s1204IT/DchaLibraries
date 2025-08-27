package com.android.launcher3.util;

import android.os.Looper;
import android.os.MessageQueue;
import com.android.launcher3.Utilities;

/* loaded from: classes.dex */
public class LooperIdleLock implements MessageQueue.IdleHandler, Runnable {
    private boolean mIsLocked = true;
    private final Object mLock;

    public LooperIdleLock(Object obj, Looper looper) {
        this.mLock = obj;
        if (Utilities.ATLEAST_MARSHMALLOW) {
            looper.getQueue().addIdleHandler(this);
        } else {
            new LooperExecutor(looper).execute(this);
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        Looper.myQueue().addIdleHandler(this);
    }

    @Override // android.os.MessageQueue.IdleHandler
    public boolean queueIdle() {
        synchronized (this.mLock) {
            this.mIsLocked = false;
            this.mLock.notify();
        }
        return false;
    }

    public boolean awaitLocked(long j) throws InterruptedException {
        if (this.mIsLocked) {
            try {
                this.mLock.wait(j);
            } catch (InterruptedException e) {
            }
        }
        return this.mIsLocked;
    }
}
