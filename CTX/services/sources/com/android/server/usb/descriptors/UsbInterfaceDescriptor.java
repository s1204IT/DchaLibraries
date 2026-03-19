package com.android.server.usb.descriptors;

import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;
import java.util.ArrayList;

public class UsbInterfaceDescriptor extends UsbDescriptor {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbInterfaceDescriptor";
    protected byte mAlternateSetting;
    protected byte mDescrIndex;
    private ArrayList<UsbEndpointDescriptor> mEndpointDescriptors;
    protected int mInterfaceNumber;
    protected byte mNumEndpoints;
    protected int mProtocol;
    protected int mUsbClass;
    protected int mUsbSubclass;

    UsbInterfaceDescriptor(int i, byte b) {
        super(i, b);
        this.mEndpointDescriptors = new ArrayList<>();
        this.mHierarchyLevel = 3;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mInterfaceNumber = byteStream.getUnsignedByte();
        this.mAlternateSetting = byteStream.getByte();
        this.mNumEndpoints = byteStream.getByte();
        this.mUsbClass = byteStream.getUnsignedByte();
        this.mUsbSubclass = byteStream.getUnsignedByte();
        this.mProtocol = byteStream.getUnsignedByte();
        this.mDescrIndex = byteStream.getByte();
        return this.mLength;
    }

    public int getInterfaceNumber() {
        return this.mInterfaceNumber;
    }

    public byte getAlternateSetting() {
        return this.mAlternateSetting;
    }

    public byte getNumEndpoints() {
        return this.mNumEndpoints;
    }

    public int getUsbClass() {
        return this.mUsbClass;
    }

    public int getUsbSubclass() {
        return this.mUsbSubclass;
    }

    public int getProtocol() {
        return this.mProtocol;
    }

    public byte getDescrIndex() {
        return this.mDescrIndex;
    }

    void addEndpointDescriptor(UsbEndpointDescriptor usbEndpointDescriptor) {
        this.mEndpointDescriptors.add(usbEndpointDescriptor);
    }

    UsbInterface toAndroid(UsbDescriptorParser usbDescriptorParser) {
        UsbInterface usbInterface = new UsbInterface(this.mInterfaceNumber, this.mAlternateSetting, usbDescriptorParser.getDescriptorString(this.mDescrIndex), this.mUsbClass, this.mUsbSubclass, this.mProtocol);
        UsbEndpoint[] usbEndpointArr = new UsbEndpoint[this.mEndpointDescriptors.size()];
        for (int i = 0; i < this.mEndpointDescriptors.size(); i++) {
            usbEndpointArr[i] = this.mEndpointDescriptors.get(i).toAndroid(usbDescriptorParser);
        }
        usbInterface.setEndpoints(usbEndpointArr);
        return usbInterface;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        int usbClass = getUsbClass();
        int usbSubclass = getUsbSubclass();
        int protocol = getProtocol();
        String className = UsbStrings.getClassName(usbClass);
        String audioSubclassName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (usbClass == 1) {
            audioSubclassName = UsbStrings.getAudioSubclassName(usbSubclass);
        }
        reportCanvas.openList();
        reportCanvas.writeListItem("Interface #" + getInterfaceNumber());
        reportCanvas.writeListItem("Class: " + ReportCanvas.getHexString(usbClass) + ": " + className);
        reportCanvas.writeListItem("Subclass: " + ReportCanvas.getHexString(usbSubclass) + ": " + audioSubclassName);
        reportCanvas.writeListItem("Protocol: " + protocol + ": " + ReportCanvas.getHexString(protocol));
        StringBuilder sb = new StringBuilder();
        sb.append("Endpoints: ");
        sb.append((int) getNumEndpoints());
        reportCanvas.writeListItem(sb.toString());
        reportCanvas.closeList();
    }
}
