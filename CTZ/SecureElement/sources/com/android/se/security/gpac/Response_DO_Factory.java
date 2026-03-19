package com.android.se.security.gpac;

public class Response_DO_Factory {
    public static BerTlv createDO(byte[] bArr) throws ParserException {
        BerTlv response_RefreshTag_DO;
        BerTlv berTlvDecode = BerTlv.decode(bArr, 0);
        int tag = berTlvDecode.getTag();
        if (tag == 57120) {
            response_RefreshTag_DO = new Response_RefreshTag_DO(bArr, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
        } else if (tag == 65344) {
            response_RefreshTag_DO = new Response_ALL_AR_DO(bArr, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
        } else if (tag == 65360) {
            response_RefreshTag_DO = new Response_AR_DO(bArr, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
        } else if (tag == 65392) {
            response_RefreshTag_DO = new Response_ARAC_AID_DO(bArr, berTlvDecode.getValueIndex(), berTlvDecode.getValueLength());
        } else {
            response_RefreshTag_DO = berTlvDecode;
        }
        response_RefreshTag_DO.interpret();
        return response_RefreshTag_DO;
    }
}
