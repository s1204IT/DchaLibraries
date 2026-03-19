package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.net.util.NetworkConstants;
import android.util.Slog;
import com.android.server.hdmi.HdmiCecLocalDevice;
import com.mediatek.server.powerhal.PowerHalManager;
import java.io.UnsupportedEncodingException;

final class NewDeviceAction extends HdmiCecFeatureAction {
    static final int STATE_WAITING_FOR_DEVICE_VENDOR_ID = 2;
    static final int STATE_WAITING_FOR_SET_OSD_NAME = 1;
    private static final String TAG = "NewDeviceAction";
    private final int mDeviceLogicalAddress;
    private final int mDevicePhysicalAddress;
    private final int mDeviceType;
    private String mDisplayName;
    private int mTimeoutRetry;
    private int mVendorId;

    NewDeviceAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, int i2, int i3) {
        super(hdmiCecLocalDevice);
        this.mDeviceLogicalAddress = i;
        this.mDevicePhysicalAddress = i2;
        this.mDeviceType = i3;
        this.mVendorId = 16777215;
    }

    @Override
    public boolean start() {
        requestOsdName(true);
        return true;
    }

    private void requestOsdName(boolean z) {
        if (z) {
            this.mTimeoutRetry = 0;
        }
        this.mState = 1;
        if (mayProcessCommandIfCached(this.mDeviceLogicalAddress, 71)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGiveOsdNameCommand(getSourceAddress(), this.mDeviceLogicalAddress));
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
    }

    @Override
    public boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        int opcode = hdmiCecMessage.getOpcode();
        int source = hdmiCecMessage.getSource();
        byte[] params = hdmiCecMessage.getParams();
        if (this.mDeviceLogicalAddress != source) {
            return false;
        }
        if (this.mState == 1) {
            if (opcode == 71) {
                try {
                    this.mDisplayName = new String(params, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    Slog.e(TAG, "Failed to get OSD name: " + e.getMessage());
                }
                requestVendorId(true);
                return true;
            }
            if (opcode == 0 && (params[0] & 255) == 70) {
                requestVendorId(true);
                return true;
            }
        } else if (this.mState == 2) {
            if (opcode == 135) {
                this.mVendorId = HdmiUtils.threeBytesToInt(params);
                addDeviceInfo();
                finish();
                return true;
            }
            if (opcode == 0 && (params[0] & 255) == 140) {
                addDeviceInfo();
                finish();
                return true;
            }
        }
        return false;
    }

    private boolean mayProcessCommandIfCached(int i, int i2) {
        HdmiCecMessage message = getCecMessageCache().getMessage(i, i2);
        if (message != null) {
            return processCommand(message);
        }
        return false;
    }

    private void requestVendorId(boolean z) {
        if (z) {
            this.mTimeoutRetry = 0;
        }
        this.mState = 2;
        if (mayProcessCommandIfCached(this.mDeviceLogicalAddress, NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGiveDeviceVendorIdCommand(getSourceAddress(), this.mDeviceLogicalAddress));
        addTimer(this.mState, PowerHalManager.ROTATE_BOOST_TIME);
    }

    private void addDeviceInfo() {
        if (!tv().isInDeviceList(this.mDeviceLogicalAddress, this.mDevicePhysicalAddress)) {
            Slog.w(TAG, String.format("Device not found (%02x, %04x)", Integer.valueOf(this.mDeviceLogicalAddress), Integer.valueOf(this.mDevicePhysicalAddress)));
            return;
        }
        if (this.mDisplayName == null) {
            this.mDisplayName = HdmiUtils.getDefaultDeviceName(this.mDeviceLogicalAddress);
        }
        HdmiDeviceInfo hdmiDeviceInfo = new HdmiDeviceInfo(this.mDeviceLogicalAddress, this.mDevicePhysicalAddress, tv().getPortId(this.mDevicePhysicalAddress), this.mDeviceType, this.mVendorId, this.mDisplayName);
        tv().addCecDevice(hdmiDeviceInfo);
        tv().processDelayedMessages(this.mDeviceLogicalAddress);
        if (HdmiUtils.getTypeFromAddress(this.mDeviceLogicalAddress) == 5) {
            tv().onNewAvrAdded(hdmiDeviceInfo);
        }
    }

    @Override
    public void handleTimerEvent(int i) {
        if (this.mState == 0 || this.mState != i) {
            return;
        }
        if (i == 1) {
            int i2 = this.mTimeoutRetry + 1;
            this.mTimeoutRetry = i2;
            if (i2 < 5) {
                requestOsdName(false);
                return;
            } else {
                requestVendorId(true);
                return;
            }
        }
        if (i == 2) {
            int i3 = this.mTimeoutRetry + 1;
            this.mTimeoutRetry = i3;
            if (i3 < 5) {
                requestVendorId(false);
            } else {
                addDeviceInfo();
                finish();
            }
        }
    }

    boolean isActionOf(HdmiCecLocalDevice.ActiveSource activeSource) {
        return this.mDeviceLogicalAddress == activeSource.logicalAddress && this.mDevicePhysicalAddress == activeSource.physicalAddress;
    }
}
