package gov.nist.javax.sip.stack;

import java.util.TimerTask;

public abstract class SIPStackTimerTask extends TimerTask {
    protected abstract void runTask();

    @Override
    public final void run() {
        try {
            runTask();
        } catch (Throwable th) {
            System.out.println("SIP stack timer task failed due to exception:");
            th.printStackTrace();
        }
    }
}
