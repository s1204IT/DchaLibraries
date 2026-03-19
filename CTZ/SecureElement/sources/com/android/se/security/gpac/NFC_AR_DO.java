package com.android.se.security.gpac;

import java.io.ByteArrayOutputStream;

public class NFC_AR_DO extends BerTlv {
    public static final int TAG = 209;
    private boolean mNfcAllowed;

    public NFC_AR_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mNfcAllowed = false;
    }

    public NFC_AR_DO(boolean z) {
        super(null, TAG, 0, 0);
        this.mNfcAllowed = false;
        this.mNfcAllowed = z;
    }

    public boolean isNfcAllowed() {
        return this.mNfcAllowed;
    }

    @Override
    public void interpret() throws ParserException {
        this.mNfcAllowed = false;
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getValueLength() + valueIndex > rawData.length) {
            throw new ParserException("Not enough data for NFC_AR_DO!");
        }
        if (getValueLength() != 1) {
            throw new ParserException("Invalid length of NFC-AR-DO!");
        }
        if (rawData[valueIndex] != 1 && rawData[valueIndex] != 0) {
            throw new ParserException("Invalid value of NFC-AR-DO : " + String.format("%02x", Integer.valueOf(rawData[valueIndex] & 255)));
        }
        this.mNfcAllowed = rawData[valueIndex] == 1;
    }

    @Override
    public void build(ByteArrayOutputStream byteArrayOutputStream) throws DO_Exception {
        byteArrayOutputStream.write(getTag());
        byteArrayOutputStream.write(1);
        byteArrayOutputStream.write(this.mNfcAllowed ? 1 : 0);
    }
}
