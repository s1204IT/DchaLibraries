package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.ComprehensionTlv;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.ResultException;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.net.UnknownHostException;

abstract class BipValueParser {
    BipValueParser() {
    }

    static BearerDesc retrieveBearerDesc(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        int length = comprehensionTlv.getLength();
        int i = valueIndex + 1;
        try {
            int i2 = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
            MtkCatLog.d("CAT", "retrieveBearerDesc: bearerType:" + i2 + ", length: " + length);
            if (2 == i2) {
                GPRSBearerDesc gPRSBearerDesc = new GPRSBearerDesc();
                int i3 = i + 1;
                gPRSBearerDesc.precedence = rawValue[i] & PplMessageManager.Type.INVALID;
                int i4 = i3 + 1;
                gPRSBearerDesc.delay = rawValue[i3] & PplMessageManager.Type.INVALID;
                int i5 = i4 + 1;
                gPRSBearerDesc.reliability = rawValue[i4] & PplMessageManager.Type.INVALID;
                int i6 = i5 + 1;
                gPRSBearerDesc.peak = rawValue[i5] & PplMessageManager.Type.INVALID;
                gPRSBearerDesc.mean = rawValue[i6] & PplMessageManager.Type.INVALID;
                gPRSBearerDesc.pdpType = rawValue[i6 + 1] & PplMessageManager.Type.INVALID;
                return gPRSBearerDesc;
            }
            if (9 == i2) {
                UTranBearerDesc uTranBearerDesc = new UTranBearerDesc();
                int i7 = i + 1;
                uTranBearerDesc.trafficClass = rawValue[i] & PplMessageManager.Type.INVALID;
                int i8 = i7 + 1;
                uTranBearerDesc.maxBitRateUL_High = rawValue[i7] & PplMessageManager.Type.INVALID;
                int i9 = i8 + 1;
                uTranBearerDesc.maxBitRateUL_Low = rawValue[i8] & PplMessageManager.Type.INVALID;
                int i10 = i9 + 1;
                uTranBearerDesc.maxBitRateDL_High = rawValue[i9] & PplMessageManager.Type.INVALID;
                int i11 = i10 + 1;
                uTranBearerDesc.maxBitRateDL_Low = rawValue[i10] & PplMessageManager.Type.INVALID;
                int i12 = i11 + 1;
                uTranBearerDesc.guarBitRateUL_High = rawValue[i11] & PplMessageManager.Type.INVALID;
                int i13 = i12 + 1;
                uTranBearerDesc.guarBitRateUL_Low = rawValue[i12] & PplMessageManager.Type.INVALID;
                int i14 = i13 + 1;
                uTranBearerDesc.guarBitRateDL_High = rawValue[i13] & PplMessageManager.Type.INVALID;
                int i15 = i14 + 1;
                uTranBearerDesc.guarBitRateDL_Low = rawValue[i14] & PplMessageManager.Type.INVALID;
                int i16 = i15 + 1;
                uTranBearerDesc.deliveryOrder = rawValue[i15] & PplMessageManager.Type.INVALID;
                int i17 = i16 + 1;
                uTranBearerDesc.maxSduSize = rawValue[i16] & PplMessageManager.Type.INVALID;
                int i18 = i17 + 1;
                uTranBearerDesc.sduErrorRatio = rawValue[i17] & PplMessageManager.Type.INVALID;
                int i19 = i18 + 1;
                uTranBearerDesc.residualBitErrorRadio = rawValue[i18] & PplMessageManager.Type.INVALID;
                int i20 = i19 + 1;
                uTranBearerDesc.deliveryOfErroneousSdus = rawValue[i19] & PplMessageManager.Type.INVALID;
                int i21 = i20 + 1;
                uTranBearerDesc.transferDelay = rawValue[i20] & PplMessageManager.Type.INVALID;
                uTranBearerDesc.trafficHandlingPriority = rawValue[i21] & PplMessageManager.Type.INVALID;
                uTranBearerDesc.pdpType = rawValue[i21 + 1] & PplMessageManager.Type.INVALID;
                return uTranBearerDesc;
            }
            if (11 == i2) {
                EUTranBearerDesc eUTranBearerDesc = new EUTranBearerDesc();
                int i22 = i + 1;
                eUTranBearerDesc.QCI = rawValue[i] & PplMessageManager.Type.INVALID;
                int i23 = i22 + 1;
                eUTranBearerDesc.maxBitRateU = rawValue[i22] & PplMessageManager.Type.INVALID;
                int i24 = i23 + 1;
                eUTranBearerDesc.maxBitRateD = rawValue[i23] & PplMessageManager.Type.INVALID;
                int i25 = i24 + 1;
                eUTranBearerDesc.guarBitRateU = rawValue[i24] & PplMessageManager.Type.INVALID;
                int i26 = i25 + 1;
                eUTranBearerDesc.guarBitRateD = rawValue[i25] & PplMessageManager.Type.INVALID;
                int i27 = i26 + 1;
                eUTranBearerDesc.maxBitRateUEx = rawValue[i26] & PplMessageManager.Type.INVALID;
                int i28 = i27 + 1;
                eUTranBearerDesc.maxBitRateDEx = rawValue[i27] & PplMessageManager.Type.INVALID;
                int i29 = i28 + 1;
                eUTranBearerDesc.guarBitRateUEx = rawValue[i28] & PplMessageManager.Type.INVALID;
                eUTranBearerDesc.guarBitRateDEx = rawValue[i29] & PplMessageManager.Type.INVALID;
                eUTranBearerDesc.pdnType = rawValue[i29 + 1] & PplMessageManager.Type.INVALID;
                return eUTranBearerDesc;
            }
            if (3 == i2) {
                return new DefaultBearerDesc();
            }
            if (1 == i2) {
                MtkCatLog.d("CAT", "retrieveBearerDesc: unsupport CSD");
                throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
            }
            MtkCatLog.d("CAT", "retrieveBearerDesc: un-understood bearer type");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d("CAT", "retrieveBearerDesc: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int retrieveBufferSize(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        try {
            return ((rawValue[valueIndex] & PplMessageManager.Type.INVALID) << 8) + (rawValue[valueIndex + 1] & PplMessageManager.Type.INVALID);
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d("CAT", "retrieveBufferSize: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static String retrieveNetworkAccessName(ComprehensionTlv comprehensionTlv) throws ResultException {
        String str;
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        try {
            int length = comprehensionTlv.getLength();
            new String(rawValue, valueIndex, length);
            String str2 = null;
            if (length > 0) {
                int i = valueIndex + 1;
                byte b = rawValue[valueIndex];
                if (length > b) {
                    str = new String(rawValue, i, (int) b);
                    i += b;
                } else {
                    str = null;
                }
                MtkCatLog.d("CAT", "totalLen:" + length + ";" + i + ";" + ((int) b));
                String str3 = null;
                while (true) {
                    int i2 = b + 1;
                    if (length <= i2) {
                        break;
                    }
                    length -= i2;
                    int i3 = i + 1;
                    byte b2 = rawValue[i];
                    MtkCatLog.d("CAT", "next len: " + ((int) b2));
                    if (length > b2) {
                        String str4 = new String(rawValue, i3, (int) b2);
                        if (str3 != null) {
                            str4 = str3 + "." + str4;
                        }
                        str3 = str4;
                    }
                    int i4 = i3 + b2;
                    MtkCatLog.d("CAT", "totalLen:" + length + ";" + i4 + ";" + ((int) b2));
                    i = i4;
                    b = b2;
                }
                if (str != null && str3 != null) {
                    str2 = str + "." + str3;
                } else if (str != null) {
                    str2 = str;
                }
                MtkCatLog.d("CAT", "nw:" + str + ";" + str3);
            }
            return str2;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d("CAT", "retrieveNetworkAccessName: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static TransportProtocol retrieveTransportProtocol(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        int i = valueIndex + 1;
        try {
            return new TransportProtocol(rawValue[valueIndex], ((rawValue[i] & PplMessageManager.Type.INVALID) << 8) + (rawValue[i + 1] & PplMessageManager.Type.INVALID));
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static OtherAddress retrieveOtherAddress(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        int i = valueIndex + 1;
        try {
            byte b = rawValue[valueIndex];
            if (33 == b) {
                return new OtherAddress(b, rawValue, i);
            }
            if (87 == b) {
                return new OtherAddress(b, rawValue, i);
            }
            return null;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d("CAT", "retrieveOtherAddress: out of bounds");
            return null;
        } catch (UnknownHostException e2) {
            MtkCatLog.d("CAT", "retrieveOtherAddress: unknown host");
            return null;
        }
    }

    static int retrieveChannelDataLength(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        MtkCatLog.d("CAT", "valueIndex:" + valueIndex);
        try {
            return rawValue[valueIndex] & PplMessageManager.Type.INVALID;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static byte[] retrieveChannelData(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        try {
            byte[] bArr = new byte[comprehensionTlv.getLength()];
            System.arraycopy(rawValue, valueIndex, bArr, 0, bArr.length);
            return bArr;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d("CAT", "retrieveChannelData: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static byte[] retrieveNextActionIndicator(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        int length = comprehensionTlv.getLength();
        byte[] bArr = new byte[length];
        int i = 0;
        while (i < length) {
            int i2 = i + 1;
            int i3 = valueIndex + 1;
            try {
                bArr[i] = rawValue[valueIndex];
                i = i2;
                valueIndex = i3;
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return bArr;
    }
}
