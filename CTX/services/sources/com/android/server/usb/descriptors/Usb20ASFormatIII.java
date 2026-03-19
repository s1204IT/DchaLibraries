package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb20ASFormatIII extends UsbASFormat {
    private static final String TAG = "Usb20ASFormatIII";
    private byte mBitResolution;
    private byte mSubslotSize;

    public Usb20ASFormatIII(int i, byte b, byte b2, byte b3, int i2) {
        super(i, b, b2, b3, i2);
    }

    public byte getSubslotSize() {
        return this.mSubslotSize;
    }

    public byte getBitResolution() {
        return this.mBitResolution;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mSubslotSize = byteStream.getByte();
        this.mBitResolution = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Subslot Size: " + ((int) getSubslotSize()));
        reportCanvas.writeListItem("Bit Resolution: " + ((int) getBitResolution()));
        reportCanvas.closeList();
    }
}
