package com.android.server.usb.descriptors;

import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbInterface;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;

public final class UsbConfigDescriptor extends UsbDescriptor {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbConfigDescriptor";
    private int mAttribs;
    private byte mConfigIndex;
    private int mConfigValue;
    private ArrayList<UsbInterfaceDescriptor> mInterfaceDescriptors;
    private int mMaxPower;
    private byte mNumInterfaces;
    private int mTotalLength;

    UsbConfigDescriptor(int i, byte b) {
        super(i, b);
        this.mInterfaceDescriptors = new ArrayList<>();
        this.mHierarchyLevel = 2;
    }

    public int getTotalLength() {
        return this.mTotalLength;
    }

    public byte getNumInterfaces() {
        return this.mNumInterfaces;
    }

    public int getConfigValue() {
        return this.mConfigValue;
    }

    public byte getConfigIndex() {
        return this.mConfigIndex;
    }

    public int getAttribs() {
        return this.mAttribs;
    }

    public int getMaxPower() {
        return this.mMaxPower;
    }

    void addInterfaceDescriptor(UsbInterfaceDescriptor usbInterfaceDescriptor) {
        this.mInterfaceDescriptors.add(usbInterfaceDescriptor);
    }

    UsbConfiguration toAndroid(UsbDescriptorParser usbDescriptorParser) {
        UsbConfiguration usbConfiguration = new UsbConfiguration(this.mConfigValue, usbDescriptorParser.getDescriptorString(this.mConfigIndex), this.mAttribs, this.mMaxPower);
        UsbInterface[] usbInterfaceArr = new UsbInterface[this.mInterfaceDescriptors.size()];
        for (int i = 0; i < this.mInterfaceDescriptors.size(); i++) {
            usbInterfaceArr[i] = this.mInterfaceDescriptors.get(i).toAndroid(usbDescriptorParser);
        }
        usbConfiguration.setInterfaces(usbInterfaceArr);
        return usbConfiguration;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mTotalLength = byteStream.unpackUsbShort();
        this.mNumInterfaces = byteStream.getByte();
        this.mConfigValue = byteStream.getUnsignedByte();
        this.mConfigIndex = byteStream.getByte();
        this.mAttribs = byteStream.getUnsignedByte();
        this.mMaxPower = byteStream.getUnsignedByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Config # " + getConfigValue());
        reportCanvas.writeListItem(((int) getNumInterfaces()) + " Interfaces.");
        reportCanvas.writeListItem("Attributes: " + ReportCanvas.getHexString(getAttribs()));
        reportCanvas.closeList();
    }
}
