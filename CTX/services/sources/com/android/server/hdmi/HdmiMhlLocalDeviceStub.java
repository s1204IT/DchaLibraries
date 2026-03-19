package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.net.util.NetworkConstants;

final class HdmiMhlLocalDeviceStub {
    private static final HdmiDeviceInfo INFO = new HdmiDeviceInfo(NetworkConstants.ARP_HWTYPE_RESERVED_HI, -1, -1, -1);
    private final int mPortId;
    private final HdmiControlService mService;

    protected HdmiMhlLocalDeviceStub(HdmiControlService hdmiControlService, int i) {
        this.mService = hdmiControlService;
        this.mPortId = i;
    }

    void onDeviceRemoved() {
    }

    HdmiDeviceInfo getInfo() {
        return INFO;
    }

    void setBusMode(int i) {
    }

    void onBusOvercurrentDetected(boolean z) {
    }

    void setDeviceStatusChange(int i, int i2) {
    }

    int getPortId() {
        return this.mPortId;
    }

    void turnOn(IHdmiControlCallback iHdmiControlCallback) {
    }

    void sendKeyEvent(int i, boolean z) {
    }

    void sendStandby() {
    }
}
