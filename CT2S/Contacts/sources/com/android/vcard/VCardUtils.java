package com.android.vcard;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VCardUtils {
    private static final int[] sEscapeIndicatorsV30;
    private static final int[] sEscapeIndicatorsV40;
    private static final Map<Integer, String> sKnownImPropNameMap_ItoS;
    private static final Set<String> sMobilePhoneLabelSet;
    private static final Set<String> sPhoneTypesUnknownToContactsSet;
    private static final Set<Character> sUnAcceptableAsciiInV21WordSet;
    private static final Map<Integer, String> sKnownPhoneTypesMap_ItoS = new HashMap();
    private static final Map<String, Integer> sKnownPhoneTypeMap_StoI = new HashMap();

    private static class DecoderException extends Exception {
        public DecoderException(String pMessage) {
            super(pMessage);
        }
    }

    private static class QuotedPrintableCodecPort {
        private static byte ESCAPE_CHAR = 61;

        public static final byte[] decodeQuotedPrintable(byte[] bytes) throws DecoderException {
            if (bytes == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int i = 0;
            while (i < bytes.length) {
                int b = bytes[i];
                if (b == ESCAPE_CHAR) {
                    int i2 = i + 1;
                    try {
                        int u = Character.digit((char) bytes[i2], 16);
                        i = i2 + 1;
                        int l = Character.digit((char) bytes[i], 16);
                        if (u == -1 || l == -1) {
                            throw new DecoderException("Invalid quoted-printable encoding");
                        }
                        buffer.write((char) ((u << 4) + l));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new DecoderException("Invalid quoted-printable encoding");
                    }
                } else {
                    buffer.write(b);
                }
                i++;
            }
            return buffer.toByteArray();
        }
    }

    public static class PhoneNumberUtilsPort {
        public static String formatNumber(String source, int defaultFormattingType) {
            SpannableStringBuilder text = new SpannableStringBuilder(source);
            PhoneNumberUtils.formatNumber(text, defaultFormattingType);
            return text.toString();
        }
    }

    public static class TextUtilsPort {
        public static boolean isPrintableAscii(char c) {
            return (' ' <= c && c <= '~') || c == '\r' || c == '\n';
        }

        public static boolean isPrintableAsciiOnly(CharSequence str) {
            int len = str.length();
            for (int i = 0; i < len; i++) {
                if (!isPrintableAscii(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    static {
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
        sMobilePhoneLabelSet = new HashSet(Arrays.asList("MOBILE", "携帯電話", "携帯", "ケイタイ", "ｹｲﾀｲ"));
        sUnAcceptableAsciiInV21WordSet = new HashSet(Arrays.asList('[', ']', '=', ':', '.', ',', ' '));
        sEscapeIndicatorsV30 = new int[]{58, 59, 44, 32};
        sEscapeIndicatorsV40 = new int[]{59, 58};
    }

    public static String getPhoneTypeString(Integer type) {
        return sKnownPhoneTypesMap_ItoS.get(type);
    }

    public static Object getPhoneTypeFromStrings(Collection<String> types, String number) {
        String labelCandidate;
        if (number == null) {
            number = "";
        }
        int type = -1;
        String label = null;
        boolean isFax = false;
        boolean hasPref = false;
        if (types != null) {
            for (String typeStringOrg : types) {
                if (typeStringOrg != null) {
                    String typeStringUpperCase = typeStringOrg.toUpperCase();
                    if (typeStringUpperCase.equals("PREF")) {
                        hasPref = true;
                    } else if (typeStringUpperCase.equals("FAX")) {
                        isFax = true;
                    } else {
                        if (typeStringUpperCase.startsWith("X-") && type < 0) {
                            labelCandidate = typeStringOrg.substring(2);
                        } else {
                            labelCandidate = typeStringOrg;
                        }
                        if (labelCandidate.length() != 0) {
                            Integer tmp = sKnownPhoneTypeMap_StoI.get(labelCandidate.toUpperCase());
                            if (tmp != null) {
                                int typeCandidate = tmp.intValue();
                                int indexOfAt = number.indexOf("@");
                                if ((typeCandidate == 6 && indexOfAt > 0 && indexOfAt < number.length() - 1) || type < 0 || type == 0 || type == 7) {
                                    type = tmp.intValue();
                                }
                            } else if (type < 0) {
                                type = 0;
                                label = labelCandidate;
                            }
                        }
                    }
                }
            }
        }
        if (type < 0) {
            if (hasPref) {
                type = 12;
            } else {
                type = 1;
            }
        }
        if (isFax) {
            if (type == 1) {
                type = 5;
            } else if (type == 3) {
                type = 4;
            } else if (type == 7) {
                type = 13;
            }
        }
        return type == 0 ? label : Integer.valueOf(type);
    }

    public static boolean isMobilePhoneLabel(String label) {
        return "_AUTO_CELL".equals(label) || sMobilePhoneLabelSet.contains(label);
    }

    public static boolean isValidInV21ButUnknownToContactsPhoteType(String label) {
        return sPhoneTypesUnknownToContactsSet.contains(label);
    }

    public static String getPropertyNameForIm(int protocol) {
        return sKnownImPropNameMap_ItoS.get(Integer.valueOf(protocol));
    }

    public static String[] sortNameElements(int nameOrder, String familyName, String middleName, String givenName) {
        String[] list = new String[3];
        int nameOrderType = VCardConfig.getNameOrderType(nameOrder);
        switch (nameOrderType) {
            case 4:
                list[0] = middleName;
                list[1] = givenName;
                list[2] = familyName;
                return list;
            case 8:
                if (containsOnlyPrintableAscii(familyName) && containsOnlyPrintableAscii(givenName)) {
                    list[0] = givenName;
                    list[1] = middleName;
                    list[2] = familyName;
                } else {
                    list[0] = familyName;
                    list[1] = middleName;
                    list[2] = givenName;
                }
                return list;
            default:
                list[0] = givenName;
                list[1] = middleName;
                list[2] = familyName;
                return list;
        }
    }

    public static int getPhoneNumberFormat(int vcardType) {
        return VCardConfig.isJapaneseDevice(vcardType) ? 2 : 1;
    }

    public static String constructNameFromElements(int nameOrder, String familyName, String middleName, String givenName) {
        return constructNameFromElements(nameOrder, familyName, middleName, givenName, null, null);
    }

    public static String constructNameFromElements(int nameOrder, String familyName, String middleName, String givenName, String prefix, String suffix) {
        StringBuilder builder = new StringBuilder();
        String[] nameList = sortNameElements(nameOrder, familyName, middleName, givenName);
        boolean first = true;
        if (!TextUtils.isEmpty(prefix)) {
            first = false;
            builder.append(prefix);
        }
        for (String namePart : nameList) {
            if (!TextUtils.isEmpty(namePart)) {
                if (first) {
                    first = false;
                } else {
                    builder.append(' ');
                }
                builder.append(namePart);
            }
        }
        if (!TextUtils.isEmpty(suffix)) {
            if (!first) {
                builder.append(' ');
            }
            builder.append(suffix);
        }
        return builder.toString();
    }

    public static List<String> constructListFromValue(String value, int vcardType) {
        String unescapedString;
        List<String> list = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        int i = 0;
        while (i < length) {
            char ch = value.charAt(i);
            if (ch == '\\' && i < length - 1) {
                char nextCh = value.charAt(i + 1);
                if (VCardConfig.isVersion40(vcardType)) {
                    unescapedString = VCardParserImpl_V40.unescapeCharacter(nextCh);
                } else if (VCardConfig.isVersion30(vcardType)) {
                    unescapedString = VCardParserImpl_V30.unescapeCharacter(nextCh);
                } else {
                    if (!VCardConfig.isVersion21(vcardType)) {
                        Log.w("vCard", "Unknown vCard type");
                    }
                    unescapedString = VCardParserImpl_V21.unescapeCharacter(nextCh);
                }
                if (unescapedString != null) {
                    builder.append(unescapedString);
                    i++;
                } else {
                    builder.append(ch);
                }
            } else if (ch == ';') {
                list.add(builder.toString());
                builder = new StringBuilder();
            } else {
                builder.append(ch);
            }
            i++;
        }
        list.add(builder.toString());
        return list;
    }

    public static boolean containsOnlyPrintableAscii(String... values) {
        if (values == null) {
            return true;
        }
        return containsOnlyPrintableAscii(Arrays.asList(values));
    }

    public static boolean containsOnlyPrintableAscii(Collection<String> values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !TextUtilsPort.isPrintableAsciiOnly(value)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsOnlyNonCrLfPrintableAscii(String... values) {
        if (values == null) {
            return true;
        }
        return containsOnlyNonCrLfPrintableAscii(Arrays.asList(values));
    }

    public static boolean containsOnlyNonCrLfPrintableAscii(Collection<String> values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                int length = value.length();
                for (int i = 0; i < length; i = value.offsetByCodePoints(i, 1)) {
                    int c = value.codePointAt(i);
                    if (32 > c || c > 126) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean containsOnlyAlphaDigitHyphen(String... values) {
        if (values == null) {
            return true;
        }
        return containsOnlyAlphaDigitHyphen(Arrays.asList(values));
    }

    public static boolean containsOnlyAlphaDigitHyphen(Collection<String> values) {
        if (values == null) {
            return true;
        }
        for (String str : values) {
            if (!TextUtils.isEmpty(str)) {
                int length = str.length();
                for (int i = 0; i < length; i = str.offsetByCodePoints(i, 1)) {
                    int codepoint = str.codePointAt(i);
                    if ((97 > codepoint || codepoint >= 123) && ((65 > codepoint || codepoint >= 91) && ((48 > codepoint || codepoint >= 58) && codepoint != 45))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean containsOnlyWhiteSpaces(String... values) {
        if (values == null) {
            return true;
        }
        return containsOnlyWhiteSpaces(Arrays.asList(values));
    }

    public static boolean containsOnlyWhiteSpaces(Collection<String> values) {
        if (values == null) {
            return true;
        }
        for (String str : values) {
            if (!TextUtils.isEmpty(str)) {
                int length = str.length();
                for (int i = 0; i < length; i = str.offsetByCodePoints(i, 1)) {
                    if (!Character.isWhitespace(str.codePointAt(i))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isV21Word(String value) {
        if (TextUtils.isEmpty(value)) {
            return true;
        }
        int length = value.length();
        int i = 0;
        while (i < length) {
            int c = value.codePointAt(i);
            if (32 <= c && c <= 126 && !sUnAcceptableAsciiInV21WordSet.contains(Character.valueOf((char) c))) {
                i = value.offsetByCodePoints(i, 1);
            } else {
                return false;
            }
        }
        return true;
    }

    public static String toStringAsV30ParamValue(String value) {
        return toStringAsParamValue(value, sEscapeIndicatorsV30);
    }

    public static String toStringAsV40ParamValue(String value) {
        return toStringAsParamValue(value, sEscapeIndicatorsV40);
    }

    private static String toStringAsParamValue(String value, int[] escapeIndicators) {
        if (TextUtils.isEmpty(value)) {
            value = "";
        }
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        boolean needQuote = false;
        for (int i = 0; i < length; i = value.offsetByCodePoints(i, 1)) {
            int codePoint = value.codePointAt(i);
            if (codePoint >= 32 && codePoint != 34) {
                builder.appendCodePoint(codePoint);
                int len$ = escapeIndicators.length;
                int i$ = 0;
                while (true) {
                    if (i$ < len$) {
                        int indicator = escapeIndicators[i$];
                        if (codePoint != indicator) {
                            i$++;
                        } else {
                            needQuote = true;
                            break;
                        }
                    }
                }
            }
        }
        String result = builder.toString();
        return (result.isEmpty() || containsOnlyWhiteSpaces(result)) ? "" : needQuote ? '\"' + result + '\"' : result;
    }

    public static String toHalfWidthString(String orgString) {
        if (TextUtils.isEmpty(orgString)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        int length = orgString.length();
        int i = 0;
        while (i < length) {
            char ch = orgString.charAt(i);
            String halfWidthText = JapaneseUtils.tryGetHalfWidthText(ch);
            if (halfWidthText != null) {
                builder.append(halfWidthText);
            } else {
                builder.append(ch);
            }
            i = orgString.offsetByCodePoints(i, 1);
        }
        return builder.toString();
    }

    public static String guessImageType(byte[] input) {
        if (input == null) {
            return null;
        }
        if (input.length >= 3 && input[0] == 71 && input[1] == 73 && input[2] == 70) {
            return "GIF";
        }
        if (input.length >= 4 && input[0] == -119 && input[1] == 80 && input[2] == 78 && input[3] == 71) {
            return "PNG";
        }
        if (input.length >= 2 && input[0] == -1 && input[1] == -40) {
            return "JPEG";
        }
        return null;
    }

    public static boolean areAllEmpty(String... values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    public static boolean appearsLikeAndroidVCardQuotedPrintable(String value) {
        int remainder = value.length() % 3;
        if (value.length() < 2) {
            return false;
        }
        if (remainder != 1 && remainder != 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i += 3) {
            if (value.charAt(i) != '=') {
                return false;
            }
        }
        return true;
    }

    public static String parseQuotedPrintable(String value, boolean strictLineBreaking, String sourceCharset, String targetCharset) {
        String[] lines;
        byte[] rawBytes;
        byte[] decodedBytes;
        char nextCh;
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        int i = 0;
        while (i < length) {
            char ch = value.charAt(i);
            if (ch == '=' && i < length - 1 && ((nextCh = value.charAt(i + 1)) == ' ' || nextCh == '\t')) {
                builder.append(nextCh);
                i++;
            } else {
                builder.append(ch);
            }
            i++;
        }
        String quotedPrintable = builder.toString();
        if (strictLineBreaking) {
            lines = quotedPrintable.split("\r\n");
        } else {
            StringBuilder builder2 = new StringBuilder();
            int length2 = quotedPrintable.length();
            ArrayList<String> list = new ArrayList<>();
            int i2 = 0;
            while (i2 < length2) {
                char ch2 = quotedPrintable.charAt(i2);
                if (ch2 == '\n') {
                    list.add(builder2.toString());
                    builder2 = new StringBuilder();
                } else if (ch2 == '\r') {
                    list.add(builder2.toString());
                    builder2 = new StringBuilder();
                    if (i2 < length2 - 1 && quotedPrintable.charAt(i2 + 1) == '\n') {
                        i2++;
                    }
                } else {
                    builder2.append(ch2);
                }
                i2++;
            }
            String lastLine = builder2.toString();
            if (lastLine.length() > 0) {
                list.add(lastLine);
            }
            lines = (String[]) list.toArray(new String[0]);
        }
        StringBuilder builder3 = new StringBuilder();
        String[] arr$ = lines;
        for (String line : arr$) {
            if (line.endsWith("=")) {
                line = line.substring(0, line.length() - 1);
            }
            builder3.append(line);
        }
        String rawString = builder3.toString();
        if (TextUtils.isEmpty(rawString)) {
            Log.w("vCard", "Given raw string is empty.");
        }
        try {
            rawBytes = rawString.getBytes(sourceCharset);
        } catch (UnsupportedEncodingException e) {
            Log.w("vCard", "Failed to decode: " + sourceCharset);
            rawBytes = rawString.getBytes();
        }
        try {
            decodedBytes = QuotedPrintableCodecPort.decodeQuotedPrintable(rawBytes);
        } catch (DecoderException e2) {
            Log.e("vCard", "DecoderException is thrown.");
            decodedBytes = rawBytes;
        }
        try {
            return new String(decodedBytes, targetCharset);
        } catch (UnsupportedEncodingException e3) {
            Log.e("vCard", "Failed to encode: charset=" + targetCharset);
            return new String(decodedBytes);
        }
    }

    public static final String convertStringCharset(String originalString, String sourceCharset, String targetCharset) {
        if (!sourceCharset.equalsIgnoreCase(targetCharset)) {
            Charset charset = Charset.forName(sourceCharset);
            ByteBuffer byteBuffer = charset.encode(originalString);
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            try {
                return new String(bytes, targetCharset);
            } catch (UnsupportedEncodingException e) {
                Log.e("vCard", "Failed to encode: charset=" + targetCharset);
                return null;
            }
        }
        return originalString;
    }
}
