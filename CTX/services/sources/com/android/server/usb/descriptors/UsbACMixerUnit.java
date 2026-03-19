package com.android.server.usb.descriptors;

public class UsbACMixerUnit extends UsbACInterface {
    private static final String TAG = "UsbACMixerUnit";
    protected byte[] mInputIDs;
    protected byte mNumInputs;
    protected byte mNumOutputs;
    protected byte mUnitID;

    public UsbACMixerUnit(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    public byte getUnitID() {
        return this.mUnitID;
    }

    public byte getNumInputs() {
        return this.mNumInputs;
    }

    public byte[] getInputIDs() {
        return this.mInputIDs;
    }

    public byte getNumOutputs() {
        return this.mNumOutputs;
    }

    protected static int calcControlArraySize(int i, int i2) {
        return ((i * i2) + 7) / 8;
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        this.mUnitID = byteStream.getByte();
        this.mNumInputs = byteStream.getByte();
        this.mInputIDs = new byte[this.mNumInputs];
        for (int i = 0; i < this.mNumInputs; i++) {
            this.mInputIDs[i] = byteStream.getByte();
        }
        this.mNumOutputs = byteStream.getByte();
        return this.mLength;
    }
}
