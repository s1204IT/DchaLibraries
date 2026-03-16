package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;

final class OneTouchPlayAction extends HdmiCecFeatureAction {
    private static final int LOOP_COUNTER_MAX = 10;
    private static final int STATE_WAITING_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "OneTouchPlayAction";
    private final IHdmiControlCallback mCallback;
    private int mPowerStatusCounter;
    private final int mTargetAddress;

    static OneTouchPlayAction create(HdmiCecLocalDevicePlayback source, int targetAddress, IHdmiControlCallback callback) {
        if (source != null && callback != null) {
            return new OneTouchPlayAction(source, targetAddress, callback);
        }
        Slog.e(TAG, "Wrong arguments");
        return null;
    }

    private OneTouchPlayAction(HdmiCecLocalDevice localDevice, int targetAddress, IHdmiControlCallback callback) {
        super(localDevice);
        this.mPowerStatusCounter = 0;
        this.mTargetAddress = targetAddress;
        this.mCallback = callback;
    }

    @Override
    boolean start() {
        sendCommand(HdmiCecMessageBuilder.buildTextViewOn(getSourceAddress(), this.mTargetAddress));
        broadcastActiveSource();
        queryDevicePowerStatus();
        this.mState = 1;
        addTimer(this.mState, 2000);
        return true;
    }

    private void broadcastActiveSource() {
        sendCommand(HdmiCecMessageBuilder.buildActiveSource(getSourceAddress(), getSourcePath()));
        playback().setActiveSource(true);
    }

    private void queryDevicePowerStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), this.mTargetAddress));
    }

    @Override
    boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1 || this.mTargetAddress != cmd.getSource()) {
            return false;
        }
        if (cmd.getOpcode() != 144) {
            return false;
        }
        int status = cmd.getParams()[0];
        if (status != 0) {
            return true;
        }
        broadcastActiveSource();
        invokeCallback(0);
        finish();
        return true;
    }

    @Override
    void handleTimerEvent(int state) {
        if (this.mState == state && state == 1) {
            int i = this.mPowerStatusCounter;
            this.mPowerStatusCounter = i + 1;
            if (i < 10) {
                queryDevicePowerStatus();
                addTimer(this.mState, 2000);
            } else {
                invokeCallback(1);
                finish();
            }
        }
    }

    private void invokeCallback(int result) {
        try {
            this.mCallback.onComplete(result);
        } catch (RemoteException e) {
            Slog.e(TAG, "Callback failed:" + e);
        }
    }
}
