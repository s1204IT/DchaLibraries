package com.android.se.security.gpac;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class REF_AR_DO extends BerTlv {
    public static final int TAG = 226;
    private AR_DO mArDo;
    private REF_DO mRefDo;

    public REF_AR_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mRefDo = null;
        this.mArDo = null;
    }

    public REF_AR_DO() {
        super(null, TAG, 0, 0);
        this.mRefDo = null;
        this.mArDo = null;
    }

    public REF_AR_DO(REF_DO ref_do, AR_DO ar_do) {
        super(null, TAG, 0, 0);
        this.mRefDo = null;
        this.mArDo = null;
        this.mRefDo = ref_do;
        this.mArDo = ar_do;
    }

    public REF_DO getRefDo() {
        return this.mRefDo;
    }

    public AR_DO getArDo() {
        return this.mArDo;
    }

    @Override
    public void interpret() throws ParserException {
        this.mRefDo = null;
        this.mArDo = null;
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getValueLength() + valueIndex > rawData.length) {
            throw new ParserException("Not enough data for AR_DO!");
        }
        do {
            BerTlv berTlvDecode = BerTlv.decode(rawData, valueIndex);
            if (berTlvDecode.getTag() == 225) {
                this.mRefDo = new REF_DO(rawData, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                this.mRefDo.interpret();
            } else if (berTlvDecode.getTag() == 227) {
                this.mArDo = new AR_DO(rawData, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                this.mArDo.interpret();
            }
            valueIndex = berTlvDecode.getValueLength() + berTlvDecode.getValueIndex();
        } while (getValueIndex() + getValueLength() > valueIndex);
        if (this.mRefDo == null) {
            throw new ParserException("Missing Ref-DO in REF-AR-DO!");
        }
        if (this.mArDo == null) {
            throw new ParserException("Missing AR-DO in REF-AR-DO!");
        }
    }

    @Override
    public void build(ByteArrayOutputStream byteArrayOutputStream) throws DO_Exception {
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
        if (this.mRefDo == null || this.mArDo == null) {
            throw new DO_Exception("REF-AR-DO: Required DO missing!");
        }
        byteArrayOutputStream.write(getTag());
        this.mRefDo.build(byteArrayOutputStream2);
        this.mArDo.build(byteArrayOutputStream2);
        byte[] byteArray = byteArrayOutputStream2.toByteArray();
        BerTlv.encodeLength(byteArray.length, byteArrayOutputStream);
        try {
            byteArrayOutputStream.write(byteArray);
        } catch (IOException e) {
            throw new DO_Exception("REF-AR-DO Memory IO problem! " + e.getMessage());
        }
    }
}
