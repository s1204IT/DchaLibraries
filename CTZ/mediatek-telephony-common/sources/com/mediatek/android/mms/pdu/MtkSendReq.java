package com.mediatek.android.mms.pdu;

import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.SendReq;

public class MtkSendReq extends SendReq {
    private static final String TAG = "MtkSendReq";

    public MtkSendReq() {
    }

    public MtkSendReq(byte[] bArr, MtkEncodedStringValue mtkEncodedStringValue, int i, byte[] bArr2) throws InvalidHeaderValueException {
        super(bArr, mtkEncodedStringValue, i, bArr2);
    }

    MtkSendReq(MtkPduHeaders mtkPduHeaders) {
        super(mtkPduHeaders);
    }

    MtkSendReq(MtkPduHeaders mtkPduHeaders, PduBody pduBody) {
        super(mtkPduHeaders, pduBody);
    }

    public long getDateSent() {
        return this.mPduHeaders.getLongInteger(201);
    }
}
