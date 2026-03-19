package com.android.server.hdmi;

import com.android.server.hdmi.HdmiControlService;
import com.mediatek.server.powerhal.PowerHalManager;

final class RequestArcInitiationAction extends RequestArcAction {
    private static final String TAG = "RequestArcInitiationAction";

    RequestArcInitiationAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i) {
        super(hdmiCecLocalDevice, i);
    }

    @Override
    boolean start() {
        this.mState = 1;
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
        sendCommand(HdmiCecMessageBuilder.buildRequestArcInitiation(getSourceAddress(), this.mAvrAddress), new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int i) {
                if (i != 0) {
                    RequestArcInitiationAction.this.tv().setArcStatus(false);
                    RequestArcInitiationAction.this.finish();
                }
            }
        });
        return true;
    }
}
