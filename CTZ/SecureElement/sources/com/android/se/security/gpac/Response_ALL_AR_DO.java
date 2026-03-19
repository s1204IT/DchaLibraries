package com.android.se.security.gpac;

import java.util.ArrayList;

public class Response_ALL_AR_DO extends BerTlv {
    public static final int TAG = 65344;
    private ArrayList<REF_AR_DO> mRefArDos;

    public Response_ALL_AR_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mRefArDos = new ArrayList<>();
    }

    public ArrayList<REF_AR_DO> getRefArDos() {
        return this.mRefArDos;
    }

    @Override
    public void interpret() throws ParserException {
        this.mRefArDos.clear();
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
            if (berTlvDecode.getTag() == 226) {
                REF_AR_DO ref_ar_do = new REF_AR_DO(rawData, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                ref_ar_do.interpret();
                this.mRefArDos.add(ref_ar_do);
            }
            valueIndex = berTlvDecode.getValueLength() + berTlvDecode.getValueIndex();
        } while (valueIndex < valueLength);
    }
}
