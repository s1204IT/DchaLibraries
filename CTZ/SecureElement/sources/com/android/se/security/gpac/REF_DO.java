package com.android.se.security.gpac;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class REF_DO extends BerTlv {
    public static final int TAG = 225;
    private AID_REF_DO mAidDo;
    private Hash_REF_DO mHashDo;

    public REF_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mAidDo = null;
        this.mHashDo = null;
    }

    public REF_DO(AID_REF_DO aid_ref_do, Hash_REF_DO hash_REF_DO) {
        super(null, TAG, 0, 0);
        this.mAidDo = null;
        this.mHashDo = null;
        this.mAidDo = aid_ref_do;
        this.mHashDo = hash_REF_DO;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("REF_DO: ");
        if (this.mAidDo != null) {
            sb.append(this.mAidDo.toString());
            sb.append(' ');
        }
        if (this.mHashDo != null) {
            sb.append(this.mHashDo.toString());
        }
        return sb.toString();
    }

    public AID_REF_DO getAidDo() {
        return this.mAidDo;
    }

    public Hash_REF_DO getHashDo() {
        return this.mHashDo;
    }

    @Override
    public void interpret() throws ParserException {
        this.mAidDo = null;
        this.mHashDo = null;
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getValueLength() + valueIndex > rawData.length) {
            throw new ParserException("Not enough data for AR_DO!");
        }
        do {
            BerTlv berTlvDecode = BerTlv.decode(rawData, valueIndex);
            if (berTlvDecode.getTag() == 79 || berTlvDecode.getTag() == 192) {
                this.mAidDo = new AID_REF_DO(rawData, berTlvDecode.getTag(), berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                this.mAidDo.interpret();
            } else if (berTlvDecode.getTag() == 193) {
                this.mHashDo = new Hash_REF_DO(rawData, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                this.mHashDo.interpret();
            }
            valueIndex = berTlvDecode.getValueLength() + berTlvDecode.getValueIndex();
        } while (getValueIndex() + getValueLength() > valueIndex);
        if (this.mAidDo == null) {
            throw new ParserException("Missing AID-REF-DO in REF-DO!");
        }
        if (this.mHashDo == null) {
            throw new ParserException("Missing Hash-REF-DO in REF-DO!");
        }
    }

    @Override
    public void build(ByteArrayOutputStream byteArrayOutputStream) throws DO_Exception {
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
        if (this.mAidDo == null || this.mHashDo == null) {
            throw new DO_Exception("REF-DO: Required DO missing!");
        }
        this.mAidDo.build(byteArrayOutputStream2);
        this.mHashDo.build(byteArrayOutputStream2);
        byte[] byteArray = byteArrayOutputStream2.toByteArray();
        new BerTlv(byteArray, getTag(), 0, byteArray.length).build(byteArrayOutputStream);
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof REF_DO) && getTag() == obj.getTag() && AID_REF_DO.equals(this.mAidDo, obj.mAidDo) && Hash_REF_DO.equals(this.mHashDo, obj.mHashDo)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            build(byteArrayOutputStream);
            return Arrays.hashCode(byteArrayOutputStream.toByteArray());
        } catch (DO_Exception e) {
            return 1;
        }
    }
}
