package com.android.server.usb.descriptors;

import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class UsbHIDDescriptor extends UsbDescriptor {
    private static final String TAG = "UsbHIDDescriptor";
    private byte mCountryCode;
    private int mDescriptorLen;
    private byte mDescriptorType;
    private byte mNumDescriptors;
    private int mRelease;

    public UsbHIDDescriptor(int i, byte b) {
        super(i, b);
        this.mHierarchyLevel = 3;
    }

    public int getRelease() {
        return this.mRelease;
    }

    public byte getCountryCode() {
        return this.mCountryCode;
    }

    public byte getNumDescriptors() {
        return this.mNumDescriptors;
    }

    public byte getDescriptorType() {
        return this.mDescriptorType;
    }

    public int getDescriptorLen() {
        return this.mDescriptorLen;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mRelease = byteStream.unpackUsbShort();
        this.mCountryCode = byteStream.getByte();
        this.mNumDescriptors = byteStream.getByte();
        this.mDescriptorType = byteStream.getByte();
        this.mDescriptorLen = byteStream.unpackUsbShort();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Spec: " + ReportCanvas.getBCDString(getRelease()));
        reportCanvas.writeListItem("Type: " + ReportCanvas.getBCDString(getDescriptorType()));
        reportCanvas.writeListItem(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((int) getNumDescriptors()) + " Descriptors Len: " + getDescriptorLen());
        reportCanvas.closeList();
    }
}
