package com.android.server.am;

final class AppErrorResult {
    boolean mHasResult = false;
    int mResult;

    AppErrorResult() {
    }

    public void set(int i) {
        synchronized (this) {
            this.mHasResult = true;
            this.mResult = i;
            notifyAll();
        }
    }

    public int get() {
        synchronized (this) {
            while (!this.mHasResult) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return this.mResult;
    }
}
