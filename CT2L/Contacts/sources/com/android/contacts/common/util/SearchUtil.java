package com.android.contacts.common.util;

public class SearchUtil {

    public static class MatchedLine {
        public String line;
        public int startIndex = -1;

        public String toString() {
            return "MatchedLine{line='" + this.line + "', startIndex=" + this.startIndex + '}';
        }
    }

    public static MatchedLine findMatchingLine(String contents, String substring) {
        MatchedLine matched = new MatchedLine();
        int index = contains(contents, substring);
        if (index != -1) {
            int start = index - 1;
            while (start > -1 && contents.charAt(start) != '\n') {
                start--;
            }
            int end = index + 1;
            while (end < contents.length() && contents.charAt(end) != '\n') {
                end++;
            }
            matched.line = contents.substring(start + 1, end);
            matched.startIndex = index - (start + 1);
        }
        return matched;
    }

    static int contains(String value, String substring) {
        if (value.length() < substring.length()) {
            return -1;
        }
        int[] substringCodePoints = new int[substring.length()];
        int substringLength = 0;
        int i = 0;
        while (i < substring.length()) {
            int codePoint = Character.codePointAt(substring, i);
            substringCodePoints[substringLength] = codePoint;
            substringLength++;
            i += Character.charCount(codePoint);
        }
        int i2 = 0;
        while (i2 < value.length()) {
            int numMatch = 0;
            int j = i2;
            while (j < value.length() && numMatch < substringLength) {
                int valueCp = Character.toLowerCase(value.codePointAt(j));
                int substringCp = substringCodePoints[numMatch];
                if (valueCp != substringCp) {
                    break;
                }
                j += Character.charCount(valueCp);
                numMatch++;
            }
            if (numMatch != substringLength) {
                i2 = findNextTokenStart(value, i2);
            } else {
                return i2;
            }
        }
        return -1;
    }

    static int findNextTokenStart(String line, int startIndex) {
        int index = startIndex;
        while (index <= line.length()) {
            if (index == line.length()) {
                return index;
            }
            int codePoint = line.codePointAt(index);
            if (!Character.isLetterOrDigit(codePoint)) {
                break;
            }
            index += Character.charCount(codePoint);
        }
        while (index <= line.length()) {
            if (index == line.length()) {
                return index;
            }
            int codePoint2 = line.codePointAt(index);
            if (Character.isLetterOrDigit(codePoint2)) {
                break;
            }
            index += Character.charCount(codePoint2);
        }
        return index;
    }

    public static String cleanStartAndEndOfSearchQuery(String query) {
        int start = 0;
        while (start < query.length()) {
            int codePoint = query.codePointAt(start);
            if (Character.isLetterOrDigit(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        if (start == query.length()) {
            return "";
        }
        int end = query.length() - 1;
        while (end > -1) {
            if (Character.isLowSurrogate(query.charAt(end))) {
                end--;
            }
            if (Character.isLetterOrDigit(query.codePointAt(end))) {
                break;
            }
            end--;
        }
        return query.substring(start, end + 1);
    }
}
