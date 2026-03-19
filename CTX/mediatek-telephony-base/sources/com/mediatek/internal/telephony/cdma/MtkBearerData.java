package com.mediatek.internal.telephony.cdma;

import android.telephony.Rlog;
import android.text.format.Time;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.cdma.sms.BearerData;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.util.BitwiseInputStream;
import com.android.internal.util.BitwiseOutputStream;
import mediatek.telephony.MtkServiceState;
import mediatek.telephony.MtkSmsCbCmasInfo;

public class MtkBearerData extends BearerData {
    private static final String LOG_TAG = "MtkBearerData";
    private static final byte SUBPARAM_MESSAGE_CENTER_TIME_STAMP = 3;
    private static final byte SUBPARAM_USER_DATA = 1;
    private static final byte UNENCODABLE_7_BIT_CHAR = 32;

    public static BearerData decode(byte[] bArr, int i) {
        BearerData bearerDataDecode = BearerData.decode(bArr, i);
        if (bearerDataDecode != null && bearerDataDecode.userData != null && BearerData.isCmasAlertCategory(i)) {
            try {
                BearerData bearerDataReGetUserData = reGetUserData(bArr, i);
                UserData userData = bearerDataDecode.userData;
                bearerDataDecode.userData = bearerDataReGetUserData.userData;
                decodeCmasUserData(bearerDataDecode, i);
                bearerDataDecode.userData = userData;
            } catch (BitwiseInputStream.AccessException e) {
                Rlog.e(LOG_TAG, "BearerData decode failed: " + e);
            } catch (BearerData.CodingException e2) {
                Rlog.e(LOG_TAG, "BearerData decode failed: " + e2);
                e2.printStackTrace();
            }
        }
        return bearerDataDecode;
    }

    public static byte[] encode(BearerData bearerData) {
        UserData userData;
        byte[] bArrEncodeUserData = new byte[0];
        if (isMtkEmsEncodingExt(bearerData)) {
            bArrEncodeUserData = encodeUserData(bearerData);
            if (bArrEncodeUserData == null) {
                return null;
            }
            userData = bearerData.userData;
            bearerData.userData = null;
        } else {
            userData = null;
        }
        byte[] bArrEncode = BearerData.encode(bearerData);
        if (userData != null) {
            bearerData.userData = userData;
        }
        if (bArrEncode == null) {
            return null;
        }
        bearerData.hasUserDataHeader = (bearerData.userData == null || bearerData.userData.userDataHeader == null) ? false : true;
        if (bearerData.hasUserDataHeader) {
            bArrEncode[4] = (byte) (((bArrEncode[4] & 255) | 8) & 255);
        }
        byte[] bArrEncodeMsgCenterTimeStamp = encodeMsgCenterTimeStamp(bearerData);
        byte[] bArr = new byte[bArrEncode.length + bArrEncodeUserData.length + bArrEncodeMsgCenterTimeStamp.length];
        System.arraycopy(bArrEncode, 0, bArr, 0, bArrEncode.length);
        System.arraycopy(bArrEncodeUserData, 0, bArr, bArrEncode.length, bArrEncodeUserData.length);
        System.arraycopy(bArrEncodeMsgCenterTimeStamp, 0, bArr, bArrEncode.length + bArrEncodeUserData.length, bArrEncodeMsgCenterTimeStamp.length);
        return bArr;
    }

