package com.android.server.net;

import android.net.INetdEventCallback;

public class BaseNetdEventCallback extends INetdEventCallback.Stub {
    @Override
    public void onDnsEvent(String str, String[] strArr, int i, long j, int i2) {
    }

    @Override
    public void onPrivateDnsValidationEvent(int i, String str, String str2, boolean z) {
    }

    @Override
    public void onConnectEvent(String str, int i, long j, int i2) {
    }
}
