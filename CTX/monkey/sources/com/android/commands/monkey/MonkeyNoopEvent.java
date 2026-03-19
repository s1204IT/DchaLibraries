package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;

public class MonkeyNoopEvent extends MonkeyEvent {
    public MonkeyNoopEvent() {
        super(8);
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        if (i > 1) {
            Logger.out.println("NOOP");
        }
        return 1;
    }
}
