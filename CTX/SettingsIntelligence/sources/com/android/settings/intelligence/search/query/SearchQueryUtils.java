package com.android.settings.intelligence.search.query;

import android.text.TextUtils;

public class SearchQueryUtils {
    public static int getWordDifference(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return -1;
        }
        char[] charArray = str2.toLowerCase().toCharArray();
        char[] charArray2 = str.toLowerCase().toCharArray();
        int length = charArray2.length;
        if (charArray.length > length) {
            return -1;
        }
        int i = 0;
        while (i < length) {
            int i2 = 0;
            do {
                int i3 = i + i2;
                if (i3 < length && charArray[i2] == charArray2[i3]) {
                    i2++;
                } else {
                    if (charArray.length > length - i3) {
                        return -1;
                    }
                    while (true) {
                        if (i3 >= length) {
                            i = i3;
                            break;
                        }
                        i = i3 + 1;
                        if (Character.isWhitespace(charArray2[i3])) {
                            break;
                        }
                        i3 = i;
                    }
                    while (i < length && !Character.isLetter(charArray2[i]) && !Character.isDigit(charArray2[i])) {
                        i++;
                    }
                }
            } while (i2 < charArray.length);
            return length - charArray.length;
        }
        return -1;
    }
}
