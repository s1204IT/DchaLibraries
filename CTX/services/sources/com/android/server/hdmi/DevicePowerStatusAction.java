package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.mediatek.server.powerhal.PowerHalManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class DevicePowerStatusAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "DevicePowerStatusAction";
    private final List<IHdmiControlCallback> mCallbacks;
    private final int mTargetAddress;

    static DevicePowerStatusAction create(HdmiCecLocalDevice hdmiCecLocalDevice, int i, IHdmiControlCallback iHdmiControlCallback) {
        if (hdmiCecLocalDevice == null || iHdmiControlCallback == null) {
            Slog.e(TAG, "Wrong arguments");
            return null;
        }
        return new DevicePowerStatusAction(hdmiCecLocalDevice, i, iHdmiControlCallback);
    }

    private DevicePowerStatusAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, IHdmiControlCallback iHdmiControlCallback) {
        super(hdmiCecLocalDevice);
        this.mCallbacks = new ArrayList();
        this.mTargetAddress = i;
        addCallback(iHdmiControlCallback);
    }

    @Override
    boolean start() {
        queryDevicePowerStatus();
        this.mState = 1;
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
        return true;
    }

    private void queryDevicePowerStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), this.mTargetAddress));
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (this.mState != 1 || this.mTargetAddress != hdmiCecMessage.getSource() || hdmiCecMessage.getOpcode() != 144) {
            return false;
        }
        invokeCallback(hdmiCecMessage.getParams()[0]);
        finish();
        return true;
    }

    @Override
    void handleTimerEvent(int i) {
        if (this.mState == i && i == 1) {
            invokeCallback(-1);
            finish();
        }
    }

    public void addCallback(IHdmiControlCallback iHdmiControlCallback) {
        this.mCallbacks.add(iHdmiControlCallback);
    }

    private void invokeCallback(int i) {
        try {
            Iterator<IHdmiControlCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onComplete(i);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Callback failed:" + e);
        }
    }
}
