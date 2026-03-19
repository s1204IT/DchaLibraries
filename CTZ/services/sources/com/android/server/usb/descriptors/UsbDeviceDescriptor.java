package com.android.server.usb.descriptors;

import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;
import java.util.ArrayList;

public final class UsbDeviceDescriptor extends UsbDescriptor {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbDeviceDescriptor";
    public static final int USBSPEC_1_0 = 256;
    public static final int USBSPEC_1_1 = 272;
    public static final int USBSPEC_2_0 = 512;
    private ArrayList<UsbConfigDescriptor> mConfigDescriptors;
    private int mDevClass;
    private int mDevSubClass;
    private int mDeviceRelease;
    private byte mMfgIndex;
    private byte mNumConfigs;
    private byte mPacketSize;
    private int mProductID;
    private byte mProductIndex;
    private int mProtocol;
    private byte mSerialIndex;
    private int mSpec;
    private int mVendorID;

    UsbDeviceDescriptor(int i, byte b) {
        super(i, b);
        this.mConfigDescriptors = new ArrayList<>();
        this.mHierarchyLevel = 1;
    }

    public int getSpec() {
        return this.mSpec;
    }

    public int getDevClass() {
        return this.mDevClass;
    }

    public int getDevSubClass() {
        return this.mDevSubClass;
    }

    public int getProtocol() {
        return this.mProtocol;
    }

    public byte getPacketSize() {
        return this.mPacketSize;
    }

    public int getVendorID() {
        return this.mVendorID;
    }

    public int getProductID() {
        return this.mProductID;
    }

    public int getDeviceRelease() {
        return this.mDeviceRelease;
    }

    public String getDeviceReleaseString() {
        int i = this.mDeviceRelease & 15;
        return String.format("%d.%d%d", Integer.valueOf((((this.mDeviceRelease & 61440) >> 12) * 10) + ((this.mDeviceRelease & 3840) >> 8)), Integer.valueOf((this.mDeviceRelease & 240) >> 4), Integer.valueOf(i));
    }

    public byte getMfgIndex() {
        return this.mMfgIndex;
    }

    public String getMfgString(UsbDescriptorParser usbDescriptorParser) {
        return usbDescriptorParser.getDescriptorString(this.mMfgIndex);
    }

    public byte getProductIndex() {
        return this.mProductIndex;
    }

    public String getProductString(UsbDescriptorParser usbDescriptorParser) {
        return usbDescriptorParser.getDescriptorString(this.mProductIndex);
    }

    public byte getSerialIndex() {
        return this.mSerialIndex;
    }

    public String getSerialString(UsbDescriptorParser usbDescriptorParser) {
        return usbDescriptorParser.getDescriptorString(this.mSerialIndex);
    }

    public byte getNumConfigs() {
        return this.mNumConfigs;
    }

    void addConfigDescriptor(UsbConfigDescriptor usbConfigDescriptor) {
        this.mConfigDescriptors.add(usbConfigDescriptor);
    }

    public UsbDevice toAndroid(UsbDescriptorParser usbDescriptorParser) {
        UsbDevice usbDevice = new UsbDevice(usbDescriptorParser.getDeviceAddr(), this.mVendorID, this.mProductID, this.mDevClass, this.mDevSubClass, this.mProtocol, getMfgString(usbDescriptorParser), getProductString(usbDescriptorParser), getDeviceReleaseString(), getSerialString(usbDescriptorParser));
        UsbConfiguration[] usbConfigurationArr = new UsbConfiguration[this.mConfigDescriptors.size()];
        Log.d(TAG, "  " + usbConfigurationArr.length + " configs");
        for (int i = 0; i < this.mConfigDescriptors.size(); i++) {
            usbConfigurationArr[i] = this.mConfigDescriptors.get(i).toAndroid(usbDescriptorParser);
        }
        usbDevice.setConfigurations(usbConfigurationArr);
        return usbDevice;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mSpec = byteStream.unpackUsbShort();
        this.mDevClass = byteStream.getUnsignedByte();
        this.mDevSubClass = byteStream.getUnsignedByte();
        this.mProtocol = byteStream.getUnsignedByte();
        this.mPacketSize = byteStream.getByte();
        this.mVendorID = byteStream.unpackUsbShort();
        this.mProductID = byteStream.unpackUsbShort();
        this.mDeviceRelease = byteStream.unpackUsbShort();
        this.mMfgIndex = byteStream.getByte();
        this.mProductIndex = byteStream.getByte();
        this.mSerialIndex = byteStream.getByte();
        this.mNumConfigs = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Spec: " + ReportCanvas.getBCDString(getSpec()));
        int devClass = getDevClass();
        String className = UsbStrings.getClassName(devClass);
        int devSubClass = getDevSubClass();
        reportCanvas.writeListItem("Class " + devClass + ": " + className + " Subclass" + devSubClass + ": " + UsbStrings.getClassName(devSubClass));
        StringBuilder sb = new StringBuilder();
        sb.append("Vendor ID: ");
        sb.append(ReportCanvas.getHexString(getVendorID()));
        sb.append(" Product ID: ");
        sb.append(ReportCanvas.getHexString(getProductID()));
        sb.append(" Product Release: ");
        sb.append(ReportCanvas.getBCDString(getDeviceRelease()));
        reportCanvas.writeListItem(sb.toString());
        UsbDescriptorParser parser = reportCanvas.getParser();
        byte mfgIndex = getMfgIndex();
        String descriptorString = parser.getDescriptorString(mfgIndex);
        byte productIndex = getProductIndex();
        reportCanvas.writeListItem("Manufacturer " + ((int) mfgIndex) + ": " + descriptorString + " Product " + ((int) productIndex) + ": " + parser.getDescriptorString(productIndex));
        reportCanvas.closeList();
    }
}
