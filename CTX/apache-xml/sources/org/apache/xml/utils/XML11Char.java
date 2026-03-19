package org.apache.xml.utils;

import java.util.Arrays;
import org.apache.xpath.axes.WalkerFactory;

public class XML11Char {
    public static final int MASK_XML11_CONTENT = 32;
    public static final int MASK_XML11_CONTENT_INTERNAL = 48;
    public static final int MASK_XML11_CONTROL = 16;
    public static final int MASK_XML11_NAME = 8;
    public static final int MASK_XML11_NAME_START = 4;
    public static final int MASK_XML11_NCNAME = 128;
    public static final int MASK_XML11_NCNAME_START = 64;
    public static final int MASK_XML11_SPACE = 2;
    public static final int MASK_XML11_VALID = 1;
    private static final byte[] XML11CHARS = new byte[65536];

    static {
        Arrays.fill(XML11CHARS, 1, 9, (byte) 17);
        XML11CHARS[9] = 35;
        XML11CHARS[10] = 3;
        Arrays.fill(XML11CHARS, 11, 13, (byte) 17);
        XML11CHARS[13] = 3;
        Arrays.fill(XML11CHARS, 14, 32, (byte) 17);
        XML11CHARS[32] = 35;
        Arrays.fill(XML11CHARS, 33, 38, (byte) 33);
        XML11CHARS[38] = 1;
        Arrays.fill(XML11CHARS, 39, 45, (byte) 33);
        Arrays.fill(XML11CHARS, 45, 47, (byte) -87);
        XML11CHARS[47] = 33;
        Arrays.fill(XML11CHARS, 48, 58, (byte) -87);
        XML11CHARS[58] = 45;
        XML11CHARS[59] = 33;
        XML11CHARS[60] = 1;
        Arrays.fill(XML11CHARS, 61, 65, (byte) 33);
        Arrays.fill(XML11CHARS, 65, 91, (byte) -19);
        Arrays.fill(XML11CHARS, 91, 93, (byte) 33);
        XML11CHARS[93] = 1;
        XML11CHARS[94] = 33;
        XML11CHARS[95] = -19;
        XML11CHARS[96] = 33;
        Arrays.fill(XML11CHARS, 97, 123, (byte) -19);
        Arrays.fill(XML11CHARS, 123, 127, (byte) 33);
        Arrays.fill(XML11CHARS, 127, 133, (byte) 17);
        XML11CHARS[133] = 35;
        Arrays.fill(XML11CHARS, 134, 160, (byte) 17);
        Arrays.fill(XML11CHARS, 160, 183, (byte) 33);
        XML11CHARS[183] = -87;
        Arrays.fill(XML11CHARS, 184, 192, (byte) 33);
        Arrays.fill(XML11CHARS, 192, 215, (byte) -19);
        XML11CHARS[215] = 33;
        Arrays.fill(XML11CHARS, 216, 247, (byte) -19);
        XML11CHARS[247] = 33;
        Arrays.fill(XML11CHARS, 248, 768, (byte) -19);
        Arrays.fill(XML11CHARS, 768, 880, (byte) -87);
        Arrays.fill(XML11CHARS, 880, 894, (byte) -19);
        XML11CHARS[894] = 33;
        Arrays.fill(XML11CHARS, 895, WalkerFactory.BIT_ANCESTOR, (byte) -19);
        Arrays.fill(XML11CHARS, WalkerFactory.BIT_ANCESTOR, 8204, (byte) 33);
        Arrays.fill(XML11CHARS, 8204, 8206, (byte) -19);
        Arrays.fill(XML11CHARS, 8206, 8232, (byte) 33);
        XML11CHARS[8232] = 35;
        Arrays.fill(XML11CHARS, 8233, 8255, (byte) 33);
        Arrays.fill(XML11CHARS, 8255, 8257, (byte) -87);
        Arrays.fill(XML11CHARS, 8257, 8304, (byte) 33);
        Arrays.fill(XML11CHARS, 8304, 8592, (byte) -19);
        Arrays.fill(XML11CHARS, 8592, 11264, (byte) 33);
        Arrays.fill(XML11CHARS, 11264, 12272, (byte) -19);
        Arrays.fill(XML11CHARS, 12272, 12289, (byte) 33);
        Arrays.fill(XML11CHARS, 12289, 55296, (byte) -19);
        Arrays.fill(XML11CHARS, 57344, 63744, (byte) 33);
        Arrays.fill(XML11CHARS, 63744, 64976, (byte) -19);
        Arrays.fill(XML11CHARS, 64976, 65008, (byte) 33);
        Arrays.fill(XML11CHARS, 65008, 65534, (byte) -19);
    }

    public static boolean isXML11Space(int i) {
        return i < 65536 && (XML11CHARS[i] & 2) != 0;
    }

