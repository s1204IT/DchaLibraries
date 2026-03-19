package com.android.server.usb.descriptors;

import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACHeader extends UsbACHeaderInterface {
    private static final String TAG = "Usb10ACHeader";
    private byte mControls;
    private byte[] mInterfaceNums;
    private byte mNumInterfaces;

    public Usb10ACHeader(int i, byte b, byte b2, int i2, int i3) {
        super(i, b, b2, i2, i3);
        this.mNumInterfaces = (byte) 0;
        this.mInterfaceNums = null;
    }

    public byte getNumInterfaces() {
        return this.mNumInterfaces;
    }

    public byte[] getInterfaceNums() {
        return this.mInterfaceNums;
    }

    public byte getControls() {
        return this.mControls;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mTotalLength = byteStream.unpackUsbShort();
        if (this.mADCRelease >= 512) {
            this.mControls = byteStream.getByte();
        } else {
            this.mNumInterfaces = byteStream.getByte();
            this.mInterfaceNums = new byte[this.mNumInterfaces];
            for (int i = 0; i < this.mNumInterfaces; i++) {
                this.mInterfaceNums[i] = byteStream.getByte();
            }
        }
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.openList();
        byte numInterfaces = getNumInterfaces();
        StringBuilder sb = new StringBuilder();
        sb.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((int) numInterfaces) + " Interfaces");
        if (numInterfaces > 0) {
            sb.append(" [");
            byte[] interfaceNums = getInterfaceNums();
            if (interfaceNums != null) {
                for (int i = 0; i < numInterfaces; i++) {
                    sb.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((int) interfaceNums[i]));
                    if (i < numInterfaces - 1) {
                        sb.append(" ");
                    }
                }
            }
            sb.append("]");
        }
        reportCanvas.writeListItem(sb.toString());
        reportCanvas.writeListItem("Controls: " + ReportCanvas.getHexString(getControls()));
        reportCanvas.closeList();
    }
}
