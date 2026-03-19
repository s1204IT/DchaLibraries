package android.telecom.Logging;

import android.telecom.Log;

public abstract class Runnable {
    private final Object mLock;
    private final java.lang.Runnable mRunnable = new java.lang.Runnable() {
        @Override
        public void run() {
            synchronized (Runnable.this.mLock) {
                try {
                    Log.continueSession(Runnable.this.mSubsession, Runnable.this.mSubsessionName);
                    Runnable.this.loggedRun();
                } finally {
                    if (Runnable.this.mSubsession != null) {
                        Log.endSession();
                        Runnable.this.mSubsession = null;
                    }
                }
            }
        }
    };
    private Session mSubsession;
    private final String mSubsessionName;

    public abstract void loggedRun();

    public Runnable(String str, Object obj) {
        if (obj == null) {
            this.mLock = new Object();
        } else {
            this.mLock = obj;
        }
        this.mSubsessionName = str;
    }

    public final java.lang.Runnable getRunnableToCancel() {
        return this.mRunnable;
    }

    public java.lang.Runnable prepare() {
        cancel();
        this.mSubsession = Log.createSubsession();
        return this.mRunnable;
    }

    public void cancel() {
        synchronized (this.mLock) {
            Log.cancelSubsession(this.mSubsession);
            this.mSubsession = null;
        }
    }
}
