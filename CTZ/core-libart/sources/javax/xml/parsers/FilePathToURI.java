package javax.xml.parsers;

import android.icu.impl.PatternTokenizer;
import java.io.File;
import java.io.UnsupportedEncodingException;

class FilePathToURI {
    private static boolean[] gNeedEscaping = new boolean[128];
    private static char[] gAfterEscaping1 = new char[128];
    private static char[] gAfterEscaping2 = new char[128];
    private static char[] gHexChs = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    FilePathToURI() {
    }

    static {
        for (int i = 0; i <= 31; i++) {
            gNeedEscaping[i] = true;
            gAfterEscaping1[i] = gHexChs[i >> 4];
            gAfterEscaping2[i] = gHexChs[i & 15];
        }
        gNeedEscaping[127] = true;
        gAfterEscaping1[127] = '7';
        gAfterEscaping2[127] = 'F';
        for (char c : new char[]{' ', '<', '>', '#', '%', '\"', '{', '}', '|', PatternTokenizer.BACK_SLASH, '^', '~', '[', ']', '`'}) {
            gNeedEscaping[c] = true;
            gAfterEscaping1[c] = gHexChs[c >> 4];
            gAfterEscaping2[c] = gHexChs[c & 15];
        }
    }

    public static String filepath2URI(String str) {
        char cCharAt;
        char upperCase;
        if (str == null) {
            return null;
        }
        String strReplace = str.replace(File.separatorChar, '/');
        int length = strReplace.length();
        StringBuilder sb = new StringBuilder(length * 3);
        sb.append("file://");
        if (length >= 2 && strReplace.charAt(1) == ':' && (upperCase = Character.toUpperCase(strReplace.charAt(0))) >= 'A' && upperCase <= 'Z') {
            sb.append('/');
        }
        int i = 0;
        while (i < length && (cCharAt = strReplace.charAt(i)) < 128) {
            if (gNeedEscaping[cCharAt]) {
                sb.append('%');
                sb.append(gAfterEscaping1[cCharAt]);
                sb.append(gAfterEscaping2[cCharAt]);
            } else {
                sb.append(cCharAt);
            }
            i++;
        }
        if (i < length) {
            try {
                for (byte b : strReplace.substring(i).getBytes("UTF-8")) {
                    if (b < 0) {
                        int i2 = b + 256;
                        sb.append('%');
                        sb.append(gHexChs[i2 >> 4]);
                        sb.append(gHexChs[i2 & 15]);
                    } else if (gNeedEscaping[b]) {
                        sb.append('%');
                        sb.append(gAfterEscaping1[b]);
                        sb.append(gAfterEscaping2[b]);
                    } else {
                        sb.append((char) b);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                return strReplace;
            }
        }
        return sb.toString();
    }
}
