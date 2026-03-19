package com.adobe.xmp.impl;

public class Utils {
    private static boolean[] xmlNameChars;
    private static boolean[] xmlNameStartChars;

    static {
        initCharTables();
    }

    public static String normalizeLangValue(String str) {
        if ("x-default".equals(str)) {
            return str;
        }
        StringBuffer stringBuffer = new StringBuffer();
        int i = 1;
        for (int i2 = 0; i2 < str.length(); i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt != ' ') {
                if (cCharAt == '-' || cCharAt == '_') {
                    stringBuffer.append('-');
                    i++;
                } else if (i != 2) {
                    stringBuffer.append(Character.toLowerCase(str.charAt(i2)));
                } else {
                    stringBuffer.append(Character.toUpperCase(str.charAt(i2)));
                }
            }
        }
        return stringBuffer.toString();
    }

    static String[] splitNameAndValue(String str) {
        int i;
        int iIndexOf = str.indexOf(61);
        if (str.charAt(1) != '?') {
            i = 1;
        } else {
            i = 2;
        }
        String strSubstring = str.substring(i, iIndexOf);
        int i2 = iIndexOf + 1;
        char cCharAt = str.charAt(i2);
        int i3 = i2 + 1;
        int length = str.length() - 2;
        StringBuffer stringBuffer = new StringBuffer(length - iIndexOf);
        while (i3 < length) {
            stringBuffer.append(str.charAt(i3));
            i3++;
            if (str.charAt(i3) == cCharAt) {
                i3++;
            }
        }
        return new String[]{strSubstring, stringBuffer.toString()};
    }

    public static boolean isXMLName(String str) {
        if (str.length() > 0 && !isNameStartChar(str.charAt(0))) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            if (!isNameChar(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isXMLNameNS(String str) {
        if (str.length() > 0 && (!isNameStartChar(str.charAt(0)) || str.charAt(0) == ':')) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            if (!isNameChar(str.charAt(i)) || str.charAt(i) == ':') {
                return false;
            }
        }
        return true;
    }

    static boolean isControlChar(char c) {
        return ((c > 31 && c != 127) || c == '\t' || c == '\n' || c == '\r') ? false : true;
    }

    static String removeControlChars(String str) {
        StringBuffer stringBuffer = new StringBuffer(str);
        for (int i = 0; i < stringBuffer.length(); i++) {
            if (isControlChar(stringBuffer.charAt(i))) {
                stringBuffer.setCharAt(i, ' ');
            }
        }
        return stringBuffer.toString();
    }

    private static boolean isNameStartChar(char c) {
        return c > 255 || xmlNameStartChars[c];
    }

    private static boolean isNameChar(char c) {
        return c > 255 || xmlNameChars[c];
    }

    private static void initCharTables() {
        xmlNameChars = new boolean[256];
        xmlNameStartChars = new boolean[256];
        char c = 0;
        while (c < xmlNameChars.length) {
            boolean z = true;
            xmlNameStartChars[c] = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == ':' || c == '_' || ((192 <= c && c <= 214) || (216 <= c && c <= 246));
            boolean[] zArr = xmlNameChars;
            if (('a' > c || c > 'z') && (('A' > c || c > 'Z') && (('0' > c || c > '9') && c != ':' && c != '_' && c != '-' && c != '.' && c != 183 && ((192 > c || c > 214) && (216 > c || c > 246))))) {
                z = false;
            }
            zArr[c] = z;
            c = (char) (c + 1);
        }
    }
}
