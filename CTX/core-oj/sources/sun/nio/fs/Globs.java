package sun.nio.fs;

import java.sql.Types;
import java.util.regex.PatternSyntaxException;

public class Globs {
    private static char EOL = 0;
    private static final String globMetaChars = "\\*?[{";
    private static final String regexMetaChars = ".^$+{[]|()";

    private Globs() {
    }

    private static boolean isRegexMeta(char c) {
        return regexMetaChars.indexOf(c) != -1;
    }

    private static boolean isGlobMeta(char c) {
        return globMetaChars.indexOf(c) != -1;
    }

    private static char next(String str, int i) {
        if (i < str.length()) {
            return str.charAt(i);
        }
        return EOL;
    }

    private static String toRegexPattern(String str, boolean z) {
        int i;
        StringBuilder sb = new StringBuilder("^");
        int i2 = 0;
        boolean z2 = false;
        while (i2 < str.length()) {
            int i3 = i2 + 1;
            char cCharAt = str.charAt(i2);
            if (cCharAt != '*') {
                if (cCharAt != ',') {
                    if (cCharAt != '/') {
                        if (cCharAt != '?') {
                            if (cCharAt == '{') {
                                if (z2) {
                                    throw new PatternSyntaxException("Cannot nest groups", str, i3 - 1);
                                }
                                sb.append("(?:(?:");
                                i2 = i3;
                                z2 = true;
                            } else if (cCharAt != '}') {
                                switch (cCharAt) {
                                    case Types.DATE:
                                        if (z) {
                                            sb.append("[[^\\\\]&&[");
                                        } else {
                                            sb.append("[[^/]&&[");
                                        }
                                        if (next(str, i3) == '^') {
                                            sb.append("\\^");
                                            i3++;
                                        } else {
                                            if (next(str, i3) == '!') {
                                                sb.append('^');
                                                i3++;
                                            }
                                            if (next(str, i3) == '-') {
                                                sb.append('-');
                                                i3++;
                                            }
                                        }
                                        char c = 0;
                                        char next = cCharAt;
                                        boolean z3 = false;
                                        while (true) {
                                            if (i3 < str.length()) {
                                                i = i3 + 1;
                                                char cCharAt2 = str.charAt(i3);
                                                if (cCharAt2 != ']') {
                                                    if (cCharAt2 != '/' && (!z || cCharAt2 != '\\')) {
                                                        if (cCharAt2 == '\\' || cCharAt2 == '[' || (cCharAt2 == '&' && next(str, i) == '&')) {
                                                            sb.append('\\');
                                                        }
                                                        sb.append(cCharAt2);
                                                        if (cCharAt2 != '-') {
                                                            c = cCharAt2;
                                                            z3 = true;
                                                            i3 = i;
                                                            next = c;
                                                        } else {
                                                            if (!z3) {
                                                                throw new PatternSyntaxException("Invalid range", str, i - 1);
                                                            }
                                                            i3 = i + 1;
                                                            next = next(str, i);
                                                            if (next != EOL && next != ']') {
                                                                if (next < c) {
                                                                    throw new PatternSyntaxException("Invalid range", str, i3 - 3);
                                                                }
                                                                sb.append(next);
                                                                z3 = false;
                                                            }
                                                        }
                                                        break;
                                                    }
                                                } else {
                                                    next = cCharAt2;
                                                    i3 = i;
                                                    break;
                                                }
                                            } else {
                                                break;
                                            }
                                        }
                                        throw new PatternSyntaxException("Explicit 'name separator' in class", str, i - 1);
                                    case Types.TIME:
                                        if (i3 == str.length()) {
                                            throw new PatternSyntaxException("No character to escape", str, i3 - 1);
                                        }
                                        i2 = i3 + 1;
                                        char cCharAt3 = str.charAt(i3);
                                        if (isGlobMeta(cCharAt3) || isRegexMeta(cCharAt3)) {
                                            sb.append('\\');
                                        }
                                        sb.append(cCharAt3);
                                        continue;
                                        break;
                                    default:
                                        if (isRegexMeta(cCharAt)) {
                                            sb.append('\\');
                                        }
                                        sb.append(cCharAt);
                                        break;
                                }
                            } else if (z2) {
                                sb.append("))");
                                z2 = false;
                            } else {
                                sb.append('}');
                            }
                        } else if (z) {
                            sb.append("[^\\\\]");
                        } else {
                            sb.append("[^/]");
                        }
                    } else if (z) {
                        sb.append("\\\\");
                    } else {
                        sb.append(cCharAt);
                    }
                } else if (z2) {
                    sb.append(")|(?:");
                } else {
                    sb.append(',');
                }
            } else if (next(str, i3) == '*') {
                sb.append(".*");
                i3++;
            } else if (z) {
                sb.append("[^\\\\]*");
            } else {
                sb.append("[^/]*");
            }
            i2 = i3;
        }
        if (z2) {
            throw new PatternSyntaxException("Missing '}", str, i2 - 1);
        }
        sb.append('$');
        return sb.toString();
    }

    static String toUnixRegexPattern(String str) {
        return toRegexPattern(str, false);
    }

    static String toWindowsRegexPattern(String str) {
        return toRegexPattern(str, true);
    }
}
