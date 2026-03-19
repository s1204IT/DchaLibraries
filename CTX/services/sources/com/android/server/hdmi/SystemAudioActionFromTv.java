package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;

final class SystemAudioActionFromTv extends SystemAudioAction {
    SystemAudioActionFromTv(HdmiCecLocalDevice hdmiCecLocalDevice, int i, boolean z, IHdmiControlCallback iHdmiControlCallback) {
        super(hdmiCecLocalDevice, i, z, iHdmiControlCallback);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
    }

    @Override
    boolean start() {
        removeSystemAudioActionInProgress();
        sendSystemAudioModeRequest();
        return true;
    }
}
