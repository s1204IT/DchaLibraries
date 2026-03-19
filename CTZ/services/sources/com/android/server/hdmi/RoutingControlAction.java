package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;

final class RoutingControlAction extends HdmiCecFeatureAction {
    private static final int STATE_WAIT_FOR_REPORT_POWER_STATUS = 2;
    private static final int STATE_WAIT_FOR_ROUTING_INFORMATION = 1;
    private static final String TAG = "RoutingControlAction";
    private static final int TIMEOUT_REPORT_POWER_STATUS_MS = 1000;
    private static final int TIMEOUT_ROUTING_INFORMATION_MS = 1000;
    private final IHdmiControlCallback mCallback;
    private int mCurrentRoutingPath;
    private final boolean mNotifyInputChange;
    private final boolean mQueryDevicePowerStatus;

    RoutingControlAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, boolean z, IHdmiControlCallback iHdmiControlCallback) {
        super(hdmiCecLocalDevice);
        this.mCallback = iHdmiControlCallback;
        this.mCurrentRoutingPath = i;
        this.mQueryDevicePowerStatus = z;
        this.mNotifyInputChange = iHdmiControlCallback == null;
    }

    @Override
    public boolean start() {
        this.mState = 1;
        addTimer(this.mState, 1000);
        return true;
    }

    @Override
    public boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        int opcode = hdmiCecMessage.getOpcode();
        byte[] params = hdmiCecMessage.getParams();
        if (this.mState == 1 && opcode == 129) {
            int iTwoBytesToInt = HdmiUtils.twoBytesToInt(params);
            if (!HdmiUtils.isInActiveRoutingPath(this.mCurrentRoutingPath, iTwoBytesToInt)) {
                return true;
            }
            this.mCurrentRoutingPath = iTwoBytesToInt;
            removeActionExcept(RoutingControlAction.class, this);
            addTimer(this.mState, 1000);
            return true;
        }
        if (this.mState != 2 || opcode != 144) {
            return false;
        }
        handleReportPowerStatus(hdmiCecMessage.getParams()[0]);
        return true;
    }

    private void handleReportPowerStatus(int i) {
        if (isPowerOnOrTransient(getTvPowerStatus())) {
            updateActiveInput();
            if (isPowerOnOrTransient(i)) {
                sendSetStreamPath();
            }
        }
        finishWithCallback(0);
    }

    private void updateActiveInput() {
        HdmiCecLocalDeviceTv hdmiCecLocalDeviceTvTv = tv();
        hdmiCecLocalDeviceTvTv.setPrevPortId(hdmiCecLocalDeviceTvTv.getActivePortId());
        hdmiCecLocalDeviceTvTv.updateActiveInput(this.mCurrentRoutingPath, this.mNotifyInputChange);
    }

    private int getTvPowerStatus() {
        return tv().getPowerStatus();
    }

    private static boolean isPowerOnOrTransient(int i) {
        return i == 0 || i == 2;
    }

    private void sendSetStreamPath() {
        sendCommand(HdmiCecMessageBuilder.buildSetStreamPath(getSourceAddress(), this.mCurrentRoutingPath));
    }

    private void finishWithCallback(int i) {
        invokeCallback(i);
        finish();
    }

    @Override
    public void handleTimerEvent(int i) {
        if (this.mState != i || this.mState == 0) {
            Slog.w("CEC", "Timer in a wrong state. Ignored.");
            return;
        }
        switch (i) {
            case 1:
                HdmiDeviceInfo deviceInfoByPath = tv().getDeviceInfoByPath(this.mCurrentRoutingPath);
                if (deviceInfoByPath != null && this.mQueryDevicePowerStatus) {
                    queryDevicePowerStatus(deviceInfoByPath.getLogicalAddress(), new HdmiControlService.SendMessageCallback() {
                        @Override
                        public void onSendCompleted(int i2) {
                            RoutingControlAction.this.handlDevicePowerStatusAckResult(i2 == 0);
                        }
                    });
                } else {
                    updateActiveInput();
                    finishWithCallback(0);
                }
                break;
            case 2:
                if (isPowerOnOrTransient(getTvPowerStatus())) {
                    updateActiveInput();
                    sendSetStreamPath();
                }
                finishWithCallback(0);
                break;
        }
    }

    private void queryDevicePowerStatus(int i, HdmiControlService.SendMessageCallback sendMessageCallback) {
        sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), i), sendMessageCallback);
    }

    private void handlDevicePowerStatusAckResult(boolean z) {
        if (z) {
            this.mState = 2;
            addTimer(this.mState, 1000);
        } else {
            updateActiveInput();
            sendSetStreamPath();
            finishWithCallback(0);
        }
    }

    private void invokeCallback(int i) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.onComplete(i);
        } catch (RemoteException e) {
        }
    }
}
