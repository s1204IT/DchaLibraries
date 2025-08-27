package com.android.systemui.recents.misc;

import android.os.Handler;
import android.view.ViewDebug;

/* loaded from: classes.dex */
public class DozeTrigger {

    @ViewDebug.ExportedProperty(category = "recents")
    int mDozeDurationMilliseconds;
    Runnable mDozeRunnable = new Runnable() { // from class: com.android.systemui.recents.misc.DozeTrigger.1
        @Override // java.lang.Runnable
        public void run() {
            DozeTrigger.this.mIsDozing = false;
            DozeTrigger.this.mIsAsleep = true;
            DozeTrigger.this.mOnSleepRunnable.run();
        }
    };
    Handler mHandler = new Handler();

    @ViewDebug.ExportedProperty(category = "recents")
    boolean mIsAsleep;

    @ViewDebug.ExportedProperty(category = "recents")
    boolean mIsDozing;
    Runnable mOnSleepRunnable;

    public DozeTrigger(int i, Runnable runnable) {
        this.mDozeDurationMilliseconds = i;
        this.mOnSleepRunnable = runnable;
    }

    public void startDozing() {
        forcePoke();
        this.mIsAsleep = false;
    }

    public void stopDozing() {
        this.mHandler.removeCallbacks(this.mDozeRunnable);
        this.mIsDozing = false;
        this.mIsAsleep = false;
    }

    public void poke() {
        if (this.mIsDozing) {
            forcePoke();
        }
    }

    void forcePoke() {
        this.mHandler.removeCallbacks(this.mDozeRunnable);
        this.mHandler.postDelayed(this.mDozeRunnable, this.mDozeDurationMilliseconds);
        this.mIsDozing = true;
    }

    public boolean isDozing() {
        return this.mIsDozing;
    }

    public boolean isAsleep() {
        return this.mIsAsleep;
    }
}
