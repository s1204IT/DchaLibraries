package com.android.server;

import android.os.HandlerThread;
import android.os.Process;
import android.os.StrictMode;

public class ServiceThread extends HandlerThread {
    private static final String TAG = "ServiceThread";
    private final boolean mAllowIo;

    public ServiceThread(String str, int i, boolean z) {
        super(str, i);
        this.mAllowIo = z;
    }

    @Override
    public void run() {
        Process.setCanSelfBackground(false);
        if (!this.mAllowIo) {
            StrictMode.initThreadDefaults(null);
        }
        super.run();
    }
}
