package com.android.server.hdmi;

import com.android.server.hdmi.HdmiControlService;

final class RequestArcTerminationAction extends RequestArcAction {
    private static final String TAG = "RequestArcTerminationAction";

    RequestArcTerminationAction(HdmiCecLocalDevice source, int avrAddress) {
        super(source, avrAddress);
    }

    @Override
    boolean start() {
        this.mState = 1;
        addTimer(this.mState, 2000);
        HdmiCecMessage command = HdmiCecMessageBuilder.buildRequestArcTermination(getSourceAddress(), this.mAvrAddress);
        sendCommand(command, new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int error) {
                if (error == 0) {
                    return;
                }
                RequestArcTerminationAction.this.disableArcTransmission();
                RequestArcTerminationAction.this.finish();
            }
        });
        return true;
    }
}
