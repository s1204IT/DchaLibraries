package mf.org.apache.xerces.impl.xs;

import java.io.File;
import java.io.UnsupportedEncodingException;

final class FilePathToURI {
    private static boolean[] gNeedEscaping = new boolean[128];
    private static char[] gAfterEscaping1 = new char[128];
    private static char[] gAfterEscaping2 = new char[128];
    private static char[] gHexChs = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    static {
        for (int i = 0; i <= 31; i++) {
            gNeedEscaping[i] = true;
            gAfterEscaping1[i] = gHexChs[i >> 4];
            gAfterEscaping2[i] = gHexChs[i & 15];
        }
        gNeedEscaping[127] = true;
        gAfterEscaping1[127] = '7';
        gAfterEscaping2[127] = 'F';
        char[] escChs = {' ', '<', '>', '#', '%', '\"', '{', '}', '|', '\\', '^', '~', '[', ']', '`'};
        for (char ch : escChs) {
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> 4];
            gAfterEscaping2[ch] = gHexChs[ch & 15];
        }
    }

    private FilePathToURI() {
    }

    public static String filepath2URI(String path) {
        int ch;
        int ch2;
        if (path == null) {
            return null;
        }
        char separator = File.separatorChar;
        String path2 = path.replace(separator, '/');
        int len = path2.length();
        StringBuffer buffer = new StringBuffer(len * 3);
        buffer.append("file://");
        if (len >= 2 && path2.charAt(1) == ':' && (ch2 = Character.toUpperCase(path2.charAt(0))) >= 65 && ch2 <= 90) {
            buffer.append('/');
        }
        int i = 0;
        while (i < len && (ch = path2.charAt(i)) < 128) {
            if (gNeedEscaping[ch]) {
                buffer.append('%');
                buffer.append(gAfterEscaping1[ch]);
                buffer.append(gAfterEscaping2[ch]);
            } else {
                buffer.append((char) ch);
            }
            i++;
        }
        if (i < len) {
            try {
                byte[] bytes = path2.substring(i).getBytes("UTF-8");
                for (byte b : bytes) {
                    if (b < 0) {
                        int ch3 = b + 256;
                        buffer.append('%');
                        buffer.append(gHexChs[ch3 >> 4]);
                        buffer.append(gHexChs[ch3 & 15]);
                    } else if (gNeedEscaping[b]) {
                        buffer.append('%');
                        buffer.append(gAfterEscaping1[b]);
                        buffer.append(gAfterEscaping2[b]);
                    } else {
                        buffer.append((char) b);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                return path2;
            }
        }
        return buffer.toString();
    }
}
