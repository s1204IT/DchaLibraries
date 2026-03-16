package com.adobe.xmp.impl;

import android.support.v4.app.NotificationCompat;

public class Utils {
    private static boolean[] xmlNameChars;
    private static boolean[] xmlNameStartChars;

    static {
        initCharTables();
    }

    public static String normalizeLangValue(String value) {
        if (!"x-default".equals(value)) {
            int subTag = 1;
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < value.length(); i++) {
                switch (value.charAt(i)) {
                    case ' ':
                        break;
                    case '-':
                    case '_':
                        buffer.append('-');
                        subTag++;
                        break;
                    default:
                        if (subTag != 2) {
                            buffer.append(Character.toLowerCase(value.charAt(i)));
                        } else {
                            buffer.append(Character.toUpperCase(value.charAt(i)));
                        }
                        break;
                }
            }
            return buffer.toString();
        }
        return value;
    }

    static String[] splitNameAndValue(String selector) {
        int eq = selector.indexOf(61);
        int pos = 1;
        if (selector.charAt(1) == '?') {
            pos = 1 + 1;
        }
        String name = selector.substring(pos, eq);
        int pos2 = eq + 1;
        char quote = selector.charAt(pos2);
        int pos3 = pos2 + 1;
        int end = selector.length() - 2;
        StringBuffer value = new StringBuffer(end - eq);
        while (pos3 < end) {
            value.append(selector.charAt(pos3));
            pos3++;
            if (selector.charAt(pos3) == quote) {
                pos3++;
            }
        }
        return new String[]{name, value.toString()};
    }

    public static boolean isXMLName(String name) {
        if (name.length() > 0 && !isNameStartChar(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!isNameChar(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isXMLNameNS(String name) {
        if (name.length() > 0 && (!isNameStartChar(name.charAt(0)) || name.charAt(0) == ':')) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!isNameChar(name.charAt(i)) || name.charAt(i) == ':') {
                return false;
            }
        }
        return true;
    }

    static boolean isControlChar(char c) {
        return ((c > 31 && c != 127) || c == '\t' || c == '\n' || c == '\r') ? false : true;
    }

    static String removeControlChars(String value) {
        StringBuffer buffer = new StringBuffer(value);
        for (int i = 0; i < buffer.length(); i++) {
            if (isControlChar(buffer.charAt(i))) {
                buffer.setCharAt(i, ' ');
            }
        }
        return buffer.toString();
    }

    private static boolean isNameStartChar(char ch) {
        return ch > 255 || xmlNameStartChars[ch];
    }

    private static boolean isNameChar(char ch) {
        return ch > 255 || xmlNameChars[ch];
    }

    private static void initCharTables() {
        xmlNameChars = new boolean[NotificationCompat.FLAG_LOCAL_ONLY];
        xmlNameStartChars = new boolean[NotificationCompat.FLAG_LOCAL_ONLY];
        char ch = 0;
        while (ch < xmlNameChars.length) {
            xmlNameStartChars[ch] = ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == ':' || ch == '_' || ((192 <= ch && ch <= 214) || (216 <= ch && ch <= 246));
            xmlNameChars[ch] = ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || (('0' <= ch && ch <= '9') || ch == ':' || ch == '_' || ch == '-' || ch == '.' || ch == 183 || ((192 <= ch && ch <= 214) || (216 <= ch && ch <= 246)));
            ch = (char) (ch + 1);
        }
    }
}
