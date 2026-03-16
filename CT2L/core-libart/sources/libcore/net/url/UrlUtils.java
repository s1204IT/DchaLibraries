package libcore.net.url;

import java.util.Locale;

public final class UrlUtils {
    private UrlUtils() {
    }

    public static String canonicalizePath(String path, boolean discardRelativePrefix) {
        int nextSegmentStart;
        int segmentStart = 0;
        int deletableSegments = 0;
        int i = 0;
        while (i <= path.length()) {
            if (i == path.length()) {
                nextSegmentStart = i;
            } else if (path.charAt(i) == '/') {
                nextSegmentStart = i + 1;
            } else {
                i++;
            }
            if (i == segmentStart + 1 && path.regionMatches(segmentStart, ".", 0, 1)) {
                path = path.substring(0, segmentStart) + path.substring(nextSegmentStart);
                i = segmentStart;
            } else if (i == segmentStart + 2 && path.regionMatches(segmentStart, "..", 0, 2)) {
                if (deletableSegments > 0 || discardRelativePrefix) {
                    deletableSegments--;
                    int prevSegmentStart = path.lastIndexOf(47, segmentStart - 2) + 1;
                    path = path.substring(0, prevSegmentStart) + path.substring(nextSegmentStart);
                    segmentStart = prevSegmentStart;
                    i = prevSegmentStart;
                } else {
                    i++;
                    segmentStart = i;
                }
            } else {
                if (i > 0) {
                    deletableSegments++;
                }
                i++;
                segmentStart = i;
            }
        }
        return path;
    }

    public static String authoritySafePath(String authority, String path) {
        if (authority != null && !authority.isEmpty() && !path.isEmpty() && !path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    public static String getSchemePrefix(String spec) {
        int colon = spec.indexOf(58);
        if (colon < 1) {
            return null;
        }
        for (int i = 0; i < colon; i++) {
            char c = spec.charAt(i);
            if (!isValidSchemeChar(i, c)) {
                return null;
            }
        }
        return spec.substring(0, colon).toLowerCase(Locale.US);
    }

    public static boolean isValidSchemeChar(int index, char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c < 'A' || c > 'Z') {
            return index > 0 && ((c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.');
        }
        return true;
    }

    public static int findFirstOf(String string, String chars, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = string.charAt(i);
            if (chars.indexOf(c) != -1) {
                return i;
            }
        }
        return end;
    }
}
