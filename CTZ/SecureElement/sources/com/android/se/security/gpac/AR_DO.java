package com.android.se.security.gpac;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AR_DO extends BerTlv {
    public static final int TAG = 227;
    private APDU_AR_DO mApduAr;
    private NFC_AR_DO mNfcAr;

    public AR_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mApduAr = null;
        this.mNfcAr = null;
    }

    public AR_DO(APDU_AR_DO apdu_ar_do, NFC_AR_DO nfc_ar_do) {
        super(null, TAG, 0, 0);
        this.mApduAr = null;
        this.mNfcAr = null;
        this.mApduAr = apdu_ar_do;
        this.mNfcAr = nfc_ar_do;
    }

    public APDU_AR_DO getApduArDo() {
        return this.mApduAr;
    }

    public NFC_AR_DO getNfcArDo() {
        return this.mNfcAr;
    }

    @Override
    public void interpret() throws ParserException {
        this.mApduAr = null;
        this.mNfcAr = null;
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getValueLength() + valueIndex > rawData.length) {
            throw new ParserException("Not enough data for AR_DO!");
        }
        do {
            BerTlv berTlvDecode = BerTlv.decode(rawData, valueIndex);
            if (berTlvDecode.getTag() == 208) {
                this.mApduAr = new APDU_AR_DO(rawData, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                this.mApduAr.interpret();
            } else if (berTlvDecode.getTag() == 209) {
                this.mNfcAr = new NFC_AR_DO(rawData, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                this.mNfcAr.interpret();
            }
            valueIndex = berTlvDecode.getValueLength() + berTlvDecode.getValueIndex();
        } while (getValueIndex() + getValueLength() > valueIndex);
        if (this.mApduAr == null && this.mNfcAr == null) {
            throw new ParserException("No valid DO in AR-DO!");
        }
    }

    @Override
    public void build(ByteArrayOutputStream byteArrayOutputStream) throws DO_Exception {
        byteArrayOutputStream.write(getTag());
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
        if (this.mApduAr != null) {
            this.mApduAr.build(byteArrayOutputStream2);
        }
        if (this.mNfcAr != null) {
            this.mNfcAr.build(byteArrayOutputStream2);
        }
        BerTlv.encodeLength(byteArrayOutputStream2.size(), byteArrayOutputStream);
        try {
            byteArrayOutputStream.write(byteArrayOutputStream2.toByteArray());
        } catch (IOException e) {
            throw new DO_Exception("AR-DO Memory IO problem! " + e.getMessage());
        }
    }
}
