package com.google.common.base;
/* loaded from: classes.dex */
public final class Ascii {
    public static String toLowerCase(String str) {
        int length = str.length();
        int i = 0;
        while (i < length) {
            if (!isUpperCase(str.charAt(i))) {
                i++;
            } else {
                char[] charArray = str.toCharArray();
                while (i < length) {
                    char c = charArray[i];
                    if (isUpperCase(c)) {
                        charArray[i] = (char) (c ^ ' ');
                    }
                    i++;
                }
                return String.valueOf(charArray);
            }
        }
        return str;
    }

    public static boolean isUpperCase(char c) {
        return c >= 'A' && c <= 'Z';
    }
}
