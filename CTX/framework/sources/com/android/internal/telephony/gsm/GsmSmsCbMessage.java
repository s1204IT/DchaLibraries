package com.android.internal.telephony.gsm;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.util.Pair;
import com.android.internal.R;
import com.android.internal.telephony.GsmAlphabet;
import java.io.UnsupportedEncodingException;

public class GsmSmsCbMessage {
    private static final char CARRIAGE_RETURN = '\r';
    private static final String[] LANGUAGE_CODES_GROUP_0 = {"de", "en", "it", "fr", "es", "nl", "sv", "da", "pt", "fi", "no", "el", "tr", "hu", "pl", null};
    private static final String[] LANGUAGE_CODES_GROUP_2 = {"cs", "he", "ar", "ru", "is", null, null, null, null, null, null, null, null, null, null, null};
    private static final int PDU_BODY_PAGE_LENGTH = 82;

    private GsmSmsCbMessage() {
    }

    private static String getEtwsPrimaryMessage(Context context, int i) {
        Resources resources = context.getResources();
        switch (i) {
            case 0:
                return resources.getString(R.string.etws_primary_default_message_earthquake);
            case 1:
                return resources.getString(R.string.etws_primary_default_message_tsunami);
            case 2:
                return resources.getString(R.string.etws_primary_default_message_earthquake_and_tsunami);
            case 3:
                return resources.getString(R.string.etws_primary_default_message_test);
            case 4:
                return resources.getString(R.string.etws_primary_default_message_others);
            default:
                return "";
        }
    }

    public static SmsCbMessage createSmsCbMessage(Context context, SmsCbHeader smsCbHeader, SmsCbLocation smsCbLocation, byte[][] bArr) throws IllegalArgumentException {
        if (smsCbHeader.isEtwsPrimaryNotification()) {
            return new SmsCbMessage(1, smsCbHeader.getGeographicalScope(), smsCbHeader.getSerialNumber(), smsCbLocation, smsCbHeader.getServiceCategory(), null, getEtwsPrimaryMessage(context, smsCbHeader.getEtwsInfo().getWarningType()), 3, smsCbHeader.getEtwsInfo(), smsCbHeader.getCmasInfo());
        }
        StringBuilder sb = new StringBuilder();
        String str = null;
        for (byte[] bArr2 : bArr) {
            Pair<String, String> body = parseBody(smsCbHeader, bArr2);
            str = body.first;
            sb.append(body.second);
        }
        return new SmsCbMessage(1, smsCbHeader.getGeographicalScope(), smsCbHeader.getSerialNumber(), smsCbLocation, smsCbHeader.getServiceCategory(), str, sb.toString(), smsCbHeader.isEmergencyMessage() ? 3 : 0, smsCbHeader.getEtwsInfo(), smsCbHeader.getCmasInfo());
    }

    private static Pair<String, String> parseBody(SmsCbHeader smsCbHeader, byte[] bArr) {
        boolean z;
        String str;
        int dataCodingScheme = smsCbHeader.getDataCodingScheme();
        int i = (dataCodingScheme & 240) >> 4;
        if (i != 9) {
            int i2 = 1;
            String str2 = null;
            switch (i) {
                case 0:
                    str2 = LANGUAGE_CODES_GROUP_0[dataCodingScheme & 15];
                    z = false;
                    str = str2;
                    if (!smsCbHeader.isUmtsFormat()) {
                        return unpackBody(bArr, i2, 6, bArr.length - 6, z, str);
                    }
                    byte b = bArr[6];
                    if (bArr.length < (83 * b) + 7) {
                        throw new IllegalArgumentException("Pdu length " + bArr.length + " does not match " + ((int) b) + " pages");
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i3 = 0; i3 < b; i3++) {
                        int i4 = 7 + (83 * i3);
                        byte b2 = bArr[i4 + 82];
                        if (b2 > 82) {
                            throw new IllegalArgumentException("Page length " + ((int) b2) + " exceeds maximum value 82");
                        }
                        Pair<String, String> pairUnpackBody = unpackBody(bArr, i2, i4, b2, z, str);
                        str = pairUnpackBody.first;
                        sb.append(pairUnpackBody.second);
                    }
                    return new Pair<>(str, sb.toString());
                case 1:
                    if ((dataCodingScheme & 15) == 1) {
                        z = true;
                        str = null;
                        i2 = 3;
                        if (!smsCbHeader.isUmtsFormat()) {
                        }
                    } else {
                        z = true;
                        str = str2;
                        if (!smsCbHeader.isUmtsFormat()) {
                        }
                    }
                    break;
                case 2:
                    str2 = LANGUAGE_CODES_GROUP_2[dataCodingScheme & 15];
                    z = false;
                    str = str2;
                    if (!smsCbHeader.isUmtsFormat()) {
                    }
                    break;
                case 3:
                    z = false;
                    str = str2;
                    if (!smsCbHeader.isUmtsFormat()) {
                    }
                    break;
                case 4:
                case 5:
                    switch ((dataCodingScheme & 12) >> 2) {
                        case 1:
                            z = false;
                            i2 = 2;
                            break;
                        case 2:
                            i2 = 3;
                    }
                    str = str2;
                    if (!smsCbHeader.isUmtsFormat()) {
                    }
                    break;
                case 6:
                case 7:
                    break;
                default:
                    switch (i) {
                        case 15:
                            if (((dataCodingScheme & 4) >> 2) == 1) {
                            }
                            str = str2;
                            if (!smsCbHeader.isUmtsFormat()) {
                            }
                    }
                    break;
            }
        }
        throw new IllegalArgumentException("Unsupported GSM dataCodingScheme " + dataCodingScheme);
    }

    private static Pair<String, String> unpackBody(byte[] bArr, int i, int i2, int i3, boolean z, String str) {
        String strGsm7BitPackedToString;
        int i4;
        if (i == 1) {
            strGsm7BitPackedToString = GsmAlphabet.gsm7BitPackedToString(bArr, i2, (i3 * 8) / 7);
            if (z && strGsm7BitPackedToString != null && strGsm7BitPackedToString.length() > 2) {
                str = strGsm7BitPackedToString.substring(0, 2);
                strGsm7BitPackedToString = strGsm7BitPackedToString.substring(3);
            }
        } else if (i == 3) {
            if (z && bArr.length >= (i4 = i2 + 2)) {
                i3 -= 2;
                str = GsmAlphabet.gsm7BitPackedToString(bArr, i2, 2);
                i2 = i4;
            }
            try {
                strGsm7BitPackedToString = new String(bArr, i2, i3 & 65534, "utf-16");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Error decoding UTF-16 message", e);
            }
        } else {
            strGsm7BitPackedToString = null;
        }
        if (strGsm7BitPackedToString != null) {
            int length = strGsm7BitPackedToString.length() - 1;
            while (true) {
                if (length < 0) {
                    break;
                }
                if (strGsm7BitPackedToString.charAt(length) != '\r') {
                    strGsm7BitPackedToString = strGsm7BitPackedToString.substring(0, length + 1);
                    break;
                }
                length--;
            }
        } else {
            strGsm7BitPackedToString = "";
        }
        return new Pair<>(str, strGsm7BitPackedToString);
    }
}
