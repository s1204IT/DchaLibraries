package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.hdmi.HdmiCecLocalDevice;

final class ActiveSourceHandler {
    private static final String TAG = "ActiveSourceHandler";
    private final IHdmiControlCallback mCallback;
    private final HdmiControlService mService;
    private final HdmiCecLocalDeviceTv mSource;

    static ActiveSourceHandler create(HdmiCecLocalDeviceTv source, IHdmiControlCallback callback) {
        if (source != null) {
            return new ActiveSourceHandler(source, callback);
        }
        Slog.e(TAG, "Wrong arguments");
        return null;
    }

    private ActiveSourceHandler(HdmiCecLocalDeviceTv source, IHdmiControlCallback callback) {
        this.mSource = source;
        this.mService = this.mSource.getService();
        this.mCallback = callback;
    }

    void process(HdmiCecLocalDevice.ActiveSource newActive, int deviceType) {
        HdmiCecLocalDeviceTv tv = this.mSource;
        HdmiDeviceInfo device = this.mService.getDeviceInfo(newActive.logicalAddress);
        if (device == null) {
            tv.startNewDeviceAction(newActive, deviceType);
        }
        if (!tv.isProhibitMode()) {
            tv.updateActiveSource(newActive);
            boolean notifyInputChange = this.mCallback == null;
            tv.updateActiveInput(newActive.physicalAddress, notifyInputChange);
            invokeCallback(0);
            return;
        }
        HdmiCecLocalDevice.ActiveSource current = tv.getActiveSource();
        if (current.logicalAddress == getSourceAddress()) {
            HdmiCecMessage activeSourceCommand = HdmiCecMessageBuilder.buildActiveSource(current.logicalAddress, current.physicalAddress);
            this.mService.sendCecCommand(activeSourceCommand);
            tv.updateActiveSource(current);
            invokeCallback(0);
            return;
        }
        tv.startRoutingControl(newActive.physicalAddress, current.physicalAddress, true, this.mCallback);
    }

    private final int getSourceAddress() {
        return this.mSource.getDeviceInfo().getLogicalAddress();
    }

    private void invokeCallback(int result) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onComplete(result);
            } catch (RemoteException e) {
                Slog.e(TAG, "Callback failed:" + e);
            }
        }
    }
}
