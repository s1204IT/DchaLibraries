package com.android.server;

import android.os.ConditionVariable;
import android.os.SystemClock;

abstract class ResettableTimeout {
    private ConditionVariable mLock = new ConditionVariable();
    private volatile long mOffAt;
    private volatile boolean mOffCalled;
    private Thread mThread;

    public abstract void off();

    public abstract void on(boolean z);

    ResettableTimeout() {
    }

    public void go(long j) {
        synchronized (this) {
            this.mOffAt = SystemClock.uptimeMillis() + j;
            boolean z = false;
            if (this.mThread == null) {
                this.mLock.close();
                this.mThread = new T();
                this.mThread.start();
                this.mLock.block();
                this.mOffCalled = false;
            } else {
                z = true;
                this.mThread.interrupt();
            }
            on(z);
        }
    }

    public void cancel() {
        synchronized (this) {
            this.mOffAt = 0L;
            if (this.mThread != null) {
                this.mThread.interrupt();
                this.mThread = null;
            }
            if (!this.mOffCalled) {
                this.mOffCalled = true;
                off();
            }
        }
    }

    private class T extends Thread {
        private T() {
        }

        @Override
        public void run() {
            long jUptimeMillis;
            ResettableTimeout.this.mLock.open();
            while (true) {
                synchronized (this) {
                    jUptimeMillis = ResettableTimeout.this.mOffAt - SystemClock.uptimeMillis();
                    if (jUptimeMillis <= 0) {
                        ResettableTimeout.this.mOffCalled = true;
                        ResettableTimeout.this.off();
                        ResettableTimeout.this.mThread = null;
                        return;
                    }
                }
                try {
                    sleep(jUptimeMillis);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
