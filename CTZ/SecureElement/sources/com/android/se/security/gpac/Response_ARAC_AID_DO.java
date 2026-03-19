package com.android.se.security.gpac;

import java.util.ArrayList;

public class Response_ARAC_AID_DO extends BerTlv {
    public static final int TAG = 65392;
    private ArrayList<AID_REF_DO> mAidDos;

    public Response_ARAC_AID_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mAidDos = new ArrayList<>();
    }

    public ArrayList<AID_REF_DO> getAidRefDos() {
        return this.mAidDos;
    }

    @Override
    public void interpret() throws ParserException {
        this.mAidDos.clear();
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getValueLength() == 0) {
            return;
        }
        if (getValueLength() + valueIndex > rawData.length) {
            throw new ParserException("Not enough data for Response_ARAC_AID_DO!");
        }
        int valueLength = getValueLength() + valueIndex;
        do {
            BerTlv berTlvDecode = BerTlv.decode(rawData, valueIndex);
            if (berTlvDecode.getTag() == 79 || berTlvDecode.getTag() == 192) {
                AID_REF_DO aid_ref_do = new AID_REF_DO(rawData, berTlvDecode.getTag(), berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
                aid_ref_do.interpret();
                this.mAidDos.add(aid_ref_do);
            }
            valueIndex = berTlvDecode.getValueLength() + berTlvDecode.getValueIndex();
        } while (valueIndex < valueLength);
    }
}
