package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.util.SparseIntArray;
import com.android.server.hdmi.HdmiControlService;
import java.util.Iterator;
import java.util.List;

public class PowerStatusMonitorAction extends HdmiCecFeatureAction {
    private static final int INVALID_POWER_STATUS = -2;
    private static final int MONITIROING_INTERNAL_MS = 60000;
    private static final int REPORT_POWER_STATUS_TIMEOUT_MS = 5000;
    private static final int STATE_WAIT_FOR_NEXT_MONITORING = 2;
    private static final int STATE_WAIT_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "PowerStatusMonitorAction";
    private final SparseIntArray mPowerStatus;

    PowerStatusMonitorAction(HdmiCecLocalDevice hdmiCecLocalDevice) {
        super(hdmiCecLocalDevice);
        this.mPowerStatus = new SparseIntArray();
    }

    @Override
    boolean start() {
        queryPowerStatus();
        return true;
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (this.mState == 1 && hdmiCecMessage.getOpcode() == 144) {
            return handleReportPowerStatus(hdmiCecMessage);
        }
        return false;
    }

    private boolean handleReportPowerStatus(HdmiCecMessage hdmiCecMessage) {
        int source = hdmiCecMessage.getSource();
        if (this.mPowerStatus.get(source, -2) == -2) {
            return false;
        }
        updatePowerStatus(source, hdmiCecMessage.getParams()[0] & 255, true);
        return true;
    }

    @Override
    void handleTimerEvent(int i) {
        switch (this.mState) {
            case 1:
                handleTimeout();
                break;
            case 2:
                queryPowerStatus();
                break;
        }
    }

    private void handleTimeout() {
        for (int i = 0; i < this.mPowerStatus.size(); i++) {
            updatePowerStatus(this.mPowerStatus.keyAt(i), -1, false);
        }
        this.mPowerStatus.clear();
        this.mState = 2;
    }

    private void resetPowerStatus(List<HdmiDeviceInfo> list) {
        this.mPowerStatus.clear();
        for (HdmiDeviceInfo hdmiDeviceInfo : list) {
            this.mPowerStatus.append(hdmiDeviceInfo.getLogicalAddress(), hdmiDeviceInfo.getDevicePowerStatus());
        }
    }

    private void queryPowerStatus() {
        List<HdmiDeviceInfo> deviceInfoList = tv().getDeviceInfoList(false);
        resetPowerStatus(deviceInfoList);
        Iterator<HdmiDeviceInfo> it = deviceInfoList.iterator();
        while (it.hasNext()) {
            final int logicalAddress = it.next().getLogicalAddress();
            sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), logicalAddress), new HdmiControlService.SendMessageCallback() {
                @Override
                public void onSendCompleted(int i) {
                    if (i != 0) {
                        PowerStatusMonitorAction.this.updatePowerStatus(logicalAddress, -1, true);
                    }
                }
            });
        }
        this.mState = 1;
        addTimer(2, MONITIROING_INTERNAL_MS);
        addTimer(1, REPORT_POWER_STATUS_TIMEOUT_MS);
    }

    private void updatePowerStatus(int i, int i2, boolean z) {
        tv().updateDevicePowerStatus(i, i2);
        if (z) {
            this.mPowerStatus.delete(i);
        }
    }
}
