package com.android.server.hdmi;

import com.android.server.hdmi.HdmiControlService;

final class SystemAudioAutoInitiationAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_FOR_SYSTEM_AUDIO_MODE_STATUS = 1;
    private final int mAvrAddress;

    SystemAudioAutoInitiationAction(HdmiCecLocalDevice source, int avrAddress) {
        super(source);
        this.mAvrAddress = avrAddress;
    }

    @Override
    boolean start() {
        this.mState = 1;
        addTimer(this.mState, 2000);
        sendGiveSystemAudioModeStatus();
        return true;
    }

    private void sendGiveSystemAudioModeStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveSystemAudioModeStatus(getSourceAddress(), this.mAvrAddress), new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int error) {
                if (error != 0) {
                    SystemAudioAutoInitiationAction.this.tv().setSystemAudioMode(false, true);
                    SystemAudioAutoInitiationAction.this.finish();
                }
            }
        });
    }

    @Override
    boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1 || this.mAvrAddress != cmd.getSource()) {
            return false;
        }
        if (cmd.getOpcode() != 126) {
            return false;
        }
        handleSystemAudioModeStatusMessage();
        return true;
    }

    private void handleSystemAudioModeStatusMessage() {
        if (!canChangeSystemAudio()) {
            HdmiLogger.debug("Cannot change system audio mode in auto initiation action.", new Object[0]);
            finish();
        } else {
            boolean systemAudioModeSetting = tv().getSystemAudioModeSetting();
            addAndStartAction(new SystemAudioActionFromTv(tv(), this.mAvrAddress, systemAudioModeSetting, null));
            finish();
        }
    }

    @Override
    void handleTimerEvent(int state) {
        if (this.mState == state) {
            switch (this.mState) {
                case 1:
                    handleSystemAudioModeStatusTimeout();
                    break;
            }
        }
    }

    private void handleSystemAudioModeStatusTimeout() {
        if (tv().getSystemAudioModeSetting()) {
            if (canChangeSystemAudio()) {
                addAndStartAction(new SystemAudioActionFromTv(tv(), this.mAvrAddress, true, null));
            }
        } else {
            tv().setSystemAudioMode(false, true);
        }
        finish();
    }

    private boolean canChangeSystemAudio() {
        return (tv().hasAction(SystemAudioActionFromTv.class) || tv().hasAction(SystemAudioActionFromAvr.class)) ? false : true;
    }
}
