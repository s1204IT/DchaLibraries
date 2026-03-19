package com.android.server.hdmi;

import android.hardware.hdmi.HdmiPortInfo;
import android.util.SparseArray;
import com.android.internal.util.IndentingPrintWriter;

final class HdmiMhlControllerStub {
    private static final int INVALID_DEVICE_ROLES = 0;
    private static final int INVALID_MHL_VERSION = 0;
    private static final int NO_SUPPORTED_FEATURES = 0;
    private static final SparseArray<HdmiMhlLocalDeviceStub> mLocalDevices = new SparseArray<>();
    private static final HdmiPortInfo[] EMPTY_PORT_INFO = new HdmiPortInfo[0];

    private HdmiMhlControllerStub(HdmiControlService hdmiControlService) {
    }

    boolean isReady() {
        return false;
    }

    static HdmiMhlControllerStub create(HdmiControlService hdmiControlService) {
        return new HdmiMhlControllerStub(hdmiControlService);
    }

    HdmiPortInfo[] getPortInfos() {
        return EMPTY_PORT_INFO;
    }

    HdmiMhlLocalDeviceStub getLocalDevice(int i) {
        return null;
    }

    HdmiMhlLocalDeviceStub getLocalDeviceById(int i) {
        return null;
    }

    SparseArray<HdmiMhlLocalDeviceStub> getAllLocalDevices() {
        return mLocalDevices;
    }

    HdmiMhlLocalDeviceStub removeLocalDevice(int i) {
        return null;
    }

    HdmiMhlLocalDeviceStub addLocalDevice(HdmiMhlLocalDeviceStub hdmiMhlLocalDeviceStub) {
        return null;
    }

    void clearAllLocalDevices() {
    }

    void sendVendorCommand(int i, int i2, int i3, byte[] bArr) {
    }

    void setOption(int i, int i2) {
    }

    int getMhlVersion(int i) {
        return 0;
    }

    int getPeerMhlVersion(int i) {
        return 0;
    }

    int getSupportedFeatures(int i) {
        return 0;
    }

    int getEcbusDeviceRoles(int i) {
        return 0;
    }

    void dump(IndentingPrintWriter indentingPrintWriter) {
    }
}
