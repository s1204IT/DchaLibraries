package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb20ACOutputTerminal extends UsbACTerminal {
    private static final String TAG = "Usb20ACOutputTerminal";
    private byte mClkSoureID;
    private int mControls;
    private byte mSourceID;
    private byte mTerminalID;

    public Usb20ACOutputTerminal(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    public byte getSourceID() {
        return this.mSourceID;
    }

    public byte getClkSourceID() {
        return this.mClkSoureID;
    }

    public int getControls() {
        return this.mControls;
    }

    @Override
    public byte getTerminalID() {
        return this.mTerminalID;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        this.mSourceID = byteStream.getByte();
        this.mClkSoureID = byteStream.getByte();
        this.mControls = byteStream.unpackUsbShort();
        this.mTerminalID = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Clock Source ID: " + ((int) getClkSourceID()));
        reportCanvas.writeListItem("Controls: " + ReportCanvas.getHexString(getControls()));
        reportCanvas.writeListItem("Terminal Name ID: " + ((int) getTerminalID()));
        reportCanvas.closeList();
    }
}
