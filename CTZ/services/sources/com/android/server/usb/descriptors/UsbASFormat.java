package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;

public class UsbASFormat extends UsbACInterface {
    public static final byte EXT_FORMAT_TYPE_I = -127;
    public static final byte EXT_FORMAT_TYPE_II = -126;
    public static final byte EXT_FORMAT_TYPE_III = -125;
    public static final byte FORMAT_TYPE_I = 1;
    public static final byte FORMAT_TYPE_II = 2;
    public static final byte FORMAT_TYPE_III = 3;
    public static final byte FORMAT_TYPE_IV = 4;
    private static final String TAG = "UsbASFormat";
    private final byte mFormatType;

    public UsbASFormat(int i, byte b, byte b2, byte b3, int i2) {
        super(i, b, b2, i2);
        this.mFormatType = b3;
    }

    public byte getFormatType() {
        return this.mFormatType;
    }

    public int[] getSampleRates() {
        return null;
    }

    public int[] getBitDepths() {
        return null;
    }

    public int[] getChannelCounts() {
        return null;
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser usbDescriptorParser, ByteStream byteStream, int i, byte b, byte b2, int i2) {
        byte b3 = byteStream.getByte();
        int aCInterfaceSpec = usbDescriptorParser.getACInterfaceSpec();
        switch (b3) {
            case 1:
                if (aCInterfaceSpec == 512) {
                    return new Usb20ASFormatI(i, b, b2, b3, i2);
                }
                return new Usb10ASFormatI(i, b, b2, b3, i2);
            case 2:
                if (aCInterfaceSpec == 512) {
                    return new Usb20ASFormatII(i, b, b2, b3, i2);
                }
                return new Usb10ASFormatII(i, b, b2, b3, i2);
            case 3:
                return new Usb20ASFormatIII(i, b, b2, b3, i2);
            default:
                return new UsbASFormat(i, b, b2, b3, i2);
        }
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.writeParagraph(UsbStrings.getFormatName(getFormatType()), false);
    }
}
