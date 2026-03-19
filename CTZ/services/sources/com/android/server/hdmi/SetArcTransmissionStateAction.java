package com.android.server.hdmi;

import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
import com.mediatek.server.powerhal.PowerHalManager;

final class SetArcTransmissionStateAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_TIMEOUT = 1;
    private static final String TAG = "SetArcTransmissionStateAction";
    private final int mAvrAddress;
    private final boolean mEnabled;

    SetArcTransmissionStateAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, boolean z) {
        super(hdmiCecLocalDevice);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
        HdmiUtils.verifyAddressType(i, 5);
        this.mAvrAddress = i;
        this.mEnabled = z;
    }

    @Override
    boolean start() {
        if (this.mEnabled) {
            setArcStatus(true);
            this.mState = 1;
            addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
            sendReportArcInitiated();
        } else {
            setArcStatus(false);
            finish();
        }
        return true;
    }

    private void sendReportArcInitiated() {
        sendCommand(HdmiCecMessageBuilder.buildReportArcInitiated(getSourceAddress(), this.mAvrAddress), new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int i) {
                switch (i) {
                    case 1:
                        SetArcTransmissionStateAction.this.setArcStatus(false);
                        HdmiLogger.debug("Failed to send <Report Arc Initiated>.", new Object[0]);
                        SetArcTransmissionStateAction.this.finish();
                        break;
                }
            }
        });
    }

    private void setArcStatus(boolean z) {
        boolean arcStatus = tv().setArcStatus(z);
        Slog.i(TAG, "Change arc status [old:" + arcStatus + ", new:" + z + "]");
        if (!z && arcStatus) {
            sendCommand(HdmiCecMessageBuilder.buildReportArcTerminated(getSourceAddress(), this.mAvrAddress));
        }
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (this.mState != 1 || hdmiCecMessage.getOpcode() != 0 || (hdmiCecMessage.getParams()[0] & 255) != 193) {
            return false;
        }
        HdmiLogger.debug("Feature aborted for <Report Arc Initiated>", new Object[0]);
        setArcStatus(false);
        finish();
        return true;
    }

    @Override
    void handleTimerEvent(int i) {
        if (this.mState != i || this.mState != 1) {
            return;
        }
        finish();
    }
}
