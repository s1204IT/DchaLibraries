package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;

public class MonkeyThrottleEvent extends MonkeyEvent {
    private long mThrottle;

    public MonkeyThrottleEvent(long j) {
        super(6);
        this.mThrottle = j;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        if (i > 1) {
            Logger.out.println("Sleeping for " + this.mThrottle + " milliseconds");
        }
        try {
            Thread.sleep(this.mThrottle);
            return 1;
        } catch (InterruptedException e) {
            Logger.out.println("** Monkey interrupted in sleep.");
            return 0;
        }
    }
}
