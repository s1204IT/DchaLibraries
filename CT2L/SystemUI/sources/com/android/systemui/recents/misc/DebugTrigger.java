package com.android.systemui.recents.misc;

import android.os.Handler;

public class DebugTrigger {
    Handler mHandler = new Handler();
    Runnable mTriggeredRunnable;

    public DebugTrigger(Runnable triggeredRunnable) {
        this.mTriggeredRunnable = triggeredRunnable;
    }

    public void onKeyEvent(int keyCode) {
    }
}
