package com.android.internal.telephony.cat;

import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.net.UnknownHostException;

abstract class BipValueParser {
    BipValueParser() {
    }

    static BearerDesc retrieveBearerDesc(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        int valueIndex2 = valueIndex + 1;
        try {
            int bearerType = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
            CatLog.d("CAT", "retrieveBearerDesc: bearerType:" + bearerType + ", length: " + length);
            if (2 == bearerType) {
                GPRSBearerDesc gprsbearerDesc = new GPRSBearerDesc();
                int valueIndex3 = valueIndex2 + 1;
                try {
                    gprsbearerDesc.precedence = rawValue[valueIndex2] & PplMessageManager.Type.INVALID;
                    int valueIndex4 = valueIndex3 + 1;
                    try {
                        gprsbearerDesc.delay = rawValue[valueIndex3] & PplMessageManager.Type.INVALID;
                        int valueIndex5 = valueIndex4 + 1;
                        gprsbearerDesc.reliability = rawValue[valueIndex4] & PplMessageManager.Type.INVALID;
                        int valueIndex6 = valueIndex5 + 1;
                        gprsbearerDesc.peak = rawValue[valueIndex5] & PplMessageManager.Type.INVALID;
                        int valueIndex7 = valueIndex6 + 1;
                        gprsbearerDesc.mean = rawValue[valueIndex6] & PplMessageManager.Type.INVALID;
                        valueIndex4 = valueIndex7 + 1;
                        gprsbearerDesc.pdpType = rawValue[valueIndex7] & PplMessageManager.Type.INVALID;
                        return gprsbearerDesc;
                    } catch (IndexOutOfBoundsException e) {
                    }
                } catch (IndexOutOfBoundsException e2) {
                }
            } else if (9 == bearerType) {
                UTranBearerDesc uTranbearerDesc = new UTranBearerDesc();
                int valueIndex8 = valueIndex2 + 1;
                try {
                    uTranbearerDesc.trafficClass = rawValue[valueIndex2] & PplMessageManager.Type.INVALID;
                    int valueIndex9 = valueIndex8 + 1;
                    try {
                        uTranbearerDesc.maxBitRateUL_High = rawValue[valueIndex8] & PplMessageManager.Type.INVALID;
                        int valueIndex10 = valueIndex9 + 1;
                        uTranbearerDesc.maxBitRateUL_Low = rawValue[valueIndex9] & PplMessageManager.Type.INVALID;
                        int valueIndex11 = valueIndex10 + 1;
                        uTranbearerDesc.maxBitRateDL_High = rawValue[valueIndex10] & PplMessageManager.Type.INVALID;
                        int valueIndex12 = valueIndex11 + 1;
                        uTranbearerDesc.maxBitRateDL_Low = rawValue[valueIndex11] & PplMessageManager.Type.INVALID;
                        int valueIndex13 = valueIndex12 + 1;
                        uTranbearerDesc.guarBitRateUL_High = rawValue[valueIndex12] & PplMessageManager.Type.INVALID;
                        int valueIndex14 = valueIndex13 + 1;
                        uTranbearerDesc.guarBitRateUL_Low = rawValue[valueIndex13] & PplMessageManager.Type.INVALID;
                        int valueIndex15 = valueIndex14 + 1;
                        uTranbearerDesc.guarBitRateDL_High = rawValue[valueIndex14] & PplMessageManager.Type.INVALID;
                        int valueIndex16 = valueIndex15 + 1;
                        uTranbearerDesc.guarBitRateDL_Low = rawValue[valueIndex15] & PplMessageManager.Type.INVALID;
                        int valueIndex17 = valueIndex16 + 1;
                        uTranbearerDesc.deliveryOrder = rawValue[valueIndex16] & PplMessageManager.Type.INVALID;
                        int valueIndex18 = valueIndex17 + 1;
                        uTranbearerDesc.maxSduSize = rawValue[valueIndex17] & PplMessageManager.Type.INVALID;
                        int valueIndex19 = valueIndex18 + 1;
                        uTranbearerDesc.sduErrorRatio = rawValue[valueIndex18] & PplMessageManager.Type.INVALID;
                        int valueIndex20 = valueIndex19 + 1;
                        uTranbearerDesc.residualBitErrorRadio = rawValue[valueIndex19] & PplMessageManager.Type.INVALID;
                        int valueIndex21 = valueIndex20 + 1;
                        uTranbearerDesc.deliveryOfErroneousSdus = rawValue[valueIndex20] & PplMessageManager.Type.INVALID;
                        int valueIndex22 = valueIndex21 + 1;
                        uTranbearerDesc.transferDelay = rawValue[valueIndex21] & PplMessageManager.Type.INVALID;
                        valueIndex9 = valueIndex22 + 1;
                        uTranbearerDesc.trafficHandlingPriority = rawValue[valueIndex22] & PplMessageManager.Type.INVALID;
                        int i = valueIndex9 + 1;
                        uTranbearerDesc.pdpType = rawValue[valueIndex9] & PplMessageManager.Type.INVALID;
                        return uTranbearerDesc;
                    } catch (IndexOutOfBoundsException e3) {
                    }
                } catch (IndexOutOfBoundsException e4) {
                }
            } else if (11 == bearerType) {
                EUTranBearerDesc euTranbearerDesc = new EUTranBearerDesc();
                int valueIndex23 = valueIndex2 + 1;
                try {
                    euTranbearerDesc.QCI = rawValue[valueIndex2] & PplMessageManager.Type.INVALID;
                    int valueIndex24 = valueIndex23 + 1;
                    try {
                        euTranbearerDesc.maxBitRateU = rawValue[valueIndex23] & PplMessageManager.Type.INVALID;
                        int valueIndex25 = valueIndex24 + 1;
                        euTranbearerDesc.maxBitRateD = rawValue[valueIndex24] & PplMessageManager.Type.INVALID;
                        int valueIndex26 = valueIndex25 + 1;
                        euTranbearerDesc.guarBitRateU = rawValue[valueIndex25] & PplMessageManager.Type.INVALID;
                        int valueIndex27 = valueIndex26 + 1;
                        euTranbearerDesc.guarBitRateD = rawValue[valueIndex26] & PplMessageManager.Type.INVALID;
                        int valueIndex28 = valueIndex27 + 1;
                        euTranbearerDesc.maxBitRateUEx = rawValue[valueIndex27] & PplMessageManager.Type.INVALID;
                        int valueIndex29 = valueIndex28 + 1;
                        euTranbearerDesc.maxBitRateDEx = rawValue[valueIndex28] & PplMessageManager.Type.INVALID;
                        int valueIndex30 = valueIndex29 + 1;
                        euTranbearerDesc.guarBitRateUEx = rawValue[valueIndex29] & PplMessageManager.Type.INVALID;
                        int valueIndex31 = valueIndex30 + 1;
                        euTranbearerDesc.guarBitRateDEx = rawValue[valueIndex30] & PplMessageManager.Type.INVALID;
                        valueIndex24 = valueIndex31 + 1;
                        euTranbearerDesc.pdnType = rawValue[valueIndex31] & PplMessageManager.Type.INVALID;
                        return euTranbearerDesc;
                    } catch (IndexOutOfBoundsException e5) {
                    }
                } catch (IndexOutOfBoundsException e6) {
                }
            } else {
                if (3 == bearerType) {
                    DefaultBearerDesc defaultbearerDesc = new DefaultBearerDesc();
                    return defaultbearerDesc;
                }
                if (1 == bearerType) {
                    CatLog.d("CAT", "retrieveBearerDesc: unsupport CSD");
                    throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                }
                CatLog.d("CAT", "retrieveBearerDesc: un-understood bearer type");
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } catch (IndexOutOfBoundsException e7) {
        }
        CatLog.d("CAT", "retrieveBearerDesc: out of bounds");
        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
    }

    static int retrieveBufferSize(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            int size = ((rawValue[valueIndex] & PplMessageManager.Type.INVALID) << 8) + (rawValue[valueIndex + 1] & PplMessageManager.Type.INVALID);
            return size;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveBufferSize: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static String retrieveNetworkAccessName(ComprehensionTlv ctlv) throws ResultException {
        int valueIndex;
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex2 = ctlv.getValueIndex();
        String networkAccessName = null;
        try {
            int totalLen = ctlv.getLength();
            String stkNetworkAccessName = new String(rawValue, valueIndex2, totalLen);
            String stkNetworkIdentifier = null;
            String stkOperatorIdentifier = null;
            if (stkNetworkAccessName != null && totalLen > 0) {
                int valueIndex3 = valueIndex2 + 1;
                try {
                    int len = rawValue[valueIndex2];
                    if (totalLen > len) {
                        stkNetworkIdentifier = new String(rawValue, valueIndex3, len);
                        valueIndex = valueIndex3 + len;
                    } else {
                        valueIndex = valueIndex3;
                    }
                    CatLog.d("CAT", "totalLen:" + totalLen + ";" + valueIndex + ";" + len);
                    valueIndex3 = valueIndex;
                    while (totalLen > len + 1) {
                        totalLen -= len + 1;
                        int valueIndex4 = valueIndex3 + 1;
                        len = rawValue[valueIndex3];
                        CatLog.d("CAT", "next len: " + len);
                        if (totalLen > len) {
                            String tmp_string = new String(rawValue, valueIndex4, len);
                            if (stkOperatorIdentifier == null) {
                                stkOperatorIdentifier = tmp_string;
                            } else {
                                stkOperatorIdentifier = stkOperatorIdentifier + "." + tmp_string;
                            }
                        }
                        int valueIndex5 = valueIndex4 + len;
                        CatLog.d("CAT", "totalLen:" + totalLen + ";" + valueIndex5 + ";" + len);
                        valueIndex3 = valueIndex5;
                    }
                    if (stkNetworkIdentifier != null && stkOperatorIdentifier != null) {
                        networkAccessName = stkNetworkIdentifier + "." + stkOperatorIdentifier;
                    } else if (stkNetworkIdentifier != null) {
                        networkAccessName = stkNetworkIdentifier;
                    }
                    CatLog.d("CAT", "nw:" + stkNetworkIdentifier + ";" + stkOperatorIdentifier);
                } catch (IndexOutOfBoundsException e) {
                    CatLog.d("CAT", "retrieveNetworkAccessName: out of bounds");
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            return networkAccessName;
        } catch (IndexOutOfBoundsException e2) {
        }
    }

    static TransportProtocol retrieveTransportProtocol(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int valueIndex2 = valueIndex + 1;
        try {
            int protocolType = rawValue[valueIndex];
            int portNumber = ((rawValue[valueIndex2] & PplMessageManager.Type.INVALID) << 8) + (rawValue[valueIndex2 + 1] & PplMessageManager.Type.INVALID);
            return new TransportProtocol(protocolType, portNumber);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static OtherAddress retrieveOtherAddress(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int valueIndex2 = valueIndex + 1;
        try {
            int addressType = rawValue[valueIndex];
            if (33 != addressType) {
                return 87 == addressType ? null : null;
            }
            OtherAddress otherAddress = new OtherAddress(addressType, rawValue, valueIndex2);
            return otherAddress;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveOtherAddress: out of bounds");
            return null;
        } catch (UnknownHostException e2) {
            CatLog.d("CAT", "retrieveOtherAddress: unknown host");
            return null;
        }
    }

    static int retrieveChannelDataLength(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        CatLog.d("CAT", "valueIndex:" + valueIndex);
        try {
            int length = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
            return length;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveTransportProtocol: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static byte[] retrieveChannelData(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            byte[] channelData = new byte[ctlv.getLength()];
            System.arraycopy(rawValue, valueIndex, channelData, 0, channelData.length);
            return channelData;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("CAT", "retrieveChannelData: out of bounds");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static byte[] retrieveNextActionIndicator(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        byte[] nai = new byte[length];
        int index = 0;
        int valueIndex2 = valueIndex;
        while (index < length) {
            int index2 = index + 1;
            int valueIndex3 = valueIndex2 + 1;
            try {
                nai[index] = rawValue[valueIndex2];
                index = index2;
                valueIndex2 = valueIndex3;
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return nai;
    }
}
