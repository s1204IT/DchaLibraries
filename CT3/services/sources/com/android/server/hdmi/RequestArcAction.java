package com.android.server.hdmi;

import android.net.dhcp.DhcpPacket;

abstract class RequestArcAction extends HdmiCecFeatureAction {
    protected static final int STATE_WATING_FOR_REQUEST_ARC_REQUEST_RESPONSE = 1;
    private static final String TAG = "RequestArcAction";
    protected final int mAvrAddress;

    RequestArcAction(HdmiCecLocalDevice source, int avrAddress) {
        super(source);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
        HdmiUtils.verifyAddressType(avrAddress, 5);
        this.mAvrAddress = avrAddress;
    }

    @Override
    boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1 || !HdmiUtils.checkCommandSource(cmd, this.mAvrAddress, TAG)) {
            return false;
        }
        int opcode = cmd.getOpcode();
        switch (opcode) {
            case 0:
                int originalOpcode = cmd.getParams()[0] & DhcpPacket.MAX_OPTION_LEN;
                if (originalOpcode == 196) {
                    disableArcTransmission();
                    finish();
                } else if (originalOpcode == 195) {
                    tv().setArcStatus(false);
                    finish();
                }
                break;
        }
        return false;
    }

    protected final void disableArcTransmission() {
        SetArcTransmissionStateAction action = new SetArcTransmissionStateAction(localDevice(), this.mAvrAddress, false);
        addAndStartAction(action);
    }

    @Override
    final void handleTimerEvent(int state) {
        if (this.mState != state || state != 1) {
            return;
        }
        HdmiLogger.debug("[T] RequestArcAction.", new Object[0]);
        disableArcTransmission();
        finish();
    }
}
