package com.android.server.hdmi;

import com.android.server.hdmi.HdmiControlService;

final class RequestArcInitiationAction extends RequestArcAction {
    private static final String TAG = "RequestArcInitiationAction";

    RequestArcInitiationAction(HdmiCecLocalDevice source, int avrAddress) {
        super(source, avrAddress);
    }

    @Override
    boolean start() {
        this.mState = 1;
        addTimer(this.mState, 2000);
        HdmiCecMessage command = HdmiCecMessageBuilder.buildRequestArcInitiation(getSourceAddress(), this.mAvrAddress);
        sendCommand(command, new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int error) {
                if (error != 0) {
                    RequestArcInitiationAction.this.disableArcTransmission();
                    RequestArcInitiationAction.this.finish();
                }
            }
        });
        return true;
    }
}
