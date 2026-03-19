package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import java.util.ArrayList;
import java.util.Iterator;

final class DelayedMessageBuffer {
    private final ArrayList<HdmiCecMessage> mBuffer = new ArrayList<>();
    private final HdmiCecLocalDevice mDevice;

    DelayedMessageBuffer(HdmiCecLocalDevice hdmiCecLocalDevice) {
        this.mDevice = hdmiCecLocalDevice;
    }

    void add(HdmiCecMessage hdmiCecMessage) {
        boolean z;
        int opcode = hdmiCecMessage.getOpcode();
        if (opcode == 114) {
            this.mBuffer.add(hdmiCecMessage);
        } else if (opcode == 130) {
            removeActiveSource();
            this.mBuffer.add(hdmiCecMessage);
        } else {
            z = opcode == 192;
            this.mBuffer.add(hdmiCecMessage);
        }
        if (z) {
            HdmiLogger.debug("Buffering message:" + hdmiCecMessage, new Object[0]);
        }
    }

    private void removeActiveSource() {
        Iterator<HdmiCecMessage> it = this.mBuffer.iterator();
        while (it.hasNext()) {
            if (it.next().getOpcode() == 130) {
                it.remove();
            }
        }
    }

    boolean isBuffered(int i) {
        Iterator<HdmiCecMessage> it = this.mBuffer.iterator();
        while (it.hasNext()) {
            if (it.next().getOpcode() == i) {
                return true;
            }
        }
        return false;
    }

    void processAllMessages() {
        ArrayList<HdmiCecMessage> arrayList = new ArrayList(this.mBuffer);
        this.mBuffer.clear();
        for (HdmiCecMessage hdmiCecMessage : arrayList) {
            this.mDevice.onMessage(hdmiCecMessage);
            HdmiLogger.debug("Processing message:" + hdmiCecMessage, new Object[0]);
        }
    }

    void processMessagesForDevice(int i) {
        ArrayList<HdmiCecMessage> arrayList = new ArrayList(this.mBuffer);
        this.mBuffer.clear();
        HdmiLogger.debug("Checking message for address:" + i, new Object[0]);
        for (HdmiCecMessage hdmiCecMessage : arrayList) {
            if (hdmiCecMessage.getSource() != i) {
                this.mBuffer.add(hdmiCecMessage);
            } else if (hdmiCecMessage.getOpcode() == 130 && !this.mDevice.isInputReady(HdmiDeviceInfo.idForCecDevice(i))) {
                this.mBuffer.add(hdmiCecMessage);
            } else {
                this.mDevice.onMessage(hdmiCecMessage);
                HdmiLogger.debug("Processing message:" + hdmiCecMessage, new Object[0]);
            }
        }
    }

    void processActiveSource(int i) {
        ArrayList<HdmiCecMessage> arrayList = new ArrayList(this.mBuffer);
        this.mBuffer.clear();
        for (HdmiCecMessage hdmiCecMessage : arrayList) {
            if (hdmiCecMessage.getOpcode() == 130 && hdmiCecMessage.getSource() == i) {
                this.mDevice.onMessage(hdmiCecMessage);
                HdmiLogger.debug("Processing message:" + hdmiCecMessage, new Object[0]);
            } else {
                this.mBuffer.add(hdmiCecMessage);
            }
        }
    }
}
