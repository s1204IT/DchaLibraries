package com.android.server.usb.descriptors.report;

import android.net.util.NetworkConstants;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.UsbDescriptorParser;

public abstract class ReportCanvas {
    private static final String TAG = "ReportCanvas";
    private final UsbDescriptorParser mParser;

    public abstract void closeHeader(int i);

    public abstract void closeList();

    public abstract void closeListItem();

    public abstract void closeParagraph();

    public abstract void openHeader(int i);

    public abstract void openList();

    public abstract void openListItem();

    public abstract void openParagraph(boolean z);

    public abstract void write(String str);

    public abstract void writeParagraph(String str, boolean z);

    public ReportCanvas(UsbDescriptorParser usbDescriptorParser) {
        this.mParser = usbDescriptorParser;
    }

    public UsbDescriptorParser getParser() {
        return this.mParser;
    }

    public void writeHeader(int i, String str) {
        openHeader(i);
        write(str);
        closeHeader(i);
    }

    public void writeListItem(String str) {
        openListItem();
        write(str);
        closeListItem();
    }

    public static String getHexString(byte b) {
        return "0x" + Integer.toHexString(b & 255).toUpperCase();
    }

    public static String getBCDString(int i) {
        return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((i >> 8) & 15) + "." + ((i >> 4) & 15) + (i & 15);
    }

    public static String getHexString(int i) {
        return "0x" + Integer.toHexString(i & NetworkConstants.ARP_HWTYPE_RESERVED_HI).toUpperCase();
    }

    public void dumpHexArray(byte[] bArr, StringBuilder sb) {
        if (bArr != null) {
            openParagraph(false);
            for (byte b : bArr) {
                sb.append(getHexString(b) + " ");
            }
            closeParagraph();
        }
    }
}
