package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
import com.mediatek.server.powerhal.PowerHalManager;

final class DeviceSelectAction extends HdmiCecFeatureAction {
    private static final int LOOP_COUNTER_MAX = 20;
    private static final int STATE_WAIT_FOR_DEVICE_POWER_ON = 3;
    private static final int STATE_WAIT_FOR_DEVICE_TO_TRANSIT_TO_STANDBY = 2;
    private static final int STATE_WAIT_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "DeviceSelect";
    private static final int TIMEOUT_POWER_ON_MS = 5000;
    private static final int TIMEOUT_TRANSIT_TO_STANDBY_MS = 5000;
    private final IHdmiControlCallback mCallback;
    private final HdmiCecMessage mGivePowerStatus;
    private int mPowerStatusCounter;
    private final HdmiDeviceInfo mTarget;

    public DeviceSelectAction(HdmiCecLocalDeviceTv hdmiCecLocalDeviceTv, HdmiDeviceInfo hdmiDeviceInfo, IHdmiControlCallback iHdmiControlCallback) {
        super(hdmiCecLocalDeviceTv);
        this.mPowerStatusCounter = 0;
        this.mCallback = iHdmiControlCallback;
        this.mTarget = hdmiDeviceInfo;
        this.mGivePowerStatus = HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), getTargetAddress());
    }

    int getTargetAddress() {
        return this.mTarget.getLogicalAddress();
    }

    @Override
    public boolean start() {
        queryDevicePowerStatus();
        return true;
    }

    private void queryDevicePowerStatus() {
        sendCommand(this.mGivePowerStatus, new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int i) {
                if (i != 0) {
                    DeviceSelectAction.this.invokeCallback(7);
                    DeviceSelectAction.this.finish();
                }
            }
        });
        this.mState = 1;
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
    }

    @Override
    public boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (hdmiCecMessage.getSource() != getTargetAddress()) {
            return false;
        }
        int opcode = hdmiCecMessage.getOpcode();
        byte[] params = hdmiCecMessage.getParams();
        if (this.mState == 1 && opcode == 144) {
            return handleReportPowerStatus(params[0]);
        }
        return false;
    }

    private boolean handleReportPowerStatus(int i) {
        switch (i) {
            case 0:
                sendSetStreamPath();
                break;
            case 1:
                if (this.mPowerStatusCounter == 0) {
                    turnOnDevice();
                } else {
                    sendSetStreamPath();
                }
                break;
            case 2:
                if (this.mPowerStatusCounter < 20) {
                    this.mState = 3;
                    addTimer(this.mState, 5000);
                } else {
                    sendSetStreamPath();
                }
                break;
            case 3:
                if (this.mPowerStatusCounter < 4) {
                    this.mState = 2;
                    addTimer(this.mState, 5000);
                } else {
                    sendSetStreamPath();
                }
                break;
        }
        return true;
    }

    private void turnOnDevice() {
        sendUserControlPressedAndReleased(this.mTarget.getLogicalAddress(), 64);
        sendUserControlPressedAndReleased(this.mTarget.getLogicalAddress(), HdmiCecKeycode.CEC_KEYCODE_POWER_ON_FUNCTION);
        this.mState = 3;
        addTimer(this.mState, 5000);
    }

    private void sendSetStreamPath() {
        tv().getActiveSource().invalidate();
        tv().setActivePath(this.mTarget.getPhysicalAddress());
        sendCommand(HdmiCecMessageBuilder.buildSetStreamPath(getSourceAddress(), this.mTarget.getPhysicalAddress()));
        invokeCallback(0);
        finish();
    }

    @Override
    public void handleTimerEvent(int i) {
        if (this.mState != i) {
            Slog.w(TAG, "Timer in a wrong state. Ignored.");
        }
        switch (this.mState) {
            case 1:
                if (tv().isPowerStandbyOrTransient()) {
                    invokeCallback(6);
                    finish();
                } else {
                    sendSetStreamPath();
                }
                break;
            case 2:
            case 3:
                this.mPowerStatusCounter++;
                queryDevicePowerStatus();
                break;
        }
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
