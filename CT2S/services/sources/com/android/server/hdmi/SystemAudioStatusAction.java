package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;

final class SystemAudioStatusAction extends HdmiCecFeatureAction {
    private static final int STATE_WAIT_FOR_REPORT_AUDIO_STATUS = 1;
    private static final String TAG = "SystemAudioStatusAction";
    private final int mAvrAddress;
    private final IHdmiControlCallback mCallback;

    SystemAudioStatusAction(HdmiCecLocalDevice source, int avrAddress, IHdmiControlCallback callback) {
        super(source);
        this.mAvrAddress = avrAddress;
        this.mCallback = callback;
    }

    @Override
    boolean start() {
        this.mState = 1;
        addTimer(this.mState, 2000);
        sendGiveAudioStatus();
        return true;
    }

    private void sendGiveAudioStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveAudioStatus(getSourceAddress(), this.mAvrAddress), new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int error) {
                if (error != 0) {
                    SystemAudioStatusAction.this.handleSendGiveAudioStatusFailure();
                }
            }
        });
    }

    private void handleSendGiveAudioStatusFailure() {
        tv().setAudioStatus(false, -1);
        int uiCommand = tv().isSystemAudioActivated() ? HdmiCecKeycode.CEC_KEYCODE_RESTORE_VOLUME_FUNCTION : 101;
        sendUserControlPressedAndReleased(this.mAvrAddress, uiCommand);
        finishWithCallback(0);
    }

    @Override
    boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1 || this.mAvrAddress != cmd.getSource()) {
            return false;
        }
        switch (cmd.getOpcode()) {
            case 122:
                handleReportAudioStatus(cmd);
                break;
        }
        return false;
    }

    private void handleReportAudioStatus(HdmiCecMessage cmd) {
        byte[] params = cmd.getParams();
        boolean mute = (params[0] & 128) == 128;
        int volume = params[0] & 127;
        tv().setAudioStatus(mute, volume);
        if (!(tv().isSystemAudioActivated() ^ mute)) {
            sendUserControlPressedAndReleased(this.mAvrAddress, 67);
        }
        finishWithCallback(0);
    }

    private void finishWithCallback(int returnCode) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onComplete(returnCode);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to invoke callback.", e);
            }
        }
        finish();
    }

    @Override
    void handleTimerEvent(int state) {
        if (this.mState == state) {
            handleSendGiveAudioStatusFailure();
        }
    }
}
