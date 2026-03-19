package com.android.server.usb.descriptors;

import android.util.Log;

abstract class UsbACEndpoint extends UsbDescriptor {
    private static final String TAG = "UsbACEndpoint";
    protected final int mSubclass;
    protected byte mSubtype;

    UsbACEndpoint(int i, byte b, int i2) {
        super(i, b);
        this.mSubclass = i2;
    }

    public int getSubclass() {
        return this.mSubclass;
    }

    public byte getSubtype() {
        return this.mSubtype;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mSubtype = byteStream.getByte();
        return this.mLength;
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser usbDescriptorParser, int i, byte b) {
        int usbSubclass = usbDescriptorParser.getCurInterface().getUsbSubclass();
        switch (usbSubclass) {
            case 1:
                return new UsbACAudioControlEndpoint(i, b, usbSubclass);
            case 2:
                return new UsbACAudioStreamEndpoint(i, b, usbSubclass);
            case 3:
                return new UsbACMidiEndpoint(i, b, usbSubclass);
            default:
                Log.w(TAG, "Unknown Audio Class Endpoint id:0x" + Integer.toHexString(usbSubclass));
                return null;
        }
    }
}
