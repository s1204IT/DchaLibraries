package com.android.server.wifi;

import com.android.server.wifi.WifiLog;

public class FakeWifiLog implements WifiLog {
    private static final DummyLogMessage sDummyLogMessage = new DummyLogMessage();

    @Override
    public WifiLog.LogMessage err(String str) {
        return sDummyLogMessage;
    }

    @Override
    public WifiLog.LogMessage warn(String str) {
        return sDummyLogMessage;
    }

    @Override
    public WifiLog.LogMessage info(String str) {
        return sDummyLogMessage;
    }

    @Override
    public WifiLog.LogMessage trace(String str) {
        return sDummyLogMessage;
    }

    @Override
    public WifiLog.LogMessage trace(String str, int i) {
        return sDummyLogMessage;
    }

    @Override
    public WifiLog.LogMessage dump(String str) {
        return sDummyLogMessage;
    }

    @Override
    public void eC(String str) {
    }

    @Override
    public void wC(String str) {
    }

    @Override
    public void iC(String str) {
    }

    @Override
    public void tC(String str) {
    }

    @Override
    public void e(String str) {
    }

    @Override
    public void w(String str) {
    }

    @Override
    public void i(String str) {
    }

    @Override
    public void d(String str) {
    }

    @Override
    public void v(String str) {
    }
}
