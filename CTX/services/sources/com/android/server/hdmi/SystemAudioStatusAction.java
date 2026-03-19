package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
import com.mediatek.server.powerhal.PowerHalManager;

final class SystemAudioStatusAction extends HdmiCecFeatureAction {
    private static final int STATE_WAIT_FOR_REPORT_AUDIO_STATUS = 1;
    private static final String TAG = "SystemAudioStatusAction";
    private final int mAvrAddress;
    private final IHdmiControlCallback mCallback;

    SystemAudioStatusAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, IHdmiControlCallback iHdmiControlCallback) {
        super(hdmiCecLocalDevice);
        this.mAvrAddress = i;
        this.mCallback = iHdmiControlCallback;
    }

    @Override
    boolean start() {
        this.mState = 1;
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
        sendGiveAudioStatus();
        return true;
    }

    private void sendGiveAudioStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveAudioStatus(getSourceAddress(), this.mAvrAddress), new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int i) {
                if (i != 0) {
                    SystemAudioStatusAction.this.handleSendGiveAudioStatusFailure();
                }
            }
        });
    }

    private void handleSendGiveAudioStatusFailure() {
        tv().setAudioStatus(false, -1);
        sendUserControlPressedAndReleased(this.mAvrAddress, HdmiCecKeycode.getMuteKey(!tv().isSystemAudioActivated()));
        finishWithCallback(0);
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (this.mState != 1 || this.mAvrAddress != hdmiCecMessage.getSource() || hdmiCecMessage.getOpcode() != 122) {
            return false;
        }
        handleReportAudioStatus(hdmiCecMessage);
        return true;
    }

    private void handleReportAudioStatus(HdmiCecMessage hdmiCecMessage) {
        hdmiCecMessage.getParams();
        boolean zIsAudioStatusMute = HdmiUtils.isAudioStatusMute(hdmiCecMessage);
        tv().setAudioStatus(zIsAudioStatusMute, HdmiUtils.getAudioStatusVolume(hdmiCecMessage));
        if (!(tv().isSystemAudioActivated() ^ zIsAudioStatusMute)) {
            sendUserControlPressedAndReleased(this.mAvrAddress, 67);
        }
        finishWithCallback(0);
    }

    private void finishWithCallback(int i) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onComplete(i);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to invoke callback.", e);
            }
        }
        finish();
    }

    @Override
    void handleTimerEvent(int i) {
        if (this.mState != i) {
            return;
        }
        handleSendGiveAudioStatusFailure();
    }
}
