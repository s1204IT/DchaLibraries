package com.android.contacts.util;

public class SearchUtil {

    public static class MatchedLine {
        public String line;
        public int startIndex = -1;

        public String toString() {
            return "MatchedLine{line='" + this.line + "', startIndex=" + this.startIndex + '}';
        }
    }

    public static MatchedLine findMatchingLine(String str, String str2) {
        MatchedLine matchedLine = new MatchedLine();
        int iContains = contains(str, str2);
        if (iContains != -1) {
            int i = iContains - 1;
            while (i > -1 && str.charAt(i) != '\n') {
                i--;
            }
            int i2 = iContains + 1;
            while (i2 < str.length() && str.charAt(i2) != '\n') {
                i2++;
            }
            int i3 = i + 1;
            matchedLine.line = str.substring(i3, i2);
            matchedLine.startIndex = iContains - i3;
        }
        return matchedLine;
    }

    static int contains(String str, String str2) {
        int lowerCase;
        if (str.length() < str2.length()) {
            return -1;
        }
        int[] iArr = new int[str2.length()];
        int iCharCount = 0;
        int i = 0;
        while (iCharCount < str2.length()) {
            int iCodePointAt = Character.codePointAt(str2, iCharCount);
            iArr[i] = iCodePointAt;
            i++;
            iCharCount += Character.charCount(iCodePointAt);
        }
        int iFindNextTokenStart = 0;
        while (iFindNextTokenStart < str.length()) {
            int iCharCount2 = iFindNextTokenStart;
            int i2 = 0;
            while (iCharCount2 < str.length() && i2 < i && (lowerCase = Character.toLowerCase(str.codePointAt(iCharCount2))) == iArr[i2]) {
                iCharCount2 += Character.charCount(lowerCase);
                i2++;
            }
            if (i2 != i) {
                iFindNextTokenStart = findNextTokenStart(str, iFindNextTokenStart);
            } else {
                return iFindNextTokenStart;
            }
        }
        return -1;
    }

    static int findNextTokenStart(String str, int i) {
        while (i <= str.length()) {
            if (i == str.length()) {
                return i;
            }
            int iCodePointAt = str.codePointAt(i);
            if (!Character.isLetterOrDigit(iCodePointAt)) {
                break;
            }
            i += Character.charCount(iCodePointAt);
        }
        while (i <= str.length()) {
            if (i == str.length()) {
                return i;
            }
            int iCodePointAt2 = str.codePointAt(i);
            if (Character.isLetterOrDigit(iCodePointAt2)) {
                break;
            }
            i += Character.charCount(iCodePointAt2);
        }
        return i;
    }

    public static String cleanStartAndEndOfSearchQuery(String str) {
        int iCharCount = 0;
        while (iCharCount < str.length()) {
            int iCodePointAt = str.codePointAt(iCharCount);
            if (Character.isLetterOrDigit(iCodePointAt)) {
                break;
            }
            iCharCount += Character.charCount(iCodePointAt);
        }
        if (iCharCount == str.length()) {
            return "";
        }
        int length = str.length();
        do {
            length--;
            if (length <= -1) {
                break;
            }
            if (Character.isLowSurrogate(str.charAt(length))) {
                length--;
            }
        } while (!Character.isLetterOrDigit(str.codePointAt(length)));
        return str.substring(iCharCount, length + 1);
    }
}
