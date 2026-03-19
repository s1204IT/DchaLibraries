package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class UsbMSMidiHeader extends UsbACInterface {
    private static final String TAG = "UsbMSMidiHeader";

    public UsbMSMidiHeader(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        byteStream.advance(this.mLength - byteStream.getReadCount());
        return this.mLength;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        super.report(reportCanvas);
        reportCanvas.writeHeader(3, "MS Midi Header: " + ReportCanvas.getHexString(getType()) + " SubType: " + ReportCanvas.getHexString(getSubclass()) + " Length: " + getLength());
    }
}