    public static boolean isXML11Valid(int i) {
        if (i >= 65536 || (XML11CHARS[i] & 1) == 0) {
            return 65536 <= i && i <= 1114111;
        }
        return true;
    }

    public static boolean isXML11Invalid(int i) {
        return !isXML11Valid(i);
    }

    public static boolean isXML11ValidLiteral(int i) {
        if (i >= 65536 || (XML11CHARS[i] & 1) == 0 || (XML11CHARS[i] & 16) != 0) {
            return 65536 <= i && i <= 1114111;
        }
        return true;
    }

    public static boolean isXML11Content(int i) {
        return (i < 65536 && (XML11CHARS[i] & 32) != 0) || (65536 <= i && i <= 1114111);
    }

    public static boolean isXML11InternalEntityContent(int i) {
        return (i < 65536 && (XML11CHARS[i] & 48) != 0) || (65536 <= i && i <= 1114111);
    }

    public static boolean isXML11NameStart(int i) {
        return (i < 65536 && (XML11CHARS[i] & 4) != 0) || (65536 <= i && i < 983040);
    }

    public static boolean isXML11Name(int i) {
        return (i < 65536 && (XML11CHARS[i] & 8) != 0) || (i >= 65536 && i < 983040);
    }

    public static boolean isXML11NCNameStart(int i) {
        return (i < 65536 && (XML11CHARS[i] & 64) != 0) || (65536 <= i && i < 983040);
    }

    public static boolean isXML11NCName(int i) {
        return (i < 65536 && (XML11CHARS[i] & 128) != 0) || (65536 <= i && i < 983040);
    }

    public static boolean isXML11NameHighSurrogate(int i) {
        return 55296 <= i && i <= 56191;
    }

    public static boolean isXML11ValidName(String str) {
        int i;
        int length = str.length();
        if (length == 0) {
            return false;
        }
        char cCharAt = str.charAt(0);
        if (!isXML11NameStart(cCharAt)) {
            if (length <= 1 || !isXML11NameHighSurrogate(cCharAt)) {
                return false;
            }
            char cCharAt2 = str.charAt(1);
            if (!XMLChar.isLowSurrogate(cCharAt2) || !isXML11NameStart(XMLChar.supplemental(cCharAt, cCharAt2))) {
                return false;
            }
            i = 2;
        } else {
            i = 1;
        }
        while (i < length) {
            char cCharAt3 = str.charAt(i);
            if (!isXML11Name(cCharAt3)) {
                i++;
                if (i >= length || !isXML11NameHighSurrogate(cCharAt3)) {
                    return false;
                }
                char cCharAt4 = str.charAt(i);
                if (!XMLChar.isLowSurrogate(cCharAt4) || !isXML11Name(XMLChar.supplemental(cCharAt3, cCharAt4))) {
                    return false;
                }
            }
            i++;
        }
        return true;
    }

    public static boolean isXML11ValidNCName(String str) {
        int i;
        int length = str.length();
        if (length == 0) {
            return false;
        }
        char cCharAt = str.charAt(0);
        if (!isXML11NCNameStart(cCharAt)) {
            if (length <= 1 || !isXML11NameHighSurrogate(cCharAt)) {
                return false;
            }
            char cCharAt2 = str.charAt(1);
            if (!XMLChar.isLowSurrogate(cCharAt2) || !isXML11NCNameStart(XMLChar.supplemental(cCharAt, cCharAt2))) {
                return false;
            }
            i = 2;
        } else {
            i = 1;
        }
        while (i < length) {
            char cCharAt3 = str.charAt(i);
            if (!isXML11NCName(cCharAt3)) {
                i++;
                if (i >= length || !isXML11NameHighSurrogate(cCharAt3)) {
                    return false;
                }
                char cCharAt4 = str.charAt(i);
                if (!XMLChar.isLowSurrogate(cCharAt4) || !isXML11NCName(XMLChar.supplemental(cCharAt3, cCharAt4))) {
                    return false;
                }
            }
            i++;
        }
        return true;
    }

    public static boolean isXML11ValidNmtoken(String str) {
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (!isXML11Name(cCharAt)) {
                i++;
                if (i >= length || !isXML11NameHighSurrogate(cCharAt)) {
                    return false;
                }
                char cCharAt2 = str.charAt(i);
                if (!XMLChar.isLowSurrogate(cCharAt2) || !isXML11Name(XMLChar.supplemental(cCharAt, cCharAt2))) {
                    return false;
                }
            }
            i++;
        }
        return true;
    }

    public static boolean isXML11ValidQName(String str) {
        int iIndexOf = str.indexOf(58);
        if (iIndexOf == 0 || iIndexOf == str.length() - 1) {
            return false;
        }
        if (iIndexOf > 0) {
            return isXML11ValidNCName(str.substring(0, iIndexOf)) && isXML11ValidNCName(str.substring(iIndexOf + 1));
        }
        return isXML11ValidNCName(str);
    }
}
