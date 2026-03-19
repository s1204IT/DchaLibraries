package com.android.internal.telephony.cdma;

import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.telephony.SmsMessage;
import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.CdmaSmsSubaddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;

public class SmsMessageConverter {
    private static final String LOGGABLE_TAG = "CDMA:SMS";
    static final String LOG_TAG = "SmsMessageConverter";
    private static final boolean VDBG = false;

    public static SmsMessage newCdmaSmsMessageFromRil(CdmaSmsMessage cdmaSmsMessage) {
        SmsEnvelope smsEnvelope = new SmsEnvelope();
        CdmaSmsAddress cdmaSmsAddress = new CdmaSmsAddress();
        CdmaSmsSubaddress cdmaSmsSubaddress = new CdmaSmsSubaddress();
        smsEnvelope.teleService = cdmaSmsMessage.teleserviceId;
        if (cdmaSmsMessage.isServicePresent) {
            smsEnvelope.messageType = 1;
        } else if (smsEnvelope.teleService == 0) {
            smsEnvelope.messageType = 2;
        } else {
            smsEnvelope.messageType = 0;
        }
        smsEnvelope.serviceCategory = cdmaSmsMessage.serviceCategory;
        int i = cdmaSmsMessage.address.digitMode;
        cdmaSmsAddress.digitMode = (byte) (255 & i);
        cdmaSmsAddress.numberMode = (byte) (cdmaSmsMessage.address.numberMode & 255);
        cdmaSmsAddress.ton = cdmaSmsMessage.address.numberType;
        cdmaSmsAddress.numberPlan = (byte) (255 & cdmaSmsMessage.address.numberPlan);
        int size = (byte) cdmaSmsMessage.address.digits.size();
        cdmaSmsAddress.numberOfDigits = size;
        byte[] bArr = new byte[size];
        for (int i2 = 0; i2 < size; i2++) {
            bArr[i2] = cdmaSmsMessage.address.digits.get(i2).byteValue();
            if (i == 0) {
                bArr[i2] = SmsMessage.convertDtmfToAscii(bArr[i2]);
            }
        }
        cdmaSmsAddress.origBytes = bArr;
        cdmaSmsSubaddress.type = cdmaSmsMessage.subAddress.subaddressType;
        cdmaSmsSubaddress.odd = cdmaSmsMessage.subAddress.odd ? (byte) 1 : (byte) 0;
        int size2 = (byte) cdmaSmsMessage.subAddress.digits.size();
        if (size2 < 0) {
            size2 = 0;
        }
        byte[] bArr2 = new byte[size2];
        for (int i3 = 0; i3 < size2; i3++) {
            bArr2[i3] = cdmaSmsMessage.subAddress.digits.get(i3).byteValue();
        }
        cdmaSmsSubaddress.origBytes = bArr2;
        int size3 = cdmaSmsMessage.bearerData.size();
        if (size3 < 0) {
            size3 = 0;
        }
        byte[] bArr3 = new byte[size3];
        for (int i4 = 0; i4 < size3; i4++) {
            bArr3[i4] = cdmaSmsMessage.bearerData.get(i4).byteValue();
        }
        smsEnvelope.bearerData = bArr3;
        smsEnvelope.origAddress = cdmaSmsAddress;
        smsEnvelope.origSubaddress = cdmaSmsSubaddress;
        return new SmsMessage(cdmaSmsAddress, smsEnvelope);
    }

    public static SmsMessage newSmsMessageFromCdmaSmsMessage(CdmaSmsMessage cdmaSmsMessage) {
        return new SmsMessage(newCdmaSmsMessageFromRil(cdmaSmsMessage));
    }
}
