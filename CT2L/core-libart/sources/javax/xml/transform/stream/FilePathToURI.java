package javax.xml.transform.stream;

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
        char[] escChs = {' ', '<', '>', '#', '%', '\"', '{', '}', '|', '\\', '^', '~', '[', ']', '`'};
        for (char ch : escChs) {
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> 4];
            gAfterEscaping2[ch] = gHexChs[ch & 15];
        }
    }

    public static String filepath2URI(String path) {
        int ch;
        if (path == null) {
            return null;
        }
        char separator = File.separatorChar;
        String path2 = path.replace(separator, '/');
        int len = path2.length();
        StringBuilder buffer = new StringBuilder(len * 3);
        buffer.append("file://");
        if (len >= 2 && path2.charAt(1) == ':' && (ch = Character.toUpperCase(path2.charAt(0))) >= 65 && ch <= 90) {
            buffer.append('/');
        }
        int i = 0;
        while (i < len) {
            int ch2 = path2.charAt(i);
            if (ch2 >= 128) {
                break;
            }
            if (gNeedEscaping[ch2]) {
                buffer.append('%');
                buffer.append(gAfterEscaping1[ch2]);
                buffer.append(gAfterEscaping2[ch2]);
            } else {
                buffer.append((char) ch2);
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
