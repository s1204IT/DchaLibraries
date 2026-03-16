package java.util.regex;

import java.util.ArrayList;
import java.util.List;
import libcore.util.EmptyArray;

public class Splitter {
    private static final String METACHARACTERS = "\\?*+[](){}^$.|";

    private Splitter() {
    }

    public static String[] fastSplit(String re, String input, int limit) {
        int end;
        int len = re.length();
        if (len == 0) {
            return null;
        }
        char ch = re.charAt(0);
        if (len != 1 || METACHARACTERS.indexOf(ch) != -1) {
            if (len != 2 || ch != '\\') {
                return null;
            }
            ch = re.charAt(1);
            if (METACHARACTERS.indexOf(ch) == -1) {
                return null;
            }
        }
        if (input.isEmpty()) {
            return new String[]{""};
        }
        int separatorCount = 0;
        int begin = 0;
        while (separatorCount + 1 != limit && (end = input.indexOf(ch, begin)) != -1) {
            separatorCount++;
            begin = end + 1;
        }
        int lastPartEnd = input.length();
        if (limit == 0 && begin == lastPartEnd) {
            if (separatorCount == lastPartEnd) {
                return EmptyArray.STRING;
            }
            do {
                begin--;
            } while (input.charAt(begin - 1) == ch);
            separatorCount -= input.length() - begin;
            lastPartEnd = begin;
        }
        String[] result = new String[separatorCount + 1];
        int begin2 = 0;
        for (int i = 0; i != separatorCount; i++) {
            int end2 = input.indexOf(ch, begin2);
            result[i] = input.substring(begin2, end2);
            begin2 = end2 + 1;
        }
        result[separatorCount] = input.substring(begin2, lastPartEnd);
        return result;
    }

    public static String[] split(Pattern pattern, String re, String input, int limit) {
        String[] fastResult = fastSplit(re, input, limit);
        if (fastResult == null) {
            if (input.isEmpty()) {
                return new String[]{""};
            }
            ArrayList<String> list = new ArrayList<>();
            Matcher matcher = new Matcher(pattern, input);
            int begin = 0;
            while (list.size() + 1 != limit && matcher.find()) {
                list.add(input.substring(begin, matcher.start()));
                begin = matcher.end();
            }
            return finishSplit(list, input, begin, limit);
        }
        return fastResult;
    }

    private static String[] finishSplit(List<String> list, String input, int begin, int limit) {
        if (begin < input.length()) {
            list.add(input.substring(begin));
        } else if (limit != 0) {
            list.add("");
        } else {
            for (int i = list.size() - 1; i >= 0 && list.get(i).isEmpty(); i--) {
                list.remove(i);
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }
}
