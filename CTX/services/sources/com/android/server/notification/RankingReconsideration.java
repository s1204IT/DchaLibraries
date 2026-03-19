package com.android.server.notification;

import java.util.concurrent.TimeUnit;

public abstract class RankingReconsideration implements Runnable {
    private static final int CANCELLED = 3;
    private static final int DONE = 2;
    private static final long IMMEDIATE = 0;
    private static final int RUNNING = 1;
    private static final int START = 0;
    private long mDelay;
    protected String mKey;
    private int mState;

    public abstract void applyChangesLocked(NotificationRecord notificationRecord);

    public abstract void work();

    public RankingReconsideration(String str) {
        this(str, 0L);
    }

    public RankingReconsideration(String str, long j) {
        this.mDelay = j;
        this.mKey = str;
        this.mState = 0;
    }

    public String getKey() {
        return this.mKey;
    }

    @Override
    public void run() {
        if (this.mState == 0) {
            this.mState = 1;
            work();
            this.mState = 2;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public long getDelay(TimeUnit timeUnit) {
        return timeUnit.convert(this.mDelay, TimeUnit.MILLISECONDS);
    }

    public boolean cancel(boolean z) {
        if (this.mState == 0) {
            this.mState = 3;
            return true;
        }
        return false;
    }

    public boolean isCancelled() {
        return this.mState == 3;
    }

    public boolean isDone() {
        return this.mState == 2;
    }
}
