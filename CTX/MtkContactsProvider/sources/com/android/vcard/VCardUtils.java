package com.android.vcard;

import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
}
