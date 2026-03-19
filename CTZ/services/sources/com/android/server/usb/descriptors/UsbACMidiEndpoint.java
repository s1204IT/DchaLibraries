package com.android.server.usb.descriptors;

import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class UsbACMidiEndpoint extends UsbACEndpoint {
    private static final String TAG = "UsbACMidiEndpoint";
    private byte[] mJackIds;
    private byte mNumJacks;

    @Override
    public int getSubclass() {
        return super.getSubclass();
    }

    @Override
    public byte getSubtype() {
        return super.getSubtype();
    }

    public UsbACMidiEndpoint(int i, byte b, int i2) {
        super(i, b, i2);
    }

    public byte getNumJacks() {
        return this.mNumJacks;
    }

    public byte[] getJackIds() {
        return this.mJackIds;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        this.mNumJacks = byteStream.getByte();
        this.mJackIds = new byte[this.mNumJacks];
        for (int i = 0; i < this.mNumJacks; i++) {
            this.mJackIds[i] = byteStream.getByte();
        }
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.writeHeader(3, "AC Midi Endpoint: " + ReportCanvas.getHexString(getType()) + " Length: " + getLength());
        reportCanvas.openList();
        reportCanvas.writeListItem(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((int) getNumJacks()) + " Jacks.");
        reportCanvas.closeList();
    }
}
