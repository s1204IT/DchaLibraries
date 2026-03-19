package com.android.server.usb.descriptors;

import android.hardware.usb.UsbEndpoint;
import com.android.server.usb.descriptors.report.ReportCanvas;

public class UsbEndpointDescriptor extends UsbDescriptor {
    private static final boolean DEBUG = false;
    public static final int DIRECTION_INPUT = -128;
    public static final int DIRECTION_OUTPUT = 0;
    public static final byte MASK_ATTRIBS_SYNCTYPE = 12;
    public static final int MASK_ATTRIBS_TRANSTYPE = 3;
    public static final int MASK_ATTRIBS_USEAGE = 48;
    public static final int MASK_ENDPOINT_ADDRESS = 15;
    public static final int MASK_ENDPOINT_DIRECTION = -128;
    public static final byte SYNCTYPE_ADAPTSYNC = 8;
    public static final byte SYNCTYPE_ASYNC = 4;
    public static final byte SYNCTYPE_NONE = 0;
    public static final byte SYNCTYPE_RESERVED = 12;
    private static final String TAG = "UsbEndpointDescriptor";
    public static final int TRANSTYPE_BULK = 2;
    public static final int TRANSTYPE_CONTROL = 0;
    public static final int TRANSTYPE_INTERRUPT = 3;
    public static final int TRANSTYPE_ISO = 1;
    public static final int USEAGE_DATA = 0;
    public static final int USEAGE_EXPLICIT = 32;
    public static final int USEAGE_FEEDBACK = 16;
    public static final int USEAGE_RESERVED = 48;
    private int mAttributes;
    private int mEndpointAddress;
    private int mInterval;
    private int mPacketSize;
    private byte mRefresh;
    private byte mSyncAddress;

    public UsbEndpointDescriptor(int i, byte b) {
        super(i, b);
        this.mHierarchyLevel = 4;
    }

    public int getEndpointAddress() {
        return this.mEndpointAddress;
    }

    public int getAttributes() {
        return this.mAttributes;
    }

    public int getPacketSize() {
        return this.mPacketSize;
    }

    public int getInterval() {
        return this.mInterval;
    }

    public byte getRefresh() {
        return this.mRefresh;
    }

    public byte getSyncAddress() {
        return this.mSyncAddress;
    }

    UsbEndpoint toAndroid(UsbDescriptorParser usbDescriptorParser) {
        return new UsbEndpoint(this.mEndpointAddress, this.mAttributes, this.mPacketSize, this.mInterval);
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mEndpointAddress = byteStream.getUnsignedByte();
        this.mAttributes = byteStream.getUnsignedByte();
        this.mPacketSize = byteStream.unpackUsbShort();
        this.mInterval = byteStream.getUnsignedByte();
        if (this.mLength == 9) {
            this.mRefresh = byteStream.getByte();
            this.mSyncAddress = byteStream.getByte();
        }
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        int endpointAddress = getEndpointAddress();
        StringBuilder sb = new StringBuilder();
        sb.append("Address: ");
        sb.append(ReportCanvas.getHexString(endpointAddress & 15));
        sb.append((endpointAddress & (-128)) == 0 ? " [out]" : " [in]");
        reportCanvas.writeListItem(sb.toString());
        int attributes = getAttributes();
        reportCanvas.openListItem();
        reportCanvas.write("Attributes: " + ReportCanvas.getHexString(attributes) + " ");
        int i = attributes & 3;
        switch (i) {
            case 0:
                reportCanvas.write("Control");
                break;
            case 1:
                reportCanvas.write("Iso");
                break;
            case 2:
                reportCanvas.write("Bulk");
                break;
            case 3:
                reportCanvas.write("Interrupt");
                break;
        }
        reportCanvas.closeListItem();
        if (i == 1) {
            reportCanvas.openListItem();
            reportCanvas.write("Aync: ");
            int i2 = attributes & 12;
            if (i2 == 0) {
                reportCanvas.write("NONE");
            } else if (i2 == 4) {
                reportCanvas.write("ASYNC");
            } else if (i2 == 8) {
                reportCanvas.write("ADAPTIVE ASYNC");
            }
            reportCanvas.closeListItem();
            reportCanvas.openListItem();
            reportCanvas.write("Useage: ");
            int i3 = attributes & 48;
            if (i3 == 0) {
                reportCanvas.write("DATA");
            } else if (i3 == 16) {
                reportCanvas.write("FEEDBACK");
            } else if (i3 != 32) {
                if (i3 == 48) {
                    reportCanvas.write("RESERVED");
                }
            } else {
                reportCanvas.write("EXPLICIT FEEDBACK");
            }
            reportCanvas.closeListItem();
        }
        reportCanvas.writeListItem("Package Size: " + getPacketSize());
        reportCanvas.writeListItem("Interval: " + getInterval());
        reportCanvas.closeList();
    }
}
