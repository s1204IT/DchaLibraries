package com.android.server.usb.descriptors;

import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb20ACInputTerminal extends UsbACTerminal {
    private static final String TAG = "Usb20ACInputTerminal";
    private int mChanConfig;
    private byte mChanNames;
    private byte mClkSourceID;
    private int mControls;
    private byte mNumChannels;
    private byte mTerminalName;

    public Usb20ACInputTerminal(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    public byte getClkSourceID() {
        return this.mClkSourceID;
    }

    public byte getNumChannels() {
        return this.mNumChannels;
    }

    public int getChanConfig() {
        return this.mChanConfig;
    }

    public int getControls() {
        return this.mControls;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        this.mClkSourceID = byteStream.getByte();
        this.mNumChannels = byteStream.getByte();
        this.mChanConfig = byteStream.unpackUsbInt();
        this.mChanNames = byteStream.getByte();
        this.mControls = byteStream.unpackUsbShort();
        this.mTerminalName = byteStream.getByte();
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        reportCanvas.writeListItem("Clock Source: " + ((int) getClkSourceID()));
        reportCanvas.writeListItem(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((int) getNumChannels()) + " Channels. Config: " + ReportCanvas.getHexString(getChanConfig()));
        reportCanvas.closeList();
    }
}
