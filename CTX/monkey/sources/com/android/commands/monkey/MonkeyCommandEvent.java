package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;

public class MonkeyCommandEvent extends MonkeyEvent {
    private String mCmd;

    public MonkeyCommandEvent(String str) {
        super(4);
        this.mCmd = str;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        if (this.mCmd != null) {
            try {
                int iWaitFor = Runtime.getRuntime().exec(this.mCmd).waitFor();
                Logger.err.println("// Shell command " + this.mCmd + " status was " + iWaitFor);
                return 1;
            } catch (Exception e) {
                Logger.err.println("// Exception from " + this.mCmd + ":");
                Logger.err.println(e.toString());
                return 1;
            }
        }
        return 1;
    }
}
