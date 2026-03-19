package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;

public class MonkeyWaitEvent extends MonkeyEvent {
    private long mWaitTime;

    public MonkeyWaitEvent(long j) {
        super(6);
        this.mWaitTime = j;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        if (i > 1) {
            Logger.out.println("Wait Event for " + this.mWaitTime + " milliseconds");
        }
        try {
            Thread.sleep(this.mWaitTime);
            return 1;
        } catch (InterruptedException e) {
            Logger.out.println("** Monkey interrupted in sleep.");
            return 0;
        }
    }
}
