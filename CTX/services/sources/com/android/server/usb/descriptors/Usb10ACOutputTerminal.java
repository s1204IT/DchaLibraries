package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACOutputTerminal extends UsbACTerminal {
    private static final String TAG = "Usb10ACOutputTerminal";
    private byte mSourceID;
    private byte mTerminal;

    public Usb10ACOutputTerminal(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    public byte getSourceID() {
        return this.mSourceID;
    }

    public byte getTerminal() {
        return this.mTerminal;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        this.mSourceID = byteStream.getByte();
        this.mTerminal = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Source ID: " + ReportCanvas.getHexString(getSourceID()));
        reportCanvas.closeList();
    }
}
