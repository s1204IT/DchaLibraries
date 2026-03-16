package android.net.http;

import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

class CharArrayBuffers {
    static final char uppercaseAddon = ' ';

    CharArrayBuffers() {
    }

    static boolean containsIgnoreCaseTrimmed(CharArrayBuffer buffer, int beginIndex, String str) {
        int len = buffer.length();
        char[] chars = buffer.buffer();
        while (beginIndex < len && HTTP.isWhitespace(chars[beginIndex])) {
            beginIndex++;
        }
        int size = str.length();
        boolean ok = len >= beginIndex + size;
        for (int j = 0; ok && j < size; j++) {
            char a = chars[beginIndex + j];
            char b = str.charAt(j);
            if (a != b) {
                ok = toLower(a) == toLower(b);
            }
        }
        return ok;
    }

    static int setLowercaseIndexOf(CharArrayBuffer buffer, int ch) {
        int endIndex = buffer.length();
        char[] chars = buffer.buffer();
        for (int i = 0; i < endIndex; i++) {
            char current = chars[i];
            if (current != ch) {
                if (current >= 'A' && current <= 'Z') {
                    chars[i] = (char) (current + uppercaseAddon);
                }
            } else {
                return i;
            }
        }
        return -1;
    }

    private static char toLower(char c) {
        if (c >= 'A' && c <= 'Z') {
            return (char) (c + uppercaseAddon);
        }
        return c;
    }
}