    public static GsmAlphabet.TextEncodingDetails calcTextEncodingDetails(CharSequence charSequence, boolean z, int i) {
        int iCountAsciiSeptets = BearerData.countAsciiSeptets(charSequence, z);
        if (i == 3) {
            Rlog.d(LOG_TAG, "16bit in cdma");
            iCountAsciiSeptets = -1;
        }
        if (iCountAsciiSeptets != -1 && iCountAsciiSeptets <= 160) {
            GsmAlphabet.TextEncodingDetails textEncodingDetails = new GsmAlphabet.TextEncodingDetails();
            textEncodingDetails.msgCount = 1;
            textEncodingDetails.codeUnitCount = iCountAsciiSeptets;
            textEncodingDetails.codeUnitsRemaining = 160 - iCountAsciiSeptets;
            textEncodingDetails.codeUnitSize = 1;
            return textEncodingDetails;
        }
        Rlog.d(LOG_TAG, "gsm can understand the control character, but cdma ignore it(<0x20)");
        GsmAlphabet.TextEncodingDetails textEncodingDetailsCalcTextEncodingDetails = BearerData.calcTextEncodingDetails(charSequence, z, true);
        if (textEncodingDetailsCalcTextEncodingDetails.msgCount == 1 && textEncodingDetailsCalcTextEncodingDetails.codeUnitSize == 1) {
            textEncodingDetailsCalcTextEncodingDetails.codeUnitCount = charSequence.length();
            int i2 = textEncodingDetailsCalcTextEncodingDetails.codeUnitCount * 2;
            if (i2 > 140) {
                textEncodingDetailsCalcTextEncodingDetails.msgCount = (i2 + MtkServiceState.RIL_RADIO_TECHNOLOGY_DC_DPA) / MtkServiceState.RIL_RADIO_TECHNOLOGY_DC_UPA;
                textEncodingDetailsCalcTextEncodingDetails.codeUnitsRemaining = ((textEncodingDetailsCalcTextEncodingDetails.msgCount * MtkServiceState.RIL_RADIO_TECHNOLOGY_DC_UPA) - i2) / 2;
            } else {
                textEncodingDetailsCalcTextEncodingDetails.msgCount = 1;
                textEncodingDetailsCalcTextEncodingDetails.codeUnitsRemaining = (140 - i2) / 2;
            }
            textEncodingDetailsCalcTextEncodingDetails.codeUnitSize = 3;
        }
        return textEncodingDetailsCalcTextEncodingDetails;
    }

    private static byte cdmaIntToBcdByte(int i) {
        int i2 = i % 100;
        return (byte) ((i2 % 10) | ((i2 / 10) << 4));
    }

    private static void encodeMsgCenterTimeStamp(BearerData bearerData, BitwiseOutputStream bitwiseOutputStream) throws BitwiseOutputStream.AccessException {
        bitwiseOutputStream.write(8, 6);
        int i = bearerData.msgCenterTimeStamp.year - 2000;
        if (i < 0) {
            i = bearerData.msgCenterTimeStamp.year - 1900;
        }
        bitwiseOutputStream.write(8, cdmaIntToBcdByte(i));
        bitwiseOutputStream.write(8, cdmaIntToBcdByte(bearerData.msgCenterTimeStamp.month + 1));
        bitwiseOutputStream.write(8, cdmaIntToBcdByte(bearerData.msgCenterTimeStamp.monthDay));
        bitwiseOutputStream.write(8, cdmaIntToBcdByte(bearerData.msgCenterTimeStamp.hour));
        bitwiseOutputStream.write(8, cdmaIntToBcdByte(bearerData.msgCenterTimeStamp.minute));
        bitwiseOutputStream.write(8, cdmaIntToBcdByte(bearerData.msgCenterTimeStamp.second));
    }

    private static byte[] encodeMsgCenterTimeStamp(BearerData bearerData) {
        try {
            BitwiseOutputStream bitwiseOutputStream = new BitwiseOutputStream(200);
            if (bearerData.msgCenterTimeStamp != null) {
                bitwiseOutputStream.write(8, 3);
                encodeMsgCenterTimeStamp(bearerData, bitwiseOutputStream);
                return bitwiseOutputStream.toByteArray();
            }
        } catch (BitwiseOutputStream.AccessException e) {
            Rlog.e(LOG_TAG, "BearerData encode failed: " + e);
        }
        return new byte[0];
    }

    private static boolean isMtkEmsEncodingExt(BearerData bearerData) {
        if (bearerData.userData == null || bearerData.userData.userDataHeader == null || !bearerData.userData.msgEncodingSet) {
            return false;
        }
        if (bearerData.userData.msgEncoding != 2 && bearerData.userData.msgEncoding != 0) {
            return false;
        }
        return true;
    }

