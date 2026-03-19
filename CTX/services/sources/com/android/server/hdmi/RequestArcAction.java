package com.android.server.hdmi;

abstract class RequestArcAction extends HdmiCecFeatureAction {
    protected static final int STATE_WATING_FOR_REQUEST_ARC_REQUEST_RESPONSE = 1;
    private static final String TAG = "RequestArcAction";
    protected final int mAvrAddress;

    RequestArcAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i) {
        super(hdmiCecLocalDevice);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
        HdmiUtils.verifyAddressType(i, 5);
        this.mAvrAddress = i;
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (this.mState != 1 || !HdmiUtils.checkCommandSource(hdmiCecMessage, this.mAvrAddress, TAG) || hdmiCecMessage.getOpcode() != 0) {
            return false;
        }
        int i = hdmiCecMessage.getParams()[0] & 255;
        if (i == 196) {
            disableArcTransmission();
            finish();
            return true;
        }
        if (i != 195) {
            return false;
        }
        tv().setArcStatus(false);
        finish();
        return true;
    }

    protected final void disableArcTransmission() {
        addAndStartAction(new SetArcTransmissionStateAction(localDevice(), this.mAvrAddress, false));
    }

    @Override
    final void handleTimerEvent(int i) {
        if (this.mState != i || i != 1) {
            return;
        }
        HdmiLogger.debug("[T] RequestArcAction.", new Object[0]);
        disableArcTransmission();
        finish();
    }
}
