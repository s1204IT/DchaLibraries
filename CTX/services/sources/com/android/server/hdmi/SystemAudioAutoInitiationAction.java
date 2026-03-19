package com.android.server.hdmi;

import com.android.server.hdmi.HdmiControlService;
import com.mediatek.server.powerhal.PowerHalManager;

final class SystemAudioAutoInitiationAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_FOR_SYSTEM_AUDIO_MODE_STATUS = 1;
    private final int mAvrAddress;

    SystemAudioAutoInitiationAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i) {
        super(hdmiCecLocalDevice);
        this.mAvrAddress = i;
    }

    @Override
    boolean start() {
        this.mState = 1;
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
        sendGiveSystemAudioModeStatus();
        return true;
    }

    private void sendGiveSystemAudioModeStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveSystemAudioModeStatus(getSourceAddress(), this.mAvrAddress), new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int i) {
                if (i != 0) {
                    SystemAudioAutoInitiationAction.this.tv().setSystemAudioMode(false);
                    SystemAudioAutoInitiationAction.this.finish();
                }
            }
        });
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (this.mState != 1 || this.mAvrAddress != hdmiCecMessage.getSource() || hdmiCecMessage.getOpcode() != 126) {
            return false;
        }
        handleSystemAudioModeStatusMessage(HdmiUtils.parseCommandParamSystemAudioStatus(hdmiCecMessage));
        return true;
    }

    private void handleSystemAudioModeStatusMessage(boolean z) {
        if (!canChangeSystemAudio()) {
            HdmiLogger.debug("Cannot change system audio mode in auto initiation action.", new Object[0]);
            finish();
            return;
        }
        boolean zIsSystemAudioControlFeatureEnabled = tv().isSystemAudioControlFeatureEnabled();
        if (z != zIsSystemAudioControlFeatureEnabled) {
            addAndStartAction(new SystemAudioActionFromTv(tv(), this.mAvrAddress, zIsSystemAudioControlFeatureEnabled, null));
        } else {
            tv().setSystemAudioMode(zIsSystemAudioControlFeatureEnabled);
        }
        finish();
    }

    @Override
    void handleTimerEvent(int i) {
        if (this.mState == i && this.mState == 1) {
            handleSystemAudioModeStatusTimeout();
        }
    }

    private void handleSystemAudioModeStatusTimeout() {
        if (!canChangeSystemAudio()) {
            HdmiLogger.debug("Cannot change system audio mode in auto initiation action.", new Object[0]);
            finish();
        } else {
            addAndStartAction(new SystemAudioActionFromTv(tv(), this.mAvrAddress, tv().isSystemAudioControlFeatureEnabled(), null));
            finish();
        }
    }

    private boolean canChangeSystemAudio() {
        return (tv().hasAction(SystemAudioActionFromTv.class) || tv().hasAction(SystemAudioActionFromAvr.class)) ? false : true;
    }
}