    private static byte[] encodeUserData(BearerData bearerData) {
        if (bearerData.userData.payloadStr == null && bearerData.userData.msgEncoding != 0) {
            Rlog.e(LOG_TAG, "user data with null payloadStr");
            bearerData.userData.payloadStr = "";
        }
        try {
            BitwiseOutputStream bitwiseOutputStream = new BitwiseOutputStream(200);
            bitwiseOutputStream.write(8, 1);
            byte[] byteArray = SmsHeader.toByteArray(bearerData.userData.userDataHeader);
            if (bearerData.userData.msgEncoding == 2) {
                encode7bitAsciiEms(bearerData.userData, byteArray, true);
            }
            bearerData.hasUserDataHeader = bearerData.userData.userDataHeader != null;
            if (bearerData.userData.payload.length > 140) {
                throw new BearerData.CodingException("encoded user data too large (" + bearerData.userData.payload.length + " > 140 bytes)");
            }
            int length = (bearerData.userData.payload.length * 8) - bearerData.userData.paddingBits;
            int i = length + 13;
            if (bearerData.userData.msgEncoding == 1 || bearerData.userData.msgEncoding == 10) {
                i += 8;
            }
            int i2 = (i / 8) + (i % 8 > 0 ? 1 : 0);
            int i3 = (i2 * 8) - i;
            bitwiseOutputStream.write(8, i2);
            bitwiseOutputStream.write(5, bearerData.userData.msgEncoding);
            if (bearerData.userData.msgEncoding == 1 || bearerData.userData.msgEncoding == 10) {
                bitwiseOutputStream.write(8, bearerData.userData.msgType);
            }
            bitwiseOutputStream.write(8, bearerData.userData.numFields);
            bitwiseOutputStream.writeByteArray(length, bearerData.userData.payload);
            if (i3 > 0) {
                bitwiseOutputStream.write(i3, 0);
            }
            return bitwiseOutputStream.toByteArray();
        } catch (BearerData.CodingException e) {
            Rlog.e(LOG_TAG, "UserData encode failed: " + e);
            return null;
        } catch (BitwiseOutputStream.AccessException e2) {
            Rlog.e(LOG_TAG, "UserData encode failed: " + e2);
            return null;
        }
    }

    private static void encode7bitAsciiEms(UserData userData, byte[] bArr, boolean z) throws BearerData.CodingException {
        int length = (((bArr.length + 1) * 8) + 6) / 7;
        int length2 = userData.payloadStr.length();
        try {
            int i = length + length2;
            BitwiseOutputStream bitwiseOutputStream = new BitwiseOutputStream(i * 7);
            bitwiseOutputStream.skip(length * 7);
            for (int i2 = 0; i2 < length2; i2++) {
                int i3 = UserData.charToAscii.get(userData.payloadStr.charAt(i2), -1);
                if (i3 == -1) {
                    if (z) {
                        bitwiseOutputStream.write(7, 32);
                    } else {
                        throw new BearerData.CodingException("cannot ASCII encode (" + userData.payloadStr.charAt(i2) + ")");
                    }
                } else {
                    bitwiseOutputStream.write(7, i3);
                }
            }
            userData.payload = bitwiseOutputStream.toByteArray();
            userData.msgEncoding = 2;
            userData.msgEncodingSet = true;
            userData.numFields = i;
            userData.payload[0] = (byte) bArr.length;
            System.arraycopy(bArr, 0, userData.payload, 1, bArr.length);
            Rlog.d(LOG_TAG, "encode7bitAsciiEms");
        } catch (BitwiseOutputStream.AccessException e) {
            throw new BearerData.CodingException("7bit ASCII encode failed: " + e);
        }
    }

