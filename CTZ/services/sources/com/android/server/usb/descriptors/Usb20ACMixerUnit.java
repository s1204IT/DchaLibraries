package com.android.server.usb.descriptors;

public final class Usb20ACMixerUnit extends UsbACMixerUnit {
    private static final String TAG = "Usb20ACMixerUnit";
    private int mChanConfig;
    private byte mChanNames;
    private byte[] mControls;
    private byte mControlsMask;
    private byte mNameID;

    public Usb20ACMixerUnit(int i, byte b, byte b2, int i2) {
        super(i, b, b2, i2);
    }

    @Override
    public int parseRawDescriptors(ByteStream byteStream) {
        super.parseRawDescriptors(byteStream);
        this.mChanConfig = byteStream.unpackUsbInt();
        this.mChanNames = byteStream.getByte();
        int iCalcControlArraySize = calcControlArraySize(this.mNumInputs, this.mNumOutputs);
        this.mControls = new byte[iCalcControlArraySize];
        for (int i = 0; i < iCalcControlArraySize; i++) {
            this.mControls[i] = byteStream.getByte();
        }
        this.mControlsMask = byteStream.getByte();
        this.mNameID = byteStream.getByte();
        return this.mLength;
    }
}
