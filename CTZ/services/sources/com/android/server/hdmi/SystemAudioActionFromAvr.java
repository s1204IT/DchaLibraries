package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;

final class SystemAudioActionFromAvr extends SystemAudioAction {
    SystemAudioActionFromAvr(HdmiCecLocalDevice hdmiCecLocalDevice, int i, boolean z, IHdmiControlCallback iHdmiControlCallback) {
        super(hdmiCecLocalDevice, i, z, iHdmiControlCallback);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
    }

    @Override
    boolean start() {
        removeSystemAudioActionInProgress();
        handleSystemAudioActionFromAvr();
        return true;
    }

    private void handleSystemAudioActionFromAvr() {
        if (this.mTargetAudioStatus == tv().isSystemAudioActivated()) {
            finishWithCallback(0);
            return;
        }
        if (tv().isProhibitMode()) {
            sendCommand(HdmiCecMessageBuilder.buildFeatureAbortCommand(getSourceAddress(), this.mAvrLogicalAddress, 114, 4));
            this.mTargetAudioStatus = false;
            sendSystemAudioModeRequest();
            return;
        }
        removeAction(SystemAudioAutoInitiationAction.class);
        if (this.mTargetAudioStatus) {
            setSystemAudioMode(true);
            startAudioStatusAction();
        } else {
            setSystemAudioMode(false);
            finishWithCallback(0);
        }
    }
}
