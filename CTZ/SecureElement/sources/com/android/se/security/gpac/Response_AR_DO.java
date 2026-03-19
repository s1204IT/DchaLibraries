package com.android.se.security.gpac;

public class Response_AR_DO extends BerTlv {
    public static final int TAG = 65360;
    private AR_DO mArDo;

    public Response_AR_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mArDo = null;
    }

    public AR_DO getArDo() {
        return this.mArDo;
    }

    @Override
    public void interpret() throws ParserException {
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getValueLength() == 0) {
            return;
        }
        if (getValueLength() + valueIndex > rawData.length) {
            throw new ParserException("Not enough data for Response_AR_DO!");
        }
        int valueLength = getValueLength() + valueIndex;
        do {
            BerTlv berTlvDecode = BerTlv.decode(rawData, valueIndex);
            if (berTlvDecode.getTag() == 227) {
                this.mArDo = new AR_DO(rawData, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                this.mArDo.interpret();
            }
            valueIndex = berTlvDecode.getValueLength() + berTlvDecode.getValueIndex();
        } while (valueIndex < valueLength);
    }
}
