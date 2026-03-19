package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDeviceConnection;
import com.android.server.usb.descriptors.report.UsbStrings;

public final class UsbBinaryParser {
    private static final boolean LOGGING = false;
    private static final String TAG = "UsbBinaryParser";

    private void dumpDescriptor(ByteStream byteStream, int i, byte b, StringBuilder sb) {
        sb.append("<p>");
        sb.append("<b> l: " + i + " t:0x" + Integer.toHexString(b) + " " + UsbStrings.getDescriptorName(b) + "</b><br>");
        for (int i2 = 2; i2 < i; i2++) {
            sb.append("0x" + Integer.toHexString(byteStream.getByte() & 255) + " ");
        }
        sb.append("</p>");
    }

    public void parseDescriptors(UsbDeviceConnection usbDeviceConnection, byte[] bArr, StringBuilder sb) {
        sb.append("<tt>");
        ByteStream byteStream = new ByteStream(bArr);
        while (byteStream.available() > 0) {
            dumpDescriptor(byteStream, byteStream.getByte() & 255, byteStream.getByte(), sb);
        }
        sb.append("</tt>");
    }
}
