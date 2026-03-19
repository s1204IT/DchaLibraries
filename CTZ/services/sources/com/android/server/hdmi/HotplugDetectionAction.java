package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

final class HotplugDetectionAction extends HdmiCecFeatureAction {
    private static final int AVR_COUNT_MAX = 3;
    private static final int NUM_OF_ADDRESS = 15;
    private static final int POLLING_INTERVAL_MS = 5000;
    private static final int STATE_WAIT_FOR_NEXT_POLLING = 1;
    private static final String TAG = "HotPlugDetectionAction";
    private static final int TIMEOUT_COUNT = 3;
    private int mAvrStatusCount;
    private int mTimeoutCount;

    HotplugDetectionAction(HdmiCecLocalDevice hdmiCecLocalDevice) {
        super(hdmiCecLocalDevice);
        this.mTimeoutCount = 0;
        this.mAvrStatusCount = 0;
    }

    @Override
    boolean start() {
        Slog.v(TAG, "Hot-plug dection started.");
        this.mState = 1;
        this.mTimeoutCount = 0;
        addTimer(this.mState, POLLING_INTERVAL_MS);
        return true;
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    @Override
    void handleTimerEvent(int i) {
        if (this.mState == i && this.mState == 1) {
            this.mTimeoutCount = (this.mTimeoutCount + 1) % 3;
            pollDevices();
        }
    }

    void pollAllDevicesNow() {
        this.mActionTimer.clearTimerMessage();
        this.mTimeoutCount = 0;
        this.mState = 1;
        pollAllDevices();
        addTimer(this.mState, POLLING_INTERVAL_MS);
    }

    private void pollDevices() {
        if (this.mTimeoutCount == 0) {
            pollAllDevices();
        } else if (tv().isSystemAudioActivated()) {
            pollAudioSystem();
        }
        addTimer(this.mState, POLLING_INTERVAL_MS);
    }

    private void pollAllDevices() {
        Slog.v(TAG, "Poll all devices.");
        pollDevices(new HdmiControlService.DevicePollingCallback() {
            @Override
            public void onPollingFinished(List<Integer> list) {
                HotplugDetectionAction.this.checkHotplug(list, false);
            }
        }, 65537, 1);
    }

    private void pollAudioSystem() {
        Slog.v(TAG, "Poll audio system.");
        pollDevices(new HdmiControlService.DevicePollingCallback() {
            @Override
            public void onPollingFinished(List<Integer> list) {
                HotplugDetectionAction.this.checkHotplug(list, true);
            }
        }, 65538, 1);
    }

    private void checkHotplug(List<Integer> list, boolean z) {
        HdmiDeviceInfo avrDeviceInfo;
        BitSet bitSetInfoListToBitSet = infoListToBitSet(tv().getDeviceInfoList(false), z);
        BitSet bitSetAddressListToBitSet = addressListToBitSet(list);
        BitSet bitSetComplement = complement(bitSetInfoListToBitSet, bitSetAddressListToBitSet);
        int iNextSetBit = -1;
        while (true) {
            iNextSetBit = bitSetComplement.nextSetBit(iNextSetBit + 1);
            if (iNextSetBit == -1) {
                break;
            }
            if (iNextSetBit == 5 && (avrDeviceInfo = tv().getAvrDeviceInfo()) != null && tv().isConnected(avrDeviceInfo.getPortId())) {
                this.mAvrStatusCount++;
                Slog.w(TAG, "Ack not returned from AVR. count: " + this.mAvrStatusCount);
                if (this.mAvrStatusCount < 3) {
                }
            }
            Slog.v(TAG, "Remove device by hot-plug detection:" + iNextSetBit);
            removeDevice(iNextSetBit);
        }
        if (!bitSetComplement.get(5)) {
            this.mAvrStatusCount = 0;
        }
        BitSet bitSetComplement2 = complement(bitSetAddressListToBitSet, bitSetInfoListToBitSet);
        int iNextSetBit2 = -1;
        while (true) {
            iNextSetBit2 = bitSetComplement2.nextSetBit(iNextSetBit2 + 1);
            if (iNextSetBit2 != -1) {
                Slog.v(TAG, "Add device by hot-plug detection:" + iNextSetBit2);
                addDevice(iNextSetBit2);
            } else {
                return;
            }
        }
    }

    private static BitSet infoListToBitSet(List<HdmiDeviceInfo> list, boolean z) {
        BitSet bitSet = new BitSet(15);
        for (HdmiDeviceInfo hdmiDeviceInfo : list) {
            if (z) {
                if (hdmiDeviceInfo.getDeviceType() == 5) {
                    bitSet.set(hdmiDeviceInfo.getLogicalAddress());
                }
            } else {
                bitSet.set(hdmiDeviceInfo.getLogicalAddress());
            }
        }
        return bitSet;
    }

    private static BitSet addressListToBitSet(List<Integer> list) {
        BitSet bitSet = new BitSet(15);
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            bitSet.set(it.next().intValue());
        }
        return bitSet;
    }

    private static BitSet complement(BitSet bitSet, BitSet bitSet2) {
        BitSet bitSet3 = (BitSet) bitSet.clone();
        bitSet3.andNot(bitSet2);
        return bitSet3;
    }

    private void addDevice(int i) {
        sendCommand(HdmiCecMessageBuilder.buildGivePhysicalAddress(getSourceAddress(), i));
    }

    private void removeDevice(int i) {
        mayChangeRoutingPath(i);
        mayCancelDeviceSelect(i);
        mayCancelOneTouchRecord(i);
        mayDisableSystemAudioAndARC(i);
        tv().removeCecDevice(i);
    }

    private void mayChangeRoutingPath(int i) {
        HdmiDeviceInfo cecDeviceInfo = tv().getCecDeviceInfo(i);
        if (cecDeviceInfo != null) {
            tv().handleRemoveActiveRoutingPath(cecDeviceInfo.getPhysicalAddress());
        }
    }

    private void mayCancelDeviceSelect(int i) {
        List actions = getActions(DeviceSelectAction.class);
        if (!actions.isEmpty() && ((DeviceSelectAction) actions.get(0)).getTargetAddress() == i) {
            removeAction(DeviceSelectAction.class);
        }
    }

    private void mayCancelOneTouchRecord(int i) {
        for (OneTouchRecordAction oneTouchRecordAction : getActions(OneTouchRecordAction.class)) {
            if (oneTouchRecordAction.getRecorderAddress() == i) {
                removeAction(oneTouchRecordAction);
            }
        }
    }

    private void mayDisableSystemAudioAndARC(int i) {
        if (HdmiUtils.getTypeFromAddress(i) != 5) {
            return;
        }
        tv().setSystemAudioMode(false);
        if (tv().isArcEstablished()) {
            tv().enableAudioReturnChannel(false);
            addAndStartAction(new RequestArcTerminationAction(localDevice(), i));
        }
    }
}
