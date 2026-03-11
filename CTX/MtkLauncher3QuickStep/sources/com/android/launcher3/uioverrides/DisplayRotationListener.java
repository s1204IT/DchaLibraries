package com.android.launcher3.uioverrides;

import android.content.Context;
import android.os.Handler;
import com.android.systemui.shared.system.RotationWatcher;

public class DisplayRotationListener extends RotationWatcher {
    private final Runnable mCallback;
    private Handler mHandler;

    public DisplayRotationListener(Context context, Runnable runnable) {
        super(context);
        this.mCallback = runnable;
    }

    @Override
    public void enable() {
        if (this.mHandler == null) {
            this.mHandler = new Handler();
        }
        super.enable();
    }

    @Override
    protected void onRotationChanged(int i) {
        this.mHandler.post(this.mCallback);
    }
}
