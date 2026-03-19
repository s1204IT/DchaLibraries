package com.android.server.usb.descriptors;

public class UsbACAudioStreamEndpoint extends UsbACEndpoint {
    private static final String TAG = "UsbACAudioStreamEndpoint";

    @Override
    public int getSubclass() {
        return super.getSubclass();
    }

    @Override
    public byte getSubtype() {
        return super.getSubtype();
    }

    public UsbACAudioStreamEndpoint(int i, byte b, int i2) {
        super(i, b, i2);
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        byteStream.advance(this.mLength - byteStream.getReadCount());
        return this.mLength;
    }
}