    public static void decodeCmasUserData(BearerData bearerData, int i) throws BitwiseInputStream.AccessException, BearerData.CodingException {
        BitwiseInputStream bitwiseInputStream = new BitwiseInputStream(bearerData.userData.payload);
        int i2 = bitwiseInputStream.read(8);
        if (i2 != 0) {
            throw new BearerData.CodingException("unsupported CMAE_protocol_version " + i2);
        }
        long j = 0;
        while (bitwiseInputStream.available() >= 16) {
            int i3 = bitwiseInputStream.read(8);
            int i4 = bitwiseInputStream.read(8);
            if (i3 == 2) {
                bitwiseInputStream.read(8);
                bitwiseInputStream.read(8);
                bitwiseInputStream.read(8);
                long cmasExpireTime = getCmasExpireTime(bitwiseInputStream.readByteArray(48));
                bitwiseInputStream.read(8);
                j = cmasExpireTime;
            } else {
                Rlog.w(LOG_TAG, "skipping CMAS record type " + i3);
                bitwiseInputStream.skip(i4 * 8);
            }
        }
        bearerData.cmasWarningInfo = new MtkSmsCbCmasInfo(bearerData.cmasWarningInfo.getMessageClass(), bearerData.cmasWarningInfo.getCategory(), bearerData.cmasWarningInfo.getResponseType(), bearerData.cmasWarningInfo.getSeverity(), bearerData.cmasWarningInfo.getUrgency(), bearerData.cmasWarningInfo.getCertainty(), j);
        Rlog.w(LOG_TAG, "MtkSmsCbCmasInfo " + bearerData.cmasWarningInfo);
    }

    private static BearerData reGetUserData(byte[] bArr, int i) throws BitwiseInputStream.AccessException {
        BitwiseInputStream bitwiseInputStream = new BitwiseInputStream(bArr);
        BearerData bearerData = new BearerData();
        while (bitwiseInputStream.available() > 0) {
            int i2 = bitwiseInputStream.read(8);
            if (i2 == 1) {
                decodeUserData(bearerData, bitwiseInputStream);
            } else {
                decodeReserved(bearerData, bitwiseInputStream, i2);
            }
        }
        return bearerData;
    }

    private static boolean decodeUserData(BearerData bearerData, BitwiseInputStream bitwiseInputStream) throws BitwiseInputStream.AccessException {
        int i = bitwiseInputStream.read(8) * 8;
        bearerData.userData = new UserData();
        int i2 = 5;
        bearerData.userData.msgEncoding = bitwiseInputStream.read(5);
        bearerData.userData.msgEncodingSet = true;
        bearerData.userData.msgType = 0;
        if (bearerData.userData.msgEncoding == 1 || bearerData.userData.msgEncoding == 10) {
            bearerData.userData.msgType = bitwiseInputStream.read(8);
            i2 = 13;
        }
        bearerData.userData.numFields = bitwiseInputStream.read(8);
        bearerData.userData.payload = bitwiseInputStream.readByteArray(i - (i2 + 8));
        return true;
    }

    private static boolean decodeReserved(BearerData bearerData, BitwiseInputStream bitwiseInputStream, int i) throws BitwiseInputStream.AccessException {
        int i2 = bitwiseInputStream.read(8) * 8;
        if (i2 <= bitwiseInputStream.available()) {
            bitwiseInputStream.skip(i2);
            return true;
        }
        return false;
    }

    private static long getCmasExpireTime(byte[] bArr) {
        Time time = new Time("UTC");
        byte b = bArr[0];
        if (b > 99 || b < 0) {
            return 0L;
        }
        time.year = b >= 96 ? b + 1900 : b + 2000;
        byte b2 = bArr[1];
        if (b2 < 1 || b2 > 12) {
            return 0L;
        }
        time.month = b2 - 1;
        byte b3 = bArr[2];
        if (b3 < 1 || b3 > 31) {
            return 0L;
        }
        time.monthDay = b3;
        byte b4 = bArr[3];
        if (b4 < 0 || b4 > 23) {
            return 0L;
        }
        time.hour = b4;
        byte b5 = bArr[4];
        if (b5 < 0 || b5 > 59) {
            return 0L;
        }
        time.minute = b5;
        byte b6 = bArr[5];
        if (b6 < 0 || b6 > 59) {
            return 0L;
        }
        time.second = b6;
        return time.toMillis(true);
    }
}
