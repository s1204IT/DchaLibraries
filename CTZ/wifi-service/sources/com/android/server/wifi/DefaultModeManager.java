package com.android.server.wifi;

import android.content.Context;
import android.os.Looper;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DefaultModeManager implements ActiveModeManager {
    private static final String TAG = "WifiDefaultModeManager";
    private final Context mContext;

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    DefaultModeManager(Context context, Looper looper) {
        this.mContext = context;
    }
}
