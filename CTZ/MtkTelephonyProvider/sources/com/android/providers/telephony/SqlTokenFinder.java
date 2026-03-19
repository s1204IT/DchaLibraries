package com.android.providers.telephony;

import java.util.function.Consumer;

public class SqlTokenFinder {
    private static boolean isAlpha(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    private static boolean isNum(char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isAlNum(char c) {
        return isAlpha(c) || isNum(c);
    }

    private static boolean isAnyOf(char c, String str) {
        return str.indexOf(c) >= 0;
    }

    private static char peek(String str, int i) {
        if (i < str.length()) {
            return str.charAt(i);
        }
        return (char) 0;
    }

    public static void findTokens(String str, Consumer<String> consumer) {
        if (str == null) {
            return;
        }
        int i = 0;
        int length = str.length();
        while (i < length) {
            char cPeek = peek(str, i);
            if (isAlpha(cPeek)) {
                int i2 = i + 1;
                while (isAlNum(peek(str, i2))) {
                    i2++;
                }
                consumer.accept(str.substring(i, i2));
                i = i2;
            } else if (isAnyOf(cPeek, "'\"`")) {
                int i3 = i + 1;
                int i4 = i3;
                while (true) {
                    int iIndexOf = str.indexOf(cPeek, i4);
                    if (iIndexOf < 0) {
                        throw new IllegalArgumentException("Unterminated quote in" + str);
                    }
                    int i5 = iIndexOf + 1;
                    if (peek(str, i5) != cPeek) {
                        if (cPeek != '\'') {
                            String strSubstring = str.substring(i3, iIndexOf);
                            if (strSubstring.indexOf(cPeek) >= 0) {
                                strSubstring = strSubstring.replaceAll(String.valueOf(cPeek) + cPeek, String.valueOf(cPeek));
                            }
                            consumer.accept(strSubstring);
                        }
                        i = i5;
                    } else {
                        i4 = iIndexOf + 2;
                    }
                }
            } else if (cPeek == '[') {
                int i6 = i + 1;
                int iIndexOf2 = str.indexOf(93, i6);
                if (iIndexOf2 < 0) {
                    throw new IllegalArgumentException("Unterminated quote in" + str);
                }
                consumer.accept(str.substring(i6, iIndexOf2));
                i = iIndexOf2 + 1;
            } else if (cPeek == '-' && peek(str, i + 1) == '-') {
                int iIndexOf3 = str.indexOf(10, i + 2);
                if (iIndexOf3 < 0) {
                    throw new IllegalArgumentException("Unterminated comment in" + str);
                }
                i = iIndexOf3 + 1;
            } else if (cPeek == '/' && peek(str, i + 1) == '*') {
                int iIndexOf4 = str.indexOf("*/", i + 2);
                if (iIndexOf4 < 0) {
                    throw new IllegalArgumentException("Unterminated comment in" + str);
                }
                i = iIndexOf4 + 2;
            } else {
                if (cPeek == ';') {
                    throw new IllegalArgumentException("Semicolon is not allowed in " + str);
                }
                i++;
            }
        }
    }
}
