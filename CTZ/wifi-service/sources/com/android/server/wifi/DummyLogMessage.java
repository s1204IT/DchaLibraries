package com.android.server.wifi;

import com.android.server.wifi.WifiLog;

public class DummyLogMessage implements WifiLog.LogMessage {
    @Override
    public WifiLog.LogMessage r(String str) {
        return this;
    }

    @Override
    public WifiLog.LogMessage c(String str) {
        return this;
    }

    @Override
    public WifiLog.LogMessage c(long j) {
        return this;
    }

    @Override
    public WifiLog.LogMessage c(char c) {
        return this;
    }

    @Override
    public WifiLog.LogMessage c(boolean z) {
        return this;
    }

    @Override
    public void flush() {
    }
}
