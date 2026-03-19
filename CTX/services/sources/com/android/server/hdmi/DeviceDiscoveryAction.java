package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.net.util.NetworkConstants;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.hdmi.HdmiControlService;
import com.mediatek.server.powerhal.PowerHalManager;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class DeviceDiscoveryAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_FOR_DEVICE_POLLING = 1;
    private static final int STATE_WAITING_FOR_OSD_NAME = 3;
    private static final int STATE_WAITING_FOR_PHYSICAL_ADDRESS = 2;
    private static final int STATE_WAITING_FOR_VENDOR_ID = 4;
    private static final String TAG = "DeviceDiscoveryAction";
    private final DeviceDiscoveryCallback mCallback;
    private final ArrayList<DeviceInfo> mDevices;
    private int mProcessedDeviceCount;
    private int mTimeoutRetry;

    interface DeviceDiscoveryCallback {
        void onDeviceDiscoveryDone(List<HdmiDeviceInfo> list);
    }

    private static final class DeviceInfo {
        private int mDeviceType;
        private String mDisplayName;
        private final int mLogicalAddress;
        private int mPhysicalAddress;
        private int mPortId;
        private int mVendorId;

        private DeviceInfo(int i) {
            this.mPhysicalAddress = NetworkConstants.ARP_HWTYPE_RESERVED_HI;
            this.mPortId = -1;
            this.mVendorId = 16777215;
            this.mDisplayName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.mDeviceType = -1;
            this.mLogicalAddress = i;
        }

        private HdmiDeviceInfo toHdmiDeviceInfo() {
            return new HdmiDeviceInfo(this.mLogicalAddress, this.mPhysicalAddress, this.mPortId, this.mDeviceType, this.mVendorId, this.mDisplayName);
        }
    }

    DeviceDiscoveryAction(HdmiCecLocalDevice hdmiCecLocalDevice, DeviceDiscoveryCallback deviceDiscoveryCallback) {
        super(hdmiCecLocalDevice);
        this.mDevices = new ArrayList<>();
        this.mProcessedDeviceCount = 0;
        this.mTimeoutRetry = 0;
        this.mCallback = (DeviceDiscoveryCallback) Preconditions.checkNotNull(deviceDiscoveryCallback);
    }

    @Override
    boolean start() {
        this.mDevices.clear();
        this.mState = 1;
        pollDevices(new HdmiControlService.DevicePollingCallback() {
            @Override
            public void onPollingFinished(List<Integer> list) {
                if (list.isEmpty()) {
                    Slog.v(DeviceDiscoveryAction.TAG, "No device is detected.");
                    DeviceDiscoveryAction.this.wrapUpAndFinish();
                    return;
                }
                Slog.v(DeviceDiscoveryAction.TAG, "Device detected: " + list);
                DeviceDiscoveryAction.this.allocateDevices(list);
                DeviceDiscoveryAction.this.startPhysicalAddressStage();
            }
        }, 131073, 1);
        return true;
    }

    private void allocateDevices(List<Integer> list) {
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            this.mDevices.add(new DeviceInfo(it.next().intValue()));
        }
    }

    private void startPhysicalAddressStage() {
        Slog.v(TAG, "Start [Physical Address Stage]:" + this.mDevices.size());
        this.mProcessedDeviceCount = 0;
        this.mState = 2;
        checkAndProceedStage();
    }

    private boolean verifyValidLogicalAddress(int i) {
        return i >= 0 && i < 15;
    }

    private void queryPhysicalAddress(int i) {
        if (!verifyValidLogicalAddress(i)) {
            checkAndProceedStage();
            return;
        }
        this.mActionTimer.clearTimerMessage();
        if (mayProcessMessageIfCached(i, 132)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGivePhysicalAddress(getSourceAddress(), i));
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
    }

    private void startOsdNameStage() {
        Slog.v(TAG, "Start [Osd Name Stage]:" + this.mDevices.size());
        this.mProcessedDeviceCount = 0;
        this.mState = 3;
        checkAndProceedStage();
    }

    private void queryOsdName(int i) {
        if (!verifyValidLogicalAddress(i)) {
            checkAndProceedStage();
            return;
        }
        this.mActionTimer.clearTimerMessage();
        if (mayProcessMessageIfCached(i, 71)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGiveOsdNameCommand(getSourceAddress(), i));
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
    }

    private void startVendorIdStage() {
        Slog.v(TAG, "Start [Vendor Id Stage]:" + this.mDevices.size());
        this.mProcessedDeviceCount = 0;
        this.mState = 4;
        checkAndProceedStage();
    }

    private void queryVendorId(int i) {
        if (!verifyValidLogicalAddress(i)) {
            checkAndProceedStage();
            return;
        }
        this.mActionTimer.clearTimerMessage();
        if (mayProcessMessageIfCached(i, NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGiveDeviceVendorIdCommand(getSourceAddress(), i));
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
    }

    private boolean mayProcessMessageIfCached(int i, int i2) {
        HdmiCecMessage message = getCecMessageCache().getMessage(i, i2);
        if (message != null) {
            processCommand(message);
            return true;
        }
        return false;
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        switch (this.mState) {
            case 2:
                if (hdmiCecMessage.getOpcode() != 132) {
                    return false;
                }
                handleReportPhysicalAddress(hdmiCecMessage);
                return true;
            case 3:
                if (hdmiCecMessage.getOpcode() == 71) {
                    handleSetOsdName(hdmiCecMessage);
                    return true;
                }
                if (hdmiCecMessage.getOpcode() != 0 || (hdmiCecMessage.getParams()[0] & 255) != 70) {
                    return false;
                }
                handleSetOsdName(hdmiCecMessage);
                return true;
            case 4:
                if (hdmiCecMessage.getOpcode() == 135) {
                    handleVendorId(hdmiCecMessage);
                    return true;
                }
                if (hdmiCecMessage.getOpcode() != 0 || (hdmiCecMessage.getParams()[0] & 255) != 140) {
                    return false;
                }
                handleVendorId(hdmiCecMessage);
                return true;
            default:
                return false;
        }
    }

    private void handleReportPhysicalAddress(HdmiCecMessage hdmiCecMessage) {
        Preconditions.checkState(this.mProcessedDeviceCount < this.mDevices.size());
        DeviceInfo deviceInfo = this.mDevices.get(this.mProcessedDeviceCount);
        if (deviceInfo.mLogicalAddress != hdmiCecMessage.getSource()) {
            Slog.w(TAG, "Unmatched address[expected:" + deviceInfo.mLogicalAddress + ", actual:" + hdmiCecMessage.getSource());
            return;
        }
        byte[] params = hdmiCecMessage.getParams();
        deviceInfo.mPhysicalAddress = HdmiUtils.twoBytesToInt(params);
        deviceInfo.mPortId = getPortId(deviceInfo.mPhysicalAddress);
        deviceInfo.mDeviceType = params[2] & 255;
        tv().updateCecSwitchInfo(deviceInfo.mLogicalAddress, deviceInfo.mDeviceType, deviceInfo.mPhysicalAddress);
        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private int getPortId(int i) {
        return tv().getPortId(i);
    }

    private void handleSetOsdName(HdmiCecMessage hdmiCecMessage) {
        String defaultDeviceName;
        Preconditions.checkState(this.mProcessedDeviceCount < this.mDevices.size());
        DeviceInfo deviceInfo = this.mDevices.get(this.mProcessedDeviceCount);
        if (deviceInfo.mLogicalAddress != hdmiCecMessage.getSource()) {
            Slog.w(TAG, "Unmatched address[expected:" + deviceInfo.mLogicalAddress + ", actual:" + hdmiCecMessage.getSource());
            return;
        }
        try {
            if (hdmiCecMessage.getOpcode() == 0) {
                defaultDeviceName = HdmiUtils.getDefaultDeviceName(deviceInfo.mLogicalAddress);
            } else {
                defaultDeviceName = new String(hdmiCecMessage.getParams(), "US-ASCII");
            }
        } catch (UnsupportedEncodingException e) {
            Slog.w(TAG, "Failed to decode display name: " + hdmiCecMessage.toString());
            defaultDeviceName = HdmiUtils.getDefaultDeviceName(deviceInfo.mLogicalAddress);
        }
        deviceInfo.mDisplayName = defaultDeviceName;
        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private void handleVendorId(HdmiCecMessage hdmiCecMessage) {
        Preconditions.checkState(this.mProcessedDeviceCount < this.mDevices.size());
        DeviceInfo deviceInfo = this.mDevices.get(this.mProcessedDeviceCount);
        if (deviceInfo.mLogicalAddress != hdmiCecMessage.getSource()) {
            Slog.w(TAG, "Unmatched address[expected:" + deviceInfo.mLogicalAddress + ", actual:" + hdmiCecMessage.getSource());
            return;
        }
        if (hdmiCecMessage.getOpcode() != 0) {
            deviceInfo.mVendorId = HdmiUtils.threeBytesToInt(hdmiCecMessage.getParams());
        }
        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private void increaseProcessedDeviceCount() {
        this.mProcessedDeviceCount++;
        this.mTimeoutRetry = 0;
    }

    private void removeDevice(int i) {
        this.mDevices.remove(i);
    }

    private void wrapUpAndFinish() {
        Slog.v(TAG, "---------Wrap up Device Discovery:[" + this.mDevices.size() + "]---------");
        ArrayList arrayList = new ArrayList();
        Iterator<DeviceInfo> it = this.mDevices.iterator();
        while (it.hasNext()) {
            HdmiDeviceInfo hdmiDeviceInfo = it.next().toHdmiDeviceInfo();
            Slog.v(TAG, " DeviceInfo: " + hdmiDeviceInfo);
            arrayList.add(hdmiDeviceInfo);
        }
        Slog.v(TAG, "--------------------------------------------");
        this.mCallback.onDeviceDiscoveryDone(arrayList);
        finish();
        tv().processAllDelayedMessages();
    }

    private void checkAndProceedStage() {
        if (this.mDevices.isEmpty()) {
            wrapUpAndFinish();
            return;
        }
        if (this.mProcessedDeviceCount == this.mDevices.size()) {
            this.mProcessedDeviceCount = 0;
            switch (this.mState) {
                case 2:
                    startOsdNameStage();
                    break;
                case 3:
                    startVendorIdStage();
                    break;
                case 4:
                    wrapUpAndFinish();
                    break;
            }
            return;
        }
        sendQueryCommand();
    }

    private void sendQueryCommand() {
        int i = this.mDevices.get(this.mProcessedDeviceCount).mLogicalAddress;
        switch (this.mState) {
            case 2:
                queryPhysicalAddress(i);
                break;
            case 3:
                queryOsdName(i);
                break;
            case 4:
                queryVendorId(i);
                break;
        }
    }

    @Override
    void handleTimerEvent(int i) {
        if (this.mState == 0 || this.mState != i) {
            return;
        }
        int i2 = this.mTimeoutRetry + 1;
        this.mTimeoutRetry = i2;
        if (i2 < 5) {
            sendQueryCommand();
            return;
        }
        this.mTimeoutRetry = 0;
        Slog.v(TAG, "Timeout[State=" + this.mState + ", Processed=" + this.mProcessedDeviceCount);
        removeDevice(this.mProcessedDeviceCount);
        checkAndProceedStage();
    }
}
