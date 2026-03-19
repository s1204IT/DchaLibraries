package org.apache.xml.utils;

public class XMLCharacterRecognizer {
    public static boolean isWhiteSpace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    public static boolean isWhiteSpace(char[] cArr, int i, int i2) {
        int i3 = i2 + i;
        while (i < i3) {
            if (isWhiteSpace(cArr[i])) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean isWhiteSpace(StringBuffer stringBuffer) {
        int length = stringBuffer.length();
        for (int i = 0; i < length; i++) {
            if (!isWhiteSpace(stringBuffer.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWhiteSpace(String str) {
        if (str != null) {
            int length = str.length();
            for (int i = 0; i < length; i++) {
                if (!isWhiteSpace(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }
}
