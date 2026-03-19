package android.icu.impl;

import android.icu.text.PluralRules;

public final class SimplePatternFormatter {
    private static final int ARG_NUM_LIMIT = 256;
    private static final int MAX_SEGMENT_LENGTH = 65279;
    private static final char SEGMENT_LENGTH_PLACEHOLDER_CHAR = 65535;
    private final String compiledPattern;

    private SimplePatternFormatter(String compiledPattern) {
        this.compiledPattern = compiledPattern;
    }

    public static SimplePatternFormatter compile(CharSequence pattern) {
        return compileMinMaxPlaceholders(pattern, 0, Integer.MAX_VALUE);
    }

    public static SimplePatternFormatter compileMinMaxPlaceholders(CharSequence pattern, int min, int max) {
        StringBuilder sb = new StringBuilder();
        String compiledPattern = compileToStringMinMaxPlaceholders(pattern, sb, min, max);
        return new SimplePatternFormatter(compiledPattern);
    }

    public static String compileToStringMinMaxPlaceholders(CharSequence pattern, StringBuilder sb, int min, int max) {
        int argNumber;
        int patternLength = pattern.length();
        sb.ensureCapacity(patternLength);
        sb.setLength(1);
        int textLength = 0;
        int maxArg = -1;
        boolean inQuote = false;
        int i = 0;
        while (i < patternLength) {
            int i2 = i + 1;
            char c = pattern.charAt(i);
            if (c == '\'') {
                if (i2 < patternLength && (c = pattern.charAt(i2)) == '\'') {
                    i2++;
                } else if (inQuote) {
                    inQuote = false;
                } else if (c == '{' || c == '}') {
                    i2++;
                    inQuote = true;
                } else {
                    c = PatternTokenizer.SINGLE_QUOTE;
                }
                if (textLength == 0) {
                    sb.append((char) 65535);
                }
                sb.append(c);
                textLength++;
                if (textLength != MAX_SEGMENT_LENGTH) {
                    textLength = 0;
                }
            } else if (inQuote || c != '{') {
                if (textLength == 0) {
                }
                sb.append(c);
                textLength++;
                if (textLength != MAX_SEGMENT_LENGTH) {
                }
            } else {
                if (textLength > 0) {
                    sb.setCharAt((sb.length() - textLength) - 1, (char) (textLength + 256));
                    textLength = 0;
                }
                if (i2 + 1 >= patternLength || pattern.charAt(i2) - '0' < 0 || argNumber > 9 || pattern.charAt(i2 + 1) != '}') {
                    int argStart = i2 - 1;
                    argNumber = -1;
                    if (i2 < patternLength) {
                        int i3 = i2 + 1;
                        c = pattern.charAt(i2);
                        if ('1' <= c && c <= '9') {
                            argNumber = c - 48;
                            while (true) {
                                if (i3 >= patternLength) {
                                    i2 = i3;
                                    break;
                                }
                                i2 = i3 + 1;
                                c = pattern.charAt(i3);
                                if ('0' > c || c > '9' || (argNumber = (argNumber * 10) + (c - 48)) >= 256) {
                                    break;
                                }
                                i3 = i2;
                            }
                        } else {
                            i2 = i3;
                        }
                    }
                    if (argNumber < 0 || c != '}') {
                        throw new IllegalArgumentException("Argument syntax error in pattern \"" + pattern + "\" at index " + argStart + PluralRules.KEYWORD_RULE_SEPARATOR + pattern.subSequence(argStart, i2));
                    }
                } else {
                    i2 += 2;
                }
                if (argNumber > maxArg) {
                    maxArg = argNumber;
                }
                sb.append((char) argNumber);
            }
            i = i2;
        }
        if (textLength > 0) {
            sb.setCharAt((sb.length() - textLength) - 1, (char) (textLength + 256));
        }
        int argCount = maxArg + 1;
        if (argCount < min) {
            throw new IllegalArgumentException("Fewer than minimum " + min + " placeholders in pattern \"" + pattern + "\"");
        }
        if (argCount > max) {
            throw new IllegalArgumentException("More than maximum " + max + " placeholders in pattern \"" + pattern + "\"");
        }
        sb.setCharAt(0, (char) argCount);
        return sb.toString();
    }

    public int getPlaceholderCount() {
        return getPlaceholderCount(this.compiledPattern);
    }

    public static int getPlaceholderCount(String compiledPattern) {
        return compiledPattern.charAt(0);
    }

    public String format(CharSequence... values) {
        return formatCompiledPattern(this.compiledPattern, values);
    }

