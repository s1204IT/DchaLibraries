package com.android.systemui.recents.misc;

import android.os.Handler;

public class DozeTrigger {
    int mDozeDurationSeconds;
    Runnable mDozeRunnable = new Runnable() {
        @Override
        public void run() {
            DozeTrigger.this.mSleepRunnable.run();
            DozeTrigger.this.mIsDozing = false;
            DozeTrigger.this.mHasTriggered = true;
        }
    };
    Handler mHandler = new Handler();
    boolean mHasTriggered;
    boolean mIsDozing;
    Runnable mSleepRunnable;

    public DozeTrigger(int dozeDurationSeconds, Runnable sleepRunnable) {
        this.mDozeDurationSeconds = dozeDurationSeconds;
        this.mSleepRunnable = sleepRunnable;
    }

    public void startDozing() {
        forcePoke();
        this.mHasTriggered = false;
    }

    public void stopDozing() {
        this.mHandler.removeCallbacks(this.mDozeRunnable);
        this.mIsDozing = false;
    }

    public void poke() {
        if (this.mIsDozing) {
            forcePoke();
        }
    }

    void forcePoke() {
        this.mHandler.removeCallbacks(this.mDozeRunnable);
        this.mHandler.postDelayed(this.mDozeRunnable, this.mDozeDurationSeconds * 1000);
        this.mIsDozing = true;
    }

    public boolean hasTriggered() {
        return this.mHasTriggered;
    }

    public void resetTrigger() {
        this.mHasTriggered = false;
    }
}
