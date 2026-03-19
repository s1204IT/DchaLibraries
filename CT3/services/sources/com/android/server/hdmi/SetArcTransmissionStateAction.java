package com.android.server.hdmi;

import android.net.dhcp.DhcpPacket;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;

final class SetArcTransmissionStateAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_TIMEOUT = 1;
    private static final String TAG = "SetArcTransmissionStateAction";
    private final int mAvrAddress;
    private final boolean mEnabled;

    SetArcTransmissionStateAction(HdmiCecLocalDevice source, int avrAddress, boolean enabled) {
        super(source);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
        HdmiUtils.verifyAddressType(avrAddress, 5);
        this.mAvrAddress = avrAddress;
        this.mEnabled = enabled;
    }

    @Override
    boolean start() {
        if (this.mEnabled) {
            setArcStatus(true);
            this.mState = 1;
            addTimer(this.mState, 2000);
            sendReportArcInitiated();
        } else {
            setArcStatus(false);
            finish();
        }
        return true;
    }

    private void sendReportArcInitiated() {
        HdmiCecMessage command = HdmiCecMessageBuilder.buildReportArcInitiated(getSourceAddress(), this.mAvrAddress);
        sendCommand(command, new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int error) {
                switch (error) {
                    case 1:
                        SetArcTransmissionStateAction.this.setArcStatus(false);
                        HdmiLogger.debug("Failed to send <Report Arc Initiated>.", new Object[0]);
                        SetArcTransmissionStateAction.this.finish();
                        break;
                }
            }
        });
    }

    private void setArcStatus(boolean enabled) {
        boolean wasEnabled = tv().setArcStatus(enabled);
        Slog.i(TAG, "Change arc status [old:" + wasEnabled + ", new:" + enabled + "]");
        if (enabled || !wasEnabled) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildReportArcTerminated(getSourceAddress(), this.mAvrAddress));
    }

    @Override
    boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1) {
            return false;
        }
        int opcode = cmd.getOpcode();
        if (opcode == 0) {
            int originalOpcode = cmd.getParams()[0] & DhcpPacket.MAX_OPTION_LEN;
            if (originalOpcode == 193) {
                HdmiLogger.debug("Feature aborted for <Report Arc Initiated>", new Object[0]);
                setArcStatus(false);
                finish();
                return true;
            }
        }
        return false;
    }

    @Override
    void handleTimerEvent(int state) {
        if (this.mState != state || this.mState != 1) {
            return;
        }
        finish();
    }
}
