package com.android.server.wifi.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;

public class WifiHandler extends Handler {
    private static final String LOG_TAG = "WifiHandler";
    private WifiLog mLog;
    private String mTag;

    public WifiHandler(String str, Looper looper) {
        super(looper);
        this.mTag = "WifiHandler." + str;
    }

    private WifiLog getOrInitLog() {
        if (this.mLog == null) {
            this.mLog = WifiInjector.getInstance().makeLog(this.mTag);
        }
        return this.mLog;
    }

    @Override
    public void handleMessage(Message message) {
        getOrInitLog().trace("Received message=%d sendingUid=%").c(message.what).c(message.sendingUid).flush();
    }

    @VisibleForTesting
    public void setWifiLog(WifiLog wifiLog) {
        this.mLog = wifiLog;
    }
}
