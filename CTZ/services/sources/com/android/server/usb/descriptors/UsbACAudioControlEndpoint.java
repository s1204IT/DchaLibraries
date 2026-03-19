package com.android.server.usb.descriptors;

public class UsbACAudioControlEndpoint extends UsbACEndpoint {
    static final byte ADDRESSMASK_DIRECTION = -128;
    static final byte ADDRESSMASK_ENDPOINT = 15;
    static final byte ATTRIBMASK_TRANS = 3;
    static final byte ATTRIBSMASK_SYNC = 12;
    private static final String TAG = "UsbACAudioControlEndpoint";
    private byte mAddress;
    private byte mAttribs;
    private byte mInterval;
    private int mMaxPacketSize;

    @Override
    public int getSubclass() {
        return super.getSubclass();
    }

    @Override
    public byte getSubtype() {
        return super.getSubtype();
    }

    public UsbACAudioControlEndpoint(int i, byte b, int i2) {
        super(i, b, i2);
    }

    public byte getAddress() {
        return this.mAddress;
    }

    public byte getAttribs() {
        return this.mAttribs;
    }

    public int getMaxPacketSize() {
        return this.mMaxPacketSize;
    }

    public byte getInterval() {
        return this.mInterval;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        this.mAddress = byteStream.getByte();
        this.mAttribs = byteStream.getByte();
        this.mMaxPacketSize = byteStream.unpackUsbShort();
        this.mInterval = byteStream.getByte();
        return this.mLength;
    }
}
