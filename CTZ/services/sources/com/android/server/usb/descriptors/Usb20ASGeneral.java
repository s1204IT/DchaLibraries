package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb20ASGeneral extends UsbACInterface {
    private static final String TAG = "Usb20ASGeneral";
    private int mChannelConfig;
    private byte mChannelNames;
    private byte mControls;
    private byte mFormatType;
    private int mFormats;
    private byte mNumChannels;
    private byte mTerminalLink;

    public Usb20ASGeneral(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    public byte getTerminalLink() {
        return this.mTerminalLink;
    }

    public byte getControls() {
        return this.mControls;
    }

    public byte getFormatType() {
        return this.mFormatType;
    }

    public int getFormats() {
        return this.mFormats;
    }

    public byte getNumChannels() {
        return this.mNumChannels;
    }

    public int getChannelConfig() {
        return this.mChannelConfig;
    }

    public byte getChannelNames() {
        return this.mChannelNames;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mTerminalLink = byteStream.getByte();
        this.mControls = byteStream.getByte();
        this.mFormatType = byteStream.getByte();
        this.mFormats = byteStream.unpackUsbInt();
        this.mNumChannels = byteStream.getByte();
        this.mChannelConfig = byteStream.unpackUsbInt();
        this.mChannelNames = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Terminal Link: " + ((int) getTerminalLink()));
        reportCanvas.writeListItem("Controls: " + ReportCanvas.getHexString(getControls()));
        reportCanvas.writeListItem("Format Type: " + ReportCanvas.getHexString(getFormatType()));
        reportCanvas.writeListItem("Formats: " + ReportCanvas.getHexString(getFormats()));
        reportCanvas.writeListItem("Num Channels: " + ((int) getNumChannels()));
        reportCanvas.writeListItem("Channel Config: " + ReportCanvas.getHexString(getChannelConfig()));
        reportCanvas.writeListItem("Channel Names String ID: " + ((int) getChannelNames()));
        reportCanvas.closeList();
    }
}
