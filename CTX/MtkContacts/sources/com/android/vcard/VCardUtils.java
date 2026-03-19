package com.android.vcard;

import android.os.Build;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VCardUtils {
    static final boolean ENGDEBUG = TextUtils.equals(Build.TYPE, "eng");
    public static final boolean FORCE_DEBUG;
    private static final int[] sEscapeIndicatorsV30;
    private static final int[] sEscapeIndicatorsV40;
    private static final Map<String, Integer> sKnownCombinationTypeMap;
    private static final Map<Integer, String> sKnownImPropNameMap_ItoS;
    private static final Map<String, Integer> sKnownPhoneTypeMap_StoI;
    private static final Map<Integer, String> sKnownPhoneTypesMap_ItoS;
    private static final Set<String> sMobilePhoneLabelSet;
    private static final Set<String> sPhoneTypesUnknownToContactsSet;
    private static final Set<Character> sUnAcceptableAsciiInV21WordSet;

    static {
        FORCE_DEBUG = SystemProperties.getInt("persist.vendor.log.tel_dbg", 0) == 1;
        sKnownPhoneTypesMap_ItoS = new HashMap();
        sKnownPhoneTypeMap_StoI = new HashMap();
        sKnownPhoneTypesMap_ItoS.put(9, "CAR");
        sKnownPhoneTypeMap_StoI.put("CAR", 9);
        sKnownPhoneTypesMap_ItoS.put(6, "PAGER");
        sKnownPhoneTypeMap_StoI.put("PAGER", 6);
        sKnownPhoneTypesMap_ItoS.put(11, "ISDN");
        sKnownPhoneTypeMap_StoI.put("ISDN", 11);
        sKnownPhoneTypeMap_StoI.put("HOME", 1);
        sKnownPhoneTypeMap_StoI.put("WORK", 3);
        sKnownPhoneTypeMap_StoI.put("CELL", 2);
        sKnownPhoneTypeMap_StoI.put("OTHER", 7);
        sKnownPhoneTypeMap_StoI.put("CALLBACK", 8);
        sKnownPhoneTypeMap_StoI.put("COMPANY-MAIN", 10);
        sKnownPhoneTypeMap_StoI.put("RADIO", 14);
        sKnownPhoneTypeMap_StoI.put("TTY-TDD", 16);
        sKnownPhoneTypeMap_StoI.put("ASSISTANT", 19);
        sKnownPhoneTypeMap_StoI.put("OTHER-FAX", 13);
        sKnownPhoneTypeMap_StoI.put("TLX", 15);
        sKnownPhoneTypeMap_StoI.put("MSG", 20);
        sKnownPhoneTypeMap_StoI.put("VOICE", 7);
        sPhoneTypesUnknownToContactsSet = new HashSet();
        sPhoneTypesUnknownToContactsSet.add("MODEM");
        sPhoneTypesUnknownToContactsSet.add("MSG");
        sPhoneTypesUnknownToContactsSet.add("BBS");
        sPhoneTypesUnknownToContactsSet.add("VIDEO");
        sKnownImPropNameMap_ItoS = new HashMap();
        sKnownImPropNameMap_ItoS.put(0, "X-AIM");
        sKnownImPropNameMap_ItoS.put(1, "X-MSN");
        sKnownImPropNameMap_ItoS.put(2, "X-YAHOO");
        sKnownImPropNameMap_ItoS.put(3, "X-SKYPE-USERNAME");
        sKnownImPropNameMap_ItoS.put(5, "X-GOOGLE-TALK");
        sKnownImPropNameMap_ItoS.put(6, "X-ICQ");
        sKnownImPropNameMap_ItoS.put(7, "X-JABBER");
        sKnownImPropNameMap_ItoS.put(4, "X-QQ");
        sKnownImPropNameMap_ItoS.put(8, "X-NETMEETING");
        sKnownImPropNameMap_ItoS.put(-1, "X-CUSTOM-IM");
        sMobilePhoneLabelSet = new HashSet(Arrays.asList("MOBILE", "携帯電話", "携帯", "ケイタイ", "ｹｲﾀｲ"));
        sKnownCombinationTypeMap = new HashMap();
        sKnownCombinationTypeMap.put("WORK;PAGER", 18);
        sKnownCombinationTypeMap.put("WORK;CELL", 17);
        sUnAcceptableAsciiInV21WordSet = new HashSet(Arrays.asList('[', ']', '=', ':', '.', ',', ' '));
        sEscapeIndicatorsV30 = new int[]{58, 59, 44, 32};
        sEscapeIndicatorsV40 = new int[]{59, 58};
    }

    private static class DecoderException extends Exception {
        public DecoderException(String str) {
            super(str);
        }
    }

    private static class QuotedPrintableCodecPort {
        private static byte ESCAPE_CHAR = 61;

        public static final byte[] decodeQuotedPrintable(byte[] bArr) throws DecoderException {
            if (bArr == null) {
                return null;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i = 0;
            while (i < bArr.length) {
                byte b = bArr[i];
                if (b == ESCAPE_CHAR) {
                    int i2 = i + 1;
                    try {
                        int iDigit = Character.digit((char) bArr[i2], 16);
                        i = i2 + 1;
                        int iDigit2 = Character.digit((char) bArr[i], 16);
                        if (iDigit == -1 || iDigit2 == -1) {
                            throw new DecoderException("Invalid quoted-printable encoding");
                        }
                        byteArrayOutputStream.write((char) ((iDigit << 4) + iDigit2));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new DecoderException("Invalid quoted-printable encoding");
                    }
                } else {
                    byteArrayOutputStream.write(b);
                }
                i++;
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static class PhoneNumberUtilsPort {
        public static String formatNumber(String str, int i) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
            PhoneNumberUtils.formatNumber(spannableStringBuilder, i);
            return spannableStringBuilder.toString();
        }
    }

    public static class TextUtilsPort {
        public static boolean isPrintableAscii(char c) {
            return (' ' <= c && c <= '~') || c == '\r' || c == '\n';
        }

        public static boolean isPrintableAsciiOnly(CharSequence charSequence) {
            int length = charSequence.length();
            for (int i = 0; i < length; i++) {
                if (!isPrintableAscii(charSequence.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static String getPhoneTypeString(Integer num) {
        return sKnownPhoneTypesMap_ItoS.get(num);
    }

    public static Object getPhoneTypeFromStrings(Collection<String> collection, String str) {
        boolean z;
        int i;
        boolean z2;
        if (str == null) {
            str = "";
        }
        int i2 = -1;
        String str2 = null;
        boolean z3 = false;
        if (collection != null) {
            if (collection.size() >= 2) {
                for (String str3 : sKnownCombinationTypeMap.keySet()) {
                    Iterator<String> it = collection.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            if (str3.indexOf(it.next()) < 0) {
                                z2 = false;
                                break;
                            }
                        } else {
                            z2 = true;
                            break;
                        }
                    }
                    if (z2) {
                        return Integer.valueOf(sKnownCombinationTypeMap.get(str3).intValue());
                    }
                }
            }
            Iterator<String> it2 = collection.iterator();
            String str4 = null;
            z = false;
            int iIntValue = -1;
            boolean z4 = false;
            while (it2.hasNext()) {
                String next = it2.next();
                if (next != null) {
                    String upperCase = next.toUpperCase();
                    if (!upperCase.equals("PREF")) {
                        if (!upperCase.equals("FAX")) {
                            if (upperCase.startsWith("X-") && iIntValue < 0) {
                                next = next.substring(2);
                            }
                            if (next.length() != 0) {
                                Integer num = sKnownPhoneTypeMap_StoI.get(next.toUpperCase());
                                if (num != null) {
                                    int iIntValue2 = num.intValue();
                                    int iIndexOf = str.indexOf("@");
                                    if ((iIntValue2 == 6 && iIndexOf > 0 && iIndexOf < str.length() - 1) || iIntValue < 0 || iIntValue == 0 || iIntValue == 7) {
                                        iIntValue = num.intValue();
                                    }
                                } else if (iIntValue < 0) {
                                    iIntValue = 0;
                                    str4 = next;
                                }
                            }
                        } else {
                            z = true;
                        }
                    } else {
                        z4 = true;
                    }
                }
            }
            z3 = z4;
            i2 = iIntValue;
            str2 = str4;
        } else {
            z = false;
        }
        if (i2 >= 0) {
            i = i2;
        } else if (z3) {
            i = 12;
        } else {
            i = 1;
        }
        if (z) {
            if (i == 1) {
                i = 5;
            } else if (i == 3) {
                i = 4;
            } else if (i == 7) {
                i = 13;
            }
        }
        if (i == 0) {
            return str2;
        }
        return Integer.valueOf(i);
    }

    public static boolean isMobilePhoneLabel(String str) {
        return "_AUTO_CELL".equals(str) || sMobilePhoneLabelSet.contains(str);
    }

    public static boolean isValidInV21ButUnknownToContactsPhoteType(String str) {
        return sPhoneTypesUnknownToContactsSet.contains(str);
    }

    public static String getPropertyNameForIm(int i) {
        return sKnownImPropNameMap_ItoS.get(Integer.valueOf(i));
    }

    public static String[] sortNameElements(int i, String str, String str2, String str3) {
        String[] strArr = new String[3];
        int nameOrderType = VCardConfig.getNameOrderType(i);
        if (nameOrderType == 4) {
            strArr[0] = str2;
            strArr[1] = str3;
            strArr[2] = str;
        } else if (nameOrderType == 8) {
            if (containsOnlyPrintableAscii(str) && containsOnlyPrintableAscii(str3)) {
                strArr[0] = str3;
                strArr[1] = str2;
                strArr[2] = str;
            } else {
                strArr[0] = str;
                strArr[1] = str2;
                strArr[2] = str3;
            }
        } else {
            strArr[0] = str3;
            strArr[1] = str2;
            strArr[2] = str;
        }
        return strArr;
    }

    public static int getPhoneNumberFormat(int i) {
        if (VCardConfig.isJapaneseDevice(i)) {
            return 2;
        }
        return 1;
    }

    public static String constructNameFromElements(int i, String str, String str2, String str3) {
        return constructNameFromElements(i, str, str2, str3, null, null);
    }

    public static String constructNameFromElements(int i, String str, String str2, String str3, String str4, String str5) {
        boolean z;
        StringBuilder sb = new StringBuilder();
        String[] strArrSortNameElements = sortNameElements(i, str, str2, str3);
        if (TextUtils.isEmpty(str4)) {
            z = true;
        } else {
            sb.append(str4);
            z = false;
        }
        boolean z2 = z;
        for (String str6 : strArrSortNameElements) {
            if (!TextUtils.isEmpty(str6)) {
                if (!z2) {
                    sb.append(' ');
                } else {
                    z2 = false;
                }
                sb.append(str6);
            }
        }
        if (!TextUtils.isEmpty(str5)) {
            if (!z2) {
                sb.append(' ');
            }
            sb.append(str5);
        }
        return sb.toString();
    }

    public static List<String> constructListFromValue(String str, int i) {
        String strUnescapeCharacter;
        ArrayList arrayList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i2 = 0;
        while (i2 < length) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == '\\' && i2 < length - 1) {
                int i3 = i2 + 1;
                char cCharAt2 = str.charAt(i3);
                if (VCardConfig.isVersion40(i)) {
                    strUnescapeCharacter = VCardParserImpl_V40.unescapeCharacter(cCharAt2);
                } else if (VCardConfig.isVersion30(i)) {
                    strUnescapeCharacter = VCardParserImpl_V30.unescapeCharacter(cCharAt2);
                } else {
                    if (!VCardConfig.isVersion21(i) && ENGDEBUG && FORCE_DEBUG) {
                        Log.w("MTK_vCard", "Unknown vCard type");
                    }
                    strUnescapeCharacter = VCardParserImpl_V21.unescapeCharacter(cCharAt2);
                }
                if (strUnescapeCharacter != null) {
                    sb.append(strUnescapeCharacter);
                    i2 = i3;
                } else {
                    sb.append(cCharAt);
                }
            } else if (cCharAt == ';') {
                arrayList.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(cCharAt);
            }
            i2++;
        }
        arrayList.add(sb.toString());
        return arrayList;
    }

    public static boolean containsOnlyPrintableAscii(String... strArr) {
        if (strArr == null) {
            return true;
        }
        return containsOnlyPrintableAscii(Arrays.asList(strArr));
    }

    public static boolean containsOnlyPrintableAscii(Collection<String> collection) {
        if (collection == null) {
            return true;
        }
        for (String str : collection) {
            if (!TextUtils.isEmpty(str) && !TextUtilsPort.isPrintableAsciiOnly(str)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsOnlyNonCrLfPrintableAscii(String... strArr) {
        if (strArr == null) {
            return true;
        }
        return containsOnlyNonCrLfPrintableAscii(Arrays.asList(strArr));
    }

    public static boolean containsOnlyNonCrLfPrintableAscii(Collection<String> collection) {
        if (collection == null) {
            return true;
        }
        for (String str : collection) {
            if (!TextUtils.isEmpty(str)) {
                int length = str.length();
                for (int iOffsetByCodePoints = 0; iOffsetByCodePoints < length; iOffsetByCodePoints = str.offsetByCodePoints(iOffsetByCodePoints, 1)) {
                    int iCodePointAt = str.codePointAt(iOffsetByCodePoints);
                    if (32 > iCodePointAt || iCodePointAt > 126) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean containsOnlyAlphaDigitHyphen(String... strArr) {
        if (strArr == null) {
            return true;
        }
        return containsOnlyAlphaDigitHyphen(Arrays.asList(strArr));
    }

    public static boolean containsOnlyAlphaDigitHyphen(Collection<String> collection) {
        if (collection == null) {
            return true;
        }
        for (String str : collection) {
            if (!TextUtils.isEmpty(str)) {
                int length = str.length();
                for (int iOffsetByCodePoints = 0; iOffsetByCodePoints < length; iOffsetByCodePoints = str.offsetByCodePoints(iOffsetByCodePoints, 1)) {
                    int iCodePointAt = str.codePointAt(iOffsetByCodePoints);
                    if ((97 > iCodePointAt || iCodePointAt >= 123) && ((65 > iCodePointAt || iCodePointAt >= 91) && ((48 > iCodePointAt || iCodePointAt >= 58) && iCodePointAt != 45))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean containsOnlyWhiteSpaces(String... strArr) {
        if (strArr == null) {
            return true;
        }
        return containsOnlyWhiteSpaces(Arrays.asList(strArr));
    }

    public static boolean containsOnlyWhiteSpaces(Collection<String> collection) {
        if (collection == null) {
            return true;
        }
        for (String str : collection) {
            if (!TextUtils.isEmpty(str)) {
                int length = str.length();
                for (int iOffsetByCodePoints = 0; iOffsetByCodePoints < length; iOffsetByCodePoints = str.offsetByCodePoints(iOffsetByCodePoints, 1)) {
                    if (!Character.isWhitespace(str.codePointAt(iOffsetByCodePoints))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isV21Word(String str) {
        if (TextUtils.isEmpty(str)) {
            return true;
        }
        int length = str.length();
        int iOffsetByCodePoints = 0;
        while (iOffsetByCodePoints < length) {
            int iCodePointAt = str.codePointAt(iOffsetByCodePoints);
            if (32 > iCodePointAt || iCodePointAt > 126 || sUnAcceptableAsciiInV21WordSet.contains(Character.valueOf((char) iCodePointAt))) {
                return false;
            }
            iOffsetByCodePoints = str.offsetByCodePoints(iOffsetByCodePoints, 1);
        }
        return true;
    }

    public static String toStringAsV30ParamValue(String str) {
        return toStringAsParamValue(str, sEscapeIndicatorsV30);
    }

    public static String toStringAsV40ParamValue(String str) {
        return toStringAsParamValue(str, sEscapeIndicatorsV40);
    }

    private static String toStringAsParamValue(String str, int[] iArr) {
        if (TextUtils.isEmpty(str)) {
            str = "";
        }
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        boolean z = false;
        for (int iOffsetByCodePoints = 0; iOffsetByCodePoints < length; iOffsetByCodePoints = str.offsetByCodePoints(iOffsetByCodePoints, 1)) {
            int iCodePointAt = str.codePointAt(iOffsetByCodePoints);
            if (iCodePointAt >= 32 && iCodePointAt != 34) {
                sb.appendCodePoint(iCodePointAt);
                int length2 = iArr.length;
                int i = 0;
                while (true) {
                    if (i >= length2) {
                        break;
                    }
                    if (iCodePointAt == iArr[i]) {
                        z = true;
                        break;
                    }
                    i++;
                }
            }
        }
        String string = sb.toString();
        if (string.isEmpty() || containsOnlyWhiteSpaces(string)) {
            return "";
        }
        if (!z) {
            return string;
        }
        return '\"' + string + '\"';
    }

    public static String toHalfWidthString(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int iOffsetByCodePoints = 0;
        while (iOffsetByCodePoints < length) {
            char cCharAt = str.charAt(iOffsetByCodePoints);
            String strTryGetHalfWidthText = JapaneseUtils.tryGetHalfWidthText(cCharAt);
            if (strTryGetHalfWidthText != null) {
                sb.append(strTryGetHalfWidthText);
            } else {
                sb.append(cCharAt);
            }
            iOffsetByCodePoints = str.offsetByCodePoints(iOffsetByCodePoints, 1);
        }
        return sb.toString();
    }

    public static String guessImageType(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        if (bArr.length >= 3 && bArr[0] == 71 && bArr[1] == 73 && bArr[2] == 70) {
            return "GIF";
        }
        if (bArr.length >= 4 && bArr[0] == -119 && bArr[1] == 80 && bArr[2] == 78 && bArr[3] == 71) {
            return "PNG";
        }
        if (bArr.length < 2 || bArr[0] != -1 || bArr[1] != -40) {
            return null;
        }
        return "JPEG";
    }

    public static boolean areAllEmpty(String... strArr) {
        if (strArr == null) {
            return true;
        }
        for (String str : strArr) {
            if (!TextUtils.isEmpty(str)) {
                return false;
            }
        }
        return true;
    }

    public static boolean appearsLikeAndroidVCardQuotedPrintable(String str) {
        int length = str.length() % 3;
        if (str.length() < 2 || !(length == 1 || length == 0)) {
            return false;
        }
        for (int i = 0; i < str.length(); i += 3) {
            if (str.charAt(i) != '=') {
                return false;
            }
        }
        return true;
    }

    public static String parseQuotedPrintable(String str, boolean z, String str2, String str3) {
        String[] strArrSplit;
        byte[] bytes;
        byte[] bArrDecodeQuotedPrintable;
        int i;
        char cCharAt;
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i2 = 0;
        while (i2 < length) {
            char cCharAt2 = str.charAt(i2);
            if (cCharAt2 == '=' && i2 < length - 1 && ((cCharAt = str.charAt((i = i2 + 1))) == ' ' || cCharAt == '\t')) {
                sb.append(cCharAt);
                i2 = i;
            } else {
                sb.append(cCharAt2);
            }
            i2++;
        }
        String string = sb.toString();
        if (z) {
            strArrSplit = string.split("\r\n");
        } else {
            StringBuilder sb2 = new StringBuilder();
            int length2 = string.length();
            ArrayList arrayList = new ArrayList();
            StringBuilder sb3 = sb2;
            int i3 = 0;
            while (i3 < length2) {
                char cCharAt3 = string.charAt(i3);
                if (cCharAt3 == '\n') {
                    arrayList.add(sb3.toString());
                    sb3 = new StringBuilder();
                } else if (cCharAt3 == '\r') {
                    arrayList.add(sb3.toString());
                    sb3 = new StringBuilder();
                    if (i3 < length2 - 1) {
                        int i4 = i3 + 1;
                        if (string.charAt(i4) == '\n') {
                            i3 = i4;
                        }
                    }
                } else {
                    sb3.append(cCharAt3);
                }
                i3++;
            }
            String string2 = sb3.toString();
            if (string2.length() > 0) {
                arrayList.add(string2);
            }
            strArrSplit = (String[]) arrayList.toArray(new String[0]);
        }
        StringBuilder sb4 = new StringBuilder();
        for (String strSubstring : strArrSplit) {
            if (strSubstring.endsWith("=")) {
                strSubstring = strSubstring.substring(0, strSubstring.length() - 1);
            }
            sb4.append(strSubstring);
        }
        String string3 = sb4.toString();
        if (TextUtils.isEmpty(string3) && ENGDEBUG && FORCE_DEBUG) {
            Log.w("MTK_vCard", "Given raw string is empty.");
        }
        try {
            bytes = string3.getBytes(str2);
        } catch (UnsupportedEncodingException e) {
            Log.w("MTK_vCard", "Failed to decode: " + str2);
            bytes = string3.getBytes();
        }
        try {
            bArrDecodeQuotedPrintable = QuotedPrintableCodecPort.decodeQuotedPrintable(bytes);
        } catch (DecoderException e2) {
            Log.e("MTK_vCard", "DecoderException is thrown.");
            bArrDecodeQuotedPrintable = bytes;
        }
        try {
            return new String(bArrDecodeQuotedPrintable, str3);
        } catch (UnsupportedEncodingException e3) {
            Log.e("MTK_vCard", "Failed to encode: charset=" + str3);
            return new String(bArrDecodeQuotedPrintable);
        }
    }

    public static final String convertStringCharset(String str, String str2, String str3) {
        if (str2.equalsIgnoreCase(str3)) {
            return str;
        }
        ByteBuffer byteBufferEncode = Charset.forName(str2).encode(str);
        byte[] bArr = new byte[byteBufferEncode.remaining()];
        byteBufferEncode.get(bArr);
        try {
            return new String(bArr, str3);
        } catch (UnsupportedEncodingException e) {
            Log.e("MTK_vCard", "Failed to encode: charset=" + str3);
            return null;
        }
    }
}
