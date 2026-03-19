package com.android.server.usb.descriptors;

public final class UsbACSelectorUnit extends UsbACInterface {
    private static final String TAG = "UsbACSelectorUnit";
    private byte mNameIndex;
    private byte mNumPins;
    private byte[] mSourceIDs;
    private byte mUnitID;

    public UsbACSelectorUnit(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    public byte getUnitID() {
        return this.mUnitID;
    }

    public byte getNumPins() {
        return this.mNumPins;
    }

    public byte[] getSourceIDs() {
        return this.mSourceIDs;
    }

    public byte getNameIndex() {
        return this.mNameIndex;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mUnitID = byteStream.getByte();
        this.mNumPins = byteStream.getByte();
        this.mSourceIDs = new byte[this.mNumPins];
        for (int i = 0; i < this.mNumPins; i++) {
            this.mSourceIDs[i] = byteStream.getByte();
        }
        this.mNameIndex = byteStream.getByte();
        return this.mLength;
    }
}
