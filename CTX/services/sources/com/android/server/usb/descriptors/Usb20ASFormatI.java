package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb20ASFormatI extends UsbASFormat {
    private static final String TAG = "Usb20ASFormatI";
    private byte mBitResolution;
    private byte mSubSlotSize;

    public Usb20ASFormatI(int i, byte b, byte b2, byte b3, int i2) {
        super(i, b, b2, b3, i2);
    }

    public byte getSubSlotSize() {
        return this.mSubSlotSize;
    }

    public byte getBitResolution() {
        return this.mBitResolution;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mSubSlotSize = byteStream.getByte();
        this.mBitResolution = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Subslot Size: " + ((int) getSubSlotSize()));
        reportCanvas.writeListItem("Bit Resolution: " + ((int) getBitResolution()));
        reportCanvas.closeList();
    }
}