    public static String formatCompiledPattern(String compiledPattern, CharSequence... values) {
        return formatAndAppend(compiledPattern, new StringBuilder(), null, values).toString();
    }

    public StringBuilder formatAndAppend(StringBuilder appendTo, int[] offsets, CharSequence... values) {
        return formatAndAppend(this.compiledPattern, appendTo, offsets, values);
    }

    public static StringBuilder formatAndAppend(String compiledPattern, StringBuilder appendTo, int[] offsets, CharSequence... values) {
        int valuesLength = values != null ? values.length : 0;
        if (valuesLength < getPlaceholderCount(compiledPattern)) {
            throw new IllegalArgumentException("Too few values.");
        }
        return format(compiledPattern, values, appendTo, null, true, offsets);
    }

    public StringBuilder formatAndReplace(StringBuilder result, int[] offsets, CharSequence... values) {
        return formatAndReplace(this.compiledPattern, result, offsets, values);
    }

    public static StringBuilder formatAndReplace(String compiledPattern, StringBuilder result, int[] offsets, CharSequence... values) {
        int valuesLength = values != null ? values.length : 0;
        if (valuesLength < getPlaceholderCount(compiledPattern)) {
            throw new IllegalArgumentException("Too few values.");
        }
        int firstArg = -1;
        String resultCopy = null;
        if (getPlaceholderCount(compiledPattern) > 0) {
            int i = 1;
            while (i < compiledPattern.length()) {
                int i2 = i + 1;
                int n = compiledPattern.charAt(i);
                if (n < 256) {
                    if (values[n] == result) {
                        if (i2 == 2) {
                            firstArg = n;
                            i = i2;
                        } else if (resultCopy == null) {
                            resultCopy = result.toString();
                            i = i2;
                        }
                    }
                    i = i2;
                } else {
                    i = i2 + (n - 256);
                }
            }
        }
        if (firstArg < 0) {
            result.setLength(0);
        }
        return format(compiledPattern, values, result, resultCopy, false, offsets);
    }

    public String toString() {
        String[] values = new String[getPlaceholderCount()];
        for (int i = 0; i < values.length; i++) {
            values[i] = String.format("{%d}", Integer.valueOf(i));
        }
        return formatAndAppend(new StringBuilder(), null, values).toString();
    }

    public String getTextWithNoPlaceholders() {
        return getTextWithNoPlaceholders(this.compiledPattern);
    }

    public static String getTextWithNoPlaceholders(String compiledPattern) {
        int capacity = (compiledPattern.length() - 1) - getPlaceholderCount(compiledPattern);
        StringBuilder sb = new StringBuilder(capacity);
        int i = 1;
        while (i < compiledPattern.length()) {
            int i2 = i + 1;
            int segmentLength = compiledPattern.charAt(i) - 256;
            if (segmentLength > 0) {
                int limit = i2 + segmentLength;
                sb.append((CharSequence) compiledPattern, i2, limit);
                i = limit;
            } else {
                i = i2;
            }
        }
        return sb.toString();
    }

    private static StringBuilder format(String compiledPattern, CharSequence[] values, StringBuilder result, String resultCopy, boolean forbidResultAsValue, int[] offsets) {
        int offsetsLength;
        if (offsets == null) {
            offsetsLength = 0;
        } else {
            offsetsLength = offsets.length;
            for (int i = 0; i < offsetsLength; i++) {
                offsets[i] = -1;
            }
        }
        int i2 = 1;
        while (i2 < compiledPattern.length()) {
            int i3 = i2 + 1;
            int n = compiledPattern.charAt(i2);
            if (n < 256) {
                CharSequence value = values[n];
                if (value == result) {
                    if (forbidResultAsValue) {
                        throw new IllegalArgumentException("Value must not be same object as result");
                    }
                    if (i3 == 2) {
                        if (n < offsetsLength) {
                            offsets[n] = 0;
                            i2 = i3;
                        } else {
                            i2 = i3;
                        }
                    } else {
                        if (n < offsetsLength) {
                            offsets[n] = result.length();
                        }
                        result.append(resultCopy);
                        i2 = i3;
                    }
                } else {
                    if (n < offsetsLength) {
                        offsets[n] = result.length();
                    }
                    result.append(value);
                    i2 = i3;
                }
            } else {
                int limit = i3 + (n - 256);
                result.append((CharSequence) compiledPattern, i3, limit);
                i2 = limit;
            }
        }
        return result;
    }
}
