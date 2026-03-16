package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;

public class MonkeyCommandEvent extends MonkeyEvent {
    private String mCmd;

    public MonkeyCommandEvent(String cmd) {
        super(4);
        this.mCmd = cmd;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (this.mCmd != null) {
            try {
                Process p = Runtime.getRuntime().exec(this.mCmd);
                int status = p.waitFor();
                System.err.println("// Shell command " + this.mCmd + " status was " + status);
                return 1;
            } catch (Exception e) {
                System.err.println("// Exception from " + this.mCmd + ":");
                System.err.println(e.toString());
                return 1;
            }
        }
        return 1;
    }
}
