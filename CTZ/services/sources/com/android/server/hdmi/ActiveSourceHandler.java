package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.hdmi.HdmiCecLocalDevice;

final class ActiveSourceHandler {
    private static final String TAG = "ActiveSourceHandler";
    private final IHdmiControlCallback mCallback;
    private final HdmiControlService mService;
    private final HdmiCecLocalDeviceTv mSource;

    static ActiveSourceHandler create(HdmiCecLocalDeviceTv hdmiCecLocalDeviceTv, IHdmiControlCallback iHdmiControlCallback) {
        if (hdmiCecLocalDeviceTv == null) {
            Slog.e(TAG, "Wrong arguments");
            return null;
        }
        return new ActiveSourceHandler(hdmiCecLocalDeviceTv, iHdmiControlCallback);
    }

    private ActiveSourceHandler(HdmiCecLocalDeviceTv hdmiCecLocalDeviceTv, IHdmiControlCallback iHdmiControlCallback) {
        this.mSource = hdmiCecLocalDeviceTv;
        this.mService = this.mSource.getService();
        this.mCallback = iHdmiControlCallback;
    }

    void process(HdmiCecLocalDevice.ActiveSource activeSource, int i) {
        HdmiCecLocalDeviceTv hdmiCecLocalDeviceTv = this.mSource;
        if (this.mService.getDeviceInfo(activeSource.logicalAddress) == null) {
            hdmiCecLocalDeviceTv.startNewDeviceAction(activeSource, i);
        }
        if (!hdmiCecLocalDeviceTv.isProhibitMode()) {
            HdmiCecLocalDevice.ActiveSource activeSourceOf = HdmiCecLocalDevice.ActiveSource.of(hdmiCecLocalDeviceTv.getActiveSource());
            hdmiCecLocalDeviceTv.updateActiveSource(activeSource);
            boolean z = this.mCallback == null;
            if (!activeSourceOf.equals(activeSource)) {
                hdmiCecLocalDeviceTv.setPrevPortId(hdmiCecLocalDeviceTv.getActivePortId());
            }
            hdmiCecLocalDeviceTv.updateActiveInput(activeSource.physicalAddress, z);
            invokeCallback(0);
            return;
        }
        HdmiCecLocalDevice.ActiveSource activeSource2 = hdmiCecLocalDeviceTv.getActiveSource();
        if (activeSource2.logicalAddress == getSourceAddress()) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(activeSource2.logicalAddress, activeSource2.physicalAddress));
            hdmiCecLocalDeviceTv.updateActiveSource(activeSource2);
            invokeCallback(0);
            return;
        }
        hdmiCecLocalDeviceTv.startRoutingControl(activeSource.physicalAddress, activeSource2.physicalAddress, true, this.mCallback);
    }

    private final int getSourceAddress() {
        return this.mSource.getDeviceInfo().getLogicalAddress();
    }

    private void invokeCallback(int i) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.onComplete(i);
        } catch (RemoteException e) {
            Slog.e(TAG, "Callback failed:" + e);
        }
    }
}
