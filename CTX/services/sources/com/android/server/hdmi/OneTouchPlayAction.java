package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.mediatek.server.powerhal.PowerHalManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class OneTouchPlayAction extends HdmiCecFeatureAction {
    private static final int LOOP_COUNTER_MAX = 10;
    private static final int STATE_WAITING_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "OneTouchPlayAction";
    private final List<IHdmiControlCallback> mCallbacks;
    private int mPowerStatusCounter;
    private final int mTargetAddress;

    static OneTouchPlayAction create(HdmiCecLocalDevicePlayback hdmiCecLocalDevicePlayback, int i, IHdmiControlCallback iHdmiControlCallback) {
        if (hdmiCecLocalDevicePlayback == null || iHdmiControlCallback == null) {
            Slog.e(TAG, "Wrong arguments");
            return null;
        }
        return new OneTouchPlayAction(hdmiCecLocalDevicePlayback, i, iHdmiControlCallback);
    }

    private OneTouchPlayAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, IHdmiControlCallback iHdmiControlCallback) {
        super(hdmiCecLocalDevice);
        this.mCallbacks = new ArrayList();
        this.mPowerStatusCounter = 0;
        this.mTargetAddress = i;
        addCallback(iHdmiControlCallback);
    }

    @Override
    boolean start() {
        sendCommand(HdmiCecMessageBuilder.buildTextViewOn(getSourceAddress(), this.mTargetAddress));
        broadcastActiveSource();
        queryDevicePowerStatus();
        this.mState = 1;
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
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
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (this.mState != 1 || this.mTargetAddress != hdmiCecMessage.getSource() || hdmiCecMessage.getOpcode() != 144) {
            return false;
        }
        if (hdmiCecMessage.getParams()[0] == 0) {
            broadcastActiveSource();
            invokeCallback(0);
            finish();
        }
        return true;
    }

    @Override
    void handleTimerEvent(int i) {
        if (this.mState == i && i == 1) {
            int i2 = this.mPowerStatusCounter;
            this.mPowerStatusCounter = i2 + 1;
            if (i2 < 10) {
                queryDevicePowerStatus();
                addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
            } else {
                invokeCallback(1);
                finish();
            }
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
