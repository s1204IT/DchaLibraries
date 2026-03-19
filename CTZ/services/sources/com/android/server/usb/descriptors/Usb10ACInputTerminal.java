package com.android.server.usb.descriptors;

import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACInputTerminal extends UsbACTerminal {
    private static final String TAG = "Usb10ACInputTerminal";
    private int mChannelConfig;
    private byte mChannelNames;
    private byte mNrChannels;
    private byte mTerminal;

    public Usb10ACInputTerminal(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    public byte getNrChannels() {
        return this.mNrChannels;
    }

    public int getChannelConfig() {
        return this.mChannelConfig;
    }

    public byte getChannelNames() {
        return this.mChannelNames;
    }

    public byte getTerminal() {
        return this.mTerminal;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        this.mNrChannels = byteStream.getByte();
        this.mChannelConfig = byteStream.unpackUsbShort();
        this.mChannelNames = byteStream.getByte();
        this.mTerminal = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Associated Terminal: " + ReportCanvas.getHexString(getAssocTerminal()));
        reportCanvas.writeListItem(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((int) getNrChannels()) + " Chans. Config: " + ReportCanvas.getHexString(getChannelConfig()));
        reportCanvas.closeList();
    }
}
