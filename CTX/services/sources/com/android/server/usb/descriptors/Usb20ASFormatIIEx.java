package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb20ASFormatIIEx extends UsbASFormat {
    private static final String TAG = "Usb20ASFormatIIEx";
    private byte mHeaderLength;
    private int mMaxBitRate;
    private int mSamplesPerFrame;
    private byte mSidebandProtocol;

    public Usb20ASFormatIIEx(int i, byte b, byte b2, byte b3, byte b4) {
        super(i, b, b2, b3, b4);
    }

    public int getMaxBitRate() {
        return this.mMaxBitRate;
    }

    public int getSamplesPerFrame() {
        return this.mSamplesPerFrame;
    }

    public byte getHeaderLength() {
        return this.mHeaderLength;
    }

    public byte getSidebandProtocol() {
        return this.mSidebandProtocol;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mMaxBitRate = byteStream.unpackUsbShort();
        this.mSamplesPerFrame = byteStream.unpackUsbShort();
        this.mHeaderLength = byteStream.getByte();
        this.mSidebandProtocol = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Max Bit Rate: " + getMaxBitRate());
        reportCanvas.writeListItem("Samples Per Frame: " + getSamplesPerFrame());
        reportCanvas.writeListItem("Header Length: " + ((int) getHeaderLength()));
        reportCanvas.writeListItem("Sideband Protocol: " + ((int) getSidebandProtocol()));
        reportCanvas.closeList();
    }
}
